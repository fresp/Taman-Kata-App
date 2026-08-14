package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.SessionHistory
import com.example.ui.TamanKataViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(
    viewModel: TamanKataViewModel,
    onNavigateBack: () -> Unit
) {
    val history by viewModel.sessionHistory.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard Orang Tua") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                val totalSecs = history.sumOf { it.durationSeconds }
                val currentHours = totalSecs / 3600.0
                val targetHours = 60.0
                val remaining = maxOf(0.0, targetHours - currentHours)
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Ringkasan Progres", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Total Belajar: ${String.format("%.1f", currentHours)} Jam", style = MaterialTheme.typography.bodyLarge)
                        Text("Estimasi Sisa Waktu (Target 60 Jam): ${String.format("%.1f", remaining)} Jam", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            if (history.isEmpty()) {
                item {
                    Text(
                        "Belum ada riwayat sesi.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(history) { session ->
                    SessionHistoryCard(session)
                }
            }
        }
    }
}

@Composable
fun SessionHistoryCard(session: SessionHistory) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(session.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = dateString,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Durasi: ${session.durationSeconds} dtk", style = MaterialTheme.typography.bodyMedium)
                Text("Item: ${session.itemsTrainedCount}", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Akurasi: ${session.averageScore}%", style = MaterialTheme.typography.bodyLarge)
                Text("Kemandirian: ${session.independencePercentage}%", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Kelancaran (Fluency): ${session.averageFluency}%", style = MaterialTheme.typography.bodyMedium)
                Text("WCPM: ${session.averageWcpm}", style = MaterialTheme.typography.bodyMedium)
            }
            if (session.averageComprehension > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Pemahaman Literasi: ${session.averageComprehension}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
