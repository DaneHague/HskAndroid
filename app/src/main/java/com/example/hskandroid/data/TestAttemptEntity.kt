package com.hskmaster.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "test_attempts")
@TypeConverters(Converters::class)
data class TestAttemptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val testId: String,
    val hskLevel: Int,
    val totalScore: Int,
    val totalQuestions: Int,
    val listeningScore: Int,
    val readingScore: Int,
    val completionTime: Long,
    val attemptDate: Long = System.currentTimeMillis(),
    val passed: Boolean,
    val answers: List<AnswerRecord>
)

data class AnswerRecord(
    val questionNumber: Int,
    val userAnswer: String,
    val correctAnswer: String,
    val isCorrect: Boolean,
    val questionType: String,
    val section: String // "listening" or "reading"
)

class Converters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromAnswerList(value: List<AnswerRecord>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toAnswerList(value: String): List<AnswerRecord> {
        val listType = object : TypeToken<List<AnswerRecord>>() {}.type
        return gson.fromJson(value, listType)
    }
}