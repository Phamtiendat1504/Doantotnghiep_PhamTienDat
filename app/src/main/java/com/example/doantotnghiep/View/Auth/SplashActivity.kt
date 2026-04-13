package com.example.doantotnghiep.View.Auth

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.example.doantotnghiep.MainActivity
import com.example.doantotnghiep.R

class SplashActivity : AppCompatActivity() {

    private val SPLASH_DURATION = 2500L // 2.5 giây

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ẩn action bar
        supportActionBar?.hide()
        setContentView(R.layout.activity_splash)

        val imgLogo   = findViewById<View>(R.id.imgLogo)
        val tvAppName = findViewById<View>(R.id.tvAppName)
        val tvSlogan  = findViewById<View>(R.id.tvSlogan)
        val logoContainer = findViewById<View>(R.id.logoContainer)
        val layoutDots = findViewById<View>(R.id.layoutDots)
        val dot1 = findViewById<View>(R.id.dot1)
        val dot2 = findViewById<View>(R.id.dot2)
        val dot3 = findViewById<View>(R.id.dot3)

        // === Animation logo container: fade in + scale up ===
        val logoAlpha = ObjectAnimator.ofFloat(logoContainer, "alpha", 0f, 1f).apply {
            duration = 800
            startDelay = 200
        }
        val logoScaleX = ObjectAnimator.ofFloat(logoContainer, "scaleX", 0.7f, 1f).apply {
            duration = 800
            startDelay = 200
            interpolator = android.view.animation.OvershootInterpolator()
        }
        val logoScaleY = ObjectAnimator.ofFloat(logoContainer, "scaleY", 0.7f, 1f).apply {
            duration = 800
            startDelay = 200
            interpolator = android.view.animation.OvershootInterpolator()
        }

        // === Hiệu ứng "Pulse" (nhịp thở) cho Logo lặp vô tận ===
        val pulseX = ObjectAnimator.ofFloat(imgLogo, "scaleX", 1f, 1.1f, 1f).apply {
            duration = 2000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        val pulseY = ObjectAnimator.ofFloat(imgLogo, "scaleY", 1f, 1.1f, 1f).apply {
            duration = 2000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }

        // === Animation tên app: hiện ra với khoảng cách chữ rộng dần ===
        tvAppName.translationY = 40f
        val nameAlpha = ObjectAnimator.ofFloat(tvAppName, "alpha", 0f, 1f).apply {
            duration = 800
            startDelay = 600
        }
        val nameTranslate = ObjectAnimator.ofFloat(tvAppName, "translationY", 40f, 0f).apply {
            duration = 800
            startDelay = 600
            interpolator = DecelerateInterpolator()
        }

        // === Animation slogan: fade in ===
        val sloganAlpha = ObjectAnimator.ofFloat(tvSlogan, "alpha", 0f, 1f).apply {
            duration = 500
            startDelay = 1100
        }

        // === Animation dots: fade in ===
        val dotsAlpha = ObjectAnimator.ofFloat(layoutDots, "alpha", 0f, 1f).apply {
            duration = 400
            startDelay = 1400
        }

        AnimatorSet().apply {
            playTogether(logoAlpha, logoScaleX, logoScaleY, nameAlpha, nameTranslate, sloganAlpha, dotsAlpha)
            start()
        }

        // Chạy hiệu ứng Pulse sau khi logo đã hiện xong
        pulseX.start()
        pulseY.start()

        // === Nhấp nháy dots loading ===
        val handler = Handler(Looper.getMainLooper())
        var dotStep = 0
        val dotRunnable = object : Runnable {
            override fun run() {
                dotStep = (dotStep + 1) % 3
                dot1.alpha = if (dotStep == 0) 1f else 0.4f
                dot2.alpha = if (dotStep == 1) 1f else 0.4f
                dot3.alpha = if (dotStep == 2) 1f else 0.4f
                handler.postDelayed(this, 300)
            }
        }
        handler.postDelayed(dotRunnable, 1500)

        // === Chuyển sang MainActivity sau SPLASH_DURATION ===
        Handler(Looper.getMainLooper()).postDelayed({
            handler.removeCallbacks(dotRunnable)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            // Hiệu ứng chuyển màn hình mượt
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, SPLASH_DURATION)
    }
}
