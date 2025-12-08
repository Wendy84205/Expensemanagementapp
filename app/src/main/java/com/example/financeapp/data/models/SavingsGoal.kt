package com.example.financeapp.data.models

import java.util.*

data class SavingsGoal(
    val id: String = "",
    val name: String = "",
    val targetAmount: Long = 0,
    val currentAmount: Long = 0, // Số tiền đã tiết kiệm được
    val deadline: Long = 0, // Timestamp
    val category: String = "",
    val userId: String = "",
    val color: Int = 0,
    val icon: Int = 0,
    val description: String = "",
    val progress: Float = 0f,
    val isCompleted: Boolean = false,
    val monthlyContribution: Long = 0, // Số tiền muốn góp hàng tháng
    val startDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
) {
    constructor() : this("", "", 0, 0, 0, "", "", 0, 0, "", 0f, false, 0, System.currentTimeMillis(), true)

    // Tính số tháng còn lại đến deadline
    fun getRemainingMonths(): Int {
        if (deadline == 0L) return 0
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = deadline
        val deadlineMonth = calendar.get(Calendar.MONTH)
        val deadlineYear = calendar.get(Calendar.YEAR)

        calendar.timeInMillis = System.currentTimeMillis()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        return (deadlineYear - currentYear) * 12 + (deadlineMonth - currentMonth)
    }

    // Tính số tiền cần góp mỗi tháng để đạt mục tiêu
    fun getMonthlyNeeded(): Long {
        val remaining = targetAmount - currentAmount
        val remainingMonths = getRemainingMonths()

        return if (remaining > 0 && remainingMonths > 0) {
            (remaining / remainingMonths).coerceAtLeast(1)
        } else 0
    }

    // Tính % hoàn thành
    fun calculateProgress(): Float {
        return if (targetAmount > 0) {
            (currentAmount.toFloat() / targetAmount.toFloat() * 100).coerceAtMost(100f)
        } else 0f
    }
}