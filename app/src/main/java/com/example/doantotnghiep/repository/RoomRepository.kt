package com.example.doantotnghiep.repository

import android.net.Uri
import com.example.doantotnghiep.Model.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class RoomRepository {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun postRoom(
        room: Room,
        imageUris: List<Uri>,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
        onProgress: (Int) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return onFailure("Chưa đăng nhập")
        val docRef = db.collection("rooms").document()
        val roomId = docRef.id

        if (imageUris.isEmpty()) {
            // Lưu không có ảnh
            val roomData = room.copy(id = roomId, userId = uid)
            saveRoomToFirestore(docRef, roomData, onSuccess, onFailure)
        } else {
            // Upload ảnh rồi lưu
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
        val urls = mutableListOf<String>()
        var uploaded = 0

        for ((index, uri) in uris.withIndex()) {
            val ref = storage.reference.child("rooms/$roomId/img_$index.jpg")
            ref.putFile(uri)
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { downloadUrl ->
                        urls.add(downloadUrl.toString())
                        uploaded++
                        onProgress((uploaded * 100) / uris.size)

                        if (uploaded == uris.size) {
                            onComplete(urls)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    onError("Upload ảnh thất bại: ${e.message}")
                }
        }
    }
}