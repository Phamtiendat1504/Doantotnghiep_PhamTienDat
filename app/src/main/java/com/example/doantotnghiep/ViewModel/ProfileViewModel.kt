package com.example.doantotnghiep.ViewModel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth

class ProfileViewModel : ViewModel() {

    private val repository = AuthRepository()

    data class UserInfo(
        val fullName: String,
        val email: String,
        val avatarUrl: String,
        val role: String,
        val isVerified: Boolean
    )

    private val _userInfo = MutableLiveData<UserInfo>()
    val userInfo: LiveData<UserInfo> = _userInfo

    private val _verificationStatus = MutableLiveData<String?>()
    val verificationStatus: LiveData<String?> = _verificationStatus

    private val _verifyRejectReason = MutableLiveData<String>()
    val verifyRejectReason: LiveData<String> = _verifyRejectReason

    private val _myPostsBadgeCount = MutableLiveData<Int>()
    val myPostsBadgeCount: LiveData<Int> = _myPostsBadgeCount

    private val _appointmentBadgeCount = MutableLiveData<Int>()
    val appointmentBadgeCount: LiveData<Int> = _appointmentBadgeCount

    private val _newAvatarUrl = MutableLiveData<String>()
    val newAvatarUrl: LiveData<String> = _newAvatarUrl

    private val _isUploadingAvatar = MutableLiveData<Boolean>()
    val isUploadingAvatar: LiveData<Boolean> = _isUploadingAvatar

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    // Cung cấp UID từ ViewModel, Fragment không cần gọi FirebaseAuth
    fun getCurrentUserId(): String? = FirebaseAuth.getInstance().currentUser?.uid

    fun logOut() {
        FirebaseAuth.getInstance().signOut()
    }

    fun isLoggedIn(): Boolean = FirebaseAuth.getInstance().currentUser != null

    fun loadUserInfo() {
        repository.loadUserProfile(
            onSuccess = { fullName, email, avatarUrl, role, isVerified ->
                val info = UserInfo(fullName, email, avatarUrl, role, isVerified)
                _userInfo.value = info
                if (role == "landlord" && !isVerified || role == "tenant") {
                    checkVerificationStatus()
                }
            },
            onFailure = { e -> _errorMessage.value = e }
        )
    }

    fun checkVerificationStatus() {
        repository.loadVerificationStatusDetail(
            onSuccess = { status, rejectReason ->
                _verificationStatus.value = status
                if (status == "rejected") _verifyRejectReason.value = rejectReason
            },
            onFailure = { e -> _errorMessage.value = e }
        )
    }

    fun uploadAvatar(imageUri: Uri) {
        _isUploadingAvatar.value = true
        repository.uploadAvatar(
            imageUri,
            onSuccess = { url ->
                _isUploadingAvatar.value = false
                _newAvatarUrl.value = url
            },
            onFailure = { e ->
                _isUploadingAvatar.value = false
                _errorMessage.value = e
            }
        )
    }

    fun loadMyPostsBadge(context: android.content.Context) {
        val uid = getCurrentUserId() ?: return
        repository.loadMyPostsBadge(uid, context) { count -> _myPostsBadgeCount.value = count }
    }

    fun loadAppointmentBadge() {
        val uid = getCurrentUserId() ?: return
        repository.loadAppointmentBadge(uid) { count -> _appointmentBadgeCount.value = count }
    }
}