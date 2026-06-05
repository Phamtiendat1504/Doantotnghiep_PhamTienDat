package com.example.doantotnghiep.View.Auth

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.ViewModel.MyPostDetailViewModel
import com.google.android.material.button.MaterialButton
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyPostDetailActivity : AppCompatActivity() {

    private lateinit var viewModel: MyPostDetailViewModel
    private var imagePageCallback: ViewPager2.OnPageChangeCallback? = null
    private var loadingDialog: AlertDialog? = null
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
        if (roomId.isEmpty()) {
            finish()
            return
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        viewModel = ViewModelProvider(this)[MyPostDetailViewModel::class.java]

        viewModel.roomData.observe(this) { data ->
            if (data == null) {
                finish()
                return@observe
            }
            bindData(data)
        }

        viewModel.markRentedStatus.observe(this) { success ->
            hideLoading()
            if (success) {
                MessageUtils.showSuccessDialog(
                    context = this,
                    title = "Thành công",
                    message = "Bài đăng đã được chuyển sang trạng thái Đã cho thuê thành công."
                ) {
                    finish()
                }
            } else {
                MessageUtils.showErrorDialog(
                    context = this,
                    title = "Thất bại",
                    message = "Không thể cập nhật trạng thái bài đăng. Vui lòng thử lại."
                )
            }
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
        setStatusBadge(tvStatusBadge, status)

        val layoutReject = findViewById<LinearLayout>(R.id.layoutRejectReason)
        val tvRejectReason = findViewById<TextView>(R.id.tvRejectReason)
        if (status == "rejected" && rejectReason.isNotEmpty()) {
            layoutReject.visibility = View.VISIBLE
            tvRejectReason.text = "L\u00fd do t\u1eeb ch\u1ed1i: $rejectReason"
        } else {
            layoutReject.visibility = View.GONE
        }

        findViewById<TextView>(R.id.tvTitle).text = d["title"] as? String ?: "Ch\u01b0a c\u00f3 ti\u00eau \u0111\u1ec1"

        val price = (d["price"] as? Number)?.toLong() ?: 0L
        findViewById<TextView>(R.id.tvPrice).text = "${formatter.format(price)} \u0111/th\u00e1ng"

        val address = d["address"] as? String ?: ""
        val ward = d["ward"] as? String ?: ""
        val district = d["district"] as? String ?: ""
        val locationList = listOf(address, ward, district).filter { it.isNotBlank() }.distinct()
        findViewById<TextView>(R.id.tvAddress).text = "Địa chỉ: ${locationList.joinToString(", ")}"

        val createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi", "VN"))
        findViewById<TextView>(R.id.tvPostDate).text = if (createdAt > 0L) {
            "Ng\u00e0y \u0111\u0103ng: ${sdf.format(Date(createdAt))}"
        } else {
            "Ng\u00e0y \u0111\u0103ng: --"
        }

        findViewById<TextView>(R.id.tvDescription).text =
            (d["description"] as? String)?.takeIf { it.isNotBlank() } ?: "Kh\u00f4ng c\u00f3 m\u00f4 t\u1ea3"

        setupImageSlider((d["imageUrls"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList())
        setupBasicInfo(d)
        setupCostInfo(d)
        setupRulesInfo(d)
        setupAmenities(d)
        setupAppointmentInfo(d)

        val btnEdit = findViewById<MaterialButton>(R.id.btnEditPost)
        val btnMarkRented = findViewById<MaterialButton>(R.id.btnMarkRented)

        if (status == "rejected") {
            btnEdit.visibility = View.VISIBLE
            btnEdit.setOnClickListener {
                startActivity(Intent(this, EditPostActivity::class.java).apply {
                    putExtra("roomId", roomId)
                })
            }
        } else {
            btnEdit.visibility = View.GONE
        }

        if (status == "approved") {
            btnMarkRented.visibility = View.VISIBLE
            btnMarkRented.isEnabled = true
            btnMarkRented.setOnClickListener {
                showMarkRentedDialog()
            }
        } else {
            btnMarkRented.visibility = View.GONE
        }
    }

    private fun setStatusBadge(view: TextView, status: String) {
        val (text, textColor, bgColor) = when (status) {
            "approved" -> Triple("\u0110\u00e3 duy\u1ec7t", 0xFF2E7D32.toInt(), 0x334CAF50)
            "rejected" -> Triple("T\u1eeb ch\u1ed1i", 0xFFD32F2F.toInt(), 0x33F44336)
            "rented" -> Triple("Đã cho thuê", 0xFF1976D2.toInt(), 0x331976D2)
            else -> Triple("Ch\u1edd duy\u1ec7t", 0xFFE65100.toInt(), 0x33FF9800)
        }
        view.text = text
        view.setTextColor(textColor)
        view.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(999).toFloat()
            setColor(bgColor)
        }
    }

    private fun showMarkRentedDialog() {
        MessageUtils.showConfirmDialog(
            context = this,
            title = "Xác nhận đã cho thuê",
            message = "Bài đăng sẽ được ẩn khỏi trang tìm kiếm sau khi bạn xác nhận.\n\nCác lịch hẹn liên quan sẽ được cập nhật theo trạng thái phòng đã cho thuê.",
            positiveText = "Xác nhận",
            negativeText = "Hủy",
            onConfirm = {
                showLoading()
                viewModel.markAsRented(roomId)
            }
        )
    }

    private fun showLoading() {
        loadingDialog?.dismiss()
        loadingDialog = MessageUtils.showLoadingDialog(this, "Đang cập nhật trạng thái...", "Đang xử lý")
    }

    private fun hideLoading() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    private fun setupImageSlider(imageUrls: List<String>) {
        val viewPager = findViewById<ViewPager2>(R.id.viewPagerImages)
        val tvCount = findViewById<TextView>(R.id.tvImageCount)
        val images = if (imageUrls.isEmpty()) listOf("") else imageUrls

        viewPager.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val imageView = ImageView(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setBackgroundColor(0xFFE0E0E0.toInt())
                }
                return object : RecyclerView.ViewHolder(imageView) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val imageView = holder.itemView as ImageView
                val url = images[position]
                if (url.isNotEmpty()) {
                    Glide.with(this@MyPostDetailActivity).load(url).centerCrop().into(imageView)
                }
            }

            override fun getItemCount(): Int = images.size
        }

        imagePageCallback?.let { viewPager.unregisterOnPageChangeCallback(it) }
        tvCount.text = "1/${images.size}"
        val callback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                tvCount.text = "${position + 1}/${images.size}"
            }
        }
        imagePageCallback = callback
        viewPager.registerOnPageChangeCallback(callback)
    }

    private fun setupBasicInfo(data: Map<String, Any>) {
        val layout = findViewById<LinearLayout>(R.id.layoutBasicInfo)
        layout.removeAllViews()

        val area = (data["area"] as? Number)?.toInt() ?: 0
        if (area > 0) addInfoRow(layout, "Diện tích", "${area} m²")

        val peopleCount = (data["peopleCount"] as? Number)?.toInt() ?: 0
        if (peopleCount > 0) addInfoRow(layout, "Số người ở", "$peopleCount người")

        val roomType = data["roomType"] as? String ?: ""
        if (roomType.isNotEmpty()) addInfoRow(layout, "Loại phòng", roomType)

        val roomCount = (data["roomCount"] as? Number)?.toInt() ?: 0
        val rentedCount = (data["rentedCount"] as? Number)?.toInt() ?: 0
        if (roomCount > 0) {
            val available = roomCount - rentedCount
            addInfoRow(layout, "Số phòng", "$roomCount phòng (còn $available trống)")
        }

        val genderPrefer = data["genderPrefer"] as? String ?: ""
        if (genderPrefer.isNotEmpty()) addInfoRow(layout, "Ưu tiên giới tính", genderPrefer)

        if (layout.childCount == 0) addInfoRow(layout, "Thông tin", "Chưa cập nhật")
    }

    private fun setupCostInfo(data: Map<String, Any>) {
        val layout = findViewById<LinearLayout>(R.id.layoutCostInfo)
        layout.removeAllViews()

        val depositAmount = (data["depositAmount"] as? Number)?.toLong() ?: 0L
        if (depositAmount > 0) addInfoRow(layout, "Tiền đặt cọc", "${formatter.format(depositAmount)} đ")

        val depositMonths = (data["depositMonths"] as? Number)?.toInt() ?: 0
        if (depositMonths > 0) addInfoRow(layout, "Đặt cọc trước", "$depositMonths tháng")

        val electricPrice = (data["electricPrice"] as? Number)?.toLong() ?: 0L
        if (electricPrice > 0) addInfoRow(layout, "Tiền điện", "${formatter.format(electricPrice)} đ/kWh")

        val waterPrice = (data["waterPrice"] as? Number)?.toLong() ?: 0L
        if (waterPrice > 0) addInfoRow(layout, "Tiền nước", "${formatter.format(waterPrice)} đ/m³")

        val wifiPrice = (data["wifiPrice"] as? Number)?.toLong() ?: 0L
        if (data["hasWifi"] == true) {
            addInfoRow(layout, "Tiền wifi", if (wifiPrice > 0) "${formatter.format(wifiPrice)} đ/tháng" else "Miễn phí")
        }

        (data["otherFees"] as? List<Map<String, Any>> ?: emptyList()).forEach { fee ->
            val label = fee["label"] as? String ?: ""
            val price = (fee["price"] as? Number)?.toLong() ?: 0L
            if (label.isNotEmpty()) addInfoRow(layout, label, "${formatter.format(price)} đ/tháng")
        }

        if (data["hasMotorbike"] == true) {
            val fee = (data["motorbikeFee"] as? Number)?.toLong() ?: 0L
            addInfoRow(layout, "Để xe máy", if (fee > 0) "${formatter.format(fee)} đ/xe/tháng" else "Miễn phí")
        }
        if (data["hasEBike"] == true) {
            val fee = (data["eBikeFee"] as? Number ?: data["ebikeFee"] as? Number)?.toLong() ?: 0L
            addInfoRow(layout, "Để xe đạp điện", if (fee > 0) "${formatter.format(fee)} đ/xe/tháng" else "Miễn phí")
        }
        if (data["hasBicycle"] == true) {
            val fee = (data["bicycleFee"] as? Number)?.toLong() ?: 0L
            addInfoRow(layout, "Để xe đạp", if (fee > 0) "${formatter.format(fee)} đ/xe/tháng" else "Miễn phí")
        }

        if (layout.childCount == 0) addInfoRow(layout, "Chi phí", "Chưa cập nhật")
    }

    private fun setupRulesInfo(data: Map<String, Any>) {
        val layout = findViewById<LinearLayout>(R.id.layoutRulesInfo)
        layout.removeAllViews()

        val kitchen = data["kitchen"] as? String ?: ""
        if (kitchen.isNotEmpty() && kitchen != "Không") addInfoRow(layout, "Phòng bếp", kitchen)

        val bathroom = data["bathroom"] as? String ?: ""
        if (bathroom.isNotEmpty()) addInfoRow(layout, "Phòng vệ sinh", bathroom)

        val curfew = data["curfew"] as? String ?: ""
        val curfewTime = data["curfewTime"] as? String ?: ""
        if (curfew.isNotEmpty()) {
            val text = if (curfew == "Tùy chọn" && curfewTime.isNotEmpty()) "Đóng cửa lúc $curfewTime" else curfew
            addInfoRow(layout, "Giờ giấc", text)
        }

        val pet = data["pet"] as? String ?: ""
        if (pet.isNotEmpty()) {
            val petName = data["petName"] as? String ?: ""
            val petCount = (data["petCount"] as? Number)?.toInt() ?: 0
            val petText = if (pet == "Cho nuôi") {
                val details = mutableListOf<String>()
                if (petName.isNotEmpty()) details.add(petName)
                if (petCount > 0) details.add("Số lượng: $petCount")
                if (details.isNotEmpty()) "Cho nuôi (${details.joinToString(" - ")})" else "Cho nuôi"
            } else {
                pet
            }
            addInfoRow(layout, "Thú cưng", petText)
        }

        if (layout.childCount == 0) addInfoRow(layout, "Quy định", "Chưa cập nhật")
    }

    private fun addInfoRow(parent: LinearLayout, label: String, value: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dpToPx(8), 0, dpToPx(8))
        }

        row.addView(TextView(this).apply {
            text = label
            textSize = 14f
            setTextColor(0xFF757575.toInt())
            layoutParams = LinearLayout.LayoutParams(dpToPx(130), ViewGroup.LayoutParams.WRAP_CONTENT)
        })

        row.addView(TextView(this).apply {
            text = value
            textSize = 14f
            setTextColor(0xFF212121.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })

        parent.addView(row)
        parent.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(1)
            )
            setBackgroundColor(0xFFEEEEEE.toInt())
        })
    }

    private fun setupAmenities(data: Map<String, Any>) {
        val layout = findViewById<LinearLayout>(R.id.layoutAmenities)
        layout.removeAllViews()

        fun aqty(key: String) = (data[key] as? Number)?.toInt() ?: 0
        fun alabel(name: String, qty: Int) = "$name : Số lượng ${qty.coerceAtLeast(1)}"
        val amenities = mutableListOf<String>()
        if (data["hasAirCon"] == true) amenities.add(alabel("Điều hòa", aqty("airConQty")))
        if (data["hasWaterHeater"] == true) amenities.add(alabel("Bình nóng lạnh", aqty("waterHeaterQty")))
        if (data["hasWasher"] == true) amenities.add(alabel("Máy giặt", aqty("washerQty")))
        if (data["hasDryingArea"] == true) amenities.add(alabel("Sân phơi đồ", aqty("dryingAreaQty")))
        if (data["hasWardrobe"] == true) amenities.add(alabel("Tủ quần áo", aqty("wardrobeQty")))
        if (data["hasBed"] == true) amenities.add(alabel("Giường ngủ", aqty("bedQty")))
        (data["furnitureItems"] as? List<Map<String, Any>> ?: emptyList()).forEach { item ->
            val name = item["name"] as? String ?: ""
            val qty = (item["qty"] as? Number)?.toInt() ?: 1
            if (name.isNotEmpty()) amenities.add("$name : Số lượng $qty")
        }
        (data["serviceItems"] as? List<Map<String, Any>> ?: emptyList()).forEach { item ->
            val name = item["name"] as? String ?: ""
            val price = (item["price"] as? Number)?.toLong() ?: 0L
            if (name.isNotEmpty()) {
                val priceText = if (price > 0) " - ${formatter.format(price)} đ/tháng" else ""
                amenities.add("$name$priceText")
            }
        }

        val hasAnyParking = data["hasMotorbike"] == true || data["hasEBike"] == true || data["hasBicycle"] == true

        if (amenities.isEmpty() && !hasAnyParking) {
            layout.addView(TextView(this).apply {
                text = "Không có thông tin tiện ích"
                textSize = 13f
                setTextColor(0xFF999999.toInt())
            })
            return
        }

        // Hiển thị tiện ích dạng 2 cột
        for (i in amenities.indices step 2) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dpToPx(5), 0, dpToPx(5))
            }
            row.addView(TextView(this).apply {
                text = "✓  ${amenities[i]}"
                textSize = 13f
                setTextColor(0xFF212121.toInt())
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            if (i + 1 < amenities.size) {
                row.addView(TextView(this).apply {
                    text = "✓  ${amenities[i + 1]}"
                    textSize = 13f
                    setTextColor(0xFF212121.toInt())
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                })
            } else {
                row.addView(View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                })
            }
            layout.addView(row)
        }
    }

    override fun onDestroy() {
        imagePageCallback?.let {
            findViewById<ViewPager2>(R.id.viewPagerImages).unregisterOnPageChangeCallback(it)
        }
        super.onDestroy()
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
            card.visibility = View.GONE
            return
        }
        card.visibility = View.VISIBLE

        if (createdAt > 0 || expiryDate > 0) {
            layoutExpiry.visibility = View.VISIBLE
            tvCreatedAt.text = if (createdAt > 0) sdf.format(java.util.Date(createdAt)) else "--"
            tvExpiry.text = if (expiryDate > 0) sdf.format(java.util.Date(expiryDate)) else "--"
        } else {
            layoutExpiry.visibility = View.GONE
        }

        if (timeSlots.isNotBlank()) {
            layoutSlots.visibility = View.VISIBLE
            tvSlots.text = timeSlots
        } else {
            layoutSlots.visibility = View.GONE
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
