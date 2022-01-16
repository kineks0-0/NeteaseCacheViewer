package io.github.kineks.neteaseviewer.data.local

import io.github.kineks.neteaseviewer.data.api.Song
import java.io.File


fun Music(song: Song, file: File?) {
    var artists = ""
    when (song.artists.size) {
        0 -> {
            artists = "N/A"
        }
        else -> {
            song.artists.forEachIndexed { index, name ->
                artists += when (index) {
                    0 -> name
                    //song.artists.lastIndex -> ",$name"
                    else -> ",$name"
                }
            }
        }
    }
    Music(song.id, song.name, artists, song.bMusic.bitrate, song, file)
}

data class Music(
    val id: Int,
    val name: String,
    val artists: String,
    val bitrate: Int = -1,
    val song: Song? = null,
    val file: File? = null,
    val info: CacheFileInfo? = null
) {

    val incomplete =
        when {
            info == null -> false
            file == null -> false
            else -> {
                // 判断缓存文件和缓存文件消息中的大小是否一致
                info.fileSize != file.length()
            }
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

}
