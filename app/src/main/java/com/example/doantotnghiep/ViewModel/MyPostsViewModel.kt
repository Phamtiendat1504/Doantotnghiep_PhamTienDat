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

    private val _deleteResult = MutableLiveData<Boolean>()
    val deleteResult: LiveData<Boolean> = _deleteResult

    fun getCurrentUserId(): String? = FirebaseAuth.getInstance().currentUser?.uid

    fun loadPosts(filter: String) {
        _isLoading.value = true
        _errorMessage.value = null
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
                    }.thenByDescending {
                        when (val raw = it.get("createdAt")) {
                            is Number -> raw.toLong()
                            is com.google.firebase.Timestamp -> raw.toDate().time
                            else -> 0L
                        }
                    })
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

    fun deletePost(docId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        repository.deleteRoom(docId, onSuccess = onSuccess, onFailure = onFailure)
    }

    fun markAsRented(docId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        repository.markAsRented(docId, onSuccess = onSuccess, onFailure = onFailure)
    }

    fun markPostsAsRead(uid: String) {
        repository.markPostsAsRead(uid)
    }

    fun resetErrorMessage() { _errorMessage.value = "" }
}
