package com.example.financeapp.data.models

data class User(
    val id: String,
    val name: String,
    val email: String,
    val profileImage: String? = null,
    val phoneNumber: String? = null,          // THÊM
    val providerId: String? = null,           // THÊM: "google", "facebook", "email"
    val createdAt: Long? = null,              // THÊM
    val updatedAt: Long? = null               // THÊM
)