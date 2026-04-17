package com.magicgrid.logic

import kotlin.math.abs

/**
 * Validates that a puzzle level is completely solvable
 * Ensures all cells can be filled without conflicts
 */
class PuzzleSolver {
    /**
     * Check if a level configuration is solvable
     * Returns true only if all cells can be filled without overlaps
     */
    fun isSolvable(board: GameBoard, endpoints: List<Endpoint>): Boolean {
        if (endpoints.isEmpty()) return false

        val endpointCells = endpoints.flatMap {
            listOf(Pair(it.startRow, it.startCol), Pair(it.endRow, it.endCol))
        }

        if (endpointCells.any { (r, c) -> r !in 0 until board.rows || c !in 0 until board.cols }) {
            return false
        }

        if (endpointCells.toSet().size != endpointCells.size) {
            return false
        }

        val solvedPaths = trySolve(board, endpoints) ?: return false
        return arePathsValid(solvedPaths, endpoints) && isGridComplete(board.rows, board.cols, solvedPaths)
    }

    /**
     * Attempt to solve the puzzle using backtracking
     */
    private fun trySolve(board: GameBoard, endpoints: List<Endpoint>): Map<Int, List<Pair<Int, Int>>>? {
        val solvedPaths = mutableMapOf<Int, List<Pair<Int, Int>>>()

        // Route long pairs first to reduce blocked-end failures.
        val ordered = endpoints.sortedByDescending {
            abs(it.startRow - it.endRow) + abs(it.startCol - it.endCol)
        }

        for (endpoint in ordered) {
            val start = Pair(endpoint.startRow, endpoint.startCol)
            val end = Pair(endpoint.endRow, endpoint.endCol)

            val blocked = mutableSetOf<Pair<Int, Int>>()

            // Other-color endpoints are always blocked.
            for (other in endpoints) {
                if (other.color == endpoint.color) continue
                blocked.add(Pair(other.startRow, other.startCol))
                blocked.add(Pair(other.endRow, other.endCol))
            }

            // Already solved paths are blocked to avoid overlaps.
            for ((_, path) in solvedPaths) {
                blocked.addAll(path)
            }

            val path = findPath(start, end, board.rows, board.cols, blocked) ?: return null
            solvedPaths[endpoint.color] = path
        }

        return solvedPaths
    }

    /**
     * Find a path between two points using A* algorithm
     */
    private fun findPath(
        start: Pair<Int, Int>,
        end: Pair<Int, Int>,
        rows: Int,
        cols: Int,
        blockedCells: Set<Pair<Int, Int>>
    ): List<Pair<Int, Int>>? {
        val openSet = mutableSetOf(start)
        val cameFrom = mutableMapOf<Pair<Int, Int>, Pair<Int, Int>>()
        val gScore = mutableMapOf(start to 0)
        val fScore = mutableMapOf(start to heuristic(start, end))

        while (openSet.isNotEmpty()) {
            val current = openSet.minByOrNull { fScore[it] ?: Int.MAX_VALUE } ?: return null
            
            if (current == end) {
                return reconstructPath(cameFrom, current)
            }

            openSet.remove(current)

            for (neighbor in getNeighbors(current, rows, cols)) {
                if (neighbor != end && blockedCells.contains(neighbor)) continue

                val tentativeGScore = (gScore[current] ?: 0) + 1

                if (tentativeGScore < (gScore[neighbor] ?: Int.MAX_VALUE)) {
                    cameFrom[neighbor] = current
                    gScore[neighbor] = tentativeGScore
                    val h = heuristic(neighbor, end)
                    fScore[neighbor] = tentativeGScore + h

                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor)
                    }
                }
            }
        }

        return null  // No path found
    }

    private fun heuristic(from: Pair<Int, Int>, to: Pair<Int, Int>): Int {
        return abs(from.first - to.first) + abs(from.second - to.second)
    }

    private fun getNeighbors(
        cell: Pair<Int, Int>,
        rows: Int,
        cols: Int
    ): List<Pair<Int, Int>> {
        val neighbors = mutableListOf<Pair<Int, Int>>()
        val (r, c) = cell

        if (r > 0) neighbors.add(Pair(r - 1, c))  // Up
        if (r < rows - 1) neighbors.add(Pair(r + 1, c))  // Down
        if (c > 0) neighbors.add(Pair(r, c - 1))  // Left
        if (c < cols - 1) neighbors.add(Pair(r, c + 1))  // Right

        return neighbors
    }

    private fun reconstructPath(
        cameFrom: Map<Pair<Int, Int>, Pair<Int, Int>>,
        end: Pair<Int, Int>
    ): List<Pair<Int, Int>> {
        val path = mutableListOf(end)
        var cursor = end

        while (cameFrom.containsKey(cursor)) {
            cursor = cameFrom[cursor]!!
            path.add(cursor)
        }

        path.reverse()
        return path
    }

    private fun arePathsValid(
        paths: Map<Int, List<Pair<Int, Int>>>,
        endpoints: List<Endpoint>
    ): Boolean {
        for (endpoint in endpoints) {
            val path = paths[endpoint.color] ?: return false
            if (path.isEmpty()) return false

            val start = Pair(endpoint.startRow, endpoint.startCol)
            val end = Pair(endpoint.endRow, endpoint.endCol)

            val startsCorrectly = (path.first() == start && path.last() == end) ||
                (path.first() == end && path.last() == start)
            if (!startsCorrectly) return false

            for (i in 0 until path.size - 1) {
                val a = path[i]
                val b = path[i + 1]
                val rowDiff = abs(a.first - b.first)
                val colDiff = abs(a.second - b.second)
                if (!((rowDiff == 1 && colDiff == 0) || (rowDiff == 0 && colDiff == 1))) {
                    return false
                }
            }
        }

        return true
    }

    private fun isGridComplete(
        rows: Int,
        cols: Int,
        paths: Map<Int, List<Pair<Int, Int>>>
    ): Boolean {
        val occupied = mutableSetOf<Pair<Int, Int>>()
        for ((_, cells) in paths) {
            for (cell in cells) {
                occupied.add(cell)
            }
        }

        return occupied.size == rows * cols
    }
}

/**
 * Data class for endpoint definition
 */
data class Endpoint(
    val color: Int,
    val startRow: Int,
    val startCol: Int,
    val endRow: Int,
    val endCol: Int
)
