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
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val authRepository = com.example.doantotnghiep.repository.AuthRepository()
    private var activeFragment: Fragment? = null

    // Launcher xin quyền thông báo (Android 13+)
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.d("FCM", "Quyền thông báo được cấp, đang cập nhật FCM Token...")
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

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            startListeningUserStatus(currentUser.uid)
            loadBadgeWithRole(currentUser.uid)
        }

        val openTab = intent.getStringExtra("openTab")
        val action = intent.getStringExtra("action")

        if (openTab == "post") {
            bottomNav.selectedItemId = R.id.nav_post
            showTabFragment(R.id.nav_post)
        } else if (action == "open_appointments") {
            bottomNav.selectedItemId = R.id.nav_profile
            showTabFragment(R.id.nav_profile)
            // Delay một chút để Fragment kịp load rồi mới mở Activity
            window.decorView.postDelayed({
                startActivity(Intent(this, com.example.doantotnghiep.View.Auth.MyAppointmentsActivity::class.java))
            }, 500)
        } else {
            bottomNav.selectedItemId = R.id.nav_home
            showTabFragment(R.id.nav_home)
        }

        // Xử lý FCM notification khi app đang tắt (background/killed)
        if (savedInstanceState == null) {
            handleFcmNotificationIntent(intent)
        }

        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId != R.id.nav_post) {
                val postFragment = supportFragmentManager.findFragmentByTag("PostFragment") as? PostFragment
                if (postFragment != null && !postFragment.isHidden && postFragment.isDirty) {
                    postFragment.showDiscardDialog {
                        postFragment.isDirty = false
                        bottomNav.selectedItemId = item.itemId
                        showTabFragment(item.itemId)
                    }
                    return@setOnItemSelectedListener false
                }
            }
            when (item.itemId) {
                R.id.nav_home -> showTabFragment(R.id.nav_home)
                R.id.nav_search -> showTabFragment(R.id.nav_search)
                R.id.nav_ai -> return@setOnItemSelectedListener false
                R.id.nav_post -> showTabFragment(R.id.nav_post)
                R.id.nav_profile -> showTabFragment(R.id.nav_profile)
            }
            true
        }

        fabAI.setOnClickListener {
            val intent = Intent(this, com.example.doantotnghiep.View.Auth.AIChatActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Cập nhật intent mới cho Activity

        val openTab = intent.getStringExtra("openTab")
        val action = intent.getStringExtra("action")

        if (openTab == "post") {
            bottomNav.selectedItemId = R.id.nav_post
            showTabFragment(R.id.nav_post)
        } else if (action == "open_appointments") {
            bottomNav.selectedItemId = R.id.nav_profile
            showTabFragment(R.id.nav_profile)
            window.decorView.postDelayed({
                startActivity(Intent(this, com.example.doantotnghiep.View.Auth.MyAppointmentsActivity::class.java))
            }, 300)
        } else {
            bottomNav.selectedItemId = R.id.nav_home
            showTabFragment(R.id.nav_home)
        }

        // Xử lý FCM notification khi app đang chạy nền
        handleFcmNotificationIntent(intent)

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
                // Fallback: nếu không lấy được role, mặc định quyền user thường
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

        // Đánh dấu offline rồi đăng xuất Firebase
        com.example.doantotnghiep.Utils.PresenceManager.goOfflineAndThen {
            FirebaseAuth.getInstance().signOut()
        }

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





    override fun onDestroy() {
        super.onDestroy()
        userStatusListener?.remove()
        networkCallback?.let {
            try {
                (getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager).unregisterNetworkCallback(it)
            } catch (_: Exception) {
            }
        }
    }

    private fun observeNetworkStatus() {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback?.let {
            runCatching { connectivityManager.unregisterNetworkCallback(it) }
                .onFailure { error -> Log.w("MainActivity", "Network callback was already unregistered", error) }
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Mất kết nối Internet. Dữ liệu sẽ được đồng bộ khi có mạng lại.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        networkCallback = callback
        connectivityManager.registerNetworkCallback(networkRequest, callback)
    }

    private fun showTabFragment(itemId: Int) {
        val fm = supportFragmentManager
        val transaction = fm.beginTransaction()
        
        val tag = when (itemId) {
            R.id.nav_home -> "HomeFragment"
            R.id.nav_search -> "SearchFragment"
            R.id.nav_post -> "PostFragment"
            R.id.nav_profile -> "ProfileFragment"
            else -> return
        }
        
        // Ẩn tất cả các Fragment khác để tránh bị đè hoặc hiển thị chồng chéo
        val allTags = listOf("HomeFragment", "SearchFragment", "PostFragment", "ProfileFragment")
        for (t in allTags) {
            if (t != tag) {
                val f = fm.findFragmentByTag(t)
                if (f != null && !f.isHidden) {
                    transaction.hide(f)
                }
            }
        }

        var targetFragment = fm.findFragmentByTag(tag)
        if (targetFragment == null) {
            targetFragment = when (itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_search -> SearchFragment()
                R.id.nav_post -> PostFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> return
            }
            transaction.add(R.id.fragmentContainer, targetFragment, tag)
        } else {
            transaction.show(targetFragment)
        }
        
        transaction.commit()
        activeFragment = targetFragment
    }

    /**
     * Xử lý điều hướng khi người dùng bấm vào FCM notification lúc app đang tắt hoặc nền.
     * Khi app foreground: onMessageReceived đã tạo PendingIntent đúng màn hình.
     * Khi app background/killed: FCM SDK tự hiển thị notification, bấm vào → MainActivity
     * nhận data payload qua intent extras, cần đọc "type" để điều hướng đúng.
     */
    private fun handleFcmNotificationIntent(intent: Intent?) {
        val type = intent?.getStringExtra("type")?.takeIf { it.isNotEmpty() } ?: return
        val senderId = intent.getStringExtra("senderId") ?: ""
        val ticketId = intent.getStringExtra("ticketId") ?: ""
        val ticketTitle = intent.getStringExtra("ticketTitle") ?: "Yêu cầu hỗ trợ"
        when {
            type == "new_message" && senderId.isNotEmpty() -> {
                startActivity(
                    Intent(this, com.example.doantotnghiep.View.Auth.ChatActivity::class.java).apply {
                        putExtra(com.example.doantotnghiep.View.Auth.ChatActivity.EXTRA_OTHER_UID, senderId)
                    }
                )
            }
            (type == "support_reply" || type == "support_status") && ticketId.isNotEmpty() -> {
                startActivity(
                    Intent(this, com.example.doantotnghiep.View.Auth.SupportTicketDetailActivity::class.java).apply {
                        putExtra(com.example.doantotnghiep.View.Auth.SupportTicketDetailActivity.EXTRA_TICKET_ID, ticketId)
                        putExtra(com.example.doantotnghiep.View.Auth.SupportTicketDetailActivity.EXTRA_TICKET_TITLE, ticketTitle)
                        putExtra(com.example.doantotnghiep.View.Auth.SupportTicketDetailActivity.EXTRA_TICKET_STATUS, "new")
                    }
                )
            }
            type == "post_expiry_warning" || type == "post_expired_deleted" -> {
                startActivity(Intent(this, com.example.doantotnghiep.View.Auth.MyPostsActivity::class.java))
            }
            type != "BROADCAST" -> {
                window.decorView.postDelayed({
                    if (!isDestroyed) {
                        startActivity(Intent(this, com.example.doantotnghiep.View.Auth.MyAppointmentsActivity::class.java))
                    }
                }, 500)
            }
        }
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

    private fun fetchAndSaveFcmToken() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            val updates = hashMapOf<String, Any>(
                "fcmToken" to token,
                "lastLoginAt" to System.currentTimeMillis()
            )
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener { Log.d("FCM", "Cập nhật Token và thời gian đăng nhập thành công.") }
                .addOnFailureListener { e -> Log.e("FCM", "Lỗi cập nhật thông tin: ${e.message}") }
        }
    }
}
