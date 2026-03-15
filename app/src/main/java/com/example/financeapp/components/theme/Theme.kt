package com.example.financeapp.components.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

// 🎨 Hiện tại chỉ hỗ trợ Light Mode để đồng bộ thiết kế cao cấp (Premium)
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6366F1), // Modern Indigo
    secondary = Color(0xFF10B981), // Emerald Green
    tertiary = Color(0xFFF59E0B), // Amber Alert
    background = Color(0xFFF8FAFC), // Slate 50
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF1E293B),
    error = Color(0xFFEF4444),
    onError = Color.White,
    outline = Color(0xFFE2E8F0)
)

@Composable
fun FinanceAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Luôn ưu tiên LightColorScheme để giữ thiết kế tươi sáng đồng bộ
    val colorScheme = LightColorScheme
    val appColors = AppColors()

    CompositionLocalProvider(
        LocalAppColors provides appColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography, // Sẽ định nghĩa trong Type.kt nếu cần, mặc định Material3
            content = content
        )
    }
}
