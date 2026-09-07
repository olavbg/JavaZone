package com.olavbg.javazone.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val NOTIFICATION_LEAD_TIME = intPreferencesKey("notification_lead_time_minutes")
        val SIMULATED_TIME_OFFSET = longPreferencesKey("simulated_time_offset_millis")
    }

    val notificationLeadTime: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_LEAD_TIME] ?: 10
        }

    val simulatedTimeOffset: Flow<Long> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.SIMULATED_TIME_OFFSET] ?: 0L
        }

    suspend fun updateNotificationLeadTime(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_LEAD_TIME] = minutes
        }
    }

    suspend fun updateSimulatedTimeOffset(offsetMillis: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SIMULATED_TIME_OFFSET] = offsetMillis
        }
    }
}
