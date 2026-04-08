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
    private lateinit var cbRememberPassword: CheckBox
    private lateinit var viewModel: AuthViewModel

    private val KEY_REMEMBER = "remember_password"
    // Password lưu theo key "pwd_<email>" để mỗi email có password riêng

    // Lưu email để gợi ý lần sau (không lưu password)
    private val prefs by lazy {
        getSharedPreferences("login_prefs", MODE_PRIVATE)
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
        cbRememberPassword = findViewById(R.id.cbRememberPassword)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        val isRemember = prefs.getBoolean(KEY_REMEMBER, false)
        // Chỉ khôi phục trạng thái checkbox, không tự fill email/password
        cbRememberPassword.isChecked = isRemember

        // Auto-fill password khi người dùng gõ email đã từng lưu mật khẩu
        edtEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val typedEmail = s?.toString()?.trim() ?: ""
                if (typedEmail.isNotEmpty()) {
                    val savedPassword = prefs.getString("pwd_$typedEmail", "") ?: ""
                    if (savedPassword.isNotEmpty()) {
                        edtPassword.setText(savedPassword)
                        cbRememberPassword.isChecked = true
                    } else {
                        edtPassword.setText("")
                        cbRememberPassword.isChecked = prefs.getBoolean(KEY_REMEMBER, false)
                    }
                } else {
                    edtPassword.setText("")
                }
            }
        })

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnLogin.isEnabled = !isLoading
        }

        viewModel.loginResult.observe(this) { success ->
            if (success) {
                viewModel.resetLoginResult()

                // Chỉ lưu credentials sau khi đăng nhập thành công
                val email = edtEmail.text.toString().trim()
                val password = edtPassword.text.toString().trim()
                val editor = prefs.edit()
                if (cbRememberPassword.isChecked) {
                    editor.putBoolean(KEY_REMEMBER, true)
                    editor.putString("last_email", email)
                    editor.putString("pwd_$email", password)
                } else {
                    editor.clear()
                }
                editor.apply()

                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
                finish()
            }
        }

        viewModel.errorMessage.observe(this) { message ->
            clearErrors()
            when {
                message.contains("nhập email", ignoreCase = true) -> tilEmail.error = message
                message.contains("nhập mật khẩu", ignoreCase = true) -> tilPassword.error = message
                else -> {
                    // Chỉ xóa credentials khi sai email/mật khẩu thực sự (lỗi từ Firebase)
                    val email = edtEmail.text.toString().trim()
                    prefs.edit()
                        .remove("pwd_$email")
                        .putBoolean(KEY_REMEMBER, false)
                        .apply()
                    cbRememberPassword.isChecked = false

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