package com.example.doantotnghiep.View.Auth

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.ImageUtils
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.View.Adapter.MessageAdapter
import com.example.doantotnghiep.ViewModel.SupportViewModel
import com.google.firebase.auth.FirebaseAuth

class SupportTicketDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TICKET_ID = "ticket_id"
        const val EXTRA_TICKET_TITLE = "ticket_title"
        const val EXTRA_TICKET_STATUS = "ticket_status"
    }

    private lateinit var viewModel: SupportViewModel
    private lateinit var adapter: MessageAdapter
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var cardPreview: CardView
    private lateinit var ivPreview: ImageView
    private lateinit var layoutInput: View
    private lateinit var btnCloseTicket: TextView

    private val ticketId by lazy { intent.getStringExtra(EXTRA_TICKET_ID) ?: "" }
    private val myUid by lazy { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    private var selectedImageUri: Uri? = null
    private var ticketStatus = ""

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = ImageUtils.compressImage(this, uri) ?: uri
            cardPreview.visibility = View.VISIBLE
            ivPreview.setImageURI(selectedImageUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_support_ticket_detail)

        if (ticketId.isBlank() || myUid.isBlank()) {
            finish()
            return
        }

        ticketStatus = intent.getStringExtra(EXTRA_TICKET_STATUS) ?: "new"
        viewModel = ViewModelProvider(this)[SupportViewModel::class.java]
        initViews()
        setupRecyclerView()
        observeViewModel()
        viewModel.listenMessages(ticketId)
    }

    private fun initViews() {
        rvMessages = findViewById(R.id.rvSupportMessages)
        etMessage = findViewById(R.id.etSupportMessage)
        cardPreview = findViewById(R.id.cardImagePreview)
        ivPreview = findViewById(R.id.ivImagePreview)
        layoutInput = findViewById(R.id.layoutSupportInput)
        btnCloseTicket = findViewById(R.id.btnCloseTicket)

        findViewById<TextView>(R.id.tvSupportDetailTitle).text =
            intent.getStringExtra(EXTRA_TICKET_TITLE) ?: "Chi tiết hỗ trợ"
        updateStatusUi(ticketStatus)

        findViewById<View>(R.id.btnSupportDetailBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnSupportAttachImage).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        findViewById<View>(R.id.btnRemovePreview).setOnClickListener { clearImagePreview() }
        findViewById<View>(R.id.btnSupportSend).setOnClickListener { sendMessage() }
        etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }
        btnCloseTicket.setOnClickListener { confirmCloseTicket() }
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter(myUid = myUid)
        rvMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        rvMessages.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.messages.observe(this) { messages ->
            adapter.submitList(messages)
            if (messages.isNotEmpty()) {
                rvMessages.scrollToPosition(messages.size - 1)
                viewModel.markUserRead(ticketId)
            }
        }
    }

    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        val imageUri = selectedImageUri
        if (text.isBlank() && imageUri == null) return
        if (ticketStatus == "closed") {
            MessageUtils.showInfoDialog(this, "Thông báo", "Yêu cầu này đã đóng, bạn không thể gửi thêm tin nhắn.")
            return
        }

        val loading = MessageUtils.showLoadingDialog(
            context = this,
            title = "Đang gửi tin nhắn",
            message = "Nội dung hỗ trợ đang được gửi lên hệ thống."
        )
        viewModel.sendMessage(
            ticketId,
            text,
            imageUri,
            onSuccess = {
                loading.dismiss()
                etMessage.text.clear()
                clearImagePreview()
            },
            onFailure = {
                loading.dismiss()
                MessageUtils.showErrorDialog(this, "Lỗi", it)
            }
        )
    }

    private fun confirmCloseTicket() {
        AlertDialog.Builder(this)
            .setTitle("Đóng yêu cầu hỗ trợ")
            .setMessage("Bạn đã được hỗ trợ xong và muốn đóng yêu cầu này?")
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Đóng") { _, _ ->
                viewModel.closeTicket(
                    ticketId,
                    onSuccess = {
                        ticketStatus = "closed"
                        updateStatusUi(ticketStatus)
                    },
                    onFailure = { MessageUtils.showErrorDialog(this, "Lỗi", it) }
                )
            }
            .show()
    }

    private fun clearImagePreview() {
        selectedImageUri = null
        cardPreview.visibility = View.GONE
        ivPreview.setImageDrawable(null)
    }

    private fun updateStatusUi(status: String) {
        findViewById<TextView>(R.id.tvSupportDetailStatus).text = when (status) {
            "new" -> "Yêu cầu mới, đang chờ admin phản hồi"
            "in_progress" -> "Admin đang xử lý yêu cầu này"
            "resolved" -> "Yêu cầu đã được admin xử lý"
            "closed" -> "Yêu cầu đã đóng"
            else -> "Admin sẽ phản hồi trong thời gian sớm nhất"
        }
        val closed = status == "closed"
        layoutInput.visibility = if (closed) View.GONE else View.VISIBLE
        btnCloseTicket.visibility = if (closed) View.GONE else View.VISIBLE
    }
}
