package com.example.doantotnghiep.View.Auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doantotnghiep.R
import com.example.doantotnghiep.View.Adapter.RoomAdapter
import com.example.doantotnghiep.ViewModel.UserProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import de.hdodenhof.circleimageview.CircleImageView

class UserProfileActivity : AppCompatActivity() {

    private val viewModel: UserProfileViewModel by viewModels()

    private lateinit var ivAvatar: CircleImageView
    private lateinit var tvName: TextView
    private lateinit var tvVerified: TextView
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
    private lateinit var toolbar: Toolbar

    private lateinit var roomAdapter: RoomAdapter
    private var userId: String = ""
    private var userPhone: String = ""

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
        toolbar = findViewById(R.id.toolbarProfile)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false) // Xoá title mặc định để không đè lên avatar
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRoomList() {
        roomAdapter = RoomAdapter(viewType = RoomAdapter.VIEW_TYPE_VERTICAL)
        rvRooms.layoutManager = LinearLayoutManager(this)
        rvRooms.adapter = roomAdapter
        rvRooms.isNestedScrollingEnabled = false
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

            if (user.isVerified) {
                tvVerified.visibility = View.VISIBLE
            }

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
            }

            tvEmail.text = if (user.email.isNotEmpty()) user.email else "Chưa cập nhật email"
            
            val roleStr = if (user.role == "landlord") "Chủ trọ" else if (user.role == "admin") "Quản trị viên" else "Khách thuê"
            val genderStr = if (user.gender.isNotEmpty()) " • ${user.gender}" else ""
            tvRole.text = "$roleStr$genderStr"
            
            if (user.createdAt > 0) {
                val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                val dateStr = sdf.format(java.util.Date(user.createdAt))
                tvJoinDate.text = "Tham gia: $dateStr"
            } else {
                tvJoinDate.text = "Tham gia: Không rõ"
            }

            // Nút Nhắn tin - Không hiện nếu là trang của chính mình
            val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            if (user.uid != myUid) {
                btnChat.visibility = View.VISIBLE
                btnChat.setOnClickListener {
                    val intent = Intent(this, ChatActivity::class.java)
                    intent.putExtra(ChatActivity.EXTRA_OTHER_UID, user.uid)
                    intent.putExtra(ChatActivity.EXTRA_OTHER_NAME, user.fullName)
                    intent.putExtra(ChatActivity.EXTRA_OTHER_AVATAR, user.avatarUrl)
                    startActivity(intent)
                }
            } else {
                btnChat.visibility = View.GONE
            }
        }

        viewModel.rooms.observe(this) { rooms ->
            tvRoomCount.text = "${rooms.size}"
            if (rooms.isEmpty()) {
                tvNoRooms.visibility = View.VISIBLE
                rvRooms.visibility = View.GONE
            } else {
                tvNoRooms.visibility = View.GONE
                rvRooms.visibility = View.VISIBLE
                roomAdapter.submitList(rooms)
            }
        }

        viewModel.errorMessage.observe(this) { msg ->
            if (msg.isNotEmpty()) {
                android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}
