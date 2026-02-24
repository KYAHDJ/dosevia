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

        val outerRadius = w * 0.08f
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EC2BAA") }
        canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), outerRadius, outerRadius, bgPaint)

        val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F74CC0")
            strokeWidth = w * 0.045f
            alpha = 130
        }
        val stripeStep = w * 0.125f
        var startX = -h.toFloat()
        while (startX < w + h) {
            canvas.drawLine(startX, 0f, startX + h, h.toFloat(), stripePaint)
            startX += stripeStep
        }

        val margin = w * 0.05f
        val headerHeight = h * 0.22f
        val headerRect = RectF(margin, margin, w - margin, margin + headerHeight)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                headerRect.left,
                headerRect.top,
                headerRect.left,
                headerRect.bottom,
                intArrayOf(Color.parseColor("#D4D8DE"), Color.parseColor("#B9BEC8")),
                null,
                Shader.TileMode.CLAMP
            )
        }
        val headerCorner = w * 0.035f
        canvas.drawRoundRect(headerRect, headerCorner, headerCorner, headerPaint)

        val headerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = w * 0.006f
            color = Color.parseColor("#778091")
        }
        canvas.drawRoundRect(headerRect, headerCorner, headerCorner, headerBorder)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827")
            textSize = w * 0.085f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("PILL PACK", w / 2f, headerRect.top + headerHeight * 0.46f, titlePaint)

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1F2937")
            textSize = w * 0.041f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("28 Day Calendar", w / 2f, headerRect.bottom - headerHeight * 0.2f, subtitlePaint)

        val weekdays = listOf("S", "M", "T", "W", "T", "F", "S")
        val weekdayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827")
            textSize = w * 0.034f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val gridLeft = margin + w * 0.018f
        val gridRight = w - margin - w * 0.018f
        val weekY = headerRect.bottom + h * 0.10f

        val colWidth = (gridRight - gridLeft) / 7f
        weekdays.forEachIndexed { index, label ->
            val x = gridLeft + index * colWidth + colWidth / 2f
            canvas.drawText(label, x, weekY, weekdayPaint)
        }

        val cal = Calendar.getInstance().apply {
            set(year, month, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val firstDayOffset = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
        val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val gridTop = weekY + h * 0.045f
        val gridBottom = h - margin - h * 0.02f
        val rowHeight = (gridBottom - gridTop) / 6f
        val pillRadius = minOf(colWidth, rowHeight) * 0.43f

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = pillRadius * 0.92f
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
            val cy = gridTop + row * rowHeight + rowHeight / 2f

            val status = dayStatus[day]
            val (outerColor, innerTop, innerBottom, textColor) = when (status) {
                PillStatus.MISSED -> arrayOf("#DC2626", "#FB7185", "#F97316", "#FFFFFF")
                PillStatus.TAKEN -> arrayOf("#334155", "#4B5563", "#374151", "#E5E7EB")
                PillStatus.NOT_TAKEN -> arrayOf("#8C95A5", "#F1F5F9", "#CBD5E1", "#111827")
                null -> arrayOf("#8C95A5", "#F1F5F9", "#CBD5E1", "#111827")
            }

            outerPaint.color = Color.parseColor(outerColor)
            innerPaint.shader = LinearGradient(
                cx,
                cy - pillRadius,
                cx,
                cy + pillRadius,
                intArrayOf(Color.parseColor(innerTop), Color.parseColor(innerBottom)),
                null,
                Shader.TileMode.CLAMP
            )
            textPaint.color = Color.parseColor(textColor)

            canvas.drawCircle(cx, cy, pillRadius, outerPaint)
            canvas.drawCircle(cx, cy, pillRadius * 0.78f, innerPaint)
            innerPaint.shader = null

            val textY = cy - (textPaint.ascent() + textPaint.descent()) / 2f
            canvas.drawText(day.toString(), cx, textY, textPaint)
        }

        return bitmap
    }
}
