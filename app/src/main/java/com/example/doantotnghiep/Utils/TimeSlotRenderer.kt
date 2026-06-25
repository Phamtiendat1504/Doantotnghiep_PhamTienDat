package com.example.doantotnghiep.Utils

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView

object TimeSlotRenderer {

    // Danh sách tên đầy đủ các ngày trong tuần (bao gồm Chủ nhật)
    private val DAY_FULL_NAMES = listOf("Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật")

    fun render(container: LinearLayout, timeSlots: String) {
        container.removeAllViews()
        if (timeSlots.isBlank()) return
        val ctx = container.context
        when {
            timeSlots.trimStart().startsWith("Ngày:") -> renderStructured(container, ctx, timeSlots)
            isPerDayFormat(timeSlots) -> renderPerDay(container, ctx, timeSlots)
            else -> renderLegacy(container, ctx, timeSlots)
        }
    }

    /** Kiểm tra xem chuỗi có ở định dạng per-day mới không (VD: dòng đầu là "Thứ 2:") */
    private fun isPerDayFormat(text: String): Boolean {
        val firstLine = text.lines().firstOrNull()?.trim() ?: return false
        return DAY_FULL_NAMES.any { firstLine == "$it:" }
    }

    private fun dp(ctx: Context, dp: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, ctx.resources.displayMetrics).toInt()

    private fun sp(ctx: Context, sp: Float): Float = sp

    private fun renderStructured(container: LinearLayout, ctx: Context, text: String) {
        val lines = text.lines()
        var days = ""
        var morning = ""
        var noon = ""
        var evening = ""
        var note = ""

        for (line in lines) {
            when {
                line.startsWith("Ngày:") -> days = line.removePrefix("Ngày:").trim()
                line.startsWith("Buổi sáng:") -> morning = line.removePrefix("Buổi sáng:").trim()
                line.startsWith("Buổi trưa:") -> noon = line.removePrefix("Buổi trưa:").trim()
                line.startsWith("Buổi chiều/tối:") -> evening = line.removePrefix("Buổi chiều/tối:").trim()
                line.startsWith("Ghi chú:") -> note = line.removePrefix("Ghi chú:").trim()
            }
        }

        // Day chips row
        if (days.isNotEmpty()) {
            val scrollView = HorizontalScrollView(ctx).apply {
                isHorizontalScrollBarEnabled = false
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(ctx, 10f) }
            }
            val chipRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            days.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { day ->
                chipRow.addView(createDayChip(ctx, day))
            }
            scrollView.addView(chipRow)
            container.addView(scrollView)
        }

        // Period rows
        if (morning.isNotEmpty()) {
            container.addView(
                createPeriodRow(ctx, "Buổi sáng", morning,
                    rowBg = 0xFFFFFFFF.toInt(), accentColor = 0xFF1976D2.toInt(), initial = "S")
            )
        }
        if (noon.isNotEmpty()) {
            container.addView(
                createPeriodRow(ctx, "Buổi trưa", noon,
                    rowBg = 0xFFFFFFFF.toInt(), accentColor = 0xFF1976D2.toInt(), initial = "T")
            )
        }
        if (evening.isNotEmpty()) {
            container.addView(
                createPeriodRow(ctx, "Buổi chiều/tối", evening,
                    rowBg = 0xFFFFFFFF.toInt(), accentColor = 0xFF1976D2.toInt(), initial = "C")
            )
        }

        // Notes row
        if (note.isNotEmpty()) {
            container.addView(createNoteRow(ctx, note))
        }
    }

    /** Hiển thị định dạng per-day mới: mỗi ngày có dòng riêng "Thứ 2:\nBuổi sáng: 08:00-12:00\n..." */
    private fun renderPerDay(container: LinearLayout, ctx: Context, text: String) {
        val lines = text.lines()
        var currentDay = ""
        val dayOrder = mutableListOf<String>()  // Giữ thứ tự các ngày
        val dayPeriods = LinkedHashMap<String, MutableList<Pair<String, String>>>()  // day -> [(tên buổi, khoảng giờ)]
        var noteText = ""

        for (line in lines) {
            val trimmed = line.trim()
            val matchedDay = DAY_FULL_NAMES.firstOrNull { trimmed == "$it:" }
            when {
                matchedDay != null -> {
                    currentDay = matchedDay
                    if (!dayPeriods.containsKey(currentDay)) {
                        dayOrder.add(currentDay)
                        dayPeriods[currentDay] = mutableListOf()
                    }
                }
                trimmed.startsWith("Buổi sáng:") && currentDay.isNotEmpty() ->
                    dayPeriods[currentDay]?.add("Buổi sáng" to trimmed.removePrefix("Buổi sáng:").trim())
                trimmed.startsWith("Buổi trưa:") && currentDay.isNotEmpty() ->
                    dayPeriods[currentDay]?.add("Buổi trưa" to trimmed.removePrefix("Buổi trưa:").trim())
                trimmed.startsWith("Buổi chiều/tối:") && currentDay.isNotEmpty() ->
                    dayPeriods[currentDay]?.add("Buổi chiều/tối" to trimmed.removePrefix("Buổi chiều/tối:").trim())
                trimmed.startsWith("Ghi chú:") ->
                    noteText = trimmed.removePrefix("Ghi chú:").trim()
            }
        }

        if (dayPeriods.isEmpty()) { renderLegacy(container, ctx, text); return }

        // Hàng chip hiển thị tất cả các ngày
        val scrollView = HorizontalScrollView(ctx).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(ctx, 10f) }
        }
        val chipRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        dayOrder.forEach { day -> chipRow.addView(createDayChip(ctx, day)) }
        scrollView.addView(chipRow)
        container.addView(scrollView)

        // Hiển thị từng ngày và các buổi của ngày đó
        dayOrder.forEach { day ->
            val periods = dayPeriods[day] ?: return@forEach
            container.addView(createDayHeaderView(ctx, day))
            periods.forEach { (periodName, range) ->
                val (rowBg, accentColor, initial) = when (periodName) {
                    "Buổi sáng" -> Triple(0xFFFFFFFF.toInt(), 0xFF1976D2.toInt(), "S")
                    "Buổi trưa" -> Triple(0xFFFFFFFF.toInt(), 0xFF1976D2.toInt(), "T")
                    else         -> Triple(0xFFFFFFFF.toInt(), 0xFF1976D2.toInt(), "C")
                }
                container.addView(createPeriodRow(ctx, periodName, range, rowBg, accentColor, initial))
            }
        }

        if (noteText.isNotEmpty()) container.addView(createNoteRow(ctx, noteText))
    }

    /** Tạo TextView tiêu đề ngày (VD: "📅 Thứ 2 (T2)") */
    private fun createDayHeaderView(ctx: Context, dayName: String): TextView {
        val shortLabel = when (dayName) {
            "Thứ 2" -> "T2"; "Thứ 3" -> "T3"; "Thứ 4" -> "T4"
            "Thứ 5" -> "T5"; "Thứ 6" -> "T6"; "Thứ 7" -> "T7"
            "Chủ nhật" -> "CN"; else -> dayName
        }
        return TextView(ctx).apply {
            text = "📅 $dayName ($shortLabel)"
            setTextColor(android.graphics.Color.parseColor("#1A1A2E"))
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(ctx, 8f); bottomMargin = dp(ctx, 4f) }
        }
    }

    private fun createDayChip(ctx: Context, day: String): TextView {
        val label = when (day.trim()) {
            "Thứ 2" -> "T2"
            "Thứ 3" -> "T3"
            "Thứ 4" -> "T4"
            "Thứ 5" -> "T5"
            "Thứ 6" -> "T6"
            "Thứ 7" -> "T7"
            "Chủ nhật" -> "CN"
            else -> day.trim()
        }
        return TextView(ctx).apply {
            text = label
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            val hPad = dp(ctx, 10f)
            val vPad = dp(ctx, 5f)
            setPadding(hPad, vPad, hPad, vPad)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dp(ctx, 6f) }
            background = GradientDrawable().apply {
                setColor(0xFF1976D2.toInt())
                cornerRadius = dp(ctx, 14f).toFloat()
            }
        }
    }

    private fun createPeriodRow(
        ctx: Context,
        periodName: String,
        timeRange: String,
        rowBg: Int,
        accentColor: Int,
        initial: String
    ): LinearLayout {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val hPad = dp(ctx, 10f)
            val vPad = dp(ctx, 9f)
            setPadding(hPad, vPad, hPad, vPad)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(ctx, 6f) }
            background = GradientDrawable().apply {
                setColor(rowBg)
                cornerRadius = dp(ctx, 8f).toFloat()
                setStroke(dp(ctx, 1f), 0xFFE2E8F0.toInt())
            }
        }

        // Colored circle with initial letter
        val circleSize = dp(ctx, 30f)
        val iconView = TextView(ctx).apply {
            text = initial
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(circleSize, circleSize).apply {
                marginEnd = dp(ctx, 10f)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(accentColor)
            }
        }
        row.addView(iconView)

        // Period name
        val nameView = TextView(ctx).apply {
            text = periodName
            setTextColor(Color.parseColor("#1A1A2E"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        }
        row.addView(nameView)

        // Time range badge
        val timeView = TextView(ctx).apply {
            text = timeRange
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTypeface(null, Typeface.BOLD)
            val hPad = dp(ctx, 8f)
            val vPad = dp(ctx, 4f)
            setPadding(hPad, vPad, hPad, vPad)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            background = GradientDrawable().apply {
                setColor(accentColor)
                cornerRadius = dp(ctx, 10f).toFloat()
            }
        }
        row.addView(timeView)

        return row
    }

    private fun createNoteRow(ctx: Context, note: String): LinearLayout {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val hPad = dp(ctx, 10f)
            val vPad = dp(ctx, 8f)
            setPadding(hPad, vPad, hPad, vPad)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(ctx, 2f) }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F9FAFB"))
                cornerRadius = dp(ctx, 8f).toFloat()
                setStroke(dp(ctx, 1f), Color.parseColor("#E5E7EB"))
            }
        }

        val noteLabel = TextView(ctx).apply {
            text = "Ghi chú:"
            setTextColor(Color.parseColor("#6B7280"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dp(ctx, 6f) }
        }
        row.addView(noteLabel)

        val noteText = TextView(ctx).apply {
            text = note
            setTextColor(Color.parseColor("#6B7280"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(null, Typeface.ITALIC)
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        }
        row.addView(noteText)

        return row
    }

    private fun renderLegacy(container: LinearLayout, ctx: Context, text: String) {
        val tv = TextView(ctx).apply {
            this.text = text
            setTextColor(Color.parseColor("#1A1A2E"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(tv)
    }
}
