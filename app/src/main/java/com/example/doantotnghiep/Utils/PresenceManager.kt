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

    fun goOnline() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("users").document(uid).update(
            mapOf(
                "isOnline" to true,
                "lastSeen"  to System.currentTimeMillis()
            )
        )
    }

    fun goOffline() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("users").document(uid).update(
            mapOf(
                "isOnline" to false,
                "lastSeen"  to System.currentTimeMillis()
            )
        )
    }
}
