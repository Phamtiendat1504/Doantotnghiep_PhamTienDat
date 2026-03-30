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
import com.bumptech.glide.Glide
import com.example.doantotnghiep.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class SavedPostsActivity : AppCompatActivity() {

    private lateinit var layoutPosts: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var btnBack: ImageView

    private val db = FirebaseFirestore.getInstance()
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

        btnBack.setOnClickListener { finish() }

        loadSavedPosts()
    }

    override fun onResume() {
        super.onResume()
        loadSavedPosts()
    }

    private fun loadSavedPosts() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        layoutPosts.removeAllViews()
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE

        db.collection("savedPosts")
            .whereEqualTo("userId", uid)
            .orderBy("savedAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                progressBar.visibility = View.GONE
                layoutPosts.removeAllViews()

                if (documents.isEmpty) {
                    tvEmpty.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                for (doc in documents) {
                    val card = createSavedCard(
                        savedDocId = doc.id,
                        roomId = doc.getString("roomId") ?: "",
                        ownerId = doc.getString("ownerId") ?: "",
                        title = doc.getString("title") ?: "",
                        price = doc.getLong("price") ?: 0,
                        address = doc.getString("address") ?: "",
                        ward = doc.getString("ward") ?: "",
                        district = doc.getString("district") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: ""
                    )
                    layoutPosts.addView(card)
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                tvEmpty.text = "Lỗi tải dữ liệu"
                tvEmpty.visibility = View.VISIBLE
            }
    }

    private fun createSavedCard(
        savedDocId: String, roomId: String, ownerId: String,
        title: String, price: Long, address: String,
        ward: String, district: String, imageUrl: String
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
        mainLayout.orientation = LinearLayout.HORIZONTAL
        mainLayout.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))

        // Ảnh
        val imgView = ImageView(this)
        val imgParams = LinearLayout.LayoutParams(dpToPx(100), dpToPx(85))
        imgView.layoutParams = imgParams
        imgView.scaleType = ImageView.ScaleType.CENTER_CROP
        imgView.setBackgroundColor(0xFFE0E0E0.toInt())
        imgView.clipToOutline = true
        imgView.outlineProvider = object : android.view.ViewOutlineProvider() {
            override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, dpToPx(10).toFloat())
            }
        }

        if (imageUrl.isNotEmpty()) {
            Glide.with(this).load(imageUrl).centerCrop().into(imgView)
        }
        mainLayout.addView(imgView)

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

        val tvAddress = TextView(this)
        val fullAddr = if (address.isNotEmpty()) "$address, $ward, $district" else "$ward, $district"
        tvAddress.text = fullAddr
        tvAddress.textSize = 12f
        tvAddress.setTextColor(0xFF999999.toInt())
        tvAddress.setPadding(0, dpToPx(3), 0, 0)
        tvAddress.maxLines = 2
        infoLayout.addView(tvAddress)

        val tvPrice = TextView(this)
        tvPrice.text = "${formatter.format(price)} đ/tháng"
        tvPrice.textSize = 15f
        tvPrice.setTextColor(0xFFD32F2F.toInt())
        tvPrice.setTypeface(tvPrice.typeface, android.graphics.Typeface.BOLD)
        tvPrice.setPadding(0, dpToPx(4), 0, 0)
        infoLayout.addView(tvPrice)

        // Nút bỏ lưu
        val tvRemove = TextView(this)
        tvRemove.text = "Bỏ lưu"
        tvRemove.textSize = 12f
        tvRemove.setTextColor(0xFFD32F2F.toInt())
        tvRemove.setTypeface(tvRemove.typeface, android.graphics.Typeface.BOLD)
        tvRemove.setPadding(0, dpToPx(4), 0, 0)
        tvRemove.setOnClickListener {
            db.collection("savedPosts").document(savedDocId).delete()
                .addOnSuccessListener { loadSavedPosts() }
        }
        infoLayout.addView(tvRemove)

        mainLayout.addView(infoLayout)
        card.addView(mainLayout)

        // Bấm vào xem chi tiết
        card.setOnClickListener {
            val intent = Intent(this, RoomDetailActivity::class.java)
            intent.putExtra("userId", ownerId)
            intent.putExtra("roomId", roomId)
            startActivity(intent)
        }

        return card
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}