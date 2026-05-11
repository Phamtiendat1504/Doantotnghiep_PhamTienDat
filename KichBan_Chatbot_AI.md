# BÁO CÁO CHI TIẾT: PHÂN HỆ TRỢ LÝ ẢO AI TOÀN NĂNG (UNIVERSE AI ASSISTANT) - TIM TRO 24/7

*Tài liệu này cung cấp bản mô tả kỹ thuật chuyên sâu và kịch bản nghiệp vụ của phân hệ Chatbot AI thế hệ mới, được thiết kế để tích hợp trực tiếp vào báo cáo Đồ án Tốt nghiệp chuyên ngành Công nghệ Thông tin/Kỹ thuật Phần mềm.*

---

## 1. TỔNG QUAN PHÂN HỆ (MODULE OVERVIEW)

### 1.1. Đặt vấn đề
Các hệ thống chatbot truyền thống thường chỉ dừng lại ở việc trả lời dựa trên văn bản tĩnh (Text-only), thiếu khả năng hiểu ngữ cảnh thực tế của người dùng và không thể thực hiện các thao tác trực tiếp trên hệ thống. Điều này khiến người dùng vẫn phải thực hiện nhiều bước thủ công sau khi được tư vấn.

### 1.2. Giải pháp: Trợ lý AI "Thấu hiểu ngữ cảnh" (Context-Aware AI)
Phân hệ AI trong dự án **TIM TRO 24/7** được nâng cấp từ một chatbot thông thường thành một **Siêu Trợ lý Toàn năng** nhờ tích hợp các công nghệ tiên tiến:
1. **Context Injection:** AI biết rõ người dùng là ai, đã xác minh CCCD chưa, tài khoản có bị khóa không và còn bao nhiêu lượt đăng bài.
2. **Actionable Tools (Function Calling):** AI có thể trực tiếp tìm phòng, lưu bài viết vào danh sách yêu thích và đặt lịch hẹn xem phòng ngay trong khung chat.
3. **Deep Domain Knowledge:** AI nắm lòng toàn bộ quy trình nghiệp vụ của app (Xác minh CCCD, nạp tiền SePay, trạng thái duyệt bài).

---

## 2. KIẾN TRÚC KỸ THUẬT (TECHNICAL ARCHITECTURE)

Hệ thống được xây dựng trên mô hình **Agentic Workflow** (Luồng xử lý tác vụ thông minh).

### 2.1. Thành phần công nghệ
*   **LLM Engine:** Google Gemini (các phiên bản Flash 2.0/1.5).
*   **Middleware:** Firebase Cloud Functions (v2) đóng vai trò là "Bộ não trung tâm".
*   **Action Logic:** Tích hợp trực tiếp với Firestore Admin SDK để thao tác dữ liệu theo thời gian thực.
*   **UI/UX:** Hiển thị dưới dạng tin nhắn Markdown kèm theo các **Card phòng trọ (Listing Cards)** tương tác được.

### 2.2. Cơ chế "Bơm ngữ cảnh" (Context Injected Logic)
Trước khi gửi yêu cầu tới AI, hệ thống thực hiện các bước:
1. **Truy vấn Firestore:** Lấy trạng thái `isVerified`, `purchasedSlots`, `isLocked` của UID hiện tại.
2. **Đóng gói dữ liệu:** Chuyển đổi dữ liệu thô thành ngôn ngữ tự nhiên (Ví dụ: "Người dùng này hiện chưa xác minh CCCD").
3. **Gắn vào System Instruction:** Ép AI phải sử dụng thông tin này để tư vấn cá nhân hóa.

---

## 3. KỊCH BẢN NGHIỆP VỤ & HÀNH ĐỘNG (BUSINESS SCENARIOS & TOOLS)

AI Assistant hỗ trợ 3 nhóm hành động chính thông qua **Function Calling**:

### 3.1. Nhóm Tìm kiếm & Gợi ý (Tool: `searchRooms`)
*   **Kịch bản:** Người dùng hỏi: *"Tìm giúp mình phòng trọ quanh Quận 7 giá dưới 3 triệu."*
*   **Hành động AI:** Gọi hàm `searchRooms(district="Quận 7", maxPrice=3000000)`.
*   **Phản hồi:** AI trả về câu trả lời kèm mã định danh `|||roomId1,roomId2`. App Android sẽ nhận diện mã này để hiển thị các Card phòng trọ đẹp mắt bên dưới tin nhắn.

### 3.2. Nhóm Tương tác dữ liệu (Tool: `saveRoomToFavorites`)
*   **Kịch bản:** Sau khi xem gợi ý, người dùng nói: *"Lưu giúp mình phòng đầu tiên nhé."*
*   **Hành động AI:** Nhận diện mã phòng và gọi hàm `saveRoomToFavorites(roomId="...")`.
*   **Phản hồi:** AI xác nhận: *"Đã lưu phòng vào danh sách yêu thích cho Bạn rồi nhé!"*.

### 3.3. Nhóm Nghiệp vụ lịch hẹn (Tool: `bookAppointment`)
*   **Kịch bản:** *"Mình muốn hẹn xem phòng này vào sáng mai lúc 9h."*
*   **Hành động AI:** Gọi hàm `bookAppointment(roomId="...", date="...", time="09:00")`.
*   **Kết quả:** Hệ thống tự động tạo Document trong collection `appointments` và gửi Push Notification cho chủ nhà.

---

## 4. TƯ VẤN NGHIỆP VỤ CHUYÊN SÂU (DOMAIN EXPERTISE)

Chatbot được nạp bộ quy tắc vận hành của ứng dụng để giải quyết các vấn đề "nhức nhối":

*   **Xử lý lỗi xác minh:** Giải thích tại sao người dùng phải chờ Admin duyệt (do sai sót OCR quá 3 lần).
*   **Hướng dẫn thanh toán:** Giải thích cách quét mã VietQR và cú pháp `REQ_...` để nạp lượt đăng bài qua SePay.
*   **Giải đáp trạng thái bài đăng:** Giải thích tại sao bài bị `Rejected` (vi phạm hình ảnh, giá ảo) và hướng dẫn sửa lại.

---

## 5. BẢO MẬT VÀ KIỂM SOÁT TÀI NGUYÊN

1. **Quota Management:** Giới hạn nghiêm ngặt 50 lượt chat/ngày để kiểm soát chi phí API. Cơ chế tự động reset vào 0h00 mỗi ngày.
2. **Secret Manager:** API Key được lưu trữ trong Google Cloud Secret Manager (không hardcode trong mã nguồn).
3. **Authentication Guard:** Chỉ người dùng đã đăng nhập và có Token hợp lệ mới có thể gọi hàm AI.

---

## 6. CHIẾN LƯỢC DỰ PHÒNG (FALLBACK STRATEGY)

Để đảm bảo AI luôn phản hồi nhanh nhất, hệ thống áp dụng cơ chế **Waterfall Models**:
1.  **Priority 1:** `gemini-2.0-flash` (Hiện đại nhất, hỗ trợ Tool Calling tốt nhất).
2.  **Priority 2:** `gemini-1.5-flash` (Ổn định, tốc độ cao).
3.  **Tự động bắt lỗi:** Nếu gặp lỗi `429` (Quá tải) hoặc `503` (Bảo trì), hệ thống tự động chuyển sang model tiếp theo trong danh sách mà không làm gián đoạn cuộc trò chuyện.

---

## 7. HƯỚNG DẪN TRIỂN KHAI NHANH

### Bước 1: Cấu hình Secret API Key
```bash
firebase functions:secrets:set GEMINI_API_KEY
```

### Bước 2: Deploy Cloud Functions
```bash
firebase deploy --only functions:askAIAssistant
```

### Bước 3: Kiểm tra tích hợp
Mở tab **AI Assistant** trên ứng dụng Android, thực hiện câu hỏi: *"Kiểm tra giúp mình xem tài khoản của mình đã đăng bài được chưa?"*. AI sẽ đọc trạng thái `isVerified` và trả lời chính xác thực trạng của bạn.
