package com.example.doantotnghiep.View.Auth

import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.ViewModel.ForgotPasswordViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var tilEmail: TextInputLayout
    private lateinit var edtEmail: TextInputEditText
    private lateinit var tilPhone: TextInputLayout
    private lateinit var edtPhone: TextInputEditText
    private lateinit var btnSendEmail: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: ImageView
    private lateinit var tvGoToLogin: TextView
    private lateinit var cardSuccess: CardView
    private lateinit var tvSuccessMessage: TextView

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
        tilPhone      = findViewById(R.id.tilPhone)
        edtPhone      = findViewById(R.id.edtPhone)
        btnSendEmail  = findViewById(R.id.btnSendEmail)
        progressBar   = findViewById(R.id.progressBar)
        btnBack       = findViewById(R.id.btnBack)
        tvGoToLogin   = findViewById(R.id.tvGoToLogin)
        cardSuccess   = findViewById(R.id.cardSuccess)
        tvSuccessMessage = findViewById(R.id.tvSuccessMessage)
    }

    private fun setupListeners() {
        btnSendEmail.setOnClickListener {
            // Xóa lỗi cũ
            tilEmail.error = null
            tilPhone.error = null

            val email = edtEmail.text.toString().trim()
            val phone = edtPhone.text.toString().trim()

            // Ẩn bàn phím
            hideKeyboard()

            viewModel.requestPasswordReset(email, phone)
        }

        btnBack.setOnClickListener { finish() }
        tvGoToLogin.setOnClickListener { finish() }
    }

    private fun observeViewModel() {

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnSendEmail.isEnabled = !isLoading
            edtEmail.isEnabled = !isLoading
            edtPhone.isEnabled = !isLoading
        }

        // Lỗi ô Email
        viewModel.emailError.observe(this) { msg ->
            tilEmail.error = if (msg.isNullOrEmpty()) null else msg
        }

        // Lỗi ô Số điện thoại
        viewModel.phoneError.observe(this) { msg ->
            tilPhone.error = if (msg.isNullOrEmpty()) null else msg
        }

        // Lỗi chung (thông tin không khớp, lỗi mạng...)
        viewModel.generalError.observe(this) { msg ->
            if (!msg.isNullOrEmpty()) {
                MessageUtils.showErrorDialog(
                    this,
                    "Xác minh thất bại",
                    msg
                )
            }
        }

        // Gửi email thành công
        viewModel.sendEmailSuccess.observe(this) { email ->
            // Hiển thị card thành công
            cardSuccess.visibility = View.VISIBLE
            tvSuccessMessage.text =
                "Liên kết đặt lại mật khẩu đã được gửi đến\n$email\nVui lòng kiểm tra hộp thư của bạn."

            // Vô hiệu hoá form để tránh gửi lại nhiều lần
            btnSendEmail.isEnabled = false
            edtEmail.isEnabled = false
            edtPhone.isEnabled = false
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }
}