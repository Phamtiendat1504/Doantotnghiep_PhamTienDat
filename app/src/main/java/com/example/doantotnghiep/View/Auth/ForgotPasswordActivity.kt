package com.example.doantotnghiep.View.Auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.doantotnghiep.R
import com.example.doantotnghiep.repository.AuthRepository
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

    private val repository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        tilPhone = findViewById(R.id.tilPhone)
        edtPhone = findViewById(R.id.edtPhone)
        btnSendOtp = findViewById(R.id.btnSendOtp)
        progressBar = findViewById(R.id.progressBar)
        btnBack = findViewById(R.id.btnBack)
        tvGoToLogin = findViewById(R.id.tvGoToLogin)

        btnSendOtp.setOnClickListener {
            tilPhone.error = null
            val phone = edtPhone.text.toString().trim()

            if (phone.isEmpty()) {
                tilPhone.error = "Vui lòng nhập số điện thoại"
                return@setOnClickListener
            }
            if (phone.length != 10 || !phone.startsWith("0")) {
                tilPhone.error = "Số điện thoại phải có 10 số và bắt đầu bằng 0"
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnSendOtp.isEnabled = false

            // Kiểm tra số điện thoại có trong hệ thống không
            repository.findEmailByPhone(phone,
                onSuccess = { email ->
                    // Chuyển sang format quốc tế: 0xx -> +84xx
                    val phoneInternational = "+84" + phone.substring(1)
                    sendOtp(phoneInternational, email)
                },
                onFailure = { error ->
                    progressBar.visibility = View.GONE
                    btnSendOtp.isEnabled = true
                    tilPhone.error = error
                }
            )
        }

        btnBack.setOnClickListener { finish() }
        tvGoToLogin.setOnClickListener { finish() }
    }

    private fun sendOtp(phone: String, email: String) {
        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Tự động verify trên một số thiết bị
                    progressBar.visibility = View.GONE
                    btnSendOtp.isEnabled = true
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    progressBar.visibility = View.GONE
                    btnSendOtp.isEnabled = true
                    Toast.makeText(
                        this@ForgotPasswordActivity,
                        "Gửi mã thất bại: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    progressBar.visibility = View.GONE
                    btnSendOtp.isEnabled = true

                    // Chuyển sang màn nhập OTP
                    val intent = Intent(
                        this@ForgotPasswordActivity,
                        VerifyOtpActivity::class.java
                    )
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