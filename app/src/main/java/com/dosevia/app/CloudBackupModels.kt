package com.dosevia.app

data class BackupPayload(
    val version: Int = 1,
    val lastModifiedEpochMs: Long,
    val deviceId: String,
    val prefs: Map<String, Map<String, Any?>>
)

data class AccountUiState(
    val isSignedIn: Boolean = false,
    val displayName: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,
    val syncEnabled: Boolean = false
)
