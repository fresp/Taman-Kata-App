package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.VolumeUp
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
import com.example.data.Story
import com.example.ui.TamanKataViewModel
import com.example.ui.theme.ActionOrange
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.TextDark
import com.example.ui.theme.WarmOrange
import com.example.util.TTSHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryLibraryScreen(
    viewModel: TamanKataViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToStory: (Int) -> Unit
) {
    val stories by viewModel.stories.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perpustakaan", fontWeight = FontWeight.Bold, color = PrimaryGreen) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Selamat membaca!",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextDark,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(stories) { story ->
                StoryCard(story = story, onClick = { onNavigateToStory(story.id) })
            }
        }
    }
}

@Composable
fun StoryCard(story: Story, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(2.dp, PrimaryGreen.copy(alpha = 0.5f)),
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = PrimaryGreen.copy(alpha = 0.1f),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoStories,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = story.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = story.category,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryReaderScreen(
    storyId: Int,
    viewModel: TamanKataViewModel,
    onNavigateBack: () -> Unit
) {
    val stories by viewModel.stories.collectAsState()
    val story = stories.find { it.id == storyId }

    if (story == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val paragraphs = remember(story.body) { story.body.split("\n\n").filter { it.isNotBlank() } }
    var currentParagraphIndex by remember { mutableStateOf(0) }
    
    val context = LocalContext.current
    val ttsHelper = remember { TTSHelper(context) }
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        onDispose { ttsHelper.shutdown() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(story.title, fontWeight = FontWeight.Bold, color = PrimaryGreen) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = PrimaryGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (currentParagraphIndex > 0) currentParagraphIndex-- },
                        enabled = currentParagraphIndex > 0
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Sebelumnya", modifier = Modifier.size(32.dp))
                    }
                    
                    Text(
                        text = "${currentParagraphIndex + 1} / ${paragraphs.size}",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextDark
                    )
                    
                    IconButton(
                        onClick = { if (currentParagraphIndex < paragraphs.size - 1) currentParagraphIndex++ },
                        enabled = currentParagraphIndex < paragraphs.size - 1
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Selanjutnya", modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFFDFBF7)) // Sedikit warna kertas
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = paragraphs[currentParagraphIndex],
                style = MaterialTheme.typography.headlineMedium.copy(
                    lineHeight = 40.sp,
                    fontSize = 24.sp
                ),
                color = TextDark,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = { 
                    coroutineScope.launch {
                        ttsHelper.speak(paragraphs[currentParagraphIndex])
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ActionOrange),
                shape = CircleShape,
                modifier = Modifier.size(64.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.VolumeUp, contentDescription = "Bacakan", modifier = Modifier.size(32.dp))
            }
        }
    }
}
