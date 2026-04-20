package com.example.doantotnghiep.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.repository.RoomRepository
import java.util.Locale

class SearchViewModel : ViewModel() {

    private val repository = RoomRepository()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

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
        val roomCount: Int,
        val rentedCount: Int,
        val createdAt: Long
    )

    fun searchByQuery(query: String) {
        _isLoading.value = true
        repository.searchApprovedRooms(
            onSuccess = { docs ->
                _isLoading.value = false
                val lowerQuery = query.lowercase().trim()
                val matched = docs.filter { doc ->
                    val data = doc.data
                    val title = (data["title"] as? String ?: "").lowercase()
                    val ward = (data["ward"] as? String ?: "").lowercase()
                    val district = (data["district"] as? String ?: "").lowercase()
                    val address = (data["address"] as? String ?: "").lowercase()
                    val description = (data["description"] as? String ?: "").lowercase()
                    title.contains(lowerQuery) || ward.contains(lowerQuery) ||
                    district.contains(lowerQuery) || address.contains(lowerQuery) ||
                    description.contains(lowerQuery)
                }.map { doc -> mapToRoomItem(doc.id, doc.data) }
                _searchResults.value = matched
            },
            onFailure = { e ->
                _isLoading.value = false
                _errorMessage.value = e
                _searchResults.value = emptyList()
            }
        )
    }

    fun searchByFilters(ward: String, district: String, searchMode: String, minPrice: Long, maxPrice: Long) {
        _isLoading.value = true
        val wardFilter = normalizeLocationValue(ward)
        val districtFilter = normalizeLocationValue(district)
        val mode = searchMode.trim().lowercase(Locale.ROOT)
        repository.searchApprovedRooms(
            onSuccess = { docs ->
                _isLoading.value = false
                val matched = docs.filter { doc ->
                    val data = doc.data
                    val roomPrice = data["price"] as? Long ?: 0
                    val roomWard = normalizeLocationValue(data["ward"] as? String ?: "")
                    val roomDistrict = normalizeLocationValue(data["district"] as? String ?: "")
                    val priceMatch = (minPrice == 0L || roomPrice >= minPrice) &&
                                     (maxPrice == 0L || roomPrice <= maxPrice)
                    val locationMatch = when {
                        mode == "district" && districtFilter.isNotEmpty() -> roomDistrict == districtFilter
                        mode == "ward" && wardFilter.isNotEmpty() -> roomWard == wardFilter
                        wardFilter.isNotEmpty() -> roomWard == wardFilter
                        districtFilter.isNotEmpty() -> roomDistrict == districtFilter
                        else -> true
                    }
                    priceMatch && locationMatch
                }.map { doc -> mapToRoomItem(doc.id, doc.data) }
                _searchResults.value = matched
            },
            onFailure = { e ->
                _isLoading.value = false
                _errorMessage.value = e
                _searchResults.value = emptyList()
            }
        )
    }

    private fun normalizeLocationValue(value: String): String {
        return value.trim()
            .replace(Regex("\\s+"), " ")
            .lowercase(Locale("vi", "VN"))
    }

    private fun mapToRoomItem(roomId: String, data: Map<String, Any>): RoomItem {
        return RoomItem(
            roomId = roomId,
            userId = data["userId"] as? String ?: "",
            title = data["title"] as? String ?: "",
            address = data["address"] as? String ?: "",
            ward = data["ward"] as? String ?: "",
            district = data["district"] as? String ?: "",
            firstImage = (data["imageUrls"] as? List<String>)?.firstOrNull() ?: "",
            price = data["price"] as? Long ?: 0,
            roomCount = (data["roomCount"] as? Long ?: 0).toInt(),
            rentedCount = (data["rentedCount"] as? Long ?: 0).toInt(),
            createdAt = data["createdAt"] as? Long ?: 0L
        )
    }
}
