package com.olavbg.javazone.data.repository

import android.app.ActivityManager
import android.content.Context
import androidx.core.content.getSystemService
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
import com.olavbg.javazone.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

// Persisted timeline filters, tagged with the conference year they were saved for so a cold
// start never silently re-applies filters meant for a previous year.
data class TimelineFilters(
    val savedForYear: Int,
    val selectedDay: String?,
    val selectedRoom: String?,
    val selectedFormat: String?,
    val selectedLanguage: String?,
    val onlyFavorites: Boolean,
)

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val context: Context? = null
) {

    // Convenience constructor used by the app; tests can inject a JVM DataStore instead.
    constructor(context: Context) : this(context.dataStore, context)

    companion object {
        val APP_LANGUAGE_KEY = stringPreferencesKey("app_language")
    }

    private object PreferencesKeys {
        val NOTIFICATION_LEAD_TIME = intPreferencesKey("notification_lead_time_minutes")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val SIMULATED_TIME_OFFSET = longPreferencesKey("simulated_time_offset_millis")
        val BACKGROUND_MODE = stringPreferencesKey("background_mode")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NOTIFICATION_PROMPT_SHOWN = booleanPreferencesKey("notification_prompt_shown")
        val BATTERY_HINT_DISMISSED = booleanPreferencesKey("battery_hint_dismissed")
        val FILTERS_EXPANDED = booleanPreferencesKey("filters_expanded")
        val TIMELINE_FILTER_YEAR = intPreferencesKey("timeline_filter_year")
        val TIMELINE_FILTER_DAY = stringPreferencesKey("timeline_filter_day")
        val TIMELINE_FILTER_ROOM = stringPreferencesKey("timeline_filter_room")
        val TIMELINE_FILTER_FORMAT = stringPreferencesKey("timeline_filter_format")
        val TIMELINE_FILTER_LANGUAGE = stringPreferencesKey("timeline_filter_language")
        val TIMELINE_FILTER_ONLY_FAVORITES = booleanPreferencesKey("timeline_filter_only_favorites")
        val FIRED_REMINDER_IDS =
            stringSetPreferencesKey("fired_reminder_session_ids_${SessionRepository.CURRENT_YEAR}")
    }

    val notificationLeadTime: Flow<Int> = dataStore.data
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

    val notificationsEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true
        }

    val simulatedTimeOffset: Flow<Long> = dataStore.data
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

    fun defaultBackgroundMode(): BackgroundMode {
        val activityManager = context?.getSystemService<ActivityManager>()
        return if (activityManager?.isLowRamDevice == true) {
            BackgroundMode.Static
        } else {
            BackgroundMode.Animated
        }
    }

    val backgroundMode: Flow<BackgroundMode> = dataStore.data
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
                ?: defaultBackgroundMode()
        }

    val themeMode: Flow<ThemeMode> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            ThemeMode.fromStorage(preferences[PreferencesKeys.THEME_MODE]) ?: ThemeMode.Dark
        }

    val notificationPromptShown: Flow<Boolean> = dataStore.data
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

    val batteryHintDismissed: Flow<Boolean> = dataStore.data
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

    val filtersExpanded: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.FILTERS_EXPANDED] ?: false
        }

    val timelineFilters: Flow<TimelineFilters?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val year = preferences[PreferencesKeys.TIMELINE_FILTER_YEAR] ?: return@map null
            TimelineFilters(
                savedForYear = year,
                selectedDay = preferences[PreferencesKeys.TIMELINE_FILTER_DAY],
                selectedRoom = preferences[PreferencesKeys.TIMELINE_FILTER_ROOM],
                selectedFormat = preferences[PreferencesKeys.TIMELINE_FILTER_FORMAT],
                selectedLanguage = preferences[PreferencesKeys.TIMELINE_FILTER_LANGUAGE],
                onlyFavorites = preferences[PreferencesKeys.TIMELINE_FILTER_ONLY_FAVORITES] ?: false,
            )
        }

    val appLanguage: Flow<AppLanguage> = dataStore.data
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

    val firedReminderSessionIds: Flow<Set<String>> = dataStore.data
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
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_LEAD_TIME] = minutes
        }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun updateAppLanguage(language: AppLanguage) {
        dataStore.edit { preferences ->
            preferences[APP_LANGUAGE_KEY] = language.storageValue
        }
    }

    suspend fun updateSimulatedTimeOffset(offsetMillis: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SIMULATED_TIME_OFFSET] = offsetMillis
        }
    }

    suspend fun updateBackgroundMode(mode: BackgroundMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BACKGROUND_MODE] = mode.name
        }
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.storageValue
        }
    }

    suspend fun isConferenceDoneNotified(year: Int): Boolean {
        val key = booleanPreferencesKey("conference_done_notified_$year")
        return dataStore.data.first()[key] ?: false
    }

    suspend fun markConferenceDoneNotified(year: Int) {
        val key = booleanPreferencesKey("conference_done_notified_$year")
        dataStore.edit { preferences ->
            preferences[key] = true
        }
    }

    suspend fun markNotificationPromptShown() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_PROMPT_SHOWN] = true
        }
    }

    suspend fun dismissBatteryHint() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BATTERY_HINT_DISMISSED] = true
        }
    }

    suspend fun setFiltersExpanded(expanded: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FILTERS_EXPANDED] = expanded
        }
    }

    suspend fun saveTimelineFilters(filters: TimelineFilters) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TIMELINE_FILTER_YEAR] = filters.savedForYear
            if (filters.selectedDay != null) {
                preferences[PreferencesKeys.TIMELINE_FILTER_DAY] = filters.selectedDay
            } else {
                preferences.remove(PreferencesKeys.TIMELINE_FILTER_DAY)
            }
            if (filters.selectedRoom != null) {
                preferences[PreferencesKeys.TIMELINE_FILTER_ROOM] = filters.selectedRoom
            } else {
                preferences.remove(PreferencesKeys.TIMELINE_FILTER_ROOM)
            }
            if (filters.selectedFormat != null) {
                preferences[PreferencesKeys.TIMELINE_FILTER_FORMAT] = filters.selectedFormat
            } else {
                preferences.remove(PreferencesKeys.TIMELINE_FILTER_FORMAT)
            }
            if (filters.selectedLanguage != null) {
                preferences[PreferencesKeys.TIMELINE_FILTER_LANGUAGE] = filters.selectedLanguage
            } else {
                preferences.remove(PreferencesKeys.TIMELINE_FILTER_LANGUAGE)
            }
            preferences[PreferencesKeys.TIMELINE_FILTER_ONLY_FAVORITES] = filters.onlyFavorites
        }
    }

    suspend fun clearTimelineFilters() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.TIMELINE_FILTER_YEAR)
            preferences.remove(PreferencesKeys.TIMELINE_FILTER_DAY)
            preferences.remove(PreferencesKeys.TIMELINE_FILTER_ROOM)
            preferences.remove(PreferencesKeys.TIMELINE_FILTER_FORMAT)
            preferences.remove(PreferencesKeys.TIMELINE_FILTER_LANGUAGE)
            preferences.remove(PreferencesKeys.TIMELINE_FILTER_ONLY_FAVORITES)
        }
    }

    suspend fun markSessionReminderFired(sessionId: String) {
        dataStore.edit { preferences ->
            val fired = preferences[PreferencesKeys.FIRED_REMINDER_IDS].orEmpty().toMutableSet()
            fired.add(sessionId)
            preferences[PreferencesKeys.FIRED_REMINDER_IDS] = fired
        }
    }
}
