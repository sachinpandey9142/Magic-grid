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
        const val TARGET_TIME_SECONDS = 60
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
    fun getTargetTime(@Suppress("UNUSED_PARAMETER") levelId: Int): Int {
        return TARGET_TIME_SECONDS
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
