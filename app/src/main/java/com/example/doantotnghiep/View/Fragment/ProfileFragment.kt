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
            Toast.makeText(requireContext(), "Chức năng đang phát triển", Toast.LENGTH_SHORT).show()
        }

        // Bấm Lịch hẹn xem phòng
        btnAppointments.setOnClickListener {
            Toast.makeText(requireContext(), "Chức năng đang phát triển", Toast.LENGTH_SHORT).show()
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
    }

    private fun loadUserInfo() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    tvUserName.text = document.getString("fullName") ?: "Chưa cập nhật"
                    tvUserEmail.text = document.getString("email") ?: ""

                    // Load ảnh đại diện từ Base64
                    val avatarBase64 = document.getString("avatarUrl") ?: ""
                    if (avatarBase64.isNotEmpty()) {
                        try {
                            val imageBytes = Base64.decode(avatarBase64, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            imgAvatar.setPadding(0, 0, 0, 0)
                            imgAvatar.imageTintList = null
                            Glide.with(requireContext())
                                .load(bitmap)
                                .circleCrop()
                                .into(imgAvatar)
                        } catch (e: Exception) {
                            // Giữ ảnh mặc định nếu lỗi
                        }
                    }
                }
            }
            .addOnFailureListener {
                tvUserName.text = "Lỗi tải thông tin"
            }
    }

    private fun uploadAvatar(imageUri: Uri) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        Toast.makeText(requireContext(), "Đang cập nhật ảnh...", Toast.LENGTH_SHORT).show()

        try {
            // Đọc ảnh và nén
            val inputStream = requireContext().contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            // Resize ảnh xuống 200x200 để tiết kiệm dung lượng
            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 200, 200, true)

            // Chuyển sang Base64
            val byteArrayOutputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
            val imageBytes = byteArrayOutputStream.toByteArray()
            val base64String = Base64.encodeToString(imageBytes, Base64.DEFAULT)

            // Lưu vào Firestore
            FirebaseFirestore.getInstance().collection("users").document(uid)
                .update("avatarUrl", base64String)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Cập nhật ảnh đại diện thành công!", Toast.LENGTH_SHORT).show()

                    // Hiển thị ảnh mới
                    imgAvatar.setPadding(0, 0, 0, 0)
                    imgAvatar.imageTintList = null
                    Glide.with(requireContext())
                        .load(resizedBitmap)
                        .circleCrop()
                        .into(imgAvatar)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Cập nhật thất bại: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Lỗi xử lý ảnh: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserInfo()
    }
}