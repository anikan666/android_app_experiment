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
    secondary = SarangiSecondary,
    tertiary = SarangiTertiary,
    surface = SarangiSurface,
    onPrimary = SarangiOnPrimary,
    onSecondary = SarangiOnSecondary,
    onTertiary = SarangiOnTertiary,
    onSurface = SarangiOnSurface,
    background = SarangiBackground,
    onBackground = SarangiOnBackground,
    surfaceVariant = SarangiSurfaceVariant,
    error = SarangiError
)

private val DarkColorScheme = darkColorScheme(
    primary = SarangiPrimaryDark,
    secondary = SarangiSecondaryDark,
    tertiary = SarangiTertiaryDark,
    surface = SarangiSurfaceDark,
    onPrimary = SarangiOnPrimaryDark,
    onSecondary = SarangiOnPrimaryDark,
    onTertiary = SarangiOnTertiary,
    onSurface = SarangiOnSurfaceDark,
    background = SarangiBackgroundDark,
    onBackground = SarangiOnBackgroundDark,
    surfaceVariant = SarangiSurfaceVariantDark,
    error = SarangiError
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
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
