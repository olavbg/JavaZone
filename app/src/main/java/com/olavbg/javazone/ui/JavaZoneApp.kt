package com.olavbg.javazone.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun JavaZoneApp(
    repository: SessionRepository,
    settingsRepository: SettingsRepository,
    reminderManager: ReminderManager,
    initialSessionId: String? = null,
    showDonationOnLaunch: Boolean = false,
    onNavigation: () -> Unit = {},
) {
    val timelineViewModel: TimelineViewModel = viewModel(
        factory = TimelineViewModelFactory(repository, settingsRepository)
    )

    val showLiveBanners by timelineViewModel.showLiveIndicators.collectAsState()

    val context = LocalContext.current
    var showNotificationPrompt by remember { mutableStateOf(false) }
    val promptShown by settingsRepository.notificationPromptShown.collectAsState(initial = true)
    val favoriteCount by repository.favoriteCount.collectAsState(initial = null)
    var previousFavoriteCount by remember { mutableStateOf<Int?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(favoriteCount, promptShown) {
        val current = favoriteCount ?: return@LaunchedEffect
        val previous = previousFavoriteCount
        previousFavoriteCount = current
        if (previous == null || current <= previous) return@LaunchedEffect
        if (promptShown || !needsNotificationPermission(context)) return@LaunchedEffect
        showNotificationPrompt = true
        settingsRepository.markNotificationPromptShown()
    }

    var showDonationDialog by rememberSaveable { mutableStateOf(showDonationOnLaunch) }

    val backStack = if (initialSessionId != null) {
        rememberNavBackStack(
            NavDestination.Timeline, NavDestination.SessionDetail(initialSessionId)
        )
    } else {
        rememberNavBackStack(NavDestination.Timeline)
    }

    // Reanimate the background on every screen change; the background ignores calls
    // made while a reanimate is still playing.
    var isFirstNav by remember { mutableStateOf(true) }
    LaunchedEffect(backStack.size) {
        if (isFirstNav) {
            isFirstNav = false
            return@LaunchedEffect
        }
        onNavigation()
    }

    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            sceneStrategies = listOf(SinglePaneSceneStrategy()),
            sharedTransitionScope = this,
            // A fade-through (out-then-in) gives the shared element hero the full ~700 ms it
            // needs to interpolate bounds, while avoiding the classic crossfade problem where
            // both screens are visible simultaneously and the shared title ghosts on top of
            // itself. Old scene fades out over 350 ms; new scene fades in over 350 ms.
            transitionSpec = {
                fadeIn(animationSpec = tween(350, delayMillis = 350)) togetherWith
                    fadeOut(animationSpec = tween(350))
            },
            popTransitionSpec = {
                fadeIn(animationSpec = tween(350, delayMillis = 350)) togetherWith
                    fadeOut(animationSpec = tween(350))
            },
            // Predictive back re-uses the same fade-through; the default spec scales and
            // crossfades, which clashes with the shared element transition.
            predictivePopTransitionSpec = { _ ->
                fadeIn(animationSpec = tween(350, delayMillis = 350)) togetherWith
                    fadeOut(animationSpec = tween(350))
            },
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
                            },
                            sharedScope = this@SharedTransitionLayout,
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
                            sharedScope = this@SharedTransitionLayout
                        )
                    }

                    NavDestination.Settings -> NavEntry(
                        key = key
                    ) {
                        SettingsScreen(
                            viewModel = viewModel(
                                factory = SettingsViewModelFactory(
                                    settingsRepository, repository
                                )
                            ),
                            onBackClick = { backStack.removeAt(backStack.size - 1) }
                        )
                    }

                    else -> NavEntry(key) { }
                }
            })
    }

    if (showDonationDialog) {
        AlertDialog(
            onDismissRequest = { showDonationDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 1f),
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

    if (showNotificationPrompt) {
        AlertDialog(
            onDismissRequest = { showNotificationPrompt = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 1f),
            title = { Text("Vil du ha påminnelser?") },
            text = {
                Text(
                    "Vi kan sende deg et varsel før foredragene du favorittmarkerer starter. For at det skal fungere, ber vi om tillatelse til å sende varslinger."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNotificationPrompt = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                ) {
                    Text("Be om tillatelse")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationPrompt = false }) {
                    Text("Ikke nå")
                }
            }
        )
    }
}

private fun needsNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) != PackageManager.PERMISSION_GRANTED
}
