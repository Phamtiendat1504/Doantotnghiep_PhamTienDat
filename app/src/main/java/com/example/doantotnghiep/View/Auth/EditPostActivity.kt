package com.example.doantotnghiep.View.Auth

import android.app.Activity
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

class EditPostActivity : AppCompatActivity() {

    private val viewModel: EditPostViewModel by viewModels()

    private var existingImageUrls = mutableListOf<String>()
    private val newImageUris = mutableListOf<Uri>()
    private val deletedImageUrls = mutableListOf<String>()
    
    private val MAX_PHOTOS = 10
    private var roomId = ""
    
    private var replaceIndex = -1
    private var isReplacingExisting = false

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data?.clipData != null) {
                val count = data.clipData!!.itemCount
                for (i in 0 until count) {
                    val uri = data.clipData!!.getItemAt(i).uri
                    if ((existingImageUrls.size + newImageUris.size) < MAX_PHOTOS) {
                        newImageUris.add(uri)
                    }
                }
                renderPhotos()
            } else if (data?.data != null) {
                val uri = data.data!!
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_post)
        
        onBackPressedDispatcher.addCallback(this) { finish() }
        
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
            setOnClickListener { finish() }
        }

        findViewById<TextView>(R.id.tvHeaderTitle).text = "Sửa bài đăng"
        findViewById<TextView>(R.id.tvHeaderSubTitle).text = "Chạm vào ảnh để thay đổi hoặc xóa"
        findViewById<Button>(R.id.btnPostRoom)?.text = "Cập nhật bài viết"
        
        val spinnerWard = findViewById<Spinner>(R.id.spinnerWard)
        val allAreas = mutableListOf("-- Chọn phường/xã --").apply {
            addAll(AddressData.phuongList.drop(1))
            addAll(AddressData.xaList.drop(1))
        }
        spinnerWard.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, allAreas).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val priceIds = listOf(R.id.edtPrice, R.id.edtWifiPrice, R.id.edtElectricPrice, R.id.edtWaterPrice, 
               R.id.edtDepositAmount, R.id.edtMotorbikeFee, R.id.edtEBikeFee, R.id.edtBicycleFee)
        priceIds.forEach { id -> findViewById<EditText>(id)?.let { NumberFormatUtils.addFormatWatcher(it) } }

        setupMainListeners()
        setupParkingListeners()
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
        if (d["status"] == "rejected") addRejectionBanner(d["rejectReason"] as? String ?: "")

        findViewById<EditText>(R.id.edtOwnerName).setText(d["ownerName"] as? String ?: "")
        findViewById<EditText>(R.id.edtOwnerPhone).setText(d["ownerPhone"] as? String ?: "")
        if (d["ownerGender"] == "Nam") findViewById<RadioButton>(R.id.rbOwnerMale).isChecked = true
        else if (d["ownerGender"] == "Nữ") findViewById<RadioButton>(R.id.rbOwnerFemale).isChecked = true

        findViewById<EditText>(R.id.edtTitle).setText(d["title"] as? String ?: "")
        findViewById<EditText>(R.id.edtAddress).setText(d["address"] as? String ?: "")
        findViewById<EditText>(R.id.edtDescription).setText(d["description"] as? String ?: "")
        
        val ward = d["ward"] as? String ?: ""
        val spinner = findViewById<Spinner>(R.id.spinnerWard)
        for (i in 0 until spinner.count) {
            if (spinner.getItemAtPosition(i).toString().contains(ward)) { spinner.setSelection(i); break }
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
        findViewById<CheckBox>(R.id.cbElectric).isChecked = (d["electricPrice"] as? Long ?: 0) > 0
        findViewById<EditText>(R.id.edtElectricPrice).setText(d["electricPrice"]?.toString() ?: "")
        findViewById<CheckBox>(R.id.cbWater).isChecked = (d["waterPrice"] as? Long ?: 0) > 0
        findViewById<EditText>(R.id.edtWaterPrice).setText(d["waterPrice"]?.toString() ?: "")

        findViewById<CheckBox>(R.id.cbAirCon).isChecked = d["hasAirCon"] as? Boolean ?: false
        findViewById<CheckBox>(R.id.cbWaterHeater).isChecked = d["hasWaterHeater"] as? Boolean ?: false
        findViewById<CheckBox>(R.id.cbWasher).isChecked = d["hasWasher"] as? Boolean ?: false
        findViewById<CheckBox>(R.id.cbDryingArea).isChecked = d["hasDryingArea"] as? Boolean ?: false
        findViewById<CheckBox>(R.id.cbWardrobe).isChecked = d["hasWardrobe"] as? Boolean ?: false
        findViewById<CheckBox>(R.id.cbBed).isChecked = d["hasBed"] as? Boolean ?: false

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
                
                // Parse time string (e.g., "11:30 PM") and set to NumberPickers
                try {
                    val parts = curfewTime.split(" ")
                    if (parts.size == 2) {
                        val timeParts = parts[0].split(":")
                        if (timeParts.size == 2) {
                            findViewById<NumberPicker>(R.id.pickerHour).value = timeParts[0].toInt()
                            findViewById<NumberPicker>(R.id.pickerMinute).value = timeParts[1].toInt()
                            findViewById<NumberPicker>(R.id.pickerAmPm).value = if (parts[1] == "AM") 0 else 1
                        }
                    }
                } catch (e: Exception) {}
            }
        }

        when (d["genderPrefer"] as? String ?: "") {
            "Nam" -> findViewById<RadioButton>(R.id.rbGenderMale).isChecked = true
            "Nữ" -> findViewById<RadioButton>(R.id.rbGenderFemale).isChecked = true
            else -> findViewById<RadioButton>(R.id.rbGenderAll).isChecked = true
        }

        loadParkingData("Motorbike", d); loadParkingData("EBike", d); loadParkingData("Bicycle", d)

        existingImageUrls = (d["imageUrls"] as? List<String>)?.toMutableList() ?: mutableListOf()
        renderPhotos()
    }

    private fun loadParkingData(type: String, d: Map<String, Any>) {
        val hasKey = "has$type"
        val cb = findViewById<CheckBox>(resources.getIdentifier("cb$type", "id", packageName))
        val rg = findViewById<RadioGroup>(resources.getIdentifier("rg${type}Fee", "id", packageName))
        if (d[hasKey] as? Boolean == true) {
            cb.isChecked = true; rg.visibility = View.VISIBLE
            val fee = d["${type.lowercase()}Fee"] as? Long ?: 0
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
        val options = arrayOf("🔍 Xem ảnh phóng to", "🔄 Thay đổi ảnh khác", "🗑️ Xóa ảnh này")
        
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
        val layoutProgress = findViewById<LinearLayout>(R.id.layoutProgress)
        layoutProgress?.visibility = View.VISIBLE
        findViewById<TextView>(R.id.tvProgressPercent)?.text = "Đang đăng bài..."

        val spinnerWard = findViewById<Spinner>(R.id.spinnerWard)
        if (spinnerWard.selectedItemPosition == 0) {
            layoutProgress?.visibility = View.GONE
            MessageUtils.showInfoDialog(this, "Thông tin thiếu", "Vui lòng chọn khu vực phường/xã.")
            return
        }

        val wardWithDistrict = spinnerWard.selectedItem.toString()
        val ward = wardWithDistrict.substringBefore("(").trim()
        val district = wardWithDistrict.substringAfter("(").replace(")", "").trim()

        val data = buildFormData()
        
        // Gọi ViewModel thay vì Firebase trực tiếp
        viewModel.updatePost(
            roomId = roomId,
            ward = ward,
            district = district,
            existingImageUrls = existingImageUrls,
            newImageUris = newImageUris,
            deletedImageUrls = deletedImageUrls,
            data = data
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
        data["electricPrice"] = NumberFormatUtils.getRawNumber(findViewById(R.id.edtElectricPrice)).toLongOrNull() ?: 0L
        data["waterPrice"] = NumberFormatUtils.getRawNumber(findViewById(R.id.edtWaterPrice)).toLongOrNull() ?: 0L
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

        return data
    }

    private fun saveParkingToMap(type: String, map: HashMap<String, Any>) {
        val isChecked = findViewById<CheckBox>(resources.getIdentifier("cb$type", "id", packageName)).isChecked
        map["has$type"] = isChecked
        if (isChecked) {
            val isPaid = findViewById<RadioButton>(resources.getIdentifier("rb${type}Paid", "id", packageName)).isChecked
            map["${type.lowercase()}Fee"] = if (isPaid) NumberFormatUtils.getRawNumber(findViewById(resources.getIdentifier("edt${type}Fee", "id", packageName))).toLongOrNull() ?: 0L else 0L
        }
    }

    private fun setupAccordionLogic() {
        val headers = listOf(
            findViewById<LinearLayout>(R.id.llHeaderCard1),
            findViewById<LinearLayout>(R.id.llHeaderCard2),
            findViewById<LinearLayout>(R.id.llHeaderCard3),
            findViewById<LinearLayout>(R.id.llHeaderCard4)
        )
        val contents = listOf(
            findViewById<LinearLayout>(R.id.llContentCard1),
            findViewById<LinearLayout>(R.id.llContentCard2),
            findViewById<LinearLayout>(R.id.llContentCard3),
            findViewById<LinearLayout>(R.id.llContentCard4)
        )
        val arrows = listOf(
            findViewById<ImageView>(R.id.ivArrowCard1),
            findViewById<ImageView>(R.id.ivArrowCard2),
            findViewById<ImageView>(R.id.ivArrowCard3),
            findViewById<ImageView>(R.id.ivArrowCard4)
        )
        val nextBtns = listOf(
            findViewById<View>(R.id.btnNextCard1),
            findViewById<View>(R.id.btnNextCard2),
            findViewById<View>(R.id.btnNextCard3),
            null
        )

        for (i in 0..3) {
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
                if (i + 1 < 4) {
                    contents[i + 1]?.visibility = View.VISIBLE
                    arrows[i + 1]?.animate()?.rotation(90f)?.setDuration(200)?.start()
                }
            }
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
