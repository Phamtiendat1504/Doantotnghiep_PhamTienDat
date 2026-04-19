package com.example.doantotnghiep.View.Auth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.ViewModel.VerifyLandlordViewModel

class VerifyLandlordActivity : AppCompatActivity() {

    private lateinit var edtFullName: EditText
    private lateinit var edtCccdNumber: EditText
    private lateinit var edtPhone: EditText
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
    private lateinit var progressBar: ProgressBar

    private lateinit var viewModel: VerifyLandlordViewModel

    private var frontUri: Uri? = null
    private var backUri: Uri? = null
    private var isPickingFront = true

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            if (isPickingFront) {
                frontUri = uri
                imgFront.setImageURI(uri)
                imgFront.visibility = View.VISIBLE
                layoutFrontPlaceholder.visibility = View.GONE
            } else {
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

        // Kiểm tra trạng thái hiện tại ngay khi mở màn hình
        // (lấy thẳng từ Server để tránh đọc cache cũ)
        checkCurrentStatusBeforeShow()
    }

    private fun checkCurrentStatusBeforeShow() {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) { finish(); return }
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        // Bước 1: Kiểm tra user document (isVerified)
        db.collection("users").document(uid)
            .get(com.google.firebase.firestore.Source.SERVER)
            .addOnSuccessListener { userDoc ->
                val isVerified = userDoc.getBoolean("isVerified") ?: false
                if (isVerified) {
                    // Đã được xác minh rồi, không cho nộp lại
                    MessageUtils.showSuccessDialog(
                        this,
                        "Đã xác minh",
                        "Tài khoản của bạn đã được xác minh. Bạn có thể đăng tin cho thuê ngay!"
                    ) { finish() }
                    return@addOnSuccessListener
                }

                // Bước 2: Kiểm tra document verifications — nếu đang "pending" thì không cho nộp lại
                db.collection("verifications").document(uid)
                    .get(com.google.firebase.firestore.Source.SERVER)
                    .addOnSuccessListener { verifyDoc ->
                        val status = if (verifyDoc.exists()) verifyDoc.getString("status") else null
                        if (status == "pending") {
                            MessageUtils.showInfoDialog(
                                this,
                                "Đang chờ phê duyệt",
                                "Hồ sơ của bạn đã được gửi và đang chờ Admin phê duyệt. Vui lòng quay lại sau 24-48h làm việc."
                            ) { finish() }
                        } else {
                            // Chưa nộp hoặc đã bị từ chối → cho phép nộp (lại)
                            setupForm()
                        }
                    }
                    .addOnFailureListener { setupForm() } // Lỗi mạng → vẫn cho hiển form
            }
            .addOnFailureListener { setupForm() } // Lỗi mạng → vẫn cho hiển form
    }

    private fun setupForm() {
        viewModel.ownerInfo.observe(this) { (fullName, phone) ->
            edtFullName.setText(fullName)
            edtPhone.setText(phone)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnSubmitVerify.isEnabled = !isLoading
        }

        viewModel.submitResult.observe(this) { success ->
            if (success == true) {
                MessageUtils.showSuccessDialog(
                    this,
                    "Gửi yêu cầu thành công",
                    "Thông tin xác minh của bạn đã được gửi. Vui lòng chờ Admin phê duyệt để có quyền đăng tin."
                ) { finish() }
            }
        }

        viewModel.errorMessage.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                MessageUtils.showErrorDialog(this, "Lỗi gửi yêu cầu", error)
            }
        }

        viewModel.loadUserInfo()

        frameFront.setOnClickListener { isPickingFront = true; pickImage() }
        frameBack.setOnClickListener { isPickingFront = false; pickImage() }
        btnSubmitVerify.setOnClickListener { submitVerification() }
        btnBack.setOnClickListener { finish() }
    }

    private fun initViews() {
        edtFullName = findViewById(R.id.edtFullName)
        edtCccdNumber = findViewById(R.id.edtCccdNumber)
        edtPhone = findViewById(R.id.edtPhone)
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
        progressBar = findViewById(R.id.progressBar)
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        pickImageLauncher.launch(intent)
    }

    private fun submitVerification() {
        val cccd = edtCccdNumber.text.toString().trim()
        val address = edtAddress.text.toString().trim()
        val fullName = edtFullName.text.toString().trim()
        val phone = edtPhone.text.toString().trim()

        if (cccd.length != 12) {
            MessageUtils.showInfoDialog(this, "Số CCCD không hợp lệ", "Vui lòng nhập đúng 12 số CCCD của bạn.")
            return
        }
        if (address.isEmpty() || frontUri == null || backUri == null) {
            MessageUtils.showInfoDialog(this, "Thông tin chưa đủ", "Vui lòng nhập địa chỉ và tải đủ 2 mặt ảnh CCCD.")
            return
        }
        if (!cbCommit.isChecked) {
            MessageUtils.showInfoDialog(this, "Chưa đồng ý quy định", "Vui lòng đọc và tích vào ô đồng ý với các quy định đăng tin.")
            return
        }

        viewModel.submitVerification(fullName, cccd, phone, address, frontUri!!, backUri!!)
    }
}