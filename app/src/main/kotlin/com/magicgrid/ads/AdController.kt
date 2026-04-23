package com.magicgrid.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.magicgrid.BuildConfig
import com.magicgrid.R

/**
 * Centralized ad loading/showing for banner, interstitial, and rewarded placements.
 */
object AdController {
    private const val TAG = "AdController"
    private const val ERROR_NO_FILL = 3
    private const val RETRY_BASE_MS = 4_000L
    private const val RETRY_NO_FILL_BASE_MS = 15_000L
    private const val RETRY_MAX_MS = 180_000L
    private const val INTERSTITIAL_COOLDOWN_MS = 90_000L
    private const val INTERSTITIAL_SESSION_LIMIT = 6

    enum class RewardPlacement {
        COINS,
        HINT,
        MILESTONE
    }

    private data class RetryState(
        var attempt: Int = 0,
        var scheduled: Boolean = false
    )

    private var initialized = false
    private var launchInterstitialShown = false

    private val mainHandler = Handler(Looper.getMainLooper())

    private var interstitialAd: InterstitialAd? = null
    private val rewardedAds = mutableMapOf<RewardPlacement, RewardedAd>()

    private var isLoadingInterstitial = false
    private val rewardedLoadingStates = mutableMapOf<RewardPlacement, Boolean>()

    private val interstitialRetryState = RetryState()
    private val rewardedRetryStates = mutableMapOf<RewardPlacement, RetryState>()

    private var lastInterstitialShownAtMs = 0L
    private var interstitialShownCount = 0

    fun initialize(context: Context) {
        if (initialized) return

        MobileAds.initialize(context)
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                .build()
        )

        initialized = true
        logDebug("MobileAds initialized")
    }

    fun loadBanner(adView: AdView) {
        initialize(adView.context.applicationContext)
        adView.loadAd(AdRequest.Builder().build())
    }

    fun preloadInterstitial(context: Context) {
        val appContext = context.applicationContext
        initialize(appContext)
        if (interstitialAd != null || isLoadingInterstitial) return

        isLoadingInterstitial = true
        logDebug("Loading interstitial ad")
        InterstitialAd.load(
            appContext,
            appContext.getString(R.string.admob_interstitial_unit_id),
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoadingInterstitial = false
                    resetRetry(interstitialRetryState)
                    logDebug("Interstitial loaded")
                }

                override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                    interstitialAd = null
                    isLoadingInterstitial = false
                    scheduleRetry(
                        state = interstitialRetryState,
                        errorCode = error.code,
                        retryLabel = "interstitial"
                    ) {
                        preloadInterstitial(appContext)
                    }
                    logWarn("Interstitial failed to load: code=${error.code}, message=${error.message}")
                }
            }
        )
    }

    fun preloadRewarded(
        context: Context,
        placement: RewardPlacement = RewardPlacement.COINS
    ) {
        val appContext = context.applicationContext
        initialize(appContext)
        if (rewardedAds[placement] != null || isLoadingRewarded(placement)) return

        setLoadingRewarded(placement, true)
        logDebug("Loading rewarded ad for placement=$placement")
        RewardedAd.load(
            appContext,
            rewardedUnitId(appContext, placement),
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAds[placement] = ad
                    setLoadingRewarded(placement, false)
                    resetRetry(retryStateForPlacement(placement))
                    logDebug("Rewarded loaded for placement=$placement")
                }

                override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                    rewardedAds.remove(placement)
                    setLoadingRewarded(placement, false)
                    scheduleRetry(
                        state = retryStateForPlacement(placement),
                        errorCode = error.code,
                        retryLabel = "rewarded-$placement"
                    ) {
                        preloadRewarded(appContext, placement)
                    }
                    logWarn(
                        "Rewarded failed to load for placement=$placement: code=${error.code}, message=${error.message}"
                    )
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
        if (!canShowInterstitial()) {
            logDebug("Interstitial skipped by cooldown or session cap")
            preloadInterstitial(activity.applicationContext)
            onFinished()
            return false
        }

        val ad = interstitialAd
        if (ad == null) {
            logDebug("Interstitial not ready when requested")
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
            override fun onAdShowedFullScreenContent() {
                lastInterstitialShownAtMs = SystemClock.elapsedRealtime()
                interstitialShownCount += 1
                logDebug("Interstitial shown (count=$interstitialShownCount)")
            }

            override fun onAdDismissedFullScreenContent() {
                logDebug("Interstitial dismissed")
                preloadInterstitial(activity.applicationContext)
                finishOnce()
            }

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                logWarn("Interstitial failed to show: code=${error.code}, message=${error.message}")
                preloadInterstitial(activity.applicationContext)
                finishOnce()
            }
        }

        logDebug("Showing interstitial")
        ad.show(activity)
        return true
    }

    fun showRewardedVideo(
        activity: Activity,
        placement: RewardPlacement = RewardPlacement.COINS,
        onReward: () -> Unit,
        onFinished: () -> Unit = {}
    ): Boolean {
        val ad = rewardedAds[placement]
        if (ad == null) {
            logDebug("Rewarded not ready when requested for placement=$placement")
            preloadRewarded(activity.applicationContext, placement)
            onFinished()
            return false
        }

        rewardedAds.remove(placement)
        var finished = false

        fun finishOnce() {
            if (finished) return
            finished = true
            onFinished()
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                logDebug("Rewarded dismissed for placement=$placement")
                preloadRewarded(activity.applicationContext, placement)
                finishOnce()
            }

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                logWarn(
                    "Rewarded failed to show for placement=$placement: code=${error.code}, message=${error.message}"
                )
                preloadRewarded(activity.applicationContext, placement)
                finishOnce()
            }
        }

        logDebug("Showing rewarded for placement=$placement")
        ad.show(activity) {
            logDebug("Rewarded callback fired for placement=$placement")
            onReward()
        }

        return true
    }

    private fun rewardedUnitId(context: Context, placement: RewardPlacement): String {
        val resId = when (placement) {
            RewardPlacement.COINS -> R.string.admob_rewarded_coins_unit_id
            RewardPlacement.HINT -> R.string.admob_rewarded_hint_unit_id
            RewardPlacement.MILESTONE -> R.string.admob_rewarded_milestone_unit_id
        }

        val placementId = context.getString(resId).trim()
        if (placementId.isNotEmpty()) {
            return placementId
        }

        return context.getString(R.string.admob_rewarded_unit_id)
    }

    private fun isLoadingRewarded(placement: RewardPlacement): Boolean {
        return rewardedLoadingStates[placement] == true
    }

    private fun setLoadingRewarded(placement: RewardPlacement, loading: Boolean) {
        rewardedLoadingStates[placement] = loading
    }

    private fun retryStateForPlacement(placement: RewardPlacement): RetryState {
        return rewardedRetryStates.getOrPut(placement) { RetryState() }
    }

    private fun resetRetry(state: RetryState) {
        state.attempt = 0
        state.scheduled = false
    }

    private fun scheduleRetry(
        state: RetryState,
        errorCode: Int,
        retryLabel: String,
        action: () -> Unit
    ) {
        if (state.scheduled) {
            return
        }

        state.scheduled = true
        state.attempt += 1

        val delayMs = computeRetryDelayMs(errorCode, state.attempt)
        logDebug("Scheduling $retryLabel retry in ${delayMs}ms (attempt=${state.attempt})")

        mainHandler.postDelayed({
            state.scheduled = false
            action()
        }, delayMs)
    }

    private fun computeRetryDelayMs(errorCode: Int, attempt: Int): Long {
        val baseDelay = if (errorCode == ERROR_NO_FILL) RETRY_NO_FILL_BASE_MS else RETRY_BASE_MS
        val exponent = (attempt - 1).coerceAtLeast(0).coerceAtMost(4)
        val multiplier = 1L shl exponent
        return (baseDelay * multiplier).coerceAtMost(RETRY_MAX_MS)
    }

    private fun canShowInterstitial(): Boolean {
        if (interstitialShownCount >= INTERSTITIAL_SESSION_LIMIT) {
            return false
        }

        val now = SystemClock.elapsedRealtime()
        val timeSinceLastShow = now - lastInterstitialShownAtMs
        return timeSinceLastShow >= INTERSTITIAL_COOLDOWN_MS
    }

    private fun logDebug(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }

    private fun logWarn(message: String) {
        Log.w(TAG, message)
    }
}
