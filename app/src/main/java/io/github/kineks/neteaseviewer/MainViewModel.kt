package io.github.kineks.neteaseviewer

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kineks.neteaseviewer.data.api.Api
import io.github.kineks.neteaseviewer.data.api.Song
import io.github.kineks.neteaseviewer.data.api.SongDetail
import io.github.kineks.neteaseviewer.data.local.Music
import io.github.kineks.neteaseviewer.data.local.NeteaseCacheProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class MainViewModel : ViewModel() {

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://music.163.com/")
        .addConverterFactory(GsonConverterFactory.create())//设置数据解析器
        .build()

    private val api = retrofit.create(Api::class.java)


    // todo: 状态保持与恢复
    private var initList = false
    var songs by mutableStateOf(ArrayList<Music>().toList())
        private set

    fun initList(init: Boolean = initList, callback: () -> Unit = {}) {
        if (!init) {
            viewModelScope.launch {
                reloadSongsList()
                initList = true
                callback.invoke()
            }
        }
    }

    suspend fun reloadSongsList(list: List<Music>? = null): List<Music> {

        //songs.clear()
        //songs.addAll(list ?: NeteaseCacheProvider.getCacheSongs())
        songs = list?.toArrayList() ?: NeteaseCacheProvider.getCacheSongs()
        return songs

    }

    private fun <T> List<T>.toArrayList() = ArrayList<T>(this)

    fun updateSongsInfo(
        quantity: Int = 50,
        onUpdate: (songDetail: SongDetail, song: Song) -> Unit = { _, _ -> },
        onUpdateComplete: (songs: List<Music>, isFailure: Boolean) -> Unit = { _, _ -> }
    ) {
        // ?
        val songs = this.songs.toArrayList()//.toMutableList()

        // 计算分页数量
        var pages = songs.size / quantity
        if (songs.size % quantity != 0)
            pages++


        for (i in 1..pages) {

            viewModelScope.launch {
                withContext(Dispatchers.IO) {

                    // 对于 list 索引的偏移值
                    val offset = (i - 1) * quantity
                    // 该页的数量
                    val size =
                        when (true) {
                            quantity > songs.size -> songs.size
                            i == pages -> songs.size - offset
                            else -> quantity
                        }

                    // 对于该页 只有一个 的情况下的分支处理
                    val get: Call<SongDetail> =
                        when (size) {
                            1 -> {
                                api.getSongDetail(songs[offset].id)
                            }
                            else -> {

                                val ids = Array(size) { 0 }
                                repeat(size) {
                                    ids[it] = songs[offset + it].id
                                }
                                api.getSongsDetail(ids.toURLArray())
                            }
                        }

                    Log.d(this.toString(), get.request().url().toString())
                    get.enqueue(object : Callback<SongDetail> {
                        override fun onResponse(
                            call: Call<SongDetail>,
                            response: Response<SongDetail>
                        ) {

                            Log.d(this.javaClass.name, response.message())
                            //Log.d(this.javaClass.name, response.errorBody()?.string() ?: "")

                            if (response.isSuccessful && response.body()?.songs != null) {

                                response.body()!!.songs.forEachIndexed { x, song ->
                                    val index = (i - 1) * quantity + x
                                    Log.d(
                                        this.javaClass.name,
                                        "update Song $index : " + song.name
                                    )
                                    songs[index] = songs[index].copy(
                                        song = song,
                                        name = song.name,
                                        artists = song.artists.getArtists(),
                                        id = song.id
                                    )
                                    onUpdate.invoke(response.body()!!, song)
                                }

                                viewModelScope.launch {
                                    reloadSongsList(songs)
                                }
                                if (i == pages) onUpdateComplete.invoke(songs, false)

                            } else {
                                Log.e(this.javaClass.name, "GetSongDetail Failure")
                                if (i == pages) onUpdateComplete.invoke(songs, true)
                            }

                        }

                        override fun onFailure(call: Call<SongDetail>, t: Throwable) {
                            Log.e(this.javaClass.name, call.request().url().toString())
                            Log.e(this.javaClass.name, t.message, t)
                            if (i == pages) onUpdateComplete.invoke(songs, true)
                        }

                    })
                }
            }
        }


    }


}