package com.example.financeapp.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.financeapp.data.models.UserProfile

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val monthlyIncome: Long = 0,
    val financialGoals: String = "", // Lưu dạng JSON string
    val currency: String = "VNĐ",
    val joinDate: Long = System.currentTimeMillis(),
    val photoUrl: String = "",
    val phoneNumber: String = "",
    val dateOfBirth: Long = 0,
    val occupation: String = "",
    val monthlyExpense: Long = 0,
    val riskTolerance: String = "MEDIUM",
    val isSynced: Boolean = false
) {
    fun toUserProfile(): UserProfile {
        return UserProfile(
            userId = userId,
            name = name,
            email = email,
            monthlyIncome = monthlyIncome,
            financialGoals = financialGoals.split(",").filter { it.isNotBlank() },
            currency = currency,
            joinDate = joinDate,
            photoUrl = photoUrl,
            phoneNumber = phoneNumber,
            dateOfBirth = dateOfBirth,
            occupation = occupation,
            monthlyExpense = monthlyExpense,
            riskTolerance = riskTolerance
        )
    }

    companion object {
        fun fromUserProfile(profile: UserProfile): UserProfileEntity {
            return UserProfileEntity(
                userId = profile.userId,
                name = profile.name,
                email = profile.email,
                monthlyIncome = profile.monthlyIncome,
                financialGoals = profile.financialGoals.joinToString(","),
                currency = profile.currency,
                joinDate = profile.joinDate,
                photoUrl = profile.photoUrl,
                phoneNumber = profile.phoneNumber,
                dateOfBirth = profile.dateOfBirth,
                occupation = profile.occupation,
                monthlyExpense = profile.monthlyExpense,
                riskTolerance = profile.riskTolerance,
                isSynced = false
            )
        }
    }
}