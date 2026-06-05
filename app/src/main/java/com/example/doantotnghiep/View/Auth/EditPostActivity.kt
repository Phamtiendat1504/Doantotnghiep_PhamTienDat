package com.example.doantotnghiep.View.Auth

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.AddressData
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.Utils.NumberFormatUtils
import com.example.doantotnghiep.ViewModel.EditPostViewModel
import com.google.android.material.button.MaterialButton
import java.util.*
import androidx.activity.addCallback
import java.text.SimpleDateFormat

class EditPostActivity : AppCompatActivity() {

    private val viewModel: EditPostViewModel by viewModels()

    private var existingImageUrls = mutableListOf<String>()
    private val newImageUris = mutableListOf<Uri>()
    private val deletedImageUrls = mutableListOf<String>()

    private var otherFeeContainer: LinearLayout? = null
    private val otherFeeRows = mutableListOf<Pair<EditText, EditText>>()
    private var furnitureContainer: LinearLayout? = null
    private val furnitureRows = mutableListOf<Pair<EditText, EditText>>()
    private var serviceContainer: LinearLayout? = null
    private val serviceRows = mutableListOf<Pair<EditText, EditText>>()

    private val dayLabels = listOf("Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật")
    private val dayRows = mutableListOf<Triple<CheckBox, TextView, TextView>>()
    private var selectedExpiryMs: Long? = null
    private var currentDistrict = ""
    private var originalExpiryDate: Long = 0
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null
    private var selectedLocationAddress: String = ""
    private var isFormLoaded = false

    private fun addFurnitureRow(initName: String = "", initQty: String = "") {
        val container = furnitureContainer ?: return
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d + 0.5f).toInt()
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(42))
                .apply { bottomMargin = dp(6) }
        }
        val edtName = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            hint = "Tên đồ dùng (VD: Sofa, Tủ lạnh...)"
            textSize = 13f
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setPadding(dp(12), 0, dp(4), 0)
            setBackgroundResource(R.drawable.bg_edit_post)
            if (initName.isNotEmpty()) setText(initName)
        }
        val edtQty = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(72), LinearLayout.LayoutParams.MATCH_PARENT)
                .apply { marginStart = dp(6) }
            hint = "SL"
            textSize = 13f
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            gravity = android.view.Gravity.CENTER
            setPadding(dp(8), 0, dp(8), 0)
            setBackgroundResource(R.drawable.bg_edit_post)
            if (initQty.isNotEmpty()) setText(initQty)
        }
        val tvDel = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(32), dp(32)).apply { marginStart = dp(6) }
            text = "✕"; textSize = 16f; gravity = android.view.Gravity.CENTER
            setTextColor(androidx.core.content.ContextCompat.getColor(this@EditPostActivity, R.color.text_secondary))
        }
        val pair = edtName to edtQty
        furnitureRows.add(pair)
        row.addView(edtName); row.addView(edtQty); row.addView(tvDel)
        container.addView(row)
        tvDel.setOnClickListener { container.removeView(row); furnitureRows.remove(pair) }
    }

    private fun addFeeRow(initLabel: String = "", initPrice: String = "") {
        val container = otherFeeContainer ?: return
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d + 0.5f).toInt()
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(42))
                .apply { bottomMargin = dp(6) }
        }
        val edtLabel = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.5f)
            hint = "Tên khoản phí (VD: Phí vệ sinh)"
            textSize = 13f
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setPadding(dp(12), 0, dp(4), 0)
            setBackgroundResource(R.drawable.bg_edit_post)
            if (initLabel.isNotEmpty()) setText(initLabel)
        }
        val edtPrice = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                .apply { marginStart = dp(6) }
            hint = "Số tiền/tháng"
            textSize = 13f
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setPadding(dp(12), 0, dp(4), 0)
            setBackgroundResource(R.drawable.bg_edit_post)
            if (initPrice.isNotEmpty()) setText(initPrice)
        }
        val tvDel = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(32), dp(32)).apply { marginStart = dp(6) }
            text = "✕"; textSize = 16f; gravity = android.view.Gravity.CENTER
            setTextColor(androidx.core.content.ContextCompat.getColor(this@EditPostActivity, R.color.text_secondary))
        }
        NumberFormatUtils.addFormatWatcher(edtPrice)
        val pair = edtLabel to edtPrice
        otherFeeRows.add(pair)
        row.addView(edtLabel); row.addView(edtPrice); row.addView(tvDel)
        container.addView(row)
        tvDel.setOnClickListener { container.removeView(row); otherFeeRows.remove(pair) }
    }

    private fun addServiceRow(initName: String = "", initPrice: String = "") {
        val container = serviceContainer ?: return
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d + 0.5f).toInt()
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(42))
                .apply { bottomMargin = dp(6) }
        }
        val edtName = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            hint = "Tên dịch vụ (VD: Giặt đồ, Dọn phòng...)"
            textSize = 13f
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setPadding(dp(12), 0, dp(4), 0)
            setBackgroundResource(R.drawable.bg_edit_post)
            if (initName.isNotEmpty()) setText(initName)
        }
        val edtPrice = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(100), LinearLayout.LayoutParams.MATCH_PARENT)
                .apply { marginStart = dp(6) }
            hint = "đ/tháng"
            textSize = 12f
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            gravity = android.view.Gravity.CENTER
            setPadding(dp(8), 0, dp(8), 0)
            setBackgroundResource(R.drawable.bg_edit_post)
            if (initPrice.isNotEmpty()) setText(initPrice)
        }
        NumberFormatUtils.addFormatWatcher(edtPrice)
        val tvDel = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(32), dp(32)).apply { marginStart = dp(6) }
            text = "✕"; textSize = 16f; gravity = android.view.Gravity.CENTER
            setTextColor(androidx.core.content.ContextCompat.getColor(this@EditPostActivity, R.color.text_secondary))
        }
        val pair = edtName to edtPrice
        serviceRows.add(pair)
        row.addView(edtName); row.addView(edtPrice); row.addView(tvDel)
        container.addView(row)
        tvDel.setOnClickListener { container.removeView(row); serviceRows.remove(pair) }
    }

    private val MAX_PHOTOS = 10
    private var roomId = ""
    
    private var replaceIndex = -1
    private var isReplacingExisting = false

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val clipData = data?.clipData
            if (clipData != null) {
                val count = clipData.itemCount
                for (i in 0 until count) {
                    val uri = clipData.getItemAt(i).uri
                    if ((existingImageUrls.size + newImageUris.size) < MAX_PHOTOS) {
                        newImageUris.add(uri)
                    }
                }
                renderPhotos()
            } else if (data?.data != null) {
                val uri = data.data ?: return@registerForActivityResult
                if ((existingImageUrls.size + newImageUris.size) < MAX_PHOTOS) {
                    newImageUris.add(uri)
                    renderPhotos()
                }
            }
        }
    }

    private val replaceImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            if (isReplacingExisting) {
                if (replaceIndex < 0 || replaceIndex >= existingImageUrls.size) return@registerForActivityResult
                deletedImageUrls.add(existingImageUrls[replaceIndex])
                existingImageUrls.removeAt(replaceIndex)
                newImageUris.add(uri)
            } else {
                if (replaceIndex < 0 || replaceIndex >= newImageUris.size) return@registerForActivityResult
                newImageUris[replaceIndex] = uri
            }
            renderPhotos()
        }
    }

    // Launcher nhận kết quả từ LocationPickerActivity
    private val pickLocationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult
        val lat = data.getDoubleExtra(LocationPickerActivity.EXTRA_RESULT_LAT, Double.NaN)
        val lng = data.getDoubleExtra(LocationPickerActivity.EXTRA_RESULT_LNG, Double.NaN)
        if (lat.isNaN() || lng.isNaN()) return@registerForActivityResult

        selectedLatitude = lat
        selectedLongitude = lng
        selectedLocationAddress = data.getStringExtra(LocationPickerActivity.EXTRA_RESULT_ADDRESS).orEmpty()

        val tvPickedLocation = findViewById<TextView>(R.id.tvPickedLocation)
        val edtAddress = findViewById<EditText>(R.id.edtAddress)
        val spinnerWard = findViewById<Spinner>(R.id.spinnerWard)

        if (selectedLocationAddress.isNotBlank()) {
            edtAddress.setText(selectedLocationAddress)
        }
        // Cố gắng sync spinnerWard từ địa chỉ bản đồ trả về
        if (selectedLocationAddress.isNotBlank()) {
            val adapterCount = spinnerWard.adapter?.count ?: 0
            data class WardCandidate(val index: Int, val wardName: String)
            val candidates = (0 until adapterCount).mapNotNull { i ->
                val item = spinnerWard.adapter.getItem(i)?.toString() ?: return@mapNotNull null
                val wardName = if (item.contains("(")) item.substringBefore("(").trim() else item
                if (wardName.isNotBlank()) WardCandidate(i, wardName) else null
            }.sortedByDescending { it.wardName.length }
            for (candidate in candidates) {
                if (selectedLocationAddress.contains(candidate.wardName, ignoreCase = true)) {
                    spinnerWard.setSelection(candidate.index); break
                }
            }
        }
        tvPickedLocation?.text = if (selectedLocationAddress.isNotBlank()) {
            "Đã chọn: $selectedLocationAddress"
        } else {
            "Đã chọn tọa độ: %.6f, %.6f".format(Locale.US, lat, lng)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_post)
        
        onBackPressedDispatcher.addCallback(this) { confirmDiscard() }
        
        roomId = intent.getStringExtra("roomId") ?: ""
        if (roomId.isEmpty()) { finish(); return }

        setupUI()
        setupObservers()
        viewModel.loadRoomData(roomId)
    }

    private fun setupObservers() {
        // Quan sát dữ liệu phòng từ ViewModel
        viewModel.roomData.observe(this) { data ->
            if (data == null) return@observe
            populateForm(data)
        }

        // Quan sát tiến trình upload
        viewModel.uploadProgressText.observe(this) { text ->
            findViewById<TextView>(R.id.tvProgressPercent)?.text = text
        }

        // Quan sát kết quả lưu
        viewModel.saveResult.observe(this) { success ->
            if (success == true) {
                viewModel.resetSaveResult()
                findViewById<LinearLayout>(R.id.layoutProgress)?.visibility = View.GONE
                MessageUtils.showSuccessDialog(this, "Thành công", "Bài đăng đã được cập nhật và đang chờ duyệt lại.") { finish() }
            }
        }

        // Quan sát lỗi
        viewModel.errorMessage.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                viewModel.resetErrorMessage()
                findViewById<LinearLayout>(R.id.layoutProgress)?.visibility = View.GONE
                MessageUtils.showErrorDialog(this, "Lỗi cập nhật", error)
            }
        }

        // Quan sát trạng thái loading
        viewModel.isLoading.observe(this) { isLoading ->
            val layoutProgress = findViewById<LinearLayout>(R.id.layoutProgress)
            layoutProgress?.visibility = if (isLoading) View.VISIBLE else View.GONE
            findViewById<MaterialButton>(R.id.btnPostRoom)?.isEnabled = !isLoading
        }
    }

    private fun setupUI() {
        findViewById<ImageView>(R.id.btnBack).apply {
            visibility = View.VISIBLE
            setOnClickListener { confirmDiscard() }
        }

        findViewById<TextView>(R.id.tvHeaderTitle).text = "Sửa bài đăng"
        findViewById<TextView>(R.id.tvHeaderSubTitle).text = "Chạm vào ảnh để thay đổi hoặc xóa"
        findViewById<Button>(R.id.btnPostRoom)?.text = "Cập nhật bài viết"

        // Khóa Họ và tên, Số điện thoại và Giới tính — chỉ thay đổi qua trang thông tin cá nhân
        listOf(R.id.edtOwnerName, R.id.edtOwnerPhone).forEach { id ->
            findViewById<EditText>(id)?.apply {
                isEnabled = false
                isFocusable = false
            }
        }
        val rgGender = findViewById<RadioGroup>(R.id.rgOwnerGender)
        rgGender?.isEnabled = false
        for (i in 0 until (rgGender?.childCount ?: 0)) {
            rgGender?.getChildAt(i)?.isEnabled = false
        }
        
        val spinnerWard = findViewById<Spinner>(R.id.spinnerWard)
        val initAreas = mutableListOf("-- Chọn phường/xã --").apply { addAll(AddressData.phuongList.drop(1)) }
        spinnerWard.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, initAreas).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val chipPostPhuong = findViewById<com.google.android.material.chip.Chip>(R.id.chipPostPhuong)
        val chipPostXa = findViewById<com.google.android.material.chip.Chip>(R.id.chipPostXa)
        chipPostPhuong.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) spinnerWard.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item,
                mutableListOf("-- Chọn phường/xã --").apply { addAll(AddressData.phuongList.drop(1)) }).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        }
        chipPostXa.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) spinnerWard.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item,
                mutableListOf("-- Chọn phường/xã --").apply { addAll(AddressData.xaList.drop(1)) }).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        }

        val priceIds = listOf(R.id.edtPrice, R.id.edtWifiPrice, R.id.edtElectricPrice, R.id.edtWaterPrice, 
               R.id.edtDepositAmount, R.id.edtMotorbikeFee, R.id.edtEBikeFee, R.id.edtBicycleFee)
        priceIds.forEach { id -> findViewById<EditText>(id)?.let { NumberFormatUtils.addFormatWatcher(it) } }

        otherFeeContainer = findViewById(R.id.layoutOtherFees)
        otherFeeRows.clear()
        addFeeRow()
        findViewById<MaterialButton>(R.id.btnAddOtherFee).setOnClickListener { addFeeRow() }
        furnitureContainer = findViewById(R.id.layoutFurnitureItems)
        furnitureRows.clear()
        addFurnitureRow()
        findViewById<MaterialButton>(R.id.btnAddFurnitureItem).setOnClickListener { addFurnitureRow() }
        // Setup serviceItems
        serviceContainer = findViewById(R.id.layoutServiceItems)
        serviceRows.clear()
        addServiceRow()
        findViewById<MaterialButton?>(R.id.btnAddServiceItem)?.setOnClickListener { addServiceRow() }
        setupMainListeners()
        setupParkingListeners()
        setupCard5()
        setupAccordionLogic()
        
        findViewById<CardView>(R.id.btnAddPhoto).setOnClickListener {
            if ((existingImageUrls.size + newImageUris.size) >= MAX_PHOTOS) {
                MessageUtils.showInfoDialog(this, "Giới hạn ảnh", "Tối đa $MAX_PHOTOS ảnh.")
            } else {
                val intent = Intent(Intent.ACTION_PICK)
                intent.type = "image/*"
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                pickImageLauncher.launch(intent)
            }
        }

        findViewById<MaterialButton>(R.id.btnPostRoom).setOnClickListener { updatePost() }
        findViewById<android.widget.TextView>(R.id.tvRulesLink)?.setOnClickListener { showRulesDialog() }

        // Setup nút chọn vị trí trên bản đồ
        val btnPickLocation = findViewById<MaterialButton?>(R.id.btnPickLocation)
        val tvPickedLocation = findViewById<TextView?>(R.id.tvPickedLocation)
        btnPickLocation?.setOnClickListener {
            val spinnerWard = findViewById<Spinner>(R.id.spinnerWard)
            val selectedWard = spinnerWard.selectedItem?.toString().orEmpty()
            val wardName = if (selectedWard.contains("(")) selectedWard.substringBefore("(").trim() else selectedWard
            val districtName = if (selectedWard.contains("(")) selectedWard.substringAfter("(").replace(")", "").trim() else currentDistrict
            val intent = Intent(this, LocationPickerActivity::class.java).apply {
                putExtra(LocationPickerActivity.EXTRA_INITIAL_ADDRESS, findViewById<EditText>(R.id.edtAddress).text.toString().trim())
                putExtra(LocationPickerActivity.EXTRA_INITIAL_WARD, wardName)
                putExtra(LocationPickerActivity.EXTRA_INITIAL_DISTRICT, districtName)
                putExtra(LocationPickerActivity.EXTRA_IS_STRICT, true)
                // Truyền tọa độ hiện tại nếu đã có
                selectedLatitude?.let { putExtra(LocationPickerActivity.EXTRA_RESULT_LAT, it) }
                selectedLongitude?.let { putExtra(LocationPickerActivity.EXTRA_RESULT_LNG, it) }
            }
            pickLocationLauncher.launch(intent)
        }

        // Hiển thị ngày đăng bài hiện tại
        val tvPostDate = findViewById<TextView?>(R.id.tvPostDate)
        tvPostDate?.text = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN")).format(Date())
    }

    private fun setupMainListeners() {
        findViewById<CheckBox>(R.id.cbWifi).setOnCheckedChangeListener { _, isChecked ->
            findViewById<EditText>(R.id.edtWifiPrice).isEnabled = isChecked
        }
        findViewById<CheckBox>(R.id.cbElectric).setOnCheckedChangeListener { _, isChecked ->
            findViewById<EditText>(R.id.edtElectricPrice).isEnabled = isChecked
        }
        findViewById<CheckBox>(R.id.cbWater).setOnCheckedChangeListener { _, isChecked ->
            findViewById<EditText>(R.id.edtWaterPrice).isEnabled = isChecked
        }
        findViewById<RadioGroup>(R.id.rgPet).setOnCheckedChangeListener { _, checkedId ->
            findViewById<LinearLayout>(R.id.layoutPetDetail).visibility = if (checkedId == R.id.rbPetYes) View.VISIBLE else View.GONE
        }
        findViewById<RadioGroup>(R.id.rgCurfew).setOnCheckedChangeListener { _, checkedId ->
            findViewById<LinearLayout>(R.id.layoutCurfewTime).visibility = if (checkedId == R.id.rbCurfewCustom) View.VISIBLE else View.GONE
        }
        
        listOf(
            Pair(R.id.cbAirCon, R.id.edtAirConQty), Pair(R.id.cbWaterHeater, R.id.edtWaterHeaterQty),
            Pair(R.id.cbWasher, R.id.edtWasherQty), Pair(R.id.cbDryingArea, R.id.edtDryingAreaQty),
            Pair(R.id.cbWardrobe, R.id.edtWardrobeQty), Pair(R.id.cbBed, R.id.edtBedQty)
        ).forEach { (cbId, edtId) ->
            findViewById<CheckBox>(cbId).setOnCheckedChangeListener { _, c ->
                val e = findViewById<EditText>(edtId); e.isEnabled = c; if (!c) e.text?.clear()
            }
        }
        setupCurfewPickers()
    }

    private fun setupCurfewPickers() {
        val pickerHour = findViewById<NumberPicker>(R.id.pickerHour)
        val pickerMinute = findViewById<NumberPicker>(R.id.pickerMinute)
        val pickerAmPm = findViewById<NumberPicker>(R.id.pickerAmPm)
        val tvCurfewTime = findViewById<TextView>(R.id.edtCurfewTime)

        pickerHour.minValue = 1; pickerHour.maxValue = 12
        pickerMinute.minValue = 0; pickerMinute.maxValue = 59
        pickerAmPm.minValue = 0; pickerAmPm.maxValue = 1
        pickerAmPm.displayedValues = arrayOf("AM", "PM")

        val onValueChangeListener = NumberPicker.OnValueChangeListener { _, _, _ ->
            val hour = pickerHour.value
            val minute = String.format(Locale.US, "%02d", pickerMinute.value)
            val amPm = if (pickerAmPm.value == 0) "AM" else "PM"
            tvCurfewTime.text = "$hour:$minute $amPm"
        }

        pickerHour.setOnValueChangedListener(onValueChangeListener)
        pickerMinute.setOnValueChangedListener(onValueChangeListener)
        pickerAmPm.setOnValueChangedListener(onValueChangeListener)

        // Initial value
        tvCurfewTime.text = "11:00 PM"
        pickerHour.value = 11; pickerMinute.value = 0; pickerAmPm.value = 1
    }

    private fun setupParkingListeners() {
        setupSingleParking(R.id.cbMotorbike, R.id.rgMotorbikeFee, R.id.rbMotorbikePaid, R.id.edtMotorbikeFee)
        setupSingleParking(R.id.cbEBike, R.id.rgEBikeFee, R.id.rbEBikePaid, R.id.edtEBikeFee)
        setupSingleParking(R.id.cbBicycle, R.id.rgBicycleFee, R.id.rbBicyclePaid, R.id.edtBicycleFee)
    }

    private fun setupSingleParking(cbId: Int, rgId: Int, rbPaidId: Int, edtId: Int) {
        val cb = findViewById<CheckBox>(cbId)
        val rg = findViewById<RadioGroup>(rgId)
        val edt = findViewById<EditText>(edtId)
        cb.setOnCheckedChangeListener { _, isChecked ->
            rg.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) { rg.clearCheck(); edt.visibility = View.GONE }
        }
        rg.setOnCheckedChangeListener { _, checkedId ->
            edt.visibility = if (checkedId == rbPaidId) View.VISIBLE else View.GONE
        }
    }

    // Điền dữ liệu vào form từ Map — được gọi từ observer của viewModel.roomData
    private fun populateForm(d: Map<String, Any>) {
        currentDistrict = d["district"] as? String ?: ""
        if (d["status"] == "rejected") addRejectionBanner(d["rejectReason"] as? String ?: "")

        findViewById<EditText>(R.id.edtOwnerName).setText(d["ownerName"] as? String ?: "")
        findViewById<EditText>(R.id.edtOwnerPhone).setText(d["ownerPhone"] as? String ?: "")
        if (d["ownerGender"] == "Nam") findViewById<RadioButton>(R.id.rbOwnerMale).isChecked = true
        else if (d["ownerGender"] == "Nữ") findViewById<RadioButton>(R.id.rbOwnerFemale).isChecked = true

        findViewById<EditText>(R.id.edtTitle).setText(d["title"] as? String ?: "")
        findViewById<EditText>(R.id.edtAddress).setText(d["address"] as? String ?: "")
        findViewById<EditText>(R.id.edtDescription).setText(d["description"] as? String ?: "")
        
        val ward = d["ward"] as? String ?: ""
        val chipPostPhuong = findViewById<com.google.android.material.chip.Chip>(R.id.chipPostPhuong)
        val chipPostXa = findViewById<com.google.android.material.chip.Chip>(R.id.chipPostXa)
        val isPhuong = AddressData.phuongList.drop(1).contains(ward)
        if (isPhuong || ward.isEmpty()) chipPostPhuong.isChecked = true else chipPostXa.isChecked = true
        val spinner = findViewById<Spinner>(R.id.spinnerWard)
        for (i in 0 until spinner.count) {
            if (spinner.getItemAtPosition(i).toString() == ward) { spinner.setSelection(i); break }
        }

        findViewById<EditText>(R.id.edtPrice).setText(d["price"]?.toString() ?: "")
        findViewById<EditText>(R.id.edtArea).setText(d["area"]?.toString() ?: "")
        findViewById<EditText>(R.id.edtPeopleCount).setText(d["peopleCount"]?.toString() ?: "")
        if (d["roomType"] == "Chung chủ") findViewById<RadioButton>(R.id.rbShared).isChecked = true
        else if (d["roomType"] == "Riêng chủ") findViewById<RadioButton>(R.id.rbPrivate).isChecked = true

        findViewById<EditText>(R.id.edtDepositMonths).setText(d["depositMonths"]?.toString() ?: "")
        findViewById<EditText>(R.id.edtDepositAmount).setText(d["depositAmount"]?.toString() ?: "")

        findViewById<CheckBox>(R.id.cbWifi).isChecked = d["hasWifi"] as? Boolean ?: false
        findViewById<EditText>(R.id.edtWifiPrice).setText(d["wifiPrice"]?.toString() ?: "")
        val hasElectricStored = d["hasElectric"] as? Boolean
            ?: (((d["electricPrice"] as? Number)?.toLong() ?: 0L) > 0L)
        findViewById<CheckBox>(R.id.cbElectric).isChecked = hasElectricStored
        findViewById<EditText>(R.id.edtElectricPrice).setText(d["electricPrice"]?.toString() ?: "")
        val hasWaterStored = d["hasWater"] as? Boolean
            ?: (((d["waterPrice"] as? Number)?.toLong() ?: 0L) > 0L)
        findViewById<CheckBox>(R.id.cbWater).isChecked = hasWaterStored
        findViewById<EditText>(R.id.edtWaterPrice).setText(d["waterPrice"]?.toString() ?: "")
        val storedFees = d["otherFees"] as? List<Map<String, Any>> ?: emptyList()
        if (storedFees.isNotEmpty()) {
            otherFeeContainer?.removeAllViews()
            otherFeeRows.clear()
            storedFees.forEach { fee ->
                val lbl = fee["label"] as? String ?: ""
                val prc = (fee["price"] as? Number)?.toLong() ?: 0L
                addFeeRow(lbl, if (prc > 0) prc.toString() else "")
            }
        }
        // Set checkbox states first — triggers listeners that enable/disable qty fields
        findViewById<CheckBox>(R.id.cbAirCon).isChecked = d["hasAirCon"] as? Boolean ?: false
        findViewById<CheckBox>(R.id.cbWaterHeater).isChecked = d["hasWaterHeater"] as? Boolean ?: false
        findViewById<CheckBox>(R.id.cbWasher).isChecked = d["hasWasher"] as? Boolean ?: false
        findViewById<CheckBox>(R.id.cbDryingArea).isChecked = d["hasDryingArea"] as? Boolean ?: false
        findViewById<CheckBox>(R.id.cbWardrobe).isChecked = d["hasWardrobe"] as? Boolean ?: false
        findViewById<CheckBox>(R.id.cbBed).isChecked = d["hasBed"] as? Boolean ?: false
        // Then set qty values into now-properly-enabled fields
        listOf(
            Triple("airConQty", R.id.cbAirCon, R.id.edtAirConQty),
            Triple("waterHeaterQty", R.id.cbWaterHeater, R.id.edtWaterHeaterQty),
            Triple("washerQty", R.id.cbWasher, R.id.edtWasherQty),
            Triple("dryingAreaQty", R.id.cbDryingArea, R.id.edtDryingAreaQty),
            Triple("wardrobeQty", R.id.cbWardrobe, R.id.edtWardrobeQty),
            Triple("bedQty", R.id.cbBed, R.id.edtBedQty)
        ).forEach { (key, _, edtId) ->
            val qty = (d[key] as? Number)?.toInt() ?: 0
            if (qty > 0) { findViewById<EditText>(edtId).setText(qty.toString()) }
        }
        val storedFurniture = d["furnitureItems"] as? List<Map<String, Any>> ?: emptyList()
        if (storedFurniture.isNotEmpty()) {
            furnitureContainer?.removeAllViews(); furnitureRows.clear()
            storedFurniture.forEach { item ->
                val name = item["name"] as? String ?: ""
                val qty = (item["qty"] as? Number)?.toInt() ?: 1
                addFurnitureRow(name, if (qty > 0) qty.toString() else "")
            }
        }
        // Load serviceItems (Dịch vụ phòng)
        val storedServices = d["serviceItems"] as? List<Map<String, Any>> ?: emptyList()
        if (storedServices.isNotEmpty()) {
            serviceContainer?.removeAllViews(); serviceRows.clear()
            storedServices.forEach { item ->
                val name = item["name"] as? String ?: ""
                val price = (item["price"] as? Number)?.toLong() ?: 0L
                addServiceRow(name, if (price > 0) price.toString() else "")
            }
        }
        // Load tọa độ bản đồ đã chọn
        val lat = (d["latitude"] as? Number)?.toDouble()
        val lng = (d["longitude"] as? Number)?.toDouble()
        if (lat != null && lng != null) {
            selectedLatitude = lat
            selectedLongitude = lng
            val savedAddress = d["locationAddress"] as? String ?: ""
            val fallbackAddress = d["address"] as? String ?: ""
            selectedLocationAddress = savedAddress
            val tvPickedLocation = findViewById<TextView?>(R.id.tvPickedLocation)
            tvPickedLocation?.text = when {
                savedAddress.isNotBlank() -> "Đã chọn: $savedAddress"
                fallbackAddress.isNotBlank() -> "Đã chọn: $fallbackAddress"
                else -> "Đã chọn tọa độ: %.6f, %.6f".format(Locale.US, lat, lng)
            }
            tvPickedLocation?.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_primary))
        }

        when(d["kitchen"] as? String) {
            "Chung" -> findViewById<RadioButton>(R.id.rbKitchenShared).isChecked = true
            "Riêng" -> findViewById<RadioButton>(R.id.rbKitchenPrivate).isChecked = true
            "Không" -> findViewById<RadioButton>(R.id.rbKitchenNone).isChecked = true
        }
        if (d["bathroom"] == "Chung") findViewById<RadioButton>(R.id.rbBathroomShared).isChecked = true
        else if (d["bathroom"] == "Riêng") findViewById<RadioButton>(R.id.rbBathroomPrivate).isChecked = true

        if (d["pet"] == "Cho nuôi") {
            findViewById<RadioButton>(R.id.rbPetYes).isChecked = true
            findViewById<LinearLayout>(R.id.layoutPetDetail).visibility = View.VISIBLE
            findViewById<EditText>(R.id.edtPetName).setText(d["petName"] as? String ?: "")
            findViewById<EditText>(R.id.edtPetCount).setText(d["petCount"]?.toString() ?: "")
        } else {
            findViewById<RadioButton>(R.id.rbPetNo).isChecked = true
        }

        val curfew = d["curfew"] as? String ?: ""
        val curfewTime = d["curfewTime"] as? String ?: ""
        when (curfew) {
            "Tự do" -> {
                findViewById<RadioButton>(R.id.rbCurfewFree).isChecked = true
                findViewById<LinearLayout>(R.id.layoutCurfewTime).visibility = View.GONE
            }
            "Tùy chọn" -> {
                findViewById<RadioButton>(R.id.rbCurfewCustom).isChecked = true
                findViewById<LinearLayout>(R.id.layoutCurfewTime).visibility = View.VISIBLE
                val tvCurfew = findViewById<TextView>(R.id.edtCurfewTime)
                tvCurfew.text = curfewTime
                
                // Parse time string (e.g., "11:30 PM") and set to NumberPickers.
                runCatching {
                    val parts = curfewTime.split(" ")
                    if (parts.size == 2) {
                        val timeParts = parts[0].split(":")
                        if (timeParts.size == 2) {
                            findViewById<NumberPicker>(R.id.pickerHour).value = timeParts[0].toInt().coerceIn(1, 12)
                            findViewById<NumberPicker>(R.id.pickerMinute).value = timeParts[1].toInt().coerceIn(0, 59)
                            findViewById<NumberPicker>(R.id.pickerAmPm).value = if (parts[1] == "AM") 0 else 1
                        }
                    }
                }
            }
        }

        when (d["genderPrefer"] as? String ?: "") {
            "Nam" -> findViewById<RadioButton>(R.id.rbGenderMale).isChecked = true
            "Nữ" -> findViewById<RadioButton>(R.id.rbGenderFemale).isChecked = true
            else -> findViewById<RadioButton>(R.id.rbGenderAll).isChecked = true
        }

        loadParkingData("Motorbike", d); loadParkingData("EBike", d); loadParkingData("Bicycle", d)

        existingImageUrls = (d["imageUrls"] as? List<*>)?.mapNotNull { it as? String }?.toMutableList() ?: mutableListOf()
        renderPhotos()

        // Mục 5: Hạn hiển thị — hiển thị ngày cũ nếu còn hợp lệ
        val expiryMs = (d["postExpiryDate"] as? Number)?.toLong() ?: 0L
        originalExpiryDate = expiryMs
        if (expiryMs > 0) {
            selectedExpiryMs = expiryMs
            val expiryDateFormat = java.text.SimpleDateFormat("HH:mm, dd/MM/yyyy", java.util.Locale("vi", "VN"))
            val tvExp = findViewById<TextView>(R.id.tvExpiryDisplay)
            tvExp?.text = expiryDateFormat.format(java.util.Date(expiryMs))
            tvExp?.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_primary))
        }

        // Mục 5: Khung giờ — parse chuỗi "Thứ 2: 08:00-17:00\nThứ 6: 09:00-21:00"
        val savedSlots = d["availableTimeSlots"] as? String ?: ""
        if (savedSlots.isNotBlank()) {
            val slotMap = savedSlots.lines().mapNotNull { line ->
                val parts = line.split(": ")
                if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
            }.toMap()
            dayRows.forEachIndexed { i, (cb, tvStart, tvEnd) ->
                val times = slotMap[dayLabels[i]]
                if (times != null) {
                    cb.isChecked = true
                    tvStart.isEnabled = true; tvStart.alpha = 1f
                    tvEnd.isEnabled = true; tvEnd.alpha = 1f
                    val t = times.split("-")
                    if (t.size == 2) { tvStart.text = t[0].trim(); tvEnd.text = t[1].trim() }
                }
            }
        }
        isFormLoaded = true
    }

    private fun loadParkingData(type: String, d: Map<String, Any>) {
        val hasKey = "has$type"
        val cb = findViewById<CheckBox>(resources.getIdentifier("cb$type", "id", packageName))
        val rg = findViewById<RadioGroup>(resources.getIdentifier("rg${type}Fee", "id", packageName))
        if (d[hasKey] as? Boolean == true) {
            cb.isChecked = true; rg.visibility = View.VISIBLE
            val feeKey = if (type == "EBike") "eBikeFee" else "${type.lowercase()}Fee"
            val fee = (d[feeKey] as? Number ?: d["ebikeFee"] as? Number)?.toLong() ?: 0L
            if (fee > 0) {
                findViewById<RadioButton>(resources.getIdentifier("rb${type}Paid", "id", packageName)).isChecked = true
                findViewById<EditText>(resources.getIdentifier("edt${type}Fee", "id", packageName)).apply { visibility = View.VISIBLE; setText(fee.toString()) }
            } else {
                findViewById<RadioButton>(resources.getIdentifier("rb${type}Free", "id", packageName)).isChecked = true
            }
        }
    }

    private fun addRejectionBanner(reason: String) {
        val btnPost = findViewById<View>(R.id.btnPostRoom)
        val container = btnPost.parent as? ViewGroup ?: return
        val banner = layoutInflater.inflate(R.layout.layout_rejection_banner, container, false)
        banner.findViewById<TextView>(R.id.tvRejectReason).text = "Lý do từ chối: $reason"
        container.addView(banner, 1)
    }

    private fun renderPhotos() {
        val layout = findViewById<LinearLayout>(R.id.layoutPhotos)
        val tvPhotoCount = findViewById<TextView>(R.id.tvPhotoCount)
        val btnAdd = findViewById<CardView>(R.id.btnAddPhoto)
        layout.removeAllViews()

        existingImageUrls.forEachIndexed { index, url ->
            val imgView = createImageView()
            Glide.with(this).load(url).centerCrop().into(imgView)
            imgView.setOnClickListener { showImageOptions(index, true, url, null) }
            layout.addView(imgView)
        }

        newImageUris.forEachIndexed { index, uri ->
            val imgView = createImageView()
            imgView.setImageURI(uri)
            imgView.setOnClickListener { showImageOptions(index, false, null, uri) }
            layout.addView(imgView)
        }

        layout.addView(btnAdd)
        val total = existingImageUrls.size + newImageUris.size
        tvPhotoCount.text = "$total/$MAX_PHOTOS ảnh"
        btnAdd.visibility = if (total < MAX_PHOTOS) View.VISIBLE else View.GONE
    }

    private fun createImageView(): ImageView {
        return ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(90), dpToPx(90)).apply { marginEnd = dpToPx(8) }
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            background = getDrawable(R.drawable.bg_edit_post)
            outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
        }
    }

    private fun showImageOptions(index: Int, isExisting: Boolean, url: String?, uri: Uri?) {
        val options = arrayOf("Xem ảnh phóng to", "Thay đổi ảnh khác", "Xóa ảnh này")
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Tùy chọn hình ảnh")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showImagePreview(url, uri)
                    1 -> {
                        replaceIndex = index
                        isReplacingExisting = isExisting
                        replaceImageLauncher.launch(Intent(Intent.ACTION_PICK).apply { type = "image/*" })
                    }
                    2 -> {
                        if (isExisting) {
                            deletedImageUrls.add(existingImageUrls[index])
                            existingImageUrls.removeAt(index)
                        } else {
                            newImageUris.removeAt(index)
                        }
                        renderPhotos()
                    }
                }
            }
            .setNegativeButton("Đóng", null)
            .show()
    }

    private fun showImagePreview(url: String?, uri: Uri?) {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen).create()
        val view = LayoutInflater.from(this).inflate(R.layout.layout_image_preview, null)
        val imgFull = view.findViewById<ImageView>(R.id.imgFull)
        val btnClose = view.findViewById<ImageView>(R.id.btnClose)

        if (url != null) Glide.with(this).load(url).into(imgFull)
        else imgFull.setImageURI(uri)

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.setView(view)
        dialog.show()
    }

    private fun updatePost() {
        val spinnerWard = findViewById<Spinner>(R.id.spinnerWard)
        if (spinnerWard.selectedItemPosition == 0) {
            MessageUtils.showInfoDialog(this, "Thông tin thiếu", "Vui lòng chọn khu vực phường/xã.")
            return
        }

        MessageUtils.showConfirmDialog(
            context = this,
            title = "Xác nhận cập nhật",
            message = "Bài đăng sẽ được gửi lại để admin duyệt sau khi cập nhật. Bạn có muốn tiếp tục?",
            positiveText = "Cập nhật",
            negativeText = "Hủy",
            onConfirm = {
                val layoutProgress = findViewById<LinearLayout>(R.id.layoutProgress)
                layoutProgress?.visibility = View.VISIBLE
                findViewById<TextView>(R.id.tvProgressPercent)?.text = "Đang đăng bài..."

                val ward = spinnerWard.selectedItem.toString()
                val district = ward

                val data = buildFormData()
                viewModel.updatePost(
                    context = this,
                    roomId = roomId,
                    ward = ward,
                    district = district,
                    existingImageUrls = existingImageUrls,
                    newImageUris = newImageUris,
                    deletedImageUrls = deletedImageUrls,
                    data = data
                )
            }
        )
    }

    private fun buildFormData(): HashMap<String, Any> {
        val data = hashMapOf<String, Any>(
            "ownerName" to findViewById<EditText>(R.id.edtOwnerName).text.toString().trim(),
            "ownerPhone" to findViewById<EditText>(R.id.edtOwnerPhone).text.toString().trim(),
            "ownerGender" to if (findViewById<RadioButton>(R.id.rbOwnerMale).isChecked) "Nam" else "Nữ",
            "title" to findViewById<EditText>(R.id.edtTitle).text.toString().trim(),
            "address" to findViewById<EditText>(R.id.edtAddress).text.toString().trim(),
            "description" to findViewById<EditText>(R.id.edtDescription).text.toString().trim(),
            "price" to (NumberFormatUtils.getRawNumber(findViewById(R.id.edtPrice)).toLongOrNull() ?: 0L),
            "area" to (findViewById<EditText>(R.id.edtArea).text.toString().toIntOrNull() ?: 0),
            "peopleCount" to (findViewById<EditText>(R.id.edtPeopleCount).text.toString().toIntOrNull() ?: 0),
            "roomType" to if (findViewById<RadioButton>(R.id.rbShared).isChecked) "Chung chủ" else "Riêng chủ",
            "depositMonths" to (findViewById<EditText>(R.id.edtDepositMonths).text.toString().toIntOrNull() ?: 0),
            "depositAmount" to (NumberFormatUtils.getRawNumber(findViewById(R.id.edtDepositAmount)).toLongOrNull() ?: 0L)
        )

        data["hasWifi"] = findViewById<CheckBox>(R.id.cbWifi).isChecked
        data["wifiPrice"] = NumberFormatUtils.getRawNumber(findViewById(R.id.edtWifiPrice)).toLongOrNull() ?: 0L
        data["hasElectric"] = findViewById<CheckBox>(R.id.cbElectric).isChecked
        data["electricPrice"] = NumberFormatUtils.getRawNumber(findViewById(R.id.edtElectricPrice)).toLongOrNull() ?: 0L
        data["hasWater"] = findViewById<CheckBox>(R.id.cbWater).isChecked
        data["waterPrice"] = NumberFormatUtils.getRawNumber(findViewById(R.id.edtWaterPrice)).toLongOrNull() ?: 0L
        data["otherFees"] = otherFeeRows.mapNotNull { (lbl, prc) ->
            val label = lbl.text.toString().trim()
            val price = NumberFormatUtils.getRawNumber(prc).toLongOrNull() ?: 0L
            if (label.isNotEmpty()) mapOf<String, Any>("label" to label, "price" to price) else null
        }
        data["airConQty"] = if (findViewById<CheckBox>(R.id.cbAirCon).isChecked) findViewById<EditText>(R.id.edtAirConQty).text.toString().toIntOrNull() ?: 1 else 0
        data["waterHeaterQty"] = if (findViewById<CheckBox>(R.id.cbWaterHeater).isChecked) findViewById<EditText>(R.id.edtWaterHeaterQty).text.toString().toIntOrNull() ?: 1 else 0
        data["washerQty"] = if (findViewById<CheckBox>(R.id.cbWasher).isChecked) findViewById<EditText>(R.id.edtWasherQty).text.toString().toIntOrNull() ?: 1 else 0
        data["dryingAreaQty"] = if (findViewById<CheckBox>(R.id.cbDryingArea).isChecked) findViewById<EditText>(R.id.edtDryingAreaQty).text.toString().toIntOrNull() ?: 1 else 0
        data["wardrobeQty"] = if (findViewById<CheckBox>(R.id.cbWardrobe).isChecked) findViewById<EditText>(R.id.edtWardrobeQty).text.toString().toIntOrNull() ?: 1 else 0
        data["bedQty"] = if (findViewById<CheckBox>(R.id.cbBed).isChecked) findViewById<EditText>(R.id.edtBedQty).text.toString().toIntOrNull() ?: 1 else 0
        data["furnitureItems"] = furnitureRows.mapNotNull { (name, qty) ->
            val n = name.text.toString().trim()
            val q = qty.text.toString().toIntOrNull() ?: 1
            if (n.isNotEmpty()) mapOf<String, Any>("name" to n, "qty" to q) else null
        }
        data["hasAirCon"] = findViewById<CheckBox>(R.id.cbAirCon).isChecked
        data["hasWaterHeater"] = findViewById<CheckBox>(R.id.cbWaterHeater).isChecked
        data["hasWasher"] = findViewById<CheckBox>(R.id.cbWasher).isChecked
        data["hasDryingArea"] = findViewById<CheckBox>(R.id.cbDryingArea).isChecked
        data["hasWardrobe"] = findViewById<CheckBox>(R.id.cbWardrobe).isChecked
        data["hasBed"] = findViewById<CheckBox>(R.id.cbBed).isChecked
        
        data["kitchen"] = when {
            findViewById<RadioButton>(R.id.rbKitchenShared).isChecked -> "Chung"
            findViewById<RadioButton>(R.id.rbKitchenPrivate).isChecked -> "Riêng"
            else -> "Không"
        }
        data["bathroom"] = if (findViewById<RadioButton>(R.id.rbBathroomShared).isChecked) "Chung" else "Riêng"
        data["pet"] = if (findViewById<RadioButton>(R.id.rbPetYes).isChecked) "Cho nuôi" else "Không"
        data["petName"] = findViewById<EditText>(R.id.edtPetName).text.toString().trim()
        data["petCount"] = findViewById<EditText>(R.id.edtPetCount).text.toString().toIntOrNull() ?: 0
        
        data["genderPrefer"] = when {
            findViewById<RadioButton>(R.id.rbGenderMale).isChecked -> "Nam"
            findViewById<RadioButton>(R.id.rbGenderFemale).isChecked -> "Nữ"
            else -> "Tất cả"
        }
        data["curfew"] = if (findViewById<RadioButton>(R.id.rbCurfewCustom).isChecked) "Tùy chọn" else "Tự do"
        data["curfewTime"] = findViewById<TextView>(R.id.edtCurfewTime).text.toString().trim()

        saveParkingToMap("Motorbike", data)
        saveParkingToMap("EBike", data)
        saveParkingToMap("Bicycle", data)

        data["postExpiryDate"] = selectedExpiryMs ?: originalExpiryDate
        data["availableTimeSlots"] = dayRows.mapIndexed { i, (cb, tvStart, tvEnd) ->
            if (cb.isChecked) "${dayLabels[i]}: ${tvStart.text}-${tvEnd.text}" else null
        }.filterNotNull().joinToString("\n")

        // Lưu serviceItems
        data["serviceItems"] = serviceRows.mapNotNull { (name, price) ->
            val n = name.text.toString().trim()
            val p = NumberFormatUtils.getRawNumber(price).toLongOrNull() ?: 0L
            if (n.isNotEmpty()) mapOf<String, Any>("name" to n, "price" to p) else null
        }
        // Lưu tọa độ bản đồ nếu đã chọn
        selectedLatitude?.let { data["latitude"] = it }
        selectedLongitude?.let { data["longitude"] = it }
        if (selectedLocationAddress.isNotBlank()) data["locationAddress"] = selectedLocationAddress

        return data
    }

    private fun saveParkingToMap(type: String, map: HashMap<String, Any>) {
        val isChecked = findViewById<CheckBox>(resources.getIdentifier("cb$type", "id", packageName)).isChecked
        map["has$type"] = isChecked
        if (isChecked) {
            val isPaid = findViewById<RadioButton>(resources.getIdentifier("rb${type}Paid", "id", packageName)).isChecked
            val feeValue = if (isPaid) NumberFormatUtils.getRawNumber(findViewById(resources.getIdentifier("edt${type}Fee", "id", packageName))).toLongOrNull() ?: 0L else 0L
            val feeKey = if (type == "EBike") "eBikeFee" else "${type.lowercase()}Fee"
            map[feeKey] = feeValue
            // ebikeFee is deprecated and only kept in read-paths for backwards compatibility.
            // We only write to the standard eBikeFee key to maintain database cleanliness.
        }
    }

    private fun setupAccordionLogic() {
        val headers = listOf(
            findViewById<LinearLayout>(R.id.llHeaderCard1),
            findViewById<LinearLayout>(R.id.llHeaderCard2),
            findViewById<LinearLayout>(R.id.llHeaderCard3),
            findViewById<LinearLayout>(R.id.llHeaderCard4),
            findViewById<LinearLayout>(R.id.llHeaderCard5)
        )
        val contents = listOf(
            findViewById<LinearLayout>(R.id.llContentCard1),
            findViewById<LinearLayout>(R.id.llContentCard2),
            findViewById<LinearLayout>(R.id.llContentCard3),
            findViewById<LinearLayout>(R.id.llContentCard4),
            findViewById<LinearLayout>(R.id.llContentCard5)
        )
        val arrows = listOf(
            findViewById<ImageView>(R.id.ivArrowCard1),
            findViewById<ImageView>(R.id.ivArrowCard2),
            findViewById<ImageView>(R.id.ivArrowCard3),
            findViewById<ImageView>(R.id.ivArrowCard4),
            findViewById<ImageView>(R.id.ivArrowCard5)
        )
        val nextBtns = listOf(
            findViewById<View>(R.id.btnNextCard1),
            findViewById<View>(R.id.btnNextCard2),
            findViewById<View>(R.id.btnNextCard3),
            findViewById<View>(R.id.btnNextCard4),
            null
        )

        for (i in 0..4) {
            headers[i]?.setOnClickListener {
                val isVisible = contents[i]?.visibility == View.VISIBLE
                if (isVisible) {
                    contents[i]?.visibility = View.GONE
                    arrows[i]?.animate()?.rotation(0f)?.setDuration(200)?.start()
                } else {
                    contents[i]?.visibility = View.VISIBLE
                    arrows[i]?.animate()?.rotation(90f)?.setDuration(200)?.start()
                }
            }

            nextBtns[i]?.setOnClickListener {
                contents[i]?.visibility = View.GONE
                arrows[i]?.animate()?.rotation(0f)?.setDuration(200)?.start()
                if (i + 1 < 5) {
                    contents[i + 1]?.visibility = View.VISIBLE
                    arrows[i + 1]?.animate()?.rotation(90f)?.setDuration(200)?.start()
                }
            }
        }
    }

    private fun setupCard5() {
        val tvExpiryDisplay = findViewById<TextView>(R.id.tvExpiryDisplay)
        val containerExpiryPicker = findViewById<LinearLayout>(R.id.containerExpiryPicker)
        val expiryDateFormat = java.text.SimpleDateFormat("HH:mm, dd/MM/yyyy", java.util.Locale("vi", "VN"))

        fun openExpiryPicker() {
            val now = Calendar.getInstance()
            val maxCal = Calendar.getInstance().apply { add(Calendar.MONTH, 6) }
            val initCal = if (selectedExpiryMs != null && selectedExpiryMs!! > System.currentTimeMillis()) {
                Calendar.getInstance().apply { timeInMillis = selectedExpiryMs!! }
            } else now

            DatePickerDialog(
                this,
                { _, year, month, day ->
                    TimePickerDialog(
                        this,
                        { _, hour, minute ->
                            val selected = Calendar.getInstance().apply {
                                set(year, month, day, hour, minute, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis

                            if (selected <= System.currentTimeMillis()) {
                                android.widget.Toast.makeText(this, "Hạn hiển thị phải sau thời điểm hiện tại", android.widget.Toast.LENGTH_SHORT).show()
                                return@TimePickerDialog
                            }
                            if (selected > maxCal.timeInMillis) {
                                android.widget.Toast.makeText(this, "Hạn hiển thị không được quá 6 tháng kể từ hôm nay", android.widget.Toast.LENGTH_SHORT).show()
                                return@TimePickerDialog
                            }

                            selectedExpiryMs = selected
                            tvExpiryDisplay.text = expiryDateFormat.format(java.util.Date(selected))
                            tvExpiryDisplay.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_primary))
                        },
                        initCal.get(Calendar.HOUR_OF_DAY),
                        initCal.get(Calendar.MINUTE),
                        true
                    ).show()
                },
                initCal.get(Calendar.YEAR),
                initCal.get(Calendar.MONTH),
                initCal.get(Calendar.DAY_OF_MONTH)
            ).apply {
                datePicker.minDate = System.currentTimeMillis()
                datePicker.maxDate = maxCal.timeInMillis
                show()
            }
        }

        containerExpiryPicker.setOnClickListener { openExpiryPicker() }
        tvExpiryDisplay.setOnClickListener { openExpiryPicker() }

        val layoutRows = findViewById<LinearLayout>(R.id.layoutTimeSlotRows)
        dayRows.clear()
        for ((i, label) in dayLabels.withIndex()) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    .apply { bottomMargin = if (i < dayLabels.size - 1) dpToPx(8) else 0 }
            }
            val cb = CheckBox(this).apply {
                text = label; textSize = 13f
                setTextColor(androidx.core.content.ContextCompat.getColor(this@EditPostActivity, R.color.text_primary))
                buttonTintList = androidx.core.content.ContextCompat.getColorStateList(this@EditPostActivity, R.color.primary)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f)
            }
            val tvStart = TextView(this).apply {
                text = "08:00"; textSize = 13f
                setTextColor(androidx.core.content.ContextCompat.getColor(this@EditPostActivity, R.color.primary))
                background = androidx.core.content.ContextCompat.getDrawable(this@EditPostActivity, R.drawable.bg_edit_post)
                setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
                isEnabled = false; alpha = 0.4f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val tvSep = TextView(this).apply { text = " – "; textSize = 14f }
            val tvEnd = TextView(this).apply {
                text = "17:00"; textSize = 13f
                setTextColor(androidx.core.content.ContextCompat.getColor(this@EditPostActivity, R.color.primary))
                background = androidx.core.content.ContextCompat.getDrawable(this@EditPostActivity, R.drawable.bg_edit_post)
                setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
                isEnabled = false; alpha = 0.4f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            cb.setOnCheckedChangeListener { _, checked ->
                tvStart.isEnabled = checked; tvEnd.isEnabled = checked
                tvStart.alpha = if (checked) 1f else 0.4f; tvEnd.alpha = if (checked) 1f else 0.4f
            }
            fun showTimePicker(tv: TextView) {
                val parts = tv.text.toString().split(":").mapNotNull { it.toIntOrNull() }
                android.app.TimePickerDialog(this, { _, h, m -> tv.text = String.format("%02d:%02d", h, m) },
                    parts.getOrElse(0) { 8 }, parts.getOrElse(1) { 0 }, true).show()
            }
            tvStart.setOnClickListener { if (cb.isChecked) showTimePicker(tvStart) }
            tvEnd.setOnClickListener { if (cb.isChecked) showTimePicker(tvEnd) }
            row.addView(cb); row.addView(tvStart); row.addView(tvSep); row.addView(tvEnd)
            layoutRows.addView(row)
            dayRows.add(Triple(cb, tvStart, tvEnd))
        }
    }

    private fun confirmDiscard() {
        if (!isFormLoaded) {
            finish()
            return
        }
        MessageUtils.showConfirmDialog(
            context = this,
            title = "Bỏ thay đổi?",
            message = "Các chỉnh sửa chưa được lưu sẽ bị mất. Bạn có muốn thoát không?",
            positiveText = "Thoát",
            negativeText = "Tiếp tục chỉnh sửa",
            onConfirm = { finish() }
        )
    }

    private fun showRulesDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_rules, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAcceptRules)
            ?.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
