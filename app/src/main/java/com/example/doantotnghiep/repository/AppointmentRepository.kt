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
                    "isRead" to false,
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
            .update("status", "tenant_confirmed")
            .addOnSuccessListener {
                db.collection("bookedSlots").document(appointmentId).delete()
                sendNotification(
                    userId = landlordId,
                    title = "Người thuê đã xác nhận lịch hẹn",
                    message = "Khách đã xác nhận lịch xem phòng \"$roomTitle\"",
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
            .update(mapOf("status" to "rejected", "rejectReason" to reason))
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

    // Đánh dấu đã cho thuê — xóa appointment, sau đó xóa phòng qua RoomRepository
    fun deleteAppointment(
        appointmentId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("appointments").document(appointmentId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e.message ?: "Lỗi xóa lịch hẹn") }
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
            .update("status", "cancelled_by_tenant")
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

    // Gửi thông báo
    fun sendNotification(userId: String, title: String, message: String, type: String) {
        db.collection("notifications").add(
            hashMapOf(
                "userId" to userId,
                "title" to title,
                "message" to message,
                "type" to type,
                "isRead" to false,
                "createdAt" to System.currentTimeMillis()
            )
        )
    }
}