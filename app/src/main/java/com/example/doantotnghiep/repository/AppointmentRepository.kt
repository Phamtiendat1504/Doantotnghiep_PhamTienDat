package com.example.doantotnghiep.repository

import com.example.doantotnghiep.Model.Room
import com.example.doantotnghiep.Model.User
import com.example.doantotnghiep.Utils.toUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration

data class RoomRentedNotice(
    val id: String,
    val title: String,
    val message: String
)

class AppointmentRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val currentUid: String? get() = auth.currentUser?.uid

    // Tạo ID xác định cho bookedSlots từ roomId + date + time
    // Chuẩn hóa định dạng để tránh lỗi mismatch do định dạng chuỗi không nhất quán
    private fun buildSlotId(roomId: String, date: String, time: String): String {
        return "${roomId}_${date}_${time}"
            .replace("/", "-")
            .replace(":", "-")
            .replace(" ", "_")
    }

    // Gửi yêu cầu đặt lịch hẹn — PHƯƠNG ÁN A:
    // Nhiều người có thể gửi pending cùng khung giờ, slot chỉ bị khóa khi chủ trọ CONFIRM.
    // Không còn dùng Transaction hay tạo bookedSlots tại bước này nữa.
    fun submitBooking(
        appointment: HashMap<String, Any>,
        landlordId: String,
        roomTitle: String,
        fullName: String,
        selectedDateDisplay: String,
        selectedTime: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        appointment["status"] = "pending"
        appointment["createdAt"] = System.currentTimeMillis()
        appointment["hasUnreadUpdate"] = true

        val apptRef = db.collection("appointments").document()
        apptRef.set(appointment)
            .addOnSuccessListener {
                val notification = hashMapOf(
                    "userId" to landlordId,
                    "title" to "Có lịch hẹn mới!",
                    "message" to "$fullName muốn xem phòng \"$roomTitle\" vào $selectedDateDisplay lúc $selectedTime",
                    "type" to "appointment_new",
                    "seen" to false,
                    "createdAt" to System.currentTimeMillis()
                )
                db.collection("notifications").add(notification)
                    .addOnCompleteListener { onSuccess() }
            }
            .addOnFailureListener { e -> onFailure("Lỗi đặt lịch: ${e.message}") }
    }

    // Lắng nghe danh sách lịch hẹn realtime
    fun listenAppointments(
        isLandlord: Boolean,
        onUpdate: (List<Map<String, Any>>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        val uid = auth.currentUser?.uid ?: return object : ListenerRegistration {
            override fun remove() {}
        }
        val field = if (isLandlord) "landlordId" else "tenantId"

        return db.collection("appointments")
            .whereEqualTo(field, uid)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    onError("Lỗi tải lịch hẹn: ${error.message}")
                    return@addSnapshotListener
                }
                val list = (value?.map { doc ->
                    val data = doc.data.toMutableMap()
                    data["id"] = doc.id
                    data
                } ?: emptyList())
                    .sortedByDescending { (it["createdAt"] as? Long) ?: 0L }
                onUpdate(list)
            }
    }

    /**
     * Lắng nghe CẢ 2 chiều lịch hẹn cùng lúc cho user đã xác minh:
     *  - Lịch hẹn HỌ ĐẶT (tenantId = uid)
     *  - Lịch hẹn KHÁCH ĐẶT PHÒNG CỦA HỌ (landlordId = uid)
     * Kết quả được gọi riêng biệt qua 2 callback để Activity phân tab.
     */
    fun listenBothAppointments(
        onTenantUpdate: (List<Map<String, Any>>) -> Unit,
        onLandlordUpdate: (List<Map<String, Any>>) -> Unit,
        onError: (String) -> Unit
    ): Pair<ListenerRegistration, ListenerRegistration> {
        val uid = auth.currentUser?.uid
        val empty = object : ListenerRegistration { override fun remove() {} }
        if (uid == null) return Pair(empty, empty)

        val tenantListener = db.collection("appointments")
            .whereEqualTo("tenantId", uid)
            .addSnapshotListener { value, error ->
                if (error != null) { onError(error.message ?: "Lỗi"); return@addSnapshotListener }
                val list = (value?.map { doc ->
                    val data = doc.data.toMutableMap(); data["id"] = doc.id; data
                } ?: emptyList()).sortedByDescending { (it["createdAt"] as? Long) ?: 0L }
                onTenantUpdate(list)
            }

        val landlordListener = db.collection("appointments")
            .whereEqualTo("landlordId", uid)
            .addSnapshotListener { value, error ->
                if (error != null) { onError(error.message ?: "Lỗi"); return@addSnapshotListener }
                val list = (value?.map { doc ->
                    val data = doc.data.toMutableMap(); data["id"] = doc.id; data
                } ?: emptyList()).sortedByDescending { (it["createdAt"] as? Long) ?: 0L }
                onLandlordUpdate(list)
            }

        return Pair(tenantListener, landlordListener)
    }


    // Lấy role người dùng hiện tại
    fun getCurrentUserRole(onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onFailure("Chưa đăng nhập")
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role") ?: ""
                val isVerified = doc.getBoolean("isVerified") ?: false
                val effectiveRole = when {
                    role == "admin" -> "admin"
                    isVerified -> "verified"
                    else -> "user"
                }
                onSuccess(effectiveRole)
            }
            .addOnFailureListener { e -> onFailure(e.message ?: "Lỗi") }
    }

    // Lấy thông tin phòng + người thuê song song (cho chủ trọ bấm vào lịch hẹn)
    fun loadCurrentUserAppointmentAccess(
        onSuccess: (isHostAccess: Boolean, effectiveRole: String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return onFailure("Chưa đăng nhập")
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val isVerified = doc.getBoolean("isVerified") ?: false
                val role = doc.getString("role").orEmpty()
                val effectiveRole = if (isVerified || role == "admin") "verified" else "user"
                onSuccess(isVerified || role == "admin", effectiveRole)
            }
            .addOnFailureListener { e -> onFailure(e.message ?: "Không thể tải quyền người dùng") }
    }

    fun listenRoomRentedNotices(
        uid: String,
        onNotice: (RoomRentedNotice) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return db.collection("notifications")
            .whereEqualTo("userId", uid)
            .whereEqualTo("type", "room_already_rented")
            .whereEqualTo("seen", false)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    onError(error.message ?: "Không thể nghe thông báo lịch hẹn")
                    return@addSnapshotListener
                }
                if (snapshots == null || snapshots.isEmpty) return@addSnapshotListener

                val doc = snapshots.documents
                    .maxByOrNull { it.getLong("createdAt") ?: 0L }
                    ?: return@addSnapshotListener

                onNotice(
                    RoomRentedNotice(
                        id = doc.id,
                        title = doc.getString("title") ?: "Lịch hẹn đã bị hủy",
                        message = doc.getString("message")
                            ?: "Phòng đã có người thuê. Lịch hẹn của bạn đã bị hủy tự động."
                    )
                )
            }
    }

    fun markRoomRentedNoticeRead(notificationId: String) {
        if (notificationId.isBlank()) return
        db.collection("notifications").document(notificationId).update(
            mapOf(
                "seen" to true,
                "isRead" to true
            )
        )
    }

    // Lấy chi tiết phòng và người thuê — tránh dùng toObject() vì Room có val fields
    // và User có Boolean fields tên bắt đầu bằng "is" gây lỗi deserialization qua reflection
    fun fetchAppointmentDetails(
        roomId: String,
        tenantId: String,
        onRoomLoaded: (Room?) -> Unit,
        onTenantLoaded: (User?) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("rooms").document(roomId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) { onRoomLoaded(null); return@addOnSuccessListener }
                val room = Room(
                    id = doc.id,
                    userId = doc.getString("userId") ?: "",
                    title = doc.getString("title") ?: "",
                    description = doc.getString("description") ?: "",
                    price = doc.getLong("price") ?: 0L,
                    area = doc.getLong("area")?.toInt() ?: 0,
                    address = doc.getString("address") ?: "",
                    ward = doc.getString("ward") ?: "",
                    district = doc.getString("district") ?: "",
                    latitude = doc.getDouble("latitude"),
                    longitude = doc.getDouble("longitude"),
                    roomType = doc.getString("roomType") ?: "",
                    gender = doc.getString("gender") ?: "",
                    peopleCount = doc.getLong("peopleCount")?.toInt() ?: 0,
                    roomCount = doc.getLong("roomCount")?.toInt() ?: 0,
                    rentedCount = doc.getLong("rentedCount")?.toInt() ?: 0,
                    hasWifi = doc.getBoolean("hasWifi") ?: false,
                    hasWater = doc.getBoolean("hasWater") ?: false,
                    hasAirCon = doc.getBoolean("hasAirCon") ?: false,
                    hasWaterHeater = doc.getBoolean("hasWaterHeater") ?: false,
                    hasParking = doc.getBoolean("hasParking") ?: false,
                    hasWasher = doc.getBoolean("hasWasher") ?: false,
                    hasDryingArea = doc.getBoolean("hasDryingArea") ?: false,
                    hasWardrobe = doc.getBoolean("hasWardrobe") ?: false,
                    hasBed = doc.getBoolean("hasBed") ?: false,
                    wifiPrice = doc.getLong("wifiPrice") ?: 0L,
                    electricPrice = doc.getLong("electricPrice") ?: 0L,
                    waterPrice = doc.getLong("waterPrice") ?: 0L,
                    kitchen = doc.getString("kitchen") ?: "",
                    bathroom = doc.getString("bathroom") ?: "",
                    pet = doc.getString("pet") ?: "",
                    petName = doc.getString("petName") ?: "",
                    petCount = doc.getLong("petCount")?.toInt() ?: 0,
                    curfew = doc.getString("curfew") ?: "",
                    curfewTime = doc.getString("curfewTime") ?: "",
                    genderPrefer = doc.getString("genderPrefer") ?: "",
                    hasMotorbike = doc.getBoolean("hasMotorbike") ?: false,
                    motorbikeFee = doc.getLong("motorbikeFee") ?: 0L,
                    hasEBike = doc.getBoolean("hasEBike") ?: false,
                    eBikeFee = doc.getLong("eBikeFee") ?: 0L,
                    hasBicycle = doc.getBoolean("hasBicycle") ?: false,
                    bicycleFee = doc.getLong("bicycleFee") ?: 0L,
                    depositMonths = doc.getLong("depositMonths")?.toInt() ?: 0,
                    depositAmount = doc.getLong("depositAmount") ?: 0L,
                    imageUrls = (doc.get("imageUrls") as? List<*>)?.filterIsInstance<String>() ?: listOf(),
                    ownerName = doc.getString("ownerName") ?: "",
                    ownerPhone = doc.getString("ownerPhone") ?: "",
                    ownerGender = doc.getString("ownerGender") ?: "",
                    ownerAvatarUrl = doc.getString("ownerAvatarUrl") ?: "",
                    status = doc.getString("status") ?: "pending",
                    isFeatured = doc.getBoolean("isFeatured") ?: false,
                    createdAt = doc.getLong("createdAt") ?: 0L
                )
                onRoomLoaded(room)
            }
            .addOnFailureListener { e -> onError("Lỗi khi lấy thông tin phòng: ${e.message}") }

        db.collection("users").document(tenantId).get()
            .addOnSuccessListener { doc ->
                onTenantLoaded(doc.toUser())
            }
            .addOnFailureListener { onError("Không thể lấy thông tin khách hàng") }
    }

    // Người thuê xác nhận đã đến xem phòng (tenant_confirmed) — không động đến bookedSlots
    fun tenantConfirmAppointment(
        appointmentId: String, landlordId: String, roomTitle: String,
        roomId: String, date: String, time: String,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        db.collection("appointments").document(appointmentId)
            .update(mapOf(
                "status" to "tenant_confirmed",
                "hasUnreadUpdate" to true,
                "updatedAt" to System.currentTimeMillis()
            ))
            .addOnSuccessListener {
                sendNotification(
                    userId = landlordId,
                    title = "Người thuê đã xác nhận!",
                    message = "Khách đã xác nhận xem phòng \"$roomTitle\". Nếu thỏa thuận thuê thành công, hãy bấm 'Xác nhận đã cho thuê' để hệ thống tự động ẩn bài và hủy lịch hẹn khác giúp bạn nhé!",
                    type = "appointment_tenant_confirmed"
                )
                onSuccess()
            }
            .addOnFailureListener { e -> onFailure("Xác nhận lịch thất bại: ${e.message}") }
    }

    // Chủ trọ xác nhận lịch hẹn — PHƯƠNG ÁN A:
    // Dùng Transaction để kiểm tra slot trống, tạo bookedSlots và tự động từ chối các lịch pending trùng giờ.
    fun confirmAppointment(
        appointmentId: String, tenantId: String, roomTitle: String, date: String, time: String,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        val apptRef = db.collection("appointments").document(appointmentId)

        // Đọc appointment để lấy roomId trước khi chạy Transaction
        apptRef.get().addOnSuccessListener { apptDoc ->
            if (!apptDoc.exists()) { onFailure("Không tìm thấy lịch hẹn"); return@addOnSuccessListener }
            val roomId = apptDoc.getString("roomId") ?: run { onFailure("Lịch hẹn thiếu thông tin phòng"); return@addOnSuccessListener }

            val slotId = buildSlotId(roomId, date, time)
            val slotRef = db.collection("bookedSlots").document(slotId)

            db.runTransaction { tx ->
                val slotSnap = tx.get(slotRef)
                if (slotSnap.exists()) {
                    // Slot đã bị chủ trọ confirm cho người khác trước
                    throw FirebaseFirestoreException(
                        "Khung giờ này đã được xác nhận cho khách hàng khác rồi.",
                        FirebaseFirestoreException.Code.ABORTED
                    )
                }
                // Cập nhật lịch hẹn thành confirmed
                tx.update(apptRef, mapOf(
                    "status" to "confirmed",
                    "hasUnreadUpdate" to true,
                    "updatedAt" to System.currentTimeMillis()
                ))
                // Tạo slot khóa trong bookedSlots
                val dateDisplay: String = apptDoc.getString("dateDisplay") ?: date
                tx.set(slotRef, hashMapOf<String, Any>(
                    "roomId" to roomId,
                    "appointmentId" to appointmentId,
                    "date" to date,
                    "dateDisplay" to dateDisplay,
                    "time" to time,
                    "status" to "confirmed"
                ))
            }.addOnSuccessListener {
                // Gửi thông báo xác nhận cho người thuê được chọn
                sendNotification(
                    userId = tenantId,
                    title = "Lịch hẹn đã được xác nhận!",
                    message = "Chủ trọ đã xác nhận lịch hẹn xem phòng \"$roomTitle\" vào $date lúc $time. Hẹn gặp bạn!",
                    type = "appointment_confirmed"
                )
                // Tự động từ chối các lịch pending khác cùng phòng cùng khung giờ
                rejectOverlappingPendingAppointments(roomId, date, time, excludeAppointmentId = appointmentId, roomTitle = roomTitle)
                onSuccess()
            }.addOnFailureListener { e ->
                val msg = if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.ABORTED)
                    e.message ?: "Khung giờ đã được đặt bởi khách khác"
                else
                    "Xác nhận thất bại: ${e.message}"
                onFailure(msg)
            }
        }.addOnFailureListener { e -> onFailure("Không thể đọc lịch hẹn: ${e.message}") }
    }

    // Tự động hủy tất cả các lịch hẹn pending trùng phòng + ngày + giờ do trùng lịch (trừ lịch vừa được confirm)
    private fun rejectOverlappingPendingAppointments(
        roomId: String, date: String, time: String,
        excludeAppointmentId: String, roomTitle: String
    ) {
        db.collection("appointments")
            .whereEqualTo("roomId", roomId)
            .whereEqualTo("date", date)
            .whereEqualTo("time", time)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { snapshots ->
                snapshots.documents.forEach { doc ->
                    if (doc.id == excludeAppointmentId) return@forEach
                    doc.reference.update(mapOf(
                        "status" to "cancelled_by_system",
                        "rejectReason" to "Khung giờ này đã được xác nhận cho khách hàng khác.",
                        "hasUnreadUpdate" to true,
                        "updatedAt" to System.currentTimeMillis()
                    ))
                    val otherTenantId = doc.getString("tenantId") ?: return@forEach
                    sendNotification(
                        userId = otherTenantId,
                        title = "Lịch hẹn bị hủy do trùng giờ",
                        message = "Rất tiếc, khung giờ bạn đăng ký xem phòng \"$roomTitle\" đã được xác nhận cho khách hàng khác. Bạn có thể chọn khung giờ trống khác để đặt lại ngay!",
                        type = "appointment_cancelled"
                    )
                }
            }
    }

    // Chủ trọ từ chối lịch hẹn pending — không cần xóa bookedSlots vì pending không có slot
    fun rejectAppointment(
        appointmentId: String, tenantId: String, roomTitle: String, reason: String,
        roomId: String, date: String, time: String,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        db.collection("appointments").document(appointmentId)
            .update(mapOf(
                "status" to "rejected",
                "rejectReason" to reason,
                "hasUnreadUpdate" to true,
                "updatedAt" to System.currentTimeMillis()
            ))
            .addOnSuccessListener {
                val msg = if (reason.isNotEmpty())
                    "Chủ trọ đã từ chối lịch hẹn xem phòng \"$roomTitle\". Lý do: $reason"
                else
                    "Chủ trọ đã từ chối lịch hẹn xem phòng \"$roomTitle\"."
                sendNotification(userId = tenantId, title = "Lịch hẹn bị từ chối", message = msg, type = "appointment_rejected")
                onSuccess()
            }
            .addOnFailureListener { e -> onFailure("Từ chối thất bại: ${e.message}") }
    }

    // Đánh dấu đã cho thuê — Cập nhật status phòng thành "rented" thay vì xóa hẳn và dọn dẹp các lịch hẹn khác đồng bộ bằng WriteBatch
    fun markAsRented(
        appointmentId: String, roomId: String, tenantId: String, roomTitle: String,
        date: String, time: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val uid = currentUid ?: return onFailure("Chưa đăng nhập")
        val roomRef = db.collection("rooms").document(roomId)
        val activeStatuses = setOf("pending", "confirmed", "tenant_confirmed")

        // Truy vấn tất cả thông tin liên quan trước để gộp vào 1 batch duy nhất
        db.collection("appointments")
            .whereEqualTo("roomId", roomId)
            .whereEqualTo("landlordId", uid)
            .get()
            .addOnSuccessListener { apptSnaps ->
                db.collection("savedPosts")
                    .whereEqualTo("roomId", roomId)
                    .get()
                    .addOnSuccessListener { savedSnaps ->
                        val batch = db.batch()
                        
                        // 1. Cập nhật trạng thái phòng thành "rented"
                        batch.update(roomRef, mapOf(
                            "status" to "rented",
                            "updatedAt" to System.currentTimeMillis()
                        ))
                        
                        // 2. Cập nhật lịch hẹn hiện tại thành completed_rented
                        val currentApptRef = db.collection("appointments").document(appointmentId)
                        batch.update(currentApptRef, mapOf(
                            "status" to "completed_rented",
                            "updatedAt" to System.currentTimeMillis()
                        ))
                        
                        // Xóa slot của lịch hẹn hiện tại.
                        // Slot tồn tại khi status là "confirmed" hoặc "tenant_confirmed"
                        // vì tenantConfirmAppointment không xóa slot.
                        val currentApptDoc = apptSnaps.documents.find { it.id == appointmentId }
                        val currentApptStatus = currentApptDoc?.getString("status") ?: ""
                        if (currentApptStatus == "confirmed" || currentApptStatus == "tenant_confirmed") {
                            val currentSlotRef = db.collection("bookedSlots").document(buildSlotId(roomId, date, time))
                            batch.delete(currentSlotRef)
                        }
                        
                        // 3. Xóa các bài lưu
                        savedSnaps.documents.forEach { batch.delete(it.reference) }
                        
                        // 4. Hủy các lịch hẹn khác của phòng này và xóa slot tương ứng
                        apptSnaps.documents.forEach { doc ->
                            if (doc.id != appointmentId) {
                                val status = doc.getString("status") ?: ""
                                if (status in activeStatuses) {
                                    batch.update(doc.reference, mapOf(
                                        "status" to "cancelled_by_system",
                                        "hasUnreadUpdate" to true,
                                        "updatedAt" to System.currentTimeMillis()
                                    ))
                                    
                                    // Chỉ xóa slot nếu lịch đã confirmed (pending không có slot, tenant_confirmed đã không còn slot)
                                    if (status == "confirmed") {
                                        val otherDate = doc.getString("date") ?: ""
                                        val otherTime = doc.getString("time") ?: ""
                                        if (otherDate.isNotEmpty() && otherTime.isNotEmpty()) {
                                            val otherSlotRef = db.collection("bookedSlots").document(buildSlotId(roomId, otherDate, otherTime))
                                            batch.delete(otherSlotRef)
                                        }
                                    }
                                }
                            }
                        }
                        
                        batch.commit()
                            .addOnSuccessListener {
                                // Gửi thông báo cho người thuê hiện tại
                                sendNotification(
                                    userId = tenantId,
                                    title = "Thuê phòng thành công!",
                                    message = "Chúc mừng bạn đã thuê thành công phòng \"$roomTitle\".",
                                    type = "room_rented_success"
                                )
                                
                                // Gửi thông báo cho các người thuê khác bị hủy lịch
                                apptSnaps.documents.forEach { doc ->
                                    if (doc.id != appointmentId) {
                                        val status = doc.getString("status") ?: ""
                                        if (status in activeStatuses) {
                                            val otherTenantId = doc.getString("tenantId") ?: ""
                                            if (otherTenantId.isNotEmpty()) {
                                                sendNotification(
                                                    userId = otherTenantId,
                                                    title = "Phòng đã có người thuê",
                                                    message = "Phòng \"$roomTitle\" mà bạn hẹn xem đã được thuê bởi người khác. Lịch hẹn của bạn đã bị hủy tự động.",
                                                    type = "room_already_rented"
                                                )
                                            }
                                        }
                                    }
                                }
                                onSuccess()
                            }
                            .addOnFailureListener { e ->
                                onFailure("Lỗi cập nhật trạng thái cho thuê: ${e.message}")
                            }
                    }
                    .addOnFailureListener { e -> onFailure("Lỗi truy xuất danh sách lưu phòng: ${e.message}") }
            }
            .addOnFailureListener { e -> onFailure("Lỗi truy xuất danh sách lịch hẹn khác: ${e.message}") }
    }


    // Người thuê hủy lịch hẹn đang pending — không cần xóa bookedSlots vì pending không có slot
    fun cancelPendingAppointment(
        appointmentId: String, landlordId: String, roomTitle: String,
        roomId: String, date: String, time: String,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        db.collection("appointments").document(appointmentId)
            .update(mapOf(
                "status" to "cancelled_by_tenant",
                "hasUnreadUpdate" to true,
                "updatedAt" to System.currentTimeMillis()
            ))
            .addOnSuccessListener {
                sendNotification(
                    userId = landlordId,
                    title = "Lịch hẹn đã bị hủy",
                    message = "Người thuê đã hủy yêu cầu đặt lịch xem phòng \"$roomTitle\"",
                    type = "appointment_cancelled"
                )
                onSuccess()
            }
            .addOnFailureListener { e -> onFailure("Hủy lịch thất bại: ${e.message}") }
    }

    // Người thuê đổi ngày/giờ lịch hẹn đang pending — chỉ cần cập nhật appointment, không động bookedSlots
    fun editPendingAppointment(
        appointmentId: String, landlordId: String, roomTitle: String,
        newDate: String, newDateDisplay: String, newTime: String,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        val apptRef = db.collection("appointments").document(appointmentId)
        apptRef.update(mapOf(
            "date" to newDate,
            "dateDisplay" to newDateDisplay,
            "time" to newTime,
            "updatedAt" to System.currentTimeMillis()
        ))
        .addOnSuccessListener {
            sendNotification(
                userId = landlordId,
                title = "Lịch hẹn đã được đổi giờ",
                message = "Người thuê đã đổi lịch xem phòng \"$roomTitle\" sang $newDateDisplay lúc $newTime",
                type = "appointment_edited"
            )
            onSuccess()
        }
        .addOnFailureListener { e -> onFailure("Đổi lịch thất bại: ${e.message}") }
    }

    // Người thuê huỷ lịch hẹn sau khi chủ trọ đã xác nhận — cần xóa bookedSlots vì lịch này đã có slot
    fun tenantRejectAppointment(
        appointmentId: String, landlordId: String, roomTitle: String,
        roomId: String, date: String, time: String,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        val batch = db.batch()
        val apptRef = db.collection("appointments").document(appointmentId)
        batch.update(apptRef, mapOf(
            "status" to "cancelled_by_tenant",
            "hasUnreadUpdate" to true,
            "updatedAt" to System.currentTimeMillis()
        ))
        // Lịch confirmed đã có slot trong bookedSlots → xóa để mở lại khung giờ
        val slotRef = db.collection("bookedSlots").document(buildSlotId(roomId, date, time))
        batch.delete(slotRef)

        batch.commit()
            .addOnSuccessListener {
                sendNotification(
                    userId = landlordId,
                    title = "Người thuê huỷ lịch hẹn",
                    message = "Khách đã huỷ lịch xem phòng \"$roomTitle\"",
                    type = "appointment_cancelled"
                )
                onSuccess()
            }
            .addOnFailureListener { e -> onFailure("Huỷ lịch thất bại: ${e.message}") }
    }

    // Kiểm tra xem người dùng đã có lịch hẹn active hoặc đã thuê phòng này chưa
    fun checkExistingAppointment(
        tenantId: String,
        roomId: String,
        onResult: (Boolean, String?, String?, Long?) -> Unit
    ) {
        db.collection("appointments")
            .whereEqualTo("tenantId", tenantId)
            .whereEqualTo("roomId", roomId)
            .get()
            .addOnSuccessListener { snapshots ->
                if (snapshots.isEmpty) {
                    onResult(false, null, null, null)
                    return@addOnSuccessListener
                }

                val appointments = snapshots.documents.sortedByDescending { it.getLong("createdAt") ?: 0L }
                val latest = appointments[0]
                val status = latest.getString("status") ?: ""

                if (status in listOf("pending", "confirmed", "tenant_confirmed")) {
                    onResult(true, latest.id, "active", null)
                    return@addOnSuccessListener
                }

                if (status == "completed_rented") {
                    onResult(true, latest.id, "rented", null)
                    return@addOnSuccessListener
                }

                onResult(false, null, null, null)
            }
            .addOnFailureListener { onResult(false, null, null, null) }
    }

    // Kiểm tra giới hạn 3 lần đặt lịch mỗi ngày (tính tất cả lần gửi, reset lúc 00:00)
    fun checkDailyBookingLimit(
        tenantId: String,
        onAllowed: (remaining: Int) -> Unit,
        onBlocked: (usedToday: Int) -> Unit
    ) {
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val todayMidnight = calendar.timeInMillis

        db.collection("appointments")
            .whereEqualTo("tenantId", tenantId)
            .whereGreaterThanOrEqualTo("createdAt", todayMidnight)
            .get()
            .addOnSuccessListener { snap ->
                val usedToday = snap.size()
                val limit = 3
                if (usedToday >= limit) onBlocked(usedToday)
                else onAllowed(limit - usedToday)
            }
            .addOnFailureListener { onAllowed(3) }
    }

    // Kiểm tra các khung giờ đã BỊ KHÓA (confirmed) cho cùng một phòng — dùng để hiển thị lịch bận cho khách thuê
    // Phương án A: chỉ đếm slot đã confirmed, không đếm pending (pending không phải slot bị khóa)
    fun checkTimeConflicts(
        roomId: String,
        onUpdate: (Map<String, Int>) -> Unit
    ): ListenerRegistration {
        return db.collection("bookedSlots")
            .whereEqualTo("roomId", roomId)
            .whereEqualTo("status", "confirmed")
            .addSnapshotListener { snapshots, _ ->
                val conflicts = mutableMapOf<String, Int>()
                snapshots?.forEach { doc ->
                    val date = doc.getString("date") ?: ""
                    val time = doc.getString("time") ?: ""
                    if (date.isNotEmpty() && time.isNotEmpty()) {
                        val key = "${date}_${time}"
                            .replace("/", "-")
                            .replace(":", "-")
                            .replace(" ", "_")
                        conflicts[key] = (conflicts[key] ?: 0) + 1
                    }
                }
                onUpdate(conflicts)
            }
    }

    // Gửi thông báo
    fun sendNotification(userId: String, title: String, message: String, type: String) {
        db.collection("notifications").add(
            hashMapOf(
                "userId" to userId,
                "title" to title,
                "message" to message,
                "type" to type,
                "seen" to false,
                "createdAt" to System.currentTimeMillis()
            )
        )
    }

    /**
     * Badge lịch hẹn: chỉ đếm những lịch hẹn có CẬP NHẬT MỚI chưa xem.
     * Field `hasUnreadUpdate` được set = true khi có status mới, reset = false khi user vào xem.
     */
    fun listenBadge(
        uid: String,
        role: String,
        onResult: (Int) -> Unit
    ): ListenerRegistration {
        val isHostAccess = role == "admin" || role == "verified"
        val hostStatuses = setOf("pending", "tenant_confirmed")
        return if (isHostAccess) {
            db.collection("appointments")
                .whereEqualTo("landlordId", uid)
                .addSnapshotListener { snap, _ ->
                    // Lọc phía client để tránh Composite Index
                    val count = snap?.count {
                        it.getString("status") in hostStatuses &&
                        it.getBoolean("hasUnreadUpdate") == true
                    } ?: 0
                    onResult(count)
                }
        } else {
            db.collection("appointments")
                .whereEqualTo("tenantId", uid)
                .addSnapshotListener { snap, _ ->
                    val count = snap?.count {
                        it.getString("status") == "confirmed" &&
                        it.getBoolean("hasUnreadUpdate") == true
                    } ?: 0
                    onResult(count)
                }
        }
    }

    /**
     * Đánh dấu tất cả lịch hẹn của user là đã đọc (reset hasUnreadUpdate = false).
     * Gọi khi user vào màn hình MyAppointmentsActivity.
     */
    fun markAllAppointmentsRead(uid: String, role: String) {
        val isHostAccess = role == "admin" || role == "verified"
        val field = if (isHostAccess) "landlordId" else "tenantId"
        db.collection("appointments")
            .whereEqualTo(field, uid)
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) return@addOnSuccessListener
                // Lọc phía client để tránh Composite Index
                val toUpdate = snap.documents.filter { it.getBoolean("hasUnreadUpdate") == true }
                if (toUpdate.isEmpty()) return@addOnSuccessListener
                val batch = db.batch()
                toUpdate.forEach { doc ->
                    batch.update(doc.reference, "hasUnreadUpdate", false)
                }
                batch.commit()
            }
    }
}
