# TIM TRO 24/7

`TIM TRO 24/7` là một ứng dụng Android dành cho bài toán tìm và đăng phòng trọ. Nếu bạn mở folder này lần đầu tiên, hãy hiểu đơn giản rằng app không chỉ là nơi để xem danh sách phòng, mà còn là một hệ thống khá đầy đủ cho cả người thuê lẫn chủ trọ: người thuê có thể tìm phòng, lưu bài, đặt lịch xem, nhắn tin; còn chủ trọ có thể xác minh danh tính, đăng bài, theo dõi trạng thái bài đăng và quản lý lịch hẹn.

Toàn bộ backend của app dựa trên Firebase, vì vậy phần lớn dữ liệu người dùng, bài đăng, ảnh, tin nhắn và thông báo đều được đồng bộ qua Firebase thay vì lưu cục bộ trong máy.

Tài liệu này được viết lại theo đúng cấu trúc mã nguồn hiện tại của project để phục vụ:
- người mới cài và chạy project,
- người phát triển muốn hiểu kiến trúc,
- người kiểm thử cần biết luồng nghiệp vụ,
- và người bảo trì muốn tìm nhanh file cần sửa.

---

## 1. Tổng quan nhanh

`TIM TRO 24/7` là một ứng dụng Android viết bằng **Kotlin** theo hướng **MVVM**, dùng Firebase làm backend chính.

### 1.1 Ứng dụng làm gì
Nếu nhìn từ góc độ người dùng cuối, app này được chia thành hai vai trò chính:
- **Người thuê**: họ vào app để tìm phòng phù hợp, xem ảnh và thông tin, lưu lại bài viết đáng chú ý, đặt lịch xem phòng và nhắn tin với chủ trọ.
- **Chủ trọ**: họ dùng app để xác minh danh tính, đăng bài cho thuê, chỉnh sửa bài đã đăng, theo dõi số lượt đăng, quản lý lịch hẹn và phản hồi người thuê.

Từ đó, các chức năng chính của app gồm:
- Tìm phòng theo khu vực, quận/huyện, bản đồ, giá, diện tích, tiện ích.
- Xem chi tiết phòng với ảnh, thông tin chủ trọ, mô tả, tiện nghi.
- Lưu bài viết.
- Đặt lịch xem phòng.
- Nhắn tin giữa người thuê và chủ trọ.
- Đăng bài cho thuê.
- Xác minh chủ trọ bằng CCCD.
- Theo dõi thông báo và badge số lượng.
- Quản lý hồ sơ cá nhân, đổi mật khẩu, đổi avatar.
- Quản lý bài đăng của chủ trọ.

### 1.2 Các khu vực chính trong app
- `SplashActivity`: màn hình khởi động.
- `WelcomeActivity`: màn hình chào.
- `LoginActivity`, `RegisterActivity`, `ForgotPasswordActivity`, `ResetPasswordActivity`: xác thực tài khoản.
- `MainActivity`: shell chính sau khi đăng nhập.
- `HomeFragment`, `SearchFragment`, `PostFragment`, `ProfileFragment`: 4 tab chính.
- Các màn hình chi tiết và nghiệp vụ ở `View/Auth`.

---

## 2. Cách dùng README này khi bạn nhờ mình "đọc hết code"

Phần này được viết để rút ngắn thời gian mỗi lần bạn quay lại project và muốn mình nắm bắt nhanh toàn bộ codebase.

### 2.1 Nếu bạn chỉ muốn mình hiểu project trước khi sửa
Khi bạn gửi yêu cầu kiểu:
- "đọc hết code để nắm bắt"
- "xem nhanh folder code"
- "nắm cấu trúc project rồi đợi lệnh"

thì mình sẽ ưu tiên đọc theo đúng thứ tự tài liệu này trước, thay vì phải mở từng file rời rạc:
1. `README.md` — tổng quan nhanh, kiến trúc, luồng chính.
2. `MainActivity.kt` và `MyApp.kt` — điểm vào app, điều hướng, presence.
3. `View/Fragment` — các tab chính và luồng nghiệp vụ lớn.
4. `ViewModel` — trạng thái UI và logic từng màn hình.
5. `repository` — các thao tác Firebase / dữ liệu.
6. `res/layout` — tên file UI để biết màn hình nào đang tồn tại.

### 2.2 Tóm tắt siêu ngắn cho việc đọc code
Nếu cần hiểu nhanh app này thì nhớ 4 ý:
- App là Android Kotlin theo MVVM.
- Firebase là backend chính.
- `MainActivity` điều phối shell sau đăng nhập.
- `Home`, `Search`, `Post`, `Profile` là 4 trụ chính của app.

### 2.3 Những file nên đọc trước khi đụng vào logic lớn
- `MainActivity.kt`
- `MyApp.kt`
- `View/Fragment/HomeFragment.kt`
- `View/Fragment/SearchFragment.kt`
- `View/Fragment/PostFragment.kt`
- `View/Fragment/ProfileFragment.kt`
- `ViewModel/HomeViewModel.kt`
- `ViewModel/SearchViewModel.kt`
- `ViewModel/PostViewModel.kt`
- `ViewModel/ProfileViewModel.kt`
- `repository/RoomRepository.kt`
- `repository/AuthRepository.kt`
- `repository/ChatRepository.kt`
- `repository/VerificationRepository.kt`

### 2.4 Ghi chú để tăng tốc cho những lần sau
- Nếu bạn bảo mình đọc "toàn bộ code folder", mình sẽ không cần quét từng file thủ công từ đầu nếu README đã được cập nhật đúng.
- Khi README có đủ map màn hình, map ViewModel và map layout, mình có thể nắm luồng app nhanh hơn nhiều.
- Nếu bạn thêm chức năng mới, hãy cập nhật README ở phần đúng nhóm: auth, home, search, post, profile, chat, booking, verification, support.

### 2.5 Quy ước cập nhật README khi code thay đổi
Khi project có thay đổi lớn, README nên được cập nhật theo đúng lớp thay đổi để lần sau chỉ cần đọc lại tài liệu này là đủ nắm bức tranh chung:
- **Có màn hình mới**: thêm vào map màn hình và map layout.
- **Có ViewModel mới**: thêm vào danh sách ViewModel theo nhóm tính năng.
- **Có Repository mới**: mô tả nguồn dữ liệu, collection Firebase và luồng đọc/ghi.
- **Có thay đổi business rule**: cập nhật ngay phần mô tả nghiệp vụ tương ứng.
- **Có thêm file layout / dialog / item**: cập nhật danh sách layout quan trọng.
- **Có thay đổi Firebase schema**: cập nhật collection, field, status và rule liên quan.

### 2.6 Bức tranh rất nhanh của toàn bộ codebase
Nếu chỉ cần một cái nhìn 10 giây để nhớ project này đang có gì, thì có thể chia như sau:
- **Entry & shell**: `MyApp`, `SplashActivity`, `MainActivity`
- **4 tab chính**: `HomeFragment`, `SearchFragment`, `PostFragment`, `ProfileFragment`
- **Luồng account**: `Login`, `Register`, `ForgotPassword`, `ResetPassword`, `ChangePassword`, `PersonalInfo`
- **Luồng phòng trọ**: `RoomDetail`, `SavedPosts`, `MyPosts`, `EditPost`, `SearchResults`
- **Luồng giao tiếp**: `Conversations`, `Chat`, `Notifications`
- **Luồng nghiệp vụ đặc biệt**: `VerifyLandlord`, `Booking`, `Appointments`, `Support`, `Payment QR`
- **Xử lý dữ liệu**: `ViewModel` + `Repository` + Firebase

### 2.7 Bản đồ lớp logic nên nhớ trước khi đọc code chi tiết
Đây là chuỗi tư duy đúng nhất khi muốn hiểu bất kỳ tính năng nào trong app:

`Activity/Fragment -> ViewModel -> Repository -> Firebase -> LiveData -> UI`

Nói cách khác:
- `Activity/Fragment` bắt sự kiện và hiển thị.
- `ViewModel` giữ trạng thái và ra quyết định nghiệp vụ.
- `Repository` truy cập dữ liệu thật.
- Firebase là nơi lưu / đọc / phát sinh dữ liệu.
- `LiveData` trả kết quả ngược về UI.

### 2.8 Những phần có độ ảnh hưởng cao, cần đọc đầu tiên khi sửa
Nếu đụng vào các file dưới đây thì phải đọc kỹ toàn bộ luồng liên quan trước khi sửa:
- `MainActivity.kt` — badge, FCM, network, user status, điều hướng shell.
- `MyApp.kt` — presence online/offline và heartbeat.
- `PostFragment.kt` — form đăng bài, quota, payment QR, upload ảnh.
- `PostViewModel.kt` — quyền đăng bài, kiểm tra quota, submit post.
- `RoomRepository.kt` — truy xuất dữ liệu phòng, save, search, quota.
- `ChatRepository.kt` — hội thoại, messages, reaction, delete.
- `VerificationRepository.kt` — xác minh CCCD và duyệt chủ trọ.
- `BookingViewModel.kt` và `AppointmentRepository` — lịch hẹn và trạng thái booking.

---

## 3. Công nghệ và thư viện chính

### 2.1 Nền tảng
- Kotlin
- Android SDK
- minSdk 24
- compileSdk 35
- targetSdk 35
- Java 11

### 2.2 Thư viện và dịch vụ quan trọng
- AndroidX Core / AppCompat / Fragment / Lifecycle
- Material Design
- Firebase BoM
- Firebase Auth
- Firebase Firestore
- Firebase Storage
- Firebase Messaging
- Glide
- CameraX
- ML Kit Text Recognition
- Google Maps
- SwipeRefreshLayout
- OkHttp
- Security Crypto

### 2.3 Kiểu kiến trúc
Project đang đi theo mô hình gần với **MVVM**. Nếu bạn chưa quen với cách này thì có thể hiểu ngắn gọn như sau: phần giao diện chỉ lo hiển thị và bắt sự kiện, phần `ViewModel` lo điều khiển trạng thái và kiểm tra dữ liệu, còn phần `Repository` là nơi thực sự nói chuyện với Firebase.

Nói cách khác:
- `View`: `Activity`, `Fragment`, XML layout. Đây là phần người dùng nhìn thấy và chạm vào.
- `ViewModel`: giữ trạng thái UI, validate input, điều phối luồng.
- `Repository`: truy cập Firebase / Storage / Auth / messaging.
- `Model`: cấu trúc dữ liệu.
- `Utils`: helper cho format số, ảnh, thông báo, presence, notification.

---

## 3. Cách chạy project trên máy

Phần này dành cho người lần đầu mở project. Mục tiêu là giúp bạn chạy được app trên máy local một cách ổn định trước khi đi sâu vào từng chức năng.

## 3.1 Yêu cầu cài đặt
Trên máy phát triển cần có:
- **Android Studio** phiên bản ổn định mới
- **JDK 11**
- **Android SDK** tương thích với `compileSdk 35`
- **Gradle** sẽ được Android Studio tải tự động
- Một project Firebase hợp lệ đã gắn với app
- File `google-services.json` đúng project Firebase của bạn

## 3.2 Cấu hình Firebase cần có
App đang dùng các sản phẩm Firebase sau:
- Authentication
- Firestore Database
- Storage
- Cloud Messaging

Bạn cần đảm bảo:
1. Project Firebase đã tạo đúng.
2. App Android đã được thêm vào Firebase.
3. `google-services.json` đã đặt trong thư mục `app/`.
4. Bật các sign-in / rule / index cần thiết theo nghiệp vụ của app.
5. Nếu dùng FCM thì cấu hình thông báo và token lưu trong Firestore.

## 3.3 Chạy ứng dụng
1. Mở project bằng Android Studio.
2. Chờ Gradle sync xong.
3. Kết nối máy thật hoặc mở emulator.
4. Run app.
5. Luồng khởi động thực tế đi qua `SplashActivity`.

## 3.4 Lệnh build thường dùng
Trên Windows PowerShell:
```powershell
./gradlew.bat assembleDebug
./gradlew.bat testDebugUnitTest
./gradlew.bat lintDebug
```

---

## 4. Điểm vào ứng dụng và luồng khởi động

Nếu bạn chỉ mở app lên mà chưa biết nó đi từ đâu tới đâu, phần này sẽ giúp bạn hình dung: app không mở thẳng vào màn chính ngay lập tức, mà đi qua màn khởi động, kiểm tra trạng thái đăng nhập, rồi mới quyết định chuyển tới màn hình phù hợp.

### 4.1 `AndroidManifest.xml`
File manifest khai báo:
- `MyApp` là class `Application`
- `SplashActivity` là màn hình launcher
- `MainActivity` là màn hình shell chính
- Các activity nghiệp vụ khác như chat, booking, profile, search, saved posts, notifications, support...

### 4.2 `MyApp.kt`
`MyApp` theo dõi lifecycle toàn app.

Nhiệm vụ:
- Đếm số activity đang mở.
- Khi app vào foreground, gọi `PresenceManager.goOnline()`.
- Khi app vào background, gọi `PresenceManager.goOffline()`.
- Duy trì heartbeat mỗi 60 giây khi app đang mở.

Ý nghĩa nghiệp vụ:
- Giúp đồng bộ trạng thái online/offline của user.
- Hữu ích cho chat, trạng thái hoạt động và presence.

### 4.3 `SplashActivity`
- Là màn đầu tiên khi mở app.
- Quyết định user đã đăng nhập chưa và điều hướng sang màn phù hợp.

### 4.4 `MainActivity`
File: `app/src/main/java/com/example/doantotnghiep/MainActivity.kt`

`MainActivity` là trung tâm điều phối sau đăng nhập.

#### Nó làm gì
- Gắn `HomeFragment`, `SearchFragment`, `PostFragment`, `ProfileFragment` vào khung `fragmentContainer`.
- Quản lý `BottomNavigationView`.
- Xử lý nút FAB AI.
- Xin quyền thông báo trên Android 13+.
- Lấy FCM token và lưu vào Firestore.
- Lắng nghe trạng thái user bị khóa / bị xóa.
- Kiểm tra bài hết hạn định kỳ theo ngày.
- Load badge lịch hẹn từ `MainViewModel`.
- Load badge thông báo / bài hết hạn / lịch hẹn khi quay lại app.

#### Điều hướng đặc biệt
- `openTab = post` sẽ mở tab đăng bài.
- `action = open_appointments` sẽ mở `ProfileFragment` rồi đi tới `MyAppointmentsActivity`.

#### Các tình huống cần test
- Đăng nhập xong mở đúng tab mặc định.
- Chuyển tab không bị reload sai state.
- Mất mạng hiện Toast.
- Tài khoản bị khóa bị đẩy về login.
- FCM token được cập nhật khi đổi user đăng nhập.

---

## 5. Cấu trúc thư mục chính

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
- `Model`: các class dữ liệu.
- `repository`: thao tác với Firebase / Storage / Auth / Messaging.
- `ViewModel`: logic màn hình.
- `View/Auth`: phần lớn `Activity`.
- `View/Fragment`: 4 tab chính.
- `View/Adapter`: adapter cho RecyclerView / ViewPager.
- `Utils`: helper dùng chung.

---

## 6. Luồng xác thực tài khoản

Đây là phần đầu tiên mà hầu như mọi người dùng đều đi qua. Nó bao gồm tạo tài khoản, đăng nhập, quên mật khẩu và đổi mật khẩu. Với app này, xác thực không chỉ để “vào được app”, mà còn là nền cho rất nhiều quyền truy cập phía sau, ví dụ như đăng bài, xác minh chủ trọ hay dùng chat.

### 6.1 Đăng ký
File liên quan:
- `View/Auth/RegisterActivity.kt`
- `ViewModel/AuthViewModel.kt`
- `repository/AuthRepository.kt`

#### Nghiệp vụ chính
- Nhập họ tên, email, số điện thoại, mật khẩu.
- Validate dữ liệu trước khi gửi.
- Mật khẩu yêu cầu mạnh hơn mức tối thiểu thông thường.
- Khi hợp lệ thì tạo Firebase Auth user và lưu document `users/{uid}`.

#### Dữ liệu user sau đăng ký
Các field thường gặp:
- `role = "user"`
- `isVerified = false`
- `hasAcceptedRules = false`
- `isLocked = false`
- `purchasedSlots = 0`

#### Test nên làm
- Thiếu họ tên.
- Email sai format.
- Số điện thoại không đủ chuẩn.
- Mật khẩu yếu.
- Mật khẩu xác nhận không khớp.
- Email / phone đã tồn tại.

### 6.2 Đăng nhập
File liên quan:
- `View/Auth/LoginActivity.kt`
- `ViewModel/AuthViewModel.kt`
- `repository/AuthRepository.kt`

#### Nghiệp vụ
- Đăng nhập Firebase Auth.
- Sau đó kiểm tra trạng thái user ở Firestore.
- Nếu tài khoản bị khóa hoặc không hợp lệ, app sign out ngay.

### 6.3 Quên mật khẩu
File liên quan:
- `View/Auth/ForgotPasswordActivity.kt`
- `ViewModel/ForgotPasswordViewModel.kt`
- `repository/AuthRepository.kt`

#### Nghiệp vụ
- Yêu cầu nhập đúng email và số điện thoại khớp cùng một user.
- Nếu đúng, gửi email đặt lại mật khẩu.

### 6.4 Đặt lại mật khẩu
File liên quan:
- `View/Auth/ResetPasswordActivity.kt`
- `ViewModel/ResetPasswordViewModel.kt`

#### Nghiệp vụ hiện tại
- App yêu cầu email hợp lệ.
- Gửi email reset password qua Firebase.
- Không có luồng đổi mật khẩu trực tiếp trên app sau khi nhận link.

### 6.5 Đổi mật khẩu
File liên quan:
- `View/Auth/ChangePasswordActivity.kt`
- `ViewModel/AuthViewModel.kt`

#### Nghiệp vụ
- Người dùng nhập mật khẩu cũ, mật khẩu mới, xác nhận.
- Kiểm tra mật khẩu mới đủ mạnh.
- Nếu đúng, đổi mật khẩu và buộc đăng nhập lại.

---

## 7. Hồ sơ cá nhân và avatar

Phần hồ sơ là nơi người dùng nhìn lại và chỉnh sửa thông tin của chính mình. Ở đây app không chỉ hiển thị dữ liệu, mà còn cho phép sửa tên, email, số điện thoại, ngày sinh, giới tính, nghề nghiệp và ảnh đại diện. Nhiều chức năng khác của app cũng dựa trên dữ liệu từ đây, nên nếu phần này sai thì các màn hình khác có thể bị ảnh hưởng theo.

### 7.1 `ProfileFragment`
File: `app/src/main/java/com/example/doantotnghiep/View/Fragment/ProfileFragment.kt`

#### Khi chưa đăng nhập
- Hiện layout guest.
- Có nút đăng nhập / đăng ký.
- Không hiện profile thật.

#### Khi đã đăng nhập
- Hiện avatar, tên, email.
- Hiện badge vai trò / xác minh.
- Hiện badge bài đăng, lịch hẹn, tin nhắn, thông báo.
- Hiện các mục điều hướng:
  - Thông tin cá nhân
  - Đổi mật khẩu
  - Bài lưu
  - Lịch sử thanh toán
  - Bài đăng của tôi
  - Lịch hẹn của tôi
  - Tin nhắn
  - Hỗ trợ
  - Cài đặt
  - Đăng xuất

#### Badge vai trò
`ProfileFragment` đọc `role`, `isVerified`, và `verificationStatus` để hiển thị:
- quản trị viên
- tài khoản đã xác minh
- đang chờ xác minh
- xác minh bị từ chối
- chưa xác minh

### 7.2 `PersonalInfoActivity`
File: `app/src/main/java/com/example/doantotnghiep/View/Auth/PersonalInfoActivity.kt`

#### Nghiệp vụ
- Xem và sửa họ tên, email, số điện thoại, địa chỉ, ngày sinh, giới tính, nghề nghiệp.
- Dữ liệu gốc được load từ `PersonalInfoViewModel`.
- Chỉ khi bấm Edit mới bật chế độ sửa.
- Nếu đổi email thì cần reauthenticate bằng mật khẩu.

#### Test
- Trường bắt buộc rỗng.
- Email sai format.
- SĐT không đúng chuẩn.
- Đổi email phải xác thực lại mật khẩu.
- Cancel phải trả lại giá trị ban đầu.

### 7.3 Avatar
- Chọn ảnh từ thư viện.
- Upload vào Storage.
- Khi thành công, avatar cập nhật ngay trên `ProfileFragment`.
- Nếu mở avatar hiện tại thì đi tới `ImageViewerActivity`.

---

## 8. Xác minh chủ trọ

Đây là bước rất quan trọng nếu người dùng muốn trở thành chủ trọ và đăng tin. Luồng này tồn tại để giảm tài khoản ảo và giúp hệ thống có một lớp kiểm tra danh tính trước khi cho phép đăng bài.

### 8.1 Các file liên quan
- `View/Auth/VerifyLandlordActivity.kt`
- `View/Auth/CccdCameraActivity.kt`
- `ViewModel/VerifyLandlordViewModel.kt`
- `repository/VerificationRepository.kt`

### 8.2 Mục tiêu
- Xác minh danh tính chủ trọ bằng CCCD.
- Chống trùng số CCCD giữa nhiều tài khoản.
- Cho phép escalated sang duyệt thủ công nếu auto-check không đạt.

### 8.3 Luồng nghiệp vụ
1. Người dùng mở màn xác minh.
2. App load thông tin cá nhân có sẵn lên form.
3. Người dùng chụp / chọn ảnh mặt trước và mặt sau CCCD.
4. App kiểm tra CCCD đã tồn tại ở tài khoản khác chưa.
5. App chạy auto-check bằng OCR / phân tích ảnh.
6. Nếu pass -> xác minh thành công.
7. Nếu fail nhưng còn số lần thử -> báo lỗi và cho thử lại.
8. Nếu fail đủ ngưỡng -> đẩy sang admin duyệt thủ công.

### 8.4 Kỳ vọng kiểm thử
- CCCD đã được user khác dùng -> bị chặn.
- Ảnh mờ / thiếu / sai -> không pass.
- Pass ngay -> user được đánh dấu xác minh.
- Fail liên tiếp -> chuyển sang luồng admin review.

### 8.5 `CccdCameraActivity`
- Dùng Camera để chụp CCCD.
- Tối ưu cho thao tác chụp giấy tờ.
- Có thể test việc chụp khi giấy tờ đặt sai hướng hoặc rung tay.

---

## 9. Tab Home

Tab Home là trang đầu tiên người dùng thường nhìn thấy sau khi đăng nhập. Nó không chỉ là một danh sách bài viết, mà còn đóng vai trò như cổng vào các khu vực quan trọng: tìm kiếm nhanh, xem khu vực phổ biến, xem phòng nổi bật, xem phòng mới và đi thẳng tới tra cứu hồ sơ chủ trọ.

File: `app/src/main/java/com/example/doantotnghiep/View/Fragment/HomeFragment.kt`

### 9.1 Chức năng
- Chào người dùng.
- Hiển thị ngày hiện tại.
- Hiển thị badge thông báo.
- Ô tìm kiếm nhanh.
- Tra cứu hồ sơ chủ trọ.
- Khu vực phổ biến.
- Phòng nổi bật.
- Phòng mới.
- Xem thêm bài viết.
- Hiển thị lịch sử tìm kiếm gần đây.

### 9.2 Dữ liệu load từ ViewModel
`HomeViewModel` cung cấp:
- `userName`
- `greeting`
- `currentDate`
- `popularAreas`
- `featuredRooms`
- `newRooms`
- `notificationBadgeCount`
- trạng thái loading của featured / new rooms

### 9.3 Lưu lịch sử tìm kiếm
- Lưu vào `SharedPreferences` với key `SearchHistory`.
- Tối đa 5 từ khóa gần nhất.
- Khi bấm chip lịch sử, app tự đổ query vào ô search và tìm lại.

### 9.4 Test quan trọng
- Không có featured rooms -> hiện empty state.
- Không có new rooms -> hiện empty state.
- Pull to refresh -> reload lại dữ liệu.
- Bấm khu vực phổ biến -> chuyển sang kết quả tìm kiếm.
- Bấm Search Profile -> mở `SearchProfileActivity`.

---

## 10. Tab Search

Tab Search là nơi người dùng chủ động lọc và khoanh vùng phòng theo nhu cầu thực tế. Nếu Home là nơi khám phá, thì Search là nơi đi tìm rất cụ thể: khu vực nào, giá bao nhiêu, diện tích thế nào, có tiện ích gì, hoặc thậm chí là phòng gần một vị trí bản đồ nhất định.

File: `app/src/main/java/com/example/doantotnghiep/View/Fragment/SearchFragment.kt`

### 10.1 Chức năng chính
- Tìm phòng theo khu vực.
- Chọn phường/xã hoặc xã.
- Chọn scope tìm theo phường hoặc quận/huyện.
- Lọc giá.
- Lọc diện tích.
- Lọc số người.
- Lọc tiện ích.
- Tìm gần vị trí bản đồ.
- Chọn bán kính tìm kiếm.

### 10.2 Luồng tìm theo bản đồ
- Người dùng chọn vị trí trên bản đồ qua `LocationPickerActivity`.
- Nếu có tọa độ, app ưu tiên tìm nearby.
- Bán kính mặc định là 2km.
- Có thể tăng đến 5km theo `SeekBar`.

### 10.3 Luồng tìm theo khu vực
- Nếu không có vị trí map, app tìm theo `autoArea`.
- `chipPhuong` và `chipXa` quyết định loại danh sách khu vực.
- `chipScopeWard` và `chipScopeDistrict` quyết định cách lọc scope.

### 10.4 Test nên làm
- Chưa chọn khu vực mà bấm tìm.
- Chọn khu vực không nằm trong danh sách gợi ý.
- Chọn vị trí trên map rồi tìm gần đó.
- Xóa vị trí map.
- Chọn giá custom.
- Chọn tiện ích wifi / điện / nước.
- Chọn loại phòng và giờ giới nghiêm.

### 10.5 `SearchViewModel`
- `searchByQuery()`
- `searchByFilters()`
- `searchNearby()`

Logic quan trọng:
- Chỉ lấy phòng còn chỗ.
- Loại phòng hết chỗ thuê.
- Nearby search dùng Haversine.
- Filter search ưu tiên dữ liệu khớp location, giá, diện tích, số người, loại phòng, tiện ích.

---

## 11. Tab Post

Đây là phần chứa nhiều nghiệp vụ nhất của ứng dụng. Nếu người dùng chỉ là khách thì sẽ không thấy form đăng bài. Nếu đã đăng nhập nhưng chưa xác minh thì app cũng chặn. Chỉ khi hội đủ điều kiện thì form đăng bài mới mở ra, vì vậy luồng này có nhiều trạng thái hơn hầu hết các màn hình khác.

File: `app/src/main/java/com/example/doantotnghiep/View/Fragment/PostFragment.kt`

### 11.1 Vai trò của màn hình
Đây là màn phức tạp nhất của app vì gộp nhiều nghiệp vụ:
- guest / login gate
- xác minh chủ trọ
- quyền đăng bài
- quota đăng bài
- mua thêm lượt đăng
- chọn vị trí map
- upload ảnh phòng
- submit bài đăng

### 11.2 Các trạng thái hiển thị
`PostViewModel` trả về `userObject` để fragment quyết định giao diện:
- chưa đăng nhập -> hiện layout guest
- chưa xác minh -> hiện layout yêu cầu xác minh
- đang chờ duyệt -> hiện trạng thái pending
- bị từ chối -> hiện lý do từ chối
- đã xác minh nhưng đang chờ mở quyền -> hiện countdown
- đủ điều kiện -> hiện form đăng bài

### 11.3 Form đăng bài gồm
- Thông tin chủ trọ
- Tiêu đề
- Địa chỉ / phường / quận
- Vị trí map
- Mô tả
- Giá, diện tích, số người
- Loại phòng
- Đặt cọc
- Dịch vụ tiện ích
- Chỗ để xe
- Bếp / WC / thú cưng / giờ giới nghiêm
- Ảnh phòng

### 11.4 Ảnh phòng
- Chọn nhiều ảnh.
- Preview trực tiếp.
- Giới hạn 10 ảnh.
- Long press để xóa ảnh đã chọn.

### 11.5 Vị trí bản đồ
- Chọn thông qua `LocationPickerActivity`.
- App có thể tự điền địa chỉ nếu ô đang trống.
- Có cố gắng đồng bộ lại phường/xã nếu địa chỉ map chứa tên phù hợp.

### 11.6 Upload và trạng thái submit
`PostViewModel` kiểm tra:
- có đăng nhập chưa
- có verified chưa
- có đang trong thời gian chờ mở quyền không
- có đủ dữ liệu bắt buộc không
- có tối thiểu 1 ảnh không
- tọa độ có hợp lệ không
- còn quota không

Khi đăng thành công:
- hiện progress upload
- hiện dialog thành công
- chuyển tới `PostSuccessActivity`

### 11.7 Quota đăng bài
- User thường được kiểm tra quota theo logic 24 giờ.
- Nếu hết quota, app mở dialog quota limit.
- Có thể mua thêm lượt để tiếp tục đăng.

### 11.8 Mua thêm lượt đăng
Luồng hiện tại trong code:
- Chọn gói cố định hoặc nhập custom slots.
- Tạo document trong `slot_upgrade_requests`.
- Sinh QR VietQR từ thông tin ngân hàng trong fragment.
- Poll trạng thái giao dịch từ Firestore.
- Khi trạng thái là `paid`, nút hoàn tất được mở.
- Sau khi hoàn tất, app reload user để nhận số lượt mới.

Ghi chú quan trọng:
- Đây là luồng mua **lượt đăng bài** đang có trong code.
- Không nên mô tả là luồng featured post nếu trong code chưa có xử lý đó.

---

## 12. Tìm kiếm người dùng / hồ sơ chủ trọ

Phần này giúp người dùng tra cứu thông tin những người đang đăng phòng. Đây là một luồng nhỏ nhưng rất hữu ích, vì nó cho phép kiểm tra uy tín chủ trọ trước khi liên hệ hoặc đặt phòng.

File: `app/src/main/java/com/example/doantotnghiep/View/Auth/SearchProfileActivity.kt`

### Chức năng
- Tìm user theo tên.
- Có debounce 400ms.
- Có loading khi tìm.
- Hiển thị danh sách user với số bài công khai.
- Bấm một user để mở `UserProfileActivity`.

### Test quan trọng
- Ít hơn 2 ký tự -> chưa tìm.
- Kết quả rỗng -> empty state.
- Có nhiều kết quả -> list render đúng.
- Bàn phím ẩn khi bấm search hoặc chạm danh sách.

### `SearchProfileViewModel`
- Gọi `UserRepository.searchUsersByName()`.
- Sau đó đếm số phòng công khai của từng user.
- Cập nhật UI từng bước khi số bài được đếm xong.

---

## 13. Xem hồ sơ người dùng

Khi bấm vào một người dùng từ kết quả tìm kiếm, app mở trang hồ sơ công khai của họ. Trang này thường được dùng để xem nhanh người đó có bao nhiêu bài đang đăng và thông tin cơ bản nào có thể công khai cho người thuê.

File: `app/src/main/java/com/example/doantotnghiep/View/Auth/UserProfileActivity.kt`

### Chức năng
- Xem thông tin người dùng công khai.
- Xem danh sách phòng mà user đó đang đăng.
- Dùng `UserProfileViewModel` để load profile và rooms.

### Test
- User có phòng.
- User không có phòng.
- Avatar có / không có.
- Trạng thái xác minh hiển thị đúng.

---

## 14. Bài đăng của tôi

Đây là khu vực quản trị bài viết của chính chủ trọ. Nó cho phép theo dõi từng bài đang ở trạng thái nào, sửa bài khi bị từ chối, đánh dấu đã cho thuê và xem lại chi tiết từng bài đã đăng.

### 14.1 `MyPostsActivity`
- Hiển thị danh sách bài của chủ trọ.
- Có lọc theo trạng thái.
- Có badge / sort theo logic nghiệp vụ.

### 14.2 `MyPostDetailActivity`
File: `app/src/main/java/com/example/doantotnghiep/View/Auth/MyPostDetailActivity.kt`

#### Chức năng
- Xem chi tiết bài của mình.
- Xem ảnh bài viết.
- Xem trạng thái bài.
- Xem lý do từ chối nếu có.
- Mở `EditPostActivity` khi bài bị từ chối.
- Đánh dấu đã cho thuê nếu bài approved hoặc expired.

#### Test
- `pending`: không hiện nút sửa.
- `rejected`: hiện lý do từ chối và nút sửa.
- `approved` / `expired`: hiện nút đã cho thuê.
- `markAsRented()` thành công thì đóng màn hình và thông báo.

### 14.3 `EditPostActivity`
- Sửa bài đăng đã có.
- Thay ảnh / xóa ảnh cũ / thêm ảnh mới.
- Update ward / district / thông tin phòng.

---

## 15. Bài lưu

Đây là nơi người dùng lưu lại những bài viết quan tâm để xem lại sau. Nó giống như danh sách bookmark trong một ứng dụng mua hàng: không phải bài nào cũng xem ngay, nhưng bài hay thì giữ lại để quay về sau.

### 15.1 `SavedPostsActivity`
- Danh sách bài người dùng đã lưu.
- Khi bài bị xóa hoặc không còn tồn tại, app có thể tự kiểm tra để xử lý.

### 15.2 `SavedPostDetailActivity`
File: `app/src/main/java/com/example/doantotnghiep/View/Auth/SavedPostDetailActivity.kt`

#### Chức năng
- Xem chi tiết bài đã lưu.
- Bỏ lưu bài.
- Hiển thị thông tin chủ nhà, phòng, tiện ích.

### 15.3 `SavedPostsViewModel`
- Load danh sách bài đã lưu.
- Kiểm tra bài còn tồn tại không.
- Xóa bài lưu khi cần.

---

## 16. Chi tiết phòng, đặt lịch và thanh toán liên quan

Nhóm màn này là các hành động “đi tiếp sau khi xem phòng”. Người dùng không chỉ dừng ở việc đọc thông tin, mà có thể đặt lịch xem phòng, theo dõi chi tiết một lịch hẹn cụ thể, hoặc nhìn rõ hơn từng thông tin phòng trước khi ra quyết định.

### 16.1 `RoomDetailActivity`
- Xem chi tiết phòng.
- Xem slider ảnh.
- Xem chủ trọ.
- Có các hành động như lưu bài, đặt lịch, chat.

### 16.2 `BookingActivity`
- Tạo đặt lịch xem phòng.
- Dùng `BookingViewModel`.
- Có kiểm tra slot / thời gian trước khi đặt.

### 16.3 `MyAppointmentsActivity`
- Xem lịch hẹn của user.
- Badge lịch hẹn được quản lý từ `MainViewModel`.

### 16.4 `AppointmentRoomDetailActivity`
- Hiển thị chi tiết phòng gắn với một lịch hẹn cụ thể.

---

## 17. Chat và conversation

Đây là kênh liên lạc trực tiếp giữa người thuê và chủ trọ. Luồng chat trong app không chỉ là gửi text, mà còn có ảnh, trạng thái đã xem, xóa tin nhắn và phản ứng emoji, nên nó gần giống một hệ thống nhắn tin mini hơn là một form chat đơn giản.

### File liên quan
- `View/Auth/ChatActivity.kt`
- `View/Auth/ConversationsActivity.kt`
- `ViewModel/ChatViewModel.kt`
- `repository/ChatRepository.kt`

### Chức năng
- Mở hoặc tạo chat giữa 2 user.
- Hiển thị danh sách hội thoại.
- Lắng nghe messages real-time.
- Gửi text.
- Gửi ảnh.
- Đánh dấu đã đọc.
- Xóa tin nhắn của chính mình.
- Xóa cuộc trò chuyện chỉ trên phía mình.
- Gắn reaction emoji cho message.

### Test
- Chat lần đầu giữa 2 user.
- Chat lại cùng người -> phải mở đúng room cũ.
- Gửi ảnh lớn -> app upload ảnh rồi mới gửi tin.
- Xóa tin của người khác -> bị chặn.
- Đóng mở lại activity -> listener không bị nhân đôi.

---

## 18. Thông báo

Thông báo là phần giúp người dùng không bị bỏ lỡ sự kiện quan trọng như tin nhắn mới, lịch hẹn, trạng thái bài đăng hoặc cập nhật liên quan đến tài khoản. App dùng FCM để nhận push và đồng thời có cơ chế hiển thị badge trong giao diện.

### File liên quan
- `Utils/MyFirebaseMessagingService.kt`
- `Utils/PostNotificationHelper.kt`
- `View/Auth/NotificationsActivity.kt`
- `View/Adapter/NotificationAdapter.kt`

### Nguồn thông báo trong app
- Thông báo hệ thống.
- Thông báo lịch hẹn.
- Thông báo bài đăng.
- Thông báo chat.
- Thông báo trạng thái tài khoản.

### Cách hoạt động
- FCM token được lấy trong `MainActivity`.
- Token được lưu vào Firestore user.
- Khi có push notification, service sẽ xử lý và hiển thị local notification.

---

## 19. Chatbot AI / AI Assistant

Đây là chức năng trợ lý ảo của app, cho phép người dùng hỏi về phòng trọ, nhận gợi ý và xem các phòng liên quan ngay trong hội thoại AI.

### 19.1 File liên quan
- `View/Auth/AIChatActivity.kt`
- `View/Adapter/AIChatAdapter.kt`
- `Model/AIMessage.kt`
- `Model/AIRoom` (nằm trong `AIMessage.kt`)
- `index.js` của Cloud Functions phía web/backend

### 19.2 Luồng hoạt động hiện tại
```text
AIChatActivity
├─ load lịch sử chat từ Firestore
├─ thêm tin nhắn người dùng vào danh sách hiển thị
├─ hiển thị trạng thái "typing"
├─ gọi Cloud Function `askAIAssistant`
├─ nhận response từ AI
├─ parse `suggestedRooms`
└─ lưu lại lịch sử chat vào Firestore
```

### 19.3 Dữ liệu Firestore liên quan
- `users/{uid}/ai_conversations`
  - `role`
  - `content`
  - `timestamp`
  - `suggestedRooms`

### 19.4 Điểm đáng chú ý của chatbot AI
- AI không chỉ trả text mà còn có thể trả danh sách phòng gợi ý.
- Giao diện hiện tại đang giữ logic ngay trong `AIChatActivity`, chưa tách thành `AIChatViewModel`.
- Cloud Function sẽ đọc thông tin thực tế của user trước khi trả lời để cá nhân hóa tư vấn.
- AI có quota sử dụng hằng ngày.
- AI có thể gọi tool logic như tìm phòng, lưu phòng, đặt lịch hẹn, kiểm tra bài đăng và lịch hẹn.

### 19.5 Cloud Function chính
- `askAIAssistant` là endpoint Callable xử lý hội thoại AI.
- Hàm này:
  - kiểm tra đăng nhập,
  - đọc hồ sơ user,
  - load lịch sử hội thoại gần nhất,
  - gửi prompt + history vào Gemini,
  - xử lý function calling,
  - lưu response trở lại Firestore.

### 19.6 Nghiệp vụ mà AI đang hỗ trợ
- tìm phòng theo quận/phường/giá,
- lưu phòng vào yêu thích,
- đặt lịch xem phòng,
- kiểm tra trạng thái bài đăng của tôi,
- kiểm tra lịch hẹn của tôi,
- gợi ý phòng liên quan.

---

## 20. Hỗ trợ, cài đặt và màn phụ

Những màn hình này không phải là luồng chính để tìm hay đăng phòng, nhưng lại rất quan trọng cho trải nghiệm thực tế: hỗ trợ người dùng khi có vấn đề, cung cấp các nội dung phụ trợ, và cho phép điều chỉnh một số cài đặt cá nhân trong app.

### Hỗ trợ
- `SupportTicketsActivity`
- `SupportTicketDetailActivity`
- `SupportViewModel`
- Các dialog tạo ticket và chi tiết trạng thái ticket.

### Cài đặt
- `SettingsActivity`
- Các màn thông tin nội dung, điều khoản, hướng dẫn nội bộ.

### Ảnh và popup
- `ImageViewerActivity`
- `dialog_*`
- `popup_emoji_picker.xml`

---

## 21. ViewModel chính và vai trò

Nếu bạn muốn hiểu logic từng màn hình mà không đọc hết repository, hãy bắt đầu từ đây. Mỗi ViewModel giữ trạng thái riêng cho một nhóm chức năng, nên đây là chỗ tốt nhất để lần theo luồng dữ liệu của app.

### Auth / account
- `AuthViewModel`
- `ForgotPasswordViewModel`
- `ResetPasswordViewModel`
- `PersonalInfoViewModel`

### Home / search / user profile
- `HomeViewModel`
- `SearchViewModel`
- `SearchProfileViewModel`
- `UserProfileViewModel`
- `RoomViewModel`

### Post / my posts / edit post
- `PostViewModel`
- `MyPostsViewModel`
- `MyPostDetailViewModel`
- `EditPostViewModel`

### Booking / appointments
- `BookingViewModel`
- `AppointmentRoomDetailViewModel`

### Profile / saved
- `ProfileViewModel`
- `SavedPostsViewModel`
- `SavedPostDetailViewModel`

### Chat / support
- `ChatViewModel`
- `SupportViewModel`

### Verification / app shell
- `VerifyLandlordViewModel`
- `MainViewModel`

---

## 21. Repository chính và nhiệm vụ

Repository là nơi các thao tác thật sự chạm vào Firebase, Firestore, Storage hoặc Auth. Nếu ViewModel là phần ra quyết định, thì Repository chính là phần đi thực hiện quyết định đó.

### `AuthRepository`
- Đăng nhập, đăng ký, đổi mật khẩu.
- Load / update user info.
- Theo dõi trạng thái user.
- Cập nhật rules acceptance, role, lock status, email re-auth.

### `RoomRepository`
- Đăng bài.
- Load bài.
- Tìm kiếm bài.
- Lưu / bỏ lưu bài.
- Sửa bài.
- Xóa bài.
- Đánh dấu rented.
- Quản lý quota.
- Kiểm tra hết hạn bài.
- Lấy bài theo chủ, theo id, theo filter.

### `ChatRepository`
- Chat room.
- Messages.
- Upload image chat.
- Seen / reaction / delete.
- Danh sách conversation và badge chat.

### `UserRepository`
- Tìm user.
- Đếm phòng công khai.
- Lấy user theo id.
- Load profile công khai.

### `VerificationRepository`
- Kiểm tra CCCD.
- Upload ảnh xác minh.
- Gửi hồ sơ xác minh.
- Auto OCR / auto check.
- Chặn trùng CCCD.

### `SupportRepository`
- Tạo và quản lý ticket hỗ trợ.
- Load ticket list / detail.
- Cập nhật trạng thái ticket.

### `AppointmentRepository`
- Đặt lịch xem phòng.
- Xác nhận / từ chối / sửa lịch.
- Badge lịch hẹn.
- Lịch hẹn của chủ trọ và người thuê.

---

## 22. View/Auth map

Đây là danh sách toàn bộ file trong `View/Auth` để khi bạn nhớ tên màn hình hoặc tính năng, chỉ cần tra ở đây là biết file nào đang điều khiển nó.

### 22.1 Danh sách file `View/Auth`
| File | Vai trò chính |
| --- | --- |
| `SplashActivity.kt` | Màn khởi động, kiểm tra đăng nhập và điều hướng đầu vào |
| `WelcomeActivity.kt` | Màn chào / giới thiệu ban đầu |
| `LoginActivity.kt` | Đăng nhập tài khoản |
| `RegisterActivity.kt` | Đăng ký tài khoản mới |
| `ForgotPasswordActivity.kt` | Yêu cầu khôi phục mật khẩu qua email + phone |
| `ResetPasswordActivity.kt` | Màn đặt lại mật khẩu sau khi nhận email |
| `ChangePasswordActivity.kt` | Đổi mật khẩu trong app |
| `PersonalInfoActivity.kt` | Xem / sửa thông tin cá nhân |
| `SearchProfileActivity.kt` | Tìm kiếm người dùng / chủ trọ công khai |
| `UserProfileActivity.kt` | Hồ sơ công khai của người dùng |
| `SearchResultsActivity.kt` | Hiển thị kết quả tìm phòng |
| `RoomDetailActivity.kt` | Chi tiết phòng trọ |
| `BookingActivity.kt` | Đặt lịch xem phòng |
| `MyAppointmentsActivity.kt` | Danh sách lịch hẹn của tôi |
| `AppointmentRoomDetailActivity.kt` | Chi tiết lịch hẹn gắn với phòng |
| `MyPostsActivity.kt` | Danh sách bài đăng của tôi |
| `MyPostDetailActivity.kt` | Chi tiết một bài đăng của tôi |
| `EditPostActivity.kt` | Sửa bài đăng |
| `SavedPostsActivity.kt` | Danh sách bài đã lưu |
| `SavedPostDetailActivity.kt` | Chi tiết bài đã lưu |
| `ConversationsActivity.kt` | Danh sách hội thoại chat |
| `ChatActivity.kt` | Màn chat 1-1 |
| `NotificationsActivity.kt` | Danh sách thông báo |
| `SupportTicketsActivity.kt` | Danh sách ticket hỗ trợ |
| `SupportTicketDetailActivity.kt` | Chi tiết ticket hỗ trợ |
| `VerifyLandlordActivity.kt` | Màn xác minh chủ trọ bằng CCCD |
| `CccdCameraActivity.kt` | Camera chụp CCCD |
| `LocationPickerActivity.kt` | Chọn vị trí trên bản đồ |
| `ImageViewerActivity.kt` | Xem ảnh phóng to |
| `InfoContentActivity.kt` | Màn nội dung thông tin / bài viết phụ trợ |
| `PostSuccessActivity.kt` | Màn báo đăng bài thành công |
| `PaymentHistoryActivity.kt` | Lịch sử thanh toán / giao dịch |
| `SettingsActivity.kt` | Cài đặt ứng dụng |
| `AIChatActivity.kt` | Chat với AI assistant |

### 22.2 Cách đọc nhóm màn hình này
- Nếu là tài khoản / đăng nhập / cá nhân -> bắt đầu từ nhóm auth.
- Nếu là tìm phòng / chi tiết phòng -> bắt đầu từ `SearchResultsActivity`, `RoomDetailActivity`.
- Nếu là bài đăng của chủ trọ -> bắt đầu từ `MyPostsActivity`, `MyPostDetailActivity`, `EditPostActivity`.
- Nếu là booking / lịch hẹn -> bắt đầu từ `BookingActivity`, `MyAppointmentsActivity`.
- Nếu là chat / thông báo / hỗ trợ -> bắt đầu từ `ConversationsActivity`, `NotificationsActivity`, `SupportTicketsActivity`.
- Nếu là xác minh chủ trọ -> bắt đầu từ `VerifyLandlordActivity` và `CccdCameraActivity`.

---

## 23. Feature flow trees

Phần này mô tả luồng từng chức năng lớn theo dạng cây để bạn nhìn một phát là biết code đi từ đâu đến đâu.

### 23.1 Luồng khởi động app
```text
App launch
├─ `MyApp`
│  ├─ theo dõi lifecycle toàn app
│  ├─ `PresenceManager.goOnline()` khi app vào foreground
│  └─ `PresenceManager.goOffline()` khi app vào background
├─ `SplashActivity`
│  ├─ kiểm tra trạng thái đăng nhập
│  └─ điều hướng sang màn phù hợp
└─ `MainActivity`
   ├─ tải badge
   ├─ lấy FCM token
   ├─ theo dõi user status
   └─ gắn 4 fragment chính
```

### 23.2 Luồng đăng nhập / đăng ký / quên mật khẩu
```text
Auth flow
├─ `LoginActivity`
│  └─ `AuthViewModel.login()`
│     └─ `AuthRepository.login()`
├─ `RegisterActivity`
│  └─ `AuthViewModel.register()`
│     └─ `AuthRepository.register()`
├─ `ForgotPasswordActivity`
│  └─ `ForgotPasswordViewModel.requestPasswordReset()`
│     ├─ `AuthRepository.verifyEmailAndPhone()`
│     └─ `AuthRepository.sendPasswordResetEmail()`
└─ `ChangePasswordActivity`
   └─ `AuthViewModel.changePassword()`
      └─ `AuthRepository.changePassword()`
```

### 23.3 Luồng Home
```text
Home tab
├─ `HomeFragment`
│  ├─ load user name / greeting / date
│  ├─ load popular areas
│  ├─ load featured rooms
│  ├─ load new rooms
│  ├─ load notification badge
│  ├─ search nhanh
│  └─ lịch sử tìm kiếm gần đây
└─ `HomeViewModel`
   ├─ `loadUserName()`
   ├─ `loadPopularAreas()`
   ├─ `loadFeaturedRooms()`
   └─ `loadNewRooms()`
```

### 23.4 Luồng Search
```text
Search tab
├─ `SearchFragment`
│  ├─ chọn khu vực
│  ├─ chọn map location
│  ├─ chọn giá / diện tích / tiện ích
│  └─ submit search
└─ `SearchViewModel`
   ├─ `searchByQuery()`
   ├─ `searchByFilters()`
   └─ `searchNearby()`
      └─ dùng Haversine để tính khoảng cách
```

### 23.5 Luồng đăng bài
```text
Post flow
├─ `PostFragment`
│  ├─ kiểm tra login
│  ├─ kiểm tra user role / verify
│  ├─ hiển thị form theo trạng thái
│  ├─ chọn ảnh
│  ├─ chọn vị trí bản đồ
│  ├─ hiển thị quota / payment QR nếu hết lượt
│  └─ submit bài đăng
└─ `PostViewModel`
   ├─ `loadUserObject()`
   ├─ `checkPrePostQuota()`
   ├─ `postRoom()`
   └─ `RoomRepository.postRoom()`
```

### 23.6 Luồng xác minh chủ trọ
```text
Verification flow
├─ `VerifyLandlordActivity`
│  ├─ load thông tin user
│  ├─ chụp ảnh CCCD trước / sau
│  └─ gửi hồ sơ xác minh
└─ `VerifyLandlordViewModel`
   ├─ check CCCD đã tồn tại chưa
   ├─ auto-check OCR / logic xác minh
   ├─ upload verification
   └─ cập nhật kết quả submit
```

### 23.7 Luồng booking / appointments
```text
Booking flow
├─ `RoomDetailActivity`
│  └─ mở đặt lịch
├─ `BookingActivity`
│  ├─ hiển thị form đặt lịch
│  ├─ kiểm tra conflict / role / trạng thái phòng
│  └─ submit booking
└─ `BookingViewModel`
   ├─ load user info
   ├─ load room / tenant details
   ├─ xác nhận / từ chối / sửa lịch
   └─ cập nhật badge lịch hẹn
```

### 23.8 Luồng chat
```text
Chat flow
├─ `ConversationsActivity`
│  └─ danh sách hội thoại
├─ `ChatActivity`
│  ├─ load room chat
│  ├─ load messages realtime
│  ├─ gửi text / ảnh
│  ├─ reaction emoji
│  ├─ đánh dấu đã đọc
│  └─ xóa tin / xóa cuộc trò chuyện phía mình
└─ `ChatViewModel`
   ├─ điều phối state chat
   └─ gọi `ChatRepository`
```

### 23.9 Luồng saved posts
```text
Saved posts flow
├─ `RoomDetailActivity`
│  └─ toggle save / unsave
├─ `SavedPostsActivity`
│  ├─ load saved list
│  ├─ kiểm tra bài còn tồn tại không
│  └─ auto delete saved item nếu bài đã mất
└─ `SavedPostsViewModel`
   ├─ `loadSavedPosts()`
   ├─ `checkRoomExists()`
   └─ `deleteSavedPost()`
```

### 23.10 Luồng notifications
```text
Notification flow
├─ `MainActivity`
│  ├─ lấy FCM token
│  ├─ xin quyền POST_NOTIFICATIONS
│  └─ lưu token vào `users`
├─ `MyFirebaseMessagingService`
│  └─ nhận push / dựng local notification
├─ `NotificationsActivity`
│  └─ hiển thị danh sách thông báo
└─ badge trong `HomeFragment` / `ProfileFragment`
   └─ cập nhật số lượng chưa đọc
```

---

## 24. Model / Adapter / Utils map

Đây là phần giúp đọc nhanh những class nền tảng mà UI và logic đang dùng chung. Nếu bạn chỉ muốn hiểu app theo cấu trúc dữ liệu và lớp hiển thị, đây là khu vực nên đọc.

### 22.1 `Model`
| File | Vai trò |
| --- | --- |
| `Model/User.kt` | Mô hình tài khoản người dùng, role, verified, avatar, lock, slot, rules |
| `Model/Room.kt` | Mô hình bài đăng phòng trọ, giá, diện tích, tiện ích, trạng thái, tọa độ |
| `Model/Message.kt` | Mô hình tin nhắn chat, nội dung, sender, timestamp, ảnh, reaction |
| `Model/Conversation.kt` | Mô hình cuộc trò chuyện / room chat / metadata hội thoại |
| `Model/SupportTicket.kt` | Mô hình ticket hỗ trợ, tiêu đề, nội dung, trạng thái, thời gian |
| `Model/AIMessage.kt` | Mô hình tin nhắn AI / chatbot |

### 22.2 `Adapter`
| File | Vai trò |
| --- | --- |
| `View/Adapter/RoomAdapter.kt` | Hiển thị danh sách phòng theo kiểu ngang / dọc |
| `View/Adapter/MessageAdapter.kt` | Hiển thị tin nhắn chat gửi / nhận |
| `View/Adapter/ConversationAdapter.kt` | Hiển thị danh sách hội thoại |
| `View/Adapter/UserSearchAdapter.kt` | Hiển thị kết quả tìm kiếm user |
| `View/Adapter/NotificationAdapter.kt` | Hiển thị danh sách thông báo |
| `View/Adapter/SupportTicketAdapter.kt` | Hiển thị danh sách ticket hỗ trợ |
| `View/Adapter/AIChatAdapter.kt` | Hiển thị hội thoại với AI assistant |
| `View/Adapter/AIRoomAdapter.kt` | Hiển thị card phòng trong AI context |

### 22.3 `Utils`
| File | Vai trò |
| --- | --- |
| `Utils/MessageUtils.kt` | Dialog thông báo, loading, success / error / info |
| `Utils/PostNotificationHelper.kt` | Notification cho trạng thái đăng bài / tiến trình |
| `Utils/MyFirebaseMessagingService.kt` | Nhận FCM và xử lý push notification |
| `Utils/PresenceManager.kt` | Online/offline presence của user |
| `Utils/ImageUtils.kt` | Nén ảnh, xử lý ảnh upload |
| `Utils/NumberFormatUtils.kt` | Format số tiền, watcher cho ô nhập tiền |
| `Utils/LocationNormalizer.kt` | Chuẩn hóa tên địa chỉ, ward, district |
| `Utils/AddressData.kt` | Danh sách phường/xã và dữ liệu địa chỉ dùng trong form/search |
| `Utils/AppSettings.kt` | Các cấu hình / hằng số app dùng chung |

---

## 23. Các collection Firestore chi tiết hơn

Phần này giúp bạn hình dung dữ liệu đang được lưu ở đâu trong Firebase. Khi debug, đây là những collection nên mở trước tiên để kiểm tra xem app đã ghi đúng dữ liệu hay chưa.

### 23.1 `users`
Chứa thông tin tài khoản và quyền của người dùng.

Các trường thường gặp:
- `uid`
- `fullName`
- `email`
- `phone`
- `address`
- `birthday`
- `gender`
- `occupation`
- `avatarUrl`
- `role`
- `isVerified`
- `hasAcceptedRules`
- `isLocked`
- `lockReason`
- `lockUntil`
- `postingUnlockAt`
- `purchasedSlots`
- `verifiedAt`
- `createdAt`
- `fcmToken`
- `lastLoginAt`

### 23.2 `rooms`
Chứa bài đăng phòng trọ.

Các trường thường gặp:
- `userId`
- `ownerName`
- `ownerPhone`
- `ownerGender`
- `ownerAvatarUrl`
- `title`
- `ward`
- `district`
- `address`
- `latitude`
- `longitude`
- `description`
- `price`
- `area`
- `peopleCount`
- `roomType`
- `depositMonths`
- `depositAmount`
- `hasWifi`, `hasElectric`, `hasWater`
- `wifiPrice`, `electricPrice`, `waterPrice`
- `hasAirCon`, `hasWaterHeater`, `hasWasher`, `hasDryingArea`, `hasWardrobe`, `hasBed`
- `kitchen`, `bathroom`, `pet`, `petName`, `petCount`
- `genderPrefer`
- `hasMotorbike`, `motorbikeFee`, `hasEBike`, `eBikeFee`, `hasBicycle`, `bicycleFee`
- `curfew`, `curfewTime`
- `status`
- `createdAt`
- `updatedAt`
- `roomCount`
- `rentedCount`
- `imageUrls`

### 23.3 `verifications`
Hồ sơ xác minh CCCD của user.

Các trường thường gặp:
- `uid`
- `fullName`
- `email`
- `cccd`
- `phone`
- `address`
- `frontImageUrl`
- `backImageUrl`
- `status`
- `rejectReason`
- `autoCheckStatus`
- `autoCheckReason`
- `autoCheckRecognizedCccd`
- `autoFailCountToday`
- `escalatedToAdmin`
- `createdAt`
- `updatedAt`

### 23.4 `chats` / `conversations`
Lưu cuộc trò chuyện và metadata hội thoại.

Các trường thường gặp:
- `participants`
- `lastMessage`
- `lastMessageAt`
- `lastSenderId`
- `unreadCount`
- `deletedBy`
- `roomId` hoặc `relatedRoomId`

### 23.5 `messages`
Lưu tin nhắn riêng lẻ trong từng hội thoại.

Các trường thường gặp:
- `chatId`
- `senderId`
- `receiverId`
- `message`
- `imageUrl`
- `type`
- `timestamp`
- `isRead`
- `reactions`
- `deletedFor`

### 23.6 `saved_posts`
Lưu bài viết đã bookmark.

Các trường thường gặp:
- `uid`
- `roomId`
- `ownerId`
- `title`
- `price`
- `address`
- `ward`
- `district`
- `imageUrl`
- `savedAt`

### 23.7 `appointments`
Lưu lịch hẹn xem phòng.

Các trường thường gặp:
- `appointmentId`
- `roomId`
- `roomTitle`
- `landlordId`
- `tenantId`
- `tenantName`
- `date`
- `time`
- `dateDisplay`
- `status`
- `reason`
- `createdAt`
- `updatedAt`
- `isReadByLandlord`
- `isReadByTenant`

### 23.8 `notifications`
Lưu thông báo lịch sử trong app.

Các trường thường gặp:
- `uid`
- `title`
- `message`
- `type`
- `targetId`
- `isRead`
- `createdAt`
- `payload`

### 23.9 `support_tickets`
Lưu ticket hỗ trợ.

Các trường thường gặp:
- `uid`
- `subject`
- `content`
- `category`
- `priority`
- `status`
- `attachments`
- `createdAt`
- `updatedAt`
- `lastReplyAt`

### 23.10 `slot_upgrade_requests`
Lưu yêu cầu mua thêm lượt đăng bài.

Các trường thường gặp:
- `uid`
- `requestId`
- `slots`
- `amount`
- `label`
- `code`
- `transferNote`
- `status`
- `createdAt`
- `updatedAt`
- `expiresAt`
- `paidAt`
- `cancelledAt`

### 23.11 Ghi chú về trạng thái nghiệp vụ
Tùy theo collection và repository, một số field nghiệp vụ bổ sung có thể xuất hiện thêm:
- trạng thái `pending`, `approved`, `rejected`, `expired`, `rented`
- dữ liệu kiểm soát hiển thị trên search
- dữ liệu phục vụ badge bài hết hạn và bài cần gia hạn
- dữ liệu kiểm soát unread / seen cho chat và appointment

---

## 25. Feature business rules map

Phần này là bản tóm tắt trạng thái / quy tắc nghiệp vụ theo từng feature lớn. Nó giúp bạn đọc nhanh để biết màn nào được phép làm gì, khi nào bị chặn, và dữ liệu nào đang điều khiển UI.

### 24.1 Đăng bài
| Trạng thái | Điều kiện | Kết quả UI / nghiệp vụ |
| --- | --- | --- |
| Chưa đăng nhập | `FirebaseAuth.currentUser == null` | Hiện guest layout, không cho vào form |
| Chưa xác minh | `isVerified = false` và không phải admin | Hiện layout yêu cầu xác minh |
| Pending / chờ duyệt | verification status là `pending` / `pending_admin_review` / `queued_manual` | Hiện trạng thái chờ, không cho đăng |
| Bị từ chối | verification status là `rejected` | Hiện lý do từ chối, cho gửi lại xác minh |
| Đã xác minh nhưng chờ mở quyền | `postingUnlockAt > now` | Hiện countdown chờ 24h |
| Hết quota | `checkDailyPostQuota` trả về blocked | Hiện dialog quota limit / mua thêm lượt |
| Đủ điều kiện | login + verified + không bị khóa quota | Hiện form đăng bài |

### 24.2 Xác minh
| Trạng thái | Điều kiện | Kết quả UI / nghiệp vụ |
| --- | --- | --- |
| Chưa đăng nhập | Không có user hiện tại | Chặn submit |
| CCCD đã tồn tại | Số CCCD trùng user khác | Báo lỗi, yêu cầu nhập lại |
| Auto-check pass | OCR/logic pass | Tạo hồ sơ xác minh thành công |
| Auto-check fail nhưng còn lượt | Sai nhưng chưa vượt ngưỡng | Báo reason + remaining retries |
| Auto-check fail và escalated | Vượt ngưỡng lỗi | Gửi admin review |
| Approved | `status = approved` | `isVerified = true` |
| Rejected | `status = rejected` | Hiện lý do từ chối |

### 24.3 Booking
| Trạng thái | Điều kiện | Kết quả UI / nghiệp vụ |
| --- | --- | --- |
| Chưa đăng nhập | Không có user | Chặn thao tác |
| Thiếu dữ liệu | Chưa chọn phòng / thời gian / thông tin bắt buộc | Hiện validation |
| Có conflict | `checkTimeConflicts` trả về xung đột | Báo lịch đã trùng |
| Chờ xác nhận | Appointment mới tạo | Chủ trọ / tenant xem badge |
| Đã xác nhận | Chủ trọ duyệt | Lịch hẹn chuyển sang trạng thái được xác nhận |
| Bị từ chối | Chủ trọ reject | Hiện lý do từ chối |
| Đã hoàn tất / rented | Phòng được đánh dấu thuê | Ẩn khỏi search nếu logic filter yêu cầu |

### 24.4 Chat
| Trạng thái | Điều kiện | Kết quả UI / nghiệp vụ |
| --- | --- | --- |
| Chưa mở chat | Chưa có conversation | Tạo / mở room chat |
| Có tin mới | Message chưa đọc | Tăng badge / unread count |
| Tin đã xem | `isRead = true` | Giảm badge |
| Gửi ảnh | Message type là image | Upload Storage rồi mới ghi message |
| Xóa tin | Người gửi xóa message của mình | Ẩn theo owner / deletedFor |
| Reaction | Có emoji reaction | Cập nhật message metadata |

### 24.5 Saved posts
| Trạng thái | Điều kiện | Kết quả UI / nghiệp vụ |
| --- | --- | --- |
| Đã lưu | Có document trong `saved_posts` | Hiện trong danh sách saved |
| Chưa lưu | Không có document | Nút save ở trạng thái chưa chọn |
| Bài bị xóa | Room không còn tồn tại | Tự xóa saved item |
| Bài đã thuê | `status = rented` hoặc hết chỗ | Có thể bị ẩn tùy logic hiển thị |

### 24.6 Notifications
| Trạng thái | Điều kiện | Kết quả UI / nghiệp vụ |
| --- | --- | --- |
| Có unread | `notificationBadgeCount > 0` | Hiện badge |
| Không có unread | `notificationBadgeCount = 0` | Ẩn badge |
| Token mới | FCM token đổi | Lưu lại vào `users.fcmToken` |
| App foreground | `MyApp` detect started activity > 0 | Presence online |
| App background | `startedActivityCount = 0` | Presence offline |

---

## 26. Các layout quan trọng trong `res/layout`

Phần này dành cho người lần đầu mở project và chưa biết cần cấu hình Firebase như thế nào để app chạy đúng. Vì app phụ thuộc khá nhiều vào Firebase, nếu cấu hình thiếu một bước thì rất dễ gặp lỗi trắng màn hình, không đăng nhập được, không tải được dữ liệu, hoặc upload ảnh thất bại.

### 24.1 Tạo project Firebase
1. Vào Firebase Console.
2. Tạo project mới hoặc chọn project có sẵn.
3. Đặt tên project sao cho dễ nhớ, vì tên này sẽ đi cùng app Android của bạn trong suốt quá trình phát triển.
4. Chờ Firebase tạo xong project.

### 24.2 Thêm ứng dụng Android vào Firebase
1. Trong project Firebase, chọn thêm app Android.
2. Nhập đúng package name của app.
   - Với project này, package name là `com.example.doantotnghiep`.
3. Điền tên app nếu cần.
4. Tiếp tục để Firebase sinh file cấu hình.

### 24.3 Tải và đặt `google-services.json`
1. Sau khi đăng ký app Android, Firebase sẽ cho tải về file `google-services.json`.
2. Copy file này vào thư mục `app/` của project.
3. Đảm bảo đường dẫn đúng là:
   - `Doantotnghiep/app/google-services.json`
4. Nếu file đặt sai chỗ, app sẽ không kết nối đúng Firebase.

### 24.4 Kiểm tra Gradle đã tích hợp Firebase chưa
Để Firebase hoạt động, project cần có cấu hình plugin Google Services và Firebase BoM.

Người duy trì project cần kiểm tra:
1. Gradle cấp project có plugin Google Services.
2. Gradle cấp app có apply Google Services plugin.
3. Dependencies Firebase đã được khai báo đồng bộ bằng BoM.
4. Sync Gradle thành công trước khi chạy app.

Nếu thiếu một trong các bước này, app có thể build fail hoặc chạy nhưng không gọi được Firebase.

### 24.5 Bật Firebase Authentication
App đang dùng Firebase Auth cho các chức năng:
- đăng ký,
- đăng nhập,
- đổi mật khẩu,
- quên mật khẩu,
- reauthenticate khi đổi email.

#### Cách cấu hình
1. Vào Firebase Console.
2. Mở mục **Authentication**.
3. Chọn tab **Sign-in method**.
4. Bật các phương thức mà app cần.

Thông thường với project này, tối thiểu phải có:
- Email/Password.

#### Việc cần test sau khi bật
- Đăng ký có tạo được user không.
- Đăng nhập có tạo session không.
- Quên mật khẩu có gửi email được không.
- Đổi mật khẩu có cập nhật đúng user hiện tại không.

### 24.6 Tạo Firestore Database
Firestore là nơi app lưu phần lớn dữ liệu nghiệp vụ.

#### Bật Firestore
1. Trong Firebase Console, chọn **Firestore Database**.
2. Tạo database mới.
3. Chọn chế độ phù hợp cho giai đoạn phát triển.
4. Chọn region gần người dùng hoặc theo tiêu chuẩn dự án.

#### Vì sao Firestore rất quan trọng
App này không chỉ lưu một vài thông tin đơn giản. Nó dùng Firestore để lưu:
- tài khoản người dùng,
- hồ sơ xác minh,
- bài đăng,
- tin nhắn,
- cuộc trò chuyện,
- bài lưu,
- lịch hẹn,
- thông báo,
- yêu cầu mua thêm lượt đăng bài,
- ticket hỗ trợ.

Nói ngắn gọn: nếu Firestore không chạy đúng, phần lớn app sẽ không hoạt động đúng.

### 24.7 Gợi ý các collection cần có
Dựa trên code hiện tại, các collection thường xuất hiện gồm:
- `users`
- `rooms`
- `verifications`
- `chats`
- `messages`
- `saved_posts`
- `appointments`
- `notifications`
- `support_tickets`
- `slot_upgrade_requests`

Bạn không nhất thiết phải tạo sẵn mọi document bằng tay, nhưng cần hiểu rõ app sẽ đọc và ghi vào các collection này.

### 24.8 Mẫu ý nghĩa từng collection
#### `users`
Lưu hồ sơ tài khoản và quyền của người dùng.

Các trường thường gặp:
- `uid`
- `fullName`
- `email`
- `phone`
- `address`
- `birthday`
- `gender`
- `occupation`
- `avatarUrl`
- `role`
- `isVerified`
- `hasAcceptedRules`
- `isLocked`
- `lockReason`
- `lockUntil`
- `postingUnlockAt`
- `purchasedSlots`
- `fcmToken`
- `createdAt`

#### `rooms`
Lưu tất cả bài đăng phòng.

Các trường thường gặp:
- `userId`
- thông tin chủ trọ
- `title`
- `ward`
- `district`
- `address`
- `latitude`
- `longitude`
- `description`
- `price`
- `area`
- `peopleCount`
- `roomType`
- `depositMonths`
- `depositAmount`
- tiện ích
- `status`
- `createdAt`

#### `verifications`
Lưu hồ sơ xác minh CCCD.

#### `chats` và `messages`
Lưu cuộc trò chuyện và tin nhắn real-time.

#### `saved_posts`
Lưu danh sách bài người dùng đã bookmark.

#### `appointments`
Lưu thông tin đặt lịch xem phòng.

#### `notifications`
Lưu dữ liệu thông báo đã gửi hoặc lịch sử thông báo.

#### `support_tickets`
Lưu ticket hỗ trợ khách hàng.

#### `slot_upgrade_requests`
Lưu giao dịch mua thêm lượt đăng bài.

### 24.9 Cấu hình Firebase Auth chi tiết hơn
Ngoài việc bật Sign-in method, bạn cần nghĩ đến luồng dữ liệu của app:
- Khi user đăng ký, thông tin cơ bản phải được ghi sang `users`.
- Khi user đăng nhập, app sẽ đọc thêm dữ liệu từ Firestore để biết role và trạng thái.
- Khi user đổi email hoặc mật khẩu, cần đảm bảo Firebase Auth và Firestore không bị lệch dữ liệu.

Vì vậy, sau khi bật Auth, bạn nên test các trường hợp:
1. Tạo tài khoản mới.
2. Đăng nhập lần đầu.
3. Đăng xuất và đăng nhập lại.
4. Quên mật khẩu.
5. Đổi mật khẩu.
6. Đổi email.

### 24.10 Cấu hình Firestore Rules
Rules là phần rất quan trọng vì nó quyết định ai được đọc / ghi dữ liệu nào.

Khi setup ban đầu, người phát triển thường làm theo 2 giai đoạn:
- **Giai đoạn dev**: mở quyền vừa đủ để test.
- **Giai đoạn production**: siết lại rule theo từng collection.

Lưu ý:
- Không nên để Firestore mở hoàn toàn trong môi trường thật.
- Nên bảo vệ dữ liệu user, chat, verification, payment request và support ticket cẩn thận.
- Dữ liệu nhạy cảm như CCCD, ảnh xác minh và token nên có rule rõ ràng.

### 24.11 Cấu hình Firebase Storage
Storage dùng cho:
- ảnh phòng,
- avatar,
- ảnh chat,
- ảnh CCCD,
- ảnh phục vụ xác minh.

#### Cách setup
1. Bật **Storage** trong Firebase Console.
2. Chọn region.
3. Kiểm tra rule đọc / ghi.
4. Đảm bảo app có quyền Internet và xử lý URI đúng.

#### Test sau khi bật
- Upload avatar có thành công không.
- Upload ảnh phòng có thành công không.
- Upload ảnh CCCD có thành công không.
- Xóa bài có xóa ảnh liên quan không.
- Xóa tin nhắn có xóa ảnh đính kèm không.

### 24.12 Cấu hình Firebase Messaging
Messaging được dùng để nhận thông báo đẩy.

#### Các bước chính
1. Bật Cloud Messaging trong Firebase.
2. Đảm bảo app có `google-services.json` đúng.
3. Đảm bảo manifest đã khai báo service nhận FCM.
4. Kiểm tra token được lấy và lưu vào `users/{uid}`.

#### Luồng trong app
- `MainActivity` lấy token.
- Token được ghi vào Firestore.
- `MyFirebaseMessagingService` xử lý push notification.

#### Nên test
- Cấp quyền thông báo trên Android 13+.
- Từ chối quyền thông báo.
- Đăng nhập tài khoản khác thì token có cập nhật lại không.
- Nhận notification từ server / console.

### 24.13 Kiểm tra kết nối Firebase sau khi cấu hình
Sau khi làm xong, hãy tự hỏi 5 câu này:
1. App có mở được không?
2. Đăng nhập có thành công không?
3. Có đọc được user từ Firestore không?
4. Có upload được ảnh lên Storage không?
5. Có nhận được thông báo và lưu token không?

Nếu 1 trong 5 câu này sai, nghĩa là phần setup Firebase هنوز chưa hoàn chỉnh.

### 24.14 Những lỗi Firebase thường gặp
- Sai package name giữa app và Firebase.
- Đặt sai vị trí `google-services.json`.
- Chưa bật Authentication.
- Firestore rules chặn đọc / ghi.
- Storage rules chặn upload.
- Thiếu quyền Internet hoặc notification.
- Token FCM không được lưu vào user.
- Data model trong Firestore không khớp với field mà app đang đọc.

---

## 25. Sơ đồ luồng tổng thể của app

Nếu muốn hình dung app chạy như thế nào từ lúc mở đến lúc dùng các chức năng chính, có thể hiểu theo luồng đơn giản dưới đây:

```text
Mở app
  -> SplashActivity
  -> kiểm tra trạng thái đăng nhập
    -> chưa đăng nhập
         -> Welcome / Login / Register / Forgot Password
    -> đã đăng nhập
         -> MainActivity
              -> HomeFragment
              -> SearchFragment
              -> PostFragment
              -> ProfileFragment
```

### Luồng dữ liệu chính trong app
```text
UI (Activity / Fragment)
  -> ViewModel
      -> Repository
          -> Firebase Auth / Firestore / Storage / Messaging
              -> dữ liệu trả về
          <- Repository
      <- ViewModel cập nhật LiveData
  <- UI render lại theo state mới
```

### Luồng nghiệp vụ thường gặp
- Người dùng đăng nhập -> `MainActivity` mở -> app tải badge, profile và trạng thái tài khoản.
- Người dùng tìm phòng -> `SearchFragment` hoặc `HomeFragment` -> `SearchResultsActivity` -> đọc dữ liệu từ `SearchViewModel`.
- Người dùng đăng bài -> `PostFragment` -> `PostViewModel` -> `RoomRepository` -> Firestore / Storage.
- Người dùng xác minh -> `VerifyLandlordActivity` -> `VerifyLandlordViewModel` -> `VerificationRepository`.
- Người dùng chat -> `ChatActivity` -> `ChatViewModel` -> `ChatRepository` -> Firestore real-time.

---

## 26. Bảng map giữa màn hình và file code

Bảng này giúp bạn tìm nhanh file cần sửa khi biết tên màn hình đang thấy trên app.

### 26.1 Màn hình / chức năng và file chính
| Màn hình / chức năng | File chính |
| --- | --- |
| Màn khởi động | `View/Auth/SplashActivity.kt` |
| Màn chào | `View/Auth/WelcomeActivity.kt` |
| Đăng nhập | `View/Auth/LoginActivity.kt` |
| Đăng ký | `View/Auth/RegisterActivity.kt` |
| Quên mật khẩu | `View/Auth/ForgotPasswordActivity.kt` |
| Đặt lại mật khẩu | `View/Auth/ResetPasswordActivity.kt` |
| Đổi mật khẩu | `View/Auth/ChangePasswordActivity.kt` |
| Thông tin cá nhân | `View/Auth/PersonalInfoActivity.kt` |
| Xác minh chủ trọ | `View/Auth/VerifyLandlordActivity.kt` |
| Camera CCCD | `View/Auth/CccdCameraActivity.kt` |
| Trang chủ | `View/Fragment/HomeFragment.kt` |
| Tìm kiếm | `View/Fragment/SearchFragment.kt` |
| Đăng bài | `View/Fragment/PostFragment.kt` |
| Hồ sơ cá nhân | `View/Fragment/ProfileFragment.kt` |
| Tìm kiếm người dùng | `View/Auth/SearchProfileActivity.kt` |
| Hồ sơ user công khai | `View/Auth/UserProfileActivity.kt` |
| Kết quả tìm kiếm | `View/Auth/SearchResultsActivity.kt` |
| Chi tiết phòng | `View/Auth/RoomDetailActivity.kt` |
| Bài đăng của tôi | `View/Auth/MyPostsActivity.kt` |
| Chi tiết bài đăng của tôi | `View/Auth/MyPostDetailActivity.kt` |
| Sửa bài đăng | `View/Auth/EditPostActivity.kt` |
| Bài đã lưu | `View/Auth/SavedPostsActivity.kt` |
| Chi tiết bài đã lưu | `View/Auth/SavedPostDetailActivity.kt` |
| Lịch hẹn của tôi | `View/Auth/MyAppointmentsActivity.kt` |
| Chi tiết lịch hẹn / phòng | `View/Auth/AppointmentRoomDetailActivity.kt` |
| Danh sách chat | `View/Auth/ConversationsActivity.kt` |
| Màn chat | `View/Auth/ChatActivity.kt` |
| Thông báo | `View/Auth/NotificationsActivity.kt` |
| Hỗ trợ | `View/Auth/SupportTicketsActivity.kt` |
| Chi tiết ticket | `View/Auth/SupportTicketDetailActivity.kt` |
| Cài đặt | `View/Auth/SettingsActivity.kt` |
| Ảnh xem lớn | `View/Auth/ImageViewerActivity.kt` |
| Màn chính điều hướng | `MainActivity.kt` |
| Presence / lifecycle app | `MyApp.kt` |

### 26.2 ViewModel nên đọc theo nhóm
| Nhóm | ViewModel |
| --- | --- |
| Auth / account | `AuthViewModel`, `ForgotPasswordViewModel`, `ResetPasswordViewModel`, `PersonalInfoViewModel` |
| Home / search / user profile | `HomeViewModel`, `SearchViewModel`, `SearchProfileViewModel`, `UserProfileViewModel`, `RoomViewModel` |
| Post / my posts / edit post | `PostViewModel`, `MyPostsViewModel`, `MyPostDetailViewModel`, `EditPostViewModel` |
| Booking / appointments | `BookingViewModel`, `AppointmentRoomDetailViewModel` |
| Profile / saved | `ProfileViewModel`, `SavedPostsViewModel`, `SavedPostDetailViewModel` |
| Chat / support | `ChatViewModel`, `SupportViewModel` |
| Verification / app shell | `VerifyLandlordViewModel`, `MainViewModel` |

### 26.3 Repository nên đọc theo nhiệm vụ
| Repository | Nhiệm vụ chính |
| --- | --- |
| `AuthRepository` | Đăng nhập, đăng ký, đổi mật khẩu, load/update user, trạng thái tài khoản |
| `RoomRepository` | Đăng bài, tìm kiếm, load bài, save/unsave, sửa, xóa, quota, rented |
| `ChatRepository` | Hội thoại, tin nhắn, upload ảnh chat, seen, reaction, delete |
| `UserRepository` | Tìm user, lấy user theo id, đếm bài công khai |
| `VerificationRepository` | Kiểm tra CCCD, OCR/auto check, nộp hồ sơ xác minh |
| `SupportRepository` | Ticket hỗ trợ |
| `AppointmentRepository` | Đặt lịch, xác nhận, từ chối, đọc badge, trạng thái lịch hẹn |

### 26.4 Layout / XML nên biết trước
| Nhóm layout | File tiêu biểu |
| --- | --- |
| Màn chính | `activity_main.xml`, `fragment_home.xml`, `fragment_search.xml`, `fragment_post.xml`, `fragment_profile.xml` |
| Auth / hồ sơ / tiện ích | `activity_login.xml`, `activity_register.xml`, `activity_forgot_password.xml`, `activity_reset_password.xml`, `activity_change_password.xml`, `activity_personal_info.xml`, `activity_search_profile.xml`, `activity_user_profile.xml`, `activity_location_picker.xml`, `activity_splash.xml`, `activity_welcome_user.xml` |
| Bài / chi tiết | `activity_room_detail.xml`, `activity_my_posts.xml`, `activity_my_post_detail.xml`, `activity_saved_posts.xml`, `activity_saved_post_detail.xml`, `activity_notifications.xml`, `activity_conversations.xml`, `activity_chat.xml`, `activity_my_appointments.xml`, `activity_appointment_room_detail.xml` |
| Dialog / popup / sheet | `dialog_search_address.xml`, `dialog_rules.xml`, `dialog_review.xml`, `dialog_loading_state.xml`, `dialog_message_state.xml`, `dialog_payment_qr.xml`, `dialog_post_quota_limit.xml`, `dialog_upgrade_slots.xml`, `dialog_featured_upgrade.xml`, `dialog_create_support_ticket.xml`, `popup_emoji_picker.xml` |
| Item / row / card | `item_room_new.xml`, `item_room_featured.xml`, `item_conversation.xml`, `item_message_sent.xml`, `item_message_received.xml`, `item_notification.xml`, `item_support_ticket.xml`, `item_user_search.xml`, `item_booked_slot_row.xml`, `item_host_info.xml` |

### 26.5 Nơi dễ phát sinh bug nhất
| Khu vực | Vì sao cần chú ý |
| --- | --- |
| `MainActivity.kt` | Liên quan badge, FCM, user status, điều hướng, network |
| `PostFragment.kt` | Có form lớn, nhiều trạng thái, quota, payment QR, upload ảnh |
| `PostViewModel.kt` | Kiểm tra quyền đăng bài, quota, upload, state submit |
| `RoomRepository.kt` | Chứa nhiều rule dữ liệu và truy vấn quan trọng nhất |
| `ChatRepository.kt` | Listener real-time, reaction, xóa tin, đồng bộ trạng thái |
| `VerificationRepository.kt` | Xử lý CCCD và luồng xác minh nên rất dễ lệch logic |
| `BookingViewModel.kt` / `AppointmentRepository` | Trạng thái lịch hẹn có nhiều nhánh |
| `ProfileFragment.kt` / `ProfileViewModel.kt` | Badge, avatar, quyền người dùng, logout |

### 26.6 Câu hỏi nhanh để tự kiểm tra mình đã hiểu project chưa
Nếu đọc xong README mà còn trả lời được các câu này thì coi như đã nắm được app:
- App dùng backend gì và vì sao?
- Màn nào là shell sau đăng nhập?
- 4 tab chính là gì?
- Những file nào điều khiển đăng bài, xác minh và booking?
- Layout nào map với màn chính và dialog quan trọng?
- Repository nào là nơi chạm Firebase cho phòng, chat, user, xác minh?
- Dữ liệu `users` và `rooms` đang giữ những field nào?

### 26.7 Quy tắc khi sửa code để README vẫn “đủ thông minh”
Nếu sau này app thay đổi, hãy cập nhật README theo quy tắc này để nó vẫn đủ sức thay thế việc đọc lại toàn bộ folder:
- Có màn mới -> thêm vào map màn hình.
- Có XML mới -> thêm vào map layout.
- Có ViewModel mới -> thêm vào map ViewModel.
- Có Repository mới -> mô tả nhiệm vụ và collection liên quan.
- Có thay đổi business rule -> cập nhật đúng phần nghiệp vụ.
- Có thay đổi field Firestore -> cập nhật schema và collection.
- Có thay đổi flow lớn -> thêm vào sơ đồ tổng thể.

---

## 27. Firebase rules mẫu cho Firestore và Storage

---

## 27. Firebase rules mẫu cho Firestore và Storage

Phần này không phải là rules “copy là chạy ngay trong production”, mà là mẫu cấu trúc để bạn hiểu cách siết quyền đúng hơn cho app này. Khi đưa lên thật, bạn nên chỉnh lại theo yêu cầu bảo mật của dự án.

### 27.1 Firestore rules mẫu
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function signedIn() {
      return request.auth != null;
    }

    function isOwner(userId) {
      return signedIn() && request.auth.uid == userId;
    }

    function isAdmin() {
      return signedIn() &&
        get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
    }

    function isVerifiedUser() {
      return signedIn() && (
        get(/databases/$(database)/documents/users/$(request.auth.uid)).data.isVerified == true ||
        get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin'
      );
    }

    match /users/{userId} {
      allow read: if isOwner(userId) || isAdmin();
      allow create: if signedIn() && request.auth.uid == userId;
      allow update: if isOwner(userId) || isAdmin();
      allow delete: if isAdmin();
    }

    match /rooms/{roomId} {
      allow read: if true;
      allow create: if isVerifiedUser();
      allow update: if isOwner(request.resource.data.userId) || isAdmin();
      allow delete: if isOwner(resource.data.userId) || isAdmin();
    }

    match /verifications/{userId} {
      allow read: if isOwner(userId) || isAdmin();
      allow create: if isOwner(userId);
      allow update: if isOwner(userId) || isAdmin();
      allow delete: if isAdmin();
    }

    match /chats/{chatId} {
      allow read, write: if signedIn();
    }

    match /messages/{messageId} {
      allow read, write: if signedIn();
    }

    match /saved_posts/{docId} {
      allow read, write: if signedIn();
    }

    match /appointments/{docId} {
      allow read, write: if signedIn();
    }

    match /notifications/{docId} {
      allow read, write: if signedIn();
    }

    match /support_tickets/{docId} {
      allow read, write: if signedIn();
    }

    match /slot_upgrade_requests/{docId} {
      allow read, write: if signedIn();
    }
  }
}
```

### 27.2 Storage rules mẫu
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {

    function signedIn() {
      return request.auth != null;
    }

    function isOwner(uid) {
      return signedIn() && request.auth.uid == uid;
    }

    function isAdmin() {
      return signedIn();
    }

    match /avatars/{userId}/{allPaths=**} {
      allow read: if true;
      allow write: if isOwner(userId) || isAdmin();
    }

    match /rooms/{userId}/{allPaths=**} {
      allow read: if true;
      allow write: if isOwner(userId) || isAdmin();
    }

    match /verifications/{userId}/{allPaths=**} {
      allow read: if isOwner(userId) || isAdmin();
      allow write: if isOwner(userId) || isAdmin();
    }

    match /chat/{chatId}/{allPaths=**} {
      allow read, write: if signedIn();
    }
  }
}
```

### 27.3 Lưu ý khi áp dụng rules
- Rules mẫu ở trên chỉ để tham khảo cấu trúc.
- Nếu muốn bảo mật tốt hơn, bạn nên giới hạn đọc / ghi theo đúng membership của chat, appointment và ticket.
- Ảnh CCCD, ảnh xác minh và file nhạy cảm không nên mở public nếu không thật sự cần.
- Với `rooms`, nên kiểm tra `userId` của bài viết có khớp người đang đăng hay không.
- Với `users`, chỉ cho user sửa dữ liệu của chính mình, admin mới có quyền can thiệp rộng hơn.

---

## 28. Hướng dẫn chạy project từ A đến Z trên máy Windows

Phần này dành cho người lần đầu cài project lên máy Windows và muốn chạy được app một cách ổn định. Mục tiêu là sau khi làm xong các bước bên dưới, bạn có thể mở project, sync Gradle, cấu hình Firebase, build thành công và chạy app trên emulator hoặc máy thật.

### 28.1 Chuẩn bị môi trường trên Windows

Trước khi mở project, bạn nên đảm bảo máy đã có đủ các thành phần sau:

#### Android Studio
- Cài Android Studio bản ổn định mới.
- Khi cài, nhớ chọn đầy đủ Android SDK, Platform Tools, Emulator và các thành phần mặc định mà Android Studio đề xuất.
- Nên để Android Studio tự quản lý JDK đi kèm nếu project không yêu cầu JDK ngoài.

#### JDK
- Project đang dùng Java 11.
- Nếu máy bạn có nhiều JDK, hãy kiểm tra Android Studio đang trỏ đúng về JDK 11.
- Nếu sync Gradle lỗi liên quan tới Java version, đây là chỗ đầu tiên cần kiểm tra.

#### Emulator hoặc điện thoại thật
Bạn có hai cách chạy app:
- **Emulator**: tiện cho test nhanh, nhưng có thể nặng máy.
- **Máy thật**: ổn định hơn khi test camera, bản đồ, upload ảnh và notification.

Nếu test các màn như `CccdCameraActivity`, `LocationPickerActivity` hoặc upload ảnh phòng, máy thật thường cho kết quả thực tế hơn.

### 28.2 Mở project đúng cách
1. Mở Android Studio.
2. Chọn **Open**.
3. Trỏ tới folder gốc của repo:
   - `C:\Users\tiend\AndroidStudioProjects\Doantotnghiep`
4. Chờ Android Studio index project xong.
5. Chờ Gradle sync tự chạy.

#### Nếu mở sai folder
Nếu bạn mở nhầm thư mục con, Android Studio có thể không thấy đầy đủ `settings.gradle`, `gradlew`, `app/` hoặc cấu hình Firebase. Khi đó project dễ bị lỗi sync hoặc thiếu module.

### 28.3 Kiểm tra cấu hình Gradle trước khi chạy
Sau khi mở project, hãy kiểm tra những điểm sau:
- Gradle sync có chạy thành công không.
- Không có lỗi dependency bị thiếu.
- Không có lỗi version xung đột giữa compileSdk, plugin và thư viện.
- Module `app` có được nhận đúng không.

Nếu project chưa sync xong mà đã bấm Run, bạn có thể gặp lỗi build thiếu class, thiếu resource hoặc không nhận đúng manifest.

### 28.4 Cấu hình Firebase từ đầu
Vì app dùng Firebase làm backend nên đây là bước rất quan trọng.

#### Bước 1: Tạo project Firebase
1. Mở Firebase Console.
2. Tạo project mới hoặc chọn project có sẵn.
3. Đặt tên project rõ ràng để dễ quản lý sau này.

#### Bước 2: Thêm ứng dụng Android
1. Trong Firebase project, chọn thêm app Android.
2. Nhập đúng package name:
   - `com.example.doantotnghiep`
3. Xác nhận đăng ký app.

#### Bước 3: Tải file cấu hình
1. Tải file `google-services.json`.
2. Copy file vào đúng thư mục:
   - `Doantotnghiep/app/google-services.json`
3. Đừng để file này nằm ngoài thư mục `app`, vì app sẽ không đọc được.

#### Bước 4: Kiểm tra Google Services plugin
- Project cần có plugin Google Services để kết nối Firebase.
- Nếu thiếu plugin, app có thể build thất bại hoặc không tìm được cấu hình Firebase.

#### Bước 5: Sync lại project
- Sau khi đặt file `google-services.json`, bấm sync lại Gradle.
- Nếu không sync lại, Android Studio có thể chưa nhận ra config Firebase mới.

### 28.5 Bật các dịch vụ Firebase cần thiết
Dựa trên code hiện tại, bạn nên bật ít nhất các dịch vụ sau:

#### Authentication
Dùng cho:
- đăng ký,
- đăng nhập,
- quên mật khẩu,
- đổi mật khẩu,
- xác thực phiên người dùng.

Cần bật tối thiểu:
- Email/Password.

#### Firestore Database
Dùng cho:
- thông tin người dùng,
- phòng trọ,
- bài viết,
- chat,
- bài lưu,
- lịch hẹn,
- thông báo,
- xác minh CCCD,
- yêu cầu mua lượt đăng bài,
- ticket hỗ trợ.

#### Storage
Dùng cho:
- ảnh phòng,
- avatar,
- ảnh chat,
- ảnh CCCD,
- ảnh xác minh.

#### Messaging
Dùng cho:
- nhận thông báo đẩy.
- lưu và cập nhật FCM token.

### 28.6 Bật Authentication đúng cách
1. Vào Firebase Console.
2. Chọn **Authentication**.
3. Chọn **Sign-in method**.
4. Bật Email/Password.

#### Test sau khi bật
- Đăng ký có tạo tài khoản không.
- Đăng nhập có thành công không.
- Quên mật khẩu có gửi mail không.
- Đổi mật khẩu có cập nhật đúng user không.

### 28.7 Tạo Firestore Database
1. Vào Firebase Console.
2. Chọn **Firestore Database**.
3. Tạo database.
4. Chọn region phù hợp.
5. Chọn chế độ rule phù hợp với giai đoạn phát triển.

#### Collection chính nên có
- `users`
- `rooms`
- `verifications`
- `chats`
- `messages`
- `saved_posts`
- `appointments`
- `notifications`
- `support_tickets`
- `slot_upgrade_requests`

#### Lưu ý
Nếu Firestore rules quá chặt, app sẽ đọc không được dữ liệu. Nếu rules quá mở, dữ liệu sẽ không an toàn. Trong quá trình phát triển, bạn nên test từng collection riêng.

### 28.8 Tạo Storage
1. Vào Firebase Console.
2. Chọn **Storage**.
3. Khởi tạo bucket.
4. Kiểm tra region.
5. Kiểm tra rule upload / download.

#### File ảnh thường gặp trong app
- avatar user
- ảnh bài đăng phòng
- ảnh xác minh CCCD
- ảnh chat
- ảnh từ `ImageViewerActivity`

Nếu Storage bị chặn, các luồng như đăng bài, xác minh và đổi avatar sẽ lỗi ngay.

### 28.9 Bật Cloud Messaging
1. Bật Firebase Cloud Messaging.
2. Kiểm tra app đã khai báo service nhận message trong manifest.
3. Kiểm tra token được lấy và lưu vào Firestore trong `users/{uid}`.

#### Test nên làm
- Cấp quyền thông báo.
- Từ chối quyền thông báo.
- Đăng nhập user khác để xem token có cập nhật lại không.
- Gửi thông báo test từ Firebase Console.

### 28.10 Thêm file cấu hình vào project
Ngoài `google-services.json`, bạn nên kiểm tra thêm:
- package name trong manifest có đúng không.
- `android:name` của `application` có trỏ tới `MyApp` không.
- Mọi `activity` cần thiết có được khai báo trong manifest chưa.
- Nếu dùng bản đồ thì API key Google Maps có được khai báo đúng không.

### 28.11 Chạy app lần đầu
Sau khi cấu hình xong:
1. Chọn thiết bị chạy.
2. Bấm Run.
3. Đợi app cài vào emulator hoặc máy thật.
4. Kiểm tra app mở tới `SplashActivity`.
5. Đăng nhập hoặc đăng ký để đi tiếp.

### 28.12 Checklist kiểm tra sau khi chạy thành công
Khi app đã mở được, hãy kiểm tra theo thứ tự:
- Màn splash có mở đúng không.
- Đăng nhập có được không.
- `MainActivity` có mở đúng sau khi login không.
- Home có load dữ liệu không.
- Search có lấy được danh sách khu vực không.
- Post có load được form không.
- Profile có hiện avatar và badge không.
- Ảnh có upload được không.
- Chat có gửi được message không.
- Notification badge có cập nhật không.

### 28.13 Các lỗi thường gặp và cách nghĩ để xử lý

#### 1. Lỗi sync Gradle
Biểu hiện:
- Android Studio báo đỏ ở đầu file.
- Project không build được.

Nguyên nhân thường gặp:
- thiếu plugin,
- sai version Gradle,
- thiếu JDK 11,
- dependency xung đột.

#### 2. Lỗi không nhận Firebase
Biểu hiện:
- app chạy nhưng không đọc được dữ liệu.
- đăng nhập hoặc load dữ liệu thất bại.

Nguyên nhân thường gặp:
- đặt sai `google-services.json`.
- package name không khớp.
- chưa sync sau khi thêm file config.

#### 3. Lỗi Firestore permission denied
Biểu hiện:
- đọc hoặc ghi dữ liệu bị từ chối.

Nguyên nhân:
- Firestore rules chưa cho phép.
- user chưa đăng nhập.
- rule kiểm tra role / owner chưa đúng.

#### 4. Lỗi upload ảnh thất bại
Biểu hiện:
- avatar, ảnh phòng hoặc ảnh CCCD không tải lên được.

Nguyên nhân:
- Storage rules chặn.
- URI ảnh không hợp lệ.
- mất mạng.
- file ảnh quá nặng hoặc xử lý ảnh lỗi.

#### 5. Lỗi notification không về
Nguyên nhân thường gặp:
- chưa cấp quyền thông báo trên Android 13+.
- FCM token chưa được lưu.
- service messaging chưa khai báo đúng.
- máy thật / emulator không hỗ trợ hoặc bị chặn thông báo.

#### 6. Lỗi map không mở đúng
Nguyên nhân:
- Google Maps API key sai.
- chưa bật API cần thiết trong Google Cloud.
- cấu hình billing hoặc restriction chưa đúng.

#### 7. Lỗi camera CCCD
Nguyên nhân:
- thiếu quyền camera.
- máy không hỗ trợ camera như kỳ vọng.
- ảnh đầu vào quá tối hoặc rung mạnh.

### 28.14 Mẹo debug nhanh khi gặp lỗi
- Đọc Logcat trước.
- Xem lỗi nằm ở `View`, `ViewModel`, `Repository` hay Firebase.
- Kiểm tra Firestore có đúng document / field đang được đọc không.
- Kiểm tra Storage có đúng đường dẫn upload không.
- Kiểm tra user đã đăng nhập thật chưa.
- Nếu là lỗi luồng dữ liệu, hãy lần ngược từ màn hình về ViewModel rồi về Repository.

---

## 29. Test / build / kiểm tra chất lượng

Một project có nhiều luồng như app này rất dễ phát sinh lỗi ở những chỗ tưởng nhỏ, ví dụ null data, listener trùng, badge không cập nhật hay trạng thái loading không tắt. Vì vậy phần build và test nên được chạy đều đặn sau mỗi thay đổi lớn.

### 24.1 Build
```powershell
./gradlew.bat assembleDebug
```

### 24.2 Unit test
```powershell
./gradlew.bat testDebugUnitTest
```

### 24.3 Lint
```powershell
./gradlew.bat lintDebug
```

### 24.4 Nên kiểm tra gì khi sửa code
- Null safety.
- LiveData observer.
- Trạng thái loading / empty / error.
- Quyền đọc / ghi Firestore / Storage.
- Dữ liệu `status` có đúng chữ không.
- Có update badge / notify khi quay lại màn hình không.
- Có listener bị trùng không.
- Có crash khi dữ liệu thiếu field không.

---

## 25. Gợi ý thứ tự đọc code khi muốn sửa một tính năng

Khi lần đầu bảo trì một chức năng, đừng đọc file ngẫu nhiên. Hãy đi từ màn hình người dùng thấy được, sau đó lần xuống ViewModel và Repository. Cách này giúp bạn hiểu đúng luồng thay vì chỉ nhìn từng đoạn code rời rạc.

### Nếu sửa đăng nhập / tài khoản
1. `LoginActivity`
2. `AuthViewModel`
3. `AuthRepository`
4. `SplashActivity` hoặc `MainActivity` nếu liên quan điều hướng

### Nếu sửa đăng bài
1. `PostFragment`
2. `PostViewModel`
3. `RoomRepository`
4. `Model/Room.kt`

### Nếu sửa tìm kiếm
1. `SearchFragment`
2. `SearchViewModel`
3. `RoomRepository`
4. `SearchResultsActivity`

### Nếu sửa xác minh CCCD
1. `VerifyLandlordActivity`
2. `CccdCameraActivity`
3. `VerifyLandlordViewModel`
4. `VerificationRepository`

### Nếu sửa chat
1. `ChatActivity`
2. `ChatViewModel`
3. `ChatRepository`
4. message / conversation adapters

### Nếu sửa profile / avatar
1. `ProfileFragment`
2. `ProfileViewModel`
3. `UserRepository` / `AuthRepository`

---

## 26. Lưu ý quan trọng khi bảo trì project

Một vài file trong project đang chứa nhiều nghiệp vụ hơn mức bình thường, nên khi sửa cần giữ khả năng đọc hiểu cho người khác. Điều này đặc biệt đúng với `RoomRepository`, `MainActivity`, `PostFragment` và `MyApp` vì các file này ảnh hưởng tới nhiều màn hình cùng lúc.

- `RoomRepository` là file rất lớn và chứa nhiều nghiệp vụ quan trọng nhất của app.
- `MainActivity` không chỉ là màn hình vào app mà còn là nơi quản lý badge, FCM và trạng thái tài khoản.
- `PostFragment` đang chứa nhiều logic nên nếu sửa cần đọc rất kỹ trước khi thay đổi.
- Một số tính năng phụ thuộc trực tiếp vào dữ liệu Firestore, nên khi debug cần kiểm tra cả dữ liệu thật trên Firebase.
- `MyApp` là nơi tác động đến presence, nên lỗi ở đây có thể làm chat / trạng thái online bị sai.

---

## 27. Tóm tắt nhanh

Nếu bạn chỉ muốn nhớ phần cốt lõi của app này, hãy giữ trong đầu bốn tab chính và một backend chung:
- Home: khám phá nội dung.
- Search: lọc và khoanh vùng.
- Post: đăng tin và quản lý quyền đăng.
- Profile: quản lý tài khoản và các tiện ích cá nhân.
- Firebase: nơi toàn bộ dữ liệu quan trọng được lưu và đồng bộ.

- **Home**: xem bài nổi bật, bài mới, khu vực phổ biến.
- **Search**: tìm phòng theo khu vực, bản đồ, filter.
- **Post**: đăng bài, xác minh, quota, mua thêm lượt.
- **Profile**: hồ sơ, bài lưu, bài của tôi, lịch hẹn, chat, cài đặt.
- **Firebase**: backend chính của toàn app.

Nếu muốn hiểu một tính năng, hãy đọc theo thứ tự:

`Activity/Fragment -> ViewModel -> Repository -> Firestore/Storage/Auth`

---

## 28. Kết luận

Đây là một ứng dụng Android có nghiệp vụ tương đối đầy đủ cho bài toán phòng trọ. Điểm đáng chú ý của project không chỉ là có nhiều màn hình, mà là các màn hình đó liên kết với nhau bằng một luồng dữ liệu khá chặt chẽ: người dùng đăng nhập, được phân quyền theo trạng thái tài khoản, xác minh nếu cần, rồi mới đi tới đăng bài, tìm phòng, chat, đặt lịch và quản lý thông báo.

Nếu bạn mới mở project lần đầu, hãy đọc README theo đúng thứ tự sau:
1. Phần cài đặt để chạy được project.
2. Phần khởi động app để hiểu `SplashActivity`, `MainActivity` và `MyApp`.
3. Phần xác thực tài khoản.
4. Phần tab Home / Search / Post / Profile.
5. Phần Firebase collections và repository.

Tài liệu này được viết để không chỉ liệt kê chức năng, mà còn giúp bạn hiểu vì sao app hoạt động như vậy và dữ liệu đang đi qua những lớp nào.

Nếu bạn muốn, ở lượt tiếp theo mình có thể làm thêm một bản:
1. ngắn gọn hơn để nộp đồ án,
2. song ngữ Việt - Anh,
3. hoặc thêm phần **Firebase setup từng bước** gồm Auth, Firestore, Storage, Messaging và rules mẫu.
