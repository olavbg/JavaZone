package com.olavbg.javazone.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath

/**
 * Subtle JavaZone background: two calm tones separated by a slowly moving diagonal.
 * Kept deliberately low contrast so foreground content remains the visual priority.
 */
@Composable
fun AnimatedDiagonalBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "diagonal-background")
    val sway by transition.animateFloat(
        initialValue = -0.08f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "diagonal-sway",
    )

    val firstTone = MaterialTheme.colorScheme.surfaceContainerLowest
    val secondTone = MaterialTheme.colorScheme.surfaceContainerLow

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diagonalX = size.width * (0.52f + sway)

            val firstPath = Path().apply {
                moveTo(0f, 0f)
                lineTo(diagonalX, 0f)
                lineTo(0f, size.height)
                close()
            }
            clipPath(firstPath) {
                drawRect(color = firstTone)
            }

            val secondPath = Path().apply {
                moveTo(diagonalX, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            clipPath(secondPath) {
                drawRect(color = secondTone)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}
