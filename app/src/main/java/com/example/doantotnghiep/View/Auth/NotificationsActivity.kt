package com.example.doantotnghiep.View.Auth

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doantotnghiep.R
import com.example.doantotnghiep.View.Adapter.NotificationAdapter
import com.example.doantotnghiep.View.Adapter.NotificationItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class NotificationsActivity : AppCompatActivity() {

    private lateinit var rvNotifications: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: View
    private lateinit var btnMarkAll: TextView
    private lateinit var btnBack: View

    private val adapter = NotificationAdapter { item ->
        // Bấm vào thông báo -> đánh dấu đã đọc
        if (!item.isRead) {
            FirebaseFirestore.getInstance()
                .collection("notifications")
                .document(item.id)
                .update("isRead", true)
        }
    }

    private var listenerRegistration: ListenerRegistration? = null
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        rvNotifications = findViewById(R.id.rvNotifications)
        progressBar     = findViewById(R.id.progressBar)
        emptyState      = findViewById(R.id.emptyState)
        btnMarkAll      = findViewById(R.id.btnMarkAll)
        btnBack         = findViewById(R.id.btnBack)

        rvNotifications.layoutManager = LinearLayoutManager(this)
        rvNotifications.adapter = adapter

        btnBack.setOnClickListener { finish() }

        btnMarkAll.setOnClickListener { markAllRead() }

        loadNotifications()
    }

    private fun loadNotifications() {
        val uid = auth.currentUser?.uid ?: return
        progressBar.visibility = View.VISIBLE

        listenerRegistration = db.collection("notifications")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snap, error ->
                progressBar.visibility = View.GONE
                if (error != null) {
                    android.util.Log.e("Notifications", "Listen failed.", error)
                    return@addSnapshotListener
                }
                if (snap == null) return@addSnapshotListener

                val list = snap.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    NotificationItem(
                        id        = doc.id,
                        title     = data["title"] as? String ?: "",
                        message   = data["message"] as? String ?: "",
                        type      = data["type"] as? String ?: "",
                        isRead    = data["isRead"] as? Boolean ?: false,
                        createdAt = data["createdAt"] as? Long ?: 0L
                    )
                }.sortedByDescending { it.createdAt } // Sắp xếp tại máy thay vì Server

                if (list.isEmpty()) {
                    emptyState.visibility = View.VISIBLE
                    rvNotifications.visibility = View.GONE
                } else {
                    emptyState.visibility = View.GONE
                    rvNotifications.visibility = View.VISIBLE
                    adapter.submitList(list)
                }
            }
    }

    private fun markAllRead() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("notifications")
            .whereEqualTo("userId", uid)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { snap ->
                val batch = db.batch()
                snap.forEach { doc -> batch.update(doc.reference, "isRead", true) }
                batch.commit()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove()
    }
}
