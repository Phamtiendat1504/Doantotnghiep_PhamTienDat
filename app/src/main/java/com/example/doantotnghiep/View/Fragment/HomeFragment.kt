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
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doantotnghiep.R
import com.example.doantotnghiep.View.Adapter.RoomAdapter
import com.example.doantotnghiep.View.Auth.SearchResultsActivity
import com.example.doantotnghiep.ViewModel.HomeViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class HomeFragment : Fragment() {

    private lateinit var tvGreeting: TextView
    private lateinit var btnNotification: ImageView
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

    private lateinit var viewModel: HomeViewModel

    private val featuredAdapter = RoomAdapter(viewType = RoomAdapter.VIEW_TYPE_HORIZONTAL)
    private val newRoomsAdapter = RoomAdapter(viewType = RoomAdapter.VIEW_TYPE_VERTICAL)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvGreeting = view.findViewById(R.id.tvGreeting)
        btnNotification = view.findViewById(R.id.btnNotification)
        tvNoFeatured = view.findViewById(R.id.tvNoFeatured)
        tvNoNewRooms = view.findViewById(R.id.tvNoNewRooms)
        rvFeaturedRooms = view.findViewById(R.id.rvFeaturedRooms)
        rvNewRooms = view.findViewById(R.id.rvNewRooms)
        skeletonFeatured = view.findViewById(R.id.skeletonFeatured)
        skeletonNewRooms = view.findViewById(R.id.skeletonNewRooms)
        edtHomeSearch = view.findViewById(R.id.edtHomeSearch)
        btnHomeSearch = view.findViewById(R.id.btnHomeSearch)
        chipGroupPopularAreas = view.findViewById(R.id.chipGroupPopularAreas)
        btnLoadMore = view.findViewById(R.id.btnLoadMore)

        setupRecyclerViews()

        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        viewModel.userName.observe(viewLifecycleOwner) { name ->
            tvGreeting.text = "Chào, $name"
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

        viewModel.loadUserName()
        viewModel.loadPopularAreas()
        viewModel.loadFeaturedRooms()
        viewModel.loadNewRooms(isRefresh = true)

        btnLoadMore.setOnClickListener {
            val isLoading = viewModel.isLoadingMore.value ?: false
            val hasMore = viewModel.hasMoreRooms.value ?: false
            if (!isLoading && hasMore) viewModel.loadMoreNewRooms()
        }

        btnNotification.setOnClickListener {
            com.example.doantotnghiep.Utils.MessageUtils.showInfoDialog(requireContext(), "Thông báo", "Chức năng thông báo đang được phát triển, vui lòng quay lại sau!")
        }

        btnHomeSearch.setOnClickListener { performHomeSearch() }
        edtHomeSearch.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                performHomeSearch(); true
            } else false
        }
    }

    private fun performHomeSearch() {
        val query = edtHomeSearch.text.toString().trim()
        if (query.isEmpty()) { edtHomeSearch.error = "Vui lòng nhập từ khóa tìm kiếm"; return }
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

    override fun onResume() {
        super.onResume()
        if (!::viewModel.isInitialized) return
        if (viewModel.popularAreas.value.isNullOrEmpty()) viewModel.loadPopularAreas()
        if (viewModel.featuredRooms.value.isNullOrEmpty()) viewModel.loadFeaturedRooms()
        viewModel.loadNewRooms(isRefresh = true)
    }
}