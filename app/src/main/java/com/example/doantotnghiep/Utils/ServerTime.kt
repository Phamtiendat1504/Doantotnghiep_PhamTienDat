package com.example.doantotnghiep.Utils

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

/**
 * Cung cấp thời gian thực từ server Firebase, không phụ thuộc đồng hồ điện thoại.
 * Firebase SDK tự động đồng bộ serverTimeOffset khi kết nối — dù người dùng
 * chỉnh giờ máy, now() vẫn trả về thời gian thực của server.
 */
object ServerTime {

    private var offsetMs: Long = 0L

    fun init() {
        Firebase.database.getReference(".info/serverTimeOffset")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    offsetMs = snapshot.getValue(Long::class.java) ?: 0L
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    /** Thời gian hiện tại theo server (milliseconds). */
    fun now(): Long = System.currentTimeMillis() + offsetMs
}
