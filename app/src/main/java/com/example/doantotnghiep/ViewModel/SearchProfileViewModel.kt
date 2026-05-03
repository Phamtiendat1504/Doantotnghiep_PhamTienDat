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

    private var currentSearchQuery = ""

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            _users.value = emptyList()
            return
        }

        currentSearchQuery = query
        _isLoading.value = true
        userRepository.searchUsersByName(
            query = query,
            onSuccess = { rawUsers ->
                if (currentSearchQuery != query) return@searchUsersByName
                
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

                // Hiển thị danh sách ngay lập tức với số phòng = 0 để tránh UI bị đơ
                _users.value = result.toList()
                
                var remaining = rawUsers.size
                if (remaining == 0) {
                    _isLoading.value = false
                    return@searchUsersByName
                }

                rawUsers.forEachIndexed { index, user ->
                    userRepository.countPublicRooms(user.uid) { count ->
                        if (currentSearchQuery != query) return@countPublicRooms
                        
                        result[index] = result[index].copy(roomCount = count)
                        remaining--
                        
                        // Cập nhật lại UI từng bước hoặc khi tất cả hoàn thành để thấy thay đổi
                        // Update UI progressively so user sees numbers appear
                        _users.value = result.toList()

                        if (remaining == 0) {
                            _isLoading.value = false
                        }
                    }
                }
            },
            onFailure = {
                if (currentSearchQuery == query) {
                    _users.value = emptyList()
                    _isLoading.value = false
                }
            }
        )
    }
}
