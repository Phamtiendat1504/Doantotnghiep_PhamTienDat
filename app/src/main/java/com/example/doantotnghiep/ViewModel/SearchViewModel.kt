package com.example.doantotnghiep.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.Utils.LocationNormalizer
import com.example.doantotnghiep.repository.RoomRepository
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

class SearchViewModel : ViewModel() {

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
        val distanceKm: Double = -1.0  // -1 = chưa tính khoảng cách
    )

    fun searchByQuery(query: String) {
        _isLoading.value = true
        _errorMessage.value = null
        repository.searchApprovedRooms(
            onSuccess = { docs ->
                _isLoading.value = false
                val normalizedQuery = LocationNormalizer.normalizeRaw(query)
                val matched = docs.mapNotNull { doc ->
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
        hasWater: Boolean
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

        repository.searchApprovedRooms(
            onSuccess = { docs ->
                _isLoading.value = false

                val ranked = docs.mapNotNull { doc ->
                    val item = mapToRoomItem(doc.id, doc.data)

                    if (!hasAvailableRoom(item)) return@mapNotNull null
                    if (!matchesLocation(item, mode, wardFilter, districtFilter)) return@mapNotNull null
                    if (!matchesPrice(item.price, minPrice, maxPrice)) return@mapNotNull null
                    if (!matchesArea(item.area, minArea, maxArea)) return@mapNotNull null
                    if (!matchesAddress(item, addressFilter)) return@mapNotNull null
                    if (!matchesRoomType(item.roomType, roomTypeFilter)) return@mapNotNull null
                    if (!matchesPeople(item.peopleCount, desiredPeople)) return@mapNotNull null
                    if (hasWifi && !item.hasWifi) return@mapNotNull null
                    if (hasElectric && !item.hasElectric) return@mapNotNull null
                    if (hasWater && !item.hasWater) return@mapNotNull null

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
    fun searchNearby(lat: Double, lng: Double, radiusKm: Double) {
        _isLoading.value = true
        _errorMessage.value = null

        repository.searchApprovedRooms(
            onSuccess = { docs ->
                _isLoading.value = false

                val results = docs.mapNotNull { doc ->
                    val item = mapToRoomItem(doc.id, doc.data)

                    // Bỏ qua phòng không có tọa độ
                    if (item.lat == 0.0 && item.lng == 0.0) return@mapNotNull null
                    // Bỏ qua phòng hết chỗ
                    if (!hasAvailableRoom(item)) return@mapNotNull null

                    val distKm = haversineKm(lat, lng, item.lat, item.lng)
                    if (distKm > radiusKm) return@mapNotNull null

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

    /**
     * Công thức Haversine – tính khoảng cách (km) giữa 2 tọa độ GPS.
     * Độ chính xác ~0.5% – đủ tốt cho bán kính vài km.
     */
    private fun haversineKm(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double
    ): Double {
        val r = 6371.0  // Bán kính Trái Đất (km)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).let { it * it } +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2).let { it * it }
        return r * 2 * asin(sqrt(a))
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
        if (area <= 0) return false

        val minOk = minArea <= 0 || area >= minArea
        val maxOk = maxArea <= 0 || area <= maxArea
        return minOk && maxOk
    }

    private fun matchesAddress(item: RoomItem, addressFilter: String): Boolean {
        if (addressFilter.isEmpty()) return true
        val full = LocationNormalizer.normalizeRaw("${item.address} ${item.ward} ${item.district}")
        return full.contains(addressFilter)
    }

    private fun matchesRoomType(roomType: String, roomTypeFilter: String): Boolean {
        if (roomTypeFilter.isEmpty()) return true
        return LocationNormalizer.normalizeRaw(roomType) == roomTypeFilter
    }

    private fun matchesPeople(peopleCount: Int, desiredPeople: Int): Boolean {
        if (desiredPeople <= 0) return true
        if (peopleCount <= 0) return false
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
            hasElectric = data["hasElectric"] as? Boolean ?: false,
            hasWater = data["hasWater"] as? Boolean ?: false,
            roomCount = (data["roomCount"] as? Number)?.toInt() ?: 0,
            rentedCount = (data["rentedCount"] as? Number)?.toInt() ?: 0,
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
            lat = (data["latitude"] as? Number)?.toDouble() ?: 0.0,
            lng = (data["longitude"] as? Number)?.toDouble() ?: 0.0
        )
    }

    private data class ScoredRoom(
        val item: RoomItem,
        val score: Double
    )
}
