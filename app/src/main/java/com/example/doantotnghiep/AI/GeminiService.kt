package com.example.doantotnghiep.AI

import android.util.Log
import com.example.doantotnghiep.BuildConfig
import com.example.doantotnghiep.Model.AIMessage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SearchParams(
    val district: String? = null,
    val ward: String? = null,
    val minPrice: Long? = null,
    val maxPrice: Long? = null,
    val minArea: Int? = null,
    val maxArea: Int? = null,
    val roomType: String? = null,
    val genderPrefer: String? = null,
    val amenities: List<String> = emptyList(),
    val maxElectricPrice: Long? = null,
    val maxWaterPrice: Long? = null,
    val maxWifiPrice: Long? = null,
    val maxDepositMonths: Int? = null,
    val maxDepositAmount: Long? = null,
    val petAllowed: Boolean? = null,
    val freeTime: Boolean? = null,
    val maxPeopleCount: Int? = null,
    val addressQuery: String? = null,
    val radiusKm: Double? = null,
    val newestFirst: Boolean? = null,
    val daysAgo: Int? = null,
    val specificDate: String? = null,
    // === CÁC TRƯỜNG MỚI THÊM ===
    val minAvailableRooms: Int? = null,   // Số phòng còn trống tối thiểu
    val maxCurfewHour: Int? = null,        // Giờ đóng cửa muộn nhất (vd: 23 = trước 23h)
    val maxMotorbikeFee: Long? = null,     // Phí gửi xe máy tối đa (VNĐ/tháng)
    val maxBikeFee: Long? = null,          // Phí gửi xe đạp/xe điện tối đa (VNĐ/tháng)
    val hasParkingCar: Boolean? = null     // Có chỗ để ô tô không
)

sealed class ChatResult {
    data class Reply(val text: String) : ChatResult()
    data class RoomSearch(val params: SearchParams) : ChatResult()
    data class GeoSearch(val params: SearchParams) : ChatResult()
    data class Error(val message: String) : ChatResult()
}

class GeminiService {

    companion object {
        private const val MAX_HISTORY_TURNS = 40
    }

    private val apiKey = BuildConfig.GEMINI_API_KEY

    // Danh sách các model dự phòng xếp theo thứ tự ưu tiên (Quota nhiều nhất xếp trước)
    private val fallbackModels = listOf(
        "gemini-3.1-flash-lite", // 500 RPD, 15 RPM
        "gemini-2.5-flash-lite", // 20 RPD, 10 RPM
        "gemini-3.5-flash",      // 20 RPD, 5 RPM
        "gemini-3-flash",        // 20 RPD, 5 RPM
        "gemini-2.5-flash"       // 20 RPD, 5 RPM
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val history = mutableListOf<JSONObject>()

    private val systemInstruction = """
        Bạn là trợ lý hỗ trợ tìm kiếm phòng trọ của ứng dụng "Tìm Trọ 24/7" tại Hà Nội.
        Trả lời hoàn toàn bằng tiếng Việt, ngắn gọn, thân thiện.
        Nếu người dùng hỏi các vấn đề KHÔNG liên quan đến tìm phòng trọ (toán học, code, chính trị...), BẮT BUỘC TỪ CHỐI khéo léo và hướng họ về việc tìm phòng. KHÔNG gọi bất kỳ function nào.

        Khi người dùng muốn tìm phòng theo quận/phường, gọi function searchRooms.
        Khi người dùng muốn tìm phòng gần một địa điểm cụ thể + bán kính km, gọi function searchRoomsByLocation.
        Nếu người dùng chỉ nói "gần [địa điểm]" mà không nêu bán kính, CHỈ trả lời bằng văn bản để hỏi lại bán kính mong muốn. TUYỆT ĐỐI KHÔNG GỌI function searchRoomsByLocation khi chưa có thông số bán kính (radiusKm).

        LƯU Ý ĐỊA CHỈ: Hệ thống hiện tại gom chung tất cả Quận, Phường, Xã thành một cấp duy nhất là 'Phường/Xã'. Bất kể người dùng đọc tên Quận (Cầu Giấy, Đống Đa...) hay tên Phường/Xã (Phú Diễn...), LUÔN LUÔN điền tên đó vào biến 'ward'. Tuyệt đối KHÔNG dùng biến 'district'.

        ===== QUY ĐỔI TIỀN TỆ (TIẾNG LÓNG) =====
        'củ' = triệu = 1.000.000 (Ví dụ: '2 củ', '2tr' -> 2000000).
        'rưỡi' = phẩy 5 (Ví dụ: '2 triệu rưỡi', '2tr5' -> 2500000).
        'lít', 'k' = nghìn (Ví dụ: '500k', '500 lít' -> 500000).
        LUÔN quy đổi ra con số nguyên VNĐ khi gán vào các biến giá tiền.

        QUY TẮC LỌC GIÁ TIỀN (minPrice / maxPrice):
        - "dưới X", "nhỏ hơn X", "tối đa X", "tầm X đổ lại": Gán maxPrice = X.
        - "trên X", "lớn hơn X", "từ X trở lên": Gán minPrice = X.
        - "từ X đến Y": Gán minPrice = X, maxPrice = Y.
        - "khoảng X", "tầm X": Gán minPrice = X - 500000, maxPrice = X + 500000 (tự động cho dao động 500k).


        ===== MAPPING LOẠI PHÒNG → roomType =====
        "phòng trọ"/"phòng thuê" → "Phòng trọ"
        "chung cư mini"/"căn hộ mini" → "Chung cư mini"
        "homestay"/"nhà homestay" → "Homestay"
        "nhà nguyên căn"/"nhà riêng"/"thuê cả nhà" → "Nhà nguyên căn"
        "căn hộ dịch vụ"/"serviced apartment" → "Căn hộ dịch vụ"
        QUAN TRỌNG: Điền roomType khi người dùng nêu rõ loại phòng muốn thuê.

        ===== MAPPING TIỆN ÍCH → KEY (chỉ điền khi người dùng YÊU CẦU CỤ THỂ) =====
        QUAN TRỌNG VỀ PHỦ ĐỊNH: NẾU người dùng có ý PHỦ ĐỊNH (ví dụ: 'không cần điều hòa', 'không quan trọng máy giặt'), TUYỆT ĐỐI BỎ QUA tiện ích đó, KHÔNG được gán giá trị vào function call.

        [Điện - Nước - Mạng]
        wifi/internet → "wifi"
        bình nóng lạnh/máy nước nóng → "waterHeater"

        [Điều hòa - Máy móc]
        điều hòa/máy lạnh → "airConditioner"
        máy giặt → "washer"

        [Chỗ để xe - QUAN TRỌNG: phân biệt rõ loại xe]
        để xe máy/xe gắn máy → "motorbike"
        để xe đạp điện/xe điện → "ebike"
        để xe đạp → "bicycle"
        để ô tô → "parking"
        "chỗ để xe" (không rõ loại) → "motorbike" (mặc định xe máy tại VN)

        [Nội thất - Phòng ốc]
        giường/có giường → "bed"
        tủ quần áo/tủ đồ → "wardrobe"
        bếp riêng/nhà bếp riêng → "kitchen"
        WC riêng/nhà vệ sinh riêng → "privateWC"
        ban công/sân phơi/chỗ phơi đồ → "balcony"
        nội thất đầy đủ/có đồ → "furniture"
        tủ lạnh → "refrigerator"
        tivi/TV → "tv"

        [Thú cưng]
        "nuôi thú cưng"/"nuôi mèo"/"nuôi chó"/"có thú cưng"/... → petAllowed=true
        "không nuôi thú cưng"/"không có thú cưng" → petAllowed=false
        Lưu ý: petAllowed chỉ lọc phòng THEO TIÊU CHÍ. Chatbot KHÔNG biết chi tiết loại thú/số lượng
        của từng phòng cụ thể — hãy gợi ý user liên hệ chủ trọ để hỏi thêm chi tiết.

        ===== CÁC PARAM KHÁC =====
        genderPrefer: "Nam", "Nữ", "Tất cả" — chỉ điền khi người dùng nêu yêu cầu giới tính.
        maxPeopleCount: số người tối đa — điền khi người dùng hỏi "phòng cho N người".
        freeTime=true: chỉ khi người dùng nói "tự do"/"không giới nghiêm"/"24/24"/"không giờ giấc".
        maxDepositMonths: số tháng cọc tối đa.
        maxDepositAmount: tiền cọc tối đa bằng VNĐ.
        maxElectricPrice: giá điện tối đa VNĐ/kWh.
        maxWaterPrice: giá nước tối đa VNĐ/m³.
        maxWifiPrice: giá wifi tối đa VNĐ/tháng.

        [Số phòng còn trống]
        minAvailableRooms: số phòng còn trống tối thiểu — điền khi người dùng hỏi "còn N phòng trống",
        "phòng còn chỗ", "dãy trọ còn phòng trống"...
        Ví dụ: "tìm nhà trọ còn ít nhất 2 phòng" → minAvailableRooms=2

        [Giờ giấc / Giờ đóng cửa]
        maxCurfewHour: số giờ đóng cửa muộn nhất (theo hệ 24 giờ).
        Ví dụ: "không đóng cửa trước 23h" → maxCurfewHour=23
                "đóng cửa sau 22h" → maxCurfewHour=22
                "không giới nghiêm"/"tự do" → freeTime=true (KHÔNG dùng maxCurfewHour)

        [Phí gửi xe]
        maxMotorbikeFee: phí gửi xe máy tối đa VNĐ/tháng.
        Ví dụ: "phí xe máy dưới 100k" → maxMotorbikeFee=100000
        maxBikeFee: phí gửi xe đạp/xe đạp điện tối đa VNĐ/tháng.

        [Thời gian đăng bài]
        - newestFirst: điền true khi người dùng hỏi "mới nhất", "vừa đăng", "gần đây nhất", "mới nhất có thể".
        - daysAgo: điền số ngày N khi người dùng yêu cầu bài đăng trong khoảng ngày (vd: "hôm nay"/"trong 24h qua" -> 1, "3 ngày qua" -> 3, "tuần qua" -> 7).
        - specificDate: điền ngày cụ thể định dạng "DD/MM/YYYY" khi người dùng yêu cầu ngày cụ thể (vd: "ngày 9/6/2026" -> "09/06/2026"). Hãy tính toán ngày hiện tại được cung cấp ở cuối chỉ thị để quy đổi các từ như "hôm qua" sang ngày cụ thể dạng DD/MM/YYYY.

        ===== CÁC TRƯỜNG CHỈ ĐỌC (KHÔNG DÙNG ĐỂ LỌC — TRẢ LỜI BẰNG CHỮ) =====
        Một số thông tin người dùng hay hỏi nhưng KHÔNG thể lọc qua function call.
        Khi gặp các câu hỏi này, hãy trả lời thân thiện bằng văn bản thay vì gọi function:

        - Hỏi về hạn bài đăng (postExpiryDate): Khẳng định các phòng hiển thị đều CÒN HẠN.
          Khuyên người dùng nhấn xem chi tiết để biết ngày hết hạn cụ thể.
        - Hỏi về số ngày đăng bài (postDurationDays): Giải thích bài đăng thường có hiệu lực 30 ngày.
          Khuyên nhấn xem chi tiết để biết chính xác ngày hết hạn.
        - Hỏi về thông tin chủ nhà (ownerName, ownerPhone, ownerGender): Chatbot không hiển thị
          số điện thoại chủ nhà trực tiếp. Hướng dẫn người dùng nhấn vào phòng trọ để xem thông tin
          liên hệ và gọi trực tiếp cho chủ nhà.
        - Hỏi về lịch hẹn xem phòng (availableTimeSlots, maxDailyAppointments, appointmentNotice,
          totalAppointments, activeAppointments): Chatbot không quản lý lịch hẹn trực tiếp. Hướng dẫn
          người dùng nhấn vào phòng trọ, chọn lịch trống rồi bấm "Đặt lịch hẹn" trên ứng dụng.
        - Hỏi về chính sách thú cưng cụ thể (nuôi chó gì, mèo giống gì): Chatbot chỉ biết phòng
          có cho phép nuôi thú cưng hay không (True/False). Hướng dẫn liên hệ chủ nhà để hỏi chi tiết.
        - Hỏi về số phòng đã cho thuê (rentedCount): Chatbot sẽ tìm phòng còn trống dựa vào
          tổng số phòng trừ đi số đã thuê. Khuyên liên hệ chủ nhà để xác nhận thực tế.

        QUAN TRỌNG: KHÔNG tự động điền các param không được người dùng đề cập rõ ràng.
        Nếu hỏi ngoài chủ đề tìm phòng trọ tại Hà Nội, từ chối lịch sự và hướng về tìm phòng.
    """.trimIndent()

    private val tools: JSONArray by lazy {
        JSONArray().apply {
            put(JSONObject().apply {
                put("functionDeclarations", JSONArray().apply {
                    put(buildSearchRoomsDeclaration())
                    put(buildSearchByLocationDeclaration())
                })
            })
        }
    }

    private fun buildSearchRoomsDeclaration() = JSONObject().apply {
        put("name", "searchRooms")
        put("description", "Tìm phòng trọ theo quận/phường và các tiêu chí lọc.")
        put("parameters", JSONObject().apply {
            put("type", "OBJECT")
            put("properties", JSONObject().apply {
                // Vị trí (Gom chung quận/phường/xã)
                put("ward", prop("STRING", "Tên khu vực (Quận/Huyện/Phường/Xã) tại Hà Nội (VD: Cầu Giấy, Đống Đa, Phú Diễn). BẮT BUỘC điền mọi địa danh vào biến này."))

                // Loại phòng
                put("roomType", prop("STRING", "Loại phòng: 'Phòng trọ', 'Chung cư mini', 'Homestay', 'Nhà nguyên căn', 'Căn hộ dịch vụ' — chỉ điền khi người dùng nêu rõ"))
                // Giá
                put("minPrice", prop("INTEGER", "Giá thuê tối thiểu VNĐ/tháng, chỉ điền khi người dùng nêu rõ"))
                put("maxPrice", prop("INTEGER", "Giá thuê tối đa VNĐ/tháng, chỉ điền khi người dùng nêu rõ"))
                // Diện tích
                put("minArea", prop("INTEGER", "Diện tích tối thiểu m², chỉ điền khi người dùng nêu rõ"))
                put("maxArea", prop("INTEGER", "Diện tích tối đa m², chỉ điền khi người dùng nêu rõ"))
                // Giới tính
                put("genderPrefer", prop("STRING", "Giới tính ưu tiên: Nam, Nữ, Tất cả — chỉ điền khi người dùng nêu rõ"))
                // Số người
                put("maxPeopleCount", prop("INTEGER", "Số người ở tối đa — điền khi người dùng hỏi 'phòng cho N người'"))
                // Số phòng còn trống
                put("minAvailableRooms", prop("INTEGER", "Số phòng còn trống tối thiểu — điền khi người dùng hỏi 'còn N phòng', 'còn phòng trống', 'dãy trọ còn chỗ'"))
                // Tiện ích
                put("amenities", JSONObject().apply {
                    put("type", "ARRAY")
                    put("items", JSONObject().put("type", "STRING"))
                    put("description", """Danh sách key tiện ích người dùng YÊU CẦU CỤ THỂ. Để trống nếu không đề cập.
                        Keys hợp lệ: wifi, airConditioner, washer, motorbike, ebike, bicycle, parking,
                        bed, wardrobe, kitchen, privateWC, balcony, furniture, refrigerator, tv, security, waterHeater""")
                })
                // Chi phí dịch vụ
                put("maxElectricPrice", prop("INTEGER", "Giá điện tối đa VNĐ/kWh, chỉ điền khi người dùng nêu rõ"))
                put("maxWaterPrice", prop("INTEGER", "Giá nước tối đa VNĐ/m³, chỉ điền khi người dùng nêu rõ"))
                put("maxWifiPrice", prop("INTEGER", "Giá wifi tối đa VNĐ/tháng, chỉ điền khi người dùng nêu rõ"))
                // Phí gửi xe
                put("maxMotorbikeFee", prop("INTEGER", "Phí gửi xe máy tối đa VNĐ/tháng, chỉ điền khi người dùng hỏi về phí gửi xe máy"))
                put("maxBikeFee", prop("INTEGER", "Phí gửi xe đạp/xe đạp điện tối đa VNĐ/tháng, chỉ điền khi người dùng hỏi về phí xe đạp/xe điện"))
                // Đặt cọc
                put("maxDepositMonths", prop("INTEGER", "Số tháng cọc tối đa, chỉ điền khi người dùng nêu rõ"))
                put("maxDepositAmount", prop("INTEGER", "Tiền cọc tối đa VNĐ, chỉ điền khi người dùng nêu số tiền cọc cụ thể"))
                // Điều kiện ở
                put("petAllowed", prop("BOOLEAN", "Cho nuôi thú cưng, chỉ điền khi người dùng hỏi về thú cưng"))
                put("freeTime", prop("BOOLEAN", "Giờ giấc tự do/không giới nghiêm hoàn toàn, chỉ điền khi người dùng nói rõ 'không giới nghiêm','tự do','24/24'"))
                put("maxCurfewHour", prop("INTEGER", "Giờ đóng cửa muộn nhất theo hệ 24h (ví dụ: 23 = trước 23h). Điền khi người dùng nói 'không đóng trước Xh', 'đóng cửa sau Xh'. KHÔNG dùng khi người dùng nói 'tự do'/'không giới nghiêm' (dùng freeTime thay thế)"))
                // Lọc theo thời gian đăng bài
                put("newestFirst", prop("BOOLEAN", "Sắp xếp bài đăng mới nhất lên đầu, chỉ điền khi người dùng yêu cầu 'mới nhất', 'gần đây nhất'"))
                put("daysAgo", prop("INTEGER", "Chỉ tìm bài đăng trong vòng N ngày qua (ví dụ: hôm nay/trong 24h qua = 1, 3 ngày qua = 3, tuần qua = 7)"))
                put("specificDate", prop("STRING", "Chỉ tìm bài đăng đúng ngày cụ thể. Định dạng bắt buộc: 'DD/MM/YYYY' (ví dụ: '09/06/2026'). Hãy tự phân tích từ câu chat của user dựa trên ngày hiện tại trong system instruction."))
            })
        })
    }

    private fun buildSearchByLocationDeclaration() = JSONObject().apply {
        put("name", "searchRoomsByLocation")
        put("description", "Tìm phòng gần một địa điểm cụ thể theo bán kính km.")
        put("parameters", JSONObject().apply {
            put("type", "OBJECT")
            put("required", JSONArray().apply { put("addressQuery"); put("radiusKm") })
            put("properties", JSONObject().apply {
                put("addressQuery", prop("STRING", "Tên địa điểm đầy đủ bằng tiếng Việt, thêm ', Hà Nội' nếu chưa có. Ví dụ: 'Đại học Bách Khoa Hà Nội', 'Học viện Công nghệ Bưu chính Viễn thông, Hà Nội'"))
                put("radiusKm", prop("NUMBER", "Bán kính km người dùng chỉ định (vd: 1.0, 2.5, 5.0)"))
                // Loại phòng
                put("roomType", prop("STRING", "Loại phòng: 'Phòng trọ', 'Chung cư mini', 'Homestay', 'Nhà nguyên căn', 'Căn hộ dịch vụ' — chỉ điền khi người dùng nêu rõ"))
                // Giá
                put("minPrice", prop("INTEGER", "Giá thuê tối thiểu VNĐ/tháng, chỉ điền khi người dùng nêu rõ"))
                put("maxPrice", prop("INTEGER", "Giá thuê tối đa VNĐ/tháng, chỉ điền khi người dùng nêu rõ"))
                // Diện tích
                put("minArea", prop("INTEGER", "Diện tích tối thiểu m², chỉ điền khi người dùng nêu rõ"))
                put("maxArea", prop("INTEGER", "Diện tích tối đa m², chỉ điền khi người dùng nêu rõ"))
                // Giới tính & số người
                put("genderPrefer", prop("STRING", "Giới tính ưu tiên: Nam, Nữ, Tất cả — chỉ điền khi người dùng nêu rõ"))
                put("maxPeopleCount", prop("INTEGER", "Số người ở tối đa — điền khi người dùng hỏi 'phòng cho N người'"))
                // Số phòng còn trống
                put("minAvailableRooms", prop("INTEGER", "Số phòng còn trống tối thiểu — điền khi người dùng hỏi 'còn N phòng', 'còn phòng trống'"))
                // Tiện ích
                put("amenities", JSONObject().apply {
                    put("type", "ARRAY")
                    put("items", JSONObject().put("type", "STRING"))
                    put("description", """Danh sách key tiện ích người dùng YÊU CẦU CỤ THỂ. Để trống nếu không đề cập.
                        Keys hợp lệ: wifi, airConditioner, washer, motorbike, ebike, bicycle, parking,
                        bed, wardrobe, kitchen, privateWC, balcony, furniture, refrigerator, tv, security, waterHeater""")
                })
                // Chi phí dịch vụ
                put("maxElectricPrice", prop("INTEGER", "Giá điện tối đa VNĐ/kWh, chỉ điền khi người dùng nêu rõ"))
                put("maxWaterPrice", prop("INTEGER", "Giá nước tối đa VNĐ/m³, chỉ điền khi người dùng nêu rõ"))
                // Phí gửi xe
                put("maxMotorbikeFee", prop("INTEGER", "Phí gửi xe máy tối đa VNĐ/tháng, chỉ điền khi người dùng hỏi về phí gửi xe máy"))
                put("maxBikeFee", prop("INTEGER", "Phí gửi xe đạp/xe đạp điện tối đa VNĐ/tháng"))
                // Đặt cọc
                put("maxDepositMonths", prop("INTEGER", "Số tháng cọc tối đa, chỉ điền khi người dùng nêu rõ"))
                put("maxDepositAmount", prop("INTEGER", "Tiền cọc tối đa VNĐ, chỉ điền khi người dùng nêu số tiền cọc cụ thể"))
                // Điều kiện ở
                put("petAllowed", prop("BOOLEAN", "Cho nuôi thú cưng, chỉ điền khi người dùng hỏi về thú cưng"))
                put("freeTime", prop("BOOLEAN", "Giờ giấc tự do/không giới nghiêm hoàn toàn"))
                put("maxCurfewHour", prop("INTEGER", "Giờ đóng cửa muộn nhất theo hệ 24h. Điền khi người dùng nói 'không đóng trước Xh'"))
                // Lọc theo thời gian đăng bài
                put("newestFirst", prop("BOOLEAN", "Sắp xếp bài đăng mới nhất lên đầu"))
                put("daysAgo", prop("INTEGER", "Chỉ tìm bài đăng trong vòng N ngày qua"))
                put("specificDate", prop("STRING", "Chỉ tìm bài đăng đúng ngày cụ thể. Định dạng bắt buộc: 'DD/MM/YYYY'"))
            })
        })
    }

    private fun prop(type: String, description: String) =
        JSONObject().put("type", type).put("description", description)

    @Synchronized
    fun clearHistory() { history.clear() }

    @Synchronized
    fun restoreHistory(messages: List<AIMessage>) {
        history.clear()
        for (msg in messages) {
            if (msg.role != "user" && msg.role != "model") continue
            if (msg.content.isBlank()) continue
            history.add(JSONObject().apply {
                put("role", msg.role)
                put("parts", JSONArray().apply { put(JSONObject().put("text", msg.content)) })
            })
        }
        trimHistory()
    }

    @Synchronized
    fun processUserMessage(userText: String): ChatResult {
        history.add(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply { put(JSONObject().put("text", userText)) })
        })
        return callGeminiWithFallback()
    }

    private fun callGeminiWithFallback(): ChatResult {
        var lastError: ChatResult.Error = ChatResult.Error("Không thể kết nối AI. Kiểm tra mạng và thử lại.")

        for (modelName in fallbackModels) {
            val result = callSingleModelWithRetry(modelName)

            // Nếu thành công, trả về kết quả ngay
            if (result !is ChatResult.Error) return result

            // Nếu gặp lỗi 429 (Rate Limit / Hết Quota), ghi log và lặp sang model tiếp theo
            if (result.message.contains("quá tải") || result.message.contains("bận")) {
                Log.w("GeminiService", "Model $modelName hết Quota hoặc Rate Limit, tự động chuyển sang model dự phòng...")
                lastError = result
                continue
            }

            // Nếu là các lỗi khác (không phải lỗi mạng tạm thời) -> thoát và báo lỗi luôn
            if (!result.message.startsWith("Không thể kết nối")) {
                return result
            }

            lastError = result
        }

        // Nếu tất cả các model đều hết quota
        return ChatResult.Error("Tất cả các trợ lý AI đều đang quá tải (Hết Quota). Vui lòng đợi 1 phút rồi thử lại.")
    }

    private fun callSingleModelWithRetry(modelName: String, maxAttempts: Int = 3): ChatResult {
        var lastError: ChatResult.Error = ChatResult.Error("Không thể kết nối AI. Kiểm tra mạng và thử lại.")
        for (attempt in 1..maxAttempts) {
            val result = callGeminiAPI(modelName)
            if (result !is ChatResult.Error) return result

            // Chỉ retry cho lỗi mạng tạm thời
            if (!result.message.startsWith("Không thể kết nối")) return result

            lastError = result
            if (attempt < maxAttempts) {
                Thread.sleep(1000L * (1 shl (attempt - 1))) // 1s, 2s
            }
        }
        return lastError
    }

    private fun callGeminiAPI(modelName: String): ChatResult {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("vi"))
        sdf.timeZone = java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
        val todayStr = sdf.format(java.util.Date())
        val dynamicInstruction = systemInstruction + "\n\nQUAN TRỌNG: Hôm nay là ngày $todayStr (giờ Hà Nội). Hãy dùng ngày này để tính toán các ngày tương đối như 'hôm qua' hoặc ngày cụ thể khác."

        val body = JSONObject().apply {
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().apply { put(JSONObject().put("text", dynamicInstruction)) })
            })
            put("contents", JSONArray(history))
            put("tools", tools)
            put("generationConfig", JSONObject().apply {
                put("thinkingConfig", JSONObject().apply {
                    put("thinkingBudget", 0)
                })
            })
        }.toString()

        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(endpoint)
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            val response = client.newCall(request).execute()
            val bodyStr = response.body?.string()
                ?: return ChatResult.Error("Không nhận được phản hồi từ máy chủ.")
            if (!response.isSuccessful) {
                Log.e("GeminiService", "HTTP ${response.code} — model: $modelName — body: $bodyStr")
                if (response.code == 429) {
                    return ChatResult.Error("Trợ lý AI đang bận") // Trả về thông báo chứa chữ "bận" để kích hoạt logic fallback
                }
                if (response.code == 503) {
                    return ChatResult.Error("Không thể kết nối AI. Kiểm tra mạng và thử lại.")
                }
                return ChatResult.Error("Lỗi kết nối (${response.code}). Vui lòng thử lại.")
            }
            parseResponse(bodyStr)
        } catch (e: Exception) {
            Log.e("GeminiService", "API call failed for model $modelName", e)
            ChatResult.Error("Không thể kết nối AI. Kiểm tra mạng và thử lại.")
        }
    }

    private fun parseResponse(body: String): ChatResult {
        return try {
            val json = JSONObject(body)
            val candidates = json.optJSONArray("candidates")
                ?: return ChatResult.Error("Phản hồi không hợp lệ.")
            if (candidates.length() == 0) return ChatResult.Error("AI không có câu trả lời.")

            val content = candidates.getJSONObject(0).optJSONObject("content")
                ?: return ChatResult.Error("Nội dung phản hồi rỗng.")
            val parts = content.optJSONArray("parts")
                ?: return ChatResult.Error("Không có nội dung.")

            // Ghi lại lượt model vào history, giới hạn n lượt trao đổi
            history.add(JSONObject(content.toString()).put("role", "model"))
            trimHistory()

            // Kiểm tra function call trước
            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                val funcCall = part.optJSONObject("functionCall") ?: continue
                val name = funcCall.optString("name")
                val args = funcCall.optJSONObject("args") ?: JSONObject()
                val params = parseSearchParams(args)
                return when (name) {
                    "searchRoomsByLocation" -> ChatResult.GeoSearch(params)
                    "searchRooms" -> ChatResult.RoomSearch(params)
                    else -> ChatResult.Error("Hàm không xác định: $name")
                }
            }

            // Trả lời văn bản
            val text = buildString {
                for (i in 0 until parts.length()) {
                    val t = parts.getJSONObject(i).optString("text")
                    if (t.isNotEmpty()) append(t)
                }
            }
            if (text.isNotEmpty()) ChatResult.Reply(text)
            else ChatResult.Error("Không có phản hồi văn bản.")
        } catch (e: Exception) {
            Log.e("GeminiService", "Parse error: $body", e)
            ChatResult.Error("Lỗi xử lý phản hồi AI.")
        }
    }

    private fun trimHistory() {
        val maxMessages = MAX_HISTORY_TURNS * 2
        while (history.size > maxMessages) history.removeAt(0)
    }

    private fun parseSearchParams(args: JSONObject): SearchParams {
        val amenitiesArr = args.optJSONArray("amenities")
        val amenities = if (amenitiesArr != null) {
            (0 until amenitiesArr.length()).map { amenitiesArr.getString(it) }
        } else emptyList()

        return SearchParams(
            district = args.optString("district").takeIf { it.isNotEmpty() },
            ward = args.optString("ward").takeIf { it.isNotEmpty() },
            minPrice = if (args.has("minPrice")) args.getLong("minPrice") else null,
            maxPrice = if (args.has("maxPrice")) args.getLong("maxPrice") else null,
            minArea = if (args.has("minArea")) args.getInt("minArea") else null,
            maxArea = if (args.has("maxArea")) args.getInt("maxArea") else null,
            roomType = args.optString("roomType").takeIf { it.isNotEmpty() },
            genderPrefer = args.optString("genderPrefer").takeIf { it.isNotEmpty() },
            amenities = amenities,
            maxElectricPrice = if (args.has("maxElectricPrice")) args.getLong("maxElectricPrice") else null,
            maxWaterPrice = if (args.has("maxWaterPrice")) args.getLong("maxWaterPrice") else null,
            maxWifiPrice = if (args.has("maxWifiPrice")) args.getLong("maxWifiPrice") else null,
            maxDepositMonths = if (args.has("maxDepositMonths")) args.getInt("maxDepositMonths") else null,
            maxDepositAmount = if (args.has("maxDepositAmount")) args.getLong("maxDepositAmount") else null,
            petAllowed = if (args.has("petAllowed")) args.getBoolean("petAllowed") else null,
            freeTime = if (args.has("freeTime")) args.getBoolean("freeTime") else null,
            maxPeopleCount = if (args.has("maxPeopleCount")) args.getInt("maxPeopleCount") else null,
            addressQuery = args.optString("addressQuery").takeIf { it.isNotEmpty() },
            radiusKm = if (args.has("radiusKm")) args.getDouble("radiusKm") else null,
            newestFirst = if (args.has("newestFirst")) args.getBoolean("newestFirst") else null,
            daysAgo = if (args.has("daysAgo")) args.getInt("daysAgo") else null,
            specificDate = args.optString("specificDate").takeIf { it.isNotEmpty() },
            minAvailableRooms = if (args.has("minAvailableRooms")) args.getInt("minAvailableRooms") else null,
            maxCurfewHour = if (args.has("maxCurfewHour")) args.getInt("maxCurfewHour") else null,
            maxMotorbikeFee = if (args.has("maxMotorbikeFee")) args.getLong("maxMotorbikeFee") else null,
            maxBikeFee = if (args.has("maxBikeFee")) args.getLong("maxBikeFee") else null
        )
    }
}
