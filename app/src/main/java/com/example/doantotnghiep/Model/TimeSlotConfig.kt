package com.example.doantotnghiep.Model

data class TimeRange(
    val startHour: Int = 8,
    val startMinute: Int = 0,
    val endHour: Int = 18,
    val endMinute: Int = 0
) {
    fun toMap(): Map<String, Any> = mapOf(
        "startHour" to startHour, "startMinute" to startMinute,
        "endHour" to endHour, "endMinute" to endMinute
    )
    companion object {
        fun fromMap(map: Map<String, Any>): TimeRange = TimeRange(
            startHour = (map["startHour"] as? Long)?.toInt() ?: 8,
            startMinute = (map["startMinute"] as? Long)?.toInt() ?: 0,
            endHour = (map["endHour"] as? Long)?.toInt() ?: 18,
            endMinute = (map["endMinute"] as? Long)?.toInt() ?: 0
        )
    }
}

data class TimeSlotConfig(
    val dayOfWeek: Int = 2,       // 2=T2, 3=T3, 4=T4, 5=T5, 6=T6, 7=T7, 1=CN (Calendar.DAY_OF_WEEK)
    val dayLabel: String = "",
    val isEnabled: Boolean = false,
    val timeRanges: List<TimeRange> = emptyList()
) {
    // Sinh slot 30 phút từ tất cả timeRanges của ngày này
    fun generateSlots(): List<String> {
        val slots = mutableListOf<String>()
        for (range in timeRanges) {
            var h = range.startHour
            var m = range.startMinute
            val endTotal = range.endHour * 60 + range.endMinute
            while (h * 60 + m + 30 <= endTotal) {
                slots.add("%02d:%02d".format(h, m))
                m += 30
                if (m >= 60) { h++; m -= 60 }
            }
        }
        return slots
    }

    fun toMap(): Map<String, Any> = mapOf(
        "dayOfWeek" to dayOfWeek,
        "dayLabel" to dayLabel,
        "isEnabled" to isEnabled,
        "timeRanges" to timeRanges.map { it.toMap() }
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any>): TimeSlotConfig {
            val ranges = (map["timeRanges"] as? List<Map<String, Any>> ?: emptyList())
                .map { TimeRange.fromMap(it) }
            return TimeSlotConfig(
                dayOfWeek = (map["dayOfWeek"] as? Long)?.toInt() ?: 2,
                dayLabel = map["dayLabel"] as? String ?: "",
                isEnabled = map["isEnabled"] as? Boolean ?: false,
                timeRanges = ranges
            )
        }

        // Mặc định T2-T7 08:00-12:00 và 14:00-18:00
        fun defaults(): List<TimeSlotConfig> {
            val defaultRanges = listOf(TimeRange(8, 0, 12, 0), TimeRange(14, 0, 18, 0))
            return listOf(
                TimeSlotConfig(2, "Thứ Hai", true, defaultRanges),
                TimeSlotConfig(3, "Thứ Ba", true, defaultRanges),
                TimeSlotConfig(4, "Thứ Tư", true, defaultRanges),
                TimeSlotConfig(5, "Thứ Năm", true, defaultRanges),
                TimeSlotConfig(6, "Thứ Sáu", true, defaultRanges),
                TimeSlotConfig(7, "Thứ Bảy", true, listOf(TimeRange(9, 0, 12, 0))),
                TimeSlotConfig(1, "Chủ Nhật", false, emptyList())
            )
        }
    }
}
