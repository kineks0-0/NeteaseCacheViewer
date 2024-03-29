package io.github.kineks.neteaseviewer.data.network

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface Api {
    @GET("api/song/detail")
    fun getSongDetail(@Query("id") id: Int, @Query("ids") ids: String = "[$id]"): Call<SongDetail>

    @GET("api/song/detail")
    fun getSongsDetail(@Query("ids") ids: String): Call<SongDetail>
}