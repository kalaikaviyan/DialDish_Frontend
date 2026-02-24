package com.simats.dialdish.ui.theme

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 1. Map the Soft Dark Mode Colors
private val DarkColorScheme = darkColorScheme(
    primary = DarkModeOrange,             // Uses the pop-orange
    background = SoftDarkBackground,      // Beautiful slate gray, no black
    surface = SoftDarkCard,               // Lighter gray for input fields
    onPrimary = Color.White,
    onBackground = DarkModeText,
    onSurface = DarkModeText
)

// 2. Map the DialDish Light Mode Colors
private val LightColorScheme = lightColorScheme(
    primary = LightModeOrange,            // Uses the lighter tone orange
    background = CreamBackground,
    surface = WhiteCard,
    onPrimary = Color.White,
    onBackground = LightModeText,
    onSurface = LightModeText
)

@Composable
fun DialDishTheme(
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("DialDishPrefs", Context.MODE_PRIVATE)
    val systemTheme = isSystemInDarkTheme()

    // Create an observable state that checks the saved memory
    var isDarkState by remember { mutableStateOf(sharedPrefs.getBoolean("isDarkTheme", systemTheme)) }

    // REAL-TIME LISTENER: This forces every screen to update instantly when Settings are changed!
    DisposableEffect(sharedPrefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == "isDarkTheme") {
                isDarkState = prefs.getBoolean("isDarkTheme", systemTheme)
            }
        }
        sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    // Use the forced theme (if provided), otherwise use our real-time state
    val isDark = darkTheme ?: isDarkState

    val colorScheme = if (isDark) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}