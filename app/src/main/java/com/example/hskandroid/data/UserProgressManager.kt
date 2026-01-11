package com.hskmaster.app.data

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manages user progress including:
 * - Daily streaks
 * - XP and leveling system
 */
class UserProgressManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "user_progress"

        // Streak keys
        private const val KEY_CURRENT_STREAK = "current_streak"
        private const val KEY_LONGEST_STREAK = "longest_streak"
        private const val KEY_LAST_ACTIVE_DATE = "last_active_date"

        // XP keys
        private const val KEY_TOTAL_XP = "total_xp"
        private const val KEY_TODAY_XP = "today_xp"
        private const val KEY_TODAY_XP_DATE = "today_xp_date"

        // XP rewards
        const val XP_CORRECT_ANSWER = 10
        const val XP_WRONG_ANSWER = 2
        const val XP_GAME_COMPLETE = 25
        const val XP_PERFECT_GAME = 50

        // Level thresholds (exponential growth)
        val LEVEL_THRESHOLDS = listOf(
            0,      // Level 1
            100,    // Level 2
            250,    // Level 3
            500,    // Level 4
            850,    // Level 5
            1300,   // Level 6
            1900,   // Level 7
            2650,   // Level 8
            3550,   // Level 9
            4600,   // Level 10
            5800,   // Level 11
            7200,   // Level 12
            8800,   // Level 13
            10600,  // Level 14
            12600,  // Level 15
            14850,  // Level 16
            17350,  // Level 17
            20100,  // Level 18
            23100,  // Level 19
            26400,  // Level 20
            30000,  // Level 21
            34000,  // Level 22
            38500,  // Level 23
            43500,  // Level 24
            49000,  // Level 25
            55000,  // Level 26
            62000,  // Level 27
            70000,  // Level 28
            79000,  // Level 29
            89000   // Level 30
        )
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // ==================== STREAK SYSTEM ====================

    /**
     * Get current streak count
     */
    fun getCurrentStreak(): Int {
        checkAndUpdateStreak()
        return prefs.getInt(KEY_CURRENT_STREAK, 0)
    }

    /**
     * Get longest streak ever achieved
     */
    fun getLongestStreak(): Int {
        return prefs.getInt(KEY_LONGEST_STREAK, 0)
    }

    /**
     * Record activity for today (call this when user completes any game)
     */
    fun recordActivity() {
        val today = dateFormat.format(Date())
        val lastActiveDate = prefs.getString(KEY_LAST_ACTIVE_DATE, null)

        if (lastActiveDate == today) {
            // Already recorded activity today
            return
        }

        val yesterday = getYesterdayDate()
        var currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0)

        when (lastActiveDate) {
            yesterday -> {
                // Consecutive day - increment streak
                currentStreak++
            }
            null -> {
                // First time user
                currentStreak = 1
            }
            else -> {
                // Streak broken - start fresh
                currentStreak = 1
            }
        }

        val longestStreak = prefs.getInt(KEY_LONGEST_STREAK, 0)

        prefs.edit()
            .putInt(KEY_CURRENT_STREAK, currentStreak)
            .putInt(KEY_LONGEST_STREAK, maxOf(longestStreak, currentStreak))
            .putString(KEY_LAST_ACTIVE_DATE, today)
            .apply()
    }

    /**
     * Check if user was active today
     */
    fun wasActiveToday(): Boolean {
        val today = dateFormat.format(Date())
        val lastActiveDate = prefs.getString(KEY_LAST_ACTIVE_DATE, null)
        return lastActiveDate == today
    }

    /**
     * Check if streak is at risk (user hasn't practiced today)
     */
    fun isStreakAtRisk(): Boolean {
        val currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0)
        return currentStreak > 0 && !wasActiveToday()
    }

    private fun checkAndUpdateStreak() {
        val today = dateFormat.format(Date())
        val yesterday = getYesterdayDate()
        val lastActiveDate = prefs.getString(KEY_LAST_ACTIVE_DATE, null)

        // If last activity was before yesterday, reset streak
        if (lastActiveDate != null && lastActiveDate != today && lastActiveDate != yesterday) {
            prefs.edit()
                .putInt(KEY_CURRENT_STREAK, 0)
                .apply()
        }
    }

    private fun getYesterdayDate(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        return dateFormat.format(calendar.time)
    }

    // ==================== XP SYSTEM ====================

    /**
     * Get total XP earned
     */
    fun getTotalXp(): Int {
        return prefs.getInt(KEY_TOTAL_XP, 0)
    }

    /**
     * Get XP earned today
     */
    fun getTodayXp(): Int {
        val today = dateFormat.format(Date())
        val storedDate = prefs.getString(KEY_TODAY_XP_DATE, null)

        return if (storedDate == today) {
            prefs.getInt(KEY_TODAY_XP, 0)
        } else {
            0
        }
    }

    /**
     * Add XP for correct answer
     */
    fun addCorrectAnswerXp(): Int {
        return addXp(XP_CORRECT_ANSWER)
    }

    /**
     * Add XP for wrong answer (participation)
     */
    fun addWrongAnswerXp(): Int {
        return addXp(XP_WRONG_ANSWER)
    }

    /**
     * Add XP for completing a game
     */
    fun addGameCompleteXp(): Int {
        return addXp(XP_GAME_COMPLETE)
    }

    /**
     * Add XP for perfect game (100% correct)
     */
    fun addPerfectGameXp(): Int {
        return addXp(XP_PERFECT_GAME)
    }

    /**
     * Add custom XP amount
     */
    fun addXp(amount: Int): Int {
        val today = dateFormat.format(Date())
        val storedDate = prefs.getString(KEY_TODAY_XP_DATE, null)

        val currentTotalXp = prefs.getInt(KEY_TOTAL_XP, 0)
        val currentTodayXp = if (storedDate == today) {
            prefs.getInt(KEY_TODAY_XP, 0)
        } else {
            0
        }

        val newTotalXp = currentTotalXp + amount
        val newTodayXp = currentTodayXp + amount

        prefs.edit()
            .putInt(KEY_TOTAL_XP, newTotalXp)
            .putInt(KEY_TODAY_XP, newTodayXp)
            .putString(KEY_TODAY_XP_DATE, today)
            .apply()

        // Also record activity for streak
        recordActivity()

        return amount
    }

    // ==================== LEVEL SYSTEM ====================

    /**
     * Get current level based on total XP
     */
    fun getCurrentLevel(): Int {
        val totalXp = getTotalXp()
        var level = 1

        for (i in LEVEL_THRESHOLDS.indices) {
            if (totalXp >= LEVEL_THRESHOLDS[i]) {
                level = i + 1
            } else {
                break
            }
        }

        return minOf(level, LEVEL_THRESHOLDS.size)
    }

    /**
     * Get XP required for next level
     */
    fun getXpForNextLevel(): Int {
        val currentLevel = getCurrentLevel()
        return if (currentLevel < LEVEL_THRESHOLDS.size) {
            LEVEL_THRESHOLDS[currentLevel]
        } else {
            LEVEL_THRESHOLDS.last()
        }
    }

    /**
     * Get XP threshold for current level
     */
    fun getXpForCurrentLevel(): Int {
        val currentLevel = getCurrentLevel()
        return if (currentLevel > 0 && currentLevel <= LEVEL_THRESHOLDS.size) {
            LEVEL_THRESHOLDS[currentLevel - 1]
        } else {
            0
        }
    }

    /**
     * Get progress percentage to next level (0.0 to 1.0)
     */
    fun getLevelProgress(): Float {
        val totalXp = getTotalXp()
        val currentLevelXp = getXpForCurrentLevel()
        val nextLevelXp = getXpForNextLevel()

        if (nextLevelXp == currentLevelXp) return 1f

        val xpIntoLevel = totalXp - currentLevelXp
        val xpNeeded = nextLevelXp - currentLevelXp

        return (xpIntoLevel.toFloat() / xpNeeded.toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Get XP remaining until next level
     */
    fun getXpToNextLevel(): Int {
        val totalXp = getTotalXp()
        val nextLevelXp = getXpForNextLevel()
        return (nextLevelXp - totalXp).coerceAtLeast(0)
    }

    /**
     * Get level title based on level number
     */
    fun getLevelTitle(): String {
        return when (getCurrentLevel()) {
            in 1..5 -> "Beginner"
            in 6..10 -> "Elementary"
            in 11..15 -> "Intermediate"
            in 16..20 -> "Advanced"
            in 21..25 -> "Expert"
            else -> "Master"
        }
    }

    // ==================== STATS ====================

    /**
     * Get combined progress stats
     */
    fun getProgressStats(): ProgressStats {
        return ProgressStats(
            currentStreak = getCurrentStreak(),
            longestStreak = getLongestStreak(),
            totalXp = getTotalXp(),
            todayXp = getTodayXp(),
            currentLevel = getCurrentLevel(),
            levelProgress = getLevelProgress(),
            xpToNextLevel = getXpToNextLevel(),
            levelTitle = getLevelTitle(),
            isStreakAtRisk = isStreakAtRisk()
        )
    }
}

data class ProgressStats(
    val currentStreak: Int,
    val longestStreak: Int,
    val totalXp: Int,
    val todayXp: Int,
    val currentLevel: Int,
    val levelProgress: Float,
    val xpToNextLevel: Int,
    val levelTitle: String,
    val isStreakAtRisk: Boolean
)
