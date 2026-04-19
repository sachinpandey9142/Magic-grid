package com.magicgrid.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Dialog
import android.media.MediaPlayer
import android.os.Bundle
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
    private var starsEarned = 0
    private var timeSeconds = 0
    private var movesCount = 0
    private var onNextLevel: (() -> Unit)? = null
    private var onReplay: (() -> Unit)? = null
    private val activeSfxPlayers = mutableSetOf<MediaPlayer>()
    private val preloadedStarPlayers = ArrayDeque<MediaPlayer>()

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

        // Preload short-lived players so each star pop starts with minimal latency.
        preloadStarSfx(starsEarned)

        // Animate stars one by one
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
                val starDelayMs = (index * 200).toLong()

                // Earn this star - animate it
                star.alpha = 0f
                star.scaleX = 0f
                star.scaleY = 0f

                val scaleX = ObjectAnimator.ofFloat(star, "scaleX", 0f, 1.2f, 1f).apply {
                    duration = 450
                    startDelay = starDelayMs
                    interpolator = OvershootInterpolator(1.5f)
                }

                val scaleY = ObjectAnimator.ofFloat(star, "scaleY", 0f, 1.2f, 1f).apply {
                    duration = 450
                    startDelay = starDelayMs
                    interpolator = OvershootInterpolator(1.5f)
                }

                val alpha = ObjectAnimator.ofFloat(star, "alpha", 0f, 1f).apply {
                    duration = 200
                    startDelay = starDelayMs
                }

                AnimatorSet().apply {
                    playTogether(scaleX, scaleY, alpha)
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: Animator) {
                            playStarFillTone()
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
        activeSfxPlayers.forEach { it.release() }
        activeSfxPlayers.clear()
        releasePreloadedStarSfx()
    }

    private fun preloadStarSfx(count: Int) {
        releasePreloadedStarSfx()

        val context = context ?: return
        repeat(count) {
            MediaPlayer.create(context, R.raw.foxboytails_game_start_317318)?.let { player ->
                player.isLooping = false
                preloadedStarPlayers.addLast(player)
            }
        }
    }

    private fun releasePreloadedStarSfx() {
        preloadedStarPlayers.forEach { it.release() }
        preloadedStarPlayers.clear()
    }

    private fun playStarFillTone() {
        val context = context ?: return
        val player = preloadedStarPlayers.removeFirstOrNull()
            ?: MediaPlayer.create(context, R.raw.foxboytails_game_start_317318)
            ?: return

        activeSfxPlayers.add(player)

        player.setOnCompletionListener { mediaPlayer ->
            activeSfxPlayers.remove(mediaPlayer)
            mediaPlayer.release()
        }

        player.setOnErrorListener { mediaPlayer, _, _ ->
            activeSfxPlayers.remove(mediaPlayer)
            mediaPlayer.release()
            true
        }

        runCatching {
            player.seekTo(0)
            player.start()
        }.onFailure {
            activeSfxPlayers.remove(player)
            player.release()
        }
    }
}
