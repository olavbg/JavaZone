package com.olavbg.javazone.ui.detail

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.olavbg.javazone.R
import com.olavbg.javazone.data.repository.SessionRepository
import com.olavbg.javazone.data.repository.SettingsRepository
import com.olavbg.javazone.model.Speaker
import com.olavbg.javazone.ui.components.FormatBadge
import com.olavbg.javazone.ui.components.buildSpeakerSocialLinks
import com.olavbg.javazone.ui.components.resolveSpeakerImageUrl
import com.olavbg.javazone.ui.components.resolveVideoUrl
import com.olavbg.javazone.ui.components.sharedElementModifier
import com.olavbg.javazone.ui.theme.FavoriteRed
import com.olavbg.javazone.util.calculateSessionDurationMinutes
import com.olavbg.javazone.util.formatDay
import com.olavbg.javazone.util.formatFullDay
import com.olavbg.javazone.util.formatTime
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
    repository: SessionRepository,
    settingsRepository: SettingsRepository,
    onBackClick: () -> Unit,
    onSpeakerClick: (String) -> Unit,
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
    // Navigation bar inset applied to the scroll content instead of the Scaffold, so the
    // screen can draw all the way down behind the system bar.
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                ),
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
                                    tint = if (s.isFavorite) FavoriteRed else LocalContentColor.current,
                                    modifier = Modifier.sharedElementModifier(sharedScope, "session-favorite-${s.id}")
                                )
                            }
                        }
                    }
                }
            )
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
                                                text = if (isNorwegian) "Norsk" else "Engelsk",
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
                                        "Mangler tidspunkt"
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
                                            text = "$duration min",
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
                            text = "Om foredraget",
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
                                text = "Passer for",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = s.intendedAudience,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!s.suggestedKeywords.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Emneknagger",
                                style = MaterialTheme.typography.titleMedium,
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
                        
                        Spacer(modifier = Modifier.height(48.dp + navBarBottom + contentPadding.calculateBottomPadding()))
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
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            val socials = remember(speaker) { buildSpeakerSocialLinks(speaker) }
            Row(verticalAlignment = Alignment.Top) {
                // Photo, falling back to the initial-letter avatar while loading/absent.
                Box(modifier = Modifier.size(56.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        modifier = Modifier.matchParentSize()
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
                    if (!speaker.pictureUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = resolveSpeakerImageUrl(speaker.pictureUrl),
                            contentDescription = speaker.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    // The name row is pinned to the avatar height, so name and icons are
                    // always vertically centred on the avatar. Social links sit on the
                    // same line, right-aligned, which reads tidier than wrapping below.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.heightIn(min = 56.dp)
                    ) {
                        Text(
                            text = speaker.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        if (socials.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                socials.forEach { link ->
                                    SpeakerSocialButton(
                                        iconRes = socialIconRes(link.kind),
                                        contentDescription = socialContentDescription(link.kind),
                                        url = link.url,
                                        context = context
                                    )
                                }
                            }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SpeakerSocialButton(
    @DrawableRes iconRes: Int,
    contentDescription: String?,
    url: String,
    context: Context
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), CircleShape)
            .combinedClickable(
                onClick = {
                    openUrl(context, url)
                },
                onLongClick = {
                    // Long-pressing the brand icon shows the exact URL before it is
                    // opened, since opening a link can be hard to undo.
                    Toast.makeText(context, url, Toast.LENGTH_LONG).show()
                }
            )
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(19.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
    }
}

@DrawableRes
private fun socialIconRes(kind: String): Int = when (kind) {
    "bluesky" -> R.drawable.ic_bluesky_logo
    "linkedin" -> R.drawable.ic_linkedin_logo
    else -> R.drawable.ic_x_logo
}

private fun socialContentDescription(kind: String): String = when (kind) {
    "bluesky" -> "Bluesky-profil"
    "linkedin" -> "LinkedIn-profil"
    else -> "X-profil"
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}
