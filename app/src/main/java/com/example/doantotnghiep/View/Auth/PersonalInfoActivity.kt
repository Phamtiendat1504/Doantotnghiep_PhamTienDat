package com.example.doantotnghiep.View.Auth

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.doantotnghiep.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class PersonalInfoActivity : AppCompatActivity() {

    private lateinit var tilFullName: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPhone: TextInputLayout
    private lateinit var tilAddress: TextInputLayout
    private lateinit var tilOccupation: TextInputLayout
    private lateinit var edtFullName: TextInputEditText
    private lateinit var edtEmail: TextInputEditText
    private lateinit var edtPhone: TextInputEditText
    private lateinit var edtAddress: TextInputEditText
    private lateinit var edtOccupation: TextInputEditText
    private lateinit var tvBirthday: TextView
    private lateinit var btnPickDate: ImageView
    private lateinit var rgGender: RadioGroup
    private lateinit var rbMale: RadioButton
    private lateinit var rbFemale: RadioButton
    private lateinit var btnEdit: MaterialButton
    private lateinit var btnSave: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: ImageView

    private var originalEmail = ""
    private var originalFullName = ""
    private var originalPhone = ""
    private var originalAddress = ""
    private var originalBirthday = ""
    private var originalGender = ""
    private var originalOccupation = ""

    private var isEditing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_info)

        tilFullName = findViewById(R.id.tilFullName)
        tilEmail = findViewById(R.id.tilEmail)
        tilPhone = findViewById(R.id.tilPhone)
        tilAddress = findViewById(R.id.tilAddress)
        tilOccupation = findViewById(R.id.tilOccupation)
        edtFullName = findViewById(R.id.edtFullName)
        edtEmail = findViewById(R.id.edtEmail)
        edtPhone = findViewById(R.id.edtPhone)
        edtAddress = findViewById(R.id.edtAddress)
        edtOccupation = findViewById(R.id.edtOccupation)
        tvBirthday = findViewById(R.id.tvBirthday)
        btnPickDate = findViewById(R.id.btnPickDate)
        rgGender = findViewById(R.id.rgGender)
        rbMale = findViewById(R.id.rbMale)
        rbFemale = findViewById(R.id.rbFemale)
        btnEdit = findViewById(R.id.btnEdit)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
        progressBar = findViewById(R.id.progressBar)
        btnBack = findViewById(R.id.btnBack)

        loadUserInfo()

        // Bấm chọn ngày sinh
        tvBirthday.setOnClickListener {
            if (isEditing) showDatePicker()
        }
        btnPickDate.setOnClickListener {
            if (isEditing) showDatePicker()
        }

        // Bấm Thay đổi thông tin
        btnEdit.setOnClickListener {
            isEditing = true
            enableEditing(true)
        }

        // Bấm Hủy
        btnCancel.setOnClickListener {
            isEditing = false
            edtFullName.setText(originalFullName)
            edtEmail.setText(originalEmail)
            edtPhone.setText(originalPhone)
            edtAddress.setText(originalAddress)
            edtOccupation.setText(originalOccupation)
            tvBirthday.text = if (originalBirthday.isNotEmpty()) originalBirthday else "Chưa cập nhật"
            tvBirthday.setTextColor(if (originalBirthday.isNotEmpty()) 0xFF333333.toInt() else 0xFF999999.toInt())
            setGenderRadio(originalGender)
            clearErrors()
            enableEditing(false)
        }

        // Bấm Lưu thông tin
        btnSave.setOnClickListener {
            clearErrors()

            val fullName = edtFullName.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val phone = edtPhone.text.toString().trim()
            val address = edtAddress.text.toString().trim()
            val occupation = edtOccupation.text.toString().trim()
            val birthday = if (tvBirthday.text.toString() == "Chưa cập nhật") "" else tvBirthday.text.toString()
            val gender = when (rgGender.checkedRadioButtonId) {
                R.id.rbMale -> "Nam"
                R.id.rbFemale -> "Nữ"
                else -> ""
            }

            // Validate
            if (fullName.isEmpty()) {
                tilFullName.error = "Vui lòng nhập họ và tên"
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                tilEmail.error = "Vui lòng nhập email"
                return@setOnClickListener
            }
            if (!email.endsWith("@gmail.com") || email.length <= 10) {
                tilEmail.error = "Email phải có đuôi @gmail.com"
                return@setOnClickListener
            }
            if (phone.isEmpty() || phone.length != 10 || !phone.startsWith("0")) {
                tilPhone.error = "Số điện thoại phải có 10 số và bắt đầu bằng 0"
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnSave.isEnabled = false
            btnCancel.isEnabled = false

            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener

            updateFirestore(uid, fullName, email, phone, address, birthday, gender, occupation)
        }

        btnBack.setOnClickListener { finish() }
    }

    private fun enableEditing(enabled: Boolean) {
        edtFullName.isEnabled = enabled
        edtEmail.isEnabled = false
        edtPhone.isEnabled = enabled
        edtAddress.isEnabled = enabled
        edtOccupation.isEnabled = enabled
        rbMale.isEnabled = enabled
        rbFemale.isEnabled = enabled
        btnPickDate.visibility = if (enabled) View.VISIBLE else View.GONE

        if (enabled) {
            btnEdit.visibility = View.GONE
            btnSave.visibility = View.VISIBLE
            btnCancel.visibility = View.VISIBLE
            edtFullName.requestFocus()
        } else {
            btnEdit.visibility = View.VISIBLE
            btnSave.visibility = View.GONE
            btnCancel.visibility = View.GONE
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        // Nếu đã có ngày sinh, set về ngày đó
        if (originalBirthday.isNotEmpty()) {
            val parts = originalBirthday.split("/")
            if (parts.size == 3) {
                calendar.set(Calendar.DAY_OF_MONTH, parts[0].toIntOrNull() ?: 1)
                calendar.set(Calendar.MONTH, (parts[1].toIntOrNull() ?: 1) - 1)
                calendar.set(Calendar.YEAR, parts[2].toIntOrNull() ?: 2000)
            }
        } else {
            calendar.set(2000, 0, 1)
        }

        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                tvBirthday.text = selectedDate
                tvBirthday.setTextColor(0xFF333333.toInt())
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Giới hạn: không quá ngày hiện tại, không trước 1950
        datePicker.datePicker.maxDate = System.currentTimeMillis()
        val minCal = Calendar.getInstance()
        minCal.set(1950, 0, 1)
        datePicker.datePicker.minDate = minCal.timeInMillis

        datePicker.show()
    }

    private fun setGenderRadio(gender: String) {
        when (gender) {
            "Nam" -> rbMale.isChecked = true
            "Nữ" -> rbFemale.isChecked = true
            else -> rgGender.clearCheck()
        }
    }

    private fun showPasswordDialogToUpdateEmail(
        newEmail: String, fullName: String, phone: String, address: String,
        birthday: String, gender: String, occupation: String, uid: String
    ) {
        val input = android.widget.EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        input.hint = "Nhập mật khẩu hiện tại"
        input.setPadding(50, 30, 50, 30)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Xác nhận đổi email")
            .setMessage("Để thay đổi email, vui lòng nhập mật khẩu hiện tại của bạn.")
            .setView(input)
            .setPositiveButton("Xác nhận") { _, _ ->
                val password = input.text.toString().trim()
                if (password.isEmpty()) {
                    progressBar.visibility = View.GONE
                    btnSave.isEnabled = true
                    btnCancel.isEnabled = true
                    Toast.makeText(this, "Vui lòng nhập mật khẩu", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val user = FirebaseAuth.getInstance().currentUser ?: return@setPositiveButton
                val credential = EmailAuthProvider.getCredential(originalEmail, password)

                user.reauthenticate(credential)
                    .addOnSuccessListener {
                        user.verifyBeforeUpdateEmail(newEmail)
                            .addOnSuccessListener {
                                updateFirestore(uid, fullName, newEmail, phone, address, birthday, gender, occupation)

                                androidx.appcompat.app.AlertDialog.Builder(this)
                                    .setTitle("Xác nhận email mới")
                                    .setMessage("Một email xác nhận đã được gửi đến $newEmail. Vui lòng kiểm tra hộp thư và xác nhận để hoàn tất đổi email.")
                                    .setPositiveButton("OK", null)
                                    .show()
                            }
                            .addOnFailureListener { e ->
                                progressBar.visibility = View.GONE
                                btnSave.isEnabled = true
                                btnCancel.isEnabled = true
                                Toast.makeText(this, "Đổi email thất bại: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                    .addOnFailureListener {
                        progressBar.visibility = View.GONE
                        btnSave.isEnabled = true
                        btnCancel.isEnabled = true
                        Toast.makeText(this, "Mật khẩu không đúng", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Hủy") { _, _ ->
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true
                btnCancel.isEnabled = true
            }
            .setCancelable(false)
            .show()
    }

    private fun updateFirestore(
        uid: String, fullName: String, email: String, phone: String,
        address: String, birthday: String, gender: String, occupation: String
    ) {
        val updates = mapOf(
            "fullName" to fullName,
            "email" to email,
            "phone" to phone,
            "address" to address,
            "birthday" to birthday,
            "gender" to gender,
            "occupation" to occupation
        )

        FirebaseFirestore.getInstance().collection("users").document(uid)
            .update(updates)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true
                btnCancel.isEnabled = true
                isEditing = false

                originalFullName = fullName
                originalEmail = email
                originalPhone = phone
                originalAddress = address
                originalBirthday = birthday
                originalGender = gender
                originalOccupation = occupation

                enableEditing(false)

                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Thành công")
                    .setMessage("Thông tin cá nhân đã được cập nhật thành công!")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setCancelable(false)
                    .show()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true
                btnCancel.isEnabled = true
                Toast.makeText(this, "Cập nhật thất bại: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun loadUserInfo() {
        progressBar.visibility = View.VISIBLE

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                progressBar.visibility = View.GONE
                if (document.exists()) {
                    originalFullName = document.getString("fullName") ?: ""
                    originalEmail = document.getString("email") ?: ""
                    originalPhone = document.getString("phone") ?: ""
                    originalAddress = document.getString("address") ?: ""
                    originalBirthday = document.getString("birthday") ?: ""
                    originalGender = document.getString("gender") ?: ""
                    originalOccupation = document.getString("occupation") ?: ""

                    edtFullName.setText(originalFullName)
                    edtEmail.setText(originalEmail)
                    edtPhone.setText(originalPhone)
                    edtAddress.setText(originalAddress)
                    edtOccupation.setText(originalOccupation)

                    if (originalBirthday.isNotEmpty()) {
                        tvBirthday.text = originalBirthday
                        tvBirthday.setTextColor(0xFF333333.toInt())
                    } else {
                        tvBirthday.text = "Chưa cập nhật"
                        tvBirthday.setTextColor(0xFF999999.toInt())
                    }

                    setGenderRadio(originalGender)
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Lỗi tải thông tin", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearErrors() {
        tilFullName.error = null
        tilEmail.error = null
        tilPhone.error = null
        tilAddress.error = null
        tilOccupation.error = null
    }
}