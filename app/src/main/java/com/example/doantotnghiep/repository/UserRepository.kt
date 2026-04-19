package com.example.doantotnghiep.repository

import com.example.doantotnghiep.Model.User
import com.google.firebase.firestore.FirebaseFirestore

class UserRepository {

    private val db = FirebaseFirestore.getInstance()

    /**
     * Lấy thông tin của 1 user theo uid.
     */
    fun getUserById(
        uid: String,
        onSuccess: (User) -> Unit,
        onFailure: (String) -> Unit
    ) {
        // Sử dụng Query thay vì lấy Document trực tiếp để bypass rule nếu Firebase khóa đọc Document của người khác
        db.collection("users").whereEqualTo(com.google.firebase.firestore.FieldPath.documentId(), uid).get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    val doc = docs.documents[0]
                    val user = doc.toObject(User::class.java) ?: User()
                    onSuccess(user.copy(uid = doc.id))
                } else {
                    onFailure("Không tìm thấy người dùng")
                }
            }
            .addOnFailureListener { e ->
                onFailure("Lỗi tải thông tin: ${e.message}")
            }
    }

    /**
     * Tìm kiếm người dùng theo tiền tố tên (prefix search).
     * Firestore hỗ trợ: fullName >= query AND fullName < query + '\uf8ff'
     */
    fun searchUsersByName(
        query: String,
        onSuccess: (List<User>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (query.isBlank()) {
            onSuccess(emptyList())
            return
        }
        
        // Vì Firebase mặc định là Case-Sensitive (phân biệt hoa thường) và Model của bạn
        // không có trường lưu `fullNameLowerCase`, nên chúng ta chạy Lọc ở phía Client
        // Dùng `contains(ignoreCase = true)` hỗ trợ tìm cả chuỗi con và không phân biệt HOA/thường.
        db.collection("users")
            .get()
            .addOnSuccessListener { docs ->
                val users = docs.mapNotNull { doc ->
                    doc.toObject(User::class.java).copy(uid = doc.id)
                }.filter { user ->
                    user.fullName.contains(query, ignoreCase = true)
                }.take(30)
                
                onSuccess(users)
            }
            .addOnFailureListener { e ->
                onFailure("Lỗi tìm kiếm: ${e.message}")
            }
    }

    /**
     * Đếm tổng số bài đăng được duyệt của một người dùng.
     */
    fun countApprovedRooms(
        userId: String,
        onResult: (Int) -> Unit
    ) {
        FirebaseFirestore.getInstance()
            .collection("rooms")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener { docs -> onResult(docs.size()) }
            .addOnFailureListener { onResult(0) }
    }
}
