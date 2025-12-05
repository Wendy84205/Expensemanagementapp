package com.example.financeapp.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.financeapp.viewmodel.transaction.Category

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String, // "expense" hoặc "income"
    val isMainCategory: Boolean = false,
    val parentCategoryId: String? = null,
    val icon: String = "🍹",
    val color: String = "#FF69B4",
    val userId: String = "", // Có thể là "system" cho mặc định
    val isSynced: Boolean = false
) {
    fun toCategory(): Category {
        return Category(
            id = id,
            name = name,
            type = type,
            isMainCategory = isMainCategory,
            parentCategoryId = parentCategoryId,
            icon = icon,
            color = color
        )
    }

    companion object {
        fun fromCategory(category: Category, userId: String = "system"): CategoryEntity {
            return CategoryEntity(
                id = category.id,
                name = category.name,
                type = category.type,
                isMainCategory = category.isMainCategory,
                parentCategoryId = category.parentCategoryId,
                icon = category.icon,
                color = category.color,
                userId = userId,
                isSynced = false
            )
        }
    }
}