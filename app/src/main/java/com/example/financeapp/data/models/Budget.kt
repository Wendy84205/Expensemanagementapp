package com.example.financeapp.data.models

import androidx.room.TypeConverter
import com.example.financeapp.viewmodel.settings.LanguageViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

data class Budget(
    val id: String,
    val categoryId: String,
    val amount: Double,
    val periodType: BudgetPeriodType,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val note: String? = null,
    val spentAmount: Double = 0.0,
    val isActive: Boolean = true,
    val userId: String = "",
    val lastModified: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val version: Int = 1
)

enum class BudgetPeriodType {
    WEEK,
    MONTH,
    QUARTER,
    YEAR;

    companion object {
        fun fromName(name: String): BudgetPeriodType {
            return values().find { it.name == name } ?: MONTH
        }
    }

    fun getDisplayName(languageViewModel: LanguageViewModel): String {
        return when (this) {
            WEEK -> languageViewModel.getTranslation("week")
            MONTH -> languageViewModel.getTranslation("month")
            QUARTER -> languageViewModel.getTranslation("quarter")
            YEAR -> languageViewModel.getTranslation("year")
        }
    }
}

fun calculateBudgetEndDate(startDate: LocalDate, periodType: BudgetPeriodType): LocalDate {
    return when (periodType) {
        BudgetPeriodType.WEEK -> startDate.plusWeeks(1)
        BudgetPeriodType.MONTH -> startDate.plusMonths(1)
        BudgetPeriodType.QUARTER -> startDate.plusMonths(3)
        BudgetPeriodType.YEAR -> startDate.plusYears(1)
    }
}

val Budget.remainingAmount: Double
    get() = amount - spentAmount

val Budget.progressPercentage: Float
    get() = if (amount > 0) (spentAmount / amount).toFloat() else 0f

val Budget.isOverBudget: Boolean
    get() = spentAmount > amount

val Budget.progressColor: String
    get() = when {
        progressPercentage < 0.7 -> "#48BB78"
        progressPercentage < 0.9 -> "#ED8936"
        else -> "#F56565"
    }

val Budget.statusText: String
    get() = when {
        !isActive -> "Đã hết hạn"
        isOverBudget -> "Vượt hạn mức"
        progressPercentage > 0.9 -> "Sắp hết hạn mức"
        else -> "Đang hoạt động"
    }

class LocalDateConverter {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.format(formatter)
    }

    @TypeConverter
    fun toLocalDate(dateString: String?): LocalDate? {
        return dateString?.let { LocalDate.parse(it, formatter) }
    }
}

class BudgetPeriodTypeConverter {
    @TypeConverter
    fun fromBudgetPeriodType(type: BudgetPeriodType): String {
        return type.name
    }

    @TypeConverter
    fun toBudgetPeriodType(name: String): BudgetPeriodType {
        return BudgetPeriodType.fromName(name)
    }
}