package com.example.doantotnghiep.View.Auth

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.doantotnghiep.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PaymentHistoryActivity : AppCompatActivity() {

    private lateinit var layoutPayments: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView

    private val db = FirebaseFirestore.getInstance()
    private val formatter by lazy {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN"))
        symbols.groupingSeparator = '.'
        DecimalFormat("#,###", symbols)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContentView())
        findViewById<ImageView>(R.id.btnBack)?.setOnClickListener { finish() }
        loadPayments()
    }

    private fun buildContentView(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFF5F7FA.toInt())

            addView(LinearLayout(this@PaymentHistoryActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(18), dp(16), dp(14))
                setBackgroundColor(0xFF1976D2.toInt())

                addView(ImageView(this@PaymentHistoryActivity).apply {
                    id = R.id.btnBack
                    setImageResource(R.drawable.ic_back)
                    setColorFilter(0xFFFFFFFF.toInt())
                    setPadding(dp(8), dp(8), dp(8), dp(8))
                    layoutParams = LinearLayout.LayoutParams(dp(42), dp(42))
                })
                addView(TextView(this@PaymentHistoryActivity).apply {
                    text = "Lịch sử thanh toán"
                    textSize = 20f
                    setTextColor(0xFFFFFFFF.toInt())
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginStart = dp(12)
                    }
                })
            })

            progressBar = ProgressBar(this@PaymentHistoryActivity).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    topMargin = dp(24)
                }
            }
            addView(progressBar)

            tvEmpty = TextView(this@PaymentHistoryActivity).apply {
                text = "Chưa có giao dịch nào"
                textSize = 15f
                gravity = Gravity.CENTER
                setTextColor(0xFF757575.toInt())
                visibility = View.GONE
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = dp(28)
                }
            }
            addView(tvEmpty)

            addView(ScrollView(this@PaymentHistoryActivity).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
                layoutPayments = LinearLayout(this@PaymentHistoryActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(14), dp(14), dp(14), dp(18))
                }
                addView(layoutPayments)
            })
        }
    }

    private fun loadPayments() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        progressBar.visibility = View.VISIBLE
        val slotTask = db.collection("slot_upgrade_requests")
            .whereEqualTo("uid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .get()
        val featuredTask = db.collection("featured_upgrade_requests")
            .whereEqualTo("uid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .get()

        com.google.android.gms.tasks.Tasks.whenAllSuccess<Any>(slotTask, featuredTask)
            .addOnSuccessListener { results ->
                val rows = mutableListOf<Map<String, Any>>()
                results.forEach { result ->
                    val snap = result as? com.google.firebase.firestore.QuerySnapshot ?: return@forEach
                    snap.documents.forEach { doc ->
                        val data = doc.data?.toMutableMap() ?: mutableMapOf()
                        data["serviceType"] = if (doc.reference.parent.id == "featured_upgrade_requests") "featured" else "slot"
                        rows.add(data)
                    }
                }
                renderPayments(rows.sortedByDescending { (it["createdAt"] as? Number)?.toLong() ?: 0L })
            }
            .addOnFailureListener {
                tvEmpty.text = "Không thể tải lịch sử thanh toán"
                tvEmpty.visibility = View.VISIBLE
            }
            .addOnCompleteListener { progressBar.visibility = View.GONE }
    }

    private fun renderPayments(rows: List<Map<String, Any>>) {
        layoutPayments.removeAllViews()
        tvEmpty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        rows.forEach { layoutPayments.addView(createPaymentCard(it)) }
    }

    private fun createPaymentCard(data: Map<String, Any>): CardView {
        val type = data["serviceType"] as? String ?: "slot"
        val label = data["label"] as? String ?: if (type == "featured") "Gói nổi bật" else "Gói lượt đăng"
        val amount = (data["amount"] as? Number)?.toLong() ?: 0L
        val status = data["status"] as? String ?: "waiting_for_payment"
        val createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L
        val roomTitle = data["roomTitle"] as? String ?: ""
        return CardView(this).apply {
            radius = dp(14).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(0xFFFFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(10)
            }
            addView(LinearLayout(this@PaymentHistoryActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                addView(TextView(this@PaymentHistoryActivity).apply {
                    text = if (type == "featured") "Đẩy nổi bật: $label" else "Mua lượt đăng: $label"
                    textSize = 15f
                    setTextColor(0xFF1A1A2E.toInt())
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                })
                if (roomTitle.isNotBlank()) addView(TextView(this@PaymentHistoryActivity).apply {
                    text = roomTitle
                    textSize = 12f
                    setTextColor(0xFF666666.toInt())
                    setPadding(0, dp(3), 0, 0)
                })
                addView(TextView(this@PaymentHistoryActivity).apply {
                    text = "${formatter.format(amount)} đ • ${formatStatus(status)}"
                    textSize = 13f
                    setTextColor(statusColor(status))
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setPadding(0, dp(6), 0, 0)
                })
                addView(TextView(this@PaymentHistoryActivity).apply {
                    text = if (createdAt > 0) SimpleDateFormat("HH:mm dd/MM/yyyy", Locale("vi", "VN")).format(Date(createdAt)) else ""
                    textSize = 11f
                    setTextColor(0xFF999999.toInt())
                    setPadding(0, dp(4), 0, 0)
                })
            })
        }
    }

    private fun formatStatus(status: String): String = when (status) {
        "paid" -> "Đã thanh toán"
        "paid_waiting_admin" -> "Đã thanh toán, chờ admin duyệt"
        "approved" -> "Đã duyệt"
        "rejected" -> "Bị từ chối"
        "expired" -> "Hết hạn"
        "cancelled" -> "Đã hủy"
        else -> "Chờ thanh toán"
    }

    private fun statusColor(status: String): Int = when (status) {
        "paid", "approved" -> 0xFF2E7D32.toInt()
        "paid_waiting_admin" -> 0xFF1976D2.toInt()
        "rejected", "expired", "cancelled" -> 0xFFD32F2F.toInt()
        else -> 0xFFE65100.toInt()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
