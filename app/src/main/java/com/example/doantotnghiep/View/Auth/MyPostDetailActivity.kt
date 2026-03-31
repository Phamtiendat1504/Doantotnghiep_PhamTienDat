package com.example.doantotnghiep.View.Auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.doantotnghiep.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

class MyPostDetailActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
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

        loadRoomDetail()
    }

    override fun onResume() {
        super.onResume()
        if (roomId.isNotEmpty()) loadRoomDetail()
    }

    private fun loadRoomDetail() {
        db.collection("rooms").document(roomId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) { finish(); return@addOnSuccessListener }
                val d = doc.data ?: return@addOnSuccessListener

                val status = d["status"] as? String ?: "pending"
                val rejectReason = d["rejectReason"] as? String ?: ""

                // Badge trạng thái
                val tvStatusBadge = findViewById<TextView>(R.id.tvStatusBadge)
                when (status) {
                    "pending" -> {
                        tvStatusBadge.text = "⏳ Chờ duyệt"
                        tvStatusBadge.setTextColor(0xFFE65100.toInt())
                        tvStatusBadge.setBackgroundColor(0x33FF9800.toInt())
                    }
                    "approved" -> {
                        tvStatusBadge.text = "✓ Đã duyệt"
                        tvStatusBadge.setTextColor(0xFF2E7D32.toInt())
                        tvStatusBadge.setBackgroundColor(0x334CAF50.toInt())
                    }
                    "rejected" -> {
                        tvStatusBadge.text = "✗ Từ chối"
                        tvStatusBadge.setTextColor(0xFFD32F2F.toInt())
                        tvStatusBadge.setBackgroundColor(0x33F44336.toInt())
                    }
                }

                // Lý do từ chối
                val layoutReject = findViewById<LinearLayout>(R.id.layoutRejectReason)
                val tvRejectReason = findViewById<TextView>(R.id.tvRejectReason)
                if (status == "rejected" && rejectReason.isNotEmpty()) {
                    layoutReject.visibility = View.VISIBLE
                    tvRejectReason.text = "Lý do từ chối: $rejectReason"
                } else {
                    layoutReject.visibility = View.GONE
                }

                // Tiêu đề
                findViewById<TextView>(R.id.tvTitle).text = d["title"] as? String ?: "Chưa có tiêu đề"

                // Giá
                val price = d["price"] as? Long ?: 0
                findViewById<TextView>(R.id.tvPrice).text = "${formatter.format(price)} đ/tháng"

                // Địa chỉ
                val address = d["address"] as? String ?: ""
                val ward = d["ward"] as? String ?: ""
                val district = d["district"] as? String ?: ""
                findViewById<TextView>(R.id.tvAddress).text =
                    if (address.isNotEmpty()) "📍 $address, $ward, $district" else "📍 $ward, $district"

                // Ngày đăng
                val createdAt = d["createdAt"] as? Long ?: 0
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi", "VN"))
                findViewById<TextView>(R.id.tvPostDate).text = "📅 Ngày đăng: ${sdf.format(Date(createdAt))}"

                // Mô tả
                val desc = d["description"] as? String ?: ""
                findViewById<TextView>(R.id.tvDescription).text = if (desc.isNotEmpty()) desc else "Không có mô tả"

                // Ảnh
                val imageUrls = d["imageUrls"] as? List<String> ?: listOf()
                setupImageSlider(imageUrls)

                // Thông tin phòng
                setupRoomInfo(d)

                // Tiện ích
                setupAmenities(d)

                // Nút sửa (chỉ hiện khi bị từ chối)
                val btnEdit = findViewById<MaterialButton>(R.id.btnEditPost)
                if (status == "rejected") {
                    btnEdit.visibility = View.VISIBLE
                    btnEdit.setOnClickListener {
                        val intent = Intent(this, EditPostActivity::class.java)
                        intent.putExtra("roomId", roomId)
                        startActivity(intent)
                    }
                } else {
                    btnEdit.visibility = View.GONE
                }
            }
    }

    private fun setupImageSlider(imageUrls: List<String>) {
        val viewPager = findViewById<ViewPager2>(R.id.viewPagerImages)
        val tvCount = findViewById<TextView>(R.id.tvImageCount)
        val images = if (imageUrls.isEmpty()) listOf("") else imageUrls

        viewPager.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val img = ImageView(parent.context)
                img.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                img.scaleType = ImageView.ScaleType.CENTER_CROP
                img.setBackgroundColor(0xFFE0E0E0.toInt())
                return object : RecyclerView.ViewHolder(img) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val imgView = holder.itemView as ImageView
                if (images[position].isNotEmpty()) {
                    Glide.with(this@MyPostDetailActivity)
                        .load(images[position])
                        .centerCrop()
                        .into(imgView)

                    imgView.setOnClickListener {
                        val intent = Intent(this@MyPostDetailActivity, ImageViewerActivity::class.java)
                        intent.putExtra("imageUrl", images[position])
                        startActivity(intent)
                    }
                }
            }

            override fun getItemCount() = images.size
        }

        tvCount.text = "1/${images.size}"
        tvCount.background = android.graphics.drawable.GradientDrawable().apply {
            setColor(0x99000000.toInt())
            cornerRadius = dpToPx(10).toFloat()
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                tvCount.text = "${position + 1}/${images.size}"
            }
        })
    }

    private fun setupRoomInfo(data: Map<String, Any>) {
        val layout = findViewById<LinearLayout>(R.id.layoutRoomInfo)
        layout.removeAllViews()

        val area = (data["area"] as? Long)?.toInt() ?: (data["area"] as? Int ?: 0)
        val peopleCount = (data["peopleCount"] as? Long)?.toInt() ?: 0
        val roomType = data["roomType"] as? String ?: ""
        val deposit = data["depositAmount"] as? Long ?: 0
        val depositMonths = (data["depositMonths"] as? Long)?.toInt() ?: 0
        val kitchen = data["kitchen"] as? String ?: ""
        val bathroom = data["bathroom"] as? String ?: ""
        val genderPrefer = data["genderPrefer"] as? String ?: ""
        val curfew = data["curfew"] as? String ?: ""
        val curfewTime = data["curfewTime"] as? String ?: ""
        val electricPrice = data["electricPrice"] as? Long ?: 0
        val waterPrice = data["waterPrice"] as? Long ?: 0
        val wifiPrice = data["wifiPrice"] as? Long ?: 0
        val ownerName = data["ownerName"] as? String ?: ""
        val ownerPhone = data["ownerPhone"] as? String ?: ""
        val ownerGender = data["ownerGender"] as? String ?: ""

        if (area > 0) addInfoRow(layout, "Diện tích", "${area} m²")
        if (peopleCount > 0) addInfoRow(layout, "Số người/phòng", "$peopleCount người")
        if (roomType.isNotEmpty()) addInfoRow(layout, "Dạng phòng", roomType)
        if (genderPrefer.isNotEmpty()) addInfoRow(layout, "Giới tính ưu tiên", genderPrefer)
        if (kitchen.isNotEmpty()) addInfoRow(layout, "Bếp", kitchen)
        if (bathroom.isNotEmpty()) addInfoRow(layout, "Vệ sinh", bathroom)
        if (electricPrice > 0) addInfoRow(layout, "Tiền điện", "${formatter.format(electricPrice)} đ/kWh")
        if (waterPrice > 0) addInfoRow(layout, "Tiền nước", "${formatter.format(waterPrice)} đ/m³")
        if (wifiPrice > 0) addInfoRow(layout, "Tiền wifi", "${formatter.format(wifiPrice)} đ/tháng")
        if (deposit > 0) addInfoRow(layout, "Đặt cọc", "${formatter.format(deposit)} đ")
        if (depositMonths > 0) addInfoRow(layout, "Cọc trước", "$depositMonths tháng")
        if (curfew.isNotEmpty()) {
            val curfewText = if (curfew == "Tùy chọn" && curfewTime.isNotEmpty()) "Khóa cửa $curfewTime" else curfew
            addInfoRow(layout, "Giờ giấc", curfewText)
        }
        val pet = data["pet"] as? String ?: ""
        if (pet.isNotEmpty()) {
            val petName = data["petName"] as? String ?: ""
            val petCount = (data["petCount"] as? Long)?.toInt() ?: 0
            val petText = if (pet == "Cho nuôi" && petName.isNotEmpty())
                "Cho nuôi: $petName (tối đa $petCount)" else pet
            addInfoRow(layout, "Thú cưng", petText)
        }
        if (ownerName.isNotEmpty()) addInfoRow(layout, "Chủ trọ", ownerName)
        if (ownerPhone.isNotEmpty()) addInfoRow(layout, "SĐT chủ trọ", ownerPhone)
        if (ownerGender.isNotEmpty()) addInfoRow(layout, "Giới tính CT", ownerGender)
    }

    private fun addInfoRow(parent: LinearLayout, label: String, value: String) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.setPadding(0, dpToPx(6), 0, dpToPx(6))

        val tvLabel = TextView(this)
        tvLabel.text = label
        tvLabel.textSize = 13f
        tvLabel.setTextColor(0xFF999999.toInt())
        tvLabel.layoutParams = LinearLayout.LayoutParams(dpToPx(120), LinearLayout.LayoutParams.WRAP_CONTENT)
        row.addView(tvLabel)

        val tvValue = TextView(this)
        tvValue.text = value
        tvValue.textSize = 14f
        tvValue.setTextColor(0xFF333333.toInt())
        tvValue.setTypeface(tvValue.typeface, android.graphics.Typeface.BOLD)
        row.addView(tvValue)

        parent.addView(row)

        val divider = View(this)
        divider.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)
        )
        divider.setBackgroundColor(0xFFF5F5F5.toInt())
        parent.addView(divider)
    }

    private fun setupAmenities(data: Map<String, Any>) {
        val layout = findViewById<LinearLayout>(R.id.layoutAmenities)
        layout.removeAllViews()

        val amenities = mutableListOf<String>()
        if (data["hasWifi"] == true) amenities.add("✓ Wifi")
        if (data["hasAirCon"] == true) amenities.add("✓ Điều hòa")
        if (data["hasWaterHeater"] == true) amenities.add("✓ Nóng lạnh")
        if (data["hasWasher"] == true) amenities.add("✓ Máy giặt")
        if (data["hasDryingArea"] == true) amenities.add("✓ Chỗ phơi đồ")
        if (data["hasWardrobe"] == true) amenities.add("✓ Tủ quần áo")
        if (data["hasBed"] == true) amenities.add("✓ Giường")
        if (data["hasMotorbike"] == true) amenities.add("✓ Để xe máy")
        if (data["hasEBike"] == true) amenities.add("✓ Để xe đạp điện")
        if (data["hasBicycle"] == true) amenities.add("✓ Để xe đạp")

        if (amenities.isEmpty()) {
            val tv = TextView(this)
            tv.text = "Không có thông tin"
            tv.textSize = 13f
            tv.setTextColor(0xFF999999.toInt())
            layout.addView(tv)
            return
        }

        var i = 0
        while (i < amenities.size) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.setPadding(0, dpToPx(4), 0, dpToPx(4))

            val tv1 = TextView(this)
            tv1.text = amenities[i]
            tv1.textSize = 14f
            tv1.setTextColor(0xFF2E7D32.toInt())
            tv1.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            row.addView(tv1)

            if (i + 1 < amenities.size) {
                val tv2 = TextView(this)
                tv2.text = amenities[i + 1]
                tv2.textSize = 14f
                tv2.setTextColor(0xFF2E7D32.toInt())
                tv2.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                row.addView(tv2)
            }

            layout.addView(row)
            i += 2
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}