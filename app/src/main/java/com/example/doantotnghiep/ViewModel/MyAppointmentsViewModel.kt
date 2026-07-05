package com.example.doantotnghiep.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.Model.Appointment
import com.example.doantotnghiep.Model.AppointmentActionResult
import com.example.doantotnghiep.repository.AppointmentRepository
import com.example.doantotnghiep.repository.RoomRentedNotice
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

// Fix #2: Tách SRP — ViewModel này chỉ quản lý màn hình MyAppointmentsActivity
// Fix #1: Constructor injection thay vì khởi tạo trực tiếp
class MyAppointmentsViewModel(
    private val repository: AppointmentRepository = AppointmentRepository()
) : ViewModel() {

    data class AppointmentAccess(val isHostAccess: Boolean, val effectiveRole: String)

    fun getCurrentUserId(): String? = FirebaseAuth.getInstance().currentUser?.uid

    private var appointmentListener: ListenerRegistration? = null
    private var roomRentedNoticeListener: ListenerRegistration? = null
    // Fix #6: Lưu trữ tập trung tất cả timeConflicts listeners để tránh memory leak
    private val timeConflictsListeners = mutableListOf<ListenerRegistration>()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Fix #7: Sealed class thay thế LiveData<Boolean> overloaded
    private val _actionResult = MutableLiveData<AppointmentActionResult?>()
    val actionResult: LiveData<AppointmentActionResult?> = _actionResult

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _appointments = MutableLiveData<List<Appointment>>()
    val appointments: LiveData<List<Appointment>> = _appointments

    private val _tenantAppointments = MutableLiveData<List<Appointment>>(emptyList())
    val tenantAppointments: LiveData<List<Appointment>> = _tenantAppointments

    private val _landlordAppointments = MutableLiveData<List<Appointment>>(emptyList())
    val landlordAppointments: LiveData<List<Appointment>> = _landlordAppointments

    private val _userRole = MutableLiveData<String>()
    val userRole: LiveData<String> = _userRole

    private val _timeConflicts = MutableLiveData<Map<String, Int>>()
    val timeConflicts: LiveData<Map<String, Int>> = _timeConflicts

    private val _appointmentAccess = MutableLiveData<AppointmentAccess>()
    val appointmentAccess: LiveData<AppointmentAccess> = _appointmentAccess

    private val _roomRentedNotice = MutableLiveData<RoomRentedNotice?>()
    val roomRentedNotice: LiveData<RoomRentedNotice?> = _roomRentedNotice

    //

    fun initializeAppointmentsScreen() {
        val uid = getCurrentUserId() ?: run { _errorMessage.value = "Chưa đăng nhập"; return }
        listenRoomRentedNotices(uid)
        _isLoading.value = true
        repository.loadCurrentUserAppointmentAccess(
            onSuccess = { isHostAccess, effectiveRole ->
                _isLoading.value = false
                _appointmentAccess.value = AppointmentAccess(isHostAccess, effectiveRole)
                repository.markAllAppointmentsRead(uid, effectiveRole)
                if (isHostAccess) fetchBothAppointments() else fetchAppointmentsByRole()
            },
            onFailure = { _isLoading.value = false; fetchAppointmentsByRole() }
        )
    }

    private fun listenRoomRentedNotices(uid: String) {
        roomRentedNoticeListener?.remove()
        roomRentedNoticeListener = repository.listenRoomRentedNotices(
            uid = uid,
            onNotice = { notice -> _roomRentedNotice.value = notice },
            onError = { e -> _errorMessage.value = e }
        )
    }

    fun markRoomRentedNoticeRead(notificationId: String) {
        repository.markRoomRentedNoticeRead(notificationId)
        _roomRentedNotice.value = null
    }

    fun fetchAppointmentsByRole() {
        _isLoading.value = true
        repository.getCurrentUserRole(
            onSuccess = { role ->
                _userRole.value = role
                val hasHostAccess = role == "admin" || role == "verified"
                appointmentListener?.remove()
                appointmentListener = repository.listenAppointments(hasHostAccess,
                    onUpdate = { list -> _isLoading.value = false; _appointments.value = list },
                    onError = { e -> _isLoading.value = false; _errorMessage.value = e }
                )
            },
            onFailure = { e -> _isLoading.value = false; _errorMessage.value = e }
        )
    }

    fun fetchBothAppointments() {
        _isLoading.value = true
        appointmentListener?.remove()
        val listeners = repository.listenBothAppointments(
            onTenantUpdate = { list -> _tenantAppointments.value = list; _isLoading.value = false },
            onLandlordUpdate = { list -> _landlordAppointments.value = list; _isLoading.value = false },
            onError = { e -> _isLoading.value = false; _errorMessage.value = e }
        )
        appointmentListener = object : ListenerRegistration {
            override fun remove() { listeners.first.remove(); listeners.second.remove() }
        }
    }

    // Fix #6: Xóa listeners cũ trước khi đăng ký mới — ngăn memory leak và nhân đôi listener
    fun refreshTimeConflictsListeners(roomIds: List<String>) {
        timeConflictsListeners.forEach { it.remove() }
        timeConflictsListeners.clear()
        val combinedConflicts = mutableMapOf<String, Int>()
        roomIds.forEach { roomId ->
            val reg = repository.checkTimeConflicts(roomId) { conflicts ->
                combinedConflicts.putAll(conflicts)
                _timeConflicts.value = combinedConflicts.toMap()
            }
            timeConflictsListeners.add(reg)
        }
    }

    fun resetActionResult() { _actionResult.value = null }
    fun resetErrorMessage() { _errorMessage.value = "" }

    //

    // CHủ trọ chốt lịch hẹn
    fun confirmAppointment(apptId: String, tenantId: String, roomTitle: String, date: String, time: String) {
        _isLoading.value = true
        repository.confirmAppointment(apptId, tenantId, roomTitle, date, time,
            onSuccess = { _isLoading.value = false; _actionResult.value = AppointmentActionResult.Success },
            onFailure = { e -> _isLoading.value = false; _errorMessage.value = e }
        )
    }

    // Người chủ chốt lịch hẹn
    fun rejectAppointment(apptId: String, tenantId: String, roomTitle: String, reason: String) {
        _isLoading.value = true
        repository.rejectAppointment(apptId, tenantId, roomTitle, reason,
            onSuccess = { _isLoading.value = false; _actionResult.value = AppointmentActionResult.Success },
            onFailure = { e -> _isLoading.value = false; _errorMessage.value = e }
        )
    }

    fun cancelByLandlord(apptId: String, tenantId: String, roomTitle: String,
        roomId: String, date: String, time: String, reason: String) {
        _isLoading.value = true
        repository.cancelByLandlord(apptId, tenantId, roomTitle, roomId, date, time, reason,
            onSuccess = { _isLoading.value = false; _actionResult.value = AppointmentActionResult.Success },
            onFailure = { e -> _isLoading.value = false; _errorMessage.value = e }
        )
    }

    fun markAsRented(apptId: String, roomId: String, tenantId: String, roomTitle: String, date: String, time: String) {
        _isLoading.value = true
        repository.markAsRented(apptId, roomId, tenantId, roomTitle, date, time,
            onSuccess = { _isLoading.value = false; _actionResult.value = AppointmentActionResult.Success },
            onFailure = { e -> _isLoading.value = false; _errorMessage.value = e }
        )
    }

    // Khách không đến xem
    fun markAsNoShow(apptId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        _isLoading.value = true
        repository.markAsNoShow(apptId,
            onSuccess = { _isLoading.value = false; _actionResult.value = AppointmentActionResult.Success; onSuccess() },
            onFailure = { e -> _isLoading.value = false; onFailure(e) }
        )
    }

    fun reopenRoom(roomId: String, apptId: String, tenantId: String,
        onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        _isLoading.value = true
        repository.reopenRoom(roomId, apptId, tenantId,
            onSuccess = { _isLoading.value = false; _actionResult.value = AppointmentActionResult.Success; onSuccess() },
            onFailure = { e -> _isLoading.value = false; onFailure(e) }
        )
    }

    //

    fun markAsLandlordNoShow(apptId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        _isLoading.value = true
        repository.markAsLandlordNoShow(apptId,
            onSuccess = { _isLoading.value = false; _actionResult.value = AppointmentActionResult.Success; onSuccess() },
            onFailure = { e -> _isLoading.value = false; onFailure(e) }
        )
    }

    // Người đặt lịch hẹn bấm xác nhận đi xem
    fun tenantConfirmAppointment(apptId: String, landlordId: String, roomTitle: String) {
        _isLoading.value = true
        repository.tenantConfirmAppointment(apptId, landlordId, roomTitle,
            onSuccess = { _isLoading.value = false; _actionResult.value = AppointmentActionResult.Success },
            onFailure = { e -> _isLoading.value = false; _errorMessage.value = e }
        )
    }

    // Người đặt lịch hủy lịch hẹn
    fun tenantRejectAppointment(apptId: String, landlordId: String, roomTitle: String,
        roomId: String, date: String, time: String, currentStatus: String, cancelReason: String) {
        _isLoading.value = true
        repository.tenantRejectAppointment(apptId, landlordId, roomTitle, roomId, date, time, currentStatus, cancelReason,
            onSuccess = { _isLoading.value = false; _actionResult.value = AppointmentActionResult.Success },
            onFailure = { e -> _isLoading.value = false; _errorMessage.value = e }
        )
    }

    // Người đặt lịch hủy yêu cầu đặt lịch hẹn khi đang chờ chủ trọ duyệt
    fun cancelPendingAppointment(apptId: String, landlordId: String, roomTitle: String,
        onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        _isLoading.value = true
        repository.cancelPendingAppointment(apptId, landlordId, roomTitle,
            onSuccess = { _isLoading.value = false; onSuccess() },
            onFailure = { e -> _isLoading.value = false; onFailure(e) }
        )
    }

    fun editPendingAppointment(apptId: String, landlordId: String, roomTitle: String,
        newDate: String, newDateDisplay: String, newTime: String,
        newDateMs: Long, newTimestampMs: Long,
        onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        _isLoading.value = true
        repository.editPendingAppointment(apptId, landlordId, roomTitle,
            newDate, newDateDisplay, newTime, newDateMs, newTimestampMs,
            onSuccess = { _isLoading.value = false; onSuccess() },
            onFailure = { e -> _isLoading.value = false; onFailure(e) }
        )
    }

    fun markAsViewed(apptId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        _isLoading.value = true
        repository.markAsViewed(apptId,
            onSuccess = { _isLoading.value = false; _actionResult.value = AppointmentActionResult.Success; onSuccess() },
            onFailure = { e -> _isLoading.value = false; onFailure(e) }
        )
    }

    fun markAsNotRented(apptId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        _isLoading.value = true
        repository.markAsNotRented(apptId,
            onSuccess = { _isLoading.value = false; _actionResult.value = AppointmentActionResult.Success; onSuccess() },
            onFailure = { e -> _isLoading.value = false; onFailure(e) }
        )
    }

    override fun onCleared() {
        super.onCleared()
        appointmentListener?.remove()
        roomRentedNoticeListener?.remove()
        timeConflictsListeners.forEach { it.remove() }
        timeConflictsListeners.clear()
    }
}
