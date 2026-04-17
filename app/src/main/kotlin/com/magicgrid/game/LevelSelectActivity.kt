package com.magicgrid.game

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import com.magicgrid.R
import com.magicgrid.data.Level
import com.magicgrid.data.LevelRepository
import com.magicgrid.data.ProgressManager
import kotlin.math.roundToInt

/**
 * Level selection screen - shows all available levels
 */
class LevelSelectActivity : AppCompatActivity() {
    private lateinit var progressManager: ProgressManager
    private lateinit var titleTextView: TextView
    private lateinit var subtitleTextView: TextView
    private lateinit var levelMapContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_level_select)

        progressManager = ProgressManager(this)
        titleTextView = findViewById(R.id.level_select_title)
        subtitleTextView = findViewById(R.id.level_select_subtitle)
        levelMapContainer = findViewById(R.id.level_map_container)

        // Populate level map
        populateLevels()

        // Back button
        val backButton = findViewById<Button>(R.id.btn_back)
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun populateLevels() {
        val levels = LevelRepository.getAllLevels()
        val completedLevels = progressManager.getCompletedLevels()
        val highestUnlocked = progressManager.getHighestUnlockedLevel()
        val totalStars = progressManager.getTotalStars()

        titleTextView.text = getString(R.string.level_map_title_format, highestUnlocked, levels.size)
        subtitleTextView.text = getString(
            R.string.level_map_subtitle_format,
            completedLevels.size,
            levels.size,
            totalStars
        )

        levelMapContainer.removeAllViews()

        if (levels.isEmpty()) {
            subtitleTextView.text = getString(R.string.level_map_empty)
            return
        }

        val rows = levels.chunked(3)
        rows.forEachIndexed { rowIndex, chunk ->
            addLevelRow(chunk, rowIndex, completedLevels)

            if (rowIndex < rows.lastIndex) {
                addConnector(rowIndex)
            }
        }
    }

    private fun addLevelRow(
        rowLevels: List<Level>,
        rowIndex: Int,
        completedLevels: Set<String>
    ) {
        val rowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val displayLevels = if (rowIndex % 2 == 0) rowLevels else rowLevels.reversed()

        displayLevels.forEachIndexed { slotIndex, level ->
            val visualIndex = rowIndex * 3 + slotIndex
            rowLayout.addView(createLevelNode(level, completedLevels, visualIndex))
        }

        repeat((3 - displayLevels.size).coerceAtLeast(0)) {
            rowLayout.addView(createLevelSpacer())
        }

        levelMapContainer.addView(rowLayout)
    }

    private fun addConnector(rowIndex: Int) {
        val connector = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(20)
            ).apply {
                marginStart = dp(22)
                marginEnd = dp(22)
            }

            background = getDrawable(
                if (rowIndex % 2 == 0) {
                    R.drawable.level_map_connector_right
                } else {
                    R.drawable.level_map_connector_left
                }
            )
            alpha = 0.85f
        }

        levelMapContainer.addView(connector)
    }

    private fun createLevelNode(
        level: Level,
        completedLevels: Set<String>,
        visualIndex: Int
    ): AppCompatButton {
        val isUnlocked = progressManager.isLevelUnlocked(level.id)
        val isCompleted = completedLevels.contains(level.id.toString())
        val stars = progressManager.getLevelStars(level.id)

        val nodeButton = AppCompatButton(this)
        nodeButton.layoutParams = LinearLayout.LayoutParams(0, dp(108), 1f).apply {
            marginStart = dp(6)
            marginEnd = dp(6)
            topMargin = dp(4)
            bottomMargin = dp(4)
        }

        nodeButton.text = buildNodeText(level, isUnlocked, isCompleted, stars)
        nodeButton.isAllCaps = false
        nodeButton.textSize = 12.5f
        nodeButton.gravity = Gravity.CENTER
        nodeButton.setLineSpacing(0f, 1.08f)
        nodeButton.setPadding(dp(6), dp(10), dp(6), dp(10))
        nodeButton.elevation = dp(4).toFloat()

        when {
            isCompleted -> {
                nodeButton.setBackgroundResource(R.drawable.level_node_completed)
                nodeButton.isEnabled = true
                nodeButton.alpha = 1f
            }
            isUnlocked -> {
                nodeButton.setBackgroundResource(R.drawable.level_node_unlocked)
                nodeButton.isEnabled = true
                nodeButton.alpha = 1f
            }
            else -> {
                nodeButton.setBackgroundResource(R.drawable.level_node_locked)
                nodeButton.isEnabled = false
                nodeButton.alpha = 0.8f
            }
        }

        if (isUnlocked) {
            nodeButton.setOnClickListener {
                launchLevelWithFeedback(level.id, nodeButton)
            }
        } else {
            nodeButton.setOnClickListener {
                showToast(getString(R.string.level_locked_toast))
            }
        }

        nodeButton.alpha = 0f
        nodeButton.translationY = dp(26).toFloat()
        nodeButton.animate()
            .alpha(if (isUnlocked) 1f else 0.85f)
            .translationY(0f)
            .setDuration(320)
            .setStartDelay((visualIndex * 45L) + 90L)
            .setInterpolator(OvershootInterpolator(0.9f))
            .start()

        return nodeButton
    }

    private fun createLevelSpacer(): Space {
        return Space(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, dp(108), 1f).apply {
                marginStart = dp(6)
                marginEnd = dp(6)
                topMargin = dp(4)
                bottomMargin = dp(4)
            }
        }
    }

    private fun buildNodeText(level: Level, isUnlocked: Boolean, isCompleted: Boolean, stars: Int): String {
        val firstLine = getString(R.string.level_card_title_format, level.id)

        val secondLine = when {
            !isUnlocked -> getString(R.string.level_locked_label)
            isCompleted -> getString(
                R.string.level_completed_info_format,
                stars.coerceAtLeast(1),
                level.gridSize,
                level.gridSize
            )
            else -> getString(
                R.string.level_available_info_format,
                level.getDifficulty(),
                level.gridSize,
                level.gridSize
            )
        }

        return "$firstLine\n$secondLine"
    }

    private fun launchLevelWithFeedback(levelId: Int, sourceView: View) {
        sourceView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        sourceView.isEnabled = false

        sourceView.animate().cancel()
        sourceView.animate()
            .scaleX(0.94f)
            .scaleY(0.94f)
            .setDuration(65)
            .withEndAction {
                sourceView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .withEndAction {
                        progressManager.setCurrentLevel(levelId)
                        startGameActivity(levelId)
                    }
                    .start()
            }
            .start()
    }

    private fun startGameActivity(levelId: Int) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("level_id", levelId)
        startActivity(intent)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).roundToInt()
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
