package com.example.doantotnghiep.View.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doantotnghiep.R

data class NotificationItem(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "",
    val ticketId: String = "",
    val ticketTitle: String = "",
    val isRead: Boolean = false,
    val createdAt: Long = 0L
)

class NotificationAdapter(
    private val onItemClick: (NotificationItem) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    private val items = mutableListOf<NotificationItem>()

    fun submitList(list: List<NotificationItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvNotifTitle)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvNotifMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvNotifTime)
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivIcon)
        private val unreadDot: View = itemView.findViewById(R.id.unreadDot)

        fun bind(item: NotificationItem) {
            tvTitle.text = item.title
            tvMessage.text = item.message
            tvTime.text = timeAgo(item.createdAt)

            // Ẩn/hiện chấm chưa đọc
            unreadDot.visibility = if (item.isRead) View.GONE else View.VISIBLE

            // Đổi màu nền card khi chưa đọc
            val cardBg = itemView.rootView.findViewById<View>(R.id.unreadDot)
            itemView.setBackgroundColor(
                if (item.isRead) 0xFFFFFFFF.toInt() else 0xFFF0F4FF.toInt()
            )

            // Icon tương ứng loại thông báo
            val iconRes = when (item.type) {
                "post_approved"          -> R.drawable.ic_check_circle
                "post_rejected"          -> R.drawable.ic_cancel
                "verification_approved"  -> R.drawable.ic_verified
                "verification_rejected"  -> R.drawable.ic_cancel
                "appointment"            -> R.drawable.ic_calendar
                "support_reply"          -> R.drawable.ic_info
                else                     -> R.drawable.ic_notification
            }
            ivIcon.setImageResource(iconRes)

            itemView.setOnClickListener { onItemClick(item) }
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
            return java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(ms)
        }
    }
}
