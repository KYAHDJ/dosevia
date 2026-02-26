package com.dosevia.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class CloudSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val syncManager = CloudSyncManager(applicationContext)
        return try {
            syncManager.uploadNowWithConflictProtection()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
