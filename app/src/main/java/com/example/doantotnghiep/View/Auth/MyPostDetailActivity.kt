package com.example.doantotnghiep.View.Auth

import android.content.Intent
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
import com.example.doantotnghiep.ViewModel.MyPostDetailViewModel
import com.google.android.material.button.MaterialButton
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

class MyPostDetailActivity : AppCompatActivity() {

    private lateinit var viewModel: MyPostDetailViewModel
    private val formatter: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN"))
        symbols.groupingSeparator = '.'
        DecimalFormat("#,###", symbols)
    }
    private var roomId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_post_detail)

        roomId = intent.getStringExtra("roomId") ?: ""
        if (roomId.isEmpty()) { finish(); return }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        viewModel = ViewModelProvider(this)[MyPostDetailViewModel::class.java]

        viewModel.roomData.observe(this) { data ->
            if (data == null) { finish(); return@observe }
            bindData(data)
        }

        loadRoomDetail()
    }

    override fun onResume() {
        super.onResume()
        if (roomId.isNotEmpty()) loadRoomDetail()
    }

    private fun loadRoomDetail() {
        viewModel.loadRoomDetail(roomId)
    }

    private fun bindData(d: Map<String, Any>) {
        val status = d["status"] as? String ?: "pending"
        val rejectReason = d["rejectReason"] as? String ?: ""

        val tvStatusBadge = findViewById<TextView>(R.id.tvStatusBadge)
        when (status) {
            "pending" -> { tvStatusBadge.text = "⏳ Chờ duyệt"; tvStatusBadge.setTextColor(0xFFE65100.toInt()); tvStatusBadge.setBackgroundColor(0x33FF9800.toInt()) }
            "approved" -> { tvStatusBadge.text = "✓ Đã duyệt"; tvStatusBadge.setTextColor(0xFF2E7D32.toInt()); tvStatusBadge.setBackgroundColor(0x334CAF50.toInt()) }
            "rejected" -> { tvStatusBadge.text = "✗ Từ chối"; tvStatusBadge.setTextColor(0xFFD32F2F.toInt()); tvStatusBadge.setBackgroundColor(0x33F44336.toInt()) }
            "expired" -> { tvStatusBadge.text = "⏰ Hết hạn"; tvStatusBadge.setTextColor(0xFF757575.toInt()); tvStatusBadge.setBackgroundColor(0x33757575.toInt()) }
        }

        val layoutReject = findViewById<LinearLayout>(R.id.layoutRejectReason)
        val tvRejectReason = findViewById<TextView>(R.id.tvRejectReason)
        if (status == "rejected" && rejectReason.isNotEmpty()) {
            layoutReject.visibility = View.VISIBLE
            tvRejectReason.text = "Lý do từ chối: $rejectReason"
        } else {
            layoutReject.visibility = View.GONE
        }

        findViewById<TextView>(R.id.tvTitle).text = d["title"] as? String ?: "Chưa có tiêu đề"
        val price = d["price"] as? Long ?: 0
        findViewById<TextView>(R.id.tvPrice).text = "${formatter.format(price)} đ/tháng"

        val address = d["address"] as? String ?: ""
        val ward = d["ward"] as? String ?: ""
        val district = d["district"] as? String ?: ""
        findViewById<TextView>(R.id.tvAddress).text = if (address.isNotEmpty()) "📍 $address, $ward, $district" else "📍 $ward, $district"

        val createdAt = d["createdAt"] as? Long ?: 0
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi", "VN"))
        findViewById<TextView>(R.id.tvPostDate).text = "📅 Ngày đăng: ${sdf.format(Date(createdAt))}"
        findViewById<TextView>(R.id.tvDescription).text = (d["description"] as? String) ?: "Không có mô tả"

        setupImageSlider(d["imageUrls"] as? List<String> ?: listOf())
        setupRoomInfo(d)
        setupAmenities(d)

        val btnEdit = findViewById<MaterialButton>(R.id.btnEditPost)
        if (status == "rejected") {
            btnEdit.visibility = View.VISIBLE
            btnEdit.setOnClickListener {
                startActivity(Intent(this, EditPostActivity::class.java).apply { putExtra("roomId", roomId) })
            }
        } else {
            btnEdit.visibility = View.GONE
        }
    }

    private fun setupImageSlider(imageUrls: List<String>) {
        val viewPager = findViewById<ViewPager2>(R.id.viewPagerImages)
        val tvCount = findViewById<TextView>(R.id.tvImageCount)
        val images = if (imageUrls.isEmpty()) listOf("") else imageUrls

        viewPager.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
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
                if (images[position].isNotEmpty()) Glide.with(this@MyPostDetailActivity).load(images[position]).centerCrop().into(imgView)
            }
            override fun getItemCount() = images.size
        }
        tvCount.text = "1/${images.size}"
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) { tvCount.text = "${position + 1}/${images.size}" }
        })
    }

    private fun setupRoomInfo(data: Map<String, Any>) {
        val layout = findViewById<LinearLayout>(R.id.layoutRoomInfo)
        layout.removeAllViews()
        val area = (data["area"] as? Number)?.toInt() ?: 0
        if (area > 0) addInfoRow(layout, "Diện tích", "${area} m²")
        val peopleCount = (data["peopleCount"] as? Number)?.toInt() ?: 0
        if (peopleCount > 0) addInfoRow(layout, "Số người ở", "$peopleCount người")
        val roomType = data["roomType"] as? String ?: ""
        if (roomType.isNotEmpty()) addInfoRow(layout, "Loại phòng", roomType)
        val genderPrefer = data["genderPrefer"] as? String ?: ""
        if (genderPrefer.isNotEmpty()) addInfoRow(layout, "Đối tượng thuê", genderPrefer)
        val depositMonths = (data["depositMonths"] as? Number)?.toInt() ?: 0
        if (depositMonths > 0) addInfoRow(layout, "Đặt cọc", "$depositMonths tháng")
        val depositAmount = (data["depositAmount"] as? Number)?.toLong() ?: 0L
        if (depositAmount > 0) addInfoRow(layout, "Số tiền cọc", "${formatter.format(depositAmount)} đ")
        val wifiPrice = (data["wifiPrice"] as? Number)?.toLong() ?: 0L
        if (data["hasWifi"] == true) addInfoRow(layout, "Internet", if (wifiPrice > 0) "${formatter.format(wifiPrice)} đ/tháng" else "Miễn phí")
        val electricPrice = (data["electricPrice"] as? Number)?.toLong() ?: 0L
        if (electricPrice > 0) addInfoRow(layout, "Tiền điện", "${formatter.format(electricPrice)} đ/kWh")
        val waterPrice = (data["waterPrice"] as? Number)?.toLong() ?: 0L
        if (waterPrice > 0) addInfoRow(layout, "Tiền nước", "${formatter.format(waterPrice)} đ/khối")
        if (data["hasMotorbike"] == true) {
            val fee = (data["motorbikeFee"] as? Number)?.toLong() ?: 0L
            addInfoRow(layout, "Để xe máy", if (fee > 0) "${formatter.format(fee)} đ/xe/tháng" else "Miễn phí")
        }
        if (data["hasEBike"] == true) {
            val fee = (data["eBikeFee"] as? Number)?.toLong() ?: 0L
            addInfoRow(layout, "Để xe đạp điện", if (fee > 0) "${formatter.format(fee)} đ/xe/tháng" else "Miễn phí")
        }
        if (data["hasBicycle"] == true) {
            val fee = (data["bicycleFee"] as? Number)?.toLong() ?: 0L
            addInfoRow(layout, "Để xe đạp", if (fee > 0) "${formatter.format(fee)} đ/xe/tháng" else "Miễn phí")
        }
        val curfew = data["curfew"] as? String ?: ""
        val curfewTime = data["curfewTime"] as? String ?: ""
        if (curfew.isNotEmpty()) {
            val text = if (curfew == "Tùy chọn" && curfewTime.isNotEmpty()) curfewTime else curfew
            addInfoRow(layout, "Giờ đóng cửa", text)
        }
        val pet = data["pet"] as? String ?: ""
        if (pet.isNotEmpty()) {
            val petName = data["petName"] as? String ?: ""
            val petCount = (data["petCount"] as? Number)?.toInt() ?: 0
            val petText = if (pet == "Cho nuôi") {
                buildString { append("Cho nuôi"); if (petName.isNotEmpty()) append(": $petName"); if (petCount > 0) append(" (SL: $petCount con)") }
            } else pet
            addInfoRow(layout, "Thú cưng", petText)
        }
    }

    private fun addInfoRow(parent: LinearLayout, label: String, value: String) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dpToPx(8), 0, dpToPx(8)) }
        row.addView(TextView(this).apply {
            text = label; textSize = 14f; setTextColor(0xFF757575.toInt())
            layoutParams = LinearLayout.LayoutParams(dpToPx(130), -2)
        })
        row.addView(TextView(this).apply {
            text = value; textSize = 14f; setTextColor(0xFF212121.toInt()); setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        })
        parent.addView(row)
        parent.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, dpToPx(1)); setBackgroundColor(0xFFEEEEEE.toInt())
        })
    }

    private fun setupAmenities(data: Map<String, Any>) {
        val layout = findViewById<LinearLayout>(R.id.layoutAmenities)
        layout.removeAllViews()
        val basicAms = mutableListOf<String>()
        if (data["hasAirCon"] == true) basicAms.add("Điều hòa")
        if (data["hasWaterHeater"] == true) basicAms.add("Máy nước nóng")
        if (data["hasWasher"] == true) basicAms.add("Máy giặt")
        if (data["hasBed"] == true) basicAms.add("Giường ngủ")
        if (data["hasWardrobe"] == true) basicAms.add("Tủ quần áo")
        if (data["hasDryingArea"] == true) basicAms.add("Sân phơi đồ")
        val kitchen = data["kitchen"] as? String ?: ""
        if (kitchen.isNotEmpty() && kitchen != "Không") basicAms.add("Bếp $kitchen")
        val bathroom = data["bathroom"] as? String ?: ""
        if (bathroom.isNotEmpty()) basicAms.add("WC $bathroom")
        val hasParking = data["hasMotorbike"] == true || data["hasEBike"] == true || data["hasBicycle"] == true
        if (basicAms.isEmpty() && !hasParking) {
            layout.addView(TextView(this).apply { text = "Không có thông tin tiện ích"; textSize = 13f; setTextColor(0xFF999999.toInt()) })
            return
        }
        for (i in basicAms.indices step 2) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dpToPx(5), 0, dpToPx(5)) }
            row.addView(TextView(this).apply { text = basicAms[i]; textSize = 13f; setTextColor(0xFF212121.toInt()); layoutParams = LinearLayout.LayoutParams(0, -2, 1f) })
            if (i + 1 < basicAms.size) {
                row.addView(TextView(this).apply { text = basicAms[i + 1]; textSize = 13f; setTextColor(0xFF212121.toInt()); layoutParams = LinearLayout.LayoutParams(0, -2, 1f) })
            } else {
                row.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(0, -2, 1f) })
            }
            layout.addView(row)
        }
        if (basicAms.isNotEmpty() && hasParking) {
            layout.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(-1, dpToPx(1)).apply { topMargin = dpToPx(6); bottomMargin = dpToPx(6) }
                setBackgroundColor(0xFFEEEEEE.toInt())
            })
        }
        if (data["hasMotorbike"] == true) {
            val fee = (data["motorbikeFee"] as? Number)?.toLong() ?: 0L
            addInfoRow(layout, "Để xe máy", if (fee > 0) "${formatter.format(fee)} đ/xe/tháng" else "Miễn phí")
        }
        if (data["hasEBike"] == true) {
            val fee = (data["eBikeFee"] as? Number)?.toLong() ?: 0L
            addInfoRow(layout, "Để xe đạp điện", if (fee > 0) "${formatter.format(fee)} đ/xe/tháng" else "Miễn phí")
        }
        if (data["hasBicycle"] == true) {
            val fee = (data["bicycleFee"] as? Number)?.toLong() ?: 0L
            addInfoRow(layout, "Để xe đạp", if (fee > 0) "${formatter.format(fee)} đ/xe/tháng" else "Miễn phí")
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}