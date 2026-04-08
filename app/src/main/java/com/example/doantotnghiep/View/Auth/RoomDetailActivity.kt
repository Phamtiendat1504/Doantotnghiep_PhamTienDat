package com.example.doantotnghiep.View.Auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.ViewModel.RoomViewModel
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
    private lateinit var btnBack: ImageView
    private lateinit var btnSave: MaterialButton
    private lateinit var btnBooking: MaterialButton

    private lateinit var viewModel: RoomViewModel

    private val formatter: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN"))
        symbols.groupingSeparator = '.'
        DecimalFormat("#,###", symbols)
    }

    private val uid by lazy { viewModel.getCurrentUserId() }
    private var currentRoomId = ""
    private var currentRoomData: Map<String, Any> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_detail)

        viewModel = ViewModelProvider(this)[RoomViewModel::class.java]

        initViews()
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
        btnBack = findViewById(R.id.btnBack)
        btnSave = findViewById(R.id.btnSavePost)
        btnBooking = findViewById(R.id.btnBooking)
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
                btnSave.text = "✓ Đã lưu"
                btnSave.setBackgroundColor(0xFF999999.toInt())
            } else {
                btnSave.text = "Lưu bài viết"
                btnSave.setBackgroundColor(0xFF1976D2.toInt())
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

        // Role người dùng → ẩn/hiện nút
        viewModel.userRole.observe(this) { role ->
            if (role == "landlord" || role == "admin") {
                btnSave.visibility = View.GONE
                btnBooking.visibility = View.GONE
            } else {
                btnSave.visibility = View.VISIBLE
                btnBooking.visibility = View.VISIBLE
            }
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

        tvDescription.text = (data["description"] as? String)?.takeIf { it.isNotEmpty() } ?: "Không có mô tả"

        val imageUrls = data["imageUrls"] as? List<String> ?: listOf()
        setupImageSlider(imageUrls)
        setupRoomInfo(data)
        setupAmenities(data)
        setupOwnerInfo(data)

        // Load dữ liệu phụ qua ViewModel
        viewModel.loadBookedSlots(currentRoomId)
        setupActionButtons()
    }

    private fun setupActionButtons() {
        if (uid == null) {
            btnSave.visibility = View.VISIBLE
            btnBooking.visibility = View.VISIBLE
            btnSave.text = "Lưu bài viết"
            btnSave.setBackgroundColor(0xFF1976D2.toInt())
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
        val layoutBooked = findViewById<LinearLayout>(R.id.layoutBookedSlots) ?: return
        val cardBooked = findViewById<CardView>(R.id.cardBookedSlots) ?: return

        layoutBooked.removeAllViews()
        if (slots.isEmpty()) {
            cardBooked.visibility = View.GONE
            return
        }

        cardBooked.visibility = View.VISIBLE
        for (slot in slots) {
            val date = slot["dateDisplay"] as? String ?: slot["date"] as? String ?: ""
            val time = slot["time"] as? String ?: ""
            val tv = TextView(this).apply {
                text = "📅 $date  —  ⏰ $time  (Đã có người hẹn)"
                textSize = 13f
                setTextColor(0xFFE65100.toInt())
                setPadding(0, dpToPx(4), 0, dpToPx(4))
            }
            layoutBooked.addView(tv)
            layoutBooked.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1))
                setBackgroundColor(0xFFF5F5F5.toInt())
            })
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
        val area = (data["area"] as? Number)?.toInt() ?: 0
        val peopleCount = (data["peopleCount"] as? Number)?.toInt() ?: 0
        val roomType = data["roomType"] as? String ?: ""
        val deposit = (data["depositAmount"] as? Number)?.toLong() ?: 0L
        val depositMonths = (data["depositMonths"] as? Number)?.toInt() ?: 0
        val kitchen = data["kitchen"] as? String ?: ""
        val bathroom = data["bathroom"] as? String ?: ""
        val genderPrefer = data["genderPrefer"] as? String ?: ""
        val curfew = data["curfew"] as? String ?: ""
        val curfewTime = data["curfewTime"] as? String ?: ""
        val electricPrice = (data["electricPrice"] as? Number)?.toLong() ?: 0L
        val waterPrice = (data["waterPrice"] as? Number)?.toLong() ?: 0L
        val wifiPrice = (data["wifiPrice"] as? Number)?.toLong() ?: 0L

        if (area > 0) addInfoRow("Diện tích", "${area} m²")
        if (peopleCount > 0) addInfoRow("Số người ở", "$peopleCount người")
        if (roomType.isNotEmpty()) addInfoRow("Dạng phòng", roomType)
        if (genderPrefer.isNotEmpty()) addInfoRow("Giới tính ưu tiên", genderPrefer)
        if (kitchen.isNotEmpty()) addInfoRow("Bếp", kitchen)
        if (bathroom.isNotEmpty()) addInfoRow("Vệ sinh", bathroom)
        if (electricPrice > 0) addInfoRow("Tiền điện", "${formatter.format(electricPrice)} đ/kWh")
        if (waterPrice > 0) addInfoRow("Tiền nước", "${formatter.format(waterPrice)} đ/m³")
        if (wifiPrice > 0) addInfoRow("Tiền wifi", "${formatter.format(wifiPrice)} đ/tháng")
        if (deposit > 0) addInfoRow("Đặt cọc", "${formatter.format(deposit)} đ")
        if (depositMonths > 0) addInfoRow("Cọc trước", "$depositMonths tháng")

        if (curfew.isNotEmpty()) {
            val text = if (curfew == "Tùy chọn" && curfewTime.isNotEmpty()) curfewTime else curfew
            addInfoRow("Giờ đóng cửa", text)
        }

        val pet = data["pet"] as? String ?: ""
        if (pet.isNotEmpty()) {
            val petName = data["petName"] as? String ?: ""
            val petCount = (data["petCount"] as? Number)?.toInt() ?: 0
            val text = buildString {
                if (pet == "Cho nuôi") {
                    append("Cho nuôi")
                    if (petName.isNotEmpty()) append(": $petName")
                    if (petCount > 0) append(" (SL: $petCount con)")
                } else {
                    append(pet)
                }
            }
            addInfoRow("Thú cưng", text)
        }
    }

    private fun addInfoRow(label: String, value: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dpToPx(6), 0, dpToPx(6))
        }
        row.addView(TextView(this).apply {
            text = label
            textSize = 13f
            setTextColor(0xFF999999.toInt())
            layoutParams = LinearLayout.LayoutParams(dpToPx(120), LinearLayout.LayoutParams.WRAP_CONTENT)
        })
        row.addView(TextView(this).apply {
            text = value
            textSize = 14f
            setTextColor(0xFF333333.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        layoutRoomInfo.addView(row)
        layoutRoomInfo.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1))
            setBackgroundColor(0xFFF5F5F5.toInt())
        })
    }

    private fun setupAmenities(data: Map<String, Any>) {
        layoutAmenities.removeAllViews()

        val basicAmenities = mutableListOf<String>()
        if (data["hasWifi"] == true) basicAmenities.add("Wifi")
        if (data["hasAirCon"] == true) basicAmenities.add("Điều hòa")
        if (data["hasWaterHeater"] == true) basicAmenities.add("Máy nước nóng")
        if (data["hasWasher"] == true) basicAmenities.add("Máy giặt")
        if (data["hasDryingArea"] == true) basicAmenities.add("Chỗ phơi đồ")
        if (data["hasWardrobe"] == true) basicAmenities.add("Tủ quần áo")
        if (data["hasBed"] == true) basicAmenities.add("Giường")

        val motorbikeFee = (data["motorbikeFee"] as? Number)?.toLong() ?: 0L
        val eBikeFee = (data["eBikeFee"] as? Number)?.toLong() ?: 0L
        val bicycleFee = (data["bicycleFee"] as? Number)?.toLong() ?: 0L

        val hasAnyAmenity = basicAmenities.isNotEmpty() ||
                data["hasMotorbike"] == true ||
                data["hasEBike"] == true ||
                data["hasBicycle"] == true

        if (!hasAnyAmenity) {
            layoutAmenities.addView(TextView(this).apply {
                text = "Không có thông tin"
                textSize = 13f
                setTextColor(0xFF999999.toInt())
            })
            return
        }

        if (basicAmenities.isNotEmpty()) {
            for (i in basicAmenities.indices step 2) {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, dpToPx(5), 0, dpToPx(5))
                }
                row.addView(TextView(this).apply {
                    text = basicAmenities[i]
                    textSize = 13f
                    setTextColor(0xFF1A1A2E.toInt())
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                if (i + 1 < basicAmenities.size) {
                    row.addView(TextView(this).apply {
                        text = basicAmenities[i + 1]
                        textSize = 13f
                        setTextColor(0xFF1A1A2E.toInt())
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    })
                } else {
                    row.addView(android.view.View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
                    })
                }
                layoutAmenities.addView(row)
            }
        }

        if (basicAmenities.isNotEmpty() &&
            (data["hasMotorbike"] == true || data["hasEBike"] == true || data["hasBicycle"] == true)) {
            layoutAmenities.addView(android.view.View(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)).apply {
                    topMargin = dpToPx(6); bottomMargin = dpToPx(6)
                }
                setBackgroundColor(0xFFF0F0F0.toInt())
            })
        }

        if (data["hasMotorbike"] == true) {
            val feeText = if (motorbikeFee > 0) "${formatter.format(motorbikeFee)} đ/xe/tháng" else "Miễn phí"
            addAmenityFeeRow("Để xe máy", feeText)
        }
        if (data["hasEBike"] == true) {
            val feeText = if (eBikeFee > 0) "${formatter.format(eBikeFee)} đ/xe/tháng" else "Miễn phí"
            addAmenityFeeRow("Để xe đạp điện", feeText)
        }
        if (data["hasBicycle"] == true) {
            val feeText = if (bicycleFee > 0) "${formatter.format(bicycleFee)} đ/xe/tháng" else "Miễn phí"
            addAmenityFeeRow("Để xe đạp", feeText)
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
        val name = data["ownerName"] as? String ?: ""
        val phone = data["ownerPhone"] as? String ?: ""
        val gender = data["ownerGender"] as? String ?: ""
        if (name.isNotEmpty()) addOwnerRow("Họ tên", name)
        if (phone.isNotEmpty()) addOwnerRow("SĐT", phone, isPhone = true)
        if (gender.isNotEmpty()) addOwnerRow("Giới tính", gender)
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