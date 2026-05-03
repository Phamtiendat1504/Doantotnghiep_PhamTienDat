package com.example.doantotnghiep.View.Adapter

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doantotnghiep.Model.Message
import com.example.doantotnghiep.R
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val myUid: String,
    private val messages: MutableList<Message> = mutableListOf(),
    private val onLongClickMessage: ((Message) -> Unit)? = null,
    private val onReaction: ((messageId: String, emoji: String) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT     = 1
        private const val VIEW_TYPE_RECEIVED = 2

        val EMOJIS = listOf(
            "\u2764\uFE0F", "\uD83D\uDE02", "\uD83D\uDE0D", "\uD83D\uDE22",
            "\uD83D\uDE21", "\uD83D\uDC4D", "\uD83D\uDC4F", "\uD83D\uDE4F",
            "\uD83D\uDD25", "\uD83C\uDF89", "\u2B50", "\u2705"
        )
    }

    fun submitList(newMessages: List<Message>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        if (messages[position].senderId == myUid) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_sent, parent, false)
            SentViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_received, parent, false)
            ReceivedViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        val timeStr = formatTime(msg.createdAt)
        val showTime = position == 0 ||
            (msg.createdAt - messages[position - 1].createdAt > 5 * 60 * 1000) ||
            !isSameDay(msg.createdAt, messages[position - 1].createdAt)

        val reactionText = buildReactionText(msg.reactions)

        if (holder is SentViewHolder) {
            // Hiển thị ảnh nếu có
            if (msg.imageUrl.isNotEmpty()) {
                holder.cardImage.visibility = View.VISIBLE
                Glide.with(holder.itemView.context).load(msg.imageUrl).into(holder.ivImage)
            } else {
                holder.cardImage.visibility = View.GONE
            }

            // Hiển thị text hoặc ẩn nếu rỗng
            if (msg.text.isNotEmpty()) {
                holder.tvMessage.text = msg.text
                holder.tvMessage.visibility = View.VISIBLE
            } else {
                holder.tvMessage.visibility = View.GONE
            }

            holder.tvTime.text = timeStr
            holder.tvTime.visibility = if (showTime) View.VISIBLE else View.GONE
            holder.tvSeen.visibility =
                if (position == messages.size - 1 && msg.seen) View.VISIBLE else View.GONE

            if (reactionText.isNotEmpty()) {
                holder.tvReactions.text = reactionText
                holder.tvReactions.visibility = View.VISIBLE
            } else {
                holder.tvReactions.visibility = View.GONE
            }

            // Gắn event onLongClick
            val anchorView = if (msg.text.isNotEmpty()) holder.tvMessage else holder.cardImage
            anchorView.setOnLongClickListener {
                showOptionsPopup(anchorView, msg, isMine = true)
                true
            }
        } else if (holder is ReceivedViewHolder) {
            // Hiển thị ảnh nếu có
            if (msg.imageUrl.isNotEmpty()) {
                holder.cardImage.visibility = View.VISIBLE
                Glide.with(holder.itemView.context).load(msg.imageUrl).into(holder.ivImage)
            } else {
                holder.cardImage.visibility = View.GONE
            }

            // Hiển thị text hoặc ẩn nếu rỗng
            if (msg.text.isNotEmpty()) {
                holder.tvMessage.text = msg.text
                holder.tvMessage.visibility = View.VISIBLE
            } else {
                holder.tvMessage.visibility = View.GONE
            }

            holder.tvTime.text = timeStr
            holder.tvTime.visibility = if (showTime) View.VISIBLE else View.GONE

            if (reactionText.isNotEmpty()) {
                holder.tvReactions.text = reactionText
                holder.tvReactions.visibility = View.VISIBLE
            } else {
                holder.tvReactions.visibility = View.GONE
            }

            val anchorView = if (msg.text.isNotEmpty()) holder.tvMessage else holder.cardImage
            anchorView.setOnLongClickListener {
                showOptionsPopup(anchorView, msg, isMine = false)
                true
            }
        }
    }

    override fun getItemCount() = messages.size

    // ── Options popup (tin mình: có thêm tùy chọn xóa, ảnh: lưu) ──────────────

    private fun showOptionsPopup(anchor: View, msg: Message, isMine: Boolean) {
        val context = anchor.context
        val options = mutableListOf<String>()
        val actions = mutableListOf<() -> Unit>()

        // Luôn có thả cảm xúc
        options.add("Thả cảm xúc")
        actions.add { showEmojiPickerPopup(anchor, msg) }

        // Nếu là tin nhắn của mình thì có nút xóa
        if (isMine) {
            options.add("Xóa tin nhắn")
            actions.add { onLongClickMessage?.invoke(msg) }
        }

        // Nếu là tin nhắn ảnh thì cho phép lưu ảnh
        if (msg.imageUrl.isNotEmpty()) {
            options.add("Lưu ảnh")
            actions.add { downloadImage(context, msg.imageUrl) }
        }

        // Nếu người kia gửi tin nhắn GHI CHỮ (chỉ có 1 tùy chọn thả cảm xúc), xổ lưới ra luôn cho nhanh y hệt fb
        if (options.size == 1) {
            actions[0].invoke()
            return
        }

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle(null)
            .setItems(options.toTypedArray()) { _, which ->
                actions[which].invoke()
            }
            .show()
    }

    private fun downloadImage(context: android.content.Context, url: String) {
        try {
            val request = android.app.DownloadManager.Request(android.net.Uri.parse(url))
            val fileName = "Chat_Image_${System.currentTimeMillis()}.jpg"
            request.setTitle("Đang tải ảnh...")
            request.setDescription("Đang lưu ảnh từ Chat xuống máy")
            request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_PICTURES, fileName)
            
            val manager = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
            manager.enqueue(request)
            android.widget.Toast.makeText(context, "Đang tải ảnh xuống Thư viện (Pictures)...", android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Lỗi tải ảnh: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // ── Emoji Picker Popup (lưới 6 cột, cuộn được) ───────────────────

    private fun showEmojiPickerPopup(anchor: View, msg: Message) {
        val context = anchor.context
        val popupView = LayoutInflater.from(context)
            .inflate(R.layout.popup_emoji_picker, null)

        val grid = popupView.findViewById<GridLayout>(R.id.emojiGrid)
        val density = context.resources.displayMetrics.density
        val cellSize = (44 * density).toInt()

        // Tạo tham chiếu PopupWindow trước để dùng trong lambda
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.elevation = 16f

        // Populate động tất cả emoji
        EMOJIS.forEachIndexed { _, emoji ->
            val tv = TextView(context).apply {
                text = emoji
                textSize = 22f
                gravity = Gravity.CENTER
                layoutParams = GridLayout.LayoutParams().apply {
                    width  = cellSize
                    height = cellSize
                    setMargins(2, 2, 2, 2)
                }
                setOnClickListener {
                    onReaction?.invoke(msg.id, emoji)
                    popupWindow.dismiss()
                }
            }
            grid.addView(tv)
        }

        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

        // Hiện popup phía trên bubble, căn giữa
        popupWindow.showAsDropDown(
            anchor,
            0,
            -(anchor.height + popupView.measuredHeight + 8),
            Gravity.CENTER_HORIZONTAL
        )
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private fun buildReactionText(reactions: Map<String, String>): String {
        if (reactions.isEmpty()) return ""
        val count = mutableMapOf<String, Int>()
        reactions.values.forEach { emoji -> count[emoji] = (count[emoji] ?: 0) + 1 }
        return count.entries.joinToString(" ") { (emoji, cnt) ->
            if (cnt > 1) "$emoji$cnt" else emoji
        }
    }

    private fun formatTime(ts: Long): String {
        if (ts == 0L) return ""
        val today = Calendar.getInstance()
        return if (isSameDay(ts, today.timeInMillis)) {
            SimpleDateFormat("HH:mm", Locale("vi", "VN")).format(Date(ts))
        } else {
            SimpleDateFormat("dd/MM HH:mm", Locale("vi", "VN")).format(Date(ts))
        }
    }

    private fun isSameDay(ts1: Long, ts2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = ts1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = ts2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    // ── ViewHolders ──────────────────────────────────────────────────
    inner class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMessage:   TextView = itemView.findViewById(R.id.tvMessageSent)
        val tvTime:      TextView = itemView.findViewById(R.id.tvTimeSent)
        val tvSeen:      TextView = itemView.findViewById(R.id.tvSeen)
        val tvReactions: TextView = itemView.findViewById(R.id.tvReactionsSent)
        val cardImage:   CardView = itemView.findViewById(R.id.cardImageSent)
        val ivImage:     ImageView= itemView.findViewById(R.id.ivImageSent)
    }

    inner class ReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMessage:   TextView = itemView.findViewById(R.id.tvMessageReceived)
        val tvTime:      TextView = itemView.findViewById(R.id.tvTimeReceived)
        val tvReactions: TextView = itemView.findViewById(R.id.tvReactionsReceived)
        val cardImage:   CardView = itemView.findViewById(R.id.cardImageReceived)
        val ivImage:     ImageView= itemView.findViewById(R.id.ivImageReceived)
    }
}
