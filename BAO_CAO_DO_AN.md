# TRƯỜNG ĐẠI HỌC THỦY LỢI
## KHOA CÔNG NGHỆ THÔNG TIN

---

# PHÁT TRIỂN ỨNG DỤNG TÌM KIẾM VÀ ĐẶT LỊCH XEM PHÒNG TRỌ

**Sinh viên thực hiện:** Phạm Tiến Đạt  
**Mã sinh viên:** 2251061740  
**Lớp:** 64CNTT2  
**Ngành:** Công nghệ Thông tin  
**Giáo viên hướng dẫn:** TS. Nguyễn Văn Thẩm  

**HÀ NỘI, NĂM 2025**

---

## MỤC LỤC

- [DANH MỤC TỪ VIẾT TẮT](#danh-mục-từ-viết-tắt)
- [DANH MỤC HÌNH ẢNH](#danh-mục-hình-ảnh)
- [DANH MỤC BẢNG BIỂU](#danh-mục-bảng-biểu)
- [MỞ ĐẦU](#mở-đầu)
- [CHƯƠNG 1: CƠ SỞ LÝ THUYẾT VÀ CÔNG NGHỆ PHÁT TRIỂN ỨNG DỤNG DI ĐỘNG](#chương-1)
  - [1.1 Bài toán tìm kiếm phòng trọ và quản lý lịch hẹn](#11)
  - [1.2 Yêu cầu đặt ra đối với hệ thống](#12)
    - [1.2.1 Yêu cầu đối với khách truy cập (chưa đăng nhập)](#121)
    - [1.2.2 Yêu cầu đối với người thuê (đã đăng nhập)](#122)
    - [1.2.3 Yêu cầu đối với người cho thuê (đã xác minh CCCD)](#123)
    - [1.2.4 Yêu cầu đối với quản trị viên](#124)
    - [1.2.5 Yêu cầu đối với chức năng quản lý lịch hẹn](#125)
    - [1.2.6 Yêu cầu phi chức năng](#126)
  - [1.3 Công nghệ phát triển](#13)
    - [1.3.1 Công nghệ phát triển ứng dụng di động Android](#131)
    - [1.3.2 Nền tảng Firebase](#132)
    - [1.3.3 Công nghệ bản đồ và định vị](#133)
    - [1.3.4 Công nghệ nhận dạng ký tự quang học và thanh toán](#134)
    - [1.3.5 Công nghệ Trợ lý ảo AI tìm kiếm thông minh (AI Chatbot Engine)](#135)
    - [1.3.6 Công nghệ bảng điều khiển Web Admin](#136)
- [CHƯƠNG 2: MÔ HÌNH CHỨC NĂNG VÀ DỮ LIỆU](#chương-2)
  - [2.1 Mô hình chức năng](#21)
    - [2.1.1 Quy tắc nghiệp vụ hệ thống](#211)
    - [2.1.2 Xác định và mô tả tác nhân](#212)
    - [2.1.3 Xác định các chức năng chính](#213)
  - [2.2 Mô tả chi tiết các chức năng](#22)
  - [2.3 Mô hình dữ liệu](#23)
    - [2.3.1 Mô hình thực thể liên hệ](#231)
    - [2.3.2 Chuyển mô hình thực thể liên hệ thành mô hình dữ liệu Firebase](#232)
    - [2.3.3 Bảng dữ liệu](#233)
    - [2.3.4 Mô hình quan hệ tổng quan](#234)
- [CHƯƠNG 3: MÔ HÌNH PHẦN MỀM VÀ TRIỂN KHAI ỨNG DỤNG](#chương-3)
  - [3.1 Tổng quan kiến trúc hệ thống](#31)
    - [3.1.1 Mô hình Client – Firebase](#311)
    - [3.1.2 Kiến trúc tổng thể hệ thống](#312)
    - [3.1.3 Giao tiếp giữa ứng dụng di động và Firebase](#313)
    - [3.1.4 Đồng bộ dữ liệu thời gian thực](#314)
  - [3.2 Thiết kế và triển khai Firebase](#32)
    - [3.2.1 Thiết kế Firebase Authentication](#321)
    - [3.2.2 Thiết kế Cloud Firestore](#322)
    - [3.2.3 Thiết kế Firebase Storage](#323)
    - [3.2.4 Thiết kế Firebase Cloud Messaging](#324)
    - [3.2.5 Thiết kế Firebase Security Rules](#325)
  - [3.3 Thiết kế ứng dụng di động](#33)
    - [3.3.1 Cấu trúc ứng dụng di động](#331)
    - [3.3.2 Thiết kế giao diện người dùng](#332)
    - [3.3.3 Luồng hoạt động cơ bản](#333)
  - [3.4 Kết quả triển khai ứng dụng](#34)
    - [3.4.1 Chức năng đăng ký, đăng nhập](#341)
    - [3.4.2 Chức năng của người thuê](#342)
    - [3.4.3 Chức năng của người cho thuê](#343)
    - [3.4.4 Chức năng của quản trị viên](#344)
  - [3.5 Kiểm thử hệ thống](#35)
  - [3.6 Đánh giá hệ thống](#36)
    - [3.6.1 Ưu điểm](#361)
    - [3.6.2 Hạn chế](#362)
    - [3.6.3 Hướng phát triển](#363)
- [KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN](#kết-luận)
- [TÀI LIỆU THAM KHẢO](#tài-liệu-tham-khảo)

---

## DANH MỤC TỪ VIẾT TẮT

| Từ viết tắt | Giải nghĩa |
|---|---|
| API | Application Programming Interface (Giao diện lập trình ứng dụng) |
| BaaS | Backend as a Service (Dịch vụ backend đám mây) |
| CCCD | Căn cước công dân |
| CNTT | Công nghệ thông tin |
| FCM | Firebase Cloud Messaging (Dịch vụ thông báo đẩy của Firebase) |
| GCP | Google Cloud Platform (Nền tảng đám mây của Google) |
| GPS | Global Positioning System (Hệ thống định vị toàn cầu) |
| HTTP | Hypertext Transfer Protocol (Giao thức truyền tải siêu văn bản) |
| IDE | Integrated Development Environment (Môi trường phát triển tích hợp) |
| JSON | JavaScript Object Notation (Định dạng trao đổi dữ liệu) |
| MVVM | Model-View-ViewModel (Kiến trúc phát triển phần mềm) |
| NoSQL | Not Only SQL (Cơ sở dữ liệu phi quan hệ) |
| OCR | Optical Character Recognition (Nhận dạng ký tự quang học) |
| SDK | Software Development Kit (Bộ công cụ phát triển phần mềm) |
| UI | User Interface (Giao diện người dùng) |
| UID | User Identifier (Mã định danh người dùng) |
| URL | Uniform Resource Locator (Địa chỉ tài nguyên thống nhất) |
| UX | User Experience (Trải nghiệm người dùng) |
| VNĐ | Việt Nam Đồng (Đơn vị tiền tệ Việt Nam) |

---

## DANH MỤC HÌNH ẢNH

| Số hiệu | Tên hình | Trang |
|---|---|---|
| Hình 2.1 | Biểu đồ Use case tổng quát hệ thống | |
| Hình 2.2 | Biểu đồ Use case phân rã tác nhân Người dùng | |
| Hình 2.3 | Biểu đồ Use case phân rã tác nhân Người đăng tin (đã xác minh) | |
| Hình 2.4 | Biểu đồ Use case phân rã tác nhân Quản trị viên | |
| Hình 2.5 | Biểu đồ Use case Quản lý tài khoản | |
| Hình 2.6 | Biểu đồ hoạt động Đăng ký tài khoản | |
| Hình 2.7 | Biểu đồ tuần tự Đăng ký tài khoản | |
| Hình 2.8 | Biểu đồ Use case Tìm kiếm và xem phòng trọ | |
| Hình 2.9 | Biểu đồ hoạt động Tìm kiếm phòng trọ | |
| Hình 2.10 | Biểu đồ tuần tự Xem chi tiết phòng trọ | |
| Hình 2.11 | Biểu đồ Use case Nhắn tin | |
| Hình 2.12 | Biểu đồ hoạt động Nhắn tin | |
| Hình 2.13 | Biểu đồ tuần tự Gửi tin nhắn | |
| Hình 2.14 | Biểu đồ Use case Đặt lịch xem phòng | |
| Hình 2.15 | Biểu đồ hoạt động Đặt lịch hẹn xem phòng | |
| Hình 2.16 | Biểu đồ tuần tự Đặt lịch hẹn xem phòng | |
| Hình 2.17 | Biểu đồ Use case Đăng và quản lý tin phòng trọ | |
| Hình 2.18 | Biểu đồ hoạt động Đăng tin phòng trọ | |
| Hình 2.19 | Biểu đồ tuần tự Đăng tin phòng trọ | |
| Hình 2.20 | Biểu đồ Use case Xác minh danh tính CCCD | |
| Hình 2.21 | Biểu đồ hoạt động Xác minh danh tính CCCD | |
| Hình 2.22 | Biểu đồ tuần tự Xác minh danh tính CCCD | |
| Hình 2.23 | Sơ đồ mô hình dữ liệu tổng quan của ứng dụng | |
| Hình 3.1 | Sơ đồ kiến trúc hệ thống tổng thể | |
| Hình 3.2 | Sơ đồ tổ chức collection và subcollection trong Firestore | |
| Hình 3.3 | Sơ đồ phân quyền truy cập Firestore theo vai trò | |
| Hình 3.4 | Sơ đồ luồng xác minh danh tính và phân quyền Storage | |
| Hình 3.5 | Wireframe màn hình Trang chủ | |
| Hình 3.6 | Wireframe màn hình Tìm kiếm nâng cao | |
| Hình 3.7 | Wireframe màn hình Chi tiết phòng trọ | |
| Hình 3.8 | Wireframe màn hình Đặt lịch hẹn | |
| Hình 3.9 | Wireframe màn hình Nhắn tin | |
| Hình 3.10 | Wireframe màn hình Đăng nhập và Đăng ký | |
| Hình 3.11 | Wireframe màn hình Xác minh CCCD | |
| Hình 3.12 | Wireframe màn hình Quản lý lịch hẹn và tin đăng (người đăng tin) | |
| Hình 3.13 | Wireframe màn hình Đăng tin phòng trọ | |
| Hình 3.14 | Sơ đồ luồng đăng ký và đăng nhập | |
| Hình 3.15 | Sơ đồ luồng đăng tin phòng trọ | |
| Hình 3.16 | Sơ đồ luồng đặt lịch hẹn xem phòng | |
| Hình 3.17 | Sơ đồ luồng xác minh CCCD | |
| Hình 3.18 | Giao diện đăng nhập | |
| Hình 3.19 | Giao diện đăng ký tài khoản | |
| Hình 3.20 | Giao diện trang chủ ứng dụng | |
| Hình 3.21 | Giao diện khu vực phổ biến và tin nổi bật trên trang chủ | |
| Hình 3.22 | Giao diện tìm kiếm với bộ lọc | |
| Hình 3.23 | Giao diện danh sách kết quả tìm kiếm | |
| Hình 3.24 | Giao diện chi tiết phòng trọ (ảnh và thông tin cơ bản) | |
| Hình 3.25 | Giao diện chi tiết phòng trọ (tiện ích, quy định và thông tin chủ trọ) | |
| Hình 3.26 | Giao diện chọn ngày và giờ đặt lịch hẹn | |
| Hình 3.27 | Thông báo đặt lịch hẹn thành công | |
| Hình 3.28 | Giao diện danh sách lịch hẹn của người dùng | |
| Hình 3.29 | Giao diện danh sách hội thoại | |
| Hình 3.30 | Giao diện màn hình chat với người đăng tin | |
| Hình 3.31 | Giao diện hướng dẫn xác minh và nhập số CCCD | |
| Hình 3.32 | Giao diện camera chụp CCCD với khung căn chỉnh | |
| Hình 3.33 | Thông báo kết quả xác minh | |
| Hình 3.34 | Giao diện form đăng tin phòng (thông tin cơ bản) | |
| Hình 3.35 | Giao diện form đăng tin phòng (tiện ích và quy định) | |
| Hình 3.36 | Giao diện danh sách tin đã đăng với trạng thái | |
| Hình 3.37 | Giao diện quản lý lịch hẹn của người đăng tin | |
| Hình 3.38 | Giao diện dashboard tổng quan với thống kê và biểu đồ | |
| Hình 3.39 | Giao diện duyệt tin đăng phòng trọ | |
| Hình 3.40 | Giao diện xét duyệt hồ sơ xác minh CCCD | |
| Hình 3.41 | Giao diện quản lý người dùng với chức năng khóa tài khoản | |

---

## DANH MỤC BẢNG BIỂU

| Số hiệu | Tên bảng | Trang |
|---|---|---|
| Bảng 2.1 | Đặc tả Use case Đăng ký tài khoản | |
| Bảng 2.2 | Đặc tả Use case Đăng nhập | |
| Bảng 2.3 | Đặc tả Use case Quên mật khẩu | |
| Bảng 2.4 | Đặc tả Use case Đổi mật khẩu | |
| Bảng 2.5 | Đặc tả Use case Cập nhật thông tin cá nhân | |
| Bảng 2.6 | Đặc tả Use case Tìm kiếm phòng trọ | |
| Bảng 2.7 | Đặc tả Use case Xem chi tiết phòng trọ | |
| Bảng 2.8 | Đặc tả Use case Lưu tin phòng trọ | |
| Bảng 2.9 | Đặc tả Use case Gửi tin nhắn | |
| Bảng 2.10 | Đặc tả Use case Đặt lịch hẹn xem phòng | |
| Bảng 2.11 | Đặc tả Use case Xác nhận/Từ chối lịch hẹn | |
| Bảng 2.12 | Đặc tả Use case Hủy lịch hẹn | |
| Bảng 2.13 | Đặc tả Use case Đăng tin phòng trọ | |
| Bảng 2.14 | Đặc tả Use case Chỉnh sửa tin phòng trọ | |
| Bảng 2.15 | Đặc tả Use case Xóa tin phòng trọ | |
| Bảng 2.16 | Đặc tả Use case Đánh dấu phòng đã cho thuê | |
| Bảng 2.17 | Đặc tả Use case Xác minh danh tính CCCD | |
| Bảng 2.18 | Đặc tả Use case Gửi yêu cầu hỗ trợ | |
| Bảng 2.19 | Đặc tả Use case Mua thêm lượt đăng bài | |
| Bảng 2.20 | Đặc tả Use case Đẩy nổi bật tin đăng | |
| Bảng 2.21 | Collection `users` (Thông tin tài khoản người dùng) | |
| Bảng 2.22 | Collection `rooms` (Tin đăng phòng trọ) | |
| Bảng 2.23 | Collection `chats` và subcollection `messages` (Hệ thống nhắn tin) | |
| Bảng 2.24 | Collection `appointments` và `bookedSlots` (Lịch hẹn xem phòng) | |
| Bảng 2.25 | Collection `verifications` (Hồ sơ xác minh CCCD) | |
| Bảng 2.26 | Collection `notifications` (Thông báo) | |
| Bảng 2.27 | Collection `support_tickets` và subcollection `messages` | |
| Bảng 2.28 | Collection `reviews` (Đánh giá chủ trọ) | |
| Bảng 3.1 | Mô tả các thành phần trong kiến trúc hệ thống | |
| Bảng 3.2 | Các collection trong Cloud Firestore | |
| Bảng 3.3 | Quy tắc bảo mật Firestore theo từng collection | |
| Bảng 3.4 | Quy tắc bảo mật Firebase Storage theo đường dẫn | |
| Bảng 3.5 | Mô tả các màn hình chính của ứng dụng | |
| Bảng 3.6 | Cấu trúc mã nguồn ứng dụng Android theo package | |
| Bảng 3.7 | Cấu trúc mã nguồn bảng điều khiển Web Admin và Cloud Functions | |
| Bảng 3.8 | Kịch bản kiểm thử các chức năng chính của hệ thống | |

---

## MỞ ĐẦU

### Lý do chọn đề tài

Nhu cầu tìm kiếm nhà trọ tại Việt Nam, đặc biệt ở các đô thị lớn như Hà Nội và Thành phố Hồ Chí Minh, ngày càng trở nên cấp thiết do tốc độ đô thị hóa nhanh và số lượng sinh viên, người lao động ngoại tỉnh không ngừng tăng lên. Tuy nhiên, các nền tảng tìm kiếm phòng trọ hiện có còn tồn tại nhiều hạn chế: tin đăng không được kiểm duyệt chặt chẽ dẫn đến thông tin sai lệch; không có hệ thống đặt lịch hẹn xem phòng trực tiếp trong ứng dụng; giao tiếp giữa người thuê và chủ trọ phụ thuộc vào nhiều kênh rời rạc.

Nhận thấy những bất cập đó và xu hướng phát triển mạnh mẽ của các công nghệ di động, điện toán đám mây và trí tuệ nhân tạo, tác giả đề xuất và thực hiện đồ án **"Phát triển ứng dụng tìm kiếm và đặt lịch xem phòng trọ"** nhằm giải quyết những vấn đề thực tế đã nêu, được triển khai trên nền tảng di động Android.

### Mục tiêu đề tài

Đề tài hướng đến các mục tiêu cụ thể sau:

- Xây dựng ứng dụng Android cho phép khách truy cập duyệt và tìm kiếm phòng trọ mà không cần đăng nhập; người dùng đã đăng nhập có thể đặt lịch hẹn, nhắn tin và lưu tin yêu thích.
- Triển khai hệ thống đặt lịch hẹn xem phòng trực tiếp, giúp người thuê và người cho thuê phối hợp thuận tiện, tránh trùng lịch nhờ cơ chế khóa khung giờ tại tầng cơ sở dữ liệu.
- Tích hợp hệ thống nhắn tin thời gian thực giữa người thuê và người cho thuê ngay trong ứng dụng.
- Xây dựng hệ thống xác minh danh tính CCCD tự động bằng công nghệ nhận dạng ký tự quang học (OCR), đảm bảo chỉ người dùng đã xác minh danh tính mới được phép đăng tin.
- Xây dựng bảng điều khiển web dành riêng cho quản trị viên để kiểm duyệt nội dung, quản lý người dùng và theo dõi thống kê hệ thống.

### Đối tượng và phạm vi nghiên cứu

**Đối tượng nghiên cứu:** Quy trình tìm kiếm phòng trọ, đặt lịch hẹn xem phòng và các tính năng hỗ trợ liên quan trong bối cảnh thị trường cho thuê nhà trọ tại Việt Nam.

**Phạm vi nghiên cứu:**
- Phát triển ứng dụng di động Android phục vụ bốn nhóm đối tượng: khách truy cập chưa đăng nhập, người thuê đã đăng nhập, người cho thuê đã xác minh danh tính và quản trị viên hệ thống.
- Xây dựng hệ thống backend dựa trên nền tảng Firebase (BaaS).
- Xây dựng bảng điều khiển web dành riêng cho quản trị viên.
- Phạm vi địa lý: thị trường phòng trọ tại Việt Nam (ưu tiên các thành phố lớn).

### Phương pháp nghiên cứu

- **Nghiên cứu tài liệu:** Tham khảo tài liệu kỹ thuật chính thức của Google Android, Firebase và Kotlin.
- **Phân tích yêu cầu:** Khảo sát thực tế nhu cầu tìm phòng trọ, xác định các tính năng cốt lõi cần có.
- **Thiết kế hệ thống:** Áp dụng kiến trúc MVVM, mô hình hóa use case và thiết kế lược đồ dữ liệu Firestore.
- **Lập trình và kiểm thử:** Phát triển theo chu kỳ lặp, kiểm thử từng chức năng trên thiết bị Android thực tế.

### Cấu trúc báo cáo

Báo cáo được tổ chức thành ba chương chính:

- **Chương 1 (Cơ sở lý thuyết và công nghệ):** Trình bày bài toán, yêu cầu hệ thống và các công nghệ được sử dụng.
- **Chương 2 (Mô hình chức năng và dữ liệu):** Trình bày mô hình chức năng, đặc tả use case chi tiết và mô hình dữ liệu.
- **Chương 3 (Mô hình phần mềm và triển khai):** Trình bày kiến trúc hệ thống, thiết kế Firebase, thiết kế giao diện và kết quả triển khai.

---

# CHƯƠNG 1: CƠ SỞ LÝ THUYẾT VÀ CÔNG NGHỆ PHÁT TRIỂN ỨNG DỤNG DI ĐỘNG

## 1.1 Bài toán tìm kiếm phòng trọ và quản lý lịch hẹn

Nhu cầu thuê nhà trọ tại Việt Nam, đặc biệt tại các thành phố lớn như Hà Nội và Thành phố Hồ Chí Minh, ngày càng tăng cao do sự dịch chuyển lao động và số lượng sinh viên nhập học tại các trường đại học. Thực tế cho thấy, việc tìm kiếm phòng trọ hiện nay vẫn còn nhiều bất cập: thông tin đăng tải trên các nền tảng trực tuyến thường không được kiểm duyệt chặt chẽ, gây ra tình trạng tin giả, tin đã cho thuê nhưng vẫn được hiển thị, hoặc thông tin không khớp với thực tế phòng trọ. Người thuê phải mất nhiều thời gian liên hệ từng chủ trọ, sắp xếp lịch đến xem phòng mà không có hệ thống hỗ trợ đặt lịch, dẫn đến nhiều bất tiện trong quá trình tìm kiếm.

Bên cạnh đó, phía chủ trọ cũng gặp khó khăn trong việc quản lý các yêu cầu xem phòng khi phải tiếp nhận thông qua nhiều kênh liên lạc khác nhau như điện thoại, mạng xã hội hay tin nhắn cá nhân, dẫn đến dễ bỏ sót hoặc nhầm lẫn lịch hẹn.

Từ thực tế đó, đề tài hướng đến việc phát triển ứng dụng **Tìm Trọ 24/7**, đây là một nền tảng kết nối giữa người tìm phòng và người cho thuê trên thiết bị di động Android, phục vụ bốn nhóm đối tượng với các tính năng chính như sau:

- **Khách truy cập (chưa đăng nhập)** có thể duyệt danh sách phòng trọ, tìm kiếm theo khu vực địa lý và tra cứu thông tin chi tiết của từng phòng trọ mà không yêu cầu tạo tài khoản.
- **Người thuê (đã đăng nhập)** có thể đặt lịch hẹn xem phòng, nhắn tin trực tiếp với chủ trọ, lưu tin yêu thích và tương tác với trợ lý AI để được tư vấn tìm phòng phù hợp.
- **Người cho thuê (đã xác minh danh tính CCCD)** có thể đăng tin phòng trọ, quản lý lịch hẹn từ người thuê và nâng cấp tin đăng thông qua hệ thống thanh toán tích hợp.
- **Quản trị viên** kiểm soát chất lượng nội dung, duyệt hồ sơ xác minh CCCD và quản lý người dùng qua bảng điều khiển web riêng biệt.

Ứng dụng không hướng đến việc thay thế các nền tảng bất động sản lớn, mà tập trung vào phân khúc phòng trọ bình dân, nơi tính xác thực thông tin và sự tiện lợi trong liên lạc là yếu tố then chốt.

---

## 1.2 Yêu cầu đặt ra đối với hệ thống

### 1.2.1 Yêu cầu đối với khách truy cập (chưa đăng nhập)

Khách truy cập là người dùng mở ứng dụng mà chưa có tài khoản hoặc chưa đăng nhập. Nhóm đối tượng này được phép sử dụng các chức năng xem và tìm kiếm thông tin công khai mà không cần xác thực:

- Xem danh sách phòng trọ theo khu vực phổ biến, tin nổi bật và tin mới nhất trên trang chủ.
- Tìm kiếm và lọc phòng trọ theo địa điểm, mức giá và tiện ích.
- Xem thông tin chi tiết phòng trọ bao gồm hình ảnh, mô tả, giá điện nước và thông tin liên hệ (họ tên, số điện thoại) của chủ trọ.
- Thực hiện cuộc gọi trực tiếp đến số điện thoại của chủ trọ thông qua trình quay số của thiết bị.
- Xem vị trí phòng trọ trên bản đồ Google Maps.

Khi khách truy cập thực hiện các thao tác yêu cầu đăng nhập (đặt lịch, nhắn tin, lưu tin), hệ thống sẽ tự động hiển thị hộp thoại thông báo và điều hướng đến màn hình đăng nhập. Trợ lý ảo AI yêu cầu tài khoản đã đăng nhập và không khả dụng đối với khách truy cập.

### 1.2.2 Yêu cầu đối với người thuê (đã đăng nhập)

Người thuê là người dùng đã tạo tài khoản và đăng nhập thành công. Nhóm đối tượng này kế thừa đầy đủ các quyền xem tin công khai của khách truy cập, đồng thời được cung cấp các chức năng tương tác nâng cao yêu cầu xác thực:

- Đăng nhập hệ thống; cập nhật hồ sơ cá nhân (số điện thoại, địa chỉ, ngày sinh, giới tính, tiểu sử); thay đổi mật khẩu và sử dụng chức năng khôi phục mật khẩu khi quên.
- Lưu bài đăng phòng trọ yêu thích vào danh sách lưu trữ riêng để xem lại hoặc quản lý sau này.
- Gửi yêu cầu đặt lịch hẹn xem phòng trực tiếp với người cho thuê, lựa chọn cụ thể ngày và khung giờ mong muốn trong ứng dụng.
- Theo dõi danh sách lịch hẹn và quản lý trạng thái lịch hẹn thời gian thực (Chờ xác nhận, Đã xác nhận, Người thuê đã xác nhận đến xem, Bị từ chối, Đã hủy, Đã hoàn thành).
- Nhắn tin trò chuyện thời gian thực trực tiếp với người cho thuê thông qua hệ thống chat tích hợp sẵn.
- Sử dụng trợ lý ảo AI tìm phòng thông minh chạy offline (on-device NLP) để trò chuyện hỏi đáp và nhận gợi ý danh sách phòng phù hợp ngay trong cuộc hội thoại.
- Gửi yêu cầu hỗ trợ kỹ thuật kèm tiêu đề, nội dung mô tả chi tiết và danh mục phân loại trực tiếp đến quản trị viên khi gặp sự cố hệ thống.
- Thực hiện gửi hồ sơ xác minh danh tính cá nhân (chụp ảnh 2 mặt thẻ CCCD thông qua giao diện camera custom) để được hệ thống xem xét cấp quyền chủ trọ (Người đăng tin).

### 1.2.3 Yêu cầu đối với người cho thuê (đã xác minh CCCD)

Người cho thuê là người dùng đã hoàn tất việc xác minh danh tính bằng ảnh chụp Căn cước công dân (CCCD) và được quản trị viên phê duyệt. Nhóm đối tượng này kế thừa toàn bộ quyền hạn tương tác của người thuê, đồng thời sở hữu các đặc quyền quản lý tin đăng và lịch hẹn xem phòng:

- Đăng tin cho thuê phòng trọ mới kèm theo việc đăng tải hình ảnh thực tế (tối đa 10 ảnh), định vị vị trí chính xác trên bản đồ và cung cấp đầy đủ thông tin chi tiết (như diện tích, giá thuê, giá điện nước, tiền đặt cọc, các tiện ích đi kèm và nội quy phòng trọ).
- Chỉnh sửa nội dung thông tin phòng trọ hoặc thực hiện xóa tin đăng khi không còn nhu cầu; hệ thống sẽ tự động dọn dẹp các tệp hình ảnh liên quan để tối ưu hóa không gian lưu trữ của hệ thống.
- Đánh dấu trạng thái phòng đã tìm được người thuê; hệ thống sẽ tự động cập nhật trạng thái tin đăng, gỡ bài đăng khỏi danh sách yêu thích của các khách thuê khác, đồng thời tự động hủy toàn bộ các lịch hẹn xem phòng còn lại của phòng trọ này và gửi thông báo trực tiếp đến những khách thuê bị hủy lịch.
- Phê duyệt đồng ý hoặc từ chối các yêu cầu đặt lịch hẹn xem phòng từ phía người thuê (cho phép chủ trọ nhập lý do cụ thể khi từ chối lịch hẹn).
- Quản lý danh sách các bài đăng cá nhân và theo dõi trực quan trạng thái kiểm duyệt của từng tin đăng (chờ duyệt, đã duyệt, bị từ chối, hết hạn hoặc đã cho thuê).
- Thực hiện nâng cấp bài viết lên vị trí nổi bật hoặc mua thêm lượt đăng tin mới thông qua việc quét mã QR thanh toán tự động vô cùng nhanh chóng và tiện lợi.
- Tương tác với trợ lý ảo AI thông minh ngay trên ứng dụng để nhận hỗ trợ giải đáp các thắc mắc, hướng dẫn sử dụng tính năng hoặc nhận gợi ý tìm phòng trọ phù hợp theo khu vực và mức giá mong muốn.

### 1.2.4 Yêu cầu đối với quản trị viên

Quản trị viên quản lý và vận hành hệ thống thông qua bảng điều khiển Web Admin Panel. Đây là nhóm đối tượng hoàn toàn độc lập, không sử dụng ứng dụng di động mà thao tác qua giao diện trang web quản trị riêng biệt:

- Quản lý tài khoản người dùng: Xem thông tin chi tiết hồ sơ cá nhân, thực hiện tạm khóa tài khoản vi phạm (thiết lập cụ thể lý do và thời gian khóa) hoặc mở khóa tài khoản hoạt động trở lại.
- Kiểm duyệt bài đăng mới: Phê duyệt cho phép hiển thị tin phòng trọ công khai hoặc từ chối bài đăng vi phạm quy định (chủ trọ sẽ nhận được thông báo phản hồi kèm lý do từ chối cụ thể).
- Gỡ bỏ hoặc xóa bỏ hoàn toàn các bài đăng có nội dung vi phạm hoặc bài đăng không còn giá trị sử dụng khỏi hệ thống.
- Xem xét và phê duyệt hồ sơ xác minh danh tính của người cho thuê: Đối chiếu thông tin ảnh chụp thực tế thẻ Căn cước công dân và kết quả kiểm tra tự động từ hệ thống để phê duyệt cấp quyền đăng tin hoặc từ chối yêu cầu xác minh.
- Tiếp nhận, trao đổi phản hồi và xử lý các yêu cầu hỗ trợ kỹ thuật hoặc báo cáo sự cố từ phía người dùng gửi về hệ thống.
- Theo dõi biểu đồ thống kê tổng quan về số lượng bài đăng mới phát sinh, số lượng người dùng tham gia và cơ cấu các nhóm đối tượng người dùng trên hệ thống thông qua bảng điều khiển tổng quan (dashboard).
- Xuất danh sách thông tin người dùng và các bài đăng phòng trọ ra tệp định dạng Excel phục vụ mục đích lưu trữ báo cáo số liệu.
- Quét hệ thống và dọn dẹp các tài khoản không hoạt động (tài khoản ngủ đông lâu ngày không có lượt đăng nhập theo mốc thời gian tùy chọn) để tối ưu hiệu năng và giải phóng tài nguyên lưu trữ của hệ thống.

### 1.2.5 Yêu cầu đối với chức năng quản lý lịch hẹn

- Cho phép người thuê gửi yêu cầu đặt lịch xem phòng với ngày giờ cụ thể.
- Cho phép người cho thuê xác nhận hoặc từ chối lịch hẹn.
- Hiển thị lịch hẹn theo ngày, giờ, trạng thái (chờ xác nhận, đã xác nhận, người thuê đã xác nhận đến xem, đã hoàn thành, bị từ chối, người thuê đã hủy, hệ thống tự hủy).
- Gửi thông báo đẩy (push notification) khi lịch hẹn được tạo, cập nhật hoặc hủy.
- Hạn chế trùng lịch giữa các cuộc hẹn trong cùng một khung giờ bằng cơ chế khóa khung giờ ở tầng cơ sở dữ liệu.
- Cho phép người thuê hủy lịch hẹn và giải phóng khung giờ đã đặt.
- Hệ thống tự động hủy lịch hẹn của phòng khi chủ trọ đánh dấu phòng đã cho thuê.

### 1.2.6 Yêu cầu phi chức năng

- **Giao diện:** Thân thiện, dễ sử dụng trên thiết bị di động, tuân theo nguyên tắc Material Design 3.
- **Thời gian thực:** Dữ liệu lịch hẹn, tin nhắn và thông báo phải được cập nhật ngay lập tức trên mọi thiết bị nhờ cơ chế lắng nghe snapshot listener của Firestore.
- **Bảo mật:** Toàn bộ dữ liệu được bảo vệ bởi Firestore Security Rules và Firebase Storage Rules, đảm bảo mỗi người dùng chỉ truy cập được dữ liệu được phân quyền. Mật khẩu được quản lý hoàn toàn bởi Firebase Authentication.
- **Hiệu năng:** Danh sách phòng trọ được phân trang (10 bản ghi mỗi lần tải) nhằm giảm thiểu băng thông và tránh quá tải tài nguyên hệ thống. Thống kê trên bảng điều khiển được tổng hợp sẵn phía máy chủ thông qua Cloud Functions, giúp giảm đáng kể số lượt đọc Firestore so với việc truy vấn trực tiếp từ client.
- **Xác thực người dùng an toàn:** Sử dụng Firebase Authentication với mật khẩu mạnh (tối thiểu 12 ký tự, có chữ hoa, số và ký tự đặc biệt).
- **Khả năng mở rộng:** Kiến trúc Firebase Serverless cho phép hệ thống tự động mở rộng theo lượng người dùng mà không cần quản lý hạ tầng máy chủ.
- **Tính khả dụng:** Ứng dụng hoạt động ổn định trên Android từ phiên bản 7.0 (API 24) trở lên.
- **Đồng bộ dữ liệu:** Dữ liệu được đồng bộ theo thời gian thực giữa các thiết bị qua Firestore.
- **Hỗ trợ bản đồ:** Tích hợp Google Maps API để hiển thị vị trí phòng trọ và hỗ trợ tìm kiếm theo khu vực.

---

## 1.3 Công nghệ phát triển

### 1.3.1 Công nghệ phát triển ứng dụng di động Android

Ứng dụng được xây dựng dành riêng cho nền tảng Android với các công nghệ sau:

- **Ngôn ngữ lập trình Kotlin:** Là ngôn ngữ lập trình chính thức được Google khuyến nghị cho phát triển ứng dụng Android từ năm 2017. Kotlin có cú pháp ngắn gọn, an toàn kiểu dữ liệu và hỗ trợ tốt lập trình bất đồng bộ (coroutines), giúp xây dựng ứng dụng ổn định và dễ bảo trì.
- **Kiến trúc MVVM (Model-View-ViewModel):** Đây là kiến trúc được Google chính thức khuyến nghị cho ứng dụng Android. MVVM tách biệt rõ ràng giữa lớp giao diện (View), lớp xử lý logic nghiệp vụ (ViewModel) và lớp dữ liệu (Model/Repository), giúp code dễ kiểm thử và mở rộng.
- **Môi trường phát triển:** Android Studio Ladybug (2024), phiên bản Android tối thiểu Android 7.0 (API 24), mục tiêu Android 15 (API 35).
- **Android Jetpack Libraries:** Bao gồm LiveData, ViewModel, CameraX và ViewBinding. LiveData và ViewModel giúp quản lý vòng đời của dữ liệu trên giao diện; CameraX cung cấp API camera thống nhất để chụp ảnh CCCD trong quá trình xác minh danh tính; ViewBinding cho phép tham chiếu an toàn đến các thành phần giao diện.
- **Glide 4.16.0:** Thư viện tải và hiển thị hình ảnh từ URL, hỗ trợ bộ nhớ đệm và xử lý ảnh hiệu quả.
- **ML Kit Text Recognition 16.0.1:** Bộ công cụ nhận dạng văn bản trực tiếp trên thiết bị (on-device OCR) do Google cung cấp, được sử dụng để trích xuất tự động thông tin trên thẻ CCCD trực tiếp từ camera.
- **OkHttp 4.12.0:** Thư viện HTTP Client để giao tiếp với Cloud Functions.
- **Trợ lý AI tìm phòng (ChatbotEngine):** Được xây dựng hoàn toàn on-device bằng Kotlin, sử dụng thuật toán TF-IDF (Term Frequency–Inverse Document Frequency) để phân tích câu hỏi của người dùng, nhận diện ý định tìm kiếm và trả về gợi ý phòng trọ theo khu vực và mức giá mà không cần kết nối internet hay gọi API bên ngoài.
- **Markwon 4.6.2:** Thư viện render nội dung Markdown trực tiếp trong giao diện Android, được sử dụng để hiển thị các câu trả lời có định dạng từ trợ lý AI trong màn hình Chat AI.
- **Thiết kế giao diện:** Tuân theo Material Design 3 của Google với bảng màu chủ đạo là xanh lam (blue).

### 1.3.2 Nền tảng Firebase

Toàn bộ hệ thống backend được xây dựng trên nền tảng **Firebase** của Google, đây là một nền tảng phát triển ứng dụng đám mây cung cấp đầy đủ các dịch vụ cần thiết:

- **Firebase Authentication:** Quản lý xác thực người dùng bằng email/mật khẩu, cung cấp cơ chế bảo mật tin cậy cho quá trình đăng ký và đăng nhập.
- **Cloud Firestore:** Cơ sở dữ liệu NoSQL dạng tài liệu (document-oriented) với khả năng đồng bộ dữ liệu theo thời gian thực. Firestore phù hợp cho các ứng dụng cần cập nhật trực tiếp như hệ thống nhắn tin, thông báo và quản lý lịch hẹn.
- **Firebase Cloud Storage:** Lưu trữ các tệp nhị phân như ảnh phòng trọ, ảnh đại diện và ảnh CCCD xác minh. Tích hợp liền mạch với Firestore và Firebase Authentication thông qua Security Rules.
- **Firebase Cloud Messaging (FCM):** Dịch vụ gửi thông báo đẩy đến thiết bị Android, đảm bảo người dùng nhận được thông báo ngay cả khi ứng dụng đang chạy nền.
- **Firebase Cloud Functions:** Nền tảng không máy chủ (serverless) cho phép thực thi mã nguồn JavaScript (Node.js) ở phía máy chủ mà không cần quản lý hạ tầng phức tạp. Cloud Functions đảm nhận xử lý các tác vụ nghiệp vụ nâng cao như tự động kiểm duyệt CCCD thông qua tích hợp Google Cloud Vision, đối soát giao dịch thanh toán tự động qua SePay và dọn dẹp dữ liệu định kỳ.
- **Firebase Hosting:** Lưu trữ và phân phối bảng điều khiển Web Admin qua CDN toàn cầu.
- **Firebase BOM 33.1.2:** Quản lý thống nhất phiên bản của tất cả SDK Firebase trong dự án Android.

### 1.3.3 Công nghệ bản đồ và định vị

- **Google Maps API:** Tích hợp bản đồ tương tác để hiển thị vị trí phòng trọ bằng marker. Hỗ trợ người dùng quan sát trực quan vị trí phòng trọ trên bản đồ và tìm kiếm theo khu vực địa lý.
- **Lưu trữ tọa độ:** Mỗi bài đăng phòng trọ lưu cặp tọa độ vĩ độ (latitude) và kinh độ (longitude) để hiển thị chính xác vị trí trên bản đồ.
- **Tìm kiếm theo khu vực:** Hỗ trợ lọc kết quả theo quận/huyện, phường/xã từ danh sách địa chỉ hành chính được chuẩn hóa (AddressData).
- **Thuật toán giới hạn tọa độ (Bounding Box):** Để tối ưu hóa hiệu năng truy vấn của Cloud Firestore và giảm thiểu chi phí đọc dữ liệu (Read Quotas), hệ thống áp dụng thuật toán giới hạn tọa độ trên vĩ độ và kinh độ dựa trên bán kính tìm kiếm thực tế (ví dụ: `radiusKm / 111.32` độ vĩ tuyến). Việc lọc phạm vi tọa độ sơ bộ trực tiếp trên server (Bounding Box Query) giúp giảm lượng tài liệu phải tải về phía client, tăng tốc độ xử lý định vị lân cận.

### 1.3.4 Công nghệ nhận dạng ký tự quang học và thanh toán

**Công nghệ nhận dạng ký tự quang học (OCR):**
- **ML Kit Text Recognition (On-device):** Nhận dạng văn bản trực tiếp trên thiết bị trong quá trình chụp ảnh CCCD, phân tích kết quả ngay lập tức mà không cần kết nối internet, giúp phản hồi nhanh cho người dùng.
- **Google Cloud Vision API (Cloud):** Dịch vụ nhận dạng văn bản trên đám mây, được kích hoạt qua Cloud Functions khi ML Kit on-device không cho kết quả đủ tin cậy. Dịch vụ này phân tích ảnh CCCD mặt trước và mặt sau, trích xuất số CCCD và họ tên để đối khớp với thông tin người dùng đã nhập.

**Công nghệ thanh toán tự động SePay:**
- **Kiến trúc Lai (Hybrid Architecture):** Hệ thống tích hợp cổng thanh toán trực tuyến SePay để tự động hóa quy trình nạp lượt đăng bài và nâng cấp bài nổi bật của chủ trọ với hai cơ chế bổ trợ lẫn nhau:
  - *Cơ chế chính (Real-time Webhook):* Hệ thống cung cấp một Endpoint HTTPS trên nền tảng Cloud Functions để tiếp nhận dữ liệu POST tức thời từ SePay ngay khi có giao dịch chuyển khoản thành công với nội dung chuyển khoản khớp mã yêu cầu định danh (`REQ-xxxxxxxx`), kích hoạt quyền lợi cho người dùng trong vòng dưới 2 giây.
  - *Cơ chế dự phòng (Scheduled Polling):* Hệ thống thiết lập các tác vụ lập lịch tự động định kỳ mỗi 1 phút ở phía server để chủ động truy vấn danh sách giao dịch từ API SePay. Cơ chế này đóng vai trò đối soát dự phòng, đảm bảo phát hiện và xử lý thành công toàn bộ giao dịch ngay cả khi Webhook bị gián đoạn kết nối mạng.

### 1.3.5 Công nghệ Trợ lý ảo AI tìm kiếm thông minh (AI Chatbot Engine)

Để nâng cao trải nghiệm người dùng, ứng dụng tích hợp một Trợ lý ảo AI hỗ trợ tìm kiếm phòng trọ tương tác trực tiếp chạy hoàn toàn trên thiết bị (On-device NLP):
- **Xử lý ngôn ngữ tự nhiên tiếng Việt:** Sử dụng các kỹ thuật tiền xử lý văn bản, chuẩn hóa từ viết tắt trong tiếng Việt (ví dụ từ "ptro" thành "phòng trọ", "ko" thành "không", "đc" thành "được") và tách từ (Tokenization).
- **Thuật toán TF-IDF và Cosine Similarity:** Chuyển đổi câu hỏi của người dùng và các mẫu ý định (patterns) thành các vector tần suất từ và tính toán độ tương đồng Cosine để xác định ý định người dùng (Intent Matching) với độ chính xác cao mà không cần kết nối Internet.
- **Mô hình Trạng thái (State Machine):** AI hỗ trợ duy trì ngữ cảnh cuộc hội thoại qua nhiều bước (từ bước nhận biết khu vực, nhận diện mức giá mong muốn cho đến trích xuất tham số lọc cụ thể) để tự động điền bộ lọc và thực hiện tìm kiếm phòng trọ theo yêu cầu thực tế.

### 1.3.6 Công nghệ bảng điều khiển Web Admin

Bảng điều khiển dành cho quản trị viên được xây dựng bằng các công nghệ web nhẹ, tối ưu cho nhu cầu quản trị nội bộ:

- **JavaScript (ES6+) thuần:** Phát triển giao diện quản trị viên trực tiếp mà không phụ thuộc vào framework frontend phức tạp, giúp ứng dụng tải nhanh và dễ bảo trì.
- **Firebase JS SDK:** Kết nối trực tiếp với Firestore, Authentication và Storage từ trình duyệt web, đồng bộ dữ liệu thời gian thực.
- **Chart.js:** Vẽ biểu đồ thống kê trực quan (biểu đồ đường số bài đăng theo 6 tháng gần nhất, biểu đồ vành khuyên phân loại người dùng theo trạng thái xác minh).
- **XLSX:** Xuất danh sách người dùng và bài đăng ra file Excel phục vụ lưu trữ và báo cáo nội bộ.
- **Firebase Hosting:** Triển khai và phân phối bảng điều khiển qua CDN toàn cầu, đảm bảo tốc độ tải trang ổn định.
### Tổng kết Chương 1

Chương 1 đã trình bày toàn diện bài toán thực tiễn về tìm kiếm phòng trọ và xác định rõ các yêu cầu chức năng cho bốn nhóm tác nhân của hệ thống bao gồm: khách truy cập chưa đăng nhập, người thuê đã đăng nhập, người cho thuê đã xác minh danh tính và quản trị viên. Mỗi nhóm tác nhân sở hữu phạm vi quyền hạn và chức năng riêng biệt, tạo thành một mô hình phân quyền chặt chẽ. Bên cạnh đó, các yêu cầu phi chức năng cũng được thiết lập cụ thể về tính thời gian thực, bảo mật, hiệu năng và khả năng mở rộng hệ thống. Về mặt công nghệ, chương đã làm rõ cơ sở khoa học và thực tiễn của việc lựa chọn ngôn ngữ Kotlin kết hợp kiến trúc MVVM cho ứng dụng di động, nền tảng Firebase làm backend không máy chủ, Google Maps API cho các tính năng định vị, giải pháp ML Kit và Google Cloud Vision cho nhận dạng CCCD tự động, cổng SePay cho hệ thống thanh toán, thuật toán TF-IDF on-device cho trợ lý AI tìm phòng và công nghệ JavaScript thuần kết hợp Firebase JS SDK cho bảng điều khiển Web Admin. Các nền tảng kỹ thuật này là cơ sở vững chắc để xây dựng mô hình chức năng, cấu trúc cơ sở dữ liệu và triển khai chi tiết hệ thống ở các chương tiếp theo.

---

# CHƯƠNG 2: MÔ HÌNH CHỨC NĂNG VÀ DỮ LIỆU

## 2.1 Mô hình chức năng

### 2.1.1 Quy tắc nghiệp vụ hệ thống

- Người dùng phải đăng nhập để đặt lịch hẹn, đăng tin, nhắn tin, lưu phòng yêu thích hoặc gửi yêu cầu hỗ trợ.
- Người cho thuê (người đăng tin) chỉ được đăng tin sau khi xác minh danh tính CCCD thành công và được quản trị viên phê duyệt.
- Người cho thuê chỉ được quản lý các bài đăng do mình tạo ra.
- Bài đăng phòng trọ cần được quản trị viên kiểm duyệt trước khi hiển thị công khai.
- Mỗi tài khoản đăng tin được tối đa 3 tin miễn phí trong 24 giờ; vượt quá phải mua thêm slot qua cổng thanh toán SePay.
- Một phòng trọ có thể có nhiều lịch hẹn xem phòng nhưng không được trùng khung giờ.
- Một lịch hẹn chỉ thuộc về một người thuê và một phòng trọ cụ thể.
- Lịch hẹn có các trạng thái: chờ xác nhận, đã xác nhận, người thuê đã xác nhận đến xem, đã hoàn thành, bị từ chối, người thuê đã hủy, hệ thống tự hủy.
- Khi chủ trọ đánh dấu phòng đã cho thuê, hệ thống tự động hủy toàn bộ lịch hẹn đang chờ và đã xác nhận của phòng đó.
- Người thuê chỉ được đánh giá chủ trọ sau khi đã có lịch hẹn với phòng trọ liên quan.
- Quản trị viên có quyền ẩn, xóa bài đăng hoặc khóa/xóa tài khoản vi phạm.
- Số CCCD đã được đăng ký không thể dùng để xác minh cho tài khoản khác.
- Hệ thống tự động mở khóa tài khoản khi hết thời hạn khóa do quản trị viên thiết lập.

### 2.1.2 Xác định và mô tả tác nhân

- **Khách truy cập (Người dùng chưa đăng nhập):** Xem danh sách phòng trọ công khai, tìm kiếm phòng trọ. Không thể đặt lịch, nhắn tin hay lưu yêu thích.
- **Người thuê (Người dùng đã đăng nhập, chưa xác minh CCCD):** Tìm kiếm, xem chi tiết, lưu yêu thích, đặt lịch hẹn, nhắn tin với người đăng tin, đánh giá, gửi hỗ trợ. Không thể đăng tin.
- **Người đăng tin (Người dùng đã xác minh CCCD):** Bao gồm tất cả quyền của người thuê và thêm: đăng tin, chỉnh sửa/xóa tin, quản lý lịch hẹn từ người dùng, nâng cấp tin đăng.
- **Quản trị viên:** Quản lý hệ thống qua Web Admin Panel nhằm kiểm duyệt bài đăng, duyệt hồ sơ CCCD, quản lý người dùng, phản hồi hỗ trợ, xem thống kê, gửi thông báo hàng loạt.
- **Hệ thống Firebase:** Xác thực tài khoản, lưu trữ dữ liệu, lưu trữ hình ảnh, đồng bộ dữ liệu thời gian thực, chạy Cloud Functions (OCR, thanh toán, FCM).
- **Hệ thống thông báo (FCM):** Gửi thông báo đẩy đến thiết bị Android liên quan đến lịch hẹn, bài đăng được duyệt, kết quả xác minh CCCD và phản hồi hỗ trợ.

### 2.1.3 Xác định các chức năng chính

1. Đăng ký tài khoản
2. Đăng nhập
3. Quản lý thông tin cá nhân
4. Xem danh sách phòng trọ
5. Tìm kiếm và lọc phòng trọ
6. Xem chi tiết phòng trọ
7. Xem phòng trọ trên bản đồ
8. Lưu phòng trọ yêu thích
9. Liên hệ người cho thuê (nhắn tin)
10. Đặt lịch hẹn xem phòng
11. Quản lý lịch hẹn
12. Đăng tin phòng trọ
13. Quản lý bài đăng phòng trọ
14. Đánh giá và bình luận phòng trọ
15. Kiểm duyệt bài đăng (quản trị viên)
16. Quản lý người dùng (quản trị viên)
17. Xác minh danh tính CCCD
18. Thống kê hệ thống (quản trị viên)

> **[Hình 2.1: Biểu đồ Use case tổng quát hệ thống]**
>
> *(Biểu đồ thể hiện 3 tác nhân chính: Người dùng, Người đăng tin (đã xác minh), Quản trị viên và các use case chính tương ứng)*

> **[Hình 2.2: Biểu đồ Use case phân rã tác nhân Người dùng]**
>
> *(Phân rã: Đăng ký/Đăng nhập, Tìm kiếm & lọc phòng, Xem chi tiết phòng, Lưu tin yêu thích, Đặt lịch hẹn, Nhắn tin với người đăng tin, Gửi yêu cầu hỗ trợ)*

> **[Hình 2.3: Biểu đồ Use case phân rã tác nhân Người đăng tin (đã xác minh)]**
>
> *(Phân rã: Kế thừa toàn bộ use case của người dùng + Đăng tin phòng, Chỉnh sửa/Xóa tin, Quản lý lịch hẹn, Nâng cấp tin đăng)*

> **[Hình 2.4: Biểu đồ Use case phân rã tác nhân Quản trị viên]**
>
> *(Phân rã: Duyệt tin đăng, Duyệt CCCD, Quản lý người dùng, Phản hồi hỗ trợ, Xem thống kê, Gửi thông báo hàng loạt)*

---

## 2.2 Mô tả chi tiết các chức năng

### 2.2.1 Chức năng đăng ký tài khoản

> **[Hình 2.5: Biểu đồ Use case Quản lý tài khoản]**

**Bảng 2.1: Đặc tả Use case Đăng ký tài khoản**

| Trường | Nội dung |
|---|---|
| **Tên use case** | Đăng ký tài khoản |
| **Tác nhân chính** | Người dùng |
| **Mục đích** | Cho phép người dùng mới tạo tài khoản trong hệ thống |
| **Mức độ ưu tiên** | Bắt Buộc |
| **Điều kiện kích hoạt** | Khi người dùng nhấn nút "Đăng ký" trên màn hình đăng nhập |
| **Điều kiện tiên quyết** | Người dùng chưa đăng nhập vào hệ thống |
| **Điều kiện thành công** | Tài khoản được tạo thành công; người dùng được đăng nhập tự động và chuyển đến trang chủ |
| **Điều kiện thất bại** | Tài khoản không được tạo; người dùng vẫn ở màn hình đăng ký với thông báo lỗi |
| **Luồng sự kiện chính** | 1. Người dùng nhấn "Đăng ký" tại màn hình đăng nhập<br>2. Hệ thống hiển thị form đăng ký<br>3. Người dùng nhập họ tên, số điện thoại, email Gmail, mật khẩu (tối thiểu 12 ký tự, có chữ hoa, chữ số và ký tự đặc biệt)<br>4. Người dùng nhấn "Tạo tài khoản"<br>5. Hệ thống kiểm tra tính hợp lệ của thông tin<br>6. Hệ thống kiểm tra số điện thoại chưa được dùng<br>7. Hệ thống tạo tài khoản Firebase Authentication và lưu thông tin vào Firestore<br>8. Hệ thống đăng nhập tự động và chuyển đến trang chủ |
| **Luồng thay thế** | Không có |
| **Luồng ngoại lệ** | 5a. Email không hợp lệ hoặc không phải Gmail — thông báo lỗi, quay lại bước 3<br>5b. Mật khẩu không đủ độ mạnh — thông báo yêu cầu, quay lại bước 3<br>6a. Số điện thoại đã tồn tại trong hệ thống — thông báo lỗi |

> **[Hình 2.6: Biểu đồ hoạt động Đăng ký tài khoản]**

> **[Hình 2.7: Biểu đồ tuần tự Đăng ký tài khoản]**
> *(Các đối tượng: RegisterActivity — AuthViewModel — AuthRepository — FirebaseAuth — Firestore)*

### 2.2.2 Chức năng đăng nhập

**Bảng 2.2: Đặc tả Use case Đăng nhập**

| Trường | Nội dung |
|---|---|
| **Tên use case** | Đăng nhập |
| **Tác nhân chính** | Người dùng, Người đăng tin |
| **Mục đích** | Cho phép người dùng đã có tài khoản xác thực và truy cập hệ thống |
| **Mức độ ưu tiên** | Bắt Buộc |
| **Điều kiện kích hoạt** | Khi người dùng nhấn nút "Đăng nhập" |
| **Điều kiện tiên quyết** | Người dùng đã có tài khoản trong hệ thống |
| **Điều kiện thành công** | Người dùng đăng nhập thành công và được chuyển đến trang chủ |
| **Điều kiện thất bại** | Người dùng không thể truy cập hệ thống; vẫn ở màn hình đăng nhập với thông báo lỗi |
| **Luồng sự kiện chính** | 1. Người dùng nhập email và mật khẩu<br>2. Người dùng nhấn "Đăng nhập"<br>3. Hệ thống xác thực thông tin qua Firebase Authentication<br>4. Hệ thống kiểm tra trạng thái tài khoản (bị khóa hay không)<br>5. Hệ thống cập nhật token FCM cho thiết bị hiện tại<br>6. Hệ thống chuyển đến trang chủ |
| **Luồng thay thế** | Không có |
| **Luồng ngoại lệ** | 3a. Email hoặc mật khẩu không đúng — thông báo lỗi, quay lại bước 1<br>4a. Tài khoản bị khóa — thông báo lý do khóa và thời gian mở khóa |

**Bảng 2.3: Đặc tả Use case Quên mật khẩu**

| Trường | Nội dung |
|---|---|
| **Tên use case** | Quên mật khẩu |
| **Tác nhân chính** | Người dùng |
| **Mục đích** | Cho phép người dùng lấy lại quyền truy cập tài khoản khi quên mật khẩu |
| **Mức độ ưu tiên** | Bắt Buộc |
| **Điều kiện kích hoạt** | Khi người dùng nhấn "Quên mật khẩu" trên màn hình đăng nhập |
| **Điều kiện tiên quyết** | Người dùng đã có tài khoản trong hệ thống |
| **Điều kiện thành công** | Email đặt lại mật khẩu được gửi đến địa chỉ email của người dùng |
| **Điều kiện thất bại** | Email không được gửi; người dùng vẫn ở màn hình với thông báo lỗi |
| **Luồng sự kiện chính** | 1. Người dùng nhấn "Quên mật khẩu" tại màn hình đăng nhập<br>2. Hệ thống hiển thị form nhập email và số điện thoại<br>3. Người dùng nhập email và số điện thoại đã đăng ký<br>4. Hệ thống xác minh email và số điện thoại khớp với tài khoản<br>5. Hệ thống gửi email đặt lại mật khẩu qua Firebase Auth<br>6. Hệ thống thông báo "Email đặt lại mật khẩu đã được gửi" |
| **Luồng ngoại lệ** | 3a. Email không đúng định dạng — thông báo lỗi<br>4a. Email và số điện thoại không khớp — thông báo lỗi |

### 2.2.3 Chức năng quản lý thông tin cá nhân

**Bảng 2.4: Đặc tả Use case Đổi mật khẩu**

| Trường | Nội dung |
|---|---|
| **Tên use case** | Đổi mật khẩu |
| **Tác nhân chính** | Người dùng |
| **Mục đích** | Cho phép người dùng thay đổi mật khẩu tài khoản |
| **Mức độ ưu tiên** | Quan Trọng |
| **Điều kiện kích hoạt** | Khi người dùng nhấn "Đổi mật khẩu" trong cài đặt tài khoản |
| **Điều kiện tiên quyết** | Người dùng đã đăng nhập |
| **Điều kiện thành công** | Mật khẩu được cập nhật; người dùng được đăng xuất tự động và chuyển về màn hình đăng nhập |
| **Điều kiện thất bại** | Mật khẩu không được cập nhật; hệ thống thông báo lỗi |
| **Luồng sự kiện chính** | 1. Người dùng nhập mật khẩu cũ, mật khẩu mới và xác nhận mật khẩu mới<br>2. Hệ thống kiểm tra mật khẩu mới đáp ứng yêu cầu (tối thiểu 12 ký tự)<br>3. Hệ thống xác thực lại với Firebase Auth bằng mật khẩu cũ<br>4. Hệ thống cập nhật mật khẩu mới trong Firebase Auth<br>5. Hệ thống tự động đăng xuất và chuyển về màn hình đăng nhập |
| **Luồng ngoại lệ** | 2a. Mật khẩu mới không đủ mạnh — thông báo yêu cầu cụ thể<br>2b. Mật khẩu xác nhận không khớp — thông báo lỗi<br>3a. Mật khẩu cũ không đúng — thông báo xác thực thất bại |

**Bảng 2.5: Đặc tả Use case Cập nhật thông tin cá nhân**

| Trường | Nội dung |
|---|---|
| **Tên use case** | Cập nhật thông tin cá nhân |
| **Tác nhân chính** | Người dùng |
| **Mục đích** | Cho phép người dùng chỉnh sửa thông tin hồ sơ cá nhân |
| **Mức độ ưu tiên** | Quan Trọng |
| **Điều kiện kích hoạt** | Khi người dùng nhấn "Chỉnh sửa thông tin" trong hồ sơ cá nhân |
| **Điều kiện tiên quyết** | Người dùng đã đăng nhập |
| **Điều kiện thành công** | Thông tin cá nhân được cập nhật thành công trong Firestore |
| **Điều kiện thất bại** | Thông tin không được cập nhật; hệ thống thông báo lỗi |
| **Luồng sự kiện chính** | 1. Hệ thống tải thông tin hiện tại từ Firestore và hiển thị lên form<br>2. Người dùng chỉnh sửa: họ tên, số điện thoại, địa chỉ, ngày sinh, giới tính, nghề nghiệp<br>3. Người dùng nhấn "Lưu"<br>4. Hệ thống cập nhật dữ liệu vào Firestore<br>5. Hệ thống thông báo cập nhật thành công |
| **Luồng thay thế** | Người dùng thay đổi email — hệ thống yêu cầu xác thực lại bằng mật khẩu hiện tại |
| **Luồng ngoại lệ** | 2a. Các trường bắt buộc bị bỏ trống — thông báo lỗi |

### 2.2.4 Chức năng xem danh sách phòng trọ

Trang chủ hiển thị các khu vực phổ biến có nhiều phòng trọ nhất (dựa trên thống kê từ Firestore), danh sách tin nổi bật (tự động luân chuyển mỗi 10 giây) và danh sách phòng mới đăng. Người dùng cuộn xuống để tải thêm 10 bản ghi mỗi lần. Tính năng này không yêu cầu đăng nhập — khách truy cập đã có thể xem danh sách phòng công khai.

### 2.2.5 Chức năng xem chi tiết phòng trọ

**Bảng 2.6: Đặc tả Use case Xem chi tiết phòng trọ**

| Trường | Nội dung |
|---|---|
| **Tên use case** | Xem chi tiết phòng trọ |
| **Tác nhân chính** | Người dùng |
| **Mục đích** | Cho phép người dùng xem toàn bộ thông tin của một phòng trọ |
| **Mức độ ưu tiên** | Bắt Buộc |
| **Điều kiện kích hoạt** | Khi người dùng nhấn vào một phòng trong danh sách |
| **Điều kiện tiên quyết** | Người dùng đã đăng nhập |
| **Điều kiện thành công** | Hệ thống hiển thị đầy đủ thông tin chi tiết phòng trọ |
| **Điều kiện thất bại** | Không thể tải thông tin phòng; hệ thống thông báo lỗi và quay lại danh sách |
| **Luồng sự kiện chính** | 1. Người dùng nhấn vào phòng muốn xem<br>2. Hệ thống truy vấn Firestore lấy thông tin chi tiết<br>3. Hệ thống hiển thị: hình ảnh, địa chỉ, giá thuê, giá điện/nước, tiện ích, quy định, thông tin người đăng tin<br>4. Người dùng có thể nhấn "Lưu tin", "Nhắn tin" hoặc "Đặt lịch xem phòng" |
| **Luồng ngoại lệ** | 2a. Phòng không còn tồn tại — thông báo lỗi và quay lại danh sách |

> **[Hình 2.8: Biểu đồ Use case Tìm kiếm và xem phòng trọ]**

> **[Hình 2.9: Biểu đồ hoạt động Tìm kiếm phòng trọ]**

> **[Hình 2.10: Biểu đồ tuần tự Xem chi tiết phòng trọ]**
> *(Các đối tượng: RoomDetailActivity — RoomViewModel — RoomRepository — Firestore)*

### 2.2.6 Chức năng tìm kiếm và lọc phòng trọ

**Bảng 2.7: Đặc tả Use case Tìm kiếm phòng trọ**

| Trường | Nội dung |
|---|---|
| **Tên use case** | Tìm kiếm phòng trọ |
| **Tác nhân chính** | Người dùng |
| **Mục đích** | Cho phép người dùng tìm kiếm phòng trọ theo nhiều tiêu chí |
| **Mức độ ưu tiên** | Bắt Buộc |
| **Điều kiện kích hoạt** | Khi người dùng nhập từ khóa hoặc chọn bộ lọc tìm kiếm |
| **Điều kiện tiên quyết** | Người dùng đã đăng nhập vào hệ thống |
| **Điều kiện thành công** | Hệ thống hiển thị danh sách phòng trọ phù hợp với tiêu chí tìm kiếm |
| **Điều kiện thất bại** | Không tìm thấy phòng phù hợp; hệ thống hiển thị thông báo "Không có kết quả" |
| **Luồng sự kiện chính** | 1. Người dùng nhấn vào ô tìm kiếm hoặc chọn khu vực phổ biến<br>2. Hệ thống hiển thị giao diện tìm kiếm với các bộ lọc<br>3. Người dùng nhập địa điểm, khoảng giá, tiện ích cần thiết<br>4. Hệ thống truy vấn Firestore lấy danh sách phòng phù hợp (phân trang 10 kết quả)<br>5. Hệ thống hiển thị danh sách phòng với thông tin tóm tắt |
| **Luồng ngoại lệ** | 4a. Không tìm thấy phòng phù hợp — hiển thị thông báo "Không có kết quả" |

### 2.2.7 Chức năng xem phòng trọ trên bản đồ

Từ màn hình chi tiết phòng trọ, người dùng có thể xem vị trí của phòng trọ trên Google Maps thông qua tọa độ latitude/longitude được lưu trong bài đăng. Bản đồ hiển thị marker tại vị trí phòng trọ, giúp người dùng quan sát khoảng cách đến các địa điểm quen thuộc (trường học, cơ quan, bến xe,...). Tính năng này được tích hợp vào màn hình chi tiết phòng và màn hình tìm kiếm theo khu vực.

### 2.2.8 Chức năng lưu phòng trọ yêu thích

**Bảng 2.8: Đặc tả Use case Lưu tin phòng trọ**

| Trường | Nội dung |
|---|---|
| **Tên use case** | Lưu tin phòng trọ |
| **Tác nhân chính** | Người dùng |
| **Mục đích** | Cho phép người dùng lưu phòng trọ yêu thích để xem lại sau |
| **Mức độ ưu tiên** | Quan Trọng |
| **Điều kiện kích hoạt** | Khi người dùng nhấn biểu tượng "Lưu" trên trang chi tiết phòng |
| **Điều kiện tiên quyết** | Người dùng đã đăng nhập; phòng đang được hiển thị công khai |
| **Điều kiện thành công** | Tin phòng được lưu vào danh sách yêu thích trong Firestore |
| **Điều kiện thất bại** | Tin không được lưu; hệ thống thông báo lỗi |
| **Luồng sự kiện chính** | 1. Người dùng nhấn biểu tượng lưu trên trang chi tiết phòng<br>2. Hệ thống tạo bản ghi lưu trong Firestore (collection `savedPosts`)<br>3. Hệ thống cập nhật giao diện hiển thị trạng thái đã lưu |
| **Luồng thay thế** | Người dùng nhấn lại biểu tượng lưu khi đã lưu — hệ thống xóa bản ghi (bỏ lưu) và cập nhật giao diện |

### 2.2.9 Chức năng liên hệ người cho thuê

**Bảng 2.9: Đặc tả Use case Gửi tin nhắn**

| Trường | Nội dung |
|---|---|
| **Tên use case** | Gửi tin nhắn |
| **Tác nhân chính** | Người dùng, Người đăng tin |
| **Mục đích** | Cho phép người dùng và người đăng tin nhắn tin trao đổi trực tiếp với nhau |
| **Mức độ ưu tiên** | Bắt Buộc |
| **Điều kiện kích hoạt** | Khi người dùng nhấn "Nhắn tin" trên trang chi tiết phòng hoặc chọn hội thoại có sẵn |
| **Điều kiện tiên quyết** | Người dùng đã đăng nhập |
| **Điều kiện thành công** | Tin nhắn được lưu thành công; hiển thị ngay lập tức trên cả hai thiết bị theo thời gian thực |
| **Điều kiện thất bại** | Tin nhắn không được gửi; hệ thống thông báo lỗi kết nối |
| **Luồng sự kiện chính** | 1. Người dùng mở cuộc hội thoại<br>2. Hệ thống tạo hoặc lấy ID hội thoại xác định giữa hai người dùng<br>3. Hệ thống hiển thị lịch sử tin nhắn theo thời gian thực<br>4. Người dùng nhập nội dung và nhấn gửi<br>5. Hệ thống lưu tin nhắn vào hội thoại<br>6. Hệ thống cập nhật tin nhắn cuối cùng và đếm tin chưa đọc |
| **Luồng thay thế** | Người dùng gửi hình ảnh — hệ thống tải ảnh lên Storage và lưu đường dẫn vào tin nhắn |

> **[Hình 2.11: Biểu đồ Use case Nhắn tin]**

> **[Hình 2.12: Biểu đồ hoạt động Nhắn tin]**

> **[Hình 2.13: Biểu đồ tuần tự Gửi tin nhắn]**
> *(Các đối tượng: ChatActivity — ChatViewModel — ChatRepository — Firestore)*

### 2.2.10 Chức năng đặt lịch hẹn xem phòng

**Bảng 2.10: Đặc tả Use case Đặt lịch hẹn xem phòng**

| Trường | Nội dung |
|---|---|
| **Tên use case** | Đặt lịch hẹn xem phòng |
| **Tác nhân chính** | Người dùng |
| **Mục đích** | Cho phép người dùng đặt lịch hẹn xem phòng trực tiếp với người đăng tin |
| **Mức độ ưu tiên** | Bắt Buộc |
| **Điều kiện kích hoạt** | Khi người dùng nhấn "Đặt lịch xem phòng" trên trang chi tiết phòng |
| **Điều kiện tiên quyết** | Người dùng đã đăng nhập; phòng đang được hiển thị công khai |
| **Điều kiện thành công** | Lịch hẹn được tạo thành công; người đăng tin nhận được thông báo đặt lịch mới |
| **Điều kiện thất bại** | Lịch hẹn không được tạo; khung giờ đã bị đặt hoặc xảy ra lỗi hệ thống |
| **Luồng sự kiện chính** | 1. Người dùng nhấn "Đặt lịch xem phòng"<br>2. Hệ thống hiển thị giao diện chọn ngày và giờ<br>3. Người dùng chọn ngày và khung giờ muốn đến xem<br>4. Hệ thống kiểm tra xung đột lịch hẹn tại khung giờ đó<br>5. Người dùng nhập ghi chú (nếu có) và xác nhận<br>6. Hệ thống tạo lịch hẹn ở trạng thái chờ xác nhận<br>7. Hệ thống ghi nhận khung giờ đã được đặt (collection `bookedSlots`)<br>8. Hệ thống gửi thông báo đến người đăng tin<br>9. Hệ thống thông báo đặt lịch thành công |
| **Luồng ngoại lệ** | 4a. Khung giờ đã có người đặt — thông báo và yêu cầu chọn khung giờ khác |

> **[Hình 2.14: Biểu đồ Use case Đặt lịch xem phòng]**

> **[Hình 2.15: Biểu đồ hoạt động Đặt lịch hẹn xem phòng]**

> **[Hình 2.16: Biểu đồ tuần tự Đặt lịch hẹn xem phòng]**
> *(Các đối tượng: BookingActivity — BookingViewModel — AppointmentRepository — Firestore — FCM)*

### 2.2.11 Chức năng quản lý lịch hẹn

**Bảng 2.11: Đặc tả Use case Xác nhận/Từ chối lịch hẹn**

| Trường | Nội dung |
|---|---|
| **Tên use case** | Xác nhận/Từ chối lịch hẹn |
| **Tác nhân chính** | Người đăng tin |
| **Mục đích** | Cho phép người đăng tin xác nhận hoặc từ chối yêu cầu đặt lịch từ người dùng |
| **Mức độ ưu tiên** | Bắt Buộc |
| **Điều kiện kích hoạt** | Người đăng tin nhận thông báo có lịch hẹn mới và vào xem |
| **Điều kiện tiên quyết** | Đã đăng nhập, là chủ sở hữu phòng được đặt lịch |
| **Điều kiện thành công** | Trạng thái lịch hẹn được cập nhật; người dùng nhận được thông báo về kết quả |
| **Điều kiện thất bại** | Trạng thái lịch hẹn không được cập nhật; hệ thống thông báo lỗi |
| **Luồng sự kiện chính** | 1. Người đăng tin vào mục "Lịch hẹn" và chọn lịch hẹn cần xử lý<br>2. Hệ thống hiển thị thông tin lịch hẹn: tên người dùng, ngày giờ, ghi chú<br>3. Người đăng tin nhấn "Xác nhận" hoặc "Từ chối"<br>4. Hệ thống cập nhật trạng thái lịch hẹn<br>5. Hệ thống gửi thông báo đến người dùng về kết quả |

**Bảng 2.12: Đặc tả Use case Hủy lịch hẹn**

| Trường | Nội dung |
|---|---|
| **Tên use case** | Hủy lịch hẹn |
| **Tác nhân chính** | Người dùng |
| **Mục đích** | Cho phép người dùng hủy lịch hẹn xem phòng đã đặt khi không còn nhu cầu |
| **Mức độ ưu tiên** | Quan Trọng |
| **Điều kiện kích hoạt** | Khi người dùng nhấn "Hủy lịch hẹn" trong danh sách lịch hẹn |
| **Điều kiện tiên quyết** | Người dùng đã đăng nhập; lịch hẹn đang ở trạng thái chờ xác nhận hoặc đã xác nhận |
| **Điều kiện thành công** | Lịch hẹn chuyển sang trạng thái đã hủy; khung giờ tương ứng được giải phóng |
| **Luồng sự kiện chính** | 1. Người dùng vào mục "Lịch hẹn" và chọn lịch hẹn cần hủy<br>2. Hệ thống hiển thị thông tin chi tiết lịch hẹn<br>3. Người dùng nhấn "Hủy lịch hẹn"<br>4. Hệ thống cập nhật trạng thái thành đã hủy<br>5. Hệ thống giải phóng khung giờ đã đặt trong `bookedSlots`<br>6. Hệ thống thông báo hủy thành công |

### 2.2.12 Chức năng đăng tin phòng trọ

**Bảng 2.13: Đặc tả Use case Đăng tin phòng trọ**

| Trường | Nội dung |
|---|---|
| **Tên use case** | Đăng tin phòng trọ |
| **Tác nhân chính** | Người đăng tin |
| **Mục đích** | Cho phép người đăng tin đăng phòng trọ lên hệ thống để người dùng tìm kiếm |
| **Mức độ ưu tiên** | Bắt Buộc |
| **Điều kiện kích hoạt** | Khi người đăng tin nhấn nút "Đăng tin" |
| **Điều kiện tiên quyết** | Người dùng đã đăng nhập và đã xác minh CCCD thành công |
| **Điều kiện thành công** | Tin đăng được tạo thành công ở trạng thái chờ xét duyệt |
| **Điều kiện thất bại** | Tin đăng không được tạo; người dùng vẫn ở form đăng tin với thông báo lỗi |
| **Luồng sự kiện chính** | 1. Người đăng tin nhấn "Đăng tin mới"<br>2. Hệ thống kiểm tra hạn mức đăng tin miễn phí (3 tin/24 giờ) hoặc slot đã mua<br>3. Hệ thống hiển thị form đăng tin<br>4. Người đăng tin nhập đầy đủ thông tin: địa chỉ, giá thuê, tiện ích, quy định, tải lên ảnh phòng<br>5. Người đăng tin nhấn "Đăng tin"<br>6. Hệ thống tạo bài đăng phòng với trạng thái chờ duyệt<br>7. Hệ thống tải ảnh lên Firebase Storage<br>8. Hệ thống thông báo đăng tin thành công và chờ quản trị viên duyệt |
| **Luồng ngoại lệ** | 2a. Đã hết hạn mức đăng tin miễn phí và không có slot — gợi ý mua thêm slot<br>4a. Thiếu thông tin bắt buộc — thông báo lỗi, yêu cầu điền đủ |

> **[Hình 2.17: Biểu đồ Use case Đăng và quản lý tin phòng trọ]**

> **[Hình 2.18: Biểu đồ hoạt động Đăng tin phòng trọ]**

> **[Hình 2.19: Biểu đồ tuần tự Đăng tin phòng trọ]**
> *(Các đối tượng: PostActivity — PostViewModel — RoomRepository — Firebase Storage — Firestore)*

### 2.2.13 Chức năng quản lý bài đăng phòng trọ

**Bảng 2.14: Đặc tả Use case Chỉnh sửa tin phòng trọ**

| Trường | Nội dung |
|---|---|
| **Tên use case** | Chỉnh sửa tin phòng trọ |
| **Tác nhân chính** | Người đăng tin |
| **Mục đích** | Cho phép người đăng tin cập nhật thông tin phòng trọ đã đăng |
| **Mức độ ưu tiên** | Quan Trọng |
| **Điều kiện kích hoạt** | Khi người đăng tin nhấn nút "Chỉnh sửa" trên tin đã đăng |
| **Điều kiện tiên quyết** | Đã đăng nhập, là chủ sở hữu của tin đăng |
| **Điều kiện thành công** | Thông tin tin đăng được cập nhật thành công trong Firestore |
| **Luồng sự kiện chính** | 1. Người đăng tin chọn tin cần sửa trong mục "Tin của tôi"<br>2. Hệ thống hiển thị form chỉnh sửa với thông tin hiện có<br>3. Người đăng tin cập nhật các thông tin cần thay đổi<br>4. Người đăng tin nhấn "Lưu thay đổi"<br>5. Hệ thống cập nhật dữ liệu trong Firestore |
| **Luồng ngoại lệ** | 3a. Thông tin nhập không hợp lệ — thông báo lỗi cụ thể |

**Bảng 2.15: Đặc tả Use case Xóa tin phòng trọ**

| Trường | Nội dung |
|---|---|
| **Tên use case** | Xóa tin phòng trọ |
| **Tác nhân chính** | Người đăng tin |
| **Mục đích** | Cho phép người đăng tin xóa vĩnh viễn tin đăng khỏi hệ thống |
| **Mức độ ưu tiên** | Quan Trọng |
| **Điều kiện tiên quyết** | Đã đăng nhập, là chủ sở hữu của tin đăng |
| **Điều kiện thành công** | Tin đăng bị xóa khỏi Firestore; không còn hiển thị trong kết quả tìm kiếm |
| **Luồng sự kiện chính** | 1. Người đăng tin chọn tin cần xóa trong mục "Tin của tôi"<br>2. Hệ thống hiển thị hộp thoại xác nhận xóa<br>3. Người đăng tin xác nhận xóa<br>4. Hệ thống xóa tài liệu phòng khỏi Firestore<br>5. Hệ thống cập nhật danh sách "Tin của tôi" |

**Bảng 2.16: Đặc tả Use case Đánh dấu phòng đã cho thuê**

| Trường | Nội dung |
|---|---|
| **Tên use case** | Đánh dấu phòng đã cho thuê |
| **Tác nhân chính** | Người đăng tin |
| **Mục đích** | Cho phép người đăng tin cập nhật trạng thái phòng khi đã tìm được người thuê |
| **Mức độ ưu tiên** | Quan Trọng |
| **Điều kiện thành công** | Trạng thái phòng được cập nhật thành đã cho thuê; phòng không còn nhận lịch hẹn mới; tất cả lịch hẹn đang chờ bị hủy tự động |
| **Luồng sự kiện chính** | 1. Người đăng tin nhấn "Đánh dấu đã cho thuê" trong chi tiết tin đăng<br>2. Hệ thống hiển thị hộp thoại xác nhận<br>3. Người đăng tin xác nhận<br>4. Hệ thống cập nhật trạng thái phòng<br>5. Hệ thống tự động hủy tất cả lịch hẹn đang ở trạng thái chờ xác nhận, đã xác nhận của phòng |

### 2.2.14 Chức năng đánh giá và bình luận phòng trọ

**Bảng 2.13b: Đặc tả Use case Gửi đánh giá phòng trọ**

| Trường | Nội dung |
|---|---|
| **Tên use case** | Gửi đánh giá phòng trọ |
| **Tác nhân chính** | Người dùng (người thuê) |
| **Mục đích** | Cho phép người thuê gửi đánh giá và nhận xét về chủ trọ sau khi đã liên hệ hoặc đặt lịch xem phòng |
| **Mức độ ưu tiên** | Quan Trọng |
| **Điều kiện kích hoạt** | Khi người dùng nhấn "Gửi đánh giá" trên trang chi tiết phòng hoặc từ danh sách lịch hẹn đã hoàn thành |
| **Điều kiện tiên quyết** | Người dùng đã đăng nhập; đã có lịch hẹn hoặc liên hệ với phòng trọ liên quan; không phải chủ sở hữu phòng trọ đó |
| **Điều kiện thành công** | Đánh giá được lưu vào Firestore ở trạng thái chờ duyệt; hiển thị thông báo gửi thành công |
| **Điều kiện thất bại** | Đánh giá không được lưu; hệ thống thông báo lỗi |
| **Luồng sự kiện chính** | 1. Người dùng chọn số sao đánh giá (1–5)<br>2. Người dùng nhập nhận xét (tùy chọn)<br>3. Người dùng nhấn "Gửi đánh giá"<br>4. Hệ thống kiểm tra điều kiện (đã đăng nhập, không tự đánh giá)<br>5. Hệ thống lưu đánh giá vào collection `reviews` với trạng thái "pending"<br>6. Hệ thống thông báo gửi thành công, chờ quản trị viên phê duyệt |
| **Luồng ngoại lệ** | 4a. Người dùng cố tự đánh giá phòng của mình — Firestore Security Rules từ chối và thông báo lỗi |

Đánh giá được lưu vào collection `reviews` với trạng thái chờ duyệt. Quản trị viên có thể ẩn hoặc xóa các đánh giá không phù hợp. Đánh giá được hiển thị công khai trên trang chi tiết phòng sau khi được phê duyệt.

### 2.2.15 Chức năng kiểm duyệt bài đăng

Quản trị viên kiểm duyệt bài đăng qua Web Admin Panel. Tại trang "Bài đăng", quản trị viên lọc theo trạng thái (chờ duyệt/đã duyệt/từ chối/đã cho thuê), xem chi tiết từng bài (ảnh, mô tả, giá, địa chỉ, thông tin chủ trọ) và thực hiện duyệt hoặc từ chối kèm lý do. Sau khi duyệt, tin đăng được hiển thị công khai và chủ trọ nhận thông báo FCM.

### 2.2.16 Chức năng quản lý người dùng

Quản trị viên quản lý toàn bộ tài khoản qua Web Admin Panel. Có thể tìm kiếm, lọc theo vai trò (tất cả/chưa xác minh/đã xác minh/quản trị viên), khóa tài khoản có thời hạn với lý do cụ thể, mở khóa, hoặc xóa vĩnh viễn tài khoản vi phạm. Hệ thống tự động mở khóa khi hết thời hạn khóa. Quản trị viên cũng có thể xuất danh sách người dùng ra file Excel và quét dọn tài khoản không hoạt động theo mốc thời gian (1/3/6/12 tháng).

### 2.2.17 Chức năng xác minh danh tính CCCD

**Bảng 2.17: Đặc tả Use case Xác minh danh tính CCCD**

| Trường | Nội dung |
|---|---|
| **Tên use case** | Xác minh danh tính CCCD |
| **Tác nhân chính** | Người dùng |
| **Mục đích** | Cho phép người dùng xác minh danh tính bằng CCCD để được cấp quyền đăng tin |
| **Mức độ ưu tiên** | Bắt Buộc |
| **Điều kiện kích hoạt** | Khi người dùng nhấn "Xác minh danh tính" trong hồ sơ cá nhân |
| **Điều kiện tiên quyết** | Người dùng đã đăng nhập, chưa xác minh |
| **Điều kiện thành công** | Hồ sơ xác minh được tạo thành công; hệ thống tiến hành kiểm tra OCR tự động |
| **Điều kiện thất bại** | Hồ sơ không được gửi; ảnh CCCD không hợp lệ hoặc xảy ra lỗi hệ thống |
| **Luồng sự kiện chính** | 1. Người dùng nhập số CCCD và nhấn tiếp tục<br>2. Hệ thống mở camera để chụp mặt trước thẻ CCCD<br>3. Hệ thống dùng ML Kit kiểm tra sơ bộ hình ảnh trên thiết bị<br>4. Hệ thống mở camera để chụp mặt sau thẻ CCCD<br>5. Hệ thống tải ảnh lên Firebase Storage<br>6. Hệ thống tạo hồ sơ xác minh ở trạng thái chờ duyệt<br>7. Cloud Function tự động phân tích ảnh bằng Google Cloud Vision<br>8. Nếu xác minh tự động thành công: cập nhật trạng thái đã duyệt, thông báo cho người dùng<br>9. Nếu không đủ điều kiện tự động: chuyển sang quản trị viên xét duyệt thủ công |
| **Luồng ngoại lệ** | 3a. Ảnh mờ, không nhận diện được CCCD — thông báo lỗi và yêu cầu chụp lại<br>7a. Thông tin OCR không khớp số CCCD đã nhập — tăng bộ đếm thất bại<br>7b. Thất bại 3 lần trong ngày — yêu cầu thử lại vào ngày hôm sau hoặc chờ quản trị viên duyệt |

> **[Hình 2.20: Biểu đồ Use case Xác minh danh tính CCCD]**

> **[Hình 2.21: Biểu đồ hoạt động Xác minh danh tính CCCD]**

> **[Hình 2.22: Biểu đồ tuần tự Xác minh danh tính CCCD]**
> *(Các đối tượng: VerificationActivity — VerificationRepository — Firebase Storage — Firestore — Cloud Function — Google Cloud Vision)*

### 2.2.18 Chức năng thống kê hệ thống

Quản trị viên xem thống kê tổng quan qua Dashboard của Web Admin Panel. Dashboard tải song song hai tập dữ liệu: (1) thống kê tổng hợp gồm tổng người dùng, tổng phòng trọ, số bài chờ duyệt, số xác minh chờ duyệt; (2) biểu đồ cột số bài đăng theo 6 tháng gần nhất và biểu đồ tròn phân loại người dùng theo trạng thái xác minh. Dữ liệu thống kê được cache phía client trong 5 phút để giảm số lần truy vấn Firestore. Dashboard còn hiển thị danh sách 5 bài đăng mới nhất chờ duyệt và 5 người dùng mới đăng ký để quản trị viên xử lý ngay.

### 2.2.19 Chức năng gửi yêu cầu hỗ trợ

**Bảng 2.18: Đặc tả Use case Gửi yêu cầu hỗ trợ**

| Trường | Nội dung |
|---|---|
| **Tên use case** | Gửi yêu cầu hỗ trợ |
| **Tác nhân chính** | Người dùng |
| **Mục đích** | Cho phép người dùng gửi phiếu yêu cầu hỗ trợ đến quản trị viên khi gặp vấn đề với ứng dụng hoặc tài khoản |
| **Mức độ ưu tiên** | Quan Trọng |
| **Điều kiện kích hoạt** | Khi người dùng nhấn "Hỗ trợ" trong mục cài đặt và tạo phiếu mới |
| **Điều kiện tiên quyết** | Người dùng đã đăng nhập |
| **Điều kiện thành công** | Phiếu hỗ trợ được tạo ở trạng thái đang mở; người dùng có thể theo dõi hội thoại hỗ trợ theo thời gian thực |
| **Điều kiện thất bại** | Phiếu hỗ trợ không được tạo; hệ thống thông báo lỗi |
| **Luồng sự kiện chính** | 1. Người dùng nhấn "Tạo phiếu hỗ trợ mới"<br>2. Hệ thống hiển thị biểu mẫu với các trường: danh mục, tiêu đề, mô tả chi tiết<br>3. Người dùng điền thông tin và nhấn "Gửi"<br>4. Nếu có ảnh đính kèm, hệ thống tải ảnh lên Firebase Storage<br>5. Hệ thống tạo phiếu hỗ trợ với trạng thái "new"<br>6. Hệ thống chuyển người dùng đến màn hình hội thoại hỗ trợ<br>7. Người dùng theo dõi phản hồi từ quản trị viên theo thời gian thực |
| **Luồng thay thế** | Người dùng gửi thêm tin nhắn vào phiếu đã tạo — tin nhắn được lưu vào lịch sử hội thoại của phiếu hỗ trợ tương ứng |

### 2.2.20 Chức năng thanh toán và nâng cấp dịch vụ

**Bảng 2.19: Đặc tả Use case Mua thêm lượt đăng bài**

| Trường | Nội dung |
|---|---|
| **Tên use case** | Mua thêm lượt đăng bài |
| **Tác nhân chính** | Người đăng tin |
| **Mục đích** | Cho phép người đăng tin mua thêm lượt đăng bài khi đã hết quota miễn phí |
| **Mức độ ưu tiên** | Quan Trọng |
| **Điều kiện kích hoạt** | Khi người dùng hết lượt đăng bài và nhấn "Mua thêm lượt" |
| **Điều kiện tiên quyết** | Người dùng đã đăng nhập và đã xác minh danh tính |
| **Điều kiện thành công** | Số lượt đăng bài được cộng thêm vào tài khoản; người dùng có thể tiếp tục đăng tin |
| **Điều kiện thất bại** | Giao dịch không hoàn thành; lượt đăng bài không được cộng thêm |
| **Luồng sự kiện chính** | 1. Hệ thống hiển thị cửa sổ chọn gói: +3 lượt (10.000đ), +5 lượt (20.000đ), +10 lượt (40.000đ)<br>2. Người dùng chọn gói phù hợp<br>3. Hệ thống tạo yêu cầu mua ở trạng thái chờ thanh toán<br>4. Hệ thống hiển thị mã QR VietQR kèm nội dung chuyển khoản duy nhất<br>5. Người dùng thực hiện chuyển khoản ngân hàng<br>6. Hệ thống tự động phát hiện giao dịch thành công qua SePay webhook<br>7. Hệ thống cộng số lượt đăng bài vào tài khoản |
| **Luồng ngoại lệ** | 6a. Hết 30 phút không thanh toán — yêu cầu hết hạn |

**Bảng 2.20: Đặc tả Use case Đẩy nổi bật tin đăng**

| Trường | Nội dung |
|---|---|
| **Tên use case** | Đẩy nổi bật tin đăng |
| **Tác nhân chính** | Người đăng tin |
| **Mục đích** | Cho phép người đăng tin trả phí để tin đăng được hiển thị nổi bật ở vị trí ưu tiên |
| **Mức độ ưu tiên** | Quan Trọng |
| **Điều kiện kích hoạt** | Khi người đăng tin nhấn "Đẩy nổi bật" trên tin đăng đang được hiển thị công khai |
| **Điều kiện tiên quyết** | Đã đăng nhập; là chủ sở hữu tin đăng; tin đang được hiển thị công khai; không có yêu cầu nổi bật đang hoạt động |
| **Điều kiện thành công** | Yêu cầu nổi bật được lưu ở trạng thái chờ quản trị viên xét duyệt |
| **Luồng sự kiện chính** | 1. Hệ thống hiển thị cửa sổ chọn gói: 3 ngày (10.000đ), 7 ngày (20.000đ), 15 ngày (40.000đ)<br>2. Người dùng chọn gói phù hợp<br>3. Hệ thống tạo yêu cầu nổi bật ở trạng thái chờ thanh toán<br>4. Hệ thống hiển thị mã QR VietQR<br>5. Người dùng thực hiện chuyển khoản<br>6. Hệ thống tự động xác nhận thanh toán, chuyển sang chờ quản trị viên duyệt<br>7. Quản trị viên phê duyệt → tin đăng được gắn nhãn nổi bật |
| **Luồng ngoại lệ** | 1a. Tin đã có yêu cầu nổi bật đang hoạt động — hệ thống thông báo và không cho tạo yêu cầu mới |

---

## 2.3 Mô hình dữ liệu

### 2.3.1 Mô hình thực thể liên hệ

Hệ thống Tìm Trọ 24/7 xây dựng trên nền tảng Cloud Firestore — cơ sở dữ liệu NoSQL dạng tài liệu của Google. Khác với cơ sở dữ liệu quan hệ truyền thống, Firestore không sử dụng bảng và khóa ngoại, mà tổ chức dữ liệu theo collection và document. Tuy nhiên, các thực thể và mối quan hệ nghiệp vụ vẫn được xác định rõ ràng trước khi chuyển sang mô hình lưu trữ NoSQL.

**Các thực thể chính:**
- **Người dùng (User):** Thông tin tài khoản, vai trò, trạng thái xác minh.
- **Phòng trọ (Room):** Thông tin chi tiết bài đăng, giá, tiện ích, trạng thái.
- **Lịch hẹn (Appointment):** Thông tin hẹn giữa người thuê và người đăng tin.
- **Tin nhắn (Chat/Message):** Hội thoại 1-1 giữa người dùng.
- **Xác minh (Verification):** Hồ sơ xác minh CCCD.
- **Thông báo (Notification):** Thông báo cá nhân gửi đến người dùng.
- **Đánh giá (Review):** Đánh giá của người thuê về chủ trọ.
- **Phiếu hỗ trợ (SupportTicket):** Yêu cầu hỗ trợ kỹ thuật.

**Các mối quan hệ chính:**
- Người dùng **đăng** nhiều Phòng trọ (1 - N)
- Người dùng **đặt** nhiều Lịch hẹn (1 - N)
- Phòng trọ **có** nhiều Lịch hẹn (1 - N)
- Người dùng **tham gia** nhiều Hội thoại (N - N)
- Người dùng **gửi** nhiều Đánh giá (1 - N)
- Phòng trọ **nhận** nhiều Đánh giá (1 - N)

> **[Hình 2.23: Sơ đồ mô hình dữ liệu tổng quan của ứng dụng]**
>
> *(Sơ đồ thể hiện quan hệ giữa các collection: users, rooms, chats/messages, appointments/bookedSlots, verifications, notifications, support_tickets/messages, reviews)*

### 2.3.2 Chuyển mô hình thực thể liên hệ thành mô hình dữ liệu Firebase

Việc chuyển đổi sang Firestore NoSQL có những điểm khác biệt quan trọng so với SQL:

- **Nhúng dữ liệu (Embedding):** Một số thông tin được sao chép (denormalize) vào document để tránh truy vấn phụ. Ví dụ: thông tin chủ trọ (ownerName, ownerPhone, ownerAvatarUrl) được sao chép vào document phòng trọ; tên người dùng được sao chép vào document đánh giá.
- **Subcollection:** Tin nhắn chat được tổ chức thành subcollection `messages` bên trong document `chats/{chatId}`, giúp phân trang và truy vấn hiệu quả.
- **Composite key:** Collection `bookedSlots` sử dụng khóa tổng hợp `{roomId}_{ngày}_{giờ}` làm Document ID, đảm bảo ràng buộc duy nhất ở tầng cơ sở dữ liệu — Firestore tự động từ chối ghi đè document đã tồn tại.
- **Tham chiếu bằng ID:** Thay vì khóa ngoại, các document tham chiếu nhau qua trường lưu ID (ví dụ: `userId`, `roomId`, `appointmentId`).

Lý do chọn Firestore NoSQL:
- **Đồng bộ thời gian thực** qua snapshot listeners — nền tảng cho chat, thông báo và lịch hẹn.
- **Mô hình linh hoạt** phù hợp với dữ liệu phòng trọ có số lượng trường biến động theo từng loại phòng.
- **Bảo mật phía server** qua Firestore Security Rules được đánh giá trước mọi thao tác đọc/ghi.
- **Phân trang con trỏ** giúp tải danh sách phòng 10 bản ghi mỗi lần không cần logic phức tạp.

### 2.3.3 Bảng dữ liệu

**Bảng 2.21: Collection `users` — Thông tin tài khoản người dùng**

| Trường | Kiểu dữ liệu | Mô tả |
|---|---|---|
| uid | String | Mã định danh duy nhất (Firebase Auth UID) |
| fullName | String | Họ và tên đầy đủ |
| email | String | Địa chỉ email |
| phone | String | Số điện thoại (10 chữ số) |
| address | String | Địa chỉ thường trú |
| birthday | String | Ngày sinh |
| gender | String | Giới tính |
| occupation | String | Nghề nghiệp |
| avatarUrl | String | URL ảnh đại diện trên Cloud Storage |
| fcmToken | String | Token FCM của thiết bị; cập nhật tự động mỗi khi token thay đổi |
| lastLoginAt | Long | Thời điểm đăng nhập lần cuối (timestamp) |
| role | String | Vai trò: "user" hoặc "admin" |
| isVerified | Boolean | Đã xác minh CCCD thành công; khi `true` được phép đăng tin |
| hasAcceptedRules | Boolean | Đã đồng ý với nội quy sử dụng ứng dụng |
| isLocked | Boolean | Tài khoản đang bị khóa |
| lockReason | String | Lý do khóa tài khoản |
| lockUntil | Long | Thời điểm tự động mở khóa (timestamp) |
| postingUnlockAt | Long | Thời điểm mở khóa quyền đăng tin (timestamp) |
| verifiedAt | Long | Thời điểm xác minh danh tính thành công (timestamp) |
| purchasedSlots | Int | Số lượt đăng tin đã mua thêm |
| createdAt | Long | Thời điểm tạo tài khoản (timestamp) |

**Bảng 2.22: Collection `rooms` — Tin đăng phòng trọ**

| Trường | Kiểu dữ liệu | Mô tả |
|---|---|---|
| id | String | Mã định danh tin đăng |
| userId | String | UID của chủ trọ |
| title | String | Tiêu đề tin đăng |
| description | String | Mô tả chi tiết phòng trọ |
| address | String | Địa chỉ cụ thể |
| ward | String | Phường/xã |
| district | String | Quận/huyện |
| latitude | Double? | Vĩ độ GPS |
| longitude | Double? | Kinh độ GPS |
| price | Long | Giá thuê hàng tháng (VNĐ) |
| area | Int | Diện tích phòng (m²) |
| roomType | String | Loại phòng (phòng trọ, chung cư mini, nhà nguyên căn,...) |
| electricPrice | Long | Giá điện (VNĐ/kWh) |
| waterPrice | Long | Giá nước (VNĐ/m³) |
| wifiPrice | Long | Giá wifi hàng tháng (VNĐ) |
| depositMonths | Int | Số tháng đặt cọc |
| depositAmount | Long | Số tiền đặt cọc (VNĐ) |
| peopleCount | Int | Số người hiện đang ở |
| roomCount | Int | Tổng số phòng trong khu trọ |
| rentedCount | Int | Số phòng đã cho thuê |
| gender | String | Giới tính được phép thuê (nam/nữ/hỗn hợp) |
| hasWifi | Boolean | Có wifi |
| hasAirCon | Boolean | Có điều hòa |
| hasParking | Boolean | Có chỗ để xe |
| hasWaterHeater | Boolean | Có máy nước nóng |
| hasWasher | Boolean | Có máy giặt |
| hasDryingArea | Boolean | Có chỗ phơi đồ |
| hasWardrobe | Boolean | Có tủ quần áo |
| hasBed | Boolean | Có giường |
| kitchen | String | Loại bếp (không có/bếp chung/bếp riêng) |
| bathroom | String | Loại phòng tắm (không có/chung/riêng) |
| pet | String | Cho phép nuôi thú cưng: "Không", "Có", hoặc mô tả |
| curfew | String | Có giờ giới nghiêm hoặc mô tả quy định |
| hasMotorbike | Boolean | Có chỗ gửi xe máy |
| motorbikeFee | Long | Phí gửi xe máy hàng tháng |
| hasEBike | Boolean | Có chỗ sạc/gửi xe điện |
| eBikeFee | Long | Phí gửi xe điện hàng tháng |
| hasBicycle | Boolean | Có chỗ gửi xe đạp |
| bicycleFee | Long | Phí gửi xe đạp hàng tháng |
| ownerName | String | Tên chủ trọ hiển thị trên tin đăng |
| ownerPhone | String | Số điện thoại liên hệ của chủ trọ |
| ownerGender | String | Giới tính chủ trọ |
| ownerAvatarUrl | String | URL ảnh đại diện chủ trọ |
| status | String | Trạng thái: "pending", "approved", "rejected", "expired" |
| isFeatured | Boolean | Tin đang ở vị trí nổi bật |
| featuredUntil | Long | Thời điểm hết hạn nổi bật (timestamp) |
| imageUrls | List\<String\> | Danh sách URL ảnh phòng |
| createdAt | Long | Thời điểm đăng tin (timestamp) |

**Bảng 2.23: Collection `chats` và subcollection `messages` — Hệ thống nhắn tin**

| Trường (chats) | Kiểu dữ liệu | Mô tả |
|---|---|---|
| chatId | String | ID xác định duy nhất từ 2 UID sắp xếp theo bảng chữ cái |
| participants | List\<String\> | Danh sách UID của 2 người tham gia |
| participantNames | Map\<String, String\> | Tên hiển thị của từng người tham gia |
| participantAvatars | Map\<String, String\> | URL ảnh đại diện của từng người tham gia |
| lastMessage | String | Nội dung tin nhắn cuối cùng |
| lastMessageAt | Long | Thời điểm gửi tin nhắn cuối (timestamp) |
| lastSenderId | String | UID người gửi tin nhắn cuối |
| unreadCount | Map\<String, Long\> | Số tin chưa đọc theo từng người |

| Trường (messages) | Kiểu dữ liệu | Mô tả |
|---|---|---|
| id | String | Mã định danh tin nhắn |
| senderId | String | UID người gửi |
| text | String | Nội dung văn bản |
| imageUrl | String | URL hình ảnh đính kèm |
| seen | Boolean | Đã được đọc |
| reactions | Map\<String, String\> | Cảm xúc: userId → emoji |
| createdAt | Long | Thời điểm gửi (timestamp) |
| deletedFor | Map\<String, Long\> | Dấu thời gian xóa theo từng người dùng |

**Bảng 2.24: Collection `appointments` và `bookedSlots` — Lịch hẹn xem phòng**

| Trường (appointments) | Kiểu dữ liệu | Mô tả |
|---|---|---|
| id | String | Mã định danh lịch hẹn |
| tenantId | String | UID người thuê |
| landlordId | String | UID chủ trọ |
| roomId | String | ID phòng trọ |
| date | String | Ngày hẹn (định dạng yyyy-MM-dd) |
| dateDisplay | String | Chuỗi ngày đã định dạng để hiển thị |
| time | String | Giờ hẹn |
| note | String | Ghi chú thêm từ người thuê |
| status | String | Trạng thái: "pending", "confirmed", "tenant_confirmed", "completed_rented", "rejected", "cancelled_by_tenant", "cancelled_by_system" |
| hasUnreadUpdate | Boolean | Có cập nhật trạng thái chưa được xem |
| createdAt | Long | Thời điểm tạo lịch hẹn (timestamp) |

| Trường (bookedSlots) | Kiểu dữ liệu | Mô tả |
|---|---|---|
| roomId | String | ID phòng trọ được đặt lịch |
| appointmentId | String | ID lịch hẹn tương ứng |
| date | String | Ngày đặt (định dạng yyyy-MM-dd) |
| time | String | Giờ đặt lịch |
| status | String | Trạng thái slot: "pending", "confirmed", "cancelled" |

**Bảng 2.25: Collection `verifications` — Hồ sơ xác minh CCCD**

| Trường | Kiểu dữ liệu | Mô tả |
|---|---|---|
| userId | String | UID người dùng |
| fullName | String | Họ tên đầy đủ (sao chép tại thời điểm nộp) |
| cccdNumber | String | Số CCCD do người dùng nhập |
| cccdFrontUrl | String | URL ảnh mặt trước CCCD trên Cloud Storage |
| cccdBackUrl | String | URL ảnh mặt sau CCCD trên Cloud Storage |
| status | String | Trạng thái: "pending", "approved", "rejected" |
| autoCheckStatus | String | Kết quả kiểm tra OCR tự động: "pass", "fail", "manual_review" |
| autoCheckReason | String | Lý do cụ thể nếu OCR thất bại |
| autoCheckRecognizedCccd | String | Số CCCD mà hệ thống OCR nhận dạng được từ ảnh |
| autoFailCountToday | Int | Số lần kiểm tra OCR thất bại trong ngày |
| escalatedToAdmin | Boolean | Hồ sơ đã được chuyển sang quản trị viên xét duyệt thủ công |
| createdAt | Long | Thời điểm nộp hồ sơ (timestamp) |
| updatedAt | Long | Thời điểm cập nhật lần cuối (timestamp) |

**Bảng 2.26: Collection `notifications` — Thông báo**

| Trường | Kiểu dữ liệu | Mô tả |
|---|---|---|
| userId | String | UID người nhận |
| title | String | Tiêu đề thông báo |
| message | String | Nội dung thông báo |
| type | String | Loại: "appointment_new", "appointment_accepted", "new_message", "verification_result",... |
| seen | Boolean | Người dùng đã xem thông báo |
| createdAt | Long | Thời điểm tạo (timestamp) |

**Bảng 2.27: Collection `support_tickets` và subcollection `messages` — Hỗ trợ người dùng**

| Trường (support_tickets) | Kiểu dữ liệu | Mô tả |
|---|---|---|
| id | String | Mã định danh phiếu hỗ trợ |
| userId | String | UID người gửi |
| userName | String | Tên người gửi (sao chép tại thời điểm tạo phiếu) |
| userEmail | String | Email người gửi |
| category | String | Danh mục vấn đề |
| title | String | Tiêu đề yêu cầu |
| status | String | Trạng thái: "new", "in_progress", "resolved", "closed" |
| priority | String | Mức độ ưu tiên: "normal" hoặc "high" |
| lastMessage | String | Nội dung tin nhắn cuối cùng |
| unreadForUser | Boolean | Có phản hồi mới từ quản trị viên chưa đọc |
| unreadForAdmin | Boolean | Có tin nhắn mới từ người dùng chưa đọc |
| createdAt | Long | Thời điểm tạo phiếu (timestamp) |

**Bảng 2.28: Collection `reviews` — Đánh giá chủ trọ**

| Trường | Kiểu dữ liệu | Mô tả |
|---|---|---|
| userId | String | UID người thuê gửi đánh giá |
| userName | String | Tên người thuê |
| landlordId | String | UID chủ trọ được đánh giá |
| roomId | String | ID phòng trọ liên quan |
| roomTitle | String | Tiêu đề phòng trọ |
| rating | Float | Điểm đánh giá từ 1 đến 5 sao |
| comment | String | Nhận xét của người thuê |
| status | String | Trạng thái: "approved" (công khai), "hidden" (đã ẩn bởi admin) |
| createdAt | Long | Thời điểm gửi đánh giá (timestamp) |

### 2.3.4 Mô hình quan hệ tổng quan

Mặc dù Firestore là cơ sở dữ liệu NoSQL không có quan hệ ràng buộc cứng, hệ thống vẫn duy trì tính nhất quán dữ liệu thông qua các quy tắc nghiệp vụ và Security Rules. Sơ đồ quan hệ tổng quan thể hiện cách các collection liên kết với nhau:

- `users` ← (userId) → `rooms`: một người dùng đăng nhiều phòng
- `users` ← (tenantId, landlordId) → `appointments`: người thuê và chủ trọ tham gia lịch hẹn
- `rooms` ← (roomId) → `appointments`: một phòng có nhiều lịch hẹn
- `appointments` ← (appointmentId) → `bookedSlots`: mỗi lịch hẹn chiếm một khung giờ
- `users` ← (participants) → `chats`: hai người dùng tạo một kênh chat
- `users` ← (userId) → `verifications`: một người dùng có một hồ sơ xác minh
- `users` ← (userId, landlordId) → `reviews`: người thuê đánh giá chủ trọ
- `users` ← (userId) → `notifications`: thông báo cá nhân cho từng người dùng

> **[Hình 2.23: Sơ đồ mô hình dữ liệu tổng quan của ứng dụng]**

### Tổng kết Chương 2

Chương 2 đã trình bày toàn bộ mô hình chức năng và mô hình dữ liệu của hệ thống Tìm Trọ 24/7. Phần 2.1 xác định quy tắc nghiệp vụ, tác nhân và danh sách 18 chức năng chính. Phần 2.2 đặc tả chi tiết từng chức năng thông qua bảng use case, biểu đồ hoạt động và biểu đồ tuần tự. Phần 2.3 trình bày mô hình dữ liệu Firestore với 8 collection chính, giải thích lý do chọn NoSQL và cách chuyển đổi từ mô hình thực thể liên hệ sang cấu trúc document. Những thiết kế này là cơ sở để Chương 3 triển khai kiến trúc phần mềm và cài đặt thực tế ứng dụng.

---

# CHƯƠNG 3: MÔ HÌNH PHẦN MỀM VÀ TRIỂN KHAI ỨNG DỤNG

## 3.1 Tổng quan kiến trúc hệ thống

### 3.1.1 Mô hình Client – Firebase

Hệ thống Tìm Trọ 24/7 được xây dựng dựa trên mô hình **Client – Firebase (BaaS)**, trong đó:

- **Ứng dụng di động Android** đóng vai trò Client cho Người dùng và Người đăng tin, giao tiếp trực tiếp với các dịch vụ Firebase thông qua Firebase Android SDK.
- **Bảng điều khiển Web Admin** đóng vai trò Client dành riêng cho Quản trị viên, truy cập Firebase qua Firebase JS SDK và được triển khai trên Firebase Hosting.
- **Firebase Platform** đóng vai trò Backend-as-a-Service, cung cấp toàn bộ hạ tầng: xác thực, lưu trữ dữ liệu, lưu trữ tệp, thông báo đẩy và thực thi logic phía máy chủ.

Mô hình này loại bỏ nhu cầu xây dựng và quản lý máy chủ riêng, giúp tập trung hoàn toàn vào phát triển tính năng ứng dụng.

### 3.1.2 Kiến trúc tổng thể hệ thống

> **[Hình 3.1: Sơ đồ kiến trúc hệ thống tổng thể]**

**Bảng 3.1: Mô tả các thành phần công nghệ trong kiến trúc hệ thống**

| Phân hệ / Thành phần | Công nghệ triển khai | Vai trò và Chức năng kỹ thuật |
|---|---|---|
| **Ứng dụng di động Android** | Kotlin, MVVM, LiveData, ViewBinding, CameraX, Glide | Giao diện máy khách di động cho người dùng và chủ trọ. Thực hiện tìm kiếm, đăng tin, quản lý lịch hẹn, OCR cục bộ và nhắn tin thời gian thực. |
| **Bảng điều khiển Web Admin** | JavaScript, Firebase Web SDK, Chart.js, XLSX, Firebase Hosting | Bảng điều khiển quản trị hệ thống. Thực hiện thống kê trực quan, kiểm duyệt bài đăng, duyệt hồ sơ xác minh CCCD và xuất dữ liệu Excel. |
| **Dịch vụ xác thực tài khoản** | Firebase Authentication | Xác thực, phân quyền và duy trì phiên đăng nhập an toàn. |
| **Cơ sở dữ liệu thời gian thực** | Cloud Firestore | Cơ sở dữ liệu NoSQL document. Lưu trữ và đồng bộ tức thì: phòng trọ, người dùng, tin nhắn, lịch hẹn. |
| **Kho lưu trữ tệp đám mây** | Firebase Storage | Lưu trữ ảnh phòng trọ, ảnh đại diện và ảnh CCCD với phân quyền bảo mật. |
| **Hàm xử lý phía máy chủ** | Firebase Cloud Functions (Node.js) | Tự động xác minh CCCD qua Cloud Vision, kết nối thanh toán SePay, gửi thông báo FCM. |
| **Dịch vụ thông báo đẩy** | Firebase Cloud Messaging | Gửi thông báo thời gian thực từ đám mây đến thiết bị Android. |
| **Dịch vụ nhận dạng đám mây** | Google Cloud Vision API | OCR đám mây nâng cao để phân tích ảnh CCCD, trích xuất dữ liệu đối khớp số và họ tên. |
| **Cổng thanh toán tự động** | SePay Webhook API | Đối khớp giao dịch ngân hàng theo cú pháp chuyển khoản để kích hoạt nâng cấp tin đăng. |
| **Trợ lý ảo thông minh** | Thuật toán phân tích tần suất từ khóa (Kotlin, on-device) | Trợ lý AI tư vấn phòng trọ, xử lý trực tiếp trên thiết bị không cần gọi dịch vụ ngoài. |

### 3.1.3 Giao tiếp giữa ứng dụng di động và Firebase

- Ứng dụng gửi yêu cầu đăng nhập/đăng ký đến **Firebase Authentication** và nhận ID Token để xác thực các yêu cầu tiếp theo.
- Ứng dụng đọc/ghi dữ liệu phòng trọ, lịch hẹn, tin nhắn từ **Cloud Firestore** thông qua Firebase SDK với Firestore Security Rules kiểm soát phân quyền.
- Ứng dụng tải ảnh phòng trọ và ảnh CCCD lên **Firebase Storage**; URL ảnh sau khi tải lên được lưu vào Firestore.
- Ứng dụng nhận thông báo lịch hẹn, kết quả duyệt bài và xác minh CCCD qua **Firebase Cloud Messaging**.
- Một số tác vụ nặng (OCR CCCD, xử lý thanh toán SePay) được ủy quyền cho **Cloud Functions** thực thi phía máy chủ.

### 3.1.4 Đồng bộ dữ liệu thời gian thực

Ứng dụng sử dụng cơ chế **snapshot listener** của Firestore để đăng ký nhận cập nhật tức thì khi dữ liệu thay đổi, không cần polling định kỳ:

- **Cập nhật trạng thái lịch hẹn theo thời gian thực:** Khi chủ trọ xác nhận hoặc từ chối, người thuê thấy trạng thái thay đổi ngay lập tức.
- **Cập nhật tin nhắn chat:** Tin nhắn mới xuất hiện tức thì trên cả hai thiết bị.
- **Cập nhật danh sách phòng trọ:** Khi tin đăng được duyệt, phòng xuất hiện ngay trong kết quả tìm kiếm.
- **Cập nhật trạng thái kiểm duyệt bài đăng:** Người đăng tin thấy trạng thái "Đã duyệt" ngay khi quản trị viên phê duyệt.

---

## 3.2 Thiết kế và triển khai Firebase

### 3.2.1 Thiết kế Firebase Authentication

- **Phương thức xác thực:** Đăng ký và đăng nhập bằng email/mật khẩu.
- **Yêu cầu mật khẩu:** Tối thiểu 12 ký tự, bao gồm chữ hoa, chữ số và ký tự đặc biệt.
- **Quản lý phiên đăng nhập:** Firebase SDK tự động duy trì phiên đăng nhập và làm mới token. Ứng dụng kiểm tra trạng thái tài khoản (bị khóa hay không) sau mỗi lần đăng nhập thành công.
- **Quên mật khẩu:** Gửi email đặt lại mật khẩu qua Firebase Auth; xác minh thêm số điện thoại trong Firestore trước khi gửi.
- **Phân quyền theo vai trò:** Vai trò (user/admin) và quyền đăng tin (isVerified) được lưu trong Firestore, không trong Firebase Auth token, đảm bảo phân quyền động.

### 3.2.2 Thiết kế Cloud Firestore

> **[Hình 3.2: Sơ đồ tổ chức collection và subcollection trong Firestore]**

**Bảng 3.2: Các collection trong Cloud Firestore**

| Tên & Đường dẫn | Quy tắc Document ID | Mô tả |
|---|---|---|
| `users/{uid}` | uid — ID từ Firebase Auth | Hồ sơ người dùng: tên, email, số điện thoại, vai trò, trạng thái xác minh, số slot đăng tin |
| `rooms/{roomId}` | roomId — ID do hệ thống tự sinh | Tin đăng phòng trọ: địa chỉ, giá, diện tích, tiện ích, ảnh, trạng thái duyệt |
| `savedPosts/{docId}` | Ghép từ uid và roomId | Tin đăng đã lưu; mỗi cặp người dùng–phòng chỉ lưu một lần |
| `appointments/{appointmentId}` | ID do hệ thống tự sinh | Lịch hẹn xem phòng: người dùng, chủ trọ, phòng, thời gian, trạng thái |
| `bookedSlots/{slotId}` | Khóa tổng hợp `{roomId}_{ngày}_{giờ}` | Khung giờ đã đặt; Document ID là khóa tổng hợp ngăn đặt trùng ở tầng cơ sở dữ liệu |
| `verifications/{uid}` | uid — trùng với ID người dùng | Hồ sơ xác minh CCCD: ảnh hai mặt, họ tên, trạng thái duyệt |
| `cccd_registry/{cccdNumber}` | Chính là số CCCD | Đăng ký số CCCD đã dùng; ngăn một CCCD tạo nhiều tài khoản |
| `notifications/{notifId}` | ID do hệ thống tự sinh | Thông báo cá nhân: loại, tiêu đề, nội dung, trạng thái đã đọc |
| `chats/{chatId}` | Ghép hai uid theo bảng chữ cái | Kênh chat 1-1 giữa hai người dùng |
| `chats/{chatId}/messages/{msgId}` | ID do hệ thống tự sinh | Tin nhắn trong kênh chat |
| `support_tickets/{ticketId}` | ID do hệ thống tự sinh | Phiếu hỗ trợ kỹ thuật |
| `support_tickets/{ticketId}/messages/{msgId}` | ID do hệ thống tự sinh | Tin nhắn trao đổi trong phiếu hỗ trợ |
| `slot_upgrade_requests/{requestId}` | ID do hệ thống tự sinh | Yêu cầu mua slot đăng tin: số lượng, số tiền, trạng thái thanh toán |
| `featured_upgrade_requests/{requestId}` | ID do hệ thống tự sinh | Yêu cầu đẩy tin nổi bật: phòng, thời hạn, số tiền, trạng thái |
| `system_notifications/{docId}` | ID do hệ thống tự sinh | Thông báo hệ thống gửi toàn bộ người dùng qua FCM |
| `stats/popular_areas` | `popular_areas` — document tĩnh | Thống kê số lượng phòng theo quận/huyện; Cloud Function tự động cập nhật |
| `reviews/{reviewId}` | ID do hệ thống tự sinh | Đánh giá của người dùng dành cho người đăng tin |
| `phone_registry/{phone}` | Chính là số điện thoại | Đăng ký số điện thoại đã dùng; ngăn đăng ký nhiều tài khoản |

### 3.2.3 Thiết kế Firebase Storage

- **`avatars/{uid}`:** Ảnh đại diện người dùng — đọc công khai, ghi chỉ chính chủ; tối đa 5 MB.
- **`rooms/{roomId}`:** Ảnh phòng trọ — đọc công khai, ghi chỉ chủ sở hữu phòng; tối đa 15 MB.
- **`verifications/{uid}`:** Ảnh CCCD — đọc/ghi chỉ chính chủ và quản trị viên; tối đa 10 MB.
- **`chat_images/{chatId}`:** Ảnh trong kênh chat — đọc/ghi chỉ người dùng đã đăng nhập; tối đa 15 MB.
- **`support_images/{ticketId}`:** Ảnh phiếu hỗ trợ — đọc/ghi chỉ chủ phiếu và quản trị viên; tối đa 10 MB.
- Mọi đường dẫn không được khai báo tường minh đều bị từ chối theo nguyên tắc mặc định từ chối.

### 3.2.4 Thiết kế Firebase Cloud Messaging

- **Thông báo lịch hẹn:** Gửi FCM đến người đăng tin khi người dùng đặt lịch hẹn mới.
- **Kết quả xác nhận/từ chối:** Gửi FCM đến người dùng khi người đăng tin xác nhận hoặc từ chối lịch hẹn.
- **Kết quả kiểm duyệt bài đăng:** Gửi FCM đến người đăng tin khi quản trị viên duyệt hoặc từ chối tin.
- **Kết quả xác minh CCCD:** Gửi FCM đến người dùng khi hồ sơ xác minh được duyệt hoặc từ chối.
- **Thông báo hàng loạt (Broadcast):** Quản trị viên gửi FCM đến tất cả thiết bị đã đăng ký.
- FCM token được cập nhật tự động mỗi khi token thiết bị thay đổi và lưu vào trường `fcmToken` trong Firestore.

### 3.2.5 Thiết kế Firebase Security Rules

**a) Các điều kiện kiểm tra dùng chung**

Hệ thống định nghĩa các hàm kiểm tra tái sử dụng: `isSignedIn()` (kiểm tra đăng nhập), `isAdmin()` (kiểm tra quyền quản trị viên), `canPostRoom()` (kiểm tra quyền đăng tin — đã đăng nhập + đã xác minh + không trong thời hạn đình chỉ), `isChatParticipant(chatId)` (kiểm tra thành viên hội thoại).

> **[Hình 3.3: Sơ đồ phân quyền truy cập Firestore theo vai trò]**

**b) Quy tắc phân quyền theo collection**

**Bảng 3.3: Quy tắc bảo mật Firestore theo từng collection (tóm tắt)**

| Collection | Quyền Đọc | Quyền Ghi |
|---|---|---|
| `users` | Chính chủ hoặc Quản trị viên | Quản trị viên: toàn quyền. Chính chủ: tạo tài khoản, cập nhật thông tin cơ bản; cấm tự cấp quyền xác minh hoặc thay đổi vai trò |
| `rooms` | Công khai với tin đã duyệt; chính chủ và quản trị viên xem mọi trạng thái | Quản trị viên: toàn quyền. Người đăng tin: tạo tin (mặc định pending), sửa tin chính chủ, xóa tin |
| `appointments` | Các bên liên quan hoặc quản trị viên | Tạo lịch hẹn (mặc định pending), cập nhật trạng thái |
| `bookedSlots` | Tất cả người dùng đã đăng nhập | Người liên quan đến lịch hẹn; quản trị viên toàn quyền |
| `chats` | Thành viên tham gia | Tạo kênh chat mới (phải là thành viên); thành viên cập nhật/xóa |
| `verifications` | Chính chủ hoặc quản trị viên | Chính chủ: tạo hồ sơ, cập nhật khi đang chờ duyệt; quản trị viên: toàn quyền |
| `notifications` | Chính chủ nhận thông báo hoặc quản trị viên | Người dùng đã đăng nhập: tạo thông báo cho người khác; chính chủ: cập nhật trạng thái đã đọc |
| `reviews` | Công khai | Người dùng: tạo đánh giá (cấm tự đánh giá); quản trị viên: ẩn/xóa |

**Bảng 3.4: Quy tắc bảo mật Firebase Storage theo đường dẫn**

| Đường dẫn | Quyền đọc | Quyền ghi | Ràng buộc tệp |
|---|---|---|---|
| `avatars/{uid}` | Công khai | Chính chủ hoặc quản trị viên | Ảnh, tối đa 5 MB |
| `rooms/{roomId}` | Công khai | Chủ sở hữu phòng hoặc quản trị viên | Ảnh, tối đa 15 MB |
| `verifications/{uid}` | Chính chủ hoặc quản trị viên | Chính chủ hoặc quản trị viên | Ảnh, tối đa 10 MB |
| `chat_images/{chatId}` | Đã đăng nhập | Đã đăng nhập | Ảnh, tối đa 15 MB |
| `support_images/{ticketId}` | Chủ phiếu hoặc quản trị viên | Chủ phiếu hoặc quản trị viên | Ảnh, tối đa 10 MB |
| (mặc định) | Từ chối | Từ chối | — |

> **[Hình 3.4: Sơ đồ luồng xác minh danh tính và phân quyền Storage]**

---

## 3.3 Thiết kế ứng dụng di động

### 3.3.1 Cấu trúc ứng dụng di động

Ứng dụng Android được tổ chức theo kiến trúc **MVVM** kết hợp **Repository Pattern**, phân tách rõ ràng thành bốn tầng:

1. **Tầng hiển thị (View):** Activity và Fragment chịu trách nhiệm hiển thị giao diện, thu nhận tương tác người dùng và quan sát LiveData từ ViewModel.
2. **Tầng điều phối logic (ViewModel):** Quản lý trạng thái giao diện và điều khiển luồng dữ liệu; không giữ tham chiếu đến View, bảo toàn dữ liệu khi xoay màn hình.
3. **Tầng trừu tượng hóa dữ liệu (Repository):** Điểm tiếp xúc duy nhất với Firebase SDK; ViewModel chỉ gọi Repository mà không biết dữ liệu lấy từ đâu.
4. **Tầng thực thể dữ liệu (Model):** Các data class Kotlin ánh xạ trực tiếp với document Firestore.

**Bảng 3.6: Cấu trúc mã nguồn ứng dụng Android theo package**

| Package | Các lớp/tệp chính | Vai trò |
|---|---|---|
| Model | User.kt, Room.kt, Message.kt, Conversation.kt, SupportTicket.kt | Các lớp dữ liệu Kotlin ánh xạ với tài liệu Firestore |
| View (Activity) | SplashActivity, LoginActivity, RegisterActivity, MainActivity, RoomDetailActivity, BookingActivity, ChatActivity, VerifyLandlordActivity, MyPostsActivity, NotificationsActivity, SupportTicketsActivity và 22 Activity khác | Lớp View trong MVVM; hiển thị giao diện, quan sát LiveData từ ViewModel |
| ViewModel | AuthViewModel, HomeViewModel, PostViewModel, BookingViewModel, ChatViewModel và 17 ViewModel khác | Quản lý trạng thái giao diện và logic nghiệp vụ; truyền dữ liệu qua LiveData |
| Repository | AuthRepository, RoomRepository, ChatRepository, AppointmentRepository, UserRepository, VerificationRepository, SupportRepository | Tầng dữ liệu duy nhất tương tác với Firebase SDK |
| Utils | MyFirebaseMessagingService, PresenceManager, ImageUtils, LocationNormalizer, NumberFormatUtils, AddressData | Tiện ích dùng chung: định dạng số, chuẩn hóa địa chỉ, xử lý FCM |

**Cấu trúc bảng điều khiển Web Admin:**

**Bảng 3.7: Cấu trúc mã nguồn Web Admin và Cloud Functions**

| Tệp | Vị trí | Chức năng |
|---|---|---|
| 00-globals.js | public/js/ | Biến toàn cục và cấu hình phân trang |
| 01-firebase-config.js | public/js/ | Khởi tạo kết nối Firebase |
| 02-helpers.js | public/js/ | Hàm tiện ích: định dạng dữ liệu, toast, hộp thoại xác nhận |
| 03-auth.js | public/js/ | Xử lý xác thực quản trị viên và điều hướng trang |
| 04-navigation.js | public/js/ | Điều hướng giữa các trang trong bảng điều khiển |
| 05-dashboard.js | public/js/ | Tải thống kê và vẽ biểu đồ |
| 06-verifications.js | public/js/ | Quản lý yêu cầu xác minh CCCD |
| 07-posts.js | public/js/ | Quản lý tin đăng phòng trọ và thanh toán |
| 08-users.js | public/js/ | Quản lý người dùng, khóa/mở khóa tài khoản |
| 09-exports.js | public/js/ | Xuất dữ liệu ra file Excel |
| 11-support.js | public/js/ | Quản lý và phản hồi phiếu hỗ trợ |
| index.js | functions/ | Cloud Functions: xử lý OCR, thanh toán SePay và thông báo FCM |

### 3.3.2 Thiết kế giao diện người dùng

Giao diện ứng dụng tuân theo **Material Design 3** của Google với bảng màu chủ đạo xanh lam. Hệ thống điều hướng sử dụng Bottom Navigation Bar với 4 tab chính (Trang chủ, Tìm kiếm, Đăng tin, Hồ sơ) và nút FAB ở trung tâm để kích hoạt trợ lý AI.

**Bảng 3.5: Mô tả các màn hình chính của ứng dụng**

| Màn hình | Vai trò | Chức năng chính |
|---|---|---|
| Đăng nhập / Đăng ký | Tất cả | Xác thực tài khoản qua email và mật khẩu; đăng ký mới với họ tên, số điện thoại và mật khẩu |
| Trang chủ | Người dùng | Hiển thị thanh tìm kiếm nhanh, danh sách khu vực phổ biến, tin phòng nổi bật và phòng mới đăng |
| Tìm kiếm nâng cao | Người dùng | Lọc phòng theo địa chỉ, khoảng giá, diện tích và tiện ích; hiển thị danh sách kết quả phân trang |
| Chi tiết phòng trọ | Người dùng | Xem thư viện ảnh, thông tin giá và diện tích, danh sách tiện ích, thông tin người đăng tin; đặt lịch và nhắn tin |
| Đặt lịch xem phòng | Người dùng | Chọn ngày và khung giờ còn trống, nhập ghi chú và xác nhận đặt lịch |
| Lịch hẹn của tôi | Người dùng | Danh sách lịch hẹn đã đặt kèm trạng thái |
| Nhắn tin | Người dùng, Người đăng tin | Danh sách kênh chat; giao diện nhắn tin với bong bóng phân biệt bên gửi và bên nhận; gửi ảnh đính kèm |
| Xác minh CCCD | Người đăng tin | Chụp ảnh CCCD bằng camera; nhận dạng văn bản; gửi hồ sơ để quản trị viên duyệt |
| Đăng tin phòng trọ | Người đăng tin | Biểu mẫu nhập thông tin phòng, tải nhiều ảnh, chọn tiện ích và quy định |
| Quản lý tin đăng | Người đăng tin | Danh sách phòng đã đăng kèm trạng thái; chỉnh sửa và xóa tin |
| Quản lý yêu cầu đặt lịch | Người đăng tin | Danh sách yêu cầu đặt lịch từ người dùng; xác nhận hoặc từ chối |
| Hồ sơ cá nhân | Tất cả | Xem và chỉnh sửa thông tin tài khoản; trạng thái xác minh; đăng xuất |
| Hỗ trợ kỹ thuật | Tất cả | Tạo phiếu hỗ trợ; danh sách phiếu đã gửi; chat với quản trị viên |
| Chat AI | Tất cả | Trợ lý AI hỗ trợ tìm phòng trọ on-device; lưu lịch sử hội thoại |

Dưới đây là một số wireframe màn hình trọng tâm:

> **[Hình 3.5: Wireframe màn hình Trang chủ]**
> *(Thanh tìm kiếm nhanh phía trên, dải khu vực phổ biến dạng chip cuộn ngang, danh sách tin nổi bật dạng thẻ lớn, danh sách phòng mới đăng cuộn dọc)*

> **[Hình 3.6: Wireframe màn hình Tìm kiếm nâng cao]**
> *(Thanh nhập địa chỉ tìm kiếm, bộ lọc giá thuê theo dải giá, bộ lọc diện tích, bộ lọc tiện ích dạng chip đa chọn, danh sách kết quả phân trang)*

> **[Hình 3.7: Wireframe màn hình Chi tiết phòng trọ]**
> *(Thư viện ảnh vuốt ngang phía trên, thông tin giá và diện tích, danh sách tiện ích dạng chip, mô tả và quy định thuê, thông tin người đăng tin, thanh hành động cố định phía dưới với nút Đặt lịch và Nhắn tin)*

> **[Hình 3.8: Wireframe màn hình Đặt lịch hẹn]**
> *(Lịch chọn ngày dạng calendar, danh sách khung giờ còn trống dạng chip, ô nhập ghi chú tùy chọn, nút xác nhận đặt lịch)*

> **[Hình 3.9: Wireframe màn hình Nhắn tin]**
> *(Danh sách kênh chat kèm ảnh đại diện và nội dung tin nhắn cuối; màn hình chat với bong bóng tin nhắn phân biệt bên gửi và bên nhận; thanh nhập tin nhắn kèm nút gửi ảnh đính kèm)*

> **[Hình 3.10: Wireframe màn hình Đăng nhập và Đăng ký]**
> *(Form đăng nhập với trường email và mật khẩu; màn hình đăng ký gồm thông tin họ tên, email, số điện thoại và mật khẩu)*

> **[Hình 3.11: Wireframe màn hình Xác minh CCCD]**
> *(Màn hình hướng dẫn xác minh; giao diện camera CameraX với khung căn chỉnh CCCD; màn hình hiển thị kết quả OCR để người dùng xác nhận trước khi gửi)*

> **[Hình 3.12: Wireframe màn hình Quản lý lịch hẹn và tin đăng (Người đăng tin)]**
> *(Tab Lịch hẹn với danh sách yêu cầu kèm thông tin người dùng, tên phòng và thời gian; nút Xác nhận và Từ chối; tab Tin đăng với danh sách phòng đã đăng kèm nhãn trạng thái duyệt)*

> **[Hình 3.13: Wireframe màn hình Đăng tin phòng trọ]**
> *(Biểu mẫu nhập địa chỉ và giá thuê; danh sách tiện ích dạng chip đa chọn; ô nhập quy định nhà trọ; nút tải nhiều ảnh; thanh hiển thị số slot đăng tin còn lại)*

### 3.3.3 Luồng hoạt động cơ bản

Phần này mô tả bốn luồng nghiệp vụ cốt lõi của hệ thống, thể hiện sự phối hợp giữa ứng dụng Android, Firestore và Cloud Functions.

> **[Hình 3.14: Sơ đồ luồng đăng ký và đăng nhập]**

Khi người dùng nhấn "Đăng ký", ứng dụng thu thập thông tin gồm email, mật khẩu và số điện thoại, sau đó tạo tài khoản trên Firebase Authentication. Ngay sau khi tài khoản được tạo thành công, hồ sơ người dùng được khởi tạo trong Firestore với vai trò mặc định là người dùng thường, chưa xác minh danh tính. Từ lần đăng nhập tiếp theo, ứng dụng đọc hồ sơ này để xác định quyền truy cập của người dùng.

> **[Hình 3.15: Sơ đồ luồng đăng tin phòng trọ]**

Người đăng tin đã xác minh điền thông tin phòng và chọn ảnh. Ứng dụng tạo bản ghi phòng trọ trong Firestore ở trạng thái chờ duyệt, sau đó tải ảnh lên Firebase Storage và cập nhật đường dẫn ảnh vào bản ghi. Tại bảng điều khiển quản trị, quản trị viên kiểm tra nội dung và nhấn "Duyệt" — lúc này tin đăng mới được phép xuất hiện công khai trong kết quả tìm kiếm.

> **[Hình 3.16: Sơ đồ luồng đặt lịch hẹn xem phòng]**

Khi người dùng nhấn "Đặt lịch xem phòng", ứng dụng chuyển sang giao diện chọn ngày và khung giờ còn trống. Sau khi xác nhận, hệ thống đồng thời ghi nhận lịch hẹn vào collection `appointments` ở trạng thái chờ xác nhận và đánh dấu khung giờ đã được đặt trong collection `bookedSlots` (dùng composite key) để ngăn trùng lặp. Ngay sau đó, hệ thống gửi thông báo FCM đến người đăng tin.

> **[Hình 3.17: Sơ đồ luồng xác minh danh tính CCCD]**

Người dùng dùng camera CameraX với khung căn chỉnh để chụp mặt trước và mặt sau CCCD. Ứng dụng chạy ML Kit Text Recognition trực tiếp trên thiết bị để trích xuất thông tin và so khớp với số CCCD đã nhập. Nếu nhận dạng on-device không thành công, ảnh được tải lên Cloud Storage và Cloud Function gọi Google Cloud Vision API để phân tích lại. Sau khi xác minh thành công hoặc được quản trị viên phê duyệt, tài khoản được nâng lên quyền người đăng tin.

---

## 3.4 Kết quả triển khai ứng dụng

### 3.4.1 Chức năng đăng ký, đăng nhập

**a. Chức năng đăng nhập**

**Mô tả:** Cho phép người dùng đã có tài khoản xác thực và truy cập hệ thống bằng email/mật khẩu thông qua Firebase Authentication.

**Kết quả thực hiện:**

> **[Hình 3.18: Giao diện đăng nhập]**

**Nhận xét:** Chức năng đăng nhập hoạt động ổn định. Hệ thống phát hiện và thông báo lỗi email/mật khẩu không đúng, tài khoản bị khóa kèm lý do và thời hạn mở khóa. FCM token được cập nhật ngay sau đăng nhập thành công để đảm bảo nhận được thông báo đẩy.

**b. Chức năng đăng ký**

**Mô tả:** Cho phép người dùng mới tạo tài khoản với email Gmail, số điện thoại và mật khẩu mạnh, sau đó tự động đăng nhập và chuyển đến trang chủ.

**Kết quả thực hiện:**

> **[Hình 3.19: Giao diện đăng ký tài khoản]**

**Nhận xét:** Hệ thống kiểm tra trùng lặp số điện thoại và email trước khi tạo tài khoản, ngăn đăng ký nhiều tài khoản bằng cùng một số điện thoại. Tài khoản được tạo trong Firebase Auth và Firestore đồng thời trong cùng một luồng.

---

### 3.4.2 Chức năng của người thuê

**a. Chức năng tìm kiếm phòng trọ**

**Mô tả:** Cho phép người dùng tìm kiếm và lọc phòng trọ theo địa chỉ, khoảng giá, diện tích và các tiện ích cần thiết.

**Kết quả thực hiện:**

> **[Hình 3.22: Giao diện tìm kiếm với bộ lọc]**

**Nhận xét:** Bộ lọc tìm kiếm hoạt động chính xác, kết hợp được nhiều tiêu chí đồng thời. Kết quả được phân trang 10 bản ghi mỗi lần, tải thêm khi cuộn xuống cuối danh sách.

**b. Chức năng danh sách phòng trọ**

**Mô tả:** Hiển thị danh sách phòng trọ trên trang chủ theo khu vực phổ biến, tin nổi bật và tin mới nhất.

**Kết quả thực hiện:**

> **[Hình 3.20: Giao diện trang chủ ứng dụng]**

> **[Hình 3.21: Giao diện khu vực phổ biến và tin nổi bật trên trang chủ]**

**Nhận xét:** Trang chủ tải nhanh nhờ phân trang và cache. Tin nổi bật tự động luân chuyển mỗi 10 giây tạo trải nghiệm sinh động.

**c. Chức năng chi tiết phòng trọ**

**Mô tả:** Hiển thị đầy đủ thông tin phòng trọ bao gồm thư viện ảnh, giá thuê, tiện ích, quy định nhà trọ và thông tin liên hệ chủ trọ.

**Kết quả thực hiện:**

> **[Hình 3.24: Giao diện chi tiết phòng trọ — ảnh và thông tin cơ bản]**

> **[Hình 3.25: Giao diện chi tiết phòng trọ — tiện ích, quy định và thông tin chủ trọ]**

**Nhận xét:** Giao diện chi tiết phòng trình bày đầy đủ thông tin người dùng cần để ra quyết định. Các nút hành động chính (Đặt lịch, Nhắn tin, Lưu tin) luôn hiển thị không cần cuộn trang.

**d. Chức năng bản đồ phòng trọ**

**Mô tả:** Hiển thị vị trí phòng trọ trên Google Maps thông qua tọa độ GPS được lưu trong bài đăng, giúp người dùng quan sát vị trí thực tế của phòng.

**Nhận xét:** Tọa độ latitude/longitude được lưu trong bài đăng phòng trọ. Khi xem chi tiết phòng, người dùng có thể mở bản đồ để quan sát vị trí phòng và khoảng cách đến các địa điểm quen thuộc.

**e. Chức năng đặt lịch hẹn**

**Mô tả:** Cho phép người dùng chọn ngày và khung giờ còn trống để đặt lịch hẹn xem phòng trực tiếp với người đăng tin.

**Kết quả thực hiện:**

> **[Hình 3.26: Giao diện chọn ngày và giờ đặt lịch hẹn]**

> **[Hình 3.27: Thông báo đặt lịch hẹn thành công]**

**Nhận xét:** Cơ chế khóa khung giờ bằng composite key trong `bookedSlots` hoạt động chính xác, ngăn hai người dùng đặt cùng khung giờ. Người đăng tin nhận thông báo FCM ngay lập tức sau khi lịch hẹn được tạo.

**f. Chức năng quản lý lịch hẹn**

**Mô tả:** Cho phép người dùng xem danh sách lịch hẹn đã đặt kèm trạng thái và hủy lịch khi cần.

**Kết quả thực hiện:**

> **[Hình 3.28: Giao diện danh sách lịch hẹn của người dùng]**

**Nhận xét:** Trạng thái lịch hẹn cập nhật theo thời gian thực qua Firestore snapshot listener. Badge thông báo xuất hiện khi có cập nhật trạng thái mới chưa được người dùng xem.

**g. Chức năng nhắn tin**

**Mô tả:** Cho phép người dùng và người đăng tin trao đổi trực tiếp trong ứng dụng qua hệ thống chat thời gian thực, hỗ trợ gửi văn bản và hình ảnh.

**Kết quả thực hiện:**

> **[Hình 3.29: Giao diện danh sách hội thoại]**

> **[Hình 3.30: Giao diện màn hình chat với người đăng tin]**

**Nhận xét:** Tin nhắn hiển thị theo thời gian thực trên cả hai thiết bị nhờ Firestore snapshot listener. Danh sách hội thoại hiển thị số tin chưa đọc và nội dung tin nhắn cuối. Người dùng có thể thêm cảm xúc (emoji reaction) vào từng tin nhắn. Hình ảnh đính kèm được tải lên Firebase Storage và đường dẫn lưu trong document tin nhắn.

**h. Chức năng đánh giá, bình luận phòng trọ**

**Mô tả:** Cho phép người thuê gửi đánh giá (1–5 sao) và nhận xét về chủ trọ sau khi đã có lịch hẹn với phòng trọ liên quan.

**Nhận xét:** Đánh giá được lưu vào collection `reviews` ở trạng thái chờ duyệt. Quản trị viên có thể ẩn hoặc xóa các đánh giá không phù hợp. Người dùng bị cấm tự đánh giá phòng của mình theo quy tắc Firestore Security Rules. Sau khi được phê duyệt, đánh giá hiển thị công khai trên trang chi tiết phòng trọ.

---

### 3.4.3 Chức năng của người cho thuê

**a. Chức năng đăng tin phòng trọ**

**Mô tả:** Cho phép người đăng tin (đã xác minh CCCD) điền thông tin phòng, tải ảnh và gửi bài đăng để quản trị viên kiểm duyệt.

**Kết quả thực hiện:**

> **[Hình 3.34: Giao diện form đăng tin phòng — thông tin cơ bản]**

> **[Hình 3.35: Giao diện form đăng tin phòng — tiện ích và quy định]**

**Nhận xét:** Form đăng tin thu thập đầy đủ thông tin cần thiết. Hạn mức 3 tin miễn phí/24 giờ được kiểm tra phía Firestore Security Rules, tránh lách luật từ phía client. Ảnh được tải lên Storage sau khi tạo document phòng để tránh ảnh mồ côi.

**b. Chức năng quản lý bài đăng**

**Mô tả:** Cho phép người đăng tin xem danh sách bài đăng với trạng thái kiểm duyệt, chỉnh sửa thông tin và xóa tin.

**Kết quả thực hiện:**

> **[Hình 3.36: Giao diện danh sách tin đã đăng với trạng thái]**

**Nhận xét:** Trạng thái kiểm duyệt (chờ duyệt/đã duyệt/từ chối) hiển thị rõ ràng bằng màu sắc khác nhau. Người đăng tin nhận thông báo ngay khi quản trị viên duyệt hoặc từ chối bài.

**c. Chức năng quản lý lịch hẹn**

**Mô tả:** Cho phép người đăng tin xem danh sách yêu cầu đặt lịch và xác nhận hoặc từ chối từng lịch hẹn.

**Kết quả thực hiện:**

> **[Hình 3.37: Giao diện quản lý lịch hẹn của người đăng tin]**

**Nhận xét:** Giao diện hiển thị đầy đủ thông tin người đặt lịch, thời gian hẹn và ghi chú. Sau khi xác nhận hoặc từ chối, người dùng nhận thông báo FCM ngay lập tức.

**d. Chức năng xem đánh giá, bình luận phòng trọ**

**Mô tả:** Người đăng tin có thể xem các đánh giá mà người thuê gửi về phòng trọ và chủ trọ của mình.

**Nhận xét:** Đánh giá được hiển thị công khai trên trang chi tiết phòng sau khi được quản trị viên phê duyệt, giúp tăng độ minh bạch và tin cậy của nền tảng.

---

### 3.4.4 Chức năng của quản trị viên

**a. Chức năng quản trị người dùng**

**Mô tả:** Cho phép quản trị viên xem danh sách tài khoản, khóa/mở khóa tài khoản vi phạm và xóa tài khoản không hoạt động.

**Kết quả thực hiện:**

> **[Hình 3.41: Giao diện quản lý người dùng với chức năng khóa tài khoản]**

**Nhận xét:** Giao diện hiển thị đầy đủ thông tin người dùng kèm trạng thái. Khóa tài khoản có thể đặt thời hạn cụ thể; hệ thống tự động mở khóa khi hết hạn. Tính năng xuất Excel hỗ trợ lưu trữ và báo cáo.

**b. Chức năng kiểm duyệt bài đăng**

**Mô tả:** Cho phép quản trị viên kiểm tra nội dung bài đăng và duyệt hoặc từ chối trước khi hiển thị công khai.

**Kết quả thực hiện:**

> **[Hình 3.39: Giao diện duyệt tin đăng phòng trọ]**

> **[Hình 3.40: Giao diện xét duyệt hồ sơ xác minh CCCD]**

**Nhận xét:** Quản trị viên xem đầy đủ ảnh, thông tin và kết quả OCR tự động trước khi ra quyết định. Sau khi duyệt, người đăng tin nhận FCM và tin xuất hiện trong kết quả tìm kiếm ngay lập tức.

**c. Chức năng thống kê hệ thống**

**Mô tả:** Cung cấp bảng điều khiển tổng quan với số liệu thống kê, biểu đồ tăng trưởng và danh sách chờ xử lý.

**Kết quả thực hiện:**

> **[Hình 3.38: Giao diện dashboard tổng quan với thống kê và biểu đồ]**

**Nhận xét:** Dashboard hiển thị 4 thẻ thống kê chính, biểu đồ cột 6 tháng và biểu đồ tròn phân loại người dùng. Dữ liệu được cache 5 phút phía client, giảm thiểu số lần truy vấn Firestore.

---

## 3.5 Kiểm thử hệ thống

**Bảng 3.8: Kịch bản kiểm thử các chức năng chính của hệ thống**

| STT | Chức năng | Dữ liệu/thao tác kiểm thử | Kết quả mong đợi |
|-----|-----------|---------------------------|------------------|
| 1 | Đăng ký tài khoản | Email hợp lệ, mật khẩu tối thiểu 12 ký tự, số điện thoại chưa đăng ký | Tài khoản được tạo; trạng thái "chưa xác minh" |
| 2 | Đăng nhập | Email/mật khẩu đúng | Chuyển vào trang chủ; phiên đăng nhập được duy trì |
| 3 | Khóa tài khoản | Quản trị viên khóa tài khoản đang hoạt động | Người dùng bị đăng xuất; đăng nhập lại hiển thị thông báo bị khóa kèm lý do và thời hạn |
| 4 | Tìm kiếm phòng | Nhập từ khóa địa chỉ, chọn bộ lọc giá và tiện ích | Danh sách phòng phù hợp hiển thị đúng điều kiện |
| 5 | Đăng bài phòng trọ | Điền đủ thông tin, tải ảnh, nhấn đăng | Tin ở trạng thái "Chờ duyệt"; hiển thị trong danh sách tin của tài khoản |
| 6 | Duyệt bài đăng | Quản trị viên duyệt/từ chối tin đang chờ | Trạng thái tin cập nhật theo thời gian thực; người đăng nhận thông báo |
| 7 | Đặt lịch hẹn | Chọn phòng → chọn ngày/khung giờ còn trống → xác nhận | Lịch hẹn được tạo; chủ trọ nhận thông báo; khung giờ bị khóa cho yêu cầu khác |
| 8 | Nhắn tin | Gửi tin nhắn văn bản và hình ảnh giữa hai người dùng | Tin nhắn xuất hiện tức thì ở cả hai phía; không mất tin khi đổi màn hình |
| 9 | Xác minh CCCD | Chụp mặt trước và mặt sau CCCD | Thông tin được trích xuất và hiển thị để xác nhận; hồ sơ chờ quản trị viên phê duyệt |
| 10 | Thanh toán nâng cấp slot | Chọn gói slot → hoàn tất thanh toán qua SePay | Số lượng slot tăng đúng; lịch sử giao dịch được ghi nhận |
| 11 | Gửi yêu cầu hỗ trợ | Nhập nội dung hỗ trợ → gửi | Phiếu hỗ trợ được tạo; quản trị viên thấy yêu cầu và có thể phản hồi |

## 3.6 Đánh giá hệ thống

### 3.6.1 Ưu điểm

- **Đồng bộ thời gian thực:** Toàn bộ dữ liệu được đồng bộ tức thì qua Firestore snapshot listeners, đảm bảo người dùng luôn thấy trạng thái mới nhất của tin đăng, lịch hẹn và tin nhắn mà không cần tải lại thủ công.
- **Backend serverless linh hoạt:** Cloud Functions đảm nhận các tác vụ nặng (OCR CCCD, xử lý thanh toán SePay, gửi FCM), giúp ứng dụng di động nhẹ và không bị nghẽn luồng giao diện.
- **Cơ chế xác minh và kiểm soát nội dung:** Quy trình xác minh CCCD hai bước (OCR tự động + quản trị viên phê duyệt) và hệ thống quota đăng bài giúp kiểm soát chất lượng và giảm spam hiệu quả.
- **Phân quyền chặt chẽ:** Firestore Security Rules phân tách rõ ràng quyền truy cập giữa người dùng thường, người đăng bài và quản trị viên, bảo vệ dữ liệu nhạy cảm ngay ở tầng cơ sở dữ liệu.
- **Trải nghiệm tìm kiếm đa chiều:** Người dùng có thể kết hợp tìm kiếm theo địa chỉ và bộ lọc giá/tiện ích, đáp ứng nhiều phong cách sử dụng khác nhau.

### 3.6.2 Hạn chế

- **Thuật toán gợi ý chưa cá nhân hóa:** Thứ tự hiển thị phòng trọ hiện dựa trên thời gian đăng và độ nổi bật, chưa tích hợp mô hình học máy để gợi ý theo lịch sử tìm kiếm và hành vi người dùng.
- **Một số quy trình vẫn cần can thiệp thủ công:** Phê duyệt tin đăng và hồ sơ xác minh CCCD phụ thuộc vào quản trị viên, dẫn đến độ trễ khi lượng yêu cầu tăng đột biến.
- **Chưa có kiểm thử tự động:** Hệ thống chưa được trang bị bộ kiểm thử tích hợp (unit test, instrumented test) và công cụ giám sát tải.
- **Phụ thuộc hoàn toàn vào Firebase:** Mọi tính năng đều gắn chặt với Google Firebase, gây rủi ro khi có thay đổi chính sách giá hoặc giới hạn quota.

### 3.6.3 Hướng phát triển

- **Nâng cao thuật toán gợi ý:** Tích hợp mô hình collaborative filtering hoặc content-based recommendation để cá nhân hóa kết quả tìm kiếm.
- **Hệ thống đánh giá chủ trọ mở rộng:** Thêm đánh giá phòng trọ sau khi thực sự thuê, tăng độ tin cậy của nền tảng.
- **Tìm kiếm bán kính trên bản đồ:** Cho phép tìm phòng trong bán kính cụ thể từ vị trí hiện tại của người dùng.
- **Tự động hóa thanh toán tiền thuê:** Tích hợp nhắc nhở và xử lý thanh toán tiền thuê định kỳ.
- **Kiểm thử và giám sát tự động:** Bổ sung unit test, instrumented test và tích hợp Firebase Performance Monitoring, Crashlytics.

### Tổng kết Chương 3

Chương 3 đã trình bày toàn bộ quá trình triển khai thực tế của hệ thống Tìm Trọ 24/7 theo ba tầng kiến trúc. Phần 3.1 mô tả mô hình Client–Firebase, kiến trúc tổng thể và cơ chế đồng bộ thời gian thực. Phần 3.2 thiết kế chi tiết từng dịch vụ Firebase: Authentication, Firestore, Storage, FCM và Security Rules. Phần 3.3 trình bày cấu trúc mã nguồn MVVM và các wireframe giao diện chính. Phần 3.4 minh họa kết quả triển khai qua mười nhóm giao diện thực tế theo cấu trúc Mô tả – Kết quả – Nhận xét. Phần 3.5 trình bày bộ kịch bản kiểm thử thủ công xác nhận hệ thống hoạt động đúng. Phần 3.6 đánh giá ưu điểm, hạn chế và định hướng phát triển tương lai, cho thấy toàn bộ các chức năng đã phân tích trong Chương 2 được hiện thực hóa hoàn chỉnh trên nền tảng Android và Firebase.

---

# KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN

Trong quá trình thực hiện đồ án, tác giả đã xây dựng thành công ứng dụng **Tìm Trọ 24/7** — một nền tảng kết nối người thuê trọ và chủ trọ trên thiết bị di động Android. Ứng dụng triển khai đầy đủ các chức năng cốt lõi: tìm kiếm và lọc phòng trọ đa tiêu chí, đặt lịch hẹn xem phòng trực tiếp, nhắn tin thời gian thực và hệ thống xác minh danh tính CCCD tự động. Bên cạnh ứng dụng Android, hệ thống còn có bảng điều khiển web dành cho quản trị viên để kiểm duyệt nội dung và quản lý người dùng.

Ứng dụng được phát triển bằng ngôn ngữ Kotlin với kiến trúc MVVM, đảm bảo tính rõ ràng, dễ kiểm thử và dễ mở rộng mã nguồn. Toàn bộ backend được xây dựng trên nền tảng Firebase, tận dụng khả năng đồng bộ thời gian thực của Firestore và sức mạnh serverless của Cloud Functions để tích hợp các tính năng nâng cao như nhận dạng CCCD (Google Cloud Vision) và xử lý thanh toán (SePay).

Tuy ứng dụng đã đáp ứng được các yêu cầu đề ra, vẫn còn nhiều hướng phát triển tiềm năng trong tương lai:

- **Tích hợp bản đồ tương tác:** Cho phép người dùng tìm phòng trong bán kính cụ thể từ vị trí hiện tại trên Google Maps.
- **Tính năng đánh giá và xếp hạng mở rộng:** Người thuê đánh giá chủ trọ và phòng trọ sau khi thực sự thuê, giúp tăng độ tin cậy của nền tảng.
- **Hợp đồng điện tử:** Tích hợp tạo và ký kết hợp đồng thuê nhà điện tử ngay trong ứng dụng.
- **Phát triển đa nền tảng:** Mở rộng ứng dụng sang iOS và web để tiếp cận thêm nhiều người dùng.
- **Tối ưu hóa tìm kiếm:** Áp dụng Elasticsearch hoặc Algolia cho tính năng tìm kiếm toàn văn bản hiệu quả hơn khi quy mô dữ liệu tăng lớn.

Thông qua quá trình thực hiện đồ án, tác giả đã củng cố và mở rộng kiến thức về phát triển ứng dụng di động Android, kiến trúc phần mềm MVVM và các dịch vụ đám mây Firebase. Đây là nền tảng vững chắc để tiếp tục học hỏi và phát triển trong lĩnh vực công nghệ thông tin.

---

# TÀI LIỆU THAM KHẢO

[1] Bộ môn Hệ thống thông tin. (2020). *Lập trình di động Phần 1 và Lập trình di động Phần 2*. Tài liệu truy cập từ: Thư viện Trường Đại học Thuỷ Lợi.

[2] Google. (n.d.). *Android Developers — Guide to app architecture*. Tài liệu truy cập từ: https://developer.android.com/topic/architecture

[3] Google. (n.d.). *Firebase Documentation*. Tài liệu truy cập từ: https://firebase.google.com/docs

[4] JetBrains. (n.d.). *Kotlin Documentation*. Tài liệu truy cập từ: https://kotlinlang.org/docs/home.html

[5] Google. (n.d.). *Cloud Firestore Documentation*. Tài liệu truy cập từ: https://firebase.google.com/docs/firestore

[6] Google. (n.d.). *ML Kit — Text Recognition*. Tài liệu truy cập từ: https://developers.google.com/ml-kit/vision/text-recognition/android

[7] Google. (n.d.). *Google Cloud Vision API Documentation*. Tài liệu truy cập từ: https://cloud.google.com/vision/docs

[8] Google. (n.d.). *CameraX Overview*. Tài liệu truy cập từ: https://developer.android.com/training/camerax

[9] SePay. (n.d.). *SePay API Documentation*. Tài liệu truy cập từ: https://docs.sepay.vn

[10] Google. (n.d.). *Google Maps Platform Documentation*. Tài liệu truy cập từ: https://developers.google.com/maps/documentation/android-sdk

[11] Google. (n.d.). *Firebase Cloud Messaging*. Tài liệu truy cập từ: https://firebase.google.com/docs/cloud-messaging

[12] Google. (n.d.). *Firebase Cloud Functions*. Tài liệu truy cập từ: https://firebase.google.com/docs/functions

[13] Google. (n.d.). *Material Design 3*. Tài liệu truy cập từ: https://m3.material.io