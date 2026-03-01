package com.dosevia.app

import android.app.Application

class DoseviaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AccountStateRepository.getInstance(this)
        SyncStateRepository.getInstance(this)
        // Ensure widget picker only shows widgets allowed by the current plan.
        WidgetEntitlementManager.apply(this, PremiumAccess.readTier(this))
        CloudSyncManager(this).startAutomaticSync()
    }
}
