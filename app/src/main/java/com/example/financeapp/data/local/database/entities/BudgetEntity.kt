package com.example.financeapp.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.financeapp.data.models.Budget
import com.example.financeapp.data.models.BudgetPeriodType
import java.time.LocalDate

@Entity(tableName = "budgets")
@TypeConverters(Converters::class)
data class BudgetEntity(
    @PrimaryKey
    val id: String,
    val categoryId: String,
    val amount: Double,
    val periodType: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val note: String? = null,
    val spentAmount: Double = 0.0,
    val isActive: Boolean = true,
    val userId: String = "",
    val lastModified: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val version: Int = 1,
    val isSynced: Boolean = false
) {
    fun toBudget(): Budget {
        return Budget(
            id = id,
            categoryId = categoryId,
            amount = amount,
            periodType = BudgetPeriodType.fromName(periodType),
            startDate = startDate,
            endDate = endDate,
            note = note,
            spentAmount = spentAmount,
            isActive = isActive,
            userId = userId,
            lastModified = lastModified,
            isDeleted = isDeleted,
            version = version
        )
    }

    companion object {
        fun fromBudget(budget: Budget): BudgetEntity {
            return BudgetEntity(
                id = budget.id,
                categoryId = budget.categoryId,
                amount = budget.amount,
                periodType = budget.periodType.name,
                startDate = budget.startDate,
                endDate = budget.endDate,
                note = budget.note,
                spentAmount = budget.spentAmount,
                isActive = budget.isActive,
                userId = budget.userId,
                lastModified = budget.lastModified,
                isDeleted = budget.isDeleted,
                version = budget.version,
                isSynced = false
            )
        }
    }
}