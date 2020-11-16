package com.owo.recall.music.core.api

import com.owo.recall.music.aesEncrypt
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object AESCrypt {
    // 記得定義一下你的 key
    private var key: String = "Your AES Key"
    // 這裡是宣告加解密的方法
    private const val transformation = "AES/CBC/PKCS5Padding"

    private var keySpec = SecretKeySpec(key.toByteArray(), 0, 32, "AES")
    private var ivParameterSpec = IvParameterSpec(key.toByteArray(), 0, 16)
    private val ByteArray.asHexUpper: String
        inline get() {
            return this.joinToString(separator = "") {
                String.format("%02X", (it.toInt() and 0xFF))
            }
        }
    private val String.hexAsByteArray: ByteArray
        inline get() {
            return this.chunked(2).map {
                it.toUpperCase(Locale.US).toInt(16).toByte()
            }.toByteArray()
        }

    private fun updataKey(key: String) {
        if (this.key!=key) {
            this.key = key
            keySpec = SecretKeySpec(AESCrypt.key.toByteArray(), 0, 32, "AES")
            ivParameterSpec = IvParameterSpec(AESCrypt.key.toByteArray(), 0, 16)
        }
    }


    // 加密使用的方法
    fun encrypt(input: String, key: String = this.key): String {
        val cipher = Cipher.getInstance(transformation)
        updataKey(key)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParameterSpec)
        val encrypt = cipher.doFinal(input.toByteArray())
        return encrypt.asHexUpper
    }

    // 解密使用的方法
    fun decrypt(input: String, key: String = this.key): String {
        val cipher = Cipher.getInstance(transformation)
        updataKey(key)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivParameterSpec)
        val encrypt = cipher.doFinal(input.hexAsByteArray)
        return String(encrypt)
    }
}