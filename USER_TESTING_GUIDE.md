# KỊCH BẢN KIỂM THỬ VÀ LUỒNG LOGIC CHI TIẾT TOÀN HỆ THỐNG

*Tài liệu kiểm thử thủ công cho QA, developer và người xác nhận nghiệp vụ.*

Tài liệu này được viết lại dựa trên cấu trúc code hiện tại của app Android trong repo `Doantotnghiep`, đặc biệt là các màn hình trong `View/Auth`, các fragment chính ở `View/Fragment`, các `ViewModel`, `Repository`, và các layout trong `res/layout`.

Mục tiêu của tài liệu:
- Giúp người test đi đúng luồng màn hình.
- Mô tả rõ dữ liệu nào được nhập, dữ liệu nào được hiển thị, và dữ liệu nào được lưu lên Firebase.
- Chỉ ra các điểm cần kiểm thử kỹ vì liên quan đến xác thực, đăng bài, xác minh chủ trọ, tìm kiếm, chat, lưu bài viết và thông báo.
- Làm rõ các trạng thái UI: loading, empty state, success dialog, error dialog, badge, nút bị ẩn/hiện.

---

## 0. Phạm vi hệ thống cần kiểm thử

### 0.1 Các khu vực chính trong app
- `MainActivity`: điều hướng 4 tab chính bằng bottom navigation.
- `HomeFragment`: trang chủ, tìm kiếm nhanh, khu vực phổ biến, phòng nổi bật, phòng mới.
- `SearchFragment`: bộ lọc tìm phòng theo khu vực, giá, bản đồ, diện tích, tiện ích.
- `PostFragment`: đăng phòng cho thuê, xác minh chủ trọ, quota đăng bài, thanh toán nâng lượt đăng.
- `ProfileFragment`: hồ sơ cá nhân, đổi avatar, đổi mật khẩu, bài đăng của tôi, lịch hẹn, tin nhắn, bài lưu.
- Các màn hình auth: `LoginActivity`, `RegisterActivity`, `ForgotPasswordActivity`, `ResetPasswordActivity`, `ChangePasswordActivity`, `PersonalInfoActivity`, `SearchProfileActivity`, `LocationPickerActivity`, `VerifyLandlordActivity`, `MyPostDetailActivity`, `SavedPostDetailActivity`.

### 0.2 Các nguồn dữ liệu chính
- Firebase Authentication: đăng nhập / đăng ký / đổi mật khẩu / đăng xuất.
- Firestore: users, rooms, verifications, chats, messages, saved_posts, notifications, appointments, support tickets, quota / slot upgrade requests.
- Firebase Storage: ảnh phòng, ảnh CCCD, avatar, ảnh chat.

### 0.3 Các trạng thái UI cần chú ý
- Loading dialog / progress bar / skeleton.
- Empty state khi không có dữ liệu.
- Badge số lượng thông báo / lịch hẹn / bài đăng.
- Nút ẩn/hiện theo role, theo trạng thái duyệt, theo quota.
- Dialog xác nhận ở các thao tác nguy hiểm: đăng xuất, xóa, đánh dấu đã cho thuê, mua lượt, xác minh.

---

## 1. Điều hướng tổng thể của app

### 1.1 `MainActivity`
`MainActivity` là màn hình khung chứa toàn bộ app sau khi người dùng đăng nhập.

#### Luồng chính
1. App mở `MainActivity`.
2. Load fragment mặc định là `HomeFragment`.
3. Người dùng có thể chuyển tab:
   - Home
   - Search
   - Post
   - Profile
4. Nút FAB AI hiện dialog thông báo chatbot AI đang phát triển.

#### Dữ liệu và kiểm tra logic
- App xin quyền thông báo trên Android 13+.
- App lấy FCM token và lưu vào document `users/{uid}`.
- App theo dõi trạng thái online/offline bằng `PresenceManager` thông qua `MyApp`.
- App kiểm tra trạng thái user bị khóa / bị xóa:
  - Nếu tài khoản không còn hợp lệ, app sign out và đẩy về `LoginActivity`.
- App kiểm tra bài đăng hết hạn và badge lịch hẹn khi vào app hoặc khi quay lại.

#### Test case
- Mở app khi chưa có user đăng nhập -> không nên vào `MainActivity` mà phải đi qua login.
- Đăng nhập thành công -> vào `HomeFragment`.
- Chuyển tab liên tục -> fragment load đúng, không mất state nghiêm trọng.
- Tắt mạng khi app đang chạy -> hiện Toast mất kết nối Internet.
- User bị khóa từ admin -> app tự logout.

---

## 2. App lifecycle và trạng thái online/offline

### 2.1 `MyApp.kt`
`MyApp` là class `Application` dùng để theo dõi toàn bộ vòng đời activity.

#### Luồng logic
- Khi app đi vào foreground:
  - gọi `PresenceManager.goOnline()`
  - bắt đầu heartbeat mỗi 60 giây
- Khi app đi vào background:
  - hủy heartbeat
  - gọi `PresenceManager.goOffline()`

#### Test case
- Mở app từ trạng thái tắt hoàn toàn -> user được set online.
- Chuyển app sang nền -> trạng thái offline được ghi nhận.
- Quay lại app -> online được cập nhật lại.
- Tắt app hoàn toàn -> heartbeat dừng.

---

## 3. Module xác thực tài khoản

### 3.1 Đăng ký tài khoản
Màn hình: `RegisterActivity`

#### Mục tiêu test
Đảm bảo người dùng chỉ tạo được tài khoản hợp lệ, không trùng và không thiếu dữ liệu.

#### Các kiểm tra nên thử
1. Bỏ trống họ tên.
2. Bỏ trống email.
3. Email sai định dạng.
4. Bỏ trống số điện thoại.
5. Số điện thoại không đủ 10 số hoặc không bắt đầu bằng 0.
6. Mật khẩu không đạt yêu cầu bảo mật.
7. Mật khẩu và xác nhận không khớp.
8. Đăng ký bằng email đã tồn tại.

#### Kỳ vọng
- Lỗi hiện ngay dưới ô nhập hoặc trong dialog lỗi.
- Không tạo user nếu dữ liệu không hợp lệ.
- Khi đăng ký thành công:
  - tạo auth account
  - tạo document `users/{uid}`
  - mặc định role là user thường
  - chưa xác minh
  - chưa có avatar
  - các trạng thái khác phụ thuộc logic server sau khi tạo tài khoản

---

### 3.2 Đăng nhập
Màn hình: `LoginActivity`

#### Mục tiêu test
Xác minh người dùng đăng nhập đúng và bị chặn nếu tài khoản không hợp lệ.

#### Test case
1. Nhập thiếu email hoặc mật khẩu.
2. Nhập sai mật khẩu.
3. Đăng nhập bằng tài khoản bị khóa.
4. Đăng nhập bằng tài khoản đã bị xóa / không còn quyền truy cập.

#### Kỳ vọng
- Sai thông tin -> hiển thị lỗi rõ ràng.
- Đúng thông tin -> vào app.
- Nếu Firestore báo `isLocked = true` -> logout ngay.
- Luồng khóa / xóa tài khoản được xử lý từ `MainActivity` sau khi đăng nhập thành công.

---

### 3.3 Quên mật khẩu
Màn hình: `ForgotPasswordActivity`

#### Mục tiêu test
Chỉ cho reset khi email và số điện thoại khớp cùng một tài khoản.

#### Test case
1. Nhập email trống.
2. Nhập số điện thoại trống.
3. Nhập email đúng nhưng số điện thoại sai.
4. Nhập đúng cặp email + phone.

#### Kỳ vọng
- Chỉ khi khớp cả email và phone mới gửi email reset.
- Nếu không khớp -> báo lỗi rõ ràng.
- Code hiện tại chỉ kiểm tra email và phone khớp tài khoản, không mô tả thêm logic khóa tài khoản ở màn này.

---

### 3.4 Đặt lại mật khẩu
Màn hình: `ResetPasswordActivity`

#### Ghi chú
Theo code hiện tại, luồng chính là Firebase gửi link reset email.

#### Test case
- Email rỗng -> báo lỗi.
- Gửi link reset thành công -> hiện trạng thái thành công.
- Xử lý đúng màn hình sau khi gửi.

---

### 3.5 Đổi mật khẩu
Màn hình: `ChangePasswordActivity`

#### Mục tiêu test
Người dùng đổi mật khẩu cũ sang mật khẩu mới và được yêu cầu đăng nhập lại.

#### Test case
1. Bỏ trống mật khẩu cũ.
2. Bỏ trống mật khẩu mới.
3. Mật khẩu mới yếu.
4. Bỏ trống xác nhận mật khẩu.
5. Xác nhận không khớp.
6. Mật khẩu mới trùng mật khẩu cũ.
7. Mật khẩu cũ sai.
8. Đổi thành công.

#### Kỳ vọng
- Sau khi đổi thành công:
  - hiện dialog thành công
  - logout
  - chuyển về `LoginActivity`

---

## 4. Module hồ sơ cá nhân

### 4.1 `ProfileFragment`
Đây là khu vực quản lý thông tin tài khoản của người dùng.

#### Khi chưa đăng nhập
- Hiện layout guest.
- Có nút đăng nhập và đăng ký.
- Ẩn phần profile cá nhân.

#### Khi đã đăng nhập
- Hiện ảnh đại diện.
- Hiện họ tên, email.
- Hiện badge vai trò / xác minh.
- Hiện các mục:
  - Thông tin cá nhân
  - Đổi mật khẩu
  - Bài lưu
  - Lịch sử thanh toán
  - Bài đăng của tôi
  - Lịch hẹn
  - Tin nhắn
  - Hỗ trợ
  - Cài đặt
  - Đăng xuất

#### Test case
- User thường: badge chưa xác minh.
- User đã xác minh: badge xác minh.
- Admin: badge quản trị viên.
- Verification pending: badge đang chờ xác minh.
- Verification rejected: badge bị từ chối.

#### Kiểm thử avatar
1. Chọn ảnh đại diện từ thư viện.
2. Upload xong -> ảnh đổi ngay.
3. Nếu upload lỗi -> hiện dialog lỗi.
4. Mở avatar hiện tại -> `ImageViewerActivity`.

#### Kiểm thử badge số lượng
- Badge thông báo.
- Badge lịch hẹn.
- Badge tin nhắn.
- Badge bài đăng của tôi.

---

### 4.2 `PersonalInfoActivity`
Màn hình cập nhật thông tin cá nhân.

#### Mục tiêu test
Người dùng xem và chỉnh sửa thông tin profile.

#### Trường dữ liệu
- Họ tên
- Email
- Số điện thoại
- Địa chỉ
- Nghề nghiệp
- Ngày sinh
- Giới tính

#### Luồng đúng
1. Mở màn hình.
2. Dữ liệu gốc được load lên form.
3. Bấm `Edit` để bật chế độ sửa.
4. Sửa thông tin.
5. Bấm `Save`.
6. Nếu đổi email -> phải xác thực lại mật khẩu.

#### Test case
- Tên trống -> lỗi.
- Email trống -> lỗi.
- Email không có đuôi `@gmail.com` -> lỗi theo logic hiện tại.
- Số điện thoại không đủ chuẩn -> lỗi.
- Đổi email -> hiện dialog nhập mật khẩu.
- Mật khẩu sai -> báo lỗi.
- Lưu thành công -> hiện dialog success.

#### Ghi chú quan trọng
- Khi `Cancel`, toàn bộ dữ liệu phải quay về giá trị gốc.
- Date picker chỉ bật khi đang ở chế độ edit.

---

## 5. Module xác minh chủ trọ eKYC

### 5.1 `VerifyLandlordActivity` và `VerifyLandlordViewModel`
Đây là luồng xác minh danh tính chủ trọ bằng CCCD.

#### Mục tiêu test
- Tách riêng luồng tự động và luồng duyệt thủ công.
- Chặn CCCD bị đăng ký trùng.
- Cho phép user gửi ảnh mặt trước / mặt sau CCCD.

#### Dữ liệu chính
- Họ tên
- Email
- Số CCCD
- Số điện thoại
- Địa chỉ
- Ảnh mặt trước CCCD
- Ảnh mặt sau CCCD

#### Luồng logic tổng quát
1. Load thông tin user hiện tại lên form.
2. User nhập dữ liệu và chọn ảnh mặt trước / mặt sau CCCD.
3. App kiểm tra CCCD đã tồn tại chưa.
4. App chạy kiểm tra tự động và quyết định kết quả pass / fail / escalated.
5. Nếu pass -> gửi hồ sơ xác minh tự động.
6. Nếu fail quá số lần hoặc vượt ngưỡng hệ thống -> đẩy sang admin duyệt thủ công.

#### Test case quan trọng
- CCCD trống.
- CCCD nhập sai format.
- CCCD đã được tài khoản khác sử dụng.
- Thiếu ảnh mặt trước / mặt sau.
- Ảnh mờ, ảnh nghiêng, ảnh quá tối.
- Kết quả kiểm tra tự động không đạt.
- Pass ngay từ lần đầu.
- Fail nhưng chưa đủ số lần để escalated.
- Fail quá số lần cho phép -> đẩy admin.

#### Kỳ vọng UI
- Có loading trong lúc kiểm tra.
- Có thông báo rõ khi CCCD bị trùng.
- Có thông báo rõ khi pass.
- Có thông báo rõ khi bị đẩy sang admin.

---

### 5.2 `CccdCameraActivity`
Nơi chụp CCCD thông minh.

#### Mục tiêu test
- Camera chụp đúng vùng giấy tờ.
- Không chụp khi khung chưa ổn định.
- Không làm đơ UI.

#### Test case
- CCCD đặt sai hướng.
- CCCD đặt lệch khung.
- Camera chuyển động mạnh.
- Ảnh chụp thành công và crop đúng.

---

## 6. Module đăng bài cho thuê

### 6.1 `PostFragment`
Đây là một trong các luồng nghiệp vụ phức tạp nhất của app.

#### Các nhánh chính
- Chưa đăng nhập -> hiện layout guest.
- Đã đăng nhập nhưng chưa xác minh -> hiện yêu cầu xác minh.
- Đang chờ duyệt xác minh -> hiện trạng thái pending.
- Bị từ chối xác minh -> hiện lý do từ chối.
- Đã xác minh -> hiện form đăng bài.
- Đã được cấp quyền nhưng chưa qua thời gian chờ -> hiện trạng thái chờ mở quyền.
- Hết quota -> hiện dialog mua thêm lượt.

#### Test case theo role / trạng thái
1. Guest chưa login.
2. User chưa xác minh.
3. User đang chờ duyệt.
4. User bị từ chối.
5. User verified nhưng đang trong thời gian chờ sau duyệt.
6. Admin.
7. User có quota free.
8. User hết quota free nhưng còn gói mua thêm.

---

### 6.2 Form đăng bài
#### Dữ liệu cần nhập
- Họ tên chủ trọ
- SĐT chủ trọ
- Giới tính chủ trọ
- Tiêu đề
- Phường/xã
- Địa chỉ cụ thể
- Tọa độ bản đồ
- Mô tả
- Giá thuê
- Diện tích
- Số người ở
- Loại phòng
- Đặt cọc
- Phí internet / điện / nước
- Tiện ích nội thất
- Khu để xe
- Bếp
- Nhà vệ sinh
- Thú cưng
- Giờ giới nghiêm
- Ảnh phòng

#### Test case validate
- Thiếu tiêu đề.
- Thiếu địa chỉ.
- Thiếu vị trí bản đồ.
- Giá = 0.
- Diện tích = 0.
- Không có ảnh.
- Chọn quá 10 ảnh.
- Không chọn phường/xã.

#### Kỳ vọng
- Không cho submit khi thiếu dữ liệu bắt buộc.
- Progress upload hiện theo %.
- Có loading dialog.
- Có thông báo thành công sau khi đăng.

---

### 6.3 Chọn vị trí trên bản đồ
Màn hình: `LocationPickerActivity`

#### Mục tiêu test
Người dùng chọn vị trí thực tế trước khi đăng bài hoặc lọc tìm kiếm.

#### Luồng đúng
- Mở bản đồ.
- Chạm điểm trên map để chọn.
- Hoặc tìm địa chỉ bằng dialog search.
- Xem địa chỉ hiển thị.
- Bấm confirm.

#### Test case
- Không chọn vị trí mà bấm confirm -> báo lỗi.
- Search địa chỉ không tìm thấy -> Toast không tìm thấy.
- Tìm nhiều kết quả -> chọn trong danh sách.
- Mở màn hình với địa chỉ ban đầu từ form đăng bài -> map tự gợi ý.

---

### 6.4 Quota đăng bài
#### Luồng nghiệp vụ
- User thường có 3 lượt đăng bài miễn phí trong mỗi chu kỳ 24 giờ.
- Nếu đã mua thêm lượt, hệ thống vẫn ưu tiên dùng lượt miễn phí trước.
- Nếu hết lượt -> hiện dialog quota limit.

#### Test case
1. User có 0 lượt mua thêm.
2. User có 3 bài trong 24 giờ.
3. User có lượt mua thêm.
4. User bấm đăng khi đang trong thời gian chờ mở quyền sau duyệt.
5. User mở dialog quota, đóng dialog mà không mua.

#### Kỳ vọng
- Hệ thống chặn đúng thời điểm.
- Hiện text giải thích rõ vì sao chưa đăng được.
- Nếu user mua slot thành công -> có thể đăng tiếp.
- Nội dung thông báo quota phải khớp với luồng đang hiển thị trong `PostFragment`.

---

### 6.5 Mua thêm lượt đăng bài
#### Luồng hiện tại
- Chọn gói lượt.
- Tạo giao dịch QR thanh toán.
- Theo dõi trạng thái giao dịch từ Firestore.
- Khi `paid` -> cho phép hoàn tất.

#### Test case
- Chọn gói cố định 3 / 5 / 10 lượt.
- Nhập custom slots.
- Đóng dialog trước khi thanh toán.
- Giao dịch hết hạn.
- Giao dịch bị hủy.
- Giao dịch `paid` thành công.

#### Kỳ vọng
- Dialog QR hiển thị thông tin rõ ràng.
- Khi `paid` thì nút hoàn tất mở.
- Khi giao dịch thành công thì user được cộng slot.
- Nội dung hiện tại là luồng gói lượt đăng bài trong `PostFragment`, chưa thấy logic `featured` hay `paid_waiting_admin` trong code đã đọc.

---

## 7. Module trang chủ

### 7.1 `HomeFragment`
Trang chủ là nơi user nhìn thấy các nội dung chính nhất.

#### Thành phần chính
- Lời chào theo thời gian.
- Ngày hiện tại.
- Badge thông báo.
- Ô tìm kiếm nhanh.
- Tra cứu hồ sơ chủ trọ.
- Khu vực phổ biến.
- Phòng nổi bật.
- Phòng mới.
- Xem thêm bài viết.
- Lịch sử tìm kiếm gần đây.

#### Test case
- Không có dữ liệu nổi bật -> empty state.
- Không có phòng mới -> empty state.
- Có lịch sử tìm kiếm -> chip/tag xuất hiện.
- Bấm chip khu vực phổ biến -> sang kết quả tìm kiếm theo quận/huyện.
- Bấm ô search -> sang màn kết quả.
- Bấm tra cứu hồ sơ chủ trọ -> sang `SearchProfileActivity`.

#### Ghi chú
- Các list phải render ổn định khi kéo refresh.
- Khi đang loading, skeleton phải hiện.
- Khi load xong, skeleton phải tắt.

---

## 8. Module tìm kiếm

### 8.1 `SearchFragment`
Đây là màn hình lọc phòng nâng cao.

#### Chức năng chính
- Tìm theo phường/xã hoặc xã.
- Tìm theo quận/huyện.
- Chọn khu vực bằng AutoComplete.
- Chọn vị trí bản đồ.
- Chọn bán kính tìm kiếm.
- Lọc giá.
- Lọc diện tích.
- Lọc số người.
- Lọc loại phòng.
- Lọc tiện ích.
- Lọc giờ giới nghiêm.

#### Test case khu vực
1. Chọn phường.
2. Chọn xã.
3. Chọn scope ward.
4. Chọn scope district.
5. Nhập khu vực không đúng gợi ý.

#### Test case bản đồ
1. Chọn vị trí trên map.
2. Xem địa chỉ đã chọn.
3. Thay đổi vị trí.
4. Xóa vị trí map.
5. Thay đổi bán kính từ 1km đến 5km.

#### Kỳ vọng
- Nếu có vị trí bản đồ, search theo nearby sẽ được ưu tiên.
- Nếu không có vị trí bản đồ, search theo khu vực text truyền thống.
- Bộ lọc giá / diện tích / tiện ích được đẩy vào màn kết quả.

---

### 8.2 `SearchProfileActivity`
Tra cứu người dùng / chủ trọ.

#### Test case
- Nhập ít hơn 2 ký tự.
- Nhập tên hợp lệ.
- Kết quả trả về rỗng.
- Kết quả có nhiều người.
- Bấm vào một user -> mở `UserProfileActivity`.

#### Kỳ vọng
- Có debounce 400ms.
- Có loading khi đang search.
- Có empty state khi không tìm thấy.
- Bàn phím tự ẩn đúng lúc.
- Danh sách hiển thị số bài công khai của từng user sau khi đếm xong.

---

## 9. Module tìm phòng và hiển thị kết quả

### 9.1 `SearchViewModel`
Dùng cho các kiểu tìm kiếm:
- theo query
- theo filter
- theo khoảng cách bản đồ

#### Logic cần kiểm thử
- Chỉ hiển thị phòng còn chỗ.
- Loại bỏ phòng đã hết chỗ thuê.
- Sắp xếp theo độ khớp và độ mới.
- Với nearby search: tính khoảng cách bằng Haversine.
- Search theo filter chỉ giữ kết quả khớp các điều kiện location / price / area / people / room type / tiện ích theo đúng logic client-side hiện tại.

#### Test case
- Query khớp tiêu đề.
- Query khớp địa chỉ.
- Query khớp ward/district.
- Filter giá quá hẹp -> không có kết quả.
- Filter diện tích quá lớn -> không có kết quả.
- Search nearby với radius nhỏ.
- Search nearby với phòng không có tọa độ -> bỏ qua.
- Search theo query chỉ match các phòng còn available.

---

## 10. Module bài đăng của tôi

### 10.1 `MyPostsViewModel`
Màn hình danh sách các bài đăng của chủ trọ.

#### Test case
- Chưa có bài đăng.
- Có bài pending.
- Có bài approved.
- Có bài rejected.
- Có bài expired.
- Có bài rented.

#### Kỳ vọng
- Tab/filter hoạt động đúng.
- Sắp xếp hợp lý khi lọc all.
- Từ màn danh sách có thể vào chi tiết.

---

### 10.2 `MyPostDetailActivity`
Màn hình xem chi tiết một bài đăng của chính mình.

#### Chức năng
- Xem ảnh.
- Xem trạng thái duyệt.
- Xem lý do từ chối.
- Sửa bài đăng nếu bị từ chối.
- Đánh dấu đã cho thuê nếu approved hoặc expired.

#### Test case
- Status pending -> không hiện nút sửa / đã cho thuê sai trạng thái.
- Status rejected -> hiện nút sửa.
- Status approved -> hiện nút đã cho thuê.
- Status expired -> hiện nút đã cho thuê.
- Status rented -> không nên cho sửa theo logic nghiệp vụ.

#### Kỳ vọng
- Badge trạng thái đổi màu đúng.
- Dialog xác nhận khi đánh dấu đã cho thuê.
- Sau khi mark rented, bài cập nhật và màn hình đóng lại.

---

### 10.3 `EditPostViewModel`
Dùng để sửa bài đăng.

#### Test case
- Load dữ liệu cũ.
- Xóa / thêm ảnh.
- Cập nhật ward / district.
- Lưu thay đổi thành công.
- Lỗi upload ảnh.

---

## 11. Module bài lưu

### 11.1 `SavedPostsViewModel`
Danh sách bài viết đã lưu.

#### Test case
- Chưa lưu bài nào.
- Có nhiều bài đã lưu.
- Một bài đã bị xóa từ phía chủ trọ.
- Một bài đã không còn tồn tại.

#### Kỳ vọng
- Danh sách tự làm sạch nếu bài không còn hợp lệ.
- Có thể xóa bài lưu thủ công.

---

### 11.2 `SavedPostDetailActivity`
Chi tiết bài đã lưu.

#### Test case
- Hiện đúng ảnh và thông tin.
- Bỏ lưu bài.
- Bài không còn tồn tại -> hiện lỗi và thoát.

---

## 12. Module phòng chi tiết và lịch hẹn

### 12.1 `RoomDetailActivity`, `AppointmentRoomDetailActivity`
Mục tiêu là hiển thị thông tin phòng chi tiết và dữ liệu liên quan đến lịch hẹn.

#### Cần kiểm thử
- Ảnh hiển thị theo slider.
- Giá / diện tích / tiện ích / chủ nhà hiển thị đúng.
- Các layout info row không bị tràn.
- Dữ liệu thiếu vẫn hiển thị fallback text hợp lý.

---

## 13. Module chat

### 13.1 `ChatViewModel` và `ChatActivity`

#### Luồng chính
- Mở hoặc tạo chat giữa 2 user.
- Listen messages real-time.
- Gửi tin nhắn text.
- Gửi ảnh.
- Mark seen.
- Xóa tin nhắn của chính mình.
- Xóa cuộc trò chuyện chỉ phía mình.
- Thả reaction emoji.

#### Test case
- Chat với user khác lần đầu -> tạo room mới.
- Chat lại -> mở đúng room cũ.
- Gửi text rỗng -> không gửi.
- Gửi ảnh -> ảnh upload lên Storage rồi mới gửi message.
- Xóa tin nhắn của người khác -> bị chặn.
- Xóa cuộc trò chuyện -> chỉ xóa phía user hiện tại.
- Đếm tin chưa đọc -> badge / trạng thái seen cập nhật.

#### Kỳ vọng UI
- Danh sách tin nhắn cập nhật real-time.
- Tin nhắn gửi / nhận tách layout riêng.
- Có trạng thái loading khi upload ảnh.

---

### 13.2 `ConversationsActivity`
- Hiển thị danh sách cuộc trò chuyện.
- Badge tin nhắn chưa đọc.
- Bấm vào từng item để mở chat.

---

## 14. Module thông báo, hỗ trợ, và các màn phụ

### 14.1 Notifications
- Badge số thông báo phải đúng.
- Mở danh sách thông báo.
- Đọc xong có cập nhật trạng thái.

### 14.2 Support tickets
- Tạo ticket hỗ trợ.
- Xem danh sách ticket.
- Xem chi tiết ticket.
- Kiểm thử trạng thái: open, processing, resolved, closed.

### 14.3 Cài đặt
- Đọc các option trong settings.
- Kiểm thử điều hướng tới màn liên quan.

---

## 15. Các layout cần kiểm tra khi test UI

Những layout đã thấy trong project có nhiều màn hình. Khi kiểm thử UI nên đi theo nhóm:

### Nhóm auth / account
- `activity_login.xml`
- `activity_register.xml`
- `activity_forgot_password.xml`
- `activity_reset_password.xml`
- `activity_change_password.xml`
- `activity_personal_info.xml`
- `activity_search_profile.xml`
- `activity_user_profile.xml`

### Nhóm home / search / profile / post
- `activity_main.xml`
- `fragment_home.xml`
- `fragment_search.xml`
- `fragment_post.xml`
- `fragment_profile.xml`
- `layout_guest_post.xml`
- `layout_verify_required.xml`
- `layout_rejection_banner.xml`

### Nhóm detail / list
- `activity_room_detail.xml`
- `activity_my_posts.xml`
- `activity_my_post_detail.xml`
- `activity_saved_posts.xml`
- `activity_saved_post_detail.xml`
- `activity_notifications.xml`
- `activity_my_appointments.xml`
- `activity_conversations.xml`
- `activity_chat.xml`
- `activity_appointment_room_detail.xml`

### Nhóm dialog / popup
- `dialog_loading_state.xml`
- `dialog_message_state.xml`
- `dialog_rules.xml`
- `dialog_review.xml`
- `dialog_search_address.xml`
- `dialog_payment_qr.xml`
- `dialog_post_quota_limit.xml`
- `dialog_upgrade_slots.xml`
- `dialog_featured_upgrade.xml`
- `dialog_create_support_ticket.xml`
- `popup_emoji_picker.xml`

### Nhóm item / row
- `item_room_new.xml`
- `item_room_featured.xml`
- `item_conversation.xml`
- `item_message_sent.xml`
- `item_message_received.xml`
- `item_notification.xml`
- `item_support_ticket.xml`
- `item_user_search.xml`
- `item_booked_slot_row.xml`
- `item_host_info.xml`

---

## 16. Checklist kiểm thử nhanh theo luồng thực tế

### 16.1 User mới
1. Đăng ký.
2. Đăng nhập.
3. Cập nhật profile.
4. Tìm phòng.
5. Lưu bài.
6. Nhắn tin.
7. Gửi yêu cầu xác minh chủ trọ.

### 16.2 User muốn đăng bài
1. Xác minh thành công.
2. Vào tab đăng bài.
3. Điền form.
4. Chọn ảnh.
5. Chọn vị trí map.
6. Đăng bài.
7. Xem bài trong `My Posts`.

### 16.3 User hết quota
1. Đăng bài đủ 3 lần miễn phí.
2. Vào lại Post.
3. Xem dialog quota.
4. Thử mua thêm lượt.
5. Thanh toán thành công.
6. Đăng lại bài.

### 16.4 User bị từ chối xác minh
1. Mở lại Post.
2. Xem reason bị từ chối.
3. Chỉnh sửa hồ sơ / giấy tờ.
4. Gửi lại xác minh.

---

## 17. Điểm dễ lỗi cần ưu tiên test

- Logic role giữa `user`, `pending`, `rejected`, `admin`, `verified`.
- `isVerified` và `verificationStatus` có thể lệch nhau nếu dữ liệu không đồng bộ.
- Trạng thái quota 24 giờ và slot mua thêm.
- Tìm kiếm theo nearby vì phụ thuộc tọa độ.
- Đồng bộ avatar và badge trong `ProfileFragment` khi quay lại màn hình.
- Chat real-time với listener cần tránh trùng listener khi xoay màn hình / quay lại app.
- Bài đăng đã hết hạn nhưng vẫn xuất hiện sai ở một số list cũ.
- Các empty state trên UI có thể bị che bởi view khác nếu không reset visibility đúng.
- `MyApp` quản lý online/offline bằng lifecycle app, nên cần test vào nền / ra nền để tránh presence sai.
- `MainActivity` hiện được khai báo với `android:name=".MainActivity"` trong manifest, còn màn khởi chạy thực tế là `SplashActivity`.

---

## 18. Kết luận

App hiện tại có cấu trúc nghiệp vụ khá rõ ràng và chia thành nhiều luồng:
- Auth và account
- eKYC xác minh chủ trọ
- Đăng bài và quota
- Search theo khu vực / bản đồ / filter
- Home feed
- Chat real-time
- Profile / saved posts / notifications / support

Khi test thủ công, nên đi theo đúng từng vai trò:
- Guest
- User thường
- User pending xác minh
- User bị từ chối xác minh
- User đã verified
- Admin

Mỗi vai trò sẽ thấy UI và quyền khác nhau, nên cần test riêng từng nhánh để tránh bỏ sót lỗi logic.

---

*Nếu cần, ở lượt tiếp theo mình có thể viết tiếp cho bạn một bản “Test Case dạng bảng” theo từng màn hình, hoặc rút tài liệu này thành checklist ngắn gọn để bạn dùng khi test thực tế.*