package com.example.doantotnghiep.ViewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.doantotnghiep.AI.ChatResult
import com.example.doantotnghiep.AI.GeminiService
import com.example.doantotnghiep.AI.GeocodingHelper
import com.example.doantotnghiep.AI.SearchParams
import com.example.doantotnghiep.Model.AIMessage
import com.example.doantotnghiep.Model.AIRoom
import com.example.doantotnghiep.repository.AIChatRepository
import com.example.doantotnghiep.repository.RoomRepository
import com.example.doantotnghiep.usecase.GeoSearchRoomsUseCase
import com.example.doantotnghiep.usecase.SearchRoomsUseCase
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.Locale

class AIChatViewModel(application: Application) : AndroidViewModel(application) {

    private val aiChatRepository = AIChatRepository()
    private val roomRepository = RoomRepository()
    private val geminiService = GeminiService()
    private val geocodingHelper = GeocodingHelper(application)
    private val searchRoomsUseCase = SearchRoomsUseCase()
    private val geoSearchRoomsUseCase = GeoSearchRoomsUseCase()

    private val _messages = MutableLiveData<List<AIMessage>>(emptyList())
    val messages: LiveData<List<AIMessage>> = _messages

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isProcessing = MutableLiveData(false)
    val isProcessing: LiveData<Boolean> = _isProcessing

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error



    private val displayMessages = mutableListOf<AIMessage>()

    // Cache tọa độ geocoded — tránh geocode lại cùng địa chỉ → kết quả khác nhau mỗi lần
    private var cachedGeoAddress: String? = null
    private var cachedGeoLat: Double? = null
    private var cachedGeoLng: Double? = null

    // Cache danh sách phòng — tránh fetch 200 docs Firestore mỗi lần tìm kiếm (TTL 5 phút)
    private var cachedRoomDocs: List<DocumentSnapshot>? = null
    private var cacheTimestampMs: Long = 0L
    private val CACHE_TTL_MS = 5 * 60 * 1000L

    // --- Chat history ---
    private var cachedUserName: String = ""

    fun loadHistory(uid: String, userName: String = "") {       // Fix #9: loadContext() removed
        cachedUserName = userName
        _isLoading.value = true
        aiChatRepository.loadConversationHistory(uid,
            onSuccess = { docs ->
                displayMessages.clear()
                if (docs.isNotEmpty()) {
                    for (doc in docs) {
                        val role = doc.getString("role") ?: ""
                        val content = doc.getString("content") ?: ""
                        val timestamp = doc.getDate("timestamp")?.time ?: System.currentTimeMillis()
                        val rooms = parseRooms(doc.get("suggestedRooms"))
                        displayMessages.add(AIMessage(role, content, timestamp, rooms))
                    }
                } else {
                    addGreeting(userName)
                }
                _messages.value = displayMessages.toList()
                geminiService.restoreHistory(displayMessages.toList())
                _isLoading.value = false
            },
            onFailure = { msg ->
                _isLoading.value = false
                _error.value = msg
            }
        )
    }

    // Send message

    fun sendMessage(text: String, uid: String) {
        if (_isProcessing.value == true) return

        val now = System.currentTimeMillis()
        displayMessages.add(AIMessage("user", text, now))
        displayMessages.add(AIMessage("typing", "...", now + 1))
        _messages.value = displayMessages.toList()
        _isProcessing.value = true

        aiChatRepository.saveConversationMessage(uid,
            mapOf("role" to "user", "content" to text, "timestamp" to Date(now)))

        viewModelScope.launch(Dispatchers.IO) {
            val result = geminiService.processUserMessage(text)
            withContext(Dispatchers.Main) {
                removeTypingIndicator()
                _isProcessing.value = false
                when (result) {
                    is ChatResult.Reply -> {
                        val replyTime = System.currentTimeMillis()
                        displayMessages.add(AIMessage("model", result.text, replyTime))
                        _messages.value = displayMessages.toList()
                        aiChatRepository.saveConversationMessage(uid,
                            mapOf("role" to "model", "content" to result.text,
                                "timestamp" to Date(replyTime)))
                    }
                    is ChatResult.RoomSearch -> searchRooms(result.params, uid)
                    is ChatResult.GeoSearch  -> searchRooms(result.params, uid)
                    is ChatResult.Error -> {
                        val replyTime = System.currentTimeMillis()
                        displayMessages.add(AIMessage("model", result.message, replyTime))
                        _messages.value = displayMessages.toList()
                    }
                }
            }
        }
    }

    // --- Room search ---

    private fun fetchRoomsOrCache(
        onSuccess: (List<DocumentSnapshot>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val cached = cachedRoomDocs
        if (cached != null && System.currentTimeMillis() - cacheTimestampMs < CACHE_TTL_MS) {
            onSuccess(cached)
            return
        }
        aiChatRepository.fetchApprovedRooms(
            onSuccess = { docs ->
                cachedRoomDocs = docs
                cacheTimestampMs = System.currentTimeMillis()
                onSuccess(docs)
            },
            onFailure = onFailure
        )
    }

    private fun searchRooms(params: SearchParams, uid: String) {
        if (params.addressQuery != null && params.radiusKm != null) {
            displayMessages.add(AIMessage("typing", "...", System.currentTimeMillis()))
            _messages.value = displayMessages.toList()

            viewModelScope.launch {
                val latLng: Pair<Double, Double>? =
                    if (params.addressQuery == cachedGeoAddress &&
                        cachedGeoLat != null && cachedGeoLng != null) {
                        Pair(cachedGeoLat!!, cachedGeoLng!!)
                    } else {
                        geocodingHelper.geocode(params.addressQuery)
                    }

                if (latLng == null) {
                    removeTypingIndicator()
                    val t = System.currentTimeMillis()
                    val msg = "Không tìm được vị trí cho địa chỉ bạn nhập.\n" +
                        "Vui lòng kiểm tra lại hoặc thử nhập thêm tên phường/xã " +
                        "(ví dụ: \"ngõ 6 Cầu Giấy\")."
                    displayMessages.add(AIMessage("model", msg, t,
                        quickReplies = listOf("Tìm lại", "Đổi khu vực")))
                    _messages.value = displayMessages.toList()
                } else {
                    cachedGeoAddress = params.addressQuery
                    cachedGeoLat = latLng.first
                    cachedGeoLng = latLng.second
                    searchRoomsByGeo(latLng.first, latLng.second, params.radiusKm, params, uid)
                }
            }
            return
        }

        displayMessages.add(AIMessage("typing", "...", System.currentTimeMillis()))
        _messages.value = displayMessages.toList()

        fetchRoomsOrCache(
            onSuccess = { docs ->
                removeTypingIndicator()
                val result = searchRoomsUseCase(docs, params)
                val allScoredSorted = result.scored
                val maxScore = result.maxScore

                val primary: List<AIRoom>
                val related: List<AIRoom>
                if (maxScore == 0) {
                    primary = allScoredSorted.map { it.room }.take(10)
                    related = emptyList()
                } else {
                    val primaryMin = (maxScore * 0.75).toInt().coerceAtLeast(1)
                    val relatedMin = (maxScore * 0.35).toInt().coerceAtLeast(1)
                    primary = allScoredSorted.filter { it.score >= primaryMin }.map { it.room }.take(5)
                    related = allScoredSorted
                        .filter { it.score in relatedMin until primaryMin }
                        .map { it.room }.take(3)
                }

                val districtStr = when {
                    params.ward != null && params.district != null ->
                        " ở **${params.ward}**, **${params.district}**"
                    params.district != null -> " ở **${params.district}**"
                    else -> ""
                }
                val priceStr = buildPriceStr(params.minPrice, params.maxPrice)
                val quickReplies = listOf("Tìm lại", "Đổi khu vực", "Đổi mức giá")

                if (primary.isEmpty() && related.isEmpty()) {
                    val t = System.currentTimeMillis()
                    val nearbyChips = params.district?.let { NEARBY_DISTRICTS[it] } ?: emptyList()
                    val noResultMsg = if (nearbyChips.isNotEmpty()) {
                        randomNoResults(districtStr, priceStr) + "\nBạn có muốn thử tìm ở khu lân cận không?"
                    } else {
                        randomNoResults(districtStr, priceStr)
                    }
                    val noResultReplies = if (nearbyChips.isNotEmpty()) nearbyChips + "Đổi mức giá"
                                         else quickReplies
                    postAndSave(uid, noResultMsg, t, emptyList(), noResultReplies)
                } else {
                    val t1 = System.currentTimeMillis()
                    val primaryMsg = if (primary.isEmpty()) {
                        randomNoResults(districtStr, priceStr)
                    } else {
                        randomFoundResults(primary.size, districtStr, priceStr)
                    }
                    postAndSave(uid, primaryMsg, t1, primary,
                        if (related.isEmpty()) quickReplies else emptyList())

                    if (related.isNotEmpty()) {
                        val t2 = t1 + 1
                        postAndSave(uid, randomRelatedResults(related.size), t2, related, quickReplies)
                    }
                }
            },
            onFailure = { errMsg ->
                removeTypingIndicator()
                val t = System.currentTimeMillis()
                displayMessages.add(AIMessage("model",
                    "Có lỗi khi tìm kiếm phòng. Vui lòng thử lại sau.",
                    t, quickReplies = listOf("Tìm lại")))
                _messages.value = displayMessages.toList()
                Log.e("AIChatViewModel", "Room search failed: $errMsg")
            }
        )
    }

    private fun postAndSave(
        uid: String,
        content: String,
        timestamp: Long,
        rooms: List<AIRoom>,
        quickReplies: List<String>
    ) {
        displayMessages.add(AIMessage("model", content, timestamp, rooms, quickReplies))
        _messages.value = displayMessages.toList()
        val roomsData = rooms.map {
            mapOf("id" to it.id, "userId" to it.userId, "title" to it.title,
                  "price" to it.price, "area" to it.area,
                  "district" to it.district, "imageUrl" to it.imageUrl)
        }
        aiChatRepository.saveConversationMessage(uid,
            hashMapOf("role" to "model", "content" to content,
                "timestamp" to Date(timestamp), "suggestedRooms" to roomsData))
    }

    // --- Delete history ---

    fun deleteHistory(uid: String) {
        _isLoading.value = true
        aiChatRepository.deleteConversationHistory(uid,
            onSuccess = { onDeleteComplete(uid) },
            onFailure = { msg ->
                _isLoading.value = false
                _error.value = msg
            }
        )
    }

    private fun onDeleteComplete(uid: String) {
        aiChatRepository.deleteContext(uid)
        displayMessages.clear()
        cachedRoomDocs = null
        cacheTimestampMs = 0L
        geminiService.clearHistory()
        addGreeting(cachedUserName)
        _messages.value = displayMessages.toList()
        _isLoading.value = false
    }

    // --- Helpers ---

    fun resolveQuickReplyAction(displayText: String): String =
        QUICK_REPLY_ACTIONS[displayText] ?: displayText

    /**
     * Cập nhật lại lời chào với tên đầy đủ.
     * - Nếu tin nhắn đầu tiên là lời chào: cập nhật nội dung.
     * - Nếu có lịch sử cũ (không có lời chào đầu): chèn thêm lời chào vào đầu.
     */
    fun updateGreetingName(fullName: String) {
        cachedUserName = fullName
        val greetingText = if (fullName.isNotBlank()) {
            "Xin chào $fullName! Mình là Trợ lý hỗ trợ tìm kiếm phòng trọ từ Tìm trọ 24/7.\nBạn cần tìm phòng khu vực nào hay cần mình tư vấn điều gì không?"
        } else {
            "Xin chào! Mình là Trợ lý hỗ trợ tìm kiếm phòng trọ từ Tìm trọ 24/7.\nBạn cần tìm phòng khu vực nào hay cần mình tư vấn điều gì không?"
        }

        if (displayMessages.isEmpty()) return

        val firstMsg = displayMessages[0]
        if (firstMsg.role == "model" && firstMsg.content.startsWith("Xin chào")) {
            // Cập nhật lời chào hiện có
            displayMessages[0] = firstMsg.copy(content = greetingText)
        } else {
            // Có lịch sử cũ nhưng không có lời chào → chèn vào đầu danh sách
            displayMessages.add(0, AIMessage(
                role = "model",
                content = greetingText,
                timestamp = (displayMessages.firstOrNull()?.timestamp ?: System.currentTimeMillis()) - 1
            ))
        }
        _messages.value = displayMessages.toList()
    }

    private fun addGreeting(userName: String = "") {
        val greeting = if (userName.isNotBlank()) {
            "Xin chào $userName! Mình là Trợ lý hỗ trợ tìm kiếm phòng trọ từ Tìm trọ 24/7.\nBạn cần tìm phòng khu vực nào hay cần mình tư vấn điều gì không?"
        } else {
            "Xin chào! Mình là Trợ lý hỗ trợ tìm kiếm phòng trọ từ Tìm trọ 24/7.\nBạn cần tìm phòng khu vực nào hay cần mình tư vấn điều gì không?"
        }
        displayMessages.add(AIMessage(
            role = "model",
            content = greeting,
            timestamp = System.currentTimeMillis()
        ))
    }

    private fun removeTypingIndicator() {
        val idx = displayMessages.indexOfLast { it.role == "typing" }
        if (idx != -1) displayMessages.removeAt(idx)
    }

    private fun parseRooms(raw: Any?): List<AIRoom> {
        @Suppress("UNCHECKED_CAST")
        val list = raw as? List<Map<String, Any>> ?: return emptyList()
        return list.map { m ->
            AIRoom(
                id = m["id"] as? String ?: "",
                userId = m["userId"] as? String ?: "",
                title = m["title"] as? String ?: "",
                price = (m["price"] as? Number)?.toLong() ?: 0L,
                area = (m["area"] as? Number)?.toInt() ?: 0,
                district = m["district"] as? String ?: "",
                imageUrl = m["imageUrl"] as? String ?: ""
            )
        }
    }

    private fun formatPrice(price: Long): String {
        val millions = price / 1_000_000.0
        return if (millions >= 1.0) {
            val s = if (millions == millions.toLong().toDouble()) millions.toLong().toString()
                    else String.format(Locale.US, "%.1f", millions)  // Fix #11
            "${s}tr"
        } else "${price / 1000}K"
    }

    private fun buildPriceStr(minPrice: Long?, maxPrice: Long?): String = when {
        minPrice != null && maxPrice != null -> ", giá ${formatPrice(minPrice)}–${formatPrice(maxPrice)}"
        maxPrice != null -> ", giá tối đa ${formatPrice(maxPrice)}"
        minPrice != null -> ", giá trên ${formatPrice(minPrice)}"
        else -> ""
    }

    private fun formatRadius(radiusKm: Double): String {
        return if (radiusKm < 1.0) "${(radiusKm * 1000).toInt()}m" else {
            val s = if (radiusKm == radiusKm.toLong().toDouble()) radiusKm.toLong().toString()
                    else String.format(Locale.US, "%.1f", radiusKm)  // Fix #11
            "${s}km"
        }
    }

    private fun randomNoResults(districtStr: String, priceStr: String): String =
        listOf(
            "Rất tiếc, hiện không có phòng nào phù hợp$districtStr$priceStr. Bạn thử mở rộng tiêu chí nhé!",
            "Mình chưa tìm được phòng nào$districtStr$priceStr. Bạn có muốn thay đổi mức giá hoặc khu vực không?"
        ).random()

    private fun randomFoundResults(count: Int, districtStr: String, priceStr: String): String =
        listOf(
            "Mình tìm được **$count phòng** phù hợp$districtStr$priceStr! ",
            "Có **$count phòng** đang trống$districtStr$priceStr, bạn xem thử nhé! "
        ).random()

    private fun randomRelatedResults(count: Int): String =
        listOf(
            "Ngoài ra có **$count phòng** tương tự có thể phù hợp:",
            "Thêm **$count phòng** gần đúng yêu cầu:"
        ).random()

    private fun randomFoundGeoResults(count: Int, radius: String, priceStr: String): String =
        "Tìm được **$count phòng** trong bán kính **$radius**$priceStr! Bạn xem thử nhé"

    private fun randomNoGeoResults(radius: String, priceStr: String): String =
        "Không tìm thấy phòng nào trong bán kính **$radius**$priceStr. Bạn thử mở rộng bán kính nhé!"

    private fun randomNoAmenityMatch(amenities: String, radius: String, priceStr: String): String =
        "Không có phòng nào đủ tiện ích **$amenities** trong bán kính **$radius**$priceStr.\n" +
        "Mình gợi ý phòng không yêu cầu tiện ích này nhé:"

    // --- Tìm phòng theo bán kính địa lý ---
    private fun searchRoomsByGeo(
        lat: Double, lng: Double,
        radiusKm: Double,
        params: SearchParams,
        uid: String
    ) {
        val radiusLabel = formatRadius(radiusKm)
        roomRepository.searchNearbyRooms(
            lat = lat,
            radiusKm = radiusKm,
            onSuccess = { docs ->
                removeTypingIndicator()

                val geoResult = geoSearchRoomsUseCase(docs, lat, lng, radiusKm, params)
                val priceStr = buildPriceStr(params.minPrice, params.maxPrice)
                val quickReplies = listOf("Tìm lại", "Đổi bán kính", "Đổi mức giá")
                val t = System.currentTimeMillis()

                when {
                    geoResult.rooms.isNotEmpty() -> {
                        postAndSave(uid, randomFoundGeoResults(geoResult.rooms.size, radiusLabel, priceStr),
                            t, geoResult.rooms, quickReplies)
                    }
                    geoResult.relaxedRooms.isNotEmpty() -> {
                        val amenityNames = params.amenities.mapNotNull { AMENITY_DISPLAY[it] }.joinToString(", ")
                        postAndSave(uid, randomNoAmenityMatch(amenityNames, radiusLabel, priceStr),
                            t, geoResult.relaxedRooms, quickReplies)
                    }
                    else -> {
                        postAndSave(uid, randomNoGeoResults(radiusLabel, priceStr),
                            t, emptyList(), quickReplies)
                    }
                }
            },
            onFailure = { errMsg ->
                removeTypingIndicator()
                val t = System.currentTimeMillis()
                displayMessages.add(AIMessage("model",
                    "Có lỗi khi tìm kiếm quanh địa chỉ: $errMsg\nVui lòng thử lại sau.",
                    t, quickReplies = listOf("Tìm lại")))
                _messages.value = displayMessages.toList()
                Log.e("AIChatViewModel", "Geo search failed: $errMsg")
            }
        )
    }

    companion object {
        private val QUICK_REPLY_ACTIONS = mapOf(
            "Tìm lại" to "Tìm lại những phòng như vậy cho mình",
            "Đổi khu vực" to "Tôi muốn tìm phòng ở khu vực khác",
            "Đổi mức giá" to "Tôi muốn thay đổi mức giá",
            "Đổi bán kính" to "Tôi muốn mở rộng bán kính tìm kiếm"
        )

        private val AMENITY_DISPLAY = mapOf(
            "airConditioner" to "điều hòa",
            "washer" to "máy giặt",
            "wifi" to "wifi",
            "parking" to "chỗ để ô tô",
            "motorbike" to "chỗ để xe máy",
            "ebike" to "chỗ để xe đạp điện",
            "bicycle" to "chỗ để xe đạp",
            "kitchen" to "bếp riêng",
            "balcony" to "ban công/sân phơi",
            "furniture" to "nội thất đầy đủ",
            "refrigerator" to "tủ lạnh",
            "tv" to "tivi",
            "security" to "bảo vệ",
            "waterHeater" to "bình nóng lạnh",
            "privateWC" to "WC riêng",
            "bed" to "giường",
            "wardrobe" to "tủ quần áo"
        )

        private val NEARBY_DISTRICTS = mapOf(
            "Cầu Giấy"     to listOf("Đống Đa", "Từ Liêm", "Tây Hồ"),
            "Đống Đa"      to listOf("Thanh Xuân", "Cầu Giấy", "Hai Bà Trưng"),
            "Thanh Xuân"   to listOf("Đống Đa", "Hoàng Mai", "Từ Liêm"),
            "Hoàng Mai"    to listOf("Thanh Xuân", "Hai Bà Trưng", "Long Biên"),
            "Hai Bà Trưng" to listOf("Đống Đa", "Hoàng Mai", "Hoàn Kiếm"),
            "Hoàn Kiếm"   to listOf("Ba Đình", "Hai Bà Trưng", "Đống Đa"),
            "Ba Đình"      to listOf("Hoàn Kiếm", "Tây Hồ", "Cầu Giấy"),
            "Tây Hồ"       to listOf("Ba Đình", "Cầu Giấy", "Từ Liêm"),
            "Long Biên"    to listOf("Gia Lâm", "Hoàng Mai", "Hai Bà Trưng"),
            "Hà Đông"      to listOf("Thanh Xuân", "Từ Liêm", "Hoàng Mai"),
            "Từ Liêm"      to listOf("Cầu Giấy", "Hà Đông", "Tây Hồ")
        )
    }
}
