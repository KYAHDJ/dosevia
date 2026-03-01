package com.dosevia.app

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Enables/disables widget receivers so the widget picker only shows widgets
 * that the current plan includes.
 */
object WidgetEntitlementManager {

    fun apply(context: Context, tier: UserTier) {
        val pm = context.packageManager

        // Small widget is always available
        setEnabled(pm, context, PillWidget::class.java, true)

        val mediumEnabled = tier.hasPro()
        val calendarEnabled = tier.hasLifetime()

        setEnabled(pm, context, PillWidgetMedium::class.java, mediumEnabled)
        setEnabled(pm, context, PillWidgetCalendar::class.java, calendarEnabled)
    }

    private fun setEnabled(pm: PackageManager, context: Context, clazz: Class<*>, enabled: Boolean) {
        val cn = ComponentName(context, clazz)
        val newState = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        pm.setComponentEnabledSetting(cn, newState, PackageManager.DONT_KILL_APP)
    }
}
