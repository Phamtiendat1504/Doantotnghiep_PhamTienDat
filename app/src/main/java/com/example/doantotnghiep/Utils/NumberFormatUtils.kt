package com.example.doantotnghiep.Utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object NumberFormatUtils {

    private val formatter: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN"))
        symbols.groupingSeparator = '.'
        DecimalFormat("#,###", symbols)
    }

    fun addFormatWatcher(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return
                isFormatting = true

                val originalString = s.toString().replace(".", "")
                if (originalString.isNotEmpty()) {
                    try {
                        val number = originalString.toLong()
                        val formatted = formatter.format(number)
                        editText.setText(formatted)
                        editText.setSelection(formatted.length)
                    } catch (e: NumberFormatException) {
                        // Giữ nguyên nếu lỗi
                    }
                }

                isFormatting = false
            }
        })
    }

    // Lấy số gốc (bỏ dấu chấm) từ EditText
    fun getRawNumber(editText: EditText): String {
        return editText.text.toString().replace(".", "")
    }
}