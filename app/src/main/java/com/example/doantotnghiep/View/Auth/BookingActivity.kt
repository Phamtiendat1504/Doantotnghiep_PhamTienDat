package com.example.doantotnghiep.View.Auth

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
        tvRoomTitle.text = roomTitle
        tvRoomAddress.text = roomAddress
        tvRoomPrice.text = "${formatter.format(roomPrice)} đ/tháng"

        // Load thông tin user mặc định
        viewModel.loadUserInfo()

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
                MessageUtils.showErrorDialog(this, "Lỗi", it)
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
                selectedDate = String.format("%02d/%02d/%04d", day, month + 1, year)
                selectedDateDisplay = "$dayOfWeek, $selectedDate"
                tvSelectDate.text = selectedDateDisplay
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        picker.datePicker.minDate = System.currentTimeMillis() - 1000
        picker.show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val picker = TimePickerDialog(this, android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
            { _, hour, minute ->
                selectedTime = String.format("%02d:%02d", hour, minute)
                tvSelectTime.text = selectedTime
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)
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

        if (fullName.isEmpty() || phone.isEmpty() || selectedDate.isEmpty() || selectedTime.isEmpty()) {
            MessageUtils.showInfoDialog(this, "Thông tin chưa đủ", "Vui lòng điền đầy đủ thông tin để đặt lịch.")
            return
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val roomId = intent.getStringExtra("roomId") ?: ""
        val landlordId = intent.getStringExtra("landlordId") ?: ""
        val roomTitle = intent.getStringExtra("roomTitle") ?: ""
        val roomAddress = intent.getStringExtra("roomAddress") ?: ""

        val appointment: HashMap<String, Any> = hashMapOf(
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
            "note" to edtNote.text.toString().trim(),
            "status" to "pending",
            "createdAt" to System.currentTimeMillis()
        )

        viewModel.submitBooking(appointment, landlordId, roomTitle, fullName, selectedDateDisplay, selectedTime)
    }
}
