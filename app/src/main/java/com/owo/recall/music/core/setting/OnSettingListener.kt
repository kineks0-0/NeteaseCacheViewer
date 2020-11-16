package com.owo.recall.music.core.setting

interface OnSettingListener {
    fun onListener(keyValueEditor: SettingItem.KeyValueEditor) : Boolean
    fun getSettingItemBoolean(keyValueEditor: SettingItem.KeyValueEditor) : Boolean
}