package com.example.hskandroid.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningRecordDao {
    @Insert
    suspend fun insertRecord(record: LearningRecord)
    
    @Query("SELECT * FROM learning_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<LearningRecord>>
    
    @Query("SELECT * FROM learning_records WHERE hskLevel = :level ORDER BY timestamp DESC")
    fun getRecordsByLevel(level: Int): Flow<List<LearningRecord>>
    
    @Query("SELECT * FROM learning_records WHERE gameType = :gameType ORDER BY timestamp DESC")
    fun getRecordsByGameType(gameType: String): Flow<List<LearningRecord>>
    
    @Query("SELECT * FROM learning_records WHERE character = :character ORDER BY timestamp DESC")
    fun getRecordsByCharacter(character: String): Flow<List<LearningRecord>>
    
    @Query("""
        SELECT * FROM learning_records 
        WHERE timestamp >= :startTime AND timestamp <= :endTime 
        ORDER BY timestamp DESC
    """)
    fun getRecordsByDateRange(startTime: Long, endTime: Long): Flow<List<LearningRecord>>
    
    @Query("""
        SELECT 
            character,
            pinyin,
            COUNT(*) as totalAttempts,
            SUM(CASE WHEN isCorrect = 1 THEN 1 ELSE 0 END) as correctCount,
            MAX(timestamp) as lastSeen,
            CAST(SUM(CASE WHEN isCorrect = 1 THEN 1 ELSE 0 END) AS FLOAT) / COUNT(*) * 100 as mastery
        FROM learning_records
        GROUP BY character
        ORDER BY mastery DESC
    """)
    fun getCharacterProgress(): Flow<List<CharacterProgressEntity>>
    
    @Query("""
        SELECT 
            gameType,
            COUNT(*) as attempts,
            SUM(CASE WHEN isCorrect = 1 THEN 1 ELSE 0 END) as correct,
            CAST(SUM(CASE WHEN isCorrect = 1 THEN 1 ELSE 0 END) AS FLOAT) / COUNT(*) * 100 as accuracy
        FROM learning_records
        WHERE timestamp >= :startTime
        GROUP BY gameType
    """)
    suspend fun getGameStatsByDate(startTime: Long): List<GameStatsEntity>
    
    @Query("SELECT COUNT(DISTINCT character) FROM learning_records WHERE timestamp >= :startTime")
    suspend fun getUniqueCharacterCount(startTime: Long): Int
    
    @Query("DELETE FROM learning_records")
    suspend fun deleteAllRecords()
    
    @Query("DELETE FROM learning_records WHERE timestamp < :beforeTime")
    suspend fun deleteOldRecords(beforeTime: Long)
}

data class CharacterProgressEntity(
    val character: String,
    val pinyin: String,
    val totalAttempts: Int,
    val correctCount: Int,
    val lastSeen: Long,
    val mastery: Float
)

data class GameStatsEntity(
    val gameType: String,
    val attempts: Int,
    val correct: Int,
    val accuracy: Float
)