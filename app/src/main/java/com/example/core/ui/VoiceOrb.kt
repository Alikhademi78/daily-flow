package com.example.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ai_engine.orchestrator.AssistantState

@Composable
fun VoiceOrb(
    state: AssistantState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbTransitions")

    // --- Core Animations ---
    // Continuous breathing scale animation
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbScale"
    )

    // Infinite spinning rotation speed when processing AI responses
    val processingRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbRotation"
    )

    // Ripple multiplier animation for listening
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 2.0f, // More expansion
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing), // Faster
            repeatMode = RepeatMode.Restart
        ),
        label = "RippleScale"
    )

    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, // Stronger start
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RippleAlpha"
    )

    // --- Dynamic Voice Wave Animations ---
    // If we're listening or processing, let's create a beautiful soundwave animation
    val isAnimating = state is AssistantState.Listening || state is AssistantState.Processing || state is AssistantState.AwaitingConfirmation

    val bar1Height by if (isAnimating) {
        infiniteTransition.animateFloat(
            initialValue = 12f,
            targetValue = 28f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Bar1"
        )
    } else {
        remember { mutableStateOf(16f) }
    }

    val bar2Height by if (isAnimating) {
        infiniteTransition.animateFloat(
            initialValue = 20f,
            targetValue = 44f,
            animationSpec = infiniteRepeatable(
                animation = tween(500, delayMillis = 100, easing = FastOutLinearInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Bar2"
        )
    } else {
        remember { mutableStateOf(32f) }
    }

    val bar3Height by if (isAnimating) {
        infiniteTransition.animateFloat(
            initialValue = 8f,
            targetValue = 24f,
            animationSpec = infiniteRepeatable(
                animation = tween(700, delayMillis = 50, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Bar3"
        )
    } else {
        remember { mutableStateOf(12f) }
    }

    val bar4Height by if (isAnimating) {
        infiniteTransition.animateFloat(
            initialValue = 16f,
            targetValue = 38f,
            animationSpec = infiniteRepeatable(
                animation = tween(550, delayMillis = 150, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Bar4"
        )
    } else {
        remember { mutableStateOf(26f) }
    }

    val bar5Height by if (isAnimating) {
        infiniteTransition.animateFloat(
            initialValue = 10f,
            targetValue = 22f,
            animationSpec = infiniteRepeatable(
                animation = tween(650, delayMillis = 80, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Bar5"
        )
    } else {
        remember { mutableStateOf(18f) }
    }

    // --- Colors from Immersive UI theme html ---
    val ambientGlowColor = when (state) {
        is AssistantState.Listening -> Color(0x5D10B981) // Strong emerald active listening glow
        is AssistantState.Processing -> Color(0x3D06B6D4) // Turquoise cyber glow
        is AssistantState.AwaitingConfirmation -> Color(0x3DF59E0B) // Amber alert glow
        else -> Color(0x1F8B5CF6) // Standard elegant soft violet shadow
    }

    // Main central gradient colors depending on AI status
    val gradientColors = when (state) {
        is AssistantState.Listening -> listOf(
            Color(0xFF10B981), // Emerald-500
            Color(0xFF059669), // Emerald-600
            Color(0xFF34D399)  // Mint Emerald
        )
        is AssistantState.Processing -> listOf(
            Color(0xFF06B6D4), // Cyan-500
            Color(0xFF3B82F6), // Blue-500
            Color(0xFF00D2FF)  // Brighter Sky
        )
        is AssistantState.AwaitingConfirmation -> listOf(
            Color(0xFFF59E0B), // Amber-500
            Color(0xFFEF4444), // Red-500
            Color(0xFFF43F5E)  // Rose
        )
        else -> listOf(
            Color(0xFF7C3AED), // Violet-600
            Color(0xFFD946EF), // Fuchsia-500
            Color(0xFF818CF8)  // Indigo-400
        )
    }

    val activeScale = when (state) {
        is AssistantState.Listening -> breathingScale * 1.15f
        is AssistantState.Processing -> breathingScale * 0.98f
        is AssistantState.AwaitingConfirmation -> breathingScale * 1.05f
        else -> 0.94f // Static UI when off
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(240.dp) // Large sizing to fit atmospheric rings safely
            .testTag("voice_orb_container")
    ) {
        // A. Atmospheric Background Glow
        Box(
            modifier = Modifier
                .size(170.dp)
                .scale(activeScale * 1.15f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(ambientGlowColor, Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        // B. HTML: "Outer Glow Rings"
        if (state !is AssistantState.Idle) {
            val ringColor = when(state) {
                is AssistantState.Processing -> Color(0x4006B6D4) // Cyan
                is AssistantState.Listening -> Color(0x4010B981) // Emerald Green
                is AssistantState.AwaitingConfirmation -> Color(0x40F59E0B) // Amber
                else -> Color(0x338B5CF6)
            }
            Box(
                modifier = Modifier
                    .size(210.dp)
                    .scale(activeScale)
                    .border(2.dp, ringColor.copy(alpha = 0.2f), CircleShape)
            )
            
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .scale(activeScale)
                    .border(2.dp, ringColor, CircleShape)
            )
        }

        // Listening Ripple (Pulse wave expanding on listening)
        if (state is AssistantState.Listening || state is AssistantState.AwaitingConfirmation) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(rippleScale)
                    .background(
                        color = (if (state is AssistantState.Listening) Color(0xFF065F46) else Color(0xFFF59E0B)).copy(alpha = rippleAlpha * 0.8f),
                        shape = CircleShape
                    )
            )
        }

        // C. Core Orb Container
        // `w-32 h-32 rounded-full bg-gradient-to-tr from-violet-600 via-fuchsia-500 to-indigo-400 z-10 shadow-[0_0_50px_rgba(139,92,246,0.4)]`
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(124.dp)
                .scale(activeScale)
                .clip(CircleShape)
                .rotate(if (state is AssistantState.Processing) processingRotation else 0f)
                .background(
                    brush = Brush.linearGradient(
                        colors = gradientColors
                    )
                )
                .clickable(
                    onClick = onClick,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                )
                .testTag("voice_orb_clickable")
        ) {
            // D. Inner Glassmorphic layer: `w-24 h-24 rounded-full bg-white/10 backdrop-blur-sm border border-white/20`
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                    .padding(8.dp)
            ) {
                if (state is AssistantState.Processing) {
                    // Modern rotating AI thinking circle indicator
                    androidx.compose.material3.CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(28.dp)
                    )
                } else if (!isAnimating) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "دکمه صوتی دستیار",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    // Custom Waveform: <div class="flex gap-1 items-center h-8">
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(48.dp)
                    ) {
                        // Bar 1
                        Box(
                            modifier = Modifier
                                .width(3.5.dp)
                                .height(bar1Height.dp)
                                .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                        )
                        // Bar 2
                        Box(
                            modifier = Modifier
                                .width(3.5.dp)
                                .height(bar2Height.dp)
                                .background(Color.White, RoundedCornerShape(2.dp))
                        )
                        // Bar 3
                        Box(
                            modifier = Modifier
                                .width(3.5.dp)
                                .height(bar3Height.dp)
                                .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                        )
                        // Bar 4
                        Box(
                            modifier = Modifier
                                .width(3.5.dp)
                                .height(bar4Height.dp)
                                .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(2.dp))
                        )
                        // Bar 5
                        Box(
                            modifier = Modifier
                                .width(3.5.dp)
                                .height(bar5Height.dp)
                                .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
        }
    }
}

