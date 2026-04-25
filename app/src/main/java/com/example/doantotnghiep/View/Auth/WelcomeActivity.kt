package com.example.doantotnghiep.View.Auth

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.doantotnghiep.MainActivity
import com.example.doantotnghiep.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

class WelcomeActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val autoNavigateDelayMs = 4200L
    private var hasNavigated = false

    private var logoFloatAnimator: ObjectAnimator? = null
    private var pulseAnimator1: ObjectAnimator? = null
    private var pulseAnimator2: ObjectAnimator? = null
    private var pulseAlpha1: ObjectAnimator? = null
    private var pulseAlpha2: ObjectAnimator? = null

    private lateinit var tvUserName: TextView
    private lateinit var tvAutoHint: TextView

    private val autoNavigateRunnable = Runnable {
        navigateToMain()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_welcome_user)

        val card = findViewById<View>(R.id.welcomeCard)
        val logoPlate = findViewById<View>(R.id.logoPlate)
        val pulseDot1 = findViewById<View>(R.id.pulseDot1)
        val pulseDot2 = findViewById<View>(R.id.pulseDot2)
        val btnStart = findViewById<MaterialButton>(R.id.btnStartApp)
        tvUserName = findViewById(R.id.tvWelcomeUserName)
        tvAutoHint = findViewById(R.id.tvAutoHint)

        card.alpha = 0f
        card.translationY = 36f
        logoPlate.scaleX = 0.86f
        logoPlate.scaleY = 0.86f

        card.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(560)
            .setInterpolator(DecelerateInterpolator())
            .start()

        logoPlate.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(120)
            .setDuration(600)
            .setInterpolator(DecelerateInterpolator())
            .start()

        logoFloatAnimator = ObjectAnimator.ofFloat(logoPlate, View.TRANSLATION_Y, 0f, -8f, 0f).apply {
            duration = 1900
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }

        pulseAnimator1 = ObjectAnimator.ofFloat(pulseDot1, View.SCALE_X, 1f, 1.22f, 1f).apply {
            duration = 1700
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
        pulseAnimator2 = ObjectAnimator.ofFloat(pulseDot2, View.SCALE_X, 1f, 1.16f, 1f).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
        pulseAlpha1 = ObjectAnimator.ofFloat(pulseDot1, View.ALPHA, 0.35f, 0.85f, 0.35f).apply {
            duration = 1700
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
        pulseAlpha2 = ObjectAnimator.ofFloat(pulseDot2, View.ALPHA, 0.30f, 0.75f, 0.30f).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }

        btnStart.setOnClickListener { navigateToMain() }
        tvAutoHint.text = "Tự động chuyển sau 5 giây..."

        loadUserName()
        handler.postDelayed(autoNavigateRunnable, autoNavigateDelayMs)
    }

    private fun loadUserName() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            tvUserName.text = "Bạn"
            return
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get(Source.SERVER)
            .addOnSuccessListener { doc ->
                val fullName = doc.getString("fullName").orEmpty().trim()
                tvUserName.text = if (fullName.isNotBlank()) fullName else "Bạn"
            }
            .addOnFailureListener {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener { doc ->
                        val fullName = doc.getString("fullName").orEmpty().trim()
                        tvUserName.text = if (fullName.isNotBlank()) fullName else "Bạn"
                    }
                    .addOnFailureListener {
                        tvUserName.text = "Bạn"
                    }
            }
    }

    private fun navigateToMain() {
        if (hasNavigated) return
        hasNavigated = true
        handler.removeCallbacks(autoNavigateRunnable)
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoNavigateRunnable)
        logoFloatAnimator?.cancel()
        pulseAnimator1?.cancel()
        pulseAnimator2?.cancel()
        pulseAlpha1?.cancel()
        pulseAlpha2?.cancel()
        super.onDestroy()
    }
}

