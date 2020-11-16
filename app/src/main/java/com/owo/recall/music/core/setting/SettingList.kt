package com.owo.recall.music.core.setting

import com.owo.recall.music.core.MusicFileProvider
import java.util.*
import kotlin.collections.ArrayList

object SettingList {

    val MainSettingList: ArrayList<SettingItem> = ArrayList();

    init {

        MainSettingList.add(
            SettingItem("扫描设置","","","", SettingItem.TYPE_ITEM_HEAD,
                keyValueEditor = SettingItem.KeyValueEditor(-1,"null","null",SettingItem.KeyValueEditor.TYPE_STRING,
                    object : OnSettingListener{
                        override fun onListener(keyValueEditor: SettingItem.KeyValueEditor): Boolean = getSettingItemBoolean(keyValueEditor)
                        override fun getSettingItemBoolean(keyValueEditor: SettingItem.KeyValueEditor): Boolean {return false}
                    }))
        )

        MainSettingList.add(
            SettingItem("扫描路径","NeteaseMusicCacheFolder","NeteaseMusicCacheFolder OFF","NeteaseMusicCacheFolder ON", SettingItem.TYPE_ITEM,
                keyValueEditor = SettingItem.KeyValueEditor(0,"NeteaseMusicCacheFolder",MusicFileProvider.NeteaseMusicCacheFolder.absolutePath,SettingItem.KeyValueEditor.TYPE_STRING,
                object : OnSettingListener{
                    override fun onListener(keyValueEditor: SettingItem.KeyValueEditor): Boolean  {
                        return getSettingItemBoolean(keyValueEditor)
                    }
                    override fun getSettingItemBoolean(keyValueEditor: SettingItem.KeyValueEditor): Boolean {
                        return when(keyValueEditor.getValueAsString()) {
                            MusicFileProvider.NeteaseMusicCacheFolder.absolutePath -> true
                            else -> false
                        }
                    }
                })
            )
        )

    }
}