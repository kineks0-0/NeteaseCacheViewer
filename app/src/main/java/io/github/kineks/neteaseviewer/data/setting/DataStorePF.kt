package io.github.kineks.neteaseviewer.data.setting

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotMutableState
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import io.github.kineks.neteaseviewer.App.Companion.context
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.reflect.KProperty


val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    // 文件名称
    name = "settings"
)

class SettingValue<T>(
    val defValue: T,
    name: String? = null,
    private val mutableState: SnapshotMutableState<T> = mutableStateOf(defValue) as SnapshotMutableState<T>
) : SnapshotMutableState<T> by mutableState {

    //private var _value = defValue
    override var value: T
        get() = mutableState.value
        set(value) {
            saveValue(value)
            mutableState.value = value
            Log.d("SettingValue", "$name :  new value is   ${mutableState.value}")
        }

    lateinit var name: String
    var initialized = false

    private fun syncValue(v: T) {
        //Log.d("SettingValue", "$name :  old value is   $value")
        //_value = v
        //mutableState.value = v
        //Log.d("SettingValue", "$name :  new value is   $value")
    }


    private fun initPreferencesKey(v: T = defValue): Preferences.Key<T> =
        when (v) {
            is Int -> intPreferencesKey(this.name)
            is Long -> longPreferencesKey(this.name)
            is Float -> floatPreferencesKey(this.name)
            is Double -> doublePreferencesKey(this.name)
            is String -> stringPreferencesKey(this.name)
            is Boolean -> booleanPreferencesKey(this.name)
            is Set<*> -> {
                if ((v as? Set<String>) != null)
                    stringSetPreferencesKey(this.name)
                else throw Exception("Value T $v : Set<*> cant be cast to Set<String>.")
            }
            else -> throw Exception("Value T $v must be one of the following: Boolean, Int, Long, Float, String, Set<String>.")
        } as Preferences.Key<T>

    private val preferencesKey: Preferences.Key<T> by lazy {
        initPreferencesKey()
    }
    private val flow: Flow<T> by lazy {
        context.dataStore.data.map { preferences ->
            val t = (preferences[preferencesKey] ?: defValue)
            //syncValue(t)
            //onValueChange(t)
            t
        }
    }
    private val flowCollector = FlowCollector<T> {
        Log.d("SettingValue", "$name :  flow update   $it")
        syncValue(it)
    }

    init {
        if (name != null) {
            this.name = name
            GlobalScope.launch {
                flow.collect(flowCollector)
            }
            initialized = true
        }
    }

    private fun saveValue(v: T) {
        Log.d("SettingValue", "$name： check  initialized")
        if (!initialized) return
        if (value != v)
            syncValue(v)
        saveValue()
    }

    private fun saveValue() {
        Log.d("SettingValue", "$name： check  initialized")
        if (!initialized) return
        Log.d("SettingValue", "$preferencesKey： save   $value")
        GlobalScope.launch {
            context.dataStore.edit { settings ->
                settings[preferencesKey] = value
            }
        }
    }

    /*
        operator fun getValue(thisRef: Nothing?, property: KProperty<*>): T {
            return value
        }

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return value
        }
    */
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (!initialized) {
            syncValue(value)
            if (!this::name.isInitialized) {
                name = property.name
            }
            GlobalScope.launch {
                flow.collect(flowCollector)
            }
            initialized = true
        } else {
            saveValue(value)
        }
        mutableState.setValue(thisRef, property, value)
    }
/*
    override fun component1(): T {
        return value
    }

    override fun component2(): (T) -> Unit {
        return {
            value = it
        }
    }
*/
}

object Setting {
    private val FIRST_TIME_LAUNCH = booleanPreferencesKey("first_time_launch")
    val firstTimeLaunch: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[FIRST_TIME_LAUNCH] ?: true
    }

    suspend fun setFirstTimeLaunch(value: Boolean) {
        context.dataStore.edit { settings ->
            settings[FIRST_TIME_LAUNCH] = value
        }
    }

    /*private val LAST_CHECK_UPDATES = longPreferencesKey("Last_check_updates")
    val lastCheckUpdates: Flow<Long> = context.dataStore.data.map { preferences ->
            preferences[LAST_CHECK_UPDATES] ?: -1L
        }*/

    /*
    private val EXAMPLE_COUNTER = intPreferencesKey("example_counter")
    val exampleCounterFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            // No type safety.
            preferences[EXAMPLE_COUNTER] ?: 0
        }*/
}

