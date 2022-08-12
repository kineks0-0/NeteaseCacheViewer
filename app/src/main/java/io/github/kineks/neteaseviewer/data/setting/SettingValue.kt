package io.github.kineks.neteaseviewer.data.setting

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotMutableState
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import io.github.kineks.neteaseviewer.App
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.map
import kotlin.reflect.KProperty

class SettingValue<T> @OptIn(DelicateCoroutinesApi::class) constructor(
    val defValue: T,
    name: String? = null,
    private val coroutineScope: CoroutineScope = GlobalScope,
    private val dataStore: DataStore<Preferences> = App.context.dataStore,
    private val mutableState: SnapshotMutableState<T> = mutableStateOf(defValue) as SnapshotMutableState<T>
) : SnapshotMutableState<T> by mutableState {

    override var value: T
        get() = mutableState.value
        set(value) {
            saveValue(value)
            mutableState.value = value
            Log.d("SettingValue", "$name :  new value is   ${mutableState.value}")
        }

    lateinit var name: String
    var initialized = false

    // 不触发保存的赋值
    private fun syncValue(v: T) {
        coroutineScope.launch(Dispatchers.Main) {
            mutableState.value = v
        }
    }

    // 由于 PreferencesKey 限制了允许保存的数据类型，所以这里需要根据数据类型返回相应 PreferencesKey
    @Suppress("UNCHECKED_CAST")
    private fun initPreferencesKey(v: T = defValue): Preferences.Key<T> =
        when (v) {
            is Int -> intPreferencesKey(this.name)
            is Long -> longPreferencesKey(this.name)
            is Float -> floatPreferencesKey(this.name)
            is Double -> doublePreferencesKey(this.name)
            is String -> stringPreferencesKey(this.name)
            is Boolean -> booleanPreferencesKey(this.name)
            is Set<*> -> {
                if ((v as? Set<String>) != null) stringSetPreferencesKey(this.name)
                else throw Exception("Value T $v : Set<*> cant be cast to Set<String>.")
            }
            else -> throw Exception("Value T $v must be one of the following: Boolean, Int, Long, Float, String, Set<String>.")
        } as Preferences.Key<T>

    private val preferencesKey: Preferences.Key<T> by lazy { initPreferencesKey() }

    private val flow: Flow<T> by lazy {
        dataStore.data.map { preferences ->
            preferences[preferencesKey] ?: defValue
        }
    }

    private val flowCollector = FlowCollector<T> {
        Log.d("SettingValue", "$name :  flow update   $it")
        syncValue(it)
    }

    init {
        if (name != null) {
            this.name = name
            coroutineScope.launch {
                flow.collect(flowCollector)
            }
            initialized = true
        }
    }

    private fun saveValue(v: T) {
        if (!initialized) return
        if (value != v)
            syncValue(v)
        saveValue()
    }

    private fun saveValue() {
        if (!initialized) return
        Log.d("SettingValue", "$preferencesKey： save   $value")
        coroutineScope.launch {
            dataStore.edit { settings ->
                settings[preferencesKey] = value
            }
        }
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (!initialized) {
            syncValue(value)
            if (!this::name.isInitialized) {
                name = property.name
            }
            coroutineScope.launch {
                flow.collect(flowCollector)
            }
            initialized = true
        } else {
            saveValue(value)
        }
        mutableState.setValue(thisRef, property, value)
    }

}