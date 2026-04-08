package com.example.doantotnghiep.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.Model.Room
import com.example.doantotnghiep.Model.User
import com.example.doantotnghiep.repository.AppointmentRepository
import com.example.doantotnghiep.repository.AuthRepository
import com.example.doantotnghiep.repository.RoomRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

class BookingViewModel : ViewModel() {

    private val repository = AppointmentRepository()

    fun getCurrentUserId(): String? = FirebaseAuth.getInstance().currentUser?.uid
    private val authRepository = AuthRepository()
    private val roomRepository = RoomRepository()
    private var appointmentListener: ListenerRegistration? = null

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _userData = MutableLiveData<User?>()
    val userData: LiveData<User?> = _userData

    private val _bookingResult = MutableLiveData<Boolean>()
    val bookingResult: LiveData<Boolean> = _bookingResult

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _appointments = MutableLiveData<List<Map<String, Any>>>()
    val appointments: LiveData<List<Map<String, Any>>> = _appointments

    private val _userRole = MutableLiveData<String>()
    val userRole: LiveData<String> = _userRole

    private val _selectedRoomDetails = MutableLiveData<Room?>()
    val selectedRoomDetails: LiveData<Room?> = _selectedRoomDetails

    private val _selectedTenantDetails = MutableLiveData<User?>()
    val selectedTenantDetails: LiveData<User?> = _selectedTenantDetails

    fun clearSelectedDetails() {
        _selectedRoomDetails.value = null
        _selectedTenantDetails.value = null
    }

    fun loadUserInfo() {
        authRepository.loadUserObject(
            onSuccess = { user -> _userData.value = user },
            onFailure = { e -> _errorMessage.value = e }
        )
    }

    fun submitBooking(
        appointment: HashMap<String, Any>,
        landlordId: String,
        roomTitle: String,
        fullName: String,
        selectedDateDisplay: String,
        selectedTime: String
    ) {
        _isLoading.value = true
        repository.submitBooking(
            appointment, landlordId, roomTitle, fullName, selectedDateDisplay, selectedTime,
            onSuccess = {
                _isLoading.value = false
                _bookingResult.value = true
            },
            onFailure = { e ->
                _isLoading.value = false
                _errorMessage.value = e
            }
        )
    }

    fun fetchAppointments(isLandlord: Boolean) {
        _isLoading.value = true
        appointmentListener?.remove()
        appointmentListener = repository.listenAppointments(
            isLandlord,
            onUpdate = { list ->
                _isLoading.value = false
                _appointments.value = list
            },
            onError = { e ->
                _isLoading.value = false
                _errorMessage.value = e
            }
        )
    }

    // Load role rồi tự động fetch appointments — dùng cho MyAppointmentsActivity
    fun fetchAppointmentsByRole() {
        _isLoading.value = true
        repository.getCurrentUserRole(
            onSuccess = { role ->
                _userRole.value = role
                val isLandlord = role == "landlord" || role == "admin"
                fetchAppointments(isLandlord)
            },
            onFailure = { e ->
                _isLoading.value = false
                _errorMessage.value = e
            }
        )
    }

    fun fetchAppointmentDetails(roomId: String, tenantId: String) {
        _isLoading.value = true
        var roomDone = false
        var tenantDone = false

        fun checkDone() {
            if (roomDone && tenantDone) _isLoading.value = false
        }

        repository.fetchAppointmentDetails(
            roomId, tenantId,
            onRoomLoaded = { room ->
                _selectedRoomDetails.value = room
                roomDone = true
                checkDone()
            },
            onTenantLoaded = { user ->
                _selectedTenantDetails.value = user
                tenantDone = true
                checkDone()
            },
            onError = { e ->
                _errorMessage.value = e
                roomDone = true
                tenantDone = true
                checkDone()
            }
        )
    }

    fun tenantConfirmAppointment(appointmentId: String, landlordId: String, roomTitle: String) {
        _isLoading.value = true
        repository.tenantConfirmAppointment(
            appointmentId, landlordId, roomTitle,
            onSuccess = {
                _isLoading.value = false
                _bookingResult.value = true
            },
            onFailure = { e ->
                _isLoading.value = false
                _errorMessage.value = e
            }
        )
    }

    fun confirmAppointment(appointmentId: String, tenantId: String, roomTitle: String, date: String, time: String) {
        _isLoading.value = true
        repository.confirmAppointment(
            appointmentId, tenantId, roomTitle, date, time,
            onSuccess = {
                _isLoading.value = false
                _bookingResult.value = true
            },
            onFailure = { e ->
                _isLoading.value = false
                _errorMessage.value = e
            }
        )
    }

    fun rejectAppointment(appointmentId: String, tenantId: String, roomTitle: String, reason: String) {
        _isLoading.value = true
        repository.rejectAppointment(
            appointmentId, tenantId, roomTitle, reason,
            onSuccess = {
                _isLoading.value = false
                _bookingResult.value = true
            },
            onFailure = { e ->
                _isLoading.value = false
                _errorMessage.value = e
            }
        )
    }

    fun markAsRented(
        appointmentId: String, tenantId: String, roomTitle: String, roomId: String,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        _isLoading.value = true
        repository.sendNotification(
            userId = tenantId,
            title = "Phòng đã được cho thuê",
            message = "Phòng \"$roomTitle\" đã được cho thuê. Cảm ơn bạn đã quan tâm!",
            type = "room_rented"
        )
        repository.deleteAppointment(
            appointmentId,
            onSuccess = {
                roomRepository.deleteRoom(
                    roomId,
                    onSuccess = {
                        _isLoading.value = false
                        onSuccess()
                    },
                    onFailure = { error ->
                        _isLoading.value = false
                        onFailure(error)
                    }
                )
            },
            onFailure = { e ->
                _isLoading.value = false
                _errorMessage.value = e
            }
        )
    }

    fun cancelPendingAppointment(
        appointmentId: String, landlordId: String, roomTitle: String,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        _isLoading.value = true
        repository.cancelPendingAppointment(
            appointmentId, landlordId, roomTitle,
            onSuccess = {
                _isLoading.value = false
                onSuccess()
            },
            onFailure = { e ->
                _isLoading.value = false
                onFailure(e)
            }
        )
    }

    fun editPendingAppointment(
        appointmentId: String, landlordId: String, roomTitle: String,
        newDate: String, newDateDisplay: String, newTime: String,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        _isLoading.value = true
        repository.editPendingAppointment(
            appointmentId, landlordId, roomTitle, newDate, newDateDisplay, newTime,
            onSuccess = {
                _isLoading.value = false
                onSuccess()
            },
            onFailure = { e ->
                _isLoading.value = false
                onFailure(e)
            }
        )
    }

    fun tenantRejectAppointment(appointmentId: String, landlordId: String, roomTitle: String) {
        _isLoading.value = true
        repository.tenantRejectAppointment(
            appointmentId, landlordId, roomTitle,
            onSuccess = {
                _isLoading.value = false
                _bookingResult.value = true
            },
            onFailure = { e ->
                _isLoading.value = false
                _errorMessage.value = e
            }
        )
    }

    // Lấy bookingResult để reset từ bên ngoài
    fun resetBookingResult() {
        _bookingResult.value = false
    }

    // Lấy errorMessage để reset từ bên ngoài
    fun resetErrorMessage() {
        _errorMessage.value = ""
    }

    override fun onCleared() {
        super.onCleared()
        appointmentListener?.remove()
    }
}