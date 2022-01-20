package io.github.kineks.neteaseviewer.data.local

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.kineks.neteaseviewer.data.api.Song
import io.github.kineks.neteaseviewer.data.player.XorByteInputStream
import io.github.kineks.neteaseviewer.filterIllegalPathChar
import io.github.kineks.neteaseviewer.scanFile
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
    val info: CacheFileInfo? = null
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
        get() =
            ("$artists - $name." + displayBitrate.replace(" ", ""))
                .filterIllegalPathChar()

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

    suspend fun decryptFile(): Boolean {
        val begin = System.currentTimeMillis()
        val out = NeteaseCacheProvider
            .getMusicFile(displayFileName + "." + FileType.getFileType(XorByteInputStream(file)))
        Log.d("Music", "导出路径 : " + out.path)
        NeteaseCacheProvider.decryptFile(
            file,
            out
        )
        saved = true
        AudioInfoEdit.setInfo(this@Music, out)
        out.scanFile()
        val costTime = System.currentTimeMillis() - begin
        Log.d(this::javaClass.name, "导出文件耗时: ${costTime}ms")

        return out.exists()
    }

    fun delete() = NeteaseCacheProvider.removeCacheFile(this).apply {
        deleted = this
    }

    val inputStream get() = XorByteInputStream(file)

}
