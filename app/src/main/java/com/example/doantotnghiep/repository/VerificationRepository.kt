package com.example.doantotnghiep.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class VerificationRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    fun loadCurrentUserInfo(onSuccess: (fullName: String, phone: String) -> Unit, onFailure: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onFailure("Chưa đăng nhập")
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    onSuccess(doc.getString("fullName") ?: "", doc.getString("phone") ?: "")
                }
            }
            .addOnFailureListener { e -> onFailure(e.message ?: "Lỗi") }
    }

    fun submitVerification(
        fullName: String, cccd: String, phone: String, address: String,
        frontUri: Uri, backUri: Uri,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return onFailure("Chưa đăng nhập")
        val storageRef = storage.reference

        val frontRef = storageRef.child("verifications/$uid/cccd_front_${System.currentTimeMillis()}.jpg")
        frontRef.putFile(frontUri).continueWithTask { task ->
            if (!task.isSuccessful) task.exception?.let { throw it }
            frontRef.downloadUrl
        }.continueWithTask { task ->
            val frontUrl = task.result.toString()
            val backRef = storageRef.child("verifications/$uid/cccd_back_${System.currentTimeMillis()}.jpg")
            backRef.putFile(backUri).continueWithTask { backTask ->
                if (!backTask.isSuccessful) backTask.exception?.let { throw it }
                backRef.downloadUrl
            }.continueWithTask { backUrlTask ->
                val backUrl = backUrlTask.result.toString()
                val data = hashMapOf(
                    "userId" to uid,
                    "fullName" to fullName,
                    "cccdNumber" to cccd,
                    "phone" to phone,
                    "address" to address,
                    "cccdFrontUrl" to frontUrl,
                    "cccdBackUrl" to backUrl,
                    "status" to "pending",
                    "createdAt" to System.currentTimeMillis()
                )
                db.collection("verifications").document(uid).set(data)
            }
        }.addOnSuccessListener { onSuccess() }
         .addOnFailureListener { e -> onFailure(e.message ?: "Đã có lỗi xảy ra, vui lòng thử lại sau.") }
    }
}