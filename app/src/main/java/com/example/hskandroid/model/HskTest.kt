package com.hskmaster.app.model

import com.google.gson.annotations.SerializedName

data class HskTest(
    val testId: String,
    val level: String,
    val title: String,
    val sections: TestSections
)

data class TestSections(
    val listening: TestSection,
    val reading: TestSection
)

data class TestSection(
    val title: String,
    val parts: Map<String, TestPart>
)

data class TestPart(
    val title: String,
    val instructions: String,
    val questions: List<TestQuestion>,
    val options: Map<String, String>? = null // For shared options in some parts
)

data class TestQuestion(
    val questionNumber: Int,
    val type: String,
    val audioScript: String? = null,
    val prompt: String? = null,
    val imagePath: String? = null,
    val options: Any? = null, // Can be List<String> or Map<String, String>
    val answer: String
)

// User's answer tracking
data class TestAnswer(
    val questionNumber: Int,
    val userAnswer: String?,
    val isCorrect: Boolean
)

data class TestResult(
    val testId: String,
    val level: String,
    val listeningScore: Int,
    val readingScore: Int,
    val totalScore: Int,
    val totalQuestions: Int,
    val completionTime: Long,
    val answers: List<TestAnswer>
)