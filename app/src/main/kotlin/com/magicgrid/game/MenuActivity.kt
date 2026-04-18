package com.magicgrid.game

import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import com.magicgrid.R
import com.magicgrid.data.ProgressManager
import com.magicgrid.ui.TwinkleTitleView

/**
 * Main menu activity - home screen with navigation
 */
class MenuActivity : AppCompatActivity() {
    private lateinit var progressManager: ProgressManager
    private lateinit var continueStatusText: TextView
    private lateinit var appTitle: TwinkleTitleView
    private lateinit var titleDotMagicI: View
    private lateinit var titleDotGridI: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        progressManager = ProgressManager(this)

        val playButton = findViewById<Button>(R.id.btn_play)
        val levelsButton = findViewById<Button>(R.id.btn_levels)
        val settingsButton = findViewById<Button>(R.id.btn_settings)
        val exitButton = findViewById<Button>(R.id.btn_exit)
        continueStatusText = findViewById(R.id.menu_continue_text)
        appTitle = findViewById(R.id.app_title)
        titleDotMagicI = findViewById(R.id.title_dot_magic_i)
        titleDotGridI = findViewById(R.id.title_dot_grid_i)

        updateContinueStatus()
        prepareTitleIntroAnimation()

        animateMenuEntry(780L, continueStatusText, playButton, levelsButton, settingsButton, exitButton)

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

    override fun onResume() {
        super.onResume()
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
    }

    private fun prepareTitleIntroAnimation() {
        appTitle.pauseTwinkle()

        appTitle.post {
            val titleText = appTitle.text?.toString().orEmpty()
            val firstI = titleText.lowercase().indexOf('i')
            val secondI = if (firstI >= 0) {
                titleText.lowercase().indexOf('i', firstI + 1)
            } else {
                -1
            }

            positionDotOverCharacter(titleDotMagicI, firstI)
            positionDotOverCharacter(titleDotGridI, secondI)

            appTitle.alpha = 0f
            appTitle.translationY = -220f

            resetTitleDot(titleDotMagicI)
            resetTitleDot(titleDotGridI)

            appTitle.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(680L)
                .setInterpolator(DecelerateInterpolator(1.25f))
                .withEndAction {
                    appTitle.postDelayed({
                        dropTitleDots()
                    }, 180L)
                }
                .start()
        }
    }

    private fun dropTitleDots() {
        titleDotMagicI.animate().cancel()
        titleDotGridI.animate().cancel()

        titleDotMagicI.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(560L)
            .setInterpolator(BounceInterpolator())
            .start()

        titleDotGridI.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(560L)
            .setStartDelay(90L)
            .setInterpolator(BounceInterpolator())
            .withEndAction {
                appTitle.resumeTwinkle()
            }
            .start()
    }

    private fun resetTitleDot(dot: View) {
        dot.alpha = 0f
        dot.translationY = -120f
        dot.scaleX = 0.74f
        dot.scaleY = 0.74f
    }

    private fun positionDotOverCharacter(dot: View, charIndex: Int) {
        val text = appTitle.text?.toString().orEmpty()
        if (charIndex !in text.indices) {
            dot.visibility = View.INVISIBLE
            return
        }

        dot.visibility = View.VISIBLE

        val textPaint = appTitle.paint
        val prefixWidth = textPaint.measureText(text, 0, charIndex)
        val charWidth = textPaint.measureText(text, charIndex, charIndex + 1)
        val charCenterX = appTitle.left + prefixWidth + (charWidth / 2f)

        val metrics = textPaint.fontMetrics
        val baselineY = appTitle.top + appTitle.baseline.toFloat()
        val textTop = baselineY + metrics.ascent
        val textHeight = metrics.descent - metrics.ascent
        val dotCenterY = textTop + (textHeight * 0.16f)

        dot.x = charCenterX - (dot.width / 2f)
        dot.y = dotCenterY - (dot.height / 2f)
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

    private fun startGameActivity(levelId: Int) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("level_id", levelId)
        startActivity(intent)
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
