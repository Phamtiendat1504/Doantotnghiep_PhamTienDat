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
    // 1. THÔNG TIN CHUNG CỦA LỊCH HẸN
    val id: String = "",                    // Mã ID duy nhất của lịch hẹn này
    val roomId: String = "",                // Mã ID của phòng trọ
    val roomTitle: String = "",             // Tên/Tiêu đề của phòng trọ (vd: Phòng trọ giá rẻ sinh viên)
    val roomAddress: String = "",           // Địa chỉ phòng trọ
    val roomImageUrl: String = "",          // Link ảnh đại diện của phòng
    val postExpiryDate: Long = 0L,          // Hạn sử dụng của bài đăng phòng (tránh việc phòng đã gỡ mà vẫn hẹn)

    // 2. THÔNG TIN NGƯỜI THUÊ (Khách)
    val tenantId: String = "",              // Mã ID của người đi thuê
    val tenantName: String = "",            // Tên người đi thuê
    val tenantPhone: String = "",           // Số điện thoại người đi thuê
    val tenantGender: String = "",          // Giới tính người đi thuê

    // 3. THÔNG TIN CHỦ TRỌ
    val landlordId: String = "",            // Mã ID của chủ trọ
    val landlordName: String = "",          // Tên chủ trọ
    val landlordPhone: String = "",         // Số điện thoại chủ trọ

    // 4. THỜI GIAN HẸN XEM PHÒNG
    val appointmentDate: String = "",           // Ngày hẹn dạng chữ (VD: "20/10/2026")
    val appointmentDateMs: Long = 0L,           // Mốc thời gian (0h sáng) của ngày hẹn (tính bằng mili-giây)
    val appointmentTimestampMs: Long = 0L,      // Mốc thời gian chính xác của Giờ + Ngày hẹn (dùng để đếm ngược)
    val appointmentTime: String = "",           // Giờ hẹn (VD: "08:30")
    val appointmentDateDisplay: String = "",    // Ngày hẹn hiển thị đẹp (VD: "Thứ Ba, 20/10/2026")

    // 5. TRẠNG THÁI & LỊCH SỬ
    val status: String = "pending",             // Trạng thái hiện tại (đang chờ duyệt, đã duyệt, đã hủy,...)
    // Statuses: pending | confirmed | tenant_confirmed | rejected | cancelled |
    //           cancelled_by_landlord | expired_pending | no_show | completed | rented
    val statusHistory: List<StatusChange> = emptyList(), // Danh sách lưu lại toàn bộ lịch sử các lần đổi trạng thái
    val rejectReason: String = "",              // Lý do chủ trọ từ chối lịch hẹn (nếu có)
    val cancelReason: String = "",              // Lý do khách hàng tự hủy lịch hẹn (nếu có)

    // 6. HẠN CHÓT XÁC NHẬN
    val landlordConfirmDeadline: Long = 0L,     // Hạn chót chủ trọ phải duyệt (= appointmentTimestampMs, tức là đúng giờ hẹn)
    val tenantConfirmDeadline: Long = 0L,       // Hạn chót khách phải xác nhận sẽ đến (thường là trước giờ hẹn 1 tiếng)

    // 7. THÔNG TIN BỔ SUNG
    val note: String = "",                      // Lời nhắn, ghi chú của khách gửi cho chủ trọ
    val editCount: Int = 0,                     // Số lần khách đã sửa lịch hẹn (giới hạn tối đa 3 lần để chống spam)

    // 8. CÁC CỜ (FLAGS) ĐÁNH DẤU ĐÃ GỬI THÔNG BÁO CHO CHỦ TRỌ
    // (Dùng để Server biết là đã gửi tin nhắn nhắc nhở rồi, không gửi lặp lại nữa)
    val landlordRemind1hPendingSent: Boolean = false, // Đã nhắc chủ trọ duyệt lịch (trước 1 tiếng so với giờ hẹn)

    // 9. CÁC CỜ ĐÁNH DẤU ĐÃ NHẮC LỊCH SẮP ĐẾN GIỜ
    val reminder24hSent: Boolean = false,           // Đã nhắc khách: "Còn 24 tiếng nữa đến giờ xem phòng"
    val reminder2hSent: Boolean = false,            // Đã nhắc khách: "Còn 2 tiếng"
    val reminder30mSent: Boolean = false,           // Đã nhắc khách: "Còn 30 phút"
    val reminder0hSent: Boolean = false,            // Đã báo khách: "Đến giờ rồi!"

    val landlordReminder24hSent: Boolean = false,   // Đã nhắc chủ trọ: "Còn 24 tiếng nữa khách đến"

    val landlordReminder30mSent: Boolean = false,   // Đã nhắc chủ trọ: "Còn 30 phút"
    val landlordReminder0hSent: Boolean = false,    // Đã báo chủ trọ: "Khách đang đến!"

    // 10. CÁC CỜ SAU KHI LỊCH HẸN KẾT THÚC
    val resultAskedSent: Boolean = false,       // Đã nhắn tin hỏi chủ trọ: "Khách có đến không?"
    val autoNoShowSent: Boolean = false,        // Đã tự động phạt khách (vì chủ trọ lười không trả lời)

    // 11. THÔNG TIN HỆ THỐNG
    val hasUnreadUpdate: Boolean = false,       // Có cập nhật mới mà người dùng chưa đọc (để hiện chấm đỏ)
    val lastNotifiedAt: Long = 0L,              // Lần gửi thông báo gần nhất
    val createdAt: Long = 0L,                   // Thời điểm tạo lịch hẹn này
    val updatedAt: Long = 0L                    // Thời điểm có chỉnh sửa gần nhất
) {
    // hàm toMap() dùng để parse (chuyển đổi) đối tượng Appointment từ Kotlin sang kiểu dữ liệu Map (Key-Value) tiêu chuẩn
    // giúp cho việc đẩy dữ liệu lên Firebase Firestore được chính xác và không bị lỗi kiểu dữ liệu
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
        put("landlordRemind1hPendingSent", landlordRemind1hPendingSent)
        put("reminder24hSent", reminder24hSent)
        put("reminder2hSent", reminder2hSent)
        put("reminder30mSent", reminder30mSent)
        put("reminder0hSent", reminder0hSent)
        put("landlordReminder24hSent", landlordReminder24hSent)

        put("landlordReminder30mSent", landlordReminder30mSent)
        put("landlordReminder0hSent", landlordReminder0hSent)
        put("resultAskedSent", resultAskedSent)
        put("autoNoShowSent", autoNoShowSent)
        put("hasUnreadUpdate", hasUnreadUpdate)
        put("lastNotifiedAt", lastNotifiedAt)
        put("createdAt", createdAt); put("updatedAt", updatedAt)
    }

    // companion object trong Kotlin đóng vai trò giống như các hàm static trong Java
    // hàm fromMap(). Hàm này là một Factory Method, làm nhiệm vụ nhận dữ liệu thô dạng Map (Key-Value) tải từ Firebase về
    // sau đó parse và khởi tạo ra một đối tượng Appointment hoàn chỉnh mà không cần phải new (khởi tạo) class Appointment từ trước
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
                landlordRemind1hPendingSent = map["landlordRemind1hPendingSent"] as? Boolean ?: false,
                reminder24hSent = map["reminder24hSent"] as? Boolean ?: false,
                reminder2hSent = map["reminder2hSent"] as? Boolean ?: false,
                reminder30mSent = map["reminder30mSent"] as? Boolean ?: false,
                reminder0hSent = map["reminder0hSent"] as? Boolean ?: false,
                landlordReminder24hSent = map["landlordReminder24hSent"] as? Boolean ?: false,
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
