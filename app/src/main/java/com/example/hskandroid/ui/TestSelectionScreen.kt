package com.hskmaster.app.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hskmaster.app.data.TestAttemptEntity
import com.hskmaster.app.data.repository.TestRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class TestInfo(
    val testId: String,
    val testPath: String,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestSelectionScreen(
    hskLevel: Int,
    onTestSelected: (String) -> Unit,
    onAttemptSelected: (Long) -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { TestRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    
    val availableTests = remember {
        try {
            val testFolder = "Hsk${hskLevel}Tests"
            val testDirs = context.assets.list(testFolder) ?: emptyArray()
            
            testDirs.mapNotNull { testDir ->
                val testPath = "$testFolder/$testDir/${testDir}.json"
                try {
                    context.assets.open(testPath).use {
                        TestInfo(
                            testId = testDir,
                            testPath = testPath,
                            description = getTestDescription(testDir)
                        )
                    }
                } catch (e: Exception) {
                    null
                }
            }.sortedBy { it.testId }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "HSK $hskLevel Tests",
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
        if (availableTests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Coming Soon",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Available Tests",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "Select a test to practice",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(availableTests) { test ->
                        TestCardWithHistory(
                            test = test,
                            repository = repository,
                            onTestClick = { onTestSelected(test.testPath) },
                            onAttemptClick = onAttemptSelected
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TestCard(
    test: TestInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Test ${test.testId}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = test.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TestCardWithHistory(
    test: TestInfo,
    repository: TestRepository,
    onTestClick: () -> Unit,
    onAttemptClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val attempts by repository.getTestAttempts(test.testId)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Main test info row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Test ${test.testId}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = test.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (attempts.isNotEmpty()) {
                        val bestScore = attempts.maxOf { it.totalScore * 100.0 / it.totalQuestions }
                        val passedCount = attempts.count { it.passed }
                        Text(
                            text = "Best: ${bestScore.toInt()}% | Attempts: ${attempts.size} | Passed: $passedCount",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Column {
                    Button(
                        onClick = onTestClick,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Start")
                    }
                    
                    if (attempts.isNotEmpty()) {
                        TextButton(
                            onClick = { expanded = !expanded },
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(if (expanded) "Hide" else "History")
                            Icon(
                                imageVector = if (expanded) 
                                    Icons.Default.KeyboardArrowUp 
                                else 
                                    Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            
            // Expandable history section
            AnimatedVisibility(visible = expanded && attempts.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Recent Attempts",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    attempts.take(5).forEach { attempt ->
                        AttemptRow(
                            attempt = attempt,
                            dateFormat = dateFormat,
                            onClick = { onAttemptClick(attempt.id) }
                        )
                        if (attempt != attempts.last()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttemptRow(
    attempt: TestAttemptEntity,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit
) {
    val percentage = (attempt.totalScore * 100.0 / attempt.totalQuestions).toInt()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = dateFormat.format(Date(attempt.attemptDate)),
                style = MaterialTheme.typography.bodyMedium
            )
            Row {
                Text(
                    text = "Score: $percentage% (${attempt.totalScore}/${attempt.totalQuestions})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                val minutes = attempt.completionTime / 60000
                Text(
                    text = "${minutes}m",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Box(
            modifier = Modifier
                .background(
                    color = if (attempt.passed) Color(0xFF4CAF50) else Color(0xFFF44336),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (attempt.passed) "PASS" else "FAIL",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

private fun getTestDescription(testId: String): String {
    return when {
        testId.startsWith("H109") -> "HSK Level 1 Practice Test (2009 Edition)"
        testId.startsWith("H110") -> "HSK Level 1 Practice Test (2010 Edition)"
        testId.startsWith("H111") -> "HSK Level 1 Practice Test (2011 Edition)"
        testId.startsWith("H112") -> "HSK Level 1 Practice Test (2012 Edition)"
        testId.startsWith("H113") -> "HSK Level 1 Practice Test (2013 Edition)"
        testId.startsWith("H114") -> "HSK Level 1 Practice Test (2014 Edition)"
        else -> "HSK Practice Test"
    }
}