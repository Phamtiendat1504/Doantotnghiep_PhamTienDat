package com.example.doantotnghiep.ViewModel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.Model.Room
import com.example.doantotnghiep.Model.User
import com.example.doantotnghiep.repository.AuthRepository
import com.example.doantotnghiep.repository.RoomRepository
import com.example.doantotnghiep.repository.UserRepository
import com.example.doantotnghiep.usecase.CheckPostQuotaUseCase
import com.google.firebase.auth.FirebaseAuth

class PostViewModel : ViewModel() {

    data class PostQuotaBlockInfo(
        val unlockAt: Long
    )

    data class OwnerInfo(val name: String, val phone: String, val avatarUrl: String, val gender: String)

    private val repository = RoomRepository()
    private val checkPostQuotaUseCase = CheckPostQuotaUseCase(repository)
    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

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

    private val _ownerInfo = MutableLiveData<OwnerInfo>()
    val ownerInfo: LiveData<OwnerInfo> = _ownerInfo

    private val _userObject = MutableLiveData<User?>()
    val userObject: LiveData<User?> = _userObject

    private val _postQuotaBlocked = MutableLiveData<PostQuotaBlockInfo?>()
    val postQuotaBlocked: LiveData<PostQuotaBlockInfo?> = _postQuotaBlocked

    private val _showLastPurchasedSlotWarning = MutableLiveData<Boolean>()
    val showLastPurchasedSlotWarning: LiveData<Boolean> = _showLastPurchasedSlotWarning
    
    private var cachedPendingRoom: Room? = null
    private var cachedPendingUris: List<Uri>? = null

    fun proceedWithPendingPost(context: android.content.Context) {
        val room = cachedPendingRoom
        val uris = cachedPendingUris
        if (room != null && uris != null) {
            submitPostWithUpload(context, room, uris, usePurchasedSlot = true)
        }
        clearCachedPendingPost()
    }

    fun cancelPendingPost() {
        clearCachedPendingPost()
    }

    private fun clearCachedPendingPost() {
        cachedPendingRoom = null
        cachedPendingUris = null
    }

    fun resetLastPurchasedSlotWarning() {
        _showLastPurchasedSlotWarning.value = false
    }

    private var userListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var verificationListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    fun loadUserObject(forceFromServer: Boolean = false) {
        val uid = getCurrentUserId() ?: run {
            _userObject.value = null
            return
        }

        if (forceFromServer) {
            userRepository.getUserFromServer(
                uid,
                onSuccess = { user ->
                    user?.let { _userObject.value = it }
                },
                onFailure = { /* ignore */ }
            )
        }

        userListenerRegistration?.remove()
        userListenerRegistration = userRepository.listenUser(uid) { user ->
            if (user == null) return@listenUser
            if (!user.isVerified && user.role != "admin") {
                listenVerificationStatus(user, uid)
            } else {
                verificationListenerRegistration?.remove()
                verificationListenerRegistration = null
                _userObject.value = user
            }
        }
    }

    private fun listenVerificationStatus(baseUser: User, uid: String) {
        verificationListenerRegistration?.remove()
        verificationListenerRegistration = userRepository.listenVerificationStatus(uid) { status, reason, autoCheckStatus ->
            val waitingStatuses = setOf("pending", "pending_admin_review", "queued_manual")
            if (autoCheckStatus == "pass" || status == "approved") {
                _userObject.value = baseUser.copy(isVerified = true)
            } else if (status in waitingStatuses) {
                _userObject.value = baseUser.copy(role = "pending")
            } else if (status == "rejected") {
                _userObject.value = baseUser.copy(role = "rejected", verificationRejectReason = reason ?: "")
            } else {
                _userObject.value = baseUser
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        userListenerRegistration?.remove()
        verificationListenerRegistration?.remove()
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
                    _ownerInfo.value = OwnerInfo(it.fullName, it.phone, it.avatarUrl, it.gender)
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
                val remainMessage = if (usePurchasedSlot) {
                    "Bài đăng đã được duyệt. Bạn vừa sử dụng 1 lượt đăng bài tính phí (Mua thêm)."
                } else {
                    "Bài đăng đã được duyệt. Bạn đang sử dụng lượt đăng bài miễn phí trong ngày."
                }
                // Dùng thumbnailUrl để chứa câu thông báo (trick để khỏi sửa quá nhiều data class)
                _postResult.value = Pair(roomId, remainMessage)
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

        // Ép buộc thông tin Họ tên, Số điện thoại và Giới tính đối với tài khoản KYC (đã xác minh) và không phải Admin
        val finalRoom = if (currentUser.isVerified && currentUser.role != "admin") {
            room.copy(
                ownerName = currentUser.fullName,
                ownerPhone = currentUser.phone,
                ownerGender = currentUser.gender
            )
        } else {
            room
        }

        if (finalRoom.ownerName.isBlank()) { _errorMessage.value = "Vui lòng nhập họ tên chủ trọ"; return }
        if (finalRoom.ownerPhone.isBlank()) { _errorMessage.value = "Vui lòng nhập số điện thoại"; return }
        
        // Đồng bộ validation số điện thoại Việt Nam chuẩn
        val phoneRegex = "^(03|05|07|08|09)\\d{8}$".toRegex()
        if (!finalRoom.ownerPhone.matches(phoneRegex)) {
            _errorMessage.value = "Số điện thoại chủ trọ không hợp lệ (phải gồm 10 chữ số chuẩn Việt Nam)"
            return
        }

        if (finalRoom.title.isBlank()) { _errorMessage.value = "Vui lòng nhập tiêu đề bài đăng"; return }
        if (finalRoom.ward.isBlank() || finalRoom.ward.contains("Chọn phường/xã")) {
            _errorMessage.value = "Vui lòng chọn khu vực (Phường/Xã)"; return
        }
        // Phải có ít nhất một trong hai: địa chỉ cụ thể HOẶC vị trí trên bản đồ
        val lat = finalRoom.latitude
        val lng = finalRoom.longitude
        val hasAddress = finalRoom.address.isNotBlank()
        val hasMapLocation = lat != null && lng != null
        if (!hasAddress && !hasMapLocation) {
            _errorMessage.value = "Vui lòng nhập địa chỉ cụ thể hoặc chọn vị trí trên bản đồ"
            return
        }
        if (hasMapLocation && (lat!! !in -90.0..90.0 || lng!! !in -180.0..180.0)) {
            _errorMessage.value = "Tọa độ vị trí không hợp lệ. Vui lòng chọn lại."
            return
        }
        if (finalRoom.price <= 0) { _errorMessage.value = "Vui lòng nhập giá thuê hợp lệ"; return }
        if (finalRoom.area <= 0) { _errorMessage.value = "Vui lòng nhập diện tích hợp lệ"; return }
        if (imageUris.isEmpty()) { _errorMessage.value = "Vui lòng thêm ít nhất 1 ảnh phòng trọ"; return }
        val uid = currentUser.uid.ifBlank { getCurrentUserId().orEmpty() }
        if (uid.isBlank()) {
            _errorMessage.value = "Chưa đăng nhập. Vui lòng đăng nhập lại."
            return
        }

        checkPostQuotaUseCase(
            uid = uid,
            onAllowed = { remaining, usePurchasedSlot ->
                if (usePurchasedSlot && remaining == 1) {
                    cachedPendingRoom = finalRoom
                    cachedPendingUris = imageUris
                    _showLastPurchasedSlotWarning.value = true
                } else {
                    submitPostWithUpload(context, finalRoom, imageUris, usePurchasedSlot)
                }
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

        checkPostQuotaUseCase(
            uid = uid,
            onAllowed = { _, _ -> /* Do nothing, they can post */ },
            onBlocked = { unlockAt ->
                _postQuotaBlocked.value = PostQuotaBlockInfo(unlockAt = unlockAt)
            },
            onFailure = { /* Ignore */ }
        )
    }
}
