package com.example.ui.avatar

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MayaAnimeAvatar(
    modifier: Modifier = Modifier,
    state: MayaAvatarState = MayaAvatarState.IDLE,
    audioVolume: Float = 0f,
    onAvatarClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_anim")

    // 1. Sinusoidal floating offset (idle breathing)
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating"
    )

    // 2. Subtle breathing scale
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.99f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // 3. Holographic ring rotation
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rotation"
    )

    // 4. Hair physics sway
    val hairSway by infiniteTransition.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hair_sway"
    )

    // 5. Lip-sync mouth animation when speaking
    val mouthAnim by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(180, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mouth_speaking"
    )

    // 6. Natural blinking timer (blinks every ~3.5 to 4.5 seconds)
    var isBlinking by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3400)
            isBlinking = true
            delay(140)
            isBlinking = false
            // occasional double blink
            if (Math.random() > 0.65) {
                delay(120)
                isBlinking = true
                delay(120)
                isBlinking = false
            }
        }
    }

    val blinkProgress by animateFloatAsState(
        targetValue = if (isBlinking) 1f else 0f,
        animationSpec = tween(durationMillis = 90),
        label = "blink_progress"
    )

    // 7. Head tilt depending on state
    val targetHeadTilt = when (state) {
        MayaAvatarState.LISTENING -> 4.5f
        MayaAvatarState.THINKING -> -3.5f
        MayaAvatarState.HAPPY -> 2.0f
        MayaAvatarState.SPEAKING -> 1.0f
        MayaAvatarState.IDLE -> 0f
    }
    val headTilt by animateFloatAsState(
        targetValue = targetHeadTilt,
        animationSpec = tween(durationMillis = 400),
        label = "head_tilt"
    )

    // State-based accent glow color
    val stateGlowColor = when (state) {
        MayaAvatarState.LISTENING -> EmeraldGlow
        MayaAvatarState.THINKING -> AmberGlow
        MayaAvatarState.SPEAKING -> NeonMagenta
        MayaAvatarState.HAPPY -> Color(0xFFFF69B4)
        MayaAvatarState.IDLE -> NeonCyan
    }

    Box(
        modifier = modifier
            .size(280.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onAvatarClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height
            val centerX = canvasW / 2f
            val centerY = canvasH / 2f

            // A. Draw Holographic Sci-Fi Halo & Particle Rings in background
            drawHolographicHalo(
                centerX = centerX,
                centerY = centerY,
                rotation = ringRotation,
                glowColor = stateGlowColor,
                state = state,
                audioVolume = audioVolume
            )

            // Apply Breathing + Floating translation
            translate(top = floatOffset) {
                scale(scaleX = breathingScale, scaleY = breathingScale, pivot = Offset(centerX, centerY)) {
                    rotate(degrees = headTilt, pivot = Offset(centerX, centerY + 30f)) {

                        // B. Back Hair: Twin-tails with dynamic physics sway
                        drawBackHairTwinTails(
                            centerX = centerX,
                            centerY = centerY,
                            sway = hairSway
                        )

                        // C. Cybernetic Assistant Outfit (Shoulders, Collar, Glowing Circuits)
                        drawCyberOutfit(
                            centerX = centerX,
                            centerY = centerY,
                            stateGlowColor = stateGlowColor
                        )

                        // D. Neck & Face Base (Anime chin, jaw, skin tones)
                        drawAnimeFaceBase(
                            centerX = centerX,
                            centerY = centerY
                        )

                        // E. Cheek Blush (Anime cuteness)
                        drawAnimeBlush(
                            centerX = centerX,
                            centerY = centerY,
                            state = state
                        )

                        // F. Expressive Anime Eyes (Blinking, Glancing, Sparkles)
                        drawAnimeEyes(
                            centerX = centerX,
                            centerY = centerY,
                            blinkProgress = blinkProgress,
                            state = state
                        )

                        // G. Anime Eyebrows
                        drawAnimeEyebrows(
                            centerX = centerX,
                            centerY = centerY,
                            state = state
                        )

                        // H. Cute Anime Nose
                        drawAnimeNose(
                            centerX = centerX,
                            centerY = centerY
                        )

                        // I. Animated Lip-sync Mouth
                        drawAnimeMouth(
                            centerX = centerX,
                            centerY = centerY,
                            state = state,
                            mouthAnim = mouthAnim,
                            audioVolume = audioVolume
                        )

                        // J. Front Bangs and Hair Layers
                        drawFrontHairAndBangs(
                            centerX = centerX,
                            centerY = centerY,
                            sway = hairSway
                        )

                        // K. Futuristic Headset (Glow LED ring, mic boom)
                        drawFuturisticHeadset(
                            centerX = centerX,
                            centerY = centerY,
                            glowColor = stateGlowColor,
                            state = state
                        )
                    }
                }
            }
        }
    }
}

/**
 * Layer A: Holographic Cybernetic Ring & Particle Waves
 */
private fun DrawScope.drawHolographicHalo(
    centerX: Float,
    centerY: Float,
    rotation: Float,
    glowColor: Color,
    state: MayaAvatarState,
    audioVolume: Float
) {
    val radius = size.width * 0.44f

    // Soft radial aura behind avatar
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                glowColor.copy(alpha = 0.28f + (audioVolume * 0.2f)),
                glowColor.copy(alpha = 0.08f),
                Color.Transparent
            ),
            center = Offset(centerX, centerY),
            radius = radius * 1.25f
        ),
        radius = radius * 1.25f,
        center = Offset(centerX, centerY)
    )

    // Rotating tech ring with tick marks
    rotate(degrees = rotation, pivot = Offset(centerX, centerY)) {
        // Outer fine ring
        drawCircle(
            color = glowColor.copy(alpha = 0.35f),
            radius = radius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 1.5f)
        )

        // Concentric inner dashed/accent arcs
        drawArc(
            color = glowColor.copy(alpha = 0.6f),
            startAngle = 0f,
            sweepAngle = 45f,
            useCenter = false,
            topLeft = Offset(centerX - radius * 0.92f, centerY - radius * 0.92f),
            size = Size(radius * 1.84f, radius * 1.84f),
            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )
        drawArc(
            color = glowColor.copy(alpha = 0.6f),
            startAngle = 120f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = Offset(centerX - radius * 0.92f, centerY - radius * 0.92f),
            size = Size(radius * 1.84f, radius * 1.84f),
            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )
        drawArc(
            color = glowColor.copy(alpha = 0.6f),
            startAngle = 240f,
            sweepAngle = 45f,
            useCenter = false,
            topLeft = Offset(centerX - radius * 0.92f, centerY - radius * 0.92f),
            size = Size(radius * 1.84f, radius * 1.84f),
            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )

        // Tech tick dots
        val tickCount = 12
        for (i in 0 until tickCount) {
            val angle = (i * 360f / tickCount) * (Math.PI / 180f)
            val px = centerX + (radius * 1.05f) * cos(angle).toFloat()
            val py = centerY + (radius * 1.05f) * sin(angle).toFloat()
            drawCircle(
                color = glowColor.copy(alpha = if (i % 3 == 0) 0.8f else 0.35f),
                radius = if (i % 3 == 0) 3.5f else 2f,
                center = Offset(px, py)
            )
        }
    }

    // Audio reactive pulse ripples (when listening or speaking)
    if (state == MayaAvatarState.LISTENING || state == MayaAvatarState.SPEAKING) {
        val pulse = 1f + audioVolume * 0.35f
        drawCircle(
            color = glowColor.copy(alpha = 0.25f * (1f - audioVolume.coerceIn(0f, 0.8f))),
            radius = radius * pulse * 1.08f,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2f)
        )
    }
}

/**
 * Layer B: Back Anime Twin-Tails Hair with physics sway
 */
private fun DrawScope.drawBackHairTwinTails(
    centerX: Float,
    centerY: Float,
    sway: Float
) {
    val hairGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFD4C7F5), // Lavender highlight
            Color(0xFF9E86E0), // Soft anime violet
            Color(0xFF6B4EB8)  // Deep shadow purple
        )
    )

    // Left Twin-tail
    val leftPath = Path().apply {
        moveTo(centerX - 50f, centerY - 20f)
        cubicTo(
            centerX - 95f + sway * 1.8f, centerY + 30f,
            centerX - 110f + sway * 2.5f, centerY + 110f,
            centerX - 75f + sway * 2.2f, centerY + 155f
        )
        cubicTo(
            centerX - 60f + sway * 1.5f, centerY + 130f,
            centerX - 65f + sway * 1.2f, centerY + 60f,
            centerX - 42f, centerY + 20f
        )
        close()
    }
    drawPath(path = leftPath, brush = hairGradient)

    // Right Twin-tail
    val rightPath = Path().apply {
        moveTo(centerX + 50f, centerY - 20f)
        cubicTo(
            centerX + 95f - sway * 1.8f, centerY + 30f,
            centerX + 110f - sway * 2.5f, centerY + 110f,
            centerX + 75f - sway * 2.2f, centerY + 155f
        )
        cubicTo(
            centerX + 60f - sway * 1.5f, centerY + 130f,
            centerX + 65f - sway * 1.2f, centerY + 60f,
            centerX + 42f, centerY + 20f
        )
        close()
    }
    drawPath(path = rightPath, brush = hairGradient)

    // Cute cyber hair ribbons/rings holding twin-tails
    drawCircle(
        color = NeonCyan,
        radius = 7.5f,
        center = Offset(centerX - 52f, centerY - 15f),
        style = Stroke(width = 3f)
    )
    drawCircle(
        color = NeonCyan,
        radius = 7.5f,
        center = Offset(centerX + 52f, centerY - 15f),
        style = Stroke(width = 3f)
    )
}

/**
 * Layer C: Futuristic Cybernetic Assistant Outfit
 */
private fun DrawScope.drawCyberOutfit(
    centerX: Float,
    centerY: Float,
    stateGlowColor: Color
) {
    val shoulderY = centerY + 86f

    // Futuristic Jacket Shoulders
    val jacketPath = Path().apply {
        moveTo(centerX - 24f, shoulderY - 8f)
        lineTo(centerX - 85f, shoulderY + 36f)
        lineTo(centerX - 92f, centerY + 160f)
        lineTo(centerX + 92f, centerY + 160f)
        lineTo(centerX + 85f, shoulderY + 36f)
        lineTo(centerX + 24f, shoulderY - 8f)
        close()
    }

    val jacketBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1E1738),
            Color(0xFF130E26),
            Color(0xFF0C091A)
        )
    )
    drawPath(path = jacketPath, brush = jacketBrush)

    // Jacket Trim & Cyber Collar (High-tech white/silver with neon accents)
    val collarPath = Path().apply {
        moveTo(centerX - 30f, shoulderY - 14f)
        lineTo(centerX - 16f, shoulderY + 12f)
        lineTo(centerX, shoulderY + 18f)
        lineTo(centerX + 16f, shoulderY + 12f)
        lineTo(centerX + 30f, shoulderY - 14f)
        lineTo(centerX + 22f, shoulderY - 4f)
        lineTo(centerX, shoulderY + 2f)
        lineTo(centerX - 22f, shoulderY - 4f)
        close()
    }
    drawPath(path = collarPath, color = Color(0xFFE2DCF7))

    // Glowing Cyber Circuit Trims on Jacket
    val circuitPath = Path().apply {
        moveTo(centerX - 60f, shoulderY + 24f)
        lineTo(centerX - 35f, shoulderY + 38f)
        lineTo(centerX - 35f, centerY + 155f)
        moveTo(centerX + 60f, shoulderY + 24f)
        lineTo(centerX + 35f, shoulderY + 38f)
        lineTo(centerX + 35f, centerY + 155f)
    }
    drawPath(
        path = circuitPath,
        color = stateGlowColor.copy(alpha = 0.85f),
        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
    )

    // Center Core Emblem on Chest
    drawCircle(
        color = stateGlowColor,
        radius = 5.5f,
        center = Offset(centerX, shoulderY + 34f)
    )
    drawCircle(
        color = Color.White,
        radius = 2.5f,
        center = Offset(centerX, shoulderY + 34f)
    )
}

/**
 * Layer D: Neck & Anime Face Base
 */
private fun DrawScope.drawAnimeFaceBase(
    centerX: Float,
    centerY: Float
) {
    // Neck
    val neckPath = Path().apply {
        moveTo(centerX - 15f, centerY + 45f)
        lineTo(centerX - 15f, centerY + 85f)
        lineTo(centerX + 15f, centerY + 85f)
        lineTo(centerX + 15f, centerY + 45f)
        close()
    }
    drawPath(
        path = neckPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF3D2C4), // Neck shadow
                Color(0xFFFFDFD3)
            )
        )
    )

    // Soft shadow under chin
    drawCircle(
        color = Color(0x33B8887A),
        radius = 14f,
        center = Offset(centerX, centerY + 58f)
    )

    // Anime Head & Chin contour
    val facePath = Path().apply {
        moveTo(centerX - 50f, centerY - 15f)
        cubicTo(
            centerX - 54f, centerY + 22f,
            centerX - 36f, centerY + 52f,
            centerX, centerY + 62f // Soft pointed anime chin
        )
        cubicTo(
            centerX + 36f, centerY + 52f,
            centerX + 54f, centerY + 22f,
            centerX + 50f, centerY - 15f
        )
        cubicTo(
            centerX + 48f, centerY - 55f,
            centerX - 48f, centerY - 55f,
            centerX - 50f, centerY - 15f
        )
        close()
    }

    val skinBrush = Brush.radialGradient(
        colors = listOf(
            Color(0xFFFFF6F0), // Soft anime fair skin
            Color(0xFFFFE8DC),
            Color(0xFFFCDAC9)
        ),
        center = Offset(centerX, centerY),
        radius = 65f
    )
    drawPath(path = facePath, brush = skinBrush)
}

/**
 * Layer E: Anime Blush
 */
private fun DrawScope.drawAnimeBlush(
    centerX: Float,
    centerY: Float,
    state: MayaAvatarState
) {
    val blushAlpha = when (state) {
        MayaAvatarState.HAPPY -> 0.65f
        MayaAvatarState.SPEAKING -> 0.45f
        else -> 0.32f
    }

    val blushBrush = Brush.radialGradient(
        colors = listOf(
            Color(0xFFFF5C8D).copy(alpha = blushAlpha),
            Color.Transparent
        )
    )

    // Left cheek blush
    drawCircle(
        brush = blushBrush,
        radius = 16f,
        center = Offset(centerX - 32f, centerY + 18f)
    )

    // Right cheek blush
    drawCircle(
        brush = blushBrush,
        radius = 16f,
        center = Offset(centerX + 32f, centerY + 18f)
    )

    // Cute anime blush diagonal accent dashes
    if (state == MayaAvatarState.HAPPY || state == MayaAvatarState.SPEAKING) {
        drawLine(
            color = Color(0xFFFF4081).copy(alpha = 0.5f),
            start = Offset(centerX - 35f, centerY + 15f),
            end = Offset(centerX - 29f, centerY + 21f),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFFF4081).copy(alpha = 0.5f),
            start = Offset(centerX + 29f, centerY + 15f),
            end = Offset(centerX + 35f, centerY + 21f),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Layer F: Expressive Anime Eyes (Iris gradients, pupils, highlights, eyelids)
 */
private fun DrawScope.drawAnimeEyes(
    centerX: Float,
    centerY: Float,
    blinkProgress: Float,
    state: MayaAvatarState
) {
    val eyeSpacing = 26f
    val eyeCenterY = centerY - 2f

    val eyeGlanceX = when (state) {
        MayaAvatarState.THINKING -> 3.5f
        MayaAvatarState.LISTENING -> -1.5f
        else -> 0f
    }
    val eyeGlanceY = when (state) {
        MayaAvatarState.THINKING -> -3.0f
        else -> 0f
    }

    // When HAPPY, draw cute curved anime crescent eyes (^.^)
    if (state == MayaAvatarState.HAPPY) {
        val happyEyeLeft = Path().apply {
            moveTo(centerX - eyeSpacing - 14f, eyeCenterY + 4f)
            quadraticTo(centerX - eyeSpacing, eyeCenterY - 10f, centerX - eyeSpacing + 14f, eyeCenterY + 4f)
        }
        val happyEyeRight = Path().apply {
            moveTo(centerX + eyeSpacing - 14f, eyeCenterY + 4f)
            quadraticTo(centerX + eyeSpacing, eyeCenterY - 10f, centerX + eyeSpacing + 14f, eyeCenterY + 4f)
        }
        drawPath(path = happyEyeLeft, color = Color(0xFF261943), style = Stroke(width = 4.5f, cap = StrokeCap.Round))
        drawPath(path = happyEyeRight, color = Color(0xFF261943), style = Stroke(width = 4.5f, cap = StrokeCap.Round))
        return
    }

    // Normal or Blinking Eyes
    val eyeWidth = 28f
    val maxEyeHeight = 32f
    val currentEyeHeight = (maxEyeHeight * (1f - blinkProgress)).coerceAtLeast(2f)

    if (currentEyeHeight <= 3f) {
        // Fully closed eyelid line
        drawLine(
            color = Color(0xFF2B1C49),
            start = Offset(centerX - eyeSpacing - 14f, eyeCenterY),
            end = Offset(centerX - eyeSpacing + 14f, eyeCenterY),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF2B1C49),
            start = Offset(centerX + eyeSpacing - 14f, eyeCenterY),
            end = Offset(centerX + eyeSpacing + 14f, eyeCenterY),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
        return
    }

    // Draw Left and Right Eyes
    listOf(-eyeSpacing, eyeSpacing).forEach { xOffset ->
        val ex = centerX + xOffset + (if (xOffset > 0) 1f else -1f) * 2f

        // Eye Sclera (White background)
        val eyePath = Path().apply {
            moveTo(ex - eyeWidth / 2f, eyeCenterY)
            cubicTo(
                ex - eyeWidth / 2f, eyeCenterY - currentEyeHeight / 2f,
                ex + eyeWidth / 2f, eyeCenterY - currentEyeHeight / 2f,
                ex + eyeWidth / 2f, eyeCenterY
            )
            cubicTo(
                ex + eyeWidth / 2f, eyeCenterY + currentEyeHeight / 2f,
                ex - eyeWidth / 2f, eyeCenterY + currentEyeHeight / 2f,
                ex - eyeWidth / 2f, eyeCenterY
            )
            close()
        }
        drawPath(path = eyePath, color = Color(0xFFFAFAFE))

        // Large Anime Iris (Rich Violet & Amethyst gradient with Cyan Sparks)
        val irisRadiusX = 10.5f
        val irisRadiusY = (14f * (1f - blinkProgress * 0.7f)).coerceAtLeast(3f)
        val irisCenterX = ex + eyeGlanceX
        val irisCenterY = eyeCenterY + eyeGlanceY

        val irisBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF2A104E), // Deep dark violet top
                Color(0xFF6B2FB8), // Vibrant amethyst
                Color(0xFFB367FF), // Bright lavender violet
                Color(0xFF00E5FF)  // Glowing cyan neon base
            ),
            startY = irisCenterY - irisRadiusY,
            endY = irisCenterY + irisRadiusY
        )

        drawArc(
            brush = irisBrush,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = true,
            topLeft = Offset(irisCenterX - irisRadiusX, irisCenterY - irisRadiusY),
            size = Size(irisRadiusX * 2f, irisRadiusY * 2f)
        )

        // Dark Pupil
        drawCircle(
            color = Color(0xFF140827),
            radius = irisRadiusX * 0.45f,
            center = Offset(irisCenterX, irisCenterY)
        )

        // Main Anime Sparkle Highlight (Top right)
        drawCircle(
            color = Color.White,
            radius = 3.5f,
            center = Offset(irisCenterX + 3.2f, irisCenterY - 4.5f)
        )

        // Secondary Sparkle Highlight (Bottom left)
        drawCircle(
            color = Color(0xFFCCFFFF),
            radius = 2.0f,
            center = Offset(irisCenterX - 3.5f, irisCenterY + 4f)
        )

        // Upper Eyelash line (Bold, stylish anime wing)
        val upperLash = Path().apply {
            moveTo(ex - eyeWidth / 2f - 2f, eyeCenterY - currentEyeHeight * 0.42f)
            quadraticTo(
                ex, eyeCenterY - currentEyeHeight * 0.58f,
                ex + eyeWidth / 2f + (if (xOffset > 0) 3.5f else 1f), eyeCenterY - currentEyeHeight * 0.35f
            )
        }
        drawPath(
            path = upperLash,
            color = Color(0xFF1E1138),
            style = Stroke(width = 3.8f, cap = StrokeCap.Round)
        )

        // Double Eyelid Crease
        val crease = Path().apply {
            moveTo(ex - eyeWidth * 0.35f, eyeCenterY - currentEyeHeight * 0.65f)
            quadraticTo(
                ex, eyeCenterY - currentEyeHeight * 0.72f,
                ex + eyeWidth * 0.35f, eyeCenterY - currentEyeHeight * 0.62f
            )
        }
        drawPath(
            path = crease,
            color = Color(0x664A3870),
            style = Stroke(width = 1.6f, cap = StrokeCap.Round)
        )
    }
}

/**
 * Layer G: Anime Eyebrows
 */
private fun DrawScope.drawAnimeEyebrows(
    centerX: Float,
    centerY: Float,
    state: MayaAvatarState
) {
    val browY = centerY - 24f

    val leftBrow = Path()
    val rightBrow = Path()

    when (state) {
        MayaAvatarState.THINKING -> {
            // One raised curiously, one thoughtful
            leftBrow.moveTo(centerX - 40f, browY + 3f)
            leftBrow.quadraticTo(centerX - 26f, browY - 4f, centerX - 12f, browY + 1f)

            rightBrow.moveTo(centerX + 12f, browY - 6f)
            rightBrow.quadraticTo(centerX + 26f, browY - 12f, centerX + 40f, browY - 5f)
        }
        MayaAvatarState.LISTENING -> {
            // Attentive, slightly raised
            leftBrow.moveTo(centerX - 38f, browY - 2f)
            leftBrow.quadraticTo(centerX - 25f, browY - 7f, centerX - 12f, browY - 2f)

            rightBrow.moveTo(centerX + 12f, browY - 2f)
            rightBrow.quadraticTo(centerX + 25f, browY - 7f, centerX + 38f, browY - 2f)
        }
        MayaAvatarState.HAPPY -> {
            // Joyful gentle high curve
            leftBrow.moveTo(centerX - 38f, browY - 4f)
            leftBrow.quadraticTo(centerX - 25f, browY - 11f, centerX - 12f, browY - 4f)

            rightBrow.moveTo(centerX + 12f, browY - 4f)
            rightBrow.quadraticTo(centerX + 25f, browY - 11f, centerX + 38f, browY - 4f)
        }
        else -> {
            // Friendly soft resting smile brows
            leftBrow.moveTo(centerX - 38f, browY)
            leftBrow.quadraticTo(centerX - 25f, browY - 6f, centerX - 12f, browY - 1f)

            rightBrow.moveTo(centerX + 12f, browY - 1f)
            rightBrow.quadraticTo(centerX + 25f, browY - 6f, centerX + 38f, browY)
        }
    }

    drawPath(path = leftBrow, color = Color(0xFF674B9E), style = Stroke(width = 2.8f, cap = StrokeCap.Round))
    drawPath(path = rightBrow, color = Color(0xFF674B9E), style = Stroke(width = 2.8f, cap = StrokeCap.Round))
}

/**
 * Layer H: Cute Anime Nose
 */
private fun DrawScope.drawAnimeNose(
    centerX: Float,
    centerY: Float
) {
    // Subtle, minimalist anime button nose
    drawCircle(
        color = Color(0xFFE4A496),
        radius = 2.2f,
        center = Offset(centerX, centerY + 18f)
    )
}

/**
 * Layer I: Animated Lip-sync Mouth
 */
private fun DrawScope.drawAnimeMouth(
    centerX: Float,
    centerY: Float,
    state: MayaAvatarState,
    mouthAnim: Float,
    audioVolume: Float
) {
    val mouthY = centerY + 36f

    when (state) {
        MayaAvatarState.SPEAKING -> {
            // Phoneme modulation based on speaking animation & audio amplitude
            val mouthOpen = (mouthAnim * 0.7f + audioVolume * 0.5f).coerceIn(0.2f, 1.2f)
            val mouthWidth = 12f + (mouthOpen * 6f)
            val mouthHeight = 4f + (mouthOpen * 14f)

            val mouthPath = Path().apply {
                moveTo(centerX - mouthWidth / 2f, mouthY)
                cubicTo(
                    centerX - mouthWidth / 3f, mouthY + mouthHeight,
                    centerX + mouthWidth / 3f, mouthY + mouthHeight,
                    centerX + mouthWidth / 2f, mouthY
                )
                close()
            }
            // Mouth interior (deep rose)
            drawPath(path = mouthPath, color = Color(0xFF9E2A48))

            // Cute little tongue hint
            drawCircle(
                color = Color(0xFFFF7096),
                radius = mouthHeight * 0.35f,
                center = Offset(centerX, mouthY + mouthHeight * 0.75f)
            )

            // Lip outline
            drawPath(
                path = mouthPath,
                color = Color(0xFF5E1026),
                style = Stroke(width = 2.0f, cap = StrokeCap.Round)
            )
        }
        MayaAvatarState.HAPPY -> {
            // Broad cheerful open smile
            val happyMouth = Path().apply {
                moveTo(centerX - 13f, mouthY - 1f)
                cubicTo(centerX - 8f, mouthY + 12f, centerX + 8f, mouthY + 12f, centerX + 13f, mouthY - 1f)
                close()
            }
            drawPath(path = happyMouth, color = Color(0xFFC2185B))
            // Upper teeth hint
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(centerX - 7f, mouthY - 1f),
                size = Size(14f, 4.5f)
            )
            drawPath(path = happyMouth, color = Color(0xFF6E0D27), style = Stroke(width = 2.2f))
        }
        MayaAvatarState.THINKING -> {
            // Cute thoughtful small pursed mouth
            val thinkMouth = Path().apply {
                moveTo(centerX - 4f, mouthY)
                quadraticTo(centerX + 3f, mouthY - 2f, centerX + 6f, mouthY + 2f)
            }
            drawPath(path = thinkMouth, color = Color(0xFF8E3B5C), style = Stroke(width = 2.6f, cap = StrokeCap.Round))
        }
        MayaAvatarState.LISTENING -> {
            // Soft open O listening shape
            drawOval(
                color = Color(0xFF9E2A48),
                topLeft = Offset(centerX - 4.5f, mouthY - 1f),
                size = Size(9f, 6.5f)
            )
            drawOval(
                color = Color(0xFF5E1026),
                topLeft = Offset(centerX - 4.5f, mouthY - 1f),
                size = Size(9f, 6.5f),
                style = Stroke(width = 1.8f)
            )
        }
        MayaAvatarState.IDLE -> {
            // Gentle anime smile curve
            val idleSmile = Path().apply {
                moveTo(centerX - 10f, mouthY)
                quadraticTo(centerX, mouthY + 6f, centerX + 10f, mouthY)
            }
            drawPath(path = idleSmile, color = Color(0xFF8E3B5C), style = Stroke(width = 2.4f, cap = StrokeCap.Round))
        }
    }
}

/**
 * Layer J: Front Bangs & Hair Layer
 */
private fun DrawScope.drawFrontHairAndBangs(
    centerX: Float,
    centerY: Float,
    sway: Float
) {
    val hairGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFE8DEFC), // Silvery lavender top highlight
            Color(0xFFC0ADE8), // Main lavender hair
            Color(0xFF8F72D0)  // Hair shadow
        )
    )

    // Top Voluminous Hair Dome
    val hairDome = Path().apply {
        moveTo(centerX - 56f, centerY - 10f)
        cubicTo(
            centerX - 62f, centerY - 65f,
            centerX + 62f, centerY - 65f,
            centerX + 56f, centerY - 10f
        )
        cubicTo(
            centerX + 48f, centerY - 35f,
            centerX - 48f, centerY - 35f,
            centerX - 56f, centerY - 10f
        )
        close()
    }
    drawPath(path = hairDome, brush = hairGradient)

    // Left Front Bangs
    val leftBang = Path().apply {
        moveTo(centerX - 52f, centerY - 30f)
        cubicTo(
            centerX - 45f + sway * 0.5f, centerY - 10f,
            centerX - 35f + sway * 0.8f, centerY + 8f,
            centerX - 24f, centerY + 5f
        )
        cubicTo(
            centerX - 28f, centerY - 15f,
            centerX - 35f, centerY - 25f,
            centerX - 45f, centerY - 32f
        )
        close()
    }
    drawPath(path = leftBang, brush = hairGradient)

    // Center Anime Forehead Bangs
    val centerBang = Path().apply {
        moveTo(centerX - 20f, centerY - 32f)
        quadraticTo(centerX - 8f + sway * 0.4f, centerY - 8f, centerX - 2f, centerY - 12f)
        quadraticTo(centerX + 8f + sway * 0.4f, centerY - 6f, centerX + 20f, centerY - 32f)
        close()
    }
    drawPath(path = centerBang, brush = hairGradient)

    // Right Front Bangs
    val rightBang = Path().apply {
        moveTo(centerX + 52f, centerY - 30f)
        cubicTo(
            centerX + 45f - sway * 0.5f, centerY - 10f,
            centerX + 35f - sway * 0.8f, centerY + 8f,
            centerX + 24f, centerY + 5f
        )
        cubicTo(
            centerX + 28f, centerY - 15f,
            centerX + 35f, centerY - 25f,
            centerX + 45f, centerY - 32f
        )
        close()
    }
    drawPath(path = rightBang, brush = hairGradient)

    // Hair Shimmer / Angel Ring (Glossy anime highlight across hair)
    val shimmerPath = Path().apply {
        moveTo(centerX - 42f, centerY - 38f)
        quadraticTo(centerX, centerY - 44f, centerX + 42f, centerY - 38f)
    }
    drawPath(
        path = shimmerPath,
        color = Color(0xCCFFFFFF),
        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
    )
}

/**
 * Layer K: Futuristic Headset
 */
private fun DrawScope.drawFuturisticHeadset(
    centerX: Float,
    centerY: Float,
    glowColor: Color,
    state: MayaAvatarState
) {
    // Headset Over-Head Arch (Sleek dark metallic titanium)
    val headArch = Path().apply {
        moveTo(centerX - 56f, centerY - 8f)
        cubicTo(
            centerX - 60f, centerY - 68f,
            centerX + 60f, centerY - 68f,
            centerX + 56f, centerY - 8f
        )
    }
    drawPath(
        path = headArch,
        color = Color(0xFF231B3D),
        style = Stroke(width = 5.5f, cap = StrokeCap.Round)
    )
    drawPath(
        path = headArch,
        color = glowColor.copy(alpha = 0.7f),
        style = Stroke(width = 1.8f, cap = StrokeCap.Round)
    )

    // Left Ear Cup (Circular cybernetic node with glowing ring)
    val leftEarCenter = Offset(centerX - 56f, centerY - 4f)
    drawCircle(color = Color(0xFF16102B), radius = 14f, center = leftEarCenter)
    drawCircle(color = glowColor, radius = 11f, center = leftEarCenter, style = Stroke(width = 2.5f))
    drawCircle(color = Color.White, radius = 4f, center = leftEarCenter)

    // Right Ear Cup
    val rightEarCenter = Offset(centerX + 56f, centerY - 4f)
    drawCircle(color = Color(0xFF16102B), radius = 14f, center = rightEarCenter)
    drawCircle(color = glowColor, radius = 11f, center = rightEarCenter, style = Stroke(width = 2.5f))
    drawCircle(color = Color.White, radius = 4f, center = rightEarCenter)

    // Sleek Holographic Mic Boom from right ear to mouth
    val micBoom = Path().apply {
        moveTo(centerX + 52f, centerY + 2f)
        cubicTo(
            centerX + 46f, centerY + 26f,
            centerX + 32f, centerY + 36f,
            centerX + 18f, centerY + 38f
        )
    }
    drawPath(
        path = micBoom,
        color = Color(0xFF322557),
        style = Stroke(width = 2.6f, cap = StrokeCap.Round)
    )
    // Mic Tip with glowing LED
    drawCircle(
        color = glowColor,
        radius = 3.5f,
        center = Offset(centerX + 18f, centerY + 38f)
    )
    drawCircle(
        color = Color.White,
        radius = 1.5f,
        center = Offset(centerX + 18f, centerY + 38f)
    )
}
