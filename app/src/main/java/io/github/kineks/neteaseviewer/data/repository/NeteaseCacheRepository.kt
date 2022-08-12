package io.github.kineks.neteaseviewer.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import io.github.kineks.neteaseviewer.data.local.NeteaseCacheProvider
import io.github.kineks.neteaseviewer.data.local.cacheFile.MusicState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow

object NeteaseCacheRepository {

    fun getMusicStatePagingData(
        coroutineScope: CoroutineScope = GlobalScope,
        neteaseAppCaches: List<NeteaseCacheProvider.NeteaseAppCache> = NeteaseCacheProvider.cacheDir
    ): Flow<PagingData<MusicState>> = Pager(
        config = PagingConfig(neteaseAppCaches.size),
        pagingSourceFactory = { NeteaseCachePagingSource(neteaseAppCaches) }
    ).flow.cachedIn(coroutineScope)

}