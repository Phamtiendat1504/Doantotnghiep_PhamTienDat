package com.example.doantotnghiep.View.Auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.example.doantotnghiep.Utils.MessageUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.doantotnghiep.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.doantotnghiep.ViewModel.BookingViewModel

class MyAppointmentsActivity : AppCompatActivity() {

    private lateinit var layoutAppointments: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var btnBack: ImageView

    private val db = FirebaseFirestore.getInstance()
    private val bookingViewModel = BookingViewModel()
    // BUG FIX #12: Prevent reloading data on every onResume
    private var isDataLoaded = false

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
        // BUG FIX #12: Only load on first time or when explicitly needed
        if (!isDataLoaded) {
            loadAppointments()
        }
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
                    // BUG FIX #12: Mark data as loaded
                    isDataLoaded = true

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
                            tenantId = doc.getString("tenantId") ?: "",
                            roomId = doc.getString("roomId") ?: ""
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
        tenantId: String, roomId: String
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

        // MỚI: Click vào Card để xem chi tiết phòng dành riêng cho chủ trọ/người thuê (Giao diện riêng)
        card.setOnClickListener {
            if (roomId.isNotEmpty()) {
                val intent = Intent(this, AppointmentRoomDetailActivity::class.java)
                intent.putExtra("roomId", roomId)
                startActivity(intent)
            } else {
                MessageUtils.showErrorDialog(this, "Lỗi", "Không tìm thấy thông tin phòng")
            }
        }

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
                tvStatus.text = "✓ Chủ trọ đã xác nhận (chờ bạn xác nhận)"
                tvStatus.setTextColor(0xFF2E7D32.toInt())
                tvStatus.setBackgroundColor(0xFFE8F5E9.toInt())
            }
            "tenant_confirmed" -> {
                tvStatus.text = "✅ Cả hai đã xác nhận"
                tvStatus.setTextColor(0xFF1565C0.toInt())
                tvStatus.setBackgroundColor(0xFFE3F2FD.toInt())
            }
            "rejected" -> {
                tvStatus.text = "✗ Đã từ chối"
                tvStatus.setTextColor(0xFFD32F2F.toInt())
                tvStatus.setBackgroundColor(0xFFFFEBEE.toInt())
            }
            "cancelled_by_tenant" -> {
                tvStatus.text = "❌ Bạn đã hủy"
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
        if (isLandlord) {
            val btnLayout = LinearLayout(this)
            btnLayout.orientation = LinearLayout.HORIZONTAL
            btnLayout.setPadding(0, dpToPx(12), 0, 0)
            btnLayout.gravity = android.view.Gravity.END

            // Nút "Đã cho thuê" - hiện với tất cả lịch hẹn của chủ trọ
            val btnRented = TextView(this)
            btnRented.text = "Đã cho thuê"
            btnRented.textSize = 13f
            btnRented.setTextColor(0xFF1565C0.toInt())
            btnRented.setTypeface(btnRented.typeface, android.graphics.Typeface.BOLD)
            btnRented.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
            btnRented.background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xFFE3F2FD.toInt())
                cornerRadius = dpToPx(8).toFloat()
            }
            btnRented.setOnClickListener {
                markAsRented(docId, tenantId, roomTitle, roomId)
            }
            btnLayout.addView(btnRented)

            // Chỉ hiện nút Từ chối và Xác nhận khi pending
            if (status == "pending") {
                val space = View(this)
                space.layoutParams = LinearLayout.LayoutParams(dpToPx(10), 0)
                btnLayout.addView(space)

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

                val space2 = View(this)
                space2.layoutParams = LinearLayout.LayoutParams(dpToPx(10), 0)
                btnLayout.addView(space2)

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
                btnConfirm.setOnClickListener {
                    confirmAppointment(docId, tenantId, roomTitle, dateDisplay, time)
                }
                btnLayout.addView(btnConfirm)
            }

            mainLayout.addView(btnLayout)
        } else {
            // Tenant - Xác nhận nút khi status = "confirmed"
            if (status == "confirmed") {
                val btnLayout = LinearLayout(this)
                btnLayout.orientation = LinearLayout.HORIZONTAL
                btnLayout.setPadding(0, dpToPx(12), 0, 0)
                btnLayout.gravity = android.view.Gravity.END

                // Nút "Huỷ lịch"
                val btnCancel = TextView(this)
                btnCancel.text = "Huỷ lịch"
                btnCancel.textSize = 13f
                btnCancel.setTextColor(0xFFD32F2F.toInt())
                btnCancel.setTypeface(btnCancel.typeface, android.graphics.Typeface.BOLD)
                btnCancel.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
                btnCancel.background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(0xFFFFEBEE.toInt())
                    cornerRadius = dpToPx(8).toFloat()
                }
                btnCancel.setOnClickListener {
                    tenantRejectAppointment(docId, tenantId, roomTitle)
                }
                btnLayout.addView(btnCancel)

                val space = View(this)
                space.layoutParams = LinearLayout.LayoutParams(dpToPx(10), 0)
                btnLayout.addView(space)

                // Nút "Xác nhận"
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
                btnConfirm.setOnClickListener {
                    tenantConfirmAppointment(docId, tenantId, roomTitle)
                }
                btnLayout.addView(btnConfirm)

                mainLayout.addView(btnLayout)
            }
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
                        MessageUtils.showSuccessDialog(this, "Xác nhận thành công", "Lịch hẹn đã được xác nhận. Người thuê sẽ nhận được thông báo.") { loadAppointments() }
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
                                "message" to ("Chủ trọ đã từ chối lịch hẹn xem phòng \"$roomTitle\"." +
                                        (if (reason.isNotEmpty()) " Lý do: $reason" else "")),
                                "type" to "appointment_rejected",
                                "isRead" to false,
                                "createdAt" to System.currentTimeMillis()
                            )
                        )
                        MessageUtils.showInfoDialog(this, "Đã từ chối", "Lịch hẹn đã bị từ chối. Người thuê sẽ nhận được thông báo.") { loadAppointments() }
                    }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun markAsRented(docId: String, tenantId: String, roomTitle: String, roomId: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Xác nhận cho thuê")
            .setMessage("Xác nhận phòng \"$roomTitle\" đã được cho thuê?\n\nPhòng này sẽ bị xóa khỏi hệ thống.")
            .setPositiveButton("Xác nhận") { _, _ ->
                progressBar.visibility = View.VISIBLE

                // 1. Gửi thông báo cho người thuê
                db.collection("notifications").add(
                    hashMapOf(
                        "userId" to tenantId,
                        "title" to "Phòng đã được cho thuê",
                        "message" to "Phòng \"$roomTitle\" đã được cho thuê. Cảm ơn bạn đã quan tâm!",
                        "type" to "room_rented",
                        "isRead" to false,
                        "createdAt" to System.currentTimeMillis()
                    )
                )

                // 2. Xóa appointment
                db.collection("appointments").document(docId)
                    .delete()
                    .addOnSuccessListener {
                        // 3. Xóa phòng (cả ảnh Storage) qua RoomRepository
                        com.example.doantotnghiep.repository.RoomRepository().deleteRoom(
                            roomId,
                            onSuccess = {
                                progressBar.visibility = View.GONE
                                MessageUtils.showSuccessDialog(this, "Cập nhật thành công", "Phòng đã được đánh dấu cho thuê và xóa khỏi hệ thống.") { loadAppointments() }
                            },
                            onFailure = { error ->
                                progressBar.visibility = View.GONE
                                MessageUtils.showErrorDialog(this, "Lỗi xóa phòng", error)
                                loadAppointments()
                            }
                        )
                    }
                    .addOnFailureListener { e ->
                        progressBar.visibility = View.GONE
                        MessageUtils.showErrorDialog(this, "Lỗi", e.message ?: "Vui lòng thử lại")
                    }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    // TENANT: Xác nhận lịch hẹn (sau khi chủ trọ xác nhận)
    private fun tenantConfirmAppointment(docId: String, landlordId: String, roomTitle: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Xác nhận lịch hẹn")
            .setMessage("Bạn xác nhận sẽ đi xem phòng \"$roomTitle\" vào đúng giờ hẹn?\n\nChủ trọ sẽ nhận được thông báo xác nhận của bạn.")
            .setPositiveButton("Xác nhận") { _, _ ->
                bookingViewModel.tenantConfirmAppointment(docId, landlordId, roomTitle)
                MessageUtils.showSuccessDialog(this, "Xác nhận thành công", "Bạn đã xác nhận sẽ đến xem phòng đúng giờ hẹn.") { loadAppointments() }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    // TENANT: Từ chối/Huỷ lịch hẹn (sau khi chủ trọ xác nhận)
    private fun tenantRejectAppointment(docId: String, landlordId: String, roomTitle: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Huỷ lịch hẹn")
            .setMessage("Bạn có chắc chắn muốn huỷ lịch hẹn xem phòng \"$roomTitle\"?\n\nChủ trọ sẽ được thông báo về việc huỷ này.")
            .setPositiveButton("Huỷ lịch") { _, _ ->
                bookingViewModel.tenantRejectAppointment(docId, landlordId, roomTitle)
                MessageUtils.showInfoDialog(this, "Đã huỷ lịch hẹn", "Lịch hẹn đã được huỷ. Chủ trọ sẽ nhận được thông báo.") { loadAppointments() }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}