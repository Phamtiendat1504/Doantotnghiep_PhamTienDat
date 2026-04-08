package com.example.doantotnghiep.Utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.doantotnghiep.MainActivity
import com.example.doantotnghiep.R

object PostNotificationHelper {

    private const val CHANNEL_ID = "post_room_channel"
    private const val NOTIF_ID = 1001

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Đăng bài cho thuê",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Thông báo tiến trình đăng bài phòng trọ"
                setSound(null, null)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun showProgress(context: Context, progress: Int) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Đang đăng bài...")
            .setContentText("Đang tải ảnh và lưu thông tin: $progress%")
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)

        notify(context, builder)
    }

    fun showSuccess(context: Context, title: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openTab", "posts")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Đăng bài thành công!")
            .setContentText("\"$title\" đang chờ admin duyệt.")
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(pendingIntent)

        notify(context, builder)
    }

    fun showError(context: Context) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Đăng bài thất bại")
            .setContentText("Có lỗi xảy ra khi đăng bài. Vui lòng thử lại.")
            .setAutoCancel(true)
            .setOngoing(false)

        notify(context, builder)
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIF_ID)
    }

    private fun notify(context: Context, builder: NotificationCompat.Builder) {
        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID, builder.build())
        } catch (_: SecurityException) {
            // Permission chưa được cấp (Android 13+), bỏ qua
        }
    }
}