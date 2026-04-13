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
    val verifySuccess: LiveData<String> = _verifySuccess

    private val _invalidOtp = MutableLiveData<Boolean>()
    val invalidOtp: LiveData<Boolean> = _invalidOtp

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun verifyOtp(verificationId: String, otpInput: String, email: String) {
        if (otpInput.isEmpty()) { 
            _errorMessage.value = "otp_empty"
            return 
        }
        if (otpInput.length < 6) {
            _errorMessage.value = "otp_invalid"
            return
        }

        _isLoading.value = true
        
        // Tạo credential từ mã OTP người dùng nhập
        val credential = com.google.firebase.auth.PhoneAuthProvider.getCredential(verificationId, otpInput)
        
        // Dùng Firebase Auth để xác thực mã này
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                // Xác thực OTP thành công, không cần gửi Email nữa
                auth.signOut() 
                _isLoading.value = false
                _verifySuccess.value = email // Báo thành công để chuyển màn hình
            }
            .addOnFailureListener {
                _isLoading.value = false
                _invalidOtp.value = true
            }
    }
}
