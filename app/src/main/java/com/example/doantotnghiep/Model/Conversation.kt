package com.example.doantotnghiep.Model

data class Conversation(
    val chatId: String = "",
    val participants: List<String> = listOf(),
    val participantNames: Map<String, String> = mapOf(),
    val participantAvatars: Map<String, String> = mapOf(),
    val lastMessage: String = "",
    val lastMessageAt: Long = 0L,
    val lastSenderId: String = "",
    val unreadCount: Map<String, Long> = mapOf()
)
