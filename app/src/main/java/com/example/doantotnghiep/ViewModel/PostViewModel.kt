package com.example.doantotnghiep.ViewModel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.Model.Room
import com.example.doantotnghiep.repository.AuthRepository
import com.example.doantotnghiep.repository.RoomRepository
import com.google.firebase.auth.FirebaseAuth

class PostViewModel : ViewModel() {

    private val repository = RoomRepository()
    private val authRepository = AuthRepository()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _postResult = MutableLiveData<Boolean>()
    val postResult: LiveData<Boolean> = _postResult

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _uploadProgress = MutableLiveData<Int>()
    val uploadProgress: LiveData<Int> = _uploadProgress

    private val _userRole = MutableLiveData<String>()
    val userRole: LiveData<String> = _userRole

    private val _verifyStatus = MutableLiveData<String?>()
    val verifyStatus: LiveData<String?> = _verifyStatus

    private val _verifyRejectReason = MutableLiveData<String>()
    val verifyRejectReason: LiveData<String> = _verifyRejectReason

    private val _ownerInfo = MutableLiveData<Pair<String, String>>()
    val ownerInfo: LiveData<Pair<String, String>> = _ownerInfo

    // Lấy UID hiện tại — không để Fragment gọi FirebaseAuth trực tiếp
    fun getCurrentUserId(): String? = FirebaseAuth.getInstance().currentUser?.uid

    fun checkUserRole() {
        val uid = getCurrentUserId() ?: run {
            _userRole.value = "tenant"
            return
        }
        authRepository.getUserRole(
            onSuccess = { role ->
                _userRole.value = role
                if (role != "landlord" && role != "admin") {
                    checkVerificationStatus(uid)
                }
            },
            onFailure = { _userRole.value = "tenant" }
        )
    }

    private fun checkVerificationStatus(uid: String) {
        repository.getVerificationStatus(
            uid,
            onSuccess = { status, rejectReason ->
                _verifyStatus.value = status
                if (status == "rejected") {
                    _verifyRejectReason.value = rejectReason ?: ""
                }
            },
            onFailure = { _verifyStatus.value = null }
        )
    }

    fun loadOwnerInfo() {
        val uid = getCurrentUserId() ?: return
        authRepository.loadUserById(
            uid,
            onSuccess = { user ->
                user?.let {
                    _ownerInfo.value = Pair(it.fullName ?: "", it.phone ?: "")
                }
            },
            onFailure = { /* ignore */ }
        )
    }

    fun resetPostResult() {
        _postResult.value = false
    }

    fun postRoom(room: Room, imageUris: List<Uri>) {
        if (room.ownerName.isBlank()) { _errorMessage.value = "Vui lòng nhập họ tên chủ trọ"; return }
        if (room.ownerPhone.isBlank()) { _errorMessage.value = "Vui lòng nhập số điện thoại"; return }
        if (room.ownerPhone.length < 10) { _errorMessage.value = "Số điện thoại không hợp lệ"; return }
        if (room.title.isBlank()) { _errorMessage.value = "Vui lòng nhập tiêu đề bài đăng"; return }
        if (room.ward.isBlank() || room.ward.contains("Chọn phường/xã")) {
            _errorMessage.value = "Vui lòng chọn khu vực (Phường/Xã)"; return
        }
        if (room.address.isBlank()) { _errorMessage.value = "Vui lòng nhập địa chỉ cụ thể"; return }
        if (room.price <= 0) { _errorMessage.value = "Vui lòng nhập giá thuê hợp lệ"; return }
        if (room.area <= 0) { _errorMessage.value = "Vui lòng nhập diện tích hợp lệ"; return }
        if (imageUris.isEmpty()) { _errorMessage.value = "Vui lòng thêm ít nhất 1 ảnh phòng trọ"; return }

        _isLoading.value = true
        repository.postRoom(room, imageUris,
            onSuccess = { _isLoading.value = false; _postResult.value = true },
            onFailure = { error -> _isLoading.value = false; _errorMessage.value = error },
            onProgress = { progress -> _uploadProgress.value = progress }
        )
    }
}