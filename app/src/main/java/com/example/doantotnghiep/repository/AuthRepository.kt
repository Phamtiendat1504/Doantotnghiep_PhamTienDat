package com.example.doantotnghiep.repository

import android.net.Uri
import com.example.doantotnghiep.Model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // Đăng ký tài khoản
    fun register(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: ""

                val user = User(
                    uid = uid,
                    fullName = fullName,
                    email = email,
                    phone = phone,
                    role = "tenant", // Mặc định là người thuê
                    createdAt = System.currentTimeMillis()
                )

                db.collection("users").document(uid)
                    .set(user)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onFailure("Lưu thông tin thất bại: ${e.message}") }
            }
            .addOnFailureListener { e -> onFailure("Đăng ký thất bại: ${e.message}") }
    }

    // Đăng nhập
    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure("Đăng nhập thất bại: ${e.message}") }
    }

    // Tìm email theo số điện thoại
    fun findEmailByPhone(
        phone: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("users")
            .whereEqualTo("phone", phone)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val email = documents.documents[0].getString("email") ?: ""
                    onSuccess(email)
                } else {
                    onFailure("Số điện thoại chưa được đăng ký")
                }
            }
            .addOnFailureListener { e -> onFailure("Lỗi hệ thống: ${e.message}") }
    }

    // Đổi mật khẩu (xác thực lại bằng mật khẩu cũ rồi cập nhật)
    fun changePassword(
        oldPassword: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onWrongOldPassword: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val user = auth.currentUser ?: return onFailure("Không tìm thấy người dùng")
        val email = user.email ?: return onFailure("Không lấy được email")
        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, oldPassword)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(newPassword)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onFailure(e.message ?: "Không thể cập nhật mật khẩu mới.") }
            }
            .addOnFailureListener { onWrongOldPassword() }
    }

    // Tải thông tin cá nhân user (dạng Map)
    fun loadUserInfo(
        onSuccess: (Map<String, Any>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return onFailure("Không tìm thấy người dùng")
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) onSuccess(document.data ?: emptyMap())
                else onFailure("Không tìm thấy dữ liệu người dùng")
            }
            .addOnFailureListener { onFailure("Không thể tải thông tin người dùng từ máy chủ.") }
    }

    // Tải thông tin cá nhân user (dạng User object)
    fun loadUserObject(
        onSuccess: (User?) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return onFailure("Không tìm thấy người dùng")
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                onSuccess(if (document.exists()) document.toObject(User::class.java) else null)
            }
            .addOnFailureListener { e -> onFailure("Không thể tải thông tin người dùng: ${e.message}") }
    }

    // Kiểm tra role người dùng hiện tại
    fun getUserRole(onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onFailure("Chưa đăng nhập")
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc -> onSuccess(doc.getString("role") ?: "tenant") }
            .addOnFailureListener { e -> onFailure(e.message ?: "Lỗi") }
    }

    // Kiểm tra trạng thái xác minh chủ trọ
    fun getVerificationStatus(onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onFailure("Chưa đăng nhập")
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc -> onSuccess(doc.getString("verificationStatus") ?: "none") }
            .addOnFailureListener { e -> onFailure(e.message ?: "Lỗi") }
    }

    // Tải thông tin user theo uid bất kỳ
    fun loadUserById(uid: String, onSuccess: (User?) -> Unit, onFailure: (String) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                onSuccess(if (doc.exists()) doc.toObject(User::class.java) else null)
            }
            .addOnFailureListener { e -> onFailure(e.message ?: "Lỗi") }
    }

    // Cập nhật thông tin cá nhân
    fun updateUserInfo(
        fullName: String, email: String, phone: String,
        address: String, birthday: String, gender: String, occupation: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return onFailure("Không tìm thấy người dùng")
        val updates = mapOf(
            "fullName" to fullName, "email" to email, "phone" to phone,
            "address" to address, "birthday" to birthday,
            "gender" to gender, "occupation" to occupation
        )
        db.collection("users").document(uid)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e.message ?: "Đã có lỗi xảy ra.") }
    }

    // Xác thực lại rồi gửi email xác nhận đổi email mới
    fun reauthenticateAndUpdateEmail(
        currentEmail: String, password: String, newEmail: String,
        onSuccess: () -> Unit,
        onWrongPassword: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val user = auth.currentUser ?: return onFailure("Không tìm thấy người dùng")
        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(currentEmail, password)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.verifyBeforeUpdateEmail(newEmail)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onFailure(e.message ?: "Không thể gửi yêu cầu đổi email.") }
            }
            .addOnFailureListener { onWrongPassword() }
    }

    // Xác minh OTP rồi gửi email đặt lại mật khẩu
    fun verifyOtpAndSendResetEmail(
        verificationId: String,
        otp: String,
        email: String,
        onSuccess: () -> Unit,
        onInvalidOtp: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val credential = com.google.firebase.auth.PhoneAuthProvider.getCredential(verificationId, otp)
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                auth.signOut()
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onFailure(e.message ?: "Gửi email thất bại") }
            }
            .addOnFailureListener { onInvalidOtp() }
    }

    // Cập nhật mật khẩu (Dùng sau khi verify OTP thành công)
    fun updatePasswordAfterOtp(
        email: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        // Lưu ý: Firebase Auth yêu cầu re-authenticate nếu đổi pass mà ko có session
        // Cách tốt nhất cho "Quên mật khẩu" là dùng sendPasswordResetEmail
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure("Gửi yêu cầu đặt lại mật khẩu thất bại: ${e.message}") }
    }

    // Tải thông tin profile đầy đủ (fullName, email, avatarUrl, role, isVerified)
    fun loadUserProfile(
        onSuccess: (fullName: String, email: String, avatarUrl: String, role: String, isVerified: Boolean) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return onFailure("Chưa đăng nhập")
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    onSuccess(
                        doc.getString("fullName") ?: "Chưa cập nhật",
                        doc.getString("email") ?: "",
                        doc.getString("avatarUrl") ?: "",
                        doc.getString("role") ?: "tenant",
                        doc.getBoolean("isVerified") ?: false
                    )
                }
            }
            .addOnFailureListener { e -> onFailure(e.message ?: "Lỗi") }
    }

    // Kiểm tra trạng thái xác minh trong collection verifications
    fun loadVerificationStatusDetail(
        onSuccess: (status: String?, rejectReason: String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return onFailure("Chưa đăng nhập")
        db.collection("verifications").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    onSuccess(doc.getString("status"), doc.getString("rejectReason") ?: "")
                } else {
                    onSuccess(null, "")
                }
            }
            .addOnFailureListener { e -> onFailure(e.message ?: "Lỗi") }
    }

    // Upload ảnh đại diện lên Storage rồi cập nhật Firestore
    fun uploadAvatar(
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return onFailure("Chưa đăng nhập")
        val storageRef = storage.reference.child("avatars/$uid.jpg")
        storageRef.putFile(imageUri).continueWithTask { task ->
            if (!task.isSuccessful) task.exception?.let { throw it }
            storageRef.downloadUrl
        }.addOnSuccessListener { downloadUrl ->
            val url = downloadUrl.toString()
            db.collection("users").document(uid)
                .update("avatarUrl", url)
                .addOnSuccessListener { onSuccess(url) }
                .addOnFailureListener { e -> onFailure(e.message ?: "Lỗi cập nhật ảnh") }
        }.addOnFailureListener { e ->
            val msg = when {
                e.message?.contains("timeout", ignoreCase = true) == true -> "Upload quá lâu, vui lòng thử lại"
                e.message?.contains("network", ignoreCase = true) == true -> "Lỗi mạng, vui lòng kiểm tra kết nối"
                else -> "Lỗi: ${e.message}"
            }
            onFailure(msg)
        }
    }

    // Đếm badge bài đăng chưa xem (approved/rejected)
    fun loadMyPostsBadge(
        uid: String,
        context: android.content.Context,
        onResult: (Int) -> Unit
    ) {
        val prefs = context.getSharedPreferences("post_seen_$uid", android.content.Context.MODE_PRIVATE)
        val seenIds = prefs.getStringSet("seen_ids", emptySet()) ?: emptySet()
        db.collection("rooms")
            .whereEqualTo("userId", uid)
            .whereIn("status", listOf("approved", "rejected"))
            .get()
            .addOnSuccessListener { docs ->
                onResult(docs.count { !seenIds.contains(it.id) })
            }
    }

    // Đếm badge lịch hẹn cần xử lý
    fun loadAppointmentBadge(uid: String, onResult: (Int) -> Unit) {
        db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
            val role = userDoc.getString("role") ?: "tenant"
            val isLandlord = role == "landlord" || role == "admin"
            val field = if (isLandlord) "landlordId" else "tenantId"
            val statusFilter = if (isLandlord) "pending" else "confirmed"
            db.collection("appointments")
                .whereEqualTo(field, uid)
                .whereEqualTo("status", statusFilter)
                .get()
                .addOnSuccessListener { docs -> onResult(docs.size()) }
        }
    }
}