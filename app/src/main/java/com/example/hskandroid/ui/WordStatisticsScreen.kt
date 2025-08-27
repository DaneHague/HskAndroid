package com.example.hskandroid.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hskandroid.data.repository.LearningRepository
import com.example.hskandroid.model.HskWord
import com.example.hskandroid.ui.components.DonutChart
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordStatisticsScreen(
    word: HskWord,
    hskLevel: Int,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { LearningRepository(context) }
    
    var listeningStats by remember { mutableStateOf<GameTypeStats?>(null) }
    var writingStats by remember { mutableStateOf<GameTypeStats?>(null) }
    var matchingStats by remember { mutableStateOf<GameTypeStats?>(null) }
    var quizStats by remember { mutableStateOf<GameTypeStats?>(null) }
    var totalAttempts by remember { mutableStateOf(0) }
    var totalCorrect by remember { mutableStateOf(0) }
    var lastPracticed by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(word) {
        val allRecords = repository.getAllRecords().first()
        val wordRecords = allRecords.filter { it.character == word.simplified }
        
        totalAttempts = wordRecords.size
        totalCorrect = wordRecords.count { it.isCorrect }
        
        lastPracticed = if (wordRecords.isNotEmpty()) {
            val lastRecord = wordRecords.maxBy { it.timestamp }
            val date = java.util.Date(lastRecord.timestamp)
            val formatter = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            formatter.format(date)
        } else null
        
        // Calculate stats by game type
        listeningStats = calculateGameTypeStats(wordRecords.filter { it.gameType == "listening" })
        writingStats = calculateGameTypeStats(wordRecords.filter { it.gameType == "writing" })
        matchingStats = calculateGameTypeStats(wordRecords.filter { it.gameType == "matching" })
        quizStats = calculateGameTypeStats(wordRecords.filter { it.gameType == "quiz" })
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Word Statistics",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Word Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                        text = word.simplified,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Text(
                        text = word.forms.firstOrNull()?.transcriptions?.pinyin ?: "",
                        fontSize = 24.sp,
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Text(
                        text = word.forms.firstOrNull()?.meanings?.firstOrNull() ?: "",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "HSK Level",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = hskLevel.toString(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        if (lastPracticed != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Last Practiced",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = lastPracticed ?: "Never",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
            
            // Overall Progress
            if (totalAttempts > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DonutChart(
                            completed = totalCorrect,
                            total = totalAttempts,
                            size = 120.dp,
                            strokeWidth = 16.dp,
                            primaryColor = Color(0xFF4CAF50),
                            centerContent = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${if (totalAttempts > 0) (totalCorrect * 100 / totalAttempts) else 0}%",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Accuracy",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatRow(
                                label = "Total Attempts",
                                value = totalAttempts.toString(),
                                color = MaterialTheme.colorScheme.primary
                            )
                            StatRow(
                                label = "Correct",
                                value = totalCorrect.toString(),
                                color = Color(0xFF4CAF50)
                            )
                            StatRow(
                                label = "Incorrect",
                                value = (totalAttempts - totalCorrect).toString(),
                                color = Color(0xFFF44336)
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "No practice data available yet",
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Game Type Statistics
            Text(
                text = "Performance by Activity",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            GameTypeStatCard(
                title = "Listening",
                stats = listeningStats,
                icon = Icons.Default.PlayArrow,
                color = Color(0xFF2196F3)
            )
            
            GameTypeStatCard(
                title = "Writing",
                stats = writingStats,
                icon = Icons.Default.Edit,
                color = Color(0xFF9C27B0)
            )
            
            GameTypeStatCard(
                title = "Matching",
                stats = matchingStats,
                icon = Icons.Default.DateRange,
                color = Color(0xFFFF9800)
            )
            
            GameTypeStatCard(
                title = "Quiz",
                stats = quizStats,
                icon = Icons.Default.CheckCircle,
                color = Color(0xFF4CAF50)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun StatRow(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, MaterialTheme.shapes.small)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GameTypeStatCard(
    title: String,
    stats: GameTypeStats?,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (stats != null) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            if (stats != null && stats.attempts > 0) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${stats.attempts} attempts",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${stats.accuracy}%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            stats.accuracy >= 80 -> Color(0xFF4CAF50)
                            stats.accuracy >= 60 -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                    )
                    Text(
                        text = "${stats.correct}/${stats.attempts}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Not practiced yet",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

data class GameTypeStats(
    val attempts: Int,
    val correct: Int,
    val accuracy: Int
)

fun calculateGameTypeStats(records: List<com.example.hskandroid.data.database.LearningRecord>): GameTypeStats? {
    if (records.isEmpty()) return null
    
    val attempts = records.size
    val correct = records.count { it.isCorrect }
    val accuracy = if (attempts > 0) (correct * 100 / attempts) else 0
    
    return GameTypeStats(
        attempts = attempts,
        correct = correct,
        accuracy = accuracy
    )
}