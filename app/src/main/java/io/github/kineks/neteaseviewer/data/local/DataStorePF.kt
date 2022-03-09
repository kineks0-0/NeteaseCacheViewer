package io.github.kineks.neteaseviewer.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import io.github.kineks.neteaseviewer.App.Companion.context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.*
import java.util.*


val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    // 文件名称
    name = "settings"
)

object Setting {
    private val FIRST_TIME_LAUNCH = booleanPreferencesKey("first_time_launch")
    val firstTimeLaunch: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[FIRST_TIME_LAUNCH] ?: true
        }
    suspend fun setFirstTimeLaunch(value: Boolean) {
        context.dataStore.edit { settings ->
            settings[FIRST_TIME_LAUNCH] = value
        }
    }

    private val LAST_CHECK_UPDATES = longPreferencesKey("Last_check_updates")
    val lastCheckUpdates: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[LAST_CHECK_UPDATES] ?: -1L
        }
    suspend fun setLastCheckUpdates(value: Long = Calendar.getInstance().timeInMillis) {
        context.dataStore.edit { settings ->
            settings[LAST_CHECK_UPDATES] = value
        }
    }
    /*
    private val EXAMPLE_COUNTER = intPreferencesKey("example_counter")
    val exampleCounterFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            // No type safety.
            preferences[EXAMPLE_COUNTER] ?: 0
        }*/
}

