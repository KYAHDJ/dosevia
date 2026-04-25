package com.dosevia.app

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds

class DoseviaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AccountStateRepository.getInstance(this)
        SyncStateRepository.getInstance(this)
        WidgetEntitlementManager.apply(this, PremiumAccess.readTier(this))
        CloudSyncManager(this).startAutomaticSync()
        AdFeaturePrefs.isTemporarilyAdFree(this)
        AchievementsManager.ensureChannel(this)
        try {
            MobileAds.initialize(this)
        } catch (t: Throwable) {
            Log.e("DoseviaAds", "Failed to initialize MobileAds", t)
        }
    }
}
