package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TacticalDarkColorScheme = darkColorScheme(
    primary = SignalAmber,
    onPrimary = Color(0xFF1A0D00),
    primaryContainer = SignalAmberContainer,
    onPrimaryContainer = SignalAmberLight,
    secondary = TelemetryCyan,
    onSecondary = Color(0xFF00262B),
    secondaryContainer = TelemetryCyanContainer,
    onSecondaryContainer = TelemetryCyan,
    tertiary = SatelliteEmerald,
    onTertiary = Color(0xFF00291B),
    tertiaryContainer = SatelliteEmeraldContainer,
    onTertiaryContainer = SatelliteEmerald,
    background = SlateNavyDeep,
    onBackground = TextPrimaryDark,
    surface = SlateNavySurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = SlateNavyCard,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerHigh = SlateNavyElevated,
    outline = OutlineDark,
    error = WarningCrimson
)

private val DaylightLightColorScheme = lightColorScheme(
    primary = DaylightAmber,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE5D0),
    onPrimaryContainer = Color(0xFF4A1E00),
    secondary = DaylightCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6F6FA),
    onSecondaryContainer = Color(0xFF00333B),
    tertiary = Color(0xFF059669),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF064E3B),
    background = DaylightBg,
    onBackground = TextPrimaryLight,
    surface = DaylightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = DaylightCard,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerHigh = Color(0xFFDCE5F0),
    outline = OutlineLight,
    error = Color(0xFFDC2626)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) TacticalDarkColorScheme else DaylightLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
