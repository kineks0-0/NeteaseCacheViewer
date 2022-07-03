package io.github.kineks.neteaseviewer.data.local

import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.net.toUri
import com.google.gson.Gson
import io.github.kineks.neteaseviewer.App
import io.github.kineks.neteaseviewer.data.local.cacheFile.CacheFileInfo
import io.github.kineks.neteaseviewer.data.local.cacheFile.FileType
import io.github.kineks.neteaseviewer.data.local.cacheFile.MusicState
import io.github.kineks.neteaseviewer.data.network.service.NeteaseDataService
import io.github.kineks.neteaseviewer.scanFile
import io.github.kineks.neteaseviewer.toRFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

object NeteaseCacheProvider {

    // todo: 从应用配置读取而不是硬编码
    // todo: 修复 Android R 上无法访问 Android Data 的问题
    val cacheDir: List<NeteaseAppCache> = ArrayList<NeteaseAppCache>().apply {
        add(
            NeteaseAppCache(
                "Netease", listOf(
                    RFile.RType.ShareStorage.toRFile("/netease/cloudmusic/Cache/Music1/"),
                    // 部分修改版本会用这个路径
                    RFile.RType.AndroidData.toRFile("/com.netease.cloudmusic/cache/Music1/")
                )
            )
        )
        add(
            NeteaseAppCache(
                "NeteaseLite", listOf(
                    RFile.RType.ShareStorage.toRFile("/netease/cloudmusiclite/Cache/Music1/"),
                    // 部分修改版本会用这个路径
                    RFile.RType.AndroidData.toRFile("/com.netease.cloudmusiclite/Cache/Music1/")
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
    private val gson by lazy { Gson() }

    // 在 Android P 及以下的使用的导出路径
    @Suppress("DEPRECATION")
    private val musicDirectory =
        try {
            File(
                Environment.getExternalStorageDirectory().path + "/"
                        + Environment.DIRECTORY_MUSIC + "/NeteaseViewer/"
            ).toRFile()
        } catch (e: Exception) {
            File("../test/").toRFile()
        }


    suspend fun getCacheFiles(neteaseAppCache: NeteaseAppCache): List<RFile> {
        val begin = System.currentTimeMillis()
        val files = ArrayList<RFile>()

        withContext(Dispatchers.IO) {
            neteaseAppCache.rFiles.forEach {
                var size = -1
                it.read2File { index, rfile ->
                    if (rfile.extension == playExt) {
                        files.add(rfile)
                    }
                    size = index
                    Log.d(
                        this.javaClass.name,
                        "load file : ${rfile.type}://${rfile.path}  size : " + size
                    )
                }
                size++
                Log.d(
                    this.javaClass.name,
                    "load file : ${it.type}://${it.path}  size : " + size
                )
            }
        }

        val costTime = System.currentTimeMillis() - begin
        Log.d(this::javaClass.name, "加载文件列表耗时: ${costTime}ms")
        return files
    }

    suspend fun getCacheSongs(uri: String): MusicState =
        getCacheSongs(
            cacheDir = listOf(
                NeteaseAppCache(
                    "SingleUri", listOf(
                        uri.toUri().toSingleRFile()
                    )
                )
            )
        )[0]

    suspend fun getCacheSongs(cacheDir: List<NeteaseAppCache> = this.cacheDir): ArrayList<MusicState> {
        val begin = System.currentTimeMillis()
        val songs = ArrayList<MusicState>()

        withContext(Dispatchers.IO) {
            val begin1 = System.currentTimeMillis()
            cacheDir.forEach { neteaseAppCache ->
                getCacheFiles(neteaseAppCache).forEach {
                    val begin2 = System.currentTimeMillis()
                    val infoFile = when (fastReader) {
                        true -> RFile.of(type = RFile.RType.File, "", "")
                        false -> RFile.of(it.parentFile!!, it.nameWithoutExtension + ".$infoExt")
                    }

                    if (fastReader || infoFile == null || !infoFile.exists()) {
                        // 如果启用快速扫描则跳过读取idx文件
                        // 或者 *.idx! 文件并不存在(一般是 unlock netease music 导致)
                        it.nameWithoutExtension.split("-").let { str ->
                            songs.add(
                                MusicState(
                                    id = str[0].toInt(),
                                    bitrate = str[1].toInt(),
                                    md5 = str[2].substring(0, 31),
                                    file = it,
                                    song = NeteaseDataService.instance.getSongFromCache(str[0].toInt()),
                                    incomplete = false,
                                    missingInfo = true,
                                    neteaseAppCache = neteaseAppCache
                                )
                            )
                        }
                    } else {
                        val idx = gson.fromJson(infoFile.readText(), CacheFileInfo::class.java)

                        songs.add(
                            MusicState(
                                id = idx.id,
                                bitrate = idx.bitrate,
                                md5 = idx.fileMD5,
                                file = it,
                                song = NeteaseDataService.instance.getSongFromCache(idx.id),
                                incomplete = when (idx) {
                                    null -> false
                                    else -> {
                                        // 判断缓存文件和缓存文件信息中的文件长度大小是否一致
                                        idx.fileSize != it.length()
                                    }
                                },
                                missingInfo = false,
                                neteaseAppCache = neteaseAppCache
                            )
                        )
                    }

                    val costTime = System.currentTimeMillis() - begin2
                    Log.d(this.javaClass.name, "加载单个耗时: ${costTime}ms")
                }
                val costTime = System.currentTimeMillis() - begin1
                Log.d(this.javaClass.name, "加载缓存信息耗时: ${costTime}ms")
            }
        }

        val costTime = System.currentTimeMillis() - begin
        Log.d(this.javaClass.name, "总加载耗时: ${costTime}ms")
        return songs
    }

    fun getCacheFileInfo(musicState: MusicState): CacheFileInfo? {
        val infoFile = RFile.of(
            musicState.file.parentFile!!,
            musicState.file.nameWithoutExtension + ".$infoExt"
        )
        return if (infoFile != null) getCacheFileInfo(infoFile) else null
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
        val begin = System.currentTimeMillis()

        var error = false
        var exception: Exception? = null

        var out: Uri? = null
        withContext(Dispatchers.IO) {

            musicState.run {

                try {
                    // 从输入流获取文件头判断,获取失败则默认 mp3
                    val ext = FileType.getFileType(inputStream) ?: "mp3"
                    val path =
                        if (App.isAndroidQorAbove)
                            App.context.cacheDir.toRFile()
                        else
                            musicDirectory
                    val file = RFile.of(path, "$displayFileName.$ext")!!
                    // 在 Android Q 之后的先放在私有目录, P 及以下的则直接写出
                    // 避免文件父目录不存在
                    path.mkdirs()

                    Log.d("Music", file.type.name + "://" + file.path)
                    if (file.output == null) throw Exception("output == null")

                    inputStream.use { input ->
                        file.output.use {
                            input.buffered().copyTo(it!!.buffered())
                        }
                    }

                    Log.d("Music", file.length().toString())
                    MediaStoreProvider.setInfo(this, file)
                    // 在 Android Q 之后的用 MediaStore 导出文件,然后清理私有目录的源副本文件
                    if (App.isAndroidQorAbove) {
                        out = MediaStoreProvider.insert2Music(
                            file.input!!,
                            this,
                            ext
                        )
                            ?: throw Exception("")
                        Log.d("Music", out.toString())
                        file.delete()
                    } else {
                        file.file.scanFile()
                    }

                    saved = true

                } catch (e: Exception) {
                    error = true
                    exception = e
                    Log.e("Music", e.message + " / " + file.path, e)
                }
            }

            val costTime = System.currentTimeMillis() - begin
            Log.d(this::javaClass.name, "导出文件耗时: ${costTime}ms")

            callback.invoke(out, error, exception)
        }

        return out != null
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
                    if (index == list.lastIndex) isLastOne.invoke(true)
                    if (skipIncomplete && music.incomplete) return@forEachIndexed
                    if (skipMissingInfo && music.missingInfo) return@forEachIndexed
                    music.decryptFile(callback)
                }
            }
        }
    }

    data class NeteaseAppCache(
        val type: String,
        val rFiles: List<RFile>
    )

}