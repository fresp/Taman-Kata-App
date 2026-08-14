package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.components.KikiExpression
import com.example.ui.components.KikiMascot
import com.example.ui.theme.SoftYellow

import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.WarmOrange
import com.example.ui.theme.WarmOrangeShadow
import com.example.ui.theme.PrimaryGreen
import androidx.compose.foundation.BorderStroke
import com.example.ui.theme.TextDark

@Composable
fun ResultScreen(
    avgScore: Int,
    passed: Boolean,
    isTimeLimit: Boolean = false,
    onBackToRoadmap: () -> Unit
) {
    val stars = when {
        avgScore >= 90 -> 3
        avgScore >= 70 -> 2
        avgScore >= 40 -> 1
        else -> 0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        KikiMascot(
            expression = if (isTimeLimit || passed) KikiExpression.HAPPY else KikiExpression.NEUTRAL,
            modifier = Modifier.size(150.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = if (isTimeLimit) "Waktunya Istirahat Dulu! 🌟" else if (passed) "Hore! Berhasil!" else "Ayo coba lagi!",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
            color = PrimaryGreen,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        if (isTimeLimit) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Hebat sekali belajarnya hari ini!\nNanti kita lanjut lagi ya. Semua bintangmu tersimpan rapi ✨",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = TextDark,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            for (i in 1..3) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Star",
                    modifier = Modifier.size(64.dp),
                    tint = if (i <= stars) WarmOrange else Color.Black.copy(alpha = 0.1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onBackToRoadmap,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .shadow(8.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WarmOrange),
            border = BorderStroke(4.dp, Color.White)
        ) {
            Text(
                if (isTimeLimit) "Selesai & Istirahat" else "Kembali ke Peta",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
            )
        }
    }
}
