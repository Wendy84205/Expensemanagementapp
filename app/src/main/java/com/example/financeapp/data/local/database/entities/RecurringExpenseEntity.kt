package com.example.financeapp.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.financeapp.data.models.RecurringExpense
import com.example.financeapp.data.models.RecurringFrequency

@Entity(tableName = "recurring_expenses")
data class RecurringExpenseEntity(
    @PrimaryKey
    val id: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val categoryIcon: String = "",
    val categoryColor: String = "",
    val wallet: String = "",
    val description: String? = null,
    val frequency: String = "",
    val startDate: String = "",
    val endDate: String? = null,
    val nextOccurrence: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val userId: String = "",
    val totalGenerated: Int = 0,
    val lastGenerated: String? = null,
    val isSynced: Boolean = false
) {
    fun toRecurringExpense(): RecurringExpense {
        return RecurringExpense.Companion.fromEnum(
            id = id,
            title = title,
            amount = amount,
            category = category,
            categoryIcon = categoryIcon,
            categoryColor = categoryColor,
            wallet = wallet,
            description = description,
            frequency = RecurringFrequency.valueOf(frequency),
            startDate = startDate,
            endDate = endDate,
            nextOccurrence = nextOccurrence,
            isActive = isActive,
            userId = userId,
            totalGenerated = totalGenerated,
            lastGenerated = lastGenerated
        )
    }

    companion object {
        fun fromRecurringExpense(recurringExpense: RecurringExpense): RecurringExpenseEntity {
            return RecurringExpenseEntity(
                id = recurringExpense.id,
                title = recurringExpense.title,
                amount = recurringExpense.amount,
                category = recurringExpense.category,
                categoryIcon = recurringExpense.categoryIcon,
                categoryColor = recurringExpense.categoryColor,
                wallet = recurringExpense.wallet,
                description = recurringExpense.description,
                frequency = recurringExpense.frequency,
                startDate = recurringExpense.startDate,
                endDate = recurringExpense.endDate,
                nextOccurrence = recurringExpense.nextOccurrence,
                isActive = recurringExpense.isActive,
                createdAt = recurringExpense.createdAt,
                userId = recurringExpense.userId,
                totalGenerated = recurringExpense.totalGenerated,
                lastGenerated = recurringExpense.lastGenerated,
                isSynced = false
            )
        }
    }
}