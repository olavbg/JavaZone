package com.olavbg.javazone.ui.timeline

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyLayoutScrollScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import com.olavbg.javazone.model.Session
import com.olavbg.javazone.util.isSessionActive
import java.time.Instant
import kotlin.math.abs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

internal fun findFirstActiveOrUpcomingIndex(
    groupedSessions: List<AgendaGroup>,
    currentTime: Instant
): Int? {
    var index = 0
    var bestIndex: Int? = null

    for (group in groupedSessions) {
        val anyActive = group.sessions.any { isSessionActive(it, currentTime) }
        val isFuture = group.sessions.firstOrNull()?.start?.let { it.isAfter(currentTime) || it == currentTime } ?: false

        if (anyActive) {
            return index
        }
        if (isFuture && bestIndex == null) {
            bestIndex = index
        }

        index += 1 + group.sessions.size // header + sessions count
    }

    return bestIndex
}

internal fun isGroupedSessionsForDay(groupedSessions: List<AgendaGroup>, selectedDay: String?): Boolean {
    if (groupedSessions.isEmpty()) return false
    if (selectedDay == null) return true
    val firstSession = groupedSessions.firstOrNull()?.sessions?.firstOrNull() ?: return false
    return getDayFromZulu(firstSession.start).equals(selectedDay, ignoreCase = true)
}

// Wait until the list has laid-out content, giving up after a timeout so callers never stall.
internal suspend fun LazyListState.awaitContent(): Boolean =
    withTimeoutOrNull(SCROLL_READY_TIMEOUT_MS) {
        snapshotFlow { layoutInfo.visibleItemsInfo.isNotEmpty() }.first { it }
    } == true

// Drive the scroll with a single, continuous eased glide. Starts with an ease-in acceleration
// from a standstill, cruises smoothly at high speed, and decelerates into the target with exact
// pixel landing - no intermediate stops, no hitches, no overshoot.
@OptIn(ExperimentalFoundationApi::class)
internal suspend fun LazyListState.smoothScrollToItemEased(targetIndex: Int): Boolean {
    if (!awaitContent()) return false

    val totalItems = layoutInfo.totalItemsCount
    if (totalItems <= 0) return false
    val target = targetIndex.coerceIn(0, totalItems - 1)

    var finishedSuccessfully = false
    scroll {
        val itemScope = LazyLayoutScrollScope(this@smoothScrollToItemEased, this)
        if (itemScope.itemCount <= 0) return@scroll

        // A sticky header is the landing target, but while it is "stuck" pinned at the top the
        // list measures its distance as ~0 even though the first card is still sliding under it.
        // Anchor the landing to the first card below the header, sitting exactly one header height
        // below the top, so the trip always ends with the header at the very top and the first
        // card tucked right underneath - no overlap, no extra offset.
        fun remainingToLanding(): Float {
            val headerInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == target }
            return if (headerInfo != null && target + 1 < itemScope.itemCount) {
                (itemScope.calculateDistanceTo(target + 1) - headerInfo.size).toFloat()
            } else {
                itemScope.calculateDistanceTo(target).toFloat()
            }
        }

        val initialDistance = remainingToLanding()
        if (initialDistance == 0f) {
            finishedSuccessfully = true
            return@scroll
        }

        val rowsToMove = abs(target - firstVisibleItemIndex)
        // Per-frame pixel step cap: keeps the number of rows composed per frame low enough to
        // avoid frame drops. Higher cap = faster long trips (more rows stream past per frame).
        val maxStepPx = (layoutInfo.viewportSize.height * 0.45f).coerceIn(720f, 1500f)

        // For a quick trip the eased duration (~160-260ms) is used as-is. For long trips the
        // duration is stretched to the minimum that fits the eased curve inside the step cap,
        // so the scroll cruises at full allowed speed instead of being clamped (which would
        // add a drawn-out catch-up tail).
        val easedMs = (150 + rowsToMove * 3).coerceIn(160, 260)
        val capAwareMs = (1.35f * abs(initialDistance) / maxStepPx * 16.7f).toInt()
        val durationMs = maxOf(easedMs, capAwareMs).coerceIn(160, 420)

        var previousProgress = 0f
        var lastFrameNanos = 0L
        var accumulatedMillis = 0f

        while (true) {
            val frameNanos = withFrameNanos { it }
            if (lastFrameNanos == 0L) {
                lastFrameNanos = frameNanos
                continue
            }

            // Clamp frame delta time to at most 20ms to prevent frame drop cascades
            val deltaNanos = frameNanos - lastFrameNanos
            lastFrameNanos = frameNanos
            val deltaMillis = (deltaNanos / 1_000_000f).coerceIn(1f, 20f)
            accumulatedMillis += deltaMillis

            val remainingDistance = remainingToLanding()
            if (abs(remainingDistance) < 1f) {
                if (remainingDistance != 0f) {
                    itemScope.scrollBy(remainingDistance)
                }
                break
            }

            val rawFraction = (accumulatedMillis / durationMs).coerceIn(0f, 1f)
            val currentProgress = ScrollToNowEasing.transform(rawFraction)

            // While the eased ramp is running the step is a fixed share of the travel left, so
            // the easing curve is honoured all the way (including its deceleration into the
            // target - no drawn-out crawl at the end). Only when the ramp has already finished
            // but the position is still catching up (frame-drop lag) do we settle the leftover
            // quickly instead of stalling.
            val stepFraction = if (rawFraction >= 1f) {
                LANDING_CATCHUP_PER_FRAME
            } else {
                val progressRemaining = (1f - previousProgress).coerceAtLeast(0.0001f)
                ((currentProgress - previousProgress) / progressRemaining).coerceIn(0f, 1f)
            }

            var stepPx = remainingDistance * stepFraction

            // Clamp stepPx to maxStepPx so Compose never has to compose multiple cards in one frame
            stepPx = stepPx.coerceIn(-maxStepPx, maxStepPx)

            if (abs(stepPx) > 0.001f) {
                val consumed = itemScope.scrollBy(stepPx)
                if (abs(consumed) < 0.001f && abs(stepPx) > 1f) {
                    break
                }
            }

            previousProgress = currentProgress
        }

        finishedSuccessfully = true
    }

    return finishedSuccessfully
}

// Ease-in curve with a visible acceleration ramp and a mild deceleration into the landing zone.
private val ScrollToNowEasing = CubicBezierEasing(0.25f, 0.0f, 0.25f, 1.0f)

// If the eased ramp finishes while the position is still catching up (e.g. a frame-dropping
// spell during cold start), the leftover is settled fast - a few frames, never a drawn-out tail.
private const val LANDING_CATCHUP_PER_FRAME = 0.35f

// How long to wait for the list to have laid-out content before giving up on the auto-scroll.
private const val SCROLL_READY_TIMEOUT_MS = 5_000L

// Rows the viewport must move away from now before the "Scroll to now" pill appears.
internal const val NOW_FAB_SLOP_ITEMS = 6

internal enum class NowFabState { Hidden, ShowUp, ShowDown }