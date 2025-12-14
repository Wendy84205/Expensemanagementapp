package com.example.financeapp.data.local.dao

import androidx.room.*
import com.example.financeapp.data.local.database.entities.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity) // SỬA: TransactionEntity thay vì String

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteById(transactionId: String) // THÊM hàm này để xóa theo ID

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC")
    fun getTransactionsByUser(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND isIncome = :isIncome ORDER BY date DESC")
    fun getTransactionsByType(userId: String, isIncome: Boolean): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(userId: String, startDate: String, endDate: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND categoryId = :categoryId")
    fun getTransactionsByCategory(userId: String, categoryId: String): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND isIncome = :isIncome AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalByType(userId: String, isIncome: Boolean, startDate: String, endDate: String): Double?

    @Query("SELECT * FROM transactions WHERE isSynced = 0")
    suspend fun getUnsyncedTransactions(): List<TransactionEntity>

    @Query("DELETE FROM transactions WHERE userId = :userId")
    suspend fun deleteAllByUser(userId: String)

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteTransactionById(transactionId: String)
}