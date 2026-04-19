package com.example.doantotnghiep.View.Auth

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.doantotnghiep.MainActivity
import com.example.doantotnghiep.R

class SplashActivity : AppCompatActivity() {

    private val SPLASH_DURATION = 3000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        
        // Full screen mode
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
        
        setContentView(R.layout.activity_splash)

        val imgLogo = findViewById<View>(R.id.imgLogo)
        val tvAppName = findViewById<TextView>(R.id.tvAppName)
        val tvSlogan = findViewById<TextView>(R.id.tvSlogan)
        val tvDeveloper = findViewById<TextView>(R.id.tvDeveloper)
        val topBackground = findViewById<View>(R.id.topBackground)

        // ─── 1. Hiệu ứng nền trắng đổ xuống từ trên ──────────────────
        topBackground.translationY = -600f
        ObjectAnimator.ofFloat(topBackground, "translationY", -600f, 0f).apply {
            duration = 1000
            interpolator = DecelerateInterpolator()
            start()
        }

        // ─── 2. Hiệu ứng Logo hiện hình mượt mà ──────────────────────
        imgLogo.alpha = 0f
        imgLogo.scaleX = 0.8f
        imgLogo.scaleY = 0.8f
        
        val logoAnim = ObjectAnimator.ofPropertyValuesHolder(
            imgLogo,
            PropertyValuesHolder.ofFloat("alpha", 0f, 1f),
            PropertyValuesHolder.ofFloat("scaleX", 0.8f, 1f),
            PropertyValuesHolder.ofFloat("scaleY", 0.8f, 1f)
        ).apply {
            duration = 1200
            startDelay = 300
            interpolator = DecelerateInterpolator()
        }

        // ─── 3. Hiệu ứng Tên App & Slogan trượt nhẹ lên ───────────────
        tvAppName.alpha = 0f
        tvAppName.translationY = 50f
        tvSlogan.alpha = 0f
        
        val nameAnim = ObjectAnimator.ofPropertyValuesHolder(
            tvAppName,
            PropertyValuesHolder.ofFloat("alpha", 0f, 1f),
            PropertyValuesHolder.ofFloat("translationY", 50f, 0f)
        ).apply {
            duration = 1000
            startDelay = 800
            interpolator = DecelerateInterpolator()
        }

        val sloganAnim = ObjectAnimator.ofFloat(tvSlogan, "alpha", 0f, 0.8f).apply {
            duration = 800
            startDelay = 1100
        }

        val devAnim = ObjectAnimator.ofFloat(tvDeveloper, "alpha", 0f, 0.9f).apply {
            duration = 1000
            startDelay = 1200
        }

        // Chạy đồng bộ các hiệu ứng
        AnimatorSet().apply {
            playTogether(logoAnim, nameAnim, sloganAnim, devAnim)
            start()
        }

        // Chuyển màn hình
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, SPLASH_DURATION)
    }
}