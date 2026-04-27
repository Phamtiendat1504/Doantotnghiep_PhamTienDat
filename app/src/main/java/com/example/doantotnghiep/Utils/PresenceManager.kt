package com.example.doantotnghiep.Utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Quản lý trạng thái Online/Offline của người dùng trên Firestore.
 *
 * - Gọi [goOnline] khi App vào foreground (onStart của MainActivity)
 * - Gọi [goOffline] khi App ra background (onStop của MainActivity)
 *
 * Dữ liệu lưu trong: users/{uid}/isOnline (Boolean) + lastSeen (Long timestamp)
 */
object PresenceManager {

    private val db = FirebaseFirestore.getInstance()

    fun goOnline() = updatePresence(isOnline = true)

    fun goOffline() = updatePresence(isOnline = false)

    fun goOfflineAndThen(onComplete: () -> Unit) {
        updatePresence(isOnline = false, onComplete = onComplete)
    }

    private fun updatePresence(isOnline: Boolean, onComplete: (() -> Unit)? = null) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            onComplete?.invoke()
            return
        }
        db.collection("users").document(uid).update(
            mapOf(
                "isOnline" to isOnline,
                "lastSeen"  to System.currentTimeMillis()
            )
        ).addOnCompleteListener {
            onComplete?.invoke()
        }
    }
}
