package com.olavbg.javazone.ui.theme.light

import com.olavbg.javazone.ui.theme.ActiveSessionCardLight
import com.olavbg.javazone.ui.theme.JavaZoneThemeTokens
import com.olavbg.javazone.ui.theme.LightModeDiagonalBlue
import com.olavbg.javazone.ui.theme.LightModeDiagonalMint
import com.olavbg.javazone.ui.theme.LightOutlineVariant
import com.olavbg.javazone.ui.theme.LightPrimary

val LightThemeTokens = JavaZoneThemeTokens(
    activeSessionCardContainer = ActiveSessionCardLight.copy(alpha = 0.60f),
    activeSessionCardBorder = LightPrimary.copy(alpha = 0.55f),
    sessionCardBorder = LightOutlineVariant.copy(alpha = 0.9f),
    topBarSurfaceAlpha = 0.60f,
    diagonalTintPrimary = LightModeDiagonalMint,
    diagonalTintSecondary = LightModeDiagonalBlue,
    diagonalBandScale = 1.25f
)