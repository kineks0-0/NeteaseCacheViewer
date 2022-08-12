package io.github.kineks.neteaseviewer.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import io.github.kineks.neteaseviewer.data.local.NeteaseCacheProvider
import io.github.kineks.neteaseviewer.data.local.cacheFile.MusicState
import kotlinx.coroutines.flow.Flow

object NeteaseCacheRepository {

    fun getMusicStatePagingData(
        neteaseAppCaches: List<NeteaseCacheProvider.NeteaseAppCache> = NeteaseCacheProvider.cacheDir
    ): Flow<PagingData<MusicState>> = Pager(
        config = PagingConfig(PagingConfig.MAX_SIZE_UNBOUNDED),
        pagingSourceFactory = { NeteaseCachePagingSource(neteaseAppCaches) }
    ).flow

}