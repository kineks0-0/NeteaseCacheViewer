package io.github.kineks.neteaseviewer

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lzx.starrysky.OnPlayerEventListener
import com.lzx.starrysky.SongInfo
import com.lzx.starrysky.StarrySky
import com.lzx.starrysky.manager.PlaybackStage
import io.github.kineks.neteaseviewer.data.local.*
import io.github.kineks.neteaseviewer.data.network.Network
import io.github.kineks.neteaseviewer.data.network.Song
import io.github.kineks.neteaseviewer.data.network.SongDetail
import io.github.kineks.neteaseviewer.data.network.service.NeteaseService
import io.github.kineks.neteaseviewer.data.update.Update
import io.github.kineks.neteaseviewer.data.update.UpdateJSON
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.await

class MainViewModel : ViewModel() {
    var displayWelcomeScreen by mutableStateOf(false)
    var updateJSON by mutableStateOf(UpdateJSON())
    var hasUpdate by mutableStateOf(false)

    var errorWhenPlaying by mutableStateOf(false)
    var selectedMusicItem: Music by mutableStateOf(EmptyMusic)

    var isFailure by mutableStateOf(false)
    var isUpdating by mutableStateOf(false)
    var isUpdateComplete by mutableStateOf(false)

    // todo: 状态保持与恢复
    var hadListInited = false
    val songs = mutableStateListOf<Music>()

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
                                errorWhenPlaying = true
                            }
                            PlaybackStage.SWITCH -> {
                                if (stage.songInfo?.songUrl ==
                                    selectedMusicItem.file.toUri().toString()
                                ) return
                                viewModelScope.launch {
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

    fun initList(init: Boolean = hadListInited, callback: () -> Unit = {}) {
        if (!init) {
            viewModelScope.launch {
                reloadSongsList()
                hadListInited = true
                callback()
            }
        }
    }

    suspend fun reloadSongsList(list: List<Music>? = null, updateInfo: Boolean = false) {
        if (songs.isNotEmpty())
            songs.clear()

        if (list != null && list.isNotEmpty()) {
            songs.addAll(list)
        } else {
            songs.addAll(NeteaseCacheProvider.getCacheSongs())
        }

        if (updateInfo)
            updateSongsInfo()

        hadListInited = true
    }

    suspend fun updateSongsInfo(
        music: Music
    ): Music =
        withContext(Dispatchers.IO) {
            try {
                NeteaseService.instance.getSongDetail(music.id).songs[0].let {
                    return@let it.run {
                        music.copy(name = name, artists = artists.getArtists(), song = this)
                    }
                }
            } catch (e: CancellationException) {
                music
            }
        }

    fun updateSongsInfo(
        quantity: Int = 50,
        onUpdate: (songDetail: SongDetail, song: Song) -> Unit = { _, _ -> },
        onUpdateComplete: (songs: List<Music>, isFailure: Boolean) -> Unit = { _, _ -> }
    ) {
        isUpdateComplete = false
        isFailure = false

        isUpdating = true

        val _onUpdateComplete: (songs: List<Music>, isFailure: Boolean) -> Unit =
            { songs, isFailure ->
                isUpdating = false
                this.isUpdateComplete = true
                this.isFailure = isFailure
                onUpdateComplete.invoke(songs, isFailure)
            }

        // 如果列表为空
        if (songs.isEmpty()) {
            _onUpdateComplete.invoke(songs, true)
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
                            // 单页加载,但列表数量小于单页加载数量
                            quantity > songs.size -> songs.size

                            // 最后一页,计算剩下多少
                            i == pages -> songs.size - offset

                            // 其余情况都是单页加载数量
                            else -> quantity
                        }

                    // 对于该页 只有一个 的情况下的分支处理
                    val get: Call<SongDetail> =
                        when (size) {
                            1 -> Network.api.getSongDetail(songs[offset].id)
                            else -> {
                                val ids = Array(size) { 0 }
                                repeat(size) {
                                    ids[it] = songs[offset + it].id
                                }
                                Network.api.getSongsDetail(ids.toURLArray())
                            }
                        }

                    Log.d(this.toString(), get.request().url().toString())

                    try {
                        val songDetail = get.await()
                        songDetail.songs.forEachIndexed { x, song ->
                            // 计算该对象对应列表索引
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
                            onUpdate.invoke(songDetail, song)
                        }
                        // 如果加载完最后一页
                        if (i == pages)
                            _onUpdateComplete.invoke(songs, false)


                    } catch (e: Exception) {

                        Log.e(this.javaClass.name, get.request().url().toString())
                        Log.e(this.javaClass.name, e.message, e)
                        if (i == pages)
                            _onUpdateComplete.invoke(songs, true)

                    }

                }
            }
        }


    }

    fun playMusic(song: Music) {
        selectedMusicItem = song
        val info = SongInfo(
            songId = song.md5,
            songUrl = song.file.toUri().toString(),
            songName = song.name,
            songCover = song.getAlbumPicUrl(200, 200) ?: "",
            artist = song.artists + " - " + song.album
        )
        StarrySky.with().playMusicByInfo(info)
    }

}