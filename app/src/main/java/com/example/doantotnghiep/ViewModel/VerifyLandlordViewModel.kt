package com.example.doantotnghiep.ViewModel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.repository.VerificationRepository

class VerifyLandlordViewModel : ViewModel() {

    private val repository = VerificationRepository()

    private val _ownerInfo = MutableLiveData<com.example.doantotnghiep.Model.User>()
    val ownerInfo: LiveData<com.example.doantotnghiep.Model.User> = _ownerInfo

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _submitResult = MutableLiveData<Boolean>()
    val submitResult: LiveData<Boolean> = _submitResult

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun loadUserInfo() {
        repository.loadCurrentUserInfo(
            onSuccess = { fullName, phone, email, address ->
                _ownerInfo.value = com.example.doantotnghiep.Model.User(
                    fullName = fullName,
                    phone = phone,
                    email = email,
                    address = address
                )
            },
            onFailure = { e -> _errorMessage.value = e }
        )
    }

    fun submitVerification(
        fullName: String, cccd: String, phone: String, address: String,
        frontUri: Uri, backUri: Uri
    ) {
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: run { _errorMessage.value = "Chưa đăng nhập"; return }

        _isLoading.value = true

        // Bước 1: Kiểm tra số CCCD có bị trùng với người khác không
        repository.checkCccdExists(
            cccdNumber = cccd,
            currentUid = currentUid,
            onExists = {
                _isLoading.value = false
                _errorMessage.value = "Số CCCD này đã được đăng ký bởi tài khoản khác trong hệ thống. Vui lòng kiểm tra lại."
            },
            onNotExists = {
                // Bước 2: CCCD chưa trùng → tiến hành gửi hồ sơ
                repository.submitVerification(
                    fullName, cccd, phone, address, frontUri, backUri,
                    onSuccess = {
                        _isLoading.value = false
                        _submitResult.value = true
                    },
                    onFailure = { e ->
                        _isLoading.value = false
                        _errorMessage.value = e
                    }
                )
            },
            onFailure = { e ->
                _isLoading.value = false
                _errorMessage.value = e
            }
        )
    }
}