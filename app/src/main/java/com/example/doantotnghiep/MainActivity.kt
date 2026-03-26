package com.example.doantotnghiep

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.doantotnghiep.View.Fragment.HomeFragment
import com.example.doantotnghiep.View.Fragment.SearchFragment
import com.example.doantotnghiep.View.Fragment.PostFragment
import com.example.doantotnghiep.View.Fragment.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        val fabAI = findViewById<FloatingActionButton>(R.id.fabAI)

        // Mặc định hiển thị Trang chủ
        loadFragment(HomeFragment())

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> loadFragment(HomeFragment())
                R.id.nav_search -> loadFragment(SearchFragment())
                R.id.nav_ai -> {
                    // Không làm gì, để FAB xử lý
                    return@setOnItemSelectedListener false
                }
                R.id.nav_post -> loadFragment(PostFragment())
                R.id.nav_profile -> loadFragment(ProfileFragment())
            }
            true
        }

        // Bấm nút AI nổi
        fabAI.setOnClickListener {
            Toast.makeText(this, "Chatbox AI đang phát triển", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}