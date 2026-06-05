package com.example.doantotnghiep.AI

import android.content.Context
import com.example.doantotnghiep.Utils.AddressData
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import kotlin.math.ln
import kotlin.math.sqrt

class ChatbotEngine(private val context: Context) {

    data class Intent(
        val id: String,
        val patterns: List<String>,
        val responses: List<String>,
        val deepLink: String? = null
    ) {
        fun randomResponse(): String = responses.random()
    }

    enum class ConversationState { IDLE, AWAIT_DISTRICT, AWAIT_WARD, AWAIT_PRICE, AWAIT_RADIUS, AWAIT_PRICE_FOR_GEO }

    data class ConversationContext(
        val state: ConversationState = ConversationState.IDLE,
        val district: String? = null,
        val maxPrice: Long? = null,
        val minPrice: Long? = null,
        val ward: String? = null,
        val lastSearchDistrict: String? = null,
        val lastSearchMaxPrice: Long? = null,
        val lastSearchMinPrice: Long? = null,
        val lastSearchWard: String? = null,
        val lastUserQuery: String? = null,
        val lastResultCount: Int = -1,
        // --- Geo search fields ---
        val pendingGeoAddress: String? = null,   // địa chỉ text đang chờ hỏi bán kính
        val pendingRadiusKm: Double? = null,     // bán kính đang chờ hỏi giá
        val lastSearchAddressQuery: String? = null,
        val lastSearchRadiusKm: Double? = null,

        // --- Advanced search fields ---
        val amenities: List<String> = emptyList(),
        val minArea: Int? = null,
        val maxArea: Int? = null,
        val roomType: String? = null,
        val genderPrefer: String? = null,
        val maxElectricPrice: Long? = null,
        val maxWaterPrice: Long? = null,
        val maxWifiPrice: Long? = null,
        val maxDepositMonths: Int? = null,
        val petAllowed: Boolean? = null,
        val freeTime: Boolean? = null,

        // --- Remember last advanced query for "Tìm lại" ---
        val lastSearchAmenities: List<String> = emptyList(),
        val lastSearchMinArea: Int? = null,
        val lastSearchMaxArea: Int? = null,
        val lastSearchRoomType: String? = null,
        val lastSearchGenderPrefer: String? = null,
        val lastSearchMaxElectricPrice: Long? = null,
        val lastSearchMaxWaterPrice: Long? = null,
        val lastSearchMaxWifiPrice: Long? = null,
        val lastSearchMaxDepositMonths: Int? = null,
        val lastSearchPetAllowed: Boolean? = null,
        val lastSearchFreeTime: Boolean? = null
    )

    data class SearchParams(
        val district: String? = null,
        val maxPrice: Long? = null,
        val minPrice: Long? = null,
        val amenities: List<String> = emptyList(),
        val ward: String? = null,
        // --- Geo search fields ---
        val addressQuery: String? = null,  // địa chỉ cần geocode
        val radiusKm: Double? = null,      // bán kính tìm kiếm (km)

        // --- Advanced search fields ---
        val minArea: Int? = null,
        val maxArea: Int? = null,
        val roomType: String? = null,
        val genderPrefer: String? = null,
        val maxElectricPrice: Long? = null,
        val maxWaterPrice: Long? = null,
        val maxWifiPrice: Long? = null,
        val maxDepositMonths: Int? = null,
        val petAllowed: Boolean? = null,
        val freeTime: Boolean? = null,
        val maxPeopleCount: Int? = null   // "phòng cho 2 người"
    )

    data class ChatbotResponse(
        val answer: String,
        val confidence: Double,
        val deepLink: String? = null,
        val quickReplies: List<String> = emptyList(),
        val searchParams: SearchParams? = null,
        val nextContext: ConversationContext = ConversationContext()
    )

    private val intents = mutableListOf<Intent>()
    private val idfScores = mutableMapOf<String, Double>()
    private val patternVectors = mutableListOf<Pair<String, Map<String, Double>>>()
    private val uiResponses = mutableMapOf<String, List<String>>()

    private val avgPatternLength: Double by lazy {
        val allPatterns = intents.flatMap { it.patterns }
        if (allPatterns.isEmpty()) 5.0
        else allPatterns.sumOf { baseTokenize(it).size } / allPatterns.size.toDouble()
    }

    companion object {
        private const val BASE_THRESHOLD = 0.15
        private const val MAX_INPUT_LENGTH = 150
        private const val BM25_K1 = 1.5
        private const val BM25_B = 0.75
        // Fix 6: sentinel for "no more results" signal inside resolveReferences
        private const val NO_MORE_RESULTS_SENTINEL = "__no_more_results__"

        val QUICK_REPLY_ACTIONS = mapOf(
            "🔍 Tìm phòng ngay" to "tìm phòng trọ",
            "📝 Đăng tin mới" to "đăng tin phòng",
            "📞 Hỗ trợ trực tiếp" to "liên hệ hỗ trợ",
            "🔍 Tìm lại" to "tìm lại",
            "Đổi khu vực" to "đổi khu vực",
            "Đổi mức giá" to "đổi mức giá",
            "Lọc theo giá" to "đổi mức giá",
            "Đổi bán kính" to "đổi bán kính"
        )

        private val FALLBACK_QUICK_REPLIES = listOf("🔍 Tìm phòng ngay", "📝 Đăng tin mới", "📞 Hỗ trợ trực tiếp")

        private const val FALLBACK_RESPONSE =
            "Xin lỗi, mình chưa hiểu câu hỏi của bạn.\nBạn có thể hỏi về:\n" +
            "• Cách tìm kiếm phòng\n" +
            "• Đặt lịch hẹn xem phòng\n" +
            "• Đăng tin phòng trọ\n" +
            "• Các tính năng khác của ứng dụng\n" +
            "Hoặc gõ 'menu' để xem danh sách đầy đủ!"

        private val VIETNAMESE_STOPWORDS = setOf(
            "tôi", "mình", "bạn", "của", "là", "và", "với", "có", "này",
            "đó", "thì", "mà", "để", "được", "cho", "từ", "trong", "về",
            "ra", "vào", "lên", "xuống", "đến", "đi", "lại", "thế", "nào",
            "như", "vậy", "rất", "cũng", "đã", "sẽ", "đang", "hay", "hoặc",
            "nhưng", "vì", "nên", "khi", "nếu", "theo", "sau", "trước",
            "ở", "tại", "qua", "các", "một", "nhiều", "ít", "hơn", "nhất",
            "ai", "gì", "sao", "đâu", "bao", "mấy", "lắm", "quá", "ạ",
            "nhé", "nha", "ơi", "uh", "ừ", "ok", "okay", "vẫn", "luôn"
        )

        private val SYNONYM_GROUPS = mapOf(
            "phòng trọ" to listOf("nhà trọ", "căn trọ", "chỗ ở", "phòng thuê", "phòng cho thuê", "phòng ở", "chỗ thuê"),
            "đặt lịch" to listOf("book lịch", "hẹn xem phòng", "lên lịch", "đặt hẹn", "xem phòng", "hẹn xem"),
            "chủ trọ" to listOf("người cho thuê", "chủ nhà", "chủ phòng"),
            "người thuê" to listOf("khách thuê", "người tìm phòng", "người thuê nhà"),
            "tiện ích" to listOf("tiện nghi", "nội thất", "cơ sở vật chất", "trang thiết bị"),
            "giá thuê" to listOf("giá phòng", "chi phí thuê", "tiền thuê", "mức giá"),
            "hủy lịch" to listOf("cancel lịch", "bỏ lịch", "hủy hẹn", "hủy đặt lịch"),
            "thông báo" to listOf("notification", "nhắc nhở", "cảnh báo"),
            "tìm kiếm" to listOf("tìm phòng", "tra cứu phòng", "lọc phòng", "search phòng")
        )

        private val CHEAPER_KEYWORDS = setOf(
            "rẻ hơn", "giá thấp hơn", "ít tiền hơn", "rẻ hơn nữa",
            "giảm giá", "giá rẻ hơn", "thấp hơn", "bớt tiền hơn"
        )
        private val EXPENSIVE_KEYWORDS = setOf(
            "đắt hơn", "giá cao hơn", "nhiều tiền hơn", "cao hơn",
            "xịn hơn", "tốt hơn", "chất hơn"
        )
        private val SAME_AREA_KEYWORDS = setOf(
            "khu vực đó", "chỗ đó", "khu đó", "khu vực đấy",
            "chỗ đấy", "ở đó", "vẫn khu đó", "khu vực kia",
            "chỗ kia", "khu đấy", "khu vực vừa tìm"
        )
        private val MORE_RESULTS_KEYWORDS = setOf(
            "xem thêm", "tìm thêm", "còn phòng nào không", "phòng khác",
            "có thêm không", "còn không", "nhiều hơn", "thêm phòng"
        )

        private val NORMALIZATION_DICT = mapOf(
            // phòng trọ
            "ptro" to "phòng trọ", "phongtro" to "phòng trọ", "ptr" to "phòng trọ",
            "nhatho" to "nhà trọ", "nt" to "nhà trọ",
            // phủ định / trạng thái
            "ko" to "không", "k" to "không", "khg" to "không", "hk" to "không",
            "đc" to "được", "dc" to "được", "duoc" to "được",
            // giá tiền
            "tr" to "triệu", "trieu" to "triệu", "m" to "triệu",
            // người
            "bn" to "bạn", "mk" to "mình", "vs" to "với",
            "tui" to "tôi", "mik" to "mình", "bro" to "bạn", "mn" to "mọi người",
            // hỏi
            "tn" to "thế nào", "ntn" to "như thế nào", "sao" to "thế nào",
            // ứng dụng / tài khoản
            "app" to "ứng dụng", "acc" to "tài khoản", "account" to "tài khoản",
            "pass" to "mật khẩu", "password" to "mật khẩu",
            "login" to "đăng nhập", "register" to "đăng ký", "signup" to "đăng ký",
            "đk" to "đăng ký", "đn" to "đăng nhập",
            "hd" to "hướng dẫn",
            // lỗi / hỗ trợ
            "bug" to "lỗi", "error" to "lỗi",
            // hành động
            "cancel" to "hủy", "book" to "đặt lịch", "chat" to "nhắn tin",
            "update" to "cập nhật", "check" to "kiểm tra",
            // khác
            "dv" to "dịch vụ", "tt" to "thông tin", "sdt" to "số điện thoại",
            "đc" to "được", "nv" to "nhân viên"
        )

        private val DISTRICT_ALIASES = listOf(
            // Phường (phuongList)
            "hai bà trưng" to "Hai Bà Trưng", "hai ba trung" to "Hai Bà Trưng", "hbt" to "Hai Bà Trưng",
            "hoàn kiếm" to "Hoàn Kiếm", "hoan kiem" to "Hoàn Kiếm",
            "thanh xuân" to "Thanh Xuân", "thanh xuan" to "Thanh Xuân", "tx" to "Thanh Xuân",
            "hoàng mai" to "Hoàng Mai", "hoang mai" to "Hoàng Mai", "hm" to "Hoàng Mai",
            "long biên" to "Long Biên", "long bien" to "Long Biên", "lb" to "Long Biên",
            "đống đa" to "Đống Đa", "dong da" to "Đống Đa", "dd" to "Đống Đa",
            "cầu giấy" to "Cầu Giấy", "cau giay" to "Cầu Giấy", "cg" to "Cầu Giấy",
            "ba đình" to "Ba Đình", "ba dinh" to "Ba Đình", "bd" to "Ba Đình",
            "tây hồ" to "Tây Hồ", "tay ho" to "Tây Hồ", "th" to "Tây Hồ",
            "hà đông" to "Hà Đông", "ha dong" to "Hà Đông",
            "từ liêm" to "Từ Liêm", "tu liem" to "Từ Liêm",
            "nam từ liêm" to "Từ Liêm", "nam tu liem" to "Từ Liêm", "ntl" to "Từ Liêm",
            "bắc từ liêm" to "Từ Liêm", "bac tu liem" to "Từ Liêm", "btl" to "Từ Liêm",
            "chương mỹ" to "Chương Mỹ", "chuong my" to "Chương Mỹ",
            // Xã (xaList)
            "gia lâm" to "Gia Lâm", "gia lam" to "Gia Lâm", "gl" to "Gia Lâm",
            "sóc sơn" to "Sóc Sơn", "soc son" to "Sóc Sơn", "ss" to "Sóc Sơn",
            "mê linh" to "Mê Linh", "me linh" to "Mê Linh", "ml" to "Mê Linh",
            "đan phượng" to "Đan Phượng", "dan phuong" to "Đan Phượng",
            "thạch thất" to "Thạch Thất", "thach that" to "Thạch Thất",
            "quốc oai" to "Quốc Oai", "quoc oai" to "Quốc Oai",
            "thanh oai" to "Thanh Oai",
            "thường tín" to "Thường Tín", "thuong tin" to "Thường Tín",
            "phú xuyên" to "Phú Xuyên", "phu xuyen" to "Phú Xuyên",
            "mỹ đức" to "Mỹ Đức", "my duc" to "Mỹ Đức",
            "ba vì" to "Ba Vì", "ba vi" to "Ba Vì",
            "phúc thọ" to "Phúc Thọ", "phuc tho" to "Phúc Thọ"
        )

        private val SKIP_KEYWORDS = setOf(
            "không biết", "không có", "tùy", "tuỳ", "bất kỳ", "bất kì",
            "không quan tâm", "không cần", "cũng được", "nào cũng", "thôi", "skip", "bỏ qua"
        )

        // --- Geo-search detection ---
        // Từ khoá gợi ý tìm kiếm gần một địa điểm cụ thể
        private val GEO_TRIGGER_KEYWORDS = setOf(
            "quanh", "gần", "xung quanh", "lân cận", "trong vòng", "bán kính",
            "cạnh", "bên cạnh", "kế bên", "ngang", "gần đây", "khu vực gần"
        )

        // Pattern nhận dạng địa chỉ cụ thể (số nhà, ngõ, đường...)
        private val STREET_PATTERNS = listOf(
            Regex("\\bsố\\s*\\d+"),
            Regex("\\bngõ\\s*\\d+"),
            Regex("\\bngách\\s*\\d+"),
            Regex("\\bno\\s*\\d+"),
            Regex("\\bđường\\s+[a-zA-ZÀ-ỹ]"),
            Regex("\\bphố\\s+[a-zA-ZÀ-ỹ]"),
            Regex("\\bduong\\s+[a-zA-ZÀ-ỹ]"),
            Regex("\\bpho\\s+[a-zA-ZÀ-ỹ]"),
            Regex("\\b[a-zA-ZÀ-ỹ]\\d+[,\\s]"),  // e2, c5...
            Regex("\\blinh dam\\b"), Regex("\\bmy dinh\\b"),
            Regex("\\bđịa chỉ\\b"), Regex("\\bdia chi\\b")
        )

        // Nhận dạng bán kính từ input
        private val RADIUS_ALIASES = listOf(
            Regex("(\\d+(?:[.,]\\d+)?)\\s*km") to 1.0,
            Regex("(\\d+(?:[.,]\\d+)?)\\s*kilômét") to 1.0,
            Regex("(\\d+(?:[.,]\\d+)?)\\s*kilomet") to 1.0,
            Regex("(\\d+(?:[.,]\\d+)?)\\s*m\\b") to 0.001,  // metres
            Regex("(\\d+(?:[.,]\\d+)?)\\s*mét") to 0.001,
            Regex("(\\d+(?:[.,]\\d+)?)\\s*met\\b") to 0.001
        )

        private val RADIUS_QUICK_REPLIES = listOf("500m", "1 km", "2 km", "3 km", "5 km")

        private val SEARCH_INTENT_KEYWORDS = setOf(
            // có dấu
            "tìm phòng", "tìm trọ", "thuê phòng", "phòng trọ", "phòng cho thuê",
            "muốn thuê", "cần thuê", "đang tìm phòng", "tìm chỗ ở", "thuê trọ",
            "tìm kiếm phòng", "cho thuê phòng", "phòng giá",
            "cần phòng", "muốn tìm phòng", "muốn thuê trọ", "tìm nhà trọ",
            "tìm trọ giá", "phòng trọ giá", "chỗ ở giá rẻ", "thuê chỗ",
            "phòng rẻ", "phòng đẹp", "phòng ở", "muốn ở", "cần ở",
            "phòng trống", "cần chỗ ở", "muốn tìm chỗ", "tìm chỗ thuê",
            "phòng gần", "phòng sinh viên", "phòng giá rẻ", "phòng còn không",
            // câu tự nhiên / hội thoại thường gặp
            "kiếm phòng", "kiếm chỗ", "kiếm trọ", "tìm chỗ trọ",
            "cần chỗ trọ", "đang cần chỗ", "đang kiếm phòng",
            "ngân sách", "budget", "có tiền", "tiền thuê",
            "sinh viên mới", "mới lên hà nội", "mới ra hà nội",
            "gần trường", "gần chỗ làm", "gần công ty",
            "ở ghép", "tìm người ghép", "ghép phòng",
            "chuyển nhà", "tìm chỗ mới", "cần chỗ mới",
            "hỏi thuê", "hỏi phòng", "cho hỏi phòng",
            "còn phòng", "phòng trống không", "có phòng không",
            "xem phòng", "tham quan phòng",
            // không dấu / typo thường gặp
            "tim phong", "tim tro", "phong tro", "thue phong",
            "phong re", "can phong", "muon thue", "can thue",
            "phong sinh vien", "tim cho o", "thue cho",
            "kiem phong", "kiem cho", "can cho tro", "cho thue khong"
        )

        // Từ gợi ý search khi BM25 fallback — nhận biết user có thể đang tìm phòng
        private val ROOM_HINT_WORDS = setOf(
            "phòng", "trọ", "thuê", "ở trọ", "nhà trọ", "chỗ ở",
            "phong", "tro", "thue", "nha tro", "cho o",
            // thêm: từ ngữ sinh viên/người đi làm hay dùng
            "kiếm", "kiem", "chỗ", "cho", "ngân sách", "budget",
            "sinh viên", "sinh vien", "đi làm", "di lam", "gần trường", "gan truong"
        )

        // Words to strip when extracting a ward/sub-area before the district name.
        // Includes spatial/proximity terms so "quanh khu vực", "xung quanh", etc.
        // are never mistaken for a ward name.
        private val WARD_NAV_WORDS = setOf(
            // navigation / structural
            "tìm", "phòng", "trọ", "thuê", "khu", "vực", "đường", "ngõ", "ngách",
            "phường", "quận", "huyện", "xã", "thị", "trấn", "số",
            // proximity / spatial
            "gần", "quanh", "xung", "lân", "cận", "ven", "ngoại", "vùng",
            "khoảng", "xấp", "xỉ", "tầm", "đây",
            // centre / area qualifiers
            "trung", "tâm", "nội", "ngoài", "rìa"
        )

        // Fix 7: amenity keyword → Firestore field value mapping
        private val AMENITY_KEYWORDS = mapOf(
            // Làm mát
            "điều hòa" to "airConditioner", "máy lạnh" to "airConditioner", "điều hoà" to "airConditioner",
            // Mạng
            "wifi" to "wifi", "internet" to "wifi", "mạng" to "wifi",
            // Nội thất (full)
            "nội thất" to "furniture", "full nội thất" to "furniture", "có đồ" to "furniture",
            // Máy móc
            "máy giặt" to "washer",
            "bình nóng lạnh" to "waterHeater", "nóng lạnh" to "waterHeater",
            // Bếp
            "bếp riêng" to "kitchen", "bếp" to "kitchen", "nấu ăn riêng" to "kitchen",
            // Giường/tủ
            "giường" to "bed", "giường ngủ" to "bed",
            "tủ quần áo" to "wardrobe", "tủ đồ" to "wardrobe",
            // Chỗ để xe
            "chỗ để xe" to "parking", "gửi xe" to "parking", "bãi xe" to "parking", "để xe" to "parking",
            // Ban công / sân phơi
            "ban công" to "balcony", "sân phơi" to "balcony",
            // WC
            "wc riêng" to "privateWC", "nhà vệ sinh riêng" to "privateWC",
            "toilet riêng" to "privateWC", "phòng tắm riêng" to "privateWC",
            "vệ sinh riêng" to "privateWC", "vs riêng" to "privateWC",
            // An ninh
            "bảo vệ" to "security", "camera" to "security", "an ninh" to "security",
            // Điện tử
            "tủ lạnh" to "refrigerator",
            "tivi" to "tv", "ti vi" to "tv"
        )
    }

    // AddressData không còn dùng format "TênPhường (TênQuận)" sau khi cải cách hành chính 2025.
    // Mỗi entry trong phuongList/xaList là phường/xã cấp 1 — không có cấp phân cấp con.
    private val districtWards: Map<String, List<String>> = emptyMap()

    // Return up to 3 ward names for quick-reply chips; empty if district unknown
    private fun getWardQuickReplies(district: String?): List<String> {
        if (district == null) return emptyList()
        return districtWards[district]?.take(3) ?: emptyList()
    }

    fun initialize() {
        loadFromJson()
        computeIdf()
        computePatternVectors()
    }

    // --- Tokenizer ---

    private fun normalize(words: List<String>): List<String> =
        words.map { word -> NORMALIZATION_DICT[word] ?: word }

    private fun baseTokenize(text: String): List<String> {
        val truncated = Normalizer.normalize(text, Normalizer.Form.NFC).take(MAX_INPUT_LENGTH)
        val cleaned = truncated.lowercase().trim().replace(
            Regex("[^a-záàảãạăắằẳẵặâấầẩẫậéèẻẽẹêếềểễệíìỉĩịóòỏõọôốồổỗộơớờởỡợúùủũụưứừửữựýỳỷỹỵđ0-9 ]"),
            " "
        )
        val words = normalize(
            cleaned.split(Regex("\\s+"))
                .filter { it.length > 1 }
                .filter { it !in VIETNAMESE_STOPWORDS }
        )
        val bigrams = (0 until words.size - 1).map { i -> "${words[i]} ${words[i + 1]}" }
        return words + bigrams
    }

    private fun tokenize(text: String): List<String> {
        val base = baseTokenize(text)
        return expandSynonyms(base, text.lowercase())
    }

    private fun expandSynonyms(tokens: List<String>, lowerText: String): List<String> {
        val expanded = tokens.toMutableList()
        for ((canonical, synonyms) in SYNONYM_GROUPS) {
            val foundSynonym = synonyms.any { lowerText.contains(it) }
            val foundCanonical = lowerText.contains(canonical)
            if (foundSynonym) expanded.addAll(baseTokenize(canonical))
            if (foundCanonical) synonyms.forEach { expanded.addAll(baseTokenize(it)) }
        }
        return expanded
    }

    // --- BM25 core ---

    private fun computeTermFrequencies(tokens: List<String>): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        for (token in tokens) counts[token] = (counts[token] ?: 0) + 1
        return counts
    }

    private fun computeIdf() {
        val allPatterns = intents.flatMap { it.patterns }
        val n = allPatterns.size.toDouble()
        val docFreq = mutableMapOf<String, Int>()
        for (pattern in allPatterns) {
            val uniqueTokens = baseTokenize(pattern).toSet()
            for (token in uniqueTokens) docFreq[token] = (docFreq[token] ?: 0) + 1
        }
        for ((token, df) in docFreq) {
            idfScores[token] = ln((n + 1.0) / (df + 1.0)) + 1.0
        }
    }

    private fun computeBM25(text: String): Map<String, Double> {
        val tokens = tokenize(text)
        if (tokens.isEmpty()) return emptyMap()
        val dl = tokens.size.toDouble()
        val termFreqs = computeTermFrequencies(tokens)
        val totalPatterns = intents.flatMap { it.patterns }.size.toDouble()
        val result = mutableMapOf<String, Double>()
        for ((token, freq) in termFreqs) {
            val idf = idfScores[token] ?: (ln((totalPatterns + 1.0) / 1.0) + 1.0)
            val bm25Tf = (freq * (BM25_K1 + 1.0)) /
                (freq + BM25_K1 * (1.0 - BM25_B + BM25_B * dl / avgPatternLength))
            result[token] = idf * bm25Tf
        }
        return result
    }

    private fun cosineSimilarity(vecA: Map<String, Double>, vecB: Map<String, Double>): Double {
        if (vecA.isEmpty() || vecB.isEmpty()) return 0.0
        val commonKeys = vecA.keys.intersect(vecB.keys)
        if (commonKeys.isEmpty()) return 0.0
        val dotProduct = commonKeys.sumOf { key -> vecA[key]!! * vecB[key]!! }
        val magnitudeA = sqrt(vecA.values.sumOf { it * it })
        val magnitudeB = sqrt(vecB.values.sumOf { it * it })
        return if (magnitudeA == 0.0 || magnitudeB == 0.0) 0.0
        else dotProduct / (magnitudeA * magnitudeB)
    }

    private fun computePatternVectors() {
        for (intent in intents) {
            for (pattern in intent.patterns) {
                patternVectors.add(Pair(intent.id, computeBM25(pattern)))
            }
        }
    }

    private fun dynamicThreshold(wordCount: Int): Double = when {
        wordCount <= 2 -> 0.30
        wordCount <= 4 -> 0.22
        else -> BASE_THRESHOLD
    }

    // --- Entity extraction ---

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1]
                            else 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
            }
        }
        return dp[a.length][b.length]
    }

    fun extractDistrict(input: String): String? {
        val lower = input.lowercase().trim()
        if (SKIP_KEYWORDS.any { lower.contains(it) }) return ""
        for ((alias, canonical) in DISTRICT_ALIASES) {
            if (lower.contains(alias)) return canonical
        }
        val words = lower.split(Regex("\\s+"))
        for (word in words) {
            if (word.length < 3) continue
            for ((alias, canonical) in DISTRICT_ALIASES) {
                if (alias.length < 3) continue
                val maxDist = when {
                    alias.length <= 5 -> 1
                    alias.length <= 9 -> 2
                    else -> 3
                }
                if (levenshtein(word, alias) <= maxDist) return canonical
            }
        }
        return null
    }

    fun extractPrice(input: String): Long? {
        val lower = input.lowercase().trim()
        if (SKIP_KEYWORDS.any { lower.contains(it) }) return -1L

        // "2tr5" → 2,500,000 | "3tr5" → 3,500,000
        val compoundMatch = Regex("(\\d+)tr(\\d+)").find(lower)
        if (compoundMatch != null) {
            val wholePart = compoundMatch.groupValues[1].toLongOrNull() ?: 0L
            val fracPart = compoundMatch.groupValues[2].toLongOrNull() ?: 0L
            return (wholePart * 1_000_000L) + (fracPart * 100_000L)
        }

        // "3 triệu rưỡi" → 3,500,000
        val ruoiMatch = Regex("(\\d+)\\s*triệu\\s*rưỡi").find(lower)
        if (ruoiMatch != null) {
            val wholePart = ruoiMatch.groupValues[1].toLongOrNull() ?: 0L
            return (wholePart * 1_000_000L) + 500_000L
        }

        val millionMatch = Regex("(\\d+(?:[.,]\\d+)?)\\s*(?:triệu|tr\\b)").find(lower)
        if (millionMatch != null) {
            val num = millionMatch.groupValues[1].replace(",", ".").toDoubleOrNull()
            if (num != null) return (num * 1_000_000.0).toLong()
        }

        val thousandMatch = Regex("(\\d+(?:[.,]\\d+)?)\\s*(?:k\\b|nghìn|ngàn|nghin)").find(lower)
        if (thousandMatch != null) {
            val num = thousandMatch.groupValues[1].replace(",", ".").toDoubleOrNull()
            if (num != null) return (num * 1_000.0).toLong()
        }

        val rawMatch = Regex("\\b(\\d{6,})\\b").find(lower)
        if (rawMatch != null) return rawMatch.groupValues[1].toLongOrNull()

        return null
    }

    // Fix 4: Extract price range "từ X đến Y triệu" or single max price
    // Returns Pair(minPrice, maxPrice). null maxPrice = parse failed. -1L = skip/no limit.
    fun extractPriceRange(input: String): Pair<Long?, Long?> {
        val lower = input.lowercase().trim()
        if (SKIP_KEYWORDS.any { lower.contains(it) }) return Pair(null, -1L)

        val rangePattern = Regex(
            "(\\d+(?:[.,]\\d+)?)\\s*(?:triệu|tr)?\\s*(?:đến|-)\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:triệu|tr\\b)"
        )
        val rangeMatch = rangePattern.find(lower)
        if (rangeMatch != null) {
            val minVal = rangeMatch.groupValues[1].replace(",", ".").toDoubleOrNull()
            val maxVal = rangeMatch.groupValues[2].replace(",", ".").toDoubleOrNull()
            if (minVal != null && maxVal != null) {
                return Pair((minVal * 1_000_000).toLong(), (maxVal * 1_000_000).toLong())
            }
        }

        // "trên X triệu" / "hơn X triệu" / "ít nhất X triệu" → minPrice = X, không giới hạn max
        val aboveMatch = Regex(
            "(?:trên|hơn|ít nhất|tối thiểu|từ)\\s+(\\d+(?:[.,]\\d+)?)\\s*(?:triệu|tr\\b)"
        ).find(lower)
        if (aboveMatch != null) {
            val num = aboveMatch.groupValues[1].replace(",", ".").toDoubleOrNull()
            if (num != null) return Pair((num * 1_000_000).toLong(), -1L)
        }

        // "dưới X" / "không quá X" / "tối đa X" → maxPrice = X
        val belowMatch = Regex(
            "(?:dưới|không quá|tối đa|duoi|khong qua)\\s+(\\d+(?:[.,]\\d+)?)\\s*(?:triệu|tr\\b)"
        ).find(lower)
        if (belowMatch != null) {
            val num = belowMatch.groupValues[1].replace(",", ".").toDoubleOrNull()
            if (num != null) return Pair(null, (num * 1_000_000).toLong())
        }

        // Single value → treat as max price
        val single = extractPrice(lower)
        return Pair(null, single)
    }

    // Fix 7: Extract amenity requirements from user input
    fun extractAmenities(input: String): List<String> {
        val lower = input.lowercase()
        return AMENITY_KEYWORDS.entries
            .filter { lower.contains(it.key) }
            .map { it.value }
            .distinct()
    }

    /** Trích xuất bán kính (km) từ input người dùng.
     *  Ví dụ: "500m" → 0.5, "1 km" → 1.0, "2km" → 2.0.
     *  Trả về null nếu không nhận dạng được. -1.0 nếu skip. */
    fun extractRadius(input: String): Double? {
        val lower = input.lowercase().trim()
        if (SKIP_KEYWORDS.any { lower.contains(it) }) return -1.0

        for ((regex, multiplier) in RADIUS_ALIASES) {
            val m = regex.find(lower) ?: continue
            val num = m.groupValues[1].replace(",", ".").toDoubleOrNull() ?: continue
            val km = num * multiplier
            // Giới hạn hợp lý: 0.1 km đến 20 km
            if (km in 0.1..20.0) return km
        }
        return null
    }

    fun extractAreaRange(input: String): Pair<Int?, Int?> {
        val lower = input.lowercase().trim()
        val rangePattern = Regex(
            "(\\d+)\\s*(?:m2|mét vuông|met vuong)?\\s*(?:đến|-|tới)\\s*(\\d+)\\s*(?:m2|mét vuông|met vuong\\b)"
        )
        val rangeMatch = rangePattern.find(lower)
        if (rangeMatch != null) {
            val minVal = rangeMatch.groupValues[1].toIntOrNull()
            val maxVal = rangeMatch.groupValues[2].toIntOrNull()
            if (minVal != null && maxVal != null) {
                return Pair(minVal, maxVal)
            }
        }
        
        val aboveMatch = Regex(
            "(?:trên|hơn|rộng hơn|rộng trên|ít nhất|tối thiểu|từ)\\s+(\\d+)\\s*(?:m2|mét vuông|met vuong\\b)"
        ).find(lower)
        if (aboveMatch != null) {
            val num = aboveMatch.groupValues[1].toIntOrNull()
            if (num != null) return Pair(num, null)
        }
        
        val belowMatch = Regex(
            "(?:dưới|nhỏ hơn|tối đa|duoi|rộng dưới)\\s+(\\d+)\\s*(?:m2|mét vuông|met vuong\\b)"
        ).find(lower)
        if (belowMatch != null) {
            val num = belowMatch.groupValues[1].toIntOrNull()
            if (num != null) return Pair(null, num)
        }

        val singleMatch = Regex(
            "\\b(\\d+)\\s*(?:m2|mét vuông|met vuong)\\b"
        ).find(lower)
        if (singleMatch != null) {
            val num = singleMatch.groupValues[1].toIntOrNull()
            if (num != null) {
                return Pair(num - 5, num + 5)
            }
        }
        return Pair(null, null)
    }

    fun extractRoomType(input: String): String? {
        val lower = input.lowercase()
        return when {
            lower.contains("chung cư mini") || lower.contains("ccmn") -> "Chung cư mini"
            lower.contains("căn hộ") || lower.contains("chung cư") -> "Căn hộ"
            lower.contains("ký túc xá") || lower.contains("ktx") || lower.contains("homestay") -> "Kí túc xá / Homestay"
            lower.contains("ở ghép") || lower.contains("o ghep") -> "Ở ghép"
            lower.contains("phòng trọ") || lower.contains("phong tro") || lower.contains("nhà trọ") -> "Phòng trọ"
            else -> null
        }
    }

    fun extractGenderPrefer(input: String): String? {
        val lower = input.lowercase()
        return when {
            lower.contains("cho nữ") || lower.contains("phòng nữ") || lower.contains("nữ ở") || lower.contains("nữ thuê") -> "Nữ"
            lower.contains("cho nam") || lower.contains("phòng nam") || lower.contains("nam ở") || lower.contains("nam thuê") -> "Nam"
            else -> null
        }
    }

    fun extractElectricPrice(input: String): Long? {
        val lower = input.lowercase()
        // Hỗ trợ cả "giá điện 3k" và "giá điện dưới/không quá/tối đa 50k"
        val kMatch = Regex(
            "(?:giá điện|tiền điện|điện)\\s*(?:dưới|không quá|tối đa|duoi|khong qua)?\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:k\\b|nghìn|ngàn|đ\\b)"
        ).find(lower)
        if (kMatch != null) {
            val num = kMatch.groupValues[1].replace(",", ".").toDoubleOrNull()
            if (num != null) {
                return if (num < 100.0) (num * 1000.0).toLong() else num.toLong()
            }
        }
        val rawMatch = Regex(
            "(?:giá điện|tiền điện|điện)\\s*(?:dưới|không quá|tối đa)?\\s*(\\d{4,6})"
        ).find(lower)
        if (rawMatch != null) {
            return rawMatch.groupValues[1].toLongOrNull()
        }
        return null
    }

    fun extractWaterPrice(input: String): Long? {
        val lower = input.lowercase()
        val kMatch = Regex("(?:giá nước|tiền nước|nước)\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:k|nghìn|ngàn|đ\\b)").find(lower)
        if (kMatch != null) {
            val num = kMatch.groupValues[1].replace(",", ".").toDoubleOrNull()
            if (num != null) {
                return if (num < 1000.0) (num * 1000.0).toLong() else num.toLong()
            }
        }
        val rawMatch = Regex("(?:giá nước|tiền nước|nước)\\s*(\\d{4,6})").find(lower)
        if (rawMatch != null) {
            return rawMatch.groupValues[1].toLongOrNull()
        }
        return null
    }

    fun extractWaterPriceOnly(input: String): Long? {
        return extractWaterPrice(input)
    }

    fun extractWifiPrice(input: String): Long? {
        val lower = input.lowercase()
        val kMatch = Regex("(?:wifi|mạng|internet)\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:k|nghìn|ngàn|đ\\b)").find(lower)
        if (kMatch != null) {
            val num = kMatch.groupValues[1].replace(",", ".").toDoubleOrNull()
            if (num != null) {
                return if (num < 1000.0) (num * 1000.0).toLong() else num.toLong()
            }
        }
        val rawMatch = Regex("(?:wifi|mạng|internet)\\s*(\\d{4,6})").find(lower)
        if (rawMatch != null) {
            return rawMatch.groupValues[1].toLongOrNull()
        }
        return null
    }

    fun extractDepositMonths(input: String): Int? {
        val lower = input.lowercase()
        if (lower.contains("không cọc") || lower.contains("không cần cọc") || lower.contains("khong coc")) return 0
        val match = Regex("cọc\\s*(\\d+)\\s*tháng").find(lower)
        if (match != null) {
            return match.groupValues[1].toIntOrNull()
        }
        return null
    }

    fun extractPetAllowed(input: String): Boolean? {
        val lower = input.lowercase()
        if (lower.contains("không nuôi pet") || lower.contains("không cho nuôi chó") || lower.contains("cấm nuôi")) return false
        if (lower.contains("nuôi pet") || lower.contains("nuôi chó") || lower.contains("nuôi mèo") || lower.contains("nuôi thú cưng") || lower.contains("cho nuôi pet") || lower.contains("cho nuôi chó") || lower.contains("cho nuôi mèo")) return true
        return null
    }

    fun extractFreeTime(input: String): Boolean? {
        val lower = input.lowercase()
        if (lower.contains("tự do giờ giấc") || lower.contains("giờ giấc tự do") || lower.contains("không chung chủ") || lower.contains("khóa vân tay") || lower.contains("giờ tự do") || lower.contains("tự quản")) return true
        return null
    }

    fun extractPeopleCount(input: String): Int? {
        val lower = input.lowercase()
        // "phòng cho 1 người", "2 người ở", "ở ghép 3 người"
        val match = Regex(
            "(?:cho|ở|ghép|tối đa)?\\s*(\\d+)\\s*(?:người|ng\\b)"
        ).find(lower) ?: return null
        return match.groupValues[1].toIntOrNull()
    }


    /** Nhận dạng câu có ý định tìm phòng theo địa chỉ/vị trí cụ thể.
     *  Điều kiện: có từ khoá geo-trigger VÀ có pattern địa chỉ hoặc từ khoá tìm phòng.
     *  Hoặc người dùng nhập "địa chỉ" / "số nhà" / "ngõ..." kết hợp tìm phòng. */
    fun isGeoAddressIntent(input: String): Boolean {
        val lower = input.lowercase()
        val hasSearchHint = SEARCH_INTENT_KEYWORDS.any { lower.contains(it) }
            || ROOM_HINT_WORDS.any { lower.contains(it) }
        val hasGeoTrigger = GEO_TRIGGER_KEYWORDS.any { lower.contains(it) }
        val hasStreetPattern = STREET_PATTERNS.any { it.containsMatchIn(lower) }
        return (hasGeoTrigger && (hasSearchHint || hasStreetPattern)) ||
               (hasStreetPattern && hasSearchHint)
    }

    /** Trích xuất phần địa chỉ/địa điểm từ câu người dùng nhập.
     *  Loại bỏ tất cả từ khoá tìm phòng, geo-trigger, "khu vực"...
     *  để lấy phần địa điểm thuần dùng cho geocoding.
     *
     *  Ví dụ:
     *  "tìm phòng quanh khu vực học viện an ninh nhân dân" → "học viện an ninh nhân dân"
     *  "tìm phòng gần đường Láng" → "đường láng"
     *  "phòng quanh địa chỉ số 5 ngõ 6 cầu giấy" → "số 5 ngõ 6 cầu giấy"
     */
    fun extractAddressQuery(input: String): String {
        var cleaned = input.lowercase().trim()

        // Danh sách từ cần xóa — sắp xếp từ DÀI → NGẮN để tránh xóa một phần từ dài
        val removeWords = listOf(
            // Cụm từ tìm phòng (dài nhất trước)
            "tìm tôi phòng trọ", "tìm phòng trọ", "thuê phòng trọ",
            "kiếm phòng trọ", "cần phòng trọ", "muốn thuê phòng trọ",
            "tìm phòng", "tìm trọ", "thuê phòng", "muốn thuê",
            "cần thuê", "kiếm phòng", "xem phòng",
            // Geo trigger dạng cụm (dài trước)
            "quanh địa chỉ", "xung quanh địa chỉ", "gần địa chỉ",
            "khu vực gần", "gần khu vực",
            "xung quanh", "lân cận", "gần đây", "trong vòng",
            "bán kính", "bên cạnh", "kế bên",
            // Từ đơn địa lý thừa
            "khu vực", "khu", "vực",
            "quanh", "cạnh", "gần",
            "địa chỉ", "dia chi",
            // Loại bỏ dấu ngoặc kép nếu user bao địa chỉ
            "\"", "\""
        )

        for (w in removeWords) {
            cleaned = cleaned.replace(w, " ")
        }

        // Dọn khoảng trắng thừa và ký tự đặc biệt đầu/cuối
        cleaned = cleaned
            .replace(Regex("\\s+"), " ")
            .trimStart(',', '"', '\'', ' ', '-', '.')
            .trimEnd(',', '"', '\'', ' ', '-', '.')

        // Trả về phần còn lại nếu đủ dài, ngược lại giữ nguyên input
        return if (cleaned.length >= 4) cleaned else input.trim()
    }

    // Remove Vietnamese diacritics for fuzzy ward/street matching.
    // "Mộ Lao" → "Mo Lao", "Văn Quán" → "Van Quan"
    private fun stripDiacritics(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("[\\u0300-\\u036f\\u1dc0-\\u1dff]"), "")
            .replace('đ', 'd').replace('Đ', 'D')

    // Extract the ward/sub-area from user input.
    // Strategy 1: match known wards of the district — both with & without diacritics.
    // Strategy 2: fallback positional extraction (words before district alias).
    fun extractWard(input: String, district: String?): String? {
        if (district == null || district.isEmpty()) return null
        val lower = input.lowercase().trim()
        val lowerStripped = stripDiacritics(lower)

        // Strategy 1: check against known wards of this district
        val knownWards = districtWards[district]
        if (knownWards != null) {
            val matched = knownWards.firstOrNull { ward ->
                lower.contains(ward.lowercase()) ||
                lowerStripped.contains(stripDiacritics(ward).lowercase())
            }
            if (matched != null) return matched
        }

        // Strategy 2: positional — words before the district alias in the string
        for ((alias, canonical) in DISTRICT_ALIASES) {
            if (canonical != district) continue
            val idx = lower.indexOf(alias)
            if (idx <= 0) continue

            val candidateWords = lower.substring(0, idx).trim()
                .split(Regex("\\s+"))
                .filter { word ->
                    word.length >= 2
                        && word !in VIETNAMESE_STOPWORDS
                        && word !in WARD_NAV_WORDS
                }
            if (candidateWords.isEmpty()) continue

            return candidateWords.joinToString(" ") { w ->
                w.replaceFirstChar { c -> c.uppercaseChar() }
            }
        }
        return null
    }

    // Fix 6: "Xem thêm" handling — sentinel if already showed all results
    private fun resolveReferences(input: String, ctx: ConversationContext): String {
        val lower = input.lowercase().trim()

        if (CHEAPER_KEYWORDS.any { lower.contains(it) } && ctx.lastSearchMaxPrice != null) {
            val newPrice = (ctx.lastSearchMaxPrice * 0.75).toLong()
            val districtPart = if (ctx.lastSearchDistrict != null) " ${ctx.lastSearchDistrict}" else ""
            return "tìm phòng$districtPart giá $newPrice"
        }

        if (EXPENSIVE_KEYWORDS.any { lower.contains(it) } && ctx.lastSearchMaxPrice != null) {
            val newPrice = (ctx.lastSearchMaxPrice * 1.3).toLong()
            val districtPart = if (ctx.lastSearchDistrict != null) " ${ctx.lastSearchDistrict}" else ""
            return "tìm phòng$districtPart giá $newPrice"
        }

        if (SAME_AREA_KEYWORDS.any { lower.contains(it) } && ctx.lastSearchDistrict != null) {
            val pricePart = if (ctx.lastSearchMaxPrice != null) " giá ${ctx.lastSearchMaxPrice}" else ""
            return "tìm phòng ${ctx.lastSearchDistrict}$pricePart"
        }

        if (MORE_RESULTS_KEYWORDS.any { lower.contains(it) }) {
            if (ctx.lastSearchDistrict != null || ctx.lastSearchMaxPrice != null) {
                val districtPart = if (ctx.lastSearchDistrict != null) " ${ctx.lastSearchDistrict}" else ""
                val pricePart = if (ctx.lastSearchMaxPrice != null) " giá ${ctx.lastSearchMaxPrice}" else ""
                return "tìm phòng$districtPart$pricePart"
            }
        }

        return input
    }

    private fun isSearchIntent(input: String): Boolean {
        val lower = input.lowercase()
        if (SEARCH_INTENT_KEYWORDS.any { lower.contains(it) }) return true
        // Direct location shortcut: user typed district/ward (+price) without search keywords.
        // Exclude if the sentence contains question indicators so we don't intercept
        // informational questions like "Hà Đông có tiện nghi gì?".
        if (extractDistrict(input) != null) {
            val hasQuestion = lower.contains('?') ||
                Regex("\\b(có|không|thế nào|như thế|bao nhiêu|tại sao|vì sao|sao vậy|được không)\\b")
                    .containsMatchIn(lower)
            if (!hasQuestion) return true
        }
        return false
    }

    fun randomUiResponse(key: String, vars: Map<String, String> = emptyMap()): String {
        val templates = uiResponses[key]
        if (templates.isNullOrEmpty()) return "..."
        var text = templates.random()
        vars.forEach { (k, v) -> text = text.replace("{$k}", v) }
        return text
    }

    private fun randomSearchingMsg(districtLabel: String, priceLabel: String? = null): String {
        val pricePart = if (priceLabel != null) ", $priceLabel" else ""
        return randomUiResponse("searching", mapOf("district" to districtLabel, "price" to pricePart))
    }

    private fun randomAskDistrictMsg() = randomUiResponse("ask_district")

    private fun randomAskPriceMsg(districtHint: String = "") =
        randomUiResponse("ask_price", mapOf("district_hint" to districtHint))

    private fun randomDistrictNotFoundMsg() = randomUiResponse("district_not_found")

    private fun randomPriceNotFoundMsg() = randomUiResponse("price_not_found")

    private fun randomFallbackMsg() = randomUiResponse("fallback")

    // Build a response that either asks the user to pick a ward (if district has known wards
    // and no ward was already detected) or jumps straight to asking for price.
    private fun askWardOrPrice(
        canonical: String?,
        detectedWard: String?,
        ctx: ConversationContext,
        userInput: String
    ): ChatbotResponse {
        val hasKnownWards = canonical != null && districtWards.containsKey(canonical)

        val currentRoomType = extractRoomType(userInput)
        val currentGender = extractGenderPrefer(userInput)
        val (currentMinArea, currentMaxArea) = extractAreaRange(userInput)
        val currentElec = extractElectricPrice(userInput)
        val currentWater = extractWaterPrice(userInput)
        val currentWifi = extractWifiPrice(userInput)
        val currentDep = extractDepositMonths(userInput)
        val currentPet = extractPetAllowed(userInput)
        val currentFree = extractFreeTime(userInput)
        val currentAmenities = extractAmenities(userInput)

        return if (detectedWard == null && hasKnownWards && canonical != null) {
            val quickReplies = getWardQuickReplies(canonical) + listOf("Cả quận")
            ChatbotResponse(
                answer = randomUiResponse("ask_ward", mapOf("district" to canonical)),
                confidence = 1.0,
                quickReplies = quickReplies,
                nextContext = ctx.copy(
                    state = ConversationState.AWAIT_WARD,
                    district = canonical,
                    amenities = currentAmenities,
                    minArea = currentMinArea,
                    maxArea = currentMaxArea,
                    roomType = currentRoomType,
                    genderPrefer = currentGender,
                    maxElectricPrice = currentElec,
                    maxWaterPrice = currentWater,
                    maxWifiPrice = currentWifi,
                    maxDepositMonths = currentDep,
                    petAllowed = currentPet,
                    freeTime = currentFree
                )
            )
        } else {
            val districtDisplay = when {
                detectedWard != null && canonical != null -> "$detectedWard, $canonical"
                else -> canonical ?: "tất cả khu vực"
            }
            ChatbotResponse(
                answer = randomUiResponse("ask_price_with_district", mapOf("district" to districtDisplay)),
                confidence = 1.0,
                quickReplies = listOf("2 triệu", "3 triệu", "5 triệu", "Tùy giá"),
                nextContext = ctx.copy(
                    state = ConversationState.AWAIT_PRICE,
                    district = canonical,
                    ward = detectedWard,
                    amenities = currentAmenities,
                    minArea = currentMinArea,
                    maxArea = currentMaxArea,
                    roomType = currentRoomType,
                    genderPrefer = currentGender,
                    maxElectricPrice = currentElec,
                    maxWaterPrice = currentWater,
                    maxWifiPrice = currentWifi,
                    maxDepositMonths = currentDep,
                    petAllowed = currentPet,
                    freeTime = currentFree
                )
            )
        }
    }

    private fun formatPriceShort(price: Long): String {
        val millions = price / 1_000_000.0
        return if (millions >= 1.0) {
            val s = if (millions == millions.toLong().toDouble()) "${millions.toLong()}tr"
                    else String.format("%.1ftr", millions)
            s
        } else "${price / 1000}K"
    }

    // Format nhãn giá cho searching_geo: "(null, 2tr)" → "giá dưới 2 triệu", "(1tr, 2tr)" → "giá 1tr–2tr"
    private fun formatGeoSearchPriceLabel(minPrice: Long?, maxPrice: Long?): String = when {
        minPrice != null && maxPrice != null ->
            ", giá ${formatPriceShort(minPrice)}–${formatMaxPrice(maxPrice)}"
        maxPrice != null -> ", giá ${formatMaxPrice(maxPrice)}"
        minPrice != null -> ", giá từ ${formatPriceShort(minPrice)}"
        else -> ""
    }

    fun formatMaxPrice(price: Long): String {
        val millions = price / 1_000_000.0
        return when {
            millions >= 1.0 -> {
                val s = if (millions == millions.toLong().toDouble()) millions.toLong().toString()
                        else String.format("%.1f", millions)
                "dưới $s triệu"
            }
            price >= 1000 -> "dưới ${price / 1000}K"
            else -> "dưới $price"
        }
    }

    /** Format bán kính để hiển thị thân thiện: 0.5 → "500m", 1.0 → "1 km", 2.5 → "2.5 km" */
    fun formatRadius(radiusKm: Double): String {
        return if (radiusKm < 1.0) {
            "${(radiusKm * 1000).toInt()}m"
        } else {
            val s = if (radiusKm == radiusKm.toLong().toDouble()) radiusKm.toLong().toString()
                    else String.format("%.1f", radiusKm)
            "$s km"
        }
    }

    // Fix 1: Single source-of-truth for building a search response
    private fun buildSearchResponse(
        district: String?,
        maxPrice: Long?,
        minPrice: Long?,
        amenities: List<String>,
        ward: String?,
        ctx: ConversationContext,
        userInput: String
    ): ChatbotResponse {
        val canonical = district?.ifEmpty { null }
        val effectiveMax = if (maxPrice == -1L) null else maxPrice
        val districtLabel = when {
            ward != null && canonical != null -> "khu **$ward**, **$canonical**"
            canonical != null -> "khu **$canonical**"
            else -> "tất cả khu vực"
        }
        val priceLabel = when {
            minPrice != null && effectiveMax != null ->
                "giá ${formatMaxPrice(minPrice).replace("dưới ", "")} – ${formatMaxPrice(effectiveMax)}"
            effectiveMax != null -> formatMaxPrice(effectiveMax)
            else -> null
        }

        val currentRoomType = extractRoomType(userInput)
        val currentGender = extractGenderPrefer(userInput)
        val (currentMinArea, currentMaxArea) = extractAreaRange(userInput)
        val currentElec = extractElectricPrice(userInput)
        val currentWater = extractWaterPrice(userInput)
        val currentWifi = extractWifiPrice(userInput)
        val currentDep = extractDepositMonths(userInput)
        val currentPet = extractPetAllowed(userInput)
        val currentFree = extractFreeTime(userInput)

        val finalRoomType = currentRoomType ?: ctx.roomType
        val finalGender = currentGender ?: ctx.genderPrefer
        val finalMinArea = currentMinArea ?: ctx.minArea
        val finalMaxArea = currentMaxArea ?: ctx.maxArea
        val finalElec = currentElec ?: ctx.maxElectricPrice
        val finalWater = currentWater ?: ctx.maxWaterPrice
        val finalWifi = currentWifi ?: ctx.maxWifiPrice
        val finalDep = currentDep ?: ctx.maxDepositMonths
        val finalPet = currentPet ?: ctx.petAllowed
        val finalFree = currentFree ?: ctx.freeTime
        
        val finalAmenities = (ctx.amenities + amenities).distinct()

        return ChatbotResponse(
            answer = randomSearchingMsg(districtLabel, priceLabel),
            confidence = 1.0,
            searchParams = SearchParams(
                district = canonical,
                maxPrice = effectiveMax,
                minPrice = minPrice,
                amenities = finalAmenities,
                ward = ward,
                minArea = finalMinArea,
                maxArea = finalMaxArea,
                roomType = finalRoomType,
                genderPrefer = finalGender,
                maxElectricPrice = finalElec,
                maxWaterPrice = finalWater,
                maxWifiPrice = finalWifi,
                maxDepositMonths = finalDep,
                petAllowed = finalPet,
                freeTime = finalFree
            ),
            quickReplies = listOf("Lọc theo giá", "Đổi khu vực", "🔍 Tìm lại"),
            nextContext = ctx.copy(
                state = ConversationState.IDLE,
                district = null,
                maxPrice = null,
                minPrice = null,
                ward = null,
                amenities = emptyList(),
                minArea = null,
                maxArea = null,
                roomType = null,
                genderPrefer = null,
                maxElectricPrice = null,
                maxWaterPrice = null,
                maxWifiPrice = null,
                maxDepositMonths = null,
                petAllowed = null,
                freeTime = null,
                lastSearchDistrict = canonical,
                lastSearchMaxPrice = effectiveMax,
                lastSearchMinPrice = minPrice,
                lastSearchWard = ward,
                lastSearchAmenities = finalAmenities,
                lastSearchMinArea = finalMinArea,
                lastSearchMaxArea = finalMaxArea,
                lastSearchRoomType = finalRoomType,
                lastSearchGenderPrefer = finalGender,
                lastSearchMaxElectricPrice = finalElec,
                lastSearchMaxWaterPrice = finalWater,
                lastSearchMaxWifiPrice = finalWifi,
                lastSearchMaxDepositMonths = finalDep,
                lastSearchPetAllowed = finalPet,
                lastSearchFreeTime = finalFree
            )
        )
    }

    // --- Public entry point ---

    fun processMessage(userInput: String, context: ConversationContext = ConversationContext()): ChatbotResponse {
        val sanitized = Normalizer.normalize(userInput.trim(), Normalizer.Form.NFC).take(MAX_INPUT_LENGTH)
        val result = processMessageCore(sanitized, context)
        return result.copy(nextContext = result.nextContext.copy(lastUserQuery = sanitized))
    }

    // --- Core processing ---

    private fun processMessageCore(sanitized: String, context: ConversationContext): ChatbotResponse {
        if (sanitized.isBlank()) {
            return ChatbotResponse(randomFallbackMsg(), 0.0, quickReplies = FALLBACK_QUICK_REPLIES)
        }

        val resolved = resolveReferences(sanitized, context)

        // Fix 6: handle "no more results" sentinel before re-processing
        if (resolved == NO_MORE_RESULTS_SENTINEL) {
            val count = context.lastResultCount.coerceAtLeast(0)
            return ChatbotResponse(
                answer = randomUiResponse("no_more_results", mapOf("count" to count.toString())),
                confidence = 1.0,
                quickReplies = listOf("Đổi khu vực", "Đổi mức giá", "🔍 Tìm lại"),
                nextContext = context
            )
        }

        if (resolved != sanitized) {
            return processMessageCore(resolved, context)
        }

        val sanitizedLower = sanitized.lowercase().trim()
        when (sanitizedLower) {
            "tìm lại" -> {
                // Ưu tiên: retry geo-search nếu lần trước là tìm theo địa chỉ
                val hasGeoSearch = context.lastSearchAddressQuery != null && context.lastSearchRadiusKm != null
                val hasAnySearch = hasGeoSearch || context.lastSearchDistrict != null || context.lastSearchMaxPrice != null

                if (hasAnySearch) {
                    // Build SearchParams đầy đủ từ toàn bộ lastSearch* fields
                    val fullParams = SearchParams(
                        district = context.lastSearchDistrict,
                        maxPrice = context.lastSearchMaxPrice,
                        minPrice = context.lastSearchMinPrice,
                        ward = context.lastSearchWard,
                        amenities = context.lastSearchAmenities,
                        addressQuery = context.lastSearchAddressQuery,
                        radiusKm = context.lastSearchRadiusKm,
                        minArea = context.lastSearchMinArea,
                        maxArea = context.lastSearchMaxArea,
                        roomType = context.lastSearchRoomType,
                        genderPrefer = context.lastSearchGenderPrefer,
                        maxElectricPrice = context.lastSearchMaxElectricPrice,
                        maxWaterPrice = context.lastSearchMaxWaterPrice,
                        maxWifiPrice = context.lastSearchMaxWifiPrice,
                        maxDepositMonths = context.lastSearchMaxDepositMonths,
                        petAllowed = context.lastSearchPetAllowed,
                        freeTime = context.lastSearchFreeTime
                    )

                    val answer = if (hasGeoSearch) {
                        val radiusLabel = formatRadius(context.lastSearchRadiusKm!!)
                        val priceLabel = formatGeoSearchPriceLabel(context.lastSearchMinPrice, context.lastSearchMaxPrice)
                        "Đang tìm lại phòng bán kính $radiusLabel quanh địa chỉ đó$priceLabel..."
                    } else {
                        val districtLabel = when {
                            context.lastSearchWard != null && context.lastSearchDistrict != null ->
                                "khu **${context.lastSearchWard}**, **${context.lastSearchDistrict}**"
                            context.lastSearchDistrict != null -> "khu **${context.lastSearchDistrict}**"
                            else -> "tất cả khu vực"
                        }
                        val priceLabel = if (context.lastSearchMaxPrice != null)
                            formatMaxPrice(context.lastSearchMaxPrice) else "không giới hạn"
                        "Đang tìm lại phòng ở $districtLabel, giá $priceLabel..."
                    }

                    return ChatbotResponse(
                        answer = answer,
                        confidence = 1.0,
                        searchParams = fullParams,
                        nextContext = context.copy(
                            state = ConversationState.IDLE,
                            district = null, maxPrice = null,
                            minPrice = null, ward = null
                        )
                    )
                }
            }
            "đổi khu vực" -> {
                val priceHint = if (context.lastSearchMaxPrice != null)
                    "\n_(Giá hiện tại: ${formatMaxPrice(context.lastSearchMaxPrice)} — sẽ giữ nguyên)_" else ""
                return ChatbotResponse(
                    answer = randomAskDistrictMsg() + priceHint,
                    confidence = 1.0,
                    quickReplies = listOf("Cầu Giấy", "Đống Đa", "Hoàng Mai", "Thanh Xuân"),
                    nextContext = context.copy(
                        state = ConversationState.AWAIT_DISTRICT,
                        maxPrice = context.lastSearchMaxPrice
                    )
                )
            }
            "đổi mức giá" -> {
                val districtHint = if (context.lastSearchDistrict != null)
                    "\n_(Khu vực: **${context.lastSearchDistrict}** — sẽ giữ nguyên)_" else ""
                return ChatbotResponse(
                    answer = randomAskPriceMsg(districtHint),
                    confidence = 1.0,
                    quickReplies = listOf("2 triệu", "3 triệu", "5 triệu", "Tùy giá"),
                    nextContext = context.copy(
                        state = ConversationState.AWAIT_PRICE,
                        district = context.lastSearchDistrict
                    )
                )
            }
            "đổi bán kính" -> {
                // Giữ lại địa chỉ geo cũ (nếu có), hỏi bán kính mới
                val addrHint = if (context.lastSearchAddressQuery != null)
                    "\n_(Địa chỉ: **${context.lastSearchAddressQuery}** — sẽ giữ nguyên)_" else ""
                return ChatbotResponse(
                    answer = randomUiResponse("ask_radius") + addrHint,
                    confidence = 1.0,
                    quickReplies = RADIUS_QUICK_REPLIES,
                    nextContext = context.copy(
                        state = ConversationState.AWAIT_RADIUS,
                        pendingGeoAddress = context.lastSearchAddressQuery
                    )
                )
            }
        }

        when (context.state) {
            ConversationState.AWAIT_DISTRICT -> {
                val district = extractDistrict(sanitized)
                return if (district != null) {
                    val canonical = district.ifEmpty { null }
                    val ward = extractWard(sanitized, canonical)
                    val existingPrice = context.maxPrice
                    if (existingPrice != null) {
                        buildSearchResponse(canonical, existingPrice, context.minPrice, emptyList(), ward, context, sanitized)
                    } else {
                        askWardOrPrice(canonical, ward, context, sanitized)
                    }
                } else {
                    ChatbotResponse(
                        answer = randomDistrictNotFoundMsg(),
                        confidence = 1.0,
                        quickReplies = listOf("Cầu Giấy", "Đống Đa", "Hoàng Mai", "Thanh Xuân"),
                        nextContext = context
                    )
                }
            }

            ConversationState.AWAIT_WARD -> {
                val lower = sanitized.lowercase().trim()
                // User can skip ward with "Cả quận", "Tùy", skip keywords, etc.
                val skip = SKIP_KEYWORDS.any { lower.contains(it) }
                    || lower.contains("cả quận") || lower.contains("ca quan")
                    || lower.contains("toàn quận") || lower.contains("toan quan")
                    || lower.contains("tất cả")
                return if (skip) {
                    val districtHint = if (context.district != null)
                        "\n_(Khu vực: **${context.district}** — toàn quận)_" else ""
                    ChatbotResponse(
                        answer = randomAskPriceMsg(districtHint),
                        confidence = 1.0,
                        quickReplies = listOf("2 triệu", "3 triệu", "5 triệu", "Tùy giá"),
                        nextContext = context.copy(state = ConversationState.AWAIT_PRICE, ward = null)
                    )
                } else {
                    val ward = extractWard(sanitized, context.district)
                    if (ward != null) {
                        val districtDisplay = if (context.district != null)
                            "$ward, ${context.district}" else ward
                        ChatbotResponse(
                            answer = randomUiResponse("ask_price_with_district",
                                mapOf("district" to districtDisplay)),
                            confidence = 1.0,
                            quickReplies = listOf("2 triệu", "3 triệu", "5 triệu", "Tùy giá"),
                            nextContext = context.copy(state = ConversationState.AWAIT_PRICE, ward = ward)
                        )
                    } else {
                        // Ward not recognised — prompt again with chips
                        val chips = getWardQuickReplies(context.district) + listOf("Cả quận")
                        ChatbotResponse(
                            answer = randomUiResponse("ward_not_found",
                                mapOf("district" to (context.district ?: ""))),
                            confidence = 1.0,
                            quickReplies = chips,
                            nextContext = context
                        )
                    }
                }
            }

            ConversationState.AWAIT_PRICE -> {
                val (minP, maxP) = extractPriceRange(sanitized)
                return if (maxP != null) {
                    buildSearchResponse(context.district, maxP, minP,
                        extractAmenities(sanitized), context.ward, context, sanitized)
                } else {
                    ChatbotResponse(
                        answer = randomPriceNotFoundMsg(),
                        confidence = 1.0,
                        quickReplies = listOf("2 triệu", "3 triệu", "5 triệu", "Tùy giá"),
                        nextContext = context
                    )
                }
            }

            ConversationState.AWAIT_RADIUS -> {
                val radius = extractRadius(sanitized)
                return when {
                    radius == null -> {
                        // Không nhận ra bán kính → hỏi lại
                        ChatbotResponse(
                            answer = randomUiResponse("radius_not_found"),
                            confidence = 1.0,
                            quickReplies = RADIUS_QUICK_REPLIES,
                            nextContext = context
                        )
                    }
                    radius == -1.0 -> {
                        // Skip → dùng bán kính mặc định 1km
                        val effectiveRadius = 1.0
                        val radiusLabel = formatRadius(effectiveRadius)
                        val priceAlreadyKnown = context.maxPrice != null || context.minPrice != null
                        if (priceAlreadyKnown) {
                            // Giá đã biết từ câu hỏi ban đầu → tìm kiếm ngay
                            val priceLabel = formatGeoSearchPriceLabel(context.minPrice, context.maxPrice)
                            ChatbotResponse(
                                answer = randomUiResponse("searching_geo", mapOf("radius" to radiusLabel, "price" to priceLabel)),
                                confidence = 1.0,
                                searchParams = SearchParams(
                                    addressQuery = context.pendingGeoAddress ?: "",
                                    radiusKm = effectiveRadius,
                                    maxPrice = context.maxPrice,
                                    minPrice = context.minPrice,
                                    amenities = context.amenities,
                                    minArea = context.minArea,
                                    maxArea = context.maxArea,
                                    roomType = context.roomType,
                                    genderPrefer = context.genderPrefer,
                                    maxElectricPrice = context.maxElectricPrice,
                                    maxWaterPrice = context.maxWaterPrice,
                                    maxWifiPrice = context.maxWifiPrice,
                                    maxDepositMonths = context.maxDepositMonths,
                                    petAllowed = context.petAllowed,
                                    freeTime = context.freeTime
                                ),
                                quickReplies = listOf("🔍 Tìm lại", "Đổi bán kính", "Đổi mức giá"),
                                nextContext = context.copy(
                                    state = ConversationState.IDLE,
                                    pendingGeoAddress = null,
                                    pendingRadiusKm = null,
                                    maxPrice = null, minPrice = null,
                                    amenities = emptyList(),
                                    lastSearchAddressQuery = context.pendingGeoAddress,
                                    lastSearchRadiusKm = effectiveRadius,
                                    lastSearchMaxPrice = context.maxPrice,
                                    lastSearchMinPrice = context.minPrice,
                                    lastSearchAmenities = context.amenities
                                )
                            )
                        } else {
                            ChatbotResponse(
                                answer = randomUiResponse("ask_price_for_geo", mapOf("radius" to radiusLabel)),
                                confidence = 1.0,
                                quickReplies = listOf("2 triệu", "3 triệu", "5 triệu", "Tùy giá"),
                                nextContext = context.copy(
                                    state = ConversationState.AWAIT_PRICE_FOR_GEO,
                                    pendingRadiusKm = effectiveRadius
                                )
                            )
                        }
                    }
                    else -> {
                        val radiusLabel = formatRadius(radius)
                        val priceAlreadyKnown = context.maxPrice != null || context.minPrice != null
                        if (priceAlreadyKnown) {
                            // Giá đã biết từ câu hỏi ban đầu → tìm kiếm ngay
                            val priceLabel = formatGeoSearchPriceLabel(context.minPrice, context.maxPrice)
                            ChatbotResponse(
                                answer = randomUiResponse("searching_geo", mapOf("radius" to radiusLabel, "price" to priceLabel)),
                                confidence = 1.0,
                                searchParams = SearchParams(
                                    addressQuery = context.pendingGeoAddress ?: "",
                                    radiusKm = radius,
                                    maxPrice = context.maxPrice,
                                    minPrice = context.minPrice,
                                    amenities = context.amenities,
                                    minArea = context.minArea,
                                    maxArea = context.maxArea,
                                    roomType = context.roomType,
                                    genderPrefer = context.genderPrefer,
                                    maxElectricPrice = context.maxElectricPrice,
                                    maxWaterPrice = context.maxWaterPrice,
                                    maxWifiPrice = context.maxWifiPrice,
                                    maxDepositMonths = context.maxDepositMonths,
                                    petAllowed = context.petAllowed,
                                    freeTime = context.freeTime
                                ),
                                quickReplies = listOf("🔍 Tìm lại", "Đổi bán kính", "Đổi mức giá"),
                                nextContext = context.copy(
                                    state = ConversationState.IDLE,
                                    pendingGeoAddress = null,
                                    pendingRadiusKm = null,
                                    maxPrice = null, minPrice = null,
                                    amenities = emptyList(),
                                    lastSearchAddressQuery = context.pendingGeoAddress,
                                    lastSearchRadiusKm = radius,
                                    lastSearchMaxPrice = context.maxPrice,
                                    lastSearchMinPrice = context.minPrice,
                                    lastSearchAmenities = context.amenities
                                )
                            )
                        } else {
                            ChatbotResponse(
                                answer = randomUiResponse("ask_price_for_geo", mapOf("radius" to radiusLabel)),
                                confidence = 1.0,
                                quickReplies = listOf("2 triệu", "3 triệu", "5 triệu", "Tùy giá"),
                                nextContext = context.copy(
                                    state = ConversationState.AWAIT_PRICE_FOR_GEO,
                                    pendingRadiusKm = radius
                                )
                            )
                        }
                    }
                }
            }

            ConversationState.AWAIT_PRICE_FOR_GEO -> {
                val (minP, maxP) = extractPriceRange(sanitized)
                val address = context.pendingGeoAddress ?: ""
                val radiusKm = context.pendingRadiusKm ?: 1.0
                return if (maxP != null) {
                    val effectiveMax = if (maxP == -1L) null else maxP
                    val radiusLabel = formatRadius(radiusKm)
                    val priceLabel = if (effectiveMax != null) ", giá ${formatMaxPrice(effectiveMax)}" else ""
                    val currentAmenities = (context.amenities + extractAmenities(sanitized)).distinct()
                    ChatbotResponse(
                        answer = randomUiResponse("searching_geo",
                            mapOf("radius" to radiusLabel, "price" to priceLabel)),
                        confidence = 1.0,
                        searchParams = SearchParams(
                            addressQuery = address,
                            radiusKm = radiusKm,
                            maxPrice = effectiveMax,
                            minPrice = minP,
                            amenities = currentAmenities,
                            minArea = context.minArea,
                            maxArea = context.maxArea,
                            roomType = context.roomType,
                            genderPrefer = context.genderPrefer,
                            maxElectricPrice = context.maxElectricPrice,
                            maxWaterPrice = context.maxWaterPrice,
                            maxWifiPrice = context.maxWifiPrice,
                            maxDepositMonths = context.maxDepositMonths,
                            petAllowed = context.petAllowed,
                            freeTime = context.freeTime
                        ),
                        quickReplies = listOf("🔍 Tìm lại", "Đổi bán kính", "Đổi mức giá"),
                        nextContext = context.copy(
                            state = ConversationState.IDLE,
                            pendingGeoAddress = null,
                            pendingRadiusKm = null,
                            amenities = emptyList(),
                            minArea = null,
                            maxArea = null,
                            roomType = null,
                            genderPrefer = null,
                            maxElectricPrice = null,
                            maxWaterPrice = null,
                            maxWifiPrice = null,
                            maxDepositMonths = null,
                            petAllowed = null,
                            freeTime = null,
                            lastSearchAddressQuery = address,
                            lastSearchRadiusKm = radiusKm,
                            lastSearchMaxPrice = effectiveMax,
                            lastSearchMinPrice = minP,
                            lastSearchAmenities = currentAmenities,
                            lastSearchMinArea = context.minArea,
                            lastSearchMaxArea = context.maxArea,
                            lastSearchRoomType = context.roomType,
                            lastSearchGenderPrefer = context.genderPrefer,
                            lastSearchMaxElectricPrice = context.maxElectricPrice,
                            lastSearchMaxWaterPrice = context.maxWaterPrice,
                            lastSearchMaxWifiPrice = context.maxWifiPrice,
                            lastSearchMaxDepositMonths = context.maxDepositMonths,
                            lastSearchPetAllowed = context.petAllowed,
                            lastSearchFreeTime = context.freeTime
                        )
                    )
                } else {
                    // Không nhận ra giá → hỏi lại
                    ChatbotResponse(
                        answer = randomPriceNotFoundMsg(),
                        confidence = 1.0,
                        quickReplies = listOf("2 triệu", "3 triệu", "5 triệu", "Tùy giá"),
                        nextContext = context
                    )
                }
            }

            ConversationState.IDLE -> { /* fall through */ }
        }

        // --- Ƭu tiên kiểm tra geo-address intent trước search intent thông thường ---
        if (isGeoAddressIntent(sanitized)) {
            val address = extractAddressQuery(sanitized)
            val inlineRadius = extractRadius(sanitized)
            val (inlineMinP, inlineMaxP) = extractPriceRange(sanitized)

            val currentRoomType = extractRoomType(sanitized)
            val currentGender = extractGenderPrefer(sanitized)
            val (currentMinArea, currentMaxArea) = extractAreaRange(sanitized)
            val currentElec = extractElectricPrice(sanitized)
            val currentWater = extractWaterPrice(sanitized)
            val currentWifi = extractWifiPrice(sanitized)
            val currentDep = extractDepositMonths(sanitized)
            val currentPet = extractPetAllowed(sanitized)
            val currentFree = extractFreeTime(sanitized)
            val currentAmenities = extractAmenities(sanitized)

            return when {
                // Có cả bán kính và giá trong 1 câu → tìm kiếm ngay
                inlineRadius != null && inlineRadius > 0 && inlineMaxP != null -> {
                    val effectiveMax = if (inlineMaxP == -1L) null else inlineMaxP
                    val radiusLabel = formatRadius(inlineRadius)
                    val priceLabel = if (effectiveMax != null) ", giá ${formatMaxPrice(effectiveMax)}" else ""
                    ChatbotResponse(
                        answer = randomUiResponse("searching_geo",
                            mapOf("radius" to radiusLabel, "price" to priceLabel)),
                        confidence = 1.0,
                        searchParams = SearchParams(
                            addressQuery = address,
                            radiusKm = inlineRadius,
                            maxPrice = effectiveMax,
                            minPrice = inlineMinP,
                            amenities = currentAmenities,
                            minArea = currentMinArea,
                            maxArea = currentMaxArea,
                            roomType = currentRoomType,
                            genderPrefer = currentGender,
                            maxElectricPrice = currentElec,
                            maxWaterPrice = currentWater,
                            maxWifiPrice = currentWifi,
                            maxDepositMonths = currentDep,
                            petAllowed = currentPet,
                            freeTime = currentFree
                        ),
                        quickReplies = listOf("🔍 Tìm lại", "Đổi bán kính", "Đổi mức giá"),
                        nextContext = context.copy(
                            state = ConversationState.IDLE,
                            lastSearchAddressQuery = address,
                            lastSearchRadiusKm = inlineRadius,
                            lastSearchMaxPrice = effectiveMax,
                            lastSearchMinPrice = inlineMinP,
                            lastSearchAmenities = currentAmenities,
                            lastSearchMinArea = currentMinArea,
                            lastSearchMaxArea = currentMaxArea,
                            lastSearchRoomType = currentRoomType,
                            lastSearchGenderPrefer = currentGender,
                            lastSearchMaxElectricPrice = currentElec,
                            lastSearchMaxWaterPrice = currentWater,
                            lastSearchMaxWifiPrice = currentWifi,
                            lastSearchMaxDepositMonths = currentDep,
                            lastSearchPetAllowed = currentPet,
                            lastSearchFreeTime = currentFree
                        )
                    )
                }
                // Có bán kính nhưng chưa có giá → hỏi giá
                inlineRadius != null && inlineRadius > 0 -> {
                    val radiusLabel = formatRadius(inlineRadius)
                    ChatbotResponse(
                        answer = randomUiResponse("ask_price_for_geo", mapOf("radius" to radiusLabel)),
                        confidence = 1.0,
                        quickReplies = listOf("2 triệu", "3 triệu", "5 triệu", "Tùy giá"),
                        nextContext = context.copy(
                            state = ConversationState.AWAIT_PRICE_FOR_GEO,
                            pendingGeoAddress = address,
                            pendingRadiusKm = inlineRadius,
                            amenities = currentAmenities,
                            minArea = currentMinArea,
                            maxArea = currentMaxArea,
                            roomType = currentRoomType,
                            genderPrefer = currentGender,
                            maxElectricPrice = currentElec,
                            maxWaterPrice = currentWater,
                            maxWifiPrice = currentWifi,
                            maxDepositMonths = currentDep,
                            petAllowed = currentPet,
                            freeTime = currentFree
                        )
                    )
                }
                // Chưa có bán kính → hỏi bán kính trước
                // Lưu giá đã extract (nếu có) để tránh hỏi lại giá sau khi nhận bán kính
                else -> ChatbotResponse(
                    answer = randomUiResponse("ask_radius"),
                    confidence = 1.0,
                    quickReplies = RADIUS_QUICK_REPLIES,
                    nextContext = context.copy(
                        state = ConversationState.AWAIT_RADIUS,
                        pendingGeoAddress = address,
                        amenities = currentAmenities,
                        minArea = currentMinArea,
                        maxArea = currentMaxArea,
                        roomType = currentRoomType,
                        genderPrefer = currentGender,
                        maxElectricPrice = currentElec,
                        maxWaterPrice = currentWater,
                        maxWifiPrice = currentWifi,
                        maxDepositMonths = currentDep,
                        petAllowed = currentPet,
                        freeTime = currentFree,
                        maxPrice = if (inlineMaxP != null && inlineMaxP != -1L) inlineMaxP else null,
                        minPrice = inlineMinP
                    )
                )
            }
        }

        if (isSearchIntent(sanitized)) {
            val district = extractDistrict(sanitized)
            val (minP, maxP) = extractPriceRange(sanitized)
            val amenities = extractAmenities(sanitized)
            val canonical = district?.ifEmpty { null }
            val ward = extractWard(sanitized, canonical)
            return when {
                // Price already known → search immediately (ward extracted if present)
                canonical != null && maxP != null ->
                    buildSearchResponse(canonical, maxP, minP, amenities, ward, context, sanitized)
                // District known, no price → ask ward (if applicable) then price
                canonical != null -> askWardOrPrice(canonical, ward, context, sanitized)
                else -> ChatbotResponse(
                    answer = "Mình sẽ giúp bạn tìm phòng trọ phù hợp!\n\nBạn muốn tìm phòng ở khu vực nào?\n_(Ví dụ: Cầu Giấy, Đống Đa, Hoàng Mai, Thanh Xuân...)_",
                    confidence = 1.0,
                    quickReplies = listOf("Cầu Giấy", "Đống Đa", "Hoàng Mai", "Thanh Xuân"),
                    nextContext = context.copy(state = ConversationState.AWAIT_DISTRICT)
                )
            }
        }

        // Augment short queries with previous query for better BM25 matching
        val wordCount = tokenize(sanitized).count { !it.contains(' ') }
        val augmentedInput = if (wordCount <= 2 && context.lastUserQuery != null &&
                                 context.lastUserQuery != sanitized) {
            "${context.lastUserQuery} $sanitized"
        } else {
            sanitized
        }

        val inputVector = computeBM25(augmentedInput)
        if (inputVector.isEmpty()) {
            return ChatbotResponse(randomFallbackMsg(), 0.0, quickReplies = FALLBACK_QUICK_REPLIES)
        }

        val threshold = dynamicThreshold(wordCount)

        val intentAllScores = mutableMapOf<String, MutableList<Double>>()
        for ((intentId, patternVector) in patternVectors) {
            val similarity = cosineSimilarity(inputVector, patternVector)
            intentAllScores.getOrPut(intentId) { mutableListOf() }.add(similarity)
        }

        val intentScores = mutableMapOf<String, Double>()
        for ((intentId, scores) in intentAllScores) {
            val top3 = scores.sortedDescending().take(3)
            intentScores[intentId] = top3.first() * 0.7 + top3.average() * 0.3
        }

        val bestMatch = intentScores.maxByOrNull { it.value }
        if (bestMatch == null || bestMatch.value < threshold) {
            // ===== Smart Fallback 3 cấp độ (Offline, không API) =====
            val lowerSan = sanitized.lowercase()

            // Cấp 1: Trích xuất district từ câu phức tạp → route thẳng vào luồng tìm kiếm
            // VD: "mình sinh viên bách khoa muốn thuê phòng gần trường ở đống đa"
            val districtFallback = extractDistrict(sanitized)
            val canonicalFallback = districtFallback?.ifEmpty { null }
            if (canonicalFallback != null) {
                val (minPF, maxPF) = extractPriceRange(sanitized)
                val amenitiesF = extractAmenities(sanitized)
                val wardF = extractWard(sanitized, canonicalFallback)
                return when {
                    maxPF != null ->
                        buildSearchResponse(canonicalFallback, maxPF, minPF, amenitiesF, wardF, context, sanitized)
                    else -> askWardOrPrice(canonicalFallback, wardF, context, sanitized)
                }
            }

            // Cấp 2: Trích xuất giá nhưng không có quận → hỏi quận, ghi nhớ giá
            // VD: "mình có khoảng 2 triệu rưỡi muốn tìm phòng" hoặc "ngân sách 3 triệu"
            val (minPF2, maxPF2) = extractPriceRange(sanitized)
            val hasPriceInfo = maxPF2 != null && maxPF2 > 0L
            val looksLikeSearch = ROOM_HINT_WORDS.any { lowerSan.contains(it) }
            if (hasPriceInfo && (looksLikeSearch ||
                SEARCH_INTENT_KEYWORDS.any { lowerSan.contains(it) })) {
                val priceHint = "\n_(Giá tối đa: **${formatMaxPrice(maxPF2!!)}** — mình đã ghi nhớ)_"
                return ChatbotResponse(
                    answer = randomAskDistrictMsg() + priceHint,
                    confidence = 0.5,
                    quickReplies = listOf("Cầu Giấy", "Đống Đa", "Hoàng Mai", "Thanh Xuân"),
                    nextContext = context.copy(
                        state = ConversationState.AWAIT_DISTRICT,
                        maxPrice = maxPF2,
                        minPrice = minPF2
                    )
                )
            }

            // Cấp 3: Phát hiện từ khoá phòng/trọ → dẫn dắt tìm kiếm rõ ràng hơn
            // VD: "tôi cần thuê chỗ ở gấp", "kiếm chỗ trọ được không"
            return if (looksLikeSearch || SEARCH_INTENT_KEYWORDS.any { lowerSan.contains(it) }) {
                ChatbotResponse(
                    answer = "Mình hiểu bạn đang cần tìm chỗ ở! 🏠\n\nĐể tìm phòng phù hợp, bạn cho mình biết:\n**Bạn muốn thuê phòng ở khu vực nào tại Hà Nội?**\n_(Ví dụ: Hà Đông, Cầu Giấy, Hoàng Mai, Thanh Xuân, Đống Đa...)_",
                    confidence = 0.0,
                    quickReplies = listOf("Hà Đông", "Cầu Giấy", "Hoàng Mai", "Thanh Xuân", "Đống Đa"),
                    nextContext = context.copy(state = ConversationState.AWAIT_DISTRICT)
                )
            } else {
                ChatbotResponse(randomFallbackMsg(), 0.0, quickReplies = FALLBACK_QUICK_REPLIES)
            }
        }

        val matched = intents.find { it.id == bestMatch.key }
            ?: return ChatbotResponse(FALLBACK_RESPONSE, 0.0, quickReplies = FALLBACK_QUICK_REPLIES)

        if (matched.id == "search_room" || matched.id == "search_by_amenity") {
            val district = extractDistrict(sanitized)
            val (minP, maxP) = extractPriceRange(sanitized)
            val amenities = extractAmenities(sanitized)
            val canonical = district?.ifEmpty { null }
            val ward = extractWard(sanitized, canonical)
            return when {
                canonical != null && maxP != null ->
                    buildSearchResponse(canonical, maxP, minP, amenities, ward, context, sanitized)
                canonical != null -> askWardOrPrice(canonical, ward, context, sanitized)
                else -> ChatbotResponse(
                    answer = matched.randomResponse(),
                    confidence = 1.0,
                    quickReplies = listOf("Cầu Giấy", "Đống Đa", "Hoàng Mai", "Thanh Xuân"),
                    nextContext = context.copy(
                        state = ConversationState.AWAIT_DISTRICT,
                        amenities = amenities
                    )
                )
            }
        }

        return ChatbotResponse(matched.randomResponse(), bestMatch.value, matched.deepLink)
    }

    private fun loadFromJson() {
        try {
            val json = context.assets.open("chatbot_data.json").bufferedReader().use { it.readText() }
            val root = JSONObject(json)

            if (root.has("ui_responses")) {
                val uiObj = root.getJSONObject("ui_responses")
                uiObj.keys().forEach { key ->
                    val arr = uiObj.getJSONArray(key)
                    uiResponses[key] = (0 until arr.length()).map { arr.getString(it) }
                }
            }

            val intentsArray: JSONArray = root.getJSONArray("intents")
            for (i in 0 until intentsArray.length()) {
                val obj = intentsArray.getJSONObject(i)
                val id = obj.getString("id")
                val deepLink = if (obj.has("deepLink") && !obj.isNull("deepLink"))
                    obj.getString("deepLink") else null
                val patternsArray = obj.getJSONArray("patterns")
                val patterns = (0 until patternsArray.length()).map { j -> patternsArray.getString(j) }
                val responses = when {
                    obj.has("responses") -> {
                        val arr = obj.getJSONArray("responses")
                        (0 until arr.length()).map { j -> arr.getString(j) }
                    }
                    else -> listOf(obj.getString("response"))
                }
                intents.add(Intent(id, patterns, responses, deepLink))
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatbotEngine", "Failed to load chatbot_data.json", e)
        }
    }
}
