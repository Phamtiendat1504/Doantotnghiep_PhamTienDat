package com.example.doantotnghiep.ViewModel

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.Model.Room
import com.example.doantotnghiep.repository.RoomRepository

class PostViewModel : ViewModel() {

    private val repository = RoomRepository()

    val isLoading = MutableLiveData<Boolean>()
    val postResult = MutableLiveData<Boolean>()
    val errorMessage = MutableLiveData<String>()
    val uploadProgress = MutableLiveData<Int>()

    fun postRoom(room: Room, imageUris: List<Uri>) {
        // Validate
        if (room.ownerName.isEmpty()) { errorMessage.value = "Vui lòng nhập họ tên chủ trọ"; return }
        if (room.ownerPhone.isEmpty()) { errorMessage.value = "Vui lòng nhập số điện thoại"; return }
        if (room.ownerPhone.length != 10 || !room.ownerPhone.startsWith("0")) {
            errorMessage.value = "Số điện thoại phải có 10 số và bắt đầu bằng 0"; return
        }
        if (room.title.isEmpty()) { errorMessage.value = "Vui lòng nhập tiêu đề bài đăng"; return }
        if (room.ward.isEmpty() || room.ward == "-- Chọn phường/xã --" || room.ward == "-- Ch") {
            errorMessage.value = "Vui lòng chọn khu vực"; return
        }
        if (room.address.isEmpty()) { errorMessage.value = "Vui lòng nhập địa chỉ cụ thể"; return }
        if (room.price <= 0) { errorMessage.value = "Vui lòng nhập giá thuê"; return }
        if (room.area <= 0) { errorMessage.value = "Vui lòng nhập diện tích"; return }

        isLoading.value = true

        repository.postRoom(room, imageUris,
            onSuccess = {
                isLoading.value = false
                postResult.value = true
            },
            onFailure = { error ->
                isLoading.value = false
                errorMessage.value = error
            },
            onProgress = { progress ->
                uploadProgress.value = progress
            }
        )
    }
}