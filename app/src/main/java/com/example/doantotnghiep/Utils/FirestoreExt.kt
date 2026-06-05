package com.example.doantotnghiep.Utils

import com.example.doantotnghiep.Model.User
import com.google.firebase.firestore.DocumentSnapshot

/**
 * Extension function chuyển đổi DocumentSnapshot từ Firestore thành đối tượng User.
 * Cách tiếp cận thủ công này là bắt buộc để xử lý đúng các trường Boolean bắt đầu bằng "is" (ví dụ: isVerified, isLocked)
 * do cơ chế mapping mặc định của Firestore SDK qua reflection bị lỗi với tiếp đầu ngữ "is" trong Kotlin/Java.
 */
fun DocumentSnapshot.toUser(): User? {
    if (!exists()) return null
    return User(
        uid              = id,
        fullName         = getString("fullName") ?: "",
        email            = getString("email") ?: "",
        phone            = getString("phone") ?: "",
        address          = getString("address") ?: "",
        birthday         = getString("birthday") ?: "",
        gender           = getString("gender") ?: "",
        // verificationRejectReason: chỉ dùng runtime nội bộ, không đọc từ Firestore (mặc định rỗng "")
        avatarUrl        = getString("avatarUrl") ?: "",
        role             = getString("role") ?: "user",
        isVerified       = getBoolean("isVerified") ?: false,
        hasAcceptedRules = getBoolean("hasAcceptedRules") ?: false,
        isLocked         = getBoolean("isLocked") ?: false,
        lockReason       = getString("lockReason") ?: "",
        lockUntil        = getLong("lockUntil") ?: 0L,
        postingUnlockAt  = getLong("postingUnlockAt") ?: 0L,
        verifiedAt       = getLong("verifiedAt") ?: 0L,
        createdAt        = getLong("createdAt") ?: 0L,
        purchasedSlots   = getLong("purchasedSlots")?.toInt() ?: 0,
        dailyPostCount   = getLong("dailyPostCount")?.toInt() ?: 0,
        dailyPostCountDate = getString("dailyPostCountDate") ?: "",
        lastLogin        = getLong("lastLogin") ?: 0L,
        lastDevice       = getString("lastDevice") ?: "",
        lastOsVersion    = getString("lastOsVersion") ?: "",
        bio              = getString("bio") ?: ""
    )
}
