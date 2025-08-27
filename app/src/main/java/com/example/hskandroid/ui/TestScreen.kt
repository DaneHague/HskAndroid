package com.example.hskandroid.ui

import android.media.MediaPlayer
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import kotlinx.coroutines.delay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hskandroid.data.repository.TestRepository
import com.example.hskandroid.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreen(
    hskLevel: Int = 1,
    onBackPressed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { TestRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var test by remember { mutableStateOf<HskTest?>(null) }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var currentSection by remember { mutableStateOf("listening") }
    var userAnswers by remember { mutableStateOf(mutableMapOf<Int, String>()) }
    var showResults by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<TestResult?>(null) }
    var startTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    // Audio player for listening section
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var audioAvailable by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(0) }
    var isSeekBarDragging by remember { mutableStateOf(false) }
    
    // Load test on launch
    LaunchedEffect(Unit) {
        test = repository.loadTest("TestH10901.json")
        
        // Initialize media player for listening section
        try {
            val afd = context.assets.openFd("Hsk1Tests/H10901Audio.mp3")
            mediaPlayer = MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                prepareAsync()
                setOnPreparedListener { mp ->
                    audioAvailable = true
                    duration = mp.duration
                }
                setOnCompletionListener {
                    isPlaying = false
                    currentPosition = 0
                }
                setOnErrorListener { _, _, _ ->
                    audioAvailable = false
                    true
                }
            }
            afd.close()
        } catch (e: Exception) {
            audioAvailable = false
            e.printStackTrace()
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }
    
    val allQuestions = remember(test) {
        if (test == null) {
            emptyList()
        } else {
            val questions = mutableListOf<TestQuestion>()
            
            // Add listening questions
            test!!.sections.listening.parts.forEach { (_, part) ->
                questions.addAll(part.questions)
            }
            
            // Add reading questions
            test!!.sections.reading.parts.forEach { (_, part) ->
                questions.addAll(part.questions)
            }
            
            questions.sortedBy { it.questionNumber }
        }
    }
    
    val currentQuestion = if (currentQuestionIndex < allQuestions.size) {
        allQuestions[currentQuestionIndex]
    } else null
    
    // Determine which section and part the current question belongs to
    val (currentPart, currentPartData) = remember(currentQuestion, test) {
        if (currentQuestion == null || test == null) {
            null to null
        } else {
            val questionNumber = currentQuestion.questionNumber
            var previousSection = currentSection
            
            // Check listening sections
            test!!.sections.listening.parts.entries.forEach { (partKey, part) ->
                if (part.questions.any { it.questionNumber == questionNumber }) {
                    currentSection = "listening"
                    return@remember partKey to part
                }
            }
            
            // Check reading sections
            test!!.sections.reading.parts.entries.forEach { (partKey, part) ->
                if (part.questions.any { it.questionNumber == questionNumber }) {
                    currentSection = "reading"
                    // Stop audio when entering reading section
                    if (previousSection == "listening") {
                        mediaPlayer?.pause()
                        isPlaying = false
                    }
                    return@remember partKey to part
                }
            }
            
            null to null
        }
    }
    
    // Update audio position periodically
    LaunchedEffect(isPlaying, mediaPlayer) {
        if (isPlaying && mediaPlayer != null && !isSeekBarDragging) {
            while (isPlaying) {
                currentPosition = mediaPlayer?.currentPosition ?: 0
                kotlinx.coroutines.delay(100)
            }
        }
    }
    
    fun moveToNextQuestion() {
        if (currentQuestionIndex < allQuestions.size - 1) {
            currentQuestionIndex++
            // Don't pause audio when navigating - let it continue playing
        } else {
            // Test complete, calculate results
            val completionTime = System.currentTimeMillis() - startTime
            testResult = test?.let {
                repository.calculateTestResult(it, userAnswers, completionTime)
            }
            showResults = true
            // Stop audio when test is complete
            mediaPlayer?.stop()
            isPlaying = false
        }
    }
    
    fun moveToPreviousQuestion() {
        if (currentQuestionIndex > 0) {
            currentQuestionIndex--
            // Don't pause audio when navigating - let it continue playing
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = test?.let { "Test ${it.testId}" } ?: "HSK Test",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (!showResults) {
                        Text(
                            text = "Q${currentQuestionIndex + 1}/${allQuestions.size}",
                            modifier = Modifier.padding(end = 16.dp),
                            style = MaterialTheme.typography.titleMedium
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
        if (showResults && testResult != null) {
            TestResultScreen(
                testResult = testResult!!,
                onRetakeTest = {
                    // Reset test
                    currentQuestionIndex = 0
                    userAnswers.clear()
                    showResults = false
                    testResult = null
                    startTime = System.currentTimeMillis()
                },
                modifier = Modifier.padding(paddingValues)
            )
        } else if (test != null && currentQuestion != null && currentPartData != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // Section and Part header
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (currentSection == "listening")
                            MaterialTheme.colorScheme.secondaryContainer
                        else
                            MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (currentSection == "listening") 
                                    Icons.Default.PlayArrow 
                                else 
                                    Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentSection == "listening") 
                                    test!!.sections.listening.title 
                                else 
                                    test!!.sections.reading.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = " - ${currentPartData.title}",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = currentPartData.instructions,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Audio control for listening questions
                if (currentSection == "listening" && audioAvailable) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Play/Pause and Restart buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Restart button
                                OutlinedButton(
                                    onClick = {
                                        mediaPlayer?.seekTo(0)
                                        currentPosition = 0
                                        if (!isPlaying) {
                                            mediaPlayer?.start()
                                            isPlaying = true
                                        }
                                    },
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Restart",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Restart")
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                // Play/Pause button
                                Button(
                                    onClick = {
                                        if (isPlaying) {
                                            mediaPlayer?.pause()
                                            isPlaying = false
                                        } else {
                                            mediaPlayer?.start()
                                            isPlaying = true
                                        }
                                    },
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) 
                                            Icons.Default.PauseCircle 
                                        else 
                                            Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isPlaying) "Pause" else "Play",
                                        fontSize = 16.sp
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Progress bar
                            Column {
                                Slider(
                                    value = currentPosition.toFloat(),
                                    onValueChange = { value ->
                                        isSeekBarDragging = true
                                        currentPosition = value.toInt()
                                    },
                                    onValueChangeFinished = {
                                        mediaPlayer?.seekTo(currentPosition)
                                        isSeekBarDragging = false
                                    },
                                    valueRange = 0f..duration.toFloat(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                // Time display
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = formatTime(currentPosition),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = formatTime(duration),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Question display
                QuestionCard(
                    question = currentQuestion,
                    userAnswer = userAnswers[currentQuestion.questionNumber],
                    partOptions = currentPartData.options,
                    onAnswerSelected = { answer ->
                        userAnswers = userAnswers.toMutableMap().apply {
                            this[currentQuestion.questionNumber] = answer
                        }
                    }
                )
                
                // Navigation buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { moveToPreviousQuestion() },
                        enabled = currentQuestionIndex > 0,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Previous")
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Button(
                        onClick = { moveToNextQuestion() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (currentQuestionIndex == allQuestions.size - 1) 
                                "Finish" 
                            else 
                                "Next"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (currentQuestionIndex == allQuestions.size - 1)
                                Icons.Default.Check
                            else
                                Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next"
                        )
                    }
                }
                
                // Progress indicator
                LinearProgressIndicator(
                    progress = { (currentQuestionIndex + 1).toFloat() / allQuestions.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        } else {
            // Loading state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun QuestionCard(
    question: TestQuestion,
    userAnswer: String?,
    partOptions: Map<String, String>?,
    onAnswerSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Question number and prompt
            Text(
                text = "Question ${question.questionNumber}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Display question content based on type
            when (question.type) {
                "true-false-image" -> {
                    question.prompt?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    question.audioScript?.let {
                        Text(
                            text = "Audio: $it",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // True/False buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AnswerButton(
                            text = "√ True",
                            isSelected = userAnswer == "√",
                            onClick = { onAnswerSelected("√") }
                        )
                        AnswerButton(
                            text = "X False",
                            isSelected = userAnswer == "X",
                            onClick = { onAnswerSelected("X") }
                        )
                    }
                }
                
                "multiple-choice-image", "multiple-choice-passage" -> {
                    question.prompt?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    question.audioScript?.let {
                        Text(
                            text = "Audio: $it",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Multiple choice options
                    val options = when (question.options) {
                        is List<*> -> (question.options as List<String>).associateWith { it }
                        is Map<*, *> -> question.options as Map<String, String>
                        else -> emptyMap()
                    }
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        options.forEach { (key, value) ->
                            AnswerOptionCard(
                                optionKey = key,
                                optionText = value,
                                isSelected = userAnswer == key,
                                onClick = { onAnswerSelected(key) }
                            )
                        }
                    }
                }
                
                "matching-dialogue-image", "matching-sentence-image", "matching-question-answer" -> {
                    question.prompt?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    question.audioScript?.let {
                        Text(
                            text = "Audio: $it",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Matching options (could be from question or from part)
                    val options = when {
                        question.options is List<*> -> (question.options as List<String>).associateWith { it }
                        partOptions != null -> partOptions
                        else -> emptyMap()
                    }
                    
                    // Display options in a grid
                    val optionsList = options.entries.toList()
                    for (i in optionsList.indices step 3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (j in 0..2) {
                                if (i + j < optionsList.size) {
                                    val (key, value) = optionsList[i + j]
                                    AnswerButton(
                                        text = "$key: $value",
                                        isSelected = userAnswer == key,
                                        onClick = { onAnswerSelected(key) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (j < 2 && i + j < optionsList.size - 1) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        if (i + 3 < optionsList.size) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                
                "fill-in-the-blank" -> {
                    question.prompt?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Options for fill in the blank (from part options)
                    partOptions?.let { options ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            options.forEach { (key, value) ->
                                AnswerOptionCard(
                                    optionKey = key,
                                    optionText = value,
                                    isSelected = userAnswer == key,
                                    onClick = { onAnswerSelected(key) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnswerButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = Color(0xFF4CAF50).copy(alpha = 0.8f),
                        shape = RoundedCornerShape(50)
                    )
                } else {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(50)
                    )
                }
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) 
                Color(0xFF4CAF50) // Bright green when selected
            else 
                MaterialTheme.colorScheme.surface,
            contentColor = if (isSelected)
                Color.White
            else
                MaterialTheme.colorScheme.onSurface
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isSelected) 8.dp else 1.dp,
            pressedElevation = if (isSelected) 12.dp else 4.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.White.copy(alpha = 0.9f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontSize = if (isSelected) 17.sp else 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun AnswerOptionCard(
    optionKey: String,
    optionText: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isSelected) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                            )
                        )
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = optionKey,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = optionText,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurface,
                fontSize = if (isSelected) 16.sp else 15.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun TestResultScreen(
    testResult: TestResult,
    onRetakeTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Test Complete!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Score cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ScoreCard(
                title = "Listening",
                score = testResult.listeningScore,
                total = testResult.answers.count { it.questionNumber <= 20 },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            ScoreCard(
                title = "Reading",
                score = testResult.readingScore,
                total = testResult.answers.count { it.questionNumber > 20 },
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Total score
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Total Score",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${testResult.totalScore}/${testResult.totalQuestions}",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                val percentage = (testResult.totalScore * 100f / testResult.totalQuestions).toInt()
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Time taken
        val minutes = testResult.completionTime / 60000
        val seconds = (testResult.completionTime % 60000) / 1000
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Time: ${minutes}m ${seconds}s",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Action buttons
        Button(
            onClick = onRetakeTest,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retake Test")
        }
    }
}

@Composable
fun ScoreCard(
    title: String,
    score: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$score/$total",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

private fun formatTime(milliseconds: Int): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}