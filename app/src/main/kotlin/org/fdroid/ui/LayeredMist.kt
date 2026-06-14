package org.fdroid.ui
/* Copyright (C) 2026 Phillip Ahlgren - Modifications for CustoneOS */

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun CustoneLayeredMist(isOverlay: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "mist")
    
    // 🚨 CRANKED VIBRANCY & SATURATION 🚨
    val blue = Color(0xFF00E5FF)
    val green = Color(0xFF00FF88)
    val purple = Color(0xFFFF00FF)
    val deepPurple = Color(0xFF7C4DFF)

    val o1x by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(25000, easing = LinearEasing), RepeatMode.Restart), label = "o1x")
    val o1y by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(18000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "o1y")
    val o2x by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(22000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "o2x")
    val o2y by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(28000, easing = LinearEasing), RepeatMode.Restart), label = "o2y")
    val o3x by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(30000, easing = LinearEasing), RepeatMode.Restart), label = "o3x")
    val o3y by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(35000, easing = LinearEasing), RepeatMode.Restart), label = "o3y")
    val o4x by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(19000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "o4x")
    val o4y by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(26000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "o4y")

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .blur(if (isOverlay) 110.dp else 60.dp)
            .graphicsLayer(alpha = if (isOverlay) 0.35f else 0.55f)
    ) {
        val w = size.width
        val h = size.height

        fun mapWrap(v: Float, r: Float, max: Float): Float = -r + v * (max + r * 2)
        fun mapBounce(v: Float, r: Float, max: Float): Float = -r * 0.5f + v * (max + r)

        // 🚨 ANTI-BANDING: Fading to 0f alpha of the SAME color instead of default black-transparent! 🚨
        if (!isOverlay) {
            val r1 = 1400f
            drawCircle(Brush.radialGradient(listOf(blue, blue.copy(alpha = 0f)), center = Offset(mapWrap(o1x, r1, w), mapBounce(o1y, r1, h)), radius = r1), radius = r1)

            val r2 = 1200f
            drawCircle(Brush.radialGradient(listOf(purple, purple.copy(alpha = 0f)), center = Offset(mapBounce(o2x, r2, w), mapWrap(o2y, r2, h)), radius = r2), radius = r2)

            val r3 = 1500f
            drawCircle(Brush.radialGradient(listOf(green, green.copy(alpha = 0f)), center = Offset(mapWrap(o3x, r3, w), mapWrap(o3y, r3, h)), radius = r3), radius = r3)
        } else {
            val r4 = 1100f
            drawCircle(Brush.radialGradient(listOf(deepPurple, deepPurple.copy(alpha = 0f)), center = Offset(mapBounce(o4x, r4, w), mapBounce(o4y, r4, h)), radius = r4), radius = r4)
            
            val r5 = 900f
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha=0.6f), Color.White.copy(alpha = 0f)), center = Offset(mapWrap(o1y, r5, w), mapWrap(o2x, r5, h)), radius = r5), radius = r5)
        }
    }
}
