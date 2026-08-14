package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsentScreen(
    isReviewMode: Boolean = false,
    consentTimestamp: Long = 0L,
    onAcceptConsent: () -> Unit,
    onRevokeConsent: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    var isChecked by remember { mutableStateOf(false) }
    var showRevokeDialog by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val geminiTermsUrl = "https://ai.google.dev/gemini-api/terms"

    Scaffold(
        topBar = {
            if (isReviewMode) {
                TopAppBar(
                    title = {
                        Text(
                            "Informasi Privasi",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LightBackground)
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header Section
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                border = BorderStroke(2.dp, PrimaryGreen.copy(alpha = 0.5f)),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = PrimaryGreen.copy(alpha = 0.15f),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Keamanan & Privasi",
                                tint = LeafGreenDark,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Privasi & Persetujuan Orang Tua",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        color = TextDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Transparansi dan keamanan data anak adalah prioritas utama kami di Taman Kata.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF555555),
                        textAlign = TextAlign.Center
                    )

                    if (isReviewMode) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFE8F5E9),
                            border = BorderStroke(1.dp, PrimaryGreen)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Status Aktif",
                                    tint = LeafGreenDark,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Status: Persetujuan Aktif",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = LeafGreenDark
                                )
                            }
                        }

                        if (consentTimestamp > 0L) {
                            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
                            val dateStr = dateFormat.format(Date(consentTimestamp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Disetujui pada: $dateStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF777777)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Information Items
            PrivacyPointCard(
                icon = Icons.Default.Mic,
                iconTint = ActionOrange,
                title = "Perekaman Suara Anak",
                description = "Aplikasi merekam suara anak saat sesi membaca atau mengeja untuk menilai ketepatan pelafalan (fonemik) dan kelancaran belajar secara langsung."
            )

            Spacer(modifier = Modifier.height(12.dp))

            PrivacyPointCard(
                icon = Icons.Default.AutoAwesome,
                iconTint = WarmOrange,
                title = "Evaluasi Google Gemini API & Penghapusan Cache",
                description = "Audio rekaman dikirim secara aman ke Gemini API (layanan AI dari Google) untuk evaluasi pengucapan. Segera setelah proses penilaian selesai, file rekaman audio di memori perangkat langsung DIHAPUS dan tidak disimpan secara permanen."
            )

            Spacer(modifier = Modifier.height(12.dp))

            PrivacyPointCard(
                icon = Icons.Default.Storage,
                iconTint = PrimaryGreen,
                title = "Penyimpanan 100% Lokal di Perangkat",
                description = "Seluruh progres belajar, bintang pencapaian, dan riwayat sesi latihan anak HANYA disimpan secara lokal di perangkat ini (Room Database) dan tidak pernah diunggah ke server cloud manapun."
            )

            Spacer(modifier = Modifier.height(12.dp))

            PrivacyPointCard(
                icon = Icons.Default.VerifiedUser,
                iconTint = LeafGreenDark,
                title = "Bebas Iklan & Tanpa Pelacakan Komersial",
                description = "Taman Kata sepenuhnya bebas iklan. Kami tidak mengumpulkan, menjual, atau membagikan profil data anak kepada pihak ketiga untuk keperluan pemasaran."
            )

            Spacer(modifier = Modifier.height(12.dp))

            // External Link Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .clickable { uriHandler.openUri(geminiTermsUrl) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Buka tautan",
                        tint = LeafGreenDark,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ketentuan & Kebijakan Privasi Google Gemini API",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = LeafGreenDark
                        )
                        Text(
                            text = "ai.google.dev/gemini-api/terms",
                            style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline),
                            color = Color(0xFF1976D2)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Area
            if (!isReviewMode) {
                // Checkbox
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(
                        width = if (isChecked) 2.dp else 1.dp,
                        color = if (isChecked) PrimaryGreen else Color(0xFFCCCCCC)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isChecked = !isChecked }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { isChecked = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = LeafGreenDark,
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Saya membaca dan menyetujui hal di atas untuk aktivitas belajar anak.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = TextDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Agree Button
                Button(
                    onClick = onAcceptConsent,
                    enabled = isChecked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .shadow(if (isChecked) 8.dp else 0.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WarmOrange,
                        disabledContainerColor = Color(0xFFCCCCCC)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Text(
                            text = "Setuju & Mulai",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            ),
                            color = Color.White
                        )
                    }
                }
            } else {
                // Review Mode Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(4.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text(
                            text = "Tutup",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    OutlinedButton(
                        onClick = { showRevokeDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ActionOrange),
                        border = BorderStroke(1.5.dp, ActionOrange.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = null,
                            tint = ActionOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cabut Persetujuan",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = ActionOrange
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Revoke Confirmation Dialog
        if (showRevokeDialog) {
            AlertDialog(
                onDismissRequest = { showRevokeDialog = false },
                title = {
                    Text(
                        "Cabut Persetujuan?",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Text(
                        "Mencabut persetujuan berarti aplikasi tidak bisa dipakai sampai disetujui ulang. Lanjutkan?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showRevokeDialog = false
                            onRevokeConsent()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ActionOrange)
                    ) {
                        Text("Ya, Cabut", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRevokeDialog = false }) {
                        Text("Batal", color = TextDark)
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
private fun PrivacyPointCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE8E8E8)),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconTint.copy(alpha = 0.12f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF424242),
                    lineHeight = 20.sp
                )
            }
        }
    }
}
