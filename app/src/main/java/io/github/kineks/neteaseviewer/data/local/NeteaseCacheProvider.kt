package io.github.kineks.neteaseviewer.data.local

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.experimental.xor

object NeteaseCacheProvider {

    // todo: 从应用配置读取而不是硬编码
    var cacheDir: List<File> = ArrayList<File>().apply {
        add(File("/storage/self/primary/netease/cloudmusic/Cache/Music1"))
        add(File("/storage/self/primary/netease/cloudmusiclite/Cache/Music1"))
    }

    // Music File : *.UC!
    val playExt = "uc!"

    // Music File Info : *.IDX!
    val infoExt = "idx!"

    // 快速读取,跳过读取一些目前还不需要的信息
    var fastReader = !true
    val gson by lazy { Gson() }

    suspend fun getCacheFiles(): List<File> {
        val begin = System.currentTimeMillis()
        val files = ArrayList<File>()

        withContext(Dispatchers.IO) {
            cacheDir.forEach {
                it.walk().forEach { file ->
                    if (file.extension == playExt) {
                        files.add(file)
                    }
                }
                //files.addAll(it.listFiles() ?: emptyArray())
                Log.d(
                    this.javaClass.name,
                    "load file : " + it.absoluteFile + "  size : " + files.size
                )
            }
        }

        val costTime = System.currentTimeMillis() - begin
        Log.d(this::javaClass.name, "加载文件列表耗时: ${costTime}ms")
        return files
    }

    suspend fun getCacheSongs(): ArrayList<Music> {
        val begin = System.currentTimeMillis()
        val songs = ArrayList<Music>()

        withContext(Dispatchers.IO) {
            val begin = System.currentTimeMillis()
            getCacheFiles().forEach {
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
                                info = null
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
                            null,
                            file = it,
                            info = idx
                        )
                    )
                }

                val costTime = System.currentTimeMillis() - begin
                Log.d(this.javaClass.name, "加载单个耗时: ${costTime}ms")
            }
            val costTime = System.currentTimeMillis() - begin
            Log.d(this.javaClass.name, "加载缓存信息耗时: ${costTime}ms")
        }

        val costTime = System.currentTimeMillis() - begin
        Log.d(this.javaClass.name, "总加载耗时: ${costTime}ms")
        return songs
    }

    suspend fun decryptFile(byteArray: ByteArray, key: Byte = -93): ByteArray =
        withContext(Dispatchers.IO) {
            val byteArrayOut = ByteArray(byteArray.size)
            for ((index, byte) in byteArray.withIndex()) {
                byteArrayOut[index] = byte.xor(key)
            }
            return@withContext byteArrayOut
        }

}