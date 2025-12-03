package com.example.financeapp.components.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 🎨 Màu sắc đồng bộ với HomeScreen
private val AppLightColors = lightColorScheme(
    primary = Color(0xFF0F4C75), // Navy
    secondary = Color(0xFF2E8B57), // Success Green
    background = Color(0xFFF5F7FA), // SoftGray
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Color(0xFF2D3748), // TextDark
    onSurface = Color(0xFF2D3748)
)

@Composable
fun FinanceAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppLightColors,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
