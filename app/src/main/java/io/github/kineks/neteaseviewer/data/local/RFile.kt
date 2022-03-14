package io.github.kineks.neteaseviewer.data.local

import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.net.toFile
import androidx.core.net.toUri
import io.github.kineks.neteaseviewer.App
import java.io.File
import java.io.InputStream
import java.io.OutputStream

const val TAG = "RFile"
data class RFile(val type: RFileType, val path: String) {

    enum class RFileType(val type: String) {
        // 目录
        Uri("uri"),
        File("file"),

        // 单个文件
        SingleUri("single uri"),
        SingleFile("single file"),

        // 某个存储域下的目录或文件
        ShareStorage("share storage"),
        AndroidData("android data"),
        Root("root")
    }

    companion object {
        var androidData: Uri = Uri.EMPTY
    }

    val uri: Uri
        get() = when (type) {
            RFileType.Uri, RFileType.SingleUri -> Uri.parse(path)
            RFileType.ShareStorage ->
                File(Environment.getExternalStorageDirectory().path + path).toUri()
            RFileType.File, RFileType.SingleFile -> File(path).toUri()
            RFileType.AndroidData -> TODO()
            RFileType.Root -> TODO()
        }
    val file: File
        get() = uri.toFile()
    val input: InputStream?
        get() = when (type) {
            RFileType.Uri, RFileType.File -> null
            RFileType.SingleUri, RFileType.SingleFile, RFileType.ShareStorage -> file.inputStream()
            RFileType.AndroidData -> TODO()
            RFileType.Root -> TODO()
        }
    val output: OutputStream?
        get() = when (type) {
            RFileType.Uri, RFileType.File -> null
            RFileType.SingleUri, RFileType.SingleFile, RFileType.ShareStorage -> file.outputStream()
            RFileType.AndroidData -> TODO()
            RFileType.Root -> TODO()
        }

    fun read2File(callback: (index: Int, file: File) -> Unit) {
        when (type) {
            RFileType.Uri -> {
                uri
                    .toFile().walk().forEachIndexed { index, file ->
                        callback.invoke(index, file)
                    }
            }
            RFileType.File -> {
                File(path).walk().forEachIndexed { index, file ->
                    callback.invoke(index, file)
                }
            }
            RFileType.SingleUri -> callback.invoke(0, file)
            RFileType.SingleFile -> callback.invoke(0, file)
            RFileType.ShareStorage -> {
                File(Environment.getExternalStorageDirectory().path + path)
                    .walk().forEachIndexed { index, file ->
                        callback.invoke(index, file)
                    }
            }
            RFileType.AndroidData -> {
                if (!App.isAndroidRorAbove)
                    File(Environment.getExternalStorageDirectory().path + "/Android/Data/" + path)
                        .walk().forEachIndexed { index, file ->
                            callback.invoke(index, file)
                        }
                else
                    Log.e(TAG, "RFileType.AndroidData not support on Android R+")
            }
            RFileType.Root -> TODO()
        }
    }


}