package com.example.doantotnghiep.View.Auth

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.doantotnghiep.R
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import android.content.Intent
import com.example.doantotnghiep.View.Auth.ImageViewerActivity

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
    private lateinit var layoutRoomGrid: LinearLayout
    private lateinit var cardRoomList: CardView
    private lateinit var btnToggleRoomList: LinearLayout
    private lateinit var imgArrow: ImageView
    private lateinit var btnBack: ImageView

    private val db = FirebaseFirestore.getInstance()
    private val formatter: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN"))
        symbols.groupingSeparator = '.'
        DecimalFormat("#,###", symbols)
    }
    private var allRoomDocs = mutableListOf<DocumentSnapshot>()
    private var currentRoomIndex = 0
    private var isRoomListExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_detail)
        // Ánh xạ view
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
        layoutRoomGrid = findViewById(R.id.layoutRoomGrid)
        cardRoomList = findViewById(R.id.cardRoomList)
        btnToggleRoomList = findViewById(R.id.btnToggleRoomList)
        imgArrow = findViewById(R.id.imgArrow)
        btnBack = findViewById(R.id.btnBack)
        btnBack.setOnClickListener { finish() }
        // Toggle danh sách phòng
        btnToggleRoomList.setOnClickListener {
            isRoomListExpanded = !isRoomListExpanded
            layoutRoomGrid.visibility = if (isRoomListExpanded) View.VISIBLE else View.GONE
            imgArrow.rotation = if (isRoomListExpanded) 270f else 90f
        }
        // Lấy userId từ intent
        val userId = intent.getStringExtra("userId") ?: ""
        val roomId = intent.getStringExtra("roomId") ?: ""
        if (userId.isNotEmpty()) {
            loadOwnerRooms(userId, roomId)
        } else if (roomId.isNotEmpty()) {
            loadSingleRoom(roomId)
        }
    }
    // ═══ LOAD TẤT CẢ PHÒNG CỦA CHỦ TRỌ ═══
    private fun loadOwnerRooms(userId: String, selectedRoomId: String) {
        db.collection("rooms")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener { documents ->
                allRoomDocs = documents.documents.toMutableList()
                // Sắp xếp theo thời gian đăng
                allRoomDocs.sortBy { it.getLong("createdAt") ?: 0 }
                if (allRoomDocs.isEmpty()) {
                    finish()
                    return@addOnSuccessListener
                }
                // Tìm phòng được chọn hoặc lấy phòng đầu tiên
                currentRoomIndex = if (selectedRoomId.isNotEmpty()) {
                    allRoomDocs.indexOfFirst { it.id == selectedRoomId }.takeIf { it >= 0 } ?: 0
                } else 0
                // Hiển thị thông tin phòng
                displayRoomDetail(currentRoomIndex)
                // Hiển thị danh sách phòng nếu có nhiều hơn 1
                if (allRoomDocs.size > 1) {
                    cardRoomList.visibility = View.VISIBLE
                    displayRoomGrid()
                }
                // Load lịch hẹn đã đặt
                val roomId = allRoomDocs[currentRoomIndex].id
                loadBookedSlots(roomId)
            }
    }
    // ═══ LOAD 1 PHÒNG ═══
    private fun loadSingleRoom(roomId: String) {
        db.collection("rooms").document(roomId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    allRoomDocs = mutableListOf(doc)
                    currentRoomIndex = 0
                    displayRoomDetail(0)
                }
            }
    }
    // ═══ HIỂN THỊ CHI TIẾT PHÒNG ═══
    private fun displayRoomDetail(index: Int) {
        val doc = allRoomDocs[index]
        val data = doc.data ?: return
        val roomNum = String.format("%02d", index + 1)
        tvRoomNumber.text = "Phòng $roomNum"
        // Tiêu đề
        tvTitle.text = data["title"] as? String ?: "Chưa có tiêu đề"
        // Giá
        val price = data["price"] as? Long ?: 0
        tvPrice.text = "${formatter.format(price)} đ/tháng"
        // Địa chỉ
        val address = data["address"] as? String ?: ""
        val ward = data["ward"] as? String ?: ""
        val district = data["district"] as? String ?: ""
        tvAddress.text = if (address.isNotEmpty()) "$address, $ward, $district" else "$ward, $district"
        // Mô tả
        val desc = data["description"] as? String ?: ""
        tvDescription.text = if (desc.isNotEmpty()) desc else "Không có mô tả"
        // Ảnh
        val imageUrls = data["imageUrls"] as? List<String> ?: listOf()
        setupImageSlider(imageUrls)
        // Thông tin phòng
        setupRoomInfo(data)
        // Tiện ích
        setupAmenities(data)
        // Thông tin chủ trọ
        setupOwnerInfo(data)
        // Cập nhật highlight trong grid
        if (allRoomDocs.size > 1) {
            displayRoomGrid()
        }
        // Load lịch hẹn đã đặt theo đúng phòng đang xem
        val currentRoomId = allRoomDocs[index].id
        loadBookedSlots(currentRoomId)
        // Kiểm tra role để hiện/ẩn nút lưu
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
            val role = userDoc.getString("role") ?: "tenant"
            val btnSave = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSavePost)

            if (role == "landlord" || role == "admin") {
                btnSave.visibility = View.GONE
            } else {
                btnSave.visibility = View.VISIBLE
                // Kiểm tra đã lưu chưa
                val roomId = allRoomDocs[index].id
                db.collection("savedPosts").document("${uid}_${roomId}").get()
                    .addOnSuccessListener { savedDoc ->
                        if (savedDoc.exists()) {
                            btnSave.text = "✓ Đã lưu"
                            btnSave.setBackgroundColor(0xFF999999.toInt())
                        } else {
                            btnSave.text = "Lưu bài viết"
                            btnSave.setBackgroundColor(0xFF1976D2.toInt())
                        }
                    }
                btnSave.setOnClickListener {
                    val roomData = allRoomDocs[index].data ?: return@setOnClickListener
                    val savedPost = hashMapOf(
                        "userId" to uid,
                        "roomId" to roomId,
                        "ownerId" to (roomData["userId"] as? String ?: ""),
                        "title" to (roomData["title"] as? String ?: ""),
                        "price" to (roomData["price"] as? Long ?: 0),
                        "address" to (roomData["address"] as? String ?: ""),
                        "ward" to (roomData["ward"] as? String ?: ""),
                        "district" to (roomData["district"] as? String ?: ""),
                        "imageUrl" to ((roomData["imageUrls"] as? List<String>)?.firstOrNull() ?: ""),
                        "savedAt" to System.currentTimeMillis()
                    )
                    db.collection("savedPosts").document("${uid}_${roomId}")
                        .get()
                        .addOnSuccessListener { doc ->
                            if (doc.exists()) {
                                // Bỏ lưu
                                db.collection("savedPosts").document("${uid}_${roomId}").delete()
                                    .addOnSuccessListener {
                                        btnSave.text = "Lưu bài viết"
                                        btnSave.setBackgroundColor(0xFF1976D2.toInt())
                                        android.widget.Toast.makeText(this, "Đã bỏ lưu", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                // Lưu
                                db.collection("savedPosts").document("${uid}_${roomId}").set(savedPost)
                                    .addOnSuccessListener {
                                        btnSave.text = "✓ Đã lưu"
                                        btnSave.setBackgroundColor(0xFF999999.toInt())
                                        android.widget.Toast.makeText(this, "Đã lưu bài viết", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                }
                // Nút đặt lịch hẹn
                val btnBooking = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBooking)
                if (role == "landlord" || role == "admin") {
                    btnBooking.visibility = View.GONE
                } else {
                    btnBooking.visibility = View.VISIBLE
                    btnBooking.setOnClickListener {
                        val roomData = allRoomDocs[currentRoomIndex].data ?: return@setOnClickListener
                        val intent = Intent(this, BookingActivity::class.java)
                        intent.putExtra("roomId", allRoomDocs[currentRoomIndex].id)
                        intent.putExtra("landlordId", roomData["userId"] as? String ?: "")
                        intent.putExtra("roomTitle", roomData["title"] as? String ?: "")
                        val addr = roomData["address"] as? String ?: ""
                        val ward = roomData["ward"] as? String ?: ""
                        val district = roomData["district"] as? String ?: ""
                        intent.putExtra("roomAddress", if (addr.isNotEmpty()) "$addr, $ward, $district" else "$ward, $district")
                        intent.putExtra("roomPrice", roomData["price"] as? Long ?: 0)
                        startActivity(intent)
                    }
                }
            }
        }
    }
    // ═══ SLIDER ẢNH ═══
    private fun setupImageSlider(imageUrls: List<String>) {
        val images = if (imageUrls.isEmpty()) listOf("") else imageUrls

        viewPagerImages.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
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
                    Glide.with(this@RoomDetailActivity)
                        .load(images[position])
                        .centerCrop()
                        .into(imgView)
                    imgView.setOnClickListener {
                        val intent = Intent(this@RoomDetailActivity, ImageViewerActivity::class.java)
                        intent.putExtra("imageUrl", images[position])
                        startActivity(intent)
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
    // ═══ THÔNG TIN PHÒNG ═══
    private fun setupRoomInfo(data: Map<String, Any>) {
        layoutRoomInfo.removeAllViews()

        val area = (data["area"] as? Long)?.toInt() ?: (data["area"] as? Int ?: 0)
        val peopleCount = (data["peopleCount"] as? Long)?.toInt() ?: (data["peopleCount"] as? Int ?: 0)
        val roomType = data["roomType"] as? String ?: ""
        val deposit = data["depositAmount"] as? Long ?: 0
        val depositMonths = (data["depositMonths"] as? Long)?.toInt() ?: (data["depositMonths"] as? Int ?: 0)
        val kitchen = data["kitchen"] as? String ?: ""
        val bathroom = data["bathroom"] as? String ?: ""
        val genderPrefer = data["genderPrefer"] as? String ?: ""
        val curfew = data["curfew"] as? String ?: ""
        val curfewTime = data["curfewTime"] as? String ?: ""
        val electricPrice = data["electricPrice"] as? Long ?: 0
        val waterPrice = data["waterPrice"] as? Long ?: 0
        val wifiPrice = data["wifiPrice"] as? Long ?: 0

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
            val curfewText = if (curfew == "Tùy chọn" && curfewTime.isNotEmpty()) "Khóa cửa $curfewTime" else curfew
            addInfoRow("Giờ giấc", curfewText)
        }
        // Thú cưng
        val pet = data["pet"] as? String ?: ""
        if (pet.isNotEmpty()) {
            val petName = data["petName"] as? String ?: ""
            val petCount = (data["petCount"] as? Long)?.toInt() ?: 0
            val petText = if (pet == "Cho nuôi" && petName.isNotEmpty())
                "Cho nuôi: $petName (tối đa $petCount)"
            else pet
            addInfoRow("Thú cưng", petText)
        }
    }
    private fun addInfoRow(label: String, value: String) {
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

        layoutRoomInfo.addView(row)

        // Đường kẻ
        val divider = View(this)
        divider.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)
        )
        divider.setBackgroundColor(0xFFF5F5F5.toInt())
        layoutRoomInfo.addView(divider)
    }
    // ═══ TIỆN ÍCH ═══
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
            val tv = TextView(this)
            tv.text = "Không có thông tin"
            tv.textSize = 13f
            tv.setTextColor(0xFF999999.toInt())
            layoutAmenities.addView(tv)
            return
        }

        // Hiển thị 2 cột
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

            layoutAmenities.addView(row)
            i += 2
        }
    }
    // ═══ THÔNG TIN CHỦ TRỌ ═══
    private fun setupOwnerInfo(data: Map<String, Any>) {
        layoutOwnerInfo.removeAllViews()

        val ownerName = data["ownerName"] as? String ?: ""
        val ownerPhone = data["ownerPhone"] as? String ?: ""
        val ownerGender = data["ownerGender"] as? String ?: ""

        if (ownerName.isNotEmpty()) addOwnerRow("Họ tên", ownerName)
        if (ownerPhone.isNotEmpty()) addOwnerRow("SĐT", ownerPhone)
        if (ownerGender.isNotEmpty()) addOwnerRow("Giới tính", ownerGender)
    }

    private fun addOwnerRow(label: String, value: String) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.setPadding(0, dpToPx(6), 0, dpToPx(6))
        val tvLabel = TextView(this)
        tvLabel.text = label
        tvLabel.textSize = 13f
        tvLabel.setTextColor(0xFF999999.toInt())
        tvLabel.layoutParams = LinearLayout.LayoutParams(dpToPx(80), LinearLayout.LayoutParams.WRAP_CONTENT)
        row.addView(tvLabel)
        val tvValue = TextView(this)
        tvValue.text = value
        tvValue.textSize = 14f
        tvValue.setTextColor(0xFF333333.toInt())
        tvValue.setTypeface(tvValue.typeface, android.graphics.Typeface.BOLD)
        row.addView(tvValue)

        layoutOwnerInfo.addView(row)
    }
    // ═══ GRID DANH SÁCH PHÒNG ═══
    private fun displayRoomGrid() {
        layoutRoomGrid.removeAllViews()
        var i = 0
        while (i < allRoomDocs.size) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            // 4 cột mỗi hàng
            for (col in 0..3) {
                val idx = i + col
                if (idx < allRoomDocs.size) {
                    val roomCard = createRoomGridItem(idx)
                    val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    params.marginEnd = dpToPx(4)
                    params.bottomMargin = dpToPx(4)
                    roomCard.layoutParams = params
                    row.addView(roomCard)
                } else {
                    val empty = View(this)
                    empty.layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
                    row.addView(empty)
                }
            }
            layoutRoomGrid.addView(row)
            i += 4
        }
    }
    private fun createRoomGridItem(index: Int): CardView {
        val doc = allRoomDocs[index]
        val data = doc.data ?: mapOf()
        val price = data["price"] as? Long ?: 0
        val roomNum = String.format("%02d", index + 1)
        val isSelected = index == currentRoomIndex

        val card = CardView(this)
        card.radius = dpToPx(8).toFloat()
        card.cardElevation = dpToPx(2).toFloat()

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = android.view.Gravity.CENTER
        layout.setPadding(dpToPx(8), dpToPx(10), dpToPx(8), dpToPx(10))

        if (isSelected) {
            card.setCardBackgroundColor(0xFF1976D2.toInt())
        } else {
            card.setCardBackgroundColor(0xFFE0F2F1.toInt())
        }

        // Số phòng
        val tvNum = TextView(this)
        tvNum.text = roomNum
        tvNum.textSize = 16f
        tvNum.setTypeface(tvNum.typeface, android.graphics.Typeface.BOLD)
        tvNum.setTextColor(if (isSelected) 0xFFFFFFFF.toInt() else 0xFF333333.toInt())
        tvNum.gravity = android.view.Gravity.CENTER
        layout.addView(tvNum)

        // Giá
        val tvPrice = TextView(this)
        tvPrice.text = formatter.format(price)
        tvPrice.textSize = 11f
        tvPrice.setTextColor(if (isSelected) 0xFFB3DEFF.toInt() else 0xFF666666.toInt())
        tvPrice.gravity = android.view.Gravity.CENTER
        tvPrice.setPadding(0, dpToPx(2), 0, 0)
        layout.addView(tvPrice)
        card.addView(layout)
        // Bấm chuyển phòng
        card.setOnClickListener {
            currentRoomIndex = index
            displayRoomDetail(index)

        }
        return card
    }
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
    private fun loadBookedSlots(roomId: String) {
        val layoutBooked = findViewById<LinearLayout>(R.id.layoutBookedSlots)
        val cardBooked = findViewById<androidx.cardview.widget.CardView>(R.id.cardBookedSlots)

        db.collection("appointments")
            .whereEqualTo("roomId", roomId)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { docs ->
                layoutBooked.removeAllViews()

                // Lấy thêm lịch đã xác nhận
                db.collection("appointments")
                    .whereEqualTo("roomId", roomId)
                    .whereEqualTo("status", "confirmed")
                    .get()
                    .addOnSuccessListener { confirmedDocs ->
                        val allDocs = docs.documents + confirmedDocs.documents

                        if (allDocs.isEmpty()) {
                            cardBooked.visibility = View.GONE
                            return@addOnSuccessListener
                        }

                        cardBooked.visibility = View.VISIBLE

                        // Sắp xếp theo ngày
                        val sorted = allDocs.sortedBy { it.getString("date") ?: "" }

                        for (doc in sorted) {
                            val date = doc.getString("dateDisplay") ?: doc.getString("date") ?: ""
                            val time = doc.getString("time") ?: ""

                            val tv = android.widget.TextView(this)
                            tv.text = "📅 $date  —  ⏰ $time  (Đã có người hẹn)"
                            tv.textSize = 13f
                            tv.setTextColor(0xFFE65100.toInt())
                            tv.setPadding(0, dpToPx(4), 0, dpToPx(4))
                            layoutBooked.addView(tv)

                            // Đường kẻ
                            val divider = View(this)
                            divider.layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)
                            )
                            divider.setBackgroundColor(0xFFF5F5F5.toInt())
                            layoutBooked.addView(divider)
                        }
                    }
            }
    }
}