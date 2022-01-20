package io.github.kineks.neteaseviewer.data.local

import android.util.Log
import io.github.kineks.neteaseviewer.toHex
import java.io.InputStream


object FileType {
    private val mFileTypes = HashMap<String?, String>()

    fun getFileType(inputStream: InputStream): String? {
        val hex = getFileHeader(inputStream, 0, 8)
        var key = hex?.substring(0, 3 * 2)
        if (mFileTypes[key] != null)
            return mFileTypes[key]
        key = hex?.substring(4 * 2, 4 * 2 + 3 * 2)
        return when (true) {
            mFileTypes[key] != null -> mFileTypes[key]
            else -> "mp3"
        }
    }

    //获取文件头信息
    private fun getFileHeader(inputStream: InputStream, offset: Int = 0, size: Int = 3): String? {
        var value: String? = null
        try {
            //`is` = FileInputStream(filePath)
            val b = ByteArray(size = size)
            inputStream.use {
                inputStream.read(b, offset, b.size)
            }
            value = bytesToHexString(b)
            Log.d(this.javaClass.name, value)
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message, e)
        }
        return value
    }

    private fun bytesToHexString(bytes: ByteArray): String {
        val builder = StringBuilder()
        if (bytes.isEmpty()) return ""
        for (i in bytes.indices) {
            builder.append(bytes[i].toHex())
        }
        return builder.toString()
    }

    init {
        //
        mFileTypes["57415645"] = "wav"
        mFileTypes["41564920"] = "avi"
        mFileTypes["2E524D46"] = "rm"
        mFileTypes["000001BA"] = "mpg"
        mFileTypes["000001B3"] = "mpg"
        mFileTypes["6D6F6F76"] = "mov"
        mFileTypes["4D546864"] = "mid"

        //mFileTypes[""] = "mp3"
        mFileTypes["494433"] = "mp3" //id3
        mFileTypes["664c61"] = "flac"//664c6143
        mFileTypes["667479"] = "m4a"//跳过4位得到ftyp 66747970
    }
}
