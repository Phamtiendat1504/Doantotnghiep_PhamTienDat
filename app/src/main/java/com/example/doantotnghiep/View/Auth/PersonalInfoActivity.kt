package com.example.doantotnghiep.View.Auth

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.ViewModel.PersonalInfoViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
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

    private val viewModel: PersonalInfoViewModel by viewModels()

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

        observeViewModel()
        viewModel.loadUserInfo()

        tvBirthday.setOnClickListener { if (isEditing) showDatePicker() }
        btnPickDate.setOnClickListener { if (isEditing) showDatePicker() }

        btnEdit.setOnClickListener {
            isEditing = true
            enableEditing(true)
        }

        btnCancel.setOnClickListener {
            isEditing = false
            restoreOriginalValues()
            clearErrors()
            enableEditing(false)
        }

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

            if (fullName.isEmpty()) { tilFullName.error = "Vui lòng nhập họ và tên"; return@setOnClickListener }
            if (email.isEmpty()) { tilEmail.error = "Vui lòng nhập email"; return@setOnClickListener }
            if (!email.endsWith("@gmail.com") || email.length <= 10) {
                tilEmail.error = "Email phải có đuôi @gmail.com"; return@setOnClickListener
            }
            if (phone.isEmpty() || phone.length != 10 || !phone.startsWith("0")) {
                tilPhone.error = "Số điện thoại phải có 10 số và bắt đầu bằng 0"; return@setOnClickListener
            }

            if (email != originalEmail) {
                showPasswordDialogToUpdateEmail(email, fullName, phone, address, birthday, gender, occupation)
            } else {
                viewModel.updateUserInfo(fullName, email, phone, address, birthday, gender, occupation)
            }
        }

        btnBack.setOnClickListener { finish() }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            btnSave.isEnabled = !loading
            btnCancel.isEnabled = !loading
        }

        viewModel.userInfo.observe(this) { data ->
            originalFullName = data["fullName"] as? String ?: ""
            originalEmail = data["email"] as? String ?: ""
            originalPhone = data["phone"] as? String ?: ""
            originalAddress = data["address"] as? String ?: ""
            originalBirthday = data["birthday"] as? String ?: ""
            originalGender = data["gender"] as? String ?: ""
            originalOccupation = data["occupation"] as? String ?: ""

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

        viewModel.updateResult.observe(this) { success ->
            if (success) {
                val fullName = edtFullName.text.toString().trim()
                val phone = edtPhone.text.toString().trim()
                val address = edtAddress.text.toString().trim()
                val occupation = edtOccupation.text.toString().trim()
                val birthday = if (tvBirthday.text.toString() == "Chưa cập nhật") "" else tvBirthday.text.toString()
                val gender = when (rgGender.checkedRadioButtonId) {
                    R.id.rbMale -> "Nam"
                    R.id.rbFemale -> "Nữ"
                    else -> ""
                }
                originalFullName = fullName
                originalPhone = phone
                originalAddress = address
                originalBirthday = birthday
                originalGender = gender
                originalOccupation = occupation
                isEditing = false
                enableEditing(false)
                MessageUtils.showSuccessDialog(this, "Cập nhật thành công", "Thông tin cá nhân của bạn đã được cập nhật trên hệ thống.")
            }
        }

        viewModel.emailUpdateResult.observe(this) { success ->
            if (success) {
                val newEmail = edtEmail.text.toString().trim()
                viewModel.updateUserInfo(
                    edtFullName.text.toString().trim(), newEmail,
                    edtPhone.text.toString().trim(), edtAddress.text.toString().trim(),
                    if (tvBirthday.text.toString() == "Chưa cập nhật") "" else tvBirthday.text.toString(),
                    when (rgGender.checkedRadioButtonId) { R.id.rbMale -> "Nam"; R.id.rbFemale -> "Nữ"; else -> "" },
                    edtOccupation.text.toString().trim()
                )
                MessageUtils.showSuccessDialog(
                    this, "Xác nhận email mới",
                    "Một email xác nhận đã được gửi đến $newEmail. Vui lòng kiểm tra hộp thư và xác nhận để hoàn tất đổi email."
                )
            }
        }

        viewModel.wrongPassword.observe(this) { wrong ->
            if (wrong) MessageUtils.showErrorDialog(this, "Xác thực thất bại", "Mật khẩu bạn nhập không chính xác.")
        }

        viewModel.errorMessage.observe(this) { msg ->
            if (!msg.isNullOrEmpty()) MessageUtils.showErrorDialog(this, "Lỗi", msg)
        }
    }

    private fun showPasswordDialogToUpdateEmail(
        newEmail: String, fullName: String, phone: String,
        address: String, birthday: String, gender: String, occupation: String
    ) {
        val input = EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        input.hint = "Nhập mật khẩu hiện tại"
        input.setPadding(50, 30, 50, 30)

        AlertDialog.Builder(this)
            .setTitle("Xác nhận đổi email")
            .setMessage("Để thay đổi email, vui lòng nhập mật khẩu hiện tại của bạn.")
            .setView(input)
            .setPositiveButton("Xác nhận") { _, _ ->
                val password = input.text.toString().trim()
                if (password.isEmpty()) {
                    MessageUtils.showInfoDialog(this, "Thông tin thiếu", "Vui lòng nhập mật khẩu để xác nhận.")
                    return@setPositiveButton
                }
                viewModel.reauthenticateAndUpdateEmail(originalEmail, password, newEmail)
            }
            .setNegativeButton("Hủy", null)
            .setCancelable(false)
            .show()
    }

    private fun restoreOriginalValues() {
        edtFullName.setText(originalFullName)
        edtEmail.setText(originalEmail)
        edtPhone.setText(originalPhone)
        edtAddress.setText(originalAddress)
        edtOccupation.setText(originalOccupation)
        tvBirthday.text = if (originalBirthday.isNotEmpty()) originalBirthday else "Chưa cập nhật"
        tvBirthday.setTextColor(if (originalBirthday.isNotEmpty()) 0xFF333333.toInt() else 0xFF999999.toInt())
        setGenderRadio(originalGender)
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

    private fun clearErrors() {
        tilFullName.error = null
        tilEmail.error = null
        tilPhone.error = null
        tilAddress.error = null
        tilOccupation.error = null
    }
}