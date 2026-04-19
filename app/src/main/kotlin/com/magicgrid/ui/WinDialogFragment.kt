package com.magicgrid.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Dialog
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import com.magicgrid.R
import com.magicgrid.data.StarManager

/**
 * Dialog that shows when the player wins a level
 * Displays stars, stats, and provides next/replay options
 */
class WinDialogFragment : AppCompatDialogFragment() {
    companion object {
        private const val STAR_STAGGER_MS = 1050L
        private const val STAR_POP_DURATION_MS = 900L
        private const val STAR_SFX_PRELOAD_FALLBACK_MS = 280L
        private const val STAR_SFX_CLIP_MS = 280L
    }

    private var starsEarned = 0
    private var timeSeconds = 0
    private var movesCount = 0
    private var onNextLevel: (() -> Unit)? = null
    private var onReplay: (() -> Unit)? = null
    private var soundPool: SoundPool? = null
    private var starSfxId = 0
    private var starSfxLoaded = false
    private var activeStreamId = 0
    private val starRates = floatArrayOf(0.88f, 1.0f, 1.12f)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val activeFallbackPlayers = mutableSetOf<MediaPlayer>()

    fun setData(stars: Int, time: Int, moves: Int): WinDialogFragment {
        starsEarned = stars.coerceIn(0, 3)
        timeSeconds = time
        movesCount = moves
        return this
    }

    fun setCallbacks(onNext: () -> Unit, onReplay: () -> Unit): WinDialogFragment {
        onNextLevel = onNext
        this.onReplay = onReplay
        return this
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext()).apply {
            setView(R.layout.dialog_win)
        }.create().also { dialog ->
            // Configure dialog after it's created
            dialog.setOnShowListener {
                setupDialog(dialog)
            }
        }
    }

    private fun setupDialog(dialog: Dialog) {
        val star1 = dialog.findViewById<ImageView>(R.id.star_1)
        val star2 = dialog.findViewById<ImageView>(R.id.star_2)
        val star3 = dialog.findViewById<ImageView>(R.id.star_3)
        val statTime = dialog.findViewById<TextView>(R.id.stat_time)
        val statMoves = dialog.findViewById<TextView>(R.id.stat_moves)
        val btnNext = dialog.findViewById<Button>(R.id.btn_next)
        val btnReplay = dialog.findViewById<Button>(R.id.btn_replay)

        // Set stats
        statTime.text = StarManager().formatTime(timeSeconds)
        statMoves.text = movesCount.toString()

        val stars = listOf(star1, star2, star3)
        var animationStarted = false

        fun startStarsOnce() {
            if (animationStarted) return
            animationStarted = true
            animateStars(stars)
        }

        // Wait for SFX preload so each star pop can play right at animation start.
        prepareStarSfx(onReady = ::startStarsOnce)
        dialog.window?.decorView?.postDelayed({
            startStarsOnce()
        }, STAR_SFX_PRELOAD_FALLBACK_MS)

        // Button handlers
        btnNext?.setOnClickListener {
            onNextLevel?.invoke()
            dialog.dismiss()
        }

        btnReplay?.setOnClickListener {
            onReplay?.invoke()
            dialog.dismiss()
        }
    }

    /**
     * Animate stars appearing one after another with scale and color change
     */
    private fun animateStars(stars: List<ImageView>) {
        stars.forEachIndexed { index, star ->
            if (index < starsEarned) {
                val starDelayMs = index * STAR_STAGGER_MS

                // Earn this star - animate it
                star.alpha = 0f
                star.scaleX = 0f
                star.scaleY = 0f

                val scaleX = ObjectAnimator.ofFloat(star, "scaleX", 0f, 1.2f, 1f).apply {
                    duration = STAR_POP_DURATION_MS
                    interpolator = OvershootInterpolator(1.5f)
                }

                val scaleY = ObjectAnimator.ofFloat(star, "scaleY", 0f, 1.2f, 1f).apply {
                    duration = STAR_POP_DURATION_MS
                    interpolator = OvershootInterpolator(1.5f)
                }

                val alpha = ObjectAnimator.ofFloat(star, "alpha", 0f, 1f).apply {
                    duration = 280
                }

                AnimatorSet().apply {
                    playTogether(scaleX, scaleY, alpha)
                    startDelay = starDelayMs
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: Animator) {
                            playStarFillTone(index)
                        }
                    })
                    start()
                }

                // Change color to gold
                star.setColorFilter(android.graphics.Color.parseColor("#FFC107"))
            } else {
                // Unearned star - keep gray
                star.alpha = 0.5f
                star.setColorFilter(android.graphics.Color.parseColor("#CCCCCC"))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        releaseStarSfx()
    }

    private fun prepareStarSfx(onReady: () -> Unit) {
        releaseStarSfx()

        val context = context ?: return
        var readyDispatched = false

        fun dispatchReadyOnce() {
            if (readyDispatched) return
            readyDispatched = true
            onReady()
        }

        val pool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()

        soundPool = pool
        starSfxLoaded = false

        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (sampleId != starSfxId || status != 0) return@setOnLoadCompleteListener

            starSfxLoaded = true
            dispatchReadyOnce()
        }

        starSfxId = pool.load(context, R.raw.foxboytails_game_start_317318, 1)
        if (starSfxId == 0) {
            dispatchReadyOnce()
        }
    }

    private fun releaseStarSfx() {
        if (activeStreamId != 0) {
            soundPool?.stop(activeStreamId)
            activeStreamId = 0
        }

        mainHandler.removeCallbacksAndMessages(null)

        soundPool?.release()
        soundPool = null
        starSfxLoaded = false
        starSfxId = 0

        synchronized(activeFallbackPlayers) {
            activeFallbackPlayers.forEach { player ->
                runCatching {
                    if (player.isPlaying) {
                        player.stop()
                    }
                }
                player.release()
            }
            activeFallbackPlayers.clear()
        }
    }

    private fun playStarFillTone(starIndex: Int) {
        if (starSfxLoaded) {
            playStarFillToneNow(starIndex)
            return
        }

        playStarFillToneFallback(starIndex)
    }

    private fun playStarFillToneNow(starIndex: Int) {
        val pool = soundPool ?: return
        if (starSfxId == 0) return

        if (activeStreamId != 0) {
            pool.stop(activeStreamId)
            activeStreamId = 0
        }

        val playbackRate = starRates[starIndex % starRates.size]
        val streamId = pool.play(starSfxId, 1f, 1f, 1, 0, playbackRate)
        if (streamId == 0) return

        activeStreamId = streamId
        mainHandler.postDelayed({
            soundPool?.stop(streamId)
            if (activeStreamId == streamId) {
                activeStreamId = 0
            }
        }, STAR_SFX_CLIP_MS)
    }

    private fun playStarFillToneFallback(starIndex: Int) {
        val context = context ?: return
        val player = MediaPlayer.create(context, R.raw.foxboytails_game_start_317318) ?: return

        synchronized(activeFallbackPlayers) {
            activeFallbackPlayers.add(player)
        }

        player.setOnCompletionListener { mediaPlayer ->
            releaseFallbackPlayer(mediaPlayer)
        }

        player.setOnErrorListener { mediaPlayer, _, _ ->
            releaseFallbackPlayer(mediaPlayer)
            true
        }

        val clipRunnable = Runnable {
            runCatching {
                if (player.isPlaying) {
                    player.stop()
                }
            }
            releaseFallbackPlayer(player)
        }

        runCatching {
            player.seekTo(0)
            player.start()
            mainHandler.postDelayed(clipRunnable, STAR_SFX_CLIP_MS)
        }.onFailure {
            mainHandler.removeCallbacks(clipRunnable)
            releaseFallbackPlayer(player)
        }
    }

    private fun releaseFallbackPlayer(player: MediaPlayer) {
        synchronized(activeFallbackPlayers) {
            if (!activeFallbackPlayers.remove(player)) {
                return
            }
        }

        runCatching {
            if (player.isPlaying) {
                player.stop()
            }
        }
        player.release()
    }
}
