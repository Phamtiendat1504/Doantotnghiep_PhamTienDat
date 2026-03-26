package com.example.doantotnghiep.View.Fragment

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.AddressData
import com.example.doantotnghiep.Utils.NumberFormatUtils
import com.google.android.material.button.MaterialButton
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostFragment : Fragment() {

    private lateinit var tvPostDate: TextView
    private lateinit var tvPhotoCount: TextView
    private lateinit var layoutPhotos: LinearLayout
    private lateinit var btnAddPhoto: CardView
    private lateinit var edtPrice: EditText
    private lateinit var edtWifiPrice: EditText
    private lateinit var edtElectricPrice: EditText
    private lateinit var edtWaterPrice: EditText
    private lateinit var cbWifi: CheckBox
    private lateinit var cbElectric: CheckBox
    private lateinit var cbWater: CheckBox
    private lateinit var rgPet: RadioGroup
    private lateinit var layoutPetDetail: LinearLayout
    private lateinit var rgCurfew: RadioGroup
    private lateinit var edtCurfewTime: EditText
    private lateinit var spinnerWard: Spinner
    private lateinit var btnPostRoom: MaterialButton
    private lateinit var progressBar: ProgressBar

    private val photoBase64List = mutableListOf<String>()
    private val MAX_PHOTOS = 10

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            if (imageUri != null) addPhoto(imageUri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvPostDate = view.findViewById(R.id.tvPostDate)
        tvPhotoCount = view.findViewById(R.id.tvPhotoCount)
        layoutPhotos = view.findViewById(R.id.layoutPhotos)
        btnAddPhoto = view.findViewById(R.id.btnAddPhoto)
        edtPrice = view.findViewById(R.id.edtPrice)
        edtWifiPrice = view.findViewById(R.id.edtWifiPrice)
        edtElectricPrice = view.findViewById(R.id.edtElectricPrice)
        edtWaterPrice = view.findViewById(R.id.edtWaterPrice)
        cbWifi = view.findViewById(R.id.cbWifi)
        cbElectric = view.findViewById(R.id.cbElectric)
        cbWater = view.findViewById(R.id.cbWater)
        rgPet = view.findViewById(R.id.rgPet)
        layoutPetDetail = view.findViewById(R.id.layoutPetDetail)
        rgCurfew = view.findViewById(R.id.rgCurfew)
        edtCurfewTime = view.findViewById(R.id.edtCurfewTime)
        spinnerWard = view.findViewById(R.id.spinnerWard)
        btnPostRoom = view.findViewById(R.id.btnPostRoom)
        progressBar = view.findViewById(R.id.progressBar)

        // Ngày đăng tự động
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))
        tvPostDate.text = dateFormat.format(Date())

        // Setup spinner khu vực
        setupSpinner()

        // Format giá tiền
        NumberFormatUtils.addFormatWatcher(edtPrice)
        NumberFormatUtils.addFormatWatcher(edtWifiPrice)
        NumberFormatUtils.addFormatWatcher(edtElectricPrice)
        NumberFormatUtils.addFormatWatcher(edtWaterPrice)

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

        // Thú cưng: chọn "Cho nuôi" → hiện chi tiết
        rgPet.setOnCheckedChangeListener { _, checkedId ->
            layoutPetDetail.visibility = if (checkedId == R.id.rbPetYes) View.VISIBLE else View.GONE
        }

        // Giờ ra vào: chọn "Tùy chọn" → hiện ô nhập
        rgCurfew.setOnCheckedChangeListener { _, checkedId ->
            edtCurfewTime.visibility = if (checkedId == R.id.rbCurfewCustom) View.VISIBLE else View.GONE
        }

        // Thêm ảnh
        btnAddPhoto.setOnClickListener {
            if (photoBase64List.size >= MAX_PHOTOS) {
                Toast.makeText(requireContext(), "Tối đa $MAX_PHOTOS ảnh", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            pickImageLauncher.launch(intent)
        }

        // Nút đăng bài
        btnPostRoom.setOnClickListener {
            Toast.makeText(requireContext(), "Chức năng đăng bài đang phát triển", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSpinner() {
        val allAreas = mutableListOf("-- Chọn phường/xã --")
        allAreas.addAll(AddressData.phuongList.drop(1))
        allAreas.addAll(AddressData.xaList.drop(1))

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, allAreas)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerWard.adapter = adapter
    }

    private fun addPhoto(imageUri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            val resized = Bitmap.createScaledBitmap(bitmap, 400, 300, true)
            val baos = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 60, baos)
            val base64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)

            photoBase64List.add(base64)
            tvPhotoCount.text = "${photoBase64List.size}/$MAX_PHOTOS ảnh"

            val imgView = ImageView(requireContext())
            val params = LinearLayout.LayoutParams(dpToPx(90), dpToPx(90))
            params.marginEnd = dpToPx(8)
            imgView.layoutParams = params
            imgView.scaleType = ImageView.ScaleType.CENTER_CROP
            imgView.setImageBitmap(resized)
            layoutPhotos.addView(imgView, layoutPhotos.childCount - 1)

            if (photoBase64List.size >= MAX_PHOTOS) {
                btnAddPhoto.visibility = View.GONE
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Lỗi xử lý ảnh", Toast.LENGTH_SHORT).show()
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}