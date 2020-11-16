package com.owo.recall.music.core.setting

import com.owo.recall.music.CoreApplication
import com.owo.recall.music.core.MusicFileProvider

object SettingList {

    val MainSettingList: ArrayList<SettingItem> = ArrayList()

    init {
        //Todo: id和设置id查询设置选项

        SettingItem.addItemHead("扫描设置", MainSettingList)

        MainSettingList.add(
            SettingItem("扫描路径","NeteaseMusicCacheFolder","NeteaseMusicCacheFolder OFF","NeteaseMusicCacheFolder ON", SettingItem.TYPE_ITEM,
                keyValueEditor = SettingItem.KeyValueEditor(0,"NeteaseMusicCacheFolder",MusicFileProvider.NeteaseMusicCacheFolder.absolutePath,SettingItem.KeyValueEditor.TYPE_STRING,
                object : OnSettingListener{
                    override fun onListener(keyValueEditor: SettingItem.KeyValueEditor): Boolean  {
                        CoreApplication.toast("测试委托事件")
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



        SettingItem.addItemHead("导出设置", MainSettingList)

        MainSettingList.add(
            SettingItem("导出路径","ExportMusicFolder","Export music files","Export music files ON", SettingItem.TYPE_ITEM,
                keyValueEditor = SettingItem.KeyValueEditor(1,"NeteaseMusicCacheFolder",MusicFileProvider.NeteaseMusicCacheFolder.absolutePath,SettingItem.KeyValueEditor.TYPE_STRING,
                    object : OnSettingListener{
                        override fun onListener(keyValueEditor: SettingItem.KeyValueEditor): Boolean  {
                            CoreApplication.toast("测试委托事件")
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