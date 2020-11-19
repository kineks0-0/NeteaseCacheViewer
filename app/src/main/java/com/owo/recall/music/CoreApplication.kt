package com.owo.recall.music

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.util.Log
import android.widget.Toast
import com.bumptech.glide.Glide

class CoreApplication : Application() {

    companion object {
        lateinit var context: Context

        private val handler = Handler()
        const val REQUEST_PERMISSION_CODE_WRITE_EXTERNAL_STORAGE = 10
        val SettingSharedPreferences by lazy { context.getSharedPreferences("setting",Context.MODE_PRIVATE) }

        fun post(run: Runnable) {
            handler.post(run)
        }

        fun toast(s: String) {
            handler.post {
                Toast.makeText(context,s,Toast.LENGTH_SHORT).show()
            }
        }

        fun toast(r: Int) {
            handler.post {
                Toast.makeText(context, context.getText(r),Toast.LENGTH_SHORT).show()
            }
        }

        /*fun getDefaultSettingSharedPreferences() {
            context.getSharedPreferences("setting",Context.MODE_PRIVATE)
        }*/

        fun getAppVersionCode(): Long {
            var appVersionCode = 0L

            try {
                val packageInfo = context.applicationContext
                    .packageManager
                    .getPackageInfo(context.packageName, 0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    appVersionCode = packageInfo.longVersionCode
                } else {
                    appVersionCode = packageInfo.versionCode.toLong()
                }
            } catch ( e : PackageManager.NameNotFoundException ) {
                e.let { Log.e(CoreApplication::class.toString() , it.message , it) }
            }
            return appVersionCode
        }

        fun getAppVersionName(): String {
            var appVersionName = ""
            try {
                val packageInfo = context.applicationContext
                    .packageManager
                    .getPackageInfo(context.packageName, 0)
                appVersionName = packageInfo.versionName
            } catch ( e : PackageManager.NameNotFoundException ) {
                e.let { Log.e(CoreApplication::class.toString() , it.message , it) }
            }
            return appVersionName
        }

    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Glide.get(context).clearMemory()
    }


}