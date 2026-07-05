package com.example.doantotnghiep.View.Auth

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doantotnghiep.Model.Appointment
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.View.Adapter.AppointmentActionListener
import com.example.doantotnghiep.View.Adapter.AppointmentAdapter
import com.example.doantotnghiep.View.Adapter.AppointmentItem
import com.example.doantotnghiep.Model.AppointmentActionResult
import com.example.doantotnghiep.ViewModel.MyAppointmentsViewModel

class MyAppointmentsActivity : AppCompatActivity(), AppointmentActionListener {

    private lateinit var rvAppointments: RecyclerView
    private lateinit var appointmentAdapter: AppointmentAdapter
    private lateinit var tvEmpty: TextView
    private lateinit var btnBack: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var chipGroupFilter: com.google.android.material.chip.ChipGroup
    private lateinit var edtSearch: com.google.android.material.textfield.TextInputEditText
    private lateinit var tabLayoutRole: com.google.android.material.tabs.TabLayout

    private var currentFilterIndex = 0
    private var searchQuery = ""
    private var isDualMode = false
    private var currentTab = 0

    private val viewModel: MyAppointmentsViewModel by viewModels()
    private var tenantList = listOf<Appointment>()
    private var landlordList = listOf<Appointment>()
    private val allAppointments: List<Appointment>
        get() = if (isDualMode && currentTab == 1) landlordList else tenantList

    private var isRentedDialogShowing = false
    private var isReminderShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_appointments)
        initViews()
        setupObservers()
        btnBack.setOnClickListener { finish() }
        viewModel.initializeAppointmentsScreen()
    }

    private fun initViews() {
        rvAppointments = findViewById(R.id.rvAppointments)
        tvEmpty = findViewById(R.id.tvEmpty)
        btnBack = findViewById(R.id.btnBack)
        progressBar = findViewById(R.id.progressBar)
        chipGroupFilter = findViewById(R.id.chipGroupFilter)
        edtSearch = findViewById(R.id.edtSearch)
        tabLayoutRole = findViewById(R.id.tabLayoutRole)

        appointmentAdapter = AppointmentAdapter(this)
        rvAppointments.layoutManager = LinearLayoutManager(this)
        rvAppointments.adapter = appointmentAdapter

        edtSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s.toString().trim(); updateListByTab()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        val swipe: androidx.swiperefreshlayout.widget.SwipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        swipe.setOnRefreshListener {
            if (isDualMode) viewModel.fetchBothAppointments() else viewModel.fetchAppointmentsByRole()
            swipe.isRefreshing = false
        }
    }

    private fun setupDualTabs() {
        tabLayoutRole.visibility = View.VISIBLE
        tabLayoutRole.clearOnTabSelectedListeners()
        tabLayoutRole.removeAllTabs()
        tabLayoutRole.addTab(tabLayoutRole.newTab().setText("Lịch tôi đặt"))
        tabLayoutRole.addTab(tabLayoutRole.newTab().setText("Khách hẹn phòng tôi"))
        tabLayoutRole.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
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
        else (viewModel.userRole.value ?: "user").let { it == "admin" || it == "verified" }

        var filtered = when (currentFilterIndex) {
            1 -> allAppointments.filter { it.status == "pending" }
            2 -> allAppointments.filter { it.status in listOf("confirmed", "tenant_confirmed") }
            3 -> allAppointments.filter { it.status == "completed_viewed" }
            4 -> allAppointments.filter {
                it.status in listOf("rejected", "cancelled_by_tenant", "cancelled_by_system", "expired_pending", "no_show", "viewed_not_rented", "landlord_no_show")
            }
            5 -> allAppointments.filter { it.status == "completed_rented" }
            else -> allAppointments
        }

        if (searchQuery.isNotEmpty()) {
            val q = searchQuery.lowercase()
            filtered = filtered.filter { a ->
                a.tenantName.lowercase().contains(q) || a.tenantPhone.contains(q) ||
                a.landlordName.lowercase().contains(q) || a.roomTitle.lowercase().contains(q) ||
                a.roomAddress.lowercase().contains(q)
            }
        }

        if (filtered.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE; appointmentAdapter.submitList(emptyList())
        } else {
            tvEmpty.visibility = View.GONE
            val isHistory = currentFilterIndex in listOf(4, 5)
            val sorted = if (isHistory) filtered.sortedByDescending { it.createdAt }
                         else filtered.sortedBy { it.appointmentTimestampMs.takeIf { t -> t > 0 } ?: it.appointmentDateMs }
            
            // Tính toán lịch trùng trực tiếp trên danh sách pending
            val pendingConflicts = if (isLandlordView) {
                allAppointments.filter { it.status == "pending" }
                    .groupBy { "${it.roomId}_${it.appointmentDate}_${it.appointmentTime}" }
                    .mapValues { it.value.size }
            } else emptyMap()

            appointmentAdapter.isLandlord = isLandlordView
            appointmentAdapter.submitList(sorted.map { appt ->
                val conflict = if (isLandlordView && appt.status == "pending") {
                    (pendingConflicts["${appt.roomId}_${appt.appointmentDate}_${appt.appointmentTime}"] ?: 0) > 1
                } else false
                appt.toItem(hasConflict = conflict)
            })
        }
    }

    private fun Appointment.toItem(hasConflict: Boolean = false) = AppointmentItem(
        id = id, tenantName = tenantName, tenantPhone = tenantPhone, tenantGender = tenantGender,
        landlordName = landlordName, landlordPhone = landlordPhone,
        roomTitle = roomTitle, roomAddress = roomAddress, roomImageUrl = roomImageUrl.ifEmpty { null },
        appointmentDate = appointmentDate, appointmentDateDisplay = appointmentDateDisplay.ifEmpty { appointmentDate },
        appointmentTime = appointmentTime, note = note, status = status,
        rejectReason = rejectReason, cancelReason = cancelReason,
        tenantId = tenantId, landlordId = landlordId, roomId = roomId,
        landlordConfirmDeadline = landlordConfirmDeadline,
        appointmentTimestampMs = appointmentTimestampMs,
        hasConflict = hasConflict
    )

    //

    private fun setupObservers() {
        viewModel.appointmentAccess.observe(this) { access ->
            isDualMode = access.isHostAccess
            if (access.isHostAccess) setupDualTabs()
            else { tabLayoutRole.visibility = View.GONE; currentTab = 0; updateFilterMenu(false) }
        }

        viewModel.roomRentedNotice.observe(this) { notice ->
            if (notice == null || isRentedDialogShowing) return@observe
            isRentedDialogShowing = true
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(notice.title).setMessage(notice.message).setCancelable(false)
                .setPositiveButton("Đã hiểu") { _, _ ->
                    viewModel.markRoomRentedNoticeRead(notice.id); isRentedDialogShowing = false
                }
                .setOnDismissListener { isRentedDialogShowing = false }.show()
        }

        viewModel.appointments.observe(this) { list ->
            if (isDualMode) return@observe
            tenantList = list
            val role = viewModel.userRole.value ?: "user"
            val isLandlord = role in listOf("admin", "verified")
            updateFilterMenu(isLandlord)
            if (isLandlord) {
                if (list.any { it.status == "tenant_confirmed" } && !isReminderShown) showTenantConfirmedReminder()
            }
            updateListByTab()
        }

        viewModel.tenantAppointments.observe(this) { list ->
            if (!isDualMode) return@observe
            tenantList = list
            if (currentTab == 0) { updateFilterMenu(false); updateListByTab() }
        }

        viewModel.landlordAppointments.observe(this) { list ->
            if (!isDualMode) return@observe
            landlordList = list
            if (currentTab == 1) {
                updateFilterMenu(true)
                if (list.any { it.status == "tenant_confirmed" } && !isReminderShown) showTenantConfirmedReminder()
                updateListByTab()
            }
        }

        viewModel.actionResult.observe(this) { result ->
            if (result is AppointmentActionResult.Success) viewModel.resetActionResult()
        }

        viewModel.errorMessage.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                viewModel.resetErrorMessage()
                MessageUtils.showErrorDialog(this, "Lỗi", error)
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    private fun showTenantConfirmedReminder() {
        isReminderShown = true
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Khách đã xác nhận đến")
            .setMessage("Bạn có lịch hẹn mà khách đã xác nhận sẽ đến xem phòng. Sau giờ hẹn, hãy cập nhật kết quả (khách có đến không) để mở lại slot cho người khác.")
            .setPositiveButton("Tôi đã hiểu", null).show()
    }

    private fun updateFilterMenu(isLandlord: Boolean) {
        val options = if (isLandlord)
            arrayOf("Tất cả", "Cần xác nhận", "Đã xác nhận", "Đã đến xem", "Đã hủy/từ chối", "Đã cho thuê")
        else
            arrayOf("Tất cả", "Đang chờ duyệt", "Đã xác nhận", "Đã đến xem", "Đã hủy/từ chối", "Đã thuê được")

        chipGroupFilter.removeAllViews()
        for ((index, option) in options.withIndex()) {
            val chip = com.google.android.material.chip.Chip(this).apply {
                text = option
                isCheckable = true
                isChecked = index == currentFilterIndex
                id = 1000 + index // Tránh ID 0 không hợp lệ
                tag = index
                chipBackgroundColor = android.content.res.ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(androidx.core.content.ContextCompat.getColor(context, R.color.primary),
                               androidx.core.content.ContextCompat.getColor(context, R.color.gray_200))
                )
                setTextColor(android.content.res.ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(androidx.core.content.ContextCompat.getColor(context, R.color.white),
                               androidx.core.content.ContextCompat.getColor(context, R.color.text_primary))
                ))
            }
            chipGroupFilter.addView(chip)
        }

        chipGroupFilter.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val selectedId = checkedIds[0]
            val selectedChip = group.findViewById<com.google.android.material.chip.Chip>(selectedId)
            currentFilterIndex = selectedChip.tag as Int
            updateListByTab()
        }
    }

    

    override fun onConfirmLandlord(item: AppointmentItem) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Xác nhận lịch hẹn")
            .setMessage("Xác nhận lịch hẹn với ${item.tenantName} vào ${item.appointmentDateDisplay} lúc ${item.appointmentTime}?")
            .setPositiveButton("Xác nhận") { _, _ ->
                viewModel.confirmAppointment(item.id, item.tenantId, item.roomTitle, item.appointmentDate, item.appointmentTime)
            }.setNegativeButton("Hủy", null).show()
    }

    // Nhập lý do từ chối từ phái người chủ khi lịch hẹn trạng thái chờ duyệt
    override fun onRejectLandlord(item: AppointmentItem) {
        val input = EditText(this).apply { hint = "Lý do từ chối (bắt buộc)"; setPadding(48, 32, 48, 32) }
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Từ chối lịch hẹn").setView(input)
            .setPositiveButton("Gửi") { _, _ ->
                val reason = input.text.toString().trim()
                if (reason.isEmpty()) { MessageUtils.showInfoDialog(this, "Thiếu lý do", "Vui lòng nhập lý do từ chối."); return@setPositiveButton }
                viewModel.rejectAppointment(item.id, item.tenantId, item.roomTitle, reason)
                MessageUtils.showInfoDialog(this, "Đã từ chối", "Người thuê sẽ nhận được thông báo.")
            }.setNegativeButton("Hủy", null).show()
    }

    override fun onCancelConfirmedLandlord(item: AppointmentItem) {
        val input = EditText(this).apply { hint = "Ví dụ: Phòng đã có người thuê"; setPadding(48, 32, 48, 32) }
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Hủy lịch hẹn đã xác nhận").setView(input)
            .setMessage("Lưu ý: Bạn chỉ nên hủy khi có lý do chính đáng. Người thuê sẽ nhận được thông báo.")
            .setPositiveButton("Hủy lịch") { _, _ ->
                val reason = input.text.toString().trim()
                if (reason.isEmpty()) { MessageUtils.showInfoDialog(this, "Thiếu lý do", "Vui lòng nhập lý do hủy lịch."); return@setPositiveButton }
                viewModel.cancelByLandlord(item.id, item.tenantId, item.roomTitle, item.roomId, item.appointmentDate, item.appointmentTime, reason)
                MessageUtils.showInfoDialog(this, "Đã hủy lịch", "Người thuê sẽ nhận được thông báo.")
            }.setNegativeButton("Đóng", null).show()
    }

    // Xác nhận khách đã đến xem
    override fun onMarkAsViewed(item: AppointmentItem) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Khách đã đến xem")
            .setMessage("Xác nhận khách ${item.tenantName} đã đến xem phòng. Slot sẽ được mở lại. Nếu khách quyết định thuê, bạn có thể chọn 'Xác nhận cho thuê' sau đó.")
            .setPositiveButton("Khách đã đến") { _, _ ->
                viewModel.markAsViewed(item.id,
                    onSuccess = { MessageUtils.showSuccessDialog(this, "Đã cập nhật", "Cảm ơn! Slot đã được mở lại.") },
                    onFailure = { MessageUtils.showErrorDialog(this, "Lỗi", it) }
                )
            }.setNegativeButton("Hủy", null).show()
    }

    // Xác nhận khách không đến
    override fun onMarkAsNoShow(item: AppointmentItem) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Khách không đến")
            .setMessage("Xác nhận khách ${item.tenantName} đã không đến xem phòng. Slot sẽ được mở lại.")
            .setPositiveButton("Khách không đến") { _, _ ->
                viewModel.markAsNoShow(item.id,
                    onSuccess = { MessageUtils.showInfoDialog(this, "Đã ghi nhận", "Slot đã được mở lại cho người khác.") },
                    onFailure = { MessageUtils.showErrorDialog(this, "Lỗi", it) }
                )
            }.setNegativeButton("Hủy", null).show()
    }

    override fun onMarkAsNotRented(item: AppointmentItem) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Khách không thuê phòng?")
            .setMessage("Xác nhận khách ${item.tenantName} đã xem nhưng không thuê phòng. Phòng vẫn còn trống và sẽ tiếp tục nhận lịch hẹn mới.")
            .setPositiveButton("Xác nhận") { _, _ ->
                viewModel.markAsNotRented(item.id,
                    onSuccess = { MessageUtils.showInfoDialog(this, "Đã ghi nhận", "Phòng vẫn còn trống. Chúc bạn sớm tìm được người thuê!") },
                    onFailure = { MessageUtils.showErrorDialog(this, "Lỗi", it) }
                )
            }.setNegativeButton("Hủy", null).show()
    }

    override fun onMarkAsRented(item: AppointmentItem) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Xác nhận cho thuê")
            .setMessage("Phòng sẽ chuyển sang 'Đã thuê', bài đăng bị ẩn và các lịch hẹn khác sẽ bị hủy tự động.")
            .setPositiveButton("Xác nhận") { _, _ ->
                viewModel.markAsRented(item.id, item.roomId, item.tenantId, item.roomTitle, item.appointmentDate, item.appointmentTime)
                MessageUtils.showSuccessDialog(this, "Chúc mừng!", "Phòng đã được đánh dấu cho thuê thành công.")
            }.setNegativeButton("Hủy", null).show()
    }

    // Khách bấm xác nhận xem phòng khi chủ bài đăng duyệt lịch hẹn
    override fun onTenantConfirm(item: AppointmentItem) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Xác nhận sẽ đến")
            .setMessage("Bạn chắc chắn sẽ đến xem phòng vào ${item.appointmentDateDisplay} lúc ${item.appointmentTime}?")
            .setPositiveButton("Xác nhận") { _, _ ->
                viewModel.tenantConfirmAppointment(item.id, item.landlordId, item.roomTitle)
                MessageUtils.showSuccessDialog(this, "Đã xác nhận", "Chủ trọ sẽ nhận được thông báo. Hẹn gặp bạn!")
            }.setNegativeButton("Hủy", null).show()
    }

    // Khách hủy lịch hẹn khi trạng thái lịch hẹn sẽ xác nhận
    override fun onTenantCancel(item: AppointmentItem) {
        val input = EditText(this).apply { hint = "Lý do hủy (VD: Bận đột xuất)"; setPadding(48, 32, 48, 32) }
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Hủy lịch hẹn")
            .setView(input)
            .setMessage("Việc hủy lịch sẽ thông báo cho chủ trọ. Vui lòng nhập lý do (tùy chọn):")
            .setPositiveButton("Hủy lịch") { _, _ ->
                val reason = input.text.toString().trim().ifEmpty { "Không có lý do" }
                viewModel.tenantRejectAppointment(item.id, item.landlordId, item.roomTitle, item.roomId, item.appointmentDate, item.appointmentTime, item.status, reason)
                MessageUtils.showInfoDialog(this, "Đã hủy", "Lịch hẹn đã được hủy thành công.")
            }.setNegativeButton("Đóng", null).show()
    }

    // Khách hủy lịch hẹn khi trạng thái lịch hẹn đang chờ duyệt
    override fun onCancelPending(item: AppointmentItem) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Hủy yêu cầu đặt lịch")
            .setMessage("Hủy yêu cầu đặt lịch xem phòng \"${item.roomTitle}\"?")
            .setPositiveButton("Hủy lịch") { _, _ ->
                viewModel.cancelPendingAppointment(item.id, item.landlordId, item.roomTitle,
                    onSuccess = { MessageUtils.showSuccessDialog(this, "Thành công", "Đã hủy yêu cầu đặt lịch.") },
                    onFailure = { MessageUtils.showErrorDialog(this, "Lỗi", it) }
                )
            }.setNegativeButton("Đóng", null).show()
    }

    // Chủ trọ không đến
    override fun onMarkAsLandlordNoShow(item: AppointmentItem) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Chủ trọ không đến?")
            .setMessage("Xác nhận chủ trọ ${item.landlordName} không đến đúng giờ hẹn xem phòng \"${item.roomTitle}\".")
            .setPositiveButton("Xác nhận") { _, _ ->
                viewModel.markAsLandlordNoShow(item.id,
                    onSuccess = { MessageUtils.showInfoDialog(this, "Đã ghi nhận", "Chúng tôi đã ghi nhận báo cáo của bạn.") },
                    onFailure = { MessageUtils.showErrorDialog(this, "Lỗi", it) }
                )
            }.setNegativeButton("Hủy", null).show()
    }

    override fun onReopenRoom(item: AppointmentItem) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Mở lại phòng?")
            .setMessage("Xác nhận mở lại phòng \"${item.roomTitle}\" để cho thuê. Phòng sẽ xuất hiện lại trong danh sách tìm kiếm.")
            .setPositiveButton("Mở lại phòng") { _, _ ->
                viewModel.reopenRoom(item.roomId, item.id, item.tenantId,
                    onSuccess = { MessageUtils.showInfoDialog(this, "Thành công", "Phòng đã được mở lại.") },
                    onFailure = { MessageUtils.showErrorDialog(this, "Lỗi", it) }
                )
            }.setNegativeButton("Hủy", null).show()
    }

    override fun onEditSchedule(item: AppointmentItem) {
        showEditScheduleDialog(item)
    }

    private fun showEditScheduleDialog(item: AppointmentItem) {
        var newDate = ""; var newDateDisplay = ""; var newTime = ""
        var newDateMs = 0L; var newTimestampMs = 0L

        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(64, 32, 64, 32)
        }
        val tvDate = TextView(this).apply { text = "Chọn ngày mới"; textSize = 14f; setPadding(0, 32, 0, 32) }
        val tvTime = TextView(this).apply { text = "Chọn giờ mới"; textSize = 14f; setPadding(0, 32, 0, 32) }

        tvDate.setOnClickListener {
            val cal = java.util.Calendar.getInstance()
            android.app.DatePickerDialog(this, { _, y, m, d ->
                val c = java.util.Calendar.getInstance().apply { set(y, m, d, 0, 0, 0); set(java.util.Calendar.MILLISECOND, 0) }
                newDate = String.format("%02d/%02d/%04d", d, m + 1, y)
                newDateDisplay = "${java.text.SimpleDateFormat("EEEE", java.util.Locale("vi","VN")).format(c.time).replaceFirstChar { it.uppercase() }}, $newDate"
                newDateMs = c.timeInMillis
                tvDate.text = newDateDisplay
            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH))
                .also { it.datePicker.minDate = System.currentTimeMillis() }.show()
        }

        tvTime.setOnClickListener {
            val cal = java.util.Calendar.getInstance()
            android.app.TimePickerDialog(this, { _, h, min ->
                newTime = String.format("%02d:%02d", h, min)
                tvTime.text = "Giờ: $newTime"
                if (newDateMs > 0) newTimestampMs = newDateMs + h * 3600_000L + min * 60_000L
            }, cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE), true).show()
        }

        container.addView(tvDate); container.addView(tvTime)

        androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Đổi lịch hẹn").setView(container)
            .setPositiveButton("Cập nhật") { _, _ ->
                if (newDate.isEmpty() || newTime.isEmpty()) { MessageUtils.showInfoDialog(this, "Thiếu thông tin", "Vui lòng chọn đủ ngày và giờ."); return@setPositiveButton }
                viewModel.editPendingAppointment(item.id, item.landlordId, item.roomTitle,
                    newDate, newDateDisplay, newTime, newDateMs, newTimestampMs,
                    onSuccess = { MessageUtils.showSuccessDialog(this, "Thành công", "Lịch hẹn đã được cập nhật.") },
                    onFailure = { MessageUtils.showErrorDialog(this, "Lỗi", it) }
                )
            }.setNegativeButton("Hủy", null).show()
    }
}
