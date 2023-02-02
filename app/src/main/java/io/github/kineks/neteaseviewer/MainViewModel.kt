package io.github.kineks.neteaseviewer

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lzx.starrysky.OnPlayerEventListener
import com.lzx.starrysky.SongInfo
import com.lzx.starrysky.StarrySky
import com.lzx.starrysky.manager.PlaybackStage
import io.github.kineks.neteaseviewer.data.local.cacheFile.EmptyMusicState
import io.github.kineks.neteaseviewer.data.local.cacheFile.MusicState
import io.github.kineks.neteaseviewer.data.repository.NeteaseCacheRepository
import io.github.kineks.neteaseviewer.data.setting.SettingValue
import io.github.kineks.neteaseviewer.data.update.Update
import io.github.kineks.neteaseviewer.data.update.UpdateJSON
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    var displayWelcomeScreen by SettingValue(false, "firstTimeLaunch", true, viewModelScope)
    var displayPermissionDialog by mutableStateOf(false)
    var updateAppJSON by mutableStateOf(UpdateJSON())
    var hasAppUpdate by mutableStateOf(false)
    var isPlaying by mutableStateOf(false)
    var hadListInited by mutableStateOf(false)

    var refresh by mutableStateOf(false)
    var errorWhenPlaying by mutableStateOf(false)
    var selectedMusicStateItem: MusicState by mutableStateOf(EmptyMusicState)
    private val selectedMusicStateItemMap = HashMap<String, MusicState>()

    val songs = mutableListOf<MusicState>()

    init {

        viewModelScope.launch {
            Update.checkUpdate { json, hasUpdate ->
                if (hasUpdate) {
                    updateAppJSON = json ?: UpdateJSON()
                    this@MainViewModel.hasAppUpdate = true
                }
            }
        }

        viewModelScope.launch {
            StarrySky.with().addPlayerEventListener(
                object : OnPlayerEventListener {
                    override fun onPlaybackStageChange(stage: PlaybackStage) {
                        when (stage.stage) {
                            PlaybackStage.PLAYING -> {
                                isPlaying = true
                            }
                            PlaybackStage.PAUSE -> {
                                isPlaying = false
                            }
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

    suspend fun refresh(list: List<MusicState>? = null, updateInfo: Boolean = false) {
        if (refresh) return
        refresh = true
        try {
            if (songs.isNotEmpty())
                songs.clear()

            if (list != null && list.isNotEmpty()) {
                songs.addAll(list)
            } else {
                /*runWithPrintTimeCostSuspend("MainViewerModel","NeteaseCacheProvider.getCacheSongs()") {
                    NeteaseCacheProvider.getCacheSongs()
                }*/

                songs.addAll(
                    runWithPrintTimeCostSuspend(
                        "MainViewerModel",
                        "NeteaseCacheRepository.getMusicStateList()"
                    ) {
                        NeteaseCacheRepository.getMusicStateList()
                    }
                )
            }

            if (updateInfo)
                runWithPrintTimeCostSuspend(
                    "MainViewerModel",
                    "NeteaseCacheRepository.updateMusicStateList(songs)"
                ) {
                    NeteaseCacheRepository.updateMusicStateList(songs)
                }


            hadListInited = true
            refresh = false
        } catch (e: Exception) {
            refresh = false
            Log.e("MainViewerModel", e.message, e)
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
                refresh(updateInfo = updateInfo)
                callback()
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
            songCover = song.getAlbumPicUrl(300, 300) ?: "",
            artist = song.artists,
            duration = song.duration,
            headData = HashMap<String, String>().apply {
                put("album", song.album)
            }
        )
        Log.d("MainViewModel", "SongInfo : ${info.songId}")
        Log.d("MainViewModel", "SongInfo.url : ${info.songUrl}")
        Log.d("MainViewModel", "MusicState : $song")
        StarrySky.with().playMusicByInfo(info)
    }

}