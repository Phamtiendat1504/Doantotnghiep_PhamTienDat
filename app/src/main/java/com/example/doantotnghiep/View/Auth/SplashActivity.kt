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
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.doantotnghiep.MainActivity
import com.example.doantotnghiep.R

class SplashActivity : AppCompatActivity() {

    private val splashDuration = 2400L
    private val handler = Handler(Looper.getMainLooper())
    private var progressAnimator: ValueAnimator? = null
    private var logoFloatAnimator: ObjectAnimator? = null

    private val navigateRunnable = Runnable {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        setContentView(R.layout.activity_splash)

        val splashCard = findViewById<View>(R.id.splashCard)
        val logoContainer = findViewById<View>(R.id.logoContainer)
        val tvAppName = findViewById<TextView>(R.id.tvAppName)
        val tvSlogan = findViewById<TextView>(R.id.tvSlogan)
        val loadingContainer = findViewById<View>(R.id.loadingContainer)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        splashCard.alpha = 0f
        splashCard.translationY = 34f
        tvAppName.alpha = 0f
        tvSlogan.alpha = 0f
        loadingContainer.alpha = 0f
        logoContainer.scaleX = 0.84f
        logoContainer.scaleY = 0.84f

        splashCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(580)
            .setInterpolator(DecelerateInterpolator())
            .start()

        logoContainer.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(120)
            .setDuration(680)
            .setInterpolator(DecelerateInterpolator())
            .start()

        tvAppName.animate()
            .alpha(1f)
            .setStartDelay(260)
            .setDuration(520)
            .setInterpolator(DecelerateInterpolator())
            .start()

        tvSlogan.animate()
            .alpha(0.96f)
            .setStartDelay(420)
            .setDuration(460)
            .start()

        loadingContainer.animate()
            .alpha(1f)
            .setStartDelay(520)
            .setDuration(500)
            .start()

        logoFloatAnimator = ObjectAnimator.ofFloat(logoContainer, View.TRANSLATION_Y, 0f, -6f, 0f).apply {
            duration = 1800
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }

        progressAnimator = ValueAnimator.ofInt(0, 100).apply {
            duration = splashDuration - 150
            interpolator = LinearInterpolator()
            addUpdateListener { progressBar.progress = it.animatedValue as Int }
            start()
        }

        handler.postDelayed(navigateRunnable, splashDuration)
    }

    override fun onDestroy() {
        handler.removeCallbacks(navigateRunnable)
        progressAnimator?.cancel()
        logoFloatAnimator?.cancel()
        super.onDestroy()
    }
}
