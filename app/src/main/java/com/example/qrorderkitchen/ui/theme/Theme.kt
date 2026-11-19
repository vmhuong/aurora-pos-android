package com.example.qrorderkitchen.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ModernLightColorScheme = lightColorScheme(
    primary = AuroraGold,
    onPrimary = LightSurface,
    primaryContainer = AuroraGoldSoft,
    background = LightBgWarm,
    surface = LightSurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun QROrderKitchenTheme(
    darkTheme: Boolean = false, // Default to Modern Light Theme
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ModernLightColorScheme,
        typography = Typography,
        content = content
    )
}
