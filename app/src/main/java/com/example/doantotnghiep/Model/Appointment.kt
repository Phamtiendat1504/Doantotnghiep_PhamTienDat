package com.example.doantotnghiep.Model

data class StatusChange(
    val fromStatus: String = "",
    val toStatus: String = "",
    val changedBy: String = "",     // "tenant" | "landlord" | "system"
    val changedById: String = "",
    val reason: String = "",
    val timestamp: Long = 0L
) {
    fun toMap(): Map<String, Any> = mapOf(
        "fromStatus" to fromStatus, "toStatus" to toStatus,
        "changedBy" to changedBy, "changedById" to changedById,
        "reason" to reason, "timestamp" to timestamp
    )
}

data class Appointment(
    val id: String = "",
    val roomId: String = "",
    val roomTitle: String = "",
    val roomAddress: String = "",
    val roomImageUrl: String = "",
    val postExpiryDate: Long = 0L,

    val tenantId: String = "",
    val tenantName: String = "",
    val tenantPhone: String = "",
    val tenantGender: String = "",
    val landlordId: String = "",
    val landlordName: String = "",
    val landlordPhone: String = "",

    val appointmentDate: String = "",           // "dd/MM/yyyy"
    val appointmentDateMs: Long = 0L,           // Midnight ngày hẹn (ms)
    val appointmentTimestampMs: Long = 0L,      // Timestamp chính xác ngày + giờ (ms)
    val appointmentTime: String = "",           // "HH:mm"
    val appointmentDateDisplay: String = "",    // "Thứ Tư, 10/06/2026"

    val status: String = "pending",
    // Statuses: pending | confirmed | tenant_confirmed | rejected | cancelled |
    //           cancelled_by_landlord | expired_pending | no_show | completed | rented
    val statusHistory: List<StatusChange> = emptyList(),
    val rejectReason: String = "",
    val cancelReason: String = "",

    val landlordConfirmDeadline: Long = 0L,     // createdAt + 48h
    val tenantConfirmDeadline: Long = 0L,       // appointmentTimestampMs - 1h

    val note: String = "",
    val editCount: Int = 0,                     // Số lần đổi lịch, tối đa 3

    // Flags nhắc landlord khi pending (+12h, +36h, +47h)
    val landlordRemind12hSent: Boolean = false,
    val landlordRemind36hSent: Boolean = false,
    val landlordRemind47hSent: Boolean = false,

    // Flags nhắc tenant khi appointment đến gần (T-24h, T-2h, T-30m, T=0)
    val reminder24hSent: Boolean = false,
    val reminder2hSent: Boolean = false,
    val reminder30mSent: Boolean = false,
    val reminder0hSent: Boolean = false,

    // Flags nhắc landlord khi appointment đến gần (T-24h, T-2h, T-30m, T=0)
    val landlordReminder24hSent: Boolean = false,
    val landlordReminder2hSent: Boolean = false,
    val landlordReminder30mSent: Boolean = false,
    val landlordReminder0hSent: Boolean = false,

    val resultAskedSent: Boolean = false,       // Đã nhắc landlord cập nhật kết quả
    val autoNoShowSent: Boolean = false,        // Auto no_show sau 24h không cập nhật

    val hasUnreadUpdate: Boolean = false,
    val lastNotifiedAt: Long = 0L,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    fun toMap(): Map<String, Any> = buildMap {
        put("roomId", roomId); put("roomTitle", roomTitle)
        put("roomAddress", roomAddress); put("roomImageUrl", roomImageUrl)
        put("postExpiryDate", postExpiryDate)
        put("tenantId", tenantId); put("tenantName", tenantName)
        put("tenantPhone", tenantPhone); put("tenantGender", tenantGender)
        put("landlordId", landlordId); put("landlordName", landlordName)
        put("landlordPhone", landlordPhone)
        put("appointmentDate", appointmentDate)
        put("appointmentDateMs", appointmentDateMs)
        put("appointmentTimestampMs", appointmentTimestampMs)
        put("appointmentTime", appointmentTime)
        put("appointmentDateDisplay", appointmentDateDisplay)
        put("status", status)
        put("statusHistory", statusHistory.map { it.toMap() })
        put("rejectReason", rejectReason); put("cancelReason", cancelReason)
        put("landlordConfirmDeadline", landlordConfirmDeadline)
        put("tenantConfirmDeadline", tenantConfirmDeadline)
        put("note", note)
        put("editCount", editCount)
        put("landlordRemind12hSent", landlordRemind12hSent)
        put("landlordRemind36hSent", landlordRemind36hSent)
        put("landlordRemind47hSent", landlordRemind47hSent)
        put("reminder24hSent", reminder24hSent)
        put("reminder2hSent", reminder2hSent)
        put("reminder30mSent", reminder30mSent)
        put("reminder0hSent", reminder0hSent)
        put("landlordReminder24hSent", landlordReminder24hSent)
        put("landlordReminder2hSent", landlordReminder2hSent)
        put("landlordReminder30mSent", landlordReminder30mSent)
        put("landlordReminder0hSent", landlordReminder0hSent)
        put("resultAskedSent", resultAskedSent)
        put("autoNoShowSent", autoNoShowSent)
        put("hasUnreadUpdate", hasUnreadUpdate)
        put("lastNotifiedAt", lastNotifiedAt)
        put("createdAt", createdAt); put("updatedAt", updatedAt)
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(id: String, map: Map<String, Any>): Appointment {
            val historyRaw = map["statusHistory"] as? List<Map<String, Any>> ?: emptyList()
            val history = historyRaw.map { sc ->
                StatusChange(
                    fromStatus = sc["fromStatus"] as? String ?: "",
                    toStatus = sc["toStatus"] as? String ?: "",
                    changedBy = sc["changedBy"] as? String ?: "",
                    changedById = sc["changedById"] as? String ?: "",
                    reason = sc["reason"] as? String ?: "",
                    timestamp = (sc["timestamp"] as? Long)
                        ?: (sc["timestamp"] as? Number)?.toLong() ?: 0L
                )
            }
            return Appointment(
                id = id,
                roomId = map["roomId"] as? String ?: "",
                roomTitle = map["roomTitle"] as? String ?: "",
                roomAddress = map["roomAddress"] as? String ?: "",
                roomImageUrl = map["roomImageUrl"] as? String ?: "",
                postExpiryDate = (map["postExpiryDate"] as? Long)
                    ?: (map["postExpiryDate"] as? Number)?.toLong() ?: 0L,
                tenantId = map["tenantId"] as? String ?: "",
                tenantName = map["tenantName"] as? String ?: "",
                tenantPhone = map["tenantPhone"] as? String ?: "",
                tenantGender = map["tenantGender"] as? String ?: "",
                landlordId = map["landlordId"] as? String ?: "",
                landlordName = map["landlordName"] as? String ?: "",
                landlordPhone = map["landlordPhone"] as? String ?: "",
                appointmentDate = (map["appointmentDate"] ?: map["date"]) as? String ?: "",
                appointmentDateMs = (map["appointmentDateMs"] as? Long)
                    ?: (map["appointmentDateMs"] as? Number)?.toLong() ?: 0L,
                appointmentTimestampMs = (map["appointmentTimestampMs"] as? Long)
                    ?: (map["appointmentTimestampMs"] as? Number)?.toLong() ?: 0L,
                appointmentTime = (map["appointmentTime"] ?: map["time"]) as? String ?: "",
                appointmentDateDisplay = (map["appointmentDateDisplay"] ?: map["dateDisplay"]) as? String ?: "",
                status = map["status"] as? String ?: "pending",
                statusHistory = history,
                rejectReason = map["rejectReason"] as? String ?: "",
                cancelReason = map["cancelReason"] as? String ?: "",
                landlordConfirmDeadline = (map["landlordConfirmDeadline"] as? Long)
                    ?: (map["landlordConfirmDeadline"] as? Number)?.toLong() ?: 0L,
                tenantConfirmDeadline = (map["tenantConfirmDeadline"] as? Long)
                    ?: (map["tenantConfirmDeadline"] as? Number)?.toLong() ?: 0L,
                note = map["note"] as? String ?: "",
                editCount = (map["editCount"] as? Long)?.toInt()
                    ?: (map["editCount"] as? Number)?.toInt() ?: 0,
                // New flags — fallback to old names for any existing test data
                landlordRemind12hSent = (map["landlordRemind12hSent"] ?: map["landlordRemind1Sent"]) as? Boolean ?: false,
                landlordRemind36hSent = (map["landlordRemind36hSent"] ?: map["landlordRemind2Sent"]) as? Boolean ?: false,
                landlordRemind47hSent = (map["landlordRemind47hSent"] ?: map["landlordRemindFinalSent"]) as? Boolean ?: false,
                reminder24hSent = map["reminder24hSent"] as? Boolean ?: false,
                reminder2hSent = map["reminder2hSent"] as? Boolean ?: false,
                reminder30mSent = map["reminder30mSent"] as? Boolean ?: false,
                reminder0hSent = map["reminder0hSent"] as? Boolean ?: false,
                landlordReminder24hSent = map["landlordReminder24hSent"] as? Boolean ?: false,
                landlordReminder2hSent = map["landlordReminder2hSent"] as? Boolean ?: false,
                landlordReminder30mSent = map["landlordReminder30mSent"] as? Boolean ?: false,
                landlordReminder0hSent = map["landlordReminder0hSent"] as? Boolean ?: false,
                resultAskedSent = map["resultAskedSent"] as? Boolean ?: false,
                autoNoShowSent = map["autoNoShowSent"] as? Boolean ?: false,
                hasUnreadUpdate = map["hasUnreadUpdate"] as? Boolean ?: false,
                lastNotifiedAt = (map["lastNotifiedAt"] as? Long)
                    ?: (map["lastNotifiedAt"] as? Number)?.toLong() ?: 0L,
                createdAt = (map["createdAt"] as? Long)
                    ?: (map["createdAt"] as? Number)?.toLong() ?: 0L,
                updatedAt = (map["updatedAt"] as? Long)
                    ?: (map["updatedAt"] as? Number)?.toLong() ?: 0L
            )
        }
    }
}
