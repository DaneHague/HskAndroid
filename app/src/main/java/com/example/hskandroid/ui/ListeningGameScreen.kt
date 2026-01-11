package com.hskmaster.app.ui

import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hskmaster.app.data.repository.LearningRepository
import com.hskmaster.app.model.SimpleHskWord
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeningGameScreen(
    vocabulary: List<SimpleHskWord>,
    hskLevel: Int = 1,
    onBackPressed: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var showResult by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }
    var questionStartTime by remember { mutableStateOf(0L) }
    var totalAttempts by remember { mutableStateOf(0) }
    var canPlaySound by remember { mutableStateOf(true) }
    
    val context = LocalContext.current
    val repository = remember { LearningRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize Text-to-Speech
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsInitialized by remember { mutableStateOf(false) }
    
    // Initialize ToneGenerator for sound effects
    val toneGenerator = remember {
        ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
    }
    
    DisposableEffect(context) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.CHINESE
                ttsInitialized = true
            }
        }
        
        onDispose {
            tts?.shutdown()
            toneGenerator.release()
        }
    }
    
    val questionsToUse = remember(vocabulary) {
        vocabulary.shuffled().take(10)
    }
    
    val currentQuestion = if (currentQuestionIndex < questionsToUse.size) {
        questionsToUse[currentQuestionIndex]
    } else null
    
    val answerOptions = remember(currentQuestion) {
        currentQuestion?.let { correct ->
            val wrongAnswers = vocabulary
                .filter { it.chinese != correct.chinese }
                .shuffled()
                .take(3)
            (wrongAnswers + correct).shuffled()
        } ?: emptyList()
    }
    
    // Auto-play sound when question changes
    LaunchedEffect(currentQuestionIndex, ttsInitialized) {
        if (ttsInitialized && currentQuestion != null) {
            delay(500) // Small delay for better UX
            tts?.speak(currentQuestion.chinese, TextToSpeech.QUEUE_FLUSH, null, null)
            questionStartTime = System.currentTimeMillis()
            canPlaySound = false
            delay(2000) // Cooldown before allowing replay
            canPlaySound = true
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "HSK $hskLevel Listening",
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
                    Text(
                        text = "$score/${questionsToUse.size}",
                        modifier = Modifier.padding(end = 16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        if (currentQuestion != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Progress indicator
                LinearProgressIndicator(
                    progress = { (currentQuestionIndex + 1) / questionsToUse.size.toFloat() },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "Question ${currentQuestionIndex + 1} of ${questionsToUse.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                // Sound play button - smaller and more compact
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .weight(0.3f),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.size(120.dp),
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(
                            containerColor = if (canPlaySound && !showResult)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = {
                                    if (ttsInitialized && canPlaySound) {
                                        tts?.speak(currentQuestion.chinese, TextToSpeech.QUEUE_FLUSH, null, null)
                                        canPlaySound = false
                                        coroutineScope.launch {
                                            delay(2000)
                                            canPlaySound = true
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                                enabled = canPlaySound && !showResult
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = if (canPlaySound) Icons.Default.PlayArrow else Icons.Default.Phone,
                                        contentDescription = "Play Sound",
                                        modifier = Modifier.size(48.dp),
                                        tint = if (canPlaySound && !showResult) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                    Text(
                                        text = if (canPlaySound) "Replay" else "Playing",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (canPlaySound && !showResult)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }
                }
                
                Text(
                    text = "Select what you heard:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Answer options in 2x2 grid
                Box(
                    modifier = Modifier.weight(0.4f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // First row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (answerOptions.size > 0) {
                                CompactAnswerCard(
                                    word = answerOptions[0],
                                    isSelected = selectedAnswer == answerOptions[0].chinese,
                                    isCorrect = showResult && answerOptions[0].chinese == currentQuestion.chinese,
                                    isWrong = showResult && selectedAnswer == answerOptions[0].chinese && answerOptions[0].chinese != currentQuestion.chinese,
                                    enabled = !showResult,
                                    onClick = { handleAnswerClick(answerOptions[0], currentQuestion, toneGenerator, repository, coroutineScope, hskLevel) {
                                        selectedAnswer = answerOptions[0].chinese
                                        showResult = true
                                        totalAttempts++
                                        val responseTime = System.currentTimeMillis() - questionStartTime
                                        isCorrect = answerOptions[0].chinese == currentQuestion.chinese
                                        if (isCorrect) {
                                            score++
                                        }
                                        isCorrect
                                    }},
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (answerOptions.size > 1) {
                                CompactAnswerCard(
                                    word = answerOptions[1],
                                    isSelected = selectedAnswer == answerOptions[1].chinese,
                                    isCorrect = showResult && answerOptions[1].chinese == currentQuestion.chinese,
                                    isWrong = showResult && selectedAnswer == answerOptions[1].chinese && answerOptions[1].chinese != currentQuestion.chinese,
                                    enabled = !showResult,
                                    onClick = { handleAnswerClick(answerOptions[1], currentQuestion, toneGenerator, repository, coroutineScope, hskLevel) {
                                        selectedAnswer = answerOptions[1].chinese
                                        showResult = true
                                        totalAttempts++
                                        val responseTime = System.currentTimeMillis() - questionStartTime
                                        isCorrect = answerOptions[1].chinese == currentQuestion.chinese
                                        if (isCorrect) {
                                            score++
                                        }
                                        isCorrect
                                    }},
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        
                        // Second row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (answerOptions.size > 2) {
                                CompactAnswerCard(
                                    word = answerOptions[2],
                                    isSelected = selectedAnswer == answerOptions[2].chinese,
                                    isCorrect = showResult && answerOptions[2].chinese == currentQuestion.chinese,
                                    isWrong = showResult && selectedAnswer == answerOptions[2].chinese && answerOptions[2].chinese != currentQuestion.chinese,
                                    enabled = !showResult,
                                    onClick = { handleAnswerClick(answerOptions[2], currentQuestion, toneGenerator, repository, coroutineScope, hskLevel) {
                                        selectedAnswer = answerOptions[2].chinese
                                        showResult = true
                                        totalAttempts++
                                        val responseTime = System.currentTimeMillis() - questionStartTime
                                        isCorrect = answerOptions[2].chinese == currentQuestion.chinese
                                        if (isCorrect) {
                                            score++
                                        }
                                        isCorrect
                                    }},
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (answerOptions.size > 3) {
                                CompactAnswerCard(
                                    word = answerOptions[3],
                                    isSelected = selectedAnswer == answerOptions[3].chinese,
                                    isCorrect = showResult && answerOptions[3].chinese == currentQuestion.chinese,
                                    isWrong = showResult && selectedAnswer == answerOptions[3].chinese && answerOptions[3].chinese != currentQuestion.chinese,
                                    enabled = !showResult,
                                    onClick = { handleAnswerClick(answerOptions[3], currentQuestion, toneGenerator, repository, coroutineScope, hskLevel) {
                                        selectedAnswer = answerOptions[3].chinese
                                        showResult = true
                                        totalAttempts++
                                        val responseTime = System.currentTimeMillis() - questionStartTime
                                        isCorrect = answerOptions[3].chinese == currentQuestion.chinese
                                        if (isCorrect) {
                                            score++
                                        }
                                        isCorrect
                                    }},
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                
                // Result feedback - more compact
                AnimatedVisibility(
                    visible = showResult,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                    modifier = Modifier.weight(0.3f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Compact result card
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCorrect) 
                                    Color(0xFF4CAF50) 
                                else 
                                    Color(0xFFFF5722)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isCorrect) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isCorrect) "Correct!" else "Incorrect!",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                        
                        // Compact word details
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentQuestion.chinese,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = currentQuestion.pinyin,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = currentQuestion.english.take(20),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                        
                        if (currentQuestionIndex < questionsToUse.size - 1) {
                            Text(
                                text = "Next in 3 seconds...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontStyle = FontStyle.Italic
                            )
                        }
                        
                        // Auto-advance
                        LaunchedEffect(showResult) {
                            if (showResult) {
                                delay(3000)
                                if (currentQuestionIndex < questionsToUse.size - 1) {
                                    currentQuestionIndex++
                                    selectedAnswer = null
                                    showResult = false
                                    canPlaySound = true
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Game complete screen
            GameCompleteScreen(
                score = score,
                totalQuestions = questionsToUse.size,
                onRestart = {
                    currentQuestionIndex = 0
                    score = 0
                    selectedAnswer = null
                    showResult = false
                    totalAttempts = 0
                },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

private fun handleAnswerClick(
    option: SimpleHskWord,
    currentQuestion: SimpleHskWord,
    toneGenerator: ToneGenerator,
    repository: LearningRepository,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    hskLevel: Int,
    onAnswer: () -> Boolean
) {
    val isCorrect = onAnswer()
    val responseTime = System.currentTimeMillis()
    
    if (isCorrect) {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
    }
    
    coroutineScope.launch {
        repository.recordListeningAnswer(
            hskLevel = hskLevel,
            word = currentQuestion,
            isCorrect = isCorrect,
            responseTime = responseTime
        )
    }
}

@Composable
fun CompactAnswerCard(
    word: SimpleHskWord,
    isSelected: Boolean,
    isCorrect: Boolean,
    isWrong: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isCorrect -> Color(0xFF4CAF50)
        isWrong -> Color(0xFFFF5722)
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    
    val borderColor = when {
        isCorrect -> Color(0xFF388E3C)
        isWrong -> Color(0xFFD32F2F)
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    
    OutlinedCard(
        onClick = { if (enabled) onClick() },
        modifier = modifier.height(80.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = CardDefaults.outlinedCardBorder().copy(
            width = if (isSelected || isCorrect || isWrong) 2.dp else 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(borderColor)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = word.chinese,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCorrect || isWrong) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontSize = 28.sp
                )
                Text(
                    text = word.pinyin,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCorrect || isWrong) 
                        Color.White.copy(alpha = 0.9f) 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            
            if (isCorrect || isWrong) {
                Icon(
                    imageVector = if (isCorrect) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.TopEnd)
                )
            }
        }
    }
}

@Composable
fun GameCompleteScreen(
    score: Int,
    totalQuestions: Int,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Phone,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Listening Practice Complete!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (score >= totalQuestions * 0.7) 
                    Color(0xFF4CAF50) 
                else if (score >= totalQuestions * 0.5) 
                    Color(0xFFFF9800)
                else 
                    Color(0xFFFF5722)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Your Score",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "$score/$totalQuestions",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${(score * 100 / totalQuestions)}%",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = when {
                score >= totalQuestions * 0.9 -> "Excellent listening skills!"
                score >= totalQuestions * 0.7 -> "Good job! Keep practicing!"
                score >= totalQuestions * 0.5 -> "Not bad, but there's room for improvement."
                else -> "Keep practicing to improve your listening!"
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Practice Again")
        }
    }
}