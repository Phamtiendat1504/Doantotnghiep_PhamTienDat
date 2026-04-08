package com.example.doantotnghiep

import android.content.Intent
import android.os.Bundle
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.Utils.PostNotificationHelper
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.doantotnghiep.View.Fragment.HomeFragment
import com.example.doantotnghiep.View.Fragment.SearchFragment
import com.example.doantotnghiep.View.Fragment.PostFragment
import com.example.doantotnghiep.View.Fragment.ProfileFragment
import com.example.doantotnghiep.View.Auth.LoginActivity
import com.example.doantotnghiep.ViewModel.MainViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var mainViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PostNotificationHelper.createChannel(this)

        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        mainViewModel.expiredPostsCount.observe(this) { count ->
            if (count > 0) {
                MessageUtils.showInfoDialog(
                    this,
                    "Bài đăng hết hạn",
                    "Bạn có $count bài đăng đã hết hạn 2 tháng và đã bị ẩn khỏi kết quả tìm kiếm.\n\nVào \"Bài đăng của tôi\" để gia hạn thêm 2 tháng hoặc đánh dấu đã cho thuê."
                )
            }
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            val lastCheck = prefs.getLong("last_expire_check_${currentUser.uid}", 0L)
            val oneDayMs = 24 * 60 * 60 * 1000L
            if (System.currentTimeMillis() - lastCheck > oneDayMs) {
                prefs.edit().putLong("last_expire_check_${currentUser.uid}", System.currentTimeMillis()).apply()
                mainViewModel.checkAndExpirePosts(currentUser.uid)
            }
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        val fabAI = findViewById<FloatingActionButton>(R.id.fabAI)

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
                R.id.nav_search -> loadFragment(SearchFragment())
                R.id.nav_ai -> return@setOnItemSelectedListener false
                R.id.nav_post -> {
                    if (!isLoggedIn) { promptLogin(); return@setOnItemSelectedListener false }
                    loadFragment(PostFragment())
                }
                R.id.nav_profile -> loadFragment(ProfileFragment())
            }
            true
        }

        fabAI.setOnClickListener {
            MessageUtils.showInfoDialog(this, "Thông báo", "Chatbox AI đang được phát triển, vui lòng quay lại sau!")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_home
        loadFragment(HomeFragment())
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
            onConfirm = { startActivity(Intent(this, LoginActivity::class.java)) }
        )
    }
}