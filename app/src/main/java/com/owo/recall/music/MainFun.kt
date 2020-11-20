package com.owo.recall.music

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatActivity

fun getApplicationContext() : Context = CoreApplication.context
fun getMainActivity() : Activity = MainActivity.ThisActivity
fun getMainActivityAsAppCompatActivity() : AppCompatActivity  = MainActivity.ThisActivity
fun getMainActivityContext() : Context = MainActivity.ThisActivity


fun toast(text: String) = CoreApplication.toast(text)
fun toast(text: Int) = CoreApplication.toast(text)