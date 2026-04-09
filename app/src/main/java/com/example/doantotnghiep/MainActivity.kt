package com.example.doantotnghiep

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging


class MainActivity : AppCompatActivity() {

    private lateinit var mainViewModel: MainViewModel

    // Launcher xin quyền thông báo (Android 13+)
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.d("FCM", "Quyền thông báo được cấp, đang lấy FCM Token...")
                fetchAndSaveFcmToken()
            } else {
                Log.w("FCM", "Người dùng từ chối quyền thông báo.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        PostNotificationHelper.createChannel(this)

        // Xin quyền và lấy FCM Token
        setupFcmNotifications()

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
        
        // Gọi lại để lấy và gán Token cho User mới đăng nhập
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            setupFcmNotifications()
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
            onConfirm = { startActivity(Intent(this, LoginActivity::class.java)) }
        )
    }

    /**
     * Xin quyền thông báo trên Android 13+ (API 33).
     * Nếu quyền đã được cấp rồi thì lấy Token luôn không hỏi lại.
     */
    private fun setupFcmNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                fetchAndSaveFcmToken()
            } else {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // Android 12 trở xuống không cần xin quyền, lấy Token thẳng
            fetchAndSaveFcmToken()
        }
    }

    /**
     * Lấy FCM Token từ Firebase và lưu vào Firestore
     * (Kết hợp với MyFirebaseMessagingService.onNewToken để luôn cập nhật Token mới nhất)
     */
    private fun fetchAndSaveFcmToken() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            Log.d("FCM", "FCM Token: $token")
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("fcmToken", token)
                .addOnSuccessListener { Log.d("FCM", "Lưu Token thành công!") }
                .addOnFailureListener { e -> Log.e("FCM", "Lỗi lưu Token: ${e.message}") }
        }
    }
}