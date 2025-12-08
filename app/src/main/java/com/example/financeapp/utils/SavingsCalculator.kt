package com.example.financeapp.utils

import com.example.financeapp.data.models.SavingsGoal

object SavingsCalculator {

    /**
     * Tính số tiền có thể tiết kiệm trong tháng này
     * @param monthlyIncome Tổng thu nhập tháng
     * @param monthlyExpenses Tổng chi tiêu tháng
     * @param existingSavingsGoals Các mục tiêu tiết kiệm hiện có
     * @return Số tiền có thể phân bổ cho tiết kiệm
     */
    fun calculateAvailableForSavings(
        monthlyIncome: Long,
        monthlyExpenses: Long,
        existingSavingsGoals: List<SavingsGoal>
    ): Long {
        // Số tiền còn lại sau khi trừ chi tiêu
        val remainingIncome = monthlyIncome - monthlyExpenses

        // Tổng số tiền cần góp cho tất cả các mục tiêu đang active
        val totalMonthlyNeeded = existingSavingsGoals
            .filter { it.isActive && !it.isCompleted }
            .sumOf { it.getMonthlyNeeded() }

        // Số tiền thực sự có thể tiết kiệm
        return minOf(remainingIncome, totalMonthlyNeeded).coerceAtLeast(0)
    }

    /**
     * Tính phân bổ tiền tiết kiệm cho từng mục tiêu
     */
    fun calculateSavingsDistribution(
        availableForSavings: Long,
        savingsGoals: List<SavingsGoal>
    ): Map<String, Long> {
        if (availableForSavings <= 0) return emptyMap()

        val activeGoals = savingsGoals.filter { it.isActive && !it.isCompleted }
        if (activeGoals.isEmpty()) return emptyMap()

        val totalNeeded = activeGoals.sumOf { it.getMonthlyNeeded() }

        return if (availableForSavings >= totalNeeded) {
            // Có đủ tiền -> góp đủ theo nhu cầu
            activeGoals.associate { it.id to it.getMonthlyNeeded() }
        } else {
            // Không đủ tiền -> phân bổ tỷ lệ
            val distribution = mutableMapOf<String, Long>()
            var remainingAmount = availableForSavings

            // Ưu tiên các mục tiêu gần deadline
            val sortedGoals = activeGoals.sortedBy { it.deadline }

            sortedGoals.forEach { goal ->
                if (remainingAmount > 0) {
                    val proportion = if (totalNeeded > 0) {
                        goal.getMonthlyNeeded().toFloat() / totalNeeded.toFloat()
                    } else 0f

                    val allocated = (availableForSavings * proportion).toLong().coerceAtLeast(1)
                    val finalAllocation = minOf(allocated, remainingAmount)

                    distribution[goal.id] = finalAllocation
                    remainingAmount -= finalAllocation
                }
            }

            distribution
        }
    }

    /**
     * Tự động thêm tiền vào savings goals dựa trên phân bổ
     */
    fun getAutoAddAmounts(
        availableForSavings: Long,
        savingsGoals: List<SavingsGoal>
    ): Map<String, Long> {
        val distribution = calculateSavingsDistribution(availableForSavings, savingsGoals)

        // Chỉ thêm vào các mục tiêu chưa hoàn thành
        return distribution.mapValues { (goalId, amount) ->
            val goal = savingsGoals.find { it.id == goalId }
            if (goal != null && !goal.isCompleted && amount > 0) {
                amount
            } else 0
        }.filterValues { it > 0 }
    }

    /**
     * Kiểm tra xem user có thể đạt được mục tiêu không
     */
    fun checkGoalFeasibility(
        goal: SavingsGoal,
        monthlyIncome: Long,
        monthlyExpenses: Long,
        otherSavingsNeeds: Long = 0
    ): Pair<Boolean, String> {
        val remainingIncome = monthlyIncome - monthlyExpenses - otherSavingsNeeds
        val monthlyNeeded = goal.getMonthlyNeeded()

        return if (remainingIncome <= 0) {
            Pair(false, "Thu nhập không đủ để tiết kiệm")
        } else if (monthlyNeeded > remainingIncome) {
            val neededMonths = if (goal.targetAmount > goal.currentAmount) {
                val remainingAmount = goal.targetAmount - goal.currentAmount
                (remainingAmount / remainingIncome) + 1
            } else 0

            Pair(false, "Cần ${neededMonths} tháng để hoàn thành mục tiêu")
        } else {
            Pair(true, "Có thể đạt được mục tiêu")
        }
    }
}