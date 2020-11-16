package com.owo.recall.music.core

import java.io.File

data class NeteaseMusicSong(val songFile: File, val duration: Long, val filesize: Long, val musicId: Long, val filemd5: String, val bitrate: Int, val md5: String, var songInfo: SongInfo = SongInfo(),
                            var decodeFile: File = File("404")) {

    data class SongInfo(val name: String = "", val id: Long = -1,
                        val artists: Array<Artists> = Array<Artists>(1){Artists()}, val albums: Album = Album()) {

        fun getArtistsName(): String {
            if ( artists.size < 2 ){
                return artists[0].name
            }
            val s: StringBuilder = StringBuilder()
            for ( i in artists.indices) {
                if ( i == artists.size-1 ) {
                    s.append(artists[i].name)
                } else {
                    s.append(artists[i].name).append(",")
                }
            }
            return s.toString()
        }
    }

    data class Artists(val name: String = "", val id: Long = -1,
                       val picUrl: String = "", val img1v1Url: String = "")

    data class Album(val name: String = "", val id: Long = -1,val type: String = "", val size: Int = -1, val No: Int = -1,
                     val picId: Long = -1, val blurPicUrl: String = "", val picUrl: String = "", val description: String = "")
}