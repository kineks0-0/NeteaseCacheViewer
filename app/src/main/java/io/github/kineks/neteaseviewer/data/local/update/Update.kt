package io.github.kineks.neteaseviewer.data.local.update

import android.annotation.SuppressLint
import android.content.pm.PackageInfo
import io.github.kineks.neteaseviewer.App
import io.github.kineks.neteaseviewer.data.local.Setting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.*


data class UpdateJSON(
    val updateInfo: String = "NaN",
    val updateLink: String = "https://github.com/kineks0-0/NeteaseCacheViewer/",
    val versionCode: Long = -1L,
    val versionName: String = "NaN"
)

interface UpdateApi {
    @GET("/NeteaseCacheViewer/raw/dev/screenshot/configuration.json")
    fun getUpdateDetail(): Call<UpdateJSON>
}

object Update {

    private const val Day = 1000 * 60 * 60 * 24

    suspend fun checkUpdateWithTime(callback: (json: UpdateJSON?, hasUpdate: Boolean) -> Unit) {
        Setting.lastCheckUpdates.collect {
            if (it < 100) {
                checkUpdate(callback)
            } else {
                val timeMillis: Long = Calendar.getInstance().timeInMillis
                if (timeMillis - it > Day) {
                    checkUpdate(callback)
                }
            }
        }
    }

    @SuppressLint("NewApi")
    suspend fun checkUpdate(callback: (json: UpdateJSON?, hasUpdate: Boolean) -> Unit) {
        withContext(Dispatchers.IO) {
            Setting.setLastCheckUpdates()
            val retrofit = Retrofit.Builder()
                .baseUrl("https://github.com/kineks0-0/")
                .addConverterFactory(GsonConverterFactory.create())//设置数据解析器
                .build()

            val api = retrofit.create(UpdateApi::class.java)

            val json = api.getUpdateDetail().execute().body()
            if (json == null)
                callback.invoke(null, false)

            val pInfo: PackageInfo =
                App.context.packageManager.getPackageInfo(App.context.packageName, 0)
            val versionCode: Long =
                if (App.isAndroidPorAbove) pInfo.longVersionCode else pInfo.versionCode.toLong()

            callback.invoke(json, versionCode < json!!.versionCode)

        }

    }
}