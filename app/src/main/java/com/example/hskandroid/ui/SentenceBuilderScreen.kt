package com.hskmaster.app.ui

import android.speech.tts.TextToSpeech
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hskmaster.app.data.repository.LearningRepository
import com.hskmaster.app.model.HskSentence
import com.hskmaster.app.model.SentenceWord
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SentenceBuilderScreen(
    sentences: List<HskSentence>,
    hskLevel: Int = 1,
    repository: LearningRepository? = null,
    onBackPressed: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Game state
    var currentSentenceIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var isGameComplete by remember { mutableStateOf(false) }

    // Word selection state
    var selectedWords by remember { mutableStateOf<List<SentenceWord>>(emptyList()) }
    var availableWords by remember { mutableStateOf<List<SentenceWord>>(emptyList()) }
    var shuffledWords by remember { mutableStateOf<List<SentenceWord>>(emptyList()) }

    // Feedback state
    var showFeedback by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }
    var wordResults by remember { mutableStateOf<List<Boolean>>(emptyList()) }
    var attempts by remember { mutableStateOf(0) }

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

    val totalSentences = minOf(10, sentences.size)
    val gameSentences = remember(sentences) {
        sentences.shuffled().take(totalSentences)
    }

    val currentSentence = if (currentSentenceIndex < gameSentences.size) {
        gameSentences[currentSentenceIndex]
    } else null

    // Initialize/reset for new sentence
    LaunchedEffect(currentSentenceIndex) {
        currentSentence?.let { sentence ->
            val shuffled = shuffleWords(sentence.words)
            shuffledWords = shuffled
            availableWords = shuffled
            selectedWords = emptyList()
            showFeedback = false
            isCorrect = false
            wordResults = emptyList()
            attempts = 0
            startTime = System.currentTimeMillis()
        }
    }

    // Auto-advance after correct answer
    LaunchedEffect(showFeedback, isCorrect) {
        if (showFeedback && isCorrect) {
            delay(2500)
            if (currentSentenceIndex < gameSentences.size - 1) {
                currentSentenceIndex++
            } else {
                isGameComplete = true
            }
        }
    }

    fun onWordSelected(word: SentenceWord) {
        if (!showFeedback) {
            selectedWords = selectedWords + word
            availableWords = availableWords - word
        }
    }

    fun onWordDeselected(word: SentenceWord) {
        if (!showFeedback) {
            selectedWords = selectedWords - word
            availableWords = availableWords + word
        }
    }

    fun clearSelection() {
        if (!showFeedback) {
            selectedWords = emptyList()
            availableWords = shuffledWords
        }
    }

    fun checkAnswer() {
        currentSentence?.let { sentence ->
            attempts++
            val correctOrder = sentence.words.sortedBy { it.position }

            if (selectedWords.size != correctOrder.size) {
                // Not all words selected
                return
            }

            val results = selectedWords.mapIndexed { index, word ->
                word.position == index
            }

            wordResults = results
            isCorrect = results.all { it }
            showFeedback = true

            if (isCorrect) {
                score++
                // Speak the sentence
                if (ttsInitialized) {
                    textToSpeech?.speak(
                        sentence.chinese,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "sentence_correct"
                    )
                }

                // Record to repository
                repository?.let { repo ->
                    coroutineScope.launch {
                        repo.recordSentenceBuilder(
                            hskLevel = hskLevel,
                            sentenceChinese = sentence.chinese,
                            isCorrect = true,
                            responseTime = System.currentTimeMillis() - startTime,
                            attempts = attempts
                        )
                    }
                }
            }
        }
    }

    fun tryAgain() {
        showFeedback = false
        wordResults = emptyList()
        // Keep selected words but allow rearranging
    }

    fun nextSentence() {
        if (currentSentenceIndex < gameSentences.size - 1) {
            currentSentenceIndex++
        } else {
            isGameComplete = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "HSK $hskLevel Sentence Builder",
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
            SentenceBuilderCompleteScreen(
                score = score,
                totalSentences = totalSentences,
                onRestartGame = {
                    currentSentenceIndex = 0
                    score = 0
                    isGameComplete = false
                },
                modifier = Modifier.padding(paddingValues)
            )
        } else if (currentSentence != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Progress and Score
                SentenceBuilderProgressBar(
                    currentSentence = currentSentenceIndex + 1,
                    totalSentences = totalSentences,
                    score = score
                )

                Spacer(modifier = Modifier.height(24.dp))

                // English prompt
                EnglishPromptCard(
                    english = currentSentence.english,
                    onSpeakSentence = {
                        if (ttsInitialized) {
                            textToSpeech?.speak(
                                currentSentence.chinese,
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                "sentence_hint"
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Answer area (selected words)
                AnswerArea(
                    selectedWords = selectedWords,
                    wordResults = wordResults,
                    showFeedback = showFeedback,
                    onWordDeselected = { onWordDeselected(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Word bank
                Text(
                    text = "Tap words to build the sentence:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                WordBankArea(
                    availableWords = availableWords,
                    onWordSelected = { onWordSelected(it) }
                )

                Spacer(modifier = Modifier.weight(1f))

                // Feedback section
                if (showFeedback) {
                    FeedbackCard(
                        isCorrect = isCorrect,
                        sentence = currentSentence,
                        onTryAgain = { tryAgain() },
                        onNext = { nextSentence() }
                    )
                } else {
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { clearSelection() },
                            modifier = Modifier.weight(1f),
                            enabled = selectedWords.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear")
                        }

                        Button(
                            onClick = { checkAnswer() },
                            modifier = Modifier.weight(1f),
                            enabled = selectedWords.size == currentSentence.words.size
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Check")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SentenceBuilderProgressBar(
    currentSentence: Int,
    totalSentences: Int,
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
                text = "Sentence $currentSentence/$totalSentences",
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
fun EnglishPromptCard(
    english: String,
    onSpeakSentence: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Translate to Chinese:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = english,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnswerArea(
    selectedWords: List<SentenceWord>,
    wordResults: List<Boolean>,
    showFeedback: Boolean,
    onWordDeselected: (SentenceWord) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        if (selectedWords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tap words below to build your answer",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selectedWords.forEachIndexed { index, word ->
                    val resultColor = when {
                        !showFeedback -> null
                        wordResults.getOrNull(index) == true -> Color(0xFF4CAF50)
                        else -> Color(0xFFEF5350)
                    }

                    WordChip(
                        word = word,
                        isSelected = true,
                        resultColor = resultColor,
                        onClick = { if (!showFeedback) onWordDeselected(word) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordBankArea(
    availableWords: List<SentenceWord>,
    onWordSelected: (SentenceWord) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        availableWords.forEach { word ->
            WordChip(
                word = word,
                isSelected = false,
                resultColor = null,
                onClick = { onWordSelected(word) }
            )
        }
    }
}

@Composable
fun WordChip(
    word: SentenceWord,
    isSelected: Boolean,
    resultColor: Color?,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        resultColor != null -> resultColor
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

    val contentColor = when {
        resultColor != null -> Color.White
        isSelected -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = if (isSelected) 4.dp else 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = word.chinese,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                fontSize = 20.sp
            )
            Text(
                text = word.pinyin,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun FeedbackCard(
    isCorrect: Boolean,
    sentence: HskSentence,
    onTryAgain: () -> Unit,
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
                    text = if (isCorrect) "Correct!" else "Not quite right",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFEF5350)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = sentence.chinese,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = sentence.pinyin,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isCorrect) {
                Text(
                    text = "Next sentence in 2 seconds...",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = onTryAgain) {
                        Text("Try Again")
                    }
                    Button(onClick = onNext) {
                        Text("Skip")
                    }
                }
            }
        }
    }
}

@Composable
fun SentenceBuilderCompleteScreen(
    score: Int,
    totalSentences: Int,
    onRestartGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val percentage = (score * 100) / totalSentences
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
                    text = "$score / $totalSentences",
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

private fun shuffleWords(words: List<SentenceWord>): List<SentenceWord> {
    var shuffled = words.shuffled()

    // Ensure shuffled order is different from original
    var attempts = 0
    val originalOrder = words.sortedBy { it.position }
    while (shuffled.map { it.position } == originalOrder.map { it.position } && attempts < 10) {
        shuffled = words.shuffled()
        attempts++
    }

    return shuffled
}
