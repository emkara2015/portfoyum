package com.antigravity.networthtracker.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
