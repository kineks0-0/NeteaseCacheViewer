package io.github.kineks.neteaseviewer.data.local.cacheFile

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.kineks.neteaseviewer.data.local.NeteaseCacheProvider
import io.github.kineks.neteaseviewer.data.network.Song
import io.github.kineks.neteaseviewer.data.player.XorByteInputStream
import io.github.kineks.neteaseviewer.replaceIllegalChar
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

val EmptyMusic = Music(-1, "Name", "N/A", -1, "", File(""))
const val EmptyAlbum = "N/A"

data class Music(
    val id: Int,
    val name: String,
    val artists: String,
    val bitrate: Int = -1,
    val md5: String,
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

    val displayFileName: String
        get() {
            var name = "$artists - $name"
            // 避免极端情况导致文件名过长
            if (name.length > 80)
                name = if (this.name.length > 80)
                    this.name.substring(0, 80)
                else
                    this.name

            // 无损文件不需要添加比特率避免重名
            if (bitrate != 999000)
                name += " .${displayBitrate.replace(" ", "")}"
            return name.replaceIllegalChar()
        }


    val displayBitrate by lazy {
        when (bitrate) {
            1000 -> {
                "trial"//试听
            }
            999000 -> {
                "flac"//无损
            }
            else -> "${bitrate / 1000} k"
        }
    }

    val smallAlbumArt by lazy { getAlbumPicUrl(80, 80) }

    fun getAlbumPicUrl(width: Int = -1, height: Int = -1): String? {
        Log.d("Music", "$id - $width $height")
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
    ): Boolean = NeteaseCacheProvider.decryptCacheFile(this, callback)

    fun delete() = NeteaseCacheProvider.removeCacheFile(this).apply {
        deleted = this
    }

    val inputStream get() = XorByteInputStream(file)

}
