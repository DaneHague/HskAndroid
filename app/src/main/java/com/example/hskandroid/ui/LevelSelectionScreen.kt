package com.hskmaster.app.ui

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
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
import com.hskmaster.app.data.AssetChecker
import com.hskmaster.app.data.BillingManager
import com.hskmaster.app.data.ProgressStats
import com.hskmaster.app.data.PurchaseManager
import com.hskmaster.app.data.UserProgressManager

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
    onAboutPressed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val availableLevels = remember {
        AssetChecker.getAvailableLevels(context)
    }
    val progressManager = remember { UserProgressManager(context) }
    var progressStats by remember { mutableStateOf(progressManager.getProgressStats()) }

    // Billing
    val purchaseManager = remember { PurchaseManager(context) }
    val billingManager = remember { BillingManager(context, purchaseManager) }
    val isPremium by billingManager.isPremium.collectAsState()
    val purchaseInProgress by billingManager.purchaseInProgress.collectAsState()

    var showPremiumDialog by remember { mutableStateOf(false) }

    // Connect to billing
    LaunchedEffect(Unit) {
        progressStats = progressManager.getProgressStats()
        billingManager.connect()
    }

    // Disconnect when leaving
    DisposableEffect(Unit) {
        onDispose {
            billingManager.disconnect()
        }
    }

    val hskLevels = listOf(
        HskLevel(
            level = 1,
            name = "HSK 1",
            wordCount = "500 words",
            description = "Basic daily expressions",
            color = Color(0xFF4CAF50),
            isAvailable = availableLevels.contains(1)
        ),
        HskLevel(
            level = 2,
            name = "HSK 2",
            wordCount = "1272 words",
            description = "Simple communication",
            color = Color(0xFF66BB6A),
            isAvailable = availableLevels.contains(2)
        ),
        HskLevel(
            level = 3,
            name = "HSK 3",
            wordCount = "2245 words",
            description = "Daily life topics",
            color = Color(0xFF42A5F5),
            isAvailable = availableLevels.contains(3)
        ),
        HskLevel(
            level = 4,
            name = "HSK 4",
            wordCount = "3245 words",
            description = "Fluent communication",
            color = Color(0xFF5C6BC0),
            isAvailable = availableLevels.contains(4)
        ),
        HskLevel(
            level = 5,
            name = "HSK 5",
            wordCount = "4316 words",
            description = "Newspapers and magazines",
            color = Color(0xFF7E57C2),
            isAvailable = availableLevels.contains(5)
        ),
        HskLevel(
            level = 6,
            name = "HSK 6",
            wordCount = "5456 words",
            description = "Professional proficiency",
            color = Color(0xFFAB47BC),
            isAvailable = availableLevels.contains(6)
        ),
        HskLevel(
            level = 7,
            name = "HSK 7",
            wordCount = "11000+ words",
            description = "Advanced proficiency",
            color = Color(0xFFEF5350),
            isAvailable = availableLevels.contains(7)
        )
    )

    // Premium Dialog
    if (showPremiumDialog) {
        PremiumUpgradeDialog(
            price = billingManager.getFormattedPrice(),
            isLoading = purchaseInProgress,
            onDismiss = { showPremiumDialog = false },
            onPurchase = {
                if (activity != null && billingManager.isReady()) {
                    billingManager.launchPurchaseFlow(activity)
                }
            },
            onRestore = {
                billingManager.connect() // Re-query purchases
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "HSK Master",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onAboutPressed) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About"
                        )
                    }
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
            // Progress Card with Streak and XP
            ProgressCard(stats = progressStats)

            Spacer(modifier = Modifier.height(12.dp))

            // Premium Status Card
            PremiumStatusCard(
                isPremium = isPremium,
                onUpgradeClick = { showPremiumDialog = true }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Select HSK Level",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
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
fun PremiumStatusCard(
    isPremium: Boolean,
    onUpgradeClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isPremium) { onUpgradeClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPremium)
                Color(0xFF4CAF50).copy(alpha = 0.15f)
            else
                Color(0xFFFFD700).copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            brush = if (isPremium) {
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF4CAF50),
                                        Color(0xFF8BC34A)
                                    )
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFFD700),
                                        Color(0xFFFFA500)
                                    )
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPremium) Icons.Default.CheckCircle else Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Text
                Column {
                    Text(
                        text = if (isPremium) "Premium Active" else "Upgrade to Premium",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isPremium)
                            "All games unlocked!"
                        else
                            "Unlock 3 extra games",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Button/Badge
            if (isPremium) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF4CAF50)
                ) {
                    Text(
                        text = "PRO",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFD700)
                ) {
                    Text(
                        text = "£0.99",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumUpgradeDialog(
    price: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onPurchase: () -> Unit,
    onRestore: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
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
                    PremiumFeatureRow("Listening Practice")
                    PremiumFeatureRow("Sentence Builder")
                    PremiumFeatureRow("Fill in the Blank")
                    PremiumFeatureRow("No advertisements")
                    PremiumFeatureRow("Future updates included")
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onPurchase,
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD700)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Processing...",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Get Premium - $price",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Restore purchases button
                TextButton(
                    onClick = onRestore,
                    enabled = !isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restore Purchases")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Maybe Later")
            }
        }
    )
}

@Composable
private fun PremiumFeatureRow(feature: String) {
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

@Composable
fun ProgressCard(stats: ProgressStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top row: Streak and Level
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Streak
                StreakDisplay(
                    streak = stats.currentStreak,
                    isAtRisk = stats.isStreakAtRisk
                )

                // Level Badge
                LevelBadge(
                    level = stats.currentLevel,
                    title = stats.levelTitle
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // XP Progress Bar
            XpProgressBar(
                totalXp = stats.totalXp,
                levelProgress = stats.levelProgress,
                xpToNext = stats.xpToNextLevel,
                todayXp = stats.todayXp
            )
        }
    }
}

@Composable
fun StreakDisplay(streak: Int, isAtRisk: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Fire emoji or warning
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isAtRisk) Color(0xFFFFE0B2)
                    else if (streak > 0) Color(0xFFFFE0B2)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isAtRisk && streak > 0) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Streak at risk",
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Text(
                    text = if (streak > 0) "\uD83D\uDD25" else "\uD83D\uDD25",
                    fontSize = 24.sp
                )
            }
        }

        Column {
            Text(
                text = "$streak day${if (streak != 1) "s" else ""}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isAtRisk && streak > 0) "Practice to keep!" else "Streak",
                style = MaterialTheme.typography.bodySmall,
                color = if (isAtRisk) Color(0xFFFF9800) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LevelBadge(level: Int, title: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF6366F1),
                            Color(0xFF8B5CF6)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$level",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun XpProgressBar(
    totalXp: Int,
    levelProgress: Float,
    xpToNext: Int,
    todayXp: Int
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "$totalXp XP",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (todayXp > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF4CAF50).copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "+$todayXp today",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Progress bar
        LinearProgressIndicator(
            progress = { levelProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = Color(0xFF6366F1),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "$xpToNext XP to next level",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
            .height(140.dp),
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
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = level.name,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = level.wordCount,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = level.description,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            if (!level.isAvailable) {
                Spacer(modifier = Modifier.height(4.dp))
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
