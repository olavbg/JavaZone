package com.olavbg.javazone.ui.timeline

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olavbg.javazone.R
import com.olavbg.javazone.model.Session
import com.olavbg.javazone.ui.components.FormatBadge
import com.olavbg.javazone.ui.components.RoomTag
import com.olavbg.javazone.ui.components.sharedElementModifier
import com.olavbg.javazone.ui.theme.FavoriteRed
import com.olavbg.javazone.util.calculateSessionDurationMinutes
import com.olavbg.javazone.util.formatTime
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailedSessionCard(
    session: Session,
    isPast: Boolean,
    isActive: Boolean,
    currentTime: Instant,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    favoriteDisplay: FavoriteDisplay = FavoriteDisplay.Toggle,
    sharedScope: SharedTransitionScope? = null
) {
    val id = session.id
    val cardAlpha = if (isPast) 0.55f else 1f
    val cardShape = RoundedCornerShape(10.dp)

    Box(
        modifier = modifier
            .graphicsLayer { this.alpha = cardAlpha }
            .background(
                color = if (isActive) MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = MaterialTheme.colorScheme.surface.alpha
                ) else MaterialTheme.colorScheme.surface,
                shape = cardShape
            )
            .border(
                width = if (isActive) 1.5.dp else 0.5.dp,
                color = if (isActive) MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = MaterialTheme.colorScheme.surface.alpha
                ) else MaterialTheme.colorScheme.surface,
                shape = cardShape
            )
            .clip(cardShape)
            .combinedClickable(
                role = Role.Button,
                onClick = onClick,
                onLongClick = { }
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            val durationMins = remember(session) { calculateSessionDurationMinutes(session) }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!session.startTimeZulu.isBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, if (isActive) MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = stringResource(R.string.duration_minutes, durationMins),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                RoomTag(room = session.room)
                Spacer(modifier = Modifier.width(8.dp))
                FormatBadge(
                    format = session.format,
                    modifier = Modifier.sharedElementModifier(sharedScope, "session-format-$id")
                )
                if (session.language != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (session.language.contains("no", ignoreCase = true)) "🇳🇴" else "🇬🇧",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                if (favoriteDisplay == FavoriteDisplay.Toggle || session.isFavorite) {
                    Spacer(modifier = Modifier.weight(1f))
                    if (favoriteDisplay == FavoriteDisplay.Toggle) {
                        IconButton(onClick = onFavoriteClick, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = if (session.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (session.isFavorite) FavoriteRed else LocalContentColor.current,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = FavoriteRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = session.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.sharedElementModifier(sharedScope, "session-title-$id")
            )

            if (session.speakers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = session.speakers.joinToString { it.name },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isActive) {
                Spacer(modifier = Modifier.height(10.dp))
                val progress = calculateSessionProgress(session, currentTime)
                val remainingMins = calculateRemainingMinutes(session, currentTime)

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                    trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {}
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${formatTime(session.start)} – ${formatTime(session.end)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = if (remainingMins > 0) stringResource(R.string.minutes_left, remainingMins) else stringResource(R.string.ending_now),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            } else if (!isPast) {
                val minsUntil = calculateMinutesUntilStart(session, currentTime)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${formatTime(session.start)} – ${formatTime(session.end)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    if (minsUntil <= 60 && !isSessionPast(session, currentTime)) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = if (minsUntil > 0) stringResource(R.string.in_minutes, minsUntil) else stringResource(R.string.in_less_than_a_minute),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun isSessionPast(session: Session, currentTime: Instant): Boolean {
    val end = session.end ?: return false
    return currentTime.isAfter(end)
}

private fun calculateSessionProgress(session: Session, currentTime: Instant): Float {
    val start = session.start ?: return 0f
    val end = session.end ?: return 0f
    val total = Duration.between(start, end).toMillis()
    val elapsed = Duration.between(start, currentTime).toMillis()
    return if (total <= 0) 0f else (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
}

private fun calculateRemainingMinutes(session: Session, currentTime: Instant): Long {
    val end = session.end ?: return 0
    val remaining = Duration.between(currentTime, end).toMinutes()
    return if (remaining < 0) 0 else remaining
}

private fun calculateMinutesUntilStart(session: Session, currentTime: Instant): Long {
    val start = session.start ?: return 0
    val minutes = Duration.between(currentTime, start).toMinutes()
    return if (minutes < 0) 0 else minutes
}