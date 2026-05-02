package com.example.doantotnghiep.View.Auth

import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doantotnghiep.Model.ChatMessage
import com.example.doantotnghiep.R
import com.example.doantotnghiep.View.Adapter.AIChatAdapter
import com.example.doantotnghiep.ViewModel.AIChatViewModel

class AIChatActivity : AppCompatActivity() {

    private lateinit var viewModel: AIChatViewModel
    private lateinit var adapter: AIChatAdapter
    private lateinit var rvChat: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton

    // List dùng chung giữa adapter và observer
    private val messageList: MutableList<ChatMessage> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_chat)

        rvChat    = findViewById(R.id.rvChat)
        etMessage = findViewById(R.id.etMessage)
        btnSend   = findViewById(R.id.btnSend)
        val btnBack: ImageButton = findViewById(R.id.btnBack)
        val btnClearHistory: ImageButton = findViewById(R.id.btnClearHistory)

        btnBack.setOnClickListener { finish() }
        btnClearHistory.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Xóa lịch sử chat")
                .setMessage("Bạn có chắc muốn xóa toàn bộ lịch sử chat không?")
                .setPositiveButton("Xóa") { _, _ -> viewModel.deleteChatHistory() }
                .setNegativeButton("Hủy", null)
                .show()
        }

        // ── Adapter dùng đúng list chung ──────────────────────────────
        adapter = AIChatAdapter(this, messageList)
        rvChat.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvChat.adapter = adapter

        // ── ViewModel ─────────────────────────────────────────────────
        viewModel = ViewModelProvider(this)[AIChatViewModel::class.java]

        // ── Observe LiveData và cập nhật adapter ──────────────────────
        viewModel.messages.observe(this) { newMessages ->
            Log.d("AIChatActivity", "Messages updated: ${newMessages.size} items")
            messageList.clear()
            messageList.addAll(newMessages)
            adapter.notifyDataSetChanged()
            if (messageList.isNotEmpty()) {
                rvChat.scrollToPosition(messageList.size - 1)
            }
        }

        // ── Nút gửi ───────────────────────────────────────────────────
        btnSend.setOnClickListener { sendMessage() }

        etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else false
        }
    }

    private fun sendMessage() {
        val text = etMessage.text?.toString()?.trim() ?: return
        if (text.isBlank()) return

        etMessage.setText("")
        hideKeyboard()
        viewModel.sendMessage(text)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etMessage.windowToken, 0)
    }
}
