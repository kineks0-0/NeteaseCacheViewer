package com.owo.recall.music.core.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

class CoreService : Service() {

    private val mBinder: CoreBind = CoreBind()

    class CoreBind : Binder()

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

}
