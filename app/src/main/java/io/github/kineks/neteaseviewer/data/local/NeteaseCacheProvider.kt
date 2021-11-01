package io.github.kineks.neteaseviewer.data.local

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object NeteaseCacheProvider {

    var cacheDir : List<File> = ArrayList<File>().apply {
        add(File("/storage/self/primary/netease/cloudmusic/Cache/Music1"))
        add(File("/storage/self/primary/netease/cloudmusiclite/Cache/Music1"))
    }

    // Music File : UC!
    val playExt = "uc!"
    // Music File Info : IDX!
    val infoExt = "idx!"

    // 快速读取,跳过读取一些目前还不需要的信息
    var fastReader = true
    val gson by lazy { Gson() }

    suspend fun getCacheFiles() : List<File> {
        val files = ArrayList<File>()

        withContext(Dispatchers.IO) {
            cacheDir.forEach {
                it.walk().forEach { file ->
                    if (file.extension == playExt) {
                        files.add(file)
                    }
                }
                //files.addAll(it.listFiles() ?: emptyArray())
                Log.d(this.javaClass.name,"load file : " + it.absoluteFile + "  size : " + files.size)
            }
        }

        return files
    }

    suspend fun getCacheSongs() : MutableList<Music> {
        val songs = ArrayList<Music>()

        withContext(Dispatchers.IO) {
            getCacheFiles().forEach {
                val id: Int
                val name: String
                var artist = "N/A"
                val bitrate: Int
                var info: CacheFileInfo? = null

                if (fastReader) {
                    val str = it.nameWithoutExtension.split("-")
                    id = str[0].toInt()
                    bitrate = str[1].toInt()
                    name = str[2]
                    artist = "N/A - $id"

                } else {
                    val infoFile = File(it.parentFile,it.nameWithoutExtension + ".$infoExt")
                    val idx = gson.fromJson(infoFile.readText(),CacheFileInfo::class.java)
                    id = idx.id
                    name = idx.fileMD5
                    bitrate = idx.bitrate
                    info = idx
                }

                songs.add(Music(id,name,artist,bitrate,null,it,info))
            }
        }

        return songs
    }

}