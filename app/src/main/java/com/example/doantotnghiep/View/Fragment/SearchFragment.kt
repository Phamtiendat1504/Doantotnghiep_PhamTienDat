package com.example.doantotnghiep.View.Fragment

import android.content.Intent
import android.os.Bundle
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
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.AddressData
import com.example.doantotnghiep.Utils.NumberFormatUtils
import com.example.doantotnghiep.View.Auth.LocationPickerActivity
import com.example.doantotnghiep.View.Auth.SearchResultsActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class SearchFragment : Fragment() {

    private lateinit var chipPhuong: Chip
    private lateinit var chipXa: Chip
    private lateinit var chipScopeWard: Chip
    private lateinit var chipScopeDistrict: Chip
    private lateinit var autoArea: AutoCompleteTextView
    private lateinit var edtAddress: EditText
    private lateinit var chipGroupPrice: ChipGroup
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
    private lateinit var rgRoomStyle: RadioGroup
    private lateinit var rgCurfew: RadioGroup
    private lateinit var edtCurfewTime: EditText
    private lateinit var btnSearch: MaterialButton

    // ── Map location picker ──
    private lateinit var btnPickMapLocation: MaterialButton
    private lateinit var layoutMapSelected: LinearLayout
    private lateinit var tvMapSelectedAddress: TextView
    private lateinit var btnClearMapLocation: ImageView
    private lateinit var layoutRadiusPicker: LinearLayout
    private lateinit var tvRadiusValue: TextView
    private lateinit var seekBarRadius: SeekBar

    // Lưu vị trí bản đồ đã chọn
    private var mapLat: Double = 0.0
    private var mapLng: Double = 0.0
    private var mapAddress: String = ""
    // Bán kính mặc định 2km (seekbar: 0→1km, 1→2km, ..., 4→5km)
    private var radiusKm: Double = 2.0

    private val radiusOptions = listOf(1.0, 2.0, 3.0, 4.0, 5.0)

    private val locationPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            mapLat     = data.getDoubleExtra(LocationPickerActivity.EXTRA_RESULT_LAT, 0.0)
            mapLng     = data.getDoubleExtra(LocationPickerActivity.EXTRA_RESULT_LNG, 0.0)
            mapAddress = data.getStringExtra(LocationPickerActivity.EXTRA_RESULT_ADDRESS).orEmpty()
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
        chipScopeWard = view.findViewById(R.id.chipScopeWard)
        chipScopeDistrict = view.findViewById(R.id.chipScopeDistrict)
        autoArea = view.findViewById(R.id.autoArea)
        edtAddress = view.findViewById(R.id.edtAddress)
        chipGroupPrice = view.findViewById(R.id.chipGroupPrice)
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
        rgRoomStyle = view.findViewById(R.id.rgRoomStyle)
        rgCurfew = view.findViewById(R.id.rgCurfew)
        edtCurfewTime = view.findViewById(R.id.edtCurfewTime)
        btnSearch = view.findViewById(R.id.btnSearch)

        // Map location views
        btnPickMapLocation     = view.findViewById(R.id.btnPickMapLocation)
        layoutMapSelected      = view.findViewById(R.id.layoutMapSelected)
        tvMapSelectedAddress   = view.findViewById(R.id.tvMapSelectedAddress)
        btnClearMapLocation    = view.findViewById(R.id.btnClearMapLocation)
        layoutRadiusPicker     = view.findViewById(R.id.layoutRadiusPicker)
        tvRadiusValue          = view.findViewById(R.id.tvRadiusValue)
        seekBarRadius          = view.findViewById(R.id.seekBarRadius)

        loadAreaOptions(AddressData.phuongList)
        setupAreaPickerBehavior()
        setupMapLocationPicker()

        chipPhuong.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) loadAreaOptions(AddressData.phuongList)
        }
        chipXa.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) loadAreaOptions(AddressData.xaList)
        }

        chipPriceCustom.setOnCheckedChangeListener { _, isChecked ->
            edtCustomPrice.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (isChecked) chipGroupPrice.clearCheck()
        }

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
    // Map location picker setup
    // ────────────────────────────────────────────────
    private fun setupMapLocationPicker() {
        // Mặc định SeekBar ở vị trí 1 (= 2km)
        seekBarRadius.progress = 1
        tvRadiusValue.text = "2 km"
        radiusKm = 2.0

        btnPickMapLocation.setOnClickListener {
            val intent = Intent(requireContext(), LocationPickerActivity::class.java)
            locationPickerLauncher.launch(intent)
        }

        btnClearMapLocation.setOnClickListener {
            clearMapSelection()
        }

        seekBarRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                radiusKm = radiusOptions[progress]
                tvRadiusValue.text = "${radiusKm.toInt()} km"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun applyMapSelection() {
        if (mapLat == 0.0 && mapLng == 0.0) return

        // Hiển thị địa chỉ đã chọn và thanh bán kính
        tvMapSelectedAddress.text = mapAddress.ifEmpty { "Vị trí đã chọn: $mapLat, $mapLng" }
        layoutMapSelected.visibility   = View.VISIBLE
        layoutRadiusPicker.visibility  = View.VISIBLE

        // Thay đổi nút từ outline sang highlight để báo người dùng đã chọn
        btnPickMapLocation.text = "🗺 Thay đổi vị trí"
    }

    private fun clearMapSelection() {
        mapLat = 0.0
        mapLng = 0.0
        mapAddress = ""
        layoutMapSelected.visibility   = View.GONE
        layoutRadiusPicker.visibility  = View.GONE
        btnPickMapLocation.text = "Chọn vị trí trên bản đồ"
        // Reset radius
        seekBarRadius.progress = 1
        tvRadiusValue.text = "2 km"
        radiusKm = 2.0
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
            intent.putExtra("searchMode", "nearby")
            intent.putExtra("lat", mapLat)
            intent.putExtra("lng", mapLng)
            intent.putExtra("radiusKm", radiusKm)
            intent.putExtra("mapAddress", mapAddress)
            // Truyền thêm các bộ lọc giá/tiện ích nếu người dùng đã chọn
            appendPriceAndAmenityExtras(intent)
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
        val searchMode = if (chipScopeDistrict.isChecked) "district" else "ward"
        val districtForSearch = if (
            searchMode == "district" && districtName.equals("Hà Nội", ignoreCase = true)
        ) {
            wardName
        } else {
            districtName
        }

        val intent = Intent(requireContext(), SearchResultsActivity::class.java)
        intent.putExtra("ward", wardName)
        intent.putExtra("district", districtForSearch)
        intent.putExtra("searchMode", searchMode)
        intent.putExtra("addressKeyword", edtAddress.text?.toString()?.trim().orEmpty())

        appendPriceAndAmenityExtras(intent)

        val roomType = when (rgRoomStyle.checkedRadioButtonId) {
            R.id.rbShared -> "Ở ghép"
            R.id.rbPrivate -> "Riêng tư"
            else -> ""
        }
        intent.putExtra("roomType", roomType)
        intent.putExtra("hasWifi", cbWifi.isChecked)
        intent.putExtra("hasElectric", cbElectric.isChecked)
        intent.putExtra("hasWater", cbWater.isChecked)

        val curfew = when (rgCurfew.checkedRadioButtonId) {
            R.id.rbCurfewFree -> "Tự do"
            R.id.rbCurfewCustom -> "Tùy chọn"
            else -> ""
        }
        intent.putExtra("curfew", curfew)
        intent.putExtra("genderPrefer", "")

        startActivity(intent)
    }

    /**
     * Đóng gói các extra về giá và tiện ích vào Intent (dùng chung cho cả 2 chế độ tìm kiếm).
     */
    private fun appendPriceAndAmenityExtras(intent: Intent) {
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
            when (chipGroupPrice.checkedChipId) {
                R.id.chipPrice1 -> { minPrice = 1_000_000L; maxPrice = 3_000_000L }
                R.id.chipPrice2 -> { minPrice = 3_000_000L; maxPrice = 6_000_000L }
                R.id.chipPrice3 -> { minPrice = 6_000_000L; maxPrice = 9_000_000L }
                R.id.chipPrice4 -> { minPrice = 9_000_000L; maxPrice = 12_000_000L }
                R.id.chipPrice5 -> { minPrice = 12_000_000L; maxPrice = 0L }
            }
        }
        intent.putExtra("minPrice", minPrice)
        intent.putExtra("maxPrice", maxPrice)

        val roomArea = edtRoomArea.text.toString().toIntOrNull() ?: 0
        if (roomArea > 0) {
            intent.putExtra("minArea", roomArea - 5)
            intent.putExtra("maxArea", roomArea + 5)
        } else {
            intent.putExtra("minArea", 0)
            intent.putExtra("maxArea", 0)
        }
        intent.putExtra("desiredPeople", edtPeopleCount.text.toString().toIntOrNull() ?: 0)
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
        chipScopeWard.isChecked = true
        loadAreaOptions(AddressData.phuongList)
        autoArea.setText("")
        edtAddress.text?.clear()

        chipGroupPrice.clearCheck()
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

        rgCurfew.clearCheck()
        edtCurfewTime.text?.clear()
        edtCurfewTime.visibility = View.GONE

        // Xóa vị trí bản đồ đã chọn
        clearMapSelection()
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
