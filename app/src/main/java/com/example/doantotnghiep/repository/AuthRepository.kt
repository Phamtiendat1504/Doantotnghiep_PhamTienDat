package com.example.doantotnghiep.repository

import android.net.Uri
import com.example.doantotnghiep.Model.User
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val client = OkHttpClient()

    fun register(fullName: String, email: String, phone: String, password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password).addOnSuccessListener { result ->
            val uid = result.user?.uid ?: ""
            val user = User(uid = uid, fullName = fullName, email = email, phone = phone, role = "tenant", createdAt = System.currentTimeMillis())
            db.collection("users").document(uid).set(user).addOnSuccessListener { onSuccess() }.addOnFailureListener { e -> onFailure("Lưu thất bại: ${e.message}") }
        }.addOnFailureListener { e -> onFailure("Đăng ký thất bại: ${e.message}") }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password).addOnSuccessListener { 
            // Cập nhật FCM Token ngay khi đăng nhập để đảm bảo nhận được thông báo kể cả khi bị khóa
            updateFcmToken()
            onSuccess() 
        }.addOnFailureListener { e -> 
            val errorCode = (e as? com.google.firebase.auth.FirebaseAuthException)?.errorCode
            val message = e.message ?: ""
            
            val errorMessage = when {
                errorCode == "ERROR_WRONG_PASSWORD" || 
                errorCode == "ERROR_USER_NOT_FOUND" ||
                message.contains("credential is incorrect", ignoreCase = true) -> 
                    "Đăng nhập thất bại do bạn nhập sai mật khẩu"
                
                errorCode == "ERROR_INVALID_EMAIL" -> 
                    "Định dạng email không hợp lệ"
                
                errorCode == "ERROR_USER_DISABLED" -> 
                    "Tài khoản này đã bị vô hiệu hóa"
                
                errorCode == "ERROR_TOO_MANY_REQUESTS" || 
                message.contains("too many requests", ignoreCase = true) -> 
                    "Quá nhiều lần thử thất bại. Vui lòng thử lại sau ít phút"

                else -> "Đăng nhập thất bại: ${e.localizedMessage}"
            }
            onFailure(errorMessage)
        }
    }

    private fun updateFcmToken() {
        val uid = auth.currentUser?.uid ?: return
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            db.collection("users").document(uid).update("fcmToken", token)
        }
    }

    fun checkUserLockStatus(onResult: (Boolean, String, Long, Int) -> Unit, onFailure: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onFailure("Chưa đăng nhập")
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val isLocked = doc.getBoolean("isLocked") ?: false
                val reason = doc.getString("lockReason") ?: "Vi phạm chính sách"
                val until = doc.getLong("lockUntil") ?: 0L
                val lockDays = doc.getLong("lockDays")?.toInt() ?: 0
                
                // Không tự động mở khóa ở đây nữa, để Server (Cloud Functions) và Web Admin lo.
                // Điều này giúp đảm bảo thông báo Realtime luôn đến từ hệ thống.
                onResult(isLocked, reason, until, lockDays)
            } else { onResult(false, "", 0L, 0) }
        }.addOnFailureListener { e -> onFailure(e.message ?: "Lỗi kiểm tra khóa") }
    }

    fun loadUserProfile(onSuccess: (String, String, String, String, Boolean) -> Unit, onFailure: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onFailure("Chưa đăng nhập")
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                onSuccess(
                    doc.getString("fullName") ?: "",
                    doc.getString("email") ?: "",
                    doc.getString("avatarUrl") ?: "",
                    doc.getString("role") ?: "tenant",
                    doc.getBoolean("isVerified") ?: false
                )
            }
        }.addOnFailureListener { onFailure(it.message ?: "Lỗi") }
    }

    fun loadUserObject(onSuccess: (User?) -> Unit, onFailure: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onSuccess(null)
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            onSuccess(doc.toObject(User::class.java))
        }.addOnFailureListener { onFailure(it.message ?: "Lỗi tải dữ liệu") }
    }

    fun loadUserById(uid: String, onSuccess: (User?) -> Unit, onFailure: (String) -> Unit) {
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            onSuccess(doc.toObject(User::class.java))
        }.addOnFailureListener { onFailure(it.message ?: "Lỗi tải dữ liệu") }
    }

    fun getUserRole(onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onSuccess("tenant")
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            onSuccess(doc.getString("role") ?: "tenant")
        }.addOnFailureListener { onFailure(it.message ?: "Lỗi") }
    }

    fun changePassword(oldPass: String, newPass: String, onSuccess: () -> Unit, onWrongOldPassword: () -> Unit, onFailure: (String) -> Unit) {
        val user = auth.currentUser ?: return onFailure("Chưa đăng nhập")
        val credential = EmailAuthProvider.getCredential(user.email!!, oldPass)
        user.reauthenticate(credential).addOnSuccessListener {
            user.updatePassword(newPass).addOnSuccessListener { onSuccess() }.addOnFailureListener { onFailure(it.message ?: "Lỗi đổi mật khẩu") }
        }.addOnFailureListener { onWrongOldPassword() }
    }

    fun loadVerificationStatusDetail(onSuccess: (String?, String?) -> Unit, onFailure: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onFailure("Chưa đăng nhập")
        db.collection("verifications").document(uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                onSuccess(doc.getString("status"), doc.getString("rejectReason"))
            } else { onSuccess(null, null) }
        }.addOnFailureListener { onFailure(it.message ?: "Lỗi") }
    }

    fun updateRulesAcceptedStatus(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onFailure("Chưa đăng nhập")
        db.collection("users").document(uid).update("hasAcceptedRules", true).addOnSuccessListener { onSuccess() }.addOnFailureListener { onFailure(it.message ?: "Lỗi") }
    }

    fun uploadAvatar(uri: Uri, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onFailure("Chưa đăng nhập")
        // Đường dẫn cố định avatars/$uid sẽ ghi đè file cũ nếu cùng tên, 
        // nhưng tốt nhất là xóa file cũ trước nếu tồn tại (để tránh lỗi cache hoặc metadata)
        val ref = storage.reference.child("avatars/$uid")
        
        ref.putFile(uri).addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { downloadUri ->
                val url = downloadUri.toString()
                db.collection("users").document(uid).update("avatarUrl", url)
                    .addOnSuccessListener { onSuccess(url) }
                    .addOnFailureListener { onFailure(it.message ?: "Lỗi cập nhật Firestore") }
            }
        }.addOnFailureListener { onFailure(it.message ?: "Lỗi upload") }
    }

    // Bổ sung hàm xóa toàn bộ dữ liệu Storage của user (dùng khi xóa tài khoản)
    fun deleteUserStorage(uid: String, onComplete: () -> Unit) {
        val ref = storage.reference.child("avatars/$uid")
        ref.delete().addOnCompleteListener { onComplete() }
    }

    fun loadMyPostsBadge(uid: String, context: android.content.Context, onResult: (Int) -> Unit) {
        db.collection("rooms").whereEqualTo("userId", uid).get().addOnSuccessListener { snap ->
            onResult(snap.size())
        }.addOnFailureListener { onResult(0) }
    }

    fun loadAppointmentBadge(uid: String, onResult: (Int) -> Unit) {
        db.collection("appointments").whereEqualTo("landlordId", uid).whereEqualTo("status", "pending").get().addOnSuccessListener { snap ->
            onResult(snap.size())
        }.addOnFailureListener { onResult(0) }
    }

    fun findEmailByPhone(phone: String, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        db.collection("users").whereEqualTo("phone", phone).limit(1).get().addOnSuccessListener { snap ->
            if (!snap.isEmpty) onSuccess(snap.documents[0].getString("email") ?: "")
            else onFailure("Không tìm thấy email với số điện thoại này")
        }.addOnFailureListener { onFailure(it.message ?: "Lỗi") }
    }

    /**
     * Cập nhật mật khẩu mới sau khi xác nhận OTP qua Cloud Function.
     * Cách này giúp vượt qua Security Rules khi người dùng chưa đăng nhập.
     */
    fun updatePasswordAfterOtp(email: String, newPass: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        // Thay url này bằng link function của bạn sau khi deploy thành công
        val url = "https://phamtriendat-doantotnghiep.cloudfunctions.net/resetPasswordAfterOtp"
        
        val json = JSONObject()
        json.put("email", email)
        json.put("newPassword", newPass)

        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onFailure("Lỗi kết nối Server: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    val errorMsg = try {
                        JSONObject(responseBody).getString("error")
                    } catch (e: Exception) { "Lỗi không xác định" }
                    onFailure(errorMsg)
                }
            }
        })
    }

    fun verifyOtpAndSendResetEmail(email: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        auth.sendPasswordResetEmail(email).addOnSuccessListener { onSuccess() }.addOnFailureListener { onFailure(it.message ?: "Lỗi") }
    }

    fun updateUserInfo(updates: Map<String, Any>, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onFailure("Chưa đăng nhập")
        db.collection("users").document(uid).update(updates).addOnSuccessListener { onSuccess() }.addOnFailureListener { onFailure(it.message ?: "Lỗi") }
    }

    fun listenUserStatus(uid: String, onStatusChange: (Boolean, String?) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        return db.collection("users").document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                if (!snapshot.exists()) {
                    // Tài khoản bị xóa
                    onStatusChange(true, "Tài khoản của bạn đã bị xóa khỏi hệ thống.")
                } else {
                    val isLocked = snapshot.getBoolean("isLocked") ?: false
                    val until = snapshot.getLong("lockUntil") ?: 0L
                    
                    // Kiểm tra nếu đã quá thời gian khóa thì tự động mở khóa
                    if (isLocked && until > 0 && System.currentTimeMillis() > until) {
                        val batch = db.batch()
                        val userRef = snapshot.reference
                        batch.update(userRef, 
                            "isLocked", false,
                            "lockReason", "",
                            "lockUntil", 0
                        )
                        
                        // Thêm bản ghi thông báo để kích hoạt FCM báo về máy
                        val notifRef = db.collection("notifications").document()
                        val notifData = hashMapOf(
                            "userId" to uid,
                            "title" to "Tài khoản đã được mở khóa",
                            "message" to "Thời gian tạm khóa của bạn đã kết thúc. Chào mừng bạn quay trở lại!",
                            "type" to "account_unlocked",
                            "seen" to false,
                            "createdAt" to System.currentTimeMillis()
                        )
                        batch.set(notifRef, notifData)
                        
                        batch.commit()
                        onStatusChange(false, null)
                        return@addSnapshotListener
                    }

                    if (isLocked) {
                        val reason = snapshot.getString("lockReason") ?: "Vi phạm chính sách"
                        
                        val sdf = java.text.SimpleDateFormat("HH:mm dd/MM/yyyy", java.util.Locale("vi", "VN"))
                        val dateStr = if (until > 0) sdf.format(java.util.Date(until)) else "Không xác định"
                        
                        val detailedMessage = "Tài khoản của bạn đang bị tạm khóa.\n\n" +
                                "• Lý do: $reason\n" +
                                "• Thời gian mở khóa dự kiến: $dateStr\n\n" +
                                "Tài khoản sẽ tự động mở khóa sau thời gian trên. Vui lòng quay lại sau."
                        
                        onStatusChange(true, detailedMessage)
                    } else {
                        onStatusChange(false, null)
                    }
                }
            }
        }
    }

    fun reauthenticateAndUpdateEmail(pass: String, newEmail: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val user = auth.currentUser ?: return onFailure("Chưa đăng nhập")
        val cred = EmailAuthProvider.getCredential(user.email!!, pass)
        user.reauthenticate(cred).addOnSuccessListener {
            user.updateEmail(newEmail).addOnSuccessListener {
                db.collection("users").document(user.uid).update("email", newEmail).addOnSuccessListener { onSuccess() }
            }.addOnFailureListener { onFailure(it.message ?: "Lỗi cập nhật email") }
        }.addOnFailureListener { onFailure("Mật khẩu không chính xác") }
    }
}
