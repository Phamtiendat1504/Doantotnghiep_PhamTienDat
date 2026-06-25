package com.example.doantotnghiep.View.Auth

import android.content.Intent
import android.location.Address
import android.os.Build
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
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
    private lateinit var layoutFacilitiesInfo: LinearLayout
    private lateinit var layoutRulesInfo: LinearLayout
    private lateinit var layoutAmenities: LinearLayout
    private lateinit var layoutServiceItems: LinearLayout
    private lateinit var layoutOwnerInfo: LinearLayout
    private lateinit var gridAmenities: GridLayout
    private lateinit var btnBack: ImageView
    private lateinit var btnSave: MaterialButton
    private lateinit var btnBooking: MaterialButton
    private lateinit var btnOpenMaps: MaterialButton
    private lateinit var tvMapAddress: TextView
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
        layoutFacilitiesInfo = findViewById(R.id.layoutFacilitiesInfo)
        layoutRulesInfo = findViewById(R.id.layoutRulesInfo)
        layoutAmenities = findViewById(R.id.layoutAmenities)
        layoutServiceItems = findViewById(R.id.layoutServiceItems)
        layoutOwnerInfo = findViewById(R.id.layoutOwnerInfo)
        gridAmenities = findViewById(R.id.gridAmenities)
        btnBack = findViewById(R.id.btnBack)
        btnSave = findViewById(R.id.btnSavePost)
        btnBooking = findViewById(R.id.btnBooking)
        btnOpenMaps = findViewById(R.id.btnOpenMaps)
        tvMapAddress = findViewById(R.id.tvMapAddress)
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
        tvAddress.text = listOf(address, ward, district).filter { it.isNotBlank() }.distinct().joinToString(", ").ifBlank { "Chưa cập nhật địa chỉ" }

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
        setupFacilitiesInfo(data)
        setupRulesInfo(data)
        setupAmenities(data)
        setupServiceItems(data)
        setupOwnerInfo(data)
        val latitude = (data["latitude"] as? Number)?.toDouble()
        val longitude = (data["longitude"] as? Number)?.toDouble()
        setupMapSection(address, ward, district, latitude, longitude)
        setupAppointmentInfo(data)

        val status = data["status"] as? String ?: "pending"
        val roomOwnerId = data["userId"] as? String ?: ""
        val isOwner = currentUid != null && currentUid == roomOwnerId

        if (status == "rented") {
            btnBooking.isEnabled = false
            btnBooking.text = "Phòng đã cho thuê"
            btnBooking.setBackgroundColor(0xFF9E9E9E.toInt())
            btnSave.visibility = View.GONE
            tvPrice.text = "ĐÃ CHO THUÊ"
            tvPrice.setTextColor(0xFFE53935.toInt())
        } else if (isOwner) {
            // Chủ phòng xem phòng của chính mình → ẩn Lưu và Đặt lịch
            btnSave.visibility = View.GONE
            btnBooking.visibility = View.GONE
        } else {
            // Người dùng khác → hiện nút và load thêm dữ liệu
            btnSave.visibility = View.VISIBLE
            btnBooking.visibility = View.VISIBLE
            setupActionButtons()
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
            val locationParts = listOf(ward, district).filter { it.isNotBlank() }.distinct()
            if (locationParts.isNotEmpty()) append("${locationParts.joinToString(", ")}, ")
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

    override fun onDestroy() {
        super.onDestroy()
        geocodeExecutor.shutdownNow()
    }

    private fun setupAppointmentInfo(data: Map<String, Any>) {
        val cardAppointmentInfo = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardAppointmentInfo)
        val layoutExpiryRow = findViewById<LinearLayout>(R.id.layoutExpiryRow)
        val tvPostCreatedAtDisplay = findViewById<TextView>(R.id.tvPostCreatedAtDisplay)
        val tvPostExpiryDisplay = findViewById<TextView>(R.id.tvPostExpiryDisplay)
        val layoutTimeSlotsRow = findViewById<LinearLayout>(R.id.layoutTimeSlotsRow)
        val layoutTimeSlotsContent = findViewById<LinearLayout>(R.id.layoutTimeSlotsContent)

        val createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L
        val expiryDate = (data["postExpiryDate"] as? Number)?.toLong() ?: 0L
        val timeSlots = data["availableTimeSlots"] as? String ?: ""

        val hasExpiry = expiryDate > 0
        val hasTimeSlots = timeSlots.isNotBlank()

        if (!hasExpiry && !hasTimeSlots) { cardAppointmentInfo.visibility = View.GONE; return }
        cardAppointmentInfo.visibility = View.VISIBLE

        if (hasExpiry) {
            layoutExpiryRow.visibility = View.VISIBLE
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            tvPostCreatedAtDisplay.text = if (createdAt > 0) sdf.format(java.util.Date(createdAt)) else "--"
            tvPostExpiryDisplay.text = sdf.format(java.util.Date(expiryDate))
        } else {
            layoutExpiryRow.visibility = View.GONE
        }

        if (hasTimeSlots) {
            layoutTimeSlotsRow.visibility = View.VISIBLE
            com.example.doantotnghiep.Utils.TimeSlotRenderer.render(layoutTimeSlotsContent, timeSlots)
        } else {
            layoutTimeSlotsRow.visibility = View.GONE
        }
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
        val hasWifi = data["hasWifi"] == true
        val otherFees = data["otherFees"] as? List<Map<String, Any>> ?: emptyList()

        // Nhóm Chi phí (đặc biệt quan trọng nên làm nổi bật)
        if (deposit > 0 || electricPrice > 0 || waterPrice > 0 || hasWifi || otherFees.isNotEmpty()) {
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
            if (hasWifi) {
                addInfoRow("Tiền wifi", if (wifiPrice > 0) "${formatter.format(wifiPrice)} đ/tháng" else "Miễn phí")
            }
            otherFees.forEach { fee ->
                val label = fee["label"] as? String ?: ""; val price = (fee["price"] as? Number)?.toLong() ?: 0L
                if (label.isNotEmpty()) addInfoRow(label, "${formatter.format(price)} đ/tháng")
            }
        }

    }

    private fun setupFacilitiesInfo(data: Map<String, Any>) {
        layoutFacilitiesInfo.removeAllViews()
        val kitchen = data["kitchen"] as? String ?: ""
        val bathroom = data["bathroom"] as? String ?: ""
        if (kitchen.isNotEmpty() && kitchen != "Không") addFacilityRow("Phòng bếp", kitchen)
        if (bathroom.isNotEmpty()) addFacilityRow("Phòng vệ sinh", bathroom)
        if (layoutFacilitiesInfo.childCount == 0) addFacilityRow("Thông tin", "Chưa cập nhật")
    }

    private fun addFacilityRow(label: String, value: String) {
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
        layoutFacilitiesInfo.addView(row)
    }

    private fun setupRulesInfo(data: Map<String, Any>) {
        layoutRulesInfo.removeAllViews()
        val genderPrefer = data["genderPrefer"] as? String ?: ""
        val curfew = data["curfew"] as? String ?: ""
        val curfewTime = data["curfewTime"] as? String ?: ""
        val pet = data["pet"] as? String ?: ""
        if (genderPrefer.isNotEmpty()) addRuleRow("Ưu tiên giới tính", genderPrefer)
        if (curfew.isNotEmpty()) {
            val text = if (curfew == "Tùy chọn" && curfewTime.isNotEmpty()) "Đóng cửa lúc $curfewTime" else curfew
            addRuleRow("Giờ giấc", text)
        }
        if (pet.isNotEmpty()) {
            val petName = data["petName"] as? String ?: ""
            val petCount = (data["petCount"] as? Number)?.toInt() ?: 0
            val text = if (pet == "Cho nuôi") {
                val details = mutableListOf<String>()
                if (petName.isNotEmpty()) details.add(petName)
                if (petCount > 0) details.add("Số lượng: $petCount")
                if (details.isNotEmpty()) "Cho nuôi (${details.joinToString(" - ")})" else "Cho nuôi"
            } else pet
            addRuleRow("Thú cưng", text)
        }
        if (layoutRulesInfo.childCount == 0) addRuleRow("Quy định", "Chưa cập nhật")
    }

    private fun addRuleRow(label: String, value: String) {
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
        layoutRulesInfo.addView(row)
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

        fun aqty(key: String) = (data[key] as? Number)?.toInt() ?: 0
        fun alabel(name: String, qty: Int) = "$name : Số lượng ${qty.coerceAtLeast(1)}"
        val amenitiesNames = mutableListOf<String>()
        if (data["hasAirCon"] == true) amenitiesNames.add(alabel("Điều hòa", aqty("airConQty")))
        if (data["hasWaterHeater"] == true) amenitiesNames.add(alabel("Bình nóng lạnh", aqty("waterHeaterQty")))
        if (data["hasWasher"] == true) amenitiesNames.add(alabel("Máy giặt", aqty("washerQty")))
        if (data["hasDryingArea"] == true) amenitiesNames.add(alabel("Sân phơi đồ", aqty("dryingAreaQty")))
        if (data["hasWardrobe"] == true) amenitiesNames.add(alabel("Tủ quần áo", aqty("wardrobeQty")))
        if (data["hasBed"] == true) amenitiesNames.add(alabel("Giường ngủ", aqty("bedQty")))
        (data["furnitureItems"] as? List<Map<String, Any>> ?: emptyList()).forEach { item ->
            val name = item["name"] as? String ?: ""
            val qty = (item["qty"] as? Number)?.toInt() ?: 1
            if (name.isNotEmpty()) amenitiesNames.add("$name : Số lượng $qty")
        }
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
        val eBikeFee = (data["eBikeFee"] as? Number ?: data["ebikeFee"] as? Number)?.toLong() ?: 0L
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

    private fun setupServiceItems(data: Map<String, Any>) {
        val cardServiceItems = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardServiceItems)
        layoutServiceItems.removeAllViews()
        val services = data["serviceItems"] as? List<Map<String, Any>> ?: emptyList()
        if (services.isEmpty()) {
            cardServiceItems.visibility = android.view.View.GONE
            return
        }
        cardServiceItems.visibility = android.view.View.VISIBLE
        services.forEach { item ->
            val name = item["name"] as? String ?: ""
            val price = (item["price"] as? Number)?.toLong() ?: 0L
            if (name.isNotEmpty()) {
                val priceText = if (price > 0) "${formatter.format(price)} đ/tháng" else "Miễn phí"
                addServiceRow(name, priceText)
            }
        }
    }

    private fun addServiceRow(label: String, value: String) {
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
        layoutServiceItems.addView(row)
        layoutServiceItems.addView(android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1))
            setBackgroundColor(0xFFF5F5F5.toInt())
        })
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
                promptLogin()
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

        // Gọi Firebase lấy dữ liệu chi tiết còn thiếu (email, ngày tham gia, sdt mới nhất)
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

                    // SĐT hiển thị giữ nguyên ownerPhone từ room document (đã set ở trên)
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
            val callPhone = tvSheetPhone.text.toString().trim()
            if (callPhone.isNotEmpty() && callPhone != "Đang cập nhật") {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$callPhone")))
            }
        }
        
        btnSheetChat.setOnClickListener {
            dialog.dismiss()
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            currentUid = userId
            if (userId.isNullOrBlank()) {
                dialog.dismiss()
                promptLogin()
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
            putExtra("roomAddress", listOf(addr, ward, district).filter { it.isNotBlank() }.distinct().joinToString(", "))
            putExtra("roomPrice", roomPrice)
            putExtra("roomImageUrl", imageUrls.firstOrNull() ?: "")
            putExtra("landlordName", roomData["ownerName"] as? String ?: "")
            putExtra("landlordPhone", roomData["ownerPhone"] as? String ?: "")
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
