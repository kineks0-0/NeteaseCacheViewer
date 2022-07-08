package io.github.kineks.neteaseviewer.data.local

import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import io.github.kineks.neteaseviewer.App
import io.github.kineks.neteaseviewer.data.local.RFile.RType.*
import io.github.kineks.neteaseviewer.toFile
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset

const val TAG = "RFile"


abstract class RFile(open val type: RType, open val path: String, open val name: String) {

    companion object {

        fun of(type: RType, path: String, name: String): RFile =
            when (type) {
                AndroidData, SingleAndroidData -> {
                    if (App.isAndroidRorAbove)
                        RFileAndroidData(
                            type = type, path = path, name = name
                        )
                    else
                        (Environment.getExternalStorageDirectory().path + "/Android/Data/" + path).toFile()
                            .toRFile()
                }
                File, SingleFile -> RFileFile(
                    type = type, path = path, name = name
                )
                Uri, SingleUri -> RFileUri(
                    type = type, path = path, name = name
                )
                ShareStorage -> RFileShareStorage(
                    type = type, path = path, name = name
                )
                else -> TODO()
            }

        fun of(parentFile: RFile, name: String): RFile? = when (parentFile.type) {
            SingleAndroidData, AndroidData -> {
                val file = parentFile as RFileAndroidData
                val documentFile = file.documentFile.findFile(name)
                Log.d(TAG, "documentFile: " + documentFile?.name)
                Log.d(TAG, "documentFileUri: " + documentFile?.uri.toString())
                if (documentFile != null)
                    RFileAndroidData(
                        type = SingleAndroidData,
                        path = parentFile.path + "/" + name,
                        name = name,
                        documentFile = documentFile
                    )
                else null
            }
            else -> of(
                parentFile.type,
                parentFile.path + "/" + name,
                name
            )
        }

        fun RFile.reload(): RFile = of(type, path, name)

    }

    enum class RType(val type: String) {
        // 目录或文件, 通常实现类会先识别为目录
        Uri("uri"),
        File("file"),

        // 单个文件，对于无法确定对象类型的时候手动指定为文件
        SingleUri("single uri"),
        SingleFile("single file"),
        SingleAndroidData("single android data"),

        // 某个存储域下的目录或文件
        ShareStorage("share storage"),
        AndroidData("android data"),
        Root("root")
    }


    val extension: String
        get() = name.substringAfterLast('.', "")

    val nameWithoutExtension: String
        get() = name.substringBeforeLast(".")

    abstract val isFile: Boolean

    val isDirectory: Boolean
        get() = if (exists()) !isFile else false

    abstract val parentFile: RFile?

    abstract fun exists(): Boolean

    abstract fun readText(charset: Charset = Charsets.UTF_8): String

    abstract fun mkdirs()

    abstract fun length(): Long

    abstract fun delete(): Boolean

    open fun copyto(
        rfile: RFile,
        overwrite: Boolean = false,
        bufferSize: Int = DEFAULT_BUFFER_SIZE
    ): RFile {
        if (rfile.exists() || !rfile.isFile) {
            if (overwrite)
                rfile.parentFile?.delete()
            else
                return rfile
        } else {
            rfile.parentFile?.mkdirs()
        }

        val input = input
        val output = rfile.output

        if (input != null && output != null) {
            input.use { input ->
                output.use { output ->
                    input.copyTo(output, bufferSize)
                }
            }
            return rfile
        }

        throw Exception("input or output == null")
    }

    abstract val uri: Uri

    abstract val file: File

    abstract val input: InputStream?

    abstract val output: OutputStream?

    abstract fun read2File(callback: (index: Int, rfile: RFile) -> Unit)


}


fun getNewDocumentFile(path: String, name: String, type: RFile.RType): DocumentFile =
    if (type == SingleAndroidData) {
        DocumentFile.fromFile(
            (Environment.getExternalStorageDirectory().path + "/Android/Data/" + path).toFile()
        )
    } else {
        var documentFile = DocumentFile.fromTreeUri(
            App.context,
            Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata")
        ) ?: TODO()
        Log.d("RFILE:", path)
        for (str in path.split("/")) {
            if (str != "")
                documentFile = documentFile.findFile(str) ?: documentFile
        }
        documentFile
    }


data class RFileAndroidData(
    override val path: String,
    override val name: String,
    override val type: RType = AndroidData,
    var documentFile: DocumentFile = getNewDocumentFile(path, name, type)
) : RFile(type, path, name) {

    override val isFile: Boolean
        get() = documentFile.isFile

    override val parentFile: RFile?
        get() = documentFile.parentFile?.toRFile()

    override fun exists(): Boolean = documentFile.exists()

    override fun readText(charset: Charset): String = try {
        App.context.contentResolver
            .openInputStream(documentFile.uri)?.bufferedReader(charset)
            ?.readText() ?: ""
    } catch (e: Exception) {
        Log.e(TAG, e.message, e)
        ""
    }

    override fun mkdirs() {
        documentFile.createDirectory(name)
    }


    override fun length(): Long = documentFile.length()

    override fun delete(): Boolean = documentFile.delete()

    override val uri: Uri
        get() = documentFile.uri

    override val file: File
        get() = (Environment.getExternalStorageDirectory().path + "/Android/Data/" + path).toFile()
    override val input: InputStream?
        get() = App.context.contentResolver.openInputStream(uri)
    override val output: OutputStream?
        get() = App.context.contentResolver.openOutputStream(uri)

    override fun read2File(callback: (index: Int, rfile: RFile) -> Unit) {
        if (fileUriUtils.isGrant()) {
            if (documentFile.isDirectory) {
                var index = 0
                for (docuFile: DocumentFile in documentFile.listFiles()) {
                    // 不会循环加载子目录
                    if (docuFile.isFile) {
                        callback(
                            index,
                            RFileAndroidData(
                                type = SingleAndroidData,
                                path = path + "/" + docuFile.name,
                                name = docuFile.name ?: "",
                                documentFile = docuFile
                            )
                        )
                        index++
                    }
                }

            } else {
                callback(0, documentFile.toRFile())
            }
        } else {
            Log.e(TAG, "RFileType.AndroidData not support on Android R+")
        }
    }


}


data class RFileFile(
    override val path: String,
    override val name: String,
    override val file: File = path.toFile(),
    override val type: RType = file.getRFileType()
) : RFile(type, path, name) {

    override val isFile: Boolean
        get() = file.isFile

    override val parentFile: RFile?
        get() = file.parentFile?.toRFile()

    override fun exists(): Boolean = file.exists()

    override fun readText(charset: Charset): String = file.readText(charset)

    override fun mkdirs() {
        file.mkdirs()
    }

    override fun length(): Long = file.length()

    override fun delete(): Boolean = file.delete()

    override val uri: Uri
        get() = file.toUri()

    override val input: InputStream
        get() = file.inputStream()
    override val output: OutputStream
        get() = file.outputStream()

    override fun read2File(callback: (index: Int, rfile: RFile) -> Unit) {
        if (file.isDirectory) {
            file.walk().forEachIndexed { index, file ->
                callback.invoke(index, file.toRFile())
            }
        } else {
            callback.invoke(0, file.toRFile())
        }
    }

}


data class RFileUri(
    override val path: String,
    override val name: String,
    override val uri: Uri = Uri.parse(path),
    override val type: RType = RType.Uri
) : RFile(type, path, name) {

    override val isFile: Boolean
        get() = try {
            input?.read()// 如果 inputstream 正常读取出文件内容就确定为文件
            true
        } catch (e: Exception) {
            Log.e(TAG, e.message, e)
            false
        }

    override val parentFile: RFile?
        get() = null

    override fun exists(): Boolean =
        input != null

    override fun readText(charset: Charset): String =
        input?.bufferedReader(charset)?.readText() ?: ""

    override fun mkdirs() {}

    override fun length(): Long =
        input?.available()?.toLong() ?: 0L

    override fun delete(): Boolean = App.context.contentResolver.delete(
        uri, MediaStore.Files.getContentUri(path).toString(),
        arrayOf(path)
    ) != -1 //

    override val file: File
        get() = uri.toFile()
    override val input: InputStream?
        get() = App.context.contentResolver.openInputStream(uri)
    override val output: OutputStream?
        get() = App.context.contentResolver.openOutputStream(uri)

    override fun read2File(callback: (index: Int, rfile: RFile) -> Unit) {
        if (type == SingleUri)
            callback(0, uri.toSingleRFile(name = name))
        else
            callback(0, uri.toRFile(name = name))
    }

}

data class RFileShareStorage(
    override val path: String,
    override val name: String,
    val rfile: RFile = (Environment.getExternalStorageDirectory().path + path).toFile().toRFile(),
    override val type: RType = RType.ShareStorage
) : RFile(type, path, name) {

    override val isFile: Boolean
        get() = rfile.isFile

    override val parentFile: RFile?
        get() = rfile.parentFile

    override fun exists(): Boolean = rfile.exists()

    override fun readText(charset: Charset): String = rfile.readText(charset)

    override fun mkdirs() = rfile.mkdirs()

    override fun length(): Long = rfile.length()

    override fun delete(): Boolean = rfile.delete()

    override val uri: Uri
        get() = rfile.uri
    override val file: File
        get() = rfile.file
    override val input: InputStream?
        get() = rfile.input
    override val output: OutputStream?
        get() = rfile.output

    override fun read2File(callback: (index: Int, rfile: RFile) -> Unit) = rfile.read2File(callback)

}


fun File.getRFileType() = if (this.isFile) SingleFile else File
fun DocumentFile.getRFileType() = if (this.isFile) SingleAndroidData else AndroidData

fun File.toRFile() = RFile.of(
    type = getRFileType(),
    path = path,
    name = name
)

fun Uri.toRFile(name: String = DocumentFile.fromTreeUri(App.context, this)?.name ?: "") = RFile.of(
    type = Uri,
    path = toString() ?: "",
    name = name
)

fun Uri.toSingleRFile(name: String = DocumentFile.fromSingleUri(App.context, this)?.name ?: "") =
    RFile.of(
        type = SingleUri,
        path = toString() ?: "",
        name = name
    )

fun DocumentFile.toRFile() = RFileAndroidData(
    type = getRFileType(),
    path = uri.toString(),
    name = name ?: "",
    documentFile = this
)