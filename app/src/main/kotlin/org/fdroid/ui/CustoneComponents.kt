package org.fdroid.ui
/* Copyright (C) 2026 Phillip Ahlgren - Pure Box Physics Engine with Haptics */

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun Modifier.bounceClick(scaleDown: Float = 0.92f, onClick: () -> Unit): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val view = LocalView.current
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = if (isPressed) tween(50) else spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "bounce"
    )

    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    val up = waitForUpOrCancellation()
                    isPressed = false
                    if (up != null) {
                        // 🚨 ACCENT HAPTIC BUMP 🚨
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onClick()
                    }
                }
            }
        }
}

@Composable
fun CustoneProgressIndicator(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "spinner")
    val pulseScale by transition.animateFloat(initialValue = 0.8f, targetValue = 1.4f, animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "pulseScale")
    val pulseAlpha by transition.animateFloat(initialValue = 1f, targetValue = 0f, animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "pulseAlpha")
    val rotation by transition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart), label = "rotation")

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.matchParentSize().scale(pulseScale).border(2.dp, Color(0xFF007AFF).copy(alpha = pulseAlpha), CircleShape))
        CircularProgressIndicator(modifier = Modifier.matchParentSize().graphicsLayer { rotationZ = rotation }, color = Color(0xFF007AFF), strokeWidth = 3.dp)
    }
}

@Composable
fun ClickyButton(text: String, onClick: () -> Unit, isPrimary: Boolean = true, modifier: Modifier = Modifier) {
    val bgColor = if (isPrimary) Color(0xFF007AFF) else Color(0xFFF2F2F2).copy(alpha = 0.2f)
    val contentColor = if (isPrimary) Color.White else Color.Black
    
    Box(
        modifier = modifier
            .height(58.dp)
            .bounceClick(onClick = onClick)
            .background(bgColor, CircleShape)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) { 
        Text(text = text, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = contentColor) 
    }
}

@Composable
fun ClickyIconButton(icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val iconColor = if (isSystemInDarkTheme()) Color.White else Color(0xFF007AFF)
    
    Box(
        modifier = modifier
            .size(48.dp)
            .bounceClick(onClick = onClick)
            .background(Color(0xFFF2F2F2).copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) { 
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = iconColor) 
    }
}
