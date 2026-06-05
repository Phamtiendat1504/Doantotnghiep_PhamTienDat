package com.example.doantotnghiep.View.Auth

import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.ViewModel.ForgotPasswordViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var tilEmail: TextInputLayout
    private lateinit var edtEmail: TextInputEditText
    private lateinit var btnSendEmail: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: ImageView
    private lateinit var tvGoToLogin: TextView

    private val viewModel: ForgotPasswordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        bindViews()
        observeViewModel()
        setupListeners()
    }

    private fun bindViews() {
        tilEmail      = findViewById(R.id.tilEmail)
        edtEmail      = findViewById(R.id.edtEmail)
        btnSendEmail  = findViewById(R.id.btnSendEmail)
        progressBar   = findViewById(R.id.progressBar)
        btnBack       = findViewById(R.id.btnBack)
        tvGoToLogin   = findViewById(R.id.tvGoToLogin)
    }

    private fun setupListeners() {
        btnSendEmail.setOnClickListener {
            tilEmail.error = null

            val email = edtEmail.text.toString().trim()

            hideKeyboard()

            viewModel.requestPasswordReset(email)
        }

        edtEmail.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                btnSendEmail.performClick()
                true
            } else {
                false
            }
        }

        btnBack.setOnClickListener { finish() }
        tvGoToLogin.setOnClickListener { finish() }
    }

    private fun observeViewModel() {

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnSendEmail.isEnabled = !isLoading
            edtEmail.isEnabled = !isLoading
        }

        // Lỗi ô Email
        viewModel.emailError.observe(this) { msg ->
            tilEmail.error = if (msg.isNullOrEmpty()) null else msg
        }

        // Lỗi chung (thông tin không khớp, lỗi mạng...)
        viewModel.generalError.observe(this) { msg ->
            if (!msg.isNullOrEmpty()) {
                viewModel.clearGeneralError()
                MessageUtils.showErrorDialog(
                    this,
                    "Xác minh thất bại",
                    msg
                )
            }
        }

        // Gửi email thành công
        viewModel.sendEmailSuccess.observe(this) { email ->
            if (email.isNullOrEmpty()) return@observe
            viewModel.clearSendEmailSuccess()
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Đã gửi liên kết đặt lại")
                .setMessage(
                    "Nếu địa chỉ $email đã được đăng ký trong hệ thống, một liên kết đặt lại mật khẩu sẽ được gửi đến hộp thư của bạn.\n\n" +
                    "Vui lòng kiểm tra hộp thư (kể cả thư mục Spam) và nhấn vào liên kết trong vòng 1 giờ.\n\n" +
                    "Lưu ý bảo mật: Nếu bạn không yêu cầu đặt lại mật khẩu, " +
                    "hãy bỏ qua email này — tài khoản của bạn vẫn an toàn."
                )
                .setPositiveButton("Đã hiểu") { dialog, _ ->
                    dialog.dismiss()
                    finish()
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }
}
