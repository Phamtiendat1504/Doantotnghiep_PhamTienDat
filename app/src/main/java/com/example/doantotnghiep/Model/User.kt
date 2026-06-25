package com.example.doantotnghiep.Model

data class User(
    var uid: String = "",
    var fullName: String = "",
    var email: String = "",
    var phone: String = "",
    var address: String = "",
    var birthday: String = "",
    var gender: String = "",
    var verificationRejectReason: String = "", // Lý do Admin từ chối duyệt tài khoản KYC (chỉ dùng nội bộ, không lưu Firestore)
    var avatarUrl: String = "",
    @set:Deprecated("role is managed server-side; only mutate through UserRepository")
    var role: String = "user",
    // Firestore yêu cầu var (không phải val) để có thể deserialize đúng qua reflection.
    // Nếu dùng val, Firestore sẽ bỏ qua các field Boolean và lúc nào cũng trả về giá trị default (false).
    @set:Deprecated("isVerified is managed server-side; only mutate through UserRepository")
    var isVerified: Boolean = false,
    var hasAcceptedRules: Boolean = false,
    var isLocked: Boolean = false,
    var lockReason: String = "",
    var lockUntil: Long = 0,
    var postingUnlockAt: Long = 0,
    var verifiedAt: Long = 0,
    var createdAt: Long = 0,
    var purchasedSlots: Int = 0,
    var dailyPostCount: Int = 0,
    var dailyPostCountDate: String = "",
    var lastLogin: Long = 0,
    var lastDevice: String = "",
    var lastOsVersion: String = "",
    var bio: String = "",
    var noShowCount: Int = 0           // Số lần không đến đúng hẹn (cộng dồn)
)
