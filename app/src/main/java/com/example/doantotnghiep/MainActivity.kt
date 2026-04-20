package com.example.doantotnghiep

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.widget.Toast
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
import com.google.firebase.firestore.ListenerRegistration


class MainActivity : AppCompatActivity() {

    private lateinit var mainViewModel: MainViewModel
    private var userStatusListener: ListenerRegistration? = null
    private var appointmentListenerL: ListenerRegistration? = null
    private var appointmentListenerT: ListenerRegistration? = null
    private val authRepository = com.example.doantotnghiep.repository.AuthRepository()

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

    private lateinit var bottomNav: BottomNavigationView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottomNav)
        val fabAI = findViewById<FloatingActionButton>(R.id.fabAI)

        observeNetworkStatus()

        PostNotificationHelper.createChannel(this)

        // Xin quyền và lấy FCM Token
        setupFcmNotifications()

        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        mainViewModel.appointmentBadgeCount.observe(this) { count ->
            val badge = bottomNav.getOrCreateBadge(R.id.nav_profile)
            if (count > 0) {
                badge.isVisible = true
                badge.number = count
                badge.backgroundColor = ContextCompat.getColor(this, R.color.secondary)
                badge.badgeTextColor = ContextCompat.getColor(this, R.color.white)
            } else {
                badge.isVisible = false
            }
        }

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
            startListeningUserStatus(currentUser.uid)
            loadBadgeWithRole(currentUser.uid)
        }

        val openTab = intent.getStringExtra("openTab")
        val action = intent.getStringExtra("action")

        if (openTab == "post") {
            loadFragment(PostFragment())
            bottomNav.selectedItemId = R.id.nav_post
        } else if (action == "open_appointments") {
            loadFragment(ProfileFragment())
            bottomNav.selectedItemId = R.id.nav_profile
            // Delay một chút để Fragment kịp load rồi mới mở Activity
            window.decorView.postDelayed({
                startActivity(Intent(this, com.example.doantotnghiep.View.Auth.MyAppointmentsActivity::class.java))
            }, 500)
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
        setIntent(intent) // Cập nhật intent mới cho Activity

        val action = intent.getStringExtra("action")

        if (action == "open_appointments") {
            loadFragment(ProfileFragment())
            bottomNav.selectedItemId = R.id.nav_profile
            window.decorView.postDelayed({
                startActivity(Intent(this, com.example.doantotnghiep.View.Auth.MyAppointmentsActivity::class.java))
            }, 300)
        } else {
            bottomNav.selectedItemId = R.id.nav_home
            loadFragment(HomeFragment())
        }
        
        // Gọi lại để lấy và gán Token cho User mới đăng nhập
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            setupFcmNotifications()
            startListeningUserStatus(currentUser.uid)
            loadBadgeWithRole(currentUser.uid)
        }
    }

    /**
     * Lấy role từ Firestore rồi gọi loadAppointmentBadge đúng vai trò.
     * Giảm 50% Firestore reads so với cách mở 2 listeners song song trước đây.
     */
    private fun loadBadgeWithRole(uid: String) {
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role") ?: ""
                val isVerified = doc.getBoolean("isVerified") ?: false
                val effectiveRole = if (role == "admin") "admin" else if (isVerified) "verified" else "user"
                mainViewModel.loadAppointmentBadge(uid, effectiveRole)
            }
            .addOnFailureListener {
                // Fallback: nếu không lấy được role, dùng tenant (chỉ ngịe confirmed)
                mainViewModel.loadAppointmentBadge(uid, "user")
            }
    }

    private fun startListeningUserStatus(uid: String) {
        userStatusListener?.remove()
        userStatusListener = authRepository.listenUserStatus(uid) { isLockedOrDeleted, message ->
            if (isLockedOrDeleted) {
                handleUserLockedOrDeleted(message ?: "Tài khoản của bạn không khả dụng.")
            }
        }
    }

    private fun handleUserLockedOrDeleted(message: String) {
        // Hủy listener ngay để tránh loop
        userStatusListener?.remove()
        userStatusListener = null

        // Đăng xuất Firebase
        FirebaseAuth.getInstance().signOut()

        // Hiển thị thông báo và đá ra ngoài
        runOnUiThread {
            MessageUtils.showInfoDialog(
                this,
                "Thông báo tài khoản",
                message,
                onConfirm = {
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        com.example.doantotnghiep.Utils.PresenceManager.goOnline()
    }

    override fun onStop() {
        super.onStop()
        com.example.doantotnghiep.Utils.PresenceManager.goOffline()
    }

    override fun onDestroy() {
        super.onDestroy()
        userStatusListener?.remove()
    }

    private fun observeNetworkStatus() {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Có mạng
            }

            override fun onLost(network: Network) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Mất kết nối Internet. Dữ liệu sẽ được đồng bộ khi có mạng lại.", Toast.LENGTH_LONG).show()
                }
            }
        })
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
            val updates = hashMapOf<String, Any>(
                "fcmToken" to token,
                "lastLoginAt" to System.currentTimeMillis()
            )
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update(updates)
                .addOnSuccessListener { Log.d("FCM", "Cập nhật Token và thời gian đăng nhập thành công!") }
                .addOnFailureListener { e -> Log.e("FCM", "Lỗi cập nhật thông tin: ${e.message}") }
        }
    }
}
