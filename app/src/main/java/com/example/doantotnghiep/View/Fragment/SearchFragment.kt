package com.example.doantotnghiep.View.Fragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.AddressData
import com.example.doantotnghiep.Utils.NumberFormatUtils
import com.example.doantotnghiep.View.Auth.LocationPickerActivity
import com.example.doantotnghiep.View.Auth.SearchResultsActivity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip

class SearchFragment : Fragment() {

    private lateinit var chipPhuong: Chip
    private lateinit var chipXa: Chip
    private lateinit var autoArea: AutoCompleteTextView
    private lateinit var edtAddress: EditText
    private lateinit var autoCompletePrice: AutoCompleteTextView
    private lateinit var chipPriceCustom: Chip
    private lateinit var edtCustomPrice: EditText
    private lateinit var edtRoomArea: EditText
    private lateinit var edtPeopleCount: EditText
    private lateinit var cbWifi: CheckBox
    private lateinit var cbElectric: CheckBox
    private lateinit var cbWater: CheckBox
    private lateinit var edtWifiPrice: EditText
    private lateinit var edtElectricPrice: EditText
    private lateinit var edtWaterPrice: EditText
    private lateinit var layoutExtraAmenities: LinearLayout
    private lateinit var btnAddAmenity: MaterialButton
    private lateinit var rgRoomStyle: RadioGroup
    private lateinit var rgCurfew: RadioGroup
    private lateinit var edtCurfewTime: EditText
    private lateinit var btnSearch: MaterialButton

    private val priceOptions = listOf(
        "1 - 3 triệu",
        "3 - 6 triệu",
        "6 - 9 triệu",
        "9 - 12 triệu",
        "Trên 12 triệu"
    )

    // ── Map location picker ──
    private lateinit var btnPickMapLocation: MaterialButton
    private lateinit var btnUseMyLocation: MaterialButton
    private lateinit var layoutMapSelected: LinearLayout
    private lateinit var tvMapSelectedAddress: TextView
    private lateinit var btnClearMapLocation: ImageView

    // ── Location permission launcher ──
    private val locationPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            fetchCurrentLocationAndSearch()
        } else {
            Toast.makeText(requireContext(), "Cần cấp quyền vị trí để sử dụng tính năng này.", Toast.LENGTH_SHORT).show()
        }
    }

    // Lưu kết quả trả về từ LocationPickerActivity
    private var mapLat: Double = 0.0
    private var mapLng: Double = 0.0
    private var mapAddress: String = ""
    private var mapSearchMode: String = "nearby"            // "exact_post" | "selected_posts" | "nearby"
    private var mapPostId: String = ""                      // postId khi exact_post
    private var mapPostIds: ArrayList<String> = arrayListOf() // postIds khi selected_posts
    private var mapRadiusKm: Double = 2.0                   // bán kính khi nearby

    private val locationPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            mapLat        = data.getDoubleExtra(LocationPickerActivity.EXTRA_RESULT_LAT, 0.0)
            mapLng        = data.getDoubleExtra(LocationPickerActivity.EXTRA_RESULT_LNG, 0.0)
            mapAddress    = data.getStringExtra(LocationPickerActivity.EXTRA_RESULT_ADDRESS).orEmpty()
            mapSearchMode = data.getStringExtra(LocationPickerActivity.EXTRA_SEARCH_MODE) ?: "nearby"
            mapPostId     = data.getStringExtra(LocationPickerActivity.EXTRA_POST_ID).orEmpty()
            mapPostIds    = data.getStringArrayListExtra(LocationPickerActivity.EXTRA_POST_IDS) ?: arrayListOf()
            mapRadiusKm   = data.getDoubleExtra(LocationPickerActivity.EXTRA_RADIUS_KM, 2.0)
            applyMapSelection()
        }
    }

    private var currentAreaOptions: List<String> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chipPhuong = view.findViewById(R.id.chipPhuong)
        chipXa = view.findViewById(R.id.chipXa)
        autoArea = view.findViewById(R.id.autoArea)
        edtAddress = view.findViewById(R.id.edtAddress)
        autoCompletePrice = view.findViewById(R.id.autoCompletePrice)
        chipPriceCustom = view.findViewById(R.id.chipPriceCustom)
        edtCustomPrice = view.findViewById(R.id.edtCustomPrice)
        edtRoomArea = view.findViewById(R.id.edtRoomArea)
        edtPeopleCount = view.findViewById(R.id.edtPeopleCount)
        cbWifi = view.findViewById(R.id.cbWifi)
        cbElectric = view.findViewById(R.id.cbElectric)
        cbWater = view.findViewById(R.id.cbWater)
        edtWifiPrice = view.findViewById(R.id.edtWifiPrice)
        edtElectricPrice = view.findViewById(R.id.edtElectricPrice)
        edtWaterPrice = view.findViewById(R.id.edtWaterPrice)
        layoutExtraAmenities = view.findViewById(R.id.layoutExtraAmenities)
        btnAddAmenity = view.findViewById(R.id.btnAddAmenity)
        rgRoomStyle = view.findViewById(R.id.rgRoomStyle)
        rgCurfew = view.findViewById(R.id.rgCurfew)
        edtCurfewTime = view.findViewById(R.id.edtCurfewTime)
        btnSearch = view.findViewById(R.id.btnSearch)

        // Map location views
        btnUseMyLocation     = view.findViewById(R.id.btnUseMyLocation)
        btnPickMapLocation   = view.findViewById(R.id.btnPickMapLocation)
        layoutMapSelected    = view.findViewById(R.id.layoutMapSelected)
        tvMapSelectedAddress = view.findViewById(R.id.tvMapSelectedAddress)
        btnClearMapLocation  = view.findViewById(R.id.btnClearMapLocation)

        loadAreaOptions(AddressData.phuongList)
        setupAreaPickerBehavior()
        setupMapLocationPicker()
        setupMyLocationButton()
        setupPriceDropdown()

        chipPhuong.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) loadAreaOptions(AddressData.phuongList)
        }
        chipXa.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) loadAreaOptions(AddressData.xaList)
        }

        chipPriceCustom.setOnCheckedChangeListener { _, isChecked ->
            edtCustomPrice.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (isChecked) autoCompletePrice.setText("", false)
        }

        btnAddAmenity.setOnClickListener { addAmenityRow() }

        NumberFormatUtils.addFormatWatcher(edtCustomPrice)
        NumberFormatUtils.addFormatWatcher(edtWifiPrice)
        NumberFormatUtils.addFormatWatcher(edtElectricPrice)
        NumberFormatUtils.addFormatWatcher(edtWaterPrice)

        cbWifi.setOnCheckedChangeListener { _, isChecked ->
            edtWifiPrice.isEnabled = isChecked
            if (!isChecked) edtWifiPrice.text?.clear()
        }
        cbElectric.setOnCheckedChangeListener { _, isChecked ->
            edtElectricPrice.isEnabled = isChecked
            if (!isChecked) edtElectricPrice.text?.clear()
        }
        cbWater.setOnCheckedChangeListener { _, isChecked ->
            edtWaterPrice.isEnabled = isChecked
            if (!isChecked) edtWaterPrice.text?.clear()
        }

        rgCurfew.setOnCheckedChangeListener { _, checkedId ->
            edtCurfewTime.visibility = if (checkedId == R.id.rbCurfewCustom) View.VISIBLE else View.GONE
        }

        view.findViewById<MaterialButton>(R.id.btnResetFilter).setOnClickListener { resetFilters() }
        btnSearch.setOnClickListener { submitSearch() }
    }

    // ────────────────────────────────────────────────
    // Price dropdown setup
    // ────────────────────────────────────────────────
    private fun setupPriceDropdown() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            priceOptions
        )
        autoCompletePrice.setAdapter(adapter)
        autoCompletePrice.setOnClickListener { autoCompletePrice.showDropDown() }
        autoCompletePrice.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) autoCompletePrice.showDropDown()
        }
        autoCompletePrice.setOnItemClickListener { _, _, _, _ ->
            if (chipPriceCustom.isChecked) chipPriceCustom.isChecked = false
        }
    }

    // ────────────────────────────────────────────────
    // Dynamic amenity row
    // ────────────────────────────────────────────────
    private fun addAmenityRow() {
        val ctx = requireContext()
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = resources.getDimensionPixelSize(R.dimen.padding_small)
            }
        }

        val pad = resources.getDimensionPixelSize(R.dimen.corner_radius_10)
        val rowHeight = resources.getDimensionPixelSize(R.dimen.dimen_44dp)

        val nameInput = EditText(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, rowHeight, 1f)
            background = ContextCompat.getDrawable(ctx, R.drawable.bg_spinner_new)
            hint = "Tên tiện ích"
            inputType = InputType.TYPE_CLASS_TEXT
            setPadding(pad, 0, pad, 0)
            textSize = 14f
        }

        val priceInput = EditText(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, rowHeight, 1f).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.padding_small)
            }
            background = ContextCompat.getDrawable(ctx, R.drawable.bg_spinner_new)
            hint = "Giá/tháng (VNĐ)"
            inputType = InputType.TYPE_CLASS_NUMBER
            setPadding(pad, 0, pad, 0)
            textSize = 13f
        }
        NumberFormatUtils.addFormatWatcher(priceInput)

        val deleteBtn = ImageView(ctx).apply {
            val size = resources.getDimensionPixelSize(R.dimen.spacing_24)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.padding_small)
            }
            setImageResource(R.drawable.ic_close)
            imageTintList = ContextCompat.getColorStateList(ctx, R.color.text_secondary)
            setBackgroundResource(android.R.drawable.list_selector_background)
            setOnClickListener { layoutExtraAmenities.removeView(row) }
        }

        row.addView(nameInput)
        row.addView(priceInput)
        row.addView(deleteBtn)
        layoutExtraAmenities.addView(row)
    }

    // ────────────────────────────────────────────────
    // Map location picker setup
    // ────────────────────────────────────────────────
    private fun setupMapLocationPicker() {
        btnPickMapLocation.setOnClickListener {
            val intent = Intent(requireContext(), LocationPickerActivity::class.java).apply {
                putExtra(LocationPickerActivity.EXTRA_IS_STRICT, false)
            }
            locationPickerLauncher.launch(intent)
        }
        btnClearMapLocation.setOnClickListener { clearMapSelection() }
    }

    private fun applyMapSelection() {
        if (mapLat == 0.0 && mapLng == 0.0) return
        tvMapSelectedAddress.text = when {
            mapSearchMode == "exact_post" ->
                mapAddress.ifEmpty { "Vị trí đã chọn" }
            mapRadiusKm > 0 ->
                "${mapAddress.ifEmpty { "$mapLat, $mapLng" }} (trong ${mapRadiusKm.toInt()}km)"
            else ->
                mapAddress.ifEmpty { "Vị trí đã chọn: $mapLat, $mapLng" }
        }
        layoutMapSelected.visibility = View.VISIBLE
        btnPickMapLocation.text = "🗺 Thay đổi vị trí"
    }

    private fun clearMapSelection() {
        mapLat        = 0.0
        mapLng        = 0.0
        mapAddress    = ""
        mapSearchMode = "nearby"
        mapPostId     = ""
        mapPostIds    = arrayListOf()
        mapRadiusKm   = 2.0
        layoutMapSelected.visibility = View.GONE
        btnPickMapLocation.text = "Chọn vị trí trên bản đồ"
    }

    // ────────────────────────────────────────────────
    private fun setupAreaPickerBehavior() {
        autoArea.setOnClickListener { autoArea.showDropDown() }
        autoArea.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) autoArea.showDropDown()
        }
    }

    private fun submitSearch() {
        // ── Chế độ tìm theo bản đồ ──
        if (mapLat != 0.0 || mapLng != 0.0) {
            val intent = Intent(requireContext(), SearchResultsActivity::class.java)
            when {
                mapSearchMode == "exact_post" && mapPostId.isNotEmpty() -> {
                    intent.putExtra("searchMode", "exact_post")
                    intent.putExtra("postId", mapPostId)
                    intent.putExtra("mapAddress", mapAddress)
                }
                mapSearchMode == "selected_posts" && mapPostIds.isNotEmpty() -> {
                    intent.putExtra("searchMode", "selected_posts")
                    intent.putStringArrayListExtra("postIds", mapPostIds)
                    intent.putExtra("mapAddress", mapAddress)
                }
                else -> {
                    intent.putExtra("searchMode", "nearby")
                    intent.putExtra("lat", mapLat)
                    intent.putExtra("lng", mapLng)
                    intent.putExtra("radiusKm", mapRadiusKm)
                    intent.putExtra("mapAddress", mapAddress)
                    appendPriceAndAmenityExtras(intent)
                }
            }
            startActivity(intent)
            return
        }

        // ── Chế độ tìm theo phường/xã (logic cũ) ──
        val selectedItem = autoArea.text?.toString()?.trim().orEmpty()
        if (selectedItem.isEmpty()) {
            showInfoDialog(
                title = "Chưa chọn khu vực",
                message = "Vui lòng chọn khu vực để tìm kiếm hoặc chọn vị trí trên bản đồ."
            )
            return
        }

        if (selectedItem !in currentAreaOptions) {
            showInfoDialog(
                title = "Khu vực không hợp lệ",
                message = "Vui lòng chọn từ danh sách gợi ý."
            )
            return
        }

        val (wardName, districtName) = parseAreaSelection(selectedItem)
        val searchMode = "ward"

        val intent = Intent(requireContext(), SearchResultsActivity::class.java)
        intent.putExtra("ward", wardName)
        intent.putExtra("district", districtName)
        intent.putExtra("searchMode", searchMode)
        intent.putExtra("addressKeyword", edtAddress.text?.toString()?.trim().orEmpty())

        appendPriceAndAmenityExtras(intent)

        startActivity(intent)
    }

    /**
     * Đóng gói toàn bộ bộ lọc (giá, diện tích, loại phòng, tiện ích, giờ giới nghiêm)
     * vào Intent — dùng chung cho cả chế độ bản đồ lẫn phường/xã.
     */
    private fun appendPriceAndAmenityExtras(intent: Intent) {
        // ── Giá thuê ──
        var minPrice = 0L
        var maxPrice = 0L
        if (chipPriceCustom.isChecked && edtCustomPrice.text.isNotEmpty()) {
            val customPrice = edtCustomPrice.text.toString()
                .replace(".", "")
                .replace(",", "")
                .toLongOrNull() ?: 0L
            minPrice = (customPrice - 500_000L).coerceAtLeast(0L)
            maxPrice = customPrice + 500_000L
        } else {
            when (autoCompletePrice.text.toString()) {
                "1 - 3 triệu"    -> { minPrice = 1_000_000L;  maxPrice = 3_000_000L  }
                "3 - 6 triệu"    -> { minPrice = 3_000_000L;  maxPrice = 6_000_000L  }
                "6 - 9 triệu"    -> { minPrice = 6_000_000L;  maxPrice = 9_000_000L  }
                "9 - 12 triệu"   -> { minPrice = 9_000_000L;  maxPrice = 12_000_000L }
                "Trên 12 triệu"  -> { minPrice = 12_000_000L; maxPrice = 0L          }
            }
        }
        intent.putExtra("minPrice", minPrice)
        intent.putExtra("maxPrice", maxPrice)

        // ── Diện tích & số người ──
        val roomArea = edtRoomArea.text.toString().toIntOrNull() ?: 0
        if (roomArea > 0) {
            intent.putExtra("minArea", roomArea - 5)
            intent.putExtra("maxArea", roomArea + 5)
        } else {
            intent.putExtra("minArea", 0)
            intent.putExtra("maxArea", 0)
        }
        intent.putExtra("desiredPeople", edtPeopleCount.text.toString().toIntOrNull() ?: 0)

        // ── Loại phòng ──
        val roomType = when (rgRoomStyle.checkedRadioButtonId) {
            R.id.rbShared -> "Chung chủ"
            R.id.rbPrivate -> "Riêng chủ"
            else -> ""
        }
        intent.putExtra("roomType", roomType)

        // ── Tiện ích cố định ──
        intent.putExtra("hasWifi", cbWifi.isChecked)
        intent.putExtra("hasElectric", cbElectric.isChecked)
        intent.putExtra("hasWater", cbWater.isChecked)

        // ── Tiện ích bổ sung (người dùng tự thêm) ──
        val extraAmenities = ArrayList<String>()
        val extraAmenityPrices = ArrayList<Long>()
        for (i in 0 until layoutExtraAmenities.childCount) {
            val row = layoutExtraAmenities.getChildAt(i) as? LinearLayout ?: continue
            val nameEdt = row.getChildAt(0) as? EditText ?: continue
            val priceEdt = row.getChildAt(1) as? EditText ?: continue
            val name = nameEdt.text.toString().trim()
            if (name.isNotEmpty()) {
                extraAmenities.add(name)
                extraAmenityPrices.add(NumberFormatUtils.getRawNumber(priceEdt).toLongOrNull() ?: 0L)
            }
        }
        intent.putStringArrayListExtra("extraAmenities", extraAmenities)
        intent.putExtra("extraAmenityPrices", extraAmenityPrices.toLongArray())

        // ── Giá tiện ích tối đa ──
        val maxWifiPrice = NumberFormatUtils.getRawNumber(edtWifiPrice).toLongOrNull() ?: 0L
        val maxElectricPrice = NumberFormatUtils.getRawNumber(edtElectricPrice).toLongOrNull() ?: 0L
        val maxWaterPrice = NumberFormatUtils.getRawNumber(edtWaterPrice).toLongOrNull() ?: 0L
        intent.putExtra("maxWifiPrice", maxWifiPrice)
        intent.putExtra("maxElectricPrice", maxElectricPrice)
        intent.putExtra("maxWaterPrice", maxWaterPrice)

        // ── Giờ giới nghiêm ──
        val curfew = when (rgCurfew.checkedRadioButtonId) {
            R.id.rbCurfewFree -> "Tự do"
            R.id.rbCurfewCustom -> edtCurfewTime.text.toString().trim()
            else -> ""
        }
        intent.putExtra("curfew", curfew)
    }

    private fun parseAreaSelection(selectedItem: String): Pair<String, String> {
        val parenStart = selectedItem.indexOf('(')
        val parenEnd = selectedItem.indexOf(')')
        return if (parenStart > 0 && parenEnd > parenStart) {
            val wardName = selectedItem.substring(0, parenStart).trim()
            val districtName = selectedItem.substring(parenStart + 1, parenEnd).trim()
            Pair(wardName, districtName)
        } else {
            Pair(selectedItem, "")
        }
    }

    private fun showInfoDialog(title: String, message: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Đã hiểu", null)
            .show()
    }

    private fun resetFilters() {
        chipPhuong.isChecked = true
        loadAreaOptions(AddressData.phuongList)
        autoArea.setText("")
        edtAddress.text?.clear()

        autoCompletePrice.setText("", false)
        chipPriceCustom.isChecked = false
        edtCustomPrice.text?.clear()
        edtCustomPrice.visibility = View.GONE

        rgRoomStyle.clearCheck()
        edtRoomArea.text?.clear()
        edtPeopleCount.text?.clear()

        cbWifi.isChecked = false
        cbElectric.isChecked = false
        cbWater.isChecked = false
        edtWifiPrice.text?.clear()
        edtElectricPrice.text?.clear()
        edtWaterPrice.text?.clear()
        layoutExtraAmenities.removeAllViews()

        rgCurfew.clearCheck()
        edtCurfewTime.text?.clear()
        edtCurfewTime.visibility = View.GONE

        clearMapSelection()
    }

    // ────────────────────────────────────────────────
    // GPS — Tìm phòng gần vị trí hiện tại
    // ────────────────────────────────────────────────
    private fun setupMyLocationButton() {
        btnUseMyLocation.setOnClickListener {
            val fineGranted = ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val coarseGranted = ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (fineGranted || coarseGranted) {
                fetchCurrentLocationAndSearch()
            } else {
                locationPermLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun fetchCurrentLocationAndSearch() {
        val loadingDialog = AlertDialog.Builder(requireContext())
            .setMessage("Đang lấy vị trí của bạn...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        val client = LocationServices.getFusedLocationProviderClient(requireActivity())
        val cts = com.google.android.gms.tasks.CancellationTokenSource()
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { location ->
                loadingDialog.dismiss()
                if (location != null) {
                    showRadiusPickerDialog(location.latitude, location.longitude)
                } else {
                    // Fallback: thử lastLocation
                    client.lastLocation.addOnSuccessListener { last ->
                        if (last != null) {
                            showRadiusPickerDialog(last.latitude, last.longitude)
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Không lấy được vị trí. Hãy bật GPS và thử lại.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
            .addOnFailureListener {
                loadingDialog.dismiss()
                Toast.makeText(requireContext(), "Lỗi lấy vị trí: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showRadiusPickerDialog(lat: Double, lng: Double) {
        val options = arrayOf("1 km", "2 km", "5 km", "10 km")
        val radii = doubleArrayOf(1.0, 2.0, 5.0, 10.0)
        var selected = 1 // mặc định 2km

        AlertDialog.Builder(requireContext())
            .setTitle("Chọn bán kính tìm kiếm")
            .setSingleChoiceItems(options, selected) { _, which -> selected = which }
            .setPositiveButton("Tìm ngay") { dialog, _ ->
                dialog.dismiss()
                val radiusKm = radii[selected]
                val intent = Intent(requireContext(), SearchResultsActivity::class.java).apply {
                    putExtra("searchMode", "nearby")
                    putExtra("lat", lat)
                    putExtra("lng", lng)
                    putExtra("radiusKm", radiusKm)
                    putExtra("mapAddress", "Vị trí hiện tại của tôi")
                }
                startActivity(intent)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun loadAreaOptions(list: Array<String>) {
        currentAreaOptions = list.drop(1)
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            currentAreaOptions
        )
        autoArea.setAdapter(adapter)
        autoArea.setText("")
    }
}
