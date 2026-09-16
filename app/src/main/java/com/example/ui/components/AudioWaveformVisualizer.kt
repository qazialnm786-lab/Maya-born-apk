package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet

@Composable
fun AudioWaveformVisualizer(
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    audioVolume: Float = 0f,
    barCount: Int = 18,
    barWidth: Dp = 3.dp,
    barSpacing: Dp = 3.dp,
    height: Dp = 36.dp,
    primaryColor: Color = NeonCyan,
    secondaryColor: Color = NeonMagenta
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")

    val anim1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar_anim_1"
    )

    val anim2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(310, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar_anim_2"
    )

    val anim3 by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(530, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar_anim_3"
    )

    Canvas(
        modifier = modifier
            .height(height)
            .width((barWidth + barSpacing) * barCount)
    ) {
        val maxH = size.height
        val bw = barWidth.toPx()
        val spacing = barSpacing.toPx()

        for (i in 0 until barCount) {
            val factor = when (i % 4) {
                0 -> anim1
                1 -> anim2
                2 -> anim3
                else -> (anim1 + anim2) / 2f
            }

            val volumeBoost = if (isActive) (audioVolume * 0.7f).coerceIn(0f, 0.7f) else 0f
            val normalizedHeight = if (isActive) {
                (factor * 0.6f + volumeBoost + 0.15f).coerceIn(0.12f, 1.0f) * maxH
            } else {
                maxH * 0.12f
            }

            val x = i * (bw + spacing)
            val y = (maxH - normalizedHeight) / 2f

            val brush = Brush.verticalGradient(
                colors = listOf(primaryColor, secondaryColor),
                startY = y,
                endY = y + normalizedHeight
            )

            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, y),
                size = Size(bw, normalizedHeight),
                cornerRadius = CornerRadius(bw / 2f, bw / 2f)
            )
        }
    }
}
