package com.example.doantotnghiep.View.Auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doantotnghiep.R
import com.example.doantotnghiep.View.Adapter.NotificationAdapter
import com.example.doantotnghiep.View.Adapter.NotificationItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class NotificationsActivity : AppCompatActivity() {

    private lateinit var rvNotifications: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: View
    private lateinit var btnMarkAll: TextView
    private lateinit var btnBack: View

    private val adapter = NotificationAdapter { item -> showNotificationDetail(item) }

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

        listenerRegistration?.remove()
        listenerRegistration = db.collection("notifications")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snap, error ->
                progressBar.visibility = View.GONE
                if (error != null) {
                    android.util.Log.e("Notifications", "Listen failed.", error)
                    emptyState.visibility = View.VISIBLE
                    rvNotifications.visibility = View.GONE
                    return@addSnapshotListener
                }
                if (snap == null) return@addSnapshotListener

                val list = snap.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val isSeen = (data["seen"] as? Boolean)
                        ?: (data["isRead"] as? Boolean)
                        ?: false
                    NotificationItem(
                        id = doc.id,
                        title = data["title"] as? String ?: "",
                        message = data["message"] as? String ?: "",
                        type = data["type"] as? String ?: "",
                        ticketId = data["ticketId"] as? String ?: "",
                        ticketTitle = data["ticketTitle"] as? String ?: data["title"] as? String ?: "Yêu cầu hỗ trợ",
                        isRead = isSeen,
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
                val unreadDocs = snap.documents.filter { doc ->
                    val seen = doc.getBoolean("seen")
                    val isRead = doc.getBoolean("isRead")
                    (seen == null && isRead == null) || (seen == false) || (isRead == false)
                }

                if (unreadDocs.isEmpty()) {
                    com.example.doantotnghiep.Utils.MessageUtils.showInfoDialog(this, "Thông báo", "Bạn không có thông báo mới nào.")
                    return@addOnSuccessListener
                }
                
                val batch = db.batch()
                unreadDocs.forEach { doc ->
                    batch.update(doc.reference, mapOf("seen" to true, "isRead" to true))
                }
                batch.commit().addOnSuccessListener {
                    com.example.doantotnghiep.Utils.MessageUtils.showSuccessDialog(this, "Thành công", "Đã đánh dấu tất cả là đã đọc.")
                }
            }
    }

    private fun showNotificationDetail(item: NotificationItem) {
        // Đánh dấu đã đọc khi mở dialog
        if (!item.isRead) {
            db.collection("notifications").document(item.id)
                .update(mapOf("seen" to true, "isRead" to true))
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_notification_detail, null)

        val ivIcon     = dialogView.findViewById<ImageView>(R.id.dialogIvIcon)
        val tvTitle    = dialogView.findViewById<TextView>(R.id.dialogTvTitle)
        val tvMessage  = dialogView.findViewById<TextView>(R.id.dialogTvMessage)
        val tvTime     = dialogView.findViewById<TextView>(R.id.dialogTvTime)

        tvTitle.text   = item.title
        tvMessage.text = item.message
        tvTime.text    = timeAgo(item.createdAt)

        val iconRes = when (item.type) {
            "post_approved"         -> R.drawable.ic_check_circle
            "post_rejected"         -> R.drawable.ic_cancel
            "verification_approved" -> R.drawable.ic_verified
            "verification_rejected" -> R.drawable.ic_cancel
            "appointment"           -> R.drawable.ic_calendar
            "support_reply"         -> R.drawable.ic_info
            else                    -> R.drawable.ic_notification
        }
        ivIcon.setImageResource(iconRes)

        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Đóng", null)

        if (item.type == "support_reply" && item.ticketId.isNotBlank()) {
            builder.setPositiveButton("Xem chi tiết") { _, _ ->
                startActivity(Intent(this, SupportTicketDetailActivity::class.java).apply {
                    putExtra(SupportTicketDetailActivity.EXTRA_TICKET_ID, item.ticketId)
                    putExtra(SupportTicketDetailActivity.EXTRA_TICKET_TITLE, item.ticketTitle.ifBlank { "Yêu cầu hỗ trợ" })
                    putExtra(SupportTicketDetailActivity.EXTRA_TICKET_STATUS, "new")
                })
            }
        }

        builder.show()
    }

    private fun timeAgo(ms: Long): String {
        if (ms == 0L) return ""
        val diff = System.currentTimeMillis() - ms
        val minutes = diff / 60000
        if (minutes < 1) return "Vừa xong"
        if (minutes < 60) return "$minutes phút trước"
        val hours = minutes / 60
        if (hours < 24) return "$hours giờ trước"
        val days = hours / 24
        if (days < 7) return "$days ngày trước"
        return java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(ms)
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove()
    }
}
