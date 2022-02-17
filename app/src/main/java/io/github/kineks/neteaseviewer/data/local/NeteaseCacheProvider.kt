package io.github.kineks.neteaseviewer.data.local

import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.google.gson.Gson
import io.github.kineks.neteaseviewer.toRFile
import kotlinx.coroutines.*
import java.io.File
import java.io.InputStream
import java.io.OutputStream

object NeteaseCacheProvider {

    // todo: 从应用配置读取而不是硬编码
    // todo: 修复 Android R 上无法访问 Android Data 的问题
    var cacheDir: List<NeteaseAppCache> = ArrayList<NeteaseAppCache>().apply {
        add(
            NeteaseAppCache(
                "Netease", listOf(
                    RFileType.ShareStorage.toRFile("/netease/cloudmusic/Cache/Music1"),
                    // 部分版本在Q或者R上会用这个路径
                    RFileType.AndroidData.toRFile("/com.netease.cloudmusic/cache/Music1")
                )
            )
        )
        add(
            NeteaseAppCache(
                "NeteaseLite", listOf(
                    RFileType.ShareStorage.toRFile("/netease/cloudmusiclite/Cache/Music1"),
                    RFileType.AndroidData.toRFile("/com.netease.cloudmusiclite/Cache/Music1")
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

    val musicDirectory =
        File(Environment.getExternalStorageDirectory().path + "/Music/NeteaseViewer/")


    private suspend fun getCacheFiles(neteaseAppCache: NeteaseAppCache): List<File> {
        val begin = System.currentTimeMillis()
        val files = ArrayList<File>()

        withContext(Dispatchers.IO) {

            neteaseAppCache.rFiles.forEach {
                var size = -1
                it.read2File { index, file ->
                    print(file.absolutePath)
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

    private suspend fun getCachesFiles(): List<File> {
        val begin = System.currentTimeMillis()
        val files = ArrayList<File>()

        withContext(Dispatchers.IO) {
            cacheDir.forEach {
                it.rFiles.forEach {
                    var size = -1
                    it.read2File { index, file ->
                        print(file.absolutePath)
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
        }

        val costTime = System.currentTimeMillis() - begin
        Log.d(this::javaClass.name, "加载文件列表耗时: ${costTime}ms")
        return files
    }

    suspend fun getCacheSongs(cacheDir: List<NeteaseAppCache> = this.cacheDir): ArrayList<Music> {
        val begin = System.currentTimeMillis()
        val songs = ArrayList<Music>()

        withContext(Dispatchers.IO) {
            val begin = System.currentTimeMillis()
            cacheDir.forEach { neteaseAppCache ->
                getCacheFiles(neteaseAppCache).forEach {
                    val begin = System.currentTimeMillis()

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
                                file = it,
                                song = null,
                                info = idx,
                                neteaseAppCache = neteaseAppCache
                            )
                        )
                    }

                    val costTime = System.currentTimeMillis() - begin
                    Log.d(this.javaClass.name, "加载单个耗时: ${costTime}ms")
                }
                val costTime = System.currentTimeMillis() - begin
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

    @OptIn(DelicateCoroutinesApi::class)
    fun decryptSongList(
        list: List<Music>,
        skipIncomplete: Boolean = true,
        skipMissingInfo: Boolean = true,
        callback: (out: Uri?, hasError: Boolean, e: Exception?) -> Unit = { _, _, _ -> }
    ) {
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                list.forEach {
                    if (skipIncomplete && it.incomplete) return@forEach
                    if (skipMissingInfo && it.info == null) return@forEach
                    it.decryptFile(callback)
                }
            }
        }
    }

    data class NeteaseAppCache(
        val type: String,
        val rFiles: List<RFile>
    )

    enum class RFileType(val type: String) {
        // 目录
        Uri("uri"),
        File("file"),
        // 单个文件
        SingleUri("single uri"),
        SingleFile("single file"),

        ShareStorage("share storage"),
        AndroidData("android data"),
        Root("root")
    }

    data class RFile(val type: RFileType, val path: String) {

        companion object {
            var androidData: Uri = Uri.EMPTY
        }
        val uri: Uri
            get() = when (type) {
                RFileType.Uri, RFileType.SingleUri -> Uri.parse(path)
                RFileType.ShareStorage ->
                    File(Environment.getExternalStorageDirectory().path + path).toUri()
                RFileType.File, RFileType.SingleFile -> File(path).toUri()
                RFileType.AndroidData -> TODO()
                RFileType.Root -> TODO()
            }
        val file: File
            get() = uri.toFile()
        val input : InputStream? get() = when(type) {
            RFileType.Uri, RFileType.File -> null
            RFileType.SingleUri ,RFileType.SingleFile ,RFileType.ShareStorage -> file.inputStream()
            RFileType.AndroidData -> null
            RFileType.Root -> TODO()
        }
        val output : OutputStream? get() =when(type) {
            RFileType.Uri, RFileType.File -> null
            RFileType.SingleUri ,RFileType.SingleFile ,RFileType.ShareStorage -> file.outputStream()
            RFileType.AndroidData -> null
            RFileType.Root -> TODO()
        }

        fun read2File(callback: (index: Int, file: File) -> Unit) {
            when (type) {
                RFileType.Uri -> {
                    uri
                        .toFile().walk().forEachIndexed { index, file ->
                            callback.invoke(index, file)
                        }
                }
                RFileType.File -> {
                    File(path).walk().forEachIndexed { index, file ->
                        callback.invoke(index, file)
                    }
                }
                RFileType.SingleUri -> callback.invoke(0, file)
                RFileType.SingleFile -> callback.invoke(0, file)
                RFileType.ShareStorage -> {
                    File(Environment.getExternalStorageDirectory().path + path)
                        .walk().forEachIndexed { index, file ->
                            callback.invoke(index, file)
                        }
                }
                RFileType.AndroidData -> {

                    /*val paths =
                        path.split("/").toTypedArray()
                    val stringBuilder =
                        StringBuilder("content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata/document/primary%3AAndroid%2Fdata")
                    for (p in paths) {
                        if (p.isEmpty()) continue
                        stringBuilder.append("%2F").append(p)
                    }*/
                    /*if (androidData == Uri.EMPTY) return
                    val uri = androidData//Uri.parse(stringBuilder.toString())
                    DocumentFile.fromTreeUri(App.context, uri)?.listFiles()
                        ?.forEachIndexed { index, documentFile ->
                            callback.invoke(
                                index,
                                File(URI.create(documentFile.uri.toString()))
                            )

                            print(documentFile.name)
                        }*/
                }
                RFileType.Root -> TODO()
            }
        }


    }
}