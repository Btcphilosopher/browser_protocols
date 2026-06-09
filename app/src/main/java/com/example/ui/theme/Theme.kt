package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AureomDarkColorScheme = darkColorScheme(
    primary = AureomGold,
    secondary = HighTechCyan,
    tertiary = NeonMagenta,
    background = AmbientDarkBg,
    surface = PanelBlack,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = CoralAlert
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AureomDarkColorScheme,
        typography = Typography,
        content = content
    )
}
