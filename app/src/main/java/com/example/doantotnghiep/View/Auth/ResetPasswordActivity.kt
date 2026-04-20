package com.example.doantotnghiep.View.Auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.ViewModel.ResetPasswordViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var tilNewPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var edtNewPassword: TextInputEditText
    private lateinit var edtConfirmPassword: TextInputEditText
    private lateinit var btnResetPassword: MaterialButton
    private lateinit var progressBar: ProgressBar

    private val viewModel: ResetPasswordViewModel by viewModels()
    private var email = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        tilNewPassword = findViewById(R.id.tilNewPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        edtNewPassword = findViewById(R.id.edtNewPassword)
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword)
        btnResetPassword = findViewById(R.id.btnResetPassword)
        progressBar = findViewById(R.id.progressBar)

        email = intent.getStringExtra("email") ?: ""

        observeViewModel()

        btnResetPassword.setOnClickListener {
            tilNewPassword.error = null
            tilConfirmPassword.error = null
            val newPassword = edtNewPassword.text.toString().trim()
            val confirmPassword = edtConfirmPassword.text.toString().trim()
            viewModel.resetPassword(email, newPassword, confirmPassword)
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            btnResetPassword.isEnabled = !loading
        }

        viewModel.resetResult.observe(this) { success ->
            if (success) {
                MessageUtils.showSuccessDialog(
                    this,
                    "Thành công",
                    "Mật khẩu đã được đổi thành công! Vui lòng đăng nhập lại."
                ) {
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
            }
        }

        viewModel.errorMessage.observe(this) { msg ->
            when (msg) {
                "new_empty" -> tilNewPassword.error = "Vui lòng nhập mật khẩu mới"
                "new_weak" -> tilNewPassword.error = "Mật khẩu phải có ít nhất 12 ký tự, gồm chữ hoa, số và ký tự đặc biệt"
                "confirm_empty" -> tilConfirmPassword.error = "Vui lòng xác nhận mật khẩu"
                "confirm_mismatch" -> tilConfirmPassword.error = "Mật khẩu xác nhận không khớp"
                else -> if (!msg.isNullOrEmpty()) MessageUtils.showErrorDialog(this, "Đặt lại mật khẩu thất bại", msg)
            }
        }
    }
}
