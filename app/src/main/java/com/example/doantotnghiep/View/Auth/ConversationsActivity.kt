package com.example.doantotnghiep.View.Auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doantotnghiep.Model.Conversation
import com.example.doantotnghiep.R
import com.example.doantotnghiep.View.Adapter.ConversationAdapter
import com.example.doantotnghiep.ViewModel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth

class ConversationsActivity : AppCompatActivity() {

    private lateinit var viewModel: ChatViewModel
    private lateinit var adapter: ConversationAdapter
    private lateinit var rvConversations: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var btnBack: ImageView

    private val myUid by lazy { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversations)

        if (myUid.isEmpty()) { finish(); return }

        rvConversations = findViewById(R.id.rvConversations)
        layoutEmpty     = findViewById(R.id.layoutConvEmpty)
        btnBack         = findViewById(R.id.btnConvBack)

        setupRecyclerView()

        viewModel = ViewModelProvider(this)[ChatViewModel::class.java]
        viewModel.listenConversations(myUid)

        viewModel.conversations.observe(this) { convList ->
            if (convList.isEmpty()) {
                layoutEmpty.visibility     = View.VISIBLE
                rvConversations.visibility = View.GONE
            } else {
                layoutEmpty.visibility     = View.GONE
                rvConversations.visibility = View.VISIBLE
                adapter.submitList(convList)
            }
        }

        btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecyclerView() {
        adapter = ConversationAdapter(
            myUid       = myUid,
            onItemClick = { conv -> openChat(conv) },
            onLongClick = { conv -> confirmDeleteConversation(conv) }
        )
        rvConversations.layoutManager = LinearLayoutManager(this)
        rvConversations.adapter = adapter
    }

    private fun confirmDeleteConversation(conv: Conversation) {
        val otherId   = conv.participants.firstOrNull { it != myUid } ?: return
        val otherName = conv.participantNames[otherId] ?: "người dùng này"

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Xóa cuộc trò chuyện")
            .setMessage("Xóa toàn bộ tin nhắn với $otherName?\nChỉ xóa phía bạn, người kia vẫn thấy bình thường.")
            .setPositiveButton("Xóa") { _, _ ->
                // Đảm bảo ViewModel biết uid của mình trước khi xóa
                viewModel.currentMyUid = myUid
                viewModel.deleteConversation(
                    chatId    = conv.chatId,
                    onSuccess = { /* danh sách tự cập nhật qua real-time listener */ },
                    onFailure = { err ->
                        android.widget.Toast.makeText(this, "Lỗi: $err", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Hủy", null)
            .show()
    }



    private fun openChat(conv: Conversation) {
        val otherId     = conv.participants.firstOrNull { it != myUid } ?: return
        val otherName   = conv.participantNames[otherId] ?: "Người dùng"
        val otherAvatar = conv.participantAvatars[otherId] ?: ""

        startActivity(
            Intent(this, ChatActivity::class.java).apply {
                putExtra(ChatActivity.EXTRA_OTHER_UID,    otherId)
                putExtra(ChatActivity.EXTRA_OTHER_NAME,   otherName)
                putExtra(ChatActivity.EXTRA_OTHER_AVATAR, otherAvatar)
            }
        )
    }
}
