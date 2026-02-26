package com.dosevia.app

import android.app.Application

class DoseviaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        CloudSyncManager(this).startAutomaticSync()
    }
}
