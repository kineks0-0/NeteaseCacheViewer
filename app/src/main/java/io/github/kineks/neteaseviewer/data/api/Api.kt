package io.github.kineks.neteaseviewer.data.api

import io.github.kineks.neteaseviewer.toURLArray
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface Api {

    @GET("api/song/detail")
    fun getSongDetail(@Query("id")id: Int,@Query("ids")ids: String = "[$id]" ) : Call<SongDetail>

    // todo: 添加请求缓存
    @GET("api/song/detail")
    fun getSongsDetail(@Query("ids")ids: String) : Call<SongDetail>

    //fun ids2str(ids: Array<Int>) : String = ids.toURLArray()

}