package io.github.kineks.neteaseviewer

import android.annotation.SuppressLint
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import com.lzx.starrysky.StarrySky
import com.lzx.starrysky.StarrySkyInstall
import com.lzx.starrysky.notification.INotification
import com.lzx.starrysky.notification.NotificationConfig
import com.lzx.starrysky.notification.NotificationManager
import ealvatag.tag.TagOptionSingleton
import io.github.kineks.neteaseviewer.data.player.SystemNotification
import io.github.kineks.neteaseviewer.data.player.exoplayer.ExoPlayback


class App : Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context

        // 当前系统版本 等于或大于 “X” 版本
        val isAndroidPorAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        val isAndroidQorAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        val isAndroidRorAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

        const val REQUEST_CODE_FOR_DIR = 100

        fun copyText(text: String) {
            //获取剪贴板管理器：
            val cm: ClipboardManager =
                context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            // 创建普通字符型ClipData
            val mClipData = ClipData.newPlainText("Label", text)
            // 将ClipData内容放到系统剪贴板里。
            cm.setPrimaryClip(mClipData)
        }
    }

    override fun onCreate() {
        super.onCreate()
        context = this

        AppLib().init(this)

        StarrySky.openNotification()
        // 初始化播放控制类
        StarrySkyInstall
            .init(this)
            .setPlayback(ExoPlayback(this, true))
            .setNotificationType(INotification.CUSTOM_NOTIFICATION)
            .setNotificationFactory(
                object : NotificationManager.NotificationFactory {
                    override fun build(
                        context: Context,
                        config: NotificationConfig?
                    ): INotification =
                        SystemNotification(context, config!!)
                })
            .setNotificationConfig(
                NotificationConfig.create {
                    targetClass { "io.github.kineks.neteaseviewer.MainActivity" }
                    pauseDrawableRes { com.google.android.exoplayer2.ui.R.drawable.exo_notification_pause }
                    playDrawableRes { com.google.android.exoplayer2.ui.R.drawable.exo_notification_play }
                    skipNextDrawableRes { com.google.android.exoplayer2.ui.R.drawable.exo_notification_next }
                    skipPreviousDrawableRes { com.google.android.exoplayer2.ui.R.drawable.exo_notification_previous }
                })
            .setNotificationSwitch(true)
            .setNotificationType(INotification.SYSTEM_NOTIFICATION)
            .setOpenCache(false)
            //.isStartService(false)
            .startForegroundByWorkManager(true)
            //.onlyStartService(false)
            .connService(false)
            .setDebug(true)
            .apply()


        // 标记 jaudiotagger 的目标平台为安卓
        TagOptionSingleton.getInstance().isAndroid = true


    }
}