package com.example.doantotnghiep.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.repository.RoomRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.auth.FirebaseAuth

class MyPostsViewModel : ViewModel() {

    private val repository = RoomRepository()

    private val _posts = MutableLiveData<List<DocumentSnapshot>>()
    val posts: LiveData<List<DocumentSnapshot>> = _posts

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _renewResult = MutableLiveData<Boolean>()
    val renewResult: LiveData<Boolean> = _renewResult

    private val _deleteResult = MutableLiveData<Boolean>()
    val deleteResult: LiveData<Boolean> = _deleteResult

    fun getCurrentUserId(): String? = FirebaseAuth.getInstance().currentUser?.uid

    fun loadPosts(filter: String) {
        _isLoading.value = true
        repository.getMyPosts(
            filter,
            onSuccess = { list ->
                _isLoading.value = false
                val sorted = if (filter == "all") {
                    list.sortedWith(compareBy<DocumentSnapshot> {
                        when (it.getString("status")) {
                            "rejected" -> 0
                            "pending" -> 1
                            else -> 2
                        }
                    }.thenByDescending { it.getLong("createdAt") ?: 0 })
                } else {
                    list
                }
                _posts.value = sorted
            },
            onFailure = { e ->
                _isLoading.value = false
                _errorMessage.value = e
            }
        )
    }

    fun renewPost(docId: String) {
        repository.renewPost(
            docId,
            onSuccess = { _renewResult.value = true },
            onFailure = { e -> _errorMessage.value = e }
        )
    }

    fun deletePost(docId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        repository.deleteRoom(docId, onSuccess = onSuccess, onFailure = onFailure)
    }

    fun markAsRented(docId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        repository.markAsRented(docId, onSuccess = onSuccess, onFailure = onFailure)
    }

    fun markPostsAsRead(uid: String) {
        repository.markPostsAsRead(uid)
    }

    fun resetRenewResult() { _renewResult.value = false }
    fun resetErrorMessage() { _errorMessage.value = "" }
}
