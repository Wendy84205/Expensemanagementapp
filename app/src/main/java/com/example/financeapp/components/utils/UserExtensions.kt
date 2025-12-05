package com.example.financeapp.components.utils

import com.example.financeapp.data.models.User
import com.google.firebase.auth.FirebaseUser

fun FirebaseUser.toAppUser(): User {
    return User(
        id = uid,
        name = displayName ?: email?.substringBefore("@") ?: "Người dùng",
        email = email ?: "",
        profileImage = photoUrl?.toString()
    )
}