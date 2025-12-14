package com.example.financeapp.data.models

import java.util.Calendar

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
    val isCompleted: Boolean = false,
    val monthlyContribution: Long = 0,
    val startDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // Thêm trường mới cho tính toán tự động
    val autoCalculate: Boolean = false,
    val allocationPercentage: Int = 0,
    val lastAutoCalculation: Long = 0L
) {
    constructor() : this(
        "", "", 0, 0, 0, "", "", 0, 0, "", 0f, false, 0,
        System.currentTimeMillis(), true, System.currentTimeMillis(),
        System.currentTimeMillis(), false, 0, 0L
    )

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