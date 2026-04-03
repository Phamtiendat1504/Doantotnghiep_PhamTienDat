package com.example.doantotnghiep.View.Auth

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.MessageUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.EmailAuthProvider
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

        btnChangePassword.setOnClickListener {
            clearErrors()

            val oldPassword = edtOldPassword.text.toString().trim()
            val newPassword = edtNewPassword.text.toString().trim()
            val confirmPassword = edtConfirmPassword.text.toString().trim()

            // Validate
            if (oldPassword.isEmpty()) {
                tilOldPassword.error = "Vui lòng nhập mật khẩu cũ"
                return@setOnClickListener
            }
            if (newPassword.isEmpty()) {
                tilNewPassword.error = "Vui lòng nhập mật khẩu mới"
                return@setOnClickListener
            }
            if (newPassword.length < 12 ||
                !newPassword.any { it.isUpperCase() } ||
                !newPassword.any { it.isDigit() } ||
                !newPassword.any { !it.isLetterOrDigit() }
            ) {
                tilNewPassword.error = "Mật khẩu phải có ít nhất 12 ký tự, gồm chữ hoa, số và ký tự đặc biệt"
                return@setOnClickListener
            }
            if (confirmPassword.isEmpty()) {
                tilConfirmPassword.error = "Vui lòng nhập lại mật khẩu mới"
                return@setOnClickListener
            }
            if (newPassword != confirmPassword) {
                tilConfirmPassword.error = "Mật khẩu xác nhận không khớp"
                return@setOnClickListener
            }
            if (oldPassword == newPassword) {
                tilNewPassword.error = "Mật khẩu mới phải khác mật khẩu cũ"
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnChangePassword.isEnabled = false

            // Xác thực lại bằng mật khẩu cũ
            val user = FirebaseAuth.getInstance().currentUser
            val email = user?.email ?: ""
            val credential = EmailAuthProvider.getCredential(email, oldPassword)

            user?.reauthenticate(credential)
                ?.addOnSuccessListener {
                    // Xác thực thành công → đổi mật khẩu
                    user.updatePassword(newPassword)
                        .addOnSuccessListener {
                            progressBar.visibility = View.GONE
                            btnChangePassword.isEnabled = true

                            MessageUtils.showSuccessDialog(this, "Thành công", "Mật khẩu của bạn đã được thay đổi thành công!") {
                                finish()
                            }
                        }
                        .addOnFailureListener { e ->
                            progressBar.visibility = View.GONE
                            btnChangePassword.isEnabled = true
                            MessageUtils.showErrorDialog(this, "Lỗi đổi mật khẩu", e.message ?: "Không thể cập nhật mật khẩu mới.")
                        }
                }
                ?.addOnFailureListener {
                    progressBar.visibility = View.GONE
                    btnChangePassword.isEnabled = true
                    tilOldPassword.error = "Mật khẩu cũ không đúng"
                }
        }

        btnBack.setOnClickListener { finish() }
    }

    private fun clearErrors() {
        tilOldPassword.error = null
        tilNewPassword.error = null
        tilConfirmPassword.error = null
    }
}
