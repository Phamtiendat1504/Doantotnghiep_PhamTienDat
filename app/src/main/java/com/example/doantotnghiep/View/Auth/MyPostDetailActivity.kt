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
import com.example.doantotnghiep.ViewModel.MyPostDetailViewModel
import com.google.android.material.button.MaterialButton
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            if (success) {
                Toast.makeText(this, "\u0110\u00e3 c\u1eadp nh\u1eadt tr\u1ea1ng th\u00e1i \u0111\u00e3 cho thu\u00ea", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "C\u1eadp nh\u1eadt th\u1ea5t b\u1ea1i", Toast.LENGTH_SHORT).show()
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
        findViewById<TextView>(R.id.tvAddress).text = if (address.isNotEmpty()) {
            "\u0110\u1ecba ch\u1ec9: $address, $ward, $district"
        } else {
            "\u0110\u1ecba ch\u1ec9: $ward, $district"
        }

        val createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi", "VN"))
        findViewById<TextView>(R.id.tvPostDate).text = if (createdAt > 0L) {
            "Ng\u00e0y \u0111\u0103ng: ${sdf.format(Date(createdAt))}"
        } else {
            "Ng\u00e0y \u0111\u0103ng: --"
        }

        findViewById<TextView>(R.id.tvDescription).text =
            (d["description"] as? String)?.takeIf { it.isNotBlank() } ?: "Kh\u00f4ng c\u00f3 m\u00f4 t\u1ea3"

        setupImageSlider(d["imageUrls"] as? List<String> ?: emptyList())
        setupRoomInfo(d)
        setupAmenities(d)

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

        if (status == "approved" || status == "expired") {
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
            "expired" -> Triple("H\u1ebft h\u1ea1n", 0xFF757575.toInt(), 0x33757575)
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
        AlertDialog.Builder(this)
            .setTitle("X\u00e1c nh\u1eadn \u0111\u00e3 cho thu\u00ea")
            .setMessage(
                "B\u00e0i \u0111\u0103ng s\u1ebd \u0111\u01b0\u1ee3c \u1ea9n kh\u1ecfi trang t\u00ecm ki\u1ebfm sau khi b\u1ea1n x\u00e1c nh\u1eadn.\n\n" +
                    "C\u00e1c l\u1ecbch h\u1eb9n li\u00ean quan s\u1ebd \u0111\u01b0\u1ee3c c\u1eadp nh\u1eadt theo tr\u1ea1ng th\u00e1i ph\u00f2ng \u0111\u00e3 cho thu\u00ea."
            )
            .setPositiveButton("X\u00e1c nh\u1eadn") { _, _ ->
                viewModel.markAsRented(roomId)
            }
            .setNegativeButton("H\u1ee7y", null)
            .show()
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

        tvCount.text = "1/${images.size}"
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                tvCount.text = "${position + 1}/${images.size}"
            }
        })
    }

    private fun setupRoomInfo(data: Map<String, Any>) {
        val layout = findViewById<LinearLayout>(R.id.layoutRoomInfo)
        layout.removeAllViews()

        val area = (data["area"] as? Number)?.toInt() ?: 0
        if (area > 0) addInfoRow(layout, "Di\u1ec7n t\u00edch", "${area} m\u00b2")

        val peopleCount = (data["peopleCount"] as? Number)?.toInt() ?: 0
        if (peopleCount > 0) addInfoRow(layout, "S\u1ed1 ng\u01b0\u1eddi \u1edf", "$peopleCount ng\u01b0\u1eddi")

        val roomType = data["roomType"] as? String ?: ""
        if (roomType.isNotEmpty()) addInfoRow(layout, "Lo\u1ea1i ph\u00f2ng", roomType)

        val genderPrefer = data["genderPrefer"] as? String ?: ""
        if (genderPrefer.isNotEmpty()) addInfoRow(layout, "\u0110\u1ed1i t\u01b0\u1ee3ng thu\u00ea", genderPrefer)

        val depositMonths = (data["depositMonths"] as? Number)?.toInt() ?: 0
        if (depositMonths > 0) addInfoRow(layout, "\u0110\u1eb7t c\u1ecdc", "$depositMonths th\u00e1ng")

        val depositAmount = (data["depositAmount"] as? Number)?.toLong() ?: 0L
        if (depositAmount > 0) addInfoRow(layout, "S\u1ed1 ti\u1ec1n c\u1ecdc", "${formatter.format(depositAmount)} \u0111")

        val wifiPrice = (data["wifiPrice"] as? Number)?.toLong() ?: 0L
        if (data["hasWifi"] == true) {
            addInfoRow(layout, "Internet", if (wifiPrice > 0) "${formatter.format(wifiPrice)} \u0111/th\u00e1ng" else "Mi\u1ec5n ph\u00ed")
        }

        val electricPrice = (data["electricPrice"] as? Number)?.toLong() ?: 0L
        if (electricPrice > 0) addInfoRow(layout, "Ti\u1ec1n \u0111i\u1ec7n", "${formatter.format(electricPrice)} \u0111/kWh")

        val waterPrice = (data["waterPrice"] as? Number)?.toLong() ?: 0L
        if (waterPrice > 0) addInfoRow(layout, "Ti\u1ec1n n\u01b0\u1edbc", "${formatter.format(waterPrice)} \u0111/kh\u1ed1i")

        if (data["hasMotorbike"] == true) {
            val fee = (data["motorbikeFee"] as? Number)?.toLong() ?: 0L
            addInfoRow(layout, "\u0110\u1ec3 xe m\u00e1y", if (fee > 0) "${formatter.format(fee)} \u0111/xe/th\u00e1ng" else "Mi\u1ec5n ph\u00ed")
        }

        if (data["hasEBike"] == true) {
            val fee = (data["eBikeFee"] as? Number)?.toLong() ?: 0L
            addInfoRow(layout, "\u0110\u1ec3 xe \u0111\u1ea1p \u0111i\u1ec7n", if (fee > 0) "${formatter.format(fee)} \u0111/xe/th\u00e1ng" else "Mi\u1ec5n ph\u00ed")
        }

        if (data["hasBicycle"] == true) {
            val fee = (data["bicycleFee"] as? Number)?.toLong() ?: 0L
            addInfoRow(layout, "\u0110\u1ec3 xe \u0111\u1ea1p", if (fee > 0) "${formatter.format(fee)} \u0111/xe/th\u00e1ng" else "Mi\u1ec5n ph\u00ed")
        }

        val curfew = data["curfew"] as? String ?: ""
        val curfewTime = data["curfewTime"] as? String ?: ""
        if (curfew.isNotEmpty()) {
            val text = if (curfew == "T\u00f9y ch\u1ecdn" && curfewTime.isNotEmpty()) curfewTime else curfew
            addInfoRow(layout, "Gi\u1edd \u0111\u00f3ng c\u1eeda", text)
        }

        val pet = data["pet"] as? String ?: ""
        if (pet.isNotEmpty()) {
            val petName = data["petName"] as? String ?: ""
            val petCount = (data["petCount"] as? Number)?.toInt() ?: 0
            val petText = if (pet == "Cho nu\u00f4i") {
                buildString {
                    append("Cho nu\u00f4i")
                    if (petName.isNotEmpty()) append(": $petName")
                    if (petCount > 0) append(" (SL: $petCount con)")
                }
            } else {
                pet
            }
            addInfoRow(layout, "Th\u00fa c\u01b0ng", petText)
        }

        if (layout.childCount == 0) {
            addInfoRow(layout, "Th\u00f4ng tin", "Ch\u01b0a c\u1eadp nh\u1eadt")
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

        val amenities = mutableListOf<String>()
        if (data["hasAirCon"] == true) amenities.add("\u0110i\u1ec1u h\u00f2a")
        if (data["hasWaterHeater"] == true) amenities.add("M\u00e1y n\u01b0\u1edbc n\u00f3ng")
        if (data["hasWasher"] == true) amenities.add("M\u00e1y gi\u1eb7t")
        if (data["hasBed"] == true) amenities.add("Gi\u01b0\u1eddng ng\u1ee7")
        if (data["hasWardrobe"] == true) amenities.add("T\u1ee7 qu\u1ea7n \u00e1o")
        if (data["hasDryingArea"] == true) amenities.add("S\u00e2n ph\u01a1i \u0111\u1ed3")

        val kitchen = data["kitchen"] as? String ?: ""
        if (kitchen.isNotEmpty() && kitchen != "Kh\u00f4ng") amenities.add("B\u1ebfp $kitchen")

        val bathroom = data["bathroom"] as? String ?: ""
        if (bathroom.isNotEmpty()) amenities.add("WC $bathroom")

        if (amenities.isEmpty()) {
            layout.addView(TextView(this).apply {
                text = "Kh\u00f4ng c\u00f3 th\u00f4ng tin ti\u1ec7n \u00edch"
                textSize = 13f
                setTextColor(0xFF999999.toInt())
            })
            return
        }

        for (i in amenities.indices step 2) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dpToPx(5), 0, dpToPx(5))
            }

            row.addView(TextView(this).apply {
                text = amenities[i]
                textSize = 13f
                setTextColor(0xFF212121.toInt())
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })

            if (i + 1 < amenities.size) {
                row.addView(TextView(this).apply {
                    text = amenities[i + 1]
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

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
