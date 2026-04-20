package com.example.doantotnghiep.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doantotnghiep.View.Adapter.RoomItem
import com.example.doantotnghiep.repository.RoomRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot

class HomeViewModel : ViewModel() {

    private val repository = RoomRepository()

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _greeting = MutableLiveData<String>()
    val greeting: LiveData<String> = _greeting

    private val _currentDate = MutableLiveData<String>()
    val currentDate: LiveData<String> = _currentDate

    private val _popularAreas = MutableLiveData<List<Pair<String, Int>>>()
    val popularAreas: LiveData<List<Pair<String, Int>>> = _popularAreas

    private val _featuredRooms = MutableLiveData<List<RoomItem>>()
    val featuredRooms: LiveData<List<RoomItem>> = _featuredRooms

    private val _newRooms = MutableLiveData<List<RoomItem>>()
    val newRooms: LiveData<List<RoomItem>> = _newRooms

    private val _isLoadingFeatured = MutableLiveData<Boolean>()
    val isLoadingFeatured: LiveData<Boolean> = _isLoadingFeatured

    private val _isLoadingNew = MutableLiveData<Boolean>()
    val isLoadingNew: LiveData<Boolean> = _isLoadingNew

    private val _hasMoreRooms = MutableLiveData<Boolean>()
    val hasMoreRooms: LiveData<Boolean> = _hasMoreRooms

    private val _isLoadingMore = MutableLiveData<Boolean>()
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    private val _notificationBadgeCount = MutableLiveData<Int>()
    val notificationBadgeCount: LiveData<Int> = _notificationBadgeCount

    private var notificationListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var lastNewRoomDoc: DocumentSnapshot? = null
    private var lastLoadTime = 0L
    private val REFRESH_INTERVAL = 60 * 60 * 1000L
    private val NEW_ROOMS_PAGE_SIZE = 10L

    fun loadUserName() {
        updateGreetingAndDate()
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            _userName.value = "Khách"
            return
        }
        repository.loadUserName(uid) { name -> _userName.value = name }
    }

    private fun updateGreetingAndDate() {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        _greeting.value = when (hour) {
            in 5..11 -> "CHÀO BUỔI SÁNG,"
            in 12..17 -> "CHÀO BUỔI CHIỀU,"
            else -> "CHÀO BUỔI TỐI,"
        }

        val sdf = java.text.SimpleDateFormat("EEEE, dd/MM/yyyy", java.util.Locale("vi", "VN"))
        _currentDate.value = sdf.format(calendar.time)
    }

    fun loadPopularAreas() {
        repository.loadPopularAreas { areas -> _popularAreas.value = areas }
    }

    fun loadFeaturedRooms() {
        _isLoadingFeatured.value = true
        repository.loadFeaturedRooms { featuredDocs ->
            @Suppress("UNCHECKED_CAST")
            val featuredList = featuredDocs.map { doc ->
                RoomItem(
                    id = doc.id,
                    title = doc.getString("title") ?: "",
                    price = doc.getLong("price") ?: 0,
                    ward = doc.getString("ward") ?: "",
                    district = doc.getString("district") ?: "",
                    area = doc.getLong("area")?.toInt() ?: 0,
                    imageUrl = (doc.get("imageUrls") as? List<String>)?.firstOrNull()
                )
            }.toMutableList()

            if (featuredList.size >= 3) {
                _isLoadingFeatured.value = false
                _featuredRooms.value = featuredList
            } else {
                val existingIds = featuredList.map { it.id }.toSet()
                val needed = 3 - featuredList.size
                repository.loadApprovedRoomsPage(
                    startAfter = null,
                    limit = (needed + 5).toLong(),
                    onSuccess = { docs, _ ->
                        @Suppress("UNCHECKED_CAST")
                        val extras = docs
                            .filter { it.id !in existingIds }
                            .take(needed)
                            .map { doc ->
                                RoomItem(
                                    id = doc.id,
                                    title = doc.getString("title") ?: "",
                                    price = doc.getLong("price") ?: 0,
                                    ward = doc.getString("ward") ?: "",
                                    district = doc.getString("district") ?: "",
                                    area = doc.getLong("area")?.toInt() ?: 0,
                                    imageUrl = (doc.get("imageUrls") as? List<String>)?.firstOrNull()
                                )
                            }
                        featuredList.addAll(extras)
                        _isLoadingFeatured.value = false
                        _featuredRooms.value = featuredList
                    },
                    onFailure = {
                        _isLoadingFeatured.value = false
                        _featuredRooms.value = featuredList
                    }
                )
            }
        }
    }

    fun loadNewRooms(isRefresh: Boolean = false) {
        val currentList = _newRooms.value
        if (!isRefresh && !currentList.isNullOrEmpty()) return
        if (isRefresh && !currentList.isNullOrEmpty()) {
            val elapsed = System.currentTimeMillis() - lastLoadTime
            if (elapsed < REFRESH_INTERVAL) return
        }

        _isLoadingNew.value = true
        lastNewRoomDoc = null
        _hasMoreRooms.value = true

        repository.loadApprovedRoomsPage(
            startAfter = null,
            limit = NEW_ROOMS_PAGE_SIZE,
            onSuccess = { docs, lastDoc ->
                _isLoadingNew.value = false
                if (docs.isEmpty()) {
                    _newRooms.value = emptyList()
                    _hasMoreRooms.value = false
                    return@loadApprovedRoomsPage
                }
                lastLoadTime = System.currentTimeMillis()
                lastNewRoomDoc = lastDoc
                _hasMoreRooms.value = docs.size.toLong() == NEW_ROOMS_PAGE_SIZE
                @Suppress("UNCHECKED_CAST")
                _newRooms.value = docs.map { doc ->
                    RoomItem(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        price = doc.getLong("price") ?: 0,
                        ward = doc.getString("ward") ?: "",
                        district = doc.getString("district") ?: "",
                        area = doc.getLong("area")?.toInt() ?: 0,
                        imageUrl = (doc.get("imageUrls") as? List<String>)?.firstOrNull()
                    )
                }
            },
            onFailure = {
                _isLoadingNew.value = false
                _newRooms.value = emptyList()
            }
        )
    }

    fun loadMoreNewRooms() {
        val lastDoc = lastNewRoomDoc ?: return
        _isLoadingMore.value = true
        repository.loadApprovedRoomsPage(
            startAfter = lastDoc,
            limit = NEW_ROOMS_PAGE_SIZE,
            onSuccess = { docs, newLastDoc ->
                _isLoadingMore.value = false
                if (docs.isEmpty()) {
                    _hasMoreRooms.value = false
                    return@loadApprovedRoomsPage
                }
                lastNewRoomDoc = newLastDoc
                _hasMoreRooms.value = docs.size.toLong() == NEW_ROOMS_PAGE_SIZE
                @Suppress("UNCHECKED_CAST")
                val newItems = docs.map { doc ->
                    RoomItem(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        price = doc.getLong("price") ?: 0,
                        ward = doc.getString("ward") ?: "",
                        district = doc.getString("district") ?: "",
                        area = doc.getLong("area")?.toInt() ?: 0,
                        imageUrl = (doc.get("imageUrls") as? List<String>)?.firstOrNull()
                    )
                }
                val currentList = _newRooms.value?.toMutableList() ?: mutableListOf()
                currentList.addAll(newItems)
                _newRooms.value = currentList
            },
            onFailure = { _isLoadingMore.value = false }
        )
    }

    fun startNotificationListener() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        notificationListener?.remove()
        notificationListener = repository.listenUnseenNotificationCount(uid) { count ->
            _notificationBadgeCount.postValue(count)
        }
    }

    override fun onCleared() {
        super.onCleared()
        notificationListener?.remove()
    }

    fun loadNotificationBadge() {
        startNotificationListener()
    }
}
