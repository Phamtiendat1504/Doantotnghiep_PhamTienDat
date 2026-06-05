package com.example.doantotnghiep.View.Auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.ViewModel.AuthViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegisterActivity : AppCompatActivity() {

    private lateinit var tilFullName: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPhone: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var edtFullName: TextInputEditText
    private lateinit var edtEmail: TextInputEditText
    private lateinit var edtPhone: TextInputEditText
    private lateinit var edtPassword: TextInputEditText
    private lateinit var edtConfirmPassword: TextInputEditText
    private lateinit var btnRegister: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: ImageView
    private lateinit var tvGoToLogin: TextView
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        tilFullName = findViewById(R.id.tilFullName)
        tilEmail = findViewById(R.id.tilEmail)
        tilPhone = findViewById(R.id.tilPhone)
        tilPassword = findViewById(R.id.tilPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        edtFullName = findViewById(R.id.edtFullName)
        edtEmail = findViewById(R.id.edtEmail)
        edtPhone = findViewById(R.id.edtPhone)
        edtPassword = findViewById(R.id.edtPassword)
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        progressBar = findViewById(R.id.progressBar)
        btnBack = findViewById(R.id.btnBack)
        tvGoToLogin = findViewById(R.id.tvGoToLogin)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        // Dialog xác nhận khi bấm nút Back hệ thống
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (hasAnyInputFilled()) {
                    showExitConfirmDialog()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnRegister.isEnabled = !isLoading
        }

        // Dialog đăng ký thành công
        viewModel.registerResult.observe(this) { success ->
            if (success) {
                MessageUtils.showSuccessDialog(
                    this,
                    "Đăng ký thành công!",
                    "Tài khoản của bạn đã được tạo thành công!\n\nEmail xác thực đã được gửi đến\n${edtEmail.text.toString().trim()}\n\nVui lòng kiểm tra hộp thư và nhấn link xác thực trước khi đăng nhập."
                ) {
                    viewModel.resetRegisterResult()
                    viewModel.logOut()
                    val intent = Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }

        viewModel.registerFieldError.observe(this) { fieldError ->
            if (fieldError == null) return@observe
            clearErrors()
            when (fieldError.field) {
                "fullName"        -> tilFullName.error = fieldError.message
                "email"           -> tilEmail.error = fieldError.message
                "phone"           -> tilPhone.error = fieldError.message
                "password"        -> tilPassword.error = fieldError.message
                "confirmPassword" -> tilConfirmPassword.error = fieldError.message
            }
            viewModel.resetRegisterFieldError()
        }

        viewModel.errorMessage.observe(this) { message ->
            if (message.isNullOrEmpty()) return@observe
            if (message.contains("mạng", ignoreCase = true) || message.contains("Internet", ignoreCase = true)) {
                android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
            } else {
                MessageUtils.showErrorDialog(this, "Lỗi đăng ký", message)
            }
            viewModel.resetErrorMessage()
        }

        btnRegister.setOnClickListener {
            clearErrors()
            val fullName = edtFullName.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val phone = edtPhone.text.toString().trim()
            val password = edtPassword.text.toString()
            val confirmPassword = edtConfirmPassword.text.toString()
            viewModel.register(fullName, email, phone, password, confirmPassword)
        }

        edtConfirmPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                btnRegister.performClick()
                true
            } else {
                false
            }
        }

        // Nút Back trong UI và link "Đã có tài khoản" đều hiện dialog xác nhận nếu đã nhập thông tin
        btnBack.setOnClickListener {
            if (hasAnyInputFilled()) showExitConfirmDialog() else finish()
        }
        tvGoToLogin.setOnClickListener {
            if (hasAnyInputFilled()) showExitConfirmDialog() else finish()
        }
    }

    private fun hasAnyInputFilled(): Boolean =
        edtFullName.text?.isNotEmpty() == true ||
        edtEmail.text?.isNotEmpty() == true ||
        edtPhone.text?.isNotEmpty() == true ||
        edtPassword.text?.isNotEmpty() == true ||
        edtConfirmPassword.text?.isNotEmpty() == true

    private fun showExitConfirmDialog() {
        MessageUtils.showConfirmDialog(
            context = this,
            title = "Bỏ qua đăng ký?",
            message = "Thông tin bạn đã nhập sẽ bị mất. Bạn có chắc muốn thoát không?",
            positiveText = "Thoát",
            negativeText = "Tiếp tục điền",
            onConfirm = { finish() }
        )
    }

    private fun clearErrors() {
        tilFullName.error = null
        tilEmail.error = null
        tilPhone.error = null
        tilPassword.error = null
        tilConfirmPassword.error = null
    }
}
