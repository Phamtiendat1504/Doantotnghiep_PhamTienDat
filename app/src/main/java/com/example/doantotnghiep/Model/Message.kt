package com.example.doantotnghiep.Model

data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val createdAt: Long = 0L,
    val seen: Boolean = false,
    // Map<userId, emoji> — mỗi người chỉ thả 1 reaction
    val reactions: Map<String, String> = mapOf()
)
