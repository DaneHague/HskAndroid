package com.example.hskandroid.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DonutChart(
    completed: Int,
    total: Int,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    strokeWidth: Dp = 24.dp,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    animationDuration: Int = 1000,
    centerContent: @Composable BoxScope.() -> Unit = {
        val percentage = if (total > 0) (completed * 100) / total else 0
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$percentage%",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$completed / $total",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
) {
    val sweepAngle = remember(completed, total) {
        if (total > 0) (completed.toFloat() / total.toFloat()) * 360f else 0f
    }
    
    var animatedSweepAngle by remember { mutableStateOf(0f) }
    
    LaunchedEffect(sweepAngle) {
        animatedSweepAngle = 0f
        animate(
            initialValue = 0f,
            targetValue = sweepAngle,
            animationSpec = tween(
                durationMillis = animationDuration,
                easing = FastOutSlowInEasing
            )
        ) { value, _ ->
            animatedSweepAngle = value
        }
    }
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val strokePx = strokeWidth.toPx()
            val radius = (this.size.minDimension - strokePx) / 2
            val center = Offset(this.size.width / 2, this.size.height / 2)
            
            // Draw background circle
            drawCircle(
                color = backgroundColor,
                radius = radius,
                center = center,
                style = Stroke(width = strokePx)
            )
            
            // Draw progress arc
            if (animatedSweepAngle > 0) {
                drawArc(
                    color = primaryColor,
                    startAngle = -90f,
                    sweepAngle = animatedSweepAngle,
                    useCenter = false,
                    topLeft = Offset(
                        (this.size.width - radius * 2) / 2,
                        (this.size.height - radius * 2) / 2
                    ),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(
                        width = strokePx,
                        cap = StrokeCap.Round
                    )
                )
            }
        }
        
        // Center content
        centerContent()
    }
}

@Composable
fun MultiColorDonutChart(
    segments: List<DonutSegment>,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    strokeWidth: Dp = 24.dp,
    animationDuration: Int = 1000,
    centerContent: @Composable BoxScope.() -> Unit = {}
) {
    val total = segments.sumOf { it.value }
    var animationProgress by remember { mutableStateOf(0f) }
    
    LaunchedEffect(segments) {
        animationProgress = 0f
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = animationDuration,
                easing = FastOutSlowInEasing
            )
        ) { value, _ ->
            animationProgress = value
        }
    }
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val strokePx = strokeWidth.toPx()
            val radius = (this.size.minDimension - strokePx) / 2
            val center = Offset(this.size.width / 2, this.size.height / 2)
            
            var currentAngle = -90f
            
            segments.forEach { segment ->
                val sweepAngle = (segment.value.toFloat() / total.toFloat()) * 360f * animationProgress
                
                if (sweepAngle > 0) {
                    drawArc(
                        color = segment.color,
                        startAngle = currentAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(
                            (this.size.width - radius * 2) / 2,
                            (this.size.height - radius * 2) / 2
                        ),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(
                            width = strokePx,
                            cap = StrokeCap.Butt
                        )
                    )
                    currentAngle += sweepAngle
                }
            }
        }
        
        centerContent()
    }
}

data class DonutSegment(
    val label: String,
    val value: Int,
    val color: Color
)