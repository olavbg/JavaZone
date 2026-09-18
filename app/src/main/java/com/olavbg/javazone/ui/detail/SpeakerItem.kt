package com.olavbg.javazone.ui.detail

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import coil.compose.AsyncImage
import com.olavbg.javazone.R
import com.olavbg.javazone.model.Speaker
import com.olavbg.javazone.ui.components.buildSpeakerSocialLinks
import com.olavbg.javazone.ui.components.resolveSpeakerImageUrl

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