package com.olavbg.javazone.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olavbg.javazone.data.repository.SessionRepository
import com.olavbg.javazone.data.repository.SettingsRepository
import com.olavbg.javazone.model.AppLanguage
import com.olavbg.javazone.model.BackgroundMode
import com.olavbg.javazone.model.Session
import com.olavbg.javazone.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    val notificationLeadTime: StateFlow<Int> = repository.notificationLeadTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    val notificationsEnabled: StateFlow<Boolean> = repository.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val simulatedTimeOffset: StateFlow<Long> = repository.simulatedTimeOffset
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val backgroundMode: StateFlow<BackgroundMode> = repository.backgroundMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BackgroundMode.Animated)

    val themeMode: StateFlow<ThemeMode> = repository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.Dark)

    val batteryHintDismissed: StateFlow<Boolean> = repository.batteryHintDismissed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val appLanguage: StateFlow<AppLanguage> = repository.appLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppLanguage.System)

    val missedReminderCount: StateFlow<Int> = combine(
        sessionRepository.getSessionsFlow(),
        simulatedTimeOffset,
        notificationLeadTime,
        repository.firedReminderSessionIds
    ) { sessions, timeOffset, leadTimeMinutes, firedIds ->
        countMissedReminders(
            sessions = sessions,
            firedSessionIds = firedIds,
            timeOffsetMillis = timeOffset,
            leadTimeMinutes = leadTimeMinutes,
            nowMillis = System.currentTimeMillis()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun dismissBatteryHint() {
        viewModelScope.launch {
            repository.dismissBatteryHint()
        }
    }

    fun setNotificationLeadTime(minutes: Int) {
        viewModelScope.launch {
            repository.updateNotificationLeadTime(minutes)
            sessionRepository.rescheduleAllFavorites()
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateNotificationsEnabled(enabled)
            sessionRepository.rescheduleAllFavorites()
        }
    }

    fun setSimulatedTime(dateTime: LocalDateTime) {
        val simulatedInstant = dateTime.atZone(ZoneId.of("Europe/Oslo")).toInstant()
        val actualInstant = Instant.now()
        val offset = simulatedInstant.toEpochMilli() - actualInstant.toEpochMilli()
        viewModelScope.launch {
            repository.updateSimulatedTimeOffset(offset)
            sessionRepository.rescheduleAllFavorites()
        }
    }

    fun resetSimulation() {
        viewModelScope.launch {
            repository.updateSimulatedTimeOffset(0L)
            sessionRepository.rescheduleAllFavorites()
        }
    }

    fun setBackgroundMode(mode: BackgroundMode) {
        viewModelScope.launch {
            repository.updateBackgroundMode(mode)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.updateThemeMode(mode)
        }
    }

    /**
     * Persists the selected language. The caller is expected to await this before
     * telling [com.olavbg.javazone.util.AppLocale] to apply the change, so the
     * recreated UI reads the freshly stored value.
     */
    suspend fun setAppLanguage(language: AppLanguage) {
        repository.updateAppLanguage(language)
    }
}

internal fun countMissedReminders(
    sessions: List<Session>,
    firedSessionIds: Set<String>,
    timeOffsetMillis: Long,
    leadTimeMinutes: Int,
    nowMillis: Long
): Int {
    val now = nowMillis + timeOffsetMillis
    return sessions.count { session ->
        session.isFavorite &&
            session.start != null &&
            session.start.toEpochMilli() - leadTimeMinutes * 60_000L <= now &&
            session.id !in firedSessionIds
    }
}
