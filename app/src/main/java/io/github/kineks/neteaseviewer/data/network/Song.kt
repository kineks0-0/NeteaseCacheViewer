package io.github.kineks.neteaseviewer.data.network

data class SongDetail(
    val code: Int,
    val equalizers: Equalizers,
    val songs: List<Song>
)

class Equalizers

data class Song(
    val album: Album,
    //val alias: List<String>,
    val artists: List<ArtistXX>,
    //val audition: Any,
    val bMusic: BMusic,
    //val commentThreadId: String,
    //val copyFrom: String,
    //val copyright: Int,
    //val copyrightId: Int,
    //val crbt: Any,
    //val dayPlays: Int,
    val disc: String,
    val duration: Int,
    //val fee: Int,
    //val ftype: Int,
    val hMusic: HMusic,
    //val hearTime: Int,
    val id: Int,
    val lMusic: LMusic,
    val mMusic: MMusic,
    //val mark: Int,
    //val mp3Url: Any,
    //val mvid: Int,
    val name: String?,
    val no: Int,
    //val noCopyrightRcmd: Any,
    //val originCoverType: Int,
    //val originSongSimpleData: Any,
    //val playedNum: Int,
    //val popularity: Double,
    //val position: Int,
    //val ringtone: String,
    //val rtUrl: Any,
    //val rtUrls: List<Any>,
    //val rtype: Int,
    //val rurl: Any,
    //val score: Int,
    //val sign: Any,
    //val single: Int,
    //val starred: Boolean,
    //val starredNum: Int,
    //val status: Int,
    val transName: Any
)

data class Album(
    //val alias: List<String>,
    val artist: Artist,
    val artists: List<ArtistX>,
    //val blurPicUrl: String,
    //val briefDesc: String,
    //val commentThreadId: String,
    //val company: String,
    //val companyId: Int,
    //val copyrightId: Int,
    //val description: String,
    val id: Int,
    //val mark: Int,
    val name: String,
    //val onSale: Boolean,
    //val pic: Long,
    //val picId: Long,
    //val picId_str: String,
    val picUrl: String,
    val publishTime: Long,
    //val size: Int,
    val songs: List<Any>,
    //val status: Int,
    //val subType: String,
    //val tags: String,
    val transName: Any,
    //val type: String
)

data class ArtistXX(
    //val albumSize: Int,
    //val alias: List<Any>,
    //val briefDesc: String,
    //val id: Int,
    //val img1v1Id: Int,
    //val img1v1Url: String,
    //val musicSize: Int,
    val name: String,
    //val picId: Int,
    //val picUrl: String,
    //val topicPerson: Int,
    //val trans: String
)

data class BMusic(
    //val bitrate: Int,
    //val dfsId: Int,
    //val extension: String,
    val id: Long,
    val name: String,
    //val playTime: Int,
    //val size: Int,
    //val sr: Int,
    //val volumeDelta: Double
)

data class HMusic(
    //val bitrate: Int,
    //val dfsId: Int,
    //val extension: String,
    val id: Long,
    val name: String,
    //val playTime: Int,
    //val size: Int,
    //val sr: Int,
    //val volumeDelta: Double
)

data class LMusic(
    //val bitrate: Int,
    //val dfsId: Int,
    //val extension: String,
    val id: Long,
    val name: String?,
    //val playTime: Int,
    //val size: Int,
    //val sr: Int,
    //val volumeDelta: Double
)

data class MMusic(
    //val bitrate: Int,
    //val dfsId: Int,
    //val extension: String,
    val id: Long,
    val name: Any,
    //val playTime: Int,
    //val size: Int,
    //val sr: Int,
    //val volumeDelta: Double
)

data class Artist(
    //val albumSize: Int,
    //val alias: List<Any>,
    //val briefDesc: String,
    val id: Int,
    //val img1v1Id: Int,
    //val img1v1Url: String,
    //val musicSize: Int,
    val name: String,
    //val picId: Int,
    //val picUrl: String,
    //val topicPerson: Int,
    //val trans: String
)

data class ArtistX(
    //val albumSize: Int,
    //val alias: List<Any>,
    //val briefDesc: String,
    val id: Int,
    //val img1v1Id: Int,
    //val img1v1Url: String,
    //val musicSize: Int,
    val name: String,
    //val picId: Int,
    //val picUrl: String,
    //val topicPerson: Int,
    //val trans: String
)