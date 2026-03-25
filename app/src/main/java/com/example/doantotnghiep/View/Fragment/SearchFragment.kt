package com.example.doantotnghiep.View.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.AddressData
import com.example.doantotnghiep.Utils.NumberFormatUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class SearchFragment : Fragment() {

    private lateinit var chipPhuong: Chip
    private lateinit var chipXa: Chip
    private lateinit var spinnerArea: Spinner
    private lateinit var edtAddress: EditText
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
    private lateinit var chipCurfewCustom: Chip
    private lateinit var edtCurfewTime: EditText
    private lateinit var chipGroupCurfew: ChipGroup
    private lateinit var chipGroupPrice: ChipGroup
    private lateinit var btnSearch: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chipPhuong = view.findViewById(R.id.chipPhuong)
        chipXa = view.findViewById(R.id.chipXa)
        spinnerArea = view.findViewById(R.id.spinnerArea)
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
        chipCurfewCustom = view.findViewById(R.id.chipCurfewCustom)
        edtCurfewTime = view.findViewById(R.id.edtCurfewTime)
        chipGroupCurfew = view.findViewById(R.id.chipGroupCurfew)
        btnSearch = view.findViewById(R.id.btnSearch)

        // Mặc định hiển thị danh sách Phường
        loadAreaSpinner(AddressData.phuongList)

        // Chuyển đổi Phường / Xã
        chipPhuong.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) loadAreaSpinner(AddressData.phuongList)
        }
        chipXa.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) loadAreaSpinner(AddressData.xaList)
        }

        // Giá thuê: chọn "Tùy chọn" → hiện ô nhập giá
        chipGroupPrice.setOnCheckedStateChangeListener { _, checkedIds ->
            edtCustomPrice.visibility = if (checkedIds.contains(R.id.chipPriceCustom)) View.VISIBLE else View.GONE
        }

        // Tự động format số khi nhập giá
        NumberFormatUtils.addFormatWatcher(edtCustomPrice)
        NumberFormatUtils.addFormatWatcher(edtWifiPrice)
        NumberFormatUtils.addFormatWatcher(edtElectricPrice)
        NumberFormatUtils.addFormatWatcher(edtWaterPrice)

        // Khi tick Wifi → bật ô nhập giá
        cbWifi.setOnCheckedChangeListener { _, isChecked ->
            edtWifiPrice.isEnabled = isChecked
            if (!isChecked) edtWifiPrice.text?.clear()
        }

        // Khi tick Điện → bật ô nhập giá
        cbElectric.setOnCheckedChangeListener { _, isChecked ->
            edtElectricPrice.isEnabled = isChecked
            if (!isChecked) edtElectricPrice.text?.clear()
        }

        // Khi tick Nước → bật ô nhập giá
        cbWater.setOnCheckedChangeListener { _, isChecked ->
            edtWaterPrice.isEnabled = isChecked
            if (!isChecked) edtWaterPrice.text?.clear()
        }

        // Giờ khóa cửa: chọn "Có giờ giấc" → hiện ô nhập giờ
        chipGroupCurfew.setOnCheckedStateChangeListener { _, checkedIds ->
            edtCurfewTime.visibility = if (checkedIds.contains(R.id.chipCurfewCustom)) View.VISIBLE else View.GONE
        }

        // Nút Tìm kiếm
        btnSearch.setOnClickListener {
            Toast.makeText(requireContext(), "Chức năng tìm kiếm đang phát triển", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadAreaSpinner(list: Array<String>) {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            list
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerArea.adapter = adapter
    }
}