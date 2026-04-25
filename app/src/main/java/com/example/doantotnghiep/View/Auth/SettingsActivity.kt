package com.example.doantotnghiep.View.Auth

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.AppSettings
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.util.Locale
import kotlin.concurrent.thread

class SettingsActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var switchPushAll: SwitchCompat
    private lateinit var switchPushChat: SwitchCompat
    private lateinit var switchPushAppointment: SwitchCompat
    private lateinit var switchPushSystem: SwitchCompat
    private lateinit var btnClearImageCache: LinearLayout
    private lateinit var btnTerms: LinearLayout
    private lateinit var btnAbout: LinearLayout
    private lateinit var btnDeleteAccount: LinearLayout
    private lateinit var tvCacheSize: TextView
    private lateinit var tvAppVersion: TextView

    private val authRepository = AuthRepository()
    private var bindingState = false
    private var deleteLoadingDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initViews()
        setupToolbar()
        bindStateFromPrefs()
        setupListeners()
        updateVersion()
        updateCacheSize()
    }

    override fun onDestroy() {
        deleteLoadingDialog?.dismiss()
        deleteLoadingDialog = null
        super.onDestroy()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbarSettings)
        switchPushAll = findViewById(R.id.switchPushAll)
        switchPushChat = findViewById(R.id.switchPushChat)
        switchPushAppointment = findViewById(R.id.switchPushAppointment)
        switchPushSystem = findViewById(R.id.switchPushSystem)
        btnClearImageCache = findViewById(R.id.btnClearImageCache)
        btnTerms = findViewById(R.id.btnTerms)
        btnAbout = findViewById(R.id.btnAbout)
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount)
        tvCacheSize = findViewById(R.id.tvCacheSize)
        tvAppVersion = findViewById(R.id.tvAppVersion)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationIcon(R.drawable.ic_back)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun bindStateFromPrefs() {
        bindingState = true
        switchPushAll.isChecked = AppSettings.isPushEnabled(this)
        switchPushChat.isChecked = AppSettings.isChatPushEnabled(this)
        switchPushAppointment.isChecked = AppSettings.isAppointmentPushEnabled(this)
        switchPushSystem.isChecked = AppSettings.isSystemPushEnabled(this)
        bindingState = false
        updateChildSwitchEnabled()
    }

    private fun setupListeners() {
        switchPushAll.setOnCheckedChangeListener { _, isChecked ->
            if (bindingState) return@setOnCheckedChangeListener
            AppSettings.setPushEnabled(this, isChecked)
            updateChildSwitchEnabled()
        }

        switchPushChat.setOnCheckedChangeListener { _, isChecked ->
            if (bindingState) return@setOnCheckedChangeListener
            AppSettings.setChatPushEnabled(this, isChecked)
        }

        switchPushAppointment.setOnCheckedChangeListener { _, isChecked ->
            if (bindingState) return@setOnCheckedChangeListener
            AppSettings.setAppointmentPushEnabled(this, isChecked)
        }

        switchPushSystem.setOnCheckedChangeListener { _, isChecked ->
            if (bindingState) return@setOnCheckedChangeListener
            AppSettings.setSystemPushEnabled(this, isChecked)
        }

        btnClearImageCache.setOnClickListener {
            MessageUtils.showConfirmDialog(
                context = this,
                title = "Xóa bộ nhớ tạm",
                message = "Bạn có chắc muốn xóa bộ nhớ tạm hình ảnh không?",
                positiveText = "Xóa"
            ) { clearImageCache() }
        }

        btnTerms.setOnClickListener {
            openInfoContent(
                title = getString(R.string.settings_terms_title),
                content = getString(R.string.settings_terms_content)
            )
        }

        btnAbout.setOnClickListener {
            openInfoContent(
                title = getString(R.string.settings_about_title),
                content = getString(R.string.settings_about_content)
            )
        }

        btnDeleteAccount.setOnClickListener {
            MessageUtils.showConfirmDialog(
                context = this,
                title = "Yêu cầu hủy tài khoản",
                message = "Hệ thống sẽ xóa dữ liệu tài khoản của bạn trên backend. Thao tác này không thể khôi phục. Bạn có chắc muốn tiếp tục?",
                positiveText = "Xác nhận hủy"
            ) {
                requestDeleteAccount()
            }
        }
    }

    private fun requestDeleteAccount() {
        val functionUrl = getString(R.string.delete_account_function_url).trim()
        if (functionUrl.isBlank()) {
            MessageUtils.showErrorDialog(this, "Thiếu cấu hình", "Chưa cấu hình URL Cloud Function xóa tài khoản.")
            return
        }

        deleteLoadingDialog?.dismiss()
        deleteLoadingDialog = MessageUtils.showLoadingDialog(
            context = this,
            title = "Đang xử lý hủy tài khoản",
            message = "Hệ thống đang xóa dữ liệu trên backend. Vui lòng chờ trong giây lát."
        )

        authRepository.requestAccountDeletion(
            functionUrl = functionUrl,
            onSuccess = {
                deleteLoadingDialog?.dismiss()
                deleteLoadingDialog = null
                FirebaseAuth.getInstance().signOut()
                MessageUtils.showSuccessDialog(
                    context = this,
                    title = "Đã hủy tài khoản",
                    message = "Tài khoản và dữ liệu liên quan đã được xử lý xóa trên hệ thống."
                ) {
                    val intent = Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
            },
            onFailure = { error ->
                deleteLoadingDialog?.dismiss()
                deleteLoadingDialog = null
                MessageUtils.showErrorDialog(
                    context = this,
                    title = "Hủy tài khoản thất bại",
                    message = error
                )
            }
        )
    }

    private fun openInfoContent(title: String, content: String) {
        startActivity(
            Intent(this, InfoContentActivity::class.java).apply {
                putExtra(InfoContentActivity.EXTRA_TITLE, title)
                putExtra(InfoContentActivity.EXTRA_CONTENT, content)
            }
        )
    }

    private fun updateChildSwitchEnabled() {
        val enabled = switchPushAll.isChecked
        switchPushChat.isEnabled = enabled
        switchPushAppointment.isEnabled = enabled
        switchPushSystem.isEnabled = enabled
        switchPushChat.alpha = if (enabled) 1f else 0.5f
        switchPushAppointment.alpha = if (enabled) 1f else 0.5f
        switchPushSystem.alpha = if (enabled) 1f else 0.5f
    }

    private fun updateVersion() {
        @Suppress("DEPRECATION")
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
        tvAppVersion.text = getString(R.string.settings_version_format, versionName)
    }

    private fun updateCacheSize() {
        val totalSize = dirSize(cacheDir) + dirSize(externalCacheDir)
        tvCacheSize.text = formatSize(totalSize)
    }

    private fun clearImageCache() {
        Glide.get(this).clearMemory()
        thread {
            Glide.get(this).clearDiskCache()
            clearDirectory(cacheDir)
            clearDirectory(externalCacheDir)
            runOnUiThread {
                updateCacheSize()
                MessageUtils.showSuccessDialog(
                    context = this,
                    title = "Đã xóa bộ nhớ tạm",
                    message = "Dữ liệu cache ảnh đã được dọn dẹp thành công."
                )
            }
        }
    }

    private fun dirSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        val files = dir.listFiles() ?: return 0L
        var sum = 0L
        for (file in files) {
            sum += if (file.isDirectory) dirSize(file) else file.length()
        }
        return sum
    }

    private fun clearDirectory(dir: File?) {
        if (dir == null || !dir.exists()) return
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) clearDirectory(file)
            file.delete()
        }
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        var value = bytes.toDouble()
        var unitIndex = 0
        while (value >= 1024 && unitIndex < units.lastIndex) {
            value /= 1024
            unitIndex++
        }
        return String.format(Locale.getDefault(), "%.1f %s", value, units[unitIndex])
    }
}
