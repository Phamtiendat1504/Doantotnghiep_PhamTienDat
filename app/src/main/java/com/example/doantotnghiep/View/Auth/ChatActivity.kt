package com.example.doantotnghiep.View.Auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.View.Adapter.MessageAdapter
import com.example.doantotnghiep.ViewModel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChatActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_OTHER_UID    = "other_uid"
        const val EXTRA_OTHER_NAME   = "other_name"
        const val EXTRA_OTHER_AVATAR = "other_avatar"

        // Biến toàn cục để nhận biết người dùng đang mở chat với ai
        var currentOpenedChatOtherUid: String? = null
    }

    private lateinit var viewModel: ChatViewModel
    private lateinit var adapter: MessageAdapter
    private lateinit var rvMessages: RecyclerView
    private lateinit var etInput: EditText
    private lateinit var btnSend: CardView
    private lateinit var ivPartnerAvatar: ImageView
    private lateinit var tvPartnerName: TextView
    private lateinit var btnCall: ImageView
    private lateinit var btnBack: ImageView
    private lateinit var btnAttachImage: ImageView

    private val myUid by lazy { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    private var otherUid: String = "" // Lưu biến mức class để dùng trong onResume/onPause

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        otherUid = intent.getStringExtra(EXTRA_OTHER_UID) ?: run { finish(); return }
        val otherName   = intent.getStringExtra(EXTRA_OTHER_NAME) ?: "Người dùng"
        val otherAvatar = intent.getStringExtra(EXTRA_OTHER_AVATAR) ?: ""

        // Kiểm tra auth
        if (myUid.isEmpty()) {
            android.widget.Toast.makeText(this, "Bạn cần đăng nhập để nhắn tin.", android.widget.Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Không cho tự nhắn tin với chính mình
        if (myUid == otherUid) { finish(); return }

        initViews()
        setupHeader(otherName, otherAvatar)
        setupRecyclerView()

        viewModel = ViewModelProvider(this)[ChatViewModel::class.java]
        observeViewModel()

        // Tải thông tin của mình để gửi lên chat
        loadMyInfoAndOpenChat(myUid, otherUid, otherName, otherAvatar)

        btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        btnSend.setOnClickListener { sendMessage() }
        etInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { sendMessage(); true } else false
        }
        
        btnCall.setOnClickListener { callUser() }
    }

    private fun callUser() {
        FirebaseFirestore.getInstance().collection("users").document(otherUid).get()
            .addOnSuccessListener { doc ->
                val phone = doc.getString("phone") ?: ""
                if (phone.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:$phone"))
                    startActivity(intent)
                } else {
                    MessageUtils.showErrorDialog(this, "Thông báo", "Người dùng này chưa cập nhật số điện thoại.")
                }
            }
            .addOnFailureListener {
                MessageUtils.showErrorDialog(this, "Lỗi", "Không lấy được số điện thoại.")
            }
    }

    private val pickImageLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
        if (uri != null) {
            // Không upload ngay mà gửi qua ViewModel
            val loadingDialog = MessageUtils.showLoadingDialog(
                context = this,
                title = "\u0110ang g\u1eedi \u1ea3nh",
                message = "\u1ea2nh \u0111ang \u0111\u01b0\u1ee3c t\u1ea3i l\u00ean, vui l\u00f2ng ch\u1edd."
            )

            viewModel.sendImageMessage(
                imageUri = uri,
                text = etInput.text.toString().trim(),
                onSuccess = {
                    loadingDialog.dismiss()
                    etInput.text.clear()
                    if (adapter.itemCount > 0) {
                        rvMessages.smoothScrollToPosition(adapter.itemCount - 1)
                    }
                },
                onFailure = { err ->
                    loadingDialog.dismiss()
                    MessageUtils.showErrorDialog(this, "Lỗi", "Không thể gửi ảnh: $err")
                }
            )
        }
    }

    private lateinit var btnCamera: ImageView
    private var tempCameraUri: android.net.Uri? = null

    private val takePictureLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success) {
            tempCameraUri?.let { uri ->
                val loadingDialog = MessageUtils.showLoadingDialog(
                    context = this,
                    title = "\u0110ang g\u1eedi \u1ea3nh",
                    message = "\u1ea2nh \u0111ang \u0111\u01b0\u1ee3c t\u1ea3i l\u00ean, vui l\u00f2ng ch\u1edd."
                )
                viewModel.sendImageMessage(
                    imageUri = uri,
                    text = etInput.text.toString().trim(),
                    onSuccess = {
                        loadingDialog.dismiss()
                        etInput.text.clear()
                        if (adapter.itemCount > 0) {
                            rvMessages.smoothScrollToPosition(adapter.itemCount - 1)
                        }
                    },
                    onFailure = { err ->
                        loadingDialog.dismiss()
                        MessageUtils.showErrorDialog(this, "Lỗi", "Không thể gửi ảnh: $err")
                    }
                )
            }
        }
    }

    private fun openCamera() {
        try {
            val storageDir = cacheDir
            val tempFile = java.io.File.createTempFile("camera_image_${System.currentTimeMillis()}", ".jpg", storageDir)
            tempCameraUri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "com.example.doantotnghiep.fileprovider",
                tempFile
            )
            tempCameraUri?.let { uri ->
                takePictureLauncher.launch(uri)
            }
        } catch (e: Exception) {
            MessageUtils.showErrorDialog(this, "Lỗi", "Không thể khởi động Camera: ${e.message}")
        }
    }

    private fun initViews() {
        rvMessages      = findViewById(R.id.rvMessages)
        etInput         = findViewById(R.id.etChatInput)
        btnSend         = findViewById(R.id.btnSendMessage)
        ivPartnerAvatar = findViewById(R.id.ivChatPartnerAvatar)
        tvPartnerName   = findViewById(R.id.tvChatPartnerName)
        btnBack         = findViewById(R.id.btnChatBack)
        btnCall         = findViewById(R.id.btnCall)
        btnAttachImage  = findViewById(R.id.btnAttachImage)
        btnCamera       = findViewById(R.id.btnCamera)

        btnAttachImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        btnCamera.setOnClickListener {
            openCamera()
        }
    }

    private fun setupHeader(name: String, avatarUrl: String) {
        tvPartnerName.text = name
        if (avatarUrl.isNotEmpty()) {
            ivPartnerAvatar.setPadding(0, 0, 0, 0)
            ivPartnerAvatar.imageTintList = null
            Glide.with(this).load(avatarUrl).circleCrop().into(ivPartnerAvatar)
        }
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter(
            myUid = myUid,
            onLongClickMessage = { msg ->
                // Giữ lâu → hỏi xác nhận xóa (chỉ tin mình gửi, popup đã lọc ở Adapter)
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Xóa tin nhắn")
                    .setMessage("Bạn có chắc chắn muốn xóa tin nhắn này không?")
                    .setPositiveButton("Xóa") { _, _ ->
                        viewModel.deleteMessage(
                            message   = msg,
                            onSuccess = { /* RecyclerView tự cập nhật qua real-time listener */ },
                            onFailure = { err ->
                                MessageUtils.showErrorDialog(this, "Lỗi", err)
                            }
                        )
                    }
                    .setNegativeButton("Hủy", null)
                    .show()
            },
            onReaction = { messageId, emoji ->
                viewModel.addReaction(messageId, emoji)
            }
        )
        rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvMessages.adapter = adapter
    }


    private fun loadMyInfoAndOpenChat(
        myUid: String,
        otherUid: String,
        otherName: String,
        otherAvatar: String
    ) {
        FirebaseFirestore.getInstance().collection("users").document(myUid)
            .get()
            .addOnSuccessListener { doc ->
                val myName   = doc.getString("fullName") ?: "Tôi"
                val myAvatar = doc.getString("avatarUrl") ?: ""
                viewModel.openChat(myUid, myName, myAvatar, otherUid, otherName, otherAvatar)
            }
            .addOnFailureListener {
                // Fallback nếu không tải được tên
                viewModel.openChat(myUid, "Tôi", "", otherUid, otherName, otherAvatar)
            }
    }

    private fun observeViewModel() {
        viewModel.messages.observe(this) { messages ->
            adapter.submitList(messages)
            if (messages.isNotEmpty()) {
                rvMessages.scrollToPosition(messages.size - 1)
            }
        }

        viewModel.errorMessage.observe(this) { err ->
            if (!err.isNullOrEmpty()) {
                MessageUtils.showErrorDialog(this, "Lỗi", err)
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            // Có thể thêm progress indicator nếu muốn
        }
    }

    private var presenceListener: com.google.firebase.firestore.ListenerRegistration? = null

    private fun startListeningPresence() {
        presenceListener?.remove()
        if (otherUid.isEmpty()) return
        val tvStatus = findViewById<android.widget.TextView>(R.id.tvChatStatus)
        presenceListener = FirebaseFirestore.getInstance()
            .collection("users").document(otherUid)
            .addSnapshotListener { doc, _ ->
                if (doc == null || !doc.exists()) return@addSnapshotListener
                val isOnline  = doc.getBoolean("isOnline") ?: false
                val lastSeen  = doc.getLong("lastSeen") ?: 0L
                if (isOnline) {
                    tvStatus.text      = "Đang hoạt động"
                    tvStatus.setTextColor(0xFF4CAF50.toInt()) // Xanh lá
                } else {
                    tvStatus.text      = formatLastSeen(lastSeen)
                    tvStatus.setTextColor(0xFF9E9E9E.toInt()) // Xám
                }
            }
    }

    private fun formatLastSeen(lastSeen: Long): String {
        if (lastSeen == 0L) return "Ngoại tuyến"
        val diff = System.currentTimeMillis() - lastSeen
        val minutes = diff / 60_000
        val hours   = diff / 3_600_000
        val days    = diff / 86_400_000
        return when {
            minutes < 1  -> "Vừa mới hoạt động"
            minutes < 60 -> "Hoạt động $minutes phút trước"
            hours   < 24 -> "Hoạt động $hours giờ trước"
            days    < 7  -> "Hoạt động $days ngày trước"
            else         -> "Ngoại tuyến"
        }
    }

    private fun sendMessage() {
        val text = etInput.text.toString().trim()
        if (text.isEmpty()) return
        etInput.text.clear()
        viewModel.sendMessage(text)
    }

    override fun onResume() {
        super.onResume()
        currentOpenedChatOtherUid = otherUid
        viewModel.markSeen()
        startListeningPresence()
    }

    override fun onPause() {
        super.onPause()
        currentOpenedChatOtherUid = null
        presenceListener?.remove()
        presenceListener = null
    }
}
