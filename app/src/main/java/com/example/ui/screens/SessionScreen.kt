package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.draw.scale
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
import com.example.util.NetworkUtils
import com.example.util.TTSHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SessionScreen(
    stageId: Int,
    viewModel: TamanKataViewModel,
    onSessionFinished: (duration: Int, itemsCount: Int, avgScore: Int, passed: Boolean, isTimeLimit: Boolean) -> Unit,
    onNavigateToLibrary: () -> Unit
) {
    val context = LocalContext.current
    val sessionState by viewModel.sessionState.collectAsState()
    
    val ttsHelper = remember { TTSHelper(context) }
    val audioRecorder = remember { AudioRecorderHelper(context) }
    val sttHelper = remember { com.example.util.OnDeviceSttHelper(context) }
    val coroutineScope = rememberCoroutineScope()

    var isSoftLimit by remember { mutableStateOf(false) }
    var isOnline by remember { mutableStateOf(NetworkUtils.isOnline(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2000L)
            isOnline = NetworkUtils.isOnline(context)
            isSoftLimit = viewModel.isSoftLimitReached()
        }
    }

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
        // Header with Mascot, Soft Limit Hint, and Non-numeric Item Progress
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val expression = when (val state = sessionState) {
                    is SessionState.Feedback -> {
                        if (state.isCorrect) {
                            if (state.item.stageId == 5 && state.item.text.endsWith("!") && state.intonationMatched) {
                                KikiExpression.CHEERING
                            } else {
                                KikiExpression.HAPPY
                            }
                        } else {
                            KikiExpression.NEUTRAL
                        }
                    }
                    is SessionState.SttFallback -> KikiExpression.NEUTRAL
                    else -> if (isSoftLimit) KikiExpression.HAPPY else KikiExpression.NEUTRAL
                }
                KikiMascot(expression = expression, modifier = Modifier.size(72.dp))

                AnimatedVisibility(
                    visible = isSoftLimit,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFFF9C4),
                        border = BorderStroke(2.dp, WarmOrange.copy(alpha = 0.5f)),
                        shadowElevation = 4.dp,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = "🌟 Sebentar lagi selesai ya!",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextDark,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Friendly non-numeric Item Progress Card
            val (currentItemNum, totalItemsNum) = viewModel.getCurrentItemProgress()
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(2.dp, PrimaryGreen.copy(alpha = 0.4f)),
                shadowElevation = 4.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "⭐ $currentItemNum / $totalItemsNum",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextDark
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !isOnline,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFFF3E0),
                border = BorderStroke(2.dp, ActionOrange.copy(alpha = 0.6f)),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("📡", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Sedang offline — sesi ini akan dibantu Ayah/Bunda untuk menilai ya",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextDark
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Main Content based on state
        when (val state = sessionState) {
            is SessionState.Loading -> {
                CircularProgressIndicator(color = PrimaryGreen)
            }
            is SessionState.ResumePrompt -> {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val remainingCount = try {
                        org.json.JSONArray(state.checkpoint.remainingItemIds).length()
                    } catch (e: Exception) { 0 }
                    val completedCount = try {
                        org.json.JSONArray(state.checkpoint.completedItemIds).length()
                    } catch (e: Exception) { 0 }
                    val total = remainingCount + completedCount
                    
                    Text(
                        text = "Sesi Tertunda",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Sepertinya ada sesi sebelumnya yang belum selesai (progres $completedCount dari $total item). Lanjutkan dari situ, atau mulai sesi baru?",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = { viewModel.startNewSessionOverridingCheckpoint(stageId) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                        ) {
                            Text("Mulai Baru")
                        }
                        Button(
                            onClick = { viewModel.resumeSession(state.checkpoint) },
                            colors = ButtonDefaults.buttonColors(containerColor = ActionOrange)
                        ) {
                            Text("Lanjutkan")
                        }
                    }
                }
            }
            is SessionState.Playing -> {
                PlayingView(
                    item = state.item,
                    isRecording = state.isRecording,
                    isEvaluating = state.isEvaluating,
                    onPlaySound = { isReplay -> 
                        viewModel.onTtsPlayed(isReplay)
                        ttsHelper.speak(state.item.text) 
                    },
                    onToggleRecord = {
                        if (hasMicPermission) {
                            if (state.isRecording) {
                                val duration = viewModel.getRecordingDuration()
                                if (duration < 400) {
                                    viewModel.setRecording(false)
                                    audioRecorder.stopRecording()
                                    Toast.makeText(context, "Kakak belum dengar suaranya, coba tekan lalu ucapkan ya!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val base64Audio = audioRecorder.stopRecording()
                                    if (base64Audio != null) {
                                        val onlineStatus = NetworkUtils.isOnline(context)
                                        viewModel.evaluateAudio(base64Audio, isOnline = onlineStatus)
                                    } else {
                                        viewModel.setRecording(false) // Error recording
                                    }
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
            is SessionState.SttFallback -> {
                LaunchedEffect(state) {
                    val recognized = sttHelper.startListening()
                    viewModel.evaluateSttResult(state.item, recognized)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                    KikiMascot(
                        expression = KikiExpression.NEUTRAL,
                        modifier = Modifier.size(96.dp)
                    )
                    Text(
                        text = "Oh, ada gangguan koneksi. Coba ucapkan lagi pelan-pelan ya!", 
                        color = TextDark, 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
            is SessionState.Feedback -> {
                FeedbackView(
                    state = state,
                    onNext = { viewModel.continueToNext() },
                    onParentDecide = { isCorrect -> viewModel.manualParentEvaluation(isCorrect) },
                    lastRecordingPath = audioRecorder.lastRecordingPath
                )
            }
            is SessionState.Error -> {
                ErrorView(
                    state = state,
                    onRetry = { viewModel.resetToPlaying() }
                )
            }
            is SessionState.Comprehension -> {
                ComprehensionView(
                    state = state,
                    onAnswerSelected = { idx -> viewModel.answerComprehension(idx) }
                )
            }
            is SessionState.Graduation -> {
                GraduationCertificateView(
                    studentName = state.studentName,
                    totalHours = state.totalHours,
                    onFinish = { onSessionFinished(0, 0, 100, true, false) },
                    onNavigateToLibrary = onNavigateToLibrary
                )
            }
            else -> {}
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun PlayingView(
    item: com.example.data.LearningItem,
    isRecording: Boolean,
    isEvaluating: Boolean,
    onPlaySound: (Boolean) -> Unit,
    onToggleRecord: () -> Unit
) {
    // Auto-play TTS on first composition
    LaunchedEffect(item.text) {
        delay(500)
        onPlaySound(false)
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
                FadingScaffoldText(
                    text = item.text,
                    syllables = item.syllables,
                    score = item.lastAccuracyScore
                )
                
                // Sound button
                IconButton(
                    onClick = { onPlaySound(true) },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Dengarkan", tint = WarmOrange, modifier = Modifier.size(48.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(64.dp))

        if (isEvaluating) {
            val messages = listOf("Kiki lagi dengerin...", "Sebentar ya...", "Lagi mikir...", "Wah, suaranya bagus!")
            var messageIndex by remember { mutableStateOf(0) }
            
            LaunchedEffect(Unit) {
                while(true) {
                    delay(2000)
                    messageIndex = (messageIndex + 1) % messages.size
                }
            }
            
            val infiniteTransition = rememberInfiniteTransition(label = "eval")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                KikiMascot(
                    expression = KikiExpression.NEUTRAL,
                    modifier = Modifier
                        .size(96.dp)
                        .scale(scale)
                )
                Text(
                    text = messages[messageIndex], 
                    color = TextDark, 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
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
fun FadingScaffoldText(text: String, syllables: String, score: Int) {
    val parts = if (syllables.isNotEmpty()) syllables.split("-") else listOf(text)
    
    if (parts.size <= 1 || score >= 80) {
        // High mastery or no syllables to split: Plain text
        Text(
            text = text,
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 80.sp, fontWeight = FontWeight.Black),
            color = TextDark,
            textAlign = TextAlign.Center
        )
    } else if (score >= 40) {
        // Medium mastery: slight spacing
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            parts.forEach { part ->
                Text(
                    text = part,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 80.sp, fontWeight = FontWeight.Black),
                    color = TextDark,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        // Low mastery: high contrast colors for each syllable
        val colors = listOf(PrimaryGreen, ActionOrange, WarmOrange)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            parts.forEachIndexed { index, part ->
                Text(
                    text = part,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 80.sp, fontWeight = FontWeight.Black),
                    color = colors[index % colors.size],
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ErrorView(state: SessionState.Error, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        Text(
            text = "🔌",
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = state.message,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = WarmOrange,
            textAlign = TextAlign.Center
        )
        
        if (com.example.BuildConfig.DEBUG && state.debugMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "DEBUG: ${state.debugMessage}",
                style = MaterialTheme.typography.bodySmall,
                color = androidx.compose.ui.graphics.Color.Gray,
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
        ) {
            Text("Coba Lagi", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun FeedbackView(
    state: SessionState.Feedback,
    onNext: () -> Unit,
    onParentDecide: (Boolean) -> Unit,
    lastRecordingPath: String? = null
) {
    LaunchedEffect(state.isCorrect, state.showParentHelp) {
        if (!state.showParentHelp) {
            delay(2000)
            onNext()
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (state.showParentHelp) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val mediaPlayer = remember { android.media.MediaPlayer() }
            DisposableEffect(Unit) {
                onDispose { mediaPlayer.release() }
            }

            Surface(
                modifier = Modifier.padding(32.dp).shadow(8.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                border = BorderStroke(4.dp, ActionOrange)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val isOfflineHelp = state.parentHelpReason == "OFFLINE"
                    Text(
                        text = if (isOfflineHelp) "Bantuan Ayah/Bunda (Offline)" else "Bantuan Ayah/Bunda",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = ActionOrange
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isOfflineHelp) {
                            "Internet sedang gangguan, yuk minta Ayah/Bunda bantu dengar ya!"
                        } else {
                            "Apakah ucapan anak sudah benar?"
                        },
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (lastRecordingPath != null) {
                        Button(
                            onClick = {
                                try {
                                    mediaPlayer.reset()
                                    mediaPlayer.setDataSource(lastRecordingPath)
                                    mediaPlayer.prepare()
                                    mediaPlayer.start()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ActionOrange)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Play")
                            Spacer(Modifier.width(8.dp))
                            Text("Putar Suara Anak")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
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
            val color = if (state.isCorrect) PrimaryGreen else WarmOrange
            val message = if (state.isCorrect) "Bagus Sekali!" else "Ayo Coba Lagi!"
            
            Text(
                text = message,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 48.sp, fontWeight = FontWeight.Black),
                color = color,
                textAlign = TextAlign.Center
            )

            // Tahap 5 (3+ Suku Kata) Kecepatan Gabung Gamification
            if (state.item.stageId == 4 && state.isCorrect) {
                Spacer(modifier = Modifier.height(16.dp))
                val speedText = when {
                    state.fluency >= 85 -> "🚀 Secepat Roket!"
                    state.fluency >= 60 -> "🚗 Sekencang Mobil!"
                    else -> "🐢 Santai seperti Kura-kura!"
                }
                Text(
                    text = speedText,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = ActionOrange
                )
            }
            
            // Tahap 6 (Kalimat) Intonasi Gamification
            if (state.item.stageId == 5 && state.isCorrect && state.intonationMatched) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "🎶 Nada Bicaramu Pas Sekali!",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = ActionOrange
                )
            }
        }
    }
}

@Composable
fun ComprehensionView(state: SessionState.Comprehension, onAnswerSelected: (Int) -> Unit) {
    val currentQuestion = state.questions[state.currentQuestionIndex]
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp).fillMaxWidth()
    ) {
        Text(
            text = currentQuestion.question,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextDark,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            currentQuestion.options.forEachIndexed { index, optionText ->
                Button(
                    onClick = { onAnswerSelected(index) },
                    modifier = Modifier.weight(1f).height(100.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ActionOrange)
                ) {
                    Text(
                        text = optionText,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun GraduationCertificateView(studentName: String, totalHours: Double, onFinish: () -> Unit, onNavigateToLibrary: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp).fillMaxWidth()
    ) {
        Text(
            text = "🎉 SELAMAT! 🎉",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
            color = PrimaryGreen
        )
        Spacer(modifier = Modifier.height(16.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(4.dp, ActionOrange),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Text("SERTIFIKAT KELULUSAN", style = MaterialTheme.typography.headlineSmall, color = TextDark)
                Spacer(modifier = Modifier.height(16.dp))
                Text(studentName, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = ActionOrange)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Telah menyelesaikan program membaca\ndengan total waktu belajar:", textAlign = TextAlign.Center)
                Text(String.format("%.1f Jam", totalHours), style = MaterialTheme.typography.headlineMedium, color = PrimaryGreen, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onNavigateToLibrary,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ActionOrange)
        ) {
            Text("Baca Dongeng Yuk!", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
        ) {
            Text("Kembali ke Beranda", style = MaterialTheme.typography.titleLarge)
        }
    }
}
