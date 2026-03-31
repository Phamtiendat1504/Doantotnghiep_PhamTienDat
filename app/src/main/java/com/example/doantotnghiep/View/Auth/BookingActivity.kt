package com.example.doantotnghiep.View.Auth

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.doantotnghiep.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
    private lateinit var edtPhone: EditText
    private lateinit var tvSelectDate: TextView
    private lateinit var tvSelectTime: TextView
    private lateinit var edtNote: EditText
    private lateinit var btnSubmit: MaterialButton
    private lateinit var btnBack: ImageView
    private lateinit var progressBar: ProgressBar

    private val db = FirebaseFirestore.getInstance()
    private var selectedDate = ""
    private var selectedTime = ""
    private var selectedDateDisplay = ""

    private val formatter: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN"))
        symbols.groupingSeparator = '.'
        DecimalFormat("#,###", symbols)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking)

        tvRoomTitle = findViewById(R.id.tvRoomTitle)
        tvRoomAddress = findViewById(R.id.tvRoomAddress)
        tvRoomPrice = findViewById(R.id.tvRoomPrice)
        edtFullName = findViewById(R.id.edtFullName)
        rgGender = findViewById(R.id.rgGender)
        edtPhone = findViewById(R.id.edtPhone)
        tvSelectDate = findViewById(R.id.tvSelectDate)
        tvSelectTime = findViewById(R.id.tvSelectTime)
        edtNote = findViewById(R.id.edtNote)
        btnSubmit = findViewById(R.id.btnSubmit)
        btnBack = findViewById(R.id.btnBack)
        progressBar = findViewById(R.id.progressBar)

        btnBack.setOnClickListener { finish() }

        // Hiển thị thông tin phòng
        val roomTitle = intent.getStringExtra("roomTitle") ?: ""
        val roomAddress = intent.getStringExtra("roomAddress") ?: ""
        val roomPrice = intent.getLongExtra("roomPrice", 0)
        tvRoomTitle.text = roomTitle
        tvRoomAddress.text = roomAddress
        tvRoomPrice.text = "${formatter.format(roomPrice)} đ/tháng"

        // Load thông tin user
        loadUserInfo()

        // Chọn ngày
        tvSelectDate.setOnClickListener { showDatePicker() }

        // Chọn giờ
        tvSelectTime.setOnClickListener { showTimePicker() }

        // Gửi yêu cầu
        btnSubmit.setOnClickListener { submitBooking() }
    }

    private fun loadUserInfo() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    edtFullName.setText(doc.getString("fullName") ?: "")
                    edtPhone.setText(doc.getString("phone") ?: "")
                    val gender = doc.getString("gender") ?: ""
                    when (gender) {
                        "Nam" -> rgGender.check(R.id.rbMale)
                        "Nữ" -> rgGender.check(R.id.rbFemale)
                    }
                }
            }
    }
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val picker = DatePickerDialog(this, android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
            { _, year, month, day ->
                val cal = Calendar.getInstance()
                cal.set(year, month, day)
                val dayOfWeek = SimpleDateFormat("EEEE", Locale("vi", "VN")).format(cal.time)
                val dateStr = String.format("%02d/%02d/%04d", day, month + 1, year)
                selectedDate = dateStr
                selectedDateDisplay = "$dayOfWeek, $dateStr"
                tvSelectDate.text = selectedDateDisplay
                tvSelectDate.setTextColor(0xFF333333.toInt())
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        picker.datePicker.minDate = System.currentTimeMillis() - 1000
        picker.setTitle("Chọn ngày hẹn")
        picker.window?.setBackgroundDrawableResource(android.R.color.transparent)
        picker.show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val picker = TimePickerDialog(this, android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
            { _, hour, minute ->
                selectedTime = String.format("%02d:%02d", hour, minute)
                tvSelectTime.text = selectedTime
                tvSelectTime.setTextColor(0xFF333333.toInt())
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)
        picker.setTitle("Chọn giờ hẹn")
        picker.window?.setBackgroundDrawableResource(android.R.color.transparent)
        picker.show()
    }

    private fun submitBooking() {
        val fullName = edtFullName.text.toString().trim()
        val phone = edtPhone.text.toString().trim()
        val gender = when (rgGender.checkedRadioButtonId) {
            R.id.rbMale -> "Nam"
            R.id.rbFemale -> "Nữ"
            else -> ""
        }
        val note = edtNote.text.toString().trim()

        // Validate
        if (fullName.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập họ và tên", Toast.LENGTH_SHORT).show()
            return
        }
        if (gender.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn giới tính", Toast.LENGTH_SHORT).show()
            return
        }
        if (phone.isEmpty() || phone.length != 10 || !phone.startsWith("0")) {
            Toast.makeText(this, "Số điện thoại phải có 10 số và bắt đầu bằng 0", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ngày hẹn", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedTime.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn giờ hẹn", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        btnSubmit.isEnabled = false

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val roomId = intent.getStringExtra("roomId") ?: ""
        val landlordId = intent.getStringExtra("landlordId") ?: ""
        val roomTitle = intent.getStringExtra("roomTitle") ?: ""
        val roomAddress = intent.getStringExtra("roomAddress") ?: ""

        val appointment = hashMapOf(
            "tenantId" to uid,
            "tenantName" to fullName,
            "tenantPhone" to phone,
            "tenantGender" to gender,
            "landlordId" to landlordId,
            "roomId" to roomId,
            "roomTitle" to roomTitle,
            "roomAddress" to roomAddress,
            "date" to selectedDate,
            "dateDisplay" to selectedDateDisplay,
            "time" to selectedTime,
            "note" to note,
            "status" to "pending",
            "rejectReason" to "",
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("appointments")
            .add(appointment)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                btnSubmit.isEnabled = true

                // Tạo thông báo cho chủ trọ
                db.collection("notifications").add(
                    hashMapOf(
                        "userId" to landlordId,
                        "title" to "Có lịch hẹn mới!",
                        "message" to "$fullName muốn xem phòng \"$roomTitle\" vào $selectedDateDisplay lúc $selectedTime",
                        "type" to "appointment_new",
                        "isRead" to false,
                        "createdAt" to System.currentTimeMillis()
                    )
                )

                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Đặt lịch thành công!")
                    .setMessage("Yêu cầu đặt lịch hẹn xem phòng đã được gửi.\n\n📅 $selectedDateDisplay\n⏰ $selectedTime\n\nVui lòng chờ chủ trọ xác nhận.")
                    .setPositiveButton("OK") { _, _ -> finish() }
                    .setCancelable(false)
                    .show()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                btnSubmit.isEnabled = true
                Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}