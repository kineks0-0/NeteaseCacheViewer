package com.owo.recall.music.core.play

import android.media.MediaDataSource
import android.os.Build
import androidx.annotation.RequiresApi


@RequiresApi(Build.VERSION_CODES.M)
class ByteArrayMediaDataSource(private val data: ByteArray) : MediaDataSource() {

    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {

        if (position >= data.size) return -1 // -1 indicates EOF

        val endPosition: Int = (position + size).toInt()
        var size2: Int = size
        if (endPosition > data.size)
            size2 -= endPosition - data.size

        System.arraycopy(data, position.toInt(), buffer, offset, size2)
        return size2
    }

    override fun getSize(): Long {
        return data.size.toLong()
    }

    override fun close() {}

}