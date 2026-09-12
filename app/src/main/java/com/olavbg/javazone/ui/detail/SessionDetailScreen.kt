package com.olavbg.javazone.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import android.content.Intent
import com.olavbg.javazone.data.repository.SessionRepository
import com.olavbg.javazone.data.repository.SettingsRepository
import com.olavbg.javazone.model.Speaker
import com.olavbg.javazone.ui.components.FormatBadge
import com.olavbg.javazone.ui.components.resolveVideoUrl
import com.olavbg.javazone.ui.theme.*
import com.olavbg.javazone.util.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: String,
    year: Int? = null,
    repository: SessionRepository,
    settingsRepository: SettingsRepository,
    onBackClick: () -> Unit,
    onSpeakerClick: (String) -> Unit,
    showLiveBanners: Boolean = true,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val effectiveYear = year ?: SessionRepository.CURRENT_YEAR
    val scope = rememberCoroutineScope()
    val sessionFlow = remember(sessionId, effectiveYear) {
        repository.getSessionsFlow(effectiveYear).map { it.find { s -> s.id == sessionId } }
    }
    val session by sessionFlow.collectAsState(initial = null)

    val offset by settingsRepository.simulatedTimeOffset.collectAsState(initial = 0L)
    var simulatedTime by remember { mutableStateOf(Instant.now().plusMillis(offset)) }

    // Update time when offset changes or when screen resumes
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, offset) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                simulatedTime = Instant.now().plusMillis(offset)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(offset) {
        simulatedTime = Instant.now().plusMillis(offset)
    }

    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Foredrag",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tilbake")
                    }
                },
                actions = {
                    if (effectiveYear == SessionRepository.CURRENT_YEAR) {
                        session?.let { s ->
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        repository.toggleFavorite(s.id, !s.isFavorite)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (s.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (s.isFavorite) MaterialTheme.colorScheme.tertiary else LocalContentColor.current
                                )
                            }
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        session?.let { s ->
            val startTime = remember(s.startTimeZulu) { Instant.parse(s.startTimeZulu) }
            val endTime = remember(s.endTimeZulu) { Instant.parse(s.endTimeZulu) }
            
            val isFinished = endTime.isBefore(simulatedTime)
            val isLive = !isFinished && simulatedTime.isAfter(startTime)
            val minutesUntilStart = Duration.between(simulatedTime, startTime).toMinutes()
            val startsSoon = !isFinished && (minutesUntilStart in 0..60)

            PullToRefreshBox(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        if (effectiveYear == SessionRepository.CURRENT_YEAR) {
                            repository.refreshSessions()
                        } else {
                            repository.loadArchiveSessions(effectiveYear)
                        }
                        isRefreshing = false
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    if (showLiveBanners && isFinished) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Dette foredraget er allerede avsluttet",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    
                                    if (s.videoUrl != null) {
                                        val context = LocalContext.current
                                        Surface(
                                            onClick = {
                                                try {
                                                    val videoUrl = resolveVideoUrl(s.videoUrl)
                                                    val intent = Intent(Intent.ACTION_VIEW, videoUrl.toUri()).apply {
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {}
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                                            modifier = Modifier.padding(top = 8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.PlayCircle,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Se videoopptak",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (showLiveBanners && isLive) {
                        val minutesRemaining = Duration.between(simulatedTime, endTime).toMinutes().coerceAtLeast(0)
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "Går nå i ${s.room} – $minutesRemaining min igjen",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    } else if (showLiveBanners && startsSoon) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "Starter om $minutesUntilStart minutter i ${s.room}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    } else if (!s.videoUrl.isNullOrEmpty()) {
                        val context = LocalContext.current
                        Surface(
                            onClick = {
                                try {
                                    val videoUrl = resolveVideoUrl(s.videoUrl)
                                    val intent = Intent(Intent.ACTION_VIEW, videoUrl.toUri()).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp, bottom = 12.dp, start = 20.dp, end = 20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Se videoopptak",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    if (effectiveYear != SessionRepository.CURRENT_YEAR) {
                                        Text(
                                            text = "Opptak fra JavaZone $effectiveYear",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    // Header Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(vertical = 24.dp, horizontal = 20.dp)
                    ) {
                        Column {
                            Text(
                                text = s.title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                lineHeight = 32.sp
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FormatBadge(format = s.format)
                                if (s.language != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = if (s.language.contains("no", ignoreCase = true)) "🇳🇴 Norsk" else "🇬🇧 Engelsk",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(20.dp)) {
                        // Time and Room Card
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val timeLabel = if (effectiveYear == SessionRepository.CURRENT_YEAR) {
                                        "${formatDay(s.startTimeZulu)}, ${formatTime(s.startTimeZulu)} – ${formatTime(s.endTimeZulu)}"
                                    } else {
                                        "${formatFullDay(s.startTimeZulu) ?: formatDay(s.startTimeZulu)}, ${formatTime(s.startTimeZulu)} – ${formatTime(s.endTimeZulu)}"
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = timeLabel,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = s.room,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                
                                val duration = calculateSessionDurationMinutes(s)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                ) {
                                    Text(
                                        text = "$duration min",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Om foredraget",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = s.abstract,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 26.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                        )

                        if (s.speakers.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(
                                text = if (s.speakers.size > 1) "Foredragsholdere" else "Foredragsholder",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            s.speakers.forEach { speaker ->
                                SpeakerItem(
                                    speaker = speaker,
                                    onClick = { onSpeakerClick(speaker.name) }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(48.dp + contentPadding.calculateBottomPadding()))
                    }
                }
            }
        } ?: Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun SpeakerItem(
    speaker: Speaker,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Placeholder for Avatar
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = speaker.name.firstOrNull()?.toString() ?: "?",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = speaker.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (speaker.twitter != null) {
                        TextButton(
                            onClick = {
                                try {
                                    val twitterUrl = "https://twitter.com/${speaker.twitter.removePrefix("@")}"
                                    val intent = Intent(Intent.ACTION_VIEW, twitterUrl.toUri()).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = "@${speaker.twitter.removePrefix("@")}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
            
            if (!speaker.bio.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = speaker.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Se alle foredrag fra denne foreleseren",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}



