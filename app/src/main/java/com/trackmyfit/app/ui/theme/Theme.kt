package com.trackmyfit.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.trackmyfit.app.data.ThemeMode

val Orange = Color(0xFFFF7A2F)
val Amber = Color(0xFFFFB547)
val Navy = Color(0xFF1A1D3A)

private val Dark = darkColorScheme(
    primary = Orange,
    onPrimary = Color(0xFF2B1100),
    primaryContainer = Color(0xFF5A2A0C),
    onPrimaryContainer = Color(0xFFFFDBCA),
    secondary = Amber,
    onSecondary = Color(0xFF2A1A00),
    secondaryContainer = Color(0xFF2E3260),
    onSecondaryContainer = Color(0xFFDDE0FF),
    tertiary = Color(0xFF7FD1AE),
    background = Color(0xFF111320),
    onBackground = Color(0xFFE4E4EE),
    surface = Color(0xFF111320),
    onSurface = Color(0xFFE4E4EE),
    surfaceVariant = Color(0xFF2A2D40),
    onSurfaceVariant = Color(0xFFC3C5D6),
    surfaceContainerLowest = Color(0xFF0C0E18),
    surfaceContainerLow = Color(0xFF171A29),
    surfaceContainer = Color(0xFF1B1E2E),
    surfaceContainerHigh = Color(0xFF242839),
    surfaceContainerHighest = Color(0xFF2E3245),
    outline = Color(0xFF5B5F75),
    outlineVariant = Color(0xFF363A4F),
)

private val Light = lightColorScheme(
    primary = Color(0xFFE2601A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCA),
    onPrimaryContainer = Color(0xFF3A1500),
    secondary = Color(0xFF3A3F7A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDE0FF),
    onSecondaryContainer = Navy,
    tertiary = Color(0xFF1E7A55),
    background = Color(0xFFFBF8FF),
    surface = Color(0xFFFBF8FF),
    surfaceContainerLow = Color(0xFFF4F1FA),
    surfaceContainer = Color(0xFFEFECF6),
    surfaceContainerHigh = Color(0xFFE9E6F0),
    surfaceContainerHighest = Color(0xFFE3E1EB),
)

@Composable
fun TrackMyFitTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
