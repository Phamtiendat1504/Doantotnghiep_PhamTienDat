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
}