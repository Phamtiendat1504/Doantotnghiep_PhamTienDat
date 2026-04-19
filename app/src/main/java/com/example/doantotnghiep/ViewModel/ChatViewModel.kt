package com.example.doantotnghiep.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.Model.Conversation
import com.example.doantotnghiep.Model.Message
import com.example.doantotnghiep.repository.ChatRepository
import com.google.firebase.firestore.ListenerRegistration

class ChatViewModel : ViewModel() {

    private val repository = ChatRepository()

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    private val _conversations = MutableLiveData<List<Conversation>>()
    val conversations: LiveData<List<Conversation>> = _conversations

    private val _chatId = MutableLiveData<String>()
    val chatId: LiveData<String> = _chatId

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _sendSuccess = MutableLiveData<Boolean>()
    val sendSuccess: LiveData<Boolean> = _sendSuccess

    private var messagesListener: ListenerRegistration? = null
    private var conversationsListener: ListenerRegistration? = null

    // currentChatId và receiverId dùng khi gửi tin
    var currentChatId: String = ""
    var currentMyDeletedAt: Long = 0L
    var currentReceiverId: String = ""
    var currentMyUid: String = ""

    /**
     * Mở hoặc tạo cuộc chat với người kia. Sau đó lắng nghe messages.
     */
    fun openChat(
        myUid: String,
        myName: String,
        myAvatar: String,
        otherUid: String,
        otherName: String,
        otherAvatar: String
    ) {
        _isLoading.value = true
        currentMyUid = myUid
        currentReceiverId = otherUid

        repository.getOrCreateChat(
            myUid, myName, myAvatar,
            otherUid, otherName, otherAvatar,
            onSuccess = { id, myDeletedAt ->
                currentChatId = id
                currentMyDeletedAt = myDeletedAt
                _chatId.value = id
                _isLoading.value = false
                listenMessages(id, myDeletedAt)
                repository.markMessagesAsSeen(id, myUid)
            },
            onFailure = { err ->
                _isLoading.value = false
                _errorMessage.value = err
            }
        )
    }

    /**
     * Gửi tin nhắn văn bản.
     */
    fun sendMessage(text: String) {
        if (text.isBlank() || currentChatId.isEmpty()) return
        repository.sendMessage(
            chatId     = currentChatId,
            senderId   = currentMyUid,
            receiverId = currentReceiverId,
            text       = text.trim(),
            onSuccess  = { _sendSuccess.value = true },
            onFailure  = { _errorMessage.value = it }
        )
    }

    /**
     * Lắng nghe messages real-time.
     */
    private fun listenMessages(chatId: String, myDeletedAt: Long) {
        messagesListener?.remove()
        messagesListener = repository.listenMessages(chatId, myDeletedAt) { msgs ->
            _messages.postValue(msgs)
        }
    }


    /**
     * Lắng nghe danh sách cuộc trò chuyện.
     */
    fun listenConversations(uid: String) {
        conversationsListener?.remove()
        conversationsListener = repository.listenConversations(uid) { convs ->
            _conversations.postValue(convs)
        }
    }

    /**
     * Đánh dấu đã đọc khi vào màn hình chat.
     */
    fun markSeen() {
        if (currentChatId.isNotEmpty() && currentMyUid.isNotEmpty()) {
            repository.markMessagesAsSeen(currentChatId, currentMyUid)
        }
    }

    /**
     * Xóa 1 tin nhắn. Chỉ cho phép nếu người đang đăng nhập là người gửi.
     */
    fun deleteMessage(
        message: com.example.doantotnghiep.Model.Message,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (message.senderId != currentMyUid) {
            onFailure("Bạn chỉ có thể xóa tin nhắn của chính mình")
            return
        }
        repository.deleteMessage(currentChatId, message.id, onSuccess, onFailure)
    }

    /**
     * Xóa cuộc trò chuyện chỉ phía mình (soft delete).
     */
    fun deleteConversation(
        chatId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        repository.deleteConversation(chatId, currentMyUid, onSuccess, onFailure)
    }



    /**
     * Thả / bỏ emoji reaction trên một tin nhắn.
     */
    fun addReaction(messageId: String, emoji: String) {
        if (currentChatId.isEmpty() || currentMyUid.isEmpty()) return
        repository.addReaction(currentChatId, messageId, currentMyUid, emoji)
    }

    /**
     * Gửi tin nhắn chứa hình ảnh
     */
    fun sendImageMessage(
        imageUri: android.net.Uri,
        text: String = "",
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (currentChatId.isEmpty() || currentMyUid.isEmpty() || currentReceiverId.isEmpty()) return

        // Mặc định tạo tin nhắn (văn bản trống, chưa có URL) để lưu trữ
        // nhưng thực tế ta gọi uploadChatImage rồi mới đẩy lên Firestore
        repository.uploadChatImage(
            chatId = currentChatId,
            imageUri = imageUri,
            onSuccess = { imageUrl ->
                repository.sendMessage(
                    chatId = currentChatId,
                    senderId = currentMyUid,
                    receiverId = currentReceiverId,
                    text = text,
                    imageUrl = imageUrl,
                    onSuccess = onSuccess,
                    onFailure = onFailure
                )
            },
            onFailure = onFailure
        )
    }

    override fun onCleared() {
        super.onCleared()
        messagesListener?.remove()
        conversationsListener?.remove()
    }
}
