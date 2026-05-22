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

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun loadUserInfo() {
        _isLoading.value = true
        repository.loadUserObject(
            onSuccess = { user ->
                _isLoading.value = false
                if (user != null) {
                    val map = mutableMapOf<String, Any>()
                    map["fullName"] = user.fullName
                    map["email"] = user.email
                    map["phone"] = user.phone
                    map["address"] = user.address
                    map["birthday"] = user.birthday
                    map["gender"] = user.gender
                    map["occupation"] = user.occupation
                    _userInfo.value = map
                }
            },
            onFailure = { error ->
                _isLoading.value = false
                _errorMessage.value = error
            }
        )
    }

    fun updateUserInfo(fullName: String, email: String, phone: String, address: String, birthday: String, gender: String, occupation: String) {
        if (fullName.isBlank() || phone.isBlank()) {
            _errorMessage.value = "Họ tên và Số điện thoại không được để trống"
            return
        }
        val updates = mapOf(
            "fullName" to fullName,
            "email" to email,
            "phone" to phone,
            "address" to address,
            "birthday" to birthday,
            "gender" to gender,
            "occupation" to occupation
        )
        _isLoading.value = true
        repository.updateUserInfo(updates, 
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

    fun resetUpdateResult() { _updateResult.value = false }
}
