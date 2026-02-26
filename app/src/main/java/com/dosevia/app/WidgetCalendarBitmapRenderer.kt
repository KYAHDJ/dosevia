package com.dosevia.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import java.util.Calendar
import java.util.Locale

object WidgetCalendarBitmapRenderer {

    private const val MIN_W_DP = 220
    private const val MIN_H_DP = 170
    private const val MAX_W_DP = 360
    private const val MAX_H_DP = 320

    private data class DayPalette(
        val outer: Int,
        val innerTop: Int,
        val innerBottom: Int,
        val text: Int
    )

    @Suppress("UNUSED_PARAMETER")
    fun render(
        context: Context,
        year: Int,
        month: Int,
        dayStatus: Map<Int, PillStatus?>,
        missedCount: Int,
        widthDp: Int,
        heightDp: Int,
        theme: WidgetThemeColors,
        todayDay: Int,
        todayIndicatorColor: Int,
        todayPulseScale: Float
    ): Bitmap {
        val dm = context.resources.displayMetrics
        val w = (widthDp.coerceIn(MIN_W_DP, MAX_W_DP) * dm.density).toInt()
        val h = (heightDp.coerceIn(MIN_H_DP, MAX_H_DP) * dm.density).toInt()

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val radius = w * 0.08f
        val outerRect = RectF(0f, 0f, w.toFloat(), h.toFloat())
        val clipPath = Path().apply { addRoundRect(outerRect, radius, radius, Path.Direction.CW) }

        canvas.save()
        canvas.clipPath(clipPath)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.background }
        canvas.drawRoundRect(outerRect, radius, radius, bgPaint)

        val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.accent2
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
                intArrayOf(theme.accent1, theme.accent1),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(headerRect, headerCorner, headerCorner, headerPaint)

        val headerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = w * 0.006f
            color = theme.textSecondary
        }
        canvas.drawRoundRect(headerRect, headerCorner, headerCorner, headerBorderPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.textPrimary
            textSize = w * 0.05f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Dosevia Calendar", w / 2f, headerRect.top + headerHeight * 0.56f, titlePaint)

        val monthCal = Calendar.getInstance().apply {
            set(year, month, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val monthName = monthCal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: ""
        val monthLabel = "$monthName $year"

        val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.textSecondary
            textSize = w * 0.033f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(monthLabel, w / 2f, headerRect.bottom - headerHeight * 0.16f, monthPaint)

        // Weekday row
        val weekdays = listOf("S", "M", "T", "W", "T", "F", "S")
        val weekPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.textPrimary
            textSize = w * 0.03f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val gridLeft = margin + w * 0.02f
        val gridRight = w - margin - w * 0.02f
        val headerY = headerRect.bottom + h * 0.085f
        val colWidth = (gridRight - gridLeft) / 7f

        val firstDayOffset = monthCal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
        val totalDays = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val gridTop = headerY + h * 0.04f
        val gridBottom = h - margin - h * 0.24f
        val rowHeight = (gridBottom - gridTop) / 6f
        val pillRadius = minOf(colWidth, rowHeight) * 0.45f

        val calendarPanelRect = RectF(
            margin + w * 0.008f,
            headerY - h * 0.055f,
            w - margin - w * 0.008f,
            gridBottom + h * 0.015f
        )
        val calendarPanelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F2FFFFFF")
        }
        val calendarPanelBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = w * 0.004f
            color = Color.parseColor("#AAB4C3")
        }
        val panelCorner = w * 0.04f
        canvas.drawRoundRect(calendarPanelRect, panelCorner, panelCorner, calendarPanelPaint)
        canvas.drawRoundRect(calendarPanelRect, panelCorner, panelCorner, calendarPanelBorder)

        for (i in weekdays.indices) {
            val cx = gridLeft + i * colWidth + colWidth / 2f
            canvas.drawText(weekdays[i], cx, headerY, weekPaint)
        }

        val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = pillRadius * 0.96f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        for (day in 1..totalDays) {
            val slot = firstDayOffset + (day - 1)
            val row = slot / 7
            val col = slot % 7

            val cx = gridLeft + col * colWidth + colWidth / 2f
            val cy = gridTop + row * rowHeight + rowHeight / 2f

            val palette = when (dayStatus[day]) {
                PillStatus.MISSED -> DayPalette(
                    outer = Color.parseColor("#DC2626"),
                    innerTop = Color.parseColor("#FB7185"),
                    innerBottom = Color.parseColor("#F43F5E"),
                    text = Color.WHITE
                )
                PillStatus.TAKEN -> DayPalette(
                    outer = theme.textSecondary,
                    innerTop = Color.parseColor("#4B5563"),
                    innerBottom = Color.parseColor("#1F2937"),
                    text = Color.parseColor("#D1D5DB")
                )
                else -> DayPalette(
                    outer = Color.parseColor("#8C95A5"),
                    innerTop = Color.parseColor("#F8FAFC"),
                    innerBottom = Color.parseColor("#D4DAE3"),
                    text = theme.textPrimary
                )
            }

            val dayOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.outer
            }
            val dayInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    cx,
                    cy - pillRadius,
                    cx,
                    cy + pillRadius,
                    intArrayOf(palette.innerTop, palette.innerBottom),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            dayPaint.color = palette.text


            if (day == todayDay) {
                val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = todayIndicatorColor
                    alpha = 90
                }
                canvas.drawCircle(cx, cy, pillRadius * 1.2f * todayPulseScale, pulsePaint)
            }

            canvas.drawCircle(cx, cy, pillRadius, dayOuterPaint)
            canvas.drawCircle(cx, cy, pillRadius * 0.78f, dayInnerPaint)

            val textY = cy - (dayPaint.ascent() + dayPaint.descent()) / 2f
            canvas.drawText(day.toString(), cx, textY, dayPaint)
        }

        canvas.restore()

        return bitmap
    }
}
