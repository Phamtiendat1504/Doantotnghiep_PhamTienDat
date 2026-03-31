package com.example.doantotnghiep.View.Auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.doantotnghiep.R
import com.example.doantotnghiep.ViewModel.AuthViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.example.doantotnghiep.MainActivity

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        // Kiểm tra nếu đã đăng nhập → vào thẳng MainActivity
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            return
        }
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvGoToRegister = findViewById(R.id.tvGoToRegister)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnLogin.isEnabled = !isLoading
        }

        viewModel.loginResult.observe(this) { success ->
            if (success) {
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }

        viewModel.errorMessage.observe(this) { message ->
            clearErrors()
            when {
                message.contains("nhập email", ignoreCase = true) -> tilEmail.error = message
                message.contains("nhập mật khẩu", ignoreCase = true) -> tilPassword.error = message
                else -> {
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Đăng nhập thất bại")
                        .setMessage("Thông tin không chính xác. Vui lòng nhập lại.")
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
}