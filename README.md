# TIM TRO 24/7

> Ứng dụng Android tìm phòng trọ, đăng bài cho thuê, xác minh chủ trọ, đặt lịch xem phòng, chat, thông báo và quản lý lượt đăng bài bằng Firebase.

Tài liệu này được viết lại dựa trên việc đọc và đối chiếu trực tiếp mã nguồn trong project Android này. Mục tiêu là để người mới có thể hiểu nhanh:
- Ứng dụng làm gì.
- Kiến trúc hoạt động ra sao.
- Luồng dữ liệu đi từ UI đến Firebase.
- Mỗi màn hình / ViewModel / Repository dùng để làm gì.
- Khi cần sửa một tính năng thì mở file nào trước.

---

## 1. Tổng quan dự án

`TIM TRO 24/7` là một ứng dụng Android viết bằng **Kotlin** theo kiểu **MVVM**, kết nối với hệ sinh thái **Firebase** để xử lý hầu hết nghiệp vụ:

- **Firebase Authentication**: đăng ký, đăng nhập, đổi mật khẩu, kiểm tra phiên đăng nhập.
- **Firestore**: lưu user, phòng trọ, lịch hẹn, thông báo, bài lưu, xác minh, yêu cầu nâng cấp lượt đăng bài.
- **Firebase Storage**: lưu ảnh phòng, ảnh CCCD, ảnh liên quan bài đăng.
- **Firebase Messaging**: nhận push notification.
- **Google Maps / Geocoding**: chọn vị trí phòng trên bản đồ.
- **CameraX + ML Kit**: chụp CCCD và đọc OCR trong quy trình xác minh.

Dự án được tổ chức để phục vụ **2 nhóm người dùng chính**:

1. **Người thuê / người tìm trọ**
   - Tìm phòng theo khu vực, giá, nhu cầu.
   - Xem chi tiết phòng.
   - Lưu bài, đặt lịch xem phòng.
   - Chat với chủ trọ.
   - Nhận thông báo về lịch hẹn và trạng thái bài đăng.

2. **Chủ trọ**
   - Đăng bài cho thuê.
   - Xác minh danh tính bằng CCCD.
   - Quản lý bài đăng của mình.
   - Xem lịch hẹn.
   - Theo dõi lượt đăng bài còn lại.

---

## 2. Công nghệ và thư viện chính

### Ngôn ngữ / nền tảng
- Kotlin
- Android SDK
- minSdk 24
- compileSdk 35
- targetSdk 35
- Java 11

### Thư viện nổi bật trong `app/build.gradle.kts`
- AndroidX Core, AppCompat, Material
- Lifecycle ViewModel / LiveData
- Firebase BoM
- Firebase Auth / Firestore / Storage / Messaging
- Glide
- CameraX
- ML Kit Text Recognition
- Google Maps
- SwipeRefreshLayout
- CircleImageView
- Security Crypto
- OkHttp

### Điểm đáng chú ý
- `viewBinding = true` đã được bật.
- Ứng dụng dùng nhiều `LiveData` để cập nhật UI theo dữ liệu Firebase.
- Có cơ chế xử lý online/offline và token FCM ở `MainActivity`.

---

## 3. Cấu trúc kiến trúc

Dự án đi theo mô hình gần với **MVVM**:

- **View**: `Activity`, `Fragment`, XML layout
- **ViewModel**: chứa logic UI, gọi repository, giữ trạng thái hiển thị
- **Repository**: làm việc trực tiếp với Firebase / Storage / Auth
- **Model**: dữ liệu phòng, user, tin nhắn, cuộc hẹn...
- **Utils**: helper xử lý UI, định dạng, ảnh, thông báo, trạng thái online

### Luồng tổng quát

```text
UI (Activity / Fragment)
    -> ViewModel
        -> Repository
            -> Firebase / Storage / FCM / Maps
                -> trả dữ liệu
            -> ViewModel cập nhật LiveData
        -> UI observe LiveData và render
```

### Ưu điểm của cách tổ chức này
- Màn hình đỡ phải gọi Firebase trực tiếp.
- Logic nghiệp vụ tập trung ở Repository / ViewModel.
- Dễ truy vết khi cần debug.
- Dễ mở rộng thêm tính năng mới.

---

## 4. Điểm vào ứng dụng

### `SplashActivity`
- Là màn hình khởi động.
- Kiểm tra trạng thái đăng nhập.
- Điều hướng người dùng sang màn hình phù hợp.

### `WelcomeActivity`
- Màn hình chào / giới thiệu ban đầu.

### `LoginActivity`, `RegisterActivity`, `ForgotPasswordActivity`, `ResetPasswordActivity`
- Dùng cho xác thực tài khoản.
- Kết hợp với `AuthViewModel` và `AuthRepository`.

### `MainActivity`
- Là màn hình chính sau khi đăng nhập.
- Chứa bottom navigation.
- Quản lý fragment Home / Search / Post / Profile.
- Khởi tạo FCM token.
- Lắng nghe trạng thái Internet.
- Lắng nghe trạng thái khóa / xóa tài khoản.
- Hiển thị badge cho thông báo và lịch hẹn.

---

## 5. `MainActivity` làm gì

File: `app/src/main/java/com/example/doantotnghiep/MainActivity.kt`

Đây là một trong những file trung tâm nhất của app. Nó thực hiện các việc sau:

### 5.1 Điều hướng chính
- Dùng `BottomNavigationView` để đổi fragment:
  - `HomeFragment`
  - `SearchFragment`
  - `PostFragment`
  - `ProfileFragment`

### 5.2 Quản lý thông báo FCM
- Với Android 13+ app xin quyền `POST_NOTIFICATIONS`.
- Nếu được cấp quyền thì lấy FCM token và lưu vào Firestore user.
- Token được cập nhật lại khi đăng nhập mới.

### 5.3 Theo dõi trạng thái tài khoản
- Gọi `AuthRepository.listenUserStatus(...)`.
- Nếu tài khoản bị khóa hoặc bị xóa, app:
  - đưa user về offline bằng `PresenceManager`
  - sign out Firebase Auth
  - hiển thị dialog thông báo
  - chuyển về `LoginActivity`

### 5.4 Theo dõi mạng
- Đăng ký `ConnectivityManager.NetworkCallback`.
- Khi mất mạng sẽ hiện Toast cảnh báo.

### 5.5 Badge lịch hẹn
- `MainViewModel` theo dõi badge count của lịch hẹn và bài hết hạn.
- Role user được đọc từ collection `users` để quyết định logic badge.

### 5.6 Điều hướng từ notification
- `intent` có thể mở tab hoặc mở lịch hẹn trực tiếp.

---

## 6. Màn hình Home

File: `app/src/main/java/com/example/doantotnghiep/View/Fragment/HomeFragment.kt`

### Chức năng chính
- Chào người dùng.
- Hiển thị ngày hiện tại.
- Hiển thị bài nổi bật.
- Hiển thị bài mới.
- Hiển thị khu vực phổ biến.
- Tìm kiếm nhanh.
- Lưu lịch sử tìm kiếm gần đây.
- Mở màn hình thông báo.
- Mở profile search.

### Dữ liệu hiển thị
- `userName`
- `greeting`
- `currentDate`
- `popularAreas`
- `featuredRooms`
- `newRooms`
- `notificationBadgeCount`

### Các tương tác nổi bật
- Chạm chip khu vực để tìm theo quận.
- Chạm “Xem thêm” để tải thêm bài mới.
- Nhập từ khóa và bấm tìm kiếm để mở `SearchResultsActivity`.
- Có swipe-to-refresh để tải lại dữ liệu.

### Search history
- Lưu vào `SharedPreferences` tên `SearchHistory`.
- Tối đa 5 truy vấn gần nhất.

---

## 7. Màn hình đăng bài

File: `app/src/main/java/com/example/doantotnghiep/View/Fragment/PostFragment.kt`

Đây là luồng nghiệp vụ lớn nhất trong app.

### 7.1 Vai trò của màn hình
- Cho chủ trọ đăng bài.
- Với người chưa đăng nhập thì hiện màn hình guest.
- Với user chưa xác minh thì yêu cầu xác minh.
- Với user đã xác minh hoặc admin thì cho nhập form đăng bài.
- Có cơ chế chờ mở quyền sau khi được duyệt.

### 7.2 Quy trình hiển thị theo trạng thái user
`PostFragment` nhận `userObject` từ `PostViewModel` và quyết định:

- **Chưa đăng nhập**: hiện nút đăng nhập / đăng ký.
- **Chưa xác minh**: hiện màn hình yêu cầu xác minh.
- **Đang chờ duyệt**: hiện trạng thái pending.
- **Bị từ chối**: hiện lý do từ chối.
- **Đã được cấp quyền nhưng còn thời gian chờ**: hiện countdown mở quyền.
- **Đủ quyền**: hiện form đăng bài.

### 7.3 Form đăng bài chứa gì
- Họ tên chủ trọ
- Số điện thoại
- Giới tính
- Tiêu đề
- Phường/xã
- Địa chỉ
- Vị trí bản đồ
- Mô tả
- Giá phòng
- Diện tích
- Số người
- Loại phòng
- Tiền cọc
- Dịch vụ đi kèm
- Tiện nghi
- Bếp / WC / thú cưng / giờ giới nghiêm
- Ảnh phòng

### 7.4 Ảnh phòng
- Cho phép chọn tối đa 10 ảnh.
- Dùng `Intent.ACTION_PICK` với `EXTRA_ALLOW_MULTIPLE`.
- Ảnh được preview trực tiếp trong form.
- Có thể nhấn giữ để xóa ảnh.

### 7.5 Chọn vị trí
- Mở `LocationPickerActivity`.
- Trả về tọa độ và địa chỉ.
- Có tự động điền vào ô địa chỉ và sync lại phường/xã nếu khớp.

### 7.6 Gửi bài
- Bài được tạo thành `Model.Room`.
- Trạng thái mặc định là `pending`.
- Dữ liệu được gửi đến `PostViewModel.postRoom(...)`.
- Có dialog tiến trình upload.
- Khi thành công sẽ mở `PostSuccessActivity`.

### 7.7 Quy định và quota
- `PostFragment` có dialog điều khoản.
- `PostViewModel` kiểm tra quota trước khi đăng.
- Nếu hết lượt, user có thể mua thêm lượt đăng.

### 7.8 Mua thêm lượt đăng
Luồng nâng cấp lượt đăng bài được tích hợp trực tiếp trong `PostFragment`:
- Chọn gói `+3 lượt` hoặc `+10 lượt`.
- Có thể nhập gói tùy chọn.
- App tạo request trong `slot_upgrade_requests`.
- Hiển thị QR thanh toán VietQR.
- Poll trạng thái thanh toán theo document Firestore.
- Khi admin / cloud function xác nhận thanh toán, user được cộng slot.

---

## 8. Xác minh chủ trọ bằng CCCD

### File liên quan
- `View/Auth/VerifyLandlordActivity.kt`
- `View/Auth/CccdCameraActivity.kt`
- `ViewModel/VerifyLandlordViewModel.kt`
- `repository/VerificationRepository.kt`

### Luồng nghiệp vụ
1. User vào màn hình xác minh.
2. Chụp ảnh CCCD bằng CameraX.
3. Upload ảnh lên Storage.
4. Tạo document xác minh trong Firestore.
5. Ảnh / dữ liệu được kiểm tra bằng OCR hoặc quy trình duyệt.
6. Trạng thái có thể là:
   - `pending`
   - `pending_admin_review`
   - `approved`
   - `rejected`

### Ý nghĩa nghiệp vụ
- Chỉ user đã xác minh mới được mở quyền đăng bài.
- Giúp giảm đăng tin ảo và tăng độ tin cậy cho chủ trọ.

---

## 9. Danh sách phòng, chi tiết phòng và lưu bài

### 9.1 `SearchFragment`
File: `View/Fragment/SearchFragment.kt`
- Tìm kiếm phòng theo điều kiện.
- Kết hợp nhiều bộ lọc.
- Mở `SearchResultsActivity`.

### 9.2 `SearchResultsActivity`
- Hiển thị danh sách kết quả sau khi tìm.
- Dựa trên dữ liệu từ `SearchViewModel`.

### 9.3 `RoomDetailActivity`
- Xem chi tiết một phòng.
- Xem ảnh, thông tin chủ trọ, giá, tiện nghi, mô tả.
- Có logic lưu bài, đặt lịch, mở hồ sơ chủ trọ, và điều hướng chat.

### 9.4 `SavedPostsActivity`, `SavedPostDetailActivity`
- Quản lý bài đã lưu.
- Xem chi tiết bài đã lưu.

### 9.5 `RoomAdapter`
- Adapter hiển thị card phòng theo kiểu horizontal / vertical.
- Dùng lại cho nhiều màn hình.

---

## 10. Đặt lịch xem phòng

### File liên quan
- `View/Auth/BookingActivity.kt`
- `View/Auth/MyAppointmentsActivity.kt`
- `View/Auth/AppointmentRoomDetailActivity.kt`
- `ViewModel/BookingViewModel.kt`
- `repository/AppointmentRepository.kt`

### Chức năng
- Người thuê đặt lịch xem phòng.
- Lịch hẹn được lưu vào Firestore.
- Có cơ chế chống trùng giờ bằng `bookedSlots`.
- Chủ trọ xem các cuộc hẹn của mình trong `MyAppointmentsActivity`.

### Dữ liệu thường gặp
- tenantId
- landlordId
- roomId
- date
- time
- status

### Luồng tổng quát
1. Người thuê mở trang chi tiết phòng.
2. Chọn ngày giờ phù hợp.
3. Tạo lịch hẹn.
4. Lịch hẹn được phản ánh sang chủ trọ.
5. Badge trong `MainActivity` cập nhật số lượng lịch hẹn.

---

## 11. Chat và conversations

### File liên quan
- `View/Auth/ChatActivity.kt`
- `View/Auth/ConversationsActivity.kt`
- `View/Adapter/MessageAdapter.kt`
- `View/Adapter/ConversationAdapter.kt`
- `repository/ChatRepository.kt`
- `ViewModel/ChatViewModel.kt`

### Chức năng
- Nhắn tin giữa người thuê và chủ trọ.
- Danh sách cuộc trò chuyện.
- Hiển thị tin nhắn theo thread.
- Hỗ trợ thông báo cho tin nhắn mới.

### Đối tượng dữ liệu
- `Conversation`
- `Message`

### Ghi chú kỹ thuật
- Chat được lưu trên Firestore.
- UI dùng adapter riêng cho list tin nhắn và list conversation.

---

## 12. Thông báo

### File liên quan
- `Utils/MyFirebaseMessagingService.kt`
- `Utils/PostNotificationHelper.kt`
- `View/Auth/NotificationsActivity.kt`
- `View/Adapter/NotificationAdapter.kt`

### Loại thông báo
- Thông báo hệ thống
- Lịch hẹn
- Bài đăng được duyệt / từ chối / hết hạn
- Chat / tin nhắn mới
- Trạng thái tài khoản

### Cách hoạt động
- App nhận FCM token.
- Token được lưu vào Firestore user.
- Cloud Function hoặc server có thể gửi push.
- Trong app cũng có collection `notifications` để hiển thị lịch sử.

---

## 13. Hồ sơ cá nhân, cài đặt và đổi mật khẩu

### File liên quan
- `View/Fragment/ProfileFragment.kt`
- `View/Auth/UserProfileActivity.kt`
- `View/Auth/SettingsActivity.kt`
- `View/Auth/ChangePasswordActivity.kt`
- `View/Auth/PersonalInfoActivity.kt`
- `ViewModel/ProfileViewModel.kt`
- `ViewModel/UserProfileViewModel.kt`

### Nội dung chính
- Xem thông tin người dùng.
- Chỉnh sửa hồ sơ.
- Đổi mật khẩu.
- Cập nhật trạng thái online/offline.
- Xem bài đăng của mình.
- Xem lịch hẹn và thông báo liên quan.

---

## 14. Quản lý bài đăng của chủ trọ

### File liên quan
- `View/Auth/MyPostsActivity.kt`
- `View/Auth/MyPostDetailActivity.kt`
- `View/Auth/EditPostActivity.kt`
- `ViewModel/MyPostsViewModel.kt`
- `ViewModel/MyPostDetailViewModel.kt`
- `ViewModel/EditPostViewModel.kt`
- `repository/RoomRepository.kt`

### Tính năng
- Xem toàn bộ bài đã đăng.
- Lọc theo trạng thái.
- Sửa bài.
- Xóa bài.
- Đánh dấu đã cho thuê.
- Gia hạn bài.
- Theo dõi bài hết hạn.

### Cách `RoomRepository` hỗ trợ
- Lấy danh sách bài của user.
- Gia hạn bài.
- Xóa bài và xóa ảnh trong Storage.
- Cập nhật bài đã sửa.
- Xử lý bài hết hạn.
- Cập nhật badge unread cho chủ trọ.

---

## 15. Model dữ liệu quan trọng

### `Room`
File: `app/src/main/java/com/example/doantotnghiep/Model/Room.kt`

Đây là model trung tâm cho bài đăng. Nó chứa rất nhiều trường, gồm:
- thông tin phòng
- tiện nghi
- giá dịch vụ
- ảnh
- thông tin chủ trọ
- trạng thái bài
- thời gian tạo

Một số trường đáng chú ý:
- `status`: `pending`, `approved`, `rejected`, `expired`
- `imageUrls`: danh sách link ảnh
- `userId`: chủ bài
- `createdAt`: thời gian đăng
- `isFeatured`: bài nổi bật
- `depositAmount`, `depositMonths`
- `ownerName`, `ownerPhone`, `ownerAvatarUrl`

### Các model khác
- `User`
- `Conversation`
- `Message`
- `SupportTicket`

---

## 16. Repository và trách nhiệm của từng lớp

### `AuthRepository`
- Đăng nhập
- Đăng ký
- Đổi mật khẩu
- Kiểm tra trạng thái khóa tài khoản
- Cập nhật user

### `RoomRepository`
- Đăng bài
- Upload ảnh lên Storage
- Load danh sách phòng
- Load bài của chủ trọ
- Lưu / bỏ lưu bài
- Đánh dấu đã cho thuê
- Gia hạn bài
- Tính quota đăng bài
- Xử lý badge bài hết hạn

### `AppointmentRepository`
- Tạo lịch hẹn
- Xem lịch hẹn
- Kiểm tra slot đã bị đặt

### `ChatRepository`
- Gửi và nhận tin nhắn
- Quản lý conversation

### `UserRepository`
- Lấy / cập nhật thông tin user

### `VerificationRepository`
- Gửi hồ sơ xác minh
- Xử lý trạng thái xác minh

### `SupportRepository`
- Gửi ticket hỗ trợ
- Xem ticket hỗ trợ

---

## 17. ViewModel nào dùng cho màn nào

### Nhóm auth
- `AuthViewModel`
- `ForgotPasswordViewModel`
- `ResetPasswordViewModel`
- `PersonalInfoViewModel`

### Nhóm home / search / room
- `HomeViewModel`
- `SearchViewModel`
- `RoomViewModel`
- `SearchProfileViewModel`
- `SavedPostsViewModel`
- `SavedPostDetailViewModel`

### Nhóm bài đăng
- `PostViewModel`
- `EditPostViewModel`
- `MyPostsViewModel`
- `MyPostDetailViewModel`

### Nhóm booking / appointment
- `BookingViewModel`
- `AppointmentRoomDetailViewModel`

### Nhóm user / profile / verify
- `ProfileViewModel`
- `UserProfileViewModel`
- `VerifyLandlordViewModel`

### Nhóm chat / support
- `ChatViewModel`
- `SupportViewModel`

### Nhóm quản lý badge ở app shell
- `MainViewModel`

---

## 18. Dữ liệu Firestore và ý nghĩa nghiệp vụ

### `users`
Các trường thường gặp:
- `role`: `user`, `admin`
- `isVerified`: đã xác minh chủ trọ hay chưa
- `purchasedSlots`: số lượt đăng bài đã mua
- `isLocked`, `lockUntil`: trạng thái khóa
- `isOnline`, `lastSeen`: trạng thái online
- `fcmToken`: token để push notification
- `postingUnlockAt`: thời điểm được mở quyền đăng bài

### `rooms`
- Thông tin bài đăng phòng trọ
- `status` quyết định bài có hiển thị hay không
- `hasUnreadUpdate` dùng cho badge
- `expireAt` dùng cho TTL

### `verifications`
- Hồ sơ xác minh CCCD
- Trạng thái duyệt xác minh
- Lý do từ chối

### `appointments`
- Lịch hẹn xem phòng
- Dùng `status` để xác định trạng thái xử lý

### `bookedSlots`
- Dữ liệu chống trùng lịch
- Giúp kiểm tra slot đã bị đặt

### `notifications`
- Lịch sử thông báo trong app
- `seen` / `isRead` dùng để tính badge

### `savedPosts`
- Bài người dùng đã lưu

### `slot_upgrade_requests`
- Yêu cầu nâng cấp lượt đăng bài
- Dùng cho luồng thanh toán VietQR

---

## 19. Điểm đã được xử lý kỹ trong code

Dựa trên mã nguồn hiện tại, các điểm sau đã được chú ý và sửa/giảm rủi ro:

- Không ép kiểu `imageUrls` quá cứng.
- Đọc số Firestore qua `Number` thay vì chỉ `Long`.
- Tránh crash khi user chưa có email.
- Tránh `uid!!` ở chỗ nhạy cảm.
- Xử lý chọn ảnh từ `clipData` và `data.data` an toàn hơn.
- Có cơ chế chống trùng lịch hẹn.
- Có cơ chế TTL để giảm dữ liệu cũ.
- Có badge / notification count tách riêng theo logic nghiệp vụ.
- Có luồng nâng cấp lượt đăng bài bằng payment request.

---

## 20. Cấu trúc thư mục chính trong Android app

```text
app/src/main/java/com/example/doantotnghiep/
├── MainActivity.kt
├── MyApp.kt
├── Model/
├── repository/
├── Utils/
├── View/
│   ├── Adapter/
│   ├── Auth/
│   └── Fragment/
└── ViewModel/
```

### Ý nghĩa
- `Model`: cấu trúc dữ liệu.
- `repository`: truy cập Firebase / Storage.
- `ViewModel`: trạng thái UI và logic màn hình.
- `View/Auth`: hầu hết Activity của app.
- `View/Fragment`: các tab chính.
- `View/Adapter`: adapter RecyclerView.
- `Utils`: helper dùng chung.

---

## 21. Cách build và chạy

### Yêu cầu môi trường
- Android Studio
- JDK 11
- Firebase project đã cấu hình đúng
- `google-services.json` hợp lệ

### Chạy project
1. Mở thư mục project bằng Android Studio.
2. Sync Gradle.
3. Run trên emulator hoặc thiết bị thật.

### Lệnh build / test thường dùng
```bash
./gradlew.bat assembleDebug testDebugUnitTest
./gradlew.bat lintDebug
```

---

## 22. Khi cần sửa tính năng thì mở file nào trước

### Đăng nhập / đăng ký / đổi mật khẩu
- `View/Auth/LoginActivity.kt`
- `View/Auth/RegisterActivity.kt`
- `View/Auth/ForgotPasswordActivity.kt`
- `View/Auth/ResetPasswordActivity.kt`
- `ViewModel/AuthViewModel.kt`
- `repository/AuthRepository.kt`

### Tìm phòng
- `View/Fragment/SearchFragment.kt`
- `View/Auth/SearchResultsActivity.kt`
- `ViewModel/SearchViewModel.kt`
- `repository/RoomRepository.kt`

### Đăng bài
- `View/Fragment/PostFragment.kt`
- `ViewModel/PostViewModel.kt`
- `repository/RoomRepository.kt`

### Xác minh CCCD
- `View/Auth/VerifyLandlordActivity.kt`
- `View/Auth/CccdCameraActivity.kt`
- `ViewModel/VerifyLandlordViewModel.kt`
- `repository/VerificationRepository.kt`

### Đặt lịch xem phòng
- `View/Auth/BookingActivity.kt`
- `View/Auth/MyAppointmentsActivity.kt`
- `ViewModel/BookingViewModel.kt`
- `repository/AppointmentRepository.kt`

### Chat
- `View/Auth/ChatActivity.kt`
- `View/Auth/ConversationsActivity.kt`
- `repository/ChatRepository.kt`
- `ViewModel/ChatViewModel.kt`

### Hồ sơ người dùng
- `View/Fragment/ProfileFragment.kt`
- `View/Auth/UserProfileActivity.kt`
- `ViewModel/ProfileViewModel.kt`

### Bài đăng của tôi
- `View/Auth/MyPostsActivity.kt`
- `View/Auth/MyPostDetailActivity.kt`
- `View/Auth/EditPostActivity.kt`
- `ViewModel/MyPostsViewModel.kt`
- `ViewModel/EditPostViewModel.kt`

---

## 23. Nếu lỗi thì nên debug theo thứ tự nào

Khi gặp một lỗi, nên đi theo chuỗi sau:

1. **Màn hình nào lỗi?**
   - Activity / Fragment nào đang mở?

2. **ViewModel nào nhận dữ liệu?**
   - Xem observer, LiveData, input validation.

3. **Repository nào chạm Firebase?**
   - Query Firestore nào?
   - Upload Storage nào?
   - Có lỗi quyền không?

4. **Dữ liệu Firestore thực tế ra sao?**
   - Kiểu dữ liệu có đúng không?
   - Field có tồn tại không?
   - Status có đúng chữ không?

5. **Quy tắc Firebase Rules / Storage Rules**
   - Có chặn quyền đọc / ghi không?

6. **Logcat / exception**
   - Xem cụ thể exception để biết là null, cast sai, permission hay network.

---

## 24. Ghi chú quan trọng về dự án

- Đây là một app có khá nhiều luồng nghiệp vụ, không chỉ là app tìm phòng đơn giản.
- Dữ liệu đã được tách theo lớp khá rõ: `View` → `ViewModel` → `Repository`.
- Có nhiều màn hình và nhiều trạng thái tài khoản, vì vậy khi sửa nên đọc từ luồng dữ liệu trước rồi mới đọc UI.
- `RoomRepository` là một file cực kỳ quan trọng vì nó chứa gần như toàn bộ logic liên quan phòng trọ.
- `MainActivity` là trung tâm điều phối app shell, badge và notification.

---

## 25. Tóm tắt ngắn gọn để nhớ nhanh

- **Home**: xem bài nổi bật, bài mới, khu vực phổ biến.
- **Search**: lọc và tìm phòng.
- **Post**: đăng bài, xác minh, mua thêm lượt.
- **Profile**: hồ sơ cá nhân, bài của tôi, lịch hẹn, cài đặt.
- **Auth**: đăng nhập / đăng ký / quên mật khẩu.
- **Firebase**: là backend chính của toàn bộ app.

Nếu muốn hiểu nhanh một tính năng, hãy đọc theo thứ tự:

**Activity/Fragment → ViewModel → Repository → Firestore/Storage rules**

---

## 26. Kết luận

`TIM TRO 24/7` là một ứng dụng Android tương đối đầy đủ cho bài toán tìm phòng trọ và quản lý đăng bài cho thuê. Project này có các lớp xử lý rõ ràng, nhiều nghiệp vụ thực tế, và đã tích hợp khá sâu với Firebase.

Nếu bạn cần, tôi có thể làm tiếp một trong các bản sau:

1. Viết **README rút gọn hơn nhưng đẹp và chuẩn để nộp đồ án**.
2. Viết **README song ngữ Việt - Anh**.
3. Viết **tài liệu kiến trúc chi tiết hơn theo từng file**.
4. Viết **bản hướng dẫn chạy dự án từ A đến Z cho người mới**.
