package com.example.doantotnghiep.ViewModel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.repository.VerificationRepository

class VerifyLandlordViewModel : ViewModel() {

    private val repository = VerificationRepository()

    private val _ownerInfo = MutableLiveData<Pair<String, String>>()
    val ownerInfo: LiveData<Pair<String, String>> = _ownerInfo

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _submitResult = MutableLiveData<Boolean>()
    val submitResult: LiveData<Boolean> = _submitResult

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun loadUserInfo() {
        repository.loadCurrentUserInfo(
            onSuccess = { fullName, phone -> _ownerInfo.value = Pair(fullName, phone) },
            onFailure = { e -> _errorMessage.value = e }
        )
    }

    fun submitVerification(
        fullName: String, cccd: String, phone: String, address: String,
        frontUri: Uri, backUri: Uri
    ) {
        _isLoading.value = true
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
    }
}