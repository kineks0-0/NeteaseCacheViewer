package io.github.kineks.neteaseviewer.data.local.cacheFile

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.kineks.neteaseviewer.data.local.NeteaseCacheProvider
import io.github.kineks.neteaseviewer.data.network.Song
import io.github.kineks.neteaseviewer.data.network.service.NeteaseDataService
import io.github.kineks.neteaseviewer.data.player.XorByteInputStream
import io.github.kineks.neteaseviewer.getArtists
import io.github.kineks.neteaseviewer.replaceIllegalChar
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

val EmptyMusicState = MusicState(-1, -1, "", File(""))
const val EmptyAlbum = "N/A"
const val EmptyArtists = "N/A"

data class MusicState(
    val id: Int,
    val bitrate: Int = -1,
    val md5: String,
    val file: File,
    val song: Song? = null,

    // 歌名
    val name: String = when (song) {
        null -> md5
        else -> song.name ?: song.lMusic.name
    },

    // 歌手
    val artists: String = when (song) {
        null -> EmptyArtists
        else -> song.artists.getArtists()
    },

    // 专辑
    val album: String = song?.album?.name ?: "$EmptyAlbum $id",
    // 专辑缩略图
    val smallAlbumArt: String? = when (song) {
        null -> ""
        else -> NeteaseDataService.instance.getAlbumPicUrl(id, 80, 80)
    },

    // 缓存损坏
    val incomplete: Boolean = false,
    // 丢失 idx 文件
    val missingInfo: Boolean = true,

    val neteaseAppCache: NeteaseCacheProvider.NeteaseAppCache? = null
) {

    val track get() = song?.no ?: -1
    val disc get() = song?.disc ?: ""
    val year: String
        get() =
            if (song == null) "N/A" else
                SimpleDateFormat("yyyy", Locale.US).format(Date(song.album.publishTime ?: 0))


    var deleted by mutableStateOf(false)
    var saved by mutableStateOf(false)

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

    fun delete() = NeteaseCacheProvider.removeCacheFile(this).apply {
        deleted = this
    }

    val inputStream get() = XorByteInputStream(file)

    // 包装调用函数
    fun getAlbumPicUrl(width: Int = -1, height: Int = -1): String? =
        NeteaseDataService.instance.getAlbumPicUrl(id, width, height)

    suspend fun decryptFile(
        callback: (out: Uri?, hasError: Boolean, e: Exception?) -> Unit = { _, _, _ -> }
    ): Boolean = NeteaseCacheProvider.decryptCacheFile(this, callback)

    // 主要避免无效拷贝
    fun reload(_song: Song?): MusicState {
        if (_song == null) return this
        if (_song.name == name) return this
        // kotlin 的 copy 只是替换参数,并不会重新走类初始化的构建函数
        // 所以需要自己替换更新参数
        return copy(
            song = _song,
            name = when (_song) {
                null -> md5
                else -> _song.name ?: _song.lMusic.name
            },
            artists = when (_song) {
                null -> EmptyArtists
                else -> _song.artists.getArtists()
            },
            album = _song.album.name ?: "$EmptyAlbum $id",
            smallAlbumArt = when (_song) {
                null -> ""
                else -> NeteaseDataService.instance.getAlbumPicUrl(id, 80, 80)
            }
        )

    }
}
