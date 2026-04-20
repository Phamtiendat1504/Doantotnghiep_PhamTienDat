package com.example.doantotnghiep.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.View.Adapter.UserSearchItem
import com.example.doantotnghiep.repository.UserRepository

class SearchProfileViewModel : ViewModel() {

    private val userRepository = UserRepository()

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _users = MutableLiveData<List<UserSearchItem>>(emptyList())
    val users: LiveData<List<UserSearchItem>> = _users

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            _users.value = emptyList()
            return
        }

        _isLoading.value = true
        userRepository.searchUsersByName(
            query = query,
            onSuccess = { rawUsers ->
                if (rawUsers.isEmpty()) {
                    _users.value = emptyList()
                    _isLoading.value = false
                    return@searchUsersByName
                }

                val result = MutableList(rawUsers.size) { index ->
                    val u = rawUsers[index]
                    UserSearchItem(
                        uid = u.uid,
                        fullName = u.fullName,
                        avatarUrl = u.avatarUrl,
                        roomCount = 0,
                        isVerified = u.isVerified
                    )
                }

                var remaining = rawUsers.size
                rawUsers.forEachIndexed { index, user ->
                    userRepository.countApprovedRooms(user.uid) { count ->
                        result[index] = result[index].copy(roomCount = count)
                        remaining--
                        if (remaining == 0) {
                            _users.value = result
                            _isLoading.value = false
                        }
                    }
                }
            },
            onFailure = {
                _users.value = emptyList()
                _isLoading.value = false
            }
        )
    }
}
