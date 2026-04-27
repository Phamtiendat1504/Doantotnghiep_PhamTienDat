# ĐỀ CƯƠNG ĐỒ ÁN TỐT NGHIỆP

**Tên đề tài:** Xây dựng ứng dụng di động hỗ trợ tìm kiếm phòng trọ và quản lý lịch hẹn dựa trên nền tảng Firebase  
**Sinh viên thực hiện:** Phạm Tiến Đạt  
**Lớp:** 64CNTT2	 
**Mã sinh viên:** 2251061740  
**Số điện thoại:** 0889740127   
**Email:** tiendatpham1504@gmail.com  
**Giáo viên hướng dẫn:** TS. Nguyễn Văn Thẩm  

---
Công nghệ sử dụng
Ngôn ngữ Code: Kotlin (Ngôn ngữ lập trình hiện đại, được Google khuyến khích sử dụng kết hợp với kiến trúc MVVM để tối ưu hóa hiệu suất và tính an toàn của mã nguồn).
Kiến trúc ứng dụng: MVVM (Model-View-ViewModel) – Mô hình kiến trúc giúp tách biệt rõ ràng giữa logic nghiệp vụ, dữ liệu và giao diện người dùng, giúp ứng dụng dễ dàng mở rộng và bảo trì.
Nền tảng xây dựng phát triển: Android Studio – Môi trường phát triển tích hợp (IDE) chuyên dụng và mạnh mẽ nhất để xây dựng các ứng dụng trên hệ điều hành Android.
Backend: Firebase Console – Nền tảng đám mây của Google cung cấp các dịch vụ Backend-as-a-Service (BaaS), cụ thể bao gồm:
Firebase Authentication: Xác thực và quản lý người dùng.
Cloud Firestore: Cơ sở dữ liệu NoSQL lưu trữ dữ liệu theo thời gian thực.
Firebase Cloud Messaging (FCM): Gửi thông báo đẩy (push notification).
Firebase Storage: Lưu trữ hình ảnh phòng trọ.
Công nghệ tích hợp khác:
Google Maps API: Tích hợp bản đồ và định vị vị trí.
Artificial Intelligence (AI): Các thuật toán hỗ trợ tìm kiếm và gợi ý phòng trọ thông minh.


## TÓM TẮT ĐỀ TÀI

Trong bối cảnh nhu cầu tìm kiếm phòng trọ tại Hà Nội ngày càng gia tăng, đặc biệt đối với sinh viên và người lao động, việc tiếp cận thông tin phòng trọ hiện nay vẫn còn nhiều hạn chế như dữ liệu phân tán, thiếu tính xác thực và khó khăn trong việc quản lý lịch hẹn xem phòng. Điều này gây mất thời gian, công sức và làm giảm hiệu quả trong quá trình tìm kiếm chỗ ở phù hợp.

Xuất phát từ thực tiễn đó, đề tài **“Xây dựng ứng dụng di động hỗ trợ tìm kiếm phòng trọ và quản lý lịch hẹn dựa trên nền tảng Firebase”** được thực hiện nhằm cung cấp một giải pháp tiện lợi, hiện đại và hiệu quả. Ứng dụng được phát triển trên nền tảng Android, đóng vai trò là cầu nối giữa người thuê và người cho thuê, cho phép người dùng dễ dàng tìm kiếm phòng trọ theo các tiêu chí như khu vực, giá cả, tiện ích, đồng thời hỗ trợ đặt lịch xem phòng và quản lý trạng thái lịch hẹn một cách trực quan.

Hệ thống được thiết kế theo kiến trúc MVVM, kết hợp với nền tảng Firebase để xử lý backend, bao gồm các dịch vụ như Firebase Authentication (xác thực người dùng), Cloud Firestore (lưu trữ dữ liệu), Firebase Cloud Messaging (gửi thông báo realtime) và Firebase Storage (lưu trữ hình ảnh). Ngoài ra, ứng dụng tích hợp Google Maps API nhằm hiển thị vị trí phòng trọ trên bản đồ, giúp người dùng dễ dàng định vị và lựa chọn khu vực phù hợp. Bên cạnh đó, hệ thống còn hướng tới việc áp dụng các kỹ thuật trí tuệ nhân tạo (AI) để hỗ trợ tìm kiếm và gợi ý phòng trọ phù hợp với nhu cầu người dùng.

Kết quả của đề tài là một ứng dụng Android hoàn chỉnh với các chức năng chính như đăng ký/đăng nhập, quản lý bài đăng phòng trọ, tìm kiếm thông minh, hiển thị bản đồ, đặt lịch xem phòng và gửi thông báo realtime. Ứng dụng không chỉ đáp ứng nhu cầu thực tế của người dùng mà còn có khả năng mở rộng và phát triển thêm trong tương lai.

---

## CÁC MỤC TIÊU CHÍNH

### Mục tiêu nghiên cứu
Đề tài tập trung nghiên cứu bài toán xây dựng hệ thống hỗ trợ tìm kiếm phòng trọ và quản lý lịch hẹn trên nền tảng di động, cụ thể:
*   **Hệ thống hóa cơ sở lý thuyết:** Nghiên cứu các khái niệm liên quan đến phát triển ứng dụng di động Android, kiến trúc MVVM và các dịch vụ backend hiện đại trên nền tảng Firebase. 
*   **Nghiên cứu bài toán tìm kiếm phòng trọ:** Phân tích nhu cầu thực tế của người thuê và chủ trọ, từ đó xác định các tiêu chí tìm kiếm như khu vực, giá cả, tiện ích và khả năng mở rộng. 
*   **Khai thác nền tảng Firebase:** Tìm hiểu cơ chế hoạt động của Firebase Authentication, Cloud Firestore, Firebase Cloud Messaging trong việc xây dựng hệ thống realtime và đồng bộ dữ liệu. 
*   **Phân tích bài toán quản lý lịch hẹn:** Xây dựng mô hình xử lý luồng đặt lịch giữa người thuê và chủ trọ, bao gồm các trạng thái và cơ chế phản hồi. 
*   **Nghiên cứu tích hợp bản đồ:** Ứng dụng Google Maps API để hiển thị vị trí phòng trọ và hỗ trợ định vị trực quan. 
*   **Tiếp cận tìm kiếm thông minh:** Nghiên cứu khả năng áp dụng các kỹ thuật trí tuệ nhân tạo (AI) nhằm cải thiện hiệu quả tìm kiếm và gợi ý phòng trọ phù hợp với người dùng.

### Mục tiêu kỹ thuật
Xây dựng ứng dụng di động hỗ trợ tìm kiếm phòng trọ và quản lý lịch hẹn, bao gồm:
*   **Xây dựng hệ thống xác thực:** Triển khai chức năng đăng ký, đăng nhập và quản lý phiên người dùng thông qua Firebase Authentication. 
*   **Phát triển module quản lý bài đăng:** Cho phép tạo mới, chỉnh sửa, xóa bài đăng và lưu trữ dữ liệu trên Cloud Firestore. 
*   **Xây dựng chức năng tìm kiếm:** Hỗ trợ tìm kiếm phòng trọ theo nhiều tiêu chí khác nhau, hiển thị danh sách và thông tin chi tiết. 
*   **Tích hợp bản đồ:** Sử dụng Google Maps API để hiển thị vị trí phòng trọ và hỗ trợ người dùng quan sát trực quan. 
*   **Phát triển hệ thống đặt lịch:** Cho phép người thuê tạo lịch hẹn, chủ trọ xác nhận hoặc từ chối và quản lý trạng thái lịch. 
*   **Xây dựng hệ thống thông báo:** Triển khai gửi thông báo realtime khi có thay đổi liên quan đến lịch hẹn bằng Firebase Cloud Messaging. 
*   **Áp dụng kiến trúc MVVM:** Tổ chức mã nguồn theo mô hình MVVM nhằm tăng tính mở rộng, dễ bảo trì và tối ưu hiệu năng. 
*   **Đảm bảo tính realtime và ổn định:** Đồng bộ dữ liệu nhanh chóng, đảm bảo ứng dụng hoạt động mượt mà trên thiết bị Android. 

### Mục tiêu đánh giá
*   **Đánh giá chức năng hệ thống:** Kiểm tra mức độ hoàn thiện của các module như xác thực, tìm kiếm, bài đăng, đặt lịch và thông báo. 
*   **Đánh giá hiệu năng ứng dụng:** Xem xét tốc độ phản hồi, khả năng xử lý realtime và độ ổn định khi sử dụng. 
*   **Đánh giá trải nghiệm người dùng:** Phân tích mức độ thân thiện của giao diện và sự thuận tiện trong quá trình sử dụng. 
*   **Kiểm thử thực tế:** Thực hiện demo các luồng chính như tìm phòng, đặt lịch và nhận thông báo trên thiết bị Android. 
*   **Khả năng mở rộng hệ thống:** Đánh giá khả năng phát triển thêm các chức năng trong tương lai như gợi ý thông minh hoặc tích hợp dịch vụ nâng cao.

---

## KẾT QUẢ DỰ KIẾN

### Kết quả về mô hình
*   Xây dựng được mô hình hệ thống ứng dụng di động hỗ trợ tìm kiếm phòng trọ và quản lý lịch hẹn. 
*   Thiết kế kiến trúc ứng dụng theo mô hình MVVM, đảm bảo tách biệt giữa giao diện, xử lý dữ liệu và logic nghiệp vụ. 
*   Xây dựng mô hình dữ liệu trên Firebase Firestore phù hợp với các chức năng như người dùng, bài đăng, lịch hẹn và thông báo. 
*   Hoàn thiện luồng xử lý nghiệp vụ giữa các thành phần: người thuê – chủ trọ – hệ thống. 
*   Nghiên cứu và tích hợp cơ chế tìm kiếm thông minh, hỗ trợ cải thiện hiệu quả tìm kiếm phòng trọ.

### Kết quả về hệ thống
*   Xây dựng hệ thống ứng dụng Android hoàn chỉnh với đầy đủ các module chức năng. 
*   Triển khai module xác thực người dùng, đảm bảo đăng ký, đăng nhập và phân quyền hoạt động ổn định. 
*   Phát triển module quản lý bài đăng, hỗ trợ đầy đủ các thao tác thêm, sửa, xóa và lưu trữ dữ liệu trên Firestore. 
*   Xây dựng chức năng tìm kiếm phòng trọ theo nhiều tiêu chí, đảm bảo hiển thị danh sách và thông tin chi tiết chính xác. 
*   Tích hợp Google Maps để hiển thị vị trí phòng trọ và hỗ trợ người dùng quan sát trực quan. 
*   Hoàn thiện hệ thống đặt lịch xem phòng với đầy đủ các trạng thái xử lý và tương tác giữa người dùng. 
*   Triển khai hệ thống thông báo realtime bằng Firebase Cloud Messaging khi có thay đổi liên quan đến lịch hẹn. 
*   Đảm bảo hệ thống hoạt động ổn định, dữ liệu được đồng bộ nhanh chóng và đáp ứng yêu cầu thực tế.

### Sản phẩm đầu ra
*   Ứng dụng di động hoàn chỉnh hỗ trợ tìm kiếm phòng trọ và quản lý lịch hẹn trên nền tảng Android. 
*   Hệ thống backend sử dụng Firebase đáp ứng các chức năng lưu trữ dữ liệu, xác thực và thông báo realtime. 
*   Bộ mã nguồn được tổ chức theo kiến trúc MVVM, đảm bảo tính rõ ràng, dễ bảo trì và mở rộng. 
*   Báo cáo đồ án tốt nghiệp đầy đủ các nội dung: cơ sở lý thuyết, phân tích thiết kế, triển khai và kết quả đạt được. 
*   Slide thuyết trình và demo minh họa các chức năng chính của hệ thống.
