package com.example.doantotnghiep.View.Auth

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.doantotnghiep.MainActivity
import com.example.doantotnghiep.R
import com.google.android.material.button.MaterialButton
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class PostSuccessActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_success)

        val title    = intent.getStringExtra("title") ?: ""
        val price    = intent.getLongExtra("price", 0L)
        val location = intent.getStringExtra("location") ?: ""

        val formatter = DecimalFormat("#,###", DecimalFormatSymbols(Locale("vi", "VN")).apply {
            groupingSeparator = '.'
        })

        if (title.isNotEmpty())    findViewById<TextView>(R.id.tvPostTitle).text = title
        if (price > 0)             findViewById<TextView>(R.id.tvPostPrice).text = "${formatter.format(price)} đ/tháng"
        if (location.isNotEmpty()) findViewById<TextView>(R.id.tvPostLocation).text = location

        // Nút Xem bài đăng của tôi → sang MyPostsActivity
        findViewById<MaterialButton>(R.id.btnViewMyPosts).setOnClickListener {
            startActivity(Intent(this, MyPostsActivity::class.java))
            finish()
        }

        // Nút Đăng bài khác → về tab Post của MainActivity
        findViewById<MaterialButton>(R.id.btnPostAnother).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("openTab", "post")
            }
            startActivity(intent)
            finish()
        }

        // Chặn nút Back — không cho quay về form đăng bài cũ
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent(this@PostSuccessActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
                finish()
            }
        })
    }
}