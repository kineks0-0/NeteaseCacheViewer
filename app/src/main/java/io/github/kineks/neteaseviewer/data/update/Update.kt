package io.github.kineks.neteaseviewer.data.update

import android.annotation.SuppressLint
import android.content.pm.PackageInfo
import android.util.Log
import io.github.kineks.neteaseviewer.App
import io.github.kineks.neteaseviewer.data.local.Setting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.await
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
    @GET("/kineks0-0/NeteaseCacheViewer/dev/screenshot/configuration.json")
    fun getUpdateDetail(): Call<UpdateJSON>

    @GET("/https://raw.githubusercontent.com/kineks0-0/NeteaseCacheViewer/dev/screenshot/configuration.json")
    fun getCDNUpdateDetail(): Call<UpdateJSON>
}

object Update {

    private const val Day = 1000 * 60 * 60 * 24
    private var cdn = true
    private var json: UpdateJSON? = null

    suspend fun checkUpdateWithTime(callback: (json: UpdateJSON?, hasUpdate: Boolean) -> Unit) {
        Setting.lastCheckUpdates.collect {
            Log.d("checkUpdateWithTime", "lastCheckUpdates: $it")
            if (it < 100) {
                checkUpdate(callback)
            } else {
                val timeMillis: Long = Calendar.getInstance().timeInMillis
                Log.d("checkUpdateWithTime", "timeMillis: $timeMillis")
                Log.d("checkUpdateWithTime", "day: ${timeMillis - it}")
                if (timeMillis - it > Day) {
                    checkUpdate(callback)
                }
            }
        }
    }

    @SuppressLint("NewApi")
    suspend fun checkUpdate(callback: (json: UpdateJSON?, hasUpdate: Boolean) -> Unit) {
        withContext(Dispatchers.IO) {
            val timeMillis: Long = Calendar.getInstance().timeInMillis
            Log.d("checkUpdate", "save lastTimeMillis: $timeMillis")
            Setting.setLastCheckUpdates()

            if (json == null) {
                val retrofit = Retrofit.Builder()
                    .baseUrl(
                        if (cdn)
                            "https://ghproxy.com/"
                        else
                            "https://raw.githubusercontent.com/"
                    )
                    .addConverterFactory(GsonConverterFactory.create())//设置数据解析器
                    .build()

                val api =
                    if (cdn)
                        retrofit.create(UpdateApi::class.java).getCDNUpdateDetail()
                    else
                        retrofit.create(UpdateApi::class.java).getUpdateDetail()
                Log.d("checkUpdate", api.request().url().toString())
                json = api.await()
                if (json == null) {
                    Log.d("checkUpdate", "json == null")
                    callback.invoke(null, false)
                    return@withContext
                }
            }

            val pInfo: PackageInfo =
                App.context.packageManager.getPackageInfo(App.context.packageName, 0)

            @Suppress("DEPRECATION")
            val versionCode: Long =
                if (App.isAndroidPorAbove) pInfo.longVersionCode else pInfo.versionCode.toLong()

            Log.d("checkUpdate", json.toString())
            callback.invoke(json, versionCode < json!!.versionCode)

        }

    }
}