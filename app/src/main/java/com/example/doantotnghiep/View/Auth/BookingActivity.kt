package com.example.doantotnghiep.View.Auth

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doantotnghiep.Model.Appointment
import com.example.doantotnghiep.Model.StatusChange
import com.example.doantotnghiep.Model.TimeSlotConfig
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.ViewModel.BookingViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

private const val TYPE_HEADER = 0
private const val TYPE_SLOT   = 1

class BookingActivity : AppCompatActivity() {

    // App bar / progress
    private lateinit var btnBack: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var viewFlipper: ViewFlipper
    private lateinit var bottomNavBar: LinearLayout
    private lateinit var btnPrevStep: MaterialButton
    private lateinit var btnNextStep: MaterialButton
    private lateinit var btnSpacer: View

    // Step indicators
    private lateinit var tvStep1: TextView
    private lateinit var tvStep2: TextView
    private lateinit var tvStep3: TextView
    private lateinit var tvStep4: TextView
    private lateinit var divider12: View
    private lateinit var divider23: View
    private lateinit var divider34: View

    // Room info card
    private lateinit var tvRoomTitle: TextView
    private lateinit var tvRoomAddress: TextView
    private lateinit var tvPostExpiry: TextView

    // Step 1 – Calendar
    private lateinit var tvNoShowWarning: TextView
    private lateinit var rvCalendar: RecyclerView
    private lateinit var tvMonthYear: TextView
    private lateinit var btnPrevMonth: ImageView
    private lateinit var btnNextMonth: ImageView
    private lateinit var tvRangeInfo: TextView

    // Step 2 – Time slots
    private lateinit var rvTimeSlots: RecyclerView
    private lateinit var tvSelectedDateLabel: TextView
    private lateinit var tvNoSlots: TextView
    private lateinit var tvAppointmentNotice: TextView

    // Step 3 – Personal info
    private lateinit var tvConfirmDateTime: TextView
    private lateinit var edtFullName: com.google.android.material.textfield.TextInputEditText
    private lateinit var rgGender: RadioGroup
    private lateinit var rbMale: RadioButton
    private lateinit var rbFemale: RadioButton
    private lateinit var rbOther: RadioButton
    private lateinit var edtPhone: com.google.android.material.textfield.TextInputEditText
    private lateinit var tvPhoneError: TextView
    private lateinit var edtNote: com.google.android.material.textfield.TextInputEditText
    private lateinit var tvNoteCounter: TextView
    private lateinit var tvNoticeStep3: TextView

    // Step 4 – Summary
    private lateinit var tvSummaryRoomValue: TextView
    private lateinit var tvSummaryAddressValue: TextView
    private lateinit var tvSummaryDateValue: TextView
    private lateinit var tvSummaryTimeValue: TextView
    private lateinit var tvSummaryNameValue: TextView
    private lateinit var tvSummaryGenderValue: TextView
    private lateinit var tvSummaryPhoneValue: TextView
    private lateinit var tvSummaryNoteValue: TextView
    private lateinit var btnEditInfo: MaterialButton
    private lateinit var btnConfirmBooking: MaterialButton

    // Step 5 – Success
    private lateinit var btnGoHome: MaterialButton
    private lateinit var btnViewAppointments: MaterialButton

    private val viewModel: BookingViewModel by viewModels()

    // ─── Intent extras ────────────────────────────────────────────────────────
    private var roomId = ""
    private var landlordId = ""
    private var roomTitle = ""
    private var roomAddress = ""
    private var roomImageUrl = ""
    private var landlordName = ""
    private var landlordPhone = ""
    private var roomPrice = 0L

    // ─── UI state ─────────────────────────────────────────────────────────────
    private var currentStep = 1
    private var selectedDateStr = ""      // "dd/MM/yyyy"
    private var selectedDateDisplay = "" // "Thứ Tư, 10/06/2026"
    private var selectedDateMs = 0L
    private var selectedTime = ""         // "HH:mm"
    private var currentMonth = Calendar.getInstance()

    // Data state
    private var timeSlots: List<TimeSlotConfig> = emptyList()
    private var bookedSlotKeys = setOf<String>()     // "dd-MM-yyyy_HH-mm"
    private var dailyBookingCounts = mapOf<String, Int>() // "dd/MM/yyyy" → count
    private var postExpiryDate = 0L
    private var appointmentNotice = ""
    private var pendingBottomSheetDay: CalendarDay? = null

    //Adapters
    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var slotAdapter: GroupedSlotAdapter

    //-1-//
    // HÀM KHỞI TẠO (VÒNG ĐỜI ACTIVITY)
    // Chạy đầu tiên khi mở màn hình. Nhận dữ liệu truyền từ màn hình trước (Intent) và gọi các hàm cài đặt giao diện ban đầu.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking)

        roomId      = intent.getStringExtra("roomId")      ?: ""
        landlordId  = intent.getStringExtra("landlordId")  ?: ""
        roomTitle   = intent.getStringExtra("roomTitle")   ?: ""
        roomAddress = intent.getStringExtra("roomAddress") ?: ""
        roomImageUrl  = intent.getStringExtra("roomImageUrl")  ?: ""
        landlordName  = intent.getStringExtra("landlordName")  ?: ""
        landlordPhone = intent.getStringExtra("landlordPhone") ?: ""
        roomPrice = intent.getLongExtra("roomPrice", 0L)

        initViews() // Khởi tạo và gán sự kiện cho các nút bấm trên màn hình.
        runPreChecks() // Kiểm tra xem khách có vi phạm lỗi (như bùng kèo) để chặn không cho đặt lịch.
        setupObservers() // Đứng chờ, khi nào server báo lưu thành công thì tự động chuyển sang trang Hoàn tất.
        loadData() // Tải dữ liệu các ngày, giờ còn trống từ trên mạng về để hiện ra cho khách chọn.
    }

    //-2-//
    //View binding
    // HÀM ÁNH XẠ GIAO DIỆN (LIÊN KẾT CODE VỚI XML)
    // Dùng findViewById để lấy các nút bấm, textview trên giao diện và gắn sự kiện (click) cho chúng.
    private fun initViews() {
        btnBack      = findViewById(R.id.btnBack)
        progressBar  = findViewById(R.id.progressBar)
        viewFlipper  = findViewById(R.id.viewFlipper)
        bottomNavBar = findViewById(R.id.bottomNavBar)
        btnPrevStep  = findViewById(R.id.btnPrevStep)
        btnNextStep  = findViewById(R.id.btnNextStep)
        btnSpacer    = findViewById(R.id.btnSpacer)

        tvStep1   = findViewById(R.id.tvStep1Indicator)
        tvStep2   = findViewById(R.id.tvStep2Indicator)
        tvStep3   = findViewById(R.id.tvStep3Indicator)
        tvStep4   = findViewById(R.id.tvStep4Indicator)
        divider12 = findViewById(R.id.divider12)
        divider23 = findViewById(R.id.divider23)
        divider34 = findViewById(R.id.divider34)

        tvRoomTitle   = findViewById(R.id.tvRoomTitle)
        tvRoomAddress = findViewById(R.id.tvRoomAddress)
        tvPostExpiry  = findViewById(R.id.tvPostExpiry)

        tvNoShowWarning = findViewById(R.id.tvNoShowWarning)
        rvCalendar   = findViewById(R.id.rvCalendar)
        tvMonthYear  = findViewById(R.id.tvMonthYear)
        btnPrevMonth = findViewById(R.id.btnPrevMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)
        tvRangeInfo  = findViewById(R.id.tvRangeInfo)

        rvTimeSlots        = findViewById(R.id.rvTimeSlots)
        tvSelectedDateLabel = findViewById(R.id.tvSelectedDateLabel)
        tvNoSlots           = findViewById(R.id.tvNoSlots)
        tvAppointmentNotice = findViewById(R.id.tvAppointmentNotice)

        tvConfirmDateTime = findViewById(R.id.tvConfirmDateTime)
        edtFullName  = findViewById(R.id.edtFullName)
        rgGender     = findViewById(R.id.rgGender)
        rbMale       = findViewById(R.id.rbMale)
        rbFemale     = findViewById(R.id.rbFemale)
        rbOther      = findViewById(R.id.rbOther)
        edtPhone     = findViewById(R.id.edtPhone)
        tvPhoneError = findViewById(R.id.tvPhoneError)
        edtNote      = findViewById(R.id.edtNote)
        tvNoteCounter = findViewById(R.id.tvNoteCounter)
        tvNoticeStep3 = findViewById(R.id.tvNoticeStep3)

        tvSummaryRoomValue     = findViewById(R.id.tvSummaryRoomValue)
        tvSummaryAddressValue  = findViewById(R.id.tvSummaryAddressValue)
        tvSummaryDateValue     = findViewById(R.id.tvSummaryDateValue)
        tvSummaryTimeValue     = findViewById(R.id.tvSummaryTimeValue)
        tvSummaryNameValue     = findViewById(R.id.tvSummaryNameValue)
        tvSummaryGenderValue   = findViewById(R.id.tvSummaryGenderValue)
        tvSummaryPhoneValue    = findViewById(R.id.tvSummaryPhoneValue)
        tvSummaryNoteValue     = findViewById(R.id.tvSummaryNoteValue)
        btnEditInfo        = findViewById(R.id.btnEditInfo)
        btnConfirmBooking  = findViewById(R.id.btnConfirmBooking)

        btnGoHome            = findViewById(R.id.btnGoHome)
        btnViewAppointments  = findViewById(R.id.btnViewAppointments)

        tvRoomTitle.text   = roomTitle
        tvRoomAddress.text = roomAddress

        // Navigation listeners
        btnBack.setOnClickListener {
            if (currentStep in 2..4) prevStep() else finish()
        }
        btnPrevStep.setOnClickListener { prevStep() }
        btnNextStep.setOnClickListener {
            when (currentStep) {
                1 -> validateStep1()
                2 -> validateStep2()
                3 -> { if (validateInfoForm()) goToStep(4) }
            }
        }
        btnEditInfo.setOnClickListener { goToStep(3) }
        
        // KHI NGƯỜI DÙNG BẤM NÚT "XÁC NHẬN ĐẶT LỊCH" (Bước 4)
        // Hệ thống sẽ hiện popup hỏi lại lần nữa (gọi hàm showConfirmDialog)
        btnConfirmBooking.setOnClickListener { showConfirmDialog() }
        
        btnGoHome.setOnClickListener { finish() }
        btnViewAppointments.setOnClickListener {
            startActivity(android.content.Intent(this, MyAppointmentsActivity::class.java))
            finish()
        }

        // Month navigation
        btnPrevMonth.setOnClickListener { currentMonth.add(Calendar.MONTH, -1); renderCalendar() }
        btnNextMonth.setOnClickListener { currentMonth.add(Calendar.MONTH, 1); renderCalendar() }

        // Note character counter
        edtNote.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                tvNoteCounter.text = "${s?.length ?: 0}/500"
            }
        })

        // Phone blur validation
        edtPhone.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) validatePhone() }

        // Calendar adapter
        calendarAdapter = CalendarAdapter { day ->
            if (day.isEmpty || day.isPast || !day.isAvailable) return@CalendarAdapter
            val count = dailyBookingCounts[day.dateStr] ?: 0
            if (count > 0) showOccupiedBottomSheet(day, count)
            else selectCalendarDay(day)
        }
        rvCalendar.layoutManager = GridLayoutManager(this, 7)
        rvCalendar.adapter = calendarAdapter
        rvCalendar.isNestedScrollingEnabled = false

        // Grouped slot adapter with spanning headers
        slotAdapter = GroupedSlotAdapter { slot ->
            if (slot.isBooked) {
                MessageUtils.showInfoDialog(this, "Slot đã bận", "Khung giờ ${slot.time} đã được đặt rồi.")
                return@GroupedSlotAdapter
            }
            selectedTime = slot.time
            slotAdapter.selectSlot(slot.time)
        }
        val slotManager = GridLayoutManager(this, 4)
        slotManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int) =
                if (slotAdapter.getItemViewType(position) == TYPE_HEADER) 4 else 1
        }
        rvTimeSlots.layoutManager = slotManager
        rvTimeSlots.adapter = slotAdapter

        updateStepUI()
    }

    //-3-//
    // HÀM KIỂM TRA ĐIỀU KIỆN TRƯỚC KHI ĐẶT LỊCH
    // Kiểm tra xem người dùng có phải chủ phòng không, có đang bị cấm đặt lịch do "bùng kèo" quá nhiều không, hoặc đã đặt lịch phòng này chưa.
    private fun runPreChecks() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Nếu là chủ bài đăng thì không được đặt chính bài đăng này
        if (uid == landlordId) {
            AlertDialog.Builder(this)
                .setTitle("Không thể đặt lịch")
                .setMessage("Bạn không thể đặt lịch cho phòng của chính mình.")
                .setPositiveButton("Đóng") { _, _ -> finish() }
                .setCancelable(false).show()
            return
        }

        // Kiểm tra đè lịch: người thuê đang có lịch hẹn đang chờ xác nhận, đã được xác nhận, không được đặt lịch này
        viewModel.checkExistingAppointment(uid, roomId) { exists, status, _, _ ->
            if (exists && status != null) {
                val label = when (status) {
                    "pending"           -> "đang chờ xác nhận"
                    "confirmed"         -> "đã được xác nhận"
                    "tenant_confirmed"  -> "đã xác nhận từ hai phía"
                    else                -> status
                }
                AlertDialog.Builder(this)
                    .setTitle("Đã có lịch hẹn")
                    .setMessage("Bạn đã có lịch hẹn $label cho phòng này.")
                    .setPositiveButton("Xem lịch hẹn") { _, _ ->
                        startActivity(android.content.Intent(this, MyAppointmentsActivity::class.java))
                        finish()
                    }
                    .setNegativeButton("Đóng") { _, _ -> finish() }
                    .setCancelable(false).show()
                return@checkExistingAppointment
            }

            // Kiểm tra xem là người thuê đã được xác nhận nhiều lịch hẹn rồi mà chưa hoàn tất đi xem
            viewModel.checkTenantConfirmedCount(uid) { confirmedCount ->
                if (confirmedCount >= 2) {
                    AlertDialog.Builder(this)
                        .setTitle("Quá nhiều lịch hẹn đã xác nhận")
                        .setMessage("Bạn đang có $confirmedCount lịch hẹn đã xác nhận. Vui lòng hoàn tất các lịch hẹn trước khi đặt thêm.")
                        .setPositiveButton("Xem lịch hẹn") { _, _ ->
                            startActivity(android.content.Intent(this, MyAppointmentsActivity::class.java))
                            finish()
                        }
                        .setNegativeButton("Đóng") { _, _ -> finish() }
                        .setCancelable(false).show()
                    return@checkTenantConfirmedCount
                }

                // Kiểm tra người thuê đặt nhiều hơn 3 lịch hẹn cho các phòng khác nhau nhưng mà chưa được duyệt lịch hẹn
                // Đang ở trạng thái chờ người cho thuê duyệt nên không được thêm lịch hẹn
                viewModel.checkTenantPendingCount(uid) { pendingCount ->
                    if (pendingCount >= 3) {
                        AlertDialog.Builder(this)
                            .setTitle("Quá nhiều lịch hẹn chờ")
                            .setMessage("Bạn đang có $pendingCount lịch hẹn chờ xác nhận. Vui lòng hoàn tất hoặc hủy lịch cũ trước.")
                            .setPositiveButton("Xem lịch hẹn") { _, _ ->
                                startActivity(android.content.Intent(this, MyAppointmentsActivity::class.java))
                                finish()
                            }
                            .setNegativeButton("Đóng") { _, _ -> finish() }
                            .setCancelable(false).show()
                        return@checkTenantPendingCount
                    }

                    // Kiểm tra người thuê không đến xem phòng bị chủ trọ bấm "Khách không đến":
                    // Nếu >= 2 lần trong ngày -> Cảnh báo trên màn hình, sang ngày mới tự reset về 0.
                    // Nếu >= 3 lần trong ngày -> Khóa chức năng đặt lịch, sang ngày mới tự động mở lại và reset về 0.
                    viewModel.checkTenantNoShowCount(uid) { noShowCount, _ ->
                        if (noShowCount >= 3) {
                            // Tài khoản đang bị khóa trong ngày hôm nay, phải sang ngày mới để mở lại
                            // (Nếu đã sang ngày mới, Repository đã tự reset count về 0, code sẽ không vào đây)
                            AlertDialog.Builder(this)
                                .setTitle("Tài khoản bị tạm khóa hôm nay")
                                .setMessage("Bạn đã bị ghi nhận không đến xem phòng $noShowCount lần trong hôm nay. Chức năng đặt lịch hẹn sẽ tự động mở lại vào ngày mai.")
                                .setPositiveButton("Đã hiểu") { _, _ -> finish() }
                                .setCancelable(false).show()
                        } else if (noShowCount >= 2) {
                            // Cảnh báo lần 2, sang ngày mới sẽ tự reset về 0
                            tvNoShowWarning.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    //-4-//
    // Giới hạn 5 lượt đặt lịch hẹn cho 1 bài đăng trong ngày -1-
    // khi mở màn hình, làm nhiệm vụ phát lệnh gọi hàm loadRemainingQuota() của ViewModel để bắt đầu đếm số lượt.
    private fun loadData() {
        progressBar.visibility = View.VISIBLE
        viewModel.loadRemainingQuota(roomId)//Tải số lượt đặt lịch còn lại cho bài đăng đấy trong ngày
        //Tải thông tin phòng và tính số ngày bài đăng còn hiệu lực
        viewModel.loadRoomForBooking(roomId) { room, slots ->
            progressBar.visibility = View.GONE
            timeSlots = slots
            postExpiryDate = room.postExpiryDate
            appointmentNotice = room.appointmentNotice

            if (postExpiryDate > 0) {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
                val daysLeft = ((postExpiryDate - System.currentTimeMillis()) / 86_400_000L).toInt()
                tvPostExpiry.text = "Còn hiệu lực đến ${sdf.format(Date(postExpiryDate))} ($daysLeft ngày)"
                tvRangeInfo.text = "Phạm vi đặt: Từ hôm nay đến ${sdf.format(Date(postExpiryDate - 86_400_000L))}"
            }

            //Load thông tin người thuê để nắm bắt rõ và để sau đó tự động fill thông tin
            viewModel.listenBookedSlotsForRoom(roomId) { booked ->
                bookedSlotKeys = booked
                dailyBookingCounts = computeDailyCounts(booked)
                renderCalendar()
                if (currentStep == 2 && selectedDateStr.isNotEmpty()) renderTimeSlots()
            }

            viewModel.loadUserInfo()
            renderCalendar()
        }
    }

    //-5-//
    private fun computeDailyCounts(keys: Set<String>): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        for (key in keys) {
            // key = "dd-MM-yyyy_HH-mm"  →  datePart = "dd/MM/yyyy"
            val datePart = key.substringBefore("_").replace("-", "/")
            counts[datePart] = (counts[datePart] ?: 0) + 1
        }
        return counts
    }

    //-6-//
    // Giới hạn 5 lượt đặt lịch hẹn cho 1 bài đăng trong ngày -2-
    // Lắng nghe kết quả trả về từ ViewModel để hiển thị số lượt còn lại lên giao diện (đổi màu đỏ cảnh báo nếu hết lượt).
    private fun setupObservers() {
        // Tự động điền họ tên, sdt, giới tính sau khi lấy thông tin người dùng
        viewModel.userData.observe(this) { user ->
            user ?: return@observe
            edtFullName.setText(user.fullName)
            edtPhone.setText(user.phone)
            when (user.gender) {
                "Nam" -> rbMale.isChecked = true
                "Nữ"  -> rbFemale.isChecked = true
                else  -> rbOther.isChecked = true
            }
        }
        // Khóa 2 nút bấm tiếp theo và xác nhận tránh việc bấm liên tục
        viewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            btnNextStep.isEnabled = !loading
            btnConfirmBooking.isEnabled = !loading
        }
        // BƯỚC 3: LẮNG NGHE KẾT QUẢ (NHẬN BIÊN NHẬN)
        // Nếu đặt lịch thành công sẽ chuyển sang bước 5 là hiển thị màn hình đặt lịch thành công
        viewModel.bookingResult.observe(this) { success ->
            if (success == true) { viewModel.resetBookingResult(); goToStep(5) }
        }
        // Mạng lỗi hủy và out màn hình
        viewModel.errorMessage.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                viewModel.resetErrorMessage()
                MessageUtils.showErrorDialog(this, "Lỗi", error)
            }
        }
        // Kiểm tra và thông báo giới hạn đặt lịch 1 phòng cho 1 người trong 1 ngày
        viewModel.remainingQuota.observe(this) { remaining ->
            val tvBookingQuota = findViewById<TextView>(R.id.tvBookingQuota)
            tvBookingQuota.visibility = View.VISIBLE
            val maxQuota = com.example.doantotnghiep.Utils.AppointmentConstants.MAX_DAILY_BOOKING_QUOTA
            tvBookingQuota.text = "Bạn có $remaining/$maxQuota lượt đặt lịch phòng này trong ngày hôm nay"
            if (remaining <= 0) {
                tvBookingQuota.setTextColor(android.graphics.Color.parseColor("#D32F2F"))
                tvBookingQuota.setBackgroundColor(android.graphics.Color.parseColor("#FFEBEE"))
            } else {
                tvBookingQuota.setTextColor(android.graphics.Color.parseColor("#1976D2"))
                tvBookingQuota.setBackgroundColor(android.graphics.Color.parseColor("#E3F2FD"))
            }
        }
    }

    //-7-//
    // HÀM VẼ LỊCH (BƯỚC 1)
    // Tính toán số ngày trong tháng, kiểm tra ngày nào đã qua, ngày nào trống, ngày nào kín lịch để vẽ lên màn hình dạng lưới (Grid).
    private fun renderCalendar() {
        tvMonthYear.text = "Tháng ${currentMonth.get(Calendar.MONTH) + 1} / ${currentMonth.get(Calendar.YEAR)}"

        val days = mutableListOf<CalendarDay>()
        val cal = currentMonth.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)

        val offset = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        repeat(offset) { days.add(CalendarDay.empty()) }

        val todayMidnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        for (d in 1..cal.getActualMaximum(Calendar.DAY_OF_MONTH)) {
            val dayCal = cal.clone() as Calendar
            dayCal.set(Calendar.DAY_OF_MONTH, d)
            dayCal.set(Calendar.HOUR_OF_DAY, 0); dayCal.set(Calendar.MINUTE, 0)
            dayCal.set(Calendar.SECOND, 0);      dayCal.set(Calendar.MILLISECOND, 0)
            val dayMs = dayCal.timeInMillis
            val dayOfWeek = dayCal.get(Calendar.DAY_OF_WEEK)
            val dateStr = String.format("%02d/%02d/%04d",
                d, currentMonth.get(Calendar.MONTH) + 1, currentMonth.get(Calendar.YEAR))
            val dayName = SimpleDateFormat("EEEE", Locale("vi", "VN"))
                .format(dayCal.time).replaceFirstChar { it.uppercase() }

            val isPast        = dayMs < todayMidnight
            val isAfterExpiry = postExpiryDate > 0 && dayMs >= postExpiryDate
            val slotConfig    = timeSlots.find { it.dayOfWeek == dayOfWeek }
            val isAvailable   = !isPast && !isAfterExpiry && slotConfig?.isEnabled == true

            days.add(CalendarDay(
                day = d, dateStr = dateStr, displayStr = "$dayName, $dateStr",
                dateMs = dayMs, dayOfWeek = dayOfWeek,
                isAvailable = isAvailable, isPast = isPast || isAfterExpiry,
                isSelected = dateStr == selectedDateStr,
                bookingCount = dailyBookingCounts[dateStr] ?: 0
            ))
        }
        calendarAdapter.submitList(days)
    }

    //-8-//
    private fun selectCalendarDay(day: CalendarDay) {
        selectedDateStr     = day.dateStr
        selectedDateDisplay = day.displayStr
        selectedDateMs      = day.dateMs
        calendarAdapter.selectDate(day.dateStr)
    }

    //-9-//
    // HÀM HIỂN THỊ DANH SÁCH LỊCH ĐÃ ĐẶT (BOTTOM SHEET)
    // Khi bấm vào một ngày đã có người đặt, hiện bảng kéo từ dưới lên cho người dùng xem các khung giờ nào đã bị xí chỗ để tránh chọn trùng.
    private fun showOccupiedBottomSheet(day: CalendarDay, count: Int) {
        pendingBottomSheetDay = day
        val sheet = BottomSheetDialog(this)
        val v = layoutInflater.inflate(R.layout.bottom_sheet_occupied_date, null)

        v.findViewById<TextView>(R.id.tvBottomSheetDate).text  = "Lịch hẹn ngày ${day.dateStr}"
        v.findViewById<TextView>(R.id.tvBottomSheetCount).text = "$count lịch hẹn đã đặt trong ngày này"

        val layoutList = v.findViewById<LinearLayout>(R.id.layoutAppointmentsList)
        val spinnerSort = v.findViewById<Spinner>(R.id.spinnerSort)
        val layoutPagination = v.findViewById<LinearLayout>(R.id.layoutPagination)
        val btnPrev = v.findViewById<MaterialButton>(R.id.btnPrevPage)
        val btnNext = v.findViewById<MaterialButton>(R.id.btnNextPage)
        val tvPageInfo = v.findViewById<TextView>(R.id.tvPageInfo)

        var listData = listOf<Appointment>()
        var currentPage = 0
        var sortAscending = true

        fun renderList() {
            val sorted = if (sortAscending) listData.sortedBy { it.appointmentTime }
                         else listData.sortedByDescending { it.appointmentTime }

            val itemsPerPage = 10
            val totalPages = (sorted.size + itemsPerPage - 1) / itemsPerPage
            val pageStart = currentPage * itemsPerPage
            val pageEnd = minOf(pageStart + itemsPerPage, sorted.size)
            val pageItems = if (sorted.isNotEmpty()) sorted.subList(pageStart, pageEnd) else emptyList()

            layoutList.removeAllViews()
            pageItems.forEachIndexed { idx, appt ->
                val itemNum = pageStart + idx + 1
                val itemView = layoutInflater.inflate(R.layout.item_bottom_sheet_appointment, layoutList, false)
                itemView.findViewById<TextView>(R.id.tvIndex).text = "$itemNum."
                itemView.findViewById<TextView>(R.id.tvName).visibility = View.GONE
                itemView.findViewById<TextView>(R.id.tvGender).visibility = View.GONE
                itemView.findViewById<TextView>(R.id.tvTime).text = appt.appointmentTime
                layoutList.addView(itemView)
            }

            if (totalPages > 1) {
                layoutPagination.visibility = View.VISIBLE
                tvPageInfo.text = "Trang ${currentPage + 1} / $totalPages"
                btnPrev.isEnabled = currentPage > 0
                btnNext.isEnabled = currentPage < totalPages - 1
            } else {
                layoutPagination.visibility = View.GONE
            }
        }

        // Setup sorting Spinner
        val sortOptions = arrayOf("Sớm nhất -> Muộn nhất", "Muộn nhất -> Sớm nhất")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sortOptions)
        spinnerSort.adapter = spinnerAdapter
        spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                sortAscending = pos == 0
                currentPage = 0
                renderList()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Setup pagination buttons
        btnPrev.setOnClickListener {
            if (currentPage > 0) {
                currentPage--
                renderList()
            }
        }
        btnNext.setOnClickListener {
            currentPage++
            renderList()
        }

        viewModel.loadConfirmedAppointmentsForDate(roomId, day.dateStr) { appointments ->
            listData = appointments
            renderList()
        }

        v.findViewById<MaterialButton>(R.id.btnBottomSheetClose).setOnClickListener { sheet.dismiss() }
        v.findViewById<MaterialButton>(R.id.btnBottomSheetProceed).setOnClickListener {
            sheet.dismiss()
            pendingBottomSheetDay?.let { selectCalendarDay(it) }
        }

        sheet.setContentView(v)
        sheet.show()
    }


    //-10-//
    // HÀM VẼ DANH SÁCH KHUNG GIỜ (BƯỚC 2)
    // Bôi xám khung giờ đặt
    // Dựa vào ngày đã chọn ở Bước 1, sinh ra các mốc giờ cách nhau 30 phút. Bôi xám các giờ đã qua hoặc đã bị khách khác đặt.
    private fun renderTimeSlots() {
        if (selectedDateStr.isEmpty()) return
        val dayCal = Calendar.getInstance()
        val parts  = selectedDateStr.split("/")
        if (parts.size == 3) dayCal.set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt())

        val slotConfig = timeSlots.find { it.dayOfWeek == dayCal.get(Calendar.DAY_OF_WEEK) }
        val rawSlots   = slotConfig?.generateSlots() ?: emptyList()

        if (rawSlots.isEmpty()) {
            rvTimeSlots.visibility = View.GONE; tvNoSlots.text = "Không có khung giờ nào khả dụng cho ngày này."; tvNoSlots.visibility = View.VISIBLE; return
        }

        val dateKey  = selectedDateStr.replace("/", "-")
        val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())

        val items = rawSlots.map { timeStr ->
            val slotKey   = "${dateKey}_${timeStr.replace(":", "-")}"
            val isBooked  = bookedSlotKeys.contains(slotKey)
            val isPastSlot = selectedDateStr == todayStr && run {
                val (h, m) = timeStr.split(":").map { it.toInt() }
                val nowCal  = Calendar.getInstance()
                h * 60 + m <= nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)
            }
            TimeSlotItem(
                time = timeStr,
                isBooked = isBooked || isPastSlot,
                isSelected = timeStr == selectedTime,
                statusLabel = when {
                    isBooked   -> "Đã đặt"
                    isPastSlot -> "Đã qua"
                    else       -> "Còn trống"
                }
            )
        }

        val allBooked = items.all { it.isBooked }
        if (allBooked) {
            rvTimeSlots.visibility = View.GONE
            tvNoSlots.text = "Ngày này đã kín lịch hoặc các khung giờ đã qua. Vui lòng chọn ngày khác."
            tvNoSlots.visibility = View.VISIBLE
        } else {
            rvTimeSlots.visibility = View.VISIBLE
            tvNoSlots.visibility = View.GONE
            slotAdapter.submitList(buildGroupedItems(items))
        }

        if (appointmentNotice.isNotEmpty()) {
            tvAppointmentNotice.text = "Lưu ý: $appointmentNotice"
            tvAppointmentNotice.visibility = View.VISIBLE
        }
    }

    //-11-//
    private fun buildGroupedItems(slots: List<TimeSlotItem>): List<GroupedSlotItem> {
        val result = mutableListOf<GroupedSlotItem>()
        var lastGroup = ""
        for (slot in slots) {
            val hour  = slot.time.substringBefore(":").toIntOrNull() ?: 0
            val group = when {
                hour < 12 -> "Buổi sáng"
                hour < 14 -> "Buổi trưa"
                else      -> "Buổi chiều-tối"
            }
            if (group != lastGroup) { result.add(GroupedSlotItem.Header(group)); lastGroup = group }
            result.add(GroupedSlotItem.Slot(slot))
        }
        return result
    }


    //-12-//
    // Giới hạn 5 lượt đặt lịch hẹn cho 1 bài đăng trong ngày -3-
    //
    private fun validateStep1() {
        val remaining = viewModel.remainingQuota.value ?: 0
        if (remaining <= 0) {
            val maxQuota = com.example.doantotnghiep.Utils.AppointmentConstants.MAX_DAILY_BOOKING_QUOTA
            MessageUtils.showErrorDialog(this, "Hết lượt đặt lịch", "Bạn đã hết $maxQuota/$maxQuota lượt đặt lịch cho phòng này hôm nay. Vui lòng thử lại vào ngày mai.")
            return
        }
        if (selectedDateStr.isEmpty()) {
            MessageUtils.showInfoDialog(this, "Chưa chọn ngày", "Vui lòng chọn ngày muốn xem phòng.")
            return
        }
        goToStep(2)
    }

    private fun validateStep2() {
        if (selectedTime.isEmpty()) {
            MessageUtils.showInfoDialog(this, "Chưa chọn giờ", "Vui lòng chọn khung giờ muốn xem phòng.")
            return
        }
        goToStep(3)
    }

    // HÀM KIỂM TRA THÔNG TIN CÁ NHÂN (BƯỚC 3)
    // Đảm bảo tên không được để trống và gọi hàm validatePhone() để check SĐT.
    private fun validateInfoForm(): Boolean {
        if (edtFullName.text.toString().trim().isEmpty()) {
            MessageUtils.showInfoDialog(this, "Thiếu thông tin", "Vui lòng nhập họ và tên.")
            return false
        }
        return validatePhone()
    }

    private fun validatePhone(): Boolean {
        val phone = edtPhone.text.toString().trim()
        return if (phone.length != 10 || !phone.all { it.isDigit() } || !phone.startsWith("0")) {
            tvPhoneError.text = "Số điện thoại phải có 10 chữ số, bắt đầu bằng 0"
            tvPhoneError.visibility = View.VISIBLE
            false
        } else {
            tvPhoneError.visibility = View.GONE; true
        }
    }

    private fun prevStep() {
        if (currentStep in 2..4) goToStep(currentStep - 1)
    }

    // HÀM CHUYỂN BƯỚC (CHUYỂN MÀN HÌNH NHỎ BÊN TRONG)
    // Dùng ViewFlipper để trượt qua lại giữa 5 bước (1: Chọn Ngày, 2: Chọn Giờ, 3: Điền Thông Tin, 4: Xác Nhận, 5: Thành Công).
    private fun goToStep(step: Int) {
        currentStep = step
        viewFlipper.displayedChild = step - 1

        when (step) {
            2 -> {
                tvSelectedDateLabel.text = "Ngày đã chọn: $selectedDateDisplay"
                selectedTime = ""
                renderTimeSlots()
            }
            3 -> {
                tvConfirmDateTime.text = "$selectedDateDisplay  •  $selectedTime"
                if (appointmentNotice.isNotEmpty()) {
                    tvNoticeStep3.text = "Lưu ý từ chủ trọ: $appointmentNotice"
                    tvNoticeStep3.visibility = View.VISIBLE
                }
            }
            4 -> populateSummary()
            5 -> { /* success screen — nothing extra */ }
        }
        updateStepUI()
    }

    //-13-//
    // HÀM CẬP NHẬT GIAO DIỆN THANH TIẾN ĐỘ
    // Đổi màu các hình tròn (1,2,3,4) ở trên cùng màn hình để người dùng biết mình đang ở bước nào trong tiến trình.
    private fun updateStepUI() {
        val activeColor   = ContextCompat.getColor(this, R.color.bottom_nav_selected)
        val inactiveColor = ContextCompat.getColor(this, R.color.gray_200)

        fun applyStep(tv: TextView, active: Boolean, done: Boolean) {
            val drawableRes = if (active || done) R.drawable.bg_step_active else R.drawable.bg_step_inactive
            tv.background = ContextCompat.getDrawable(this, drawableRes)
            tv.setTextColor(if (active || done) Color.WHITE else ContextCompat.getColor(this, R.color.text_secondary))
        }

        applyStep(tvStep1, currentStep == 1, currentStep > 1)
        applyStep(tvStep2, currentStep == 2, currentStep > 2)
        applyStep(tvStep3, currentStep == 3, currentStep > 3)
        applyStep(tvStep4, currentStep == 4, currentStep > 4)

        divider12.setBackgroundColor(if (currentStep > 1) activeColor else inactiveColor)
        divider23.setBackgroundColor(if (currentStep > 2) activeColor else inactiveColor)
        divider34.setBackgroundColor(if (currentStep > 3) activeColor else inactiveColor)


        if (currentStep >= 4) {
            bottomNavBar.visibility = View.GONE
        } else {
            bottomNavBar.visibility = View.VISIBLE
            val showBack = currentStep > 1
            btnPrevStep.visibility = if (showBack) View.VISIBLE else View.GONE
            btnSpacer.visibility   = if (showBack) View.VISIBLE else View.GONE
            btnNextStep.text       = "Tiếp theo"
        }
    }


    //-14-//
    // HÀM ĐỔ DỮ LIỆU RA MÀN HÌNH TỔNG KẾT (BƯỚC 4)
    // Lấy tất cả thông tin đã gom nhặt được ở các Bước 1, 2, 3 để hiển thị lên một bảng tóm tắt cho người dùng xem lại trước khi chốt.
    private fun populateSummary() {
        val gender = when (rgGender.checkedRadioButtonId) {
            R.id.rbMale   -> "Nam"
            R.id.rbFemale -> "Nữ"
            R.id.rbOther  -> "Khác"
            else          -> ""
        }
        tvSummaryRoomValue.text    = roomTitle
        tvSummaryAddressValue.text = roomAddress
        tvSummaryDateValue.text    = selectedDateDisplay
        tvSummaryTimeValue.text    = selectedTime
        tvSummaryNameValue.text    = edtFullName.text.toString().trim()
        tvSummaryGenderValue.text  = gender
        tvSummaryPhoneValue.text   = edtPhone.text.toString().trim()
        val note = edtNote.text.toString().trim()
        tvSummaryNoteValue.text = if (note.isEmpty()) "(không có)" else note
    }


    private fun showConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("Xác nhận đặt lịch")
            .setMessage("Đặt lịch xem phòng lúc $selectedTime ngày $selectedDateStr?")
            .setPositiveButton("Đặt lịch") { _, _ -> submitBooking() }
            .setNegativeButton("Hủy", null)
            .show()
    }

    //-15//
    // BƯỚC 1: ĐÓNG GÓI DỮ LIỆU ĐỂ GỬI ĐI
    // Hàm này sẽ gom toàn bộ thông tin ngày, giờ, thông tin người thuê, chủ trọ thành đối tượng Appointment
    private fun submitBooking() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val now = System.currentTimeMillis()

        val (hStr, mStr) = selectedTime.split(":")
        val appointmentTimestampMs = selectedDateMs + hStr.toLong() * 3_600_000L + mStr.toLong() * 60_000L

        val gender = when (rgGender.checkedRadioButtonId) {
            R.id.rbMale   -> "Nam"
            R.id.rbFemale -> "Nữ"
            R.id.rbOther  -> "Khác"
            else          -> ""
        }

        val appointment = Appointment(
            roomId = roomId, roomTitle = roomTitle, roomAddress = roomAddress,
            roomImageUrl = roomImageUrl, postExpiryDate = postExpiryDate,
            tenantId = uid,
            tenantName   = edtFullName.text.toString().trim(),
            tenantPhone  = edtPhone.text.toString().trim(),
            tenantGender = gender,
            landlordId = landlordId, landlordName = landlordName, landlordPhone = landlordPhone,
            appointmentDate        = selectedDateStr,
            appointmentDateMs      = selectedDateMs,
            appointmentTimestampMs = appointmentTimestampMs,
            appointmentTime        = selectedTime,
            appointmentDateDisplay = selectedDateDisplay,
            status = "pending",
            statusHistory = listOf(
                StatusChange("", "pending", "tenant", uid, "Người thuê đặt lịch", now)
            ),
            landlordConfirmDeadline = appointmentTimestampMs, // Hạn duyệt = đúng giờ hẹn
            tenantConfirmDeadline   = appointmentTimestampMs - 3_600_000L,
            note             = edtNote.text.toString().trim(),
            hasUnreadUpdate  = true,
            createdAt = now, updatedAt = now
        )

        // BƯỚC 2: CHUYỂN PHÁT DỮ LIỆU SANG VIEWMODEL
        // Gọi hàm submitBooking của ViewModel để chuyển tác vụ xuống tầng logic
        viewModel.submitBooking(appointment)
    }


    // Inner data classes
    // Khuôn dữ liệu
    data class CalendarDay(
        val day: Int,
        val dateStr: String,
        val displayStr: String,
        val dateMs: Long,
        val dayOfWeek: Int,
        val isAvailable: Boolean,
        val isPast: Boolean,
        val isSelected: Boolean,
        val bookingCount: Int = 0,
        val isEmpty: Boolean = false
    ) {
        companion object {
            fun empty() = CalendarDay(0, "", "", 0L, 0, false, true, false, 0, true)
        }
    }

    data class TimeSlotItem(
        val time: String,
        val isBooked: Boolean,
        val isSelected: Boolean,
        val statusLabel: String
    )

    sealed class GroupedSlotItem {
        data class Header(val label: String) : GroupedSlotItem()
        data class Slot(val item: TimeSlotItem) : GroupedSlotItem()
    }


    // Hàm vẽ lịch hiển thị
    inner class CalendarAdapter(private val onDayClick: (CalendarDay) -> Unit) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var days: List<CalendarDay> = emptyList()
        private var selectedDate = ""

        fun submitList(list: List<CalendarDay>) { days = list; notifyDataSetChanged() }
        fun selectDate(date: String) { selectedDate = date; notifyDataSetChanged() }

        override fun getItemCount() = days.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            object : RecyclerView.ViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false)
            ) {}

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val day       = days[position]
            val container = holder.itemView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.dayContainer)
            val tvDay     = holder.itemView.findViewById<TextView>(R.id.tvDay)
            val tvBadge   = holder.itemView.findViewById<TextView>(R.id.tvBadge)

            if (day.isEmpty) {
                container.alpha = 0f; holder.itemView.isClickable = false; return
            }

            container.alpha = 1f
            tvDay.text      = day.day.toString()
            tvBadge.visibility = View.GONE

            val isSelected = day.dateStr == selectedDate

            when {
                isSelected -> {
                    container.setCardBackgroundColor(ContextCompat.getColor(this@BookingActivity, R.color.bottom_nav_selected))
                    tvDay.setTextColor(Color.WHITE)
                }
                day.isPast || !day.isAvailable -> {
                    container.setCardBackgroundColor(ContextCompat.getColor(this@BookingActivity, R.color.gray_200))
                    tvDay.setTextColor(ContextCompat.getColor(this@BookingActivity, R.color.text_secondary))
                }
                day.bookingCount == 0 -> {
                    container.setCardBackgroundColor(Color.parseColor("#E3F2FD"))  // xanh nhạt
                    tvDay.setTextColor(ContextCompat.getColor(this@BookingActivity, R.color.bottom_nav_selected))
                }
                day.bookingCount in 1..5 -> {
                    container.setCardBackgroundColor(Color.parseColor("#FFF9C4"))  // vàng nhạt
                    tvDay.setTextColor(Color.parseColor("#F57F17"))
                    tvBadge.text = "${day.bookingCount}"
                    tvBadge.visibility = View.VISIBLE
                }
                else -> {
                    container.setCardBackgroundColor(Color.parseColor("#FFCDD2"))  // đỏ nhạt
                    tvDay.setTextColor(ContextCompat.getColor(this@BookingActivity, R.color.error))
                    tvBadge.text = "${day.bookingCount}"
                    tvBadge.visibility = View.VISIBLE
                }
            }

            holder.itemView.setOnClickListener { onDayClick(day) }
        }
    }

    // Hàm vẽ khung giờ hiển thị
    inner class GroupedSlotAdapter(private val onSlotClick: (TimeSlotItem) -> Unit) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var items: List<GroupedSlotItem> = emptyList()
        private var selectedTime = ""

        fun submitList(list: List<GroupedSlotItem>) { items = list; notifyDataSetChanged() }
        fun selectSlot(time: String) { selectedTime = time; notifyDataSetChanged() }

        override fun getItemCount() = items.size
        override fun getItemViewType(position: Int) =
            if (items[position] is GroupedSlotItem.Header) TYPE_HEADER else TYPE_SLOT

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            if (viewType == TYPE_HEADER)
                object : RecyclerView.ViewHolder(
                    LayoutInflater.from(parent.context).inflate(R.layout.item_slot_header, parent, false)
                ) {}
            else
                object : RecyclerView.ViewHolder(
                    LayoutInflater.from(parent.context).inflate(R.layout.item_time_slot, parent, false)
                ) {}

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = items[position]) {
                is GroupedSlotItem.Header -> {
                    holder.itemView.findViewById<TextView>(R.id.tvSlotGroupHeader).text = item.label
                }
                is GroupedSlotItem.Slot -> {
                    val slot     = item.item
                    val card     = holder.itemView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardSlot)
                    val tvTime   = holder.itemView.findViewById<TextView>(R.id.tvSlotTime)
                    val tvStatus = holder.itemView.findViewById<TextView>(R.id.tvSlotStatus)

                    tvTime.text   = slot.time
                    tvStatus.text = slot.statusLabel

                    when {
                        slot.time == selectedTime -> {
                            card.setCardBackgroundColor(ContextCompat.getColor(this@BookingActivity, R.color.bottom_nav_selected))
                            card.strokeColor = ContextCompat.getColor(this@BookingActivity, R.color.bottom_nav_selected)
                            tvTime.setTextColor(Color.WHITE)
                            tvStatus.setTextColor(Color.WHITE)
                        }
                        slot.isBooked -> {
                            card.setCardBackgroundColor(ContextCompat.getColor(this@BookingActivity, R.color.gray_200))
                            card.strokeColor = ContextCompat.getColor(this@BookingActivity, R.color.gray_200)
                            tvTime.setTextColor(ContextCompat.getColor(this@BookingActivity, R.color.text_secondary))
                            tvStatus.setTextColor(ContextCompat.getColor(this@BookingActivity, R.color.error))
                        }
                        else -> {
                            card.setCardBackgroundColor(ContextCompat.getColor(this@BookingActivity, R.color.bg_white))
                            card.strokeColor = Color.parseColor("#EAEAEA")
                            tvTime.setTextColor(ContextCompat.getColor(this@BookingActivity, R.color.text_primary))
                            tvStatus.setTextColor(ContextCompat.getColor(this@BookingActivity, R.color.success))
                        }
                    }

                    holder.itemView.setOnClickListener { onSlotClick(slot) }
                }
            }
        }
    }
}
