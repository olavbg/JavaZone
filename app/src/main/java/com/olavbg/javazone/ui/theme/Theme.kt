package com.olavbg.javazone.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.olavbg.javazone.ui.components.AnimatedDiagonalBackground

private val DarkColorScheme = darkColorScheme(
    primary = FreshGreen,
    secondary = JavaBlue,
    tertiary = JavaOrange,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceContainerLowest = Color(0xFF15171B),
    surfaceContainerLow = Color(0xFF1B1E23),
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = Color(0xFF282C33),
    onPrimary = Color(0xFF17201A),
    onSecondary = Color(0xFF17202A),
    onTertiary = Color(0xFF271A18),
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFB7B9BD),
    secondaryContainer = Color(0xFF344253),
    onSecondaryContainer = Color(0xFFDCE5EF),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF536B5B),
    secondary = Color(0xFF58718F),
    tertiary = Color(0xFF986B62),
    background = LightBackground,
    surface = LightSurface,
    surfaceContainerLowest = Color(0xFFECEAE5),
    surfaceContainerLow = Color(0xFFF0EEE9),
    surfaceContainer = LightSurface,
    surfaceContainerHigh = Color(0xFFE7E5E0),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = LightOnSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF5E6064),
    secondaryContainer = Color(0xFFDCE5EF),
    onSecondaryContainer = Color(0xFF30465D),
)

@Composable
fun JavaZoneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
        shapes = Shapes,
    ) {
        AnimatedDiagonalBackground {
            content()
        }
    }
}
