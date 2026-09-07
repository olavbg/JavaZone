package com.olavbg.javazone.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = JavaGreen,
    onPrimary = Color(0xFF00391D),
    primaryContainer = Color(0xFF07552F),
    onPrimaryContainer = Color(0xFFB9FFD1),
    secondary = JavaBlue,
    onSecondary = Color(0xFF003547),
    secondaryContainer = Color(0xFF12485D),
    onSecondaryContainer = Color(0xFFB8E9FF),
    tertiary = JavaOrange,
    onTertiary = Color(0xFF4D190D),
    tertiaryContainer = Color(0xFF71301F),
    onTertiaryContainer = Color(0xFFFFDAD0),
    background = JavaInk,
    onBackground = JavaText,
    surface = JavaInkElevated,
    onSurface = JavaText,
    surfaceVariant = JavaInkContainer,
    onSurfaceVariant = JavaTextMuted,
    outline = Color(0xFF465B63),
    outlineVariant = Color(0xFF263942),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006B3C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF8BFFB8),
    onPrimaryContainer = Color(0xFF002110),
    secondary = Color(0xFF006782),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB8EAFF),
    onSecondaryContainer = Color(0xFF001F2A),
    tertiary = Color(0xFF9D3B20),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDBD0),
    onTertiaryContainer = Color(0xFF3B0A01),
    background = Color(0xFFF5FAF8),
    onBackground = Color(0xFF101916),
    surface = Color(0xFFF5FAF8),
    onSurface = Color(0xFF101916),
    surfaceVariant = Color(0xFFDCE7E3),
    onSurfaceVariant = Color(0xFF40504B),
    outline = Color(0xFF6F7F79),
    outlineVariant = Color(0xFFBECAC5),
)

@Composable
fun JavaZoneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // JavaZone deliberately keeps its own visual identity rather than adopting
    // arbitrary device wallpaper colors. This makes the programme semantics
    // predictable across devices and preserves the JavaZone brand language.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
