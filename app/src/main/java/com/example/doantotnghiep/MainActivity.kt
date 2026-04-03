package com.example.doantotnghiep

import android.content.Intent
import android.os.Bundle
import com.example.doantotnghiep.Utils.MessageUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.doantotnghiep.View.Fragment.HomeFragment
import com.example.doantotnghiep.View.Fragment.SearchFragment
import com.example.doantotnghiep.View.Fragment.PostFragment
import com.example.doantotnghiep.View.Fragment.ProfileFragment
import com.example.doantotnghiep.View.Auth.LoginActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        val fabAI = findViewById<FloatingActionButton>(R.id.fabAI)

        // Mặc định hiển thị Trang chủ, hoặc tab được yêu cầu từ Intent
        val openTab = intent.getStringExtra("openTab")
        if (openTab == "post") {
            loadFragment(PostFragment())
            bottomNav.selectedItemId = R.id.nav_post
        } else {
            loadFragment(HomeFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
            when (item.itemId) {
                R.id.nav_home -> loadFragment(HomeFragment())
                R.id.nav_search -> {
                    if (!isLoggedIn) { promptLogin(); return@setOnItemSelectedListener false }
                    loadFragment(SearchFragment())
                }
                R.id.nav_ai -> return@setOnItemSelectedListener false
                R.id.nav_post -> {
                    if (!isLoggedIn) { promptLogin(); return@setOnItemSelectedListener false }
                    loadFragment(PostFragment())
                }
                R.id.nav_profile -> loadFragment(ProfileFragment())
            }
            true
        }

        // Bấm nút AI nổi
        fabAI.setOnClickListener {
            MessageUtils.showInfoDialog(this, "Thông báo", "Chatbox AI đang được phát triển, vui lòng quay lại sau!")
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun promptLogin() {
        MessageUtils.showInfoDialog(
            this,
            "Yêu cầu đăng nhập",
            "Bạn cần đăng nhập để sử dụng tính năng này.",
            onConfirm = {
                startActivity(Intent(this, LoginActivity::class.java))
            }
        )
    }
}