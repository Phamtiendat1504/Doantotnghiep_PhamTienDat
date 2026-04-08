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
import com.bumptech.glide.Glide
import com.example.doantotnghiep.R
import androidx.activity.viewModels
import com.example.doantotnghiep.ViewModel.BookingViewModel

class MyAppointmentsActivity : AppCompatActivity() {

    private lateinit var layoutAppointments: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var btnBack: ImageView

    private val bookingViewModel: BookingViewModel by viewModels()

    // Tránh khởi tạo listener trùng lặp mỗi lần onResume
    private var isListenerActive = false

    // Theo dõi action đang thực hiện để hiện đúng dialog kết quả
    private enum class PendingAction { NONE, CONFIRM_LANDLORD, REJECT_LANDLORD, TENANT_CONFIRM, TENANT_CANCEL }
    private var pendingAction = PendingAction.NONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_appointments)

        layoutAppointments = findViewById(R.id.layoutAppointments)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener { finish() }

        setupObservers()
        loadAppointments()
    }

    private fun setupObservers() {
        // Quan sát danh sách lịch hẹn từ ViewModel
        bookingViewModel.appointments.observe(this) { appointmentList ->
            layoutAppointments.removeAllViews()
            val role = bookingViewModel.userRole.value ?: "tenant"
            val isLandlord = role == "landlord" || role == "admin"

            if (appointmentList.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                return@observe
            }

            tvEmpty.visibility = View.GONE
            // Sắp xếp theo thời gian mới nhất
            val sorted = appointmentList.sortedByDescending { it["createdAt"] as? Long ?: 0L }

            for (doc in sorted) {
                val card = createAppointmentCard(
                    docId = doc["id"] as? String ?: "",
                    tenantName = doc["tenantName"] as? String ?: "",
                    tenantPhone = doc["tenantPhone"] as? String ?: "",
                    tenantGender = doc["tenantGender"] as? String ?: "",
                    landlordName = doc["landlordName"] as? String ?: "",
                    landlordPhone = doc["landlordPhone"] as? String ?: "",
                    roomTitle = doc["roomTitle"] as? String ?: "",
                    roomAddress = doc["roomAddress"] as? String ?: "",
                    roomImageUrl = doc["roomImageUrl"] as? String ?: "",
                    dateDisplay = doc["dateDisplay"] as? String ?: doc["date"] as? String ?: "",
                    time = doc["time"] as? String ?: "",
                    note = doc["note"] as? String ?: "",
                    status = doc["status"] as? String ?: "pending",
                    rejectReason = doc["rejectReason"] as? String ?: "",
                    isLandlord = isLandlord,
                    tenantId = doc["tenantId"] as? String ?: "",
                    landlordId = doc["landlordId"] as? String ?: "",
                    roomId = doc["roomId"] as? String ?: ""
                )
                layoutAppointments.addView(card)
            }
        }

        // Quan sát kết quả action (confirm/reject...)
        bookingViewModel.bookingResult.observe(this) { success ->
            if (success == true) {
                bookingViewModel.resetBookingResult() // Dùng ViewModel reset, không set trực tiếp
                val action = pendingAction
                pendingAction = PendingAction.NONE
                when (action) {
                    PendingAction.CONFIRM_LANDLORD ->
                        MessageUtils.showSuccessDialog(this, "Xác nhận thành công", "Lịch hẹn đã được xác nhận. Người thuê sẽ nhận được thông báo.")
                    PendingAction.REJECT_LANDLORD ->
                        MessageUtils.showInfoDialog(this, "Đã từ chối", "Lịch hẹn đã bị từ chối. Người thuê sẽ nhận được thông báo.")
                    PendingAction.TENANT_CONFIRM ->
                        MessageUtils.showSuccessDialog(this, "Xác nhận thành công", "Bạn đã xác nhận sẽ đến xem phòng đúng giờ hẹn.")
                    PendingAction.TENANT_CANCEL ->
                        MessageUtils.showInfoDialog(this, "Đã huỷ lịch hẹn", "Lịch hẹn đã được huỷ. Chủ trọ sẽ nhận được thông báo.")
                    else -> {}
                }
                // Realtime listener tự cập nhật — không cần gọi lại thủ công
            }
        }

        bookingViewModel.errorMessage.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                bookingViewModel.resetErrorMessage() // Dùng ViewModel reset
                pendingAction = PendingAction.NONE
                MessageUtils.showErrorDialog(this, "Lỗi", error)
            }
        }

        bookingViewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        // Chỉ khởi tạo listener lần đầu tiên — tránh duplicate listener khi dialog đóng lại
        if (!isListenerActive) {
            loadAppointments()
            isListenerActive = true
        }
    }

    private fun loadAppointments() {
        layoutAppointments.removeAllViews()
        tvEmpty.visibility = View.GONE
        // ViewModel tự lấy role rồi fetch appointments — không cần db trực tiếp
        bookingViewModel.fetchAppointmentsByRole()
    }

    private fun createAppointmentCard(
        docId: String, tenantName: String, tenantPhone: String,
        tenantGender: String, landlordName: String, landlordPhone: String,
        roomTitle: String, roomAddress: String,
        roomImageUrl: String, dateDisplay: String, time: String, note: String,
        status: String, rejectReason: String, isLandlord: Boolean,
        tenantId: String, landlordId: String, roomId: String
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
        mainLayout.orientation = LinearLayout.HORIZONTAL
        mainLayout.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))

        // Ảnh phòng
        val imgView = ImageView(this)
        val imgParams = LinearLayout.LayoutParams(dpToPx(90), dpToPx(80))
        imgView.layoutParams = imgParams
        imgView.scaleType = ImageView.ScaleType.CENTER_CROP
        imgView.setBackgroundColor(0xFFE0E0E0.toInt())
        imgView.clipToOutline = true
        imgView.outlineProvider = object : android.view.ViewOutlineProvider() {
            override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, dpToPx(10).toFloat())
            }
        }
        if (roomImageUrl.isNotEmpty()) {
            Glide.with(this).load(roomImageUrl).centerCrop().into(imgView)
        }
        mainLayout.addView(imgView)

        // Nội dung bên phải
        val contentLayout = LinearLayout(this)
        contentLayout.orientation = LinearLayout.VERTICAL
        val contentParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        contentParams.marginStart = dpToPx(12)
        contentLayout.layoutParams = contentParams

        // Badge trạng thái
        val tvStatus = TextView(this)
        when (status) {
            "pending" -> {
                tvStatus.text = "⏳ Chờ xác nhận"
                tvStatus.setTextColor(0xFFE65100.toInt())
                tvStatus.setBackgroundColor(0xFFFFF3E0.toInt())
            }
            "confirmed" -> {
                tvStatus.text = if (isLandlord)
                    "✓ Đã xác nhận (chờ người thuê xác nhận)"
                else
                    "✓ Chủ trọ đã xác nhận (chờ bạn xác nhận)"
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
        contentLayout.addView(tvStatus)

        // Tên phòng
        val tvRoom = TextView(this)
        tvRoom.text = "🏠 $roomTitle"
        tvRoom.textSize = 14f
        tvRoom.setTextColor(0xFF1A1A2E.toInt())
        tvRoom.setTypeface(tvRoom.typeface, android.graphics.Typeface.BOLD)
        tvRoom.setPadding(0, dpToPx(6), 0, 0)
        tvRoom.maxLines = 2
        contentLayout.addView(tvRoom)

        // Địa chỉ
        val tvAddr = TextView(this)
        tvAddr.text = "📍 $roomAddress"
        tvAddr.textSize = 12f
        tvAddr.setTextColor(0xFF666666.toInt())
        tvAddr.setPadding(0, dpToPx(2), 0, 0)
        tvAddr.maxLines = 2
        contentLayout.addView(tvAddr)

        // Ngày giờ
        val tvDateTime = TextView(this)
        tvDateTime.text = "📅 $dateDisplay  ⏰ $time"
        tvDateTime.textSize = 13f
        tvDateTime.setTextColor(0xFF1976D2.toInt())
        tvDateTime.setTypeface(tvDateTime.typeface, android.graphics.Typeface.BOLD)
        tvDateTime.setPadding(0, dpToPx(4), 0, 0)
        contentLayout.addView(tvDateTime)

        // Thông tin người hẹn
        if (isLandlord) {
            val tvTenant = TextView(this)
            tvTenant.text = "👤 $tenantName ($tenantGender) — $tenantPhone"
            tvTenant.textSize = 12f
            tvTenant.setTextColor(0xFF333333.toInt())
            tvTenant.setPadding(0, dpToPx(3), 0, 0)
            contentLayout.addView(tvTenant)
        } else {
            val tvLandlordLabel = TextView(this)
            tvLandlordLabel.textSize = 12f
            tvLandlordLabel.setTextColor(0xFF333333.toInt())
            tvLandlordLabel.setPadding(0, dpToPx(3), 0, 0)
            tvLandlordLabel.text = when {
                landlordName.isNotEmpty() && landlordPhone.isNotEmpty() ->
                    "👤 Chủ trọ: $landlordName — 📞 $landlordPhone"
                landlordName.isNotEmpty() ->
                    "👤 Chủ trọ: $landlordName"
                else ->
                    "👤 Chủ trọ: Chưa có thông tin"
            }
            contentLayout.addView(tvLandlordLabel)
        }

        // Ghi chú
        if (note.isNotEmpty()) {
            val tvNote = TextView(this)
            tvNote.text = "💬 $note"
            tvNote.textSize = 12f
            tvNote.setTextColor(0xFF666666.toInt())
            tvNote.setPadding(0, dpToPx(2), 0, 0)
            contentLayout.addView(tvNote)
        }

        // Lý do từ chối
        if (status == "rejected" && rejectReason.isNotEmpty()) {
            val tvReason = TextView(this)
            tvReason.text = "⚠️ Lý do: $rejectReason"
            tvReason.textSize = 12f
            tvReason.setTextColor(0xFFD32F2F.toInt())
            tvReason.setPadding(0, dpToPx(2), 0, 0)
            contentLayout.addView(tvReason)
        }

        mainLayout.addView(contentLayout)

        val outerLayout = LinearLayout(this)
        outerLayout.orientation = LinearLayout.VERTICAL
        outerLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        outerLayout.addView(mainLayout)

        // Nút hành động — chủ trọ
        if (isLandlord) {
            val btnLayout = LinearLayout(this)
            btnLayout.orientation = LinearLayout.HORIZONTAL
            btnLayout.setPadding(dpToPx(12), 0, dpToPx(12), dpToPx(10))
            btnLayout.gravity = android.view.Gravity.END

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

            outerLayout.addView(btnLayout)
        } else {
            // Nút hành động — người thuê khi đang pending
            if (status == "pending") {
                val btnLayout = LinearLayout(this)
                btnLayout.orientation = LinearLayout.HORIZONTAL
                btnLayout.setPadding(dpToPx(12), 0, dpToPx(12), dpToPx(10))
                btnLayout.gravity = android.view.Gravity.END

                val btnCancel = TextView(this)
                btnCancel.text = "Hủy đặt lịch"
                btnCancel.textSize = 13f
                btnCancel.setTextColor(0xFFD32F2F.toInt())
                btnCancel.setTypeface(btnCancel.typeface, android.graphics.Typeface.BOLD)
                btnCancel.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
                btnCancel.background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(0xFFFFEBEE.toInt())
                    cornerRadius = dpToPx(8).toFloat()
                }
                btnCancel.setOnClickListener { showCancelPendingDialog(docId, landlordId, roomTitle) }
                btnLayout.addView(btnCancel)

                val space = View(this)
                space.layoutParams = LinearLayout.LayoutParams(dpToPx(10), 0)
                btnLayout.addView(space)

                val btnEdit = TextView(this)
                btnEdit.text = "Đổi lịch"
                btnEdit.textSize = 13f
                btnEdit.setTextColor(0xFFFFFFFF.toInt())
                btnEdit.setTypeface(btnEdit.typeface, android.graphics.Typeface.BOLD)
                btnEdit.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
                btnEdit.background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(0xFF1976D2.toInt())
                    cornerRadius = dpToPx(8).toFloat()
                }
                btnEdit.setOnClickListener { showEditScheduleDialog(docId, landlordId, roomTitle) }
                btnLayout.addView(btnEdit)

                outerLayout.addView(btnLayout)
            }

            // Nút hành động — người thuê khi đã được chủ trọ xác nhận
            if (status == "confirmed") {
                val btnLayout = LinearLayout(this)
                btnLayout.orientation = LinearLayout.HORIZONTAL
                btnLayout.setPadding(dpToPx(12), 0, dpToPx(12), dpToPx(10))
                btnLayout.gravity = android.view.Gravity.END

                val btnCancel = TextView(this)
                btnCancel.text = "Không đến nữa"
                btnCancel.textSize = 13f
                btnCancel.setTextColor(0xFFD32F2F.toInt())
                btnCancel.setTypeface(btnCancel.typeface, android.graphics.Typeface.BOLD)
                btnCancel.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
                btnCancel.background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(0xFFFFEBEE.toInt())
                    cornerRadius = dpToPx(8).toFloat()
                }
                btnCancel.setOnClickListener { tenantRejectAppointment(docId, landlordId, roomTitle) }
                btnLayout.addView(btnCancel)

                val space = View(this)
                space.layoutParams = LinearLayout.LayoutParams(dpToPx(10), 0)
                btnLayout.addView(space)

                val btnConfirm = TextView(this)
                btnConfirm.text = "Xác nhận đi xem"
                btnConfirm.textSize = 13f
                btnConfirm.setTextColor(0xFFFFFFFF.toInt())
                btnConfirm.setTypeface(btnConfirm.typeface, android.graphics.Typeface.BOLD)
                btnConfirm.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
                btnConfirm.background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(0xFF2E7D32.toInt())
                    cornerRadius = dpToPx(8).toFloat()
                }
                btnConfirm.setOnClickListener { tenantConfirmAppointment(docId, landlordId, roomTitle) }
                btnLayout.addView(btnConfirm)

                outerLayout.addView(btnLayout)
            }
        }

        card.addView(outerLayout)
        return card
    }

    private fun confirmAppointment(docId: String, tenantId: String, roomTitle: String, date: String, time: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Xác nhận lịch hẹn")
            .setMessage("Bạn xác nhận đồng ý lịch hẹn xem phòng này?")
            .setPositiveButton("Xác nhận") { _, _ ->
                pendingAction = PendingAction.CONFIRM_LANDLORD
                bookingViewModel.confirmAppointment(docId, tenantId, roomTitle, date, time)
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
            .setPositiveButton("Từ chối") { _, _ ->
                val reason = input.text.toString().trim()
                pendingAction = PendingAction.REJECT_LANDLORD
                bookingViewModel.rejectAppointment(docId, tenantId, roomTitle, reason)
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
                bookingViewModel.markAsRented(
                    docId, tenantId, roomTitle, roomId,
                    onSuccess = {
                        progressBar.visibility = View.GONE
                        MessageUtils.showSuccessDialog(this, "Cập nhật thành công", "Phòng đã được đánh dấu cho thuê và xóa khỏi hệ thống.")
                    },
                    onFailure = { error ->
                        progressBar.visibility = View.GONE
                        MessageUtils.showErrorDialog(this, "Lỗi xóa phòng", error)
                    }
                )
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showCancelPendingDialog(appointmentId: String, landlordId: String, roomTitle: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Hủy đặt lịch")
            .setMessage("Bạn chắc chắn muốn hủy lịch hẹn xem phòng \"$roomTitle\"?\n\nChủ trọ sẽ nhận được thông báo.")
            .setPositiveButton("Hủy đặt lịch") { _, _ ->
                bookingViewModel.cancelPendingAppointment(
                    appointmentId, landlordId, roomTitle,
                    onSuccess = {
                        MessageUtils.showSuccessDialog(this, "Đã hủy", "Lịch hẹn đã được hủy thành công.")
                    },
                    onFailure = { error ->
                        MessageUtils.showErrorDialog(this, "Lỗi", error)
                    }
                )
            }
            .setNegativeButton("Quay lại", null)
            .show()
    }

    private fun showEditScheduleDialog(appointmentId: String, landlordId: String, roomTitle: String) {
        var selectedDate = ""
        var selectedDateDisplay = ""
        var selectedTime = ""

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(8), dpToPx(20), dpToPx(8))
        }

        val tvDate = TextView(this).apply {
            text = "Chọn ngày mới"
            textSize = 14f
            setTextColor(0xFF1976D2.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dpToPx(8), 0, dpToPx(8))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xFFE3F2FD.toInt())
                cornerRadius = dpToPx(8).toFloat()
            }
            gravity = android.view.Gravity.CENTER
        }

        val tvTime = TextView(this).apply {
            text = "Chọn giờ mới"
            textSize = 14f
            setTextColor(0xFF1976D2.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dpToPx(8), 0, dpToPx(8))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xFFE3F2FD.toInt())
                cornerRadius = dpToPx(8).toFloat()
            }
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dpToPx(10) }
        }

        tvDate.setOnClickListener {
            val cal = java.util.Calendar.getInstance()
            android.app.DatePickerDialog(this, android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
                { _, year, month, day ->
                    val c = java.util.Calendar.getInstance().apply { set(year, month, day) }
                    val dayOfWeek = java.text.SimpleDateFormat("EEEE", java.util.Locale("vi", "VN")).format(c.time)
                    selectedDate = String.format("%02d/%02d/%04d", day, month + 1, year)
                    selectedDateDisplay = "$dayOfWeek, $selectedDate"
                    tvDate.text = "📅 $selectedDateDisplay"
                    tvDate.setTextColor(0xFF1A1A2E.toInt())
                },
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH),
                cal.get(java.util.Calendar.DAY_OF_MONTH)
            ).apply { datePicker.minDate = System.currentTimeMillis() - 1000 }.show()
        }

        tvTime.setOnClickListener {
            val cal = java.util.Calendar.getInstance()
            android.app.TimePickerDialog(this, android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
                { _, hour, minute ->
                    selectedTime = String.format("%02d:%02d", hour, minute)
                    tvTime.text = "⏰ $selectedTime"
                    tvTime.setTextColor(0xFF1A1A2E.toInt())
                },
                cal.get(java.util.Calendar.HOUR_OF_DAY),
                cal.get(java.util.Calendar.MINUTE), true
            ).show()
        }

        container.addView(tvDate)
        container.addView(tvTime)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Đổi lịch hẹn")
            .setMessage("Chọn ngày và giờ mới cho lịch xem phòng \"$roomTitle\":")
            .setView(container)
            .setPositiveButton("Xác nhận đổi") { _, _ ->
                if (selectedDate.isEmpty() || selectedTime.isEmpty()) {
                    MessageUtils.showInfoDialog(this, "Thiếu thông tin", "Vui lòng chọn cả ngày và giờ mới.")
                    return@setPositiveButton
                }
                bookingViewModel.editPendingAppointment(
                    appointmentId, landlordId, roomTitle,
                    selectedDate, selectedDateDisplay, selectedTime,
                    onSuccess = {
                        MessageUtils.showSuccessDialog(this, "Đổi lịch thành công", "Lịch hẹn đã được cập nhật. Chủ trọ sẽ nhận được thông báo.")
                    },
                    onFailure = { error ->
                        MessageUtils.showErrorDialog(this, "Lỗi", error)
                    }
                )
            }
            .setNegativeButton("Quay lại", null)
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
                pendingAction = PendingAction.TENANT_CONFIRM
                bookingViewModel.tenantConfirmAppointment(docId, landlordId, roomTitle)
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
                pendingAction = PendingAction.TENANT_CANCEL
                bookingViewModel.tenantRejectAppointment(docId, landlordId, roomTitle)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}