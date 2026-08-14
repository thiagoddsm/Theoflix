package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGold,
    primaryContainer = PrimaryContainerGold,
    onPrimary = OnGold,
    secondary = SecondaryGold,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onBackground = Color(0xFFF1F5F9), // slate 100
    onSurface = Color(0xFFF1F5F9),
    onSurfaceVariant = LightGraySec,
    outline = Color(0x33FFFFFF)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGold,
    primaryContainer = Color(0xFFFEF9C3),
    onPrimary = OnGold,
    secondary = SecondaryGold,
    background = LightBg,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVer,
    onBackground = Color(0xFF0F172A), // slate 900
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFE2E8F0)
)

@Composable
fun TheoflixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
