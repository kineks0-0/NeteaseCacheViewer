package com.owo.recall.music

import com.owo.recall.music.core.api.AESCrypt

fun String.aesEncrypt(): String = AESCrypt.encrypt(this)

fun String.aesDecrypt(): String = AESCrypt.decrypt(this)