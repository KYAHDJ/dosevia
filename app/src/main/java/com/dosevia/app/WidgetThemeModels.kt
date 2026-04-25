package com.dosevia.app

import android.content.Context
import android.graphics.Color
import com.google.gson.Gson

enum class WidgetKind { SMALL, MEDIUM, CALENDAR }

data class WidgetThemeColors(
    val background: Int,
    val accent1: Int,
    val accent2: Int,
    val textPrimary: Int,
    val textSecondary: Int
)

data class WidgetThemeSettings(
    val small: WidgetThemeColors,
    val medium: WidgetThemeColors,
    val calendar: WidgetThemeColors
)

fun defaultWidgetThemeSettings() = WidgetThemeSettings(
    small = WidgetThemeColors(
        background = Color.parseColor("#CC0F0F1A"),
        accent1 = Color.parseColor("#F609BC"),
        accent2 = Color.parseColor("#FAB86D"),
        textPrimary = Color.parseColor("#FFFFFF"),
        textSecondary = Color.parseColor("#88FFFFFF")
    ),
    medium = WidgetThemeColors(
        background = Color.parseColor("#F0EEFF"),
        accent1 = Color.parseColor("#22C55E"),
        accent2 = Color.parseColor("#EF4444"),
        textPrimary = Color.parseColor("#1A1A2E"),
        textSecondary = Color.parseColor("#666688")
    ),
    calendar = WidgetThemeColors(
        background = Color.parseColor("#EC2BAA"),
        accent1 = Color.parseColor("#C5CBD5"),
        accent2 = Color.parseColor("#F74CC0"),
        textPrimary = Color.parseColor("#111827"),
        textSecondary = Color.parseColor("#374151")
    )
)

fun WidgetThemeSettings.forKind(kind: WidgetKind): WidgetThemeColors = when (kind) {
    WidgetKind.SMALL -> small
    WidgetKind.MEDIUM -> medium
    WidgetKind.CALENDAR -> calendar
}

fun loadWidgetThemesFromPrefs(context: Context, gson: Gson = Gson()): WidgetThemeSettings {
    // FREE tier: no widget customization. Always return default themes.
    if (PremiumAccess.readTier(context) == UserTier.FREE) return defaultWidgetThemeSettings()

    val prefs = context.applicationContext.getSharedPreferences("dosevia_prefs", Context.MODE_PRIVATE)
    val raw = prefs.getString("widgetThemes", null) ?: return defaultWidgetThemeSettings()
    return try {
        gson.fromJson(raw, WidgetThemeSettings::class.java) ?: defaultWidgetThemeSettings()
    } catch (_: Exception) {
        defaultWidgetThemeSettings()
    }
}
