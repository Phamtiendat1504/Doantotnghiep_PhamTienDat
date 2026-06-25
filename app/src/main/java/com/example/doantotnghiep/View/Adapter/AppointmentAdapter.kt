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
    val appointmentDate: String,
    val appointmentDateDisplay: String,
    val appointmentTime: String,
    val note: String,
    val status: String,
    val rejectReason: String,
    val cancelReason: String,
    val tenantId: String,
    val landlordId: String,
    val roomId: String,
    val landlordConfirmDeadline: Long,
    val appointmentTimestampMs: Long,
    val hasConflict: Boolean = false
)

interface AppointmentActionListener {
    fun onConfirmLandlord(item: AppointmentItem)
    fun onRejectLandlord(item: AppointmentItem)
    fun onCancelConfirmedLandlord(item: AppointmentItem)
    fun onMarkAsViewed(item: AppointmentItem)
    fun onMarkAsNoShow(item: AppointmentItem)
    fun onMarkAsRented(item: AppointmentItem)
    fun onMarkAsNotRented(item: AppointmentItem)
    fun onTenantConfirm(item: AppointmentItem)
    fun onTenantCancel(item: AppointmentItem)
    fun onCancelPending(item: AppointmentItem)
    fun onEditSchedule(item: AppointmentItem)
    fun onMarkAsLandlordNoShow(item: AppointmentItem)
    fun onReopenRoom(item: AppointmentItem)
}

class AppointmentAdapter(
    private val listener: AppointmentActionListener
) : ListAdapter<AppointmentItem, AppointmentAdapter.ViewHolder>(DIFF_CALLBACK) {

    var isLandlord: Boolean = false

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AppointmentItem>() {
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
        val tvStatusDetail: TextView = itemView.findViewById(R.id.tvStatusDetail)
        val viewStatusIndicator: View = itemView.findViewById(R.id.viewStatusIndicator)
        val dividerButtons: View = itemView.findViewById(R.id.dividerButtons)

        val layoutLandlordPending: LinearLayout = itemView.findViewById(R.id.layoutLandlordPending)
        val btnReject: MaterialButton = itemView.findViewById(R.id.btnReject)
        val btnConfirmLandlord: MaterialButton = itemView.findViewById(R.id.btnConfirmLandlord)

        val layoutLandlordConfirmed: LinearLayout = itemView.findViewById(R.id.layoutLandlordConfirmed)
        val btnCancelConfirmed: MaterialButton = itemView.findViewById(R.id.btnCancelConfirmed)

        val layoutLandlordResult: LinearLayout = itemView.findViewById(R.id.layoutLandlordResult)
        val btnNoShow: MaterialButton = itemView.findViewById(R.id.btnNoShow)
        val btnViewed: MaterialButton = itemView.findViewById(R.id.btnViewed)

        val layoutLandlordRented: LinearLayout = itemView.findViewById(R.id.layoutLandlordRented)
        val btnNotRented: MaterialButton = itemView.findViewById(R.id.btnNotRented)
        val btnMarkRented: MaterialButton = itemView.findViewById(R.id.btnMarkRented)

        val layoutTenantPending: LinearLayout = itemView.findViewById(R.id.layoutTenantPending)
        val btnCancelPending: MaterialButton = itemView.findViewById(R.id.btnCancelPending)
        val btnEdit: MaterialButton = itemView.findViewById(R.id.btnEdit)

        val layoutTenantConfirmed: LinearLayout = itemView.findViewById(R.id.layoutTenantConfirmed)
        val btnNotComing: MaterialButton = itemView.findViewById(R.id.btnNotComing)
        val btnTenantConfirm: MaterialButton = itemView.findViewById(R.id.btnTenantConfirm)

        val layoutTenantLandlordNoShow: LinearLayout = itemView.findViewById(R.id.layoutTenantLandlordNoShow)
        val btnLandlordNoShow: MaterialButton = itemView.findViewById(R.id.btnLandlordNoShow)

        val layoutLandlordReopenRoom: LinearLayout = itemView.findViewById(R.id.layoutLandlordReopenRoom)
        val btnReopenRoom: MaterialButton = itemView.findViewById(R.id.btnReopenRoom)

        fun bind(item: AppointmentItem, isLandlord: Boolean) {
            bindStatusBadge(item)
            bindStatusIndicator(item)
            bindRoomInfo(item)
            bindPersonInfo(item, isLandlord)
            bindButtonGroups(item, isLandlord)
            bindStatusDetail(item)
        }

        private fun styleStatusBadge(textView: TextView, bgColor: String, textColor: String) {
            textView.setTextColor(Color.parseColor(textColor))
            textView.background?.let { bg ->
                val wrappedBg = androidx.core.graphics.drawable.DrawableCompat.wrap(bg).mutate()
                androidx.core.graphics.drawable.DrawableCompat.setTint(wrappedBg, Color.parseColor(bgColor))
                textView.background = wrappedBg
            }
        }

        private fun styleCallout(textView: TextView, bgColor: String, textColor: String) {
            textView.setTextColor(Color.parseColor(textColor))
            textView.background?.let { bg ->
                val wrappedBg = androidx.core.graphics.drawable.DrawableCompat.wrap(bg).mutate()
                androidx.core.graphics.drawable.DrawableCompat.setTint(wrappedBg, Color.parseColor(bgColor))
                textView.background = wrappedBg
            }
            val drawables = textView.compoundDrawablesRelative
            drawables[0]?.let { icon ->
                val wrappedIcon = androidx.core.graphics.drawable.DrawableCompat.wrap(icon).mutate()
                androidx.core.graphics.drawable.DrawableCompat.setTint(wrappedIcon, Color.parseColor(textColor))
                textView.setCompoundDrawablesRelativeWithIntrinsicBounds(wrappedIcon, drawables[1], drawables[2], drawables[3])
            }
        }

        private fun bindStatusIndicator(item: AppointmentItem) {
            val colorStr = when (item.status) {
                "pending"             -> "#FFB74D" // Orange
                "confirmed", "completed_rented" -> "#81C784" // Green
                "tenant_confirmed"    -> "#64B5F6" // Blue
                "completed_viewed"    -> "#FFB74D" // Light Orange
                "rejected", "no_show", "landlord_no_show" -> "#E57373" // Red
                "viewed_not_rented"   -> "#90A4AE" // Grey
                else                  -> "#90A4AE" // Grey
            }
            viewStatusIndicator.setBackgroundColor(Color.parseColor(colorStr))
        }

        private fun bindStatusBadge(item: AppointmentItem) {
            val (label, bgColor, textColor) = when (item.status) {
                "pending"             -> Triple("CHỜ XÁC NHẬN", "#FFF3CD", "#856404")
                "confirmed"           -> Triple("ĐÃ XÁC NHẬN", "#D4EDDA", "#155724")
                "tenant_confirmed"    -> Triple("SẼ ĐẾN XEM", "#CCE5FF", "#004085")
                "completed_viewed"    -> Triple("ĐÃ ĐẾN XEM", "#FFE5B4", "#7D4E00")
                "completed_rented"    -> Triple("ĐÃ THUÊ ĐƯỢC PHÒNG", "#D4EDDA", "#155724")
                "rejected"            -> Triple("BỊ TỪ CHỐI", "#F8D7DA", "#721C24")
                "cancelled_by_tenant" -> Triple("ĐÃ HỦY", "#E2E3E5", "#383D41")
                "cancelled_by_system" -> Triple("HỦY TỰ ĐỘNG", "#E2E3E5", "#383D41")
                "expired_pending"     -> Triple("HẾT HẠN CHỜ", "#FFE0B2", "#6D4C41")
                "no_show"             -> Triple("KHÔNG ĐẾN", "#F8D7DA", "#721C24")
                "landlord_no_show"    -> Triple("CHỦ KHÔNG ĐẾN", "#F8D7DA", "#721C24")
                "viewed_not_rented"   -> Triple("KHÔNG THUÊ PHÒNG", "#E2E3E5", "#383D41")
                else                  -> Triple(item.status.uppercase(), "#E2E3E5", "#383D41")
            }
            tvStatus.text = label
            styleStatusBadge(tvStatus, bgColor, textColor)
        }

        private fun bindStatusDetail(item: AppointmentItem) {
            val detail = when (item.status) {
                "pending" -> {
                    if (item.landlordConfirmDeadline > 0) {
                        val deadlineFmt = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.US)
                            .format(java.util.Date(item.landlordConfirmDeadline))
                        "Chủ trọ cần xác nhận trước: $deadlineFmt"
                    } else ""
                }
                "rejected" -> if (item.rejectReason.isNotEmpty()) "Lý do: ${item.rejectReason}" else ""
                "cancelled_by_tenant", "cancelled_by_system" ->
                    if (item.cancelReason.isNotEmpty()) "Lý do: ${item.cancelReason}" else ""
                "expired_pending" -> "Chủ trọ không xác nhận trong 48h"
                "completed_viewed" -> "Liên hệ chủ trọ nếu muốn thuê"
                "viewed_not_rented" -> "Khách đã xem nhưng không thuê. Phòng vẫn còn trống."
                "landlord_no_show" -> "Bạn đã báo cáo chủ trọ không đến đúng giờ hẹn."
                else -> ""
            }
            if (detail.isNotEmpty()) {
                tvStatusDetail.text = detail
                val (detailBg, detailText) = when (item.status) {
                    "rejected", "no_show", "landlord_no_show" -> Pair("#FFF5F5", "#E53E3E") // Soft Red
                    "pending" -> Pair("#FFFDF5", "#D69E2E")             // Soft Gold
                    "completed_viewed" -> Pair("#EBF8FF", "#2B6CB0")    // Soft Blue
                    else -> Pair("#F7FAFC", "#4A5568")                  // Soft Grey
                }
                styleCallout(tvStatusDetail, detailBg, detailText)
                tvStatusDetail.visibility = View.VISIBLE
            } else {
                tvStatusDetail.visibility = View.GONE
            }
        }

        private fun bindRoomInfo(item: AppointmentItem) {
            Glide.with(itemView.context).load(item.roomImageUrl?.ifEmpty { null })
                .placeholder(R.color.gray_200).error(R.color.gray_200).centerCrop().into(imgRoom)
            tvRoomTitle.text = item.roomTitle
            tvRoomAddress.text = item.roomAddress
            tvDateTime.text = "${item.appointmentDateDisplay} lúc ${item.appointmentTime}"
            tvConflict.visibility = if (item.hasConflict) View.VISIBLE else View.GONE
            if (item.note.isNotBlank()) {
                tvNote.text = "Ghi chú: ${item.note}"
                styleCallout(tvNote, "#F7FAFC", "#4A5568") // Soft Grey Note
                tvNote.visibility = View.VISIBLE
            } else {
                tvNote.visibility = View.GONE
            }
        }

        private fun bindPersonInfo(item: AppointmentItem, isLandlord: Boolean) {
            if (isLandlord) {
                val genderText = when (item.tenantGender) { "Nam" -> "Nam"; "Nữ" -> "Nữ"; else -> "" }
                tvPerson.text = buildString {
                    append("Người xem: ${item.tenantName}")
                    if (genderText.isNotEmpty()) append(" ($genderText)")
                    // Chỉ hiển thị SĐT khi status >= confirmed
                    if (item.status in listOf("confirmed", "tenant_confirmed", "completed_viewed", "viewed_not_rented", "landlord_no_show", "completed_rented") && item.tenantPhone.isNotEmpty())
                        append(" — ${item.tenantPhone}")
                }
            } else {
                tvPerson.text = buildString {
                    append("Chủ phòng: ${item.landlordName}")
                    // Chỉ hiển thị SĐT khi đã confirmed trở lên
                    if (item.status in listOf("confirmed", "tenant_confirmed", "completed_viewed", "viewed_not_rented", "landlord_no_show", "completed_rented") && item.landlordPhone.isNotEmpty())
                        append(" — ${item.landlordPhone}")
                }
            }
        }

        private fun bindButtonGroups(item: AppointmentItem, isLandlord: Boolean) {
            layoutLandlordPending.visibility = View.GONE
            layoutLandlordConfirmed.visibility = View.GONE
            layoutLandlordResult.visibility = View.GONE
            layoutLandlordRented.visibility = View.GONE
            layoutLandlordReopenRoom.visibility = View.GONE
            layoutTenantPending.visibility = View.GONE
            layoutTenantConfirmed.visibility = View.GONE
            layoutTenantLandlordNoShow.visibility = View.GONE
            dividerButtons.visibility = View.GONE

            if (isLandlord) {
                when (item.status) {
                    "pending" -> {
                        layoutLandlordPending.visibility = View.VISIBLE
                        btnReject.setOnClickListener { listener.onRejectLandlord(item) }
                        btnConfirmLandlord.setOnClickListener { listener.onConfirmLandlord(item) }
                    }
                    "confirmed", "tenant_confirmed" -> {
                        // Cho phép chủ trọ cập nhật kết quả từ trước giờ hẹn 30 phút (trường hợp khách đến sớm)
                        val now = System.currentTimeMillis()
                        val showResultTimeMs = item.appointmentTimestampMs - 30 * 60 * 1000L
                        if (item.appointmentTimestampMs > 0 && now >= showResultTimeMs) {
                            layoutLandlordResult.visibility = View.VISIBLE
                            btnNoShow.setOnClickListener { listener.onMarkAsNoShow(item) }
                            btnViewed.setOnClickListener { listener.onMarkAsViewed(item) }
                        } else {
                            layoutLandlordConfirmed.visibility = View.VISIBLE
                            btnCancelConfirmed.setOnClickListener { listener.onCancelConfirmedLandlord(item) }
                        }
                    }
                    "completed_viewed" -> {
                        layoutLandlordRented.visibility = View.VISIBLE
                        btnNotRented.setOnClickListener { listener.onMarkAsNotRented(item) }
                        btnMarkRented.setOnClickListener { listener.onMarkAsRented(item) }
                    }
                    "completed_rented" -> {
                        layoutLandlordReopenRoom.visibility = View.VISIBLE
                        btnReopenRoom.setOnClickListener { listener.onReopenRoom(item) }
                    }
                }
            } else {
                when (item.status) {
                    "pending" -> {
                        layoutTenantPending.visibility = View.VISIBLE
                        btnCancelPending.setOnClickListener { listener.onCancelPending(item) }
                        btnEdit.setOnClickListener { listener.onEditSchedule(item) }
                    }
                    "confirmed", "tenant_confirmed" -> {
                        val now = System.currentTimeMillis()
                        val showResultTimeMs = item.appointmentTimestampMs - 30 * 60 * 1000L
                        if (item.appointmentTimestampMs > 0 && now >= showResultTimeMs) {
                            // Sau giờ hẹn: tenant có thể báo cáo chủ không đến
                            layoutTenantLandlordNoShow.visibility = View.VISIBLE
                            btnLandlordNoShow.setOnClickListener { listener.onMarkAsLandlordNoShow(item) }
                        } else {
                            // Trước giờ hẹn: hiển thị nút hủy / xác nhận
                            layoutTenantConfirmed.visibility = View.VISIBLE
                            btnNotComing.setOnClickListener { listener.onTenantCancel(item) }
                            if (item.status == "confirmed") {
                                btnTenantConfirm.visibility = View.VISIBLE
                                btnTenantConfirm.setOnClickListener { listener.onTenantConfirm(item) }
                            } else {
                                btnTenantConfirm.visibility = View.GONE
                            }
                        }
                    }
                }
            }

            val hasButtons = layoutLandlordPending.visibility == View.VISIBLE ||
                             layoutLandlordConfirmed.visibility == View.VISIBLE ||
                             layoutLandlordResult.visibility == View.VISIBLE ||
                             layoutLandlordRented.visibility == View.VISIBLE ||
                             layoutLandlordReopenRoom.visibility == View.VISIBLE ||
                             layoutTenantPending.visibility == View.VISIBLE ||
                             layoutTenantConfirmed.visibility == View.VISIBLE ||
                             layoutTenantLandlordNoShow.visibility == View.VISIBLE
            dividerButtons.visibility = if (hasButtons) View.VISIBLE else View.GONE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_appointment, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position), isLandlord)
}
