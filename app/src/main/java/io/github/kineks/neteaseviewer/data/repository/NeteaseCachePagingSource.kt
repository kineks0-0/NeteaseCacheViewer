package io.github.kineks.neteaseviewer.data.repository

import android.util.Log
import io.github.kineks.neteaseviewer.data.local.NeteaseCacheProvider
import io.github.kineks.neteaseviewer.data.local.RFile
import io.github.kineks.neteaseviewer.data.local.cacheFile.CacheFileInfo
import io.github.kineks.neteaseviewer.data.local.cacheFile.MusicState
import io.github.kineks.neteaseviewer.data.network.service.NeteaseDataService
import io.github.kineks.neteaseviewer.runWithPrintTimeCostSuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "Paging 3"

class NeteaseCachePagingSource(
    private val cacheDir: List<NeteaseCacheProvider.NeteaseAppCache> = NeteaseCacheProvider.cacheDir
) {

    suspend fun rfile2MusicSate(
        neteaseAppCache: NeteaseCacheProvider.NeteaseAppCache,
        infoFileMap: HashMap<String, RFile>,
        rfile: RFile
    ): MusicState {
        val infoFile = when (NeteaseCacheProvider.fastReader) {
            true -> null
            false -> infoFileMap[rfile.nameWithoutExtension]
        }
        var id = -1
        var bitrate: Int = -1
        var duration: Long = -1
        var fileSize: Long = -1
        var md5 = ""
        var incomplete = false
        var missingInfo = true


        if ((infoFile != null) && infoFile.exists()) {
            val idx =
                NeteaseCacheProvider.gson.fromJson(infoFile.readText(), CacheFileInfo::class.java)
            if (idx != null) {
                id = idx.id
                bitrate = idx.bitrate
                md5 = idx.fileMD5
                // 判断缓存文件和缓存文件信息中的文件长度大小是否一致
                duration = idx.duration
                fileSize = idx.fileSize
                incomplete = idx.fileSize != rfile.length()
                missingInfo = false
            }
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

        return when (val song = NeteaseDataService.instance.getSongFromCache(id)) {
            null ->
                MusicState(
                    id = id,
                    rawBitrate = bitrate,
                    duration = duration,
                    fileSize = fileSize,
                    md5 = md5,
                    file = rfile,
                    incomplete = incomplete,
                    missingInfo = missingInfo,
                    neteaseAppCache = neteaseAppCache
                )
            else -> MusicState.get(
                id = id,
                rawBitrate = bitrate,
                duration = duration,
                fileSize = fileSize,
                md5 = md5,
                file = rfile,
                song = song,
                incomplete = incomplete,
                missingInfo = missingInfo,
                neteaseAppCache = neteaseAppCache
            )
        }
    }

    suspend fun updateSongsInfo(
        songs: MutableList<MusicState>,
        index: Int = -1,
        quantity: Int = 50
    ) {

        // 如果列表为空
        if (songs.isEmpty()) return

        // 计算分页数量
        var pages = songs.size / quantity - 1
        if (songs.size % quantity != 0)
            pages++
        val pi = if (index == -1) 0..pages else index..index

        for (i in pi) {
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
                        val offsetIndex = offset + it
                        //Log.d(this@MainViewModel::javaClass.name, "update:$index")
                        // 如果缓存里有则跳过
                        if (songs[offsetIndex].track == -1) {
                            val id = songs[offsetIndex].id
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

    private suspend fun RFile.toNeteaseRawData(
        infoFileMap: HashMap<String, RFile>,
        playFileList: ArrayList<RFile>
    ): Pair<ArrayList<RFile>, HashMap<String, RFile>> {
        withContext(Dispatchers.IO) {
            read2File { _, rfile ->
                launch {
                    runWithPrintTimeCostSuspend(
                        TAG,
                        "${playFileList.size} rfile: ${rfile.name}"
                    ) {
                        when (rfile.extension) {
                            NeteaseCacheProvider.playExt -> {
                                playFileList.add(rfile)
                            }
                            NeteaseCacheProvider.infoExt -> {
                                infoFileMap[rfile.nameWithoutExtension] = rfile
                            }
                            else -> {
                                Log.e(TAG, "Unknown RFile Ext: " + rfile.extension)
                            }
                        }
                    }

                }

            }
        }
        return Pair(playFileList, infoFileMap)
    }


    private suspend fun List<RFile>.toMusicState(
        neteaseAppCache: NeteaseCacheProvider.NeteaseAppCache,
        infoFileMap: HashMap<String, RFile>
    ): ArrayList<MusicState> {
        val playFileList = this
        val musicStateList = ArrayList<MusicState>(playFileList.size + 2)
        withContext(Dispatchers.Default) {
            playFileList.forEachIndexed { index, rfile ->
                launch {
                    runWithPrintTimeCostSuspend(
                        TAG,
                        "${playFileList.size} RFile to MusicState ${rfile.name}"
                    ) {
                        musicStateList.add(
                            rfile2MusicSate(
                                neteaseAppCache,
                                infoFileMap,
                                rfile
                            )
                        )
                    }

                }
            }
        }


        Log.d(TAG, "musicStateList :  ${musicStateList.size}")
        return musicStateList

    }

    suspend fun load(): MutableList<MusicState> {
        val result = mutableListOf<MusicState>()
        coroutineScope {
            launch {
                cacheDir.forEachIndexed { index, _ ->
                    result.addAll(load(index))
                }
            }
        }
        return result
    }

    suspend fun load(index: Int): ArrayList<MusicState> {
        val result = ArrayList<MusicState>()

        try {
            withContext(Dispatchers.IO) {

                val neteaseAppCache = cacheDir[index]
                Log.d(TAG, "neteaseAppCache : " + neteaseAppCache.type)
                val infoFileMap = HashMap<String, RFile>()
                val playFileList = ArrayList<RFile>()
                val musicStateList = ArrayList<MusicState>()

                neteaseAppCache.rFiles.forEach {
                    runWithPrintTimeCostSuspend(
                        TAG,
                        neteaseAppCache.type + " ： 文件检索总耗时"
                    ) {
                        it.toNeteaseRawData(infoFileMap, playFileList)
                    }
                }


                musicStateList.addAll(
                    runWithPrintTimeCostSuspend(
                        TAG,
                        neteaseAppCache.type + " ： 读取缓存信息总耗时"
                    ) {
                        playFileList.toMusicState(
                            neteaseAppCache = neteaseAppCache,
                            infoFileMap = infoFileMap
                        )
                    }

                )



                Log.d(TAG, "musicStateList : " + musicStateList.size)
                Log.d(TAG, "playFileList : " + playFileList.size)
                Log.d(TAG, "infoFileMap : " + infoFileMap.size)

                playFileList.clear()
                infoFileMap.clear()

                result.addAll(musicStateList)

            }

        } catch (e: Exception) {
            Log.e(TAG, e.message, e)
        }

        return result
    }


}