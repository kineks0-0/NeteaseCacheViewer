package com.owo.recall.music.core.setting

import android.app.Activity
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.owo.recall.music.CoreApplication
import com.owo.recall.music.MainActivity
import com.owo.recall.music.core.AvoidOnResult
import com.owo.recall.music.core.MusicFileProvider
import com.owo.recall.music.getApplicationContext
import java.io.File


object SettingList {

    val MainSettingList: ArrayList<SettingItem> = ArrayList()
    val MainSettingKeyList: MutableMap<String, Int> = HashMap()

    init {
        //Todo: id和设置id查询设置选项

        SettingItem.addItemHead("扫描设置", MainSettingList)

        MainSettingKeyList["NeteaseMusicCacheFolder"] = MainSettingList.size
        MainSettingList.add(
            SettingItem(
                "扫描路径", "NeteaseMusicCacheFolder",
                itemType =  SettingItem.TYPE_ITEM,
                keyValueEditor = SettingItem.KeyValueEditor(0,
                    "NeteaseMusicCacheFolder",
                    //MusicFileProvider.NeteaseMusicCacheFolder.absolutePath 该对象初始化需要调用这里，所以没法调用这个（不然死锁）
                    "/storage/emulated/0/netease/cloudmusic/Cache/Music1/",
                    SettingItem.KeyValueEditor.TYPE_STRING,
                    object : OnSettingListener {
                        override fun onListener(keyValueEditor: SettingItem.KeyValueEditor, callback: OnSettingListener.Callback) {
                            CoreApplication.toast("测试委托事件，请选择目录下的文件")
                            val REQUEST_CODE_CALLBACK = 160

                            val intent = Intent(Intent.ACTION_GET_CONTENT)
                            //val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                            //val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                            //val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                            //intent.type = "file/*"
                            intent.type = "*/*"
                            intent.addCategory(Intent.CATEGORY_OPENABLE)

                            AvoidOnResult(MainActivity.ThisActivity).startForResult(intent,//MainActivity::class.java,
                                REQUEST_CODE_CALLBACK,
                                object : AvoidOnResult.Callback {
                                    override fun onActivityResult(
                                        requestCode: Int,
                                        resultCode: Int,
                                        data: Intent?
                                    ) {

                                        if (resultCode == Activity.RESULT_OK) {
                                            /*val folderPath = data?.dataString
                                            folderPath?.let {
                                                CoreApplication.toast(it)
                                            }*/

                                            val uri: Uri = data?.data!!
                                            val file = File(getPath(getApplicationContext(),uri))
                                            //CoreApplication.toast(file.absolutePath)
                                            //CoreApplication.toast(file.parentFile.absolutePath)
                                            val fileParentFile = file.parentFile
                                            if (fileParentFile != null) {
                                                keyValueEditor.setValueAsString(fileParentFile.absolutePath)
                                                MusicFileProvider.NeteaseMusicCacheFolder = fileParentFile
                                                getSettingItem(keyValueEditor.key).summary = fileParentFile.absolutePath.replaceFirst("/storage/emulated/0","")
                                                CoreApplication.toast("部分数据重启生效")
                                                callback.onListenerBack(keyValueEditor,getSettingItem("NeteaseMusicCacheFolder"),true)
                                            } else {
                                                CoreApplication.toast("fileParentFile == null ,无法设置null为路径")
                                            }
                                        } else {
                                            CoreApplication.toast("Error: DataResultCode != RESULT_OK")
                                        }
                                    }
                                })
                        }

                        override fun getSettingItemBoolean(keyValueEditor: SettingItem.KeyValueEditor): Boolean {
                            return when (keyValueEditor.getValueAsString()) {
                                MusicFileProvider.NeteaseMusicCacheFolder.absolutePath -> true
                                else -> false
                            }
                        }
                    })
            )
        )



        SettingItem.addItemHead("导出设置", MainSettingList)

        MainSettingKeyList["ExportMusicFolder"] = MainSettingList.size
        MainSettingList.add(
            SettingItem(
                "导出路径",
                "ExportMusicFolder",
                itemType = SettingItem.TYPE_ITEM,
                keyValueEditor = SettingItem.KeyValueEditor(1,
                    "ExportMusicFolder",
                    "Music",
                    SettingItem.KeyValueEditor.TYPE_STRING,
                    object : OnSettingListener {
                        override fun onListener(keyValueEditor: SettingItem.KeyValueEditor, callback: OnSettingListener.Callback) {
                            CoreApplication.toast("测试委托事件，请选择的目录下确定，并不会新建文件")
                            val REQUEST_CODE_CALLBACK = 170

                            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                            intent.type = "*/*"
                            intent.addCategory(Intent.CATEGORY_OPENABLE)

                            AvoidOnResult(MainActivity.ThisActivity).startForResult(intent,//MainActivity::class.java,
                                REQUEST_CODE_CALLBACK,
                                object : AvoidOnResult.Callback {
                                    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

                                        if (resultCode == Activity.RESULT_OK) {

                                            val uri: Uri = data?.data!!
                                            val file = File(getPath(getApplicationContext(),uri))
                                            file.delete()
                                            val fileParentFile = file.parentFile
                                            if (fileParentFile != null) {
                                                keyValueEditor.setValueAsString(fileParentFile.absolutePath)
                                                MusicFileProvider.DIR_Music = fileParentFile
                                                getSettingItem(keyValueEditor.key).summary = fileParentFile.absolutePath.replaceFirst("/storage/emulated/0","")
                                                CoreApplication.toast("部分数据重启生效")
                                                callback.onListenerBack(keyValueEditor,getSettingItem(keyValueEditor.key),true)
                                            } else {
                                                CoreApplication.toast("fileParentFile == null ,无法设置null为路径")
                                            }
                                        } else {
                                            CoreApplication.toast("Error: DataResultCode != RESULT_OK")
                                        }
                                    }
                                })
                        }

                        override fun getSettingItemBoolean(keyValueEditor: SettingItem.KeyValueEditor): Boolean {
                            return when (keyValueEditor.getValueAsString()) {
                                MusicFileProvider.NeteaseMusicCacheFolder.absolutePath -> true
                                else -> false
                            }
                        }
                    })
            )
        )




    }

    fun getSettingItemIndex(key: String) : Int = MainSettingKeyList[key] ?: -1

    fun getSettingItem(index: Int,list: ArrayList<SettingItem> = MainSettingList) : SettingItem = list[index]

    fun getSettingItem(key: String,list: ArrayList<SettingItem> = MainSettingList) : SettingItem = getSettingItem(getSettingItemIndex(key),list)

    fun getSettingItem(key: String,Def: String,list: ArrayList<SettingItem> = MainSettingList) : String {
        val index = getSettingItemIndex(key)
        return if (index != -1) {
            list[index].keyValueEditor.getValueAsString()
        }else{
            Def
        }
    }
}

fun getPath(context: Context, uri: Uri): String {

    var path = ""
    //file: 开头的
    if (ContentResolver.SCHEME_FILE == uri.scheme) {
        path = uri.path.toString()
        return path
    }

    // 以 content:// 开头的，比如 content://media/extenral/images/media/17766// 4.4

    if (DocumentsContract.isDocumentUri(context, uri)) {
        if (isExternalStorageDocument(uri)) {
            // ExternalStorageProvider
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":".toRegex()).toTypedArray()
            val type = split[0]
            if ("primary".equals(type, ignoreCase = true)) {
                path = Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                return path
            }
        } else if (isDownloadsDocument(uri)) {
            // DownloadsProvider
            val id = DocumentsContract.getDocumentId(uri)
            val contentUri = ContentUris.withAppendedId(
                Uri.parse("content://downloads/public_downloads"),
                java.lang.Long.valueOf(id)
            )
            path = getDataColumn(context, contentUri, null, arrayOf("")).toString()
            return path
        } else if (isMediaDocument(uri)) {
            // MediaProvider
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":".toRegex()).toTypedArray()
            val type = split[0]
            var contentUri: Uri? = null
            if ("image" == type) {
                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            } else if ("video" == type) {
                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else if ("audio" == type) {
                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
            val selection = "_id=?"
            val selectionArgs = arrayOf(split[1])
            path = contentUri?.let { getDataColumn(context, it, selection, selectionArgs) } ?: ""
            return path
        }
    }
    return ""
}

private fun getDataColumn(
    context: Context,
    uri: Uri,
    selection: String?,
    selectionArgs: Array<String>
): String? {
    var cursor: Cursor? = null
    val column = "_data"
    val projection = arrayOf(column)
    try {
        cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
        if (cursor != null && cursor.moveToFirst()) {
            val column_index: Int = cursor.getColumnIndexOrThrow(column)
            return cursor.getString(column_index)
        }
    } finally {
        if (cursor != null) cursor.close()
    }
    return null
}

private fun isExternalStorageDocument(uri: Uri): Boolean {
    return "com.android.externalstorage.documents" == uri.authority
}

private fun isDownloadsDocument(uri: Uri): Boolean {
    return "com.android.providers.downloads.documents" == uri.authority
}

private fun isMediaDocument(uri: Uri): Boolean {
    return "com.android.providers.media.documents" == uri.authority
}
