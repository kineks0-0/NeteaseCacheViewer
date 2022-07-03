package io.github.kineks.neteaseviewer.data.player

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlin.experimental.xor

/**
 * 避免直接获取ByteArray导致OOM于是改用异或输入流
 */
class XorByteInputStream(private val inputStream: InputStream) : InputStream() {

    constructor(file: File) : this(FileInputStream(file))

    override fun read(): Int = inputStream.read()
    override fun available(): Int = inputStream.available()
    override fun toString(): String = inputStream.toString()
    override fun hashCode(): Int = inputStream.hashCode()
    override fun close() = inputStream.close()
    override fun markSupported(): Boolean = inputStream.markSupported()
    override fun reset() = inputStream.reset()
    override fun equals(other: Any?): Boolean = inputStream.equals(other)
    override fun mark(readlimit: Int) = inputStream.mark(readlimit)

    override fun read(b: ByteArray?): Int = inputStream.read(b)
    override fun skip(n: Long): Long = inputStream.skip(n)

    override fun read(b: ByteArray?, off: Int, len: Int): Int {
        val i = inputStream.read(b, off, len)
        var index = 0
        repeat(len) {
            b?.set(index + off, b[index + off].xor(-93))
            index++
        }
        return i
    }

}