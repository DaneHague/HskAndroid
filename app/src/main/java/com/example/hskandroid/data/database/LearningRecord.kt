package com.hskmaster.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "learning_records")
data class LearningRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val hskLevel: Int,
    val gameType: String, // "matching", "quiz", "writing"
    val character: String,
    val pinyin: String,
    val meaning: String,
    val isCorrect: Boolean,
    val responseTime: Long? = null, // Time taken to answer in milliseconds
    val attempts: Int = 1,
    val hintUsed: Boolean = false,
    val questionType: String? = null // For quiz: "CHARACTER_TO_PINYIN", etc.
)

enum class GameType(val displayName: String) {
    MATCHING("Matching Game"),
    QUIZ("Quiz"),
    WRITING("Writing Practice")
}

data class DailyStats(
    val date: String,
    val totalAttempts: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val accuracyRate: Float,
    val charactersLearned: Int,
    val gameBreakdown: Map<String, GameStats>
)

data class GameStats(
    val gameType: String,
    val attempts: Int,
    val correct: Int,
    val accuracy: Float
)

data class CharacterProgress(
    val character: String,
    val pinyin: String,
    val totalAttempts: Int,
    val correctCount: Int,
    val lastSeen: Long,
    val mastery: Float // 0 to 100
)