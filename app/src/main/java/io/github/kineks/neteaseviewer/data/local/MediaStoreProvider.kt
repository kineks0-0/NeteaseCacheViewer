package io.github.kineks.neteaseviewer.data.local

import android.annotation.SuppressLint
import android.content.ContentValues
import android.net.Uri
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
import io.github.kineks.neteaseviewer.await
import io.github.kineks.neteaseviewer.data.local.cacheFile.MusicState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream


object MediaStoreProvider {

    private lateinit var okHttpClient: OkHttpClient
    private val parentFile: File = App.context.cacheDir
    private const val TAG = "MediaStoreProvider"

    suspend fun setInfo(musicState: MusicState, file: RFile) {
        if (musicState.track == -1) return

        val audioFile = withContext(Dispatchers.IO) {
            AudioFileIO.read(file.file)
        }

        var tag: Tag = audioFile.tag.or(NullTag.INSTANCE)
        val title: String = tag.getValue(FieldKey.TITLE).or("")
        if ("" == title) {
            if (tag === NullTag.INSTANCE) {
                // 标签信息不存在，需根据文件类型设置一个新的默认标签。
                tag = audioFile.setNewDefaultTag()
            }
        }

        tag.setField(FieldKey.TITLE, musicState.name)
        tag.setField(FieldKey.ALBUM, musicState.album)
        tag.setField(FieldKey.ARTIST, musicState.artists)
        tag.setField(FieldKey.ARTISTS, musicState.artists)
        tag.setField(FieldKey.TRACK, musicState.track.toString())
        tag.setField(FieldKey.YEAR, musicState.year)
        tag.setField(FieldKey.DISC_NO, musicState.disc)
        val pic = musicState.getAlbumPicUrl()
        if (!pic.isNullOrEmpty() && tag.artworkList.isEmpty()) {
            Log.d(this.javaClass.name, "下载专辑图 : $pic")

            val supportedFields: ImmutableSet<FieldKey> = tag.supportedFields
            if (supportedFields.contains(FieldKey.COVER_ART)) {
                Log.d(TAG, "File type supports Artwork")

                if (!this::okHttpClient.isInitialized) okHttpClient = OkHttpClient()
                val request: Request = Request.Builder().url(pic).build()

                withContext(Dispatchers.IO) {
                    try {
                        val byteArray =
                            okHttpClient.newCall(request).await().byteStream()
                                ?: throw Exception("下载失败")

                        val artwork = File(parentFile, musicState.displayFileName + ".image")
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
                            Log.e(
                                this@MediaStoreProvider.javaClass.name,
                                file.type.name + "://" + file.path
                            )
                        }

                        artwork.delete()
                    } catch (e: Exception) {
                        Log.e(this@MediaStoreProvider.javaClass.name, e.message, e)
                        Log.e(
                            this@MediaStoreProvider.javaClass.name,
                            file.type.name + "://" + file.path
                        )
                    }
                }
            }


        }

        withContext(Dispatchers.IO) {
            audioFile.save()
        }


    }

    @SuppressLint("InlinedApi")
    fun insert2Music(inputStream: InputStream, musicState: MusicState, ext: String = "mp3"): Uri? {
        val songDetails = ContentValues()
        val resolver = App.context.contentResolver
        val fileName = "${musicState.displayFileName}.$ext"
        songDetails.apply {
            put(MediaStore.Audio.AudioColumns.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.AudioColumns.TITLE, musicState.name)
            put(MediaStore.Audio.AudioColumns.ARTIST, musicState.artists)
            put(MediaStore.Audio.AudioColumns.ALBUM, musicState.album)
            put(MediaStore.Audio.AudioColumns.TRACK, musicState.track)
            put(MediaStore.Audio.AudioColumns.YEAR, musicState.year)
            //put(MediaStore.Audio.AudioColumns.MIME_TYPE, "")
        }

        // 在 Android Q 之后可以先获取该媒体的句柄
        if (App.isAndroidQorAbove) {
            songDetails.put(MediaStore.Audio.Media.IS_PENDING, 1)
        }

        if (App.isAndroidRorAbove) {
            songDetails.apply {
                put(MediaStore.Audio.AudioColumns.ALBUM_ARTIST, musicState.artists)
                put(MediaStore.Audio.AudioColumns.DISC_NUMBER, musicState.disc)
                put(MediaStore.Audio.AudioColumns.NUM_TRACKS, musicState.track)
            }
        }

        // Find all audio files on the primary external storage device.
        val audioCollection =
            if (App.isAndroidQorAbove) {
                MediaStore.Audio.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY
                )
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

        if (App.isAndroidQorAbove) {
            // RELATIVE_PATH 字段表示相对路径
            songDetails.put(
                MediaStore.Audio.AudioColumns.RELATIVE_PATH,
                Environment.DIRECTORY_MUSIC + "/NeteaseViewer/"
            )
        } else {
            @Suppress("DEPRECATION")
            val dstPath = (Environment.getExternalStorageDirectory().toString()
                    + File.separator + Environment.DIRECTORY_MUSIC + "/NeteaseViewer/"
                    + File.separator + fileName)
            // DATA 字段在 Android Q 之后已经废弃
            @Suppress("DEPRECATION")
            songDetails.put(MediaStore.Audio.AudioColumns.DATA, dstPath)
        }

        // 插入媒体
        val songContentUri: Uri =
            App.context.contentResolver.insert(
                audioCollection,
                songDetails
            )
                ?: return null

        // 写入文件
        val outPut = resolver.openOutputStream(songContentUri, "w")
            ?: return null

        inputStream.use { input ->
            outPut.use {
                input.copyTo(it)
            }
        }

        // 释放占用并更新媒体库信息
        if (App.isAndroidQorAbove) {
            songDetails.clear()
            songDetails.put(MediaStore.Audio.Media.IS_PENDING, 0)
            resolver.update(songContentUri, songDetails, null, null)
        }

        return songContentUri
    }


}