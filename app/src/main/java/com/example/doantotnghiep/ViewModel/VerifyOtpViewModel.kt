package com.example.doantotnghiep.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.repository.AuthRepository

class VerifyOtpViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _verifySuccess = MutableLiveData<String>()
    val verifySuccess: LiveData<String> = _verifySuccess // chứa email để hiển thị dialog

    private val _invalidOtp = MutableLiveData<Boolean>()
    val invalidOtp: LiveData<Boolean> = _invalidOtp

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun verifyOtp(verificationId: String, otp: String, email: String) {
        if (otp.isEmpty()) { _errorMessage.value = "otp_empty"; return }
        if (otp.length != 6) { _errorMessage.value = "otp_invalid"; return }

        _isLoading.value = true
        repository.verifyOtpAndSendResetEmail(
            verificationId, otp, email,
            onSuccess = {
                _isLoading.value = false
                _verifySuccess.value = email
            },
            onInvalidOtp = {
                _isLoading.value = false
                _invalidOtp.value = true
            },
            onFailure = { error ->
                _isLoading.value = false
                _errorMessage.value = error
            }
        )
    }
}