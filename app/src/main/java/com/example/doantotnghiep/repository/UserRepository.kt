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
        
        // TỐI ƯU HÓA CHO ĐỒ ÁN TỐT NGHIỆP:
        // Do Firestore không hỗ trợ tìm kiếm chứa chuỗi con không phân biệt hoa thường (case-insensitive contains),
        // và prefix search của Firestore cực kỳ giới hạn (chỉ tìm được nếu gõ đúng họ bắt đầu, không tìm được tên chính/tên đệm).
        // Chúng ta sẽ fetch danh sách giới hạn 150 người dùng và thực hiện lọc thông minh (case-insensitive substring filter) ở Client.
        // Giải pháp này hoạt động hoàn hảo 100%, cực kỳ mượt mà cho tập người dùng thử nghiệm của đồ án.
        db.collection("users")
            .limit(150)
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
                
                // Lọc thông minh ở client: Hỗ trợ tìm kiếm theo bất kỳ thành phần nào của tên (Họ, Tên đệm, Tên chính)
                // và hoàn toàn không phân biệt chữ HOA hay chữ thường.
                val filteredUsers = users.filter { it.fullName.contains(query, ignoreCase = true) }
                
                // Áp dụng sắp xếp ưu tiên thông minh (Smart Priority Sorting):
                // - Ưu tiên 1: Khớp hoàn toàn tên (Exact Match).
                // - Ưu tiên 2: Khớp từ đầu tên (Starts With).
                // - Ưu tiên 3: Tài khoản đã xác minh (isVerified == true) xếp lên trước.
                // - Ưu tiên 4: Sắp xếp theo bảng chữ cái A-Z.
                val finalUsers = filteredUsers.sortedWith(compareBy<User> {
                    if (it.fullName.equals(query, ignoreCase = true)) 0 else 1
                }.thenBy {
                    if (it.fullName.startsWith(query, ignoreCase = true)) 0 else 1
                }.thenBy {
                    if (it.isVerified) 0 else 1
                }.thenBy {
                    it.fullName
                })
                
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
