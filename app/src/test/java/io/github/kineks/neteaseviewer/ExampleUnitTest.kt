package io.github.kineks.neteaseviewer

import android.util.Log
import androidx.compose.compiler.plugins.kotlin.write
import io.github.kineks.neteaseviewer.data.player.XorByteInputStream
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import kotlin.experimental.xor

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun test() {
        "EAE790A0A3A3A3A1959EF7E0ECEEA3A3".decodeHex().let { byteArray ->
           println("测试 " + String(byteArray))
            var index = 0
            repeat(byteArray.size) {
                byteArray[index] = byteArray[index].xor(-93)
                index++
            }
            println("测试2 " + String(byteArray))
        }

        val file = File("E:\\1378762448-96004-3f7b06ce9eae0909e78ed9f8c668106c.mp3.uc!")
        val fileOut = File("E:\\1378762448-96004-3f7b06ce9eae0909e78ed9f8c668106c.mp3")
        val xorByteInputStream = XorByteInputStream(file = file)
        xorByteInputStream.buffered().use {
            fileOut.writeBytes(it.readBytes())
        }
    }
}