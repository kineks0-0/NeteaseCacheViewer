package io.github.kineks.neteaseviewer

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.FileUtils
import android.provider.MediaStore
import io.github.kineks.neteaseviewer.data.local.RFile
import io.github.kineks.neteaseviewer.data.network.ArtistXX
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.net.URLConnection
import kotlin.coroutines.resume
import kotlin.experimental.and

fun Array<Int>.toURLArray(): String {
    val str = StringBuilder()
    this.forEachIndexed { index, id ->
        str.append(
            when (index) {
                0 -> "[$id,"
                this.lastIndex -> "$id]"
                else -> "$id,"
            }
        )
    }
    return str.toString()
}

fun List<ArtistXX>.getArtists(delimiters: String = ",", defValue: String = "N/A"): String {
    if (isEmpty()) return defValue
    val str = StringBuilder()
    forEachIndexed { index, info ->
        val i = info.name
        str.append(
            when (index) {
                lastIndex -> i
                0 -> "$i$delimiters"
                else -> "$i$delimiters"
            }
        )
    }
    return str.toString()
}

fun getString(id: Int) = App.context.getString(id)

fun String.decodeHex(): ByteArray {
    check(length % 2 == 0) { "Must have an even length" }

    return chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}

fun Byte.toHex(): String {
    var hex = Integer.toHexString((this and 0xFF.toByte()).toInt())
    if (hex.length < 2) {
        hex = "0$hex"
    }
    return hex
}


fun String.replaceIllegalChar() =
    this.replace("/", "／")
//.replace("*", Char(10034).toString())
//.replace("?", "？")
//.replace("|", "｜")
//.replace(":", ":")
//.replace("<", "＜")
//.replace(">", "＞")

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun File.scanFile(context: Context = App.context) =
    suspendCancellableCoroutine<Uri?> { cont ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val uri = ContentValues().run {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType())
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MUSIC)
                context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, this)
            }?.also { it ->
                context.contentResolver.openOutputStream(it)?.let {
                    val fis = this@scanFile.inputStream()
                    FileUtils.copy(fis, it)
                    fis.close()
                    it.close()
                }
            }

            cont.resume(uri)
        } else {
            MediaScannerConnection.scanFile(
                context, arrayOf(path), arrayOf(mimeType())
            ) { _, uri ->
                cont.resume(uri)
            }
        }
    }

fun File.mimeType() =
    URLConnection.getFileNameMap().getContentTypeFor(name) ?: "multipart/form-data"

fun RFile.RFileType.toRFile(path: String) = RFile(this, path)