package com.example.doantotnghiep.View.Auth

import android.content.Intent
import android.location.Address
import android.os.Build
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import androidx.core.content.ContextCompat
import androidx.gridlayout.widget.GridLayout
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.ViewModel.RoomViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class RoomDetailActivity : AppCompatActivity() {

    private lateinit var viewPagerImages: ViewPager2
    private lateinit var tvImageCount: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvPrice: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvRoomNumber: TextView
    private lateinit var layoutRoomInfo: LinearLayout
    private lateinit var layoutAmenities: LinearLayout
    private lateinit var layoutOwnerInfo: LinearLayout
    private lateinit var gridAmenities: GridLayout
    private lateinit var btnBack: ImageView
    private lateinit var btnSave: MaterialButton
    private lateinit var btnBooking: MaterialButton
    private lateinit var btnOpenMaps: MaterialButton
    private lateinit var tvMapAddress: TextView
    private lateinit var tvBookedCount: TextView
    private lateinit var btnViewAllSlots: TextView
    private lateinit var layoutBookedSummary: LinearLayout
    private lateinit var cardBookedSlots: CardView
    private lateinit var layoutReviews: LinearLayout
    private lateinit var btnAddReview: MaterialButton
    private lateinit var tvReviewsTitle: TextView

    private lateinit var tvSpecArea: TextView
    private lateinit var tvSpecPeople: TextView
    private lateinit var tvSpecType: TextView
    private lateinit var btnReadMore: TextView

    private lateinit var viewModel: RoomViewModel
    private var currentUid: String? = null

    private val formatter: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN"))
        symbols.groupingSeparator = '.'
        DecimalFormat("#,###", symbols)
    }

    private var currentRoomId = ""
    private var currentRoomData: Map<String, Any> = emptyMap()
    private var googleMap: GoogleMap? = null
    private var pendingMapAddress: String? = null
    private var pendingMapLatLng: LatLng? = null
    private var lastResolvedLatLng: LatLng? = null
    private val mapFallbackLatLng = LatLng(21.0285, 105.8542)
    private val geocodeExecutor = Executors.newSingleThreadExecutor()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var btnBackContainer: android.view.View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Làm trong suốt Status Bar và xử lý Edge-to-Edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContentView(R.layout.activity_room_detail)
        
        // Khởi tạo container nút Back ngay lập tức để tránh crash khi xử lý Insets
        btnBackContainer = findViewById(R.id.btnBackContainer)

        viewModel = ViewModelProvider(this)[RoomViewModel::class.java]
        currentUid = FirebaseAuth.getInstance().currentUser?.uid

        initViews()
        setupGoogleMap()

        // Xử lý WindowInsets để thanh tiêu đề tràn lên Status Bar nhưng nút Back vẫn ở vùng an toàn
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(btnBackContainer) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        observeViewModel()

        btnBack.setOnClickListener { finish() }

        val userId = intent.getStringExtra("userId") ?: ""
        val roomId = intent.getStringExtra("roomId") ?: ""

        if (userId.isNotEmpty()) {
            viewModel.loadOwnerRooms(userId, roomId)
        } else if (roomId.isNotEmpty()) {
            tvRoomNumber.visibility = View.GONE
            viewModel.loadSingleRoom(roomId)
        }
    }

    private fun initViews() {
        viewPagerImages = findViewById(R.id.viewPagerImages)
        tvImageCount = findViewById(R.id.tvImageCount)
        tvTitle = findViewById(R.id.tvTitle)
        tvPrice = findViewById(R.id.tvPrice)
        tvAddress = findViewById(R.id.tvAddress)
        tvDescription = findViewById(R.id.tvDescription)
        tvRoomNumber = findViewById(R.id.tvRoomNumber)
        layoutRoomInfo = findViewById(R.id.layoutRoomInfo)
        layoutAmenities = findViewById(R.id.layoutAmenities)
        layoutOwnerInfo = findViewById(R.id.layoutOwnerInfo)
        gridAmenities = findViewById(R.id.gridAmenities)
        btnBack = findViewById(R.id.btnBack)
        btnSave = findViewById(R.id.btnSavePost)
        btnBooking = findViewById(R.id.btnBooking)
        btnOpenMaps = findViewById(R.id.btnOpenMaps)
        tvMapAddress = findViewById(R.id.tvMapAddress)
        tvBookedCount = findViewById(R.id.tvBookedCount)
        btnViewAllSlots = findViewById(R.id.btnViewAllSlots)
        layoutBookedSummary = findViewById(R.id.layoutBookedSummary)
        cardBookedSlots = findViewById(R.id.cardBookedSlots)
        layoutReviews = findViewById(R.id.layoutReviews)
        btnAddReview = findViewById(R.id.btnAddReview)
        tvReviewsTitle = findViewById(R.id.tvReviewsTitle)
        tvSpecArea = findViewById(R.id.tvSpecArea)
        tvSpecPeople = findViewById(R.id.tvSpecPeople)
        tvSpecType = findViewById(R.id.tvSpecType)
        btnReadMore = findViewById(R.id.btnReadMore)
        // btnBackContainer đã được init sớm trong onCreate
    }

    private fun setupGoogleMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapContainer) as? SupportMapFragment
            ?: return

        mapFragment.getMapAsync { map ->
            googleMap = map
            map.uiSettings.isZoomControlsEnabled = true
            map.uiSettings.isCompassEnabled = true
            map.uiSettings.isMapToolbarEnabled = true
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(mapFallbackLatLng, 11f))
            pendingMapAddress?.let { updateMapMarker(it, pendingMapLatLng) }
        }
    }

    private fun observeViewModel() {
        // Khi danh sách phòng thay đổi → hiển thị phòng được chọn
        viewModel.roomDocs.observe(this) { docs ->
            val index = viewModel.selectedRoomIndex.value ?: 0
            if (docs.isEmpty()) { finish(); return@observe }
            displayRoomDetail(index)
        }

        viewModel.selectedRoomIndex.observe(this) { index ->
            val docs = viewModel.roomDocs.value ?: return@observe
            if (docs.isNotEmpty()) displayRoomDetail(index)
        }

        // Trạng thái lưu bài
        viewModel.isSaved.observe(this) { saved ->
            btnSave.text = ""
            btnSave.setIconResource(R.drawable.ic_bookmark)
            if (saved) {
                btnSave.iconTint = ContextCompat.getColorStateList(this, R.color.primary)
            } else {
                btnSave.iconTint = ContextCompat.getColorStateList(this, R.color.gray_500)
            }
        }

        // Kết quả sau khi toggle lưu
        viewModel.saveResult.observe(this) { saved ->
            if (saved) {
                MessageUtils.showSuccessDialog(this, "Đã lưu", "Bài đăng đã được thêm vào danh sách yêu thích.")
            } else {
                MessageUtils.showSuccessDialog(this, "Đã bỏ lưu", "Bài đăng đã được xóa khỏi danh sách yêu thích.")
            }
        }

        // Trạng thái đặt lịch
        viewModel.hasActiveBooking.observe(this) { hasActive ->
            if (hasActive) {
                btnBooking.text = "✓ Đã đặt lịch"
                btnBooking.isEnabled = false
                btnBooking.setBackgroundColor(0xFF9E9E9E.toInt())
            } else {
                btnBooking.isEnabled = true
                btnBooking.setOnClickListener {
                    startActivity(buildBookingIntent(currentRoomId, currentRoomData))
                }
            }
        }

        // Thay vì ẩn nút theo role, giờ chỉ ẩn nút Lưu/Đặt lịch
        // khi user đang xem CHÍNH phòng của mình (tránh tự đặt lịch với mình)
        viewModel.userRole.observe(this) { _ ->
            // Không còn dùng role để ẩn nút — xử lý trong displayRoomDetail
        }

        // Slot đã đặt
        viewModel.bookedSlots.observe(this) { slots ->
            renderBookedSlots(slots)
        }

        // Lỗi
        viewModel.errorMessage.observe(this) { msg ->
            if (!msg.isNullOrEmpty()) {
                MessageUtils.showErrorDialog(this, "Lỗi", msg)
            }
        }
    }

    private fun displayRoomDetail(index: Int) {
        val docs = viewModel.roomDocs.value ?: return
        if (index < 0 || index >= docs.size) { finish(); return }

        val doc = docs[index]
        val data = doc.data ?: return
        currentRoomId = doc.id
        currentRoomData = data

        // Reset các nút để tránh trạng thái còn sót từ phòng xem trước
        btnSave.visibility = View.VISIBLE
        btnBooking.visibility = View.VISIBLE
        btnAddReview.visibility = View.VISIBLE
        btnBooking.isEnabled = true
        btnBooking.text = "Đặt lịch"
        btnBooking.setBackgroundColor(ContextCompat.getColor(this, R.color.primary))
        btnSave.text = ""
        btnSave.setIconResource(R.drawable.ic_bookmark)
        btnSave.iconTint = ContextCompat.getColorStateList(this, R.color.gray_500)

        tvRoomNumber.text = "Phòng ${String.format("%02d", index + 1)}"
        tvTitle.text = data["title"] as? String ?: "Chưa có tiêu đề"

        val price = (data["price"] as? Number)?.toLong() ?: 0L
        tvPrice.text = "${formatter.format(price)} đ/tháng"

        val address = data["address"] as? String ?: ""
        val ward = data["ward"] as? String ?: ""
        val district = data["district"] as? String ?: ""
        tvAddress.text = listOf(address, ward, district).filter { it.isNotBlank() }.joinToString(", ").ifBlank { "Chưa cập nhật địa chỉ" }

        val desc = (data["description"] as? String)?.takeIf { it.isNotEmpty() } ?: "Không có mô tả"
        tvDescription.text = desc

        // Xử lý nút "Xem thêm" cho mô tả
        tvDescription.post {
            if (tvDescription.lineCount > 4) {
                btnReadMore.visibility = View.VISIBLE
                btnReadMore.setOnClickListener {
                    if (tvDescription.maxLines == 4) {
                        tvDescription.maxLines = Int.MAX_VALUE
                        btnReadMore.text = "Thu gọn"
                    } else {
                        tvDescription.maxLines = 4
                        btnReadMore.text = "Xem thêm"
                    }
                }
            } else {
                btnReadMore.visibility = View.GONE
            }
        }

        val area = (data["area"] as? Number)?.toInt() ?: 0
        val peopleCount = (data["peopleCount"] as? Number)?.toInt() ?: 0
        val roomType = data["roomType"] as? String ?: "Phòng trọ"

        tvSpecArea.text = if (area > 0) "$area m²" else "-- m²"
        tvSpecPeople.text = if (peopleCount > 0) "$peopleCount người" else "-- người"
        tvSpecType.text = roomType

        val imageUrls = (data["imageUrls"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        setupImageSlider(imageUrls)
        setupRoomInfo(data)
        setupAmenities(data)
        setupOwnerInfo(data)
        loadReviews(data["userId"] as? String ?: "")
        val latitude = (data["latitude"] as? Number)?.toDouble()
        val longitude = (data["longitude"] as? Number)?.toDouble()
        setupMapSection(address, ward, district, latitude, longitude)

        val status = data["status"] as? String ?: "pending"
        val roomOwnerId = data["userId"] as? String ?: ""
        val isOwner = currentUid != null && currentUid == roomOwnerId

        if (status == "rented") {
            btnBooking.isEnabled = false
            btnBooking.text = "Phòng đã cho thuê"
            btnBooking.setBackgroundColor(0xFF9E9E9E.toInt())
            btnSave.visibility = View.GONE
            btnAddReview.visibility = View.GONE
            tvPrice.text = "ĐÃ CHO THUÊ"
            tvPrice.setTextColor(0xFFE53935.toInt())
        } else if (isOwner) {
            // Chủ phòng xem phòng của chính mình → ẩn Lưu và Đặt lịch
            btnSave.visibility = View.GONE
            btnBooking.visibility = View.GONE
            btnAddReview.visibility = View.GONE
        } else {
            // Người dùng khác → hiện nút và load thêm dữ liệu
            btnSave.visibility = View.VISIBLE
            btnBooking.visibility = View.VISIBLE
            btnAddReview.visibility = View.VISIBLE
            btnAddReview.setOnClickListener { showAddReviewDialog(roomOwnerId) }
            viewModel.loadBookedSlots(currentRoomId)
            setupActionButtons()
        }
    }

    private fun loadReviews(landlordId: String) {
        layoutReviews.removeAllViews()
        if (landlordId.isBlank()) {
            renderEmptyReviews()
            return
        }
        db.collection("reviews")
            .whereEqualTo("landlordId", landlordId)
            .get()
            .addOnSuccessListener { snap ->
                val reviews = snap.documents.mapNotNull { doc ->
                    val d = doc.data ?: return@mapNotNull null
                    if (d["status"] != "approved") return@mapNotNull null
                    ReviewRow(
                        userName = d["userName"] as? String ?: "Người dùng",
                        rating = (d["rating"] as? Number)?.toFloat() ?: 0f,
                        comment = d["comment"] as? String ?: "",
                        createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L,
                        roomTitle = d["roomTitle"] as? String ?: ""
                    )
                }.sortedByDescending { it.createdAt }.take(10)
                renderReviews(reviews)
            }
            .addOnFailureListener { renderEmptyReviews("Không thể tải đánh giá") }
    }

    private data class ReviewRow(
        val userName: String,
        val rating: Float,
        val comment: String,
        val createdAt: Long,
        val roomTitle: String
    )

    private fun renderReviews(reviews: List<ReviewRow>) {
        layoutReviews.removeAllViews()
        if (reviews.isEmpty()) {
            renderEmptyReviews()
            return
        }
        val avg = reviews.map { it.rating }.average()
        tvReviewsTitle.text = "Đánh giá chủ trọ (${String.format(Locale("vi", "VN"), "%.1f", avg)}★)"
        reviews.forEach { review ->
            layoutReviews.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dpToPx(10), 0, dpToPx(10))
                addView(TextView(this@RoomDetailActivity).apply {
                    text = "${review.userName} • ${"★".repeat(review.rating.toInt().coerceIn(1, 5))}"
                    textSize = 14f
                    setTextColor(0xFF111827.toInt())
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                })
                addView(TextView(this@RoomDetailActivity).apply {
                    text = review.comment.ifBlank { "Không có bình luận" }
                    textSize = 13f
                    setTextColor(0xFF374151.toInt())
                    setPadding(0, dpToPx(4), 0, 0)
                })
                val dateText = if (review.createdAt > 0) {
                    SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN")).format(Date(review.createdAt))
                } else ""
                addView(TextView(this@RoomDetailActivity).apply {
                    text = listOf(review.roomTitle, dateText).filter { it.isNotBlank() }.joinToString(" • ")
                    textSize = 11f
                    setTextColor(0xFF9CA3AF.toInt())
                    setPadding(0, dpToPx(3), 0, 0)
                })
            })
        }
    }

    private fun renderEmptyReviews(message: String = "Chưa có đánh giá nào cho chủ trọ này.") {
        tvReviewsTitle.text = "Đánh giá chủ trọ"
        layoutReviews.removeAllViews()
        layoutReviews.addView(TextView(this).apply {
            text = message
            textSize = 13f
            setTextColor(0xFF6B7280.toInt())
            setPadding(0, dpToPx(4), 0, 0)
        })
    }

    private fun showAddReviewDialog(landlordId: String) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid == null) {
            promptLogin()
            return
        }
        if (landlordId.isBlank() || landlordId == currentUid) return
        
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_review)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val ratingBar = dialog.findViewById<RatingBar>(R.id.ratingBar)
        val edtComment = dialog.findViewById<EditText>(R.id.edtComment)
        val btnCancel = dialog.findViewById<View>(R.id.btnCancel)
        val btnSubmit = dialog.findViewById<View>(R.id.btnSubmit)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSubmit.setOnClickListener {
            val comment = edtComment.text.toString().trim()
            if (comment.length < 5) {
                edtComment.error = "Nhận xét cần ít nhất 5 ký tự"
                return@setOnClickListener
            }
            val rating = ratingBar.rating.toInt().coerceIn(1, 5)
            if (rating < 1) {
                Toast.makeText(this@RoomDetailActivity, "Vui lòng chọn số sao", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            submitReview(landlordId, rating, comment) {
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun submitReview(landlordId: String, rating: Int, comment: String, onDone: () -> Unit) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userName = FirebaseAuth.getInstance().currentUser?.displayName
            ?: FirebaseAuth.getInstance().currentUser?.email
            ?: "Người dùng"
        val now = System.currentTimeMillis()
        val data = hashMapOf(
            "userId" to currentUid,
            "userName" to userName,
            "landlordId" to landlordId,
            "roomId" to currentRoomId,
            "roomTitle" to (currentRoomData["title"] as? String ?: ""),
            "rating" to rating,
            "comment" to comment,
            "status" to "approved",
            "createdAt" to now,
            "updatedAt" to now
        )
        db.collection("reviews")
            .whereEqualTo("userId", currentUid)
            .whereEqualTo("landlordId", landlordId)
            .limit(1)
            .get(Source.SERVER)
            .addOnSuccessListener {
                if (!it.isEmpty) {
                    MessageUtils.showInfoDialog(
                        this,
                        "Bạn đã đánh giá",
                        "Bạn đã gửi đánh giá cho chủ trọ này rồi."
                    )
                    onDone()
                    return@addOnSuccessListener
                }
                db.collection("reviews").add(data)
                    .addOnSuccessListener {
                        MessageUtils.showSuccessDialog(this, "Đã gửi đánh giá", "Cảm ơn bạn đã đánh giá chủ trọ.")
                        loadReviews(landlordId)
                        onDone()
                    }
                    .addOnFailureListener { e ->
                        MessageUtils.showErrorDialog(this, "Lỗi", e.message ?: "Không thể gửi đánh giá")
                    }
            }
            .addOnFailureListener { e ->
                MessageUtils.showErrorDialog(this, "Lỗi", e.message ?: "Không thể kiểm tra đánh giá hiện tại")
            }
    }

    private fun setupActionButtons() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        currentUid = userId
        if (userId.isNullOrBlank()) {
            btnSave.visibility = View.VISIBLE
            btnBooking.visibility = View.VISIBLE
            btnSave.setIconResource(R.drawable.ic_bookmark)
            btnSave.setIconTintResource(R.color.primary)
            btnSave.setOnClickListener { promptLogin() }
            btnBooking.setOnClickListener { promptLogin() }
            return
        }

        // Load role, trạng thái lưu, trạng thái đặt lịch qua ViewModel
        viewModel.loadUserRole(userId)
        viewModel.checkSavedStatus(userId, currentRoomId)
        viewModel.checkActiveBooking(userId, currentRoomId)

        btnSave.setOnClickListener {
            viewModel.toggleSavePost(userId, currentRoomId, currentRoomData)
        }
    }

    // ─── GOOGLE MAPS ───────────────────────────────────────────────────────────
    private fun setupMapSection(
        address: String,
        ward: String,
        district: String,
        latitude: Double?,
        longitude: Double?
    ) {
        val fullAddress = buildString {
            if (address.isNotEmpty()) append("$address, ")
            if (ward.isNotEmpty()) append("$ward, ")
            if (district.isNotEmpty()) append("$district, ")
            append("Ha Noi")
        }

        tvMapAddress.text = fullAddress
        pendingMapAddress = fullAddress
        pendingMapLatLng = if (latitude != null && longitude != null) {
            LatLng(latitude, longitude)
        } else {
            null
        }
        updateMapMarker(fullAddress, pendingMapLatLng)

        btnOpenMaps.setOnClickListener {
            openGoogleMaps(fullAddress)
        }
    }

    private fun updateMapMarker(fullAddress: String, directLatLng: LatLng? = null) {
        val map = googleMap ?: return
        if (directLatLng != null) {
            map.clear()
            map.addMarker(
                MarkerOptions()
                    .position(directLatLng)
                    .title(fullAddress)
            )
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(directLatLng, 15f))
            lastResolvedLatLng = directLatLng
            return
        }

        geocodeExecutor.execute {
            val resolvedLatLng = runCatching { geocodeAddress(fullAddress) }.getOrNull()

            val target = resolvedLatLng ?: mapFallbackLatLng
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                map.clear()
                map.addMarker(
                    MarkerOptions()
                        .position(target)
                        .title(if (resolvedLatLng != null) fullAddress else "Vi tri gan dung")
                )
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(target, if (resolvedLatLng != null) 15f else 11f))
                lastResolvedLatLng = resolvedLatLng
            }
        }
    }

    private fun geocodeAddress(fullAddress: String): LatLng? {
        val geocoder = Geocoder(this, Locale("vi", "VN"))
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            var firstAddress: Address? = null
            val latch = CountDownLatch(1)
            geocoder.getFromLocationName(fullAddress, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {
                    firstAddress = addresses.firstOrNull()
                    latch.countDown()
                }

                override fun onError(errorMessage: String?) {
                    latch.countDown()
                }
            })
            latch.await(4, TimeUnit.SECONDS)
            firstAddress?.let { LatLng(it.latitude, it.longitude) }
        } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocationName(fullAddress, 1)
                ?.firstOrNull()
                ?.let { LatLng(it.latitude, it.longitude) }
        }
    }

    private fun openGoogleMaps(address: String) {
        val resolvedLatLng = lastResolvedLatLng ?: pendingMapLatLng
        val geoUri = if (resolvedLatLng != null) {
            Uri.parse("geo:${resolvedLatLng.latitude},${resolvedLatLng.longitude}?q=${Uri.encode(address)}")
        } else {
            Uri.parse("geo:0,0?q=${Uri.encode(address)}")
        }
        val mapsIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (mapsIntent.resolveActivity(packageManager) != null) {
            startActivity(mapsIntent)
        } else {
            val query = if (resolvedLatLng != null) {
                "${resolvedLatLng.latitude},${resolvedLatLng.longitude}"
            } else {
                Uri.encode(address)
            }
            val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$query")
            startActivity(Intent(Intent.ACTION_VIEW, browserUri))
        }
    }

    override fun onResume() {
        super.onResume()
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        currentUid = userId
        if (currentRoomId.isNotEmpty() && !userId.isNullOrBlank()) {
            viewModel.checkActiveBooking(userId, currentRoomId)
        }
    }


    private fun promptLogin() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Yêu cầu đăng nhập")
            .setMessage("Bạn cần đăng nhập để sử dụng tính năng này.")
            .setPositiveButton("Đăng nhập") { _, _ ->
                startActivity(Intent(this, LoginActivity::class.java))
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun renderBookedSlots(slots: List<Map<String, Any>>) {
        if (slots.isEmpty()) {
            cardBookedSlots.visibility = View.GONE
            return
        }

        cardBookedSlots.visibility = View.VISIBLE
        tvBookedCount.text = "Lịch bận (${slots.size})"
        layoutBookedSummary.removeAllViews()

        // Hiển thị tối đa 3 slot đầu tiên dạng chip ngang
        val displayLimit = 3
        slots.take(displayLimit).forEach { slot ->
            val date = slot["dateDisplay"] as? String ?: slot["date"] as? String ?: ""
            val time = slot["time"] as? String ?: ""
            
            val chipView = TextView(this).apply {
                text = "$time $date"
                textSize = 12f
                setTextColor(0xFFE65100.toInt())
                setBackgroundResource(R.drawable.bg_slot_chip)
                setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = dpToPx(8)
                }
            }
            layoutBookedSummary.addView(chipView)
        }

        if (slots.size > displayLimit) {
            val moreView = TextView(this).apply {
                text = "+${slots.size - displayLimit}"
                textSize = 12f
                setTextColor(0xFF999999.toInt())
                setPadding(dpToPx(4), dpToPx(6), dpToPx(4), dpToPx(6))
            }
            layoutBookedSummary.addView(moreView)
        }

        btnViewAllSlots.setOnClickListener {
            showAllBookedSlots(slots)
        }
    }

    private fun showAllBookedSlots(slots: List<Map<String, Any>>) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_list, null)
        
        val tvTitle = view.findViewById<TextView>(R.id.tvSheetTitle)
        val layoutList = view.findViewById<LinearLayout>(R.id.layoutSheetList)
        
        tvTitle.text = "Danh sách lịch đã hẹn"
        
        slots.forEach { slot ->
            val date = slot["dateDisplay"] as? String ?: slot["date"] as? String ?: ""
            val time = slot["time"] as? String ?: ""
            
            val itemView = layoutInflater.inflate(R.layout.item_booked_slot_row, layoutList, false)
            itemView.findViewById<TextView>(R.id.tvSlotTime).text = time
            itemView.findViewById<TextView>(R.id.tvSlotDate).text = date
            
            layoutList.addView(itemView)
        }
        
        dialog.setContentView(view)
        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        geocodeExecutor.shutdownNow()
    }

    private fun setupImageSlider(imageUrls: List<String>) {
        val images = if (imageUrls.isEmpty()) listOf("") else imageUrls
        viewPagerImages.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val img = ImageView(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setBackgroundColor(0xFFE0E0E0.toInt())
                }
                return object : RecyclerView.ViewHolder(img) {}
            }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val imgView = holder.itemView as ImageView
                if (images[position].isNotEmpty()) {
                    Glide.with(this@RoomDetailActivity).load(images[position]).centerCrop().into(imgView)
                    imgView.setOnClickListener {
                        startActivity(Intent(this@RoomDetailActivity, ImageViewerActivity::class.java).apply {
                            putExtra("imageUrl", images[position])
                        })
                    }
                }
            }
            override fun getItemCount() = images.size
        }
        tvImageCount.text = "1/${images.size}"
        tvImageCount.background = android.graphics.drawable.GradientDrawable().apply {
            setColor(0x99000000.toInt())
            cornerRadius = dpToPx(10).toFloat()
        }
        viewPagerImages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                tvImageCount.text = "${position + 1}/${images.size}"
            }
        })
    }

    private fun setupRoomInfo(data: Map<String, Any>) {
        layoutRoomInfo.removeAllViews()
        
        val deposit = (data["depositAmount"] as? Number)?.toLong() ?: 0L
        val depositMonths = (data["depositMonths"] as? Number)?.toInt() ?: 0
        val electricPrice = (data["electricPrice"] as? Number)?.toLong() ?: 0L
        val waterPrice = (data["waterPrice"] as? Number)?.toLong() ?: 0L
        val wifiPrice = (data["wifiPrice"] as? Number)?.toLong() ?: 0L

        // Nhóm Chi phí (đặc biệt quan trọng nên làm nổi bật)
        if (deposit > 0 || electricPrice > 0 || waterPrice > 0) {
            val tvHeader = TextView(this).apply {
                text = "Chi phí dự kiến"
                textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(0xFF1A1A1A.toInt())
                setPadding(0, dpToPx(16), 0, dpToPx(8))
            }
            layoutRoomInfo.addView(tvHeader)
            
            if (deposit > 0) addInfoRow("Tiền đặt cọc", "${formatter.format(deposit)} đ")
            if (depositMonths > 0) addInfoRow("Đặt cọc trước", "$depositMonths tháng")
            if (electricPrice > 0) addInfoRow("Tiền điện", "${formatter.format(electricPrice)} đ/kWh")
            if (waterPrice > 0) addInfoRow("Tiền nước", "${formatter.format(waterPrice)} đ/m³")
            if (wifiPrice > 0) addInfoRow("Tiền wifi", "${formatter.format(wifiPrice)} đ/tháng")
        }

        // Nhóm Quy định & Khác
        val kitchen = data["kitchen"] as? String ?: ""
        val bathroom = data["bathroom"] as? String ?: ""
        val genderPrefer = data["genderPrefer"] as? String ?: ""
        val curfew = data["curfew"] as? String ?: ""
        val curfewTime = data["curfewTime"] as? String ?: ""
        val pet = data["pet"] as? String ?: ""

        // Kiểm tra xem có bất kỳ thông tin quy định nào không
        if (kitchen.isNotEmpty() || bathroom.isNotEmpty() || genderPrefer.isNotEmpty() || curfew.isNotEmpty() || pet.isNotEmpty()) {
            val tvHeader = TextView(this).apply {
                text = "Quy định & Cơ sở vật chất"
                textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(0xFF1A1A1A.toInt())
                setPadding(0, dpToPx(16), 0, dpToPx(8))
            }
            layoutRoomInfo.addView(tvHeader)

            if (genderPrefer.isNotEmpty()) addInfoRow("Ưu tiên giới tính", genderPrefer)
            if (kitchen.isNotEmpty()) addInfoRow("Phòng bếp", kitchen)
            if (bathroom.isNotEmpty()) addInfoRow("Phòng vệ sinh", bathroom)
            
            if (curfew.isNotEmpty()) {
                val text = if (curfew == "Tùy chọn" && curfewTime.isNotEmpty()) "Đóng cửa lúc $curfewTime" else curfew
                addInfoRow("Giờ giấc", text)
            }

            if (pet.isNotEmpty()) {
                val petName = data["petName"] as? String ?: ""
                val petCount = (data["petCount"] as? Number)?.toInt() ?: 0
                val text = if (pet == "Cho nuôi") {
                    val details = mutableListOf<String>()
                    if (petName.isNotEmpty()) details.add(petName)
                    if (petCount > 0) details.add("Số lượng: $petCount")
                    if (details.isNotEmpty()) "Cho nuôi (${details.joinToString(" - ")})" else "Cho nuôi"
                } else {
                    pet
                }
                addInfoRow("Thú cưng", text)
            }
        }
    }

    private fun addInfoRow(label: String, value: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dpToPx(8), 0, dpToPx(8))
        }
        row.addView(TextView(this).apply {
            text = label
            textSize = 14f
            setTextColor(0xFF666666.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        row.addView(TextView(this).apply {
            text = value
            textSize = 14f
            setTextColor(0xFF1A1A1A.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.END
        })
        layoutRoomInfo.addView(row)
    }

    private fun setupAmenities(data: Map<String, Any>) {
        gridAmenities.removeAllViews()
        layoutAmenities.removeAllViews()

        val amenitiesNames = mutableListOf<String>()
        if (data["hasWifi"] == true) amenitiesNames.add("Wifi miễn phí")
        if (data["hasAirCon"] == true) amenitiesNames.add("Điều hòa")
        if (data["hasWaterHeater"] == true) amenitiesNames.add("Bình nóng lạnh")
        if (data["hasWasher"] == true) amenitiesNames.add("Máy giặt")
        if (data["hasDryingArea"] == true) amenitiesNames.add("Sân phơi đồ")
        if (data["hasWardrobe"] == true) amenitiesNames.add("Tủ quần áo")
        if (data["hasBed"] == true) amenitiesNames.add("Giường ngủ")

        if (amenitiesNames.isEmpty() && data["hasMotorbike"] != true && data["hasEBike"] != true && data["hasBicycle"] != true) {
            val tvEmpty = TextView(this).apply {
                text = "Không có thông tin tiện ích"
                textSize = 14f
                setTextColor(0xFF999999.toInt())
            }
            gridAmenities.addView(tvEmpty)
            return
        }

        // Hiển thị tiện ích dạng text labels (Không dùng Icon theo yêu cầu)
        amenitiesNames.forEach { name ->
            val tvAmenity = TextView(this).apply {
                text = name
                textSize = 13f
                setTextColor(0xFF444444.toInt())
                setBackgroundResource(R.drawable.bg_spec_item)
                setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
                
                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(0, 0, dpToPx(8), dpToPx(8))
                }
                layoutParams = params
            }
            gridAmenities.addView(tvAmenity)
        }

        // Các thông tin phí gửi xe
        val motorbikeFee = (data["motorbikeFee"] as? Number)?.toLong() ?: 0L
        val eBikeFee = (data["eBikeFee"] as? Number)?.toLong() ?: 0L
        val bicycleFee = (data["bicycleFee"] as? Number)?.toLong() ?: 0L

        if (data["hasMotorbike"] == true) {
            val feeText = if (motorbikeFee > 0) "${formatter.format(motorbikeFee)} đ/xe" else "Miễn phí"
            addAmenityFeeRow("Gửi xe máy", feeText)
        }
        if (data["hasEBike"] == true) {
            val feeText = if (eBikeFee > 0) "${formatter.format(eBikeFee)} đ/xe" else "Miễn phí"
            addAmenityFeeRow("Gửi xe đạp điện", feeText)
        }
        if (data["hasBicycle"] == true) {
            val feeText = if (bicycleFee > 0) "${formatter.format(bicycleFee)} đ/xe" else "Miễn phí"
            addAmenityFeeRow("Gửi xe đạp", feeText)
        }
    }

    private fun addAmenityFeeRow(label: String, fee: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dpToPx(5), 0, dpToPx(5))
        }
        row.addView(TextView(this).apply {
            text = label
            textSize = 13f
            setTextColor(0xFF999999.toInt())
            layoutParams = LinearLayout.LayoutParams(dpToPx(130), LinearLayout.LayoutParams.WRAP_CONTENT)
        })
        row.addView(TextView(this).apply {
            text = fee
            textSize = 13f
            setTextColor(0xFF1A1A2E.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        layoutAmenities.addView(row)
        layoutAmenities.addView(android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1))
            setBackgroundColor(0xFFF5F5F5.toInt())
        })
    }

    private fun setupOwnerInfo(data: Map<String, Any>) {
        layoutOwnerInfo.removeAllViews()
        val name = data["ownerName"] as? String ?: "Chủ trọ"
        val phone = data["ownerPhone"] as? String ?: ""
        val gender = data["ownerGender"] as? String ?: ""
        val landlordId = data["userId"] as? String ?: ""
        
        // Host Profile Style
        val hostView = layoutInflater.inflate(R.layout.item_host_info, layoutOwnerInfo, false)
        val tvHostName = hostView.findViewById<TextView>(R.id.tvHostName)
        val tvHostStatus = hostView.findViewById<TextView>(R.id.tvHostStatus)
        val btnCall = hostView.findViewById<MaterialButton>(R.id.btnCallHost)
        val btnChat = hostView.findViewById<MaterialButton>(R.id.btnChatHost)
        val ivAvatar = hostView.findViewById<ImageView>(R.id.ivHostAvatar)

        tvHostName.text = name
        tvHostStatus.text = if (gender.isNotEmpty()) "Chủ nhà • $gender" else "Chủ nhà đã xác minh"
        
        // Retrieve owner avatar (ưu tiên lấy trực tiếp từ data bài đăng)
        var savedAvatarUrl = data["ownerAvatarUrl"] as? String ?: ""
        if (savedAvatarUrl.isNotEmpty()) {
            ivAvatar.setPadding(0, 0, 0, 0)
            ivAvatar.imageTintList = null
            Glide.with(this).load(savedAvatarUrl).circleCrop().into(ivAvatar)
        } else if (landlordId.isNotEmpty()) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(landlordId).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists() && doc.getString("avatarUrl")?.isNotEmpty() == true) {
                        savedAvatarUrl = doc.getString("avatarUrl").orEmpty()
                        ivAvatar.setPadding(0, 0, 0, 0)
                        ivAvatar.imageTintList = null
                        Glide.with(this).load(savedAvatarUrl).circleCrop().into(ivAvatar)
                    }
                }
        }
        
        btnCall.setOnClickListener {
            if (phone.isNotEmpty()) {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
            }
        }
        
        btnChat.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            currentUid = userId
            if (userId.isNullOrBlank()) {
                startActivity(Intent(this, LoginActivity::class.java))
                return@setOnClickListener
            }
            if (userId == landlordId) return@setOnClickListener  // không tự nhắn mình
            startActivity(
                Intent(this, ChatActivity::class.java).apply {
                    putExtra(ChatActivity.EXTRA_OTHER_UID,    landlordId)
                    putExtra(ChatActivity.EXTRA_OTHER_NAME,   name)
                    putExtra(ChatActivity.EXTRA_OTHER_AVATAR, savedAvatarUrl)
                }
            )
        }

        // Hiện BottomSheet thông tin chủ trọ khi click vào
        hostView.setOnClickListener {
            showHostProfileBottomSheet(landlordId, data)
        }

        layoutOwnerInfo.addView(hostView)
    }

    private fun showHostProfileBottomSheet(landlordId: String, roomData: Map<String, Any>) {
        if (landlordId.isEmpty()) return
        
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_host_profile_bottom_sheet, null)
        
        val ivSheetAvatar = view.findViewById<ImageView>(R.id.ivSheetAvatar)
        val tvSheetName = view.findViewById<TextView>(R.id.tvSheetName)
        val tvSheetStatus = view.findViewById<TextView>(R.id.tvSheetStatus)
        val tvSheetPhone = view.findViewById<TextView>(R.id.tvSheetPhone)
        val tvSheetEmail = view.findViewById<TextView>(R.id.tvSheetEmail)
        val tvSheetJoinDate = view.findViewById<TextView>(R.id.tvSheetJoinDate)
        val tvSheetVerified = view.findViewById<TextView>(R.id.tvSheetVerified)
        val btnSheetChat = view.findViewById<MaterialButton>(R.id.btnSheetChat)
        val btnSheetCall = view.findViewById<MaterialButton>(R.id.btnSheetCall)
        
        // Gán dữ liệu cơ bản từ phòng
        val name = roomData["ownerName"] as? String ?: "Chủ trọ"
        val phone = roomData["ownerPhone"] as? String ?: ""
        val gender = roomData["ownerGender"] as? String ?: ""
        
        tvSheetName.text = name
        tvSheetStatus.text = if (gender.isNotEmpty()) "Chủ nhà • $gender" else "Chủ nhà"
        tvSheetPhone.text = if (phone.isNotEmpty()) phone else "Đang cập nhật"
        
        // Gán liền ảnh đại diện nều có
        var savedSheetAvatarUrl = roomData["ownerAvatarUrl"] as? String ?: ""
        if (savedSheetAvatarUrl.isNotEmpty()) {
            ivSheetAvatar.setPadding(0, 0, 0, 0)
            ivSheetAvatar.imageTintList = null
            Glide.with(this).load(savedSheetAvatarUrl).circleCrop().into(ivSheetAvatar)
        }

        // Gọi Firebase lấy dữ liệu chi tiết còn thiếu (email, ngày tham gia)
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(landlordId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    // Nếu lúc trước chưa có hình, fallback
                    if (savedSheetAvatarUrl.isEmpty()) {
                        val avatarUrl = doc.getString("avatarUrl") ?: ""
                        if (avatarUrl.isNotEmpty()) {
                            savedSheetAvatarUrl = avatarUrl
                            ivSheetAvatar.setPadding(0, 0, 0, 0)
                            ivSheetAvatar.imageTintList = null
                            Glide.with(this).load(avatarUrl).circleCrop().into(ivSheetAvatar)
                        }
                    }
                    
                    val email = doc.getString("email") ?: "Đang cập nhật"

                    tvSheetEmail.text = email
                    
                    val isVerified = doc.getBoolean("isVerified") ?: false
                    tvSheetVerified.text = if (isVerified) "Đã xác nhận" else "Chưa xác minh"
                    if (!isVerified) tvSheetVerified.setTextColor(0xFF9E9E9E.toInt())
                    
                    val joinedAt = doc.getLong("createdAt") ?: 0L
                    if (joinedAt > 0) {
                        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))
                        tvSheetJoinDate.text = "Tham gia từ: ${sdf.format(java.util.Date(joinedAt))}"
                    }
                }
            }
            
        btnSheetCall.setOnClickListener {
            if (phone.isNotEmpty()) startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
        }
        
        btnSheetChat.setOnClickListener {
            dialog.dismiss()
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            currentUid = userId
            if (userId.isNullOrBlank()) {
                startActivity(Intent(this, LoginActivity::class.java))
                return@setOnClickListener
            }
            if (userId == landlordId) return@setOnClickListener
            startActivity(
                Intent(this, ChatActivity::class.java).apply {
                    putExtra(ChatActivity.EXTRA_OTHER_UID,    landlordId)
                    putExtra(ChatActivity.EXTRA_OTHER_NAME,   name)
                    putExtra(ChatActivity.EXTRA_OTHER_AVATAR, savedSheetAvatarUrl)
                }
            )
        }
        
        dialog.setContentView(view)
        dialog.show()
    }

    private fun addOwnerRow(label: String, value: String, isPhone: Boolean = false) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dpToPx(6), 0, dpToPx(6))
        }
        row.addView(TextView(this).apply {
            text = label
            textSize = 13f
            setTextColor(0xFF999999.toInt())
            layoutParams = LinearLayout.LayoutParams(dpToPx(80), LinearLayout.LayoutParams.WRAP_CONTENT)
        })
        row.addView(TextView(this).apply {
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            if (isPhone) {
                text = "SĐT: $value"
                setTextColor(0xFF1565C0.toInt())
                paintFlags = paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    startActivity(Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:$value")))
                }
            } else {
                text = value
                setTextColor(0xFF333333.toInt())
            }
        })
        layoutOwnerInfo.addView(row)
    }

    private fun buildBookingIntent(roomId: String, roomData: Map<String, Any>): Intent {
        val addr = roomData["address"] as? String ?: ""
        val ward = roomData["ward"] as? String ?: ""
        val district = roomData["district"] as? String ?: ""
        val imageUrls = (roomData["imageUrls"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        val roomPrice = (roomData["price"] as? Number)?.toLong() ?: 0L
        return Intent(this, BookingActivity::class.java).apply {
            putExtra("roomId", roomId)
            putExtra("landlordId", roomData["userId"] as? String ?: "")
            putExtra("roomTitle", roomData["title"] as? String ?: "")
            putExtra("roomAddress", if (addr.isNotEmpty()) "$addr, $ward, $district" else "$ward, $district")
            putExtra("roomPrice", roomPrice)
            putExtra("roomImageUrl", imageUrls.firstOrNull() ?: "")
            putExtra("landlordName", roomData["ownerName"] as? String ?: "")
            putExtra("landlordPhone", roomData["ownerPhone"] as? String ?: "")
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
