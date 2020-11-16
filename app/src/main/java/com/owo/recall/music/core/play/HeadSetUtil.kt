package com.owo.recall.music.core.play

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Handler
import android.os.Message
import android.os.Parcelable
import android.util.Log
import android.view.KeyEvent
import com.owo.recall.music.core.play.HeadSetUtil.OnHeadSetListener
import java.util.*


object HeadSetUtil {

    private var headSetListener: OnHeadSetListener? = null


    /**
     * 设置耳机单击双击监听接口 必须在open前设置此接口，否则设置无效
     * @param headSetListener
     */
    fun setOnHeadSetListener(headSetListener: OnHeadSetListener?) {
        this.headSetListener = headSetListener
    }

    /**
     * 为MEDIA_BUTTON 意图注册接收器（注册开启耳机线控监听, 请务必在设置接口监听之后再调用此方法，否则接口无效）
     * @param context
     */
    fun open(context: Context) {
        checkNotNull(headSetListener) { "please set headSetListener" }
        val audioManager = context
            .getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val name = ComponentName(
            context.packageName,
            MediaButtonReceiver::class.java.name
        )

        audioManager.registerMediaButtonEventReceiver(name)
        Log.i(javaClass.name, "opened")
    }

    /**
     * 关闭耳机线控监听
     * @param context
     */
    fun close(context: Context) {
        val audioManager = context
            .getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val name = ComponentName(
            context.packageName,
            MediaButtonReceiver::class.java.name
        )
        audioManager.unregisterMediaButtonEventReceiver(name)
        Log.i(this.javaClass.name, "closed")
    }

    /**
     * 删除耳机单机双击监听接口
     */
    fun delHeadSetListener() {
        headSetListener = null
    }


    /**
     * 获取耳机单击双击接口
     *
     * @return
     */
    fun getOnHeadSetListener(): OnHeadSetListener? {
        return headSetListener
    }

    /**
     * 耳机按钮单双击监听
     */
    interface OnHeadSetListener {
        //单击触发,主线程。 此接口真正触发是在单击操作1秒后 因为需要判断1秒内是否仍监听到点击，有的话那就是双击了
        fun onClick()

        //双击触发，此接口在主线程，可以放心使用
        fun onDoubleClick()

        // 三连击
        fun onThreeClick()
    }

}



/**
 * MEDIA_BUTTON耳机媒体按键广播接收器
 * @author JPH
 * @Date 2015-6-9 下午8:35:40
 */
class MediaButtonReceiver : BroadcastReceiver() {
    private var timer: Timer? = null
    private var headSetListener: OnHeadSetListener? = null
    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "onReceive")
        val intentAction = intent.action
        if (Intent.ACTION_MEDIA_BUTTON == intentAction) {
            val keyEvent: KeyEvent? =
                intent.getParcelableExtra<Parcelable>(Intent.EXTRA_KEY_EVENT) as KeyEvent? //获得KeyEvent对象
            if (headSetListener != null) {
                try {
                    if (keyEvent != null) {
                        if (keyEvent.action == KeyEvent.ACTION_UP) {
                            when (clickCount) {
                                0 -> { //单击
                                    clickCount++
                                    myTimer = MTask()
                                    timer?.schedule(myTimer, 1000)
                                }
                                1 -> { //双击
                                    clickCount++
                                }
                                2 -> { //三连击
                                    clickCount = 0
                                    myTimer?.cancel()
                                    headSetListener?.onThreeClick()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "onReceive: ", e)
                }
            }
        }
        abortBroadcast() //终止广播(不让别的程序收到此广播，免受干扰)
    }

    /**
     * 定时器，用于延迟1秒，判断是否会发生双击和三连击
     */
    internal inner class MTask : TimerTask() {
        override fun run() {
            try {
                if (clickCount == 1) {
                    mhHandler.sendEmptyMessage(1)
                } else if (clickCount == 2) {
                    mhHandler.sendEmptyMessage(2)
                }
                clickCount = 0
            } catch (e: Exception) {
                // TODO: handle exception
            }
        }
    }

    /**
     * 此handle的目的主要是为了将接口在主线程中触发
     * ，为了安全起见把接口放到主线程触发
     */
    @SuppressLint("HandlerLeak")
    var mhHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                1 -> { //单击
                    headSetListener!!.onClick()
                }
                2 -> { //双击
                    headSetListener!!.onDoubleClick()
                }
                3 -> { //三连击
                    headSetListener!!.onThreeClick()
                }
            }
        }
    }

    companion object {
        private val TAG = MediaButtonReceiver::class.java.name
        private var myTimer: MTask? = null

        /**单击次数 */
        private var clickCount = 0
    }

    init {
        timer = Timer(true)
        headSetListener = HeadSetUtil.getOnHeadSetListener()
    }
}