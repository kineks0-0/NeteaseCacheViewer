package io.github.kineks.neteaseviewer

import io.github.kineks.neteaseviewer.data.api.ArtistXX
import kotlin.experimental.and


//val context get()= App.context

fun Array<Int>.toURLArray(): String {
    val str = StringBuilder()
    this.forEachIndexed { index, i ->
        str.append(
            when (index) {
                0 -> "[$i,"
                this.lastIndex -> "$i]"
                else -> "$i,"
            }
        )
    }
    return str.toString()
}

fun List<ArtistXX>.getArtists(delimiters: String = ",", defValue: String = "N/A"): String {
    if (isEmpty()) return defValue
    val str = StringBuilder()
    forEachIndexed { index, info ->
        val i = info.name
        str.append(
            when (index) {
                lastIndex -> i
                0 -> "$i$delimiters"
                else -> "$i$delimiters"
            }
        )
    }
    return str.toString()
}

fun getString(id: Int) = App.context.getString(id)

fun String.decodeHex(): ByteArray {
    check(length % 2 == 0) { "Must have an even length" }

    return chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}

fun Byte.toHex(): String {
    var hex = Integer.toHexString((this and 0xFF.toByte()).toInt())
    if (hex.length < 2) {
        hex = "0$hex"
    }
    return hex
}


fun String.filterIllegalPathChar() =
    this.replace("/", "／")
        .replace("*", Char(10034).toString())
        .replace("?", "?")
        .replace("|", "｜")
        .replace(":", ":")
        .replace("<", "＜")
        .replace(">", "＞")