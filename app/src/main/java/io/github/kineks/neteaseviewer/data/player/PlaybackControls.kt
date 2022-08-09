package io.github.kineks.neteaseviewer.data.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.kineks.neteaseviewer.data.local.cacheFile.MusicState
import io.github.kineks.neteaseviewer.data.setting.SettingValue

val PlaybackControls.TAG: String
    get() = "PlaybackControls"

class PlaybackControls(

) {

    val list: MutableList<MusicState> = listOf<MusicState>().toMutableList()
    var isPlaying by mutableStateOf(false)
    var isLoop: Boolean by SettingValue(false, "REPEAT_MODE_TYPE.isLoop")
    val repeatModeTypeString = SettingValue(name = "repeatModeType", defValue = "REPEAT_MODE_NONE")
    var repeatModeType: RepeatModeType = RepeatModeType.valueOf(repeatModeTypeString.value)
        set(value) {
            repeatModeTypeString.value = field.name
            field = value
        }

    /*init {
        Log.d(TAG, "repeatModeType : $repeatModeType")
        Log.d(TAG,"isLoop : $isLoop")
        repeatModeType = RepeatModeType.REPEAT_MODE_ONE
        isLoop = !isLoop
    }*/


    fun RepeatModeType.ofID(id: Int): RepeatModeType = when (id) {
        1 -> RepeatModeType.REPEAT_MODE_NONE
        2 -> RepeatModeType.REPEAT_MODE_ONE
        3 -> RepeatModeType.REPEAT_MODE_SHUFFLE
        4 -> RepeatModeType.REPEAT_MODE_REVERSE
        else -> RepeatModeType.REPEAT_MODE_NONE
    }

    enum class RepeatModeType() {
        /*  顺序播放 */
        REPEAT_MODE_NONE,

        /* 单曲播放 */
        REPEAT_MODE_ONE,

        /* 随机播放 */
        REPEAT_MODE_SHUFFLE,

        /* 倒序播放 */
        REPEAT_MODE_REVERSE

    }


}

