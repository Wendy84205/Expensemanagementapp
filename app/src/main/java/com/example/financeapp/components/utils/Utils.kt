package com.example.financeapp

class Utils {
    fun formatCurrency(value: Double): String {
        return "%,.0f".format(value)
    }
}