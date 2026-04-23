package com.magicgrid.game

import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import com.google.android.gms.ads.AdView
import com.magicgrid.R
import com.magicgrid.ads.AdController
import com.magicgrid.data.ProgressManager
import com.magicgrid.ui.TwinkleTitleView
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Main menu activity - home screen with navigation
 */
class MenuActivity : AppCompatActivity() {
    private lateinit var progressManager: ProgressManager
    private lateinit var continueStatusText: TextView
    private lateinit var coinStatusText: TextView
    private lateinit var appTitle: TwinkleTitleView
    private var menuBannerAd: AdView? = null
    private var dotBounceSoundPlayer: DotBounceSoundPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        progressManager = ProgressManager(this)

        val playButton = findViewById<Button>(R.id.btn_play)
        val levelsButton = findViewById<Button>(R.id.btn_levels)
        val storeButton = findViewById<Button>(R.id.btn_store)
        val settingsButton = findViewById<Button>(R.id.btn_settings)
        val exitButton = findViewById<Button>(R.id.btn_exit)
        continueStatusText = findViewById(R.id.menu_continue_text)
        coinStatusText = findViewById(R.id.menu_coin_text)
        appTitle = findViewById(R.id.app_title)
        menuBannerAd = findViewById(R.id.menu_banner_ad)

        dotBounceSoundPlayer = DotBounceSoundPlayer()

        AdController.initialize(this)
        AdController.preloadInterstitial(applicationContext)
        AdController.preloadRewarded(applicationContext, AdController.RewardPlacement.COINS)
        AdController.preloadRewarded(applicationContext, AdController.RewardPlacement.HINT)
        AdController.preloadRewarded(applicationContext, AdController.RewardPlacement.MILESTONE)
        menuBannerAd?.let { AdController.loadBanner(it) }

        window.decorView.post {
            AdController.showLaunchInterstitialIfNeeded(this)
        }

        appTitle.setOnDotImpactListener { event ->
            playDotImpactSound(event.dotIndex, event.impactIndex)
            if (event.isFinalImpact) {
                playFinalSettleFeedback()
            }
        }

        updateContinueStatus()
        prepareTitleIntroAnimation()

        animateMenuEntry(
            780L,
            continueStatusText,
            coinStatusText,
            playButton,
            levelsButton,
            storeButton,
            settingsButton,
            exitButton
        )

        playButton.setOnClickListener {
            playTapFeedback(playButton)

            val preferredLevel = progressManager.getCurrentLevel()
            val levelToPlay = if (progressManager.isLevelUnlocked(preferredLevel)) {
                preferredLevel
            } else {
                progressManager.getHighestUnlockedLevel()
            }

            progressManager.setCurrentLevel(levelToPlay)
            startGameActivity(levelToPlay)
        }

        levelsButton.setOnClickListener {
            playTapFeedback(levelsButton)
            startActivity(Intent(this, LevelSelectActivity::class.java))
        }

        storeButton.setOnClickListener {
            playTapFeedback(storeButton)
            startActivity(Intent(this, StoreActivity::class.java))
        }

        settingsButton.setOnClickListener {
            playTapFeedback(settingsButton)
            // Settings screen (can be implemented later)
            showToast("Settings coming soon!")
        }

        exitButton.setOnClickListener {
            playTapFeedback(exitButton)
            finish()
        }
    }

    override fun onDestroy() {
        appTitle.setOnDotImpactListener(null)
        menuBannerAd?.destroy()
        menuBannerAd = null
        dotBounceSoundPlayer?.release()
        dotBounceSoundPlayer = null
        super.onDestroy()
    }

    override fun onPause() {
        menuBannerAd?.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        menuBannerAd?.resume()
        updateContinueStatus()
    }

    private fun updateContinueStatus() {
        val preferredLevel = progressManager.getCurrentLevel()
        val levelToContinue = if (progressManager.isLevelUnlocked(preferredLevel)) {
            preferredLevel
        } else {
            progressManager.getHighestUnlockedLevel()
        }

        val highestUnlocked = progressManager.getHighestUnlockedLevel()
        val totalStars = progressManager.getTotalStars()

        continueStatusText.text = getString(
            R.string.menu_continue_status_format,
            levelToContinue,
            highestUnlocked,
            totalStars
        )
        coinStatusText.text = getString(R.string.menu_coin_balance_format, progressManager.getCoins())
    }

    private fun prepareTitleIntroAnimation() {
        appTitle.pauseTwinkle()
        appTitle.prepareDotBounceIntro()

        appTitle.alpha = 0f
        appTitle.translationY = -220f

        appTitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(680L)
            .setInterpolator(DecelerateInterpolator(1.25f))
            .withEndAction {
                appTitle.postDelayed({
                    appTitle.playDotBounceIntro {
                        appTitle.resumeTwinkle()
                    }
                }, 170L)
            }
            .start()
    }

    private fun animateMenuEntry(initialDelayMs: Long, vararg views: View) {
        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 44f

            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(340)
                .setStartDelay(initialDelayMs + (index * 70L))
                .setInterpolator(OvershootInterpolator(0.95f))
                .start()
        }
    }

    private fun playTapFeedback(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        view.animate().cancel()
        view.animate()
            .scaleX(0.97f)
            .scaleY(0.97f)
            .setDuration(60)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .start()
            }
            .start()
    }

    private fun playDotImpactSound(dotIndex: Int, impactIndex: Int) {
        dotBounceSoundPlayer?.playBounce(dotIndex, impactIndex)
    }

    private fun playFinalSettleFeedback() {
        dotBounceSoundPlayer?.playSettle()

        val primaryFeedback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }

        val handled = appTitle.performHapticFeedback(primaryFeedback)
        if (!handled && primaryFeedback != HapticFeedbackConstants.LONG_PRESS) {
            appTitle.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    private fun startGameActivity(levelId: Int) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("level_id", levelId)
        startActivity(intent)
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}

/**
 * Small synthesized bounce sound player for menu intro dot impacts.
 */
private class DotBounceSoundPlayer {
    private val sampleRate = 24000
    private val bounceSampleData = generateBounceSample(sampleRate, 58)
    private val settleSampleData = generateSettleSample(sampleRate, 188)
    private val bounceTracks = mutableListOf<AudioTrack>()
    private val settleTracks = mutableListOf<AudioTrack>()
    private var bounceTrackCursor = 0
    private var settleTrackCursor = 0

    init {
        repeat(8) {
            createStaticTrack(bounceSampleData)?.let { bounceTracks.add(it) }
        }
        repeat(3) {
            createStaticTrack(settleSampleData)?.let { settleTracks.add(it) }
        }
    }

    fun playBounce(dotIndex: Int, impactIndex: Int) {
        if (bounceTracks.isEmpty()) return

        val volume = when (impactIndex) {
            0 -> 0.136f
            1 -> 0.114f
            2 -> 0.096f
            else -> 0.082f
        }

        val pitchFactor = when (impactIndex) {
            0 -> 1.00f
            1 -> 1.07f
            2 -> 1.14f
            else -> 1.20f
        } + if (dotIndex % 2 == 0) 0f else 0.008f

        val playbackRate = (sampleRate * pitchFactor)
            .toInt()
            .coerceIn(4000, 48000)

        playFromPool(
            pool = bounceTracks,
            startCursor = bounceTrackCursor,
            onCursorUpdate = { bounceTrackCursor = it },
            playbackRate = playbackRate,
            volume = volume
        )
    }

    fun playSettle() {
        if (settleTracks.isEmpty()) return

        playFromPool(
            pool = settleTracks,
            startCursor = settleTrackCursor,
            onCursorUpdate = { settleTrackCursor = it },
            playbackRate = (sampleRate * 1.02f).toInt().coerceIn(4000, 48000),
            volume = 0.15f
        )
    }

    private fun playFromPool(
        pool: List<AudioTrack>,
        startCursor: Int,
        onCursorUpdate: (Int) -> Unit,
        playbackRate: Int,
        volume: Float
    ) {
        val selection = selectTrack(pool, startCursor) ?: return
        val track = selection.first
        onCursorUpdate(selection.second)

        try {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                track.stop()
            }
            track.setPlaybackHeadPosition(0)
            track.playbackRate = playbackRate
            track.setVolume(volume)
            track.play()
        } catch (_: IllegalStateException) {
            // Ignore transient audio state issues to avoid interrupting UI animation.
        }
    }

    private fun selectTrack(
        pool: List<AudioTrack>,
        startCursor: Int
    ): Pair<AudioTrack, Int>? {
        if (pool.isEmpty()) return null

        val normalizedStart = ((startCursor % pool.size) + pool.size) % pool.size

        for (offset in pool.indices) {
            val index = (normalizedStart + offset) % pool.size
            val track = pool[index]
            if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                return track to ((index + 1) % pool.size)
            }
        }

        val fallback = pool[normalizedStart]
        return fallback to ((normalizedStart + 1) % pool.size)
    }

    fun release() {
        bounceTracks.forEach { track ->
            try {
                track.release()
            } catch (_: Exception) {
                // Best-effort release.
            }
        }
        bounceTracks.clear()

        settleTracks.forEach { track ->
            try {
                track.release()
            } catch (_: Exception) {
                // Best-effort release.
            }
        }
        settleTracks.clear()
    }

    private fun createStaticTrack(sampleData: ShortArray): AudioTrack? {
        return try {
            val builder = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(sampleData.size * 2)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            }

            val track = builder.build()

            val written = track.write(sampleData, 0, sampleData.size, AudioTrack.WRITE_BLOCKING)
            if (written <= 0) {
                track.release()
                null
            } else {
                primeTrack(track)
                track
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun primeTrack(track: AudioTrack) {
        try {
            track.setVolume(0f)
            track.play()
            track.pause()
            track.setPlaybackHeadPosition(0)
            track.setVolume(1f)
        } catch (_: Exception) {
            // Best effort warm-up.
        }
    }

    private fun generateBounceSample(sampleRate: Int, durationMs: Int): ShortArray {
        val sampleCount = ((sampleRate * durationMs) / 1000).coerceAtLeast(1)
        val pcm = ShortArray(sampleCount)
        var noiseState = 0x6A32CD19L

        for (i in 0 until sampleCount) {
            val t = i.toDouble() / sampleRate.toDouble()

            // Fast transient for tighter perceived sync + watery resonant body.
            val attack = if (t < 0.0014) t / 0.0014 else 1.0
            val bodyDecay = exp(-t * 33.0)
            val ringDecay = exp(-t * 48.0)
            val transientDecay = exp(-t * 210.0)

            val waterBody = sin(2.0 * PI * 236.0 * t + 0.10) * bodyDecay
            val dropletRing = sin(2.0 * PI * 588.0 * t + 0.48) * ringDecay
            val bubbleTone = sin(2.0 * PI * 1190.0 * t + 0.33) * exp(-t * 74.0)
            val clickTone = sin(2.0 * PI * 2150.0 * t + 0.05) * transientDecay

            noiseState = (noiseState * 1664525L + 1013904223L) and 0xFFFFFFFFL
            val noise = (((noiseState shr 8) and 0xFFFF).toDouble() / 32768.0) - 1.0
            val airSplash = noise * transientDecay * 0.22

            val shaped =
                (waterBody * 0.50) +
                    (dropletRing * 0.27) +
                    (bubbleTone * 0.15) +
                    (clickTone * 0.11) +
                    airSplash

            val sample = (shaped * attack * 0.46).coerceIn(-1.0, 1.0)
            pcm[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }

        return pcm
    }

    private fun generateSettleSample(sampleRate: Int, durationMs: Int): ShortArray {
        val sampleCount = ((sampleRate * durationMs) / 1000).coerceAtLeast(1)
        val pcm = ShortArray(sampleCount)
        var noiseState = 0x39C17B7DL

        for (i in 0 until sampleCount) {
            val t = i.toDouble() / sampleRate.toDouble()

            val attack = if (t < 0.009) t / 0.009 else 1.0
            val plop = sin(2.0 * PI * 162.0 * t + 0.06) * exp(-t * 13.2)
            val bubblePop = sin(2.0 * PI * 510.0 * t + 0.28) * exp(-t * 21.0)
            val shimmer = sin(2.0 * PI * 1120.0 * t + 0.42) * exp(-t * 30.0)
            val wetClick = sin(2.0 * PI * 1710.0 * t + 0.03) * exp(-t * 85.0)

            noiseState = (noiseState * 1103515245L + 12345L) and 0xFFFFFFFFL
            val noise = (((noiseState shr 9) and 0xFFFF).toDouble() / 32768.0) - 1.0
            val air = noise * exp(-t * 86.0) * 0.08

            val shaped =
                ((plop * 0.52) + (bubblePop * 0.28) + (shimmer * 0.15) + (wetClick * 0.08) + air) *
                    attack
            val sample = (shaped * 0.50).coerceIn(-1.0, 1.0)

            pcm[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }

        return pcm
    }
}
