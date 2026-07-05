package com.example.doantotnghiep.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.text.SimpleDateFormat
import java.text.Normalizer
import java.util.Date
import java.util.Locale

class VerificationRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    companion object {
        private const val PREF_VERIFICATION_ATTEMPTS = "verification_attempts"
        private const val KEY_FAIL_DATE = "auto_fail_date"
        private const val KEY_FAIL_COUNT = "auto_fail_count"
        private const val MAX_AUTO_FAIL_BEFORE_ESCALATE = 3
        private val CCCD_FRONT_KEYWORDS = listOf(
            "CAN CUOC",
            "CAN CUOC CONG DAN",
            "SOCIALIST REPUBLIC OF VIET NAM",
            "IDENTITY CARD",
            "HO VA TEN",
            "DATE OF BIRTH",
            "GIOI TINH",
            "QUOC TICH"
        )
        private val CCCD_BACK_KEYWORDS = listOf(
            "DAC DIEM NHAN DANG",
            "NGAY CAP",
            "NOI CAP",
            "CO GIA TRI DEN"
        )
    }

    data class AutoCheckResult(
        val passed: Boolean,
        val reason: String = "",
        val recognizedCccd: String? = null,
        val failCountToday: Int = 0,
        val remainingAutoRetries: Int = MAX_AUTO_FAIL_BEFORE_ESCALATE,
        val escalatedToAdmin: Boolean = false
    )

    data class VerificationSubmitMeta(
        val autoCheckStatus: String,
        val autoCheckReason: String?,
        val autoCheckRecognizedCccd: String?,
        val autoFailCountToday: Int,
        val escalatedToAdmin: Boolean
    )

    private data class SideSignals(
        val frontScore: Int,
        val backScore: Int,
        val hasMrz: Boolean
    )

    fun loadCurrentUserInfo(
        onSuccess: (fullName: String, phone: String, email: String, address: String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return onFailure("Chưa đăng nhập")
        db.collection("users").document(uid)
            .get(com.google.firebase.firestore.Source.SERVER)
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    onSuccess(
                        doc.getString("fullName") ?: "",
                        doc.getString("phone") ?: "",
                        doc.getString("email") ?: (auth.currentUser?.email ?: ""),
                        doc.getString("address") ?: ""
                    )
                }
            }
            .addOnFailureListener { e -> onFailure(e.message ?: "Lỗi") }
    }

    // Xác minh danh tính -1-
    // Thuật toán kiểm duyệt tự động (Auto-Check) hồ sơ xác minh:
// 1. Quét OCR 2 mặt ảnh bằng ML Kit Vision.
// 2. Phân tích Tín hiệu (Signals): Xác định đúng ảnh mặt trước và mặt sau dựa vào từ khóa đặc trưng.
// 3. Kiểm duyệt Đường biên (Boundary Integrity): Ép buộc ảnh phải hiển thị đủ từ "CỘNG HÒA..." (phía trên) đến "THƯỜNG TRÚ..." (phía dưới) để chống cắt xén.
// 4. Đối chiếu (Matching): So khớp 12 số CCCD và Họ Tên trên ảnh với dữ liệu người dùng nhập.
// Lưu ý: Bất kỳ bước nào thất bại đều bị ghi nhận vào biến đếm Spam (AutoFailureCounter).
    fun runAutoCheckCccd(
        context: Context,
        fullName: String,
        enteredCccd: String,
        frontUri: Uri,
        backUri: Uri,
        onResult: (AutoCheckResult) -> Unit
    ) {
        val normalizedInput = normalizeDigits(enteredCccd)
        if (!isValidVietnameseCccd(normalizedInput)) {
            onResult(
                AutoCheckResult(
                    passed = false,
                    reason = "Số CCCD nhập vào không đúng định dạng chuẩn Việt Nam.",
                    failCountToday = currentFailCount(context),
                    remainingAutoRetries = remainingAutoRetries(context),
                    escalatedToAdmin = false
                )
            )
            return
        }

        val frontImage = try {
            InputImage.fromFilePath(context, frontUri)
        } catch (_: Exception) {
            val fail = recordAutoFailure(context)
            onResult(
                AutoCheckResult(
                    passed = false,
                    reason = "Không mở được ảnh CCCD mặt trước. Vui lòng chụp lại rõ hơn.",
                    failCountToday = fail,
                    remainingAutoRetries = remainingAutoRetriesFromCount(fail),
                    escalatedToAdmin = fail >= MAX_AUTO_FAIL_BEFORE_ESCALATE
                )
            )
            return
        }

        val backImage = try {
            InputImage.fromFilePath(context, backUri)
        } catch (_: Exception) {
            val fail = recordAutoFailure(context)
            onResult(
                AutoCheckResult(
                    passed = false,
                    reason = "Không mở được ảnh CCCD mặt sau. Vui lòng chụp lại rõ hơn.",
                    failCountToday = fail,
                    remainingAutoRetries = remainingAutoRetriesFromCount(fail),
                    escalatedToAdmin = fail >= MAX_AUTO_FAIL_BEFORE_ESCALATE
                )
            )
            return
        }

        textRecognizer.process(frontImage)
            .addOnSuccessListener { frontTextResult ->
                textRecognizer.process(backImage)
                    .addOnSuccessListener { backTextResult ->
                        val frontText = frontTextResult.text
                        val backText = backTextResult.text

                        val frontSignals = analyzeSignals(frontText)
                        val backSignals = analyzeSignals(backText)
                        val isFrontValid = isFrontSide(frontSignals)
                        val isBackValid = isBackSide(backSignals)

                        if (!isFrontValid || !isBackValid) {
                            val failCount = recordAutoFailure(context)
                            val reason = when {
                                !isFrontValid && isBackSide(frontSignals) ->
                                    "Ảnh MẶT TRƯỚC đang bị chụp nhầm thành MẶT SAU. Vui lòng chụp lại đúng thứ tự 2 mặt CCCD."
                                !isBackValid && isFrontSide(backSignals) ->
                                    "Ảnh MẶT SAU đang bị chụp nhầm thành MẶT TRƯỚC. Vui lòng chụp lại đúng thứ tự 2 mặt CCCD."
                                !isFrontValid && !isBackValid ->
                                    "Hệ thống không nhận ra đúng 2 mặt CCCD. Vui lòng chụp rõ nét và đúng khung."
                                !isFrontValid ->
                                    "Hệ thống không nhận ra đúng MẶT TRƯỚC CCCD."
                                else ->
                                    "Hệ thống không nhận ra đúng MẶT SAU CCCD."
                            }
                            onResult(
                                AutoCheckResult(
                                    passed = false,
                                    reason = reason,
                                    failCountToday = failCount,
                                    remainingAutoRetries = remainingAutoRetriesFromCount(failCount),
                                    escalatedToAdmin = failCount >= MAX_AUTO_FAIL_BEFORE_ESCALATE
                                )
                            )
                            return@addOnSuccessListener
                        }

                        val frontCandidates = extractCccdCandidates(frontText)
                        val backCandidates = extractBackCccdCandidates(backText)

                        // 1. Kiểm duyệt biên giới hạn (Boundary Integrity Check) cho mặt trước
                        val normalizedFront = normalizeNoAccentUpper(frontText)
                        val hasTopMotto = normalizedFront.contains("CONG HOA") || 
                                          normalizedFront.contains("XA HOI") || 
                                          normalizedFront.contains("DOC LAP") || 
                                          normalizedFront.contains("TU DO") || 
                                          normalizedFront.contains("SOCIALIST") || 
                                          normalizedFront.contains("REPUBLIC")
                        val hasBottomResidence = normalizedFront.contains("THUONG TRU") || 
                                                 normalizedFront.contains("CU TRU") || 
                                                 normalizedFront.contains("RESIDENCE") || 
                                                 normalizedFront.contains("GIA TRI") || 
                                                 normalizedFront.contains("EXPIRY")
                        if (!hasTopMotto || !hasBottomResidence) {
                            val failCount = recordAutoFailure(context)
                            onResult(
                                AutoCheckResult(
                                    passed = false,
                                    reason = "Ảnh mặt trước Căn cước công dân bị cắt xén hoặc không hiển thị đầy đủ tiêu đề quốc hiệu ở phía trên hoặc phần thường trú/hạn dùng ở phía dưới.",
                                    recognizedCccd = frontCandidates.firstOrNull(),
                                    failCountToday = failCount,
                                    remainingAutoRetries = remainingAutoRetriesFromCount(failCount),
                                    escalatedToAdmin = failCount >= MAX_AUTO_FAIL_BEFORE_ESCALATE
                                )
                            )
                            return@addOnSuccessListener
                        }
                        // 2. Kiểm duyệt biên giới hạn (Boundary Integrity Check) cho mặt sau
                        val normalizedBack = normalizeNoAccentUpper(backText)
                        val hasBackTop = normalizedBack.contains("NHAN DANG") || 
                                         normalizedBack.contains("IDENTIFICATION") ||
                                         normalizedBack.contains("NGAY CAP") ||
                                         normalizedBack.contains("NOI CAP") ||
                                         normalizedBack.contains("CAP NGAY")
                        val hasBackBottom = backSignals.hasMrz || 
                                            normalizedBack.contains("CUC TRUONG") || 
                                            normalizedBack.contains("DIRECTOR") || 
                                            normalizedBack.contains("CONG AN") ||
                                            normalizedBack.contains("POLICE")
                        if (!hasBackTop || !hasBackBottom) {
                            val failCount = recordAutoFailure(context)
                            onResult(
                                AutoCheckResult(
                                    passed = false,
                                    reason = "Ảnh mặt sau Căn cước công dân bị cắt xén hoặc không hiển thị đầy đủ thông tin đặc điểm nhận dạng ở phía trên hoặc chữ ký/vùng mã máy đọc MRZ ở phía dưới.",
                                    recognizedCccd = backCandidates.firstOrNull(),
                                    failCountToday = failCount,
                                    remainingAutoRetries = remainingAutoRetriesFromCount(failCount),
                                    escalatedToAdmin = failCount >= MAX_AUTO_FAIL_BEFORE_ESCALATE
                                )
                            )
                            return@addOnSuccessListener
                        }
                        val frontMatched = frontCandidates.any { it == normalizedInput }
                        val backMatched = backCandidates.any { it == normalizedInput }

                        if (!frontMatched) {
                            val failCount = recordAutoFailure(context)
                            onResult(
                                AutoCheckResult(
                                    passed = false,
                                    reason = "Số CCCD ở MẶT TRƯỚC không khớp với số bạn nhập.",
                                    recognizedCccd = frontCandidates.firstOrNull(),
                                    failCountToday = failCount,
                                    remainingAutoRetries = remainingAutoRetriesFromCount(failCount),
                                    escalatedToAdmin = failCount >= MAX_AUTO_FAIL_BEFORE_ESCALATE
                                )
                            )
                            return@addOnSuccessListener
                        }

                        if (backCandidates.isEmpty()) {
                            val failCount = recordAutoFailure(context)
                            onResult(
                                AutoCheckResult(
                                    passed = false,
                                    reason = "Hệ thống chưa đọc được số CCCD ở MẶT SAU (vùng MRZ). Vui lòng chụp lại rõ nét hơn.",
                                    failCountToday = failCount,
                                    remainingAutoRetries = remainingAutoRetriesFromCount(failCount),
                                    escalatedToAdmin = failCount >= MAX_AUTO_FAIL_BEFORE_ESCALATE
                                )
                            )
                            return@addOnSuccessListener
                        }

                        if (!backMatched) {
                            val failCount = recordAutoFailure(context)
                            onResult(
                                AutoCheckResult(
                                    passed = false,
                                    reason = "MẶT SAU CCCD không khớp với MẶT TRƯỚC hoặc số bạn nhập. Vui lòng dùng đúng 2 mặt của cùng 1 thẻ.",
                                    recognizedCccd = backCandidates.firstOrNull(),
                                    failCountToday = failCount,
                                    remainingAutoRetries = remainingAutoRetriesFromCount(failCount),
                                    escalatedToAdmin = failCount >= MAX_AUTO_FAIL_BEFORE_ESCALATE
                                )
                            )
                            return@addOnSuccessListener
                        }

                        val matched = normalizedInput
                        val expectedName = normalizeNoAccentUpper(fullName).replace(Regex("\\s+"), " ").trim()
                        val frontFullText = normalizeNoAccentUpper(frontText).replace("\n", " ").replace(Regex("\\s+"), " ").trim()
                        val nameMatched = frontFullText.contains(expectedName)

                        if (!nameMatched) {
                            val failCount = recordAutoFailure(context)
                            onResult(
                                AutoCheckResult(
                                    passed = false,
                                    reason = "Họ và tên trên thẻ không khớp với thông tin bạn nhập.",
                                    recognizedCccd = frontCandidates.firstOrNull(),
                                    failCountToday = failCount,
                                    remainingAutoRetries = remainingAutoRetriesFromCount(failCount),
                                    escalatedToAdmin = failCount >= MAX_AUTO_FAIL_BEFORE_ESCALATE
                                )
                            )
                            return@addOnSuccessListener
                        }



                        if (frontMatched && backMatched && nameMatched) {
                            resetAutoFailureCounter(context)
                            onResult(
                                AutoCheckResult(
                                    passed = true,
                                    recognizedCccd = matched,
                                    failCountToday = currentFailCount(context),
                                    remainingAutoRetries = remainingAutoRetries(context),
                                    escalatedToAdmin = false
                                )
                            )
                            return@addOnSuccessListener
                        }

                        val failCount = recordAutoFailure(context)
                        onResult(
                            AutoCheckResult(
                                passed = false,
                                reason = "Không xác định được CCCD hợp lệ từ cả 2 mặt ảnh.",
                                failCountToday = failCount,
                                remainingAutoRetries = remainingAutoRetriesFromCount(failCount),
                                escalatedToAdmin = failCount >= MAX_AUTO_FAIL_BEFORE_ESCALATE
                            )
                        )
                    }
                    .addOnFailureListener {
                        val failCount = recordAutoFailure(context)
                        onResult(
                            AutoCheckResult(
                                passed = false,
                                reason = "OCR gặp lỗi kỹ thuật khi xử lý ảnh CCCD mặt sau.",
                                failCountToday = failCount,
                                remainingAutoRetries = remainingAutoRetriesFromCount(failCount),
                                escalatedToAdmin = failCount >= MAX_AUTO_FAIL_BEFORE_ESCALATE
                            )
                        )
                    }
            }
            .addOnFailureListener {
                val failCount = recordAutoFailure(context)
                onResult(
                    AutoCheckResult(
                        passed = false,
                        reason = "OCR gặp lỗi kỹ thuật khi xử lý ảnh CCCD.",
                        failCountToday = failCount,
                        remainingAutoRetries = remainingAutoRetriesFromCount(failCount),
                        escalatedToAdmin = failCount >= MAX_AUTO_FAIL_BEFORE_ESCALATE
                    )
                )
            }
    }

    fun checkCccdExists(
        cccdNumber: String,
        currentUid: String,
        onExists: () -> Unit,
        onNotExists: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val normalizedCccd = normalizeDigits(cccdNumber)
        if (!isValidVietnameseCccd(normalizedCccd)) {
            onFailure("Số CCCD không đúng định dạng chuẩn Việt Nam.")
            return
        }

        val registryRef = db.collection("cccd_registry").document(normalizedCccd)
        val payload = hashMapOf<String, Any>(
            "uid" to currentUid,
            "cccdNumber" to normalizedCccd,
            "createdAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )
        db.runTransaction { txn ->
            val doc = txn.get(registryRef)
            if (doc.exists()) {
                val ownerUid = doc.getString("uid").orEmpty()
                if (ownerUid == currentUid) {
                    "self"
                } else {
                    "exists"
                }
            } else {
                txn.set(registryRef, payload)
                "reserved"
            }
        }.addOnSuccessListener { result ->
            if (result == "exists") {
                onExists()
            } else {
                onNotExists()
            }
        }.addOnFailureListener { e ->
            onFailure(e.message ?: "Lỗi kiểm tra CCCD")
        }
    }

    fun releaseCccd(cccdNumber: String) {
        val normalizedCccd = normalizeDigits(cccdNumber)
        if (normalizedCccd.length == 12) {
            db.collection("cccd_registry").document(normalizedCccd).delete()
        }
    }

    // Hủy hồ sơ xác minh đã quá 24h chưa được Admin duyệt.
    // Thực hiện 3 bước: Xóa bản ghi verifications, xóa CCCD khỏi cccd_registry, và xóa ảnh trên Storage.
    fun cancelPendingVerification(
        uid: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val verificationRef = db.collection("verifications").document(uid)
        verificationRef.get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    // Hồ sơ đã bị xóa trước đó -> coi như thành công
                    onSuccess()
                    return@addOnSuccessListener
                }
                val cccdNumber = doc.getString("cccdNumber")
                val oldFrontUrl = doc.getString("cccdFrontUrl")
                val oldBackUrl = doc.getString("cccdBackUrl")

                // Xóa bản ghi verifications + giải phóng cccd_registry trong 1 batch
                val batch = db.batch()
                batch.delete(verificationRef)
                if (!cccdNumber.isNullOrBlank()) {
                    val normalizedCccd = normalizeDigits(cccdNumber)
                    if (normalizedCccd.length == 12) {
                        batch.delete(db.collection("cccd_registry").document(normalizedCccd))
                    }
                }
                batch.commit()
                    .addOnSuccessListener {
                        // Sau khi xóa Firestore thành công, xóa ảnh trên Storage (best-effort)
                        if (!oldFrontUrl.isNullOrBlank()) {
                            try { storage.getReferenceFromUrl(oldFrontUrl).delete() } catch (_: Exception) {}
                        }
                        if (!oldBackUrl.isNullOrBlank()) {
                            try { storage.getReferenceFromUrl(oldBackUrl).delete() } catch (_: Exception) {}
                        }
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        onFailure(e.message ?: "Không thể hủy hồ sơ. Vui lòng thử lại sau.")
                    }
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Không thể kết nối máy chủ. Vui lòng thử lại.")
            }
    }

    private fun deleteOldVerificationImages(uid: String, onComplete: () -> Unit) {
        db.collection("verifications").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val oldFront = doc.getString("cccdFrontUrl")
                    val oldBack = doc.getString("cccdBackUrl")
                    val tasks = mutableListOf<com.google.android.gms.tasks.Task<Void>>()
                    
                    if (!oldFront.isNullOrBlank()) {
                        try {
                            val frontRef = storage.getReferenceFromUrl(oldFront)
                            tasks.add(frontRef.delete())
                        } catch (_: Exception) {}
                    }
                    if (!oldBack.isNullOrBlank()) {
                        try {
                            val backRef = storage.getReferenceFromUrl(oldBack)
                            tasks.add(backRef.delete())
                        } catch (_: Exception) {}
                    }
                    
                    if (tasks.isNotEmpty()) {
                        com.google.android.gms.tasks.Tasks.whenAllComplete(tasks)
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

    // Xác minh danh tính -2-
    // Hàm thao tác trực tiếp với Database, sử dụng kỹ thuật nối chuỗi (continueWithTask) để xử lý bất đồng bộ:
// 1. Dọn dẹp ảnh cũ (nếu user gửi lại hồ sơ).
// 2. Upload ảnh Mặt trước lên Storage -> Lấy URL.
// 3. Upload tiếp ảnh Mặt sau lên Storage -> Lấy URL.
// 4. Lưu toàn bộ Object (Text + URLs + Metadata AI) vào collection 'verifications'.
// 5. Nếu hồ sơ thuộc diện Spam (escalatedToAdmin), tự động bắn Notification về hệ thống cho Admin.
    fun submitVerification(
        fullName: String,
        email: String,
        cccd: String,
        phone: String,
        address: String,
        frontUri: Uri,
        backUri: Uri,
        meta: VerificationSubmitMeta,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return onFailure("Chưa đăng nhập")
        
        deleteOldVerificationImages(uid) {
            val storageRef = storage.reference
            val now = System.currentTimeMillis()
            val escalationDeadlineAt = now + (24L * 60L * 60L * 1000L)
            val imageMetadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()

            val frontRef = storageRef.child("verifications/$uid/cccd_front_${System.currentTimeMillis()}.jpg")
            frontRef.putFile(frontUri, imageMetadata).continueWithTask { task ->
                if (!task.isSuccessful) task.exception?.let { throw it }
                frontRef.downloadUrl
            }.continueWithTask { task ->
                val frontUrl = task.result.toString()
                val backRef = storageRef.child("verifications/$uid/cccd_back_${System.currentTimeMillis()}.jpg")
                backRef.putFile(backUri, imageMetadata).continueWithTask { backTask ->
                    if (!backTask.isSuccessful) backTask.exception?.let { throw it }
                    backRef.downloadUrl
                }.continueWithTask { backUrlTask ->
                    val backUrl = backUrlTask.result.toString()
                    val data = hashMapOf(
                        "userId" to uid,
                        "fullName" to fullName,
                        "email" to email,
                        "cccdNumber" to cccd,
                        "phone" to phone,
                        "address" to address,
                        "cccdFrontUrl" to frontUrl,
                        "cccdBackUrl" to backUrl,
                        "status" to "pending",
                        "createdAt" to now,
                        "updatedAt" to now,
                        "imageSource" to "camera_only",
                        "autoCheckStatus" to meta.autoCheckStatus,
                        "autoCheckReason" to (meta.autoCheckReason ?: ""),
                        "autoCheckRecognizedCccd" to (meta.autoCheckRecognizedCccd ?: ""),
                        "autoFailCountToday" to meta.autoFailCountToday,
                        "escalatedToAdmin" to meta.escalatedToAdmin,
                        "escalationDeadlineAt" to if (meta.escalatedToAdmin) escalationDeadlineAt else 0L
                    )

                    db.collection("verifications").document(uid).set(data).addOnSuccessListener {
                        if (meta.escalatedToAdmin) {
                            val notif = hashMapOf(
                                "userId" to "admin_system",
                                "title" to "Yêu cầu xác minh mới",
                                "message" to "Người dùng $fullName đã thất bại OCR quá 3 lần trong ngày. Hệ thống đã chuyển admin xử lý trong 24h.",
                                "type" to "new_verification",
                                "seen" to false,
                                "isRead" to false,
                                "createdAt" to now,
                                "targetId" to uid,
                                "escalatedToAdmin" to true
                            )
                            db.collection("notifications").add(notif)
                        }
                    }
                }
            }.addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e ->
                    val msg = when {
                        e.message?.contains("Permission denied", ignoreCase = true) == true ||
                            e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true ->
                            "Không đủ quyền truy cập dữ liệu (Firestore/Storage). Vui lòng kiểm tra Rules và đăng nhập lại."
                        e.message?.contains("Object does not exist", ignoreCase = true) == true ->
                            "Tệp ảnh tạm không tồn tại. Vui lòng chụp lại ảnh CCCD."
                        e.message?.contains("The operation retry limit has been exceeded", ignoreCase = true) == true ->
                            "Kết nối mạng không ổn định khi tải ảnh. Vui lòng thử lại."
                        else -> e.message ?: "Đã có lỗi xảy ra, vui lòng thử lại sau."
                    }
                    onFailure(msg)
                }
        }
    }

    private fun normalizeDigits(raw: String): String = raw.filter { it.isDigit() }

    private fun isValidVietnameseCccd(cccd: String): Boolean {
        if (cccd.length != 12 || !cccd.all { it.isDigit() }) return false
        val provinceCode = cccd.substring(0, 3).toIntOrNull() ?: return false
        if (provinceCode < 1 || provinceCode > 96) return false
        return true
    }

    private fun normalizeOcrDigits(raw: String): String {
        val upper = raw.uppercase(Locale.ROOT)
        val normalized = StringBuilder(upper.length)
        upper.forEach { ch ->
            normalized.append(
                when (ch) {
                    'O', 'Q', 'D' -> '0'
                    'I', 'L' -> '1'
                    'Z' -> '2'
                    'S' -> '5'
                    'B' -> '8'
                    else -> ch
                }
            )
        }
        return normalized.toString()
    }


    private fun normalizeNoAccentUpper(raw: String): String {
        val normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
        return normalized.uppercase(Locale.ROOT)
    }

    private fun analyzeSignals(text: String): SideSignals {
        val normalized = normalizeNoAccentUpper(text)
        return SideSignals(
            frontScore = CCCD_FRONT_KEYWORDS.count { normalized.contains(it) },
            backScore = CCCD_BACK_KEYWORDS.count { normalized.contains(it) },
            hasMrz = Regex("[A-Z0-9<]{20,}").containsMatchIn(normalized) || normalized.count { it == '<' } >= 8
        )
    }

    private fun isFrontSide(signals: SideSignals): Boolean {
        return signals.frontScore >= 2 &&
            !signals.hasMrz &&
            signals.frontScore >= signals.backScore
    }

    private fun isBackSide(signals: SideSignals): Boolean {
        val hasBackSignal = signals.backScore >= 1 || signals.hasMrz
        return hasBackSignal && (signals.hasMrz || signals.backScore >= signals.frontScore)
    }

    private fun extractCccdCandidates(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        val direct = Regex("\\b\\d{12}\\b").findAll(text).map { it.value }.toMutableList()
        val flexible = Regex("(?:\\d[\\s.\\-]*){12}")
            .findAll(text)
            .map { normalizeDigits(normalizeOcrDigits(it.value)) }
            .filter { it.length == 12 }
            .toList()
        direct.addAll(flexible)

        val compactMatches = Regex("\\d{12}").findAll(normalizeDigits(normalizeOcrDigits(text))).map { it.value }.toList()
        direct.addAll(compactMatches)

        if (direct.isNotEmpty()) return direct.distinct()

        val allDigits = normalizeDigits(normalizeOcrDigits(text))
        if (allDigits.length < 12) return emptyList()

        val windows = mutableListOf<String>()
        for (i in 0..(allDigits.length - 12)) {
            windows.add(allDigits.substring(i, i + 12))
        }
        return windows.distinct()
    }

    private fun extractBackCccdCandidates(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val normalized = normalizeOcrDigits(normalizeNoAccentUpper(text))
        val candidates = linkedSetOf<String>()
        val addAllWindows12: (String) -> Unit = { rawDigits ->
            if (rawDigits.length >= 12) {
                for (i in 0..(rawDigits.length - 12)) {
                    candidates.add(rawDigits.substring(i, i + 12))
                }
            }
        }

        Regex("\\b\\d{12}\\b").findAll(normalized).forEach { candidates.add(it.value) }
        Regex("\\d{12}").findAll(normalized).forEach { candidates.add(it.value) }

        Regex("VNM([0-9<]{10,})").findAll(normalized).forEach { match ->
            val digits = match.groupValues[1].replace("<", "").filter { it.isDigit() }
            addAllWindows12(digits)
        }

        Regex("IDVNM([0-9<]{8,})").findAll(normalized).forEach { match ->
            val digits = match.groupValues[1].replace("<", "").filter { it.isDigit() }
            addAllWindows12(digits)
        }

        val allDigits = normalized.filter { it.isDigit() }
        addAllWindows12(allDigits)
        return candidates.toList()
    }

    private fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private fun currentFailCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_VERIFICATION_ATTEMPTS, Context.MODE_PRIVATE)
        val date = prefs.getString(KEY_FAIL_DATE, "") ?: ""
        val count = prefs.getInt(KEY_FAIL_COUNT, 0)
        return if (date == todayKey()) count else 0
    }

    private fun remainingAutoRetries(context: Context): Int =
        remainingAutoRetriesFromCount(currentFailCount(context))

    private fun remainingAutoRetriesFromCount(failCount: Int): Int =
        (MAX_AUTO_FAIL_BEFORE_ESCALATE - failCount).coerceAtLeast(0)

    // Xác minh danh tính-1-
    // Bộ đếm (Counter) theo dõi số lần user nhập sai CCCD.
// Mỗi khi quét ảnh lỗi hoặc text không khớp, hàm này tăng biến đếm lên 1 đơn vị.
// Nó lưu cục bộ (Local) cho app chạy mượt, và lưu cả lên Đám mây (Cloud) để chống người dùng gian lận xóa App cài lại.
    private fun recordAutoFailure(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_VERIFICATION_ATTEMPTS, Context.MODE_PRIVATE)
        val today = todayKey()
        val oldDate = prefs.getString(KEY_FAIL_DATE, "") ?: ""
        val oldCount = prefs.getInt(KEY_FAIL_COUNT, 0)
        val next = if (oldDate == today) oldCount + 1 else 1
        prefs.edit()
            .putString(KEY_FAIL_DATE, today)
            .putInt(KEY_FAIL_COUNT, next)
            .apply()
        // Persist counter to Firestore so clearing app data cannot reset it
        auth.currentUser?.uid?.let { uid ->
            db.collection("verification_counters").document(uid)
                .set(mapOf("failDate" to today, "failCount" to next))
        }
        return next
    }

    fun resetAutoFailureCounter(context: Context) {
        val prefs = context.getSharedPreferences(PREF_VERIFICATION_ATTEMPTS, Context.MODE_PRIVATE)
        val today = todayKey()
        prefs.edit()
            .putString(KEY_FAIL_DATE, today)
            .putInt(KEY_FAIL_COUNT, 0)
            .apply()
        auth.currentUser?.uid?.let { uid ->
            db.collection("verification_counters").document(uid)
                .set(mapOf("failDate" to today, "failCount" to 0))
        }
    }

    // Xác minh danh tính -4-
    // Cơ chế đồng bộ hóa Anti-Spam:
// Trước khi cho phép User bấm "Xác minh", app sẽ gọi lên Server lấy số lần đã lỗi trong ngày hôm nay.
// Nếu số lần trên Server lớn hơn số lần đang lưu trên Máy (do user vừa xóa data app),
// hệ thống lập tức khôi phục biến đếm trên máy bằng với Server.
    fun syncCounterFromServer(context: Context, uid: String, onDone: () -> Unit) {
        db.collection("verification_counters").document(uid).get(com.google.firebase.firestore.Source.SERVER)
            .addOnSuccessListener { snap ->
                if (snap.exists()) {
                    val serverDate = snap.getString("failDate") ?: ""
                    val serverCount = (snap.getLong("failCount") ?: 0L).toInt()
                    val today = todayKey()
                    if (serverDate == today && serverCount > 0) {
                        val prefs = context.getSharedPreferences(PREF_VERIFICATION_ATTEMPTS, Context.MODE_PRIVATE)
                        val localCount = if (prefs.getString(KEY_FAIL_DATE, "") == today) prefs.getInt(KEY_FAIL_COUNT, 0) else 0
                        if (serverCount > localCount) {
                            prefs.edit()
                                .putString(KEY_FAIL_DATE, today)
                                .putInt(KEY_FAIL_COUNT, serverCount)
                                .apply()
                        }
                    }
                }
                onDone()
            }
            .addOnFailureListener { onDone() }
    }
}

