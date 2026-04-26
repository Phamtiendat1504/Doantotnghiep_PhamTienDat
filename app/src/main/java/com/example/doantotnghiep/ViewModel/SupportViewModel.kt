package com.example.doantotnghiep.ViewModel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.Model.Message
import com.example.doantotnghiep.Model.SupportTicket
import com.example.doantotnghiep.repository.SupportRepository
import com.google.firebase.firestore.ListenerRegistration

class SupportViewModel : ViewModel() {

    private val repository = SupportRepository()

    private val _tickets = MutableLiveData<List<SupportTicket>>(emptyList())
    val tickets: LiveData<List<SupportTicket>> = _tickets

    private val _messages = MutableLiveData<List<Message>>(emptyList())
    val messages: LiveData<List<Message>> = _messages

    private val _createdTicketId = MutableLiveData<String>()
    val createdTicketId: LiveData<String> = _createdTicketId

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private var ticketListener: ListenerRegistration? = null
    private var messageListener: ListenerRegistration? = null

    fun listenMyTickets() {
        ticketListener?.remove()
        ticketListener = repository.listenMyTickets(
            onUpdate = { _tickets.postValue(it) },
            onError = { _errorMessage.postValue(it) }
        )
    }

    fun listenMessages(ticketId: String) {
        messageListener?.remove()
        messageListener = repository.listenMessages(
            ticketId,
            onUpdate = { _messages.postValue(it) },
            onError = { _errorMessage.postValue(it) }
        )
        repository.markUserRead(ticketId)
    }

    fun markUserRead(ticketId: String) {
        repository.markUserRead(ticketId)
    }

    fun createTicket(
        category: String,
        title: String,
        text: String,
        imageUri: Uri?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        repository.createTicket(
            category,
            title,
            text,
            imageUri,
            onSuccess = {
                _createdTicketId.postValue(it)
                onSuccess()
            },
            onFailure = {
                _errorMessage.postValue(it)
                onFailure(it)
            }
        )
    }

    fun sendMessage(
        ticketId: String,
        text: String,
        imageUri: Uri?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        repository.sendUserMessage(
            ticketId,
            text,
            imageUri,
            onSuccess,
            onFailure = {
                _errorMessage.postValue(it)
                onFailure(it)
            }
        )
    }

    fun closeTicket(ticketId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        repository.closeTicket(
            ticketId,
            onSuccess,
            onFailure = {
                _errorMessage.postValue(it)
                onFailure(it)
            }
        )
    }

    override fun onCleared() {
        ticketListener?.remove()
        messageListener?.remove()
        super.onCleared()
    }
}
