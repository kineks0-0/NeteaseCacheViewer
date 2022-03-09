package io.github.kineks.neteaseviewer

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
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
        val isAndroidPorAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
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
        StarrySky.setNotificationConfig(
            NotificationConfig.create {
                this.targetClass { MainActivity::javaClass.toString() }
            }
        )

        // 标记 jaudiotagger 的目标平台为安卓
        TagOptionSingleton.getInstance().isAndroid = true

        // 设置 bugly 信息
        CrashReport.initCrashReport(applicationContext, "47b671f209", BuildConfig.DEBUG)

    }

}