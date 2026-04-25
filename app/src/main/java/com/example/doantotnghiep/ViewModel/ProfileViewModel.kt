package com.example.doantotnghiep.ViewModel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.repository.AppointmentRepository
import com.example.doantotnghiep.repository.AuthRepository
import com.example.doantotnghiep.repository.RoomRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

class ProfileViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val roomRepository = RoomRepository()
    private val appointmentRepository = AppointmentRepository()

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

    private val _notificationBadgeCount = MutableLiveData<Int>()
    val notificationBadgeCount: LiveData<Int> = _notificationBadgeCount
    private var notificationListener: ListenerRegistration? = null
    private var appointmentListener: ListenerRegistration? = null
    private var postBadgeListener: ListenerRegistration? = null

    fun getCurrentUserId(): String? = FirebaseAuth.getInstance().currentUser?.uid

    fun logOut(onDone: (() -> Unit)? = null) {
        com.example.doantotnghiep.Utils.PresenceManager.goOfflineAndThen {
            FirebaseAuth.getInstance().signOut()
            onDone?.invoke()
        }
    }

    fun isLoggedIn(): Boolean = FirebaseAuth.getInstance().currentUser != null

    fun loadUserInfo() {
        authRepository.loadUserProfile(
            onSuccess = { fullName, email, avatarUrl, role, isVerified ->
                val info = UserInfo(fullName, email, avatarUrl, role, isVerified)
                _userInfo.value = info
                if (!isVerified) {
                    checkVerificationStatus()
                } else {
                    // Tranh giu status cu (pending/rejected) de khong ghi de badge da xac minh.
                    _verificationStatus.value = null
                }
            },
            onFailure = { e -> _errorMessage.value = e }
        )
    }

    fun checkVerificationStatus() {
        authRepository.loadVerificationStatusDetail(
            onSuccess = { status, rejectReason ->
                _verificationStatus.value = status
                if (status == "rejected") _verifyRejectReason.value = rejectReason ?: ""
            },
            onFailure = { e -> _errorMessage.value = e }
        )
    }

    fun resetAvatarUploadState() {
        _isUploadingAvatar.value = false
        _newAvatarUrl.value = ""
    }

    fun uploadAvatar(imageUri: Uri) {
        _isUploadingAvatar.value = true
        authRepository.uploadAvatar(
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

    /**
     * Lắng nghe badge bài đăng chưa đọc (chỉ dành cho Chủ trọ).
     * Dùng cờ cloud `hasUnreadUpdate` thay vì SharedPreferences — đồng bộ mọi thiết bị.
     */
    fun loadMyPostsBadge() {
        val uid = getCurrentUserId() ?: return
        postBadgeListener?.remove()
        postBadgeListener = roomRepository.listenPostBadge(uid) { count ->
            _myPostsBadgeCount.postValue(count)
        }
    }

    /**
     * Lắng nghe badge lịch hẹn theo đúng vai trò người dùng.
     * Chỉ tạo 1 listener (không phải 2 như cũ), tiết kiệm chi phí đọc Firestore.
     */
    fun loadAppointmentBadge(role: String, isVerified: Boolean) {
        val uid = getCurrentUserId() ?: return
        val effectiveRole = if (role == "admin") "admin" else if (isVerified) "verified" else "user"
        appointmentListener?.remove()
        appointmentListener = appointmentRepository.listenBadge(uid, effectiveRole) { count ->
            _appointmentBadgeCount.postValue(count)
        }
    }

    fun loadNotificationBadge() {
        val uid = getCurrentUserId() ?: return
        notificationListener?.remove()
        notificationListener = roomRepository.listenUnseenNotificationCount(uid) { count ->
            _notificationBadgeCount.postValue(count)
        }
    }

    private val _messagesBadgeInfo = MutableLiveData<Pair<Int, Int>>()
    val messagesBadgeInfo: LiveData<Pair<Int, Int>> = _messagesBadgeInfo
    private var chatBadgeListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun loadMessagesBadge() {
        val uid = getCurrentUserId() ?: return
        chatBadgeListener?.remove()
        chatBadgeListener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("chats")
            .whereArrayContains("participants", uid)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                var numPeople = 0
                var numMessages = 0
                for (doc in snap.documents) {
                    val deletedMap = doc.get("deletedFor") as? Map<String, Long> ?: mapOf()
                    val myDeletedAt = deletedMap[uid] ?: 0L
                    val lastMsgAt = doc.getLong("lastMessageAt") ?: 0L
                    if (myDeletedAt > 0L && lastMsgAt <= myDeletedAt) {
                        continue // Chat đã bị xóa
                    }

                    val unreadCount = doc.get("unreadCount.$uid") as? Long ?: 0L
                    if (unreadCount > 0) {
                        numPeople++
                        numMessages += unreadCount.toInt()
                    }
                }
                _messagesBadgeInfo.postValue(Pair(numPeople, numMessages))
            }
    }

    override fun onCleared() {
        super.onCleared()
        notificationListener?.remove()
        appointmentListener?.remove()
        postBadgeListener?.remove()
        chatBadgeListener?.remove()
    }
}
