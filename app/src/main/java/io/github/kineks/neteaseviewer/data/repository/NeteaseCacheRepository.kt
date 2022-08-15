package io.github.kineks.neteaseviewer.data.repository

import io.github.kineks.neteaseviewer.data.local.NeteaseCacheProvider
import io.github.kineks.neteaseviewer.data.local.cacheFile.MusicState

object NeteaseCacheRepository {
    lateinit var source: NeteaseCachePagingSource

    suspend fun getMusicStateList(
        neteaseAppCaches: List<NeteaseCacheProvider.NeteaseAppCache> = NeteaseCacheProvider.cacheDir
    ): MutableList<MusicState> {
        source = NeteaseCachePagingSource(neteaseAppCaches)
        return source.load()
    }

    suspend fun updateMusicStateList(
        list: MutableList<MusicState>
    ) = source.updateSongsInfo(list)


}