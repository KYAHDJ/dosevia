package com.dosevia.app

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

private const val TAG_SYNC = "CloudSync"

class CloudSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val syncManager = CloudSyncManager(applicationContext)
        return try {
            val uploaded = syncManager.uploadNow()
            if (uploaded) Result.success() else Result.success()
        } catch (e: DriveAppDataService.DriveAuthException) {
            Log.d(TAG_SYNC, "Upload failed with auth error")
            Result.failure()
        } catch (e: Exception) {
            Log.d(TAG_SYNC, "Upload failed with HTTP code in message: ${e.message}")
            Result.retry()
        }
    }
}
