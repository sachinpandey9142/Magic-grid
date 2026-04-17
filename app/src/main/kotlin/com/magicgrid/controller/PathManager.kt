package com.magicgrid.controller

import com.magicgrid.logic.Path

/**
 * Helper class to manage path drawing and validation
 * Handles complex touch logic:
 * - Starting paths from either endpoint
 * - Continuing existing paths
 * - Backtracking
 * - Preventing overlaps
 */
class PathManager {
    /**
     * Check if a cell is an endpoint for a path
     */
    fun isEndpoint(path: Path, row: Int, col: Int): Boolean {
        return (row == path.startRow && col == path.startCol) ||
                (row == path.endRow && col == path.endCol)
    }

    /**
     * Get other endpoint (if touching one endpoint)
     */
    fun getOtherEndpoint(path: Path, row: Int, col: Int): Pair<Int, Int>? {
        return if (row == path.startRow && col == path.startCol) {
            Pair(path.endRow, path.endCol)
        } else if (row == path.endRow && col == path.endCol) {
            Pair(path.startRow, path.startCol)
        } else {
            null
        }
    }

    /**
     * Check if two cells are adjacent (4-way: up, down, left, right)
     */
    fun areAdjacent(row1: Int, col1: Int, row2: Int, col2: Int): Boolean {
        val rowDiff = kotlin.math.abs(row1 - row2)
        val colDiff = kotlin.math.abs(col1 - col2)
        return (rowDiff == 1 && colDiff == 0) || (rowDiff == 0 && colDiff == 1)
    }

    /**
     * Check if path is complete (all cells filled from start to end)
     */
    fun isPathComplete(path: Path): Boolean {
        if (path.cells.isEmpty()) return false
        if (path.cells.size < 2) return false
        
        // Check that path starts at one endpoint and ends at the other
        val firstCell = path.cells.first()
        val lastCell = path.cells.last()
        
        val startsCorrectly = (firstCell.row == path.startRow && firstCell.col == path.startCol) ||
                              (firstCell.row == path.endRow && firstCell.col == path.endCol)
        
        val endsCorrectly = (lastCell.row == path.startRow && lastCell.col == path.startCol) ||
                            (lastCell.row == path.endRow && lastCell.col == path.endCol)
        
        return startsCorrectly && endsCorrectly
    }

    /**
     * Calculate Manhattan distance between two cells
     */
    fun getManhattanDistance(row1: Int, col1: Int, row2: Int, col2: Int): Int {
        return kotlin.math.abs(row1 - row2) + kotlin.math.abs(col1 - col2)
    }

    /**
     * Check if move is backtracking (moving closer to opposite endpoint)
     */
    fun isMovingTowardEndpoint(
        fromRow: Int, fromCol: Int,
        toRow: Int, toCol: Int,
        targetRow: Int, targetCol: Int
    ): Boolean {
        val distanceBefore = getManhattanDistance(fromRow, fromCol, targetRow, targetCol)
        val distanceAfter = getManhattanDistance(toRow, toCol, targetRow, targetCol)
        return distanceAfter < distanceBefore
    }
}
