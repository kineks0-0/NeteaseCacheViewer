package io.github.kineks.neteaseviewer.data.player

import com.google.android.exoplayer2.upstream.DataSource

/**
 * 该代码基于 https://stackoverflow.com/questions/38729220/reproducing-encrypted-video-using-exoplayer
 */
class EncryptedFileDataSourceFactory(var dataSource: DataSource) : DataSource.Factory {

    override fun createDataSource(): DataSource {
        return EncryptedDataSource(dataSource)
    }
}
