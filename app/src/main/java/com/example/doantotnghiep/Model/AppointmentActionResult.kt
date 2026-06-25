package com.example.doantotnghiep.Model

sealed class AppointmentActionResult {
    object Success : AppointmentActionResult()
    data class Failure(val message: String) : AppointmentActionResult()
}
