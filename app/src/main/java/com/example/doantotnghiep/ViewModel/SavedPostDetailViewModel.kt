package com.example.doantotnghiep.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.repository.RoomRepository

class SavedPostDetailViewModel : ViewModel() {

    private val repository = RoomRepository()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _roomData = MutableLiveData<Map<String, Any>>()
    val roomData: LiveData<Map<String, Any>> = _roomData

    private val _deleteResult = MutableLiveData<Boolean>()
    val deleteResult: LiveData<Boolean> = _deleteResult

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun loadRoomDetail(roomId: String) {
        _isLoading.value = true
        repository.getRoomById(roomId,
            onSuccess = { data ->
                _isLoading.value = false
                _roomData.value = data
            },
            onFailure = { error ->
                _isLoading.value = false
                _errorMessage.value = error
            }
        )
    }

    fun deleteSavedPost(savedDocId: String) {
        _isLoading.value = true
        repository.deleteSavedPost(savedDocId,
            onSuccess = {
                _isLoading.value = false
                _deleteResult.value = true
            },
            onFailure = { error ->
                _isLoading.value = false
                _errorMessage.value = error
            }
        )
    }
}