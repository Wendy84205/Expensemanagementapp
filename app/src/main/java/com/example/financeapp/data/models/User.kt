package com.example.financeapp.data.models

// User.kt
data class User(
    val id: String,
    val name: String,
    val email: String,
    val profileImage: String? = null
)