package com.example.doantotnghiep.View.Auth

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doantotnghiep.Model.AIMessage
import com.example.doantotnghiep.Model.AIRoom
import com.example.doantotnghiep.R
import com.example.doantotnghiep.View.Adapter.AIChatAdapter
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult

class AIChatActivity : AppCompatActivity() {

    private lateinit var rvAiChat: RecyclerView
    private lateinit var edtMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnDeleteChat: ImageButton
    private lateinit var progressBar: ProgressBar

    private val messageList = mutableListOf<AIMessage>()
    private lateinit var adapter: AIChatAdapter

    private val db = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance()
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

        adapter = AIChatAdapter(messageList)
        rvAiChat.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvAiChat.adapter = adapter

        loadChatHistory()

        btnSend.setOnClickListener {
            val text = edtMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessageToAI(text)
            }
        }

        btnDeleteChat.setOnClickListener {
            showDeleteConfirmDialog()
        }
    }

    private fun showDeleteConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("Xóa lịch sử")
            .setMessage("Bạn có chắc chắn muốn xóa toàn bộ lịch sử trò chuyện với AI không?")
            .setPositiveButton("Xóa") { _, _ ->
                deleteAllChatHistory()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteAllChatHistory() {
        val uid = auth.currentUser?.uid ?: return
        progressBar.visibility = View.VISIBLE
        
        db.collection("users").document(uid).collection("ai_conversations")
            .get()
            .addOnSuccessListener(OnSuccessListener<QuerySnapshot> { snapshots ->
                val batch = db.batch()
                for (doc in snapshots.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().addOnCompleteListener {
                    progressBar.visibility = View.GONE
                    messageList.clear()
                    addGreetingMessage()
                    adapter.notifyDataSetChanged()
                    Toast.makeText(this@AIChatActivity, "Đã xóa lịch sử chat", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun addGreetingMessage() {
        val greeting = AIMessage(
            "model",
            "Xin chào! Mình là StayAssist AI từ Stay247.\nBạn cần tìm phòng khu vực nào hay cần mình tư vấn điều gì không?",
            System.currentTimeMillis()
        )
        messageList.add(greeting)
        adapter.notifyItemInserted(messageList.size - 1)
        rvAiChat.scrollToPosition(messageList.size - 1)
    }

    private fun loadChatHistory() {
        val uid = auth.currentUser?.uid ?: return
        progressBar.visibility = View.VISIBLE

        db.collection("users").document(uid).collection("ai_conversations")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener(OnSuccessListener<QuerySnapshot> { snapshots ->
                messageList.clear()
                if (snapshots != null && !snapshots.isEmpty) {
                    for (doc in snapshots.documents) {
                        val role = doc.getString("role") ?: ""
                        val content = doc.getString("content") ?: ""
                        val timestamp = doc.getDate("timestamp")?.time ?: System.currentTimeMillis()
                        
                        val suggestedRoomsList = mutableListOf<AIRoom>()
                        val roomsMapArray = doc.get("suggestedRooms") as? List<Map<String, Any>>
                        if (roomsMapArray != null) {
                            for (roomMap in roomsMapArray) {
                                val id = roomMap["id"] as? String ?: ""
                                val userId = roomMap["userId"] as? String ?: ""
                                val title = roomMap["title"] as? String ?: ""
                                val price = (roomMap["price"] as? Number)?.toLong() ?: 0L
                                val area = (roomMap["area"] as? Number)?.toInt() ?: 0
                                val district = roomMap["district"] as? String ?: ""
                                val imageUrl = roomMap["imageUrl"] as? String ?: ""
                                suggestedRoomsList.add(AIRoom(id, userId, title, price, area, district, imageUrl))
                            }
                        }

                        messageList.add(AIMessage(role, content, timestamp, suggestedRoomsList))
                    }
                } else {
                    addGreetingMessage()
                }
                adapter.notifyDataSetChanged()
                if (messageList.isNotEmpty()) {
                    rvAiChat.scrollToPosition(messageList.size - 1)
                }
                progressBar.visibility = View.GONE
            })
            .addOnFailureListener(OnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Lỗi tải lịch sử chat: ${e.message}", Toast.LENGTH_SHORT).show()
            })
    }

    private fun sendMessageToAI(messageText: String) {
        val uid = auth.currentUser?.uid ?: return

        // 1. Hiển thị tin nhắn của User ngay lập tức
        val now = System.currentTimeMillis()
        val userMsg = AIMessage("user", messageText, now)
        messageList.add(userMsg)
        adapter.notifyItemInserted(messageList.size - 1)
        rvAiChat.scrollToPosition(messageList.size - 1)
        edtMessage.setText("") 

        // 2. Thêm hiệu ứng "AI đang gõ..."
        val typingMsg = AIMessage("typing", "...", now + 1)
        messageList.add(typingMsg)
        val typingIndex = messageList.size - 1
        adapter.notifyItemInserted(typingIndex)
        rvAiChat.scrollToPosition(typingIndex)

        btnSend.isEnabled = false 

        val requestData = hashMapOf("message" to messageText)

        functions
            .getHttpsCallable("askAIAssistant")
            .call(requestData)
            .addOnSuccessListener { result ->
                btnSend.isEnabled = true
                
                // Xóa hiệu ứng đang gõ
                val currentTypingIndex = messageList.indexOfLast { it.role == "typing" }
                if (currentTypingIndex != -1) {
                    messageList.removeAt(currentTypingIndex)
                    adapter.notifyItemRemoved(currentTypingIndex)
                }

                val responseData = result.data as? Map<String, Any>
                val replyText = responseData?.get("reply") as? String
                
                val suggestedRoomsList = mutableListOf<AIRoom>()
                val roomsMapArray = responseData?.get("suggestedRooms") as? List<Map<String, Any>>
                if (roomsMapArray != null) {
                    for (roomMap in roomsMapArray) {
                        val id = roomMap["id"] as? String ?: ""
                        val userId = roomMap["userId"] as? String ?: ""
                        val title = roomMap["title"] as? String ?: ""
                        val price = (roomMap["price"] as? Number)?.toLong() ?: 0L
                        val area = (roomMap["area"] as? Number)?.toInt() ?: 0
                        val district = roomMap["district"] as? String ?: ""
                        val imageUrl = roomMap["imageUrl"] as? String ?: ""
                        suggestedRoomsList.add(AIRoom(id, userId, title, price, area, district, imageUrl))
                    }
                }

                if (replyText != null) {
                    val aiMsg = AIMessage("model", replyText, System.currentTimeMillis(), suggestedRoomsList)
                    messageList.add(aiMsg)
                    adapter.notifyItemInserted(messageList.size - 1)
                    rvAiChat.scrollToPosition(messageList.size - 1)
                }
            }
            .addOnFailureListener { e ->
                btnSend.isEnabled = true
                
                val currentTypingIndex = messageList.indexOfLast { it.role == "typing" }
                if (currentTypingIndex != -1) {
                    messageList.removeAt(currentTypingIndex)
                    adapter.notifyItemRemoved(currentTypingIndex)
                }
                
                val errorMsg = e.message ?: "Lỗi không xác định"
                
                // Hiển thị thông báo thân thiện nếu gặp lỗi Quota/429
                if (errorMsg.contains("429") || errorMsg.contains("Quota") || errorMsg.contains("Too Many Requests")) {
                    val friendlyMsg = "Hệ thống AI đang quá tải do có nhiều người truy cập. Bạn vui lòng đợi 1 phút rồi thử lại nhé!"
                    Toast.makeText(this, friendlyMsg, Toast.LENGTH_LONG).show()
                    
                    // Thêm một tin nhắn của AI thông báo lỗi vào list cho người dùng dễ thấy
                    val aiErrorMsg = AIMessage("model", friendlyMsg, System.currentTimeMillis())
                    messageList.add(aiErrorMsg)
                    adapter.notifyItemInserted(messageList.size - 1)
                    rvAiChat.scrollToPosition(messageList.size - 1)
                } else {
                    Toast.makeText(this, "Lỗi kết nối AI: $errorMsg", Toast.LENGTH_LONG).show()
                }
                
                android.util.Log.e("AIChat", "Error calling AI: ", e)
            }
    }
}