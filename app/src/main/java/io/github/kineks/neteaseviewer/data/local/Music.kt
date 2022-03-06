package io.github.kineks.neteaseviewer.data.local

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.kineks.neteaseviewer.App
import io.github.kineks.neteaseviewer.data.api.Song
import io.github.kineks.neteaseviewer.data.player.XorByteInputStream
import io.github.kineks.neteaseviewer.replaceIllegalChar
import io.github.kineks.neteaseviewer.scanFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

val EmptyMusic = Music(-1, "Name", "N/A", -1, File(""))
const val EmptyAlbum = "N/A"

data class Music(
    val id: Int,
    val name: String,
    val artists: String,
    val bitrate: Int = -1,
    val file: File,
    val song: Song? = null,
    val info: CacheFileInfo? = null,
    val neteaseAppCache: NeteaseCacheProvider.NeteaseAppCache? = null
) {
    val album get() = song?.album?.name ?: "$EmptyAlbum $id"
    val track get() = song?.no ?: -1
    val year: String
        get() =
            if (song == null) "N/A" else
                SimpleDateFormat("yyyy", Locale.US).format(Date(song.album.publishTime))
    val disc get() = song?.disc ?: ""


    var deleted by mutableStateOf(false)
    var saved by mutableStateOf(false)
    val incomplete =
        when (info) {
            null -> false
            else -> {
                // 判断缓存文件和缓存文件信息中的文件长度大小是否一致
                info.fileSize != file.length()
            }
        }

    val displayFileName
        get() = when (bitrate) {
            999000 -> "$artists - $name".replaceIllegalChar()
            else -> ("$artists - $name .$displayBitrate")
                .replaceIllegalChar()
        }


    val displayBitrate = when (bitrate) {
        1000 -> {
            "N/A kbps"
        }
        else -> "${bitrate / 1000} kbps"
    }

    fun getAlbumPicUrl(width: Int = -1, height: Int = -1): String? {
        if (song?.album?.picUrl != null) {
            // api 如果不同时限定宽高参数就会默认返回原图
            if (width != -1 && height != -1) {
                return song.album.picUrl + "?param=${width}y$height"
            }
            return song.album.picUrl
        }
        return null
    }

    suspend fun decryptFile(
        callback: (out: Uri?, hasError: Boolean, e: Exception?) -> Unit = { _, _, _ -> }
    ): Boolean {
        val begin = System.currentTimeMillis()

        var error: Boolean = false
        var exception: Exception? = null
        /*
        saved = true// 标记文件导出过,但不保证已经成功导出
        //error = !out.exists()// 文件不存在就说明导出失败
         */

        var out: Uri? = null
        withContext(Dispatchers.IO) {

            try {

                val ext = FileType.getFileType(inputStream) ?: "mp3"
                val path =
                    if (App.isAndroidQorAbove)
                        App.context.cacheDir
                    else
                        NeteaseCacheProvider.musicDirectory
                val file = File(path, "$displayFileName.$ext")

                Log.d("Music", file.absolutePath)
                inputStream.use { input ->
                    file.outputStream().use {
                        input.buffered().copyTo(it.buffered())
                    }
                }

                Log.d("Music", file.length().toString())
                MediaStoreProvider.setInfo(this@Music, file)
                if (App.isAndroidQorAbove) {
                    out = MediaStoreProvider.insert2Music(
                        file.inputStream(),
                        this@Music,
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

            val costTime = System.currentTimeMillis() - begin
            Log.d(this::javaClass.name, "导出文件耗时: ${costTime}ms")

            callback.invoke(out, error, exception)
        }

        return out != null
    }

    fun delete() = NeteaseCacheProvider.removeCacheFile(this).apply {
        deleted = this
    }

    val inputStream get() = XorByteInputStream(file)

}
