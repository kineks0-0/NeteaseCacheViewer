package io.github.kineks.neteaseviewer.data.player

import android.net.Uri
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.TransferListener
import io.github.kineks.neteaseviewer.App
import java.io.IOException
import kotlin.experimental.xor

/**
 * 该代码基于 https://stackoverflow.com/questions/38729220/reproducing-encrypted-video-using-exoplayer
 */
class EncryptedDataSource(private var upstream: DataSource) : DataSource {

    private val defaultDataSource = DefaultDataSource(App.context, upstream)

    override fun open(dataSpec: DataSpec): Long = defaultDataSource.open(dataSpec)

    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        val bytesRead = defaultDataSource.read(buffer, offset, readLength)
        var index = 0
        repeat(readLength) {
            buffer[index + offset] = buffer[index + offset].xor(-93)
            index++
        }
        return bytesRead
    }

    override fun addTransferListener(transferListener: TransferListener) {
        defaultDataSource.addTransferListener(transferListener)
    }

    override fun getUri(): Uri? = defaultDataSource.uri

    @Throws(IOException::class)
    override fun close() {
        defaultDataSource.close()
        upstream.close()
    }

}
