package com.example.doantotnghiep.ViewModel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.Model.User
import com.example.doantotnghiep.repository.VerificationRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

class VerifyLandlordViewModel : ViewModel() {

    enum class SubmitStatus {
        SUCCESS_AUTO_VERIFIED,
        ESCALATED_TO_ADMIN
    }

    data class SubmitUiResult(
        val status: SubmitStatus,
        val message: String
    )

    // Lớp trạng thái hồ sơ phục vụ view
    sealed class VerificationState {
        object Loading : VerificationState()
        object FormReady : VerificationState()
        class AlreadyApprovedWaiting(val hours: Long, val minutes: Long, val unlockTime: String) : VerificationState()
        object AlreadyApprovedReady : VerificationState()
        object VerificationPending : VerificationState()
        // Hồ sơ bị escalated lên Admin nhưng đã quá 24h vẫn chưa được duyệt
        object VerificationEscalatedExpired : VerificationState()
        class Error(val message: String) : VerificationState()
    }

    private val repository = VerificationRepository()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _ownerInfo = MutableLiveData<User>()
    val ownerInfo: LiveData<User> = _ownerInfo

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _submitResult = MutableLiveData<SubmitUiResult?>()
    val submitResult: LiveData<SubmitUiResult?> = _submitResult

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    // LiveData quan sát trạng thái hồ sơ của người dùng
    private val _verificationState = MutableLiveData<VerificationState>()
    val verificationState: LiveData<VerificationState> = _verificationState

    // LiveData báo kết quả hủy hồ sơ thành công hay thất bại
    private val _cancelResult = MutableLiveData<Boolean?>()
    val cancelResult: LiveData<Boolean?> = _cancelResult

    fun loadUserInfo() {
        repository.loadCurrentUserInfo(
            onSuccess = { fullName, phone, email, address ->
                _ownerInfo.value = User(
                    fullName = fullName,
                    phone = phone,
                    email = email,
                    address = address
                )
            },
            onFailure = { e -> _errorMessage.value = e }
        )
    }

    // Xác minh danh tính -1-
    // Kéo dữ liệu từ Server để quyết định trạng thái hồ sơ của người dùng:
// 1. Nếu đã xác minh (isVerified = true): Kiểm tra xem có đang bị khóa chờ 24h hay không.
// 2. Nếu chưa xác minh: Kiểm tra bảng 'verifications' xem có đơn nào đang chờ duyệt (pending) không.
// 3. Nếu chưa từng gửi đơn: Báo trạng thái FormReady để mở form cho người dùng bắt đầu điền.
    fun checkCurrentStatus(context: Context) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _verificationState.value = VerificationState.Error("Chưa đăng nhập")
            return
        }
        _verificationState.value = VerificationState.Loading

        db.collection("users").document(uid)
            .get(Source.SERVER)
            .addOnSuccessListener { userDoc ->
                val isVerified = userDoc.getBoolean("isVerified") ?: false
                val postingUnlockAt = userDoc.getLong("postingUnlockAt") ?: 0L
                val now = System.currentTimeMillis()

                if (isVerified) {
                    if (postingUnlockAt > now) {
                        val totalMinutes = ((postingUnlockAt - now).coerceAtLeast(0L)) / 60_000L
                        val hours = totalMinutes / 60L
                        val minutes = totalMinutes % 60L
                        val formatter = java.text.SimpleDateFormat("HH:mm dd/MM/yyyy", java.util.Locale("vi", "VN"))
                        val unlockTime = formatter.format(java.util.Date(postingUnlockAt))
                        _verificationState.value = VerificationState.AlreadyApprovedWaiting(hours, minutes, unlockTime)
                    } else {
                        _verificationState.value = VerificationState.AlreadyApprovedReady
                    }
                    return@addOnSuccessListener
                }

                // Nếu chưa xác minh, kiểm tra hồ sơ xác minh verifications
                db.collection("verifications").document(uid)
                    .get(Source.SERVER)
                    .addOnSuccessListener { verifyDoc ->
                        val status = if (verifyDoc.exists()) verifyDoc.getString("status") else null
                        val escalatedStatuses = setOf("pending_admin_review", "queued_manual")
                        val allWaitingStatuses = setOf("pending", "pending_admin_review", "queued_manual")
                        if (status in allWaitingStatuses) {
                            // Kiểm tra nếu hồ sơ đã được escalate lên Admin thì kiểm tra 24h
                            val escalationDeadlineAt = verifyDoc.getLong("escalationDeadlineAt") ?: 0L
                            val isEscalated = verifyDoc.getBoolean("escalatedToAdmin") == true
                                    || status in escalatedStatuses
                            val now = System.currentTimeMillis()
                            if (isEscalated && escalationDeadlineAt > 0L && now > escalationDeadlineAt) {
                                // Đã quá 24h Admin chưa duyệt -> cho phép người dùng hủy và nộp lại
                                _verificationState.value = VerificationState.VerificationEscalatedExpired
                            } else {
                                _verificationState.value = VerificationState.VerificationPending
                            }
                        } else {
                            if (!verifyDoc.exists()) {
                                viewModelScopeResetCounter(context)
                            }
                            _verificationState.value = VerificationState.FormReady
                        }
                    }
                    .addOnFailureListener { e ->
                        _verificationState.value = VerificationState.Error("Không thể tải trạng thái hồ sơ xác minh: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                _verificationState.value = VerificationState.Error("Không thể tải trạng thái xác minh từ máy chủ: ${e.message}")
            }
    }

    private fun viewModelScopeResetCounter(context: Context) {
        repository.resetAutoFailureCounter(context)
    }

    fun resetFailureCounter(context: Context) {
        repository.resetAutoFailureCounter(context)
    }

    // Hàm hủy hồ sơ xác minh hết hạn để người dùng nộp lại từ đầu
    fun cancelExpiredVerification(context: Context) {
        val uid = auth.currentUser?.uid ?: return
        _isLoading.value = true
        repository.cancelPendingVerification(
            uid = uid,
            onSuccess = {
                _isLoading.value = false
                // Reset counter để cho phép user gửi lại bình thường
                repository.resetAutoFailureCounter(context)
                _cancelResult.value = true
            },
            onFailure = { e ->
                _isLoading.value = false
                _errorMessage.value = e
                _cancelResult.value = false
            }
        )
    }

    fun clearCancelResult() { _cancelResult.value = null }

    fun clearSubmitResult() {
        _submitResult.value = null
    }

    // Xác minh danh tính -2-
    // Xử lý luồng nộp hồ sơ xác minh với các bước bảo mật:
// 1. Check trùng lặp: Gọi Database kiểm tra Số CCCD đã tồn tại chưa.
// 2. Chạy Auto-check: So sánh độ khớp của văn bản và hình ảnh.
// 3. Phân nhánh kết quả:
//    - Pass: Duyệt tự động thành công (SUCCESS_AUTO_VERIFIED).
//    - Failed > 3 lần: Chuyển sang cho Admin duyệt (ESCALATED_TO_ADMIN).
//    - Failed <= 3 lần: Báo lỗi và trừ đi 1 lượt thử lại trong ngày.
    fun submitVerification(
        context: Context,
        fullName: String,
        email: String,
        cccd: String,
        phone: String,
        address: String,
        frontUri: Uri,
        backUri: Uri
    ) {
        val currentUid = auth.currentUser?.uid ?: run {
            _errorMessage.value = "Chưa đăng nhập"
            return
        }

        _isLoading.value = true

        repository.checkCccdExists(
            cccdNumber = cccd,
            currentUid = currentUid,
            onExists = {
                _isLoading.value = false
                _errorMessage.value = "Số CCCD này đã được đăng ký bởi tài khoản khác. Vui lòng nhập lại."
            },
            onNotExists = {
                repository.syncCounterFromServer(context, currentUid) {
                    repository.runAutoCheckCccd(
                        context = context,
                        fullName = fullName,
                        enteredCccd = cccd,
                        frontUri = frontUri,
                        backUri = backUri,
                        onResult = { autoResult ->
                            if (autoResult.passed) {
                                val meta = VerificationRepository.VerificationSubmitMeta(
                                    autoCheckStatus = "pass",
                                    autoCheckReason = "",
                                    autoCheckRecognizedCccd = autoResult.recognizedCccd,
                                    autoFailCountToday = autoResult.failCountToday,
                                    escalatedToAdmin = false
                                )
                                uploadVerification(
                                    fullName = fullName,
                                    email = email,
                                    cccd = cccd,
                                    phone = phone,
                                    address = address,
                                    frontUri = frontUri,
                                    backUri = backUri,
                                    meta = meta,
                                    status = SubmitStatus.SUCCESS_AUTO_VERIFIED
                                )
                                return@runAutoCheckCccd
                            }

                            if (autoResult.escalatedToAdmin) {
                                val meta = VerificationRepository.VerificationSubmitMeta(
                                    autoCheckStatus = "failed_escalated",
                                    autoCheckReason = autoResult.reason,
                                    autoCheckRecognizedCccd = autoResult.recognizedCccd,
                                    autoFailCountToday = autoResult.failCountToday,
                                    escalatedToAdmin = true
                                )
                                uploadVerification(
                                    fullName = fullName,
                                    email = email,
                                    cccd = cccd,
                                    phone = phone,
                                    address = address,
                                    frontUri = frontUri,
                                    backUri = backUri,
                                    meta = meta,
                                    status = SubmitStatus.ESCALATED_TO_ADMIN
                                )
                                return@runAutoCheckCccd
                            }

                            repository.releaseCccd(cccd)

                            _isLoading.value = false
                            val remain = autoResult.remainingAutoRetries
                            _errorMessage.value = buildString {
                                append(autoResult.reason)
                                append("\n\nBạn còn ")
                                append(remain)
                                append(" lần thử lại trong hôm nay trước khi hệ thống chuyển hồ sơ sang admin.")
                            }
                        }
                    )
                }
            },
            onFailure = { e ->
                _isLoading.value = false
                _errorMessage.value = e
            }
        )
    }

    private fun uploadVerification(
        fullName: String,
        email: String,
        cccd: String,
        phone: String,
        address: String,
        frontUri: Uri,
        backUri: Uri,
        meta: VerificationRepository.VerificationSubmitMeta,
        status: SubmitStatus
    ) {
        repository.submitVerification(
            fullName = fullName,
            email = email,
            cccd = cccd,
            phone = phone,
            address = address,
            frontUri = frontUri,
            backUri = backUri,
            meta = meta,
            onSuccess = {
                _isLoading.value = false
                val message = if (status == SubmitStatus.ESCALATED_TO_ADMIN) {
                    buildString {
                        if (!meta.autoCheckReason.isNullOrBlank()) {
                            append("Lý do xác thực thất bại:\n")
                            append(meta.autoCheckReason)
                            append("\n\n")
                        }
                        append("Hồ sơ của bạn đã được hệ thống tự động gửi đến admin để xét duyệt thủ công do xác thực sai quá 3 lần trong ngày.\n\n")
                        append("Vui lòng chờ phản hồi từ admin trong vòng 24 giờ. Bạn sẽ nhận được thông báo khi hồ sơ được duyệt.")
                    }
                } else {
                    "Thông tin trùng khớp chính xác! Tài khoản của bạn đã được xác minh thành công."
                }
                _submitResult.value = SubmitUiResult(
                    status = status,
                    message = message
                )
            },
            onFailure = { e ->
                repository.releaseCccd(cccd)
                _isLoading.value = false
                _errorMessage.value = e
            }
        )
    }

    fun resetErrorMessage() { _errorMessage.value = "" }
}
