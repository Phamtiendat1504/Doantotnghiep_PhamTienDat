package com.example.doantotnghiep.View.Auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.ViewModel.VerifyLandlordViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

class VerifyLandlordActivity : AppCompatActivity() {

    private enum class CaptureTarget { FRONT, BACK }

    private lateinit var edtFullName: EditText
    private lateinit var edtCccdNumber: EditText
    private lateinit var edtPhone: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtAddress: EditText
    private lateinit var frameFront: FrameLayout
    private lateinit var frameBack: FrameLayout
    private lateinit var imgFront: ImageView
    private lateinit var imgBack: ImageView
    private lateinit var layoutFrontPlaceholder: LinearLayout
    private lateinit var layoutBackPlaceholder: LinearLayout
    private lateinit var btnSubmitVerify: com.google.android.material.button.MaterialButton
    private lateinit var cbCommit: android.widget.CheckBox
    private lateinit var btnBack: ImageView
    private var loadingDialog: AlertDialog? = null

    private lateinit var viewModel: VerifyLandlordViewModel

    private var frontUri: Uri? = null
    private var backUri: Uri? = null
    private var captureTarget: CaptureTarget = CaptureTarget.FRONT

    private val cccdCameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val uriString = result.data?.getStringExtra(CccdCameraActivity.EXTRA_OUTPUT_URI)
        val uri = uriString?.let(Uri::parse) ?: return@registerForActivityResult

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_landlord)

        initViews()
        viewModel = ViewModelProvider(this)[VerifyLandlordViewModel::class.java]

        checkCurrentStatusBeforeShow()
    }

    private fun checkCurrentStatusBeforeShow() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            finish()
            return
        }
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(uid)
            .get(Source.SERVER)
            .addOnSuccessListener { userDoc ->
                val isVerified = userDoc.getBoolean("isVerified") ?: false
                val postingUnlockAt = userDoc.getLong("postingUnlockAt") ?: 0L
                val now = System.currentTimeMillis()
                if (isVerified) {
                    if (postingUnlockAt > now) {
                        val totalMinutes = ((postingUnlockAt - now).coerceAtLeast(0L)) / 60_000L
                        val hours = totalMinutes / 60L
                        val minutes = totalMinutes % 60L
                        val formatter = java.text.SimpleDateFormat("HH:mm dd/MM/yyyy", java.util.Locale("vi", "VN"))
                        val unlockTime = formatter.format(java.util.Date(postingUnlockAt))
                        MessageUtils.showInfoDialog(
                            this,
                            "Đã được cấp quyền",
                            "Quyền đăng bài của bạn đã được cấp, nhưng cần chờ thêm 24 giờ " +
                                "kể từ lúc admin duyệt.\n\n" +
                                "Còn lại: ${hours} giờ ${minutes} phút\n" +
                                "Thời điểm mở đăng bài: $unlockTime"
                        ) { finish() }
                    } else {
                        MessageUtils.showSuccessDialog(
                            this,
                            "Đã xác minh",
                            "Tài khoản của bạn đã được xác minh. Bạn có thể đăng tin cho thuê ngay!"
                        ) { finish() }
                    }
                    return@addOnSuccessListener
                }

                db.collection("verifications").document(uid)
                    .get(Source.SERVER)
                    .addOnSuccessListener { verifyDoc ->
                        val status = if (verifyDoc.exists()) verifyDoc.getString("status") else null
                        val waitingStatuses = setOf("pending", "pending_admin_review", "queued_manual")
                        if (status in waitingStatuses) {
                            MessageUtils.showInfoDialog(
                                this,
                                "Đang xử lý hồ sơ",
                                "Hồ sơ của bạn đã được gửi và đang được hệ thống xử lý. Nếu cần can thiệp thủ công, admin sẽ phản hồi trong 24 giờ."
                            ) { finish() }
                        } else {
                            setupForm()
                        }
                    }
                    .addOnFailureListener {
                        MessageUtils.showInfoDialog(
                            this,
                            "Không thể kiểm tra hồ sơ",
                            "Không thể tải trạng thái hồ sơ xác minh. Vui lòng thử lại khi kết nối ổn định."
                        ) { finish() }
                    }
            }
            .addOnFailureListener {
                MessageUtils.showInfoDialog(
                    this,
                    "Không thể kiểm tra trạng thái",
                    "Không thể tải trạng thái xác minh từ máy chủ. Vui lòng kiểm tra mạng và thử lại."
                ) { finish() }
            }
    }

    private fun setupForm() {
        lockReadonlyIdentityFields()

        viewModel.ownerInfo.observe(this) { user ->
            edtFullName.setText(user.fullName)
            edtPhone.setText(user.phone)
            edtEmail.setText(user.email)
            edtAddress.setText(user.address)
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
            btnSubmitVerify.isEnabled = !isLoading
        }

        viewModel.submitResult.observe(this) { result ->
            if (result == null) return@observe
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
                    val intent = android.content.Intent(this, com.example.doantotnghiep.MainActivity::class.java)
                    intent.putExtra("navigate_to", "post")
                    intent.flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    finish()
                }
            }
        }

        viewModel.errorMessage.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                MessageUtils.showErrorDialog(this, "Lỗi gửi yêu cầu", error)
            }
        }

        viewModel.loadUserInfo()

        frameFront.setOnClickListener { openCamera(CaptureTarget.FRONT) }
        frameBack.setOnClickListener { openCamera(CaptureTarget.BACK) }
        btnSubmitVerify.setOnClickListener { submitVerification() }
        btnBack.setOnClickListener { finish() }
    }

    private fun lockReadonlyIdentityFields() {
        edtFullName.isEnabled = false
        edtPhone.isEnabled = false
        edtEmail.isEnabled = false
        edtFullName.isFocusable = false
        edtPhone.isFocusable = false
        edtEmail.isFocusable = false
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

    private fun initViews() {
        edtFullName = findViewById(R.id.edtFullName)
        edtCccdNumber = findViewById(R.id.edtCccdNumber)
        edtPhone = findViewById(R.id.edtPhone)
        edtEmail = findViewById(R.id.edtEmail)
        edtAddress = findViewById(R.id.edtAddress)
        frameFront = findViewById(R.id.frameFront)
        frameBack = findViewById(R.id.frameBack)
        imgFront = findViewById(R.id.imgFront)
        imgBack = findViewById(R.id.imgBack)
        layoutFrontPlaceholder = findViewById(R.id.layoutFrontPlaceholder)
        layoutBackPlaceholder = findViewById(R.id.layoutBackPlaceholder)
        btnSubmitVerify = findViewById(R.id.btnSubmitVerify)
        cbCommit = findViewById(R.id.cbCommit)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun submitVerification() {
        val cccd = edtCccdNumber.text.toString().trim()
        val address = edtAddress.text.toString().trim()
        val fullName = edtFullName.text.toString().trim()
        val phone = edtPhone.text.toString().trim()
        val email = edtEmail.text.toString().trim()
        val front = frontUri
        val back = backUri

        if (cccd.length != 12) {
            MessageUtils.showInfoDialog(this, "Số CCCD không hợp lệ", "Vui lòng nhập đúng 12 số CCCD.")
            return
        }
        if (address.isEmpty() || front == null || back == null) {
            MessageUtils.showInfoDialog(
                this,
                "Thông tin chưa đủ",
                "Vui lòng nhập địa chỉ và chụp đủ 2 mặt CCCD."
            )
            return
        }
        if (!cbCommit.isChecked) {
            MessageUtils.showInfoDialog(
                this,
                "Chưa đồng ý quy định",
                "Vui lòng đọc và tích vào ô đồng ý với các quy định đăng tin."
            )
            return
        }

        viewModel.submitVerification(
            context = this,
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
