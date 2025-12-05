package com.example.financeapp.data.local.dao

import androidx.room.*
import com.example.financeapp.data.local.database.entities.RecurringExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: RecurringExpenseEntity)

    @Update
    suspend fun update(expense: RecurringExpenseEntity)

    @Delete
    suspend fun delete(expense: RecurringExpenseEntity)

    @Query("SELECT * FROM recurring_expenses WHERE userId = :userId ORDER BY nextOccurrence ASC")
    fun getRecurringExpensesByUser(userId: String): Flow<List<RecurringExpenseEntity>>

    @Query("SELECT * FROM recurring_expenses WHERE userId = :userId AND isActive = 1")
    fun getActiveRecurringExpenses(userId: String): Flow<List<RecurringExpenseEntity>>

    @Query("SELECT * FROM recurring_expenses WHERE userId = :userId AND nextOccurrence = :date")
    fun getRecurringExpensesByDate(userId: String, date: String): Flow<List<RecurringExpenseEntity>>

    @Query("SELECT * FROM recurring_expenses WHERE isSynced = 0")
    suspend fun getUnsyncedRecurringExpenses(): List<RecurringExpenseEntity>

    @Query("DELETE FROM recurring_expenses WHERE userId = :userId")
    suspend fun deleteAllByUser(userId: String)
}