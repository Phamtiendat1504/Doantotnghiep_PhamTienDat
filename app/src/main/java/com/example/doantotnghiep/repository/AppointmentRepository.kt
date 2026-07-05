package com.example.doantotnghiep.repository

import com.example.doantotnghiep.Model.Appointment
import com.example.doantotnghiep.Model.Room
import com.example.doantotnghiep.Model.StatusChange
import com.example.doantotnghiep.Model.TimeRange
import com.example.doantotnghiep.Model.TimeSlotConfig
import com.example.doantotnghiep.Model.User
import com.example.doantotnghiep.Utils.AppointmentConstants
import com.example.doantotnghiep.Utils.toUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration

data class RoomRentedNotice(val id: String, val title: String, val message: String)

class AppointmentRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    val currentUid: String? get() = auth.currentUser?.uid

    private fun buildSlotId(roomId: String, date: String, time: String): String =
        "${roomId}_${date}_${time}".replace("/", "-").replace(":", "-").replace(" ", "_")

    // 3. Nhóm hàm: THỰC THI (Chốt hạ)
    /**
     * Hàm chốt hạ: Đóng gói toàn bộ dữ liệu lịch hẹn hợp lệ và gửi thẳng lên Server.
     * (Sử dụng Cloud Functions để ẩn logic xử lý, lưu chính thức bản ghi vào Database của Firebase).
     */
    // Đặt lịch hẹn -3-
    // [ĐẶT LỊCH] Gửi dữ liệu lên Cloud Functions để lưu, đảm bảo bảo mật & chống trùng lịch.
    fun submitBooking(
        appointment: Appointment,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: run {
            onFailure("Bạn chưa đăng nhập. Vui lòng đăng nhập lại.")
            return
        }

        val payload = hashMapOf(
            "roomId" to appointment.roomId,
            "roomTitle" to appointment.roomTitle,
            "roomAddress" to appointment.roomAddress,
            "roomImageUrl" to appointment.roomImageUrl,
            "postExpiryDate" to appointment.postExpiryDate,
            "tenantId" to uid,
            "tenantName" to appointment.tenantName,
            "tenantPhone" to appointment.tenantPhone,
            "tenantGender" to appointment.tenantGender,
            "landlordId" to appointment.landlordId,
            "landlordName" to appointment.landlordName,
            "landlordPhone" to appointment.landlordPhone,
            "appointmentDate" to appointment.appointmentDate,
            "appointmentDateMs" to appointment.appointmentDateMs,
            "appointmentTimestampMs" to appointment.appointmentTimestampMs,
            "appointmentTime" to appointment.appointmentTime,
            "appointmentDateDisplay" to appointment.appointmentDateDisplay,
            "note" to appointment.note,
            "tenantConfirmDeadline" to appointment.tenantConfirmDeadline
        )

        com.google.firebase.functions.FirebaseFunctions.getInstance("asia-southeast1")
            .getHttpsCallable("serverSubmitBooking")
            .call(payload)
            .addOnSuccessListener {
                sendNotification(
                    userId = appointment.landlordId,
                    title = "Có lịch hẹn mới!",
                    message = "${appointment.tenantName} muốn xem phòng \"${appointment.roomTitle}\" vào ${appointment.appointmentDateDisplay} lúc ${appointment.appointmentTime}",
                    type = "appointment_new"
                )
                onSuccess()
            }
            .addOnFailureListener { e ->
                val msg = e.message?.takeIf { it.isNotBlank() }
                    ?: "Lỗi đặt lịch. Vui lòng thử lại."
                onFailure(msg)
            }
    }

    /**
     * Thuật toán bóc tách dữ liệu chuỗi (String Manipulation).
     * Nhiệm vụ: Phân tích chuỗi văn bản lưu lịch rảnh thô ráp từ Firebase (vd: "T2:08:00-11:00;T3...")
     * và chuyển đổi nó thành danh sách các Đối tượng (Object) Thời gian có cấu trúc chuẩn,
     * giúp giao diện App dễ dàng đọc hiểu và chia nhỏ thành các khung 30 phút.
     */
    // Đặt lịch hẹn -2-
    // [THUẬT TOÁN ĐẶT LỊCH] Dịch chuỗi Text (Thứ 2:08:00-11:00) thành các ô thời gian 30 phút.
    private fun parseTimeSlotsString(str: String): List<TimeSlotConfig> {
        if (str.isBlank()) return TimeSlotConfig.defaults()
        val lines = str.lines()
        val dayLabelToDoW = mapOf(
            "Thứ 2" to 2, "Thứ 3" to 3, "Thứ 4" to 4,
            "Thứ 5" to 5, "Thứ 6" to 6, "Thứ 7" to 7, "Chủ nhật" to 1
        )
        fun parseRange(prefix: String): TimeRange? {
            val line = lines.find { it.startsWith(prefix) }?.removePrefix(prefix)?.trim() ?: return null
            val parts = line.split("-")
            if (parts.size < 2) return null
            val s = parts[0].trim().split(":")
            val e = parts[1].trim().split(":")
            if (s.size != 2 || e.size != 2) return null
            return TimeRange(
                startHour = s[0].toIntOrNull() ?: return null,
                startMinute = s[1].toIntOrNull() ?: return null,
                endHour = e[0].toIntOrNull() ?: return null,
                endMinute = e[1].toIntOrNull() ?: return null
            )
        }
        val daysLine = lines.find { it.startsWith("Ngày:") }
        if (daysLine != null) {
            // New format: "Ngày: Thứ 2, Thứ 3\nBuổi sáng: 08:00-12:00\n..."
            val dayNames = daysLine.removePrefix("Ngày:").trim().split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val ranges = listOfNotNull(
                parseRange("Buổi sáng:"),
                parseRange("Buổi trưa:"),
                parseRange("Buổi chiều/tối:")
            )
            if (dayNames.isEmpty() || ranges.isEmpty()) return TimeSlotConfig.defaults()
            return dayNames.mapNotNull { name ->
                val dow = dayLabelToDoW[name] ?: return@mapNotNull null
                TimeSlotConfig(dayOfWeek = dow, dayLabel = name, isEnabled = true, timeRanges = ranges)
            }
        } else {
            // Old format: "Thứ 2: 08:00-17:00\nThứ 6: 09:00-21:00"
            return lines.mapNotNull { line ->
                val parts = line.split(": ")
                if (parts.size != 2) return@mapNotNull null
                val name = parts[0].trim()
                val dow = dayLabelToDoW[name] ?: return@mapNotNull null
                val times = parts[1].trim().split("-")
                if (times.size != 2) return@mapNotNull null
                val s = times[0].trim().split(":")
                val e = times[1].trim().split(":")
                if (s.size != 2 || e.size != 2) return@mapNotNull null
                TimeSlotConfig(
                    dayOfWeek = dow, dayLabel = name, isEnabled = true,
                    timeRanges = listOf(TimeRange(
                        startHour = s[0].toIntOrNull() ?: return@mapNotNull null,
                        startMinute = s[1].toIntOrNull() ?: return@mapNotNull null,
                        endHour = e[0].toIntOrNull() ?: return@mapNotNull null,
                        endMinute = e[1].toIntOrNull() ?: return@mapNotNull null
                    ))
                )
            }.ifEmpty { TimeSlotConfig.defaults() }
        }
    }

    // 1. Nhóm hàm: NẠP DỮ LIỆU HIỂN THỊ
    /**
     * Lấy thông tin chi tiết của phòng và cấu hình lịch rảnh của chủ trọ từ Firebase.
     * (Dùng để có dữ liệu vẽ ra tờ lịch và các khung giờ rảnh cho khách chọn).
     */
    // Đặt lịch hẹn -1-
    // [ĐẶT LỊCH] Tải thông tin phòng và cấu hình giờ rảnh của chủ trọ để vẽ màn hình.
    fun fetchRoomForBooking(roomId: String, onSuccess: (Room, List<TimeSlotConfig>) -> Unit, onFailure: (String) -> Unit) {
        db.collection("rooms").document(roomId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) { onFailure("Không tìm thấy phòng"); return@addOnSuccessListener }
                val slotsStr = doc.getString("availableTimeSlots") ?: ""
                val timeSlots = parseTimeSlotsString(slotsStr)
                val room = Room(
                    id = doc.id,                                // Lấy mã ID duy nhất của phòng (chính là tên Document trên Firebase)
                    userId = doc.getString("userId") ?: "",     // Lấy mã UID của chủ trọ (người đăng bài). Nếu trên mạng bị null thì gán tạm là chuỗi rỗng ("") để tránh lỗi
                    title = doc.getString("title") ?: "",       // Lấy tiêu đề bài đăng phòng
                    address = doc.getString("address") ?: "",   // Lấy địa chỉ cụ thể của phòng trọ
                    price = doc.getLong("price") ?: 0L,         // Lấy giá tiền thuê phòng (Kiểu số nguyên lớn Long). Mặc định nếu lỗi là 0 đồng
                    imageUrls = (doc.get("imageUrls") as? List<*>)?.filterIsInstance<String>() ?: listOf(), // Ép kiểu an toàn: Ép mảng hình ảnh trên mạng về chuẩn danh sách chuỗi (List<String>) trong Kotlin
                    status = doc.getString("status") ?: "pending", // Lấy trạng thái duyệt bài của admin. Mặc định là "pending" (đang chờ duyệt)
                    postExpiryDate = doc.getLong("postExpiryDate") ?: 0L, // Lấy mốc thời gian bài đăng hết hạn (tính bằng Mili-giây)
                    postDurationDays = doc.getLong("postDurationDays")?.toInt() ?: 30, // Gói đăng bài trong bao nhiêu ngày. Mặc định nếu không có là 30 ngày
                    availableTimeSlots = slotsStr,              // Gắn chuỗi cấu hình lịch rảnh (đã lấy và xử lý ở phía trên) vào đây
                    maxDailyAppointments = doc.getLong("maxDailyAppointments")?.toInt() ?: 10, // Giới hạn phòng này tiếp tối đa bao nhiêu khách/ngày. Mặc định là 10
                    appointmentNotice = doc.getString("appointmentNotice") ?: "", // Lời nhắn dặn dò của chủ trọ khi khách đến xem phòng
                    ownerName = doc.getString("ownerName") ?: "",   // Lấy tên hiển thị của chủ trọ
                    ownerPhone = doc.getString("ownerPhone") ?: "", // Lấy số điện thoại liên hệ của chủ trọ
                    ownerGender = doc.getString("ownerGender") ?: ""// Lấy giới tính của chủ trọ (để khách biết đường xưng hô)
                )
                onSuccess(room, timeSlots)
            }
            .addOnFailureListener { e -> onFailure("Lỗi tải phòng: ${e.message}") }
    }

    /**
     * Lắng nghe theo thời gian thực (Real-time) các khung giờ đã bị người khác đặt mất.
     * (Hễ có người đặt xong, hàm này lập tức báo về để Giao diện bôi xám/khóa cái khung giờ đó lại).
     */
    // Đặt lịch hẹn -4-
    // [ĐẶT LỊCH] Lắng nghe Real-time các khung giờ đã bị người khác đặt để bôi xám nút.
    fun listenBookedSlotsForRoom(roomId: String, onUpdate: (Set<String>) -> Unit): ListenerRegistration {
        return db.collection("bookedSlots")
            .whereEqualTo("roomId", roomId)
            .addSnapshotListener { snap, _ ->
                val bookedKeys = mutableSetOf<String>()
                snap?.forEach { doc ->
                    val date = doc.getString("date") ?: ""
                    val time = doc.getString("time") ?: ""
                    if (date.isNotEmpty() && time.isNotEmpty()) {
                        bookedKeys.add("${date}_${time}".replace("/", "-").replace(":", "-"))
                    }
                }
                onUpdate(bookedKeys)
            }
    }


    // Đặt lịch hẹn -5-
    // Lắng nghe lịch hẹn realtime
    fun listenAppointments(isLandlord: Boolean, onUpdate: (List<Appointment>) -> Unit, onError: (String) -> Unit): ListenerRegistration {
        val uid = auth.currentUser?.uid ?: return object : ListenerRegistration { override fun remove() {} }
        val field = if (isLandlord) "landlordId" else "tenantId"
        return db.collection("appointments")
            .whereEqualTo(field, uid)
            .addSnapshotListener { value, error ->
                if (error != null) { onError("Lỗi tải lịch: ${error.message}"); return@addSnapshotListener }
                val list = value?.map { doc -> Appointment.fromMap(doc.id, doc.data) }
                    ?.sortedByDescending { it.createdAt } ?: emptyList()
                onUpdate(list)
            }
    }

    // Đặt lịch hẹn -5-
    fun listenBothAppointments(
        onTenantUpdate: (List<Appointment>) -> Unit,
        onLandlordUpdate: (List<Appointment>) -> Unit,
        onError: (String) -> Unit
    ): Pair<ListenerRegistration, ListenerRegistration> {
        val uid = auth.currentUser?.uid
        val empty = object : ListenerRegistration { override fun remove() {} }
        if (uid == null) return Pair(empty, empty)

        val tenantListener = db.collection("appointments").whereEqualTo("tenantId", uid)
            .addSnapshotListener { v, e ->
                if (e != null) { onError(e.message ?: "Lỗi"); return@addSnapshotListener }
                onTenantUpdate(v?.map { Appointment.fromMap(it.id, it.data) }?.sortedByDescending { it.createdAt } ?: emptyList())
            }
        val landlordListener = db.collection("appointments").whereEqualTo("landlordId", uid)
            .addSnapshotListener { v, e ->
                if (e != null) { onError(e.message ?: "Lỗi"); return@addSnapshotListener }
                onLandlordUpdate(v?.map { Appointment.fromMap(it.id, it.data) }?.sortedByDescending { it.createdAt } ?: emptyList())
            }
        return Pair(tenantListener, landlordListener)
    }


    // Đặt lịch hẹn -6-
    // Bấm xác nhận lịch hẹn
    fun confirmAppointment(
        appointmentId: String, tenantId: String, roomTitle: String,
        date: String, time: String,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        // 1. Tìm thông tin của lịch hẹn mà chủ trọ vừa bấm xác nhận
        val apptRef = db.collection("appointments").document(appointmentId)
        apptRef.get().addOnSuccessListener { apptDoc ->
            if (!apptDoc.exists()) { onFailure("Không tìm thấy lịch hẹn"); return@addOnSuccessListener }
            val roomId = apptDoc.getString("roomId") ?: run { onFailure("Lịch hẹn thiếu roomId"); return@addOnSuccessListener }

            // 2. BƯỚC CHUẨN BỊ: Tìm TẤT CẢ các lịch hẹn của người khác đang đặt trùng ngày, trùng giờ, trùng phòng
            // (Phải tìm trước vì Transaction của Firebase không cho phép dùng lệnh query bên trong)
            db.collection("appointments")
                .whereEqualTo("roomId", roomId)
                .whereEqualTo("appointmentDate", date)
                .whereEqualTo("appointmentTime", time)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener { overlapSnap ->
                    val now = System.currentTimeMillis()
                    val apptTimestampMs = apptDoc.getLong("appointmentTimestampMs") ?: 0L
                    val tenantDeadlineMs = if (apptTimestampMs > now + AppointmentConstants.HOURS_4_MS)
                        apptTimestampMs - AppointmentConstants.HOURS_1_MS
                    else
                        now + AppointmentConstants.HOURS_4_MS
                    val slotRef = db.collection("bookedSlots").document(buildSlotId(roomId, date, time))

                    // 3. Lọc danh sách trùng: Lấy ra ID của những người khách bị trùng giờ (để lát nữa bắn thông báo hủy)
                    val overlapEntries = overlapSnap.documents
                        .filter { it.id != appointmentId }
                        .map { Pair(it.reference, it.getString("tenantId") ?: "") }

                    // 4. MỞ GIAO DỊCH (TRANSACTION): Đảm bảo khóa giờ và hủy lịch trùng diễn ra cùng lúc 100%
                    db.runTransaction { tx ->
                        // Bước 4.1: Đọc dữ liệu trước (Bắt buộc theo nguyên tắc của Transaction)
                        val slotSnap = tx.get(slotRef)
                        val freshAppt = tx.get(apptRef)
                        val overlapFreshDocs = overlapEntries.map { (ref, tid) ->
                            Triple(ref, tid, tx.get(ref))
                        }

                        // Bước 4.2: Kiểm tra an ninh (Validation)
                        // Tránh lỗi 2 chủ trọ xài 2 máy cùng bấm xác nhận 1 lúc, ai tới trước người đó ăn
                        if (slotSnap.exists()) throw FirebaseFirestoreException(
                            "Khung giờ này đã được xác nhận cho khách hàng khác.",
                            FirebaseFirestoreException.Code.ABORTED
                        )
                        val status = freshAppt.getString("status") ?: ""
                        if (status != "pending") throw FirebaseFirestoreException(
                            "Lịch hẹn đã thay đổi trạng thái hoặc bị hủy bởi khách.",
                            FirebaseFirestoreException.Code.ABORTED
                        )

                        // Bước 4.3: TIẾN HÀNH GHI DỮ LIỆU
                        // Việc 1: Đổi trạng thái lịch của ông khách được chọn thành "Đã chốt" (confirmed)
                        tx.update(apptRef, mapOf(
                            "status" to "confirmed",
                            "hasUnreadUpdate" to true,
                            "updatedAt" to now,
                            "tenantConfirmDeadline" to tenantDeadlineMs,
                            "statusHistory" to FieldValue.arrayUnion(
                                StatusChange("pending", "confirmed", "landlord",
                                    auth.currentUser?.uid ?: "", "", now).toMap()
                            )
                        ))
                        // Việc 2: Ghi dữ liệu vào bảng bookedSlots. Đây chính là hành động "KHÓA GIỜ", làm màn hình máy khác bị bôi xám
                        tx.set(slotRef, hashMapOf<String, Any>(
                            "roomId" to roomId,
                            "appointmentId" to appointmentId,
                            "tenantId" to tenantId,
                            "landlordId" to (freshAppt.getString("landlordId") ?: ""),
                            "date" to date,
                            "time" to time,
                            "createdAt" to now
                        ))
                        // Việc 3: HỦY TỰ ĐỘNG toàn bộ lịch của những người xui xẻo trùng giờ NGAY TRONG Transaction này
                        overlapFreshDocs.forEach { (ref, _, freshDoc) ->
                            if (freshDoc.getString("status") == "pending") {
                                tx.update(ref, mapOf(
                                    "status" to "cancelled_by_system",
                                    "cancelReason" to "Khung giờ đã được xác nhận cho khách khác.",
                                    "hasUnreadUpdate" to true,
                                    "updatedAt" to now,
                                    "statusHistory" to FieldValue.arrayUnion(
                                        StatusChange("pending", "cancelled_by_system", "system",
                                            "system", "Trùng khung giờ", now).toMap()
                                    )
                                ))
                            }
                        }
                    // 5. THÀNH CÔNG: Bắn thông báo (Push Notification) cho từng người
                    }.addOnSuccessListener {
                        // Báo tin vui cho khách may mắn được chọn
                        sendNotification(tenantId, "Lịch hẹn đã xác nhận — Xác nhận sẽ đến!",
                            "Chủ trọ đã xác nhận lịch xem phòng \"$roomTitle\" vào $date lúc $time. Vui lòng mở ứng dụng và xác nhận bạn sẽ đến.",
                            "appointment_confirmed")
                        // Báo tin buồn (bị hủy lịch) cho những người rớt đài
                        overlapEntries.forEach { (_, otherTenantId) ->
                            if (otherTenantId.isNotEmpty()) {
                                sendNotification(otherTenantId, "Lịch hẹn bị hủy do trùng giờ",
                                    "Khung giờ bạn đặt xem phòng \"$roomTitle\" đã được xác nhận cho khách khác. Vui lòng chọn giờ khác.",
                                    "appointment_conflict")
                            }
                        }
                        onSuccess()
                    }.addOnFailureListener { e ->
                        val msg = if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.ABORTED)
                            e.message ?: "Khung giờ đã được đặt"
                        else "Xác nhận thất bại: ${e.message}"
                        onFailure(msg)
                    }
                }.addOnFailureListener { e -> onFailure("Lỗi tìm lịch hẹn trùng: ${e.message}") }
        }.addOnFailureListener { e -> onFailure("Lỗi đọc lịch hẹn: ${e.message}") }
    }

    // Khách hàng xác nhận "Sẽ đến xem" (Xác nhận 2 chiều sau khi chủ trọ đã chốt lịch)
    // 1 tiếng trước khi lịch hẹn bị hủy
    fun tenantConfirmAppointment(
        appointmentId: String, landlordId: String, roomTitle: String,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        val now = System.currentTimeMillis()
        val apptRef = db.collection("appointments").document(appointmentId)
        
        // Dùng Transaction để đảm bảo tính chính xác tuyệt đối khi đọc/ghi dữ liệu
        db.runTransaction { tx ->
            val doc = tx.get(apptRef)
            // 1. Kiểm tra an ninh (Validation)
            if (!doc.exists()) throw FirebaseFirestoreException("Không tìm thấy lịch hẹn", FirebaseFirestoreException.Code.NOT_FOUND)
            
            val status = doc.getString("status")
            // Chỉ cho phép khách bấm xác nhận nếu lịch đang ở trạng thái "Đã duyệt" (confirmed)
            if (status != "confirmed") throw FirebaseFirestoreException("Lịch hẹn không ở trạng thái chờ xác nhận từ bạn.", FirebaseFirestoreException.Code.ABORTED)
            
            val deadline = doc.getLong("tenantConfirmDeadline") ?: 0L
            // Kiểm tra xem khách có bấm xác nhận quá trễ so với deadline cho phép không
            if (deadline in 1..<now) throw FirebaseFirestoreException("Đã quá hạn xác nhận lịch hẹn.", FirebaseFirestoreException.Code.ABORTED)
            
            // 2. Tiến hành Ghi dữ liệu: Đổi trạng thái sang "tenant_confirmed" (Khách đã cam kết đi xem)
            tx.update(apptRef, mapOf(
                "status" to "tenant_confirmed",
                "hasUnreadUpdate" to true,
                "updatedAt" to now,
                "statusHistory" to FieldValue.arrayUnion(
                    StatusChange("confirmed", "tenant_confirmed", "tenant",
                        auth.currentUser?.uid ?: "", "", now).toMap()
                )
            ))
        }.addOnSuccessListener {
            // 3. THÀNH CÔNG: Bắn thông báo báo tin mừng về cho Chủ trọ biết để chuẩn bị đón
            sendNotification(landlordId, "Khách đã xác nhận sẽ đến!",
                "Khách đã xác nhận sẽ đến xem phòng \"$roomTitle\" đúng giờ hẹn. Hãy chuẩn bị đón tiếp!",
                "appointment_tenant_confirmed")
            onSuccess()
        }.addOnFailureListener { e ->
            // Bắt lỗi nếu có vấn đề (Ví dụ: khách bấm trễ hạn, mạng lag...)
            val msg = if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.ABORTED)
                e.message ?: "Xác nhận thất bại"
            else "Xác nhận thất bại: ${e.message}"
            onFailure(msg)
        }
    }


    // Đặt lịch hẹn -7-
    fun rejectAppointment(
        appointmentId: String, tenantId: String, roomTitle: String, reason: String,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        val now = System.currentTimeMillis()
        val apptRef = db.collection("appointments").document(appointmentId)
        db.runTransaction { tx ->
            val doc = tx.get(apptRef)
            if (!doc.exists()) throw FirebaseFirestoreException("Không tìm thấy lịch hẹn", FirebaseFirestoreException.Code.NOT_FOUND)
            if (doc.getString("status") != "pending") throw FirebaseFirestoreException("Lịch hẹn không ở trạng thái chờ duyệt.", FirebaseFirestoreException.Code.ABORTED)
            
            tx.update(apptRef, mapOf(
                "status" to "rejected",
                "rejectReason" to reason,
                "hasUnreadUpdate" to true,
                "updatedAt" to now,
                "statusHistory" to FieldValue.arrayUnion(
                    StatusChange("pending", "rejected", "landlord",
                        auth.currentUser?.uid ?: "", reason, now).toMap()
                )
            ))
        }.addOnSuccessListener {
            val msg = if (reason.isNotEmpty())
                "Chủ trọ đã từ chối lịch hẹn xem phòng \"$roomTitle\". Lý do: $reason"
            else "Chủ trọ đã từ chối lịch hẹn xem phòng \"$roomTitle\"."
            sendNotification(tenantId, "Lịch hẹn bị từ chối", msg, "appointment_rejected")
            onSuccess()
        }.addOnFailureListener { e ->
            val msg = if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.ABORTED)
                e.message ?: "Từ chối thất bại"
            else "Từ chối thất bại: ${e.message}"
            onFailure(msg)
        }
    }


    fun markAsViewed(appointmentId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val now = System.currentTimeMillis()
        val apptRef = db.collection("appointments").document(appointmentId)
        apptRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) { onFailure("Không tìm thấy lịch hẹn"); return@addOnSuccessListener }
            val appt = Appointment.fromMap(doc.id, doc.data ?: emptyMap())
            val batch = db.batch()
            batch.update(apptRef, mapOf(
                "status" to "completed_viewed",
                "hasUnreadUpdate" to true,
                "updatedAt" to now,
                "statusHistory" to FieldValue.arrayUnion(
                    StatusChange(appt.status, "completed_viewed", "landlord",
                        auth.currentUser?.uid ?: "", "Khách đã đến xem", now).toMap()
                )
            ))
            // Xóa bookedSlots để mở lại slot cho người khác
            val slotId = buildSlotId(appt.roomId, appt.appointmentDate, appt.appointmentTime)
            batch.delete(db.collection("bookedSlots").document(slotId))
            batch.commit()
                .addOnSuccessListener {
                    sendNotification(appt.tenantId, "Cảm ơn đã đến xem!",
                        "Cảm ơn bạn đã đến xem phòng \"${appt.roomTitle}\". Liên hệ chủ trọ nếu muốn thuê nhé!",
                        "appointment_viewed")
                    onSuccess()
                }
                .addOnFailureListener { e -> onFailure("Cập nhật thất bại: ${e.message}") }
        }.addOnFailureListener { e -> onFailure("Lỗi đọc lịch hẹn: ${e.message}") }
    }


    fun markAsNotRented(appointmentId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val now = System.currentTimeMillis()
        val apptRef = db.collection("appointments").document(appointmentId)
        apptRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) { onFailure("Không tìm thấy lịch hẹn"); return@addOnSuccessListener }
            val appt = Appointment.fromMap(doc.id, doc.data ?: emptyMap())
            // bookedSlot đã được xóa bởi markAsViewed() trước đó — không cần xóa lại
            apptRef.update(mapOf(
                "status" to "viewed_not_rented",
                "hasUnreadUpdate" to true,
                "updatedAt" to now,
                "statusHistory" to FieldValue.arrayUnion(
                    StatusChange(appt.status, "viewed_not_rented", "landlord",
                        auth.currentUser?.uid ?: "", "Khách đã xem nhưng không thuê", now).toMap()
                )
            )).addOnSuccessListener {
                sendNotification(appt.tenantId, "Cảm ơn đã đến xem phòng!",
                    "Cảm ơn bạn đã đến xem phòng \"${appt.roomTitle}\". Phòng vẫn còn trống, liên hệ lại nếu bạn muốn thuê nhé!",
                    "appointment_not_rented")
                onSuccess()
            }.addOnFailureListener { e -> onFailure("Cập nhật thất bại: ${e.message}") }
        }.addOnFailureListener { e -> onFailure("Lỗi đọc lịch hẹn: ${e.message}") }
    }

    // ─── Mark as No Show (Khách không đến) ────────────────────────────────────
    fun markAsNoShow(appointmentId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val now = System.currentTimeMillis()
        val apptRef = db.collection("appointments").document(appointmentId)
        apptRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) { onFailure("Không tìm thấy lịch hẹn"); return@addOnSuccessListener }
            val appt = Appointment.fromMap(doc.id, doc.data ?: emptyMap())
            val batch = db.batch()
            batch.update(apptRef, mapOf(
                "status" to "no_show",
                "hasUnreadUpdate" to true,
                "updatedAt" to now,
                "statusHistory" to FieldValue.arrayUnion(
                    StatusChange(appt.status, "no_show", "landlord",
                        auth.currentUser?.uid ?: "", "Khách không đến", now).toMap()
                )
            ))
            val slotId = buildSlotId(appt.roomId, appt.appointmentDate, appt.appointmentTime)
            batch.delete(db.collection("bookedSlots").document(slotId))
            batch.commit()
                .addOnSuccessListener {
                    // Cộng dồn no-show count trên tài khoản người thuê
                    if (appt.tenantId.isNotEmpty()) {
                        db.collection("users").document(appt.tenantId)
                            .update(mapOf(
                                "noShowCount" to FieldValue.increment(1),
                                "lastNoShowAt" to now
                            ))
                    }
                    sendNotification(appt.tenantId, "Bạn đã không đến đúng hẹn",
                        "Chủ trọ ghi nhận bạn không đến xem phòng \"${appt.roomTitle}\" theo lịch hẹn.",
                        "appointment_no_show")
                    onSuccess()
                }
                .addOnFailureListener { e -> onFailure("Cập nhật thất bại: ${e.message}") }
        }.addOnFailureListener { e -> onFailure("Lỗi đọc lịch hẹn: ${e.message}") }
    }

    // ─── Mark as Landlord No Show (Chủ trọ không đến - tenant báo cáo) ─────────
    fun markAsLandlordNoShow(appointmentId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val now = System.currentTimeMillis()
        val apptRef = db.collection("appointments").document(appointmentId)
        apptRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) { onFailure("Không tìm thấy lịch hẹn"); return@addOnSuccessListener }
            val appt = Appointment.fromMap(doc.id, doc.data ?: emptyMap())
            if (appt.status !in listOf("confirmed", "tenant_confirmed")) {
                onFailure("Không thể báo cáo với trạng thái hiện tại.")
                return@addOnSuccessListener
            }
            val batch = db.batch()
            batch.update(apptRef, mapOf(
                "status" to "landlord_no_show",
                "hasUnreadUpdate" to true,
                "updatedAt" to now,
                "statusHistory" to FieldValue.arrayUnion(
                    StatusChange(appt.status, "landlord_no_show", "tenant",
                        auth.currentUser?.uid ?: "", "Chủ trọ không đến", now).toMap()
                )
            ))
            // Xóa bookedSlots để mở lại slot — slot vẫn còn tồn tại từ confirmed/tenant_confirmed
            val slotId = buildSlotId(appt.roomId, appt.appointmentDate, appt.appointmentTime)
            batch.delete(db.collection("bookedSlots").document(slotId))
            batch.commit()
                .addOnSuccessListener {
                    // Ghi nhận hành vi vắng hẹn trên tài khoản chủ trọ
                    if (appt.landlordId.isNotEmpty()) {
                        db.collection("users").document(appt.landlordId)
                            .update(mapOf(
                                "noShowCount" to FieldValue.increment(1),
                                "lastNoShowAt" to now
                            ))
                    }

                    // ─── Tăng noShowReportCount của phòng và kiểm tra ngưỡng ẩn bài ───
                    if (appt.roomId.isNotEmpty()) {
                        val roomRef = db.collection("rooms").document(appt.roomId)
                        db.runTransaction { tx ->
                            val roomSnap = tx.get(roomRef)
                            if (!roomSnap.exists()) return@runTransaction

                            val currentCount = (roomSnap.getLong("noShowReportCount") ?: 0L).toInt()
                            val newCount = currentCount + 1
                            val currentStatus = roomSnap.getString("status") ?: ""

                            if (newCount >= 3 && currentStatus == "approved") {
                                // Đủ 3 lần bị report → ẩn bài khỏi tìm kiếm trong 24 tiếng
                                val hiddenUntilMs = now + 24 * 3_600_000L
                                tx.update(roomRef, mapOf(
                                    "noShowReportCount" to newCount,
                                    "status" to "hidden_by_system",
                                    "hiddenReason" to "Bài đăng bị ẩn do chủ trọ không đến điểm hẹn 3 lần",
                                    "hiddenAt" to now,
                                    "hiddenUntilMs" to hiddenUntilMs,
                                    "hasUnreadUpdate" to true,
                                    "updatedAt" to now
                                ))
                            } else {
                                tx.update(roomRef, mapOf(
                                    "noShowReportCount" to newCount,
                                    "updatedAt" to now
                                ))
                            }
                        }.addOnSuccessListener {
                            // Đọc lại để biết count mới sau transaction để quyết định có gửi thông báo ẩn bài không
                            db.collection("rooms").document(appt.roomId).get()
                                .addOnSuccessListener { roomDoc ->
                                    val finalCount = (roomDoc.getLong("noShowReportCount") ?: 0L).toInt()
                                    val finalStatus = roomDoc.getString("status") ?: ""
                                    val roomTitle = roomDoc.getString("title") ?: appt.roomTitle

                                    if (finalStatus == "hidden_by_system" && finalCount >= 3) {
                                        // Gửi thông báo realtime: bài đăng đã bị ẩn 24 tiếng
                                        val hiddenUntilMs = roomDoc.getLong("hiddenUntilMs") ?: 0L
                                        val sdf = java.text.SimpleDateFormat("HH:mm dd/MM/yyyy", java.util.Locale("vi", "VN"))
                                        val unlockStr = sdf.format(java.util.Date(hiddenUntilMs))
                                        sendNotification(
                                            userId = appt.landlordId,
                                            title = "Bài đăng của bạn đã bị ẩn tạm thời",
                                            message = "Bài đăng \"$roomTitle\" đã bị ẩn khỏi kết quả tìm kiếm do có $finalCount khách báo cáo bạn không đến điểm hẹn. Bài sẽ tự động hiển thị lại lúc $unlockStr.",
                                            type = "room_hidden_by_system"
                                        )
                                    }
                                }
                        }
                    }

                    sendNotification(appt.landlordId, "Khách báo cáo bạn không đến",
                        "Khách hẹn xem phòng \"${appt.roomTitle}\" báo cáo bạn không có mặt đúng giờ hẹn.",
                        "appointment_landlord_no_show")
                    onSuccess()
                }
                .addOnFailureListener { e -> onFailure("Cập nhật thất bại: ${e.message}") }
        }.addOnFailureListener { e -> onFailure("Lỗi đọc lịch hẹn: ${e.message}") }
    }

    // ─── Reopen Room (Chủ trọ mở lại phòng sau khi đã rented) ─────────────────
    fun reopenRoom(
        roomId: String, appointmentId: String, tenantId: String,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        val uid = currentUid ?: return onFailure("Chưa đăng nhập")
        val now = System.currentTimeMillis()
        val roomRef = db.collection("rooms").document(roomId)
        val apptRef = db.collection("appointments").document(appointmentId)
        roomRef.get().addOnSuccessListener { roomDoc ->
            if (!roomDoc.exists()) { onFailure("Không tìm thấy phòng"); return@addOnSuccessListener }
            val landlordId = roomDoc.getString("userId") ?: ""
            if (landlordId != uid) { onFailure("Bạn không có quyền thực hiện thao tác này."); return@addOnSuccessListener }
            val batch = db.batch()
            batch.update(roomRef, mapOf("status" to "active", "updatedAt" to now))
            batch.update(apptRef, mapOf(
                "updatedAt" to now,
                "statusHistory" to FieldValue.arrayUnion(
                    StatusChange("completed_rented", "completed_rented", "landlord",
                        uid, "Chủ trọ mở lại phòng — thỏa thuận không thành", now).toMap()
                )
            ))
            batch.commit()
                .addOnSuccessListener {
                    if (tenantId.isNotEmpty()) {
                        sendNotification(tenantId, "Phòng đã được mở lại",
                            "Chủ trọ đã mở lại phòng cho thuê. Liên hệ chủ trọ nếu bạn vẫn muốn thuê.",
                            "room_reopened")
                    }
                    onSuccess()
                }
                .addOnFailureListener { e -> onFailure("Cập nhật thất bại: ${e.message}") }
        }.addOnFailureListener { e -> onFailure("Lỗi đọc thông tin phòng: ${e.message}") }
    }

    // ─── Mark as Rented (từ completed_viewed hoặc tenant_confirmed) ──────────
    fun markAsRented(
        appointmentId: String, roomId: String, tenantId: String, roomTitle: String,
        date: String, time: String,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        val uid = currentUid ?: return onFailure("Chưa đăng nhập")
        val now = System.currentTimeMillis()
        val roomRef = db.collection("rooms").document(roomId)
        val activeStatuses = setOf("pending", "confirmed", "tenant_confirmed")

        db.collection("appointments").whereEqualTo("roomId", roomId).get()
            .addOnSuccessListener { apptSnaps ->
                // Fix #8: Kiểm tra quyền sở hữu — chỉ chủ phòng mới được đánh dấu đã thuê
                val targetDoc = apptSnaps.documents.find { it.id == appointmentId }
                val landlordIdFromDoc = targetDoc?.getString("landlordId") ?: ""
                if (landlordIdFromDoc.isEmpty() || landlordIdFromDoc != uid) {
                    onFailure("Bạn không có quyền thực hiện thao tác này.")
                    return@addOnSuccessListener
                }

                db.collection("savedPosts").whereEqualTo("roomId", roomId).get()
                    .addOnSuccessListener { savedSnaps ->
                        val batch = db.batch()
                        batch.update(roomRef, mapOf("status" to "rented", "updatedAt" to now))

                        val currentDoc = apptSnaps.documents.find { it.id == appointmentId }
                        val currentStatus = currentDoc?.getString("status") ?: ""

                        val currentApptRef = db.collection("appointments").document(appointmentId)
                        batch.update(currentApptRef, mapOf(
                            "status" to "completed_rented", "updatedAt" to now,
                            "statusHistory" to FieldValue.arrayUnion(
                                StatusChange(currentStatus, "completed_rented", "landlord",
                                    uid, "Khách đã thuê phòng", now).toMap()
                            )
                        ))

                        // Xóa slot (nếu còn - từ confirmed/tenant_confirmed)
                        if (currentStatus in setOf("confirmed", "tenant_confirmed")) {
                            batch.delete(db.collection("bookedSlots").document(buildSlotId(roomId, date, time)))
                        }

                        savedSnaps.documents.forEach { batch.delete(it.reference) }

                        apptSnaps.documents.forEach { doc ->
                            if (doc.id != appointmentId) {
                                val status = doc.getString("status") ?: ""
                                if (status in activeStatuses) {
                                    batch.update(doc.reference, mapOf(
                                        "status" to "cancelled_by_system",
                                        "cancelReason" to "Phòng đã được cho thuê",
                                        "hasUnreadUpdate" to true, "updatedAt" to now
                                    ))
                                    if (status == "confirmed" || status == "tenant_confirmed") {
                                        val d = doc.getString("appointmentDate") ?: doc.getString("date") ?: ""
                                        val t = doc.getString("appointmentTime") ?: doc.getString("time") ?: ""
                                        if (d.isNotEmpty() && t.isNotEmpty())
                                            batch.delete(db.collection("bookedSlots").document(buildSlotId(roomId, d, t)))
                                    }
                                }
                            }
                        }

                        batch.commit().addOnSuccessListener {
                            sendNotification(tenantId, "Thuê phòng thành công!",
                                "Chúc mừng! Bạn đã thuê thành công phòng \"$roomTitle\".", "room_rented_success")
                            apptSnaps.documents.forEach { doc ->
                                if (doc.id != appointmentId && (doc.getString("status") ?: "") in activeStatuses) {
                                    val oTenantId = doc.getString("tenantId") ?: ""
                                    if (oTenantId.isNotEmpty())
                                        sendNotification(oTenantId, "Phòng đã có người thuê",
                                            "Phòng \"$roomTitle\" mà bạn hẹn xem đã được thuê bởi người khác.", "room_already_rented")
                                }
                            }
                            onSuccess()
                        }.addOnFailureListener { e -> onFailure("Lỗi cập nhật: ${e.message}") }
                    }.addOnFailureListener { e -> onFailure("Lỗi: ${e.message}") }
            }.addOnFailureListener { e -> onFailure("Lỗi: ${e.message}") }
    }

    // Đặt lịch hẹn -7-
    // Cancel pending (người thuê hủy lịch pending)
    fun cancelPendingAppointment(
        appointmentId: String, landlordId: String, roomTitle: String,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        val now = System.currentTimeMillis()
        db.collection("appointments").document(appointmentId)
            .update(mapOf(
                "status" to "cancelled_by_tenant",
                "cancelReason" to "Người thuê hủy",
                "hasUnreadUpdate" to true, "updatedAt" to now,
                "statusHistory" to FieldValue.arrayUnion(
                    StatusChange("pending", "cancelled_by_tenant", "tenant",
                        auth.currentUser?.uid ?: "", "Người thuê hủy", now).toMap()
                )
            ))
            .addOnSuccessListener {
                sendNotification(landlordId, "Lịch hẹn đã bị hủy",
                    "Người thuê đã hủy yêu cầu đặt lịch xem phòng \"$roomTitle\".", "appointment_cancelled")
                onSuccess()
            }
            .addOnFailureListener { e -> onFailure("Hủy lịch thất bại: ${e.message}") }
    }

    // Người dùng bấm hủy lịch hẹn
    fun tenantRejectAppointment(
        appointmentId: String, landlordId: String, roomTitle: String,
        roomId: String, date: String, time: String, currentStatus: String,
        cancelReason: String,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        val now = System.currentTimeMillis()
        val batch = db.batch()
        val apptRef = db.collection("appointments").document(appointmentId)
        batch.update(apptRef, mapOf(
            "status" to "cancelled_by_tenant",
            "cancelReason" to cancelReason,
            "isQuotaPenalized" to true, // PHẠT: Đánh dấu mất lượt vì hủy sau khi đã duyệt
            "hasUnreadUpdate" to true, "updatedAt" to now,
            "statusHistory" to FieldValue.arrayUnion(
                StatusChange(currentStatus, "cancelled_by_tenant", "tenant",
                    auth.currentUser?.uid ?: "", cancelReason, now).toMap()
            )
        ))
        batch.delete(db.collection("bookedSlots").document(buildSlotId(roomId, date, time)))
        batch.commit()
            .addOnSuccessListener {
                sendNotification(landlordId, "Người thuê huỷ lịch hẹn",
                    "Khách đã huỷ lịch xem phòng \"$roomTitle\". Lý do: $cancelReason", "appointment_cancelled")
                onSuccess()
            }
            .addOnFailureListener { e -> onFailure("Huỷ lịch thất bại: ${e.message}") }
    }

    // ─── Edit pending appointment (đổi ngày/giờ) ─────────────────────────────
    fun editPendingAppointment(
        appointmentId: String, landlordId: String, roomTitle: String,
        newDate: String, newDateDisplay: String, newTime: String,
        newDateMs: Long, newTimestampMs: Long,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        val now = System.currentTimeMillis()
        val apptRef = db.collection("appointments").document(appointmentId)
        apptRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) { onFailure("Không tìm thấy lịch hẹn"); return@addOnSuccessListener }
            val appt = Appointment.fromMap(doc.id, doc.data ?: emptyMap())
            if (appt.editCount >= AppointmentConstants.MAX_EDIT_COUNT) {
                onFailure("Bạn đã đổi lịch tối đa ${AppointmentConstants.MAX_EDIT_COUNT} lần cho cuộc hẹn này. Vui lòng hủy và đặt lịch mới.")
                return@addOnSuccessListener
            }
            if (appt.status != "pending") {
                onFailure("Chỉ có thể đổi lịch hẹn ở trạng thái chờ xác nhận.")
                return@addOnSuccessListener
            }

                            val newSlotId = buildSlotId(appt.roomId, newDate, newTime)
                            val slotRef = db.collection("bookedSlots").document(newSlotId)
                            
                            db.runTransaction { tx ->
                                val slotSnap = tx.get(slotRef)
                                if (slotSnap.exists()) throw FirebaseFirestoreException(
                                    "Khung giờ mới $newTime ngày $newDate đã có người đặt. Vui lòng chọn giờ khác.",
                                    FirebaseFirestoreException.Code.ABORTED
                                )
                                val currentAppt = tx.get(apptRef)
                                if (currentAppt.getString("status") != "pending") throw FirebaseFirestoreException(
                                    "Trạng thái lịch hẹn đã thay đổi, không thể đổi lịch.",
                                    FirebaseFirestoreException.Code.ABORTED
                                )
                                
                                val newDeadline = newTimestampMs // Hạn duyệt = đúng giờ hẹn mới
                                val newTenantDeadline = newTimestampMs - AppointmentConstants.HOURS_1_MS
                                tx.update(apptRef, mapOf(
                                    "appointmentDate" to newDate,
                                    "appointmentDateDisplay" to newDateDisplay,
                                    "appointmentTime" to newTime,
                                    "appointmentDateMs" to newDateMs,
                                    "appointmentTimestampMs" to newTimestampMs,
                                    "landlordConfirmDeadline" to newDeadline,
                                    "tenantConfirmDeadline" to newTenantDeadline,
                                    "editCount" to (appt.editCount + 1),
                                    "landlordRemind1hPendingSent" to false,
                                    "reminder24hSent" to false,
                                    "reminder2hSent" to false,
                                    "reminder30mSent" to false,
                                    "reminder0hSent" to false,
                                    "landlordReminder24hSent" to false,
                                    "landlordReminder30mSent" to false,
                                    "landlordReminder0hSent" to false,
                                    "resultAskedSent" to false,
                                    "autoNoShowSent" to false,
                                    "hasUnreadUpdate" to true,
                                    "updatedAt" to now,
                                    "statusHistory" to FieldValue.arrayUnion(
                                        StatusChange(appt.status, appt.status, "tenant",
                                            auth.currentUser?.uid ?: "", "Đổi lịch sang $newDateDisplay lúc $newTime", now).toMap()
                                    )
                                ))
                            }.addOnSuccessListener {
                                sendNotification(landlordId, "Lịch hẹn đã được đổi giờ",
                                    "Người thuê đã đổi lịch xem phòng \"$roomTitle\" sang $newDateDisplay lúc $newTime.", "appointment_edited")
                                onSuccess()
                            }.addOnFailureListener { e ->
                                val msg = if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.ABORTED)
                                    e.message ?: "Khung giờ đã có người đặt"
                                else "Đổi lịch thất bại: ${e.message}"
                                onFailure(msg)
                            }
        }.addOnFailureListener { e -> onFailure("Lỗi đọc lịch hẹn: ${e.message}") }
    }


    // Kiểm tra xem khách đã từng tạo lịch hẹn cho phòng này vào đúng cái ngày/giờ này chưa.
    // (Ngăn chặn lỗi logic khách đặt đè 2 lịch hẹn trùng y hệt nhau).
    // Đặt lịch hẹn
    // [AN NINH ĐẶT LỊCH] Chặn khách hàng đặt đè thêm lịch vào căn phòng mà họ đang đặt.
    fun checkExistingAppointment(
        tenantId: String, roomId: String,
        onResult: (Boolean, String?, String?, Long?) -> Unit
    ) {
        db.collection("appointments")
            .whereEqualTo("tenantId", tenantId)
            .whereEqualTo("roomId", roomId)
            .get()
            .addOnSuccessListener { snaps ->
                if (snaps.isEmpty) { onResult(false, null, null, null); return@addOnSuccessListener }
                val latest = snaps.documents.maxByOrNull { it.getLong("createdAt") ?: 0L } ?: return@addOnSuccessListener
                val status = latest.getString("status") ?: ""
                when {
                    status in listOf("pending", "confirmed", "tenant_confirmed") -> onResult(true, status, latest.id, null)
                    status == "completed_rented" -> onResult(true, "rented", latest.id, null)
                    else -> onResult(false, null, null, null)
                }
            }
            .addOnFailureListener { onResult(false, null, null, null) }
    }

    // ─── Kiểm tra bookedSlots realtime ───────────────────────────────────────
    fun checkTimeConflicts(roomId: String, onUpdate: (Map<String, Int>) -> Unit): ListenerRegistration {
        return db.collection("bookedSlots")
            .whereEqualTo("roomId", roomId)
            .addSnapshotListener { snaps, _ ->
                val conflicts = mutableMapOf<String, Int>()
                snaps?.forEach { doc ->
                    val date = doc.getString("date") ?: ""
                    val time = doc.getString("time") ?: ""
                    if (date.isNotEmpty() && time.isNotEmpty()) {
                        val key = "${date}_${time}".replace("/", "-").replace(":", "-")
                        conflicts[key] = (conflicts[key] ?: 0) + 1
                    }
                }
                onUpdate(conflicts)
            }
    }

    // ─── Role / access helpers ────────────────────────────────────────────────
    fun getCurrentUserRole(onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onFailure("Chưa đăng nhập")
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role") ?: ""
                val isVerified = doc.getBoolean("isVerified") ?: false
                onSuccess(when { role == "admin" -> "admin"; isVerified -> "verified"; else -> "user" })
            }
            .addOnFailureListener { e -> onFailure(e.message ?: "Lỗi") }
    }

    fun loadCurrentUserAppointmentAccess(onSuccess: (Boolean, String) -> Unit, onFailure: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onFailure("Chưa đăng nhập")
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val isVerified = doc.getBoolean("isVerified") ?: false
                val role = doc.getString("role").orEmpty()
                val hostAccess = isVerified || role == "admin"
                val effectiveRole = if (hostAccess) "verified" else "user"
                onSuccess(hostAccess, effectiveRole)
            }
            .addOnFailureListener { e -> onFailure(e.message ?: "Lỗi") }
    }

    fun fetchAppointmentDetails(roomId: String, tenantId: String,
        onRoomLoaded: (Room?) -> Unit, onTenantLoaded: (User?) -> Unit, onError: (String) -> Unit) {
        db.collection("rooms").document(roomId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) { onRoomLoaded(null); return@addOnSuccessListener }
                @Suppress("UNCHECKED_CAST")
                onRoomLoaded(Room(
                    id = doc.id, userId = doc.getString("userId") ?: "",
                    title = doc.getString("title") ?: "", address = doc.getString("address") ?: "",
                    price = doc.getLong("price") ?: 0L,
                    imageUrls = (doc.get("imageUrls") as? List<*>)?.filterIsInstance<String>() ?: listOf(),
                    ownerName = doc.getString("ownerName") ?: "",
                    ownerPhone = doc.getString("ownerPhone") ?: "",
                    ownerGender = doc.getString("ownerGender") ?: "",
                    status = doc.getString("status") ?: "pending",
                    postExpiryDate = doc.getLong("postExpiryDate") ?: 0L,
                    availableTimeSlots = doc.getString("availableTimeSlots") ?: ""
                ))
            }
            .addOnFailureListener { e -> onError("Lỗi tải phòng: ${e.message}") }

        db.collection("users").document(tenantId).get()
            .addOnSuccessListener { onTenantLoaded(it.toUser()) }
            .addOnFailureListener { onError("Lỗi tải khách: ${it.message}") }
    }

    // ─── Badge + read status ──────────────────────────────────────────────────
    fun listenBadge(uid: String, role: String, onResult: (Int) -> Unit): ListenerRegistration {
        val isHostAccess = role == "admin" || role == "verified"
        // Landlord: cần biết có booking mới (pending) hoặc khách xác nhận (tenant_confirmed)
        val hostStatuses = setOf("pending", "tenant_confirmed")
        // Tenant: cần biết mọi update từ phía chủ (confirmed, từ chối, chủ hủy, expired)
        val tenantStatuses = setOf("confirmed", "rejected", "cancelled_by_landlord", "expired_pending", "no_show")
        return if (isHostAccess) {
            db.collection("appointments").whereEqualTo("landlordId", uid)
                .addSnapshotListener { snap, _ ->
                    onResult(snap?.count { it.getString("status") in hostStatuses && it.getBoolean("hasUnreadUpdate") == true } ?: 0)
                }
        } else {
            db.collection("appointments").whereEqualTo("tenantId", uid)
                .addSnapshotListener { snap, _ ->
                    onResult(snap?.count { it.getString("status") in tenantStatuses && it.getBoolean("hasUnreadUpdate") == true } ?: 0)
                }
        }
    }

    fun markAllAppointmentsRead(uid: String, role: String) {
        val isHostAccess = role == "admin" || role == "verified"
        val field = if (isHostAccess) "landlordId" else "tenantId"
        db.collection("appointments").whereEqualTo(field, uid).get()
            .addOnSuccessListener { snap ->
                val toUpdate = snap.documents.filter { it.getBoolean("hasUnreadUpdate") == true }
                if (toUpdate.isEmpty()) return@addOnSuccessListener
                val batch = db.batch()
                toUpdate.forEach { batch.update(it.reference, "hasUnreadUpdate", false) }
                batch.commit()
            }
    }

    // ─── Room rented notice ───────────────────────────────────────────────────
    fun listenRoomRentedNotices(uid: String, onNotice: (RoomRentedNotice) -> Unit, onError: (String) -> Unit): ListenerRegistration {
        return db.collection("notifications")
            .whereEqualTo("userId", uid)
            .whereEqualTo("type", "room_already_rented")
            .whereEqualTo("seen", false)
            .addSnapshotListener { snaps, error ->
                if (error != null) { onError(error.message ?: "Lỗi"); return@addSnapshotListener }
                if (snaps == null || snaps.isEmpty) return@addSnapshotListener
                val doc = snaps.documents.maxByOrNull { it.getLong("createdAt") ?: 0L } ?: return@addSnapshotListener
                onNotice(RoomRentedNotice(doc.id,
                    doc.getString("title") ?: "Lịch hẹn đã bị hủy",
                    doc.getString("message") ?: "Phòng đã có người thuê."))
            }
    }

    fun markRoomRentedNoticeRead(notificationId: String) {
        if (notificationId.isBlank()) return
        db.collection("notifications").document(notificationId).update(mapOf("seen" to true, "isRead" to true))
    }

    // ─── Cancel by landlord (hủy lịch đã confirmed từ phía chủ trọ) ─────────
    fun cancelByLandlord(
        appointmentId: String, tenantId: String, roomTitle: String,
        roomId: String, date: String, time: String, cancelReason: String,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        val now = System.currentTimeMillis()
        val apptRef = db.collection("appointments").document(appointmentId)
        val slotRef = db.collection("bookedSlots").document(buildSlotId(roomId, date, time))
        db.runTransaction { tx ->
            val doc = tx.get(apptRef)
            if (!doc.exists()) throw FirebaseFirestoreException("Không tìm thấy lịch hẹn", FirebaseFirestoreException.Code.NOT_FOUND)
            val currentStatus = doc.getString("status") ?: ""
            tx.update(apptRef, mapOf(
                "status" to "cancelled_by_landlord",
                "cancelReason" to cancelReason,
                "hasUnreadUpdate" to true, "updatedAt" to now,
                "statusHistory" to FieldValue.arrayUnion(
                    StatusChange(currentStatus, "cancelled_by_landlord", "landlord",
                        auth.currentUser?.uid ?: "", cancelReason, now).toMap()
                )
            ))
            tx.delete(slotRef)
        }.addOnSuccessListener {
            sendNotification(tenantId, "Chủ trọ đã hủy lịch hẹn",
                "Chủ trọ đã hủy lịch hẹn xem phòng \"$roomTitle\". Lý do: $cancelReason",
                "appointment_cancelled_by_landlord")
            onSuccess()
        }.addOnFailureListener { e -> onFailure("Hủy lịch thất bại: ${e.message}") }
    }

    // Check no-show count của tenant trước khi đặt lịch
    // Đặt lịch hẹn
    // [AN NINH ĐẶT LỊCH] Tra cứu xem khách có dính 3 án tích bùng kèo (No-show) không.
    fun checkTenantNoShowCount(tenantId: String, onResult: (Int, Long) -> Unit) {
        db.collection("users").document(tenantId).get()
            .addOnSuccessListener { doc ->
                var count = (doc.getLong("noShowCount") ?: 0L).toInt()
                val lastTime = doc.getLong("lastNoShowAt") ?: 0L
                val now = System.currentTimeMillis()

                if (count > 0 && lastTime > 0) {
                    val calLast = java.util.Calendar.getInstance().apply { timeInMillis = lastTime }
                    val calNow = java.util.Calendar.getInstance().apply { timeInMillis = now }
                    val isSameDay = calLast.get(java.util.Calendar.YEAR) == calNow.get(java.util.Calendar.YEAR) &&
                                    calLast.get(java.util.Calendar.DAY_OF_YEAR) == calNow.get(java.util.Calendar.DAY_OF_YEAR)
                    
                    if (!isSameDay) {
                        // Sang ngày mới -> Xóa án tích, reset về 0
                        count = 0
                        db.collection("users").document(tenantId).update(
                            mapOf(
                                "noShowCount" to 0,
                                "lastNoShowAt" to 0L
                            )
                        )
                    }
                }
                
                onResult(count, lastTime)
            }
            .addOnFailureListener { onResult(0, 0L) }
    }

    /**
     * Kiểm tra xem khách có đang gửi đi quá nhiều yêu cầu chờ duyệt hay không.
     * (Nếu khách đang có 3 lịch hẹn ở trạng thái Pending chưa được duyệt, sẽ khóa không cho đặt tiếp).
     */
    // Đặt lịch hẹn
    // [AN NINH ĐẶT LỊCH] Đếm xem khách có đang spam quá 3 lịch chờ duyệt không.
    fun checkTenantPendingCount(tenantId: String, onResult: (Int) -> Unit) {
        db.collection("appointments")
            .whereEqualTo("tenantId", tenantId)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { snaps -> onResult(snaps.size()) }
            .addOnFailureListener { onResult(0) }
    }

    // Check số lịch hẹn confirmed+tenant_confirmed của tenant
    // Đặt lịch hẹn
    // [AN NINH ĐẶT LỊCH] Đếm xem khách có đang ôm quá 2 lịch đã chốt không.
    fun checkTenantConfirmedCount(tenantId: String, onResult: (Int) -> Unit) {
        db.collection("appointments")
            .whereEqualTo("tenantId", tenantId)
            .whereIn("status", listOf("confirmed", "tenant_confirmed"))
            .get()
            .addOnSuccessListener { snaps -> onResult(snaps.size()) }
            .addOnFailureListener { onResult(0) }
    }

    // 2. Nhóm hàm: ĐIỀU TRA AN NINH & CHỐNG SPAM (Trước khi đặt)
    /**
     * Kiểm tra giới hạn (Quota) số lần đặt lịch trong ngày của 1 khách đối với 1 phòng.
     * (Ví dụ: Chặn không cho 1 khách đặt liên tục quá 3 lần/ngày vào cùng 1 phòng để chống phá hoại).
     */
    // Giới hạn 5 lượt đặt lịch hẹn cho 1 bài đăng trong ngày -1-
    // Trực tiếp móc nối với Database (Firebase), dùng lệnh query lọc các lịch hẹn có tenantId và roomId trùng khớp được tạo trong "ngày hôm nay",
    // đếm ra tổng số lượng và trả về cho ViewModel.
    fun checkDailyBookingQuotaForRoom(tenantId: String, roomId: String, onResult: (Int) -> Unit) {
        // 1. Tính toán mốc thời gian 00:00:00 của ngày hôm nay
        val startOfDay = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        // 2. Tìm tất cả lịch hẹn của khách này (tenantId) tại phòng này (roomId)
        db.collection("appointments")
            .whereEqualTo("tenantId", tenantId)
            .whereEqualTo("roomId", roomId)
            .get()
            .addOnSuccessListener { snaps ->
                // 3. Đếm số lượng lịch hẹn HỢP LỆ trong ngày hôm nay để trừ lượt
                val used = snaps.documents.count { doc ->
                    val createdAt = doc.getLong("createdAt") ?: 0L
                    val status = doc.getString("status")
                    // Cờ đánh dấu: Lịch này có bị tính vào án phạt trừ lượt không? (Ví dụ: khách quay xe hủy lịch)
                    val isPenalized = doc.getBoolean("isQuotaPenalized") ?: false
                    
                    if (isPenalized) {
                        // Nếu bị đánh dấu phạt: Bỏ qua trạng thái, cứ tạo trong hôm nay là Đếm (Mất 1 lượt)
                        createdAt >= startOfDay
                    } else {
                        // Nếu bình thường: Chỉ đếm những lịch tạo trong hôm nay VÀ KHÔNG bị hủy/từ chối
                        // rejected (Chủ trọ từ chối), cancelled_by_landlord (Chủ trọ hủy), cancelled_by_system (Hệ thống hủy), no_show (Khách hủy)
                        createdAt >= startOfDay && status != "rejected" && status != "cancelled_by_landlord" && status != "cancelled_by_system" && status != "cancelled_by_tenant" && status != "no_show"
                    }
                }
                // 4. Trả về tổng số lượt đã dùng cho ViewModel
                onResult(used)
            }
            // 5. Nếu lỗi mạng, tạm coi như chưa dùng lượt nào (0) để không làm kẹt tính năng đặt lịch
            .addOnFailureListener { onResult(0) }
    }

    // ─── Load confirmed appointments của phòng trong 1 ngày (BottomSheet) ────
    fun loadConfirmedAppointmentsForDate(
        roomId: String, dateStr: String, onResult: (List<Appointment>) -> Unit
    ) {
        db.collection("appointments")
            .whereEqualTo("roomId", roomId)
            .whereEqualTo("appointmentDate", dateStr)
            .whereIn("status", listOf("confirmed", "tenant_confirmed"))
            .get()
            .addOnSuccessListener { snaps ->
                onResult(snaps.documents.mapNotNull { doc ->
                    try { doc.toObject(Appointment::class.java)?.copy(id = doc.id) } catch (_: Exception) { null }
                })
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    // Send notification
    // [ĐẶT LỊCH] Bắn thông báo Push Notification rung máy chủ trọ ngay lập tức.
    fun sendNotification(userId: String, title: String, message: String, type: String) {
        db.collection("notifications").add(hashMapOf(
            "userId" to userId, "title" to title, "message" to message,
            "type" to type, "seen" to false, "isRead" to false,
            "createdAt" to System.currentTimeMillis()
        ))
    }
}
