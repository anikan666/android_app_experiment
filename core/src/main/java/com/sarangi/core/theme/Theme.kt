package com.sarangi.core.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = SarangiPrimary,
    onPrimary = SarangiOnPrimary,
    secondary = SarangiSecondary,
    onSecondary = SarangiOnSecondary,
    tertiary = SarangiAccent,
    onTertiary = SarangiOnAccent,
    surface = SarangiSurface,
    onSurface = SarangiOnSurface,
    background = SarangiBackground,
    onBackground = SarangiOnBackground,
    error = SarangiError,
    surfaceVariant = SarangiSurface,
    onSurfaceVariant = SarangiOnSurface
)

private val DarkColorScheme = darkColorScheme(
    primary = SarangiPrimaryDark,
    onPrimary = SarangiOnPrimary,
    secondary = SarangiSecondaryDark,
    onSecondary = SarangiOnSecondary,
    tertiary = SarangiAccent,
    onTertiary = SarangiOnAccent,
    surface = SarangiSurfaceDark,
    onSurface = SarangiOnSurfaceDark,
    background = SarangiBackgroundDark,
    onBackground = SarangiOnBackgroundDark,
    error = SarangiError,
    surfaceVariant = SarangiSurfaceDark,
    onSurfaceVariant = SarangiOnSurfaceDark
)

@Composable
fun SarangiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SarangiTypography,
        content = content
    )
}
