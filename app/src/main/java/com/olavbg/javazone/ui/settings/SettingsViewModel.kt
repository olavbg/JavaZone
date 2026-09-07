package com.olavbg.javazone.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olavbg.javazone.data.repository.SessionRepository
import com.olavbg.javazone.data.repository.SettingsRepository
import com.olavbg.javazone.notifications.ReminderManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val sessionRepository: SessionRepository,
    private val reminderManager: ReminderManager,
) : ViewModel() {

    val notificationLeadTime: StateFlow<Int> = repository.notificationLeadTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    val simulatedTimeOffset: StateFlow<Long> = repository.simulatedTimeOffset
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun canScheduleExactAlarms(): Boolean = reminderManager.canScheduleExact()

    fun setNotificationLeadTime(minutes: Int) {
        viewModelScope.launch {
            repository.updateNotificationLeadTime(minutes)
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
}
