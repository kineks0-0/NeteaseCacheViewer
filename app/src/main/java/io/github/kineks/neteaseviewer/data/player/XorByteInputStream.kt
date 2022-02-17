package io.github.kineks.neteaseviewer.data.player

import java.io.File
import java.io.FileInputStream
import kotlin.experimental.xor

/**
 * 避免直接获取ByteArray导致OOM于是改用异或输入流
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

}