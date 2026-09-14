package com.olavbg.javazone.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.olavbg.javazone.model.BackgroundMode
import com.olavbg.javazone.ui.components.AnimatedDiagonalBackground

private val DarkColorScheme = darkColorScheme(
    primary = AccentMintLight,
    secondary = AccentBlueLight,
    tertiary = AccentRoseLight,
    onPrimary = Color(0xFF0E1A14),
    onSecondary = Color(0xFF101A24),
    onTertiary = Color(0xFF250E15),
    background = DarkBackground.copy(alpha = 0.45f),
    surface = DarkSurface.copy(alpha = 0.74f),
    surfaceVariant = DarkSurfaceVariant.copy(alpha = 0.66f),
    surfaceContainer = DarkSurface.copy(alpha = 0.74f),
    surfaceContainerHigh = DarkSurface.copy(alpha = 0.78f),
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface,
    onSurfaceVariant = Color(0xFFC4CBD3),
    outlineVariant = DarkOutline,
    primaryContainer = Color(0xFF2E5242).copy(alpha = 0.80f),
    onPrimaryContainer = Color(0xFFC8EEDA),
    secondaryContainer = Color(0xFF34445A).copy(alpha = 0.80f),
    onSecondaryContainer = Color(0xFFD0DFEE),
    tertiaryContainer = Color(0xFF59343F).copy(alpha = 0.80f),
    onTertiaryContainer = Color(0xFFF2CCD5),
    error = Color(0xFFF2B8B5),
    errorContainer = Color(0xFF4A292B).copy(alpha = 0.80f),
    onErrorContainer = Color(0xFFF5CDCB)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF3C7A63),
    secondary = Color(0xFF5A7FA4),
    tertiary = Color(0xFFB0677E),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    background = LightBackground.copy(alpha = 0.75f),
    surface = LightSurface.copy(alpha = 0.84f),
    surfaceVariant = LightSurfaceVariant.copy(alpha = 0.80f),
    surfaceContainer = LightSurface.copy(alpha = 0.88f),
    surfaceContainerHigh = LightSurface.copy(alpha = 0.92f),
    onBackground = LightOnSurface,
    onSurface = LightOnSurface,
    onSurfaceVariant = Color(0xFF4E565F),
    outlineVariant = Color(0xFFDDE1E5),
    primaryContainer = Color(0xFFD3EBE0).copy(alpha = 0.90f),
    onPrimaryContainer = Color(0xFF22463A),
    secondaryContainer = Color(0xFFD9E4F0).copy(alpha = 0.90f),
    onSecondaryContainer = Color(0xFF2B3D51),
    tertiaryContainer = Color(0xFFF0DDE3).copy(alpha = 0.90f),
    onTertiaryContainer = Color(0xFF5E3340),
    error = Color(0xFFB4544D),
    errorContainer = Color(0xFFF6DFDC).copy(alpha = 0.90f),
    onErrorContainer = Color(0xFF6E2926)
)

@Composable
fun JavaZoneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    backgroundMode: BackgroundMode = BackgroundMode.Animated,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes
    ) {
        // Theme-level background: the animated diagonal stays consistent across all
        // screens and navigation transitions, behind the translucent surfaces.
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedDiagonalBackground(
                baseColor = if (darkTheme) DarkBackground else LightBackground,
                tintPrimary = if (darkTheme) DiagonalMintLight else AccentMint,
                tintSecondary = if (darkTheme) DiagonalBlueLight else AccentBlue,
                mode = backgroundMode,
                modifier = Modifier.fillMaxSize()
            )
            content()
        }
    }
}