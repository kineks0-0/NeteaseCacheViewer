package io.github.kineks.neteaseviewer.data.player

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.lzx.starrysky.SongInfo
import com.lzx.starrysky.playback.FocusInfo
import com.lzx.starrysky.playback.FocusManager
import com.lzx.starrysky.playback.Playback
import com.lzx.starrysky.utils.orDef
import io.github.kineks.neteaseviewer.App


/**
 * 类基于 https://github.com/EspoirX/StarrySky/blob/androidx/starrysky/src/main/java/com/lzx/starrysky/playback/ExoPlayback.kt 修改
 * 移除了对其他类型的支持，同时修改和移除已经弃用的 Api
 * isAutoManagerFocus: 是否让播放器自动管理焦点
 */
const val TAG = "ExoPlayback"

class ExoPlayback(
    private val context: Context = App.context,
    private val isAutoManagerFocus: Boolean
) : Playback, FocusManager.OnFocusStateChangeListener {

    private var player: ExoPlayer? = null
    private var mediaSource: MediaSource? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var trackSelectorParameters: DefaultTrackSelector.Parameters? = null
    private var dataSourceFactory: DataSource.Factory? = null

    private var currSongInfo: SongInfo? = null
    private var callback: Playback.Callback? = null
    private val eventListener by lazy { ExoPlayerEventListener() }
    private var sourceTypeErrorInfo: SourceTypeErrorInfo = SourceTypeErrorInfo()
    private var focusManager = FocusManager(context)
    private var hasError = false

    init {
        focusManager.listener = this
    }

    override fun playbackState(): Int {
        return if (player == null) {
            Playback.STATE_IDLE
        } else {
            when (player?.playbackState) {
                Player.STATE_IDLE -> Playback.STATE_IDLE //error或stop
                Player.STATE_BUFFERING -> Playback.STATE_BUFFERING
                Player.STATE_READY -> {
                    if (player?.playWhenReady == true) Playback.STATE_PLAYING else Playback.STATE_PAUSED
                }
                Player.STATE_ENDED -> Playback.STATE_IDLE
                else -> Playback.STATE_IDLE
            }
        }
    }

    override fun isPlaying(): Boolean = player?.playWhenReady == true

    override fun currentStreamPosition(): Long = player?.currentPosition.orDef()

    override fun bufferedPosition(): Long = player?.bufferedPosition.orDef()

    override fun duration(): Long =
        if (player?.duration.orDef() > 0) player?.duration.orDef() else 0

    override var currentMediaId: String = ""

    override fun setVolume(volume: Float) {
        if (volume < 0) {
            player?.volume = 0f
        }
        if (volume > 1) {
            player?.volume = 1f
        }
    }

    override fun getVolume(): Float = player?.volume ?: -1f

    override fun getCurrPlayInfo(): SongInfo? = currSongInfo

    override fun getAudioSessionId(): Int = player?.audioSessionId ?: 0

    private fun getPlayWhenReady() = player?.playWhenReady ?: false

    override fun play(songInfo: SongInfo, isPlayWhenReady: Boolean) {
        val mediaId = songInfo.songId
        if (mediaId.isEmpty()) {
            return
        }
        currSongInfo = songInfo
        val mediaHasChanged = mediaId != currentMediaId
        if (mediaHasChanged) {
            currentMediaId = mediaId
        }
        Log.d(
            TAG,
            "title = " + songInfo.songName +
                    " \n音频是否有改变 = " + mediaHasChanged +
                    " \n是否立即播放 = " + isPlayWhenReady +
                    " \nurl = " + songInfo.songUrl
        )

        //url 处理
        var source = songInfo.songUrl
        if (source.isEmpty()) {
            callback?.onPlaybackError(currSongInfo, "播放 url 为空")
            return
        }
        source = source.replace(" ".toRegex(), "%20") // Escape spaces for URL
        mediaSource = createMediaSource(source)
        if (mediaSource == null) return
        if (mediaHasChanged || player == null) {
            //创建播放器实例
            createExoPlayer()

            player?.setMediaSource(mediaSource!!)
            player?.prepare()
            if (!isAutoManagerFocus) {
                focusManager.updateAudioFocus(getPlayWhenReady(), Playback.STATE_BUFFERING)
            }
        }
        //当错误发生时，如果还播放同一首歌，
        //这时候需要重新加载一下，并且吧进度 seekTo 到出错的地方
        if (sourceTypeErrorInfo.happenSourceError && !mediaHasChanged) {
            player?.setMediaSource(mediaSource!!)
            player?.prepare()
            if (!isAutoManagerFocus) {
                focusManager.updateAudioFocus(getPlayWhenReady(), Playback.STATE_BUFFERING)
            }
            if (sourceTypeErrorInfo.currPositionWhenError != 0L) {
                if (sourceTypeErrorInfo.seekToPositionWhenError != 0L) {
                    player?.seekTo(sourceTypeErrorInfo.seekToPositionWhenError)
                } else {
                    player?.seekTo(sourceTypeErrorInfo.currPositionWhenError)
                }
            }
        }
        Log.d(TAG, "isPlayWhenReady = $isPlayWhenReady")
        Log.d(TAG, "---------------------------------------")
        //如果准备好就播放
        if (isPlayWhenReady) {
            player?.playWhenReady = true
            hasError = false
            if (!isAutoManagerFocus) {
                player?.playbackState?.let { focusManager.updateAudioFocus(getPlayWhenReady(), it) }
            }
        }
    }

    @Synchronized
    private fun createMediaSource(source: String): MediaSource {
        var uri = Uri.parse(source)

        if (dataSourceFactory == null) dataSourceFactory = getDataSourceFactory()
        val extractorsFactory = DefaultExtractorsFactory()

        return ProgressiveMediaSource
            .Factory(
                EncryptedFileDataSourceFactory(
                    dataSourceFactory!!.createDataSource()
                ),
                extractorsFactory
            )
            .createMediaSource(
                MediaItem.fromUri(uri)
            )
    }

    @Synchronized
    private fun createExoPlayer() {
        if (player == null) {
            val extensionRendererMode =
                DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
            val renderersFactory = DefaultRenderersFactory(context)
                .setExtensionRendererMode(extensionRendererMode)


            trackSelectorParameters = DefaultTrackSelector.ParametersBuilder(context).build()
            trackSelector = DefaultTrackSelector(context)
            trackSelector?.parameters = trackSelectorParameters as DefaultTrackSelector.Parameters

            dataSourceFactory = getDataSourceFactory()

            player = ExoPlayer.Builder(context)
                .setRenderersFactory(renderersFactory)
                .setTrackSelector(trackSelector!!)
                .build()

            player?.addListener(eventListener)
            player?.setAudioAttributes(AudioAttributes.DEFAULT, isAutoManagerFocus)
            if (!isAutoManagerFocus) {
                player?.playbackState?.let { focusManager.updateAudioFocus(getPlayWhenReady(), it) }
            }
        }
    }

    /** Returns a [DataSource.Factory].  */
    @Synchronized
    fun getDataSourceFactory(): DataSource.Factory {
        if (dataSourceFactory == null) {
            //context = context.applicationContext
            dataSourceFactory = DefaultDataSource.Factory(context)
        }
        return dataSourceFactory as DataSource.Factory
    }

    override fun stop() {
        player?.stop(true)
        player?.release()
        player?.removeListener(eventListener)
//        player?.removeAnalyticsListener(analyticsListener)
        player = null
        if (!isAutoManagerFocus) {
            focusManager.release()
        }
    }

    override fun pause() {
        player?.playWhenReady = false
        if (!isAutoManagerFocus) {
            player?.playbackState?.let { focusManager.updateAudioFocus(getPlayWhenReady(), it) }
        }
    }

    override fun seekTo(position: Long) {
        player?.seekTo(position)
        sourceTypeErrorInfo.seekToPosition = position
        if (sourceTypeErrorInfo.happenSourceError) {
            sourceTypeErrorInfo.seekToPositionWhenError = position
        }
    }

    override fun onFastForward(speed: Float) {
        player?.let {
            val currSpeed = it.playbackParameters.speed
            val currPitch = it.playbackParameters.pitch
            val newSpeed = currSpeed + speed
            it.playbackParameters = PlaybackParameters(newSpeed, currPitch)
        }
    }

    override fun onRewind(speed: Float) {
        player?.let {
            val currSpeed = it.playbackParameters.speed
            val currPitch = it.playbackParameters.pitch
            var newSpeed = currSpeed - speed
            if (newSpeed <= 0) {
                newSpeed = 0f
            }
            it.playbackParameters = PlaybackParameters(newSpeed, currPitch)
        }
    }

    override fun onDerailleur(refer: Boolean, multiple: Float) {
        player?.let {
            val currSpeed = it.playbackParameters.speed
            val currPitch = it.playbackParameters.pitch
            val newSpeed = if (refer) currSpeed * multiple else multiple
            if (newSpeed > 0) {
                it.playbackParameters = PlaybackParameters(newSpeed, currPitch)
            }
        }
    }

    override fun getPlaybackSpeed(): Float {
        return player?.playbackParameters?.speed ?: 1.0f
    }

    override fun skipToNext() {
        callback?.skipToNext()
    }

    override fun skipToPrevious() {
        callback?.skipToPrevious()
    }

    override fun setCallback(callback: Playback.Callback?) {
        this.callback = callback
    }

    private inner class ExoPlayerEventListener : Player.Listener {

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            var newState = Playback.STATE_IDLE
            when (playbackState) {
                Player.STATE_IDLE -> {
                    //error和stop的时候会是这个状态，这里过滤掉error，避免重复回调
                    newState = if (hasError) Playback.STATE_ERROR else Playback.STATE_IDLE
                }
                Player.STATE_READY -> {
                    newState =
                        if (player?.playWhenReady == true) Playback.STATE_PLAYING else Playback.STATE_PAUSED
                }
                Player.STATE_ENDED -> newState = Playback.STATE_IDLE
                Player.STATE_BUFFERING -> newState = Playback.STATE_BUFFERING
            }
            if (!hasError) {
                callback?.onPlayerStateChanged(currSongInfo, playWhenReady, newState)
            }
            if (playbackState == Player.STATE_READY) {
                sourceTypeErrorInfo.clear()
            }
            if (playbackState == Player.STATE_IDLE) {
                currentMediaId = ""
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            hasError = true
            player?.playerError?.let {
                onPlayerError(it)
                return
            }
            Log.e(TAG, error.message, error)
            val what = "Unknown: $error"
            sourceTypeErrorInfo.happenSourceError = true
            sourceTypeErrorInfo.seekToPositionWhenError = sourceTypeErrorInfo.seekToPosition
            sourceTypeErrorInfo.currPositionWhenError = currentStreamPosition()
            callback?.onPlaybackError(currSongInfo, "ExoPlayer error $what")
        }

        fun onPlayerError(error: ExoPlaybackException) {
            Log.e(TAG, error.message, error)
            hasError = true
            val what: String = when (error.type) {
                ExoPlaybackException.TYPE_SOURCE -> error.sourceException.message.toString()
                ExoPlaybackException.TYPE_RENDERER -> error.rendererException.message.toString()
                ExoPlaybackException.TYPE_UNEXPECTED -> error.unexpectedException.message.toString()
                else -> "Unknown: $error"
            }
            if (error.type == ExoPlaybackException.TYPE_SOURCE) {
                sourceTypeErrorInfo.happenSourceError = true
                sourceTypeErrorInfo.seekToPositionWhenError = sourceTypeErrorInfo.seekToPosition
                sourceTypeErrorInfo.currPositionWhenError = currentStreamPosition()
            }
            callback?.onPlaybackError(currSongInfo, "ExoPlayer error $what")
        }
    }

    override fun focusStateChange(info: FocusInfo) {
        if (isAutoManagerFocus) {
            return
        }
        callback?.onFocusStateChange(
            FocusInfo(
                currSongInfo,
                info.audioFocusState,
                info.playerCommand,
                info.volume
            )
        )
    }
}

/**
 * 发生错误时保存的信息
 */
class SourceTypeErrorInfo {
    var seekToPosition = 0L
    var happenSourceError = false //是否发生资源问题的错误
    var seekToPositionWhenError = 0L
    var currPositionWhenError = 0L //发生错误时的进度

    fun clear() {
        happenSourceError = false //是否发生资源问题的错误
        seekToPosition = 0L
        seekToPositionWhenError = 0L
        currPositionWhenError = 0L //发生错误时的进度
    }
}
