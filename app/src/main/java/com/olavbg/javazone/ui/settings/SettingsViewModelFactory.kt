package com.olavbg.javazone.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.olavbg.javazone.data.repository.SessionRepository
import com.olavbg.javazone.data.repository.SettingsRepository
import com.olavbg.javazone.notifications.ReminderManager

class SettingsViewModelFactory(
    private val repository: SettingsRepository,
    private val sessionRepository: SessionRepository,
    private val reminderManager: ReminderManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository, sessionRepository, reminderManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
