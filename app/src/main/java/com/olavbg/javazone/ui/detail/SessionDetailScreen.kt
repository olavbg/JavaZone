package com.olavbg.javazone.ui.detail

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.olavbg.javazone.data.repository.SessionRepository
import com.olavbg.javazone.data.repository.SettingsRepository
import com.olavbg.javazone.model.Session
import com.olavbg.javazone.model.Speaker
import com.olavbg.javazone.ui.components.FormatBadge
import com.olavbg.javazone.ui.components.resolveVideoUrl
import com.olavbg.javazone.ui.theme.JavaGreen
import com.olavbg.javazone.ui.theme.WorkshopPurple
import com.olavbg.javazone.ui.theme.JavaBlue
import com.olavbg.javazone.util.calculateSessionDurationMinutes
import com.olavbg.javazone.util.formatDay
import com.olavbg.javazone.util.formatTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

@Composable
fun SessionDetailScreen(
    sessionId: String,
    year: Int,
    repository: SessionRepository,
    settingsRepository: SettingsRepository,
    onBackClick: () -> Unit,
    onSpeakerClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isCurrentYear = year == 2026
    var session by remember(sessionId, year) { mutableStateOf<Session?>(null) }
    var loading by remember(sessionId, year) { mutableStateOf(true) }
    var simulatedTime by remember { mutableStateOf(Instant.now()) }

    val offset by settingsRepository.simulatedTimeOffset.collectAsState(initial = 0L)
    LaunchedEffect(offset) { simulatedTime = Instant.now().plusMillis(offset) }

    LaunchedEffect(sessionId, year) {
        loading = true
        session = if (isCurrentYear) {
            repository.getSessions().first().find { it.id == sessionId }
        } else {
            repository.getArchiveSessions(year).find { it.id == sessionId }
        }
        loading = false
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Foredrag", fontWeight = FontWeight.Black)
                        Text("JavaZone $year", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Tilbake") }
                },
                actions = {
                    val current = session
                    if (current != null && isCurrentYear) {
                        IconButton(onClick = { scope.launch { repository.toggleFavorite(current.id, !current.isFavorite) } }) {
                            Icon(
                                if (current.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                "Favoritt",
                                tint = if (current.isFavorite) MaterialTheme.colorScheme.tertiary else LocalContentColor.current
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            session == null -> EmptyDetail(onBackClick, padding, year)
            else -> DetailContent(
                session = session!!,
                simulatedTime = simulatedTime,
                isCurrentYear = isCurrentYear,
                onSpeakerClick = onSpeakerClick,
                onVideoClick = {
                    val url = session?.videoUrl?.let(::resolveVideoUrl) ?: return@DetailContent
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
                },
                modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
                bottomPadding = padding.calculateBottomPadding() + contentPadding.calculateBottomPadding()
            )
        }
    }
}

@Composable
private fun DetailContent(
    session: Session,
    simulatedTime: Instant,
    isCurrentYear: Boolean,
    onSpeakerClick: (String) -> Unit,
    onVideoClick: () -> Unit,
    modifier: Modifier,
    bottomPadding: Dp,
) {
    val start = runCatching { Instant.parse(session.startTimeZulu) }.getOrNull()
    val end = runCatching { Instant.parse(session.endTimeZulu) }.getOrNull()
    val live = start != null && end != null && simulatedTime >= start && simulatedTime < end
    val finished = end != null && simulatedTime >= end
    val upcoming = start != null && simulatedTime < start
    val minutesToStart = start?.let { Duration.between(simulatedTime, it).toMinutes().coerceAtLeast(0) }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(bottom = bottomPadding + 32.dp)
    ) {
        if (live) StatusBanner("GÅR NÅ", "${session.room} · ${Duration.between(simulatedTime, end).toMinutes().coerceAtLeast(0)} min igjen", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        else if (upcoming && minutesToStart != null && minutesToStart <= 60) StatusBanner("STARTER SNART", "Om $minutesToStart min · ${session.room}", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        else if (finished && session.videoUrl != null) StatusBanner("AVSLUTTET", "Opptak er tilgjengelig", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, onVideoClick)

        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                Text("JavaZone $sessionYearLabel", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text(session.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FormatBadge(session.format)
                    session.language?.let { AssistChip(onClick = {}, enabled = false, label = { Text(if (it.contains("no", true)) "🇳🇴 Norsk" else "🇬🇧 English") }) }
                }
            }
        }

        Column(Modifier.padding(20.dp)) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f))
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    InfoRow(Icons.Default.Schedule, "Tid", "${formatDay(session.startTimeZulu)}, ${formatTime(session.startTimeZulu)}–${formatTime(session.endTimeZulu)}")
                    InfoRow(Icons.Default.LocationOn, "Rom", session.room)
                    InfoRow(Icons.Default.Timer, "Varighet", "${calculateSessionDurationMinutes(session)} min")
                }
            }

            Spacer(Modifier.height(28.dp))
            Text("Om foredraget", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(10.dp))
            Text(session.abstract.ifBlank { "Ingen beskrivelse er tilgjengelig." }, style = MaterialTheme.typography.bodyLarge, lineHeight = 26.sp)

            if (session.speakers.isNotEmpty()) {
                Spacer(Modifier.height(30.dp))
                Text(if (session.speakers.size == 1) "Foredragsholder" else "Foredragsholdere", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(12.dp))
                session.speakers.forEach { speaker ->
                    SpeakerCard(speaker, onClick = { onSpeakerClick(speaker.name) })
                    Spacer(Modifier.height(10.dp))
                }
            }

            if (finished && session.videoUrl != null) {
                Spacer(Modifier.height(18.dp))
                Button(onClick = onVideoClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Icon(Icons.Default.PlayCircle, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Se videoopptak")
                }
            }
        }
    }
}

private const val sessionYearLabel = "Program"

@Composable
private fun StatusBanner(title: String, text: String, container: androidx.compose.ui.graphics.Color, content: androidx.compose.ui.graphics.Color, onClick: (() -> Unit)? = null) {
    Surface(color = container, onClick = onClick ?: {}, enabled = onClick != null, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (title == "GÅR NÅ") Icons.Default.PlayCircle else Icons.Default.Info, null, tint = content)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = content)
                Text(text, style = MaterialTheme.typography.bodyMedium, color = content)
            }
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp)) }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SpeakerCard(speaker: Speaker, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(speaker.name.firstOrNull()?.uppercase() ?: "?", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSecondaryContainer) }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(speaker.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                speaker.twitter?.let { Text("@${it.removePrefix("@")}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
                if (!speaker.bio.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(speaker.bio, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
                }
                Spacer(Modifier.height(6.dp))
                Text("Se foredrag fra denne foredragsholderen", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyDetail(onBackClick: () -> Unit, padding: PaddingValues, year: Int) {
    Column(Modifier.fillMaxSize().padding(padding).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.EventBusy, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Text("Foredraget ble ikke funnet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Programmet for JavaZone $year kan ha endret seg.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(onClick = onBackClick) { Text("Tilbake") }
    }
}
