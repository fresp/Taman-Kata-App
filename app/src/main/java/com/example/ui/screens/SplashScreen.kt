package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.example.ui.components.KikiExpression
import com.example.ui.components.KikiMascot
import kotlinx.coroutines.delay

import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.WarmOrange

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val scale = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
        delay(1500)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.scale(scale.value * 2f)) {
                KikiMascot(expression = KikiExpression.HAPPY)
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Taman Kata",
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black),
                color = PrimaryGreen,
                modifier = Modifier.scale(scale.value)
            )
        }
    }
}
