package com.qian.scrollsanity.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Arete Brand Colors
val AretePrimary = Color(0xFF4A90A4)
val AretePrimaryDark = Color(0xFF2D6A7A)
val AreteAccent = Color(0xFF88B04B)
val AreteDanger = Color(0xFFE74C3C)
val AreteWarning = Color(0xFFF39C12)

private val LightColorScheme = lightColorScheme(
    primary = AretePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4E9EF),
    onPrimaryContainer = AretePrimaryDark,
    secondary = Color(0xFF6B5B95),
    onSecondary = Color.White,
    tertiary = AreteAccent,
    onTertiary = Color.White,
    background = Color(0xFFFAFBFC),
    onBackground = Color(0xFF1A1A2E),
    surface = Color.White,
    onSurface = Color(0xFF1A1A2E),
    surfaceVariant = Color(0xFFF5F7FA),
    onSurfaceVariant = Color(0xFF6B7280),
    error = AreteDanger,
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF7AB8C9),
    onPrimary = Color(0xFF003544),
    primaryContainer = AretePrimaryDark,
    onPrimaryContainer = Color(0xFFD4E9EF),
    secondary = Color(0xFFCFC4E8),
    onSecondary = Color(0xFF362E4A),
    tertiary = Color(0xFFA8CF6E),
    onTertiary = Color(0xFF283618),
    background = Color(0xFF121218),
    onBackground = Color(0xFFE4E2E6),
    surface = Color(0xFF1C1C22),
    onSurface = Color(0xFFE4E2E6),
    surfaceVariant = Color(0xFF2A2A32),
    onSurfaceVariant = Color(0xFFC6C5D0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun AreteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}