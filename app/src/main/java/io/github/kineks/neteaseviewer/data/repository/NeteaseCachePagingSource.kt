package io.github.kineks.neteaseviewer.data.repository

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.github.kineks.neteaseviewer.data.local.NeteaseCacheProvider
import io.github.kineks.neteaseviewer.data.local.RFile
import io.github.kineks.neteaseviewer.data.local.cacheFile.CacheFileInfo
import io.github.kineks.neteaseviewer.data.local.cacheFile.MusicState
import io.github.kineks.neteaseviewer.data.network.service.NeteaseDataService
import io.github.kineks.neteaseviewer.runWithPrintTimeCostSuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NeteaseCachePagingSource(private val cacheDir: List<NeteaseCacheProvider.NeteaseAppCache> = NeteaseCacheProvider.cacheDir) :
    PagingSource<NeteaseCacheProvider.NeteaseAppCache, MusicState>() {

    override fun getRefreshKey(state: PagingState<NeteaseCacheProvider.NeteaseAppCache, MusicState>): NeteaseCacheProvider.NeteaseAppCache? =
        null

    private suspend inline fun rfile2MusicSate(
        neteaseAppCache: NeteaseCacheProvider.NeteaseAppCache,
        infoFileMap: HashMap<String, RFile>,
        playFileList: ArrayList<RFile>,
        rfile: RFile
    ): MusicState {
        val infoFile = when (NeteaseCacheProvider.fastReader) {
            true -> null
            false -> infoFileMap[rfile.nameWithoutExtension]
        }
        var id = -1
        var bitrate: Int = -1
        var md5 = ""
        var incomplete = false
        var missingInfo = true

        //playFileList.remove(rfile)


        if ((infoFile != null) && infoFile.exists()) {
            val idx =
                NeteaseCacheProvider.gson.fromJson(infoFile.readText(), CacheFileInfo::class.java)
            if (idx != null) {
                id = idx.id
                bitrate = idx.bitrate
                md5 = idx.fileMD5
                // 判断缓存文件和缓存文件信息中的文件长度大小是否一致
                incomplete = idx.fileSize != rfile.length()
                missingInfo = false
            }
            //infoFileMap.remove(rfile.nameWithoutExtension)
        }

        // 如果数据依旧没初始化
        if (id == -1) {
            // 如果启用快速扫描则跳过读取idx文件
            // 或者 *.idx! 文件并不存在(一般是 unlock netease music 导致)
            rfile.nameWithoutExtension.split("-").let { str ->
                id = str[0].toInt()
                bitrate = str[1].toInt()
                md5 = str[2].substring(0, 31)

            }
        }

        return MusicState(
            id = id,
            bitrate = bitrate,
            md5 = md5,
            file = rfile,
            song = NeteaseDataService.instance.getSongFromCache(id),
            incomplete = incomplete,
            missingInfo = missingInfo,
            neteaseAppCache = neteaseAppCache
        )
    }

    /*suspend fun reloadSongsList(list: List<MusicState>? = null, updateInfo: Boolean = false) {
        if (songs.isNotEmpty())
            songs.clear()

        if (list != null && list.isNotEmpty()) {
            songs.addAll(list)
        } else {
            songs.addAll(NeteaseCacheProvider.getCacheSongs())
        }

        if (updateInfo)
            updateSongsInfo()

        hadListInited = true
    }*/

    private suspend fun updateSongsInfo(songs: ArrayList<MusicState>, quantity: Int = 50) {
        /*isUpdateComplete = false
        isFailure = false

        isUpdating = true

        val updateComplete: (isFailure: Boolean) -> Unit =
            { isFailure ->
                this.isUpdating = false
                this.isUpdateComplete = true
                this.isFailure = isFailure
            }*/

        // 如果列表为空
        if (songs.isEmpty()) {
            //updateComplete.invoke(true)
            return
        }

        // 计算分页数量
        var pages = songs.size / quantity - 1
        if (songs.size % quantity != 0)
            pages++

        for (i in 0..pages) {
            withContext(Dispatchers.IO) {
                launch {

                    // 对于 list 索引的偏移值
                    val offset = i * quantity

                    // 该页的数量
                    val size =
                        when (true) {
                            // 单页加载,但列表数量小于单页加载数量
                            (quantity > songs.size) -> songs.size

                            // 最后一页,计算剩下多少
                            (i == pages) -> songs.size - offset

                            // 其余情况都是单页加载数量
                            else -> quantity
                        }


                    val ids = ArrayList<Int>()
                    val indexList = ArrayList<Int>()
                    repeat(size) {
                        val index = offset + it
                        //Log.d(this@MainViewModel::javaClass.name, "update:$index")
                        // 如果缓存里有则跳过
                        if (songs[index].song == null) {
                            val id = songs[index].id
                            ids.add(id)
                            indexList.add(offset + it)
                        }
                    }

                    when (true) {
                        ids.isEmpty() -> {}
                        (ids.size == 1) -> NeteaseDataService.instance.getSong(ids[0])
                        else -> NeteaseDataService.instance.getSong(ids)
                    }

                    indexList.forEach { index ->
                        val id = songs[index].id
                        if (NeteaseDataService.instance.getSongFromCache(id) != null) {
                            songs[index] = songs[index]
                                .reload(NeteaseDataService.instance.getSongFromCache(id))
                            Log.d(
                                this.javaClass.name,
                                "update Song $index : " + songs[index].name
                            )
                        }
                    }

                    ids.clear()
                    indexList.clear()

                    /*// 如果加载完最后一页
                    if (i == pages)
                        updateComplete.invoke(false)*/

                }
            }
        }


    }

    override suspend fun load(params: LoadParams<NeteaseCacheProvider.NeteaseAppCache>): LoadResult<NeteaseCacheProvider.NeteaseAppCache, MusicState> {
        try {
            val neteaseAppCache = params.key ?: cacheDir[0]
            val infoFileMap = HashMap<String, RFile>()
            val playFileList = ArrayList<RFile>()
            val musicStateList = ArrayList<MusicState>()

            neteaseAppCache.rFiles.forEach {
                it.read2File { _, rfile ->
                    when (rfile.extension) {
                        NeteaseCacheProvider.playExt -> {
                            playFileList.add(rfile)
                        }
                        NeteaseCacheProvider.infoExt -> {
                            infoFileMap[rfile.nameWithoutExtension] = rfile
                        }
                    }
                }
            }

            withContext(Dispatchers.IO) {
                playFileList.forEach { rfile ->
                    launch {
                        runWithPrintTimeCostSuspend("paging", "RFile to MusicState") {
                            musicStateList.add(
                                rfile2MusicSate(
                                    neteaseAppCache,
                                    infoFileMap,
                                    playFileList,
                                    rfile
                                )
                            )
                        }

                    }

                }

                launch {
                    updateSongsInfo(musicStateList)
                }

            }

            val index = cacheDir.indexOf(neteaseAppCache)
            val prevKey =
                if (index == 0 || 0 > (index - 1))
                    null
                else
                    cacheDir[index - 1]
            val nextKey =
                if (index == cacheDir.lastIndex)
                    null
                else
                    cacheDir[index + 1]


            return LoadResult.Page(musicStateList, prevKey, nextKey)
        } catch (e: Exception) {
            return LoadResult.Error(e)
        }
    }


}