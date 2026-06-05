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

    private fun stringMap(value: Any?): Map<String, String> {
        return (value as? Map<*, *>)?.mapNotNull { (key, mapValue) ->
            val safeKey = key as? String ?: return@mapNotNull null
            val safeValue = mapValue as? String ?: return@mapNotNull null
            safeKey to safeValue
        }?.toMap() ?: emptyMap()
    }

    private fun longMap(value: Any?): Map<String, Long> {
        return (value as? Map<*, *>)?.mapNotNull { (key, mapValue) ->
            val safeKey = key as? String ?: return@mapNotNull null
            val safeValue = when (mapValue) {
                is com.google.firebase.Timestamp -> mapValue.toDate().time
                is Number -> mapValue.toLong()
                else -> null
            } ?: return@mapNotNull null
            safeKey to safeValue
        }?.toMap() ?: emptyMap()
    }

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
                val deletedForMap = longMap(doc.get("deletedFor"))
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
                    "createdAt"         to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
                chatRef.set(chatData)
                    .addOnSuccessListener { onSuccess(chatId, 0L) }
                    .addOnFailureListener { onFailure(it.message ?: "Lỗi tạo cuộc chat") }
            }
        }.addOnFailureListener { e ->
            val msg = e.message ?: "Lỗi kết nối"
            if (msg.contains("offline", ignoreCase = true)) {
                onFailure("Không có kết nối mạng. Vui lòng kiểm tra lại Internet.")
            } else {
                onFailure("Lỗi kết nối: $msg")
            }
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
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "seen"      to false
        )
        if (!imageUrl.isNullOrEmpty()) {
            msgData["imageUrl"] = imageUrl
        }

        val displayMessage = if (!imageUrl.isNullOrEmpty() && text.isEmpty()) "[Hình ảnh]" else text
        val batch = db.batch()
        batch.set(msgRef, msgData)
        
        val chatRef = db.collection("chats").document(chatId)
        batch.update(
            chatRef,
            mapOf(
                "lastMessage"     to displayMessage,
                "lastMessageAt"   to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "lastSenderId"    to senderId,
                "unreadCount.$receiverId" to com.google.firebase.firestore.FieldValue.increment(1)
            )
        )

        batch.commit()
            .addOnSuccessListener {
                onSuccess()

                // Gửi thông báo push cho người nhận bằng cách ghi vào collection notifications.
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
                            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        )
                        db.collection("notifications").add(notif)
                            .addOnFailureListener { e ->
                                android.util.Log.w("ChatRepo", "Không gửi được notification: ${e.message}")
                            }
                    }
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Lỗi gửi tin nhắn")
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
                        val isPending = doc.metadata.hasPendingWrites()
                        var createdAt = when (val timeVal = doc.get("createdAt")) {
                            is com.google.firebase.Timestamp -> timeVal.toDate().time
                            is Number -> timeVal.toLong()
                            else -> 0L
                        }
                        if (isPending && createdAt == 0L) {
                            createdAt = System.currentTimeMillis()
                        }
                        // Nếu tin nhắn được tạo trước hoặc ngay tại lúc xóa -> Ẩn đi
                        if (myDeletedAt > 0L && createdAt <= myDeletedAt) {
                            return@mapNotNull null
                        }

                        val reactions = stringMap(doc.get("reactions"))

                        Message(
                            id        = doc.getString("id") ?: doc.id,
                            chatId    = doc.getString("chatId") ?: chatId,
                            senderId  = doc.getString("senderId") ?: "",
                            text      = doc.getString("text") ?: "",
                            imageUrl  = doc.getString("imageUrl") ?: "", // <--- Parse image url
                            createdAt = createdAt,
                            seen      = doc.getBoolean("seen") ?: false,
                            reactions = reactions,
                            isPending = isPending
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

        db.runTransaction { transaction ->
            val snapshot = transaction.get(msgRef)
            val current = stringMap(snapshot.get("reactions")).toMutableMap()
            if (current[userId] == emoji) {
                // Đã thả emoji này rồi → bỏ (toggle off)
                current.remove(userId)
            } else {
                current[userId] = emoji
            }
            transaction.update(msgRef, "reactions", current)
            null
        }
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { e -> onFailure(e.message ?: "Lỗi reaction") }
    }


    /**
     * Lắng nghe real-time danh sách cuộc hội thoại của một user.
     * Không dùng orderBy để tránh lỗi thiếu Composite Index — sort ở client-side.
     * Lọc ra những cuộc trò chuyện đã bị người dùng "xóa phía mình" (soft delete).
     * Lọc bỏ "conversation zombie": UID người kia không còn tồn tại trong users
     * (xảy ra khi người dùng xóa tài khoản và tạo lại với cùng email → UID mới).
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
                        val deletedForMap = longMap(doc.get("deletedFor"))
                        val myDeletedAt   = deletedForMap[uid] ?: 0L
                        val lastMsgAt = when (val timeVal = doc.get("lastMessageAt")) {
                            is com.google.firebase.Timestamp -> timeVal.toDate().time
                            is Number -> timeVal.toLong()
                            else -> 0L
                        }

                        // Ẩn nếu cuộc chat đã bị xóa phía mình VÀ chưa có tin nhắn mới
                        if (myDeletedAt > 0L && lastMsgAt <= myDeletedAt) {
                            return@mapNotNull null
                        }

                        Conversation(
                            chatId             = doc.id,
                            participants       = (doc.get("participants") as? List<*>)?.mapNotNull { it as? String } ?: listOf(),
                            participantNames   = stringMap(doc.get("participantNames")),
                            participantAvatars = stringMap(doc.get("participantAvatars")),
                            lastMessage        = doc.getString("lastMessage") ?: "",
                            lastMessageAt      = lastMsgAt,
                            lastSenderId       = doc.getString("lastSenderId") ?: "",
                            unreadCount        = longMap(doc.get("unreadCount"))
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("ChatRepo", "Parse conversation lỗi: ${e.message}")
                        null
                    }
                }

                // --- Lọc "Conversation Zombie" ---
                // Khi một tài khoản bị xóa và tạo lại, UID cũ vẫn còn trong participants
                // nhưng document users/{oldUid} không còn tồn tại.
                // Batch-get kiểm tra UID người kia còn sống hay không.
                val otherUids = convs.mapNotNull { conv ->
                    conv.participants.firstOrNull { it != uid }
                }.distinct()

                if (otherUids.isEmpty()) {
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }

                // Firestore whereIn giới hạn 30 phần tử — chunked để an toàn
                val allExistingUids = mutableSetOf<String>()
                val batches = otherUids.chunked(30)
                var completedBatches = 0

                batches.forEach { batch ->
                    db.collection("users")
                        .whereIn(com.google.firebase.firestore.FieldPath.documentId(), batch)
                        .get()
                        .addOnSuccessListener { userSnap ->
                            userSnap.documents.forEach { userDoc ->
                                if (userDoc.exists()) allExistingUids.add(userDoc.id)
                            }
                            completedBatches++
                            if (completedBatches == batches.size) {
                                // Chỉ giữ conversation mà UID người kia vẫn tồn tại trong users
                                val filtered = convs.filter { conv ->
                                    val otherId = conv.participants.firstOrNull { it != uid }
                                    otherId != null && allExistingUids.contains(otherId)
                                }
                                val sorted = filtered.sortedByDescending { it.lastMessageAt }
                                onUpdate(sorted)
                            }
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.e("ChatRepo", "Lỗi check users tồn tại: ${e.message}")
                            completedBatches++
                            // Fallback an toàn: nếu network lỗi thì vẫn hiển thị toàn bộ
                            if (completedBatches == batches.size) {
                                val sorted = convs.sortedByDescending { it.lastMessageAt }
                                onUpdate(sorted)
                            }
                        }
                }
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
            .limit(490)
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
     * Bổ sung: Xóa luôn file ảnh trên Storage nếu tin nhắn có chứa ảnh.
     */
    fun deleteMessage(
        chatId: String,
        messageId: String,
        imageUrl: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val messagesRef = db.collection("chats").document(chatId).collection("messages")
        val msgRef = messagesRef.document(messageId)
        
        messagesRef.orderBy("createdAt", Query.Direction.DESCENDING).limit(2).get()
            .addOnSuccessListener { snap ->
                val docs = snap.documents
                val isLastMessage = docs.isNotEmpty() && docs[0].id == messageId
                
                val batch = db.batch()
                batch.delete(msgRef)
                
                if (isLastMessage) {
                    val chatRef = db.collection("chats").document(chatId)
                    if (docs.size > 1) {
                        val prevDoc = docs[1]
                        val prevText = prevDoc.getString("text") ?: ""
                        val prevImg = prevDoc.getString("imageUrl") ?: ""
                        val prevSenderId = prevDoc.getString("senderId") ?: ""
                        
                        var prevCreatedAt = 0L
                        when (val timeVal = prevDoc.get("createdAt")) {
                            is com.google.firebase.Timestamp -> prevCreatedAt = timeVal.toDate().time
                            is Number -> prevCreatedAt = timeVal.toLong()
                        }
                        
                        val displayMsg = if (prevImg.isNotEmpty() && prevText.isEmpty()) "[Hình ảnh]" else prevText
                        batch.update(chatRef, mapOf(
                            "lastMessage" to displayMsg,
                            "lastMessageAt" to prevCreatedAt,
                            "lastSenderId" to prevSenderId
                        ))
                    } else {
                        batch.update(chatRef, mapOf(
                            "lastMessage" to "",
                            "lastMessageAt" to 0L,
                            "lastSenderId" to ""
                        ))
                    }
                }
                
                batch.commit()
                    .addOnSuccessListener {
                        onSuccess()
                        if (imageUrl.isNotEmpty() &&
                            (imageUrl.startsWith("https://firebasestorage.googleapis.com") ||
                             imageUrl.startsWith("gs://"))) {
                            try {
                                FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl).delete()
                                    .addOnFailureListener { e ->
                                        android.util.Log.e("ChatRepo", "Lỗi xóa ảnh cũ trên Storage: ${e.message}")
                                    }
                            } catch (e: Exception) {
                                android.util.Log.e("ChatRepo", "Lỗi getReferenceFromUrl: ${e.message}")
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        onFailure(e.message ?: "Lỗi xóa tin nhắn")
                    }
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Lỗi truy vấn tin nhắn trước khi xóa")
            }
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
            .update("deletedFor.$myUid", com.google.firebase.firestore.FieldValue.serverTimestamp())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Lỗi xóa cuộc trò chuyện") }
    }

    /**
     * Tải ảnh lên Firebase Storage
     */
    fun uploadChatImage(
        chatId: String,
        imageUri: Uri,
        onProgress: (Int) -> Unit = {},
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val fileName = "${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}.jpg"
        val storageRef = FirebaseStorage.getInstance("gs://doantotnghiep-b39ae.firebasestorage.app")
            .reference
            .child("chat_images/${chatId}/${fileName}")

        storageRef.putFile(imageUri)
            .addOnProgressListener { taskSnapshot ->
                if (taskSnapshot.totalByteCount > 0) {
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                    onProgress(progress)
                }
            }
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
