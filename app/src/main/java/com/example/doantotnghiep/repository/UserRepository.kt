package com.example.doantotnghiep.repository

import com.example.doantotnghiep.Model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

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
        // QUAN TRỌNG: Không dùng toObject() cho Boolean field có tiền tố "is" (isVerified, isLocked...)
        // Firestore SDK biên dịch Kotlin "isVerified" → Java getter "isVerified()" → nhận diện là field "verified"
        // → toObject() luôn trả false dù Firestore lưu true. Phải đọc trực tiếp qua getBoolean("isVerified").
        db.collection("users").document(uid)
            .get(Source.SERVER)
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val user = User(
                        uid            = doc.id,
                        fullName       = doc.getString("fullName") ?: "",
                        email          = doc.getString("email") ?: "",
                        phone          = doc.getString("phone") ?: "",
                        address        = doc.getString("address") ?: "",
                        birthday       = doc.getString("birthday") ?: "",
                        gender         = doc.getString("gender") ?: "",
                        occupation     = doc.getString("occupation") ?: "",
                        avatarUrl      = doc.getString("avatarUrl") ?: "",
                        role           = doc.getString("role") ?: "user",
                        isVerified     = doc.getBoolean("isVerified") ?: false,
                        hasAcceptedRules = doc.getBoolean("hasAcceptedRules") ?: false,
                        isLocked       = doc.getBoolean("isLocked") ?: false,
                        lockReason     = doc.getString("lockReason") ?: "",
                        lockUntil      = doc.getLong("lockUntil") ?: 0L,
                        postingUnlockAt = doc.getLong("postingUnlockAt") ?: 0L,
                        verifiedAt     = doc.getLong("verifiedAt") ?: 0L,
                        createdAt      = doc.getLong("createdAt") ?: 0L,
                        purchasedSlots = doc.getLong("purchasedSlots")?.toInt() ?: 0
                    )
                    onSuccess(user)
                } else {
                    onFailure("Không tìm thấy người dùng")
                }
            }
            .addOnFailureListener {
                // Fallback về cache nếu mất mạng
                db.collection("users").document(uid)
                    .get(Source.CACHE)
                    .addOnSuccessListener { cachedDoc ->
                        if (cachedDoc.exists()) {
                            val user = User(
                                uid            = cachedDoc.id,
                                fullName       = cachedDoc.getString("fullName") ?: "",
                                email          = cachedDoc.getString("email") ?: "",
                                phone          = cachedDoc.getString("phone") ?: "",
                                address        = cachedDoc.getString("address") ?: "",
                                birthday       = cachedDoc.getString("birthday") ?: "",
                                gender         = cachedDoc.getString("gender") ?: "",
                                occupation     = cachedDoc.getString("occupation") ?: "",
                                avatarUrl      = cachedDoc.getString("avatarUrl") ?: "",
                                role           = cachedDoc.getString("role") ?: "user",
                                isVerified     = cachedDoc.getBoolean("isVerified") ?: false,
                                hasAcceptedRules = cachedDoc.getBoolean("hasAcceptedRules") ?: false,
                                isLocked       = cachedDoc.getBoolean("isLocked") ?: false,
                                lockReason     = cachedDoc.getString("lockReason") ?: "",
                                lockUntil      = cachedDoc.getLong("lockUntil") ?: 0L,
                                postingUnlockAt = cachedDoc.getLong("postingUnlockAt") ?: 0L,
                                verifiedAt     = cachedDoc.getLong("verifiedAt") ?: 0L,
                                createdAt      = cachedDoc.getLong("createdAt") ?: 0L,
                                purchasedSlots = cachedDoc.getLong("purchasedSlots")?.toInt() ?: 0
                            )
                            onSuccess(user)
                        } else {
                            onFailure("Không tìm thấy người dùng (Cache)")
                        }
                    }
                    .addOnFailureListener { e ->
                        onFailure("Lỗi tải thông tin: ${e.message}")
                    }
            }
    }

    /**
     * TỐI ƯU HÓA: Tìm kiếm người dùng bằng Prefix Search (Server-side)
     * Tránh tình trạng tải toàn bộ database về điện thoại gây Crash và tốn chi phí.
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
        
        // Trick để Query chính xác: Capitalize (Viết hoa) chữ cái đầu của query
        // Ví dụ: người dùng gõ "phạm", ta chuyển thành "Phạm" để khớp với Data lưu trên Firebase.
        val formattedQuery = query.split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

        db.collection("users")
            .orderBy("fullName")
            .startAt(formattedQuery)
            .endAt(formattedQuery + "\uf8ff")
            .limit(30)
            .get()
            .addOnSuccessListener { docs ->
                val users = docs.mapNotNull { doc ->
                    User(
                        uid              = doc.id,
                        fullName         = doc.getString("fullName") ?: "",
                        email            = doc.getString("email") ?: "",
                        phone            = doc.getString("phone") ?: "",
                        address          = doc.getString("address") ?: "",
                        birthday         = doc.getString("birthday") ?: "",
                        gender           = doc.getString("gender") ?: "",
                        occupation       = doc.getString("occupation") ?: "",
                        avatarUrl        = doc.getString("avatarUrl") ?: "",
                        role             = doc.getString("role") ?: "user",
                        isVerified       = doc.getBoolean("isVerified") ?: false,
                        hasAcceptedRules = doc.getBoolean("hasAcceptedRules") ?: false,
                        isLocked         = doc.getBoolean("isLocked") ?: false,
                        lockReason       = doc.getString("lockReason") ?: "",
                        lockUntil        = doc.getLong("lockUntil") ?: 0L,
                        postingUnlockAt  = doc.getLong("postingUnlockAt") ?: 0L,
                        verifiedAt       = doc.getLong("verifiedAt") ?: 0L,
                        createdAt        = doc.getLong("createdAt") ?: 0L,
                        purchasedSlots   = doc.getLong("purchasedSlots")?.toInt() ?: 0
                    )
                }
                
                // Vì prefix search có thể dính các kết quả không chính xác 100% nếu người dùng gõ chuỗi con,
                // ta filter nhẹ lại một lần nữa ở Client cho chắc chắn (bây giờ dữ liệu trả về chỉ max 30 cái)
                val finalUsers = users.filter { it.fullName.contains(query, ignoreCase = true) }
                
                onSuccess(finalUsers)
            }
            .addOnFailureListener { e ->
                onFailure("Lỗi tìm kiếm: ${e.message}")
            }
    }

    /**
     * Đếm số bài đăng công khai của một người dùng (đúng theo quyền người khác được xem hồ sơ):
     * approved + expired.
     */
    fun countPublicRooms(
        userId: String,
        onResult: (Int) -> Unit
    ) {
        val firestore = FirebaseFirestore.getInstance()
        var approvedCount = 0
        var expiredCount = 0
        var completed = 0

        fun finishIfDone() {
            completed++
            if (completed == 2) {
                onResult(approvedCount + expiredCount)
            }
        }

        firestore.collection("rooms")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener { docs ->
                approvedCount = docs.size()
                finishIfDone()
            }
            .addOnFailureListener {
                finishIfDone()
            }

        firestore.collection("rooms")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "expired")
            .get()
            .addOnSuccessListener { docs ->
                expiredCount = docs.size()
                finishIfDone()
            }
            .addOnFailureListener {
                finishIfDone()
            }
    }
}
