package com.example.doantotnghiep.View.Auth

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.doantotnghiep.R

class InfoContentActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_CONTENT = "extra_content"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info_content)

        val toolbar = findViewById<Toolbar>(R.id.toolbarInfoContent)
        val tvTitle = findViewById<TextView>(R.id.tvInfoTitle)
        val tvContent = findViewById<TextView>(R.id.tvInfoContent)

        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Thông tin" }
        val content = intent.getStringExtra(EXTRA_CONTENT).orEmpty()

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationIcon(R.drawable.ic_back)
        toolbar.setNavigationOnClickListener { finish() }

        tvTitle.text = title
        tvContent.text = content
    }
}
