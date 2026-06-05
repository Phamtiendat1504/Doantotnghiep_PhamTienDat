package com.example.doantotnghiep.View.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doantotnghiep.ViewModel.SavedPostsViewModel.SavedPost
import com.example.doantotnghiep.databinding.ItemSavedPostBinding
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class SavedPostsAdapter(
    private val onItemClick: (SavedPost) -> Unit,
    private val onRemoveClick: (SavedPost) -> Unit
) : ListAdapter<SavedPost, SavedPostsAdapter.SavedPostViewHolder>(SavedPostDiffCallback()) {

    private val formatter: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN"))
        symbols.groupingSeparator = '.'
        DecimalFormat("#,###", symbols)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedPostViewHolder {
        val binding = ItemSavedPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SavedPostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SavedPostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SavedPostViewHolder(
        private val binding: ItemSavedPostBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: SavedPost) {
            binding.apply {
                tvTitle.text = post.title
                
                tvAddress.text = listOf(post.address, post.ward, post.district)
                    .filter { it.isNotBlank() }.distinct().joinToString(", ")
                tvPrice.text = "${formatter.format(post.price)} đ/tháng"

                if (post.imageUrl.isNotEmpty()) {
                    Glide.with(itemView.context)
                        .load(post.imageUrl)
                        .centerCrop()
                        .into(imgPost)
                } else {
                    imgPost.setImageResource(android.R.color.darker_gray)
                }

                // Reset click và alpha cho nút xóa
                tvRemove.isClickable = true
                tvRemove.alpha = 1.0f
                tvRemove.setOnClickListener {
                    tvRemove.isClickable = false
                    tvRemove.alpha = 0.4f
                    onRemoveClick(post)
                }

                root.setOnClickListener {
                    onItemClick(post)
                }
            }
        }
    }

    class SavedPostDiffCallback : DiffUtil.ItemCallback<SavedPost>() {
        override fun areItemsTheSame(oldItem: SavedPost, newItem: SavedPost): Boolean {
            return oldItem.savedDocId == newItem.savedDocId
        }

        override fun areContentsTheSame(oldItem: SavedPost, newItem: SavedPost): Boolean {
            return oldItem == newItem
        }
    }
}
