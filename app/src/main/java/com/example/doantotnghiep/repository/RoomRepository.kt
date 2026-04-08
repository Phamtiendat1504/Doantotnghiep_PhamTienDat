package com.example.doantotnghiep.repository

import android.net.Uri
import com.example.doantotnghiep.Model.Room
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.Date
import java.util.UUID

class RoomRepository {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance("gs://doantotnghiep-b39ae.firebasestorage.app")
    private val auth = FirebaseAuth.getInstance()

    fun postRoom(
        room: Room,
        imageUris: List<Uri>,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
        onProgress: (Int) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return onFailure("Chưa đăng nhập. Vui lòng đăng nhập lại.")
        val docRef = db.collection("rooms").document()
        val roomId = docRef.id

        if (imageUris.isEmpty()) {
            val roomData = room.copy(id = roomId, userId = uid)
            saveRoomToFirestore(docRef, roomData, onSuccess, onFailure)
        } else {
            uploadImages(roomId, imageUris, onProgress,
                onComplete = { urls ->
                    val roomData = room.copy(id = roomId, userId = uid, imageUrls = urls)
                    saveRoomToFirestore(docRef, roomData, onSuccess, onFailure)
                },
                onError = { error ->
                    onFailure(error)
                }
            )
        }
    }

    private fun saveRoomToFirestore(
        docRef: com.google.firebase.firestore.DocumentReference,
        room: Room,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        // TTL: xóa khỏi Firestore sau 90 ngày kể từ ngày đăng
        val expireAtMs = room.createdAt + 90L * 24 * 60 * 60 * 1000
        val expireAtTimestamp = Timestamp(Date(expireAtMs))

        docRef.set(room)
            .addOnSuccessListener {
                // Cập nhật thêm trường expireAt kiểu Firestore Timestamp (bắt buộc cho TTL)
                docRef.update("expireAt", expireAtTimestamp)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e ->
                        android.util.Log.e("RoomRepository", "Cập nhật expireAt thất bại: ${e.message}")
                        onFailure("Gia hạn bài đăng thất bại. Vui lòng thử lại.")
                    }
            }
            .addOnFailureListener { e -> onFailure("Lưu bài đăng thất bại: ${e.message}") }
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
            
            val uploadTask = ref.putFile(uri)
            
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
                    onError("Tải ảnh thất bại: ${e.message}")
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
        val query = if (filter != "all") {
            baseQuery.whereEqualTo("status", filter)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
        } else {
            baseQuery.orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
        }
        query.get()
            .addOnSuccessListener { docs -> onSuccess(docs.toList()) }
            .addOnFailureListener { e -> onFailure(e.message ?: "Lỗi tải dữ liệu") }
    }

    // Gia hạn bài đăng thêm 60 ngày
    fun renewPost(docId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val renewedAt = System.currentTimeMillis()
        val newExpireMs = renewedAt + 60L * 24 * 60 * 60 * 1000
        val newExpireTimestamp = Timestamp(Date(newExpireMs))
        db.collection("rooms").document(docId)
            .update("status", "approved", "renewedAt", renewedAt, "expireAt", newExpireTimestamp)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure("Không thể gia hạn: ${e.message}") }
    }

    // Đánh dấu các bài đăng đã được xem (lưu vào SharedPreferences)
    fun markPostsAsSeen(uid: String, context: android.content.Context) {
        db.collection("rooms")
            .whereEqualTo("userId", uid)
            .whereIn("status", listOf("approved", "rejected"))
            .get()
            .addOnSuccessListener { docs ->
                val ids = docs.map { it.id }.toSet()
                val prefs = context.getSharedPreferences("post_seen_$uid", android.content.Context.MODE_PRIVATE)
                val existing = prefs.getStringSet("seen_ids", emptySet()) ?: emptySet()
                prefs.edit().putStringSet("seen_ids", existing + ids).apply()
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
            .addOnSuccessListener { docs -> onSuccess(docs.toList()) }
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

    // --- RoomViewModel ---

    fun loadOwnerRooms(userId: String, onSuccess: (List<com.google.firebase.firestore.DocumentSnapshot>) -> Unit, onFailure: (String) -> Unit) {
        db.collection("rooms")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener { docs -> onSuccess(docs.documents.sortedBy { it.getLong("createdAt") ?: 0 }) }
            .addOnFailureListener { e -> onFailure("Không thể tải thông tin phòng: ${e.message}") }
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

    fun loadBookedSlots(roomId: String, onSuccess: (List<Map<String, Any>>) -> Unit) {
        db.collection("bookedSlots").whereEqualTo("roomId", roomId).get()
            .addOnSuccessListener { snap ->
                val activeStatuses = setOf("pending", "confirmed", "tenant_confirmed")
                val active = snap.documents
                    .filter { activeStatuses.contains(it.getString("status") ?: "") }
                    .sortedBy { it.getString("date") ?: "" }
                    .map { it.data ?: emptyMap() }
                onSuccess(active)
            }
            .addOnFailureListener { onSuccess(emptyList()) }
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
                        "price" to (roomData["price"] as? Long ?: 0),
                        "address" to (roomData["address"] as? String ?: ""),
                        "ward" to (roomData["ward"] as? String ?: ""),
                        "district" to (roomData["district"] as? String ?: ""),
                        "imageUrl" to ((roomData["imageUrls"] as? List<String>)?.firstOrNull() ?: ""),
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
        db.collection("rooms").whereEqualTo("status", "approved").limit(200).get()
            .addOnSuccessListener { documents ->
                val districtCount = mutableMapOf<String, Int>()
                for (doc in documents) {
                    val district = doc.getString("district") ?: continue
                    if (district.isNotEmpty()) districtCount[district] = (districtCount[district] ?: 0) + 1
                }
                val top6 = districtCount.entries.sortedByDescending { it.value }.take(6).map { Pair(it.key, it.value) }
                onSuccess(top6)
            }
            .addOnFailureListener { onSuccess(emptyList()) }
    }

    fun loadFeaturedRooms(onSuccess: (List<com.google.firebase.firestore.QueryDocumentSnapshot>) -> Unit) {
        db.collection("rooms").whereEqualTo("status", "approved").whereEqualTo("isFeatured", true)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(10).get()
            .addOnSuccessListener { docs -> onSuccess(docs.toList()) }
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
        val storageRef = storage.reference.child("rooms").child(roomId)
        storageRef.listAll()
            .addOnSuccessListener { listResult ->
                val deleteTasks = listResult.items.map { it.delete() }
                com.google.android.gms.tasks.Tasks.whenAllComplete(deleteTasks)
                    .addOnCompleteListener {
                        db.collection("rooms").document(roomId)
                            .delete()
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { e -> onFailure("Xóa dữ liệu thất bại: ${e.message}") }
                    }
            }
            .addOnFailureListener { e ->
                db.collection("rooms").document(roomId).delete()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onFailure(it.message ?: "Lỗi xóa") }
            }
    }

    // Tải dữ liệu phòng dưới dạng Map (dùng cho EditPostViewModel)
    fun loadRoomData(roomId: String, onSuccess: (Map<String, Any>?) -> Unit) {
        db.collection("rooms").document(roomId).get()
            .addOnSuccessListener { doc -> onSuccess(if (doc.exists()) doc.data else null) }
    }

    // Upload ảnh mới và cập nhật bài đăng (dùng cho EditPostViewModel)
    fun updatePost(
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
            val urls = mutableListOf<String>()
            var count = 0
            newImageUris.forEach { uri ->
                val ref = storage.reference.child("rooms/$roomId/img_${java.util.UUID.randomUUID()}.jpg")
                ref.putFile(uri).continueWithTask { it.result?.storage?.downloadUrl }
                    .addOnSuccessListener { downloadUrl ->
                        urls.add(downloadUrl.toString())
                        count++
                        onProgress("Đang đăng bài: $count/${newImageUris.size} ảnh")
                        if (count == newImageUris.size) {
                            saveUpdatedPost(roomId, existingImageUrls + urls, deletedImageUrls, data, onSuccess, onFailure)
                        }
                    }
                    .addOnFailureListener { e -> onFailure(e.message ?: "Lỗi upload ảnh") }
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
        db.collection("rooms").document(roomId).update(data)
            .addOnSuccessListener {
                deletedImageUrls.forEach { url ->
                    try { storage.getReferenceFromUrl(url).delete() } catch (_: Exception) {}
                }
                onSuccess()
            }
            .addOnFailureListener { e -> onFailure(e.message ?: "Lỗi cập nhật bài") }
    }

    // Kiểm tra và hết hạn bài đăng quá 2 tháng
    // FIX: Dùng max(createdAt, renewedAt) để bài đã gia hạn không bị expire sai
    fun checkAndExpirePosts(uid: String, onResult: (Int) -> Unit) {
        val twoMonthsAgo = System.currentTimeMillis() - 60L * 24 * 60 * 60 * 1000
        db.collection("rooms")
            .whereEqualTo("userId", uid)
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener { documents ->
                val expiredDocs = documents.filter { doc ->
                    // Lấy mốc thời gian hoạt động cuối (createdAt hoặc renewedAt, lấy cái mới hơn)
                    val lastActiveAt = maxOf(
                        doc.getLong("createdAt") ?: 0L,
                        doc.getLong("renewedAt") ?: 0L
                    )
                    lastActiveAt < twoMonthsAgo
                }
                if (expiredDocs.isEmpty()) { onResult(0); return@addOnSuccessListener }
                val batch = db.batch()
                for (doc in expiredDocs) {
                    batch.update(doc.reference, "status", "expired")
                    val notifRef = db.collection("notifications").document()
                    batch.set(notifRef, mapOf(
                        "userId" to uid,
                        "roomId" to doc.id,
                        "title" to (doc.getString("title") ?: ""),
                        "message" to "Bài đăng \"${doc.getString("title") ?: ""}\" đã hết hạn 2 tháng và bị ẩn khỏi kết quả tìm kiếm. Vui lòng gia hạn hoặc đánh dấu đã cho thuê.",
                        "type" to "expired",
                        "createdAt" to System.currentTimeMillis(),
                        "seen" to false
                    ))
                }
                batch.commit().addOnSuccessListener { onResult(expiredDocs.size) }
            }
    }
}
