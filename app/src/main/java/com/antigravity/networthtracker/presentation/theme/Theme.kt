package com.antigravity.networthtracker.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private val DarkColorScheme = darkColorScheme(
    primary = TradingViewBlue,
    onPrimary = Color.White,
    secondary = LightBlueAccent,
    onSecondary = Color.White,
    background = TradingViewDarkBg,
    onBackground = TextWhite,
    surface = TradingViewCardSurface,
    onSurface = TextWhite,
    surfaceVariant = DividerColor,
    onSurfaceVariant = TextGraySecondary,
    outline = CardBorderColor
)

@Composable
fun NetWorthTrackerTheme(
    content: @Composable () -> Unit
) {
    val currentDensity = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = currentDensity.density,
            fontScale = 1.0f
        )
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            content = content
        )
    }
}
