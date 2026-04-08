package com.example.doantotnghiep.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.repository.RoomRepository

class MainViewModel : ViewModel() {

    private val repository = RoomRepository()

    private val _expiredPostsCount = MutableLiveData<Int>()
    val expiredPostsCount: LiveData<Int> = _expiredPostsCount

    fun checkAndExpirePosts(uid: String) {
        repository.checkAndExpirePosts(uid) { count ->
            if (count > 0) _expiredPostsCount.value = count
        }
    }
}