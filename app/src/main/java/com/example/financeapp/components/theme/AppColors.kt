package com.example.financeapp.components.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

data class AppColors(
    val primary: Color = Color(0xFF6366F1), // Modern Indigo
    val secondary: Color = Color(0xFF10B981), // Emerald Green
    val accent: Color = Color(0xFFF59E0B), // Amber Alert
    val background: Color = Color(0xFFF8FAFC), // Slate 50
    val surface: Color = Color.White,
    val onPrimary: Color = Color.White,
    val onBackground: Color = Color(0xFF0F172A), // Slate 900
    val onSurface: Color = Color(0xFF1E293B), // Slate 800
    val income: Color = Color(0xFF10B981),
    val expense: Color = Color(0xFFEF4444),
    val primaryGradient: Brush = Brush.verticalGradient(listOf(Color(0xFF6366F1), Color(0xFF4F46E5))),
    val incomeGradient: Brush = Brush.horizontalGradient(listOf(Color(0xFF34D399), Color(0xFF10B981))),
    val expenseGradient: Brush = Brush.horizontalGradient(listOf(Color(0xFFF87171), Color(0xFFEF4444))),
    val accentGradient: Brush = Brush.linearGradient(listOf(Color(0xFFA855F7), Color(0xFF6366F1))),
    val glassBackground: Color = Color.Black.copy(alpha = 0.05f),
    val textPrimary: Color = Color(0xFF0F172A),
    val textSecondary: Color = Color(0xFF64748B),
    val textMuted: Color = Color(0xFF94A3B8),
    val divider: Color = Color(0xFFE2E8F0)
)

val LocalAppColors = staticCompositionLocalOf { AppColors() }

object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}

@Composable
fun getAppColors(): AppColors = LocalAppColors.current
