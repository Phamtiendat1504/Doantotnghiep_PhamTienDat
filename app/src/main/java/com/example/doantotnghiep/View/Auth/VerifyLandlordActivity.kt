package com.example.doantotnghiep.View.Auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.doantotnghiep.MainActivity
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.ViewModel.VerifyLandlordViewModel
import com.example.doantotnghiep.ViewModel.VerifyLandlordViewModel.VerificationState
import com.example.doantotnghiep.databinding.ActivityVerifyLandlordBinding
import java.io.File

class VerifyLandlordActivity : AppCompatActivity() {

    private enum class CaptureTarget { FRONT, BACK }

    private lateinit var binding: ActivityVerifyLandlordBinding
    private val viewModel: VerifyLandlordViewModel by viewModels()

    private var loadingDialog: AlertDialog? = null
    private var frontUri: Uri? = null
    private var backUri: Uri? = null
    private var captureTarget: CaptureTarget = CaptureTarget.FRONT

    // Xác minh danh tính
    // Khởi tạo Launcher để mở CccdCameraActivity và chờ kết quả trả về.
    // Nếu chụp thành công (RESULT_OK), nó sẽ lấy đường dẫn ảnh (Uri) từ Intent,
    // sau đó gán hình ảnh đó vào khung ảnh tương ứng (Mặt trước hoặc Mặt sau) trên giao diện.
    private val cccdCameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val uriString = result.data?.getStringExtra(CccdCameraActivity.EXTRA_OUTPUT_URI)
        val uri = uriString?.let(Uri::parse) ?: return@registerForActivityResult

        binding.apply {
            when (captureTarget) {
                CaptureTarget.FRONT -> {
                    frontUri = uri
                    imgFront.setImageURI(uri)
                    imgFront.visibility = View.VISIBLE
                    layoutFrontPlaceholder.visibility = View.GONE
                }
                CaptureTarget.BACK -> {
                    backUri = uri
                    imgBack.setImageURI(uri)
                    imgBack.visibility = View.VISIBLE
                    layoutBackPlaceholder.visibility = View.GONE
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyLandlordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeVerificationState()
        viewModel.checkCurrentStatus(this)
        clearAllCameraTempFolder()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            cleanupTempFiles()
        }
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        if (ev?.action == android.view.MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is android.widget.EditText) {
                val outRect = android.graphics.Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    v.clearFocus()
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        currentFocus?.let { view ->
            imm.hideSoftInputFromWindow(view.windowToken, 0)
            view.clearFocus()
        }
    }

    // Xác minh danh tính  -3-
    //Hàm theo dõi luồng trạng thái của quá trình xác minh. Khi ViewModel thay đổi dữ liệu, hàm này sẽ tự phản ứng:
    //Loading: Hiện vòng xoay chờ đợi.
    //FormReady: Mở form cho user nhập liệu.
    //Pending / AlreadyApproved: Hiện thông báo chờ duyệt hoặc đã duyệt thành công.
    //Error: Bật hộp thoại thông báo lỗi.
    private fun observeVerificationState() {
        viewModel.verificationState.observe(this) { state ->
            when (state) {
                is VerificationState.Loading -> {
                    showPrecheckLoading(true)
                }
                is VerificationState.AlreadyApprovedWaiting -> {
                    showPrecheckLoading(false)
                    MessageUtils.showInfoDialog(
                        this,
                        "Đã được cấp quyền",
                        "Quyền đăng bài của bạn đã được cấp, nhưng cần chờ thêm 24 giờ " +
                            "kể từ lúc admin duyệt.\n\n" +
                            "Còn lại: ${state.hours} giờ ${state.minutes} phút\n" +
                            "Thời điểm mở đăng bài: ${state.unlockTime}"
                    ) { finish() }
                }
                is VerificationState.AlreadyApprovedReady -> {
                    showPrecheckLoading(false)
                    MessageUtils.showSuccessDialog(
                        this,
                        "Đã xác minh",
                        "Tài khoản của bạn đã được xác minh. Bạn có thể đăng tin cho thuê ngay!"
                    ) { finish() }
                }
                is VerificationState.VerificationPending -> {
                    showPrecheckLoading(false)
                    MessageUtils.showInfoDialog(
                        this,
                        "Đang xử lý hồ sơ",
                        "Hồ sơ của bạn đã được gửi và đang được hệ thống xử lý. Nếu cần can thiệp thủ công, admin sẽ phản hồi trong 24 giờ."
                    ) { finish() }
                }
                is VerificationState.VerificationEscalatedExpired -> {
                    // Hồ sơ đã quá 24h admin chưa duyệt -> hiển thị UI cho phép người dùng hủy và nộp lại
                    showPrecheckLoading(false)
                    showExpiredCard()
                }
                is VerificationState.FormReady -> {
                    showPrecheckLoading(false)
                    setupForm()
                }
                is VerificationState.Error -> {
                    showPrecheckLoading(false)
                    MessageUtils.showInfoDialog(
                        this,
                        "Không thể kiểm tra",
                        state.message
                    ) { finish() }
                }
            }
        }
    }

    private fun showPrecheckLoading(show: Boolean) {
        // Tận dụng dialog hoặc thay đổi trạng thái UI của màn hình trong lúc kiểm tra Firestore
        if (show) {
            if (loadingDialog == null) {
                loadingDialog = MessageUtils.showLoadingDialog(
                    context = this,
                    title = "Đang kiểm tra hồ sơ",
                    message = "Vui lòng chờ trong giây lát..."
                )
            }
        } else {
            loadingDialog?.dismiss()
            loadingDialog = null
        }
    }

    // Hiển thị card thông báo hồ sơ đã quá 24h và nút hủy để nộp lại
    private fun showExpiredCard() {
        binding.cardExpiredVerification.visibility = android.view.View.VISIBLE

        // Quan sát isLoading để hiện/ẩn dialog và disable nút tránh bấm đúp
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                loadingDialog = MessageUtils.showLoadingDialog(
                    context = this,
                    title = "Đang hủy hồ sơ",
                    message = "Vui lòng chờ trong giây lát..."
                )
            } else {
                loadingDialog?.dismiss()
                loadingDialog = null
            }
            binding.btnCancelAndResubmit.isEnabled = !isLoading
        }

        // Quan sát kết quả hủy hồ sơ
        viewModel.cancelResult.observe(this) { result ->
            if (result == null) return@observe
            viewModel.clearCancelResult()
            if (result) {
                // Hủy thành công -> ẩn card expired, tải lại form để nộp mới
                binding.cardExpiredVerification.visibility = android.view.View.GONE
                MessageUtils.showInfoDialog(
                    this,
                    "Đã hủy hồ sơ cũ",
                    "Hồ sơ cũ đã được xóa. Bạn có thể điền và gửi lại ngay bây giờ."
                ) {
                    // Tải lại trạng thái -> sẽ emit FormReady -> gọi setupForm()
                    viewModel.checkCurrentStatus(this)
                }
            } else {
                MessageUtils.showErrorDialog(
                    this,
                    "Không thể hủy",
                    "Có lỗi xảy ra khi hủy hồ sơ. Vui lòng thử lại sau."
                )
            }
        }

        binding.btnCancelAndResubmit.setOnClickListener {
            MessageUtils.showConfirmDialog(
                context = this,
                title = "Xác nhận hủy hồ sơ",
                message = "Hồ sơ cũ sẽ bị xóa và bạn cần chụp lại ảnh CCCD để nộp mới. Bạn có chắc chắn muốn hủy không?",
                positiveText = "Hủy hồ sơ",
                negativeText = "Giữ lại",
                onConfirm = {
                    viewModel.cancelExpiredVerification(this)
                }
            )
        }
    }

    private fun setupForm() {
        lockReadonlyIdentityFields()

        viewModel.ownerInfo.observe(this) { user ->
            binding.apply {
                edtFullName.setText(user.fullName)
                edtPhone.setText(user.phone)
                edtEmail.setText(user.email)
                edtAddress.setText(user.address)
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                loadingDialog = MessageUtils.showLoadingDialog(
                    context = this,
                    title = "Đang gửi yêu cầu",
                    message = "Vui lòng chờ trong giây lát..."
                )
            } else {
                loadingDialog?.dismiss()
                loadingDialog = null
            }
            binding.btnSubmitVerify.isEnabled = !isLoading
        }

        viewModel.submitResult.observe(this) { result ->
            if (result == null) return@observe
            cleanupTempFiles()
            if (result.status == VerifyLandlordViewModel.SubmitStatus.ESCALATED_TO_ADMIN) {
                MessageUtils.showInfoDialog(
                    this,
                    "Đang chờ duyệt",
                    result.message
                ) {
                    viewModel.clearSubmitResult()
                    finish()
                }
            } else if (result.status == VerifyLandlordViewModel.SubmitStatus.SUCCESS_AUTO_VERIFIED) {
                MessageUtils.showSuccessDialog(
                    this,
                    "Thông tin chính xác",
                    result.message
                ) {
                    viewModel.clearSubmitResult()
                    val intent = Intent(this, MainActivity::class.java).apply {
                        putExtra("openTab", "post")
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }

        viewModel.errorMessage.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                MessageUtils.showErrorDialog(this, "Lỗi gửi yêu cầu", error)
                viewModel.resetErrorMessage()
            }
        }

        viewModel.loadUserInfo()

        // Dialog xác nhận khi bấm Back hệ thống lúc đang điền form
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (hasAnyInputFilled()) {
                    showExitConfirmDialog()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        binding.apply {
            frameFront.setOnClickListener { openCamera(CaptureTarget.FRONT) }
            frameBack.setOnClickListener { openCamera(CaptureTarget.BACK) }
            btnSubmitVerify.setOnClickListener { submitVerification() }
            btnBack.setOnClickListener {
                if (hasAnyInputFilled()) showExitConfirmDialog() else finish()
            }

            edtAddress.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                    hideKeyboard()
                    true
                } else {
                    false
                }
            }
        }
    }

    // Có dữ liệu nếu user đã nhập CCCD hoặc đã chụp ít nhất 1 ảnh
    private fun hasAnyInputFilled(): Boolean =
        binding.edtCccdNumber.text?.isNotEmpty() == true ||
        frontUri != null ||
        backUri != null

    private fun showExitConfirmDialog() {
        MessageUtils.showConfirmDialog(
            context = this,
            title = "Bỏ qua xác minh?",
            message = "Thông tin bạn đã nhập sẽ bị mất. Bạn có chắc muốn thoát không?",
            positiveText = "Thoát",
            negativeText = "Tiếp tục điền",
            onConfirm = { finish() }
        )
    }

    private fun lockReadonlyIdentityFields() {
        binding.apply {
            edtFullName.isEnabled = false
            edtPhone.isEnabled = false
            edtEmail.isEnabled = false
            edtFullName.isFocusable = false
            edtPhone.isFocusable = false
            edtEmail.isFocusable = false
        }
    }

    private fun openCamera(target: CaptureTarget) {
        captureTarget = target
        launchCameraCapture(target)
    }

    private fun launchCameraCapture(target: CaptureTarget) {
        captureTarget = target
        val side = if (target == CaptureTarget.FRONT) {
            CccdCameraActivity.SIDE_FRONT
        } else {
            CccdCameraActivity.SIDE_BACK
        }
        val intent = Intent(this, CccdCameraActivity::class.java)
            .putExtra(CccdCameraActivity.EXTRA_TARGET_SIDE, side)
        cccdCameraLauncher.launch(intent)
    }

    // Xác mịnh danh tính -2-
    // Hàm xử lý sự kiện khi người dùng bấm nút "Gửi yêu cầu xác minh".
    // Các bước thực hiện:
    // 1. Lấy dữ liệu chữ (Tên, SĐT, CCCD, Địa chỉ) và ảnh (Mặt trước, Mặt sau).
    // 2. Validate (Kiểm tra) mã CCCD phải đúng chuẩn 12 số của Việt Nam.
    // 3. Bắt lỗi không để trống thông tin và bắt buộc tích chọn đồng ý nội quy.
    // 4. Nếu pass (hợp lệ) toàn bộ, đẩy dữ liệu qua ViewModel để xử lý gửi đi.
    private fun submitVerification() {
        binding.apply {
            val cccd = edtCccdNumber.text.toString().trim()
            val address = edtAddress.text.toString().trim()
            val fullName = edtFullName.text.toString().trim()
            val phone = edtPhone.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val front = frontUri
            val back = backUri

            if (!isValidVietnameseCccd(cccd)) {
                MessageUtils.showInfoDialog(
                    this@VerifyLandlordActivity,
                    "Số CCCD không hợp lệ",
                    "Số CCCD phải gồm đúng 12 chữ số theo định dạng chuẩn của Bộ Công An Việt Nam."
                )
                return
            }
            if (address.isEmpty() || front == null || back == null) {
                MessageUtils.showInfoDialog(
                    this@VerifyLandlordActivity,
                    "Thông tin chưa đủ",
                    "Vui lòng nhập địa chỉ và chụp đủ 2 mặt CCCD."
                )
                return
            }
            if (!cbCommit.isChecked) {
                MessageUtils.showInfoDialog(
                    this@VerifyLandlordActivity,
                    "Chưa đồng ý quy định",
                    "Vui lòng đọc và tích vào ô đồng ý với các quy định đăng tin."
                )
                return
            }

            viewModel.submitVerification(
                context = this@VerifyLandlordActivity,
                fullName = fullName,
                email = email,
                cccd = cccd,
                phone = phone,
                address = address,
                frontUri = front,
                backUri = back
            )
        }
    }

    private fun isValidVietnameseCccd(cccd: String): Boolean {
        if (cccd.length != 12 || !cccd.all { it.isDigit() }) return false
        val provinceCode = cccd.substring(0, 3).toIntOrNull() ?: return false
        if (provinceCode < 1 || provinceCode > 96) return false
        val genderCentury = cccd[3] - '0'
        if (genderCentury < 0 || genderCentury > 9) return false
        return true
    }

    private fun cleanupTempFiles() {
        try {
            frontUri?.path?.let { File(it).takeIf { f -> f.exists() }?.delete() }
            backUri?.path?.let { File(it).takeIf { f -> f.exists() }?.delete() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun clearAllCameraTempFolder() {
        try {
            val dir = File(filesDir, "verification_camera")
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    if (System.currentTimeMillis() - file.lastModified() > 2 * 60 * 60 * 1000) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
