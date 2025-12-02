package com.example.financeapp.data

import com.example.financeapp.SavingsGoal
import com.example.financeapp.data.models.Transaction
import com.example.financeapp.data.models.User
import com.example.financeapp.UserProfile
import com.example.financeapp.data.models.Budget

// Data class mở rộng cho FinancialData
data class FinancialData(
    val transactions: List<Transaction> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val accounts: List<User> = emptyList(),
    val savingsGoals: List<SavingsGoal> = emptyList(),
    val userProfile: UserProfile? = null
)

// Data class cho phân tích chi tiết
data class FinancialAnalysis(
    val totalIncome: Long,
    val totalExpense: Long,
    val totalBalance: Long,
    val topSpendingCategories: Map<String, Long>,
    val budgetStatus: List<BudgetStatus>,
    val savingsProgress: List<SavingsProgress>
)

data class BudgetStatus(
    val category: String,
    val spent: Long,
    val limit: Long,
    val percentage: Int,
    val isOverBudget: Boolean
)

data class SavingsProgress(
    val name: String,
    val currentAmount: Long,
    val targetAmount: Long,
    val progress: Int,
    val daysRemaining: Int
)