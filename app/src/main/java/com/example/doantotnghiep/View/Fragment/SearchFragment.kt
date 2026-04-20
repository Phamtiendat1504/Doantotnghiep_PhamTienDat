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
import android.widget.RadioGroup
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

        loadAreaOptions(AddressData.phuongList)
        setupAreaPickerBehavior()

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

    private fun setupAreaPickerBehavior() {
        autoArea.setOnClickListener { autoArea.showDropDown() }
        autoArea.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) autoArea.showDropDown() }
    }

    private fun submitSearch() {
        val selectedItem = autoArea.text?.toString()?.trim().orEmpty()
        if (selectedItem.isEmpty()) {
            showInfoDialog(
                title = "\u0043\u0068\u01b0\u0061\u0020\u0063\u0068\u1ecd\u006e\u0020\u006b\u0068\u0075\u0020\u0076\u1ef1\u0063",
                message = "\u0056\u0075\u0069\u0020\u006c\u00f2\u006e\u0067\u0020\u0063\u0068\u1ecd\u006e\u0020\u006b\u0068\u0075\u0020\u0076\u1ef1\u0063\u0020\u0111\u1ec3\u0020\u0074\u00ec\u006d\u0020\u006b\u0069\u1ebf\u006d\u002e"
            )
            return
        }

        if (selectedItem !in currentAreaOptions) {
            showInfoDialog(
                title = "\u004b\u0068\u0075\u0020\u0076\u1ef1\u0063\u0020\u006b\u0068\u00f4\u006e\u0067\u0020\u0068\u1ee3\u0070\u0020\u006c\u1ec7",
                message = "\u0056\u0075\u0069\u0020\u006c\u00f2\u006e\u0067\u0020\u0063\u0068\u1ecd\u006e\u0020\u0074\u1eeb\u0020\u0064\u0061\u006e\u0068\u0020\u0073\u00e1\u0063\u0068\u0020\u0067\u1ee3\u0069\u0020\u00fd\u002e"
            )
            return
        }

        val (wardName, districtName) = parseAreaSelection(selectedItem)
        val searchMode = if (chipScopeDistrict.isChecked) "district" else "ward"
        val districtForSearch = if (searchMode == "district" && districtName.equals("Hà Nội", ignoreCase = true)) {
            wardName
        } else {
            districtName
        }

        val intent = Intent(requireContext(), SearchResultsActivity::class.java)
        intent.putExtra("ward", wardName)
        intent.putExtra("district", districtForSearch)
        intent.putExtra("searchMode", searchMode)

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
            .setPositiveButton("\u0110\u00e3\u0020\u0068\u0069\u1ec3\u0075", null)
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

