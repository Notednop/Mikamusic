package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = AppleMusicRed,
    secondary = AppleMusicPink,
    tertiary = AudiophileGold,
    background = DarkBg,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = Color(0xFF381E72),       // Deep Purple Contrast text
    onSecondary = Color(0xFF21005D),     // Ultra-Deep Purple Contrast
    onBackground = Color(0xFFE6E1E5),    // Immersive main text color
    onSurface = Color(0xFFE6E1E5),       // Immersive main surface text
    onSurfaceVariant = Color(0xFFCAC4D0) // Secondary label text
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),         // Standard M3 light purple equivalent
    secondary = Color(0xFF625B71),
    tertiary = AudiophileGold,
    background = LightBg,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF49454F)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep false to preserve strict Immersive UI brand design
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
