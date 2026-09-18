package com.olavbg.javazone.ui.timeline

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.olavbg.javazone.model.Session
import com.olavbg.javazone.util.isSessionActive
import java.time.Instant

enum class FavoriteDisplay { Toggle, FavoriteOnly }

@Composable
fun AgendaListView(
    groupedSessions: List<AgendaGroup>,
    currentTime: Instant,
    listState: LazyListState,
    onSessionClick: (Session) -> Unit,
    onFavoriteClick: (Session) -> Unit,
    favoriteDisplay: FavoriteDisplay = FavoriteDisplay.Toggle,
    liveIndicators: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(bottom = 32.dp),
    sharedScope: SharedTransitionScope? = null,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Vertical guide line.
        Box(
            modifier = Modifier
                .padding(start = 36.dp)
                .width(2.dp)
                .fillMaxHeight()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    )
                )
        )

        LazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize()
        ) {
            var nextStickyIndex = 0
            groupedSessions.forEach { group ->
                val stickyIndex = nextStickyIndex
                nextStickyIndex += 1 + group.sessions.size
                val isLiveSlot = group.sessions.any { isSessionActive(it, currentTime) }

                // Sticky time header. The pin state reads the scroll position inside the header
                // itself (deferred through derivedStateOf), so the outer content builder is not
                // re-executed on every scroll frame just to compute it.
                stickyHeader(key = "agenda-sticky-${group.key}") {
                    TimelineStickyTimeHeader(
                        timeSlot = group.headerLabel,
                        isLiveSlot = isLiveSlot,
                        sessionCount = group.sessions.size,
                        pinnedRange = (stickyIndex + 1) until nextStickyIndex,
                        listState = listState
                    )
                }

                itemsIndexed(
                    items = group.sessions,
                    key = { _, session -> "agenda-item-${session.id}" },
                    contentType = { _, _ -> "session-row" }
                ) { _, session ->
                    TimelineSessionRow(
                        session = session,
                        isPast = liveIndicators && isSessionPast(session, currentTime),
                        isActive = liveIndicators && isSessionActive(session, currentTime),
                        currentTime = currentTime,
                        onFavoriteClick = remember(session) { { onFavoriteClick(session) } },
                        onClick = remember(session) { { onSessionClick(session) } },
                        favoriteDisplay = favoriteDisplay,
                        sharedScope = sharedScope
                    )
                }
            }
        }
    }
}

@Composable
fun TimelineStickyTimeHeader(
    timeSlot: String,
    isLiveSlot: Boolean,
    sessionCount: Int,
    pinnedRange: IntRange,
    listState: LazyListState
) {
    // Pinned while the first visible row is one of this group's sessions (the header then sticks
    // above the scrolling rows). Reading the scroll position via derivedStateOf keeps the churn
    // contained to this single header instead of recomposing the entire list content per frame.
    val isPinned by remember(pinnedRange) {
        derivedStateOf { listState.firstVisibleItemIndex in pinnedRange }
    }

    // The header background is nearly invisible in its natural list position, but
    // fades to a readable translucent tint while pinned over scrolling rows. It uses
    // the screen background colour (not the raised "surface" tone) so it never reads
    // as an offset panel below the toolbar.
    val bgAlpha by animateFloatAsState(
        targetValue = if (isPinned) 0.72f else 0.10f,
        animationSpec = tween(durationMillis = 250),
        label = "stickyHeaderBg"
    )
    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = bgAlpha),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp, start = 16.dp, end = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isLiveSlot) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(
                        1.dp,
                        if (isLiveSlot) MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    ),
                    shadowElevation = if (isLiveSlot) 2.dp else 0.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = timeSlot,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = if (isLiveSlot) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (isLiveSlot) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                HorizontalDivider(
                    color = if (isLiveSlot) MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "$sessionCount ${if (sessionCount == 1) "foredrag" else "parallell-løp"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun TimelineSessionRow(
    session: Session,
    isPast: Boolean,
    isActive: Boolean,
    currentTime: Instant,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
    favoriteDisplay: FavoriteDisplay = FavoriteDisplay.Toggle,
    sharedScope: SharedTransitionScope? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.width(52.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.width(36.dp))
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .weight(1f)
                        .background(
                            if (isActive) MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                )
            }

            Box(
                modifier = Modifier
                    .size(if (isActive) 12.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isActive -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                            isPast -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.background,
                        shape = CircleShape
                    )
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            DetailedSessionCard(
                session = session,
                isPast = isPast,
                isActive = isActive,
                currentTime = currentTime,
                onFavoriteClick = onFavoriteClick,
                onClick = onClick,
                favoriteDisplay = favoriteDisplay,
                sharedScope = sharedScope,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}