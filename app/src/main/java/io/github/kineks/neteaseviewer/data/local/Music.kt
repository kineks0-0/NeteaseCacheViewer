package io.github.kineks.neteaseviewer.data.local

import io.github.kineks.neteaseviewer.data.api.Song
import java.io.File


fun Music(song: Song,file: File?) {
    var artists = ""
    when (song.artists.size) {
        0 -> {
            artists = "N/A"
        }
        else -> {
            song.artists.forEachIndexed { index,name ->
                artists += when(index) {
                    0 -> name
                    //song.artists.lastIndex -> ",$name"
                    else -> ",$name"
                }
            }
        }
    }
    Music(song.id,song.name,artists,song.bMusic.bitrate,song,file)
}

data class Music(
    val id: Int,
    val name: String,
    val artists: String,
    val bitrate: Int = -1,
    val song: Song? = null,
    val file: File? = null,
    val info: CacheFileInfo? = null
)
