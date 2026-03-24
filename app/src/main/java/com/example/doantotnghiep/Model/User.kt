package com.example.doantotnghiep.Model

data class User(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val avatarUrl: String = "",
    val role: String = "tenant",
    val isVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)