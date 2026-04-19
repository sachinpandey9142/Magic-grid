package com.magicgrid.data

/**
 * Manages 3-star rating system for levels
 * Stars are awarded based on:
 * 1. Completion (always 1 star)
 * 2. Win within the target time (2 stars)
 * 3. Win within target time and use no hints (3 stars)
 */
class StarManager {

    companion object {
        private const val TIME_LIMIT_EARLY = 120
        private const val TIME_LIMIT_EARLY_MID = 90
        private const val TIME_LIMIT_MID = 60
        private const val TIME_LIMIT_LATE = 45
        private const val TIME_LIMIT_EXPERT = 30
        private const val TIME_LIMIT_FINAL = 15
    }

    /**
     * Calculate stars for a level completion
     */
    fun calculateStars(
        levelId: Int,
        timeSeconds: Int,
        hintsUsed: Int,
        targetTimeSecond: Int = getTargetTime(levelId)
    ): Int {
        val completedWithinTime = timeSeconds <= targetTimeSecond

        return when {
            completedWithinTime && hintsUsed == 0 -> 3
            completedWithinTime -> 2
            else -> 1
        }
    }

    /**
     * Returns target time in seconds for in-time completion.
     */
    fun getTargetTime(levelId: Int): Int {
        return when (levelId) {
            in 1..30 -> TIME_LIMIT_EARLY
            in 31..50 -> TIME_LIMIT_EARLY_MID
            in 51..70 -> TIME_LIMIT_MID
            in 71..85 -> TIME_LIMIT_LATE
            in 86..95 -> TIME_LIMIT_EXPERT
            else -> TIME_LIMIT_FINAL
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
