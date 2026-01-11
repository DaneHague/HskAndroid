package com.hskmaster.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hskmaster.app.data.PurchaseManager

data class GameOption(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val requiresPremium: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSelectionScreen(
    hskLevel: Int,
    onGameSelected: (String) -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val purchaseManager = remember { PurchaseManager(context) }
    var isPremium by remember { mutableStateOf(purchaseManager.isPremium()) }
    var showPremiumDialog by remember { mutableStateOf(false) }

    val gameOptions = remember {
        listOf(
            GameOption(
                id = "dictionary",
                title = "Dictionary",
                description = "Browse and search words",
                icon = Icons.Default.Search,
                color = Color(0xFF2196F3)
            ),
            GameOption(
                id = "quiz",
                title = "Quiz",
                description = "Test your knowledge",
                icon = Icons.Default.CheckCircle,
                color = Color(0xFF4CAF50)
            ),
            GameOption(
                id = "matching",
                title = "Matching Game",
                description = "Match characters with pinyin",
                icon = Icons.Default.DateRange,
                color = Color(0xFFFF9800)
            ),
            GameOption(
                id = "writing",
                title = "Writing Practice",
                description = "Practice writing characters",
                icon = Icons.Default.Edit,
                color = Color(0xFF9C27B0)
            ),
            GameOption(
                id = "listening",
                title = "Listening Practice",
                description = "Test your listening skills",
                icon = Icons.Default.PlayArrow,
                color = Color(0xFF00BCD4),
                requiresPremium = true
            ),
            GameOption(
                id = "sentence_builder",
                title = "Sentence Builder",
                description = "Arrange words into sentences",
                icon = Icons.AutoMirrored.Filled.List,
                color = Color(0xFF673AB7),
                requiresPremium = true
            ),
            GameOption(
                id = "speed_challenge",
                title = "Speed Challenge",
                description = "How many words in 60 seconds?",
                icon = Icons.Default.Favorite,
                color = Color(0xFFFF5722)
            ),
            GameOption(
                id = "fill_blank",
                title = "Fill in the Blank",
                description = "Complete the sentence",
                icon = Icons.Default.Edit,
                color = Color(0xFF795548),
                requiresPremium = true
            ),
            GameOption(
                id = "test",
                title = "Take Test",
                description = "Complete HSK practice test",
                icon = Icons.Default.Star,
                color = Color(0xFFE91E63)
            )
        )
    }

    // Premium Dialog
    if (showPremiumDialog) {
        PremiumDialog(
            onDismiss = { showPremiumDialog = false },
            onPurchase = {
                // TODO: Integrate Google Play Billing here
                // For now, just toggle premium for testing
                isPremium = purchaseManager.togglePremium()
                showPremiumDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "HSK $hskLevel",
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Choose an Activity",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Select how you want to practice",
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
                items(gameOptions) { option ->
                    val isLocked = option.requiresPremium && !isPremium
                    GameOptionCard(
                        option = option,
                        isLocked = isLocked,
                        onClick = {
                            if (isLocked) {
                                showPremiumDialog = true
                            } else {
                                onGameSelected(option.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GameOptionCard(
    option: GameOption,
    isLocked: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked)
                Color.Gray.copy(alpha = 0.1f)
            else
                option.color.copy(alpha = 0.15f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = option.title,
                    modifier = Modifier.size(48.dp),
                    tint = if (isLocked) Color.Gray else option.color
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = option.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = if (isLocked) Color.Gray else Color.Unspecified
                )

                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isLocked) Color.Gray else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Lock overlay
            if (isLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    // Lock badge in top-right corner
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFFD700),
                                        Color(0xFFFFA500)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Premium",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Premium text
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFFD700).copy(alpha = 0.9f)
                    ) {
                        Text(
                            text = "PREMIUM",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumDialog(
    onDismiss: () -> Unit,
    onPurchase: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFFD700),
                                Color(0xFFFFA500)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        },
        title = {
            Text(
                text = "Upgrade to Premium",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Unlock all games and features!",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Features list
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PremiumFeatureItem("Listening Practice")
                    PremiumFeatureItem("Sentence Builder")
                    PremiumFeatureItem("Fill in the Blank")
                    PremiumFeatureItem("No advertisements")
                    PremiumFeatureItem("Future updates included")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "One-time purchase",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onPurchase,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD700)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Get Premium - £0.99",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Maybe Later")
            }
        }
    )
}

@Composable
private fun PremiumFeatureItem(feature: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = feature,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
