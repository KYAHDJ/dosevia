package com.dosevia.app

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Google Cloud Console setup required:
 * 1) Enable Google Drive API.
 * 2) Create OAuth Android client with package + SHA-1 fingerprints (debug/release).
 * 3) Configure OAuth consent screen.
 */
class GoogleAuthManager(private val context: Context) {

    private val accountPrefs = context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)

    private val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestProfile()
        .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
        .build()

    private val signInClient: GoogleSignInClient = GoogleSignIn.getClient(context, signInOptions)

    fun getSignInIntent(): Intent = signInClient.signInIntent

    fun handleSignInResult(data: Intent?): Result<AccountUiState> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = Tasks.await(task)
            cacheAccount(account)
            Result.success(account.toUiState())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getLastSignedInAccountUiState(): AccountUiState {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return if (account != null) {
            cacheAccount(account)
            account.toUiState()
        } else {
            AccountUiState(
                isSignedIn = false,
                displayName = accountPrefs.getString("display_name", null),
                email = accountPrefs.getString("email", null),
                photoUrl = accountPrefs.getString("photo_url", null)
            )
        }
    }

    fun hasSignedInAccount(): Boolean = GoogleSignIn.getLastSignedInAccount(context) != null

    suspend fun createDriveCredential(): GoogleAccountCredential? = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext null
        GoogleAccountCredential.usingOAuth2(context, listOf(DriveScopes.DRIVE_APPDATA)).apply {
            selectedAccount = account.account
        }
    }

    fun signOut(onDone: () -> Unit) {
        signInClient.signOut().addOnCompleteListener {
            accountPrefs.edit().clear().apply()
            onDone()
        }
    }

    private fun cacheAccount(account: GoogleSignInAccount) {
        accountPrefs.edit()
            .putString("display_name", account.displayName)
            .putString("email", account.email)
            .putString("photo_url", account.photoUrl?.toString())
            .apply()
    }

    private fun GoogleSignInAccount.toUiState() = AccountUiState(
        isSignedIn = true,
        displayName = displayName,
        email = email,
        photoUrl = photoUrl?.toString()
    )
}
