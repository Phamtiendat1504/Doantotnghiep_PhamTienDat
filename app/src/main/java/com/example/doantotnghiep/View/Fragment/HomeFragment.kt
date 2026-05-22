package com.example.doantotnghiep.View.Fragment

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doantotnghiep.R
import com.example.doantotnghiep.View.Adapter.RoomAdapter
import com.example.doantotnghiep.View.Auth.SearchResultsActivity
import com.example.doantotnghiep.View.Auth.SearchProfileActivity
import com.example.doantotnghiep.ViewModel.HomeViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class HomeFragment : Fragment() {

    private lateinit var tvGreeting: TextView
    private lateinit var tvCurrentDate: TextView
    private lateinit var btnNotification: View
    private lateinit var tvNotificationBadge: TextView
    private lateinit var rvFeaturedRooms: RecyclerView
    private lateinit var rvNewRooms: RecyclerView
    private lateinit var tvNoFeatured: TextView
    private lateinit var tvNoNewRooms: TextView
    private lateinit var skeletonFeatured: View
    private lateinit var skeletonNewRooms: View
    private lateinit var edtHomeSearch: EditText
    private lateinit var btnHomeSearch: ImageView
    private lateinit var chipGroupPopularAreas: ChipGroup
    private lateinit var btnLoadMore: TextView
    private lateinit var layoutRecentSearch: LinearLayout
    private lateinit var scrollRecentSearch: View
    private lateinit var swipeRefreshLayout: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    
    // New Search History Dropdown Views
    private lateinit var cardRecentSearch: View
    private lateinit var layoutRecentSearchVertical: LinearLayout
    private lateinit var btnClearAllSearch: TextView
    private lateinit var homeNestedScrollView: androidx.core.widget.NestedScrollView

    private lateinit var viewModel: HomeViewModel

    private val featuredAdapter = RoomAdapter(viewType = RoomAdapter.VIEW_TYPE_HORIZONTAL)
    private val newRoomsAdapter = RoomAdapter(
        viewType = RoomAdapter.VIEW_TYPE_VERTICAL,
        showAvailabilityBadge = true
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvGreeting = view.findViewById(R.id.tvGreetingLabel)
        tvCurrentDate = view.findViewById(R.id.tvCurrentDate)
        btnNotification = view.findViewById(R.id.btnNotificationContainer)
        tvNotificationBadge = view.findViewById(R.id.tvNotificationBadge)
        tvNoFeatured = view.findViewById(R.id.tvNoFeatured)
        tvNoNewRooms = view.findViewById(R.id.tvNoNewRooms)
        rvFeaturedRooms = view.findViewById(R.id.rvFeaturedRooms)
        rvNewRooms = view.findViewById(R.id.rvNewRooms)
        skeletonFeatured = view.findViewById(R.id.skeletonFeatured)
        skeletonNewRooms = view.findViewById(R.id.skeletonNewRooms)
        edtHomeSearch = view.findViewById(R.id.edtHomeSearch)
        // Icon tìm kiếm đã bị xóa, dùng Label TÌM thay thế
        val btnSearchLabel: TextView = view.findViewById(R.id.btnSearchLabel)
        chipGroupPopularAreas = view.findViewById(R.id.chipGroupPopularAreas)
        btnLoadMore = view.findViewById(R.id.btnLoadMore)
        layoutRecentSearch = view.findViewById(R.id.layoutRecentSearch)
        scrollRecentSearch = view.findViewById(R.id.scrollRecentSearch)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        
        // Bind new search history components
        cardRecentSearch = view.findViewById(R.id.cardRecentSearch)
        layoutRecentSearchVertical = view.findViewById(R.id.layoutRecentSearchVertical)
        btnClearAllSearch = view.findViewById(R.id.btnClearAllSearch)
        homeNestedScrollView = view.findViewById(R.id.homeNestedScrollView)

        setupSwipeRefresh()
        setupRecyclerViews()
        setupSearchHistory()

        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        viewModel.userName.observe(viewLifecycleOwner) { name ->
            view.findViewById<TextView>(R.id.tvUserName).text = name
        }

        viewModel.greeting.observe(viewLifecycleOwner) { greeting ->
            tvGreeting.text = greeting
        }

        viewModel.currentDate.observe(viewLifecycleOwner) { date ->
            tvCurrentDate.text = date
        }

        viewModel.popularAreas.observe(viewLifecycleOwner) { areas ->
            if (!isAdded) return@observe
            chipGroupPopularAreas.removeAllViews()
            for ((district, count) in areas) {
                val chip = Chip(requireContext()).apply {
                    text = "$district ($count)"
                    isClickable = true; isFocusable = true
                    setOnClickListener {
                        val intent = Intent(requireContext(), SearchResultsActivity::class.java)
                        intent.putExtra("district", district)
                        intent.putExtra("searchMode", "district")
                        intent.putExtra("ward", "")
                        intent.putExtra("minPrice", 0L)
                        intent.putExtra("maxPrice", 0L)
                        startActivity(intent)
                    }
                }
                chipGroupPopularAreas.addView(chip)
            }
        }

        viewModel.isLoadingFeatured.observe(viewLifecycleOwner) { isLoading ->
            skeletonFeatured.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) rvFeaturedRooms.visibility = View.GONE
            checkRefreshStatus()
        }

        viewModel.featuredRooms.observe(viewLifecycleOwner) { rooms ->
            if (!isAdded) return@observe
            skeletonFeatured.visibility = View.GONE
            if (rooms.isEmpty()) {
                tvNoFeatured.visibility = View.VISIBLE
                rvFeaturedRooms.visibility = View.GONE
            } else {
                tvNoFeatured.visibility = View.GONE
                rvFeaturedRooms.visibility = View.VISIBLE
                featuredAdapter.submitList(rooms)
            }
        }

        viewModel.isLoadingNew.observe(viewLifecycleOwner) { isLoading ->
            skeletonNewRooms.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) { rvNewRooms.visibility = View.GONE; btnLoadMore.visibility = View.GONE }
            checkRefreshStatus()
        }

        viewModel.newRooms.observe(viewLifecycleOwner) { rooms ->
            if (!isAdded) return@observe
            skeletonNewRooms.visibility = View.GONE
            if (rooms.isEmpty()) {
                tvNoNewRooms.visibility = View.VISIBLE
                rvNewRooms.visibility = View.GONE
                btnLoadMore.visibility = View.GONE
            } else {
                tvNoNewRooms.visibility = View.GONE
                rvNewRooms.visibility = View.VISIBLE
                newRoomsAdapter.submitList(rooms)
            }
        }

        viewModel.hasMoreRooms.observe(viewLifecycleOwner) { hasMore ->
            val loading = viewModel.isLoadingNew.value ?: false
            btnLoadMore.visibility = if (hasMore && !loading) View.VISIBLE else View.GONE
        }

        viewModel.isLoadingMore.observe(viewLifecycleOwner) { isLoading ->
            btnLoadMore.text = if (isLoading) "Đang tải..." else "Xem thêm"
        }

        viewModel.notificationBadgeCount.observe(viewLifecycleOwner) { count ->
            if (!isAdded) return@observe
            if (count > 0) {
                tvNotificationBadge.text = if (count > 99) "99+" else count.toString()
                tvNotificationBadge.visibility = View.VISIBLE
            } else {
                tvNotificationBadge.visibility = View.GONE
            }
        }

        viewModel.loadUserName()
        viewModel.loadPopularAreas()
        viewModel.loadFeaturedRooms()
        viewModel.loadNewRooms(isRefresh = true, force = true)
        viewModel.loadNotificationBadge()

        btnLoadMore.setOnClickListener {
            val isLoading = viewModel.isLoadingMore.value ?: false
            val hasMore = viewModel.hasMoreRooms.value ?: false
            if (!isLoading && hasMore) viewModel.loadMoreNewRooms()
        }

        val navigateToNotifications = {
            val intent = android.content.Intent(requireContext(),
                com.example.doantotnghiep.View.Auth.NotificationsActivity::class.java)
            startActivity(intent)
        }

        btnNotification.setOnClickListener { navigateToNotifications() }
        view.findViewById<View>(R.id.btnNotification)?.setOnClickListener { navigateToNotifications() }

        btnSearchLabel.setOnClickListener { performHomeSearch() }
        edtHomeSearch.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                performHomeSearch(); true
            } else false
        }

        // Nút thẻ (Banner) tìm kiếm trang cá nhân người dùng
        view.findViewById<View>(R.id.btnSearchProfileView)?.setOnClickListener {
            startActivity(Intent(requireContext(), SearchProfileActivity::class.java))
        }
        view.findViewById<View>(R.id.btnSearchProfile)?.setOnClickListener {
            startActivity(Intent(requireContext(), SearchProfileActivity::class.java))
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.primary))
        swipeRefreshLayout.setOnRefreshListener {
            refreshAllData()
        }
    }

    private fun refreshAllData() {
        viewModel.loadUserName()
        viewModel.loadPopularAreas()
        viewModel.loadFeaturedRooms()
        viewModel.loadNewRooms(isRefresh = true, force = true)
        viewModel.loadNotificationBadge()
    }

    private fun checkRefreshStatus() {
        val loadingFeatured = viewModel.isLoadingFeatured.value ?: false
        val loadingNew = viewModel.isLoadingNew.value ?: false
        if (!loadingFeatured && !loadingNew) {
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(edtHomeSearch.windowToken, 0)
    }

    private fun setupSearchHistory() {
        val sharedPref = requireContext().getSharedPreferences("SearchHistory", android.content.Context.MODE_PRIVATE)
        
        fun updateHistoryUI() {
            val history = sharedPref.getString("queries", "") ?: ""
            val queries = if (history.isEmpty()) emptyList() else history.split("|").filter { it.isNotBlank() }
            
            if (queries.isEmpty() || !edtHomeSearch.hasFocus()) {
                cardRecentSearch.visibility = View.GONE
            } else {
                cardRecentSearch.visibility = View.VISIBLE
                layoutRecentSearchVertical.removeAllViews()
                queries.forEach { query ->
                    val itemView = LayoutInflater.from(requireContext()).inflate(R.layout.item_search_history, layoutRecentSearchVertical, false)
                    
                    val tvQuery = itemView.findViewById<TextView>(R.id.tvSearchQuery)
                    val btnDelete = itemView.findViewById<View>(R.id.btnDeleteQuery)
                    
                    tvQuery.text = query
                    
                    // Click on search term executes search
                    itemView.setOnClickListener {
                        edtHomeSearch.setText(query)
                        performHomeSearch()
                        edtHomeSearch.clearFocus()
                        hideKeyboard()
                    }
                    
                    // Click on X delete button deletes the item
                    btnDelete.setOnClickListener {
                        val currentHistory = sharedPref.getString("queries", "") ?: ""
                        val updatedQueries = currentHistory.split("|").filter { it.isNotBlank() && it != query }
                        val newHistory = updatedQueries.joinToString("|")
                        sharedPref.edit().putString("queries", newHistory).apply()
                        updateHistoryUI()
                    }
                    
                    layoutRecentSearchVertical.addView(itemView)
                }
            }
        }

        // Click Xóa tất cả (Clear All)
        btnClearAllSearch.setOnClickListener {
            sharedPref.edit().putString("queries", "").apply()
            updateHistoryUI()
            edtHomeSearch.clearFocus()
            hideKeyboard()
        }

        // Lắng nghe sự kiện focus để hiện/ẩn lịch sử
        edtHomeSearch.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                updateHistoryUI()
            } else {
                // Post delayed to avoid consuming touch events inside dropdown before visibility changes to GONE
                edtHomeSearch.postDelayed({
                    if (!isAdded) return@postDelayed
                    if (!edtHomeSearch.hasFocus()) {
                        cardRecentSearch.visibility = View.GONE
                    }
                }, 200)
            }
        }

        // Tự động ẩn khi người dùng bắt đầu cuộn trang chủ
        homeNestedScrollView.setOnScrollChangeListener(androidx.core.widget.NestedScrollView.OnScrollChangeListener { _, _, _, _, _ ->
            if (edtHomeSearch.hasFocus()) {
                edtHomeSearch.clearFocus()
                hideKeyboard()
            }
        })
    }

    private fun saveSearchQuery(query: String) {
        val sharedPref = requireContext().getSharedPreferences("SearchHistory", android.content.Context.MODE_PRIVATE)
        val history = sharedPref.getString("queries", "") ?: ""
        val queries = history.split("|").toMutableList().filter { it.isNotBlank() && it != query }
        val newHistory = (listOf(query) + queries).take(5).joinToString("|")
        sharedPref.edit().putString("queries", newHistory).apply()
    }

    private fun performHomeSearch() {
        val query = edtHomeSearch.text.toString().trim()
        if (query.isEmpty()) { edtHomeSearch.error = "Vui lòng nhập từ khóa tìm kiếm"; return }
        
        saveSearchQuery(query) // Lưu lịch sử
        
        edtHomeSearch.clearFocus()
        hideKeyboard()

        val intent = Intent(requireContext(), SearchResultsActivity::class.java)
        intent.putExtra("query", query)
        startActivity(intent)
    }

    private fun setupRecyclerViews() {
        rvFeaturedRooms.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvFeaturedRooms.adapter = featuredAdapter
        rvNewRooms.layoutManager = LinearLayoutManager(requireContext())
        rvNewRooms.adapter = newRoomsAdapter
        rvNewRooms.isNestedScrollingEnabled = false
    }

    private var lastRoomLoadTime = 0L
    private val roomReloadIntervalMs = 60_000L // tải lại tối đa 1 lần/phút

    override fun onResume() {
        super.onResume()
        if (!::viewModel.isInitialized) return
        if (viewModel.popularAreas.value.isNullOrEmpty()) viewModel.loadPopularAreas()
        if (viewModel.featuredRooms.value.isNullOrEmpty()) viewModel.loadFeaturedRooms()

        val now = System.currentTimeMillis()
        if (now - lastRoomLoadTime > roomReloadIntervalMs) {
            viewModel.loadNewRooms(isRefresh = true)
            lastRoomLoadTime = now
        }

        viewModel.loadNotificationBadge()

        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val mainViewModel = androidx.lifecycle.ViewModelProvider(requireActivity())[com.example.doantotnghiep.ViewModel.MainViewModel::class.java]
            mainViewModel.loadAppointmentBadgeForCurrentUser(currentUser.uid)
        }
    }
}
