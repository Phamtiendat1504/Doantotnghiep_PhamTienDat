package com.example.doantotnghiep.Utils

import android.content.Context
import android.content.SharedPreferences

/**
 * RateLimiter — Chống spam bằng cách lưu timestamp vào SharedPreferences (bộ nhớ cục bộ).
 *
 * Khác với việc lưu trong RAM (ViewModel), class này đảm bảo giới hạn được giữ nguyên
 * ngay cả khi người dùng tắt ứng dụng và bật lại để bypass.
 *
 * Cách dùng:
 *   val limiter = RateLimiter(context, key = "login", maxAttempts = 5, windowMs = 60_000)
 *   if (!limiter.tryConsume()) { // Hiển thị lỗi, còn X giây chờ }
 */
class RateLimiter(
    context: Context,
    private val key: String,
    private val maxAttempts: Int,
    private val windowMs: Long
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("rate_limiter_prefs", Context.MODE_PRIVATE)

    /**
     * Thử thực hiện một hành động.
     * @return true nếu được phép, false nếu đã vượt quá giới hạn.
     */
    fun tryConsume(): Boolean {
        val now = System.currentTimeMillis()
        val timestamps = loadTimestamps().filter { now - it <= windowMs }

        return if (timestamps.size >= maxAttempts) {
            // Cập nhật lại danh sách đã lọc (xóa các timestamp hết hạn)
            saveTimestamps(timestamps)
            false
        } else {
            saveTimestamps(timestamps + now)
            true
        }
    }

    /**
     * Trả về số giây cần chờ cho đến khi được phép thử lại.
     * Trả về 0 nếu không cần chờ.
     */
    fun secondsUntilNextAllowed(): Long {
        val now = System.currentTimeMillis()
        val timestamps = loadTimestamps().filter { now - it <= windowMs }
        if (timestamps.size < maxAttempts) return 0
        val oldest = timestamps.minOrNull() ?: return 0
        return ((windowMs - (now - oldest)) / 1000).coerceAtLeast(1)
    }

    /**
     * Xóa toàn bộ lịch sử (gọi khi đăng nhập/đăng ký thành công).
     */
    fun reset() {
        prefs.edit().remove(key).apply()
    }

    private fun loadTimestamps(): List<Long> {
        val raw = prefs.getString(key, "") ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split(",").mapNotNull { it.toLongOrNull() }
    }

    private fun saveTimestamps(timestamps: List<Long>) {
        prefs.edit().putString(key, timestamps.joinToString(",")).apply()
    }
}
