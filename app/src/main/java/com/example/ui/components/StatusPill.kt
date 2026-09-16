package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.avatar.MayaAvatarState
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.TextPrimary

@Composable
fun StatusPill(
    modifier: Modifier = Modifier,
    state: MayaAvatarState = MayaAvatarState.IDLE
) {
    val infiniteTransition = rememberInfiniteTransition(label = "status_dot_pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val targetColor = when (state) {
        MayaAvatarState.IDLE -> NeonCyan
        MayaAvatarState.LISTENING -> EmeraldGlow
        MayaAvatarState.THINKING -> AmberGlow
        MayaAvatarState.SPEAKING -> NeonMagenta
        MayaAvatarState.HAPPY -> Color(0xFFFF69B4)
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(300),
        label = "status_color"
    )

    val labelText = when (state) {
        MayaAvatarState.IDLE -> "MAYA ONLINE • READY"
        MayaAvatarState.LISTENING -> "LISTENING..."
        MayaAvatarState.THINKING -> "PROCESSING..."
        MayaAvatarState.SPEAKING -> "MAYA SPEAKING"
        MayaAvatarState.HAPPY -> "FEELING GREAT ✨"
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x22130F26))
            .border(1.dp, animatedColor.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(animatedColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = labelText,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp
        )
    }
}
