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
import com.example.doantotnghiep.Utils.MessageUtils
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

                // 1. Badge trạng thái bài đăng
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

                // 2. Lý do từ chối (nếu có)
                val layoutReject = findViewById<LinearLayout>(R.id.layoutRejectReason)
                val tvRejectReason = findViewById<TextView>(R.id.tvRejectReason)
                if (status == "rejected" && rejectReason.isNotEmpty()) {
                    layoutReject.visibility = View.VISIBLE
                    tvRejectReason.text = "Lý do từ chối: $rejectReason"
                } else {
                    layoutReject.visibility = View.GONE
                }

                // 3. Thông tin cơ bản
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

                // 4. Setup Slider và các phần chi tiết
                setupImageSlider(d["imageUrls"] as? List<String> ?: listOf())
                setupRoomInfo(d)
                setupAmenities(d)

                // 5. Nút sửa bài (chỉ hiện khi bị từ chối)
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
                    Glide.with(this@MyPostDetailActivity).load(images[position]).centerCrop().into(imgView)
                }
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

        // Diện tích & Sức chứa
        val area = (data["area"] as? Number)?.toInt() ?: 0
        if (area > 0) addInfoRow(layout, "Diện tích", "${area} m²")
        
        val peopleCount = (data["peopleCount"] as? Number)?.toInt() ?: 0
        if (peopleCount > 0) addInfoRow(layout, "Sức chứa", "$peopleCount người")

        // Loại phòng & Đối tượng
        val roomType = data["roomType"] as? String ?: ""
        if (roomType.isNotEmpty()) addInfoRow(layout, "Loại phòng", roomType)

        val genderPrefer = data["genderPrefer"] as? String ?: ""
        if (genderPrefer.isNotEmpty()) addInfoRow(layout, "Đối tượng thuê", genderPrefer)

        // Tiền cọc
        val depositMonths = (data["depositMonths"] as? Number)?.toInt() ?: 0
        if (depositMonths > 0) addInfoRow(layout, "Đặt cọc", "$depositMonths tháng")
        
        val depositAmount = (data["depositAmount"] as? Number)?.toLong() ?: 0L
        if (depositAmount > 0) addInfoRow(layout, "Số tiền cọc", "${formatter.format(depositAmount)} đ")

        // Chi phí dịch vụ
        val wifiPrice = (data["wifiPrice"] as? Number)?.toLong() ?: 0L
        if (data["hasWifi"] == true) addInfoRow(layout, "Internet", if (wifiPrice > 0) "${formatter.format(wifiPrice)} đ/tháng" else "Miễn phí")

        val electricPrice = (data["electricPrice"] as? Number)?.toLong() ?: 0L
        if (electricPrice > 0) addInfoRow(layout, "Tiền điện", "${formatter.format(electricPrice)} đ/kWh")

        val waterPrice = (data["waterPrice"] as? Number)?.toLong() ?: 0L
        if (waterPrice > 0) addInfoRow(layout, "Tiền nước", "${formatter.format(waterPrice)} đ/khối")

        // Chỗ để xe
        if (data["hasMotorbike"] == true) {
            val fee = (data["motorbikeFee"] as? Number)?.toLong() ?: 0L
            addInfoRow(layout, "Xe máy", if (fee > 0) "${formatter.format(fee)} đ/xe" else "Miễn phí")
        }

        // Giờ giấc & Thú cưng
        val curfew = data["curfew"] as? String ?: ""
        val curfewTime = data["curfewTime"] as? String ?: ""
        if (curfew.isNotEmpty()) addInfoRow(layout, "Giờ ra vào", if (curfew == "Tùy chọn") curfewTime else curfew)

        val pet = data["pet"] as? String ?: ""
        if (pet.isNotEmpty()) {
            val petName = data["petName"] as? String ?: ""
            addInfoRow(layout, "Thú cưng", if (pet == "Cho nuôi" && petName.isNotEmpty()) "Cho nuôi ($petName)" else pet)
        }
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
            layoutParams = LinearLayout.LayoutParams(dpToPx(130), -2)
        })
        row.addView(TextView(this).apply {
            text = value
            textSize = 14f
            setTextColor(0xFF212121.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        })
        parent.addView(row)
        parent.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, dpToPx(1))
            setBackgroundColor(0xFFEEEEEE.toInt())
        })
    }

    private fun setupAmenities(data: Map<String, Any>) {
        val layout = findViewById<LinearLayout>(R.id.layoutAmenities)
        layout.removeAllViews()
        val ams = mutableListOf<String>()
        
        if (data["hasAirCon"] == true) ams.add("❄️ Điều hòa")
        if (data["hasWaterHeater"] == true) ams.add("🔥 Bình nóng lạnh")
        if (data["hasWasher"] == true) ams.add("🧺 Máy giặt")
        if (data["hasBed"] == true) ams.add("🛏️ Giường ngủ")
        if (data["hasWardrobe"] == true) ams.add("👗 Tủ quần áo")
        if (data["hasDryingArea"] == true) ams.add("☀️ Sân phơi")
        
        val kitchen = data["kitchen"] as? String ?: ""
        if (kitchen.isNotEmpty() && kitchen != "Không") ams.add("🍳 Bếp: $kitchen")
        
        val bathroom = data["bathroom"] as? String ?: ""
        if (bathroom.isNotEmpty()) ams.add("🚿 WC: $bathroom")

        if (ams.isEmpty()) {
            layout.addView(TextView(this).apply { text = "Không có thông tin tiện ích"; setTextColor(0xFF999999.toInt()) })
            return
        }

        // Hiển thị dạng lưới 2 cột
        for (i in ams.indices step 2) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dpToPx(4), 0, dpToPx(4))
            }
            row.addView(TextView(this).apply {
                text = ams[i]
                textSize = 14f
                setTextColor(0xFF424242.toInt())
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            })
            if (i + 1 < ams.size) {
                row.addView(TextView(this).apply {
                    text = ams[i + 1]
                    textSize = 14f
                    setTextColor(0xFF424242.toInt())
                    layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                })
            } else {
                row.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(0, -2, 1f) })
            }
            layout.addView(row)
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
