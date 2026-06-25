# Bảng Đặc Tả Use Case: Đăng Ký

| Trường | Nội dung |
|--------|----------|
| **Tên Use Case** | Đăng ký |
| **Tác nhân chính** | Khách truy cập |
| **Mục đích** | Cho phép người dùng mới tạo tài khoản trong hệ thống để sử dụng các tính năng nâng cao. |
| **Mức độ ưu tiên** | Cao (Bắt buộc) |
| **Điều kiện kích hoạt** | Khách truy cập bấm vào nút "Đăng ký" hoặc "Chưa có tài khoản? Đăng ký ngay" tại màn hình Đăng nhập. |
| **Điều kiện tiên quyết** | Khách truy cập chưa đăng nhập vào hệ thống. |
| **Điều kiện thành công** | Tài khoản được tạo thành công, dữ liệu người dùng được lưu vào hệ thống. Hệ thống tự động gửi email xác thực đến địa chỉ email đăng ký. Người dùng được điều hướng về màn hình Đăng nhập. |
| **Điều kiện thất bại** | Không có tài khoản nào được tạo. Hệ thống hiển thị thông báo lỗi cụ thể, dữ liệu đã nhập được giữ nguyên. |

---

## Luồng sự kiện chính

| Bước | Tác nhân | Hành động / Phản hồi |
|------|----------|----------------------|
| 1 | Khách truy cập | Bấm nút "Đăng ký" tại màn hình Đăng nhập. |
| 2 | Hệ thống | Hiển thị màn hình Form Đăng ký. |
| 3 | Khách truy cập | Nhập các thông tin: Họ và tên, Địa chỉ email, Số điện thoại, Mật khẩu, Xác nhận mật khẩu. |
| 4 | Khách truy cập | Bấm nút "Đăng ký". |
| 5 | Hệ thống | Kiểm tra tính hợp lệ của toàn bộ thông tin vừa nhập. |
| 6 | Hệ thống | Tạo tài khoản và lưu thông tin người dùng vào hệ thống, gửi email xác thực. |
| 7 | Hệ thống | Hiển thị Dialog thông báo đăng ký thành công và yêu cầu kiểm tra email xác thực. |
| 8 | Khách truy cập | Bấm nút "Xác nhận" trên Dialog. |
| 9 | Hệ thống | Đưa người dùng về màn hình Đăng nhập. |

---

## Luồng thay thế

**(Rẽ nhánh từ bước 3 — Người dùng muốn hủy bỏ khi đang điền Form)**

| Bước | Tác nhân | Hành động / Phản hồi |
|------|----------|----------------------|
| 3.1 | Khách truy cập | Bấm nút "Quay lại" hoặc nhấn dòng "Đã có tài khoản". |
| 3.2 | Hệ thống | Phát hiện có dữ liệu đang nhập dở, hiển thị Dialog xác nhận: *"Bỏ qua đăng ký? Thông tin bạn đã nhập sẽ bị mất."* |
| 3.3 | Khách truy cập | Chọn nút "Thoát". |
| 3.4 | Hệ thống | Hủy bỏ quá trình, đưa người dùng về màn hình Đăng nhập. *(Use case kết thúc)* |

---

## Luồng ngoại lệ

**(Rẽ nhánh từ bước 5 — Thông tin đầu vào không hợp lệ)**

| Bước | Tác nhân | Hành động / Phản hồi |
|------|----------|----------------------|
| 5.1 | Hệ thống | Họ và tên để trống hoặc không hợp lệ, hiển thị thông báo lỗi và yêu cầu nhập lại. *(Quay lại bước 4)* |
| 5.2 | Hệ thống | Địa chỉ email không đúng định dạng hoặc không phải @gmail.com, hiển thị thông báo lỗi và yêu cầu nhập lại. *(Quay lại bước 4)* |
| 5.3 | Hệ thống | Số điện thoại không đúng định dạng, hiển thị thông báo lỗi và yêu cầu nhập lại. *(Quay lại bước 4)* |
| 5.4 | Hệ thống | Mật khẩu không đạt yêu cầu, hiển thị thông báo lỗi và yêu cầu nhập lại. *(Quay lại bước 4)* |
| 5.5 | Hệ thống | Xác nhận mật khẩu không khớp, hiển thị thông báo lỗi và yêu cầu nhập lại. *(Quay lại bước 4)* |

**(Rẽ nhánh từ bước 6 — Dữ liệu đã tồn tại hoặc lỗi hệ thống)**

| Bước | Tác nhân | Hành động / Phản hồi |
|------|----------|----------------------|
| 6.1 | Hệ thống | Email đã được sử dụng bởi tài khoản khác, hiển thị thông báo lỗi và yêu cầu nhập lại. *(Quay lại bước 4)* |
| 6.2 | Hệ thống | Số điện thoại đã được đăng ký bởi tài khoản khác, hiển thị thông báo lỗi và yêu cầu nhập lại. *(Quay lại bước 4)* |
| 6.3 | Hệ thống | Lỗi kết nối mạng hoặc hệ thống, hiển thị thông báo lỗi và yêu cầu thử lại. *(Quay lại bước 4)* |
