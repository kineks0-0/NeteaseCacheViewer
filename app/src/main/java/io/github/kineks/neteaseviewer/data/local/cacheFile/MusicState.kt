package io.github.kineks.neteaseviewer.data.local.cacheFile

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.kineks.neteaseviewer.data.local.NeteaseCacheProvider
import io.github.kineks.neteaseviewer.data.local.RFile
import io.github.kineks.neteaseviewer.data.local.toRFile
import io.github.kineks.neteaseviewer.data.network.Song
import io.github.kineks.neteaseviewer.data.network.service.NeteaseDataService
import io.github.kineks.neteaseviewer.data.player.exoplayer.XorByteInputStream
import io.github.kineks.neteaseviewer.getArtists
import io.github.kineks.neteaseviewer.replaceIllegalChar
import io.github.kineks.neteaseviewer.runWithPrintTimeCostSuspend
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

const val EmptyName = "N/A"
const val EmptyAlbum = "N/A"
const val EmptyArtists = "N/A"
const val TAG = "MusicState"
val EmptyMusicState =
    MusicState(id = -1, name = EmptyName, md5 = "", rawBitrate = 96000, file = File("").toRFile())

data class MusicState(
    val id: Int,
    val file: RFile,
    // 歌名
    val name: String = file.name,
    // 歌手
    val artists: String = EmptyArtists,
    // 专辑
    val album: String = "$EmptyAlbum $id",
    // 专辑缩略图
    val smallAlbumArt: String? = null,

    private val rawBitrate: Int,
    val bitrate: Bitrate = Bitrate(rawBitrate),
    val duration: Long = -1,
    val fileSize: Long = -1,
    val track: Int = -1,
    val disc: String = "",
    val year: String = "",
    val md5: String,

    // 缓存损坏
    val incomplete: Boolean = false,
    // 丢失 idx 文件
    val missingInfo: Boolean = true,
    val neteaseAppCache: NeteaseCacheProvider.NeteaseAppCache? = null,
) {

    val ext by lazy { inputStream?.let { FileType.getFileType(it) } ?: "mp3" }

    var deleted by mutableStateOf(false)
    var saved by mutableStateOf(false)

    val displayFileName: String
        get() {
            var name = "$artists - $name"
            // 避免极端情况导致文件名过长
            if (name.length > 80)
                name = if (this.name.length > 80)
                    this.name.substring(0, 80) + "... "
                else
                    this.name

            // 无损文件不需要添加比特率避免重名
            if (bitrate.type != Bitrate.Type.flac)
                name += " - ${displayBitrate.replace(" kb/s", "kbps ")}"
            return name.replaceIllegalChar()
        }


    val displayBitrate get() = bitrate.displayBitrate

    fun delete() = NeteaseCacheProvider.removeCacheFile(this).apply {
        deleted = this
    }

    val inputStream get() = if (deleted) null else XorByteInputStream(file.input!!)

    // 包装调用函数
    fun getAlbumPicUrl(width: Int = -1, height: Int = -1): String? =
        NeteaseDataService.instance.getAlbumPicUrl(id, width, height)

    suspend fun decryptFile(
        callback: (out: Uri?, hasError: Boolean, e: Exception?) -> Unit = { _, _, _ -> }
    ): Boolean = runWithPrintTimeCostSuspend(TAG, "导出文件耗时") {
        NeteaseCacheProvider.decryptCacheFile(
            this,
            callback
        )
    }

    // 主要避免无效拷贝
    fun reload(song: Song?): MusicState {
        if (song?.name == name) return this
        // kotlin 的 copy 只是替换参数,并不会重新走类初始化的构建函数
        // 所以需要自己替换更新参数
        return copy(
            name = when (song) {
                null -> md5
                else -> song.name ?: song.lMusic.name ?: ""
            },
            artists = when (song) {
                null -> EmptyArtists
                else -> song.artists.getArtists()
            },
            album = song?.album?.name ?: "$EmptyAlbum $id",
            smallAlbumArt = when (song) {
                null -> ""
                else -> NeteaseDataService.instance.getAlbumPicUrl(id, 80, 80)
            },
            track = song?.no ?: -1,
            disc = song?.disc ?: "",
            year = when (song) {
                null -> "N/A"
                else ->
                    SimpleDateFormat("yyyy", Locale.US)
                        .format(Date(song.album.publishTime ?: 0))
            }
        )
    }

    companion object {

        fun get(
            id: Int,
            md5: String,
            file: RFile,
            song: Song,
            rawBitrate: Int,
            bitrate: Bitrate = Bitrate(rawBitrate),
            duration: Long,
            fileSize: Long,
            // 缓存损坏
            incomplete: Boolean,
            // 丢失 idx 文件
            missingInfo: Boolean,
            neteaseAppCache: NeteaseCacheProvider.NeteaseAppCache,

            // 歌名
            name: String = song.name ?: song.lMusic.name ?: "",
            // 歌手
            artists: String = song.artists.getArtists(),
            // 专辑
            album: String = song.album.name ?: "$EmptyAlbum $id",
            // 专辑缩略图
            smallAlbumArt: String? = NeteaseDataService.instance.getAlbumPicUrl(id, 80, 80),

            track: Int = song.no,
            disc: String = song.disc,
            year: String =
                SimpleDateFormat("yyyy", Locale.US)
                    .format(Date(song.album.publishTime ?: 0))
        ): MusicState =
            MusicState(
                id = id,
                name = name,
                artists = artists,
                album = album,
                smallAlbumArt = smallAlbumArt,
                rawBitrate = rawBitrate,
                bitrate = bitrate,
                duration = duration,
                fileSize = fileSize,
                track = track,
                disc = disc,
                year = year,
                md5 = md5,
                file = file,
                incomplete = incomplete,
                missingInfo = missingInfo,
                neteaseAppCache = neteaseAppCache
            )


    }

    class Bitrate(private val rawBitrate: Int) {

        val type by lazy {
            when (rawBitrate) {
                1000 -> Type.trial
                999000 -> Type.flac
                else -> Type.kbps
            }
        }

        val displayBitrate by lazy {
            when (type) {
                Type.kbps -> "${rawBitrate / 1000} kb/s"
                else -> type.toString()
            }
        }

        val bitrate: Int by lazy {
            when (type) {
                Type.kbps -> rawBitrate / 1000
                else -> unknown
            }
        }

        enum class Type {
            trial,//  试听
            flac,// 无损
            kbps
        }

        companion object {
            const val unknown: Int = -1
        }

    }

}
