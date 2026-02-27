package com.dosevia.app

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

private const val SYNC_PREFS_NAME = "cloud_sync_prefs"
private const val KEY_LOCAL_LAST_MODIFIED = "local_last_modified"
private const val TAG_SYNC = "CloudSync"

class CloudSyncManager(private val context: Context) {
    private val appContext = context.applicationContext
    private val authManager = GoogleAuthManager(appContext)
    private val driveService = DriveAppDataService(authManager)
    private val syncPrefs = appContext.getSharedPreferences(SYNC_PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val listeners = mutableListOf<Pair<SharedPreferences, SharedPreferences.OnSharedPreferenceChangeListener>>()

    fun startAutomaticSync() {
        if (listeners.isNotEmpty()) return
        SharedPrefsBackupSerializer.findAllPrefsNames(appContext).forEach { prefName ->
            val prefs = appContext.getSharedPreferences(prefName, Context.MODE_PRIVATE)
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                markLocalChanged()
                requestSyncDebounced()
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            listeners += prefs to listener
        }
    }

    fun markLocalChanged() {
        syncPrefs.edit().putLong(KEY_LOCAL_LAST_MODIFIED, System.currentTimeMillis()).apply()
    }

    fun unregisterPrefsListeners() {
        listeners.forEach { (prefs, listener) ->
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
        listeners.clear()
    }

    fun clearSyncMetadataAndStopWork() {
        unregisterPrefsListeners()
        syncPrefs.edit().clear().apply()
        WorkManager.getInstance(appContext).cancelUniqueWork("cloud_sync")
    }

    fun requestSyncDebounced(delayMs: Long = 5000L) {
        if (!authManager.hasSignedInAccount()) return
        val request = OneTimeWorkRequestBuilder<CloudSyncWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
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
                if (cloudPayload.lastModifiedEpochMs > localPayload.lastModifiedEpochMs) {
                    SharedPrefsBackupSerializer.restoreAllPrefs(appContext, cloudPayload)
                    syncPrefs.edit().putLong(KEY_LOCAL_LAST_MODIFIED, cloudPayload.lastModifiedEpochMs).apply()
                    onRestored()
                } else {
                    driveService.updateBackup(fileId, SharedPrefsBackupSerializer.toJson(localPayload))
                    syncPrefs.edit().putLong(KEY_LOCAL_LAST_MODIFIED, localPayload.lastModifiedEpochMs).apply()
                }
            } catch (e: Exception) {
                if (e is DriveAppDataService.DriveAuthException) {
                    authManager.signOutAndClearLocalState(promptDrivePermissionDialog = true)
                    clearSyncMetadataAndStopWork()
                    return@launch
                }
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

        try {
            val cloudPayload = SharedPrefsBackupSerializer.fromJson(driveService.downloadBackup(fileId))
            if (cloudPayload.lastModifiedEpochMs > localPayload.lastModifiedEpochMs) {
                SharedPrefsBackupSerializer.restoreAllPrefs(appContext, cloudPayload)
                syncPrefs.edit().putLong(KEY_LOCAL_LAST_MODIFIED, cloudPayload.lastModifiedEpochMs).apply()
                return
            }

            driveService.updateBackup(fileId, SharedPrefsBackupSerializer.toJson(localPayload))
            syncPrefs.edit().putLong(KEY_LOCAL_LAST_MODIFIED, localPayload.lastModifiedEpochMs).apply()
        } catch (e: DriveAppDataService.DriveAuthException) {
            authManager.signOutAndClearLocalState(promptDrivePermissionDialog = true)
            clearSyncMetadataAndStopWork()
            throw e
        }
    }
}
