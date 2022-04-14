package io.github.kineks.neteaseviewer.data.local

import android.net.Uri
import android.os.Environment
import android.util.Log
import com.google.gson.Gson
import io.github.kineks.neteaseviewer.App
import io.github.kineks.neteaseviewer.data.local.cacheFile.CacheFileInfo
import io.github.kineks.neteaseviewer.data.local.cacheFile.FileType
import io.github.kineks.neteaseviewer.data.local.cacheFile.Music
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
                    RFile.RType.ShareStorage.toRFile("/netease/cloudmusic/Cache/Music1"),
                    // 部分修改版本会用这个路径
                    RFile.RType.AndroidData.toRFile("/com.netease.cloudmusic/cache/Music1")
                )
            )
        )
        add(
            NeteaseAppCache(
                "NeteaseLite", listOf(
                    RFile.RType.ShareStorage.toRFile("/netease/cloudmusiclite/Cache/Music1"),
                    RFile.RType.AndroidData.toRFile("/com.netease.cloudmusiclite/Cache/Music1")
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
    private val musicDirectory =
        File(
            Environment.getExternalStorageDirectory().path + "/"
                    + Environment.DIRECTORY_MUSIC + "/NeteaseViewer/"
        )


    suspend fun getCacheFiles(neteaseAppCache: NeteaseAppCache): List<File> {
        val begin = System.currentTimeMillis()
        val files = ArrayList<File>()

        withContext(Dispatchers.IO) {
            neteaseAppCache.rFiles.forEach {
                var size = -1
                it.read2File { index, file ->
                    if (file.extension == playExt) {
                        files.add(file)
                    }
                    size = index
                }
                size++
                Log.d(
                    this.javaClass.name,
                    "load file : ${it.type}://${it.path}  size : " + size//files.size
                )
            }
        }

        val costTime = System.currentTimeMillis() - begin
        Log.d(this::javaClass.name, "加载文件列表耗时: ${costTime}ms")
        return files
    }

    suspend fun getCacheSongs(cacheDir: List<NeteaseAppCache> = this.cacheDir): ArrayList<Music> {
        val begin = System.currentTimeMillis()
        val songs = ArrayList<Music>()

        withContext(Dispatchers.IO) {
            val begin1 = System.currentTimeMillis()
            cacheDir.forEach { neteaseAppCache ->
                getCacheFiles(neteaseAppCache).forEach {
                    val begin2 = System.currentTimeMillis()

                    if (fastReader ||
                        !File(it.parentFile, it.nameWithoutExtension + ".$infoExt").exists()
                    ) {
                        // 如果启用快速扫描则跳过读取idx文件
                        // 或者 *.idx! 文件并不存在(一般是 unlock netease music 导致)
                        it.nameWithoutExtension.split("-").let { str ->
                            songs.add(
                                Music(
                                    id = str[0].toInt(),
                                    name = str[2],
                                    artists = "N/A - ${str[0].toInt()}",
                                    bitrate = str[1].toInt(),
                                    md5 = str[2].substring(0, 31),
                                    song = null,
                                    file = it,
                                    info = null,
                                    neteaseAppCache = neteaseAppCache
                                )
                            )
                        }
                    } else {
                        val infoFile = File(it.parentFile, it.nameWithoutExtension + ".$infoExt")
                        val idx = gson.fromJson(infoFile.readText(), CacheFileInfo::class.java)

                        songs.add(
                            Music(
                                id = idx.id,
                                name = idx.fileMD5,
                                artists = "N/A",
                                bitrate = idx.bitrate,
                                md5 = idx.fileMD5,
                                file = it,
                                song = null,
                                info = idx,
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

    fun removeCacheFile(music: Music): Boolean {
        val infoFile = File(music.file.parentFile, music.file.nameWithoutExtension + ".$infoExt")
        return music.file.delete() || infoFile.delete()
    }

    suspend fun decryptCacheFile(
        music: Music,
        callback: (out: Uri?, hasError: Boolean, e: Exception?) -> Unit = { _, _, _ -> }
    ): Boolean {
        val begin = System.currentTimeMillis()

        var error = false
        var exception: Exception? = null

        var out: Uri? = null
        withContext(Dispatchers.IO) {

            music.run {

                try {
                    // 从输入流获取文件头判断,获取失败则默认 mp3
                    val ext = FileType.getFileType(inputStream) ?: "mp3"
                    val path =
                        if (App.isAndroidQorAbove)
                            App.context.cacheDir
                        else
                            musicDirectory
                    val file = File(path, "$displayFileName.$ext")
                    // 在 Android Q 之后的先放在私有目录, P 及以下的则直接写出
                    // 避免文件父目录不存在
                    path.mkdirs()

                    Log.d("Music", file.absolutePath)
                    inputStream.use { input ->
                        file.outputStream().use {
                            input.buffered().copyTo(it.buffered())
                        }
                    }

                    Log.d("Music", file.length().toString())
                    MediaStoreProvider.setInfo(this, file)
                    // 在 Android Q 之后的用 MediaStore 导出文件,然后清理私有目录的源副本文件
                    if (App.isAndroidQorAbove) {
                        out = MediaStoreProvider.insert2Music(
                            file.inputStream(),
                            this,
                            ext
                        )
                            ?: throw Exception("")
                        Log.d("Music", out.toString())
                        file.delete()
                    } else {
                        file.scanFile()
                    }

                    saved = true

                } catch (e: Exception) {
                    error = true
                    exception = e
                    Log.e("Music", e.message, e)
                }
            }

            val costTime = System.currentTimeMillis() - begin
            Log.d(this::javaClass.name, "导出文件耗时: ${costTime}ms")

            callback.invoke(out, error, exception)
        }

        return out != null
    }

    fun decryptSongList(
        list: List<Music>,
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
                    if (skipMissingInfo && music.info == null) return@forEachIndexed
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