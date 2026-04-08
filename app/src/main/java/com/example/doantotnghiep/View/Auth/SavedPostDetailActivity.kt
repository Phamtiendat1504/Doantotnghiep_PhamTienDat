package com.example.doantotnghiep.View.Auth

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.example.doantotnghiep.Utils.MessageUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import androidx.activity.viewModels
import com.example.doantotnghiep.R
import com.example.doantotnghiep.ViewModel.SavedPostDetailViewModel
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class SavedPostDetailActivity : AppCompatActivity() {

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
    private lateinit var btnRemoveSaved: LinearLayout
    private lateinit var progressBar: ProgressBar

    private val viewModel: SavedPostDetailViewModel by viewModels()
    private val formatter: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN"))
        symbols.groupingSeparator = '.'
        DecimalFormat("#,###", symbols)
    }

    private var roomId = ""
    private var savedDocId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_post_detail)

        initViews()

        btnBack.setOnClickListener { finish() }

        roomId = intent.getStringExtra("roomId") ?: ""
        savedDocId = intent.getStringExtra("savedDocId") ?: ""

        observeViewModel()

        if (roomId.isNotEmpty()) {
            viewModel.loadRoomDetail(roomId)
        }

        btnRemoveSaved.setOnClickListener { confirmRemoveSavedPost() }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.roomData.observe(this) { data ->
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
        }

        viewModel.deleteResult.observe(this) { success ->
            if (success) {
                MessageUtils.showSuccessDialog(this, "Thành công", "Đã bỏ lưu bài viết") { finish() }
            }
        }

        viewModel.errorMessage.observe(this) { msg ->
            if (!msg.isNullOrEmpty()) {
                MessageUtils.showErrorDialog(this, "Lỗi", msg)
                if (viewModel.roomData.value == null) finish()
            }
        }
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
        btnRemoveSaved = findViewById(R.id.btnRemoveSaved)
        progressBar = findViewById(R.id.progressBar)
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
                    Glide.with(this@SavedPostDetailActivity).load(images[position]).centerCrop().into(imgView)
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
        if (peopleCount > 0) addInfoRow("Số người/phòng", "$peopleCount người")
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
            val text = if (curfew == "Tùy chọn" && curfewTime.isNotEmpty()) "Khóa cửa $curfewTime" else curfew
            addInfoRow("Giờ giấc", text)
        }

        val pet = data["pet"] as? String ?: ""
        if (pet.isNotEmpty()) {
            val petName = data["petName"] as? String ?: ""
            val petCount = (data["petCount"] as? Number)?.toInt() ?: 0
            val text = if (pet == "Cho nuôi" && petName.isNotEmpty()) "Cho nuôi: $petName (tối đa $petCount)" else pet
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
            layoutAmenities.addView(TextView(this).apply {
                text = "Không có thông tin"
                textSize = 13f
                setTextColor(0xFF999999.toInt())
            })
            return
        }

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

    private fun setupOwnerInfo(data: Map<String, Any>) {
        layoutOwnerInfo.removeAllViews()
        val name = data["ownerName"] as? String ?: ""
        val phone = data["ownerPhone"] as? String ?: ""
        val gender = data["ownerGender"] as? String ?: ""

        if (name.isNotEmpty()) addOwnerRow("Họ tên", name)
        if (phone.isNotEmpty()) addOwnerRow("SĐT", phone)
        if (gender.isNotEmpty()) addOwnerRow("Giới tính", gender)
    }

    private fun addOwnerRow(label: String, value: String) {
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
            text = value
            textSize = 14f
            setTextColor(0xFF333333.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
        })

        layoutOwnerInfo.addView(row)
    }

    private fun confirmRemoveSavedPost() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Bỏ lưu bài viết")
            .setMessage("Bạn có chắc chắn muốn bỏ lưu bài viết này?")
            .setPositiveButton("Bỏ lưu") { _, _ -> viewModel.deleteSavedPost(savedDocId) }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}

