package com.rideconnect.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreen,
    background = DarkBackground,
    surface = SurfaceColor,
    surfaceVariant = SurfaceContainerHigh,
    error = ErrorRed,
    onBackground = TextOnSurface,
    onSurface = TextOnSurface,
    onSurfaceVariant = TextOnSurfaceVariant
)

@Composable
fun RideConnectTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
