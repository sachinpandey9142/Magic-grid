package com.magicgrid.logic

import kotlin.random.Random

/**
 * Generates valid, solvable puzzle levels.
 * Ensures every generated level fills the entire grid with non-overlapping paths.
 */
class LevelGenerator(private val seed: Long = System.currentTimeMillis()) {
    private val random = Random(seed)

    private val palette = listOf(
        0xFFFF5252.toInt(), // Red
        0xFF2196F3.toInt(), // Blue
        0xFF4CAF50.toInt(), // Green
        0xFFFFC107.toInt(), // Yellow
        0xFF9C27B0.toInt(), // Purple
        0xFFFF9800.toInt(), // Orange
        0xFF00BCD4.toInt(), // Cyan
        0xFFE91E63.toInt()  // Pink
    )

    /**
     * Generate a guaranteed-solvable level.
     * Construction strategy:
     * 1) Build a Hamiltonian snake path that visits every cell exactly once.
     * 2) Split that path into contiguous color segments.
     * 3) Use segment endpoints as dots.
     *
     * Because every segment is contiguous and segments cover all cells,
     * at least one valid full-board solution is always guaranteed.
     */
    fun generateLevel(gridSize: Int, difficulty: String): GeneratedLevel? {
        val colorCount = pickColorCount(difficulty)

        // Try a few randomized transforms / splits for variety.
        repeat(24) {
            val candidate = buildGuaranteedEndpoints(gridSize, colorCount)
            if (candidate != null) {
                return GeneratedLevel(
                    gridSize = gridSize,
                    rows = gridSize,
                    cols = gridSize,
                    endpoints = candidate.map { ep ->
                        Endpoint(
                            ep.color,
                            ep.startRow,
                            ep.startCol,
                            ep.endRow,
                            ep.endCol
                        )
                    },
                    difficulty = difficulty,
                    solvable = true
                )
            }
        }

        // Deterministic fallback that still guarantees a full-grid solution.
        val fallback = buildGuaranteedEndpoints(gridSize, colorCount, transformId = 0, strictDistribution = false)
            ?: return null

        return GeneratedLevel(
            gridSize = gridSize,
            rows = gridSize,
            cols = gridSize,
            endpoints = fallback.map { ep ->
                Endpoint(
                    ep.color,
                    ep.startRow,
                    ep.startCol,
                    ep.endRow,
                    ep.endCol
                )
            },
            difficulty = difficulty,
            solvable = true
        )
    }

    private fun pickColorCount(difficulty: String): Int {
        val range = when (difficulty.lowercase()) {
            "easy" -> 2..3
            "medium" -> 3..4
            "hard" -> 4..5
            else -> 3..4
        }

        return random.nextInt(range.first, range.last + 1)
    }

    private fun buildGuaranteedEndpoints(
        gridSize: Int,
        colorCount: Int,
        transformId: Int? = null,
        strictDistribution: Boolean = true
    ): List<ColorEndpointSpec>? {
        val snake = buildSnakePath(gridSize)
        val transformed = applyTransform(
            snake,
            gridSize,
            transformId ?: random.nextInt(0, 8)
        )

        val minSegmentLength = maxOf(3, gridSize - 1)
        val segmentLengths = splitIntoContiguousSegments(transformed.size, colorCount, minSegmentLength)
            ?: return null

        val colors = palette.shuffled(random).take(colorCount)
        val endpoints = mutableListOf<ColorEndpointSpec>()

        var cursor = 0
        for (i in 0 until colorCount) {
            val segmentLength = segmentLengths[i]
            val start = transformed[cursor]
            val end = transformed[cursor + segmentLength - 1]
            endpoints.add(
                ColorEndpointSpec(
                    color = colors[i],
                    startRow = start.first,
                    startCol = start.second,
                    endRow = end.first,
                    endCol = end.second
                )
            )
            cursor += segmentLength
        }

        if (strictDistribution && !passesDistributionCheck(endpoints, gridSize)) {
            return null
        }

        return endpoints
    }

    private fun buildSnakePath(gridSize: Int): List<Pair<Int, Int>> {
        val path = mutableListOf<Pair<Int, Int>>()
        for (row in 0 until gridSize) {
            if (row % 2 == 0) {
                for (col in 0 until gridSize) {
                    path.add(Pair(row, col))
                }
            } else {
                for (col in (gridSize - 1) downTo 0) {
                    path.add(Pair(row, col))
                }
            }
        }
        return path
    }

    private fun applyTransform(
        path: List<Pair<Int, Int>>,
        gridSize: Int,
        transformId: Int
    ): List<Pair<Int, Int>> {
        return path.map { (row, col) ->
            when (transformId % 8) {
                0 -> Pair(row, col)
                1 -> Pair(col, gridSize - 1 - row)
                2 -> Pair(gridSize - 1 - row, gridSize - 1 - col)
                3 -> Pair(gridSize - 1 - col, row)
                4 -> Pair(row, gridSize - 1 - col)
                5 -> Pair(gridSize - 1 - row, col)
                6 -> Pair(col, row)
                else -> Pair(gridSize - 1 - col, gridSize - 1 - row)
            }
        }
    }

    private fun splitIntoContiguousSegments(
        totalCells: Int,
        segmentCount: Int,
        minSegmentLength: Int
    ): IntArray? {
        if (segmentCount <= 0) return null
        if (totalCells < segmentCount * minSegmentLength) return null

        val segments = IntArray(segmentCount)
        var remaining = totalCells

        for (index in 0 until segmentCount) {
            val left = segmentCount - index - 1
            if (index == segmentCount - 1) {
                segments[index] = remaining
                break
            }

            val maxForCurrent = remaining - (left * minSegmentLength)
            val target = remaining / (left + 1)
            var low = maxOf(minSegmentLength, target - (minSegmentLength / 2))
            var high = minOf(maxForCurrent, target + (minSegmentLength / 2) + 2)

            if (low > high) {
                low = minSegmentLength
                high = maxForCurrent
            }

            val current = random.nextInt(low, high + 1)
            segments[index] = current
            remaining -= current
        }

        return segments
    }

    private fun passesDistributionCheck(endpoints: List<ColorEndpointSpec>, gridSize: Int): Boolean {
        if (endpoints.isEmpty()) return false

        val points = endpoints.flatMap { ep ->
            listOf(
                Pair(ep.startRow, ep.startCol),
                Pair(ep.endRow, ep.endCol)
            )
        }

        // All endpoints must be unique.
        if (points.toSet().size != points.size) return false

        // Keep endpoint spread wide enough to avoid ultra-trivial boards.
        var total = 0
        var count = 0
        for (i in points.indices) {
            for (j in (i + 1) until points.size) {
                val a = points[i]
                val b = points[j]
                total += kotlin.math.abs(a.first - b.first) + kotlin.math.abs(a.second - b.second)
                count++
            }
        }

        val avgDistance = if (count == 0) 0.0 else total.toDouble() / count.toDouble()
        return avgDistance >= (gridSize * 0.65)
    }
}

/**
 * Represents an endpoint specification (equivalent to Level.ColorEndpoint)
 */
data class ColorEndpointSpec(
    val color: Int,
    val startRow: Int,
    val startCol: Int,
    val endRow: Int,
    val endCol: Int
)

/**
 * Represents a generated level
 */
data class GeneratedLevel(
    val gridSize: Int,
    val rows: Int,
    val cols: Int,
    val endpoints: List<Endpoint>,
    val difficulty: String,
    val solvable: Boolean
)
