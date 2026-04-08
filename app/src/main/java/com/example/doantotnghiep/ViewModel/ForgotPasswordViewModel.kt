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
        if (phone.isEmpty()) { _errorMessage.value = "Vui lòng nhập số điện thoại"; return }
        if (phone.length != 10 || !phone.startsWith("0")) {
            _errorMessage.value = "Số điện thoại phải có 10 số và bắt đầu bằng 0"; return
        }

        _isLoading.value = true
        repository.findEmailByPhone(phone,
            onSuccess = { email ->
                _isLoading.value = false
                _emailFound.value = email
            },
            onFailure = { error ->
                _isLoading.value = false
                _errorMessage.value = error
            }
        )
    }
}