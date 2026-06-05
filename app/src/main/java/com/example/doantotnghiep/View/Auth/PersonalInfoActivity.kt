package com.example.doantotnghiep.View.Auth

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.doantotnghiep.Model.User
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.ViewModel.PersonalInfoViewModel
import com.example.doantotnghiep.databinding.ActivityPersonalInfoBinding
import java.util.Calendar

class PersonalInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPersonalInfoBinding
    private val viewModel: PersonalInfoViewModel by viewModels()

    private var originalUser: User? = null
    private var isEditing = false

    companion object {
        private const val BIRTHDAY_PLACEHOLDER = "Chưa cập nhật"
        // Regex kiểm tra số điện thoại chuẩn viễn thông Việt Nam (10 số, bắt đầu bằng 03, 05, 07, 08, 09)
        private val VIETNAM_PHONE_REGEX = "^(03|05|07|08|09)\\d{8}$".toRegex()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPersonalInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeViewModel()

        viewModel.loadUserInfo()
    }

    private fun setupListeners() {
        // Dialog xác nhận khi bấm Back hệ thống lúc đang chỉnh sửa
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isEditing) {
                    showExitEditingDialog()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        binding.apply {
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
                val bio = edtBio.text.toString().trim()
                val birthday = if (tvBirthday.text.toString() == BIRTHDAY_PLACEHOLDER) "" else tvBirthday.text.toString()
                val gender = getSelectedGender()

                if (fullName.isEmpty()) {
                    tilFullName.error = "Vui lòng nhập họ và tên"
                    return@setOnClickListener
                }
                if (email.isEmpty()) {
                    tilEmail.error = "Vui lòng nhập email"
                    return@setOnClickListener
                }
                
                // Email đã bị khóa không cho sửa đổi nên không cần validate chi tiết, nhưng nếu cần:
                if (!email.endsWith("@gmail.com") || email.length <= 10) {
                    tilEmail.error = "Email phải có đuôi @gmail.com"
                    return@setOnClickListener
                }

                if (phone.isEmpty()) {
                    tilPhone.error = "Số điện thoại không được để trống"
                    return@setOnClickListener
                }
                if (!phone.matches(VIETNAM_PHONE_REGEX)) {
                    tilPhone.error = "Số điện thoại phải có 10 số, bắt đầu bằng 03, 05, 07, 08 hoặc 09"
                    return@setOnClickListener
                }

                viewModel.updateUserInfo(fullName, email, phone, address, birthday, gender, bio)
            }

            // Nút Back trong UI: hiện dialog nếu đang chỉnh sửa, thoát thẳng nếu không
            btnBack.setOnClickListener {
                if (isEditing) showExitEditingDialog() else finish()
            }
        }
    }

    private fun showExitEditingDialog() {
        MessageUtils.showConfirmDialog(
            context = this,
            title = "Hủy chỉnh sửa?",
            message = "Thông tin bạn đang chỉnh sửa sẽ không được lưu. Bạn có chắc muốn thoát không?",
            positiveText = "Thoát",
            negativeText = "Tiếp tục chỉnh sửa",
            onConfirm = { finish() }
        )
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { loading ->
            binding.apply {
                progressBar.visibility = if (loading) View.VISIBLE else View.GONE
                btnSave.isEnabled = !loading
                btnCancel.isEnabled = !loading
            }
        }

        viewModel.userInfo.observe(this) { user ->
            originalUser = user
            bindUserData(user)
        }

        viewModel.updateResult.observe(this) { success ->
            if (success) {
                binding.apply {
                    originalUser = User(
                        fullName = edtFullName.text.toString().trim(),
                        email = edtEmail.text.toString().trim(),
                        phone = edtPhone.text.toString().trim(),
                        address = edtAddress.text.toString().trim(),
                        bio = edtBio.text.toString().trim(),
                        birthday = if (tvBirthday.text.toString() == BIRTHDAY_PLACEHOLDER) "" else tvBirthday.text.toString(),
                        gender = getSelectedGender()
                    )
                }
                isEditing = false
                enableEditing(false)
                MessageUtils.showSuccessDialog(
                    this,
                    "Cập nhật thành công",
                    "Thông tin cá nhân đã được cập nhật."
                )
                viewModel.resetUpdateResult()
            }
        }

        viewModel.errorMessage.observe(this) { msg ->
            if (!msg.isNullOrEmpty()) {
                MessageUtils.showErrorDialog(this, "Lỗi", msg)
                viewModel.resetErrorMessage()
            }
        }
    }

    private fun bindUserData(user: User) {
        binding.apply {
            edtFullName.setText(user.fullName)
            edtEmail.setText(user.email)
            edtPhone.setText(user.phone)
            edtAddress.setText(user.address)
            edtBio.setText(user.bio)

            if (user.birthday.isNotEmpty()) {
                tvBirthday.text = user.birthday
                tvBirthday.setTextColor(ContextCompat.getColor(this@PersonalInfoActivity, R.color.text_primary))
            } else {
                tvBirthday.text = BIRTHDAY_PLACEHOLDER
                tvBirthday.setTextColor(ContextCompat.getColor(this@PersonalInfoActivity, R.color.text_hint))
            }
            setGenderRadio(user.gender)
        }
    }

    private fun restoreOriginalValues() {
        originalUser?.let { bindUserData(it) }
    }

    private fun enableEditing(enabled: Boolean) {
        binding.apply {
            edtFullName.isEnabled = false
            edtEmail.isEnabled = false
            edtPhone.isEnabled = enabled
            edtAddress.isEnabled = enabled
            edtBio.isEnabled = enabled
            rbMale.isEnabled = enabled
            rbFemale.isEnabled = enabled
            btnPickDate.visibility = if (enabled) View.VISIBLE else View.GONE
            btnEdit.visibility = if (enabled) View.GONE else View.VISIBLE
            btnSave.visibility = if (enabled) View.VISIBLE else View.GONE
            btnCancel.visibility = if (enabled) View.VISIBLE else View.GONE
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, day ->
                val selectedDate = String.format("%02d/%02d/%04d", day, month + 1, year)
                binding.tvBirthday.text = selectedDate
                binding.tvBirthday.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun setGenderRadio(gender: String) {
        binding.apply {
            when (gender) {
                "Nam" -> rbMale.isChecked = true
                "Nữ" -> rbFemale.isChecked = true
                else -> rgGender.clearCheck()
            }
        }
    }

    private fun getSelectedGender(): String {
        return when (binding.rgGender.checkedRadioButtonId) {
            R.id.rbMale -> "Nam"
            R.id.rbFemale -> "Nữ"
            else -> ""
        }
    }

    private fun clearErrors() {
        binding.apply {
            tilFullName.error = null
            tilEmail.error = null
            tilPhone.error = null
            tilAddress.error = null
            tilBio.error = null
        }
    }
}
