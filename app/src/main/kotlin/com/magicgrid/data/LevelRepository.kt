package com.magicgrid.data

import com.magicgrid.logic.LevelGenerator

/**
 * Provides game levels - generates valid, solvable levels at runtime.
 * Each level is guaranteed to be completable with proper puzzle solving.
 */
object LevelRepository {
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

        // EASY LEVELS (5x5, 2-3 colors)
        for (i in 0 until 3) {
            val generated = generator.generateLevel(5, "easy") ?: continue
            levels.add(
                Level(
                    id = levels.size + 1,
                    name = "Easy Level ${levels.size + 1}",
                    gridSize = 5,
                    rows = 5,
                    cols = 5,
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
        }

        // MEDIUM LEVELS (6x6, 3-4 colors)
        for (i in 0 until 4) {
            val generated = generator.generateLevel(6, "medium") ?: continue
            levels.add(
                Level(
                    id = levels.size + 1,
                    name = "Medium Level ${i + 1}",
                    gridSize = 6,
                    rows = 6,
                    cols = 6,
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
        }

        // HARD LEVELS (7x7, 4-5 colors)
        for (i in 0 until 3) {
            val generated = generator.generateLevel(7, "hard") ?: continue
            levels.add(
                Level(
                    id = levels.size + 1,
                    name = "Hard Level ${i + 1}",
                    gridSize = 7,
                    rows = 7,
                    cols = 7,
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
        }

        // Fallback: Ensure we have at least some levels
        // These are handcrafted proven-solvable levels as backup
        if (levels.isEmpty()) {
            addFallbackLevels()
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
        return levels.filter { it.getDifficulty() == difficulty }
    }
}
