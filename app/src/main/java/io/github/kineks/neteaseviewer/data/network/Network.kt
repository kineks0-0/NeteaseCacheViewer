package io.github.kineks.neteaseviewer.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object Network {
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://music.163.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: Api = retrofit.create(Api::class.java)
}