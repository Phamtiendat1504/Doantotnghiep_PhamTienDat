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

    private lateinit var layoutPosts: LinearLayout
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

    private val bankId = "mbbank"
    private val accountNo = "0889740127"
    private val accountName = "PHAM TIEN DAT"
    private val bankDisplay = "MB Bank"

    private val formatter: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN"))
        symbols.groupingSeparator = '.'
        DecimalFormat("#,###", symbols)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_posts)

        layoutPosts = findViewById(R.id.layoutPosts)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)
        btnBack = findViewById(R.id.btnBack)
        menuFilter = findViewById(R.id.menuFilter)
        edtSearchPost = findViewById(R.id.edtSearchPost)
        setupFilterMenu()
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        btnBack.setOnClickListener { finish() }

        viewModel = ViewModelProvider(this)[MyPostsViewModel::class.java]

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading && !swipeRefreshLayout.isRefreshing) View.VISIBLE else View.GONE
            if (!isLoading) swipeRefreshLayout.isRefreshing = false
            if (isLoading) { layoutPosts.removeAllViews(); tvEmpty.visibility = View.GONE }
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

        viewModel.renewResult.observe(this) { success ->
            if (success == true) {
                viewModel.resetRenewResult()
                MessageUtils.showSuccessDialog(
                    this, "Gia hạn thành công",
                    "Bài đăng đã được gia hạn thêm 2 tháng và hiển thị trở lại trên kết quả tìm kiếm."
                ) { refreshList() }
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
        val options = arrayOf("Tất cả", "Chờ duyệt", "Đã duyệt", "Bị từ chối", "Hết hạn")
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
        layoutPosts.removeAllViews()
        val filtered = if (keyword.isEmpty()) allPosts
        else allPosts.filter { (it.getString("title") ?: "").contains(keyword, ignoreCase = true) }

        if (filtered.isEmpty()) {
            tvEmpty.text = if (keyword.isEmpty()) "Chưa có bài đăng nào" else "Không tìm thấy bài đăng nào"
            tvEmpty.visibility = View.VISIBLE
            layoutPosts.addView(tvEmpty)
            return
        }
        tvEmpty.visibility = View.GONE
        for (doc in filtered) {
            layoutPosts.addView(createPostCard(
                docId = doc.id,
                title = doc.getString("title") ?: "Chưa có tiêu đề",
                price = doc.getLong("price") ?: 0,
                ward = doc.getString("ward") ?: "",
                district = doc.getString("district") ?: "",
                area = doc.getLong("area")?.toInt() ?: 0,
                status = doc.getString("status") ?: "pending",
                rejectReason = doc.getString("rejectReason") ?: "",
                imageUrls = (doc.get("imageUrls") as? List<*>)?.mapNotNull { it as? String } ?: listOf(),
                createdAt = doc.getLong("createdAt") ?: 0,
                isFeatured = doc.getBoolean("isFeatured") == true,
                featuredUntil = doc.getLong("featuredUntil") ?: 0,
                featuredRequestStatus = doc.getString("featuredRequestStatus") ?: ""
            ))
        }
    }

    private fun createPostCard(
        docId: String, title: String, price: Long, ward: String,
        district: String, area: Int, status: String, rejectReason: String,
        imageUrls: List<String>, createdAt: Long, isFeatured: Boolean,
        featuredUntil: Long, featuredRequestStatus: String
    ): CardView {
        val card = CardView(this)
        val cardParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        cardParams.bottomMargin = dpToPx(12)
        card.layoutParams = cardParams
        card.radius = dpToPx(14).toFloat()
        card.cardElevation = dpToPx(3).toFloat()
        card.setCardBackgroundColor(0xFFFFFFFF.toInt())

        val mainLayout = LinearLayout(this)
        mainLayout.orientation = LinearLayout.VERTICAL

        val topLayout = LinearLayout(this)
        topLayout.orientation = LinearLayout.HORIZONTAL
        topLayout.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(10))

        val imgView = ImageView(this)
        imgView.layoutParams = LinearLayout.LayoutParams(dpToPx(100), dpToPx(85))
        imgView.scaleType = ImageView.ScaleType.CENTER_CROP
        imgView.setBackgroundColor(0xFFE0E0E0.toInt())
        imgView.clipToOutline = true
        imgView.outlineProvider = object : android.view.ViewOutlineProvider() {
            override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, dpToPx(10).toFloat())
            }
        }
        if (imageUrls.isNotEmpty()) Glide.with(this).load(imageUrls[0]).centerCrop().into(imgView)
        topLayout.addView(imgView)

        val infoLayout = LinearLayout(this)
        infoLayout.orientation = LinearLayout.VERTICAL
        val infoParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        infoParams.marginStart = dpToPx(12)
        infoLayout.layoutParams = infoParams

        infoLayout.addView(TextView(this).apply {
            text = title; textSize = 15f; setTextColor(0xFF1A1A2E.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD); maxLines = 2
        })
        infoLayout.addView(TextView(this).apply {
            text = "$ward, $district"; textSize = 12f; setTextColor(0xFF999999.toInt()); setPadding(0, dpToPx(3), 0, 0)
        })
        if (area > 0) infoLayout.addView(TextView(this).apply {
            text = "${area}m²"; textSize = 12f; setTextColor(0xFF666666.toInt()); setPadding(0, dpToPx(2), 0, 0)
        })
        infoLayout.addView(TextView(this).apply {
            text = "${formatter.format(price)} đ/tháng"; textSize = 15f; setTextColor(0xFFD32F2F.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD); setPadding(0, dpToPx(4), 0, 0)
        })
        val now = System.currentTimeMillis()
        if (isFeatured && featuredUntil > now) {
            infoLayout.addView(TextView(this).apply {
                text = "Đang nổi bật đến ${SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN")).format(Date(featuredUntil))}"
                textSize = 11f
                setTextColor(0xFFE65100.toInt())
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(0, dpToPx(4), 0, 0)
            })
        } else if (featuredRequestStatus == "waiting_for_payment" || featuredRequestStatus == "paid" || featuredRequestStatus == "paid_waiting_admin") {
            infoLayout.addView(TextView(this).apply {
                text = if (featuredRequestStatus == "paid" || featuredRequestStatus == "paid_waiting_admin") "Đã thanh toán, chờ admin duyệt nổi bật" else "Có yêu cầu nổi bật đang chờ thanh toán"
                textSize = 11f
                setTextColor(0xFF1976D2.toInt())
                setPadding(0, dpToPx(4), 0, 0)
            })
        }
        topLayout.addView(infoLayout)
        mainLayout.addView(topLayout)

        mainLayout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1))
            setBackgroundColor(0xFFF0F0F0.toInt())
        })

        val bottomLayout = LinearLayout(this)
        bottomLayout.orientation = LinearLayout.HORIZONTAL
        bottomLayout.gravity = android.view.Gravity.CENTER_VERTICAL
        bottomLayout.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))

        val tvStatus = TextView(this)
        when (status) {
            "pending" -> { tvStatus.text = " Chờ duyệt"; tvStatus.setTextColor(0xFFE65100.toInt()); tvStatus.setBackgroundResource(R.drawable.bg_badge_pending) }
            "approved" -> { tvStatus.text = "Đã duyệt"; tvStatus.setTextColor(0xFFFFFFFF.toInt()); tvStatus.background = android.graphics.drawable.GradientDrawable().apply { setColor(0xFF2E7D32.toInt()); cornerRadius = dpToPx(8).toFloat() } }
            "rejected" -> { tvStatus.text = "Từ chối"; tvStatus.setTextColor(0xFFD32F2F.toInt()); tvStatus.setBackgroundResource(R.drawable.bg_badge_pending) }
            "expired" -> { tvStatus.text = " Hết hạn"; tvStatus.setTextColor(0xFFFFFFFF.toInt()); tvStatus.background = android.graphics.drawable.GradientDrawable().apply { setColor(0xFF757575.toInt()); cornerRadius = dpToPx(8).toFloat() } }
        }
        tvStatus.textSize = 12f; tvStatus.setTypeface(tvStatus.typeface, android.graphics.Typeface.BOLD)
        tvStatus.setPadding(dpToPx(12), dpToPx(4), dpToPx(12), dpToPx(4))
        bottomLayout.addView(tvStatus)

        val tvDate = TextView(this)
        tvDate.text = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN")).format(Date(createdAt))
        tvDate.textSize = 11f; tvDate.setTextColor(0xFF999999.toInt()); tvDate.setPadding(dpToPx(10), 0, 0, 0)
        tvDate.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        bottomLayout.addView(tvDate)

        val btnDelete = TextView(this).apply {
            text = "Xóa"; textSize = 13f; setTextColor(0xFFD32F2F.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD); setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
            setOnClickListener {
                androidx.appcompat.app.AlertDialog.Builder(this@MyPostsActivity)
                    .setTitle("Xóa bài đăng")
                    .setMessage("Bạn có chắc chắn muốn xóa bài đăng này? Mọi thông tin và hình ảnh sẽ bị xóa hoàn toàn khỏi hệ thống.")
                    .setPositiveButton("Xóa") { _, _ -> deletePost(docId) }
                    .setNegativeButton("Hủy", null).show()
            }
        }
        bottomLayout.addView(btnDelete)

        if (status == "approved") {
            val canBuyFeatured = !(isFeatured && featuredUntil > System.currentTimeMillis()) &&
                featuredRequestStatus != "waiting_for_payment" &&
                featuredRequestStatus != "paid" &&
                featuredRequestStatus != "paid_waiting_admin"
            if (canBuyFeatured) {
                val btnFeatured = TextView(this).apply {
                    text = "Đẩy nổi bật"; textSize = 13f; setTextColor(0xFFE65100.toInt())
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
                    setOnClickListener { showFeaturedPackagesDialog(docId, title) }
                }
                bottomLayout.addView(btnFeatured)
            }
        }

        if (status == "pending") {
            val btnEditPending = TextView(this).apply {
                text = "Chỉnh sửa"; textSize = 13f; setTextColor(0xFF1976D2.toInt())
                setTypeface(typeface, android.graphics.Typeface.BOLD); setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
                setOnClickListener {
                    startActivity(Intent(this@MyPostsActivity, EditPostActivity::class.java).apply { putExtra("roomId", docId) })
                }
            }
            bottomLayout.addView(btnEditPending)
        }
        mainLayout.addView(bottomLayout)

        if (status == "rejected" && rejectReason.isNotEmpty()) {
            val rejectLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dpToPx(12), 0, dpToPx(12), dpToPx(10)); gravity = android.view.Gravity.CENTER_VERTICAL }
            rejectLayout.addView(TextView(this).apply { text = "⚠️"; textSize = 13f })
            rejectLayout.addView(TextView(this).apply { text = "Lý do: $rejectReason"; textSize = 12f; setTextColor(0xFFD32F2F.toInt()); setPadding(dpToPx(6), 0, 0, 0) })
            mainLayout.addView(rejectLayout)

            val btnEdit = TextView(this).apply {
                text = "✏️ Sửa và đăng lại"; textSize = 14f; setTextColor(0xFFFFFFFF.toInt())
                setTypeface(typeface, android.graphics.Typeface.BOLD); gravity = android.view.Gravity.CENTER
                setPadding(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(10))
                background = android.graphics.drawable.GradientDrawable().apply { setColor(0xFF1976D2.toInt()); cornerRadius = dpToPx(10).toFloat() }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dpToPx(12), 0, dpToPx(12), dpToPx(12)) }
                setOnClickListener { startActivity(Intent(this@MyPostsActivity, EditPostActivity::class.java).apply { putExtra("roomId", docId) }) }
            }
            mainLayout.addView(btnEdit)
        }

        if (status == "expired") {
            mainLayout.addView(TextView(this).apply {
                text = "⚠ Bài đăng đã ẩn khỏi kết quả tìm kiếm sau 2 tháng. Bài sẽ tự động xóa sau 1 tháng nữa."
                textSize = 12f; setTextColor(0xFF795548.toInt()); setPadding(dpToPx(12), 0, dpToPx(12), dpToPx(8))
            })
            val btnRenew = TextView(this).apply {
                text = " Gia hạn bài đăng thêm 2 tháng"; textSize = 14f; setTextColor(0xFFFFFFFF.toInt())
                setTypeface(typeface, android.graphics.Typeface.BOLD); gravity = android.view.Gravity.CENTER
                setPadding(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(10))
                background = android.graphics.drawable.GradientDrawable().apply { setColor(0xFF1976D2.toInt()); cornerRadius = dpToPx(10).toFloat() }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dpToPx(12), 0, dpToPx(12), dpToPx(12)) }
                setOnClickListener { viewModel.renewPost(docId) }
            }
            mainLayout.addView(btnRenew)
        }

        card.addView(mainLayout)
        card.setOnClickListener {
            startActivity(Intent(this, MyPostDetailActivity::class.java).apply { putExtra("roomId", docId) })
        }
        return card
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
            4 -> viewModel.loadPosts("expired")
            else -> viewModel.loadPosts("all")
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
                    tvCustomPrice.text = "%,d đ".format(price).replace(",", ".")
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
                createFeaturedPaymentRequest(db, uid, roomId, roomTitle, pkg, loadingDialog)
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
            "expiresAt" to now + 30L * 60L * 1000L
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
                showFeaturedPaymentQrDialogContent(db, docRef, roomId, pkg, addInfo)
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
        addInfo: String
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_payment_qr, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this, R.style.RoundedDialogStyle)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<TextView>(R.id.tvQrPackageName).text = "${pkg.label} — ${formatter.format(pkg.price.toLong())} đ"
        dialogView.findViewById<TextView>(R.id.tvQrBank).text = bankDisplay
        dialogView.findViewById<TextView>(R.id.tvQrAccountNo).text = accountNo
        dialogView.findViewById<TextView>(R.id.tvQrAccountName).text = accountName
        dialogView.findViewById<TextView>(R.id.tvQrTransferNote).text = addInfo
        val qrUrl = "https://img.vietqr.io/image/$bankId-$accountNo-compact2.png" +
            "?amount=${pkg.price}&addInfo=${addInfo.replace(" ", "%20")}&accountName=${accountName.replace(" ", "%20")}"
        Glide.with(this).load(qrUrl).placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_dialog_alert).into(dialogView.findViewById(R.id.imgVietQR))

        val btnConfirm = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConfirmPayment)
        btnConfirm.isEnabled = false
        btnConfirm.text = "Đang chờ xác nhận thanh toán..."
        btnConfirm.setBackgroundColor(android.graphics.Color.GRAY)

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
                }
                "approved" -> {
                    btnConfirm.isEnabled = true
                    btnConfirm.text = "Đã duyệt nổi bật"
                    btnConfirm.setBackgroundColor(0xFF2E7D32.toInt())
                }
                "expired" -> {
                    btnConfirm.isEnabled = false
                    btnConfirm.text = "Giao dịch đã hết hạn"
                    btnConfirm.setBackgroundColor(android.graphics.Color.GRAY)
                }
                "rejected" -> {
                    btnConfirm.isEnabled = false
                    btnConfirm.text = "Admin đã từ chối"
                    btnConfirm.setBackgroundColor(android.graphics.Color.GRAY)
                }
            }
        }

        val pollRunnable = object : Runnable {
            override fun run() {
                if (stopPolling || !dialog.isShowing) return
                docRef.get(Source.SERVER).addOnSuccessListener {
                    it.getString("status")?.let(::applyStatus)
                }.addOnCompleteListener {
                    if (!stopPolling && dialog.isShowing) handler.postDelayed(this, 3000L)
                }
            }
        }

        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        fun startWatchingPayment() {
            listener = docRef.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                applyStatus(snapshot.getString("status") ?: return@addSnapshotListener)
            }
            handler.postDelayed(pollRunnable, 3000L)
            dialog.show()
        }

        startWatchingPayment()

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancelPayment)
            .setOnClickListener {
                if (latestStatus == "paid" || latestStatus == "paid_waiting_admin" || latestStatus == "approved") {
                    dialog.dismiss()
                    return@setOnClickListener
                }
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
                    .addOnCompleteListener { dialog.dismiss() }
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
            stopPolling = true
            handler.removeCallbacks(pollRunnable)
            listener?.remove()
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
}
