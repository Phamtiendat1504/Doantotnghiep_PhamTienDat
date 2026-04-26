package com.example.doantotnghiep.Utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.doantotnghiep.MainActivity
import com.example.doantotnghiep.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "fcm_notification_channel"
        private const val CHANNEL_NAME = "Thông báo hệ thống"
    }

    /**
     * Hàm này được gọi khi Token của thiết bị bị thay đổi (do cài lại app, reset máy v.v.)
     * Nhiệm vụ: Lưu Token mới nhất lên Firestore để Server luôn biết địa chỉ đúng.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        saveFcmToken(token)
    }

    /**
     * Hàm này được gọi khi có tin nhắn Push bay về LÚC APP ĐANG MỞ.
     * (Nếu App đang tắt, hệ thống Android tự hiện thông báo mà không cần code)
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // KIỂM TRA BẢO MẬT: Chỉ hiện thông báo nếu thiết bị đang đăng nhập đúng tài khoản được gửi
        val targetUid = remoteMessage.data["userId"]
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid

        if (targetUid != null && currentUid != null && targetUid != currentUid) {
            android.util.Log.w("FCM", "Từ chối hiển thị thông báo: Nhầm tài khoản")
            return
        }

        val type    = remoteMessage.data["type"] ?: "general"
        val senderId = remoteMessage.data["senderId"] ?: ""

        if (!AppSettings.shouldShowNotification(this, type)) {
            return
        }

        // KIỂM TRA MÀN HÌNH CHAT ĐANG MỞ: Nếu người dùng đang nhắn tin với người gửi, bỏ qua Pop-up
        if (type == "new_message" &&
            com.example.doantotnghiep.View.Auth.ChatActivity.currentOpenedChatOtherUid == senderId
        ) {
            return // Đang chat với người này rồi, không hiện thông báo nữa.
        }

        val title   = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Thông báo mới"
        val body    = remoteMessage.notification?.body  ?: remoteMessage.data["message"] ?: ""
        val chatId  = remoteMessage.data["chatId"] ?: ""
        val ticketId = remoteMessage.data["ticketId"] ?: ""
        val ticketTitle = remoteMessage.data["ticketTitle"] ?: "Yêu cầu hỗ trợ"

        showNotification(title, body, type, chatId, senderId, ticketId, ticketTitle)
    }



    /**
     * Lưu FCM Token lên Firestore vào document của User hiện tại.
     */
    private fun saveFcmToken(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .update("fcmToken", token)
            .addOnSuccessListener {
                android.util.Log.d("FCM", "Token đã được lưu thành công: $token")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("FCM", "Lỗi lưu Token: ${e.message}")
            }
    }

    /**
     * Vẽ và hiển thị thông báo dạng Pop-up khi App đang được mở.
     */
    private fun showNotification(
        title: String,
        body: String,
        type: String = "general",
        chatId: String = "",
        senderId: String = "",
        ticketId: String = "",
        ticketTitle: String = ""
    ) {
        createNotificationChannel()

        val intent: Intent
        if (type == "new_message" && senderId.isNotEmpty()) {
            intent = Intent(this, com.example.doantotnghiep.View.Auth.ChatActivity::class.java).apply {
                putExtra(com.example.doantotnghiep.View.Auth.ChatActivity.EXTRA_OTHER_UID, senderId)
                putExtra(com.example.doantotnghiep.View.Auth.ChatActivity.EXTRA_OTHER_NAME, title.replace("Tin nhắn mới từ ", ""))
            }
        } else if (type == "support_reply" && ticketId.isNotEmpty()) {
            intent = Intent(this, com.example.doantotnghiep.View.Auth.SupportTicketDetailActivity::class.java).apply {
                putExtra(com.example.doantotnghiep.View.Auth.SupportTicketDetailActivity.EXTRA_TICKET_ID, ticketId)
                putExtra(com.example.doantotnghiep.View.Auth.SupportTicketDetailActivity.EXTRA_TICKET_TITLE, ticketTitle.ifBlank { "Yêu cầu hỗ trợ" })
                putExtra(com.example.doantotnghiep.View.Auth.SupportTicketDetailActivity.EXTRA_TICKET_STATUS, "in_progress")
            }
        } else {
            // Mặc định mở MainActivity kèm lệnh mở MyAppointmentsActivity
            intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("action", "open_appointments")
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body)) // Hiện full text nếu dài
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Rung + Âm thanh mặc định
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo lịch hẹn, duyệt bài, trạng thái phòng"
                enableLights(true)
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
