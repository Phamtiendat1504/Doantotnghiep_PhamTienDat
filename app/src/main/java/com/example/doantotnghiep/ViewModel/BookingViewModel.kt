package com.example.doantotnghiep.ViewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.Model.Room
import com.example.doantotnghiep.Model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class BookingViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var appointmentListener: ListenerRegistration? = null

    val isLoading = MutableLiveData<Boolean>()
    val userData = MutableLiveData<User?>()
    val bookingResult = MutableLiveData<Boolean>()
    val errorMessage = MutableLiveData<String>()
    
    // LiveData chứa danh sách lịch hẹn để hiển thị lên UI
    val appointments = MutableLiveData<List<Map<String, Any>>>()

    // LiveData chứa thông tin chi tiết khi chủ trọ muốn xem lại từ lịch hẹn
    val selectedRoomDetails = MutableLiveData<Room?>()
    val selectedTenantDetails = MutableLiveData<User?>()

    // Xóa dữ liệu cũ khi đóng màn hình chi tiết
    fun clearSelectedDetails() {
        selectedRoomDetails.value = null
        selectedTenantDetails.value = null
    }

    fun loadUserInfo() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    userData.value = doc.toObject(User::class.java)
                }
            }
            .addOnFailureListener { e ->
                errorMessage.value = "Không thể tải thông tin người dùng: ${e.message}"
            }
    }

    // 1. NGƯỜI THUÊ: Gửi yêu cầu đặt lịch
    fun submitBooking(appointment: HashMap<String, Any>, landlordId: String, roomTitle: String, fullName: String, selectedDateDisplay: String, selectedTime: String) {
        isLoading.value = true
        
        // Đảm bảo có trạng thái mặc định là pending và thời gian tạo
        appointment["status"] = "pending"
        appointment["createdAt"] = System.currentTimeMillis()
        
        db.collection("appointments")
            .add(appointment)
            .addOnSuccessListener {
                // Tạo thông báo cho chủ trọ
                val notification = hashMapOf(
                    "userId" to landlordId,
                    "title" to "Có lịch hẹn mới!",
                    "message" to "$fullName muốn xem phòng \"$roomTitle\" vào $selectedDateDisplay lúc $selectedTime",
                    "type" to "appointment_new",
                    "isRead" to false,
                    "createdAt" to System.currentTimeMillis()
                )
                
                db.collection("notifications").add(notification)
                    .addOnCompleteListener {
                        isLoading.value = false
                        bookingResult.value = true
                    }
            }
            .addOnFailureListener { e ->
                isLoading.value = false
                errorMessage.value = "Lỗi đặt lịch: ${e.message}"
            }
    }

    // 2. LẤY LỊCH HẸN: Tự động cập nhật Realtime (Dùng cho cả Người thuê và Chủ trọ)
    fun fetchAppointments(isLandlord: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        isLoading.value = true

        // Hủy listener cũ trước khi tạo mới để tránh memory leak
        appointmentListener?.remove()

        val field = if (isLandlord) "landlordId" else "tenantId"

        appointmentListener = db.collection("appointments")
            .whereEqualTo(field, uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                isLoading.value = false
                if (error != null) {
                    errorMessage.value = "Lỗi tải lịch hẹn: ${error.message}"
                    return@addSnapshotListener
                }

                val list = mutableListOf<Map<String, Any>>()
                value?.forEach { doc ->
                    val data = doc.data.toMutableMap()
                    data["id"] = doc.id
                    list.add(data)
                }
                appointments.value = list
            }
    }

    // 3. CHỦ TRỌ BẤM VÀO LỊCH HẸN: Lấy song song thông tin Phòng và Người thuê
    fun fetchAppointmentDetails(roomId: String, tenantId: String) {
        isLoading.value = true

        var roomDone = false
        var tenantDone = false
        var hasError = false

        fun checkDone() {
            if (roomDone && tenantDone) isLoading.value = false
        }

        db.collection("rooms").document(roomId).get()
            .addOnSuccessListener { roomDoc ->
                if (roomDoc.exists()) selectedRoomDetails.value = roomDoc.toObject(Room::class.java)
                roomDone = true
                checkDone()
            }
            .addOnFailureListener { e ->
                if (!hasError) { hasError = true; errorMessage.value = "Lỗi khi lấy thông tin phòng: ${e.message}" }
                roomDone = true
                checkDone()
            }

        db.collection("users").document(tenantId).get()
            .addOnSuccessListener { userDoc ->
                if (userDoc.exists()) selectedTenantDetails.value = userDoc.toObject(User::class.java)
                tenantDone = true
                checkDone()
            }
            .addOnFailureListener {
                if (!hasError) { hasError = true; errorMessage.value = "Không thể lấy thông tin khách hàng" }
                tenantDone = true
                checkDone()
            }
    }

    // 4. CHỦ TRỌ: Xác nhận hoặc Từ chối lịch hẹn
    fun updateAppointmentStatus(appointmentId: String, newStatus: String, tenantId: String, roomTitle: String) {
        isLoading.value = true
        
        db.collection("appointments").document(appointmentId)
            .update("status", newStatus)
            .addOnSuccessListener {
                // Gửi thông báo ngược lại cho người thuê để họ biết lịch đã được duyệt/từ chối
                val statusText = if (newStatus == "confirmed") "đã được xác nhận" else "đã bị từ chối"
                val notification = hashMapOf(
                    "userId" to tenantId,
                    "title" to "Cập nhật lịch xem phòng",
                    "message" to "Lịch hẹn xem phòng \"$roomTitle\" của bạn $statusText",
                    "type" to "appointment_update",
                    "isRead" to false,
                    "createdAt" to System.currentTimeMillis()
                )
                
                db.collection("notifications").add(notification)
                isLoading.value = false
            }
            .addOnFailureListener { e ->
                isLoading.value = false
                errorMessage.value = "Cập nhật trạng thái thất bại: ${e.message}"
            }
    }

    // 5. NGƯỜI THUÊ: Xác nhận lại lịch hẹn (Sau khi landlord xác nhận)
    fun tenantConfirmAppointment(appointmentId: String, landlordId: String, roomTitle: String) {
        isLoading.value = true

        db.collection("appointments").document(appointmentId)
            .update("status", "tenant_confirmed")
            .addOnSuccessListener {
                // Gửi thông báo cho chủ trọ rằng người thuê đã xác nhận
                val notification = hashMapOf(
                    "userId" to landlordId,
                    "title" to "Người thuê đã xác nhận lịch hẹn",
                    "message" to "Khách đã xác nhận lịch xem phòng \"$roomTitle\"",
                    "type" to "appointment_tenant_confirmed",
                    "isRead" to false,
                    "createdAt" to System.currentTimeMillis()
                )

                db.collection("notifications").add(notification)
                isLoading.value = false
                bookingResult.value = true
            }
            .addOnFailureListener { e ->
                isLoading.value = false
                errorMessage.value = "Xác nhận lịch thất bại: ${e.message}"
            }
    }

    override fun onCleared() {
        super.onCleared()
        appointmentListener?.remove()
    }

    // 6. NGƯỜI THUÊ: Từ chối lịch hẹn (Huỷ xác nhận)
    fun tenantRejectAppointment(appointmentId: String, landlordId: String, roomTitle: String) {
        isLoading.value = true

        db.collection("appointments").document(appointmentId)
            .update("status", "cancelled_by_tenant")
            .addOnSuccessListener {
                // Gửi thông báo cho chủ trọ rằng người thuê đã huỷ
                val notification = hashMapOf(
                    "userId" to landlordId,
                    "title" to "Người thuê huỷ lịch hẹn",
                    "message" to "Khách đã huỷ lịch xem phòng \"$roomTitle\"",
                    "type" to "appointment_cancelled",
                    "isRead" to false,
                    "createdAt" to System.currentTimeMillis()
                )

                db.collection("notifications").add(notification)
                isLoading.value = false
            }
            .addOnFailureListener { e ->
                isLoading.value = false
                errorMessage.value = "Huỷ lịch thất bại: ${e.message}"
            }
    }
}
