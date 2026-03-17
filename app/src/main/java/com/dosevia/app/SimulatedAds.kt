package com.dosevia.app

import android.app.Activity
import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd

@Composable
fun SimulatedBannerAd(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val adUnitId = stringResource(R.string.admob_banner_unit_id)

    if (adUnitId.isBlank()) return

    val adWidthDp = remember(configuration.screenWidthDp) {
        configuration.screenWidthDp.coerceAtLeast(320)
    }
    val minHeightDp = remember(adWidthDp, context) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidthDp).getHeightInPixels(context)
    }

    val adView = remember(context, adUnitId, adWidthDp) {
        AdView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            this.adUnitId = adUnitId
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidthDp))
            loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(adView) {
        onDispose { adView.destroy() }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = with(density) { minHeightDp.toDp() })
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { adView }
        )
    }
}

@Composable
fun SimulatedAppOpenAd(
    minSeconds: Int = 5,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val adUnitId = stringResource(R.string.admob_app_open_unit_id)

    LaunchedEffect(activity, adUnitId) {
        if (activity == null || adUnitId.isBlank() || AdMobExtras.isAnyFullscreenAdShowing()) {
            onClose()
            return@LaunchedEffect
        }

        delay((minSeconds.coerceAtLeast(2) * 1000L) / 2L)
        AppOpenAd.load(
            activity,
            adUnitId,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(appOpenAd: AppOpenAd) {
                    if (!AdMobExtras.tryStartStandaloneFullscreenAd()) {
                        onClose()
                        return
                    }

                    appOpenAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            Log.d("DoseviaAds", "App open ad dismissed")
                            AdMobExtras.finishStandaloneFullscreenAd()
                            onClose()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e("DoseviaAds", "App open ad failed to show: ${adError.message}")
                            AdMobExtras.finishStandaloneFullscreenAd()
                            onClose()
                        }

                        override fun onAdShowedFullScreenContent() {
                            Log.d("DoseviaAds", "App open ad showed")
                        }
                    }
                    activity.window.decorView.postDelayed({
                        try {
                            appOpenAd.show(activity)
                        } catch (_: Exception) {
                            AdMobExtras.finishStandaloneFullscreenAd()
                            onClose()
                        }
                    }, 1200L)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e("DoseviaAds", "App open ad failed to load: ${loadAdError.message}")
                    onClose()
                }
            }
        )
    }
}
