package com.magicgrid.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.magicgrid.R

/**
 * Centralized ad loading/showing for banner, interstitial, and rewarded placements.
 */
object AdController {
    private var initialized = false
    private var launchInterstitialShown = false

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    private var isLoadingInterstitial = false
    private var isLoadingRewarded = false

    fun initialize(context: Context) {
        if (initialized) return

        MobileAds.initialize(context)
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                .build()
        )

        initialized = true
    }

    fun loadBanner(adView: AdView) {
        initialize(adView.context.applicationContext)
        adView.loadAd(AdRequest.Builder().build())
    }

    fun preloadInterstitial(context: Context) {
        initialize(context.applicationContext)
        if (interstitialAd != null || isLoadingInterstitial) return

        isLoadingInterstitial = true
        InterstitialAd.load(
            context,
            context.getString(R.string.admob_interstitial_unit_id),
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoadingInterstitial = false
                }

                override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                    interstitialAd = null
                    isLoadingInterstitial = false
                }
            }
        )
    }

    fun preloadRewarded(context: Context) {
        initialize(context.applicationContext)
        if (rewardedAd != null || isLoadingRewarded) return

        isLoadingRewarded = true
        RewardedAd.load(
            context,
            context.getString(R.string.admob_rewarded_unit_id),
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoadingRewarded = false
                }

                override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                    rewardedAd = null
                    isLoadingRewarded = false
                }
            }
        )
    }

    fun showLaunchInterstitialIfNeeded(activity: Activity, onFinished: () -> Unit = {}): Boolean {
        if (launchInterstitialShown) {
            onFinished()
            return false
        }

        launchInterstitialShown = true
        return showInterstitial(activity, onFinished)
    }

    fun showInterstitial(activity: Activity, onFinished: () -> Unit = {}): Boolean {
        val ad = interstitialAd
        if (ad == null) {
            preloadInterstitial(activity.applicationContext)
            onFinished()
            return false
        }

        interstitialAd = null
        var finished = false

        fun finishOnce() {
            if (finished) return
            finished = true
            onFinished()
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                preloadInterstitial(activity.applicationContext)
                finishOnce()
            }

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                preloadInterstitial(activity.applicationContext)
                finishOnce()
            }
        }

        ad.show(activity)
        return true
    }

    fun showRewardedVideo(
        activity: Activity,
        onReward: () -> Unit,
        onFinished: () -> Unit = {}
    ): Boolean {
        val ad = rewardedAd
        if (ad == null) {
            preloadRewarded(activity.applicationContext)
            onFinished()
            return false
        }

        rewardedAd = null
        var finished = false

        fun finishOnce() {
            if (finished) return
            finished = true
            onFinished()
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                preloadRewarded(activity.applicationContext)
                finishOnce()
            }

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                preloadRewarded(activity.applicationContext)
                finishOnce()
            }
        }

        ad.show(activity) {
            onReward()
        }

        return true
    }
}
