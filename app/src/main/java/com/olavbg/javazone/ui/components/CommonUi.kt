package com.olavbg.javazone.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olavbg.javazone.ui.theme.*
import kotlin.math.abs

@Composable
fun FormatBadge(format: String, modifier: Modifier = Modifier) {
    val (color, label) = when {
        format.contains("lightning", ignoreCase = true) -> LightningAmber to "Lynforedrag"
        format.contains("workshop", ignoreCase = true) -> WorkshopPurple to "Workshop"
        else -> PresentationBlue to "Foredrag"
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun RoomTag(room: String, modifier: Modifier = Modifier) {
    val index = room.lowercase().replace(Regex("[^0-9]"), "").toIntOrNull() ?: room.hashCode()
    val color = RoomAccentColors.getOrElse(abs(index) % RoomAccentColors.size) { MaterialTheme.colorScheme.primary }

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
