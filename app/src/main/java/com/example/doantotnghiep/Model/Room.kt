package com.example.doantotnghiep.Model

data class Room(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val price: Long = 0,
    val area: Int = 0,
    val address: String = "",
    val ward: String = "",
    val district: String = "",
    val roomType: String = "",
    val gender: String = "",
    val peopleCount: Int = 0,
    val hasWifi: Boolean = false,
    val hasAirCon: Boolean = false,
    val hasWaterHeater: Boolean = false,
    val hasParking: Boolean = false,
    val wifiPrice: Long = 0,
    val electricPrice: Long = 0,
    val waterPrice: Long = 0,
    val curfew: String = "",
    val imageUrls: List<String> = listOf(),
    val ownerName: String = "",
    val ownerPhone: String = "",
    val isFeatured: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)