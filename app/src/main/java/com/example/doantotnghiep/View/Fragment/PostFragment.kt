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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
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

import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.firestore.Source

class PostFragment : Fragment() {

    private var verifyRequiredView: View? = null
    private var postFormView: View? = null
    private var layoutGuest: View? = null

    private val imageUris = mutableListOf<Uri>()
    private val MAX_PHOTOS = 10
    private var isFormSetup = false
    private var userRoleChecked = false

    private lateinit var viewModel: PostViewModel

    private var lastPostedTitle = ""
    private var lastPostedPrice = 0L
    private var lastPostedLocation = ""
    private var currentOwnerAvatarUrl = ""
    private var postLoadingDialog: AlertDialog? = null
    private var postQuotaDialog: AlertDialog? = null
    private var postQuotaTimer: CountDownTimer? = null

    private var rulesDialogShown = false  // Tránh show dialog quy định 2 lần do observer trigger lại
    private var lastPostUnlockDialogTimestamp: Long = -1L
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null
    private var selectedLocationAddress: String = ""

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data?.clipData != null) {
                val count = data.clipData!!.itemCount
                for (i in 0 until count) {
                    val uri = data.clipData!!.getItemAt(i).uri
                    if (imageUris.size < MAX_PHOTOS) { imageUris.add(uri); addPhotoToLayout(uri) }
                }
            } else if (data?.data != null) {
                val uri = data.data!!
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
        if (selectedLocationAddress.isNotBlank()) {
            val adapterCount = spinnerWard.adapter?.count ?: 0
            for (i in 0 until adapterCount) {
                val item = spinnerWard.adapter.getItem(i)?.toString() ?: continue
                // Lấy tên phường/xã từ item spinner (bỏ phần quận trong ngoặc)
                val wardInSpinner = if (item.contains("(")) item.substringBefore("(").trim() else item
                if (wardInSpinner.isNotBlank() &&
                    selectedLocationAddress.contains(wardInSpinner, ignoreCase = true)) {
                    spinnerWard.setSelection(i)
                    break
                }
            }
        }

        tvPickedLocation.text = if (selectedLocationAddress.isNotBlank()) {
            "📍 Đã chọn: $selectedLocationAddress"
        } else {
            "📍 Đã chọn tọa độ: %.6f, %.6f".format(Locale.US, lat, lng)
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
                if (!user.hasAcceptedRules && !rulesDialogShown) {
                    rulesDialogShown = true
                    showRulesDialog(isFirstTime = true)
                }
                viewModel.checkPrePostQuota()
            } else {
                when (user.role) {
                    "pending" -> showPendingStatus()
                    "rejected" -> showRejectedStatus(user.occupation)
                    else -> showVerifyRequired()
                }
            }
        }


        viewModel.ownerInfo.observe(viewLifecycleOwner) { (name, phone, avatarUrl) ->
            val v = postFormView ?: return@observe
            v.findViewById<EditText>(R.id.edtOwnerName)?.setText(name)
            v.findViewById<EditText>(R.id.edtOwnerPhone)?.setText(phone)
            currentOwnerAvatarUrl = avatarUrl
        }

        viewModel.postQuotaBlocked.observe(viewLifecycleOwner) { info ->
            if (info == null) return@observe
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

    private fun showVerifyRequired() {
        verifyRequiredView?.visibility = View.VISIBLE
        postFormView?.visibility = View.GONE

        val tvVerifyTitle = verifyRequiredView?.findViewById<TextView>(R.id.tvVerifyTitle)
        val tvVerifyStatus = verifyRequiredView?.findViewById<TextView>(R.id.tvVerifyStatus)
        val btnStartVerify = verifyRequiredView?.findViewById<MaterialButton>(R.id.btnStartVerify)
        val tvViewRules = verifyRequiredView?.findViewById<TextView>(R.id.tvViewRulesBeforeVerify)

        tvVerifyTitle?.text = "Xác minh\nChủ cho thuê"
        tvVerifyStatus?.text = "Để tham gia vào cộng đồng tìm trọ minh bạch, bạn vui lòng hoàn tất xác minh danh tính. Việc này giúp tăng độ tin cậy đối với khách hàng và đảm bảo quyền lợi pháp lý cho bạn."

        btnStartVerify?.visibility = View.VISIBLE
        btnStartVerify?.text = "BẮT ĐẦU XÁC MINH NGAY"
        tvViewRules?.visibility = View.VISIBLE
    }

    private fun showPendingStatus() {
        verifyRequiredView?.visibility = View.VISIBLE
        postFormView?.visibility = View.GONE

        val tvVerifyTitle = verifyRequiredView?.findViewById<TextView>(R.id.tvVerifyTitle)
        val tvVerifyStatus = verifyRequiredView?.findViewById<TextView>(R.id.tvVerifyStatus)
        val btnStartVerify = verifyRequiredView?.findViewById<MaterialButton>(R.id.btnStartVerify)
        val tvViewRules = verifyRequiredView?.findViewById<TextView>(R.id.tvViewRulesBeforeVerify)

        tvVerifyTitle?.text = "Đang chờ\nPhê duyệt"
        tvVerifyStatus?.text = "Hồ sơ của bạn đã được gửi thành công. Đội ngũ kiểm duyệt đang tiến hành xác minh thông tin. Vui lòng quay lại sau 24-48h làm việc."

        btnStartVerify?.visibility = View.GONE
        tvViewRules?.visibility = View.VISIBLE
    }

    private fun showRejectedStatus(reason: String) {
        verifyRequiredView?.visibility = View.VISIBLE
        postFormView?.visibility = View.GONE

        val tvVerifyTitle = verifyRequiredView?.findViewById<TextView>(R.id.tvVerifyTitle)
        val tvVerifyStatus = verifyRequiredView?.findViewById<TextView>(R.id.tvVerifyStatus)
        val btnStartVerify = verifyRequiredView?.findViewById<MaterialButton>(R.id.btnStartVerify)
        val tvViewRules = verifyRequiredView?.findViewById<TextView>(R.id.tvViewRulesBeforeVerify)

        tvVerifyTitle?.text = "Xác minh\nBị từ chối"
        tvVerifyStatus?.text = "Rất tiếc, hồ sơ của bạn chưa đáp ứng đủ điều kiện.\nLý do: $reason\n\nVui lòng cập nhật lại thông tin chính xác để gửi lại yêu cầu."

        btnStartVerify?.visibility = View.VISIBLE
        btnStartVerify?.text = "GỬI LẠI YÊU CẦU"
        tvViewRules?.visibility = View.VISIBLE
    }

    private fun showPostForm() {
        verifyRequiredView?.visibility = View.GONE
        postFormView?.visibility = View.VISIBLE
        if (!isFormSetup) {
            setupPostForm()
            isFormSetup = true
        }
    }

    private fun showPostUnlockWaitingStatus(unlockAt: Long, now: Long) {
        verifyRequiredView?.visibility = View.VISIBLE
        postFormView?.visibility = View.GONE

        val tvVerifyTitle = verifyRequiredView?.findViewById<TextView>(R.id.tvVerifyTitle)
        val tvVerifyStatus = verifyRequiredView?.findViewById<TextView>(R.id.tvVerifyStatus)
        val btnStartVerify = verifyRequiredView?.findViewById<MaterialButton>(R.id.btnStartVerify)
        val tvViewRules = verifyRequiredView?.findViewById<TextView>(R.id.tvViewRulesBeforeVerify)

        tvVerifyTitle?.text = "Đã cấp quyền\nĐăng bài"
        tvVerifyStatus?.text = "Tài khoản của bạn đã được admin duyệt. " +
                "Hệ thống sẽ mở đăng bài sau " + formatRemainingTime(unlockAt - now) +
                " (dự kiến lúc " + formatDateTime(unlockAt) + ")."
        btnStartVerify?.visibility = View.GONE
        tvViewRules?.visibility = View.VISIBLE
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

        tvQuotaMessage.text = "Số lần đăng bài trong ngày của bạn đã hết. Sau 24 giờ kể từ lúc này, bạn sẽ có thêm 3 lượt đăng bài mới."
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
        val cbWaterHeater = view.findViewById<CheckBox>(R.id.cbWaterHeater)
        val cbWasher = view.findViewById<CheckBox>(R.id.cbWasher)
        val cbDryingArea = view.findViewById<CheckBox>(R.id.cbDryingArea)
        val cbWardrobe = view.findViewById<CheckBox>(R.id.cbWardrobe)
        val cbBed = view.findViewById<CheckBox>(R.id.cbBed)
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

        val allAreas = mutableListOf("-- Chọn phường/xã --")
        allAreas.addAll(AddressData.phuongList.drop(1))
        allAreas.addAll(AddressData.xaList.drop(1))
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, allAreas)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerWard.adapter = adapter
        tvPickedLocation.text = "Chưa chọn vị trí chính xác"

        btnPickLocation.setOnClickListener {
            val selectedWard = spinnerWard.selectedItem?.toString().orEmpty()
            val wardName = if (selectedWard.contains("(")) selectedWard.substringBefore("(").trim() else selectedWard
            val districtName = if (selectedWard.contains("(")) selectedWard.substringAfter("(").replace(")", "").trim() else ""
            val intent = Intent(requireContext(), LocationPickerActivity::class.java).apply {
                putExtra(LocationPickerActivity.EXTRA_INITIAL_ADDRESS, edtAddress.text.toString().trim())
                putExtra(LocationPickerActivity.EXTRA_INITIAL_WARD, wardName)
                putExtra(LocationPickerActivity.EXTRA_INITIAL_DISTRICT, districtName)
            }
            pickLocationLauncher.launch(intent)
        }

        NumberFormatUtils.addFormatWatcher(edtPrice)
        NumberFormatUtils.addFormatWatcher(edtWifiPrice)
        NumberFormatUtils.addFormatWatcher(edtElectricPrice)
        NumberFormatUtils.addFormatWatcher(edtWaterPrice)
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
                val (postId, thumbnailUrl) = result
                viewModel.resetPostResult()
                PostNotificationHelper.showSuccess(requireContext(), lastPostedTitle)
                resetForm(view)
                val intent = Intent(requireContext(), com.example.doantotnghiep.View.Auth.PostSuccessActivity::class.java).apply {
                    putExtra("postId", postId)
                    putExtra("thumbnail", thumbnailUrl)
                    putExtra("title", lastPostedTitle)
                    putExtra("price", lastPostedPrice)
                    putExtra("location", lastPostedLocation)
                }
                startActivity(intent)
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrEmpty()) {
                dismissPostLoadingDialog()
                PostNotificationHelper.cancel(requireContext())
                MessageUtils.showErrorDialog(requireContext(), "Lỗi đăng bài", error)
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
            if (lat == null || lng == null) {
                MessageUtils.showInfoDialog(requireContext(), "Thiếu vị trí", "Vui lòng chọn vị trí trên bản đồ trước khi đăng bài.")
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

            val room = Room(
                ownerName = edtOwnerName.text.toString().trim(),
                ownerPhone = edtOwnerPhone.text.toString().trim(),
                ownerGender = when (rgOwnerGender.checkedRadioButtonId) { R.id.rbOwnerMale -> "Nam"; R.id.rbOwnerFemale -> "Nữ"; else -> "" },
                ownerAvatarUrl = currentOwnerAvatarUrl,
                title = edtTitle.text.toString().trim(),
                ward = wardName, district = districtName,
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
                hasWater = cbWater.isChecked,
                wifiPrice = NumberFormatUtils.getRawNumber(edtWifiPrice).toLongOrNull() ?: 0,
                electricPrice = NumberFormatUtils.getRawNumber(edtElectricPrice).toLongOrNull() ?: 0,
                waterPrice = NumberFormatUtils.getRawNumber(edtWaterPrice).toLongOrNull() ?: 0,
                hasAirCon = cbAirCon.isChecked, hasWaterHeater = cbWaterHeater.isChecked,
                hasWasher = cbWasher.isChecked, hasDryingArea = cbDryingArea.isChecked,
                hasWardrobe = cbWardrobe.isChecked, hasBed = cbBed.isChecked,
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
                status = "pending", createdAt = System.currentTimeMillis()
            )

            lastPostedTitle = room.title
            lastPostedPrice = room.price
            lastPostedLocation = if (room.ward.isNotEmpty()) "${room.ward}, ${room.district}" else room.district
            viewModel.postRoom(requireContext(), room, imageUris)
        }

        setupAccordionLogic(view)
    }

    private fun setupAccordionLogic(view: View) {
        val headers = listOf(
            view.findViewById<LinearLayout>(R.id.llHeaderCard1),
            view.findViewById<LinearLayout>(R.id.llHeaderCard2),
            view.findViewById<LinearLayout>(R.id.llHeaderCard3),
            view.findViewById<LinearLayout>(R.id.llHeaderCard4)
        )
        val contents = listOf(
            view.findViewById<LinearLayout>(R.id.llContentCard1),
            view.findViewById<LinearLayout>(R.id.llContentCard2),
            view.findViewById<LinearLayout>(R.id.llContentCard3),
            view.findViewById<LinearLayout>(R.id.llContentCard4)
        )
        val arrows = listOf(
            view.findViewById<ImageView>(R.id.ivArrowCard1),
            view.findViewById<ImageView>(R.id.ivArrowCard2),
            view.findViewById<ImageView>(R.id.ivArrowCard3),
            view.findViewById<ImageView>(R.id.ivArrowCard4)
        )
        val nextBtns = listOf(
            view.findViewById<View>(R.id.btnNextCard1),
            view.findViewById<View>(R.id.btnNextCard2),
            view.findViewById<View>(R.id.btnNextCard3),
            null
        )

        for (i in 0..3) {
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
                if (i + 1 < 4) {
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

    private fun resetForm(view: View) {
        listOf(R.id.edtOwnerName, R.id.edtOwnerPhone, R.id.edtTitle, R.id.edtAddress,
            R.id.edtDescription, R.id.edtPrice, R.id.edtArea, R.id.edtPeopleCount,
            R.id.edtDepositMonths, R.id.edtDepositAmount, R.id.edtWifiPrice,
            R.id.edtElectricPrice, R.id.edtWaterPrice, R.id.edtPetName, R.id.edtPetCount,
            R.id.edtMotorbikeFee, R.id.edtEBikeFee, R.id.edtBicycleFee
        ).forEach { id -> view.findViewById<EditText>(id)?.text?.clear() }
        view.findViewById<TextView>(R.id.edtCurfewTime)?.text = ""
        view.findViewById<Spinner>(R.id.spinnerWard)?.setSelection(0)
        selectedLatitude = null
        selectedLongitude = null
        selectedLocationAddress = ""
        view.findViewById<TextView>(R.id.tvPickedLocation)?.text = "Chưa chọn vị trí chính xác"
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
            if (isFirstTime) {
                viewModel.markRulesAccepted()
            }
            dialog.dismiss()
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
        SlotPackage("+3 lượt đăng bài",  "GOI01", 3,  15_000),
        SlotPackage("+10 lượt đăng bài", "GOI02", 10, 50_000)
    )

    // ⚠️ ĐIỀN THÔNG TIN NGÂN HÀNG CỦA BẠN VÀO ĐÂY:
    private val BANK_ID       = "mbbank"          // mã ngân hàng VietQR (mbbank, vietcombank, tpbank...)
    private val ACCOUNT_NO    = "0889740127"       // số tài khoản của bạn
    private val ACCOUNT_NAME  = "PHAM TIEN DAT"   // tên chủ tài khoản IN HOA không dấu
    private val BANK_DISPLAY  = "MB Bank"          // tên hiển thị trên dialog

    private fun showUpgradeSlotsDialog() {
        if (!isAdded) return
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_upgrade_slots, null)

        val dialog = AlertDialog.Builder(requireContext(), R.style.RoundedDialogStyle)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

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
                showPaymentQrDialogContent(docRef, pkg, addInfo)
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

    private fun showPaymentQrDialogContent(
        docRef: com.google.firebase.firestore.DocumentReference,
        pkg: SlotPackage,
        addInfo: String
    ) {
        if (!isAdded) return
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_payment_qr, null)

        val dialog = AlertDialog.Builder(requireContext(), R.style.RoundedDialogStyle)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<TextView>(R.id.tvQrPackageName).text = "${pkg.label} — ${"%,.0f".format(pkg.price.toDouble())}đ"
        dialogView.findViewById<TextView>(R.id.tvQrBank).text = BANK_DISPLAY
        dialogView.findViewById<TextView>(R.id.tvQrAccountNo).text = ACCOUNT_NO
        dialogView.findViewById<TextView>(R.id.tvQrAccountName).text = ACCOUNT_NAME
        dialogView.findViewById<TextView>(R.id.tvQrTransferNote).text = addInfo

        val addInfoEncoded = addInfo.replace(" ", "%20").replace("/", "%2F")
        val qrUrl = "https://img.vietqr.io/image/$BANK_ID-$ACCOUNT_NO-compact2.png" +
                "?amount=${pkg.price}&addInfo=$addInfoEncoded&accountName=${ACCOUNT_NAME.replace(" ", "%20")}"

        Glide.with(this)
            .load(qrUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_dialog_alert)
            .into(dialogView.findViewById(R.id.imgVietQR))

        val btnConfirm = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConfirmPayment)
        btnConfirm.isEnabled = false
        btnConfirm.text = "Đang chờ xác nhận thanh toán..."
        btnConfirm.setBackgroundColor(android.graphics.Color.GRAY)

        var paymentCompleted = false
        var latestStatus = "waiting_for_payment"
        val statusPollHandler = Handler(Looper.getMainLooper())
        var stopStatusPolling = false

        fun applyPaymentStatus(status: String) {
            latestStatus = status
            when (status) {
                "paid" -> {
                    btnConfirm.isEnabled = true
                    btnConfirm.text = "Hoàn tất giao dịch"
                    btnConfirm.setBackgroundColor(resources.getColor(R.color.primary))
                }
                "waiting_for_payment" -> {
                    btnConfirm.isEnabled = false
                    btnConfirm.text = "Đang chờ xác nhận thanh toán..."
                    btnConfirm.setBackgroundColor(android.graphics.Color.GRAY)
                }
                "expired" -> {
                    btnConfirm.isEnabled = false
                    btnConfirm.text = "Giao dịch đã hết hạn"
                    btnConfirm.setBackgroundColor(android.graphics.Color.GRAY)
                }
                "failed" -> {
                    btnConfirm.isEnabled = false
                    btnConfirm.text = "Giao dịch thất bại"
                    btnConfirm.setBackgroundColor(android.graphics.Color.GRAY)
                }
                "cancelled" -> {
                    btnConfirm.isEnabled = false
                    btnConfirm.text = "Đã hủy giao dịch"
                    btnConfirm.setBackgroundColor(android.graphics.Color.GRAY)
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

        val pollStatusRunnable = object : Runnable {
            override fun run() {
                if (stopStatusPolling || !dialog.isShowing) return
                docRef.get(Source.SERVER)
                    .addOnSuccessListener { serverSnap ->
                        val serverStatus = serverSnap.getString("status")
                        if (!serverStatus.isNullOrBlank()) {
                            applyPaymentStatus(serverStatus)
                        }
                    }
                    .addOnFailureListener { pollError ->
                        android.util.Log.w("PostFragment", "Payment status polling failed", pollError)
                    }
                    .addOnCompleteListener {
                        if (!stopStatusPolling && dialog.isShowing) {
                            statusPollHandler.postDelayed(this, 3000L)
                        }
                    }
            }
        }
        statusPollHandler.postDelayed(pollStatusRunnable, 3000L)

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancelPayment)
            .setOnClickListener {
                if (latestStatus == "paid") {
                    dialog.dismiss()
                    return@setOnClickListener
                }
                val cancelAt = System.currentTimeMillis()
                docRef.update(
                    mapOf(
                        "status" to "cancelled",
                        "cancelledAt" to cancelAt,
                        "updatedAt" to cancelAt
                    )
                ).addOnCompleteListener { dialog.dismiss() }
            }

        btnConfirm.setOnClickListener {
            if (latestStatus != "paid") return@setOnClickListener
            paymentCompleted = true
            dialog.dismiss()
            MessageUtils.showInfoDialog(
                requireContext(),
                "Thanh toán thành công",
                "Hệ thống đã xác nhận thanh toán. Bạn đã được cộng thêm ${pkg.slots} lượt đăng bài."
            )
            viewModel.loadUserObject()
        }

        dialog.setOnDismissListener {
            stopStatusPolling = true
            statusPollHandler.removeCallbacks(pollStatusRunnable)
            listener.remove()
            if (!paymentCompleted) {
                viewModel.checkPrePostQuota()
            }
        }
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            viewModel.loadUserObject()
            viewModel.loadOwnerInfo()
            val mainViewModel = androidx.lifecycle.ViewModelProvider(requireActivity())[com.example.doantotnghiep.ViewModel.MainViewModel::class.java]
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { doc ->
                    val role = doc.getString("role") ?: ""
                    val isVerified = doc.getBoolean("isVerified") ?: false
                    val effectiveRole = if (role == "admin") "admin" else if (isVerified) "verified" else "user"
                    mainViewModel.loadAppointmentBadge(currentUser.uid, effectiveRole)
                }
        }
    }

    override fun onDestroyView() {
        dismissPostLoadingDialog()
        dismissPostQuotaDialog()
        super.onDestroyView()
    }
}
