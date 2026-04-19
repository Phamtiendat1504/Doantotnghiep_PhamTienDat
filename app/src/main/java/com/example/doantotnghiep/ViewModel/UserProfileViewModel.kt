package com.example.doantotnghiep.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.Model.User
import com.example.doantotnghiep.View.Adapter.RoomItem
import com.example.doantotnghiep.repository.RoomRepository
import com.example.doantotnghiep.repository.UserRepository

class UserProfileViewModel : ViewModel() {

    private val userRepo = UserRepository()
    private val roomRepo = RoomRepository()

    private val _userInfo = MutableLiveData<User>()
    val userInfo: LiveData<User> = _userInfo

    private val _rooms = MutableLiveData<List<RoomItem>>()
    val rooms: LiveData<List<RoomItem>> = _rooms

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun loadProfile(userId: String) {
        _isLoading.value = true

        // Tải thông tin user
        userRepo.getUserById(
            uid = userId,
            onSuccess = { user -> _userInfo.value = user },
            onFailure = { msg -> _errorMessage.value = msg }
        )

        // Tải danh sách bài đăng đã approved của user đó
        roomRepo.loadOwnerRooms(
            userId = userId,
            onSuccess = { docs ->
                val items = docs.mapNotNull { doc ->
                    val id = doc.id
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val price = doc.getLong("price") ?: 0L
                    val ward = doc.getString("ward") ?: ""
                    val district = doc.getString("district") ?: ""
                    val area = doc.getLong("area")?.toInt() ?: 0
                    val imageUrl = (doc.get("imageUrls") as? List<*>)?.filterIsInstance<String>()?.firstOrNull()
                    val status = doc.getString("status") ?: "approved"
                    RoomItem(id, title, price, ward, district, area, imageUrl, status)
                }
                _rooms.value = items
                _isLoading.value = false
            },
            onFailure = { msg ->
                _errorMessage.value = msg
                _isLoading.value = false
            }
        )
    }
}
