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
        // Bấm vào thông báo -> đánh dấu đã xem
        if (!item.isRead) {
            FirebaseFirestore.getInstance()
                .collection("notifications")
                .document(item.id)
                .update("seen", true)
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
                    // Kiểm tra cả 'seen' và 'isRead' để tránh mất dữ liệu cũ
                    val isSeen = if (data.containsKey("seen")) {
                        data["seen"] as? Boolean ?: false
                    } else {
                        data["isRead"] as? Boolean ?: false
                    }
                    
                    NotificationItem(
                        id        = doc.id,
                        title     = data["title"] as? String ?: "",
                        message   = data["message"] as? String ?: "",
                        type      = data["type"] as? String ?: "",
                        isRead    = isSeen,
                        createdAt = data["createdAt"] as? Long ?: 0L
                    )
                }.sortedByDescending { it.createdAt }

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
            .get()
            .addOnSuccessListener { snap ->
                // Lọc thủ công các thông báo chưa đọc (vì có thể thiếu trường seen)
                val unreadDocs = snap.documents.filter { doc ->
                    val seen = doc.getBoolean("seen")
                    val isRead = doc.getBoolean("isRead")
                    // Nếu không có seen và không có isRead, hoặc một trong hai là false thì coi là chưa đọc
                    (seen == null && isRead == null) || (seen == false) || (isRead == false)
                }

                if (unreadDocs.isEmpty()) {
                    com.example.doantotnghiep.Utils.MessageUtils.showInfoDialog(this, "Thông báo", "Bạn không có thông báo mới nào.")
                    return@addOnSuccessListener
                }
                
                val batch = db.batch()
                unreadDocs.forEach { doc -> 
                    batch.update(doc.reference, "seen", true)
                    batch.update(doc.reference, "isRead", true) // Cập nhật cả hai cho chắc chắn
                }
                batch.commit().addOnSuccessListener {
                    // Thành công
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove()
    }
}
