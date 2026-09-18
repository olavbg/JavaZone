package com.olavbg.javazone.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.olavbg.javazone.model.AppLanguage
import com.olavbg.javazone.model.BackgroundMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val APP_LANGUAGE_KEY = stringPreferencesKey("app_language")
    }

    private object PreferencesKeys {
        val NOTIFICATION_LEAD_TIME = intPreferencesKey("notification_lead_time_minutes")
        val SIMULATED_TIME_OFFSET = longPreferencesKey("simulated_time_offset_millis")
        val BACKGROUND_MODE = stringPreferencesKey("background_mode")
        val NOTIFICATION_PROMPT_SHOWN = booleanPreferencesKey("notification_prompt_shown")
        val BATTERY_HINT_DISMISSED = booleanPreferencesKey("battery_hint_dismissed")
        val FIRED_REMINDER_IDS =
            stringSetPreferencesKey("fired_reminder_session_ids_${SessionRepository.CURRENT_YEAR}")
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

    val backgroundMode: Flow<BackgroundMode> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.BACKGROUND_MODE]
                ?.let { raw -> BackgroundMode.entries.firstOrNull { it.name == raw } }
                ?: BackgroundMode.Animated
        }

    val notificationPromptShown: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_PROMPT_SHOWN] ?: false
        }

    val batteryHintDismissed: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.BATTERY_HINT_DISMISSED] ?: false
        }

    val appLanguage: Flow<AppLanguage> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppLanguage.fromStorage(preferences[APP_LANGUAGE_KEY]) ?: AppLanguage.System
        }

    val firedReminderSessionIds: Flow<Set<String>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.FIRED_REMINDER_IDS] ?: emptySet()
        }

    suspend fun updateNotificationLeadTime(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_LEAD_TIME] = minutes
        }
    }

    suspend fun updateAppLanguage(language: AppLanguage) {
        context.dataStore.edit { preferences ->
            preferences[APP_LANGUAGE_KEY] = language.storageValue
        }
    }

    suspend fun updateSimulatedTimeOffset(offsetMillis: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SIMULATED_TIME_OFFSET] = offsetMillis
        }
    }

    suspend fun updateBackgroundMode(mode: BackgroundMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BACKGROUND_MODE] = mode.name
        }
    }

    suspend fun isConferenceDoneNotified(year: Int): Boolean {
        val key = booleanPreferencesKey("conference_done_notified_$year")
        return context.dataStore.data.first()[key] ?: false
    }

    suspend fun markConferenceDoneNotified(year: Int) {
        val key = booleanPreferencesKey("conference_done_notified_$year")
        context.dataStore.edit { preferences ->
            preferences[key] = true
        }
    }

    suspend fun markNotificationPromptShown() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_PROMPT_SHOWN] = true
        }
    }

    suspend fun dismissBatteryHint() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BATTERY_HINT_DISMISSED] = true
        }
    }

    suspend fun markSessionReminderFired(sessionId: String) {
        context.dataStore.edit { preferences ->
            val fired = preferences[PreferencesKeys.FIRED_REMINDER_IDS].orEmpty().toMutableSet()
            fired.add(sessionId)
            preferences[PreferencesKeys.FIRED_REMINDER_IDS] = fired
        }
    }
}
