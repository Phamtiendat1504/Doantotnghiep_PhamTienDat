package com.example.doantotnghiep.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.repository.AppointmentRepository
import com.example.doantotnghiep.repository.RoomRepository
import com.google.firebase.firestore.ListenerRegistration

class MainViewModel : ViewModel() {

    private val roomRepository = RoomRepository()
    private val appointmentRepository = AppointmentRepository()
    private var appointmentBadgeListener: ListenerRegistration? = null

    private val _expiredPostsCount = MutableLiveData<Int>()
    val expiredPostsCount: LiveData<Int> = _expiredPostsCount

    private val _appointmentBadgeCount = MutableLiveData<Int>()
    val appointmentBadgeCount: LiveData<Int> = _appointmentBadgeCount

    fun checkAndExpirePosts(uid: String) {
        roomRepository.checkAndExpirePosts(uid) { count ->
            if (count > 0) _expiredPostsCount.value = count
        }
    }

    fun loadAppointmentBadge(uid: String, role: String) {
        appointmentBadgeListener?.remove()
        appointmentBadgeListener = appointmentRepository.listenBadge(uid, role) { count ->
            _appointmentBadgeCount.postValue(count)
        }
    }

    override fun onCleared() {
        super.onCleared()
        appointmentBadgeListener?.remove()
    }
}
