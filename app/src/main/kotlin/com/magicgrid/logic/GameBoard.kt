package com.magicgrid.logic

/**
 * Main game board logic. Manages grid state, cell validation, and path management.
 * No UI logic in this class - purely game mechanics.
 */
class GameBoard(
    val rows: Int,
    val cols: Int
) {
    // The actual grid
    private val grid: Array<Array<Cell>> = Array(rows) { row ->
        Array(cols) { col ->
            Cell(row, col)
        }
    }

    // Paths for each color (color -> Path)
    private val paths: MutableMap<Int, Path> = mutableMapOf()

    // List of all endpoint colors
    val colors: MutableList<Int> = mutableListOf()

    init {
        // Initialize grid
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                grid[r][c] = Cell(r, c)
            }
        }
    }

    /**
     * Get a cell at specified position
     */
    fun getCell(row: Int, col: Int): Cell? {
        return if (row in 0 until rows && col in 0 until cols) {
            grid[row][col]
        } else {
            null
        }
    }

    /**
     * Set an endpoint at a specific position
     */
    fun setEndpoint(row: Int, col: Int, color: Int) {
        if (row in 0 until rows && col in 0 until cols) {
            grid[row][col].color = color
            grid[row][col].isEndpoint = true
            if (!colors.contains(color)) {
                colors.add(color)
                paths[color] = Path(color, row, col, -1, -1)
            }
        }
    }

    /**
     * Set endpoint positions (for both endpoints of a color)
     */
    fun setColorEndpoints(color: Int, start: Pair<Int, Int>, end: Pair<Int, Int>) {
        setEndpoint(start.first, start.second, color)
        val endCell = getCell(end.first, end.second)
        endCell?.also {
            it.color = color
            it.isEndpoint = true
        }
        // Update the path with correct end position
        paths[color] = Path(color, start.first, start.second, end.first, end.second)
    }

    /**
     * Get the path for a specific color
     */
    fun getPath(color: Int): Path? = paths[color]

    /**
     * Create a new path from scratch or get existing one
     */
    fun getOrCreatePath(color: Int): Path? {
        return paths[color]
    }

    /**
     * Check if a cell can be added to a path (is it valid and adjacent?)
     */
    fun canAddToPath(color: Int, fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        val path = paths[color] ?: return false

        // Check bounds
        if (toRow !in 0 until rows || toCol !in 0 until cols) return false

        // Check if adjacent (only 4-way: up, down, left, right)
        val rowDiff = (toRow - fromRow).coerceIn(-1, 1)
        val colDiff = (toCol - fromCol).coerceIn(-1, 1)
        val isAdjacent = (rowDiff == 0 && colDiff != 0) || (rowDiff != 0 && colDiff == 0)
        if (!isAdjacent) return false

        val targetCell = grid[toRow][toCol]

        // Can't overlap with other paths (but can cross own color endpoints)
        if (targetCell.pathColor != null && targetCell.pathColor != color) return false

        // Can't pass through other color's endpoints (but can use its own)
        if (targetCell.isEndpoint && targetCell.color != color) return false

        // Can't revisit own path cells (except the endpoint)
        if (path.containsCell(toRow, toCol)) {
            // Only allow if it's the destination endpoint
            return toRow == path.endRow && toCol == path.endCol
        }

        return true
    }

    /**
     * Add a cell to the current path for a color
     */
    fun addCellToPath(color: Int, row: Int, col: Int): Boolean {
        val path = paths[color] ?: return false

        val cell = grid[row][col]
        path.addCell(cell)
        cell.pathColor = color

        return true
    }

    /**
     * Remove the last cell from a path
     */
    fun removeLastCellFromPath(color: Int): Boolean {
        val path = paths[color] ?: return false
        val removedCell = path.getLastCell()
        if (path.removeLastCell()) {
            removedCell?.pathColor = null
            return true
        }
        return false
    }

    /**
     * Add a cell to the start of the path (useful when extending from the start)
     */
    fun addCellToPathAtStart(color: Int, row: Int, col: Int): Boolean {
        val path = paths[color] ?: return false
        val cell = grid[row][col]
        path.cells.add(0, cell)
        cell.pathColor = color
        return true
    }

    /**
     * Remove all cells after the given index (keeps elements 0..index)
     */
    fun removeCellsAfterIndex(color: Int, index: Int): Boolean {
        val path = paths[color] ?: return false
        if (index >= path.cells.size - 1) return false
        for (i in path.cells.size - 1 downTo index + 1) {
            val removed = path.cells.removeAt(i)
            removed.pathColor = null
        }
        return true
    }

    /**
     * Remove all cells before the given index (keeps elements index..end)
     */
    fun removeCellsBeforeIndex(color: Int, index: Int): Boolean {
        val path = paths[color] ?: return false
        if (index <= 0) return false
        for (i in 0 until index) {
            val removed = path.cells.removeAt(0)
            removed.pathColor = null
        }
        return true
    }

    /**
     * Clear a path completely
     */
    fun clearPath(color: Int) {
        val path = paths[color] ?: return
        path.cells.forEach { cell ->
            cell.pathColor = null
        }
        path.clear()
    }

    /**
     * Check if all grid cells are filled
     */
    fun isGridComplete(): Boolean {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (!grid[r][c].isFilled()) return false
            }
        }
        return true
    }

    /**
     * Check if all paths are complete
     */
    fun areAllPathsComplete(): Boolean {
        return colors.all { color ->
            paths[color]?.isComplete() ?: false
        }
    }

    /**
     * Check if the game is won
     */
    fun isWon(): Boolean {
        return areAllPathsComplete() && isGridComplete()
    }

    /**
     * Get completion percentage (for hint/UI purposes)
     */
    fun getCompletionPercentage(): Int {
        var filledCells = 0
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (grid[r][c].isFilled()) filledCells++
            }
        }
        val totalCells = rows * cols
        return (filledCells * 100) / totalCells
    }

    /**
     * Reset the entire board (keep endpoints, clear paths)
     */
    fun reset() {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                grid[r][c].reset()
            }
        }
        paths.forEach { (_, path) ->
            path.clear()
        }
    }

    /**
     * Get all occupied cells (for drawing)
     */
    fun getOccupiedCells(): List<Cell> {
        val occupied = mutableListOf<Cell>()
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (grid[r][c].isFilled()) {
                    occupied.add(grid[r][c])
                }
            }
        }
        return occupied
    }

    /**
     * Get the grid (for testing/debugging)
     */
    fun getGrid(): Array<Array<Cell>> = grid

    override fun toString(): String {
        return "GameBoard($rows x $cols, ${colors.size} colors, ${paths.values.count { it.isComplete() }} complete paths)"
    }
}
