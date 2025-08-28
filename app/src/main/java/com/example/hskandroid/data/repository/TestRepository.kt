package com.example.hskandroid.data.repository

import android.content.Context
import com.example.hskandroid.model.HskTest
import com.example.hskandroid.model.TestAnswer
import com.example.hskandroid.model.TestResult
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

class TestRepository(private val context: Context) {
    private val gson = Gson()
    
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
}