package com.magicgrid.data

import kotlin.math.abs
import kotlin.random.Random

/**
 * Provides game levels generated on demand.
 * Construction is guaranteed-solvable because endpoints come from contiguous
 * segments of a full-grid Hamiltonian path.
 */
object LevelRepository {
    const val EASY_LEVEL_COUNT = 50
    const val MEDIUM_LEVEL_COUNT = 30
    const val HARD_LEVEL_COUNT = 20
    private const val TOTAL_LEVEL_COUNT = EASY_LEVEL_COUNT + MEDIUM_LEVEL_COUNT + HARD_LEVEL_COUNT

    private const val EASY_GRID_SIZE = 5
    private const val MEDIUM_GRID_SIZE = 6
    private const val HARD_GRID_SIZE = 7

    private data class PackSpec(
        val range: IntRange,
        val gridSize: Int,
        val primaryDifficulty: String,
        val namePrefix: String
    )

    private data class DifficultyProfile(
        val colorCount: Int,
        val minSegmentLength: Int,
        val minPairDistance: Int,
        val minTurnsPerPath: Int,
        val maxStraightPairs: Int,
        val minInteriorEndpoints: Int,
        val minSpreadFactor: Double,
        val minTotalTurns: Int,
        val shuffleMultiplier: Int,
        val attempts: Int
    )

    private data class CandidateMetrics(
        val closeColorPairs: Int,
        val straightPairs: Int,
        val lowTurnPaths: Int,
        val totalTurns: Int,
        val interiorEndpoints: Int,
        val avgSpread: Double,
        val score: Int
    )

    private val easyPack = PackSpec(
        range = 1..EASY_LEVEL_COUNT,
        gridSize = EASY_GRID_SIZE,
        primaryDifficulty = "easy",
        namePrefix = "Easy Level"
    )

    private val mediumPack = PackSpec(
        range = (EASY_LEVEL_COUNT + 1)..(EASY_LEVEL_COUNT + MEDIUM_LEVEL_COUNT),
        gridSize = MEDIUM_GRID_SIZE,
        primaryDifficulty = "medium",
        namePrefix = "Medium Level"
    )

    private val hardPack = PackSpec(
        range = (EASY_LEVEL_COUNT + MEDIUM_LEVEL_COUNT + 1)..TOTAL_LEVEL_COUNT,
        gridSize = HARD_GRID_SIZE,
        primaryDifficulty = "hard",
        namePrefix = "Hard Level"
    )

    private val levelsById = mutableMapOf<Int, Level>()
    private val generationLock = Any()

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

    private fun ensureLevelGenerated(levelId: Int) {
        if (levelId !in 1..TOTAL_LEVEL_COUNT) return

        synchronized(generationLock) {
            if (levelsById.containsKey(levelId)) return
            levelsById[levelId] = generateLevelForId(levelId)
        }
    }

    private fun ensureRangeGenerated(range: IntRange) {
        if (range.isEmpty()) return

        val safeStart = range.first.coerceAtLeast(1)
        val safeEnd = range.last.coerceAtMost(TOTAL_LEVEL_COUNT)
        if (safeStart > safeEnd) return

        for (id in safeStart..safeEnd) {
            ensureLevelGenerated(id)
        }
    }

    private fun generateLevelForId(levelId: Int): Level {
        val pack = resolvePack(levelId) ?: easyPack
        val packIndex = levelId - pack.range.first + 1
        val profile = resolveProfile(pack)
        val randomSeed = (levelId * 7919L) + (pack.gridSize * 101L) + (pack.range.first * 17L)
        val random = Random(randomSeed)

        val endpoints = buildEndpoints(pack, profile, levelId, random)

        return Level(
            id = levelId,
            name = "${pack.namePrefix} $packIndex",
            gridSize = pack.gridSize,
            rows = pack.gridSize,
            cols = pack.gridSize,
            endpoints = endpoints
        )
    }

    private fun resolveProfile(pack: PackSpec): DifficultyProfile {
        return when (pack.primaryDifficulty) {
            "hard" -> DifficultyProfile(
                colorCount = 8,
                minSegmentLength = 5,
                minPairDistance = 5,
                minTurnsPerPath = 5,
                maxStraightPairs = 0,
                minInteriorEndpoints = 8,
                minSpreadFactor = 0.8,
                minTotalTurns = 26,
                shuffleMultiplier = 10,
                attempts = 140
            )

            "medium" -> DifficultyProfile(
                colorCount = 6,
                minSegmentLength = 4,
                minPairDistance = 4,
                minTurnsPerPath = 4,
                maxStraightPairs = 1,
                minInteriorEndpoints = 6,
                minSpreadFactor = 0.7,
                minTotalTurns = 18,
                shuffleMultiplier = 7,
                attempts = 88
            )

            else -> DifficultyProfile(
                colorCount = 5,
                minSegmentLength = 3,
                minPairDistance = 2,
                minTurnsPerPath = 2,
                maxStraightPairs = 2,
                minInteriorEndpoints = 3,
                minSpreadFactor = 0.58,
                minTotalTurns = 8,
                shuffleMultiplier = 5,
                attempts = 56
            )
        }
    }

    private fun buildEndpoints(
        pack: PackSpec,
        profile: DifficultyProfile,
        levelId: Int,
        random: Random
    ): List<Level.ColorEndpoint> {
        val gridSize = pack.gridSize
        val totalCells = gridSize * gridSize
        val colorCount = profile.colorCount
            .coerceAtMost(palette.size)
            .coerceAtMost((totalCells / 2).coerceAtLeast(2))
            .coerceAtLeast(2)

        var bestEndpoints: List<Level.ColorEndpoint>? = null
        var bestScore = Int.MIN_VALUE
        var bestValidEndpoints: List<Level.ColorEndpoint>? = null
        var bestValidScore = Int.MIN_VALUE

        repeat(profile.attempts) {
            val baseSnake = buildSnakePath(gridSize)
            val shuffledSnake = randomizeHamiltonianPath(
                basePath = baseSnake,
                gridSize = gridSize,
                random = random,
                shuffleSteps = totalCells * profile.shuffleMultiplier
            )
            val transformedSnake = applyTransform(shuffledSnake, gridSize, random.nextInt(8))
            val segmentLengths = splitIntoSegments(
                totalCells = totalCells,
                segmentCount = colorCount,
                minSegmentLength = profile.minSegmentLength,
                random = random
            )

            val (endpoints, segments) = createEndpointsAndSegments(
                transformedSnake = transformedSnake,
                segmentLengths = segmentLengths,
                levelId = levelId,
                random = random
            )

            val metrics = evaluateCandidate(
                endpoints = endpoints,
                segments = segments,
                gridSize = gridSize,
                profile = profile
            )

            if (metrics.score > bestScore) {
                bestScore = metrics.score
                bestEndpoints = endpoints
            }

            // Keep the most complex valid board instead of returning the first pass.
            if (isCandidateValid(metrics, profile, gridSize) && metrics.score > bestValidScore) {
                bestValidScore = metrics.score
                bestValidEndpoints = endpoints
            }
        }

        return bestValidEndpoints ?: bestEndpoints ?: buildDeterministicFallbackEndpoints(
            gridSize = gridSize,
            colorCount = colorCount,
            levelId = levelId
        )
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

    private fun randomizeHamiltonianPath(
        basePath: List<Pair<Int, Int>>,
        gridSize: Int,
        random: Random,
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

    private fun splitIntoSegments(
        totalCells: Int,
        segmentCount: Int,
        minSegmentLength: Int,
        random: Random
    ): IntArray {
        val count = segmentCount.coerceAtLeast(1)
        val safeMin = minSegmentLength
            .coerceAtLeast(2)
            .coerceAtMost((totalCells / count).coerceAtLeast(2))

        val segments = IntArray(count) { safeMin }
        var remaining = totalCells - (safeMin * count)

        while (remaining > 0) {
            val index = random.nextInt(count)
            segments[index] += 1
            remaining--
        }

        return segments
    }

    private fun createEndpointsAndSegments(
        transformedSnake: List<Pair<Int, Int>>,
        segmentLengths: IntArray,
        levelId: Int,
        random: Random
    ): Pair<List<Level.ColorEndpoint>, List<List<Pair<Int, Int>>>> {
        val colorOffset = ((levelId % palette.size) + random.nextInt(palette.size)) % palette.size
        val endpoints = mutableListOf<Level.ColorEndpoint>()
        val segments = mutableListOf<List<Pair<Int, Int>>>()

        var cursor = 0
        for (index in segmentLengths.indices) {
            val segmentLength = segmentLengths[index]
            val segment = transformedSnake.subList(cursor, cursor + segmentLength).toList()
            val start = segment.first()
            val end = segment.last()
            val color = palette[(colorOffset + index) % palette.size]

            endpoints.add(
                Level.ColorEndpoint(
                    color = color,
                    startRow = start.first,
                    startCol = start.second,
                    endRow = end.first,
                    endCol = end.second
                )
            )
            segments.add(segment)
            cursor += segmentLength
        }

        return endpoints to segments
    }

    private fun evaluateCandidate(
        endpoints: List<Level.ColorEndpoint>,
        segments: List<List<Pair<Int, Int>>>,
        gridSize: Int,
        profile: DifficultyProfile
    ): CandidateMetrics {
        val pairDistances = endpoints.map { ep ->
            abs(ep.startRow - ep.endRow) + abs(ep.startCol - ep.endCol)
        }

        val closeColorPairs = pairDistances.count { it < profile.minPairDistance }
        val straightPairs = endpoints.count { ep ->
            ep.startRow == ep.endRow || ep.startCol == ep.endCol
        }

        val turnCounts = segments.map { segment -> countTurns(segment) }
        val lowTurnPaths = segments.indices.count { index ->
            val maxTurns = (segments[index].size - 2).coerceAtLeast(0)
            val requiredTurns = minOf(profile.minTurnsPerPath, maxTurns)
            turnCounts[index] < requiredTurns
        }
        val totalTurns = turnCounts.sum()

        val points = endpoints.flatMap { ep ->
            listOf(Pair(ep.startRow, ep.startCol), Pair(ep.endRow, ep.endCol))
        }

        val interiorEndpoints = points.count { (row, col) ->
            row in 1 until (gridSize - 1) && col in 1 until (gridSize - 1)
        }

        var spreadTotal = 0
        var spreadPairs = 0
        for (i in points.indices) {
            for (j in (i + 1) until points.size) {
                spreadTotal += abs(points[i].first - points[j].first) + abs(points[i].second - points[j].second)
                spreadPairs++
            }
        }
        val avgSpread = if (spreadPairs == 0) 0.0 else spreadTotal.toDouble() / spreadPairs.toDouble()

        val score =
            (avgSpread * 100).toInt() +
            (interiorEndpoints * 18) +
            (totalTurns * 8) -
            (straightPairs * 44) -
            (closeColorPairs * 70) -
            (lowTurnPaths * 85)

        return CandidateMetrics(
            closeColorPairs = closeColorPairs,
            straightPairs = straightPairs,
            lowTurnPaths = lowTurnPaths,
            totalTurns = totalTurns,
            interiorEndpoints = interiorEndpoints,
            avgSpread = avgSpread,
            score = score
        )
    }

    private fun countTurns(segment: List<Pair<Int, Int>>): Int {
        if (segment.size < 3) return 0

        var turns = 0
        for (index in 1 until segment.lastIndex) {
            val prev = segment[index - 1]
            val current = segment[index]
            val next = segment[index + 1]

            val dirA = Pair(current.first - prev.first, current.second - prev.second)
            val dirB = Pair(next.first - current.first, next.second - current.second)

            if (dirA != dirB) {
                turns++
            }
        }

        return turns
    }

    private fun isCandidateValid(
        metrics: CandidateMetrics,
        profile: DifficultyProfile,
        gridSize: Int
    ): Boolean {
        if (metrics.closeColorPairs > 0) return false
        if (metrics.straightPairs > profile.maxStraightPairs) return false
        if (metrics.lowTurnPaths > 0) return false
        if (metrics.totalTurns < profile.minTotalTurns) return false
        if (metrics.interiorEndpoints < profile.minInteriorEndpoints) return false
        if (metrics.avgSpread < (gridSize * profile.minSpreadFactor)) return false
        return true
    }

    private fun buildDeterministicFallbackEndpoints(
        gridSize: Int,
        colorCount: Int,
        levelId: Int
    ): List<Level.ColorEndpoint> {
        val totalCells = gridSize * gridSize
        val random = Random(levelId * 104729L + 37L)
        val snake = applyTransform(buildSnakePath(gridSize), gridSize, levelId % 8)
        val segmentLengths = splitIntoSegments(
            totalCells = totalCells,
            segmentCount = colorCount,
            minSegmentLength = 2,
            random = random
        )

        return createEndpointsAndSegments(
            transformedSnake = snake,
            segmentLengths = segmentLengths,
            levelId = levelId,
            random = random
        ).first
    }

    private fun resolvePack(levelId: Int): PackSpec? {
        return when (levelId) {
            in easyPack.range -> easyPack
            in mediumPack.range -> mediumPack
            in hardPack.range -> hardPack
            else -> null
        }
    }

    /**
     * Get all levels
     */
    fun getAllLevels(): List<Level> {
        ensureRangeGenerated(1..TOTAL_LEVEL_COUNT)
        synchronized(generationLock) {
            return (1..TOTAL_LEVEL_COUNT).mapNotNull { id -> levelsById[id] }
        }
    }

    /**
     * Get a specific level by ID
     */
    fun getLevel(id: Int): Level? {
        if (id !in 1..TOTAL_LEVEL_COUNT) return null
        ensureLevelGenerated(id)
        synchronized(generationLock) {
            return levelsById[id]
        }
    }

    /**
     * Get the next level after the given ID
     */
    fun getNextLevel(currentId: Int): Level? {
        val nextId = currentId + 1
        if (nextId !in 1..TOTAL_LEVEL_COUNT) return null
        return getLevel(nextId)
    }

    /**
     * Get total number of levels
     */
    fun getTotalLevels(): Int = TOTAL_LEVEL_COUNT

    /**
     * Get level count by difficulty
     */
    fun getLevelsByDifficulty(difficulty: String): List<Level> {
        val range = getDifficultyRange(difficulty)
        return if (range != null) {
            ensureRangeGenerated(range)
            synchronized(generationLock) {
                range.mapNotNull { id -> levelsById[id] }
            }
        } else {
            ensureRangeGenerated(1..TOTAL_LEVEL_COUNT)
            synchronized(generationLock) {
                levelsById.values
                    .sortedBy { it.id }
                    .filter { it.getDifficulty().equals(difficulty, ignoreCase = true) }
            }
        }
    }

    fun getDifficultyRange(difficulty: String): IntRange? {
        return when {
            difficulty.equals("easy", ignoreCase = true) -> 1..EASY_LEVEL_COUNT
            difficulty.equals("medium", ignoreCase = true) -> (EASY_LEVEL_COUNT + 1)..(EASY_LEVEL_COUNT + MEDIUM_LEVEL_COUNT)
            difficulty.equals("hard", ignoreCase = true) -> (EASY_LEVEL_COUNT + MEDIUM_LEVEL_COUNT + 1)..(EASY_LEVEL_COUNT + MEDIUM_LEVEL_COUNT + HARD_LEVEL_COUNT)
            else -> null
        }
    }
}
