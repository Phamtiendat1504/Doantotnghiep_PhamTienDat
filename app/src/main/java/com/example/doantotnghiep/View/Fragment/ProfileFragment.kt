package com.example.doantotnghiep.View.Fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.View.Auth.*
import com.example.doantotnghiep.ViewModel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {

    private lateinit var imgAvatar: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var btnPersonalInfo: LinearLayout
    private lateinit var btnChangePassword: LinearLayout
    private lateinit var btnSavedPosts: LinearLayout
    private lateinit var btnAppointments: LinearLayout
    private lateinit var tvAppointmentBadge: TextView
    private lateinit var btnLogout: LinearLayout
    private lateinit var btnNotification: ImageView
    private lateinit var btnChangeAvatar: androidx.cardview.widget.CardView
    private lateinit var cardMyPosts: androidx.cardview.widget.CardView
    private lateinit var btnMyPosts: LinearLayout
    private lateinit var tvMyPostsBadge: TextView
    private lateinit var tvRoleBadge: TextView
    private lateinit var layoutGuest: android.widget.ScrollView
    private lateinit var layoutProfile: android.widget.ScrollView

    private lateinit var viewModel: ProfileViewModel
    private var currentAvatarUrl: String = ""

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { viewModel.uploadAvatar(it) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)

        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]

        // Dùng isLoggedIn() từ ViewModel thay vì gọi FirebaseAuth trực tiếp
        if (!viewModel.isLoggedIn()) {
            layoutGuest.visibility = View.VISIBLE
            layoutProfile.visibility = View.GONE
            setupGuestListeners()
        } else {
            layoutGuest.visibility = View.GONE
            layoutProfile.visibility = View.VISIBLE
            setupClickListeners()
            setupObservers()
            viewModel.loadUserInfo()
            viewModel.loadAppointmentBadge()
        }
    }

    private fun setupObservers() {
        viewModel.userInfo.observe(viewLifecycleOwner) { info ->
            if (!isAdded) return@observe
            tvUserName.text = info.fullName
            tvUserEmail.text = info.email
            currentAvatarUrl = info.avatarUrl
            if (currentAvatarUrl.isNotEmpty()) {
                imgAvatar.setPadding(0, 0, 0, 0)
                imgAvatar.imageTintList = null
                Glide.with(this).load(currentAvatarUrl).circleCrop().placeholder(R.drawable.ic_person).into(imgAvatar)
            }
            val role = info.role
            val isVerified = info.isVerified
            when {
                role == "landlord" && isVerified -> {
                    tvRoleBadge.text = "✓ Chủ trọ đã xác minh"
                    tvRoleBadge.setTextColor(0xFF2E7D32.toInt())
                    tvRoleBadge.setBackgroundResource(R.drawable.bg_badge_landlord)
                }
                role == "admin" -> {
                    tvRoleBadge.text = "★ Quản trị viên"
                    tvRoleBadge.setTextColor(0xFF1976D2.toInt())
                    tvRoleBadge.setBackgroundResource(R.drawable.bg_badge_tenant)
                }
                else -> {
                    // tenant or unverified landlord — observe verificationStatus
                }
            }
            cardMyPosts.visibility = if (role == "landlord" || role == "admin") View.VISIBLE else View.GONE
            btnSavedPosts.visibility = if (role == "landlord" || role == "admin") View.GONE else View.VISIBLE
        }

        viewModel.verificationStatus.observe(viewLifecycleOwner) { status ->
            if (!isAdded) return@observe
            when (status) {
                "pending" -> {
                    tvRoleBadge.text = "⏳ Đang chờ xác minh"
                    tvRoleBadge.setTextColor(0xFFE65100.toInt())
                    tvRoleBadge.setBackgroundResource(R.drawable.bg_badge_pending)
                }
                "rejected" -> {
                    tvRoleBadge.text = "✗ Xác minh bị từ chối"
                    tvRoleBadge.setTextColor(0xFFD32F2F.toInt())
                    tvRoleBadge.setBackgroundResource(R.drawable.bg_badge_pending)
                }
                else -> {
                    tvRoleBadge.text = "Người thuê trọ"
                    tvRoleBadge.setTextColor(0xFF666666.toInt())
                    tvRoleBadge.setBackgroundResource(R.drawable.bg_badge_tenant)
                }
            }
        }

        viewModel.myPostsBadgeCount.observe(viewLifecycleOwner) { count ->
            if (!isAdded) return@observe
            if (count > 0) {
                tvMyPostsBadge.text = if (count > 99) "99+" else count.toString()
                tvMyPostsBadge.visibility = View.VISIBLE
            } else {
                tvMyPostsBadge.visibility = View.GONE
            }
        }

        viewModel.appointmentBadgeCount.observe(viewLifecycleOwner) { count ->
            if (!isAdded) return@observe
            if (count > 0) {
                tvAppointmentBadge.text = if (count > 99) "99+" else count.toString()
                tvAppointmentBadge.visibility = View.VISIBLE
            } else {
                tvAppointmentBadge.visibility = View.GONE
            }
        }

        viewModel.newAvatarUrl.observe(viewLifecycleOwner) { url ->
            if (!isAdded || url.isNullOrEmpty()) return@observe
            currentAvatarUrl = url
            Glide.with(this).load(url).circleCrop().into(imgAvatar)
            imgAvatar.setPadding(0, 0, 0, 0)
            imgAvatar.imageTintList = null
            MessageUtils.showSuccessDialog(requireContext(), "Cập nhật thành công", "Ảnh đại diện đã được cập nhật")
        }

        viewModel.isUploadingAvatar.observe(viewLifecycleOwner) { isUploading ->
            if (isUploading == true && isAdded) {
                MessageUtils.showInfoDialog(requireContext(), "Đang xử lý", "Ảnh đang được tải lên, vui lòng chờ...")
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrEmpty() && isAdded) {
                MessageUtils.showErrorDialog(requireContext(), "Lỗi cập nhật ảnh", error)
            }
        }
    }

    private fun setupGuestListeners() {
        view?.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnGuestLogin)?.setOnClickListener {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
        }
        view?.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnGuestRegister)?.setOnClickListener {
            startActivity(Intent(requireContext(), RegisterActivity::class.java))
        }
    }

    private fun initViews(view: View) {
        layoutGuest = view.findViewById(R.id.layoutGuest)
        layoutProfile = view.findViewById(R.id.layoutProfile)
        imgAvatar = view.findViewById(R.id.imgAvatar)
        tvUserName = view.findViewById(R.id.tvUserName)
        tvUserEmail = view.findViewById(R.id.tvUserEmail)
        btnPersonalInfo = view.findViewById(R.id.btnPersonalInfo)
        btnChangePassword = view.findViewById(R.id.btnChangePassword)
        btnSavedPosts = view.findViewById(R.id.btnSavedPosts)
        btnAppointments = view.findViewById(R.id.btnAppointments)
        tvAppointmentBadge = view.findViewById(R.id.tvAppointmentBadge)
        btnLogout = view.findViewById(R.id.btnLogout)
        btnNotification = view.findViewById(R.id.btnNotification)
        btnChangeAvatar = view.findViewById(R.id.btnChangeAvatar)
        cardMyPosts = view.findViewById(R.id.cardMyPosts)
        btnMyPosts = view.findViewById(R.id.btnMyPosts)
        tvMyPostsBadge = view.findViewById(R.id.tvMyPostsBadge)
        tvRoleBadge = view.findViewById(R.id.tvRoleBadge)
    }

    private fun setupClickListeners() {
        imgAvatar.setOnClickListener {
            if (currentAvatarUrl.isNotEmpty()) {
                startActivity(Intent(requireContext(), ImageViewerActivity::class.java).apply {
                    putExtra("imageUrl", currentAvatarUrl)
                })
            }
        }
        btnChangeAvatar.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*"; addCategory(Intent.CATEGORY_OPENABLE) }
            pickImageLauncher.launch(intent)
        }
        btnPersonalInfo.setOnClickListener { startActivity(Intent(requireContext(), PersonalInfoActivity::class.java)) }
        btnChangePassword.setOnClickListener { startActivity(Intent(requireContext(), ChangePasswordActivity::class.java)) }
        btnSavedPosts.setOnClickListener { startActivity(Intent(requireContext(), SavedPostsActivity::class.java)) }
        btnMyPosts.setOnClickListener {
            tvMyPostsBadge.visibility = View.GONE
            startActivity(Intent(requireContext(), MyPostsActivity::class.java))
        }
        btnAppointments.setOnClickListener {
            tvAppointmentBadge.visibility = View.GONE
            startActivity(Intent(requireContext(), MyAppointmentsActivity::class.java))
        }
        btnLogout.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                .setPositiveButton("Đăng xuất") { _, _ ->
                    viewModel.logOut()
                    val intent = Intent(requireContext(), com.example.doantotnghiep.MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                }
                .setNegativeButton("Hủy", null).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Dùng isLoggedIn() từ ViewModel thay vì gọi FirebaseAuth trực tiếp
        if (viewModel.isLoggedIn() && layoutGuest.visibility == View.VISIBLE) {
            layoutGuest.visibility = View.GONE
            layoutProfile.visibility = View.VISIBLE
            setupClickListeners()
            setupObservers()
        }
        if (viewModel.isLoggedIn() && ::viewModel.isInitialized) {
            viewModel.loadUserInfo()
            viewModel.loadAppointmentBadge()
            viewModel.loadMyPostsBadge(requireContext())
        }
    }
}