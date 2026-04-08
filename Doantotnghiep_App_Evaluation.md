# ĐÁNH GIÁ CHUYÊN SÂU SOURCE CODE APP ANDROID (DOANTOTNGHIEP) - VERSION CẬP NHẬT MỚI NHẤT

Sau khi quét sâu lần 2, đào thẳng vào `build.gradle.kts`, `AndroidManifest.xml` và các class Model gốc, mình đã lôi ra thêm những lỗi nền tảng cực kỳ "hiểm hóc" mà bạn có thể đã quên mất trong quá trình code. Đây là danh sách đầy đủ, chính xác tuyệt đối không trượt một dấu chấm.

---

## 1. VI PHẠM MVVM & CLEAN CODE TỚI MỨC NGHIÊM TRỌNG

### 1.1 "Phí phạm tài nguyên": Đã bật ViewBinding nhưng lại code bằng findViewById
*   **Vị trí:** `app/build.gradle.kts` (Dòng 37) và hàng loạt giao diện (`PostFragment`, `RoomAdapter`, `MainActivity`).
*   **Chi tiết:** Bạn đã config chính xác khối lượng xây dựng là `viewBinding = true`. Code đã hỗ trợ tận răng cơ chế truy xuất an toàn. Nhưng suốt toàn bộ dự án, bạn lại ôm đồm gần 100 câu lệnh `findViewById`.
*   **Điểm trừ:** Giám khảo sẽ hỏi bạn: *"Tại sao bật ViewBinding ở gradle mà file Fragment lại code bằng findViewById?"*. Chậm hiệu năng, dài file, mã "rác" nhiều và rủi ro NullPointer rơi vãi khắp nơi.
*   **Cách fix:** Thay thế toàn bộ bằng `binding.<tên_id_view>`.

### 1.2 Rò rỉ UI State - Mất sạch dữ liệu khi xoay màn hình (Configuration Change Bug)
*   **Vị trí:** `PostFragment.kt` (Các biến `imageUris`, `lastPostedTitle`...) và `RoomDetailActivity.kt` (`currentRoomData`).
*   **Chi tiết lỗi:** Theo chuẩn MVVM, mọi trạng thái của màn hình (UI State) phải được lưu giữ trong `ViewModel` (Vì ViewModel sống dai hơn View). NHƯNG ở `PostFragment`, bạn đang lưu danh sách 10 bức ảnh (`val imageUris = mutableListOf<Uri>()`) ngay trong Fragment.
*   **Hậu quả thực tế:** Nếu người dùng đang chọn 5 bức ảnh, tự nhiên họ vô tình **xoay ngang điện thoại** -> Android lập tức Tiêu diệt (Destroy) và Tạo lại (Recreate) `PostFragment`. Toàn bộ 5 bức ảnh vừa chọn sẽ **Biến mất tăm** vì biến `imageUris` bị reset lại từ đầu.
*   **Cách fix:** Chuyển `imageUris` và các biến lưu nháp vào trong `PostViewModel`.

### 1.3 Business Logic bị rò rỉ ra lớp Giao Diện (View)
*   **Vị trí:** `MainActivity.kt` (Dòng 45-50).
*   **Chi tiết lỗi:** Logic tính xem ngày hôm nay đã hết hạn 24 giờ hay chưa đang bị nhét vào Activity và ghi vào `SharedPreferences`. Nhớ kỹ: `MainActivity` không có quyền làm Toán học hay đọc ghi Database. Nó phải truyền lệnh qua `MainViewModel` xử lý.

### 1.4 Thư viện Firebase giẫm chân lên ViewModel
*   **Vị trí:** Có mặt ở tất cả ViewModel (VD: `HomeViewModel.kt` dòng 44).
*   **Chi tiết lỗi:** Trong Clean Architecture / MVVM, gọi thẳng SDK `FirebaseAuth.getInstance()` vào ViewModel là điều cấm kỵ vì nó tạo ra sự trói buộc cứng rắc (tight-coupling) không thể Unit Test được.
*   **Cách fix:** Bọc `FirebaseAuth` về `AuthRepository.kt` và gọi biến tham chiếu sang.

### 1.5 Bán rẻ Dữ liệu thô (Raw Data) ra ngoài UI
*   **Vị trí:** `AppointmentRoomDetailViewModel.kt`.
*   **Chi tiết lỗi:** Nhận Firebase map thô `Map<String, Any>?` rồi vứt ra cho UI xử lý. Ép ViewModel trả Data Class `Room`.

---

## 2. BUGS LỖ HỔNG THỜI GIAN & KHÔNG ĐỒNG BỘ DỮ LIỆU

### 2.1 Lỗ hổng thời gian giả mạo (Time Vulnerability) ở Data Models
*   **Vị trí:** `Room.kt` và `User.kt`.
*   **Chi tiết:** Bạn chốt cứng `val createdAt: Long = System.currentTimeMillis()`. Việc tin tưởng chiếc Đồng Hồ trên máy khách hàng (Client) là sai làm cơ bản. Khách dời lùi giờ điện thoại, thời gian phân trang và TTL (90 ngày) tự huỷ sẽ bị phá nát.
*   **Cách fix:** Dùng thời gian của Máy Chủ bằng `FieldValue.serverTimestamp()` khi push lên.

### 2.2 Thiếu Đồng bộ Ghi (Atomic Batching) đa bảng
*   **Vị trí:** Toàn bộ lệnh Tạo mới, Hủy lịch trong `AppointmentRepository.kt`.
*   **Chi tiết lỗi:** Code đang ghi lần lượt `appointments` -> `bookedSlots` -> `notifications`. Rớt mạng ở giữa, dữ liệu sẽ vỡ phân nửa.
*   **Cách fix:** Đưa tất cả vào `WriteBatch` (`db.batch()`).

### 2.3 Lỗ hổng xóa "Bóng ma" Lịch hẹn
*   **Vị trí:** Hàm `deleteAppointment(...)`.
*   **Chi tiết lỗi:** Chỉ xóa được dữ liệu ở `appointments`, bỏ quên cọc mốc giờ ở Database `bookedSlots`. Bất kỳ Lịch nào xóa ngang đều tạo ra 1 slot chết kẹt vĩnh viễn không ai đặt vào được.

---

## 3. LỖI LOGIC TÍNH NĂNG (MISSING LOGIC)

### 3.1 "Thông báo Tàng hình" (Push Notifications Configuration Error)
*   **Vị trí:** `AndroidManifest.xml` (Từ dòng 104 -> 113).
*   **Chi tiết lỗi:** File code `MyFirebaseMessagingService` có mặt nhưng bị **Comment ẩn (<!-- -->)** ở Manifest. Hệ thống nhận tin nhắn Rung (FCM) bị đứt lìa. Khởi tạo `notifications` trên Firestore trở nên vô dụng nếu Chủ trọ tắt App. Chủ trọ sẽ rơi vào trạng thái "mù" thông báo nếu không chủ động mở app ra xem.