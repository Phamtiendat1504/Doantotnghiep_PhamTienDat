package com.example.doantotnghiep.View.Fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.example.doantotnghiep.R
import com.example.doantotnghiep.Utils.AddressData
import com.example.doantotnghiep.Utils.NumberFormatUtils
import com.example.doantotnghiep.View.Auth.SearchResultsActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class SearchFragment : Fragment() {

    private lateinit var chipPhuong: Chip
    private lateinit var chipXa: Chip
    private lateinit var chipScopeWard: Chip
    private lateinit var chipScopeDistrict: Chip
    private lateinit var spinnerArea: Spinner
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
    private lateinit var rgCurfew: RadioGroup
    private lateinit var edtCurfewTime: EditText
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
        chipScopeWard = view.findViewById(R.id.chipScopeWard)
        chipScopeDistrict = view.findViewById(R.id.chipScopeDistrict)
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
        rgCurfew = view.findViewById(R.id.rgCurfew)
        edtCurfewTime = view.findViewById(R.id.edtCurfewTime)
        btnSearch = view.findViewById(R.id.btnSearch)

        // Mặc định hiển thị Phường
        loadAreaSpinner(AddressData.phuongList)

        // Chuyển Phường / Xã
        chipPhuong.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) loadAreaSpinner(AddressData.phuongList)
        }
        chipXa.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) loadAreaSpinner(AddressData.xaList)
        }

        // Tùy chọn giá
        chipPriceCustom.setOnCheckedChangeListener { _, isChecked ->
            edtCustomPrice.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (isChecked) chipGroupPrice.clearCheck()
        }

        // Format giá tiền
        NumberFormatUtils.addFormatWatcher(edtCustomPrice)
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

        // Giờ giấc
        rgCurfew.setOnCheckedChangeListener { _, checkedId ->
            edtCurfewTime.visibility =
                if (checkedId == R.id.rbCurfewCustom) View.VISIBLE else View.GONE
        }

        // Nút tìm kiếm
        btnSearch.setOnClickListener {
            val selectedItem = spinnerArea.selectedItem?.toString() ?: ""

            if (selectedItem.isEmpty() || spinnerArea.selectedItemPosition == 0) {
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Chưa chọn khu vực")
                    .setMessage("Vui lòng chọn khu vực bạn muốn tìm phòng trọ để bắt đầu tìm kiếm.")
                    .setPositiveButton("Đã hiểu", null)
                    .show()
                return@setOnClickListener
            }

            // Parse "Mộ Lao (Hà Đông)" → ward = "Mộ Lao", district = "Hà Đông"
            val wardName: String
            val districtName: String
            val parenStart = selectedItem.indexOf('(')
            val parenEnd = selectedItem.indexOf(')')
            if (parenStart > 0 && parenEnd > parenStart) {
                wardName = selectedItem.substring(0, parenStart).trim()
                districtName = selectedItem.substring(parenStart + 1, parenEnd).trim()
            } else {
                wardName = selectedItem
                districtName = ""
            }

            val searchMode = if (chipScopeDistrict.isChecked) "district" else "ward"

            val intent = Intent(requireContext(), SearchResultsActivity::class.java)
            intent.putExtra("ward", wardName)
            intent.putExtra("district", districtName)
            intent.putExtra("searchMode", searchMode)

            // Giá
            var minPrice = 0L
            var maxPrice = 0L

            if (chipPriceCustom.isChecked && edtCustomPrice.text.isNotEmpty()) {
                val customPrice = edtCustomPrice.text.toString()
                    .replace(".", "").replace(",", "").toLongOrNull() ?: 0
                minPrice = customPrice - 500000
                maxPrice = customPrice + 500000
            } else {
                when (chipGroupPrice.checkedChipId) {
                    R.id.chipPrice1 -> { minPrice = 1000000; maxPrice = 3000000 }
                    R.id.chipPrice2 -> { minPrice = 3000000; maxPrice = 6000000 }
                    R.id.chipPrice3 -> { minPrice = 6000000; maxPrice = 9000000 }
                    R.id.chipPrice4 -> { minPrice = 9000000; maxPrice = 12000000 }
                    R.id.chipPrice5 -> { minPrice = 12000000; maxPrice = 0 }
                }
            }
            intent.putExtra("minPrice", minPrice)
            intent.putExtra("maxPrice", maxPrice)

            // Diện tích
            val roomArea = edtRoomArea.text.toString().toIntOrNull() ?: 0
            if (roomArea > 0) {
                intent.putExtra("minArea", roomArea - 5)
                intent.putExtra("maxArea", roomArea + 5)
            } else {
                intent.putExtra("minArea", 0)
                intent.putExtra("maxArea", 0)
            }

            // Tiện ích
            intent.putExtra("hasWifi", cbWifi.isChecked)
            intent.putExtra("hasAirCon", false)
            intent.putExtra("hasWaterHeater", false)
            intent.putExtra("hasParking", false)

            // Giờ giấc
            val curfew = when (rgCurfew.checkedRadioButtonId) {
                R.id.rbCurfewFree -> "Tự do"
                R.id.rbCurfewCustom -> "Tùy chọn"
                else -> ""
            }
            intent.putExtra("curfew", curfew)
            intent.putExtra("genderPrefer", "")

            startActivity(intent)
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