package com.example.doantotnghiep.View.Adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doantotnghiep.R
import com.example.doantotnghiep.View.Auth.RoomDetailActivity
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RoomItem(
    val id: String,
    val title: String,
    val price: Long,
    val ward: String,
    val district: String,
    val area: Int,
    val imageUrl: String?,
    val isAvailable: Boolean = true,
    val createdAt: Long = 0L
)

class RoomAdapter(
    private val items: MutableList<RoomItem> = mutableListOf(),
    private val viewType: Int = VIEW_TYPE_HORIZONTAL,
    private val showAvailabilityBadge: Boolean = false
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_HORIZONTAL = 0
        const val VIEW_TYPE_VERTICAL = 1
    }

    private val formatter: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN"))
        symbols.groupingSeparator = '.'
        DecimalFormat("#,###", symbols)
    }
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))

    inner class HorizontalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgRoom: ImageView = itemView.findViewById(R.id.imgRoom)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
    }

    inner class VerticalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgRoom: ImageView = itemView.findViewById(R.id.imgRoom)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        val tvPostedDate: TextView = itemView.findViewById(R.id.tvPostedDate)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val tvArea: TextView = itemView.findViewById(R.id.tvArea)
        val tvRoomStatus: TextView = itemView.findViewById(R.id.tvRoomStatus)
    }

    override fun getItemViewType(position: Int) = viewType

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HORIZONTAL -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_room_featured, parent, false)
                HorizontalViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_room_new, parent, false)
                VerticalViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]

        when (holder) {
            is HorizontalViewHolder -> {
                val imageUrl = item.imageUrl ?: ""
                if (imageUrl.isNotEmpty()) {
                    Glide.with(holder.itemView.context)
                        .load(imageUrl)
                        .placeholder(R.color.gray_300)
                        .error(R.color.gray_300)
                        .centerCrop()
                        .into(holder.imgRoom)
                }
                holder.tvPrice.text = "${formatter.format(item.price)} đ/th"
                holder.tvTitle.text = item.title
                holder.tvLocation.text = "${item.ward}, ${item.district}"
            }
            is VerticalViewHolder -> {
                val imageUrl = item.imageUrl ?: ""
                if (imageUrl.isNotEmpty()) {
                    Glide.with(holder.itemView.context)
                        .load(imageUrl)
                        .placeholder(R.color.gray_300)
                        .error(R.color.gray_300)
                        .centerCrop()
                        .into(holder.imgRoom)
                }
                holder.tvTitle.text = item.title
                holder.tvLocation.text = "${item.ward}, ${item.district}"
                holder.tvPrice.text = "${formatter.format(item.price)} đ/tháng"
                if (item.createdAt > 0L) {
                    holder.tvPostedDate.text = "Đăng ngày ${dateFormatter.format(Date(item.createdAt))}"
                    holder.tvPostedDate.visibility = View.VISIBLE
                } else {
                    holder.tvPostedDate.visibility = View.GONE
                }

                if (item.area > 0) {
                    holder.tvArea.text = "${item.area} m²"
                    holder.tvArea.visibility = View.VISIBLE
                } else {
                    holder.tvArea.visibility = View.GONE
                }

                if (showAvailabilityBadge && item.isAvailable) {
                    holder.tvRoomStatus.visibility = View.VISIBLE
                    holder.tvRoomStatus.text = "Còn phòng"
                } else {
                    holder.tvRoomStatus.visibility = View.GONE
                }
            }
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, RoomDetailActivity::class.java)
            intent.putExtra("roomId", item.id)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = items.size

    fun getItems(): List<RoomItem> = items.toList()

    fun submitList(newItems: List<RoomItem>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = newItems.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) = items[oldPos].id == newItems[newPos].id
            override fun areContentsTheSame(oldPos: Int, newPos: Int) = items[oldPos] == newItems[newPos]
        })
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }
}
