// HomeFragment.kt
package com.example.doantotnghiep.View.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val tv = TextView(requireContext())
        tv.text = "Trang chủ"
        tv.textSize = 24f
        tv.gravity = android.view.Gravity.CENTER
        return tv
    }
}