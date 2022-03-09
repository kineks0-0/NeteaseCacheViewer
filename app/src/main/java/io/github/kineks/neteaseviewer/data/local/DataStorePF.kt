package io.github.kineks.neteaseviewer.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.kineks.neteaseviewer.App.Companion.context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.*


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

    private val EXAMPLE_COUNTER = intPreferencesKey("example_counter")
    val exampleCounterFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            // No type safety.
            preferences[EXAMPLE_COUNTER] ?: 0
        }
}
