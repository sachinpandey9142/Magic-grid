package com.magicgrid.logic

import kotlin.math.ceil
import kotlin.random.Random

/**
 * Generates valid, solvable puzzle levels.
 * Ensures every generated level fills the entire grid with non-overlapping paths.
 */
class LevelGenerator(private val seed: Long = System.currentTimeMillis()) {
    private val random = Random(seed)

    private data class DifficultyProfile(
        val colorRange: IntRange,
        val segmentSlack: Int,
        val spreadFactor: Double,
        val avgPairDistanceFactor: Double,
        val minPairDistance: Int,
        val interiorEndpointRatio: Double,
        val maxClosePairs: Int,
        val pathShuffleMultiplier: Int,
        val attempts: Int
    )

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
     * 1) Build a Hamiltonian path that visits every cell exactly once.
     * 2) Split that path into contiguous color segments.
     * 3) Use segment endpoints as dots.
     *
     * Because every segment is contiguous and segments cover all cells,
     * at least one valid full-board solution is always guaranteed.
     */
    fun generateLevel(gridSize: Int, difficulty: String): GeneratedLevel? {
        val profile = resolveDifficultyProfile(difficulty, gridSize)
        val minSegmentLength = maxOf(3, gridSize - profile.segmentSlack)
        val colorCount = pickColorCount(profile.colorRange, gridSize, minSegmentLength)

        // Try strict generation first to keep tiers meaningfully separated.
        repeat(profile.attempts) {
            val candidate = buildGuaranteedEndpoints(
                gridSize = gridSize,
                colorCount = colorCount,
                minSegmentLength = minSegmentLength,
                pathShuffleSteps = gridSize * gridSize * profile.pathShuffleMultiplier,
                profile = profile
            )
            if (candidate != null) {
                return toGeneratedLevel(gridSize, difficulty, candidate)
            }
        }

        // Relax constraints slightly if strict generation could not find enough variety.
        val relaxedProfile = profile.copy(
            spreadFactor = profile.spreadFactor * 0.9,
            avgPairDistanceFactor = profile.avgPairDistanceFactor * 0.9,
            minPairDistance = maxOf(2, profile.minPairDistance - 1),
            interiorEndpointRatio = profile.interiorEndpointRatio * 0.82,
            maxClosePairs = profile.maxClosePairs + 1
        )

        val relaxed = buildGuaranteedEndpoints(
            gridSize = gridSize,
            colorCount = colorCount,
            minSegmentLength = maxOf(3, minSegmentLength - 1),
            pathShuffleSteps = gridSize * gridSize * maxOf(2, profile.pathShuffleMultiplier / 2),
            profile = relaxedProfile,
            transformId = 0
        )

        if (relaxed != null) {
            return toGeneratedLevel(gridSize, difficulty, relaxed)
        }

        // Emergency fallback that keeps boards solvable and avoids generation deadlocks.
        val emergencyMinSegment = maxOf(3, gridSize - 2)
        val emergencyColorCount = pickColorCount(3..5, gridSize, emergencyMinSegment)
        val emergency = buildGuaranteedEndpoints(
            gridSize = gridSize,
            colorCount = emergencyColorCount,
            minSegmentLength = emergencyMinSegment,
            pathShuffleSteps = gridSize * gridSize,
            profile = null,
            transformId = 0,
            strictDistribution = false
        ) ?: return null

        return toGeneratedLevel(gridSize, difficulty, emergency)
    }

    private fun toGeneratedLevel(
        gridSize: Int,
        difficulty: String,
        endpoints: List<ColorEndpointSpec>
    ): GeneratedLevel {
        return GeneratedLevel(
            gridSize = gridSize,
            rows = gridSize,
            cols = gridSize,
            endpoints = endpoints.map { ep ->
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

    private fun resolveDifficultyProfile(difficulty: String, gridSize: Int): DifficultyProfile {
        return when {
            difficulty.equals("easy", ignoreCase = true) -> DifficultyProfile(
                colorRange = 4..5,
                segmentSlack = 1,
                spreadFactor = 0.72,
                avgPairDistanceFactor = 0.76,
                minPairDistance = maxOf(3, gridSize / 2),
                interiorEndpointRatio = 0.35,
                maxClosePairs = 2,
                pathShuffleMultiplier = 4,
                attempts = 56
            )

            difficulty.equals("medium", ignoreCase = true) -> DifficultyProfile(
                colorRange = 5..6,
                segmentSlack = 1,
                spreadFactor = 0.76,
                avgPairDistanceFactor = 0.84,
                minPairDistance = maxOf(3, (gridSize / 2) + 1),
                interiorEndpointRatio = 0.43,
                maxClosePairs = 1,
                pathShuffleMultiplier = 6,
                attempts = 76
            )

            difficulty.equals("hard", ignoreCase = true) -> DifficultyProfile(
                colorRange = 6..7,
                segmentSlack = 2,
                spreadFactor = 0.8,
                avgPairDistanceFactor = 0.9,
                minPairDistance = maxOf(4, (gridSize / 2) + 1),
                interiorEndpointRatio = 0.5,
                maxClosePairs = 0,
                pathShuffleMultiplier = 8,
                attempts = 96
            )

            else -> DifficultyProfile(
                colorRange = 5..6,
                segmentSlack = 1,
                spreadFactor = 0.74,
                avgPairDistanceFactor = 0.8,
                minPairDistance = maxOf(3, gridSize / 2),
                interiorEndpointRatio = 0.4,
                maxClosePairs = 1,
                pathShuffleMultiplier = 5,
                attempts = 64
            )
        }
    }

    private fun pickColorCount(range: IntRange, gridSize: Int, minSegmentLength: Int): Int {
        val maxBySegmentLength = (gridSize * gridSize) / maxOf(1, minSegmentLength)
        val maxAllowed = minOf(range.last, maxBySegmentLength, palette.size)
        if (maxAllowed <= 2) return 2

        val minAllowed = minOf(range.first, maxAllowed).coerceAtLeast(2)
        return nextIntInclusive(minAllowed, maxAllowed)
    }

    private fun nextIntInclusive(min: Int, max: Int): Int {
        if (max <= min) return min
        return min + random.nextInt(max - min + 1)
    }

    private fun buildGuaranteedEndpoints(
        gridSize: Int,
        colorCount: Int,
        minSegmentLength: Int,
        pathShuffleSteps: Int,
        profile: DifficultyProfile?,
        transformId: Int? = null,
        strictDistribution: Boolean = true
    ): List<ColorEndpointSpec>? {
        val snake = buildSnakePath(gridSize)
        val shuffled = randomizeHamiltonianPath(snake, gridSize, pathShuffleSteps)
        val transformed = applyTransform(
            shuffled,
            gridSize,
            transformId ?: random.nextInt(8)
        )

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

        if (strictDistribution) {
            val passes = if (profile != null) {
                passesDifficultyChecks(endpoints, gridSize, profile)
            } else {
                passesDistributionCheck(endpoints, gridSize, thresholdFactor = 0.65)
            }

            if (!passes) return null
        }

        return endpoints
    }

    private fun randomizeHamiltonianPath(
        basePath: List<Pair<Int, Int>>,
        gridSize: Int,
        shuffleSteps: Int
    ): List<Pair<Int, Int>> {
        var path = basePath.toMutableList()
        if (path.size < 4) return path

        repeat(shuffleSteps) {
            if (random.nextBoolean()) {
                val head = path.first()
                val nextToHead = path[1]
                val candidateIndices = getOrthogonalNeighbors(head, gridSize)
                    .filter { it != nextToHead }
                    .map { neighbor -> path.indexOf(neighbor) }
                    .filter { index -> index > 1 }

                if (candidateIndices.isEmpty()) return@repeat

                val spliceIndex = candidateIndices[random.nextInt(candidateIndices.size)]
                val reversedPrefix = path.subList(0, spliceIndex).asReversed()
                val suffix = path.subList(spliceIndex, path.size)
                path = (reversedPrefix + suffix).toMutableList()
            } else {
                val tail = path.last()
                val prevToTail = path[path.size - 2]
                val candidateIndices = getOrthogonalNeighbors(tail, gridSize)
                    .filter { it != prevToTail }
                    .map { neighbor -> path.indexOf(neighbor) }
                    .filter { index -> index in 0 until (path.size - 2) }

                if (candidateIndices.isEmpty()) return@repeat

                val spliceIndex = candidateIndices[random.nextInt(candidateIndices.size)]
                val prefix = path.subList(0, spliceIndex + 1)
                val reversedSuffix = path.subList(spliceIndex + 1, path.size).asReversed()
                path = (prefix + reversedSuffix).toMutableList()
            }
        }

        return if (random.nextBoolean()) path else path.asReversed()
    }

    private fun getOrthogonalNeighbors(cell: Pair<Int, Int>, gridSize: Int): List<Pair<Int, Int>> {
        val (row, col) = cell
        val neighbors = mutableListOf<Pair<Int, Int>>()

        if (row > 0) neighbors.add(Pair(row - 1, col))
        if (row < gridSize - 1) neighbors.add(Pair(row + 1, col))
        if (col > 0) neighbors.add(Pair(row, col - 1))
        if (col < gridSize - 1) neighbors.add(Pair(row, col + 1))

        return neighbors
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

            val current = nextIntInclusive(low, high)
            segments[index] = current
            remaining -= current
        }

        return segments
    }

    private fun passesDifficultyChecks(
        endpoints: List<ColorEndpointSpec>,
        gridSize: Int,
        profile: DifficultyProfile
    ): Boolean {
        if (!passesDistributionCheck(endpoints, gridSize, profile.spreadFactor)) return false

        val pairDistances = endpoints.map { ep ->
            kotlin.math.abs(ep.startRow - ep.endRow) + kotlin.math.abs(ep.startCol - ep.endCol)
        }

        if (pairDistances.isEmpty()) return false
        if (pairDistances.any { it < profile.minPairDistance }) return false

        val avgPairDistance = pairDistances.average()
        if (avgPairDistance < gridSize * profile.avgPairDistanceFactor) return false

        val points = endpoints.flatMap { ep ->
            listOf(Pair(ep.startRow, ep.startCol), Pair(ep.endRow, ep.endCol))
        }

        val interiorPoints = points.count { (row, col) ->
            row in 1 until (gridSize - 1) && col in 1 until (gridSize - 1)
        }
        val requiredInterior = ceil(points.size * profile.interiorEndpointRatio).toInt()
        if (interiorPoints < requiredInterior) return false

        val closePairThreshold = maxOf(2, (gridSize / 2) - 1)
        val closePairs = pairDistances.count { it <= closePairThreshold }
        if (closePairs > profile.maxClosePairs) return false

        return true
    }

    private fun passesDistributionCheck(
        endpoints: List<ColorEndpointSpec>,
        gridSize: Int,
        thresholdFactor: Double
    ): Boolean {
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
        return avgDistance >= (gridSize * thresholdFactor)
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
