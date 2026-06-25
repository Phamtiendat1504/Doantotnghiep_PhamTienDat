package com.example.doantotnghiep.View.Auth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.doantotnghiep.R
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.ViewModel.MyPostsViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

class MyPostsActivity : AppCompatActivity() {

    private lateinit var rvMyPosts: androidx.recyclerview.widget.RecyclerView
    private lateinit var postsAdapter: com.example.doantotnghiep.View.Adapter.MyPostsAdapter
    private var paymentQrDialog: androidx.appcompat.app.AlertDialog? = null
    private var paymentWarningDialog: androidx.appcompat.app.AlertDialog? = null
    private var paymentListener: com.google.firebase.firestore.ListenerRegistration? = null
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var btnBack: ImageView
    private lateinit var menuFilter: android.widget.AutoCompleteTextView
    private lateinit var edtSearchPost: EditText
    private var currentFilterIndex = 0
    private lateinit var swipeRefreshLayout: androidx.swiperefreshlayout.widget.SwipeRefreshLayout

    private lateinit var viewModel: MyPostsViewModel
    private var allPosts = listOf<DocumentSnapshot>()
    private var currentFilter = "all"

    private data class FeaturedPackage(val label: String, val code: String, val days: Int, val price: Int)

    private val featuredPackages = listOf(
        FeaturedPackage("Nổi bật 3 ngày", "FT03", 3, 10_000),
        FeaturedPackage("Nổi bật 7 ngày", "FT07", 7, 20_000),
        FeaturedPackage("Nổi bật 15 ngày", "FT15", 15, 40_000)
    )

    private var bankId = "970422" // default MB Bank BIN
    private var accountNo = "9999999999"   // Thay thế số tài khoản thật bằng thông tin Demo bảo mật
    private var accountName = "TAI KHOAN DEMO"
    private var bankDisplay = "MB Bank"

    private fun fetchPaymentConfig(onComplete: () -> Unit) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("system_configs")
            .document("payment")
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    bankId = doc.getString("bankId") ?: bankId
                    accountNo = doc.getString("accountNo") ?: accountNo
                    accountName = doc.getString("accountName") ?: accountName
                    bankDisplay = doc.getString("bankDisplay") ?: bankDisplay
                }
                onComplete()
            }
            .addOnFailureListener {
                onComplete()
            }
    }

    private val formatter: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN"))
        symbols.groupingSeparator = '.'
        DecimalFormat("#,###", symbols)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_posts)

        rvMyPosts = findViewById(R.id.rvMyPosts)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)
        btnBack = findViewById(R.id.btnBack)
        menuFilter = findViewById(R.id.menuFilter)
        edtSearchPost = findViewById(R.id.edtSearchPost)
        setupFilterMenu()
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        btnBack.setOnClickListener { finish() }

        // Setup RecyclerView
        rvMyPosts.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        postsAdapter = com.example.doantotnghiep.View.Adapter.MyPostsAdapter(
            emptyList(),
            object : com.example.doantotnghiep.View.Adapter.MyPostsAdapter.OnPostActionListener {
                override fun onItemClick(docId: String) {
                    startActivity(Intent(this@MyPostsActivity, MyPostDetailActivity::class.java).apply { putExtra("roomId", docId) })
                }

                override fun onDeleteClick(docId: String) {
                    deletePost(docId)
                }

                override fun onFeaturedClick(docId: String, title: String) {
                    startFeaturedUpgradeFlow(docId, title)
                }

                override fun onEditClick(docId: String) {
                    startActivity(Intent(this@MyPostsActivity, EditPostActivity::class.java).apply { putExtra("roomId", docId) })
                }


            }
        )
        rvMyPosts.adapter = postsAdapter

        viewModel = ViewModelProvider(this)[MyPostsViewModel::class.java]

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading && !swipeRefreshLayout.isRefreshing) View.VISIBLE else View.GONE
            if (!isLoading) swipeRefreshLayout.isRefreshing = false
            if (isLoading) {
                postsAdapter.submitList(emptyList())
                tvEmpty.visibility = View.GONE
            }
        }

        swipeRefreshLayout.setOnRefreshListener {
            refreshList()
        }

        viewModel.posts.observe(this) { posts ->
            allPosts = posts
            applySearchFilter(edtSearchPost.text.toString().trim())
        }

        viewModel.errorMessage.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                tvEmpty.text = "Lỗi tải dữ liệu"
                tvEmpty.visibility = View.VISIBLE
                swipeRefreshLayout.isRefreshing = false
            }
        }

        val uid = viewModel.getCurrentUserId()
        // Đánh dấu đã đọc trên Firebase (cờ cloud) để badge biến mất ngay trên mọi thiết bị
        if (uid != null) viewModel.markPostsAsRead(uid)

        edtSearchPost.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val keyword = s?.toString()?.trim() ?: ""
                applySearchFilter(keyword)
            }
        })

        viewModel.loadPosts("all")
    }

    private fun setupFilterMenu() {
        val options = arrayOf("Tất cả", "Chờ duyệt", "Đã duyệt", "Bị từ chối", "Đã cho thuê")
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, options)
        menuFilter.setAdapter(adapter)

        if (menuFilter.text.toString().isEmpty()) {
            menuFilter.setText(options[currentFilterIndex], false)
        }

        menuFilter.setOnItemClickListener { _, _, position, _ ->
            currentFilterIndex = position
            refreshList()
            menuFilter.clearFocus()
        }
    }

    private fun applySearchFilter(keyword: String) {
        val filtered = if (keyword.isEmpty()) allPosts
        else allPosts.filter { (it.getString("title") ?: "").contains(keyword, ignoreCase = true) }

        if (filtered.isEmpty()) {
            tvEmpty.text = if (keyword.isEmpty()) "Chưa có bài đăng nào" else "Không tìm thấy bài đăng nào"
            tvEmpty.visibility = View.VISIBLE
            rvMyPosts.visibility = View.GONE
            postsAdapter.submitList(emptyList())
            return
        }
        tvEmpty.visibility = View.GONE
        rvMyPosts.visibility = View.VISIBLE
        postsAdapter.submitList(filtered)
    }

    private fun deletePost(docId: String) {
        progressBar.visibility = View.VISIBLE
        viewModel.deletePost(docId,
            onSuccess = {
                progressBar.visibility = View.GONE
                MessageUtils.showSuccessDialog(this, "Đã xóa bài đăng", "Bài đăng của bạn và toàn bộ hình ảnh liên quan đã được xóa sạch khỏi hệ thống.") { refreshList() }
            },
            onFailure = { error ->
                progressBar.visibility = View.GONE
                MessageUtils.showErrorDialog(this, "Lỗi xóa bài", error)
            }
        )
    }

    private fun refreshList() {
        when (currentFilterIndex) {
            1 -> viewModel.loadPosts("pending")
            2 -> viewModel.loadPosts("approved")
            3 -> viewModel.loadPosts("rejected")
            4 -> viewModel.loadPosts("rented")
            else -> viewModel.loadPosts("all")
        }
    }

    private fun startFeaturedUpgradeFlow(roomId: String, roomTitle: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val loadingDialog = MessageUtils.showLoadingDialog(this, "Đang kiểm tra giao dịch...")
        
        val activeStatuses = listOf("waiting_for_payment", "paid", "paid_waiting_admin", "rejected")
        db.collection("featured_upgrade_requests")
            .whereEqualTo("uid", uid)
            .whereEqualTo("roomId", roomId)
            .get(Source.SERVER)
            .addOnSuccessListener { snap ->
                val activeDoc = snap.documents.firstOrNull { activeStatuses.contains(it.getString("status")) }
                loadingDialog.dismiss()
                if (activeDoc != null) {
                    val status = activeDoc.getString("status") ?: "waiting_for_payment"
                    if (status == "waiting_for_payment") {
                        val pkg = FeaturedPackage(
                            label = activeDoc.getString("label") ?: "Nổi bật",
                            code = activeDoc.getString("code") ?: "FT",
                            days = activeDoc.getLong("days")?.toInt() ?: 0,
                            price = activeDoc.getLong("amount")?.toInt() ?: 0
                        )
                        val addInfo = activeDoc.getString("transferNote") ?: ""
                        val expiresAt = activeDoc.getLong("expiresAt") ?: ((activeDoc.getLong("createdAt") ?: System.currentTimeMillis()) + 30L * 60L * 1000L)
                        showFeaturedPaymentQrDialogContent(db, activeDoc.reference, roomId, pkg, addInfo, expiresAt)
                    } else if (status == "rejected") {
                        val rejectReason = activeDoc.getString("rejectReason") ?: ""
                        val dialogView = LayoutInflater.from(this@MyPostsActivity)
                            .inflate(R.layout.dialog_featured_rejected, null)
                        
                        val dialog = androidx.appcompat.app.AlertDialog.Builder(this@MyPostsActivity, R.style.RoundedDialogStyle)
                            .setView(dialogView)
                            .create()
                        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                        
                        dialogView.findViewById<android.widget.TextView>(R.id.tvRejectReason).text = rejectReason
                        
                        dialogView.findViewById<android.view.View>(R.id.btnCancel).setOnClickListener {
                            dialog.dismiss()
                        }
                        
                        dialogView.findViewById<android.view.View>(R.id.btnEditPost).setOnClickListener {
                            dialog.dismiss()
                            startActivity(Intent(this@MyPostsActivity, EditPostActivity::class.java).apply { putExtra("roomId", roomId) })
                        }
                        
                        dialogView.findViewById<android.view.View>(R.id.btnResubmit).setOnClickListener {
                            dialog.dismiss()
                            val progress = MessageUtils.showLoadingDialog(this@MyPostsActivity, "Đang gửi yêu cầu duyệt lại...")
                            val now = System.currentTimeMillis()
                            val batch = db.batch()
                            
                            batch.update(
                                activeDoc.reference,
                                mapOf(
                                    "status" to "paid_waiting_admin",
                                    "approvalStatus" to "pending_admin_review",
                                    "rejectReason" to "",
                                    "updatedAt" to now
                                )
                            )
                            batch.update(
                                db.collection("rooms").document(roomId),
                                mapOf(
                                    "featuredRequestStatus" to "paid_waiting_admin",
                                    "featuredRequestRejectReason" to "",
                                    "featuredRequestUpdatedAt" to now
                                )
                            )
                            batch.commit()
                                .addOnSuccessListener {
                                    progress.dismiss()
                                    MessageUtils.showSuccessDialog(
                                        this@MyPostsActivity,
                                        "Thành công",
                                        "Đã gửi lại yêu cầu duyệt nổi bật thành công!"
                                    ) { refreshList() }
                                }
                                .addOnFailureListener { e ->
                                    progress.dismiss()
                                    MessageUtils.showErrorDialog(
                                        this@MyPostsActivity,
                                        "Lỗi kết nối",
                                        "Không thể gửi yêu cầu duyệt lại: ${e.message}"
                                    )
                                }
                        }
                        
                        dialog.show()
                        dialog.window?.apply {
                            val width = (resources.displayMetrics.widthPixels * 0.92f).toInt()
                            setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
                        }
                    } else {
                        syncRoomFeaturedRequestStatus(db, roomId, status, activeDoc.id)
                        MessageUtils.showInfoDialog(
                            this,
                            "Đã có yêu cầu nổi bật",
                            "Bài đăng này đã thanh toán và đang chờ admin duyệt nổi bật."
                        ) { refreshList() }
                    }
                } else {
                    val roomRef = db.collection("rooms").document(roomId)
                    roomRef.get(Source.SERVER).addOnSuccessListener { roomSnap ->
                        val currentRoomStatus = roomSnap.getString("featuredRequestStatus") ?: ""
                        if (currentRoomStatus == "waiting_for_payment" || currentRoomStatus == "paid" || currentRoomStatus == "paid_waiting_admin") {
                            val staleRequestId = roomSnap.getString("featuredRequestId") ?: ""
                            val now = System.currentTimeMillis()
                            val batch = db.batch()
                            batch.update(
                                db.collection("rooms").document(roomId),
                                mapOf("featuredRequestStatus" to "cancelled", "featuredRequestUpdatedAt" to now)
                            )
                            if (staleRequestId.isNotBlank()) {
                                batch.update(
                                    db.collection("featured_upgrade_requests").document(staleRequestId),
                                    mapOf("status" to "cancelled", "approvalStatus" to "cancelled", "updatedAt" to now)
                                )
                            }
                            batch.commit()
                                .addOnSuccessListener { refreshList(); showFeaturedPackagesDialog(roomId, roomTitle) }
                                .addOnFailureListener { showFeaturedPackagesDialog(roomId, roomTitle) }
                        } else {
                            showFeaturedPackagesDialog(roomId, roomTitle)
                        }
                    }.addOnFailureListener {
                        showFeaturedPackagesDialog(roomId, roomTitle)
                    }
                }
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                MessageUtils.showErrorDialog(
                    this,
                    "Không thể kết nối máy chủ",
                    e.message ?: "Vui lòng kiểm tra kết nối mạng."
                )
            }
    }

    private fun showFeaturedPackagesDialog(roomId: String, roomTitle: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_featured_upgrade, null)
        val dialog = AlertDialog.Builder(this, R.style.RoundedDialogStyle)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<View>(R.id.layoutFeatured3).setOnClickListener {
            dialog.dismiss()
            showFeaturedPaymentQrDialog(roomId, roomTitle, featuredPackages[0])
        }
        dialogView.findViewById<View>(R.id.layoutFeatured7).setOnClickListener {
            dialog.dismiss()
            showFeaturedPaymentQrDialog(roomId, roomTitle, featuredPackages[1])
        }
        dialogView.findViewById<View>(R.id.layoutFeatured15).setOnClickListener {
            dialog.dismiss()
            showFeaturedPaymentQrDialog(roomId, roomTitle, featuredPackages[2])
        }
        dialogView.findViewById<View>(R.id.btnCancelFeatured).setOnClickListener {
            dialog.dismiss()
        }

        val edtCustomDays = dialogView.findViewById<EditText>(R.id.edtCustomDays)
        val tvCustomPrice = dialogView.findViewById<TextView>(R.id.tvCustomPrice)
        val btnBuyCustom = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBuyCustom)

        edtCustomDays.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val daysStr = s?.toString() ?: ""
                val days = daysStr.toIntOrNull() ?: 0
                if (days > 0) {
                    val price = days * 10_000L
                    val priceStr = "%,d đ".format(price).replace(",", ".")
                    // Gợi ý gói preset tiết kiệm hơn nếu có
                    val cheaperPreset = featuredPackages
                        .filter { it.days >= days && it.price < price }
                        .minByOrNull { it.price }
                    if (cheaperPreset != null) {
                        val saving = price - cheaperPreset.price
                        val savingStr = "%,d đ".format(saving).replace(",", ".")
                        tvCustomPrice.text = "$priceStr ⚠ Gói ${cheaperPreset.days} ngày chỉ ${"%,d đ".format(cheaperPreset.price.toLong()).replace(",", ".")} (tiết kiệm $savingStr)"
                    } else {
                        tvCustomPrice.text = priceStr
                    }
                    btnBuyCustom.isEnabled = true
                } else {
                    tvCustomPrice.text = "0 đ"
                    btnBuyCustom.isEnabled = false
                }
            }
        })

        btnBuyCustom.setOnClickListener {
            val daysStr = edtCustomDays.text.toString()
            val days = daysStr.toIntOrNull() ?: 0
            if (days > 0) {
                dialog.dismiss()
                val customPkg = FeaturedPackage("Nổi bật $days ngày (Tùy chọn)", "FT_CUSTOM", days, days * 10000)
                showFeaturedPaymentQrDialog(roomId, roomTitle, customPkg)
            }
        }

        dialog.show()
    }

    private fun showFeaturedPaymentQrDialog(roomId: String, roomTitle: String, pkg: FeaturedPackage) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val loadingDialog = MessageUtils.showLoadingDialog(
            this,
            "Đang kiểm tra yêu cầu nổi bật hiện tại..."
        )
        val activeStatuses = listOf("waiting_for_payment", "paid", "paid_waiting_admin")
        db.collection("featured_upgrade_requests")
            .whereEqualTo("uid", uid)
            .whereEqualTo("roomId", roomId)
            .get(Source.SERVER)
            .addOnSuccessListener { snap ->
                val activeDoc = snap.documents.firstOrNull { activeStatuses.contains(it.getString("status")) }
                if (activeDoc != null) {
                    val doc = activeDoc
                    val status = doc.getString("status") ?: "waiting_for_payment"
                    syncRoomFeaturedRequestStatus(db, roomId, status, doc.id)
                    loadingDialog.dismiss()
                    MessageUtils.showInfoDialog(
                        this,
                        "Đã có yêu cầu nổi bật",
                        if (status == "waiting_for_payment") {
                            "Bài đăng này đã có yêu cầu nổi bật đang chờ thanh toán."
                        } else {
                            "Bài đăng này đã thanh toán và đang chờ admin duyệt nổi bật."
                        }
                    ) { refreshList() }
                    return@addOnSuccessListener
                }
                loadingDialog.dismiss()
                showFeaturedPaymentWarningDialog(roomId, roomTitle, pkg)
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                MessageUtils.showErrorDialog(
                    this,
                    "Không thể kiểm tra giao dịch",
                    e.message ?: "Vui lòng kiểm tra kết nối rồi thử lại."
                )
            }
    }

    private fun showFeaturedPaymentWarningDialog(roomId: String, roomTitle: String, pkg: FeaturedPackage) {
        val builder = AlertDialog.Builder(this, R.style.RoundedDialogStyle)
            .setTitle("Lưu ý quan trọng trước khi thanh toán")
            .setMessage(
                "Khi quét mã QR, ứng dụng ngân hàng sẽ tự điền đầy đủ thông tin.\n\n" +
                "⚠ KHÔNG thay đổi số tiền hoặc nội dung chuyển khoản.\n\n" +
                "Nếu chuyển sai số tiền hoặc sai nội dung, hệ thống sẽ không xác nhận giao dịch " +
                "và bài đăng của bạn sẽ không được đẩy nổi bật. Số tiền đã chuyển sẽ không được hoàn lại."
            )
            .setPositiveButton("Đã hiểu, tiếp tục") { warningDialog, _ ->
                warningDialog.dismiss()
                proceedToCreateFeaturedRequest(roomId, roomTitle, pkg)
            }
            .setNegativeButton("Quay lại") { warningDialog, _ ->
                warningDialog.dismiss()
                showFeaturedPackagesDialog(roomId, roomTitle)
            }

        val dialog = builder.create()
        dialog.setOnDismissListener {
            paymentWarningDialog = null
        }
        paymentWarningDialog = dialog
        dialog.show()

        dialog.window?.apply {
            setBackgroundDrawableResource(R.drawable.bg_dialog_surface)
            val width = (resources.displayMetrics.widthPixels * 0.9f).toInt()
            setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun proceedToCreateFeaturedRequest(roomId: String, roomTitle: String, pkg: FeaturedPackage) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val loadingDialog = MessageUtils.showLoadingDialog(
            this,
            "Đang tải thông tin thanh toán..."
        )
        fetchPaymentConfig {
            loadingDialog.setMessage("Đang tạo yêu cầu đẩy nổi bật...")
            createFeaturedPaymentRequest(db, uid, roomId, roomTitle, pkg, loadingDialog)
        }
    }

    private fun createFeaturedPaymentRequest(
        db: FirebaseFirestore,
        uid: String,
        roomId: String,
        roomTitle: String,
        pkg: FeaturedPackage,
        loadingDialog: androidx.appcompat.app.AlertDialog
    ) {
        val docRef = db.collection("featured_upgrade_requests").document()
        val roomRef = db.collection("rooms").document(roomId)
        val requestId = docRef.id
        val requestCode = requestId.takeLast(8).uppercase(Locale.US)
        val addInfo = "NOIBAT ${pkg.code} REQ_$requestCode"
        val now = System.currentTimeMillis()
        val expiresAt = now + 30L * 60L * 1000L
        val request = hashMapOf(
            "uid" to uid,
            "roomId" to roomId,
            "roomTitle" to roomTitle,
            "requestId" to requestId,
            "days" to pkg.days,
            "amount" to pkg.price,
            "label" to pkg.label,
            "code" to pkg.code,
            "transferNote" to addInfo,
            "status" to "waiting_for_payment",
            "approvalStatus" to "pending_payment",
            "createdAt" to now,
            "updatedAt" to now,
            "expiresAt" to expiresAt
        )

        val batch = db.batch()
        batch.set(docRef, request)
        batch.update(
            roomRef,
            mapOf(
                "featuredRequestStatus" to "waiting_for_payment",
                "featuredRequestId" to requestId,
                "featuredRequestUpdatedAt" to now
            )
        )
        batch.commit()
            .addOnSuccessListener {
                loadingDialog.dismiss()
                showFeaturedPaymentQrDialogContent(db, docRef, roomId, pkg, addInfo, expiresAt)
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                MessageUtils.showErrorDialog(
                    this,
                    "Không thể tạo giao dịch",
                    e.message ?: "Vui lòng thử lại sau."
                )
            }
    }

    private fun showFeaturedPaymentQrDialogContent(
        db: FirebaseFirestore,
        docRef: com.google.firebase.firestore.DocumentReference,
        roomId: String,
        pkg: FeaturedPackage,
        addInfo: String,
        expiresAt: Long
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_payment_qr, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this, R.style.RoundedDialogStyle)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        paymentQrDialog = dialog

        dialogView.findViewById<TextView>(R.id.tvQrPackageName).text = "${pkg.label} — ${formatter.format(pkg.price.toLong())} đ"
        dialogView.findViewById<TextView>(R.id.tvQrBank).text = bankDisplay
        dialogView.findViewById<TextView>(R.id.tvQrAccountNo).text = accountNo
        dialogView.findViewById<TextView>(R.id.tvQrAccountName).text = accountName
        dialogView.findViewById<TextView>(R.id.tvQrTransferNote).text = addInfo
        val cleanBankId = bankId.trim().lowercase(java.util.Locale.US)
        val normalizedBankId = if (cleanBankId == "mb") "970422" else bankId.trim()
        val normalizedAccountNo = accountNo.trim()
        val qrUrl = "https://img.vietqr.io/image/$normalizedBankId-$normalizedAccountNo-compact2.png" +
            "?amount=${pkg.price}&addInfo=${android.net.Uri.encode(addInfo)}&accountName=${android.net.Uri.encode(accountName)}"
        Glide.with(this).load(qrUrl).placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_dialog_alert).into(dialogView.findViewById(R.id.imgVietQR))

        val btnConfirm = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConfirmPayment)
        btnConfirm.isEnabled = false
        btnConfirm.text = "Đang chờ xác nhận thanh toán..."
        btnConfirm.setBackgroundColor(android.graphics.Color.GRAY)

        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancelPayment)
        val tvCountdown = dialogView.findViewById<TextView>(R.id.tvQrCountdown)

        var paymentCompleted = false
        var latestStatus = "waiting_for_payment"
        var latestSyncedStatus = ""
        val handler = Handler(Looper.getMainLooper())
        var stopPolling = false

        fun applyStatus(status: String) {
            latestStatus = status
            if (status != latestSyncedStatus) {
                latestSyncedStatus = status
                syncRoomFeaturedRequestStatus(db, roomId, status, docRef.id)
            }
            when (status) {
                "paid", "paid_waiting_admin" -> {
                    btnConfirm.isEnabled = true
                    btnConfirm.text = "Đã thanh toán - chờ admin duyệt"
                    btnConfirm.setBackgroundColor(0xFF1976D2.toInt())
                    btnCancel.visibility = android.view.View.GONE
                }
                "approved" -> {
                    btnConfirm.isEnabled = true
                    btnConfirm.text = "Đã duyệt nổi bật"
                    btnConfirm.setBackgroundColor(0xFF2E7D32.toInt())
                    btnCancel.visibility = android.view.View.GONE
                }
                "waiting_for_payment" -> {
                    btnConfirm.isEnabled = false
                    btnConfirm.text = "Đang chờ xác nhận thanh toán..."
                    btnConfirm.setBackgroundColor(android.graphics.Color.GRAY)
                    btnCancel.visibility = android.view.View.VISIBLE
                }
                "expired" -> {
                    btnConfirm.isEnabled = false
                    btnConfirm.text = "Giao dịch đã hết hạn"
                    btnConfirm.setBackgroundColor(android.graphics.Color.GRAY)
                    btnCancel.visibility = android.view.View.GONE
                }
                "rejected" -> {
                    btnConfirm.isEnabled = false
                    btnConfirm.text = "Admin đã từ chối"
                    btnConfirm.setBackgroundColor(android.graphics.Color.GRAY)
                    btnCancel.visibility = android.view.View.VISIBLE
                    btnCancel.text = "Đóng"
                }
                "cancelled" -> {
                    btnConfirm.isEnabled = false
                    btnConfirm.text = "Đã hủy giao dịch"
                    btnConfirm.setBackgroundColor(android.graphics.Color.GRAY)
                    btnCancel.visibility = android.view.View.GONE
                }
            }
        }

        val remainMs = expiresAt - System.currentTimeMillis()
        val paymentCountdown: android.os.CountDownTimer? = if (remainMs > 0) {
            object : android.os.CountDownTimer(remainMs, 1000L) {
                override fun onTick(millisUntilFinished: Long) {
                    val mins = millisUntilFinished / 60000
                    val secs = (millisUntilFinished % 60000) / 1000
                    tvCountdown.text = "Hết hạn sau: %02d:%02d".format(mins, secs)
                }
                override fun onFinish() {
                    tvCountdown.text = "Giao dịch đã hết hạn"
                }
            }.also { it.start() }
        } else {
            tvCountdown.text = "Giao dịch đã hết hạn"
            null
        }

        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        fun startWatchingPayment() {
            listener = docRef.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                applyStatus(snapshot.getString("status") ?: return@addSnapshotListener)
            }
            paymentListener = listener
            dialog.show()
        }

        startWatchingPayment()

        btnCancel.setOnClickListener {
            if (latestStatus == "paid" || latestStatus == "paid_waiting_admin" || latestStatus == "approved") {
                dialog.dismiss()
                return@setOnClickListener
            }
            if (latestStatus == "cancelled" || latestStatus == "expired" || latestStatus == "rejected") {
                dialog.dismiss()
                MessageUtils.showInfoDialog(this, "Thông báo", "Yêu cầu thanh toán này đã dừng hoạt động.") {
                    refreshList()
                }
                return@setOnClickListener
            }

            MessageUtils.showConfirmDialog(
                context = this,
                title = "Xác nhận hủy giao dịch",
                message = "Nếu bạn ĐÃ thực hiện chuyển tiền thành công bằng app ngân hàng, vui lòng KHÔNG hủy giao dịch này để tránh thất thoát và được hỗ trợ tốt nhất.\n\nBạn có chắc chắn muốn hủy giao dịch?",
                positiveText = "Hủy giao dịch",
                negativeText = "Quay lại",
                onConfirm = {
                    btnCancel.isEnabled = false
                    btnCancel.text = "Đang hủy..."
                    val cancelAt = System.currentTimeMillis()
                    val batch = db.batch()
                    batch.update(
                        docRef,
                        mapOf(
                            "status" to "cancelled",
                            "approvalStatus" to "cancelled",
                            "updatedAt" to cancelAt
                        )
                    )
                    batch.update(
                        db.collection("rooms").document(roomId),
                        mapOf(
                            "featuredRequestStatus" to "cancelled",
                            "featuredRequestUpdatedAt" to cancelAt
                        )
                    )
                    batch.commit()
                        .addOnSuccessListener {
                            dialog.dismiss()
                            MessageUtils.showSuccessDialog(
                                this,
                                "Hủy giao dịch thành công",
                                "Yêu cầu đẩy nổi bật đã được hủy bỏ thành công."
                            ) {
                                refreshList()
                            }
                        }
                        .addOnFailureListener { e ->
                            btnCancel.isEnabled = true
                            btnCancel.text = "Hủy thanh toán"
                            MessageUtils.showErrorDialog(
                                this,
                                "Hủy giao dịch thất bại",
                                e.message ?: "Đã xảy ra lỗi không xác định khi hủy giao dịch. Vui lòng kiểm tra lại kết nối mạng hoặc thử lại sau."
                            )
                        }
                }
            )
        }

        btnConfirm.setOnClickListener {
            if (latestStatus != "paid" && latestStatus != "paid_waiting_admin" && latestStatus != "approved") return@setOnClickListener
            paymentCompleted = true
            dialog.dismiss()
            MessageUtils.showInfoDialog(
                this,
                "Đã ghi nhận thanh toán",
                "Yêu cầu đẩy nổi bật đã được gửi cho admin duyệt. Sau khi duyệt, bài sẽ hiển thị ở mục Phòng nổi bật."
            )
            refreshList()
        }

        dialog.setOnDismissListener {
            listener?.remove()
            paymentListener?.remove()
            paymentListener = null
            paymentCountdown?.cancel()
            if (!paymentCompleted) refreshList()
        }
    }

    private fun syncRoomFeaturedRequestStatus(
        db: FirebaseFirestore,
        roomId: String,
        status: String,
        requestId: String? = null
    ) {
        val normalizedStatus = if (status == "paid") "paid_waiting_admin" else status
        val update = hashMapOf<String, Any>(
            "featuredRequestStatus" to normalizedStatus,
            "featuredRequestUpdatedAt" to System.currentTimeMillis()
        )
        if (!requestId.isNullOrBlank()) update["featuredRequestId"] = requestId
        db.collection("rooms").document(roomId).update(update)
            .addOnFailureListener { e ->
                android.util.Log.w("MyPostsActivity", "Sync featured request status failed", e)
            }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        super.onDestroy()
        paymentListener?.remove()
        paymentListener = null
        paymentQrDialog?.dismiss()
        paymentQrDialog = null
        paymentWarningDialog?.dismiss()
        paymentWarningDialog = null
    }
}
