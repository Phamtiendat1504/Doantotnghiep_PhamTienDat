# HƯỚNG DẪN TÍCH HỢP BẢN ĐỒ GOOGLE MAPS VÀO CHI TIẾT PHÒNG TRỌ

Tài liệu này được viết theo cách dễ hiểu nhất để bất kỳ ai cũng có thể đọc, nắm rõ luồng xử lý và tự tin thuyết trình/trả lời câu hỏi khi bảo vệ đồ án.

---

## 1. Ý TƯỞNG VÀ CÁCH HOẠT ĐỘNG
Thay vì dùng thư viện `Google Maps SDK` phức tạp và phải cài đặt API Key (rất dễ bị lỗi tốn phí), chúng ta chọn giải pháp **thông minh & an toàn hơn**: 
- Dùng **WebView** (một trình duyệt web thu nhỏ được nhúng sẵn trong Android) để hiển thị bản đồ trực tiếp trên màn hình.
- Bản đồ này được load thông qua một **URL nhúng (embed URL)** của Google Maps hoàn toàn miễn phí.
- Khi người dùng muốn xem to hơn, họ bấm nút "Mở Google Maps", hệ thống sẽ bật hẳn ứng dụng Google Maps có trong điện thoại lên, giúp xem đường đi dễ dàng.

**Để thực hiện, chúng ta chỉ thay đổi nội dung ở đúng 2 file:**
1. File giao diện (UI): `activity_room_detail.xml`
2. File xử lý logic (Code): `RoomDetailActivity.kt`

---

## 2. CHI TIẾT CÁCH LÀM

### BƯỚC 1: Tạo Giao Diện (Thiết kế XML)
**📍 File chỉnh sửa:** `app/src/main/res/layout/activity_room_detail.xml`

**Làm những gì?**
Giữa phần "Thông tin chủ trọ" và "Lịch hẹn đã đặt", chúng ta thêm một thẻ giao diện tên là `CardView` (Một khối chữ nhật có bo góc) để chứa bản đồ. Trong thẻ `CardView` này có 3 phần chính:
1. **TextView:** Text chữ hiển thị địa chỉ chi tiết (Ví dụ: Số 12, Mộ Lao, Hà Đông, Hà Nội).
2. **WebView (`@+id/webViewMap`):** Một ô vuông cao `200dp`, đây chính là nơi bản đồ sẽ chạy lên.
3. **Button (`@+id/btnOpenMaps`):** Nút nhấn "Mở Google Maps" phía dưới cùng của thẻ.

---

### BƯỚC 2: Viết Code Xử Lý (Logic Kotlin)
**📍 File chỉnh sửa:** `app/src/main/java.../RoomDetailActivity.kt`

**Làm những gì?**
Chúng ta tạo ra màn hình chờ bản đồ và nạp bản đồ vào đó. Thực hiện qua 2 hàm chính do mình tự viết thêm:

#### A. Hàm hiển thị bản đồ lên WebView: `setupMapSection()`
1. **Ghép chuỗi địa chỉ:** Lấy địa chỉ cụ thể, phường, quận rồi cộng thêm chữ "Hà Nội" ở đuôi thành một câu hoàn chỉnh `fullAddress` để Google tìm cho chuẩn. Hiển thị chữ lên giao diện.
2. **Setup trình duyệt nhúng (`WebView`):** Bật bật tính năng `javaScriptEnabled = true` để webview chạy được bản đồ của Google vì web của Google dùng JavaScript. Khóa tính năng cuộn (`isClickable = false`) để khi lướt xem phòng ko bị nhầm thành lướt bản đồ.
3. **Mã code HTML thần thánh (Vượt lỗi iframe):** 
   - Google có một quy định bảo mật: "Không cho phép kéo trực tiếp link bản đồ (URL) bỏ vào ứng dụng, bắt buộc URL đó phải được đặt trong một thẻ `<iframe>` của web".
   - **Cách giải quyết:** Chúng ta "đánh lừa" quy định này bằng cách tự tạo ra một chuỗi văn bản (`iframeHtml`) có chứa sẵn cấu trúc chuẩn của trang Web (gồm `<html>`, `<body>`, và thẻ `<iframe src="link_ban_do">`).
   - Cuối cùng, nhồi khúc mã HTML giả lập này vào WebView bằng lệnh: `webViewMap.loadDataWithBaseURL(...)`. Nhờ đó, bản đồ hiện lên gọn gàng, đẹp mắt và ko bị lỗi trắng màn hình.

#### B. Hàm mở ứng dụng Google Maps: `openGoogleMaps()`
Được gắn vào nút bấm "Mở Google Maps" qua sự kiện `setOnClickListener`.
- **Cách hoạt động:** Nó dùng một khái niệm gọi là `Intent` (mệnh lệnh gửi cho hệ điều hành Android).
- Chúng ta yêu cầu Android: *"Hãy tìm và mở app Google Maps lên, truyền tọa độ tương ứng với cái địa chỉ đang xem vào đó"*. 
- App sẽ tự mở sang Google maps kèm địa chỉ.

---

## 3. TẠI SAO LÀM THẾ NÀY LÀ ĐÚNG CHUẨN MVVM?
Nếu có ai hỏi: *"Em xử lý việc này trong `Activity` có làm vỡ cấu trúc MVVM không?"*

**Câu trả lời để dõng dạc bảo vệ đồ án:**
Dạ thưa **KHÔNG**, thậm chí rất chuẩn chỉ ạ. 
Bởi vì nguyên lý của MVVM là tách biệt phần giao diện lấy từ ViewModel. 
- Nhiệm vụ thay đổi cài đặt WebView (`webView.settings`), việc load code HTML lên màn hình (`webView.loadData...`) cũng như gắn sự kiện kích hoạt nút bấm `Intent` mở app khác đều là các tác vụ **THUẦN TÚY THUỘC VỀ UI (GIAO DIỆN)** và phải cần đến môi trường Context của View. 
- ViewModel không quan tâm app đang dùng WebView hiển thị bản đồ hay dùng Text, ViewModel chỉ có việc cung cấp dữ liệu phòng thô thôi. Cho nên em code logic này ở tầng `View` (tức là file Android Activity) là hoàn toàn tuân thủ kiến trúc thiết kế MVVM ạ!

---

## 4. ĐÁNH GIÁ KIẾN TRÚC: TẠI SAO DÙNG WEBVIEW MÀ KHÔNG DÙNG GOOGLE MAPS NATIVE SDK?

Nếu hội đồng phản biện hỏi: *"Tại sao em không dùng thư viện Google Maps SDK chuẩn của Android mà lại nhúng web?"*

**1. Ưu điểm tuyệt đối (Điểm sáng của cách làm này):**
- **Miễn phí 100% & Không sợ sập API:** Dùng SDK Native bắt buộc phải nhập thẻ Visa để lấy API Key, nếu vượt giới hạn (Quota) bản đồ sẽ bị sập (lỗi xám màn hình) ngay lập tức. Tính năng nhúng WebView hoàn toàn miễn nhiễm với lỗi này.
- **Tối ưu dung lượng:** Không phải cài thêm bộ thư viện Google Play Services nặng nề, giúp tốc độ Build và tải App mượt mà, tốn ít bộ nhớ.
- **Đáp ứng cực gọn nhu cầu:** Ứng dụng "Tìm phòng trọ" cơ bản chỉ cần hiển thị vị trí khu vực. Nhúng WebView làm tròn chức năng này mà không hề dư thừa.

**2. Thủ thuật trả lời phản biện xuất sắc:**
*"Dạ thưa thầy/cô, vì tính năng chính của app em là Quản lý hệ thống và Tìm phòng trực quan. Em chủ đích dùng WebView và Intent để tối ưu hiệu năng app, làm app nhẹ nhàng, đồng thời giảm thiểu hoàn toàn rủi ro sập API Key khi demo. Nếu người dùng muốn trải nghiệm dẫn đường sâu hơn, em đã đính kèm nút điều hướng để bật luôn ứng dụng Google Maps gốc có sẵn trong điện thoại họ lên, đó mới mang lại trải nghiệm bản đồ mượt và tốt nhất ạ!"*

---
*(Lưu ý: Bạn có thể in/đọc kĩ file này trước khi bảo vệ để có mạch trình bày lưu loát nhất nhé!)*
