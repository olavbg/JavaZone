package com.olavbg.javazone.ui.settings

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.olavbg.javazone.model.BackgroundMode
import com.olavbg.javazone.notifications.ConferenceDoneReceiver
import com.olavbg.javazone.ui.components.DonationButtons
import com.olavbg.javazone.ui.theme.JavaZoneTheme
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

    BackHandler(onBack = onBackClick)

    SettingsContent(
        notificationLeadTime = notificationLeadTime,
        simulatedTimeOffset = simulatedTimeOffset,
        permissions = permissions,
        backgroundMode = backgroundMode,
        batteryHintDismissed = batteryHintDismissed,
        missedReminderCount = missedReminderCount,
        onNotificationLeadTimeChange = viewModel::setNotificationLeadTime,
        onSimulatedTimeChange = viewModel::setSimulatedTime,
        onResetSimulation = viewModel::resetSimulation,
        onBackgroundModeChange = viewModel::setBackgroundMode,
        onPermissionsAction = onPermissionsAction,
        onOpenNotificationSettings = onOpenNotificationSettings,
        onDismissBatteryHint = viewModel::dismissBatteryHint,
        onBackClick = onBackClick,
        modifier = modifier,
        contentPadding = contentPadding,
    )
}

@Composable
private fun PermissionsWarningCard(
    permissions: AppPermissions,
    onPermissionsAction: () -> Unit,
    onOpenNotificationSettings: () -> Unit
) {
    val message = when {
        !permissions.canPostNotifications && !permissions.canScheduleExact ->
            "Notifications and exact alarms are disabled. Tap to request access."
        !permissions.canPostNotifications ->
            "Notifications are disabled. Tap to request access."
        else ->
            "Exact alarms are disabled. Tap to request access."
    }
    Card(
        onClick = onPermissionsAction,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                Text(
                    text = "Permissions needed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            TextButton(
                onClick = onOpenNotificationSettings,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Settings", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun PermissionsGrantedCard(onOpenNotificationSettings: () -> Unit) {
    Surface(
        onClick = onOpenNotificationSettings,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                Text(
                    text = "Notifications enabled",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap to open this app's notification settings.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BatteryOptimizationHintCard(
    onOpenBatterySettings: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "Batterioptimalisering",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
            Text(
                text = "Opplever du at varsler ikke kommer til forventet tid? Det kan skyldes batterioptimalisering. Prøv å ekskludere JavaZone i innstillingene for batterioptimalisering.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(top = 12.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Ikke vis igjen")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = onOpenBatterySettings,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text("Åpne innstillinger")
                }
            }
        }
    }
}

private fun computeAppPermissions(context: Context): AppPermissions {
    val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
    } else {
        true
    }
    val canPostNotifications = when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> true
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED -> true
        else -> false
    }
    return AppPermissions(
        canScheduleExact = canScheduleExact,
        canPostNotifications = canPostNotifications,
        isAggressiveOem = isAggressiveOem(),
        isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations(context)
    )
}

private val AGGRESSIVE_OEM_MANUFACTURERS = setOf(
    "xiaomi", "redmi", "poco",
    "huawei", "honor",
    "oppo", "realme", "oneplus",
    "vivo", "iqoo",
    "samsung",
    "meizu",
    "asus", "nokia", "tecno", "infinix", "itel", "wiko"
)

private fun isAggressiveOem(): Boolean = isAggressiveOem(Build.MANUFACTURER, Build.BRAND)

internal fun isAggressiveOem(manufacturer: String, brand: String): Boolean {
    val manufacturerLower = manufacturer.lowercase()
    val brandLower = brand.lowercase()
    return AGGRESSIVE_OEM_MANUFACTURERS.any {
        manufacturerLower.contains(it) || brandLower.contains(it)
    }
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

private fun openBatteryOptimizationSettings(context: Context) {
    runCatching {
        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }.onFailure {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            )
        }
    }
}

@Suppress("InlinedApi")
private fun openExactAlarmSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        )
    }
}

private fun openAppNotificationSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        )
    }
}

private fun shouldShowPermissionRationale(context: Context, permission: String): Boolean {
    val activity = context.findActivity() ?: return true
    return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun isLocalBuild(context: Context): Boolean {
    if ((context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) return true
    val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        runCatching {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        }.getOrNull()
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getInstallerPackageName(context.packageName)
    }
    return installer != "com.android.vending"
}

data class AppPermissions(
    val canScheduleExact: Boolean,
    val canPostNotifications: Boolean,
    val isAggressiveOem: Boolean = false,
    val isIgnoringBatteryOptimizations: Boolean = true
) {
    val allGranted: Boolean get() = canScheduleExact && canPostNotifications
    val showBatteryHint: Boolean get() = isAggressiveOem && !isIgnoringBatteryOptimizations
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    notificationLeadTime: Int,
    simulatedTimeOffset: Long,
    permissions: AppPermissions,
    backgroundMode: BackgroundMode,
    batteryHintDismissed: Boolean,
    missedReminderCount: Int,
    onNotificationLeadTimeChange: (Int) -> Unit,
    onSimulatedTimeChange: (LocalDateTime) -> Unit,
    onResetSimulation: () -> Unit,
    onBackgroundModeChange: (BackgroundMode) -> Unit,
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
                    title = { Text("Settings") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
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

            SettingsSection(title = "Notifications") {
                if (!permissions.allGranted) {
                    PermissionsWarningCard(
                        permissions = permissions,
                        onPermissionsAction = onPermissionsAction,
                        onOpenNotificationSettings = onOpenNotificationSettings
                    )
                }
                Text(
                    "How many minutes before a session starts should you be notified?",
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
                            Text("$minutes min")
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
                            text = "Får du ikke varsler som forventet?",
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
                        Text("Send test notification now")
                    }
                }
                if (permissions.allGranted) {
                    PermissionsGrantedCard(onOpenNotificationSettings = onOpenNotificationSettings)
                }
            }

            HorizontalDivider()

            SettingsSection(title = "Background") {
                Text(
                    "Choose how the diagonal bands behind the app content are rendered.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val modes = listOf(
                    BackgroundMode.None to "Ingen",
                    BackgroundMode.Static to "Statisk",
                    BackgroundMode.Animated to "Animasjon"
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
                        BackgroundMode.None -> "Only the plain background color."
                        BackgroundMode.Static -> "Bands visible, but frozen in place."
                        BackgroundMode.Animated -> "Bands drift, tilt and sweep on navigation."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (showTimeSimulation) {
                HorizontalDivider()

                SettingsSection(title = "Time Simulation") {
                    Text(
                        "Simulate the app's current time. Useful for demoing 'NOW' indicator and past session logic.",
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
                            Text("Current Simulation", style = MaterialTheme.typography.labelMedium)
                            Text(
                                if (simulatedTimeOffset == 0L) "Actual Time" else simulatedDateTime.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, HH:mm")),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = onResetSimulation,
                            enabled = simulatedTimeOffset != 0L
                        ) {
                            Text("Reset to Actual Time")
                        }
                    }
                }
            }

            HorizontalDivider()

            SettingsSection(title = "About") {
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
                            "Dette er en helt uoffisiell app, laget av en JavaZone-fan med et hobbyprosjekt som har fått eget liv. Målet er å utforske nye teknologier på fritiden, og å leke med AI.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Ja, appen er hovedsakelig vibe-kodet: AI-assistentene har banket på tastaturet mens jeg har stått bak og sagt \"Det ser bra ut!\". Mesteparten av tiden fungerer det overraskende bra. Resten av tiden er jeg glad for at dette kun er et hobbyprosjekt.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Uten JavaZone og JavaBin sine åpne API-er ville dette bare vært en god idé uten innhold. Takk for at dere deler!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Liker du app'en, og har lyst til å støtte videreutviklingen? Da setter jeg pris på et lite bidrag – enten via Vipps, eller \"Buy Me a Coffee\":",
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
                    Text("Next")
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
                    Text("Set Time")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
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
            missedReminderCount = 0,
            onNotificationLeadTimeChange = {},
            onSimulatedTimeChange = {},
            onResetSimulation = {},
            onBackgroundModeChange = {},
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
