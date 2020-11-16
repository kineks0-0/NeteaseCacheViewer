package com.owo.recall.music.core.net

import android.os.Build
import android.webkit.WebSettings
import com.owo.recall.music.CoreApplication.Companion.context
import okhttp3.*
import okio.IOException
import kotlin.concurrent.thread


object HttpUtil {

    fun checkUpdate(address: String, callback: Callback){
        val client = OkHttpClient()
        val response = Request.Builder()
            .url(address)
            .removeHeader("User-Agent").addHeader("User-Agent", getUserAgent())
            /*.addHeader("Accept-Encoding","gzip, deflate")*/
            .build()
        client.newCall(response).enqueue(callback)
    }

    fun sendHttpRequest(address: String, callback: Callback) {
        val client = OkHttpClient()
        val response = Request.Builder()
            .url(address)
            .addHeader("Host","music.163.com")
            .removeHeader("User-Agent").addHeader("User-Agent", getUserAgent())
            .addHeader("Content-Type","application/x-www-form-urlencoded")
            .addHeader("Accept","*/*")
            /*.addHeader("Accept-Encoding","gzip, deflate")*/
            .addHeader("Accept-Language","zh-CN,zh;q=0.8")
            .addHeader("cookie","null")
            .build()
        client.newCall(response).enqueue(callback)
    }

    //修改了okhttp请求头和标准请求不一致的错误
    private fun getUserAgent(): String {
        val userAgent = try {
            WebSettings.getDefaultUserAgent(context)
        } catch (e: Exception) {
            System.getProperty("http.agent") ?: "Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36"
        }
        //userAgent = System.getProperty("http.agent")
        //调整编码，防止中文出错
        val sb = StringBuffer()
        var i = 0
        val length = userAgent.length
        while (i < length) {
            val c = userAgent[i]
            if (c <= '\u001f' || c >= '\u007f') {
                sb.append(String.format("\\u%04x", c.toInt()))
            } else {
                sb.append(c)
            }
            i++
        }
        return sb.toString()
    }
}