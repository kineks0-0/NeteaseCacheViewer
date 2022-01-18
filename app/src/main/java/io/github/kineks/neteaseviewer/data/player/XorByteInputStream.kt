package io.github.kineks.neteaseviewer.data.player

import java.io.File
import java.io.FileInputStream
import kotlin.experimental.xor

/**
 * 避免直接获取ByteArray导致OOM于是写了个输入流
 */
class XorByteInputStream(file: File) : FileInputStream(file) {

    override fun read(b: ByteArray?, off: Int, len: Int): Int {
        val i = super.read(b, off, len)
        var index = 0
        repeat(len) {
            b?.set(index + off, b[index + off].xor(-93))
            index++
        }
        return i
    }

    override fun read(b: ByteArray?): Int {
        return super.read(b).apply {
            var index = 0
            repeat(b?.size ?: 0) {
                b?.set(index, b[index].xor(-93))
                index++
            }
        }
    }

}