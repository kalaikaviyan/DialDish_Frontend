package com.simats.directdine.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Only the directdine Light Mode Colors remain
private val LightColorScheme = lightColorScheme(
    primary = LightModeOrange,
    background = CreamBackground,
    surface = WhiteCard,
    onPrimary = Color.White,
    onBackground = LightModeText,
    onSurface = LightModeText
)

@Composable
fun directdineTheme(
    content: @Composable () -> Unit
) {
    // ALWAYS force the Light Color Scheme
    val colorScheme = LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            // Force status bar icons (wifi, battery) to be dark so they are visible on light background
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}