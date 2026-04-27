package com.example.doantotnghiep.View.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doantotnghiep.Model.SupportTicket
import com.example.doantotnghiep.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SupportTicketAdapter(
    private val onClick: (SupportTicket) -> Unit
) : RecyclerView.Adapter<SupportTicketAdapter.ViewHolder>() {

    private val items = mutableListOf<SupportTicket>()
    private var selectedTicketId = ""

    fun submitList(tickets: List<SupportTicket>, selectedId: String) {
        items.clear()
        items.addAll(tickets)
        selectedTicketId = selectedId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_support_ticket, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTicketTitle)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvTicketCategory)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvTicketStatus)
        private val tvLastMessage: TextView = itemView.findViewById(R.id.tvTicketLastMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTicketTime)
        private val unreadDot: View = itemView.findViewById(R.id.viewTicketUnread)
        private val cardView: androidx.cardview.widget.CardView = itemView as androidx.cardview.widget.CardView

        fun bind(ticket: SupportTicket) {
            tvTitle.text = ticket.title
            tvCategory.text = ticket.category
            tvStatus.text = statusText(ticket.status)
            tvLastMessage.text = ticket.lastMessage.ifBlank { "Chưa có nội dung" }
            tvTime.text = if (ticket.updatedAt > 0) {
                SimpleDateFormat("dd/MM HH:mm", Locale("vi", "VN")).format(Date(ticket.updatedAt))
            } else {
                ""
            }
            unreadDot.visibility = if (ticket.unreadForUser) View.VISIBLE else View.GONE
            itemView.isSelected = ticket.id == selectedTicketId
            cardView.setCardBackgroundColor(
                if (ticket.id == selectedTicketId) 0xFFE3F2FD.toInt() else 0xFFFFFFFF.toInt()
            )
            
            // Đổi màu badge trạng thái cho đẹp
            when (ticket.status) {
                "new" -> tvStatus.setTextColor(0xFFE53935.toInt()) // Red
                "in_progress" -> tvStatus.setTextColor(0xFFFB8C00.toInt()) // Orange
                "resolved" -> tvStatus.setTextColor(0xFF43A047.toInt()) // Green
                "closed" -> tvStatus.setTextColor(0xFF757575.toInt()) // Grey
            }

            itemView.setOnClickListener { onClick(ticket) }
        }
    }

    private fun statusText(status: String): String {
        return when (status) {
            "new" -> "Mới"
            "in_progress" -> "Đang xử lý"
            "resolved" -> "Đã xử lý"
            "closed" -> "Đã đóng"
            else -> status
        }
    }
}
