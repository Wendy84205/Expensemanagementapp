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

    @Query("SELECT * FROM budgets WHERE userId = :userId AND categoryId = :categoryId")
    fun getBudgetByCategory(userId: String, categoryId: String): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE userId = :userId AND isActive = 1")
    fun getActiveBudgets(userId: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE isSynced = 0")
    suspend fun getUnsyncedBudgets(): List<BudgetEntity>

    @Query("DELETE FROM budgets WHERE userId = :userId")
    suspend fun deleteAllByUser(userId: String)
}