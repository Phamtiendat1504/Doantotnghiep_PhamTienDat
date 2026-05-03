package com.example.doantotnghiep.View.Auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.ViewModel.SavedPostsViewModel
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class SavedPostsActivity : AppCompatActivity() {

    private lateinit var layoutPosts: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var btnBack: ImageView
    private lateinit var swipeRefreshLayout: androidx.swiperefreshlayout.widget.SwipeRefreshLayout

    private lateinit var viewModel: SavedPostsViewModel

    private val formatter: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN"))
        symbols.groupingSeparator = '.'
        DecimalFormat("#,###", symbols)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_posts)

        layoutPosts = findViewById(R.id.layoutPosts)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)
        btnBack = findViewById(R.id.btnBack)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        btnBack.setOnClickListener { finish() }

        viewModel = ViewModelProvider(this)[SavedPostsViewModel::class.java]

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.savedPosts.observe(this) { posts ->
            layoutPosts.removeAllViews()
            if (posts.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
            } else {
                tvEmpty.visibility = View.GONE
                posts.forEach { post ->
                    layoutPosts.addView(createSavedCard(post))
                }
            }
        }

        viewModel.errorMessage.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                tvEmpty.text = "Lỗi tải dữ liệu"
                tvEmpty.visibility = View.VISIBLE
                swipeRefreshLayout.isRefreshing = false
            }
        }

        swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadSavedPosts()
        }

        viewModel.deleteResult.observe(this) { _ ->
            swipeRefreshLayout.isRefreshing = false
            viewModel.loadSavedPosts()
        }

        viewModel.roomCheckResult.observe(this) { result ->
            if (result == null) return@observe
            val (savedDocId, exists) = result
            
            // Xóa state để tránh lỗi LiveData trigger lại khi resume Activity
            viewModel.clearRoomCheckResult()
            
            progressBar.visibility = View.GONE
            val post = viewModel.savedPosts.value?.find { it.savedDocId == savedDocId } ?: return@observe
            
            if (exists) {
                startActivity(Intent(this, RoomDetailActivity::class.java).apply {
                    putExtra("roomId", post.roomId)
                })
            } else {
                viewModel.autoDeleteSavedPost(savedDocId)
                MessageUtils.showInfoDialog(
                    this,
                    "Phòng không còn tồn tại",
                    "Bài đăng này đã bị gỡ bởi chủ trọ và đã được xóa khỏi danh sách yêu thích của bạn."
                ) { viewModel.loadSavedPosts() }
            }
        }

        viewModel.loadSavedPosts()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadSavedPosts()
    }

    private fun createSavedCard(post: SavedPostsViewModel.SavedPost): CardView {
        val card = CardView(this)
        val cardParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        cardParams.bottomMargin = dpToPx(12)
        card.layoutParams = cardParams
        card.radius = dpToPx(14).toFloat()
        card.cardElevation = dpToPx(3).toFloat()
        card.setCardBackgroundColor(0xFFFFFFFF.toInt())

        val mainLayout = LinearLayout(this)
        mainLayout.orientation = LinearLayout.HORIZONTAL
        mainLayout.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))

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
        if (post.imageUrl.isNotEmpty()) Glide.with(this).load(post.imageUrl).centerCrop().into(imgView)
        mainLayout.addView(imgView)

        val infoLayout = LinearLayout(this)
        infoLayout.orientation = LinearLayout.VERTICAL
        val infoParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        infoParams.marginStart = dpToPx(12)
        infoLayout.layoutParams = infoParams

        val tvTitle = TextView(this).apply {
            text = post.title; textSize = 15f; setTextColor(0xFF1A1A2E.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD); maxLines = 2
        }
        infoLayout.addView(tvTitle)

        val fullAddr = if (post.address.isNotEmpty()) "${post.address}, ${post.ward}, ${post.district}" else "${post.ward}, ${post.district}"
        val tvAddress = TextView(this).apply {
            text = fullAddr; textSize = 12f; setTextColor(0xFF999999.toInt())
            setPadding(0, dpToPx(3), 0, 0); maxLines = 2
        }
        infoLayout.addView(tvAddress)

        val tvPrice = TextView(this).apply {
            text = "${formatter.format(post.price)} đ/tháng"; textSize = 15f; setTextColor(0xFFD32F2F.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD); setPadding(0, dpToPx(4), 0, 0)
        }
        infoLayout.addView(tvPrice)

        val tvRemove = TextView(this).apply {
            text = "Bỏ lưu"; textSize = 12f; setTextColor(0xFFD32F2F.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD); setPadding(0, dpToPx(4), 0, 0)
        }
        tvRemove.setOnClickListener {
            tvRemove.isClickable = false
            tvRemove.alpha = 0.4f
            viewModel.deleteSavedPost(post.savedDocId)
        }
        infoLayout.addView(tvRemove)

        mainLayout.addView(infoLayout)
        card.addView(mainLayout)

        card.setOnClickListener {
            if (post.roomId.isEmpty()) {
                MessageUtils.showErrorDialog(this, "Lỗi", "Không tìm thấy thông tin phòng.")
                return@setOnClickListener
            }
            progressBar.visibility = View.VISIBLE
            viewModel.checkRoomExists(post.savedDocId, post.roomId)
        }

        return card
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}