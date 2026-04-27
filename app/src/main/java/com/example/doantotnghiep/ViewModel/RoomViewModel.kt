package com.example.doantotnghiep.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.repository.AuthRepository
import com.example.doantotnghiep.repository.RoomRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.auth.FirebaseAuth

class RoomViewModel : ViewModel() {

    private val repository = RoomRepository()
    private val authRepository = AuthRepository()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _roomDocs = MutableLiveData<List<DocumentSnapshot>>()
    val roomDocs: LiveData<List<DocumentSnapshot>> = _roomDocs

    private val _selectedRoomIndex = MutableLiveData<Int>()
    val selectedRoomIndex: LiveData<Int> = _selectedRoomIndex

    private val _userRole = MutableLiveData<String>()
    val userRole: LiveData<String> = _userRole

    private val _isSaved = MutableLiveData<Boolean>()
    val isSaved: LiveData<Boolean> = _isSaved

    private val _hasActiveBooking = MutableLiveData<Boolean>()
    val hasActiveBooking: LiveData<Boolean> = _hasActiveBooking

    private val _bookedSlots = MutableLiveData<List<Map<String, Any>>>()
    val bookedSlots: LiveData<List<Map<String, Any>>> = _bookedSlots

    private val _saveResult = MutableLiveData<Boolean>()
    val saveResult: LiveData<Boolean> = _saveResult

    fun getCurrentUserId(): String? = FirebaseAuth.getInstance().currentUser?.uid

    fun loadOwnerRooms(userId: String, selectedRoomId: String) {
        _isLoading.value = true
        repository.loadOwnerRooms(
            userId,
            onSuccess = { docs ->
                _isLoading.value = false
                _roomDocs.value = docs
                if (docs.isNotEmpty()) {
                    val index = docs.indexOfFirst { it.id == selectedRoomId }.takeIf { it >= 0 } ?: 0
                    _selectedRoomIndex.value = index
                }
            },
            onFailure = { e ->
                _isLoading.value = false
                _errorMessage.value = e
            }
        )
    }

    fun loadSingleRoom(roomId: String) {
        _isLoading.value = true
        repository.loadSingleRoomDoc(
            roomId,
            onSuccess = { doc ->
                _isLoading.value = false
                if (doc != null) {
                    _roomDocs.value = listOf(doc)
                    _selectedRoomIndex.value = 0
                } else {
                    _errorMessage.value = "Phòng không tồn tại"
                }
            },
            onFailure = { e ->
                _isLoading.value = false
                _errorMessage.value = e
            }
        )
    }

    fun loadUserRole(uid: String) {
        authRepository.getUserRole(
            onSuccess = { role -> _userRole.value = role },
            onFailure = { _userRole.value = "user" }
        )
    }

    fun checkSavedStatus(uid: String, roomId: String) {
        repository.checkSavedStatus(uid, roomId) { saved -> _isSaved.value = saved }
    }

    fun checkActiveBooking(uid: String, roomId: String) {
        repository.checkActiveBooking(uid, roomId) { active -> _hasActiveBooking.value = active }
    }

    fun loadBookedSlots(roomId: String) {
        repository.loadBookedSlots(roomId) { slots -> _bookedSlots.value = slots }
    }

    fun toggleSavePost(uid: String, roomId: String, roomData: Map<String, Any>) {
        repository.toggleSavePost(
            uid, roomId, roomData,
            onResult = { saved ->
                _isSaved.value = saved
                _saveResult.value = saved
            },
            onFailure = { e -> _errorMessage.value = e }
        )
    }
}
