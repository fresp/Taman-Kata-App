package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Stage
import com.example.ui.TamanKataViewModel
import com.example.ui.components.KikiExpression
import com.example.ui.components.KikiMascot
import com.example.ui.theme.ActionOrange
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.TextDark
import com.example.ui.theme.WarmOrange
import com.example.ui.theme.WarmOrangeShadow
import com.example.ui.theme.SoftYellow

@Composable
fun RoadmapScreen(
    viewModel: TamanKataViewModel,
    onStageSelected: (Int) -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToCollection: () -> Unit,
    onNavigateToLibrary: () -> Unit
) {
    val stages by viewModel.stages.collectAsState()
    val sessionHistory by viewModel.sessionHistory.collectAsState()
    val hasGraduated by viewModel.hasGraduated.collectAsState()
    
    val currentLevel = stages.count { it.isUnlocked }
    
    val recentSessions = sessionHistory.takeLast(5)
    val avgScore = if (recentSessions.isNotEmpty()) recentSessions.map { it.averageScore }.average() else 0.0
    val starCount = when {
        avgScore >= 90 -> 3
        avgScore >= 75 -> 2
        avgScore >= 60 -> 1
        else -> 0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp)),
            color = PrimaryGreen,
            shape = RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(SoftYellow, CircleShape)
                            .padding(4.dp)
                            .background(Color.White, CircleShape)
                    ) {
                        KikiMascot(expression = KikiExpression.NEUTRAL, modifier = Modifier.fillMaxSize().padding(8.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Taman Kata",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (i in 1..3) {
                                Icon(
                                    Icons.Default.Star, 
                                    contentDescription = null, 
                                    tint = if (i <= starCount) WarmOrange else Color.White.copy(alpha = 0.3f), 
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "Level $currentLevel",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 14.sp
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Dot Background
            Canvas(modifier = Modifier.fillMaxSize()) {
                val dotRadius = 1.dp.toPx()
                val spacing = 24.dp.toPx()
                val color = PrimaryGreen.copy(alpha = 0.1f)
                
                var y = 0f
                while (y < size.height) {
                    var x = 0f
                    while (x < size.width) {
                        drawCircle(color = color, radius = dotRadius, center = Offset(x, y))
                        x += spacing
                    }
                    y += spacing
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(stages) { stage ->
                    val isActive = stage.isUnlocked && (stage.id == stages.lastOrNull { it.isUnlocked }?.id)
                    StageButton(
                        stage = stage,
                        isActive = isActive,
                        onClick = {
                            if (stage.isUnlocked) {
                                onStageSelected(stage.id)
                            }
                        }
                    )
                }
            }
        }

        // Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(onLongPress = { onNavigateToDashboard() })
            }) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(2.dp, Color(0xFFF3F4F6)),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier
                            .size(24.dp)
                            .drawBehind {
                                drawRect(
                                    color = PrimaryGreen,
                                    style = Stroke(width = 4.dp.toPx())
                                )
                                drawRect(
                                    color = PrimaryGreen,
                                    topLeft = Offset(size.width - 4.dp.toPx(), 0f),
                                    size = Size(4.dp.toPx(), 4.dp.toPx())
                                )
                            }
                        )
                    }
                }
                Text("3S HOLD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White,
                    border = BorderStroke(2.dp, PrimaryGreen),
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .clickable { onNavigateToCollection() }
                ) {
                    Text(
                        text = "KOLEKSI",
                        color = TextDark,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        letterSpacing = 1.sp
                    )
                }
                
                if (hasGraduated) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = ActionOrange,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .clickable { onNavigateToLibrary() }
                    ) {
                        Text(
                            text = "PERPUSTAKAAN",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Surface(
                shape = CircleShape,
                color = ActionOrange,
                border = BorderStroke(4.dp, Color.White),
                shadowElevation = 8.dp,
                modifier = Modifier.size(64.dp).clickable {
                    val activeStage = stages.lastOrNull { it.isUnlocked }
                    if (activeStage != null) {
                        onStageSelected(activeStage.id)
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Main",
                    tint = Color.White,
                    modifier = Modifier.padding(12.dp).fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun StageButton(stage: Stage, isActive: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        val buttonModifier = Modifier
            .size(80.dp)
            .clickable(enabled = stage.isUnlocked, onClick = onClick)

        when {
            isActive -> {
                Box(contentAlignment = Alignment.Center) {
                    // Outer ring
                    Surface(
                        modifier = Modifier.size(92.dp),
                        shape = RoundedCornerShape(32.dp),
                        color = Color.Transparent,
                        border = BorderStroke(4.dp, PrimaryGreen)
                    ) {}
                    
                    Surface(
                        modifier = buttonModifier.shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = WarmOrangeShadow, spotColor = WarmOrangeShadow),
                        color = WarmOrange,
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(4.dp, Color.White)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text("${stage.id + 1}", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Lanjut!", color = TextDark, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontStyle = FontStyle.Italic)
            }
            stage.isUnlocked -> {
                Surface(
                    modifier = buttonModifier,
                    color = Color.White.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                val stroke = Stroke(
                                    width = 4.dp.toPx(),
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )
                                drawRoundRect(
                                    color = PrimaryGreen,
                                    size = size,
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                                    style = stroke
                                )
                            }
                    ) {
                        Text("${stage.id + 1}", color = PrimaryGreen, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            else -> {
                Surface(
                    modifier = buttonModifier,
                    color = Color(0xFFE5E7EB),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Terkunci",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}
