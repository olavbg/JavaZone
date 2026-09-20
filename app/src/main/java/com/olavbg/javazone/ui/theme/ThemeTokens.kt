package com.olavbg.javazone.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.olavbg.javazone.ui.theme.light.LightThemeTokens

@Immutable
data class JavaZoneThemeTokens(
    val activeSessionCardContainer: Color,
    val activeSessionCardBorder: Color,
    val sessionCardBorder: Color,
    val topBarSurfaceAlpha: Float,
    val settingsCardSurfaceAlpha: Float,
    val diagonalTintPrimary: Color,
    val diagonalTintSecondary: Color,
    val diagonalBandScale: Float
)

val LocalJavaZoneThemeTokens = staticCompositionLocalOf { LightThemeTokens }