package io.github.kineks.neteaseviewer

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lzx.starrysky.OnPlayerEventListener
import com.lzx.starrysky.SongInfo
import com.lzx.starrysky.StarrySky
import com.lzx.starrysky.manager.PlaybackStage
import io.github.kineks.neteaseviewer.data.local.NeteaseCacheProvider
import io.github.kineks.neteaseviewer.data.local.cacheFile.EmptyMusicState
import io.github.kineks.neteaseviewer.data.local.cacheFile.MusicState
import io.github.kineks.neteaseviewer.data.network.service.NeteaseDataService
import io.github.kineks.neteaseviewer.data.player.PlaybackControls
import io.github.kineks.neteaseviewer.data.setting.SettingValue
import io.github.kineks.neteaseviewer.data.update.Update
import io.github.kineks.neteaseviewer.data.update.UpdateJSON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel : ViewModel() {
    var displayWelcomeScreen by SettingValue(false, "firstTimeLaunch") //mutableStateOf(false)
    var displayPermissionDialog by mutableStateOf(false)
    var updateAppJSON by mutableStateOf(UpdateJSON())
    var hasUpdateApp by mutableStateOf(false)

    var errorWhenPlaying by mutableStateOf(false)
    var selectedMusicStateItem: MusicState by mutableStateOf(EmptyMusicState)
    private val selectedMusicStateItemMap = HashMap<String, MusicState>()

    var isFailure by mutableStateOf(false)
    var isUpdating by mutableStateOf(false)
    var isUpdateComplete by mutableStateOf(false)

    var hadListInited = false
    val songs = mutableListOf(mutableStateListOf<MusicState>())
    val playbackControls = PlaybackControls()

    init {

        /*viewModelScope.launch {
            Setting.firstTimeLaunch.collect { firstTimeLaunch ->
                displayWelcomeScreen = firstTimeLaunch
            }
        }*/

        viewModelScope.launch {
            Update.checkUpdate { json, hasUpdate ->
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
                                    if (selectedMusicStateItem.md5 != stage.songInfo!!.songId) {
                                        selectedMusicStateItem =
                                            selectedMusicStateItemMap[stage.songInfo!!.songId]!!
                                    }
                                }
                            }
                        }
                    }
                }, "Main"
            )
        }

    }

    fun initList(
        init: Boolean = hadListInited,
        updateInfo: Boolean = false,
        callback: () -> Unit = {}
    ) {
        if (!init) {
            viewModelScope.launch {
                hadListInited = true
                reloadSongsList(updateInfo = updateInfo)
                callback()
            }
        }
    }

    suspend fun reloadSongsList(list: List<MusicState>? = null, updateInfo: Boolean = false) {
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

    private suspend fun updateSongsInfo(quantity: Int = 50) {
        isUpdateComplete = false
        isFailure = false

        isUpdating = true

        val updateComplete: (isFailure: Boolean) -> Unit =
            { isFailure ->
                this.isUpdating = false
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
            withContext(Dispatchers.IO) {
                launch {

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
                    val indexList = ArrayList<Int>()
                    repeat(size) {
                        val index = offset + it
                        Log.d(this@MainViewModel::javaClass.name, "update:$index")
                        // 如果缓存里有则跳过
                        if (songs[index].song == null) {
                            val id = songs[index].id
                            ids.add(id)
                            indexList.add(offset + it)
                        }
                    }

                    when (true) {
                        ids.isEmpty() -> {}
                        (ids.size == 1) -> NeteaseDataService.instance.getSong(ids[0])
                        else -> NeteaseDataService.instance.getSong(ids)
                    }

                    indexList.forEach { index ->
                        val id = songs[index].id
                        if (NeteaseDataService.instance.getSongFromCache(id) != null) {
                            songs[index] = songs[index]
                                .reload(NeteaseDataService.instance.getSongFromCache(id))
                            Log.d(
                                this.javaClass.name,
                                "update Song $index : " + songs[index].name
                            )
                        }
                    }

                    ids.clear()
                    indexList.clear()

                    // 如果加载完最后一页
                    if (i == pages)
                        updateComplete.invoke(false)

                }
            }
        }


    }

    fun playMusic(song: MusicState) {
        selectedMusicStateItem = song
        selectedMusicStateItemMap[song.md5] = song
        val info = SongInfo(
            songId = song.md5,
            songUrl = song.file.uri.toString(),
            songName = song.name,
            songCover = song.getAlbumPicUrl(200, 200) ?: "",
            artist = song.artists + " - " + song.album
        )
        StarrySky.with().playMusicByInfo(info)
    }

}