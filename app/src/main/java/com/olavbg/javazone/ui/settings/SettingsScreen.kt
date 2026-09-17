package com.olavbg.javazone.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.olavbg.javazone.BuildConfig
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

    val canScheduleExact = viewModel.canScheduleExactAlarms()

    BackHandler(onBack = onBackClick)

    SettingsContent(
        notificationLeadTime = notificationLeadTime,
        simulatedTimeOffset = simulatedTimeOffset,
        backgroundMode = backgroundMode,
        canScheduleExact = canScheduleExact,
        onNotificationLeadTimeChange = viewModel::setNotificationLeadTime,
        onSimulatedTimeChange = viewModel::setSimulatedTime,
        onResetSimulation = viewModel::resetSimulation,
        onBackgroundModeChange = viewModel::setBackgroundMode,
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
    canScheduleExact: Boolean,
    backgroundMode: BackgroundMode,
    onNotificationLeadTimeChange: (Int) -> Unit,
    onSimulatedTimeChange: (LocalDateTime) -> Unit,
    onResetSimulation: () -> Unit,
    onBackgroundModeChange: (BackgroundMode) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    val context = LocalContext.current
    val simulatedInstant = Instant.now().plusMillis(simulatedTimeOffset)
    val simulatedDateTime = simulatedInstant.atZone(ZoneId.of("Europe/Oslo")).toLocalDateTime()

    var showDatePicker by remember { mutableStateOf(value = false) }
    var showTimePicker by remember { mutableStateOf(value = false) }

    // Navigation bar inset added to the scroll content so the screen can draw behind it.
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                )
            )
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SettingsSection(title = "Permissions") {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (canScheduleExact) 
                                MaterialTheme.colorScheme.surfaceVariant 
                            else 
                                MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (canScheduleExact) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = if (canScheduleExact) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                                Text(
                                    "Exact Alarms",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (canScheduleExact) "Enabled - Notifications will be precise." else "Disabled - Notifications might be delayed.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (!canScheduleExact) {
                                Button(
                                    onClick = {
                                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                        context.startActivity(intent)
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("Fix", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
                HorizontalDivider()
            }

            SettingsSection(title = "Notifications") {
                Text(
                    "How many minutes before a session starts should you be notified?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val options = listOf(5, 10, 15)
                options.forEach { minutes ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = notificationLeadTime == minutes,
                            onClick = { onNotificationLeadTimeChange(minutes) }
                        )
                        Text(
                            text = "$minutes minutes",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                if (BuildConfig.DEBUG) {
                    TextButton(
                        onClick = { ConferenceDoneReceiver.showConferenceDoneNotification(context) }
                    ) {
                        Text("Send test notification now")
                    }
                }
            }

            HorizontalDivider()

            SettingsSection(title = "Time Simulation") {
                Text(
                    "Simulate the app's current time. Useful for demoing 'NOW' indicator and past session logic.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Current Simulation", style = MaterialTheme.typography.labelMedium)
                        Text(
                            if (simulatedTimeOffset == 0L) "Actual Time" else simulatedDateTime.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, HH:mm")),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Button(
                    onClick = onResetSimulation,
                    enabled = simulatedTimeOffset != 0L,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset to Actual Time")
                }
            }

            HorizontalDivider()

            SettingsSection(title = "Background") {
                Text(
                    "Choose how the diagonal bands behind the app content are rendered.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val modeLabels = listOf(
                    BackgroundMode.None to ("Ingen bånd" to "Only the plain background color."),
                    BackgroundMode.Static to ("Statiske bånd" to "Bands visible, but frozen in place."),
                    BackgroundMode.Animated to ("Bånd med animasjon" to "Bands drift, tilt and sweep on navigation.")
                )
                modeLabels.forEach { (mode, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = backgroundMode == mode,
                            onClick = { onBackgroundModeChange(mode) }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = label.first,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = label.second,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Ja, appen er hovedsakelig vibe-kodet: AI-assistentene har banket på tastaturet mens jeg har stått bak og sagt \"Det ser bra ut!\". Mesteparten av tiden fungerer det overraskende bra. Resten av tiden er jeg glad for at dette kun er et hobbyprosjekt.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Uten JavaZone og JavaBin sine åpne API-er ville dette bare vært en god idé uten innhold. Takk for at dere deler!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Liker du app'en, og har lyst til å støtte videreutviklingen? Da setter jeg pris på et lite bidrag – enten via Vipps, eller \"Buy Me a Coffee\":",
                            style = MaterialTheme.typography.bodyMedium,
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
            canScheduleExact = true,
            backgroundMode = BackgroundMode.Animated,
            onNotificationLeadTimeChange = {},
            onSimulatedTimeChange = {},
            onResetSimulation = {},
            onBackgroundModeChange = {},
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
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        content()
    }
}
