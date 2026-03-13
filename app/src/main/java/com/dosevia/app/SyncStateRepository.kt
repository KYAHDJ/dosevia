package com.dosevia.app

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SyncStatus(val value: String) {
    NOT_SYNCED("NOT_SYNCED"),
    SUCCESS("SUCCESS"),
    NO_BACKUP("NO_BACKUP"),
    ERROR("ERROR");

    companion object {
        fun from(raw: String?): SyncStatus = entries.firstOrNull { it.value == raw } ?: NOT_SYNCED
    }
}

data class SyncState(
    val initialSyncCompleted: Boolean = false,
    val autoUploadEnabled: Boolean = false,
    val lastSyncStatus: SyncStatus = SyncStatus.NOT_SYNCED,
    val lastSyncTime: Long = 0L,
    val lastSyncError: String? = null
)

class SyncStateRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("sync_state_prefs", Context.MODE_PRIVATE)

    private val _syncState = MutableStateFlow(readState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _initialSyncCompleted = MutableStateFlow(_syncState.value.initialSyncCompleted)
    val initialSyncCompleted: StateFlow<Boolean> = _initialSyncCompleted.asStateFlow()

    fun updateAfterSuccess(nowMs: Long = System.currentTimeMillis()) {
        prefs.edit()
            .putBoolean(KEY_INITIAL_SYNC_COMPLETED, true)
            .putBoolean(KEY_AUTO_UPLOAD_ENABLED, true)
            .putString(KEY_LAST_SYNC_STATUS, SyncStatus.SUCCESS.value)
            .putLong(KEY_LAST_SYNC_TIME, nowMs)
            .remove(KEY_LAST_SYNC_ERROR)
            .apply()
        publish()
    }

    fun setNoBackupFound() {
        prefs.edit()
            .putString(KEY_LAST_SYNC_STATUS, SyncStatus.NO_BACKUP.value)
            .remove(KEY_LAST_SYNC_ERROR)
            .apply()
        publish()
    }

    fun setError(message: String?) {
        prefs.edit()
            .putString(KEY_LAST_SYNC_STATUS, SyncStatus.ERROR.value)
            .putString(KEY_LAST_SYNC_ERROR, message)
            .apply()
        publish()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
        publish()
    }

    fun completeInitialChoiceWithoutAutoUpload() {
        prefs.edit()
            .putBoolean(KEY_INITIAL_SYNC_COMPLETED, true)
            .putBoolean(KEY_AUTO_UPLOAD_ENABLED, false)
            .putString(KEY_LAST_SYNC_STATUS, SyncStatus.NOT_SYNCED.value)
            .remove(KEY_LAST_SYNC_ERROR)
            .apply()
        publish()
    }

    fun isAutoUploadEnabled(): Boolean = _syncState.value.autoUploadEnabled

    fun isInitialSyncCompleted(): Boolean = _initialSyncCompleted.value

    private fun publish() {
        val state = readState()
        _syncState.value = state
        _initialSyncCompleted.value = state.initialSyncCompleted
    }

    private fun readState(): SyncState {
        return SyncState(
            initialSyncCompleted = prefs.getBoolean(KEY_INITIAL_SYNC_COMPLETED, false),
            autoUploadEnabled = prefs.getBoolean(KEY_AUTO_UPLOAD_ENABLED, false),
            lastSyncStatus = SyncStatus.from(prefs.getString(KEY_LAST_SYNC_STATUS, null)),
            lastSyncTime = prefs.getLong(KEY_LAST_SYNC_TIME, 0L),
            lastSyncError = prefs.getString(KEY_LAST_SYNC_ERROR, null)
        )
    }

    companion object {
        private const val KEY_INITIAL_SYNC_COMPLETED = "initialSyncCompleted"
        private const val KEY_AUTO_UPLOAD_ENABLED = "autoUploadEnabled"
        private const val KEY_LAST_SYNC_STATUS = "lastSyncStatus"
        private const val KEY_LAST_SYNC_TIME = "lastSyncTime"
        private const val KEY_LAST_SYNC_ERROR = "lastSyncError"

        @Volatile
        private var instance: SyncStateRepository? = null

        fun getInstance(context: Context): SyncStateRepository {
            return instance ?: synchronized(this) {
                instance ?: SyncStateRepository(context).also { instance = it }
            }
        }
    }
}
