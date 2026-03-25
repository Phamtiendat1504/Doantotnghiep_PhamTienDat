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

                // Tạo object User để lưu vào Firestore
                val user = User(
                    uid = uid,
                    fullName = fullName,
                    email = email,
                    phone = phone
                )

                // Lưu thông tin user vào Firestore collection "users"
                db.collection("users").document(uid)
                    .set(user)
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        onFailure("Lưu thông tin thất bại: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                onFailure("Đăng ký thất bại: ${e.message}")
            }
    }

    // Đăng nhập
    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure("Đăng nhập thất bại: ${e.message}")
            }
    }
    // Tìm email theo số điện thoại từ Firestore
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
            .addOnFailureListener { e ->
                onFailure("Lỗi: ${e.message}")
            }
    }

    // Đổi mật khẩu sau khi xác thực OTP
    fun updatePasswordAfterOtp(
        email: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val user = auth.currentUser
        if (user != null) {
            user.updatePassword(newPassword)
                .addOnSuccessListener {
                    auth.signOut()
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    onFailure("Đổi mật khẩu thất bại: ${e.message}")
                }
        } else {
            onFailure("Không tìm thấy người dùng")
        }
    }
}