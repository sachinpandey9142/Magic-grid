package com.magicgrid.data

/**
 * Manages 3-star rating system for levels
 * Stars are awarded based on:
 * 1. Completion (always 1 star)
 * 2. Time-based achievement (1 star if completed within target time)
 * 3. No hints used (1 star if 0 hints used)
 */
class StarManager {
    /**
     * Calculate stars for a level completion
     */
    fun calculateStars(
        levelId: Int,
        timeSeconds: Int,
        hintsUsed: Int,
        targetTimeSecond: Int = getTargetTime(levelId)
    ): Int {
        var stars = 1  // Always 1 star for completing

        // Star 2: Complete within target time
        if (timeSeconds <= targetTimeSecond) {
            stars++
        }

        // Star 3: No hints used
        if (hintsUsed == 0) {
            stars++
        }

        return stars.coerceIn(1, 3)
    }

    /**
     * Get target times for levels by difficulty
     * Returns time in seconds for 3-star achievement
     */
    fun getTargetTime(levelId: Int): Int {
        return when {
            levelId <= 5 -> 120    // Easy: 2 minutes
            levelId <= 8 -> 180    // Medium: 3 minutes
            else -> 240            // Hard: 4 minutes
        }
    }

    /**
     * Format seconds to mm:ss
     */
    fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", minutes, secs)
    }
}
