package com.example.doantotnghiep.View.Auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doantotnghiep.R
import com.example.doantotnghiep.View.Adapter.RoomAdapter
import com.example.doantotnghiep.View.Adapter.RoomItem
import com.example.doantotnghiep.ViewModel.UserProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import de.hdodenhof.circleimageview.CircleImageView
import kotlin.math.min
import android.graphics.Typeface

class UserProfileActivity : AppCompatActivity() {

    private val viewModel: UserProfileViewModel by viewModels()

    private lateinit var ivAvatar: CircleImageView
    private lateinit var tvName: TextView
    private lateinit var tvVerified: LinearLayout
    private lateinit var tvPhone: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvRole: TextView
    private lateinit var tvJoinDate: TextView
    private lateinit var btnCall: TextView
    private lateinit var btnChat: com.google.android.material.button.MaterialButton
    private lateinit var rvRooms: RecyclerView
    private lateinit var progressRooms: ProgressBar
    private lateinit var tvNoRooms: TextView
    private lateinit var tvRoomCount: TextView
    private lateinit var spinnerRoomSort: Spinner
    private lateinit var btnPrevRoomPage: TextView
    private lateinit var tvRoomPageInfo: TextView
    private lateinit var btnNextRoomPage: TextView
    private lateinit var layoutRoomControls: View
    private lateinit var layoutRoomPagination: View
    private lateinit var toolbar: Toolbar

    private lateinit var roomAdapter: RoomAdapter
    private val allRooms = mutableListOf<RoomItem>()
    private var currentPage = 1
    private val pageSize = 10
    private var selectedSort = SortMode.NEWEST
    private var userId: String = ""
    private var userPhone: String = ""

    private enum class SortMode {
        NEWEST,
        OLDEST,
        PRICE_ASC,
        PRICE_DESC,
        AREA_ASC,
        AREA_DESC,
        TITLE_ASC
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        userId = intent.getStringExtra("userId") ?: run {
            finish()
            return
        }

        initViews()
        setupToolbar()
        setupRoomList()
        observeViewModel()
        viewModel.loadProfile(userId)
    }

    private fun initViews() {
        ivAvatar = findViewById(R.id.ivProfileAvatar)
        tvName = findViewById(R.id.tvProfileName)
        tvVerified = findViewById(R.id.tvProfileVerified)
        tvPhone = findViewById(R.id.tvProfilePhone)
        tvEmail = findViewById(R.id.tvProfileEmail)
        tvRole = findViewById(R.id.tvProfileRole)
        tvJoinDate = findViewById(R.id.tvProfileJoinDate)
        btnCall = findViewById(R.id.btnCallProfile)
        btnChat = findViewById(R.id.btnChatWithUser)
        rvRooms = findViewById(R.id.rvProfileRooms)
        progressRooms = findViewById(R.id.progressRooms)
        tvNoRooms = findViewById(R.id.tvNoRooms)
        tvRoomCount = findViewById(R.id.tvRoomCount)
        spinnerRoomSort = findViewById(R.id.spinnerRoomSort)
        btnPrevRoomPage = findViewById(R.id.btnPrevRoomPage)
        tvRoomPageInfo = findViewById(R.id.tvRoomPageInfo)
        btnNextRoomPage = findViewById(R.id.btnNextRoomPage)
        layoutRoomControls = findViewById(R.id.layoutRoomControls)
        layoutRoomPagination = findViewById(R.id.layoutRoomPagination)
        toolbar = findViewById(R.id.toolbarProfile)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false) // Xoá title mặc định để không đè lên avatar
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRoomList() {
        roomAdapter = RoomAdapter(
            viewType = RoomAdapter.VIEW_TYPE_VERTICAL,
            showAvailabilityBadge = true
        )
        rvRooms.layoutManager = LinearLayoutManager(this)
        rvRooms.adapter = roomAdapter
        rvRooms.isNestedScrollingEnabled = false
        setupSortAndPaginationControls()
    }

    private fun setupSortAndPaginationControls() {
        val labels = listOf(
            "Mới nhất",
            "Cũ nhất",
            "Giá tăng dần",
            "Giá giảm dần",
            "Diện tích tăng dần",
            "Diện tích giảm dần",
            "Tên A-Z"
        )

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRoomSort.adapter = spinnerAdapter

        spinnerRoomSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedSort = when (position) {
                    0 -> SortMode.NEWEST
                    1 -> SortMode.OLDEST
                    2 -> SortMode.PRICE_ASC
                    3 -> SortMode.PRICE_DESC
                    4 -> SortMode.AREA_ASC
                    5 -> SortMode.AREA_DESC
                    else -> SortMode.TITLE_ASC
                }
                currentPage = 1
                renderRooms()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        btnPrevRoomPage.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                renderRooms()
            }
        }

        btnNextRoomPage.setOnClickListener {
            val totalPages = calculateTotalPages(allRooms.size)
            if (currentPage < totalPages) {
                currentPage++
                renderRooms()
            }
        }
    }

    private fun calculateTotalPages(totalItems: Int): Int {
        if (totalItems <= 0) return 1
        return ((totalItems - 1) / pageSize) + 1
    }

    private fun sortRooms(items: List<RoomItem>): List<RoomItem> {
        return when (selectedSort) {
            SortMode.NEWEST -> items.sortedByDescending { it.createdAt }
            SortMode.OLDEST -> items.sortedBy { it.createdAt }
            SortMode.PRICE_ASC -> items.sortedBy { it.price }
            SortMode.PRICE_DESC -> items.sortedByDescending { it.price }
            SortMode.AREA_ASC -> items.sortedBy { it.area }
            SortMode.AREA_DESC -> items.sortedByDescending { it.area }
            SortMode.TITLE_ASC -> items.sortedBy { it.title.lowercase() }
        }
    }

    private fun renderRooms() {
        val sorted = sortRooms(allRooms)
        val totalPages = calculateTotalPages(sorted.size)

        if (currentPage > totalPages) currentPage = totalPages
        if (currentPage < 1) currentPage = 1

        if (sorted.isEmpty()) {
            tvNoRooms.visibility = View.VISIBLE
            rvRooms.visibility = View.GONE
            layoutRoomControls.visibility = View.GONE
            layoutRoomPagination.visibility = View.GONE
            roomAdapter.submitList(emptyList())
            return
        }

        layoutRoomControls.visibility = View.VISIBLE
        layoutRoomPagination.visibility = View.VISIBLE
        tvNoRooms.visibility = View.GONE
        rvRooms.visibility = View.VISIBLE

        val fromIndex = (currentPage - 1) * pageSize
        val toIndex = min(fromIndex + pageSize, sorted.size)
        val pageItems = sorted.subList(fromIndex, toIndex)
        roomAdapter.submitList(pageItems)

        tvRoomPageInfo.text = "Trang $currentPage/$totalPages"

        val canPrev = currentPage > 1
        btnPrevRoomPage.isEnabled = canPrev
        btnPrevRoomPage.alpha = if (canPrev) 1f else 0.5f

        val canNext = currentPage < totalPages
        btnNextRoomPage.isEnabled = canNext
        btnNextRoomPage.alpha = if (canNext) 1f else 0.5f
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { loading ->
            progressRooms.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.userInfo.observe(this) { user ->
            tvName.text = user.fullName.ifEmpty { "Người dùng" }

            if (user.avatarUrl.isNotEmpty()) {
                Glide.with(this).load(user.avatarUrl)
                    .placeholder(R.drawable.ic_person).into(ivAvatar)
            }

            val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

            // Dữ liệu userInfo từ viewModel (qua getUserById) đã sử dụng Source.SERVER 
            // nên chắc chắn là dữ liệu mới nhất. Không cần call Firestore lần 2.
            applyUserDisplay(user, myUid)
        }

        viewModel.rooms.observe(this) { rooms ->
            allRooms.clear()
            allRooms.addAll(rooms)
            tvRoomCount.text = "${allRooms.size}"
            currentPage = 1
            renderRooms()
        }

        viewModel.errorMessage.observe(this) { msg ->
            if (msg.isNotEmpty()) {
                android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Áp dụng toàn bộ thông tin hiển thị của user lên UI.
     * Tách ra để có thể gọi sau khi xác nhận isVerified chính xác từ server.
     */
    private fun applyUserDisplay(user: com.example.doantotnghiep.Model.User, myUid: String) {
        // Badge xác minh ở header
        tvVerified.visibility = if (user.isVerified) View.VISIBLE else View.GONE

        // Số điện thoại
        if (user.phone.isNotEmpty()) {
            userPhone = user.phone
            tvPhone.text = user.phone
            btnCall.visibility = View.VISIBLE
            btnCall.setOnClickListener {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${user.phone}"))
                startActivity(intent)
            }
        } else {
            tvPhone.text = "Chưa cập nhật số điện thoại"
            btnCall.visibility = View.GONE
        }

        // Email
        tvEmail.text = if (user.email.isNotEmpty()) user.email else "Chưa cập nhật email"

        // Trạng thái tài khoản
        val roleStr = when {
            user.role == "admin" -> "Quản trị viên"
            user.isVerified -> "Tài khoản đã xác minh"
            else -> "Tài khoản chưa xác minh"
        }
        val genderStr = if (user.gender.isNotEmpty()) " • ${user.gender}" else ""
        tvRole.text = "$roleStr$genderStr"

        // Ngày tham gia
        if (user.createdAt > 0) {
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            tvJoinDate.text = "Tham gia: ${sdf.format(java.util.Date(user.createdAt))}"
        } else {
            tvJoinDate.text = "Tham gia: Không rõ"
        }

        // Nút Nhắn tin — ẩn khi xem hồ sơ chính mình
        if (user.uid != myUid) {
            btnChat.visibility = View.VISIBLE
            btnChat.setOnClickListener {
                if (myUid.isEmpty()) {
                    androidx.appcompat.app.AlertDialog.Builder(this@UserProfileActivity)
                        .setTitle("Yêu cầu đăng nhập")
                        .setMessage("Bạn cần đăng nhập để sử dụng tính năng này.")
                        .setPositiveButton("Đăng nhập") { _, _ ->
                            startActivity(Intent(this@UserProfileActivity, LoginActivity::class.java))
                        }
                        .setNegativeButton("Hủy", null)
                        .show()
                } else {
                    val intent = Intent(this@UserProfileActivity, ChatActivity::class.java)
                    intent.putExtra(ChatActivity.EXTRA_OTHER_UID, user.uid)
                    intent.putExtra(ChatActivity.EXTRA_OTHER_NAME, user.fullName)
                    intent.putExtra(ChatActivity.EXTRA_OTHER_AVATAR, user.avatarUrl)
                    startActivity(intent)
                }
            }
        } else {
            btnChat.visibility = View.GONE
        }
    }
}

