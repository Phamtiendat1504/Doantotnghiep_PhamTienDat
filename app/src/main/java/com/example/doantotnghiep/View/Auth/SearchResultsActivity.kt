package com.example.doantotnghiep.View.Auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doantotnghiep.R
import com.example.doantotnghiep.View.Adapter.RoomAdapter
import com.example.doantotnghiep.ViewModel.SearchViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.ceil

class SearchResultsActivity : AppCompatActivity() {

    private lateinit var recyclerViewResults: RecyclerView
    private lateinit var layoutPagination: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var tvResultCount: TextView
    private lateinit var tvSearchArea: TextView
    private lateinit var btnBack: ImageView

    private lateinit var viewModel: SearchViewModel
    private lateinit var roomAdapter: RoomAdapter

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

        savedInstanceState?.getInt("currentSortOrdinal", SortType.NONE.ordinal)?.let {
            currentSort = SortType.values()[it]
        }

        recyclerViewResults = findViewById(R.id.recyclerViewResults)
        layoutPagination = findViewById(R.id.layoutPagination)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)
        tvResultCount = findViewById(R.id.tvResultCount)
        tvSearchArea = findViewById(R.id.tvSearchArea)
        btnBack = findViewById(R.id.btnBack)

        tvCurrentSort = findViewById(R.id.tvCurrentSort)
        btnSortDropdown = findViewById(R.id.btnSortDropdown)
        ivSortArrow = findViewById(R.id.ivSortArrow)

        // Setup RecyclerView & Adapter
        roomAdapter = RoomAdapter(
            viewType = RoomAdapter.VIEW_TYPE_VERTICAL,
            showAvailabilityBadge = true
        )
        recyclerViewResults.layoutManager = LinearLayoutManager(this)
        recyclerViewResults.adapter = roomAdapter

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
        val curfew = intent.getStringExtra("curfew") ?: ""
        val maxWifiPrice = intent.getLongExtra("maxWifiPrice", 0L)
        val maxElectricPrice = intent.getLongExtra("maxElectricPrice", 0L)
        val maxWaterPrice = intent.getLongExtra("maxWaterPrice", 0L)

        // ── Tìm theo bài đăng cụ thể (user chọn 1 phòng từ panel) ──
        if (searchMode == "exact_post") {
            val postId     = intent.getStringExtra("postId").orEmpty()
            val mapAddress = intent.getStringExtra("mapAddress").orEmpty()
            tvSearchArea.text = mapAddress.ifEmpty { "Bài đăng đã chọn" }
            viewModel.searchByPostId(postId)
            return
        }

        // ── Tìm theo nhiều bài đăng (user chọn nhiều địa chỉ từ panel) ──
        if (searchMode == "selected_posts") {
            val postIds    = intent.getStringArrayListExtra("postIds") ?: arrayListOf()
            val mapAddress = intent.getStringExtra("mapAddress").orEmpty()
            tvSearchArea.text = mapAddress.ifEmpty { "${postIds.size} địa chỉ đã chọn" }
            viewModel.searchByPostIds(postIds)
            return
        }

        // ── Tìm theo bán kính bản đồ ──
        if (searchMode == "nearby") {
            val lat = intent.getDoubleExtra("lat", 0.0)
            val lng = intent.getDoubleExtra("lng", 0.0)
            val radiusKm = intent.getDoubleExtra("radiusKm", 2.0)
            val mapAddress = intent.getStringExtra("mapAddress").orEmpty()

            val radiusLabel = if (radiusKm == radiusKm.toLong().toDouble())
                "${radiusKm.toInt()}km" else "${radiusKm}km"
            tvSearchArea.text = if (mapAddress.isNotEmpty())
                "$mapAddress — trong ${radiusLabel}"
            else
                "Trong vòng $radiusLabel từ vị trí chọn"

            viewModel.searchNearby(
                lat = lat, lng = lng, radiusKm = radiusKm,
                minPrice = minPrice, maxPrice = maxPrice,
                minArea = minArea, maxArea = maxArea,
                desiredPeople = desiredPeople,
                roomType = roomType,
                hasWifi = hasWifi, hasElectric = hasElectric, hasWater = hasWater,
                curfew = curfew,
                maxWifiPrice = maxWifiPrice, maxElectricPrice = maxElectricPrice, maxWaterPrice = maxWaterPrice
            )
            return
        }

        tvSearchArea.text = when {
            query.isNotEmpty() -> "\"$query\""
            searchMode == "district" && district.isNotEmpty() -> district
            ward.isBlank() && district.isBlank() -> "Tất cả khu vực"
            ward.isNotBlank() && district.isNotBlank() -> "$ward ($district)"
            ward.isNotBlank() -> ward
            else -> district
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
                hasWater = hasWater,
                curfew = curfew,
                maxWifiPrice = maxWifiPrice,
                maxElectricPrice = maxElectricPrice,
                maxWaterPrice = maxWaterPrice
            )
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
                roomAdapter.submitList(emptyList())
                layoutPagination.removeAllViews()
            } else {
                tvEmpty.visibility = View.GONE
                currentPage = 1
                applySortAndDisplay(results)
            }
        }

        viewModel.errorMessage.observe(this) { msg ->
            if (!msg.isNullOrEmpty()) {
                viewModel.resetErrorMessage()
                tvEmpty.visibility = View.VISIBLE
                tvEmpty.text = msg
                tvResultCount.text = "Kết quả tìm kiếm: 0 bài đăng"
                roomAdapter.submitList(emptyList())
                layoutPagination.removeAllViews()
            }
        }
    }

    private fun showSortMenu() {
        val popup = androidx.appcompat.widget.PopupMenu(this, btnSortDropdown)
        popup.menu.add("Phù hợp nhất")
        popup.menu.add("Mới nhất")
        popup.menu.add("Cũ nhất")
        popup.menu.add("Giá thấp đến cao")
        popup.menu.add("Giá cao đến thấp")

        popup.setOnMenuItemClickListener { item ->
            val newSort = when (item.title.toString()) {
                "Phù hợp nhất" -> SortType.NONE
                "Giá thấp đến cao" -> SortType.PRICE_ASC
                "Giá cao đến thấp" -> SortType.PRICE_DESC
                "Mới nhất" -> SortType.NEWEST
                "Cũ nhất" -> SortType.OLDEST
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
            SortType.NONE -> "Phù hợp nhất"
            SortType.PRICE_ASC -> "Giá thấp đến cao"
            SortType.PRICE_DESC -> "Giá cao đến thấp"
            SortType.NEWEST -> "Mới nhất"
            SortType.OLDEST -> "Cũ nhất"
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

        // Map data class SearchViewModel.RoomItem -> Adapter's RoomItem
        val adapterItems = pageItems.map { searchItem ->
            com.example.doantotnghiep.View.Adapter.RoomItem(
                id = searchItem.roomId,
                title = searchItem.title,
                price = searchItem.price,
                ward = searchItem.ward,
                district = searchItem.district,
                area = searchItem.area,
                imageUrl = searchItem.firstImage,
                isAvailable = (searchItem.roomCount - searchItem.rentedCount) > 0,
                createdAt = searchItem.createdAt,
                isFeatured = searchItem.isFeatured
            )
        }

        roomAdapter.submitList(adapterItems)

        displayPagination(page, totalPages, sorted)
    }

    private fun displayPagination(currentPage: Int, totalPages: Int, items: List<SearchViewModel.RoomItem>) {
        layoutPagination.removeAllViews()
        if (totalPages <= 1) return

        layoutPagination.gravity = android.view.Gravity.CENTER

        val btnPrev = TextView(this).apply {
            text = "Trước"
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
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply {
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currentSortOrdinal", currentSort.ordinal)
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
