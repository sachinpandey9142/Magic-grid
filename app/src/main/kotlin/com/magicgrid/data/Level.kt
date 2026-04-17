package com.magicgrid.data

/**
 * Represents a single game level.
 * Contains grid size and endpoint positions/colors.
 */
data class Level(
    val id: Int,
    val name: String,
    val gridSize: Int, // For square grids (e.g., 5 for 5x5)
    val rows: Int = gridSize,
    val cols: Int = gridSize,
    val endpoints: List<ColorEndpoint> // List of color endpoints
) {
    data class ColorEndpoint(
        val color: Int,
        val startRow: Int,
        val startCol: Int,
        val endRow: Int,
        val endCol: Int
    )

    /**
     * Get difficulty rating based on grid size and number of colors
     */
    fun getDifficulty(): String {
        return when {
            gridSize <= 5 && endpoints.size <= 3 -> "Easy"
            gridSize <= 6 && endpoints.size <= 5 -> "Medium"
            gridSize <= 7 && endpoints.size <= 6 -> "Hard"
            else -> "Expert"
        }
    }

    override fun toString(): String {
        return "Level(id=$id, name='$name', grid=${gridSize}x${gridSize}, colors=${endpoints.size})"
    }
}
