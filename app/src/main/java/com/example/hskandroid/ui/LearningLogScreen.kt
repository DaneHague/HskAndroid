package com.example.hskandroid.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hskandroid.data.VocabularyLoader
import com.example.hskandroid.data.database.*
import com.example.hskandroid.data.repository.LearningRepository
import com.example.hskandroid.ui.components.DonutChart
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningLogScreen(
    onBackPressed: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { LearningRepository(context) }
    val scope = rememberCoroutineScope()
    
    var selectedTab by remember { mutableStateOf(0) }
    var records by remember { mutableStateOf<List<LearningRecord>>(emptyList()) }
    var dailyStats by remember { mutableStateOf<DailyStats?>(null) }
    var characterProgress by remember { mutableStateOf<List<CharacterProgress>>(emptyList()) }
    var filterGameType by remember { mutableStateOf<String?>(null) }
    var filterLevel by remember { mutableStateOf<Int?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(selectedTab, filterGameType, filterLevel) {
        when (selectedTab) {
            0 -> { // History
                repository.getAllRecords().collect { allRecords ->
                    records = allRecords.filter { record ->
                        (filterGameType == null || record.gameType == filterGameType) &&
                        (filterLevel == null || record.hskLevel == filterLevel)
                    }
                }
            }
            1 -> { // Stats
                dailyStats = repository.getDailyStats()
            }
            2 -> { // Progress
                repository.getCharacterProgress().collect {
                    characterProgress = it
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Learning History",
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
                    var expanded by remember { mutableStateOf(false) }
                    
                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }
                        
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Clear Old Data (30+ days)") },
                                onClick = {
                                    expanded = false
                                    showDeleteDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear All History") },
                                onClick = {
                                    expanded = false
                                    showClearAllDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Warning, contentDescription = null)
                                }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("History") },
                    icon = { Icon(Icons.Default.DateRange, null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Stats") },
                    icon = { Icon(Icons.Default.Star, null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Progress") },
                    icon = { Icon(Icons.Default.Face, null) }
                )
            }
            
            when (selectedTab) {
                0 -> HistoryTab(
                    records = records,
                    filterGameType = filterGameType,
                    filterLevel = filterLevel,
                    onFilterGameTypeChange = { filterGameType = it },
                    onFilterLevelChange = { filterLevel = it }
                )
                1 -> StatsTab(dailyStats)
                2 -> LevelProgressTab(repository = repository)
            }
        }
    }
    
    // Delete Old Data Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Clear Old Data") },
            text = { Text("This will delete all learning records older than 30 days. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.clearOldData(30)
                            showDeleteDialog = false
                            // Refresh data
                            when (selectedTab) {
                                0 -> repository.getAllRecords().collect { allRecords ->
                                    records = allRecords.filter { record ->
                                        (filterGameType == null || record.gameType == filterGameType) &&
                                        (filterLevel == null || record.hskLevel == filterLevel)
                                    }
                                }
                                1 -> dailyStats = repository.getDailyStats()
                                2 -> repository.getCharacterProgress().collect {
                                    characterProgress = it
                                }
                            }
                        }
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Clear All Data Dialog
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Clear All History") },
            text = { 
                Column {
                    Text("⚠️ Warning: This will delete ALL learning history!", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("This includes:")
                    Text("• All practice records")
                    Text("• All progress data")
                    Text("• All statistics")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("This action cannot be undone!", color = Color.Red)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.clearAllData()
                            showClearAllDialog = false
                            // Clear all displayed data
                            records = emptyList()
                            dailyStats = null
                            characterProgress = emptyList()
                        }
                    }
                ) {
                    Text("Clear All", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun HistoryTab(
    records: List<LearningRecord>,
    filterGameType: String?,
    filterLevel: Int?,
    onFilterGameTypeChange: (String?) -> Unit,
    onFilterLevelChange: (Int?) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filterGameType == null,
                onClick = { onFilterGameTypeChange(null) },
                label = { Text("All Games") }
            )
            FilterChip(
                selected = filterGameType == "matching",
                onClick = { onFilterGameTypeChange("matching") },
                label = { Text("Matching") }
            )
            FilterChip(
                selected = filterGameType == "quiz",
                onClick = { onFilterGameTypeChange("quiz") },
                label = { Text("Quiz") }
            )
            FilterChip(
                selected = filterGameType == "writing",
                onClick = { onFilterGameTypeChange("writing") },
                label = { Text("Writing") }
            )
            FilterChip(
                selected = filterGameType == "listening",
                onClick = { onFilterGameTypeChange("listening") },
                label = { Text("Listening") }
            )
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filterLevel == null,
                onClick = { onFilterLevelChange(null) },
                label = { Text("All Levels") }
            )
            (1..7).forEach { level ->
                FilterChip(
                    selected = filterLevel == level,
                    onClick = { onFilterLevelChange(level) },
                    label = { Text("HSK $level") }
                )
            }
        }
        
        // Records list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(records) { record ->
                RecordCard(record)
            }
            
            if (records.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No learning records yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecordCard(record: LearningRecord) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val gameColor = when (record.gameType) {
        "matching" -> Color(0xFF4CAF50)
        "quiz" -> Color(0xFFFF9800)
        "writing" -> Color(0xFF9C27B0)
        "listening" -> Color(0xFF00BCD4)
        else -> MaterialTheme.colorScheme.primary
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (record.isCorrect) {
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            } else {
                Color(0xFFFF5252).copy(alpha = 0.1f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Result icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (record.isCorrect) Color(0xFF4CAF50) else Color(0xFFFF5252)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (record.isCorrect) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = record.character,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = record.pinyin,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = record.meaning,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { },
                        label = { Text(record.gameType.replaceFirstChar { it.uppercase() }, fontSize = 10.sp) },
                        modifier = Modifier.height(24.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = gameColor.copy(alpha = 0.2f)
                        )
                    )
                    
                    AssistChip(
                        onClick = { },
                        label = { Text("HSK ${record.hskLevel}", fontSize = 10.sp) },
                        modifier = Modifier.height(24.dp)
                    )
                    
                    if (record.hintUsed) {
                        AssistChip(
                            onClick = { },
                            label = { Text("Hint", fontSize = 10.sp) },
                            modifier = Modifier.height(24.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFFFFC107).copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }
            
            // Time
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = dateFormat.format(Date(record.timestamp)),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                record.responseTime?.let { time ->
                    Text(
                        text = "${time / 1000}s",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StatsTab(dailyStats: DailyStats?) {
    if (dailyStats == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Overall stats card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Today's Performance",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            label = "Total",
                            value = dailyStats.totalAttempts.toString(),
                            color = MaterialTheme.colorScheme.primary
                        )
                        StatItem(
                            label = "Correct",
                            value = dailyStats.correctCount.toString(),
                            color = Color(0xFF4CAF50)
                        )
                        StatItem(
                            label = "Wrong",
                            value = dailyStats.wrongCount.toString(),
                            color = Color(0xFFFF5252)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Accuracy bar
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Accuracy", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${dailyStats.accuracyRate.toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { dailyStats.accuracyRate / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = when {
                                dailyStats.accuracyRate >= 80 -> Color(0xFF4CAF50)
                                dailyStats.accuracyRate >= 60 -> Color(0xFFFFC107)
                                else -> Color(0xFFFF5252)
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Characters learned
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${dailyStats.charactersLearned} unique characters practiced",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        
        // Game breakdown
        item {
            Text(
                text = "Game Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        items(dailyStats.gameBreakdown.entries.toList()) { (gameType, stats) ->
            GameStatsCard(gameType, stats)
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun GameStatsCard(gameType: String, stats: GameStats) {
    val gameColor = when (gameType) {
        "matching" -> Color(0xFF4CAF50)
        "quiz" -> Color(0xFFFF9800)
        "writing" -> Color(0xFF9C27B0)
        "listening" -> Color(0xFF00BCD4)
        else -> MaterialTheme.colorScheme.primary
    }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(gameColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (gameType) {
                        "matching" -> Icons.Default.CheckCircle
                        "quiz" -> Icons.Default.Edit
                        "writing" -> Icons.Default.Create
                        "listening" -> Icons.Default.Phone
                        else -> Icons.Default.PlayArrow
                    },
                    contentDescription = null,
                    tint = gameColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = gameType.replaceFirstChar { it.uppercase() },
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${stats.attempts} attempts",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${stats.accuracy.toInt()}%",
                    fontWeight = FontWeight.Bold,
                    color = gameColor
                )
                Text(
                    text = "${stats.correct}/${stats.attempts}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ProgressTab(characterProgress: List<CharacterProgress>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(characterProgress) { progress ->
            CharacterProgressCard(progress)
        }
        
        if (characterProgress.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No character progress yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CharacterProgressCard(progress: CharacterProgress) {
    val masteryColor = when {
        progress.mastery >= 80 -> Color(0xFF4CAF50)
        progress.mastery >= 60 -> Color(0xFFFFC107)
        else -> Color(0xFFFF5252)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = progress.character,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(48.dp),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = progress.pinyin,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${progress.totalAttempts} attempts",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${progress.correctCount} correct",
                        fontSize = 12.sp,
                        color = Color(0xFF4CAF50)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = { progress.mastery / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = masteryColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${progress.mastery.toInt()}%",
                    fontWeight = FontWeight.Bold,
                    color = masteryColor,
                    fontSize = 18.sp
                )
                Text(
                    text = "Mastery",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun LevelProgressTab(
    repository: LearningRepository,
    modifier: Modifier = Modifier
) {
    var selectedLevel by remember { mutableStateOf(1) }
    var uniqueWords by remember { mutableStateOf(0) }
    var totalWords by remember { mutableStateOf(150) } // Default HSK 1
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // HSK level total words
    val hskTotalWords = mapOf(
        1 to 150,
        2 to 300,
        3 to 600,
        4 to 1200,
        5 to 2500,
        6 to 5000,
        7 to 11000
    )
    
    LaunchedEffect(selectedLevel) {
        isLoading = true
        totalWords = hskTotalWords[selectedLevel] ?: 150
        
        // Load actual vocabulary count for the level
        val vocabulary = VocabularyLoader().loadHskVocabulary(context, selectedLevel)
        totalWords = vocabulary.size
        
        // Get unique practiced words
        uniqueWords = repository.getUniqueWordsByLevel(selectedLevel)
        isLoading = false
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Level selector
        Text(
            text = "Select HSK Level",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(7) { index ->
                val level = index + 1
                FilterChip(
                    selected = selectedLevel == level,
                    onClick = { selectedLevel = level },
                    label = { Text("HSK $level") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = when(level) {
                            1 -> Color(0xFF4CAF50)
                            2 -> Color(0xFF66BB6A)
                            3 -> Color(0xFF42A5F5)
                            4 -> Color(0xFF5C6BC0)
                            5 -> Color(0xFF7E57C2)
                            6 -> Color(0xFFAB47BC)
                            else -> Color(0xFFEF5350)
                        }
                    )
                )
            }
        }
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Progress Card with Donut Chart
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "HSK $selectedLevel Progress",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    DonutChart(
                        completed = uniqueWords,
                        total = totalWords,
                        size = 220.dp,
                        strokeWidth = 28.dp,
                        primaryColor = when(selectedLevel) {
                            1 -> Color(0xFF4CAF50)
                            2 -> Color(0xFF66BB6A)
                            3 -> Color(0xFF42A5F5)
                            4 -> Color(0xFF5C6BC0)
                            5 -> Color(0xFF7E57C2)
                            6 -> Color(0xFFAB47BC)
                            else -> Color(0xFFEF5350)
                        }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val percentage = if (totalWords > 0) 
                                (uniqueWords * 100) / totalWords else 0
                            Text(
                                text = "$percentage%",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$uniqueWords / $totalWords",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "words",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProgressStatItem(
                            label = "Practiced",
                            value = uniqueWords.toString(),
                            color = MaterialTheme.colorScheme.primary
                        )
                        ProgressStatItem(
                            label = "Remaining",
                            value = (totalWords - uniqueWords).toString(),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        ProgressStatItem(
                            label = "Total",
                            value = totalWords.toString(),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
            
            // Motivational message
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = when {
                            uniqueWords == 0 -> "Start practicing to track your progress!"
                            uniqueWords < totalWords * 0.25 -> "Great start! Keep going!"
                            uniqueWords < totalWords * 0.5 -> "You're making good progress!"
                            uniqueWords < totalWords * 0.75 -> "Amazing! You're over halfway there!"
                            uniqueWords < totalWords -> "Incredible! You're almost done with HSK $selectedLevel!"
                            else -> "Congratulations! You've completed HSK $selectedLevel!"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun ProgressStatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}