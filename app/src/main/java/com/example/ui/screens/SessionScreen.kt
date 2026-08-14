package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.TamanKataViewModel
import com.example.ui.components.KikiExpression
import com.example.ui.components.KikiMascot

import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.WarmOrange
import com.example.ui.theme.WarmOrangeShadow
import com.example.ui.theme.ActionOrange
import com.example.ui.theme.TextDark
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke

@Composable
fun SessionScreen(
    stageId: Int,
    viewModel: TamanKataViewModel,
    onSessionFinished: (duration: Int, itemsCount: Int, avgScore: Int, passed: Boolean) -> Unit
) {
    val items by viewModel.getItemsForStage(stageId).collectAsState()
    var currentIndex by remember { mutableStateOf(0) }
    var scoreSum by remember { mutableStateOf(0) }
    val startTime = remember { System.currentTimeMillis() }

    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryGreen)
        }
        return
    }

    if (currentIndex >= items.size) {
        LaunchedEffect(Unit) {
            val duration = ((System.currentTimeMillis() - startTime) / 1000).toInt()
            val avgScore = if (items.isNotEmpty()) scoreSum / items.size else 0
            val passed = avgScore >= 80
            onSessionFinished(duration, items.size, avgScore, passed)
        }
        return
    }

    val currentItem = items[currentIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            KikiMascot(expression = KikiExpression.CHEERING, modifier = Modifier.size(80.dp))
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(2.dp, PrimaryGreen),
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = "${currentIndex + 1} / ${items.size}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = PrimaryGreen,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Skeleton "Item Card"
        Surface(
            modifier = Modifier
                .size(300.dp)
                .shadow(12.dp, RoundedCornerShape(40.dp)),
            shape = RoundedCornerShape(40.dp),
            color = Color.White,
            border = BorderStroke(8.dp, WarmOrange)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = currentItem.text,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 80.sp, fontWeight = FontWeight.Black),
                    color = TextDark,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    viewModel.updateItemProgress(currentItem, 50)
                    scoreSum += 50
                    currentIndex++
                },
                modifier = Modifier.size(120.dp, 80.dp).shadow(8.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ActionOrange),
                border = BorderStroke(4.dp, Color.White)
            ) {
                Text("Ulang", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black))
            }
            Button(
                onClick = {
                    viewModel.updateItemProgress(currentItem, 100)
                    scoreSum += 100
                    currentIndex++
                },
                modifier = Modifier.size(160.dp, 80.dp).shadow(8.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                border = BorderStroke(4.dp, Color.White)
            ) {
                Text("Bisa!", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black))
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}
