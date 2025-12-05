package com.example.financeapp.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.financeapp.data.models.User

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val email: String,
    val profileImage: String? = null,
    val lastSyncTime: Long = System.currentTimeMillis()
) {
    fun toUser(): User {
        return User(
            id = id,
            name = name,
            email = email,
            profileImage = profileImage
        )
    }

    companion object {
        fun fromUser(user: User): UserEntity {
            return UserEntity(
                id = user.id,
                name = user.name,
                email = user.email,
                profileImage = user.profileImage
            )
        }
    }
}