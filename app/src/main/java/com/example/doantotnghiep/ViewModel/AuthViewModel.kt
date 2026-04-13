package com.example.doantotnghiep.ViewModel

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.repository.AuthRepository

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()
    
    fun isLoggedIn(): Boolean = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null
    fun getCurrentUserEmail(): String = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: ""
    fun logOut() {
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
    }

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _loginResult = MutableLiveData<Boolean>()
    val loginResult: LiveData<Boolean> = _loginResult

    private val _registerResult = MutableLiveData<Boolean>()
    val registerResult: LiveData<Boolean> = _registerResult

    private val _changePasswordResult = MutableLiveData<Boolean>()
    val changePasswordResult: LiveData<Boolean> = _changePasswordResult

    private val _wrongOldPassword = MutableLiveData<Boolean>()
    val wrongOldPassword: LiveData<Boolean> = _wrongOldPassword

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _lockInfo = MutableLiveData<Pair<Boolean, Triple<String, Long, Int>>>()
    val lockInfo: LiveData<Pair<Boolean, Triple<String, Long, Int>>> = _lockInfo

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
        if (email.isEmpty()) { _errorMessage.value = "Vui lòng nhập email"; return }
        if (password.isEmpty()) { _errorMessage.value = "Vui lòng nhập mật khẩu"; return }

        _isLoading.value = true
        repository.login(email, password,
            onSuccess = {
                _isLoading.value = false
                _loginResult.value = true
            },
            onFailure = { error ->
                _isLoading.value = false
                _errorMessage.value = error
            }
        )
    }

    fun changePassword(oldPassword: String, newPassword: String, confirmPassword: String) {
        if (oldPassword.isEmpty()) { _errorMessage.value = "old_empty"; return }
        if (newPassword.isEmpty()) { _errorMessage.value = "new_empty"; return }
        if (newPassword.length < 12 ||
            !newPassword.any { it.isUpperCase() } ||
            !newPassword.any { it.isDigit() } ||
            !newPassword.any { !it.isLetterOrDigit() }
        ) { _errorMessage.value = "new_weak"; return }
        if (confirmPassword.isEmpty()) { _errorMessage.value = "confirm_empty"; return }
        if (newPassword != confirmPassword) { _errorMessage.value = "confirm_mismatch"; return }
        if (oldPassword == newPassword) { _errorMessage.value = "same_password"; return }

        _isLoading.value = true
        repository.changePassword(
            oldPassword, newPassword,
            onSuccess = {
                _isLoading.value = false
                _changePasswordResult.value = true
            },
            onWrongOldPassword = {
                _isLoading.value = false
                _wrongOldPassword.value = true
            },
            onFailure = { error ->
                _isLoading.value = false
                _errorMessage.value = error
            }
        )
    }

    fun register(fullName: String, email: String, phone: String, password: String, confirmPassword: String) {
        if (fullName.isBlank()) { _errorMessage.value = "Vui lòng nhập họ và tên"; return }
        if (email.isBlank()) { _errorMessage.value = "Vui lòng nhập email"; return }
        if (!isValidEmail(email)) { _errorMessage.value = "Định dạng email không hợp lệ"; return }

        if (phone.isBlank()) { _errorMessage.value = "Vui lòng nhập số điện thoại"; return }
        if (!isValidPhone(phone)) {
            _errorMessage.value = "Số điện thoại không hợp lệ (phải có 10 số và bắt đầu bằng 0)"
            return
        }

        if (password.isEmpty()) { _errorMessage.value = "Vui lòng nhập mật khẩu"; return }
        if (!isValidPassword(password)) {
            _errorMessage.value = "Mật khẩu phải có ít nhất 12 ký tự, gồm chữ hoa, số và ký tự đặc biệt"
            return
        }
        if (password != confirmPassword) { _errorMessage.value = "Mật khẩu xác nhận không khớp"; return }

        _isLoading.value = true
        repository.register(fullName, email, phone, password,
            onSuccess = {
                _isLoading.value = false
                _registerResult.value = true
            },
            onFailure = { error ->
                _isLoading.value = false
                _errorMessage.value = error
            }
        )
    }

    // Reset helpers — dùng trong View thay vì set .value trực tiếp
    fun resetLoginResult() { _loginResult.value = false }
    fun resetRegisterResult() { _registerResult.value = false }
    fun resetChangePasswordResult() { _changePasswordResult.value = false }
    fun resetWrongOldPassword() { _wrongOldPassword.value = false }
    fun resetErrorMessage() { _errorMessage.value = "" }

    fun checkLockStatus() {
        _isLoading.value = true
        repository.checkUserLockStatus(
            onResult = { isLocked: Boolean, reason: String, until: Long, lockDays: Int ->
                _isLoading.value = false
                _lockInfo.value = Pair(isLocked, Triple(reason, until, lockDays))
            },
            onFailure = { error: String ->
                _isLoading.value = false
                _errorMessage.value = error
            }
        )
    }
}
