package com.olavbg.javazone.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.olavbg.javazone.model.AppLanguage
import com.olavbg.javazone.model.BackgroundMode
import com.olavbg.javazone.model.ThemeMode
import com.olavbg.javazone.notifications.ConferenceDoneReceiver
import com.olavbg.javazone.R
import com.olavbg.javazone.ui.components.DonationButtons
import com.olavbg.javazone.ui.theme.JavaZoneTheme
import com.olavbg.javazone.ui.theme.LocalJavaZoneThemeTokens
import com.olavbg.javazone.util.AppLocale
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val GITHUB_URL = "https://github.com/olavbg/JavaZone"

private enum class SettingsDialog { LeadTime, Theme, Language, Background }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val notificationLeadTime by viewModel.notificationLeadTime.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val simulatedTimeOffset by viewModel.simulatedTimeOffset.collectAsState()
    val backgroundMode by viewModel.backgroundMode.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val batteryHintDismissed by viewModel.batteryHintDismissed.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val missedReminderCount by viewModel.missedReminderCount.collectAsState()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var permissions by remember { mutableStateOf(computeAppPermissions(context)) }
    val permissionRequestLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissions = computeAppPermissions(context)
        if (!granted && !shouldShowPermissionRationale(context, Manifest.permission.POST_NOTIFICATIONS)) {
            openAppNotificationSettings(context)
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissions = computeAppPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val onPermissionsAction: () -> Unit = {
        val current = computeAppPermissions(context)
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !current.canPostNotifications ->
                permissionRequestLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !current.canScheduleExact ->
                openExactAlarmSettings(context)
        }
    }
    val onOpenNotificationSettings: () -> Unit = { openAppNotificationSettings(context) }

    val scope = rememberCoroutineScope()
    val onLanguageChange: (AppLanguage) -> Unit = { language ->
        scope.launch {
            viewModel.setAppLanguage(language)
            AppLocale.apply(context, language)
        }
    }

    BackHandler(onBack = onBackClick)

    SettingsContent(
        notificationLeadTime = notificationLeadTime,
        notificationsEnabled = notificationsEnabled,
        simulatedTimeOffset = simulatedTimeOffset,
        permissions = permissions,
        backgroundMode = backgroundMode,
        themeMode = themeMode,
        batteryHintDismissed = batteryHintDismissed,
        appLanguage = appLanguage,
        missedReminderCount = missedReminderCount,
        onNotificationLeadTimeChange = viewModel::setNotificationLeadTime,
        onNotificationsEnabledChange = viewModel::setNotificationsEnabled,
        onSimulatedTimeChange = viewModel::setSimulatedTime,
        onResetSimulation = viewModel::resetSimulation,
        onBackgroundModeChange = viewModel::setBackgroundMode,
        onThemeModeChange = viewModel::setThemeMode,
        onAppLanguageChange = onLanguageChange,
        onPermissionsAction = onPermissionsAction,
        onOpenNotificationSettings = onOpenNotificationSettings,
        onDismissBatteryHint = viewModel::dismissBatteryHint,
        onBackClick = onBackClick,
        modifier = modifier,
        contentPadding = contentPadding,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    notificationLeadTime: Int,
    notificationsEnabled: Boolean,
    simulatedTimeOffset: Long,
    permissions: AppPermissions,
    backgroundMode: BackgroundMode,
    themeMode: ThemeMode,
    batteryHintDismissed: Boolean,
    appLanguage: AppLanguage,
    missedReminderCount: Int,
    onNotificationLeadTimeChange: (Int) -> Unit,
    onNotificationsEnabledChange: (Boolean) -> Unit,
    onSimulatedTimeChange: (LocalDateTime) -> Unit,
    onResetSimulation: () -> Unit,
    onBackgroundModeChange: (BackgroundMode) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onPermissionsAction: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onDismissBatteryHint: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    val context = LocalContext.current
    val showTimeSimulation = remember(context) { isLocalBuild(context) }
    val simulatedInstant = Instant.now().plusMillis(simulatedTimeOffset)
    val simulatedDateTime = simulatedInstant.atZone(ZoneId.of("Europe/Oslo")).toLocalDateTime()

    fun openBrowser(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        }
    }

    var showDatePicker by remember { mutableStateOf(value = false) }
    var showTimePicker by remember { mutableStateOf(value = false) }
    var batteryHintExpanded by remember { mutableStateOf(false) }
    var dialogToShow by remember { mutableStateOf<SettingsDialog?>(null) }

    // Navigation bar inset added to the scroll content so the screen can draw behind it.
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(
                    alpha = LocalJavaZoneThemeTokens.current.topBarSurfaceAlpha
                ),
                shadowElevation = 2.dp
            ) {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings)) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(top = padding.calculateTopPadding())
                .padding(horizontal = 12.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            SettingsCard(title = stringResource(R.string.settings_section_notifications)) {
                SettingsSwitchRow(
                    label = stringResource(R.string.setting_notifications_enabled),
                    secondaryLabel = stringResource(R.string.setting_notifications_enabled_description),
                    checked = notificationsEnabled,
                    onCheckedChange = onNotificationsEnabledChange
                )
                if (notificationsEnabled) {
                    if (permissions.allGranted) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    } else {
                        PermissionsWarningCard(
                            permissions = permissions,
                            onPermissionsAction = onPermissionsAction,
                            onOpenNotificationSettings = onOpenNotificationSettings
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    SettingsRow(
                        label = stringResource(R.string.setting_lead_time),
                        value = stringResource(R.string.duration_minutes, notificationLeadTime),
                        onClick = { dialogToShow = SettingsDialog.LeadTime }
                    )
                    if (permissions.canPostNotifications &&
                        permissions.showBatteryHint &&
                        missedReminderCount > 0 &&
                        !batteryHintDismissed
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { batteryHintExpanded = !batteryHintExpanded }
                                .padding(start = 4.dp, top = 2.dp, bottom = 2.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.battery_hint_title),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (batteryHintExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            BatteryOptimizationHintCard(
                                onOpenBatterySettings = { openBatteryOptimizationSettings(context) },
                                onDismiss = onDismissBatteryHint
                            )
                        }
                    }
                    if (isLocalBuild(context)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            TextButton(
                                onClick = { ConferenceDoneReceiver.showConferenceDoneNotification(context) }
                            ) {
                                Text(stringResource(R.string.notification_test_button))
                            }
                        }
                    }
                }
            }

            SettingsCard(title = stringResource(R.string.settings_section_appearance)) {
                SettingsRow(
                    label = stringResource(R.string.setting_theme),
                    value = themeModeOptionLabel(themeMode),
                    secondaryValue = if (themeMode == ThemeMode.System) themeModeResolvedSystemLabel() else null,
                    onClick = { dialogToShow = SettingsDialog.Theme }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                SettingsRow(
                    label = stringResource(R.string.setting_language),
                    value = languageOptionLabel(appLanguage),
                    secondaryValue = if (appLanguage == AppLanguage.System) languageResolvedSystemLabel() else null,
                    onClick = { dialogToShow = SettingsDialog.Language }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                SettingsRow(
                    label = stringResource(R.string.setting_background),
                    value = backgroundModeLabel(backgroundMode),
                    onClick = { dialogToShow = SettingsDialog.Background }
                )
            }

            if (showTimeSimulation) {
                SettingsCard(title = stringResource(R.string.settings_section_time_simulation)) {
                    Text(
                        stringResource(R.string.time_simulation_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showDatePicker = true }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(stringResource(R.string.time_simulation_current_label), style = MaterialTheme.typography.labelMedium)
                            Text(
                                if (simulatedTimeOffset == 0L) stringResource(R.string.time_simulation_actual_time) else simulatedDateTime.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, HH:mm")),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = onResetSimulation,
                            enabled = simulatedTimeOffset != 0L
                        ) {
                            Text(stringResource(R.string.time_simulation_reset))
                        }
                    }
                }
            }

            SettingsCard(title = stringResource(R.string.settings_section_about)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.about_paragraph_1),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        stringResource(R.string.about_paragraph_2),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        stringResource(R.string.about_paragraph_3),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        stringResource(R.string.about_paragraph_4),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    DonationButtons(modifier = Modifier.fillMaxWidth())
                }
            }

            SettingsCard(title = stringResource(R.string.settings_section_open_source)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openBrowser(GITHUB_URL) }
                        .padding(vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Rounded.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            stringResource(R.string.about_open_source_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            stringResource(R.string.about_open_source_link),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.about_privacy_paragraph),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(32.dp + navBarBottom + contentPadding.calculateBottomPadding()))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = simulatedDateTime.atZone(ZoneId.of("Europe/Oslo")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedDate = Instant.ofEpochMilli(datePickerState.selectedDateMillis ?: System.currentTimeMillis())
                            .atZone(ZoneId.of("Europe/Oslo"))
                            .toLocalDate()
                        
                        val newDateTime = LocalDateTime.of(selectedDate, simulatedDateTime.toLocalTime())
                        onSimulatedTimeChange(newDateTime)
                        showDatePicker = false
                        showTimePicker = true
                    }
                ) {
                    Text(stringResource(R.string.next))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = simulatedDateTime.hour,
            initialMinute = simulatedDateTime.minute
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 1f),
            confirmButton = {
                TextButton(
                    onClick = {
                        val newDateTime = simulatedDateTime.withHour(timePickerState.hour).withMinute(timePickerState.minute)
                        onSimulatedTimeChange(newDateTime)
                        showTimePicker = false
                    }
                ) {
                    Text(stringResource(R.string.time_simulation_set_time))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }

    when (val dialog = dialogToShow) {
        SettingsDialog.LeadTime -> SelectionDialog(
            title = stringResource(R.string.setting_lead_time),
            subtitle = stringResource(R.string.notification_lead_time_description),
            options = listOf(5, 10, 15, 20, 25, 30),
            selected = notificationLeadTime,
            valueLabel = { minutes -> stringResource(R.string.duration_minutes, minutes) },
            leadingContent = { minutes, isSelected ->
                MinuteClockIcon(
                    minutes = minutes,
                    isSelected = isSelected
                )
            },
            onSelect = onNotificationLeadTimeChange,
            onDismiss = { dialogToShow = null }
        )
        SettingsDialog.Theme -> SelectionDialog(
            title = stringResource(R.string.setting_theme),
            options = listOf(ThemeMode.Dark, ThemeMode.Light, ThemeMode.System),
            selected = themeMode,
            valueLabel = { themeModeOptionLabel(it) },
            supportLabel = { mode ->
                if (mode == ThemeMode.System) themeModeResolvedSystemLabel() else null
            },
            icon = { mode ->
                when (mode) {
                    ThemeMode.Dark -> Icons.Rounded.DarkMode
                    ThemeMode.Light -> Icons.Rounded.LightMode
                    ThemeMode.System -> Icons.Rounded.Smartphone
                }
            },
            onSelect = onThemeModeChange,
            onDismiss = { dialogToShow = null }
        )
        SettingsDialog.Language -> SelectionDialog(
            title = stringResource(R.string.setting_language),
            options = listOf(AppLanguage.Norwegian, AppLanguage.English, AppLanguage.System),
            selected = appLanguage,
            valueLabel = { languageOptionLabel(it) },
            supportLabel = { lang ->
                if (lang == AppLanguage.System) languageResolvedSystemLabel() else null
            },
            leadingContent = { lang, isSelected ->
                when (lang) {
                    AppLanguage.Norwegian -> FlagEmojiBadge(
                        emoji = "🇳🇴",
                        isSelected = isSelected
                    )
                    AppLanguage.English -> FlagEmojiBadge(
                        emoji = "🇬🇧",
                        isSelected = isSelected
                    )
                    AppLanguage.System -> SelectionTileIcon(
                        imageVector = Icons.Rounded.Smartphone,
                        isSelected = isSelected
                    )
                }
            },
            onSelect = onAppLanguageChange,
            onDismiss = { dialogToShow = null }
        )
        SettingsDialog.Background -> SelectionDialog(
            title = stringResource(R.string.setting_background),
            options = listOf(BackgroundMode.Animated, BackgroundMode.Static, BackgroundMode.None),
            selected = backgroundMode,
            valueLabel = { backgroundModeLabel(it) },
            supportLabel = { backgroundModeDetail(it) },
            icon = { mode ->
                when (mode) {
                    BackgroundMode.Animated -> Icons.Rounded.AutoAwesome
                    BackgroundMode.Static -> Icons.Rounded.Image
                    BackgroundMode.None -> Icons.Rounded.Block
                }
            },
            onSelect = onBackgroundModeChange,
            onDismiss = { dialogToShow = null }
        )
        null -> Unit
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun SettingsScreenPreview() {
    JavaZoneTheme {
        SettingsContent(
            notificationLeadTime = 10,
            notificationsEnabled = true,
            simulatedTimeOffset = 0,
            permissions = AppPermissions(canScheduleExact = true, canPostNotifications = true),
            backgroundMode = BackgroundMode.Animated,
            themeMode = ThemeMode.Dark,
            batteryHintDismissed = false,
            appLanguage = AppLanguage.System,
            missedReminderCount = 0,
            onNotificationLeadTimeChange = {},
            onNotificationsEnabledChange = {},
            onSimulatedTimeChange = {},
            onResetSimulation = {},
            onBackgroundModeChange = {},
            onThemeModeChange = {},
            onAppLanguageChange = {},
            onPermissionsAction = {},
            onOpenNotificationSettings = {},
            onDismissBatteryHint = {},
            onBackClick = {}
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp, top = 4.dp)
            )
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    secondaryLabel: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!secondaryLabel.isNullOrEmpty()) {
                Text(
                    text = secondaryLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsRow(
    label: String,
    value: String,
    secondaryValue: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!secondaryValue.isNullOrEmpty()) {
                Text(
                    text = secondaryValue,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SelectionTileIcon(
    imageVector: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun MinuteClockIcon(
    minutes: Int,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val handColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val arcColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.20f)
    }
    val outlineColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.70f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.70f)
    }

    Box(
        modifier = modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            val strokeWidth = 1.6.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Dynamic pie arc showing time elapsed / lead time
            val sweepAngle = (minutes.coerceIn(1, 60) * 6f)
            drawArc(
                color = arcColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )

            // Outer clock dial
            drawCircle(
                color = outlineColor,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // Hour hand (pointing straight up to 12 o'clock)
            val hourHandLength = radius * 0.52f
            drawLine(
                color = handColor,
                start = center,
                end = Offset(center.x, center.y - hourHandLength),
                strokeWidth = strokeWidth * 1.1f,
                cap = StrokeCap.Round
            )

            // Minute hand (pointing dynamically to the chosen minutes)
            val minuteHandLength = radius * 0.78f
            val angleRad = Math.toRadians((minutes * 6 - 90).toDouble())
            val minuteEndX = center.x + minuteHandLength * cos(angleRad).toFloat()
            val minuteEndY = center.y + minuteHandLength * sin(angleRad).toFloat()
            drawLine(
                color = handColor,
                start = center,
                end = Offset(minuteEndX, minuteEndY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Center pivot dot
            drawCircle(
                color = handColor,
                radius = strokeWidth * 0.9f,
                center = center
            )
        }
    }
}

@Composable
private fun FlagEmojiBadge(
    emoji: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = 20.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun <T> SelectionDialog(
    title: String,
    subtitle: String? = null,
    options: List<T>,
    selected: T,
    valueLabel: @Composable (T) -> String,
    supportLabel: (@Composable (T) -> String?)? = null,
    icon: ((T) -> ImageVector)? = null,
    leadingContent: (@Composable (T, Boolean) -> Unit)? = null,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 1f),
        shape = RoundedCornerShape(28.dp),
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { option ->
                    val isSelected = option == selected
                    val backgroundColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f)
                    }
                    val borderColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = backgroundColor,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = borderColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = isSelected,
                                    role = Role.RadioButton,
                                    onClick = {
                                        onSelect(option)
                                        onDismiss()
                                    }
                                )
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (leadingContent != null) {
                                leadingContent(option, isSelected)
                                Spacer(modifier = Modifier.width(14.dp))
                            } else if (icon != null) {
                                SelectionTileIcon(
                                    imageVector = icon(option),
                                    isSelected = isSelected
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = valueLabel(option),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (supportLabel != null) {
                                    val support = supportLabel(option)
                                    if (!support.isNullOrEmpty()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = support,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = 1.5.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Preview
@Composable
fun SelectionDialogPreview() {
    JavaZoneTheme {
        SelectionDialog(
            title = "Varsel før start",
            subtitle = "Hvor mange minutter før et foredrag starter vil du varsles?",
            options = listOf(5, 10, 15, 20, 25, 30),
            selected = 10,
            valueLabel = { "$it min" },
            leadingContent = { minutes, isSelected ->
                MinuteClockIcon(
                    minutes = minutes,
                    isSelected = isSelected
                )
            },
            onSelect = {},
            onDismiss = {}
        )
    }
}

@Preview
@Composable
fun LanguageSelectionDialogPreview() {
    JavaZoneTheme {
        SelectionDialog(
            title = "Språk",
            subtitle = "Velg språket i appen.",
            options = listOf(AppLanguage.Norwegian, AppLanguage.English, AppLanguage.System),
            selected = AppLanguage.Norwegian,
            valueLabel = { languageOptionLabel(it) },
            supportLabel = { if (it == AppLanguage.System) "Norsk" else null },
            leadingContent = { lang, isSelected ->
                when (lang) {
                    AppLanguage.Norwegian -> FlagEmojiBadge(
                        emoji = "🇳🇴",
                        isSelected = isSelected
                    )
                    AppLanguage.English -> FlagEmojiBadge(
                        emoji = "🇬🇧",
                        isSelected = isSelected
                    )
                    AppLanguage.System -> SelectionTileIcon(
                        imageVector = Icons.Rounded.Smartphone,
                        isSelected = isSelected
                    )
                }
            },
            onSelect = {},
            onDismiss = {}
        )
    }
}

@Composable
private fun themeModeOptionLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.Dark -> stringResource(R.string.theme_mode_dark)
    ThemeMode.Light -> stringResource(R.string.theme_mode_light)
    ThemeMode.System -> stringResource(R.string.theme_mode_system)
}

@Composable
private fun themeModeResolvedSystemLabel(): String = if (isSystemInDarkTheme()) {
    stringResource(R.string.theme_mode_dark)
} else {
    stringResource(R.string.theme_mode_light)
}

@Composable
private fun languageOptionLabel(language: AppLanguage): String = when (language) {
    AppLanguage.Norwegian -> stringResource(R.string.language_norwegian)
    AppLanguage.English -> stringResource(R.string.language_english)
    AppLanguage.System -> stringResource(R.string.language_system)
}

@Composable
private fun languageResolvedSystemLabel(): String {
    val systemLocale = AppLocale.systemLocale()
    val isNorwegian = systemLocale.language.startsWith("no", ignoreCase = true) ||
            systemLocale.language.startsWith("nb", ignoreCase = true) ||
            systemLocale.language.startsWith("nn", ignoreCase = true)
    return if (isNorwegian) {
        stringResource(R.string.language_norwegian)
    } else {
        stringResource(R.string.language_english)
    }
}

@Composable
private fun backgroundModeLabel(mode: BackgroundMode): String = when (mode) {
    BackgroundMode.None -> stringResource(R.string.background_mode_none)
    BackgroundMode.Static -> stringResource(R.string.background_mode_static)
    BackgroundMode.Animated -> stringResource(R.string.background_mode_animated)
}

@Composable
private fun backgroundModeDetail(mode: BackgroundMode): String = when (mode) {
    BackgroundMode.None -> stringResource(R.string.background_mode_none_detail)
    BackgroundMode.Static -> stringResource(R.string.background_mode_static_detail)
    BackgroundMode.Animated -> stringResource(R.string.background_mode_animated_detail)
}