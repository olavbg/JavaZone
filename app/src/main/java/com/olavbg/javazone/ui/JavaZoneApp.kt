package com.olavbg.javazone.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.olavbg.javazone.data.repository.SessionRepository
import com.olavbg.javazone.data.repository.SettingsRepository
import com.olavbg.javazone.notifications.ReminderManager
import com.olavbg.javazone.ui.archive.ArchiveScreen
import com.olavbg.javazone.ui.archive.ArchiveViewModelFactory
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
    val timelineViewModel: TimelineViewModel = viewModel(
        factory = TimelineViewModelFactory(repository, settingsRepository)
    )

    val backStack = if (initialSessionId != null) {
        rememberNavBackStack(
            NavDestination.Timeline,
            NavDestination.SessionDetail(initialSessionId)
        )
    } else {
        rememberNavBackStack(NavDestination.Timeline)
    }

    // The schedule is the primary product surface. Historical programmes will be
    // exposed through the year selector rather than a persistent bottom navigation item.
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
                            onSessionClick = { id ->
                                backStack.add(NavDestination.SessionDetail(id))
                            },
                            onSettingsClick = {
                                backStack.add(NavDestination.Settings)
                            },
                            contentPadding = innerPadding
                        )
                    }

                    NavDestination.Archive -> NavEntry(key = key) {
                        ArchiveScreen(
                            viewModel = viewModel(factory = ArchiveViewModelFactory(repository)),
                            contentPadding = innerPadding
                        )
                    }

                    is NavDestination.SessionDetail -> NavEntry(key = key) {
                        SessionDetailScreen(
                            sessionId = key.sessionId,
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
                            viewModel = viewModel(
                                factory = SettingsViewModelFactory(
                                    settingsRepository,
                                    repository,
                                    reminderManager
                                )
                            ),
                            onBackClick = { backStack.removeAt(backStack.size - 1) },
                            contentPadding = innerPadding
                        )
                    }

                    else -> NavEntry(key) { }
                }
            }
        )
    }
}
