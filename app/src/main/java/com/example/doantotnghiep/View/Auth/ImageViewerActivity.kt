package com.example.doantotnghiep.View.Auth

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.doantotnghiep.R

class ImageViewerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        val imgFullScreen = findViewById<ImageView>(R.id.imgFullScreen)
        val btnClose = findViewById<ImageView>(R.id.btnClose)

        val imageUrl = intent.getStringExtra("imageUrl") ?: ""

        if (imageUrl.isNotEmpty()) {
            Glide.with(this).load(imageUrl).into(imgFullScreen)
        }

        btnClose.setOnClickListener { finish() }
        imgFullScreen.setOnClickListener { finish() }
    }
}