# TÀI LIỆU HƯỚNG DẪN BẢO VỆ TÍNH NĂNG FCM REALTIME (THÔNG BÁO ĐẨY)

Tài liệu này được biên soạn ngắn gọn, dễ hiểu nhất để bạn (Sinh viên) nắm vững bản chất kiến trúc và trả lời trơn tru mọi câu hỏi phản biện của Hội đồng về tính năng Thông báo (FCM).

---

## 1. FCM là gì? Bản chất Thông báo "Ting Ting"
**FCM** (Firebase Cloud Messaging) là "Trạm Bưu Điện" của Google phát minh ra để gửi tin nhắn thẳng vào khe màn hình điện thoại (Push Notification), ngay cả khi người dùng đang tắt màn hình cất đút túi quần.

> [!NOTE] 
> Bạn KHÔNG THỂ lấy máy Android của người này bắn thông báo thẳng sang máy Android của người kia. Đừng cố giải thích như vậy, thầy cô sẽ cười.
> Bạn PHẢI giải thích rằng: **"Mọi luồng sóng âm đều phải đi qua Máy chủ Trung gian (Server)"**.

---

## 2. Hoạt động như thế nào? (Quy trình 3 Bước Lõi)

Tính năng thông báo của hệ thống mình hoạt động theo luồng **Nhà máy Dây chuyền Tự động**:

### BƯỚC 1: Thu thập "Biển số nhà" (FCM Token)
- Khi user **Đăng Nhập** vào App thành công, Hệ thống (trong file `MainActivity.kt`) sẽ tự động chộp lấy một dãy mã bí mật từ Google gọi là **FCM Token** (Nó chính là địa chỉ biển số nhà để gửi hàng).
- App sẽ lưu cái thẻ Token này lên bảng `users` trên Firestore. Ai dùng máy nào thì lưu biển số máy đó. (Nếu Đăng xuất rồi Đăng nhập nick khác, Token tự động được cắm cho nick mới).

### BƯỚC 2: Thùng thư Trung tâm (Notifications Collection)
- Khi Khách thuê **Đặt phòng** (Thao tác trên Android), hoặc Admin **Duyệt bài** (Thao tác trên Web), họ chỉ thao tác với 1 thứ duy nhất: Tạo ra một dòng Thư rơi vào cái thùng `notifications` trên Database.
- Web và App Android của mình MÙ tịt về việc làm sao để điện thoại đổ chuông. Chức trách của tụi nó chỉ là Ghi Thư thả vào Thùng!

### BƯỚC 3: Kẻ Hủy Diệt - Cloud Functions (Quan trọng nhất)
- Mình xây một con Server "Không Máy Chủ" (`index.js` bằng Node.js) nhúng vào nền tảng Google Cloud Functions.
- Thằng Server này giống hệt con Robot trực tổng đài: Оно cứ thức 24/7. 
- Ngay khi có Thư bay vào bảng `notifications`, Robot lập tức Lôi lá thư lên -> Nhìn vào ID Người Nhận -> Lục trong kho `users` ra cái "FCM Token" của ông Nhận -> Ra lệnh cho trạm Radar của FCM bắn đại bác bùm một phát thẳng tới điện thoại chứa cái Token đó.
- Thế là: "TING TING!" Màn hình sáng lên đổ thông báo!

---

## 3. Câu Hỏi Phản Biện Chắc Chắn Bị Hỏi (Và Cách Cãi)

### ❓ CÂU HỎI 1: Tại sao em không gọi API bắn thông báo thẳng từ cái hàm trên nút "Duyệt Bài"?
> **💡 Tương kế tựu kế phản biện:**
> "Dạ thưa Hội đồng, nếu em code chức năng bắn thông báo vào ngay nút Duyệt Bài trên Android thì là điều tối kỵ vì nó làm **Lộ Chìa Khóa Server Key** của Firbase vào tay người dùng. Google cực kì cấm kỵ điều này.
> Do đó em chọn giải pháp Kiến trúc Back-End Cloud Functions (Event Trigger). Web hoặc App chỉ lo ném dữ liệu thôi, còn toàn bộ áp lực bảo mật và gánh nặng gửi hàng nghìn Push Notification em đẩy hoàn toàn cho Cloud Server của Google tự tính toán và xử lý. Cực kỳ bảo mật và tốc độ cao ạ!"

### ❓ CÂU HỎI 2: Nếu người ta mượn điện thoại, 2 nick đăng nhập thay phiên nhau thì App em có gửi nhầm thông báo không?
> **💡 Trả lời tự tin 100%:**
> "Dạ không bao giờ nhầm ạ! Trong vòng đời hệ thống em thiết kế, hàm `fetchAndSaveFcmToken()` được chốt kín vào sự kiện `onNewIntent` (Lúc Đăng Nhập xong rẽ vào Home).
> Nghĩa là CỨ ĐĂNG XUẤT RA, vứt nick khác dăng nhập vào, là hệ thống đè cái Token vào túi của thằng chủ mới ngay lập tức. Nick vừa bị đá ra khỏi máy sẽ mất liên kết Token nên 100% không bao giờ xảy ra vụ đọc trộm hay nhầm chuông ạ!"

---

## 4. Tổng Kết Giá Trị Đồ Án
Tích hợp Cloud Functions (Event-driven Architecture) với Front-end Android không phải dự án sinh viên nào cũng liều lĩnh làm. Bạn làm thành công được luồng này:
1. Bạn đã lấn sân sang làm **DevOps / Backend Serverless** cực xịn.
2. Bạn chứng minh được Web và Mobile đan chéo thao tác cho nhau rất mượt (Web bấm, Mobile rung).
3. Kiến trúc đạt quy chuẩn sạch (Clean Architecture) vì tách rời chức năng Database và chức năng đẩy Tín hiệu độc lập hoàn toàn.

**BẠN ĐÃ RẤT GIỎI RỒI! HÃY TỰ TIN CHÉM GIÓ TRƯỚC HỘI ĐỒNG!**
