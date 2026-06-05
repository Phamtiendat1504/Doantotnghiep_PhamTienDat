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
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PaymentHistoryActivity : AppCompatActivity() {

    private lateinit var layoutPayments: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var tvTabAll: TextView
    private lateinit var tvTabSlot: TextView
    private lateinit var tvTabFeatured: TextView

    private val db = FirebaseFirestore.getInstance()
    private val formatter by lazy {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN"))
        symbols.groupingSeparator = '.'
        DecimalFormat("#,###", symbols)
    }

    private var allRows: List<Map<String, Any>> = emptyList()
    private var activeTab = "all" // "all" | "slot" | "featured"

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

            // ── Toolbar ──
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

            // ── Tab Filter ──
            addView(buildTabBar())

            // ── ProgressBar ──
            progressBar = ProgressBar(this@PaymentHistoryActivity).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    topMargin = dp(24)
                }
            }
            addView(progressBar)

            // ── Empty state ──
            tvEmpty = TextView(this@PaymentHistoryActivity).apply {
                text = "Chưa có giao dịch nào"
                textSize = 15f
                gravity = Gravity.CENTER
                setTextColor(0xFF757575.toInt())
                visibility = View.GONE
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(28) }
            }
            addView(tvEmpty)

            // ── Scrollable list ──
            addView(ScrollView(this@PaymentHistoryActivity).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
                layoutPayments = LinearLayout(this@PaymentHistoryActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(14), dp(12), dp(14), dp(18))
                }
                addView(layoutPayments)
            })
        }
    }

    private fun buildTabBar(): View {
        val tabBg = 0xFFFFFFFF.toInt()
        val activeColor = 0xFF1976D2.toInt()
        val inactiveColor = 0xFF757575.toInt()

        val tabParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        fun makeTab(label: String, tag: String): TextView {
            return TextView(this).apply {
                text = label
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(dp(4), dp(12), dp(4), dp(12))
                layoutParams = tabParams
                setTextColor(if (tag == activeTab) activeColor else inactiveColor)
                setTypeface(typeface, if (tag == activeTab) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
                setOnClickListener { switchTab(tag) }
            }
        }

        tvTabAll = makeTab("Tất cả", "all")
        tvTabSlot = makeTab("Mua lượt đăng", "slot")
        tvTabFeatured = makeTab("Đẩy nổi bật", "featured")

        val indicator = View(this).apply {
            setBackgroundColor(activeColor)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(3))
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(tabBg)
            elevation = dp(2).toFloat()

            val row = LinearLayout(this@PaymentHistoryActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(tvTabAll)
                addView(tvTabSlot)
                addView(tvTabFeatured)
            }
            addView(row)
            addView(indicator)
        }
    }

    private fun switchTab(tab: String) {
        activeTab = tab
        val activeColor = 0xFF1976D2.toInt()
        val inactiveColor = 0xFF757575.toInt()

        listOf(tvTabAll to "all", tvTabSlot to "slot", tvTabFeatured to "featured").forEach { (tv, tag) ->
            tv.setTextColor(if (tag == tab) activeColor else inactiveColor)
            tv.setTypeface(null, if (tag == tab) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }

        renderPayments(filteredRows())
    }

    private fun filteredRows(): List<Map<String, Any>> = when (activeTab) {
        "slot" -> allRows.filter { it["serviceType"] == "slot" }
        "featured" -> allRows.filter { it["serviceType"] == "featured" }
        else -> allRows
    }

    private fun loadPayments() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        progressBar.visibility = View.VISIBLE

        val slotTask = db.collection("slot_upgrade_requests")
            .whereEqualTo("uid", uid)
            .limit(50)
            .get()
        val featuredTask = db.collection("featured_upgrade_requests")
            .whereEqualTo("uid", uid)
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
                allRows = rows.sortedByDescending { (it["createdAt"] as? Number)?.toLong() ?: 0L }
                updateTabBadges()
                renderPayments(filteredRows())
            }
            .addOnFailureListener {
                tvEmpty.text = "Không thể tải lịch sử thanh toán"
                tvEmpty.visibility = View.VISIBLE
            }
            .addOnCompleteListener { progressBar.visibility = View.GONE }
    }

    private fun updateTabBadges() {
        val slotCount = allRows.count { it["serviceType"] == "slot" }
        val featuredCount = allRows.count { it["serviceType"] == "featured" }
        tvTabAll.text = "Tất cả (${allRows.size})"
        tvTabSlot.text = "Lượt đăng ($slotCount)"
        tvTabFeatured.text = "Nổi bật ($featuredCount)"
    }

    private fun renderPayments(rows: List<Map<String, Any>>) {
        layoutPayments.removeAllViews()
        val isEmpty = rows.isEmpty()
        tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        tvEmpty.text = when {
            isEmpty && activeTab == "slot" -> "Chưa có giao dịch mua lượt đăng"
            isEmpty && activeTab == "featured" -> "Chưa có giao dịch đẩy nổi bật"
            else -> "Chưa có giao dịch nào"
        }
        rows.forEach { layoutPayments.addView(createPaymentCard(it)) }
    }

    private fun createPaymentCard(data: Map<String, Any>): CardView {
        val type = data["serviceType"] as? String ?: "slot"
        val label = data["label"] as? String ?: if (type == "featured") "Gói nổi bật" else "Gói lượt đăng"
        val amount = (data["amount"] as? Number)?.toLong() ?: 0L
        val status = data["status"] as? String ?: "waiting_for_payment"
        val createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L
        val roomTitle = data["roomTitle"] as? String ?: ""

        val accentColor = if (type == "featured") 0xFF7B1FA2.toInt() else 0xFF1565C0.toInt()

        return CardView(this).apply {
            radius = dp(14).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(0xFFFFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }

            addView(LinearLayout(this@PaymentHistoryActivity).apply {
                orientation = LinearLayout.HORIZONTAL

                // ── Thanh màu bên trái phân biệt loại ──
                addView(View(this@PaymentHistoryActivity).apply {
                    setBackgroundColor(accentColor)
                    layoutParams = LinearLayout.LayoutParams(dp(4), LinearLayout.LayoutParams.MATCH_PARENT)
                })

                addView(LinearLayout(this@PaymentHistoryActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(12), dp(12), dp(12), dp(12))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

                    // Loại dịch vụ + tên gói
                    addView(LinearLayout(this@PaymentHistoryActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL

                        // Badge loại
                        addView(TextView(this@PaymentHistoryActivity).apply {
                            text = if (type == "featured") "Nổi bật" else "Lượt đăng"
                            textSize = 10f
                            setTextColor(0xFFFFFFFF.toInt())
                            setBackgroundColor(accentColor)
                            setPadding(dp(6), dp(2), dp(6), dp(2))
                            background = android.graphics.drawable.GradientDrawable().apply {
                                setColor(accentColor)
                                cornerRadius = dp(4).toFloat()
                            }
                        })

                        addView(TextView(this@PaymentHistoryActivity).apply {
                            text = label
                            textSize = 14f
                            setTextColor(0xFF1A1A2E.toInt())
                            setTypeface(typeface, android.graphics.Typeface.BOLD)
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { marginStart = dp(8) }
                        })
                    })

                    // Tên bài đăng (chỉ hiện với featured)
                    if (roomTitle.isNotBlank()) {
                        addView(TextView(this@PaymentHistoryActivity).apply {
                            text = "📋 $roomTitle"
                            textSize = 12f
                            setTextColor(0xFF555555.toInt())
                            setPadding(0, dp(4), 0, 0)
                        })
                    }

                    // Số tiền + Trạng thái
                    addView(LinearLayout(this@PaymentHistoryActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(0, dp(6), 0, 0)

                        addView(TextView(this@PaymentHistoryActivity).apply {
                            text = "${formatter.format(amount)} đ"
                            textSize = 14f
                            setTextColor(0xFF1A1A2E.toInt())
                            setTypeface(typeface, android.graphics.Typeface.BOLD)
                        })

                        addView(TextView(this@PaymentHistoryActivity).apply {
                            text = "  •  "
                            textSize = 13f
                            setTextColor(0xFFBDBDBD.toInt())
                        })

                        addView(TextView(this@PaymentHistoryActivity).apply {
                            text = formatStatus(status, type)
                            textSize = 12f
                            setTextColor(statusColor(status, type))
                            setTypeface(typeface, android.graphics.Typeface.BOLD)
                        })
                    })

                    // Ngày giờ
                    addView(TextView(this@PaymentHistoryActivity).apply {
                        text = if (createdAt > 0)
                            SimpleDateFormat("HH:mm  dd/MM/yyyy", Locale("vi", "VN")).format(Date(createdAt))
                        else ""
                        textSize = 11f
                        setTextColor(0xFF9E9E9E.toInt())
                        setPadding(0, dp(4), 0, 0)
                    })
                })
            })
        }
    }

    private fun formatStatus(status: String, type: String): String = when (status) {
        "paid" -> if (type == "featured") "Đã TT, chờ admin duyệt" else "Đã thanh toán"
        "paid_waiting_admin" -> "Đã TT, chờ admin duyệt"
        "approved" -> "Đã duyệt"
        "rejected" -> "Bị từ chối"
        "expired" -> "Hết hạn"
        "cancelled" -> "Đã hủy"
        "amount_mismatch" -> "Sai số tiền"
        "failed" -> "Thất bại"
        else -> "Chờ thanh toán"
    }

    private fun statusColor(status: String, type: String): Int = when (status) {
        "paid" -> if (type == "featured") 0xFF1976D2.toInt() else 0xFF2E7D32.toInt()
        "approved" -> 0xFF2E7D32.toInt()
        "paid_waiting_admin" -> 0xFF1976D2.toInt()
        "rejected", "expired", "cancelled", "failed", "amount_mismatch" -> 0xFFD32F2F.toInt()
        else -> 0xFFE65100.toInt()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
