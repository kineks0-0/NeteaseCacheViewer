package io.github.kineks.neteaseviewer

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
import io.github.kineks.neteaseviewer.data.player.PlaybackControls
import io.github.kineks.neteaseviewer.data.repository.NeteaseCacheRepository
import io.github.kineks.neteaseviewer.data.setting.SettingValue
import io.github.kineks.neteaseviewer.data.update.Update
import io.github.kineks.neteaseviewer.data.update.UpdateJSON
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    var displayWelcomeScreen by SettingValue(false, "firstTimeLaunch", viewModelScope)
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
    var songs =
        NeteaseCacheRepository.getMusicStatePagingData()//mutableListOf(mutableStateListOf<MusicState>())

    /*val updateInfoFlow: Flow<PagingData<MusicState>> = songs.buffer().transform {
        //it.
    }*/
    val playbackControls = PlaybackControls()

    init {

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
                //reloadSongsList(updateInfo = updateInfo)
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
            songCover = song.getAlbumPicUrl(200, 200) ?: "",
            artist = song.artists + " - " + song.album
        )
        StarrySky.with().playMusicByInfo(info)
    }

}