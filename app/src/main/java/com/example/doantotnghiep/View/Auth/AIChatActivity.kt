package com.example.doantotnghiep.View.Auth

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.Intent
import android.net.Uri
import com.example.doantotnghiep.AI.ChatbotEngine
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
import java.util.Date

class AIChatActivity : AppCompatActivity() {

    private lateinit var rvAiChat: RecyclerView
    private lateinit var edtMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnDeleteChat: ImageButton
    private lateinit var progressBar: ProgressBar

    private val messageList = mutableListOf<AIMessage>()
    private lateinit var adapter: AIChatAdapter
    private lateinit var chatbotEngine: ChatbotEngine

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var currentContext = ChatbotEngine.ConversationContext()

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

        adapter = AIChatAdapter(messageList, object : AIChatAdapter.QuickReplyClickListener {
            override fun onQuickReplyClicked(text: String) {
                handleQuickReply(text)
            }
        })
        rvAiChat.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvAiChat.adapter = adapter

        chatbotEngine = ChatbotEngine(this).also { it.initialize() }

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
                    currentContext = ChatbotEngine.ConversationContext()
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

        val now = System.currentTimeMillis()
        val userMsg = AIMessage("user", messageText, now)
        messageList.add(userMsg)
        adapter.notifyItemInserted(messageList.size - 1)
        rvAiChat.scrollToPosition(messageList.size - 1)
        edtMessage.setText("")

        val typingMsg = AIMessage("typing", "...", now + 1)
        messageList.add(typingMsg)
        val typingIndex = messageList.size - 1
        adapter.notifyItemInserted(typingIndex)
        rvAiChat.scrollToPosition(typingIndex)

        btnSend.isEnabled = false

        val userMsgData = hashMapOf(
            "role" to "user",
            "content" to messageText,
            "timestamp" to Date(now)
        )
        db.collection("users").document(uid).collection("ai_conversations")
            .add(userMsgData)

        Handler(Looper.getMainLooper()).postDelayed({
            btnSend.isEnabled = true

            val typingIdx = messageList.indexOfLast { it.role == "typing" }
            if (typingIdx != -1) {
                messageList.removeAt(typingIdx)
                adapter.notifyItemRemoved(typingIdx)
            }

            val response = chatbotEngine.processMessage(messageText, currentContext)
            currentContext = response.nextContext

            val replyTime = System.currentTimeMillis()
            val aiMsg = AIMessage(
                "model",
                response.answer,
                replyTime,
                quickReplies = response.quickReplies
            )
            messageList.add(aiMsg)
            adapter.notifyItemInserted(messageList.size - 1)
            rvAiChat.scrollToPosition(messageList.size - 1)

            if (!response.deepLink.isNullOrEmpty()) {
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(response.deepLink)))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, 400)
            }

            if (response.searchParams != null) {
                searchRoomsFromFirebase(response.searchParams, uid, replyTime)
            } else {
                val aiMsgData = hashMapOf(
                    "role" to "model",
                    "content" to response.answer,
                    "timestamp" to Date(replyTime)
                )
                db.collection("users").document(uid).collection("ai_conversations")
                    .add(aiMsgData)
            }
        }, 800)
    }

    private fun searchRoomsFromFirebase(
        params: ChatbotEngine.SearchParams,
        uid: String,
        searchTimestamp: Long
    ) {
        db.collection("rooms")
            .whereEqualTo("status", "approved")
            .limit(50)
            .get()
            .addOnSuccessListener { snapshots ->
                val filtered = snapshots.documents.mapNotNull { doc ->
                    val docDistrict = doc.getString("district") ?: ""
                    val docPrice = doc.getLong("price") ?: 0L

                    val districtOk = params.district == null ||
                        docDistrict.contains(params.district, ignoreCase = true)
                    val priceOk = params.maxPrice == null || docPrice <= params.maxPrice

                    if (districtOk && priceOk) {
                        val imageUrl = (doc.get("imageUrls") as? List<*>)
                            ?.filterIsInstance<String>()
                            ?.firstOrNull() ?: ""
                        AIRoom(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            title = doc.getString("title") ?: "",
                            price = docPrice,
                            area = (doc.getLong("area") ?: 0L).toInt(),
                            district = docDistrict,
                            imageUrl = imageUrl
                        )
                    } else null
                }.take(10)

                val replyTime = System.currentTimeMillis()
                val resultContent: String
                val quickReplies: List<String>

                if (filtered.isEmpty()) {
                    resultContent = buildString {
                        append("Mình không tìm thấy phòng nào phù hợp")
                        if (params.district != null) append(" ở **${params.district}**")
                        if (params.maxPrice != null) append(", giá dưới ${formatPrice(params.maxPrice)}")
                        append(".\n\nBạn thử điều chỉnh lại khu vực hoặc mức giá nhé!")
                    }
                    quickReplies = listOf("🔍 Tìm lại", "Đổi khu vực", "Đổi mức giá")
                } else {
                    resultContent = buildString {
                        append("Mình tìm được **${filtered.size} phòng** phù hợp")
                        if (params.district != null) append(" ở **${params.district}**")
                        if (params.maxPrice != null) append(", giá dưới ${formatPrice(params.maxPrice)}")
                        append(":")
                    }
                    quickReplies = listOf("🔍 Tìm lại", "Đổi khu vực", "Đổi mức giá")
                }

                val resultMsg = AIMessage(
                    role = "model",
                    content = resultContent,
                    timestamp = replyTime,
                    suggestedRooms = filtered,
                    quickReplies = quickReplies
                )
                messageList.add(resultMsg)
                adapter.notifyItemInserted(messageList.size - 1)
                rvAiChat.scrollToPosition(messageList.size - 1)

                val roomsData = filtered.map { room ->
                    mapOf(
                        "id" to room.id,
                        "userId" to room.userId,
                        "title" to room.title,
                        "price" to room.price,
                        "area" to room.area,
                        "district" to room.district,
                        "imageUrl" to room.imageUrl
                    )
                }

                val aiMsgData = hashMapOf(
                    "role" to "model",
                    "content" to resultContent,
                    "timestamp" to Date(replyTime),
                    "suggestedRooms" to roomsData
                )
                db.collection("users").document(uid).collection("ai_conversations")
                    .add(aiMsgData)
            }
            .addOnFailureListener { e ->
                val replyTime = System.currentTimeMillis()
                val errorMsg = AIMessage(
                    "model",
                    "Có lỗi khi tìm kiếm phòng. Vui lòng thử lại sau.",
                    replyTime,
                    quickReplies = listOf("🔍 Tìm lại")
                )
                messageList.add(errorMsg)
                adapter.notifyItemInserted(messageList.size - 1)
                rvAiChat.scrollToPosition(messageList.size - 1)
                android.util.Log.e("AIChatActivity", "Room search failed", e)
            }
    }

    private fun formatPrice(price: Long): String {
        val millions = price / 1_000_000.0
        return if (millions >= 1.0) {
            val s = if (millions == millions.toLong().toDouble()) millions.toLong().toString()
                    else String.format("%.1f", millions)
            "$s triệu"
        } else {
            "${price / 1000}K"
        }
    }

    private fun handleQuickReply(displayText: String) {
        val query = ChatbotEngine.QUICK_REPLY_ACTIONS[displayText] ?: displayText
        edtMessage.setText(query)
        sendMessageToAI(query)
    }
}
