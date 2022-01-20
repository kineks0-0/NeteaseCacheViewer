package io.github.kineks.neteaseviewer.data.local

import android.util.Log
import okhttp3.*
import okio.IOException
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.AndroidArtwork
import java.io.File

object AudioInfoEdit {

    lateinit var okHttpClient: OkHttpClient

    fun setInfo(music: Music,file: File) {
        music.song ?: return
        val audioFile = AudioFileIO.read(file)
        val tag = audioFile.tag
        tag.setField(FieldKey.TITLE,music.name)
        tag.setField(FieldKey.ALBUM,music.album)
        tag.setField(FieldKey.ARTIST,music.artists)
        tag.setField(FieldKey.ARTISTS,music.artists)
        tag.setField(FieldKey.TRACK,music.track.toString())
        tag.setField(FieldKey.YEAR,music.year)
        tag.setField(FieldKey.DISC_NO,music.disc)
        val pic = music.getAlbumPicUrl()
        if (!pic.isNullOrEmpty()) {
            Log.d(this.javaClass.name,"下载专辑图 : $pic")
            if (!this::okHttpClient.isInitialized) okHttpClient = OkHttpClient()
            val request: Request = Request.Builder().url(pic).build()
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call?, e: IOException?) {
                    // 下载失败
                    Log.e(this@AudioInfoEdit.javaClass.name,e?.message,e)
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call?, response: Response) {
                    try {
                        val artwork = File(file.parentFile,music.displayFileName + ".image")
                        artwork.parentFile?.mkdirs()
                        val byteArray = response.body()?.byteStream() ?: return
                        artwork.writeBytes(byteArray.readBytes())

                        tag.setField(AndroidArtwork.createArtworkFromFile(artwork))
                        audioFile.commit()
                        artwork.delete()
                    } catch (e: Exception) {
                        Log.e(this@AudioInfoEdit.javaClass.name,e.message,e)
                    }
                }
            })
            //tag.setField(ArtworkFactory.createLinkedArtworkFromURL(pic))
        }
        audioFile.commit()


    }

}