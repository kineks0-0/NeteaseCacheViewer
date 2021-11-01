package io.github.kineks.neteaseviewer.data.local

import com.google.gson.annotations.SerializedName

data class CacheFileInfo(
    val bitrate: Int,
    val duration: Int,
    @SerializedName("filemd5")
    val fileMD5: String,
    @SerializedName("filesize")
    val fileSize: Int,
    val md5: String,
    @SerializedName("musicId")
    val id: Int,
    val parts: List<String>,
    val version: Int
)