package com.hskmaster.app.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hskmaster.app.data.AnswerRecord
import com.hskmaster.app.data.TestAttemptEntity
import com.hskmaster.app.data.repository.TestRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestResultDetailScreen(
    attemptId: Long,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { TestRepository(context) }
    var testAttempt by remember { mutableStateOf<TestAttemptEntity?>(null) }
    var showQuestionDetails by remember { mutableStateOf(false) }
    
    LaunchedEffect(attemptId) {
        testAttempt = repository.getTestAttemptById(attemptId)
    }
    
    testAttempt?.let { attempt ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Test ${attempt.testId} Results",
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Score Overview Card
                item {
                    ScoreOverviewCard(attempt)
                }
                
                // Performance Graph
                item {
                    PerformanceGraphCard(attempt)
                }
                
                // Section Breakdown
                item {
                    SectionBreakdownCard(attempt)
                }
                
                // Question Details Toggle
                item {
                    Card(
                        onClick = { showQuestionDetails = !showQuestionDetails },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Question Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (showQuestionDetails) 
                                    Icons.Default.KeyboardArrowUp 
                                else 
                                    Icons.Default.KeyboardArrowDown,
                                contentDescription = if (showQuestionDetails) "Collapse" else "Expand"
                            )
                        }
                    }
                }
                
                // Question Details
                if (showQuestionDetails) {
                    items(attempt.answers.sortedBy { it.questionNumber }) { answer ->
                        QuestionDetailCard(answer)
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreOverviewCard(attempt: TestAttemptEntity) {
    val percentage = (attempt.totalScore * 100.0 / attempt.totalQuestions).toInt()
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (attempt.passed) 
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            else 
                Color(0xFFF44336).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pass/Fail Badge
            Box(
                modifier = Modifier
                    .background(
                        color = if (attempt.passed) Color(0xFF4CAF50) else Color(0xFFF44336),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (attempt.passed) "PASSED" else "FAILED",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Score Circle
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { percentage / 100f },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 12.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    color = when {
                        percentage >= 80 -> Color(0xFF4CAF50)
                        percentage >= 60 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$percentage%",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${attempt.totalScore}/${attempt.totalQuestions}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Date and Time
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = dateFormat.format(Date(attempt.attemptDate)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Completion Time
            val minutes = attempt.completionTime / 60000
            val seconds = (attempt.completionTime % 60000) / 1000
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Completed in ${minutes}m ${seconds}s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PerformanceGraphCard(attempt: TestAttemptEntity) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Performance Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Bar Chart
            val listeningPercentage = if (attempt.answers.count { it.section == "listening" } > 0) {
                (attempt.listeningScore * 100.0 / attempt.answers.count { it.section == "listening" }).toInt()
            } else 0
            
            val readingPercentage = if (attempt.answers.count { it.section == "reading" } > 0) {
                (attempt.readingScore * 100.0 / attempt.answers.count { it.section == "reading" }).toInt()
            } else 0
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BarChartColumn(
                    label = "Listening",
                    value = listeningPercentage,
                    color = Color(0xFF2196F3)
                )
                BarChartColumn(
                    label = "Reading",
                    value = readingPercentage,
                    color = Color(0xFF9C27B0)
                )
                BarChartColumn(
                    label = "Overall",
                    value = (attempt.totalScore * 100 / attempt.totalQuestions),
                    color = Color(0xFF4CAF50)
                )
            }
            
            // Pass line indicator
            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color.Gray
            )
            Text(
                text = "Pass line: 60%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun BarChartColumn(
    label: String,
    value: Int,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Background bar
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .fillMaxHeight()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                    )
            )
            // Value bar
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .fillMaxHeight(value / 100f)
                    .background(
                        color = color,
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                    )
            )
            // Value text
            Text(
                text = "$value%",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SectionBreakdownCard(attempt: TestAttemptEntity) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Section Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Listening Section
            val listeningQuestions = attempt.answers.count { it.section == "listening" }
            if (listeningQuestions > 0) {
                SectionRow(
                    icon = Icons.Default.Phone,
                    section = "Listening",
                    correct = attempt.listeningScore,
                    total = listeningQuestions
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Reading Section
            val readingQuestions = attempt.answers.count { it.section == "reading" }
            if (readingQuestions > 0) {
                SectionRow(
                    icon = Icons.Default.Edit,
                    section = "Reading",
                    correct = attempt.readingScore,
                    total = readingQuestions
                )
            }
        }
    }
}

@Composable
fun SectionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    section: String,
    correct: Int,
    total: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = section,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = section,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$correct / $total",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(8.dp))
            val percentage = (correct * 100 / total)
            Text(
                text = "$percentage%",
                color = when {
                    percentage >= 80 -> Color(0xFF4CAF50)
                    percentage >= 60 -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                },
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun QuestionDetailCard(answer: AnswerRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (answer.isCorrect)
                Color(0xFF4CAF50).copy(alpha = 0.05f)
            else
                Color(0xFFF44336).copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Question number with correct/wrong indicator
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = if (answer.isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = answer.questionNumber.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = answer.section.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row {
                        Text(
                            text = "Your answer: ",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = answer.userAnswer.ifEmpty { "Not answered" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (answer.isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                    }
                    if (!answer.isCorrect) {
                        Row {
                            Text(
                                text = "Correct answer: ",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = answer.correctAnswer,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }
            
            Icon(
                imageVector = if (answer.isCorrect) Icons.Default.Check else Icons.Default.Close,
                contentDescription = if (answer.isCorrect) "Correct" else "Wrong",
                tint = if (answer.isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}