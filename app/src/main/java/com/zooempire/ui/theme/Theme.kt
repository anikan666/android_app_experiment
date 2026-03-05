package com.zooempire.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

val ZooGreen = Color(0xFF2E7D32)
val ZooGreenLight = Color(0xFF66BB6A)
val ZooGreenDark = Color(0xFF1B5E20)
val ZooBrown = Color(0xFF795548)
val ZooSand = Color(0xFFF5E6CC)
val ZooGold = Color(0xFFFFD54F)
val ZooBlue = Color(0xFF42A5F5)
val ZooRed = Color(0xFFEF5350)

private val LightColorScheme = lightColorScheme(
    primary = ZooGreen,
    onPrimary = Color.White,
    primaryContainer = ZooGreenLight,
    onPrimaryContainer = ZooGreenDark,
    secondary = ZooBrown,
    onSecondary = Color.White,
    secondaryContainer = ZooSand,
    onSecondaryContainer = ZooBrown,
    tertiary = ZooGold,
    background = Color(0xFFF1F8E9),
    surface = Color.White,
    error = ZooRed,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

@Composable
fun ZooEmpireTheme(content: @Composable () -> Unit) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = ZooGreenDark.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            titleLarge = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            ),
            titleMedium = TextStyle(
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            ),
            bodyMedium = TextStyle(
                fontSize = 14.sp
            ),
            labelSmall = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        ),
        content = content
    )
}
