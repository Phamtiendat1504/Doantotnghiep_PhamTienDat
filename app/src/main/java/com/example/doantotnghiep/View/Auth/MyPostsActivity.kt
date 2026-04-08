package com.example.doantotnghiep.View.Auth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.doantotnghiep.R
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Intent
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.ViewModel.MyPostsViewModel

class MyPostsActivity : AppCompatActivity() {

    private lateinit var layoutPosts: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var btnBack: ImageView
    private lateinit var chipAll: Chip
    private lateinit var chipPending: Chip
    private lateinit var chipApproved: Chip
    private lateinit var chipRejected: Chip
    private lateinit var chipExpired: Chip
    private lateinit var edtSearchPost: EditText
    private lateinit var btnClearSearch: ImageView

    private lateinit var viewModel: MyPostsViewModel
    private var allPosts = listOf<DocumentSnapshot>()
    private var currentFilter = "all"

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
        chipExpired = findViewById(R.id.chipExpired)
        edtSearchPost = findViewById(R.id.edtSearchPost)
        btnClearSearch = findViewById(R.id.btnClearSearch)

        btnBack.setOnClickListener { finish() }

        viewModel = ViewModelProvider(this)[MyPostsViewModel::class.java]

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) { layoutPosts.removeAllViews(); tvEmpty.visibility = View.GONE }
        }

        viewModel.posts.observe(this) { posts ->
            allPosts = posts
            applySearchFilter(edtSearchPost.text.toString().trim())
        }

        viewModel.errorMessage.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                tvEmpty.text = "Lỗi tải dữ liệu"
                tvEmpty.visibility = View.VISIBLE
            }
        }

        viewModel.renewResult.observe(this) { success ->
            if (success == true) {
                viewModel.resetRenewResult()
                MessageUtils.showSuccessDialog(
                    this, "Gia hạn thành công",
                    "Bài đăng đã được gia hạn thêm 2 tháng và hiển thị trở lại trên kết quả tìm kiếm."
                ) { refreshList() }
            }
        }

        val uid = viewModel.getCurrentUserId()
        if (uid != null) viewModel.markPostsAsSeen(uid, this)

        chipAll.setOnClickListener { clearChips(); chipAll.isChecked = true; viewModel.loadPosts("all") }
        chipPending.setOnClickListener { clearChips(); chipPending.isChecked = true; viewModel.loadPosts("pending") }
        chipApproved.setOnClickListener { clearChips(); chipApproved.isChecked = true; viewModel.loadPosts("approved") }
        chipRejected.setOnClickListener { clearChips(); chipRejected.isChecked = true; viewModel.loadPosts("rejected") }
        chipExpired.setOnClickListener { clearChips(); chipExpired.isChecked = true; viewModel.loadPosts("expired") }

        edtSearchPost.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val keyword = s?.toString()?.trim() ?: ""
                btnClearSearch.visibility = if (keyword.isNotEmpty()) View.VISIBLE else View.GONE
                applySearchFilter(keyword)
            }
        })
        btnClearSearch.setOnClickListener { edtSearchPost.setText("") }

        viewModel.loadPosts("all")
    }

    private fun clearChips() {
        chipAll.isChecked = false; chipPending.isChecked = false
        chipApproved.isChecked = false; chipRejected.isChecked = false; chipExpired.isChecked = false
    }

    private fun applySearchFilter(keyword: String) {
        layoutPosts.removeAllViews()
        val filtered = if (keyword.isEmpty()) allPosts
        else allPosts.filter { (it.getString("title") ?: "").contains(keyword, ignoreCase = true) }

        if (filtered.isEmpty()) {
            tvEmpty.text = if (keyword.isEmpty()) "Chưa có bài đăng nào" else "Không tìm thấy bài đăng nào"
            tvEmpty.visibility = View.VISIBLE
            layoutPosts.addView(tvEmpty)
            return
        }
        tvEmpty.visibility = View.GONE
        for (doc in filtered) {
            layoutPosts.addView(createPostCard(
                docId = doc.id,
                title = doc.getString("title") ?: "Chưa có tiêu đề",
                price = doc.getLong("price") ?: 0,
                ward = doc.getString("ward") ?: "",
                district = doc.getString("district") ?: "",
                area = doc.getLong("area")?.toInt() ?: 0,
                status = doc.getString("status") ?: "pending",
                rejectReason = doc.getString("rejectReason") ?: "",
                imageUrls = doc.get("imageUrls") as? List<String> ?: listOf(),
                createdAt = doc.getLong("createdAt") ?: 0
            ))
        }
    }

    private fun createPostCard(
        docId: String, title: String, price: Long, ward: String,
        district: String, area: Int, status: String, rejectReason: String,
        imageUrls: List<String>, createdAt: Long
    ): CardView {
        val card = CardView(this)
        val cardParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        cardParams.bottomMargin = dpToPx(12)
        card.layoutParams = cardParams
        card.radius = dpToPx(14).toFloat()
        card.cardElevation = dpToPx(3).toFloat()
        card.setCardBackgroundColor(0xFFFFFFFF.toInt())

        val mainLayout = LinearLayout(this)
        mainLayout.orientation = LinearLayout.VERTICAL

        val topLayout = LinearLayout(this)
        topLayout.orientation = LinearLayout.HORIZONTAL
        topLayout.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(10))

        val imgView = ImageView(this)
        imgView.layoutParams = LinearLayout.LayoutParams(dpToPx(100), dpToPx(85))
        imgView.scaleType = ImageView.ScaleType.CENTER_CROP
        imgView.setBackgroundColor(0xFFE0E0E0.toInt())
        imgView.clipToOutline = true
        imgView.outlineProvider = object : android.view.ViewOutlineProvider() {
            override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, dpToPx(10).toFloat())
            }
        }
        if (imageUrls.isNotEmpty()) Glide.with(this).load(imageUrls[0]).centerCrop().into(imgView)
        topLayout.addView(imgView)

        val infoLayout = LinearLayout(this)
        infoLayout.orientation = LinearLayout.VERTICAL
        val infoParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        infoParams.marginStart = dpToPx(12)
        infoLayout.layoutParams = infoParams

        infoLayout.addView(TextView(this).apply {
            text = title; textSize = 15f; setTextColor(0xFF1A1A2E.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD); maxLines = 2
        })
        infoLayout.addView(TextView(this).apply {
            text = "$ward, $district"; textSize = 12f; setTextColor(0xFF999999.toInt()); setPadding(0, dpToPx(3), 0, 0)
        })
        if (area > 0) infoLayout.addView(TextView(this).apply {
            text = "${area}m²"; textSize = 12f; setTextColor(0xFF666666.toInt()); setPadding(0, dpToPx(2), 0, 0)
        })
        infoLayout.addView(TextView(this).apply {
            text = "${formatter.format(price)} đ/tháng"; textSize = 15f; setTextColor(0xFFD32F2F.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD); setPadding(0, dpToPx(4), 0, 0)
        })
        topLayout.addView(infoLayout)
        mainLayout.addView(topLayout)

        mainLayout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1))
            setBackgroundColor(0xFFF0F0F0.toInt())
        })

        val bottomLayout = LinearLayout(this)
        bottomLayout.orientation = LinearLayout.HORIZONTAL
        bottomLayout.gravity = android.view.Gravity.CENTER_VERTICAL
        bottomLayout.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))

        val tvStatus = TextView(this)
        when (status) {
            "pending" -> { tvStatus.text = "⏳ Chờ duyệt"; tvStatus.setTextColor(0xFFE65100.toInt()); tvStatus.setBackgroundResource(R.drawable.bg_badge_pending) }
            "approved" -> { tvStatus.text = "✓ Đã duyệt"; tvStatus.setTextColor(0xFFFFFFFF.toInt()); tvStatus.background = android.graphics.drawable.GradientDrawable().apply { setColor(0xFF2E7D32.toInt()); cornerRadius = dpToPx(8).toFloat() } }
            "rejected" -> { tvStatus.text = "✗ Từ chối"; tvStatus.setTextColor(0xFFD32F2F.toInt()); tvStatus.setBackgroundResource(R.drawable.bg_badge_pending) }
            "expired" -> { tvStatus.text = "⏰ Hết hạn"; tvStatus.setTextColor(0xFFFFFFFF.toInt()); tvStatus.background = android.graphics.drawable.GradientDrawable().apply { setColor(0xFF757575.toInt()); cornerRadius = dpToPx(8).toFloat() } }
        }
        tvStatus.textSize = 12f; tvStatus.setTypeface(tvStatus.typeface, android.graphics.Typeface.BOLD)
        tvStatus.setPadding(dpToPx(12), dpToPx(4), dpToPx(12), dpToPx(4))
        bottomLayout.addView(tvStatus)

        val tvDate = TextView(this)
        tvDate.text = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN")).format(Date(createdAt))
        tvDate.textSize = 11f; tvDate.setTextColor(0xFF999999.toInt()); tvDate.setPadding(dpToPx(10), 0, 0, 0)
        tvDate.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        bottomLayout.addView(tvDate)

        val btnDelete = TextView(this).apply {
            text = "Xóa"; textSize = 13f; setTextColor(0xFFD32F2F.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD); setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
            setOnClickListener {
                androidx.appcompat.app.AlertDialog.Builder(this@MyPostsActivity)
                    .setTitle("Xóa bài đăng")
                    .setMessage("Bạn có chắc chắn muốn xóa bài đăng này? Mọi thông tin và hình ảnh sẽ bị xóa hoàn toàn khỏi hệ thống.")
                    .setPositiveButton("Xóa") { _, _ -> deletePost(docId) }
                    .setNegativeButton("Hủy", null).show()
            }
        }
        bottomLayout.addView(btnDelete)
        mainLayout.addView(bottomLayout)

        if (status == "rejected" && rejectReason.isNotEmpty()) {
            val rejectLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dpToPx(12), 0, dpToPx(12), dpToPx(10)); gravity = android.view.Gravity.CENTER_VERTICAL }
            rejectLayout.addView(TextView(this).apply { text = "⚠️"; textSize = 13f })
            rejectLayout.addView(TextView(this).apply { text = "Lý do: $rejectReason"; textSize = 12f; setTextColor(0xFFD32F2F.toInt()); setPadding(dpToPx(6), 0, 0, 0) })
            mainLayout.addView(rejectLayout)

            val btnEdit = TextView(this).apply {
                text = "✏️ Sửa và đăng lại"; textSize = 14f; setTextColor(0xFFFFFFFF.toInt())
                setTypeface(typeface, android.graphics.Typeface.BOLD); gravity = android.view.Gravity.CENTER
                setPadding(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(10))
                background = android.graphics.drawable.GradientDrawable().apply { setColor(0xFF1976D2.toInt()); cornerRadius = dpToPx(10).toFloat() }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dpToPx(12), 0, dpToPx(12), dpToPx(12)) }
                setOnClickListener { startActivity(Intent(this@MyPostsActivity, EditPostActivity::class.java).apply { putExtra("roomId", docId) }) }
            }
            mainLayout.addView(btnEdit)
        }

        if (status == "expired") {
            mainLayout.addView(TextView(this).apply {
                text = "⚠️ Bài đăng đã ẩn khỏi kết quả tìm kiếm sau 2 tháng. Bài sẽ tự động xóa sau 1 tháng nữa."
                textSize = 12f; setTextColor(0xFF795548.toInt()); setPadding(dpToPx(12), 0, dpToPx(12), dpToPx(8))
            })
            val actionRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dpToPx(12), 0, dpToPx(12), dpToPx(12)) }
            actionRow.addView(TextView(this).apply {
                text = "🔄 Gia hạn 2 tháng"; textSize = 13f; setTextColor(0xFFFFFFFF.toInt())
                setTypeface(typeface, android.graphics.Typeface.BOLD); gravity = android.view.Gravity.CENTER
                setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
                background = android.graphics.drawable.GradientDrawable().apply { setColor(0xFF1976D2.toInt()); cornerRadius = dpToPx(8).toFloat() }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dpToPx(6) }
                setOnClickListener { viewModel.renewPost(docId) }
            })
            actionRow.addView(TextView(this).apply {
                text = "✓ Đã cho thuê"; textSize = 13f; setTextColor(0xFFFFFFFF.toInt())
                setTypeface(typeface, android.graphics.Typeface.BOLD); gravity = android.view.Gravity.CENTER
                setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
                background = android.graphics.drawable.GradientDrawable().apply { setColor(0xFF388E3C.toInt()); cornerRadius = dpToPx(8).toFloat() }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dpToPx(6) }
                setOnClickListener {
                    androidx.appcompat.app.AlertDialog.Builder(this@MyPostsActivity)
                        .setTitle("Xác nhận đã cho thuê")
                        .setMessage("Bài đăng này sẽ bị xóa khỏi hệ thống. Bạn có chắc chắn phòng đã được cho thuê?")
                        .setPositiveButton("Đã cho thuê") { _, _ ->
                            progressBar.visibility = View.VISIBLE
                            viewModel.deletePost(docId,
                                onSuccess = {
                                    progressBar.visibility = View.GONE
                                    MessageUtils.showSuccessDialog(this@MyPostsActivity, "Đã xóa bài đăng", "Bài đăng đã được xóa khỏi hệ thống. Chúc mừng bạn đã cho thuê phòng!") { refreshList() }
                                },
                                onFailure = { error ->
                                    progressBar.visibility = View.GONE
                                    MessageUtils.showErrorDialog(this@MyPostsActivity, "Lỗi", error)
                                }
                            )
                        }
                        .setNegativeButton("Hủy", null).show()
                }
            })
            mainLayout.addView(actionRow)
        }

        card.addView(mainLayout)
        card.setOnClickListener {
            startActivity(Intent(this, MyPostDetailActivity::class.java).apply { putExtra("roomId", docId) })
        }
        return card
    }

    private fun deletePost(docId: String) {
        progressBar.visibility = View.VISIBLE
        viewModel.deletePost(docId,
            onSuccess = {
                progressBar.visibility = View.GONE
                MessageUtils.showSuccessDialog(this, "Đã xóa bài đăng", "Bài đăng của bạn và toàn bộ hình ảnh liên quan đã được xóa sạch khỏi hệ thống.") { refreshList() }
            },
            onFailure = { error ->
                progressBar.visibility = View.GONE
                MessageUtils.showErrorDialog(this, "Lỗi xóa bài", error)
            }
        )
    }

    private fun refreshList() {
        when {
            chipPending.isChecked -> viewModel.loadPosts("pending")
            chipApproved.isChecked -> viewModel.loadPosts("approved")
            chipRejected.isChecked -> viewModel.loadPosts("rejected")
            chipExpired.isChecked -> viewModel.loadPosts("expired")
            else -> viewModel.loadPosts("all")
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}