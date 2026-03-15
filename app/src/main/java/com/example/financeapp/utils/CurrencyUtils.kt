package com.example.financeapp.utils

import java.util.Locale

object CurrencyUtils {
    /**
     * Formats an amount as a VND string
     * Example: 50000 -> "50.000 đ"
     */
    fun formatVND(amount: Float): String {
        return String.format("%,.0f đ", amount).replace(",", ".")
    }

    /**
     * Formats an amount as a VND string (Long)
     * Example: 50000 -> "50.000 đ"
     */
    fun formatVND(amount: Long): String {
        return formatVND(amount.toFloat())
    }
}
