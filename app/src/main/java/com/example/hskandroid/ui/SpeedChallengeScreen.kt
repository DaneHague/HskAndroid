package com.hskmaster.app.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hskmaster.app.data.repository.LearningRepository
import com.hskmaster.app.model.SimpleHskWord
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

enum class SpeedQuestionType {
    CHINESE_TO_ENGLISH,  // Show Chinese, pick English
    ENGLISH_TO_CHINESE   // Show English, pick Chinese
}

data class SpeedQuestion(
    val word: SimpleHskWord,
    val questionType: SpeedQuestionType,
    val options: List<String>,  // 4 options including correct answer
    val correctIndex: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedChallengeScreen(
    vocabulary: List<SimpleHskWord>,
    hskLevel: Int = 1,
    repository: LearningRepository? = null,
    onBackPressed: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Game state
    var timeRemaining by remember { mutableStateOf(60) }
    var isGameActive by remember { mutableStateOf(false) }
    var isGameComplete by remember { mutableStateOf(false) }
    var showCountdown by remember { mutableStateOf(false) }
    var countdownValue by remember { mutableStateOf(3) }

    // Question state
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }
    var showFeedback by remember { mutableStateOf(false) }

    // Score state
    var correctCount by remember { mutableStateOf(0) }
    var wrongCount by remember { mutableStateOf(0) }
    var totalAnswered by remember { mutableStateOf(0) }

    // Personal best
    val prefs = remember { context.getSharedPreferences("speed_challenge", Context.MODE_PRIVATE) }
    var personalBest by remember { mutableStateOf(prefs.getInt("best_hsk$hskLevel", 0)) }
    var isNewRecord by remember { mutableStateOf(false) }

    // Generate questions with mixed types
    val questions = remember(vocabulary) {
        generateSpeedQuestions(vocabulary)
    }

    // Text-to-Speech
    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsInitialized by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.let { tts ->
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

    val currentQuestion = if (currentQuestionIndex < questions.size) {
        questions[currentQuestionIndex]
    } else {
        questions[currentQuestionIndex % questions.size]
    }

    // Countdown before game starts
    LaunchedEffect(showCountdown) {
        if (showCountdown) {
            countdownValue = 3
            while (countdownValue > 0) {
                delay(1000)
                countdownValue--
            }
            showCountdown = false
            isGameActive = true
        }
    }

    // Main game timer
    LaunchedEffect(isGameActive) {
        if (isGameActive) {
            timeRemaining = 60
            while (timeRemaining > 0 && isGameActive) {
                delay(1000)
                timeRemaining--
            }
            if (isGameActive) {
                isGameActive = false
                isGameComplete = true

                // Check for new record
                if (correctCount > personalBest) {
                    isNewRecord = true
                    personalBest = correctCount
                    prefs.edit().putInt("best_hsk$hskLevel", correctCount).apply()
                }
            }
        }
    }

    fun startGame() {
        currentQuestionIndex = 0
        selectedAnswer = null
        showFeedback = false
        correctCount = 0
        wrongCount = 0
        totalAnswered = 0
        isGameComplete = false
        isNewRecord = false
        showCountdown = true
    }

    fun selectAnswer(answerIndex: Int) {
        if (selectedAnswer != null || !isGameActive) return

        selectedAnswer = answerIndex
        showFeedback = true
        totalAnswered++

        val isCorrect = answerIndex == currentQuestion.correctIndex
        if (isCorrect) {
            correctCount++
            // Play TTS for correct answer
            if (ttsInitialized) {
                textToSpeech?.speak(
                    currentQuestion.word.chinese,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "speed_word"
                )
            }
        } else {
            wrongCount++
        }

        // Record to repository
        repository?.let { repo ->
            coroutineScope.launch {
                repo.recordSpeedChallenge(
                    hskLevel = hskLevel,
                    word = currentQuestion.word,
                    isCorrect = isCorrect
                )
            }
        }

        // Auto-advance after brief delay
        coroutineScope.launch {
            delay(600)
            if (isGameActive) {
                currentQuestionIndex++
                selectedAnswer = null
                showFeedback = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "HSK $hskLevel Speed Challenge",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                showCountdown -> {
                    CountdownOverlay(countdownValue = countdownValue)
                }
                isGameComplete -> {
                    SpeedChallengeCompleteScreen(
                        correctCount = correctCount,
                        wrongCount = wrongCount,
                        totalAnswered = totalAnswered,
                        personalBest = personalBest,
                        isNewRecord = isNewRecord,
                        onPlayAgain = { startGame() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                !isGameActive && !showCountdown -> {
                    SpeedChallengeStartScreen(
                        personalBest = personalBest,
                        onStart = { startGame() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    // Active game
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Timer and score row
                        SpeedTimerAndScoreRow(
                            timeRemaining = timeRemaining,
                            correctCount = correctCount,
                            totalAnswered = totalAnswered
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Question card
                        SpeedQuestionCard(
                            question = currentQuestion,
                            modifier = Modifier.weight(0.35f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Multiple choice options
                        SpeedOptionsGrid(
                            question = currentQuestion,
                            selectedAnswer = selectedAnswer,
                            onSelectAnswer = { selectAnswer(it) },
                            modifier = Modifier.weight(0.65f)
                        )
                    }
                }
            }
        }
    }
}

private fun generateSpeedQuestions(vocabulary: List<SimpleHskWord>): List<SpeedQuestion> {
    val shuffledVocab = vocabulary.shuffled()
    val questions = mutableListOf<SpeedQuestion>()

    for (word in shuffledVocab) {
        // Randomly decide question type (50/50)
        val questionType = if (Math.random() > 0.5) {
            SpeedQuestionType.CHINESE_TO_ENGLISH
        } else {
            SpeedQuestionType.ENGLISH_TO_CHINESE
        }

        // Get 3 wrong options from other words
        val otherWords = shuffledVocab.filter { it.chinese != word.chinese }.shuffled().take(3)

        val options = when (questionType) {
            SpeedQuestionType.CHINESE_TO_ENGLISH -> {
                val wrongOptions = otherWords.map { it.english }
                val allOptions = (wrongOptions + word.english).shuffled()
                allOptions
            }
            SpeedQuestionType.ENGLISH_TO_CHINESE -> {
                val wrongOptions = otherWords.map { it.chinese }
                val allOptions = (wrongOptions + word.chinese).shuffled()
                allOptions
            }
        }

        val correctAnswer = when (questionType) {
            SpeedQuestionType.CHINESE_TO_ENGLISH -> word.english
            SpeedQuestionType.ENGLISH_TO_CHINESE -> word.chinese
        }

        questions.add(
            SpeedQuestion(
                word = word,
                questionType = questionType,
                options = options,
                correctIndex = options.indexOf(correctAnswer)
            )
        )
    }

    return questions
}

@Composable
fun CountdownOverlay(countdownValue: Int) {
    val scale by animateFloatAsState(
        targetValue = if (countdownValue > 0) 1f else 0f,
        animationSpec = tween(200),
        label = "countdown_scale"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (countdownValue > 0) countdownValue.toString() else "GO!",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 120.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.scale(scale)
        )
    }
}

@Composable
fun SpeedTimerAndScoreRow(
    timeRemaining: Int,
    correctCount: Int,
    totalAnswered: Int
) {
    val timerColor = when {
        timeRemaining > 30 -> Color(0xFF4CAF50)
        timeRemaining > 15 -> Color(0xFFFFA726)
        else -> Color(0xFFEF5350)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (timeRemaining <= 10) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Score
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$correctCount / $totalAnswered",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Timer
        Card(
            colors = CardDefaults.cardColors(
                containerColor = timerColor.copy(alpha = 0.2f)
            ),
            modifier = Modifier.scale(pulseScale)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = timerColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${timeRemaining}s",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = timerColor
                )
            }
        }
    }
}

@Composable
fun SpeedQuestionCard(
    question: SpeedQuestion,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Question type indicator
            Text(
                text = when (question.questionType) {
                    SpeedQuestionType.CHINESE_TO_ENGLISH -> "What does this mean?"
                    SpeedQuestionType.ENGLISH_TO_CHINESE -> "How do you say this in Chinese?"
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Main question text
            when (question.questionType) {
                SpeedQuestionType.CHINESE_TO_ENGLISH -> {
                    Text(
                        text = question.word.chinese,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 48.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = question.word.pinyin,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                SpeedQuestionType.ENGLISH_TO_CHINESE -> {
                    Text(
                        text = question.word.english,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun SpeedOptionsGrid(
    question: SpeedQuestion,
    selectedAnswer: Int?,
    onSelectAnswer: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 2x2 grid of options
        for (row in 0..1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (col in 0..1) {
                    val index = row * 2 + col
                    if (index < question.options.size) {
                        SpeedOptionButton(
                            option = question.options[index],
                            index = index,
                            isSelected = selectedAnswer == index,
                            isCorrect = index == question.correctIndex,
                            showResult = selectedAnswer != null,
                            questionType = question.questionType,
                            onClick = { onSelectAnswer(index) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpeedOptionButton(
    option: String,
    index: Int,
    isSelected: Boolean,
    isCorrect: Boolean,
    showResult: Boolean,
    questionType: SpeedQuestionType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val labels = listOf("A", "B", "C", "D")

    val backgroundColor = when {
        showResult && isCorrect -> Color(0xFF4CAF50)
        showResult && isSelected && !isCorrect -> Color(0xFFEF5350)
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = when {
        showResult && isCorrect -> Color(0xFF4CAF50)
        showResult && isSelected && !isCorrect -> Color(0xFFEF5350)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }

    val textColor = when {
        showResult && (isCorrect || isSelected) -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(enabled = !showResult) { onClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Letter label
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (showResult && (isCorrect || isSelected))
                            Color.White.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = labels[index],
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (showResult && (isCorrect || isSelected))
                        Color.White
                    else
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Option text
            Text(
                text = option,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = textColor,
                fontSize = when (questionType) {
                    SpeedQuestionType.ENGLISH_TO_CHINESE -> 24.sp
                    SpeedQuestionType.CHINESE_TO_ENGLISH -> 14.sp
                },
                modifier = Modifier.weight(1f)
            )

            // Result icon
            if (showResult) {
                if (isCorrect) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Correct",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Wrong",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SpeedChallengeStartScreen(
    personalBest: Int,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFFFF5722)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Speed Challenge",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Answer as many questions as you can in 60 seconds!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Game description
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Mix of question types:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Chinese → English\n• English → Chinese",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (personalBest > 0) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Personal Best",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "$personalBest correct",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF5722)
            )
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Start Challenge",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SpeedChallengeCompleteScreen(
    correctCount: Int,
    wrongCount: Int,
    totalAnswered: Int,
    personalBest: Int,
    isNewRecord: Boolean,
    onPlayAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accuracy = if (totalAnswered > 0) {
        (correctCount * 100) / totalAnswered
    } else 0

    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isNewRecord) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color(0xFFFFC107)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "NEW RECORD!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFC107)
            )
        } else {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color(0xFF4CAF50)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Time's Up!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Stats card
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
                    text = "$correctCount",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
                Text(
                    text = "correct answers",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$totalAnswered",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "answered",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$accuracy%",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "accuracy",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$personalBest",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFC107)
                        )
                        Text(
                            text = "best",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onPlayAgain,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF5722)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Play Again",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
