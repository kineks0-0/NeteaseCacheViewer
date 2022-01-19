package io.github.kineks.neteaseviewer

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.lzx.starrysky.StarrySky
import com.lzx.starrysky.notification.INotification
import com.lzx.starrysky.notification.NotificationConfig
import dagger.hilt.android.HiltAndroidApp
import io.github.kineks.neteaseviewer.data.player.ExoPlayback

@HiltAndroidApp
class App : Application() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()
        context = this

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
        )/*
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
    }

}