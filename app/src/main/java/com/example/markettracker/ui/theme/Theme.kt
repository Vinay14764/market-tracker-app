package com.example.markettracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(

    primary = PrimaryGreen,
    onPrimary = Background,

    background = Background,
    onBackground = TextPrimary,

    surface = CardBackground,
    onSurface = TextPrimary,

    secondary = TextSecondary,

    outline = DividerColor
)
@Composable
fun CryptoAppTheme(
    content: @Composable () -> Unit
){
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}