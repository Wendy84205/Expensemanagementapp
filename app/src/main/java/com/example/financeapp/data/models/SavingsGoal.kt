package com.example.financeapp.data.models

data class SavingsGoal(
    val id: String = "",
    val name: String = "",
    val targetAmount: Long = 0,
    val currentAmount: Long = 0,
    val deadline: Long = 0,
    val category: String = "",
    val userId: String = "",
    val color: Int = 0,
    val icon: Int = 0,
    val description: String = "",
    val progress: Float = 0f,
    val isCompleted: Boolean = false
) {
    constructor() : this("", "", 0, 0, 0, "", "", 0, 0, "", 0f, false)
}