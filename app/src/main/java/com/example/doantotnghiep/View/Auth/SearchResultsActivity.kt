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
    private var currentSort = SortType.NONE

    private lateinit var tvCurrentSort: TextView
    private lateinit var btnSortDropdown: FrameLayout
    private lateinit var ivSortArrow: ImageView

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

        tvCurrentSort = findViewById(R.id.tvCurrentSort)
        btnSortDropdown = findViewById(R.id.btnSortDropdown)
        ivSortArrow = findViewById(R.id.ivSortArrow)

        btnBack.setOnClickListener { finish() }

        btnSortDropdown.setOnClickListener {
            ivSortArrow.animate().rotation(270f).setDuration(140).start()
            showSortMenu()
        }
        updateSortUI()

        observeViewModel()

        val query = intent.getStringExtra("query") ?: ""
        val ward = intent.getStringExtra("ward") ?: ""
        val district = intent.getStringExtra("district") ?: ""
        val searchMode = intent.getStringExtra("searchMode") ?: "ward"
        val minPrice = intent.getLongExtra("minPrice", 0)
        val maxPrice = intent.getLongExtra("maxPrice", 0)
        val addressKeyword = intent.getStringExtra("addressKeyword") ?: ""
        val minArea = intent.getIntExtra("minArea", 0)
        val maxArea = intent.getIntExtra("maxArea", 0)
        val desiredPeople = intent.getIntExtra("desiredPeople", 0)
        val roomType = intent.getStringExtra("roomType") ?: ""
        val hasWifi = intent.getBooleanExtra("hasWifi", false)
        val hasElectric = intent.getBooleanExtra("hasElectric", false)
        val hasWater = intent.getBooleanExtra("hasWater", false)

        tvSearchArea.text = when {
            query.isNotEmpty() -> "\"$query\""
            searchMode == "district" && district.isNotEmpty() -> district
            ward.isEmpty() -> "\u0054\u1ea5\u0074 \u0063\u1ea3 \u006b\u0068\u0075 \u0076\u1ef1\u0063"
            district.isNotEmpty() -> "$ward ($district)"
            else -> ward
        }

        if (query.isNotEmpty()) {
            viewModel.searchByQuery(query)
        } else {
            viewModel.searchByFilters(
                ward = ward,
                district = district,
                searchMode = searchMode,
                minPrice = minPrice,
                maxPrice = maxPrice,
                addressKeyword = addressKeyword,
                minArea = minArea,
                maxArea = maxArea,
                desiredPeople = desiredPeople,
                roomType = roomType,
                hasWifi = hasWifi,
                hasElectric = hasElectric,
                hasWater = hasWater
            )
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            tvEmpty.visibility = View.GONE
        }

        viewModel.searchResults.observe(this) { results ->
            tvResultCount.text = "\u004b\u1ebf\u0074 \u0071\u0075\u1ea3 \u0074\u00ec\u006d \u006b\u0069\u1ebf\u006d\u003a ${results.size} \u0062\u00e0\u0069 \u0111\u0103\u006e\u0067"
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
                tvResultCount.text = "\u004b\u1ebf\u0074 \u0071\u0075\u1ea3 \u0074\u00ec\u006d \u006b\u0069\u1ebf\u006d\u003a 0 \u0062\u00e0\u0069 \u0111\u0103\u006e\u0067"
            }
        }
    }

    private fun showSortMenu() {
        val popup = androidx.appcompat.widget.PopupMenu(this, btnSortDropdown)
        popup.menu.add("\u0050\u0068\u00f9 \u0068\u1ee3\u0070 \u006e\u0068\u1ea5\u0074")
        popup.menu.add("\u004d\u1edb\u0069 \u006e\u0068\u1ea5\u0074")
        popup.menu.add("\u0043\u0169 \u006e\u0068\u1ea5\u0074")
        popup.menu.add("\u0047\u0069\u00e1 \u0074\u0068\u1ea5\u0070 \u0111\u1ebf\u006e \u0063\u0061\u006f")
        popup.menu.add("\u0047\u0069\u00e1 \u0063\u0061\u006f \u0111\u1ebf\u006e \u0074\u0068\u1ea5\u0070")

        popup.setOnMenuItemClickListener { item ->
            val newSort = when (item.title.toString()) {
                "\u0050\u0068\u00f9 \u0068\u1ee3\u0070 \u006e\u0068\u1ea5\u0074" -> SortType.NONE
                "\u0047\u0069\u00e1 \u0074\u0068\u1ea5\u0070 \u0111\u1ebf\u006e \u0063\u0061\u006f" -> SortType.PRICE_ASC
                "\u0047\u0069\u00e1 \u0063\u0061\u006f \u0111\u1ebf\u006e \u0074\u0068\u1ea5\u0070" -> SortType.PRICE_DESC
                "\u004d\u1edb\u0069 \u006e\u0068\u1ea5\u0074" -> SortType.NEWEST
                "\u0043\u0169 \u006e\u0068\u1ea5\u0074" -> SortType.OLDEST
                else -> SortType.NONE
            }
            currentSort = newSort
            updateSortUI()

            currentPage = 1
            val results = viewModel.searchResults.value ?: return@setOnMenuItemClickListener true
            applySortAndDisplay(results)
            true
        }
        popup.setOnDismissListener {
            ivSortArrow.animate().rotation(90f).setDuration(140).start()
        }
        popup.show()
    }

    private fun updateSortUI() {
        tvCurrentSort.text = when (currentSort) {
            SortType.NONE -> "\u0050\u0068\u00f9 \u0068\u1ee3\u0070 \u006e\u0068\u1ea5\u0074"
            SortType.PRICE_ASC -> "\u0047\u0069\u00e1 \u0074\u0068\u1ea5\u0070 \u0111\u1ebf\u006e \u0063\u0061\u006f"
            SortType.PRICE_DESC -> "\u0047\u0069\u00e1 \u0063\u0061\u006f \u0111\u1ebf\u006e \u0074\u0068\u1ea5\u0070"
            SortType.NEWEST -> "\u004d\u1edb\u0069 \u006e\u0068\u1ea5\u0074"
            SortType.OLDEST -> "\u0043\u0169 \u006e\u0068\u1ea5\u0074"
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
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dpToPx(10) }
            }

            row.addView(createRoomCard(pageItems[i]).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply { marginEnd = dpToPx(5) }
            })

            if (i + 1 < pageItems.size) {
                row.addView(createRoomCard(pageItems[i + 1]).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply { marginStart = dpToPx(5) }
                })
            } else {
                row.addView(View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
                })
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
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dpToPx(130)
            )
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
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                topMargin = dpToPx(8)
                leftMargin = dpToPx(8)
            }
        }

        verifiedBadge.addView(TextView(this).apply {
            text = "\u2713"
            textSize = 13f
            setTextColor(0xFF2E7D32.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(dpToPx(14), dpToPx(14))
        })

        verifiedBadge.addView(TextView(this).apply {
            text = "\u0110\u00e3 \u006b\u0069\u1ec3\u006d \u0064\u0075\u0079\u1ec7\u0074"
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
            if (item.ward.isNotEmpty()) {
                if (isNotEmpty()) append(", ")
                append(item.ward)
            }
            if (item.district.isNotEmpty()) {
                if (isNotEmpty()) append(", ")
                append(item.district)
            }
        }

        infoLayout.addView(TextView(this).apply {
            text = "\u0110\u1ecba ch\u1ec9: $fullAddress"
            textSize = 11f
            setTextColor(0xFF757575.toInt())
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dpToPx(3) }
        })

        val rowPriceDate = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dpToPx(5) }
        }

        rowPriceDate.addView(TextView(this).apply {
            text = "${formatter.format(item.price)} \u0111/th"
            setTextColor(0xFFD32F2F.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        val dateStr = if (item.createdAt > 0) {
            SimpleDateFormat("dd/MM/yyyy", Locale("vi")).format(Date(item.createdAt))
        } else {
            ""
        }

        if (dateStr.isNotEmpty()) {
            rowPriceDate.addView(TextView(this).apply {
                text = "Ng\u00e0y: $dateStr"
                textSize = 10f
                setTextColor(0xFF9E9E9E.toInt())
            })
        }
        infoLayout.addView(rowPriceDate)

        val available = item.roomCount - item.rentedCount
        if (item.roomCount > 0) {
            infoLayout.addView(TextView(this).apply {
                text = if (available > 0) "\u0043\u00f2\u006e $available \u0070\u0068\u00f2\u006e\u0067" else "\u0048\u1ebf\u0074 \u0070\u0068\u00f2\u006e\u0067"
                textSize = 10f
                setTextColor(0xFFFFFFFF.toInt())
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(dpToPx(8), dpToPx(2), dpToPx(8), dpToPx(2))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(if (available > 0) 0xFF1976D2.toInt() else 0xFF9E9E9E.toInt())
                    cornerRadius = dpToPx(4).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dpToPx(4) }
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

        layoutPagination.gravity = Gravity.CENTER

        val btnPrev = TextView(this).apply {
            text = "\u0054\u0072\u01b0\u1edb\u0063"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
            setTextColor(if (currentPage > 1) 0xFF1976D2.toInt() else 0xFFBDBDBD.toInt())
            if (currentPage > 1) {
                setOnClickListener {
                    this@SearchResultsActivity.currentPage = currentPage - 1
                    applySortAndDisplay(items)
                }
            }
        }
        layoutPagination.addView(btnPrev)

        val tvCurrent = TextView(this).apply {
            text = "$currentPage"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
            setTextColor(0xFFFFFFFF.toInt())
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xFF1976D2.toInt())
                cornerRadius = dpToPx(8).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = dpToPx(4)
                marginEnd = dpToPx(4)
            }
        }
        layoutPagination.addView(tvCurrent)

        val btnNext = TextView(this).apply {
            text = "Sau"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
            setTextColor(if (currentPage < totalPages) 0xFF1976D2.toInt() else 0xFFBDBDBD.toInt())
            if (currentPage < totalPages) {
                setOnClickListener {
                    this@SearchResultsActivity.currentPage = currentPage + 1
                    applySortAndDisplay(items)
                }
            }
        }
        layoutPagination.addView(btnNext)
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
