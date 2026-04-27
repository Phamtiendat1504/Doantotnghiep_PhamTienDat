package com.example.doantotnghiep.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.repository.RoomRepository

class MyPostDetailViewModel : ViewModel() {

    private val repository = RoomRepository()

    private val _roomData = MutableLiveData<Map<String, Any>?>()
    val roomData: LiveData<Map<String, Any>?> = _roomData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _markRentedStatus = MutableLiveData<Boolean>()
    val markRentedStatus: LiveData<Boolean> = _markRentedStatus

    fun loadRoomDetail(roomId: String) {
        _isLoading.value = true
        repository.getRoomById(
            roomId,
            onSuccess = { data ->
                _isLoading.value = false
                _roomData.value = data
            },
            onFailure = {
                _isLoading.value = false
                _roomData.value = null
            }
        )
    }

    fun markAsRented(roomId: String) {
        _isLoading.value = true
        repository.markAsRented(
            roomId,
            onSuccess = {
                _isLoading.value = false
                _markRentedStatus.value = true
            },
            onFailure = {
                _isLoading.value = false
                _markRentedStatus.value = false
            }
        )
    }
}