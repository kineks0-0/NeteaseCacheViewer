package io.github.kineks.neteaseviewer.data.network.service

import io.github.kineks.neteaseviewer.data.network.SongDetail
import io.github.kineks.neteaseviewer.data.network.service.impl.NeteaseServiceImpl

interface NeteaseService {
    companion object {
        val instance: NeteaseService
            get() = NeteaseServiceImpl
    }

    suspend fun getSongDetail(id: Int, ids: String = "[$id]"): SongDetail

    suspend fun getSongsDetail(ids: String): SongDetail
}