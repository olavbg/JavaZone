package com.olavbg.javazone.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.olavbg.javazone.data.repository.SessionRepository
import com.olavbg.javazone.data.repository.SettingsRepository
import com.olavbg.javazone.notifications.ReminderManager
import com.olavbg.javazone.ui.components.DonationButtons
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
    showDonationOnLaunch: Boolean = false,
) {
    val timelineViewModel: TimelineViewModel = viewModel(
        factory = TimelineViewModelFactory(repository, settingsRepository)
    )

    val showLiveBanners by timelineViewModel.showLiveIndicators.collectAsState()

    var showDonationDialog by rememberSaveable { mutableStateOf(showDonationOnLaunch) }

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

    if (showDonationDialog) {
        AlertDialog(
            onDismissRequest = { showDonationDialog = false },
            title = { Text("Takk for i år!") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Text(
                        "JavaZone ${SessionRepository.CURRENT_YEAR} er over. Likte du appen, og har du lyst til å støtte videreutviklingen? Da setter jeg pris på et lite bidrag – enten via Vipps eller \"Buy Me a Coffee\":",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 24.sp
                    )
                    DonationButtons(modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { showDonationDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("Lukk")
                    }
                }
            }
        )
    }
}