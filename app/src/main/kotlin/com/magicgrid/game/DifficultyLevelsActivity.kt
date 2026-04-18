package com.magicgrid.game

import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.magicgrid.R
import com.magicgrid.data.Level
import com.magicgrid.data.LevelRepository
import com.magicgrid.data.ProgressManager
import kotlin.math.roundToInt

/**
 * Dedicated difficulty page showing all levels for the selected difficulty.
 */
class DifficultyLevelsActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_DIFFICULTY_KEY = "extra_difficulty_key"

        const val DIFFICULTY_EASY = "easy"
        const val DIFFICULTY_MEDIUM = "medium"
        const val DIFFICULTY_HARD = "hard"
    }

    private data class DifficultyPack(
        val key: String,
        val title: String,
        val subtitle: String,
        val gridLabel: String,
        val levelRange: IntRange
    )

    private lateinit var progressManager: ProgressManager
    private lateinit var difficultyTitleTextView: TextView
    private lateinit var difficultySubtitleTextView: TextView
    private lateinit var starsTextView: TextView
    private lateinit var progressTextView: TextView
    private lateinit var unlockedTextView: TextView
    private lateinit var quickPlayButton: AppCompatButton
    private lateinit var levelMapContainer: LinearLayout

    private lateinit var selectedPack: DifficultyPack

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_difficulty_levels)

        progressManager = ProgressManager(this)

        difficultyTitleTextView = findViewById(R.id.difficulty_title)
        difficultySubtitleTextView = findViewById(R.id.difficulty_subtitle)
        starsTextView = findViewById(R.id.difficulty_stars)
        progressTextView = findViewById(R.id.difficulty_progress)
        unlockedTextView = findViewById(R.id.difficulty_unlocked)
        quickPlayButton = findViewById(R.id.btn_quick_play)
        levelMapContainer = findViewById(R.id.level_map_container)

        selectedPack = resolvePack(intent.getStringExtra(EXTRA_DIFFICULTY_KEY))

        findViewById<Button>(R.id.btn_back).setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        renderPackDetails()
    }

    private fun resolvePack(key: String?): DifficultyPack {
        val easyRange = 1..LevelRepository.EASY_LEVEL_COUNT
        val mediumRangeStart = easyRange.last + 1
        val mediumRange = mediumRangeStart..(mediumRangeStart + LevelRepository.MEDIUM_LEVEL_COUNT - 1)
        val hardRangeStart = mediumRange.last + 1
        val hardRange = hardRangeStart..(hardRangeStart + LevelRepository.HARD_LEVEL_COUNT - 1)

        return when (key) {
            DIFFICULTY_MEDIUM -> DifficultyPack(
                key = DIFFICULTY_MEDIUM,
                title = getString(R.string.difficulty_medium_title),
                subtitle = getString(R.string.difficulty_medium_subtitle),
                gridLabel = "6x6",
                levelRange = mediumRange
            )
            DIFFICULTY_HARD -> DifficultyPack(
                key = DIFFICULTY_HARD,
                title = getString(R.string.difficulty_hard_title),
                subtitle = getString(R.string.difficulty_hard_subtitle),
                gridLabel = "7x7",
                levelRange = hardRange
            )
            else -> DifficultyPack(
                key = DIFFICULTY_EASY,
                title = getString(R.string.difficulty_easy_title),
                subtitle = getString(R.string.difficulty_easy_subtitle),
                gridLabel = "5x5",
                levelRange = easyRange
            )
        }
    }

    private fun renderPackDetails() {
        val levelsInPack = LevelRepository.getAllLevels().filter { it.id in selectedPack.levelRange }
        val totalLevels = levelsInPack.size
        val highestUnlocked = progressManager.getHighestUnlockedLevel()

        val starsInPack = levelsInPack.sumOf { progressManager.getLevelStars(it.id) }
        val completedCount = levelsInPack.count { progressManager.isLevelCompleted(it.id) }
        val unlockedInPack = levelsInPack.count { it.id <= highestUnlocked }

        difficultyTitleTextView.text = selectedPack.title
        difficultySubtitleTextView.text = getString(
            R.string.difficulty_detail_subtitle_format,
            selectedPack.subtitle,
            selectedPack.gridLabel,
            selectedPack.levelRange.first,
            selectedPack.levelRange.last
        )

        starsTextView.text = getString(R.string.difficulty_detail_stars_format, starsInPack)
        progressTextView.text = getString(R.string.difficulty_detail_progress_format, completedCount, totalLevels)
        unlockedTextView.text = getString(R.string.difficulty_detail_unlock_format, unlockedInPack, totalLevels)

        setupQuickPlay(levelsInPack, highestUnlocked)
        populateLevels(levelsInPack, highestUnlocked)
    }

    private fun setupQuickPlay(levelsInPack: List<Level>, highestUnlocked: Int) {
        val playable = levelsInPack.filter { it.id <= highestUnlocked }
        if (playable.isEmpty()) {
            quickPlayButton.text = getString(
                R.string.quick_play_locked,
                selectedPack.levelRange.first
            )
            quickPlayButton.isEnabled = false
            quickPlayButton.alpha = 0.65f
            quickPlayButton.setOnClickListener(null)
            return
        }

        val nextLevel = playable.firstOrNull { !progressManager.isLevelCompleted(it.id) } ?: playable.last()

        quickPlayButton.text = getString(R.string.quick_play_level_format, nextLevel.id)
        quickPlayButton.isEnabled = true
        quickPlayButton.alpha = 1f
        quickPlayButton.setOnClickListener {
            launchLevelWithFeedback(nextLevel.id, quickPlayButton)
        }
    }

    private fun populateLevels(levelsInPack: List<Level>, highestUnlocked: Int) {
        levelMapContainer.removeAllViews()

        if (levelsInPack.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = getString(R.string.level_map_empty)
                textSize = 14f
                setTextColor(getColor(R.color.text_secondary))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(16)
                    bottomMargin = dp(24)
                }
            }
            levelMapContainer.addView(emptyText)
            return
        }

        val completedLevels = progressManager.getCompletedLevels()
        val rows = levelsInPack.chunked(4)

        rows.forEachIndexed { rowIndex, rowLevels ->
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
                val visualIndex = rowIndex * 4 + slotIndex
                rowLayout.addView(
                    createLevelNode(
                        level = level,
                        completedLevels = completedLevels,
                        highestUnlocked = highestUnlocked,
                        visualIndex = visualIndex
                    )
                )
            }

            repeat((4 - displayLevels.size).coerceAtLeast(0)) {
                rowLayout.addView(createLevelSpacer())
            }

            levelMapContainer.addView(rowLayout)

            if (rowIndex < rows.lastIndex) {
                levelMapContainer.addView(createConnector(rowIndex))
            }
        }
    }

    private fun createConnector(rowIndex: Int): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(18)
            ).apply {
                marginStart = dp(18)
                marginEnd = dp(18)
            }

            background = getDrawable(
                if (rowIndex % 2 == 0) {
                    R.drawable.level_map_connector_right
                } else {
                    R.drawable.level_map_connector_left
                }
            )
            alpha = 0.7f
        }
    }

    private fun createLevelNode(
        level: Level,
        completedLevels: Set<String>,
        highestUnlocked: Int,
        visualIndex: Int
    ): AppCompatButton {
        val isUnlocked = level.id <= highestUnlocked
        val isCompleted = completedLevels.contains(level.id.toString())
        val stars = progressManager.getLevelStars(level.id)

        val nodeButton = AppCompatButton(this)
        nodeButton.layoutParams = LinearLayout.LayoutParams(0, dp(86), 1f).apply {
            marginStart = dp(5)
            marginEnd = dp(5)
            topMargin = dp(4)
            bottomMargin = dp(4)
        }

        nodeButton.text = if (isCompleted) {
            buildCompletedLevelLabel(level.id, stars)
        } else {
            getString(R.string.level_node_number_format, level.id)
        }

        nodeButton.isAllCaps = false
        nodeButton.textSize = 13f
        nodeButton.gravity = Gravity.CENTER
        nodeButton.setPadding(dp(4), dp(8), dp(4), dp(8))
        nodeButton.setLineSpacing(0f, 1.06f)
        nodeButton.elevation = dp(3).toFloat()

        when {
            isCompleted -> {
                nodeButton.setBackgroundResource(resolveCompletedNodeBackground(stars))
                nodeButton.alpha = 1f
            }
            isUnlocked -> {
                nodeButton.setBackgroundResource(R.drawable.level_node_unlocked)
                nodeButton.alpha = 1f
            }
            else -> {
                nodeButton.setBackgroundResource(R.drawable.level_node_locked)
                nodeButton.alpha = 0.85f
            }
        }

        nodeButton.isEnabled = true
        nodeButton.setOnClickListener {
            if (isUnlocked) {
                launchLevelWithFeedback(level.id, nodeButton)
            } else {
                nodeButton.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                showToast(getString(R.string.level_locked_toast_detail, highestUnlocked))
            }
        }

        nodeButton.alpha = 0f
        nodeButton.translationY = dp(20).toFloat()
        nodeButton.animate()
            .alpha(if (isUnlocked) 1f else 0.85f)
            .translationY(0f)
            .setDuration(220)
            .setStartDelay(50L + (visualIndex * 14L))
            .setInterpolator(OvershootInterpolator(0.9f))
            .start()

        return nodeButton
    }

    private fun buildCompletedLevelLabel(levelId: Int, earnedStars: Int): CharSequence {
        val stars = earnedStars.coerceIn(1, 3)
        val starTint = ContextCompat.getColor(this, R.color.accent_gold)
        val builder = SpannableStringBuilder()
        builder.append(levelId.toString())
        builder.append("\n")

        repeat(stars) { index ->
            if (index > 0) {
                builder.append(" ")
            }

            val start = builder.length
            builder.append("*")

            val starDrawable = ContextCompat.getDrawable(this, R.drawable.ic_star)?.mutate()
            if (starDrawable != null) {
                val tintedDrawable = DrawableCompat.wrap(starDrawable)
                DrawableCompat.setTint(tintedDrawable, starTint)
                val size = dp(12)
                tintedDrawable.setBounds(0, 0, size, size)
                val starSpan = ImageSpan(tintedDrawable, ImageSpan.ALIGN_BOTTOM)
                builder.setSpan(starSpan, start, start + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        return builder
    }

    private fun resolveCompletedNodeBackground(earnedStars: Int): Int {
        return when (earnedStars.coerceIn(1, 3)) {
            1 -> R.drawable.level_node_completed_one_star
            2 -> R.drawable.level_node_completed_two_stars
            else -> R.drawable.level_node_completed
        }
    }

    private fun createLevelSpacer(): Space {
        return Space(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, dp(86), 1f).apply {
                marginStart = dp(5)
                marginEnd = dp(5)
                topMargin = dp(4)
                bottomMargin = dp(4)
            }
        }
    }

    private fun launchLevelWithFeedback(levelId: Int, sourceView: View) {
        sourceView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

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
