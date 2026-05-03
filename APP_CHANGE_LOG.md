# APP Change Log

Ghi chú các lỗi đã được đọc, rà soát và chỉnh sửa trong phần app Android.

## Đợt 1

### 1. `ProfileFragment.kt`
- Làm sạch các comment bị lỗi mã hóa ký tự.
- Giữ luồng hiển thị guest/profile ổn định hơn.
- Rà lại hiển thị badge vai trò và avatar.

### 2. `fragment_profile.xml`
- Rà lại thuộc tính `contentDescription` của ảnh đại diện.
- Kiểm tra lại các ID liên quan đến profile screen.

### 3. `SearchFragment.kt`
- Làm sạch logic tìm kiếm và reset filter.
- Ổn định hơn phần chọn khu vực, giá và các điều kiện lọc.

### 4. `SearchViewModel.kt`
- Reset `errorMessage` trước khi chạy tìm kiếm.
- Tránh giữ lỗi cũ khi user search lại.

### 5. `AppointmentRoomDetailActivity.kt`
- Tăng độ ổn định khi không còn dữ liệu phòng.
- Làm fallback tốt hơn cho slider ảnh.
- Rà lại phần hiển thị chi tiết phòng hẹn.

### 6. `MyAppointmentsActivity.kt`
- Rà lại logic lọc lịch hẹn theo tab và trạng thái.
- Ổn định hơn phần hiển thị danh sách lịch.

### 7. `BookingActivity.kt`
- Cải thiện hiển thị thông tin phòng khi dữ liệu trống.
- Giá phòng có fallback rõ ràng hơn.

### 8. `NotificationsActivity.kt`
- Gộp cập nhật trạng thái đọc thông báo thành một batch.
- Hiển thị phản hồi rõ hơn sau khi đánh dấu đã đọc.

### 9. `SavedPostDetailActivity.kt`
- Cải thiện hiển thị thông tin phòng đã lưu.
- Làm rõ fallback cho giá, địa chỉ và thông tin chủ trọ.

### 10. `UserProfileActivity.kt`
- Rà lại phần hiển thị hồ sơ người dùng.
- Kiểm tra lại phân trang và sắp xếp danh sách phòng.

### 11. `SettingsActivity.kt`
- Đồng bộ trạng thái các switch con khi tắt thông báo tổng.
- Rà lại luồng xóa cache và hủy tài khoản.

### 12. `MyPostDetailActivity.kt`
- Thêm quản lý callback của `ViewPager2`.
- Tránh đăng ký callback lặp khi vào lại màn hình.

### 13. `SavedPostsActivity.kt`
- Nối lại `SwipeRefreshLayout` để kéo làm mới.
- Rà lại luồng mở chi tiết và xóa bài đã lưu.

### 14. `SupportTicketDetailActivity.kt`
- Giảm gọi lặp `markUserRead` khi message cập nhật.
- Ổn định hơn khi nhận message mới.

### 15. `MyPostDetailActivity.kt`
- Quản lý callback `ViewPager2` để tránh đăng ký lặp khi mở lại màn hình.
- Dọn callback đúng vòng đời activity.

### 16. `SavedPostsActivity.kt`
- Nối lại luồng kéo làm mới danh sách bài đã lưu.
- Rà lại trạng thái mở chi tiết / xóa bài đã lưu.

## Trạng thái kiểm tra
- Tổng cộng đã rà và ghi chú **26 file code** trong app ở đợt hiện tại.
- Các file đã chỉnh đều được rà lint cục bộ.
- Kết quả hiện tại các lần kiểm tra đều báo `No linter errors found`.

## Ghi chú
- File này sẽ được cập nhật tiếp trong các đợt sửa sau để theo dõi những lỗi đã fix trong app.

## Cập nhật phiên làm việc hiện tại
- `ProfileFragment.kt`: làm sạch comment lỗi mã hóa và ổn định hiển thị profile/guest.
- `SearchFragment.kt` + `SearchViewModel.kt`: làm sạch luồng search, reset error trước khi tìm kiếm.
- `AppointmentRoomDetailActivity.kt`: cải thiện fallback khi dữ liệu phòng thiếu hoặc không còn tồn tại.
- `MyAppointmentsActivity.kt`: rà lại lọc lịch hẹn và luồng hiển thị theo tab.
- `BookingActivity.kt`: thêm fallback cho tiêu đề, địa chỉ, giá phòng.
- `NotificationsActivity.kt`: batch cập nhật trạng thái đọc và thêm phản hồi thành công.
- `SavedPostDetailActivity.kt`: làm rõ fallback cho dữ liệu phòng đã lưu.
- `SavedPostsActivity.kt`: nối lại pull-to-refresh, rà lại mở chi tiết/xóa bài đã lưu.
- `MyPostDetailActivity.kt`: quản lý callback `ViewPager2` đúng vòng đời.
- `SupportTicketDetailActivity.kt`: giảm gọi lặp `markUserRead` khi message cập nhật.
- `SettingsActivity.kt`: đồng bộ switch con khi tắt thông báo tổng.
- `UserProfileActivity.kt`: rà lại phần hồ sơ, sắp xếp và phân trang phòng đăng.
- `EditPostActivity.kt`: rà lại toàn bộ form sửa bài đăng, ảnh và dữ liệu danh mục.
- `MyPostsActivity.kt`: rà lại danh sách bài đăng của tôi, lọc, xóa, gia hạn, đẩy nổi bật.
- `SupportTicketsActivity.kt`: rà lại danh sách ticket và tạo ticket mới.
- `SavedPostsViewModel.kt`: rà lại luồng tải/xóa bài đã lưu và trạng thái check phòng.
- `SupportViewModel.kt`: đồng bộ tốt hơn `clearCreatedTicketId()` với LiveData bằng `postValue`.

## Các lỗi/fix nổi bật theo nhóm
- Chuỗi comment bị lỗi mã hóa ký tự được làm sạch ở một số file UI.
- Nhiều màn hình được bổ sung fallback khi dữ liệu backend thiếu hoặc trống.
- Một số LiveData/observer được chỉnh để tránh trigger lặp hoặc giữ state cũ.
- Một số màn hình có pull-to-refresh / refresh state được đồng bộ lại tốt hơn.
- Một số danh sách đã được sắp xếp theo thời gian hoặc trạng thái để dễ dùng hơn.
- Một số luồng UI đã được ổn định lại để hạn chế callback trùng hoặc hiển thị lệch trạng thái.
- Một số ViewModel được chỉnh để xử lý LiveData an toàn và nhất quán hơn.

## Chi tiết đã fix theo file
### 1. `ProfileFragment.kt`
- Làm sạch các comment lỗi mã hóa ký tự.
- Giữ UI profile ổn định hơn khi dữ liệu guest/user thay đổi.
- Rà lại hiển thị avatar mặc định và badge hồ sơ.

### 2. `SearchFragment.kt`
- Rà lại logic tìm kiếm, lọc và reset trạng thái tìm kiếm.
- Làm sạch luồng chọn khu vực/giá để tránh lọc bị giữ trạng thái cũ.

### 3. `SearchViewModel.kt`
- Reset `errorMessage` trước khi chạy tìm kiếm mới.
- Tránh hiển thị lỗi cũ khi user search lại.

### 4. `AppointmentRoomDetailActivity.kt`
- Cải thiện fallback khi phòng không còn tồn tại hoặc dữ liệu chi tiết thiếu.
- Làm slider ảnh an toàn hơn khi danh sách ảnh rỗng.

### 5. `MyAppointmentsActivity.kt`
- Rà lại lọc lịch hẹn theo tab và theo trạng thái.
- Ổn định hơn phần hiển thị danh sách lịch hẹn đã đặt / lịch khách hẹn.

### 6. `BookingActivity.kt`
- Thêm fallback cho tiêu đề phòng, địa chỉ phòng và giá phòng.
- Tránh hiển thị chuỗi trống khi dữ liệu đầu vào thiếu.

### 7. `NotificationsActivity.kt`
- Gom cập nhật `seen` và `isRead` vào một batch khi đánh dấu tất cả đã đọc.
- Thêm phản hồi thành công sau khi mark all read.

### 8. `SavedPostDetailActivity.kt`
- Hiển thị giá phòng rõ hơn khi thiếu dữ liệu.
- Ghép địa chỉ sạch hơn, giảm trường hợp dư dấu phẩy.
- Thông tin chủ trọ có fallback rõ ràng nếu thiếu.

### 9. `SavedPostsActivity.kt`
- Nối lại `SwipeRefreshLayout` để kéo làm mới danh sách.
- Tắt trạng thái refresh sau khi load lỗi hoặc xóa bài xong.
- Rà lại luồng mở chi tiết bài đã lưu và tự xóa khi phòng không còn tồn tại.

### 10. `MyPostDetailActivity.kt`
- Quản lý callback của `ViewPager2` đúng vòng đời.
- Tránh đăng ký callback lặp khi mở lại màn hình.
- Làm phần slider ảnh ổn định hơn khi quay lại activity.

### 11. `SupportTicketDetailActivity.kt`
- Giảm gọi lặp `markUserRead` mỗi khi message cập nhật.
- Theo dõi số lượng message để chỉ đánh dấu đọc khi có thay đổi thật.

### 12. `SettingsActivity.kt`
- Đồng bộ tắt các switch con khi tắt switch thông báo tổng.
- Tránh trạng thái UI và dữ liệu lưu bị lệch nhau.

### 13. `UserProfileActivity.kt`
- Rà lại phần hiển thị hồ sơ người dùng.
- Ổn định hơn phần sắp xếp và phân trang phòng đăng.

### 14. `EditPostActivity.kt`
- Rà lại toàn bộ form sửa bài đăng.
- Kiểm tra lại phần ảnh, chọn ảnh, xoá ảnh và thay ảnh.
- Rà lại phần map dữ liệu từ form sang payload cập nhật.

### 15. `MyPostsActivity.kt`
- Rà lại danh sách bài đăng của tôi, lọc theo trạng thái và tìm kiếm.
- Ổn định hơn luồng xóa bài, gia hạn bài và đẩy nổi bật.
- Tắt refresh state khi load lỗi để UI không bị treo.

### 16. `SupportTicketsActivity.kt`
- Rà lại danh sách ticket và form tạo ticket mới.
- Sắp xếp ticket theo thời gian cập nhật mới nhất.

### 17. `SavedPostsViewModel.kt`
- Rà lại luồng tải bài đã lưu và check phòng còn tồn tại.
- Giữ trạng thái check/delete rõ hơn để UI xử lý đúng.

### 18. `SupportViewModel.kt`
- Đồng bộ tốt hơn `clearCreatedTicketId()` với LiveData bằng `postValue`.
- Tránh giữ state ticket cũ sau khi tạo ticket mới.

### 19. `MainActivity.kt`
- Rà lại điểm vào app và luồng điều hướng chính.

### 20. `RoomDetailActivity.kt`
- Rà lại màn chi tiết phòng và fallback dữ liệu hiển thị.

### 21. `PostFragment.kt`
- Rà lại UI và logic hiển thị danh sách bài trong màn chính.

### 22. `HomeViewModel.kt`
- Rà lại luồng dữ liệu home và trạng thái load.

### 23. `ProfileViewModel.kt`
- Rà lại luồng dữ liệu hồ sơ và trạng thái hiển thị.

### 24. `AppointmentRepository.kt`
- Rà lại repository xử lý lịch hẹn.

### 25. `RoomRepository.kt`
- Rà lại repository xử lý bài phòng / saved posts / related room data.

### 26. `AuthRepository.kt`
- Rà lại repository xử lý tác vụ tài khoản và xác thực.

### 27. `ChatRepository.kt`
- Rà lại repository xử lý chat / ticket liên quan.

### 28. `VerificationRepository.kt`
- Rà lại repository liên quan xác minh.

### 29. `activity_saved_posts.xml`
- Bổ sung/kiểm tra lại vùng refresh, progress và empty state cho danh sách bài đã lưu.

### 30. `activity_my_posts.xml`
- Rà lại bố cục danh sách bài của tôi, vùng lọc và pull-to-refresh.

### 31. `activity_my_post_detail.xml`
- Rà lại giao diện chi tiết bài của tôi, slider ảnh và khu vực thông tin.

### 32. `activity_saved_post_detail.xml`
- Rà lại bố cục bài đã lưu, khu vực slider ảnh và thông tin chi tiết.

### 33. `activity_settings.xml`
- Rà lại phần switch thông báo, hỗ trợ, cache và hủy tài khoản.

### 34. `fragment_profile.xml`
- Rà lại giao diện profile, avatar, badge và danh sách phòng.

### 35. `fragment_search.xml`
- Rà lại phần filter tìm kiếm và layout kết quả.

### 36. `activity_search_results.xml`
- Rà lại giao diện danh sách kết quả tìm kiếm.

### 37. `activity_main.xml`
- Rà lại layout màn hình chính.

### 38. `activity_register.xml`
- Rà lại giao diện đăng ký.

### 39. `activity_change_password.xml`
- Rà lại giao diện đổi mật khẩu.

### 40. `activity_cccd_camera.xml`
- Rà lại giao diện camera chụp CCCD.

### 41. `activity_image_viewer.xml`
- Rà lại giao diện xem ảnh.

### 42. `activity_verify_landlord.xml`
- Rà lại giao diện xác minh chủ trọ.

### 43. `dialog_post_quota_limit.xml`
- Rà lại dialog giới hạn số lượng bài đăng.

### 44. `dialog_search_address.xml`
- Rà lại dialog chọn địa chỉ tìm kiếm.

### 45. `activity_user_profile.xml`
- Rà lại giao diện hồ sơ người dùng.

### 46. `dialog_create_support_ticket.xml`
- Rà lại dialog tạo ticket hỗ trợ.

## Snippet code đã fix
### `SavedPostsActivity.kt`
```kotlin
viewModel.deleteResult.observe(this) { _ ->
    swipeRefreshLayout.isRefreshing = false
    viewModel.loadSavedPosts()
}
```

### `MyPostsActivity.kt`
```kotlin
viewModel.errorMessage.observe(this) { error ->
    if (!error.isNullOrEmpty()) {
        tvEmpty.text = "Lỗi tải dữ liệu"
        tvEmpty.visibility = View.VISIBLE
        swipeRefreshLayout.isRefreshing = false
    }
}
```

### `SupportTicketDetailActivity.kt`
```kotlin
private var lastKnownMessageCount = 0

viewModel.messages.observe(this) { messages ->
    adapter.submitList(messages)
    if (messages.isNotEmpty()) {
        rvMessages.scrollToPosition(messages.size - 1)
        if (messages.size != lastKnownMessageCount) {
            lastKnownMessageCount = messages.size
            viewModel.markUserRead(ticketId)
        }
    }
}
```

### `SupportViewModel.kt`
```kotlin
fun clearCreatedTicketId() {
    _createdTicketId.postValue("")
}
```

### `MyPostDetailActivity.kt`
```kotlin
private var imagePageCallback: ViewPager2.OnPageChangeCallback? = null

imagePageCallback?.let { viewPager.unregisterOnPageChangeCallback(it) }
imagePageCallback = object : ViewPager2.OnPageChangeCallback() {
    override fun onPageSelected(position: Int) {
        tvCount.text = "${position + 1}/${images.size}"
    }
}
viewPager.registerOnPageChangeCallback(imagePageCallback!!)
```

## Snippet code đã fix
### `SavedPostsActivity.kt`
```kotlin
viewModel.deleteResult.observe(this) { _ ->
    swipeRefreshLayout.isRefreshing = false
    viewModel.loadSavedPosts()
}
```

### `MyPostsActivity.kt`
```kotlin
viewModel.errorMessage.observe(this) { error ->
    if (!error.isNullOrEmpty()) {
        tvEmpty.text = "Lỗi tải dữ liệu"
        tvEmpty.visibility = View.VISIBLE
        swipeRefreshLayout.isRefreshing = false
    }
}
```

### `SupportTicketDetailActivity.kt`
```kotlin
private var lastKnownMessageCount = 0

viewModel.messages.observe(this) { messages ->
    adapter.submitList(messages)
    if (messages.isNotEmpty()) {
        rvMessages.scrollToPosition(messages.size - 1)
        if (messages.size != lastKnownMessageCount) {
            lastKnownMessageCount = messages.size
            viewModel.markUserRead(ticketId)
        }
    }
}
```

### `SupportViewModel.kt`
```kotlin
fun clearCreatedTicketId() {
    _createdTicketId.postValue("")
}
```

### `MyPostDetailActivity.kt`
```kotlin
private var imagePageCallback: ViewPager2.OnPageChangeCallback? = null

imagePageCallback?.let { viewPager.unregisterOnPageChangeCallback(it) }
imagePageCallback = object : ViewPager2.OnPageChangeCallback() {
    override fun onPageSelected(position: Int) {
        tvCount.text = "${position + 1}/${images.size}"
    }
}
viewPager.registerOnPageChangeCallback(imagePageCallback!!)
```

## Trạng thái kiểm tra
- Các file đã chỉnh đều được rà lint cục bộ.
- Kết quả hiện tại các lần kiểm tra đều báo `No linter errors found`.

## Ghi chú cho lần sau
- Mỗi lần sửa app xong sẽ tiếp tục cập nhật mục này để bạn theo dõi thay đổi.
- Khi bạn hỏi “nãy giờ fix được lỗi gì”, mình sẽ tóm ngay từ file này để không phải đọc lại toàn bộ lịch sử.

---

## 2026-05-01 10:21:42 +07:00 - App Android: sửa lỗi compile/runtime/UI và kiểm chứng Gradle

### Nhóm lỗi build/logic đã sửa
- RoomDetailActivity.kt: sửa lỗi compile do dùng biến uid không tồn tại; thay bằng UID lấy từ FirebaseAuth, cập nhật lại currentUid, bỏ currentUid!! ở các luồng lưu bài, kiểm tra lịch hẹn, chat chủ trọ.
- MainActivity.kt: bỏ 
etworkCallback!!, dùng biến callback cục bộ để tránh NullPointerException; bọc unregister callback bằng 
unCatching; không log trực tiếp FCM token ra Logcat.
- MyPostDetailActivity.kt: bỏ imagePageCallback!!, đăng ký callback ViewPager bằng biến callback an toàn.
- EditPostActivity.kt: sửa parse giờ giới nghiêm bằng 
unCatching, giới hạn giờ/phút bằng coerceIn để tránh crash khi dữ liệu giờ cũ sai format.
- LocationPickerActivity.kt: bỏ Thread.sleep(600) trong geocoder Android 13+; đổi sang xử lý bằng callback thật của Geocoder, tránh race condition/chờ mò.
- RoomRepository.kt: thay catch rỗng khi xóa ảnh Storage bằng 
unCatching và log warning nếu URL ảnh không hợp lệ.
- PostFragment.kt: thay 
esources.getColor(...) deprecated bằng ContextCompat.getColor(...).
- EditPostViewModel.kt: đổi errorMessage thành LiveData<String?> vì code cần set 
ull để clear lỗi.
- SearchViewModel.kt: đổi errorMessage thành LiveData<String?> vì code cần set 
ull trước mỗi lần tìm kiếm.
- SplashActivity.kt: đổi cast progress animator từ s Int sang s? Int ?: 0 để tránh cast crash hiếm.

### Nhóm giao diện/text đã sửa
- Bỏ emoji trang trí khỏi tab, dialog, button, trạng thái và label để giao diện bớt quê, chuyên nghiệp hơn.
- ctivity_search_profile.xml: thay empty state emoji lớn bằng tiêu đề chữ rõ ràng Tìm người dùng, căn text gọn hơn.
- ctivity_user_profile.xml: bỏ emoji trong badge xác minh và nút nhắn tin.
- dialog_payment_qr.xml, dialog_search_address.xml, item_user_search.xml, strings.xml: bỏ emoji khỏi text hiển thị.
- MyAppointmentsActivity.kt, EditPostActivity.kt, PostFragment.kt: bỏ emoji khỏi tab, option dialog, thông báo chọn vị trí/ngày.
- MessageAdapter.kt: giữ chức năng thả cảm xúc nhưng giảm danh sách reaction còn 12 lựa chọn phổ biến, bỏ emoji khỏi menu chữ như Thả cảm xúc, Xóa tin nhắn, Lưu ảnh.

### Kiểm chứng đã chạy
- ./gradlew.bat assembleDebug: PASS sau khi sửa lỗi UID/compile.
- ./gradlew.bat testDebugUnitTest lintDebug: PASS sau khi sửa 3 lỗi lint NullSafeMutableLiveData.
- Quét pattern nguy hiểm bằng 
g: không còn !!, Thread.sleep, empty catch, emoji trang trí trong UI/text app.
- ADB: đã tìm thấy C:\Users\tiend\AppData\Local\Android\Sdk\platform-tools\adb.exe, nhưng hiện chưa có thiết bị/emulator kết nối (List of devices attached trống), nên chưa thể cài APK và thao tác UI thật trên máy ảo.

### Ghi chú còn theo dõi
- App vẫn còn nhiều warning lint dạng hardcoded text/RTL/dependency version. Đây là warning, không chặn build/lint; nếu cần chuẩn hóa tuyệt đối để nộp báo cáo, có thể tách thành phase riêng.
- Một số màn cũ vẫn gọi Firebase trực tiếp trong Activity/Fragment. Build/lint đã pass, nhưng nếu yêu cầu MVVM nghiêm ngặt 100%, cần refactor tiếp các luồng payment/chat/review/notification sang Repository/ViewModel theo từng màn để tránh làm vỡ chức năng hàng loạt.

---

## 2026-05-01 10:29:33 +07:00 - App Android: refactor một phần MyAppointmentsActivity theo MVVM

### Đã sửa
- AppointmentRepository.kt: thêm RoomRentedNotice, loadCurrentUserAppointmentAccess(...), listenRoomRentedNotices(...), markRoomRentedNoticeRead(...) để gom logic Firestore liên quan lịch hẹn/thông báo xuống Repository.
- BookingViewModel.kt: thêm AppointmentAccess, ppointmentAccess, 
oomRentedNotice, initializeAppointmentsScreen(), listener thông báo phòng đã thuê và hàm đánh dấu thông báo đã đọc.
- MyAppointmentsActivity.kt: bỏ gọi trực tiếp FirebaseAuth, FirebaseFirestore, ListenerRegistration, Query; Activity giờ chỉ observe ViewModel, dựng tab, render danh sách và hiển thị dialog.
- MyAppointmentsActivity.kt: setupDualTabs() clear tab/listener cũ trước khi add lại để tránh bị add trùng nếu observer emit lại.

### Kiểm chứng
- ./gradlew.bat assembleDebug: PASS sau refactor MVVM màn lịch hẹn.

### 2026-05-01 10:31:46 +07:00 - Verification cuối cho app sau refactor MVVM
- Đã dừng Gradle daemon bằng ./gradlew.bat --stop để tránh lock cache lint.
- ./gradlew.bat assembleDebug testDebugUnitTest: PASS.
- ./gradlew.bat lintDebug: PASS.
- Quét lại pattern nguy hiểm: không còn !!, Thread.sleep, empty catch, cast ép kiểu nguy hiểm kiểu s Long/String/Int, emoji trang trí trong UI/text.
- ADB có sẵn nhưng không có device/emulator đang kết nối, nên chưa thể cài APK và bấm test UI thật trên máy ảo.
