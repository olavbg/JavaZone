package com.olavbg.javazone.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.olavbg.javazone.JavaZoneConfig
import com.olavbg.javazone.data.repository.SessionRepository
import com.olavbg.javazone.data.repository.SettingsRepository
import com.olavbg.javazone.notifications.ReminderManager
import com.olavbg.javazone.ui.components.YearPickerSheet
import com.olavbg.javazone.ui.detail.SessionDetailScreen
import com.olavbg.javazone.ui.navigation.NavDestination
import com.olavbg.javazone.ui.settings.SettingsScreen
import com.olavbg.javazone.ui.settings.SettingsViewModelFactory
import com.olavbg.javazone.ui.timeline.TimelineScreen
import com.olavbg.javazone.ui.timeline.TimelineViewModel
import com.olavbg.javazone.ui.timeline.TimelineViewModelFactory

@Composable
fun JavaZoneApp(
    repository: SessionRepository,
    settingsRepository: SettingsRepository,
    reminderManager: ReminderManager,
    initialSessionId: String? = null,
) {
    val timelineViewModel: TimelineViewModel = viewModel(factory = TimelineViewModelFactory(repository, settingsRepository))
    var yearPickerVisible by rememberSaveable { mutableStateOf(false) }
    val years = remember { (JavaZoneConfig.FIRST_ARCHIVE_YEAR..JavaZoneConfig.CURRENT_YEAR).toList().reversed() }

    val backStack = if (initialSessionId != null) {
        rememberNavBackStack(NavDestination.Timeline, NavDestination.SessionDetail(initialSessionId))
    } else {
        rememberNavBackStack(NavDestination.Timeline)
    }

    Scaffold { innerPadding ->
        NavDisplay(
            backStack = backStack,
            sceneStrategies = listOf(SinglePaneSceneStrategy()),
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            entryProvider = { key ->
                when (key) {
                    NavDestination.Timeline -> NavEntry(key = key) {
                        TimelineScreen(
                            viewModel = timelineViewModel,
                            onSessionClick = { id -> backStack.add(NavDestination.SessionDetail(id, timelineViewModel.selectedYear.value)) },
                            onSettingsClick = { backStack.add(NavDestination.Settings) },
                            onYearClick = { yearPickerVisible = true },
                            contentPadding = innerPadding
                        )
                    }
                    is NavDestination.SessionDetail -> NavEntry(key = key) {
                        SessionDetailScreen(
                            sessionId = key.sessionId,
                            year = key.year,
                            repository = repository,
                            settingsRepository = settingsRepository,
                            onBackClick = { backStack.removeAt(backStack.size - 1) },
                            onSpeakerClick = { name ->
                                timelineViewModel.setFilterSpeaker(name)
                                backStack.removeAt(backStack.size - 1)
                            },
                            contentPadding = innerPadding
                        )
                    }
                    NavDestination.Settings -> NavEntry(key = key) {
                        SettingsScreen(
                            viewModel = viewModel(factory = SettingsViewModelFactory(settingsRepository, repository, reminderManager)),
                            onBackClick = { backStack.removeAt(backStack.size - 1) },
                            contentPadding = innerPadding
                        )
                    }
                    else -> NavEntry(key) { }
                }
            }
        )
    }

    if (yearPickerVisible) {
        YearPickerSheet(
            selectedYear = timelineViewModel.selectedYear.collectAsState().value,
            years = years,
            onYearSelected = { year -> timelineViewModel.setYear(year); yearPickerVisible = false },
            onDismiss = { yearPickerVisible = false },
        )
    }
}
