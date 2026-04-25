# Đồ án Tốt nghiệp: Tìm Trọ 24/7 (Android + Web Admin + Firebase)

Tài liệu này viết cho người mở dự án lần đầu (giảng viên, phản biện, thành viên mới) để nắm nhanh toàn bộ hệ thống:
- App Android có chức năng gì.
- Web Admin làm gì.
- Firebase/GCP được cấu hình ra sao.
- Luồng xử lý dữ liệu từ đầu đến cuối cho từng nghiệp vụ.
- Chức năng nào nằm ở file Kotlin/XML nào, file web nào, Cloud Function nào.

Lưu ý thời điểm: phần cấu hình Firebase Console trong tài liệu này được tổng hợp theo trạng thái bạn chụp ngày **24/04/2026**.

---

## 1) Bức tranh tổng thể hệ thống

Hệ thống gồm 3 khối chạy cùng một backend Firebase:

1. **App Android (repo này)**
- Người dùng tìm phòng, đăng bài, xác minh CCCD, đặt lịch, chat, nhận thông báo.
- Công nghệ chính: Kotlin, XML, MVVM, Firebase SDK.

2. **Web Admin**
- Đường dẫn local: `C:\Users\tiend\Admin_TimTro_New`
- Gồm:
  - `public/index.html`, `public/app.js`: giao diện và logic quản trị.
  - `phamtriendat_doantotnghiep/index.js`: Cloud Functions backend.
- Quản trị các nghiệp vụ: duyệt bài, duyệt xác minh, khóa user, xóa user, theo dõi trạng thái online/offline, thống kê.

3. **Firebase/GCP**
- Auth, Firestore, Storage, FCM, Cloud Functions.
- GCP APIs: Maps SDK for Android, Geocoding API, Places API (New), Cloud Vision API.

Sơ đồ luồng:

```text
Android App (Kotlin/XML)            Web Admin (HTML/CSS/JS)
           \                          /
            \                        /
             ---> Firebase Backend <---
                  - Auth
                  - Firestore
                  - Storage
                  - Cloud Functions
                  - FCM
                  - Security Rules
```

---

## 2) Kiến trúc và convention trong app Android

### 2.1 Kiến trúc
- Pattern: **MVVM**.
- UI layer: `Activity/Fragment + XML + ViewBinding`.
- ViewModel layer: validate dữ liệu, điều phối nghiệp vụ.
- Repository layer: làm việc trực tiếp với Firebase.

Luồng chuẩn:

```text
View (Activity/Fragment)
  -> ViewModel
    -> Repository
      -> Firebase
    <- dữ liệu/trạng thái
  <- render UI
```

### 2.2 Công nghệ chính (theo `app/build.gradle.kts`)
- Android: `compileSdk 35`, `targetSdk 35`, `minSdk 24`.
- Kotlin + JVM 11.
- Firebase BOM `33.1.2`:
  - Auth, Firestore, Storage, Messaging.
- Camera/OCR/Maps:
  - CameraX `1.3.4`
  - ML Kit Text Recognition `16.0.1`
  - Google Maps SDK `18.2.0`
- Media & network:
  - Glide `4.16.0`
  - OkHttp `4.12.0`
- Security:
  - `androidx.security:security-crypto`.

### 2.3 Cấu trúc mã nguồn app

```text
app/src/main/java/com/example/doantotnghiep
  |- Model/
  |- View/
  |   |- Auth/
  |   |- Fragment/
  |   |- Adapter/
  |- ViewModel/
  |- repository/
  |- Utils/
```

---

## 3) Mapping chức năng chính: Android XML + Kotlin

## 3.1 Điều hướng và tài khoản

| Chức năng | XML | Kotlin |
|---|---|---|
| Splash | `activity_splash.xml` | `View/Auth/SplashActivity.kt` |
| Welcome sau login | `activity_welcome_user.xml` | `View/Auth/WelcomeActivity.kt` |
| Main + Bottom Nav | `activity_main.xml` | `MainActivity.kt` |
| Đăng nhập | `activity_login.xml` | `View/Auth/LoginActivity.kt` |
| Đăng ký | `activity_register.xml` | `View/Auth/RegisterActivity.kt` |
| Quên mật khẩu | `activity_forgot_password.xml` | `View/Auth/ForgotPasswordActivity.kt` |
| Profile tab | `fragment_profile.xml` | `View/Fragment/ProfileFragment.kt` |

## 3.2 Tìm phòng / đăng bài / bản đồ

| Chức năng | XML | Kotlin |
|---|---|---|
| Home | `fragment_home.xml` | `View/Fragment/HomeFragment.kt` |
| Search | `fragment_search.xml` | `View/Fragment/SearchFragment.kt` |
| Đăng bài | `fragment_post.xml` | `View/Fragment/PostFragment.kt` |
| Khung người chưa xác minh | `layout_verify_required.xml` | `View/Fragment/PostFragment.kt` |
| Dialog hết lượt đăng | `dialog_post_quota_limit.xml` | `View/Fragment/PostFragment.kt` |
| Dialog mua gói lượt | `dialog_upgrade_slots.xml` | `View/Fragment/PostFragment.kt` |
| Dialog QR thanh toán | `dialog_payment_qr.xml` | `View/Fragment/PostFragment.kt` |
| Chọn vị trí trên map | `activity_location_picker.xml` | `View/Auth/LocationPickerActivity.kt` |
| Chi tiết phòng | `activity_room_detail.xml` | `View/Auth/RoomDetailActivity.kt` |

## 3.3 Xác minh CCCD / lịch hẹn / chat

| Chức năng | XML | Kotlin |
|---|---|---|
| Form xác minh CCCD | `activity_verify_landlord.xml` | `View/Auth/VerifyLandlordActivity.kt` |
| Camera CCCD custom | `activity_cccd_camera.xml` | `View/Auth/CccdCameraActivity.kt` |
| Đặt lịch hẹn | `activity_booking.xml` | `View/Auth/BookingActivity.kt` |
| Danh sách lịch hẹn | `activity_my_appointments.xml` | `View/Auth/MyAppointmentsActivity.kt` |
| Danh sách chat | `activity_conversations.xml` | `View/Auth/ConversationsActivity.kt` |
| Màn hình chat | `activity_chat.xml` | `View/Auth/ChatActivity.kt` |
| Thông báo | `activity_notifications.xml` | `View/Auth/NotificationsActivity.kt` |

---

## 4) Web Admin: thành phần và file quan trọng

Repo web admin local: `C:\Users\tiend\Admin_TimTro_New`

### 4.1 Cấu trúc chính

```text
Admin_TimTro_New/
  |- public/
  |   |- index.html
  |   |- app.js
  |- firestore.rules
  |- firebase.json
  |- phamtriendat_doantotnghiep/
      |- index.js          (Cloud Functions)
      |- package.json
```

### 4.2 Vai trò từng file
- `public/index.html`: layout giao diện admin dashboard.
- `public/app.js`:
  - đăng nhập admin qua Firebase Auth.
  - realtime listener Firestore.
  - duyệt bài, duyệt xác minh, khóa/mở khóa user.
  - xóa user và dọn dữ liệu liên quan (rooms, verifications, appointments, notifications, storage folder, `cccd_registry`).
  - hiển thị trạng thái online/offline theo `isOnline/lastSeen`.
- `phamtriendat_doantotnghiep/index.js`:
  - triển khai Cloud Functions nghiệp vụ tự động.

---

## 5) Cloud Functions đang dùng (backend nghiệp vụ)

Theo code `C:\Users\tiend\Admin_TimTro_New\phamtriendat_doantotnghiep\index.js` và console Functions (24/04/2026), hệ thống có các hàm chính:

1. `autoReviewVerificationByCloudVision` (Firestore trigger)
- Trigger: `onDocumentWritten("verifications/{uid}")`.
- Mục tiêu:
  - đọc ảnh CCCD front/back bằng Cloud Vision.
  - kiểm tra tín hiệu đúng mặt trước/sau.
  - trích xuất CCCD 12 số và so khớp.
  - nếu fail nhiều lần thì chuyển `pending_admin_review`.
- Có bộ đếm fail theo ngày trong `verification_counters`.

2. `autoUnlockUsers` (Scheduler mỗi 1 phút)
- Tự mở khóa user khi `lockUntil` đã hết hạn.

3. `dailyDataCleanup` (Scheduler)
- Dọn dữ liệu cũ/orphan:
  - `notifications`, `system_notifications`, `verifications` đã cũ.
  - `savedPosts`, `bookedSlots` mồ côi.
  - ảnh mồ côi trong Storage (`rooms/`, `verifications/`, `avatars/`, `chat_images/`).

4. `deleteUserAccount` (HTTPS request)
- API xóa user trên Firebase Authentication.
- Chỉ admin gọi được (xác thực Bearer token + kiểm tra role admin).
- Web admin gọi endpoint này trước khi dọn dữ liệu Firestore/Storage.

5. `sendPushNotification` (Firestore trigger)
- Trigger: `onDocumentCreated("notifications/{notifId}")`.
- Đẩy FCM đến `users/{uid}.fcmToken`.

6. `processPendingSlotUpgradePayments` (Scheduler mỗi 1 phút)
- Quét `slot_upgrade_requests` trạng thái `waiting_for_payment`.
- Gọi API SePay bằng secret `SEPAY_API_TOKEN`.
- Match theo `REQ_XXXXXXXX` trong nội dung chuyển khoản (ưu tiên), fallback theo amount + note.
- Nếu match:
  - cập nhật request thành `paid`.
  - cộng `users/{uid}.purchasedSlots`.
  - tạo notification “Nạp lượt thành công”.
- Nếu quá hạn thì `expired`.

---

## 6) Firebase Console cấu hình hiện tại (24/04/2026)

## 6.1 Authentication
- Sign-in providers đang bật:
  - Email/Password
  - Phone
- Email template:
  - Password reset đã custom nội dung tiếng Việt.
  - Sender name đang đặt theo thương hiệu app.

## 6.2 Firestore composite indexes (đang active)

1. `appointments`: `landlordId` (ASC), `createdAt` (DESC), `__name__` (DESC)
2. `appointments`: `tenantId` (ASC), `createdAt` (DESC), `__name__` (DESC)
3. `rooms`: `status` (ASC), `createdAt` (DESC), `__name__` (DESC)
4. `rooms`: `isFeatured` (ASC), `createdAt` (DESC), `__name__` (DESC)
5. `rooms`: `userId` (ASC), `status` (ASC), `createdAt` (DESC), `__name__` (DESC)
6. `rooms`: `userId` (ASC), `createdAt` (DESC), `__name__` (DESC)
7. `rooms`: `status` (ASC), `isFeatured` (ASC), `createdAt` (DESC), `__name__` (DESC)
8. `users`: `isLocked` (ASC), `lockUntil` (ASC), `__name__` (ASC)
9. `checks`: `status` (ASC), `createdAt` (DESC), `__name__` (DESC)

## 6.3 Cloud/GCP APIs
- Cloud Vision API: enabled.
- Maps SDK for Android: enabled.
- API key restrictions:
  - Selected APIs: Geocoding API, Maps SDK for Android, Places API (New).
  - Application restriction: Android apps (package + SHA1).

---

## 7) Firestore data model và collections chính

Các collection nghiệp vụ:
- `users`: hồ sơ người dùng, role, verify state, lock state, presence, purchasedSlots.
- `rooms`: bài đăng phòng (`pending/approved/rejected/expired`).
- `verifications`: hồ sơ xác minh CCCD.
- `cccd_registry`: chống trùng CCCD giữa nhiều tài khoản.
- `slot_upgrade_requests`: yêu cầu nạp thêm lượt đăng bài.
- `appointments`: lịch hẹn người thuê/chủ trọ.
- `chats/{chatId}/messages`: hội thoại.
- `savedPosts`: bài lưu.
- `notifications`: thông báo cá nhân.
- `system_notifications`: hàng đợi broadcast nội bộ.
- `checks`, `verification_counters`: dữ liệu kỹ thuật nội bộ.
- `bookedSlots`: dữ liệu đồng bộ khung giờ đã đặt.

Storage path chính:
- `avatars/{uid}`
- `rooms/{roomId}/{fileName}`
- `verifications/{uid}/{fileName}`
- `chat_images/{chatId}/{fileName}`

---

## 8) Luồng nghiệp vụ quan trọng end-to-end

## 8.1 Presence online/offline (App -> Web Admin)

1. App vào foreground:
- `MainActivity.onStart()` gọi `PresenceManager.goOnline()`.
- Update `users/{uid}`: `isOnline=true`, `lastSeen=now`.

2. App ra background hoặc logout:
- `MainActivity.onStop()` hoặc logout gọi `PresenceManager.goOffline()` / `goOfflineAndThen`.
- Update `isOnline=false`, `lastSeen=now`.

3. Web admin:
- `public/app.js` đọc `isOnline/lastSeen`.
- Dùng ngưỡng stale timeout để tránh hiển thị online “ảo”.
- Render badge “Hoạt động / Offline từ …”.

## 8.2 Xác minh CCCD mở quyền đăng bài

1. Mobile (`VerifyLandlordActivity`, `VerificationRepository`):
- Chụp 2 mặt CCCD (camera custom).
- OCR local ML Kit + validate cơ bản.
- Upload ảnh `verifications/{uid}/...`.
- Ghi doc `verifications/{uid}` trạng thái `pending`.

2. Backend auto:
- Function `autoReviewVerificationByCloudVision` nhận trigger.
- Cloud Vision đọc ảnh và kiểm tra:
  - đúng mặt trước/mặt sau.
  - có CCCD 12 số hợp lệ.
  - khớp CCCD đã khai báo.
- Nếu pass: duyệt tự động.
- Nếu fail lặp lại: chuyển `pending_admin_review` để admin xử lý tay.

3. Web admin:
- Trang xác minh duyệt/từ chối.
- Khi duyệt: cập nhật user được mở quyền đăng bài (`isVerified=true`) và các field liên quan.

## 8.3 Đăng bài + quota 24h + nạp thêm lượt

1. Khi user đăng bài:
- `PostFragment` gọi `PostViewModel` -> `RoomRepository`.
- `RoomRepository` kiểm quota:
  - còn `purchasedSlots` thì ưu tiên dùng gói nạp.
  - nếu hết thì dùng hạn mức theo cửa sổ 24h.
- Bài mới lưu vào `rooms` với `status="pending"` chờ duyệt.

2. Mua thêm lượt bằng QR:
- `PostFragment` tạo doc `slot_upgrade_requests/{requestId}`:
  - `status="waiting_for_payment"`
  - `transferNote = "MUA GOIxx REQ_XXXXXXXX"`.
- App hiển thị QR VietQR từ bank info.
- App lắng nghe realtime status của doc request để đổi nút “Hoàn tất giao dịch”.

3. Function đối soát:
- `processPendingSlotUpgradePayments` chạy mỗi phút.
- Gọi SePay lấy giao dịch mới.
- Match `REQ_XXXXXXXX`.
- Nếu thành công:
  - request -> `paid`
  - cộng `users.purchasedSlots`
  - tạo notification thành công.

## 8.4 Duyệt bài đăng

1. User tạo bài ở trạng thái `pending`.
2. Web admin load danh sách bài chờ (`loadPosts`).
3. Admin:
- `approvePost`: chuyển `approved`.
- `rejectPost`: chuyển `rejected`, lưu lý do.

## 8.5 Khóa/mở khóa user

1. Web admin `toggleLockUser`:
- set `isLocked=true`, `lockUntil`, `lockReason`.
- push notification cho user.

2. Function `autoUnlockUsers`:
- chạy định kỳ, tự mở khóa khi đến hạn.

## 8.6 Xóa user toàn diện

1. Web admin gọi function HTTPS `deleteUserAccount` để xóa Auth account.
2. Sau đó dọn dữ liệu Firestore/Storage liên quan:
- user doc, verification doc/folder, rooms/folder ảnh, appointments, notifications, saved/chat artifacts, `cccd_registry`.
3. Tránh để lại dữ liệu “mồ côi”.

---

## 9) Security Rules đang áp dụng

## 9.1 Firestore rules (file `firestore.rules`)
- `users`:
  - user chỉ sửa whitelist field profile.
  - không tự nâng role lên admin.
  - có field presence `isOnline`, `lastSeen`.
- `rooms`:
  - tạo bài cần đúng điều kiện `canPostRoom()`.
  - public chỉ đọc bài `approved/expired`; owner/admin có quyền rộng hơn.
- `verifications`:
  - user tạo hồ sơ xác minh của chính mình.
  - update giới hạn theo trạng thái.
- `slot_upgrade_requests`:
  - client tạo request chờ thanh toán.
  - trạng thái trả tiền (`paid/failed/expired`) do backend/admin xử lý.
  - client chỉ được hủy khi đang chờ.
- `cccd_registry`:
  - chống trùng CCCD.
- `appointments`, `chats/messages`, `savedPosts`, `notifications`:
  - kiểm tra quyền participant/owner/admin.

## 9.2 Storage rules (file `storage.rules`)
- Chỉ cho phép ảnh và giới hạn dung lượng:
  - avatar tối đa 5MB.
  - ảnh room/chat tối đa 15MB.
  - ảnh verification tối đa 10MB.
- Quyền truy cập theo owner/admin/participant.
- Mặc định deny tất cả path không khai báo.

---

## 10) Mapping theo lớp code (Android app)

Repositories chính:
- `repository/AuthRepository.kt`: auth + profile + lock-check + delete-account call.
- `repository/RoomRepository.kt`: CRUD bài, quota, purchasedSlots, saved posts.
- `repository/VerificationRepository.kt`: OCR local + upload + verify docs.
- `repository/AppointmentRepository.kt`: lịch hẹn.
- `repository/ChatRepository.kt`: chat + ảnh + reactions.
- `repository/UserRepository.kt`: tìm kiếm hồ sơ user.

ViewModel nổi bật:
- `PostViewModel.kt`: điều phối đăng bài + pre-check quota.
- `VerifyLandlordViewModel.kt`: điều phối xác minh CCCD.
- `SearchViewModel.kt`: lọc/sắp xếp kết quả tìm kiếm.
- `BookingViewModel.kt`: lifecycle lịch hẹn.
- `ChatViewModel.kt`: gửi/nhận chat và trạng thái đọc.
- `ProfileViewModel.kt`: profile + đăng xuất.

---

## 11) Hướng dẫn chạy và deploy

## 11.1 Chạy app Android

```bash
./gradlew clean
./gradlew assembleDebug
```

Mở bằng Android Studio, đảm bảo đã có `google-services.json` đúng project Firebase.

## 11.2 Chạy web admin (Hosting)

```bash
cd C:\Users\tiend\Admin_TimTro_New
firebase deploy --only hosting
```

Deploy nhanh theo script hiện có:

```bash
deploy.bat
```

(`deploy.bat` đang dùng `hosting:channel:deploy ...` rồi clone sang live)

## 11.3 Deploy Cloud Functions

```bash
cd C:\Users\tiend\Admin_TimTro_New\phamtriendat_doantotnghiep
npm install
firebase deploy --only functions
```

Set secret SePay token:

```bash
firebase functions:secrets:set SEPAY_API_TOKEN
```

## 11.4 Deploy rules

```bash
cd C:\Users\tiend\AndroidStudioProjects\Doantotnghiep
firebase deploy --only firestore:rules
firebase deploy --only storage
```

---

## 12) Checklist demo cho giảng viên

1. Tạo user mới trên app (Email/Password hoặc Phone).
2. Chụp CCCD 2 mặt -> submit xác minh.
3. Kiểm tra trên web admin: hồ sơ xác minh xuất hiện.
4. Admin duyệt hồ sơ -> app mở quyền đăng bài.
5. User đăng bài -> bài vào trạng thái `pending`.
6. Admin duyệt bài -> bài thành `approved`.
7. User thử hết quota -> mở dialog nâng cấp.
8. Quét QR, chuyển khoản đúng nội dung `REQ_...`.
9. Function đối soát chạy -> request `paid`, `purchasedSlots` tăng, app đổi nút hoàn tất.
10. Test khóa user -> app bị chặn; hết hạn lock -> auto unlock.
11. Test trạng thái online/offline:
    - đăng nhập app -> web hiển thị online.
    - logout app -> web hiển thị offline + thời gian.
12. Test xóa user từ admin -> dữ liệu liên quan được dọn.

---

## 13) Ghi chú quan trọng khi bảo trì

- Không đổi tên field Firestore tùy ý vì app, web admin và function đang dùng chung schema.
- Các trạng thái workflow quan trọng cần giữ đúng enum:
  - `rooms.status`: `pending/approved/rejected/expired`
  - `verifications.status`: `pending`, `pending_admin_review`, `approved`, `rejected`
  - `slot_upgrade_requests.status`: `waiting_for_payment/paid/expired/failed/cancelled`
- Nếu thay đổi flow thanh toán, phải sửa đồng bộ:
  - `PostFragment.kt` (tạo request + listener)
  - `processPendingSlotUpgradePayments` (match giao dịch)
  - Rules `slot_upgrade_requests`
- Nếu đổi logic xác minh CCCD, sửa đồng bộ:
  - `VerificationRepository.kt` (client submit)
  - `autoReviewVerificationByCloudVision` (backend auto review)
  - Web admin màn duyệt xác minh (`public/app.js`)

