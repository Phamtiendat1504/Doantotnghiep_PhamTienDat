package com.example.doantotnghiep.View.Auth

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.doantotnghiep.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.EditText

class MyAppointmentsActivity : AppCompatActivity() {

    private lateinit var layoutAppointments: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var btnBack: ImageView

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_appointments)

        layoutAppointments = findViewById(R.id.layoutAppointments)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener { finish() }

        loadAppointments()
    }

    override fun onResume() {
        super.onResume()
        loadAppointments()
    }

    private fun loadAppointments() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        layoutAppointments.removeAllViews()
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE

        // Kiểm tra role
        db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
            val role = userDoc.getString("role") ?: "tenant"

            val field = if (role == "landlord" || role == "admin") "landlordId" else "tenantId"

            db.collection("appointments")
                .whereEqualTo(field, uid)
                .get()
                .addOnSuccessListener { documents ->
                    progressBar.visibility = View.GONE
                    layoutAppointments.removeAllViews()

                    if (documents.isEmpty) {
                        tvEmpty.visibility = View.VISIBLE
                        return@addOnSuccessListener
                    }

                    // Sắp xếp theo thời gian mới nhất
                    val sorted = documents.sortedByDescending { it.getLong("createdAt") ?: 0 }

                    for (doc in sorted) {
                        val card = createAppointmentCard(
                            docId = doc.id,
                            tenantName = doc.getString("tenantName") ?: "",
                            tenantPhone = doc.getString("tenantPhone") ?: "",
                            tenantGender = doc.getString("tenantGender") ?: "",
                            roomTitle = doc.getString("roomTitle") ?: "",
                            roomAddress = doc.getString("roomAddress") ?: "",
                            dateDisplay = doc.getString("dateDisplay") ?: doc.getString("date") ?: "",
                            time = doc.getString("time") ?: "",
                            note = doc.getString("note") ?: "",
                            status = doc.getString("status") ?: "pending",
                            rejectReason = doc.getString("rejectReason") ?: "",
                            isLandlord = (role == "landlord" || role == "admin"),
                            tenantId = doc.getString("tenantId") ?: ""
                        )
                        layoutAppointments.addView(card)
                    }
                }
                .addOnFailureListener {
                    progressBar.visibility = View.GONE
                    tvEmpty.text = "Lỗi tải dữ liệu"
                    tvEmpty.visibility = View.VISIBLE
                }
        }
    }

    private fun createAppointmentCard(
        docId: String, tenantName: String, tenantPhone: String,
        tenantGender: String, roomTitle: String, roomAddress: String,
        dateDisplay: String, time: String, note: String,
        status: String, rejectReason: String, isLandlord: Boolean,
        tenantId: String
    ): CardView {
        val card = CardView(this)
        val cardParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        cardParams.bottomMargin = dpToPx(12)
        card.layoutParams = cardParams
        card.radius = dpToPx(14).toFloat()
        card.cardElevation = dpToPx(3).toFloat()
        card.setCardBackgroundColor(0xFFFFFFFF.toInt())

        val mainLayout = LinearLayout(this)
        mainLayout.orientation = LinearLayout.VERTICAL
        mainLayout.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14))

        // Badge trạng thái
        val tvStatus = TextView(this)
        when (status) {
            "pending" -> {
                tvStatus.text = "⏳ Chờ xác nhận"
                tvStatus.setTextColor(0xFFE65100.toInt())
                tvStatus.setBackgroundColor(0xFFFFF3E0.toInt())
            }
            "confirmed" -> {
                tvStatus.text = "✓ Đã xác nhận"
                tvStatus.setTextColor(0xFF2E7D32.toInt())
                tvStatus.setBackgroundColor(0xFFE8F5E9.toInt())
            }
            "rejected" -> {
                tvStatus.text = "✗ Đã từ chối"
                tvStatus.setTextColor(0xFFD32F2F.toInt())
                tvStatus.setBackgroundColor(0xFFFFEBEE.toInt())
            }
        }
        tvStatus.textSize = 12f
        tvStatus.setTypeface(tvStatus.typeface, android.graphics.Typeface.BOLD)
        tvStatus.setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4))
        mainLayout.addView(tvStatus)

        // Tên phòng
        val tvRoom = TextView(this)
        tvRoom.text = "🏠 $roomTitle"
        tvRoom.textSize = 15f
        tvRoom.setTextColor(0xFF1A1A2E.toInt())
        tvRoom.setTypeface(tvRoom.typeface, android.graphics.Typeface.BOLD)
        tvRoom.setPadding(0, dpToPx(10), 0, 0)
        mainLayout.addView(tvRoom)

        // Địa chỉ
        val tvAddr = TextView(this)
        tvAddr.text = "📍 $roomAddress"
        tvAddr.textSize = 13f
        tvAddr.setTextColor(0xFF666666.toInt())
        tvAddr.setPadding(0, dpToPx(3), 0, 0)
        mainLayout.addView(tvAddr)

        // Ngày giờ
        val tvDateTime = TextView(this)
        tvDateTime.text = "📅 $dateDisplay  ⏰ $time"
        tvDateTime.textSize = 14f
        tvDateTime.setTextColor(0xFF1976D2.toInt())
        tvDateTime.setTypeface(tvDateTime.typeface, android.graphics.Typeface.BOLD)
        tvDateTime.setPadding(0, dpToPx(8), 0, 0)
        mainLayout.addView(tvDateTime)

        // Thông tin người hẹn (chủ trọ xem)
        if (isLandlord) {
            val tvTenant = TextView(this)
            tvTenant.text = "👤 $tenantName ($tenantGender) — $tenantPhone"
            tvTenant.textSize = 13f
            tvTenant.setTextColor(0xFF333333.toInt())
            tvTenant.setPadding(0, dpToPx(6), 0, 0)
            mainLayout.addView(tvTenant)
        }

        // Ghi chú
        if (note.isNotEmpty()) {
            val tvNote = TextView(this)
            tvNote.text = "💬 $note"
            tvNote.textSize = 13f
            tvNote.setTextColor(0xFF666666.toInt())
            tvNote.setPadding(0, dpToPx(4), 0, 0)
            mainLayout.addView(tvNote)
        }

        // Lý do từ chối
        if (status == "rejected" && rejectReason.isNotEmpty()) {
            val tvReason = TextView(this)
            tvReason.text = "⚠️ Lý do: $rejectReason"
            tvReason.textSize = 12f
            tvReason.setTextColor(0xFFD32F2F.toInt())
            tvReason.setPadding(0, dpToPx(4), 0, 0)
            mainLayout.addView(tvReason)
        }

        // Nút xác nhận/từ chối (chỉ chủ trọ thấy, chỉ khi pending)
        if (isLandlord && status == "pending") {
            val btnLayout = LinearLayout(this)
            btnLayout.orientation = LinearLayout.HORIZONTAL
            btnLayout.setPadding(0, dpToPx(12), 0, 0)
            btnLayout.gravity = android.view.Gravity.END

            // Nút từ chối
            val btnReject = TextView(this)
            btnReject.text = "Từ chối"
            btnReject.textSize = 13f
            btnReject.setTextColor(0xFFD32F2F.toInt())
            btnReject.setTypeface(btnReject.typeface, android.graphics.Typeface.BOLD)
            btnReject.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
            btnReject.background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xFFFFEBEE.toInt())
                cornerRadius = dpToPx(8).toFloat()
            }
            btnReject.setOnClickListener { rejectAppointment(docId, tenantId, roomTitle) }
            btnLayout.addView(btnReject)

            // Khoảng cách
            val space = View(this)
            space.layoutParams = LinearLayout.LayoutParams(dpToPx(10), 0)
            btnLayout.addView(space)

            // Nút xác nhận
            val btnConfirm = TextView(this)
            btnConfirm.text = "Xác nhận"
            btnConfirm.textSize = 13f
            btnConfirm.setTextColor(0xFFFFFFFF.toInt())
            btnConfirm.setTypeface(btnConfirm.typeface, android.graphics.Typeface.BOLD)
            btnConfirm.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
            btnConfirm.background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xFF2E7D32.toInt())
                cornerRadius = dpToPx(8).toFloat()
            }
            btnConfirm.setOnClickListener { confirmAppointment(docId, tenantId, roomTitle, dateDisplay, time) }
            btnLayout.addView(btnConfirm)

            mainLayout.addView(btnLayout)
        }

        card.addView(mainLayout)
        return card
    }

    private fun confirmAppointment(docId: String, tenantId: String, roomTitle: String, date: String, time: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Xác nhận lịch hẹn")
            .setMessage("Bạn xác nhận đồng ý lịch hẹn xem phòng này?")
            .setPositiveButton("Xác nhận") { dialog, which ->
                db.collection("appointments").document(docId)
                    .update("status", "confirmed")
                    .addOnSuccessListener {
                        // Thông báo cho người thuê
                        db.collection("notifications").add(
                            hashMapOf(
                                "userId" to tenantId,
                                "title" to "Lịch hẹn đã được xác nhận!",
                                "message" to "Chủ trọ đã xác nhận lịch hẹn xem phòng \"$roomTitle\" vào $date lúc $time. Hẹn gặp bạn!",
                                "type" to "appointment_confirmed",
                                "isRead" to false,
                                "createdAt" to System.currentTimeMillis()
                            )
                        )
                        Toast.makeText(this, "Đã xác nhận lịch hẹn", Toast.LENGTH_SHORT).show()
                        loadAppointments()
                    }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun rejectAppointment(docId: String, tenantId: String, roomTitle: String) {
        val input = EditText(this)
        input.hint = "Nhập lý do từ chối"
        input.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Từ chối lịch hẹn")
            .setMessage("Nhập lý do từ chối:")
            .setView(input)
            .setPositiveButton("Từ chối") { dialog, which ->
                val reason = input.text.toString().trim()
                db.collection("appointments").document(docId)
                    .update(
                        mapOf(
                            "status" to "rejected",
                            "rejectReason" to reason
                        )
                    )
                    .addOnSuccessListener {
                        db.collection("notifications").add(
                            hashMapOf(
                                "userId" to tenantId,
                                "title" to "Lịch hẹn bị từ chối",
                                "message" to "Chủ trọ đã từ chối lịch hẹn xem phòng \"$roomTitle\"." +
                                        if (reason.isNotEmpty()) " Lý do: $reason" else "",
                                "type" to "appointment_rejected",
                                "isRead" to false,
                                "createdAt" to System.currentTimeMillis()
                            )
                        )
                        Toast.makeText(this, "Đã từ chối lịch hẹn", Toast.LENGTH_SHORT).show()
                        loadAppointments()
                    }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}