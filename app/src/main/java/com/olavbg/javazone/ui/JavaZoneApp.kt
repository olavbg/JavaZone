package com.olavbg.javazone.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.olavbg.javazone.data.repository.SessionRepository
import com.olavbg.javazone.data.repository.SettingsRepository
import com.olavbg.javazone.notifications.ReminderManager
import com.olavbg.javazone.ui.detail.SessionDetailScreen
import com.olavbg.javazone.ui.navigation.NavDestination
import com.olavbg.javazone.ui.settings.SettingsScreen
import com.olavbg.javazone.ui.settings.SettingsViewModelFactory
import com.olavbg.javazone.ui.timeline.TimelineScreen
import com.olavbg.javazone.ui.timeline.TimelineViewModel
import com.olavbg.javazone.ui.timeline.TimelineViewModelFactory

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
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

    val showLiveBanners by timelineViewModel.showLiveIndicators.collectAsState()

    val backStack = if (initialSessionId != null) {
        rememberNavBackStack(
            NavDestination.Timeline, NavDestination.SessionDetail(initialSessionId)
        )
    } else {
        rememberNavBackStack(NavDestination.Timeline)
    }

    NavDisplay(
        backStack = backStack,
        sceneStrategies = listOf(SinglePaneSceneStrategy()),
        modifier = Modifier.fillMaxSize(),
        entryProvider = { key ->
            when (key) {
                NavDestination.Timeline -> NavEntry(
                    key = key
                ) {
                    TimelineScreen(
                        viewModel = timelineViewModel,
                        onSessionClick = { id, year ->
                            backStack.add(NavDestination.SessionDetail(id, year))
                        }, onSettingsClick = {
                            backStack.add(NavDestination.Settings)
                        }
                    )
                }

                is NavDestination.SessionDetail -> NavEntry(
                    key = key
                ) {
                    SessionDetailScreen(
                        sessionId = key.sessionId,
                        year = key.year,
                        repository = repository,
                        settingsRepository = settingsRepository,
                        showLiveBanners = showLiveBanners,
                        onBackClick = { backStack.removeAt(backStack.size - 1) },
                        onSpeakerClick = { name ->
                            timelineViewModel.setFilterSpeaker(name)
                            backStack.removeAt(backStack.size - 1)
                        }
                    )
                }

                NavDestination.Settings -> NavEntry(
                    key = key
                ) {
                    SettingsScreen(
                        viewModel = viewModel(
                            factory = SettingsViewModelFactory(
                                settingsRepository, repository, reminderManager
                            )
                        ),
                        onBackClick = { backStack.removeAt(backStack.size - 1) }
                    )
                }

                else -> NavEntry(key) { }
            }
        })
}