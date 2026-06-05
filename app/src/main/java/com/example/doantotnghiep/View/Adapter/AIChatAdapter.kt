package com.example.doantotnghiep.View.Adapter

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doantotnghiep.Model.AIMessage
import com.example.doantotnghiep.R
import com.example.doantotnghiep.View.Auth.RoomDetailActivity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.core.MarkwonTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AIChatAdapter(
    private val messageList: MutableList<AIMessage>,
    private val quickReplyListener: QuickReplyClickListener? = null
) : RecyclerView.Adapter<AIChatAdapter.MessageViewHolder>() {

    fun updateList(newList: List<AIMessage>) {
        messageList.clear()
        messageList.addAll(newList)
        notifyDataSetChanged()
    }

    interface QuickReplyClickListener {
        fun onQuickReplyClicked(text: String)
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val layoutUserFull: LinearLayout = itemView.findViewById(R.id.layoutUserFull)
        val tvUserMessage: TextView = itemView.findViewById(R.id.tvUserMessage)
        val tvUserTime: TextView = itemView.findViewById(R.id.tvUserTime)

        val layoutAIFull: LinearLayout = itemView.findViewById(R.id.layoutAIFull)
        val tvAIMessage: TextView = itemView.findViewById(R.id.tvAIMessage)
        val tvAITime: TextView = itemView.findViewById(R.id.tvAITime)
        val tvAIName: TextView = itemView.findViewById(R.id.tvAIName)
        val rvSuggestedRooms: RecyclerView = itemView.findViewById(R.id.rvSuggestedRooms)
        val scrollQuickReplies: HorizontalScrollView = itemView.findViewById(R.id.scrollQuickReplies)
        val chipGroupQuickReplies: ChipGroup = itemView.findViewById(R.id.chipGroupQuickReplies)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ai_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messageList[position]

        val dateFormat = SimpleDateFormat("HH:mm - dd/MM", Locale.getDefault())
        val timeString = dateFormat.format(Date(message.timestamp))

        val markwon = Markwon.builder(holder.itemView.context)
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                    builder.linkResolver { view, link ->
                        try {
                            if (link.startsWith("app://room")) {
                                val uri = Uri.parse(link)
                                val roomId = uri.getQueryParameter("roomId")
                                val userId = uri.getQueryParameter("userId")
                                if (!roomId.isNullOrEmpty() && !userId.isNullOrEmpty()) {
                                    val intent = Intent(view.context, RoomDetailActivity::class.java).apply {
                                        putExtra("roomId", roomId)
                                        putExtra("userId", userId)
                                    }
                                    view.context.startActivity(intent)
                                }
                            } else {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                                view.context.startActivity(intent)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                override fun configureTheme(builder: MarkwonTheme.Builder) {
                    builder.linkColor(0xFF0084FF.toInt())
                }
            })
            .build()

        if (message.role == "user") {
            holder.layoutUserFull.visibility = View.VISIBLE
            holder.layoutAIFull.visibility = View.GONE
            holder.tvUserMessage.text = message.content
            holder.tvUserTime.text = timeString
            holder.rvSuggestedRooms.visibility = View.GONE
            holder.scrollQuickReplies.visibility = View.GONE
        } else {
            holder.layoutUserFull.visibility = View.GONE
            holder.layoutAIFull.visibility = View.VISIBLE

            if (message.role == "typing") {
                holder.tvAITime.visibility = View.GONE
                holder.tvAIName.text = "AI is typing..."
                holder.tvAIMessage.text = "..."
                holder.rvSuggestedRooms.visibility = View.GONE
                holder.scrollQuickReplies.visibility = View.GONE
            } else {
                holder.tvAITime.visibility = View.VISIBLE
                holder.tvAITime.text = timeString
                holder.tvAIName.text = "Trợ lý hỗ trợ tìm kiếm phòng trọ"

                val hasText = message.content.trim().isNotEmpty()
                val hasRooms = message.suggestedRooms.isNotEmpty()

                if (!hasText && !hasRooms) {
                    holder.layoutAIFull.visibility = View.GONE
                    return
                }

                holder.layoutAIFull.visibility = View.VISIBLE

                if (hasText) {
                    holder.itemView.findViewById<View>(R.id.layoutAI).visibility = View.VISIBLE
                    holder.tvAIMessage.visibility = View.VISIBLE
                    markwon.setMarkdown(holder.tvAIMessage, message.content.trim())
                } else {
                    holder.itemView.findViewById<View>(R.id.layoutAI).visibility = View.GONE
                }

                if (hasRooms) {
                    holder.rvSuggestedRooms.visibility = View.VISIBLE
                    holder.rvSuggestedRooms.layoutManager = LinearLayoutManager(holder.itemView.context, LinearLayoutManager.VERTICAL, false)
                    holder.rvSuggestedRooms.isNestedScrollingEnabled = false
                    holder.rvSuggestedRooms.adapter = AIRoomAdapter(message.suggestedRooms)
                } else {
                    holder.rvSuggestedRooms.visibility = View.GONE
                }

                if (message.quickReplies.isNotEmpty()) {
                    holder.scrollQuickReplies.visibility = View.VISIBLE
                    holder.chipGroupQuickReplies.removeAllViews()
                    val context = holder.itemView.context
                    val inflater = LayoutInflater.from(context)
                    for (replyText in message.quickReplies) {
                        val suggestionView = inflater.inflate(R.layout.item_chat_suggestion, holder.chipGroupQuickReplies, false) as TextView
                        suggestionView.text = replyText
                        suggestionView.setOnClickListener {
                            quickReplyListener?.onQuickReplyClicked(replyText)
                        }
                        holder.chipGroupQuickReplies.addView(suggestionView)
                    }
                } else {
                    holder.scrollQuickReplies.visibility = View.GONE
                }
            }
        }
    }

    override fun getItemCount(): Int = messageList.size
}
