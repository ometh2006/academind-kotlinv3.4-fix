package com.academind.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Primary       = Color(0xFF6E56CF)
val PrimaryLight  = Color(0xFF9F8AEA)
val PrimaryDark   = Color(0xFF5A42B8)
val Secondary     = Color(0xFFFFB347)
val Accent        = Color(0xFF4EC9B0)
val Success       = Color(0xFF4ADE80)
val Warning       = Color(0xFFFBBF24)
val Danger        = Color(0xFFF87171)

private val DarkColors = darkColorScheme(
    primary             = Primary,
    onPrimary           = Color.White,
    primaryContainer    = Color(0xFF1E1A3A),
    onPrimaryContainer  = PrimaryLight,
    secondary           = Secondary,
    onSecondary         = Color(0xFF1A1000),
    tertiary            = Accent,
    onTertiary          = Color.White,
    background          = Color(0xFF0A0C12),
    onBackground        = Color(0xFFF0F3FA),
    surface             = Color(0xFF141820),
    onSurface           = Color(0xFFF0F3FA),
    surfaceVariant      = Color(0xFF1E2432),
    onSurfaceVariant    = Color(0xFF9CA3B0),
    outline             = Color(0xFF6E56CF).copy(alpha = 0.22f),
    error               = Danger,
    onError             = Color.White
)

private val LightColors = lightColorScheme(
    primary             = PrimaryDark,
    onPrimary           = Color.White,
    primaryContainer    = Color(0xFFEDE8FF),
    onPrimaryContainer  = PrimaryDark,
    secondary           = Color(0xFFE8920D),
    onSecondary         = Color.White,
    tertiary            = Color(0xFF0FA89A),
    onTertiary          = Color.White,
    background          = Color(0xFFF0F2F8),
    onBackground        = Color(0xFF1A1D2E),
    surface             = Color(0xFFFFFFFF),
    onSurface           = Color(0xFF1A1D2E),
    surfaceVariant      = Color(0xFFEEF0FA),
    onSurfaceVariant    = Color(0xFF4B5270),
    outline             = Color(0xFF6E56CF).copy(alpha = 0.15f),
    error               = Color(0xFFDC2626),
    onError             = Color.White
)

@Composable
fun AcadeMindTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content     = content
    )
}
