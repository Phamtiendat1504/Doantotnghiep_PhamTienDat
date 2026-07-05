package com.example.doantotnghiep.View.Auth

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.animation.ObjectAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.GeoUtils
import com.example.doantotnghiep.View.Adapter.NearbyPostAdapter
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import java.util.concurrent.Executors

class LocationPickerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_INITIAL_ADDRESS  = "initial_address"
        const val EXTRA_INITIAL_WARD     = "initial_ward"
        const val EXTRA_INITIAL_DISTRICT = "initial_district"
        const val EXTRA_RESULT_ADDRESS   = "result_address"
        const val EXTRA_RESULT_LAT       = "result_lat"
        const val EXTRA_RESULT_LNG       = "result_lng"
        const val EXTRA_IS_STRICT        = "is_strict"
        // Kết quả tìm kiếm bản đồ
        const val EXTRA_SEARCH_MODE      = "search_mode"   // "exact_post" | "selected_posts" | "nearby"
        const val EXTRA_POST_ID          = "post_id"
        const val EXTRA_POST_IDS         = "post_ids"      // ArrayList<String>
        const val EXTRA_RADIUS_KM        = "radius_km"
        // Tọa độ GPS sẵn (chế độ "tìm phòng gần tôi")
        const val EXTRA_INITIAL_LAT       = "initial_lat"
        const val EXTRA_INITIAL_LNG       = "initial_lng"
        const val EXTRA_INITIAL_RADIUS_KM = "initial_radius_km"

        // Bước bán kính: 0.5–10.0 km (15 bước)
        private val RADIUS_STEPS = listOf(0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0)
        private const val DEFAULT_RADIUS_INDEX = 5   // 3.0 km
        private const val MAX_LOAD_RADIUS = 10.0     // Luôn tải 10km, filter client-side
        private const val MAX_VISIBLE_ITEMS = 4      // Chiều cao RecyclerView = max 4 items
        private const val ITEM_HEIGHT_DP = 64
    }

    // ── Views ──
    private lateinit var btnBack: ImageView
    private lateinit var btnSearchPlace: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnConfirm: MaterialButton
    private lateinit var tvPickedAddress: TextView
    private lateinit var tvNearbyCount: TextView
    private lateinit var tvMapHint: TextView

    // ── Panel views ──
    private lateinit var panelNearbyPosts: MaterialCardView
    private lateinit var tvPanelTitle: TextView
    private lateinit var cbPanelSelectAll: CheckBox
    private lateinit var seekBarPanelRadius: SeekBar
    private lateinit var tvPanelRadiusValue: TextView
    private lateinit var rvNearbyPosts: RecyclerView
    private lateinit var btnConfirmLocations: MaterialButton
    private lateinit var btnRefreshPanel: MaterialButton
    private lateinit var btnTogglePanel: ImageView
    private lateinit var btnExpandRadius: MaterialButton

    // ── Map ──
    private var googleMap: GoogleMap? = null
    private var selectedLatLng: LatLng? = null
    private var centerLatLng: LatLng? = null
    private var selectedAddress: String = ""
    private var selectedAddressObj: android.location.Address? = null
    private var isStrict: Boolean = false
    private val hanoiLatLng = LatLng(21.0285, 105.8542)
    private val geocodeExecutor = Executors.newSingleThreadExecutor()
    private var selectedMarker: Marker? = null
    private val postMarkers = mutableListOf<Marker>()

    // ── Panel state ──
    private lateinit var nearbyAdapter: NearbyPostAdapter
    private var allNearbyDocs: List<DocumentSnapshot> = emptyList()
    private var currentPanelRadius: Double = RADIUS_STEPS[DEFAULT_RADIUS_INDEX]
    private var refreshAnimator: ObjectAnimator? = null
    private var lastPanelItems: List<NearbyPostAdapter.PostItem> = emptyList()
    private var isPanelCollapsed: Boolean = false
    private var queryGeneration: Int = 0  // Ngăn stale callback ghi đè data mới hơn

    // Tọa độ GPS ban đầu (từ chế độ "tìm phòng gần tôi")
    private var initialLat: Double = Double.NaN
    private var initialLng: Double = Double.NaN
    private var initialRadiusKm: Double = Double.NaN

    // Danh sách từ khóa nhiễu (load từ JSON)
    private var noiseWordsList: List<String> = emptyList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_picker)

        isStrict        = intent.getBooleanExtra(EXTRA_IS_STRICT, false)
        initialLat      = intent.getDoubleExtra(EXTRA_INITIAL_LAT, Double.NaN)
        initialLng      = intent.getDoubleExtra(EXTRA_INITIAL_LNG, Double.NaN)
        initialRadiusKm = intent.getDoubleExtra(EXTRA_INITIAL_RADIUS_KM, Double.NaN)

        loadNoiseWords()
        bindViews()

        if (savedInstanceState != null) {
            val lat = savedInstanceState.getDouble("sel_lat", Double.NaN)
            val lng = savedInstanceState.getDouble("sel_lng", Double.NaN)
            if (!lat.isNaN() && !lng.isNaN()) selectedLatLng = LatLng(lat, lng)
            selectedAddress = savedInstanceState.getString("sel_address", "")
        }

        setupAdapter()
        setupPanel()
        setupMap()
        setupActions()

        if (savedInstanceState == null) applyInitialAddress()
        else renderPickedAddress()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectedLatLng?.let {
            outState.putDouble("sel_lat", it.latitude)
            outState.putDouble("sel_lng", it.longitude)
        }
        outState.putString("sel_address", selectedAddress)
    }

    override fun onDestroy() {
        super.onDestroy()
        geocodeExecutor.shutdownNow()
    }

    // ────────────────────────────────────────────────
    // Bind views
    // ────────────────────────────────────────────────
    private fun bindViews() {
        btnBack            = findViewById(R.id.btnBack)
        btnSearchPlace     = findViewById(R.id.btnSearchPlace)
        btnCancel          = findViewById(R.id.btnCancel)
        btnConfirm         = findViewById(R.id.btnConfirm)
        tvPickedAddress    = findViewById(R.id.tvPickedAddress)
        tvNearbyCount      = findViewById(R.id.tvNearbyCount)
        tvMapHint          = findViewById(R.id.tvMapHint)
        panelNearbyPosts    = findViewById(R.id.panelNearbyPosts)
        tvPanelTitle        = findViewById(R.id.tvPanelTitle)
        cbPanelSelectAll    = findViewById(R.id.cbPanelSelectAll)
        seekBarPanelRadius  = findViewById(R.id.seekBarPanelRadius)
        tvPanelRadiusValue  = findViewById(R.id.tvPanelRadiusValue)
        rvNearbyPosts       = findViewById(R.id.rvNearbyPosts)
        btnConfirmLocations = findViewById(R.id.btnConfirmLocations)
        btnRefreshPanel     = findViewById(R.id.btnRefreshPanel)
        btnTogglePanel      = findViewById(R.id.btnTogglePanel)
        btnExpandRadius     = findViewById(R.id.btnExpandRadius)
    }


    // Adapter
    private fun setupAdapter() {
        nearbyAdapter = NearbyPostAdapter { selectedCount ->
            // Cập nhật nút xác nhận
            btnConfirmLocations.isEnabled = selectedCount > 0
            btnConfirmLocations.text = if (selectedCount > 0)
                "Xác nhận vị trí ($selectedCount đã chọn)"
            else
                "Xác nhận vị trí"
            // Đồng bộ checkbox "Chọn tất cả"
            cbPanelSelectAll.isChecked = nearbyAdapter.areAllSelected()
        }
        rvNearbyPosts.layoutManager = LinearLayoutManager(this)
        rvNearbyPosts.adapter = nearbyAdapter
    }


    // Panel setup
    private fun setupPanel() {
        seekBarPanelRadius.max      = RADIUS_STEPS.size - 1
        seekBarPanelRadius.progress = DEFAULT_RADIUS_INDEX
        tvPanelRadiusValue.text     = formatRadius(currentPanelRadius)

        seekBarPanelRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentPanelRadius = RADIUS_STEPS[progress]
                tvPanelRadiusValue.text = formatRadius(currentPanelRadius)
            }
            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) {
                // Filter client-side — không cần query Firestore thêm
                applyPanelFilter()
            }
        })

        // "Chọn tất cả" dùng setOnClickListener để tránh loop khi set programmatic
        cbPanelSelectAll.setOnClickListener {
            nearbyAdapter.selectAll(cbPanelSelectAll.isChecked)
        }

        // Nút làm mới: chỉ dùng trong chế độ tìm kiếm, không hiển thị khi đăng bài
        if (isStrict) {
            btnRefreshPanel.visibility = View.GONE
        } else {
            btnRefreshPanel.setOnClickListener {
                val center = centerLatLng ?: return@setOnClickListener
                startRefreshAnimation()
                loadNearbyPostsFromFirestore(center)
            }
        }

        btnConfirmLocations.setOnClickListener { confirmPanelSelection() }

        btnTogglePanel.setOnClickListener { collapsePanel() }
    }


    // Tìm phòng trọ gần vị trí của tôi -1-
    // Cài đặt và khởi tạo bản đồ Google Maps
    private fun setupMap() {
        // Tìm Fragment chứa bản đồ trong giao diện XML (R.id.mapPickerContainer)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapPickerContainer) as? SupportMapFragment ?: return
            
        // getMapAsync: Đợi bản đồ tải xong (bất đồng bộ) rồi mới thực thi khối code bên trong
        mapFragment.getMapAsync { map ->
            googleMap = map
            
            // Kích hoạt các nút điều khiển cơ bản trên bản đồ
            map.uiSettings.isZoomControlsEnabled = true // Nút phóng to/thu nhỏ (+/-)
            map.uiSettings.isCompassEnabled      = true // La bàn chỉ hướng
            map.uiSettings.isMapToolbarEnabled   = true // Thanh công cụ bản đồ
            
            // Mặc định khi mới mở, đưa camera bản đồ về khu vực trung tâm Hà Nội (Zoom level 11)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(hanoiLatLng, 11f))
            
            // Nếu trước đó đã chọn một vị trí thì vẽ lại điểm đánh dấu (Marker) đó
            selectedLatLng?.let { updateMarker(it, false) }
            
            // Bắt sự kiện khi người dùng bấm chạm vào bất kỳ đâu trên bản đồ
            map.setOnMapClickListener { latLng ->
                selectedLatLng = latLng
                centerLatLng   = latLng
                hidePanel() // Ẩn danh sách phòng trọ cũ đi
                updateMarker(latLng, true) // Đặt cây kim đỏ (Marker) xuống vị trí vừa bấm
                reverseGeocode(latLng) // Đổi từ Tọa độ -> Tên đường (địa chỉ chữ)
            }

            // ĐÂY LÀ PHẦN DÀNH CHO TÍNH NĂNG "TÌM PHÒNG GẦN VỊ TRÍ CỦA TÔI"
            // Kiểm tra xem Activity trước (SearchFragment) có truyền GPS (initialLat, initialLng) sang không
            if (!initialLat.isNaN() && !initialLng.isNaN()) {
                // Tạo đối tượng Tọa độ từ GPS cá nhân
                val latLng = LatLng(initialLat, initialLng)
                
                // Lấy tọa độ đó làm tâm điểm để tìm kiếm
                centerLatLng    = latLng
                selectedLatLng  = latLng
                selectedAddress = "Vị trí hiện tại của tôi"
                renderPickedAddress() // Hiển thị chữ "Vị trí hiện tại của tôi" lên màn hình
                
                // Xử lý Bán kính (Radius) được truyền từ SearchFragment sang
                if (!initialRadiusKm.isNaN()) {
                    // Tìm vị trí của bán kính này trên thanh kéo (SeekBar) và tự động kéo đến đó
                    val idx = RADIUS_STEPS.indexOfFirst { it >= initialRadiusKm }.coerceAtLeast(0)
                    currentPanelRadius = RADIUS_STEPS[idx]
                    seekBarPanelRadius.progress = idx
                    tvPanelRadiusValue.text = formatRadius(currentPanelRadius)
                }
                
                // Vẽ tâm điểm vị trí cá nhân lên bản đồ
                updateSelectedMarkerForSearch(latLng)
                
                // Hiển thị hiệu ứng xoay (Loading)
                startRefreshAnimation()
                
                // [QUAN TRỌNG NHẤT]: Bắt đầu truy vấn Firebase để tìm các phòng trọ xung quanh vị trí GPS này
                loadNearbyPostsFromFirestore(latLng)
            }
        }
    }


    // Button listener
    private fun setupActions() {
        btnBack.setOnClickListener    { finish() }
        btnCancel.setOnClickListener  { finish() }
        btnSearchPlace.setOnClickListener { openSearchDialog() }
        btnConfirm.setOnClickListener { confirmSelection() }
    }


    // Tìm địa chỉ qua Geocoder
    // Nhóm 1 — Địa điểm phổ biến (chip ngắn, cuộn ngang)
    private val locationSuggestions = listOf(
        "Đại học Thủy Lợi",
        "Cầu Giấy",
        "Hà Đông",
        "Đống Đa",
        "Thanh Xuân",
        "Hoàng Mai",
        "Đại học Bách Khoa",
        "Đại học Quốc Gia"
    )

    // Nhóm 2 — Dạng câu tìm kiếm (chip dài hơn, wrap nhiều dòng)
    // Mỗi item: Pair(text hiển thị trên chip, text điền vào ô tìm kiếm)
    private val sentenceSuggestions = listOf(
        Pair("Tìm phòng gần Đại học Thủy Lợi",          "Tìm phòng gần Đại học Thủy Lợi"),
        Pair("Phòng trọ quanh khu vực Hà Đông",           "Phòng trọ quanh khu vực Hà Đông"),
        Pair("Tìm trọ gần khu vực Cầu Giấy",              "Tìm trọ gần khu vực Cầu Giấy"),
        Pair("Muốn thuê phòng gần Đống Đa",               "Muốn thuê phòng gần Đống Đa"),
        Pair("Phòng trọ quanh 175 Tây Sơn, Đống Đa",     "175 Tây Sơn, Đống Đa")
    )

    private fun openSearchDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_search_address, null)
        val edtSearch  = dialogView.findViewById<EditText>(R.id.edtSearchQuery)

        val dialog = AlertDialog.Builder(this, R.style.RoundedDialogStyle)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Hàm tạo chip dùng chung
        fun makeChip(label: String, fillText: String): Chip = Chip(this).apply {
            text = label
            isClickable  = true
            isCheckable  = false
            chipBackgroundColor = ContextCompat.getColorStateList(this@LocationPickerActivity, R.color.chip_bg_color)
            setTextColor(ContextCompat.getColor(this@LocationPickerActivity, R.color.primary))
            setOnClickListener {
                edtSearch.setText(fillText)
                edtSearch.setSelection(fillText.length)
            }
        }

        // Nhóm 1: địa điểm ngắn
        val chipGroupLocations = dialogView.findViewById<ChipGroup>(R.id.chipGroupSuggestions)
        locationSuggestions.forEach { loc ->
            chipGroupLocations.addView(makeChip(loc, loc))
        }

        // Nhóm 2: dạng câu ví dụ
        val chipGroupSentences = dialogView.findViewById<ChipGroup>(R.id.chipGroupSentences)
        sentenceSuggestions.forEach { (label, fillText) ->
            chipGroupSentences.addView(makeChip(label, fillText))
        }

        val doSearch = {
            val raw = edtSearch.text.toString().trim()
            if (raw.isNotEmpty()) { dialog.dismiss(); geocodeAndShow(extractLocation(raw)) }
            else edtSearch.error = "Vui lòng nhập địa chỉ"
        }

        dialogView.findViewById<MaterialButton>(R.id.btnDialogCancel)
            .setOnClickListener { dialog.dismiss() }

        dialogView.findViewById<MaterialButton>(R.id.btnDialogSearch)
            .setOnClickListener { doSearch() }

        edtSearch.setOnEditorActionListener { _, _, _ -> doSearch(); true }

        dialog.show()
        edtSearch.requestFocus()
    }

    @Suppress("DEPRECATION")
    private fun geocodeAndShow(query: String) {
        val fullQuery = if (query.contains("Ha Noi", ignoreCase = true) ||
            query.contains("Hanoi", ignoreCase = true) ||
            query.contains("Hà Nội", ignoreCase = true))
            query else "$query, Hà Nội, Việt Nam"

        geocodeExecutor.execute {
            val geocoder = Geocoder(this, Locale("vi", "VN"))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                runCatching {
                    geocoder.getFromLocationName(fullQuery, 5) { handleGeocodeResults(it) }
                }.onFailure { handleGeocodeResults(emptyList()) }
                return@execute
            }
            val results = try {
                geocoder.getFromLocationName(fullQuery, 5) ?: emptyList()
            } catch (_: Exception) { emptyList() }
            handleGeocodeResults(results)
        }
    }

    private fun handleGeocodeResults(results: List<android.location.Address>) {
        if (isFinishing || isDestroyed) return
        runOnUiThread {
            when {
                results.isEmpty() -> Toast.makeText(
                    this, "Không tìm thấy. Hãy thử từ khóa khác.", Toast.LENGTH_SHORT
                ).show()
                results.size == 1 -> applyGeocoderResult(results.first())
                else              -> showResultPicker(results)
            }
        }
    }

    private fun showResultPicker(results: List<android.location.Address>) {
        val labels = results.map { addr ->
            (0..addr.maxAddressLineIndex).joinToString(", ") { addr.getAddressLine(it) }
        }
        AlertDialog.Builder(this)
            .setTitle("Chọn địa chỉ")
            .setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, labels)) { _, idx ->
                applyGeocoderResult(results[idx])
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun applyGeocoderResult(addr: android.location.Address) {
        val latLng = LatLng(addr.latitude, addr.longitude)
        selectedLatLng     = latLng
        centerLatLng       = latLng
        selectedAddressObj = addr
        selectedAddress    = (0..addr.maxAddressLineIndex)
            .joinToString(", ") { addr.getAddressLine(it) }
        renderPickedAddress()
        updateSelectedMarkerForSearch(latLng)
        if (!isStrict) loadNearbyPostsFromFirestore(latLng)
    }

    @Suppress("DEPRECATION")
    private fun reverseGeocode(latLng: LatLng) {
        geocodeExecutor.execute {
            val geocoder = Geocoder(this, Locale("vi", "VN"))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) { results ->
                    if (!isFinishing && !isDestroyed) runOnUiThread {
                        val addrObj = results.firstOrNull()
                        selectedAddressObj = addrObj
                        val addr = addrObj?.let { a ->
                            (0..a.maxAddressLineIndex).joinToString(", ") { a.getAddressLine(it) }
                        }
                        if (!addr.isNullOrBlank()) selectedAddress = addr
                        renderPickedAddress()
                        updateMarker(latLng, false)
                    }
                }
            } else {
                val result = try {
                    geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                } catch (_: Exception) { null }
                if (!isFinishing && !isDestroyed) runOnUiThread {
                    val addrObj = result?.firstOrNull()
                    selectedAddressObj = addrObj
                    val addr = addrObj?.let { a ->
                        (0..a.maxAddressLineIndex).joinToString(", ") { a.getAddressLine(it) }
                    }
                    if (!addr.isNullOrBlank()) selectedAddress = addr
                    renderPickedAddress()
                    updateMarker(latLng, false)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun applyInitialAddress() {
        val initial  = intent.getStringExtra(EXTRA_INITIAL_ADDRESS).orEmpty().trim()
        val ward     = intent.getStringExtra(EXTRA_INITIAL_WARD).orEmpty().trim()
        val district = intent.getStringExtra(EXTRA_INITIAL_DISTRICT).orEmpty().trim()
        if (initial.isEmpty() && ward.isEmpty() && district.isEmpty()) return
        val full = buildString {
            if (initial.isNotEmpty())  append(initial).append(", ")
            if (ward.isNotEmpty())     append(ward).append(", ")
            if (district.isNotEmpty()) append(district).append(", ")
            append("Hà Nội")
        }
        geocodeExecutor.execute {
            val geocoder = Geocoder(this, Locale("vi", "VN"))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocationName(full, 1) { results ->
                    results.firstOrNull()?.let { addr ->
                        val latLng = LatLng(addr.latitude, addr.longitude)
                        if (!isFinishing && !isDestroyed) runOnUiThread {
                            selectedLatLng     = latLng
                            centerLatLng       = latLng
                            selectedAddressObj = addr
                            selectedAddress    = addr.getAddressLine(0) ?: full
                            renderPickedAddress()
                            updateMarker(latLng, true)
                        }
                    }
                }
            } else {
                val result = try {
                    geocoder.getFromLocationName(full, 1)
                } catch (_: Exception) { null }
                result?.firstOrNull()?.let { addr ->
                    val latLng = LatLng(addr.latitude, addr.longitude)
                    if (!isFinishing && !isDestroyed) runOnUiThread {
                        selectedLatLng     = latLng
                        centerLatLng       = latLng
                        selectedAddressObj = addr
                        selectedAddress    = addr.getAddressLine(0) ?: full
                        renderPickedAddress()
                        updateMarker(latLng, true)
                    }
                }
            }
        }
    }


    // Tìm kiếm phòng trọ gần vị trí của tôi -2-
    // Hàm nòng cốt: Kéo dữ liệu phòng trọ từ Firebase dựa trên vùng không gian (Bounding Box)
    private fun loadNearbyPostsFromFirestore(center: LatLng) {
        // Dọn dẹp bản đồ: Xóa các mốc (Marker) và dữ liệu cũ trước khi tìm kiếm mới
        postMarkers.forEach { it.remove() }
        postMarkers.clear()
        allNearbyDocs = emptyList()
        hidePanel() // Ẩn danh sách kết quả cũ

        // Hiển thị trạng thái đang tải dữ liệu cho người dùng biết
        tvNearbyCount.text       = "Đang tìm phòng trọ gần đây..."
        tvNearbyCount.visibility = View.VISIBLE
        tvNearbyCount.setOnClickListener(null)

        // Cờ (Flag) an toàn: Đánh dấu lượt truy vấn hiện tại. Nếu người dùng spam bấm liên tục, 
        // app chỉ nhận kết quả của lượt bấm cuối cùng, bỏ qua các kết quả tải chậm của lượt trước.
        val myGen = ++queryGeneration

        // --- BƯỚC 1: TỐI ƯU TRUY VẤN (BOUNDING BOX) ---
        // Thay vì tải toàn bộ phòng trọ ở Việt Nam về, ta tính toán vĩ độ giới hạn (minLat, maxLat).
        // MAX_LOAD_RADIUS mặc định là 10km. Ứng dụng chỉ tải các phòng trong phạm vi hình vuông 10km này.
        val latDelta = GeoUtils.latDelta(MAX_LOAD_RADIUS)
        val minLat   = center.latitude - latDelta
        val maxLat   = center.latitude + latDelta

        // Gọi lên Firestore Database để lấy danh sách phòng trọ
        FirebaseFirestore.getInstance()
            .collection("rooms") // Vào bảng "rooms"
            .whereEqualTo("status", "approved") // Chỉ lấy phòng đã được Admin duyệt
            .whereGreaterThanOrEqualTo("latitude", minLat) // Cắt bỏ các phòng nằm tuốt ở phía Nam xa xôi
            .whereLessThanOrEqualTo("latitude", maxLat) // Cắt bỏ các phòng nằm tuốt ở phía Bắc xa xôi
            .get()
            .addOnSuccessListener { snapshot ->
                // Nếu người dùng đã thoát màn hình thì ngưng xử lý để tránh crash app
                if (isFinishing || isDestroyed) return@addOnSuccessListener
                // Bỏ qua kết quả này nếu người dùng đã thao tác tìm kiếm cái mới (nhờ cờ queryGeneration)
                if (myGen != queryGeneration) return@addOnSuccessListener

                // --- BƯỚC 2: LỌC CHÍNH XÁC (HAVERSINE FORMULA) ---
                // Firestore ở trên chỉ lọc được theo hình vuông (Bounding Box) dư ra ở 4 góc.
                // Ở đây ta dùng thuật toán Haversine để đo khoảng cách cong thực tế trên mặt đất.
                allNearbyDocs = snapshot.documents.filter { doc ->
                    val lat = doc.getDouble("latitude") ?: return@filter false
                    val lng = doc.getDouble("longitude") ?: return@filter false
                    // Chỉ giữ lại những phòng thực sự nằm lọt thỏm trong VÒNG TRÒN bán kính 10km
                    GeoUtils.haversineKm(center.latitude, center.longitude, lat, lng) <= MAX_LOAD_RADIUS
                }

                // Ẩn chữ đang tải
                tvNearbyCount.visibility = View.GONE
                
                // Bắt đầu hiển thị danh sách phòng vừa lọc được lên màn hình và cắm mốc lên bản đồ
                applyPanelFilter()
            }
            .addOnFailureListener { e ->
                // Xử lý báo lỗi nếu truy vấn thất bại (thường do chưa cấu hình Index trên Firebase)
                if (!isFinishing && !isDestroyed && myGen == queryGeneration) {
                    val msg = e.message ?: ""
                    tvNearbyCount.text = if (msg.contains("index", ignoreCase = true) ||
                        msg.contains("FAILED_PRECONDITION", ignoreCase = true))
                        "Chưa cấu hình Firestore Index. Vào Firebase Console → Firestore → Indexes → thêm Composite Index: collection 'rooms', fields: status (Ascending) + latitude (Ascending)."
                    else
                        "Lỗi tải dữ liệu: $msg"
                    tvNearbyCount.visibility = View.VISIBLE
                }
            }
    }


    // TÌm kiếm phòng trọ gần vị trí của tôi -3-
    // Hàm xử lý việc lọc danh sách phòng trọ ở phía Client (điện thoại) và hiển thị lên giao diện
    private fun applyPanelFilter() {
        // Lấy tâm điểm bản đồ (chính là vị trí GPS của người dùng). Nếu chưa có thì thoát hàm.
        val center = centerLatLng ?: return

        // Dọn dẹp bản đồ: Xóa sạch các cây kim đỏ (Marker) của lượt tìm kiếm/lượt kéo thanh trượt trước đó
        postMarkers.forEach { it.remove() }
        postMarkers.clear()

        // Bắt đầu duyệt qua danh sách các phòng trọ (allNearbyDocs) đã lấy được từ Firebase ở hàm trên
        val filtered = allNearbyDocs.mapNotNull { doc ->
            // Bóc tách tọa độ Vĩ độ, Kinh độ của từng phòng trọ
            val lat  = doc.getDouble("latitude")  ?: return@mapNotNull null
            val lng  = doc.getDouble("longitude") ?: return@mapNotNull null
            
            // Dùng thuật toán Haversine đo khoảng cách thực tế từ vị trí người dùng (center) đến phòng trọ
            val dist = GeoUtils.haversineKm(center.latitude, center.longitude, lat, lng)
            
            // Nếu khoảng cách này xa hơn Bán kính mà người dùng chọn (VD: lớn hơn 2km) -> Loại bỏ phòng này
            if (dist > currentPanelRadius) return@mapNotNull null

            // Nếu phòng nằm trong bán kính, tiến hành lấy thêm thông tin để hiển thị
            val address  = doc.getString("address")  ?: "Chưa có địa chỉ"
            val price    = doc.getLong("price") ?: 0L
            
            // Định dạng giá tiền (VD: 2000000 -> 2,000,000 đ/tháng)
            val snippet  = if (price > 0) "${String.format("%,d", price)} đ/tháng" else "Liên hệ"

            // Tạo và cắm một cây kim đỏ (Marker) lên bản đồ Google Maps cho phòng trọ này
            val marker = googleMap?.addMarker(
                MarkerOptions()
                    .position(LatLng(lat, lng)) // Tọa độ đặt kim
                    .title(address) // Tiêu đề khi bấm vào kim
                    .snippet(snippet) // Dòng chữ nhỏ (giá tiền) hiện dưới tiêu đề
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)) // Màu đỏ
            )
            // Lưu Marker lại vào danh sách để sau này có thể xóa đi nếu cần
            marker?.let { postMarkers.add(it) }

            // Chuyển đổi dữ liệu từ Firebase Document sang dạng Object (PostItem) để nhét vào Adapter (Danh sách RecyclerView ở dưới đáy màn hình)
            NearbyPostAdapter.PostItem(
                id          = doc.id,
                address     = address,
                ward        = doc.getString("ward")     ?: "",
                district    = doc.getString("district") ?: "",
                price       = price,
                lat         = lat,
                lng         = lng,
                distanceKm  = dist
            )
        }.sortedBy { it.distanceKm } // Quan trọng: Sắp xếp danh sách trả về theo thứ tự từ GẦN NHẤT đến XA NHẤT

        // Gửi danh sách đã lọc và sắp xếp xuống hàm updatePanel để vẽ danh sách (RecyclerView) lên màn hình
        updatePanel(filtered)
    }


    // Cập nhật UI panel
    private fun updatePanel(items: List<NearbyPostAdapter.PostItem>) {
        stopRefreshAnimation()
        lastPanelItems   = items
        isPanelCollapsed = false

        if (items.isEmpty()) {
            // Giữ panel hiển thị để người dùng có thể kéo SeekBar hoặc nhấn mở rộng
            tvNearbyCount.visibility      = View.GONE
            tvPanelTitle.text             = "Không tìm thấy phòng trọ trong ${formatRadius(currentPanelRadius)}"
            nearbyAdapter.submitList(emptyList())
            cbPanelSelectAll.isChecked    = false
            cbPanelSelectAll.visibility   = View.GONE
            btnConfirmLocations.isEnabled = false
            rvNearbyPosts.layoutParams    = rvNearbyPosts.layoutParams.also { it.height = 0 }
            btnTogglePanel.rotation       = 0f
            panelNearbyPosts.visibility   = View.VISIBLE
            tvMapHint.visibility          = View.GONE

            // Hiện nút mở rộng nếu chưa đến bán kính tối đa
            val currentIdx = RADIUS_STEPS.indexOf(currentPanelRadius)
            if (currentIdx < RADIUS_STEPS.size - 1) {
                val nextRadius = RADIUS_STEPS[currentIdx + 1]
                btnExpandRadius.text = "Mở rộng lên ${formatRadius(nextRadius)}"
                btnExpandRadius.visibility = View.VISIBLE
                btnExpandRadius.setOnClickListener {
                    val newIdx = currentIdx + 1
                    seekBarPanelRadius.progress = newIdx
                    currentPanelRadius = RADIUS_STEPS[newIdx]
                    tvPanelRadiusValue.text = formatRadius(currentPanelRadius)
                    btnExpandRadius.visibility = View.GONE
                    // allNearbyDocs đã chứa đủ 10km — chỉ cần lọc lại client-side
                    applyPanelFilter()
                }
            } else {
                btnExpandRadius.visibility = View.GONE
            }
            return
        }

        btnExpandRadius.visibility = View.GONE

        tvNearbyCount.visibility = View.GONE
        tvNearbyCount.setOnClickListener(null)
        tvPanelTitle.text = "${items.size} phòng trọ trong ${formatRadius(currentPanelRadius)}"
        cbPanelSelectAll.visibility = View.VISIBLE

        nearbyAdapter.submitList(items)
        cbPanelSelectAll.isChecked = false

        // Giới hạn chiều cao RecyclerView để panel không che quá nhiều bản đồ
        val itemHeightPx = (ITEM_HEIGHT_DP * resources.displayMetrics.density).toInt()
        val desiredHeight = minOf(items.size, MAX_VISIBLE_ITEMS) * itemHeightPx
        rvNearbyPosts.layoutParams = rvNearbyPosts.layoutParams.also {
            it.height = desiredHeight
        }

        btnTogglePanel.rotation      = 0f
        panelNearbyPosts.visibility  = View.VISIBLE
        tvMapHint.visibility         = View.GONE
    }

    private fun hidePanel() {
        panelNearbyPosts.visibility = View.GONE
        tvMapHint.visibility        = View.VISIBLE
        tvNearbyCount.setOnClickListener(null)
        nearbyAdapter.submitList(emptyList())
        cbPanelSelectAll.isChecked  = false
        lastPanelItems   = emptyList()
        isPanelCollapsed = false
        stopRefreshAnimation()
    }


    // Ẩn / hiện panel (toggle)

    private fun collapsePanel() {
        if (isPanelCollapsed || lastPanelItems.isEmpty()) return
        isPanelCollapsed = true

        // Xoay icon mũi tên lên 180° (▼ → ▲)
        ObjectAnimator.ofFloat(btnTogglePanel, "rotation", 0f, 180f).apply {
            duration     = 200
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        panelNearbyPosts.animate().alpha(0f).setDuration(180).withEndAction {
            panelNearbyPosts.visibility = View.GONE
            panelNearbyPosts.alpha      = 1f

            // Badge cho phép người dùng mở lại panel
            val count = lastPanelItems.size
            tvNearbyCount.text = "▲  $count phòng trong ${formatRadius(currentPanelRadius)} — Nhấn để xem lại"
            tvNearbyCount.visibility = View.VISIBLE
            tvNearbyCount.setOnClickListener { expandPanel() }
            tvMapHint.visibility = View.GONE
        }.start()
    }

    private fun expandPanel() {
        if (!isPanelCollapsed || lastPanelItems.isEmpty()) return
        isPanelCollapsed = false

        tvNearbyCount.visibility = View.GONE
        tvNearbyCount.setOnClickListener(null)
        tvMapHint.visibility = View.GONE

        // Xoay icon mũi tên về 0° (▲ → ▼)
        ObjectAnimator.ofFloat(btnTogglePanel, "rotation", 180f, 0f).apply {
            duration     = 200
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        panelNearbyPosts.alpha      = 0f
        panelNearbyPosts.visibility = View.VISIBLE
        panelNearbyPosts.animate().alpha(1f).setDuration(180).start()
    }

    private fun startRefreshAnimation() {
        btnRefreshPanel.isEnabled = false
        refreshAnimator?.cancel()
        refreshAnimator = ObjectAnimator.ofFloat(btnRefreshPanel, "rotation", 0f, 360f).apply {
            duration       = 700
            repeatCount    = ObjectAnimator.INFINITE
            interpolator   = LinearInterpolator()
            start()
        }
    }

    private fun stopRefreshAnimation() {
        refreshAnimator?.cancel()
        refreshAnimator = null
        btnRefreshPanel.rotation  = 0f
        btnRefreshPanel.isEnabled = true
    }

    // Xác nhận chọn từ panel (multi-select)
    private fun confirmPanelSelection() {
        val selected = nearbyAdapter.getSelectedItems()
        if (selected.isEmpty()) return

        val intent = Intent()
        if (selected.size == 1) {
            val item = selected.first()
            intent.putExtra(EXTRA_RESULT_LAT,     item.lat)
            intent.putExtra(EXTRA_RESULT_LNG,     item.lng)
            intent.putExtra(EXTRA_RESULT_ADDRESS, buildFullAddress(item))
            intent.putExtra(EXTRA_SEARCH_MODE,    "exact_post")
            intent.putExtra(EXTRA_POST_ID,        item.id)
        } else {
            val first       = selected.first()
            val firstAddr   = buildFullAddress(first).ifBlank { first.address }
            val remaining   = selected.size - 1
            val displayAddr = "$firstAddr và $remaining địa chỉ khác"
            intent.putExtra(EXTRA_RESULT_LAT,     first.lat)
            intent.putExtra(EXTRA_RESULT_LNG,     first.lng)
            intent.putExtra(EXTRA_RESULT_ADDRESS, displayAddr)
            intent.putExtra(EXTRA_SEARCH_MODE,    "selected_posts")
            intent.putStringArrayListExtra(EXTRA_POST_IDS, ArrayList(selected.map { it.id }))
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }


    // Xác nhận vị trí (nút dưới cùng — tap thẳng bản đồ hoặc tìm khu vực)
    private fun confirmSelection() {
        val latLng = selectedLatLng ?: run {
            Toast.makeText(this, "Vui lòng chọn vị trí trên bản đồ", Toast.LENGTH_SHORT).show()
            return
        }

        if (isStrict && isAddressTooGeneral(selectedAddress, selectedAddressObj)) {
            AlertDialog.Builder(this)
                .setTitle("Địa chỉ chưa cụ thể")
                .setMessage("Vui lòng nhập hoặc chọn vị trí chi tiết hơn (bao gồm số nhà, ngõ hoặc tên đường cụ thể) để người thuê dễ dàng tìm thấy phòng trọ của bạn.")
                .setPositiveButton("Đã hiểu", null)
                .show()
            return
        }

        // Dùng centerLatLng + bán kính SeekBar hiện tại cho cả 2 trường hợp
        val center = if (panelNearbyPosts.visibility == View.VISIBLE) centerLatLng ?: latLng else latLng
        val (resultLat, resultLng, radius) = Triple(center.latitude, center.longitude, currentPanelRadius)

        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(EXTRA_RESULT_LAT,     resultLat)
            putExtra(EXTRA_RESULT_LNG,     resultLng)
            putExtra(EXTRA_RESULT_ADDRESS, selectedAddress)
            putExtra(EXTRA_SEARCH_MODE,    "nearby")
            putExtra(EXTRA_RADIUS_KM,      radius)
        })
        finish()
    }


    // Helpers - Map markers

    private fun updateMarker(latLng: LatLng, animate: Boolean) {
        val map = googleMap ?: return
        postMarkers.clear()
        map.clear()
        selectedMarker = map.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(if (selectedAddress.isNotBlank()) selectedAddress else "Vị trí đã chọn")
        )
        val update = CameraUpdateFactory.newLatLngZoom(latLng, 16f)
        if (animate) map.animateCamera(update) else map.moveCamera(update)
    }

    private fun updateSelectedMarkerForSearch(latLng: LatLng) {
        val map = googleMap ?: return
        selectedMarker?.remove()
        selectedMarker = map.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(if (selectedAddress.isNotBlank()) selectedAddress else "Vị trí đã chọn")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        )
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }

    private fun renderPickedAddress() {
        tvPickedAddress.text =
            if (selectedAddress.isNotBlank()) selectedAddress else "Chưa chọn vị trí"
    }


    // Helpers - Format & Haversine

    private fun formatRadius(km: Double): String =
        if (km == km.toLong().toDouble()) "${km.toInt()} km" else "$km km"

    private fun buildFullAddress(item: NearbyPostAdapter.PostItem): String =
        listOf(item.address, item.ward, item.district)
            .filter { it.isNotBlank() }.joinToString(", ")

    private fun isAddressTooGeneral(address: String, addrObj: android.location.Address?): Boolean {
        if (addrObj != null) {
            if (!addrObj.thoroughfare.isNullOrBlank() || !addrObj.subThoroughfare.isNullOrBlank())
                return false
        }
        val addressLower = address.lowercase(Locale.getDefault())
        val specificKeywords = listOf(
            "số ", "ngõ", "ngách", "hẻm", "kiệt", "thôn", "ấp", "đường", "phố",
            "tổ", "khu", "sn ", "bản", "đội"
        )
        return specificKeywords.none { addressLower.contains(it) }
    }

    private fun loadNoiseWords() {
        try {
            val inputStream = assets.open("noise_words.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val json = String(buffer, Charsets.UTF_8)
            val jsonArray = org.json.JSONArray(json)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            noiseWordsList = list.sortedByDescending { it.length }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback nếu lỗi đọc file
            noiseWordsList = listOf("tìm phòng trọ", "khu vực", "gần")
        }
    }

    private fun extractLocation(input: String): String {
        var result = input.trim()
        noiseWordsList.forEach { word ->
            result = result.replace(word, "", ignoreCase = true)
        }
        return result.trim().ifBlank { input.trim() }
    }

}
