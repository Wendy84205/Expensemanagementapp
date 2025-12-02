package com.example.financeapp.data

import java.time.LocalDate

/**
 * Mô hình dữ liệu cho Hạn mức chi tiêu (Budget)
 */
data class Budget(
    val id: String,
    val categoryId: String,            // Gắn với danh mục con (vd: Ăn uống)
    val amount: Double,                // Số tiền hạn mức
    val periodType: BudgetPeriodType,  // Chu kỳ (Tuần / Tháng / Quý / Năm)
    val startDate: LocalDate,          // Ngày bắt đầu áp dụng
    val endDate: LocalDate,            // Ngày kết thúc (tự động theo periodType)
    val note: String? = null,          // Ghi chú tuỳ chọn
    val spentAmount: Double = 0.0,     // Số tiền đã chi tiêu (có thể cập nhật từ transaction)
    val isActive: Boolean = true,       // Đang áp dụng hay đã hết hạn
    val spent: Double = 0.0
)

/**
 * Enum xác định loại chu kỳ của hạn mức
 */
enum class BudgetPeriodType(val displayName: String, val durationDays: Long) {
    WEEK("Tuần này", 7),
    MONTH("Tháng này", 30),
    QUARTER("Quý này", 90),
    YEAR("Năm nay", 365),
}

/**
 * Hàm mở rộng tiện ích để tính ngày kết thúc dựa theo loại chu kỳ
 */
fun calculateBudgetEndDate(startDate: LocalDate, periodType: BudgetPeriodType): LocalDate {
    return startDate.plusDays(periodType.durationDays)
}

// Extension properties cho Budget
val Budget.remainingAmount: Double
    get() = amount - spentAmount

val Budget.progressPercentage: Float
    get() = if (amount > 0) (spentAmount / amount).toFloat() else 0f

val Budget.isOverBudget: Boolean
    get() = spentAmount > amount

val Budget.progressColor: String
    get() = when {
        progressPercentage < 0.7 -> "#48BB78"  // Xanh
        progressPercentage < 0.9 -> "#ED8936"  // Cam
        else -> "#F56565"                      // Đỏ
    }

val Budget.statusText: String
    get() = when {
        !isActive -> "Đã hết hạn"
        isOverBudget -> "Vượt hạn mức"
        progressPercentage > 0.9 -> "Sắp hết hạn mức"
        else -> "Đang hoạt động"
    }

// Extension function cho BudgetPeriodType
fun BudgetPeriodType.getDisplayName(): String {
    return when (this) {
        BudgetPeriodType.WEEK -> "Tuần"
        BudgetPeriodType.MONTH -> "Tháng"
        BudgetPeriodType.QUARTER -> "Quý"
        BudgetPeriodType.YEAR -> "Năm"
    }
}