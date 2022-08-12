package io.github.kineks.neteaseviewer.data.local

import android.net.Uri
import android.util.Log
import androidx.paging.PagingData
import androidx.paging.map
import com.google.gson.Gson
import io.github.kineks.neteaseviewer.App
import io.github.kineks.neteaseviewer.data.local.cacheFile.CacheFileInfo
import io.github.kineks.neteaseviewer.data.local.cacheFile.MusicState
import io.github.kineks.neteaseviewer.mutableListOf
import io.github.kineks.neteaseviewer.scanFile
import io.github.kineks.neteaseviewer.toRFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object NeteaseCacheProvider {

    // todo: 从应用配置读取而不是硬编码
    // todo: 优化 Android R 访问 Android Data 的性能问题
    val cacheDir: List<NeteaseAppCache> by lazy {
        listOf(
            NeteaseAppCache(
                "Netease", listOf(
                    RFile.RType.ShareStorage.toRFile("/netease/cloudmusic/cache/Music1/"),
                    // 部分修改版本会用这个路径
                    RFile.RType.AndroidData.toRFile("/com.netease.cloudmusic/cache/Music1/")
                )
            ),
            NeteaseAppCache(
                "NeteaseLite", listOf(
                    RFile.RType.ShareStorage.toRFile("/netease/cloudmusiclite/cache/Music1/"),
                    // 部分修改版本会用这个路径
                    RFile.RType.AndroidData.toRFile("/com.netease.cloudmusiclite/cache/Music1/")
                )
            )

        )
    }

    // Music File : *.UC!
    const val playExt = "uc!"

    // Music File Info : *.IDX!
    const val infoExt = "idx!"

    // 快速读取,跳过读取一些目前还不需要的信息
    var fastReader = !true
    val gson by lazy { Gson() }

    // 在 Android P 及以下的使用的导出路径
    @Suppress("DEPRECATION")
    private val musicDirectory by lazy {
        RFile.getExternalStorageDirectory("NeteaseViewer", RFile.DIRECTORY_MUSIC)
    }

    private const val TAG = "NeteaseCacheProvider"


    suspend fun getCacheFiles(neteaseAppCache: NeteaseAppCache): List<RFile> {
        val files = mutableListOf<RFile>()

        withContext(Dispatchers.IO) {
            neteaseAppCache.rFiles.forEach {
                launch {
                    var size = -1
                    it.read2File { index, rfile ->
                        launch {
                            if (rfile.extension == playExt) {
                                files.add(rfile)
                            }
                            size = index
                            Log.d(
                                TAG,
                                "load file : ${rfile.type}://${rfile.path}  size : " + size
                            )
                        }
                    }
                    size++
                    Log.d(
                        TAG,
                        "load file : ${it.type}://${it.path}  size : " + size
                    )
                }

            }
        }

        return files
    }

    /*suspend fun getCacheSongs(cacheDir: List<NeteaseAppCache> = this.cacheDir): MutableList<MusicState> {
        val songs = mutableListOf<MusicState>()

        runWithPrintTimeCostSuspend(TAG, "总加载耗时") {
            withContext(Dispatchers.IO) {

                runWithPrintTimeCostSuspend(TAG, "加载缓存信息耗时") {
                    cacheDir.forEach { neteaseAppCache ->

                        runWithPrintTimeCostSuspend(TAG, "加载文件列表耗时") {
                            getCacheFiles(neteaseAppCache).forEach {
                                launch {
                                    songs.add(getMusicState(it, neteaseAppCache))
                                }
                            }
                        }


                    }
                }


            }
        }

        return songs
    }*/

    /*private suspend fun getMusicState(file: RFile, neteaseAppCache: NeteaseAppCache): MusicState =
        withContext(Dispatchers.IO) {
            val begin2 = System.currentTimeMillis()
            val infoFile = when (fastReader) {
                true -> null
                false -> RFile.of(file.parentFile!!, file.nameWithoutExtension + ".$infoExt")
            }
            var id = -1
            var bitrate: Int = -1
            var md5 = ""
            var incomplete = false
            var missingInfo = true


            if (!fastReader && (infoFile != null) && infoFile.exists()) {
                val idx = gson.fromJson(infoFile.readText(), CacheFileInfo::class.java)
                if (idx != null) {
                    id = idx.id
                    bitrate = idx.bitrate
                    md5 = idx.fileMD5
                    // 判断缓存文件和缓存文件信息中的文件长度大小是否一致
                    incomplete = idx.fileSize != file.length()
                    missingInfo = false
                }
            }


            // 如果数据依旧没初始化
            if (id == -1) {
                // 如果启用快速扫描则跳过读取idx文件
                // 或者 *.idx! 文件并不存在(一般是 unlock netease music 导致)
                file.nameWithoutExtension.split("-").let { str ->
                    id = str[0].toInt()
                    bitrate = str[1].toInt()
                    md5 = str[2].substring(0, 31)

                }
            }

            val costTime = System.currentTimeMillis() - begin2
            Log.d(TAG, "加载单个耗时: ${costTime}ms")

            return@withContext MusicState(
                id = id,
                bitrate = bitrate,
                md5 = md5,
                file = file,
                song = NeteaseDataService.instance.getSongFromCache(id),
                incomplete = incomplete,
                missingInfo = missingInfo,
                neteaseAppCache = neteaseAppCache
            )
        }*/


    fun getCacheFileInfo(musicState: MusicState): CacheFileInfo? {
        val infoFile = RFile.of(
            musicState.file.parentFile!!,
            musicState.file.nameWithoutExtension + ".$infoExt"
        )
        return if (infoFile != null && infoFile.exists()) getCacheFileInfo(infoFile) else null
    }

    private fun getCacheFileInfo(infoFile: RFile): CacheFileInfo? =
        gson.fromJson(infoFile.readText(), CacheFileInfo::class.java)


    fun removeCacheFile(musicState: MusicState): Boolean {
        val infoFile =
            RFile.of(
                musicState.file.parentFile!!,
                musicState.file.nameWithoutExtension + ".$infoExt"
            )
        return musicState.file.delete() || infoFile?.delete() == true
    }

    suspend fun decryptCacheFile(
        musicState: MusicState,
        callback: (out: Uri?, hasError: Boolean, e: Exception?) -> Unit = { _, _, _ -> }
    ): Boolean {

        var error = false
        var exception: Exception? = null

        var out: Uri? = null

        withContext(Dispatchers.IO) {
            musicState.run {

                try {
                    // 从输入流获取文件头判断,获取失败则默认 mp3
                    //val ext = FileType.getFileType(inputStream) ?: "mp3"
                    val path =
                        if (App.isAndroidQorAbove)
                            App.context.cacheDir.toRFile()
                        else
                            musicDirectory
                    val file = RFile.of(path, "$displayFileName.${musicState.ext}")!!
                    // 在 Android Q 之后的先放在私有目录, P 及以下的则直接写出
                    // 避免写出文件 父目录 不存在
                    path.mkdirs()

                    Log.d(TAG, file.type.name + "://" + file.path)
                    if (file.output == null) throw Exception("output == null")

                    inputStream.use { input ->
                        file.output.use {
                            input.buffered().copyTo(it!!.buffered())
                        }
                    }

                    Log.d(TAG, file.length().toString())
                    MediaStoreProvider.setInfo(this, file)
                    // 在 Android Q 之后的用 MediaStore 导出文件,然后清理私有目录的源缓存文件
                    if (App.isAndroidQorAbove) {
                        out = MediaStoreProvider.insert2Music(
                            file.input!!,
                            this,
                            ext
                        )
                            ?: throw Exception("")
                        Log.d(TAG, out.toString())
                        file.delete()
                    } else {
                        file.file.scanFile()
                    }

                    saved = true

                } catch (e: Exception) {
                    error = true
                    exception = e
                    Log.e(TAG, e.message + " / " + file.path, e)
                }
            }

            callback.invoke(out, error, exception)
        }

        return out != null
    }

    fun decryptSongList(
        flow: Flow<PagingData<MusicState>>,
        skipIncomplete: Boolean = true,
        skipMissingInfo: Boolean = true,
        callback: (out: Uri?, hasError: Boolean, e: Exception?) -> Unit = { _, _, _ -> },
        isLastOne: (Boolean) -> Unit = {},
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.IO) {
                val list = flow.toList()
                list.forEachIndexed { index, pagingData ->
                    launch launch1@{
                        pagingData.map { music ->
                            if (index == list.lastIndex) isLastOne.invoke(true)
                            if (skipIncomplete && music.incomplete) return@map
                            if (skipMissingInfo && music.missingInfo) return@map
                            music.decryptFile(callback)
                        }
                    }
                }
            }
        }
    }

    fun decryptSongList(
        list: List<MusicState>,
        skipIncomplete: Boolean = true,
        skipMissingInfo: Boolean = true,
        callback: (out: Uri?, hasError: Boolean, e: Exception?) -> Unit = { _, _, _ -> },
        isLastOne: (Boolean) -> Unit = {},
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.IO) {
                list.forEachIndexed { index, music ->
                    launch launch1@{
                        if (index == list.lastIndex) isLastOne.invoke(true)
                        if (skipIncomplete && music.incomplete) return@launch1
                        if (skipMissingInfo && music.missingInfo) return@launch1
                        music.decryptFile(callback)
                    }
                }
            }
        }
    }

    data class NeteaseAppCache(
        val type: String,
        val rFiles: List<RFile>
    )

}