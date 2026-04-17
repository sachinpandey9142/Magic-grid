package com.magicgrid.game

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.magicgrid.R
import com.magicgrid.controller.GameManager
import com.magicgrid.data.LevelRepository
import com.magicgrid.data.ProgressManager
import com.magicgrid.data.StarManager
import com.magicgrid.ui.GameView
import com.magicgrid.ui.WinDialogFragment

/**
 * Main game activity - where the actual gameplay happens
 */
class GameActivity : AppCompatActivity(), GameManager.GameStateListener {
    companion object {
        private const val KEY_LEVEL_ID = "key_level_id"
        private const val KEY_HINTS_USED = "key_hints_used"
        private const val KEY_MANAGER_STATE = "key_manager_state"
    }

    private lateinit var gameView: GameView
    private lateinit var gameManager: GameManager
    private lateinit var progressManager: ProgressManager
    private lateinit var starManager: StarManager

    // UI elements
    private lateinit var levelTitle: TextView
    private lateinit var moveCounter: TextView
    private lateinit var timerText: TextView
    private lateinit var starTargetOne: TextView
    private lateinit var starTargetTwo: TextView
    private lateinit var starTargetThree: TextView
    private lateinit var resetButton: Button
    private lateinit var undoButton: Button
    private lateinit var hintButton: Button
    private lateinit var backButton: Button
    private lateinit var topBackButton: Button
    private lateinit var pauseButton: Button
    
    // Game state
    private var levelId = 1
    private var hintsUsed = 0
    private var isPaused = false

    // Timer
    private val timerRunnable = object : Runnable {
        override fun run() {
            updateTimer()
            if (!isPaused && !gameManager.isGameWon()) {
                gameView.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        progressManager = ProgressManager(this)
        starManager = StarManager()

        // Get level ID from intent
        levelId = savedInstanceState?.getInt(KEY_LEVEL_ID)
            ?: intent.getIntExtra("level_id", progressManager.getCurrentLevel())

        if (!progressManager.isLevelUnlocked(levelId)) {
            levelId = progressManager.getHighestUnlockedLevel()
        }

        val level = LevelRepository.getLevel(levelId)
            ?: return // Level not found

        // Initialize game manager
        gameManager = GameManager(level)
        gameManager.addListener(this)

        // Setup UI
        gameView = findViewById(R.id.game_view)
        gameView.setGameManager(gameManager)

        levelTitle = findViewById(R.id.level_title)
        moveCounter = findViewById(R.id.move_counter)
        timerText = findViewById(R.id.timer_text)
        starTargetOne = findViewById(R.id.target_text_1)
        starTargetTwo = findViewById(R.id.target_text_2)
        starTargetThree = findViewById(R.id.target_text_3)
        resetButton = findViewById(R.id.btn_reset)
        undoButton = findViewById(R.id.btn_undo)
        hintButton = findViewById(R.id.btn_hint)
        backButton = findViewById(R.id.btn_menu)
        topBackButton = findViewById(R.id.btn_back_top)
        pauseButton = findViewById(R.id.btn_pause)

        // Restore state after process death / configuration change.
        hintsUsed = savedInstanceState?.getInt(KEY_HINTS_USED, 0) ?: 0
        restoreManagerState(savedInstanceState)
        isPaused = gameManager.isPaused()

        // Set up title
        levelTitle.text = "Level ${level.id}: ${level.name}"
        updateStarTargetLabels()

        // Set up buttons
        resetButton.setOnClickListener {
            if (isPaused) return@setOnClickListener
            provideControlFeedback(resetButton)
            gameManager.resetGame()
            hintsUsed = 0
            updateUI()
        }

        undoButton.setOnClickListener {
            if (isPaused) return@setOnClickListener
            provideControlFeedback(undoButton)
            if (gameManager.undo()) {
                updateUI()
            }
        }

        hintButton.setOnClickListener {
            if (isPaused) return@setOnClickListener
            provideControlFeedback(hintButton)
            hintsUsed++
            progressManager.incrementHintsUsed(levelId)
            showHint()
        }

        backButton.setOnClickListener {
            provideControlFeedback(backButton)
            navigateToMenu()
        }

        topBackButton.setOnClickListener {
            provideControlFeedback(topBackButton)
            navigateToMenu()
        }

        pauseButton.setOnClickListener {
            provideControlFeedback(pauseButton)
            togglePause()
        }

        progressManager.setCurrentLevel(levelId)
        applyPauseUiState()

        if (!isPaused && !gameManager.isGameWon()) {
            startTimerLoop()
        }

        updateUI()
    }

    /**
     * Update UI with current game state
     */
    private fun updateUI() {
        val state = gameManager.getGameState()
        val level = gameManager.level
        
        levelTitle.text = "Level ${level.id}: ${level.name}"
        moveCounter.text = "${state.moveCount} moves"
        updateTimer()
        applyPauseUiState()
    }

    private fun updateStarTargetLabels() {
        val targetTime = starManager.getTargetTime(levelId)
        starTargetOne.text = "Complete"
        starTargetTwo.text = "<= ${starManager.formatTime(targetTime)}"
        starTargetThree.text = "No hints"
    }

    /**
     * Update timer display
     */
    private fun updateTimer() {
        val seconds = gameManager.getElapsedTimeSeconds()
        val minutes = seconds / 60
        val secondsRemaining = seconds % 60
        timerText.text = String.format("%02d:%02d", minutes, secondsRemaining)
    }

    /**
     * Show a hint to the player
     */
    private fun showHint() {
        val remainingColors = gameManager.getRemainingColors()
        if (remainingColors.isEmpty()) {
            showToast("All paths complete!")
            return
        }

        // Highlight the first remaining color
        val hintColor = remainingColors[0]
        val path = gameManager.board.getPath(hintColor)
        if (path != null && !path.isComplete()) {
            val nextCell = if (path.cells.isEmpty()) {
                gameManager.board.getCell(path.startRow, path.startCol)
            } else {
                val lastCell = path.cells.last()
                // Find an adjacent valid cell
                var nextCell = gameManager.board.getCell(lastCell.row + 1, lastCell.col)
                if (nextCell == null || !gameManager.board.canAddToPath(hintColor, lastCell.row, lastCell.col, lastCell.row + 1, lastCell.col)) {
                    nextCell = gameManager.board.getCell(lastCell.row - 1, lastCell.col)
                    if (nextCell == null || !gameManager.board.canAddToPath(hintColor, lastCell.row, lastCell.col, lastCell.row - 1, lastCell.col)) {
                        nextCell = gameManager.board.getCell(lastCell.row, lastCell.col + 1)
                        if (nextCell == null || !gameManager.board.canAddToPath(hintColor, lastCell.row, lastCell.col, lastCell.row, lastCell.col + 1)) {
                            nextCell = gameManager.board.getCell(lastCell.row, lastCell.col - 1)
                        }
                    }
                }
                nextCell
            }

            if (nextCell != null) {
                showToast("Hint: Try ${nextCell.row}, ${nextCell.col} for color ${String.format("#%06X", (0xFFFFFF and hintColor))}")
            }
        }
    }

    /**
     * Called when game state changes
     */
    override fun onGameStateChanged() {
        updateUI()
    }

    /**
     * Called when game is won
     */
    override fun onGameWon() {
        stopTimerLoop()
        isPaused = false
        val state = gameManager.getGameState()
        
        // Calculate stars earned
        val targetTime = starManager.getTargetTime(levelId)
        val starsEarned = starManager.calculateStars(levelId, state.elapsedTimeSeconds, hintsUsed, targetTime)
        
        // Track completion with ProgressManager
        progressManager.completeLevel(levelId, state.moveCount, state.elapsedTimeSeconds)
        progressManager.saveLevelStars(levelId, starsEarned, hintsUsed)
        val nextLevelId = LevelRepository.getNextLevel(levelId)?.id ?: levelId
        progressManager.setCurrentLevel(nextLevelId)
        
        // Show win dialog with stars
        showWinDialog(starsEarned, state.elapsedTimeSeconds, state.moveCount)
    }

    /**
     * Show win dialog with animated stars
     */
    private fun showWinDialog(stars: Int, timeSeconds: Int, moves: Int) {
        val nextLevel = LevelRepository.getNextLevel(levelId)
        val hasNext = nextLevel != null
        
        WinDialogFragment()
            .setData(stars, timeSeconds, moves)
            .setCallbacks(
                onNext = {
                    if (hasNext) {
                        startActivity(Intent(this, GameActivity::class.java).apply {
                            putExtra("level_id", nextLevel?.id ?: 1)
                        })
                        finish()
                    } else {
                        showToast("Congratulations! You completed all levels!")
                        startActivity(Intent(this, MenuActivity::class.java))
                        finish()
                    }
                },
                onReplay = {
                    gameManager.resetGame()
                    hintsUsed = 0
                    isPaused = false
                    startTimerLoop()
                    updateUI()
                }
            )
            .show(supportFragmentManager, "win_dialog")
    }

    private fun restoreManagerState(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) return

        val restored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            savedInstanceState.getSerializable(KEY_MANAGER_STATE, GameManager.PersistedGameState::class.java)
        } else {
            @Suppress("DEPRECATION")
            savedInstanceState.getSerializable(KEY_MANAGER_STATE) as? GameManager.PersistedGameState
        }

        if (restored != null) {
            gameManager.restorePersistedState(restored)
        }
    }

    private fun togglePause() {
        if (gameManager.isGameWon()) return

        if (isPaused) {
            resumeGame(showMessage = true)
        } else {
            pauseGame(showMessage = true)
        }
    }

    private fun pauseGame(showMessage: Boolean) {
        if (!gameManager.pauseGame()) return

        isPaused = true
        stopTimerLoop()
        applyPauseUiState()
        if (showMessage) {
            showToast("Paused")
        }
    }

    private fun resumeGame(showMessage: Boolean) {
        if (!gameManager.resumeGame()) return

        isPaused = false
        startTimerLoop()
        applyPauseUiState()
        if (showMessage) {
            showToast("Resumed")
        }
    }

    private fun applyPauseUiState() {
        val interactive = !isPaused && !gameManager.isGameWon()

        gameView.setInputEnabled(interactive)
        gameView.alpha = if (interactive) 1f else 0.85f

        resetButton.isEnabled = interactive
        undoButton.isEnabled = interactive
        hintButton.isEnabled = interactive

        pauseButton.isEnabled = !gameManager.isGameWon()
        pauseButton.text = if (isPaused) "▶" else "⏸"
    }

    private fun startTimerLoop() {
        stopTimerLoop()
        gameView.postDelayed(timerRunnable, 1000)
    }

    private fun stopTimerLoop() {
        gameView.removeCallbacks(timerRunnable)
    }

    private fun navigateToMenu() {
        startActivity(Intent(this, MenuActivity::class.java))
        finish()
    }

    private fun provideControlFeedback(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(KEY_LEVEL_ID, levelId)
        outState.putInt(KEY_HINTS_USED, hintsUsed)
        outState.putSerializable(KEY_MANAGER_STATE, gameManager.capturePersistedState())
    }

    override fun onPause() {
        super.onPause()
        if (!isFinishing && !gameManager.isGameWon() && !isPaused) {
            pauseGame(showMessage = false)
        }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimerLoop()
    }
}
