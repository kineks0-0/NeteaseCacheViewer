package io.github.kineks.neteaseviewer.data.network.service

import io.github.kineks.neteaseviewer.data.network.Song
import io.github.kineks.neteaseviewer.data.network.service.impl.NeteaseDataServiceImpl

interface NeteaseDataService {
    companion object {
        val instance: NeteaseDataService
            get() = NeteaseDataServiceImpl
    }


    suspend fun getSong(id: Int): Song

    suspend fun getSongFromCache(id: Int): Song?

    suspend fun getSongFromNetwork(id: Int): Song

    suspend fun getSong(ids: List<Int>): List<Song>

    suspend fun getSongFromCache(ids: List<Int>): List<Song?>

    suspend fun getSongFromNetwork(ids: List<Int>): List<Song>

    fun getAlbumPicUrl(id: Int, width: Int = -1, height: Int = -1): String?
}