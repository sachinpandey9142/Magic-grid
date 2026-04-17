package com.magicgrid.logic

/**
 * Represents a path connecting two endpoints of the same color.
 * Stores the sequence of cells that form the path.
 */
class Path(
    val color: Int,
    val startRow: Int,
    val startCol: Int,
    val endRow: Int,
    val endCol: Int
) {
    // List of cells that form the path (in order)
    val cells: MutableList<Cell> = mutableListOf()
    
    // Locked means gameplay is finalized (e.g., full puzzle solved)
    var isLocked: Boolean = false
    
    // Track which endpoint is the "head" (the end we're extending from)
    // true = extending from endRow/endCol, false = extending from startRow/startCol
    var isHeadAtEnd: Boolean = false

    /**
     * Check if the path is complete (connects both endpoints)
     */
    fun isComplete(): Boolean {
        if (cells.size < 2) return false

        val firstCell = cells.first()
        val lastCell = cells.last()

        val firstIsEndpoint =
            (firstCell.row == startRow && firstCell.col == startCol) ||
                (firstCell.row == endRow && firstCell.col == endCol)

        val lastIsEndpoint =
            (lastCell.row == startRow && lastCell.col == startCol) ||
                (lastCell.row == endRow && lastCell.col == endCol)

        // A complete path must connect the two different endpoints.
        return firstIsEndpoint && lastIsEndpoint &&
            !((firstCell.row == lastCell.row) && (firstCell.col == lastCell.col))
    }

    /**
     * Get the last cell in the path
     */
    fun getLastCell(): Cell? = cells.lastOrNull()

    /**
     * Add a cell to the path
     */
    fun addCell(cell: Cell) {
        cells.add(cell)
    }

    /**
     * Remove the last cell from the path
     */
    fun removeLastCell(): Boolean {
        return if (cells.isNotEmpty()) {
            cells.removeAt(cells.size - 1)
            true
        } else {
            false
        }
    }

    /**
     * Clear all cells from the path
     */
    fun clear() {
        cells.clear()
    }

    /**
     * Check if a cell is already in the path
     */
    fun containsCell(row: Int, col: Int): Boolean {
        return cells.any { it.row == row && it.col == col }
    }

    /**
     * Get the length of the path (number of cells)
     */
    fun getLength(): Int = cells.size

    override fun toString(): String {
        return "Path(color=$color, length=${cells.size}, complete=${isComplete()})"
    }
}
