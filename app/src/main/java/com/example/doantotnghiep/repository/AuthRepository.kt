package com.example.doantotnghiep.repository

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Patterns
import com.example.doantotnghiep.Model.User
import com.example.doantotnghiep.Utils.toUser
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * AuthRepository — Xử lý toàn bộ nghiệp vụ xác thực người dùng.
 *
 * ─── BẢO MẬT TẦNG BACKEND (Firestore Security Rules) ─────────────────────────
 * Việc kiểm tra trạng thái khóa tài khoản (isLocked) trong LoginActivity và
 * AuthViewModel là kiểm tra phía Client — có thể bị bypass nếu người dùng
 * dịch ngược ứng dụng (decompile/reverse engineering).
 *
 * Để đảm bảo an toàn tuyệt đối, Firestore Security Rules PHẢI được cấu hình
 * để từ chối mọi thao tác đọc/ghi từ tài khoản đang bị khóa ở tầng Backend.
 * Ví dụ cấu hình trong Firebase Console (firestore.rules):
 *
 *   function isNotLocked() {
 *     return !get(/databases/$(database)/documents/users/$(request.auth.uid)).data.isLocked;
 *   }
 *
 *   match /rooms/{roomId} {
 *     allow create, update, delete: if request.auth != null && isNotLocked();
 *     allow read: if true;
 *   }
 *
 *   match /appointments/{docId} {
 *     allow read, write: if request.auth != null && isNotLocked();
 *   }
 *
 * Với cấu hình này, dù người dùng bị khóa vẫn không thể thực hiện bất kỳ
 * thao tác nào trên dữ liệu, kể cả khi họ đã bypass tầng kiểm tra Client.
 * ─────────────────────────────────────────────────────────────────────────────
 */
class AuthRepository {


    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    fun register(fullName: String, email: String, phone: String, password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val normalizedEmail = email.trim().lowercase()

        if (!isValidGmailEmail(normalizedEmail)) {
            onFailure("Email không hợp lệ. Vui lòng nhập đúng định dạng và kết thúc bằng @gmail.com")
            return
        }

        // Bước 1: Kiểm tra số điện thoại đã được đăng ký chưa qua phone_registry (public read)
        db.collection("phone_registry").document(phone).get()
            .addOnSuccessListener { phoneDoc ->
                if (phoneDoc.exists()) {
                    onFailure("Số điện thoại này đã được đăng ký cho tài khoản khác")
                    return@addOnSuccessListener
                }

                // Bước 2: Tạo tài khoản Firebase Auth (Auth tự chặn email trùng)
                auth.createUserWithEmailAndPassword(normalizedEmail, password)
                    .addOnSuccessListener { result ->
                        val firebaseUser = result.user ?: auth.currentUser ?: run {
                            onFailure("Đăng ký thất bại: không lấy được UID tài khoản")
                            return@addOnSuccessListener
                        }
                        val uid = firebaseUser.uid
                        val createdAt = System.currentTimeMillis()
                        // Ghi map tường minh để chắc chắn đúng tên field theo Firestore Rules.
                        val userData = hashMapOf<String, Any>(
                            "uid" to uid,
                            "fullName" to fullName,
                            "email" to normalizedEmail,
                            "phone" to phone,
                            "address" to "",
                            "birthday" to "",
                            "gender" to "",
                            "avatarUrl" to "",
                            "role" to "user",
                            "isVerified" to false,
                            "hasAcceptedRules" to false,
                            "isLocked" to false,
                            "lockReason" to "",
                            "lockUntil" to 0L,
                            "postingUnlockAt" to 0L,
                            "verifiedAt" to 0L,
                            "createdAt" to createdAt,
                            "purchasedSlots" to 0L
                        )
                        // Làm mới token trước khi ghi Firestore để giảm lỗi race-condition.
                        firebaseUser.getIdToken(true)
                            .addOnCompleteListener {
                                commitRegistrationBatch(uid, phone, userData, firebaseUser, onSuccess, onFailure)
                            }
                    }
                    .addOnFailureListener { e ->
                        val errorCode = (e as? FirebaseAuthException)?.errorCode
                        when {
                            e is FirebaseAuthUserCollisionException || errorCode == "ERROR_EMAIL_ALREADY_IN_USE" ->
                                onFailure("Email này đã được sử dụng")
                            e.message?.contains("network", ignoreCase = true) == true ||
                            e.message?.contains("offline", ignoreCase = true) == true ||
                            e.message?.contains("timeout", ignoreCase = true) == true ||
                            e.message?.contains("unreachable", ignoreCase = true) == true ->
                                onFailure("Không có kết nối mạng. Vui lòng kiểm tra lại Internet và thử lại.")
                            else ->
                                onFailure("Đăng ký thất bại. Vui lòng thử lại.")
                        }
                    }
            }
            .addOnFailureListener { e ->
                val msg = e.message ?: ""
                if (msg.contains("offline", ignoreCase = true) || msg.contains("network", ignoreCase = true) || msg.contains("timeout", ignoreCase = true)) {
                    onFailure("Không có kết nối mạng. Vui lòng kiểm tra lại Internet và thử lại.")
                } else {
                    onFailure("Lỗi kiểm tra số điện thoại: $msg")
                }
            }
    }

    private fun isValidGmailEmail(email: String): Boolean {
        if (email.any { it.code > 127 }) return false  // Chặn ký tự Unicode (gõ tiếng Việt bị lẫn vào email)
        return Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                email.endsWith("@gmail.com", ignoreCase = true)
    }

    private fun commitRegistrationBatch(
        uid: String,
        phone: String,
        userData: Map<String, Any>,
        firebaseUser: com.google.firebase.auth.FirebaseUser,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val batch = db.batch()
        batch.set(db.collection("users").document(uid), userData)
        batch.set(
            db.collection("phone_registry").document(phone),
            hashMapOf("uid" to uid, "phone" to phone)
        )
        batch.commit()
            .addOnSuccessListener {
                updateFcmToken()
                firebaseUser.sendEmailVerification()
                    .addOnCompleteListener { onSuccess() }
            }
            .addOnFailureListener { e ->
                // Rollback: xóa tài khoản Firebase Auth vừa tạo để tránh tài khoản "mồ côi"
                // (Auth tồn tại nhưng không có dữ liệu Firestore tương ứng).
                // Người dùng có thể đăng ký lại bằng cùng email sau khi gặp lỗi này.
                firebaseUser.delete().addOnCompleteListener {
                    onFailure("Đăng ký thất bại do lỗi lưu dữ liệu. Vui lòng thử lại.")
                }
            }
    }


    fun login(email: String, password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password).addOnSuccessListener {
            val user = auth.currentUser
            if (user != null && !user.isEmailVerified) {
                auth.signOut()
                onFailure("EMAIL_NOT_VERIFIED")
                return@addOnSuccessListener
            }
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
                db.collection("users").document(uid).get(com.google.firebase.firestore.Source.CACHE)
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
        db.collection("users").document(uid)
            .get(com.google.firebase.firestore.Source.SERVER)
            .addOnSuccessListener { doc ->
                onSuccess(doc.toUser())
            }.addOnFailureListener {
                // Fallback về cache nếu mất mạng
                db.collection("users").document(uid).get(com.google.firebase.firestore.Source.CACHE)
                    .addOnSuccessListener { doc ->
                        onSuccess(doc.toUser())
                    }
                    .addOnFailureListener { e -> onFailure(e.message ?: "Lỗi tải dữ liệu") }
            }
    }

    fun loadUserById(uid: String, onSuccess: (User?) -> Unit, onFailure: (String) -> Unit) {
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            onSuccess(doc.toUser())
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
        val email = user.email ?: return onFailure("Tài khoản này chưa có email để đổi mật khẩu.")
        val credential = EmailAuthProvider.getCredential(email, oldPass)
        user.reauthenticate(credential).addOnSuccessListener {
            user.updatePassword(newPass).addOnSuccessListener { onSuccess() }.addOnFailureListener { onFailure(it.message ?: "Lỗi đổi mật khẩu") }
        }.addOnFailureListener { onWrongOldPassword() }
    }

    fun reauthenticate(password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val user = auth.currentUser ?: return onFailure("Chưa đăng nhập")
        val email = user.email ?: return onFailure("Tài khoản này chưa có email để xác thực.")
        val credential = EmailAuthProvider.getCredential(email, password)
        user.reauthenticate(credential)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e.message ?: "Mật khẩu không chính xác, vui lòng kiểm tra lại.") }
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
                db.collection("verifications").document(uid).get(com.google.firebase.firestore.Source.CACHE)
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
        val ref = storage.reference.child("avatars/$uid")
        ref.delete().addOnCompleteListener {
            ref.putFile(uri).addOnSuccessListener {
                ref.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        val url = downloadUri.toString()
                        db.collection("users").document(uid).update("avatarUrl", url)
                            .addOnSuccessListener { onSuccess(url) }
                            .addOnFailureListener { onFailure(it.message ?: "Lỗi cập nhật Firestore") }
                    }
                    .addOnFailureListener { onFailure(it.message ?: "Lỗi lấy URL ảnh") }
            }.addOnFailureListener { onFailure(it.message ?: "Lỗi upload") }
        }
    }

    fun deleteAvatar(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onFailure("Chưa đăng nhập")
        db.collection("users").document(uid).update("avatarUrl", "")
            .addOnSuccessListener {
                storage.reference.child("avatars/$uid").delete()
                    .addOnCompleteListener { onSuccess() }
            }
            .addOnFailureListener { onFailure(it.message ?: "Lỗi xóa ảnh đại diện") }
    }

    // Bổ sung hàm xóa toàn bộ dữ liệu Storage của user (dùng khi xóa tài khoản)
    fun deleteUserStorage(uid: String, onComplete: () -> Unit) {
        val ref = storage.reference.child("avatars/$uid")
        ref.delete().addOnCompleteListener { onComplete() }
    }



    fun checkEmailRegistered(
        email: String,
        onExists: () -> Unit,
        onNotExists: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val normalizedEmail = email.trim().lowercase()
        // Dùng fetchSignInMethodsForEmail thay vì query Firestore vì:
        // - Firestore /users yêu cầu isSignedIn() nhưng màn hình quên MK chưa đăng nhập
        // - fetchSignInMethodsForEmail không cần xác thực, kiểm tra trực tiếp Firebase Auth
        @Suppress("DEPRECATION")
        auth.fetchSignInMethodsForEmail(normalizedEmail)
            .addOnSuccessListener { result ->
                if (result.signInMethods.isNullOrEmpty()) onNotExists() else onExists()
            }
            .addOnFailureListener { e ->
                val errorCode = (e as? FirebaseAuthException)?.errorCode
                val msg = e.message ?: ""
                when {
                    errorCode == "ERROR_INVALID_EMAIL" ->
                        onFailure("Định dạng email không hợp lệ")
                    msg.contains("network", ignoreCase = true) ||
                    msg.contains("offline", ignoreCase = true) ||
                    msg.contains("timeout", ignoreCase = true) ->
                        onFailure("Không có kết nối mạng. Vui lòng kiểm tra lại Internet.")
                    else ->
                        onFailure("Lỗi kiểm tra tài khoản. Vui lòng thử lại.")
                }
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

    private fun syncPhoneToRooms(uid: String, newPhone: String) {
        db.collection("rooms")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d("AuthRepository", "syncPhoneToRooms: found ${snapshot.size()} rooms for uid=$uid")
                if (snapshot.isEmpty) return@addOnSuccessListener
                val batch = db.batch()
                // Không sync SĐT vào bài đang chờ duyệt — admin cần thấy nội dung gốc khi duyệt
                val docsToUpdate = snapshot.documents.filter { it.getString("status") != "pending" }
                if (docsToUpdate.isEmpty()) return@addOnSuccessListener
                docsToUpdate.forEach { doc ->
                    batch.update(doc.reference, "ownerPhone", newPhone)
                }
                batch.commit()
                    .addOnSuccessListener {
                        Log.d("AuthRepository", "syncPhoneToRooms: updated ownerPhone to $newPhone in ${snapshot.size()} rooms")
                    }
                    .addOnFailureListener { e ->
                        Log.e("AuthRepository", "syncPhoneToRooms: batch commit failed: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("AuthRepository", "syncPhoneToRooms: query failed: ${e.message}")
            }
    }

    private fun syncProfileToVerification(uid: String, updates: Map<String, Any>, onComplete: () -> Unit) {
        db.collection("verifications").document(uid).get()
            .addOnSuccessListener { verifyDoc ->
                if (verifyDoc.exists() && verifyDoc.getString("status") == "pending") {
                    val verifyUpdates = hashMapOf<String, Any>()
                    if (updates.containsKey("fullName")) verifyUpdates["fullName"] = updates["fullName"]!!
                    if (updates.containsKey("phone")) verifyUpdates["phone"] = updates["phone"]!!
                    if (updates.containsKey("address")) verifyUpdates["address"] = updates["address"]!!
                    if (updates.containsKey("email")) verifyUpdates["email"] = updates["email"]!!
                    
                    if (verifyUpdates.isNotEmpty()) {
                        db.collection("verifications").document(uid).update(verifyUpdates)
                            .addOnCompleteListener { onComplete() }
                    } else {
                        onComplete()
                    }
                } else {
                    onComplete()
                }
            }
            .addOnFailureListener { onComplete() }
    }

    fun updateUserInfo(updates: Map<String, Any>, oldPhone: String, newPhone: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onFailure("Chưa đăng nhập")

        if (newPhone.isBlank()) {
            onFailure("Số điện thoại không được để trống")
            return
        }

        if (oldPhone == newPhone) {
            // Không thay đổi số điện thoại, chỉ cần cập nhật user info bình thường
            db.collection("users").document(uid).update(updates)
                .addOnSuccessListener {
                    syncProfileToVerification(uid, updates) {
                        onSuccess()
                    }
                }
                .addOnFailureListener { onFailure(it.message ?: "Lỗi cập nhật") }
            return
        }

        // Có thay đổi số điện thoại -> Kiểm tra trùng số điện thoại mới trong phone_registry
        db.collection("phone_registry").document(newPhone).get()
            .addOnSuccessListener { phoneDoc ->
                if (phoneDoc.exists()) {
                    val registeredUid = phoneDoc.getString("uid")
                    if (registeredUid != uid) {
                        onFailure("Số điện thoại này đã được đăng ký cho tài khoản khác")
                        return@addOnSuccessListener
                    }
                }

                // Số điện thoại hợp lệ -> Thực hiện cập nhật bằng batch
                val batch = db.batch()

                // 1. Cập nhật bảng users
                batch.update(db.collection("users").document(uid), updates)

                // 2. Xóa registry cũ nếu có
                if (oldPhone.isNotEmpty()) {
                    batch.delete(db.collection("phone_registry").document(oldPhone))
                }

                // 3. Tạo registry mới
                batch.set(
                    db.collection("phone_registry").document(newPhone),
                    hashMapOf("uid" to uid, "phone" to newPhone)
                )

                batch.commit()
                    .addOnSuccessListener {
                        syncPhoneToRooms(uid, newPhone)
                        syncProfileToVerification(uid, updates) {
                            onSuccess()
                        }
                    }
                    .addOnFailureListener { e ->
                        onFailure("Cập nhật thất bại: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                onFailure("Lỗi kiểm tra số điện thoại: ${e.message}")
            }
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

    fun requestAccountDeletion(
        functionUrl: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val user = auth.currentUser ?: run {
            onFailure("Bạn chưa đăng nhập.")
            return
        }

        if (functionUrl.isBlank()) {
            onFailure("Thiếu URL Cloud Function xóa tài khoản.")
            return
        }

        user.getIdToken(true)
            .addOnSuccessListener { tokenResult ->
                val idToken = tokenResult.token
                if (idToken.isNullOrBlank()) {
                    onFailure("Không lấy được token xác thực.")
                    return@addOnSuccessListener
                }

                val payload = JSONObject().apply {
                    put("uid", user.uid)
                    put("requestedAt", System.currentTimeMillis())
                }

                val requestBody = payload.toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaType())

                val request = Request.Builder()
                    .url(functionUrl)
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer $idToken")
                    .build()

                httpClient.newCall(request).enqueue(object : okhttp3.Callback {
                    override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                        postFailure(onFailure, "Không thể kết nối máy chủ: ${e.message ?: "Lỗi mạng"}")
                    }

                    override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                        response.use { res ->
                            val rawBody = res.body?.string().orEmpty()
                            if (!res.isSuccessful) {
                                postFailure(onFailure, extractServerError(rawBody, res.code))
                                return
                            }
                            mainHandler.post { onSuccess.invoke() }
                        }
                    }
                })
            }
            .addOnFailureListener { e ->
                onFailure("Không thể xác thực yêu cầu hủy tài khoản: ${e.message ?: "Lỗi"}")
            }
    }

    private fun postFailure(onFailure: (String) -> Unit, message: String) {
        mainHandler.post { onFailure.invoke(message) }
    }

    private fun extractServerError(rawBody: String, code: Int): String {
        return try {
            if (rawBody.isBlank()) return "Máy chủ từ chối yêu cầu (HTTP $code)."
            val obj = JSONObject(rawBody)
            val message = obj.optString("error", obj.optString("message", "")).trim()
            if (message.isNotEmpty()) message else "Máy chủ từ chối yêu cầu (HTTP $code)."
        } catch (_: Exception) {
            "Máy chủ từ chối yêu cầu (HTTP $code)."
        }
    }
}
