package com.olavbg.javazone.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.*
import java.time.format.DateTimeFormatter

import androidx.compose.ui.tooling.preview.Preview
import com.olavbg.javazone.ui.theme.JavaZoneTheme
import android.content.Intent
import android.provider.Settings
import android.net.Uri
import android.os.Build
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.CheckCircle

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
    
    val canScheduleExact = viewModel.canScheduleExactAlarms()

    BackHandler(onBack = onBackClick)

    SettingsContent(
        notificationLeadTime = notificationLeadTime,
        simulatedTimeOffset = simulatedTimeOffset,
        canScheduleExact = canScheduleExact,
        onNotificationLeadTimeChange = viewModel::setNotificationLeadTime,
        onSimulatedTimeChange = viewModel::setSimulatedTime,
        onResetSimulation = viewModel::resetSimulation,
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
    onNotificationLeadTimeChange: (Int) -> Unit,
    onSimulatedTimeChange: (LocalDateTime) -> Unit,
    onResetSimulation: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val simulatedInstant = Instant.now().plusMillis(simulatedTimeOffset)
    val simulatedDateTime = simulatedInstant.atZone(ZoneId.of("Europe/Oslo")).toLocalDateTime()

    var showDatePicker by remember { mutableStateOf(value = false) }
    var showTimePicker by remember { mutableStateOf(value = false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(top = padding.calculateTopPadding())
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
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
                    "Notification Lead Time",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "How many minutes before a session starts should you be notified?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                
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
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            HorizontalDivider()

            SettingsSection(title = "Time Simulation") {
                Text(
                    "Simulated Time",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Simulate the app's current time. Useful for demoing 'NOW' indicator and past session logic.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Current Simulation:", style = MaterialTheme.typography.labelMedium)
                            Text(
                                if (simulatedTimeOffset == 0L) "Actual Time" else simulatedDateTime.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, HH:mm")),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (simulatedTimeOffset != 0L) {
                            IconButton(onClick = onResetSimulation) {
                                Icon(Icons.Rounded.Restore, contentDescription = "Reset Simulation")
                            }
                        }
                    }
                }

                Button(
                    onClick = onResetSimulation,
                    enabled = simulatedTimeOffset != 0L,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Reset to Actual Time")
                }
                
                Spacer(modifier = Modifier.height(32.dp + contentPadding.calculateBottomPadding()))
            }
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
            onNotificationLeadTimeChange = {},
            onSimulatedTimeChange = {},
            onResetSimulation = {},
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
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        content()
    }
}
