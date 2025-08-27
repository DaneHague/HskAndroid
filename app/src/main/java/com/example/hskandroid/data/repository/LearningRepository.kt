package com.example.hskandroid.data.repository

import android.content.Context
import com.example.hskandroid.data.database.*
import com.example.hskandroid.model.HskWord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*

class LearningRepository(context: Context) {
    private val dao = HskDatabase.getDatabase(context).learningRecordDao()
    
    suspend fun recordMatchingGame(
        hskLevel: Int,
        word: HskWord,
        isCorrect: Boolean,
        responseTime: Long,
        attempts: Int
    ) {
        val record = LearningRecord(
            hskLevel = hskLevel,
            gameType = "matching",
            character = word.simplified,
            pinyin = word.forms.firstOrNull()?.transcriptions?.pinyin ?: "",
            meaning = word.forms.firstOrNull()?.meanings?.firstOrNull() ?: "",
            isCorrect = isCorrect,
            responseTime = responseTime,
            attempts = attempts
        )
        dao.insertRecord(record)
    }
    
    suspend fun recordQuizAnswer(
        hskLevel: Int,
        word: HskWord,
        isCorrect: Boolean,
        questionType: String,
        responseTime: Long
    ) {
        val record = LearningRecord(
            hskLevel = hskLevel,
            gameType = "quiz",
            character = word.simplified,
            pinyin = word.forms.firstOrNull()?.transcriptions?.pinyin ?: "",
            meaning = word.forms.firstOrNull()?.meanings?.firstOrNull() ?: "",
            isCorrect = isCorrect,
            responseTime = responseTime,
            questionType = questionType
        )
        dao.insertRecord(record)
    }
    
    suspend fun recordWritingPractice(
        hskLevel: Int,
        word: HskWord,
        hintUsed: Boolean,
        isCorrect: Boolean = true // Allow caller to specify if it was correct
    ) {
        val record = LearningRecord(
            hskLevel = hskLevel,
            gameType = "writing",
            character = word.simplified,
            pinyin = word.forms.firstOrNull()?.transcriptions?.pinyin ?: "",
            meaning = word.forms.firstOrNull()?.meanings?.firstOrNull() ?: "",
            isCorrect = isCorrect,
            hintUsed = hintUsed
        )
        dao.insertRecord(record)
    }
    
    suspend fun recordListeningAnswer(
        hskLevel: Int,
        word: HskWord,
        isCorrect: Boolean,
        responseTime: Long
    ) {
        val record = LearningRecord(
            hskLevel = hskLevel,
            gameType = "listening",
            character = word.simplified,
            pinyin = word.forms.firstOrNull()?.transcriptions?.pinyin ?: "",
            meaning = word.forms.firstOrNull()?.meanings?.firstOrNull() ?: "",
            isCorrect = isCorrect,
            responseTime = responseTime,
            questionType = "listening"
        )
        dao.insertRecord(record)
    }
    
    fun getAllRecords(): Flow<List<LearningRecord>> = dao.getAllRecords()
    
    fun getRecordsByLevel(level: Int): Flow<List<LearningRecord>> = dao.getRecordsByLevel(level)
    
    fun getRecordsByGameType(gameType: String): Flow<List<LearningRecord>> = dao.getRecordsByGameType(gameType)
    
    fun getCharacterProgress(): Flow<List<CharacterProgress>> {
        return dao.getCharacterProgress().map { entities ->
            entities.map { entity ->
                CharacterProgress(
                    character = entity.character,
                    pinyin = entity.pinyin,
                    totalAttempts = entity.totalAttempts,
                    correctCount = entity.correctCount,
                    lastSeen = entity.lastSeen,
                    mastery = entity.mastery
                )
            }
        }
    }
    
    suspend fun getDailyStats(date: Date = Date()): DailyStats {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endTime = calendar.timeInMillis
        
        val records = dao.getRecordsByDateRange(startTime, endTime).first()
        
        val totalAttempts = records.size
        val correctCount = records.count { it.isCorrect }
        val wrongCount = totalAttempts - correctCount
        val accuracyRate = if (totalAttempts > 0) {
            (correctCount.toFloat() / totalAttempts) * 100
        } else 0f
        
        val uniqueCharacters = records.map { it.character }.distinct().size
        
        val gameStats = dao.getGameStatsByDate(startTime)
        val gameBreakdown = gameStats.associate { 
            it.gameType to GameStats(
                gameType = it.gameType,
                attempts = it.attempts,
                correct = it.correct,
                accuracy = it.accuracy
            )
        }
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        return DailyStats(
            date = dateFormat.format(date),
            totalAttempts = totalAttempts,
            correctCount = correctCount,
            wrongCount = wrongCount,
            accuracyRate = accuracyRate,
            charactersLearned = uniqueCharacters,
            gameBreakdown = gameBreakdown
        )
    }
    
    suspend fun getWeeklyStats(): List<DailyStats> {
        val stats = mutableListOf<DailyStats>()
        val calendar = Calendar.getInstance()
        
        for (i in 0..6) {
            stats.add(getDailyStats(calendar.time))
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }
        
        return stats.reversed()
    }
    
    suspend fun clearAllData() {
        dao.deleteAllRecords()
    }
    
    suspend fun clearOldData(daysToKeep: Int = 30) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -daysToKeep)
        dao.deleteOldRecords(calendar.timeInMillis)
    }
    
    suspend fun getUniqueWordsByLevel(level: Int): Int {
        val records = dao.getRecordsByLevel(level).first()
        return records.map { it.character }.distinct().size
    }
    
    suspend fun getWordMasteryByLevel(level: Int): Map<String, Float> {
        val records = dao.getRecordsByLevel(level).first()
        
        val wordStats = records.groupBy { it.character }
            .mapValues { (_, records) ->
                val correctCount = records.count { it.isCorrect }
                val totalCount = records.size
                if (totalCount > 0) correctCount.toFloat() / totalCount else 0f
            }
        
        return wordStats
    }
}