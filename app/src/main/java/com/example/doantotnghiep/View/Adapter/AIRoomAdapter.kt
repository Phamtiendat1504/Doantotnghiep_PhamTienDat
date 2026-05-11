package com.example.doantotnghiep.View.Adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doantotnghiep.Model.AIRoom
import com.example.doantotnghiep.R
import com.example.doantotnghiep.View.Auth.RoomDetailActivity
import java.text.NumberFormat
import java.util.Locale

class AIRoomAdapter(private val roomList: List<AIRoom>) : 
    RecyclerView.Adapter<AIRoomAdapter.RoomViewHolder>() {

    class RoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgRoom: ImageView = itemView.findViewById(R.id.imgRoom)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val tvInfo: TextView = itemView.findViewById(R.id.tvInfo)
        val btnViewDetail: View = itemView.findViewById(R.id.btnViewDetail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ai_room_card, parent, false)
        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val room = roomList[position]
        
        holder.tvTitle.text = room.title
        
        val format = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        holder.tvPrice.text = "${format.format(room.price)} VNĐ"
        holder.tvInfo.text = "${room.area}m² • ${room.district}"

        if (room.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(room.imageUrl)
                .centerCrop()
                .placeholder(R.color.gray_200)
                .into(holder.imgRoom)
        }

        val clickListener = View.OnClickListener {
            val intent = Intent(holder.itemView.context, RoomDetailActivity::class.java).apply {
                putExtra("roomId", room.id)
                putExtra("userId", room.userId)
            }
            holder.itemView.context.startActivity(intent)
        }

        holder.itemView.setOnClickListener(clickListener)
        holder.btnViewDetail.setOnClickListener(clickListener)
    }

    override fun getItemCount(): Int {
        return roomList.size
    }
}