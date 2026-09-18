package com.olavbg.javazone.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.olavbg.javazone.model.AppLanguage
import com.olavbg.javazone.model.BackgroundMode
import com.olavbg.javazone.notifications.ConferenceDoneReceiver
import com.olavbg.javazone.R
import com.olavbg.javazone.ui.components.DonationButtons
import com.olavbg.javazone.ui.theme.JavaZoneTheme
import com.olavbg.javazone.util.AppLocale
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val notificationLeadTime by viewModel.notificationLeadTime.collectAsState()
    val simulatedTimeOffset by viewModel.simulatedTimeOffset.collectAsState()
    val backgroundMode by viewModel.backgroundMode.collectAsState()
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
        simulatedTimeOffset = simulatedTimeOffset,
        permissions = permissions,
        backgroundMode = backgroundMode,
        batteryHintDismissed = batteryHintDismissed,
        appLanguage = appLanguage,
        missedReminderCount = missedReminderCount,
        onNotificationLeadTimeChange = viewModel::setNotificationLeadTime,
        onSimulatedTimeChange = viewModel::setSimulatedTime,
        onResetSimulation = viewModel::resetSimulation,
        onBackgroundModeChange = viewModel::setBackgroundMode,
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
    simulatedTimeOffset: Long,
    permissions: AppPermissions,
    backgroundMode: BackgroundMode,
    batteryHintDismissed: Boolean,
    appLanguage: AppLanguage,
    missedReminderCount: Int,
    onNotificationLeadTimeChange: (Int) -> Unit,
    onSimulatedTimeChange: (LocalDateTime) -> Unit,
    onResetSimulation: () -> Unit,
    onBackgroundModeChange: (BackgroundMode) -> Unit,
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

    var showDatePicker by remember { mutableStateOf(value = false) }
    var showTimePicker by remember { mutableStateOf(value = false) }
    var batteryHintExpanded by remember { mutableStateOf(false) }

    // Navigation bar inset added to the scroll content so the screen can draw behind it.
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
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
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SettingsSection(title = stringResource(R.string.settings_section_notifications)) {
                if (!permissions.allGranted) {
                    PermissionsWarningCard(
                        permissions = permissions,
                        onPermissionsAction = onPermissionsAction,
                        onOpenNotificationSettings = onOpenNotificationSettings
                    )
                }
                Text(
                    stringResource(R.string.notification_lead_time_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(5, 10, 15).forEachIndexed { index, minutes ->
                        SegmentedButton(
                            selected = notificationLeadTime == minutes,
                            onClick = { onNotificationLeadTimeChange(minutes) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 3)
                        ) {
                            Text(stringResource(R.string.duration_minutes, minutes))
                        }
                    }
                }
                if (permissions.canPostNotifications &&
                    permissions.showBatteryHint &&
                    missedReminderCount > 0 &&
                    !batteryHintDismissed
                ) {
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
                        BatteryOptimizationHintCard(
                            onOpenBatterySettings = { openBatteryOptimizationSettings(context) },
                            onDismiss = onDismissBatteryHint
                        )
                    }
                }
                if (isLocalBuild(context)) {
                    TextButton(
                        onClick = { ConferenceDoneReceiver.showConferenceDoneNotification(context) }
                    ) {
                        Text(stringResource(R.string.notification_test_button))
                    }
                }
                if (permissions.allGranted) {
                    PermissionsGrantedCard(onOpenNotificationSettings = onOpenNotificationSettings)
                }
            }

            HorizontalDivider()

            SettingsSection(title = stringResource(R.string.settings_section_language)) {
                Text(
                    stringResource(R.string.settings_language_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val languages = listOf(
                    AppLanguage.System to stringResource(R.string.language_system),
                    AppLanguage.Norwegian to stringResource(R.string.language_norwegian),
                    AppLanguage.English to stringResource(R.string.language_english),
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    languages.forEachIndexed { index, (language, label) ->
                        SegmentedButton(
                            selected = appLanguage == language,
                            onClick = { onAppLanguageChange(language) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = languages.size)
                        ) {
                            Text(label)
                        }
                    }
                }
            }

            HorizontalDivider()

            SettingsSection(title = stringResource(R.string.settings_section_background)) {
                Text(
                    stringResource(R.string.background_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val modes = listOf(
                    BackgroundMode.None to stringResource(R.string.background_mode_none),
                    BackgroundMode.Static to stringResource(R.string.background_mode_static),
                    BackgroundMode.Animated to stringResource(R.string.background_mode_animated)
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    modes.forEachIndexed { index, (mode, label) ->
                        SegmentedButton(
                            selected = backgroundMode == mode,
                            onClick = { onBackgroundModeChange(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size)
                        ) {
                            Text(label)
                        }
                    }
                }
                Text(
                    text = when (backgroundMode) {
                        BackgroundMode.None -> stringResource(R.string.background_mode_none_detail)
                        BackgroundMode.Static -> stringResource(R.string.background_mode_static_detail)
                        BackgroundMode.Animated -> stringResource(R.string.background_mode_animated_detail)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (showTimeSimulation) {
                HorizontalDivider()

                SettingsSection(title = stringResource(R.string.settings_section_time_simulation)) {
                    Text(
                        stringResource(R.string.time_simulation_description),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

            HorizontalDivider()

            SettingsSection(title = stringResource(R.string.settings_section_about)) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            stringResource(R.string.about_paragraph_1),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            stringResource(R.string.about_paragraph_2),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            stringResource(R.string.about_paragraph_3),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            stringResource(R.string.about_paragraph_4),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        DonationButtons(modifier = Modifier.fillMaxWidth())
                    }
                }
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
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun SettingsScreenPreview() {
    JavaZoneTheme {
        SettingsContent(
            notificationLeadTime = 10,
            simulatedTimeOffset = 0,
            permissions = AppPermissions(canScheduleExact = true, canPostNotifications = true),
            backgroundMode = BackgroundMode.Animated,
            batteryHintDismissed = false,
            appLanguage = AppLanguage.System,
            missedReminderCount = 0,
            onNotificationLeadTimeChange = {},
            onSimulatedTimeChange = {},
            onResetSimulation = {},
            onBackgroundModeChange = {},
            onAppLanguageChange = {},
            onPermissionsAction = {},
            onOpenNotificationSettings = {},
            onDismissBatteryHint = {},
            onBackClick = {}
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        content()
    }
}