package com.hskmaster.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TestAttemptDao {
    @Insert
    suspend fun insertTestAttempt(attempt: TestAttemptEntity): Long
    
    @Query("SELECT * FROM test_attempts WHERE testId = :testId ORDER BY attemptDate DESC")
    fun getTestAttempts(testId: String): Flow<List<TestAttemptEntity>>
    
    @Query("SELECT * FROM test_attempts WHERE id = :attemptId")
    suspend fun getTestAttemptById(attemptId: Long): TestAttemptEntity?
    
    @Query("SELECT * FROM test_attempts WHERE hskLevel = :level ORDER BY attemptDate DESC")
    fun getTestAttemptsByLevel(level: Int): Flow<List<TestAttemptEntity>>
    
    @Query("DELETE FROM test_attempts WHERE id = :attemptId")
    suspend fun deleteTestAttempt(attemptId: Long)
    
    @Query("SELECT COUNT(*) FROM test_attempts WHERE testId = :testId AND passed = 1")
    suspend fun getPassedAttemptsCount(testId: String): Int
    
    @Query("SELECT COUNT(*) FROM test_attempts WHERE testId = :testId")
    suspend fun getTotalAttemptsCount(testId: String): Int
    
    @Query("SELECT MAX(totalScore * 100.0 / totalQuestions) FROM test_attempts WHERE testId = :testId")
    suspend fun getBestScorePercentage(testId: String): Double?
}