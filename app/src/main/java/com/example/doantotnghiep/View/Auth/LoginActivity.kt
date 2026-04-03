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

    private val PREF_NAME = "login_prefs"
    private val KEY_REMEMBER = "remember_password"
    // Password lưu theo key "pwd_<email>" để mỗi email có password riêng

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

        val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        val isRemember = prefs.getBoolean(KEY_REMEMBER, false)

        // Khôi phục email lần cuối đăng nhập
        if (isRemember) {
            val lastEmail = prefs.getString("last_email", "") ?: ""
            edtEmail.setText(lastEmail)
            if (lastEmail.isNotEmpty()) {
                edtPassword.setText(prefs.getString("pwd_$lastEmail", ""))
            }
            cbRememberPassword.isChecked = true
        }

        // Khi người dùng gõ email khác → tự điền password tương ứng nếu đã lưu
        edtEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!isRemember && !cbRememberPassword.isChecked) return
                val typedEmail = s?.toString()?.trim() ?: return
                val savedPwd = prefs.getString("pwd_$typedEmail", null)
                if (savedPwd != null) {
                    edtPassword.setText(savedPwd)
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
                viewModel.loginResult.value = false
                // Quay lại MainActivity (đang chạy phía sau)
                finish()
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

            // Lưu hoặc xóa thông tin đăng nhập tùy theo checkbox
            val editor = getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit()
            if (cbRememberPassword.isChecked) {
                editor.putBoolean(KEY_REMEMBER, true)
                editor.putString("last_email", email)
                editor.putString("pwd_$email", password) // lưu password theo từng email
            } else {
                editor.clear()
            }
            editor.apply()

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