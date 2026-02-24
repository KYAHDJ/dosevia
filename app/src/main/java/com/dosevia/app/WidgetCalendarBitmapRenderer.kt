package com.dosevia.app

import android.content.Context
import android.graphics.*
import java.util.*

object WidgetCalendarBitmapRenderer {

    private const val MIN_W_DP = 220
    private const val MIN_H_DP = 170
    private const val MAX_W_DP = 360
    private const val MAX_H_DP = 320

    @Suppress("UNUSED_PARAMETER")
    fun render(
        context: Context,
        year: Int,
        month: Int,
        dayStatus: Map<Int, PillStatus?>,
        missedCount: Int,
        widthDp: Int,
        heightDp: Int
    ): Bitmap {
        val dm = context.resources.displayMetrics
        val w = (widthDp.coerceIn(MIN_W_DP, MAX_W_DP) * dm.density).toInt()
        val h = (heightDp.coerceIn(MIN_H_DP, MAX_H_DP) * dm.density).toInt()

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EC2BAA") }
        val radius = w * 0.08f
        canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), radius, radius, bgPaint)

        val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F74CC0")
            strokeWidth = w * 0.04f
            alpha = 120
        }
        val stripeStep = w * 0.12f
        var startX = -h.toFloat()
        while (startX < w + h) {
            canvas.drawLine(startX, 0f, startX + h, h.toFloat(), stripePaint)
            startX += stripeStep
        }

        val margin = w * 0.04f
        val headerHeight = h * 0.16f
        val headerRect = RectF(margin, margin, w - margin, margin + headerHeight)
        val headerCorner = w * 0.04f

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                headerRect.left,
                headerRect.top,
                headerRect.left,
                headerRect.bottom,
                intArrayOf(Color.parseColor("#E2E6EC"), Color.parseColor("#C5CBD5")),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(headerRect, headerCorner, headerCorner, headerPaint)

        val headerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = w * 0.006f
            color = Color.parseColor("#768194")
        }
        canvas.drawRoundRect(headerRect, headerCorner, headerCorner, headerBorder)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827")
            textSize = w * 0.05f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Dosevia Calendar", w / 2f, headerRect.top + headerHeight * 0.56f, titlePaint)

        val cal = Calendar.getInstance().apply {
            set(year, month, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val monthLabel = "${DateFormatSymbols.getInstance().months[month]} $year"
        val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#374151")
            textSize = w * 0.033f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(monthLabel, w / 2f, headerRect.bottom - headerHeight * 0.16f, monthPaint)

        // Weekday row
        val weekdays = listOf("S", "M", "T", "W", "T", "F", "S")
        val weekPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827")
            textSize = w * 0.03f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val gridLeft = margin + w * 0.02f
        val gridRight = w - margin - w * 0.02f
        val headerY = headerRect.bottom + h * 0.085f
        val colWidth = (gridRight - gridLeft) / 7f

        for (i in weekdays.indices) {
            val cx = gridLeft + i * colWidth + colWidth / 2f
            canvas.drawText(weekdays[i], cx, headerY, weekPaint)
        }

        val cal = Calendar.getInstance().apply {
            set(year, month, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val firstDayOffset = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
        val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val gridTop = headerY + h * 0.04f
        val gridBottom = h - margin - h * 0.02f
        val rowHeight = (gridBottom - gridTop) / 6f
        val pillRadius = minOf(colWidth, rowHeight) * 0.45f

        val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = pillRadius * 0.96f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val now = Calendar.getInstance()
        val isCurrentMonth = now.get(Calendar.YEAR) == year && now.get(Calendar.MONTH) == month
        val today = if (isCurrentMonth) now.get(Calendar.DAY_OF_MONTH) else -1

        for (day in 1..totalDays) {
            val slot = firstDayOffset + (day - 1)
            val row = slot / 7
            val col = slot % 7

            val cx = gridLeft + col * colWidth + colWidth / 2f
            val cy = gridTop + row * rowHeight + rowHeight / 2f

            val status = dayStatus[day]
            val (outer, innerTop, innerBottom, text) = when (status) {
                PillStatus.MISSED -> arrayOf("#DC2626", "#FB7185", "#F43F5E", "#FFFFFF")
                PillStatus.TAKEN -> arrayOf("#374151", "#4B5563", "#1F2937", "#D1D5DB")
                PillStatus.NOT_TAKEN -> arrayOf("#8C95A5", "#F8FAFC", "#D4DAE3", "#111827")
                null -> arrayOf("#8C95A5", "#F8FAFC", "#D4DAE3", "#111827")
            }

            outerPaint.color = Color.parseColor(outer)
            innerPaint.shader = LinearGradient(
                cx,
                cy - pillRadius,
                cx,
                cy + pillRadius,
                intArrayOf(Color.parseColor(innerTop), Color.parseColor(innerBottom)),
                null,
                Shader.TileMode.CLAMP
            )
            dayPaint.color = Color.parseColor(text)

            canvas.drawCircle(cx, cy + pillRadius * 0.07f, pillRadius * 0.96f, shadowPaint)
            canvas.drawCircle(cx, cy, pillRadius, outerPaint)
            canvas.drawCircle(cx, cy, pillRadius * 0.78f, innerPaint)
            innerPaint.shader = null

            val textY = cy - (dayPaint.ascent() + dayPaint.descent()) / 2f
            canvas.drawText(day.toString(), cx, textY, dayPaint)
        }

        return bitmap
    }
}
