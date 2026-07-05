package com.example.doantotnghiep.View.Fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Model.Room
import com.example.doantotnghiep.Utils.AddressData
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.Utils.NumberFormatUtils
import com.example.doantotnghiep.View.Auth.LoginActivity
import com.example.doantotnghiep.View.Auth.LocationPickerActivity
import com.example.doantotnghiep.View.Auth.RegisterActivity
import com.example.doantotnghiep.View.Auth.VerifyLandlordActivity
import com.bumptech.glide.Glide
import com.example.doantotnghiep.Utils.PostNotificationHelper
import com.example.doantotnghiep.ViewModel.PostViewModel
import com.google.android.material.button.MaterialButton

import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.SimpleDateFormat
import java.util.*

class PostFragment : Fragment() {

    private var verifyRequiredView: View? = null
    private var postFormView: View? = null
    private var layoutGuest: View? = null

    private val imageUris = mutableListOf<Uri>()
    private val MAX_PHOTOS = 10
    private var isFormSetup = false
    private var userRoleChecked = false
    var isDirty = false

    private lateinit var viewModel: PostViewModel

    private var lastPostedTitle = ""
    private var lastPostedPrice = 0L
    private var lastPostedLocation = ""
    private var currentOwnerAvatarUrl = ""
    private var postLoadingDialog: AlertDialog? = null
    private var postQuotaDialog: AlertDialog? = null
    private var postQuotaTimer: CountDownTimer? = null
    private var upgradeSlotsDialog: AlertDialog? = null
    private var paymentWarningDialog: AlertDialog? = null
    private var paymentQrDialog: AlertDialog? = null


    private var otherFeeContainer: LinearLayout? = null
    private val otherFeeRows = mutableListOf<Pair<EditText, EditText>>()
    private var furnitureContainer: LinearLayout? = null
    private val furnitureRows = mutableListOf<Pair<EditText, EditText>>()
    private var serviceContainer: LinearLayout? = null
    private val serviceRows = mutableListOf<Pair<EditText, EditText>>()

    private fun addFurnitureRow(initName: String = "", initQty: String = "") {
        val container = furnitureContainer ?: return
        val ctx = context ?: return
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d + 0.5f).toInt()
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(42))
                .apply { bottomMargin = dp(6) }
        }
        val edtName = EditText(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            hint = "Tên đồ dùng (VD: Sofa, Tủ lạnh...)"
            textSize = 13f
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setPadding(dp(12), 0, dp(4), 0)
            setBackgroundResource(R.drawable.bg_edit_post)
            if (initName.isNotEmpty()) setText(initName)
        }
        val edtQty = EditText(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(dp(72), LinearLayout.LayoutParams.MATCH_PARENT)
                .apply { marginStart = dp(6) }
            hint = "SL"
            textSize = 13f
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            gravity = android.view.Gravity.CENTER
            setPadding(dp(8), 0, dp(8), 0)
            setBackgroundResource(R.drawable.bg_edit_post)
            if (initQty.isNotEmpty()) setText(initQty)
        }
        val tvDel = android.widget.TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(dp(32), dp(32)).apply { marginStart = dp(6) }
            text = "✕"; textSize = 16f; gravity = android.view.Gravity.CENTER
            setTextColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.text_secondary))
        }
        val pair = edtName to edtQty
        furnitureRows.add(pair)
        row.addView(edtName); row.addView(edtQty); row.addView(tvDel)
        container.addView(row)
        tvDel.setOnClickListener { container.removeView(row); furnitureRows.remove(pair) }
    }

    private fun addServiceRow(initName: String = "", initPrice: String = "") {
        val container = serviceContainer ?: return
        val ctx = context ?: return
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d + 0.5f).toInt()
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(42))
                .apply { bottomMargin = dp(6) }
        }
        val edtName = EditText(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            hint = "Tên dịch vụ (VD: Giặt đồ, Dọn phòng...)"
            textSize = 13f
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setPadding(dp(12), 0, dp(4), 0)
            setBackgroundResource(R.drawable.bg_edit_post)
            if (initName.isNotEmpty()) setText(initName)
        }
        val edtPrice = EditText(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(dp(100), LinearLayout.LayoutParams.MATCH_PARENT)
                .apply { marginStart = dp(6) }
            hint = "đ/tháng"
            textSize = 12f
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            gravity = android.view.Gravity.CENTER
            setPadding(dp(8), 0, dp(8), 0)
            setBackgroundResource(R.drawable.bg_edit_post)
            if (initPrice.isNotEmpty()) setText(initPrice)
        }
        NumberFormatUtils.addFormatWatcher(edtPrice)
        val tvDel = android.widget.TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(dp(32), dp(32)).apply { marginStart = dp(6) }
            text = "✕"; textSize = 16f; gravity = android.view.Gravity.CENTER
            setTextColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.text_secondary))
        }
        val pair = edtName to edtPrice
        serviceRows.add(pair)
        row.addView(edtName); row.addView(edtPrice); row.addView(tvDel)
        container.addView(row)
        tvDel.setOnClickListener { container.removeView(row); serviceRows.remove(pair) }
    }

    private fun addFeeRow(initLabel: String = "", initPrice: String = "") {
        val container = otherFeeContainer ?: return
        val ctx = context ?: return
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d + 0.5f).toInt()
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(42))
                .apply { bottomMargin = dp(6) }
        }
        val edtLabel = EditText(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.5f)
            hint = "Tên khoản phí (VD: Phí vệ sinh)"
            textSize = 13f
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setPadding(dp(12), 0, dp(4), 0)
            setBackgroundResource(R.drawable.bg_edit_post)
            if (initLabel.isNotEmpty()) setText(initLabel)
        }
        val edtPrice = EditText(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                .apply { marginStart = dp(6) }
            hint = "Số tiền/tháng"
            textSize = 13f
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setPadding(dp(12), 0, dp(4), 0)
            setBackgroundResource(R.drawable.bg_edit_post)
            if (initPrice.isNotEmpty()) setText(initPrice)
        }
        val tvDel = android.widget.TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(dp(32), dp(32)).apply { marginStart = dp(6) }
            text = "✕"
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            setTextColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.text_secondary))
        }
        NumberFormatUtils.addFormatWatcher(edtPrice)
        val pair = edtLabel to edtPrice
        otherFeeRows.add(pair)
        row.addView(edtLabel); row.addView(edtPrice); row.addView(tvDel)
        container.addView(row)
        tvDel.setOnClickListener { container.removeView(row); otherFeeRows.remove(pair) }
    }

    private var rulesDialogShown = false  // Tránh show dialog quy định 2 lần do observer trigger lại
    private var lastPostUnlockDialogTimestamp: Long = -1L
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null
    private var selectedExpiryMs: Long? = null
    private var selectedLocationAddress: String = ""
    private var isPostProcessing = false

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val clipData = data?.clipData
            if (clipData != null) {
                val count = clipData.itemCount
                for (i in 0 until count) {
                    val uri = clipData.getItemAt(i).uri
                    if (imageUris.size < MAX_PHOTOS) { imageUris.add(uri); addPhotoToLayout(uri) }
                }
            } else if (data?.data != null) {
                val uri = data.data ?: return@registerForActivityResult
                if (imageUris.size < MAX_PHOTOS) { imageUris.add(uri); addPhotoToLayout(uri) }
            }
        }
    }

    private val pickLocationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult
        val lat = data.getDoubleExtra(LocationPickerActivity.EXTRA_RESULT_LAT, Double.NaN)
        val lng = data.getDoubleExtra(LocationPickerActivity.EXTRA_RESULT_LNG, Double.NaN)
        if (lat.isNaN() || lng.isNaN()) return@registerForActivityResult

        selectedLatitude = lat
        selectedLongitude = lng
        selectedLocationAddress = data.getStringExtra(LocationPickerActivity.EXTRA_RESULT_ADDRESS).orEmpty()

        val formView = postFormView ?: return@registerForActivityResult
        val tvPickedLocation = formView.findViewById<TextView>(R.id.tvPickedLocation)
        val edtAddress = formView.findViewById<EditText>(R.id.edtAddress)
        val spinnerWard = formView.findViewById<Spinner>(R.id.spinnerWard)

        // Auto-fill địa chỉ nếu ô đang trống
        if (edtAddress.text.isNullOrBlank() && selectedLocationAddress.isNotBlank()) {
            edtAddress.setText(selectedLocationAddress)
        }

        // Cố gắng sync spinnerWard từ địa chỉ bản đồ trả về
        // Sắp xếp ứng viên theo độ dài tên phường giảm dần để tránh "Phường 1" match nhầm trong "Phường 10"
        if (selectedLocationAddress.isNotBlank()) {
            val adapterCount = spinnerWard.adapter?.count ?: 0
            data class WardCandidate(val index: Int, val wardName: String)
            val candidates = (0 until adapterCount).mapNotNull { i ->
                val item = spinnerWard.adapter.getItem(i)?.toString() ?: return@mapNotNull null
                val wardName = if (item.contains("(")) item.substringBefore("(").trim() else item
                if (wardName.isNotBlank()) WardCandidate(i, wardName) else null
            }.sortedByDescending { it.wardName.length }

            for (candidate in candidates) {
                if (selectedLocationAddress.contains(candidate.wardName, ignoreCase = true)) {
                    spinnerWard.setSelection(candidate.index)
                    break
                }
            }
        }

        tvPickedLocation.text = if (selectedLocationAddress.isNotBlank()) {
            "Đã chọn: $selectedLocationAddress"
        } else {
            "Đã chọn tọa độ: %.6f, %.6f".format(Locale.US, lat, lng)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val frameLayout = FrameLayout(requireContext())
        frameLayout.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        verifyRequiredView = inflater.inflate(R.layout.layout_verify_required, container, false)
        postFormView = inflater.inflate(R.layout.fragment_post, container, false)
        layoutGuest = inflater.inflate(R.layout.layout_guest_post, container, false)

        verifyRequiredView?.visibility = View.GONE
        postFormView?.visibility = View.GONE
        layoutGuest?.visibility = View.GONE

        layoutGuest?.let { frameLayout.addView(it) }
        frameLayout.addView(verifyRequiredView)
        frameLayout.addView(postFormView)
        return frameLayout
    }

    //Xác minh danh tính -1-
    //Hàm vòng đời của Fragment: Kích hoạt khi toàn bộ các View (nút bấm, text, form...) đã được vẽ lên màn hình.
    //Chức năng chính trong file này: Kiểm tra trạng thái đăng nhập, theo dõi quyền của tài khoản (đã xác minh hay chưa) để quyết định hiển thị Form đăng bài hay giao diện chặn.
    //override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[PostViewModel::class.java]

        val isLoggedIn = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null
        if (!isLoggedIn) {
            showLoginRequired()
            return
        }

        viewModel.userObject.observe(viewLifecycleOwner) { user ->
            if (user == null) return@observe
            if (!isVisible) return@observe
            userRoleChecked = true
            val now = System.currentTimeMillis()
            val unlockAt = user.postingUnlockAt
            val isPostDelayActive = user.role != "admin" && user.isVerified && unlockAt > now

            if (isPostDelayActive) {
                showPostUnlockWaitingStatus(unlockAt, now)
                if (lastPostUnlockDialogTimestamp != unlockAt) {
                    lastPostUnlockDialogTimestamp = unlockAt
                    showPostUnlockWaitingDialog(unlockAt, now)
                }
                return@observe
            }
            // Mô hình mới: isVerified=true là chìa khóa mở quyền đăng bài.
            // Role hiện tại chỉ còn user/admin.
            val isPrivileged = user.role == "admin" || user.isVerified

            if (isPrivileged) {
                showPostForm()

                // Vô hiệu hóa Họ tên, SĐT và Giới tính khi tài khoản đã KYC (isVerified == true) và không phải admin
                val formView = postFormView
                if (formView != null) {
                    val edtName = formView.findViewById<EditText>(R.id.edtOwnerName)
                    val edtPhone = formView.findViewById<EditText>(R.id.edtOwnerPhone)
                    val rgGender = formView.findViewById<RadioGroup>(R.id.rgOwnerGender)
                    if (user.isVerified && user.role != "admin") {
                        edtName?.isEnabled = false
                        edtName?.isFocusable = false
                        edtPhone?.isEnabled = false
                        edtPhone?.isFocusable = false
                        rgGender?.isEnabled = false
                        for (i in 0 until (rgGender?.childCount ?: 0)) {
                            rgGender?.getChildAt(i)?.isEnabled = false
                        }
                    } else {
                        edtName?.isEnabled = true
                        edtName?.isFocusableInTouchMode = true
                        edtPhone?.isEnabled = true
                        edtPhone?.isFocusableInTouchMode = true
                        rgGender?.isEnabled = true
                        for (i in 0 until (rgGender?.childCount ?: 0)) {
                            rgGender?.getChildAt(i)?.isEnabled = true
                        }
                    }
                }

                if (!user.hasAcceptedRules && !rulesDialogShown) {
                    rulesDialogShown = true
                    showRulesDialog(isFirstTime = true)
                }
                if (!isPostProcessing) {
                    viewModel.checkPrePostQuota()
                }
            } else {
                when (user.role) {
                    "pending" -> showPendingStatus()
                    "rejected" -> showRejectedStatus(user.verificationRejectReason)
                    else -> showVerifyRequired()
                }
            }
        }


        viewModel.ownerInfo.observe(viewLifecycleOwner) { info ->
            val v = postFormView ?: return@observe
            v.findViewById<EditText>(R.id.edtOwnerName)?.setText(info.name)
            v.findViewById<EditText>(R.id.edtOwnerPhone)?.setText(info.phone)
            currentOwnerAvatarUrl = info.avatarUrl
            val rgGender = v.findViewById<RadioGroup>(R.id.rgOwnerGender)
            when (info.gender) {
                "Nam" -> rgGender?.check(R.id.rbOwnerMale)
                "Nữ" -> rgGender?.check(R.id.rbOwnerFemale)
            }
        }

        viewModel.postQuotaBlocked.observe(viewLifecycleOwner) { info ->
            if (info == null) return@observe
            if (!isVisible) return@observe
            showPostQuotaLimitDialog(info.unlockAt)
            viewModel.clearPostQuotaBlockedEvent()
        }

        checkUserRole()
        setupVerifyButton()
    }

    private fun showLoginRequired() {
        layoutGuest?.visibility = View.VISIBLE
        verifyRequiredView?.visibility = View.GONE
        postFormView?.visibility = View.GONE

        layoutGuest?.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnGuestLogin)?.setOnClickListener {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
        }
        layoutGuest?.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnGuestRegister)?.setOnClickListener {
            startActivity(Intent(requireContext(), RegisterActivity::class.java))
        }
    }

    private fun checkUserRole() {
        viewModel.loadUserObject()
    }

    // Xác minh danh tính -2-
    //Hàm chuyển đổi giao diện sang trạng thái "Bắt buộc xác minh".
    //Nó sẽ ẩn view nhập liệu (postFormView) và hiện view cảnh báo (verifyRequiredView),
    //đồng thời thiết lập tiêu đề, icon và hiển thị nút "TIẾN HÀNH XÁC MINH".
    private fun showVerifyRequired() {
        verifyRequiredView?.visibility = View.VISIBLE
        postFormView?.visibility = View.GONE

        val tvVerifyTitle = verifyRequiredView?.findViewById<TextView>(R.id.tvVerifyTitle)
        val tvVerifyStatus = verifyRequiredView?.findViewById<TextView>(R.id.tvVerifyStatus)
        val btnStartVerify = verifyRequiredView?.findViewById<MaterialButton>(R.id.btnStartVerify)
        val tvViewRules = verifyRequiredView?.findViewById<TextView>(R.id.tvViewRulesBeforeVerify)
        val layoutSteps = verifyRequiredView?.findViewById<View>(R.id.layoutSteps)
        val imgVerifyStatus = verifyRequiredView?.findViewById<ImageView>(R.id.imgVerifyStatus)

        tvVerifyTitle?.text = "Xác minh\nChủ cho thuê"
        tvVerifyStatus?.text = "Hoàn tất xác minh danh tính để bắt đầu đăng tin và tiếp cận hàng ngàn khách thuê tiềm năng."
        imgVerifyStatus?.setImageResource(R.drawable.ic_shield_check)
        imgVerifyStatus?.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)

        layoutSteps?.visibility = View.VISIBLE
        btnStartVerify?.visibility = View.VISIBLE
        btnStartVerify?.text = "TIẾN HÀNH XÁC MINH"
        tvViewRules?.visibility = View.VISIBLE
    }

    private fun showPendingStatus() {
        verifyRequiredView?.visibility = View.VISIBLE
        postFormView?.visibility = View.GONE

        val tvVerifyTitle = verifyRequiredView?.findViewById<TextView>(R.id.tvVerifyTitle)
        val tvVerifyStatus = verifyRequiredView?.findViewById<TextView>(R.id.tvVerifyStatus)
        val layoutBottomActions = verifyRequiredView?.findViewById<View>(R.id.layoutBottomActions)
        val layoutSteps = verifyRequiredView?.findViewById<View>(R.id.layoutSteps)
        val imgVerifyStatus = verifyRequiredView?.findViewById<ImageView>(R.id.imgVerifyStatus)

        tvVerifyTitle?.text = "Đang chờ\nPhê duyệt"
        tvVerifyStatus?.text = "Hồ sơ xác minh của bạn đã được gửi thành công.\n\nĐội ngũ kiểm duyệt đang tiến hành đối chiếu thông tin. Xin vui lòng quay lại kiểm tra sau 24 - 48 giờ làm việc."
        imgVerifyStatus?.setImageResource(R.drawable.ic_shield_check) // Dùng tạm shield check
        imgVerifyStatus?.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFE082")) // Màu vàng nhạt

        layoutSteps?.visibility = View.GONE
        layoutBottomActions?.visibility = View.GONE
    }

    private fun showRejectedStatus(reason: String) {
        verifyRequiredView?.visibility = View.VISIBLE
        postFormView?.visibility = View.GONE

        val tvVerifyTitle = verifyRequiredView?.findViewById<TextView>(R.id.tvVerifyTitle)
        val tvVerifyStatus = verifyRequiredView?.findViewById<TextView>(R.id.tvVerifyStatus)
        val btnStartVerify = verifyRequiredView?.findViewById<MaterialButton>(R.id.btnStartVerify)
        val layoutBottomActions = verifyRequiredView?.findViewById<View>(R.id.layoutBottomActions)
        val layoutSteps = verifyRequiredView?.findViewById<View>(R.id.layoutSteps)
        val imgVerifyStatus = verifyRequiredView?.findViewById<ImageView>(R.id.imgVerifyStatus)

        tvVerifyTitle?.text = "Xác minh\nBị từ chối"
        tvVerifyStatus?.text = "Rất tiếc, hồ sơ của bạn chưa đáp ứng đủ điều kiện.\nLý do: $reason\n\nVui lòng chuẩn bị lại giấy tờ và thực hiện xác minh lại từ đầu."
        imgVerifyStatus?.setImageResource(R.drawable.ic_close)
        imgVerifyStatus?.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FF8A80")) // Màu đỏ nhạt

        layoutSteps?.visibility = View.GONE
        layoutBottomActions?.visibility = View.VISIBLE
        btnStartVerify?.visibility = View.VISIBLE
        btnStartVerify?.text = "GỬI LẠI YÊU CẦU"
    }

    private fun showPostForm() {
        verifyRequiredView?.visibility = View.GONE
        postFormView?.visibility = View.VISIBLE
        if (!isFormSetup) {
            setupPostForm()
            isFormSetup = true
            requireActivity().onBackPressedDispatcher.addCallback(
                viewLifecycleOwner,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        if (isDirty) {
                            showDiscardDialog {
                                isDirty = false
                                isEnabled = false
                                requireActivity().onBackPressedDispatcher.onBackPressed()
                            }
                        } else {
                            isEnabled = false
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }
                    }
                }
            )
        }
    }

    private fun showPostUnlockWaitingStatus(unlockAt: Long, now: Long) {
        verifyRequiredView?.visibility = View.VISIBLE
        postFormView?.visibility = View.GONE

        val tvVerifyTitle = verifyRequiredView?.findViewById<TextView>(R.id.tvVerifyTitle)
        val tvVerifyStatus = verifyRequiredView?.findViewById<TextView>(R.id.tvVerifyStatus)
        val layoutBottomActions = verifyRequiredView?.findViewById<View>(R.id.layoutBottomActions)
        val layoutSteps = verifyRequiredView?.findViewById<View>(R.id.layoutSteps)
        val imgVerifyStatus = verifyRequiredView?.findViewById<ImageView>(R.id.imgVerifyStatus)

        tvVerifyTitle?.text = "Đã cấp quyền\nĐăng bài"
        tvVerifyStatus?.text = "Tài khoản của bạn đã được admin duyệt thành công.\n\nHệ thống sẽ mở chức năng đăng bài sau " + formatRemainingTime(unlockAt - now) +
                "\n(Dự kiến lúc " + formatDateTime(unlockAt) + ")."
        imgVerifyStatus?.setImageResource(R.drawable.ic_shield_check)
        imgVerifyStatus?.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#A5D6A7")) // Màu xanh lá nhạt

        layoutSteps?.visibility = View.GONE
        layoutBottomActions?.visibility = View.GONE
    }

    private fun showPostUnlockWaitingDialog(unlockAt: Long, now: Long) {
        MessageUtils.showInfoDialog(
            requireContext(),
            "Đã được cấp quyền",
            "Quyền đăng bài của bạn đã được cấp nhưng cần chờ thêm 24 giờ " +
                    "kể từ lúc admin duyệt.\n\n" +
                    "Còn lại: " + formatRemainingTime(unlockAt - now) + "\n" +
                    "Thời điểm mở đăng bài: " + formatDateTime(unlockAt)
        )
    }

    private fun formatDateTime(timestamp: Long): String {
        val formatter = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale("vi", "VN"))
        return formatter.format(Date(timestamp))
    }

    private fun formatRemainingTime(remainingMs: Long): String {
        val safe = remainingMs.coerceAtLeast(0L)
        val totalMinutes = safe / 60_000L
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return "${hours} giờ ${minutes} phút"
    }

    private fun formatDetailedCountdown(remainingMs: Long): String {
        val safe = remainingMs.coerceAtLeast(0L)
        val totalSeconds = safe / 1000L
        val days = totalSeconds / 86_400L
        val hours = (totalSeconds % 86_400L) / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L
        return "${days} ngày ${hours} giờ ${minutes} phút ${seconds} giây"
    }

    private fun showPostQuotaLimitDialog(unlockAt: Long) {
        if (!isAdded) return
        if (upgradeSlotsDialog?.isShowing == true || 
            paymentWarningDialog?.isShowing == true || 
            paymentQrDialog?.isShowing == true) {
            // Không hiển thị dialog hết lượt nếu người dùng đang trong luồng thanh toán mua lượt
            return
        }
        dismissPostQuotaDialog()


        val now = System.currentTimeMillis()
        val remainMs = (unlockAt - now).coerceAtLeast(0L)
        val formatter = SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale("vi", "VN"))

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_post_quota_limit, null)
        val tvQuotaMessage = dialogView.findViewById<TextView>(R.id.tvQuotaMessage)
        val tvQuotaUnlockAt = dialogView.findViewById<TextView>(R.id.tvQuotaUnlockAt)
        val tvQuotaCountdown = dialogView.findViewById<TextView>(R.id.tvQuotaCountdown)
        val btnQuotaClose = dialogView.findViewById<MaterialButton>(R.id.btnQuotaClose)
        val btnQuotaUpgrade = dialogView.findViewById<MaterialButton>(R.id.btnQuotaUpgrade)

        tvQuotaMessage.text = "Số lần đăng bài miễn phí trong ngày của bạn đã hết. Bạn sẽ nhận được 3 lượt mới vào lúc 00:00 ngày mai."
        tvQuotaUnlockAt.text = "Mở lại lúc: ${formatter.format(Date(unlockAt))}"
        tvQuotaCountdown.text = "Thời gian còn lại: ${formatDetailedCountdown(remainMs)}"

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnQuotaClose.setOnClickListener { dialog.dismiss() }
        btnQuotaUpgrade.setOnClickListener {
            dialog.dismiss()
            showUpgradeSlotsDialog()
        }

        dialog.setOnDismissListener { dismissPostQuotaDialog() }
        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawableResource(R.drawable.bg_dialog_surface)
            val width = (resources.displayMetrics.widthPixels * 0.9f).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        postQuotaDialog = dialog
        postQuotaTimer = object : CountDownTimer(remainMs, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                if (!isAdded) return
                tvQuotaCountdown.text = "Thời gian còn lại: ${formatDetailedCountdown(millisUntilFinished)}"
            }

            override fun onFinish() {
                if (!isAdded) return
                tvQuotaCountdown.text = "Thời gian còn lại: 0 ngày 0 giờ 0 phút 0 giây"
            }
        }.start()
    }

    private fun dismissPostQuotaDialog() {
        postQuotaTimer?.cancel()
        postQuotaTimer = null
        postQuotaDialog?.setOnDismissListener(null)
        postQuotaDialog?.dismiss()
        postQuotaDialog = null
    }

    // Xác minh danh tính -3-
    // Khởi tạo các bộ lắng nghe sự kiện (Click Listeners) cho màn hình chặn:
    // Khi bấm "Tiến hành xác minh": Chuyển hướng user sang màn hình chụp ảnh CCCD (VerifyLandlordActivity).
    // Khi bấm "Xem nội quy": Mở popup hiển thị các quy định đăng tin.
    private fun setupVerifyButton() {
        verifyRequiredView?.findViewById<MaterialButton>(R.id.btnStartVerify)?.setOnClickListener {
            startActivity(Intent(requireContext(), VerifyLandlordActivity::class.java))
        }

        verifyRequiredView?.findViewById<TextView>(R.id.tvViewRulesBeforeVerify)?.setOnClickListener {
            showRulesDialog()
        }
    }

    private fun addPhotoToLayout(uri: Uri) {
        val view = postFormView ?: return
        val layoutPhotos = view.findViewById<LinearLayout>(R.id.layoutPhotos)
        val tvPhotoCount = view.findViewById<TextView>(R.id.tvPhotoCount)
        val btnAddPhoto = view.findViewById<CardView>(R.id.btnAddPhoto)

        val imgView = ImageView(requireContext())
        val params = LinearLayout.LayoutParams(dpToPx(90), dpToPx(90))
        params.marginEnd = dpToPx(8)
        imgView.layoutParams = params
        imgView.scaleType = ImageView.ScaleType.CENTER_CROP
        Glide.with(this).load(uri).centerCrop().into(imgView)

        imgView.setOnLongClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Xóa ảnh").setMessage("Bạn có muốn xóa ảnh này?")
                .setPositiveButton("Xóa") { _, _ ->
                    val uriIndex = imageUris.indexOf(uri)
                    if (uriIndex >= 0) imageUris.removeAt(uriIndex)
                    layoutPhotos.removeView(imgView)
                    tvPhotoCount.text = "${imageUris.size}/$MAX_PHOTOS ảnh"
                    if (imageUris.size < MAX_PHOTOS) btnAddPhoto.visibility = View.VISIBLE
                }
                .setNegativeButton("Hủy", null).show()
            true
        }
        layoutPhotos.addView(imgView, layoutPhotos.childCount - 1)
        tvPhotoCount.text = "${imageUris.size}/$MAX_PHOTOS ảnh"
        if (imageUris.size >= MAX_PHOTOS) btnAddPhoto.visibility = View.GONE
    }

    private fun setupPostForm() {
        val view = postFormView ?: return

        val edtOwnerName = view.findViewById<EditText>(R.id.edtOwnerName)
        val edtOwnerPhone = view.findViewById<EditText>(R.id.edtOwnerPhone)
        val rgOwnerGender = view.findViewById<RadioGroup>(R.id.rgOwnerGender)
        val edtTitle = view.findViewById<EditText>(R.id.edtTitle)
        val tvPostDate = view.findViewById<TextView>(R.id.tvPostDate)
        val spinnerWard = view.findViewById<Spinner>(R.id.spinnerWard)
        val edtAddress = view.findViewById<EditText>(R.id.edtAddress)
        val btnPickLocation = view.findViewById<MaterialButton>(R.id.btnPickLocation)
        val tvPickedLocation = view.findViewById<TextView>(R.id.tvPickedLocation)
        val edtDescription = view.findViewById<EditText>(R.id.edtDescription)
        val edtPrice = view.findViewById<EditText>(R.id.edtPrice)

        val dirtyWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty()) isDirty = true
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        }
        edtTitle.addTextChangedListener(dirtyWatcher)
        edtAddress.addTextChangedListener(dirtyWatcher)
        edtDescription.addTextChangedListener(dirtyWatcher)
        edtPrice.addTextChangedListener(dirtyWatcher)
        val edtArea = view.findViewById<EditText>(R.id.edtArea)
        val edtPeopleCount = view.findViewById<EditText>(R.id.edtPeopleCount)
        val rgRoomStyle = view.findViewById<RadioGroup>(R.id.rgRoomStyle)
        val edtDepositMonths = view.findViewById<EditText>(R.id.edtDepositMonths)
        val edtDepositAmount = view.findViewById<EditText>(R.id.edtDepositAmount)
        val cbWifi = view.findViewById<CheckBox>(R.id.cbWifi)
        val cbElectric = view.findViewById<CheckBox>(R.id.cbElectric)
        val cbWater = view.findViewById<CheckBox>(R.id.cbWater)
        val edtWifiPrice = view.findViewById<EditText>(R.id.edtWifiPrice)
        val edtElectricPrice = view.findViewById<EditText>(R.id.edtElectricPrice)
        val edtWaterPrice = view.findViewById<EditText>(R.id.edtWaterPrice)
        val cbAirCon = view.findViewById<CheckBox>(R.id.cbAirCon)
        val edtAirConQty = view.findViewById<EditText>(R.id.edtAirConQty)
        val cbWaterHeater = view.findViewById<CheckBox>(R.id.cbWaterHeater)
        val edtWaterHeaterQty = view.findViewById<EditText>(R.id.edtWaterHeaterQty)
        val cbWasher = view.findViewById<CheckBox>(R.id.cbWasher)
        val edtWasherQty = view.findViewById<EditText>(R.id.edtWasherQty)
        val cbDryingArea = view.findViewById<CheckBox>(R.id.cbDryingArea)
        val edtDryingAreaQty = view.findViewById<EditText>(R.id.edtDryingAreaQty)
        val cbWardrobe = view.findViewById<CheckBox>(R.id.cbWardrobe)
        val edtWardrobeQty = view.findViewById<EditText>(R.id.edtWardrobeQty)
        val cbBed = view.findViewById<CheckBox>(R.id.cbBed)
        val edtBedQty = view.findViewById<EditText>(R.id.edtBedQty)
        val rgKitchen = view.findViewById<RadioGroup>(R.id.rgKitchen)
        val rgBathroom = view.findViewById<RadioGroup>(R.id.rgBathroom)
        val rgPet = view.findViewById<RadioGroup>(R.id.rgPet)
        val layoutPetDetail = view.findViewById<LinearLayout>(R.id.layoutPetDetail)
        val edtPetName = view.findViewById<EditText>(R.id.edtPetName)
        val edtPetCount = view.findViewById<EditText>(R.id.edtPetCount)
        val rgGenderPrefer = view.findViewById<RadioGroup>(R.id.rgGenderPrefer)
        val rgCurfew = view.findViewById<RadioGroup>(R.id.rgCurfew)
        val layoutCurfewTime = view.findViewById<LinearLayout>(R.id.layoutCurfewTime)
        val edtCurfewTime = view.findViewById<TextView>(R.id.edtCurfewTime)
        val pickerHour = view.findViewById<android.widget.NumberPicker>(R.id.pickerHour)
        val pickerMinute = view.findViewById<android.widget.NumberPicker>(R.id.pickerMinute)
        val pickerAmPm = view.findViewById<android.widget.NumberPicker>(R.id.pickerAmPm)

        pickerHour.minValue = 1; pickerHour.maxValue = 12; pickerHour.value = 10
        pickerMinute.minValue = 0; pickerMinute.maxValue = 59; pickerMinute.value = 0
        pickerMinute.setFormatter { String.format("%02d", it) }
        pickerAmPm.minValue = 0; pickerAmPm.maxValue = 1
        pickerAmPm.displayedValues = arrayOf("SA", "CH"); pickerAmPm.value = 1

        val syncCurfewTime = {
            val amPm = if (pickerAmPm.value == 0) "SA" else "CH"
            edtCurfewTime.text = String.format("%02d:%02d %s", pickerHour.value, pickerMinute.value, amPm)
        }
        pickerHour.setOnValueChangedListener { _, _, _ -> syncCurfewTime() }
        pickerMinute.setOnValueChangedListener { _, _, _ -> syncCurfewTime() }
        pickerAmPm.setOnValueChangedListener { _, _, _ -> syncCurfewTime() }
        syncCurfewTime()

        // Mục 5: Thông tin lịch hẹn — hạn hiển thị (bắt buộc, tối đa 6 tháng)
        val etExpiryDate = view.findViewById<TextInputEditText>(R.id.etExpiryDate)
        val tilExpiryDate = view.findViewById<TextInputLayout>(R.id.tilExpiryDate)
        val expiryDateFormat = SimpleDateFormat("HH:mm, dd/MM/yyyy", Locale("vi", "VN"))

        fun openExpiryTimePicker(year: Int, month: Int, day: Int) {
            val now = Calendar.getInstance()
            val minMs = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000
            val maxMs = Calendar.getInstance().apply { add(Calendar.MONTH, 6) }.timeInMillis
            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(now.get(Calendar.HOUR_OF_DAY))
                .setMinute(now.get(Calendar.MINUTE))
                .setTitleText("Chọn giờ hết hạn")
                .build()
            timePicker.addOnPositiveButtonClickListener {
                val selected = Calendar.getInstance().apply {
                    set(year, month, day, timePicker.hour, timePicker.minute, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                if (selected < minMs) {
                    MessageUtils.showInfoDialog(requireContext(), "Hạn hiển thị không hợp lệ", "Hạn hiển thị phải ít nhất 1 tuần kể từ ngày đăng bài.")
                    return@addOnPositiveButtonClickListener
                }
                if (selected > maxMs) {
                    MessageUtils.showInfoDialog(requireContext(), "Hạn hiển thị không hợp lệ", "Hạn hiển thị không được quá 6 tháng kể từ hôm nay.")
                    return@addOnPositiveButtonClickListener
                }
                selectedExpiryMs = selected
                etExpiryDate.setText(expiryDateFormat.format(Date(selected)))
            }
            timePicker.show(parentFragmentManager, "expiryTimePicker")
        }

        fun openExpiryPicker() {
            val minMs = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000
            val maxMs = Calendar.getInstance().apply { add(Calendar.MONTH, 6) }.timeInMillis
            val constraints = CalendarConstraints.Builder()
                .setStart(minMs)
                .setEnd(maxMs)
                .setValidator(DateValidatorPointForward.from(minMs))
                .build()
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Chọn ngày hết hạn")
                .setCalendarConstraints(constraints)
                .setSelection(minMs)
                .build()
            datePicker.addOnPositiveButtonClickListener { selection ->
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = selection }
                openExpiryTimePicker(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
            }
            datePicker.show(parentFragmentManager, "expiryDatePicker")
        }

        etExpiryDate.setOnClickListener { openExpiryPicker() }
        tilExpiryDate.setEndIconOnClickListener { openExpiryPicker() }

        // Khung giờ nhận lịch hẹn: chọn ngày + cấu hình từng ngày riêng biệt
        val layoutTimeSlotRows = view.findViewById<LinearLayout>(R.id.layoutTimeSlotRows)
        layoutTimeSlotRows.removeAllViews()

        val dayShortLabels = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
        val dayFullLabels = listOf("Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật")
        val dayCbs = mutableListOf<CheckBox>()

        // Lớp lưu trữ cấu hình buổi của từng ngày
        data class DayPeriodConfig(
            val container: LinearLayout,
            val cbMorning: CheckBox, val tvMorningStart: TextView, val tvMorningEnd: TextView,
            val cbNoon: CheckBox, val tvNoonStart: TextView, val tvNoonEnd: TextView,
            val cbEvening: CheckBox, val tvEveningStart: TextView, val tvEveningEnd: TextView
        )
        val dayConfigs = mutableMapOf<String, DayPeriodConfig>()

        // Hàm bổ trợ: hiển thị dialog chọn giờ
        fun showTimePicker(tv: TextView) {
            val parts = tv.text.toString().split(":").mapNotNull { it.toIntOrNull() }
            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(parts.getOrElse(0) { 8 })
                .setMinute(parts.getOrElse(1) { 0 })
                .setTitleText("Chọn giờ")
                .build()
            picker.addOnPositiveButtonClickListener {
                tv.text = String.format("%02d:%02d", picker.hour, picker.minute)
            }
            picker.show(parentFragmentManager, "slotTimePicker")
        }

        // Hàm bổ trợ: xây dựng 1 dòng buổi, trả về (container, cb, tvStart, tvEnd)
        data class PeriodRowResult(val container: LinearLayout, val cb: CheckBox, val tvStart: TextView, val tvEnd: TextView)
        fun buildPeriodRow(periodLabel: String, defaultStart: String, defaultEnd: String): PeriodRowResult {
            val container = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    .apply { bottomMargin = dpToPx(8) }
            }
            val cb = CheckBox(requireContext()).apply {
                text = periodLabel; textSize = 13f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                buttonTintList = ContextCompat.getColorStateList(requireContext(), R.color.primary)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f)
            }
            val tvStart = TextView(requireContext()).apply {
                text = defaultStart; textSize = 13f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_edit_post)
                setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
                isEnabled = false; alpha = 0.4f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val tvSep = TextView(requireContext()).apply {
                text = " – "; textSize = 14f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }
            val tvEnd = TextView(requireContext()).apply {
                text = defaultEnd; textSize = 13f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_edit_post)
                setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
                isEnabled = false; alpha = 0.4f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            cb.setOnCheckedChangeListener { _, checked ->
                tvStart.isEnabled = checked; tvEnd.isEnabled = checked
                tvStart.alpha = if (checked) 1f else 0.4f; tvEnd.alpha = if (checked) 1f else 0.4f
            }
            tvStart.setOnClickListener { if (cb.isChecked) showTimePicker(tvStart) }
            tvEnd.setOnClickListener { if (cb.isChecked) showTimePicker(tvEnd) }
            container.addView(cb); container.addView(tvStart); container.addView(tvSep); container.addView(tvEnd)
            return PeriodRowResult(container, cb, tvStart, tvEnd)
        }

        // Hàng chọn ngày (T2 → CN)
        val dayRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { bottomMargin = dpToPx(8) }
        }

        // Container cha chứa tất cả section cấu hình từng ngày
        val dayConfigsContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        // Tạo checkbox và section cấu hình riêng cho từng ngày
        for (i in dayShortLabels.indices) {
            val short = dayShortLabels[i]
            val full = dayFullLabels[i]

            val cb = CheckBox(requireContext()).apply {
                text = short; textSize = 12f
                gravity = android.view.Gravity.CENTER
                setButtonDrawable(0) // Hide default checkbox drawable
                
                // Use styling from XML resource files
                setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.selector_day_text))
                background = ContextCompat.getDrawable(requireContext(), R.drawable.selector_day_bg)
                
                layoutParams = LinearLayout.LayoutParams(0, dpToPx(36), 1f).apply {
                    if (i < dayShortLabels.size - 1) {
                        marginEnd = dpToPx(5)
                    }
                }
            }
            dayCbs.add(cb)
            dayRow.addView(cb)

            // Section riêng của ngày này (ban đầu ẩn)
            val daySection = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                visibility = android.view.View.GONE
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    .apply { bottomMargin = dpToPx(6) }
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(0x0A1976D2.toInt())
                    cornerRadius = dpToPx(8).toFloat()
                }
                setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
            }

            // Tiêu đề ngày trong section
            daySection.addView(TextView(requireContext()).apply {
                text = "📅 $full"
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    .apply { bottomMargin = dpToPx(8) }
            })

            // 3 dòng buổi cho ngày này (sáng/trưa/chiều tối)
            val mRow = buildPeriodRow("Buổi sáng", "08:00", "12:00")
            val nRow = buildPeriodRow("Buổi trưa", "12:00", "14:00")
            val eRow = buildPeriodRow("Buổi chiều/tối", "14:00", "18:00")
            daySection.addView(mRow.container)
            daySection.addView(nRow.container)
            daySection.addView(eRow.container)

            dayConfigs[full] = DayPeriodConfig(
                daySection,
                mRow.cb, mRow.tvStart, mRow.tvEnd,
                nRow.cb, nRow.tvStart, nRow.tvEnd,
                eRow.cb, eRow.tvStart, eRow.tvEnd
            )
            dayConfigsContainer.addView(daySection)

            // Khi tích ngày → hiện section cấu hình; bỏ tích → ẩn
            cb.setOnCheckedChangeListener { _, checked ->
                daySection.visibility = if (checked) android.view.View.VISIBLE else android.view.View.GONE
            }
        }

        layoutTimeSlotRows.addView(dayRow)
        layoutTimeSlotRows.addView(dayConfigsContainer)

        val tvNotesLabel = TextView(requireContext()).apply {
            text = "Ghi chú / Lưu ý:"
            textSize = 13f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dpToPx(6); bottomMargin = dpToPx(4) }
        }
        layoutTimeSlotRows.addView(tvNotesLabel)

        val etAppointmentNotice = EditText(requireContext()).apply {
            hint = "Ví dụ: Liên hệ trước 30 phút, không nhận hẹn thứ 2..."
            textSize = 13f
            minLines = 2; maxLines = 4
            setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_edit_post)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        layoutTimeSlotRows.addView(etAppointmentNotice)

        // Tạo chuỗi lịch hẹn theo định dạng per-day mới
        fun buildTimeSlotsString(): String {
            val result = StringBuilder()
            for (i in dayFullLabels.indices) {
                if (i >= dayCbs.size || !dayCbs[i].isChecked) continue
                val full = dayFullLabels[i]
                val config = dayConfigs[full] ?: continue
                val periods = StringBuilder()
                if (config.cbMorning.isChecked) periods.append("\nBuổi sáng: ${config.tvMorningStart.text}-${config.tvMorningEnd.text}")
                if (config.cbNoon.isChecked) periods.append("\nBuổi trưa: ${config.tvNoonStart.text}-${config.tvNoonEnd.text}")
                if (config.cbEvening.isChecked) periods.append("\nBuổi chiều/tối: ${config.tvEveningStart.text}-${config.tvEveningEnd.text}")
                if (periods.isEmpty()) continue
                result.append("$full:$periods\n")
            }
            val notes = etAppointmentNotice.text.toString().trim()
            val str = result.toString().trimEnd()
            return if (str.isEmpty()) "" else if (notes.isNotEmpty()) "$str\nGhi chú: $notes" else str
        }

        val btnAddPhoto = view.findViewById<CardView>(R.id.btnAddPhoto)
        val btnPostRoom = view.findViewById<MaterialButton>(R.id.btnPostRoom)
        val tvRulesLink = view.findViewById<TextView>(R.id.tvRulesLink)
        val layoutProgress = view.findViewById<LinearLayout>(R.id.layoutProgress)
        val tvProgressPercent = view.findViewById<TextView>(R.id.tvProgressPercent)

        tvRulesLink.setOnClickListener {
            showRulesDialog()
        }

        // Load owner info via ViewModel — không gọi FirebaseAuth từ Fragment
        viewModel.loadOwnerInfo()

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))
        tvPostDate.text = dateFormat.format(Date())

        val chipPostPhuong = view.findViewById<com.google.android.material.chip.Chip>(R.id.chipPostPhuong)
        val chipPostXa = view.findViewById<com.google.android.material.chip.Chip>(R.id.chipPostXa)

        fun loadWardList(list: Array<String>) {
            val items = mutableListOf("-- Chọn phường/xã --").apply { addAll(list.drop(1)) }
            val adp = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, items)
            adp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerWard.adapter = adp
        }
        loadWardList(AddressData.phuongList)
        chipPostPhuong.setOnCheckedChangeListener { _, isChecked -> if (isChecked) loadWardList(AddressData.phuongList) }
        chipPostXa.setOnCheckedChangeListener { _, isChecked -> if (isChecked) loadWardList(AddressData.xaList) }
        tvPickedLocation.text = "Chưa chọn vị trí chính xác"

        btnPickLocation.setOnClickListener {
            val selectedWard = spinnerWard.selectedItem?.toString().orEmpty()
            val wardName = if (selectedWard.contains("(")) selectedWard.substringBefore("(").trim() else selectedWard
            val districtName = if (selectedWard.contains("(")) selectedWard.substringAfter("(").replace(")", "").trim() else ""
            val intent = Intent(requireContext(), LocationPickerActivity::class.java).apply {
                putExtra(LocationPickerActivity.EXTRA_INITIAL_ADDRESS, edtAddress.text.toString().trim())
                putExtra(LocationPickerActivity.EXTRA_INITIAL_WARD, wardName)
                putExtra(LocationPickerActivity.EXTRA_INITIAL_DISTRICT, districtName)
                putExtra(LocationPickerActivity.EXTRA_IS_STRICT, true)
            }
            pickLocationLauncher.launch(intent)
        }

        NumberFormatUtils.addFormatWatcher(edtPrice)
        NumberFormatUtils.addFormatWatcher(edtWifiPrice)
        NumberFormatUtils.addFormatWatcher(edtElectricPrice)
        NumberFormatUtils.addFormatWatcher(edtWaterPrice)
        otherFeeContainer = view.findViewById(R.id.layoutOtherFees)
        otherFeeRows.clear()
        addFeeRow()
        view.findViewById<MaterialButton>(R.id.btnAddOtherFee).setOnClickListener { addFeeRow() }
        furnitureContainer = view.findViewById(R.id.layoutFurnitureItems)
        furnitureRows.clear()
        addFurnitureRow()
        view.findViewById<MaterialButton>(R.id.btnAddFurnitureItem).setOnClickListener { addFurnitureRow() }
        serviceContainer = view.findViewById(R.id.layoutServiceItems)
        serviceRows.clear()
        addServiceRow()
        view.findViewById<MaterialButton>(R.id.btnAddServiceItem).setOnClickListener { addServiceRow() }
        cbAirCon.setOnCheckedChangeListener { _, c -> edtAirConQty.isEnabled = c; if (!c) edtAirConQty.text?.clear() }
        cbWaterHeater.setOnCheckedChangeListener { _, c -> edtWaterHeaterQty.isEnabled = c; if (!c) edtWaterHeaterQty.text?.clear() }
        cbWasher.setOnCheckedChangeListener { _, c -> edtWasherQty.isEnabled = c; if (!c) edtWasherQty.text?.clear() }
        cbDryingArea.setOnCheckedChangeListener { _, c -> edtDryingAreaQty.isEnabled = c; if (!c) edtDryingAreaQty.text?.clear() }
        cbWardrobe.setOnCheckedChangeListener { _, c -> edtWardrobeQty.isEnabled = c; if (!c) edtWardrobeQty.text?.clear() }
        cbBed.setOnCheckedChangeListener { _, c -> edtBedQty.isEnabled = c; if (!c) edtBedQty.text?.clear() }
        NumberFormatUtils.addFormatWatcher(edtDepositAmount)

        cbWifi.setOnCheckedChangeListener { _, isChecked -> edtWifiPrice.isEnabled = isChecked; if (!isChecked) edtWifiPrice.text?.clear() }
        cbElectric.setOnCheckedChangeListener { _, isChecked -> edtElectricPrice.isEnabled = isChecked; if (!isChecked) edtElectricPrice.text?.clear() }
        cbWater.setOnCheckedChangeListener { _, isChecked -> edtWaterPrice.isEnabled = isChecked; if (!isChecked) edtWaterPrice.text?.clear() }
        rgPet.setOnCheckedChangeListener { _, checkedId -> layoutPetDetail.visibility = if (checkedId == R.id.rbPetYes) View.VISIBLE else View.GONE }
        rgCurfew.setOnCheckedChangeListener { _, checkedId -> layoutCurfewTime.visibility = if (checkedId == R.id.rbCurfewCustom) View.VISIBLE else View.GONE }

        setupParkingListeners(view)

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            layoutProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnPostRoom.isEnabled = !isLoading
            btnPostRoom.text = if (isLoading) "Đang đăng bài..." else "Đăng bài cho thuê"
            if (isLoading) {
                PostNotificationHelper.showProgress(requireContext(), 0)
                showPostLoadingDialog()
            } else {
                dismissPostLoadingDialog()
            }
        }

        viewModel.uploadProgress.observe(viewLifecycleOwner) { progress ->
            tvProgressPercent.text = "Đang đăng bài: $progress%"
            PostNotificationHelper.showProgress(requireContext(), progress)
            updatePostLoadingDialog(progress)
        }

        viewModel.postResult.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                val (postId, remainMessage) = result
                viewModel.resetPostResult()
                PostNotificationHelper.showSuccess(requireContext(), lastPostedTitle)
                resetForm(view)
                
                // Hiển thị Dialog thông báo số dư lượt đăng trước khi chuyển màn hình
                MessageUtils.showSuccessDialog(
                    requireContext(),
                    "Đăng bài thành công",
                    remainMessage ?: "Bài đăng đã được duyệt thành công."
                ) {
                    isPostProcessing = false
                    val intent = Intent(requireContext(), com.example.doantotnghiep.View.Auth.PostSuccessActivity::class.java).apply {
                        putExtra("postId", postId)
                        putExtra("title", lastPostedTitle)
                        putExtra("price", lastPostedPrice)
                        putExtra("location", lastPostedLocation)
                    }
                    startActivity(intent)
                }
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrEmpty()) {
                isPostProcessing = false
                dismissPostLoadingDialog()
                PostNotificationHelper.cancel(requireContext())
                MessageUtils.showErrorDialog(requireContext(), "Lỗi đăng bài", error)
            }
        }

        viewModel.showLastPurchasedSlotWarning.observe(viewLifecycleOwner) { shouldShow ->
            if (shouldShow == true) {
                viewModel.resetLastPurchasedSlotWarning()
                isPostProcessing = false
                
                AlertDialog.Builder(requireContext())
                    .setTitle("Lượt đăng sắp hết")
                    .setMessage("Bạn chỉ còn lại đúng 1 lượt đăng bài đã mua (tính phí).\n\nBạn có muốn tiếp tục sử dụng lượt này để đăng bài ngay hay muốn mua thêm lượt mới?")
                    .setPositiveButton("Tiếp tục đăng") { dialog, _ ->
                        dialog.dismiss()
                        isPostProcessing = true
                        viewModel.proceedWithPendingPost(requireContext())
                    }
                    .setNegativeButton("Mua thêm lượt") { dialog, _ ->
                        dialog.dismiss()
                        viewModel.cancelPendingPost()
                        showUpgradeSlotsDialog()
                    }
                    .setCancelable(false)
                    .show()
            }
        }

        btnAddPhoto.setOnClickListener {
            if (imageUris.size >= MAX_PHOTOS) {
                MessageUtils.showInfoDialog(requireContext(), "Giới hạn ảnh", "Bạn chỉ được chọn tối đa $MAX_PHOTOS ảnh.")
                return@setOnClickListener
            }
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            pickImageLauncher.launch(intent)
        }

        btnPostRoom.setOnClickListener {
            val selectedWard = spinnerWard.selectedItem?.toString() ?: ""
            if (spinnerWard.selectedItemPosition == 0) {
                MessageUtils.showInfoDialog(requireContext(), "Thông tin thiếu", "Vui lòng chọn khu vực phường/xã.")
                return@setOnClickListener
            }
            val wardName = if (selectedWard.contains("(")) selectedWard.substringBefore("(").trim() else selectedWard
            val districtName = if (selectedWard.contains("(")) selectedWard.substringAfter("(").replace(")", "").trim() else ""
            val lat = selectedLatitude
            val lng = selectedLongitude
            val address = edtAddress.text.toString().trim()
            if (address.isEmpty() && (lat == null || lng == null)) {
                MessageUtils.showInfoDialog(requireContext(), "Thiếu thông tin vị trí", "Vui lòng nhập địa chỉ cụ thể hoặc chọn vị trí trên bản đồ trước khi đăng bài.")
                return@setOnClickListener
            }

            // Kiểm tra hạn hiển thị bắt buộc
            if (selectedExpiryMs == null) {
                MessageUtils.showInfoDialog(requireContext(), "Thiếu thông tin (Mục 5)", "Vui lòng chọn hạn hiển thị bài đăng.")
                return@setOnClickListener
            }

            // Kiểm tra hạn hiển thị không vượt quá 6 tháng (phòng trường hợp state cũ)
            val maxAllowedMs = Calendar.getInstance().apply { add(Calendar.MONTH, 6) }.timeInMillis
            if (selectedExpiryMs!! > maxAllowedMs) {
                MessageUtils.showInfoDialog(requireContext(), "Hạn hiển thị không hợp lệ", "Hạn hiển thị không được quá 6 tháng kể từ hôm nay. Vui lòng chọn lại.")
                selectedExpiryMs = null
                view.findViewById<TextInputEditText>(R.id.etExpiryDate)?.setText("")
                return@setOnClickListener
            }

            // Kiểm tra khung giờ nhận lịch hẹn bắt buộc
            val timeSlotsStr = buildTimeSlotsString()
            if (timeSlotsStr.isEmpty()) {
                MessageUtils.showInfoDialog(requireContext(), "Thiếu thông tin (Mục 5)", "Vui lòng chọn ít nhất một ngày và một buổi có thể nhận lịch hẹn.")
                return@setOnClickListener
            }


            val cbMotorbike = view.findViewById<CheckBox>(R.id.cbMotorbike)
            val cbEBike = view.findViewById<CheckBox>(R.id.cbEBike)
            val cbBicycle = view.findViewById<CheckBox>(R.id.cbBicycle)
            val rgMotorbikeFee = view.findViewById<RadioGroup>(R.id.rgMotorbikeFee)
            val rgEBikeFee = view.findViewById<RadioGroup>(R.id.rgEBikeFee)
            val rgBicycleFee = view.findViewById<RadioGroup>(R.id.rgBicycleFee)
            val edtMotorbikeFee = view.findViewById<EditText>(R.id.edtMotorbikeFee)
            val edtEBikeFee = view.findViewById<EditText>(R.id.edtEBikeFee)
            val edtBicycleFee = view.findViewById<EditText>(R.id.edtBicycleFee)

            // Kiểm tra tiện ích bắt buộc đi kèm giá khi tích chọn (Mục 4)
            if (cbWifi.isChecked) {
                val wifiPrice = NumberFormatUtils.getRawNumber(edtWifiPrice).toLongOrNull() ?: 0L
                if (wifiPrice <= 0) {
                    MessageUtils.showInfoDialog(requireContext(), "Thiếu thông tin (Mục 4)", "Vui lòng nhập số tiền mạng/Wifi hợp lệ (lớn hơn 0).")
                    return@setOnClickListener
                }
            }
            if (cbElectric.isChecked) {
                val electricPrice = NumberFormatUtils.getRawNumber(edtElectricPrice).toLongOrNull() ?: 0L
                if (electricPrice <= 0) {
                    MessageUtils.showInfoDialog(requireContext(), "Thiếu thông tin (Mục 4)", "Vui lòng nhập tiền điện hợp lệ (lớn hơn 0).")
                    return@setOnClickListener
                }
            }
            if (cbWater.isChecked) {
                val waterPrice = NumberFormatUtils.getRawNumber(edtWaterPrice).toLongOrNull() ?: 0L
                if (waterPrice <= 0) {
                    MessageUtils.showInfoDialog(requireContext(), "Thiếu thông tin (Mục 4)", "Vui lòng nhập tiền nước hợp lệ (lớn hơn 0).")
                    return@setOnClickListener
                }
            }

            // Kiểm tra phí gửi xe nếu chọn tính phí
            if (cbMotorbike?.isChecked == true && rgMotorbikeFee?.checkedRadioButtonId == R.id.rbMotorbikePaid) {
                val fee = NumberFormatUtils.getRawNumber(edtMotorbikeFee).toLongOrNull() ?: 0L
                if (fee <= 0) {
                    MessageUtils.showInfoDialog(requireContext(), "Thiếu thông tin (Mục 4)", "Vui lòng nhập phí gửi xe máy hợp lệ (lớn hơn 0).")
                    return@setOnClickListener
                }
            }
            if (cbEBike?.isChecked == true && rgEBikeFee?.checkedRadioButtonId == R.id.rbEBikePaid) {
                val fee = NumberFormatUtils.getRawNumber(edtEBikeFee).toLongOrNull() ?: 0L
                if (fee <= 0) {
                    MessageUtils.showInfoDialog(requireContext(), "Thiếu thông tin (Mục 4)", "Vui lòng nhập phí gửi xe đạp điện hợp lệ (lớn hơn 0).")
                    return@setOnClickListener
                }
            }
            if (cbBicycle?.isChecked == true && rgBicycleFee?.checkedRadioButtonId == R.id.rbBicyclePaid) {
                val fee = NumberFormatUtils.getRawNumber(edtBicycleFee).toLongOrNull() ?: 0L
                if (fee <= 0) {
                    MessageUtils.showInfoDialog(requireContext(), "Thiếu thông tin (Mục 4)", "Vui lòng nhập phí gửi xe đạp hợp lệ (lớn hơn 0).")
                    return@setOnClickListener
                }
            }

            // Kiểm tra thông tin thú cưng nếu cho nuôi
            if (rgPet.checkedRadioButtonId == R.id.rbPetYes) {
                val petName = edtPetName.text.toString().trim()
                val petCountStr = edtPetCount.text.toString().trim()
                val petCount = petCountStr.toIntOrNull() ?: 0
                if (petName.isEmpty()) {
                    MessageUtils.showInfoDialog(requireContext(), "Thiếu thông tin (Mục 4)", "Vui lòng nhập loại thú cưng cho phép.")
                    return@setOnClickListener
                }
                if (petCountStr.isEmpty() || petCount <= 0) {
                    MessageUtils.showInfoDialog(requireContext(), "Thiếu thông tin (Mục 4)", "Vui lòng nhập số lượng thú cưng cho phép.")
                    return@setOnClickListener
                }
            }

            // Kiểm tra các dòng chi phí khác (dynamic list)
            for (row in otherFeeRows) {
                val label = row.first.text.toString().trim()
                val priceStr = row.second.text.toString().trim()
                val price = NumberFormatUtils.getRawNumber(row.second).toLongOrNull() ?: 0L
                if (label.isNotEmpty() && (priceStr.isEmpty() || price <= 0)) {
                    MessageUtils.showInfoDialog(requireContext(), "Thiếu thông tin (Mục 4)", "Vui lòng nhập số tiền hợp lệ cho chi phí '${label}'.")
                    return@setOnClickListener
                }
                if (label.isEmpty() && priceStr.isNotEmpty()) {
                    MessageUtils.showInfoDialog(requireContext(), "Thiếu thông tin (Mục 4)", "Vui lòng điền tên chi phí cho ô nhập số tiền.")
                    return@setOnClickListener
                }
            }

            // Kiểm tra nội thất phát sinh (dynamic list)
            for (row in furnitureRows) {
                val label = row.first.text.toString().trim()
                val qtyStr = row.second.text.toString().trim()
                val qty = qtyStr.toIntOrNull() ?: 0
                if (label.isNotEmpty() && (qtyStr.isEmpty() || qty <= 0)) {
                    MessageUtils.showInfoDialog(requireContext(), "Thiếu thông tin (Mục 4)", "Vui lòng nhập số lượng hợp lệ cho nội thất '${label}'.")
                    return@setOnClickListener
                }
                if (label.isEmpty() && qtyStr.isNotEmpty()) {
                    MessageUtils.showInfoDialog(requireContext(), "Thiếu thông tin (Mục 4)", "Vui lòng điền tên nội thất.")
                    return@setOnClickListener
                }
            }

            // Kiểm tra dịch vụ phát sinh (dynamic list)
            for (row in serviceRows) {
                val label = row.first.text.toString().trim()
                val priceStr = row.second.text.toString().trim()
                val price = NumberFormatUtils.getRawNumber(row.second).toLongOrNull() ?: 0L
                if (label.isNotEmpty() && (priceStr.isEmpty() || price <= 0)) {
                    MessageUtils.showInfoDialog(requireContext(), "Thiếu thông tin (Mục 4)", "Vui lòng nhập số tiền hợp lệ cho dịch vụ '${label}'.")
                    return@setOnClickListener
                }
                if (label.isEmpty() && priceStr.isNotEmpty()) {
                    MessageUtils.showInfoDialog(requireContext(), "Thiếu thông tin (Mục 4)", "Vui lòng điền tên dịch vụ.")
                    return@setOnClickListener
                }
            }

            // Kiểm tra số lượng đồ dùng tiện ích cơ bản (nếu được tích)
            if (cbAirCon.isChecked) {
                val qty = edtAirConQty.text.toString().toIntOrNull() ?: 0
                if (qty <= 0) {
                    MessageUtils.showInfoDialog(requireContext(), "Thiếu thông tin (Mục 4)", "Vui lòng nhập số lượng Điều hòa hợp lệ (lớn hơn 0).")
                    return@setOnClickListener
                }
            }
            if (cbWaterHeater.isChecked) {
                val qty = edtWaterHeaterQty.text.toString().toIntOrNull() ?: 0
                if (qty <= 0) {
                    MessageUtils.showInfoDialog(requireContext(), "Thiếu thông tin (Mục 4)", "Vui lòng nhập số lượng Bình nóng lạnh hợp lệ (lớn hơn 0).")
                    return@setOnClickListener
                }
            }
            if (cbWasher.isChecked) {
                val qty = edtWasherQty.text.toString().toIntOrNull() ?: 0
                if (qty <= 0) {
                    MessageUtils.showInfoDialog(requireContext(), "Thiếu thông tin (Mục 4)", "Vui lòng nhập số lượng Máy giặt hợp lệ (lớn hơn 0).")
                    return@setOnClickListener
                }
            }
            if (cbDryingArea.isChecked) {
                val qty = edtDryingAreaQty.text.toString().toIntOrNull() ?: 0
                if (qty <= 0) {
                    MessageUtils.showInfoDialog(requireContext(), "Thiếu thông tin (Mục 4)", "Vui lòng nhập số lượng Giàn phơi hợp lệ (lớn hơn 0).")
                    return@setOnClickListener
                }
            }
            if (cbWardrobe.isChecked) {
                val qty = edtWardrobeQty.text.toString().toIntOrNull() ?: 0
                if (qty <= 0) {
                    MessageUtils.showInfoDialog(requireContext(), "Thiếu thông tin (Mục 4)", "Vui lòng nhập số lượng Tủ quần áo hợp lệ (lớn hơn 0).")
                    return@setOnClickListener
                }
            }
            if (cbBed.isChecked) {
                val qty = edtBedQty.text.toString().toIntOrNull() ?: 0
                if (qty <= 0) {
                    MessageUtils.showInfoDialog(requireContext(), "Thiếu thông tin (Mục 4)", "Vui lòng nhập số lượng Giường hợp lệ (lớn hơn 0).")
                    return@setOnClickListener
                }
            }

            val room = Room(
                ownerName = edtOwnerName.text.toString().trim(),
                ownerPhone = edtOwnerPhone.text.toString().trim(),
                ownerGender = when (rgOwnerGender.checkedRadioButtonId) { R.id.rbOwnerMale -> "Nam"; R.id.rbOwnerFemale -> "Nữ"; else -> "" },
                ownerAvatarUrl = currentOwnerAvatarUrl,
                title = edtTitle.text.toString().trim(),
                ward = wardName, district = if (districtName.isEmpty()) wardName else districtName,
                latitude = lat,
                longitude = lng,
                address = edtAddress.text.toString().trim(),
                description = edtDescription.text.toString().trim(),
                price = NumberFormatUtils.getRawNumber(edtPrice).toLongOrNull() ?: 0,
                area = edtArea.text.toString().toIntOrNull() ?: 0,
                peopleCount = edtPeopleCount.text.toString().toIntOrNull() ?: 0,
                roomType = when (rgRoomStyle.checkedRadioButtonId) { R.id.rbShared -> "Chung chủ"; R.id.rbPrivate -> "Riêng chủ"; else -> "" },
                depositMonths = edtDepositMonths.text.toString().toIntOrNull() ?: 0,
                depositAmount = NumberFormatUtils.getRawNumber(edtDepositAmount).toLongOrNull() ?: 0,
                hasWifi = cbWifi.isChecked,
                hasElectric = cbElectric.isChecked,
                hasWater = cbWater.isChecked,
                wifiPrice = NumberFormatUtils.getRawNumber(edtWifiPrice).toLongOrNull() ?: 0,
                electricPrice = NumberFormatUtils.getRawNumber(edtElectricPrice).toLongOrNull() ?: 0,
                waterPrice = NumberFormatUtils.getRawNumber(edtWaterPrice).toLongOrNull() ?: 0,
                otherFees = otherFeeRows.mapNotNull { (lbl, prc) ->
                    val label = lbl.text.toString().trim()
                    val price = NumberFormatUtils.getRawNumber(prc).toLongOrNull() ?: 0L
                    if (label.isNotEmpty()) mapOf<String, Any>("label" to label, "price" to price) else null
                },
                hasAirCon = cbAirCon.isChecked, hasWaterHeater = cbWaterHeater.isChecked,
                hasWasher = cbWasher.isChecked, hasDryingArea = cbDryingArea.isChecked,
                hasWardrobe = cbWardrobe.isChecked, hasBed = cbBed.isChecked,
                airConQty = if (cbAirCon.isChecked) edtAirConQty.text.toString().toIntOrNull() ?: 1 else 0,
                waterHeaterQty = if (cbWaterHeater.isChecked) edtWaterHeaterQty.text.toString().toIntOrNull() ?: 1 else 0,
                washerQty = if (cbWasher.isChecked) edtWasherQty.text.toString().toIntOrNull() ?: 1 else 0,
                dryingAreaQty = if (cbDryingArea.isChecked) edtDryingAreaQty.text.toString().toIntOrNull() ?: 1 else 0,
                wardrobeQty = if (cbWardrobe.isChecked) edtWardrobeQty.text.toString().toIntOrNull() ?: 1 else 0,
                bedQty = if (cbBed.isChecked) edtBedQty.text.toString().toIntOrNull() ?: 1 else 0,
                furnitureItems = furnitureRows.mapNotNull { (name, qty) ->
                    val n = name.text.toString().trim()
                    val q = qty.text.toString().toIntOrNull() ?: 1
                    if (n.isNotEmpty()) mapOf<String, Any>("name" to n, "qty" to q) else null
                },
                serviceItems = serviceRows.mapNotNull { (name, price) ->
                    val n = name.text.toString().trim()
                    val p = NumberFormatUtils.getRawNumber(price).toLongOrNull() ?: 0L
                    if (n.isNotEmpty()) mapOf<String, Any>("name" to n, "price" to p) else null
                },
                kitchen = when (rgKitchen.checkedRadioButtonId) { R.id.rbKitchenShared -> "Chung"; R.id.rbKitchenPrivate -> "Riêng"; R.id.rbKitchenNone -> "Không"; else -> "" },
                bathroom = when (rgBathroom.checkedRadioButtonId) { R.id.rbBathroomShared -> "Chung"; R.id.rbBathroomPrivate -> "Riêng"; else -> "" },
                pet = when (rgPet.checkedRadioButtonId) { R.id.rbPetYes -> "Cho nuôi"; R.id.rbPetNo -> "Không"; else -> "" },
                petName = edtPetName.text.toString().trim(),
                petCount = edtPetCount.text.toString().toIntOrNull() ?: 0,
                genderPrefer = when (rgGenderPrefer.checkedRadioButtonId) { R.id.rbGenderMale -> "Nam"; R.id.rbGenderFemale -> "Nữ"; R.id.rbGenderAll -> "Tất cả"; else -> "" },
                hasMotorbike = cbMotorbike?.isChecked ?: false,
                motorbikeFee = if (cbMotorbike?.isChecked == true && rgMotorbikeFee?.checkedRadioButtonId == R.id.rbMotorbikePaid) NumberFormatUtils.getRawNumber(edtMotorbikeFee).toLongOrNull() ?: 0 else 0,
                hasEBike = cbEBike?.isChecked ?: false,
                eBikeFee = if (cbEBike?.isChecked == true && rgEBikeFee?.checkedRadioButtonId == R.id.rbEBikePaid) NumberFormatUtils.getRawNumber(edtEBikeFee).toLongOrNull() ?: 0 else 0,
                hasBicycle = cbBicycle?.isChecked ?: false,
                bicycleFee = if (cbBicycle?.isChecked == true && rgBicycleFee?.checkedRadioButtonId == R.id.rbBicyclePaid) NumberFormatUtils.getRawNumber(edtBicycleFee).toLongOrNull() ?: 0 else 0,
                curfew = when (rgCurfew.checkedRadioButtonId) { R.id.rbCurfewFree -> "Tự do"; R.id.rbCurfewCustom -> "Tùy chọn"; else -> "" },
                curfewTime = edtCurfewTime.text.toString().trim(),
                postExpiryDate = selectedExpiryMs!!,
                availableTimeSlots = timeSlotsStr,
                appointmentNotice = etAppointmentNotice.text.toString().trim(),
                status = "pending", createdAt = System.currentTimeMillis()
            )

            lastPostedTitle = room.title
            lastPostedPrice = room.price
            lastPostedLocation = listOf(room.ward, room.district).filter { it.isNotBlank() }.distinct().joinToString(", ")
            isPostProcessing = true
            viewModel.postRoom(requireContext(), room, imageUris)
        }

        setupAccordionLogic(view)
    }

    private fun setupAccordionLogic(view: View) {
        val headers = listOf(
            view.findViewById<LinearLayout>(R.id.llHeaderCard1),
            view.findViewById<LinearLayout>(R.id.llHeaderCard2),
            view.findViewById<LinearLayout>(R.id.llHeaderCard3),
            view.findViewById<LinearLayout>(R.id.llHeaderCard4),
            view.findViewById<LinearLayout>(R.id.llHeaderCard5)
        )
        val contents = listOf(
            view.findViewById<LinearLayout>(R.id.llContentCard1),
            view.findViewById<LinearLayout>(R.id.llContentCard2),
            view.findViewById<LinearLayout>(R.id.llContentCard3),
            view.findViewById<LinearLayout>(R.id.llContentCard4),
            view.findViewById<LinearLayout>(R.id.llContentCard5)
        )
        val arrows = listOf(
            view.findViewById<ImageView>(R.id.ivArrowCard1),
            view.findViewById<ImageView>(R.id.ivArrowCard2),
            view.findViewById<ImageView>(R.id.ivArrowCard3),
            view.findViewById<ImageView>(R.id.ivArrowCard4),
            view.findViewById<ImageView>(R.id.ivArrowCard5)
        )
        val nextBtns = listOf(
            view.findViewById<View>(R.id.btnNextCard1),
            view.findViewById<View>(R.id.btnNextCard2),
            view.findViewById<View>(R.id.btnNextCard3),
            view.findViewById<View>(R.id.btnNextCard4),
            null
        )

        for (i in 0..4) {
            headers[i]?.setOnClickListener {
                val isVisible = contents[i]?.visibility == View.VISIBLE
                if (isVisible) {
                    contents[i]?.visibility = View.GONE
                    arrows[i]?.animate()?.rotation(0f)?.setDuration(200)?.start()
                } else {
                    contents[i]?.visibility = View.VISIBLE
                    arrows[i]?.animate()?.rotation(90f)?.setDuration(200)?.start()
                }
            }

            nextBtns[i]?.setOnClickListener {
                contents[i]?.visibility = View.GONE
                arrows[i]?.animate()?.rotation(0f)?.setDuration(200)?.start()
                if (i + 1 < 5) {
                    contents[i + 1]?.visibility = View.VISIBLE
                    arrows[i + 1]?.animate()?.rotation(90f)?.setDuration(200)?.start()
                }
            }
        }
    }

    private fun setupParkingListeners(view: View) {
        val cbMotorbike = view.findViewById<CheckBox>(R.id.cbMotorbike) ?: return
        val rgMotorbikeFee = view.findViewById<RadioGroup>(R.id.rgMotorbikeFee) ?: return
        val edtMotorbikeFee = view.findViewById<EditText>(R.id.edtMotorbikeFee) ?: return
        cbMotorbike.setOnCheckedChangeListener { _, isChecked -> rgMotorbikeFee.visibility = if (isChecked) View.VISIBLE else View.GONE; if (!isChecked) { edtMotorbikeFee.visibility = View.GONE; edtMotorbikeFee.text?.clear(); rgMotorbikeFee.check(R.id.rbMotorbikeFree) } }
        rgMotorbikeFee.setOnCheckedChangeListener { _, checkedId -> edtMotorbikeFee.visibility = if (checkedId == R.id.rbMotorbikePaid) View.VISIBLE else View.GONE; if (checkedId == R.id.rbMotorbikeFree) edtMotorbikeFee.text?.clear() }

        val cbEBike = view.findViewById<CheckBox>(R.id.cbEBike) ?: return
        val rgEBikeFee = view.findViewById<RadioGroup>(R.id.rgEBikeFee) ?: return
        val edtEBikeFee = view.findViewById<EditText>(R.id.edtEBikeFee) ?: return
        cbEBike.setOnCheckedChangeListener { _, isChecked -> rgEBikeFee.visibility = if (isChecked) View.VISIBLE else View.GONE; if (!isChecked) { edtEBikeFee.visibility = View.GONE; edtEBikeFee.text?.clear(); rgEBikeFee.check(R.id.rbEBikeFree) } }
        rgEBikeFee.setOnCheckedChangeListener { _, checkedId -> edtEBikeFee.visibility = if (checkedId == R.id.rbEBikePaid) View.VISIBLE else View.GONE; if (checkedId == R.id.rbEBikeFree) edtEBikeFee.text?.clear() }

        val cbBicycle = view.findViewById<CheckBox>(R.id.cbBicycle) ?: return
        val rgBicycleFee = view.findViewById<RadioGroup>(R.id.rgBicycleFee) ?: return
        val edtBicycleFee = view.findViewById<EditText>(R.id.edtBicycleFee) ?: return
        cbBicycle.setOnCheckedChangeListener { _, isChecked -> rgBicycleFee.visibility = if (isChecked) View.VISIBLE else View.GONE; if (!isChecked) { edtBicycleFee.visibility = View.GONE; edtBicycleFee.text?.clear(); rgBicycleFee.check(R.id.rbBicycleFree) } }
        rgBicycleFee.setOnCheckedChangeListener { _, checkedId -> edtBicycleFee.visibility = if (checkedId == R.id.rbBicyclePaid) View.VISIBLE else View.GONE; if (checkedId == R.id.rbBicycleFree) edtBicycleFee.text?.clear() }

        NumberFormatUtils.addFormatWatcher(edtMotorbikeFee)
        NumberFormatUtils.addFormatWatcher(edtEBikeFee)
        NumberFormatUtils.addFormatWatcher(edtBicycleFee)
    }

    fun showDiscardDialog(onConfirmed: () -> Unit) {
        MessageUtils.showConfirmDialog(
            context = requireContext(),
            title = "Bỏ nội dung đã nhập?",
            message = "Thông tin bạn đã điền sẽ bị xóa. Bạn có muốn thoát không?",
            positiveText = "Thoát",
            negativeText = "Tiếp tục điền",
            onConfirm = onConfirmed
        )
    }

    private fun resetForm(view: View) {
        isDirty = false
        listOf(R.id.edtOwnerName, R.id.edtOwnerPhone, R.id.edtTitle, R.id.edtAddress,
            R.id.edtDescription, R.id.edtPrice, R.id.edtArea, R.id.edtPeopleCount,
            R.id.edtDepositMonths, R.id.edtDepositAmount, R.id.edtWifiPrice,
            R.id.edtElectricPrice, R.id.edtWaterPrice, R.id.edtPetName, R.id.edtPetCount,
            R.id.edtMotorbikeFee, R.id.edtEBikeFee, R.id.edtBicycleFee,
            R.id.edtAirConQty, R.id.edtWaterHeaterQty, R.id.edtWasherQty,
            R.id.edtDryingAreaQty, R.id.edtWardrobeQty, R.id.edtBedQty
        ).forEach { id -> view.findViewById<EditText>(id)?.text?.clear() }
        view.findViewById<TextView>(R.id.edtCurfewTime)?.text = ""
        otherFeeRows.clear()
        otherFeeContainer?.removeAllViews()
        addFeeRow()
        furnitureRows.clear()
        furnitureContainer?.removeAllViews()
        serviceRows.clear()
        serviceContainer?.removeAllViews()
        view.findViewById<com.google.android.material.chip.Chip>(R.id.chipPostPhuong)?.isChecked = true
        view.findViewById<Spinner>(R.id.spinnerWard)?.setSelection(0)
        selectedLatitude = null
        selectedLongitude = null
        selectedLocationAddress = ""
        selectedExpiryMs = null
        view.findViewById<TextView>(R.id.tvPickedLocation)?.text = "Chưa chọn vị trí chính xác"
        view.findViewById<TextInputEditText>(R.id.etExpiryDate)?.setText("")
        listOf(R.id.cbWifi, R.id.cbElectric, R.id.cbWater, R.id.cbAirCon, R.id.cbWaterHeater,
            R.id.cbWasher, R.id.cbDryingArea, R.id.cbWardrobe, R.id.cbBed,
            R.id.cbMotorbike, R.id.cbEBike, R.id.cbBicycle
        ).forEach { id -> view.findViewById<CheckBox>(id)?.isChecked = false }
        listOf(R.id.rgOwnerGender, R.id.rgRoomStyle, R.id.rgKitchen, R.id.rgBathroom,
            R.id.rgPet, R.id.rgGenderPrefer, R.id.rgCurfew
        ).forEach { id -> view.findViewById<RadioGroup>(id)?.clearCheck() }
        imageUris.clear()
        val layoutPhotos = view.findViewById<LinearLayout>(R.id.layoutPhotos)
        while (layoutPhotos.childCount > 1) layoutPhotos.removeViewAt(0)
        view.findViewById<TextView>(R.id.tvPhotoCount)?.text = "0/$MAX_PHOTOS ảnh"
        view.findViewById<CardView>(R.id.btnAddPhoto)?.visibility = View.VISIBLE

        // Reload owner info after form reset — không gọi Firebase từ Fragment
        viewModel.loadOwnerInfo()
    }

    private fun showRulesDialog(isFirstTime: Boolean = false) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_rules, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(!isFirstTime) // Nếu là lần đầu, bắt buộc phải nhấn nút đồng ý
            .create()

        val btnAccept = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAcceptRules)

        btnAccept.setOnClickListener {
            dialog.dismiss()
            if (isFirstTime) {
                viewModel.markRulesAccepted()
                // Hiển thị Dialog thông báo Quota ngay sau khi đồng ý nội quy
                MessageUtils.showInfoDialog(
                    requireContext(),
                    "Quyền lợi đăng bài",
                    "Chúc mừng bạn đã trở thành Chủ trọ!\n\nBạn được cấp 3 lượt đăng bài miễn phí. Mỗi ngày, nếu sử dụng hết 3 lượt, bạn sẽ cần chờ 24 giờ (tính từ lần đăng bài cũ nhất) để hệ thống cấp lại 3 lượt mới.\n\n*Lưu ý: Lượt miễn phí không được cộng dồn qua các ngày.*"
                )
            }
        }

        dialog.show()
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun showPostLoadingDialog() {
        if (!isAdded) return
        if (postLoadingDialog?.isShowing == true) return
        postLoadingDialog = MessageUtils.showLoadingDialog(
            requireContext(),
            title = "Đang đăng bài",
            message = "Bài đăng đang được tải lên và gửi đến quản trị viên để duyệt."
        )
    }

    private fun updatePostLoadingDialog(progress: Int) {
        postLoadingDialog?.findViewById<TextView>(R.id.tvLoadingMessage)?.text =
            "Đang tải ảnh và gửi quản trị viên duyệt... $progress%"
    }

    private fun dismissPostLoadingDialog() {
        postLoadingDialog?.dismiss()
        postLoadingDialog = null
    }

    // ─────────────────────────────────────────────────────────────────
    // UPGRADE SLOTS — VietQR Payment Flow
    // ─────────────────────────────────────────────────────────────────

    data class SlotPackage(val label: String, val code: String, val slots: Int, val price: Int)

    private val slotPackages = listOf(
        SlotPackage("+3 lượt đăng bài",  "GOI01", 3,  10_000),
        SlotPackage("+5 lượt đăng bài (Phổ biến)", "GOI02", 5, 20_000),
        SlotPackage("+10 lượt đăng bài", "GOI03", 10, 40_000)
    )

    companion object {
        private const val DEFAULT_BANK_ID = "970422"
        private const val DEFAULT_ACCOUNT_NO = "9999999999"
        private const val DEFAULT_ACCOUNT_NAME = "TAI KHOAN DEMO"
        private const val DEFAULT_BANK_DISPLAY = "MB Bank"
    }

    private var activeBankId = DEFAULT_BANK_ID
    private var activeAccountNo = DEFAULT_ACCOUNT_NO
    private var activeAccountName = DEFAULT_ACCOUNT_NAME
    private var activeBankDisplay = DEFAULT_BANK_DISPLAY

    private fun fetchPaymentConfig(onComplete: () -> Unit) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("system_configs")
            .document("payment")
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    activeBankId = doc.getString("bankId") ?: activeBankId
                    activeAccountNo = doc.getString("accountNo") ?: activeAccountNo
                    activeAccountName = doc.getString("accountName") ?: activeAccountName
                    activeBankDisplay = doc.getString("bankDisplay") ?: activeBankDisplay
                }
                onComplete()
            }
            .addOnFailureListener {
                onComplete()
            }
    }

    private fun showUpgradeSlotsDialog() {
        if (!isAdded) return
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_upgrade_slots, null)

        val dialog = AlertDialog.Builder(requireContext(), R.style.RoundedDialogStyle)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        upgradeSlotsDialog = dialog

        var packageSelected = false

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCloseUpgrade)
            .setOnClickListener { dialog.dismiss() }

        dialogView.findViewById<android.view.View>(R.id.layoutPkg1).setOnClickListener {
            packageSelected = true
            dialog.dismiss(); showPaymentQrDialog(slotPackages[0])
        }
        dialogView.findViewById<android.view.View>(R.id.layoutPkg2).setOnClickListener {
            packageSelected = true
            dialog.dismiss(); showPaymentQrDialog(slotPackages[1])
        }
        dialogView.findViewById<android.view.View>(R.id.layoutPkg3).setOnClickListener {
            packageSelected = true
            dialog.dismiss(); showPaymentQrDialog(slotPackages[2])
        }

        val edtCustomSlots = dialogView.findViewById<EditText>(R.id.edtCustomSlots)
        val tvCustomPrice = dialogView.findViewById<TextView>(R.id.tvCustomPrice)
        val btnBuyCustom = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBuyCustom)

        edtCustomSlots.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val slotsStr = s?.toString() ?: ""
                val slots = slotsStr.toIntOrNull() ?: 0
                if (slots > 0) {
                    val price = slots * 5_000L
                    tvCustomPrice.text = "%,d đ".format(price).replace(",", ".")
                    btnBuyCustom.isEnabled = true
                } else {
                    tvCustomPrice.text = "0 đ"
                    btnBuyCustom.isEnabled = false
                }
            }
        })

        btnBuyCustom.setOnClickListener {
            val slotsStr = edtCustomSlots.text.toString()
            val slots = slotsStr.toIntOrNull() ?: 0
            if (slots > 0) {
                packageSelected = true
                dialog.dismiss()
                val customPkg = SlotPackage("+$slots lượt đăng bài (Tùy chọn)", "CUSTOM", slots, slots * 5_000)
                showPaymentQrDialog(customPkg)
            }
        }

        dialog.setOnDismissListener {
            upgradeSlotsDialog = null
            if (!packageSelected) {
                // Nếu người dùng đóng bảng chọn gói mà chưa mua, kiểm tra lại quota
                // để hiện lại dialog "Hết lượt" nếu họ vẫn đang bị chặn
                viewModel.checkPrePostQuota()
            }
        }

        dialog.show()
    }

    private fun showPaymentQrDialog(pkg: SlotPackage) {
        if (!isAdded) return
        // Delay 200ms để dialog trước kịp ẩn dim, tránh double-dim làm màn hình quá tối
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (!isAdded) return@postDelayed
            val builder = AlertDialog.Builder(requireContext(), R.style.RoundedDialogStyle)
                .setTitle("Lưu ý quan trọng trước khi thanh toán")
                .setMessage(
                    "Khi quét mã QR, ứng dụng ngân hàng sẽ tự điền đầy đủ thông tin.\n\n" +
                    "⚠ KHÔNG thay đổi số tiền hoặc nội dung chuyển khoản.\n\n" +
                    "Nếu chuyển sai số tiền hoặc sai nội dung, hệ thống sẽ không xác nhận giao dịch " +
                    "và bạn sẽ không nhận được lượt đăng bài. Số tiền đã chuyển sẽ không được hoàn lại."
                )
                .setPositiveButton("Đã hiểu, tiếp tục") { warningDialog, _ ->
                    warningDialog.dismiss()
                    val progressDialog = MessageUtils.showLoadingDialog(requireContext(), "Đang tải thông tin thanh toán...")
                    fetchPaymentConfig {
                        progressDialog.dismiss()
                        proceedToPaymentQr(pkg)
                    }
                }
                .setNegativeButton("Quay lại") { warningDialog, _ ->
                    warningDialog.dismiss()
                    showUpgradeSlotsDialog()
                }
            
            val dialog = builder.create()
            dialog.setOnDismissListener {
                paymentWarningDialog = null
            }
            paymentWarningDialog = dialog
            dialog.show()
            
            dialog.window?.apply {
                setBackgroundDrawableResource(R.drawable.bg_dialog_surface)
                val width = (resources.displayMetrics.widthPixels * 0.9f).toInt()
                setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
        }, 200L)
    }

    // Mã nội dung
    private fun proceedToPaymentQr(pkg: SlotPackage) {
        if (!isAdded) return
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val docRef = db.collection("slot_upgrade_requests").document()
        val requestId = docRef.id
        val requestCode = requestId.takeLast(8).uppercase(Locale.US)
        val addInfo = "MUA ${pkg.code} REQ_$requestCode"
        val now = System.currentTimeMillis()
        val expiresAt = now + 30L * 60L * 1000L

        val request = hashMapOf(
            "uid" to uid,
            "requestId" to requestId,
            "slots" to pkg.slots,
            "amount" to pkg.price,
            "label" to pkg.label,
            "code" to pkg.code,
            "transferNote" to addInfo,
            "status" to "waiting_for_payment",
            "createdAt" to now,
            "updatedAt" to now,
            "expiresAt" to expiresAt
        )

        docRef.set(request)
            .addOnSuccessListener {
                showPaymentQrDialogContent(docRef, pkg, addInfo, expiresAt)
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                MessageUtils.showErrorDialog(
                    requireContext(),
                    "Không thể tạo giao dịch",
                    "Lỗi khởi tạo yêu cầu thanh toán. Vui lòng thử lại."
                )
            }
    }

    // Hiên thị mã qr
    private fun showPaymentQrDialogContent(
        docRef: com.google.firebase.firestore.DocumentReference,
        pkg: SlotPackage,
        addInfo: String,
        expiresAt: Long
    ) {
        if (!isAdded) return
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_payment_qr, null)

        val dialog = AlertDialog.Builder(requireContext(), R.style.RoundedDialogStyle)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        paymentQrDialog = dialog


        dialogView.findViewById<TextView>(R.id.tvQrPackageName).text = "${pkg.label} — ${"%,.0f".format(pkg.price.toDouble())}đ"
        dialogView.findViewById<TextView>(R.id.tvQrBank).text = activeBankDisplay
        dialogView.findViewById<TextView>(R.id.tvQrAccountNo).text = activeAccountNo
        dialogView.findViewById<TextView>(R.id.tvQrAccountName).text = activeAccountName
        dialogView.findViewById<TextView>(R.id.tvQrTransferNote).text = addInfo

        val cleanBankId = activeBankId.trim().lowercase(java.util.Locale.US)
        val normalizedBankId = if (cleanBankId == "mb") "970422" else activeBankId.trim()
        val normalizedAccountNo = activeAccountNo.trim()
        val qrUrl = "https://img.vietqr.io/image/$normalizedBankId-$normalizedAccountNo-compact2.png" +
                "?amount=${pkg.price}&addInfo=${android.net.Uri.encode(addInfo)}&accountName=${android.net.Uri.encode(activeAccountName)}"

        Glide.with(this)
            .load(qrUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_dialog_alert)
            .into(dialogView.findViewById(R.id.imgVietQR))

        val btnConfirm = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConfirmPayment)
        btnConfirm.isEnabled = false
        btnConfirm.text = "Đang chờ xác nhận thanh toán..."
        btnConfirm.setBackgroundColor(android.graphics.Color.GRAY)

        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancelPayment)
        val tvCountdown = dialogView.findViewById<TextView>(R.id.tvQrCountdown)

        var paymentCompleted = false
        var latestStatus = "waiting_for_payment"

        fun applyPaymentStatus(status: String) {
            latestStatus = status
            when (status) {
                "paid" -> {
                    btnConfirm.isEnabled = true
                    btnConfirm.text = "Hoàn tất giao dịch"
                    btnConfirm.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary))
                    btnCancel.visibility = android.view.View.GONE
                }
                "waiting_for_payment" -> {
                    btnConfirm.isEnabled = false
                    btnConfirm.text = "Đang chờ xác nhận thanh toán..."
                    btnConfirm.setBackgroundColor(android.graphics.Color.GRAY)
                    btnCancel.visibility = android.view.View.VISIBLE
                }
                "expired" -> {
                    btnConfirm.isEnabled = false
                    btnConfirm.text = "Giao dịch đã hết hạn"
                    btnConfirm.setBackgroundColor(android.graphics.Color.GRAY)
                    btnCancel.visibility = android.view.View.GONE
                }
                "failed" -> {
                    btnConfirm.isEnabled = false
                    btnConfirm.text = "Giao dịch thất bại"
                    btnConfirm.setBackgroundColor(android.graphics.Color.GRAY)
                    btnCancel.visibility = android.view.View.GONE
                }
                "cancelled" -> {
                    btnConfirm.isEnabled = false
                    btnConfirm.text = "Đã hủy giao dịch"
                    btnConfirm.setBackgroundColor(android.graphics.Color.GRAY)
                    btnCancel.visibility = android.view.View.GONE
                }
                "amount_mismatch" -> {
                    btnConfirm.isEnabled = false
                    btnConfirm.text = "Sai số tiền — vui lòng liên hệ hỗ trợ"
                    btnConfirm.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.warning))
                    btnCancel.visibility = android.view.View.VISIBLE
                    tvCountdown.visibility = android.view.View.GONE
                }
            }
        }

        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.w("PostFragment", "Payment snapshot listener error", error)
                return@addSnapshotListener
            }
            if (snapshot == null || !snapshot.exists()) return@addSnapshotListener
            val status = snapshot.getString("status") ?: return@addSnapshotListener
            applyPaymentStatus(status)
        }

        val remainMs = expiresAt - System.currentTimeMillis()
        val paymentCountdown: CountDownTimer? = if (remainMs > 0) {
            object : CountDownTimer(remainMs, 1000L) {
                override fun onTick(millisUntilFinished: Long) {
                    if (!isAdded) return
                    val mins = millisUntilFinished / 60000
                    val secs = (millisUntilFinished % 60000) / 1000
                    tvCountdown.text = "Hết hạn sau: %02d:%02d".format(mins, secs)
                }
                override fun onFinish() {
                    if (!isAdded) return
                    tvCountdown.text = "Giao dịch đã hết hạn"
                }
            }.also { it.start() }
        } else {
            tvCountdown.text = "Giao dịch đã hết hạn"
            null
        }

        btnCancel.setOnClickListener {
            if (latestStatus == "paid") {
                dialog.dismiss()
                return@setOnClickListener
            }

            MessageUtils.showConfirmDialog(
                context = requireContext(),
                title = "Xác nhận hủy giao dịch",
                message = "Nếu bạn ĐÃ thực hiện chuyển tiền thành công bằng app ngân hàng, vui lòng KHÔNG hủy giao dịch này để tránh thất thoát và được hỗ trợ tốt nhất.\n\nBạn có chắc chắn muốn hủy giao dịch?",
                positiveText = "Hủy giao dịch",
                negativeText = "Quay lại",
                onConfirm = {
                    btnCancel.isEnabled = false
                    btnCancel.text = "Đang hủy..."
                    val cancelAt = System.currentTimeMillis()
                    docRef.update(
                        mapOf(
                            "status" to "cancelled",
                            "cancelledAt" to cancelAt,
                            "updatedAt" to cancelAt
                        )
                    ).addOnCompleteListener { dialog.dismiss() }
                }
            )
        }

        btnConfirm.setOnClickListener {
            if (latestStatus != "paid") return@setOnClickListener
            paymentCompleted = true
            dialog.dismiss()
            MessageUtils.showInfoDialog(
                requireContext(),
                "Thanh toán thành công",
                "Hệ thống đã ghi nhận thanh toán. Lượt đăng bài mới của bạn đang được cập nhật tự động trong giây lát."
            )
            viewModel.loadUserObject(forceFromServer = true)
        }

        dialog.setOnDismissListener {
            paymentQrDialog = null
            paymentCountdown?.cancel()
            listener.remove()
            if (!paymentCompleted) {
                viewModel.checkPrePostQuota()
            }
        }
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        if (!isVisible) return
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            viewModel.loadUserObject()
            viewModel.loadOwnerInfo()
            val mainViewModel = androidx.lifecycle.ViewModelProvider(requireActivity())[com.example.doantotnghiep.ViewModel.MainViewModel::class.java]
            mainViewModel.loadAppointmentBadgeForCurrentUser(currentUser.uid)
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                viewModel.loadUserObject()
                viewModel.loadOwnerInfo()
                viewModel.checkPrePostQuota()
            }
        }
    }

    override fun onDestroyView() {
        dismissPostLoadingDialog()
        dismissPostQuotaDialog()
        upgradeSlotsDialog?.dismiss()
        upgradeSlotsDialog = null
        paymentWarningDialog?.dismiss()
        paymentWarningDialog = null
        paymentQrDialog?.dismiss()
        paymentQrDialog = null
        super.onDestroyView()
    }
}
