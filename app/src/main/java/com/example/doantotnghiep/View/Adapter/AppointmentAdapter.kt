package com.example.doantotnghiep.View.Adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doantotnghiep.R
import com.google.android.material.button.MaterialButton

data class AppointmentItem(
    val id: String,
    val tenantName: String,
    val tenantPhone: String,
    val tenantGender: String,
    val landlordName: String,
    val landlordPhone: String,
    val roomTitle: String,
    val roomAddress: String,
    val roomImageUrl: String?,
    val date: String,
    val dateDisplay: String,
    val time: String,
    val note: String,
    val status: String,
    val rejectReason: String,
    val tenantId: String,
    val landlordId: String,
    val roomId: String,
    val hasConflict: Boolean
)

interface AppointmentActionListener {
    fun onConfirmLandlord(item: AppointmentItem)
    fun onRejectLandlord(item: AppointmentItem)
    fun onMarkAsRented(item: AppointmentItem)
    fun onTenantConfirm(item: AppointmentItem)
    fun onTenantCancel(item: AppointmentItem)
    fun onCancelPending(item: AppointmentItem)
    fun onEditSchedule(item: AppointmentItem)
}

class AppointmentAdapter(
    private val listener: AppointmentActionListener
) : ListAdapter<AppointmentItem, AppointmentAdapter.ViewHolder>(DIFF_CALLBACK) {

    var isLandlord: Boolean = false

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AppointmentItem>() {
            override fun areItemsTheSame(old: AppointmentItem, new: AppointmentItem) = old.id == new.id
            override fun areContentsTheSame(old: AppointmentItem, new: AppointmentItem) = old == new
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgRoom: ImageView = itemView.findViewById(R.id.imgRoom)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvRoomTitle: TextView = itemView.findViewById(R.id.tvRoomTitle)
        val tvRoomAddress: TextView = itemView.findViewById(R.id.tvRoomAddress)
        val tvDateTime: TextView = itemView.findViewById(R.id.tvDateTime)
        val tvConflict: TextView = itemView.findViewById(R.id.tvConflict)
        val tvPerson: TextView = itemView.findViewById(R.id.tvPerson)
        val tvNote: TextView = itemView.findViewById(R.id.tvNote)

        val layoutLandlordPending: LinearLayout = itemView.findViewById(R.id.layoutLandlordPending)
        val btnReject: MaterialButton = itemView.findViewById(R.id.btnReject)
        val btnConfirmLandlord: MaterialButton = itemView.findViewById(R.id.btnConfirmLandlord)

        val layoutLandlordRented: LinearLayout = itemView.findViewById(R.id.layoutLandlordRented)
        val btnMarkRented: MaterialButton = itemView.findViewById(R.id.btnMarkRented)

        val layoutTenantPending: LinearLayout = itemView.findViewById(R.id.layoutTenantPending)
        val btnCancelPending: MaterialButton = itemView.findViewById(R.id.btnCancelPending)
        val btnEdit: MaterialButton = itemView.findViewById(R.id.btnEdit)

        val layoutTenantConfirmed: LinearLayout = itemView.findViewById(R.id.layoutTenantConfirmed)
        val btnNotComing: MaterialButton = itemView.findViewById(R.id.btnNotComing)
        val btnTenantConfirm: MaterialButton = itemView.findViewById(R.id.btnTenantConfirm)

        fun bind(item: AppointmentItem, isLandlord: Boolean) {
            bindStatusBadge(item)
            bindRoomInfo(item)
            bindPersonInfo(item, isLandlord)
            bindButtonGroups(item, isLandlord)
        }

        private fun bindStatusBadge(item: AppointmentItem) {
            val (label, bgColor, textColor) = when (item.status) {
                "pending" -> Triple("Chờ xác nhận", "#FFF3CD", "#856404")
                "confirmed" -> Triple("Đã xác nhận", "#D4EDDA", "#155724")
                "tenant_confirmed" -> Triple("Đã xác nhận đến", "#CCE5FF", "#004085")
                "completed_rented" -> Triple("Đã cho thuê", "#D4EDDA", "#155724")
                "rejected" -> Triple("Đã từ chối", "#F8D7DA", "#721C24")
                "cancelled_by_tenant" -> Triple("Đã hủy", "#E2E3E5", "#383D41")
                "cancelled_by_system" -> Triple("Hủy tự động", "#E2E3E5", "#383D41")
                else -> Triple(item.status, "#E2E3E5", "#383D41")
            }
            tvStatus.text = label
            tvStatus.setBackgroundColor(Color.parseColor(bgColor))
            tvStatus.setTextColor(Color.parseColor(textColor))
        }

        private fun bindRoomInfo(item: AppointmentItem) {
            Glide.with(itemView.context)
                .load(item.roomImageUrl?.ifEmpty { null })
                .placeholder(R.color.gray_200)
                .error(R.color.gray_200)
                .centerCrop()
                .into(imgRoom)

            tvRoomTitle.text = item.roomTitle
            tvRoomAddress.text = item.roomAddress
            tvDateTime.text = "${item.dateDisplay} lúc ${item.time}"
            tvConflict.visibility = if (item.hasConflict) View.VISIBLE else View.GONE

            if (item.note.isNotBlank()) {
                tvNote.text = "Ghi chú: ${item.note}"
                tvNote.visibility = View.VISIBLE
            } else {
                tvNote.visibility = View.GONE
            }
        }

        private fun bindPersonInfo(item: AppointmentItem, isLandlord: Boolean) {
            if (isLandlord) {
                val genderText = when (item.tenantGender) {
                    "male" -> "Nam"
                    "female" -> "Nữ"
                    else -> ""
                }
                tvPerson.text = buildString {
                    append("Người xem: ${item.tenantName}")
                    if (genderText.isNotEmpty()) append(" ($genderText)")
                    if (item.tenantPhone.isNotEmpty()) append(" — ${item.tenantPhone}")
                }
            } else {
                tvPerson.text = "Chủ phòng: ${item.landlordName}" +
                    if (item.landlordPhone.isNotEmpty()) " — ${item.landlordPhone}" else ""
            }
        }

        private fun bindButtonGroups(item: AppointmentItem, isLandlord: Boolean) {
            layoutLandlordPending.visibility = View.GONE
            layoutLandlordRented.visibility = View.GONE
            layoutTenantPending.visibility = View.GONE
            layoutTenantConfirmed.visibility = View.GONE

            if (isLandlord) {
                when (item.status) {
                    "pending" -> {
                        layoutLandlordPending.visibility = View.VISIBLE
                        btnReject.setOnClickListener { listener.onRejectLandlord(item) }
                        btnConfirmLandlord.setOnClickListener { listener.onConfirmLandlord(item) }
                    }
                    "tenant_confirmed" -> {
                        layoutLandlordRented.visibility = View.VISIBLE
                        btnMarkRented.setOnClickListener { listener.onMarkAsRented(item) }
                    }
                }
            } else {
                when (item.status) {
                    "pending" -> {
                        layoutTenantPending.visibility = View.VISIBLE
                        btnCancelPending.setOnClickListener { listener.onCancelPending(item) }
                        btnEdit.setOnClickListener { listener.onEditSchedule(item) }
                    }
                    "confirmed" -> {
                        layoutTenantConfirmed.visibility = View.VISIBLE
                        btnNotComing.setOnClickListener { listener.onTenantCancel(item) }
                        btnTenantConfirm.setOnClickListener { listener.onTenantConfirm(item) }
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), isLandlord)
    }
}
