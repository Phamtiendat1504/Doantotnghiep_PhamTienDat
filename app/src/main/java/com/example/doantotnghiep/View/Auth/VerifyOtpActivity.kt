package com.example.doantotnghiep.View.Auth

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.ViewModel.VerifyOtpViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

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
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var countDownTimer: CountDownTimer? = null

    private val viewModel: VerifyOtpViewModel by viewModels()

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

        startCountDown()
        observeViewModel()

        btnVerifyOtp.setOnClickListener {
            tilOtp.error = null
            val otp = edtOtp.text.toString().trim()
            viewModel.verifyOtp(verificationId, otp, email)
        }

        tvResend.setOnClickListener {
            if (!tvResend.isEnabled) return@setOnClickListener
            resendOtp()
        }

        btnBack.setOnClickListener { finish() }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            btnVerifyOtp.isEnabled = !loading
        }

        viewModel.verifySuccess.observe(this) { emailAddr ->
            val intent = Intent(this, ResetPasswordActivity::class.java).apply {
                putExtra("email", emailAddr)
            }
            startActivity(intent)
            finish()
        }

        viewModel.invalidOtp.observe(this) { invalid ->
            if (invalid) tilOtp.error = "Mã xác nhận không đúng"
        }

        viewModel.errorMessage.observe(this) { msg ->
            when (msg) {
                "otp_empty" -> tilOtp.error = "Vui lòng nhập mã xác nhận"
                "otp_invalid" -> tilOtp.error = "Mã xác nhận phải có 6 số"
                else -> if (!msg.isNullOrEmpty()) MessageUtils.showErrorDialog(this, "Lỗi", msg)
            }
        }
    }

    // PhoneAuth resend bắt buộc cần setActivity(this) nên giữ ở Activity
    private fun resendOtp() {
        tvResend.isEnabled = false
        progressBar.visibility = View.VISIBLE

        val optionsBuilder = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnSuccessListener {
                            FirebaseAuth.getInstance().signOut()
                            progressBar.visibility = View.GONE
                            viewModel.verifyOtp(verificationId, "", email) // trigger sendResetEmail via success path
                        }
                        .addOnFailureListener {
                            progressBar.visibility = View.GONE
                            tvResend.isEnabled = true
                        }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    progressBar.visibility = View.GONE
                    tvResend.isEnabled = true
                    MessageUtils.showErrorDialog(this@VerifyOtpActivity, "Gửi lại thất bại", e.message ?: "Vui lòng thử lại")
                }

                override fun onCodeSent(
                    newVerificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    progressBar.visibility = View.GONE
                    verificationId = newVerificationId
                    resendToken = token
                    edtOtp.text?.clear()
                    tilOtp.error = null
                    startCountDown()
                }
            })

        resendToken?.let { optionsBuilder.setForceResendingToken(it) }
        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    }

    private fun startCountDown() {
        countDownTimer?.cancel()
        tvResend.isEnabled = false
        tvResend.setTextColor(0xFF999999.toInt())

        countDownTimer = object : CountDownTimer(60000, 1000) {
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

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}