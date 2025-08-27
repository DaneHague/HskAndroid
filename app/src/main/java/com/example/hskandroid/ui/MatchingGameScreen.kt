package com.example.hskandroid.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hskandroid.data.repository.LearningRepository
import com.example.hskandroid.model.HskWord
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class GameCard(
    val id: String,
    val content: String,
    val type: CardType,
    val wordId: String,
    val word: HskWord? = null
)

enum class CardType {
    CHARACTER,
    PINYIN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchingGameScreen(
    vocabulary: List<HskWord>,
    hskLevel: Int = 1,
    onBackPressed: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var score by remember { mutableStateOf(0) }
    var attempts by remember { mutableStateOf(0) }
    var selectedCard by remember { mutableStateOf<GameCard?>(null) }
    var matchedPairs by remember { mutableStateOf(setOf<String>()) }
    var wrongMatch by remember { mutableStateOf<Pair<String, String>?>(null) }
    var attemptStartTime by remember { mutableStateOf(0L) }
    var gameKey by remember { mutableStateOf(0) } // Key to force game reset
    var countdown by remember { mutableStateOf(5) }
    var totalGamesPlayed by remember { mutableStateOf(0) }
    
    val context = LocalContext.current
    val repository = remember { LearningRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    
    val wordsToUse = remember(vocabulary, gameKey) {
        vocabulary.shuffled().take(6)
    }
    
    val gameCards = remember(wordsToUse, gameKey) {
        val cards = mutableListOf<GameCard>()
        wordsToUse.forEach { word ->
            val wordId = word.simplified
            cards.add(GameCard(
                id = "${wordId}_char",
                content = word.simplified,
                type = CardType.CHARACTER,
                wordId = wordId,
                word = word
            ))
            cards.add(GameCard(
                id = "${wordId}_pinyin",
                content = word.forms.firstOrNull()?.transcriptions?.pinyin ?: "",
                type = CardType.PINYIN,
                wordId = wordId,
                word = word
            ))
        }
        cards.shuffled()
    }
    
    LaunchedEffect(wrongMatch) {
        if (wrongMatch != null) {
            delay(1000)
            wrongMatch = null
        }
    }
    
    // Auto-restart when game is completed
    LaunchedEffect(matchedPairs.size) {
        if (matchedPairs.size == wordsToUse.size && wordsToUse.isNotEmpty()) {
            countdown = 5
            totalGamesPlayed++
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
            // Reset the game
            gameKey++
            score = 0
            attempts = 0
            selectedCard = null
            matchedPairs = emptySet()
            wrongMatch = null
            countdown = 5
        }
    }
    
    Scaffold(
        topBar = {
            if (onBackPressed != null) {
                TopAppBar(
                    title = { 
                        Column {
                            Text(
                                text = "HSK $hskLevel Matching Game",
                                fontWeight = FontWeight.Bold
                            )
                            if (totalGamesPlayed > 0) {
                                Text(
                                    text = "Round ${totalGamesPlayed + 1}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackPressed) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Card(
                modifier = Modifier.padding(4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    text = "Score: $score",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Card(
                modifier = Modifier.padding(4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Text(
                    text = "Attempts: $attempts",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        
        if (matchedPairs.size == wordsToUse.size && wordsToUse.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Congratulations!",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "You matched all pairs!",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = "Score: $score | Attempts: $attempts",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Countdown timer
                    Text(
                        text = "New game starting in...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = countdown.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(gameCards) { card ->
                    GameCardItem(
                        card = card,
                        isSelected = selectedCard?.id == card.id,
                        isMatched = matchedPairs.contains(card.wordId),
                        isWrong = wrongMatch?.let { 
                            it.first == card.id || it.second == card.id 
                        } ?: false,
                        onClick = {
                            if (!matchedPairs.contains(card.wordId)) {
                                when {
                                    selectedCard == null -> {
                                        selectedCard = card
                                        attemptStartTime = System.currentTimeMillis()
                                    }
                                    selectedCard?.id == card.id -> {
                                        selectedCard = null
                                    }
                                    selectedCard?.type == card.type -> {
                                        selectedCard = card
                                        attemptStartTime = System.currentTimeMillis()
                                    }
                                    else -> {
                                        attempts++
                                        val responseTime = System.currentTimeMillis() - attemptStartTime
                                        
                                        if (selectedCard?.wordId == card.wordId) {
                                            matchedPairs = matchedPairs + card.wordId
                                            score++
                                            
                                            // Record correct match to history
                                            card.word?.let { word ->
                                                coroutineScope.launch {
                                                    repository.recordMatchingGame(
                                                        hskLevel = hskLevel,
                                                        word = word,
                                                        isCorrect = true,
                                                        responseTime = responseTime,
                                                        attempts = attempts
                                                    )
                                                }
                                            }
                                        } else {
                                            wrongMatch = Pair(selectedCard!!.id, card.id)
                                            
                                            // Record incorrect match to history
                                            card.word?.let { word ->
                                                coroutineScope.launch {
                                                    repository.recordMatchingGame(
                                                        hskLevel = hskLevel,
                                                        word = word,
                                                        isCorrect = false,
                                                        responseTime = responseTime,
                                                        attempts = attempts
                                                    )
                                                }
                                            }
                                        }
                                        selectedCard = null
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
    }
}

@Composable
fun GameCardItem(
    card: GameCard,
    isSelected: Boolean,
    isMatched: Boolean,
    isWrong: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isMatched -> Color(0xFF4CAF50)
        isWrong -> Color(0xFFF44336)
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surface
    }
    
    val borderColor = when {
        isMatched -> Color(0xFF388E3C)
        isWrong -> Color(0xFFD32F2F)
        isSelected -> MaterialTheme.colorScheme.primary
        card.type == CardType.CHARACTER -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
    }
    
    val textColor = when {
        isMatched || isWrong || isSelected -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(enabled = !isMatched) { onClick() }
            .border(2.dp, borderColor, MaterialTheme.shapes.medium),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = card.content,
                    fontSize = if (card.type == CardType.CHARACTER) 28.sp else 16.sp,
                    fontWeight = if (card.type == CardType.CHARACTER) FontWeight.Bold else FontWeight.Normal,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
                if (card.type == CardType.CHARACTER) {
                    Text(
                        text = "字",
                        fontSize = 12.sp,
                        color = textColor.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Text(
                        text = "拼音",
                        fontSize = 12.sp,
                        color = textColor.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}