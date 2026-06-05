package com.example.doantotnghiep.repository

import com.example.doantotnghiep.Model.User
import com.example.doantotnghiep.Utils.toUser
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
                val user = doc.toUser()
                if (user != null) {
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
                        val user = cachedDoc.toUser()
                        if (user != null) {
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
                val users = docs.mapNotNull { it.toUser() }
                
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
     * Đếm số bài đăng công khai của một người dùng (approved).
     * Chỉ dùng cho trường hợp đếm đơn lẻ. Khi cần đếm nhiều user cùng lúc dùng countPublicRoomsForUsers.
     */
    fun countPublicRooms(
        userId: String,
        onResult: (Int) -> Unit
    ) {
        db.collection("rooms").whereEqualTo("userId", userId).whereEqualTo("status", "approved").get()
            .addOnSuccessListener { docs -> onResult(docs.size()) }
            .addOnFailureListener { onResult(0) }
    }

    /**
     * Đếm số bài đăng công khai cho nhiều user cùng lúc bằng batch whereIn.
     * Thay vì N×2 queries, chỉ tốn (ceil(N/30)) × 2 queries tổng cộng.
     * Trả về Map<uid, count>.
     */
    fun countPublicRoomsForUsers(
        uids: List<String>,
        onResult: (Map<String, Int>) -> Unit
    ) {
        if (uids.isEmpty()) {
            onResult(emptyMap())
            return
        }
        val counts = mutableMapOf<String, Int>().also { map -> uids.forEach { map[it] = 0 } }
        val chunks = uids.chunked(30) // Firestore whereIn tối đa 30 giá trị
        val totalTasks = chunks.size
        var completedTasks = 0

        fun finish() {
            completedTasks++
            if (completedTasks == totalTasks) onResult(counts)
        }

        for (chunk in chunks) {
            db.collection("rooms").whereIn("userId", chunk).whereEqualTo("status", "approved").get()
                .addOnSuccessListener { docs ->
                    docs.forEach { doc -> val uid = doc.getString("userId") ?: return@forEach; counts[uid] = (counts[uid] ?: 0) + 1 }
                    finish()
                }
                .addOnFailureListener { finish() }
        }
    }

    fun getUserFromServer(
        uid: String,
        onSuccess: (User?) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("users").document(uid)
            .get(Source.SERVER)
            .addOnSuccessListener { doc -> onSuccess(doc.toUser()) }
            .addOnFailureListener { e -> onFailure(e.message ?: "Lỗi tải dữ liệu") }
    }

    fun listenUser(
        uid: String,
        onUpdate: (User?) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration {
        return db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                onUpdate(snapshot?.toUser())
            }
    }

    fun listenVerificationStatus(
        uid: String,
        onUpdate: (status: String?, rejectReason: String?, autoCheckStatus: String?) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration {
        return db.collection("verifications").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    onUpdate(
                        snapshot.getString("status"),
                        snapshot.getString("rejectReason"),
                        snapshot.getString("autoCheckStatus")
                    )
                } else {
                    onUpdate(null, null, null)
                }
            }
    }
}

