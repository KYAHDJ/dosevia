package com.dosevia.app

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleAuthManager(private val context: Context) {

    companion object {
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        private const val OAUTH_SCOPE = "oauth2:$DRIVE_APPDATA_SCOPE"
    }

    private val appContext = context.applicationContext
    private val accountPrefs = appContext.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)

    private val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope(DRIVE_APPDATA_SCOPE))
        .build()

    private val signInClient: GoogleSignInClient = GoogleSignIn.getClient(appContext, signInOptions)

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

    fun hasSignedInAccount(): Boolean = GoogleSignIn.getLastSignedInAccount(appContext)?.account != null

    fun getLastSignedInAccountUiState(): AccountUiState {
        val account = GoogleSignIn.getLastSignedInAccount(appContext)
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

    fun currentGoogleAccount(): Account? = GoogleSignIn.getLastSignedInAccount(appContext)?.account

    suspend fun getAccessToken(forceRefresh: Boolean = false): String? = withContext(Dispatchers.IO) {
        val account = currentGoogleAccount() ?: return@withContext null
        val token = accountPrefs.getString("access_token", null)
        if (forceRefresh && !token.isNullOrBlank()) {
            runCatching { GoogleAuthUtil.clearToken(appContext, token) }
        }

        return@withContext try {
            GoogleAuthUtil.getToken(appContext, account, OAUTH_SCOPE).also {
                accountPrefs.edit().putString("access_token", it).apply()
            }
        } catch (_: UserRecoverableAuthException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    fun clearToken(token: String?) {
        if (token.isNullOrBlank()) return
        runCatching { GoogleAuthUtil.clearToken(appContext, token) }
        accountPrefs.edit().remove("access_token").apply()
    }

    fun signOut(onDone: () -> Unit) {
        clearToken(accountPrefs.getString("access_token", null))
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
