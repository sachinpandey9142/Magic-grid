package com.magicgrid.controller

import com.magicgrid.data.Level
import com.magicgrid.logic.GameBoard
import java.io.Serializable
import kotlin.math.abs

/**
 * Central game controller. Manages game logic, state, and communication between layers.
 * Responsible for:
 * - Initializing the board for a level
 * - Handling user input
 * - Managing game state and progression
 * - Checking win conditions
 */
class GameManager(val level: Level) {
    companion object {
        private const val MAX_UNDO_HISTORY = 120
    }

    // Core game board
    val board: GameBoard = GameBoard(level.rows, level.cols)

    // Game state
    private var moveCount = 0
    private var accumulatedElapsedMs = 0L
    private var lastResumeTimeMs = System.currentTimeMillis()
    private var isPaused = false
    private var isGameWon = false
    private val stateHistory: MutableList<GameStateSnapshot> = mutableListOf()

    // Listeners for UI updates
    private val listeners: MutableList<GameStateListener> = mutableListOf()

    data class PersistedPathState(
        val color: Int,
        val cells: IntArray,
        val isLocked: Boolean,
        val isHeadAtEnd: Boolean
    ) : Serializable

    data class PersistedGameState(
        val moveCount: Int,
        val elapsedTimeMs: Long,
        val isGameWon: Boolean,
        val wasPaused: Boolean,
        val paths: List<PersistedPathState>
    ) : Serializable

    /**
     * Snapshot of board state for undo functionality
     */
    private data class PathSnapshot(
        val color: Int,
        val cells: List<Pair<Int, Int>>,
        val isLocked: Boolean,
        val isHeadAtEnd: Boolean
    )

    private data class GameStateSnapshot(
        val moveCount: Int,
        val elapsedTimeMs: Long,
        val isGameWon: Boolean,
        val paths: List<PathSnapshot>
    )

    init {
        // Initialize board with endpoints
        for (endpoint in level.endpoints) {
            board.setColorEndpoints(
                endpoint.color,
                Pair(endpoint.startRow, endpoint.startCol),
                Pair(endpoint.endRow, endpoint.endCol)
            )
        }
        saveState()
    }

    /**
     * Add a listener for game state changes
     */
    fun addListener(listener: GameStateListener) {
        listeners.add(listener)
    }

    /**
     * Remove a listener
     */
    fun removeListener(listener: GameStateListener) {
        listeners.remove(listener)
    }

    /**
     * Notify all listeners of state change
     */
    private fun notifyStateChange() {
        listeners.forEach { it.onGameStateChanged() }
    }

    /**
     * Notify listeners of win condition
     */
    private fun notifyGameWon() {
        listeners.forEach { it.onGameWon() }
    }

    /**
     * Notify listeners when a path is completed
     */
    private fun notifyPathCompleted(color: Int) {
        listeners.forEach { it.onPathCompleted(color) }
    }

    private fun lockAllPaths() {
        for (color in board.colors) {
            board.getPath(color)?.isLocked = true
        }
    }

    /**
     * Handle touch input - COMPLETELY FLEXIBLE path logic
     * Allows forward, backward, continuation from either end, and smooth reversibility
     */
    fun onCellSelected(color: Int, row: Int, col: Int): Boolean {
        if (isPaused || isGameWon) return false

        val path = board.getPath(color) ?: return false
        val targetCell = board.getCell(row, col) ?: return false

        // If path is locked (completed), don't allow further modifications
        if (path.isLocked) return false

        // CASE 1: PATH IS EMPTY - allow starting from either endpoint
        if (path.cells.isEmpty()) {
            if ((row == path.startRow && col == path.startCol) || (row == path.endRow && col == path.endCol)) {
                path.isHeadAtEnd = (row == path.endRow && col == path.endCol)
                val added = board.addCellToPath(color, row, col)
                if (added) notifyStateChange()
                return added
            }
            // Also allow starting from a pre-existing pathColor (defensive)
            if (targetCell.pathColor == color) {
                // Load existing path as resume
                notifyStateChange()
                return true
            }
            return false
        }

        val indexInPath = path.cells.indexOfFirst { it.row == row && it.col == col }

        // Completed paths can only be edited by touching existing path cells.
        // This prevents drawing out of an endpoint after the connection is made.
        if (path.isComplete() && indexInPath == -1) {
            return false
        }

        // If user touched a cell already in this path -> treat as resume/backtrack
        if (indexInPath != -1) {
            // If touching the active head, nothing to do
            val headIndex = if (path.isHeadAtEnd) path.cells.size - 1 else 0
            if (indexInPath == headIndex) return true

            // If head at end, remove everything after touched index
            if (path.isHeadAtEnd) {
                if (indexInPath < path.cells.size - 1) {
                    board.removeCellsAfterIndex(color, indexInPath)
                    notifyStateChange()
                }
                return true
            } else {
                // Head at start: remove everything before touched index
                if (indexInPath > 0) {
                    board.removeCellsBeforeIndex(color, indexInPath)
                    notifyStateChange()
                }
                return true
            }
        }

        // Not in path: validate adjacency to current head
        val currentHeadCell = if (path.isHeadAtEnd) path.cells.last() else path.cells.first()
        val rowDiff = abs(row - currentHeadCell.row)
        val colDiff = abs(col - currentHeadCell.col)
        if (!((rowDiff == 1 && colDiff == 0) || (rowDiff == 0 && colDiff == 1))) return false

        // CASE: overlap with other colors
        if (targetCell.pathColor != null && targetCell.pathColor != color) return false
        if (targetCell.isEndpoint && targetCell.color != color) return false

        // CASE: reaching the other endpoint -> complete and lock
        val otherEndpoint = if (path.isHeadAtEnd) Pair(path.startRow, path.startCol) else Pair(path.endRow, path.endCol)
        if (row == otherEndpoint.first && col == otherEndpoint.second && path.cells.isNotEmpty()) {
            val added = if (path.isHeadAtEnd) {
                board.addCellToPath(color, row, col)
            } else {
                board.addCellToPathAtStart(color, row, col)
            }
            if (added) {
                moveCount++
                saveState()
                notifyPathCompleted(color)
                if (board.isWon()) {
                    isGameWon = true
                    lockAllPaths()
                    saveState()
                    notifyGameWon()
                } else {
                    notifyStateChange()
                }
                return true
            }
            return false
        }

        // NORMAL FORWARD MOVEMENT - add at head side
        val added = if (path.isHeadAtEnd) {
            board.addCellToPath(color, row, col)
        } else {
            board.addCellToPathAtStart(color, row, col)
        }
        if (added) notifyStateChange()
        return added
    }

    /**
     * Clear a path completely (reset button interaction)
     */
    fun resetPath(color: Int) {
        if (isPaused || isGameWon) return

        board.clearPath(color)
        board.getPath(color)?.also {
            it.isLocked = false
            it.isHeadAtEnd = false
        }
        moveCount = 0
        saveState()
        notifyStateChange()
    }

    /**
     * Reset the entire game
     */
    fun resetGame() {
        clearAllPathState()
        moveCount = 0
        accumulatedElapsedMs = 0L
        lastResumeTimeMs = System.currentTimeMillis()
        isPaused = false
        isGameWon = false
        stateHistory.clear()
        saveState()
        notifyStateChange()
    }

    fun pauseGame(): Boolean {
        if (isPaused) return false

        accumulatedElapsedMs += (System.currentTimeMillis() - lastResumeTimeMs)
        isPaused = true
        notifyStateChange()
        return true
    }

    fun resumeGame(): Boolean {
        if (!isPaused) return false

        lastResumeTimeMs = System.currentTimeMillis()
        isPaused = false
        notifyStateChange()
        return true
    }

    fun isPaused(): Boolean = isPaused

    /**
     * Undo to the previous state
     */
    fun undo(): Boolean {
        if (isGameWon) return false
        if (stateHistory.size <= 1) return false // Keep at least initial state

        stateHistory.removeAt(stateHistory.size - 1)
        val previousState = stateHistory.last()
        restoreState(previousState)
        notifyStateChange()
        return true
    }

    /**
     * Save current board state
     */
    private fun saveState() {
        if (stateHistory.size >= MAX_UNDO_HISTORY) {
            stateHistory.removeAt(0)
        }

        val pathSnapshots = board.colors.mapNotNull { color ->
            val path = board.getPath(color) ?: return@mapNotNull null
            PathSnapshot(
                color = color,
                cells = path.cells.map { Pair(it.row, it.col) },
                isLocked = path.isLocked,
                isHeadAtEnd = path.isHeadAtEnd
            )
        }

        stateHistory.add(
            GameStateSnapshot(
                moveCount = moveCount,
                elapsedTimeMs = getElapsedTimeMillis(),
                isGameWon = isGameWon,
                paths = pathSnapshots
            )
        )
    }

    /**
     * Restore board from saved state
     */
    private fun restoreState(snapshot: GameStateSnapshot) {
        clearAllPathState()

        for (pathSnapshot in snapshot.paths) {
            val path = board.getPath(pathSnapshot.color) ?: continue

            for ((row, col) in pathSnapshot.cells) {
                val cell = board.getCell(row, col) ?: continue
                path.addCell(cell)
                cell.pathColor = pathSnapshot.color
            }

            path.isLocked = pathSnapshot.isLocked
            path.isHeadAtEnd = pathSnapshot.isHeadAtEnd
        }

        moveCount = snapshot.moveCount
        accumulatedElapsedMs = snapshot.elapsedTimeMs
        lastResumeTimeMs = System.currentTimeMillis()
        isGameWon = snapshot.isGameWon
    }

    private fun clearAllPathState() {
        board.reset()

        for (r in 0 until board.rows) {
            for (c in 0 until board.cols) {
                board.getCell(r, c)?.pathColor = null
            }
        }

        for (color in board.colors) {
            board.getPath(color)?.also {
                it.clear()
                it.isLocked = false
                it.isHeadAtEnd = false
            }
        }
    }

    fun capturePersistedState(): PersistedGameState {
        val pathStates = board.colors.mapNotNull { color ->
            val path = board.getPath(color) ?: return@mapNotNull null
            val flattened = IntArray(path.cells.size * 2)

            path.cells.forEachIndexed { index, cell ->
                flattened[index * 2] = cell.row
                flattened[(index * 2) + 1] = cell.col
            }

            PersistedPathState(
                color = color,
                cells = flattened,
                isLocked = path.isLocked,
                isHeadAtEnd = path.isHeadAtEnd
            )
        }

        return PersistedGameState(
            moveCount = moveCount,
            elapsedTimeMs = getElapsedTimeMillis(),
            isGameWon = isGameWon,
            wasPaused = isPaused,
            paths = pathStates
        )
    }

    fun restorePersistedState(state: PersistedGameState) {
        clearAllPathState()

        for (pathState in state.paths) {
            val path = board.getPath(pathState.color) ?: continue

            val flattened = pathState.cells
            var i = 0
            while (i + 1 < flattened.size) {
                val row = flattened[i]
                val col = flattened[i + 1]
                val cell = board.getCell(row, col)
                if (cell != null) {
                    path.addCell(cell)
                    cell.pathColor = pathState.color
                }
                i += 2
            }

            path.isLocked = pathState.isLocked
            path.isHeadAtEnd = pathState.isHeadAtEnd
        }

        moveCount = state.moveCount.coerceAtLeast(0)
        accumulatedElapsedMs = state.elapsedTimeMs.coerceAtLeast(0L)
        lastResumeTimeMs = System.currentTimeMillis()
        isPaused = state.wasPaused
        isGameWon = state.isGameWon

        stateHistory.clear()
        saveState()
        notifyStateChange()
    }

    /**
     * Get current game state for UI
     */
    fun getGameState(): GameState {
        val elapsedSeconds = (getElapsedTimeMillis() / 1000L).toInt()
        return GameState(
            levelId = level.id,
            moveCount = moveCount,
            elapsedTimeSeconds = elapsedSeconds,
            isGameOver = isGameWon,
            won = isGameWon
        )
    }

    /**
     * Get move count
     */
    fun getMoveCount(): Int = moveCount

    /**
     * Get elapsed time in seconds
     */
    fun getElapsedTimeSeconds(): Int {
        return (getElapsedTimeMillis() / 1000L).toInt()
    }

    fun getElapsedTimeMillis(): Long {
        return if (isPaused) {
            accumulatedElapsedMs
        } else {
            accumulatedElapsedMs + (System.currentTimeMillis() - lastResumeTimeMs)
        }
    }

    /**
     * Check if game is won
     */
    fun isGameWon(): Boolean = isGameWon

    /**
     * Get board completion percentage
     */
    fun getCompletionPercentage(): Int = board.getCompletionPercentage()

    /**
     * Get list of colors that still need completion
     */
    fun getRemainingColors(): List<Int> {
        return board.colors.filter { color ->
            val isComplete = board.getPath(color)?.isComplete() ?: false
            !isComplete
        }
    }

    /**
     * Interface for listening to game state changes
     */
    interface GameStateListener {
        fun onGameStateChanged()
        fun onGameWon()
        fun onPathCompleted(color: Int) {
            // Optional callback for when a path is completed
        }
    }
}
