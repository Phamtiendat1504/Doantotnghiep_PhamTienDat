package com.example.doantotnghiep.View.Auth

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.doantotnghiep.R
import com.example.doantotnghiep.repository.AuthRepository
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

    private val repository = AuthRepository()
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

        btnResetPassword.setOnClickListener {
            tilNewPassword.error = null
            tilConfirmPassword.error = null

            val newPassword = edtNewPassword.text.toString().trim()
            val confirmPassword = edtConfirmPassword.text.toString().trim()

            if (newPassword.isEmpty()) {
                tilNewPassword.error = "Vui lòng nhập mật khẩu mới"
                return@setOnClickListener
            }
            if (newPassword.length < 12 ||
                !newPassword.any { it.isUpperCase() } ||
                !newPassword.any { it.isDigit() } ||
                !newPassword.any { !it.isLetterOrDigit() }
            ) {
                tilNewPassword.error =
                    "Mật khẩu phải có ít nhất 12 ký tự, gồm chữ hoa, số và ký tự đặc biệt"
                return@setOnClickListener
            }
            if (confirmPassword.isEmpty()) {
                tilConfirmPassword.error = "Vui lòng xác nhận mật khẩu"
                return@setOnClickListener
            }
            if (newPassword != confirmPassword) {
                tilConfirmPassword.error = "Mật khẩu xác nhận không khớp"
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnResetPassword.isEnabled = false

            repository.updatePasswordAfterOtp(email, newPassword,
                onSuccess = {
                    progressBar.visibility = View.GONE
                    btnResetPassword.isEnabled = true

                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Thành công")
                        .setMessage("Mật khẩu đã được đổi thành công! Vui lòng đăng nhập lại.")
                        .setPositiveButton("Đăng nhập") { _, _ ->
                            // Quay về LoginActivity, xóa hết stack
                            val intent = android.content.Intent(
                                this, LoginActivity::class.java
                            )
                            intent.flags =
                                android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                        android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        }
                        .setCancelable(false)
                        .show()
                },
                onFailure = { error ->
                    progressBar.visibility = View.GONE
                    btnResetPassword.isEnabled = true
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}