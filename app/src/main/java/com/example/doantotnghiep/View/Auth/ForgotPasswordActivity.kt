package com.example.doantotnghiep.View.Auth

import android.content.Intent
import android.os.Bundle
import android.view.View
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
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var tilPhone: TextInputLayout
    private lateinit var edtPhone: TextInputEditText
    private lateinit var btnSendOtp: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: ImageView
    private lateinit var tvGoToLogin: TextView

    private val viewModel: ForgotPasswordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        tilPhone = findViewById(R.id.tilPhone)
        edtPhone = findViewById(R.id.edtPhone)
        btnSendOtp = findViewById(R.id.btnSendOtp)
        progressBar = findViewById(R.id.progressBar)
        btnBack = findViewById(R.id.btnBack)
        tvGoToLogin = findViewById(R.id.tvGoToLogin)

        observeViewModel()

        btnSendOtp.setOnClickListener {
            tilPhone.error = null
            val phone = edtPhone.text.toString().trim()
            viewModel.findEmailByPhone(phone)
        }

        btnBack.setOnClickListener { finish() }
        tvGoToLogin.setOnClickListener { finish() }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            btnSendOtp.isEnabled = !loading
        }

        // Khi tìm thấy email → gửi OTP (PhoneAuth bắt buộc cần Activity nên giữ ở đây)
        viewModel.emailFound.observe(this) { email ->
            val phone = edtPhone.text.toString().trim()
            val phoneInternational = "+84" + phone.substring(1)
            sendOtp(phoneInternational, email)
        }

        viewModel.errorMessage.observe(this) { msg ->
            if (!msg.isNullOrEmpty()) tilPhone.error = msg
        }
    }

    private fun sendOtp(phone: String, email: String) {
        progressBar.visibility = View.VISIBLE
        btnSendOtp.isEnabled = false

        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    progressBar.visibility = View.GONE
                    btnSendOtp.isEnabled = true
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    progressBar.visibility = View.GONE
                    btnSendOtp.isEnabled = true
                    MessageUtils.showErrorDialog(this@ForgotPasswordActivity, "Gửi mã thất bại", e.message ?: "Vui lòng thử lại")
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    progressBar.visibility = View.GONE
                    btnSendOtp.isEnabled = true

                    val intent = Intent(this@ForgotPasswordActivity, VerifyOtpActivity::class.java)
                    intent.putExtra("verificationId", verificationId)
                    intent.putExtra("email", email)
                    intent.putExtra("phone", phone)
                    startActivity(intent)
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }
}