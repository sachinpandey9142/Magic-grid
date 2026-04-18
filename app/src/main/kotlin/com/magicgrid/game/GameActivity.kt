package com.magicgrid.game

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.magicgrid.R
import com.magicgrid.controller.GameManager
import com.magicgrid.data.LevelRepository
import com.magicgrid.data.ProgressManager
import com.magicgrid.data.StarManager
import com.magicgrid.ui.GameView
import com.magicgrid.ui.WinDialogFragment
import kotlin.math.abs

/**
 * Main game activity - where the actual gameplay happens
 */
class GameActivity : AppCompatActivity(), GameManager.GameStateListener {
    companion object {
        private const val KEY_LEVEL_ID = "key_level_id"
        private const val KEY_HINTS_USED = "key_hints_used"
        private const val KEY_MANAGER_STATE = "key_manager_state"
        private const val HINT_COIN_COST = 20
        private const val HINT_ROUTE_MAX_STEPS = 3
        private const val HINT_ROUTE_MIN_STEPS = 2
    }

    private lateinit var gameView: GameView
    private lateinit var gameManager: GameManager
    private lateinit var progressManager: ProgressManager
    private lateinit var starManager: StarManager

    // UI elements
    private lateinit var levelTitle: TextView
    private lateinit var moveCounter: TextView
    private lateinit var timerText: TextView
    private lateinit var coinText: TextView
    private lateinit var starTargetOne: TextView
    private lateinit var starTargetTwo: TextView
    private lateinit var starTargetThree: TextView
    private lateinit var starProgressLineFill: View
    private lateinit var starStepThree: View
    private lateinit var starStepTwo: View
    private lateinit var starStepOne: View
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
    private var starTargetTimeSeconds = 0
    private var liveStarTier = 3
    private var starHudInitialized = false

    private data class HintSuggestion(
        val color: Int,
        val route: List<Pair<Int, Int>>
    )

    private data class HintCandidate(
        val suggestion: HintSuggestion,
        val stepCount: Int,
        val endDistance: Int,
        val improvement: Int
    )

    // Timer
    private val timerRunnable = object : Runnable {
        override fun run() {
            updateTimer()
            updateLiveStarHud(animated = true)
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
        coinText = findViewById(R.id.coin_text)
        starTargetOne = findViewById(R.id.target_text_1)
        starTargetTwo = findViewById(R.id.target_text_2)
        starTargetThree = findViewById(R.id.target_text_3)
        starProgressLineFill = findViewById(R.id.star_progress_line_fill)
        starStepThree = findViewById(R.id.star_step_three_group)
        starStepTwo = findViewById(R.id.star_step_two_group)
        starStepOne = findViewById(R.id.star_step_one_group)
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
        updateCoinDisplay()
        starProgressLineFill.pivotX = 0f
        updateLiveStarHud(animated = false)

        // Set up buttons
        resetButton.setOnClickListener {
            if (isPaused) return@setOnClickListener
            provideControlFeedback(resetButton)
            gameManager.resetGame()
            hintsUsed = 0
            updateLiveStarHud(animated = true)
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

            if (gameManager.getRemainingColors().isEmpty()) {
                showToast("All paths complete!")
                return@setOnClickListener
            }

            if (findHintSuggestion() == null) {
                showToast(getString(R.string.hint_no_move_available))
                return@setOnClickListener
            }

            val currentCoins = progressManager.getCoins()
            if (currentCoins < HINT_COIN_COST) {
                hintButton.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                showToast(getString(R.string.hint_not_enough_coins, HINT_COIN_COST))
                return@setOnClickListener
            }

            showHintPurchaseDialog(currentCoins)
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
        updateCoinDisplay()
        updateLiveStarHud(animated = false)
        applyPauseUiState()
    }

    private fun updateCoinDisplay() {
        coinText.text = progressManager.getCoins().toString()
    }

    private fun updateStarTargetLabels() {
        starTargetTimeSeconds = starManager.getTargetTime(levelId)
        val targetTimeLabel = starManager.formatTime(starTargetTimeSeconds)

        // Left-to-right track: 3 stars -> 2 stars -> 1 star
        starTargetThree.text = "No hint + <= $targetTimeLabel"
        starTargetTwo.text = "Hint OR > $targetTimeLabel"
        starTargetOne.text = "Hint + > $targetTimeLabel"
    }

    private fun updateLiveStarHud(animated: Boolean) {
        val targetTier = calculateLiveStarTier()
        val tierChanged = targetTier != liveStarTier

        if (!tierChanged && starHudInitialized) {
            return
        }

        liveStarTier = targetTier
        val lineScale = when (liveStarTier) {
            3 -> 0.04f
            2 -> 0.52f
            else -> 1f
        }

        if (animated && starHudInitialized) {
            starProgressLineFill.animate().cancel()
            starProgressLineFill.animate()
                .scaleX(lineScale)
                .setDuration(320)
                .start()
        } else {
            starProgressLineFill.scaleX = lineScale
        }

        applyStarStepVisual(starStepThree, isMuted = liveStarTier < 3, animated = animated)
        applyStarStepVisual(starStepTwo, isMuted = liveStarTier < 2, animated = animated)
        applyStarStepVisual(starStepOne, isMuted = false, animated = animated)

        starHudInitialized = true
    }

    private fun calculateLiveStarTier(): Int {
        val usedHint = hintsUsed > 0
        val exceededTime = gameManager.getElapsedTimeSeconds() > starTargetTimeSeconds

        return when {
            usedHint && exceededTime -> 1
            usedHint || exceededTime -> 2
            else -> 3
        }
    }

    private fun applyStarStepVisual(starView: View, isMuted: Boolean, animated: Boolean) {
        val targetAlpha = if (isMuted) 0.36f else 1f

        if (animated && starHudInitialized) {
            starView.animate().cancel()
            starView.animate()
                .alpha(targetAlpha)
                .setDuration(260)
                .start()
        } else {
            starView.alpha = targetAlpha
        }
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
    private fun showHint(hintSuggestion: HintSuggestion) {
        gameView.showHintRoute(hintSuggestion.route, hintSuggestion.color)
    }

    private fun findHintSuggestion(): HintSuggestion? {
        val remainingColors = gameManager.getRemainingColors()
        if (remainingColors.isEmpty()) return null

        val candidates = mutableListOf<HintCandidate>()

        for (color in remainingColors) {
            val path = gameManager.board.getPath(color) ?: continue
            if (path.isComplete()) continue

            val blockedOwnPath = path.cells
                .map { it.row to it.col }
                .toSet()

            if (path.cells.isEmpty()) {
                buildHintCandidate(
                    color = color,
                    anchorRow = path.startRow,
                    anchorCol = path.startCol,
                    targetRow = path.endRow,
                    targetCol = path.endCol,
                    blockedOwnPath = blockedOwnPath
                )?.let { candidates.add(it) }

                buildHintCandidate(
                    color = color,
                    anchorRow = path.endRow,
                    anchorCol = path.endCol,
                    targetRow = path.startRow,
                    targetCol = path.startCol,
                    blockedOwnPath = blockedOwnPath
                )?.let { candidates.add(it) }

                continue
            }

            val headCell = if (path.isHeadAtEnd) {
                path.cells.last()
            } else {
                path.cells.first()
            }

            val targetEndpoint = if (path.isHeadAtEnd) {
                path.startRow to path.startCol
            } else {
                path.endRow to path.endCol
            }

            buildHintCandidate(
                color = color,
                anchorRow = headCell.row,
                anchorCol = headCell.col,
                targetRow = targetEndpoint.first,
                targetCol = targetEndpoint.second,
                blockedOwnPath = blockedOwnPath
            )?.let { candidates.add(it) }
        }

        if (candidates.isEmpty()) return null

        val preferred = candidates
            .filter { it.stepCount >= HINT_ROUTE_MIN_STEPS }
            .ifEmpty { candidates }

        return preferred.maxWithOrNull(::compareHintCandidates)?.suggestion
    }

    private fun buildHintCandidate(
        color: Int,
        anchorRow: Int,
        anchorCol: Int,
        targetRow: Int,
        targetCol: Int,
        blockedOwnPath: Set<Pair<Int, Int>>
    ): HintCandidate? {
        val route = findBestHintRoute(
            color = color,
            anchorRow = anchorRow,
            anchorCol = anchorCol,
            targetRow = targetRow,
            targetCol = targetCol,
            blockedOwnPath = blockedOwnPath
        ) ?: return null

        if (route.size < 2) return null

        val start = route.first()
        val end = route.last()
        val startDistance = manhattanDistance(start.first, start.second, targetRow, targetCol)
        val endDistance = manhattanDistance(end.first, end.second, targetRow, targetCol)

        return HintCandidate(
            suggestion = HintSuggestion(color = color, route = route),
            stepCount = route.size - 1,
            endDistance = endDistance,
            improvement = startDistance - endDistance
        )
    }

    private fun findBestHintRoute(
        color: Int,
        anchorRow: Int,
        anchorCol: Int,
        targetRow: Int,
        targetCol: Int,
        blockedOwnPath: Set<Pair<Int, Int>>
    ): List<Pair<Int, Int>>? {
        val target = targetRow to targetCol
        val start = anchorRow to anchorCol
        val route = mutableListOf(start)
        val visited = mutableSetOf(start)
        val routeCandidates = mutableListOf<List<Pair<Int, Int>>>()

        fun dfs(currentRow: Int, currentCol: Int, steps: Int) {
            if (steps > 0) {
                routeCandidates.add(route.toList())
            }

            if (steps == HINT_ROUTE_MAX_STEPS) return

            val directions = listOf(
                1 to 0,
                -1 to 0,
                0 to 1,
                0 to -1
            )

            for ((rowDelta, colDelta) in directions) {
                val nextRow = currentRow + rowDelta
                val nextCol = currentCol + colDelta
                val next = nextRow to nextCol

                if (!isHintStepAllowed(
                        color = color,
                        fromRow = currentRow,
                        fromCol = currentCol,
                        toRow = nextRow,
                        toCol = nextCol,
                        blockedOwnPath = blockedOwnPath,
                        localVisited = visited,
                        target = target
                    )
                ) {
                    continue
                }

                route.add(next)

                if (next == target) {
                    routeCandidates.add(route.toList())
                    route.removeAt(route.lastIndex)
                    continue
                }

                visited.add(next)
                dfs(nextRow, nextCol, steps + 1)
                visited.remove(next)
                route.removeAt(route.lastIndex)
            }
        }

        dfs(anchorRow, anchorCol, 0)

        if (routeCandidates.isEmpty()) return null

        val preferred = routeCandidates
            .filter { it.size - 1 >= HINT_ROUTE_MIN_STEPS }
            .ifEmpty { routeCandidates }

        return preferred.maxWithOrNull { first, second ->
            compareHintRoutes(first, second, target)
        }
    }

    private fun isHintStepAllowed(
        color: Int,
        fromRow: Int,
        fromCol: Int,
        toRow: Int,
        toCol: Int,
        blockedOwnPath: Set<Pair<Int, Int>>,
        localVisited: Set<Pair<Int, Int>>,
        target: Pair<Int, Int>
    ): Boolean {
        val targetCell = gameManager.board.getCell(toRow, toCol) ?: return false
        val candidate = toRow to toCol

        val rowDiff = abs(toRow - fromRow)
        val colDiff = abs(toCol - fromCol)
        if (rowDiff + colDiff != 1) return false

        if (targetCell.pathColor != null && targetCell.pathColor != color) return false
        if (targetCell.isEndpoint && targetCell.color != color) return false

        if (candidate != target) {
            if (candidate in blockedOwnPath) return false
            if (candidate in localVisited) return false
        }

        return true
    }

    private fun compareHintRoutes(
        first: List<Pair<Int, Int>>,
        second: List<Pair<Int, Int>>,
        target: Pair<Int, Int>
    ): Int {
        val firstImprovement = routeImprovement(first, target)
        val secondImprovement = routeImprovement(second, target)
        if (firstImprovement != secondImprovement) {
            return firstImprovement.compareTo(secondImprovement)
        }

        val firstDistance = routeDistanceToTarget(first, target)
        val secondDistance = routeDistanceToTarget(second, target)
        if (firstDistance != secondDistance) {
            return secondDistance.compareTo(firstDistance)
        }

        val firstSteps = first.size - 1
        val secondSteps = second.size - 1
        if (firstSteps != secondSteps) {
            return firstSteps.compareTo(secondSteps)
        }

        return 0
    }

    private fun compareHintCandidates(first: HintCandidate, second: HintCandidate): Int {
        if (first.improvement != second.improvement) {
            return first.improvement.compareTo(second.improvement)
        }

        if (first.endDistance != second.endDistance) {
            return second.endDistance.compareTo(first.endDistance)
        }

        if (first.stepCount != second.stepCount) {
            return first.stepCount.compareTo(second.stepCount)
        }

        return 0
    }

    private fun routeImprovement(route: List<Pair<Int, Int>>, target: Pair<Int, Int>): Int {
        val start = route.first()
        val end = route.last()
        val startDistance = manhattanDistance(start.first, start.second, target.first, target.second)
        val endDistance = manhattanDistance(end.first, end.second, target.first, target.second)
        return startDistance - endDistance
    }

    private fun routeDistanceToTarget(route: List<Pair<Int, Int>>, target: Pair<Int, Int>): Int {
        val end = route.last()
        return manhattanDistance(end.first, end.second, target.first, target.second)
    }

    private fun manhattanDistance(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Int {
        return abs(toRow - fromRow) + abs(toCol - fromCol)
    }

    private fun showHintPurchaseDialog(currentCoins: Int) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.hint_purchase_title))
            .setMessage(getString(R.string.hint_purchase_message, HINT_COIN_COST, currentCoins))
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.buy_hint) { _, _ ->
                purchaseHint()
            }
            .show()
    }

    private fun purchaseHint() {
        val hintSuggestion = findHintSuggestion()
        if (hintSuggestion == null) {
            showToast(getString(R.string.hint_no_move_available))
            return
        }

        if (!progressManager.spendCoins(HINT_COIN_COST)) {
            hintButton.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            updateCoinDisplay()
            showToast(getString(R.string.hint_not_enough_coins, HINT_COIN_COST))
            return
        }

        provideControlFeedback(hintButton)
        hintsUsed++
        progressManager.incrementHintsUsed(levelId)
        updateCoinDisplay()
        updateLiveStarHud(animated = true)
        showHint(hintSuggestion)
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
        val coinsEarned = progressManager.grantCoinsForLevelResult(levelId, starsEarned)
        val nextLevelId = LevelRepository.getNextLevel(levelId)?.id ?: levelId
        progressManager.setCurrentLevel(nextLevelId)
        updateCoinDisplay()

        if (coinsEarned > 0) {
            showToast(getString(R.string.coins_earned_format, coinsEarned))
        }
        
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
                    updateLiveStarHud(animated = false)
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
