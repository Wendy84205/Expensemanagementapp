package com.example.financeapp.data.local.dao

import androidx.room.*
import com.example.financeapp.data.local.database.entities.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity)

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE userId = :userId OR userId = 'system' ORDER BY isMainCategory DESC, name ASC")
    fun getCategoriesByUser(userId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type AND (userId = :userId OR userId = 'system')")
    fun getCategoriesByType(userId: String, type: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE isMainCategory = 1 AND (userId = :userId OR userId = 'system')")
    fun getMainCategories(userId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE parentCategoryId = :parentId AND (userId = :userId OR userId = 'system')")
    fun getSubCategories(userId: String, parentId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE isSynced = 0")
    suspend fun getUnsyncedCategories(): List<CategoryEntity>

    @Query("DELETE FROM categories WHERE userId = :userId")
    suspend fun deleteAllByUser(userId: String)
}