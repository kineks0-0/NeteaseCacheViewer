package io.github.kineks.neteaseviewer.data.local

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.google.common.collect.ImmutableSet
import ealvatag.audio.AudioFileIO
import ealvatag.tag.FieldKey
import ealvatag.tag.NullTag
import ealvatag.tag.Tag
import ealvatag.tag.images.ArtworkFactory
import io.github.kineks.neteaseviewer.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okio.IOException
import java.io.File
import java.io.InputStream


object MediaStoreProvider {

    lateinit var okHttpClient: OkHttpClient
    private val parentFile: File = App.context.cacheDir

    suspend fun setInfo(music: Music, file: File) {
        music.song ?: return


        val audioFile = withContext(Dispatchers.IO) {
            AudioFileIO.read(file)
        }

        val audioHeader = audioFile.audioHeader
        /*val channels = audioHeader.channelCount
        val bitRate = audioHeader.bitRate
        val encodingType = audioHeader.encodingType*/

        var tag: Tag = audioFile.tag.or(NullTag.INSTANCE)
        val title: String = tag.getValue(FieldKey.TITLE).or("")
        if ("" == title) {
            if (tag === NullTag.INSTANCE) {
                // there was no tag. set a new default tag for the file type
                tag = audioFile.setNewDefaultTag()
            }
        }

        //tag = audioFile.tag
        tag.setField(FieldKey.TITLE, music.name)
        tag.setField(FieldKey.ALBUM, music.album)
        tag.setField(FieldKey.ARTIST, music.artists)
        tag.setField(FieldKey.ARTISTS, music.artists)
        tag.setField(FieldKey.TRACK, music.track.toString())
        tag.setField(FieldKey.YEAR, music.year)
        tag.setField(FieldKey.DISC_NO, music.disc)
        val pic = music.getAlbumPicUrl()
        if (!pic.isNullOrEmpty() && tag.artworkList.isEmpty()) {
            Log.d(this.javaClass.name, "下载专辑图 : $pic")

            val supportedFields: ImmutableSet<FieldKey> = tag.supportedFields
            if (supportedFields.contains(FieldKey.COVER_ART)) {
                println("File type supports Artwork")
                /*tag.setArtwork(
                    ArtworkFactory.createLinkedArtworkFromURL(
                        pic
                    )
                )*/

                if (!this::okHttpClient.isInitialized) okHttpClient = OkHttpClient()
                val request: Request = Request.Builder().url(pic).build()

                withContext(Dispatchers.IO) {
                    try {
                        val byteArray =
                            okHttpClient.newCall(request).execute().body()?.byteStream() ?: TODO()

                        val artwork = File(parentFile, music.displayFileName + ".image")
                        artwork.delete()
                        artwork.parentFile?.mkdirs()

                        byteArray.use { input ->
                            artwork.outputStream().use {
                                input.copyTo(it)
                            }
                        }
                        val art = ArtworkFactory.createArtworkFromFile(artwork)
                        // 部分flac文件不这么写会报错
                        tag.deleteArtwork().createArtwork(art)
                        tag.setArtwork(art)
                        try {
                            audioFile.save()
                        } catch (e: Exception) {
                            Log.e(this@MediaStoreProvider.javaClass.name, e.message, e)
                            Log.e(this@MediaStoreProvider.javaClass.name, file.absolutePath)
                        }

                        artwork.delete()
                    } catch (e: Exception) {
                        Log.e(this@MediaStoreProvider.javaClass.name, e.message, e)
                        Log.e(this@MediaStoreProvider.javaClass.name, file.absolutePath)
                    }
                }
                //tag.setField(ArtworkFactory.createLinkedArtworkFromURL(pic))
            }


        }

        withContext(Dispatchers.IO) {
            audioFile.save()
        }


    }

    //fileName为需要保存到媒体的文件名
    fun insert2Music(inputStream: InputStream, music: Music, ext: String = "mp3"): Uri? {
        val songDetails = ContentValues()
        val resolver = App.context.contentResolver
        val fileName: String = music.displayFileName
        songDetails.apply {
            put(MediaStore.Audio.AudioColumns.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.AudioColumns.TITLE, music.name)
            put(MediaStore.Audio.AudioColumns.ARTIST, music.artists)
            put(MediaStore.Audio.AudioColumns.ALBUM, music.album)
            put(MediaStore.Audio.AudioColumns.TRACK, music.track)
            put(MediaStore.Audio.AudioColumns.YEAR, music.year)
            //put(MediaStore.Audio.AudioColumns.MIME_TYPE, "")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            songDetails.put(MediaStore.Audio.Media.IS_PENDING, 1)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            songDetails.apply {
                put(MediaStore.Audio.AudioColumns.ALBUM_ARTIST, music.artists)
                put(MediaStore.Audio.AudioColumns.DISC_NUMBER, music.disc)
                put(MediaStore.Audio.AudioColumns.NUM_TRACKS, music.track)
            }
        }

        // Find all audio files on the primary external storage device.
        val audioCollection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY
                )
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //RELATIVE_PATH 字段表示相对路径-------->(1)
            songDetails.put(
                MediaStore.Audio.AudioColumns.RELATIVE_PATH,
                Environment.DIRECTORY_MUSIC + "/NeteaseViewer/"
            )
        } else {
            val dstPath = (Environment.getExternalStorageDirectory().toString()
                    + File.separator + Environment.DIRECTORY_MUSIC + "/NeteaseViewer/"
                    + File.separator + fileName)
            //DATA字段在Android 10.0 之后已经废弃
            songDetails.put(MediaStore.Audio.AudioColumns.DATA, dstPath)
        }

        //插入媒体------->(2)
        val songContentUri: Uri =
            App.context.contentResolver.insert(
                audioCollection,
                songDetails
            )
                ?: return null

        //写入文件------->(3)
        val outPut = resolver.openOutputStream(songContentUri, "w")
            ?: return null

        inputStream.use { input ->
            outPut.use {
                input.copyTo(it)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            songDetails.clear()
            songDetails.put(MediaStore.Audio.Media.IS_PENDING, 0)
            resolver.update(songContentUri, songDetails, null, null)
        }

        return songContentUri
    }


}