package io.github.kineks.neteaseviewer

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
import io.github.kineks.neteaseviewer.data.local.NeteaseCacheProvider
import io.github.kineks.neteaseviewer.data.local.Setting
import io.github.kineks.neteaseviewer.data.local.cacheFile.EmptyMusic
import io.github.kineks.neteaseviewer.data.local.cacheFile.Music
import io.github.kineks.neteaseviewer.data.network.service.NeteaseDataService
import io.github.kineks.neteaseviewer.data.update.Update
import io.github.kineks.neteaseviewer.data.update.UpdateJSON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel : ViewModel() {
    var displayWelcomeScreen by mutableStateOf(false)
    var updateAppJSON by mutableStateOf(UpdateJSON())
    var hasUpdateApp by mutableStateOf(false)

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
                    updateAppJSON = json ?: UpdateJSON()
                    this@MainViewModel.hasUpdateApp = true
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
                                viewModelScope.launch {
                                    selectedMusicItem = NeteaseCacheProvider
                                        .getCacheSongs(stage.songInfo?.songUrl!!)
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
                hadListInited = true
                reloadSongsList()
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

    fun updateSongsInfo(quantity: Int = 50) {
        isUpdateComplete = false
        isFailure = false

        isUpdating = true

        val updateComplete: (isFailure: Boolean) -> Unit =
            { isFailure ->
                isUpdating = false
                this.isUpdateComplete = true
                this.isFailure = isFailure
            }

        // 如果列表为空
        if (songs.isEmpty()) {
            updateComplete.invoke(true)
            return
        }

        // 计算分页数量
        var pages = songs.size / quantity - 1
        if (songs.size % quantity != 0)
            pages++

        for (i in 0..pages) {
            viewModelScope.launch {
                withContext(Dispatchers.IO) {

                    // 对于 list 索引的偏移值
                    val offset = i * quantity

                    // 该页的数量
                    val size =
                        when (true) {
                            // 单页加载,但列表数量小于单页加载数量
                            (quantity > songs.size) -> songs.size

                            // 最后一页,计算剩下多少
                            (i == pages) -> songs.size - offset

                            // 其余情况都是单页加载数量
                            else -> quantity
                        }


                    val ids = ArrayList<Int>()
                    repeat(size) {
                        val id = songs[offset + it].id
                        // 如果缓存里有则跳过
                        if (NeteaseDataService.instance.getSongFromCache(id) == null) {
                            ids.add(id)
                        }
                    }
                    when (true) {
                        ids.isEmpty() -> {}
                        (ids.size == 1) -> NeteaseDataService.instance.getSong(ids[0])
                        else -> NeteaseDataService.instance.getSong(ids)
                    }

                    // 如果加载完最后一页
                    if (i == pages)
                        updateComplete.invoke(false)

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