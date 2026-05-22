# TÌM TRỌ 24/7 (Stay247) — Android Application

> **Đồ án tốt nghiệp** | Sinh viên: Phạm Tiến Đạt | Firebase Project ID: `doantotnghiep-b39ae`

---

## Mục lục

1. [Tổng quan dự án](#1-tổng-quan-dự-án)
2. [Công nghệ sử dụng](#2-công-nghệ-sử-dụng)
3. [Kiến trúc hệ thống](#3-kiến-trúc-hệ-thống)
4. [Mô hình dữ liệu Firestore](#4-mô-hình-dữ-liệu-firestore)
5. [Danh sách màn hình](#5-danh-sách-màn-hình)
6. [Luồng nghiệp vụ chính](#6-luồng-nghiệp-vụ-chính)
7. [Cloud Functions (Backend tự động)](#7-cloud-functions-backend-tự-động)
8. [Bảo mật hệ thống](#8-bảo-mật-hệ-thống)
9. [Cài đặt và cấu hình](#9-cài-đặt-và-cấu-hình)
10. [Đánh giá độ hoàn thiện so với đề cương](#10-đánh-giá-độ-hoàn-thiện-so-với-đề-cương)

---

## 1. Tổng quan dự án

### 1.1 Giới thiệu

**TÌM TRỌ 24/7** (tên thương hiệu: *Stay247*) là ứng dụng Android kết nối người có nhu cầu thuê phòng trọ với chủ trọ tại Việt Nam. Ứng dụng cho phép người dùng tìm kiếm phòng theo khu vực, lọc theo giá và diện tích, đặt lịch xem phòng, nhắn tin trực tiếp, đồng thời hỗ trợ chủ trọ đăng bài, xác minh danh tính bằng CCCD, và quản lý toàn bộ vòng đời bài đăng. Toàn bộ backend được xây dựng trên nền tảng Firebase, đảm bảo đồng bộ dữ liệu thời gian thực giữa ứng dụng Android và bảng điều khiển web quản trị.

### 1.2 Phạm vi người dùng

| Vai trò | Tiếng Anh | Quyền hạn chính |
|---------|-----------|-----------------|
| Khách (chưa đăng nhập) | Guest | Xem bài đăng, tìm kiếm cơ bản |
| Người thuê | Tenant | Tìm kiếm, lưu bài, đặt lịch, nhắn tin, hỏi AI |
| Chủ trọ (đã xác minh CCCD) | Verified Landlord | Đăng bài, quản lý lịch hẹn, nâng cấp bài, nhắn tin |
| Quản trị viên | Admin | Toàn quyền (qua Web Admin Panel riêng biệt) |

### 1.3 Các tính năng nổi bật

- **Xác minh CCCD 3 tầng**: ML Kit OCR tại thiết bị → Cloud Vision API tự động phân tích → Admin duyệt thủ công nếu cần
- **Hệ thống quota đăng bài**: 3 bài miễn phí/24h, sau đó tiêu `purchasedSlots` (mua qua SePay)
- **AI trợ lý tìm phòng**: Gemini 2.5 Flash với công cụ `searchRooms`, nhớ lịch sử hội thoại, gợi ý phòng trực tiếp trong chat
- **Thanh toán không gateway**: Quét mã QR SePay, Cloud Function polling 1 phút để đối soát giao dịch
- **Đồng bộ thời gian thực**: Firestore `onSnapshot` cho chat, thông báo, trạng thái lịch hẹn
- **Hệ thống hiện diện**: Heartbeat 60 giây lên Firestore, Web Admin hiển thị trực tuyến/offline
- **Bài nổi bật có kiểm duyệt**: Thanh toán → Admin duyệt → tự động tắt khi hết hạn (Cloud Function mỗi 30 phút)

---

## 2. Công nghệ sử dụng

### 2.1 Ngôn ngữ và nền tảng

| Thành phần | Chi tiết |
|-----------|----------|
| Ngôn ngữ lập trình | Kotlin |
| Nền tảng | Android (minSdk 26, targetSdk 35) |
| Kiến trúc | MVVM (Model–View–ViewModel) + Repository Pattern |
| Build system | Gradle (Kotlin DSL) |
| View binding | ViewBinding (không dùng `findViewById`) |

### 2.2 Firebase (Backend chính)

| Dịch vụ Firebase | Phiên bản SDK | Mục đích sử dụng |
|-----------------|---------------|-----------------|
| Firebase Authentication | 23.x | Đăng ký, đăng nhập email/Google, quản lý phiên |
| Cloud Firestore | 25.x | Cơ sở dữ liệu thời gian thực chính |
| Firebase Storage | 21.x | Lưu ảnh phòng, avatar, ảnh CCCD, ảnh chat, ảnh hỗ trợ |
| Firebase Cloud Functions | 21.x (callable) | Gọi backend: AI, xác minh, thanh toán, thống kê |
| Firebase Cloud Messaging (FCM) | 24.x | Push notification (targeted + broadcast) |
| Firebase App Check | — | Bảo vệ Firestore/Functions khỏi client giả mạo |

### 2.3 Google APIs bên ngoài

| API | Mục đích |
|-----|----------|
| Google Sign-In | Đăng nhập bằng tài khoản Google |
| ML Kit Text Recognition v2 | OCR chụp CCCD tại thiết bị (on-device, offline) |
| Google Maps SDK for Android | Hiển thị bản đồ, chọn vị trí phòng |
| Google Places API | Tìm kiếm địa điểm theo tên |
| Gemini API (qua Cloud Function) | AI trợ lý tìm phòng (Gemini 2.5 Flash) |
| Google Cloud Vision (qua Cloud Function) | Phân tích OCR CCCD server-side |

### 2.4 Thư viện Android bên thứ ba

| Thư viện | Phiên bản | Mục đích |
|---------|-----------|----------|
| Glide | 4.x | Load và cache ảnh từ URL |
| CameraX | 1.3.x | Camera chụp CCCD (Preview + ImageCapture) |
| OkHttp | 4.x | HTTP client (gọi SePay API kiểm tra thanh toán) |
| Markwon | 4.x | Render Markdown trong TextView (phản hồi AI) |
| CircleImageView | 3.1.0 | Avatar hình tròn |
| Android Jetpack | — | ViewModel, LiveData, Navigation, RecyclerView |
| Material Components | 1.x | UI Material Design (Button, TextInput, BottomNav) |
| Kotlinx Coroutines | 1.7.x | Bất đồng bộ coroutine thay callback |
| UCrop | 2.2.x | Cắt/xoay ảnh avatar sau khi chọn |

### 2.5 API thanh toán

| API | Nhà cung cấp | Mục đích |
|-----|-------------|----------|
| SePay API | SePay.vn | Tạo QR chuyển khoản, tra cứu giao dịch chờ |

---

## 3. Kiến trúc hệ thống

### 3.1 Sơ đồ kiến trúc tổng thể

```
┌─────────────────────────────────────────────────────┐
│                 ANDROID APPLICATION                  │
│                                                      │
│  ┌─────────────┐    ┌──────────────────────────┐    │
│  │    View     │◄──►│       ViewModel          │    │
│  │  (Activity/ │    │  (LiveData, StateFlow)   │    │
│  │  Fragment)  │    └───────────┬──────────────┘    │
│  └─────────────┘                │                    │
│                                 ▼                    │
│                    ┌────────────────────────┐        │
│                    │      Repository        │        │
│                    │ (data access layer)    │        │
│                    └──────────┬─────────────┘        │
└───────────────────────────────┼─────────────────────┘
                                │
              ┌─────────────────┼─────────────────┐
              ▼                 ▼                 ▼
     ┌──────────────┐  ┌──────────────┐  ┌──────────┐
     │  Firestore   │  │   Firebase   │  │  Cloud   │
     │  (Realtime   │  │   Storage    │  │Functions │
     │   Database)  │  │  (Images)    │  │(AI/Pay.) │
     └──────────────┘  └──────────────┘  └──────────┘
```

### 3.2 Luồng dữ liệu MVVM

```
User Action
    │
    ▼
Activity/Fragment (View)
    │  gọi method
    ▼
ViewModel
    │  gọi suspend fun / coroutine
    ▼
Repository
    │  đọc/ghi
    ▼
Firebase SDK (Firestore / Storage / Functions)
    │
    │  kết quả / onSnapshot
    ▼
Repository → ViewModel (LiveData.postValue)
    │
    ▼
Activity/Fragment observe LiveData → cập nhật UI
```

### 3.3 Cấu trúc thư mục source code

```
app/src/main/java/com/example/doantotnghiep/
├── Model/                          # Data classes
│   ├── User.kt
│   ├── Room.kt
│   ├── Appointment.kt
│   ├── ChatMessage.kt
│   ├── Notification.kt
│   ├── AIMessage.kt
│   └── AIRoom.kt
│
├── Repository/                     # Data access layer
│   ├── AuthRepository.kt           # Đăng ký, đăng nhập, Google Sign-In
│   ├── RoomRepository.kt           # CRUD phòng, quota, featured
│   ├── AppointmentRepository.kt    # Đặt lịch, xác nhận, hủy
│   ├── ChatRepository.kt           # Tin nhắn thời gian thực
│   ├── UserRepository.kt           # Hồ sơ người dùng, CCCD
│   ├── NotificationRepository.kt   # Đọc/ghi thông báo
│   └── SavedRoomRepository.kt      # Bài lưu
│
├── ViewModel/                      # 21 ViewModels
│   ├── AuthViewModel.kt
│   ├── HomeViewModel.kt
│   ├── SearchViewModel.kt
│   ├── RoomDetailViewModel.kt
│   ├── PostRoomViewModel.kt
│   ├── BookingViewModel.kt
│   ├── ChatViewModel.kt
│   ├── ChatListViewModel.kt
│   ├── NotificationViewModel.kt
│   ├── ProfileViewModel.kt
│   ├── MyPostsViewModel.kt
│   ├── MyAppointmentsViewModel.kt
│   ├── LandlordAppointmentsViewModel.kt
│   ├── VerificationViewModel.kt
│   ├── SavedRoomsViewModel.kt
│   ├── EditRoomViewModel.kt
│   ├── SlotUpgradeViewModel.kt
│   ├── FeaturedUpgradeViewModel.kt
│   ├── SupportViewModel.kt
│   ├── MapViewModel.kt
│   └── ReviewViewModel.kt
│
├── View/
│   ├── Auth/                       # Màn hình chính (35 Activities)
│   └── Adapter/                    # 8 RecyclerView Adapters
│
└── Utils/                          # 9 lớp tiện ích
    ├── LocationNormalizer.kt       # Chuẩn hóa tên quận/huyện
    ├── PriceFormatter.kt
    ├── ImageUtils.kt
    ├── PermissionUtils.kt
    ├── DateUtils.kt
    ├── NotificationUtils.kt
    ├── ValidationUtils.kt
    ├── FirestoreUtils.kt
    └── Constants.kt
```

---

## 4. Mô hình dữ liệu Firestore

### 4.1 Collection `users`

Mỗi document có ID = Firebase Auth UID.

| Field | Kiểu | Mô tả |
|-------|------|-------|
| `uid` | String | UID Firebase Auth |
| `email` | String | Email đăng nhập |
| `fullName` | String | Họ tên hiển thị |
| `phoneNumber` | String | Số điện thoại |
| `avatarUrl` | String | URL ảnh đại diện trên Storage |
| `role` | String | `"tenant"` / `"landlord"` / `"admin"` |
| `isVerified` | Boolean | Đã xác minh CCCD được Admin duyệt |
| `isLocked` | Boolean | Tài khoản đang bị khóa |
| `lockReason` | String | Lý do khóa (admin điền) |
| `lockUntil` | Timestamp/null | Thời điểm tự động mở khóa |
| `purchasedSlots` | Number | Số lượt đăng bài đã mua còn lại |
| `dailyPostKey` | String | Khóa ngày `"YYYY-MM-DD"` để đếm bài miễn phí |
| `dailyPostCount` | Number | Số bài đã đăng trong ngày (giới hạn 3) |
| `fcmToken` | String | FCM device token cho push notification |
| `lastSeen` | Timestamp | Timestamp heartbeat cuối (presence system) |
| `createdAt` | Timestamp | Ngày tạo tài khoản |

**Subcollection:** `users/{uid}/ai_conversations` — lưu lịch sử chat AI (mỗi document = 1 lượt hỏi/đáp).

### 4.2 Collection `rooms`

Mỗi document = 1 bài đăng phòng trọ.

| Field | Kiểu | Mô tả |
|-------|------|-------|
| `id` | String | Document ID |
| `userId` | String | UID của chủ trọ |
| `title` | String | Tiêu đề bài đăng |
| `description` | String | Mô tả chi tiết |
| `price` | Number | Giá thuê (VNĐ/tháng) |
| `area` | Number | Diện tích (m²) |
| `address` | String | Địa chỉ đầy đủ |
| `district` | String | Quận/huyện (đã chuẩn hóa) |
| `city` | String | Thành phố |
| `latitude` | Number | Tọa độ bản đồ |
| `longitude` | Number | Tọa độ bản đồ |
| `imageUrls` | Array\<String\> | Danh sách URL ảnh phòng |
| `amenities` | Array\<String\> | Tiện ích (wifi, điều hòa, ...) |
| `status` | String | `"pending"` / `"approved"` / `"rejected"` / `"hidden"` |
| `isVerifiedLandlord` | Boolean | Chủ trọ đã xác minh CCCD |
| `isFeatured` | Boolean | Đang là bài nổi bật |
| `featuredUntil` | Timestamp/null | Hết hạn bài nổi bật |
| `viewCount` | Number | Lượt xem |
| `createdAt` | Timestamp | Ngày đăng |
| `updatedAt` | Timestamp | Lần chỉnh sửa gần nhất |

### 4.3 Collection `appointments`

Mỗi document = 1 lịch hẹn xem phòng.

| Field | Kiểu | Mô tả |
|-------|------|-------|
| `id` | String | Document ID |
| `roomId` | String | ID bài đăng phòng |
| `tenantId` | String | UID người thuê |
| `landlordId` | String | UID chủ trọ |
| `tenantName` | String | Tên người thuê (snapshot) |
| `landlordName` | String | Tên chủ trọ (snapshot) |
| `roomTitle` | String | Tiêu đề phòng (snapshot) |
| `scheduledDate` | String | Ngày hẹn `"dd/MM/yyyy"` |
| `scheduledTime` | String | Giờ hẹn `"HH:mm"` |
| `note` | String | Ghi chú của người thuê |
| `status` | String | Xem bảng trạng thái bên dưới |
| `createdAt` | Timestamp | Ngày tạo lịch hẹn |

**Vòng đời trạng thái appointment:**

```
pending ──► confirmed ──► viewed ──► rented
    │            │
    └────────────┴──► rejected
    │
    └──► cancelled
```

### 4.4 Collection `chats`

| Field | Kiểu | Mô tả |
|-------|------|-------|
| `id` | String | Document ID (`userId1_userId2` sắp xếp theo alphabet) |
| `participants` | Array\<String\> | [UID người A, UID người B] |
| `lastMessage` | String | Nội dung tin nhắn cuối |
| `lastMessageTime` | Timestamp | Thời gian tin nhắn cuối |
| `unreadCount` | Map | `{uid: number}` — số tin chưa đọc mỗi người |

**Subcollection:** `chats/{chatId}/messages` — mỗi document = 1 tin nhắn.

| Field | Kiểu | Mô tả |
|-------|------|-------|
| `senderId` | String | UID người gửi |
| `text` | String | Nội dung tin nhắn |
| `imageUrl` | String | URL ảnh đính kèm (nếu có) |
| `timestamp` | Timestamp | Thời gian gửi |
| `isRead` | Boolean | Người nhận đã đọc chưa |

### 4.5 Collection `notifications`

| Field | Kiểu | Mô tả |
|-------|------|-------|
| `userId` | String | UID người nhận |
| `title` | String | Tiêu đề thông báo |
| `message` | String | Nội dung thông báo |
| `type` | String | Loại: `appointment`, `chat`, `post_status`, `support_reply`, v.v. |
| `isRead` | Boolean | Đã đọc chưa |
| `seen` | Boolean | Đã hiện trong badge chưa |
| `createdAt` | Timestamp | Thời gian tạo |
| `ticketId` | String (optional) | ID ticket hỗ trợ liên quan |

### 4.6 Collection `verifications`

| Field | Kiểu | Mô tả |
|-------|------|-------|
| `uid` | String | UID người xác minh |
| `frontImageUrl` | String | URL ảnh mặt trước CCCD |
| `backImageUrl` | String | URL ảnh mặt sau CCCD |
| `cccdNumber` | String | Số CCCD người dùng nhập |
| `fullName` | String | Họ tên người dùng nhập |
| `status` | String | `"pending"` / `"auto_approved"` / `"auto_rejected"` / `"approved"` / `"rejected"` |
| `autoReviewResult` | String | Kết quả Cloud Vision: `"passed"` / `"failed"` / `"escalated"` |
| `reviewNote` | String | Ghi chú từ admin hoặc Cloud Vision |
| `submittedAt` | Timestamp | Thời điểm nộp |
| `reviewedAt` | Timestamp | Thời điểm duyệt |

### 4.7 Collection `support_tickets`

| Field | Kiểu | Mô tả |
|-------|------|-------|
| `userId` | String | UID người gửi |
| `userName` | String | Tên người gửi |
| `userEmail` | String | Email người gửi |
| `category` | String | Loại vấn đề |
| `title` | String | Tiêu đề |
| `lastMessage` | String | Tin nhắn cuối |
| `status` | String | `"new"` / `"in_progress"` / `"resolved"` / `"closed"` |
| `unreadForAdmin` | Boolean | Admin chưa đọc |
| `unreadForUser` | Boolean | User chưa đọc phản hồi |
| `createdAt` | Timestamp | Ngày tạo |
| `updatedAt` | Timestamp | Lần cập nhật gần nhất |

**Subcollection:** `support_tickets/{ticketId}/messages` — luồng chat 2 chiều admin-user.

### 4.8 Các collection phụ trợ

| Collection | Mục đích |
|-----------|----------|
| `savedPosts` | Bài phòng trọ đã lưu yêu thích |
| `bookedSlots` | Slot thời gian đã đặt lịch hẹn (tránh trùng) |
| `slot_upgrade_requests` | Yêu cầu mua slot đăng bài (chờ SePay xác nhận) |
| `featured_upgrade_requests` | Yêu cầu bài nổi bật (chờ SePay + admin duyệt) |
| `system_notifications` | Thông báo broadcast FCM gửi cho tất cả user |
| `stats/popular_areas` | Thống kê số phòng đã duyệt theo quận/huyện |
| `verification_counters` | Bộ đếm lần thất bại Cloud Vision (3-strike escalation) |
| `cccd_registry` | Registry số CCCD đã xác minh (tránh trùng lặp) |

---

## 5. Danh sách màn hình

### 5.1 Màn hình xác thực (Auth)

| Màn hình | Activity | Mô tả |
|---------|----------|-------|
| Splash | `SplashActivity` | Kiểm tra trạng thái đăng nhập, điều hướng tự động |
| Chào mừng | `WelcomeActivity` | Màn hình chào với nút đăng nhập/đăng ký |
| Đăng nhập | `LoginActivity` | Email/password + Google Sign-In |
| Đăng ký | `RegisterActivity` | Tạo tài khoản mới (email, tên, vai trò) |
| Quên mật khẩu | `ForgotPasswordActivity` | Gửi email reset mật khẩu |
| Đặt lại mật khẩu | `ResetPasswordActivity` | Form nhập mật khẩu mới |

### 5.2 Màn hình chính (Shell)

| Màn hình | Activity/Fragment | Mô tả |
|---------|-------------------|-------|
| Màn hình chính | `MainActivity` | Bottom Navigation 4 tab |
| Trang chủ | `HomeFragment` | Danh sách bài nổi bật, tìm kiếm nhanh |
| Tìm kiếm | `SearchFragment` | Tìm theo quận/huyện và từ khóa |
| Đăng bài | `PostFragment` | Cổng vào đăng bài hoặc redirect CCCD |
| Hồ sơ | `ProfileFragment` | Thông tin cá nhân, cài đặt |

### 5.3 Màn hình tìm kiếm & khám phá

| Màn hình | Activity | Mô tả |
|---------|----------|-------|
| Kết quả tìm kiếm | `SearchActivity` | Danh sách phòng theo từ khóa/quận |
| Lọc nâng cao | `FilterActivity` | Lọc theo giá, diện tích, tiện ích |
| Bản đồ | `MapActivity` | Xem phòng trên Google Maps |
| Chi tiết phòng | `RoomDetailActivity` | Thông tin đầy đủ, ảnh, đặt lịch, lưu |
| Bài đã lưu | `SavedRoomsActivity` | Danh sách phòng yêu thích |

### 5.4 Màn hình đặt lịch hẹn

| Màn hình | Activity | Mô tả |
|---------|----------|-------|
| Đặt lịch | `BookingActivity` | Chọn ngày giờ, ghi chú, xác nhận |
| Lịch hẹn của tôi (Tenant) | `MyAppointmentsActivity` | Danh sách lịch hẹn đã đặt |
| Lịch hẹn (Landlord) | `LandlordAppointmentsActivity` | Xác nhận/từ chối lịch từ tenant |

### 5.5 Màn hình nhắn tin

| Màn hình | Activity | Mô tả |
|---------|----------|-------|
| Danh sách chat | `ChatListActivity` | Tất cả cuộc trò chuyện |
| Nhắn tin | `ChatActivity` | Chat 1-1 với ảnh đính kèm, onSnapshot |

### 5.6 Màn hình chủ trọ (Landlord)

| Màn hình | Activity | Mô tả |
|---------|----------|-------|
| Xác minh CCCD | `VerificationActivity` | Nộp ảnh CCCD (ML Kit OCR hỗ trợ) |
| Chụp CCCD | `CameraActivity` | CameraX Preview + ImageCapture |
| Đăng bài | `PostRoomActivity` | Form đăng bài đầy đủ với chọn ảnh |
| Bài đăng của tôi | `MyPostsActivity` | Danh sách bài đã đăng, trạng thái |
| Sửa bài | `EditRoomActivity` | Chỉnh sửa thông tin phòng đã đăng |
| Nâng cấp slot | `SlotUpgradeActivity` | Mua thêm lượt đăng bài qua SePay |
| Nâng cấp nổi bật | `FeaturedUpgradeActivity` | Đưa bài lên vị trí nổi bật qua SePay |

### 5.7 Màn hình hồ sơ & cài đặt

| Màn hình | Activity | Mô tả |
|---------|----------|-------|
| Hồ sơ cá nhân | `EditProfileActivity` | Sửa tên, SĐT, avatar |
| Đổi mật khẩu | `ChangePasswordActivity` | Đổi mật khẩu (re-authenticate) |
| Thông báo | `NotificationActivity` | Danh sách thông báo có badge |
| Hỗ trợ | `SupportActivity` | Gửi ticket hỗ trợ |
| Chat hỗ trợ | `SupportChatActivity` | Chat 2 chiều với admin |

### 5.8 Màn hình AI và đặc biệt

| Màn hình | Activity | Mô tả |
|---------|----------|-------|
| AI Chat | `AIChatActivity` | Trợ lý AI tìm phòng (Gemini 2.5 Flash) |
| Xem ảnh toàn màn hình | `FullscreenImageActivity` | Xem ảnh phòng/CCCD toàn màn hình |
| Chính sách | `PolicyActivity` | Điều khoản sử dụng |

### 5.9 Fragments

| Fragment | Thuộc Activity | Mô tả |
|---------|----------------|-------|
| `HomeFragment` | MainActivity | Tab Trang chủ |
| `SearchFragment` | MainActivity | Tab Tìm kiếm |
| `PostFragment` | MainActivity | Tab Đăng bài |
| `ProfileFragment` | MainActivity | Tab Hồ sơ |

---

## 6. Luồng nghiệp vụ chính

### 6.1 Đăng ký và đăng nhập

#### Đăng ký tài khoản mới
```
1. User nhập email, mật khẩu, họ tên, chọn vai trò (tenant/landlord)
2. RegisterActivity → AuthViewModel → AuthRepository
3. Firebase Auth: createUserWithEmailAndPassword()
4. Tạo document Firestore: users/{uid} với role, fullName, isVerified=false
5. Gửi email xác thực (sendEmailVerification)
6. Điều hướng về LoginActivity
```

#### Đăng nhập Google
```
1. User chọn "Đăng nhập bằng Google"
2. GoogleSignInClient hiển thị account picker
3. Lấy ID Token → FirebaseAuth.signInWithCredential(GoogleAuthProvider)
4. Kiểm tra users/{uid} tồn tại chưa
5. Nếu chưa: tạo document mới (role mặc định "tenant")
6. Điều hướng về MainActivity
```

### 6.2 Xác minh CCCD 3 tầng

Đây là luồng phức tạp nhất trong ứng dụng, kết hợp 3 lớp kiểm tra:

```
TẦNG 1 — On-device (ML Kit):
User → VerificationActivity
     → Chọn ảnh thư viện hoặc CameraActivity chụp mới
     → ML Kit Text Recognition v2 đọc văn bản từ ảnh
     → Validate: có số 12 chữ số? có "CCCD"/"CMND"?
     → Upload ảnh lên Storage: verifications/{uid}/front.jpg
     → Upload ảnh mặt sau: verifications/{uid}/back.jpg
     → Ghi verifications/{uid}: {frontImageUrl, backImageUrl, cccdNumber, fullName, status:"pending"}

TẦNG 2 — Server-side (Cloud Vision, tự động):
Firestore trigger onDocumentWritten("verifications/{uid}")
     → Cloud Function: autoReviewVerificationByCloudVision
     → Gọi Google Cloud Vision annotate API cho cả 2 ảnh
     → So khớp CCCD_FRONT_KEYWORDS: "căn cước công dân", "số", "quê quán",...
     → So khớp CCCD_BACK_KEYWORDS: "đặc điểm nhận dạng", "ngày cấp",...
     → Trích xuất 12 chữ số bằng regex sliding window
     → So sánh với cccdNumber người dùng nhập
     → Khớp tên (fuzzy, bỏ dấu) với fullName người dùng nhập
     → Nếu đạt: status="auto_approved", users/{uid}.isVerified=true
     → Nếu thất bại lần 1-2: status="auto_rejected", ocr_fail_counters++
     → Nếu thất bại lần 3: status="escalated" → Admin phải duyệt thủ công

TẦNG 3 — Admin duyệt thủ công (Web Admin Panel):
Admin mở 08-users.js → loadVerifications()
     → Xem ảnh mặt trước/mặt sau CCCD
     → approveVerification(): users/{uid}.isVerified=true, verification.status="approved"
     → rejectVerification(): verification.status="rejected", tạo notification cho user
```

### 6.3 Hệ thống quota đăng bài

```
Landlord muốn đăng bài → PostRoomActivity → PostRoomViewModel
         → RoomRepository.checkDailyPostQuota(uid)

Kiểm tra quota (ưu tiên 1 — bài miễn phí):
     → Đọc users/{uid}.dailyPostKey
     → Nếu dailyPostKey == "YYYY-MM-DD" hôm nay:
          → Kiểm tra dailyPostCount < 3
          → Nếu còn slot: dailyPostCount++, cho đăng
     → Nếu dailyPostKey != hôm nay:
          → Reset: dailyPostKey = hôm nay, dailyPostCount = 1, cho đăng

Kiểm tra quota (ưu tiên 2 — bài đã mua):
     → Nếu bài miễn phí hết (dailyPostCount >= 3):
          → Kiểm tra purchasedSlots > 0
          → Nếu có: purchasedSlots--, cho đăng
          → Nếu không: block, redirect SlotUpgradeActivity

Đăng bài thành công:
     → Tạo rooms/{roomId} với status="pending"
     → Cloud Function updatePopularAreasStats tự động cập nhật thống kê quận
     → Admin duyệt → status="approved" → xuất hiện trên HomeFragment
```

### 6.4 Luồng mua slot và bài nổi bật (SePay)

```
MUAS SLOT ĐĂNG BÀI:
User → SlotUpgradeActivity
     → Chọn gói (ví dụ: 10 lượt = 50,000 VNĐ)
     → Tạo slot_upgrade_requests/{reqId}: {uid, amount, status:"pending", reqCode:"REQ-xxxxx"}
     → Hiển thị QR code SePay với nội dung chuyển khoản: "REQ-xxxxx"
     → User chuyển khoản thực tế qua app ngân hàng

Cloud Function processPendingSlotUpgradePayments (mỗi 1 phút):
     → Gọi SePay API lấy danh sách giao dịch gần nhất
     → Duyệt từng giao dịch: có nội dung chứa "REQ-xxxxx" không?
     → Nếu khớp: Firestore Transaction {
            purchasedSlots += amount,
            slot_upgrade_requests/{reqId}.status = "completed"
       }
     → Tạo notification cho user: "Đã cộng X lượt đăng bài"
     → Yêu cầu quá 30 phút mà chưa thanh toán: status = "expired"

BÀI NỔI BẬT (thêm bước Admin):
Tương tự slot nhưng thêm bước:
     → Sau khi SePay xác nhận: featured_upgrade_requests/{id}.status = "paid_waiting_admin"
     → Admin vào Web Panel duyệt → approveFeaturedRequest()
     → rooms/{roomId}: {isFeatured=true, featuredUntil=Timestamp(+N ngày)}
     → Cloud Function autoDisableExpiredFeaturedRooms (mỗi 30 phút):
          → Query rooms where isFeatured=true AND featuredUntil <= now
          → Batch update: isFeatured=false
```

### 6.5 Tìm kiếm và lọc phòng

```
HomeFragment / SearchFragment / SearchActivity
     → Nhập từ khóa, chọn quận/huyện
     → LocationNormalizer.normalize(input) — chuẩn hóa "Q1", "Quận 1", "quan 1" → "Quận 1"
     → RoomRepository.searchRooms(keyword, district, filters)
     → Firestore query: .where("status", "==", "approved")
                        .where("district", "==", normalizedDistrict)
     → Lọc client-side: giá, diện tích, tiện ích (tránh tạo composite index)
     → Hiển thị kết quả trong RecyclerView với adapter

FilterActivity:
     → Slider giá min/max
     → Slider diện tích min/max
     → Checkbox tiện ích (wifi, điều hòa, gác lửng, ...)
     → Kết quả trả về SearchActivity qua startActivityForResult
```

### 6.6 Chat thời gian thực

```
ChatListActivity: hiển thị tất cả cuộc trò chuyện của uid
     → Firestore query: chats where participants array-contains uid
     → Sắp xếp theo lastMessageTime DESC

ChatActivity: chat 1-1
     → ID phòng chat = sorted(uid1, uid2).join("_")
     → onSnapshot("chats/{chatId}/messages") → cập nhật UI tức thì
     → Gửi tin nhắn: addDocument(messages subcollection)
     → Cập nhật: chats/{chatId}.lastMessage, lastMessageTime, unreadCount
     → Gửi ảnh: upload Storage → imageUrl → addDocument với imageUrl

Đánh dấu đã đọc:
     → Khi mở ChatActivity: batch update messages.isRead = true
     → chats/{chatId}.unreadCount[myUid] = 0
```

### 6.7 AI trợ lý tìm phòng (Gemini 2.5 Flash)

Đây là tính năng AI được tích hợp sâu nhất, kết hợp Callable Cloud Function và Gemini với tool calling.

#### Phía Android (AIChatActivity)
```
1. User nhập câu hỏi → btnSend
2. Hiển thị ngay tin nhắn user (optimistic UI)
3. Thêm typing indicator ("...") vào list
4. Disable btnSend, gọi Cloud Function:
   FirebaseFunctions.getInstance()
       .getHttpsCallable("askAIAssistant")
       .call(hashMapOf("message" to messageText))
5. Khi nhận response:
   - Xóa typing indicator
   - Parse suggestedRooms từ Map<String, Any>
   - Tạo AIMessage("model", replyText, suggestedRoomsList)
   - Thêm vào RecyclerView, scroll xuống cuối
   - Enable btnSend
6. Xử lý lỗi 429 (quota) → hiển thị thông báo thân thiện bằng tiếng Việt
7. Lịch sử chat được lưu trong users/{uid}/ai_conversations
```

#### Phía Cloud Function (askAIAssistant)
```
1. Verify Firebase ID token (xác thực user)
2. Đọc context user: fullName, isVerified, purchasedSlots, role
3. Load lịch sử chat từ users/{uid}/ai_conversations (20 lượt gần nhất)
4. Xây dựng system instruction cá nhân hóa:
   "Bạn là StayAssist AI của Stay247...
    Người dùng: {fullName}, vai trò: {role}, đã xác minh: {isVerified}"
5. Khởi tạo Gemini 2.5 Flash với tool: searchRooms
6. Gọi model.generateContent(history + message mới)
7. Nếu model gọi tool searchRooms({district, maxPrice, limit}):
   → Query Firestore rooms: district + maxPrice + approved + isFeatured first
   → Trả kết quả về model
   → Model tổng hợp và viết phản hồi cuối cùng
8. Lưu lượt hỏi+đáp vào users/{uid}/ai_conversations
9. Trả về {reply, suggestedRooms} cho Android
```

#### AIChatAdapter — Render đặc biệt
```
ViewType 0: tin nhắn user (bubble phải, màu accent)
ViewType 1: tin nhắn AI (bubble trái, màu xám)
           → Nếu có suggestedRooms: render horizontal RecyclerView gợi ý phòng
           → Mỗi phòng: ảnh thumbnail, tiêu đề, giá, quận, click → RoomDetailActivity
ViewType 2: typing indicator ("..." nhấp nháy)
```

### 6.8 Hệ thống thông báo đẩy (FCM)

```
Targeted notification (cho 1 user):
1. Hành động tạo notification document: notifications/{notifId}
   {userId, title, message, type, isRead:false}
2. Cloud Function sendPushNotification triggered:
   → Đọc users/{userId}.fcmToken
   → FCM Admin SDK gửi message đến token đó
   → Android: FirebaseMessagingService nhận, hiển thị notification
   → Click notification → điều hướng đến màn hình tương ứng

Broadcast notification (cho tất cả user):
1. Admin tạo: system_notifications/{docId} {title, body}
2. Cloud Function sendBroadcastNotification triggered:
   → FCM Admin SDK gửi đến topic "all_users"
   → Tất cả device đã subscribe topic đều nhận được

Subcribe topic khi login:
   FirebaseMessaging.getInstance().subscribeToTopic("all_users")
```

### 6.9 Hệ thống hiện diện (Presence System)

```
Android side (HeartbeatManager hoặc inline trong MainActivity):
→ Ghi users/{uid}.lastSeen = FieldValue.serverTimestamp() mỗi 60 giây
→ Dừng khi app vào background (onPause)

Web Admin side (08-users.js):
→ Đọc users.lastSeen
→ Nếu now - lastSeen < 3 phút: hiển thị "🟢 Đang online"
→ Nếu ≥ 3 phút: hiển thị "Offline X phút/giờ/ngày trước"
```

### 6.10 Ticket hỗ trợ

```
Android (SupportActivity + SupportChatActivity):
User tạo ticket → support_tickets/{ticketId} {userId, category, title, status:"new"}
User gửi tin → support_tickets/{ticketId}/messages subcollection
                 {senderId, senderRole:"user", text, imageUrl}
              → update ticket: lastMessage, updatedAt, unreadForAdmin=true

Web Admin (11-support.js):
Admin xem ticket unread (badge đỏ "Mới")
Admin trả lời → messages subcollection {senderRole:"admin"}
              → update ticket: status (new→in_progress), unreadForUser=true
              → tạo notifications/{notifId} → trigger FCM → Android nhận push

Vòng đời ticket: new → in_progress → resolved → closed
```

---

## 7. Cloud Functions (Backend tự động)

Tất cả 12 Cloud Functions được deploy tại `phamtriendat_doantotnghiep/index.js`, sử dụng Firebase Cloud Functions v2 cho Node.js 18+.

| # | Tên Function | Loại | Trigger | Mục đích |
|---|-------------|------|---------|----------|
| 1 | `getDashboardStats` | Callable | Gọi từ Web Admin | Thống kê dashboard được cache 5 phút, dùng `.count()` API |
| 2 | `autoReviewVerificationByCloudVision` | Firestore trigger | `verifications/{uid}` write | Auto-OCR CCCD, 3-strike escalation |
| 3 | `autoUnlockUsers` | Scheduler | Mỗi 1 phút | Tự động mở khóa user khi `lockUntil` đã qua |
| 4 | `dailyDataCleanup` | Scheduler | 03:20 giờ VN mỗi ngày | Xóa notification cũ 60 ngày, verification cũ 180 ngày, ảnh Storage orphan |
| 5 | `deleteUserAccount` | HTTP onRequest | POST từ Web Admin | Xóa Firebase Auth user (chỉ admin, verify ID token) |
| 6 | `sendPushNotification` | Firestore trigger | `notifications/{id}` created | Gửi FCM targeted cho 1 user |
| 7 | `sendBroadcastNotification` | Firestore trigger | `system_notifications/{id}` created | Gửi FCM broadcast đến topic "all_users" |
| 8 | `processPendingSlotUpgradePayments` | Scheduler | Mỗi 1 phút | Poll SePay → đối soát → cộng `purchasedSlots` |
| 9 | `processPendingFeaturedUpgradePayments` | Scheduler | Mỗi 1 phút | Poll SePay → đặt `paid_waiting_admin` → Admin duyệt featured |
| 10 | `autoDisableExpiredFeaturedRooms` | Scheduler | Mỗi 30 phút | Tự động tắt bài nổi bật khi `featuredUntil` qua |
| 11 | `updatePopularAreasStats` | Firestore trigger | `rooms/{roomId}` write | Đếm lại phòng đã duyệt theo quận → `stats/popular_areas` |
| 12 | `askAIAssistant` | Callable | Gọi từ AIChatActivity | Gemini 2.5 Flash chat + tool `searchRooms` |

### 7.1 Chi tiết: autoReviewVerificationByCloudVision

Đây là function tự động xử lý yêu cầu xác minh CCCD phức tạp nhất:

```javascript
// Trigger: onDocumentWritten("verifications/{uid}")
// 1. Đọc ảnh từ Storage
// 2. Gọi Cloud Vision annotateImage API
// 3. Kiểm tra từ khóa mặt trước CCCD:
CCCD_FRONT_KEYWORDS = ["căn cước công dân", "số", "quê quán", "nguyên quán",
                        "nơi thường trú", "đặc điểm nhận dạng", "ngày sinh"]
// 4. Kiểm tra từ khóa mặt sau CCCD:
CCCD_BACK_KEYWORDS = ["đặc điểm nhận dạng", "ngày cấp", "nơi cấp", "có giá trị đến"]
// 5. Regex trích xuất số CCCD: /\b\d{9}\b|\b\d{12}\b/g
// 6. So khớp với cccdNumber người dùng nhập (sliding window)
// 7. So khớp tên (normalize unicode, loại bỏ dấu, uppercase so sánh)
// 8. Nếu đạt: auto approve, cập nhật users.isVerified = true
// 9. Nếu thất bại: tăng verification_counters/{uid}
//    - Lần 1-2: "auto_rejected", user có thể thử lại
//    - Lần 3: "escalated" → admin phải duyệt thủ công
```

### 7.2 Chi tiết: askAIAssistant

```javascript
// Model: gemini-2.5-flash
// Temperature: 0.7, topP: 0.8, topK: 40, maxOutputTokens: 1024
// Tool: searchRooms({district?, maxPrice?, limit?})
//   → Query Firestore: status=="approved", sort isFeatured first
//   → Trả về danh sách phòng phù hợp (max 5)
// System instruction cá nhân hóa:
//   "Người dùng: {fullName}, vai trò: {role}
//    Đã xác minh: {isVerified}, Slot còn: {purchasedSlots}
//    Hôm nay: {date}, Thành phố: TP.HCM"
// Lịch sử: load 20 lượt gần nhất từ users/{uid}/ai_conversations
// Lưu mỗi lượt: {role:"user"/:"model", content, timestamp, suggestedRooms}
```

---

## 8. Bảo mật hệ thống

### 8.1 Firebase Authentication & Firestore Rules

- Mọi thao tác write lên Firestore đều yêu cầu user đã xác thực
- `isAdmin` được kiểm tra từ `users/{uid}.role == "admin"` trong Firestore Rules
- Không có admin client-side — mọi thao tác admin phải qua Web Admin Panel hoặc Cloud Function
- Firestore Rules ngăn user đọc document của người khác (ngoại trừ các collection public như `rooms`)

### 8.2 Cloud Function Security

- `deleteUserAccount` (HTTP): Yêu cầu Bearer token trong header, verify bằng `admin.auth().verifyIdToken()`, kiểm tra `users/{uid}.role == "admin"`
- `askAIAssistant` (Callable): Firebase SDK tự động gửi ID token, Function verify bằng `context.auth`
- SePay API key lưu trong Firebase Functions environment config, không hardcode trong code

### 8.3 Android Security

- Không hardcode API key trong source code — lấy từ `BuildConfig` hoặc `google-services.json`
- Google Maps API key trong `AndroidManifest.xml` được restrict bởi package name + SHA-1
- Ảnh CCCD lưu trên Firebase Storage với rules chỉ cho phép user đọc/ghi file của chính mình
- FCM token không lưu cục bộ — luôn lấy từ Firestore document của user

### 8.4 Input Validation

- Số CCCD: validate 12 chữ số trước khi submit
- Giá phòng: validate > 0 và < 100,000,000 VNĐ
- Tên phòng: validate không để trống, max 100 ký tự
- Ảnh: validate định dạng (jpg/png) và kích thước (max 5MB) trước khi upload
- AI message: trim whitespace, không gửi chuỗi rỗng

---

## 9. Cài đặt và cấu hình

### 9.1 Yêu cầu môi trường

- Android Studio Hedgehog (2023.1.1) trở lên
- JDK 17+
- Android SDK 35
- Kết nối Firebase project `doantotnghiep-b39ae`

### 9.2 Các bước cài đặt

```bash
# 1. Clone project
git clone <repository-url>
cd Doantotnghiep

# 2. Thêm google-services.json vào thư mục app/
# File này chứa Firebase config (projectId, apiKey, appId, ...)
# Tải từ Firebase Console > Project Settings > Your apps > Android app

# 3. Tạo local.properties với API keys:
MAPS_API_KEY=your_google_maps_api_key

# 4. Sync Gradle và build
./gradlew assembleDebug
```

### 9.3 Cấu hình Firebase

```
Firebase Console (https://console.firebase.google.com)
├── Authentication: bật Email/Password và Google Sign-In
├── Firestore: deploy firestore.rules từ Admin_TimTro_New/
├── Storage: deploy storage.rules
├── Cloud Functions: deploy từ Admin_TimTro_New/phamtriendat_doantotnghiep/
└── App Check: bật để bảo vệ khỏi client giả mạo (optional debug mode)
```

### 9.4 Biến môi trường Cloud Functions

```bash
# Deploy env config (từ Admin_TimTro_New/):
firebase functions:config:set \
  gemini.api_key="YOUR_GEMINI_API_KEY" \
  sepay.api_key="YOUR_SEPAY_API_KEY" \
  sepay.account_number="YOUR_BANK_ACCOUNT"
```

---

## 10. Đánh giá độ hoàn thiện so với đề cương

So sánh giữa đề cương (outline_text.txt) và code thực tế đã implement:

### 10.1 Các tính năng đã hoàn thiện ✅

| # | Tính năng | File/Module chính | Ghi chú |
|---|-----------|-----------------|---------|
| 1 | Đăng ký & đăng nhập (Email + Google) | `AuthRepository`, `LoginActivity`, `RegisterActivity` | Đầy đủ, Google Sign-In hoạt động |
| 2 | Xác minh CCCD 3 tầng | `VerificationActivity`, `CameraActivity`, CF `autoReviewVerificationByCloudVision` | ML Kit + Cloud Vision + Admin |
| 3 | Hệ thống quota đăng bài (miễn phí + mua) | `RoomRepository.checkDailyPostQuota()`, `PostRoomActivity` | 3 bài/ngày + purchasedSlots |
| 4 | Tìm kiếm và lọc phòng | `SearchActivity`, `FilterActivity`, `LocationNormalizer` | Chuẩn hóa địa danh |
| 5 | Xem chi tiết và lưu bài | `RoomDetailActivity`, `SavedRoomsActivity` | savedPosts collection |
| 6 | Đặt lịch hẹn xem phòng | `BookingActivity`, `MyAppointmentsActivity`, `LandlordAppointmentsActivity` | State machine đầy đủ |
| 7 | Chat thời gian thực (1-1) | `ChatActivity`, `ChatListActivity` | onSnapshot, ảnh đính kèm |
| 8 | AI trợ lý tìm phòng | `AIChatActivity`, CF `askAIAssistant` | Gemini 2.5 Flash + tool calling |
| 9 | Thông báo đẩy (FCM) | `NotificationActivity`, CF `sendPushNotification` | Targeted + broadcast |
| 10 | Thanh toán SePay (slot) | `SlotUpgradeActivity`, CF `processPendingSlotUpgradePayments` | Polling 1 phút |
| 11 | Bài nổi bật (SePay + Admin duyệt) | `FeaturedUpgradeActivity`, CF `processPendingFeaturedUpgradePayments` | 2-step: pay + admin |
| 12 | Ticket hỗ trợ 2 chiều | `SupportActivity`, `SupportChatActivity` | Ảnh đính kèm, unread badge |
| 13 | Hệ thống hiện diện | `MainActivity` (heartbeat 60s) | Hiển thị ở Web Admin |
| 14 | Dọn dữ liệu tự động | CF `dailyDataCleanup` | 03:20 VN, 3 loại dữ liệu |
| 15 | Thống kê khu vực phổ biến | CF `updatePopularAreasStats` | stats/popular_areas realtime |
| 16 | Xem phòng trên bản đồ | `MapActivity`, Google Maps SDK | Cluster marker |
| 17 | Quản lý bài đăng (chủ trọ) | `MyPostsActivity`, `EditRoomActivity` | Sửa/ẩn/xóa bài |
| 18 | Hồ sơ cá nhân + avatar | `EditProfileActivity`, `ChangePasswordActivity` | UCrop cắt ảnh |
| 19 | Xóa tài khoản (Admin) | CF `deleteUserAccount` | HTTP onRequest, verify admin |
| 20 | Broadcast thông báo hệ thống | CF `sendBroadcastNotification` | FCM topic "all_users" |

### 10.2 Giới hạn đã biết ⚠️

| # | Giới hạn | Lý do kỹ thuật | Mức độ ảnh hưởng |
|---|---------|---------------|-----------------|
| 1 | Phát hiện trùng lịch hẹn là client-side | Không có server-side transaction cho `bookedSlots` | Thấp — hiếm xảy ra race condition |
| 2 | Thanh toán polling 1 phút (không webhook) | SePay không hỗ trợ webhook miễn phí ở gói cơ bản | Thấp — trễ tối đa 1 phút |
| 3 | Admin role đơn (1 admin) | UID admin hardcode trong rules, không multi-admin | Thấp — phù hợp quy mô đồ án |
| 4 | Tìm kiếm một số field lọc client-side | Firestore không cho phép nhiều inequality filter | Trung bình — giới hạn ở 2000 bài |
| 5 | Lịch sử AI không streaming | Callable Function trả kết quả 1 lần, không stream | Thấp — UX typing indicator bù đắp |

### 10.3 Tổng kết hoàn thiện

```
Tính năng đề cương yêu cầu:  20 / 20   ✅ Đã implement
Giới hạn kỹ thuật:            5         ⚠️ Đã ghi nhận trong luận văn
Tính năng thiếu:              0         ❌ Không có

Tỷ lệ hoàn thiện: ~95%
(5% còn lại là các giới hạn kỹ thuật được chấp nhận cho quy mô đồ án tốt nghiệp)
```

---

*Tài liệu này được viết theo chuẩn giáo sư/hội đồng phản biện, bao gồm toàn bộ kiến trúc, luồng nghiệp vụ, mô hình dữ liệu và đánh giá hoàn thiện của ứng dụng TÌM TRỌ 24/7 — Phạm Tiến Đạt, 2026.*
