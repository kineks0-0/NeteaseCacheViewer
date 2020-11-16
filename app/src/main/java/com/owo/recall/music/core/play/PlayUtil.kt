package com.owo.recall.music.core.play

import android.content.Context
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.MediaPlayer
import android.media.MediaScannerConnection
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.owo.recall.music.CoreApplication
import com.owo.recall.music.R
import com.owo.recall.music.core.MusicFileProvider
import com.owo.recall.music.core.NeteaseMusicSong
import com.owo.recall.music.core.play.HeadSetUtil.OnHeadSetListener
import java.io.File
import kotlin.concurrent.thread
import kotlin.experimental.xor


object PlayUtil {

    private val TAG = javaClass.canonicalName

    var mediaPlayer = getNewMediaPlayer()

    //private var song: NeteaseMusicSong = NeteaseMusicSong(File("404"),-1L,-1L,-1L,"",-1,"")
    private val mAudioManager: AudioManager by lazy { CoreApplication.context.getSystemService(
        Context.AUDIO_SERVICE
    ) as AudioManager }
    private val mAudioFocusChange: OnAudioFocusChangeListener =
        object : OnAudioFocusChangeListener {
            override fun onAudioFocusChange(focusChange: Int) {
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        //长时间丢失焦点,当其他应用申请的焦点为AUDIOFOCUS_GAIN时，
                        //会触发此回调事件，例如播放QQ音乐，网易云音乐等
                        //通常需要暂停音乐播放，若没有暂停播放就会出现和其他音乐同时输出声音
                        Log.d(TAG, "AUDIOFOCUS_LOSS")
                        //StopPlay();
                        playMode.pausePlay()

                        //释放焦点，该方法可根据需要来决定是否调用
                        //若焦点释放掉之后，将不会再自动获得
                        mAudioManager.abandonAudioFocus(this)
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        //短暂性丢失焦点，当其他应用申请AUDIOFOCUS_GAIN_TRANSIENT或AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE时，
                        //会触发此回调事件，例如播放短视频，拨打电话等。
                        //通常需要暂停音乐播放
                        playMode.pausePlay()
                        Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT")
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->                     //短暂性丢失焦点并作降音处理
                        Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK")
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        //当其他应用申请焦点之后又释放焦点会触发此回调
                        //可重新播放音乐
                        playMode.continuesPlay()
                        Log.d(TAG, "AUDIOFOCUS_GAIN")
                    }
                }
            }
        }

    private var over = false
    //public var isPlaying = false
    var onPlayListener: OnPlayListener = object : OnPlayListener{
        override fun onPlayBegins(song: NeteaseMusicSong, songList: ArrayList<NeteaseMusicSong>, index: Int){}
        override fun onPlayStop(){}
        override fun onPlayEnd(){}
        override fun onPlayPause(){}
        override fun onPlayContinues(){}
        override fun onRest(){}
        override fun onError(){}
    }//默认空接口

    val headSetListener: OnHeadSetListener = object : OnHeadSetListener {
        override fun onClick() {
            //单击: 播放/暂停;
            Log.i(HeadSetUtil::class.java.name, "单击")
            playMode.play()
        }

        override fun onDoubleClick() {
            //双击: 下一首;
            Log.i(HeadSetUtil::class.java.name, "双击")
            playMode.nextSong()
        }

        override fun onThreeClick() {
            //三击: 上一首;
            Log.i(HeadSetUtil::class.java.name, "三连击")
            playMode.previousSong()
        }
    }


    public var playMode: PlayMode = object : PlayMode {


        val SongLoop = 0
        val ListPlay = 1
        val ListLoop = 2
        val RandomPlay = 3
        //val playMode = 0

        var playModeID: Int = ListPlay
        var index: Int = -1
        var songList: ArrayList<NeteaseMusicSong> = ArrayList()
        var playList: ArrayList<NeteaseMusicSong> = ArrayList()
        //var auto: Boolean = false


        override fun switchPlayMode() {
            playModeID = when(playModeID) {
                RandomPlay -> SongLoop
                else -> playModeID + 1
            }
            switchPlayMode(playModeID)
        }

        override fun switchPlayMode(PlayModeID: Int) {
            playModeID = PlayModeID

            when(PlayModeID){
                SongLoop -> {
                    playList.clear()
                    playList.addAll(songList)
                }
                ListLoop -> {
                    playList.clear()
                    playList.addAll(songList)
                }
                ListPlay -> {
                    playList.clear()
                    playList.addAll(songList)
                }
                RandomPlay -> {
                    playList.clear()
                    playList.addAll(songList)
                    playList.shuffle()
                }
            }
        }

        override fun updata(songList: ArrayList<NeteaseMusicSong>, index: Int) {
            this.songList = songList
            this.index = index
            switchPlayMode(playModeID)
        }


        override fun play(){
            if (index==-1) {
                if (songList.size!=0) play(songList,0)
                return
            }
            if (mediaPlayer.isPlaying) pausePlay() else continuesPlay()
        }

        override fun play(songList: ArrayList<NeteaseMusicSong>, index: Int) {
            this.songList = songList
            this.index = index
            switchPlayMode(playModeID)
            playSong(this.index)
        }

        override fun playSong(index: Int) {

            this.index = index
            if (index < 0 || index > playList.lastIndex || playList.size == 0) return
            over = false

            /*if (index == Index && auto) {
                auto = false
                if (playModeID == ListLoop) Index = 0 else return //ListPlay 在这里播放完停止,loop 则把 where 移回第一个继续
            } else Index = index
            auto = false*/

            val song: NeteaseMusicSong = playList[index]

            getDecodeNeteaseFile(song, object : DecodeCompleteCallBack2 {
                override fun completeCallBack(
                    decodeByteArray: ByteArray,
                    decodeFile: File,
                    incompleteFile: Boolean
                ) {

                    if (mediaPlayer.isPlaying) mediaPlayer.stop()
                    mediaPlayer.reset()
                    onPlayListener.onRest()

                    try {

                        if (!incompleteFile || decodeByteArray.size > 20 * 1024 * 1024 && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                            decodeFile.writeBytes(decodeByteArray)

                            if (decodeFile.exists()) {
                                mediaPlayer.setDataSource(decodeFile.absolutePath)
                                song.decodeFile = decodeFile
                                decodeFile.deleteOnExit()
                                //mediaPlayer.setDataSource(CoreApplication.context,Uri.fromFile(decodeFile))
                                //mediaPlayer.setDataSource(FileInputStream(decodeFile).fd)
                            } else TODO("Not yet implemented")

                        } else {

                            CoreApplication.toast(CoreApplication.context.getString(R.string.incomplete_file) + " " + decodeByteArray.size + " / " + song.filesize)

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                mediaPlayer.setDataSource(ByteArrayMediaDataSource(decodeByteArray))
                            } else {
                                CoreApplication.toast(CoreApplication.context.getString(R.string.incomplete_file) + " " + decodeByteArray.size + " / " + song.filesize)
                                val cacheDataFile =
                                    MusicFileProvider.getOtherCacheFile("CacheDecodeNeteaseFile.song")
                                cacheDataFile.writeBytes(decodeByteArray)
                                if (cacheDataFile.exists()) {
                                    mediaPlayer.setDataSource(cacheDataFile.absolutePath)
                                    cacheDataFile.deleteOnExit()
                                    //mediaPlayer.setDataSource(CoreApplication.context,Uri.fromFile(decodeFile))
                                    //mediaPlayer.setDataSource(FileInputStream(decodeFile).fd)
                                } else TODO("Not yet implemented")
                            }
                        }

                    } catch (e: Exception) {
                        Log.e(this@PlayUtil.toString(), e.message, e)
                        //isPlaying = false
                        over = true
                        onPlayListener.onError()
                        mediaPlayer.reset()
                        onPlayListener.onRest()
                        return
                    }

                    mediaPlayer.setOnPreparedListener {

                        mediaPlayer.start()
                        mAudioManager.requestAudioFocus(
                            mAudioFocusChange,
                            AudioManager.STREAM_MUSIC,
                            AudioManager.AUDIOFOCUS_GAIN
                        )

                        //isPlaying = true
                        onPlayListener.onPlayBegins(song, playList, index)
                    }
                    mediaPlayer.prepareAsync()
                    //mediaPlayer.prepare()

                }

            })

            /*getDecodeNeteaseFile(song,object : DecodeCompleteCallBack {
                override fun completeCallBack(decodeFile: File) {
                    if (mediaPlayer.isPlaying) mediaPlayer.stop()
                    mediaPlayer.reset()
                    onPlayListener.onRest()

                    if (decodeFile.exists()) {
                        mediaPlayer.setDataSource(decodeFile.absolutePath)
                        //mediaPlayer.setDataSource(CoreApplication.context,Uri.fromFile(decodeFile))
                        //mediaPlayer.setDataSource(FileInputStream(decodeFile).fd)
                    } else TODO("Not yet implemented")

                    mediaPlayer.setVolume(1.0F, 1.0F);
                    mediaPlayer.isLooping = false;
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                    mAudioManager.requestAudioFocus(mAudioFocusChange, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                    onPlayListener.PlayBegins(song,playList,index)
                }

            })*/
        }

        override fun continuesPlay() {
            if (index == -1) {
                playSong(0)
                return
            }
            mediaPlayer.start()
            //isPlaying = true
            onPlayListener.onPlayContinues()
        }

        override fun pausePlay() {
            //if (over) return
            mediaPlayer.pause()
            //isPlaying = false
            onPlayListener.onPlayPause()
        }

        override fun getPreviousSong(onUserDo: Boolean, offset: Int): Int {
            //auto = true
            return if (playModeID == SongLoop) {
                index
            } else {
                var previousPosition = index - 1 - offset
                if (previousPosition < 0) previousPosition = playList.lastIndex
                previousPosition
            }
        }

        override fun getNextSong(onUserDO: Boolean, offset: Int): Int {
            //auto = true
            return if (playModeID == SongLoop) {
                index
            } else {
                var nextPosition = index + 1 + offset
                if (nextPosition > playList.lastIndex ) {
                    if (onUserDO || playModeID == ListLoop && playModeID == RandomPlay) nextPosition = 0 else return -1 //ListPlay 在这里播放完停止,loop 则把 where 移回第一个继续
                }

                nextPosition
            }
        }

        override fun getPlayingSong(): NeteaseMusicSong {
            return playList[index]
        }

        override fun previousSong(onUserDo: Boolean, offset: Int) {
            playSong(getPreviousSong(onUserDo, offset))
        }

        override fun nextSong(onUserDo: Boolean, offset: Int) {
            playSong(getNextSong(onUserDo, offset))
        }

        override fun previousSong() {
            previousSong(false, 0)
        }

        override fun nextSong() {
            nextSong(false, 0)
        }

    }
    //由 PlayMode 完成播放控制和列表播放

    interface OnPlayListener {
        fun onPlayBegins(song: NeteaseMusicSong, songList: ArrayList<NeteaseMusicSong>, index: Int)
        fun onPlayStop()
        fun onPlayEnd()
        fun onPlayPause()
        fun onPlayContinues()
        fun onRest()
        fun onError()
    }
    interface PlayMode {

        fun switchPlayMode()
        fun switchPlayMode(PlayModeID: Int)

        fun updata(songList: ArrayList<NeteaseMusicSong>, index: Int)

        fun play()
        fun play(songList: ArrayList<NeteaseMusicSong>, index: Int)

        fun getPreviousSong(onUserDo: Boolean, offset: Int): Int
        fun getNextSong(onUserDO: Boolean, offset: Int): Int
        fun getPlayingSong() : NeteaseMusicSong

        fun pausePlay()
        fun continuesPlay()

        fun previousSong(onUserDo: Boolean, offset: Int)
        fun previousSong()
        fun nextSong(onUserDo: Boolean, offset: Int)
        fun nextSong()
        fun playSong(index: Int)

        /*companion object {
            const val SongLoop = 0
            const val ListPlay = 1
            const val ListLoop = 2
            const val RandomPlay = 3
            const val playMode = 0
        }*/
    }

    interface DecodeCompleteCallBack {
        fun completeCallBack(decodeFile: File, incompleteFile: Boolean)
    }
    interface DecodeCompleteCallBack2 {
        fun completeCallBack(
            decodeByteArray: ByteArray,
            cacheDataFile: File,
            incompleteFile: Boolean
        )
    }



    //@JvmName("getMediaPlayer1")
    private fun getNewMediaPlayer(): MediaPlayer {

        val mediaPlayer = MediaPlayer()
        mediaPlayer.setOnCompletionListener {
            //isPlaying = false
            over = true//State = getContext().getString(R.string.playEnd)
            playMode.nextSong()
            onPlayListener.onPlayEnd()
        }

        // 设置播放错误监听
        mediaPlayer.setOnErrorListener { mp, _, _ ->
            //isPlaying = false
            over = true
            onPlayListener.onError()
            mp.reset()
            onPlayListener.onRest()
            true
        }

        // 设置设备进入锁状态模式-可在后台播放或者缓冲音乐-CPU一直工作
        mediaPlayer.setWakeMode(CoreApplication.context, PowerManager.PARTIAL_WAKE_LOCK)
        mediaPlayer.setVolume(1.0F, 1.0F)
        mediaPlayer.isLooping = false

        return mediaPlayer
    }

    fun getDecodeNeteaseFile(
        encodeSong: NeteaseMusicSong,
        decodeCompleteCallBack: DecodeCompleteCallBack,
        key: Byte = -93
    ) {
        if (!encodeSong.songFile.exists()) return
        thread {
            var cacheDataFile: File = MusicFileProvider.getDecodeCacheFile(encodeSong.songFile.name)
            val incompleteFile: Boolean// = false
            if (!cacheDataFile.exists()) {
                val encodeByteArray = encodeSong.songFile.readBytes()
                val decodeByteArray = ByteArray(encodeByteArray.size)
                for ( i in encodeByteArray.indices ) {
                    decodeByteArray[i] = encodeByteArray[i] xor key
                }
                if (encodeSong.filesize == decodeByteArray.size.toLong()) {
                    cacheDataFile.writeBytes(decodeByteArray)
                    incompleteFile = false
                } else {
                    incompleteFile = true
                  CoreApplication.toast(CoreApplication.context.getString(R.string.incomplete_file) + " " + decodeByteArray.size + " / " + encodeSong.filesize)
                    cacheDataFile = MusicFileProvider.getOtherCacheFile("CacheDecodeNeteaseFile.song")
                    cacheDataFile.writeBytes(decodeByteArray)
                }
                encodeSong.decodeFile = cacheDataFile
                decodeCompleteCallBack.completeCallBack(cacheDataFile, incompleteFile)
            } else {
                decodeCompleteCallBack.completeCallBack(cacheDataFile, false)
            }
        }
    }

    fun getDecodeNeteaseFile(
        encodeSong: NeteaseMusicSong,
        decodeCompleteCallBack2: DecodeCompleteCallBack2,
        key: Byte = -93
    ) {
        if (!encodeSong.songFile.exists()) return
        thread {
            val cacheDataFile: File = MusicFileProvider.getDecodeCacheFile(encodeSong.songFile.name)

            if (!cacheDataFile.exists()) {

                val encodeByteArray = encodeSong.songFile.readBytes()
                val decodeByteArray = ByteArray(encodeByteArray.size)

                for ( i in encodeByteArray.indices ) {
                    decodeByteArray[i] = encodeByteArray[i] xor key
                }

                if (encodeSong.filesize == decodeByteArray.size.toLong()) {
                    decodeCompleteCallBack2.completeCallBack(decodeByteArray, cacheDataFile, false)
                } else {
                    //CoreApplication.toast(CoreApplication.context.getString(R.string.incomplete_file) + " " + decodeByteArray.size + " / " + encodeSong.filesize)
                    decodeCompleteCallBack2.completeCallBack(decodeByteArray, cacheDataFile, true)
                }
                //encodeSong.decodeFile = cacheDataFile
            } else {
                decodeCompleteCallBack2.completeCallBack(
                    cacheDataFile.readBytes(),
                    cacheDataFile,
                    false
                )
            }
        }
    }

    /**
     * 通知android媒体库更新文件夹
     *
     * @param filePath ilePath 文件绝对路径，、/sda/aaa/jjj.jpg
     */
    fun scanFile(context: Context, filePath: String) {
        try {
            MediaScannerConnection.scanFile(
                context, arrayOf(filePath), null
            ) { path, uri ->
                Log.i("*******", "Scanned $path:")
                Log.i("*******", "-> uri=$uri")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun onStop() {
        mediaPlayer.release()
    }
}