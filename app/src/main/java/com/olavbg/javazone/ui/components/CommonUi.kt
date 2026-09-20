package com.olavbg.javazone.ui.components

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.olavbg.javazone.R
import com.olavbg.javazone.model.Speaker
import com.olavbg.javazone.ui.theme.LightningAmber
import com.olavbg.javazone.ui.theme.PresentationBlue
import com.olavbg.javazone.ui.theme.RoomAccentColors
import com.olavbg.javazone.ui.theme.WorkshopPurple
import kotlin.math.abs

private val ROOM_NUMBER_REGEX = Regex("[^0-9]")

@Composable
fun FormatBadge(format: String, modifier: Modifier = Modifier) {
    val (color, labelRes) = remember(format) {
        when {
            format.contains("lightning", ignoreCase = true) -> LightningAmber to R.string.format_lightning_talk
            format.contains("workshop", ignoreCase = true) -> WorkshopPurple to R.string.format_workshop
            else -> PresentationBlue to R.string.format_talk
        }
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f),
        modifier = modifier
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Stable accent color for a room, picked from the shared room palette. The same color is
 * used by [RoomTag] on cards and in the detail screen, so the room picker can link up
 * visually with the places the room name otherwise appears.
 */
fun roomAccentColor(room: String, fallback: Color): Color {
    val index = room.lowercase().replace(ROOM_NUMBER_REGEX, "").toIntOrNull() ?: room.hashCode()
    return RoomAccentColors.getOrElse(abs(index) % RoomAccentColors.size) { fallback }
}

@Composable
fun RoomTag(room: String, modifier: Modifier = Modifier) {
    val fallbackColor = MaterialTheme.colorScheme.primary
    val color = remember(room, fallbackColor) { roomAccentColor(room, fallbackColor) }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Text(
            text = room,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

fun resolveVideoUrl(url: String): String {
    return when {
        url.startsWith("http") -> url
        url.startsWith("//") -> "https:$url"
        url.startsWith("/") -> "https://javazone.no$url"
        url.contains(".") -> "https://$url"
        else -> "https://vimeo.com/$url"
    }
}

fun resolveSpeakerImageUrl(url: String): String {
    return when {
        url.startsWith("http://") || url.startsWith("https://") -> url
        url.startsWith("//") -> "https:$url"
        url.startsWith("/") -> "https://javazone.no$url"
        url.contains(".") -> "https://$url"
        else -> url
    }
}

data class SpeakerSocialLink(
    val kind: String,
    val label: String,
    val url: String
)

/**
 * Builds the list of social media links shown for a speaker. Empty/placeholder
 * handles (e.g. a lone "@", "=") are dropped, and Twitter handles are presented
 * as X, using [buildSpeakerSocialLinks]'s sibling resolvers below.
 */
fun buildSpeakerSocialLinks(speaker: Speaker): List<SpeakerSocialLink> = buildList {
    speaker.twitter?.let { value ->
        val url = resolveXUrl(value)
        if (url != null) add(SpeakerSocialLink(kind = "twitter", label = displayHandle(value), url = url))
    }
    speaker.bluesky?.let { value ->
        val url = resolveBlueskyUrl(value)
        if (url != null) add(SpeakerSocialLink(kind = "bluesky", label = displayHandle(value), url = url))
    }
    speaker.linkedin?.let { value ->
        val url = resolveLinkedInUrl(value)
        if (url != null) add(SpeakerSocialLink(kind = "linkedin", label = displayHandle(value), url = url))
    }
}

private fun isHttpUrl(value: String): Boolean =
    value.startsWith("http://") || value.startsWith("https://")

private fun displayHandle(value: String): String {
    val trimmed = value.trim().substringBefore('?')
    return if (isHttpUrl(trimmed)) {
        trimmed.trimEnd('/').substringAfterLast('/').trim()
    } else {
        trimmed.removePrefix("@").trim().trimEnd('/')
    }
}

private fun isValidHandle(handle: String): Boolean =
    handle.length >= 2 && handle.any { it.isLetterOrDigit() }

private fun resolveXUrl(value: String): String? {
    val trimmed = value.trim()
    if (isHttpUrl(trimmed)) return trimmed
    val handle = displayHandle(trimmed)
    return if (isValidHandle(handle)) "https://x.com/$handle" else null
}

private fun resolveBlueskyUrl(value: String): String? {
    val trimmed = value.trim()
    if (isHttpUrl(trimmed)) return trimmed
    val handle = displayHandle(trimmed)
    if (!isValidHandle(handle)) return null
    // Bluesky handles must contain at least one ".": either a custom domain such as
    // "brianvermeer.nl", or end in ".bsky.social". A bare handle like "gsaab" would
    // 404, so give it the *.bsky.social suffix.
    val full = if (handle.contains('.') || handle.startsWith("did:")) handle else "$handle.bsky.social"
    return "https://bsky.app/profile/$full"
}

private fun resolveLinkedInUrl(value: String): String? {
    val trimmed = value.trim()
    if (isHttpUrl(trimmed)) return trimmed
    val handle = displayHandle(trimmed)
    return if (isValidHandle(handle)) "https://www.linkedin.com/in/$handle" else null
}

/**
 * Returns a modifier that registers an element as part of a hero (shared element) transition
 * when a [SharedTransitionScope] is available (i.e. during navigation between screens).
 * Returns [Modifier] untouched when [sharedScope] is null.
 *
 * @param boundsKey must match the key used for the matching element on the other screen.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedElementModifier(sharedScope: SharedTransitionScope?, boundsKey: String): Modifier {
    val scope = sharedScope ?: return this
    return with(scope) {
        Modifier.sharedElement(
            sharedContentState = rememberSharedContentState(key = boundsKey),
            animatedVisibilityScope = LocalNavAnimatedContentScope.current
        )
    }
}
