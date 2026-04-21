package com.example.doantotnghiep.repository

import android.net.Uri
import com.example.doantotnghiep.Model.User
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    fun register(fullName: String, email: String, phone: String, password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        // Bước 1: Kiểm tra số điện thoại đã được đăng ký chưa (Firestore)
        // Firebase Auth chỉ tự check trùng email, không kiểm tra số điện thoại
        db.collection("users")
            .whereEqualTo("phone", phone)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    onFailure("Số điện thoại này đã được đăng ký cho tài khoản khác")
                    return@addOnSuccessListener
                }
                // Bước 2: Phone chưa trùng → tiến hành tạo tài khoản Firebase Auth
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { result ->
                        val uid = result.user?.uid ?: ""
                        val user = User(
                            uid = uid,
                            fullName = fullName,
                            email = email,
                            phone = phone,
                            role = "user",
                            createdAt = System.currentTimeMillis()
                        )
                        db.collection("users").document(uid).set(user)
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { e -> onFailure("Đăng ký thất bại: ${e.message}") }
                    }
                    .addOnFailureListener { e -> onFailure("Đăng ký thất bại: ${e.message}") }
            }
            .addOnFailureListener { e ->
                onFailure("Lỗi kiểm tra số điện thoại: ${e.message}")
            }
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
                message.contains("credential is incorrect", ignoreCase = true) ||
                message.contains("invalid-credential", ignoreCase = true) -> 
                    "Đăng nhập thất bại do bạn nhập sai mật khẩu hoặc email"
                
                errorCode == "ERROR_INVALID_EMAIL" -> 
                    "Định dạng email không hợp lệ"
                
                errorCode == "ERROR_USER_DISABLED" -> 
                    "Tài khoản này đã bị vô hiệu hóa"

                errorCode == "ERROR_NETWORK_REQUEST_FAILED" ->
                    "Lỗi kết nối mạng, vui lòng kiểm tra lại"
                
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
        db.collection("users").document(uid)
            .get(com.google.firebase.firestore.Source.SERVER)
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    onSuccess(
                        doc.getString("fullName") ?: "",
                        doc.getString("email") ?: "",
                        doc.getString("avatarUrl") ?: "",
                        doc.getString("role") ?: "user",
                        doc.getBoolean("isVerified") ?: false
                    )
                } else {
                    onFailure("Không tìm thấy thông tin người dùng")
                }
            }
            .addOnFailureListener {
                // Fallback cache khi mất mạng
                db.collection("users").document(uid).get()
                    .addOnSuccessListener { doc ->
                        if (doc.exists()) {
                            onSuccess(
                                doc.getString("fullName") ?: "",
                                doc.getString("email") ?: "",
                                doc.getString("avatarUrl") ?: "",
                                doc.getString("role") ?: "user",
                                doc.getBoolean("isVerified") ?: false
                            )
                        } else {
                            onFailure("Không tìm thấy thông tin người dùng")
                        }
                    }
                    .addOnFailureListener { e -> onFailure(e.message ?: "Lỗi") }
            }
    }

    fun loadUserObject(onSuccess: (User?) -> Unit, onFailure: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onSuccess(null)
        // Dùng Source.SERVER và đọc từng field thủ công thay vì toObject().
        // Lý do: Kotlin Boolean property có tiền tố "is" (ví dụ isVerified) được biên dịch
        // sang Java getter là isVerified() mà Firestore SDK nhầm là field "verified",
        // không phải "isVerified" → toObject() luôn trả isVerified = false dù Firestore lưu true.
        db.collection("users").document(uid)
            .get(com.google.firebase.firestore.Source.SERVER)
            .addOnSuccessListener { doc ->
                if (!doc.exists()) { onSuccess(null); return@addOnSuccessListener }
                val user = User(
                    uid           = doc.getString("uid") ?: uid,
                    fullName      = doc.getString("fullName") ?: "",
                    email         = doc.getString("email") ?: "",
                    phone         = doc.getString("phone") ?: "",
                    address       = doc.getString("address") ?: "",
                    birthday      = doc.getString("birthday") ?: "",
                    gender        = doc.getString("gender") ?: "",
                    occupation    = doc.getString("occupation") ?: "",
                    avatarUrl     = doc.getString("avatarUrl") ?: "",
                    role          = doc.getString("role") ?: "user",
                    isVerified    = doc.getBoolean("isVerified") ?: false,   // đọc trực tiếp theo tên field
                    hasAcceptedRules = doc.getBoolean("hasAcceptedRules") ?: false,
                    isLocked      = doc.getBoolean("isLocked") ?: false,
                    lockReason    = doc.getString("lockReason") ?: "",
                    lockUntil     = doc.getLong("lockUntil") ?: 0L,
                    createdAt     = doc.getLong("createdAt") ?: 0L
                )
                onSuccess(user)
            }.addOnFailureListener {
                // Fallback về cache nếu mất mạng
                db.collection("users").document(uid).get()
                    .addOnSuccessListener { doc ->
                        if (!doc.exists()) { onSuccess(null); return@addOnSuccessListener }
                        val user = User(
                            uid           = doc.getString("uid") ?: uid,
                            fullName      = doc.getString("fullName") ?: "",
                            email         = doc.getString("email") ?: "",
                            phone         = doc.getString("phone") ?: "",
                            address       = doc.getString("address") ?: "",
                            birthday      = doc.getString("birthday") ?: "",
                            gender        = doc.getString("gender") ?: "",
                            occupation    = doc.getString("occupation") ?: "",
                            avatarUrl     = doc.getString("avatarUrl") ?: "",
                            role          = doc.getString("role") ?: "user",
                            isVerified    = doc.getBoolean("isVerified") ?: false,
                            hasAcceptedRules = doc.getBoolean("hasAcceptedRules") ?: false,
                            isLocked      = doc.getBoolean("isLocked") ?: false,
                            lockReason    = doc.getString("lockReason") ?: "",
                            lockUntil     = doc.getLong("lockUntil") ?: 0L,
                            createdAt     = doc.getLong("createdAt") ?: 0L
                        )
                        onSuccess(user)
                    }
                    .addOnFailureListener { e -> onFailure(e.message ?: "Lỗi tải dữ liệu") }
            }
    }

    fun loadUserById(uid: String, onSuccess: (User?) -> Unit, onFailure: (String) -> Unit) {
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            if (!doc.exists()) { onSuccess(null); return@addOnSuccessListener }
            val user = User(
                uid           = doc.getString("uid") ?: uid,
                fullName      = doc.getString("fullName") ?: "",
                email         = doc.getString("email") ?: "",
                phone         = doc.getString("phone") ?: "",
                address       = doc.getString("address") ?: "",
                birthday      = doc.getString("birthday") ?: "",
                gender        = doc.getString("gender") ?: "",
                occupation    = doc.getString("occupation") ?: "",
                avatarUrl     = doc.getString("avatarUrl") ?: "",
                role          = doc.getString("role") ?: "user",
                isVerified    = doc.getBoolean("isVerified") ?: false,
                hasAcceptedRules = doc.getBoolean("hasAcceptedRules") ?: false,
                isLocked      = doc.getBoolean("isLocked") ?: false,
                lockReason    = doc.getString("lockReason") ?: "",
                lockUntil     = doc.getLong("lockUntil") ?: 0L,
                createdAt     = doc.getLong("createdAt") ?: 0L
            )
            onSuccess(user)
        }.addOnFailureListener { onFailure(it.message ?: "Lỗi tải dữ liệu") }
    }

    fun getUserRole(onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onSuccess("user")
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            val role = doc.getString("role") ?: ""
            val isVerified = doc.getBoolean("isVerified") ?: false
            val effectiveRole = if (role == "admin") "admin" else if (isVerified) "verified" else "user"
            onSuccess(effectiveRole)
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
        // Dùng Source.SERVER để tránh đọc cache cũ của collection verifications
        db.collection("verifications").document(uid)
            .get(com.google.firebase.firestore.Source.SERVER)
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    onSuccess(doc.getString("status"), doc.getString("rejectReason"))
                } else { onSuccess(null, null) }
            }.addOnFailureListener {
                // Fallback về cache nếu mất mạng
                db.collection("verifications").document(uid).get()
                    .addOnSuccessListener { doc ->
                        if (doc.exists()) onSuccess(doc.getString("status"), doc.getString("rejectReason"))
                        else onSuccess(null, null)
                    }
                    .addOnFailureListener { e -> onFailure(e.message ?: "Lỗi") }
            }
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

    /**
     * Kiểm tra xem cặp Email + Số điện thoại có tồn tại trong hệ thống không.
     * Đây là bước xác minh danh tính trước khi gửi email reset mật khẩu.
     */
    fun verifyEmailAndPhone(
        email: String,
        phone: String,
        onFound: () -> Unit,
        onNotFound: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("users")
            .whereEqualTo("email", email)
            .whereEqualTo("phone", phone)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    onFound()
                } else {
                    onNotFound()
                }
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Lỗi kết nối, vui lòng thử lại")
            }
    }

    /**
     * Gửi email đặt lại mật khẩu qua Firebase Auth.
     * Firebase sẽ gửi một đường link an toàn đến hộp thư của người dùng.
     */
    fun sendPasswordResetEmail(
        email: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e.message ?: "Gửi email thất bại, vui lòng thử lại") }
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
            user.verifyBeforeUpdateEmail(newEmail).addOnSuccessListener {
                // Không cập nhật Firestore ngay lập tức. 
                // Email trong Firestore sẽ được đồng bộ sau khi người dùng xác nhận qua email và reload tài khoản.
                onSuccess()
            }.addOnFailureListener { onFailure(it.message ?: "Lỗi gửi email xác nhận") }
        }.addOnFailureListener { onFailure("Mật khẩu không chính xác") }
    }
}
