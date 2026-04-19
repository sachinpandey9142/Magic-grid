package com.magicgrid.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Dialog
import android.media.MediaPlayer
import android.os.Build
import android.media.PlaybackParams
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
        private const val STAR_STAGGER_MS = 920L
        private const val STAR_POP_DURATION_MS = 780L
        private const val STAR_SFX_START_OFFSET_MS = 180
        private const val STAR_SFX_CLIP_MS = 300L
    }

    private var starsEarned = 0
    private var timeSeconds = 0
    private var movesCount = 0
    private var onNextLevel: (() -> Unit)? = null
    private var onReplay: (() -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val starRates = floatArrayOf(0.88f, 1.0f, 1.12f)
    private val activePlayers = mutableSetOf<MediaPlayer>()

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

        animateStars(listOf(star1, star2, star3))

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

                star.postDelayed({
                    playStarFillTone(index)
                }, starDelayMs)

                AnimatorSet().apply {
                    playTogether(scaleX, scaleY, alpha)
                    startDelay = starDelayMs
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

    private fun releaseStarSfx() {
        mainHandler.removeCallbacksAndMessages(null)

        synchronized(activePlayers) {
            activePlayers.forEach { player ->
                runCatching {
                    if (player.isPlaying) {
                        player.stop()
                    }
                }
                player.release()
            }
            activePlayers.clear()
        }
    }

    private fun playStarFillTone(starIndex: Int) {
        val context = context ?: return
        val player = MediaPlayer.create(context, R.raw.foxboytails_game_start_317318) ?: return

        synchronized(activePlayers) {
            activePlayers.add(player)
        }

        player.setOnCompletionListener { mediaPlayer ->
            releasePlayer(mediaPlayer)
        }

        player.setOnErrorListener { mediaPlayer, _, _ ->
            releasePlayer(mediaPlayer)
            true
        }

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val params = PlaybackParams()
                params.speed = starRates[starIndex % starRates.size]
                player.playbackParams = params
            }

            val safeOffset = STAR_SFX_START_OFFSET_MS.coerceAtMost((player.duration - 80).coerceAtLeast(0))
            player.seekTo(safeOffset)
            player.start()

            mainHandler.postDelayed({
                releasePlayer(player)
            }, STAR_SFX_CLIP_MS)
        }.onFailure {
            releasePlayer(player)
        }
    }

    private fun releasePlayer(player: MediaPlayer) {
        synchronized(activePlayers) {
            if (!activePlayers.remove(player)) {
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
