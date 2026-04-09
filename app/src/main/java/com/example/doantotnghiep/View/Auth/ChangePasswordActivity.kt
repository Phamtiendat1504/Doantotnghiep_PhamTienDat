package com.example.doantotnghiep.View.Auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.ViewModel.AuthViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var tilOldPassword: TextInputLayout
    private lateinit var tilNewPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var edtOldPassword: TextInputEditText
    private lateinit var edtNewPassword: TextInputEditText
    private lateinit var edtConfirmPassword: TextInputEditText
    private lateinit var btnChangePassword: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: ImageView

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        tilOldPassword = findViewById(R.id.tilOldPassword)
        tilNewPassword = findViewById(R.id.tilNewPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        edtOldPassword = findViewById(R.id.edtOldPassword)
        edtNewPassword = findViewById(R.id.edtNewPassword)
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword)
        btnChangePassword = findViewById(R.id.btnChangePassword)
        progressBar = findViewById(R.id.progressBar)
        btnBack = findViewById(R.id.btnBack)

        observeViewModel()

        btnChangePassword.setOnClickListener {
            clearErrors()
            val oldPassword = edtOldPassword.text.toString().trim()
            val newPassword = edtNewPassword.text.toString().trim()
            val confirmPassword = edtConfirmPassword.text.toString().trim()
            authViewModel.changePassword(oldPassword, newPassword, confirmPassword)
        }

        btnBack.setOnClickListener { finish() }
    }

    private fun observeViewModel() {
        authViewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            btnChangePassword.isEnabled = !loading
        }

        authViewModel.changePasswordResult.observe(this) { success ->
            if (success) {
                // Cập nhật mật khẩu mới vào SharedPreferences nếu đang nhớ mật khẩu
                val prefs = getSharedPreferences("login_prefs", MODE_PRIVATE)
                val isRemember = prefs.getBoolean("remember_password", false)
                if (isRemember) {
                    val currentEmail = authViewModel.getCurrentUserEmail() ?: ""
                    val newPassword = edtNewPassword.text.toString().trim()
                    if (currentEmail.isNotEmpty()) {
                        prefs.edit().putString("pwd_$currentEmail", newPassword).apply()
                    }
                }

                MessageUtils.showSuccessDialog(this, "Thành công", "Mật khẩu đã được thay đổi. Vui lòng đăng nhập lại.") {
                    authViewModel.logOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
            }
        }

        authViewModel.wrongOldPassword.observe(this) { wrong ->
            if (wrong) tilOldPassword.error = "Mật khẩu cũ không đúng"
        }

        authViewModel.errorMessage.observe(this) { msg ->
            when (msg) {
                "old_empty" -> tilOldPassword.error = "Vui lòng nhập mật khẩu cũ"
                "new_empty" -> tilNewPassword.error = "Vui lòng nhập mật khẩu mới"
                "new_weak" -> tilNewPassword.error = "Mật khẩu phải có ít nhất 12 ký tự, gồm chữ hoa, số và ký tự đặc biệt"
                "confirm_empty" -> tilConfirmPassword.error = "Vui lòng nhập lại mật khẩu mới"
                "confirm_mismatch" -> tilConfirmPassword.error = "Mật khẩu xác nhận không khớp"
                "same_password" -> tilNewPassword.error = "Mật khẩu mới phải khác mật khẩu cũ"
                else -> if (!msg.isNullOrEmpty()) MessageUtils.showErrorDialog(this, "Lỗi", msg)
            }
        }
    }

    private fun clearErrors() {
        tilOldPassword.error = null
        tilNewPassword.error = null
        tilConfirmPassword.error = null
    }
}