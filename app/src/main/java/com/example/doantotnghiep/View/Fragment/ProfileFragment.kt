package com.example.doantotnghiep.View.Fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.View.Auth.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

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
    private lateinit var layoutGuest: android.widget.LinearLayout
    private lateinit var layoutProfile: android.widget.ScrollView

    private var currentAvatarUrl: String = ""

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uploadAvatar(it) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            layoutGuest.visibility = View.VISIBLE
            layoutProfile.visibility = View.GONE
            setupGuestListeners()
        } else {
            layoutGuest.visibility = View.GONE
            layoutProfile.visibility = View.VISIBLE
            loadUserInfo()
            setupClickListeners()
            loadAppointmentBadge()
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
                val intent = Intent(requireContext(), ImageViewerActivity::class.java)
                intent.putExtra("imageUrl", currentAvatarUrl)
                startActivity(intent)
            }
        }

        btnChangeAvatar.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            pickImageLauncher.launch(intent)
        }

        btnPersonalInfo.setOnClickListener { startActivity(Intent(requireContext(), PersonalInfoActivity::class.java)) }
        btnChangePassword.setOnClickListener { startActivity(Intent(requireContext(), ChangePasswordActivity::class.java)) }
        btnSavedPosts.setOnClickListener { startActivity(Intent(requireContext(), SavedPostsActivity::class.java)) }
        btnMyPosts.setOnClickListener {
            tvMyPostsBadge.visibility = android.view.View.GONE
            startActivity(Intent(requireContext(), MyPostsActivity::class.java))
        }
        btnAppointments.setOnClickListener {
            tvAppointmentBadge.visibility = android.view.View.GONE
            startActivity(Intent(requireContext(), MyAppointmentsActivity::class.java))
        }
        
        btnLogout.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                .setPositiveButton("Đăng xuất") { _, _ ->
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                }
                .setNegativeButton("Hủy", null).show()
        }
    }

    private fun loadUserInfo() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists() && isAdded) {
                    tvUserName.text = document.getString("fullName") ?: "Chưa cập nhật"
                    tvUserEmail.text = document.getString("email") ?: ""
                    currentAvatarUrl = document.getString("avatarUrl") ?: ""

                    if (currentAvatarUrl.isNotEmpty()) {
                        imgAvatar.setPadding(0, 0, 0, 0)
                        imgAvatar.imageTintList = null
                        Glide.with(this).load(currentAvatarUrl).circleCrop().placeholder(R.drawable.ic_person).into(imgAvatar)
                    }

                    val role = document.getString("role") ?: "tenant"
                    val isVerified = document.getBoolean("isVerified") ?: false
                    
                    updateRoleBadge(role, isVerified, uid)
                    cardMyPosts.visibility = if (role == "landlord" || role == "admin") View.VISIBLE else View.GONE
                    btnSavedPosts.visibility = if (role == "landlord" || role == "admin") View.GONE else View.VISIBLE
                }
            }
    }

    private fun updateRoleBadge(role: String, isVerified: Boolean, uid: String) {
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
            else -> checkVerificationStatus(uid)
        }
    }

    private fun checkVerificationStatus(uid: String) {
        FirebaseFirestore.getInstance().collection("verifications").document(uid).get()
            .addOnSuccessListener { doc ->
                if (!isAdded) return@addOnSuccessListener
                if (doc.exists()) {
                    when (doc.getString("status")) {
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
                        else -> setDefaultBadge()
                    }
                } else setDefaultBadge()
            }
    }

    private fun setDefaultBadge() {
        tvRoleBadge.text = "Người thuê trọ"
        tvRoleBadge.setTextColor(0xFF666666.toInt())
        tvRoleBadge.setBackgroundResource(R.drawable.bg_badge_tenant)
    }

    private fun uploadAvatar(imageUri: Uri) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        MessageUtils.showInfoDialog(requireContext(), "Đang xử lý", "Ảnh đang được tải lên, vui lòng chờ...")
        val storageRef = FirebaseStorage.getInstance().reference.child("avatars/$uid.jpg")

        storageRef.putFile(imageUri).continueWithTask { task ->
            if (!task.isSuccessful) task.exception?.let { throw it }
            storageRef.downloadUrl
        }.addOnSuccessListener { downloadUrl ->
            FirebaseFirestore.getInstance().collection("users").document(uid)
                .update("avatarUrl", downloadUrl.toString())
                .addOnSuccessListener {
                    if (isAdded) {
                        currentAvatarUrl = downloadUrl.toString()
                        Glide.with(this).load(currentAvatarUrl).circleCrop().into(imgAvatar)
                        imgAvatar.setPadding(0, 0, 0, 0)
                        imgAvatar.imageTintList = null
                        MessageUtils.showSuccessDialog(requireContext(), "Cập nhật thành công", "Ảnh đại diện đã được cập nhật")
                    }
                }
        }.addOnFailureListener { e ->
            if (isAdded) {
                // BUG FIX #13: Better error handling for timeout and other failures
                val errorMsg = when {
                    e.message?.contains("timeout", ignoreCase = true) == true -> "Upload quá lâu, vui lòng thử lại"
                    e.message?.contains("network", ignoreCase = true) == true -> "Lỗi mạng, vui lòng kiểm tra kết nối"
                    else -> "Lỗi: ${e.message}"
                }
                MessageUtils.showErrorDialog(requireContext(), "Lỗi cập nhật ảnh", errorMsg)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null && layoutGuest.visibility == View.VISIBLE) {
            // Vừa đăng nhập xong → chuyển sang profile
            layoutGuest.visibility = View.GONE
            layoutProfile.visibility = View.VISIBLE
            setupClickListeners()
        }
        if (currentUser != null) {
            loadUserInfo()
            loadAppointmentBadge()
            loadMyPostsBadge()
        }
    }

    private fun loadMyPostsBadge() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val prefs = requireContext().getSharedPreferences("post_seen_$uid", android.content.Context.MODE_PRIVATE)
        val seenIds = prefs.getStringSet("seen_ids", emptySet()) ?: emptySet()

        FirebaseFirestore.getInstance().collection("rooms")
            .whereEqualTo("userId", uid)
            .whereIn("status", listOf("approved", "rejected"))
            .get()
            .addOnSuccessListener { docs ->
                if (!isAdded) return@addOnSuccessListener
                val unseenCount = docs.count { !seenIds.contains(it.id) }
                if (unseenCount > 0) {
                    tvMyPostsBadge.text = if (unseenCount > 99) "99+" else unseenCount.toString()
                    tvMyPostsBadge.visibility = android.view.View.VISIBLE
                } else {
                    tvMyPostsBadge.visibility = android.view.View.GONE
                }
            }
    }

    private fun loadAppointmentBadge() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
            if (!isAdded) return@addOnSuccessListener
            val role = userDoc.getString("role") ?: "tenant"
            val isLandlord = role == "landlord" || role == "admin"

            // Chủ trọ: đếm lịch hẹn status="pending" (yêu cầu mới chờ xác nhận)
            // Người thuê: đếm lịch hẹn status="confirmed" (chủ trọ đã xác nhận, cần phản hồi)
            val field = if (isLandlord) "landlordId" else "tenantId"
            val statusFilter = if (isLandlord) "pending" else "confirmed"

            db.collection("appointments")
                .whereEqualTo(field, uid)
                .whereEqualTo("status", statusFilter)
                .get()
                .addOnSuccessListener { docs ->
                    if (!isAdded) return@addOnSuccessListener
                    val count = docs.size()
                    if (count > 0) {
                        tvAppointmentBadge.text = if (count > 99) "99+" else count.toString()
                        tvAppointmentBadge.visibility = android.view.View.VISIBLE
                    } else {
                        tvAppointmentBadge.visibility = android.view.View.GONE
                    }
                }
        }
    }
}