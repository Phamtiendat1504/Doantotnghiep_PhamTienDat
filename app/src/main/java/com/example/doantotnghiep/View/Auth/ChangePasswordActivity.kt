package com.example.doantotnghiep.View.Auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.ViewModel.AuthViewModel
import com.example.doantotnghiep.databinding.ActivityChangePasswordBinding

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        // Dialog xác nhận khi bấm Back hệ thống lúc đã nhập thông tin
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
            btnChangePassword.setOnClickListener {
                clearErrors()
                val oldPassword = edtOldPassword.text.toString()
                val newPassword = edtNewPassword.text.toString()
                val confirmPassword = edtConfirmPassword.text.toString()
                authViewModel.changePassword(oldPassword, newPassword, confirmPassword)
            }

            edtConfirmPassword.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    btnChangePassword.performClick()
                    true
                } else {
                    false
                }
            }

            btnBack.setOnClickListener {
                if (hasAnyInputFilled()) showExitConfirmDialog() else finish()
            }
        }
    }

    private fun hasAnyInputFilled(): Boolean =
        binding.edtOldPassword.text?.isNotEmpty() == true ||
        binding.edtNewPassword.text?.isNotEmpty() == true ||
        binding.edtConfirmPassword.text?.isNotEmpty() == true

    private fun showExitConfirmDialog() {
        MessageUtils.showConfirmDialog(
            context = this,
            title = "Bỏ qua đổi mật khẩu?",
            message = "Thông tin bạn đã nhập sẽ bị mất. Bạn có chắc muốn thoát không?",
            positiveText = "Thoát",
            negativeText = "Tiếp tục điền",
            onConfirm = { finish() }
        )
    }

    private fun observeViewModel() {
        authViewModel.isLoading.observe(this) { loading ->
            binding.apply {
                progressBar.visibility = if (loading) View.VISIBLE else View.GONE
                btnChangePassword.isEnabled = !loading
                
                // UX Improvement: Khóa nhập liệu khi đang gửi request lên Firebase
                edtOldPassword.isEnabled = !loading
                edtNewPassword.isEnabled = !loading
                edtConfirmPassword.isEnabled = !loading
            }
        }

        authViewModel.changePasswordResult.observe(this) { success ->
            if (success) {
                authViewModel.resetChangePasswordResult()
                MessageUtils.showSuccessDialog(this, "Thành công", "Mật khẩu đã được thay đổi. Vui lòng đăng nhập lại.") {
                    authViewModel.logOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
            }
        }

        authViewModel.wrongOldPassword.observe(this) { wrong ->
            if (wrong) {
                binding.apply {
                    tilOldPassword.error = "Mật khẩu cũ không đúng"
                    // UX Improvement: Xóa trống mật khẩu cũ nhập sai và tự động đưa focus về lại
                    edtOldPassword.setText("")
                    edtOldPassword.requestFocus()
                }
                authViewModel.resetWrongOldPassword()
            }
        }

        authViewModel.errorMessage.observe(this) { msg ->
            binding.apply {
                when (msg) {
                    "old_empty" -> { 
                        tilOldPassword.error = "Vui lòng nhập mật khẩu cũ"
                        edtOldPassword.requestFocus()
                        authViewModel.resetErrorMessage() 
                    }
                    "new_empty" -> { 
                        tilNewPassword.error = "Vui lòng nhập mật khẩu mới"
                        edtNewPassword.requestFocus()
                        authViewModel.resetErrorMessage() 
                    }
                    "new_weak" -> { 
                        tilNewPassword.error = "Mật khẩu phải có ít nhất 12 ký tự, gồm chữ hoa, số và ký tự đặc biệt"
                        edtNewPassword.requestFocus()
                        authViewModel.resetErrorMessage() 
                    }
                    "confirm_empty" -> { 
                        tilConfirmPassword.error = "Vui lòng nhập lại mật khẩu mới"
                        edtConfirmPassword.requestFocus()
                        authViewModel.resetErrorMessage() 
                    }
                    "confirm_mismatch" -> { 
                        tilConfirmPassword.error = "Mật khẩu xác nhận không khớp"
                        edtConfirmPassword.requestFocus()
                        authViewModel.resetErrorMessage() 
                    }
                    "same_password" -> { 
                        tilNewPassword.error = "Mật khẩu mới phải khác mật khẩu cũ"
                        edtNewPassword.requestFocus()
                        authViewModel.resetErrorMessage() 
                    }
                    else -> if (!msg.isNullOrEmpty()) { 
                        MessageUtils.showErrorDialog(this@ChangePasswordActivity, "Lỗi", msg)
                        authViewModel.resetErrorMessage() 
                    }
                }
            }
        }
    }

    private fun clearErrors() {
        binding.apply {
            tilOldPassword.error = null
            tilNewPassword.error = null
            tilConfirmPassword.error = null
        }
    }
}
