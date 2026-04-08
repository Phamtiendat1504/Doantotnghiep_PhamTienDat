package com.example.doantotnghiep.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.repository.AuthRepository

class ResetPasswordViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _resetResult = MutableLiveData<Boolean>()
    val resetResult: LiveData<Boolean> = _resetResult

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun resetPassword(email: String, newPassword: String, confirmPassword: String) {
        if (newPassword.isEmpty()) { _errorMessage.value = "new_empty"; return }
        if (newPassword.length < 12 ||
            !newPassword.any { it.isUpperCase() } ||
            !newPassword.any { it.isDigit() } ||
            !newPassword.any { !it.isLetterOrDigit() }
        ) { _errorMessage.value = "new_weak"; return }
        if (confirmPassword.isEmpty()) { _errorMessage.value = "confirm_empty"; return }
        if (newPassword != confirmPassword) { _errorMessage.value = "confirm_mismatch"; return }

        _isLoading.value = true
        repository.updatePasswordAfterOtp(email, newPassword,
            onSuccess = {
                _isLoading.value = false
                _resetResult.value = true
            },
            onFailure = { error ->
                _isLoading.value = false
                _errorMessage.value = error
            }
        )
    }
}