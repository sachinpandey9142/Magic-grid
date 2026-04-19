package com.magicgrid.logic

import kotlin.math.abs

/**
 * Validates that a puzzle level is completely solvable
 * Ensures all cells can be filled without conflicts
 */
class PuzzleSolver {
    companion object {
        private const val MAX_CANDIDATES_PER_COLOR = 18
        private const val MAX_PATH_SEARCH_NODES = 32000
    }

    /**
     * Check if a level configuration is solvable
     * Returns true only if all cells can be filled without overlaps
     */
    fun isSolvable(board: GameBoard, endpoints: List<Endpoint>): Boolean {
        return solve(board, endpoints) != null
    }

    /**
     * Returns true only when the puzzle has exactly one valid full-board solution.
     */
    fun hasUniqueSolution(board: GameBoard, endpoints: List<Endpoint>): Boolean {
        if (!hasValidEndpointLayout(board, endpoints)) return false
        return countSolutions(board, endpoints, maxSolutions = 2) == 1
    }

    /**
     * Solve puzzle and return full path map per color when successful.
     * Returns null when no full-board valid solution is found.
     */
    fun solve(board: GameBoard, endpoints: List<Endpoint>): Map<Int, List<Pair<Int, Int>>>? {
        if (!hasValidEndpointLayout(board, endpoints)) return null

        val solvedPaths = trySolve(board, endpoints) ?: return null
        val valid = arePathsValid(solvedPaths, endpoints) &&
            isGridComplete(board.rows, board.cols, solvedPaths)

        return if (valid) solvedPaths else null
    }

    /**
     * Attempt to solve the puzzle using backtracking
     */
    private fun trySolve(board: GameBoard, endpoints: List<Endpoint>): Map<Int, List<Pair<Int, Int>>>? {
        val solvedPaths = mutableMapOf<Int, List<Pair<Int, Int>>>()
        val ordered = endpoints.sortedByDescending {
            abs(it.startRow - it.endRow) + abs(it.startCol - it.endCol)
        }

        fun search(colorIndex: Int): Boolean {
            if (colorIndex >= ordered.size) {
                return isGridComplete(board.rows, board.cols, solvedPaths)
            }

            val endpoint = ordered[colorIndex]
            val start = Pair(endpoint.startRow, endpoint.startCol)
            val end = Pair(endpoint.endRow, endpoint.endCol)

            val blocked = buildBlockedCells(endpoint, endpoints, solvedPaths)

            val candidates = findCandidatePaths(
                start = start,
                end = end,
                rows = board.rows,
                cols = board.cols,
                blockedCells = blocked
            )

            for (candidate in candidates) {
                solvedPaths[endpoint.color] = candidate
                if (search(colorIndex + 1)) {
                    return true
                }
            }

            solvedPaths.remove(endpoint.color)
            return false
        }

        return if (search(0)) solvedPaths.toMap() else null
    }

    private fun countSolutions(board: GameBoard, endpoints: List<Endpoint>, maxSolutions: Int): Int {
        if (maxSolutions <= 0) return 0

        val solvedPaths = mutableMapOf<Int, List<Pair<Int, Int>>>()
        val ordered = endpoints.sortedByDescending {
            abs(it.startRow - it.endRow) + abs(it.startCol - it.endCol)
        }
        var solutionCount = 0

        fun search(colorIndex: Int) {
            if (solutionCount >= maxSolutions) return

            if (colorIndex >= ordered.size) {
                if (isGridComplete(board.rows, board.cols, solvedPaths)) {
                    solutionCount++
                }
                return
            }

            val endpoint = ordered[colorIndex]
            val start = Pair(endpoint.startRow, endpoint.startCol)
            val end = Pair(endpoint.endRow, endpoint.endCol)
            val blocked = buildBlockedCells(endpoint, endpoints, solvedPaths)

            val candidates = findCandidatePaths(
                start = start,
                end = end,
                rows = board.rows,
                cols = board.cols,
                blockedCells = blocked
            )

            for (candidate in candidates) {
                solvedPaths[endpoint.color] = candidate
                search(colorIndex + 1)
                if (solutionCount >= maxSolutions) {
                    break
                }
            }

            solvedPaths.remove(endpoint.color)
        }

        search(0)
        return solutionCount
    }

    private fun hasValidEndpointLayout(board: GameBoard, endpoints: List<Endpoint>): Boolean {
        if (endpoints.isEmpty()) return false

        val endpointCells = endpoints.flatMap {
            listOf(Pair(it.startRow, it.startCol), Pair(it.endRow, it.endCol))
        }

        if (endpointCells.any { (r, c) -> r !in 0 until board.rows || c !in 0 until board.cols }) {
            return false
        }

        return endpointCells.toSet().size == endpointCells.size
    }

    private fun buildBlockedCells(
        current: Endpoint,
        endpoints: List<Endpoint>,
        solvedPaths: Map<Int, List<Pair<Int, Int>>>
    ): MutableSet<Pair<Int, Int>> {
        val blocked = mutableSetOf<Pair<Int, Int>>()

        // Other-color endpoints stay blocked for this color route.
        for (other in endpoints) {
            if (other.color == current.color) continue
            blocked.add(Pair(other.startRow, other.startCol))
            blocked.add(Pair(other.endRow, other.endCol))
        }

        // Already assigned routes are blocked to prevent overlap.
        for ((_, path) in solvedPaths) {
            blocked.addAll(path)
        }

        return blocked
    }

    private fun findCandidatePaths(
        start: Pair<Int, Int>,
        end: Pair<Int, Int>,
        rows: Int,
        cols: Int,
        blockedCells: Set<Pair<Int, Int>>
    ): List<List<Pair<Int, Int>>> {
        if (start == end) return listOf(listOf(start))

        val candidates = mutableListOf<List<Pair<Int, Int>>>()
        val path = mutableListOf(start)
        val visited = mutableSetOf(start)
        var nodeBudget = MAX_PATH_SEARCH_NODES

        fun dfs(current: Pair<Int, Int>) {
            if (nodeBudget <= 0 || candidates.size >= MAX_CANDIDATES_PER_COLOR) {
                return
            }
            nodeBudget--

            if (current == end) {
                candidates.add(path.toList())
                return
            }

            val neighbors = getNeighbors(current, rows, cols)
                .filter { next ->
                    next == end || (next !in blockedCells && next !in visited)
                }

            if (neighbors.isEmpty()) return

            val nonEndNeighbors = neighbors
                .filter { it != end }
                .sortedWith(
                    compareBy<Pair<Int, Int>> {
                        // Prefer low-degree branches first to reduce dead-end trapping.
                        availableNeighborCount(it, end, rows, cols, blockedCells, visited)
                    }.thenByDescending {
                        // Explore farther cells first so end is not reached too early.
                        heuristic(it, end)
                    }
                )

            val ordered = if (neighbors.any { it == end }) {
                nonEndNeighbors + end
            } else {
                nonEndNeighbors
            }

            for (next in ordered) {
                path.add(next)
                visited.add(next)
                dfs(next)
                visited.remove(next)
                path.removeAt(path.lastIndex)

                if (nodeBudget <= 0 || candidates.size >= MAX_CANDIDATES_PER_COLOR) {
                    break
                }
            }
        }

        dfs(start)
        return candidates.sortedByDescending { it.size }
    }

    private fun availableNeighborCount(
        cell: Pair<Int, Int>,
        end: Pair<Int, Int>,
        rows: Int,
        cols: Int,
        blockedCells: Set<Pair<Int, Int>>,
        visited: Set<Pair<Int, Int>>
    ): Int {
        return getNeighbors(cell, rows, cols).count { next ->
            next == end || (next !in blockedCells && next !in visited)
        }
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
