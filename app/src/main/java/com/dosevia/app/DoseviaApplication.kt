package com.dosevia.app

import android.app.Application

class DoseviaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AccountStateRepository.getInstance(this)
        SyncStateRepository.getInstance(this)
        CloudSyncManager(this).startAutomaticSync()
    }
}
