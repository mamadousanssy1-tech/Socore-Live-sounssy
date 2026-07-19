package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkStadiumColorScheme = darkColorScheme(
    primary = StadiumGreen,
    onPrimary = PitchOnyx,
    primaryContainer = DarkGreen,
    onPrimaryContainer = Color.White,
    secondary = StadiumBlue,
    onSecondary = Color.White,
    tertiary = StadiumYellow,
    onTertiary = PitchOnyx,
    background = PitchOnyx,
    onBackground = Color.White,
    surface = PitchSurface,
    onSurface = Color.White,
    surfaceVariant = PitchSurfaceVariant,
    onSurfaceVariant = Color.LightGray,
    error = StadiumRed,
    onError = Color.White
)

// We focus on a premium dark stadium theme for immersive matches
@Composable
fun FootDirectTheme(
    darkTheme: Boolean = true, // Force premium dark by default for night-stadium feel
    dynamicColor: Boolean = false, // Disable dynamic colors to enforce the premium green brand
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkStadiumColorScheme else DarkStadiumColorScheme // Keep Dark Stadium theme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Legacy compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    FootDirectTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
