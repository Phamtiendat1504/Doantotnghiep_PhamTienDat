package com.example.doantotnghiep.View.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doantotnghiep.R
import com.google.firebase.firestore.DocumentSnapshot
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyPostsAdapter(
    private var items: List<DocumentSnapshot> = emptyList(),
    private val listener: OnPostActionListener
) : RecyclerView.Adapter<MyPostsAdapter.PostViewHolder>() {

    interface OnPostActionListener {
        fun onItemClick(docId: String)
        fun onDeleteClick(docId: String)
        fun onFeaturedClick(docId: String, title: String)
        fun onEditClick(docId: String)
        fun onRenewClick(docId: String)
    }

    private val formatter: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN"))
        symbols.groupingSeparator = '.'
        DecimalFormat("#,###", symbols)
    }

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgRoom: ImageView = itemView.findViewById(R.id.imgRoom)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        val tvArea: TextView = itemView.findViewById(R.id.tvArea)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val tvFeaturedStatus: TextView = itemView.findViewById(R.id.tvFeaturedStatus)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        
        val btnDelete: TextView = itemView.findViewById(R.id.btnDelete)
        val btnFeatured: TextView = itemView.findViewById(R.id.btnFeatured)
        val btnEditPending: TextView = itemView.findViewById(R.id.btnEditPending)
        
        val layoutRejectReason: LinearLayout = itemView.findViewById(R.id.layoutRejectReason)
        val tvRejectReason: TextView = itemView.findViewById(R.id.tvRejectReason)
        val btnEditAndRepublish: TextView = itemView.findViewById(R.id.btnEditAndRepublish)
        
        val layoutExpiredWarning: LinearLayout = itemView.findViewById(R.id.layoutExpiredWarning)
        val tvExpiredWarning: TextView = itemView.findViewById(R.id.tvExpiredWarning)
        val btnRenew: TextView = itemView.findViewById(R.id.btnRenew)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_my_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val doc = items[position]
        val docId = doc.id
        val title = doc.getString("title") ?: "Chưa có tiêu đề"
        val price = doc.getLong("price") ?: 0L
        val ward = doc.getString("ward") ?: ""
        val district = doc.getString("district") ?: ""
        val area = doc.getLong("area")?.toInt() ?: 0
        val status = doc.getString("status") ?: "pending"
        val rejectReason = doc.getString("rejectReason") ?: ""
        val imageUrls = (doc.get("imageUrls") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        val createdAt = when (val raw = doc.get("createdAt")) {
            is Number -> raw.toLong()
            is com.google.firebase.Timestamp -> raw.toDate().time
            else -> 0L
        }
        val isFeatured = doc.getBoolean("isFeatured") == true
        val featuredUntil = doc.getLong("featuredUntil") ?: 0L
        val featuredRequestStatus = doc.getString("featuredRequestStatus") ?: ""

        // Bind image with rounded corners using Glide
        if (imageUrls.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(imageUrls[0])
                .centerCrop()
                .placeholder(R.color.gray_200)
                .error(R.color.gray_200)
                .into(holder.imgRoom)
        } else {
            holder.imgRoom.setImageResource(R.color.gray_200)
        }

        // Apply programmatic rounding to imgRoom outline for smooth card UI
        holder.imgRoom.clipToOutline = true
        holder.imgRoom.outlineProvider = object : android.view.ViewOutlineProvider() {
            override fun getOutline(view: View, outline: android.graphics.Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, (10 * view.resources.displayMetrics.density).toFloat())
            }
        }

        // Bind basic info
        holder.tvTitle.text = title
        holder.tvLocation.text = listOf(ward, district).filter { it.isNotBlank() }.distinct().joinToString(", ")
        
        if (area > 0) {
            holder.tvArea.text = "${area}m²"
            holder.tvArea.visibility = View.VISIBLE
        } else {
            holder.tvArea.visibility = View.GONE
        }
        
        holder.tvPrice.text = "${formatter.format(price)} đ/tháng"

        // Bind featured status badge
        val now = System.currentTimeMillis()
        if (isFeatured && featuredUntil > now) {
            holder.tvFeaturedStatus.text = "Đang nổi bật đến ${dateFormatter.format(Date(featuredUntil))}"
            holder.tvFeaturedStatus.setTextColor(0xFFE65100.toInt())
            holder.tvFeaturedStatus.visibility = View.VISIBLE
        } else if (featuredRequestStatus == "waiting_for_payment" || featuredRequestStatus == "paid" || featuredRequestStatus == "paid_waiting_admin") {
            holder.tvFeaturedStatus.text = if (featuredRequestStatus == "paid" || featuredRequestStatus == "paid_waiting_admin") {
                "Đã thanh toán, chờ admin duyệt nổi bật"
            } else {
                "Có yêu cầu nổi bật đang chờ thanh toán"
            }
            holder.tvFeaturedStatus.setTextColor(0xFF1976D2.toInt())
            holder.tvFeaturedStatus.visibility = View.VISIBLE
        } else if (featuredRequestStatus == "rejected") {
            val rejectReason = doc.getString("featuredRequestRejectReason") ?: ""
            holder.tvFeaturedStatus.text = if (rejectReason.isNotBlank()) "Nổi bật bị từ chối: $rejectReason" else "Yêu cầu nổi bật bị từ chối"
            holder.tvFeaturedStatus.setTextColor(0xFFD32F2F.toInt())
            holder.tvFeaturedStatus.visibility = View.VISIBLE
        } else {
            holder.tvFeaturedStatus.visibility = View.GONE
        }

        // Bind post status badge
        when (status) {
            "pending" -> {
                holder.tvStatus.text = "Chờ duyệt"
                holder.tvStatus.setTextColor(0xFFE65100.toInt())
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_pending)
            }
            "approved" -> {
                holder.tvStatus.text = "Đã duyệt"
                holder.tvStatus.setTextColor(0xFFFFFFFF.toInt())
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_approved)
            }
            "rejected" -> {
                holder.tvStatus.text = "Từ chối"
                holder.tvStatus.setTextColor(0xFFD32F2F.toInt())
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_pending)
            }
            "rented" -> {
                holder.tvStatus.text = "Đã cho thuê"
                holder.tvStatus.setTextColor(0xFFFFFFFF.toInt())
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_approved)
            }
        }

        // Bind date
        holder.tvDate.text = dateFormatter.format(Date(createdAt))

        // Action visibility logic
        // 1. Delete click callback
        holder.btnDelete.setOnClickListener {
            AlertDialog.Builder(holder.itemView.context)
                .setTitle("Xóa bài đăng")
                .setMessage("Bạn có chắc chắn muốn xóa bài đăng này? Mọi thông tin và hình ảnh sẽ bị xóa hoàn toàn khỏi hệ thống.")
                .setPositiveButton("Xóa") { _, _ -> listener.onDeleteClick(docId) }
                .setNegativeButton("Hủy", null)
                .show()
        }

        // 2. Featured click callback
        val isApproved = status == "approved"
        val isPendingWithRejectedFeatured = status == "pending" && featuredRequestStatus == "rejected"
        if (isApproved || isPendingWithRejectedFeatured) {
            val canBuyFeatured = !(isFeatured && featuredUntil > now) &&
                featuredRequestStatus != "paid" &&
                featuredRequestStatus != "paid_waiting_admin"
            if (canBuyFeatured) {
                holder.btnFeatured.text = when (featuredRequestStatus) {
                    "waiting_for_payment" -> "Xem giao dịch"
                    "rejected" -> "Sửa & Gửi lại"
                    else -> "Đẩy nổi bật"
                }
                holder.btnFeatured.visibility = View.VISIBLE
                holder.btnFeatured.setOnClickListener { listener.onFeaturedClick(docId, title) }
            } else {
                holder.btnFeatured.visibility = View.GONE
            }
        } else {
            holder.btnFeatured.visibility = View.GONE
        }

        // 3. Edit pending click callback
        if (status == "pending") {
            holder.btnEditPending.visibility = View.VISIBLE
            holder.btnEditPending.setOnClickListener { listener.onEditClick(docId) }
        } else {
            holder.btnEditPending.visibility = View.GONE
        }

        // 4. Rejected layout & edit and republish click callback
        if (status == "rejected" && rejectReason.isNotEmpty()) {
            holder.layoutRejectReason.visibility = View.VISIBLE
            holder.tvRejectReason.text = "Lý do: $rejectReason"
            holder.btnEditAndRepublish.visibility = View.VISIBLE
            holder.btnEditAndRepublish.setOnClickListener { listener.onEditClick(docId) }
        } else {
            holder.layoutRejectReason.visibility = View.GONE
            holder.btnEditAndRepublish.visibility = View.GONE
        }

        // 5. Rented warning & renew click callback
        if (status == "rented") {
            holder.layoutExpiredWarning.visibility = View.VISIBLE
            holder.tvExpiredWarning.text = "Bài đăng đang ở trạng thái đã cho thuê. Bấm 'Đăng lại' để gửi yêu cầu duyệt lại bài đăng khi phòng trống."
            holder.btnRenew.text = "Đăng lại"
            holder.btnRenew.visibility = View.VISIBLE
            holder.btnRenew.setOnClickListener { listener.onRenewClick(docId) }
        } else {
            holder.layoutExpiredWarning.visibility = View.GONE
            holder.btnRenew.visibility = View.GONE
        }

        // Item click callback
        holder.itemView.setOnClickListener {
            listener.onItemClick(docId)
        }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<DocumentSnapshot>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = newItems.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) = items[oldPos].id == newItems[newPos].id
            override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
                val old = items[oldPos]
                val new = newItems[newPos]
                return old.getString("status") == new.getString("status") &&
                    old.getString("title") == new.getString("title") &&
                    old.getLong("price") == new.getLong("price") &&
                    old.getBoolean("isFeatured") == new.getBoolean("isFeatured") &&
                    old.getLong("featuredUntil") == new.getLong("featuredUntil") &&
                    old.getString("featuredRequestStatus") == new.getString("featuredRequestStatus") &&
                    old.getString("rejectReason") == new.getString("rejectReason")
            }
        })
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }
}
