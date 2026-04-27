package com.example.doantotnghiep.repository

import com.example.doantotnghiep.Model.Conversation
import com.example.doantotnghiep.Model.Message
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import android.net.Uri


class ChatRepository {

    private val db = FirebaseFirestore.getInstance()

    /**
     * Tạo chatId duy nhất từ 2 uid (sắp xếp để tránh trùng lặp)
     */
    fun buildChatId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }

    /**
     * Lấy hoặc tạo cuộc hội thoại giữa 2 người dùng.
     * Trả về chatId và thời điểm bị soft-delete (myDeletedAt) qua callback.
     */
    fun getOrCreateChat(
        myUid: String,
        myName: String,
        myAvatar: String,
        otherUid: String,
        otherName: String,
        otherAvatar: String,
        onSuccess: (chatId: String, myDeletedAt: Long) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val chatId = buildChatId(myUid, otherUid)
        val chatRef = db.collection("chats").document(chatId)

        chatRef.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                // Cuộc chat đã tồn tại
                @Suppress("UNCHECKED_CAST")
                val deletedForMap = (doc.get("deletedFor") as? Map<String, Long>) ?: mapOf()
                val myDeletedAt = deletedForMap[myUid] ?: 0L
                onSuccess(chatId, myDeletedAt)
            } else {
                // Tạo mới
                val chatData = hashMapOf(
                    "participants"      to listOf(myUid, otherUid),
                    "participantNames"  to mapOf(myUid to myName, otherUid to otherName),
                    "participantAvatars" to mapOf(myUid to myAvatar, otherUid to otherAvatar),
                    "lastMessage"       to "",
                    "lastMessageAt"     to 0L,
                    "lastSenderId"      to "",
                    "unreadCount"       to mapOf(myUid to 0L, otherUid to 0L),
                    "deletedFor"        to mapOf(myUid to 0L, otherUid to 0L),
                    "createdAt"         to System.currentTimeMillis()
                )
                chatRef.set(chatData)
                    .addOnSuccessListener { onSuccess(chatId, 0L) }
                    .addOnFailureListener { onFailure(it.message ?: "Lỗi tạo cuộc chat") }
            }
        }.addOnFailureListener {
            onFailure(it.message ?: "Lỗi kết nối")
        }
    }


    /**
     * Gửi tin nhắn và cập nhật lastMessage trên document chat.
     */
    fun sendMessage(
        chatId: String,
        senderId: String,
        receiverId: String,
        text: String,
        imageUrl: String? = null,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val msgRef = db.collection("chats").document(chatId)
            .collection("messages").document()

        val msgData = hashMapOf<String, Any>(
            "id"        to msgRef.id,
            "chatId"    to chatId,
            "senderId"  to senderId,
            "text"      to text,
            "createdAt" to System.currentTimeMillis(),
            "seen"      to false
        )
        if (!imageUrl.isNullOrEmpty()) {
            msgData["imageUrl"] = imageUrl
        }

        msgRef.set(msgData).addOnSuccessListener {
            // Cập nhật lastMessage trên chat document và tăng unread của người nhận
            val displayMessage = if (!imageUrl.isNullOrEmpty() && text.isEmpty()) "[Hình ảnh]" else text
            db.collection("chats").document(chatId).update(
                mapOf(
                    "lastMessage"     to displayMessage,
                    "lastMessageAt"   to System.currentTimeMillis(),
                    "lastSenderId"    to senderId,
                    "unreadCount.$receiverId" to com.google.firebase.firestore.FieldValue.increment(1)
                )
            ).addOnSuccessListener {
                onSuccess()

                // Gửi thông báo push cho người nhận bằng cách ghi vào collection notifications.
                // Cloud Function sendPushNotification sẽ tự động kích hoạt và gửi FCM.
                db.collection("users").document(senderId).get()
                    .addOnSuccessListener { senderDoc ->
                        val senderName = senderDoc.getString("fullName") ?: "Ai đó"
                        val notifMessage = if (!imageUrl.isNullOrEmpty() && text.isEmpty()) "Đã gửi một ảnh" else text
                        val notif = hashMapOf(
                            "userId"    to receiverId,
                            "title"     to "Tin nhắn mới từ $senderName",
                            "message"   to notifMessage,
                            "type"      to "new_message",
                            "chatId"    to chatId,
                            "senderId"  to senderId,
                            "seen"      to false,
                            "createdAt" to System.currentTimeMillis()
                        )
                        db.collection("notifications").add(notif)
                            .addOnFailureListener { e ->
                                android.util.Log.w("ChatRepo", "Không gửi được notification: ${e.message}")
                            }
                    }
            }
                .addOnFailureListener { onFailure(it.message ?: "Lỗi cập nhật") }
        }.addOnFailureListener {
            onFailure(it.message ?: "Lỗi gửi tin nhắn")
        }

    }

    /**
     * Lắng nghe real-time danh sách tin nhắn trong một cuộc chat.
     * Bỏ qua những tin nhắn đã có trước khi mình soft-delete (nếu có).
     */
    fun listenMessages(
        chatId: String,
        myDeletedAt: Long,
        onUpdate: (List<Message>) -> Unit
    ): ListenerRegistration {
        return db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                val messages = snap.documents.mapNotNull { doc ->
                    try {
                        val createdAt = doc.getLong("createdAt") ?: 0L
                        // Nếu tin nhắn được tạo trước hoặc ngay tại lúc xóa -> Ẩn đi
                        if (myDeletedAt > 0L && createdAt <= myDeletedAt) {
                            return@mapNotNull null
                        }

                        @Suppress("UNCHECKED_CAST")
                        val reactions = (doc.get("reactions") as? Map<String, String>) ?: mapOf()

                        Message(
                            id        = doc.getString("id") ?: doc.id,
                            chatId    = doc.getString("chatId") ?: chatId,
                            senderId  = doc.getString("senderId") ?: "",
                            text      = doc.getString("text") ?: "",
                            imageUrl  = doc.getString("imageUrl") ?: "", // <--- Parse image url
                            createdAt = createdAt,
                            seen      = doc.getBoolean("seen") ?: false,
                            reactions = reactions
                        )
                    } catch (_: Exception) { null }
                }
                onUpdate(messages)
            }
    }

    /**
     * Thêm/cập nhật/xóa emoji reaction của user vào một tin nhắn.
     * Nếu user đã thả emoji đó rồi → xóa (toggle off).
     * Nếu user thả emoji khác → thay thế.
     */
    fun addReaction(
        chatId: String,
        messageId: String,
        userId: String,
        emoji: String,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        val msgRef = db.collection("chats").document(chatId)
            .collection("messages").document(messageId)

        // Đọc reaction hiện tại rồi toggle
        msgRef.get().addOnSuccessListener { doc ->
            @Suppress("UNCHECKED_CAST")
            val current = (doc.get("reactions") as? Map<String, String>)?.toMutableMap() ?: mutableMapOf()
            if (current[userId] == emoji) {
                // Đã thả emoji này rồi → bỏ (toggle off)
                current.remove(userId)
            } else {
                current[userId] = emoji
            }
            msgRef.update("reactions", current)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { onFailure(it.message ?: "Lỗi reaction") }
        }.addOnFailureListener { onFailure(it.message ?: "Lỗi reaction") }
    }


    /**
     * Lắng nghe real-time danh sách cuộc hội thoại của một user.
     * Không dùng orderBy để tránh lỗi thiếu Composite Index — sort ở client-side.
     * Lọc ra những cuộc trò chuyện đã bị người dùng "xóa phía mình" (soft delete).
     */
    fun listenConversations(
        uid: String,
        onUpdate: (List<Conversation>) -> Unit
    ): ListenerRegistration {
        return db.collection("chats")
            .whereArrayContains("participants", uid)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    android.util.Log.e("ChatRepo", "listenConversations lỗi: ${error.message}")
                    return@addSnapshotListener
                }
                if (snap == null) return@addSnapshotListener
                val convs = snap.documents.mapNotNull { doc ->
                    try {
                        @Suppress("UNCHECKED_CAST")
                        val deletedForMap = (doc.get("deletedFor") as? Map<String, Long>) ?: mapOf()
                        val myDeletedAt   = deletedForMap[uid] ?: 0L
                        val lastMsgAt     = doc.getLong("lastMessageAt") ?: 0L

                        // Ẩn nếu cuộc chat đã bị xóa phía mình VÀ chưa có tin nhắn mới
                        if (myDeletedAt > 0L && lastMsgAt <= myDeletedAt) {
                            return@mapNotNull null
                        }

                        Conversation(
                            chatId             = doc.id,
                            participants       = (doc.get("participants") as? List<String>) ?: listOf(),
                            participantNames   = (doc.get("participantNames") as? Map<String, String>) ?: mapOf(),
                            participantAvatars = (doc.get("participantAvatars") as? Map<String, String>) ?: mapOf(),
                            lastMessage        = doc.getString("lastMessage") ?: "",
                            lastMessageAt      = lastMsgAt,
                            lastSenderId       = doc.getString("lastSenderId") ?: "",
                            unreadCount        = (doc.get("unreadCount") as? Map<String, Long>) ?: mapOf()
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("ChatRepo", "Parse conversation lỗi: ${e.message}")
                        null
                    }
                }
                // Sort mới nhất trên đầu ở phía client
                val sorted = convs.sortedByDescending { it.lastMessageAt }
                onUpdate(sorted)
            }
    }

    /**
     * Đánh dấu tất cả tin nhắn chưa đọc (của người nhận = myUid) là đã đọc.
     * Đồng thời reset unreadCount về 0.
     */
    fun markMessagesAsSeen(chatId: String, myUid: String) {
        // Reset unread count
        db.collection("chats").document(chatId)
            .update("unreadCount.$myUid", 0)

        // Đánh dấu từng message chưa đọc (chỉ những message không phải mình gửi)
        db.collection("chats").document(chatId)
            .collection("messages")
            .whereEqualTo("seen", false)
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) return@addOnSuccessListener
                val batch = db.batch()
                snap.documents.forEach { doc ->
                    if (doc.getString("senderId") != myUid) {
                        batch.update(doc.reference, "seen", true)
                    }
                }
                batch.commit()
            }
    }

    /**
     * Cập nhật tên và avatar của một người trong tất cả các cuộc chat họ tham gia.
     * Gọi sau khi user đổi avatar hoặc tên.
     */
    fun updateUserInfoInChats(uid: String, newName: String, newAvatarUrl: String) {
        db.collection("chats")
            .whereArrayContains("participants", uid)
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) return@addOnSuccessListener
                val batch = db.batch()
                snap.documents.forEach { doc ->
                    batch.update(doc.reference, mapOf(
                        "participantNames.$uid"   to newName,
                        "participantAvatars.$uid" to newAvatarUrl
                    ))
                }
                batch.commit()
            }
    }

    /**
     * Xóa 1 tin nhắn (chỉ người gửi mới được xóa).
     */
    fun deleteMessage(
        chatId: String,
        messageId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("chats").document(chatId)
            .collection("messages").document(messageId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Lỗi xóa tin nhắn") }
    }

    /**
     * Xóa cuộc trò chuyện chỉ phía mình (soft delete).
     * Người kia vẫn thấy bình thường.
     * Nếu sau này người kia gửi tin mới, cuộc trò chuyện sẽ tự hiện lại với mình.
     */
    fun deleteConversation(
        chatId: String,
        myUid: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        // Chỉ đặt timestamp "đã xóa phía mình" — không xóa document thật sự
        db.collection("chats").document(chatId)
            .update("deletedFor.$myUid", System.currentTimeMillis())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Lỗi xóa cuộc trò chuyện") }
    }

    /**
     * Tải ảnh lên Firebase Storage
     */
    fun uploadChatImage(
        chatId: String,
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val fileName = "${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}.jpg"
        val storageRef = FirebaseStorage.getInstance("gs://doantotnghiep-b39ae.firebasestorage.app")
            .reference
            .child("chat_images/${chatId}/${fileName}")

        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    onSuccess(uri.toString())
                }.addOnFailureListener { e ->
                    onFailure("Không lấy được URL ảnh: ${e.message}")
                }
            }
            .addOnFailureListener { e ->
                onFailure("Tải ảnh thất bại: ${e.message}")
            }
    }
}
