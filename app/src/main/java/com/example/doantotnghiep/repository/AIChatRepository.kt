package com.example.doantotnghiep.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AIChatRepository {

    private val db = FirebaseFirestore.getInstance()

    fun loadContextData(uid: String, onComplete: (Map<String, Any?>) -> Unit) {
        db.collection("users").document(uid)
            .collection("ai_context").document("current")
            .get()
            .addOnSuccessListener { doc ->
                onComplete(if (doc != null && doc.exists()) doc.data ?: emptyMap() else emptyMap())
            }
            .addOnFailureListener { onComplete(emptyMap()) }
    }

    fun saveContextData(uid: String, data: Map<String, Any?>) {
        db.collection("users").document(uid)
            .collection("ai_context").document("current")
            .set(data)
    }

    fun deleteContext(uid: String) {
        db.collection("users").document(uid)
            .collection("ai_context").document("current")
            .delete()
    }

    fun loadConversationHistory(
        uid: String,
        onSuccess: (List<DocumentSnapshot>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("users").document(uid).collection("ai_conversations")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { snapshots ->
                onSuccess(snapshots?.documents ?: emptyList())
            }
            .addOnFailureListener { e ->
                onFailure("Lỗi tải lịch sử chat: ${e.message}")
            }
    }

    fun saveConversationMessage(uid: String, data: Map<String, Any?>) {
        db.collection("users").document(uid).collection("ai_conversations").add(data)
    }

    fun deleteConversationHistory(
        uid: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("users").document(uid).collection("ai_conversations")
            .get()
            .addOnSuccessListener { snapshots ->
                val docs = snapshots.documents
                if (docs.isEmpty()) {
                    onSuccess()
                    return@addOnSuccessListener
                }
                val chunks = docs.chunked(500)
                var committed = 0
                for (chunk in chunks) {
                    val batch = db.batch()
                    chunk.forEach { batch.delete(it.reference) }
                    batch.commit().addOnCompleteListener {
                        committed++
                        if (committed == chunks.size) onSuccess()
                    }
                }
            }
            .addOnFailureListener { onFailure("Xóa thất bại, thử lại sau!") }
    }

    fun fetchApprovedRooms(
        limit: Long = 200,
        onSuccess: (List<DocumentSnapshot>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("rooms")
            .whereEqualTo("status", "approved")
            .limit(limit)
            .get()
            .addOnSuccessListener { snapshots ->
                onSuccess(snapshots?.documents ?: emptyList())
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Lỗi không xác định")
            }
    }
}
