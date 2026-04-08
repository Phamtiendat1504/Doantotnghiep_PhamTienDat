package com.example.doantotnghiep.View.Auth

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.doantotnghiep.R
import com.example.doantotnghiep.ViewModel.SearchViewModel
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

class SearchResultsActivity : AppCompatActivity() {

    private lateinit var layoutResults: LinearLayout
    private lateinit var layoutPagination: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var tvResultCount: TextView
    private lateinit var tvSearchArea: TextView
    private lateinit var btnBack: ImageView

    private lateinit var viewModel: SearchViewModel

    private val formatter: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN"))
        symbols.groupingSeparator = '.'
        DecimalFormat("#,###", symbols)
    }

    private var currentPage = 1
    private val itemsPerPage = 10

    enum class SortType { NONE, PRICE_ASC, PRICE_DESC, NEWEST, OLDEST }
    private var currentSort = SortType.NEWEST

    private lateinit var chipSortPriceAsc: TextView
    private lateinit var chipSortPriceDesc: TextView
    private lateinit var chipSortNewest: TextView
    private lateinit var chipSortOldest: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_results)

        viewModel = ViewModelProvider(this)[SearchViewModel::class.java]

        layoutResults = findViewById(R.id.layoutResults)
        layoutPagination = findViewById(R.id.layoutPagination)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)
        tvResultCount = findViewById(R.id.tvResultCount)
        tvSearchArea = findViewById(R.id.tvSearchArea)
        btnBack = findViewById(R.id.btnBack)
        chipSortPriceAsc = findViewById(R.id.chipSortPriceAsc)
        chipSortPriceDesc = findViewById(R.id.chipSortPriceDesc)
        chipSortNewest = findViewById(R.id.chipSortNewest)
        chipSortOldest = findViewById(R.id.chipSortOldest)

        btnBack.setOnClickListener { finish() }

        listOf(chipSortPriceAsc, chipSortPriceDesc, chipSortNewest, chipSortOldest).forEach { chip ->
            chip.setOnClickListener { onSortChipClick(chip) }
        }

        observeViewModel()

        val query = intent.getStringExtra("query") ?: ""
        val ward = intent.getStringExtra("ward") ?: ""
        val district = intent.getStringExtra("district") ?: ""
        val searchMode = intent.getStringExtra("searchMode") ?: "ward"
        val minPrice = intent.getLongExtra("minPrice", 0)
        val maxPrice = intent.getLongExtra("maxPrice", 0)

        tvSearchArea.text = when {
            query.isNotEmpty() -> "\"$query\""
            ward.isEmpty() -> "Tất cả khu vực"
            searchMode == "district" && district.isNotEmpty() -> district
            district.isNotEmpty() -> "$ward ($district)"
            else -> ward
        }

        if (query.isNotEmpty()) {
            viewModel.searchByQuery(query)
        } else {
            viewModel.searchByFilters(ward, district, searchMode, minPrice, maxPrice)
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            tvEmpty.visibility = View.GONE
        }

        viewModel.searchResults.observe(this) { results ->
            tvResultCount.text = "Kết quả tìm kiếm: ${results.size} bài đăng"
            if (results.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                layoutResults.removeAllViews()
                layoutPagination.removeAllViews()
            } else {
                tvEmpty.visibility = View.GONE
                currentPage = 1
                applySortAndDisplay(results)
            }
        }

        viewModel.errorMessage.observe(this) { msg ->
            if (!msg.isNullOrEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                tvResultCount.text = "Kết quả tìm kiếm: 0 bài đăng"
            }
        }
    }

    private fun onSortChipClick(selected: TextView) {
        val newSort = when (selected) {
            chipSortPriceAsc -> SortType.PRICE_ASC
            chipSortPriceDesc -> SortType.PRICE_DESC
            chipSortNewest -> SortType.NEWEST
            chipSortOldest -> SortType.OLDEST
            else -> SortType.NONE
        }
        currentSort = if (currentSort == newSort) SortType.NONE else newSort
        updateChipUI()
        currentPage = 1
        val results = viewModel.searchResults.value ?: return
        applySortAndDisplay(results)
    }

    private fun updateChipUI() {
        val chips = mapOf(
            chipSortPriceAsc to SortType.PRICE_ASC,
            chipSortPriceDesc to SortType.PRICE_DESC,
            chipSortNewest to SortType.NEWEST,
            chipSortOldest to SortType.OLDEST
        )
        chips.forEach { (chip, sort) ->
            if (currentSort == sort) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(0xFFFFFFFF.toInt())
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                chip.setTextColor(0xFF1976D2.toInt())
            }
        }
    }

    private fun applySortAndDisplay(items: List<SearchViewModel.RoomItem>) {
        val sorted = when (currentSort) {
            SortType.PRICE_ASC -> items.sortedBy { it.price }
            SortType.PRICE_DESC -> items.sortedByDescending { it.price }
            SortType.NEWEST -> items.sortedByDescending { it.createdAt }
            SortType.OLDEST -> items.sortedBy { it.createdAt }
            SortType.NONE -> items
        }
        val totalPages = ceil(sorted.size / itemsPerPage.toDouble()).toInt().coerceAtLeast(1)
        val page = currentPage.coerceIn(1, totalPages)
        val start = (page - 1) * itemsPerPage
        val end = minOf(start + itemsPerPage, sorted.size)
        val pageItems = sorted.subList(start, end)

        layoutResults.removeAllViews()
        var i = 0
        while (i < pageItems.size) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dpToPx(10)
                }
            }
            row.addView(createRoomCard(pageItems[i]).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dpToPx(5) }
            })
            if (i + 1 < pageItems.size) {
                row.addView(createRoomCard(pageItems[i + 1]).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dpToPx(5) }
                })
            } else {
                row.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(0, 0, 1f) })
            }
            layoutResults.addView(row)
            i += 2
        }
        displayPagination(page, totalPages, sorted)
    }

    private fun createRoomCard(item: SearchViewModel.RoomItem): CardView {
        val card = CardView(this).apply {
            radius = dpToPx(12).toFloat()
            cardElevation = dpToPx(3).toFloat()
        }
        val mainLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val imageContainer = FrameLayout(this)
        val imgView = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dpToPx(130))
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        Glide.with(this).load(item.firstImage).placeholder(R.color.gray_200).into(imgView)
        imageContainer.addView(imgView)

        val verifiedBadge = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(6), dpToPx(2), dpToPx(8), dpToPx(2))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xFFE8F5E9.toInt())
                cornerRadius = dpToPx(4).toFloat()
            }
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.TOP or Gravity.START
                topMargin = dpToPx(8); leftMargin = dpToPx(8)
            }
        }
        verifiedBadge.addView(TextView(this).apply {
            text = "✓"
            textSize = 13f
            setTextColor(0xFF2E7D32.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(dpToPx(14), dpToPx(14))
        })
        verifiedBadge.addView(TextView(this).apply {
            text = "Đã được kiểm duyệt"
            textSize = 10f
            setTextColor(0xFF2E7D32.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(dpToPx(4), 0, 0, 0)
        })
        imageContainer.addView(verifiedBadge)
        mainLayout.addView(imageContainer)

        val infoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(10))
        }

        infoLayout.addView(TextView(this).apply {
            text = item.title.ifEmpty { "${item.address}, ${item.ward}" }
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(0xFF1A1A2E.toInt())
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
        })

        val fullAddress = buildString {
            if (item.address.isNotEmpty()) append(item.address)
            if (item.ward.isNotEmpty()) { if (isNotEmpty()) append(", "); append(item.ward) }
            if (item.district.isNotEmpty()) { if (isNotEmpty()) append(", "); append(item.district) }
        }
        infoLayout.addView(TextView(this).apply {
            text = "📍 $fullAddress"
            textSize = 11f
            setTextColor(0xFF757575.toInt())
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dpToPx(3)
            }
        })

        val rowPriceDate = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dpToPx(5)
            }
        }
        rowPriceDate.addView(TextView(this).apply {
            text = "${formatter.format(item.price)}đ/th"
            setTextColor(0xFFD32F2F.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        val dateStr = if (item.createdAt > 0)
            SimpleDateFormat("dd/MM/yyyy", Locale("vi")).format(Date(item.createdAt)) else ""
        if (dateStr.isNotEmpty()) {
            rowPriceDate.addView(TextView(this).apply {
                text = "🕐 $dateStr"
                textSize = 10f
                setTextColor(0xFF9E9E9E.toInt())
            })
        }
        infoLayout.addView(rowPriceDate)

        val available = item.roomCount - item.rentedCount
        if (item.roomCount > 0) {
            infoLayout.addView(TextView(this).apply {
                text = if (available > 0) "Còn $available phòng trống" else "Hết phòng"
                textSize = 10f
                setTextColor(0xFFFFFFFF.toInt())
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(dpToPx(8), dpToPx(2), dpToPx(8), dpToPx(2))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(if (available > 0) 0xFF1976D2.toInt() else 0xFF9E9E9E.toInt())
                    cornerRadius = dpToPx(4).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = dpToPx(4)
                }
            })
        }

        mainLayout.addView(infoLayout)
        card.addView(mainLayout)
        card.setOnClickListener {
            startActivity(Intent(this, RoomDetailActivity::class.java).apply {
                putExtra("roomId", item.roomId)
            })
        }
        return card
    }

    private fun displayPagination(currentPage: Int, totalPages: Int, items: List<SearchViewModel.RoomItem>) {
        layoutPagination.removeAllViews()
        if (totalPages <= 1) return
        for (i in 1..totalPages) {
            val tv = TextView(this).apply {
                text = "$i"
                setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
                if (i == currentPage) {
                    setTextColor(0xFFFFFFFF.toInt())
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(0xFF1976D2.toInt()); cornerRadius = dpToPx(8).toFloat()
                    }
                }
                setOnClickListener {
                    this@SearchResultsActivity.currentPage = i
                    applySortAndDisplay(items)
                }
            }
            layoutPagination.addView(tv)
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}