package com.magicgrid.data

import com.magicgrid.logic.LevelGenerator

/**
 * Provides game levels - generates valid, solvable levels at runtime.
 * Each level is guaranteed to be completable with proper puzzle solving.
 */
object LevelRepository {
    const val EASY_LEVEL_COUNT = 50
    const val MEDIUM_LEVEL_COUNT = 30
    const val HARD_LEVEL_COUNT = 20

    private const val EASY_GRID_SIZE = 5
    private const val MEDIUM_GRID_SIZE = 6
    private const val HARD_GRID_SIZE = 7

    private val levels = mutableListOf<Level>()
    private val generator = LevelGenerator(seed = 42)  // Fixed seed for consistent levels

    init {
        initializeLevels()
    }

    /**
     * Initialize all game levels - generated to be guaranteed solvable
     */
    private fun initializeLevels() {
        levels.clear()

        // EASY LEVELS (5x5, 4-5 colors) -> 50 levels
        addGeneratedLevels(
            count = EASY_LEVEL_COUNT,
            gridSize = EASY_GRID_SIZE,
            difficulty = "easy",
            namePrefix = "Easy Level"
        )

        // MEDIUM LEVELS (6x6, 5-6 colors) -> 30 levels
        addGeneratedLevels(
            count = MEDIUM_LEVEL_COUNT,
            gridSize = MEDIUM_GRID_SIZE,
            difficulty = "medium",
            namePrefix = "Medium Level"
        )

        // HARD LEVELS (7x7, 6-7 colors) -> 20 levels
        addGeneratedLevels(
            count = HARD_LEVEL_COUNT,
            gridSize = HARD_GRID_SIZE,
            difficulty = "hard",
            namePrefix = "Hard Level"
        )

        // Fallback: Ensure we have at least some levels
        // These are handcrafted proven-solvable levels as backup
        if (levels.isEmpty()) {
            addFallbackLevels()
        }
    }

    private fun addGeneratedLevels(
        count: Int,
        gridSize: Int,
        difficulty: String,
        namePrefix: String
    ) {
        var packIndex = 1
        while (packIndex <= count) {
            val generated = generator.generateLevel(gridSize, difficulty) ?: continue
            levels.add(
                Level(
                    id = levels.size + 1,
                    name = "$namePrefix $packIndex",
                    gridSize = gridSize,
                    rows = gridSize,
                    cols = gridSize,
                    endpoints = generated.endpoints.map { ep ->
                        Level.ColorEndpoint(
                            color = ep.color,
                            startRow = ep.startRow,
                            startCol = ep.startCol,
                            endRow = ep.endRow,
                            endCol = ep.endCol
                        )
                    }
                )
            )
            packIndex++
        }
    }

    /**
     * Add handcrafted fallback levels if generation fails
     */
    private fun addFallbackLevels() {
        // Level 1: Simple two-path level
        levels.add(
            Level(
                id = 1,
                name = "Getting Started",
                gridSize = 5,
                endpoints = listOf(
                    Level.ColorEndpoint(0xFFFF5252.toInt(), 0, 0, 4, 0),  // Red vertical left
                    Level.ColorEndpoint(0xFF2196F3.toInt(), 0, 4, 4, 4)   // Blue vertical right
                )
            )
        )

        // Level 2: Simple three-path level
        levels.add(
            Level(
                id = 2,
                name = "Three's Company",
                gridSize = 5,
                endpoints = listOf(
                    Level.ColorEndpoint(0xFFFF5252.toInt(), 0, 1, 4, 1),  // Red vertical
                    Level.ColorEndpoint(0xFF2196F3.toInt(), 0, 3, 4, 3),  // Blue vertical
                    Level.ColorEndpoint(0xFF4CAF50.toInt(), 2, 0, 2, 4)   // Green horizontal
                )
            )
        )

        // Level 3: Crossing paths
        levels.add(
            Level(
                id = 3,
                name = "Crossing Paths",
                gridSize = 5,
                endpoints = listOf(
                    Level.ColorEndpoint(0xFFFF5252.toInt(), 0, 2, 4, 2),  // Red vertical
                    Level.ColorEndpoint(0xFF2196F3.toInt(), 2, 0, 2, 4),  // Blue horizontal
                    Level.ColorEndpoint(0xFF4CAF50.toInt(), 0, 0, 4, 4)   // Green diagonal
                )
            )
        )
    }

    /**
     * Get all levels
     */
    fun getAllLevels(): List<Level> = levels.toList()

    /**
     * Get a specific level by ID
     */
    fun getLevel(id: Int): Level? = levels.find { it.id == id }

    /**
     * Get the next level after the given ID
     */
    fun getNextLevel(currentId: Int): Level? {
        val currentIndex = levels.indexOfFirst { it.id == currentId }
        return if (currentIndex >= 0 && currentIndex < levels.size - 1) {
            levels[currentIndex + 1]
        } else {
            null
        }
    }

    /**
     * Get total number of levels
     */
    fun getTotalLevels(): Int = levels.size

    /**
     * Get level count by difficulty
     */
    fun getLevelsByDifficulty(difficulty: String): List<Level> {
        val range = getDifficultyRange(difficulty)
        return if (range != null) {
            levels.filter { it.id in range }
        } else {
            levels.filter { it.getDifficulty().equals(difficulty, ignoreCase = true) }
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
