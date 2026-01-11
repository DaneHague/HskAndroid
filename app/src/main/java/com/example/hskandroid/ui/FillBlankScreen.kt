package com.hskmaster.app.ui

import android.speech.tts.TextToSpeech
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hskmaster.app.data.repository.LearningRepository
import com.hskmaster.app.model.ClozeQuestion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FillBlankScreen(
    questions: List<ClozeQuestion>,
    hskLevel: Int = 1,
    repository: LearningRepository? = null,
    onBackPressed: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Game state
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var isGameComplete by remember { mutableStateOf(false) }

    // Selection state
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var showFeedback by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }

    // Timing
    var startTime by remember { mutableStateOf(0L) }

    // Text-to-Speech setup
    val context = LocalContext.current
    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsInitialized by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

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

    val totalQuestions = minOf(10, questions.size)
    val gameQuestions = remember(questions) {
        questions.shuffled().take(totalQuestions)
    }

    val currentQuestion = if (currentQuestionIndex < gameQuestions.size) {
        gameQuestions[currentQuestionIndex]
    } else null

    // Shuffled options for current question
    var shuffledOptions by remember { mutableStateOf<List<String>>(emptyList()) }

    // Initialize for new question
    LaunchedEffect(currentQuestionIndex) {
        currentQuestion?.let { question ->
            shuffledOptions = question.options.shuffled()
            selectedOption = null
            showFeedback = false
            isCorrect = false
            startTime = System.currentTimeMillis()
        }
    }

    // Auto-advance after correct answer
    LaunchedEffect(showFeedback, isCorrect) {
        if (showFeedback && isCorrect) {
            delay(2500)
            if (currentQuestionIndex < gameQuestions.size - 1) {
                currentQuestionIndex++
            } else {
                isGameComplete = true
            }
        }
    }

    fun checkAnswer(option: String) {
        currentQuestion?.let { question ->
            selectedOption = option
            isCorrect = option == question.correctAnswer
            showFeedback = true

            if (isCorrect) {
                score++
                // Speak the full sentence
                if (ttsInitialized) {
                    textToSpeech?.speak(
                        question.fullSentence,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "sentence_correct"
                    )
                }
            }

            // Record to repository
            repository?.let { repo ->
                coroutineScope.launch {
                    repo.recordFillBlank(
                        hskLevel = hskLevel,
                        sentence = question.fullSentence,
                        correctAnswer = question.correctAnswer,
                        isCorrect = isCorrect,
                        responseTime = System.currentTimeMillis() - startTime
                    )
                }
            }
        }
    }

    fun nextQuestion() {
        if (currentQuestionIndex < gameQuestions.size - 1) {
            currentQuestionIndex++
        } else {
            isGameComplete = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "HSK $hskLevel Fill in the Blank",
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
        if (isGameComplete) {
            FillBlankCompleteScreen(
                score = score,
                totalQuestions = totalQuestions,
                onRestartGame = {
                    currentQuestionIndex = 0
                    score = 0
                    isGameComplete = false
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
                // Progress and Score
                FillBlankProgressBar(
                    currentQuestion = currentQuestionIndex + 1,
                    totalQuestions = totalQuestions,
                    score = score
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Sentence card with blank
                SentenceCard(
                    question = currentQuestion,
                    selectedOption = selectedOption,
                    showFeedback = showFeedback,
                    isCorrect = isCorrect,
                    onSpeakSentence = {
                        if (ttsInitialized) {
                            textToSpeech?.speak(
                                currentQuestion.fullSentence,
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                "sentence_hint"
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Options grid
                Text(
                    text = "Choose the correct word:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OptionsGrid(
                    options = shuffledOptions,
                    correctAnswer = currentQuestion.correctAnswer,
                    selectedOption = selectedOption,
                    showFeedback = showFeedback,
                    onOptionSelected = { option ->
                        if (!showFeedback) {
                            checkAnswer(option)
                        }
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                // Feedback section
                if (showFeedback) {
                    FillBlankFeedbackCard(
                        isCorrect = isCorrect,
                        question = currentQuestion,
                        onNext = { nextQuestion() }
                    )
                }
            }
        }
    }
}

@Composable
fun FillBlankProgressBar(
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
fun SentenceCard(
    question: ClozeQuestion,
    selectedOption: String?,
    showFeedback: Boolean,
    isCorrect: Boolean,
    onSpeakSentence: () -> Unit
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sentence with blank
            val sentenceText = buildAnnotatedString {
                val parts = question.sentenceWithBlank.split("___")
                append(parts.getOrElse(0) { "" })

                // The blank part
                withStyle(
                    SpanStyle(
                        color = when {
                            showFeedback && isCorrect -> Color(0xFF4CAF50)
                            showFeedback && !isCorrect -> Color(0xFFEF5350)
                            selectedOption != null -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        },
                        fontWeight = FontWeight.Bold,
                        background = when {
                            showFeedback && isCorrect -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                            showFeedback && !isCorrect -> Color(0xFFEF5350).copy(alpha = 0.1f)
                            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        }
                    )
                ) {
                    val displayText = if (showFeedback) {
                        question.correctAnswer
                    } else {
                        selectedOption ?: " _____ "
                    }
                    append(" $displayText ")
                }

                if (parts.size > 1) {
                    append(parts[1])
                }
            }

            Text(
                text = sentenceText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 40.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Pinyin (shown after feedback)
            AnimatedVisibility(visible = showFeedback) {
                Text(
                    text = question.pinyin,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // English translation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = question.english,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onSpeakSentence,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Listen to sentence",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun OptionsGrid(
    options: List<String>,
    correctAnswer: String,
    selectedOption: String?,
    showFeedback: Boolean,
    onOptionSelected: (String) -> Unit
) {
    val labels = listOf("A", "B", "C", "D")

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Two rows of two options
        for (rowIndex in 0..1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (colIndex in 0..1) {
                    val index = rowIndex * 2 + colIndex
                    if (index < options.size) {
                        OptionButton(
                            label = labels[index],
                            option = options[index],
                            isCorrect = options[index] == correctAnswer,
                            isSelected = options[index] == selectedOption,
                            showFeedback = showFeedback,
                            onClick = { onOptionSelected(options[index]) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OptionButton(
    label: String,
    option: String,
    isCorrect: Boolean,
    isSelected: Boolean,
    showFeedback: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        showFeedback && isCorrect -> Color(0xFF4CAF50)
        showFeedback && isSelected && !isCorrect -> Color(0xFFEF5350)
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = when {
        showFeedback && isCorrect -> Color.White
        showFeedback && isSelected && !isCorrect -> Color.White
        isSelected -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val borderColor = when {
        showFeedback && isCorrect -> Color(0xFF4CAF50)
        showFeedback && isSelected && !isCorrect -> Color(0xFFEF5350)
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    Card(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !showFeedback, onClick = onClick)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Label circle (A, B, C, D)
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(16.dp),
                color = contentColor.copy(alpha = 0.2f)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Chinese option
            Text(
                text = option,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                fontSize = 22.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            // Feedback icon
            if (showFeedback) {
                if (isCorrect) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Correct",
                        tint = contentColor
                    )
                } else if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Incorrect",
                        tint = contentColor
                    )
                }
            }
        }
    }
}

@Composable
fun FillBlankFeedbackCard(
    isCorrect: Boolean,
    question: ClozeQuestion,
    onNext: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCorrect)
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            else
                Color(0xFFEF5350).copy(alpha = 0.1f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 2.dp,
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isCorrect) Color(0xFF4CAF50) else Color(0xFFEF5350)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isCorrect) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFEF5350),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCorrect) "Correct!" else "Incorrect",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFEF5350)
                )
            }

            if (!isCorrect) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "The correct answer is:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "${question.correctAnswer} (${question.correctPinyin})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isCorrect) {
                Text(
                    text = "Next question in 2 seconds...",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Button(onClick = onNext) {
                    Text("Next Question")
                }
            }
        }
    }
}

@Composable
fun FillBlankCompleteScreen(
    score: Int,
    totalQuestions: Int,
    onRestartGame: () -> Unit,
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
            text = "Game Complete!",
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
            onClick = onRestartGame,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Play Again")
        }
    }
}
