package com.owo.recall.music.core.setting

interface OnSettingListener {

    interface Callback {
        fun onListenerBack(keyValueEditor: SettingItem.KeyValueEditor,settingItem: SettingItem, update: Boolean)
    }

    fun onListener(keyValueEditor: SettingItem.KeyValueEditor,callback: Callback)
    fun getSettingItemBoolean(keyValueEditor: SettingItem.KeyValueEditor) : Boolean
}