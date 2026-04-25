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
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

private const val TAG_SYNC = "CloudSync"
private const val UNIQUE_WORK_NAME = "cloud_sync"

enum class SyncNowResult {
    RESTORED,
    NO_BACKUP_FOUND,
    BACKUP_CREATED,
    ERROR,
    NOT_SIGNED_IN
}

class CloudSyncManager(private val context: Context) {
    private val appContext = context.applicationContext
    private val authManager = GoogleAuthManager(appContext)
    private val driveService = DriveAppDataService(authManager)
    private val syncStateRepository = SyncStateRepository.getInstance(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val listeners = mutableListOf<Pair<SharedPreferences, SharedPreferences.OnSharedPreferenceChangeListener>>()

    fun startAutomaticSync() {
        if (!syncStateRepository.isInitialSyncCompleted()) {
            Log.d(TAG_SYNC, "Skipping automatic sync registration: initial sync not completed")
            return
        }
        if (!syncStateRepository.isAutoUploadEnabled()) {
            Log.d(TAG_SYNC, "Skipping automatic sync registration: auto upload not enabled")
            return
        }
        if (listeners.isNotEmpty()) return

        SharedPrefsBackupSerializer.findAllPrefsNames(appContext).forEach { prefName ->
            val prefs = appContext.getSharedPreferences(prefName, Context.MODE_PRIVATE)
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                requestSyncDebounced()
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            listeners += prefs to listener
        }
        Log.d(TAG_SYNC, "Automatic sync listeners registered for ${listeners.size} prefs")
    }

    fun unregisterPrefsListeners() {
        listeners.forEach { (prefs, listener) ->
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
        listeners.clear()
    }

    fun clearSyncMetadataAndStopWork() {
        unregisterPrefsListeners()
        syncStateRepository.clearAll()
        WorkManager.getInstance(appContext).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    fun requestSyncDebounced(delayMs: Long = 5000L) {
        if (!authManager.hasSignedInAccount()) return
        if (!syncStateRepository.isInitialSyncCompleted()) {
            Log.d(TAG_SYNC, "Skipping schedule: initial sync not completed")
            return
        }
        if (!syncStateRepository.isAutoUploadEnabled()) {
            Log.d(TAG_SYNC, "Skipping schedule: auto upload not enabled")
            return
        }

        val request = OneTimeWorkRequestBuilder<CloudSyncWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .build()

        WorkManager.getInstance(appContext).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
        Log.d(TAG_SYNC, "Scheduled cloud sync worker with delay=${delayMs}ms")
    }

    fun checkCloudBackupExists(onComplete: (exists: Boolean, error: Throwable?) -> Unit) {
        if (!authManager.hasSignedInAccount()) {
            onComplete(false, IllegalStateException("Not signed in"))
            return
        }

        scope.launch {
            try {
                val fileId = driveService.findBackupFileId()
                withContext(Dispatchers.Main) { onComplete(fileId != null, null) }
            } catch (e: Exception) {
                if (e is DriveAppDataService.DriveAuthException) {
                    authManager.signOutAndClearLocalState(promptDrivePermissionDialog = true)
                    clearSyncMetadataAndStopWork()
                }
                withContext(Dispatchers.Main) { onComplete(false, e) }
            }
        }
    }

    fun restoreFromCloudNow(onComplete: (SyncNowResult) -> Unit) {
        if (!authManager.hasSignedInAccount()) {
            onComplete(SyncNowResult.NOT_SIGNED_IN)
            return
        }

        scope.launch {
            Log.d(TAG_SYNC, "RestoreFromCloud started")
            try {
                val fileId = driveService.findBackupFileId()
                if (fileId == null) {
                    syncStateRepository.setNoBackupFound()
                    completeOnMain(onComplete, SyncNowResult.NO_BACKUP_FOUND)
                    return@launch
                }

                val cloudJson = driveService.downloadBackup(fileId)
                val payload = SharedPrefsBackupSerializer.fromJson(cloudJson)
                SharedPrefsBackupSerializer.restoreAllPrefs(appContext, payload)
                syncStateRepository.updateAfterSuccess()
                startAutomaticSync()
                completeOnMain(onComplete, SyncNowResult.RESTORED)
            } catch (e: Exception) {
                if (e is DriveAppDataService.DriveAuthException) {
                    authManager.signOutAndClearLocalState(promptDrivePermissionDialog = true)
                    clearSyncMetadataAndStopWork()
                }
                syncStateRepository.setError(e.message)
                completeOnMain(onComplete, SyncNowResult.ERROR)
            }
        }
    }

    fun syncNow(onComplete: (SyncNowResult) -> Unit) {
        restoreFromCloudNow(onComplete)
    }

    fun createBackupNow(onComplete: (SyncNowResult) -> Unit) {
        if (!authManager.hasSignedInAccount()) {
            onComplete(SyncNowResult.NOT_SIGNED_IN)
            return
        }

        scope.launch {
            try {
                val localPayload = SharedPrefsBackupSerializer.exportAllPrefs(appContext)
                val localJson = SharedPrefsBackupSerializer.toJson(localPayload)
                val fileId = driveService.findBackupFileId()
                if (fileId == null) {
                    val httpCode = driveService.createBackup(localJson)
                    Log.d(TAG_SYNC, "Upload success with HTTP code=$httpCode")
                } else {
                    val httpCode = driveService.updateBackup(fileId, localJson)
                    Log.d(TAG_SYNC, "Upload success with HTTP code=$httpCode")
                }
                syncStateRepository.updateAfterSuccess()
                startAutomaticSync()
                completeOnMain(onComplete, SyncNowResult.BACKUP_CREATED)
            } catch (e: Exception) {
                val message = e.message ?: "Unknown upload error"
                Log.d(TAG_SYNC, "Upload failed", e)
                syncStateRepository.setError(message)
                completeOnMain(onComplete, SyncNowResult.ERROR)
            }
        }
    }

    private suspend fun completeOnMain(onComplete: (SyncNowResult) -> Unit, result: SyncNowResult) {
        withContext(Dispatchers.Main) { onComplete(result) }
    }

    suspend fun uploadNow(): Boolean {
        if (!syncStateRepository.isInitialSyncCompleted()) {
            Log.d(TAG_SYNC, "Worker upload skipped: initial sync not completed")
            return false
        }
        if (!syncStateRepository.isAutoUploadEnabled()) {
            Log.d(TAG_SYNC, "Worker upload skipped: auto upload not enabled")
            return false
        }

        val localPayload = SharedPrefsBackupSerializer.exportAllPrefs(appContext)
        val localJson = SharedPrefsBackupSerializer.toJson(localPayload)
        val fileId = driveService.findBackupFileId()
        val httpCode = if (fileId == null) {
            driveService.createBackup(localJson)
        } else {
            driveService.updateBackup(fileId, localJson)
        }
        Log.d(TAG_SYNC, "Upload success with HTTP code=$httpCode")
        syncStateRepository.updateAfterSuccess()
        return true
    }
}
