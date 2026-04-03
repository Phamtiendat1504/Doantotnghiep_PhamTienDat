package com.example.doantotnghiep.repository

import com.example.doantotnghiep.Model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Đăng ký tài khoản
    fun register(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: ""

                val user = User(
                    uid = uid,
                    fullName = fullName,
                    email = email,
                    phone = phone,
                    role = "tenant", // Mặc định là người thuê
                    createdAt = System.currentTimeMillis()
                )

                db.collection("users").document(uid)
                    .set(user)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onFailure("Lưu thông tin thất bại: ${e.message}") }
            }
            .addOnFailureListener { e -> onFailure("Đăng ký thất bại: ${e.message}") }
    }

    // Đăng nhập
    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure("Đăng nhập thất bại: ${e.message}") }
    }

    // Tìm email theo số điện thoại
    fun findEmailByPhone(
        phone: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("users")
            .whereEqualTo("phone", phone)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val email = documents.documents[0].getString("email") ?: ""
                    onSuccess(email)
                } else {
                    onFailure("Số điện thoại chưa được đăng ký")
                }
            }
            .addOnFailureListener { e -> onFailure("Lỗi hệ thống: ${e.message}") }
    }

    // Cập nhật mật khẩu (Dùng sau khi verify OTP thành công)
    fun updatePasswordAfterOtp(
        email: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        // Lưu ý: Firebase Auth yêu cầu re-authenticate nếu đổi pass mà ko có session
        // Cách tốt nhất cho "Quên mật khẩu" là dùng sendPasswordResetEmail
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure("Gửi yêu cầu đặt lại mật khẩu thất bại: ${e.message}") }
    }
}