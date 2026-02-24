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

        // Pink striped base background
        val bgRadius = w * 0.08f
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EF2CAE") }
        canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), bgRadius, bgRadius, bgPaint)

        val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F463C3")
            strokeWidth = w * 0.04f
            alpha = 120
        }
        val stripeStep = w * 0.12f
        var startX = -h.toFloat()
        while (startX <= w + h) {
            canvas.drawLine(startX, 0f, startX + h, h.toFloat(), stripePaint)
            startX += stripeStep
        }

        val margin = w * 0.05f

        // Top metallic header card
        val headerHeight = h * 0.22f
        val headerRect = RectF(margin, margin, w - margin, margin + headerHeight)
        val headerCorner = w * 0.04f

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                headerRect.left,
                headerRect.top,
                headerRect.left,
                headerRect.bottom,
                intArrayOf(
                    Color.parseColor("#D7DBE1"),
                    Color.parseColor("#C8CDD5"),
                    Color.parseColor("#B8BEC8")
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(headerRect, headerCorner, headerCorner, headerPaint)

        val headerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = w * 0.006f
            color = Color.parseColor("#6F7888")
        }
        canvas.drawRoundRect(headerRect, headerCorner, headerCorner, headerBorderPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#101827")
            textSize = w * 0.082f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("PILL PACK", w / 2f, headerRect.top + headerHeight * 0.47f, titlePaint)

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = w * 0.038f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("28 Day Calendar", w / 2f, headerRect.bottom - headerHeight * 0.2f, subtitlePaint)

        // Weekday row
        val weekdays = listOf("S", "M", "T", "W", "T", "F", "S")
        val weekdayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = w * 0.03f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val gridLeft = margin + w * 0.016f
        val gridRight = w - margin - w * 0.016f
        val weekY = headerRect.bottom + h * 0.09f
        val colWidth = (gridRight - gridLeft) / 7f

        weekdays.forEachIndexed { index, dayLabel ->
            val x = gridLeft + index * colWidth + colWidth / 2f
            canvas.drawText(dayLabel, x, weekY, weekdayPaint)
        }

        val cal = Calendar.getInstance().apply {
            set(year, month, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val firstDayOffset = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
        val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Day bubble grid
        val gridTop = weekY + h * 0.045f
        val gridBottom = h - margin - h * 0.02f
        val rowHeight = (gridBottom - gridTop) / 6f
        val pillRadius = minOf(colWidth, rowHeight) * 0.43f

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#33000000")
        }
        val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = pillRadius * 0.9f
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
            val palette = when {
                day == today && status != PillStatus.MISSED && status != PillStatus.TAKEN ->
                    arrayOf("#F59E0B", "#F8FAFC", "#DCE3EC", "#111827")
                status == PillStatus.MISSED ->
                    arrayOf("#D62828", "#FF6B6B", "#FF4D4D", "#FFFFFF")
                status == PillStatus.TAKEN ->
                    arrayOf("#4B5563", "#3B4555", "#1F2937", "#D1D5DB")
                else ->
                    arrayOf("#8E98A8", "#F7F9FC", "#D4DAE3", "#111827")
            }

            outerPaint.color = Color.parseColor(palette[0])
            innerPaint.shader = LinearGradient(
                cx,
                cy - pillRadius,
                cx,
                cy + pillRadius,
                intArrayOf(Color.parseColor(palette[1]), Color.parseColor(palette[2])),
                null,
                Shader.TileMode.CLAMP
            )
            textPaint.color = Color.parseColor(palette[3])

            canvas.drawCircle(cx, cy + pillRadius * 0.07f, pillRadius * 0.96f, shadowPaint)
            canvas.drawCircle(cx, cy, pillRadius, outerPaint)
            canvas.drawCircle(cx, cy, pillRadius * 0.78f, innerPaint)
            innerPaint.shader = null

            val textY = cy - (textPaint.ascent() + textPaint.descent()) / 2f
            canvas.drawText(day.toString(), cx, textY, textPaint)
        }

        return bitmap
    }
}
