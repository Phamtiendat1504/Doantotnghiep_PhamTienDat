package com.example.doantotnghiep.View.Auth

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.ViewModel.BookingViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class MyAppointmentsActivity : AppCompatActivity() {

    private lateinit var layoutAppointments: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var btnBack: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var menuFilter: AutoCompleteTextView
    private lateinit var edtSearch: com.google.android.material.textfield.TextInputEditText
    private lateinit var tabLayoutRole: com.google.android.material.tabs.TabLayout
    private var currentFilterIndex = 0
    private var searchQuery = ""
    private var isDualMode = false      // true khi user đã xác minh (à 2 tab)
    private var currentTab = 0          // 0 = Lịch tôi đặt | 1 = Khách hẹn phòng tôi

    private val bookingViewModel: BookingViewModel by viewModels()
    // Lưu riêng biệt 2 list để tránh bị ghi đè khi chuyển tab
    private var tenantList = listOf<Map<String, Any>>()
    private var landlordList = listOf<Map<String, Any>>()
    // allAppointments luôn trỏ vào đúng list theo tab hiện tại
    private val allAppointments: List<Map<String, Any>>
        get() = if (isDualMode && currentTab == 1) landlordList else tenantList

    private enum class PendingAction {
        NONE, CONFIRM_LANDLORD, REJECT_LANDLORD, TENANT_CONFIRM, TENANT_CANCEL, MARK_AS_RENTED
    }

    private var pendingAction = PendingAction.NONE
    private val conflictMap = mutableMapOf<String, Int>()
    private var isReminderShown = false
    private var roomRentedNoticeListener: ListenerRegistration? = null
    private var isRentedDialogShowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_appointments)

        initViews()
        setupObservers()

        btnBack.setOnClickListener { finish() }

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) { finish(); return }
        listenRoomRentedNotices(uid)

        // Kiểm tra isVerified trước để quyết định dual-tab hay single-tab
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val isVerified = doc.getBoolean("isVerified") ?: false
                val role = doc.getString("role") ?: ""
                val effectiveRole = if (isVerified || role == "admin") "verified" else "user"

                // Đánh dấu đã đọc tất cả lịch hẹn → badge điện thoại tự biến mất
                com.example.doantotnghiep.repository.AppointmentRepository()
                    .markAllAppointmentsRead(uid, effectiveRole)

                if (isVerified || role == "admin") {
                    isDualMode = true
                    setupDualTabs()
                    bookingViewModel.fetchBothAppointments()
                } else {
                    bookingViewModel.fetchAppointmentsByRole()
                }
            }
            .addOnFailureListener { bookingViewModel.fetchAppointmentsByRole() }
    }

    private fun listenRoomRentedNotices(uid: String) {
        roomRentedNoticeListener?.remove()
        roomRentedNoticeListener = FirebaseFirestore.getInstance()
            .collection("notifications")
            .whereEqualTo("userId", uid)
            .whereEqualTo("type", "room_already_rented")
            .whereEqualTo("seen", false)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null || snapshots.isEmpty) return@addSnapshotListener
                if (isRentedDialogShowing) return@addSnapshotListener

                val doc = snapshots.documents.firstOrNull() ?: return@addSnapshotListener
                val title = doc.getString("title") ?: "Lịch hẹn đã bị hủy"
                val message = doc.getString("message")
                    ?: "Phòng đã có người thuê. Lịch hẹn của bạn đã bị hủy tự động."

                isRentedDialogShowing = true
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton("Đã hiểu") { _, _ ->
                        doc.reference.update(
                            mapOf(
                                "seen" to true,
                                "isRead" to true
                            )
                        )
                        isRentedDialogShowing = false
                    }
                    .setOnDismissListener { isRentedDialogShowing = false }
                    .show()
            }
    }

    private fun initViews() {
        layoutAppointments = findViewById(R.id.layoutAppointments)
        tvEmpty = findViewById(R.id.tvEmpty)
        btnBack = findViewById(R.id.btnBack)
        progressBar = findViewById(R.id.progressBar)
        menuFilter = findViewById(R.id.menuFilter)
        edtSearch = findViewById(R.id.edtSearch)
        tabLayoutRole = findViewById(R.id.tabLayoutRole)

        edtSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s.toString().trim()
                updateListByTab()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        val swipeRefreshLayout: androidx.swiperefreshlayout.widget.SwipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            if (isDualMode) bookingViewModel.fetchBothAppointments()
            else bookingViewModel.fetchAppointmentsByRole()
            swipeRefreshLayout.isRefreshing = false
        }
    }

    /** Hiện TabLayout 2 tab cho user đã xác minh */
    private fun setupDualTabs() {
        tabLayoutRole.visibility = android.view.View.VISIBLE
        tabLayoutRole.addTab(tabLayoutRole.newTab().setText("📅 Lịch tôi đặt"))
        tabLayoutRole.addTab(tabLayoutRole.newTab().setText("🏠 Khách hẹn phòng tôi"))
        tabLayoutRole.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                // Không cần gán allAppointments vì getter tự trỏ đúng list
                updateFilterMenu(currentTab == 1)
                updateListByTab()
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun updateListByTab() {
        val isLandlordView = if (isDualMode) currentTab == 1
                             else (bookingViewModel.userRole.value ?: "user").let { it == "landlord" || it == "admin" || it == "verified" }
        var filtered = when (currentFilterIndex) {
            1 -> allAppointments.filter { it["status"] == "pending" }
            2 -> allAppointments.filter { it["status"] == "confirmed" }
            3 -> allAppointments.filter { it["status"] == "tenant_confirmed" }
            4 -> allAppointments.filter { 
                val s = it["status"] as? String ?: ""
                s != "pending" && s != "confirmed" && s != "tenant_confirmed"
            }
            else -> allAppointments // 0 - chipAll
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

        layoutAppointments.removeAllViews()
        if (filtered.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
        } else {
            tvEmpty.visibility = View.GONE
            
            // Sắp xếp: Thời gian sớm nhất lên đầu cho các tab đang xử lý
            val isHistory = currentFilterIndex == 4
            val sorted = if (isHistory) {
                filtered.sortedByDescending { it["createdAt"] as? Long ?: 0L }
            } else {
                filtered.sortedBy { getDateTimeValue(it["date"] as? String ?: "", it["time"] as? String ?: "") }
            }

            for (doc in sorted) {
                addAppointmentToLayout(doc, isLandlordView)
            }
        }
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

    private fun setupObservers() {
        // Quan sát danh sách lịch hẹn từ ViewModel (chế độ single-role)
        bookingViewModel.appointments.observe(this) { appointmentList ->
            if (isDualMode) return@observe  // dual-tab dùng observer riêng bên dưới
            tenantList = appointmentList
            val role = bookingViewModel.userRole.value ?: "user"
            val isLandlord = role == "landlord" || role == "admin" || role == "verified"

            updateFilterMenu(isLandlord)

            if (isLandlord) {
                val hasNewlyConfirmed = appointmentList.any { it["status"] == "tenant_confirmed" }
                if (hasNewlyConfirmed && !isReminderShown) {
                    showTenantConfirmedReminderDialog()
                }
                val roomIds = appointmentList.mapNotNull { it["roomId"] as? String }.distinct()
                roomIds.forEach { rid -> bookingViewModel.listenTimeConflicts(rid) }
            }

            updateListByTab()
        }

        // Quan sát lịch hẹn TÔI ĐẶT (tab 0 - chế độ dual-tab)
        bookingViewModel.tenantAppointments.observe(this) { list ->
            if (!isDualMode) return@observe
            tenantList = list  // Luôn cập nhật, dù đang ở tab nào
            if (currentTab == 0) {
                updateFilterMenu(isLandlord = false)
                updateListByTab()
            }
        }

        // Quan sát lịch hẹn KHÁCH HẸN PHÒNG TÔI (tab 1 - chế độ dual-tab)
        bookingViewModel.landlordAppointments.observe(this) { list ->
            if (!isDualMode) return@observe
            landlordList = list  // Luôn cập nhật, dù đang ở tab nào
            if (currentTab == 1) {
                updateFilterMenu(isLandlord = true)
                val hasNewlyConfirmed = list.any { it["status"] == "tenant_confirmed" }
                if (hasNewlyConfirmed && !isReminderShown) showTenantConfirmedReminderDialog()
                val roomIds = list.mapNotNull { it["roomId"] as? String }.distinct()
                roomIds.forEach { rid -> bookingViewModel.listenTimeConflicts(rid) }
                updateListByTab()
            }
        }

        // Quan sát xung đột thời gian
        bookingViewModel.timeConflicts.observe(this) { conflicts ->
            conflictMap.clear()
            conflictMap.putAll(conflicts)
        }

        // Quan sát kết quả action
        bookingViewModel.bookingResult.observe(this) { success ->
            if (success == true) {
                bookingViewModel.resetBookingResult()
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
                    PendingAction.MARK_AS_RENTED ->
                        MessageUtils.showSuccessDialog(this, "Chúc mừng!", "Phòng đã được đánh dấu là Đã cho thuê. Các lịch hẹn khác đã được tự động hủy.")
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
            .setTitle("💡 Khách đã xác nhận đến!")
            .setMessage("Bạn có lịch hẹn khách đã xác nhận sẽ đi xem phòng. Nếu hai bên chốt thuê thành công, đừng quên bấm 'Xác nhận đã cho thuê' để hệ thống tự động ẩn bài và hủy các lịch hẹn khác giúp bạn nhé!")
            .setPositiveButton("Tôi đã hiểu", null)
            .show()
    }

    private fun updateFilterMenu(isLandlord: Boolean) {
        val options = if (isLandlord) {
            arrayOf("Tất cả", "Cần xác nhận", "Chờ khách xác nhận", "Khách sẽ đến", "Lịch sử")
        } else {
            arrayOf("Tất cả", "Đang chờ duyệt", "Chờ bạn xác nhận", "Bạn đã xác nhận", "Lịch sử")
        }
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, options)
        menuFilter.setAdapter(adapter)

        if (menuFilter.text.toString().isEmpty()) {
            menuFilter.setText(options[currentFilterIndex], false)
        }

        menuFilter.setOnItemClickListener { _, _, position, _ ->
            currentFilterIndex = position
            updateListByTab()
            // Ẩn bàn phím/focus sau khi chọn
            menuFilter.clearFocus()
        }
    }

    private fun addSectionHeader(title: String, isVisible: Boolean) {
        if (!isVisible) return
        val tvHeader = TextView(this).apply {
            text = title
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(0xFF333333.toInt())
            setPadding(dpToPx(18), dpToPx(16), dpToPx(16), dpToPx(8))
        }
        layoutAppointments.addView(tvHeader)
    }

    private fun addAppointmentToLayout(doc: Map<String, Any>, isLandlord: Boolean) {
        val date = doc["date"] as? String ?: ""
        val time = doc["time"] as? String ?: ""
        val conflictKey = "${date}_${time}"
        val hasConflict = (conflictMap[conflictKey] ?: 0) > 1

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
            time = time,
            note = doc["note"] as? String ?: "",
            status = doc["status"] as? String ?: "pending",
            rejectReason = doc["rejectReason"] as? String ?: "",
            isLandlord = isLandlord,
            tenantId = doc["tenantId"] as? String ?: "",
            landlordId = doc["landlordId"] as? String ?: "",
            roomId = doc["roomId"] as? String ?: "",
            hasConflict = hasConflict
        )
        layoutAppointments.addView(card)
    }

    private fun createAppointmentCard(
        docId: String, tenantName: String, tenantPhone: String, tenantGender: String,
        landlordName: String, landlordPhone: String, roomTitle: String, roomAddress: String,
        roomImageUrl: String, dateDisplay: String, time: String, note: String, status: String,
        rejectReason: String, isLandlord: Boolean, tenantId: String, landlordId: String, roomId: String,
        hasConflict: Boolean
    ): CardView {
        val card = CardView(this)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
        card.layoutParams = params
        card.radius = dpToPx(12).toFloat()
        card.cardElevation = dpToPx(4).toFloat()

        val mainLayout = LinearLayout(this)
        mainLayout.orientation = LinearLayout.HORIZONTAL
        mainLayout.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))

        val imgRoom = ImageView(this)
        val imgParams = LinearLayout.LayoutParams(dpToPx(80), dpToPx(80))
        imgRoom.layoutParams = imgParams
        imgRoom.scaleType = ImageView.ScaleType.CENTER_CROP
        imgRoom.background = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dpToPx(8).toFloat()
            setColor(0xFFF5F5F5.toInt())
        }
        imgRoom.clipToOutline = true
        Glide.with(this).load(roomImageUrl).placeholder(android.R.drawable.ic_menu_report_image).into(imgRoom)
        mainLayout.addView(imgRoom)

        val contentLayout = LinearLayout(this)
        contentLayout.orientation = LinearLayout.VERTICAL
        val contentParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        contentParams.marginStart = dpToPx(12)
        contentLayout.layoutParams = contentParams

        // Trạng thái
        val tvStatus = TextView(this)
        when (status) {
            "pending" -> {
                tvStatus.text = getString(R.string.status_pending)
                tvStatus.setTextColor(0xFFE65100.toInt())
                tvStatus.setBackgroundColor(0xFFFFF3E0.toInt())
            }
            "confirmed" -> {
                tvStatus.text = if (isLandlord) "Chờ khách xác nhận" else "Chủ trọ đã xác nhận"
                tvStatus.setTextColor(0xFF2E7D32.toInt())
                tvStatus.setBackgroundColor(0xFFE8F5E9.toInt())
            }
            "tenant_confirmed" -> {
                tvStatus.text = if (isLandlord) "Khách đã xác nhận sẽ đến" else "Bạn đã xác nhận sẽ đến"
                tvStatus.setTextColor(0xFF1565C0.toInt())
                tvStatus.setBackgroundColor(0xFFE3F2FD.toInt())
            }
            "rejected" -> {
                tvStatus.text = getString(R.string.status_rejected)
                tvStatus.setTextColor(0xFFD32F2F.toInt())
                tvStatus.setBackgroundColor(0xFFFFEBEE.toInt())
            }
            "cancelled_by_tenant" -> {
                tvStatus.text = if (isLandlord) "Khách đã hủy" else "Bạn đã hủy"
                tvStatus.setTextColor(0xFFD32F2F.toInt())
                tvStatus.setBackgroundColor(0xFFFFEBEE.toInt())
            }
            "completed_rented" -> {
                tvStatus.text = "Đã thuê thành công"
                tvStatus.setTextColor(0xFFFFFFFF.toInt())
                tvStatus.setBackgroundColor(0xFF2E7D32.toInt())
            }
            "cancelled_by_system" -> {
                tvStatus.text = "Hệ thống tự hủy"
                tvStatus.setTextColor(0xFF757575.toInt())
                tvStatus.setBackgroundColor(0xFFEEEEEE.toInt())
            }
        }
        tvStatus.textSize = 11f
        tvStatus.setTypeface(tvStatus.typeface, android.graphics.Typeface.BOLD)
        tvStatus.setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3))
        contentLayout.addView(tvStatus)

        // Tên phòng
        val tvRoom = TextView(this)
        tvRoom.text = roomTitle
        tvRoom.textSize = 14f
        tvRoom.setTextColor(0xFF1A1A2E.toInt())
        tvRoom.setTypeface(tvRoom.typeface, android.graphics.Typeface.BOLD)
        tvRoom.setPadding(0, dpToPx(5), 0, 0)
        tvRoom.maxLines = 2
        contentLayout.addView(tvRoom)

        // Địa chỉ
        val tvAddr = TextView(this)
        tvAddr.text = roomAddress
        tvAddr.textSize = 11f
        tvAddr.setTextColor(0xFF666666.toInt())
        tvAddr.maxLines = 1
        contentLayout.addView(tvAddr)

        // Ngày giờ + Cảnh báo
        val dateTimeLayout = LinearLayout(this)
        dateTimeLayout.orientation = LinearLayout.HORIZONTAL
        dateTimeLayout.gravity = android.view.Gravity.CENTER_VERTICAL
        dateTimeLayout.setPadding(0, dpToPx(4), 0, 0)

        val tvDateTime = TextView(this)
        tvDateTime.text = "$dateDisplay  |  $time"
        tvDateTime.textSize = 12f
        tvDateTime.setTextColor(0xFF1976D2.toInt())
        tvDateTime.setTypeface(tvDateTime.typeface, android.graphics.Typeface.BOLD)
        dateTimeLayout.addView(tvDateTime)

        if (isLandlord && hasConflict && (status == "pending" || status == "confirmed" || status == "tenant_confirmed")) {
            val tvConflict = TextView(this)
            tvConflict.text = " (Trùng lịch)"
            tvConflict.textSize = 10f
            tvConflict.setTextColor(0xFFD32F2F.toInt())
            tvConflict.setTypeface(null, android.graphics.Typeface.ITALIC)
            dateTimeLayout.addView(tvConflict)
        }
        contentLayout.addView(dateTimeLayout)

        // Thông tin người hẹn
        if (isLandlord) {
            val tvTenant = TextView(this)
            tvTenant.text = "Người thuê: $tenantName ($tenantGender) - $tenantPhone"
            tvTenant.textSize = 11f
            tvTenant.setTextColor(0xFF333333.toInt())
            contentLayout.addView(tvTenant)
        } else {
            val tvLandlord = TextView(this)
            tvLandlord.text = "Chủ trọ: $landlordName - $landlordPhone"
            tvLandlord.textSize = 11f
            tvLandlord.setTextColor(0xFF333333.toInt())
            contentLayout.addView(tvLandlord)
        }

        // Ghi chú
        if (note.isNotEmpty()) {
            val tvNote = TextView(this)
            tvNote.text = "Ghi chú: $note"
            tvNote.textSize = 11f
            tvNote.setTextColor(0xFF666666.toInt())
            contentLayout.addView(tvNote)
        }

        mainLayout.addView(contentLayout)

        val outerLayout = LinearLayout(this)
        outerLayout.orientation = LinearLayout.VERTICAL
        outerLayout.addView(mainLayout)

        // NÚT HÀNH ĐỘNG
        val btnLayout = LinearLayout(this)
        btnLayout.orientation = LinearLayout.HORIZONTAL
        btnLayout.setPadding(dpToPx(12), 0, dpToPx(12), dpToPx(12))
        btnLayout.gravity = android.view.Gravity.END

        if (isLandlord) {
            if (status == "pending") {
                val btnReject = createButton("Từ chối", 0xFFFFEBEE.toInt(), 0xFFD32F2F.toInt())
                btnReject.setOnClickListener { rejectAppointment(docId, tenantId, roomTitle) }
                btnLayout.addView(btnReject)

                val space = View(this).apply { layoutParams = LinearLayout.LayoutParams(dpToPx(10), 0) }
                btnLayout.addView(space)

                val btnConfirm = createButton("Xác nhận", 0xFFE8F5E9.toInt(), 0xFF2E7D32.toInt())
                btnConfirm.setOnClickListener { confirmAppointment(docId, tenantId, roomTitle, dateDisplay, time) }
                btnLayout.addView(btnConfirm)
                outerLayout.addView(btnLayout)
            } else if (status == "tenant_confirmed") {
                val tipLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = android.view.Gravity.END
                    setPadding(dpToPx(12), 0, dpToPx(12), dpToPx(10))
                }
                val tvTip = TextView(this).apply {
                    text = "💡 Khách đã xác nhận đến. Nếu đã chốt thuê, hãy bấm:"
                    textSize = 9f
                    setTextColor(0xFF1976D2.toInt())
                    setPadding(0, 0, 0, dpToPx(4))
                }
                tipLayout.addView(tvTip)

                val btnRented = createButton("Xác nhận đã cho thuê", 0xFFE3F2FD.toInt(), 0xFF1976D2.toInt())
                btnRented.setOnClickListener {
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Xác nhận cho thuê")
                        .setMessage("Hệ thống sẽ chuyển trạng thái phòng sang 'Đã thuê', ẩn bài đăng và hủy các lịch hẹn khác của phòng này?")
                        .setPositiveButton("Xác nhận", { _, _ -> markAsRented(docId, roomId, tenantId, roomTitle) })
                        .setNegativeButton("Hủy", null).show()
                }
                tipLayout.addView(btnRented)
                outerLayout.addView(tipLayout)
            }
        } else {
            if (status == "pending") {
                val btnCancel = createButton("Hủy đặt lịch", 0xFFFFEBEE.toInt(), 0xFFD32F2F.toInt())
                btnCancel.setOnClickListener { showCancelPendingDialog(docId, landlordId, roomTitle) }
                btnLayout.addView(btnCancel)

                val space = View(this).apply { layoutParams = LinearLayout.LayoutParams(dpToPx(10), 0) }
                btnLayout.addView(space)

                val btnEdit = createButton("Đổi lịch", 0xFFE3F2FD.toInt(), 0xFF1976D2.toInt())
                btnEdit.setOnClickListener { showEditScheduleDialog(docId, landlordId, roomTitle) }
                btnLayout.addView(btnEdit)
                outerLayout.addView(btnLayout)
            } else if (status == "confirmed") {
                val btnCancel = createButton("Không đến nữa", 0xFFFFEBEE.toInt(), 0xFFD32F2F.toInt())
                btnCancel.setOnClickListener { tenantRejectAppointment(docId, landlordId, roomTitle) }
                btnLayout.addView(btnCancel)

                val space = View(this).apply { layoutParams = LinearLayout.LayoutParams(dpToPx(10), 0) }
                btnLayout.addView(space)

                val btnConfirm = createButton("Xác nhận đi xem", 0xFFE8F5E9.toInt(), 0xFF2E7D32.toInt())
                btnConfirm.setOnClickListener { tenantConfirmAppointment(docId, landlordId, roomTitle) }
                btnLayout.addView(btnConfirm)
                outerLayout.addView(btnLayout)
            }
        }

        card.addView(outerLayout)
        return card
    }

    private fun createButton(text: String, bgColor: Int, textColor: Int): TextView {
        return TextView(this).apply {
            this.text = text
            this.textSize = 12f
            this.setTextColor(textColor)
            this.setTypeface(null, android.graphics.Typeface.BOLD)
            this.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
            this.background = android.graphics.drawable.GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = dpToPx(8).toFloat()
            }
        }
    }

    private fun confirmAppointment(docId: String, tenantId: String, roomTitle: String, date: String, time: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Xác nhận lịch hẹn")
            .setMessage("Bạn xác nhận đồng ý lịch hẹn xem phòng này?")
            .setPositiveButton("Xác nhận") { _, _ ->
                pendingAction = PendingAction.CONFIRM_LANDLORD
                bookingViewModel.confirmAppointment(docId, tenantId, roomTitle, date, time)
            }
            .setNegativeButton("Hủy", null).show()
    }

    private fun rejectAppointment(docId: String, tenantId: String, roomTitle: String) {
        val input = EditText(this).apply { hint = "Lý do từ chối"; setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12)) }
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Từ chối").setView(input)
            .setPositiveButton("Gửi") { _, _ ->
                pendingAction = PendingAction.REJECT_LANDLORD
                bookingViewModel.rejectAppointment(docId, tenantId, roomTitle, input.text.toString())
            }.setNegativeButton("Hủy", null).show()
    }

    private fun markAsRented(docId: String, roomId: String, tenantId: String, roomTitle: String) {
        pendingAction = PendingAction.MARK_AS_RENTED
        bookingViewModel.markAsRented(docId, roomId, tenantId, roomTitle)
    }

    private fun tenantConfirmAppointment(docId: String, landlordId: String, roomTitle: String) {
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Xác nhận").setMessage("Bạn chắc chắn sẽ đến xem phòng đúng hẹn?")
            .setPositiveButton("Xác nhận") { _, _ ->
                pendingAction = PendingAction.TENANT_CONFIRM
                bookingViewModel.tenantConfirmAppointment(docId, landlordId, roomTitle)
            }.setNegativeButton("Hủy", null).show()
    }

    private fun tenantRejectAppointment(docId: String, landlordId: String, roomTitle: String) {
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Hủy hẹn").setMessage("Bạn muốn hủy lịch hẹn này?")
            .setPositiveButton("Đồng ý") { _, _ ->
                pendingAction = PendingAction.TENANT_CANCEL
                bookingViewModel.tenantRejectAppointment(docId, landlordId, roomTitle)
            }.setNegativeButton("Hủy", null).show()
    }

    private fun showCancelPendingDialog(docId: String, landlordId: String, roomTitle: String) {
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Hủy yêu cầu").setMessage("Bạn muốn hủy yêu cầu đặt lịch này?")
            .setPositiveButton("Hủy lịch") { _, _ ->
                pendingAction = PendingAction.TENANT_CANCEL
                bookingViewModel.cancelPendingAppointment(docId, landlordId, roomTitle,
                    onSuccess = { MessageUtils.showSuccessDialog(this, "Thành công", "Đã hủy yêu cầu.") },
                    onFailure = { MessageUtils.showErrorDialog(this, "Lỗi", it) })
            }.setNegativeButton("Đóng", null).show()
    }

    private fun showEditScheduleDialog(docId: String, landlordId: String, roomTitle: String) {
        var selectedDate = ""; var selectedDateDisplay = ""; var selectedTime = ""
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dpToPx(20), dpToPx(10), dpToPx(20), dpToPx(10)) }
        val tvDate = TextView(this).apply { text = "📅 Chọn ngày mới"; textSize = 14f; setPadding(0, dpToPx(10), 0, dpToPx(10)) }
        val tvTime = TextView(this).apply { text = "⏰ Chọn giờ mới"; textSize = 14f; setPadding(0, dpToPx(10), 0, dpToPx(10)) }
        
        tvDate.setOnClickListener {
            val cal = java.util.Calendar.getInstance()
            android.app.DatePickerDialog(this, { _, y, m, d ->
                val c = java.util.Calendar.getInstance().apply { set(y, m, d) }
                selectedDate = String.format("%02d/%02d/%04d", d, m + 1, y)
                selectedDateDisplay = "${java.text.SimpleDateFormat("EEEE", java.util.Locale("vi", "VN")).format(c.time)}, $selectedDate"
                tvDate.text = "📅 $selectedDateDisplay"
            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show()
        }
        tvTime.setOnClickListener {
            val cal = java.util.Calendar.getInstance()
            android.app.TimePickerDialog(this, { _, h, min ->
                selectedTime = String.format("%02d:%02d", h, min)
                tvTime.text = "⏰ $selectedTime"
            }, cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE), true).show()
        }
        container.addView(tvDate); container.addView(tvTime)
        
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Đổi lịch hẹn").setView(container)
            .setPositiveButton("Cập nhật") { _, _ ->
                if (selectedDate.isEmpty() || selectedTime.isEmpty()) return@setPositiveButton
                bookingViewModel.editPendingAppointment(docId, landlordId, roomTitle, selectedDate, selectedDateDisplay, selectedTime,
                    onSuccess = { MessageUtils.showSuccessDialog(this, "Thành công", "Lịch đã đổi.") },
                    onFailure = { MessageUtils.showErrorDialog(this, "Lỗi", it) })
            }.setNegativeButton("Hủy", null).show()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        roomRentedNoticeListener?.remove()
        super.onDestroy()
    }
}
