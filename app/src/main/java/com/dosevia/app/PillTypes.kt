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

enum class PillStatus {
    NOT_TAKEN, TAKEN, MISSED
}

data class DayData(
    val day: Int,
    var status: PillStatus = PillStatus.NOT_TAKEN,
    val isPlacebo: Boolean = false,
    val isLowDose: Boolean = false,
    val date: Date,
    var takenAt: Long? = null
)

data class ReminderSettings(
    val appActive: Boolean = true,
    val dailyReminderTime: String = "9:00 PM"
)

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

fun getPillConfiguration(pillType: PillType, customConfig: CustomPillConfig? = null): PillConfiguration {
    return when (pillType) {
        PillType.TYPE_21_7 -> PillConfiguration("21+7", 21, 7, 0, 28)
        PillType.TYPE_24_4 -> PillConfiguration("24+4", 24, 4, 0, 28)
        PillType.TYPE_26_2 -> PillConfiguration("26+2", 26, 2, 0, 28)
        PillType.TYPE_28_DAY -> PillConfiguration("28-day continuous", 28, 0, 0, 28)
        PillType.TYPE_84_7 -> PillConfiguration("84+7 (91-day)", 84, 7, 0, 91)
        PillType.TYPE_84_7_LOW -> PillConfiguration("84+7 low-dose", 84, 0, 7, 91)
        PillType.TYPE_365_DAY -> PillConfiguration("365-day continuous", 365, 0, 0, 365)
        PillType.TYPE_28_POP -> PillConfiguration("28-day POP", 28, 0, 0, 28)
        PillType.FLEXIBLE -> PillConfiguration("Flexible", 84, 4, 0, 88)
        PillType.CUSTOM -> {
            if (customConfig != null) {
                val total = customConfig.active + customConfig.placebo + customConfig.lowDose
                PillConfiguration("Custom", customConfig.active, customConfig.placebo, customConfig.lowDose, total)
            } else {
                PillConfiguration("Custom", 21, 7, 0, 28)
            }
        }
    }
}

fun getPillTypeLabel(pillType: PillType): String {
    return when (pillType) {
        PillType.TYPE_21_7 -> "21 Active + 7 Placebo"
        PillType.TYPE_24_4 -> "24 Active + 4 Placebo"
        PillType.TYPE_26_2 -> "26 Active + 2 Placebo"
        PillType.TYPE_28_DAY -> "28-Day Continuous"
        PillType.TYPE_84_7 -> "84+7 (91-day)"
        PillType.TYPE_84_7_LOW -> "84+7 Low-dose"
        PillType.TYPE_365_DAY -> "365-Day Continuous"
        PillType.TYPE_28_POP -> "28-Day POP"
        PillType.FLEXIBLE -> "Flexible Extended"
        PillType.CUSTOM -> "Custom Configuration"
    }
}

data class PillTypeOption(
    val value: PillType,
    val label: String,
    val description: String,
    val category: String,
    val brands: String? = null
)

val pillTypeOptions = listOf(
    PillTypeOption(PillType.TYPE_21_7, "21 Active + 7 Placebo",
        "Most common traditional pack. Take hormone pills for 21 days, then placebo for 7 days. Period occurs during placebo week.",
        "Standard Cycle", "Ortho Tri-Cyclen, Apri, Yasmin"),
    PillTypeOption(PillType.TYPE_24_4, "24 Active + 4 Placebo",
        "Shorter placebo interval reduces withdrawal symptoms. Take active pills for 24 days, placebo for 4 days.",
        "Standard Cycle", "Yaz, Beyaz, Slynd"),
    PillTypeOption(PillType.TYPE_26_2, "26 Active + 2 Placebo",
        "Very short placebo break. Take active pills for 26 days, placebo for only 2 days. Minimal withdrawal bleeding.",
        "Standard Cycle"),
    PillTypeOption(PillType.TYPE_28_DAY, "28-Day Continuous",
        "All 28 pills are active. No placebo week, no periods. Continuous hormone protection.",
        "Standard Cycle", "Minipill variants"),
    PillTypeOption(PillType.TYPE_84_7, "84 Active + 7 Placebo (91-Day)",
        "3-month extended cycle. Take active pills for 84 days (12 weeks), then placebo for 7 days. Period only 4 times per year.",
        "Extended Cycle", "Seasonale, Jolessa, Quasense"),
    PillTypeOption(PillType.TYPE_84_7_LOW, "84 Active + 7 Low-Dose (91-Day)",
        "3-month cycle with low-dose estrogen instead of placebo. Better hormone stability, fewer withdrawal symptoms.",
        "Extended Cycle", "Seasonique, Camrese, LoSeasonique"),
    PillTypeOption(PillType.TYPE_365_DAY, "365-Day Continuous",
        "Year-round continuous active pills. No placebo, no periods. Take one active pill every day of the year.",
        "Extended Cycle", "Lybrel, Amethyst"),
    PillTypeOption(PillType.TYPE_28_POP, "28-Day Progestin-Only (Minipill)",
        "All 28 pills contain only progestin. Must take at same time daily (within 3-hour window). Safe for breastfeeding.",
        "Progestin-Only", "Nor-QD, Camila, Errin"),
    PillTypeOption(PillType.FLEXIBLE, "Flexible Extended Cycle",
        "You control cycle length. Take active pills for 24-120 days, then take 4-day break when ready. Reduces breakthrough bleeding.",
        "Flexible Regimen"),
    PillTypeOption(PillType.CUSTOM, "Custom Configuration",
        "For special regimens prescribed by your healthcare provider. Contact us for custom setup.",
        "Custom")
)
