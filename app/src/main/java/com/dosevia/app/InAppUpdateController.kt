package com.dosevia.app

import android.app.Activity
import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

class InAppUpdateController(context: Context) {
    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(context.applicationContext)

    fun checkForAvailableUpdate(onResult: (Boolean) -> Unit) {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                onResult(info.isImmediateUpdateAllowed())
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    fun startImmediateUpdate(activity: Activity) {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                if (info.isImmediateUpdateAllowed()) {
                    try {
                        @Suppress("DEPRECATION")
                        appUpdateManager.startUpdateFlowForResult(
                            info,
                            AppUpdateType.IMMEDIATE,
                            activity,
                            REQUEST_CODE
                        )
                    } catch (_: Exception) {
                    }
                }
            }
            .addOnFailureListener {
            }
    }

    private fun AppUpdateInfo.isImmediateUpdateAllowed(): Boolean {
        return updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
            isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
    }

    private companion object {
        const val REQUEST_CODE = 2601
    }
}
