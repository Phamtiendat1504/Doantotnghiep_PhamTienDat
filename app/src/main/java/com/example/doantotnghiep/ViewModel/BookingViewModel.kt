package com.example.doantotnghiep.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.Model.Appointment
import com.example.doantotnghiep.Model.Room
import com.example.doantotnghiep.Model.TimeSlotConfig
import com.example.doantotnghiep.Model.User
import com.example.doantotnghiep.Utils.AppointmentConstants
import com.example.doantotnghiep.repository.AppointmentRepository
import com.example.doantotnghiep.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

// Fix #2: ViewModel này chỉ còn logic đặt lịch (BookingActivity)
// Fix #1: Constructor injection thay vì khởi tạo trực tiếp
class BookingViewModel(
    private val repository: AppointmentRepository = AppointmentRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    fun getCurrentUserId(): String? = FirebaseAuth.getInstance().currentUser?.uid

    private var bookedSlotsListener: ListenerRegistration? = null

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _userData = MutableLiveData<User?>()
    val userData: LiveData<User?> = _userData

    private val _bookingResult = MutableLiveData<Boolean>()
    val bookingResult: LiveData<Boolean> = _bookingResult

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _remainingQuota = MutableLiveData<Int>()
    val remainingQuota: LiveData<Int> = _remainingQuota


    // Lấy số lượt đặt lịch còn lại trong ngày (để hiện cảnh báo xanh/đỏ) (loadData)
    // Giới hạn 5 lượt đặt lịch hẹn cho 1 bài đăng trong ngày -1-
    // Nơi xử lý logic phép trừ. Gọi hàm từ Repository để lấy "số lần đã đặt"
    // Sau đó tính toán số lượt còn lại bằng công thức: (5 - số lần đã đặt hôm nay) và báo kết quả lại cho Activity.
    fun loadRemainingQuota(roomId: String) {
        val uid = getCurrentUserId() ?: return
        repository.checkDailyBookingQuotaForRoom(uid, roomId) { used ->
            _remainingQuota.value = maxOf(0, AppointmentConstants.MAX_DAILY_BOOKING_QUOTA - used)
        }
    }

    // Kéo thông tin căn phòng và danh sách cấu hình giờ rảnh (loadData)
    fun loadRoomForBooking(roomId: String, onResult: (Room, List<TimeSlotConfig>) -> Unit) {
        repository.fetchRoomForBooking(roomId,
            onSuccess = { room, slots -> onResult(room, slots) },
            onFailure = { e -> _errorMessage.value = e }
        )
    }

    // Kích hoạt màng lọc Real-time, lấy về danh sách các giờ đã bị khóa để Activity bôi xám các nút bấm (loadData)
    fun listenBookedSlotsForRoom(roomId: String, onUpdate: (Set<String>) -> Unit) {
        bookedSlotsListener?.remove()
        bookedSlotsListener = repository.listenBookedSlotsForRoom(roomId, onUpdate)
    }

    // Kéo họ tên, số điện thoại của người dùng về để tự động điền (Auto-fill) vào form Bước 3.(loadData)
    fun loadUserInfo() {
        _isLoading.value = true
        authRepository.loadUserObject(
            onSuccess = { user -> _isLoading.value = false; _userData.value = user },
            onFailure = { e -> _isLoading.value = false; _errorMessage.value = e }
        )
    }

    // Nhận cái gói hồ sơ Appointment từ Activity truyền sang, bật vòng quay Loading, và mang xuống cho Repository gửi lên Firebase.
    fun submitBooking(appointment: Appointment) {
        _isLoading.value = true
        repository.submitBooking(appointment,
            onSuccess = { _isLoading.value = false; _bookingResult.value = true },
            onFailure = { e -> _isLoading.value = false; _errorMessage.value = e }
        )
    }

    // Kiểm tra xem khách có đang đặt trùng phòng này không.(runPreChecks)
    fun checkExistingAppointment(tenantId: String, roomId: String, onResult: (Boolean, String?, String?, Long?) -> Unit) {
        repository.checkExistingAppointment(tenantId, roomId, onResult)
    }

    // Kiểm tra xem khách có đang spam quá 3 lịch chờ duyệt không.(runPreChecks)
    fun checkTenantPendingCount(uid: String, onResult: (Int) -> Unit) {
        repository.checkTenantPendingCount(uid, onResult)
    }

    //Kiểm tra xem khách có đang ôm quá 2 lịch đã chốt không (runPreChecks)
    fun checkTenantConfirmedCount(uid: String, onResult: (Int) -> Unit) {
        repository.checkTenantConfirmedCount(uid, onResult)
    }

    // Kiểm tra xem khách có bị dính 3 gậy bùng kèo (No-Show) không. (runPreChecks)
    fun checkTenantNoShowCount(uid: String, onResult: (Int, Long) -> Unit) {
        repository.checkTenantNoShowCount(uid, onResult)
    }

    fun loadConfirmedAppointmentsForDate(roomId: String, dateStr: String, onResult: (List<Appointment>) -> Unit) {
        repository.loadConfirmedAppointmentsForDate(roomId, dateStr, onResult)
    }

    // Tắt cờ báo thành công (Để lỡ khách bấm Back lại thì không bị văng tiếp sang trang Thành công nữa)
    fun resetBookingResult() { _bookingResult.value = false }
    // Xóa trắng câu thông báo lỗi sau khi đã hiển thị xong Popup màu đỏ.
    fun resetErrorMessage() { _errorMessage.value = "" }

    override fun onCleared() {
        super.onCleared()
        bookedSlotsListener?.remove()
    }
}
