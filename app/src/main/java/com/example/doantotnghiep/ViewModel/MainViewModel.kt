package com.example.doantotnghiep.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.repository.AppointmentRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MainViewModel : ViewModel() {

    private val appointmentRepository = AppointmentRepository()
    private var appointmentBadgeListener: ListenerRegistration? = null

    private val _appointmentBadgeCount = MutableLiveData<Int>()
    val appointmentBadgeCount: LiveData<Int> = _appointmentBadgeCount

    fun loadAppointmentBadge(uid: String, role: String) {
        appointmentBadgeListener?.remove()
        appointmentBadgeListener = appointmentRepository.listenBadge(uid, role) { count ->
            _appointmentBadgeCount.postValue(count)
        }
    }

    fun loadAppointmentBadgeForCurrentUser(uid: String) {
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role") ?: ""
                val isVerified = doc.getBoolean("isVerified") ?: false
                val effectiveRole = if (role == "admin") "admin" else if (isVerified) "verified" else "user"
                loadAppointmentBadge(uid, effectiveRole)
            }
    }

    override fun onCleared() {
        super.onCleared()
        appointmentBadgeListener?.remove()
    }
}
