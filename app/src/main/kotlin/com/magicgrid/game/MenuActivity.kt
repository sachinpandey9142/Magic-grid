package com.magicgrid.game

import android.content.Intent
import android.os.Bundle
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

        playButton.setOnClickListener {
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
            startActivity(Intent(this, LevelSelectActivity::class.java))
        }

        settingsButton.setOnClickListener {
            // Settings screen (can be implemented later)
            showToast("Settings coming soon!")
        }

        exitButton.setOnClickListener {
            finish()
        }
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
