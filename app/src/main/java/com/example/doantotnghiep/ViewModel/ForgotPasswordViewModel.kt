package com.example.doantotnghiep.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.repository.AuthRepository

class ForgotPasswordViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _emailError = MutableLiveData<String?>()
    val emailError: LiveData<String?> = _emailError

    private val _generalError = MutableLiveData<String?>()
    val generalError: LiveData<String?> = _generalError

    private val _sendEmailSuccess = MutableLiveData<String?>()
    val sendEmailSuccess: LiveData<String?> = _sendEmailSuccess

    private val resetAttemptTimestamps = ArrayDeque<Long>()
    private val MAX_RESET_ATTEMPTS = 3
    private val RESET_WINDOW_MS = 120_000L

    fun clearSendEmailSuccess() {
        _sendEmailSuccess.value = null
    }

    fun clearGeneralError() {
        _generalError.value = null
    }

    fun requestPasswordReset(email: String) {
        _emailError.value = null
        _generalError.value = null

        val now = System.currentTimeMillis()
        resetAttemptTimestamps.removeAll { now - it > RESET_WINDOW_MS }
        if (resetAttemptTimestamps.size >= MAX_RESET_ATTEMPTS) {
            val waitSeconds = ((RESET_WINDOW_MS - (now - resetAttemptTimestamps.first())) / 1000).coerceAtLeast(1)
            _generalError.value = "Bạn đã gửi quá nhiều yêu cầu. Vui lòng đợi $waitSeconds giây rồi thử lại."
            return
        }
        resetAttemptTimestamps.addLast(now)

        if (email.isBlank()) {
            _emailError.value = "Vui lòng nhập email"
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailError.value = "Email không hợp lệ"
            return
        }
        _isLoading.value = true
        repository.sendPasswordResetEmail(
            email = email,
            onSuccess = {
                _isLoading.value = false
                _sendEmailSuccess.value = email
            },
            onFailure = { error ->
                _isLoading.value = false
                _generalError.value = error
            }
        )
    }
}
