package com.example.doantotnghiep.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.repository.RoomRepository

class AppointmentRoomDetailViewModel : ViewModel() {

    private val repository = RoomRepository()

    private val _roomData = MutableLiveData<Map<String, Any>?>()
    val roomData: LiveData<Map<String, Any>?> = _roomData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun loadRoomData(roomId: String) {
        _isLoading.value = true
        repository.getRoomById(
            roomId,
            onSuccess = { data ->
                _isLoading.value = false
                _roomData.value = data
            },
            onFailure = { e ->
                _isLoading.value = false
                _errorMessage.value = e
                _roomData.value = null
            }
        )
    }
}