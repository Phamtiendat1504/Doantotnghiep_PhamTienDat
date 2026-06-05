package com.example.doantotnghiep.View.Auth

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.View.Adapter.AppointmentActionListener
import com.example.doantotnghiep.View.Adapter.AppointmentAdapter
import com.example.doantotnghiep.View.Adapter.AppointmentItem
import com.example.doantotnghiep.ViewModel.BookingViewModel

class MyAppointmentsActivity : AppCompatActivity(), AppointmentActionListener {

    private lateinit var rvAppointments: RecyclerView
    private lateinit var appointmentAdapter: AppointmentAdapter
    private lateinit var tvEmpty: TextView
    private lateinit var btnBack: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var menuFilter: AutoCompleteTextView
    private lateinit var edtSearch: com.google.android.material.textfield.TextInputEditText
    private lateinit var tabLayoutRole: com.google.android.material.tabs.TabLayout

    private var currentFilterIndex = 0
    private var searchQuery = ""
    private var isDualMode = false
    private var currentTab = 0

    private val bookingViewModel: BookingViewModel by viewModels()
    private var tenantList = listOf<Map<String, Any>>()
    private var landlordList = listOf<Map<String, Any>>()
    private val allAppointments: List<Map<String, Any>>
        get() = if (isDualMode && currentTab == 1) landlordList else tenantList

    private enum class PendingAction {
        NONE, CONFIRM_LANDLORD, REJECT_LANDLORD, TENANT_CONFIRM, TENANT_CANCEL, MARK_AS_RENTED
    }

    private var pendingAction = PendingAction.NONE
    private val conflictMap = mutableMapOf<String, Int>()
    private var isReminderShown = false
    private var isRentedDialogShowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_appointments)

        initViews()
        setupObservers()

        btnBack.setOnClickListener { finish() }

        bookingViewModel.initializeAppointmentsScreen()
    }

    private fun initViews() {
        rvAppointments = findViewById(R.id.rvAppointments)
        tvEmpty = findViewById(R.id.tvEmpty)
        btnBack = findViewById(R.id.btnBack)
        progressBar = findViewById(R.id.progressBar)
        menuFilter = findViewById(R.id.menuFilter)
        edtSearch = findViewById(R.id.edtSearch)
        tabLayoutRole = findViewById(R.id.tabLayoutRole)

        appointmentAdapter = AppointmentAdapter(this)
        rvAppointments.layoutManager = LinearLayoutManager(this)
        rvAppointments.adapter = appointmentAdapter

        edtSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s.toString().trim()
                updateListByTab()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        val swipeRefreshLayout: androidx.swiperefreshlayout.widget.SwipeRefreshLayout =
            findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            if (isDualMode) bookingViewModel.fetchBothAppointments()
            else bookingViewModel.fetchAppointmentsByRole()
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun setupDualTabs() {
        tabLayoutRole.visibility = View.VISIBLE
        tabLayoutRole.clearOnTabSelectedListeners()
        tabLayoutRole.removeAllTabs()
        tabLayoutRole.addTab(tabLayoutRole.newTab().setText("Lịch tôi đặt"))
        tabLayoutRole.addTab(tabLayoutRole.newTab().setText("Khách hẹn phòng tôi"))
        tabLayoutRole.addOnTabSelectedListener(object :
            com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                updateFilterMenu(currentTab == 1)
                updateListByTab()
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun updateListByTab() {
        val isLandlordView = if (isDualMode) currentTab == 1
        else (bookingViewModel.userRole.value ?: "user").let { it == "admin" || it == "verified" }

        var filtered = when (currentFilterIndex) {
            1 -> allAppointments.filter { it["status"] == "pending" }
            2 -> allAppointments.filter { it["status"] == "confirmed" }
            3 -> allAppointments.filter { it["status"] == "tenant_confirmed" }
            4 -> allAppointments.filter {
                val s = it["status"] as? String ?: ""
                s == "rejected" || s == "cancelled_by_tenant" || s == "cancelled_by_system"
            }
            5 -> allAppointments.filter { it["status"] == "completed_rented" }
            else -> allAppointments
        }

        if (searchQuery.isNotEmpty()) {
            val q = searchQuery.lowercase()
            filtered = filtered.filter { doc ->
                val tenantName = (doc["tenantName"] as? String ?: "").lowercase()
                val tenantPhone = (doc["tenantPhone"] as? String ?: "").lowercase()
                val landlordName = (doc["landlordName"] as? String ?: "").lowercase()
                val landlordPhone = (doc["landlordPhone"] as? String ?: "").lowercase()
                val roomTitle = (doc["roomTitle"] as? String ?: "").lowercase()
                tenantName.contains(q) || tenantPhone.contains(q) ||
                        landlordName.contains(q) || landlordPhone.contains(q) ||
                        roomTitle.contains(q)
            }
        }

        if (filtered.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            appointmentAdapter.submitList(emptyList())
        } else {
            tvEmpty.visibility = View.GONE
            val isHistory = currentFilterIndex == 4 || currentFilterIndex == 5
            val sorted = if (isHistory) {
                filtered.sortedByDescending { it["createdAt"] as? Long ?: 0L }
            } else {
                filtered.sortedBy {
                    getDateTimeValue(it["date"] as? String ?: "", it["time"] as? String ?: "")
                }
            }
            appointmentAdapter.isLandlord = isLandlordView
            appointmentAdapter.submitList(sorted.map { mapToAppointmentItem(it) })
        }
    }

    private fun mapToAppointmentItem(doc: Map<String, Any>): AppointmentItem {
        val date = doc["date"] as? String ?: ""
        val time = doc["time"] as? String ?: ""
        val hasConflict = (conflictMap["${date}_${time}"] ?: 0) > 1
        return AppointmentItem(
            id = doc["id"] as? String ?: "",
            tenantName = doc["tenantName"] as? String ?: "",
            tenantPhone = doc["tenantPhone"] as? String ?: "",
            tenantGender = doc["tenantGender"] as? String ?: "",
            landlordName = doc["landlordName"] as? String ?: "",
            landlordPhone = doc["landlordPhone"] as? String ?: "",
            roomTitle = doc["roomTitle"] as? String ?: "",
            roomAddress = doc["roomAddress"] as? String ?: "",
            roomImageUrl = doc["roomImageUrl"] as? String,
            date = date,
            dateDisplay = doc["dateDisplay"] as? String ?: date,
            time = time,
            note = doc["note"] as? String ?: "",
            status = doc["status"] as? String ?: "pending",
            rejectReason = doc["rejectReason"] as? String ?: "",
            tenantId = doc["tenantId"] as? String ?: "",
            landlordId = doc["landlordId"] as? String ?: "",
            roomId = doc["roomId"] as? String ?: "",
            hasConflict = hasConflict
        )
    }

    private fun getDateTimeValue(dateStr: String, timeStr: String): Long {
        return try {
            val parts = dateStr.split("/")
            if (parts.size == 3) {
                val day = parts[0].toInt()
                val month = parts[1].toInt()
                val year = parts[2].toInt()
                val timeParts = timeStr.split(":")
                val hour = if (timeParts.size >= 1) timeParts[0].toInt() else 0
                val min = if (timeParts.size >= 2) timeParts[1].toInt() else 0
                (year * 100000000L + month * 1000000L + day * 10000L + hour * 100L + min)
            } else 0L
        } catch (e: Exception) { 0L }
    }

    // --- AppointmentActionListener ---

    override fun onConfirmLandlord(item: AppointmentItem) {
        confirmAppointment(item.id, item.tenantId, item.roomTitle, item.date, item.time)
    }

    override fun onRejectLandlord(item: AppointmentItem) {
        rejectAppointment(item)
    }

    override fun onMarkAsRented(item: AppointmentItem) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Xác nhận cho thuê")
            .setMessage("Hệ thống sẽ chuyển phòng sang 'Đã thuê', ẩn bài đăng và hủy các lịch hẹn khác?")
            .setPositiveButton("Xác nhận") { _, _ ->
                markAsRented(item)
            }
            .setNegativeButton("Hủy", null).show()
    }

    override fun onTenantConfirm(item: AppointmentItem) {
        tenantConfirmAppointment(item)
    }

    override fun onTenantCancel(item: AppointmentItem) {
        tenantRejectAppointment(item)
    }

    override fun onCancelPending(item: AppointmentItem) {
        showCancelPendingDialog(item)
    }

    override fun onEditSchedule(item: AppointmentItem) {
        showEditScheduleDialog(item.id, item.landlordId, item.roomTitle)
    }

    // --- Observers ---

    private fun setupObservers() {
        bookingViewModel.appointmentAccess.observe(this) { access ->
            isDualMode = access.isHostAccess
            if (access.isHostAccess) {
                setupDualTabs()
            } else {
                tabLayoutRole.visibility = View.GONE
                currentTab = 0
                updateFilterMenu(isLandlord = false)
            }
        }

        bookingViewModel.roomRentedNotice.observe(this) { notice ->
            if (notice == null || isRentedDialogShowing) return@observe
            isRentedDialogShowing = true
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(notice.title)
                .setMessage(notice.message)
                .setCancelable(false)
                .setPositiveButton("Đã hiểu") { _, _ ->
                    bookingViewModel.markRoomRentedNoticeRead(notice.id)
                    isRentedDialogShowing = false
                }
                .setOnDismissListener { isRentedDialogShowing = false }
                .show()
        }

        bookingViewModel.appointments.observe(this) { appointmentList ->
            if (isDualMode) return@observe
            tenantList = appointmentList
            val role = bookingViewModel.userRole.value ?: "user"
            val isLandlord = role == "admin" || role == "verified"
            updateFilterMenu(isLandlord)
            if (isLandlord) {
                val hasNewlyConfirmed = appointmentList.any { it["status"] == "tenant_confirmed" }
                if (hasNewlyConfirmed && !isReminderShown) showTenantConfirmedReminderDialog()
                val roomIds = appointmentList.mapNotNull { it["roomId"] as? String }.distinct()
                roomIds.forEach { rid -> bookingViewModel.listenTimeConflicts(rid) }
            }
            updateListByTab()
        }

        bookingViewModel.tenantAppointments.observe(this) { list ->
            if (!isDualMode) return@observe
            tenantList = list
            if (currentTab == 0) {
                updateFilterMenu(isLandlord = false)
                updateListByTab()
            }
        }

        bookingViewModel.landlordAppointments.observe(this) { list ->
            if (!isDualMode) return@observe
            landlordList = list
            if (currentTab == 1) {
                updateFilterMenu(isLandlord = true)
                val hasNewlyConfirmed = list.any { it["status"] == "tenant_confirmed" }
                if (hasNewlyConfirmed && !isReminderShown) showTenantConfirmedReminderDialog()
                val roomIds = list.mapNotNull { it["roomId"] as? String }.distinct()
                roomIds.forEach { rid -> bookingViewModel.listenTimeConflicts(rid) }
                updateListByTab()
            }
        }

        bookingViewModel.timeConflicts.observe(this) { conflicts ->
            conflictMap.clear()
            conflictMap.putAll(conflicts)
        }

        bookingViewModel.bookingResult.observe(this) { success ->
            if (success == true) {
                bookingViewModel.resetBookingResult()
                val action = pendingAction
                pendingAction = PendingAction.NONE
                when (action) {
                    PendingAction.CONFIRM_LANDLORD ->
                        MessageUtils.showSuccessDialog(this, "Xác nhận thành công",
                            "Lịch hẹn đã được xác nhận. Người thuê sẽ nhận được thông báo.")
                    PendingAction.REJECT_LANDLORD ->
                        MessageUtils.showInfoDialog(this, "Đã từ chối",
                            "Lịch hẹn đã bị từ chối. Người thuê sẽ nhận được thông báo.")
                    PendingAction.TENANT_CONFIRM ->
                        MessageUtils.showSuccessDialog(this, "Xác nhận thành công",
                            "Bạn đã xác nhận sẽ đến xem phòng đúng giờ hẹn.")
                    PendingAction.TENANT_CANCEL ->
                        MessageUtils.showInfoDialog(this, "Đã huỷ lịch hẹn",
                            "Lịch hẹn đã được huỷ. Chủ trọ sẽ nhận được thông báo.")
                    PendingAction.MARK_AS_RENTED ->
                        MessageUtils.showSuccessDialog(this, "Chúc mừng!",
                            "Phòng đã được đánh dấu là Đã cho thuê. Các lịch hẹn khác đã được tự động hủy.")
                    else -> {}
                }
            }
        }

        bookingViewModel.errorMessage.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                bookingViewModel.resetErrorMessage()
                pendingAction = PendingAction.NONE
                MessageUtils.showErrorDialog(this, "Lỗi", error)
            }
        }

        bookingViewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun showTenantConfirmedReminderDialog() {
        isReminderShown = true
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Khách đã xác nhận đến")
            .setMessage("Bạn có lịch hẹn khách đã xác nhận sẽ đi xem phòng. Nếu hai bên chốt thuê thành công, đừng quên bấm 'Xác nhận đã cho thuê' để hệ thống tự động ẩn bài và hủy các lịch hẹn khác giúp bạn nhé!")
            .setPositiveButton("Tôi đã hiểu", null)
            .show()
    }

    private fun updateFilterMenu(isLandlord: Boolean) {
        val options = if (isLandlord) {
            arrayOf("Tất cả", "Cần xác nhận", "Chờ khách xác nhận", "Khách sẽ đến", "Đã từ chối/hủy", "Lịch sử")
        } else {
            arrayOf("Tất cả", "Đang chờ duyệt", "Chờ bạn xác nhận", "Bạn đã xác nhận", "Đã từ chối/hủy", "Lịch sử")
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, options)
        menuFilter.setAdapter(adapter)
        if (menuFilter.text.toString().isEmpty()) {
            val safeIndex = if (currentFilterIndex in options.indices) currentFilterIndex else 0
            menuFilter.setText(options[safeIndex], false)
        }
        menuFilter.setOnItemClickListener { _, _, position, _ ->
            currentFilterIndex = position
            updateListByTab()
            menuFilter.clearFocus()
        }
    }

    // --- Dialog methods ---

    private fun confirmAppointment(
        docId: String, tenantId: String, roomTitle: String, date: String, time: String
    ) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Xác nhận lịch hẹn")
            .setMessage("Bạn xác nhận đồng ý lịch hẹn xem phòng này?")
            .setPositiveButton("Xác nhận") { _, _ ->
                pendingAction = PendingAction.CONFIRM_LANDLORD
                bookingViewModel.confirmAppointment(docId, tenantId, roomTitle, date, time)
            }
            .setNegativeButton("Hủy", null).show()
    }

    private fun rejectAppointment(item: AppointmentItem) {
        val input = EditText(this).apply {
            hint = "Lý do từ chối"
            setPadding(48, 32, 48, 32)
        }
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Từ chối").setView(input)
            .setPositiveButton("Gửi") { _, _ ->
                pendingAction = PendingAction.REJECT_LANDLORD
                bookingViewModel.rejectAppointment(item.id, item.tenantId, item.roomTitle, input.text.toString(), item.roomId, item.date, item.time)
            }.setNegativeButton("Hủy", null).show()
    }

    private fun markAsRented(item: AppointmentItem) {
        pendingAction = PendingAction.MARK_AS_RENTED
        bookingViewModel.markAsRented(item.id, item.roomId, item.tenantId, item.roomTitle, item.date, item.time)
    }

    private fun tenantConfirmAppointment(item: AppointmentItem) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Xác nhận")
            .setMessage("Bạn chắc chắn sẽ đến xem phòng đúng hẹn?")
            .setPositiveButton("Xác nhận") { _, _ ->
                pendingAction = PendingAction.TENANT_CONFIRM
                bookingViewModel.tenantConfirmAppointment(item.id, item.landlordId, item.roomTitle, item.roomId, item.date, item.time)
            }.setNegativeButton("Hủy", null).show()
    }

    private fun tenantRejectAppointment(item: AppointmentItem) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Hủy hẹn")
            .setMessage("Bạn muốn hủy lịch hẹn này?")
            .setPositiveButton("Đồng ý") { _, _ ->
                pendingAction = PendingAction.TENANT_CANCEL
                bookingViewModel.tenantRejectAppointment(item.id, item.landlordId, item.roomTitle, item.roomId, item.date, item.time)
            }.setNegativeButton("Hủy", null).show()
    }

    private fun showCancelPendingDialog(item: AppointmentItem) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Hủy yêu cầu")
            .setMessage("Bạn muốn hủy yêu cầu đặt lịch này?")
            .setPositiveButton("Hủy lịch") { _, _ ->
                pendingAction = PendingAction.TENANT_CANCEL
                bookingViewModel.cancelPendingAppointment(
                    item.id, item.landlordId, item.roomTitle, item.roomId, item.date, item.time,
                    onSuccess = { MessageUtils.showSuccessDialog(this, "Thành công", "Đã hủy yêu cầu.") },
                    onFailure = { MessageUtils.showErrorDialog(this, "Lỗi", it) }
                )
            }.setNegativeButton("Đóng", null).show()
    }

    private fun showEditScheduleDialog(docId: String, landlordId: String, roomTitle: String) {
        var selectedDate = ""
        var selectedDateDisplay = ""
        var selectedTime = ""

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 32, 64, 32)
        }
        val tvDate = TextView(this).apply {
            text = "Chọn ngày mới"
            textSize = 14f
            setPadding(0, 32, 0, 32)
        }
        val tvTime = TextView(this).apply {
            text = "⏰ Chọn giờ mới"
            textSize = 14f
            setPadding(0, 32, 0, 32)
        }

        tvDate.setOnClickListener {
            val cal = java.util.Calendar.getInstance()
            val dpDialog = android.app.DatePickerDialog(this, { _, y, m, d ->
                val c = java.util.Calendar.getInstance().apply { set(y, m, d) }
                selectedDate = String.format("%02d/%02d/%04d", d, m + 1, y)
                selectedDateDisplay = "${
                    java.text.SimpleDateFormat("EEEE", java.util.Locale("vi", "VN")).format(c.time)
                }, $selectedDate"
                tvDate.text = selectedDateDisplay
            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH),
                cal.get(java.util.Calendar.DAY_OF_MONTH))
            dpDialog.datePicker.minDate = System.currentTimeMillis() - 1000
            dpDialog.show()
        }

        tvTime.setOnClickListener {
            val cal = java.util.Calendar.getInstance()
            android.app.TimePickerDialog(this, { _, h, min ->
                selectedTime = String.format("%02d:%02d", h, min)
                tvTime.text = "⏰ $selectedTime"
            }, cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE), true)
                .show()
        }

        container.addView(tvDate)
        container.addView(tvTime)

        androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Đổi lịch hẹn").setView(container)
            .setPositiveButton("Cập nhật") { _, _ ->
                if (selectedDate.isEmpty() || selectedTime.isEmpty()) return@setPositiveButton
                bookingViewModel.editPendingAppointment(
                    docId, landlordId, roomTitle, selectedDate, selectedDateDisplay, selectedTime,
                    onSuccess = { MessageUtils.showSuccessDialog(this, "Thành công", "Lịch đã đổi.") },
                    onFailure = { MessageUtils.showErrorDialog(this, "Lỗi", it) }
                )
            }.setNegativeButton("Hủy", null).show()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
