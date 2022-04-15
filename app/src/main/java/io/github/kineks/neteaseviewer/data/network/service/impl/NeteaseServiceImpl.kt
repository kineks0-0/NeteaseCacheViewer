package io.github.kineks.neteaseviewer.data.network.service.impl

import io.github.kineks.neteaseviewer.data.network.Network
import io.github.kineks.neteaseviewer.data.network.SongDetail
import io.github.kineks.neteaseviewer.data.network.service.NeteaseService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import retrofit2.await

object NeteaseServiceImpl : NeteaseService {
    private val handler = CoroutineExceptionHandler { coroutineContext, throwable ->
        coroutineContext.cancel(CancellationException("Error when requesting network.", throwable))
    }

    override suspend fun getSongDetail(id: Int, ids: String): SongDetail =
        withContext(handler) {
            Network.api.getSongDetail(id, ids).await()
        }

    override suspend fun getSongsDetail(ids: String): SongDetail =
        withContext(handler) {
            Network.api.getSongsDetail(ids).await()
        }
}