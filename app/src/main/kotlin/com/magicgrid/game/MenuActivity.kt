package com.magicgrid.game

import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import com.magicgrid.R
import com.magicgrid.data.ProgressManager

/**
 * Main menu activity - home screen with navigation
 */
class MenuActivity : AppCompatActivity() {
    private lateinit var progressManager: ProgressManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        progressManager = ProgressManager(this)

        val playButton = findViewById<Button>(R.id.btn_play)
        val levelsButton = findViewById<Button>(R.id.btn_levels)
        val settingsButton = findViewById<Button>(R.id.btn_settings)
        val exitButton = findViewById<Button>(R.id.btn_exit)

        animateMenuEntry(playButton, levelsButton, settingsButton, exitButton)

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

    private fun animateMenuEntry(vararg views: View) {
        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 44f

            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(340)
                .setStartDelay((index * 70L) + 80L)
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

    private fun startGameActivity(levelId: Int) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("level_id", levelId)
        startActivity(intent)
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
