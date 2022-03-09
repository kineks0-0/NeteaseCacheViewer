package io.github.kineks.neteaseviewer

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lzx.starrysky.OnPlayerEventListener
import com.lzx.starrysky.StarrySky
import com.lzx.starrysky.manager.PlaybackStage
import io.github.kineks.neteaseviewer.data.api.Api
import io.github.kineks.neteaseviewer.data.api.Song
import io.github.kineks.neteaseviewer.data.api.SongDetail
import io.github.kineks.neteaseviewer.data.local.*
import io.github.kineks.neteaseviewer.data.local.update.Update
import io.github.kineks.neteaseviewer.data.local.update.UpdateJSON
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


@OptIn(InternalCoroutinesApi::class, DelicateCoroutinesApi::class)
class MainViewModel : ViewModel() {

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://music.163.com/")
        .addConverterFactory(GsonConverterFactory.create())//设置数据解析器
        .build()

    private val api = retrofit.create(Api::class.java)

    var displayWelcomeScreen by mutableStateOf(false)
    var updateJSON by mutableStateOf(UpdateJSON())
    var hasUpdate by mutableStateOf(false)

    var playOnError by mutableStateOf(false)
    var selectedMusicItem: Music by mutableStateOf(EmptyMusic)

    var isUpdating by mutableStateOf(false)
    var isUpdateComplete by mutableStateOf(false)
    var isFailure by mutableStateOf(false)

    // todo: 状态保持与恢复
    private var initList = false
    var songs by mutableStateOf(ArrayList<Music>().toList())
        private set

    init {
        viewModelScope.launch {
            Setting.firstTimeLaunch.collect { firstTimeLaunch ->
                displayWelcomeScreen = firstTimeLaunch
            }
        }
        viewModelScope.launch {
            Update.checkUpdateWithTime { json, hasUpdate ->
                if (hasUpdate) {
                    updateJSON = json ?: UpdateJSON()
                    this@MainViewModel.hasUpdate = true
                }
            }
        }
        viewModelScope.launch {
            StarrySky.with().addPlayerEventListener(
                object : OnPlayerEventListener {
                    override fun onPlaybackStageChange(stage: PlaybackStage) {
                        when (stage.stage) {
                            PlaybackStage.ERROR -> {
                                playOnError = true
                                print(playOnError)
                            }
                            PlaybackStage.SWITCH -> {
                                if (stage.songInfo?.songUrl ==
                                    selectedMusicItem.file.toUri().toString()
                                ) return
                                GlobalScope.launch {
                                    val music = NeteaseCacheProvider.getCacheSongs(
                                        cacheDir = listOf(
                                            NeteaseCacheProvider.NeteaseAppCache(
                                                "", listOf(
                                                    RFile(
                                                        RFile.RFileType.SingleUri,
                                                        stage.songInfo?.songUrl!!
                                                    )
                                                )
                                            )
                                        )
                                    )[0]
                                    selectedMusicItem = updateSongsInfo(music)
                                }
                            }
                        }
                    }
                }, "Main"
            )
        }


    }

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
        initList = true
        //songs.clear()
        //songs.addAll(list ?: NeteaseCacheProvider.getCacheSongs())
        songs = list?.toArrayList() ?: NeteaseCacheProvider.getCacheSongs()
        return songs

    }

    private fun <T> List<T>.toArrayList() = ArrayList<T>(this)
    suspend fun updateSongsInfo(
        music: Music
    ): Music {
        return withContext(Dispatchers.IO) {
            return@withContext api.getSongDetail(music.id).execute().body()?.songs?.get(0)
                ?.let { song ->
                    music.copy(
                        song = song,
                        name = song.name,
                        artists = song.artists.getArtists(),
                        id = song.id
                    )
                } ?: music
        }
    }

    fun updateSongsInfo(
        quantity: Int = 50,
        onUpdate: (songDetail: SongDetail, song: Song) -> Unit = { _, _ -> },
        onUpdateComplete: (songs: List<Music>, isFailure: Boolean) -> Unit = { _, _ -> }
    ) {

        isUpdating = false
        isUpdateComplete = false

        isUpdating = true
        // ?
        val songs = this.songs.toArrayList()

        if (songs.isEmpty()) {
            isFailure = true
            onUpdateComplete.invoke(songs, isFailure)
            isUpdateComplete = true
            return
        }

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
                                if (i == pages) {
                                    isUpdateComplete = true
                                    isFailure = false
                                    onUpdateComplete.invoke(songs, isFailure)
                                }

                            } else {
                                Log.e(this.javaClass.name, "GetSongDetail Failure")
                                if (i == pages) {
                                    isUpdateComplete = true
                                    isFailure = true
                                    onUpdateComplete.invoke(songs, isFailure)
                                }
                            }

                        }

                        override fun onFailure(call: Call<SongDetail>, t: Throwable) {
                            Log.e(this.javaClass.name, call.request().url().toString())
                            Log.e(this.javaClass.name, t.message, t)
                            if (i == pages) {
                                isUpdateComplete = true
                                isFailure = true
                                onUpdateComplete.invoke(songs, isFailure)
                            }
                        }

                    })
                }
            }
        }


    }


}