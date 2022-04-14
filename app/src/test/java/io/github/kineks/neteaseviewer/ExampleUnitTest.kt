package io.github.kineks.neteaseviewer

import io.github.kineks.neteaseviewer.data.local.cacheFile.FileType
import io.github.kineks.neteaseviewer.data.player.XorByteInputStream
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import kotlin.experimental.xor


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

        val file = File("I:\\Root\\备份\\PhoneBackup\\Mi 6x\\netease\\cloudmusic\\Cache\\Music1\\37092830-256000-1d69314c1c6d6f4651c2b046890e3d53.mp3.uc!")
        /*val fileOut = File(file.parent,file.nameWithoutExtension)
        val xorByteInputStream = XorByteInputStream(file = file)
        xorByteInputStream.buffered().use {
            fileOut.writeBytes(it.readBytes())
        }*/
        FileType.getFileType(XorByteInputStream(file = file)).println()
    }
}

fun String?.println() = println(this)