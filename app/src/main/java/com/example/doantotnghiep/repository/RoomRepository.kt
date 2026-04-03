package com.example.doantotnghiep.repository

import android.net.Uri
import com.example.doantotnghiep.Model.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
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
        docRef.set(room)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure("Lưu bài đăng thất bại: ${e.message}") }
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
}
