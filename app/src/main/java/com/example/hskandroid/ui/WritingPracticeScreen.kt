package com.hskmaster.app.ui

import android.speech.tts.TextToSpeech
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hskmaster.app.data.repository.LearningRepository
import com.hskmaster.app.model.SimpleHskWord
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

data class DrawingPath(
    val path: Path,
    val color: Color,
    val strokeWidth: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingPracticeScreen(
    vocabulary: List<SimpleHskWord>,
    hskLevel: Int = 1,
    onBackPressed: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var currentWordIndex by remember { mutableStateOf(0) }
    var paths by remember { mutableStateOf(listOf<DrawingPath>()) }
    var showHint by remember { mutableStateOf(false) }
    var hintButtonVisible by remember { mutableStateOf(false) }
    var completedWords by remember { mutableStateOf(0) }
    var showNextButton by remember { mutableStateOf(false) }
    var timeElapsed by remember { mutableStateOf(0) }
    var hintUsedForCurrentWord by remember { mutableStateOf(false) }
    var selfEvaluationDone by remember { mutableStateOf(false) }
    var showTrace by remember { mutableStateOf(false) }
    var autoProgressCountdown by remember { mutableStateOf(0) }
    
    val context = LocalContext.current
    val repository = remember { LearningRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    
    // Text-to-Speech setup
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
    
    val wordsToUse = remember(vocabulary) {
        vocabulary.shuffled().take(10)
    }
    
    val currentWord = if (currentWordIndex < wordsToUse.size) {
        wordsToUse[currentWordIndex]
    } else null
    
    // Speak the character when it first appears
    LaunchedEffect(currentWord, ttsInitialized) {
        if (ttsInitialized && currentWord != null) {
            // Small delay to ensure UI is ready
            delay(300)
            textToSpeech?.speak(
                currentWord.chinese,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "writing_character"
            )
        }
    }
    
    // Timer for hint button
    LaunchedEffect(currentWordIndex, showNextButton) {
        if (!showNextButton) {
            hintButtonVisible = false
            timeElapsed = 0
            while (timeElapsed < 10) {
                delay(1000)
                timeElapsed++
            }
            hintButtonVisible = true
        }
    }
    
    // Auto-progress timer after marking correct
    LaunchedEffect(autoProgressCountdown, selfEvaluationDone) {
        if (autoProgressCountdown > 0 && selfEvaluationDone) {
            var countdown = autoProgressCountdown
            while (countdown > 0) {
                delay(1000)
                countdown--
                autoProgressCountdown = countdown
            }
            // Auto move to next character
            if (currentWordIndex < wordsToUse.size - 1) {
                currentWordIndex++
                paths = emptyList()
                showHint = false
                showTrace = false
                showNextButton = false
                hintButtonVisible = false
                hintUsedForCurrentWord = false
                selfEvaluationDone = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "HSK $hskLevel Writing Practice",
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
                        text = "$completedWords/${wordsToUse.size}",
                        modifier = Modifier.padding(end = 16.dp),
                        style = MaterialTheme.typography.titleMedium
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
        if (currentWord != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Word info card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                            Text(
                                text = "Write this character:",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (ttsInitialized && currentWord != null) {
                                        textToSpeech?.speak(
                                            currentWord.chinese,
                                            TextToSpeech.QUEUE_FLUSH,
                                            null,
                                            "replay_character"
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Speak character",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentWord.pinyin,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentWord.english,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Drawing canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .background(Color.White)
                ) {
                    // Grid lines (draw first so they're in the background)
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        drawGridLines(size)
                    }
                    
                    // Drawing canvas (on top of grid lines)
                    var currentDrawingPath by remember { mutableStateOf<Path?>(null) }
                    
                    DrawingCanvas(
                        paths = paths,
                        currentPath = currentDrawingPath,
                        showHint = showHint,
                        showTrace = showTrace,
                        hintCharacter = currentWord.chinese,
                        onPathStart = { offset ->
                            val path = Path().apply {
                                moveTo(offset.x, offset.y)
                            }
                            currentDrawingPath = path
                        },
                        onPathUpdate = { offset ->
                            val updatedPath = currentDrawingPath?.let { existingPath ->
                                Path().apply {
                                    addPath(existingPath)
                                    lineTo(offset.x, offset.y)
                                }
                            }
                            currentDrawingPath = updatedPath
                        },
                        onPathEnd = {
                            currentDrawingPath?.let { path ->
                                paths = paths + DrawingPath(
                                    path = path,
                                    color = Color.Black,
                                    strokeWidth = 8f
                                )
                                currentDrawingPath = null
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Control buttons - First row (helper buttons)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Clear button
                    OutlinedButton(
                        onClick = {
                            paths = emptyList()
                            showHint = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear")
                    }
                    
                    // Trace toggle button
                    FilledTonalButton(
                        onClick = { showTrace = !showTrace },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (showTrace) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = if (showTrace) "Hide Trace" else "Show Trace",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showTrace) "Trace On" else "Trace Off"
                        )
                    }
                    
                    // Hint button (always visible but changes appearance)
                    FilledTonalButton(
                        onClick = { 
                            showHint = !showHint
                            if (showHint) {
                                hintUsedForCurrentWord = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (showHint) 
                                MaterialTheme.colorScheme.tertiaryContainer 
                            else if (hintButtonVisible)
                                MaterialTheme.colorScheme.secondaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                        enabled = hintButtonVisible || showHint
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Hint",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when {
                                showHint -> "Hint On"
                                hintButtonVisible -> "Hint"
                                else -> "Wait..."
                            }
                        )
                    }
                }
                
                // Submit/Check button - Second row (main action)
                AnimatedVisibility(
                    visible = !showNextButton,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Button(
                        onClick = {
                            // Always show feedback, even if no drawing
                            showNextButton = true
                            
                            // If they drew something, auto-mark as correct and start countdown
                            if (paths.isNotEmpty()) {
                                selfEvaluationDone = true
                                autoProgressCountdown = 3  // Start 3 second countdown
                                completedWords++  // Increment for correct answer since they practiced
                                
                                // Play TTS when correct
                                if (ttsInitialized && currentWord != null) {
                                    textToSpeech?.speak(
                                        currentWord.chinese,
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        "correct_character"
                                    )
                                }
                                
                                // Save as correct to history
                                currentWord?.let { word ->
                                    coroutineScope.launch {
                                        repository.recordWritingPractice(
                                            hskLevel = hskLevel,
                                            word = word,
                                            hintUsed = hintUsedForCurrentWord,
                                            isCorrect = true
                                        )
                                    }
                                }
                            } else {
                                selfEvaluationDone = false
                                // Don't auto-progress if no drawing
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Submit",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Check Answer", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                // Next button (appears after submit)
                AnimatedVisibility(
                    visible = showNextButton,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (paths.isNotEmpty()) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            ),
                            border = BorderStroke(
                                width = 2.dp,
                                color = if (paths.isNotEmpty()) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (paths.isNotEmpty()) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "✓ Great practice!",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFF4CAF50),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No drawing detected",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Answer: ",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = currentWord.chinese,
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 36.sp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    IconButton(
                                        onClick = {
                                            if (ttsInitialized && currentWord != null) {
                                                textToSpeech?.speak(
                                                    currentWord.chinese,
                                                    TextToSpeech.QUEUE_FLUSH,
                                                    null,
                                                    "feedback_character"
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                color = if (paths.isNotEmpty()) 
                                                    Color(0xFF4CAF50).copy(alpha = 0.2f) 
                                                else 
                                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                                shape = CircleShape
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Speak character",
                                            tint = if (paths.isNotEmpty()) 
                                                Color(0xFF4CAF50) 
                                            else 
                                                MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Self-evaluation buttons - only show for empty drawings
                        if (!selfEvaluationDone && paths.isEmpty()) {
                            Text(
                                text = "You haven't drawn anything yet. Try drawing the character first, or tap 'Need Practice' to skip.",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(bottom = 8.dp),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.error
                            )
                            
                            // Only show skip button for empty drawings
                            Button(
                                onClick = {
                                    selfEvaluationDone = true
                                    // Don't increment completedWords for skipped/empty
                                    
                                    // Save as incorrect to history (no drawing)
                                    currentWord?.let { word ->
                                        coroutineScope.launch {
                                            repository.recordWritingPractice(
                                                hskLevel = hskLevel,
                                                word = word,
                                                hintUsed = hintUsedForCurrentWord,
                                                isCorrect = false
                                            )
                                        }
                                    }
                                    
                                    // Move to next word immediately
                                    if (currentWordIndex < wordsToUse.size - 1) {
                                        currentWordIndex++
                                        paths = emptyList()
                                        showHint = false
                                        showTrace = false
                                        showNextButton = false
                                        hintButtonVisible = false
                                        hintUsedForCurrentWord = false
                                        selfEvaluationDone = false
                                        autoProgressCountdown = 0
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text("Skip This Character", fontSize = 16.sp)
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        // Show countdown when auto-progressing
                        if (selfEvaluationDone && autoProgressCountdown > 0) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFE8F5E9)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color(0xFF4CAF50),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Next character in ${autoProgressCountdown}...",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color(0xFF4CAF50),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        
                        // Next button (shows after self-evaluation)
                        AnimatedVisibility(
                            visible = selfEvaluationDone && autoProgressCountdown == 0,
                            enter = fadeIn() + expandVertically()
                        ) {
                            Button(
                                onClick = {
                                    if (currentWordIndex < wordsToUse.size - 1) {
                                        currentWordIndex++
                                        paths = emptyList()
                                        showHint = false
                                        showTrace = false  // Reset trace
                                        showNextButton = false
                                        hintButtonVisible = false
                                        hintUsedForCurrentWord = false
                                        selfEvaluationDone = false
                                        autoProgressCountdown = 0  // Reset countdown
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Next"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Next Character")
                            }
                        }
                        
                    }
                }
            }
        } else {
            // Practice complete screen
            PracticeCompleteScreen(
                completedWords = completedWords,
                totalWords = wordsToUse.size,
                onRestart = {
                    currentWordIndex = 0
                    completedWords = 0
                    paths = emptyList()
                    showHint = false
                    showNextButton = false
                },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
fun DrawingCanvas(
    paths: List<DrawingPath>,
    currentPath: Path?,
    showHint: Boolean,
    showTrace: Boolean,
    hintCharacter: String,
    onPathStart: (Offset) -> Unit,
    onPathUpdate: (Offset) -> Unit,
    onPathEnd: () -> Unit
) {
    val density = LocalDensity.current
    var isDrawing by remember { mutableStateOf(false) }
    
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDrawing = true
                        onPathStart(offset)
                    },
                    onDrag = { change, _ ->
                        if (isDrawing) {
                            onPathUpdate(change.position)
                        }
                    },
                    onDragEnd = {
                        if (isDrawing) {
                            onPathEnd()
                            isDrawing = false
                        }
                    }
                )
            }
    ) {
        // Draw trace character if enabled (very faint outline)
        if (showTrace) {
            drawTraceCharacter(hintCharacter, size, density)
        }
        
        // Draw hint character if enabled (darker overlay)
        if (showHint) {
            drawHintCharacter(hintCharacter, size, density)
        }
        
        // Draw all completed paths
        paths.forEach { drawingPath ->
            drawPath(
                path = drawingPath.path,
                color = drawingPath.color,
                style = Stroke(
                    width = drawingPath.strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
        
        // Draw current path
        currentPath?.let { path ->
            drawPath(
                path = path,
                color = Color.Black,
                style = Stroke(
                    width = 8f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

fun DrawScope.drawGridLines(size: Size) {
    val strokeWidth = 1.dp.toPx()
    val color = Color.LightGray.copy(alpha = 0.3f)
    
    // Draw center cross
    drawLine(
        color = color,
        start = Offset(size.width / 2, 0f),
        end = Offset(size.width / 2, size.height),
        strokeWidth = strokeWidth,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
    )
    drawLine(
        color = color,
        start = Offset(0f, size.height / 2),
        end = Offset(size.width, size.height / 2),
        strokeWidth = strokeWidth,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
    )
    
    // Draw diagonal lines
    drawLine(
        color = color.copy(alpha = 0.2f),
        start = Offset(0f, 0f),
        end = Offset(size.width, size.height),
        strokeWidth = strokeWidth,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
    )
    drawLine(
        color = color.copy(alpha = 0.2f),
        start = Offset(size.width, 0f),
        end = Offset(0f, size.height),
        strokeWidth = strokeWidth,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
    )
}

fun DrawScope.drawTraceCharacter(
    character: String,
    size: Size,
    density: androidx.compose.ui.unit.Density
) {
    with(density) {
        drawContext.canvas.nativeCanvas.apply {
            // Adjust text size based on character length
            val charCount = character.length
            val scaleFactor = when {
                charCount <= 1 -> 0.7f
                charCount == 2 -> 0.35f
                charCount == 3 -> 0.25f
                else -> 0.2f
            }
            
            val paint = android.graphics.Paint().apply {
                textSize = (size.width * scaleFactor)
                color = android.graphics.Color.GRAY  // Changed from LTGRAY for better visibility
                textAlign = android.graphics.Paint.Align.CENTER
                alpha = 70  // Increased from 25 for much better visibility
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 2.5f  // Slightly thinner stroke
            }
            
            val textBounds = android.graphics.Rect()
            paint.getTextBounds(character, 0, character.length, textBounds)
            
            val x = size.width / 2
            val y = size.height / 2 + textBounds.height() / 2
            
            drawText(character, x, y, paint)
        }
    }
}

fun DrawScope.drawHintCharacter(
    character: String,
    size: Size,
    density: androidx.compose.ui.unit.Density
) {
    with(density) {
        drawContext.canvas.nativeCanvas.apply {
            // Adjust text size based on character length
            val charCount = character.length
            val scaleFactor = when {
                charCount <= 1 -> 0.7f
                charCount == 2 -> 0.35f
                charCount == 3 -> 0.25f
                else -> 0.2f
            }
            
            val paint = android.graphics.Paint().apply {
                textSize = (size.width * scaleFactor)
                color = android.graphics.Color.LTGRAY
                textAlign = android.graphics.Paint.Align.CENTER
                alpha = 130  // Slightly more visible for hint
            }
            
            val textBounds = android.graphics.Rect()
            paint.getTextBounds(character, 0, character.length, textBounds)
            
            val x = size.width / 2
            val y = size.height / 2 + textBounds.height() / 2
            
            drawText(character, x, y, paint)
        }
    }
}

@Composable
fun PracticeCompleteScreen(
    completedWords: Int,
    totalWords: Int,
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
            imageVector = Icons.Default.Create,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Practice Complete!",
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
                    text = "You practiced",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "$completedWords characters",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Keep practicing to improve your writing!",
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