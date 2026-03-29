package com.example.doantotnghiep.View.Auth

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.example.doantotnghiep.R
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyPostsActivity : AppCompatActivity() {

    private lateinit var layoutPosts: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var btnBack: ImageView
    private lateinit var chipAll: Chip
    private lateinit var chipPending: Chip
    private lateinit var chipApproved: Chip
    private lateinit var chipRejected: Chip

    private val db = FirebaseFirestore.getInstance()
    private val formatter: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN"))
        symbols.groupingSeparator = '.'
        DecimalFormat("#,###", symbols)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_posts)

        layoutPosts = findViewById(R.id.layoutPosts)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)
        btnBack = findViewById(R.id.btnBack)
        chipAll = findViewById(R.id.chipAll)
        chipPending = findViewById(R.id.chipPending)
        chipApproved = findViewById(R.id.chipApproved)
        chipRejected = findViewById(R.id.chipRejected)

        btnBack.setOnClickListener { finish() }

        // Tab lọc
        chipAll.setOnClickListener { clearChips(); chipAll.isChecked = true; loadPosts("all") }
        chipPending.setOnClickListener { clearChips(); chipPending.isChecked = true; loadPosts("pending") }
        chipApproved.setOnClickListener { clearChips(); chipApproved.isChecked = true; loadPosts("approved") }
        chipRejected.setOnClickListener { clearChips(); chipRejected.isChecked = true; loadPosts("rejected") }

        loadPosts("all")
    }

    private fun clearChips() {
        chipAll.isChecked = false
        chipPending.isChecked = false
        chipApproved.isChecked = false
        chipRejected.isChecked = false
    }

    private fun loadPosts(filter: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        layoutPosts.removeAllViews()
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE

        var query = db.collection("rooms")
            .whereEqualTo("userId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        if (filter != "all") {
            query = db.collection("rooms")
                .whereEqualTo("userId", uid)
                .whereEqualTo("status", filter)
                .orderBy("createdAt", Query.Direction.DESCENDING)
        }

        query.get()
            .addOnSuccessListener { documents ->
                progressBar.visibility = View.GONE
                layoutPosts.removeAllViews()

                if (documents.isEmpty) {
                    tvEmpty.visibility = View.VISIBLE
                    layoutPosts.addView(tvEmpty)
                    return@addOnSuccessListener
                }

                for (doc in documents) {
                    val title = doc.getString("title") ?: "Chưa có tiêu đề"
                    val price = doc.getLong("price") ?: 0
                    val ward = doc.getString("ward") ?: ""
                    val district = doc.getString("district") ?: ""
                    val area = doc.getLong("area")?.toInt() ?: 0
                    val status = doc.getString("status") ?: "pending"
                    val rejectReason = doc.getString("rejectReason") ?: ""
                    val imageUrls = doc.get("imageUrls") as? List<String> ?: listOf()
                    val createdAt = doc.getLong("createdAt") ?: 0

                    val card = createPostCard(
                        doc.id, title, price, ward, district, area,
                        status, rejectReason, imageUrls, createdAt
                    )
                    layoutPosts.addView(card)
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                tvEmpty.text = "Lỗi tải dữ liệu"
                tvEmpty.visibility = View.VISIBLE
                layoutPosts.addView(tvEmpty)
            }
    }

    private fun createPostCard(
        docId: String, title: String, price: Long, ward: String,
        district: String, area: Int, status: String, rejectReason: String,
        imageUrls: List<String>, createdAt: Long
    ): CardView {
        val card = CardView(this)
        val cardParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        cardParams.bottomMargin = dpToPx(12)
        card.layoutParams = cardParams
        card.radius = dpToPx(14).toFloat()
        card.cardElevation = dpToPx(3).toFloat()
        card.setCardBackgroundColor(0xFFFFFFFF.toInt())

        val mainLayout = LinearLayout(this)
        mainLayout.orientation = LinearLayout.VERTICAL

        // ═══ Phần trên: ảnh + thông tin ═══
        val topLayout = LinearLayout(this)
        topLayout.orientation = LinearLayout.HORIZONTAL
        topLayout.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(10))

        // Ảnh
        val imgView = ImageView(this)
        val imgParams = LinearLayout.LayoutParams(dpToPx(100), dpToPx(85))
        imgView.layoutParams = imgParams
        imgView.scaleType = ImageView.ScaleType.CENTER_CROP
        imgView.setBackgroundColor(0xFFE0E0E0.toInt())

        if (imageUrls.isNotEmpty()) {
            Glide.with(this)
                .load(imageUrls[0])
                .centerCrop()
                .into(imgView)
        }

        // Bo góc ảnh
        imgView.clipToOutline = true
        imgView.outlineProvider = object : android.view.ViewOutlineProvider() {
            override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, dpToPx(10).toFloat())
            }
        }

        topLayout.addView(imgView)

        // Thông tin
        val infoLayout = LinearLayout(this)
        infoLayout.orientation = LinearLayout.VERTICAL
        val infoParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        infoParams.marginStart = dpToPx(12)
        infoLayout.layoutParams = infoParams

        val tvTitle = TextView(this)
        tvTitle.text = title
        tvTitle.textSize = 15f
        tvTitle.setTextColor(0xFF1A1A2E.toInt())
        tvTitle.setTypeface(tvTitle.typeface, android.graphics.Typeface.BOLD)
        tvTitle.maxLines = 2
        infoLayout.addView(tvTitle)

        val tvLocation = TextView(this)
        tvLocation.text = "$ward, $district"
        tvLocation.textSize = 12f
        tvLocation.setTextColor(0xFF999999.toInt())
        tvLocation.setPadding(0, dpToPx(3), 0, 0)
        infoLayout.addView(tvLocation)

        val tvArea = TextView(this)
        tvArea.text = "${area}m²"
        tvArea.textSize = 12f
        tvArea.setTextColor(0xFF666666.toInt())
        tvArea.setPadding(0, dpToPx(2), 0, 0)
        infoLayout.addView(tvArea)

        val tvPrice = TextView(this)
        tvPrice.text = "${formatter.format(price)} đ/tháng"
        tvPrice.textSize = 15f
        tvPrice.setTextColor(0xFFD32F2F.toInt())
        tvPrice.setTypeface(tvPrice.typeface, android.graphics.Typeface.BOLD)
        tvPrice.setPadding(0, dpToPx(4), 0, 0)
        infoLayout.addView(tvPrice)

        topLayout.addView(infoLayout)
        mainLayout.addView(topLayout)

        // ═══ Đường kẻ ═══
        val divider = View(this)
        divider.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)
        )
        divider.setBackgroundColor(0xFFF0F0F0.toInt())
        mainLayout.addView(divider)

        // ═══ Phần dưới: trạng thái + ngày + nút ═══
        val bottomLayout = LinearLayout(this)
        bottomLayout.orientation = LinearLayout.HORIZONTAL
        bottomLayout.gravity = android.view.Gravity.CENTER_VERTICAL
        bottomLayout.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))

        // Badge trạng thái
        val tvStatus = TextView(this)
        when (status) {
            "pending" -> {
                tvStatus.text = "⏳ Chờ duyệt"
                tvStatus.setTextColor(0xFFE65100.toInt())
                tvStatus.setBackgroundResource(R.drawable.bg_badge_pending)
            }
            "approved" -> {
                tvStatus.text = "✓ Đã duyệt"
                tvStatus.setTextColor(0xFF2E7D32.toInt())
                tvStatus.setBackgroundResource(R.drawable.bg_badge_landlord)
            }
            "rejected" -> {
                tvStatus.text = "✗ Từ chối"
                tvStatus.setTextColor(0xFFD32F2F.toInt())
                tvStatus.setBackgroundResource(R.drawable.bg_badge_pending)
            }
        }
        tvStatus.textSize = 12f
        tvStatus.setTypeface(tvStatus.typeface, android.graphics.Typeface.BOLD)
        tvStatus.setPadding(dpToPx(12), dpToPx(4), dpToPx(12), dpToPx(4))
        bottomLayout.addView(tvStatus)

        // Ngày đăng
        val tvDate = TextView(this)
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))
        tvDate.text = sdf.format(Date(createdAt))
        tvDate.textSize = 11f
        tvDate.setTextColor(0xFF999999.toInt())
        tvDate.setPadding(dpToPx(10), 0, 0, 0)
        val dateParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        tvDate.layoutParams = dateParams
        bottomLayout.addView(tvDate)

        // Nút xóa
        val btnDelete = TextView(this)
        btnDelete.text = "Xóa"
        btnDelete.textSize = 13f
        btnDelete.setTextColor(0xFFD32F2F.toInt())
        btnDelete.setTypeface(btnDelete.typeface, android.graphics.Typeface.BOLD)
        btnDelete.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
        btnDelete.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xóa bài đăng")
                .setMessage("Bạn có chắc chắn muốn xóa bài đăng này?")
                .setPositiveButton("Xóa") { _, _ ->
                    deletePost(docId)
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
        bottomLayout.addView(btnDelete)

        mainLayout.addView(bottomLayout)

        // ═══ Lý do từ chối (nếu có) ═══
        if (status == "rejected" && rejectReason.isNotEmpty()) {
            val rejectLayout = LinearLayout(this)
            rejectLayout.orientation = LinearLayout.HORIZONTAL
            rejectLayout.setPadding(dpToPx(12), 0, dpToPx(12), dpToPx(10))
            rejectLayout.gravity = android.view.Gravity.CENTER_VERTICAL

            val iconReject = TextView(this)
            iconReject.text = "⚠️"
            iconReject.textSize = 13f
            rejectLayout.addView(iconReject)

            val tvReason = TextView(this)
            tvReason.text = "Lý do: $rejectReason"
            tvReason.textSize = 12f
            tvReason.setTextColor(0xFFD32F2F.toInt())
            tvReason.setPadding(dpToPx(6), 0, 0, 0)
            rejectLayout.addView(tvReason)

            mainLayout.addView(rejectLayout)
        }

        card.addView(mainLayout)
        return card
    }

    private fun deletePost(docId: String) {
        db.collection("rooms").document(docId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Đã xóa bài đăng", Toast.LENGTH_SHORT).show()
                // Reload danh sách
                when {
                    chipPending.isChecked -> loadPosts("pending")
                    chipApproved.isChecked -> loadPosts("approved")
                    chipRejected.isChecked -> loadPosts("rejected")
                    else -> loadPosts("all")
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Xóa thất bại: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}