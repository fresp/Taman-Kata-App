package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

enum class KikiExpression {
    NEUTRAL, HAPPY, CHEERING
}

@Composable
fun KikiMascot(
    expression: KikiExpression,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(100.dp)) {
        Canvas(modifier = Modifier.size(100.dp)) {
            // Body (Firefly glowing tail)
            val glowColor = Color(0xFFFFEB3B)
            drawCircle(
                color = glowColor.copy(alpha = 0.5f),
                radius = size.width * 0.35f,
                center = Offset(size.width / 2, size.height * 0.65f)
            )
            drawCircle(
                color = glowColor,
                radius = size.width * 0.25f,
                center = Offset(size.width / 2, size.height * 0.65f)
            )

            // Head
            val headColor = Color(0xFF8BC34A)
            drawCircle(
                color = headColor,
                radius = size.width * 0.2f,
                center = Offset(size.width / 2, size.height * 0.35f)
            )

            // Wings
            val wingColor = Color(0x88B3E5FC)
            drawOval(
                color = wingColor,
                topLeft = Offset(size.width * 0.1f, size.height * 0.2f),
                size = Size(size.width * 0.4f, size.height * 0.5f)
            )
            drawOval(
                color = wingColor,
                topLeft = Offset(size.width * 0.5f, size.height * 0.2f),
                size = Size(size.width * 0.4f, size.height * 0.5f)
            )

            // Antennae
            drawLine(
                color = Color.Black,
                start = Offset(size.width * 0.45f, size.height * 0.2f),
                end = Offset(size.width * 0.3f, size.height * 0.1f),
                strokeWidth = 3f
            )
            drawLine(
                color = Color.Black,
                start = Offset(size.width * 0.55f, size.height * 0.2f),
                end = Offset(size.width * 0.7f, size.height * 0.1f),
                strokeWidth = 3f
            )

            // Eyes
            drawCircle(color = Color.White, radius = 8f, center = Offset(size.width * 0.42f, size.height * 0.3f))
            drawCircle(color = Color.White, radius = 8f, center = Offset(size.width * 0.58f, size.height * 0.3f))
            drawCircle(color = Color.Black, radius = 4f, center = Offset(size.width * 0.42f, size.height * 0.3f))
            drawCircle(color = Color.Black, radius = 4f, center = Offset(size.width * 0.58f, size.height * 0.3f))

            // Mouth based on expression
            when (expression) {
                KikiExpression.NEUTRAL -> {
                    drawLine(
                        color = Color.Black,
                        start = Offset(size.width * 0.45f, size.height * 0.4f),
                        end = Offset(size.width * 0.55f, size.height * 0.4f),
                        strokeWidth = 4f
                    )
                }
                KikiExpression.HAPPY -> {
                    drawArc(
                        color = Color.Black,
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(size.width * 0.4f, size.height * 0.35f),
                        size = Size(size.width * 0.2f, size.height * 0.1f),
                        style = Stroke(width = 4f)
                    )
                }
                KikiExpression.CHEERING -> {
                    drawOval(
                        color = Color.Black,
                        topLeft = Offset(size.width * 0.45f, size.height * 0.38f),
                        size = Size(size.width * 0.1f, size.height * 0.08f)
                    )
                }
            }
        }
    }
}
