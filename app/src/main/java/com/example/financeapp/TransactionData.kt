package com.example.financeapp

import androidx.compose.ui.graphics.Color

data class TransactionData(
    val id: String,
    val date: String,
    val dayOfWeek: String,
    val category: String,
    val amount: Double,
    val isIncome: Boolean,
    val group: String,
    val wallet: String,
    val description: String,
    val categoryIcon: String? = null,
    val categoryColor: Color? = null
)