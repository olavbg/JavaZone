package com.olavbg.javazone.ui.theme

import androidx.compose.ui.graphics.Color

// JavaZone design tokens. Keep semantic roles here so the UI does not invent
// one-off colors in individual screens.
val JavaGreen = Color(0xFF63F5A5)
val JavaGreenStrong = Color(0xFF00D978)
val JavaInk = Color(0xFF081018)
val JavaInkElevated = Color(0xFF101C26)
val JavaInkContainer = Color(0xFF172733)
val JavaText = Color(0xFFF2F7F5)
val JavaTextMuted = Color(0xFF9EAFAB)

val JavaBlue = Color(0xFF5CC8FF)
val JavaOrange = Color(0xFFFF8A65)
val WorkshopPurple = Color(0xFFC18CFF)
val LightningAmber = Color(0xFFFFC857)
val PresentationBlue = Color(0xFF75BFFF)

// Legacy names retained for components that have not yet migrated to semantic tokens.
val FreshGreen = JavaGreenStrong
val DeepBlue = Color(0xFF123B35)
val DarkBackground = JavaInk
val DarkSurface = JavaInkElevated
val DarkSurfaceVariant = JavaInkContainer
val DarkOnSurface = JavaText

val RoomAccentColors = listOf(
    Color(0xFF5CC8FF),
    Color(0xFF63F5A5),
    Color(0xFFFF9B71),
    Color(0xFFD29BFF),
    Color(0xFFFF7777),
    Color(0xFF72E6A3),
    Color(0xFFFFD166),
    Color(0xFF7DB9FF)
)
