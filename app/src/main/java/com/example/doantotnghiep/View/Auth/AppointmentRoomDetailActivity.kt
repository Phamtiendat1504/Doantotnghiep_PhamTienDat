package com.example.doantotnghiep.View.Auth

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.doantotnghiep.R
import com.example.doantotnghiep.ViewModel.AppointmentRoomDetailViewModel
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class AppointmentRoomDetailActivity : AppCompatActivity() {

    private lateinit var viewPagerImages: ViewPager2
    private lateinit var tvImageCount: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvPrice: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvDescription: TextView
    private lateinit var layoutRoomInfo: LinearLayout
    private lateinit var layoutAmenities: LinearLayout
    private lateinit var layoutOwnerInfo: LinearLayout
    private lateinit var btnBack: ImageView

    private lateinit var viewModel: AppointmentRoomDetailViewModel

    private val formatter: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN"))
        symbols.groupingSeparator = '.'
        DecimalFormat("#,###", symbols)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appointment_room_detail)

        initViews()
        viewModel = ViewModelProvider(this)[AppointmentRoomDetailViewModel::class.java]

        viewModel.roomData.observe(this) { data ->
            if (data != null) {
                displayData(data)
            } else {
                tvTitle.text = "Phòng không còn tồn tại"
                tvAddress.text = "Bài đăng này đã bị xóa hoặc đã được cho thuê"
                tvPrice.text = ""
                tvDescription.text = ""
                layoutRoomInfo.removeAllViews()
                layoutAmenities.removeAllViews()
                layoutOwnerInfo.removeAllViews()
            }
        }

        viewModel.errorMessage.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                tvTitle.text = "Không thể tải thông tin"
                tvAddress.text = error
                tvPrice.text = ""
                tvDescription.text = ""
            }
        }

        val roomId = intent.getStringExtra("roomId") ?: ""
        if (roomId.isNotEmpty()) viewModel.loadRoomData(roomId)

        btnBack.setOnClickListener { finish() }
    }

    private fun initViews() {
        viewPagerImages = findViewById(R.id.viewPagerImages)
        tvImageCount = findViewById(R.id.tvImageCount)
        tvTitle = findViewById(R.id.tvTitle)
        tvPrice = findViewById(R.id.tvPrice)
        tvAddress = findViewById(R.id.tvAddress)
        tvDescription = findViewById(R.id.tvDescription)
        layoutRoomInfo = findViewById(R.id.layoutRoomInfo)
        layoutAmenities = findViewById(R.id.layoutAmenities)
        layoutOwnerInfo = findViewById(R.id.layoutOwnerInfo)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun displayData(data: Map<String, Any>) {
        tvTitle.text = data["title"] as? String ?: "Chưa có tiêu đề"
        val price = (data["price"] as? Number)?.toLong() ?: 0L
        tvPrice.text = "${formatter.format(price)} đ/tháng"
        val address = data["address"] as? String ?: ""
        val ward = data["ward"] as? String ?: ""
        val district = data["district"] as? String ?: ""
        tvAddress.text = listOf(address, ward, district).filter { it.isNotBlank() }.distinct().joinToString(", ")
        tvDescription.text = (data["description"] as? String)?.takeIf { it.isNotEmpty() } ?: "Không có mô tả"

        val imageUrls = (data["imageUrls"] as? List<*>)?.mapNotNull { it as? String } ?: listOf()
        setupImageSlider(imageUrls)
        setupRoomInfo(data)
        setupAmenities(data)
        setupOwnerInfo(data)
        setupAppointmentInfo(data)
    }

    private fun setupAppointmentInfo(data: Map<String, Any>) {
        val card = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardAppointmentInfo) ?: return
        val layoutExpiry = findViewById<android.widget.LinearLayout>(R.id.layoutExpiryRow) ?: return
        val tvCreatedAt = findViewById<android.widget.TextView>(R.id.tvPostCreatedAtDisplay) ?: return
        val tvExpiry = findViewById<android.widget.TextView>(R.id.tvPostExpiryDisplay) ?: return
        val layoutSlots = findViewById<android.widget.LinearLayout>(R.id.layoutTimeSlotsRow) ?: return
        val tvSlots = findViewById<android.widget.TextView>(R.id.tvAvailableTimeSlotsDisplay) ?: return

        // Xử lý createdAt: Firestore có thể trả về Timestamp hoặc Long
        val createdAt: Long = when (val raw = data["createdAt"]) {
            is Number -> raw.toLong()
            is com.google.firebase.Timestamp -> raw.toDate().time
            else -> 0L
        }
        // Xử lý postExpiryDate tương tự
        val expiryDate: Long = when (val raw = data["postExpiryDate"]) {
            is Number -> raw.toLong()
            is com.google.firebase.Timestamp -> raw.toDate().time
            else -> 0L
        }
        val timeSlots = data["availableTimeSlots"] as? String ?: ""
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("vi", "VN"))

        // Hiển thị card nếu có bất kỳ thông tin nào: createdAt, expiryDate hoặc timeSlots
        if (createdAt <= 0 && expiryDate <= 0 && timeSlots.isBlank()) {
            card.visibility = android.view.View.GONE
            return
        }
        card.visibility = android.view.View.VISIBLE

        if (createdAt > 0 || expiryDate > 0) {
            layoutExpiry.visibility = android.view.View.VISIBLE
            tvCreatedAt.text = if (createdAt > 0) sdf.format(java.util.Date(createdAt)) else "--"
            tvExpiry.text = if (expiryDate > 0) sdf.format(java.util.Date(expiryDate)) else "--"
        } else {
            layoutExpiry.visibility = android.view.View.GONE
        }

        if (timeSlots.isNotBlank()) {
            layoutSlots.visibility = android.view.View.VISIBLE
            tvSlots.text = timeSlots
        } else {
            layoutSlots.visibility = android.view.View.GONE
        }
    }

    private fun setupOwnerInfo(data: Map<String, Any>) {
        layoutOwnerInfo.removeAllViews()
        val name = data["ownerName"] as? String ?: ""
        val phone = data["ownerPhone"] as? String ?: ""
        val gender = data["ownerGender"] as? String ?: ""
        if (name.isNotEmpty()) addOwnerRow("Họ tên", name)
        if (phone.isNotEmpty()) addOwnerRow("SĐT", phone)
        if (gender.isNotEmpty()) addOwnerRow("Giới tính", gender)
        if (name.isEmpty() && phone.isEmpty()) {
            layoutOwnerInfo.addView(TextView(this).apply {
                text = "Không có thông tin"
                textSize = 13f
                setTextColor(0xFF999999.toInt())
            })
        }
    }

    private fun addOwnerRow(label: String, value: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dpToPx(6), 0, dpToPx(6))
        }
        row.addView(TextView(this).apply {
            text = label; textSize = 13f; setTextColor(0xFF999999.toInt())
            layoutParams = LinearLayout.LayoutParams(dpToPx(80), LinearLayout.LayoutParams.WRAP_CONTENT)
        })
        row.addView(TextView(this).apply {
            text = value; textSize = 14f; setTextColor(0xFF333333.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        layoutOwnerInfo.addView(row)
    }

    private fun setupImageSlider(imageUrls: List<String>) {
        val images = if (imageUrls.isEmpty()) listOf("") else imageUrls
        viewPagerImages.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val img = ImageView(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setBackgroundColor(0xFFEDEDED.toInt())
                }
                return object : RecyclerView.ViewHolder(img) {}
            }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val imgView = holder.itemView as ImageView
                if (images[position].isNotEmpty()) {
                    Glide.with(this@AppointmentRoomDetailActivity).load(images[position]).into(imgView)
                }
            }
            override fun getItemCount() = images.size
        }
        tvImageCount.text = "1/${images.size}"
        viewPagerImages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) { tvImageCount.text = "${position + 1}/${images.size}" }
        })
    }

    private fun setupRoomInfo(data: Map<String, Any>) {
        layoutRoomInfo.removeAllViews()
        val area = (data["area"] as? Number)?.toInt() ?: 0
        val peopleCount = (data["peopleCount"] as? Number)?.toInt() ?: 0
        val roomType = data["roomType"] as? String ?: ""
        val genderPrefer = data["genderPrefer"] as? String ?: ""
        val kitchen = data["kitchen"] as? String ?: ""
        val bathroom = data["bathroom"] as? String ?: ""
        val electricPrice = (data["electricPrice"] as? Number)?.toLong() ?: 0L
        val waterPrice = (data["waterPrice"] as? Number)?.toLong() ?: 0L
        val wifiPrice = (data["wifiPrice"] as? Number)?.toLong() ?: 0L
        val deposit = (data["depositAmount"] as? Number)?.toLong() ?: 0L
        val depositMonths = (data["depositMonths"] as? Number)?.toInt() ?: 0
        val curfew = data["curfew"] as? String ?: ""
        val curfewTime = data["curfewTime"] as? String ?: ""
        val pet = data["pet"] as? String ?: ""

        if (area > 0) addInfoRow("Diện tích", "${area} m²")
        if (peopleCount > 0) addInfoRow("Số người ở", "$peopleCount người")
        if (roomType.isNotEmpty()) addInfoRow("Loại phòng", roomType)
        if (genderPrefer.isNotEmpty()) addInfoRow("Ưu tiên giới tính", genderPrefer)
        if (kitchen.isNotEmpty()) addInfoRow("Phòng bếp", kitchen)
        if (bathroom.isNotEmpty()) addInfoRow("Phòng vệ sinh", bathroom)
        if (electricPrice > 0) addInfoRow("Tiền điện", "${formatter.format(electricPrice)} đ/kWh")
        if (waterPrice > 0) addInfoRow("Tiền nước", "${formatter.format(waterPrice)} đ/m³")
        if (data["hasWifi"] == true) addInfoRow("Tiền wifi", if (wifiPrice > 0) "${formatter.format(wifiPrice)} đ/tháng" else "Miễn phí")
        (data["otherFees"] as? List<Map<String, Any>> ?: emptyList()).forEach { fee ->
            val label = fee["label"] as? String ?: ""; val price = (fee["price"] as? Number)?.toLong() ?: 0L
            if (label.isNotEmpty()) addInfoRow(label, "${formatter.format(price)} đ/tháng")
        }
        if (deposit > 0) addInfoRow("Tiền đặt cọc", "${formatter.format(deposit)} đ")
        if (depositMonths > 0) addInfoRow("Đặt cọc trước", "$depositMonths tháng")
        if (curfew.isNotEmpty()) {
            val text = if (curfew == "Tùy chọn" && curfewTime.isNotEmpty()) "Đóng cửa lúc $curfewTime" else curfew
            addInfoRow("Giờ giấc", text)
        }
        if (pet.isNotEmpty()) {
            val petName = data["petName"] as? String ?: ""
            val petCount = (data["petCount"] as? Number)?.toInt() ?: 0
            val petDetails = mutableListOf<String>()
            if (petName.isNotEmpty()) petDetails.add(petName)
            if (petCount > 0) petDetails.add("Số lượng: $petCount")
            val text = if (pet == "Cho nuôi") {
                if (petDetails.isNotEmpty()) "Cho nuôi (${petDetails.joinToString(" - ")})" else "Cho nuôi"
            } else pet
            addInfoRow("Thú cưng", text)
        }
        if (data["hasMotorbike"] == true) {
            val fee = (data["motorbikeFee"] as? Number)?.toLong() ?: 0L
            addInfoRow("Để xe máy", if (fee > 0) "${formatter.format(fee)} đ/xe/tháng" else "Miễn phí")
        }
        if (data["hasEBike"] == true) {
            val fee = (data["eBikeFee"] as? Number ?: data["ebikeFee"] as? Number)?.toLong() ?: 0L
            addInfoRow("Để xe đạp điện", if (fee > 0) "${formatter.format(fee)} đ/xe/tháng" else "Miễn phí")
        }
        if (data["hasBicycle"] == true) {
            val fee = (data["bicycleFee"] as? Number)?.toLong() ?: 0L
            addInfoRow("Để xe đạp", if (fee > 0) "${formatter.format(fee)} đ/xe/tháng" else "Miễn phí")
        }
    }

    private fun addInfoRow(label: String, value: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dpToPx(8), 0, dpToPx(8))
        }
        row.addView(TextView(this).apply {
            text = label; textSize = 14f; setTextColor(0xFF888888.toInt())
            layoutParams = LinearLayout.LayoutParams(dpToPx(120), LinearLayout.LayoutParams.WRAP_CONTENT)
        })
        row.addView(TextView(this).apply {
            text = value; textSize = 14f; setTextColor(0xFF333333.toInt())
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
        val amenities = mutableListOf<String>()
        if (data["hasWifi"] == true) amenities.add("✓ Wifi")
        fun aq(k: String) = (data[k] as? Number)?.toInt() ?: 0
        fun al(n: String, q: Int) = "✓ $n : Số lượng ${q.coerceAtLeast(1)}"
        if (data["hasAirCon"] == true) amenities.add(al("Điều hòa", aq("airConQty")))
        if (data["hasWaterHeater"] == true) amenities.add(al("Bình nóng lạnh", aq("waterHeaterQty")))
        if (data["hasWasher"] == true) amenities.add(al("Máy giặt", aq("washerQty")))
        if (data["hasDryingArea"] == true) amenities.add(al("Sân phơi đồ", aq("dryingAreaQty")))
        if (data["hasWardrobe"] == true) amenities.add(al("Tủ quần áo", aq("wardrobeQty")))
        if (data["hasBed"] == true) amenities.add(al("Giường ngủ", aq("bedQty")))
        (data["furnitureItems"] as? List<Map<String, Any>> ?: emptyList()).forEach { item ->
            val name = item["name"] as? String ?: ""; val qty = (item["qty"] as? Number)?.toInt() ?: 1
            if (name.isNotEmpty()) amenities.add("✓ $name : Số lượng $qty")
        }
        (data["serviceItems"] as? List<Map<String, Any>> ?: emptyList()).forEach { item ->
            val name = item["name"] as? String ?: ""; val price = (item["price"] as? Number)?.toLong() ?: 0L
            if (name.isNotEmpty()) {
                val priceText = if (price > 0) " - ${formatter.format(price)} đ/tháng" else ""
                amenities.add("✓ $name$priceText")
            }
        }

        if (amenities.isEmpty()) {
            layoutAmenities.addView(TextView(this).apply { text = "Không có thông tin tiện ích" })
            return
        }
        for (i in amenities.indices step 2) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dpToPx(4), 0, dpToPx(4)) }
            row.addView(TextView(this).apply {
                text = amenities[i]; textSize = 14f; setTextColor(0xFF2E7D32.toInt())
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            if (i + 1 < amenities.size) {
                row.addView(TextView(this).apply {
                    text = amenities[i + 1]; textSize = 14f; setTextColor(0xFF2E7D32.toInt())
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
            }
            layoutAmenities.addView(row)
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
