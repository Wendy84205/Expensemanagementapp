package com.example.financeapp

data class UserProfile(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val monthlyIncome: Long = 0,
    val financialGoals: List<String> = emptyList(),
    val currency: String = "VNĐ",
    val joinDate: Long = System.currentTimeMillis(),

    // Các property để tương thích
    val photoUrl: String = "",
    val phoneNumber: String = "",
    val dateOfBirth: Long = 0,
    val occupation: String = "",
    val monthlyExpense: Long = 0,
    val riskTolerance: String = "MEDIUM" // LOW, MEDIUM, HIGH
) {
    constructor() : this("", "", "", 0, emptyList(), "VNĐ", System.currentTimeMillis(), "", "", 0, "", 0, "MEDIUM")
}