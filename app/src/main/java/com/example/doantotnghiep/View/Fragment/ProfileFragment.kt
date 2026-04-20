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
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.View.Auth.*
import com.example.doantotnghiep.ViewModel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

class ProfileFragment : Fragment() {

    private lateinit var imgAvatar: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var btnPersonalInfo: LinearLayout
    private lateinit var btnChangePassword: LinearLayout
    private lateinit var btnSavedPosts: LinearLayout
    private lateinit var btnAppointments: LinearLayout
    private lateinit var btnMessages: LinearLayout
    private lateinit var tvAppointmentBadge: TextView
    private lateinit var tvMessagesBadge: TextView
    private lateinit var btnLogout: LinearLayout
    private lateinit var btnNotification: View
    private lateinit var tvNotificationBadge: TextView
    private lateinit var btnChangeAvatar: androidx.cardview.widget.CardView
    private lateinit var cardMyPosts: androidx.cardview.widget.CardView
    private lateinit var btnMyPosts: LinearLayout
    private lateinit var tvMyPostsBadge: TextView
    private lateinit var tvRoleBadge: TextView
    private lateinit var layoutGuest: android.widget.ScrollView
    private lateinit var layoutProfile: android.widget.ScrollView

    private lateinit var viewModel: ProfileViewModel
    private var currentAvatarUrl: String = ""
    private var avatarLoadingDialog: AlertDialog? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.uploadAvatar(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)

        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]

        // DĂ¹ng isLoggedIn() tá»« ViewModel thay vĂ¬ gá»i FirebaseAuth trá»±c tiáº¿p
        if (!viewModel.isLoggedIn()) {
            layoutGuest.visibility = View.VISIBLE
            layoutProfile.visibility = View.GONE
            setupGuestListeners()
        } else {
            layoutGuest.visibility = View.GONE
            layoutProfile.visibility = View.VISIBLE
            applyUnverifiedBadgePlaceholder()
            // Luon hien du 4 muc hoat dong cho moi tai khoan da dang nhap.
            cardMyPosts.visibility = View.VISIBLE
            btnSavedPosts.visibility = View.VISIBLE
            setupClickListeners()
            setupObservers()
            viewModel.loadUserInfo()
            viewModel.loadNotificationBadge()
            viewModel.loadMessagesBadge()
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
            renderRoleBadge(role, isVerified, viewModel.verificationStatus.value)

            cardMyPosts.visibility = View.VISIBLE
            btnSavedPosts.visibility = View.VISIBLE
            viewModel.loadAppointmentBadge(role, isVerified)
            viewModel.loadMyPostsBadge()
        }

        viewModel.verificationStatus.observe(viewLifecycleOwner) { status ->
            if (!isAdded) return@observe
            val info = viewModel.userInfo.value
            renderRoleBadge(info?.role, info?.isVerified, status)
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

        viewModel.messagesBadgeInfo.observe(viewLifecycleOwner) { (_, unreadMessages) ->
            if (!isAdded) return@observe
            if (unreadMessages > 0) {
                tvMessagesBadge.text = if (unreadMessages > 99) "99+" else unreadMessages.toString()
                tvMessagesBadge.visibility = View.VISIBLE
            } else {
                tvMessagesBadge.visibility = View.GONE
            }
        }

        viewModel.newAvatarUrl.observe(viewLifecycleOwner) { url ->
            if (!isAdded || url.isNullOrEmpty()) return@observe
            dismissAvatarLoadingDialog()
            currentAvatarUrl = url
            Glide.with(this).load(url).circleCrop().into(imgAvatar)
            imgAvatar.setPadding(0, 0, 0, 0)
            imgAvatar.imageTintList = null
            
            MessageUtils.showSuccessDialog(
                requireContext(),
                "C\u1eadp nh\u1eadt th\u00e0nh c\u00f4ng",
                "\u1ea2nh \u0111\u1ea1i di\u1ec7n \u0111\u00e3 \u0111\u01b0\u1ee3c c\u1eadp nh\u1eadt"
            )
            viewModel.resetAvatarUploadState() // Reset Ä‘á»ƒ khĂ´ng hiá»‡n láº¡i Dialog Ä‘ang xá»­ lĂ½
        }

        viewModel.isUploadingAvatar.observe(viewLifecycleOwner) { isUploading ->
            if (isUploading == true && isAdded) {
                showAvatarLoadingDialog()
            } else {
                dismissAvatarLoadingDialog()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrEmpty() && isAdded) {
                dismissAvatarLoadingDialog()
                MessageUtils.showErrorDialog(
                    requireContext(),
                    "L\u1ed7i c\u1eadp nh\u1eadt \u1ea3nh",
                    error
                )
            }
        }

        viewModel.notificationBadgeCount.observe(viewLifecycleOwner) { count ->
            if (!isAdded) return@observe
            val badge = view?.findViewById<TextView>(R.id.tvNotificationBadge)
            if (count > 0) {
                badge?.text = if (count > 99) "99+" else count.toString()
                badge?.visibility = View.VISIBLE
            } else {
                badge?.visibility = View.GONE
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

    private fun showAvatarLoadingDialog() {
        if (!isAdded) return
        val current = avatarLoadingDialog
        if (current != null && current.isShowing) return
        avatarLoadingDialog = MessageUtils.showLoadingDialog(
            requireContext(),
            message = "\u1ea2nh \u0111\u1ea1i di\u1ec7n \u0111ang \u0111\u01b0\u1ee3c t\u1ea3i l\u00ean, vui l\u00f2ng ch\u1edd trong gi\u00e2y l\u00e1t.",
            title = "\u0110ang c\u1eadp nh\u1eadt \u1ea3nh"
        )
    }

    private fun dismissAvatarLoadingDialog() {
        avatarLoadingDialog?.dismiss()
        avatarLoadingDialog = null
    }

    private fun applyUnverifiedBadgePlaceholder() {
        tvRoleBadge.text = "T\u00e0i kho\u1ea3n ch\u01b0a x\u00e1c minh"
        tvRoleBadge.setTextColor(0xFF666666.toInt())
        tvRoleBadge.setBackgroundResource(R.drawable.bg_badge_tenant)
    }

    private fun applyVerifiedBadge() {
        tvRoleBadge.text = "\u2713 T\u00e0i kho\u1ea3n \u0111\u00e3 x\u00e1c minh"
        tvRoleBadge.setTextColor(0xFF2E7D32.toInt())
        tvRoleBadge.setBackgroundResource(R.drawable.bg_badge_landlord)
    }

    private fun renderRoleBadge(role: String?, isVerified: Boolean?, verificationStatus: String?) {
        val normalizedRole = role?.trim()?.lowercase(Locale.ROOT).orEmpty()
        val normalizedStatus = verificationStatus?.trim()?.lowercase(Locale.ROOT)
        val isVerifiedAccount =
            (isVerified == true) ||
            normalizedRole == "landlord" ||
            normalizedRole == "owner" ||
            normalizedStatus == "approved" ||
            normalizedStatus == "verified"

        when {
            normalizedRole == "admin" -> {
                tvRoleBadge.text = "\u2605 Qu\u1ea3n tr\u1ecb vi\u00ean"
                tvRoleBadge.setTextColor(0xFF1976D2.toInt())
                tvRoleBadge.setBackgroundResource(R.drawable.bg_badge_tenant)
            }

            isVerifiedAccount -> applyVerifiedBadge()

            normalizedStatus == "pending" || normalizedStatus == "submitted" || normalizedStatus == "processing" -> {
                tvRoleBadge.text = "\u23f3 \u0110ang ch\u1edd x\u00e1c minh"
                tvRoleBadge.setTextColor(0xFFE65100.toInt())
                tvRoleBadge.setBackgroundResource(R.drawable.bg_badge_pending)
            }

            normalizedStatus == "rejected" -> {
                tvRoleBadge.text = "\u2717 X\u00e1c minh b\u1ecb t\u1eeb ch\u1ed1i"
                tvRoleBadge.setTextColor(0xFFD32F2F.toInt())
                tvRoleBadge.setBackgroundResource(R.drawable.bg_badge_pending)
            }

            else -> applyUnverifiedBadgePlaceholder()
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
        btnMessages = view.findViewById(R.id.btnMessages)
        tvAppointmentBadge = view.findViewById(R.id.tvAppointmentBadge)
        tvMessagesBadge = view.findViewById(R.id.tvMessagesBadge)
        btnLogout = view.findViewById(R.id.btnLogout)
        btnNotification = view.findViewById(R.id.btnNotificationContainer)
        tvNotificationBadge = view.findViewById(R.id.tvNotificationBadge)
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
            pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        btnPersonalInfo.setOnClickListener { startActivity(Intent(requireContext(), PersonalInfoActivity::class.java)) }
        btnChangePassword.setOnClickListener { startActivity(Intent(requireContext(), ChangePasswordActivity::class.java)) }
        btnNotification.setOnClickListener {
            val intent = Intent(requireContext(), NotificationsActivity::class.java)
            startActivity(intent)
        }
        btnSavedPosts.setOnClickListener { startActivity(Intent(requireContext(), SavedPostsActivity::class.java)) }
        btnMyPosts.setOnClickListener {
            tvMyPostsBadge.visibility = View.GONE
            startActivity(Intent(requireContext(), MyPostsActivity::class.java))
        }
        btnAppointments.setOnClickListener {
            tvAppointmentBadge.visibility = View.GONE
            startActivity(Intent(requireContext(), MyAppointmentsActivity::class.java))
        }
        btnMessages.setOnClickListener {
            tvMessagesBadge.visibility = View.GONE
            startActivity(Intent(requireContext(), ConversationsActivity::class.java))
        }
        btnLogout.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("\u0110\u0103ng xu\u1ea5t")
                .setMessage("B\u1ea1n c\u00f3 ch\u1eafc ch\u1eafn mu\u1ed1n \u0111\u0103ng xu\u1ea5t?")
                .setPositiveButton("\u0110\u0103ng xu\u1ea5t") { _, _ ->
                    viewModel.logOut()
                    val intent = Intent(requireContext(), com.example.doantotnghiep.MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                }
                .setNegativeButton("H\u1ee7y", null).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // DĂ¹ng isLoggedIn() tá»« ViewModel thay vĂ¬ gá»i FirebaseAuth trá»±c tiáº¿p
        if (viewModel.isLoggedIn() && layoutGuest.visibility == View.VISIBLE) {
            layoutGuest.visibility = View.GONE
            layoutProfile.visibility = View.VISIBLE
            cardMyPosts.visibility = View.VISIBLE
            btnSavedPosts.visibility = View.VISIBLE
            setupClickListeners()
            setupObservers()
        }
        if (viewModel.isLoggedIn() && ::viewModel.isInitialized) {
            viewModel.loadUserInfo()
            viewModel.loadNotificationBadge()
            viewModel.loadMessagesBadge()
        }
    }

    override fun onDestroyView() {
        dismissAvatarLoadingDialog()
        super.onDestroyView()
    }
}


