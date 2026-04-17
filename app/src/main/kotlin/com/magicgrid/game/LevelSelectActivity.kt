package com.magicgrid.game

import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import android.widget.Button
import android.widget.TextView
import com.magicgrid.R
import com.magicgrid.data.LevelRepository
import com.magicgrid.data.ProgressManager

/**
 * Level selection screen - shows all available levels
 */
class LevelSelectActivity : AppCompatActivity() {
    private data class DifficultyPack(
        val key: String,
        val title: String,
        val subtitle: String,
        val gridLabel: String,
        val levelCount: Int,
        val levelRange: IntRange
    )

    private lateinit var progressManager: ProgressManager
    private lateinit var titleTextView: TextView
    private lateinit var subtitleTextView: TextView
    private lateinit var easyCard: AppCompatButton
    private lateinit var mediumCard: AppCompatButton
    private lateinit var hardCard: AppCompatButton
    private lateinit var packs: List<DifficultyPack>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_level_select)

        progressManager = ProgressManager(this)
        titleTextView = findViewById(R.id.level_select_title)
        subtitleTextView = findViewById(R.id.level_select_subtitle)
        easyCard = findViewById(R.id.card_easy)
        mediumCard = findViewById(R.id.card_medium)
        hardCard = findViewById(R.id.card_hard)

        packs = buildDifficultyPacks()

        bindDifficultyCards()
        animateCardsOnEntry()
        refreshHeader()

        // Back button
        val backButton = findViewById<Button>(R.id.btn_back)
        backButton.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshHeader()
        bindDifficultyCards()
    }

    private fun buildDifficultyPacks(): List<DifficultyPack> {
        val easyRange = 1..LevelRepository.EASY_LEVEL_COUNT
        val mediumRangeStart = easyRange.last + 1
        val mediumRange = mediumRangeStart..(mediumRangeStart + LevelRepository.MEDIUM_LEVEL_COUNT - 1)
        val hardRangeStart = mediumRange.last + 1
        val hardRange = hardRangeStart..(hardRangeStart + LevelRepository.HARD_LEVEL_COUNT - 1)

        return listOf(
            DifficultyPack(
                key = DifficultyLevelsActivity.DIFFICULTY_EASY,
                title = getString(R.string.difficulty_easy_title),
                subtitle = getString(R.string.difficulty_easy_subtitle),
                gridLabel = "5x5",
                levelCount = LevelRepository.EASY_LEVEL_COUNT,
                levelRange = easyRange
            ),
            DifficultyPack(
                key = DifficultyLevelsActivity.DIFFICULTY_MEDIUM,
                title = getString(R.string.difficulty_medium_title),
                subtitle = getString(R.string.difficulty_medium_subtitle),
                gridLabel = "6x6",
                levelCount = LevelRepository.MEDIUM_LEVEL_COUNT,
                levelRange = mediumRange
            ),
            DifficultyPack(
                key = DifficultyLevelsActivity.DIFFICULTY_HARD,
                title = getString(R.string.difficulty_hard_title),
                subtitle = getString(R.string.difficulty_hard_subtitle),
                gridLabel = "7x7",
                levelCount = LevelRepository.HARD_LEVEL_COUNT,
                levelRange = hardRange
            )
        )
    }

    private fun refreshHeader() {
        val totalLevels = LevelRepository.getTotalLevels().coerceAtLeast(1)
        val highestUnlocked = progressManager.getHighestUnlockedLevel().coerceIn(1, totalLevels)
        val completedCount = progressManager.getCompletedLevels()
            .mapNotNull { it.toIntOrNull() }
            .count { it in 1..totalLevels }
        val totalStars = progressManager.getTotalStars()

        titleTextView.text = getString(R.string.level_map_title_format, highestUnlocked, totalLevels)
        subtitleTextView.text = getString(
            R.string.level_map_subtitle_format,
            completedCount,
            totalLevels,
            totalStars
        )
    }

    private fun bindDifficultyCards() {
        bindDifficultyCard(easyCard, packs[0])
        bindDifficultyCard(mediumCard, packs[1])
        bindDifficultyCard(hardCard, packs[2])
    }

    private fun bindDifficultyCard(card: AppCompatButton, pack: DifficultyPack) {
        val completedInPack = countCompletedLevels(pack.levelRange)
        val starsInPack = countStarsInRange(pack.levelRange)
        val unlocked = isPackUnlocked(pack)

        val lockHint = if (unlocked) {
            getString(R.string.difficulty_card_open_hint)
        } else {
            getString(R.string.difficulty_card_locked_hint, pack.levelRange.first)
        }

        card.text = getString(
            R.string.difficulty_card_home_format,
            pack.title,
            pack.subtitle,
            pack.gridLabel,
            pack.levelCount,
            completedInPack,
            starsInPack,
            lockHint
        )

        card.alpha = if (unlocked) 1f else 0.82f
        card.setOnClickListener {
            if (!unlocked) {
                card.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            } else {
                card.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }

            openDifficultyPage(pack, card)
        }
    }

    private fun animateCardsOnEntry() {
        val cards = listOf(easyCard, mediumCard, hardCard)
        cards.forEachIndexed { index, card ->
            card.alpha = 0f
            card.translationY = 36f
            card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(320)
                .setStartDelay(80L + (index * 70L))
                .setInterpolator(OvershootInterpolator(0.92f))
                .start()
        }
    }

    private fun openDifficultyPage(pack: DifficultyPack, sourceView: View) {
        sourceView.animate().cancel()
        sourceView.animate()
            .scaleX(0.97f)
            .scaleY(0.97f)
            .setDuration(65)
            .withEndAction {
                sourceView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .withEndAction {
                        startActivity(
                            Intent(this, DifficultyLevelsActivity::class.java).apply {
                                putExtra(DifficultyLevelsActivity.EXTRA_DIFFICULTY_KEY, pack.key)
                            }
                        )
                    }
                    .start()
            }
            .start()
    }

    private fun countCompletedLevels(range: IntRange): Int {
        return progressManager.getCompletedLevels()
            .mapNotNull { it.toIntOrNull() }
            .count { it in range }
    }

    private fun countStarsInRange(range: IntRange): Int {
        var total = 0
        for (levelId in range) {
            total += progressManager.getLevelStars(levelId)
        }
        return total
    }

    private fun isPackUnlocked(pack: DifficultyPack): Boolean {
        return progressManager.getHighestUnlockedLevel() >= pack.levelRange.first
    }
}
