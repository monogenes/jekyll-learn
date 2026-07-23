package com.renovationtracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF2E5E4E)
private val GreenLight = Color(0xFF4E8571)
private val Amber = Color(0xFFF2B84B)

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDE8DC),
    onPrimaryContainer = Color(0xFF0C2019),
    secondary = Amber,
    onSecondary = Color(0xFF3A2E00),
    tertiary = GreenLight,
)

private val DarkColors = darkColorScheme(
    primary = GreenLight,
    onPrimary = Color(0xFF08281E),
    primaryContainer = Green,
    onPrimaryContainer = Color(0xFFCDE8DC),
    secondary = Amber,
    onSecondary = Color(0xFF3A2E00),
    tertiary = Green,
)

@Composable
fun RenovationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
