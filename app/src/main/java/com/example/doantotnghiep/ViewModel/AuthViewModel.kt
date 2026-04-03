package com.example.doantotnghiep.ViewModel

import android.util.Patterns
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.repository.AuthRepository

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    val isLoading = MutableLiveData<Boolean>()
    val loginResult = MutableLiveData<Boolean>()
    val registerResult = MutableLiveData<Boolean>()
    val errorMessage = MutableLiveData<String>()

    private fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPassword(password: String): Boolean {
        // Ít nhất 12 ký tự, có chữ hoa, số và ký tự đặc biệt
        val hasUpperCase = password.any { it.isUpperCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }
        return password.length >= 12 && hasUpperCase && hasDigit && hasSpecialChar
    }

    private fun isValidPhone(phone: String): Boolean {
        // Phải đúng 10 số, bắt đầu bằng 0 và toàn là chữ số
        return phone.length == 10 && phone.startsWith("0") && phone.all { it.isDigit() }
    }

    fun login(email: String, password: String) {
        if (email.isEmpty()) { errorMessage.value = "Vui lòng nhập email"; return }
        if (password.isEmpty()) { errorMessage.value = "Vui lòng nhập mật khẩu"; return }

        isLoading.value = true
        repository.login(email, password,
            onSuccess = {
                isLoading.value = false
                loginResult.value = true
            },
            onFailure = { error ->
                isLoading.value = false
                errorMessage.value = error
            }
        )
    }

    fun register(fullName: String, email: String, phone: String, password: String, confirmPassword: String) {
        if (fullName.isBlank()) { errorMessage.value = "Vui lòng nhập họ và tên"; return }
        if (email.isBlank()) { errorMessage.value = "Vui lòng nhập email"; return }
        if (!isValidEmail(email)) { errorMessage.value = "Định dạng email không hợp lệ"; return }
        
        // SỬA LỖI XÁC THỰC SĐT
        if (phone.isBlank()) { errorMessage.value = "Vui lòng nhập số điện thoại"; return }
        if (!isValidPhone(phone)) { 
            errorMessage.value = "Số điện thoại không hợp lệ (phải có 10 số và bắt đầu bằng 0)"; 
            return 
        }

        if (password.isEmpty()) { errorMessage.value = "Vui lòng nhập mật khẩu"; return }
        if (!isValidPassword(password)) {
            errorMessage.value = "Mật khẩu phải có ít nhất 12 ký tự, gồm chữ hoa, số và ký tự đặc biệt"
            return
        }
        if (password != confirmPassword) { errorMessage.value = "Mật khẩu xác nhận không khớp"; return }

        isLoading.value = true
        repository.register(fullName, email, phone, password,
            onSuccess = {
                isLoading.value = false
                registerResult.value = true
            },
            onFailure = { error ->
                isLoading.value = false
                errorMessage.value = error
            }
        )
    }
}
