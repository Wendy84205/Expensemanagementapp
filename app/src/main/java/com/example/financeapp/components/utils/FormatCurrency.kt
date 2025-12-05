package com.example.financeapp.components.utils

import java.text.NumberFormat
import java.util.*

// Function format currency - chỉ có MỘT hàm duy nhất trong toàn bộ project
fun formatCurrency(amount: Float): String {
    val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
    return "${formatter.format(amount.toDouble())} đ"
}