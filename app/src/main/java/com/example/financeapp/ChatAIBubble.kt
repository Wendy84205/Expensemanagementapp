package com.example.financeapp

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// =============================================
//   🔥 AI BUBBLE V4 – WOBBLE + RIPPLE + SLEEP
// =============================================

@Composable
fun ChatAIBubble(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var isPressed by remember { mutableStateOf(false) }

    // 💤 Sleep/Wake State
    var isSleeping by remember { mutableStateOf(false) }
    var lastInteraction by remember { mutableStateOf(System.currentTimeMillis()) }
    val coroutineScope = rememberCoroutineScope()

    // 💤 AUTO SLEEP AFTER 8s
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            if (System.currentTimeMillis() - lastInteraction > 8000) {
                isSleeping = true
            }
        }
    }

    // 🌙 SLEEP MODE OPACITY
    val bubbleAlpha by animateFloatAsState(
        targetValue = if (isSleeping) 0.55f else 1f,
        animationSpec = tween(600, easing = LinearOutSlowInEasing),
        label = "alpha"
    )

    // 🤖 "Wake up"
    fun wakeAI() {
        lastInteraction = System.currentTimeMillis()
        if (isSleeping) {
            isSleeping = false
        }
    }

    // 🌟 Bounce animation khi nhấn
    val bubbleSize by animateDpAsState(
        targetValue = if (isPressed) 62.dp else 72.dp,
        animationSpec = spring(
            stiffness = Spring.StiffnessLow,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "bubbleSize"
    )

    // ✨ Glow animation (pulse)
    val glowAlpha by rememberInfiniteTransition().animateFloat(
        initialValue = if (isSleeping) 0.08f else 0.25f,
        targetValue = if (isSleeping) 0.15f else 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // 🔥 Wobble animation
    val wobble by rememberInfiniteTransition().animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wobble"
    )

    // 🌈 Gradient chủ đạo
    val gradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF0F4C75),
            Color(0xFF2E8B57)
        ),
        center = Offset(0.3f, 0.3f)
    )

    // 🔥 Ripple cam (#ED8936)
    var rippleTrigger by remember { mutableStateOf(false) }
    val rippleAlpha by animateFloatAsState(
        targetValue = if (rippleTrigger) 0f else 0.35f,
        animationSpec = tween(550),
        label = "rippleAlpha"
    )
    val rippleSize by animateFloatAsState(
        targetValue = if (rippleTrigger) 2.2f else 0.8f,
        animationSpec = tween(550),
        label = "rippleSize"
    )

    if (rippleTrigger) {
        LaunchedEffect(Unit) {
            delay(550)
            rippleTrigger = false
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                translationX = wobble // ✨ wobble drift
                alpha = bubbleAlpha   // ✨ sleep fade
            }
            .offset { IntOffset(offsetX.toInt(), offsetY.toInt()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isPressed = true
                        wakeAI()
                    },
                    onDragEnd = {
                        isPressed = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        wakeAI()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                )
            }
            .size(bubbleSize)
            .shadow(
                elevation = if (isPressed) 4.dp else 20.dp,
                shape = CircleShape,
                ambientColor = Color(0xFF0F4C75).copy(alpha = glowAlpha),
                spotColor = Color(0xFFED8936).copy(alpha = glowAlpha)
            )
            .clip(CircleShape)
            .background(gradient)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                wakeAI()
                rippleTrigger = true
                onClick()
                isPressed = true
                coroutineScope.launch {
                    delay(150)
                    isPressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {

        // 🔥 Ripple cam
        Box(
            modifier = Modifier
                .size(bubbleSize * rippleSize)
                .graphicsLayer { alpha = rippleAlpha }
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFFED8936).copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        // ICON: ngủ → 😴 , thức → 🤖
        Icon(
            imageVector = Icons.Default.SmartToy,
            contentDescription = "AI Assistant",
            tint = if (isSleeping) Color(0xFF718096) else Color.White,
            modifier = Modifier
                .size(if (isSleeping) 28.dp else 34.dp)
                .graphicsLayer {
                    rotationZ = if (isSleeping) -15f else 0f
                }
        )
    }
}

// ===============================
//       WRAPPER VERSION
// ===============================

@Composable
fun FloatingAIBubble(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val maxX = 300f
    val maxY = 600f
    val minX = -300f
    val minY = -600f

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    offsetX.coerceIn(minX, maxX).toInt(),
                    offsetY.coerceIn(minY, maxY).toInt()
                )
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX = (offsetX + dragAmount.x).coerceIn(minX, maxX)
                    offsetY = (offsetY + dragAmount.y).coerceIn(minY, maxY)
                }
            }
    ) {
        ChatAIBubble(onClick = onClick)
    }
}
