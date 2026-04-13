package com.example.doantotnghiep.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.repository.AuthRepository

class ForgotPasswordViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _emailFound = MutableLiveData<String>()
    val emailFound: LiveData<String> = _emailFound

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun findEmailByPhone(phone: String) {
        if (phone.length < 10) {
            _errorMessage.value = "Số điện thoại không hợp lệ"
            return
        }
        _isLoading.value = true
        repository.findEmailByPhone(
            phone,
            onSuccess = { email: String ->
                _isLoading.value = false
                _emailFound.value = email
            },
            onFailure = { error: String ->
                _isLoading.value = false
                _errorMessage.value = error
            }
        )
    }
}
