package com.dosevia.app

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val SYNC_PREFS_NAME = "cloud_sync_prefs"
private const val KEY_LOCAL_LAST_MODIFIED = "local_last_modified"
private const val TAG_SYNC = "CloudSync"

class CloudSyncManager(private val context: Context) {
    private val appContext = context.applicationContext
    private val authManager = GoogleAuthManager(appContext)
    private val driveService = DriveAppDataService(authManager)
    private val syncPrefs = appContext.getSharedPreferences(SYNC_PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun markLocalChanged() {
        syncPrefs.edit().putLong(KEY_LOCAL_LAST_MODIFIED, System.currentTimeMillis()).apply()
    }

    fun requestSyncDebounced(delayMs: Long = 5000L) {
        if (!authManager.hasSignedInAccount()) return
        val request = OneTimeWorkRequestBuilder<CloudSyncWorker>()
            .setInitialDelay(delayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, java.util.concurrent.TimeUnit.SECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(appContext).enqueueUniqueWork(
            "cloud_sync",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun runInitialSync(onRestored: () -> Unit = {}) {
        if (!authManager.hasSignedInAccount()) return
        scope.launch {
            try {
                val localPayload = SharedPrefsBackupSerializer.exportAllPrefs(appContext)
                val fileId = driveService.findBackupFileId()
                if (fileId == null) {
                    driveService.createBackup(SharedPrefsBackupSerializer.toJson(localPayload))
                    syncPrefs.edit().putLong(KEY_LOCAL_LAST_MODIFIED, localPayload.lastModifiedEpochMs).apply()
                    return@launch
                }

                val cloudPayload = SharedPrefsBackupSerializer.fromJson(driveService.downloadBackup(fileId))
                val localTs = syncPrefs.getLong(KEY_LOCAL_LAST_MODIFIED, localPayload.lastModifiedEpochMs)
                if (cloudPayload.lastModifiedEpochMs > localTs) {
                    SharedPrefsBackupSerializer.restoreAllPrefs(appContext, cloudPayload)
                    syncPrefs.edit().putLong(KEY_LOCAL_LAST_MODIFIED, cloudPayload.lastModifiedEpochMs).apply()
                    onRestored()
                } else {
                    driveService.updateBackup(fileId, SharedPrefsBackupSerializer.toJson(localPayload))
                    syncPrefs.edit().putLong(KEY_LOCAL_LAST_MODIFIED, localPayload.lastModifiedEpochMs).apply()
                }
            } catch (e: Exception) {
                Log.w(TAG_SYNC, "Initial sync failed", e)
                requestSyncDebounced()
            }
        }
    }

    suspend fun uploadNowWithConflictProtection() {
        val localPayload = SharedPrefsBackupSerializer.exportAllPrefs(appContext)
        val fileId = driveService.findBackupFileId()
        if (fileId == null) {
            driveService.createBackup(SharedPrefsBackupSerializer.toJson(localPayload))
            syncPrefs.edit().putLong(KEY_LOCAL_LAST_MODIFIED, localPayload.lastModifiedEpochMs).apply()
            return
        }

        val cloudPayload = SharedPrefsBackupSerializer.fromJson(driveService.downloadBackup(fileId))
        if (cloudPayload.lastModifiedEpochMs > localPayload.lastModifiedEpochMs) {
            SharedPrefsBackupSerializer.restoreAllPrefs(appContext, cloudPayload)
            syncPrefs.edit().putLong(KEY_LOCAL_LAST_MODIFIED, cloudPayload.lastModifiedEpochMs).apply()
            requestSyncDebounced()
            return
        }

        driveService.updateBackup(fileId, SharedPrefsBackupSerializer.toJson(localPayload))
        syncPrefs.edit().putLong(KEY_LOCAL_LAST_MODIFIED, localPayload.lastModifiedEpochMs).apply()
    }
}

