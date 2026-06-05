package com.example.doantotnghiep.View.Auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doantotnghiep.R
import com.example.doantotnghiep.View.Adapter.AIChatAdapter
import com.example.doantotnghiep.ViewModel.AIChatViewModel
import com.google.firebase.auth.FirebaseAuth

// Fix 3: Activity is now a thin UI layer — all logic lives in AIChatViewModel
class AIChatActivity : AppCompatActivity() {

    private lateinit var rvAiChat: RecyclerView
    private lateinit var edtMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnDeleteChat: ImageButton
    private lateinit var progressBar: ProgressBar

    private lateinit var adapter: AIChatAdapter
    private lateinit var viewModel: AIChatViewModel

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aichat)

        rvAiChat = findViewById(R.id.rvAiChat)
        edtMessage = findViewById(R.id.edtMessage)
        btnSend = findViewById(R.id.btnSend)
        btnDeleteChat = findViewById(R.id.btnDeleteChat)
        progressBar = findViewById(R.id.progressBar)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_back)
        toolbar.setNavigationOnClickListener { finish() }

        viewModel = ViewModelProvider(this)[AIChatViewModel::class.java]

        adapter = AIChatAdapter(mutableListOf(), object : AIChatAdapter.QuickReplyClickListener {
            override fun onQuickReplyClicked(text: String) {
                val query = viewModel.resolveQuickReplyAction(text)
                edtMessage.setText(query)
                sendMessage(query)
            }
        })
        rvAiChat.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        rvAiChat.adapter = adapter

        observeViewModel()

        val uid = auth.currentUser?.uid ?: ""
        viewModel.loadContext(uid) { viewModel.loadHistory(uid) }

        btnSend.setOnClickListener {
            val text = edtMessage.text.toString().trim()
            if (text.isNotEmpty()) sendMessage(text)
        }

        btnDeleteChat.setOnClickListener { showDeleteConfirmDialog() }
    }

    private fun observeViewModel() {
        viewModel.messages.observe(this) { messages ->
            adapter.updateList(messages)
            if (messages.isNotEmpty()) {
                rvAiChat.scrollToPosition(messages.size - 1)
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        // Fix 10: disable send button while a reply is being processed
        viewModel.isProcessing.observe(this) { processing ->
            btnSend.isEnabled = !processing
        }

        viewModel.error.observe(this) { msg ->
            if (!msg.isNullOrEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.deepLink.observe(this) { link ->
            if (!link.isNullOrEmpty()) {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun sendMessage(text: String) {
        val uid = auth.currentUser?.uid ?: return
        edtMessage.setText("")
        viewModel.sendMessage(text, uid)
    }

    private fun showDeleteConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("Xóa lịch sử")
            .setMessage("Bạn có chắc chắn muốn xóa toàn bộ lịch sử trò chuyện với AI không?")
            .setPositiveButton("Xóa") { _, _ ->
                val uid = auth.currentUser?.uid ?: return@setPositiveButton
                viewModel.deleteHistory(uid)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}
