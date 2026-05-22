package com.example.doantotnghiep.ViewModel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.Model.User
import com.example.doantotnghiep.repository.VerificationRepository
import com.google.firebase.auth.FirebaseAuth

class VerifyLandlordViewModel : ViewModel() {

    enum class SubmitStatus {
        SUCCESS_AUTO_VERIFIED,
        ESCALATED_TO_ADMIN
    }

    data class SubmitUiResult(
        val status: SubmitStatus,
        val message: String
    )

    private val repository = VerificationRepository()

    private val _ownerInfo = MutableLiveData<User>()
    val ownerInfo: LiveData<User> = _ownerInfo

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _submitResult = MutableLiveData<SubmitUiResult?>()
    val submitResult: LiveData<SubmitUiResult?> = _submitResult

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

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

    fun resetFailureCounter(context: Context) {
        repository.resetAutoFailureCounter(context)
    }

    fun clearSubmitResult() {
        _submitResult.value = null
    }

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
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            ?: run {
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

                        // Nếu không pass và không gửi admin, giải phóng CCCD để lần sau (hoặc người khác) nhập lại không bị lỗi
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
                } // end syncCounterFromServer
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
                        // Phần 1: Lý do cụ thể của lần thất bại này
                        if (!meta.autoCheckReason.isNullOrBlank()) {
                            append("⚠️ Lý do xác thực thất bại:\n")
                            append(meta.autoCheckReason)
                            append("\n\n")
                        }
                        // Phần 2: Thông báo hệ thống hành động
                        append("📋 Hồ sơ của bạn đã được hệ thống tự động gửi đến admin để xét duyệt thủ công do xác thực sai quá 3 lần trong ngày.\n\n")
                        // Phần 3: Hướng dẫn chờ đợi
                        append("⏳ Vui lòng chờ phản hồi từ admin trong vòng 24 giờ. Bạn sẽ nhận được thông báo khi hồ sơ được duyệt.")
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
                _isLoading.value = false
                _errorMessage.value = e
            }
        )
    }
}
