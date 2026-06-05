package com.example.doantotnghiep.View.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doantotnghiep.R
import java.util.Locale

class NearbyPostAdapter(
    private val onSelectionChanged: (selectedCount: Int) -> Unit
) : RecyclerView.Adapter<NearbyPostAdapter.ViewHolder>() {

    data class PostItem(
        val id: String,
        val address: String,
        val ward: String,
        val district: String,
        val price: Long,
        val lat: Double,
        val lng: Double,
        val distanceKm: Double,
        var isSelected: Boolean = false
    )

    private val items = mutableListOf<PostItem>()

    fun submitList(newItems: List<PostItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
        onSelectionChanged(getSelectedCount())
    }

    fun selectAll(select: Boolean) {
        items.forEach { it.isSelected = select }
        notifyDataSetChanged()
        onSelectionChanged(if (select) items.size else 0)
    }

    fun getSelectedItems(): List<PostItem> = items.filter { it.isSelected }
    fun getSelectedCount(): Int = items.count { it.isSelected }
    fun areAllSelected(): Boolean = items.isNotEmpty() && items.all { it.isSelected }
    fun isEmpty(): Boolean = items.isEmpty()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cbSelect: CheckBox  = view.findViewById(R.id.cbNearbyPostSelect)
        val tvAddress: TextView = view.findViewById(R.id.tvNearbyPostAddress)
        val tvLocation: TextView = view.findViewById(R.id.tvNearbyPostLocation)
        val tvPrice: TextView   = view.findViewById(R.id.tvNearbyPostPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_nearby_post, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvAddress.text = item.address.ifBlank { "Chưa có địa chỉ" }

        holder.tvLocation.text = listOf(item.ward, item.district)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(", ")

        holder.tvPrice.text = buildString {
            append(
                if (item.price > 0)
                    String.format(Locale("vi", "VN"), "%,d đ/tháng", item.price)
                else
                    "Liên hệ"
            )
            if (item.distanceKm >= 0)
                append("  •  ${String.format(Locale.US, "%.1f", item.distanceKm)} km")
        }

        holder.cbSelect.isChecked = item.isSelected

        holder.itemView.setOnClickListener {
            item.isSelected = !item.isSelected
            holder.cbSelect.isChecked = item.isSelected
            onSelectionChanged(getSelectedCount())
        }
    }

    override fun getItemCount(): Int = items.size
}
