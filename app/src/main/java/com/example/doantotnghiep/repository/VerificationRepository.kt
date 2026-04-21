package com.example.doantotnghiep.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class VerificationRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    fun loadCurrentUserInfo(
        onSuccess: (fullName: String, phone: String, email: String, address: String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return onFailure("Chưa đăng nhập")
        // Dùng Source.SERVER để lấy email/address mới nhất từ profile người dùng
        db.collection("users").document(uid)
            .get(com.google.firebase.firestore.Source.SERVER)
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    onSuccess(
                        doc.getString("fullName") ?: "",
                        doc.getString("phone") ?: "",
                        doc.getString("email") ?: (auth.currentUser?.email ?: ""),
                        doc.getString("address") ?: ""
                    )
                }
            }
            .addOnFailureListener { e -> onFailure(e.message ?: "Lỗi") }
    }


    /**
     * Kiểm tra số CCCD đã tồn tại trong hệ thống chưa (tức là người khác đã dùng số này).
     * Mỗi người chỉ có 1 CCCD duy nhất nên không cho phép trùng.
     * [currentUid] là uid hiện tại để bỏ qua chính mình (trường hợp nộp lại).
     */
    fun checkCccdExists(
        cccdNumber: String,
        currentUid: String,
        onExists: () -> Unit,
        onNotExists: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("verifications")
            .whereEqualTo("cccdNumber", cccdNumber)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    // Có kết quả — kiểm tra có phải chính mình không
                    val docUid = snapshot.documents[0].id
                    if (docUid == currentUid) {
                        // Chính mình nộp lại → cho phép
                        onNotExists()
                    } else {
                        // Người khác đã dùng CCCD này
                        onExists()
                    }
                } else {
                    onNotExists()
                }
            }
            .addOnFailureListener { e -> onFailure(e.message ?: "Lỗi kiểm tra CCCD") }
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
                db.collection("verifications").document(uid).set(data).addOnSuccessListener {
                    // CẬP NHẬT QUAN TRỌNG: Đổi role sang "pending" ngay lập tức để UI hiển thị trạng thái đang phê duyệt
                    db.collection("users").document(uid).update("role", "pending")

                    // Thêm thông báo cho Admin
                    val notif = hashMapOf(
                        "userId" to "admin_system", // ID giả định để Admin nhận diện
                        "title" to "Yêu cầu xác minh mới",
                        "message" to "Người dùng $fullName vừa gửi yêu cầu xác minh chủ trọ.",
                        "type" to "new_verification",
                        "isRead" to false,
                        "createdAt" to System.currentTimeMillis(),
                        "targetId" to uid
                    )
                    db.collection("notifications").add(notif)
                }
            }
        }.addOnSuccessListener { onSuccess() }
         .addOnFailureListener { e -> onFailure(e.message ?: "Đã có lỗi xảy ra, vui lòng thử lại sau.") }
    }
}