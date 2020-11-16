package com.owo.recall.music.core.setting

import android.content.SharedPreferences
import com.owo.recall.music.CoreApplication

class SettingItem(val title: String, val summary: String, val summaryOff: String = summary, val summaryOn: String = summary, val itemType: Int, val keyValueEditor: KeyValueEditor) {

    companion object {
        const val TYPE_ITEM = 10
        const val TYPE_ITEM_CHECKBOX = 11
        const val TYPE_ITEM_SWITCH = 11
        const val TYPE_ITEM_HEAD = 0
        const val TYPE_ITEM_NOTHING = 1
    }

    class KeyValueEditor(var id: Int, private val key: String, private var value: String, val valueType: Int, private val onSettingListener: OnSettingListener) {

        companion object {
            const val TYPE_STRING = 0
            const val TYPE_INT = 1
            const val TYPE_BOOLEAN = 2
        }

        init {
            when(valueType) {
                TYPE_STRING  -> { value = getSharedPreferences().getString (key,value).toString( )            }
                TYPE_INT     -> { value = getSharedPreferences().getInt    (key,value.toInt()    ).toString() }
                TYPE_BOOLEAN -> { value = getSharedPreferences().getBoolean(key,value.toBoolean()).toString() }
            }
        }

        fun onListener() : Boolean = onSettingListener.onListener(this)
        fun getSettingItemBoolean(): Boolean = onSettingListener.getSettingItemBoolean(this)

        fun getValueAsString() : String = value
        fun getValueAsInt() : Int = value.toInt()
        fun getValueAsBoolean() : Boolean = value.toBoolean()

        fun setValueAsString(value: String) {
            getSharedPreferences().edit().putString(key,value).apply()
            this.value = value
        }
        fun setValueAsInt(value: Int) {
            getSharedPreferences().edit().putInt(key, value).apply()
            this.value = value.toString()
        }
        fun setValueAsBoolean(value : Boolean) {
            getSharedPreferences().edit().putBoolean(key,value).apply()
            this.value = value.toString()
        }

        private fun getSharedPreferences(): SharedPreferences = CoreApplication.SettingSharedPreferences

    }
}