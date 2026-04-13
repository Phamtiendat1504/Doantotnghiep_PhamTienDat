package com.example.doantotnghiep.repository

import com.example.doantotnghiep.Model.Room
import com.example.doantotnghiep.Model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

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
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    onError("Lỗi tải lịch hẹn: ${error.message}")
                    return@addSnapshotListener
                }
                val list = value?.map { doc ->
                    val data = doc.data.toMutableMap()
                    data["id"] = doc.id
                    data
                } ?: emptyList()
                onUpdate(list)
            }
    }

    // Lấy role người dùng hiện tại
    fun getCurrentUserRole(onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onFailure("Chưa đăng nhập")
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc -> onSuccess(doc.getString("role") ?: "tenant") }
            .addOnFailureListener { e -> onFailure(e.message ?: "Lỗi") }
    }

    // Lấy thông tin phòng + người thuê song song (cho chủ trọ bấm vào lịch hẹn)
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
            .update("status", "confirmed")
            .addOnSuccessListener {
                db.collection("bookedSlots").document(appointmentId).update("status", "confirmed")
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

    // Đánh dấu đã cho thuê — Xóa ảnh Storage và cập nhật status bài đăng
    fun markAsRented(
        appointmentId: String, roomId: String, tenantId: String, roomTitle: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val roomRef = db.collection("rooms").document(roomId)
        
        // 1. Lấy thông tin bài đăng để lấy danh sách ảnh trước khi xóa
        roomRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val imageUrls = document.get("imageUrls") as? List<String> ?: emptyList()
                
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

                // 3. Thực hiện Batch update để dọn dẹp dữ liệu bài đăng
                val batch = db.batch()

                // Cập nhật lịch hẹn thành công
                val apptRef = db.collection("appointments").document(appointmentId)
                batch.update(apptRef, "status", "completed_rented")

                // Cập nhật phòng: Giữ lại thông tin cơ bản cho Admin, xóa sạch "nội dung nặng"
                batch.update(roomRef, mapOf(
                    "status" to "rented",
                    "rentedAt" to System.currentTimeMillis(),
                    "imageUrls" to emptyList<String>(), // Xóa link ảnh
                    "description" to "Phòng đã cho thuê - Dữ liệu đã được dọn dẹp", // Xóa mô tả
                    "videoUrl" to null // Xóa video nếu có
                ))

                // Xóa slot đặt lịch
                val slotRef = db.collection("bookedSlots").document(appointmentId)
                batch.delete(slotRef)

                batch.commit()
                    .addOnSuccessListener {
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

    private fun notifyOtherAppointments(roomId: String, currentApptId: String, roomTitle: String) {
        db.collection("appointments")
            .whereEqualTo("roomId", roomId)
            .whereIn("status", listOf("pending", "confirmed", "tenant_confirmed"))
            .get()
            .addOnSuccessListener { snapshots ->
                for (doc in snapshots) {
                    if (doc.id == currentApptId) continue
                    
                    val otherTenantId = doc.getString("tenantId") ?: continue
                    sendNotification(
                        userId = otherTenantId,
                        title = "Phòng đã có người thuê",
                        message = "Phòng \"$roomTitle\" mà bạn hẹn xem đã có người thuê khác chốt trước. Lịch hẹn của bạn sẽ bị hủy.",
                        type = "room_already_rented"
                    )
                    // Tự động chuyển các lịch hẹn này thành "cancelled_by_system"
                    db.collection("appointments").document(doc.id).update("status", "cancelled_by_system")
                }
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
        db.collection("appointments")
            .whereEqualTo("roomId", roomId)
            .whereIn("status", listOf("pending", "confirmed", "tenant_confirmed"))
            .addSnapshotListener { snapshots, _ ->
                val conflicts = mutableMapOf<String, Int>()
                snapshots?.forEach { doc ->
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
}