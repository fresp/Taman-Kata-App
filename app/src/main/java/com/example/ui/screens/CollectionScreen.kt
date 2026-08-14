package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LearningItem
import com.example.data.Stage
import com.example.ui.TamanKataViewModel
import com.example.ui.components.KikiExpression
import com.example.ui.components.KikiMascot
import com.example.ui.theme.ActionOrange
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.TextDark
import com.example.ui.theme.WarmOrange
import com.example.util.TTSHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    viewModel: TamanKataViewModel,
    onNavigateBack: () -> Unit
) {
    val masteredItems by viewModel.masteredItems.collectAsState()
    val stages by viewModel.stages.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val ttsHelper = remember { TTSHelper(context) }

    DisposableEffect(Unit) {
        onDispose {
            ttsHelper.shutdown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Koleksiku", fontWeight = FontWeight.Bold, color = PrimaryGreen) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = PrimaryGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        if (masteredItems.isEmpty()) {
            EmptyCollectionState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                onBack = onNavigateBack
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                stages.forEach { stage ->
                    val itemsInStage = masteredItems.filter { it.stageId == stage.id }
                    if (itemsInStage.isNotEmpty()) {
                        item {
                            StageCollectionSection(
                                stage = stage,
                                items = itemsInStage,
                                onPlayTts = { word ->
                                    coroutineScope.launch {
                                        ttsHelper.speak(word)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyCollectionState(modifier: Modifier = Modifier, onBack: () -> Unit) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.size(160.dp)) {
            KikiMascot(expression = KikiExpression.HAPPY)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Koleksimu masih kosong, ayo mulai belajar untuk mengumpulkan kata-kata pertamamu!",
            style = MaterialTheme.typography.titleLarge,
            color = TextDark,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
        ) {
            Text("Kembali ke Peta", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StageCollectionSection(
    stage: Stage,
    items: List<LearningItem>,
    onPlayTts: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stage.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = WarmOrange.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "${items.size} dikuasai",
                    style = MaterialTheme.typography.labelLarge,
                    color = ActionOrange,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items) { item ->
                CollectionCard(
                    word = item.text,
                    onClick = { onPlayTts(item.text) }
                )
            }
        }
    }
}

@Composable
fun CollectionCard(word: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(2.dp, WarmOrange),
        shadowElevation = 4.dp,
        modifier = Modifier
            .size(width = 140.dp, height = 100.dp)
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background decorative star
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = WarmOrange.copy(alpha = 0.1f),
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 16.dp, y = 16.dp)
            )
            
            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = word,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
            
            // Small badge
            Surface(
                shape = RoundedCornerShape(bottomStart = 8.dp),
                color = WarmOrange,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(4.dp).size(12.dp)
                )
            }
        }
    }
}
