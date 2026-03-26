package com.example.doantotnghiep.View.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.doantotnghiep.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var tvGreeting: TextView
    private lateinit var btnNotification: ImageView
    private lateinit var layoutFeaturedRooms: LinearLayout
    private lateinit var layoutNewRooms: LinearLayout
    private lateinit var tvNoFeatured: TextView
    private lateinit var tvNoNewRooms: TextView

    private val db = FirebaseFirestore.getInstance()
    private val formatter: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN"))
        symbols.groupingSeparator = '.'
        DecimalFormat("#,###", symbols)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvGreeting = view.findViewById(R.id.tvGreeting)
        btnNotification = view.findViewById(R.id.btnNotification)

        layoutFeaturedRooms = view.findViewById(R.id.layoutFeaturedRooms)
        layoutNewRooms = view.findViewById(R.id.layoutNewRooms)
        tvNoFeatured = view.findViewById(R.id.tvNoFeatured)
        tvNoNewRooms = view.findViewById(R.id.tvNoNewRooms)

        loadUserName()
        loadFeaturedRooms()
        loadNewRooms()

        // Bấm chuông thông báo
        btnNotification.setOnClickListener {
            Toast.makeText(requireContext(), "Chức năng đang phát triển", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserName() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    tvGreeting.text = document.getString("fullName") ?: "Bạn"
                }
            }
            .addOnFailureListener {
                tvGreeting.text = "Bạn"
            }
    }

    private fun loadFeaturedRooms() {
        db.collection("rooms")
            .whereEqualTo("isFeatured", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener { documents ->
                layoutFeaturedRooms.removeAllViews()
                if (documents.isEmpty) {
                    tvNoFeatured.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }
                tvNoFeatured.visibility = View.GONE
                for (doc in documents) {
                    val card = createFeaturedCard(
                        doc.getString("title") ?: "",
                        doc.getLong("price") ?: 0,
                        doc.getString("ward") ?: "",
                        doc.getString("district") ?: "",
                        doc.getLong("area")?.toInt() ?: 0
                    )
                    layoutFeaturedRooms.addView(card)
                }
            }
            .addOnFailureListener {
                tvNoFeatured.visibility = View.VISIBLE
            }
    }

    private fun loadNewRooms() {
        db.collection("rooms")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { documents ->
                layoutNewRooms.removeAllViews()
                if (documents.isEmpty) {
                    tvNoNewRooms.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }
                tvNoNewRooms.visibility = View.GONE
                for (doc in documents) {
                    val item = createNewRoomItem(
                        doc.getString("title") ?: "",
                        doc.getLong("price") ?: 0,
                        doc.getString("ward") ?: "",
                        doc.getString("district") ?: "",
                        doc.getLong("area")?.toInt() ?: 0,
                        doc.getBoolean("hasWifi") ?: false,
                        doc.getBoolean("hasAirCon") ?: false,
                        doc.getBoolean("hasWaterHeater") ?: false
                    )
                    layoutNewRooms.addView(item)
                }
            }
            .addOnFailureListener {
                tvNoNewRooms.visibility = View.VISIBLE
            }
    }

    private fun createFeaturedCard(
        title: String, price: Long, ward: String, district: String, area: Int
    ): CardView {
        val card = CardView(requireContext())
        val cardParams = LinearLayout.LayoutParams(dpToPx(220), LinearLayout.LayoutParams.WRAP_CONTENT)
        cardParams.marginEnd = dpToPx(10)
        card.layoutParams = cardParams
        card.radius = dpToPx(14).toFloat()
        card.cardElevation = dpToPx(3).toFloat()
        card.setCardBackgroundColor(0xFFFFFFFF.toInt())

        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL

        val imgPlaceholder = View(requireContext())
        imgPlaceholder.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(120)
        )
        imgPlaceholder.setBackgroundColor(0xFFE0E0E0.toInt())
        layout.addView(imgPlaceholder)

        val contentLayout = LinearLayout(requireContext())
        contentLayout.orientation = LinearLayout.VERTICAL
        contentLayout.setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10))

        val tvPrice = TextView(requireContext())
        tvPrice.text = "${formatter.format(price)} đ/tháng"
        tvPrice.textSize = 15f
        tvPrice.setTextColor(0xFFD32F2F.toInt())
        tvPrice.setTypeface(tvPrice.typeface, android.graphics.Typeface.BOLD)
        contentLayout.addView(tvPrice)

        val tvTitle = TextView(requireContext())
        tvTitle.text = title
        tvTitle.textSize = 13f
        tvTitle.setTextColor(0xFF333333.toInt())
        tvTitle.setPadding(0, dpToPx(2), 0, 0)
        contentLayout.addView(tvTitle)

        val tvLocation = TextView(requireContext())
        tvLocation.text = "$ward, $district • ${area}m²"
        tvLocation.textSize = 12f
        tvLocation.setTextColor(0xFF999999.toInt())
        tvLocation.setPadding(0, dpToPx(2), 0, 0)
        contentLayout.addView(tvLocation)

        layout.addView(contentLayout)
        card.addView(layout)
        return card
    }

    private fun createNewRoomItem(
        title: String, price: Long, ward: String, district: String,
        area: Int, hasWifi: Boolean, hasAirCon: Boolean, hasWaterHeater: Boolean
    ): CardView {
        val card = CardView(requireContext())
        val cardParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        cardParams.bottomMargin = dpToPx(10)
        card.layoutParams = cardParams
        card.radius = dpToPx(14).toFloat()
        card.cardElevation = dpToPx(3).toFloat()
        card.setCardBackgroundColor(0xFFFFFFFF.toInt())

        val rowLayout = LinearLayout(requireContext())
        rowLayout.orientation = LinearLayout.HORIZONTAL
        rowLayout.setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10))

        val imgPlaceholder = View(requireContext())
        imgPlaceholder.layoutParams = LinearLayout.LayoutParams(dpToPx(100), dpToPx(90))
        imgPlaceholder.setBackgroundColor(0xFFE0E0E0.toInt())
        rowLayout.addView(imgPlaceholder)

        val contentLayout = LinearLayout(requireContext())
        contentLayout.orientation = LinearLayout.VERTICAL
        val contentParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        contentParams.marginStart = dpToPx(10)
        contentLayout.layoutParams = contentParams

        val tvTitle = TextView(requireContext())
        tvTitle.text = title
        tvTitle.textSize = 14f
        tvTitle.setTextColor(0xFF1A1A2E.toInt())
        tvTitle.setTypeface(tvTitle.typeface, android.graphics.Typeface.BOLD)
        contentLayout.addView(tvTitle)

        val tvLocation = TextView(requireContext())
        tvLocation.text = "$ward, $district"
        tvLocation.textSize = 12f
        tvLocation.setTextColor(0xFF999999.toInt())
        tvLocation.setPadding(0, dpToPx(2), 0, 0)
        contentLayout.addView(tvLocation)

        val utilities = mutableListOf<String>()
        utilities.add("${area}m²")
        if (hasWifi) utilities.add("Wifi")
        if (hasAirCon) utilities.add("Điều hòa")
        if (hasWaterHeater) utilities.add("Nóng lạnh")

        val tvUtilities = TextView(requireContext())
        tvUtilities.text = utilities.joinToString(" • ")
        tvUtilities.textSize = 12f
        tvUtilities.setTextColor(0xFF666666.toInt())
        tvUtilities.setPadding(0, dpToPx(4), 0, 0)
        contentLayout.addView(tvUtilities)

        val tvPrice = TextView(requireContext())
        tvPrice.text = "${formatter.format(price)} đ/tháng"
        tvPrice.textSize = 15f
        tvPrice.setTextColor(0xFFD32F2F.toInt())
        tvPrice.setTypeface(tvPrice.typeface, android.graphics.Typeface.BOLD)
        tvPrice.setPadding(0, dpToPx(4), 0, 0)
        contentLayout.addView(tvPrice)

        rowLayout.addView(contentLayout)
        card.addView(rowLayout)
        return card
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onResume() {
        super.onResume()
        loadUserName()
        loadFeaturedRooms()
        loadNewRooms()
    }
}