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
import com.example.doantotnghiep.Utils.MessageUtils
import com.example.doantotnghiep.Utils.NumberFormatUtils
import com.example.doantotnghiep.View.Auth.VerifyLandlordActivity
import com.bumptech.glide.Glide
import com.example.doantotnghiep.Utils.PostNotificationHelper
import com.example.doantotnghiep.ViewModel.PostViewModel
import com.google.android.material.button.MaterialButton

import java.text.SimpleDateFormat
import java.util.*

class PostFragment : Fragment() {

    private var verifyRequiredView: View? = null
    private var postFormView: View? = null

    private val imageUris = mutableListOf<Uri>()
    private val MAX_PHOTOS = 10
    private var isFormSetup = false
    private var userRoleChecked = false

    private lateinit var viewModel: PostViewModel

    private var lastPostedTitle = ""
    private var lastPostedPrice = 0L
    private var lastPostedLocation = ""

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data?.clipData != null) {
                val count = data.clipData!!.itemCount
                for (i in 0 until count) {
                    val uri = data.clipData!!.getItemAt(i).uri
                    if (imageUris.size < MAX_PHOTOS) { imageUris.add(uri); addPhotoToLayout(uri) }
                }
            } else if (data?.data != null) {
                val uri = data.data!!
                if (imageUris.size < MAX_PHOTOS) { imageUris.add(uri); addPhotoToLayout(uri) }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val frameLayout = FrameLayout(requireContext())
        frameLayout.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        verifyRequiredView = inflater.inflate(R.layout.layout_verify_required, container, false)
        postFormView = inflater.inflate(R.layout.fragment_post, container, false)

        verifyRequiredView?.visibility = View.GONE
        postFormView?.visibility = View.GONE

        frameLayout.addView(verifyRequiredView)
        frameLayout.addView(postFormView)
        return frameLayout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[PostViewModel::class.java]

        viewModel.userObject.observe(viewLifecycleOwner) { user ->
            if (user == null) return@observe
            userRoleChecked = true
            
            // Logic cũ: role == "landlord" hoặc role == "admin" thì được đăng
            // Logic mới của bạn là "owner" nhưng có vẻ project dùng "landlord"
            val isPrivileged = user.role == "landlord" || user.role == "admin" || user.role == "owner"
            
            if (isPrivileged) {
                // Kiểm tra thêm trạng thái xác minh nếu cần (isVerified)
                // Tuy nhiên, nếu tài khoản đã là landlord/admin thường là đã duyệt rồi
                showPostForm()
                if (!user.hasAcceptedRules) {
                    showRulesDialog(isFirstTime = true)
                }
            } else {
                // Nếu là tenant hoặc role khác, kiểm tra status xác minh
                when (user.role) {
                    "pending" -> showPendingStatus()
                    "rejected" -> showRejectedStatus(user.occupation) 
                    else -> showVerifyRequired()
                }
            }
        }

        viewModel.ownerInfo.observe(viewLifecycleOwner) { (name, phone) ->
            val v = postFormView ?: return@observe
            v.findViewById<EditText>(R.id.edtOwnerName)?.setText(name)
            v.findViewById<EditText>(R.id.edtOwnerPhone)?.setText(phone)
        }

        checkUserRole()
        setupVerifyButton()
    }

    private fun checkUserRole() {
        viewModel.loadUserObject()
    }

    private fun showVerifyRequired() {
        verifyRequiredView?.visibility = View.VISIBLE
        postFormView?.visibility = View.GONE
        
        val tvVerifyTitle = verifyRequiredView?.findViewById<TextView>(R.id.tvVerifyTitle)
        val tvVerifyStatus = verifyRequiredView?.findViewById<TextView>(R.id.tvVerifyStatus)
        val ivVerifyIcon = verifyRequiredView?.findViewById<ImageView>(R.id.ivVerifyIcon)
        val btnStartVerify = verifyRequiredView?.findViewById<MaterialButton>(R.id.btnStartVerify)
        val tvViewRules = verifyRequiredView?.findViewById<TextView>(R.id.tvViewRulesBeforeVerify)

        tvVerifyTitle?.text = "Xác minh chủ trọ"
        tvVerifyStatus?.text = "Bạn cần xác minh danh tính chủ trọ để có thể đăng tin cho thuê phòng trên hệ thống."
        ivVerifyIcon?.setImageResource(R.drawable.ic_verify_required)
        ivVerifyIcon?.imageTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.primary))
        
        btnStartVerify?.visibility = View.VISIBLE
        btnStartVerify?.text = "Bắt đầu xác minh ngay"
        tvViewRules?.visibility = View.VISIBLE
    }

    private fun showPendingStatus() {
        verifyRequiredView?.visibility = View.VISIBLE
        postFormView?.visibility = View.GONE

        val tvVerifyTitle = verifyRequiredView?.findViewById<TextView>(R.id.tvVerifyTitle)
        val tvVerifyStatus = verifyRequiredView?.findViewById<TextView>(R.id.tvVerifyStatus)
        val ivVerifyIcon = verifyRequiredView?.findViewById<ImageView>(R.id.ivVerifyIcon)
        val btnStartVerify = verifyRequiredView?.findViewById<MaterialButton>(R.id.btnStartVerify)
        val tvViewRules = verifyRequiredView?.findViewById<TextView>(R.id.tvViewRulesBeforeVerify)

        tvVerifyTitle?.text = "Đang chờ phê duyệt"
        tvVerifyStatus?.text = "Yêu cầu xác minh của bạn đã được gửi thành công. Admin đang kiểm tra và phê duyệt thông tin của bạn. Vui lòng quay lại sau."
        ivVerifyIcon?.setImageResource(R.drawable.ic_verify_pending)
        ivVerifyIcon?.imageTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.secondary))
        
        btnStartVerify?.visibility = View.GONE
        tvViewRules?.visibility = View.VISIBLE
    }

    private fun showRejectedStatus(reason: String) {
        verifyRequiredView?.visibility = View.VISIBLE
        postFormView?.visibility = View.GONE

        val tvVerifyTitle = verifyRequiredView?.findViewById<TextView>(R.id.tvVerifyTitle)
        val tvVerifyStatus = verifyRequiredView?.findViewById<TextView>(R.id.tvVerifyStatus)
        val ivVerifyIcon = verifyRequiredView?.findViewById<ImageView>(R.id.ivVerifyIcon)
        val btnStartVerify = verifyRequiredView?.findViewById<MaterialButton>(R.id.btnStartVerify)
        val tvViewRules = verifyRequiredView?.findViewById<TextView>(R.id.tvViewRulesBeforeVerify)

        tvVerifyTitle?.text = "Xác minh bị từ chối"
        tvVerifyStatus?.text = "Rất tiếc, yêu cầu xác minh của bạn không được phê duyệt.\nLý do: $reason\n\nVui lòng kiểm tra lại thông tin và gửi lại yêu cầu."
        ivVerifyIcon?.setImageResource(R.drawable.ic_verify_required)
        ivVerifyIcon?.imageTintList = android.content.res.ColorStateList.valueOf(resources.getColor(android.R.color.holo_red_dark))
        
        btnStartVerify?.visibility = View.VISIBLE
        btnStartVerify?.text = "Gửi lại yêu cầu xác minh"
        tvViewRules?.visibility = View.VISIBLE
    }

    private fun showPostForm() {
        verifyRequiredView?.visibility = View.GONE
        postFormView?.visibility = View.VISIBLE
        if (!isFormSetup) {
            setupPostForm()
            isFormSetup = true
        }
    }

    private fun setupVerifyButton() {
        verifyRequiredView?.findViewById<MaterialButton>(R.id.btnStartVerify)?.setOnClickListener {
            startActivity(Intent(requireContext(), VerifyLandlordActivity::class.java))
        }
        
        verifyRequiredView?.findViewById<TextView>(R.id.tvViewRulesBeforeVerify)?.setOnClickListener {
            showRulesDialog()
        }
    }

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
        Glide.with(this).load(uri).centerCrop().into(imgView)

        imgView.setOnLongClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Xóa ảnh").setMessage("Bạn có muốn xóa ảnh này?")
                .setPositiveButton("Xóa") { _, _ ->
                    val uriIndex = imageUris.indexOf(uri)
                    if (uriIndex >= 0) imageUris.removeAt(uriIndex)
                    layoutPhotos.removeView(imgView)
                    tvPhotoCount.text = "${imageUris.size}/$MAX_PHOTOS ảnh"
                    if (imageUris.size < MAX_PHOTOS) btnAddPhoto.visibility = View.VISIBLE
                }
                .setNegativeButton("Hủy", null).show()
            true
        }
        layoutPhotos.addView(imgView, layoutPhotos.childCount - 1)
        tvPhotoCount.text = "${imageUris.size}/$MAX_PHOTOS ảnh"
        if (imageUris.size >= MAX_PHOTOS) btnAddPhoto.visibility = View.GONE
    }

    private fun setupPostForm() {
        val view = postFormView ?: return

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
        val layoutCurfewTime = view.findViewById<LinearLayout>(R.id.layoutCurfewTime)
        val edtCurfewTime = view.findViewById<TextView>(R.id.edtCurfewTime)
        val pickerHour = view.findViewById<android.widget.NumberPicker>(R.id.pickerHour)
        val pickerMinute = view.findViewById<android.widget.NumberPicker>(R.id.pickerMinute)
        val pickerAmPm = view.findViewById<android.widget.NumberPicker>(R.id.pickerAmPm)

        pickerHour.minValue = 1; pickerHour.maxValue = 12; pickerHour.value = 10
        pickerMinute.minValue = 0; pickerMinute.maxValue = 59; pickerMinute.value = 0
        pickerMinute.setFormatter { String.format("%02d", it) }
        pickerAmPm.minValue = 0; pickerAmPm.maxValue = 1
        pickerAmPm.displayedValues = arrayOf("SA", "CH"); pickerAmPm.value = 1

        val syncCurfewTime = {
            val amPm = if (pickerAmPm.value == 0) "SA" else "CH"
            edtCurfewTime.text = String.format("%02d:%02d %s", pickerHour.value, pickerMinute.value, amPm)
        }
        pickerHour.setOnValueChangedListener { _, _, _ -> syncCurfewTime() }
        pickerMinute.setOnValueChangedListener { _, _, _ -> syncCurfewTime() }
        pickerAmPm.setOnValueChangedListener { _, _, _ -> syncCurfewTime() }
        syncCurfewTime()

        val btnAddPhoto = view.findViewById<CardView>(R.id.btnAddPhoto)
        val btnPostRoom = view.findViewById<MaterialButton>(R.id.btnPostRoom)
        val tvRulesLink = view.findViewById<TextView>(R.id.tvRulesLink)
        val layoutProgress = view.findViewById<LinearLayout>(R.id.layoutProgress)
        val tvProgressPercent = view.findViewById<TextView>(R.id.tvProgressPercent)

        tvRulesLink.setOnClickListener {
            showRulesDialog()
        }

        // Load owner info via ViewModel — không gọi FirebaseAuth từ Fragment
        viewModel.loadOwnerInfo()

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))
        tvPostDate.text = dateFormat.format(Date())

        val allAreas = mutableListOf("-- Chọn phường/xã --")
        allAreas.addAll(AddressData.phuongList.drop(1))
        allAreas.addAll(AddressData.xaList.drop(1))
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, allAreas)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerWard.adapter = adapter

        NumberFormatUtils.addFormatWatcher(edtPrice)
        NumberFormatUtils.addFormatWatcher(edtWifiPrice)
        NumberFormatUtils.addFormatWatcher(edtElectricPrice)
        NumberFormatUtils.addFormatWatcher(edtWaterPrice)
        NumberFormatUtils.addFormatWatcher(edtDepositAmount)

        cbWifi.setOnCheckedChangeListener { _, isChecked -> edtWifiPrice.isEnabled = isChecked; if (!isChecked) edtWifiPrice.text?.clear() }
        cbElectric.setOnCheckedChangeListener { _, isChecked -> edtElectricPrice.isEnabled = isChecked; if (!isChecked) edtElectricPrice.text?.clear() }
        cbWater.setOnCheckedChangeListener { _, isChecked -> edtWaterPrice.isEnabled = isChecked; if (!isChecked) edtWaterPrice.text?.clear() }
        rgPet.setOnCheckedChangeListener { _, checkedId -> layoutPetDetail.visibility = if (checkedId == R.id.rbPetYes) View.VISIBLE else View.GONE }
        rgCurfew.setOnCheckedChangeListener { _, checkedId -> layoutCurfewTime.visibility = if (checkedId == R.id.rbCurfewCustom) View.VISIBLE else View.GONE }

        setupParkingListeners(view)

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            layoutProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnPostRoom.isEnabled = !isLoading
            btnPostRoom.text = if (isLoading) "Đang đăng bài..." else "Đăng bài cho thuê"
            if (isLoading) PostNotificationHelper.showProgress(requireContext(), 0)
        }

        viewModel.uploadProgress.observe(viewLifecycleOwner) { progress ->
            tvProgressPercent.text = "Đang đăng bài: $progress%"
            PostNotificationHelper.showProgress(requireContext(), progress)
        }

        viewModel.postResult.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                val (postId, thumbnailUrl) = result
                viewModel.resetPostResult()
                PostNotificationHelper.showSuccess(requireContext(), lastPostedTitle)
                resetForm(view)
                val intent = Intent(requireContext(), com.example.doantotnghiep.View.Auth.PostSuccessActivity::class.java).apply {
                    putExtra("postId", postId)
                    putExtra("thumbnail", thumbnailUrl)
                    putExtra("title", lastPostedTitle)
                    putExtra("price", lastPostedPrice)
                    putExtra("location", lastPostedLocation)
                }
                startActivity(intent)
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrEmpty()) {
                PostNotificationHelper.cancel(requireContext())
                MessageUtils.showErrorDialog(requireContext(), "Lỗi đăng bài", error)
            }
        }

        btnAddPhoto.setOnClickListener {
            if (imageUris.size >= MAX_PHOTOS) {
                MessageUtils.showInfoDialog(requireContext(), "Giới hạn ảnh", "Bạn chỉ được chọn tối đa $MAX_PHOTOS ảnh.")
                return@setOnClickListener
            }
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            pickImageLauncher.launch(intent)
        }

        btnPostRoom.setOnClickListener {
            val selectedWard = spinnerWard.selectedItem?.toString() ?: ""
            if (spinnerWard.selectedItemPosition == 0) {
                MessageUtils.showInfoDialog(requireContext(), "Thông tin thiếu", "Vui lòng chọn khu vực phường/xã.")
                return@setOnClickListener
            }
            val wardName = if (selectedWard.contains("(")) selectedWard.substringBefore("(").trim() else selectedWard
            val districtName = if (selectedWard.contains("(")) selectedWard.substringAfter("(").replace(")", "").trim() else ""

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
                ownerGender = when (rgOwnerGender.checkedRadioButtonId) { R.id.rbOwnerMale -> "Nam"; R.id.rbOwnerFemale -> "Nữ"; else -> "" },
                title = edtTitle.text.toString().trim(),
                ward = wardName, district = districtName,
                address = edtAddress.text.toString().trim(),
                description = edtDescription.text.toString().trim(),
                price = NumberFormatUtils.getRawNumber(edtPrice).toLongOrNull() ?: 0,
                area = edtArea.text.toString().toIntOrNull() ?: 0,
                peopleCount = edtPeopleCount.text.toString().toIntOrNull() ?: 0,
                roomType = when (rgRoomStyle.checkedRadioButtonId) { R.id.rbShared -> "Chung chủ"; R.id.rbPrivate -> "Riêng chủ"; else -> "" },
                depositMonths = edtDepositMonths.text.toString().toIntOrNull() ?: 0,
                depositAmount = NumberFormatUtils.getRawNumber(edtDepositAmount).toLongOrNull() ?: 0,
                hasWifi = cbWifi.isChecked,
                wifiPrice = NumberFormatUtils.getRawNumber(edtWifiPrice).toLongOrNull() ?: 0,
                electricPrice = NumberFormatUtils.getRawNumber(edtElectricPrice).toLongOrNull() ?: 0,
                waterPrice = NumberFormatUtils.getRawNumber(edtWaterPrice).toLongOrNull() ?: 0,
                hasAirCon = cbAirCon.isChecked, hasWaterHeater = cbWaterHeater.isChecked,
                hasWasher = cbWasher.isChecked, hasDryingArea = cbDryingArea.isChecked,
                hasWardrobe = cbWardrobe.isChecked, hasBed = cbBed.isChecked,
                kitchen = when (rgKitchen.checkedRadioButtonId) { R.id.rbKitchenShared -> "Chung"; R.id.rbKitchenPrivate -> "Riêng"; R.id.rbKitchenNone -> "Không"; else -> "" },
                bathroom = when (rgBathroom.checkedRadioButtonId) { R.id.rbBathroomShared -> "Chung"; R.id.rbBathroomPrivate -> "Riêng"; else -> "" },
                pet = when (rgPet.checkedRadioButtonId) { R.id.rbPetYes -> "Cho nuôi"; R.id.rbPetNo -> "Không"; else -> "" },
                petName = edtPetName.text.toString().trim(),
                petCount = edtPetCount.text.toString().toIntOrNull() ?: 0,
                genderPrefer = when (rgGenderPrefer.checkedRadioButtonId) { R.id.rbGenderMale -> "Nam"; R.id.rbGenderFemale -> "Nữ"; R.id.rbGenderAll -> "Tất cả"; else -> "" },
                hasMotorbike = cbMotorbike?.isChecked ?: false,
                motorbikeFee = if (cbMotorbike?.isChecked == true && rgMotorbikeFee?.checkedRadioButtonId == R.id.rbMotorbikePaid) NumberFormatUtils.getRawNumber(edtMotorbikeFee).toLongOrNull() ?: 0 else 0,
                hasEBike = cbEBike?.isChecked ?: false,
                eBikeFee = if (cbEBike?.isChecked == true && rgEBikeFee?.checkedRadioButtonId == R.id.rbEBikePaid) NumberFormatUtils.getRawNumber(edtEBikeFee).toLongOrNull() ?: 0 else 0,
                hasBicycle = cbBicycle?.isChecked ?: false,
                bicycleFee = if (cbBicycle?.isChecked == true && rgBicycleFee?.checkedRadioButtonId == R.id.rbBicyclePaid) NumberFormatUtils.getRawNumber(edtBicycleFee).toLongOrNull() ?: 0 else 0,
                curfew = when (rgCurfew.checkedRadioButtonId) { R.id.rbCurfewFree -> "Tự do"; R.id.rbCurfewCustom -> "Tùy chọn"; else -> "" },
                curfewTime = edtCurfewTime.text.toString().trim(),
                status = "pending", createdAt = System.currentTimeMillis()
            )

            lastPostedTitle = room.title
            lastPostedPrice = room.price
            lastPostedLocation = if (room.ward.isNotEmpty()) "${room.ward}, ${room.district}" else room.district
            viewModel.postRoom(requireContext(), room, imageUris)
        }

        setupAccordionLogic(view)
    }

    private fun setupAccordionLogic(view: View) {
        val headers = listOf(
            view.findViewById<LinearLayout>(R.id.llHeaderCard1),
            view.findViewById<LinearLayout>(R.id.llHeaderCard2),
            view.findViewById<LinearLayout>(R.id.llHeaderCard3),
            view.findViewById<LinearLayout>(R.id.llHeaderCard4)
        )
        val contents = listOf(
            view.findViewById<LinearLayout>(R.id.llContentCard1),
            view.findViewById<LinearLayout>(R.id.llContentCard2),
            view.findViewById<LinearLayout>(R.id.llContentCard3),
            view.findViewById<LinearLayout>(R.id.llContentCard4)
        )
        val arrows = listOf(
            view.findViewById<ImageView>(R.id.ivArrowCard1),
            view.findViewById<ImageView>(R.id.ivArrowCard2),
            view.findViewById<ImageView>(R.id.ivArrowCard3),
            view.findViewById<ImageView>(R.id.ivArrowCard4)
        )
        val nextBtns = listOf(
            view.findViewById<View>(R.id.btnNextCard1),
            view.findViewById<View>(R.id.btnNextCard2),
            view.findViewById<View>(R.id.btnNextCard3),
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

    private fun setupParkingListeners(view: View) {
        val cbMotorbike = view.findViewById<CheckBox>(R.id.cbMotorbike) ?: return
        val rgMotorbikeFee = view.findViewById<RadioGroup>(R.id.rgMotorbikeFee) ?: return
        val edtMotorbikeFee = view.findViewById<EditText>(R.id.edtMotorbikeFee) ?: return
        cbMotorbike.setOnCheckedChangeListener { _, isChecked -> rgMotorbikeFee.visibility = if (isChecked) View.VISIBLE else View.GONE; if (!isChecked) { edtMotorbikeFee.visibility = View.GONE; edtMotorbikeFee.text?.clear(); rgMotorbikeFee.check(R.id.rbMotorbikeFree) } }
        rgMotorbikeFee.setOnCheckedChangeListener { _, checkedId -> edtMotorbikeFee.visibility = if (checkedId == R.id.rbMotorbikePaid) View.VISIBLE else View.GONE; if (checkedId == R.id.rbMotorbikeFree) edtMotorbikeFee.text?.clear() }

        val cbEBike = view.findViewById<CheckBox>(R.id.cbEBike) ?: return
        val rgEBikeFee = view.findViewById<RadioGroup>(R.id.rgEBikeFee) ?: return
        val edtEBikeFee = view.findViewById<EditText>(R.id.edtEBikeFee) ?: return
        cbEBike.setOnCheckedChangeListener { _, isChecked -> rgEBikeFee.visibility = if (isChecked) View.VISIBLE else View.GONE; if (!isChecked) { edtEBikeFee.visibility = View.GONE; edtEBikeFee.text?.clear(); rgEBikeFee.check(R.id.rbEBikeFree) } }
        rgEBikeFee.setOnCheckedChangeListener { _, checkedId -> edtEBikeFee.visibility = if (checkedId == R.id.rbEBikePaid) View.VISIBLE else View.GONE; if (checkedId == R.id.rbEBikeFree) edtEBikeFee.text?.clear() }

        val cbBicycle = view.findViewById<CheckBox>(R.id.cbBicycle) ?: return
        val rgBicycleFee = view.findViewById<RadioGroup>(R.id.rgBicycleFee) ?: return
        val edtBicycleFee = view.findViewById<EditText>(R.id.edtBicycleFee) ?: return
        cbBicycle.setOnCheckedChangeListener { _, isChecked -> rgBicycleFee.visibility = if (isChecked) View.VISIBLE else View.GONE; if (!isChecked) { edtBicycleFee.visibility = View.GONE; edtBicycleFee.text?.clear(); rgBicycleFee.check(R.id.rbBicycleFree) } }
        rgBicycleFee.setOnCheckedChangeListener { _, checkedId -> edtBicycleFee.visibility = if (checkedId == R.id.rbBicyclePaid) View.VISIBLE else View.GONE; if (checkedId == R.id.rbBicycleFree) edtBicycleFee.text?.clear() }

        NumberFormatUtils.addFormatWatcher(edtMotorbikeFee)
        NumberFormatUtils.addFormatWatcher(edtEBikeFee)
        NumberFormatUtils.addFormatWatcher(edtBicycleFee)
    }

    private fun resetForm(view: View) {
        listOf(R.id.edtOwnerName, R.id.edtOwnerPhone, R.id.edtTitle, R.id.edtAddress,
            R.id.edtDescription, R.id.edtPrice, R.id.edtArea, R.id.edtPeopleCount,
            R.id.edtDepositMonths, R.id.edtDepositAmount, R.id.edtWifiPrice,
            R.id.edtElectricPrice, R.id.edtWaterPrice, R.id.edtPetName, R.id.edtPetCount,
            R.id.edtMotorbikeFee, R.id.edtEBikeFee, R.id.edtBicycleFee
        ).forEach { id -> view.findViewById<EditText>(id)?.text?.clear() }
        view.findViewById<TextView>(R.id.edtCurfewTime)?.text = ""
        view.findViewById<Spinner>(R.id.spinnerWard)?.setSelection(0)
        listOf(R.id.cbWifi, R.id.cbElectric, R.id.cbWater, R.id.cbAirCon, R.id.cbWaterHeater,
            R.id.cbWasher, R.id.cbDryingArea, R.id.cbWardrobe, R.id.cbBed,
            R.id.cbMotorbike, R.id.cbEBike, R.id.cbBicycle
        ).forEach { id -> view.findViewById<CheckBox>(id)?.isChecked = false }
        listOf(R.id.rgOwnerGender, R.id.rgRoomStyle, R.id.rgKitchen, R.id.rgBathroom,
            R.id.rgPet, R.id.rgGenderPrefer, R.id.rgCurfew
        ).forEach { id -> view.findViewById<RadioGroup>(id)?.clearCheck() }
        imageUris.clear()
        val layoutPhotos = view.findViewById<LinearLayout>(R.id.layoutPhotos)
        while (layoutPhotos.childCount > 1) layoutPhotos.removeViewAt(0)
        view.findViewById<TextView>(R.id.tvPhotoCount)?.text = "0/$MAX_PHOTOS ảnh"
        view.findViewById<CardView>(R.id.btnAddPhoto)?.visibility = View.VISIBLE

        // Reload owner info after form reset — không gọi Firebase từ Fragment
        viewModel.loadOwnerInfo()
    }

    private fun showRulesDialog(isFirstTime: Boolean = false) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_rules, null)
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(!isFirstTime) // Nếu là lần đầu, bắt buộc phải nhấn nút đồng ý
            .create()

        val btnAccept = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAcceptRules)
        
        btnAccept.setOnClickListener {
            if (isFirstTime) {
                viewModel.markRulesAccepted()
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    override fun onResume() {
        super.onResume()
        if (!userRoleChecked) checkUserRole()
    }
}