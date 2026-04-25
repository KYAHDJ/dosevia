package com.dosevia.app

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val ADS_TAG = "DoseviaAds"

object AdMobExtras {

    private val _fullscreenAdShowing = MutableStateFlow(false)
    val fullscreenAdShowing: StateFlow<Boolean> = _fullscreenAdShowing.asStateFlow()

    private val _fullscreenAdPending = MutableStateFlow(false)
    val fullscreenAdPending: StateFlow<Boolean> = _fullscreenAdPending.asStateFlow()

    private fun canUseActivity(activity: Activity): Boolean {
        return !activity.isFinishing && !activity.isDestroyed
    }

    fun isAnyFullscreenAdShowing(): Boolean = _fullscreenAdShowing.value || _fullscreenAdPending.value

    fun tryStartStandaloneFullscreenAd(): Boolean = tryBeginFullscreenAd()

    fun finishStandaloneFullscreenAd() {
        endFullscreenAd()
    }

    private fun tryBeginFullscreenAd(): Boolean {
        if (_fullscreenAdShowing.value || _fullscreenAdPending.value) return false
        _fullscreenAdPending.value = true
        return true
    }

    private fun markFullscreenAdVisible() {
        _fullscreenAdPending.value = false
        _fullscreenAdShowing.value = true
    }

    private fun endFullscreenAd() {
        _fullscreenAdPending.value = false
        _fullscreenAdShowing.value = false
    }

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAppOpen = false
    private var hasShownAppOpenThisLaunch = false
    private var appOpenLoadedAtMs: Long = 0L

    private const val APP_OPEN_MAX_AGE_MS = 4 * 60 * 60 * 1000L // 4 hours

    fun resetAppOpenForNewLaunch() {
        hasShownAppOpenThisLaunch = false
    }

    fun preloadAppOpen(
        context: Context,
        adUnitId: String,
    ) {
        if (adUnitId.isBlank()) return
        if (isLoadingAppOpen) return
        if (isAppOpenAvailable()) return

        isLoadingAppOpen = true

        AppOpenAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    appOpenLoadedAtMs = SystemClock.elapsedRealtime()
                    isLoadingAppOpen = false
                    Log.d(ADS_TAG, "App Open loaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    appOpenAd = null
                    appOpenLoadedAtMs = 0L
                    isLoadingAppOpen = false
                    Log.e(ADS_TAG, "App Open failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    fun clearAppOpenState() {
        appOpenAd = null
        appOpenLoadedAtMs = 0L
        isLoadingAppOpen = false
    }

    fun showAppOpenOncePerLaunchIfNeeded(
        activity: Activity,
        adUnitId: String,
        isFreeUser: Boolean,
        adFreeRewardActive: Boolean,
        billingReady: Boolean,
        onFinished: (() -> Unit)? = null,
    ) {
        if (hasShownAppOpenThisLaunch) {
            onFinished?.invoke()
            return
        }

        if (!billingReady) {
            onFinished?.invoke()
            return
        }

        if (!isFreeUser || adFreeRewardActive) {
            onFinished?.invoke()
            return
        }

        if (adUnitId.isBlank()) {
            onFinished?.invoke()
            return
        }

        if (!isAppOpenAvailable()) {
            preloadAppOpen(activity.applicationContext, adUnitId)
            onFinished?.invoke()
            return
        }

        if (!tryBeginFullscreenAd()) {
            onFinished?.invoke()
            return
        }

        val ad = appOpenAd ?: run {
            endFullscreenAd()
            onFinished?.invoke()
            return
        }

        hasShownAppOpenThisLaunch = true

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(ADS_TAG, "App Open dismissed")
                endFullscreenAd()
                appOpenAd = null
                appOpenLoadedAtMs = 0L
                preloadAppOpen(activity.applicationContext, adUnitId)
                onFinished?.invoke()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(ADS_TAG, "App Open failed to show: ${adError.message}")
                endFullscreenAd()
                appOpenAd = null
                appOpenLoadedAtMs = 0L
                preloadAppOpen(activity.applicationContext, adUnitId)
                onFinished?.invoke()
            }

            override fun onAdShowedFullScreenContent() {
                markFullscreenAdVisible()
                Log.d(ADS_TAG, "App Open showed")
            }
        }

        ad.show(activity)
    }

    fun showInterstitial(
        activity: Activity,
        adUnitId: String,
        onFinished: (() -> Unit)? = null,
    ) {
        if (adUnitId.isBlank()) {
            onFinished?.invoke()
            return
        }

        if (!canUseActivity(activity)) {
            onFinished?.invoke()
            return
        }

        if (!tryBeginFullscreenAd()) {
            onFinished?.invoke()
            return
        }

        InterstitialAd.load(
            activity,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            endFullscreenAd()
                            onFinished?.invoke()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e(ADS_TAG, "Interstitial failed to show: ${adError.message}")
                            endFullscreenAd()
                            onFinished?.invoke()
                        }

                        override fun onAdShowedFullScreenContent() {
                            markFullscreenAdVisible()
                            Log.d(ADS_TAG, "Interstitial showed")
                        }
                    }
                    if (canUseActivity(activity)) {
                        ad.show(activity)
                    } else {
                        endFullscreenAd()
                        onFinished?.invoke()
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(ADS_TAG, "Interstitial failed to load: ${loadAdError.message}")
                    endFullscreenAd()
                    onFinished?.invoke()
                }
            }
        )
    }

    fun showRewarded(
        activity: Activity,
        adUnitId: String,
        onRewardEarned: () -> Unit,
        onFinished: (() -> Unit)? = null,
    ) {
        if (adUnitId.isBlank()) {
            onFinished?.invoke()
            return
        }

        if (!canUseActivity(activity)) {
            onFinished?.invoke()
            return
        }

        if (!tryBeginFullscreenAd()) {
            onFinished?.invoke()
            return
        }

        RewardedAd.load(
            activity,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            endFullscreenAd()
                            onFinished?.invoke()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e(ADS_TAG, "Rewarded failed to show: ${adError.message}")
                            endFullscreenAd()
                            onFinished?.invoke()
                        }

                        override fun onAdShowedFullScreenContent() {
                            markFullscreenAdVisible()
                            Log.d(ADS_TAG, "Rewarded showed")
                        }
                    }

                    if (canUseActivity(activity)) {
                        ad.show(activity) { _: RewardItem ->
                            onRewardEarned()
                        }
                    } else {
                        endFullscreenAd()
                        onFinished?.invoke()
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(ADS_TAG, "Rewarded failed to load: ${loadAdError.message}")
                    endFullscreenAd()
                    onFinished?.invoke()
                }
            }
        )
    }

    private fun isAppOpenAvailable(): Boolean {
        appOpenAd ?: return false
        val age = SystemClock.elapsedRealtime() - appOpenLoadedAtMs
        return age < APP_OPEN_MAX_AGE_MS
    }
}
