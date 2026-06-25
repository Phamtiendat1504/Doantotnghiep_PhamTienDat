import re

with open("BAO_CAO_DO_AN.md", "r", encoding="utf-8") as f:
    content = f.read()

# 1. TOC
toc_old = """| Bảng 3.1 | Mô tả các thành phần trong kiến trúc hệ thống | |
| Bảng 3.2 | Các collection trong Cloud Firestore | |
| Bảng 3.3 | Quy tắc bảo mật Firestore theo từng collection | |
| Bảng 3.4 | Quy tắc bảo mật Firebase Storage theo đường dẫn | |
| Bảng 3.5 | Mô tả các màn hình chính của ứng dụng | |
| Bảng 3.6 | Cấu trúc mã nguồn ứng dụng Android theo package | |
| Bảng 3.7 | Cấu trúc mã nguồn bảng điều khiển Web Admin và Cloud Functions | |
| Bảng 3.8 | Kịch bản kiểm thử các chức năng chính của hệ thống | |"""

toc_new = """| Bảng 3.1 | Các collection trong Cloud Firestore | |
| Bảng 3.2 | Cấu trúc thư mục lưu trữ trên Firebase Storage | |
| Bảng 3.3 | Quy tắc bảo mật Firestore theo từng collection | |
| Bảng 3.4 | Quy tắc bảo mật Firebase Storage theo đường dẫn | |
| Bảng 3.5 | Cấu trúc mã nguồn ứng dụng Android theo package | |
| Bảng 3.6 | Các màn hình chính của ứng dụng | |
| Bảng 3.7 | Tệp mã nguồn bảng điều khiển Web Admin | |"""

content = content.replace(toc_old, toc_new)

# 2. Section 3.1
sec31_old = """## 3.1. Tổng quan kiến trúc hệ thống

### 3.1.1. Mô hình Client – Firebase
Hệ thống hoạt động theo mô hình Backend-as-a-Service (BaaS) với Firebase làm trung tâm:
*   Ứng dụng Android tương tác trực tiếp với người dùng.
*   Firebase cung cấp toàn bộ cơ sở dữ liệu, xác thực, lưu trữ và thông báo đẩy.
*   Cloud Functions xử lý tự động các nghiệp vụ máy chủ.
*   Web Admin được viết bằng JavaScript thuần, lưu trữ trên Firebase Hosting.

### 3.1.2. Kiến trúc tổng thể hệ thống
Thành phần công nghệ cốt lõi bao gồm:
*   Ứng dụng di động viết bằng Kotlin theo kiến trúc MVVM.
*   Đăng nhập tài khoản Google qua Firebase Authentication.
*   Cơ sở dữ liệu NoSQL Cloud Firestore lưu trữ và đồng bộ dữ liệu thời gian thực.
*   Firebase Storage lưu trữ hình ảnh và thẻ căn cước.
*   Cloud Functions v2 chạy mã nguồn máy chủ tại cụm Đông Nam Á.
*   Tích hợp Google Maps (bản đồ), Google Cloud Vision (nhận diện căn cước) và SePay (thanh toán).
*   Bảo mật và phân quyền truy cập thông qua Firebase Security Rules.

### 3.1.3. Giao tiếp giữa ứng dụng và Firebase
Ứng dụng kết nối Backend qua các SDK chính thức của Firebase:
*   Đọc ghi dữ liệu, tải tệp trực tiếp qua Firestore SDK và Storage SDK.
*   Gọi hàm máy chủ qua HTTPS Callable, tự động truyền token xác thực.
*   Nhận thông báo đẩy qua Firebase Cloud Messaging kể cả khi ứng dụng chạy ngầm.

### 3.1.4. Đồng bộ dữ liệu thời gian thực
Hệ thống dùng cơ chế lắng nghe sự kiện của Firestore để tối ưu trải nghiệm:
*   Giao diện tự động làm mới tin nhắn và lịch hẹn mà không cần tải lại trang.
*   Tự động ghi nhận trạng thái online/offline của người dùng vào cơ sở dữ liệu.
*   Chỉ tải về dữ liệu vừa thay đổi để tiết kiệm tối đa băng thông."""

sec31_new = """## 3.1. Tổng quan kiến trúc hệ thống
Kiến trúc hệ thống được xây dựng theo mô hình Backend-as-a-Service (BaaS) lấy Firebase làm trung tâm, với các đặc điểm cốt lõi:
*   **Ứng dụng người dùng:** Được phát triển nguyên bản bằng Kotlin cho nền tảng Android, áp dụng kiến trúc phân tầng MVVM (Model-View-ViewModel) kết hợp Repository Pattern để đảm bảo hiệu suất và khả năng mở rộng.
*   **Hạ tầng máy chủ (Backend):** Sử dụng toàn diện hệ sinh thái Firebase bao gồm:
    *   **Authentication:** Quản lý danh tính và phiên đăng nhập.
    *   **Cloud Firestore:** Cơ sở dữ liệu NoSQL đồng bộ thời gian thực.
    *   **Cloud Storage:** Lưu trữ tệp phương tiện (hình ảnh, tài liệu).
    *   **Cloud Messaging:** Dịch vụ gửi thông báo đẩy đến thiết bị.
*   **Xử lý logic nghiệp vụ:** Triển khai thông qua Firebase Cloud Functions v2 (Node.js) chạy trên cụm máy chủ tại Đông Nam Á, đảm bảo tính nguyên tử của dữ liệu (như giao dịch đặt lịch hẹn) và xử lý các tác vụ nặng (nhận dạng CCCD qua Google Cloud Vision, tích hợp thanh toán SePay).
*   **Bảng điều khiển quản trị (Web Admin):** Phát triển dưới dạng Single Page Application (SPA) bằng JavaScript thuần, được lưu trữ và phân phối qua mạng phân phối nội dung của Firebase Hosting.
*   **Bảo mật:** Toàn bộ truy xuất dữ liệu từ client đều được kiểm soát chặt chẽ bởi hệ thống Firebase Security Rules độc lập với mã nguồn ứng dụng."""

content = content.replace(sec31_old, sec31_new)

# 3. Section 3.2.1
sec321_old = """### 3.2.1. Firebase Authentication

Hệ thống sử dụng Firebase Authentication với phương thức đăng nhập bằng email và mật khẩu, trong đó địa chỉ email bắt buộc phải là tài khoản Gmail để đảm bảo tính xác thực. Mật khẩu yêu cầu tối thiểu 12 ký tự để tăng cường bảo mật. Trước khi tạo tài khoản, ứng dụng truy vấn collection phone_registry để kiểm tra số điện thoại chưa được đăng ký trước đó, ngăn chặn việc đăng ký nhiều tài khoản bằng cùng một số điện thoại. Sau khi đăng nhập thành công, FCM token của thiết bị được cập nhật vào document của người dùng trong Firestore để đảm bảo thông báo đẩy luôn được gửi đúng thiết bị. Hệ thống phân quyền ba vai trò: người thuê (user), người cho thuê (landlord) và quản trị viên (admin). Cơ chế khóa tài khoản có thời hạn cho phép admin tạm khóa tài khoản vi phạm; khi người dùng bị khóa cố đăng nhập, ứng dụng hiển thị lý do và thời điểm được mở khóa thay vì thông báo lỗi chung chung."""

sec321_new = """### 3.2.1. Firebase Authentication
Hệ thống sử dụng dịch vụ Firebase Authentication để quản lý tài khoản người dùng với các điểm cốt lõi:
*   Bắt buộc đăng nhập bằng tài khoản Gmail và mật khẩu từ 12 ký tự trở lên.
*   Tự động kiểm tra trùng lặp số điện thoại trước khi tạo tài khoản mới.
*   Lưu trữ mã định danh thiết bị ngay khi đăng nhập để gửi thông báo đẩy.
*   Phân chia người dùng thành ba vai trò gồm người thuê, chủ trọ và quản trị viên.
*   Cung cấp tính năng khóa tài khoản có thời hạn và hiển thị lý do phạt."""

content = content.replace(sec321_old, sec321_new)

# 4. Section 3.2.2
sec322_old = """### 3.2.2. Cloud Firestore – Thiết kế cơ sở dữ liệu

Toàn bộ dữ liệu nghiệp vụ của hệ thống được lưu trữ trong Cloud Firestore với 17 collection chính, mỗi collection phục vụ một nhóm đối tượng dữ liệu riêng biệt:

| Collection | Dữ liệu lưu trữ |
|---|---|
| users | Thông tin tài khoản, vai trò (role), trạng thái xác minh (isVerified), FCM token, số slot đăng tin (purchasedSlots), trạng thái khóa và thời hạn khóa |
| rooms | Thông tin phòng trọ, tọa độ GPS (latitude/longitude), danh sách tiện ích (boolean flags), trạng thái duyệt (pending/approved/rejected), cờ nổi bật (isFeatured) |
| chats | Kênh chat giữa hai người dùng; subcollection messages lưu tin nhắn văn bản, hình ảnh và emoji reaction |
| appointments | Lịch hẹn xem phòng, trạng thái (pending/confirmed/rejected/cancelled/tenant_confirmed/completed) |
| bookedSlots | Khóa khung giờ với composite key `{roomId}_{date}_{time}` để chống đặt lịch trùng |
| verifications | Kết quả OCR CCCD (họ tên, số CCCD, ngày sinh, địa chỉ), trạng thái phê duyệt |
| notifications | Thông báo cá nhân hóa theo từng người dùng |
| support_tickets | Phiếu hỗ trợ kỹ thuật, trạng thái, nội dung phản hồi từ admin |
| reviews | Đánh giá phòng trọ (1–5 sao), nhận xét văn bản, trạng thái duyệt |
| savedPosts | Danh sách tin đăng đã lưu của mỗi người dùng |
| cccd_registry | Đăng ký số CCCD đã xác minh, ngăn đăng ký nhiều tài khoản bằng một CCCD |
| phone_registry | Đăng ký số điện thoại, ngăn tạo nhiều tài khoản bằng một số điện thoại |
| slot_upgrade_requests | Yêu cầu mua thêm slot đăng tin, thông tin giao dịch SePay |
| featured_upgrade_requests | Yêu cầu nâng cấp tin đăng nổi bật, thông tin giao dịch SePay |
| system_notifications | Thông báo hệ thống do admin gửi đại trà đến tất cả người dùng |
| stats/popular_areas | Thống kê khu vực phổ biến phục vụ giao diện trang chủ |
| stats/dashboard_stats | Dữ liệu thống kê tổng quan đã tính sẵn (TTL 5 phút), phục vụ Dashboard Web Admin |"""

sec322_new = """### 3.2.2. Cloud Firestore – Thiết kế cơ sở dữ liệu
Dữ liệu hệ thống được tổ chức thành 16 nhóm chính trên Cloud Firestore để tối ưu tốc độ truy vấn:

**Bảng 3.1: Các collection trong Cloud Firestore**

| Tên nhóm dữ liệu | Mục đích lưu trữ chính |
|---|---|
| users | Thông tin tài khoản, vai trò, trạng thái xác minh và hạn mức đăng tin. |
| rooms | Thông tin chi tiết phòng trọ, tọa độ bản đồ và trạng thái kiểm duyệt. |
| chats | Lịch sử nhắn tin văn bản và hình ảnh giữa các người dùng. |
| appointments | Thông tin lịch hẹn xem phòng và tiến trình xác nhận. |
| bookedSlots | Quản lý các khung giờ đã được đặt để ngăn chặn việc trùng lịch. |
| verifications | Hồ sơ căn cước công dân và kết quả nhận diện tự động. |
| notifications | Hệ thống thông báo cá nhân hóa cho từng thiết bị. |
| support_tickets | Các phiếu yêu cầu hỗ trợ kỹ thuật và phản hồi từ quản trị viên. |
| savedPosts | Danh sách các phòng trọ được người dùng đánh dấu yêu thích. |
| cccd_registry | Danh sách căn cước độc nhất để ngăn chặn đăng ký nhiều tài khoản. |
| phone_registry | Danh sách số điện thoại độc nhất để tránh tạo tài khoản trùng lặp. |
| slot_upgrade_requests | Các giao dịch thanh toán mua thêm lượt đăng bài mới. |
| featured_upgrade_requests | Các giao dịch thanh toán để đẩy bài đăng lên vị trí nổi bật. |
| system_notifications | Thông báo đại trà từ quản trị viên đến toàn bộ hệ thống. |
| stats/popular_areas | Thống kê các khu vực tìm kiếm phổ biến để hiển thị ở trang chủ. |
| stats/dashboard_stats | Dữ liệu thống kê tính sẵn để tối ưu tốc độ tải bảng điều khiển."""

content = content.replace(sec322_old, sec322_new)

# 5. Section 3.2.3
sec323_old = """### 3.2.3. Firebase Storage

Tệp ảnh trong hệ thống được tổ chức theo cấu trúc đường dẫn logic, mỗi nhóm tệp phục vụ một mục đích cụ thể:

| Đường dẫn | Mô tả | Giới hạn |
|---|---|---|
| `avatars/{uid}` | Ảnh đại diện người dùng | Ảnh, tối đa 5 MB |
| `rooms/{roomId}` | Ảnh phòng trọ | Ảnh, tối đa 15 MB |
| `verifications/{uid}` | Ảnh mặt trước/sau CCCD | Ảnh, tối đa 10 MB |
| `chat_images/{chatId}` | Ảnh đính kèm trong tin nhắn | Ảnh, tối đa 15 MB |
| `support_images/{ticketId}` | Ảnh đính kèm phiếu hỗ trợ | Ảnh, tối đa 10 MB |

Đường dẫn tải xuống (download URL) của ảnh sau khi upload được lưu vào document Firestore tương ứng để ứng dụng có thể tải và hiển thị mà không cần truy vấn Storage lại. Ảnh phòng trọ chỉ được tải lên sau khi document phòng đã được tạo, tránh phát sinh ảnh mồ côi không có document liên kết."""

sec323_new = """### 3.2.3. Firebase Storage
Hệ thống sử dụng Firebase Storage để lưu trữ các tệp phương tiện, được phân loại theo cấu trúc thư mục rõ ràng:

**Bảng 3.2: Cấu trúc thư mục lưu trữ trên Firebase Storage**

| Thư mục lưu trữ | Mục đích sử dụng | Giới hạn dung lượng |
|---|---|---|
| avatars/ | Ảnh đại diện của người dùng. | Tối đa 5 MB |
| rooms/ | Hình ảnh chi tiết của phòng trọ. | Tối đa 15 MB |
| verifications/ | Ảnh mặt trước và mặt sau thẻ căn cước. | Tối đa 10 MB |
| chat_images/ | Hình ảnh đính kèm trong các cuộc trò chuyện. | Tối đa 15 MB |
| support_images/ | Hình ảnh đính kèm trong phiếu yêu cầu hỗ trợ. | Tối đa 10 MB |

Đường dẫn tải xuống của tệp được lưu trực tiếp vào cơ sở dữ liệu ngay sau khi tải lên thành công. Hệ thống áp dụng cơ chế chỉ cho phép tải ảnh lên khi bản ghi dữ liệu gốc đã tồn tại nhằm hạn chế tối đa rác lưu trữ."""

content = content.replace(sec323_old, sec323_new)

# 6. Section 3.2.4
sec324_old = """### 3.2.4. Firebase Cloud Messaging (FCM)

Firebase Cloud Messaging được sử dụng để gửi thông báo đẩy đến thiết bị người dùng trong các tình huống nghiệp vụ quan trọng. Hệ thống gửi thông báo đến người cho thuê khi có người dùng tạo lịch hẹn xem phòng mới, và ngược lại gửi thông báo đến người thuê khi người cho thuê xác nhận hoặc từ chối lịch hẹn. Khi quản trị viên duyệt hoặc từ chối bài đăng phòng trọ, thông báo được gửi ngay lập tức đến người cho thuê. Quy trình phê duyệt hồ sơ xác minh CCCD và phản hồi phiếu hỗ trợ kỹ thuật cũng kích hoạt thông báo tương ứng. Ngoài ra, admin có thể gửi thông báo hệ thống đại trà đến tất cả người dùng thông qua collection system_notifications. Toàn bộ việc gửi FCM được thực hiện phía máy chủ qua Cloud Functions để đảm bảo độ tin cậy và xử lý lỗi token hết hạn."""

sec324_new = """### 3.2.4. Firebase Cloud Messaging (FCM)
Hệ thống sử dụng Firebase Cloud Messaging để gửi thông báo đẩy đến thiết bị người dùng trong các tình huống nghiệp vụ:
*   Cập nhật trạng thái lịch hẹn xem phòng cho cả người thuê và chủ trọ.
*   Thông báo kết quả phê duyệt bài đăng phòng trọ và hồ sơ xác minh căn cước.
*   Thông báo phản hồi phiếu yêu cầu hỗ trợ kỹ thuật từ ban quản trị.
*   Gửi thông báo hệ thống đại trà từ quản trị viên đến toàn bộ người dùng.

Toàn bộ quá trình gửi thông báo được xử lý tập trung trên máy chủ để đảm bảo độ tin cậy và quản lý hiệu quả các thiết bị nhận tin."""

content = content.replace(sec324_old, sec324_new)

# 7. Section 3.2.5
sec325_old = """### 3.2.5. Firebase Security Rules

Firestore Security Rules và Storage Rules được cấu hình để thực thi phân quyền ở tầng cơ sở dữ liệu, là lớp bảo mật cuối cùng ngăn chặn truy cập trái phép ngay cả khi logic ứng dụng bị bỏ qua.

**Firestore Security Rules:**

| Collection | Quyền đọc | Quyền ghi | Ràng buộc đặc biệt |
|---|---|---|---|
| users | Đã đăng nhập | Chính chủ hoặc admin | Admin không thể tự hạ quyền |
| rooms | Công khai (approved) | Chủ sở hữu hoặc admin | Quota 3 tin/24 giờ kiểm tra ở Rules |
| appointments | Người liên quan hoặc admin | Người thuê tạo mới; chủ trọ cập nhật trạng thái | — |
| bookedSlots | Đã đăng nhập | Người thuê | Chỉ tạo mới, không sửa/xóa |
| reviews | Công khai | Người thuê (không phải chủ phòng) | Cấm tự đánh giá phòng của mình |
| verifications | Chính chủ hoặc admin | Chính chủ hoặc admin | — |
| support_tickets | Chủ phiếu hoặc admin | Chủ phiếu tạo; admin phản hồi | — |

**Firebase Storage Rules:**

| Đường dẫn | Quyền đọc | Quyền ghi |
|---|---|---|
| `avatars/{uid}` | Công khai | Chính chủ hoặc admin |
| `rooms/{roomId}` | Công khai | Chủ sở hữu phòng hoặc admin |
| `verifications/{uid}` | Chính chủ hoặc admin | Chính chủ hoặc admin |
| `chat_images/{chatId}` | Đã đăng nhập | Đã đăng nhập |
| `support_images/{ticketId}` | Chủ phiếu hoặc admin | Chủ phiếu hoặc admin |
| (mặc định) | Từ chối | Từ chối |"""

sec325_new = """### 3.2.5. Firebase Security Rules

Firestore Security Rules và Storage Rules được cấu hình để thực thi phân quyền ở tầng cơ sở dữ liệu, là lớp bảo mật cuối cùng ngăn chặn truy cập trái phép ngay cả khi logic ứng dụng bị bỏ qua.

**Bảng 3.3: Quy tắc bảo mật Firestore theo từng collection**

| Tên nhóm dữ liệu | Quyền đọc | Quyền ghi | Ràng buộc đặc biệt |
|---|---|---|---|
| users | Bắt buộc đăng nhập | Chính chủ hoặc quản trị viên | — |
| rooms | Công khai (phòng đã duyệt hoặc hết hạn) hoặc Đã đăng nhập (phòng đã cho thuê hoặc chính chủ) | Chủ sở hữu hoặc quản trị viên | Giới hạn tối đa 3 tin đăng mỗi ngày |
| appointments | Bắt buộc đăng nhập | Người thuê tạo lịch mới; chủ trọ cập nhật tiến trình | — |
| bookedSlots | Bắt buộc đăng nhập | Chủ trọ | Chỉ tạo mới sau khi xác nhận; người tham gia có quyền xóa khi hủy hoặc hoàn thành |
| verifications | Chính chủ hoặc quản trị viên | Chính chủ hoặc quản trị viên | — |
| support_tickets (và messages) | Chủ phiếu hoặc quản trị viên | Chủ phiếu gửi yêu cầu; quản trị viên phản hồi | — |

**Bảng 3.4: Quy tắc bảo mật Firebase Storage theo đường dẫn**

| Thư mục lưu trữ | Quyền đọc | Quyền ghi |
|---|---|---|
| avatars/ | Công khai | Chính chủ hoặc quản trị viên |
| rooms/ | Công khai | Chủ sở hữu hoặc quản trị viên |
| verifications/ | Chính chủ hoặc quản trị viên | Chính chủ hoặc quản trị viên |
| chat_images/ | Bắt buộc đăng nhập | Bắt buộc đăng nhập |
| support_images/ | Chủ phiếu hoặc quản trị viên | Chủ phiếu, quản trị viên hoặc khi tạo phiếu mới |
| Các thư mục khác | Bị từ chối | Bị từ chối |"""

content = content.replace(sec325_old, sec325_new)

# 8. Section 3.2.6 (serverSubmitBooking fix)
sec326_old = "ngược lại ghi đồng thời document lịch hẹn vào appointments và document khóa giờ vào bookedSlots trong cùng một transaction."
sec326_new = "ngược lại ghi document lịch hẹn vào appointments với trạng thái pending. Document bookedSlots được chủ trọ tạo sau khi xác nhận lịch hẹn để khóa khung giờ."
content = content.replace(sec326_old, sec326_new)

# 9. Section 3.3.1
sec331_old = """| Package | Các lớp/tệp chính | Vai trò |
|---|---|---|
| Model | User.kt, Room.kt, Message.kt, Conversation.kt, Appointment.kt, SupportTicket.kt, Review.kt | Data class Kotlin ánh xạ với document Firestore |"""

sec331_new = """**Bảng 3.5: Cấu trúc mã nguồn ứng dụng Android theo package**

| Package | Các lớp/tệp chính | Vai trò |
|---|---|---|
| Model | User.kt, Room.kt, Message.kt, Conversation.kt, Appointment.kt, SupportTicket.kt | Data class Kotlin ánh xạ với document Firestore |"""

content = content.replace(sec331_old, sec331_new)

# 10. Section 3.3.2
sec332_old = """| Màn hình | Vai trò | Chức năng chính |"""
sec332_new = """**Bảng 3.6: Các màn hình chính của ứng dụng**

| Màn hình | Vai trò | Chức năng chính |"""
content = content.replace(sec332_old, sec332_new)

# 11. Section 3.3.3 (serverSubmitBooking fix)
sec333_old = "ghi đồng thời cả document lịch hẹn lẫn document bookedSlots. Cơ chế transaction"
sec333_new = "ghi document lịch hẹn vào appointments với trạng thái pending. Document bookedSlots được chủ trọ tạo sau khi xác nhận lịch hẹn để khóa khung giờ. Cơ chế transaction"
content = content.replace(sec333_old, sec333_new)

# 12. Section 3.4.1
sec341_old = """| Tệp | Chức năng |"""
sec341_new = """**Bảng 3.7: Tệp mã nguồn bảng điều khiển Web Admin**

| Tệp | Chức năng |"""
content = content.replace(sec341_old, sec341_new)

with open("BAO_CAO_DO_AN.md", "w", encoding="utf-8") as f:
    f.write(content)

print("Restoration complete!")
