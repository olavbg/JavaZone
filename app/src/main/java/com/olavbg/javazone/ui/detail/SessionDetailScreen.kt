package com.olavbg.javazone.ui.detail

import android.content.Intent
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.olavbg.javazone.R
import com.olavbg.javazone.data.repository.SessionRepository
import com.olavbg.javazone.data.repository.SettingsRepository
import com.olavbg.javazone.ui.components.FormatBadge
import com.olavbg.javazone.ui.components.resolveVideoUrl
import com.olavbg.javazone.ui.components.sharedElementModifier
import com.olavbg.javazone.ui.theme.FavoriteRed
import com.olavbg.javazone.ui.theme.LocalJavaZoneThemeTokens
import com.olavbg.javazone.util.calculateSessionDurationMinutes
import com.olavbg.javazone.util.formatDay
import com.olavbg.javazone.util.formatFullDay
import com.olavbg.javazone.util.formatTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: String,
    repository: SessionRepository,
    settingsRepository: SettingsRepository,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    year: Int? = null,
    showLiveBanners: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(),
    sharedScope: SharedTransitionScope? = null,
) {
    val effectiveYear = year ?: SessionRepository.CURRENT_YEAR
    val scope = rememberCoroutineScope()
    val sessionFlow = remember(sessionId, effectiveYear) {
        repository.getSessionsFlow(effectiveYear).map { it.find { s -> s.id == sessionId } }
    }
    val session by sessionFlow.collectAsState(initial = null)

    LaunchedEffect(effectiveYear) {
        if (effectiveYear != SessionRepository.CURRENT_YEAR) {
            repository.loadArchiveSessions(effectiveYear)
        }
    }

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

    // Navigation bar inset applied to the scroll content instead of the Scaffold, so the
    // screen can draw all the way down behind the system bar.
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
                    title = {
                        Text(
                            text = stringResource(R.string.session_title),
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    actions = {
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
                                    tint = if (s.isFavorite) FavoriteRed else LocalContentColor.current,
                                    modifier = Modifier.sharedElementModifier(sharedScope, "session-favorite-${s.id}")
                                )
                            }
                        }
                    }
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier
    ) { padding ->
        session?.let { s ->
            val startTime = remember(s.startTimeZulu) {
                runCatching { Instant.parse(s.startTimeZulu) }.getOrNull()
            }
            val endTime = remember(s.endTimeZulu) {
                runCatching { Instant.parse(s.endTimeZulu) }.getOrNull()
            }
            
            val isFinished = startTime != null && endTime != null && endTime.isBefore(simulatedTime)
            val isLive = startTime != null && endTime != null && !isFinished && simulatedTime.isAfter(startTime)
            val minutesUntilStart = if (startTime != null) Duration.between(simulatedTime, startTime).toMinutes() else 0
            val startsSoon = startTime != null && endTime != null && !isFinished && (minutesUntilStart in 0..60)

            // Keep the "starts soon / live / finished" banners fresh by ticking the simulated
            // clock once per whole minute, the same way the timeline does. Not needed once the
            // talk has ended, and archive years only show finished talks anyway.
            LaunchedEffect(s.id, effectiveYear, offset, endTime) {
                if (effectiveYear == SessionRepository.CURRENT_YEAR && endTime != null) {
                    while (endTime.isAfter(simulatedTime)) {
                        simulatedTime = Instant.now().plusMillis(offset)
                        val millisUntilNextMinute = 60_000L - (System.currentTimeMillis() % 60_000L)
                        delay(millisUntilNextMinute)
                    }
                }
            }

            Box(
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
                                        text = stringResource(R.string.session_finished),
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
                                                    Icons.Default.VideoLibrary,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(24.dp),
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = stringResource(R.string.watch_recording),
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
                                    text = stringResource(R.string.live_banner, s.room, minutesRemaining),
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
                                    text = stringResource(R.string.starts_banner, minutesUntilStart, s.room),
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
                            shape = RoundedCornerShape(10.dp),
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
                                    Icons.Default.VideoLibrary,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.watch_recording),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    if (effectiveYear != SessionRepository.CURRENT_YEAR) {
                                        Text(
                                            text = stringResource(R.string.recording_from_year, effectiveYear),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
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
                                lineHeight = 32.sp,
                                modifier = Modifier.sharedElementModifier(sharedScope, "session-title-${s.id}")
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (sharedScope != null) {
                                    FormatBadge(
                                        format = s.format,
                                        modifier = Modifier.sharedElementModifier(sharedScope, "session-format-${s.id}")
                                    )
                                } else {
                                    FormatBadge(format = s.format)
                                }
                                if (s.language != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val isNorwegian = s.language.contains("no", ignoreCase = true)
                                            Text(
                                                text = if (isNorwegian) "🇳🇴" else "🇬🇧",
                                                style = MaterialTheme.typography.labelMedium,
                                                modifier = Modifier.padding(start = 10.dp)
                                            )
                                            Text(
                                                text = if (isNorwegian) stringResource(R.string.language_norwegian) else stringResource(R.string.language_english),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(start = 4.dp, end = 10.dp, top = 6.dp, bottom = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(20.dp)) {
                        // Time and Room Card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
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
                                    val timeLabel = if (s.startTimeZulu.isBlank()) {
                                        stringResource(R.string.missing_time)
                                    } else if (effectiveYear == SessionRepository.CURRENT_YEAR) {
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
                                if (!s.startTimeZulu.isBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.duration_minutes, duration),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = stringResource(R.string.about_talk),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = s.abstract,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 26.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.95f)
                        )

                        if (!s.intendedAudience.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = stringResource(R.string.intended_audience),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = s.intendedAudience,
                                style = MaterialTheme.typography.bodyLarge,
                                lineHeight = 26.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.95f)
                            )
                        }

                        if (!s.suggestedKeywords.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = stringResource(R.string.keywords),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                s.suggestedKeywords
                                    .split(Regex("[;,]"))
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }
                                    .forEach { keyword ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                        ) {
                                            Text(
                                                text = keyword,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                            }
                        }

                        if (s.speakers.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(
                                text = pluralStringResource(R.plurals.speakers_count, s.speakers.size, s.speakers.size),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            s.speakers.forEach { speaker ->
                                SpeakerItem(speaker = speaker)
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(48.dp + navBarBottom + contentPadding.calculateBottomPadding()))
                    }
                }
            }
        } ?: Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}