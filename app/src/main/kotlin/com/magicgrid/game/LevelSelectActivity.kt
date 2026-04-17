package com.magicgrid.game

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.GridLayout
import android.widget.Button
import android.widget.TextView
import com.magicgrid.R
import com.magicgrid.data.LevelRepository
import com.magicgrid.data.ProgressManager

/**
 * Level selection screen - shows all available levels
 */
class LevelSelectActivity : AppCompatActivity() {
    private lateinit var progressManager: ProgressManager
    private lateinit var levelGrid: GridLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_level_select)

        progressManager = ProgressManager(this)
        levelGrid = findViewById(R.id.level_grid)

        // Get title
        val titleTextView = findViewById<TextView>(R.id.level_select_title)
        val totalLevels = LevelRepository.getTotalLevels()
        val highestUnlocked = progressManager.getHighestUnlockedLevel()
        titleTextView.text = "Select Level ($highestUnlocked/$totalLevels unlocked)"

        // Populate level buttons
        populateLevels()

        // Back button (if implemented)
        val backButton = findViewById<Button>(R.id.btn_back)
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun populateLevels() {
        val levels = LevelRepository.getAllLevels()
        val completedLevels = progressManager.getCompletedLevels()

        levelGrid.removeAllViews()

        for (level in levels) {
            val levelButton = Button(this)
            val isUnlocked = progressManager.isLevelUnlocked(level.id)
            val isCompleted = completedLevels.contains(level.id.toString())

            levelButton.text = if (isUnlocked) {
                "Level ${level.id}\n${level.getDifficulty()}"
            } else {
                "Level ${level.id}\nLOCKED"
            }
            levelButton.textSize = 12f
            levelButton.layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(8, 8, 8, 8)
            }

            // Style by state: completed, unlocked, or locked.
            when {
                isCompleted -> {
                    levelButton.setBackgroundColor(getColor(R.color.level_completed))
                    levelButton.isEnabled = true
                    levelButton.alpha = 1f
                }
                isUnlocked -> {
                    levelButton.setBackgroundColor(getColor(R.color.level_available))
                    levelButton.isEnabled = true
                    levelButton.alpha = 1f
                }
                else -> {
                    levelButton.setBackgroundColor(getColor(R.color.level_locked))
                    levelButton.isEnabled = false
                    levelButton.alpha = 0.75f
                }
            }

            if (isUnlocked) {
                levelButton.setOnClickListener {
                    progressManager.setCurrentLevel(level.id)
                    startGameActivity(level.id)
                }
            } else {
                levelButton.setOnClickListener(null)
            }

            levelGrid.addView(levelButton)
        }
    }

    private fun startGameActivity(levelId: Int) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("level_id", levelId)
        startActivity(intent)
    }
}
