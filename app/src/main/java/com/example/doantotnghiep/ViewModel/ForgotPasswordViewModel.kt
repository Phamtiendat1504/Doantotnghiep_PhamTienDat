package com.example.doantotnghiep.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.doantotnghiep.Utils.RateLimiter
import com.example.doantotnghiep.repository.AuthRepository

/**
 * Đã chuyển sang AndroidViewModel để sử dụng RateLimiter lưu SharedPreferences,
 * ngăn người dùng bypass giới hạn gửi email bằng cách khởi động lại app.
 */
class ForgotPasswordViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _emailError = MutableLiveData<String?>()
    val emailError: LiveData<String?> = _emailError

    private val _generalError = MutableLiveData<String?>()
    val generalError: LiveData<String?> = _generalError

    private val _sendEmailSuccess = MutableLiveData<String?>()
    val sendEmailSuccess: LiveData<String?> = _sendEmailSuccess

    // Rate limiting lưu vào SharedPreferences — bền vững qua các lần restart app
    private val resetRateLimiter = RateLimiter(
        context     = application,
        key         = "rate_forgot_password",
        maxAttempts = 3,
        windowMs    = 120_000L
    )

    fun clearSendEmailSuccess() {
        _sendEmailSuccess.value = null
    }

    fun clearGeneralError() {
        _generalError.value = null
    }

    fun requestPasswordReset(email: String) {
        _emailError.value = null
        _generalError.value = null

        // Rate limiting lưu vào SharedPreferences — bền vững qua các lần restart app
        if (!resetRateLimiter.tryConsume()) {
            val wait = resetRateLimiter.secondsUntilNextAllowed()
            _generalError.value = "Bạn đã gửi quá nhiều yêu cầu. Vui lòng đợi $wait giây rồi thử lại."
            return
        }

        if (email.isBlank()) {
            _emailError.value = "Vui lòng nhập email"
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailError.value = "Email không hợp lệ"
            return
        }
        if (!email.endsWith("@gmail.com", ignoreCase = true)) {
            _emailError.value = "Email phải có đuôi @gmail.com"
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
