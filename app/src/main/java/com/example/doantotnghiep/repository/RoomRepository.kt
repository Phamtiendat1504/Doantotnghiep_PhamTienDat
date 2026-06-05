package com.example.doantotnghiep.repository

import android.net.Uri
import com.example.doantotnghiep.Model.Room
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageException
import java.util.Date
import java.util.Locale
import java.util.UUID

class RoomRepository {

    companion object {
        private const val FREE_POSTS_PER_DAY = 3
    }

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance("gs://doantotnghiep-b39ae.firebasestorage.app")
    private val auth = FirebaseAuth.getInstance()
    private val imageMetadata = StorageMetadata.Builder()
        .setContentType("image/jpeg")
        .build()

    private fun mapStorageUploadError(error: Exception?): String {
        val ex = error as? StorageException
        return when (ex?.errorCode) {
            StorageException.ERROR_NOT_AUTHENTICATED ->
                "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
            StorageException.ERROR_NOT_AUTHORIZED ->
                "Tài khoản chưa đủ quyền tải ảnh. Hãy kiểm tra trạng thái xác minh rồi thử lại."
            StorageException.ERROR_QUOTA_EXCEEDED ->
                "Bộ nhớ Firebase tạm thời quá tải. Vui lòng thử lại sau."
            else -> "Tải ảnh thất bại: ${error?.message ?: "Lỗi không xác định"}"
        }
    }

    private fun mapFirestorePostError(error: Exception?): String {
        val ex = error as? FirebaseFirestoreException
        return when (ex?.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Tài khoản chưa đủ điều kiện đăng bài theo chính sách hiện tại."
            FirebaseFirestoreException.Code.ABORTED ->
                ex.message ?: "Thao tác không thể hoàn thành. Vui lòng thử lại."
            else -> "Lưu bài đăng thất bại: ${error?.message ?: "Lỗi không xác định"}"
        }
    }

    fun checkDailyPostQuota(
        uid: String,
        limitPer24h: Int = FREE_POSTS_PER_DAY,
        onAllowed: (remainingToday: Int, usePurchasedSlot: Boolean) -> Unit,
        onBlocked: (unlockAt: Long) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("users").document(uid)
            .get(Source.SERVER)
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    onAllowed(limitPer24h, false)
                    return@addOnSuccessListener
                }
                
                val today = getTodayGmt7String()
                val storedDate = doc.getString("dailyPostCountDate") ?: ""
                val storedCount = if (storedDate == today) {
                    (doc.getLong("dailyPostCount") ?: 0L).toInt()
                } else {
                    0
                }
                val purchased = (doc.getLong("purchasedSlots") ?: 0L).toInt()

                // ƯU TIÊN 1: Sử dụng 3 lượt miễn phí mỗi ngày trước
                if (storedCount < limitPer24h) {
                    onAllowed((limitPer24h - storedCount).coerceAtLeast(0), false)
                    return@addOnSuccessListener
                }

                // ƯU TIÊN 2: Khi đã dùng hết lượt miễn phí trong ngày, mới bắt đầu trừ vào gói mua thêm
                if (purchased > 0) {
                    onAllowed(purchased, true)
                    return@addOnSuccessListener
                }

                // Hết lượt: thời gian mở lại tính từ 00:00:00 ngày hôm sau
                val calendar = java.util.Calendar.getInstance()
                calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                val unlockAt = calendar.timeInMillis
                
                onBlocked(unlockAt)
            }
            .addOnFailureListener { e ->
                onFailure("Không thể kiểm tra số lượt đăng bài: ${e.message ?: "Lỗi không xác định"}")
            }
    }

    fun postRoom(
        room: Room,
        imageUris: List<Uri>,
        usePurchasedSlot: Boolean = false,
        onSuccess: (String, String?) -> Unit,
        onFailure: (String) -> Unit,
        onProgress: (Int) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return onFailure("Chưa đăng nhập. Vui lòng đăng nhập lại.")
        
        // Giới hạn số lượng ảnh tối đa là 10
        if (imageUris.size > 10) {
            onFailure("Chỉ được tải lên tối đa 10 ảnh phòng trọ.")
            return
        }

        val docRef = db.collection("rooms").document()
        val roomId = docRef.id

        if (imageUris.isEmpty()) {
            val roomData = room.copy(id = roomId, userId = uid, imageUrls = emptyList())
            if (usePurchasedSlot) {
                saveRoomWithPurchasedSlot(docRef, roomData, uid, {
                    onSuccess(roomId, null)
                }, onFailure)
            } else {
                saveRoomToFirestore(docRef, roomData, usePurchasedSlot, {
                    onSuccess(roomId, null)
                }, onFailure)
            }
        } else {
            // Tạo room trước để upload ảnh chạy theo nhánh quyền isRoomOwner(roomId) trong Storage rules.
            // Cách này ổn định hơn so với upload-before-room-doc (phụ thuộc isVerifiedUser tại đúng thời điểm upload).
            val roomSeedData = room.copy(id = roomId, userId = uid, imageUrls = emptyList())
            saveRoomToFirestore(
                docRef = docRef,
                room = roomSeedData,
                usedPurchasedSlot = usePurchasedSlot,
                onSuccess = {
                    docRef.get(Source.SERVER)
                        .addOnSuccessListener {
                            uploadImages(
                                roomId = roomId,
                                uris = imageUris,
                                onProgress = onProgress,
                                onComplete = { urls ->
                                    docRef.update(
                                        mapOf(
                                            "imageUrls" to urls,
                                            "updatedAt" to System.currentTimeMillis()
                                        )
                                    ).addOnSuccessListener {
                                        onSuccess(roomId, urls.firstOrNull())
                                    }.addOnFailureListener { e ->
                                        onFailure(mapFirestorePostError(e))
                                    }
                                },
                                onError = { error ->
                                    // Dọn dẹp ảnh mồ côi khỏi Storage
                                    val folderRef = storage.reference.child("rooms").child(roomId)
                                    folderRef.listAll()
                                        .addOnSuccessListener { listResult ->
                                            listResult.items.forEach { item ->
                                                item.delete().addOnFailureListener { e ->
                                                    android.util.Log.e("RoomRepository", "Lỗi xóa ảnh mồ côi: ${e.message}")
                                                }
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            android.util.Log.e("RoomRepository", "Lỗi listAll khi dọn ảnh mồ côi: ${e.message}")
                                        }

                                    docRef.delete().addOnCompleteListener {
                                        rollbackPostQuota(uid, usePurchasedSlot)
                                        onFailure(error)
                                    }
                                }
                            )
                        }
                        .addOnFailureListener { e ->
                            docRef.delete().addOnCompleteListener {
                                rollbackPostQuota(uid, usePurchasedSlot)
                                onFailure(mapFirestorePostError(e))
                            }
                        }
                },
                onFailure = onFailure
            )
        }
    }

    private fun saveRoomWithPurchasedSlot(
        docRef: com.google.firebase.firestore.DocumentReference,
        room: Room,
        uid: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val userRef = db.collection("users").document(uid)
        db.runTransaction { tx ->
            val snap = tx.get(userRef)
            val currentSlots = (snap.getLong("purchasedSlots") ?: 0L).toInt()
            if (currentSlots <= 0) {
                throw FirebaseFirestoreException(
                    "Bạn đã dùng hết lượt đăng bài đã mua.",
                    FirebaseFirestoreException.Code.ABORTED
                )
            }
            tx.set(docRef, room)
            tx.update(
                userRef,
                mapOf(
                    "purchasedSlots" to currentSlots - 1,
                    "lastSlotConsumedAt" to System.currentTimeMillis()
                )
            )
        }.addOnSuccessListener {
            docRef.update("usedPurchasedSlot", true)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e -> onFailure(mapFirestorePostError(e)) }
        }.addOnFailureListener { e ->
            onFailure(e.message ?: "Không thể đăng bài bằng lượt đã mua. Vui lòng thử lại.")
        }
    }

    private fun saveRoomToFirestore(
        docRef: com.google.firebase.firestore.DocumentReference,
        room: Room,
        usedPurchasedSlot: Boolean = false,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val today = getTodayGmt7String()
        val userRef = db.collection("users").document(room.userId)

        db.runTransaction { tx ->
            val userSnap = tx.get(userRef)
            
            if (usedPurchasedSlot) {
                val currentSlots = (userSnap.getLong("purchasedSlots") ?: 0L).toInt()
                if (currentSlots <= 0) {
                    throw FirebaseFirestoreException(
                        "Bạn đã dùng hết lượt đăng bài đã mua.",
                        FirebaseFirestoreException.Code.ABORTED
                    )
                }
                tx.set(docRef, room)
                tx.update(userRef, mapOf(
                    "purchasedSlots" to currentSlots - 1,
                    "lastSlotConsumedAt" to System.currentTimeMillis()
                ))
            } else {
                val storedDate = userSnap.getString("dailyPostCountDate") ?: ""
                val storedCount = if (storedDate == today) {
                    (userSnap.getLong("dailyPostCount") ?: 0L).toInt()
                } else {
                    0
                }
                if (storedCount >= FREE_POSTS_PER_DAY) {
                    throw FirebaseFirestoreException(
                        "Bạn đã đăng đủ $FREE_POSTS_PER_DAY bài miễn phí hôm nay.",
                        FirebaseFirestoreException.Code.ABORTED
                    )
                }
                tx.set(docRef, room)
                tx.update(userRef, mapOf(
                    "dailyPostCountDate" to today,
                    "dailyPostCount" to storedCount + 1
                ))
            }
        }.addOnSuccessListener {
            docRef.update("usedPurchasedSlot", usedPurchasedSlot)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e -> onFailure(mapFirestorePostError(e)) }
        }.addOnFailureListener { e -> onFailure(mapFirestorePostError(e)) }
    }

    // Lấy chi tiết một phòng theo ID
    fun getRoomById(
        roomId: String,
        onSuccess: (Map<String, Any>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("rooms").document(roomId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) onSuccess(doc.data ?: emptyMap())
                else onFailure("Không tìm thấy bài viết")
            }
            .addOnFailureListener { onFailure("Không thể tải thông tin bài viết, vui lòng thử lại") }
    }

    // Lấy nhiều bài đăng đã được duyệt theo danh sách ID (batch 30, Firestore whereIn limit)
    fun getApprovedRoomsByIds(
        roomIds: List<String>,
        onSuccess: (List<com.google.firebase.firestore.DocumentSnapshot>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (roomIds.isEmpty()) { onSuccess(emptyList()); return }

        val batches   = roomIds.chunked(30)
        val allDocs   = mutableListOf<com.google.firebase.firestore.DocumentSnapshot>()
        var completed = 0
        var hasError  = false

        for (batch in batches) {
            db.collection("rooms")
                .whereEqualTo("status", "approved")
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), batch)
                .get()
                .addOnSuccessListener { snap ->
                    if (hasError) return@addOnSuccessListener
                    synchronized(allDocs) { allDocs.addAll(snap.documents.filter { it.exists() }) }
                    completed++
                    if (completed == batches.size) onSuccess(allDocs)
                }
                .addOnFailureListener { e ->
                    if (!hasError) { hasError = true; onFailure("Lỗi tải dữ liệu: ${e.message}") }
                }
        }
    }

    // Lấy một bài đăng đã được duyệt theo ID (dùng cho tìm kiếm chính xác)
    fun getApprovedRoomById(
        roomId: String,
        onSuccess: (com.google.firebase.firestore.DocumentSnapshot) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("rooms").document(roomId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists() && doc.getString("status") == "approved") onSuccess(doc)
                else onFailure("Không tìm thấy bài đăng")
            }
            .addOnFailureListener { e -> onFailure("Lỗi tải dữ liệu: ${e.message}") }
    }

    // Xóa bài viết đã lưu
    fun deleteSavedPost(
        savedDocId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("savedPosts").document(savedDocId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e.message ?: "Vui lòng thử lại") }
    }

    private fun uploadImages(
        roomId: String,
        uris: List<Uri>,
        onProgress: (Int) -> Unit,
        onComplete: (List<String>) -> Unit,
        onError: (String) -> Unit
    ) {
        val totalImages = uris.size
        if (totalImages == 0) {
            onComplete(emptyList())
            return
        }

        val uploadedUrls = arrayOfNulls<String>(totalImages)
        val progressMap = mutableMapOf<Int, Double>() // Lưu % của từng ảnh (0.0 -> 1.0)
        var successCount = 0
        var hasError = false

        uris.forEachIndexed { index, uri ->
            val fileName = "img_${UUID.randomUUID()}.jpg"
            val ref = storage.reference.child("rooms").child(roomId).child(fileName)
            
            val uploadTask = ref.putFile(uri, imageMetadata)
            
            // Theo dõi tiến độ từng byte để thanh progress chạy mượt
            uploadTask.addOnProgressListener { taskSnapshot ->
                if (hasError) return@addOnProgressListener
                if (taskSnapshot.totalByteCount > 0) {
                    val progress = taskSnapshot.bytesTransferred.toDouble() / taskSnapshot.totalByteCount
                    progressMap[index] = progress
                    
                    // BUG FIX #7: Handle division by zero
                    val avgProgress = if (totalImages > 0) {
                        progressMap.values.sum() / totalImages
                    } else 0.0
                    onProgress((avgProgress * 100).toInt())
                }
            }

            uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                ref.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                if (hasError) return@addOnSuccessListener
                
                uploadedUrls[index] = downloadUri.toString()
                successCount++
                
                if (successCount == totalImages) {
                    onComplete(uploadedUrls.filterNotNull())
                }
            }
            .addOnFailureListener { e ->
                if (!hasError) {
                    hasError = true
                    onError(mapStorageUploadError(e))
                }
            }
        }
    }

    // Lấy danh sách bài đăng của user hiện tại (filter theo status)
    fun getMyPosts(
        filter: String,
        onSuccess: (List<com.google.firebase.firestore.DocumentSnapshot>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return onFailure("Chưa đăng nhập")
        val baseQuery = db.collection("rooms").whereEqualTo("userId", uid)

        // Không dùng orderBy ở đây để tránh bắt buộc phải tạo Composite Index
        val query = if (filter != "all") {
            baseQuery.whereEqualTo("status", filter)
        } else {
            baseQuery.whereIn("status", listOf("pending", "approved", "rejected", "rented"))
        }

        query.get()
            .addOnSuccessListener { docs ->
                // Sắp xếp phía client theo createdAt giảm dần
                val sorted = docs.toList().sortedByDescending { it.getCreatedAtMs() }
                onSuccess(sorted)
            }
            .addOnFailureListener { e -> onFailure(e.message ?: "Lỗi tải dữ liệu") }
    }

    /**
     * Lắng nghe số bài đăng có cập nhật chưa đọc (approved/rejected).
     * Đây là Badge thông báo cho Chủ trọ, dùng cờ cloud `hasUnreadUpdate` thay vì SharedPreferences.
     */
    fun listenPostBadge(uid: String, onResult: (Int) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        return db.collection("rooms")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snap, _ ->
                // Lọc phía client để tránh Composite Index
                val count = snap?.count { it.getBoolean("hasUnreadUpdate") == true } ?: 0
                onResult(count)
            }
    }

    /**
     * Đánh dấu đã đọc tất cả bài đăng: xóa cờ `hasUnreadUpdate` trên Firebase.
     * Gọi khi Chủ trọ mở màn hình "Bài đăng của tôi".
     */
    fun markPostsAsRead(uid: String) {
        db.collection("rooms")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { docs ->
                // Lọc phía client để tránh Composite Index
                val toUpdate = docs.filter { it.getBoolean("hasUnreadUpdate") == true }
                if (toUpdate.isEmpty()) return@addOnSuccessListener
                val batch = db.batch()
                toUpdate.forEach { batch.update(it.reference, "hasUnreadUpdate", false) }
                batch.commit()
            }
    }

    // Kiểm tra trạng thái xác minh của user trong collection verifications
    fun getVerificationStatus(uid: String, onSuccess: (String?, String?) -> Unit, onFailure: (String) -> Unit) {
        db.collection("verifications").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    onSuccess(doc.getString("status"), doc.getString("rejectReason"))
                } else {
                    onSuccess(null, null)
                }
            }
            .addOnFailureListener { onSuccess(null, null) }
    }

    // --- SavedPostsViewModel ---

    fun loadSavedPosts(uid: String, onSuccess: (List<com.google.firebase.firestore.QueryDocumentSnapshot>) -> Unit, onFailure: (String) -> Unit) {
        db.collection("savedPosts").whereEqualTo("userId", uid).get()
            .addOnSuccessListener { docs ->
                val savedDocs = docs.toList()
                if (savedDocs.isEmpty()) {
                    onSuccess(emptyList())
                    return@addOnSuccessListener
                }

                val roomIds = savedDocs.mapNotNull { doc ->
                    doc.getString("roomId")?.takeIf { it.isNotEmpty() }
                }
                if (roomIds.isEmpty()) {
                    onSuccess(emptyList())
                    return@addOnSuccessListener
                }

                val batches = roomIds.chunked(30)
                val existingRoomIds = mutableSetOf<String>()
                var completedBatches = 0
                var hasError = false

                for (batch in batches) {
                    db.collection("rooms")
                        .whereIn(com.google.firebase.firestore.FieldPath.documentId(), batch)
                        .get()
                        .addOnSuccessListener { snap ->
                            if (hasError) return@addOnSuccessListener
                            snap.documents.filter { it.exists() }.forEach { existingRoomIds.add(it.id) }
                            completedBatches++
                            if (completedBatches == batches.size) {
                                val activeSavedDocs = mutableListOf<com.google.firebase.firestore.QueryDocumentSnapshot>()
                                val deleteBatch = db.batch()
                                var hasOrphans = false

                                for (doc in savedDocs) {
                                    val rId = doc.getString("roomId") ?: ""
                                    if (existingRoomIds.contains(rId) || rId.isEmpty()) {
                                        activeSavedDocs.add(doc)
                                    } else {
                                        deleteBatch.delete(doc.reference)
                                        hasOrphans = true
                                    }
                                }

                                if (hasOrphans) {
                                    deleteBatch.commit().addOnFailureListener { e ->
                                        android.util.Log.e("RoomRepository", "Error deleting orphan saved posts: ${e.message}")
                                    }
                                }

                                onSuccess(activeSavedDocs)
                            }
                        }
                        .addOnFailureListener { e ->
                            if (!hasError) {
                                hasError = true
                                onFailure("Lỗi tải dữ liệu: ${e.message}")
                            }
                        }
                }
            }
            .addOnFailureListener { e -> onFailure(e.message ?: "Lỗi tải dữ liệu") }
    }

    fun checkRoomExists(roomId: String, onResult: (Boolean) -> Unit, onFailure: (String) -> Unit) {
        db.collection("rooms").document(roomId).get()
            .addOnSuccessListener { doc -> onResult(doc.exists()) }
            .addOnFailureListener { onFailure("Không thể kiểm tra thông tin phòng, vui lòng thử lại.") }
    }

    fun deleteSavedPostById(savedDocId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        db.collection("savedPosts").document(savedDocId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e.message ?: "Lỗi xóa bài đã lưu") }
    }

    // --- SearchViewModel ---

    fun searchApprovedRooms(onSuccess: (List<com.google.firebase.firestore.QueryDocumentSnapshot>) -> Unit, onFailure: (String) -> Unit) {
        db.collection("rooms").whereEqualTo("status", "approved").get()
            .addOnSuccessListener { docs -> onSuccess(docs.toList()) }
            .addOnFailureListener { e -> onFailure("Lỗi tìm kiếm: ${e.message}") }
    }

    // Tối ưu hóa: Lọc Quận/Phường trực tiếp trên server thay vì tải hết toàn bộ database về
    fun searchRoomsWithBasicFilters(
        district: String,
        ward: String,
        onSuccess: (List<com.google.firebase.firestore.QueryDocumentSnapshot>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        var query = db.collection("rooms").whereEqualTo("status", "approved")
        
        if (district.isNotBlank() && !district.contains("Chọn", ignoreCase = true)) {
            query = query.whereEqualTo("district", district)
        }
        if (ward.isNotBlank() && !ward.contains("Chọn", ignoreCase = true)) {
            query = query.whereEqualTo("ward", ward)
        }

        query.get()
            .addOnSuccessListener { docs -> onSuccess(docs.toList()) }
            .addOnFailureListener { e -> onFailure("Lỗi tìm kiếm khu vực: ${e.message}") }
    }

    // Tối ưu hóa: Tìm kiếm bằng Bounding Box theo Latitude để giảm số lượt tải dữ liệu từ Firestore
    fun searchNearbyRooms(
        lat: Double,
        radiusKm: Double,
        onSuccess: (List<com.google.firebase.firestore.QueryDocumentSnapshot>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        // 1 độ vĩ tuyến tương đương khoảng 111.32 km
        val latDelta = radiusKm / 111.32
        val minLat = lat - latDelta
        val maxLat = lat + latDelta

        // FIX: Phải thêm whereEqualTo("status", "approved") để thỏa mãn Security Rules của Firestore.
        // LƯU Ý: Việc thêm filter này yêu cầu bạn phải tạo một Composite Index cho (status: ASC, latitude: ASC) 
        // trong Firebase Console, nếu không sẽ gặp lỗi "The query requires an index".
        db.collection("rooms")
            .whereEqualTo("status", "approved")
            .whereGreaterThanOrEqualTo("latitude", minLat)
            .whereLessThanOrEqualTo("latitude", maxLat)
            .get()
            .addOnSuccessListener { docs -> 
                onSuccess(docs.toList()) 
            }
            .addOnFailureListener { e ->
                val msg = e.message ?: ""
                if (msg.contains("index", ignoreCase = true) || msg.contains("FAILED_PRECONDITION", ignoreCase = true)) {
                    onFailure("Chưa cấu hình Firestore Index. Vào Firebase Console → Firestore → Indexes → thêm Composite Index: collection 'rooms', fields: status (Ascending) + latitude (Ascending).")
                } else {
                    onFailure("Lỗi tìm kiếm quanh đây: $msg")
                }
            }
    }

    // --- RoomViewModel ---

    fun loadOwnerRooms(userId: String, onSuccess: (List<com.google.firebase.firestore.DocumentSnapshot>) -> Unit, onFailure: (String) -> Unit) {
        db.collection("rooms")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener { docs -> onSuccess(docs.documents.sortedByDescending { it.getCreatedAtMs() }) }
            .addOnFailureListener { e -> onFailure("Không thể tải thông tin phòng: ${e.message}") }
    }

    fun loadOwnerPostedRooms(userId: String, onSuccess: (List<com.google.firebase.firestore.DocumentSnapshot>) -> Unit, onFailure: (String) -> Unit) {
        val currentUid = auth.currentUser?.uid
        val visibleStatuses = if (currentUid == userId) {
            listOf("pending", "approved", "rejected", "rented")
        } else {
            listOf("approved")
        }

        db.collection("rooms")
            .whereEqualTo("userId", userId)
            .whereIn("status", visibleStatuses)
            .get()
            .addOnSuccessListener { docs ->
                onSuccess(docs.documents.sortedByDescending { it.getCreatedAtMs() })
            }
            .addOnFailureListener { e -> onFailure("Không thể tải bài đăng của chủ trọ: ${e.message}") }
    }

    fun loadSingleRoomDoc(roomId: String, onSuccess: (com.google.firebase.firestore.DocumentSnapshot?) -> Unit, onFailure: (String) -> Unit) {
        db.collection("rooms").document(roomId).get()
            .addOnSuccessListener { doc -> onSuccess(if (doc.exists()) doc else null) }
            .addOnFailureListener { e -> onFailure("Không thể tải thông tin phòng: ${e.message}") }
    }

    fun checkSavedStatus(uid: String, roomId: String, onResult: (Boolean) -> Unit) {
        db.collection("savedPosts").document("${uid}_${roomId}").get()
            .addOnSuccessListener { doc -> onResult(doc.exists()) }
            .addOnFailureListener { onResult(false) }
    }

    fun checkActiveBooking(uid: String, roomId: String, onResult: (Boolean) -> Unit) {
        db.collection("appointments").whereEqualTo("tenantId", uid).get()
            .addOnSuccessListener { snap ->
                val blockingStatuses = setOf("pending", "confirmed")
                val hasActive = snap.documents.any { doc ->
                    doc.getString("roomId") == roomId &&
                    blockingStatuses.contains(doc.getString("status") ?: "")
                }
                onResult(hasActive)
            }
            .addOnFailureListener { onResult(false) }
    }

    fun toggleSavePost(uid: String, roomId: String, roomData: Map<String, Any>, onResult: (Boolean) -> Unit, onFailure: (String) -> Unit) {
        val docRef = db.collection("savedPosts").document("${uid}_${roomId}")
        docRef.get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    docRef.delete()
                        .addOnSuccessListener { onResult(false) }
                        .addOnFailureListener { e -> onFailure("Không thể bỏ lưu: ${e.message}") }
                } else {
                    val savedPost = hashMapOf(
                        "userId" to uid, "roomId" to roomId,
                        "ownerId" to (roomData["userId"] as? String ?: ""),
                        "title" to (roomData["title"] as? String ?: ""),
                        "price" to ((roomData["price"] as? Number)?.toLong() ?: 0L),
                        "address" to (roomData["address"] as? String ?: ""),
                        "ward" to (roomData["ward"] as? String ?: ""),
                        "district" to (roomData["district"] as? String ?: ""),
                        "imageUrl" to ((roomData["imageUrls"] as? List<*>)?.mapNotNull { it as? String }?.firstOrNull() ?: ""),
                        "savedAt" to System.currentTimeMillis()
                    )
                    docRef.set(savedPost)
                        .addOnSuccessListener { onResult(true) }
                        .addOnFailureListener { e -> onFailure("Không thể lưu bài: ${e.message}") }
                }
            }
            .addOnFailureListener { e -> onFailure("Lỗi kết nối: ${e.message}") }
    }

    // --- HomeViewModel ---

    fun loadUserName(uid: String, onSuccess: (String) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                onSuccess(if (doc.exists()) doc.getString("fullName") ?: "Bạn" else "Bạn")
            }
            .addOnFailureListener { onSuccess("Bạn") }
    }

    fun loadPopularAreas(onSuccess: (List<Pair<String, Int>>) -> Unit) {
        db.collection("stats").document("popular_areas").get()
            .addOnSuccessListener { doc ->
                if (doc.exists() && doc.contains("areas")) {
                    val areasList = mutableListOf<Pair<String, Int>>()
                    val areasRaw = doc.get("areas") as? List<*>
                    areasRaw?.forEach { item ->
                        val map = item as? Map<*, *>
                        if (map != null) {
                            val district = map["district"] as? String ?: ""
                            val count = (map["count"] as? Number)?.toInt() ?: 0
                            if (district.isNotEmpty()) {
                                areasList.add(Pair(district, count))
                            }
                        }
                    }
                    onSuccess(areasList.take(6))
                } else {
                    // Fallback to manual calculation if stats document doesn't exist yet
                    db.collection("rooms").whereEqualTo("status", "approved").get()
                        .addOnSuccessListener { documents ->
                            val districtCount = mutableMapOf<String, Int>()
                            val districtDisplay = mutableMapOf<String, String>()
                            for (rDoc in documents) {
                                val rawDistrict = rDoc.getString("district") ?: continue
                                val district = rawDistrict.trim().replace(Regex("\\s+"), " ")
                                if (district.isEmpty()) continue
                                val key = district.lowercase(Locale("vi", "VN"))
                                districtCount[key] = (districtCount[key] ?: 0) + 1
                                if (!districtDisplay.containsKey(key)) {
                                    districtDisplay[key] = district
                                }
                            }
                            val top6 = districtCount.entries
                                .sortedByDescending { it.value }
                                .take(6)
                                .map { entry ->
                                    Pair(districtDisplay[entry.key] ?: entry.key, entry.value)
                                }
                            onSuccess(top6)
                        }
                        .addOnFailureListener { onSuccess(emptyList()) }
                }
            }
            .addOnFailureListener { onSuccess(emptyList()) }
    }

    fun loadFeaturedRooms(onSuccess: (List<com.google.firebase.firestore.QueryDocumentSnapshot>) -> Unit) {
        db.collection("rooms").whereEqualTo("status", "approved").whereEqualTo("isFeatured", true)
            .get()
            .addOnSuccessListener { docs ->
                val now = System.currentTimeMillis()
                onSuccess(docs.toList().filter { (it.getLong("featuredUntil") ?: 0L) > now })
            }
            .addOnFailureListener { onSuccess(emptyList()) }
    }

    fun loadApprovedRoomsPage(
        startAfter: com.google.firebase.firestore.DocumentSnapshot?,
        limit: Long,
        onSuccess: (List<com.google.firebase.firestore.QueryDocumentSnapshot>, com.google.firebase.firestore.DocumentSnapshot?) -> Unit,
        onFailure: () -> Unit
    ) {
        var query = db.collection("rooms").whereEqualTo("status", "approved")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(limit)
        if (startAfter != null) query = query.startAfter(startAfter)
        query.get()
            .addOnSuccessListener { docs ->
                val last = if (docs.isEmpty) null else docs.documents.last()
                onSuccess(docs.toList(), last)
            }
            .addOnFailureListener { onFailure() }
    }

    fun deleteRoom(roomId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        db.collection("rooms").document(roomId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    onFailure("Bài đăng không tồn tại")
                    return@addOnSuccessListener
                }
                val roomTitle = doc.getString("title") ?: "Phòng trọ"
                val imageUrls = (doc.get("imageUrls") as? List<*>)?.filterIsInstance<String>() ?: listOf()
                
                // Xóa document trong Firestore
                db.collection("rooms").document(roomId).delete()
                    .addOnSuccessListener {
                        // Xóa các ảnh liên quan trong Storage
                        imageUrls.forEach { url ->
                            try {
                                storage.getReferenceFromUrl(url).delete()
                            } catch (e: Exception) {
                                android.util.Log.e("RoomRepository", "Lỗi xóa ảnh: ${e.message}")
                            }
                        }
                        
                        // FIX: Đồng bộ dọn dẹp các bài lưu và tự động hủy lịch hẹn của phòng bị xóa
                        deleteSavedPostsByRoom(roomId)
                        cancelAllAppointmentsForRoom(roomId, roomTitle)
                        cancelFeaturedRequestsForRoom(roomId)

                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        onFailure(e.message ?: "Lỗi khi xóa bài đăng")
                    }
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Lỗi khi lấy thông tin bài đăng")
            }
    }

    // Đánh dấu đã cho thuê: Cập nhật trạng thái status thành rented thay vì xóa hẳn
    fun markAsRented(roomId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val roomRef = db.collection("rooms").document(roomId)
        roomRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val roomTitle = document.getString("title") ?: "Phòng trọ"

                // Cập nhật trạng thái thành "rented"
                roomRef.update(mapOf(
                    "status" to "rented",
                    "updatedAt" to System.currentTimeMillis()
                )).addOnSuccessListener {
                    deleteSavedPostsByRoom(roomId)
                    // Hủy và thông báo tất cả lịch hẹn còn lại của phòng này.
                    cancelAllAppointmentsForRoom(roomId, roomTitle)
                    cancelFeaturedRequestsForRoom(roomId)
                    onSuccess()
                }.addOnFailureListener { e -> onFailure(e.message ?: "Lỗi khi cập nhật trạng thái cho thuê") }
            } else {
                onFailure("Không tìm thấy bài đăng")
            }
        }.addOnFailureListener { e -> onFailure("Lỗi truy xuất: ${e.message}") }
    }

    private fun deleteSavedPostsByRoom(roomId: String) {
        db.collection("savedPosts")
            .whereEqualTo("roomId", roomId)
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) return@addOnSuccessListener
                val batch = db.batch()
                docs.documents.forEach { batch.delete(it.reference) }
                batch.commit()
            }
    }

    private fun cancelFeaturedRequestsForRoom(roomId: String) {
        db.collection("featured_upgrade_requests")
            .whereEqualTo("roomId", roomId)
            .get()
            .addOnSuccessListener { snapshots ->
                if (snapshots.isEmpty) return@addOnSuccessListener
                val batch = db.batch()
                snapshots.documents.forEach { doc ->
                    val status = doc.getString("status") ?: ""
                    if (status == "waiting_for_payment" || status == "paid_waiting_admin") {
                        batch.update(doc.reference, mapOf(
                            "status" to "cancelled",
                            "approvalStatus" to "cancelled",
                            "updatedAt" to System.currentTimeMillis()
                        ))
                    }
                }
                batch.commit()
            }
    }

    /**
     * Tìm và hủy tất cả lịch hẹn đang mở (pending/confirmed/tenant_confirmed) của một phòng,
     * xóa bookedSlots tương ứng và gửi thông báo cho từng người thuê bị ảnh hưởng.
     *
     * Gọi khi Chủ trọ ấn nút "Đã cho thuê" từ màn hình Bài đăng (không có appointmentId cụ thể).
     * KHAI THÁC 1 whereEqualTo duy nhất — không cần Composite Index trên Firebase.
     */
    private fun cancelAllAppointmentsForRoom(roomId: String, roomTitle: String) {
        val uid = auth.currentUser?.uid ?: return
        val activeStatuses = setOf("pending", "confirmed", "tenant_confirmed")

        db.collection("appointments")
            .whereEqualTo("roomId", roomId)
            .whereEqualTo("landlordId", uid)
            .get()
            .addOnSuccessListener { snapshots ->
                for (doc in snapshots) {
                    val status = doc.getString("status") ?: ""
                    // Lọc status bằng code — tránh phục thuộc whereIn
                    if (status !in activeStatuses) continue

                    val tenantId = doc.getString("tenantId") ?: continue

                    // Gửi thông báo cho người thuê
                    db.collection("notifications").add(
                        hashMapOf(
                            "userId" to tenantId,
                            "title" to "Phòng đã có người thuê",
                            "message" to "Phòng \"$roomTitle\" bạn hẹn xem đã được chủ trọ cho thuê. Lịch hẹn của bạn đã bị hủy tự động.",
                            "type" to "room_already_rented",
                            "seen" to false,
                            "createdAt" to System.currentTimeMillis()
                        )
                    )

                    // Hủy lịch hẹn
                    db.collection("appointments").document(doc.id)
                        .update(
                            mapOf(
                                "status" to "cancelled_by_system",
                                "hasUnreadUpdate" to true
                            )
                        )

                    // Xóa slot đặt lịch an toàn bằng truy vấn appointmentId
                    db.collection("bookedSlots")
                        .whereEqualTo("appointmentId", doc.id)
                        .get()
                        .addOnSuccessListener { slotDocs ->
                            for (slotDoc in slotDocs) {
                                slotDoc.reference.delete()
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("RoomRepository", "cancelAllAppointmentsForRoom thất bại: ${e.message}")
            }
    }

    // Tải dữ liệu phòng dưới dạng Map (dùng cho EditPostViewModel)
    fun loadRoomData(roomId: String, onSuccess: (Map<String, Any>?) -> Unit) {
        db.collection("rooms").document(roomId).get()
            .addOnSuccessListener { doc -> onSuccess(if (doc.exists()) doc.data else null) }
    }

    // Upload ảnh mới và cập nhật bài đăng (dùng cho EditPostViewModel)
    fun updatePost(
        context: android.content.Context,
        roomId: String,
        existingImageUrls: List<String>,
        newImageUris: List<android.net.Uri>,
        deletedImageUrls: List<String>,
        data: HashMap<String, Any>,
        onProgress: (String) -> Unit,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (newImageUris.isNotEmpty()) {
            val compressedUris = newImageUris.mapNotNull { uri ->
                com.example.doantotnghiep.Utils.ImageUtils.compressImage(context, uri)
            }
            if (compressedUris.isEmpty()) {
                onFailure("Lỗi nén ảnh, vui lòng thử lại.")
                return
            }
            val urls = mutableListOf<String>()
            var count = 0
            compressedUris.forEach { uri ->
                val ref = storage.reference.child("rooms/$roomId/img_${java.util.UUID.randomUUID()}.jpg")
                ref.putFile(uri, imageMetadata).continueWithTask { it.result?.storage?.downloadUrl }
                    .addOnSuccessListener { downloadUrl ->
                        urls.add(downloadUrl.toString())
                        count++
                        onProgress("Đang đăng bài: $count/${compressedUris.size} ảnh")
                        if (count == compressedUris.size) {
                            saveUpdatedPost(roomId, existingImageUrls + urls, deletedImageUrls, data, onSuccess, onFailure)
                        }
                    }
                    .addOnFailureListener { e -> onFailure(mapStorageUploadError(e)) }
            }
        } else {
            saveUpdatedPost(roomId, existingImageUrls, deletedImageUrls, data, onSuccess, onFailure)
        }
    }

    private fun saveUpdatedPost(
        roomId: String,
        allUrls: List<String>,
        deletedImageUrls: List<String>,
        data: HashMap<String, Any>,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        data["imageUrls"] = allUrls
        data["status"] = "pending"
        data["updatedAt"] = System.currentTimeMillis()
        // Reset lý do từ chối và cờ thông báo khi chủ trọ sửa lại bài
        data["rejectReason"] = ""
        data["hasUnreadUpdate"] = false
        db.collection("rooms").document(roomId).update(data)
            .addOnSuccessListener {
                deletedImageUrls.forEach { url ->
                    runCatching { storage.getReferenceFromUrl(url).delete() }
                        .onFailure { error ->
                            android.util.Log.w("RoomRepository", "Invalid storage image url while deleting old post image", error)
                        }
                }
                onSuccess()
            }
            .addOnFailureListener { e -> onFailure(e.message ?: "Lỗi cập nhật bài") }
    }


    fun listenUnseenNotificationCount(uid: String, onResult: (Int) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        return db.collection("notifications")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    onResult(0)
                    return@addSnapshotListener
                }
                // Đếm thủ công để đảm bảo tính chính xác tuyệt đối
                val count = snapshots?.documents?.count { doc ->
                    val seenValue = doc.get("seen")
                    val isReadValue = doc.get("isRead")
                    
                    // Logic: Chưa đọc khi (seen là false HOẶC seen không tồn tại) 
                    // VÀ đồng thời (isRead là false HOẶC isRead không tồn tại)
                    val isNotSeen = (seenValue == false || seenValue == null)
                    val isNotRead = (isReadValue == false || isReadValue == null)
                    
                    isNotSeen && isNotRead
                } ?: 0
                onResult(count)
            }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.getCreatedAtMs(): Long =
        when (val raw = get("createdAt")) {
            is Number -> raw.toLong()
            is com.google.firebase.Timestamp -> raw.toDate().time
            else -> 0L
        }

    private fun getTodayGmt7String(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("GMT+7")
        return sdf.format(Date())
    }

    private fun rollbackPostQuota(uid: String, usePurchasedSlot: Boolean) {
        val userRef = db.collection("users").document(uid)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            if (usePurchasedSlot) {
                val currentSlots = (snapshot.getLong("purchasedSlots") ?: 0L).toInt()
                transaction.update(userRef, "purchasedSlots", currentSlots + 1)
            } else {
                val storedDate = snapshot.getString("dailyPostCountDate") ?: ""
                val today = getTodayGmt7String()
                if (storedDate == today) {
                    val currentCount = snapshot.getLong("dailyPostCount") ?: 0L
                    if (currentCount > 0L) {
                        transaction.update(userRef, "dailyPostCount", currentCount - 1)
                    }
                }
            }
        }.addOnFailureListener { e ->
            android.util.Log.e("RoomRepository", "Lỗi rollback quota transaction: ${e.message}")
        }
    }
}
