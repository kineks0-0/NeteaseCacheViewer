package io.github.kineks.neteaseviewer.data.network.service.impl

import io.github.kineks.neteaseviewer.data.network.Song
import io.github.kineks.neteaseviewer.data.network.service.NeteaseDataService
import io.github.kineks.neteaseviewer.data.network.service.NeteaseService
import io.github.kineks.neteaseviewer.toURLArray
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext

object NeteaseDataServiceImpl : NeteaseDataService {
    private val handler = CoroutineExceptionHandler { coroutineContext, throwable ->
        coroutineContext.cancel(CancellationException("Error when requesting network.", throwable))
    }

    // todo: 请求缓存保存本地
    private val map: HashMap<Int, Song> = HashMap()


    override suspend fun getSong(id: Int): Song =
        withContext(handler) {
            getSongFromCache(id) ?: getSongFromNetwork(id)
        }

    override suspend fun getSong(ids: List<Int>): List<Song> =
        withContext(handler) {
            val cacheList = getSongFromCache(ids).toMutableList()
            ArrayList<Song>().apply { addAll(cacheList, ids) }
        }


    override suspend fun getSongFromCache(id: Int): Song? = map[id]

    override suspend fun getSongFromCache(ids: List<Int>): List<Song?> {
        val list = ArrayList<Song?>()
        repeat(ids.size) { index ->
            list.add(map[ids[index]])
        }
        return list
    }


    override suspend fun getSongFromNetwork(id: Int): Song =
        withContext(handler) {
            NeteaseService.instance.getSongDetail(id).songs[0].apply {
                map[id] = this
            }
        }

    override suspend fun getSongFromNetwork(ids: List<Int>): List<Song> =
        withContext(handler) {
            NeteaseService.instance.getSongsDetail(ids.toURLArray()).songs.apply {
                forEachIndexed { index, song ->
                    map[ids[index]] = song
                }
            }
        }

    override fun getAlbumPicUrl(id: Int, width: Int, height: Int): String? {
        val song = map[id]
        if (song?.album?.picUrl != null) {
            // api 如果不同时限定宽高参数就会默认返回原图
            if (width != -1 && height != -1) {
                return song.album.picUrl + "?param=${width}y$height"
            }
            return song.album.picUrl
        }
        return null
    }


}

private suspend fun MutableList<Song>.addAll(
    elements: MutableList<Song?>,
    ids: List<Int>
): Boolean {

    val idsNetwork = ArrayList<Int>()
    elements.forEachIndexed { index, song ->
        if (song == null)
            idsNetwork.add(ids[index])
    }

    if (idsNetwork.isNotEmpty()) {
        val networkList = NeteaseDataServiceImpl.getSongFromNetwork(idsNetwork)
        var index = 0

        elements.forEach { song ->
            if (song == null)
                add(networkList[index])
            else
                add(song)
            index++
        }
    } else {

        elements.forEach { song ->
            add(song!!)
        }
    }

    return true
}
