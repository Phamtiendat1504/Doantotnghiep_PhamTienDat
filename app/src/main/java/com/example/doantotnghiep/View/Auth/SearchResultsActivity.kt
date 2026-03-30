package com.example.doantotnghiep.View.Auth

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.example.doantotnghiep.R
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class SearchResultsActivity : AppCompatActivity() {

    private lateinit var layoutResults: LinearLayout
    private lateinit var layoutPagination: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var tvResultCount: TextView
    private lateinit var tvSearchArea: TextView
    private lateinit var btnBack: ImageView

    private val db = FirebaseFirestore.getInstance()
    private val formatter: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN"))
        symbols.groupingSeparator = '.'
        DecimalFormat("#,###", symbols)
    }

    // Data class gộp theo chủ trọ
    data class OwnerGroup(
        val userId: String,
        val ownerName: String,
        val address: String,
        val ward: String,
        val district: String,
        val firstImage: String,
        val roomCount: Int,
        val minPrice: Long,
        val maxPrice: Long,
        val docs: List<DocumentSnapshot>
    )

    private var allOwnerGroups = mutableListOf<OwnerGroup>()
    private var currentPage = 1
    private val itemsPerPage = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_results)

        layoutResults = findViewById(R.id.layoutResults)
        layoutPagination = findViewById(R.id.layoutPagination)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)
        tvResultCount = findViewById(R.id.tvResultCount)
        tvSearchArea = findViewById(R.id.tvSearchArea)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener { finish() }

        // Hiển thị khu vực tìm kiếm
        val wardDisplay = intent.getStringExtra("ward") ?: ""
        tvSearchArea.text = wardDisplay

        // Lấy tiêu chí tìm kiếm
        val ward = intent.getStringExtra("ward") ?: ""
        val minPrice = intent.getLongExtra("minPrice", 0)
        val maxPrice = intent.getLongExtra("maxPrice", 0)
        val minArea = intent.getIntExtra("minArea", 0)
        val maxArea = intent.getIntExtra("maxArea", 0)
        val hasWifi = intent.getBooleanExtra("hasWifi", false)
        val hasAirCon = intent.getBooleanExtra("hasAirCon", false)
        val hasWaterHeater = intent.getBooleanExtra("hasWaterHeater", false)
        val hasParking = intent.getBooleanExtra("hasParking", false)
        val genderPrefer = intent.getStringExtra("genderPrefer") ?: ""
        val curfew = intent.getStringExtra("curfew") ?: ""

        searchRooms(ward, minPrice, maxPrice, minArea, maxArea,
            hasWifi, hasAirCon, hasWaterHeater, hasParking, genderPrefer, curfew)
    }

    // ═══════════════════════════════════════
    // TÌM KIẾM VÀ GỘP THEO CHỦ TRỌ
    // ═══════════════════════════════════════
    private fun searchRooms(
        ward: String, minPrice: Long, maxPrice: Long,
        minArea: Int, maxArea: Int,
        hasWifi: Boolean, hasAirCon: Boolean,
        hasWaterHeater: Boolean, hasParking: Boolean,
        genderPrefer: String, curfew: String
    ) {
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE

        db.collection("rooms")
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener { documents ->
                progressBar.visibility = View.GONE

                if (documents.isEmpty) {
                    tvEmpty.visibility = View.VISIBLE
                    tvResultCount.text = "Kết quả tìm kiếm: 0 phòng"
                    return@addOnSuccessListener
                }

                // Lọc theo khu vực
                val matchedDocs = documents.filter { doc ->
                    val data = doc.data
                    if (ward.isNotEmpty() && ward != "-- Chọn phường/xã --") {
                        val roomWard = data["ward"] as? String ?: ""
                        val roomDistrict = data["district"] as? String ?: ""
                        val roomAddress = data["address"] as? String ?: ""
                        val fullLocation = "$roomWard $roomDistrict $roomAddress"
                        fullLocation.contains(ward, ignoreCase = true) ||
                                ward.contains(roomWard, ignoreCase = true)
                    } else true
                }

                // Gộp theo chủ trọ (userId)
                val groupedByOwner = matchedDocs.groupBy { it.getString("userId") ?: "" }

                val ownerGroups = mutableListOf<OwnerGroup>()

                for ((userId, docs) in groupedByOwner) {
                    if (userId.isEmpty()) continue
                    val firstDoc = docs.first().data ?: continue

                    val ownerName = firstDoc["ownerName"] as? String ?: ""
                    val addr = firstDoc["address"] as? String ?: ""
                    val w = firstDoc["ward"] as? String ?: ""
                    val d = firstDoc["district"] as? String ?: ""

                    // Lấy ảnh đầu tiên
                    var firstImg = ""
                    for (doc in docs) {
                        val imgs = doc.data?.get("imageUrls") as? List<String> ?: listOf()
                        if (imgs.isNotEmpty()) {
                            firstImg = imgs[0]
                            break
                        }
                    }

                    // Tính giá min max
                    val prices = docs.mapNotNull { (it.data?.get("price") as? Long) }.filter { it > 0 }
                    val pMin = prices.minOrNull() ?: 0
                    val pMax = prices.maxOrNull() ?: 0

                    ownerGroups.add(
                        OwnerGroup(userId, ownerName, addr, w, d, firstImg, docs.size, pMin, pMax, docs)
                    )
                }

                allOwnerGroups = ownerGroups
                tvResultCount.text = "Kết quả tìm kiếm: ${matchedDocs.size} phòng"

                if (ownerGroups.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                currentPage = 1
                displayPage(currentPage)
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                tvEmpty.text = "Lỗi tải dữ liệu"
                tvEmpty.visibility = View.VISIBLE
            }
    }

    // ═══════════════════════════════════════
    // HIỂN THỊ TRANG (LƯỚI 2 CỘT)
    // ═══════════════════════════════════════
    private fun displayPage(page: Int) {
        layoutResults.removeAllViews()

        val totalPages = Math.ceil(allOwnerGroups.size.toDouble() / itemsPerPage).toInt()
        val startIndex = (page - 1) * itemsPerPage
        val endIndex = minOf(startIndex + itemsPerPage, allOwnerGroups.size)
        val pageItems = allOwnerGroups.subList(startIndex, endIndex)

        var i = 0
        while (i < pageItems.size) {
            val rowLayout = LinearLayout(this)
            rowLayout.orientation = LinearLayout.HORIZONTAL
            rowLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            // Cột trái
            val leftCard = createOwnerCard(pageItems[i])
            val leftParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            leftParams.marginEnd = dpToPx(5)
            leftCard.layoutParams = leftParams
            rowLayout.addView(leftCard)

            // Cột phải
            if (i + 1 < pageItems.size) {
                val rightCard = createOwnerCard(pageItems[i + 1])
                val rightParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                rightParams.marginStart = dpToPx(5)
                rightCard.layoutParams = rightParams
                rowLayout.addView(rightCard)
            } else {
                val emptyView = View(this)
                val emptyParams = LinearLayout.LayoutParams(0, 0, 1f)
                emptyParams.marginStart = dpToPx(5)
                emptyView.layoutParams = emptyParams
                rowLayout.addView(emptyView)
            }

            val rowParams = rowLayout.layoutParams as LinearLayout.LayoutParams
            rowParams.bottomMargin = dpToPx(10)
            rowLayout.layoutParams = rowParams

            layoutResults.addView(rowLayout)
            i += 2
        }

        displayPagination(page, totalPages)
    }

    // ═══════════════════════════════════════
    // PHÂN TRANG
    // ═══════════════════════════════════════
    private fun displayPagination(currentPage: Int, totalPages: Int) {
        layoutPagination.removeAllViews()

        if (totalPages <= 1) return

        // Nút Trước
        if (currentPage > 1) {
            val btnPrev = createPageButton("← Trước", false)
            btnPrev.setOnClickListener {
                this.currentPage = currentPage - 1
                displayPage(this.currentPage)
                findViewById<androidx.core.widget.NestedScrollView>(R.id.scrollView)?.scrollTo(0, 0)
            }
            layoutPagination.addView(btnPrev)
        }

        // Số trang
        for (i in 1..totalPages) {
            val btnPage = createPageButton("$i", i == currentPage)
            btnPage.setOnClickListener {
                this.currentPage = i
                displayPage(this.currentPage)
                findViewById<androidx.core.widget.NestedScrollView>(R.id.scrollView)?.scrollTo(0, 0)
            }
            layoutPagination.addView(btnPage)
        }

        // Nút Sau
        if (currentPage < totalPages) {
            val btnNext = createPageButton("Sau →", false)
            btnNext.setOnClickListener {
                this.currentPage = currentPage + 1
                displayPage(this.currentPage)
                findViewById<androidx.core.widget.NestedScrollView>(R.id.scrollView)?.scrollTo(0, 0)
            }
            layoutPagination.addView(btnNext)
        }
    }

    private fun createPageButton(text: String, isActive: Boolean): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.textSize = 14f
        tv.setTypeface(tv.typeface, android.graphics.Typeface.BOLD)
        tv.gravity = Gravity.CENTER
        tv.setPadding(dpToPx(14), dpToPx(8), dpToPx(14), dpToPx(8))

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.marginEnd = dpToPx(6)
        tv.layoutParams = params

        if (isActive) {
            tv.setTextColor(0xFFFFFFFF.toInt())
            tv.background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xFF1976D2.toInt())
                cornerRadius = dpToPx(8).toFloat()
            }
        } else {
            tv.setTextColor(0xFF1976D2.toInt())
            tv.background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xFFFFFFFF.toInt())
                setStroke(dpToPx(1), 0xFFE0E0E0.toInt())
                cornerRadius = dpToPx(8).toFloat()
            }
        }

        return tv
    }

    // ═══════════════════════════════════════
    // TẠO CARD CHỦ TRỌ (GỘP PHÒNG)
    // ═══════════════════════════════════════
    private fun createOwnerCard(group: OwnerGroup): CardView {
        val card = CardView(this)
        card.radius = dpToPx(12).toFloat()
        card.cardElevation = dpToPx(3).toFloat()
        card.setCardBackgroundColor(0xFFFFFFFF.toInt())

        val mainLayout = LinearLayout(this)
        mainLayout.orientation = LinearLayout.VERTICAL
        mainLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        // ═══ ẢNH ═══
        val imgView = ImageView(this)
        val imgParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(130)
        )
        imgView.layoutParams = imgParams
        imgView.scaleType = ImageView.ScaleType.CENTER_CROP
        imgView.setBackgroundColor(0xFFE0E0E0.toInt())
        imgView.clipToOutline = true
        imgView.adjustViewBounds = false

        if (group.firstImage.isNotEmpty()) {
            Glide.with(this).load(group.firstImage).centerCrop().into(imgView)
        }
        mainLayout.addView(imgView)

        // ═══ THÔNG TIN ═══
        val infoLayout = LinearLayout(this)
        infoLayout.orientation = LinearLayout.VERTICAL
        infoLayout.setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(10))

        // Địa chỉ
        val tvAddress = TextView(this)
        val fullAddress = if (group.address.isNotEmpty())
            "${group.address}, ${group.ward}, ${group.district}"
        else "${group.ward}, ${group.district}"
        tvAddress.text = fullAddress
        tvAddress.textSize = 12f
        tvAddress.setTextColor(0xFF333333.toInt())
        tvAddress.maxLines = 2
        tvAddress.minLines = 2
        tvAddress.ellipsize = android.text.TextUtils.TruncateAt.END
        infoLayout.addView(tvAddress)

        // Giá
        val tvPrice = TextView(this)
        if (group.minPrice == group.maxPrice || group.maxPrice == 0L) {
            tvPrice.text = "Giá: ${formatter.format(group.minPrice)}đ/th"
        } else {
            tvPrice.text = "Giá: ${formatter.format(group.minPrice)} - ${formatter.format(group.maxPrice)}đ/th"
        }
        tvPrice.textSize = 12f
        tvPrice.setTextColor(0xFFD32F2F.toInt())
        tvPrice.setTypeface(tvPrice.typeface, android.graphics.Typeface.BOLD)
        tvPrice.setPadding(0, dpToPx(4), 0, 0)
        infoLayout.addView(tvPrice)

        // Số phòng trống
        val tvRoomCount = TextView(this)
        tvRoomCount.text = "Còn ${group.roomCount} phòng trống"
        tvRoomCount.textSize = 11f
        tvRoomCount.setTextColor(0xFFFFFFFF.toInt())
        tvRoomCount.setTypeface(tvRoomCount.typeface, android.graphics.Typeface.BOLD)
        tvRoomCount.setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3))
        tvRoomCount.background = android.graphics.drawable.GradientDrawable().apply {
            setColor(0xFF1976D2.toInt())
            cornerRadius = dpToPx(6).toFloat()
        }
        val rcParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        rcParams.topMargin = dpToPx(5)
        tvRoomCount.layoutParams = rcParams
        infoLayout.addView(tvRoomCount)

        mainLayout.addView(infoLayout)
        card.addView(mainLayout)

        // ═══ BẤM VÀO XEM DANH SÁCH PHÒNG ═══
        card.setOnClickListener {
            val intent = Intent(this, RoomDetailActivity::class.java)
            intent.putExtra("userId", group.userId)
            startActivity(intent)
        }

        return card
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}