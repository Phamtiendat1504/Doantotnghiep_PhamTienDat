package com.example.doantotnghiep.Model

data class SupportTicket(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val category: String = "",
    val title: String = "",
    val status: String = "new",
    val priority: String = "normal",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val lastMessage: String = "",
    val lastSenderRole: String = "",
    val adminId: String = "",
    val adminName: String = "",
    val unreadForUser: Boolean = false,
    val unreadForAdmin: Boolean = false
)
