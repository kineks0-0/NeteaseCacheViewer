package io.github.kineks.neteaseviewer

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Build
import com.lzx.starrysky.StarrySky
import com.lzx.starrysky.notification.INotification
import com.lzx.starrysky.notification.NotificationConfig
import com.tencent.bugly.crashreport.CrashReport
import ealvatag.tag.TagOptionSingleton
import io.github.kineks.neteaseviewer.data.player.ExoPlayback

class App : Application() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
        val isAndroidQorAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        val isAndroidRorAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    }

    override fun onCreate() {
        super.onCreate()
        context = this

        // 初始化播放控制类
        StarrySky
            .init(this)
            .setPlayback(ExoPlayback(this, true))
            .setNotificationType(INotification.SYSTEM_NOTIFICATION)
            .setNotificationSwitch(true)
            .setOpenCache(false)
            .apply()
        StarrySky.bindService()
        //StarrySky.openNotification()
        StarrySky.setNotificationConfig(
            NotificationConfig.create {
                this.targetClass { MainActivity::javaClass.toString() }
            }
        )
        /*
        StarrySky.setNotificationFactory(
            object : NotificationManager.NotificationFactory {
                val SYSTEM_NOTIFICATION_FACTORY: NotificationManager.NotificationFactory = object :
                    NotificationManager.NotificationFactory {
                    override fun build(
                        context: Context, config: NotificationConfig?
                    ): INotification {
                        return if (config == null) SystemNotification(context) else SystemNotification(context, config)
                    }
                }
                override fun build(context: Context, config: NotificationConfig?): INotification =
                    SYSTEM_NOTIFICATION_FACTORY.build(context,config)
            }
        )*/

        // 标记 jaudiotagger 的目标平台为安卓
        TagOptionSingleton.getInstance().isAndroid = true

        // 设置 bugly 信息
        CrashReport.initCrashReport(applicationContext, "47b671f209", BuildConfig.DEBUG)

    }

}