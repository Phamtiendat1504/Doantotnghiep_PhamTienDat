package com.example.doantotnghiep.AI

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import kotlin.math.ln
import kotlin.math.sqrt

class ChatbotEngine(private val context: Context) {

    data class Intent(
        val id: String,
        val patterns: List<String>,
        val response: String,
        val deepLink: String? = null
    )

    enum class ConversationState { IDLE, AWAIT_DISTRICT, AWAIT_PRICE, READY_TO_SEARCH }

    data class ConversationContext(
        val state: ConversationState = ConversationState.IDLE,
        val district: String? = null,
        val maxPrice: Long? = null,
        val lastSearchDistrict: String? = null,
        val lastSearchMaxPrice: Long? = null
    )

    data class SearchParams(val district: String? = null, val maxPrice: Long? = null)

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

    companion object {
        private const val BASE_THRESHOLD = 0.15
        private const val MAX_INPUT_LENGTH = 150

        val QUICK_REPLY_ACTIONS = mapOf(
            "🔍 Tìm phòng ngay" to "tìm phòng trọ",
            "📝 Đăng tin mới" to "đăng tin phòng",
            "📞 Hỗ trợ trực tiếp" to "liên hệ hỗ trợ",
            "🔍 Tìm lại" to "tìm lại",
            "Đổi khu vực" to "đổi khu vực",
            "Đổi mức giá" to "đổi mức giá"
        )

        private val FALLBACK_QUICK_REPLIES = listOf("🔍 Tìm phòng ngay", "📝 Đăng tin mới", "📞 Hỗ trợ trực tiếp")

        private const val FALLBACK_RESPONSE =
            "Xin lỗi, mình chưa hiểu câu hỏi của bạn.\nBạn có thể hỏi về:\n" +
            "• Cách tìm kiếm phòng\n" +
            "• Đặt lịch hẹn xem phòng\n" +
            "• Đăng tin phòng trọ\n" +
            "• Các tính năng khác của ứng dụng\n" +
            "Hoặc gõ 'menu' để xem danh sách đầy đủ!"

        private val NORMALIZATION_DICT = mapOf(
            "ptro" to "phòng trọ",
            "phongtro" to "phòng trọ",
            "ko" to "không",
            "k" to "không",
            "khg" to "không",
            "hk" to "không",
            "đc" to "được",
            "dc" to "được",
            "bn" to "bạn",
            "mk" to "mình",
            "vs" to "với",
            "tn" to "thế nào",
            "app" to "ứng dụng",
            "acc" to "tài khoản",
            "account" to "tài khoản",
            "pass" to "mật khẩu",
            "password" to "mật khẩu",
            "login" to "đăng nhập",
            "register" to "đăng ký",
            "signup" to "đăng ký",
            "ntn" to "như thế nào",
            "đk" to "đăng ký",
            "đn" to "đăng nhập",
            "nt" to "nhắn tin",
            "hd" to "hướng dẫn",
            "tui" to "tôi",
            "mik" to "mình",
            "bro" to "bạn",
            "bug" to "lỗi",
            "error" to "lỗi",
            "cancel" to "hủy",
            "book" to "đặt lịch",
            "chat" to "nhắn tin",
            "update" to "cập nhật",
            "check" to "kiểm tra",
            "dv" to "dịch vụ",
            "tt" to "thông tin",
            "sdt" to "số điện thoại"
        )

        // Sorted by length descending to avoid "q1" matching "q10" first
        private val DISTRICT_ALIASES = listOf(
            // Hà Nội
            "nam từ liêm" to "Nam Từ Liêm", "nam tu liem" to "Nam Từ Liêm",
            "bắc từ liêm" to "Bắc Từ Liêm", "bac tu liem" to "Bắc Từ Liêm",
            "hai bà trưng" to "Hai Bà Trưng", "hai ba trung" to "Hai Bà Trưng",
            "hoàn kiếm" to "Hoàn Kiếm", "hoan kiem" to "Hoàn Kiếm",
            "thanh xuân" to "Thanh Xuân", "thanh xuan" to "Thanh Xuân",
            "hoàng mai" to "Hoàng Mai", "hoang mai" to "Hoàng Mai",
            "long biên" to "Long Biên", "long bien" to "Long Biên",
            "đống đa" to "Đống Đa", "dong da" to "Đống Đa",
            "cầu giấy" to "Cầu Giấy", "cau giay" to "Cầu Giấy",
            "ba đình" to "Ba Đình", "ba dinh" to "Ba Đình",
            "tây hồ" to "Tây Hồ", "tay ho" to "Tây Hồ",
            "đông anh" to "Đông Anh", "dong anh" to "Đông Anh",
            "gia lâm" to "Gia Lâm", "gia lam" to "Gia Lâm",
            "sóc sơn" to "Sóc Sơn", "soc son" to "Sóc Sơn",
            "hà đông" to "Hà Đông", "ha dong" to "Hà Đông",
            // TP. Hồ Chí Minh
            "tp thủ đức" to "Thủ Đức", "tp thu duc" to "Thủ Đức",
            "bình chánh" to "Bình Chánh", "binh chanh" to "Bình Chánh",
            "bình thạnh" to "Bình Thạnh", "binh thanh" to "Bình Thạnh",
            "phú nhuận" to "Phú Nhuận", "phu nhuan" to "Phú Nhuận",
            "tân bình" to "Tân Bình", "tan binh" to "Tân Bình",
            "tân phú" to "Tân Phú", "tan phu" to "Tân Phú",
            "bình tân" to "Bình Tân", "binh tan" to "Bình Tân",
            "thủ đức" to "Thủ Đức", "thu duc" to "Thủ Đức",
            "hóc môn" to "Hóc Môn", "hoc mon" to "Hóc Môn",
            "nhà bè" to "Nhà Bè", "nha be" to "Nhà Bè",
            "cần giờ" to "Cần Giờ", "can gio" to "Cần Giờ",
            "củ chi" to "Củ Chi", "cu chi" to "Củ Chi",
            "gò vấp" to "Gò Vấp", "go vap" to "Gò Vấp",
            "quận 12" to "Quận 12", "quan 12" to "Quận 12", "q.12" to "Quận 12", "q12" to "Quận 12",
            "quận 11" to "Quận 11", "quan 11" to "Quận 11", "q.11" to "Quận 11", "q11" to "Quận 11",
            "quận 10" to "Quận 10", "quan 10" to "Quận 10", "q.10" to "Quận 10", "q10" to "Quận 10",
            "quận 1" to "Quận 1", "quan 1" to "Quận 1", "q.1" to "Quận 1", "q1" to "Quận 1",
            "quận 2" to "Quận 2", "quan 2" to "Quận 2", "q.2" to "Quận 2", "q2" to "Quận 2",
            "quận 3" to "Quận 3", "quan 3" to "Quận 3", "q.3" to "Quận 3", "q3" to "Quận 3",
            "quận 4" to "Quận 4", "quan 4" to "Quận 4", "q.4" to "Quận 4", "q4" to "Quận 4",
            "quận 5" to "Quận 5", "quan 5" to "Quận 5", "q.5" to "Quận 5", "q5" to "Quận 5",
            "quận 6" to "Quận 6", "quan 6" to "Quận 6", "q.6" to "Quận 6", "q6" to "Quận 6",
            "quận 7" to "Quận 7", "quan 7" to "Quận 7", "q.7" to "Quận 7", "q7" to "Quận 7",
            "quận 8" to "Quận 8", "quan 8" to "Quận 8", "q.8" to "Quận 8", "q8" to "Quận 8",
            "quận 9" to "Quận 9", "quan 9" to "Quận 9", "q.9" to "Quận 9", "q9" to "Quận 9"
        )

        private val SKIP_KEYWORDS = setOf(
            "không biết", "không có", "tùy", "tuỳ", "bất kỳ", "bất kì",
            "không quan tâm", "không cần", "cũng được", "nào cũng", "thôi", "skip", "bỏ qua"
        )

        private val SEARCH_INTENT_KEYWORDS = setOf(
            "tìm phòng", "tìm trọ", "thuê phòng", "phòng trọ", "phòng cho thuê",
            "muốn thuê", "cần thuê", "đang tìm phòng", "tìm chỗ ở", "thuê trọ",
            "tìm kiếm phòng", "cho thuê phòng", "phòng giá",
            "cần phòng", "muốn tìm phòng", "muốn thuê trọ", "tìm nhà trọ",
            "tìm trọ giá", "phòng trọ giá", "chỗ ở giá rẻ", "thuê chỗ",
            "tim phong", "tim tro", "phong tro", "thue phong"
        )
    }

    fun initialize() {
        loadIntents()
        computeIdf()
        computePatternVectors()
    }

    // --- Tokenizer ---

    private fun normalize(words: List<String>): List<String> =
        words.map { word -> NORMALIZATION_DICT[word] ?: word }

    private fun tokenize(text: String): List<String> {
        val truncated = Normalizer.normalize(text, Normalizer.Form.NFC).take(MAX_INPUT_LENGTH)
        val cleaned = truncated.lowercase().trim().replace(
            Regex("[^a-záàảãạăắằẳẵặâấầẩẫậéèẻẽẹêếềểễệíìỉĩịóòỏõọôốồổỗộơớờởỡợúùủũụưứừửữựýỳỷỹỵđ0-9 ]"),
            " "
        )
        val words = normalize(
            cleaned.split(Regex("\\s+")).filter { it.length > 1 }
        )
        val bigrams = (0 until words.size - 1).map { i -> "${words[i]} ${words[i + 1]}" }
        return words + bigrams
    }

    // --- TF-IDF core ---

    private fun computeTf(tokens: List<String>): Map<String, Double> {
        if (tokens.isEmpty()) return emptyMap()
        val counts = mutableMapOf<String, Int>()
        for (token in tokens) counts[token] = (counts[token] ?: 0) + 1
        val total = tokens.size.toDouble()
        return counts.mapValues { it.value / total }
    }

    private fun computeIdf() {
        val allPatterns = intents.flatMap { it.patterns }
        val n = allPatterns.size.toDouble()
        val docFreq = mutableMapOf<String, Int>()
        for (pattern in allPatterns) {
            val uniqueTokens = tokenize(pattern).toSet()
            for (token in uniqueTokens) docFreq[token] = (docFreq[token] ?: 0) + 1
        }
        for ((token, df) in docFreq) {
            idfScores[token] = ln((n + 1.0) / (df + 1.0)) + 1.0
        }
    }

    private fun computeTfIdf(text: String): Map<String, Double> {
        val tokens = tokenize(text)
        val tf = computeTf(tokens)
        val totalPatterns = intents.flatMap { it.patterns }.size.toDouble()
        val result = mutableMapOf<String, Double>()
        for ((token, tfScore) in tf) {
            val idf = idfScores[token] ?: (ln((totalPatterns + 1.0) / 1.0) + 1.0)
            result[token] = tfScore * idf
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
                patternVectors.add(Pair(intent.id, computeTfIdf(pattern)))
            }
        }
    }

    private fun dynamicThreshold(wordCount: Int): Double = when {
        wordCount <= 2 -> 0.30
        wordCount <= 4 -> 0.22
        else -> BASE_THRESHOLD
    }

    // --- Entity extraction ---

    fun extractDistrict(input: String): String? {
        val lower = input.lowercase().trim()
        if (SKIP_KEYWORDS.any { lower.contains(it) }) return ""
        for ((alias, canonical) in DISTRICT_ALIASES) {
            if (lower.contains(alias)) return canonical
        }
        return null
    }

    fun extractPrice(input: String): Long? {
        val lower = input.lowercase().trim()
        if (SKIP_KEYWORDS.any { lower.contains(it) }) return -1L

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

    private fun isSearchIntent(input: String): Boolean {
        val lower = input.lowercase()
        return SEARCH_INTENT_KEYWORDS.any { lower.contains(it) }
    }

    private fun formatMaxPrice(price: Long): String {
        val millions = price / 1_000_000.0
        return when {
            millions >= 1.0 -> {
                val s = if (millions == millions.toLong().toDouble()) millions.toLong().toString()
                        else String.format("%.1f", millions)
                "dưới $s triệu"
            }
            price >= 1000 -> "dưới ${price / 1000}K"
            else -> "dưới $price đồng"
        }
    }

    // --- State machine ---

    fun processMessage(userInput: String, context: ConversationContext = ConversationContext()): ChatbotResponse {
        val sanitized = Normalizer.normalize(userInput.trim(), Normalizer.Form.NFC).take(MAX_INPUT_LENGTH)
        if (sanitized.isBlank()) {
            return ChatbotResponse(FALLBACK_RESPONSE, 0.0, quickReplies = FALLBACK_QUICK_REPLIES)
        }

        // Smart quick-reply handlers that carry previous search context
        val sanitizedLower = sanitized.lowercase().trim()
        when (sanitizedLower) {
            "tìm lại" -> {
                if (context.lastSearchDistrict != null || context.lastSearchMaxPrice != null) {
                    val districtLabel = if (context.lastSearchDistrict != null)
                        "khu **${context.lastSearchDistrict}**" else "tất cả khu vực"
                    val priceLabel = if (context.lastSearchMaxPrice != null)
                        formatMaxPrice(context.lastSearchMaxPrice) else "không giới hạn"
                    return ChatbotResponse(
                        answer = "Đang tìm lại phòng ở $districtLabel, giá $priceLabel...",
                        confidence = 1.0,
                        searchParams = SearchParams(
                            district = context.lastSearchDistrict,
                            maxPrice = context.lastSearchMaxPrice
                        ),
                        nextContext = ConversationContext(
                            lastSearchDistrict = context.lastSearchDistrict,
                            lastSearchMaxPrice = context.lastSearchMaxPrice
                        )
                    )
                }
                // No previous search recorded, fall through to normal search flow
            }
            "đổi khu vực" -> {
                val priceHint = if (context.lastSearchMaxPrice != null)
                    "\n_(Giá hiện tại: ${formatMaxPrice(context.lastSearchMaxPrice)} — sẽ giữ nguyên)_" else ""
                return ChatbotResponse(
                    answer = "Bạn muốn tìm phòng ở khu vực nào?$priceHint\n_(Ví dụ: Quận 1, Bình Thạnh, Gò Vấp...)_",
                    confidence = 1.0,
                    quickReplies = listOf("Quận 1", "Bình Thạnh", "Gò Vấp", "Thủ Đức"),
                    nextContext = ConversationContext(
                        state = ConversationState.AWAIT_DISTRICT,
                        maxPrice = context.lastSearchMaxPrice,
                        lastSearchDistrict = context.lastSearchDistrict,
                        lastSearchMaxPrice = context.lastSearchMaxPrice
                    )
                )
            }
            "đổi mức giá" -> {
                val districtHint = if (context.lastSearchDistrict != null)
                    "\n_(Khu vực: **${context.lastSearchDistrict}** — sẽ giữ nguyên)_" else ""
                return ChatbotResponse(
                    answer = "Bạn muốn tìm phòng với mức giá tối đa bao nhiêu?$districtHint\n_(Ví dụ: 3 triệu, 2.5tr, 1500k... hoặc \"tùy\" nếu không có yêu cầu)_",
                    confidence = 1.0,
                    quickReplies = listOf("2 triệu", "3 triệu", "5 triệu", "Tùy giá"),
                    nextContext = ConversationContext(
                        state = ConversationState.AWAIT_PRICE,
                        district = context.lastSearchDistrict,
                        lastSearchDistrict = context.lastSearchDistrict,
                        lastSearchMaxPrice = context.lastSearchMaxPrice
                    )
                )
            }
        }

        when (context.state) {
            ConversationState.AWAIT_DISTRICT -> {
                val district = extractDistrict(sanitized)
                return if (district != null) {
                    val canonical = district.ifEmpty { null }
                    val existingPrice = context.maxPrice
                    if (existingPrice != null) {
                        // Price already known (from "Đổi khu vực") — search immediately
                        val districtLabel = if (canonical != null) "khu **$canonical**" else "tất cả khu vực"
                        val priceLabel = formatMaxPrice(existingPrice)
                        ChatbotResponse(
                            answer = "Đang tìm phòng ở $districtLabel, giá $priceLabel...",
                            confidence = 1.0,
                            searchParams = SearchParams(district = canonical, maxPrice = existingPrice),
                            nextContext = ConversationContext(
                                lastSearchDistrict = canonical,
                                lastSearchMaxPrice = existingPrice
                            )
                        )
                    } else {
                        val nextCtx = ConversationContext(
                            state = ConversationState.AWAIT_PRICE,
                            district = canonical,
                            lastSearchDistrict = context.lastSearchDistrict,
                            lastSearchMaxPrice = context.lastSearchMaxPrice
                        )
                        val districtLabel = if (canonical != null) "khu vực **$canonical**" else "bất kỳ khu vực nào"
                        ChatbotResponse(
                            answer = "Bạn muốn thuê phòng ở $districtLabel.\n\nGiá thuê tối đa bạn mong muốn là bao nhiêu?\n_(Ví dụ: 3 triệu, 2.5tr, 1500k... hoặc nhập \"tùy\" nếu không có yêu cầu)_",
                            confidence = 1.0,
                            quickReplies = listOf("2 triệu", "3 triệu", "5 triệu", "Tùy giá"),
                            nextContext = nextCtx
                        )
                    }
                } else {
                    ChatbotResponse(
                        answer = "Mình chưa nhận ra tên khu vực đó.\n\nBạn nhập tên quận/huyện rõ hơn được không?\n_(Ví dụ: Quận 1, Bình Thạnh, Gò Vấp, Thủ Đức...)_",
                        confidence = 1.0,
                        quickReplies = listOf("Quận 1", "Bình Thạnh", "Gò Vấp", "Thủ Đức"),
                        nextContext = context
                    )
                }
            }

            ConversationState.AWAIT_PRICE -> {
                val price = extractPrice(sanitized)
                return if (price != null) {
                    val maxPrice = if (price == -1L) null else price
                    val priceLabel = if (maxPrice != null) formatMaxPrice(maxPrice) else "không giới hạn"
                    val districtLabel = if (context.district != null) "khu **${context.district}**" else "tất cả khu vực"
                    ChatbotResponse(
                        answer = "Đang tìm phòng ở $districtLabel, giá $priceLabel...",
                        confidence = 1.0,
                        searchParams = SearchParams(district = context.district, maxPrice = maxPrice),
                        nextContext = ConversationContext(
                            lastSearchDistrict = context.district,
                            lastSearchMaxPrice = maxPrice
                        )
                    )
                } else {
                    ChatbotResponse(
                        answer = "Mình chưa hiểu mức giá bạn nhập.\n\nBạn nhập theo dạng:\n_(Ví dụ: 3 triệu, 2.5tr, 1500k... hoặc \"tùy\" nếu không có yêu cầu giá)_",
                        confidence = 1.0,
                        quickReplies = listOf("2 triệu", "3 triệu", "5 triệu", "Tùy giá"),
                        nextContext = context
                    )
                }
            }

            ConversationState.IDLE, ConversationState.READY_TO_SEARCH -> { /* fall through */ }
        }

        // Intercept search intent before TF-IDF
        if (isSearchIntent(sanitized)) {
            val district = extractDistrict(sanitized)
            val price = extractPrice(sanitized)

            return when {
                district != null && price != null -> {
                    val canonical = district.ifEmpty { null }
                    val maxPrice = if (price == -1L) null else price
                    val districtLabel = if (canonical != null) "khu **$canonical**" else "tất cả khu vực"
                    val priceLabel = if (maxPrice != null) formatMaxPrice(maxPrice) else "không giới hạn"
                    ChatbotResponse(
                        answer = "Đang tìm phòng ở $districtLabel, giá $priceLabel...",
                        confidence = 1.0,
                        searchParams = SearchParams(district = canonical, maxPrice = maxPrice),
                        nextContext = ConversationContext(
                            lastSearchDistrict = canonical,
                            lastSearchMaxPrice = maxPrice
                        )
                    )
                }
                district != null -> {
                    val canonical = district.ifEmpty { null }
                    val nextCtx = ConversationContext(
                        state = ConversationState.AWAIT_PRICE,
                        district = canonical,
                        lastSearchDistrict = context.lastSearchDistrict,
                        lastSearchMaxPrice = context.lastSearchMaxPrice
                    )
                    val districtLabel = if (canonical != null) "khu vực **$canonical**" else "bất kỳ khu vực nào"
                    ChatbotResponse(
                        answer = "Bạn muốn thuê phòng ở $districtLabel.\n\nGiá thuê tối đa bạn mong muốn là bao nhiêu?\n_(Ví dụ: 3 triệu, 2.5tr, 1500k... hoặc nhập \"tùy\" nếu không có yêu cầu)_",
                        confidence = 1.0,
                        quickReplies = listOf("2 triệu", "3 triệu", "5 triệu", "Tùy giá"),
                        nextContext = nextCtx
                    )
                }
                else -> {
                    ChatbotResponse(
                        answer = "Mình sẽ giúp bạn tìm phòng trọ phù hợp!\n\nBạn muốn tìm phòng ở khu vực nào?\n_(Ví dụ: Quận 1, Bình Thạnh, Gò Vấp, Thủ Đức...)_",
                        confidence = 1.0,
                        quickReplies = listOf("Quận 1", "Bình Thạnh", "Gò Vấp", "Thủ Đức"),
                        nextContext = ConversationContext(
                            state = ConversationState.AWAIT_DISTRICT,
                            lastSearchDistrict = context.lastSearchDistrict,
                            lastSearchMaxPrice = context.lastSearchMaxPrice
                        )
                    )
                }
            }
        }

        // Standard TF-IDF matching
        val inputVector = computeTfIdf(sanitized)
        if (inputVector.isEmpty()) {
            return ChatbotResponse(FALLBACK_RESPONSE, 0.0, quickReplies = FALLBACK_QUICK_REPLIES)
        }

        val wordCount = tokenize(sanitized).count { !it.contains(' ') }
        val threshold = dynamicThreshold(wordCount)

        val intentScores = mutableMapOf<String, Double>()
        for ((intentId, patternVector) in patternVectors) {
            val similarity = cosineSimilarity(inputVector, patternVector)
            val currentBest = intentScores[intentId] ?: 0.0
            if (similarity > currentBest) intentScores[intentId] = similarity
        }

        val bestMatch = intentScores.maxByOrNull { it.value }
        if (bestMatch == null || bestMatch.value < threshold) {
            return ChatbotResponse(FALLBACK_RESPONSE, 0.0, quickReplies = FALLBACK_QUICK_REPLIES)
        }

        val matched = intents.find { it.id == bestMatch.key }
            ?: return ChatbotResponse(FALLBACK_RESPONSE, 0.0, quickReplies = FALLBACK_QUICK_REPLIES)

        // Redirect search_room FAQ intent to interactive search flow
        if (matched.id == "search_room") {
            val district = extractDistrict(sanitized)
            val price = extractPrice(sanitized)
            return when {
                district != null && price != null -> {
                    val canonical = district.ifEmpty { null }
                    val maxPrice = if (price == -1L) null else price
                    val districtLabel = if (canonical != null) "khu **$canonical**" else "tất cả khu vực"
                    val priceLabel = if (maxPrice != null) formatMaxPrice(maxPrice) else "không giới hạn"
                    ChatbotResponse(
                        answer = "Đang tìm phòng ở $districtLabel, giá $priceLabel...",
                        confidence = 1.0,
                        searchParams = SearchParams(district = canonical, maxPrice = maxPrice),
                        nextContext = ConversationContext(
                            lastSearchDistrict = canonical,
                            lastSearchMaxPrice = maxPrice
                        )
                    )
                }
                district != null -> {
                    val canonical = district.ifEmpty { null }
                    val nextCtx = ConversationContext(
                        state = ConversationState.AWAIT_PRICE,
                        district = canonical,
                        lastSearchDistrict = context.lastSearchDistrict,
                        lastSearchMaxPrice = context.lastSearchMaxPrice
                    )
                    val districtLabel = if (canonical != null) "khu vực **$canonical**" else "bất kỳ khu vực nào"
                    ChatbotResponse(
                        answer = "Bạn muốn thuê phòng ở $districtLabel.\n\nGiá thuê tối đa bạn mong muốn là bao nhiêu?\n_(Ví dụ: 3 triệu, 2.5tr, 1500k... hoặc nhập \"tùy\" nếu không có yêu cầu)_",
                        confidence = 1.0,
                        quickReplies = listOf("2 triệu", "3 triệu", "5 triệu", "Tùy giá"),
                        nextContext = nextCtx
                    )
                }
                else -> ChatbotResponse(
                    answer = "Mình sẽ giúp bạn tìm phòng trọ phù hợp!\n\nBạn muốn tìm phòng ở khu vực nào?\n_(Ví dụ: Quận 1, Bình Thạnh, Gò Vấp, Thủ Đức...)_",
                    confidence = 1.0,
                    quickReplies = listOf("Quận 1", "Bình Thạnh", "Gò Vấp", "Thủ Đức"),
                    nextContext = ConversationContext(
                        state = ConversationState.AWAIT_DISTRICT,
                        lastSearchDistrict = context.lastSearchDistrict,
                        lastSearchMaxPrice = context.lastSearchMaxPrice
                    )
                )
            }
        }

        return ChatbotResponse(matched.response, bestMatch.value, matched.deepLink)
    }

    private fun loadIntents() {
        try {
            val json = context.assets.open("chatbot_data.json").bufferedReader().use { it.readText() }
            val root = JSONObject(json)
            val intentsArray: JSONArray = root.getJSONArray("intents")

            for (i in 0 until intentsArray.length()) {
                val obj = intentsArray.getJSONObject(i)
                val id = obj.getString("id")
                val response = obj.getString("response")
                val deepLink = if (obj.has("deepLink") && !obj.isNull("deepLink"))
                    obj.getString("deepLink") else null

                val patternsArray = obj.getJSONArray("patterns")
                val patterns = (0 until patternsArray.length()).map { j -> patternsArray.getString(j) }

                intents.add(Intent(id, patterns, response, deepLink))
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatbotEngine", "Failed to load chatbot_data.json", e)
        }
    }
}
