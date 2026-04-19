package com.magicgrid.game

import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.ImageView
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
        val levelRange: IntRange,
        val iconRes: Int,
        val cardBackgroundRes: Int
    )

    private data class DifficultyCardViews(
        val root: View,
        val icon: ImageView,
        val state: TextView,
        val title: TextView,
        val subtitle: TextView,
        val levelsChip: TextView,
        val progressChip: TextView,
        val starsChip: TextView,
        val action: TextView
    )

    private lateinit var progressManager: ProgressManager
    private lateinit var titleTextView: TextView
    private lateinit var subtitleTextView: TextView
    private lateinit var easyCard: DifficultyCardViews
    private lateinit var mediumCard: DifficultyCardViews
    private lateinit var hardCard: DifficultyCardViews
    private lateinit var packs: List<DifficultyPack>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_level_select)

        progressManager = ProgressManager(this)
        titleTextView = findViewById(R.id.level_select_title)
        subtitleTextView = findViewById(R.id.level_select_subtitle)
        easyCard = createCardViews(R.id.card_easy)
        mediumCard = createCardViews(R.id.card_medium)
        hardCard = createCardViews(R.id.card_hard)

        packs = buildDifficultyPacks()

        attachPressAnimation(easyCard.root)
        attachPressAnimation(mediumCard.root)
        attachPressAnimation(hardCard.root)

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
                levelRange = easyRange,
                iconRes = R.drawable.ic_play,
                cardBackgroundRes = R.drawable.difficulty_card_easy
            ),
            DifficultyPack(
                key = DifficultyLevelsActivity.DIFFICULTY_MEDIUM,
                title = getString(R.string.difficulty_medium_title),
                subtitle = getString(R.string.difficulty_medium_subtitle),
                gridLabel = "6x6",
                levelCount = LevelRepository.MEDIUM_LEVEL_COUNT,
                levelRange = mediumRange,
                iconRes = R.drawable.ic_grid,
                cardBackgroundRes = R.drawable.difficulty_card_medium
            ),
            DifficultyPack(
                key = DifficultyLevelsActivity.DIFFICULTY_HARD,
                title = getString(R.string.difficulty_hard_title),
                subtitle = getString(R.string.difficulty_hard_subtitle),
                gridLabel = "7x7",
                levelCount = LevelRepository.HARD_LEVEL_COUNT,
                levelRange = hardRange,
                iconRes = R.drawable.ic_star,
                cardBackgroundRes = R.drawable.difficulty_card_hard
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

        titleTextView.text = getString(R.string.difficulty_hub_title)
        subtitleTextView.text = getString(
            R.string.difficulty_hub_summary_format,
            completedCount,
            totalLevels,
            totalStars,
            highestUnlocked
        )
    }

    private fun bindDifficultyCards() {
        bindDifficultyCard(easyCard, packs[0])
        bindDifficultyCard(mediumCard, packs[1])
        bindDifficultyCard(hardCard, packs[2])
    }

    private fun bindDifficultyCard(card: DifficultyCardViews, pack: DifficultyPack) {
        val completedInPack = countCompletedLevels(pack.levelRange)
        val starsInPack = countStarsInRange(pack.levelRange)
        val unlocked = isPackUnlocked(pack)

        card.root.setBackgroundResource(pack.cardBackgroundRes)
        card.icon.setImageResource(pack.iconRes)
        card.title.text = pack.title
        card.subtitle.text = pack.subtitle
        card.levelsChip.text = getString(
            R.string.difficulty_chip_levels_format,
            pack.levelCount,
            pack.gridLabel
        )
        card.progressChip.text = getString(
            R.string.difficulty_chip_progress_format,
            completedInPack,
            pack.levelCount
        )
        card.starsChip.text = getString(
            R.string.difficulty_chip_stars_format,
            starsInPack
        )

        card.state.text = if (unlocked) {
            getString(R.string.difficulty_card_state_unlocked)
        } else {
            getString(R.string.difficulty_card_state_locked)
        }
        card.state.setBackgroundResource(
            if (unlocked) {
                R.drawable.difficulty_state_chip_unlocked
            } else {
                R.drawable.difficulty_state_chip_locked
            }
        )

        card.action.text = if (unlocked) {
            getString(R.string.difficulty_card_action_open)
        } else {
            getString(R.string.difficulty_card_action_preview)
        }
        card.action.alpha = if (unlocked) 1f else 0.9f

        card.root.alpha = if (unlocked) 1f else 0.9f
        card.root.setOnClickListener {
            if (!unlocked) {
                card.root.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            } else {
                card.root.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }

            openDifficultyPage(pack, card.root)
        }
    }

    private fun animateCardsOnEntry() {
        val cards = listOf(easyCard.root, mediumCard.root, hardCard.root)
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

    private fun createCardViews(rootId: Int): DifficultyCardViews {
        val root = findViewById<View>(rootId)
        return DifficultyCardViews(
            root = root,
            icon = root.findViewById(R.id.card_icon),
            state = root.findViewById(R.id.card_state),
            title = root.findViewById(R.id.card_title),
            subtitle = root.findViewById(R.id.card_subtitle),
            levelsChip = root.findViewById(R.id.card_chip_levels),
            progressChip = root.findViewById(R.id.card_chip_progress),
            starsChip = root.findViewById(R.id.card_chip_stars),
            action = root.findViewById(R.id.card_action)
        )
    }

    private fun attachPressAnimation(view: View) {
        view.setOnTouchListener { target, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    target.animate()
                        .scaleX(0.985f)
                        .scaleY(0.985f)
                        .setDuration(80)
                        .start()
                    true
                }

                MotionEvent.ACTION_UP -> {
                    target.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120)
                        .start()
                    target.performClick()
                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    target.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120)
                        .start()
                    true
                }

                else -> false
            }
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
        return progressManager.isLevelUnlocked(pack.levelRange.first)
    }
}
