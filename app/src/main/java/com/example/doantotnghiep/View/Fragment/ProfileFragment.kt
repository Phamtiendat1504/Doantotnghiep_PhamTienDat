package com.example.doantotnghiep.View.Fragment

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.doantotnghiep.R
import com.example.doantotnghiep.View.Auth.ChangePasswordActivity
import com.example.doantotnghiep.View.Auth.LoginActivity
import com.example.doantotnghiep.View.Auth.PersonalInfoActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import com.example.doantotnghiep.View.Auth.MyPostsActivity
import com.example.doantotnghiep.View.Auth.SavedPostsActivity
import com.example.doantotnghiep.View.Auth.MyAppointmentsActivity
import com.example.doantotnghiep.View.Auth.ImageViewerActivity
class ProfileFragment : Fragment() {

    private lateinit var imgAvatar: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var btnPersonalInfo: LinearLayout
    private lateinit var btnChangePassword: LinearLayout
    private lateinit var btnSavedPosts: LinearLayout
    private lateinit var btnAppointments: LinearLayout
    private lateinit var btnLogout: LinearLayout
    private lateinit var btnNotification: ImageView
    private lateinit var btnChangeAvatar: androidx.cardview.widget.CardView
    private lateinit var cardMyPosts: androidx.cardview.widget.CardView
    private lateinit var btnMyPosts: LinearLayout

    private lateinit var tvRoleBadge: TextView

    // Chọn ảnh từ thư viện
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            if (imageUri != null) {
                uploadAvatar(imageUri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imgAvatar = view.findViewById(R.id.imgAvatar)
        tvUserName = view.findViewById(R.id.tvUserName)
        tvUserEmail = view.findViewById(R.id.tvUserEmail)
        btnPersonalInfo = view.findViewById(R.id.btnPersonalInfo)
        btnChangePassword = view.findViewById(R.id.btnChangePassword)
        btnSavedPosts = view.findViewById(R.id.btnSavedPosts)
        btnAppointments = view.findViewById(R.id.btnAppointments)
        btnLogout = view.findViewById(R.id.btnLogout)
        btnNotification = view.findViewById(R.id.btnNotification)
        btnChangeAvatar = view.findViewById(R.id.btnChangeAvatar)
        cardMyPosts = view.findViewById(R.id.cardMyPosts)
        btnMyPosts = view.findViewById(R.id.btnMyPosts)
        tvRoleBadge = view.findViewById(R.id.tvRoleBadge)

        loadUserInfo()

        // Bấm đổi ảnh đại diện
        btnChangeAvatar.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            pickImageLauncher.launch(intent)
        }

        // Bấm Thông tin cá nhân
        btnPersonalInfo.setOnClickListener {
            startActivity(Intent(requireContext(), PersonalInfoActivity::class.java))
        }

        // Bấm Đổi mật khẩu
        btnChangePassword.setOnClickListener {
            startActivity(Intent(requireContext(), ChangePasswordActivity::class.java))
        }

        // Bấm Bài viết đã lưu
        btnSavedPosts.setOnClickListener {
            startActivity(Intent(requireContext(), SavedPostsActivity::class.java))
        }

        // Bấm Bài đăng của tôi
        btnMyPosts.setOnClickListener {
            startActivity(Intent(requireContext(), MyPostsActivity::class.java))
        }

        // Bấm Lịch hẹn xem phòng
        btnAppointments.setOnClickListener {
            startActivity(Intent(requireContext(), MyAppointmentsActivity::class.java))
        }

        // Bấm Thông báo
        btnNotification.setOnClickListener {
            Toast.makeText(requireContext(), "Chức năng đang phát triển", Toast.LENGTH_SHORT).show()
        }

        // Bấm Đăng xuất
        btnLogout.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                .setPositiveButton("Đăng xuất") { _, _ ->
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
        // Ẩn bài viết đã lưu nếu là chủ trọ
        val uidCheck = FirebaseAuth.getInstance().currentUser?.uid
        if (uidCheck != null) {
            FirebaseFirestore.getInstance().collection("users").document(uidCheck).get()
                .addOnSuccessListener { doc ->
                    if (!isAdded) return@addOnSuccessListener
                    val role = doc.getString("role") ?: "tenant"
                    if (role == "landlord" || role == "admin") {
                        btnSavedPosts.visibility = View.GONE
                    }
                }
        }
    }
    private fun loadUserInfo() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    tvUserName.text = document.getString("fullName") ?: "Chưa cập nhật"
                    tvUserEmail.text = document.getString("email") ?: ""

                    // Load ảnh đại diện từ Firebase Storage URL
                    val avatarUrl = document.getString("avatarUrl") ?: ""
                    if (avatarUrl.isNotEmpty() && avatarUrl.startsWith("http")) {
                        imgAvatar.setPadding(0, 0, 0, 0)
                        imgAvatar.imageTintList = null
                        Glide.with(requireContext())
                            .load(avatarUrl)
                            .circleCrop()
                            .placeholder(R.drawable.ic_person)
                            .into(imgAvatar)
                    }

                    // Hiển thị badge vai trò
                    val role = document.getString("role") ?: "tenant"
                    val isVerified = document.getBoolean("isVerified") ?: false

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
                            // Kiểm tra trạng thái xác minh
                            checkVerificationBadge(uid)
                        }
                    }
                    // Hiện/ẩn mục "Bài đăng của tôi"
                    if (role == "landlord" || role == "admin") {
                        cardMyPosts.visibility = View.VISIBLE
                    } else {
                        cardMyPosts.visibility = View.GONE
                    }
                }
            }
            .addOnFailureListener {
                tvUserName.text = "Lỗi tải thông tin"
            }
    }

    private fun checkVerificationBadge(uid: String) {
        FirebaseFirestore.getInstance().collection("verifications").document(uid)
            .get()
            .addOnSuccessListener { doc ->
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
                        else -> {
                            tvRoleBadge.text = "Người thuê trọ"
                            tvRoleBadge.setTextColor(0xFF666666.toInt())
                            tvRoleBadge.setBackgroundResource(R.drawable.bg_badge_tenant)
                        }
                    }
                } else {
                    tvRoleBadge.text = "Người thuê trọ"
                    tvRoleBadge.setTextColor(0xFF666666.toInt())
                    tvRoleBadge.setBackgroundResource(R.drawable.bg_badge_tenant)
                }
            }
    }

    private fun uploadAvatar(imageUri: Uri) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        Toast.makeText(requireContext(), "Đang cập nhật ảnh...", Toast.LENGTH_SHORT).show()

        // Upload ảnh lên Firebase Storage
        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance()
            .reference.child("avatars/$uid.jpg")

        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                // Lấy URL download
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    // Lưu URL vào Firestore
                    FirebaseFirestore.getInstance().collection("users").document(uid)
                        .update("avatarUrl", downloadUrl.toString())
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Cập nhật ảnh đại diện thành công!", Toast.LENGTH_SHORT).show()

                            // Hiển thị ảnh mới
                            imgAvatar.setPadding(0, 0, 0, 0)
                            imgAvatar.imageTintList = null
                            Glide.with(requireContext())
                                .load(downloadUrl.toString())
                                .circleCrop()
                                .into(imgAvatar)
                            imgAvatar.setOnClickListener {
                                val avatarUrl = imgAvatar.tag as? String ?: ""
                                if (avatarUrl.isNotEmpty()) {
                                    val intent = Intent(requireContext(), ImageViewerActivity::class.java)
                                    intent.putExtra("imageUrl", avatarUrl)
                                    startActivity(intent)
                                }
                            }
                        }

                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Lưu thông tin thất bại: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Upload ảnh thất bại: ${e.message}", Toast.LENGTH_LONG).show()
            }

    }
    override fun onResume() {
        super.onResume()
        loadUserInfo()
    }
}