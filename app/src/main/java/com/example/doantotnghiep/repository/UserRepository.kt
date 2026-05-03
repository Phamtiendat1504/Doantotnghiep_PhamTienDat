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
        // D\u00f9ng Source.SERVER \u0111\u1ec3 \u0111\u1ea3m b\u1ea3o l\u1ea5y isVerified m\u1edbi nh\u1ea5t, tr\u00e1nh cache c\u0169
        // Dùng Source.DEFAULT (mặc định) để tìm kiếm nhanh hơn nhờ Cache của Firestore.
        // Tránh tình trạng đơ ứng dụng khi phải tải toàn bộ danh sách users liên tục từ Server.
        db.collection("users")
            .get()
            .addOnSuccessListener { docs ->
                // QUAN TRỌNG: Không dùng toObject() — isVerified (Kotlin Boolean tiền tố "is")
                // bị Firestore SDK map nhầm sang field "verified" thay vì "isVerified" → luôn false.
                val users = docs.mapNotNull { doc ->
                    val fullName = doc.getString("fullName") ?: return@mapNotNull null
                    if (!fullName.contains(query, ignoreCase = true)) return@mapNotNull null
                    User(
                        uid              = doc.id,
                        fullName         = fullName,
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
                }.take(30)

                onSuccess(users)
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
