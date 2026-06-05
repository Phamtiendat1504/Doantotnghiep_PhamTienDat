package com.example.doantotnghiep.ViewModel

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.doantotnghiep.AI.ChatbotEngine
import com.example.doantotnghiep.Model.AIMessage
import com.example.doantotnghiep.Model.AIRoom
import com.example.doantotnghiep.Utils.GeoUtils
import com.example.doantotnghiep.Utils.LocationNormalizer
import com.example.doantotnghiep.repository.RoomRepository
import com.example.doantotnghiep.repository.AIChatRepository
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date
import java.util.concurrent.TimeUnit

class AIChatViewModel(application: Application) : AndroidViewModel(application) {

    private val aiChatRepository = AIChatRepository()
    private val roomRepository = RoomRepository()
    private val chatbotEngine = ChatbotEngine(application).also { it.initialize() }

    // OkHttpClient dùng riêng cho geocoding (timeout ngắn)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val _messages = MutableLiveData<List<AIMessage>>(emptyList())
    val messages: LiveData<List<AIMessage>> = _messages

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Fix 10: isProcessing prevents race condition from rapid sends
    private val _isProcessing = MutableLiveData(false)
    val isProcessing: LiveData<Boolean> = _isProcessing

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _deepLink = MutableLiveData<String?>()
    val deepLink: LiveData<String?> = _deepLink

    private val displayMessages = mutableListOf<AIMessage>()
    var currentContext = ChatbotEngine.ConversationContext()
        private set

    // Cache tọa độ geocoded — tránh geocode lại cùng địa chỉ → kết quả khác nhau mỗi lần
    private var cachedGeoAddress: String? = null
    private var cachedGeoLat: Double? = null
    private var cachedGeoLng: Double? = null

    // --- Context persistence ---

    fun loadContext(uid: String, onComplete: () -> Unit) {
        aiChatRepository.loadContextData(uid) { data ->
            if (data.isNotEmpty()) {
                @Suppress("UNCHECKED_CAST")
                val lastSearchAmenities = (data["lastSearchAmenities"] as? List<String>) ?: emptyList()
                currentContext = ChatbotEngine.ConversationContext(
                    lastSearchDistrict = data["lastSearchDistrict"] as? String,
                    lastSearchMaxPrice = data["lastSearchMaxPrice"] as? Long,
                    lastSearchMinPrice = data["lastSearchMinPrice"] as? Long,
                    lastSearchWard = data["lastSearchWard"] as? String,
                    lastUserQuery = data["lastUserQuery"] as? String,
                    lastResultCount = (data["lastResultCount"] as? Long ?: -1L).toInt(),
                    lastSearchAmenities = lastSearchAmenities,
                    lastSearchMinArea = (data["lastSearchMinArea"] as? Long)?.toInt(),
                    lastSearchMaxArea = (data["lastSearchMaxArea"] as? Long)?.toInt(),
                    lastSearchRoomType = data["lastSearchRoomType"] as? String,
                    lastSearchGenderPrefer = data["lastSearchGenderPrefer"] as? String,
                    lastSearchMaxElectricPrice = data["lastSearchMaxElectricPrice"] as? Long,
                    lastSearchMaxWaterPrice = data["lastSearchMaxWaterPrice"] as? Long,
                    lastSearchMaxWifiPrice = data["lastSearchMaxWifiPrice"] as? Long,
                    lastSearchMaxDepositMonths = (data["lastSearchMaxDepositMonths"] as? Long)?.toInt(),
                    lastSearchPetAllowed = data["lastSearchPetAllowed"] as? Boolean,
                    lastSearchFreeTime = data["lastSearchFreeTime"] as? Boolean
                )
            }
            onComplete()
        }
    }

    fun saveContext(uid: String) {
        val data = hashMapOf<String, Any?>(
            "lastSearchDistrict" to currentContext.lastSearchDistrict,
            "lastSearchMaxPrice" to currentContext.lastSearchMaxPrice,
            "lastSearchMinPrice" to currentContext.lastSearchMinPrice,
            "lastSearchWard" to currentContext.lastSearchWard,
            "lastUserQuery" to currentContext.lastUserQuery,
            "lastResultCount" to currentContext.lastResultCount,
            "lastSearchAmenities" to currentContext.lastSearchAmenities,
            "lastSearchMinArea" to currentContext.lastSearchMinArea,
            "lastSearchMaxArea" to currentContext.lastSearchMaxArea,
            "lastSearchRoomType" to currentContext.lastSearchRoomType,
            "lastSearchGenderPrefer" to currentContext.lastSearchGenderPrefer,
            "lastSearchMaxElectricPrice" to currentContext.lastSearchMaxElectricPrice,
            "lastSearchMaxWaterPrice" to currentContext.lastSearchMaxWaterPrice,
            "lastSearchMaxWifiPrice" to currentContext.lastSearchMaxWifiPrice,
            "lastSearchMaxDepositMonths" to currentContext.lastSearchMaxDepositMonths,
            "lastSearchPetAllowed" to currentContext.lastSearchPetAllowed,
            "lastSearchFreeTime" to currentContext.lastSearchFreeTime
        )
        aiChatRepository.saveContextData(uid, data)
    }

    // --- Chat history ---

    fun loadHistory(uid: String) {
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
                    addGreeting()
                }
                _messages.value = displayMessages.toList()
                _isLoading.value = false
            },
            onFailure = { msg ->
                _isLoading.value = false
                _error.value = msg
            }
        )
    }

    // --- Send message ---

    fun sendMessage(text: String, uid: String) {
        // Fix 10: guard against concurrent sends
        if (_isProcessing.value == true) return

        val now = System.currentTimeMillis()
        displayMessages.add(AIMessage("user", text, now))
        displayMessages.add(AIMessage("typing", "...", now + 1))
        _messages.value = displayMessages.toList()
        _isProcessing.value = true

        aiChatRepository.saveConversationMessage(uid,
            mapOf("role" to "user", "content" to text, "timestamp" to Date(now)))

        Handler(Looper.getMainLooper()).postDelayed({
            removeTypingIndicator()

            val response = chatbotEngine.processMessage(text, currentContext)
            currentContext = response.nextContext
            saveContext(uid)

            val replyTime = System.currentTimeMillis()
            displayMessages.add(AIMessage("model", response.answer, replyTime,
                quickReplies = response.quickReplies))
            _messages.value = displayMessages.toList()
            _isProcessing.value = false

            if (!response.deepLink.isNullOrEmpty()) {
                _deepLink.value = response.deepLink
            }

            if (response.searchParams != null) {
                searchRooms(response.searchParams, uid)
            } else {
                aiChatRepository.saveConversationMessage(uid,
                    mapOf("role" to "model", "content" to response.answer,
                        "timestamp" to Date(replyTime)))
            }
        }, 800)
    }

    // --- Room search (scored + two-tier results) ---

    // Maximum score achievable given the search params (district filtered server-side).
    private fun computeMaxScore(params: ChatbotEngine.SearchParams): Int {
        var max = 0
        if (params.ward != null)      max += 3
        if (params.maxPrice != null)  max += 2
        if (params.minPrice != null)  max += 1
        if (params.minArea != null || params.maxArea != null) max += 1
        if (params.roomType != null)      max += 1
        if (params.genderPrefer != null)  max += 1
        max += params.amenities.size          // 1 point each
        return max
    }

    // Score a single room against the search params.
    // District is already guaranteed by the Firestore query — not re-scored here.
    // Returns -1 if the room should be excluded entirely (hard-excluded by any filter).
    private fun scoreRoom(
        doc: com.google.firebase.firestore.DocumentSnapshot,
        params: ChatbotEngine.SearchParams
    ): Int {
        val docPrice = doc.getLong("price") ?: 0L
        val docAddress = (doc.getString("address") ?: "").lowercase()

        // 1. Hard-exclude only if both min AND max price given and room is outside range
        if (params.minPrice != null && params.maxPrice != null &&
            (docPrice < params.minPrice || docPrice > params.maxPrice)) return -1

        // 2. Tiện ích (các trường boolean trong Firestore)
        val docAmenities = mutableListOf<String>()
        // --- Nhóm bool cơ bản ---
        if (doc.getBoolean("hasWifi") == true)         docAmenities.add("wifi")
        if (doc.getBoolean("hasAirCon") == true)       docAmenities.add("airConditioner")
        if (doc.getBoolean("hasWasher") == true)       docAmenities.add("washer")
        if (doc.getBoolean("hasParking") == true)      docAmenities.add("parking")
        if (doc.getBoolean("hasWaterHeater") == true)  docAmenities.add("waterHeater")
        if (doc.getBoolean("hasWardrobe") == true)     docAmenities.add("wardrobe")
        if (doc.getBoolean("hasBed") == true)          docAmenities.add("bed")
        if (doc.getBoolean("hasDryingArea") == true)   docAmenities.add("balcony")
        // --- Nhóm field String (kitchen/bathroom) → so sánh giá trị "Riêng" ---
        val kitchenVal = (doc.getString("kitchen") ?: "").lowercase()
        if (kitchenVal.contains("riêng") || kitchenVal.contains("co")) docAmenities.add("kitchen")
        val bathroomVal = (doc.getString("bathroom") ?: "").lowercase()
        if (bathroomVal.contains("riêng") || bathroomVal.contains("co")) docAmenities.add("privateWC")
        // --- Tiện ích không có trường riêng: suy luận từ furnitureItems/serviceItems ---
        val furnitureNames = (doc.get("furnitureItems") as? List<*>)
            ?.mapNotNull { (it as? Map<*, *>)?.get("name") as? String }
            ?.map { it.lowercase() } ?: emptyList()
        val serviceNames = (doc.get("serviceItems") as? List<*>)
            ?.mapNotNull { (it as? Map<*, *>)?.get("name") as? String }
            ?.map { it.lowercase() } ?: emptyList()
        val allExtras = furnitureNames + serviceNames
        if (allExtras.any { it.contains("tủ lạnh") || it.contains("refrigerator") }) docAmenities.add("refrigerator")
        if (allExtras.any { it.contains("tivi") || it.contains("tv") })               docAmenities.add("tv")
        if (allExtras.any { it.contains("bảo vệ") || it.contains("camera") })         docAmenities.add("security")
        // full-furniture: nếu bed+wardrobe+aircon đều có → coi là "có nội thất"
        if (doc.getBoolean("hasBed") == true && doc.getBoolean("hasWardrobe") == true) docAmenities.add("furniture")

        // Hard exclusion: kiểm tra xem phòng có đầy đủ các tiện ích được yêu cầu không
        for (amenity in params.amenities) {
            if (!docAmenities.contains(amenity)) return -1
        }

        // 3. Diện tích (area)
        val docArea = (doc.getLong("area") ?: 0L).toInt()
        if (params.minArea != null && docArea < params.minArea) return -1
        if (params.maxArea != null && docArea > params.maxArea) return -1

        // 4. Loại phòng (roomType)
        if (params.roomType != null) {
            val docType = doc.getString("roomType") ?: ""
            if (!docType.contains(params.roomType, ignoreCase = true)) return -1
        }

        // 5. Giới tính ưu tiên (genderPrefer)
        if (params.genderPrefer != null) {
            val docGender = doc.getString("genderPrefer") ?: doc.getString("gender") ?: ""
            if (docGender.isNotEmpty() && !docGender.contains("Tất cả", ignoreCase = true) && !docGender.contains(params.genderPrefer, ignoreCase = true)) {
                return -1
            }
        }

        // 6. Giá điện (electricPrice)
        if (params.maxElectricPrice != null) {
            val docElec = doc.getLong("electricPrice") ?: 0L
            if (docElec > 0 && docElec > params.maxElectricPrice) return -1
        }

        // 7. Giá nước (waterPrice)
        if (params.maxWaterPrice != null) {
            val docWater = doc.getLong("waterPrice") ?: 0L
            if (docWater > 0 && docWater > params.maxWaterPrice) return -1
        }

        // 8. Giá wifi (wifiPrice)
        if (params.maxWifiPrice != null) {
            val docWifi = doc.getLong("wifiPrice") ?: 0L
            if (docWifi > 0 && docWifi > params.maxWifiPrice) return -1
        }

        // 9. Số tháng cọc (depositMonths)
        if (params.maxDepositMonths != null) {
            val docDep = (doc.getLong("depositMonths") ?: 0L).toInt()
            if (docDep > params.maxDepositMonths) return -1
        }

        // 10. Cho phép nuôi thú cưng (pet)
        if (params.petAllowed != null) {
            val docPet = doc.getString("pet") ?: ""
            val docPetAllowed = docPet.contains("được", ignoreCase = true)
                || docPet.contains("thỏa thuận", ignoreCase = true)
                || docPet.contains("cho nuôi", ignoreCase = true)
                || docPet.contains("nuôi", ignoreCase = true)
            if (docPetAllowed != params.petAllowed) return -1
        }

        // 11. Giờ giấc tự do (curfew)
        if (params.freeTime != null) {
            val docCurfew = doc.getString("curfew") ?: ""
            val docFree = docCurfew.contains("tự do", ignoreCase = true) || docCurfew.contains("không chung chủ", ignoreCase = true)
            if (docFree != params.freeTime) return -1
        }

        // 12. Số người tối đa (peopleCount)
        if (params.maxPeopleCount != null) {
            val docPeople = (doc.getLong("peopleCount") ?: 0L).toInt()
            if (docPeople > 0 && docPeople < params.maxPeopleCount) return -1
        }

        // SCORING
        var score = 0
        if (params.ward != null) {
            if (docAddress.isNotEmpty() && docAddress.contains(params.ward.lowercase()))
                score += 3
        }
        if (params.maxPrice != null && docPrice <= params.maxPrice) score += 2
        if (params.minPrice != null && docPrice >= params.minPrice) score += 1
        for (amenity in params.amenities) {
            if (docAmenities.contains(amenity)) score += 1
        }
        if (params.minArea != null || params.maxArea != null) score += 1
        if (params.roomType != null) score += 1
        if (params.genderPrefer != null) score += 1
        return score
    }

    private fun searchRooms(params: ChatbotEngine.SearchParams, uid: String) {
        // --- Nhánh Geo-Search: tìm theo địa chỉ + bán kính ---
        if (params.addressQuery != null && params.radiusKm != null) {
            displayMessages.add(AIMessage("typing", "...", System.currentTimeMillis()))
            _messages.value = displayMessages.toList()

            // Dùng cache tọa độ nếu cùng địa chỉ → tránh geocode lại cho ra kết quả khác nhau
            if (params.addressQuery == cachedGeoAddress &&
                cachedGeoLat != null && cachedGeoLng != null) {
                searchRoomsByGeo(cachedGeoLat!!, cachedGeoLng!!, params.radiusKm, params, uid)
                return
            }

            geocodeAddress(params.addressQuery) { lat, lng ->
                if (lat == null || lng == null) {
                    removeTypingIndicator()
                    val t = System.currentTimeMillis()
                    val msg = "⚠️ Không tìm được vị trí cho địa chỉ bạn nhập.\n" +
                        "Vui lòng kiểm tra lại hoặc thử nhập thêm tên quận/phố " +
                        "(ví dụ: \"ngõ 6 Cầu Giấy\")."
                    displayMessages.add(AIMessage("model", msg, t,
                        quickReplies = listOf("🔍 Tìm lại", "Đổi khu vực")))
                    _messages.value = displayMessages.toList()
                } else {
                    // Lưu cache tọa độ để lần "tìm lại" dùng cùng điểm
                    cachedGeoAddress = params.addressQuery
                    cachedGeoLat = lat
                    cachedGeoLng = lng
                    searchRoomsByGeo(lat, lng, params.radiusKm, params, uid)
                }
            }
            return
        }

        // --- Luồng cũ: tìm theo quận/huyện ---
        displayMessages.add(AIMessage("typing", "...", System.currentTimeMillis()))
        _messages.value = displayMessages.toList()

        // Security rule yêu cầu query phải có status=="approved" (firestore.rules line 144).
        // Chỉ dùng single-field filter "status" để tránh composite index.
        // Tất cả các bộ lọc đều thực hiện client-side để linh hoạt và tránh index.
        aiChatRepository.fetchApprovedRooms(50,
            onSuccess = { docs ->
                removeTypingIndicator()
                val maxScore = computeMaxScore(params)

                data class Scored(val room: AIRoom, val score: Int)

                val allScored = docs.mapNotNull { doc ->
                    // Client-side district filter: normalize để tránh sai dấu/hoa thường
                    if (params.district != null) {
                        val normalizedSearch = LocationNormalizer.normalizeRaw(params.district)
                        val docWardNorm = LocationNormalizer.normalizeRaw(doc.getString("ward") ?: "")
                        val docDistrictNorm = LocationNormalizer.normalizeRaw(doc.getString("district") ?: "")
                        if (docWardNorm != normalizedSearch && docDistrictNorm != normalizedSearch) return@mapNotNull null
                    }

                    val docPrice = doc.getLong("price") ?: 0L

                    // Client-side price filter
                    if (params.maxPrice != null && docPrice > params.maxPrice) return@mapNotNull null
                    if (params.minPrice != null && docPrice < params.minPrice) return@mapNotNull null

                    val s = scoreRoom(doc, params)
                    if (s < 0) return@mapNotNull null   // hard-excluded

                    val imageUrl = (doc.get("imageUrls") as? List<*>)
                        ?.filterIsInstance<String>()?.firstOrNull() ?: ""
                    Scored(
                        room = AIRoom(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            title = doc.getString("title") ?: "",
                            price = docPrice,
                            area = (doc.getLong("area") ?: 0L).toInt(),
                            district = doc.getString("ward") ?: doc.getString("district") ?: "",
                            imageUrl = imageUrl
                        ),
                        score = s
                    )
                }.sortedByDescending { it.score }

                // Classify into primary (≥75 % of max) and related (35–74 %)
                // When maxScore == 0 (only district filter), all rooms are primary.
                val primary: List<AIRoom>
                val related: List<AIRoom>
                if (maxScore == 0) {
                    primary = allScored.map { it.room }.take(10)
                    related = emptyList()
                } else {
                    val primaryMin = (maxScore * 0.75).toInt().coerceAtLeast(1)
                    val relatedMin = (maxScore * 0.35).toInt().coerceAtLeast(1)
                    primary = allScored.filter { it.score >= primaryMin }.map { it.room }.take(5)
                    related = allScored
                        .filter { it.score in relatedMin until primaryMin }
                        .map { it.room }.take(3)
                }

                currentContext = currentContext.copy(lastResultCount = primary.size + related.size)
                saveContext(uid)

                val districtStr = when {
                    params.ward != null && params.district != null ->
                        " ở **${params.ward}**, **${params.district}**"
                    params.district != null -> " ở **${params.district}**"
                    else -> ""
                }
                val priceStr = when {
                    params.minPrice != null && params.maxPrice != null ->
                        ", giá ${formatPrice(params.minPrice)}–${formatPrice(params.maxPrice)}"
                    params.maxPrice != null -> ", giá tối đa ${formatPrice(params.maxPrice)}"
                    params.minPrice != null -> ", giá trên ${formatPrice(params.minPrice)}"
                    else -> ""
                }
                val quickReplies = listOf("🔍 Tìm lại", "Đổi khu vực", "Đổi mức giá")

                if (primary.isEmpty() && related.isEmpty()) {
                    // No results at all — suggest nearby districts if we searched by district
                    val t = System.currentTimeMillis()
                    val nearbyChips = params.district?.let { NEARBY_DISTRICTS[it] } ?: emptyList()
                    val noResultMsg = if (nearbyChips.isNotEmpty()) {
                        chatbotEngine.randomUiResponse("no_results",
                            mapOf("district" to districtStr, "price" to priceStr)) +
                            "\nBạn có muốn thử tìm ở khu lân cận không?"
                    } else {
                        chatbotEngine.randomUiResponse("no_results",
                            mapOf("district" to districtStr, "price" to priceStr))
                    }
                    val noResultReplies = if (nearbyChips.isNotEmpty()) nearbyChips + "Đổi mức giá"
                                         else quickReplies
                    postAndSave(uid, noResultMsg, t, emptyList(), noResultReplies)
                } else {
                    // Primary results message
                    val t1 = System.currentTimeMillis()
                    val primaryMsg = if (primary.isEmpty()) {
                        // No exact match but there are related — softer intro
                        chatbotEngine.randomUiResponse("no_results",
                            mapOf("district" to districtStr, "price" to priceStr))
                    } else {
                        chatbotEngine.randomUiResponse("found_results",
                            mapOf("count" to primary.size.toString(),
                                  "district" to districtStr, "price" to priceStr))
                    }
                    postAndSave(uid, primaryMsg, t1, primary,
                        if (related.isEmpty()) quickReplies else emptyList())

                    // Related results message (only when primary also exists)
                    if (related.isNotEmpty()) {
                        val t2 = t1 + 1
                        val relatedMsg = chatbotEngine.randomUiResponse("related_results",
                            mapOf("count" to related.size.toString()))
                        postAndSave(uid, relatedMsg, t2, related, quickReplies)
                    }
                }
            },
            onFailure = { errMsg ->
                removeTypingIndicator()
                val t = System.currentTimeMillis()
                displayMessages.add(AIMessage("model",
                    "Có lỗi khi tìm kiếm phòng. Vui lòng thử lại sau.",
                    t, quickReplies = listOf("🔍 Tìm lại")))
                _messages.value = displayMessages.toList()
                Log.e("AIChatViewModel", "Room search failed: $errMsg")
            }
        )
    }

    // Add a result message to the chat and save to Firestore.
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
        currentContext = ChatbotEngine.ConversationContext()
        addGreeting()
        _messages.value = displayMessages.toList()
        _isLoading.value = false
    }

    // --- Helpers ---

    fun resolveQuickReplyAction(displayText: String): String =
        ChatbotEngine.QUICK_REPLY_ACTIONS[displayText] ?: displayText

    private fun addGreeting() {
        displayMessages.add(AIMessage(
            role = "model",
            content = "Xin chào! Mình là Trợ lý hỗ trợ tìm kiếm phòng trọ từ Tìm trọ 24/7.\nBạn cần tìm phòng khu vực nào hay cần mình tư vấn điều gì không?",
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
                    else String.format("%.1f", millions)
            "${s}tr"
        } else "${price / 1000}K"
    }

    // --- Geocoding via Google Maps API & Fallbacks ---
    // Chạy trên background thread, gọi callback trên MainThread.
    private fun geocodeAddress(address: String, onResult: (lat: Double?, lng: Double?) -> Unit) {
        Thread {
            val context = getApplication<Application>()
            
            // 1. Thử dùng Google Maps Geocoding Web API trực tiếp bằng API Key của ứng dụng
            try {
                val apiKey = context.getString(com.example.doantotnghiep.R.string.google_maps_key)
                if (apiKey.isNotBlank() && apiKey != "YOUR_KEY_HERE") {
                    val hasHanoi = address.contains("hà nội", ignoreCase = true) ||
                                   address.contains("ha noi", ignoreCase = true)
                    val finalQuery = if (hasHanoi) address else "$address, Hà Nội, Việt Nam"
                    val encoded = java.net.URLEncoder.encode(finalQuery, "UTF-8")
                    
                    val url = "https://maps.googleapis.com/maps/api/geocode/json?address=$encoded&key=$apiKey&language=vi"
                    val request = Request.Builder()
                        .url(url)
                        .build()
                        
                    val response = httpClient.newCall(request).execute()
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val jsonObj = JSONObject(body)
                        val status = jsonObj.optString("status")
                        if (status == "OK") {
                            val results = jsonObj.getJSONArray("results")
                            if (results.length() > 0) {
                                val geometry = results.getJSONObject(0).getJSONObject("geometry")
                                val location = geometry.getJSONObject("location")
                                val lat = location.getDouble("lat")
                                val lng = location.getDouble("lng")
                                Log.d("AIChatViewModel", "Geocoded '$address' via Google Maps Web API → ($lat, $lng)")
                                Handler(Looper.getMainLooper()).post { onResult(lat, lng) }
                                return@Thread
                            }
                        } else {
                            Log.e("AIChatViewModel", "Google Geocoding API returned status: $status")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AIChatViewModel", "Google Maps Geocoding API error, trying system Geocoder fallback...", e)
            }

            // 2. Dự phòng 1: Thử dùng Android Geocoder của thiết bị (sử dụng Google Play Services)
            if (android.location.Geocoder.isPresent()) {
                try {
                    val geocoder = android.location.Geocoder(context, java.util.Locale("vi", "VN"))
                    var addresses = geocoder.getFromLocationName(address, 1)
                    if (addresses.isNullOrEmpty() && !address.contains("hà nội", ignoreCase = true) && !address.contains("ha noi", ignoreCase = true)) {
                        addresses = geocoder.getFromLocationName("$address, Hà Nội, Việt Nam", 1)
                    }
                    
                    if (!addresses.isNullOrEmpty()) {
                        val location = addresses[0]
                        val lat = location.latitude
                        val lng = location.longitude
                        Log.d("AIChatViewModel", "Geocoded '$address' via System Geocoder (Google Maps) → ($lat, $lng)")
                        Handler(Looper.getMainLooper()).post { onResult(lat, lng) }
                        return@Thread
                    }
                } catch (e: Exception) {
                    Log.e("AIChatViewModel", "System Geocoder failed, trying OSM fallback...", e)
                }
            }

            // 3. Dự phòng 2: Gọi OpenStreetMap Nominatim API làm phương án cuối cùng
            try {
                val hasHanoi = address.contains("hà nội", ignoreCase = true) ||
                               address.contains("ha noi", ignoreCase = true)

                // Chiến lược 1: query gốc (+ Hà Nội nếu chưa có)
                val q1 = if (hasHanoi) address else "$address, Hà Nội, Việt Nam"

                // Chiến lược 2: bỏ dấu tiếng Việt + Hanoi
                val noAccent = java.text.Normalizer.normalize(address, java.text.Normalizer.Form.NFD)
                    .replace(Regex("[\\u0300-\\u036f\\u1dc0-\\u1dff]"), "")
                    .replace('đ', 'd').replace('Đ', 'D')
                val q2 = if (hasHanoi) noAccent else "$noAccent, Hanoi, Vietnam"

                // Chiến lược 3: chỉ dùng phần tên chính (bỏ số nhà, ngõ đầu chuỗi)
                val coreName = address
                    .replace(Regex("^(số\\s*\\d+[,\\s]*|ngõ\\s*\\d+[,\\s]*|ngách\\s*\\d+[,\\s]*)"), "")
                    .trim()
                val q3 = if (coreName != address) "$coreName, Hà Nội, Việt Nam" else null

                val queries = listOfNotNull(q1, q2, q3)
                for (q in queries) {
                    val encoded = java.net.URLEncoder.encode(q, "UTF-8")
                    val url = "https://nominatim.openstreetmap.org/search" +
                        "?q=$encoded&format=json&limit=1&accept-language=vi&countrycodes=vn"
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "TimTro24_7_Android/1.0")
                        .build()
                    val response = httpClient.newCall(request).execute()
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val arr = JSONArray(body)
                        if (arr.length() > 0) {
                            val obj = arr.getJSONObject(0)
                            val lat = obj.getDouble("lat")
                            val lng = obj.getDouble("lon")
                            Log.d("AIChatViewModel", "Geocoded '$address' via OSM query '$q' → ($lat, $lng)")
                            Handler(Looper.getMainLooper()).post { onResult(lat, lng) }
                            return@Thread
                        }
                    }
                    // Tránh rate-limit Nominatim (max 1 req/s)
                    Thread.sleep(1100)
                }
                Log.w("AIChatViewModel", "Geocoding failed for all OSM queries of: $address")
                Handler(Looper.getMainLooper()).post { onResult(null, null) }
            } catch (e: Exception) {
                Log.e("AIChatViewModel", "Geocoding OSM error", e)
                Handler(Looper.getMainLooper()).post { onResult(null, null) }
            }
        }.start()
    }

    // --- Tìm phòng theo bán kính địa lý ---
    private fun searchRoomsByGeo(
        lat: Double, lng: Double,
        radiusKm: Double,
        params: ChatbotEngine.SearchParams,
        uid: String
    ) {
        val radiusLabel = chatbotEngine.formatRadius(radiusKm)
        roomRepository.searchNearbyRooms(
            lat = lat,
            radiusKm = radiusKm,
            onSuccess = { docs ->
                removeTypingIndicator()

                data class Scored(val room: AIRoom, val score: Int, val distKm: Double)

                val allScored = docs.mapNotNull { doc ->
                    val docLat = doc.getDouble("latitude") ?: return@mapNotNull null
                    val docLng = doc.getDouble("longitude") ?: return@mapNotNull null

                    // Lọc haversine chính xác (bounding-box của Firestore đã sơ lọc)
                    val distKm = GeoUtils.haversineKm(lat, lng, docLat, docLng)
                    if (distKm > radiusKm) return@mapNotNull null

                    val docPrice = doc.getLong("price") ?: 0L
                    if (params.maxPrice != null && docPrice > params.maxPrice) return@mapNotNull null
                    if (params.minPrice != null && docPrice < params.minPrice) return@mapNotNull null

                    // Áp dụng bộ lọc và tính điểm các tiêu chí khác từ scoreRoom
                    val criteriaScore = scoreRoom(doc, params)
                    if (criteriaScore < 0) return@mapNotNull null // Hard-exclude nếu không thỏa mãn bộ lọc nâng cao

                    var score = 0
                    // Gần hơn → điểm cao hơn (tối đa 5 điểm)
                    score += when {
                        distKm <= radiusKm * 0.25 -> 5
                        distKm <= radiusKm * 0.5  -> 4
                        distKm <= radiusKm * 0.75 -> 3
                        else                      -> 2
                    }
                    // Cộng thêm điểm từ các tiêu chí khớp khác
                    score += criteriaScore

                    val imageUrl = (doc.get("imageUrls") as? List<*>)
                        ?.filterIsInstance<String>()?.firstOrNull() ?: ""
                    Scored(
                        room = AIRoom(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            title = doc.getString("title") ?: "",
                            price = docPrice,
                            area = (doc.getLong("area") ?: 0L).toInt(),
                            district = doc.getString("ward") ?: doc.getString("district") ?: "",
                            imageUrl = imageUrl
                        ),
                        score = score,
                        distKm = distKm
                    )
                }.sortedWith(compareByDescending<Scored> { it.score }.thenBy { it.distKm })

                currentContext = currentContext.copy(lastResultCount = allScored.size)
                saveContext(uid)

                val priceStr = when {
                    params.minPrice != null && params.maxPrice != null ->
                        ", giá ${formatPrice(params.minPrice)}–${formatPrice(params.maxPrice)}"
                    params.maxPrice != null -> ", giá tối đa ${formatPrice(params.maxPrice)}"
                    params.minPrice != null -> ", giá trên ${formatPrice(params.minPrice)}"
                    else -> ""
                }
                val quickReplies = listOf("🔍 Tìm lại", "Đổi bán kính", "Đổi mức giá")
                val t = System.currentTimeMillis()

                if (allScored.isEmpty()) {
                    // Nếu tiện ích filter gây ra "không có kết quả" → thử lại không lọc tiện ích
                    val relaxedRooms = if (params.amenities.isNotEmpty()) {
                        val relaxedParams = params.copy(amenities = emptyList())
                        docs.mapNotNull { doc ->
                            val docLat2 = doc.getDouble("latitude") ?: return@mapNotNull null
                            val docLng2 = doc.getDouble("longitude") ?: return@mapNotNull null
                            val dist2 = GeoUtils.haversineKm(lat, lng, docLat2, docLng2)
                            if (dist2 > radiusKm) return@mapNotNull null
                            val price2 = doc.getLong("price") ?: 0L
                            if (params.maxPrice != null && price2 > params.maxPrice) return@mapNotNull null
                            if (params.minPrice != null && price2 < params.minPrice) return@mapNotNull null
                            val s2 = scoreRoom(doc, relaxedParams)
                            if (s2 < 0) return@mapNotNull null
                            val proximity2 = when {
                                dist2 <= radiusKm * 0.25 -> 5
                                dist2 <= radiusKm * 0.5  -> 4
                                dist2 <= radiusKm * 0.75 -> 3
                                else                     -> 2
                            }
                            val img2 = (doc.get("imageUrls") as? List<*>)
                                ?.filterIsInstance<String>()?.firstOrNull() ?: ""
                            Scored(
                                room = AIRoom(doc.id, doc.getString("userId") ?: "",
                                    doc.getString("title") ?: "", price2,
                                    (doc.getLong("area") ?: 0L).toInt(),
                                    doc.getString("ward") ?: doc.getString("district") ?: "", img2),
                                score = proximity2 + s2,
                                distKm = dist2
                            )
                        }.sortedWith(compareByDescending<Scored> { it.score }.thenBy { it.distKm })
                            .take(5)
                    } else emptyList()

                    if (relaxedRooms.isNotEmpty()) {
                        val amenityNames = params.amenities
                            .mapNotNull { AMENITY_DISPLAY[it] }.joinToString(", ")
                        currentContext = currentContext.copy(lastResultCount = relaxedRooms.size)
                        saveContext(uid)
                        val relaxedMsg = chatbotEngine.randomUiResponse(
                            "no_amenity_match",
                            mapOf("amenities" to amenityNames, "radius" to radiusLabel, "price" to priceStr)
                        )
                        postAndSave(uid, relaxedMsg, t, relaxedRooms.map { it.room }, quickReplies)
                    } else {
                        val noResultMsg = chatbotEngine.randomUiResponse("no_geo_results",
                            mapOf("radius" to radiusLabel, "price" to priceStr))
                        postAndSave(uid, noResultMsg, t, emptyList(), quickReplies)
                    }
                } else {
                    val rooms = allScored.map { it.room }.take(8)
                    val foundMsg = chatbotEngine.randomUiResponse("found_geo_results",
                        mapOf("count" to rooms.size.toString(),
                              "radius" to radiusLabel, "price" to priceStr))
                    postAndSave(uid, foundMsg, t, rooms, quickReplies)
                }
            },
            onFailure = { errMsg ->
                removeTypingIndicator()
                val t = System.currentTimeMillis()
                displayMessages.add(AIMessage("model",
                    "Có lỗi khi tìm kiếm quanh địa chỉ: $errMsg\nVui lòng thử lại sau.",
                    t, quickReplies = listOf("🔍 Tìm lại")))
                _messages.value = displayMessages.toList()
                Log.e("AIChatViewModel", "Geo search failed: $errMsg")
            }
        )
    }

    companion object {
        private val AMENITY_DISPLAY = mapOf(
            "airConditioner" to "điều hòa",
            "washer" to "máy giặt",
            "wifi" to "wifi",
            "parking" to "chỗ để xe",
            "kitchen" to "bếp riêng",
            "balcony" to "ban công",
            "furniture" to "nội thất",
            "refrigerator" to "tủ lạnh",
            "tv" to "tivi",
            "security" to "bảo vệ",
            "waterHeater" to "bình nóng lạnh",
            "privateWC" to "WC riêng"
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
            // "Từ Liêm" là canonical cho cả Nam Từ Liêm và Bắc Từ Liêm sau cải cách 2025
            "Từ Liêm"      to listOf("Cầu Giấy", "Hà Đông", "Tây Hồ")
        )
    }
}
