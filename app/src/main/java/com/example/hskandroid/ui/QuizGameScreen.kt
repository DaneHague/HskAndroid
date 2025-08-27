package com.example.hskandroid.ui

import android.speech.tts.TextToSpeech
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hskandroid.model.HskWord
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random

data class QuizQuestion(
    val word: HskWord,
    val questionType: QuestionType,
    val question: String,
    val correctAnswer: String,
    val options: List<String>
)

enum class QuestionType {
    CHARACTER_TO_PINYIN,
    CHARACTER_TO_MEANING,
    PINYIN_TO_CHARACTER,
    PINYIN_TO_MEANING,
    MEANING_TO_CHARACTER,
    MEANING_TO_PINYIN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizGameScreen(
    vocabulary: List<HskWord>,
    hskLevel: Int = 1,
    repository: com.example.hskandroid.data.repository.LearningRepository? = null,
    onBackPressed: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var showResult by remember { mutableStateOf(false) }
    var isQuizComplete by remember { mutableStateOf(false) }
    var timeRemaining by remember { mutableStateOf(30) }
    var isPaused by remember { mutableStateOf(false) }
    
    // Text-to-Speech setup
    val context = LocalContext.current
    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsInitialized by remember { mutableStateOf(false) }
    
    DisposableEffect(context) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.let { tts ->
                    // Set Chinese language for proper pronunciation
                    val result = tts.setLanguage(Locale.CHINESE)
                    if (result != TextToSpeech.LANG_MISSING_DATA && 
                        result != TextToSpeech.LANG_NOT_SUPPORTED) {
                        ttsInitialized = true
                    }
                }
            }
        }
        onDispose {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        }
    }
    
    val totalQuestions = 10
    val questions = remember(vocabulary) {
        generateQuizQuestions(vocabulary.shuffled(), totalQuestions)
    }
    
    val currentQuestion = if (currentQuestionIndex < questions.size) {
        questions[currentQuestionIndex]
    } else null
    
    // Speak the character when a new question appears
    LaunchedEffect(currentQuestion, ttsInitialized) {
        if (ttsInitialized && currentQuestion != null) {
            // Check if the question type shows a Chinese character
            when (currentQuestion.questionType) {
                QuestionType.CHARACTER_TO_PINYIN,
                QuestionType.CHARACTER_TO_MEANING -> {
                    // The question itself is the Chinese character
                    textToSpeech?.speak(
                        currentQuestion.question,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "quiz_character"
                    )
                }
                QuestionType.MEANING_TO_CHARACTER,
                QuestionType.PINYIN_TO_CHARACTER -> {
                    // The correct answer is the Chinese character
                    // We'll speak it when they get the answer right
                }
                else -> {
                    // Other question types don't show Chinese characters initially
                }
            }
        }
    }
    
    LaunchedEffect(currentQuestionIndex, isPaused, showResult) {
        if (!isPaused && !showResult && !isQuizComplete) {
            timeRemaining = 30
            while (timeRemaining > 0 && !isPaused && !showResult) {
                delay(1000)
                timeRemaining--
            }
            if (timeRemaining == 0 && !showResult) {
                showResult = true
            }
        }
    }
    
    LaunchedEffect(showResult) {
        if (showResult) {
            delay(3000) // Increased delay to show meaning
            if (currentQuestionIndex < questions.size - 1) {
                currentQuestionIndex++
                selectedAnswer = null
                showResult = false
            } else {
                isQuizComplete = true
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "HSK $hskLevel Quiz",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (onBackPressed != null) {
                        IconButton(onClick = onBackPressed) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { isPaused = !isPaused }) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Refresh,
                            contentDescription = if (isPaused) "Resume" else "Pause"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        if (isQuizComplete) {
            QuizCompleteScreen(
                score = score,
                totalQuestions = totalQuestions,
                onRestartQuiz = {
                    currentQuestionIndex = 0
                    score = 0
                    selectedAnswer = null
                    showResult = false
                    isQuizComplete = false
                },
                modifier = Modifier.padding(paddingValues)
            )
        } else if (currentQuestion != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                QuizProgressBar(
                    currentQuestion = currentQuestionIndex + 1,
                    totalQuestions = totalQuestions,
                    score = score
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TimerBar(
                    timeRemaining = timeRemaining,
                    totalTime = 30,
                    isPaused = isPaused
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                QuestionCard(
                    question = currentQuestion,
                    questionNumber = currentQuestionIndex + 1,
                    onSpeakCharacter = {
                        if (ttsInitialized) {
                            when (currentQuestion.questionType) {
                                QuestionType.CHARACTER_TO_PINYIN,
                                QuestionType.CHARACTER_TO_MEANING -> {
                                    textToSpeech?.speak(
                                        currentQuestion.question,
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        "quiz_character"
                                    )
                                }
                                else -> {}
                            }
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                val coroutineScope = rememberCoroutineScope()
                var answerTime by remember { mutableStateOf(0L) }
                
                LaunchedEffect(currentQuestionIndex) {
                    answerTime = System.currentTimeMillis()
                }
                
                AnswerOptions(
                    question = currentQuestion,
                    options = currentQuestion.options,
                    selectedAnswer = selectedAnswer,
                    correctAnswer = if (showResult) currentQuestion.correctAnswer else null,
                    showResult = showResult,
                    onSpeakCharacter = {
                        if (ttsInitialized) {
                            textToSpeech?.speak(
                                currentQuestion.word.simplified,
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                "feedback_character"
                            )
                        }
                    },
                    onAnswerSelected = { answer ->
                        if (!showResult) {
                            selectedAnswer = answer
                            showResult = true
                            val isCorrect = answer == currentQuestion.correctAnswer
                            if (isCorrect) {
                                score++
                                // Speak the Chinese character when correct
                                if (ttsInitialized) {
                                    textToSpeech?.speak(
                                        currentQuestion.word.simplified,
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        "correct_answer"
                                    )
                                }
                            }
                            
                            // Record the answer to the repository
                            repository?.let { repo ->
                                coroutineScope.launch {
                                    repo.recordQuizAnswer(
                                        hskLevel = hskLevel,
                                        word = currentQuestion.word,
                                        isCorrect = isCorrect,
                                        questionType = currentQuestion.questionType.name,
                                        responseTime = System.currentTimeMillis() - answerTime
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun QuizProgressBar(
    currentQuestion: Int,
    totalQuestions: Int,
    score: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Text(
                text = "Question $currentQuestion/$totalQuestions",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge
            )
        }
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Score: $score",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun TimerBar(
    timeRemaining: Int,
    totalTime: Int,
    isPaused: Boolean
) {
    val progress = timeRemaining.toFloat() / totalTime
    val color = when {
        progress > 0.5f -> Color(0xFF4CAF50)
        progress > 0.25f -> Color(0xFFFFA726)
        else -> Color(0xFFEF5350)
    }
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isPaused) "PAUSED" else "Time: ${timeRemaining}s",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isPaused) MaterialTheme.colorScheme.error else color
            )
            
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun QuestionCard(
    question: QuizQuestion,
    questionNumber: Int,
    onSpeakCharacter: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = getQuestionTypeLabel(question.questionType),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = question.question,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontSize = if (question.questionType == QuestionType.CHARACTER_TO_PINYIN || 
                                 question.questionType == QuestionType.CHARACTER_TO_MEANING) 36.sp else 24.sp
                )
                
                // Add speaker button for character questions
                if (question.questionType == QuestionType.CHARACTER_TO_PINYIN ||
                    question.questionType == QuestionType.CHARACTER_TO_MEANING) {
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(
                        onClick = onSpeakCharacter,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Speak character",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnswerOptions(
    question: QuizQuestion,
    options: List<String>,
    selectedAnswer: String?,
    correctAnswer: String?,
    showResult: Boolean,
    onSpeakCharacter: () -> Unit = {},
    onAnswerSelected: (String) -> Unit
) {
    Column {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(options) { index, option ->
                AnswerOptionCard(
                    option = option,
                    optionLabel = ('A' + index).toString(),
                    isSelected = selectedAnswer == option,
                    isCorrect = correctAnswer != null && option == correctAnswer,
                    isWrong = correctAnswer != null && selectedAnswer == option && option != correctAnswer,
                    showResult = correctAnswer != null,
                    onClick = { onAnswerSelected(option) }
                )
            }
        }
        
        // Show meaning when answer is correct
        if (showResult && selectedAnswer == correctAnswer) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 2.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF4CAF50))
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "✓ Correct!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${question.word.simplified} (${question.word.forms.firstOrNull()?.transcriptions?.pinyin ?: ""})",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onSpeakCharacter,
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Speak character",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = question.word.forms.firstOrNull()?.meanings?.joinToString("; ") ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AnswerOptionCard(
    option: String,
    optionLabel: String,
    isSelected: Boolean,
    isCorrect: Boolean,
    isWrong: Boolean,
    showResult: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        showResult && isCorrect -> Color(0xFF4CAF50)
        showResult && isWrong -> Color(0xFFEF5350)
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    
    val borderColor = when {
        showResult && isCorrect -> Color(0xFF388E3C)
        showResult && isWrong -> Color(0xFFD32F2F)
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !showResult) { onClick() }
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (showResult && (isCorrect || isWrong)) Color.White
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (showResult && isCorrect) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50)
                    )
                } else if (showResult && isWrong) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color(0xFFEF5350)
                    )
                } else {
                    Text(
                        text = optionLabel,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = option,
                style = MaterialTheme.typography.bodyLarge,
                color = if (showResult && (isCorrect || isWrong)) {
                    Color.White
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
fun QuizCompleteScreen(
    score: Int,
    totalQuestions: Int,
    onRestartQuiz: () -> Unit,
    modifier: Modifier = Modifier
) {
    val percentage = (score * 100) / totalQuestions
    val message = when {
        percentage >= 90 -> "Excellent! 非常好!"
        percentage >= 70 -> "Great job! 很好!"
        percentage >= 50 -> "Good effort! 不错!"
        else -> "Keep practicing! 继续努力!"
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFFFFC107)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Quiz Complete!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$score / $totalQuestions",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onRestartQuiz,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try Again")
        }
    }
}

fun generateQuizQuestions(vocabulary: List<HskWord>, count: Int): List<QuizQuestion> {
    val questions = mutableListOf<QuizQuestion>()
    val availableWords = vocabulary.shuffled().take(minOf(count * 3, vocabulary.size))
    
    repeat(count) { index ->
        val word = availableWords[index % availableWords.size]
        val questionType = QuestionType.values().random()
        
        val question = when (questionType) {
            QuestionType.CHARACTER_TO_PINYIN -> {
                QuizQuestion(
                    word = word,
                    questionType = questionType,
                    question = word.simplified,
                    correctAnswer = word.forms.firstOrNull()?.transcriptions?.pinyin ?: "",
                    options = generatePinyinOptions(word, availableWords)
                )
            }
            QuestionType.CHARACTER_TO_MEANING -> {
                QuizQuestion(
                    word = word,
                    questionType = questionType,
                    question = word.simplified,
                    correctAnswer = word.forms.firstOrNull()?.meanings?.firstOrNull() ?: "",
                    options = generateMeaningOptions(word, availableWords)
                )
            }
            QuestionType.PINYIN_TO_CHARACTER -> {
                QuizQuestion(
                    word = word,
                    questionType = questionType,
                    question = word.forms.firstOrNull()?.transcriptions?.pinyin ?: "",
                    correctAnswer = word.simplified,
                    options = generateCharacterOptions(word, availableWords)
                )
            }
            QuestionType.PINYIN_TO_MEANING -> {
                QuizQuestion(
                    word = word,
                    questionType = questionType,
                    question = word.forms.firstOrNull()?.transcriptions?.pinyin ?: "",
                    correctAnswer = word.forms.firstOrNull()?.meanings?.firstOrNull() ?: "",
                    options = generateMeaningOptions(word, availableWords)
                )
            }
            QuestionType.MEANING_TO_CHARACTER -> {
                QuizQuestion(
                    word = word,
                    questionType = questionType,
                    question = word.forms.firstOrNull()?.meanings?.firstOrNull() ?: "",
                    correctAnswer = word.simplified,
                    options = generateCharacterOptions(word, availableWords)
                )
            }
            QuestionType.MEANING_TO_PINYIN -> {
                QuizQuestion(
                    word = word,
                    questionType = questionType,
                    question = word.forms.firstOrNull()?.meanings?.firstOrNull() ?: "",
                    correctAnswer = word.forms.firstOrNull()?.transcriptions?.pinyin ?: "",
                    options = generatePinyinOptions(word, availableWords)
                )
            }
        }
        
        questions.add(question)
    }
    
    return questions
}

fun generateCharacterOptions(correctWord: HskWord, vocabulary: List<HskWord>): List<String> {
    val options = mutableSetOf(correctWord.simplified)
    val otherWords = vocabulary.filter { it.simplified != correctWord.simplified }.shuffled()
    
    for (word in otherWords) {
        if (options.size >= 4) break
        options.add(word.simplified)
    }
    
    while (options.size < 4) {
        options.add("无${options.size}")
    }
    
    return options.toList().shuffled()
}

fun generatePinyinOptions(correctWord: HskWord, vocabulary: List<HskWord>): List<String> {
    val correctPinyin = correctWord.forms.firstOrNull()?.transcriptions?.pinyin ?: ""
    val options = mutableSetOf(correctPinyin)
    val otherWords = vocabulary.filter { 
        it.forms.firstOrNull()?.transcriptions?.pinyin != correctPinyin 
    }.shuffled()
    
    for (word in otherWords) {
        if (options.size >= 4) break
        word.forms.firstOrNull()?.transcriptions?.pinyin?.let { options.add(it) }
    }
    
    while (options.size < 4) {
        options.add("wú${options.size}")
    }
    
    return options.toList().shuffled()
}

fun generateMeaningOptions(correctWord: HskWord, vocabulary: List<HskWord>): List<String> {
    val correctMeaning = correctWord.forms.firstOrNull()?.meanings?.firstOrNull() ?: ""
    val options = mutableSetOf(correctMeaning)
    val otherWords = vocabulary.filter { it.simplified != correctWord.simplified }.shuffled()
    
    for (word in otherWords) {
        if (options.size >= 4) break
        word.forms.firstOrNull()?.meanings?.firstOrNull()?.let { 
            if (it != correctMeaning) options.add(it) 
        }
    }
    
    while (options.size < 4) {
        options.add("Unknown meaning ${options.size}")
    }
    
    return options.toList().shuffled()
}

fun getQuestionTypeLabel(type: QuestionType): String {
    return when (type) {
        QuestionType.CHARACTER_TO_PINYIN -> "Select the correct pinyin"
        QuestionType.CHARACTER_TO_MEANING -> "Select the correct meaning"
        QuestionType.PINYIN_TO_CHARACTER -> "Select the correct character"
        QuestionType.PINYIN_TO_MEANING -> "Select the correct meaning"
        QuestionType.MEANING_TO_CHARACTER -> "Select the correct character"
        QuestionType.MEANING_TO_PINYIN -> "Select the correct pinyin"
    }
}