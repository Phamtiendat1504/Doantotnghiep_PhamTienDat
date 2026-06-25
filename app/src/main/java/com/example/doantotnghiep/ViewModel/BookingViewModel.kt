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

    // ─── Booking wizard ───────────────────────────────────────────────────────

    fun loadRemainingQuota(roomId: String) {
        val uid = getCurrentUserId() ?: return
        repository.checkDailyBookingQuotaForRoom(uid, roomId) { used ->
            // Fix #9: Dùng hằng số thay vì magic number
            _remainingQuota.value = maxOf(0, AppointmentConstants.MAX_DAILY_BOOKING_QUOTA - used)
        }
    }

    fun loadRoomForBooking(roomId: String, onResult: (Room, List<TimeSlotConfig>) -> Unit) {
        repository.fetchRoomForBooking(roomId,
            onSuccess = { room, slots -> onResult(room, slots) },
            onFailure = { e -> _errorMessage.value = e }
        )
    }

    fun listenBookedSlotsForRoom(roomId: String, onUpdate: (Set<String>) -> Unit) {
        bookedSlotsListener?.remove()
        bookedSlotsListener = repository.listenBookedSlotsForRoom(roomId, onUpdate)
    }

    fun loadUserInfo() {
        _isLoading.value = true
        authRepository.loadUserObject(
            onSuccess = { user -> _isLoading.value = false; _userData.value = user },
            onFailure = { e -> _isLoading.value = false; _errorMessage.value = e }
        )
    }

    fun submitBooking(appointment: Appointment) {
        _isLoading.value = true
        repository.submitBooking(appointment,
            onSuccess = { _isLoading.value = false; _bookingResult.value = true },
            onFailure = { e -> _isLoading.value = false; _errorMessage.value = e }
        )
    }

    fun checkExistingAppointment(tenantId: String, roomId: String, onResult: (Boolean, String?, String?, Long?) -> Unit) {
        repository.checkExistingAppointment(tenantId, roomId, onResult)
    }

    fun checkTenantPendingCount(uid: String, onResult: (Int) -> Unit) {
        repository.checkTenantPendingCount(uid, onResult)
    }

    fun checkTenantConfirmedCount(uid: String, onResult: (Int) -> Unit) {
        repository.checkTenantConfirmedCount(uid, onResult)
    }

    fun checkTenantNoShowCount(uid: String, onResult: (Int, Long) -> Unit) {
        repository.checkTenantNoShowCount(uid, onResult)
    }

    fun loadConfirmedAppointmentsForDate(roomId: String, dateStr: String, onResult: (List<Appointment>) -> Unit) {
        repository.loadConfirmedAppointmentsForDate(roomId, dateStr, onResult)
    }

    fun resetBookingResult() { _bookingResult.value = false }
    fun resetErrorMessage() { _errorMessage.value = "" }

    override fun onCleared() {
        super.onCleared()
        bookedSlotsListener?.remove()
    }
}
