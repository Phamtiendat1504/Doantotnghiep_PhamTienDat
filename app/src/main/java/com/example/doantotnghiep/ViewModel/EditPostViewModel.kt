package com.example.doantotnghiep.ViewModel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.repository.RoomRepository

class EditPostViewModel : ViewModel() {

    private val repository = RoomRepository()

    private val _roomData = MutableLiveData<Map<String, Any>?>()
    val roomData: LiveData<Map<String, Any>?> = _roomData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _uploadProgressText = MutableLiveData<String>()
    val uploadProgressText: LiveData<String> = _uploadProgressText

    private val _saveResult = MutableLiveData<Boolean>()
    val saveResult: LiveData<Boolean> = _saveResult

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun loadRoomData(roomId: String) {
        repository.loadRoomData(roomId) { data -> _roomData.value = data }
    }

    fun updatePost(
        roomId: String,
        ward: String,
        district: String,
        existingImageUrls: List<String>,
        newImageUris: List<Uri>,
        deletedImageUrls: List<String>,
        data: HashMap<String, Any>
    ) {
        _isLoading.value = true
        repository.updatePost(
            roomId, existingImageUrls, newImageUris, deletedImageUrls, data,
            onProgress = { text -> _uploadProgressText.value = text },
            onSuccess = {
                _isLoading.value = false
                _saveResult.value = true
            },
            onFailure = { e ->
                _isLoading.value = false
                _errorMessage.value = e
            }
        )
    }

    fun resetSaveResult() { _saveResult.value = false }
    fun resetErrorMessage() { _errorMessage.value = "" }
}