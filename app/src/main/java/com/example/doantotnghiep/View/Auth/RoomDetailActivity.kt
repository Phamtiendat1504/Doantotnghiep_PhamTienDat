package com.example.doantotnghiep.View.Auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
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
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

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
    private lateinit var webViewMap: WebView
    private lateinit var tvBookedCount: TextView
    private lateinit var btnViewAllSlots: TextView
    private lateinit var layoutBookedSummary: LinearLayout
    private lateinit var cardBookedSlots: CardView

    private lateinit var tvSpecArea: TextView
    private lateinit var tvSpecPeople: TextView
    private lateinit var tvSpecType: TextView
    private lateinit var btnReadMore: TextView

    private lateinit var viewModel: RoomViewModel

    private val formatter: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN"))
        symbols.groupingSeparator = '.'
        DecimalFormat("#,###", symbols)
    }

    private val uid by lazy { viewModel.getCurrentUserId() }
    private var currentRoomId = ""
    private var currentRoomData: Map<String, Any> = emptyMap()

    private lateinit var btnBackContainer: android.view.View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Làm trong suốt Status Bar và xử lý Edge-to-Edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        
        setContentView(R.layout.activity_room_detail)
        
        // Khởi tạo container nút Back ngay lập tức để tránh crash khi xử lý Insets
        btnBackContainer = findViewById(R.id.btnBackContainer)

        viewModel = ViewModelProvider(this)[RoomViewModel::class.java]

        initViews()

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
        webViewMap = findViewById(R.id.webViewMap)
        tvBookedCount = findViewById(R.id.tvBookedCount)
        btnViewAllSlots = findViewById(R.id.btnViewAllSlots)
        layoutBookedSummary = findViewById(R.id.layoutBookedSummary)
        cardBookedSlots = findViewById(R.id.cardBookedSlots)
        tvSpecArea = findViewById(R.id.tvSpecArea)
        tvSpecPeople = findViewById(R.id.tvSpecPeople)
        tvSpecType = findViewById(R.id.tvSpecType)
        btnReadMore = findViewById(R.id.btnReadMore)
        // btnBackContainer đã được init sớm trong onCreate
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
            if (saved) {
                btnSave.setIconResource(R.drawable.ic_save) // Dùng icon đã lưu
                btnSave.setIconTintResource(R.color.secondary)
            } else {
                btnSave.setIconResource(R.drawable.ic_bookmark)
                btnSave.setIconTintResource(R.color.primary)
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

        tvRoomNumber.text = "Phòng ${String.format("%02d", index + 1)}"
        tvTitle.text = data["title"] as? String ?: "Chưa có tiêu đề"

        val price = data["price"] as? Long ?: 0
        tvPrice.text = "${formatter.format(price)} đ/tháng"

        val address = data["address"] as? String ?: ""
        val ward = data["ward"] as? String ?: ""
        val district = data["district"] as? String ?: ""
        tvAddress.text = if (address.isNotEmpty()) "$address, $ward, $district" else "$ward, $district"

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

        val imageUrls = data["imageUrls"] as? List<String> ?: listOf()
        setupImageSlider(imageUrls)
        setupRoomInfo(data)
        setupAmenities(data)
        setupOwnerInfo(data)
        setupMapSection(address, ward, district)

        val status = data["status"] as? String ?: "pending"
        // Lấy chủ sở hữu bài đăng
        val roomOwnerId = data["userId"] as? String ?: ""
        val isOwner = uid != null && uid == roomOwnerId

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
            viewModel.loadBookedSlots(currentRoomId)
            setupActionButtons()
        }
    }

    private fun setupActionButtons() {
        if (uid == null) {
            btnSave.visibility = View.VISIBLE
            btnBooking.visibility = View.VISIBLE
            btnSave.setIconResource(R.drawable.ic_bookmark)
            btnSave.setIconTintResource(R.color.primary)
            btnSave.setOnClickListener { promptLogin() }
            btnBooking.setOnClickListener { promptLogin() }
            return
        }

        // Load role, trạng thái lưu, trạng thái đặt lịch qua ViewModel
        viewModel.loadUserRole(uid!!)
        viewModel.checkSavedStatus(uid!!, currentRoomId)
        viewModel.checkActiveBooking(uid!!, currentRoomId)

        btnSave.setOnClickListener {
            viewModel.toggleSavePost(uid!!, currentRoomId, currentRoomData)
        }
    }

    // ─── GOOGLE MAPS ───────────────────────────────────────────────────────────
    private fun setupMapSection(address: String, ward: String, district: String) {
        val fullAddress = buildString {
            if (address.isNotEmpty()) append("$address, ")
            if (ward.isNotEmpty()) append("$ward, ")
            if (district.isNotEmpty()) append("$district, ")
            append("Hà Nội")
        }

        // Hiển thị địa chỉ text
        tvMapAddress.text = fullAddress

        // Nhúng Google Maps vào WebView (không cần API key)
        with(webViewMap.settings) {
            javaScriptEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            domStorageEnabled = true
        }
        webViewMap.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: android.webkit.WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                
                // Nếu là link web bình thường thì cho chạy trong WebView
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    // Mở link Google Maps sang trình duyệt/app ngoài cho tiện
                    if (url.contains("maps.google.com")) {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        return true
                    }
                    return false 
                }

                // Nếu là các link gọi app hệ thống (vd: intent://, geo://)
                try {
                    val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                        return true
                    }
                    // Nếu máy không có app, thử tìm link fallback (mở trình duyệt)
                    val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                    if (fallbackUrl != null) {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)))
                        return true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                // Trả về true để báo là app đã tự xử lý, đừng văng lỗi "Không khả dụng"
                return true
            }
        }

        // URL nhúng bản đồ — tìm kiếm theo địa chỉ, không cần API key
        val encodedAddr = Uri.encode(fullAddress)
        val embedUrl = "https://maps.google.com/maps?q=$encodedAddr&output=embed&hl=vi&z=15"
        
        // Bọc trong thẻ HTML tiêu chuẩn chứa iframe để vượt lỗi của Google Maps
        val iframeHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                <style>
                    body { margin: 0; padding: 0; overflow: hidden; }
                    iframe { width: 100%; height: 100vh; border: none; }
                </style>
            </head>
            <body>
                <iframe src="$embedUrl" allowfullscreen loading="lazy"></iframe>
            </body>
            </html>
        """.trimIndent()

        webViewMap.loadDataWithBaseURL(null, iframeHtml, "text/html", "utf-8", null)

        // Nút mở full Google Maps app
        btnOpenMaps.setOnClickListener {
            openGoogleMaps(fullAddress)
        }
    }

    private fun openGoogleMaps(address: String) {
        // Thử mở app Google Maps trước
        val geoUri = Uri.parse("geo:0,0?q=${Uri.encode(address)}")
        val mapsIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (mapsIntent.resolveActivity(packageManager) != null) {
            startActivity(mapsIntent)
        } else {
            // Fallback: mở bằng trình duyệt nếu chưa cài Google Maps
            val browserUri = Uri.parse(
                "https://www.google.com/maps/search/?api=1&query=${Uri.encode(address)}"
            )
            startActivity(Intent(Intent.ACTION_VIEW, browserUri))
        }
    }
    // ──────────────────────────────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        if (currentRoomId.isNotEmpty() && uid != null) {
            viewModel.checkActiveBooking(uid!!, currentRoomId)
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
                val text = if (pet == "Cho nuôi" && petName.isNotEmpty()) "Cho nuôi ($petName)" else pet
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
                        savedAvatarUrl = doc.getString("avatarUrl")!!
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
            val currentUid = uid
            if (currentUid == null) {
                startActivity(Intent(this, LoginActivity::class.java))
                return@setOnClickListener
            }
            if (currentUid == landlordId) return@setOnClickListener  // không tự nhắn mình
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
            val currentUid = uid
            if (currentUid == null) {
                startActivity(Intent(this, LoginActivity::class.java))
                return@setOnClickListener
            }
            if (currentUid == landlordId) return@setOnClickListener
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
                text = "📞 $value"
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
        return Intent(this, BookingActivity::class.java).apply {
            putExtra("roomId", roomId)
            putExtra("landlordId", roomData["userId"] as? String ?: "")
            putExtra("roomTitle", roomData["title"] as? String ?: "")
            putExtra("roomAddress", if (addr.isNotEmpty()) "$addr, $ward, $district" else "$ward, $district")
            putExtra("roomPrice", roomData["price"] as? Long ?: 0)
            putExtra("roomImageUrl", (roomData["imageUrls"] as? List<String>)?.firstOrNull() ?: "")
            putExtra("landlordName", roomData["ownerName"] as? String ?: "")
            putExtra("landlordPhone", roomData["ownerPhone"] as? String ?: "")
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}