package com.example.doantotnghiep.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.Utils.GeoUtils
import com.example.doantotnghiep.Utils.LocationNormalizer
import com.example.doantotnghiep.repository.RoomRepository
import kotlin.math.abs
import kotlin.math.max

class SearchViewModel : ViewModel() {

    companion object {
        private val REGEX_24H = "^(\\d{1,2})[:H](\\d{2})?$".toRegex()
        private val REGEX_12H = "^(\\d{1,2})[:H](\\d{2})?\\s*(AM|PM)$".toRegex()
    }

    private val repository = RoomRepository()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _searchResults = MutableLiveData<List<RoomItem>>()
    val searchResults: LiveData<List<RoomItem>> = _searchResults

    data class RoomItem(
        val roomId: String,
        val userId: String,
        val title: String,
        val address: String,
        val ward: String,
        val district: String,
        val firstImage: String,
        val price: Long,
        val area: Int,
        val peopleCount: Int,
        val roomType: String,
        val hasWifi: Boolean,
        val hasElectric: Boolean,
        val hasWater: Boolean,
        val roomCount: Int,
        val rentedCount: Int,
        val createdAt: Long,
        val lat: Double = 0.0,
        val lng: Double = 0.0,
        val distanceKm: Double = -1.0,
        val isFeatured: Boolean = false,
        val curfew: String = "",
        val curfewTime: String = "",
        val wifiCost: Long = 0L,
        val electricCost: Long = 0L,
        val waterCost: Long = 0L
    )

    fun resetErrorMessage() {
        _errorMessage.value = null
    }

    // Trả về true nếu bài đăng đã quá hạn hiển thị do chủ trọ thiết lập
    private fun isPostExpiredByDate(data: Map<String, Any>): Boolean {
        val expiry = (data["postExpiryDate"] as? Number)?.toLong() ?: 0L
        return expiry > 0L && expiry < System.currentTimeMillis()
    }

    fun searchByQuery(query: String) {
        _isLoading.value = true
        _errorMessage.value = null
        repository.searchApprovedRooms(
            onSuccess = { docs ->
                _isLoading.value = false
                val normalizedQuery = LocationNormalizer.normalizeRaw(query)
                val matched = docs.mapNotNull { doc ->
                    if (isPostExpiredByDate(doc.data)) return@mapNotNull null
                    val item = mapToRoomItem(doc.id, doc.data)
                    if (!hasAvailableRoom(item)) return@mapNotNull null

                    val title = LocationNormalizer.normalizeRaw(item.title)
                    val ward = LocationNormalizer.normalizeWard(item.ward)
                    val district = LocationNormalizer.normalizeDistrict(item.district)
                    val address = LocationNormalizer.normalizeRaw(item.address)
                    val description = LocationNormalizer.normalizeRaw(doc.data["description"] as? String ?: "")

                    val hit = title.contains(normalizedQuery) ||
                        ward.contains(normalizedQuery) ||
                        district.contains(normalizedQuery) ||
                        address.contains(normalizedQuery) ||
                        description.contains(normalizedQuery)

                    if (hit) item else null
                }.sortedByDescending { it.createdAt }

                _searchResults.value = matched
            },
            onFailure = { e ->
                _isLoading.value = false
                _errorMessage.value = e
                _searchResults.value = emptyList()
            }
        )
    }

    fun searchByFilters(
        ward: String,
        district: String,
        searchMode: String,
        minPrice: Long,
        maxPrice: Long,
        addressKeyword: String,
        minArea: Int,
        maxArea: Int,
        desiredPeople: Int,
        roomType: String,
        hasWifi: Boolean,
        hasElectric: Boolean,
        hasWater: Boolean,
        curfew: String = "",
        maxWifiPrice: Long = 0L,
        maxElectricPrice: Long = 0L,
        maxWaterPrice: Long = 0L
    ) {
        _isLoading.value = true
        _errorMessage.value = null

        val wardFilter = LocationNormalizer.normalizeWard(ward)
        val districtFilter = LocationNormalizer.normalizeDistrict(district)
        val addressFilter = LocationNormalizer.normalizeRaw(addressKeyword)
        val mode = LocationNormalizer.normalizeRaw(searchMode)
        val roomTypeFilter = LocationNormalizer.normalizeRaw(roomType)

        val hasPriceFilter = minPrice > 0L || maxPrice > 0L
        val hasAreaFilter = minArea > 0 || maxArea > 0

        val targetPrice = when {
            minPrice > 0L && maxPrice > 0L -> (minPrice + maxPrice) / 2.0
            minPrice > 0L -> minPrice.toDouble()
            maxPrice > 0L -> maxPrice.toDouble()
            else -> 0.0
        }

        val targetArea = when {
            minArea > 0 && maxArea > 0 -> (minArea + maxArea) / 2.0
            minArea > 0 -> minArea.toDouble()
            maxArea > 0 -> maxArea.toDouble()
            else -> 0.0
        }

        // Tối ưu hóa: Gọi searchRoomsWithBasicFilters để lọc theo quận/huyện trên server trước,
        // giúp giảm thiểu số lượng tài liệu tải về client-side.
        repository.searchRoomsWithBasicFilters(
            district = district,
            ward = ward,
            onSuccess = { docs ->
                _isLoading.value = false

                val ranked = docs.mapNotNull { doc ->
                    if (isPostExpiredByDate(doc.data)) return@mapNotNull null
                    val item = mapToRoomItem(doc.id, doc.data)

                    if (!hasAvailableRoom(item)) return@mapNotNull null

                    // Vẫn giữ check location client-side đề phòng trường hợp lỗi chuẩn hóa chuỗi
                    if (!matchesLocation(item, mode, wardFilter, districtFilter)) return@mapNotNull null
                    if (!matchesPrice(item.price, minPrice, maxPrice)) return@mapNotNull null
                    if (!matchesArea(item.area, minArea, maxArea)) return@mapNotNull null
                    if (!matchesAddress(item, addressFilter)) return@mapNotNull null
                    if (!matchesRoomType(item.roomType, roomTypeFilter)) return@mapNotNull null
                    if (!matchesPeople(item.peopleCount, desiredPeople)) return@mapNotNull null
                    if (hasWifi && !item.hasWifi) return@mapNotNull null
                    if (hasElectric && !item.hasElectric) return@mapNotNull null
                    if (hasWater && !item.hasWater) return@mapNotNull null
                    if (!matchesCurfew(item.curfew, item.curfewTime, curfew)) return@mapNotNull null
                    // Lọc giá tiện ích chỉ áp dụng khi người dùng tích checkbox tiện ích tương ứng
                    if (hasWifi && maxWifiPrice > 0L && item.hasWifi && item.wifiCost > maxWifiPrice) return@mapNotNull null
                    if (hasElectric && maxElectricPrice > 0L && item.hasElectric && item.electricCost > maxElectricPrice) return@mapNotNull null
                    if (hasWater && maxWaterPrice > 0L && item.hasWater && item.waterCost > maxWaterPrice) return@mapNotNull null

                    val score = calculateRankingScore(
                        item = item,
                        mode = mode,
                        wardFilter = wardFilter,
                        districtFilter = districtFilter,
                        hasPriceFilter = hasPriceFilter,
                        targetPrice = targetPrice,
                        hasAreaFilter = hasAreaFilter,
                        targetArea = targetArea,
                        roomTypeFilter = roomTypeFilter,
                        desiredPeople = desiredPeople,
                        hasWifi = hasWifi,
                        hasElectric = hasElectric,
                        hasWater = hasWater
                    )

                    ScoredRoom(item = item, score = score)
                }

                val sorted = ranked
                    .sortedWith(compareByDescending<ScoredRoom> { it.score }.thenByDescending { it.item.createdAt })
                    .map { it.item }

                _searchResults.value = sorted
            },
            onFailure = { e ->
                _isLoading.value = false
                _errorMessage.value = e
                _searchResults.value = emptyList()
            }
        )
    }

    // ────────────────────────────────────────────────
    // Tìm kiếm theo vị trí bản đồ (Haversine)
    // ────────────────────────────────────────────────
    fun searchNearby(
        lat: Double,
        lng: Double,
        radiusKm: Double,
        minPrice: Long = 0L,
        maxPrice: Long = 0L,
        minArea: Int = 0,
        maxArea: Int = 0,
        desiredPeople: Int = 0,
        roomType: String = "",
        hasWifi: Boolean = false,
        hasElectric: Boolean = false,
        hasWater: Boolean = false,
        curfew: String = "",
        maxWifiPrice: Long = 0L,
        maxElectricPrice: Long = 0L,
        maxWaterPrice: Long = 0L
    ) {
        _isLoading.value = true
        _errorMessage.value = null

        val roomTypeFilter = LocationNormalizer.normalizeRaw(roomType)

        repository.searchNearbyRooms(
            lat = lat,
            radiusKm = radiusKm,
            onSuccess = { docs ->
                _isLoading.value = false

                val results = docs.mapNotNull { doc ->
                    if (isPostExpiredByDate(doc.data)) return@mapNotNull null
                    val item = mapToRoomItem(doc.id, doc.data)

                    if (item.lat == 0.0 && item.lng == 0.0) return@mapNotNull null
                    if (!hasAvailableRoom(item)) return@mapNotNull null

                    val distKm = GeoUtils.haversineKm(lat, lng, item.lat, item.lng)
                    if (distKm > radiusKm) return@mapNotNull null

                    if (!matchesPrice(item.price, minPrice, maxPrice)) return@mapNotNull null
                    if (!matchesArea(item.area, minArea, maxArea)) return@mapNotNull null
                    if (!matchesRoomType(item.roomType, roomTypeFilter)) return@mapNotNull null
                    if (!matchesPeople(item.peopleCount, desiredPeople)) return@mapNotNull null
                    if (hasWifi && !item.hasWifi) return@mapNotNull null
                    if (hasElectric && !item.hasElectric) return@mapNotNull null
                    if (hasWater && !item.hasWater) return@mapNotNull null
                    if (!matchesCurfew(item.curfew, item.curfewTime, curfew)) return@mapNotNull null
                    // Lọc giá tiện ích chỉ áp dụng khi người dùng tích checkbox tiện ích tương ứng
                    if (hasWifi && maxWifiPrice > 0L && item.hasWifi && item.wifiCost > maxWifiPrice) return@mapNotNull null
                    if (hasElectric && maxElectricPrice > 0L && item.hasElectric && item.electricCost > maxElectricPrice) return@mapNotNull null
                    if (hasWater && maxWaterPrice > 0L && item.hasWater && item.waterCost > maxWaterPrice) return@mapNotNull null

                    item.copy(distanceKm = distKm)
                }.sortedBy { it.distanceKm }

                _searchResults.value = results
            },
            onFailure = { e ->
                _isLoading.value = false
                _errorMessage.value = e
                _searchResults.value = emptyList()
            }
        )
    }

    // Tìm kiếm nhiều phòng theo danh sách document ID (khi user chọn nhiều địa chỉ từ panel)
    fun searchByPostIds(postIds: List<String>) {
        _isLoading.value    = true
        _errorMessage.value = null
        repository.getApprovedRoomsByIds(
            roomIds   = postIds,
            onSuccess = { docs ->
                _isLoading.value = false
                // Giữ nguyên thứ tự user chọn
                val orderMap = postIds.withIndex().associate { (i, id) -> id to i }
                val results  = docs.mapNotNull { doc ->
                    if (isPostExpiredByDate(doc.data ?: emptyMap())) return@mapNotNull null
                    val item = mapToRoomItem(doc.id, doc.data ?: emptyMap())
                    if (hasAvailableRoom(item)) item else null
                }.sortedBy { orderMap[it.roomId] ?: Int.MAX_VALUE }
                _searchResults.value = results
            },
            onFailure = { e ->
                _isLoading.value    = false
                _errorMessage.value = e
                _searchResults.value = emptyList()
            }
        )
    }

    // Tìm kiếm chính xác theo document ID (khi user chọn 1 phòng cụ thể từ bản đồ)
    fun searchByPostId(postId: String) {
        _isLoading.value = true
        _errorMessage.value = null
        repository.getApprovedRoomById(
            roomId = postId,
            onSuccess = { doc ->
                _isLoading.value = false
                val data = doc.data ?: emptyMap()
                if (isPostExpiredByDate(data)) {
                    _searchResults.value = emptyList()
                    return@getApprovedRoomById
                }
                val item = mapToRoomItem(doc.id, data)
                _searchResults.value = if (hasAvailableRoom(item)) listOf(item) else emptyList()
            },
            onFailure = { e ->
                _isLoading.value = false
                _errorMessage.value = e
                _searchResults.value = emptyList()
            }
        )
    }

    private fun matchesLocation(item: RoomItem, mode: String, wardFilter: String, districtFilter: String): Boolean {
        val roomWard = LocationNormalizer.normalizeWard(item.ward)
        val roomDistrict = LocationNormalizer.normalizeDistrict(item.district)

        return when {
            mode == "district" && districtFilter.isNotEmpty() -> roomDistrict == districtFilter
            mode == "ward" && wardFilter.isNotEmpty() -> roomWard == wardFilter
            wardFilter.isNotEmpty() -> roomWard == wardFilter
            districtFilter.isNotEmpty() -> roomDistrict == districtFilter
            else -> true
        }
    }

    private fun matchesPrice(price: Long, minPrice: Long, maxPrice: Long): Boolean {
        val minOk = minPrice <= 0L || price >= minPrice
        val maxOk = maxPrice <= 0L || price <= maxPrice
        return minOk && maxOk
    }

    private fun matchesArea(area: Int, minArea: Int, maxArea: Int): Boolean {
        if (minArea <= 0 && maxArea <= 0) return true
        if (area <= 0) return true  // bài đăng chưa khai báo diện tích thì bỏ qua filter

        val minOk = minArea <= 0 || area >= minArea
        val maxOk = maxArea <= 0 || area <= maxArea
        return minOk && maxOk
    }

    private fun matchesAddress(item: RoomItem, addressFilter: String): Boolean {
        if (addressFilter.isEmpty()) return true
        // Chuẩn hóa và loại bỏ dấu câu để so sánh linh hoạt hơn
        val stripPunct = Regex("[^\\p{L}\\p{N}\\s]") // giữ chữ cái, số, khoảng trắng
        val full = stripPunct.replace(
            LocationNormalizer.normalizeRaw("${item.address} ${item.ward} ${item.district}"), " "
        ).replace(Regex("\\s+"), " ").trim()
        val normalizedFilter = stripPunct.replace(addressFilter, " ")
            .replace(Regex("\\s+"), " ").trim()
        
        // Loại bỏ các stop words phổ biến trong địa chỉ để tránh bị lọc oan khi gõ thêm "đường", "phố"...
        val stopWords = setOf("duong", "pho", "ngo", "ngach", "hem", "so", "nha", "quan", "huyen", "phuong", "xa")
        val tokens = normalizedFilter.split(" ")
            .map { it.trim() }
            .filter { it.length >= 2 && !stopWords.contains(it) }

        if (tokens.isEmpty()) return true
        return tokens.all { token -> full.contains(token) }
    }

    private fun canonicalizeRoomType(rawType: String): String {
        val norm = LocationNormalizer.normalizeRaw(rawType)
        return when (norm) {
            "o ghep", "chung chu" -> "chung chu"
            "rieng tu", "rieng chu" -> "rieng chu"
            else -> norm
        }
    }

    private fun matchesRoomType(roomType: String, roomTypeFilter: String): Boolean {
        if (roomTypeFilter.isEmpty()) return true
        return canonicalizeRoomType(roomType) == canonicalizeRoomType(roomTypeFilter)
    }

    private fun parseTimeToMinutes(timeStr: String): Int {
        val clean = timeStr.trim().uppercase()

        val match12 = REGEX_12H.find(clean)
        if (match12 != null) {
            var hour = match12.groupValues[1].toInt()
            val minute = if (match12.groupValues[2].isEmpty()) 0 else match12.groupValues[2].toInt()
            val amPm = match12.groupValues[3]
            if (amPm == "PM" && hour < 12) hour += 12
            if (amPm == "AM" && hour == 12) hour = 0
            return hour * 60 + minute
        }

        val match24 = REGEX_24H.find(clean)
        if (match24 != null) {
            val hour = match24.groupValues[1].toInt()
            val minute = if (match24.groupValues[2].isEmpty()) 0 else match24.groupValues[2].toInt()
            return hour * 60 + minute
        }

        val hourOnly = clean.toIntOrNull()
        if (hourOnly != null && hourOnly in 0..24) {
            return hourOnly * 60
        }

        return -1
    }

    private fun matchesCurfew(roomCurfew: String, roomCurfewTime: String, filterCurfew: String): Boolean {
        if (filterCurfew.isEmpty()) return true
        val normalizedFilter = LocationNormalizer.normalizeRaw(filterCurfew)
        if (normalizedFilter == "tu do") {
            return roomCurfew.isBlank() ||
                LocationNormalizer.normalizeRaw(roomCurfew) == "tu do"
        }
        
        val filterMinutes = parseTimeToMinutes(filterCurfew)
        if (filterMinutes >= 0) {
            // Nếu phòng không có giới nghiêm (tự do) -> Thỏa mãn
            if (roomCurfew.isBlank() || LocationNormalizer.normalizeRaw(roomCurfew) == "tu do") return true
            
            // So sánh phút: giờ đóng cửa của phòng phải >= giờ giới nghiêm người dùng mong muốn
            val roomMinutes = parseTimeToMinutes(roomCurfewTime)
            if (roomMinutes >= 0) {
                return roomMinutes >= filterMinutes
            }
        }
        return true
    }

    private fun matchesPeople(peopleCount: Int, desiredPeople: Int): Boolean {
        if (desiredPeople <= 0) return true
        if (peopleCount <= 0) return true // FIX: Mặc định chấp nhận nếu bài đăng cũ không khai báo sức chứa để tránh bị ẩn oan
        return peopleCount >= desiredPeople
    }

    private fun hasAvailableRoom(item: RoomItem): Boolean {
        if (item.roomCount <= 0) return true
        return (item.roomCount - item.rentedCount) > 0
    }

    private fun calculateRankingScore(
        item: RoomItem,
        mode: String,
        wardFilter: String,
        districtFilter: String,
        hasPriceFilter: Boolean,
        targetPrice: Double,
        hasAreaFilter: Boolean,
        targetArea: Double,
        roomTypeFilter: String,
        desiredPeople: Int,
        hasWifi: Boolean,
        hasElectric: Boolean,
        hasWater: Boolean
    ): Double {
        var score = 0.0

        if (item.isFeatured) score += 500.0

        val roomWard = LocationNormalizer.normalizeWard(item.ward)
        val roomDistrict = LocationNormalizer.normalizeDistrict(item.district)
        if (mode == "district" && districtFilter.isNotEmpty() && roomDistrict == districtFilter) score += 1200.0
        if (mode == "ward" && wardFilter.isNotEmpty() && roomWard == wardFilter) score += 1400.0
        if (mode != "ward" && mode != "district") {
            if (wardFilter.isNotEmpty() && roomWard == wardFilter) score += 1400.0
            if (districtFilter.isNotEmpty() && roomDistrict == districtFilter) score += 1200.0
        }

        if (hasPriceFilter && targetPrice > 0) {
            val closeness = closenessScore(item.price.toDouble(), targetPrice)
            score += closeness * 600.0
        }

        if (hasAreaFilter && targetArea > 0 && item.area > 0) {
            val closeness = closenessScore(item.area.toDouble(), targetArea)
            score += closeness * 360.0
        }

        if (roomTypeFilter.isNotEmpty() && matchesRoomType(item.roomType, roomTypeFilter)) {
            score += 260.0
        }

        if (desiredPeople > 0 && item.peopleCount >= desiredPeople) {
            score += if (item.peopleCount == desiredPeople) 180.0 else 120.0
        }

        if (hasWifi && item.hasWifi) score += 90.0
        if (hasElectric && item.hasElectric) score += 90.0
        if (hasWater && item.hasWater) score += 90.0

        val now = System.currentTimeMillis()
        val ageMillis = max(0L, now - item.createdAt)
        val ageDays = ageMillis / (24.0 * 60 * 60 * 1000)
        val freshness = 1.0 / (1.0 + ageDays / 14.0)
        score += freshness * 140.0

        return score
    }

    private fun closenessScore(value: Double, target: Double): Double {
        if (target <= 0.0) return 0.0
        val ratio = abs(value - target) / target
        return (1.0 - ratio).coerceIn(0.0, 1.0)
    }

    private fun mapToRoomItem(roomId: String, data: Map<String, Any>): RoomItem {
        return RoomItem(
            roomId = roomId,
            userId = data["userId"] as? String ?: "",
            title = data["title"] as? String ?: "",
            address = data["address"] as? String ?: "",
            ward = data["ward"] as? String ?: "",
            district = data["district"] as? String ?: "",
            firstImage = (data["imageUrls"] as? List<*>)?.mapNotNull { it as? String }?.firstOrNull() ?: "",
            price = (data["price"] as? Number)?.toLong() ?: 0L,
            area = (data["area"] as? Number)?.toInt() ?: 0,
            peopleCount = (data["peopleCount"] as? Number)?.toInt() ?: 0,
            roomType = data["roomType"] as? String ?: "",
            hasWifi = data["hasWifi"] as? Boolean ?: false,
            hasElectric = (data["hasElectric"] as? Boolean)
                ?: (((data["electricPrice"] as? Number)?.toLong() ?: 0L) > 0L),
            hasWater = (data["hasWater"] as? Boolean)
                ?: (((data["waterPrice"] as? Number)?.toLong() ?: 0L) > 0L),
            roomCount = (data["roomCount"] as? Number)?.toInt() ?: 0,
            rentedCount = (data["rentedCount"] as? Number)?.toInt() ?: 0,
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
            lat = (data["latitude"] as? Number)?.toDouble() ?: 0.0,
            lng = (data["longitude"] as? Number)?.toDouble() ?: 0.0,
            isFeatured = data["isFeatured"] as? Boolean ?: false,
            curfew = data["curfew"] as? String ?: "",
            curfewTime = data["curfewTime"] as? String ?: "",
            wifiCost = (data["wifiPrice"] as? Number)?.toLong() ?: 0L,
            electricCost = (data["electricPrice"] as? Number)?.toLong() ?: 0L,
            waterCost = (data["waterPrice"] as? Number)?.toLong() ?: 0L
        )
    }

    private data class ScoredRoom(
        val item: RoomItem,
        val score: Double
    )
}
