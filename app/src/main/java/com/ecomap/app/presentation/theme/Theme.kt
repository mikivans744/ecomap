package com.ecomap.app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = EcoGreen,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = EcoGreenLight,
    secondary = EcoGreenDark,
    background = EcoCream,
    surface = EcoCream
)

private val DarkColors = darkColorScheme(
    primary = EcoGreenLight,
    onPrimary = EcoDarkSurface,
    background = EcoDarkSurface,
    surface = EcoDarkSurface
)

@Composable
fun EcoMapTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (androidx.compose.foundation.isSystemInDarkTheme()) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
