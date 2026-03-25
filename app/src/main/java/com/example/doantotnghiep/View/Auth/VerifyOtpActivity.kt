package com.example.doantotnghiep.View.Auth

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.doantotnghiep.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider

class VerifyOtpActivity : AppCompatActivity() {

    private lateinit var tilOtp: TextInputLayout
    private lateinit var edtOtp: TextInputEditText
    private lateinit var btnVerifyOtp: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: ImageView
    private lateinit var tvDescription: TextView
    private lateinit var tvResend: TextView

    private var verificationId = ""
    private var email = ""
    private var phone = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_otp)

        tilOtp = findViewById(R.id.tilOtp)
        edtOtp = findViewById(R.id.edtOtp)
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp)
        progressBar = findViewById(R.id.progressBar)
        btnBack = findViewById(R.id.btnBack)
        tvDescription = findViewById(R.id.tvDescription)
        tvResend = findViewById(R.id.tvResend)

        verificationId = intent.getStringExtra("verificationId") ?: ""
        email = intent.getStringExtra("email") ?: ""
        phone = intent.getStringExtra("phone") ?: ""

        tvDescription.text = "Mã xác nhận 6 số đã được gửi đến\n$phone"

        // Đếm ngược 60 giây
        startCountDown()

        btnVerifyOtp.setOnClickListener {
            tilOtp.error = null
            val otp = edtOtp.text.toString().trim()

            if (otp.isEmpty()) {
                tilOtp.error = "Vui lòng nhập mã xác nhận"
                return@setOnClickListener
            }
            if (otp.length != 6) {
                tilOtp.error = "Mã xác nhận phải có 6 số"
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnVerifyOtp.isEnabled = false

            // Xác thực OTP
            val credential = PhoneAuthProvider.getCredential(verificationId, otp)
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnSuccessListener {
                    progressBar.visibility = View.GONE
                    btnVerifyOtp.isEnabled = true

                    // Chuyển sang màn đặt mật khẩu mới
                    val intent = Intent(this, ResetPasswordActivity::class.java)
                    intent.putExtra("email", email)
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    btnVerifyOtp.isEnabled = true
                    tilOtp.error = "Mã xác nhận không đúng"
                }
        }

        btnBack.setOnClickListener { finish() }
    }

    private fun startCountDown() {
        tvResend.isEnabled = false
        tvResend.setTextColor(0xFF999999.toInt())

        object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                tvResend.text = "Gửi lại mã sau ${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                tvResend.text = "Gửi lại mã"
                tvResend.setTextColor(0xFF1976D2.toInt())
                tvResend.isEnabled = true
            }
        }.start()
    }
}