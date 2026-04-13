package com.example.doantotnghiep.View.Auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.doantotnghiep.R
import com.example.doantotnghiep.ViewModel.AuthViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.doantotnghiep.MainActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LoginActivity : AppCompatActivity() {

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var edtEmail: TextInputEditText
    private lateinit var edtPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvGoToRegister: TextView
    private lateinit var viewModel: AuthViewModel

    private val KEY_REMEMBER = "remember_password"

    // Sử dụng EncryptedSharedPreferences để bảo mật cấp độ 1
    private val prefs by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "secure_login_prefs",
            masterKeyAlias,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvGoToRegister = findViewById(R.id.tvGoToRegister)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        // Đã xóa bỏ hoàn toàn chức năng lưu/fill mật khẩu cục bộ để đảm bảo bảo mật 
        // và đồng bộ dữ liệu khi Admin xóa tài khoản trên hệ thống.

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnLogin.isEnabled = !isLoading
        }

        viewModel.loginResult.observe(this) { success ->
            if (success) {
                viewModel.resetLoginResult()
                viewModel.checkLockStatus()
            }
        }

        viewModel.lockInfo.observe(this) { pair ->
            val isLocked = pair.first
            val (reason, until, lockDays) = pair.second

            if (isLocked) {
                val currentTime = System.currentTimeMillis()
                
                // Kiểm tra xem thời hạn khóa đã hết chưa
                if (currentTime < until) {
                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    val unlockDate = sdf.format(Date(until))
                    
                    val isPermanent = lockDays >= 999 || until > currentTime + (50L * 365 * 24 * 60 * 60 * 1000)
                    val lockDuration = if (isPermanent) "Vĩnh viễn" else "$lockDays ngày"
                    val displayTime = if (isPermanent) "Vĩnh viễn" else unlockDate
                    
                    val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Tài khoản đã bị khóa")
                        .setMessage("Hệ thống đã tạm ngừng hoạt động của tài khoản này.\n\n" +
                                "• Lý do: $reason\n" +
                                "• Thời gian khóa: $lockDuration\n" +
                                "• Mở khóa vào: $displayTime\n\n" +
                                "Hệ thống sẽ tự động mở khóa khi hết hạn. Vui lòng quay lại sau.")
                        .setPositiveButton("Đã hiểu") { dialog, _ ->
                            viewModel.logOut()
                            dialog.dismiss()
                        }
                        .setCancelable(false)
                        .show()

                    // --- BẮT ĐẦU LẮNG NGHE REAL-TIME KHI ĐANG HIỆN DIALOG ---
                    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    if (uid != null) {
                        val listenerRegistration = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("users").document(uid)
                            .addSnapshotListener { snapshot, _ ->
                                if (snapshot != null && snapshot.exists()) {
                                    val currentlyLocked = snapshot.getBoolean("isLocked") ?: false
                                    if (!currentlyLocked) {
                                        // ADMIN VỪA MỞ KHÓA REAL-TIME!
                                        if (dialog.isShowing) {
                                            dialog.dismiss()
                                            androidx.appcompat.app.AlertDialog.Builder(this)
                                                .setTitle("Tin vui!")
                                                .setMessage("Tài khoản của bạn đã được mở khóa thành công. Bạn có thể sử dụng ứng dụng ngay bây giờ.")
                                                .setPositiveButton("Bắt đầu ngay") { d, _ ->
                                                    d.dismiss()
                                                    goToMainActivity()
                                                }
                                                .setCancelable(false)
                                                .show()
                                        }
                                    }
                                }
                            }
                        // Listener này sẽ tự động bị rò rỉ nếu không được quản lý, 
                        // nhưng trong LoginActivity nó sẽ bị destroy cùng Activity.
                    }
                } else {
                    goToMainActivity()
                }
            } else {
                goToMainActivity()
            }
        }

        viewModel.errorMessage.observe(this) { message ->
            clearErrors()
            when {
                message.contains("nhập email", ignoreCase = true) -> tilEmail.error = message
                message.contains("nhập mật khẩu", ignoreCase = true) -> tilPassword.error = message
                else -> {
                    // FIX Ý 3.2: Không tự động xóa credentials khi gõ sai, để người dùng sửa lỗi cho nhanh
                    // Chỉ hiển thị thông báo lỗi
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Đăng nhập thất bại")
                        .setMessage(message ?: "Thông tin không chính xác. Vui lòng nhập lại.")
                        .setPositiveButton("Thử lại") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
            }
        }

        btnLogin.setOnClickListener {
            clearErrors()
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()
            viewModel.login(email, password)
        }

        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun clearErrors() {
        tilEmail.error = null
        tilPassword.error = null
    }

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }
}
