package com.example.financeapp.components.utils

class Utils {
    fun formatCurrency(value: Double): String {
        return "%,.0f".format(value)
    }
}