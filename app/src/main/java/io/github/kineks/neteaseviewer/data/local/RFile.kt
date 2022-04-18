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

data class RFile(val type: RType, val path: String) {

    enum class RType(val type: String) {
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
            RType.Uri, RType.SingleUri -> Uri.parse(path)
            RType.ShareStorage -> file.toUri()
            RType.File, RType.SingleFile -> file.toUri()
            RType.AndroidData -> file.toUri()
            RType.Root -> TODO()
        }
    val file: File
        get() = when (type) {
            RType.File, RType.SingleFile -> File(path)
            RType.ShareStorage ->
                File(Environment.getExternalStorageDirectory().path + path)
            RType.AndroidData ->
                File(Environment.getExternalStorageDirectory().path + "/Android/Data/" + path)
            else -> uri.toFile()
        }
    val input: InputStream?
        get() = when (type) {
            RType.Uri, RType.File -> null
            RType.SingleUri, RType.SingleFile, RType.ShareStorage -> file.inputStream()
            RType.AndroidData -> TODO()
            RType.Root -> TODO()
        }
    val output: OutputStream?
        get() = when (type) {
            RType.Uri, RType.File -> null
            RType.SingleUri, RType.SingleFile, RType.ShareStorage -> file.outputStream()
            RType.AndroidData -> TODO()
            RType.Root -> TODO()
        }

    fun read2File(callback: (index: Int, file: File) -> Unit) {
        when (type) {
            RType.Uri -> {
                file.walk().forEachIndexed { index, file ->
                    callback.invoke(index, file)
                }
            }
            RType.File -> {
                file.walk().forEachIndexed { index, file ->
                    callback.invoke(index, file)
                }
            }
            RType.SingleUri -> callback.invoke(0, file)
            RType.SingleFile -> callback.invoke(0, file)
            RType.ShareStorage -> {
                file.walk().forEachIndexed { index, file ->
                    callback.invoke(index, file)
                }
            }
            RType.AndroidData -> {
                if (!App.isAndroidRorAbove)
                    file.walk().forEachIndexed { index, file ->
                        callback.invoke(index, file)
                    }
                else
                    Log.e(TAG, "RFileType.AndroidData not support on Android R+")
            }
            RType.Root -> TODO()
        }
    }


}