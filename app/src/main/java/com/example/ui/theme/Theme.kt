package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CosmicDarkColorScheme = darkColorScheme(
    primary = CosmicNeonPurple,
    secondary = CosmicNeonCyan,
    tertiary = CosmicNeonPink,
    background = DeepSpaceBackground,
    surface = SecondarySpaceBackground,
    onPrimary = Color.White,
    onSecondary = DeepSpaceBackground,
    onTertiary = Color.White,
    onBackground = SoftWhite,
    onSurface = SoftWhite,
    error = CosmicNeonPink,
    onError = Color.White
)

private val CosmicLightColorScheme = lightColorScheme(
    primary = CosmicNeonPurple,
    secondary = CosmicNeonCyan,
    tertiary = CosmicNeonPink,
    background = Color(0xFFF8FAFC),       // Slate-50 Background
    surface = Color(0xFFFFFFFF),          // pure white Containers
    onPrimary = Color.White,
    onSecondary = Color(0xFF0F172A),
    onTertiary = Color.White,
    onBackground = Color(0xFF0F172A),     // Slate-900 Core Text
    onSurface = Color(0xFF1E293B),        // Slate-800 Card Text
    error = Color(0xFFE11D48),
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CosmicDarkColorScheme else CosmicLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
