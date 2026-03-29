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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.doantotnghiep.R
import com.google.android.material.button.MaterialButton
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
    private lateinit var btnSubmitVerify: MaterialButton
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

        // Load thông tin user
        loadUserInfo()

        // Chọn ảnh mặt trước
        frameFront.setOnClickListener {
            isPickingFront = true
            pickImage()
        }

        // Chọn ảnh mặt sau
        frameBack.setOnClickListener {
            isPickingFront = false
            pickImage()
        }

        // Gửi xác minh
        btnSubmitVerify.setOnClickListener {
            submitVerification()
        }

        btnBack.setOnClickListener { finish() }
    }

    private fun loadUserInfo() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    edtFullName.setText(doc.getString("fullName") ?: "")
                    edtPhone.setText(doc.getString("phone") ?: "")
                }
            }
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private fun submitVerification() {
        val cccdNumber = edtCccdNumber.text.toString().trim()
        val address = edtAddress.text.toString().trim()
        val fullName = edtFullName.text.toString().trim()
        val phone = edtPhone.text.toString().trim()

        // Validate
        if (cccdNumber.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số CCCD", Toast.LENGTH_SHORT).show()
            return
        }
        if (cccdNumber.length != 12) {
            Toast.makeText(this, "Số CCCD phải có 12 số", Toast.LENGTH_SHORT).show()
            return
        }
        if (address.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập địa chỉ thường trú", Toast.LENGTH_SHORT).show()
            return
        }
        if (frontUri == null) {
            Toast.makeText(this, "Vui lòng chọn ảnh mặt trước CCCD", Toast.LENGTH_SHORT).show()
            return
        }
        if (backUri == null) {
            Toast.makeText(this, "Vui lòng chọn ảnh mặt sau CCCD", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        btnSubmitVerify.isEnabled = false

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val storageRef = FirebaseStorage.getInstance().reference

        // Upload ảnh mặt trước
        val frontRef = storageRef.child("verifications/$uid/cccd_front.jpg")
        frontRef.putFile(frontUri!!)
            .addOnSuccessListener {
                frontRef.downloadUrl.addOnSuccessListener { frontUrl ->

                    // Upload ảnh mặt sau
                    val backRef = storageRef.child("verifications/$uid/cccd_back.jpg")
                    backRef.putFile(backUri!!)
                        .addOnSuccessListener {
                            backRef.downloadUrl.addOnSuccessListener { backUrl ->

                                // Lưu thông tin xác minh vào Firestore
                                val verification = hashMapOf(
                                    "userId" to uid,
                                    "fullName" to fullName,
                                    "cccdNumber" to cccdNumber,
                                    "phone" to phone,
                                    "address" to address,
                                    "cccdFrontUrl" to frontUrl.toString(),
                                    "cccdBackUrl" to backUrl.toString(),
                                    "status" to "pending",
                                    "createdAt" to System.currentTimeMillis()
                                )

                                FirebaseFirestore.getInstance()
                                    .collection("verifications")
                                    .document(uid)
                                    .set(verification)
                                    .addOnSuccessListener {
                                        progressBar.visibility = View.GONE
                                        btnSubmitVerify.isEnabled = true

                                        androidx.appcompat.app.AlertDialog.Builder(this)
                                            .setTitle("Gửi thành công!")
                                            .setMessage("Yêu cầu xác minh đã được gửi. Vui lòng chờ admin phê duyệt.")
                                            .setPositiveButton("OK") { _, _ ->
                                                finish()
                                            }
                                            .setCancelable(false)
                                            .show()
                                    }
                                    .addOnFailureListener { e ->
                                        progressBar.visibility = View.GONE
                                        btnSubmitVerify.isEnabled = true
                                        Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            progressBar.visibility = View.GONE
                            btnSubmitVerify.isEnabled = true
                            Toast.makeText(this, "Upload ảnh mặt sau thất bại: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                btnSubmitVerify.isEnabled = true
                Toast.makeText(this, "Upload ảnh mặt trước thất bại: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}