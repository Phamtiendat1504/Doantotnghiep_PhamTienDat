package com.example.doantotnghiep.View.Auth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.AddressData
import com.example.doantotnghiep.Utils.NumberFormatUtils
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.addCallback
class EditPostActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val imageUris = mutableListOf<Uri>()
    private var existingImageUrls = mutableListOf<String>()
    private val MAX_PHOTOS = 10
    private var roomId = ""

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null && (imageUris.size + existingImageUrls.size) < MAX_PHOTOS) {
                imageUris.add(uri)
                addPhotoToLayout(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_post)
        // Bấm nút back điện thoại để quay lại
        onBackPressedDispatcher.addCallback(this) { finish() }
        roomId = intent.getStringExtra("roomId") ?: ""
        if (roomId.isEmpty()) { finish(); return }

        // Đổi header
        findViewById<TextView>(android.R.id.text1)?.text = "Sửa bài đăng"

        // Đổi nút đăng bài thành nút cập nhật
        val btnPost = findViewById<MaterialButton>(R.id.btnPostRoom)
        btnPost.text = "Cập nhật và đăng lại"
        // Setup spinner
        val spinnerWard = findViewById<Spinner>(R.id.spinnerWard)
        val allAreas = mutableListOf("-- Chọn phường/xã --")
        allAreas.addAll(AddressData.phuongList.drop(1))
        allAreas.addAll(AddressData.xaList.drop(1))
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, allAreas)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerWard.adapter = adapter

        // Format giá
        NumberFormatUtils.addFormatWatcher(findViewById(R.id.edtPrice))
        NumberFormatUtils.addFormatWatcher(findViewById(R.id.edtWifiPrice))
        NumberFormatUtils.addFormatWatcher(findViewById(R.id.edtElectricPrice))
        NumberFormatUtils.addFormatWatcher(findViewById(R.id.edtWaterPrice))
        NumberFormatUtils.addFormatWatcher(findViewById(R.id.edtDepositAmount))

        // Checkbox listeners
        setupCheckboxListeners()

        // Thêm ảnh
        findViewById<CardView>(R.id.btnAddPhoto).setOnClickListener {
            if ((imageUris.size + existingImageUrls.size) >= MAX_PHOTOS) {
                Toast.makeText(this, "Tối đa $MAX_PHOTOS ảnh", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            pickImageLauncher.launch(intent)
        }

        // Load dữ liệu cũ
        loadRoomData()

        // Nút cập nhật
        btnPost.setOnClickListener { updatePost() }
    }

    private fun setupCheckboxListeners() {
        val cbWifi = findViewById<CheckBox>(R.id.cbWifi)
        val edtWifiPrice = findViewById<EditText>(R.id.edtWifiPrice)
        cbWifi.setOnCheckedChangeListener { _, isChecked ->
            edtWifiPrice.isEnabled = isChecked
            if (!isChecked) edtWifiPrice.text?.clear()
        }

        val cbElectric = findViewById<CheckBox>(R.id.cbElectric)
        val edtElectricPrice = findViewById<EditText>(R.id.edtElectricPrice)
        cbElectric.setOnCheckedChangeListener { _, isChecked ->
            edtElectricPrice.isEnabled = isChecked
            if (!isChecked) edtElectricPrice.text?.clear()
        }

        val cbWater = findViewById<CheckBox>(R.id.cbWater)
        val edtWaterPrice = findViewById<EditText>(R.id.edtWaterPrice)
        cbWater.setOnCheckedChangeListener { _, isChecked ->
            edtWaterPrice.isEnabled = isChecked
            if (!isChecked) edtWaterPrice.text?.clear()
        }

        val rgPet = findViewById<RadioGroup>(R.id.rgPet)
        val layoutPetDetail = findViewById<LinearLayout>(R.id.layoutPetDetail)
        rgPet.setOnCheckedChangeListener { _, checkedId ->
            layoutPetDetail.visibility = if (checkedId == R.id.rbPetYes) View.VISIBLE else View.GONE
        }

        val rgCurfew = findViewById<RadioGroup>(R.id.rgCurfew)
        val edtCurfewTime = findViewById<EditText>(R.id.edtCurfewTime)
        rgCurfew.setOnCheckedChangeListener { _, checkedId ->
            edtCurfewTime.visibility = if (checkedId == R.id.rbCurfewCustom) View.VISIBLE else View.GONE
        }

        // Xe
        setupParkingListeners()
    }

    private fun setupParkingListeners() {
        val cbMotorbike = findViewById<CheckBox>(R.id.cbMotorbike) ?: return
        val rgMotorbikeFee = findViewById<RadioGroup>(R.id.rgMotorbikeFee) ?: return
        val edtMotorbikeFee = findViewById<EditText>(R.id.edtMotorbikeFee) ?: return

        cbMotorbike.setOnCheckedChangeListener { _, isChecked ->
            rgMotorbikeFee.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) { edtMotorbikeFee.visibility = View.GONE; edtMotorbikeFee.text?.clear() }
        }
        rgMotorbikeFee.setOnCheckedChangeListener { _, checkedId ->
            edtMotorbikeFee.visibility = if (checkedId == R.id.rbMotorbikePaid) View.VISIBLE else View.GONE
        }

        val cbEBike = findViewById<CheckBox>(R.id.cbEBike) ?: return
        val rgEBikeFee = findViewById<RadioGroup>(R.id.rgEBikeFee) ?: return
        val edtEBikeFee = findViewById<EditText>(R.id.edtEBikeFee) ?: return

        cbEBike.setOnCheckedChangeListener { _, isChecked ->
            rgEBikeFee.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) { edtEBikeFee.visibility = View.GONE; edtEBikeFee.text?.clear() }
        }
        rgEBikeFee.setOnCheckedChangeListener { _, checkedId ->
            edtEBikeFee.visibility = if (checkedId == R.id.rbEBikePaid) View.VISIBLE else View.GONE
        }

        val cbBicycle = findViewById<CheckBox>(R.id.cbBicycle) ?: return
        val rgBicycleFee = findViewById<RadioGroup>(R.id.rgBicycleFee) ?: return
        val edtBicycleFee = findViewById<EditText>(R.id.edtBicycleFee) ?: return

        cbBicycle.setOnCheckedChangeListener { _, isChecked ->
            rgBicycleFee.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) { edtBicycleFee.visibility = View.GONE; edtBicycleFee.text?.clear() }
        }
        rgBicycleFee.setOnCheckedChangeListener { _, checkedId ->
            edtBicycleFee.visibility = if (checkedId == R.id.rbBicyclePaid) View.VISIBLE else View.GONE
        }

        NumberFormatUtils.addFormatWatcher(edtMotorbikeFee)
        NumberFormatUtils.addFormatWatcher(edtEBikeFee)
        NumberFormatUtils.addFormatWatcher(edtBicycleFee)
    }

    // ═══ LOAD DỮ LIỆU CŨ ═══
    private fun loadRoomData() {
        db.collection("rooms").document(roomId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) { finish(); return@addOnSuccessListener }
                val d = doc.data ?: return@addOnSuccessListener

                // Thông tin chủ trọ
                findViewById<EditText>(R.id.edtOwnerName).setText(d["ownerName"] as? String ?: "")
                findViewById<EditText>(R.id.edtOwnerPhone).setText(d["ownerPhone"] as? String ?: "")
                when (d["ownerGender"] as? String) {
                    "Nam" -> findViewById<RadioGroup>(R.id.rgOwnerGender).check(R.id.rbOwnerMale)
                    "Nữ" -> findViewById<RadioGroup>(R.id.rgOwnerGender).check(R.id.rbOwnerFemale)
                }

                // Thông tin bài đăng
                findViewById<EditText>(R.id.edtTitle).setText(d["title"] as? String ?: "")
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))
                findViewById<TextView>(R.id.tvPostDate).text = dateFormat.format(Date())
                findViewById<EditText>(R.id.edtAddress).setText(d["address"] as? String ?: "")
                findViewById<EditText>(R.id.edtDescription).setText(d["description"] as? String ?: "")

                // Spinner khu vực
                val ward = d["ward"] as? String ?: ""
                val district = d["district"] as? String ?: ""
                val spinnerWard = findViewById<Spinner>(R.id.spinnerWard)
                for (i in 0 until spinnerWard.adapter.count) {
                    val item = spinnerWard.adapter.getItem(i).toString()
                    if (item.contains(ward) && item.contains(district)) {
                        spinnerWard.setSelection(i)
                        break
                    }
                }

                // Chi tiết
                val price = d["price"] as? Long ?: 0
                if (price > 0) findViewById<EditText>(R.id.edtPrice).setText(price.toString())
                val area = (d["area"] as? Long)?.toInt() ?: (d["area"] as? Int ?: 0)
                if (area > 0) findViewById<EditText>(R.id.edtArea).setText(area.toString())
                val people = (d["peopleCount"] as? Long)?.toInt() ?: 0
                if (people > 0) findViewById<EditText>(R.id.edtPeopleCount).setText(people.toString())

                when (d["roomType"] as? String) {
                    "Chung chủ" -> findViewById<RadioGroup>(R.id.rgRoomStyle).check(R.id.rbShared)
                    "Riêng chủ" -> findViewById<RadioGroup>(R.id.rgRoomStyle).check(R.id.rbPrivate)
                }

                val depositMonths = (d["depositMonths"] as? Long)?.toInt() ?: 0
                if (depositMonths > 0) findViewById<EditText>(R.id.edtDepositMonths).setText(depositMonths.toString())
                val depositAmount = d["depositAmount"] as? Long ?: 0
                if (depositAmount > 0) findViewById<EditText>(R.id.edtDepositAmount).setText(depositAmount.toString())

                // Tiện ích
                if (d["hasWifi"] == true) { findViewById<CheckBox>(R.id.cbWifi).isChecked = true }
                val wifiPrice = d["wifiPrice"] as? Long ?: 0
                if (wifiPrice > 0) findViewById<EditText>(R.id.edtWifiPrice).setText(wifiPrice.toString())

                if (d["hasAirCon"] == true) findViewById<CheckBox>(R.id.cbAirCon).isChecked = true
                if (d["hasWaterHeater"] == true) findViewById<CheckBox>(R.id.cbWaterHeater).isChecked = true
                if (d["hasWasher"] == true) findViewById<CheckBox>(R.id.cbWasher).isChecked = true
                if (d["hasDryingArea"] == true) findViewById<CheckBox>(R.id.cbDryingArea).isChecked = true
                if (d["hasWardrobe"] == true) findViewById<CheckBox>(R.id.cbWardrobe).isChecked = true
                if (d["hasBed"] == true) findViewById<CheckBox>(R.id.cbBed).isChecked = true

                val electricPrice = d["electricPrice"] as? Long ?: 0
                if (electricPrice > 0) {
                    findViewById<CheckBox>(R.id.cbElectric).isChecked = true
                    findViewById<EditText>(R.id.edtElectricPrice).setText(electricPrice.toString())
                }
                val waterPrice = d["waterPrice"] as? Long ?: 0
                if (waterPrice > 0) {
                    findViewById<CheckBox>(R.id.cbWater).isChecked = true
                    findViewById<EditText>(R.id.edtWaterPrice).setText(waterPrice.toString())
                }

                // Bếp, vệ sinh, giới tính, thú cưng, giờ giấc
                when (d["kitchen"] as? String) {
                    "Chung" -> findViewById<RadioGroup>(R.id.rgKitchen).check(R.id.rbKitchenShared)
                    "Riêng" -> findViewById<RadioGroup>(R.id.rgKitchen).check(R.id.rbKitchenPrivate)
                    "Không" -> findViewById<RadioGroup>(R.id.rgKitchen).check(R.id.rbKitchenNone)
                }
                when (d["bathroom"] as? String) {
                    "Chung" -> findViewById<RadioGroup>(R.id.rgBathroom).check(R.id.rbBathroomShared)
                    "Riêng" -> findViewById<RadioGroup>(R.id.rgBathroom).check(R.id.rbBathroomPrivate)
                }
                when (d["genderPrefer"] as? String) {
                    "Nam" -> findViewById<RadioGroup>(R.id.rgGenderPrefer).check(R.id.rbGenderMale)
                    "Nữ" -> findViewById<RadioGroup>(R.id.rgGenderPrefer).check(R.id.rbGenderFemale)
                    "Tất cả" -> findViewById<RadioGroup>(R.id.rgGenderPrefer).check(R.id.rbGenderAll)
                }
                when (d["pet"] as? String) {
                    "Cho nuôi" -> {
                        findViewById<RadioGroup>(R.id.rgPet).check(R.id.rbPetYes)
                        findViewById<EditText>(R.id.edtPetName).setText(d["petName"] as? String ?: "")
                        val petCount = (d["petCount"] as? Long)?.toInt() ?: 0
                        if (petCount > 0) findViewById<EditText>(R.id.edtPetCount).setText(petCount.toString())
                    }
                    "Không" -> findViewById<RadioGroup>(R.id.rgPet).check(R.id.rbPetNo)
                }
                when (d["curfew"] as? String) {
                    "Tự do" -> findViewById<RadioGroup>(R.id.rgCurfew).check(R.id.rbCurfewFree)
                    "Tùy chọn" -> {
                        findViewById<RadioGroup>(R.id.rgCurfew).check(R.id.rbCurfewCustom)
                        findViewById<EditText>(R.id.edtCurfewTime).setText(d["curfewTime"] as? String ?: "")
                    }
                }

                // Xe
                if (d["hasMotorbike"] == true) findViewById<CheckBox>(R.id.cbMotorbike)?.isChecked = true
                if (d["hasEBike"] == true) findViewById<CheckBox>(R.id.cbEBike)?.isChecked = true
                if (d["hasBicycle"] == true) findViewById<CheckBox>(R.id.cbBicycle)?.isChecked = true

                // Ảnh cũ
                existingImageUrls = (d["imageUrls"] as? List<String>)?.toMutableList() ?: mutableListOf()
                loadExistingPhotos()
            }
    }

    private fun loadExistingPhotos() {
        val layoutPhotos = findViewById<LinearLayout>(R.id.layoutPhotos)
        val tvPhotoCount = findViewById<TextView>(R.id.tvPhotoCount)

        for (url in existingImageUrls) {
            val imgView = ImageView(this)
            val params = LinearLayout.LayoutParams(dpToPx(90), dpToPx(90))
            params.marginEnd = dpToPx(8)
            imgView.layoutParams = params
            imgView.scaleType = ImageView.ScaleType.CENTER_CROP
            Glide.with(this).load(url).centerCrop().into(imgView)

            imgView.setOnLongClickListener {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Xóa ảnh")
                    .setMessage("Bạn có muốn xóa ảnh này?")
                    .setPositiveButton("Xóa") { _, _ ->
                        existingImageUrls.remove(url)
                        layoutPhotos.removeView(imgView)
                        tvPhotoCount.text = "${imageUris.size + existingImageUrls.size}/$MAX_PHOTOS ảnh"
                    }
                    .setNegativeButton("Hủy", null)
                    .show()
                true
            }

            layoutPhotos.addView(imgView, layoutPhotos.childCount - 1)
        }
        tvPhotoCount.text = "${existingImageUrls.size}/$MAX_PHOTOS ảnh"
    }

    private fun addPhotoToLayout(uri: Uri) {
        val layoutPhotos = findViewById<LinearLayout>(R.id.layoutPhotos)
        val tvPhotoCount = findViewById<TextView>(R.id.tvPhotoCount)

        val imgView = ImageView(this)
        val params = LinearLayout.LayoutParams(dpToPx(90), dpToPx(90))
        params.marginEnd = dpToPx(8)
        imgView.layoutParams = params
        imgView.scaleType = ImageView.ScaleType.CENTER_CROP
        imgView.setImageURI(uri)

        imgView.setOnLongClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xóa ảnh")
                .setMessage("Bạn có muốn xóa ảnh này?")
                .setPositiveButton("Xóa") { _, _ ->
                    val idx = layoutPhotos.indexOfChild(imgView) - existingImageUrls.size
                    if (idx >= 0 && idx < imageUris.size) imageUris.removeAt(idx)
                    layoutPhotos.removeView(imgView)
                    tvPhotoCount.text = "${imageUris.size + existingImageUrls.size}/$MAX_PHOTOS ảnh"
                }
                .setNegativeButton("Hủy", null)
                .show()
            true
        }

        layoutPhotos.addView(imgView, layoutPhotos.childCount - 1)
        tvPhotoCount.text = "${imageUris.size + existingImageUrls.size}/$MAX_PHOTOS ảnh"
    }

    // ═══ CẬP NHẬT BÀI ĐĂNG ═══
    private fun updatePost() {
        val selectedWard = findViewById<Spinner>(R.id.spinnerWard).selectedItem?.toString() ?: ""
        if (findViewById<Spinner>(R.id.spinnerWard).selectedItemPosition == 0) {
            Toast.makeText(this, "Vui lòng chọn khu vực", Toast.LENGTH_SHORT).show()
            return
        }

        val wardName = if (selectedWard.contains("(")) selectedWard.substringBefore("(").trim() else selectedWard
        val districtName = if (selectedWard.contains("(")) selectedWard.substringAfter("(").replace(")", "").trim() else ""

        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val btnPost = findViewById<MaterialButton>(R.id.btnPostRoom)
        progressBar.visibility = View.VISIBLE
        btnPost.isEnabled = false
        btnPost.text = "Đang cập nhật..."

        // Upload ảnh mới nếu có
        if (imageUris.isNotEmpty()) {
            uploadNewImages { newUrls ->
                val allUrls = existingImageUrls + newUrls
                saveUpdate(wardName, districtName, allUrls, progressBar, btnPost)
            }
        } else {
            saveUpdate(wardName, districtName, existingImageUrls, progressBar, btnPost)
        }
    }

    private fun uploadNewImages(onComplete: (List<String>) -> Unit) {
        val urls = mutableListOf<String>()
        var uploaded = 0

        for ((index, uri) in imageUris.withIndex()) {
            val ref = storage.reference.child("rooms/$roomId/edit_img_${System.currentTimeMillis()}_$index.jpg")
            ref.putFile(uri).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUrl ->
                    urls.add(downloadUrl.toString())
                    uploaded++
                    if (uploaded == imageUris.size) onComplete(urls)
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Upload ảnh thất bại", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveUpdate(wardName: String, districtName: String, allImageUrls: List<String>,
                           progressBar: ProgressBar, btnPost: MaterialButton) {

        val cbMotorbike = findViewById<CheckBox>(R.id.cbMotorbike)
        val cbEBike = findViewById<CheckBox>(R.id.cbEBike)
        val cbBicycle = findViewById<CheckBox>(R.id.cbBicycle)
        val rgMotorbikeFee = findViewById<RadioGroup>(R.id.rgMotorbikeFee)
        val rgEBikeFee = findViewById<RadioGroup>(R.id.rgEBikeFee)
        val rgBicycleFee = findViewById<RadioGroup>(R.id.rgBicycleFee)
        val edtMotorbikeFee = findViewById<EditText>(R.id.edtMotorbikeFee)
        val edtEBikeFee = findViewById<EditText>(R.id.edtEBikeFee)
        val edtBicycleFee = findViewById<EditText>(R.id.edtBicycleFee)

        val updates = hashMapOf<String, Any>(
            "ownerName" to findViewById<EditText>(R.id.edtOwnerName).text.toString().trim(),
            "ownerPhone" to findViewById<EditText>(R.id.edtOwnerPhone).text.toString().trim(),
            "ownerGender" to when (findViewById<RadioGroup>(R.id.rgOwnerGender).checkedRadioButtonId) {
                R.id.rbOwnerMale -> "Nam"; R.id.rbOwnerFemale -> "Nữ"; else -> ""
            },
            "title" to findViewById<EditText>(R.id.edtTitle).text.toString().trim(),
            "ward" to wardName,
            "district" to districtName,
            "address" to findViewById<EditText>(R.id.edtAddress).text.toString().trim(),
            "description" to findViewById<EditText>(R.id.edtDescription).text.toString().trim(),
            "price" to (NumberFormatUtils.getRawNumber(findViewById(R.id.edtPrice)).toLongOrNull() ?: 0L),
            "area" to (findViewById<EditText>(R.id.edtArea).text.toString().toIntOrNull() ?: 0),
            "peopleCount" to (findViewById<EditText>(R.id.edtPeopleCount).text.toString().toIntOrNull() ?: 0),
            "roomType" to when (findViewById<RadioGroup>(R.id.rgRoomStyle).checkedRadioButtonId) {
                R.id.rbShared -> "Chung chủ"; R.id.rbPrivate -> "Riêng chủ"; else -> ""
            },
            "depositMonths" to (findViewById<EditText>(R.id.edtDepositMonths).text.toString().toIntOrNull() ?: 0),
            "depositAmount" to (NumberFormatUtils.getRawNumber(findViewById(R.id.edtDepositAmount)).toLongOrNull() ?: 0L),
            "hasWifi" to findViewById<CheckBox>(R.id.cbWifi).isChecked,
            "wifiPrice" to (NumberFormatUtils.getRawNumber(findViewById(R.id.edtWifiPrice)).toLongOrNull() ?: 0L),
            "electricPrice" to (NumberFormatUtils.getRawNumber(findViewById(R.id.edtElectricPrice)).toLongOrNull() ?: 0L),
            "waterPrice" to (NumberFormatUtils.getRawNumber(findViewById(R.id.edtWaterPrice)).toLongOrNull() ?: 0L),
            "hasAirCon" to findViewById<CheckBox>(R.id.cbAirCon).isChecked,
            "hasWaterHeater" to findViewById<CheckBox>(R.id.cbWaterHeater).isChecked,
            "hasWasher" to findViewById<CheckBox>(R.id.cbWasher).isChecked,
            "hasDryingArea" to findViewById<CheckBox>(R.id.cbDryingArea).isChecked,
            "hasWardrobe" to findViewById<CheckBox>(R.id.cbWardrobe).isChecked,
            "hasBed" to findViewById<CheckBox>(R.id.cbBed).isChecked,
            "kitchen" to when (findViewById<RadioGroup>(R.id.rgKitchen).checkedRadioButtonId) {
                R.id.rbKitchenShared -> "Chung"; R.id.rbKitchenPrivate -> "Riêng"; R.id.rbKitchenNone -> "Không"; else -> ""
            },
            "bathroom" to when (findViewById<RadioGroup>(R.id.rgBathroom).checkedRadioButtonId) {
                R.id.rbBathroomShared -> "Chung"; R.id.rbBathroomPrivate -> "Riêng"; else -> ""
            },
            "pet" to when (findViewById<RadioGroup>(R.id.rgPet).checkedRadioButtonId) {
                R.id.rbPetYes -> "Cho nuôi"; R.id.rbPetNo -> "Không"; else -> ""
            },
            "petName" to findViewById<EditText>(R.id.edtPetName).text.toString().trim(),
            "petCount" to (findViewById<EditText>(R.id.edtPetCount).text.toString().toIntOrNull() ?: 0),
            "genderPrefer" to when (findViewById<RadioGroup>(R.id.rgGenderPrefer).checkedRadioButtonId) {
                R.id.rbGenderMale -> "Nam"; R.id.rbGenderFemale -> "Nữ"; R.id.rbGenderAll -> "Tất cả"; else -> ""
            },
            "curfew" to when (findViewById<RadioGroup>(R.id.rgCurfew).checkedRadioButtonId) {
                R.id.rbCurfewFree -> "Tự do"; R.id.rbCurfewCustom -> "Tùy chọn"; else -> ""
            },
            "curfewTime" to findViewById<EditText>(R.id.edtCurfewTime).text.toString().trim(),
            "hasMotorbike" to (cbMotorbike?.isChecked ?: false),
            "motorbikeFee" to if (cbMotorbike?.isChecked == true && rgMotorbikeFee?.checkedRadioButtonId == R.id.rbMotorbikePaid)
                (NumberFormatUtils.getRawNumber(edtMotorbikeFee).toLongOrNull() ?: 0L) else 0L,
            "hasEBike" to (cbEBike?.isChecked ?: false),
            "eBikeFee" to if (cbEBike?.isChecked == true && rgEBikeFee?.checkedRadioButtonId == R.id.rbEBikePaid)
                (NumberFormatUtils.getRawNumber(edtEBikeFee).toLongOrNull() ?: 0L) else 0L,
            "hasBicycle" to (cbBicycle?.isChecked ?: false),
            "bicycleFee" to if (cbBicycle?.isChecked == true && rgBicycleFee?.checkedRadioButtonId == R.id.rbBicyclePaid)
                (NumberFormatUtils.getRawNumber(edtBicycleFee).toLongOrNull() ?: 0L) else 0L,
            "imageUrls" to allImageUrls,
            "status" to "pending",
            "rejectReason" to "",
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("rooms").document(roomId)
            .update(updates)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                btnPost.isEnabled = true
                btnPost.text = "Cập nhật và đăng lại"

                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Cập nhật thành công!")
                    .setMessage("Bài đăng đã được sửa và gửi lại chờ admin duyệt.")
                    .setPositiveButton("OK") { _, _ -> finish() }
                    .setCancelable(false)
                    .show()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                btnPost.isEnabled = true
                btnPost.text = "Cập nhật và đăng lại"
                Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}