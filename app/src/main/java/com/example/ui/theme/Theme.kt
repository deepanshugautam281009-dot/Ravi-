package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GuardianColorScheme = darkColorScheme(
    primary = EmergencyRed,
    secondary = EmergencyOrange,
    tertiary = PrimaryTeal,
    background = DarkBackground,
    surface = SurfaceDark,
    onPrimary = NeutralWhite,
    onSecondary = NeutralWhite,
    onTertiary = DarkBackground,
    onBackground = NeutralWhite,
    onSurface = NeutralWhite,
    outline = BorderColor
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GuardianColorScheme,
        typography = Typography,
        content = content
    )
}
