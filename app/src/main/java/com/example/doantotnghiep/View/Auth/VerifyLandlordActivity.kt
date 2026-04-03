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
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.MessageUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

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
    private lateinit var btnBack: ImageView
    private lateinit var progressBar: ProgressBar

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
        loadUserInfo()

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
        btnBack = findViewById(R.id.btnBack)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun loadUserInfo() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    edtFullName.setText(doc.getString("fullName") ?: "")
                    edtPhone.setText(doc.getString("phone") ?: "")
                }
            }
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        pickImageLauncher.launch(intent)
    }

    private fun submitVerification() {
        val cccd = edtCccdNumber.text.toString().trim()
        val address = edtAddress.text.toString().trim()

        if (cccd.length != 12) {
            MessageUtils.showInfoDialog(this, "Số CCCD không hợp lệ", "Vui lòng nhập đúng 12 số CCCD của bạn.")
            return
        }
        
        if (address.isEmpty() || frontUri == null || backUri == null) {
            MessageUtils.showInfoDialog(this, "Thông tin chưa đủ", "Vui lòng nhập địa chỉ và tải đủ 2 mặt ảnh CCCD.")
            return
        }

        setLoading(true)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val storageRef = FirebaseStorage.getInstance().reference

        // Upload ảnh mặt trước
        val frontRef = storageRef.child("verifications/$uid/cccd_front_${System.currentTimeMillis()}.jpg")
        frontRef.putFile(frontUri!!).continueWithTask { task ->
            if (!task.isSuccessful) task.exception?.let { throw it }
            frontRef.downloadUrl
        }.continueWithTask { task ->
            val frontUrl = task.result.toString()
            // Upload ảnh mặt sau
            val backRef = storageRef.child("verifications/$uid/cccd_back_${System.currentTimeMillis()}.jpg")
            backRef.putFile(backUri!!).continueWithTask { backTask ->
                if (!backTask.isSuccessful) backTask.exception?.let { throw it }
                backRef.downloadUrl
            }.continueWithTask { backUrlTask ->
                val backUrl = backUrlTask.result.toString()
                // Lưu vào Firestore
                val data = hashMapOf(
                    "userId" to uid,
                    "fullName" to edtFullName.text.toString(),
                    "cccdNumber" to cccd,
                    "phone" to edtPhone.text.toString(),
                    "address" to address,
                    "cccdFrontUrl" to frontUrl,
                    "cccdBackUrl" to backUrl,
                    "status" to "pending",
                    "createdAt" to System.currentTimeMillis()
                )
                FirebaseFirestore.getInstance().collection("verifications").document(uid).set(data)
            }
        }.addOnSuccessListener {
            setLoading(false)
            MessageUtils.showSuccessDialog(
                this,
                "Gửi yêu cầu thành công",
                "Thông tin xác minh của bạn đã được gửi. Vui lòng chờ Admin phê duyệt để có quyền đăng tin."
            ) {
                finish()
            }
        }.addOnFailureListener { e ->
            setLoading(false)
            MessageUtils.showErrorDialog(this, "Lỗi gửi yêu cầu", e.message ?: "Đã có lỗi xảy ra, vui lòng thử lại sau.")
        }
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnSubmitVerify.isEnabled = !isLoading
    }

    private fun pickImageLauncher() {
        // Dummy function to keep pickImageLauncher as it is used in result activity
    }
}
