package com.olavbg.javazone.ui.theme.dark

import com.olavbg.javazone.ui.theme.DarkSurface
import com.olavbg.javazone.ui.theme.DarkSurfaceVariant
import com.olavbg.javazone.ui.theme.DiagonalBlueLight
import com.olavbg.javazone.ui.theme.DiagonalMintLight
import com.olavbg.javazone.ui.theme.JavaZoneThemeTokens

val DarkThemeTokens = JavaZoneThemeTokens(
    activeSessionCardContainer = DarkSurfaceVariant.copy(alpha = 0.74f),
    activeSessionCardBorder = DarkSurfaceVariant.copy(alpha = 0.74f),
    sessionCardBorder = DarkSurface.copy(alpha = 0.74f),
    topBarSurfaceAlpha = 0.72f,
    settingsCardSurfaceAlpha = 0.62f,
    bottomSheetSurfaceAlpha = 1f,
    diagonalTintPrimary = DiagonalMintLight,
    diagonalTintSecondary = DiagonalBlueLight,
    diagonalBandScale = 1f
)
