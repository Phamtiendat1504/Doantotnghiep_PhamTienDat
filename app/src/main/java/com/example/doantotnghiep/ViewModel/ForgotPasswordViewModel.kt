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

    private val _phoneError = MutableLiveData<String?>()
    val phoneError: LiveData<String?> = _phoneError

    private val _generalError = MutableLiveData<String?>()
    val generalError: LiveData<String?> = _generalError

    private val _sendEmailSuccess = MutableLiveData<String>()
    val sendEmailSuccess: LiveData<String> = _sendEmailSuccess

    fun requestPasswordReset(email: String, phone: String) {
        _emailError.value = null
        _phoneError.value = null
        _generalError.value = null

        if (email.isBlank()) {
            _emailError.value = "Vui lòng nhập email"
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailError.value = "Email không hợp lệ"
            return
        }
        if (phone.isBlank()) {
            _phoneError.value = "Vui lòng nhập số điện thoại"
            return
        }
        if (phone.length < 10) {
            _phoneError.value = "Số điện thoại không hợp lệ"
            return
        }

        _isLoading.value = true
        repository.verifyEmailAndPhone(
            email = email,
            phone = phone,
            onFound = {
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
            },
            onNotFound = {
                _isLoading.value = false
                _generalError.value = "Email và số điện thoại không khớp với tài khoản nào"
            },
            onFailure = { error ->
                _isLoading.value = false
                _generalError.value = error
            }
        )
    }
}
