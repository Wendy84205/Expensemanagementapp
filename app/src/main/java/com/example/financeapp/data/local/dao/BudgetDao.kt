package com.example.financeapp.data.local.dao

import androidx.room.*
import com.example.financeapp.data.local.database.entities.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity)

    @Update
    suspend fun update(budget: BudgetEntity)

    @Delete
    suspend fun delete(budget: BudgetEntity)

    @Query("SELECT * FROM budgets WHERE userId = :userId")
    fun getBudgetsByUser(userId: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE userId = :userId AND categoryId = :categoryId AND isActive = 1")
    fun getActiveBudgetByCategory(userId: String, categoryId: String): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE userId = :userId AND isActive = 1")
    fun getActiveBudgets(userId: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE isSynced = 0")
    suspend fun getUnsyncedBudgets(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE userId = :userId AND isDeleted = 1")
    suspend fun getDeletedBudgets(userId: String): List<BudgetEntity>

    @Query("UPDATE budgets SET isSynced = 1 WHERE id = :budgetId")
    suspend fun markAsSynced(budgetId: String)

    @Query("SELECT * FROM budgets WHERE userId = :userId AND lastModified > :since")
    suspend fun getBudgetsModifiedSince(userId: String, since: Long): List<BudgetEntity>

    @Query("DELETE FROM budgets WHERE userId = :userId AND isDeleted = 1")
    suspend fun clearDeletedBudgets(userId: String)

    @Query("UPDATE budgets SET isActive = 0 WHERE userId = :userId AND isActive = 1 AND endDate < :currentDate")
    suspend fun deactivateExpiredBudgets(userId: String, currentDate: String)

    @Query("SELECT * FROM budgets WHERE userId = :userId AND categoryId = :categoryId AND isActive = 1 AND :date BETWEEN startDate AND endDate")
    suspend fun getCurrentBudgetForCategory(userId: String, categoryId: String, date: String): BudgetEntity?

    @Query("DELETE FROM budgets WHERE userId = :userId")
    suspend fun deleteAllByUser(userId: String)
}