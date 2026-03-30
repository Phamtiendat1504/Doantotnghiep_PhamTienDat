package com.example.doantotnghiep.View.Fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Model.Room
import com.example.doantotnghiep.Utils.AddressData
import com.example.doantotnghiep.Utils.NumberFormatUtils
import com.example.doantotnghiep.View.Auth.VerifyLandlordActivity
import com.example.doantotnghiep.ViewModel.PostViewModel
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class PostFragment : Fragment() {

    private var verifyRequiredView: View? = null
    private var postFormView: View? = null

    private val db = FirebaseFirestore.getInstance()
    private val imageUris = mutableListOf<Uri>()
    private val MAX_PHOTOS = 10

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null && imageUris.size < MAX_PHOTOS) {
                imageUris.add(uri)
                addPhotoToLayout(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val frameLayout = FrameLayout(requireContext())
        frameLayout.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        verifyRequiredView = inflater.inflate(R.layout.layout_verify_required, container, false)
        postFormView = inflater.inflate(R.layout.fragment_post, container, false)

        frameLayout.addView(verifyRequiredView)
        frameLayout.addView(postFormView)

        return frameLayout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkUserRole()
        setupVerifyButton()
    }

    // ═══ KIỂM TRA VAI TRÒ ═══
    private fun checkUserRole() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                if (userDoc.exists()) {
                    val role = userDoc.getString("role") ?: "tenant"
                    if (role == "landlord" || role == "admin") {
                        showPostForm()
                    } else {
                        checkVerificationStatus(uid)
                    }
                }
            }
    }

    private fun checkVerificationStatus(uid: String) {
        db.collection("verifications").document(uid).get()
            .addOnSuccessListener { verifyDoc ->
                if (verifyDoc.exists()) {
                    when (verifyDoc.getString("status") ?: "none") {
                        "pending" -> showPendingStatus()
                        "rejected" -> showRejectedStatus(verifyDoc.getString("rejectReason") ?: "")
                        else -> showVerifyRequired()
                    }
                } else {
                    showVerifyRequired()
                }
            }
            .addOnFailureListener { showVerifyRequired() }
    }

    private fun showVerifyRequired() {
        verifyRequiredView?.visibility = View.VISIBLE
        postFormView?.visibility = View.GONE
        verifyRequiredView?.findViewById<TextView>(R.id.tvVerifyStatus)?.text =
            "Bạn cần xác minh là chủ trọ để có thể đăng bài cho thuê phòng trọ."
        verifyRequiredView?.findViewById<MaterialButton>(R.id.btnStartVerify)?.apply {
            text = "Xác minh ngay"
            visibility = View.VISIBLE
        }
    }

    private fun showPendingStatus() {
        verifyRequiredView?.visibility = View.VISIBLE
        postFormView?.visibility = View.GONE
        verifyRequiredView?.findViewById<TextView>(R.id.tvVerifyStatus)?.text =
            "Yêu cầu xác minh của bạn đã được gửi.\nVui lòng chờ admin phê duyệt."
        verifyRequiredView?.findViewById<MaterialButton>(R.id.btnStartVerify)?.visibility = View.GONE
    }

    private fun showRejectedStatus(reason: String) {
        verifyRequiredView?.visibility = View.VISIBLE
        postFormView?.visibility = View.GONE
        verifyRequiredView?.findViewById<TextView>(R.id.tvVerifyStatus)?.text =
            "Yêu cầu xác minh bị từ chối.\nLý do: $reason\n\nVui lòng gửi lại yêu cầu."
        verifyRequiredView?.findViewById<MaterialButton>(R.id.btnStartVerify)?.apply {
            text = "Gửi lại xác minh"
            visibility = View.VISIBLE
        }
    }

    private fun showPostForm() {
        verifyRequiredView?.visibility = View.GONE
        postFormView?.visibility = View.VISIBLE
        setupPostForm()
    }

    private fun setupVerifyButton() {
        verifyRequiredView?.findViewById<MaterialButton>(R.id.btnStartVerify)?.setOnClickListener {
            startActivity(Intent(requireContext(), VerifyLandlordActivity::class.java))
        }
    }

    // ═══ THÊM ẢNH VÀO LAYOUT ═══
    private fun addPhotoToLayout(uri: Uri) {
        val view = postFormView ?: return
        val layoutPhotos = view.findViewById<LinearLayout>(R.id.layoutPhotos)
        val tvPhotoCount = view.findViewById<TextView>(R.id.tvPhotoCount)
        val btnAddPhoto = view.findViewById<CardView>(R.id.btnAddPhoto)

        val imgView = ImageView(requireContext())
        val params = LinearLayout.LayoutParams(dpToPx(90), dpToPx(90))
        params.marginEnd = dpToPx(8)
        imgView.layoutParams = params
        imgView.scaleType = ImageView.ScaleType.CENTER_CROP
        imgView.setImageURI(uri)

        // Bấm giữ để xóa ảnh
        imgView.setOnLongClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Xóa ảnh")
                .setMessage("Bạn có muốn xóa ảnh này?")
                .setPositiveButton("Xóa") { _, _ ->
                    val index = layoutPhotos.indexOfChild(imgView)
                    if (index >= 0 && index < imageUris.size) {
                        imageUris.removeAt(index)
                    }
                    layoutPhotos.removeView(imgView)
                    tvPhotoCount.text = "${imageUris.size}/$MAX_PHOTOS ảnh"
                    if (imageUris.size < MAX_PHOTOS) btnAddPhoto.visibility = View.VISIBLE
                }
                .setNegativeButton("Hủy", null)
                .show()
            true
        }

        layoutPhotos.addView(imgView, layoutPhotos.childCount - 1)
        tvPhotoCount.text = "${imageUris.size}/$MAX_PHOTOS ảnh"
        if (imageUris.size >= MAX_PHOTOS) btnAddPhoto.visibility = View.GONE
    }

    // ═══ SETUP FORM ĐĂNG BÀI ═══
    private fun setupPostForm() {
        val view = postFormView ?: return
        val viewModel = ViewModelProvider(this)[PostViewModel::class.java]

        // Khai báo view
        val edtOwnerName = view.findViewById<EditText>(R.id.edtOwnerName)
        val edtOwnerPhone = view.findViewById<EditText>(R.id.edtOwnerPhone)
        val rgOwnerGender = view.findViewById<RadioGroup>(R.id.rgOwnerGender)
        val edtTitle = view.findViewById<EditText>(R.id.edtTitle)
        val tvPostDate = view.findViewById<TextView>(R.id.tvPostDate)
        val spinnerWard = view.findViewById<Spinner>(R.id.spinnerWard)
        val edtAddress = view.findViewById<EditText>(R.id.edtAddress)
        val edtDescription = view.findViewById<EditText>(R.id.edtDescription)
        val edtPrice = view.findViewById<EditText>(R.id.edtPrice)
        val edtArea = view.findViewById<EditText>(R.id.edtArea)
        val edtPeopleCount = view.findViewById<EditText>(R.id.edtPeopleCount)
        val rgRoomStyle = view.findViewById<RadioGroup>(R.id.rgRoomStyle)
        val edtDepositMonths = view.findViewById<EditText>(R.id.edtDepositMonths)
        val edtDepositAmount = view.findViewById<EditText>(R.id.edtDepositAmount)
        val cbWifi = view.findViewById<CheckBox>(R.id.cbWifi)
        val cbElectric = view.findViewById<CheckBox>(R.id.cbElectric)
        val cbWater = view.findViewById<CheckBox>(R.id.cbWater)
        val edtWifiPrice = view.findViewById<EditText>(R.id.edtWifiPrice)
        val edtElectricPrice = view.findViewById<EditText>(R.id.edtElectricPrice)
        val edtWaterPrice = view.findViewById<EditText>(R.id.edtWaterPrice)
        val cbAirCon = view.findViewById<CheckBox>(R.id.cbAirCon)
        val cbWaterHeater = view.findViewById<CheckBox>(R.id.cbWaterHeater)
        val cbWasher = view.findViewById<CheckBox>(R.id.cbWasher)
        val cbDryingArea = view.findViewById<CheckBox>(R.id.cbDryingArea)
        val cbWardrobe = view.findViewById<CheckBox>(R.id.cbWardrobe)
        val cbBed = view.findViewById<CheckBox>(R.id.cbBed)
        val rgKitchen = view.findViewById<RadioGroup>(R.id.rgKitchen)
        val rgBathroom = view.findViewById<RadioGroup>(R.id.rgBathroom)
        val rgPet = view.findViewById<RadioGroup>(R.id.rgPet)
        val layoutPetDetail = view.findViewById<LinearLayout>(R.id.layoutPetDetail)
        val edtPetName = view.findViewById<EditText>(R.id.edtPetName)
        val edtPetCount = view.findViewById<EditText>(R.id.edtPetCount)
        val rgGenderPrefer = view.findViewById<RadioGroup>(R.id.rgGenderPrefer)
        val rgCurfew = view.findViewById<RadioGroup>(R.id.rgCurfew)
        val edtCurfewTime = view.findViewById<EditText>(R.id.edtCurfewTime)
        val btnAddPhoto = view.findViewById<CardView>(R.id.btnAddPhoto)
        val btnPostRoom = view.findViewById<MaterialButton>(R.id.btnPostRoom)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        // Load thông tin chủ trọ từ tài khoản
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    edtOwnerName.setText(doc.getString("fullName") ?: "")
                    edtOwnerPhone.setText(doc.getString("phone") ?: "")
                    val gender = doc.getString("gender") ?: ""
                    when (gender) {
                        "Nam" -> rgOwnerGender.check(R.id.rbOwnerMale)
                        "Nữ" -> rgOwnerGender.check(R.id.rbOwnerFemale)
                    }
                }
            }

        // Ngày đăng
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))
        tvPostDate.text = dateFormat.format(Date())

        // Spinner khu vực
        val allAreas = mutableListOf("-- Chọn phường/xã --")
        allAreas.addAll(AddressData.phuongList.drop(1))
        allAreas.addAll(AddressData.xaList.drop(1))
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, allAreas)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerWard.adapter = adapter

        // Format giá
        NumberFormatUtils.addFormatWatcher(edtPrice)
        NumberFormatUtils.addFormatWatcher(edtWifiPrice)
        NumberFormatUtils.addFormatWatcher(edtElectricPrice)
        NumberFormatUtils.addFormatWatcher(edtWaterPrice)
        NumberFormatUtils.addFormatWatcher(edtDepositAmount)

        // Checkbox tiện ích
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

        // Thú cưng
        rgPet.setOnCheckedChangeListener { _, checkedId ->
            layoutPetDetail.visibility = if (checkedId == R.id.rbPetYes) View.VISIBLE else View.GONE
        }

        // Giờ ra vào
        rgCurfew.setOnCheckedChangeListener { _, checkedId ->
            edtCurfewTime.visibility = if (checkedId == R.id.rbCurfewCustom) View.VISIBLE else View.GONE
        }

        // Chỗ để xe
        setupParkingListeners(view)

        // Thêm ảnh
        btnAddPhoto.setOnClickListener {
            if (imageUris.size >= MAX_PHOTOS) {
                Toast.makeText(requireContext(), "Tối đa $MAX_PHOTOS ảnh", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            pickImageLauncher.launch(intent)
        }

        // ═══ OBSERVE VIEWMODEL ═══
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnPostRoom.isEnabled = !isLoading
            btnPostRoom.text = if (isLoading) "Đang đăng bài..." else "Đăng bài cho thuê"
        }

        viewModel.postResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Đăng bài thành công!")
                    .setMessage("Bài đăng của bạn đã được gửi và đang chờ admin kiểm duyệt nội dung và hình ảnh trước khi hiển thị trên ứng dụng.")
                    .setPositiveButton("OK") { _, _ ->
                        resetForm(view)
                    }
                    .setCancelable(false)
                    .show()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
        }

        // ═══ NÚT ĐĂNG BÀI ═══
        btnPostRoom.setOnClickListener {
            val selectedWard = spinnerWard.selectedItem?.toString() ?: ""
            val wardName = if (selectedWard.contains("(")) selectedWard.substringBefore("(").trim() else selectedWard
            val districtName = if (selectedWard.contains("(")) selectedWard.substringAfter("(").replace(")", "").trim() else ""

            // Lấy thông tin chỗ để xe
            val cbMotorbike = view.findViewById<CheckBox>(R.id.cbMotorbike)
            val cbEBike = view.findViewById<CheckBox>(R.id.cbEBike)
            val cbBicycle = view.findViewById<CheckBox>(R.id.cbBicycle)
            val rgMotorbikeFee = view.findViewById<RadioGroup>(R.id.rgMotorbikeFee)
            val rgEBikeFee = view.findViewById<RadioGroup>(R.id.rgEBikeFee)
            val rgBicycleFee = view.findViewById<RadioGroup>(R.id.rgBicycleFee)
            val edtMotorbikeFee = view.findViewById<EditText>(R.id.edtMotorbikeFee)
            val edtEBikeFee = view.findViewById<EditText>(R.id.edtEBikeFee)
            val edtBicycleFee = view.findViewById<EditText>(R.id.edtBicycleFee)

            val room = Room(
                ownerName = edtOwnerName.text.toString().trim(),
                ownerPhone = edtOwnerPhone.text.toString().trim(),
                ownerGender = when (rgOwnerGender.checkedRadioButtonId) {
                    R.id.rbOwnerMale -> "Nam"
                    R.id.rbOwnerFemale -> "Nữ"
                    else -> ""
                },
                title = edtTitle.text.toString().trim(),
                ward = wardName,
                district = districtName,
                address = edtAddress.text.toString().trim(),
                description = edtDescription.text.toString().trim(),
                price = NumberFormatUtils.getRawNumber(edtPrice).toLongOrNull() ?: 0,
                area = edtArea.text.toString().toIntOrNull() ?: 0,
                peopleCount = edtPeopleCount.text.toString().toIntOrNull() ?: 0,
                roomType = when (rgRoomStyle.checkedRadioButtonId) {
                    R.id.rbShared -> "Chung chủ"
                    R.id.rbPrivate -> "Riêng chủ"
                    else -> ""
                },
                depositMonths = edtDepositMonths.text.toString().toIntOrNull() ?: 0,
                depositAmount = NumberFormatUtils.getRawNumber(edtDepositAmount).toLongOrNull() ?: 0,
                hasWifi = cbWifi.isChecked,
                wifiPrice = NumberFormatUtils.getRawNumber(edtWifiPrice).toLongOrNull() ?: 0,
                electricPrice = NumberFormatUtils.getRawNumber(edtElectricPrice).toLongOrNull() ?: 0,
                waterPrice = NumberFormatUtils.getRawNumber(edtWaterPrice).toLongOrNull() ?: 0,
                hasAirCon = cbAirCon.isChecked,
                hasWaterHeater = cbWaterHeater.isChecked,
                hasWasher = cbWasher.isChecked,
                hasDryingArea = cbDryingArea.isChecked,
                hasWardrobe = cbWardrobe.isChecked,
                hasBed = cbBed.isChecked,
                kitchen = when (rgKitchen.checkedRadioButtonId) {
                    R.id.rbKitchenShared -> "Chung"
                    R.id.rbKitchenPrivate -> "Riêng"
                    R.id.rbKitchenNone -> "Không"
                    else -> ""
                },
                bathroom = when (rgBathroom.checkedRadioButtonId) {
                    R.id.rbBathroomShared -> "Chung"
                    R.id.rbBathroomPrivate -> "Riêng"
                    else -> ""
                },
                pet = when (rgPet.checkedRadioButtonId) {
                    R.id.rbPetYes -> "Cho nuôi"
                    R.id.rbPetNo -> "Không"
                    else -> ""
                },
                petName = edtPetName.text.toString().trim(),
                petCount = edtPetCount.text.toString().toIntOrNull() ?: 0,
                genderPrefer = when (rgGenderPrefer.checkedRadioButtonId) {
                    R.id.rbGenderMale -> "Nam"
                    R.id.rbGenderFemale -> "Nữ"
                    R.id.rbGenderAll -> "Tất cả"
                    else -> ""
                },
                hasMotorbike = cbMotorbike?.isChecked ?: false,
                motorbikeFee = if (cbMotorbike?.isChecked == true && rgMotorbikeFee?.checkedRadioButtonId == R.id.rbMotorbikePaid)
                    NumberFormatUtils.getRawNumber(edtMotorbikeFee).toLongOrNull() ?: 0 else 0,
                hasEBike = cbEBike?.isChecked ?: false,
                eBikeFee = if (cbEBike?.isChecked == true && rgEBikeFee?.checkedRadioButtonId == R.id.rbEBikePaid)
                    NumberFormatUtils.getRawNumber(edtEBikeFee).toLongOrNull() ?: 0 else 0,
                hasBicycle = cbBicycle?.isChecked ?: false,
                bicycleFee = if (cbBicycle?.isChecked == true && rgBicycleFee?.checkedRadioButtonId == R.id.rbBicyclePaid)
                    NumberFormatUtils.getRawNumber(edtBicycleFee).toLongOrNull() ?: 0 else 0,
                curfew = when (rgCurfew.checkedRadioButtonId) {
                    R.id.rbCurfewFree -> "Tự do"
                    R.id.rbCurfewCustom -> "Tùy chọn"
                    else -> ""
                },
                curfewTime = edtCurfewTime.text.toString().trim(),
                status = "pending",
                createdAt = System.currentTimeMillis()
            )

            viewModel.postRoom(room, imageUris)
        }
    }

    // ═══ SETUP CHỖ ĐỂ XE ═══
    private fun setupParkingListeners(view: View) {
        val cbMotorbike = view.findViewById<CheckBox>(R.id.cbMotorbike) ?: return
        val rgMotorbikeFee = view.findViewById<RadioGroup>(R.id.rgMotorbikeFee) ?: return
        val edtMotorbikeFee = view.findViewById<EditText>(R.id.edtMotorbikeFee) ?: return

        cbMotorbike.setOnCheckedChangeListener { _, isChecked ->
            rgMotorbikeFee.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                edtMotorbikeFee.visibility = View.GONE
                edtMotorbikeFee.text?.clear()
                rgMotorbikeFee.check(R.id.rbMotorbikeFree)
            }
        }
        rgMotorbikeFee.setOnCheckedChangeListener { _, checkedId ->
            edtMotorbikeFee.visibility = if (checkedId == R.id.rbMotorbikePaid) View.VISIBLE else View.GONE
            if (checkedId == R.id.rbMotorbikeFree) edtMotorbikeFee.text?.clear()
        }

        val cbEBike = view.findViewById<CheckBox>(R.id.cbEBike) ?: return
        val rgEBikeFee = view.findViewById<RadioGroup>(R.id.rgEBikeFee) ?: return
        val edtEBikeFee = view.findViewById<EditText>(R.id.edtEBikeFee) ?: return

        cbEBike.setOnCheckedChangeListener { _, isChecked ->
            rgEBikeFee.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                edtEBikeFee.visibility = View.GONE
                edtEBikeFee.text?.clear()
                rgEBikeFee.check(R.id.rbEBikeFree)
            }
        }
        rgEBikeFee.setOnCheckedChangeListener { _, checkedId ->
            edtEBikeFee.visibility = if (checkedId == R.id.rbEBikePaid) View.VISIBLE else View.GONE
            if (checkedId == R.id.rbEBikeFree) edtEBikeFee.text?.clear()
        }

        val cbBicycle = view.findViewById<CheckBox>(R.id.cbBicycle) ?: return
        val rgBicycleFee = view.findViewById<RadioGroup>(R.id.rgBicycleFee) ?: return
        val edtBicycleFee = view.findViewById<EditText>(R.id.edtBicycleFee) ?: return

        cbBicycle.setOnCheckedChangeListener { _, isChecked ->
            rgBicycleFee.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                edtBicycleFee.visibility = View.GONE
                edtBicycleFee.text?.clear()
                rgBicycleFee.check(R.id.rbBicycleFree)
            }
        }
        rgBicycleFee.setOnCheckedChangeListener { _, checkedId ->
            edtBicycleFee.visibility = if (checkedId == R.id.rbBicyclePaid) View.VISIBLE else View.GONE
            if (checkedId == R.id.rbBicycleFree) edtBicycleFee.text?.clear()
        }

        NumberFormatUtils.addFormatWatcher(edtMotorbikeFee)
        NumberFormatUtils.addFormatWatcher(edtEBikeFee)
        NumberFormatUtils.addFormatWatcher(edtBicycleFee)
    }

    // ═══ RESET FORM ═══
    private fun resetForm(view: View) {
        view.findViewById<EditText>(R.id.edtOwnerName)?.text?.clear()
        view.findViewById<EditText>(R.id.edtOwnerPhone)?.text?.clear()
        view.findViewById<EditText>(R.id.edtTitle)?.text?.clear()
        view.findViewById<EditText>(R.id.edtAddress)?.text?.clear()
        view.findViewById<EditText>(R.id.edtDescription)?.text?.clear()
        view.findViewById<EditText>(R.id.edtPrice)?.text?.clear()
        view.findViewById<EditText>(R.id.edtArea)?.text?.clear()
        view.findViewById<EditText>(R.id.edtPeopleCount)?.text?.clear()
        view.findViewById<EditText>(R.id.edtDepositMonths)?.text?.clear()
        view.findViewById<EditText>(R.id.edtDepositAmount)?.text?.clear()
        view.findViewById<EditText>(R.id.edtWifiPrice)?.text?.clear()
        view.findViewById<EditText>(R.id.edtElectricPrice)?.text?.clear()
        view.findViewById<EditText>(R.id.edtWaterPrice)?.text?.clear()
        view.findViewById<EditText>(R.id.edtCurfewTime)?.text?.clear()
        view.findViewById<EditText>(R.id.edtPetName)?.text?.clear()
        view.findViewById<EditText>(R.id.edtPetCount)?.text?.clear()
        view.findViewById<EditText>(R.id.edtMotorbikeFee)?.text?.clear()
        view.findViewById<EditText>(R.id.edtEBikeFee)?.text?.clear()
        view.findViewById<EditText>(R.id.edtBicycleFee)?.text?.clear()
        view.findViewById<Spinner>(R.id.spinnerWard)?.setSelection(0)

        view.findViewById<CheckBox>(R.id.cbWifi)?.isChecked = false
        view.findViewById<CheckBox>(R.id.cbElectric)?.isChecked = false
        view.findViewById<CheckBox>(R.id.cbWater)?.isChecked = false
        view.findViewById<CheckBox>(R.id.cbAirCon)?.isChecked = false
        view.findViewById<CheckBox>(R.id.cbWaterHeater)?.isChecked = false
        view.findViewById<CheckBox>(R.id.cbWasher)?.isChecked = false
        view.findViewById<CheckBox>(R.id.cbDryingArea)?.isChecked = false
        view.findViewById<CheckBox>(R.id.cbWardrobe)?.isChecked = false
        view.findViewById<CheckBox>(R.id.cbBed)?.isChecked = false
        view.findViewById<CheckBox>(R.id.cbMotorbike)?.isChecked = false
        view.findViewById<CheckBox>(R.id.cbEBike)?.isChecked = false
        view.findViewById<CheckBox>(R.id.cbBicycle)?.isChecked = false

        view.findViewById<RadioGroup>(R.id.rgOwnerGender)?.clearCheck()
        view.findViewById<RadioGroup>(R.id.rgRoomStyle)?.clearCheck()
        view.findViewById<RadioGroup>(R.id.rgKitchen)?.clearCheck()
        view.findViewById<RadioGroup>(R.id.rgBathroom)?.clearCheck()
        view.findViewById<RadioGroup>(R.id.rgPet)?.clearCheck()
        view.findViewById<RadioGroup>(R.id.rgGenderPrefer)?.clearCheck()
        view.findViewById<RadioGroup>(R.id.rgCurfew)?.clearCheck()

        imageUris.clear()
        val layoutPhotos = view.findViewById<LinearLayout>(R.id.layoutPhotos)
        while (layoutPhotos.childCount > 1) {
            layoutPhotos.removeViewAt(0)
        }
        view.findViewById<TextView>(R.id.tvPhotoCount)?.text = "0/$MAX_PHOTOS ảnh"
        view.findViewById<CardView>(R.id.btnAddPhoto)?.visibility = View.VISIBLE

        // Load lại thông tin chủ trọ
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    view.findViewById<EditText>(R.id.edtOwnerName)?.setText(doc.getString("fullName") ?: "")
                    view.findViewById<EditText>(R.id.edtOwnerPhone)?.setText(doc.getString("phone") ?: "")
                }
            }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onResume() {
        super.onResume()
        checkUserRole()
    }
}