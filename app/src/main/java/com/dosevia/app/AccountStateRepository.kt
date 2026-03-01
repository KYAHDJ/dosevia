package com.dosevia.app

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AccountStateRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val accountPrefs = appContext.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)

    private val _accountState = MutableStateFlow(readStateFromPrefs())
    val accountState: StateFlow<AccountUiState> = _accountState.asStateFlow()

    private val _showDrivePermissionRequiredDialog = MutableStateFlow(false)
    val showDrivePermissionRequiredDialog: StateFlow<Boolean> = _showDrivePermissionRequiredDialog.asStateFlow()

    fun setSignedIn(displayName: String?, email: String?, photoUrl: String?) {
        accountPrefs.edit()
            .putBoolean("is_signed_in", true)
            .putBoolean("sync_enabled", true)
            .putString("display_name", displayName)
            .putString("email", email)
            .putString("photo_url", photoUrl)
            .apply()

        _accountState.value = AccountUiState(
            isSignedIn = true,
            displayName = displayName,
            email = email,
            photoUrl = photoUrl,
            syncEnabled = true
        )
    }

    fun clearSignedInState() {
        accountPrefs.edit()
            .remove("is_signed_in")
            .remove("sync_enabled")
            .remove("display_name")
            .remove("email")
            .remove("photo_url")
            .remove("access_token")
            .apply()
        _accountState.value = AccountUiState()
    }

    fun promptDrivePermissionRequired() {
        _showDrivePermissionRequiredDialog.value = true
    }

    fun consumeDrivePermissionRequiredDialog() {
        _showDrivePermissionRequiredDialog.value = false
    }

    private fun readStateFromPrefs(): AccountUiState {
        return AccountUiState(
            isSignedIn = accountPrefs.getBoolean("is_signed_in", false),
            displayName = accountPrefs.getString("display_name", null),
            email = accountPrefs.getString("email", null),
            photoUrl = accountPrefs.getString("photo_url", null),
            syncEnabled = accountPrefs.getBoolean("sync_enabled", false)
        )
    }

    companion object {
        @Volatile private var instance: AccountStateRepository? = null

        fun getInstance(context: Context): AccountStateRepository {
            return instance ?: synchronized(this) {
                instance ?: AccountStateRepository(context).also { instance = it }
            }
        }
    }
}
