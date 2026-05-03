package com.example.doantotnghiep.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.repository.RoomRepository
import com.google.firebase.auth.FirebaseAuth

class SavedPostsViewModel : ViewModel() {

    private val repository = RoomRepository()

    data class SavedPost(
        val savedDocId: String,
        val roomId: String,
        val ownerId: String,
        val title: String,
        val price: Long,
        val address: String,
        val ward: String,
        val district: String,
        val imageUrl: String
    )

    private val _savedPosts = MutableLiveData<List<SavedPost>>()
    val savedPosts: LiveData<List<SavedPost>> = _savedPosts

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _roomCheckResult = MutableLiveData<Pair<String, Boolean>>()
    val roomCheckResult: LiveData<Pair<String, Boolean>> = _roomCheckResult

    private val _deleteResult = MutableLiveData<String>()
    val deleteResult: LiveData<String> = _deleteResult

    // Cung cấp UID từ ViewModel
    fun getCurrentUserId(): String? = FirebaseAuth.getInstance().currentUser?.uid

    fun loadSavedPosts() {
        val uid = getCurrentUserId() ?: return
        _isLoading.value = true
        repository.loadSavedPosts(
            uid,
            onSuccess = { docs ->
                _isLoading.value = false
                val list = docs.map { doc ->
                    SavedPost(
                        savedDocId = doc.id,
                        roomId = doc.getString("roomId") ?: "",
                        ownerId = doc.getString("ownerId") ?: "",
                        title = doc.getString("title") ?: "",
                        price = doc.getLong("price") ?: 0,
                        address = doc.getString("address") ?: "",
                        ward = doc.getString("ward") ?: "",
                        district = doc.getString("district") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: ""
                    )
                }
                _savedPosts.value = list
            },
            onFailure = { e ->
                _isLoading.value = false
                _errorMessage.value = e
            }
        )
    }

    fun deleteSavedPost(savedDocId: String) {
        repository.deleteSavedPostById(
            savedDocId,
            onSuccess = { _deleteResult.value = savedDocId },
            onFailure = { e -> _errorMessage.value = e }
        )
    }

    fun checkRoomExists(savedDocId: String, roomId: String) {
        repository.checkRoomExists(
            roomId,
            onResult = { exists -> _roomCheckResult.value = Pair(savedDocId, exists) },
            onFailure = { e -> _errorMessage.value = e }
        )
    }

    fun autoDeleteSavedPost(savedDocId: String) {
        repository.deleteSavedPostById(savedDocId, onSuccess = {}, onFailure = {})
    }

    fun clearRoomCheckResult() {
        // Không giữ state cũ để tránh UI xử lý nhầm kết quả trước đó
    }
}
