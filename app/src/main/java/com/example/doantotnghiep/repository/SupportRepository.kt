package com.example.doantotnghiep.repository

import android.net.Uri
import com.example.doantotnghiep.Model.Message
import com.example.doantotnghiep.Model.SupportTicket
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class SupportRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance("gs://doantotnghiep-b39ae.firebasestorage.app")

    fun listenMyTickets(
        onUpdate: (List<SupportTicket>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        val uid = auth.currentUser?.uid
        val empty = object : ListenerRegistration { override fun remove() {} }
        if (uid.isNullOrEmpty()) return empty

        return db.collection("support_tickets")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    onError("Lỗi tải yêu cầu hỗ trợ: ${error.message}")
                    return@addSnapshotListener
                }
                val tickets = snap?.documents?.map { doc ->
                    doc.toObject(SupportTicket::class.java)?.copy(id = doc.id)
                        ?: SupportTicket(id = doc.id)
                }?.sortedByDescending { it.updatedAt } ?: emptyList()
                onUpdate(tickets)
            }
    }

    fun listenMessages(
        ticketId: String,
        onUpdate: (List<Message>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return db.collection("support_tickets").document(ticketId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    onError("Lỗi tải tin nhắn hỗ trợ: ${error.message}")
                    return@addSnapshotListener
                }
                val messages = snap?.documents?.mapNotNull { doc ->
                    try {
                        Message(
                            id = doc.id,
                            chatId = ticketId,
                            senderId = doc.getString("senderId") ?: "",
                            text = doc.getString("text") ?: "",
                            imageUrl = doc.getString("imageUrl") ?: "",
                            createdAt = doc.getLong("createdAt") ?: 0L,
                            seen = doc.getBoolean("seenByAdmin") ?: false
                        )
                    } catch (_: Exception) {
                        null
                    }
                } ?: emptyList()
                onUpdate(messages)
            }
    }

    fun createTicket(
        category: String,
        title: String,
        text: String,
        imageUri: Uri?,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val user = auth.currentUser ?: run {
            onFailure("Bạn cần đăng nhập để gửi hỗ trợ")
            return
        }

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { userDoc ->
                val ticketRef = db.collection("support_tickets").document()
                val userName = userDoc.getString("fullName") ?: user.displayName ?: "Người dùng"
                val userEmail = userDoc.getString("email") ?: user.email ?: ""

                fun saveTicket(imageUrl: String) {
                    val now = System.currentTimeMillis()
                    val displayMessage = when {
                        text.isNotBlank() -> text.trim()
                        imageUrl.isNotBlank() -> "[Hình ảnh]"
                        else -> ""
                    }
                    val ticketData = hashMapOf<String, Any>(
                        "userId" to user.uid,
                        "userName" to userName,
                        "userEmail" to userEmail,
                        "category" to category,
                        "title" to title.trim(),
                        "status" to "new",
                        "priority" to "normal",
                        "createdAt" to now,
                        "updatedAt" to now,
                        "lastMessage" to displayMessage,
                        "lastSenderRole" to "user",
                        "adminId" to "",
                        "adminName" to "",
                        "unreadForUser" to false,
                        "unreadForAdmin" to true
                    )
                    val messageRef = ticketRef.collection("messages").document()
                    val messageData = hashMapOf<String, Any>(
                        "senderId" to user.uid,
                        "senderRole" to "user",
                        "text" to text.trim(),
                        "imageUrl" to imageUrl,
                        "createdAt" to now,
                        "seenByUser" to true,
                        "seenByAdmin" to false
                    )
                    val batch = db.batch()
                    batch.set(ticketRef, ticketData)
                    batch.set(messageRef, messageData)
                    batch.commit()
                        .addOnSuccessListener { onSuccess(ticketRef.id) }
                        .addOnFailureListener { onFailure("Không gửi được yêu cầu: ${it.message}") }
                }

                if (imageUri != null) {
                    uploadSupportImage(ticketRef.id, imageUri, ::saveTicket, onFailure)
                } else {
                    saveTicket("")
                }
            }
            .addOnFailureListener { onFailure("Không lấy được thông tin tài khoản: ${it.message}") }
    }

    fun sendUserMessage(
        ticketId: String,
        text: String,
        imageUri: Uri?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: run {
            onFailure("Bạn cần đăng nhập để nhắn tin hỗ trợ")
            return
        }

        fun saveMessage(imageUrl: String) {
            val now = System.currentTimeMillis()
            val displayMessage = when {
                text.isNotBlank() -> text.trim()
                imageUrl.isNotBlank() -> "[Hình ảnh]"
                else -> ""
            }
            val ticketRef = db.collection("support_tickets").document(ticketId)
            val messageRef = ticketRef.collection("messages").document()
            val batch = db.batch()
            batch.set(messageRef, hashMapOf<String, Any>(
                "senderId" to uid,
                "senderRole" to "user",
                "text" to text.trim(),
                "imageUrl" to imageUrl,
                "createdAt" to now,
                "seenByUser" to true,
                "seenByAdmin" to false
            ))
            ticketRef.get()
                .addOnSuccessListener { ticketDoc ->
                    val currentStatus = ticketDoc.getString("status") ?: "new"
                    val nextStatus = if (currentStatus == "resolved") "in_progress" else currentStatus
                    batch.update(ticketRef, mapOf(
                        "updatedAt" to now,
                        "lastMessage" to displayMessage,
                        "lastSenderRole" to "user",
                        "unreadForAdmin" to true,
                        "unreadForUser" to false,
                        "status" to nextStatus
                    ))
                    batch.commit()
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { onFailure("Không gửi được tin nhắn: ${it.message}") }
                }
                .addOnFailureListener { onFailure("Không kiểm tra được yêu cầu hỗ trợ: ${it.message}") }
        }

        if (imageUri != null) {
            uploadSupportImage(ticketId, imageUri, ::saveMessage, onFailure)
        } else {
            saveMessage("")
        }
    }

    fun closeTicket(
        ticketId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("support_tickets").document(ticketId)
            .update(
                mapOf(
                    "status" to "closed",
                    "updatedAt" to System.currentTimeMillis(),
                    "unreadForUser" to false
                )
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure("Không đóng được yêu cầu: ${it.message}") }
    }

    fun markUserRead(ticketId: String) {
        val ticketRef = db.collection("support_tickets").document(ticketId)
        ticketRef.update("unreadForUser", false).addOnFailureListener { }
        ticketRef.collection("messages")
            .whereEqualTo("senderRole", "admin")
            .whereEqualTo("seenByUser", false)
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) return@addOnSuccessListener
                val batch = db.batch()
                snap.documents.forEach { doc ->
                    batch.update(doc.reference, "seenByUser", true)
                }
                batch.commit()
            }
    }

    private fun uploadSupportImage(
        ticketId: String,
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val fileName = "${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
        val storageRef = storage.reference.child("support_images/$ticketId/$fileName")
        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl
                    .addOnSuccessListener { onSuccess(it.toString()) }
                    .addOnFailureListener { onFailure("Không lấy được URL ảnh: ${it.message}") }
            }
            .addOnFailureListener { onFailure("Tải ảnh hỗ trợ thất bại: ${it.message}") }
    }
}
