package com.dosevia.app

import java.util.Date

enum class PillType(val displayName: String) {
    TYPE_21_7("21+7"),
    TYPE_24_4("24+4"),
    TYPE_26_2("26+2"),
    TYPE_28_DAY("28-day"),
    TYPE_84_7("84+7"),
    TYPE_84_7_LOW("84+7-low"),
    TYPE_365_DAY("365-day"),
    TYPE_28_POP("28-pop"),
    FLEXIBLE("flexible"),
    CUSTOM("custom")
}

enum class PillStatus { NOT_TAKEN, TAKEN, MISSED }

data class DayData(
    val day: Int,
    var status: PillStatus = PillStatus.NOT_TAKEN,
    val isPlacebo: Boolean = false,
    val isLowDose: Boolean = false,
    val date: Date,
    var takenAt: Long? = null
)

data class Note(
    val id: String,
    val date: Date,
    val time: String,
    val content: String,
    val createdAt: Date,
    val updatedAt: Date
)

data class ReminderSettings(
    // Master switch — when OFF nothing fires at all
    val appActive: Boolean = true,
    // Time as "HH:mm" 24-hour stored internally, displayed as 12-hour
    val dailyReminderHour: Int = 9,
    val dailyReminderMinute: Int = 0,
    // Whether to send reminder on placebo days
    val placeboReminder: Boolean = true,
    // Vibration
    val vibrationEnabled: Boolean = true,
    // Notification text
    val notificationTitle: String = "Time for your daily check-in",
    val notificationSubtitle: String = "Don't forget your daily routine",
    // Alarm screen centre icon — one of: "medication", "favorite_heart", "alarm", "star", "notifications", "local_pharmacy", "health_and_safety", "ecg_heart", "healing", "medical_services"
    val notificationIcon: String = "favorite_heart",
    // Notification sound — "default" = system alarm, "silent" = no sound,
    //   or an absolute path inside the app's alarm_sounds/ folder
    val notificationSound: String = "default"
) {
    /** Formatted 12-hour display string, e.g. "9:00 AM" */
    val displayTime: String get() {
        val h12 = when {
            dailyReminderHour == 0  -> 12
            dailyReminderHour <= 12 -> dailyReminderHour
            else                    -> dailyReminderHour - 12
        }
        val ampm = if (dailyReminderHour < 12) "AM" else "PM"
        return "%d:%02d %s".format(h12, dailyReminderMinute, ampm)
    }
}

data class PillConfiguration(
    val name: String,
    val active: Int,
    val placebo: Int,
    val lowDose: Int,
    val total: Int
)

data class CustomPillConfig(
    val active: Int = 21,
    val placebo: Int = 7,
    val lowDose: Int = 0
)

fun getPillConfiguration(pillType: PillType, customConfig: CustomPillConfig? = null): PillConfiguration =
    when (pillType) {
        PillType.TYPE_21_7    -> PillConfiguration("21+7", 21, 7, 0, 28)
        PillType.TYPE_24_4    -> PillConfiguration("24+4", 24, 4, 0, 28)
        PillType.TYPE_26_2    -> PillConfiguration("26+2", 26, 2, 0, 28)
        PillType.TYPE_28_DAY  -> PillConfiguration("28-day continuous", 28, 0, 0, 28)
        PillType.TYPE_84_7    -> PillConfiguration("84+7 (91-day)", 84, 7, 0, 91)
        PillType.TYPE_84_7_LOW-> PillConfiguration("84+7 low-dose", 84, 0, 7, 91)
        PillType.TYPE_365_DAY -> PillConfiguration("365-day continuous", 365, 0, 0, 365)
        PillType.TYPE_28_POP  -> PillConfiguration("28-day POP", 28, 0, 0, 28)
        PillType.FLEXIBLE     -> PillConfiguration("Flexible", 84, 4, 0, 88)
        PillType.CUSTOM       -> {
            if (customConfig != null) {
                val total = customConfig.active + customConfig.placebo + customConfig.lowDose
                PillConfiguration("Custom", customConfig.active, customConfig.placebo, customConfig.lowDose, total)
            } else PillConfiguration("Custom", 21, 7, 0, 28)
        }
    }

fun getPillTypeLabel(pillType: PillType): String = when (pillType) {
    PillType.TYPE_21_7    -> "21 Active + 7 Pause"
    PillType.TYPE_24_4    -> "24 Active + 4 Pause"
    PillType.TYPE_26_2    -> "26 Active + 2 Pause"
    PillType.TYPE_28_DAY  -> "28-Day Continuous"
    PillType.TYPE_84_7    -> "84+7 (91-day)"
    PillType.TYPE_84_7_LOW-> "84+7 Low-dose"
    PillType.TYPE_365_DAY -> "365-Day Continuous"
    PillType.TYPE_28_POP  -> "28-Day POP"
    PillType.FLEXIBLE     -> "Flexible Extended"
    PillType.CUSTOM       -> "Custom Configuration"
}

data class PillTypeOption(
    val value: PillType,
    val label: String,
    val description: String,
    val category: String,
    val brands: String? = null
)

val pillTypeOptions = listOf(
    PillTypeOption(PillType.TYPE_21_7, "21 Active + 7 Pause",
        "Common 28-day cycle. Track 21 active days, then 7 pause days.",
        "Standard Cycle", "Ortho Tri-Cyclen, Apri, Yasmin"),
    PillTypeOption(PillType.TYPE_24_4, "24 Active + 4 Pause",
        "Shorter pause interval for a compact cycle.",
        "Standard Cycle", "Yaz, Beyaz, Slynd"),
    PillTypeOption(PillType.TYPE_26_2, "26 Active + 2 Pause",
        "Very short pause break for a compact cycle.",
        "Standard Cycle"),
    PillTypeOption(PillType.TYPE_28_DAY, "28-Day Continuous",
        "All 28 days are active. No pause week.",
        "Standard Cycle", "Daily routine variants"),
    PillTypeOption(PillType.TYPE_84_7, "84 Active + 7 Pause (91-Day)",
        "3-month extended cycle. Period only 4 times per year.",
        "Extended Cycle", "Seasonale, Jolessa, Quasense"),
    PillTypeOption(PillType.TYPE_84_7_LOW, "84 Active + 7 Low-Dose (91-Day)",
        "3-month cycle with light days instead of pause days.",
        "Extended Cycle", "Seasonique, Camrese, LoSeasonique"),
    PillTypeOption(PillType.TYPE_365_DAY, "365-Day Continuous",
        "Year-round continuous active days. No pause days.",
        "Extended Cycle", "Lybrel, Amethyst"),
    PillTypeOption(PillType.TYPE_28_POP, "28-Day Daily Routine",
        "All 28 days are active. Best used at the same time daily.",
        "Progestin-Only", "Nor-QD, Camila, Errin"),
    PillTypeOption(PillType.FLEXIBLE, "Flexible Extended Cycle",
        "You control cycle length. Track active days for 24–120 days, then a 4-day break.",
        "Flexible Regimen"),
    PillTypeOption(PillType.CUSTOM, "Custom Configuration",
        "For special regimens prescribed by your healthcare provider.",
        "Custom")
)
