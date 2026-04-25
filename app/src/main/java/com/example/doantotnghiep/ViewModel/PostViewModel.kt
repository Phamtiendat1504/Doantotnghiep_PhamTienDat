package com.example.doantotnghiep.ViewModel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.Model.Room
import com.example.doantotnghiep.Model.User
import com.example.doantotnghiep.repository.AuthRepository
import com.example.doantotnghiep.repository.RoomRepository
import com.google.firebase.auth.FirebaseAuth

class PostViewModel : ViewModel() {

    data class PostQuotaBlockInfo(
        val unlockAt: Long
    )

    private val repository = RoomRepository()
    private val authRepository = AuthRepository()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _postResult = MutableLiveData<Pair<String, String?>?>()
    val postResult: LiveData<Pair<String, String?>?> = _postResult

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

    private val _ownerInfo = MutableLiveData<Triple<String, String, String>>()
    val ownerInfo: LiveData<Triple<String, String, String>> = _ownerInfo

    private val _userObject = MutableLiveData<User?>()
    val userObject: LiveData<User?> = _userObject

    private val _postQuotaBlocked = MutableLiveData<PostQuotaBlockInfo?>()
    val postQuotaBlocked: LiveData<PostQuotaBlockInfo?> = _postQuotaBlocked

    fun loadUserObject() {
        authRepository.loadUserObject(
            onSuccess = { user: User? -> 
                if (user != null && !user.isVerified && user.role != "admin") {
                    authRepository.loadVerificationStatusDetail(
                        onSuccess = { status: String?, reason: String? ->
                            val waitingStatuses = setOf("pending", "pending_admin_review", "queued_manual")
                            if (status in waitingStatuses) {
                                _userObject.value = user.copy(role = "pending")
                            } else if (status == "rejected") {
                                _userObject.value = user.copy(role = "rejected", occupation = reason ?: "")
                            } else {
                                _userObject.value = user
                            }
                        },
                        onFailure = { _ -> _userObject.value = user }
                    )
                } else {
                    _userObject.value = user
                }
            },
            onFailure = { error: String -> _errorMessage.value = error }
        )
    }

    fun markRulesAccepted() {
        authRepository.updateRulesAcceptedStatus(
            onSuccess = {
                val current = _userObject.value
                if (current != null) {
                    _userObject.value = current.copy(hasAcceptedRules = true)
                }
            },
            onFailure = { error: String -> _errorMessage.value = error }
        )
    }

    fun getCurrentUserId(): String? = FirebaseAuth.getInstance().currentUser?.uid

    fun checkUserRole() {
        val uid = getCurrentUserId() ?: run {
            _userRole.value = "user"
            return
        }
        authRepository.getUserRole(
            onSuccess = { role: String ->
                _userRole.value = role
                if (role != "admin" && role != "verified") {
                    checkVerificationStatus(uid)
                }
            },
            onFailure = { _: String -> _userRole.value = "user" }
        )
    }

    private fun checkVerificationStatus(uid: String) {
        repository.getVerificationStatus(
            uid,
            onSuccess = { status: String?, rejectReason: String? ->
                _verifyStatus.value = status
                if (status == "rejected") {
                    _verifyRejectReason.value = rejectReason ?: ""
                }
            },
            onFailure = { _: String -> _verifyStatus.value = null }
        )
    }

    fun loadOwnerInfo() {
        val uid = getCurrentUserId() ?: return
        authRepository.loadUserById(
            uid,
            onSuccess = { user: User? ->
                user?.let {
                    _ownerInfo.value = Triple(it.fullName, it.phone, it.avatarUrl)
                }
            },
            onFailure = { _: String -> /* ignore */ }
        )
    }

    fun resetPostResult() {
        _postResult.value = null
    }

    fun clearPostQuotaBlockedEvent() {
        _postQuotaBlocked.value = null
    }

    private fun submitPostWithUpload(context: android.content.Context, room: Room, imageUris: List<Uri>, usePurchasedSlot: Boolean = false) {
        _isLoading.value = true

        val compressedUris = imageUris.mapNotNull { uri ->
            com.example.doantotnghiep.Utils.ImageUtils.compressImage(context, uri)
        }

        if (compressedUris.isEmpty()) {
            _isLoading.value = false
            _errorMessage.value = "Lỗi nén ảnh, vui lòng thử lại."
            return
        }

        repository.postRoom(
            room,
            compressedUris,
            usePurchasedSlot = usePurchasedSlot,
            onSuccess = { roomId: String, thumbnailUrl: String? ->
                _isLoading.value = false
                _postResult.value = Pair(roomId, thumbnailUrl)
            },
            onFailure = { error: String ->
                _isLoading.value = false
                _errorMessage.value = error
            },
            onProgress = { progress: Int -> _uploadProgress.value = progress }
        )
    }

    fun postRoom(context: android.content.Context, room: Room, imageUris: List<Uri>) {
        val currentUser = _userObject.value
        if (currentUser == null) {
            _errorMessage.value = "Đang tải thông tin tài khoản. Vui lòng thử lại sau ít giây."
            return
        }
        val now = System.currentTimeMillis()
        val waitingPostUnlock = currentUser.role != "admin" &&
            currentUser.isVerified &&
            currentUser.postingUnlockAt > now
        if (waitingPostUnlock) {
            _errorMessage.value = "Quyền đăng bài sẽ được mở sau 24 giờ kể từ lúc admin duyệt."
            return
        }
        if (currentUser.role != "admin" && !currentUser.isVerified) {
            _errorMessage.value = "Tài khoản chưa đủ điều kiện đăng bài."
            return
        }

        if (room.ownerName.isBlank()) { _errorMessage.value = "Vui lòng nhập họ tên chủ trọ"; return }
        if (room.ownerPhone.isBlank()) { _errorMessage.value = "Vui lòng nhập số điện thoại"; return }
        if (room.ownerPhone.length < 10) { _errorMessage.value = "Số điện thoại không hợp lệ"; return }
        if (room.title.isBlank()) { _errorMessage.value = "Vui lòng nhập tiêu đề bài đăng"; return }
        if (room.ward.isBlank() || room.ward.contains("Chọn phường/xã")) {
            _errorMessage.value = "Vui lòng chọn khu vực (Phường/Xã)"; return
        }
        if (room.address.isBlank()) { _errorMessage.value = "Vui lòng nhập địa chỉ cụ thể"; return }
        val lat = room.latitude
        val lng = room.longitude
        if (lat == null || lng == null) {
            _errorMessage.value = "Vui lòng chọn vị trí trên bản đồ trước khi đăng bài."
            return
        }
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) {
            _errorMessage.value = "Tọa độ vị trí không hợp lệ. Vui lòng chọn lại."
            return
        }
        if (room.price <= 0) { _errorMessage.value = "Vui lòng nhập giá thuê hợp lệ"; return }
        if (room.area <= 0) { _errorMessage.value = "Vui lòng nhập diện tích hợp lệ"; return }
        if (imageUris.isEmpty()) { _errorMessage.value = "Vui lòng thêm ít nhất 1 ảnh phòng trọ"; return }
        val uid = currentUser.uid.ifBlank { getCurrentUserId().orEmpty() }
        if (uid.isBlank()) {
            _errorMessage.value = "Chưa đăng nhập. Vui lòng đăng nhập lại."
            return
        }

        repository.checkDailyPostQuota(
            uid = uid,
            limitPer24h = 3,
            purchasedSlots = currentUser.purchasedSlots,
            onAllowed = { _, usePurchasedSlot ->
                submitPostWithUpload(context, room, imageUris, usePurchasedSlot)
            },
            onBlocked = { unlockAt ->
                _postQuotaBlocked.value = PostQuotaBlockInfo(unlockAt = unlockAt)
            },
            onFailure = { error ->
                _errorMessage.value = error
            }
        )
    }

    fun checkPrePostQuota() {
        val currentUser = _userObject.value ?: return
        val uid = currentUser.uid.ifBlank { getCurrentUserId().orEmpty() }
        if (uid.isBlank()) return
        
        // Admin không bị giới hạn
        if (currentUser.role == "admin") return

        repository.checkDailyPostQuota(
            uid = uid,
            limitPer24h = 3,
            purchasedSlots = currentUser.purchasedSlots,
            onAllowed = { _, _ -> /* Do nothing, they can post */ },
            onBlocked = { unlockAt ->
                _postQuotaBlocked.value = PostQuotaBlockInfo(unlockAt = unlockAt)
            },
            onFailure = { /* Ignore */ }
        )
    }
}
