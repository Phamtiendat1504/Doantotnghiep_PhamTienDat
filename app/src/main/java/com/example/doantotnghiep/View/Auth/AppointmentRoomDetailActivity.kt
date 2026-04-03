package com.example.doantotnghiep.View.Auth

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
import com.google.firebase.firestore.FirebaseFirestore
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
    private lateinit var btnBack: ImageView

    private val db = FirebaseFirestore.getInstance()
    private val formatter: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN"))
        symbols.groupingSeparator = '.'
        DecimalFormat("#,###", symbols)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appointment_room_detail)

        initViews()

        val roomId = intent.getStringExtra("roomId") ?: ""
        if (roomId.isNotEmpty()) {
            loadRoomData(roomId)
        }

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
        btnBack = findViewById(R.id.btnBack)
    }

    private fun loadRoomData(roomId: String) {
        db.collection("rooms").document(roomId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val data = doc.data ?: return@addOnSuccessListener
                    displayData(data)
                }
            }
    }

    private fun displayData(data: Map<String, Any>) {
        tvTitle.text = data["title"] as? String ?: "Chưa có tiêu đề"
        
        val price = (data["price"] as? Number)?.toLong() ?: 0L
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
    }

    private fun setupImageSlider(imageUrls: List<String>) {
        val images = if (imageUrls.isEmpty()) listOf("") else imageUrls
        viewPagerImages.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val img = ImageView(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    scaleType = ImageView.ScaleType.CENTER_CROP
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
        if (peopleCount > 0) addInfoRow("Sức chứa", "$peopleCount người")
        if (roomType.isNotEmpty()) addInfoRow("Loại phòng", roomType)
        if (genderPrefer.isNotEmpty()) addInfoRow("Ưu tiên", genderPrefer)
        if (kitchen.isNotEmpty()) addInfoRow("Bếp", kitchen)
        if (bathroom.isNotEmpty()) addInfoRow("Vệ sinh", bathroom)
        
        if (electricPrice > 0) addInfoRow("Tiền điện", "${formatter.format(electricPrice)} đ/kWh")
        if (waterPrice > 0) addInfoRow("Tiền nước", "${formatter.format(waterPrice)} đ/m³")
        if (wifiPrice > 0) addInfoRow("Tiền wifi", "${formatter.format(wifiPrice)} đ/tháng")
        
        if (deposit > 0) addInfoRow("Tiền cọc", "${formatter.format(deposit)} đ (${depositMonths} tháng)")
        
        if (curfew.isNotEmpty()) {
            val text = if (curfew == "Tùy chọn" && curfewTime.isNotEmpty()) "Khóa cửa lúc $curfewTime" else curfew
            addInfoRow("Giờ giấc", text)
        }
        
        if (pet.isNotEmpty()) {
            val petName = data["petName"] as? String ?: ""
            val petCount = (data["petCount"] as? Number)?.toInt() ?: 0
            val text = if (pet == "Cho nuôi" && petName.isNotEmpty()) "Cho nuôi: $petName ($petCount con)" else pet
            addInfoRow("Thú cưng", text)
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
            setTextColor(0xFF888888.toInt())
            layoutParams = LinearLayout.LayoutParams(dpToPx(120), LinearLayout.LayoutParams.WRAP_CONTENT)
        })
        row.addView(TextView(this).apply {
            text = value
            textSize = 14f
            setTextColor(0xFF333333.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        layoutRoomInfo.addView(row)
        
        // Thêm gạch ngang phân cách giống RoomDetailActivity
        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1))
            setBackgroundColor(0xFFF5F5F5.toInt())
        }
        layoutRoomInfo.addView(divider)
    }

    private fun setupAmenities(data: Map<String, Any>) {
        layoutAmenities.removeAllViews()
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
            layoutAmenities.addView(TextView(this).apply { text = "Không có thông tin tiện ích" })
            return
        }

        // Hiển thị 2 cột giống trang gốc
        for (i in amenities.indices step 2) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dpToPx(4), 0, dpToPx(4))
            }
            row.addView(TextView(this).apply {
                text = amenities[i]
                textSize = 14f
                setTextColor(0xFF2E7D32.toInt())
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            if (i + 1 < amenities.size) {
                row.addView(TextView(this).apply {
                    text = amenities[i + 1]
                    textSize = 14f
                    setTextColor(0xFF2E7D32.toInt())
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
            }
            layoutAmenities.addView(row)
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}