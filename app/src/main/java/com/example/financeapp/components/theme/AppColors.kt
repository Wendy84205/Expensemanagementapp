package com.example.financeapp

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
@Composable
fun getAppColors(): AppColors {
    val colorScheme = MaterialTheme.colorScheme

    return AppColors(
        primary = Color(0xFF0F4C75), // Navy
        secondary = Color(0xFF2E8B57), // Success Green
        background = Color(0xFFF5F7FA), // SoftGray
        surface = Color.White,
        onPrimary = Color.White,
        onBackground = Color(0xFF2D3748), // TextDark
        onSurface = Color(0xFF2D3748),
        // Custom colors đồng bộ với HomeScreen
        incomeColor = Color(0xFF2E8B57), // Success Green
        expenseColor = Color(0xFFED8936), // Accent Orange
        warningColor = Color(0xFFED8936), // Accent Orange
        errorColor = Color(0xFFE53E3E), // Error Red
        successColor = Color(0xFF2E8B57), // Success Green
        cardColor = Color.White,
        textPrimary = Color(0xFF2D3748), // TextDark
        textSecondary = Color(0xFF718096), // TextLight
        dividerColor = Color(0xFFE5E7EB) // Light Gray
    )
}

data class AppColors(
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val onPrimary: Color,
    val onBackground: Color,
    val onSurface: Color,
    val incomeColor: Color,
    val expenseColor: Color,
    val warningColor: Color,
    val errorColor: Color,
    val successColor: Color,
    val cardColor: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val dividerColor: Color
)

