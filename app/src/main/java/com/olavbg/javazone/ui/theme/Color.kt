package com.olavbg.javazone.ui.theme

import androidx.compose.ui.graphics.Color

// ---- Calm dark neutrals (graphite, not black) ----
val DarkBackground = Color(0xFF212529)
val DarkSurface = Color(0xFF2C3138)
val DarkSurfaceVariant = Color(0xFF39404A)
val DarkOutline = Color(0xFF4A525C)
val DarkOnSurface = Color(0xFFF2F4F7)

// ---- Light neutrals ----
val LightBackground = Color(0xFFEFF1F3)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE7EAEC)
val LightOnSurface = Color(0xFF1B1E21)
val LightPrimary = Color(0xFF3C7A63)
val LightOutlineVariant = Color(0xFFDDE1E5)

// ---- Active (live) session card ----
// A soft primary tint used only in light mode so a live talk clearly stands out from the
// white cards around it. Dark mode keeps its surfaceVariant-based highlight.
val ActiveSessionCardLight = Color(0xFFD8EBE2)

// ---- Muted pastel accents ----
val AccentMint = Color(0xFF7BC4A6)
val AccentBlue = Color(0xFF7B9EC7)
val AccentPurple = Color(0xFF9B8AC4)
val AccentAmber = Color(0xFFCBA963)
val AccentRose = Color(0xFFC08093)
val AccentTeal = Color(0xFF67A8A5)
val AccentIndigo = Color(0xFF8C94C8)
val AccentCoral = Color(0xFFBF8A72)

// High-lightness pastels that keep their hue on dark surfaces
val AccentMintLight = Color(0xFFA5E8CD)
val AccentBlueLight = Color(0xFFABC9EF)
val AccentRoseLight = Color(0xFFE2AFBB)

// Soft pastel tints for the animated diagonal background
val DiagonalMintLight = Color(0xFF8FDFBA)
val DiagonalBlueLight = Color(0xFF9FC2F0)

// Light-theme diagonal tints. Deeper mid-tones than the accent palette so the animated
// bands stay visible through the translucent white surfaces in light mode (the bright
// pastels above wash out against a light backdrop).
val LightModeDiagonalMint = Color(0xFF4FA98A)
val LightModeDiagonalBlue = Color(0xFF5B8FC0)

// Favorite heart color — clear, warm red that reads as "red" on both dark and light surfaces
val FavoriteRed = Color(0xFFEF5350)

// Format badge accents (muted, readable on both dark and light surfaces)
val LightningAmber = AccentAmber
val WorkshopPurple = AccentPurple
val PresentationBlue = AccentBlue

val RoomAccentColors = listOf(
    AccentBlue,
    AccentMint,
    AccentCoral,
    AccentPurple,
    AccentRose,
    AccentTeal,
    AccentAmber,
    AccentIndigo
)