package com.example.doantotnghiep.View.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doantotnghiep.R

data class UserSearchItem(
    val uid: String,
    val fullName: String,
    val avatarUrl: String,
    val roomCount: Int,
    val isVerified: Boolean
)

class UserSearchAdapter(
    private val onClick: (UserSearchItem) -> Unit
) : ListAdapter<UserSearchItem, UserSearchAdapter.UserViewHolder>(Diff) {

    object Diff : DiffUtil.ItemCallback<UserSearchItem>() {
        override fun areItemsTheSame(oldItem: UserSearchItem, newItem: UserSearchItem): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: UserSearchItem, newItem: UserSearchItem): Boolean {
            return oldItem == newItem
        }
    }

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivAvatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        val tvRoomCount: TextView = itemView.findViewById(R.id.tvRoomCount)
        val tvVerifiedBadge: TextView = itemView.findViewById(R.id.tvVerifiedBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_search, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val item = getItem(position)
        holder.tvUserName.text = item.fullName
        holder.tvRoomCount.text = if (item.roomCount > 0) {
            "${item.roomCount} bài đăng đang hiển thị"
        } else {
            "Chưa có bài đăng"
        }
        holder.tvVerifiedBadge.visibility = if (item.isVerified) View.VISIBLE else View.GONE

        if (item.avatarUrl.isNotEmpty()) {
            Glide.with(holder.itemView)
                .load(item.avatarUrl)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(holder.ivAvatar)
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_person)
        }

        holder.itemView.setOnClickListener { onClick(item) }
    }
}
