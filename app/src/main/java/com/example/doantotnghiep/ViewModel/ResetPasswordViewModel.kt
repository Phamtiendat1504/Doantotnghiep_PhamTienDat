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

    fun resetPassword(email: String, newPass: String, confirmPass: String) {
        if (newPass.isEmpty()) { _errorMessage.value = "Vui lòng nhập mật khẩu mới"; return }
        if (newPass.length < 6) { _errorMessage.value = "Mật khẩu quá ngắn"; return }
        if (newPass != confirmPass) { _errorMessage.value = "Mật khẩu không khớp"; return }

        _isLoading.value = true
        repository.updatePasswordAfterOtp(
            email, newPass,
            onSuccess = {
                _isLoading.postValue(false)
                _resetResult.postValue(true)
            },
            onFailure = { error ->
                _isLoading.postValue(false)
                _errorMessage.postValue(error)
            }
        )
    }
}
