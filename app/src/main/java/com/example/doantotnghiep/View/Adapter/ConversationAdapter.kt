package com.example.doantotnghiep.View.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doantotnghiep.Model.Conversation
import com.example.doantotnghiep.R
import java.text.SimpleDateFormat
import java.util.*

class ConversationAdapter(
    private val myUid: String,
    private val onItemClick: (Conversation) -> Unit,
    private val onLongClick: (Conversation) -> Unit = {}
) : RecyclerView.Adapter<ConversationAdapter.ViewHolder>() {


    private val items = mutableListOf<Conversation>()

    fun submitList(data: List<Conversation>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatar: ImageView  = itemView.findViewById(R.id.ivConvAvatar)
        private val tvName: TextView     = itemView.findViewById(R.id.tvConvName)
        private val tvLastMsg: TextView  = itemView.findViewById(R.id.tvConvLastMessage)
        private val tvTime: TextView     = itemView.findViewById(R.id.tvConvTime)
        private val tvBadge: TextView    = itemView.findViewById(R.id.tvConvUnreadBadge)

        fun bind(conv: Conversation) {
            // Lấy thông tin của người kia (không phải mình)
            val otherId = conv.participants.firstOrNull { it != myUid } ?: ""
            val otherName   = conv.participantNames[otherId] ?: "Người dùng"
            val otherAvatar = conv.participantAvatars[otherId] ?: ""

            tvName.text = otherName

            // Last message
            if (conv.lastMessage.isEmpty()) {
                tvLastMsg.text = "Bắt đầu cuộc trò chuyện..."
                tvLastMsg.alpha = 0.5f
            } else {
                val prefix = if (conv.lastSenderId == myUid) "Bạn: " else ""
                tvLastMsg.text = "$prefix${conv.lastMessage}"
                tvLastMsg.alpha = 1f
            }

            // Thời gian
            tvTime.text = formatTime(conv.lastMessageAt)

            // Unread badge
            val unread = conv.unreadCount[myUid] ?: 0L
            if (unread > 0) {
                tvBadge.isVisible = true
                tvBadge.text = if (unread > 99) "99+" else unread.toString()
                tvLastMsg.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                tvBadge.isVisible = false
                tvLastMsg.setTypeface(null, android.graphics.Typeface.NORMAL)
            }

            // Avatar
            if (otherAvatar.isNotEmpty()) {
                ivAvatar.setPadding(0, 0, 0, 0)
                ivAvatar.imageTintList = null
                Glide.with(itemView.context).load(otherAvatar).circleCrop().into(ivAvatar)
            } else {
                ivAvatar.setImageResource(com.example.doantotnghiep.R.drawable.ic_person)
                ivAvatar.setPadding(20, 20, 20, 20)
                ivAvatar.imageTintList = android.content.res.ColorStateList.valueOf(0xFF9E9E9E.toInt())
            }

            itemView.setOnClickListener { onItemClick(conv) }
            itemView.setOnLongClickListener {
                onLongClick(conv)
                true
            }
        }
    }

    private fun formatTime(ts: Long): String {
        if (ts == 0L) return ""
        val now = Calendar.getInstance()
        val cal = Calendar.getInstance().apply { timeInMillis = ts }
        return when {
            now.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR) ->
                SimpleDateFormat("HH:mm", Locale("vi", "VN")).format(Date(ts))
            now.get(Calendar.YEAR) == cal.get(Calendar.YEAR) ->
                SimpleDateFormat("dd/MM", Locale("vi", "VN")).format(Date(ts))
            else ->
                SimpleDateFormat("dd/MM/yy", Locale("vi", "VN")).format(Date(ts))
        }
    }
}
