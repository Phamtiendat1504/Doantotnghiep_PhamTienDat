package com.example.doantotnghiep.Model

data class AIRoom(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val price: Long = 0,
    val area: Int = 0,
    val district: String = "",
    val imageUrl: String = ""
)

data class AIMessage(
    val role: String = "", // "user" hoặc "model" hoặc "typing"
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val suggestedRooms: List<AIRoom> = emptyList()
)