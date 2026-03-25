package com.example.doantotnghiep.Model

data class User(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val birthday: String = "",
    val gender: String = "",
    val occupation: String = "",
    val avatarUrl: String = "",
    val role: String = "tenant",
    val isVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)