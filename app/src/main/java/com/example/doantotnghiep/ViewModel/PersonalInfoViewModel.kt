package com.example.doantotnghiep.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.repository.AuthRepository

class PersonalInfoViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _userInfo = MutableLiveData<Map<String, Any>>()
    val userInfo: LiveData<Map<String, Any>> = _userInfo

    private val _updateResult = MutableLiveData<Boolean>()
    val updateResult: LiveData<Boolean> = _updateResult

    private val _emailUpdateResult = MutableLiveData<Boolean>()
    val emailUpdateResult: LiveData<Boolean> = _emailUpdateResult

    private val _wrongPassword = MutableLiveData<Boolean>()
    val wrongPassword: LiveData<Boolean> = _wrongPassword

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun loadUserInfo() {
        _isLoading.value = true
        repository.loadUserInfo(
            onSuccess = { data ->
                _isLoading.value = false
                _userInfo.value = data
            },
            onFailure = { error ->
                _isLoading.value = false
                _errorMessage.value = error
            }
        )
    }

    fun updateUserInfo(
        fullName: String, email: String, phone: String,
        address: String, birthday: String, gender: String, occupation: String
    ) {
        _isLoading.value = true
        repository.updateUserInfo(
            fullName, email, phone, address, birthday, gender, occupation,
            onSuccess = {
                _isLoading.value = false
                _updateResult.value = true
            },
            onFailure = { error ->
                _isLoading.value = false
                _errorMessage.value = error
            }
        )
    }

    fun reauthenticateAndUpdateEmail(currentEmail: String, password: String, newEmail: String) {
        _isLoading.value = true
        repository.reauthenticateAndUpdateEmail(
            currentEmail, password, newEmail,
            onSuccess = {
                _isLoading.value = false
                _emailUpdateResult.value = true
            },
            onWrongPassword = {
                _isLoading.value = false
                _wrongPassword.value = true
            },
            onFailure = { error ->
                _isLoading.value = false
                _errorMessage.value = error
            }
        )
    }
}