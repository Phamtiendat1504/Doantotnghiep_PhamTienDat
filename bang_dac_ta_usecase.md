# BẢNG ĐẶC TẢ USE CASE 

## 1. Use case: Đăng ký

| Trường | Nội dung |
| :--- | :--- |
| **Tên Use case** | Đăng ký |
| **Tác nhân chính** | Khách truy cập |
| **Mục đích** | Cho phép Khách truy cập tạo tài khoản trong hệ thống để sử dụng đầy đủ tính năng của ứng dụng. |
| **Mức độ ưu tiên** | Cao (Bắt buộc) |
| **Điều kiện kích hoạt** | Khách truy cập bấm vào nút "Đăng ký" tại màn hình Đăng nhập. |
| **Điều kiện tiên quyết** | Khách truy cập chưa đăng nhập vào hệ thống. |
| **Điều kiện thành công** | Tài khoản được tạo thành công, thông tin lưu vào cơ sở dữ liệu. Hệ thống gửi email xác thực. Người dùng được điều hướng về màn hình Đăng nhập. |
| **Điều kiện thất bại** | Không có tài khoản nào được tạo. Hệ thống hiển thị thông báo lỗi, khách truy cập vẫn ở màn hình Đăng ký. |
| **Luồng sự kiện chính** | **1.** Khách truy cập bấm "Đăng ký" tại màn hình Đăng nhập.<br>**2.** Hệ thống hiển thị Form Đăng ký.<br>**3.** Khách truy cập điền: Họ và tên, Email, Số điện thoại, Mật khẩu, Xác nhận mật khẩu.<br>**4.** Khách truy cập bấm nút "Đăng ký".<br>**5.** Hệ thống kiểm tra hợp lệ toàn bộ thông tin.<br>**6.** Hệ thống xác nhận số điện thoại và email chưa được đăng ký.<br>**7.** Hệ thống tạo tài khoản, lưu thông tin và gửi email xác thực.<br>**8.** Hệ thống hiển thị Dialog thông báo đăng ký thành công.<br>**9.** Khách truy cập bấm "Đóng" trên Dialog.<br>**10.** Hệ thống điều hướng về màn hình Đăng nhập. |
| **Luồng thay thế** | **(Rẽ nhánh từ bước 3 - Đăng ký bằng phím ảo)**<br>**3.1.** Khách truy cập nhập xong thông tin ở ô "Xác nhận mật khẩu".<br>**3.2.** Khách truy cập nhấn phím "Hoàn tất" (Done) trên bàn phím ảo.<br>**3.3.** Hệ thống tự động kích hoạt lệnh Đăng ký.<br>*(Chuyển tiếp đến bước 5 của Luồng chính)*. |
| **Luồng ngoại lệ** | **(Rẽ nhánh từ bước 3 - Hủy bỏ đăng ký)**<br>**3.4.** Khách truy cập bấm "Quay lại" hoặc "Đã có tài khoản" khi đã nhập thông tin. Hệ thống hiển thị Dialog xác nhận. Khách truy cập bấm "Thoát", hệ thống chuyển về màn hình Đăng nhập. *(Use case kết thúc)*.<br><br>**(Rẽ nhánh từ bước 4 - Vượt giới hạn đăng ký)**<br>**4.1.** Khách truy cập kích hoạt lệnh đăng ký quá nhiều lần liên tiếp. Hệ thống chặn và yêu cầu chờ. *(Quay lại bước 3)*.<br><br>**(Rẽ nhánh từ bước 5 - Thông tin không hợp lệ)**<br>**5.1.** Họ tên trống hoặc không hợp lệ. Hệ thống báo lỗi tại ô Họ và tên. *(Quay lại bước 3)*.<br>**5.2.** Email sai định dạng hoặc không phải @gmail.com. Hệ thống báo lỗi tại ô Email. *(Quay lại bước 3)*.<br>**5.3.** Số điện thoại sai định dạng. Hệ thống báo lỗi tại ô Số điện thoại. *(Quay lại bước 3)*.<br>**5.4.** Mật khẩu chưa đủ mạnh. Hệ thống báo lỗi tại ô Mật khẩu. *(Quay lại bước 3)*.<br>**5.5.** Xác nhận mật khẩu không khớp. Hệ thống báo lỗi tại ô Xác nhận mật khẩu. *(Quay lại bước 3)*.<br><br>**(Rẽ nhánh từ bước 6 - Dữ liệu trùng lặp)**<br>**6.1.** Số điện thoại đã được đăng ký. Hệ thống thông báo lỗi. *(Quay lại bước 3)*.<br>**6.2.** Email đã tồn tại trong hệ thống. Hệ thống thông báo lỗi. *(Quay lại bước 3)*. |

## 2. Use case: Đăng nhập

| Trường | Nội dung |
| :--- | :--- |
| **Tên Use case** | Đăng nhập |
| **Tác nhân chính** | Khách truy cập |
| **Mục đích** | Xác thực danh tính Khách truy cập để cho phép truy cập vào các chức năng của Người thuê hoặc Người cho thuê. |
| **Mức độ ưu tiên** | Cao (Bắt buộc) |
| **Điều kiện kích hoạt** | Khách truy cập điền thông tin và kích hoạt lệnh "Đăng nhập". |
| **Điều kiện tiên quyết** | Khách truy cập chưa đăng nhập và đã có tài khoản trong hệ thống. |
| **Điều kiện thành công** | Xác thực thành công. Hệ thống đăng ký nhận thông báo (FCM) và chuyển người dùng vào màn hình chính. |
| **Điều kiện thất bại** | Xác thực thất bại. Hệ thống hiển thị lỗi cụ thể, khách truy cập vẫn ở lại màn hình Đăng nhập. |
| **Luồng sự kiện chính** | **1.** Khách truy cập nhập Email và Mật khẩu.<br>**2.** Khách truy cập bấm nút "Đăng nhập".<br>**3.** Hệ thống kiểm tra Email và Mật khẩu không trống.<br>**4.** Hệ thống kiểm tra giới hạn đăng nhập.<br>**5.** Hệ thống gửi yêu cầu xác thực lên Firebase.<br>**6.** Firebase xác thực thông tin hợp lệ.<br>**7.** Hệ thống kiểm tra email tài khoản đã được xác minh.<br>**8.** Hệ thống kiểm tra tài khoản không bị khóa.<br>**9.** Hệ thống đăng ký nhận thông báo cho thiết bị.<br>**10.** Hệ thống điều hướng người dùng vào màn hình chính. *(Use case kết thúc)*. |
| **Luồng thay thế** | **(Rẽ nhánh từ bước 1 - Đăng nhập bằng phím ảo)**<br>**1.1.** Khách truy cập nhập xong thông tin ở ô "Mật khẩu".<br>**1.2.** Khách truy cập nhấn phím "Hoàn tất" (Done) trên bàn phím ảo của điện thoại thay vì bấm nút Đăng nhập.<br>**1.3.** Hệ thống tự động bắt sự kiện và kích hoạt lệnh Đăng nhập.<br>*(Chuyển tiếp đến bước 3 của Luồng chính)*. |
| **Luồng ngoại lệ** | **(Rẽ nhánh từ bước 1 - Điều hướng hủy bỏ)**<br>**1.4.** Khách truy cập bấm "Quên mật khẩu". Hệ thống chuyển sang màn hình Khôi phục mật khẩu. *(Use case kết thúc - Hủy bỏ)*.<br>**1.5.** Khách truy cập bấm "Đăng ký". Hệ thống chuyển sang màn hình Đăng ký. *(Use case kết thúc - Hủy bỏ)*.<br><br>**(Rẽ nhánh từ bước 3 - Bỏ trống thông tin)**<br>**3.1.** Email hoặc Mật khẩu trống. Hệ thống báo lỗi ngay dưới ô nhập liệu. *(Quay lại bước 1)*.<br><br>**(Rẽ nhánh từ bước 4 - Spam đăng nhập)**<br>**4.1.** Khách truy cập kích hoạt lệnh đăng nhập quá nhiều lần trong thời gian ngắn. Hệ thống chặn và hiển thị Dialog yêu cầu chờ. *(Quay lại bước 1)*.<br><br>**(Rẽ nhánh từ bước 6 - Sai thông tin)**<br>**6.1.** Sai email hoặc mật khẩu. Hệ thống hiển thị Dialog báo lỗi. *(Quay lại bước 1)*.<br><br>**(Rẽ nhánh từ bước 7 - Email chưa xác thực)**<br>**7.1.** Email chưa được xác minh. Hệ thống hiển thị Dialog yêu cầu xác minh, kèm nút "Gửi lại". *(Quay lại bước 1)*.<br><br>**(Rẽ nhánh từ bước 8 - Tài khoản bị khóa)**<br>**8.1.** Admin đã khóa tài khoản. Hệ thống hiển thị Dialog thông báo lý do và thời gian khóa. Buộc đăng xuất.<br>**8.2.** *[Thời gian thực]* Admin mở khóa tài khoản trong khi Khách truy cập đang xem Dialog bị khóa. Hệ thống đóng Dialog khóa, hiển thị thông báo mở khóa thành công. Khách truy cập bấm xác nhận để vào ứng dụng. |

## 3. Use case: Xác minh danh tính

| Trường | Nội dung |
| :--- | :--- |
| **Tên Use case** | Xác minh danh tính |
| **Tác nhân chính** | Người thuê |
| **Mục đích** | Cho phép Người thuê nộp hồ sơ xác minh danh tính bằng thẻ CCCD để trở thành Người cho thuê và được cấp quyền đăng bài. |
| **Mức độ ưu tiên** | Cao (Bắt buộc) |
| **Điều kiện kích hoạt** | Người thuê truy cập vào màn hình Xác minh danh tính. |
| **Điều kiện tiên quyết** | Người thuê đã đăng nhập. |
| **Điều kiện thành công** | Hồ sơ được hệ thống tự động đối chiếu thành công (cấp quyền ngay lập tức) hoặc được lưu trữ và chuyển lên Quản trị viên chờ duyệt. |
| **Điều kiện thất bại** | Người thuê hủy bỏ tác vụ hoặc dữ liệu không hợp lệ. Hồ sơ không được gửi đi; hệ thống hiển thị thông báo tương ứng. |
| **Luồng sự kiện chính** | **1.** Người thuê truy cập màn hình Xác minh danh tính.<br>**2.** Hệ thống tự động kiểm tra trạng thái hồ sơ.<br>**3.** Nếu chưa xác minh, hệ thống tự động điền và khóa các thông tin cá nhân (Họ tên, SĐT, Email).<br>**4.** Người thuê chụp ảnh mặt trước và sau CCCD trực tiếp từ ứng dụng.<br>**5.** Người thuê nhập Số CCCD, Địa chỉ hiện tại và chọn đồng ý điều khoản.<br>**6.** Người thuê nhấn "Gửi xác minh".<br>**7.** Hệ thống kiểm tra tính hợp lệ của dữ liệu.<br>**8.** Hệ thống kiểm tra Số CCCD có bị trùng với tài khoản khác không.<br>**9.** Hệ thống đối chiếu ảnh CCCD và thông tin người dùng.<br>**10.** Hệ thống xác nhận hình ảnh hợp lệ, thông tin khớp hoàn toàn.<br>**11.** Hệ thống cấp quyền đăng bài, hiển thị thông báo "Xác minh thành công".<br>**12.** Người thuê đóng hộp thoại, hệ thống chuyển hướng về màn hình chính. *(Use case kết thúc)*. |
| **Luồng thay thế** | **(Rẽ nhánh từ bước 2 - Đã có hồ sơ)**<br>**2.1.** Hệ thống phát hiện tài khoản đang có hồ sơ chờ duyệt hoặc đã xác minh.<br>**2.2.** Hệ thống hiển thị Dialog thông báo trạng thái tương ứng (Đang chờ duyệt / Đã xác minh) và đóng màn hình. *(Use case kết thúc)*.<br><br>**(Rẽ nhánh từ bước 10 - Vượt quá số lần tự động)**<br>**10.1.** Hồ sơ đối chiếu thất bại quá 3 lần trong ngày.<br>**10.2.** Hệ thống chuyển trạng thái hồ sơ thành "Chờ duyệt thủ công".<br>**10.3.** Hệ thống thông báo Người thuê chờ Quản trị viên duyệt trong 24 giờ và đóng màn hình. *(Use case kết thúc)*. |
| **Luồng ngoại lệ** | **(Rẽ nhánh từ bước 4, 5 - Hủy bỏ tác vụ)**<br>**5.1.** Người thuê bấm nút Quay lại khi đã điền một phần thông tin.<br>**5.2.** Hệ thống cảnh báo mất dữ liệu chưa lưu.<br>**5.3.** Người thuê chọn "Thoát". Hệ thống đóng màn hình. *(Use case kết thúc - Hủy bỏ)*.<br><br>**(Rẽ nhánh từ bước 7 - Thiếu/Sai dữ liệu)**<br>**7.1.** Thông tin chưa đầy đủ hoặc không hợp lệ. Hệ thống hiển thị thông báo lỗi. *(Quay lại bước 4)*.<br><br>**(Rẽ nhánh từ bước 8 - Trùng Số CCCD)**<br>**8.1.** Hệ thống phát hiện Số CCCD đã tồn tại.<br>**8.2.** Hệ thống từ chối và yêu cầu nhập Số CCCD khác. *(Quay lại bước 5)*.<br><br>**(Rẽ nhánh từ bước 10 - Đối chiếu thất bại nhưng còn lượt)**<br>**10.1.** Hồ sơ đối chiếu thất bại nhưng chưa vượt quá số lần cho phép trong ngày.<br>**10.2.** Hệ thống thông báo lý do thất bại và số lần thử còn lại. *(Quay lại bước 4)*. |

## 4. Use case: Quên mật khẩu

| Trường | Nội dung |
| :--- | :--- |
| **Tên Use case** | Quên mật khẩu |
| **Tác nhân chính** | Khách truy cập |
| **Mục đích** | Yêu cầu hệ thống gửi liên kết đặt lại mật khẩu qua email khi quên mật khẩu cũ. |
| **Mức độ ưu tiên** | Cao (Bắt buộc) |
| **Điều kiện kích hoạt** | Khách truy cập nhập email và kích hoạt lệnh "Gửi email xác nhận". |
| **Điều kiện tiên quyết** | Khách truy cập chưa đăng nhập. |
| **Điều kiện thành công** | Hệ thống gửi thành công email chứa liên kết. Hiển thị thông báo và đưa khách truy cập về màn hình Đăng nhập. |
| **Điều kiện thất bại** | Yêu cầu không được gửi đi. Hệ thống hiển thị thông báo lỗi, khách truy cập vẫn ở màn hình Quên mật khẩu. |
| **Luồng sự kiện chính** | **1.** Khách truy cập nhập Email cần khôi phục.<br>**2.** Khách truy cập bấm nút "Gửi email xác nhận".<br>**3.** Hệ thống kiểm tra giới hạn gửi yêu cầu.<br>**4.** Hệ thống kiểm tra Email hợp lệ.<br>**5.** Hệ thống gửi yêu cầu xác thực lên Firebase.<br>**6.** Firebase gửi email chứa liên kết đặt lại mật khẩu.<br>**7.** Hệ thống hiển thị Dialog thông báo đã gửi liên kết thành công.<br>**8.** Khách truy cập bấm "Đã hiểu" trên Dialog.<br>**9.** Hệ thống điều hướng khách truy cập về màn hình Đăng nhập. *(Use case kết thúc)*. |
| **Luồng thay thế** | **(Rẽ nhánh từ bước 1 - Kích hoạt bằng phím ảo)**<br>**1.1.** Khách truy cập nhập xong thông tin ở ô Email.<br>**1.2.** Khách truy cập nhấn phím "Hoàn tất" (Done) trên bàn phím điện thoại thay vì bấm nút trên màn hình.<br>**1.3.** Hệ thống tự động bắt sự kiện và kích hoạt lệnh gửi email.<br>*(Chuyển tiếp đến bước 3 của Luồng chính)*. |
| **Luồng ngoại lệ** | **(Rẽ nhánh từ bước 1 - Hủy bỏ tác vụ)**<br>**1.4.** Khách truy cập bấm nút "Quay lại" hoặc dòng "Đã nhớ mật khẩu...". Hệ thống chuyển về màn hình Đăng nhập. *(Use case kết thúc - Hủy bỏ)*.<br><br>**(Rẽ nhánh từ bước 3 - Spam yêu cầu)**<br>**3.1.** Khách truy cập gửi yêu cầu quá nhanh. Hệ thống chặn và hiển thị thông báo lỗi. *(Quay lại bước 1)*.<br><br>**(Rẽ nhánh từ bước 4 - Email không hợp lệ)**<br>**4.1.** Email trống hoặc sai định dạng. Hệ thống hiển thị lỗi ngay dưới ô nhập. *(Quay lại bước 1)*.<br><br>**(Rẽ nhánh từ bước 6 - Yêu cầu thất bại)**<br>**6.1.** Yêu cầu gửi email thất bại. Hệ thống hiển thị Dialog báo lỗi. *(Quay lại bước 1)*. |

## 5. Use case: Đăng nhập trang quản trị

| Trường | Nội dung |
| :--- | :--- |
| **Tên Use case** | Đăng nhập trang quản trị |
| **Tác nhân chính** | Quản trị viên |
| **Mục đích** | Xác thực danh tính Quản trị viên để cho phép truy cập vào bảng điều khiển và các chức năng quản lý hệ thống. |
| **Mức độ ưu tiên** | Cao (Bắt buộc) |
| **Điều kiện kích hoạt** | Quản trị viên truy cập vào đường dẫn trang quản trị, điền Email/Mật khẩu và kích hoạt lệnh "Đăng nhập". |
| **Điều kiện tiên quyết** | Quản trị viên chưa đăng nhập và bắt buộc phải được cấp một tài khoản có quyền Quản trị viên trong cơ sở dữ liệu. |
| **Điều kiện thành công** | Xác thực thành công và vượt qua vòng kiểm tra phân quyền. Hệ thống hiển thị giao diện Quản trị và kích hoạt các tính năng thời gian thực. |
| **Điều kiện thất bại** | Xác thực thất bại hoặc tài khoản không có quyền Quản trị viên. Hệ thống tự động đăng xuất (nếu sai quyền) và giữ người dùng ở màn hình Đăng nhập kèm thông báo lỗi. |
| **Luồng sự kiện chính** | **1.** Quản trị viên nhập Email và Mật khẩu trên biểu mẫu.<br>**2.** Quản trị viên bấm nút "Đăng nhập" bằng chuột.<br>**3.** Hệ thống kiểm tra thông tin đầu vào (không bị trống).<br>**4.** Hệ thống vô hiệu hóa nút bấm và đổi chữ thành "Đang đăng nhập...".<br>**5.** Hệ thống gửi yêu cầu xác thực lên máy chủ.<br>**6.** Máy chủ xác thực thông tin hợp lệ.<br>**7.** Hệ thống tự động gọi cơ sở dữ liệu để lấy thông tin tài khoản người dùng vừa đăng nhập.<br>**8.** Hệ thống kiểm tra quyền hạn (tài khoản phải tồn tại và có quyền Quản trị viên).<br>**9.** Hệ thống ẩn màn hình Đăng nhập và hiển thị giao diện Quản trị chính.<br>**10.** Hệ thống lấy Tên hiển thị gắn vào thanh điều hướng bên trái.<br>**11.** Hệ thống chạy hiệu ứng chào mừng nếu là lần đăng nhập mới.<br>**12.** Hệ thống kích hoạt kết nối dữ liệu thời gian thực và tải Bảng điều khiển. *(Use case kết thúc)*. |
| **Luồng thay thế** | **(Rẽ nhánh từ bước 1 - Đăng nhập bằng phím Enter)**<br>**1.1.** Quản trị viên nhập xong thông tin ở ô Mật khẩu.<br>**1.2.** Quản trị viên nhấn phím "Enter" trên bàn phím máy tính thay vì dùng chuột bấm nút.<br>**1.3.** Hệ thống bắt sự kiện nhấn phím và tự động kích hoạt lệnh Đăng nhập.<br>*(Chuyển tiếp đến bước 3 của Luồng chính)*. |
| **Luồng ngoại lệ** | **(Rẽ nhánh từ bước 3 - Bỏ trống trường)**<br>**3.1.** Email hoặc Mật khẩu bị bỏ trống. Hệ thống chặn lại và hiển thị lỗi: "Vui lòng nhập đầy đủ email và mật khẩu". *(Quay lại bước 1)*.<br><br>**(Rẽ nhánh từ bước 6 - Sai Email/Mật khẩu)**<br>**6.1.** Nhập sai thông tin. Máy chủ báo lỗi. Hệ thống khôi phục nút bấm và báo lỗi: "Email hoặc mật khẩu không chính xác". *(Quay lại bước 1)*.<br><br>**(Rẽ nhánh từ bước 8 - Tài khoản không có quyền Quản trị viên)**<br>**8.1.** Xác thực thành công nhưng khi tra cứu cơ sở dữ liệu thì tài khoản không có quyền Quản trị viên (hoặc tài khoản không tồn tại).<br>**8.2.** Hệ thống lập tức gọi lệnh cưỡng chế đăng xuất, ngăn chặn truy cập trái phép.<br>**8.3.** Hệ thống báo lỗi lên màn hình: "Tài khoản không có quyền Quản trị viên...". *(Quay lại bước 1)*.<br><br>**(Rẽ nhánh từ bước 8 - Lỗi mạng/Hệ thống)**<br>**8.4.** Quá trình truy vấn cơ sở dữ liệu thất bại do mạng. Hệ thống hiển thị hộp thoại báo lỗi chi tiết, tiến hành đăng xuất và ẩn màn hình chờ. *(Quay lại bước 1)*. |

## 6. Use case: Đăng bài cho thuê

| Trường | Nội dung |
| :--- | :--- |
| **Tên Use case** | Đăng bài cho thuê phòng trọ |
| **Tác nhân chính** | Người cho thuê |
| **Mục đích** | Cho phép người cho thuê tạo bài đăng mới để tìm khách thuê phòng. |
| **Mức độ ưu tiên** | Cao (Bắt buộc) |
| **Điều kiện kích hoạt** | Người cho thuê chọn chức năng Đăng bài. |
| **Điều kiện tiên quyết** | Tài khoản đã xác minh, không bị khóa quyền đăng và còn lượt đăng bài. |
| **Điều kiện thành công** | Bài đăng được tạo thành công ở trạng thái chờ duyệt và hệ thống trừ 1 lượt đăng bài. |
| **Điều kiện thất bại** | Thông tin điền không hợp lệ hoặc tài khoản hết lượt đăng bài. Hệ thống giữ nguyên thông tin trên màn hình. |
| **Luồng sự kiện chính** | **1.** Người cho thuê chọn chức năng Đăng bài.<br>**2.** Hệ thống kiểm tra điều kiện đăng bài.<br>**3.** Hệ thống hiển thị Form đăng bài kèm thông tin cá nhân đã được khóa cố định.<br>**4.** Người cho thuê điền thông tin phòng trọ và chọn hình ảnh tải lên.<br>**5.** Người cho thuê nhấn nút "Đăng tin".<br>**6.** Hệ thống kiểm tra tính hợp lệ của dữ liệu đầu vào.<br>**7.** Hệ thống nén hình ảnh, lưu bài đăng ở trạng thái chờ duyệt và trừ lượt đăng bài.<br>**8.** Hệ thống thông báo thành công và chuyển về màn hình chính. *(Use case kết thúc)*. |
| **Luồng ngoại lệ** | **(Rẽ nhánh từ bước 2 - Không đủ điều kiện)**<br>**2.1.** Hệ thống phát hiện tài khoản hết lượt đăng bài hoặc đang trong thời gian chờ mở khóa (chưa đủ 24h kể từ lúc Quản trị viên duyệt).<br>**2.2.** Hệ thống thông báo yêu cầu mua thêm lượt hoặc chờ đợi và chặn chức năng đăng bài. *(Use case kết thúc - Thất bại)*.<br><br>**(Rẽ nhánh từ bước 6 - Thông tin không hợp lệ)**<br>**6.1.** Hệ thống phát hiện thiếu trường dữ liệu bắt buộc hoặc sai định dạng.<br>**6.2.** Hệ thống hiển thị cảnh báo lỗi tại các ô tương ứng để người cho thuê sửa lại. *(Quay lại bước 4)*. |




## 7. Use case: Xem danh sách bài đăng

| Trường | Nội dung |
| :--- | :--- |
| **Tên Use case** | Quản lý bài đăng |
| **Tác nhân chính** | Người cho thuê |
| **Mục đích** | Xem danh sách bài đăng của chính mình kèm trạng thái kiểm duyệt, và sử dụng bộ lọc để quản lý. |
| **Mức độ ưu tiên** | Trung bình |
| **Điều kiện kích hoạt** | Người cho thuê truy cập mục "Quản lý bài đăng". |
| **Điều kiện tiên quyết** | Tài khoản đã đăng nhập với vai trò Người cho thuê. |
| **Điều kiện thành công** | Hệ thống hiển thị danh sách bài đăng được sắp xếp theo trạng thái ưu tiên. |
| **Điều kiện thất bại** | Tài khoản chưa có bài đăng nào. Hệ thống hiển thị thông báo danh sách trống. |
| **Luồng sự kiện chính** | **1.** Người cho thuê chọn mục Quản lý bài đăng.<br>**2.** Hệ thống truy vấn danh sách bài đăng và xóa thông báo (đánh dấu đã đọc).<br>**3.** Hệ thống sắp xếp danh sách, đẩy các bài đăng bị "Từ chối" và "Chờ duyệt" lên trên cùng.<br>**4.** Hệ thống hiển thị danh sách bài đăng ra màn hình.<br>**5.** Người cho thuê nhập từ khóa hoặc chọn bộ lọc trạng thái (Chờ duyệt, Đã duyệt, v.v.).<br>**6.** Hệ thống lọc danh sách và hiển thị kết quả tương ứng. *(Use case kết thúc)*. |
| **Luồng ngoại lệ** | **(Rẽ nhánh từ bước 2 - Danh sách trống)**<br>**2.1.** Hệ thống không tìm thấy bài đăng nào của người dùng.<br>**2.2.** Hệ thống hiển thị thông báo "Chưa có bài đăng nào". *(Use case kết thúc - Thất bại)*. |

## 8. Use case: Chỉnh sửa bài đăng

| Trường | Nội dung |
| :--- | :--- |
| **Tên Use case** | Chỉnh sửa bài đăng |
| **Tác nhân chính** | Người cho thuê |
| **Mục đích** | Sửa đổi thông tin hoặc hình ảnh của một bài đăng đang chờ duyệt hoặc bị từ chối để gửi lại cho Quản trị viên. |
| **Mức độ ưu tiên** | Cao |
| **Điều kiện kích hoạt** | Người dùng bấm nút "Sửa bài" tại danh sách bài đăng (chỉ áp dụng cho bài Chờ duyệt hoặc Bị từ chối). |
| **Điều kiện tiên quyết** | Bài đăng không ở trạng thái "Đã duyệt" hoặc "Đã cho thuê". |
| **Điều kiện thành công** | Dữ liệu cập nhật thành công, trạng thái bài đăng chuyển về "Chờ duyệt". |
| **Điều kiện thất bại** | Dữ liệu không hợp lệ hoặc người cho thuê hủy thao tác. Hệ thống giữ nguyên thông tin trên màn hình. |
| **Luồng sự kiện chính** | **1.** Người cho thuê chọn chức năng sửa bài đăng.<br>**2.** Hệ thống hiển thị Form chứa dữ liệu cũ của bài đăng.<br>**3.** Người cho thuê chỉnh sửa thông tin và hình ảnh.<br>**4.** Người cho thuê nhấn nút cập nhật.<br>**5.** Hệ thống kiểm tra tính hợp lệ của dữ liệu.<br>**6.** Hệ thống xử lý hình ảnh, lưu thông tin mới và đổi trạng thái bài đăng thành "Chờ duyệt".<br>**7.** Hệ thống thông báo thành công và chuyển về danh sách bài đăng. *(Use case kết thúc)*. |
| **Luồng ngoại lệ** | **(Rẽ nhánh từ bước 5 - Dữ liệu không hợp lệ)**<br>**5.1.** Hệ thống phát hiện dữ liệu trống hoặc sai định dạng.<br>**5.2.** Hệ thống hiển thị cảnh báo lỗi tại các ô tương ứng. *(Quay lại bước 3)*.<br><br>**(Rẽ nhánh từ bước 3 - Hủy thao tác)**<br>**3.1.** Người cho thuê chọn nút quay lại hoặc hủy bỏ.<br>**3.2.** Hệ thống hiển thị xác nhận hủy, sau đó đóng Form và không lưu thay đổi. *(Use case kết thúc - Thất bại)*. |

## 9. Use case: Xóa bài đăng

| Trường | Nội dung |
| :--- | :--- |
| **Tên Use case** | Xóa bài đăng |
| **Tác nhân chính** | Người cho thuê |
| **Mục đích** | Xóa hoàn toàn một bài đăng và mọi hình ảnh liên quan khỏi cơ sở dữ liệu. |
| **Mức độ ưu tiên** | Trung bình |
| **Điều kiện kích hoạt** | Người dùng bấm nút "Xóa" trên thẻ bài đăng. |
| **Điều kiện tiên quyết** | Tài khoản hợp lệ, người dùng đang ở danh sách bài đăng của chính mình. |
| **Điều kiện thành công** | Bài đăng và mọi hình ảnh liên quan bị xóa hoàn toàn khỏi hệ thống. |
| **Điều kiện thất bại** | Người cho thuê hủy thao tác xóa. |
| **Luồng sự kiện chính** | **1.** Người cho thuê chọn chức năng xóa bài đăng.<br>**2.** Hệ thống hiển thị hộp thoại cảnh báo xác nhận xóa.<br>**3.** Người cho thuê nhấn nút đồng ý xóa.<br>**4.** Hệ thống xóa toàn bộ dữ liệu bài đăng và hình ảnh liên quan.<br>**5.** Hệ thống thông báo xóa thành công và làm mới danh sách. *(Use case kết thúc)*. |
| **Luồng ngoại lệ** | **(Rẽ nhánh từ bước 2 - Hủy bỏ)**<br>**2.1.** Người cho thuê chọn nút đóng hoặc hủy.<br>**2.2.** Hệ thống đóng hộp thoại cảnh báo và giữ nguyên bài đăng. *(Use case kết thúc - Thất bại)*. |


## 10. Use case: Đẩy nổi bật bài đăng

| Trường | Nội dung |
| :--- | :--- |
| **Tên Use case** | Đẩy nổi bật bài đăng |
| **Tác nhân chính** | Người cho thuê |
| **Mục đích** | Mua gói dịch vụ để ưu tiên hiển thị bài đăng lên đầu danh sách tìm kiếm thông qua thanh toán mã QR tự động. |
| **Mức độ ưu tiên** | Trung bình |
| **Điều kiện kích hoạt** | Người dùng bấm "Đẩy nổi bật" trên bài đăng. |
| **Điều kiện tiên quyết** | Bài đăng đang ở trạng thái "Đã duyệt" và hiện tại không trong thời hạn nổi bật. |
| **Điều kiện thành công** | Người cho thuê chuyển khoản đúng thông tin, hệ thống tự động ghi nhận và chuyển trạng thái chờ duyệt nổi bật. |
| **Điều kiện thất bại** | Người cho thuê hủy thao tác, giao dịch quá hạn đếm ngược, hoặc chuyển sai thông tin. |
| **Luồng sự kiện chính** | **1.** Người cho thuê chọn chức năng đẩy nổi bật bài đăng.<br>**2.** Hệ thống kiểm tra và không phát hiện giao dịch treo, sau đó hiển thị danh sách các gói nổi bật.<br>**3.** Người cho thuê chọn một gói và nhấn tiếp tục.<br>**4.** Hệ thống hiển thị cảnh báo lưu ý quan trọng về việc giữ nguyên nội dung chuyển khoản.<br>**5.** Người cho thuê nhấn nút xác nhận đã hiểu.<br>**6.** Hệ thống hiển thị mã VietQR thanh toán và bắt đầu đếm ngược 30 phút.<br>**7.** Người cho thuê quét mã QR và hoàn tất chuyển khoản trên ứng dụng ngân hàng.<br>**8.** Hệ thống tự động nhận diện thanh toán thành công và cập nhật trạng thái "Chờ admin duyệt".<br>**9.** Hệ thống thông báo giao dịch thành công và đóng hộp thoại. *(Use case kết thúc)*. |
| **Luồng ngoại lệ** | **(Rẽ nhánh từ bước 2 - Có giao dịch đang chờ thanh toán)**<br>**2.1.** Hệ thống phát hiện có giao dịch đẩy nổi bật cũ chưa thanh toán.<br>**2.2.** Hệ thống hiển thị ngay mã QR của giao dịch đó. *(Chuyển tiếp đến bước 6 Luồng chính)*.<br><br>**(Rẽ nhánh từ bước 2 - Có giao dịch bị từ chối)**<br>**2.1.** Hệ thống phát hiện giao dịch đẩy nổi bật cũ bị Quản trị viên từ chối.<br>**2.2.** Hệ thống hiển thị lý do từ chối và nút gửi lại yêu cầu.<br>**2.3.** Người cho thuê nhấn nút gửi lại yêu cầu.<br>**2.4.** Hệ thống cập nhật lại trạng thái thành "Chờ admin duyệt". *(Use case kết thúc)*.<br><br>**(Rẽ nhánh từ bước 3, 5 - Hủy thao tác)**<br>**3.1.** Người cho thuê nhấn nút đóng hoặc quay lại.<br>**3.2.** Hệ thống đóng hộp thoại và hủy thao tác. *(Use case kết thúc - Thất bại)*.<br><br>**(Rẽ nhánh từ bước 6 - Chủ động hủy giao dịch)**<br>**6.1.** Người cho thuê nhấn nút hủy giao dịch khi mã QR đang hiển thị.<br>**6.2.** Hệ thống hiển thị hộp thoại xác nhận hủy.<br>**6.3.** Người cho thuê đồng ý hủy. Hệ thống cập nhật trạng thái hủy và đóng hộp thoại. *(Use case kết thúc - Thất bại)*.<br><br>**(Rẽ nhánh từ bước 7 - Giao dịch hết hạn)**<br>**7.1.** Hệ thống đếm ngược hết 30 phút mà chưa nhận được thanh toán.<br>**7.2.** Hệ thống hiển thị thông báo giao dịch đã hết hạn và khóa nút xác nhận. *(Use case kết thúc - Thất bại)*.<br><br>**(Rẽ nhánh từ bước 7 - Chuyển sai thông tin)**<br>**7.1.** Hệ thống phát hiện giao dịch có số tiền hoặc nội dung không khớp.<br>**7.2.** Hệ thống hiển thị cảnh báo sai số tiền và yêu cầu liên hệ hỗ trợ. *(Use case kết thúc - Thất bại)*. |

## 11. Use case: Đặt lịch hẹn

| Trường | Nội dung |
| :--- | :--- |
| **Tên Use case** | Đặt lịch hẹn |
| **Tác nhân chính** | Người thuê, Người cho thuê *(chỉ được đặt lịch phòng của người khác, không phải phòng do chính mình đăng)* |
| **Mục đích** | Cho phép người dùng đặt lịch hẹn trực tiếp với chủ nhà để đến xem phòng trọ. |
| **Mức độ ưu tiên** | Cao |
| **Điều kiện kích hoạt** | Người dùng bấm nút "Đặt lịch" trên màn hình Chi tiết phòng trọ. |
| **Điều kiện tiên quyết** | Tài khoản đã đăng nhập. Người dùng không phải là chủ bài đăng. Chưa có lịch hẹn đang chờ hoặc đã xác nhận với phòng này. Tổng lịch đang chờ xác nhận < 3. Tổng lịch đã xác nhận < 2. Số lần bỏ lỡ lịch hẹn < 3. |
| **Điều kiện thành công** | Lịch hẹn được lưu trạng thái "Chờ xác nhận", chủ nhà nhận thông báo đẩy kèm hạn xác nhận 48 giờ, lịch hẹn hiển thị trong "Lịch hẹn của tôi". |
| **Điều kiện thất bại** | Người dùng bị hạn chế đặt lịch (vượt giới hạn lịch chờ/xác nhận, bỏ lỡ lịch quá nhiều lần, hết lượt đặt trong ngày). |
| **Luồng sự kiện chính** | **1.** Người dùng bấm nút "Đặt lịch" tại màn hình Chi tiết phòng trọ.<br>**2.** Hệ thống kiểm tra điều kiện đặt lịch và hiển thị màn hình chọn ngày.<br>**3.** Người dùng chọn ngày hẹn trên lịch và bấm "Tiếp theo".<br>**4.** Hệ thống kiểm tra số lượt đặt trong ngày và hiển thị danh sách khung giờ khả dụng.<br>**5.** Người dùng chọn khung giờ và bấm "Tiếp theo".<br>**6.** Hệ thống hiển thị form điền thông tin cá nhân.<br>**7.** Người dùng kiểm tra, chỉnh sửa thông tin và bấm "Tiếp theo".<br>**8.** Hệ thống xác thực thông tin và hiển thị màn hình tóm tắt.<br>**9.** Người dùng kiểm tra bản tóm tắt và bấm "Xác nhận đặt lịch".<br>**10.** Hệ thống hiển thị hộp thoại xác nhận cuối cùng.<br>**11.** Người dùng bấm "Đồng ý".<br>**12.** Hệ thống lưu thông tin, gửi thông báo cho chủ nhà và thông báo đặt lịch thành công. *(Use case kết thúc)*. |
| **Luồng ngoại lệ** | **(Rẽ nhánh từ bước 2 - Vi phạm điều kiện đặt lịch)**<br>**2.1.** Hệ thống phát hiện người dùng vi phạm điều kiện (đã có lịch với phòng này, vượt giới hạn số lịch đang có, hoặc bị cấm do bỏ lỡ lịch).<br>**2.2.** Hệ thống hiển thị thông báo lỗi tương ứng và chặn đặt lịch. *(Use case kết thúc)*.<br><br>**(Rẽ nhánh từ bước 4 - Ngày hẹn đã có lịch từ trước)**<br>**4.1.** Hệ thống phát hiện ngày được chọn đã có lịch hẹn trước đó.<br>**4.2.** Hệ thống cảnh báo các khung giờ đã bị chiếm.<br>**4.3.** Người dùng bấm "Tiếp tục" để vẫn chọn ngày này. *(Tiếp tục bước 4)*.<br><br>**(Rẽ nhánh từ bước 4 - Hết lượt đặt trong ngày)**<br>**4.1.** Hệ thống phát hiện người dùng đã dùng hết 5 lượt đặt lịch trong ngày.<br>**4.2.** Hệ thống báo lỗi và yêu cầu chọn ngày khác. *(Quay lại bước 3)*.<br><br>**(Rẽ nhánh từ bước 8 - Thông tin cá nhân không hợp lệ)**<br>**8.1.** Hệ thống phát hiện thông tin bị bỏ trống hoặc sai định dạng.<br>**8.2.** Hệ thống báo lỗi và giữ nguyên màn hình nhập liệu. *(Quay lại bước 7)*.<br><br>**(Rẽ nhánh từ bước 9 - Sửa lại thông tin)**<br>**9.1.** Người dùng bấm nút "Sửa thông tin" trên màn hình tóm tắt.<br>**9.2.** Hệ thống quay trở lại form thông tin cá nhân. *(Quay lại bước 6)*. |



## 12. Use case: Tìm kiếm phòng trọ theo bộ lọc


| Trường | Nội dung |
| :--- | :--- |
| **Tên Use case** | Tìm kiếm phòng trọ theo bộ lọc |
| **Tác nhân chính** | Khách truy cập, Người thuê, Người cho thuê |
| **Mục đích** | Cho phép người dùng tìm kiếm phòng trọ theo các tiêu chí mong muốn. |
| **Mức độ ưu tiên** | Rất cao |
| **Điều kiện kích hoạt** | Người dùng chọn chức năng Tìm kiếm. |
| **Điều kiện tiên quyết** | Không yêu cầu đăng nhập. |
| **Điều kiện thành công** | Hệ thống hiển thị danh sách phòng trọ phù hợp với tiêu chí lọc. |
| **Điều kiện thất bại** | Người dùng chưa chọn khu vực hợp lệ. |
| **Luồng sự kiện chính** | **1.** Người dùng chọn chức năng Tìm kiếm.<br>**2.** Hệ thống hiển thị biểu mẫu lọc.<br>**3.** Người dùng nhập các tiêu chí (có thể nhấn "Đặt lại" để xóa tiêu chí cũ) và nhấn "Tìm kiếm".<br>**4.** Hệ thống truy vấn phòng trọ theo khu vực.<br>**5.** Hệ thống loại bỏ các phòng hết hạn hoặc đã cho thuê hết.<br>**6.** Hệ thống đối chiếu dữ liệu với các tiêu chí lọc.<br>**7.** Hệ thống tính điểm phù hợp, sắp xếp và hiển thị kết quả. |
| **Luồng ngoại lệ** | **(Rẽ nhánh từ bước 3 - Khu vực không hợp lệ)**<br>**3.1.** Người dùng nhấn "Tìm kiếm" nhưng chưa chọn hoặc chọn khu vực không hợp lệ.<br>**3.2.** Hệ thống hiển thị cảnh báo yêu cầu chọn khu vực. *(Quay lại bước 2)*.<br><br>**(Rẽ nhánh từ bước 7 - Không có kết quả)**<br>**7.1.** Hệ thống không tìm thấy phòng trọ nào khớp với tiêu chí.<br>**7.2.** Hệ thống hiển thị thông báo không có kết quả phù hợp. *(Use case kết thúc)*. |

## 13. Use case: Tìm kiếm phòng trọ bằng bản đồ

| Trường | Nội dung |
| :--- | :--- |
| **Tên Use case** | Tìm kiếm phòng trọ bằng bản đồ |
| **Tác nhân chính** | Khách truy cập, Người thuê, Người cho thuê |
| **Mục đích** | Cho phép người dùng tìm phòng trọ xung quanh một vị trí cụ thể trên bản đồ. |
| **Mức độ ưu tiên** | Cao |
| **Điều kiện kích hoạt** | Người dùng chọn "Chọn vị trí trên bản đồ" hoặc "Dùng vị trí của tôi". |
| **Điều kiện tiên quyết** | Không yêu cầu đăng nhập. Cần cấp quyền vị trí nếu dùng tính năng GPS. |
| **Điều kiện thành công** | Hệ thống hiển thị danh sách phòng trọ theo khoảng cách gần nhất. |
| **Điều kiện thất bại** | Người dùng từ chối cấp quyền vị trí khi dùng GPS. |
| **Luồng sự kiện chính** | **1.** Người dùng chọn "Chọn vị trí trên bản đồ" hoặc "Dùng vị trí của tôi".<br>**2.** Hệ thống kiểm tra quyền vị trí (nếu cần) và hiển thị bản đồ.<br>**3.** Người dùng nhập địa điểm hoặc chọn dùng vị trí hiện tại.<br>**4.** Hệ thống đặt tâm điểm và tải danh sách phòng trọ lân cận.<br>**5.** Người dùng điều chỉnh thanh trượt bán kính.<br>**6.** Hệ thống lọc lại danh sách phòng trọ theo bán kính mới.<br>**7.** Người dùng chọn các phòng trọ mong muốn (hoặc bỏ trống để lấy cả khu vực) và nhấn "Xác nhận vị trí".<br>**8.** Hệ thống lưu vị trí và quay lại màn hình Tìm kiếm.<br>**9.** Người dùng nhấn "Tìm kiếm".<br>**10.** Hệ thống hiển thị danh sách kết quả tương ứng. |
| **Luồng ngoại lệ** | **(Rẽ nhánh từ bước 2 - Từ chối quyền vị trí)**<br>**2.1.** Người dùng từ chối cấp quyền vị trí.<br>**2.2.** Hệ thống hiển thị thông báo yêu cầu cấp quyền và hủy thao tác. *(Use case kết thúc)*.<br><br>**(Rẽ nhánh từ bước 4 - Không có phòng trong bán kính)**<br>**4.1.** Hệ thống không tìm thấy phòng trọ nào.<br>**4.2.** Hệ thống hiển thị thông báo không có phòng trọ và hiển thị nút gợi ý mở rộng bán kính. *(Quay lại bước 5)*. |

## 14. Use case: Xem thông tin chi tiết phòng trọ

| Trường | Nội dung |
| :--- | :--- |
| **Tên Use case** | Xem thông tin chi tiết phòng trọ |
| **Tác nhân chính** | Khách truy cập, Người thuê, Người cho thuê |
| **Mục đích** | Cho phép người dùng xem đầy đủ thông tin của một phòng trọ cụ thể, bao gồm hình ảnh, giá, tiện ích, quy định, vị trí bản đồ và thông tin chủ nhà. |
| **Mức độ ưu tiên** | Rất cao |
| **Điều kiện kích hoạt** | Người dùng bấm vào một thẻ phòng trọ bất kỳ trên màn hình Trang chủ, Kết quả tìm kiếm hoặc Danh sách bài đã lưu. |
| **Điều kiện tiên quyết** | Không yêu cầu đăng nhập (Công khai). Thiết bị có kết nối mạng. |
| **Điều kiện thành công** | Màn hình Chi tiết hiển thị đầy đủ nội dung phòng trọ, vị trí bản đồ và thông tin chủ nhà. |
| **Điều kiện thất bại** | Dữ liệu phòng trọ không tồn tại hoặc lỗi kết nối mạng. |
| **Luồng sự kiện chính** | **1.** Người dùng bấm vào một thẻ phòng trọ.<br>**2.** Hệ thống tải dữ liệu từ máy chủ và hiển thị màn hình Chi tiết phòng trọ.<br>**3.** Màn hình hiển thị bộ ảnh (vuốt để xem từng ảnh), tiêu đề, giá thuê, địa chỉ, diện tích, sức chứa và loại phòng.<br>**4.** Hệ thống hiển thị phần Mô tả; nếu mô tả dài, hệ thống hiển thị nút "Xem thêm" để mở rộng hoặc thu gọn nội dung.<br>**5.** Hệ thống hiển thị các nhóm thông tin chi tiết: Chi phí (điện, nước, wifi, cọc), Cơ sở vật chất, Tiện ích nội thất, Gửi xe, Nội quy và Dịch vụ thêm (nếu có).<br>**6.** Hệ thống hiển thị bản đồ thu nhỏ với điểm đánh dấu vị trí chính xác của phòng trọ.<br>**7.** Hệ thống hiển thị thẻ thông tin chủ nhà (tên, ảnh đại diện, trạng thái xác minh) cùng 2 nút "Gọi điện" và "Nhắn tin". *(Use case kết thúc)*. |
| **Luồng thay thế** | **(Rẽ nhánh từ bước 6 - Mở Google Maps)**<br>**6.1.** Người dùng bấm "Xem trên Google Maps".<br>**6.2.** Hệ thống mở ứng dụng Google Maps với điểm đánh dấu định vị chính xác vị trí phòng trọ. *(Quay lại bước 7 Luồng chính)*.<br><br>**(Rẽ nhánh từ bước 7 - Xem hồ sơ chủ nhà)**<br>**7.1.** Người dùng bấm vào thẻ thông tin chủ nhà.<br>**7.2.** Hệ thống hiển thị bảng thông tin trượt lên (Bottom Sheet) với hồ sơ đầy đủ của chủ nhà: ảnh, số điện thoại, email, ngày tham gia và trạng thái xác minh danh tính.<br>**7.3.** Người dùng có thể bấm "Gọi điện" hoặc "Nhắn tin" trực tiếp từ bảng này. *(Quay lại bước 7 Luồng chính)*.<br><br>**(Rẽ nhánh từ bước 3 - Phòng đã cho thuê hết)**<br>**3.1.** Hệ thống phát hiện toàn bộ phòng trọ trong bài đăng đã được cho thuê.<br>**3.2.** Hệ thống hiển thị nhãn "ĐÃ CHO THUÊ" màu đỏ, vô hiệu hóa nút "Đặt lịch" và ẩn nút "Lưu". *(Tiếp tục bước 4 Luồng chính — chỉ xem)*. |
| **Luồng ngoại lệ** | **(Rẽ nhánh từ bước 2 - Không tìm thấy dữ liệu)**<br>**2.1.** Dữ liệu phòng trọ không tồn tại hoặc đã bị xóa khỏi hệ thống.<br>**2.2.** Hệ thống tự động đóng màn hình và quay về màn hình trước. *(Use case kết thúc - Thất bại)*.<br><br>**(Rẽ nhánh từ bước 7 - Người dùng chưa đăng nhập)**<br>**7.1.** Người dùng chưa đăng nhập bấm nút "Lưu" hoặc "Nhắn tin".<br>**7.2.** Hệ thống hiển thị hộp thoại yêu cầu đăng nhập với lựa chọn "Đăng nhập" hoặc "Hủy".<br>**7.3.** Nếu chọn "Đăng nhập", hệ thống chuyển sang màn hình Đăng nhập. *(Use case kết thúc)*. |

## 15. Use case: Lưu phòng trọ yêu thích

| Trường | Nội dung |
| :--- | :--- |
| **Tên Use case** | Lưu phòng trọ yêu thích (Thêm / Bỏ lưu) |
| **Tác nhân chính** | Người thuê, Người cho thuê |
| **Mục đích** | Cho phép người dùng lưu lại các bài đăng phòng trọ quan tâm vào danh sách yêu thích để dễ dàng xem lại. |
| **Mức độ ưu tiên** | Trung bình |
| **Điều kiện kích hoạt** | Người dùng bấm biểu tượng "Lưu bài" (trái tim/bookmark) trên màn hình Chi tiết phòng trọ. |
| **Điều kiện tiên quyết** | Thiết bị có kết nối mạng. Bài đăng không phải do chính người dùng hiện tại đăng lên (nếu là chủ bài, hệ thống tự động ẩn nút Lưu). |
| **Điều kiện thành công** | Trạng thái lưu được cập nhật thành công, biểu tượng "Lưu bài" đổi màu. |
| **Điều kiện thất bại** | Người dùng chưa đăng nhập, hoặc lỗi kết nối. |
| **Luồng sự kiện chính** | **1.** Người dùng bấm biểu tượng "Lưu bài" tại Chi tiết phòng trọ.<br>**2.** Hệ thống kiểm tra và xác nhận người dùng đã đăng nhập.<br>**3.** Hệ thống kiểm tra trạng thái lưu của bài đăng trong dữ liệu yêu thích của người dùng.<br>**4.** Hệ thống phát hiện bài đăng chưa được lưu, tiến hành thêm thông tin tóm tắt của bài đăng vào danh sách yêu thích.<br>**5.** Hệ thống hiển thị thông báo *"Đã lưu bài đăng"*, đồng thời đổi màu biểu tượng "Lưu bài" sang màu xanh (Primary Color). *(Use case kết thúc)*. |
| **Luồng thay thế** | **(Rẽ nhánh từ bước 4 - Bỏ lưu phòng trọ đã lưu)**<br>**4.1.** Hệ thống phát hiện bài đăng đã có trong danh sách yêu thích.<br>**4.2.** Hệ thống xóa bài đăng khỏi danh sách yêu thích của người dùng.<br>**4.3.** Hệ thống hiển thị thông báo *"Đã bỏ lưu"*, đồng thời đổi màu biểu tượng "Lưu bài" về lại màu xám mặc định. *(Use case kết thúc)*. |
| **Luồng ngoại lệ** | **(Rẽ nhánh từ bước 2 - Người dùng chưa đăng nhập)**<br>**2.1.** Hệ thống phát hiện người dùng là Khách truy cập (chưa đăng nhập).<br>**2.2.** Hệ thống hiển thị hộp thoại yêu cầu đăng nhập.<br>**2.3.** Người dùng bấm "Đăng nhập", hệ thống chuyển sang màn hình Đăng nhập (hoặc bấm "Hủy" để đóng). *(Use case kết thúc)*.<br><br>**(Rẽ nhánh từ bước 4 - Lỗi kết nối)**<br>**4.1.** Quá trình xử lý dữ liệu thất bại do lỗi mạng.<br>**4.2.** Hệ thống báo lỗi *"Không thể thực hiện"* và giữ nguyên trạng thái biểu tượng. *(Use case kết thúc - Thất bại)*. |

## 16. Use case: Mua lượt đăng bài

| Trường | Nội dung |
| :--- | :--- |
| **Tên Use case** | Mua lượt đăng bài |
| **Tác nhân chính** | Người cho thuê |
| **Mục đích** | Cho phép người cho thuê mua thêm lượt đăng bài thông qua thanh toán mã QR tự động. |
| **Mức độ ưu tiên** | Cao |
| **Điều kiện kích hoạt** | Người cho thuê chọn chức năng mua lượt đăng bài. |
| **Điều kiện tiên quyết** | Tài khoản đã đăng nhập với vai trò Người cho thuê. |
| **Điều kiện thành công** | Người cho thuê chuyển khoản đúng thông tin, hệ thống tự động ghi nhận và cộng lượt đăng. |
| **Điều kiện thất bại** | Người cho thuê hủy thao tác, để giao dịch hết hạn hoặc chuyển sai số tiền/nội dung. |
| **Luồng sự kiện chính** | **1.** Người cho thuê chọn chức năng mua lượt đăng bài.<br>**2.** Hệ thống hiển thị hộp thoại chọn gói lượt (gồm các gói cố định hoặc nhập số lượng tùy chọn).<br>**3.** Người cho thuê chọn một gói và nhấn tiếp tục.<br>**4.** Hệ thống hiển thị cảnh báo lưu ý quan trọng về việc giữ nguyên nội dung chuyển khoản.<br>**5.** Người cho thuê nhấn nút xác nhận đã hiểu.<br>**6.** Hệ thống hiển thị mã VietQR thanh toán và bắt đầu đếm ngược 30 phút.<br>**7.** Người cho thuê quét mã QR và hoàn tất chuyển khoản trên ứng dụng ngân hàng.<br>**8.** Hệ thống tự động nhận diện thanh toán thành công và kích hoạt nút hoàn tất.<br>**9.** Người cho thuê nhấn nút "Hoàn tất giao dịch".<br>**10.** Hệ thống thông báo thành công và cộng lượt đăng vào tài khoản. *(Use case kết thúc)*. |
| **Luồng ngoại lệ** | **(Rẽ nhánh từ bước 2, 4 - Hủy chọn gói)**<br>**2.1.** Người cho thuê nhấn nút đóng màn hình hoặc nút quay lại.<br>**2.2.** Hệ thống đóng hộp thoại chọn gói và hủy thao tác. *(Use case kết thúc - Thất bại)*.<br><br>**(Rẽ nhánh từ bước 7 - Chủ động hủy giao dịch)**<br>**7.1.** Người cho thuê nhấn nút "Hủy giao dịch" khi mã QR đang hiển thị.<br>**7.2.** Hệ thống hiển thị hộp thoại cảnh báo xác nhận hủy.<br>**7.3.** Người cho thuê nhấn đồng ý hủy.<br>**7.4.** Hệ thống cập nhật trạng thái hủy giao dịch và đóng hộp thoại. *(Use case kết thúc - Thất bại)*.<br><br>**(Rẽ nhánh từ bước 8 - Hết thời gian)**<br>**8.1.** Hệ thống đếm ngược hết 30 phút mà chưa nhận được thanh toán.<br>**8.2.** Hệ thống hiển thị thông báo giao dịch đã hết hạn và khóa nút xác nhận. *(Use case kết thúc - Thất bại)*.<br><br>**(Rẽ nhánh từ bước 8 - Chuyển sai thông tin)**<br>**8.1.** Hệ thống phát hiện giao dịch có số tiền hoặc nội dung không khớp.<br>**8.2.** Hệ thống hiển thị cảnh báo sai số tiền và yêu cầu liên hệ hỗ trợ. *(Use case kết thúc - Thất bại)*. |

## 17. Use case: Tìm kiếm phòng trọ bằng trợ lý ảo AI

| Trường | Nội dung |
| :--- | :--- |
| **Tên Use case** | Tìm kiếm phòng trọ bằng trợ lý ảo AI |
| **Tác nhân chính** | Người thuê, Người cho thuê |
| **Mục đích** | Cho phép người dùng tìm kiếm phòng trọ và hỏi đáp thông qua hội thoại với trợ lý ảo AI. |
| **Mức độ ưu tiên** | Cao |
| **Điều kiện kích hoạt** | Người dùng chọn biểu tượng Trợ lý ảo AI. |
| **Điều kiện tiên quyết** | Tài khoản đã đăng nhập. |
| **Điều kiện thành công** | Danh sách phòng trọ hoặc câu trả lời được hiển thị trong khung chat. |
| **Điều kiện thất bại** | Người dùng cung cấp địa điểm không có thực và không sửa lại. |
| **Luồng sự kiện chính** | **1.** Người dùng chọn biểu tượng Trợ lý ảo AI.<br>**2.** Hệ thống hiển thị lịch sử hội thoại (nếu có) hoặc tin nhắn chào mừng.<br>**3.** Người dùng nhập yêu cầu (hỏi đáp hoặc tìm kiếm) và nhấn "Gửi".<br>**4.** Hệ thống hiển thị hiệu ứng đang xử lý và gửi nội dung yêu cầu đến Gemini API.<br>**5.** Hệ thống nhận kết quả phân tích từ Gemini API (bao gồm trích xuất tiêu chí tìm kiếm hoặc câu trả lời văn bản).<br>**6.** Hệ thống chuyển đổi địa điểm thành tọa độ địa lý (nếu yêu cầu tìm kiếm chứa vị trí).<br>**7.** Hệ thống truy vấn danh sách phòng trọ và hiển thị vào khung chat kèm câu trả lời tóm tắt.<br>**8.** Hệ thống hiển thị các nút gợi ý thao tác nhanh. |
| **Luồng ngoại lệ** | **(Rẽ nhánh từ bước 6 - Không xác định được vị trí)**<br>**6.1.** Hệ thống không thể tìm thấy tọa độ từ địa điểm được cung cấp.<br>**6.2.** Hệ thống thông báo yêu cầu nhập rõ tên đường, quận/huyện. *(Quay lại bước 3)*.<br><br>**(Rẽ nhánh từ bước 7 - Không tìm thấy kết quả)**<br>**7.1.** Hệ thống không tìm thấy phòng trọ nào khớp với tiêu chí.<br>**7.2.** Hệ thống thông báo không có kết quả và gợi ý mở rộng tìm kiếm. *(Quay lại bước 3)*. |


## 18. Use case: Nhắn tin trực tiếp

| Trường | Nội dung |
| :--- | :--- |
| **Tên Use case** | Nhắn tin trực tiếp |
| **Tác nhân chính** | Người thuê, Người cho thuê |
| **Mục đích** | Cho phép người dùng trao đổi thông tin, hình ảnh trực tiếp với chủ trọ hoặc người thuê khác qua hệ thống chat thời gian thực. |
| **Mức độ ưu tiên** | Rất cao |
| **Điều kiện kích hoạt** | Người dùng bấm nút "Nhắn tin" trên chi tiết phòng trọ, hồ sơ chủ nhà, hoặc chọn một cuộc trò chuyện từ danh sách Lịch sử tin nhắn. |
| **Điều kiện tiên quyết** | Thiết bị có kết nối mạng internet. Người dùng đã đăng nhập vào hệ thống. Không thể tự nhắn tin cho chính mình. |
| **Điều kiện thành công** | Tin nhắn hoặc hình ảnh được gửi đi thành công, hiển thị lên màn hình chat và lưu vào cơ sở dữ liệu. |
| **Điều kiện thất bại** | Lỗi kết nối mạng, người dùng chưa đăng nhập, hoặc tài khoản đối phương đã bị xóa khỏi hệ thống. |
| **Luồng sự kiện chính** | **1.** Người dùng bấm nút "Nhắn tin".<br>**2.** Hệ thống kiểm tra trạng thái đăng nhập. Nếu hợp lệ, hệ thống kiểm tra sự tồn tại của tài khoản đối phương.<br>**3.** Hệ thống tải thông tin người dùng (tên, avatar) và trạng thái hoạt động (Online/Offline) của đối phương.<br>**4.** Hệ thống chuyển sang màn hình Chat, hiển thị tiêu đề và danh sách tin nhắn cũ (nếu có).<br>**5.** Người dùng nhập văn bản vào ô nhập liệu và bấm "Gửi" (hoặc Enter).<br>**6.** Hệ thống lưu tin nhắn và hiển thị lên giao diện chat của cả hai bên.<br>**7.** Hệ thống tự động cuộn màn hình xuống tin nhắn mới nhất.<br>**8.** Khi đối phương mở khung chat, hệ thống tự động cập nhật trạng thái tin nhắn thành "Đã xem". *(Use case kết thúc)*. |
| **Luồng thay thế** | **(Rẽ nhánh từ bước 5 - Gửi hình ảnh)**<br>**5.1.** Người dùng bấm biểu tượng "Gắn ảnh" hoặc "Camera".<br>**5.2.** Người dùng chọn ảnh từ thư viện hoặc chụp ảnh mới.<br>**5.3.** Hệ thống hiển thị "Đang nén ảnh..." và xử lý ảnh.<br>**5.4.** Hệ thống tải ảnh lên và gửi tin nhắn dạng hình ảnh. *(Tiếp tục bước 7 Luồng chính)*.<br><br>**(Rẽ nhánh từ bước 5 - Thu hồi tin nhắn)**<br>**5.1.** Người dùng nhấn giữ vào một tin nhắn của mình.<br>**5.2.** Hệ thống hiển thị xác nhận: *"Bạn có chắc chắn muốn xóa tin nhắn này không?"*.<br>**5.3.** Người dùng bấm "Xóa". Hệ thống thu hồi tin nhắn khỏi cả hai bên.<br><br>**(Rẽ nhánh từ bước 5 - Gọi điện thoại)**<br>**5.1.** Tại màn hình chat, người dùng bấm biểu tượng "Gọi điện".<br>**5.2.** Hệ thống lấy số điện thoại đối phương và mở trình gọi điện có điền sẵn số. |
| **Luồng ngoại lệ** | **(Rẽ nhánh từ bước 2 - Chưa đăng nhập)**<br>**2.1.** Khách truy cập bấm "Nhắn tin".<br>**2.2.** Hệ thống cảnh báo: *"Bạn cần đăng nhập để nhắn tin"*. Hệ thống chuyển sang màn hình Đăng nhập. *(Use case kết thúc)*.<br><br>**(Rẽ nhánh từ bước 2 - Tài khoản đối phương bị xóa)**<br>**2.1.** Tài khoản đối phương không còn tồn tại.<br>**2.2.** Hệ thống báo: *"Tài khoản không tồn tại..."*. Người dùng bấm "Đã hiểu", hệ thống đóng giao diện chat.<br><br>**(Rẽ nhánh từ bước 5 - Gọi điện nhưng chưa có số điện thoại)**<br>**5.1.** Đối phương chưa cập nhật số điện thoại.<br>**5.2.** Hệ thống hiển thị lỗi: *"Người dùng này chưa cập nhật số điện thoại"*. *(Quay lại bước 4)*. |

## 19. Use case: Quản lý thông tin cá nhân

| Trường | Nội dung |
| :--- | :--- |
| **Tên Use case** | Quản lý thông tin cá nhân |
| **Tác nhân chính** | Người thuê, Người cho thuê |
| **Mục đích** | Cho phép người dùng xem, cập nhật thông tin cá nhân (Số điện thoại, địa chỉ, ngày sinh, giới tính, tiểu sử) và thay đổi/xóa ảnh đại diện. |
| **Mức độ ưu tiên** | Cao |
| **Điều kiện kích hoạt** | Người dùng truy cập tab "Tài khoản" và chọn "Thông tin cá nhân" hoặc chạm vào ảnh đại diện. |
| **Điều kiện tiên quyết** | Người dùng đã đăng nhập vào hệ thống. |
| **Điều kiện thành công** | Thông tin mới hoặc ảnh đại diện được cập nhật thành công lên Firestore/Storage và hiển thị trên giao diện. |
| **Điều kiện thất bại** | Dữ liệu không hợp lệ hoặc cập nhật thất bại. |
| **Luồng sự kiện chính** | **1.** Người dùng mở màn hình "Thông tin cá nhân".<br>**2.** Hệ thống tải và hiển thị thông tin hiện tại.<br>**3.** Người dùng nhấn "Chỉnh sửa". Hệ thống cho phép chỉnh sửa thông tin (Họ tên và Email bị khóa).<br>**4.** Người dùng cập nhật thông tin và nhấn "Lưu".<br>**5.** Hệ thống kiểm tra dữ liệu hợp lệ.<br>**6.** Hệ thống cập nhật lên cơ sở dữ liệu.<br>**7.** Hệ thống thông báo thành công và thoát chế độ chỉnh sửa. *(Use case kết thúc)*. |
| **Luồng thay thế** | **(Rẽ nhánh từ bước 3 - Hủy chỉnh sửa)**<br>**3.1.** Người dùng nhấn "Hủy". Hệ thống khôi phục dữ liệu cũ và thoát chế độ chỉnh sửa.<br>**3.2.** Người dùng nhấn "Quay lại" khi đang chỉnh sửa. Hệ thống hiển thị Dialog xác nhận hủy.<br>**3.3.** Người dùng chọn "Thoát". Hệ thống đóng màn hình.<br><br>**(Quản lý ảnh đại diện)**<br>**1.** Người dùng chạm vào ảnh đại diện tại tab Tài khoản.<br>**2.** Người dùng chọn "Thay ảnh đại diện" hoặc "Xóa ảnh đại diện".<br>**3.** Nếu thay ảnh: Người dùng chọn ảnh từ thư viện. Hệ thống tải ảnh lên và hiển thị.<br>**4.** Nếu xóa ảnh: Người dùng xác nhận. Hệ thống xóa ảnh và hiển thị ảnh mặc định. |
| **Luồng ngoại lệ** | **(Rẽ nhánh từ bước 5 - Dữ liệu không hợp lệ)**<br>**5.1.** Số điện thoại trống hoặc không đúng chuẩn viễn thông Việt Nam.<br>**5.2.** Hệ thống báo lỗi ngay tại ô nhập liệu. *(Quay lại bước 4)*. |

## 20. Use case: Quản lý xác minh tài khoản

| Trường | Nội dung |
| :--- | :--- |
| **Tên Use case** | Quản lý xác minh tài khoản |
| **Tác nhân chính** | Quản trị viên |
| **Mục đích** | Cho phép Quản trị viên quản lý xác minh tài khoản người dùng |
| **Mức độ ưu tiên** | Cao |
| **Điều kiện kích hoạt** | Khi Quản trị viên đăng nhập vào hệ thống quản trị |
| **Điều kiện tiên quyết** | Quản trị viên đã đăng nhập vào tài khoản |
| **Điều kiện thành công** | Trạng thái hồ sơ xác minh được cập nhật thành công |
| **Điều kiện thất bại** | Không có |
| **Luồng sự kiện chính** | **1.** Sau khi Quản trị viên đăng nhập, hệ thống hiển thị danh sách hồ sơ xác minh.<br>**2.** Quản trị viên bấm vào hồ sơ muốn xem.<br>**3.** Hệ thống hiển thị chi tiết hồ sơ xác minh.<br>**4.** Quản trị viên bấm Duyệt hoặc Từ chối hồ sơ để cập nhật trạng thái.<br>**5.** Hệ thống cập nhật trạng thái hồ sơ.<br>**6.** Hệ thống lưu thông tin vào cơ sở dữ liệu. *(Use case kết thúc)*. |
| **Luồng ngoại lệ** | Không có. |

## 21. Use case: Quản lý tài khoản người dùng

| Trường | Nội dung |
| :--- | :--- |
| **Tên Use case** | Quản lý tài khoản người dùng |
| **Tác nhân chính** | Quản trị viên |
| **Mục đích** | Cho phép Quản trị viên quản lý tài khoản người dùng |
| **Mức độ ưu tiên** | Cao |
| **Điều kiện kích hoạt** | Khi Quản trị viên đăng nhập vào hệ thống quản trị |
| **Điều kiện tiên quyết** | Quản trị viên đã đăng nhập vào tài khoản |
| **Điều kiện thành công** | Trạng thái tài khoản người dùng được cập nhật thành công |
| **Điều kiện thất bại** | Không có |
| **Luồng sự kiện chính** | **1.** Sau khi Quản trị viên đăng nhập, hệ thống hiển thị danh sách tài khoản người dùng.<br>**2.** Quản trị viên bấm vào tài khoản muốn xem.<br>**3.** Hệ thống hiển thị thông tin chi tiết tài khoản.<br>**4.** Quản trị viên bấm Khóa, Mở khóa hoặc Xóa để cập nhật trạng thái tài khoản.<br>**5.** Hệ thống cập nhật trạng thái tài khoản.<br>**6.** Hệ thống lưu thông tin vào cơ sở dữ liệu. *(Use case kết thúc)*. |
| **Luồng ngoại lệ** | Không có. |

## 22. Use case: Quản lý kiểm duyệt bài đăng

| Trường | Nội dung |
| :--- | :--- |
| **Tên Use case** | Quản lý kiểm duyệt bài đăng |
| **Tác nhân chính** | Quản trị viên |
| **Mục đích** | Cho phép Quản trị viên quản lý kiểm duyệt bài đăng |
| **Mức độ ưu tiên** | Cao |
| **Điều kiện kích hoạt** | Khi Quản trị viên đăng nhập vào hệ thống quản trị |
| **Điều kiện tiên quyết** | Quản trị viên đã đăng nhập vào tài khoản |
| **Điều kiện thành công** | Trạng thái bài đăng được cập nhật thành công |
| **Điều kiện thất bại** | Không có |
| **Luồng sự kiện chính** | **1.** Sau khi Quản trị viên đăng nhập, hệ thống hiển thị danh sách bài đăng.<br>**2.** Quản trị viên bấm vào bài đăng muốn xem.<br>**3.** Hệ thống hiển thị chi tiết bài đăng.<br>**4.** Quản trị viên bấm Duyệt, Từ chối hoặc Xóa để cập nhật trạng thái bài đăng.<br>**5.** Hệ thống cập nhật trạng thái bài đăng.<br>**6.** Hệ thống lưu thông tin vào cơ sở dữ liệu. *(Use case kết thúc)*. |
| **Luồng ngoại lệ** | Không có. |


## 23. Use case: Xem danh sách lịch hẹn (Chủ nhà)

| Trường | Nội dung |
| :--- | :--- |
| **Tên Use case** | Xem danh sách lịch hẹn (Chủ nhà) |
| **Tác nhân chính** | Chủ nhà đã xác minh |
| **Mục đích** | Theo dõi các lịch hẹn xem phòng do người thuê đặt. |
| **Mức độ ưu tiên** | Cao |
| **Điều kiện kích hoạt** | Chủ nhà bấm vào mục "Lịch hẹn của tôi" và chọn tab "Khách hẹn phòng tôi". |
| **Điều kiện tiên quyết** | Tài khoản đã đăng nhập và được xác minh là chủ nhà. |
| **Điều kiện thành công** | Danh sách lịch hẹn được hiển thị chính xác. |
| **Điều kiện thất bại** | Không có. |
| **Luồng sự kiện chính** | **1.** Chủ nhà bấm chọn "Lịch hẹn của tôi".<br>**2.** Hệ thống tải dữ liệu và hiển thị danh sách lịch hẹn tại tab "Khách hẹn phòng tôi". *(Use case kết thúc)*. |
| **Luồng ngoại lệ** | **(Rẽ nhánh từ bước 2 - Phát hiện trùng lịch)**<br>**2.1.** Hệ thống phát hiện có từ 2 lịch hẹn trở lên trùng cùng khung giờ.<br>**2.2.** Hệ thống hiển thị cảnh báo "Trùng lịch" trực tiếp trên thẻ lịch hẹn. *(Tiếp tục bước 2)*. |

## 24. Use case: Duyệt yêu cầu đặt lịch (Chủ nhà)

| Trường | Nội dung |
| :--- | :--- |
| **Tên Use case** | Duyệt yêu cầu đặt lịch (Chủ nhà) |
| **Tác nhân chính** | Chủ nhà đã xác minh |
| **Mục đích** | Chấp nhận yêu cầu xem phòng mới do người thuê đặt. |
| **Mức độ ưu tiên** | Cao |
| **Điều kiện kích hoạt** | Chủ nhà bấm nút "Xác nhận" tại lịch hẹn chờ duyệt. |
| **Điều kiện tiên quyết** | Lịch hẹn đang ở trạng thái chờ chủ nhà xác nhận. |
| **Điều kiện thành công** | Trạng thái lịch hẹn được cập nhật và gửi thông báo cho người thuê. |
| **Điều kiện thất bại** | Không có. |
| **Luồng sự kiện chính** | **1.** Chủ nhà bấm nút "Xác nhận" tại một lịch hẹn chờ duyệt.<br>**2.** Hệ thống hiển thị hộp thoại xác nhận.<br>**3.** Chủ nhà bấm "Đồng ý".<br>**4.** Hệ thống cập nhật trạng thái đã xác nhận, gửi thông báo cho người thuê và làm mới danh sách. *(Use case kết thúc)*. |
| **Luồng ngoại lệ** | Không có. |

## 25. Use case: Từ chối yêu cầu đặt lịch (Chủ nhà)

| Trường | Nội dung |
| :--- | :--- |
| **Tên Use case** | Từ chối yêu cầu đặt lịch (Chủ nhà) |
| **Tác nhân chính** | Chủ nhà đã xác minh |
| **Mục đích** | Từ chối yêu cầu xem phòng mới do người thuê đặt. |
| **Mức độ ưu tiên** | Cao |
| **Điều kiện kích hoạt** | Chủ nhà bấm nút "Từ chối" tại lịch hẹn chờ duyệt. |
| **Điều kiện tiên quyết** | Lịch hẹn đang ở trạng thái chờ chủ nhà xác nhận. |
| **Điều kiện thành công** | Trạng thái lịch hẹn được cập nhật và gửi thông báo cho người thuê. |
| **Điều kiện thất bại** | Bỏ trống lý do bắt buộc khi từ chối. |
| **Luồng sự kiện chính** | **1.** Chủ nhà bấm nút "Từ chối" tại một lịch hẹn chờ duyệt.<br>**2.** Hệ thống hiển thị hộp thoại từ chối kèm ô nhập lý do.<br>**3.** Chủ nhà nhập lý do và bấm "Từ chối".<br>**4.** Hệ thống cập nhật trạng thái từ chối, gửi thông báo cho người thuê và làm mới danh sách. *(Use case kết thúc)*. |
| **Luồng ngoại lệ** | **(Rẽ nhánh từ bước 3 - Bỏ trống lý do từ chối)**<br>**3.1.** Chủ nhà bấm "Từ chối" nhưng bỏ trống ô lý do.<br>**3.2.** Hệ thống hiển thị thông báo lỗi yêu cầu nhập lý do bắt buộc. *(Quay lại bước 2)*. |

## 26. Use case: Hủy lịch hẹn đã duyệt (Chủ nhà)

| Trường | Nội dung |
| :--- | :--- |
| **Tên Use case** | Hủy lịch hẹn đã duyệt (Chủ nhà) |
| **Tác nhân chính** | Chủ nhà đã xác minh |
| **Mục đích** | Hủy một lịch hẹn đã xác nhận trước đó do bận đột xuất. |
| **Mức độ ưu tiên** | Trung bình |
| **Điều kiện kích hoạt** | Chủ nhà bấm nút "Hủy lịch". |
| **Điều kiện tiên quyết** | Lịch hẹn đã được xác nhận. |
| **Điều kiện thành công** | Lịch hẹn bị hủy, khung giờ được mở lại. |
| **Điều kiện thất bại** | Bỏ trống lý do hủy. |
| **Luồng sự kiện chính** | **1.** Chủ nhà bấm nút "Hủy lịch" tại một lịch hẹn đã xác nhận.<br>**2.** Hệ thống hiển thị hộp thoại hủy lịch kèm ô nhập lý do.<br>**3.** Chủ nhà nhập lý do và bấm "Hủy lịch".<br>**4.** Hệ thống hủy lịch, mở lại khung giờ trống cho phòng và gửi thông báo cho người thuê. *(Use case kết thúc)*. |
| **Luồng ngoại lệ** | **(Rẽ nhánh từ bước 3 - Bỏ trống lý do hủy)**<br>**3.1.** Chủ nhà bấm "Hủy lịch" nhưng bỏ trống ô lý do.<br>**3.2.** Hệ thống hiển thị thông báo lỗi yêu cầu nhập lý do bắt buộc. *(Quay lại bước 2)*. |

## 27. Use case: Cập nhật kết quả xem phòng (Chủ nhà)

| Trường | Nội dung |
| :--- | :--- |
| **Tên Use case** | Cập nhật kết quả xem phòng (Chủ nhà) |
| **Tác nhân chính** | Chủ nhà đã xác minh |
| **Mục đích** | Đánh giá trạng thái thực tế sau khi kết thúc khung giờ hẹn. |
| **Mức độ ưu tiên** | Cao |
| **Điều kiện kích hoạt** | Chủ nhà bấm các nút cập nhật kết quả. |
| **Điều kiện tiên quyết** | Lịch hẹn đã quá hạn thời gian hẹn. |
| **Điều kiện thành công** | Ghi nhận chính xác kết quả buổi hẹn và chốt trạng thái phòng. |
| **Điều kiện thất bại** | Không có. |
| **Luồng sự kiện chính** | **1.** Chủ nhà bấm "Khách đã đến xem" tại lịch hẹn quá hạn.<br>**2.** Hệ thống hiển thị hộp thoại xác nhận.<br>**3.** Chủ nhà bấm "Đồng ý".<br>**4.** Hệ thống ghi nhận kết quả và cập nhật giao diện để chủ nhà tiếp tục chọn trạng thái thuê phòng. *(Use case kết thúc)*. |
| **Luồng ngoại lệ** | **(Rẽ nhánh từ bước 1 - Khách không đến)**<br>**1.1.** Chủ nhà bấm "Khách không đến".<br>**1.2.** Hệ thống hiển thị hộp thoại xác nhận.<br>**1.3.** Chủ nhà bấm "Đồng ý".<br>**1.4.** Hệ thống ghi nhận vi phạm cho người thuê, mở lại khung giờ trống. *(Use case kết thúc)*.<br><br>**(Rẽ nhánh từ bước 1 - Khách xác nhận thuê phòng)**<br>**1.1.** Chủ nhà bấm "Xác nhận cho thuê" đối với khách đã đến xem.<br>**1.2.** Hệ thống hiển thị hộp thoại xác nhận.<br>**1.3.** Chủ nhà bấm "Đồng ý".<br>**1.4.** Hệ thống cập nhật kết quả cho thuê, tự động ẩn bài đăng phòng và hủy các lịch hẹn còn lại của phòng đó. *(Use case kết thúc)*.<br><br>**(Rẽ nhánh từ bước 1 - Khách không thuê)**<br>**1.1.** Chủ nhà bấm "Không thuê" đối với khách đã đến xem.<br>**1.2.** Hệ thống hiển thị hộp thoại xác nhận.<br>**1.3.** Chủ nhà bấm "Đồng ý".<br>**1.4.** Hệ thống ghi nhận kết quả, duy trì bài đăng để tìm người khác. *(Use case kết thúc)*. |

## 28. Use case: Mở lại phòng (Chủ nhà)

| Trường | Nội dung |
| :--- | :--- |
| **Tên Use case** | Mở lại phòng (Chủ nhà) |
| **Tác nhân chính** | Chủ nhà đã xác minh |
| **Mục đích** | Đăng lại phòng lên ứng dụng sau khi khách đã thuê nhưng hủy ngang. |
| **Mức độ ưu tiên** | Trung bình |
| **Điều kiện kích hoạt** | Chủ nhà bấm nút "Mở lại phòng". |
| **Điều kiện tiên quyết** | Lịch hẹn đang ở trạng thái đã cho thuê. |
| **Điều kiện thành công** | Phòng hiển thị trở lại trong kết quả tìm kiếm. |
| **Điều kiện thất bại** | Không có. |
| **Luồng sự kiện chính** | **1.** Chủ nhà bấm "Mở lại phòng" tại lịch hẹn đã cho thuê.<br>**2.** Hệ thống hiển thị hộp thoại xác nhận.<br>**3.** Chủ nhà bấm "Đồng ý".<br>**4.** Hệ thống kích hoạt lại bài đăng, phòng xuất hiện trên danh sách tìm kiếm. *(Use case kết thúc)*. |
| **Luồng ngoại lệ** | Không có. |

## 29. Use case: Xem danh sách lịch hẹn (Người thuê)

| Trường | Nội dung |
| :--- | :--- |
| **Tên Use case** | Xem danh sách lịch hẹn (Người thuê) |
| **Tác nhân chính** | Người thuê |
| **Mục đích** | Giúp người thuê theo dõi trạng thái các lịch hẹn đã đặt. |
| **Mức độ ưu tiên** | Cao |
| **Điều kiện kích hoạt** | Người thuê bấm vào mục "Lịch hẹn của tôi". |
| **Điều kiện tiên quyết** | Tài khoản đã đăng nhập. |
| **Điều kiện thành công** | Danh sách lịch hẹn được hiển thị chính xác. |
| **Điều kiện thất bại** | Không có. |
| **Luồng sự kiện chính** | **1.** Người thuê bấm chọn mục "Lịch hẹn của tôi".<br>**2.** Hệ thống tải dữ liệu và hiển thị danh sách các lịch hẹn. *(Use case kết thúc)*. |
| **Luồng ngoại lệ** | **(Rẽ nhánh từ bước 2 - Phòng đã được cho thuê)**<br>**2.1.** Hệ thống phát hiện có lịch hẹn thuộc về phòng đã được cho thuê.<br>**2.2.** Hệ thống tự hủy các lịch hẹn này và hiển thị hộp thoại thông báo.<br>**2.3.** Người thuê bấm "Đã hiểu". *(Tiếp tục bước 2)*. |

## 30. Use case: Xác nhận lịch hẹn (Người thuê)

| Trường | Nội dung |
| :--- | :--- |
| **Tên Use case** | Xác nhận lịch hẹn (Người thuê) |
| **Tác nhân chính** | Người thuê |
| **Mục đích** | Xác nhận người thuê sẽ đến đúng lịch đã hẹn. |
| **Mức độ ưu tiên** | Trung bình |
| **Điều kiện kích hoạt** | Người thuê bấm nút "Xác nhận sẽ đến" tại một lịch hẹn. |
| **Điều kiện tiên quyết** | Lịch hẹn ở trạng thái chờ người thuê xác nhận. |
| **Điều kiện thành công** | Trạng thái lịch hẹn được cập nhật thành công. |
| **Điều kiện thất bại** | Không có. |
| **Luồng sự kiện chính** | **1.** Người thuê bấm "Xác nhận sẽ đến" tại một lịch hẹn.<br>**2.** Hệ thống hiển thị hộp thoại xác nhận.<br>**3.** Người thuê bấm "Đồng ý".<br>**4.** Hệ thống cập nhật trạng thái lịch hẹn, gửi thông báo cho chủ nhà và làm mới danh sách. *(Use case kết thúc)*. |
| **Luồng ngoại lệ** | Không có. |

## 31. Use case: Hủy lịch hẹn (Người thuê)

| Trường | Nội dung |
| :--- | :--- |
| **Tên Use case** | Hủy lịch hẹn (Người thuê) |
| **Tác nhân chính** | Người thuê |
| **Mục đích** | Cho phép người thuê hủy lịch hẹn khi không thể đến xem phòng. |
| **Mức độ ưu tiên** | Cao |
| **Điều kiện kích hoạt** | Người thuê bấm nút "Hủy lịch" tại một lịch hẹn. |
| **Điều kiện tiên quyết** | Lịch hẹn chưa bị hủy hoặc chưa hoàn thành. |
| **Điều kiện thành công** | Lịch hẹn bị hủy và khung giờ được mở lại. |
| **Điều kiện thất bại** | Không có. |
| **Luồng sự kiện chính** | **1.** Người thuê bấm "Hủy lịch" tại một lịch hẹn.<br>**2.** Hệ thống hiển thị hộp thoại xác nhận kèm ô nhập lý do (tùy chọn).<br>**3.** Người thuê nhập lý do và bấm "Đồng ý".<br>**4.** Hệ thống hủy lịch, mở lại khung giờ trống cho phòng và gửi thông báo đến chủ nhà. *(Use case kết thúc)*. |
| **Luồng ngoại lệ** | Không có. |

## 32. Use case: Đổi lịch hẹn (Người thuê)

| Trường | Nội dung |
| :--- | :--- |
| **Tên Use case** | Đổi lịch hẹn (Người thuê) |
| **Tác nhân chính** | Người thuê |
| **Mục đích** | Giúp người thuê chọn một khoảng thời gian khác để xem phòng. |
| **Mức độ ưu tiên** | Trung bình |
| **Điều kiện kích hoạt** | Người thuê bấm nút "Đổi lịch" tại một lịch hẹn. |
| **Điều kiện tiên quyết** | Lịch hẹn đang ở trạng thái chờ xác nhận. Số lần đổi < 3. |
| **Điều kiện thành công** | Lịch hẹn được cập nhật ngày giờ mới và quay về trạng thái chờ. |
| **Điều kiện thất bại** | Bỏ trống thông tin ngày giờ. |
| **Luồng sự kiện chính** | **1.** Người thuê bấm "Đổi lịch" tại một lịch hẹn.<br>**2.** Hệ thống hiển thị màn hình chọn ngày và giờ hẹn mới.<br>**3.** Người thuê chọn thời gian mới và bấm "Xác nhận".<br>**4.** Hệ thống lưu thời gian mới, đặt lại trạng thái chờ chủ nhà xác nhận và gửi thông báo. *(Use case kết thúc)*. |
| **Luồng ngoại lệ** | **(Rẽ nhánh từ bước 3 - Thiếu thông tin ngày giờ)**<br>**3.1.** Người thuê bấm "Xác nhận" nhưng chưa chọn đủ ngày hoặc giờ.<br>**3.2.** Hệ thống hiển thị thông báo lỗi yêu cầu chọn đầy đủ. *(Quay lại bước 2)*. |

## 33. Use case: Báo cáo chủ nhà không đến (Người thuê)

| Trường | Nội dung |
| :--- | :--- |
| **Tên Use case** | Báo cáo chủ nhà không đến (Người thuê) |
| **Tác nhân chính** | Người thuê |
| **Mục đích** | Ghi nhận vi phạm khi chủ nhà không xuất hiện tại buổi hẹn. |
| **Mức độ ưu tiên** | Thấp |
| **Điều kiện kích hoạt** | Người thuê bấm nút "Chủ nhà không đến". |
| **Điều kiện tiên quyết** | Lịch hẹn đã quá giờ hiển thị kết quả. |
| **Điều kiện thành công** | Hệ thống ghi nhận vi phạm cho chủ nhà. |
| **Điều kiện thất bại** | Không có. |
| **Luồng sự kiện chính** | **1.** Người thuê bấm "Chủ nhà không đến" tại lịch hẹn.<br>**2.** Hệ thống hiển thị hộp thoại xác nhận báo cáo.<br>**3.** Người thuê bấm "Đồng ý".<br>**4.** Hệ thống ghi nhận vi phạm cho chủ nhà, cập nhật trạng thái lịch hẹn và gửi thông báo. *(Use case kết thúc)*. |
| **Luồng ngoại lệ** | Không có. |

## 34. Use case: Thống kê hệ thống

| Trường | Nội dung |
| :--- | :--- |
| **Tên Use case** | Thống kê hệ thống |
| **Tác nhân chính** | Quản trị viên |
| **Mục đích** | Cho phép Quản trị viên thống kê hoạt động của hệ thống |
| **Mức độ ưu tiên** | Cao |
| **Điều kiện kích hoạt** | Khi Quản trị viên đăng nhập vào hệ thống quản trị |
| **Điều kiện tiên quyết** | Quản trị viên đã đăng nhập vào tài khoản |
| **Điều kiện thành công** | Bảng thống kê hệ thống được hiển thị thành công |
| **Điều kiện thất bại** | Không có |
| **Luồng sự kiện chính** | **1.** Sau khi Quản trị viên đăng nhập, hệ thống hiển thị trang Tổng quan (Bảng điều khiển).<br>**2.** Hệ thống tổng hợp các số liệu (Tổng người dùng, bài đăng, chờ duyệt).<br>**3.** Hệ thống hiển thị các thẻ số liệu và danh sách mới nhất.<br>**4.** Quản trị viên bấm "Xem tất cả" tại mục thống kê tương ứng.<br>**5.** Hệ thống điều hướng đến trang quản lý chi tiết.<br>**6.** Hệ thống hiển thị danh sách dữ liệu đầy đủ. *(Use case kết thúc)*. |
| **Luồng ngoại lệ** | Không có. |
