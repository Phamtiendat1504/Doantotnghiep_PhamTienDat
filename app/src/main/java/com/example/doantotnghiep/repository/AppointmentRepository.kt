package com.example.doantotnghiep.repository

import com.example.doantotnghiep.Model.Room
import com.example.doantotnghiep.Model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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

    // Gửi yêu cầu đặt lịch hẹn
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

        db.collection("appointments").add(appointment)
            .addOnSuccessListener { apptRef ->
                val roomId = appointment["roomId"] as? String ?: ""
                val date = appointment["date"] as? String ?: ""
                val dateDisplay = appointment["dateDisplay"] as? String ?: ""
                val time = appointment["time"] as? String ?: ""

                if (roomId.isNotEmpty()) {
                    val slot = hashMapOf(
                        "roomId" to roomId,
                        "appointmentId" to apptRef.id,
                        "date" to date,
                        "dateDisplay" to dateDisplay,
                        "time" to time,
                        "status" to "pending"
                    )
                    db.collection("bookedSlots").document(apptRef.id).set(slot)
                }

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
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    onError(error.message ?: "Không thể nghe thông báo lịch hẹn")
                    return@addSnapshotListener
                }
                if (snapshots == null || snapshots.isEmpty) return@addSnapshotListener

                // Lọc client-side để tránh Composite Index
                val doc = snapshots.documents
                    .filter {
                        it.getString("type") == "room_already_rented" &&
                        it.getBoolean("seen") == false
                    }
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
    fun fetchAppointmentDetails(
        roomId: String,
        tenantId: String,
        onRoomLoaded: (Room?) -> Unit,
        onTenantLoaded: (User?) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("rooms").document(roomId).get()
            .addOnSuccessListener { doc -> onRoomLoaded(if (doc.exists()) doc.toObject(Room::class.java) else null) }
            .addOnFailureListener { e -> onError("Lỗi khi lấy thông tin phòng: ${e.message}") }

        db.collection("users").document(tenantId).get()
            .addOnSuccessListener { doc -> onTenantLoaded(if (doc.exists()) doc.toObject(User::class.java) else null) }
            .addOnFailureListener { onError("Không thể lấy thông tin khách hàng") }
    }

    // Người thuê xác nhận lịch hẹn
    fun tenantConfirmAppointment(
        appointmentId: String, landlordId: String, roomTitle: String,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        db.collection("appointments").document(appointmentId)
            .update(mapOf(
                "status" to "tenant_confirmed",
                "hasUnreadUpdate" to true,
                "updatedAt" to System.currentTimeMillis() // Ghi nhận lúc xác nhận đi xem
            ))
            .addOnSuccessListener {
                db.collection("bookedSlots").document(appointmentId).delete()
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

    // Chủ trọ xác nhận lịch hẹn
    fun confirmAppointment(
        appointmentId: String, tenantId: String, roomTitle: String, date: String, time: String,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        db.collection("appointments").document(appointmentId)
            .update(mapOf(
                "status" to "confirmed",
                "hasUnreadUpdate" to true
            ))
            .addOnSuccessListener {
                val slotRef = db.collection("bookedSlots").document(appointmentId)
                slotRef.get().addOnSuccessListener { slotDoc ->
                    if (slotDoc.exists()) {
                        slotRef.update("status", "confirmed")
                    }
                }
                sendNotification(
                    userId = tenantId,
                    title = "Lịch hẹn đã được xác nhận!",
                    message = "Chủ trọ đã xác nhận lịch hẹn xem phòng \"$roomTitle\" vào $date lúc $time. Hẹn gặp bạn!",
                    type = "appointment_confirmed"
                )
                onSuccess()
            }
            .addOnFailureListener { e -> onFailure("Xác nhận thất bại: ${e.message}") }
    }

    // Chủ trọ từ chối lịch hẹn
    fun rejectAppointment(
        appointmentId: String, tenantId: String, roomTitle: String, reason: String,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        db.collection("appointments").document(appointmentId)
            .update(mapOf(
                "status" to "rejected",
                "rejectReason" to reason,
                "hasUnreadUpdate" to true,
                "updatedAt" to System.currentTimeMillis() // Ghi nhận lúc từ chối
            ))
            .addOnSuccessListener {
                db.collection("bookedSlots").document(appointmentId).delete()
                val msg = if (reason.isNotEmpty())
                    "Chủ trọ đã từ chối lịch hẹn xem phòng \"$roomTitle\". Lý do: $reason"
                else
                    "Chủ trọ đã từ chối lịch hẹn xem phòng \"$roomTitle\"."
                sendNotification(userId = tenantId, title = "Lịch hẹn bị từ chối", message = msg, type = "appointment_rejected")
                onSuccess()
            }
            .addOnFailureListener { e -> onFailure("Từ chối thất bại: ${e.message}") }
    }

    // Đánh dấu đã cho thuê — Xóa ảnh Storage và xóa hẳn bài đăng khỏi Firestore
    fun markAsRented(
        appointmentId: String, roomId: String, tenantId: String, roomTitle: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val roomRef = db.collection("rooms").document(roomId)
        
        // 1. Lấy thông tin bài đăng để lấy danh sách ảnh trước khi xóa
        roomRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val imageUrls = (document.get("imageUrls") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                
                // 2. Xóa ảnh trên Storage
                if (imageUrls.isNotEmpty()) {
                    val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
                    imageUrls.forEach { url ->
                        try {
                            storage.getReferenceFromUrl(url).delete()
                        } catch (e: Exception) {
                            // Bỏ qua nếu ảnh không tồn tại hoặc lỗi nhỏ
                        }
                    }
                }

                // 3. Thực hiện Batch update + xóa room
                val batch = db.batch()

                // Cập nhật lịch hẹn thành công
                val apptRef = db.collection("appointments").document(appointmentId)
                batch.update(apptRef, "status", "completed_rented")

                // Xóa hẳn room để tránh phình dữ liệu
                batch.delete(roomRef)

                // Xóa slot đặt lịch
                val slotRef = db.collection("bookedSlots").document(appointmentId)
                batch.delete(slotRef)

                batch.commit()
                    .addOnSuccessListener {
                        deleteSavedPostsByRoom(roomId)
                        // Gửi thông báo cho người thuê
                        sendNotification(
                            userId = tenantId,
                            title = "Thuê phòng thành công!",
                            message = "Chúc mừng bạn đã thuê thành công phòng \"$roomTitle\".",
                            type = "room_rented_success"
                        )
                        // Thông báo cho các lịch hẹn khác của phòng này
                        notifyOtherAppointments(roomId, appointmentId, roomTitle)
                        onSuccess()
                    }
                    .addOnFailureListener { e -> onFailure(e.message ?: "Lỗi khi cập nhật trạng thái") }
            } else {
                onFailure("Không tìm thấy bài đăng")
            }
        }.addOnFailureListener { e -> onFailure("Lỗi truy xuất bài đăng: ${e.message}") }
    }

    private fun deleteSavedPostsByRoom(roomId: String) {
        db.collection("savedPosts")
            .whereEqualTo("roomId", roomId)
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) return@addOnSuccessListener
                val batch = db.batch()
                docs.documents.forEach { batch.delete(it.reference) }
                batch.commit()
            }
    }

    /**
     * Tìm tất cả lịch hẹn còn hoạt động của phòng này (trừ cái vừa chốt),
     * gửi thông báo cho từng người thuê và hủy lịch của họ.
     *
     * KHÔNG dùng .whereIn() để tránh bắt buộc phải tạo Composite Index trên Firebase Console.
     * Thay vào đó, lấy toàn bộ lịch hẹn của phòng rồi lọc status bằng code.
     */
    private fun notifyOtherAppointments(roomId: String, currentApptId: String, roomTitle: String) {
        val activeStatuses = setOf("pending", "confirmed", "tenant_confirmed")

        db.collection("appointments")
            .whereEqualTo("roomId", roomId)  // Chỉ 1 điều kiện → không cần Composite Index
            .get()
            .addOnSuccessListener { snapshots ->
                for (doc in snapshots) {
                    // Bỏ qua lịch hẹn vừa được chốt thuê thành công
                    if (doc.id == currentApptId) continue

                    val status = doc.getString("status") ?: ""
                    // Lọc status bằng code thay vì whereIn
                    if (status !in activeStatuses) continue

                    val otherTenantId = doc.getString("tenantId") ?: continue

                    // Gửi thông báo cho người thuê bị hủy lịch
                    sendNotification(
                        userId = otherTenantId,
                        title = "Phòng đã có người thuê",
                        message = "Phòng \"$roomTitle\" mà bạn hẹn xem đã được thuê bởi người khác. Lịch hẹn của bạn đã bị hủy tự động.",
                        type = "room_already_rented"
                    )

                    // Cập nhật trạng thái lịch hẹn thành hủy hệ thống
                    db.collection("appointments").document(doc.id)
                        .update(mapOf(
                            "status" to "cancelled_by_system",
                            "hasUnreadUpdate" to true
                        ))

                    // FIX LỖI 2: Xóa slot đặt lịch — tránh rác dữ liệu treo vĩnh viễn
                    db.collection("bookedSlots").document(doc.id).delete()
                }
            }
            .addOnFailureListener { e ->
                // FIX: Thêm log lỗi thay vì im lặng thất bại
                android.util.Log.e("AppointmentRepo", "notifyOtherAppointments thất bại: ${e.message}")
            }
    }


    // Người thuê hủy lịch hẹn đang pending
    fun cancelPendingAppointment(
        appointmentId: String, landlordId: String, roomTitle: String,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        db.collection("appointments").document(appointmentId).delete()
            .addOnSuccessListener {
                db.collection("bookedSlots").document(appointmentId).delete()
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

    // Người thuê đổi ngày/giờ lịch hẹn đang pending
    fun editPendingAppointment(
        appointmentId: String, landlordId: String, roomTitle: String,
        newDate: String, newDateDisplay: String, newTime: String,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        val updates = mapOf("date" to newDate, "dateDisplay" to newDateDisplay, "time" to newTime)
        db.collection("appointments").document(appointmentId).update(updates)
            .addOnSuccessListener {
                db.collection("bookedSlots").document(appointmentId).update(updates)
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

    // Người thuê huỷ lịch hẹn sau khi chủ trọ đã xác nhận
    fun tenantRejectAppointment(
        appointmentId: String, landlordId: String, roomTitle: String,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        db.collection("appointments").document(appointmentId)
            .update(mapOf(
                "status" to "cancelled_by_tenant",
                "hasUnreadUpdate" to true,
                "updatedAt" to System.currentTimeMillis() // Ghi nhận lúc hủy
            ))
            .addOnSuccessListener {
                db.collection("bookedSlots").document(appointmentId).delete()
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

    // Kiểm tra xem người dùng đã có lịch hẹn cho phòng này chưa
    fun checkExistingAppointment(
        tenantId: String,
        roomId: String,
        onResult: (Boolean, String?, String?, Long?) -> Unit // Thêm Long cho timestamp
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
                val currentTime = System.currentTimeMillis()

                if (status in listOf("pending", "confirmed")) {
                    onResult(true, latest.id, "active", null)
                    return@addOnSuccessListener
                }

                if (status == "completed_rented") {
                    onResult(true, latest.id, "rented", null)
                    return@addOnSuccessListener
                }

                val cooldownMs = 3 * 24 * 60 * 60 * 1000L
                val lastActivityTime = latest.getLong("updatedAt") ?: latest.getLong("createdAt") ?: 0L

                // PHẠT 3 NGÀY NẾU: Đã xác nhận đi xem (tenant_confirmed), Tự hủy, hoặc Bị từ chối
                if ((status == "tenant_confirmed" || status == "cancelled_by_tenant" || status == "rejected") 
                    && (currentTime - lastActivityTime < cooldownMs)) {
                    onResult(true, latest.id, "cooldown", lastActivityTime)
                } else {
                    onResult(false, null, null, null)
                }
            }
            .addOnFailureListener { onResult(false, null, null, null) }
    }

    // Kiểm tra các lịch hẹn bị trùng giờ cho cùng một phòng
    fun checkTimeConflicts(
        roomId: String,
        onUpdate: (Map<String, Int>) -> Unit // Map<"date_time", count>
    ) {
        val activeStatuses = setOf("pending", "confirmed", "tenant_confirmed")
        db.collection("appointments")
            .whereEqualTo("roomId", roomId)
            .addSnapshotListener { snapshots, _ ->
                val conflicts = mutableMapOf<String, Int>()
                // Lọc status phía client để tránh Composite Index
                snapshots?.filter { it.getString("status") in activeStatuses }?.forEach { doc ->
                    val date = doc.getString("date") ?: ""
                    val time = doc.getString("time") ?: ""
                    val key = "${date}_${time}"
                    conflicts[key] = (conflicts[key] ?: 0) + 1
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
     * Lắng nghe số lịch hẹn cần xử lý theo vai trò.
     * - Chủ trọ/Admin: đếm lịch hẹn gửi đến đang ở trạng thái cần hành động (pending, tenant_confirmed).
     * - Người thuê: đếm lịch hẹn của mình đang chờ xác nhận đi xem (confirmed).
     * Chỉ tạo 1 listener thay vì 2, tiết kiệm 50% chi phí đọc Firebase.
     */
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
