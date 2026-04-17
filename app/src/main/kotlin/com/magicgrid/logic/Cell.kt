package com.magicgrid.logic

/**
 * Represents a single cell in the game grid.
 * Each cell contains color information and path data.
 */
data class Cell(
    val row: Int,
    val col: Int,
    var color: Int? = null, // Color if this is an endpoint
    var pathColor: Int? = null, // Color of the path currently occupying this cell
    var isEndpoint: Boolean = false // Is this an endpoint?
) {
    /**
     * Check if this cell is filled (either endpoint or has a path)
     */
    fun isFilled(): Boolean = color != null || pathColor != null

    /**
     * Reset the cell to empty state (but keep the endpoint if it exists)
     */
    fun reset() {
        if (!isEndpoint) {
            color = null
            pathColor = null
        }
    }

    /**
     * Override equality to compare only position
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Cell) return false
        return row == other.row && col == other.col
    }

    override fun hashCode(): Int {
        return row * 31 + col
    }

    override fun toString(): String {
        return "Cell($row, $col, color=$color, pathColor=$pathColor)"
    }
}
