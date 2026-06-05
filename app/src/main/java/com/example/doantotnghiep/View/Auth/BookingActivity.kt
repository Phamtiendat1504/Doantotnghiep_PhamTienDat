package com.example.doantotnghiep.View.Auth

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.ViewModel.BookingViewModel
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

class BookingActivity : AppCompatActivity() {

    private lateinit var tvRoomTitle: TextView
    private lateinit var tvRoomAddress: TextView
    private lateinit var tvRoomPrice: TextView
    private lateinit var edtFullName: EditText
    private lateinit var rgGender: RadioGroup
    private lateinit var rbMale: RadioButton
    private lateinit var rbFemale: RadioButton
    private lateinit var edtPhone: EditText
    private lateinit var tvSelectDate: TextView
    private lateinit var tvSelectTime: TextView
    private lateinit var edtNote: EditText
    private lateinit var btnSubmit: MaterialButton
    private lateinit var btnBack: ImageView
    private lateinit var progressBar: ProgressBar

    private val viewModel: BookingViewModel by viewModels()
    
    private var selectedDate = ""
    private var selectedTime = ""
    private var selectedDateDisplay = ""
    // Map khung giờ bận: key = "dd-MM-yyyy_HH-mm", value = số slot đã confirmed
    private var bookedConflicts: Map<String, Int> = emptyMap()

    private val formatter: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN"))
        symbols.groupingSeparator = '.'
        DecimalFormat("#,###", symbols)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking)

        initViews()
        setupObservers()

        btnBack.setOnClickListener { finish() }

        // Hiển thị thông tin phòng từ Intent
        val roomTitle = intent.getStringExtra("roomTitle") ?: ""
        val roomAddress = intent.getStringExtra("roomAddress") ?: ""
        val roomPrice = intent.getLongExtra("roomPrice", 0)
        tvRoomTitle.text = roomTitle.ifBlank { "Chưa có tiêu đề" }
        tvRoomAddress.text = roomAddress.ifBlank { "Chưa có địa chỉ" }
        tvRoomPrice.text = if (roomPrice > 0) "${formatter.format(roomPrice)} đ/tháng" else "Liên hệ"

        // Load thông tin user mặc định
        viewModel.loadUserInfo()

        val roomId = intent.getStringExtra("roomId") ?: ""
        // Lắng nghe realtime các khung giờ đã được chủ trọ CONFIRM (slot đã bị khóa)
        viewModel.listenTimeConflicts(roomId)

        tvSelectDate.setOnClickListener { showDatePicker() }
        tvSelectTime.setOnClickListener { showTimePicker() }
        btnSubmit.setOnClickListener { submitBooking() }
    }

    private fun initViews() {
        tvRoomTitle = findViewById(R.id.tvRoomTitle)
        tvRoomAddress = findViewById(R.id.tvRoomAddress)
        tvRoomPrice = findViewById(R.id.tvRoomPrice)
        edtFullName = findViewById(R.id.edtFullName)
        rgGender = findViewById(R.id.rgGender)
        rbMale = findViewById(R.id.rbMale)
        rbFemale = findViewById(R.id.rbFemale)
        edtPhone = findViewById(R.id.edtPhone)
        tvSelectDate = findViewById(R.id.tvSelectDate)
        tvSelectTime = findViewById(R.id.tvSelectTime)
        edtNote = findViewById(R.id.edtNote)
        btnSubmit = findViewById(R.id.btnSubmit)
        btnBack = findViewById(R.id.btnBack)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnSubmit.isEnabled = !isLoading
        }

        viewModel.userData.observe(this) { user ->
            user?.let {
                edtFullName.setText(it.fullName)
                edtPhone.setText(it.phone)
                when (it.gender) {
                    "Nam" -> rbMale.isChecked = true
                    "Nữ" -> rbFemale.isChecked = true
                }
            }
        }

        viewModel.bookingResult.observe(this) { success ->
            if (success) {
                MessageUtils.showSuccessDialog(this, "Đặt lịch thành công", "Yêu cầu của bạn đã được gửi tới chủ trọ.") {
                    finish()
                }
            }
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let { 
                if (it.isNotEmpty()) MessageUtils.showErrorDialog(this, "Lỗi", it)
            }
        }

        // Lắng nghe danh sách khung giờ đã bị khóa (chỉ confirmed) — hiển thị cảnh báo khi chọn trùng
        viewModel.timeConflicts.observe(this) { conflicts ->
            bookedConflicts = conflicts
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val picker = DatePickerDialog(this,
            { _, year, month, day ->
                val cal = Calendar.getInstance()
                cal.set(year, month, day)
                val dayOfWeek = SimpleDateFormat("EEEE", Locale("vi", "VN")).format(cal.time)
                selectedDate = String.format("%02d/%02d/%04d", day, month + 1, year)
                selectedDateDisplay = "$dayOfWeek, $selectedDate"
                tvSelectDate.text = selectedDateDisplay
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        picker.datePicker.minDate = System.currentTimeMillis() - 1000
        picker.show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val picker = TimePickerDialog(this,
            { _, hour, minute ->
                val chosen = String.format("%02d:%02d", hour, minute)
                // Kiểm tra ngay sau khi chọn giờ — nếu khung giờ đã bị khóa thì cảnh báo và reset
                val conflictKey = buildConflictKey(selectedDate, chosen)
                if (bookedConflicts.getOrDefault(conflictKey, 0) > 0) {
                    selectedTime = ""
                    tvSelectTime.text = "Chọn giờ hẹn"
                    MessageUtils.showInfoDialog(
                        this,
                        "Khung giờ đã bận",
                        "Khung giờ $chosen vào ngày bạn chọn đã được đặt bởi người khác. Vui lòng chọn giờ khác."
                    )
                } else {
                    selectedTime = chosen
                    tvSelectTime.text = selectedTime
                }
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)
        picker.show()
    }

    // Tạo key conflict khớp với format trong checkTimeConflicts: "dd-MM-yyyy_HH-mm"
    private fun buildConflictKey(date: String, time: String): String {
        return "${date}_${time}"
            .replace("/", "-")
            .replace(":", "-")
            .replace(" ", "_")
    }

    private fun submitBooking() {
        val fullName = edtFullName.text.toString().trim()
        val phone = edtPhone.text.toString().trim()
        val gender = when (rgGender.checkedRadioButtonId) {
            R.id.rbMale -> "Nam"
            R.id.rbFemale -> "Nữ"
            else -> ""
        }

        if (fullName.isEmpty() || phone.isEmpty() || selectedDate.isEmpty() || selectedTime.isEmpty()) {
            MessageUtils.showInfoDialog(this, "Thông tin chưa đủ", "Vui lòng điền đầy đủ thông tin để đặt lịch.")
            return
        }

        // Chặn đặt lịch trong quá khứ nếu ngày hẹn là hôm nay
        val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())
        if (selectedDate == todayStr) {
            val currentTimeStr = SimpleDateFormat("HH:mm", Locale.US).format(Date())
            if (selectedTime <= currentTimeStr) {
                MessageUtils.showInfoDialog(this, "Thời gian không hợp lệ", "Khung giờ hẹn xem phòng của ngày hôm nay phải lớn hơn thời gian hiện tại.")
                return
            }
        }

        val uid = viewModel.getCurrentUserId() ?: return
        val roomId = intent.getStringExtra("roomId") ?: ""

        // BƯỚC 1: Kiểm tra giới hạn 3 lần đặt lịch mỗi ngày
        viewModel.checkDailyBookingLimit(uid,
            onAllowed = { remaining ->
                val used = 3 - remaining
                val slotText = when (remaining) {
                    1 -> "⚠️ Bạn chỉ còn 1 lượt đặt lịch hôm nay!"
                    else -> "Hôm nay bạn đã dùng $used/3 lượt đặt lịch, còn lại $remaining lượt."
                }
                // Hiện dialog thông báo số lượt còn lại, hỏi xác nhận trước khi tiếp tục
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Xác nhận đặt lịch")
                    .setMessage("$slotText\n\nBạn có muốn tiếp tục đặt lịch hẹn xem phòng này không?")
                    .setPositiveButton("Tiếp tục") { _, _ ->
                        // BƯỚC 2: Kiểm tra lịch hẹn active hoặc đã thuê phòng này
                        viewModel.checkExistingAppointment(uid, roomId) { exists, _, reason, _ ->
                            if (exists) {
                                val builder = androidx.appcompat.app.AlertDialog.Builder(this)
                                when (reason) {
                                    "active" -> builder.setTitle("Bạn đã có lịch hẹn")
                                        .setMessage("Bạn đã gửi một yêu cầu đặt lịch cho phòng này và đang chờ xử lý. Bạn có muốn xem lại danh sách lịch hẹn của mình không?")
                                        .setPositiveButton("Xem lịch hẹn") { _, _ ->
                                            startActivity(Intent(this, MyAppointmentsActivity::class.java))
                                            finish()
                                        }
                                        .setNegativeButton("Đóng", null)
                                    "rented" -> builder.setTitle("Phòng đã được thuê")
                                        .setMessage("Bạn đã hoàn tất việc thuê phòng này. Không thể đặt thêm lịch hẹn mới.")
                                        .setPositiveButton("Đóng", null)
                                    else -> { performSubmit(); return@checkExistingAppointment }
                                }
                                builder.show()
                            } else {
                                performSubmit()
                            }
                        }
                    }
                    .setNegativeButton("Hủy", null)
                    .show()
            },
            onBlocked = { usedToday ->
                MessageUtils.showInfoDialog(
                    this,
                    "Đã đạt giới hạn hôm nay",
                    "Bạn đã dùng hết $usedToday/3 lượt đặt lịch trong ngày hôm nay.\nGiới hạn sẽ được reset lúc 00:00 ngày mai."
                )
            }
        )
    }

    private fun performSubmit() {
        val fullName = edtFullName.text.toString().trim()
        val phone = edtPhone.text.toString().trim()
        val gender = when (rgGender.checkedRadioButtonId) {
            R.id.rbMale -> "Nam"
            R.id.rbFemale -> "Nữ"
            else -> ""
        }
        val uid = viewModel.getCurrentUserId() ?: return
        val roomId = intent.getStringExtra("roomId") ?: ""
        val landlordId = intent.getStringExtra("landlordId") ?: ""
        val roomTitle = intent.getStringExtra("roomTitle") ?: ""
        val roomAddress = intent.getStringExtra("roomAddress") ?: ""
        val roomImageUrl = intent.getStringExtra("roomImageUrl") ?: ""
        val landlordName = intent.getStringExtra("landlordName") ?: ""
        val landlordPhone = intent.getStringExtra("landlordPhone") ?: ""

        val appointment: HashMap<String, Any> = hashMapOf(
            "tenantId" to uid,
            "tenantName" to fullName,
            "tenantPhone" to phone,
            "tenantGender" to gender,
            "landlordId" to landlordId,
            "roomId" to roomId,
            "roomTitle" to roomTitle,
            "roomAddress" to roomAddress,
            "roomImageUrl" to roomImageUrl,
            "landlordName" to landlordName,
            "landlordPhone" to landlordPhone,
            "date" to selectedDate,
            "dateDisplay" to selectedDateDisplay,
            "time" to selectedTime,
            "note" to edtNote.text.toString().trim(),
            "status" to "pending",
            "createdAt" to System.currentTimeMillis()
        )

        viewModel.submitBooking(appointment, landlordId, roomTitle, fullName, selectedDateDisplay, selectedTime)
    }
}
