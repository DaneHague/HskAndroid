package com.example.hskandroid.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
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
import com.example.hskandroid.data.AssetChecker

data class HskLevel(
    val level: Int,
    val name: String,
    val wordCount: String,
    val description: String,
    val color: Color,
    val isAvailable: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelSelectionScreen(
    onLevelSelected: (Int) -> Unit,
    onLogPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val availableLevels = remember {
        AssetChecker.getAvailableLevels(context)
    }
    
    val hskLevels = listOf(
        HskLevel(
            level = 1,
            name = "HSK 1",
            wordCount = "150 words",
            description = "Basic daily expressions",
            color = Color(0xFF4CAF50),
            isAvailable = availableLevels.contains(1)
        ),
        HskLevel(
            level = 2,
            name = "HSK 2",
            wordCount = "300 words",
            description = "Simple communication",
            color = Color(0xFF66BB6A),
            isAvailable = availableLevels.contains(2)
        ),
        HskLevel(
            level = 3,
            name = "HSK 3",
            wordCount = "600 words",
            description = "Daily life topics",
            color = Color(0xFF42A5F5),
            isAvailable = availableLevels.contains(3)
        ),
        HskLevel(
            level = 4,
            name = "HSK 4",
            wordCount = "1200 words",
            description = "Fluent communication",
            color = Color(0xFF5C6BC0),
            isAvailable = availableLevels.contains(4)
        ),
        HskLevel(
            level = 5,
            name = "HSK 5",
            wordCount = "2500 words",
            description = "Newspapers and magazines",
            color = Color(0xFF7E57C2),
            isAvailable = availableLevels.contains(5)
        ),
        HskLevel(
            level = 6,
            name = "HSK 6",
            wordCount = "5000+ words",
            description = "Professional proficiency",
            color = Color(0xFFAB47BC),
            isAvailable = availableLevels.contains(6)
        ),
        HskLevel(
            level = 7,
            name = "HSK 7-9",
            wordCount = "11000+ words",
            description = "Advanced proficiency",
            color = Color(0xFFEF5350),
            isAvailable = availableLevels.contains(7)
        )
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "HSK Learning App",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    TextButton(
                        onClick = onLogPressed,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Learning Log",
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "History",
                                style = MaterialTheme.typography.labelLarge
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
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Select HSK Level",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Choose your Chinese proficiency level",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(hskLevels) { level ->
                LevelCard(
                    level = level,
                    onClick = {
                        if (level.isAvailable) {
                            onLevelSelected(level.level)
                        }
                    }
                )
            }
        }
    }
    }
}

@Composable
fun LevelCard(
    level: HskLevel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        onClick = onClick,
        enabled = level.isAvailable,
        colors = CardDefaults.cardColors(
            containerColor = if (level.isAvailable) level.color else Color.Gray.copy(alpha = 0.3f),
            disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (level.isAvailable) 4.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = level.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = level.wordCount,
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = level.description,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            if (!level.isAvailable) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Coming Soon",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}