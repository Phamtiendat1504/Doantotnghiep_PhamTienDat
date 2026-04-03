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
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.example.doantotnghiep.Utils.MessageUtils

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

    private val db = FirebaseFirestore.getInstance()
    private val formatter: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN"))
        symbols.groupingSeparator = '.'
        DecimalFormat("#,###", symbols)
    }
    private var allRoomDocs = mutableListOf<DocumentSnapshot>()
    private var currentRoomIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_detail)

        initViews()

        btnBack.setOnClickListener { finish() }

        val userId = intent.getStringExtra("userId") ?: ""
        val roomId = intent.getStringExtra("roomId") ?: ""

        if (userId.isNotEmpty()) {
            loadOwnerRooms(userId, roomId)
        } else if (roomId.isNotEmpty()) {
            loadSingleRoom(roomId)
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
    }

    private fun loadOwnerRooms(userId: String, selectedRoomId: String) {
        db.collection("rooms")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener { documents ->
                allRoomDocs = documents.documents.toMutableList()
                allRoomDocs.sortBy { it.getLong("createdAt") ?: 0 }
                if (allRoomDocs.isEmpty()) {
                    finish()
                    return@addOnSuccessListener
                }
                currentRoomIndex = if (selectedRoomId.isNotEmpty()) {
                    allRoomDocs.indexOfFirst { it.id == selectedRoomId }.takeIf { it >= 0 } ?: 0
                } else 0
                
                displayRoomDetail(currentRoomIndex)
                
            }
    }

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

    private fun displayRoomDetail(index: Int) {
        // BUG FIX #2: Check bounds before accessing
        if (index < 0 || index >= allRoomDocs.size) {
            finish()
            return
        }
        
        val doc = allRoomDocs[index]
        val data = doc.data ?: return
        val roomId = doc.id
        
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
        
        loadBookedSlots(roomId)
        setupActionButtons(roomId, data)
    }

    private fun setupActionButtons(roomId: String, roomData: Map<String, Any>) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
            val role = userDoc.getString("role") ?: "tenant"
            val btnSave = findViewById<MaterialButton>(R.id.btnSavePost)
            val btnBooking = findViewById<MaterialButton>(R.id.btnBooking)

            if (role == "landlord" || role == "admin") {
                btnSave.visibility = View.GONE
                btnBooking.visibility = View.GONE
            } else {
                btnSave.visibility = View.VISIBLE
                btnBooking.visibility = View.VISIBLE
                
                // Check saved status
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
                    toggleSavePost(uid, roomId, roomData, btnSave)
                }

                btnBooking.setOnClickListener {
                    val intent = Intent(this, BookingActivity::class.java)
                    intent.putExtra("roomId", roomId)
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

    private fun toggleSavePost(uid: String, roomId: String, roomData: Map<String, Any>, btnSave: MaterialButton) {
        val docRef = db.collection("savedPosts").document("${uid}_${roomId}")
        docRef.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                docRef.delete().addOnSuccessListener {
                    btnSave.text = "Lưu bài viết"
                    btnSave.setBackgroundColor(0xFF1976D2.toInt())
                    MessageUtils.showSuccessDialog(this, "Đã bỏ lưu", "Bài đăng đã được xóa khỏi danh sách yêu thích của bạn.")
                }
            } else {
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
                docRef.set(savedPost).addOnSuccessListener {
                    btnSave.text = "✓ Đã lưu"
                    btnSave.setBackgroundColor(0xFF999999.toInt())
                    MessageUtils.showSuccessDialog(this, "Đã lưu", "Bài đăng đã được thêm vào danh sách yêu thích của bạn.")
                }
            }
        }
    }

    private fun loadBookedSlots(roomId: String) {
        val layoutBooked = findViewById<LinearLayout>(R.id.layoutBookedSlots) ?: return
        val cardBooked = findViewById<CardView>(R.id.cardBookedSlots) ?: return

        layoutBooked.removeAllViews()
        cardBooked.visibility = View.GONE

        if (roomId.isEmpty()) return

        db.collection("appointments")
            .whereEqualTo("roomId", roomId)
            .whereIn("status", listOf("pending", "confirmed"))
            .get()
            .addOnSuccessListener { querySnapshot ->
                // BUG FIX #3: Check bounds before accessing array
                if (allRoomDocs.isEmpty() || currentRoomIndex >= allRoomDocs.size) {
                    return@addOnSuccessListener
                }
                if (allRoomDocs[currentRoomIndex].id != roomId) return@addOnSuccessListener

                if (querySnapshot.isEmpty) {
                    cardBooked.visibility = View.GONE
                    return@addOnSuccessListener
                }

                cardBooked.visibility = View.VISIBLE
                val sorted = querySnapshot.documents.sortedBy { it.getString("date") ?: "" }

                for (doc in sorted) {
                    val date = doc.getString("dateDisplay") ?: doc.getString("date") ?: ""
                    val time = doc.getString("time") ?: ""

                    val tv = TextView(this).apply {
                        text = "📅 $date  —  ⏰ $time  (Đã có người hẹn)"
                        textSize = 13f
                        setTextColor(0xFFE65100.toInt())
                        setPadding(0, dpToPx(4), 0, dpToPx(4))
                    }
                    layoutBooked.addView(tv)

                    val divider = View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1))
                        setBackgroundColor(0xFFF5F5F5.toInt())
                    }
                    layoutBooked.addView(divider)
                }
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
                // Hiển thị icon điện thoại + số, màu xanh, gạch chân để người dùng biết có thể bấm
                text = "📞 $value"
                setTextColor(0xFF1565C0.toInt())
                paintFlags = paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_DIAL,
                        android.net.Uri.parse("tel:$value")
                    )
                    startActivity(intent)
                }
            } else {
                text = value
                setTextColor(0xFF333333.toInt())
            }
        })
        layoutOwnerInfo.addView(row)
    }


    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
