package com.owo.recall.music.core

import android.os.Environment
import android.util.Log
import com.owo.recall.music.CoreApplication
import com.owo.recall.music.R
import com.owo.recall.music.core.net.HttpUtil
import com.owo.recall.music.core.play.PlayUtil
import com.owo.recall.music.core.setting.SettingList
import com.owo.recall.music.getApplicationContext
import com.owo.recall.music.toast
import com.owo.recall.music.ui.NeteaseMusicSongAdapter
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException


object MusicFileProvider {

    private val CacheFolder: File = getApplicationContext().externalCacheDir ?: CoreApplication.context.cacheDir
    private val CacheNetFolder: File
    private val CacheDecodeFolder: File
    private val CacheOtherFolder: File
    var DIR_Music: File// = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
    var NeteaseMusicCacheFolder: File
    private val NeteaseMusicCacheFileEnd: String by lazy { "uc!" }
    private val NeteaseInfoCacheFileEnd: String by lazy { "idx!" }

    private val second: String by lazy { getApplicationContext().getString(R.string.second) }
    private val minute: String by lazy { getApplicationContext().getString(R.string.minute) }


    init {

        //val cacheDataFile: File = CoreApplication.context.cacheDir
        CacheFolder.mkdirs()
        CacheNetFolder = File(CacheFolder.absolutePath + "/NetData/")
        CacheNetFolder.mkdirs()
        CacheDecodeFolder = File(CacheFolder.absolutePath + "/DecodeData/")
        CacheDecodeFolder.mkdirs()
        CacheOtherFolder = File(CacheFolder.absolutePath + "/Other/")
        CacheOtherFolder.mkdirs()



        NeteaseMusicCacheFolder = File(SettingList.getSettingItem("NeteaseMusicCacheFolder").keyValueEditor.getValueAsString())//默认值在settinglist那处理
        DIR_Music = File(
            SettingList.getSettingItem(
                "ExportMusicFolder", Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_MUSIC
                ).absolutePath
            )
        )

        //getOtherCacheFile("CacheDecodeNeteaseFile.song").delete()

    }

    fun getMusicFile(fileName: String): File {
        return File(DIR_Music, fileName)
    }

    fun getNetCacheFile(fileName: String): File {
        return File(CacheNetFolder, fileName)
    }

    fun getDecodeCacheFile(fileName: String): File {
        return File(CacheDecodeFolder, fileName)
    }

    fun getOtherCacheFile(fileName: String): File {
        return File(CacheOtherFolder, fileName)
    }

    fun getMusicCacheFile(fileEnd: String): Array<File> {
        return NeteaseMusicCacheFolder.listFiles { _, s -> s.endsWith(".$fileEnd") }
            ?: return Array(0){File("/404")}
    }

    fun getMusicCacheFileList(): ArrayList<NeteaseMusicSong> {
        var infoFile: File// = File()
        var infoJSON: JSONObject
        //var songInfo: NeteaseMusicSong
        val songArrayList: ArrayList<NeteaseMusicSong> = ArrayList()
        getMusicCacheFile(NeteaseMusicCacheFileEnd).forEach {
            infoFile = File(
                it.absolutePath.replaceFirst(
                    ".$NeteaseMusicCacheFileEnd",
                    ".$NeteaseInfoCacheFileEnd"
                )
            )
            if (infoFile.exists()) {
                infoJSON = JSONObject(infoFile.readText())

                val cacheFile: File = getNetCacheFile(infoJSON.getLong("musicId").toString())

                if (cacheFile.exists()) {
                    if (checkJSONisAvailable(cacheFile.readText())) {
                        songArrayList.add(
                            NeteaseMusicSong(
                                it,
                                infoJSON.getLong("duration"),
                                infoJSON.getLong(
                                    "filesize"
                                ),
                                infoJSON.getLong("musicId"),
                                infoJSON.getString("filemd5"),
                                infoJSON.getInt("bitrate"),
                                infoJSON.getString("md5"),
                                getNeteaseSongInfo(
                                    cacheFile.readText()
                                )
                            )
                        )
                    } else {
                        cacheFile.delete()
                        songArrayList.add(
                            NeteaseMusicSong(
                                it, infoJSON.getLong("duration"), infoJSON.getLong(
                                    "filesize"
                                ), infoJSON.getLong("musicId"), infoJSON.getString("filemd5"),
                                infoJSON.getInt("bitrate"), infoJSON.getString("md5")
                            )
                        )
                    }

                } else {
                    songArrayList.add(
                        NeteaseMusicSong(
                            it, infoJSON.getLong("duration"), infoJSON.getLong(
                                "filesize"
                            ), infoJSON.getLong("musicId"), infoJSON.getString("filemd5"),
                            infoJSON.getInt("bitrate"), infoJSON.getString("md5")
                        )
                    )
                }

            }
        }
        return songArrayList
    }

    fun getNeteaseSongInfo(rawJSON: String): NeteaseMusicSong.SongInfo {
        //Log.d(this.javaClass.toString(),rawJSON)
        val rootJSONObject = JSONObject(rawJSON)
        if (!checkJSONisAvailable(rootJSONObject)) return NeteaseMusicSong.SongInfo()

        val songJSONObject: JSONObject = rootJSONObject.getJSONArray("songs").getJSONObject(0)

        val artistsJSONArray: JSONArray = songJSONObject.getJSONArray("artists")
        val artistsArray: Array<NeteaseMusicSong.Artists> = Array<NeteaseMusicSong.Artists>(
            artistsJSONArray.length()
        ){ NeteaseMusicSong.Artists() }
        for (i in 0 until artistsJSONArray.length()) {
            val artistsJSONObject: JSONObject = artistsJSONArray.getJSONObject(i)
            artistsArray[i] = NeteaseMusicSong.Artists(
                artistsJSONObject.getString("name"), artistsJSONObject.getLong("id"),
                artistsJSONObject.getString("picUrl"), artistsJSONObject.getString("img1v1Url")
            )
        }

        val albumsJSONObject: JSONObject = songJSONObject.getJSONObject("album")

        return NeteaseMusicSong.SongInfo(
            songJSONObject.getString("name"),
            songJSONObject.getLong("id"),
            artistsArray,
            NeteaseMusicSong.Album(
                albumsJSONObject.getString("name"),
                albumsJSONObject.getLong("id"),
                albumsJSONObject.getString("type"),
                albumsJSONObject.getInt("size"),
                songJSONObject.getInt(
                    "no"
                ),
                albumsJSONObject.getLong("picId"),
                albumsJSONObject.getString("blurPicUrl"),
                albumsJSONObject.getString("picUrl"),
                albumsJSONObject.getString("description")
            )
        )
    }

    fun getNeteaseSongIDRawJSON(songID: Long, rawJSONDataCallBack: RawJSONDataCallBack){

        val cacheFile: File = getNetCacheFile(songID.toString())

        if (cacheFile.exists()) {
           rawJSONDataCallBack.callback(cacheFile.readText(), true)

        } else {
            //http://music.163.com/api/v2/song/detail
            HttpUtil.sendHttpRequest(
                "https://music.163.com/api/song/detail?id=$songID&ids=%5B$songID%5D",
                object :
                    Callback {

                    override fun onFailure(call: Call, e: IOException) {
                        Log.e(this.javaClass.toString(), e.toString())
                        //TODO 记得log出去
                    }

                    override fun onResponse(call: Call, response: Response) {

                        if (response.body != null) {
                            val s: String = response.body!!.string()
                            cacheFile.writeText(s)
                            rawJSONDataCallBack.callback(s, false)
                        } else {
                            rawJSONDataCallBack.callback("", false)
                        }

                    }

                })
        }
    }
    interface RawJSONDataCallBack { fun callback(response: String, isCache: Boolean) }

    fun checkJSONisAvailable(rootJSONObject: JSONObject): Boolean {
        return try {
            rootJSONObject.getInt("code")==200
        } catch (e: Exception) {
            Log.e(this.javaClass.toString(), e.message, e)
            false
        }
    }

    fun checkJSONisAvailable(rawJSON: String): Boolean {
        if (rawJSON == "") {
            return false
        }
        return try {
            checkJSONisAvailable(JSONObject(rawJSON))
        } catch (e: Exception) {
            Log.e(this.javaClass.toString(), e.message, e)
            false
        }
        //return checkJSONisAvailable(JSONObject(rawJSON))
    }

    fun ms2String(ms: Long): String {
        var s = ms/1000
        return if (ms/1000 < 60) {
            "$s $second"
        } else {
            val m = s/60
            s %= 60
            "$m $minute $s $second"
        }
    }

    fun ExportNeteaseCacheFile(song: NeteaseMusicSong, adapter: NeteaseMusicSongAdapter) {
        PlayUtil.getDecodeNeteaseFile(song, object : PlayUtil.DecodeCompleteCallBack {
            override fun completeCallBack(decodeFile: File, incompleteFile: Boolean) {

                toast("导出中")

                if (incompleteFile) {
                    toast("警告: 文件不完整,无法通过校对")
                    //todo 将硬编码文本转移string.xml 对不完整缓存采用联网下载
                }
                if (song.songInfo.id != -1L) {
                    val dirMusic: File =
                        this@MusicFileProvider.DIR_Music//getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                    dirMusic.mkdirs()
                    val outFile = File(
                        dirMusic,
                        song.songInfo.name + " - " + song.songInfo.getArtistsName() + ".mp3"
                    )
                    //Todo: 对文件类型判断 对音乐写入标签信息
                    outFile.writeBytes(decodeFile.readBytes())

                    /*val audioFile = AudioFileIO.read(outFile)
                    val newTag: Tag = audioFile.tag
                    newTag.setField(FieldKey.ALBUM, song.songInfo.albums.name)
                    newTag.setField(FieldKey.ARTIST, song.songInfo.artists.toString())
                    newTag.setField(FieldKey.TITLE, song.songInfo.name)
                    //newTag.setField(FieldKey.A)
                    audioFile.commit()*/

                    PlayUtil.scanFile(outFile.absolutePath)
                    toast("已保存在: " + outFile.absolutePath)
                } else {

                    toast("正在下载歌曲信息")
                    this@MusicFileProvider.getNeteaseSongIDRawJSON(song.musicId,
                        object : MusicFileProvider.RawJSONDataCallBack {
                            override fun callback(response: String, isCache: Boolean) {
                                CoreApplication.post {
                                    if (!isCache) {
                                        song.songInfo = this@MusicFileProvider.getNeteaseSongInfo(
                                            response
                                        )
                                        CoreApplication.post {
                                            adapter.update(song)
                                        }
                                    }
                                    completeCallBack(decodeFile, incompleteFile)
                                }
                            }

                        })
                }

            }
        })
    }

            /*fun getCacheDir(context: Context): String {
                if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() || !Environment.isExternalStorageRemovable() ) {
                    context.externalCacheDir?.path
                } else {
                    context.cacheDir.path
                }
            }

            fun getDataFilePath(context: Context,dir: String): String {
                val directoryPath: String =
                    if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) { //判断外部存储是否可用
                        context.getExternalFilesDir(dir)!!.absolutePath
                    } else { //没外部存储就使用内部存储
                        context.filesDir + File.separator + dir
                    }

                val file = File(directoryPath)
                if (!file.exists()) file.mkdirs() //判断文件目录是否存在

                return directoryPath
            }*/

}