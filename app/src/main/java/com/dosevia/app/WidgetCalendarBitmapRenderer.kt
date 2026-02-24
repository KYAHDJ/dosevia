package com.dosevia.app

import android.content.Context
import android.graphics.*
import java.text.DateFormatSymbols
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
            alpha = 130
        }
        val step = w * 0.12f
        var startX = -h.toFloat()
        while (startX < w + h) {
            canvas.drawLine(startX, 0f, startX + h, h.toFloat(), stripePaint)
            startX += step
        }

        val margin = w * 0.05f
        val topCardHeight = h * 0.22f
        val topCardRect = RectF(margin, margin, w - margin, margin + topCardHeight)
        val topCardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                topCardRect.left,
                topCardRect.top,
                topCardRect.left,
                topCardRect.bottom,
                intArrayOf(Color.parseColor("#E5E7EB"), Color.parseColor("#C6CBD4")),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(topCardRect, w * 0.045f, w * 0.045f, topCardPaint)
        val topCardBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = w * 0.006f
            color = Color.parseColor("#7A8392")
        }
        canvas.drawRoundRect(topCardRect, w * 0.045f, w * 0.045f, topCardBorder)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827")
            textSize = w * 0.082f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "DOSEVIA CALENDAR",
            w / 2f,
            topCardRect.top + topCardHeight * 0.48f,
            titlePaint
        )

        val cal = Calendar.getInstance().apply {
            set(year, month, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val monthLabel = "${DateFormatSymbols.getInstance().months[month]} $year"
        val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#374151")
            textSize = w * 0.043f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(monthLabel, w / 2f, topCardRect.bottom - topCardHeight * 0.14f, monthPaint)

        val cardTop = topCardRect.bottom + (h * 0.045f)
        val cardRect = RectF(margin, cardTop, w - margin, h - margin)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EFFFFFFF") }
        canvas.drawRoundRect(cardRect, w * 0.045f, w * 0.045f, cardPaint)

        val weekdays = listOf("S", "M", "T", "W", "T", "F", "S")
        val weekPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827")
            textSize = w * 0.029f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val contentPadding = w * 0.026f
        val gridLeft = cardRect.left + contentPadding
        val gridRight = cardRect.right - contentPadding
        val headerY = cardRect.top + contentPadding + weekPaint.textSize

        val colWidth = (gridRight - gridLeft) / 7f
        for (i in weekdays.indices) {
            val cx = gridLeft + i * colWidth + colWidth / 2f
            canvas.drawText(weekdays[i], cx, headerY, weekPaint)
        }

        val firstDayOffset = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
        val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val gridTop = headerY + h * 0.012f
        val usableHeight = (cardRect.bottom - contentPadding) - gridTop
        val rowHeight = usableHeight / 6f
        val pillRadius = (minOf(colWidth, rowHeight) * 0.45f)

        val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = pillRadius * 0.96f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        for (day in 1..totalDays) {
            val slot = firstDayOffset + (day - 1)
            val row = slot / 7
            val col = slot % 7

            val cx = gridLeft + col * colWidth + colWidth / 2f
            val cy = gridTop + row * rowHeight + rowHeight / 2f + pillRadius * 0.05f

            val status = dayStatus[day]
            val (outer, inner, text) = when (status) {
                PillStatus.MISSED -> Triple(Color.parseColor("#DC2626"), Color.parseColor("#FB7185"), Color.WHITE)
                PillStatus.TAKEN -> Triple(Color.parseColor("#111827"), Color.parseColor("#1F2937"), Color.parseColor("#9CA3AF"))
                PillStatus.NOT_TAKEN -> Triple(Color.parseColor("#BFC6D1"), Color.parseColor("#E5E7EB"), Color.parseColor("#111827"))
                null -> Triple(Color.parseColor("#D1D5DB"), Color.parseColor("#F9FAFB"), Color.parseColor("#6B7280"))
            }

            outerPaint.color = outer
            innerPaint.color = inner
            dayPaint.color = text

            canvas.drawCircle(cx, cy, pillRadius, outerPaint)
            canvas.drawCircle(cx, cy, pillRadius * 0.76f, innerPaint)

            val textY = cy - (dayPaint.ascent() + dayPaint.descent()) / 2f
            canvas.drawText(day.toString(), cx, textY, dayPaint)
        }

        return bitmap
    }
}
