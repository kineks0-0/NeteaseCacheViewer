package io.github.kineks.neteaseviewer

import io.github.kineks.neteaseviewer.data.api.ArtistXX


//val context get()= App.context

fun Array<Int>.toURLArray() : String {
    val str = StringBuilder()
    this.forEachIndexed { index, i ->
        str.append(
            when(index) {
                0 -> "[$i,"
                this.lastIndex -> "$i]"
                else -> "$i,"
            }
        )
    }
    return str.toString()
}

fun List<ArtistXX>.getArtists(delimiters: String = ",", defValue: String = "N/A") : String {
    if (isEmpty()) return defValue
    val str = StringBuilder()
    forEachIndexed { index, info ->
        val i = info.name
        str.append(
            when(index) {
                lastIndex -> i
                0 -> "$i$delimiters"
                else -> "$i$delimiters"
            }
        )
    }
    return str.toString()
}

fun getString(id: Int) = App.context.getString(id)