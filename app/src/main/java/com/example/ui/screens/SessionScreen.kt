package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.SessionState
import com.example.ui.TamanKataViewModel
import com.example.ui.components.KikiExpression
import com.example.ui.components.KikiMascot
import com.example.ui.theme.ActionOrange
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.TextDark
import com.example.ui.theme.WarmOrange
import com.example.util.AudioRecorderHelper
import com.example.util.TTSHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SessionScreen(
    stageId: Int,
    viewModel: TamanKataViewModel,
    onSessionFinished: (duration: Int, itemsCount: Int, avgScore: Int, passed: Boolean) -> Unit
) {
    val context = LocalContext.current
    val sessionState by viewModel.sessionState.collectAsState()
    
    val ttsHelper = remember { TTSHelper(context) }
    val audioRecorder = remember { AudioRecorderHelper(context) }
    val coroutineScope = rememberCoroutineScope()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasMicPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        viewModel.startSession(stageId)
    }

    DisposableEffect(Unit) {
        onDispose {
            ttsHelper.shutdown()
        }
    }

    if (sessionState is SessionState.Finished) {
        LaunchedEffect(Unit) {
            viewModel.finishSession(onSessionFinished)
        }
        return
    }

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
            val expression = when (sessionState) {
                is SessionState.Feedback -> if ((sessionState as SessionState.Feedback).isCorrect) KikiExpression.HAPPY else KikiExpression.CHEERING
                else -> KikiExpression.NEUTRAL
            }
            KikiMascot(expression = expression, modifier = Modifier.size(80.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        // Main Content based on state
        when (val state = sessionState) {
            is SessionState.Loading -> {
                CircularProgressIndicator(color = PrimaryGreen)
            }
            is SessionState.Playing -> {
                PlayingView(
                    text = state.item.text,
                    isRecording = state.isRecording,
                    isEvaluating = state.isEvaluating,
                    onPlaySound = { ttsHelper.speak(state.item.text) },
                    onToggleRecord = {
                        if (hasMicPermission) {
                            if (state.isRecording) {
                                val base64Audio = audioRecorder.stopRecording()
                                if (base64Audio != null) {
                                    viewModel.evaluateAudio(base64Audio)
                                } else {
                                    viewModel.setRecording(false) // Error recording
                                }
                            } else {
                                audioRecorder.startRecording()
                                viewModel.setRecording(true)
                            }
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                )
            }
            is SessionState.Feedback -> {
                FeedbackView(
                    isCorrect = state.isCorrect,
                    showParentHelp = state.showParentHelp,
                    onNext = { viewModel.continueToNext() },
                    onParentDecide = { isCorrect -> viewModel.manualParentEvaluation(isCorrect) }
                )
            }
            else -> {}
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun PlayingView(
    text: String,
    isRecording: Boolean,
    isEvaluating: Boolean,
    onPlaySound: () -> Unit,
    onToggleRecord: () -> Unit
) {
    // Auto-play TTS on first composition
    LaunchedEffect(text) {
        delay(500)
        onPlaySound()
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                    text = text,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 80.sp, fontWeight = FontWeight.Black),
                    color = TextDark,
                    textAlign = TextAlign.Center
                )
                
                // Sound button
                IconButton(
                    onClick = onPlaySound,
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "Dengarkan", tint = WarmOrange, modifier = Modifier.size(48.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(64.dp))

        if (isEvaluating) {
            CircularProgressIndicator(color = PrimaryGreen)
            Text("Tunggu ya...", color = TextDark, modifier = Modifier.padding(top = 16.dp))
        } else {
            Surface(
                shape = CircleShape,
                color = if (isRecording) ActionOrange else PrimaryGreen,
                border = BorderStroke(4.dp, Color.White),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(96.dp)
                    .clickable { onToggleRecord() }
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isRecording) "Berhenti" else "Rekam",
                    tint = Color.White,
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxSize()
                )
            }
            Text(
                text = if (isRecording) "Tekan untuk Berhenti" else "Tekan untuk Bicara",
                color = TextDark,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
fun FeedbackView(
    isCorrect: Boolean,
    showParentHelp: Boolean,
    onNext: () -> Unit,
    onParentDecide: (Boolean) -> Unit
) {
    LaunchedEffect(isCorrect, showParentHelp) {
        if (!showParentHelp) {
            delay(2000)
            onNext()
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (showParentHelp) {
            Surface(
                modifier = Modifier.padding(32.dp).shadow(8.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                border = BorderStroke(4.dp, ActionOrange)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Bantuan Ayah/Bunda", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = ActionOrange)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Apakah ucapan anak sudah benar?", textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(onClick = { onParentDecide(false) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                            Text("Coba Lagi")
                        }
                        Button(onClick = { onParentDecide(true) }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)) {
                            Text("Benar!")
                        }
                    }
                }
            }
        } else {
            // Visual feedback
            val color = if (isCorrect) PrimaryGreen else WarmOrange
            val message = if (isCorrect) "Bagus Sekali!" else "Ayo Coba Lagi!"
            
            Text(
                text = message,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 48.sp, fontWeight = FontWeight.Black),
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}
