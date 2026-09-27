package com.olavbg.javazone.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.olavbg.javazone.R
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
    val context = LocalContext.current
    val networkMonitor = remember(context) { com.olavbg.javazone.util.NetworkMonitor(context) }
    val timelineViewModel: TimelineViewModel = viewModel(
        factory = TimelineViewModelFactory(repository, settingsRepository, networkMonitor)
    )

    val showLiveBanners by timelineViewModel.showLiveIndicators.collectAsState()

    var showNotificationPrompt by remember { mutableStateOf(false) }
    val promptShown by settingsRepository.notificationPromptShown.collectAsState(initial = true)
    val favoriteHintDismissed by settingsRepository.favoriteHintDismissed.collectAsState(initial = true)
    val notificationsEnabled by settingsRepository.notificationsEnabled.collectAsState(initial = true)
    val leadTime by settingsRepository.notificationLeadTime.collectAsState(initial = 10)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Guards against double-fire/double-tap pushing duplicate destinations onto the back
    // stack (which would otherwise require extra back presses to unwind).
    var lastPushAt by remember { mutableLongStateOf(0L) }

    val backStack = if (initialSessionId != null) {
        rememberNavBackStack(
            NavDestination.Timeline, NavDestination.SessionDetail(initialSessionId)
        )
    } else {
        rememberNavBackStack(NavDestination.Timeline)
    }

    val favoriteCount by repository.favoriteCount.collectAsState(initial = null)
    var previousFavoriteCount by remember { mutableStateOf<Int?>(null) }

    fun showFavoriteHintSnackbar() {
        scope.launch {
            settingsRepository.dismissFavoriteHint()
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.favorite_reminder_hint, leadTime),
                actionLabel = context.getString(R.string.change_lead_time),
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                val now = SystemClock.uptimeMillis()
                if (now - lastPushAt >= 350L && backStack.lastOrNull() != NavDestination.Settings) {
                    lastPushAt = now
                    backStack.add(NavDestination.Settings)
                }
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (!favoriteHintDismissed && notificationsEnabled) {
                showFavoriteHintSnackbar()
            }
        } else {
            scope.launch {
                settingsRepository.dismissFavoriteHint()
            }
        }
    }

    LaunchedEffect(favoriteCount, promptShown, favoriteHintDismissed, notificationsEnabled) {
        val current = favoriteCount ?: return@LaunchedEffect
        val previous = previousFavoriteCount
        previousFavoriteCount = current
        if (previous == null || current <= previous) return@LaunchedEffect

        if (!promptShown && needsNotificationPermission(context)) {
            showNotificationPrompt = true
            settingsRepository.markNotificationPromptShown()
            return@LaunchedEffect
        }

        if (!favoriteHintDismissed && !needsNotificationPermission(context) && notificationsEnabled) {
            showFavoriteHintSnackbar()
        }
    }

    var showDonationDialog by rememberSaveable { mutableStateOf(showDonationOnLaunch) }

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
        Box(modifier = Modifier.fillMaxSize()) {
            NavDisplay(
                backStack = backStack,
                sceneStrategies = listOf(SinglePaneSceneStrategy()),
                sharedTransitionScope = this@SharedTransitionLayout,
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
                                val now = SystemClock.uptimeMillis()
                                val debounced = now - lastPushAt >= 350L
                                lastPushAt = now
                                if (debounced && backStack.lastOrNull() != NavDestination.SessionDetail(id, year)) {
                                    backStack.add(NavDestination.SessionDetail(id, year))
                                }
                            }, onSettingsClick = {
                                val now = SystemClock.uptimeMillis()
                                val debounced = now - lastPushAt >= 350L
                                lastPushAt = now
                                if (debounced && backStack.lastOrNull() != NavDestination.Settings) {
                                    backStack.add(NavDestination.Settings)
                                }
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

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp,
                        start = 16.dp,
                        end = 16.dp
                    )
            ) { snackbarData ->
                Snackbar(
                    snackbarData = snackbarData,
                    shape = RoundedCornerShape(12.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    actionColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp)
                    )
                )
            }
        }
    }

    if (showDonationDialog) {
        AlertDialog(
            onDismissRequest = { showDonationDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 1f),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.donation_dialog_title))
                    Spacer(Modifier.width(12.dp))
                    // Logo after the title, same treatment as before (no tile, no transparency).
                    Image(
                        painter = painterResource(R.drawable.ic_logo_dialog),
                        contentDescription = null,
                        modifier = Modifier.size(44.dp)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Text(
                        stringResource(R.string.donation_dialog_text, SessionRepository.CURRENT_YEAR),
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
                        Text(stringResource(R.string.close))
                    }
                }
            }
        )
    }

    if (showNotificationPrompt) {
        AlertDialog(
            onDismissRequest = {
                showNotificationPrompt = false
                scope.launch { settingsRepository.dismissFavoriteHint() }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 1f),
            title = { Text(stringResource(R.string.notification_prompt_title)) },
            text = {
                Text(stringResource(R.string.notification_prompt_text))
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
                    Text(stringResource(R.string.notification_prompt_accept))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNotificationPrompt = false
                        scope.launch { settingsRepository.dismissFavoriteHint() }
                    }
                ) {
                    Text(stringResource(R.string.notification_prompt_dismiss))
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
