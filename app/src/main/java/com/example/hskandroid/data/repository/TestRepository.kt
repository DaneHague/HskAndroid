package com.hskmaster.app.data.repository

import android.content.Context
import com.hskmaster.app.data.AnswerRecord
import com.hskmaster.app.data.TestAttemptEntity
import com.hskmaster.app.data.database.HskDatabase
import com.hskmaster.app.model.HskTest
import com.hskmaster.app.model.TestAnswer
import com.hskmaster.app.model.TestResult
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

class TestRepository(private val context: Context) {
    private val gson = Gson()
    private val database = HskDatabase.getDatabase(context)
    private val testAttemptDao = database.testAttemptDao()
    
    suspend fun loadTest(testFileName: String): HskTest? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.assets.open(testFileName)
            val reader = InputStreamReader(inputStream)
            gson.fromJson(reader, HskTest::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun getAvailableTests(): List<String> = withContext(Dispatchers.IO) {
        try {
            context.assets.list("Hsk1Tests")
                ?.filter { it.endsWith(".json") }
                ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    fun calculateTestResult(
        test: HskTest,
        userAnswers: Map<Int, String>,
        completionTime: Long
    ): TestResult {
        val answers = mutableListOf<TestAnswer>()
        var listeningCorrect = 0
        var readingCorrect = 0
        
        // Process listening section
        test.sections.listening.parts.forEach { (_, part) ->
            part.questions.forEach { question ->
                val userAnswer = userAnswers[question.questionNumber]
                val isCorrect = userAnswer == question.answer
                answers.add(TestAnswer(question.questionNumber, userAnswer, isCorrect))
                if (isCorrect) listeningCorrect++
            }
        }
        
        // Process reading section
        test.sections.reading.parts.forEach { (_, part) ->
            part.questions.forEach { question ->
                val userAnswer = userAnswers[question.questionNumber]
                val isCorrect = userAnswer == question.answer
                answers.add(TestAnswer(question.questionNumber, userAnswer, isCorrect))
                if (isCorrect) readingCorrect++
            }
        }
        
        val totalQuestions = answers.size
        val totalScore = listeningCorrect + readingCorrect
        
        return TestResult(
            testId = test.testId,
            level = test.level,
            listeningScore = listeningCorrect,
            readingScore = readingCorrect,
            totalScore = totalScore,
            totalQuestions = totalQuestions,
            completionTime = completionTime,
            answers = answers
        )
    }
    
    suspend fun saveTestResult(
        test: HskTest,
        result: TestResult,
        userAnswers: Map<Int, String>,
        hskLevel: Int
    ): Long = withContext(Dispatchers.IO) {
        val answerRecords = mutableListOf<AnswerRecord>()
        
        // Process listening section
        test.sections.listening.parts.forEach { (_, part) ->
            part.questions.forEach { question ->
                val userAnswer = userAnswers[question.questionNumber] ?: ""
                answerRecords.add(
                    AnswerRecord(
                        questionNumber = question.questionNumber,
                        userAnswer = userAnswer,
                        correctAnswer = question.answer,
                        isCorrect = userAnswer == question.answer,
                        questionType = question.type,
                        section = "listening"
                    )
                )
            }
        }
        
        // Process reading section
        test.sections.reading.parts.forEach { (_, part) ->
            part.questions.forEach { question ->
                val userAnswer = userAnswers[question.questionNumber] ?: ""
                answerRecords.add(
                    AnswerRecord(
                        questionNumber = question.questionNumber,
                        userAnswer = userAnswer,
                        correctAnswer = question.answer,
                        isCorrect = userAnswer == question.answer,
                        questionType = question.type,
                        section = "reading"
                    )
                )
            }
        }
        
        val percentage = (result.totalScore * 100.0 / result.totalQuestions)
        val attempt = TestAttemptEntity(
            testId = test.testId,
            hskLevel = hskLevel,
            totalScore = result.totalScore,
            totalQuestions = result.totalQuestions,
            listeningScore = result.listeningScore,
            readingScore = result.readingScore,
            completionTime = result.completionTime,
            passed = percentage >= 60.0,
            answers = answerRecords
        )
        
        testAttemptDao.insertTestAttempt(attempt)
    }
    
    fun getTestAttempts(testId: String): Flow<List<TestAttemptEntity>> {
        return testAttemptDao.getTestAttempts(testId)
    }
    
    suspend fun getTestAttemptById(attemptId: Long): TestAttemptEntity? {
        return testAttemptDao.getTestAttemptById(attemptId)
    }
    
    suspend fun getTestStats(testId: String): TestStats {
        val totalAttempts = testAttemptDao.getTotalAttemptsCount(testId)
        val passedAttempts = testAttemptDao.getPassedAttemptsCount(testId)
        val bestScore = testAttemptDao.getBestScorePercentage(testId) ?: 0.0
        
        return TestStats(
            totalAttempts = totalAttempts,
            passedAttempts = passedAttempts,
            bestScorePercentage = bestScore
        )
    }
}

data class TestStats(
    val totalAttempts: Int,
    val passedAttempts: Int,
    val bestScorePercentage: Double
)