package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonPink,
    onPrimary = Color.White,
    primaryContainer = SurfaceDark3,
    onPrimaryContainer = TextDarkPrimary,
    secondary = BrightCyan,
    onSecondary = ObsidianBg,
    secondaryContainer = SurfaceDark2,
    onSecondaryContainer = TextDarkPrimary,
    tertiary = NeonPurple,
    onTertiary = Color.White,
    background = ObsidianBg,
    onBackground = TextDarkPrimary,
    surface = SurfaceDark1,
    onSurface = TextDarkPrimary,
    surfaceVariant = SurfaceDark2,
    onSurfaceVariant = TextDarkSecondary,
    outline = SurfaceDarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = NeonPurple,
    onPrimary = Color.White,
    primaryContainer = SurfaceLight3,
    onPrimaryContainer = TextLightPrimary,
    secondary = ElectricBlue,
    onSecondary = Color.White,
    secondaryContainer = SurfaceLight2,
    onSecondaryContainer = TextLightPrimary,
    tertiary = NeonPink,
    onTertiary = Color.White,
    background = LightBg,
    onBackground = TextLightPrimary,
    surface = SurfaceLight1,
    onSurface = TextLightPrimary,
    surfaceVariant = SurfaceLight2,
    onSurfaceVariant = TextLightSecondary,
    outline = SurfaceLightBorder
)

@Composable
fun PrismChatTheme(
    darkTheme: Boolean = true, // Default to smooth dark mode as requested by user
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Keep backwards-compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    PrismChatTheme(darkTheme = darkTheme, content = content)
}
