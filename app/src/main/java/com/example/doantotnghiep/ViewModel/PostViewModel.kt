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
        // Validate dữ liệu đầu vào
        if (room.ownerName.isBlank()) { errorMessage.value = "Vui lòng nhập họ tên chủ trọ"; return }
        if (room.ownerPhone.isBlank()) { errorMessage.value = "Vui lòng nhập số điện thoại"; return }
        if (room.ownerPhone.length < 10) {
            errorMessage.value = "Số điện thoại không hợp lệ"; return
        }
        if (room.title.isBlank()) { errorMessage.value = "Vui lòng nhập tiêu đề bài đăng"; return }
        
        // Sửa lỗi so sánh chuỗi bị cắt cụt
        if (room.ward.isBlank() || room.ward.contains("Chọn phường/xã")) {
            errorMessage.value = "Vui lòng chọn khu vực (Phường/Xã)"; return
        }
        
        if (room.address.isBlank()) { errorMessage.value = "Vui lòng nhập địa chỉ cụ thể"; return }
        if (room.price <= 0) { errorMessage.value = "Vui lòng nhập giá thuê hợp lệ"; return }
        if (room.area <= 0) { errorMessage.value = "Vui lòng nhập diện tích hợp lệ"; return }
        
        if (imageUris.isEmpty()) {
            errorMessage.value = "Vui lòng thêm ít nhất 1 ảnh phòng trọ"; return
        }

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
