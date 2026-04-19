package com.magicgrid.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages game progress and level completion tracking with star system
 */
class ProgressManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("MagicGridProgress", Context.MODE_PRIVATE)

    companion object {
        // Testing override: set to false before shipping.
        private const val FORCE_UNLOCK_ALL_LEVELS = true
        private const val COMPLETED_LEVELS_KEY = "completed_levels"
        private const val CURRENT_LEVEL_KEY = "current_level"
        private const val HIGHEST_UNLOCKED_LEVEL_KEY = "highest_unlocked_level"
        private const val LEVEL_SCORES_KEY = "level_scores"
        private const val LEVEL_TIMES_KEY = "level_times"
        private const val LEVEL_STARS_KEY = "level_stars"
        private const val LEVEL_HINTS_KEY = "level_hints"
        private const val COINS_BALANCE_KEY = "coins_balance"
        private const val LEVEL_COIN_REWARD_KEY = "level_coin_reward"
        private const val FIRST_LEVEL_INTERSTITIAL_SHOWN_KEY = "first_level_interstitial_shown"
        private const val LAST_VIDEO_AD_MILESTONE_KEY = "last_video_ad_milestone"
        private const val DEFAULT_STARTING_COINS = 100
        private const val COINS_PER_STAR = 15
    }

    /**
     * Mark a level as completed
     */
    fun completeLevel(levelId: Int, moveCount: Int, timeSeconds: Int) {
        val completedLevels = getCompletedLevels().toMutableSet()
        completedLevels.add(levelId.toString())
        sharedPreferences.edit().putStringSet(COMPLETED_LEVELS_KEY, completedLevels).apply()

        unlockNextLevel(levelId)

        // Save best score (least moves)
        val scores = getLevelScores().toMutableMap()
        if (levelId.toString() !in scores || scores[levelId.toString()]!!.toInt() > moveCount) {
            scores[levelId.toString()] = moveCount.toString()
        }
        saveLevelScores(scores)

        // Save best time
        val times = getLevelTimes().toMutableMap()
        if (levelId.toString() !in times || times[levelId.toString()]!!.toInt() > timeSeconds) {
            times[levelId.toString()] = timeSeconds.toString()
        }
        saveLevelTimes(times)
    }

    /**
     * Get all completed level IDs
     */
    fun getCompletedLevels(): Set<String> {
        return sharedPreferences.getStringSet(COMPLETED_LEVELS_KEY, setOf()) ?: setOf()
    }

    /**
     * Check if a specific level is completed
     */
    fun isLevelCompleted(levelId: Int): Boolean {
        return levelId.toString() in getCompletedLevels()
    }

    /**
     * Set current/last played level
     */
    fun setCurrentLevel(levelId: Int) {
        val totalLevels = LevelRepository.getTotalLevels().coerceAtLeast(1)
        val normalized = levelId.coerceIn(1, totalLevels)
        sharedPreferences.edit().putInt(CURRENT_LEVEL_KEY, normalized).apply()
    }

    /**
     * Get current/last played level
     */
    fun getCurrentLevel(): Int {
        val totalLevels = LevelRepository.getTotalLevels().coerceAtLeast(1)
        val saved = sharedPreferences.getInt(CURRENT_LEVEL_KEY, 1).coerceIn(1, totalLevels)
        return saved.coerceAtMost(getHighestUnlockedLevel())
    }

    fun getHighestUnlockedLevel(): Int {
        val totalLevels = LevelRepository.getTotalLevels().coerceAtLeast(1)
        if (FORCE_UNLOCK_ALL_LEVELS) {
            return totalLevels
        }

        val stored = sharedPreferences
            .getInt(HIGHEST_UNLOCKED_LEVEL_KEY, 1)
            .coerceIn(1, totalLevels)

        val completedMax = getCompletedLevels()
            .mapNotNull { it.toIntOrNull() }
            .maxOrNull() ?: 0

        val derived = (completedMax + 1).coerceIn(1, totalLevels)
        val highest = maxOf(stored, derived)

        if (highest != stored) {
            sharedPreferences.edit().putInt(HIGHEST_UNLOCKED_LEVEL_KEY, highest).apply()
        }

        return highest
    }

    fun isLevelUnlocked(levelId: Int): Boolean {
        if (FORCE_UNLOCK_ALL_LEVELS) {
            val totalLevels = LevelRepository.getTotalLevels().coerceAtLeast(1)
            return levelId in 1..totalLevels
        }

        if (levelId <= 1) return true
        return levelId <= getHighestUnlockedLevel()
    }

    fun unlockNextLevel(completedLevelId: Int) {
        val totalLevels = LevelRepository.getTotalLevels().coerceAtLeast(1)
        val candidate = (completedLevelId + 1).coerceIn(1, totalLevels)
        val highest = getHighestUnlockedLevel()
        if (candidate > highest) {
            sharedPreferences.edit().putInt(HIGHEST_UNLOCKED_LEVEL_KEY, candidate).apply()
        }
    }

    /**
     * Get best move count for a level
     */
    fun getLevelBestMoves(levelId: Int): Int {
        val scores = getLevelScores()
        return scores[levelId.toString()]?.toIntOrNull() ?: -1
    }

    /**
     * Get best time for a level
     */
    fun getLevelBestTime(levelId: Int): Int {
        val times = getLevelTimes()
        return times[levelId.toString()]?.toIntOrNull() ?: -1
    }

    /**
     * Get overall stats
     */
    fun getStats(): GameStats {
        val completedCount = getCompletedLevels().size
        val totalLevels = LevelRepository.getTotalLevels()
        val completionPercentage = (completedCount * 100) / totalLevels

        return GameStats(
            completedLevels = completedCount,
            totalLevels = totalLevels,
            completionPercentage = completionPercentage
        )
    }

    /**
     * Get current coin balance
     */
    fun getCoins(): Int {
        return sharedPreferences.getInt(COINS_BALANCE_KEY, DEFAULT_STARTING_COINS).coerceAtLeast(0)
    }

    /**
     * Add coins to player balance and return the updated balance
     */
    fun addCoins(amount: Int): Int {
        if (amount <= 0) return getCoins()

        val updated = getCoins() + amount
        sharedPreferences.edit().putInt(COINS_BALANCE_KEY, updated).apply()
        return updated
    }

    /**
     * Spend coins from player balance
     */
    fun spendCoins(amount: Int): Boolean {
        if (amount <= 0) return true

        val current = getCoins()
        if (current < amount) return false

        sharedPreferences.edit().putInt(COINS_BALANCE_KEY, current - amount).apply()
        return true
    }

    /**
     * Reward coins based on best star improvement for a level
     * Prevents farming by replaying a level without improving stars.
     */
    fun grantCoinsForLevelResult(levelId: Int, starsEarned: Int): Int {
        val earned = starsEarned.coerceIn(1, 3)
        val rewardMap = getLevelCoinRewardMap().toMutableMap()
        val previouslyRewardedStars = rewardMap[levelId.toString()]?.toIntOrNull() ?: 0

        if (earned <= previouslyRewardedStars) {
            return 0
        }

        val starDelta = earned - previouslyRewardedStars
        val coinReward = starDelta * COINS_PER_STAR

        addCoins(coinReward)
        rewardMap[levelId.toString()] = earned.toString()
        saveMap(LEVEL_COIN_REWARD_KEY, rewardMap)

        return coinReward
    }

    /**
     * Returns true once when level 1 is completed for the first time, then false forever.
     */
    fun consumeFirstLevelInterstitialTrigger(levelId: Int): Boolean {
        if (levelId != 1) return false

        val alreadyShown = sharedPreferences.getBoolean(FIRST_LEVEL_INTERSTITIAL_SHOWN_KEY, false)
        if (alreadyShown) return false

        sharedPreferences.edit().putBoolean(FIRST_LEVEL_INTERSTITIAL_SHOWN_KEY, true).apply()
        return true
    }

    /**
     * Returns true once per completion milestone (3, 6, 9...) and never repeats the same milestone.
     */
    fun consumeVideoAdMilestoneTrigger(completedLevelsCount: Int): Boolean {
        if (completedLevelsCount < 3 || completedLevelsCount % 3 != 0) return false

        val milestone = completedLevelsCount / 3
        val lastShownMilestone = sharedPreferences.getInt(LAST_VIDEO_AD_MILESTONE_KEY, 0)
        if (milestone <= lastShownMilestone) return false

        sharedPreferences.edit().putInt(LAST_VIDEO_AD_MILESTONE_KEY, milestone).apply()
        return true
    }

    /**
     * Reset all progress
     */
    fun resetProgress() {
        sharedPreferences.edit()
            .clear()
            .putInt(HIGHEST_UNLOCKED_LEVEL_KEY, 1)
            .putInt(CURRENT_LEVEL_KEY, 1)
            .putInt(COINS_BALANCE_KEY, DEFAULT_STARTING_COINS)
            .apply()
    }

    private fun getLevelScores(): Map<String, String> {
        return parseMap(LEVEL_SCORES_KEY)
    }

    private fun saveLevelScores(scores: Map<String, String>) {
        saveMap(LEVEL_SCORES_KEY, scores)
    }

    private fun getLevelTimes(): Map<String, String> {
        return parseMap(LEVEL_TIMES_KEY)
    }

    private fun saveLevelTimes(times: Map<String, String>) {
        saveMap(LEVEL_TIMES_KEY, times)
    }

    /**
     * Save stars earned for a level
     */
    fun saveLevelStars(levelId: Int, stars: Int, hintsUsed: Int) {
        val earnedStars = stars.coerceIn(1, 3)
        
        val starsMap = getLevelStarsMap().toMutableMap()
        val currentStars = starsMap[levelId.toString()]?.toIntOrNull() ?: 0
        
        // Only update if new stars are better
        if (earnedStars > currentStars) {
            starsMap[levelId.toString()] = earnedStars.toString()
        }
        
        saveMap(LEVEL_STARS_KEY, starsMap)
        
        // Save hints used
        val hintsMap = getLevelHintsMap().toMutableMap()
        hintsMap[levelId.toString()] = hintsUsed.toString()

        saveMap(LEVEL_HINTS_KEY, hintsMap)
    }

    /**
     * Get stars earned for a level (0-3)
     */
    fun getLevelStars(levelId: Int): Int {
        return getLevelStarsMap()[levelId.toString()]?.toIntOrNull() ?: 0
    }

    /**
     * Get total stars across all completed levels
     */
    fun getTotalStars(): Int {
        return getLevelStarsMap().values.sumOf { it.toIntOrNull() ?: 0 }
    }

    /**
     * Get hints used for a level
     */
    fun getLevelHints(levelId: Int): Int {
        return getLevelHintsMap()[levelId.toString()]?.toIntOrNull() ?: 0
    }

    /**
     * Increment hints used for current level
     */
    fun incrementHintsUsed(levelId: Int) {
        val hintsMap = getLevelHintsMap().toMutableMap()
        val current = hintsMap[levelId.toString()]?.toIntOrNull() ?: 0
        hintsMap[levelId.toString()] = (current + 1).toString()
        
        val hintsSet = hintsMap.map { "${it.key}:${it.value}" }.toSet()
        sharedPreferences.edit().putStringSet(LEVEL_HINTS_KEY, hintsSet).apply()
    }

    private fun getLevelStarsMap(): Map<String, String> {
        return parseMap(LEVEL_STARS_KEY)
    }

    private fun getLevelHintsMap(): Map<String, String> {
        return parseMap(LEVEL_HINTS_KEY)
    }

    private fun getLevelCoinRewardMap(): Map<String, String> {
        return parseMap(LEVEL_COIN_REWARD_KEY)
    }

    private fun parseMap(key: String): Map<String, String> {
        val rawSet = sharedPreferences.getStringSet(key, emptySet()) ?: emptySet()
        if (rawSet.isEmpty()) return emptyMap()

        val result = mutableMapOf<String, String>()
        for (entry in rawSet) {
            val parts = entry.split(":", limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank()) {
                result[parts[0]] = parts[1]
            }
        }

        return result
    }

    private fun saveMap(key: String, map: Map<String, String>) {
        val set = map.map { "${it.key}:${it.value}" }.toSet()
        sharedPreferences.edit().putStringSet(key, set).apply()
    }

    /**
     * Stats data class
     */
    data class GameStats(
        val completedLevels: Int,
        val totalLevels: Int,
        val completionPercentage: Int
    )
}
