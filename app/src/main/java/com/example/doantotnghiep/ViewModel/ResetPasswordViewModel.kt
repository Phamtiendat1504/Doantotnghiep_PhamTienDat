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

    /**
     * Gửi lại email đặt lại mật khẩu nếu người dùng cần.
     * Luồng mới: Firebase gửi link → người dùng click link trên email → đổi mật khẩu trên web Firebase.
     */
    fun resetPassword(email: String, newPass: String, confirmPass: String) {
        if (email.isBlank()) {
            _errorMessage.value = "Không xác định được email. Vui lòng thử lại từ đầu."
            return
        }

        _isLoading.value = true
        repository.sendPasswordResetEmail(
            email = email,
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
